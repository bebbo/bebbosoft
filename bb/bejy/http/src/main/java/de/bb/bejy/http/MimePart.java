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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.http.Part;

import de.bb.io.FastBufferedInputStream;
import de.bb.io.IOUtils;
import de.bb.util.ByteRef;
import de.bb.util.MimeFile.Info;
import de.bb.util.Pair;

public class MimePart implements Part {

    private File partsFile;
    private byte[] partsData;
    private String contentType;
    private String name;
    private String fileName;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private Info mimeInfo;


    public MimePart(File partsFile, byte[] partsData, String cType, Info mimeInfo) {
        this.partsFile = partsFile;
        this.partsData = partsData;
        this.contentType = cType;
        this.mimeInfo = mimeInfo;

        for (final Pair<String, String> header : mimeInfo.header) {
            final String headerName = header.getFirst();
            final String headerValue = header.getSecond();
            this.headers.put(headerName.toLowerCase(), headerValue);
            if ("Content-Disposition".equalsIgnoreCase(headerName)) {
                for (final StringTokenizer st = new StringTokenizer(headerValue, "; "); st.hasMoreTokens();) {
                    String value = st.nextToken();
                    int eq = value.indexOf('=');
                    if (eq > 0) {
                        final String key = value.substring(0, eq);
                        value = value.substring(eq + 1);
                        if (value.charAt(0) == '"') {
                            value = value.substring(1, value.length() - 1);
                        }
                        if ("name".equalsIgnoreCase(key)) {
                            name = value;
                        } else if ("filename".equalsIgnoreCase(key)) {
                            fileName = value;
                        }
                    }
                }
            }
        }
    }

    public void delete() throws IOException {
    }

    public String getContentType() {
        return contentType;
    }

    public String getHeader(String key) {
        return headers.get(key.toLowerCase());
    }

    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    public Collection<String> getHeaders(String key) {
        ArrayList<String> al = new ArrayList<String>();
        String value = headers.get(key.toLowerCase());
        if (value != null)
            al.add(value);
        return al;
    }

    public InputStream getInputStream() throws IOException {
        if (partsFile != null) {
            FileInputStream fis = new FileInputStream(partsFile);
            fis.skip(mimeInfo.bBegin);
            return new SIStream(new ByteRef(), getSize(), new FastBufferedInputStream(fis, 0x8000));
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(partsData);
        bis.skip(mimeInfo.bBegin);
        return bis;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return mimeInfo.end - mimeInfo.bBegin;
    }

    public String getSubmittedFileName() {
        return fileName;
    }

    public void write(String outFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outFile);
        try {
            IOUtils.copy(getInputStream(), fos, getSize());
            fos.close();
        } finally {
            fos.close();
        }
    }

}
