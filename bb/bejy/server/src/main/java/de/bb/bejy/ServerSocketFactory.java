/******************************************************************************
 * Simple factory to use old/new IO.
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
package de.bb.bejy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerSocketFactory {

    private static boolean useNio;

	public static void setUseNio(boolean useNio) {
		ServerSocketFactory.useNio = useNio;
	}

	public static ServerSocket getImpl(int port, int backlog, InetAddress bindAddr) throws IOException {
        if (useNio)
        	return new NioServerSocket(port, backlog, bindAddr);
        return new OldServerSocket(port, backlog, bindAddr);
    }

    public static Socket getSocket(String destination, int port) throws UnknownHostException, IOException {

        return new OldSocket(new java.net.Socket(destination, port));
    }

}
