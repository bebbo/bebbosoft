/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.install-plugin.
 *
 *   de.bb.tools.bnm.plugin.install-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.install-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.install-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm.plugin.install;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Id;

public abstract class AbstractInstallPlugin extends AbstractPlugin {
  /**
   * create checksums or not.
   */
  @Config("createChecksum")
  protected boolean createChecksum;

  /**
   * MD5.
   */
  protected MessageDigest md5Digester;

  /**
   * SHA-1.
   */
  protected MessageDigest sha1Digester;

  @Property("${project.build.directory}")
  protected File directory;

  protected AbstractInstallPlugin() {
    try {
      sha1Digester = MessageDigest.getInstance("SHA-1");
      md5Digester = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get File for an artifact.
   * @param artifact the artifact id
   * @return a File object.
   */
  protected File getLocalRepoFile(Id artifact) throws Exception {
    File file = project.getLoader().findFile(artifact, null);
    return file;
  }

  /**
   * Installs both checksums for a specified artifact.
   * @param artifact the artifact id
   */
  protected void installChecksums(Id artifact) throws Exception {
    if (!createChecksum) {
      return;
    }

    File artifactFile = getLocalRepoFile(artifact);
    installChecksums(artifactFile);
  }

  /**
   * Installs both checksums for a specified artifact.
   * @param installedFile the File object to create checksums for
   */
  protected void installChecksums(File installedFile) throws Exception {
    installChecksum(installedFile, md5Digester, ".md5");
    installChecksum(installedFile, sha1Digester, ".sha1");
  }

  /**
   * Installs a checksum of the specified type for a specified artifact.
   * @param installedFile the File object to create checksums for
   * @param digest the MessageDigest to use for the checksum
   */
  private void installChecksum(File installedFile, MessageDigest digest, String ext) throws Exception {
    String checksum;
    getLog().debug("Calculating " + digest.getAlgorithm() + " checksum for " + installedFile);

    checksum = calc(digest, installedFile);

    File checksumFile = new File(installedFile.getAbsolutePath() + ext);
    getLog().debug("Installing checksum to " + checksumFile);
    checksumFile.getParentFile().mkdirs();
    fileWrite(checksumFile.getAbsolutePath(), "UTF-8", checksum);
  }

  private void fileWrite(String absolutePath, String charSetName, String checksum) throws Exception {
    FileOutputStream fos = new FileOutputStream(absolutePath);
    fos.write(checksum.getBytes(charSetName));
    fos.close();
  }

}
