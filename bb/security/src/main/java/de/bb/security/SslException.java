package de.bb.security;

import java.io.IOException;

import de.bb.security.Ssl3Server.SID;

public class SslException extends IOException {
    private static final long serialVersionUID = 1L;

    public SslException(SID clientSessionId, String msg) {
        super(msg);
        Ssl3Config.dropSession(clientSessionId);
        // avoid timing attacks
        long delay = 100 - System.currentTimeMillis() % 100;
        try {
            Thread.sleep(delay);
        } catch (Exception ex) {}
    }

}
