/******************************************************************************
 * Socket interface to support old/new IO.
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

	boolean isOpen();

}
