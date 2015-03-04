/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Cron.java,v $
 * $Revision: 1.11 $
 * $Date: 2013/06/18 13:23:54 $
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

package de.bb.bejy;

import java.util.HashSet;
import java.util.Iterator;

import de.bb.util.*;

/**
 * Start Runnable at specified times.
 */
public class Cron {
    private SessionManager<Runnable, CronJob> sMan = new SessionManager<Runnable, CronJob>(0L);
    private HashSet<CronThread> cronThreads = new HashSet<CronThread>();

    /**
     * Run a Runnable object in x ms.
     * 
     * @param r
     *            a runnable object
     * @param time
     *            time in ms
     * @return an Object which can be used to remove this runnable.
     */
    public Object runIn(String name, Runnable r, long time) {
        return runEvery(name, r, time, 0);
    }

    /**
     * Run a Runnable object at a given time.
     * 
     * @param r
     *            a runnable object
     * @param at
     *            time in ms
     * @return an Object which can be used to remove this runnable.
     */
    public Object runAt(String name, Runnable r, long at) {
        return runIn(name, r, at - System.currentTimeMillis());
    }

    /**
     * Run a Runnable object each x ms.
     * 
     * @param r
     *            a runnable object
     * @param startIntervall
     *            time in ms
     * @param intervall
     *            time in ms
     * @return an Object which can be used to remove this runnable.
     */
    public Object runEvery(String name, Runnable r, long startIntervall, long intervall) {
        CronJob cj = new CronJob(name, r, sMan, (int) intervall);
        cj.setStartTime(System.currentTimeMillis() + startIntervall);
        return sMan.put(r, cj, startIntervall);
    }

    /**
     * Remove a Runnable from the Cron.
     * 
     * @param key
     *            a Runnable object's key
     * @return the Runnable.
     */
    public Object remove(Object key) {
        return sMan.remove(key);
    }

    /**
     * Removes all registered cron jobs.
     */
    public void clear() {
        sMan.clear();
    }

    /**
     * Add a CronThread.
     * 
     * @param thread
     */
    synchronized void addThread(CronThread thread) {
        cronThreads.add(thread);
    }

    /**
     * 
     * @param thread
     */
    synchronized void removeThread(CronThread thread) {
        cronThreads.remove(thread);
    }

    public Iterator<CronJob> cronJobs() {
        return sMan.elements();
    }

    public Iterator<CronThread> runningJobs() {
        return cronThreads.iterator();
    }

}

/******************************************************************************
 * $Log: Cron.java,v $
 * Revision 1.11  2013/06/18 13:23:54  bebbo
 * @I preparations to use nio sockets
 * @V 1.5.1.68
 * Revision 1.10 2006/05/09 08:38:35 bebbo
 * 
 * @B enabled lookup capability to remove cron jobs
 * 
 *    Revision 1.9 2005/11/11 18:50:53 bebbo
 * @R changed into 3 toplevel classes, added verbose stuff
 * 
 *    Revision 1.8 2004/12/16 15:58:39 bebbo
 * @D added a thread name
 * 
 *    Revision 1.7 2004/04/16 13:39:43 bebbo
 * @R using new SessionManager
 * 
 *    Revision 1.6 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *    Revision 1.5 2002/12/02 18:40:19 bebbo
 * @B fixed multiple cron starts
 * 
 *    Revision 1.4 2002/08/21 09:14:43 bebbo
 * @R changes for the admin UI
 * 
 *    Revision 1.3 2002/05/16 15:19:48 franke
 * @C CVS
 * 
 *    Revision 1.2 2001/09/15 08:44:36 bebbo
 * @I modified to use new SessionManager
 * @C added comments
 * 
 *    Revision 1.1 2000/12/30 09:03:52 bebbo
 * @N just created
 * 
 *    Revision 1.1 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *****************************************************************************/
