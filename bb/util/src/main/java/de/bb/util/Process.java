/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/main/java/de/bb/util/Process.java,v $
 * $Revision: 1.13 $
 * $Date: 2011/01/01 12:11:23 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Wrapper class around process invokation.  
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
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

package de.bb.util;

import java.io.*;

/**
 * Eases the invokation of external programs.
 */
public class Process {
    // private final static boolean DEBUG = false;
    private final static byte[] CRLF = {0xd, 0xa};

    private Process() {
    }

    /**
     * Starts an external program (process) passing the specified input to the program and fills an OutputStream with
     * the programs output. If an error occurs the content of stderr is also written to the OutputStream. This is ideal
     * for offline processing of e programs output.
     * 
     * @param command
     *            the complete command line to execute
     * @param stdin
     *            data to fill the programs stdin, or null
     * @param os
     *            an output stream which receives the programs output
     * @param env
     *            an environment or null
     * @return 0 on succes or the programs exit code
     * @throws IOException
     *             if an i/o error occurs
     */
    public static int execute(String command, byte[] stdin, OutputStream os, String[] env) throws IOException {
        return execute(command, stdin, os, env, 45000);
    }

    /**
     * Starts an external program (process) passing the specified input to the program and fills an OutputStream with
     * the programs output. If an error occurs the content of stderr is also written to the OutputStream. This is ideal
     * for offline processing of e programs output.
     * 
     * @param command
     *            the complete command line to execute
     * @param stdin
     *            data to fill the programs stdin, or null
     * @param os
     *            an output stream which receives the programs output
     * @param env
     *            an environment or null
     * @param timeout
     *            a timeout value in ms
     * @return 0 on succes or the programs exit code
     * @throws IOException
     *             if an i/o error occurs
     */
    public static int execute(String command, byte[] stdin, OutputStream os, String[] env, long timeout)
            throws IOException {
        ByteArrayInputStream bis = null;
        if (stdin != null)
            bis = new ByteArrayInputStream(stdin);
        return execute(command, bis, os, env, timeout);
    }

    /**
     * Starts an external program (process) passing the specified input to the program and fills an OutputStream with
     * the programs output. If an error occurs the content of stderr is also written to the OutputStream. This is ideal
     * for offline processing of e programs output.
     * 
     * @param command
     *            the complete command line to execute
     * @param stdin
     *            data to fill the programs stdin, or null
     * @param os
     *            an output stream which receives the programs output
     * @param env
     *            an environment or null
     * @param timeout
     *            a timeout value in ms
     * @return 0 on succes or the programs exit code
     * @throws IOException
     *             if an i/o error occurs
     */
    public static int execute(String command, InputStream stdin, OutputStream os, String[] env, long timeout)
            throws IOException {
        return execute(command, stdin, os, env, timeout, null);
    }

    /**
     * Starts an external program (process) passing the specified input to the program and fills an OutputStream with
     * the programs output. If an error occurs the content of stderr is also written to the OutputStream. This is ideal
     * for offline processing of e programs output.
     * 
     * @param command
     *            the complete command line to execute
     * @param stdin
     *            data to fill the programs stdin, or null
     * @param os
     *            an output stream which receives the programs output
     * @param env
     *            an environment or null
     * @param timeout
     *            a timeout value in ms
     * @param dir
     *            a File object defining the current dir for the process
     * @return 0 on succes or the programs exit code
     * @throws IOException
     *             if an i/o error occurs
     */
    public static int execute(String command, InputStream stdin, OutputStream os, String[] env, long timeout, File dir)
            throws IOException {
        if (os == null)
            os = new OutputStream() {
                public void write(int b) throws IOException {
                }
            };
        int ret = -1;
        InThread stdinThread = null;
        try {
            java.lang.Process proc;
            proc = Runtime.getRuntime().exec(command, env, dir);

            if (stdin != null) {
                OutputStream pos = proc.getOutputStream();
                stdinThread = new InThread(stdin, pos);
                stdinThread.start();
            }

            InputStream es = proc.getErrorStream();
            InputStream is = proc.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte b[] = new byte[1024];
            long stop = System.currentTimeMillis() + timeout;
            while (stop > System.currentTimeMillis()) {
                int el = es.available();
                if (el > b.length)
                    el = b.length;
                int il = is.available();
                if (il > b.length)
                    il = b.length;
                if (el == 0 && il == 0) {
                    try {
                        ret = proc.exitValue();
                        break;
                    } catch (Exception e) {
                    }
                    // System.out.println("sleep");
                    Thread.sleep(50);
                    continue;
                }
                if (el > 0) {
                    int len = es.read(b, 0, el);
                    bos.write(b, 0, len);
                }
                if (il > 0) {
                    int len = is.read(b, 0, il);
                    os.write(b, 0, len);
                }
            }

            for (int il = is.available(); il > 0;) {
                if (il > b.length)
                    il = b.length;
                int len = is.read(b, 0, il);
                if (len > 0)
                    os.write(b, 0, len);
            }
            for (int el = es.available(); el > 0;) {
                if (el > b.length)
                    el = b.length;
                int len = es.read(b, 0, el);
                if (len > 0)
                    bos.write(b, 0, len);
            }

            if (ret == -1) {
                proc.destroy();
            }
            if (ret != 0) {
                os.write(CRLF);
                os.write(bos.toByteArray());
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            if (stdinThread != null) {
                stdinThread.close();
                try {
                    stdinThread.join();
                } catch (InterruptedException e) {
                }
            }
        }
        return ret;
    }

    private static class InThread extends Thread {
        private InputStream is;

        private OutputStream os;

        InThread(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                for (int r = is.read(); r >= 0; r = is.read()) {
                    os.write(r);
                }
            } catch (IOException e) {
            }
            close();
        }

        void close() {
            try {
                os.close();
            } catch (IOException e) {
            }
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}

/******************************************************************************
 * $Log: Process.java,v $
 * Revision 1.13  2011/01/01 12:11:23  bebbo
 * @I internal cleanup
 * Revision 1.12 2008/06/04 06:46:40 bebbo
 * 
 * @I cleanup
 * 
 *    Revision 1.11 2008/03/15 18:01:05 bebbo
 * @R Changed the license: From now on GPL 3 applies.
 * 
 *    Revision 1.10 2007/01/18 22:04:30 bebbo
 * @I reformatted
 * 
 *    Revision 1.9 2006/03/17 11:38:11 bebbo
 * @I catch Throwable instead of Exception
 * 
 *    Revision 1.8 2006/02/02 07:48:26 bebbo
 * @I cleanup
 * 
 *    Revision 1.7 2003/09/30 13:17:19 bebbo
 * @B fixed api doc
 * 
 *    Revision 1.6 2003/07/14 11:30:06 bebbo
 * @B fixed NPE in execute when os is null
 * 
 *    Revision 1.5 2003/06/17 10:22:12 bebbo
 * @N new execute() function to specify also execution directory
 * 
 *    Revision 1.4 2003/02/05 08:09:02 bebbo
 * @B stdin is now handled by a separate thread
 * 
 *    Revision 1.3 2002/12/19 14:55:59 bebbo
 * @N added a function with timeout
 * 
 *    Revision 1.2 2001/09/15 08:56:45 bebbo
 * @C added comments
 * 
 *    Revision 1.1 2001/04/22 20:26:47 bebbo
 * @N new
 * 
 *****************************************************************************/
