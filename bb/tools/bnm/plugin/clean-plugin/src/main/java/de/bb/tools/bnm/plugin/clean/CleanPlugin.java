/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.clean-plugin.
 *
 *   de.bb.tools.bnm.plugin.clean-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.clean-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.clean-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm.plugin.clean;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Default;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.util.FileBrowser;

public class CleanPlugin extends AbstractPlugin {

  @Property("${project.build.directory}")
  private File directory;

  @Property("${project.build.outputDirectory}")
  private File outputDirectory;

  @Property("${project.build.testOutputDirectory}")
  private File testOutputDirectory;

  @Property("${project.reporting.outputDirectory}")
  private File reportDirectory;

  @Config("verbose")
  private boolean verbose;

  @Config("filesets")
  private List<FileSet> filesets;

  @Config("followSymLinks")
  private boolean followSymLinks;

  @Config("skip")
  private boolean skip;

  @Config("failOnError")
  @Default("true")
  private boolean failOnError;

  @Config("excludeDefaultDirectories")
  private boolean excludeDefaultDirectories;

  @Override
  public void execute() throws Exception {
    if (skip) {
      getLog().info("Clean is skipped.");
      return;
    }

    try {
      if (!excludeDefaultDirectories) {
        removeDirectory(directory);
        removeDirectory(outputDirectory);
        removeDirectory(testOutputDirectory);
        removeDirectory(reportDirectory);
      }

      removeAdditionalFilesets();
    } catch (Exception e) {
      if (failOnError) {
        throw e;
      }

      getLog().warn(e.getMessage());
    }
  }

  /**
   * Deletes additional file-sets specified by the <code>filesets</code> tag.
   * @throws Exception 
   *
   * @throws MojoExecutionException When a directory failed to get deleted.
   */
  private void removeAdditionalFilesets() throws Exception {
    if (filesets != null && !filesets.isEmpty()) {
      for (FileSet fileSet : filesets) {
        removeFileSet(fileSet);
      }
    }
  }

  /**
   * Deletes a directory and its contents.
   *
   * @param dir The base directory of the included and excluded files.
   * @throws Exception 
   * @throws MojoExecutionException When a directory failed to get deleted.
   */
  private void removeDirectory(File dir) throws Exception {
    if (dir != null) {
      FileSet fs = new FileSet();
      fs.directory = dir.getPath();
      fs.includes.add("**");
      fs.followSymlinks = followSymLinks;

      removeFileSet(fs);
    }
  }

  /**
   * Deletes the specified file set. If the base directory of the file set is relative, it will be resolved against
   * the base directory of the current project.
   *
   * @param fileset The file set to delete, must not be <code>null</code>.
   * @throws MojoExecutionException When the file set failed to get deleted.
   */
  private void removeFileSet(FileSet fileset) throws Exception {
    try {
      File dir = new File(fileset.directory);

      if (!dir.isAbsolute()) {
        dir = new File(project.getPath(), fileset.directory);
        fileset.directory = dir.getPath();
      }

      if (!dir.exists()) {
        getLog().debug("Skipping non-existing directory: " + dir);
        return;
      }

      if (!dir.isDirectory()) {
        throw new Exception(dir + " is not a directory.");
      }

      getLog().info("Deleting " + fileset);
      fileSetManagerDelete(fileset);
    } catch (IOException e) {
      throw new Exception("Failed to delete directory: " + fileset.directory + ". Reason: " + e.getMessage(), e);
    } catch (IllegalStateException e) {
      throw new Exception("Failed to delete directory: " + fileset.directory + ". Reason: " + e.getMessage(), e);
    }
  }

  private boolean ok = true;
  private void fileSetManagerDelete(FileSet fileset) throws IOException {
    FileBrowser fb = new FileBrowser() {
      @Override
      protected void handleFile(String path, String file) {
        ok |= new File(new File(getBaseDir(), path), file).delete();
      }

      @Override
      protected void leaveDir(String path) {
        ok |= new File(getBaseDir(), path).delete();
      }
      
    };
    
    for (String inc : fileset.includes) {
      fb.addInclude(inc);
    }
    for (String exc : fileset.excludes) {
      fb.addExclude(exc);
    }
    
    fb.scan(fileset.directory, true);
    if (!ok) throw new IOException ("failed to delete: " + fileset.directory);
  }

}
