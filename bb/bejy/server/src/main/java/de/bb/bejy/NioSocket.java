package de.bb.bejy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
/******************************************************************************
 * New IO Socket wrapper.
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2016.
 *
  * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class NioSocket implements Socket {

	private SocketChannel socketChannel;
	private IS is;
	private OutputStream os;

	public NioSocket(SocketChannel sc) {
		this.socketChannel = sc;
	}

	public InetAddress getLocalAddress() throws IOException {
		return InetAddress.getByName(socketChannel.getLocalAddress().toString());
	}

	public void setSoTimeout(int timeout) throws IOException {
		this.socketChannel.socket().setSoTimeout(timeout);
	}

	public void setTcpNoDelay(boolean b) throws IOException {
		socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
	}

	public InputStream getInputStream() throws IOException {
		if (is == null)
			is = new IS();
		return is;
	}

	public OutputStream getOutputStream() throws IOException {
		if (os == null)
			os = new OS();
		return os;
	}

	public InetAddress getInetAddress() throws IOException {
		return ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress();
	}

	public void close() throws IOException {
		socketChannel.close();
		socketChannel.socket().close();
	}

	private final class OS extends OutputStream {
		private ByteBuffer bb = ByteBuffer.allocateDirect(0x2000);

		public void write(byte[] b, int off, int len) throws IOException {
			while (bb.remaining() < len) {
				int chunk = bb.remaining();
				bb.put(b, off, chunk);
				flush();
				off += chunk;
				len -= chunk;
			}
			bb.put(b, off, len);
		}

		public void write(int b) throws IOException {
			if (bb.remaining() == 0)
				flush();
			bb.put((byte) b);
		}

		public void flush() throws IOException {
			bb.flip();
			socketChannel.write(bb);
			bb.clear();
		}

		public void close() throws IOException {
			flush();
			socketChannel.close();
		}
	}

	final class IS extends InputStream {
		private ByteBuffer bb = ByteBuffer.allocateDirect(0x1000);

		IS() {
			bb.position(bb.limit());
		}

		public int read() throws IOException {
			if (bb.remaining() == 0) {
				bb.clear();
				int r = socketChannel.read(bb);
				bb.flip();
				if (r < 0)
					return r;
			}
			return bb.get() & 0xff;
		}

		public int read(byte[] b, int off, int len) {
			if (bb.remaining() < len)
				len = bb.remaining();
			if (len > 0)
				bb.get(b, off, len);
			return len;
		}

		public int available() throws IOException {
			// socketChannel.configureBlocking(false);
			// fill();
			// socketChannel.configureBlocking(true);
			return bb.remaining();
		}

		public void fill() throws IOException {
			if (bb.remaining() > 0)
				return;

			bb.clear();
			int r = socketChannel.read(bb);
			if (r < 0)
				socketChannel.close();
			bb.flip();
		}
	}

	public boolean setBlocking(boolean block) throws IOException {
		if (!socketChannel.isOpen())
			throw new IOException();

//		if (block && getInputStream().available() == 0)
//			is.fill();

		socketChannel.configureBlocking(block);

		if (!block && getInputStream().available() == 0)
			is.fill();

		return true;
	}

	public SelectionKey register(Selector socketSelector) throws IOException {
		return socketChannel.register(socketSelector, SelectionKey.OP_READ);
	}

	public boolean isOpen() {
		return socketChannel.isOpen();
	}
}
