package de.bb.tools.bnm.plugin.dependency;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;

abstract class AbstractDepsPlugin extends AbstractPlugin {

    final static String SCOPES[] = { "COMPILE", "PROVIDED", "RUNTIME",
            "TEST", "SYSTEM" };

    @Config("outputFile")
    String outputFile;

    Log log;

    FileOutputStream fos;

    @Config("includeScope")
    String includeScope;
    
    
	@Override
	public void execute() throws Exception {
		log = getLog();

		fos = null;
		if (outputFile != null) {
			File dir = new File(outputFile);
			dir = dir.getParentFile();
			if (!dir.exists())
				dir.mkdirs();
			fos = new FileOutputStream(outputFile);
		}
	}



	protected void write(String msg) throws IOException {
	    if (fos != null) {
	        fos.write(msg.getBytes());
	        fos.write(0xd);
	        fos.write(0xa);
	    } else {
	        log.info(msg);
	    }
	}

}
