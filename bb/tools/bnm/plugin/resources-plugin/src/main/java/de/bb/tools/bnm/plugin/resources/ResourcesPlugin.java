/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.resources-plugin.
 *
 *   de.bb.tools.bnm.plugin.resources-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.resources-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.resources-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.model.Resource;
import de.bb.util.FileBrowser;
import de.bb.util.IniFile;

/**
 * Copy resources for the main source code to the main output directory.
 */
public class ResourcesPlugin extends AbstractPlugin {

    /**
     * The character encoding scheme to be applied when filtering resources.
     * 
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */

    @Property("${project.build.sourceEncoding}")
    protected String encoding;

    /**
     * The output directory into which to copy the resources.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    @Property("${project.build.outputDirectory}")
    private File buildOutputDirectory;

    /**
     * The list of resources we want to transfer.
     * 
     * @parameter expression="${project.resources}"
     * @required
     * @readonly
     */
    //  private List resources;
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    //    protected MavenProject project;
    /**
     * The list of additional key-value pairs aside from that of the System, and that of the project, which would be
     * used for the filtering.
     * 
     * @parameter expression="${project.build.filters}"
     */
    //   protected List filters;
    /**
     * 
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */
    //  protected MavenResourcesFiltering mavenResourcesFiltering;
    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    //  protected MavenSession session;
    /**
     * Expression preceded with the String won't be interpolated \${foo} will be replaced with ${foo}
     * 
     * @parameter expression="${maven.resources.escapeString}"
     * @since 2.3
     */
    @Config("escapeString")
    protected String escapeString;

    /**
     * Overwrite existing files even if the destination files are newer.
     * 
     * @parameter expression="${maven.resources.overwrite}" default-value="false"
     * @since 2.3
     */
    @Config("overwrite")
    private boolean overwrite;

    /**
     * Copy any empty directories included in the Resources.
     * 
     * @parameter expression="${maven.resources.includeEmptyDirs}" default-value="false"
     * @since 2.3
     */
    @Config("includeEmptyDirs")
    protected boolean includeEmptyDirs;

    /**
     * Additional file extensions to not apply filtering (already defined are : jpg, jpeg, gif, bmp, png)
     * 
     * @parameter
     * @since 2.3
     */
    @Config("nonFilteredFileExtensions")
    protected List<String> nonFilteredFileExtensions;

    protected HashSet<String> extensionMap = new HashSet<String>();

    public void execute() throws Exception {

        Project epom = project.getEffectivePom();
        ArrayList<Resource> resources = epom.build.resources;

        if (encoding == null && isFilteringEnabled(resources)) {
            getLog().warn("File encoding has not been set, using standard encoding utf-8!");
        }

        if (nonFilteredFileExtensions != null) {
            extensionMap.addAll(nonFilteredFileExtensions);
        }
        copyResources(resources, epom.build.filters, buildOutputDirectory);
    }

    protected void copyResources(List<? extends Resource> resources, List<String> filters, File outputDirectory)
            throws Exception {
        if (resources == null || resources.size() == 0)
            return;

        if (outputDirectory == null)
            outputDirectory = buildOutputDirectory;

        File cd = project.getPath();

        final ArrayList<File> failed = new ArrayList<File>();
        final ArrayList<Exception> except = new ArrayList<Exception>();

        final Map<String, String> variables = project.getVariables();
        if (filters != null) {
            for (String filter : filters) {
                IniFile ini = new IniFile(filter);
                for (Iterator<?> i = ini.getKeys("\1").iterator(); i.hasNext();) {
                    String key = (String) i.next();
                    variables.put(key, ini.getString("\1", key, ""));
                }
            }
        }

        for (final Resource r : resources) {
            if (r.directory == null)
                continue;
            File out = outputDirectory;
            if (r.targetPath != null) {
                File tp = new File(r.targetPath);
                if (tp.isAbsolute())
                    out = tp;
                else
                    out = new File(outputDirectory, r.targetPath);
            }
            File in = new File(r.directory);
            if (!in.isAbsolute()) {
                in = new File(cd, r.directory);
            }
            final File dest = out;
            final File baseDir = in;
            FileBrowser fb = new FileBrowser() {
                @Override
                protected void handleFile(String path, String file) {
                    int dot = file.lastIndexOf('.');
                    if (dot >= 0 && extensionMap.contains(file.substring(dot + 1)))
                        return;

                    File to = new File(new File(dest, path), file);
                    File from = new File(baseDir + path, file);
                    if (to.exists() && !overwrite && to.lastModified() > from.lastModified())
                        return;
                    File dir = to.getParentFile();
                    if (!dir.exists())
                        dir.mkdirs();
                    try {
                        project.markModified();
                        if (!r.filtering) {
                            copyFile(from, to);
                            return;
                        }
                        filteredCopyFile(from, to);
                    } catch (IOException e) {
                        failed.add(from);
                        except.add(e);
                    }
                }

                private void filteredCopyFile(File from, File to) throws IOException {
                    FileInputStream fis = new FileInputStream(from);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis, encoding));
                    FileWriter fw = new FileWriter(to);
                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                        for (int bra = line.indexOf("${");; bra = line.indexOf("${", bra + 1)) {
                            if (bra < 0)
                                break;
                            int ket = line.indexOf("}", bra);
                            String key = line.substring(bra + 2, ket);
                            String repl = variables.get(key);
                            if (repl != null) {
                                line = line.substring(0, bra) + repl + line.substring(ket + 1);
                            }
                        }
                        fw.write(line);
                        fw.write("\n"); // TODO keep line endings!?
                    }
                    br.close();
                    fw.close();
                }

                private void copyFile(File from, File to) throws IOException {
                    FileInputStream fis = new FileInputStream(from);
                    FileOutputStream fos = new FileOutputStream(to);
                    byte buf[] = new byte[0x10000];
                    for (;;) {
                        int len = fis.read(buf, 0, buf.length);
                        if (len <= 0)
                            break;
                        fos.write(buf, 0, len);
                    }
                    fis.close();
                    fos.close();
                }
            };

            for (final String inc : r.includes) {
                if (inc.startsWith("/") || inc.startsWith("*"))
                    fb.addInclude(inc);
                else
                    fb.addInclude("/" + inc);
            }

            for (String exc : r.getExcludes()) {
                if (exc.startsWith("/") || exc.startsWith("*"))
                    fb.addExclude(exc);
                else
                    fb.addExclude("/" + exc);
            }
            fb.scan(baseDir.toString(), true);
        }

        //      mavenResourcesExecution.setEscapeString(escapeString);
        //      mavenResourcesExecution.setOverwrite(overwrite);
        //      mavenResourcesExecution.setIncludeEmptyDirs(includeEmptyDirs);
        //      if (nonFilteredFileExtensions != null) {
        //        mavenResourcesExecution.setNonFilteredFileExtensions(nonFilteredFileExtensions);
        //      }
        //      mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        if (failed.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < failed.size(); ++i) {
                sb.append(failed.get(i).toString() + ": " + except.get(i).getMessage() + "\r\n");
            }
            throw new Exception("failed to copy resources:\r\n" + sb);
        }
    }

    /**
     * Determines whether filtering has been enabled for any resource.
     * 
     * @param resources
     * 
     * @param resources
     *            The set of resources to check for filtering, may be <code>null</code>.
     * @return <code>true</code> if at least one resource uses filtering, <code>false</code> otherwise.
     */
    protected boolean isFilteringEnabled(ArrayList<? extends Resource> resources) {
        if (resources != null) {
            for (Resource r : resources) {
                if (r.filtering) {
                    return true;
                }
            }
        }
        return false;
    }

}
