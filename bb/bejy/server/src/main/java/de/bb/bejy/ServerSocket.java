package de.bb.bejy;

import java.io.IOException;

public interface ServerSocket {

    Socket accept() throws IOException;

    void close() throws IOException;

    boolean isNIO();
}
