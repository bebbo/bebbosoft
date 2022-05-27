/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Id;

public class CpHelper {

  /**
   * Separate class path per output Directory (normal compiler or test compile).
   */
  private final HashMap<File, ArrayList<String>> CPMAP = new HashMap<File, ArrayList<String>>();

  /**
   * Mapping to repository files. 
   */
  // private final static HashMap/*<String, String>*/ REPOMAP = new HashMap/*<String, String>*/();
  /**
   * Unpacked elements of specific jar files.
   */
  private final HashMap<File, ArrayList<String>> EMBEDMAP = new HashMap<File, ArrayList<String>>();

  /**
   * classpathElements extended with embedded jar files.
   * @throws Exception 
   */
  public synchronized List<String> getClasspathElements(Pom project, File dest, File classes, boolean isTest) throws Exception {
    File key = dest;
    ArrayList<String> extendedClasspathElements = CPMAP.get(key);
    if (extendedClasspathElements == null) {
      Log log = Log.getLog();
      extendedClasspathElements = new ArrayList<String>();
      if (isTest)
        extendedClasspathElements.add(classes.getAbsolutePath());

      Loader loader = project.getLoader();

      List<Id> list = isTest ? project.getTestDependencies() : project.getCompileDependencies();
      for (Id a : list) {
          Dependency dep = (Dependency) a;
        if (dep.type != null && !"jar".equals(dep.type))
          continue;
        
        if (a.getId().equals(project.getId()))
        	continue;
        
        File cpElement = loader.findFile(a, dep.classifier , "jar");
        extendedClasspathElements.add(cpElement.getAbsolutePath());
        
        // add the recent evaluation result
        ArrayList<String> embedded = EMBEDMAP.get(cpElement);
        if (embedded == null) {
          embedded = new ArrayList<String>();
          File cpe = cpElement;
          // try to add embedded jar files
          try {
            // check for valid MANIFEST.MF
            ZipFile zf = new ZipFile(cpe);
            ZipEntry manifest = zf.getEntry("META-INF/MANIFEST.MF");
            if (manifest != null) {
              InputStream is = zf.getInputStream(manifest);
              byte b[] = new byte[is.available()];
              is.read(b);
              is.close();

              // does it contain 'Embed-Directory:' ?
              String dir = ".";
              String smanifest = new String(b, 0);
              int pos = smanifest.indexOf("Embed-Directory:");
              if (pos > 0) {
                smanifest = smanifest.substring(pos + 16);
                pos = smanifest.indexOf('\n');
                dir = smanifest.substring(0, pos).trim();
              }
              if (dir.startsWith("."))
                dir = dir.substring(1);
              if (dir.startsWith("/"))
                dir = dir.substring(1);

              // create a folder for embedded jars
              File embeddedFolder = new File(cpe.getParentFile(), ".embedded");
              embeddedFolder.mkdirs();

              // extract *.jar files and add 'em to class path
              for (Enumeration<?> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                String name = ze.getName();
                int last = name.lastIndexOf('/');
                if (last < 0)
                  last = 0;
                String p = name.substring(0, last);
                if (p.equals(dir) && name.endsWith(".jar")) {
                  URL u = new URL("jar:file://" + cpe.getAbsolutePath() + "!/" + name);
                  log.debug("add: " + u);

                  // copy file to temp
                  name = name.substring(last);
                  if (name.startsWith("/"))
                    name = name.substring(1);

                  File embeddedJar = new File(embeddedFolder, name);
                  // add embedded file to path
                  embedded.add(embeddedJar.getAbsolutePath());

                  if (embeddedJar.exists() && embeddedJar.lastModified() != ze.getTime())
                    continue;

                  is = zf.getInputStream(ze);
                  FileOutputStream os = new FileOutputStream(embeddedJar);

                  b = new byte[32768];
                  for (;;) {
                    int len = is.read(b);
                    if (len <= 0)
                      break;
                    os.write(b, 0, len);
                  }
                  os.close();
                  is.close();
                  embeddedJar.setLastModified(ze.getTime());
                }
              }

            }
          } catch (Exception ex) {
            // ignore
          }
          EMBEDMAP.put(cpElement, embedded);
        }
        extendedClasspathElements.addAll(embedded);
      }
      log.debug("extended classpath is:\r\n" + extendedClasspathElements);

      CPMAP.put(key, extendedClasspathElements);
    }
    return extendedClasspathElements;
  }


}
