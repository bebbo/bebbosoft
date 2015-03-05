/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
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
