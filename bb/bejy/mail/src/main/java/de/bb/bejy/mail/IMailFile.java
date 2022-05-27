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

