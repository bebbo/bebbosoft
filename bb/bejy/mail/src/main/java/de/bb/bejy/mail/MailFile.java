/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/MailFile.java,v $
 * $Revision: 1.6 $
 * $Date: 2013/05/17 11:00:15 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
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

  (c) 1994-2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy.mail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Stack;

import de.bb.util.SessionManager;

/**
 * An implementation to store mail in files.
 */
public class MailFile implements IMailFile {
    String path;
    int pathOffset;

    /**
     * Constructor MailFile.
     * 
     * @param path
     */
    MailFile(String path) {
        this.path = path;
        pathOffset = new File(path).getAbsolutePath().length();
    }

    /**
     * Method createNewMail.
     * 
     * @return MailEntry
     */
    public MailEntry createNewMail() {
        MailEntry me = new MailEntry(SessionManager.newKey(), null, null);
        return me;
    }

    /**
     * Method storeMail.
     * 
     * @param me
     * @return
     */
    public File storeMail(MailEntry me) {
        File f = new File(path, makeFilePath(me.mailId));
        if (!f.exists()) {
            f = new File(path, me.mailId);
        }
        me.size = Long.toString(f.length());

        return f.getAbsoluteFile();
    }

    /**
     * Method makeFilePath.
     * 
     * @param name
     * @return
     */
    private String makeFilePath(String name) {
        name = name.trim();
        byte b[] = name.getBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length * 2);
        for (int i = 0; i < b.length; ++i) {
            bos.write(b[i]);
            if ((i == 3) && i < b.length - 1) {
                bos.write('/');
            }
        }
        return bos.toString();
    }

    /**
     * Method makePath.
     * 
     * @param name
     * @return
     */
    private String makePath(String name) {
        String n = makeFilePath(name);
        int idx = n.lastIndexOf('/');
        return n.substring(0, idx);
    }

    /**
     * Method getInputStream.
     * 
     * @param mailId
     * @return InputStream
     * @throws FileNotFoundException
     */
    public InputStream getInputStream(String mailId) throws FileNotFoundException {
        File f = new File(path, makeFilePath(mailId));
        if (!f.exists()) {
            f = new File(path, mailId);
        }
        return new FileInputStream(f);
    }

    /**
     * Method getOutputStream.
     * 
     * @param mailId
     * @return OutputStream
     * @throws FileNotFoundException
     */
    public OutputStream getOutputStream(String mailId) throws FileNotFoundException {
        new File(path, makePath(mailId)).mkdirs();
        File f = new File(path, makeFilePath(mailId));
        return new FileOutputStream(f);
    }

    /**
     * Method openRandomAccessFile.
     * 
     * @param mailId
     * @return RandomAccessFile
     * @throws FileNotFoundException
     */
    public RandomAccessFile openRandomAccessFile(String mailId) throws FileNotFoundException {
        mailId = mailId.trim();
        File f = new File(path, makeFilePath(mailId));
        if (!f.exists()) {
            System.out.println("does not exist:" + f);
            f = new File(path, mailId);
        }
        return new RandomAccessFile(f, "r");
    }

    /**
     * Method files.
     * 
     * @return
     * @throws IOException
     */
    public Iterator files() throws IOException {
        return new I(path);
    }

    /**
     * Method removeMail.
     * 
     * @param file
     * @return boolean
     */
    public boolean removeMail(Object file) {
        File df = (File) file;
        if (df.lastModified() < System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 7) {
            return df.delete();
        }
        return false;
    }

    private class F extends File {
        int offset;

        F() {
            super(new File(path).getAbsolutePath());
            offset = getAbsolutePath().length();
        }

        F(File f) {
            super(f.getAbsolutePath());
            offset = pathOffset;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.io.File#toString()
         */
        public String toString() {
            String s = "";
            s = getAbsolutePath().substring(offset);
            for (int i = s.indexOf('/'); i >= 0; i = s.indexOf('/')) {
                s = s.substring(0, i) + s.substring(i + 1);
            }
            for (int i = s.indexOf('\\'); i >= 0; i = s.indexOf('\\')) {
                s = s.substring(0, i) + s.substring(i + 1);
            }

            return s;
        }

        /**
         * @see java.io.File#delete()
         */
        public boolean delete() {
            boolean r = super.delete();
            if (r) {
                for (File f = getParentFile(); f.getAbsolutePath().length() > offset; f = f.getParentFile()) {
                    if (f.list().length > 0) {
                        break;
                    }
                    if (!f.delete()) {
                        break;
                    }
                }
            }
            return r;
        }

    }

    private class I implements Iterator {
        private Stack stack = new Stack();
        private File file = null;

        I(String path) throws IOException {
            File f = new F();
            stack.push(f);
            loadNext();
        }

        /**
         * Method loadNext.
         */
        private void loadNext() {
            file = null;
            while (stack.size() > 0) {
                File f = (File) stack.pop();
                if (f.isFile()) {
                    file = f;
                    return;
                }

                String files[] = f.list();
                if (files == null)
                    continue;

                de.bb.util.Sort.quicksort(files, 0, files.length - 1);
                for (int i = files.length - 1; i >= 0; --i) {
                    stack.push(new F(new File(f, files[i])));
                }
            }
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return file != null;
        }

        /**
         * @see java.util.Iterator#next()
         */
        public Object next() {
            File f = file;
            loadNext();
            return f;
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
        }
    }

    /*
      public static void main(String args[])
      {
        try
        {
          MailFile mf = new MailFile("/xxx");

          System.out.println(mf.makeFilePath("abcdefgh"));
          
    //      new File("/" + mf.makePath("abcdefgh")).mkdirs();
          
          /*     
               for (Iterator i = mf.files(); i.hasNext();)
               {
                 System.out.println(i.next());
               }
          */
    /*
    mf.getOutputStream("abcdefghasdfasdfasdfasdfdf").write('!');

    throw new Exception("x");
    } catch (Exception e)
    {
    }
    }
    */
}

/******************************************************************************
 * $Log: MailFile.java,v $
 * Revision 1.6  2013/05/17 11:00:15  bebbo
 * @F reformat
 * Revision 1.5 2006/05/09 08:48:32 bebbo
 * 
 * @R support for new faster cleanup method.
 * 
 *    Revision 1.4 2005/11/30 06:16:05 bebbo
 * @C added comments
 * 
 *    Revision 1.3 2004/12/16 16:03:41 bebbo
 * @N added support to hook a virus scanner
 * 
 *    Revision 1.2 2003/02/19 13:26:51 bebbo
 * @I changed visibility of methods/fields -> more performance
 * 
 *    Revision 1.1 2002/11/19 14:35:53 bebbo
 * @R removed migration support from old version to current version
 * @N separated mail storage into separate class.
 * @R MailFile is now using subdirectories since ext2fs is so slow...
 * 
 *****************************************************************************/
