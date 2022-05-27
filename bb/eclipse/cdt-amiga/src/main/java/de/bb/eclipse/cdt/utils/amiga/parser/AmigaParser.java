package de.bb.eclipse.cdt.utils.amiga.parser;

import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.IBinaryParser;
import org.eclipse.core.runtime.IPath;

public class AmigaParser extends AbstractCExtension implements IBinaryParser {

	public AmigaParser() {
		org.eclipse.cdt.dsf.gdb.internal.GdbDebugOptions.DEBUG = true;
	}

	public IBinaryFile getBinary(final IPath path) throws IOException {
		return getBinary(null, path);
	}

	public IBinaryFile getBinary(final byte[] array, final IPath path) throws IOException {
		if (isBinary(array, path))
			return new AmigaBinary(this, array, path);
		return null;
	}

	public String getFormat() {
		return "Amiga";
	}

	public int getHintBufferSize() {
		return 8;
	}

	public boolean isBinary(byte[] array, final IPath path) {
		if (array == null || array.length <= 7) {
			array = new byte[8];
			try {
				final FileInputStream fis = new FileInputStream(path.toFile());
				fis.read(array);
				fis.close();
			} catch (final Exception e) {
				return false;
			}
		}

		final boolean is = array.length > 7 && array[0] == 0 && array[1] == 0 && array[2] == 3
				&& (array[3] & 0xff) == 0xf3;

		System.out.println("checking " + path.toFile() + " -> " + is);

		return is;
	}

}
