package de.bb.tools.bnm.plugin.eclipse;

import java.io.File;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.bb.io.IOUtils;
import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Log;
import de.bb.util.ByteRef;
import de.bb.util.MultiMap;
import de.bb.util.SingleMap;

public class ImportPluginsPlugin extends AbstractPlugin {

	private static boolean done;

	public void execute() throws Exception {

		// run only once.
		if (done)
			return;
		done = true;

		Log log = getLog();
		String eclipseDir = System.getProperty("eclipse.dir");
		log.debug("eclipse.dir=" + eclipseDir);

		File plugins = new File(eclipseDir, "plugins");
		if (!plugins.exists())
			throw new Exception(
					"not in a valid Eclipse directory given via -Declipse.dir=... - plugins folder does not exists. " + plugins.getAbsolutePath());

		// read all plugins and it's dependencies
		Map<String, String> dependencies = new MultiMap<>();
		Map<String, File> name2File = new SingleMap<>();
		for (File plugin : plugins.listFiles()) {
			byte data[];
			try {
				if (plugin.isDirectory()) {
					data = IOUtils.readFile(new File(plugin, "META-INF/MANIFEST.MF"));
				} else {
					ZipFile zf = new ZipFile(plugin);
					ZipEntry e = zf.getEntry("META-INF/MANIFEST.MF");
					data = new byte[(int)e.getSize()];
					IOUtils.readFully(zf.getInputStream(e), data);
					zf.close();
				}
				
				ByteRef br = new ByteRef(data);

				// read the name
				int sn = br.indexOf("Bundle-SymbolicName:");
				if (sn < 0)
					throw new Exception("no Bundle-SymbolicName");
				ByteRef bsn = br.substring(sn);
				bsn.nextWord(':');
				bsn = bsn.nextLine().trim();
				bsn = bsn.nextWord(';');
				
				name2File.put(bsn.toString(), plugin);
				
				// read the dependencies
				int rb = br.indexOf("Require-Bundle:");
				ByteRef brb = new ByteRef();
				if (rb > 0) {
					ByteRef b = br.substring(rb);
					b.nextWord(':');
					brb = brb.append(b.nextLine().trim());
					while (b.charAt(0) > 0 && b.charAt(0) <= ' ') {
						brb = brb.append(b.nextLine().trim());
					}
				}
				
				while (brb.length() > 0) {
					ByteRef dep = brb.nextWord(',').trim();
					
				}
				
				
			} catch (Exception ex) {
				log.info("skipping " + plugin + ": " + ex.getMessage());
			}
		}

	}
}
