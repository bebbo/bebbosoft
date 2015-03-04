package de.bb.bejy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

    private final class IS extends InputStream {
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
        socketChannel.configureBlocking(block);
        
        if (block && getInputStream().available() == 0)
            is.fill();
        return true;
    }

    public SelectionKey register(Selector socketSelector) throws IOException {
        return socketChannel.register(socketSelector, SelectionKey.OP_READ);
    }
}
