/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/IMailFile.java,v $
 * $Revision: 1.4 $
 * $Date: 2012/05/17 07:22:28 $
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

/**
  * 
  */
public interface IMailFile {
    /**
     * Creates a new mail entry.
     * 
     * @return
     */
    MailEntry createNewMail();

    /**
     * Method storeMail updates the mail size
     * 
     * @param me
     * @return
     */
    File storeMail(MailEntry me);

    /**
     * Method getInputStream gets an InputStream to the mail file.
     * 
     * @param mailId
     * @return InputStream
     * @throws FileNotFoundException
     */
    InputStream getInputStream(String mailId) throws FileNotFoundException;

    /**
     * Method getOutputStream gets an OutputStream to the mail file.
     * 
     * @param mailId
     * @return OutputStream
     * @throws FileNotFoundException
     */
    OutputStream getOutputStream(String mailId) throws FileNotFoundException;

    /**
     * Method gets an read only RandomAccessFile for the mail file.
     * 
     * @param mailId
     * @return RandomAccessFile
     * @throws FileNotFoundException
     */
    RandomAccessFile openRandomAccessFile(String mailId) throws FileNotFoundException;

    /**
     * Method files returns an Iterator with all maintained files.
     * 
     * @return
     * @throws IOException
     */
    Iterator<File> files() throws IOException;

    /**
     * Method removeMail removes the mail file from the storage.
     * 
     * @param file
     * @return boolean true on removal.
     */
    boolean removeMail(Object file);

}

/******************************************************************************
 * $Log: IMailFile.java,v $
 * Revision 1.4  2012/05/17 07:22:28  bebbo
 * @I replaced Vector with ArrayList
 * @I more typed collections
 * @R MimeFile is now in de.bb.util
 * Revision 1.3 2005/11/30 05:55:09 bebbo
 * 
 * @C added return tag to JavaDoc
 * 
 *    Revision 1.2 2004/12/16 16:01:51 bebbo
 * @N added support to hook a virus scanner
 * 
 *    Revision 1.1 2002/11/19 14:35:53 bebbo
 * @R removed migration support from old version to current version
 * @N separated mail storage into separate class.
 * @R MailFile is now using subdirectories since ext2fs is so slow...
 * 
 *****************************************************************************/
