/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Protocol.java,v $
 * $Revision: 1.21 $
 * $Date: 2014/06/23 19:02:58 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * protocol base class
 *
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    1. Every product and solution using this software, must be free
      of any charge. If the software is used by a client part, the
      server part must also be free and vice versa.

    2. Each redistribution must retain the copyright notice, and
      this list of conditions and the following disclaimer.

    3. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.

    4. All advertising materials mentioning features or use of this
      software must display the following acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

    5. Redistributions of any form whatsoever must retain the following
      acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
  DISCLAIMER OF WARRANTY

  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.

 ******************************************************************************
  LIMITATION OF LIABILITY

  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
  COPYRIGHT

  (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.bb.io.FastBufferedInputStream;
import de.bb.io.FastBufferedOutputStream;
import de.bb.security.Ssl3Server;
import de.bb.util.ByteRef;

/**
 * <p>
 * base class for all protocols used in BEJY.
 * </p>
 * <p>
 * provides some usefull functions to handle incoming data
 * </p>
 * 
 * @author Stefan Bebbo Franke
 */
public abstract class Protocol {

	/**
	 * The associated socket.
	 */
	Socket socket;

	Long expiresAt;

	/**
	 * the output stream to write to.
	 */
	// streams
	protected OutputStream os;

	/**
	 * the input stream to read from.
	 */
	protected InputStream is;

	/**
	 * reference to this protocol's factory.
	 */
	//
	protected Factory factory;

	/**
	 * reference to this protocol's server (thread).
	 */
	protected Server server;

	/**
	 * address ot the other side.
	 */
	// others address
	protected String remoteAddress;

	protected boolean isSecure;

	/**
	 * the only constructor.
	 * 
	 * @param f
	 *            the Factory object which creates this object
	 */
	protected Protocol(Factory f) {
		factory = f;
	}

	/**
	 * contains the protocol main loop.
	 * 
	 * @return
	 * 		<li>true when the established connection is reused</li>
	 *         <li>false when a new connection must be established</li>
	 *         <p>
	 *         in both cases the server object and the protocol object is reused
	 *         </p>
	 * @exception java.lang.Exception
	 *                indicates that even the server object must be discarded
	 * @exception java.lang.Exception
	 */
	protected abstract boolean doit() throws Exception;

	/**
	 * checks whether current streams are still usable.
	 * 
	 * @return true if streams are usable
	 */
	protected boolean goodStream() {
		return is != null && os != null;
	}

	/**
	 * internal function to assign a Server thread to this protocol object.
	 * 
	 * @param s
	 *            a server object
	 */
	void setServer(Server s) {
		server = s;
	}

	/**
	 * assigns an input / output stream pair to this protocol.<br>
	 * also the remote address is set
	 * 
	 * @param _is
	 *            the assigned input stream
	 * @param _os
	 *            the assigned output stream
	 * @param remote
	 *            remote IP address
	 */

	protected void setStreams(InputStream _is, OutputStream _os, String remote) {
		if (_is == null) {
			is = null;
			os = null;
		} else {
			is = new FastBufferedInputStream(_is, 0x2000);
			os = new FastBufferedOutputStream(_os, 0x4000);
		}
		remoteAddress = remote;
	}

	/**
	 * a callback function which is called before the server starts to listen
	 * for incoming data.<br>
	 * Necessary when the server must send data before the clients starts
	 * sending. E.g. SMTP or POP3 protocols send first data to the client,
	 * before they start to read data.
	 * 
	 * @return true on success.
	 * @exception java.lang.Exception
	 */
	protected boolean trigger() throws Exception {
		return true;
	}

	/**
	 * wrapper function which calls doit() and flushes/closes streams when
	 * necessary.
	 * 
	 * @return
	 * 		<li>true when the established connection is reused</li>
	 *         <li>false when a new connection must be established</li>
	 *         <p>
	 *         in both cases the server object and the protocol object is reused
	 *         </p>
	 * @exception java.lang.Exception
	 *                indicates that even the server object must be discarded
	 */
	boolean work() throws Exception {
		if (os == null)
			return false;
		try {
			return doit();
		} finally {
			os.flush();
		}
	}

	/**
	 * read the first available bytes into a ByteArray object.
	 * 
	 * @return returns a ByteArray containing the first chunk of read data or
	 *         null at EOS
	 * @throws IOException
	 */
	protected ByteRef readFirst() throws IOException {
		int len = is.available();
		int firstByte = -1;

		if (len == 0) {
			firstByte = is.read();
			++len;
		}
		byte b[] = new byte[len];
		int pos = 0;
		if (firstByte >= 0)
			b[pos++] = (byte) firstByte;

		if (len > pos)
			len = pos + is.read(b, pos, len - pos);
		return new ByteRef(b, 0, len);
	}

	/**
	 * read the next line into a ByteArray using a 2nd ByteArray as buffer.<br>
	 * If there is no complete line in the provided buffer, the next chunk of
	 * data is read from the input stream.<br>
	 * 
	 * @param br
	 *            the read buffer which contains already read data
	 * @return returns a ByteArray referring to the next line of data or null at
	 *         EOS
	 */
	public final ByteRef readLine(ByteRef br) {
		return ByteRef.readLine(br, is);
	}

	/**
	 * Method shutdown.
	 */
	protected void shutdown() {
	}

	/**
	 * Returns the InputStream.
	 * 
	 * @return InputStream
	 */
	protected InputStream getIs() {
		return is;
	}

	/**
	 * Returns the OutputStream.
	 * 
	 * @return OutputStream
	 */
	protected OutputStream getOs() {
		return os;
	}

	/**
	 * The remote address.
	 * 
	 * @return the remote address.
	 */
	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String ra) {
		this.remoteAddress = ra;
	}

	public void startTLS(Ssl3Server s3) throws IOException {
		s3.listen(getIs(), getOs());
		setStreams(s3.getInputStream(), s3.getOutputStream(), remoteAddress);
		isSecure = true;
	}

}

/******************************************************************************
 * $Log: Protocol.java,v $ Revision 1.21 2014/06/23 19:02:58 bebbo
 * 
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2
 *    with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 *    Revision 1.20 2013/06/18 13:23:53 bebbo
 * @I preparations to use nio sockets
 * @V 1.5.1.68 Revision 1.19 2010/07/08 18:14:55 bebbo
 * 
 * @N added a method to override the remote server name
 * 
 *    Revision 1.18 2008/03/13 20:43:00 bebbo
 * @I removed redundand code
 * 
 *    Revision 1.17 2008/01/17 17:22:30 bebbo
 * @R made some methods more visible
 * 
 *    Revision 1.16 2004/12/13 15:26:42 bebbo
 * @F reformatted
 * 
 *    Revision 1.15 2003/07/30 10:10:38 bebbo
 * @R enahanced information in admin interface
 * 
 *    Revision 1.14 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *    Revision 1.13 2003/02/25 07:01:59 bebbo
 * @R protocols are usinf now BufferedOutputStreams
 * 
 *    Revision 1.12 2002/11/22 21:20:10 bebbo
 * @R added shutdown() method to Protocol and invokin it
 * 
 *    Revision 1.11 2002/05/16 15:19:48 franke
 * @C CVS
 * 
 *    Revision 1.10 2002/03/04 20:40:20 franke
 * @B fixed a (Windows JVM 1.3 ?) bug: is.read(...) does not always read as many
 *    bytes as available tells!
 * 
 *    Revision 1.9 2002/01/22 08:54:22 franke
 * @I removde the closing of the streams
 * 
 *    Revision 1.8 2002/01/19 15:48:25 franke
 * @R trigger() now returns true on success, false on error
 * 
 *    Revision 1.7 2001/09/15 08:45:31 bebbo @ comments
 * 
 *    Revision 1.6 2001/08/24 08:24:16 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String
 *    class
 * 
 *    Revision 1.5 2001/03/27 19:46:52 bebbo
 * @I now knows its Factory
 * 
 *    Revision 1.4 2001/03/20 18:33:08 bebbo
 * @R new functions
 * @B fix in readfirst
 * 
 *    Revision 1.3 2000/12/30 09:01:42 bebbo
 * @R protocols now are throwing Eceptions to indicate that a thread should end
 * 
 *    Revision 1.2 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *    Revision 1.1 2000/11/10 18:13:26 bebbo
 * @N new (uncomplete stuff)
 * 
 *****************************************************************************/
