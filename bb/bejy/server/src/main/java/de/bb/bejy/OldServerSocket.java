package de.bb.bejy;

import java.io.IOException;
import java.net.InetAddress;

public class OldServerSocket implements ServerSocket {

    private java.net.ServerSocket proxy;

    public OldServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        proxy = new java.net.ServerSocket(port, backlog, bindAddr);
    }

    public Socket accept() throws IOException {
        return new OldSocket(proxy.accept());
    }

    public void close() throws IOException {
        proxy.close();
    }

	public boolean isNIO() {
		return false;
	}

}
