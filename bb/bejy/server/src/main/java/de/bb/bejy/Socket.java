package de.bb.bejy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface Socket {

    InetAddress getLocalAddress() throws IOException;

    void setSoTimeout(int timeout) throws IOException;

    void setTcpNoDelay(boolean b) throws IOException;

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    InetAddress getInetAddress() throws IOException;

    void close() throws IOException;

    boolean setBlocking(boolean b) throws IOException;

    SelectionKey register(Selector socketSelector) throws IOException;

}
