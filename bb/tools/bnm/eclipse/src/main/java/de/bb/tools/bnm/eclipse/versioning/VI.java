package de.bb.tools.bnm.eclipse.versioning;

import java.util.TreeSet;

public class VI {

	public String id;
	public String version;
	public String origVersion;
	public String bundleId;
	public String bundleVersion;
	public String origBundleVersion;
	TreeSet<String> versionList = new TreeSet<String>();

	public String get(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return id;
		case 1:
			return version;
		case 2:
			return bundleId;
		case 3:
			return bundleVersion;
		}
		return null;
	}

	public void addVersion(String version) {
		versionList.add(version);
	}

}
