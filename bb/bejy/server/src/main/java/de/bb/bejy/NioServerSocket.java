package de.bb.bejy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NioServerSocket implements ServerSocket {

    private ServerSocketChannel proxy;

    public NioServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        proxy = ServerSocketChannel.open();
        InetSocketAddress isa = new InetSocketAddress(bindAddr, port);
        proxy.socket().bind(isa);
    }

    public Socket accept() throws IOException {
        SocketChannel sc = proxy.accept();
        return new NioSocket(sc);
    }

    public void close() throws IOException {
        proxy.close();
    }

	public boolean isNIO() {
		return true;
	}

}
