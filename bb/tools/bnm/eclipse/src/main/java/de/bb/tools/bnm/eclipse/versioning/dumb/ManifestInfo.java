/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.versioning.dumb;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

public class ManifestInfo {

	private String symbolicName;
	private Pos symbolicNamePos;
	private String version;
	private Pos versionPos;
	private HashMap<String, NameVersion> importMap;
	private HashMap<String, Pos> exportMap;
	private HashMap<String, NameVersion> bundleMap = new HashMap<String, NameVersion>();
	private transient String importVersion;

	public ManifestInfo(File maniFile) throws IOException {
		DumbManifestSearcher dumbMani = new DumbManifestSearcher(maniFile);
		Pos p = dumbMani.search("Bundle-SymbolicName");
		String content = dumbMani.getContent(p);
		symbolicName = content.trim();
		int semi = symbolicName.indexOf(';');
		if (semi >= 0) symbolicName = symbolicName.substring(0, semi);
		symbolicNamePos = new Pos(
				p.getOffset() + content.indexOf(symbolicName), symbolicName
						.length());
		// System.out.println("Bundle-SymbolicName: " + symbolicName);

		p = dumbMani.search("Bundle-Version");
		content = dumbMani.getContent(p);
		version = content.trim();
		versionPos = new Pos(p.getOffset() + content.indexOf(version), version
				.length());
		// System.out.println("Bundle-Version: " + version);

		bundleMap.put(symbolicName, new NameVersion(symbolicName, symbolicNamePos, version, versionPos));
		
		p = dumbMani.search("Import-Package");
		content = dumbMani.getContent(p);
		importMap = new HashMap<String, NameVersion>();
		if (content != null) {
			int offset = -1;
			for (StringTokenizer st = new StringTokenizer(content, " \t\r\n,"); st
					.hasMoreTokens();) {
				// the element
				String e = st.nextToken();
				offset = content.indexOf(e, offset + 1);
				Pos ePos = new Pos(p.getOffset() + offset, e.length());
				Pos vPos = null;
				String ver = null;
				int version = content.indexOf("version=\"", offset);
				if (version > 0 && version - offset < e.length()) {
					version += 9;
					int end = content.indexOf('"', version);
					if (end > 0) {
						vPos = new Pos(p.getOffset() + version, end - version);
						ver = dumbMani.getContent(vPos);
					}
				}
				semi = e.indexOf(';');
				if (semi > 0)
					e = e.substring(0, semi);

				NameVersion imp = new NameVersion(e, ePos, ver, vPos);
				importMap.put(e, imp);
				// System.out.println("Import-Package: " + e + " - " + ePos + " - " + ver + " - " + vPos);
			}
		}

		p = dumbMani.search("Export-Package");
		content = dumbMani.getContent(p);
		exportMap = new HashMap<String, Pos>();
		if (content != null) {
			int offset = -1;
			for (StringTokenizer st = new StringTokenizer(content, " \t\r\n,"); st
					.hasMoreTokens();) {
				String e = st.nextToken();
				semi = e.indexOf(';');
				if (semi > 0)
					e = e.substring(0, semi);
				offset = content.indexOf(e, offset + 1);
				Pos ePos = new Pos(p.getOffset() + offset, e.length());
				exportMap.put(e, ePos);
				// System.out.println("Export-Package: " + e);
			}
		}

		p = dumbMani.search("Require-Bundle");
		content = dumbMani.getContent(p);		
		if (content != null) {
			int offset = -1;
			for (StringTokenizer st = new StringTokenizer(content, " \t\r\n,"); st
					.hasMoreTokens();) {
				String e = st.nextToken();
				offset = content.indexOf(e, offset + 1);
				Pos ePos = new Pos(p.getOffset() + offset, e.length());
				Pos vPos = null;
				String ver = null;
				int version = content.indexOf("bundle-version=\"", offset);
				if (version > 0 && version - offset < e.length()) {
					version += 16;
					int end = content.indexOf('"', version);
					if (end > 0) {
						vPos = new Pos(p.getOffset() + version, end - version);
						ver = dumbMani.getContent(vPos);
					}
				}
				semi = e.indexOf(';');
				if (semi > 0)
					e = e.substring(0, semi);

				NameVersion imp = new NameVersion(e, ePos, ver, vPos);
				bundleMap.put(e, imp);
				// System.out.println("Require-Bundle: " + e + " - " + ePos + " - " + ver + " - " + vPos);
			}
		}
	}

	public String getVersion() {
		return version;
	}

	public String getImportVersion() {
		if (importVersion != null)
			return importVersion;
		int lastDot = 0;
		int i = 0;
		for (; i < version.length(); ++i) {
			char ch = version.charAt(i);
			if (ch == '.') {
				lastDot = i;
			}
			if (!Character.isDigit(ch))
				break;
		}
		if (lastDot == 0)
			lastDot = i;
		importVersion = version.substring(0, lastDot);
		return importVersion;
	}

	public Pos getVersionPos() {
		return versionPos;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public Pos getSymbolicNamePos() {
		return symbolicNamePos;
	}

  public Set<String> getReferences() {
    return this.bundleMap.keySet();
  }

  public Pos getPositions(String mod) {
    NameVersion nv = bundleMap.get(mod);
    if (nv == null)
      return null;
    Pos p = nv.getVersionPos();
    if (p != null) return p;
      
    p = nv.getNamePos();
    return new Pos(p.getEnd(), 0);
  }
}
