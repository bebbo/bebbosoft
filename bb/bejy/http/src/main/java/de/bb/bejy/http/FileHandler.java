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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;

import de.bb.util.DateFormat;
import de.bb.util.LogFile;
import de.bb.util.Pair;

public class FileHandler extends HttpHandler {
    private final static boolean DEBUG = HttpProtocol.DEBUG;
    private final static String PROPERTIES[][] = { { "default", "the default welcome files", "index.html" },
            { "h404", "404 error handler page" },
            { "compress", "the extensions of files to compress", "css, htm, html, js" } };

    private String defIndex;
    private String h404;
    private File workDir;
    private HashSet<String> compress = new HashSet<String>();

    public FileHandler() {
        init("FileHandler", PROPERTIES);
    }

    public void activate(LogFile logFile) throws Exception {
        defIndex = getProperty("default", "index.html");
        h404 = getProperty("h404");
        String scompress = getProperty("compress");

        for (final StringTokenizer st = new StringTokenizer(scompress, " ,.\t\r\n"); st.hasMoreElements();) {
            compress.add(st.nextToken().toLowerCase());
        }
        super.activate(logFile);
    }

    public void service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response) {
        HttpRequest hp0 = RequestDispatcher.dereference(request);
        HttpResponse sr = RequestDispatcher.dereference(response);

        if (hp0.method.equals("OPTIONS")) {
            sendOptions(sr);
            return;
        }

        HttpServletRequest hreq = hp0;
        if (request instanceof HttpServletRequest)
            hreq = (HttpServletRequest) request;

        String servletPath = hreq.getServletPath();
        String pathInfo = hreq.getPathInfo();
        if (pathInfo != null)
            servletPath += pathInfo;
        String path = hp0.context.getRealPath2(servletPath);
        if (DEBUG)
            System.out.println("filehandler: " + path);

        RandomAccessFile raf = null;
        try {
            File f = new File(path);

            if (!f.exists()) {
                // redirect to CgiHandler if one is found to handle 404.
                if (h404 != null && h404.length() > 0 && !h404.startsWith(servletPath)) {
                    String newPath = h404 + servletPath;
                    RequestDispatcher rd = (RequestDispatcher) hp0.context.getRequestDispatcher(newPath);
                    if (rd != null && rd.sHandler instanceof CgiHandler) {
                        rd.sHandler.service(request, response);
                        return;
                    }
                }
                sr.setStatus(404);
                return;
            }

            if (f.isDirectory()) {
                if (!servletPath.endsWith("/")) {
                    new HttpHandler.H302Handler(hContext).service(request, response);
                    return;
                }
                if (defIndex == null) {
                    sr.setStatus(403);
                    return;
                }

                String root = hp0.getServletPath();

                for (StringTokenizer st = new StringTokenizer(defIndex, " ,\r\n\t\f"); st.hasMoreTokens();) {
                    String newPath = root + st.nextToken();
                    f = new File(hp0.context.getRealPath(newPath));
                    if (!f.exists())
                        continue;

                    RequestDispatcher rd = (RequestDispatcher) hContext.getRequestDispatcher(newPath);
                    if (rd != null) {
                        rd.forward(request, response);
                        return;
                    }
                }
                sr.setStatus(404);
                return;
            }

            hp0.sid = null; // do not set the session on static resources

            boolean isGET = hp0.method.equals("GET") || hp0.method.equals("POST");
            if (!isGET && !hp0.method.equals("HEAD")) {
                sr.setStatus(405);
                return;
            }

            sr.setStatus(404);
            long fLast = f.lastModified();
            fLast -= fLast % 1000;
            long rLast = -1;
            try {
                rLast = hp0.getDateHeader("if-modified-since");
            } catch (Exception ex) {
            }
            if (rLast != -1) {
                if (DEBUG) {
                    System.out.println("request: " + hp0.getHeader("if-modified-since"));
                    System.out.println("request: " + DateFormat.dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(rLast));
                    System.out.println("file   : " + DateFormat.dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(fLast));
                }

                if (rLast >= fLast) {
                    sr.contentLength = 0;
                    sr.setStatus(304);
                    return;
                }
            }
            sr.addDateHeader("Last-Modified", fLast);

            // get mime type
            int ep = path.lastIndexOf('.');
            String mime = null;
            if (ep > 0) {
                String ext = path.substring(ep + 1).toString();
                mime = hp0.context.getMimeType(ext);
                if (mime != null)
                    sr.setContentType(mime);
            }

            final String ce = hreq.getHeader("ACCEPT-ENCODING");
            if (ce != null) {
                if (ce.indexOf("gzip") >= 0) {
                    raf = getCompressed(f, hreq, servletPath, true);
                    if (raf != null)
                        sr.addHeader("Content-Encoding", "gzip");
                } else if (ce.indexOf("deflate") >= 0) {
                    raf = getCompressed(f, hreq, servletPath, false);
                    if (raf != null)
                        sr.addHeader("Content-Encoding", "deflate");
                }
            }
            if (raf == null)
                raf = new RandomAccessFile(f, "r");

            sr.setContentLength(raf.length());
            sr.setStatus(200);

            if (!isGET)
                return;

            OutputStream os = response.getOutputStream();
            byte[] buffer;
            if (os instanceof SOStream) {
                SOStream sos =  (SOStream)os;
                // disable chunk mode!
                sos.setChunkMode(false);
                buffer = new byte[sos.getBufferSize()];
            } else {
                buffer = new byte[0x4000];
            }

            final long flen = raf.length();
            final String rangeHeader = hp0.getHeader("RANGE");
            if (rangeHeader != null && rangeHeader.toLowerCase().startsWith("bytes=")) {
                if (DEBUG)
                    System.out.println("range: " + rangeHeader);

                sr.setStatus(206); // partial content
                long total = 0;
                final ArrayList<Pair<Long, Long>> rangeList = new ArrayList<Pair<Long, Long>>();
                for (final StringTokenizer st = new StringTokenizer(rangeHeader.substring(6), ","); st
                        .hasMoreElements();) {
                    final String range = st.nextToken();
                    int minus = range.indexOf('-');
                    if (minus == 0) {
                        // suffix
                        final long l = Long.parseLong(range);
                        long stop = flen;
                        rangeList.add(Pair.makePair(stop + l, stop));
                        total += -l;
                        continue;
                    }
                    long start = Long.parseLong(range.substring(0, minus));
                    long stop = minus + 1 == range.length() ? flen : Long.parseLong(range.substring(minus + 1));
                    if (start >= 0 && start < stop && stop <= flen) {
                        rangeList.add(Pair.makePair(start, stop));
                        total += stop - start;
                    }
                }

                if (rangeList.isEmpty()) {
                    sr.setContentLength(-1);
                    sr.setStatus(416);
                    return;
                }

                final boolean isMulti = rangeList.size() > 1;
                if (isMulti) {
                    response.setContentType("multipart/byteranges); boundary=http11_jodel_diplom");
                    response.setContentLength(-1);
                } else {
                    final Pair<Long, Long> p = rangeList.get(0);
                    final long start = p.getFirst();
                    final long stop = p.getSecond();
                    sr.setHeader("Content-Range", "bytes " + start + "-" + (stop - 1) + "/" + flen + "\r\n");
                    if (DEBUG)
                        System.out.println("Content-Range: bytes " + start + "-" + (stop - 1) + "/" + flen);
                    response.setContentLength((int) total);
                }

                for (final Pair<Long, Long> p : rangeList) {
                    final long start = p.getFirst();
                    long stop = p.getSecond();

                    if (DEBUG)
                        System.out.println("part: " + start + "-" + stop);

                    raf.seek(start);
                    stop -= start;

                    while (stop > 0) {
                        int toRead = stop > buffer.length ? buffer.length : (int) stop;
                        int len = raf.read(buffer, 0, toRead);

                        if (isMulti) {
                            os.write("--http11_jodel_diplom\r\n".getBytes());
                            if (mime != null)
                                os.write(("Content-Type: " + mime + "\r\n").getBytes());
                            os.write(("Content-Range: bytes " + start + "-" + (stop - 1) + "/" + flen + "\r\n\r\n")
                                    .getBytes());
                        }

                        os.write(buffer, 0, len);
                        stop -= len;
                    }
                    if (isMulti) {
                        os.write(0xd);
                        os.write(0xa);
                    }
                }
                if (isMulti) {
                    os.write("--http11_jodel_diplom--\r\n".getBytes());
                }
            } else {
                for (int len = raf.read(buffer, 0, buffer.length); len > 0; len = raf.read(buffer, 0, buffer.length)) {
                    os.write(buffer, 0, len);
                }
            }

            os.close();
        } catch (Exception e) {
            if (DEBUG)
                e.printStackTrace();
        } finally {
            if (raf != null)
                try {
                    raf.close();
                } catch (IOException e1) {
                }
        }
    }

    private RandomAccessFile getCompressed(File f, HttpServletRequest hreq, String servletPath, boolean gzip) {
        try {
            long toRead = f.length();
            if (toRead < 1000) // skip small files
                return null;

            // get the work dir
            if (workDir == null) {
                String swork = getParent() != null ? getParent().getProperty("workDir") : null;
                if (swork == null || swork.length() == 0)
                    swork = "work/" + hreq.getServerName() + "-" + hreq.getServerPort() + "-" + hreq.getContextPath();
                workDir = new File(swork);
                workDir.mkdirs();
            }
            int dot = servletPath.lastIndexOf('.');
            if (dot < 0)
                return null;

            final String ext = servletPath.substring(dot + 1).toLowerCase();
            if (!compress.contains(ext))
                return null;

            File compressedFile = new File(workDir, servletPath + (gzip ? ".gz" : ".zp"));
            if (!compressedFile.exists() || compressedFile.lastModified() < f.lastModified()) {
                synchronized (this) {
                    if (!compressedFile.exists() || compressedFile.lastModified() < f.lastModified()) {
                        compressedFile.getParentFile().mkdirs();
                        FileInputStream fis = new FileInputStream(f);
                        FileOutputStream fos = new FileOutputStream(compressedFile);
                        compress(gzip, toRead, fis, fos);
                        fos.close();
                        fis.close();
                    }
                }
            }
            if (compressedFile.length() == 0 || f.length() <= compressedFile.length())
                return null;

            RandomAccessFile fCompressed = new RandomAccessFile(compressedFile, "r");
            return fCompressed;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * compress the data from is of the given length.
     * 
     * @param gzip
     *            if true use gzip otherwise use zip.
     * @param toRead
     *            bytes to read
     * @param is
     *            the input stream
     * @param os
     *            the output stream
     * @throws IOException
     */
    public static void compress(boolean gzip, long toRead, InputStream is, OutputStream os) throws IOException {
        DeflaterOutputStream dos = gzip ? new GZIPOutputStream(os) : new DeflaterOutputStream(os);
        byte b[] = new byte[0x2000];
        while (toRead > 0) {
            int read = (int) (toRead > b.length ? b.length : toRead);
            read = is.read(b, 0, read);
            dos.write(b, 0, read);
            toRead -= read;
        }
        dos.finish();
        dos.close();
    }

    /**
     * @param wf
     */
    void setWelcomeFile(String wf) {
        defIndex = wf;
    }

}
