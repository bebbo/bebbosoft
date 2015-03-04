/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/MailAccount.java,v $
 * $Revision: 1.9 $
 * $Date: 2013/05/21 06:13:31 $
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

  (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy.mail;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

class MailAccount {
    private final static Hashtable<String, Integer> useCount = new Hashtable<String, Integer>();
    private final static Hashtable<String, Vector<MailEntry>> delStore = new Hashtable<String, Vector<MailEntry>>();

    /// mail account id
    String id;
    /// total size of all mails  
    int totalSize;
    /// mails which are in account during log in
    Vector<MailEntry> mails;
    private MailCfg cfg;

    /**
     * @param _id
     * @param cfg
     */
    public MailAccount(String _id, MailCfg cfg) {
        id = _id;
        this.cfg = cfg;
        mails = new Vector<MailEntry>();

        synchronized (useCount) {
            Integer i = useCount.get(id);
            if (i == null)
                i = new Integer(0);
            useCount.put(id, new Integer(i.intValue() + 1));
        }
    }

    /**
     * get total size of all MailEntries.
     * 
     * @return
     */
    public int getTotalSize() {
        return totalSize;
    }

    /**
     * add a MailEntry.
     * 
     * @param me
     */
    public void addElement(MailEntry me) {
        try {
            totalSize += Integer.parseInt(me.size);
        } catch (Exception xxx) {
        }
        mails.addElement(me);
    }

    /**
     * all elements as Enumeration.
     * 
     * @return
     */
    public Enumeration<MailEntry> elements() {
        return mails.elements();
    }

    /**
     * the MailEntry at specified position.
     * 
     * @param i
     * @return
     */
    public MailEntry elementAt(int i) {
        return mails.elementAt(i);
    }

    /**
     * count of elements.
     * 
     * @return
     */
    public int size() {
        return mails.size();
    }

    /**
     * merges mails which are marked for deletion
     * 
     * @throws Exception
     */
    public void update() throws Exception {
        synchronized (useCount) {
            Integer i = useCount.get(id);
            if (i == null)
                return;

            // add all mails which are marked for deletion
            Vector<MailEntry> del = delStore.get(id);
            if (del == null)
                del = new Vector<MailEntry>();
            for (Enumeration<MailEntry> e = mails.elements(); e.hasMoreElements();) {
                MailEntry me = e.nextElement();
                if (me.dele)
                    del.addElement(me);
            }

            int ii = i.intValue();

            if (--ii > 0) // is another account open?
            {
                useCount.put(id, new Integer(ii)); // fix usage counter
                delStore.put(id, del); // append mails to delete
            } else {
                useCount.remove(id);
                delStore.remove(id);

                // remove all marked mails
                MailDBI dbi = null;
                try {
                    dbi = cfg.getDbi(this);
                    for (Enumeration<MailEntry> e = del.elements(); e.hasMoreElements();) {
                        MailEntry me = e.nextElement();
                        if (me.dele) // should always be true here
                            dbi.deleteMail(id, me);
                    }
                } finally {
                    if (dbi != null)
                        cfg.releaseDbi(this, dbi);
                }
            }
        }
    }
}

/******************************************************************************
 * $Log: MailAccount.java,v $
 * Revision 1.9  2013/05/21 06:13:31  bebbo
 * @F formatted and typified
 * Revision 1.8 2004/12/16 16:03:30 bebbo
 * 
 * @R database connections are now shared
 * 
 *    Revision 1.7 2002/04/03 15:41:30 franke
 * @R no longer public
 * 
 *    Revision 1.6 2002/01/20 12:02:57 franke
 * @R mail table is obsolete. Spooler is now using imap_mime to keep mails. Did all necessary changes due to that
 * 
 *    Revision 1.5 2001/03/09 19:47:42 bebbo
 * @I some vars are private now
 * 
 *    Revision 1.4 2001/02/27 16:25:35 bebbo
 * @R not all methods were public - fixed
 * 
 *    Revision 1.3 2001/02/25 17:08:57 bebbo
 * @R update is throwing an exception now
 * 
 *    Revision 1.2 2001/02/20 17:40:31 bebbo
 * @B member mails is now initialized
 * 
 *    Revision 1.1 2001/02/19 19:56:16 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *****************************************************************************/
