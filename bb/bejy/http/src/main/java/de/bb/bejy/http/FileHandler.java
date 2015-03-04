/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/FileHandler.java,v $
 * $Revision: 1.41 $
 * $Date: 2014/06/24 09:16:43 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * file handler for bejy
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

/******************************************************************************
 * $Log: FileHandler.java,v $
 * Revision 1.41  2014/06/24 09:16:43  bebbo
 * @R FileHandler no longer sends the session id
 *
 * Revision 1.40  2013/11/23 16:41:15  bebbo
 * @B fixed class cast if FileHandler was called via include from a JSP page
 *
 * Revision 1.39  2013/11/23 10:36:19  bebbo
 * @R contentLength supports long values now
 *
 * Revision 1.38  2013/07/23 07:00:44  bebbo
 * @N added support for content ranges
 * Revision 1.37 2013/05/17 10:33:21 bebbo
 * 
 * @N compressed files are cached in work dir Revision 1.36 2012/12/23 10:27:40 bebbo
 * 
 * @N added compression support for the file handler Revision 1.35 2012/12/21 08:15:20 bebbo
 * 
 * @R set content Length to 0 to keep connections if 304 is sent Revision 1.34 2012/12/15 19:38:57 bebbo
 * 
 * @I refactoring Revision 1.33 2012/11/14 15:11:45 bebbo
 * 
 * @B h404 directly invokes the CgiHandler
 * @B CgiHandler no longer uses a ForwardRequest, CGI params are set directly
 * @B cgi handler with h404 is working for Drupal Revision 1.32 2012/11/08 12:10:58 bebbo
 * 
 * @N h404 handler is able to use URL rewriting Revision 1.31 2010/04/11 10:16:18 bebbo
 * 
 * @N new configuration option "h404" to add a 404 handler to CGI (e.g. PHP) based applications to enable stuff like
 *    wordpress permalinks.
 * 
 *    Revision 1.30 2009/11/25 08:29:13 bebbo
 * @V bumped the version
 * @B fixed forwarding for the welcome files with CGI: query string was lost.
 * 
 *    Revision 1.29 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.28 2008/01/17 17:28:59 bebbo
 * @I method has now type String --> changed accordingly
 * 
 *    Revision 1.27 2007/05/01 19:04:54 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.26 2004/12/16 15:59:28 bebbo
 * @I changed upper/lowercas of last-modified
 * 
 *    Revision 1.25 2004/12/13 15:30:48 bebbo
 * @B fixed broken mime type lookup
 * @B added index.html as default page for admin.jsp
 * @B fixed real path handling
 * 
 *    Revision 1.24 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.23 2004/03/23 14:44:14 bebbo
 * @B the original request is now properly resovled from request wrappers
 * 
 *    Revision 1.22 2004/03/23 12:26:40 bebbo
 * @I moved code to RequestDispatcher to determine the original request/response
 * 
 *    Revision 1.21 2004/03/23 11:11:55 bebbo
 * @B the original request is now properly resovled from request wrappers
 * 
 *    Revision 1.20 2003/06/18 08:36:52 bebbo
 * @R modification, dynamic loading, removing - all works now
 * 
 *    Revision 1.19 2003/06/17 10:18:43 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.18 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.17 2003/01/27 14:58:31 bebbo
 * @I removed usage of some obsolete functions
 * 
 *    Revision 1.16 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.15 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.14 2002/05/19 12:57:42 bebbo
 * @B fixed H401 and H302 handling
 * 
 *    Revision 1.13 2002/04/02 13:02:34 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.12 2002/03/10 20:07:42 bebbo
 * @I changes due to redesigned buffer handling: contentLength is no longer set
 * 
 *    Revision 1.11 2001/12/04 17:40:42 franke
 * @N separated RequestDispatcher to ease the forward and inlude funtions. Caused some changes, since members from
 *    HttpProtocol moved.
 * 
 *    Revision 1.10 2001/05/07 16:18:03 bebbo
 * @B fixed behaviour for long file donwload
 * 
 *    Revision 1.9 2001/05/06 13:11:04 bebbo
 * @B now buffer is enlarged to fit file size
 * 
 *    Revision 1.8 2001/04/11 13:15:41 bebbo
 * @R if requested name matches a directory, a redirect is replied (302)
 * 
 *    Revision 1.7 2001/04/06 05:54:05 bebbo
 * @B fix for HTTP/1.0
 * @B fix for cookies
 * 
 *    Revision 1.6 2001/03/29 07:08:18 bebbo
 * @R HttpHandler now implements javax.servlet.Servlet and javax.servlet.ServletConfig
 * 
 *    Revision 1.5 2001/03/28 09:15:04 bebbo
 * @D debug off
 * 
 *    Revision 1.4 2001/03/27 19:49:39 bebbo
 * @I removed clone
 * @I all member vars are readonly now
 * 
 *    Revision 1.3 2001/03/27 09:52:53 franke
 * @I uses function setStatus
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
