package de.bb.bejy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class OldSocket implements Socket {

    private java.net.Socket proxy;

    public OldSocket(java.net.Socket socket) {
        this.proxy = socket;
    }

    public InetAddress getLocalAddress() {
        return proxy.getLocalAddress();
    }

    public void setSoTimeout(int timeout) throws IOException {
        proxy.setSoTimeout(timeout);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        proxy.setTcpNoDelay(on);
    }

    public InputStream getInputStream() throws IOException {
        return proxy.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return proxy.getOutputStream();
    }

    public InetAddress getInetAddress() {
        return proxy.getInetAddress();
    }

    public void close() throws IOException {
        proxy.close();
    }

    public boolean setBlocking(boolean b) throws IOException {
        return false;
    }

    public SelectionKey register(Selector socketSelector) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
}
