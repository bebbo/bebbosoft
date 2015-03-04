/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/SIStream.java,v $
 * $Revision: 1.11 $
 * $Date: 2014/10/21 20:35:31 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Servlet input stream
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
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import de.bb.io.FastBufferedInputStream;
import de.bb.util.ByteRef;

final class SIStream extends ServletInputStream {
    private InputStream is;

    //private ByteRef buffer;
    byte[] data;

    int pos;

    long avail;

    SIStream(ByteRef buffer, long avail, FastBufferedInputStream fis) {
        this.is = fis;
        if (buffer.length() > avail) {
            data = buffer.substring(0, (int)avail).toByteArray();
            buffer.adjustLeft((int)avail);
        } else {
            data = buffer.toByteArray();
            buffer.adjustLeft(buffer.length());
        }
        this.avail = avail;
    }

    public int read() throws IOException {
        if (avail <= 0)
            return -1;
        --avail;

        //    int r = buffer.removeLeft();
        //    if (r >= 0)
        //      return r;
        if (pos < data.length)
            return data[pos++] & 0xff;

        int there = is.available();
        if (there > 0) {
            if (there > avail)
                there = (int)avail + 1;
            data = new byte[there];
            is.read(data);
            pos = 1;
            return data[0] & 0xff;
        }

        return is.read();
    }

    public int read(byte b[], int s, int l) throws IOException {
        if (l == 0)
            return 0;
        if (avail <= 0)
            return -1;
        if (l > avail)
            l = (int)avail;

        int n2 = data.length - pos;
        if (n2 > 0) {
            if (n2 > l)
                n2 = l;
            System.arraycopy(data, pos, b, s, n2);
            pos += n2;
            avail -= n2;
            return n2;
        }

        /*    if (buffer.length() > 0)
            {
              int n = buffer.copy(b, s, l);
              buffer.adjustLeft(n);
              avail -= n;
              return n;
            }
         */
        int nn = is.read(b, s, l);
        if (nn > 0)
            avail -= nn;
        return nn;
    }

    /**
     * Returns the count of available bytes, limited by the defined limit.
     */
    public int available() throws IOException {
        //    int a = is.available() + buffer.length();
        int a = is.available() + data.length - pos;
        return a < avail ? a : (int)avail;
    }

    public long skip(long n) throws IOException {

        if (n <= 0)
            return 0;

        long remaining = n;
        int size = (int) Math.min(0x2000, remaining);
        byte[] skipBuffer = new byte[size];
        while (remaining > 0) {
            int read = read(skipBuffer, 0, (int) Math.min(size, remaining));
            if (read < 0)
                break;

            if (read == 0) {
                int b = read();
                if (b < 0)
                    break;
                read = 1;
            }

            remaining -= read;
        }

        return n - remaining;
    }

    @Override
    public boolean isFinished() {
        return avail == 0;
    }

    @Override
    public boolean isReady() {
        try {
            return available() > 0;
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public void setReadListener(ReadListener arg0) {
        // TODO Auto-generated method stub
    }

}

/******************************************************************************
 * $Log: SIStream.java,v $
 * Revision 1.11  2014/10/21 20:35:31  bebbo
 * @B implemented own skip method to avoid endless loops (bug in java InputStream?)
 * Revision 1.10 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 *
 *     Revision 1.9 2009/06/15 08:36:26 bebbo
 * @B B-REMOTEHOST is used correctly
 * @I using BufferedStreams where convenient
 *
 *    Revision 1.8 2008/03/13 17:22:51 bebbo
 * @O some speedup in method read()
 *
 *    Revision 1.7 2008/01/17 17:33:25 bebbo
 * @O optimizations for better performance
 *
 *    Revision 1.6 2007/01/18 21:49:32 bebbo
 * @B fixed EOS handling --> -1 instead of 0
 *
 *    Revision 1.5 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 *
 *    Revision 1.4 2002/03/04 20:41:00 franke
 * @B limited the available bytes to that bytes from current POST request.
 *
 *    Revision 1.3 2001/03/29 07:10:40 bebbo
 * @R no longer public classes
 *
 *    Revision 1.2 2001/03/27 19:47:53 bebbo
 * @I automatic use of already read data (in br)
 *
 *    Revision 1.1 2001/03/20 18:34:07 bebbo
 * @N enhanced functionality
 * @N more functions for Servlet API
 * @B fixes in filehandler
 * @N first working CGI
 *
 *    Revision 1.1 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 *
 *****************************************************************************/
