package de.bb.eclipse.cdt.utils.amiga.parser;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.cdt.core.IAddressFactory;
import org.eclipse.cdt.core.IBinaryParser.IBinaryExecutable;
import org.eclipse.cdt.core.IBinaryParser.IBinaryFile;
import org.eclipse.cdt.core.IBinaryParser.ISymbol;
import org.eclipse.cdt.utils.Addr32;
import org.eclipse.cdt.utils.Addr32Factory;
import org.eclipse.cdt.utils.BinaryObjectAdapter;
import org.eclipse.cdt.utils.Symbol;
import org.eclipse.core.runtime.IPath;

public class AmigaBinary extends BinaryObjectAdapter implements IBinaryExecutable {

	static enum HunkType {
		Text, Data, Bss, Debug, Reloc32, End, Dwarf;
	}

	static class Hunk {
		int size;
		HunkType type;
		byte[] data;
	}

	private final BinaryObjectInfo boi;
	private final ArrayList<String> strings = new ArrayList<String>();
	private final ArrayList<Hunk> hunks = new ArrayList<Hunk>();
	private Hunk currentHunk;
	private final ArrayList<ISymbol> symbols = new ArrayList<ISymbol>();

	public AmigaBinary(final AmigaParser amigaParser, final byte[] array, final IPath path) {
		super(amigaParser, path, IBinaryFile.EXECUTABLE);

		boi = new BinaryObjectInfo();
		boi.cpu = "MC68000";
		boi.soname = path.lastSegment();
		boi.bss = 0;
		boi.data = 0;
		boi.text = 0;

		parseFile(path);
	}

	private void parseFile(final IPath path) {
		DataInputStream dos;
		try {
			dos = new DataInputStream(new FileInputStream(path.toFile()));

			readHeader(dos);

			while (dos.available() > 0) {
				final Hunk hunk = readHunk(dos);
				hunks.add(hunk);
			}

			dos.close();
		} catch (final Exception e) {
		}
	}

	private Hunk readHunk(final DataInputStream dos) throws IOException {
		final int hunkType = dos.readInt();
		final Hunk hunk = new Hunk();
		switch (hunkType) {
		case 0x3e9:
			int hunkLen = dos.readInt();
			boi.text = hunkLen * 4;
			hunk.type = HunkType.Text;
			dos.skip(hunkLen * 4);
			currentHunk = hunk;
			break;
		case 0x3ea:
			hunkLen = dos.readInt();
			boi.data = hunkLen * 4;
			hunk.type = HunkType.Data;
			dos.skip(hunkLen * 4);
			currentHunk = hunk;
			break;
		case 0x3eb:
			hunkLen = dos.readInt();
			if ((hunkLen & 0xc0000000) == 0xc0000000)
				dos.readInt();
			hunkLen &= 0x3fffffff;
			boi.bss = hunkLen * 4;
			hunk.type = HunkType.Bss;
			currentHunk = hunk;
			break;
		case 0x3ec:
			hunk.type = HunkType.Reloc32;
			readReloc32(hunk, dos);
			break;
		case 0x3f2:
			// end hunk
			hunk.type = HunkType.End;
			break;
		case 0x3f0:
			hunk.type = HunkType.Debug;
			readSymbols(dos);
			break;
		case 0x3f1:
			hunk.type = HunkType.Dwarf;
			hunkLen = 4 * dos.readInt();
			dos.skip(hunkLen);
			break;
		default:
			System.out.println("x");
		}
		return hunk;
	}

	private void readSymbols(final DataInputStream dos) throws IOException {
		byte b[] = new byte[1024];
		for (;;) {
			int count = dos.readInt();
			if (count == 0)
				break;

			count *= 4;

			if (count > b.length)
				b = new byte[count];

			dos.read(b, 0, count);
			while (b[count - 1] == 0)
				--count;
			final String name = new String(b, 0, 1, count - 1);

			final int offset = dos.readInt();
			System.out.println(name + " " + offset);

			final Symbol y = new Symbol(this, name, currentHunk.type.ordinal(), new Addr32(offset), 0L);
			symbols.add(y);
		}
	}

	private void readReloc32(final Hunk hunk, final DataInputStream dos) throws IOException {
		for (;;) {
			final int count = dos.readInt();
			if (count == 0)
				break;

			final int hunkno = dos.readInt();
			for (int i = 0; i < count; ++i) {
				final int reloc = dos.readInt();
			}
		}
	}

	private void readHeader(final DataInputStream dos) throws IOException {
		// already checked
		dos.skip(4);

		readNames(dos);

		final int numHunks = dos.readInt();
		final int firstHunk = dos.readInt();
		final int lastHunk = dos.readInt();

		for (int i = 0; i < numHunks; ++i) {
			final int hsize = dos.readInt();
		}
	}

	private void readNames(final DataInputStream dos) throws IOException {
		byte b[] = new byte[1024];
		for (;;) {
			int count = dos.readInt();
			if (count == 0)
				break;

			count *= 4;

			if (count > b.length)
				b = new byte[count];

			dos.read(b, 0, count);
			while (b[count - 1] == 0)
				--count;
			final String s = new String(b, 0, 0, count);
			strings.add(s);
		}
	}

	public ISymbol[] getSymbols() {
		return symbols.toArray(new ISymbol[symbols.size()]);
	}

	public IAddressFactory getAddressFactory() {
		return new Addr32Factory();
	}

	protected BinaryObjectInfo getBinaryObjectInfo() {
		return boi;
	}

}
