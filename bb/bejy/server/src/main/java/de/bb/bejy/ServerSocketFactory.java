package de.bb.bejy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerSocketFactory {

    public static ServerSocket getImpl(int port, int backlog, InetAddress bindAddr) throws IOException {
        if (true)
            return new OldServerSocket(port, backlog, bindAddr);
        return new NioServerSocket(port, backlog, bindAddr);
    }

    public static Socket getSocket(String destination, int port) throws UnknownHostException, IOException {

        return new OldSocket(new java.net.Socket(destination, port));
    }

}
