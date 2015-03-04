/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/SOStream.java,v $
 * $Revision: 1.22 $
 * $Date: 2013/11/23 10:36:19 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Servlet output stream
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

package de.bb.bejy.http;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import de.bb.io.FastByteArrayOutputStream;

final class SOStream extends ServletOutputStream {
    HttpResponse response;

    OutputStream os;

    int bufferSize;

    FastByteArrayOutputStream bos;

    boolean committed, closed;

    long written;

    // set to false to disable chunk mode
    private boolean chunkMode = true; 
    
    // true if chunk mode is used
    private boolean chunked;

    SOStream(HttpResponse _sr, OutputStream _os, int sz, FastByteArrayOutputStream fbos, boolean useChunked) {
        response = _sr;
        os = _os;
        if (fbos.getBufferSize() > sz)
            sz = fbos.getBufferSize();
        
        bufferSize = sz;
        fbos.reset();
        bos = fbos;
        committed = closed = false;
        this.chunkMode = useChunked;
    }

    public void write(int c) throws IOException {
        if (closed)
            return;
        if (written >= response.contentLength && response.contentLength >= 0)
            return;

        ++written;
        if (bufferSize <= 0) {
            flush();
            os.write(c);
            return;
        }

        bos.write(c);
        if (bos.size() >= bufferSize)
            flush();
        //throw new IOException("buffer size exceeded");
    }

    public void write(byte b[], int offset, int length) throws IOException {
        if (closed)
            return;
        if (response.contentLength >= 0) {
            if (written >= response.contentLength)
                return;
            if (length + written > response.contentLength)
                length = (int)(response.contentLength - written);
        }

        written += length;
        while (bos.size() + length > bufferSize) {
            int toWrite = bufferSize - bos.size();
            bos.write(b, offset, toWrite);
            flush();
            offset += toWrite;
            length -= toWrite;
        }
        bos.write(b, offset, length);
    }

    public void flush() throws IOException {
        flush(chunkMode);
    }

    private void flush(boolean enableChunked) throws IOException {
        if (!committed) {
            if (enableChunked) {
                response.addHeader("Transfer-Encoding", "chunked");
                chunked = true;
            } else if (response.contentLength < 0) {
                response.contentLength = bos.size();
            }
            response.writeHeader();
            committed = true;
        }
        if (bos.size() > 0) {
            if (chunked) {
                final String sz = Integer.toHexString(bos.size()) + "\r\n";
                os.write(sz.getBytes());
            }
            bos.writeTo(os);
            bos.reset();
            if (chunked) {
                os.write(0xd);
                os.write(0xa);
            }
        }
        os.flush();
    }

    boolean isCommitted() {
        return committed;
    }

    void reset() throws IllegalStateException {
        if (committed)
            throw new IllegalStateException();
        bos.reset();
        chunkMode = true;
    }

    void setBufferSize(int sz) throws IllegalStateException {
        if (sz < bos.size() || committed)
            throw new IllegalStateException();
        bufferSize = sz;
    }

    int size() {
        return bos.size();
    }

    public void close() throws IOException {
        if (closed)
            return;
        flush(false);
        if (chunked) {
            os.write("0\r\n\r\n".getBytes());
            os.flush();
        }
        closed = true;
    }

    void open() {
        closed = false;
    }

    long getWritten() {
        return written;
    }
    
    public void setChunkMode(boolean on) {
        if (committed)
            throw new IllegalStateException();
        chunkMode = on;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener arg0) {
        // TODO Auto-generated method stub
    }
}
/******************************************************************************
 * $Log: SOStream.java,v $
 * Revision 1.22  2013/11/23 10:36:19  bebbo
 * @R contentLength supports long values now
 *
 * Revision 1.21  2013/07/23 07:01:39  bebbo
 * @B fixed chunk mode
 * @I use underlying buffer size
 * @N add method to disable chunk mode
 *
 * Revision 1.20  2013/05/17 10:31:24  bebbo
 * @N added support to send chunked data
 * Revision 1.19 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.18 2009/06/15 08:36:26 bebbo
 * @B B-REMOTEHOST is used correctly
 * @I using BufferedStreams where convenient
 * 
 *    Revision 1.17 2008/03/13 20:43:38 bebbo
 * @O performance optimizations
 * 
 *    Revision 1.16 2008/01/17 17:33:33 bebbo
 * @O optimizations for better performance
 * 
 *    Revision 1.15 2006/02/06 09:16:44 bebbo
 * @I cleanup
 * 
 *    Revision 1.14 2004/12/13 15:36:37 bebbo
 * @I preps for chunked support
 * 
 *    Revision 1.13 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.12 2003/04/07 15:29:39 bebbo
 * @B fixed to set Content-Length when it was not used (-1), no -1 remains
 * 
 *    Revision 1.11 2003/04/07 07:59:47 bebbo
 * @B content length is updated with correct content-length, when it was set with a wrong value
 * 
 *    Revision 1.10 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.9 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.8 2002/04/02 13:02:36 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.7 2002/03/30 15:49:42 franke
 * @B many fixes.
 * 
 *    Revision 1.6 2002/03/10 20:06:36 bebbo
 * @I redesigned buffer handling: on large data now a fallback to HTTP/1.0 is used and no ContentLength is specified.
 * 
 *    Revision 1.5 2001/05/07 16:18:03 bebbo
 * @B fixed behaviour for long file donwload
 * 
 *    Revision 1.4 2001/03/29 18:25:43 bebbo
 * @I added internal buffer
 * 
 *    Revision 1.3 2001/03/29 07:10:40 bebbo
 * @R no longer public classes
 * 
 *    Revision 1.2 2001/03/20 18:34:07 bebbo
 * @N enhanced functionality
 * @N more functions for Servlet API
 * @B fixes in filehandler
 * @N first working CGI
 * 
 *    Revision 1.1 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 * 
 *****************************************************************************/
