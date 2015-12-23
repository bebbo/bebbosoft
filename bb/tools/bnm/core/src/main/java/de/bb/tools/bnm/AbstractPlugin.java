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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

import de.bb.tools.bnm.model.Dependency;
import de.bb.util.Misc;
import de.bb.util.MultiMap;

public abstract class AbstractPlugin {
    public static void copyFile(Log log, File inFile, File outFile) throws FileNotFoundException, IOException {
        log.info("copy " + inFile + " -> " + outFile);
        FileInputStream fis = new FileInputStream(inFile);
        outFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(outFile);
        byte buffer[] = new byte[0x8000];
        for (;;) {
            int read = fis.read(buffer, 0, buffer.length);
            if (read <= 0)
                break;
            fos.write(buffer, 0, read);
        }
        fos.close();
        fis.close();
    }

    // the current project.
    protected Pom project;
    
    // the current plugin dependencies.
    protected ArrayList<Dependency> dependencies;

    // the current directory
    protected File currentDir;
    
    protected MultiMap<String, String> configuration = new MultiMap<String, String>();
    
    public abstract void execute() throws Exception;

    protected Log getLog() {
        return Log.getLog();
    }
    
    protected String calc(MessageDigest digest, File originalFile) throws IOException {
        InputStream is = new FileInputStream(originalFile);
        byte b[] = new byte[8192];
        for (;;) {
          int len = is.read(b, 0, b.length);
          if (len <= 0)
            break;
          digest.update(b, 0, len);
        }
        b = digest.digest();
        is.close();
        return Misc.bytes2Hex(b);
      }

    protected boolean forceBuild;
}
