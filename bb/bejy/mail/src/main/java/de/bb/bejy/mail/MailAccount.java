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

