/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HttpSession.java,v $
 * $Revision: 1.12 $
 * $Date: 2014/06/23 15:38:46 $
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

package de.bb.bejy.http;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

class HttpSession implements javax.servlet.http.HttpSession, de.bb.util.SessionManager.Callback {

    Hashtable objectTable;

    long lastUsed;

    long created;

    String sessionId;

    long timeout;

    boolean invalid = false;

    HttpContext context;

    /**
     * Constructor
     * 
     * @param server
     *            The ServerEngine the Session is created for.<br> Every ServerEngine has an own session manager.
     */
    HttpSession(HttpContext ctx) {
        context = ctx;
        created = lastUsed = System.currentTimeMillis();
        objectTable = new Hashtable();
        timeout = context.sessionManager.getTimeout();
        sessionId = context.createSession(this);
    }

    public boolean dontRemove(Object key) {
        invalidate();
        return false;
    }

    void touch() {
        context.sessionManager.touch(sessionId);
        lastUsed = System.currentTimeMillis();
    }

    // ====== HttpSession implementation ======================

    public long getCreationTime() throws IllegalStateException {
        if (invalid)
            throw new IllegalStateException();
        return created;
    }

    public String getId() {
        return sessionId;
    }

    public long getLastAccessedTime() throws IllegalStateException {
        if (invalid)
            throw new IllegalStateException();
        return lastUsed;
    }

    public int getMaxInactiveInterval() {
        if (invalid)
            throw new IllegalStateException();
        return (int) (timeout / 1000);
    }

    /**
     * @deprecated
     */
    public Object getValue(String name) throws IllegalStateException {
        return getAttribute(name);
    }

    /**
     * @deprecated
     */
    public String[] getValueNames() throws IllegalStateException {
        synchronized (objectTable) {
            if (invalid)
                throw new IllegalStateException();
            String[] names = new String[objectTable.size()];
            Enumeration e = objectTable.keys();
            for (int i = 0; e.hasMoreElements(); ++i) {
                names[i] = (String) e.nextElement();
            }
            return names;
        }
    }

    public void invalidate() throws IllegalStateException {
        synchronized (objectTable) {
            if (invalid)
                throw new IllegalStateException();

            for (Enumeration e = objectTable.keys(); e.hasMoreElements();) {
                removeAttribute((String) e.nextElement());
            }
            objectTable.clear();
            invalid = true;
            context.removeSession(this);
        }
    }

    public boolean isNew() throws IllegalStateException {
        if (invalid)
            throw new IllegalStateException();
        return created == lastUsed;
    }

    /**
     * @deprecated
     */
    public void putValue(String name, Object value) throws IllegalStateException {
        setAttribute(name, value);
    }

    /**
     * @deprecated
     */
    public void removeValue(String name) throws IllegalStateException {
        removeAttribute(name);
    }

    public void setMaxInactiveInterval(int val) {
        if (invalid)
            throw new IllegalStateException();
        timeout = val * 1000L;
        context.sessionManager.touch(sessionId, timeout);
    }

    public java.lang.Object getAttribute(java.lang.String name) // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        return objectTable.get(name);
    }

    public java.util.Enumeration getAttributeNames() // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        return objectTable.keys();
    }

    public void removeAttribute(java.lang.String name) // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        Object old = objectTable.remove(name);
        javax.servlet.http.HttpSessionBindingEvent hsbe = new javax.servlet.http.HttpSessionBindingEvent(this, name,
                old);
        if (old instanceof javax.servlet.http.HttpSessionBindingListener) {
            ((javax.servlet.http.HttpSessionBindingListener) old).valueUnbound(hsbe);
        }
        if (context.hsalv != null) {
            for (Iterator e = context.hsalv.iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionAttributeListener hsal = (javax.servlet.http.HttpSessionAttributeListener) e
                        .next();
                hsal.attributeRemoved(hsbe);
            }
        }
    }

    public void setAttribute(java.lang.String name, java.lang.Object o) // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        Object old = objectTable.put(name, o);
        javax.servlet.http.HttpSessionBindingEvent hsbe = new javax.servlet.http.HttpSessionBindingEvent(this, name, o);
        javax.servlet.http.HttpSessionBindingEvent hsbeo = new javax.servlet.http.HttpSessionBindingEvent(this, name,
                old);
        if (o instanceof javax.servlet.http.HttpSessionBindingListener) {
            ((javax.servlet.http.HttpSessionBindingListener) o).valueBound(hsbe);
        }
        if (old instanceof javax.servlet.http.HttpSessionBindingListener) {
            ((javax.servlet.http.HttpSessionBindingListener) old).valueUnbound(hsbeo);
        }
        if (context.hsalv != null) {
            for (Iterator e = context.hsalv.iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionAttributeListener hsal = (javax.servlet.http.HttpSessionAttributeListener) e
                        .next();
                if (old == null)
                    hsal.attributeAdded(hsbe);
                else
                    hsal.attributeReplaced(hsbeo);
            }
        }
    }

    public javax.servlet.ServletContext getServletContext() {
        return context;
    }

    /**
     * @deprecated
     */
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return context;
    }

}

/******************************************************************************
 * $Log: HttpSession.java,v $
 * Revision 1.12  2014/06/23 15:38:46  bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles
 * Revision 1.11 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.10 2006/02/06 09:16:08 bebbo
 * @I cleanup
 * 
 *    Revision 1.9 2005/11/18 14:48:10 bebbo
 * @R getId() is now allowed for invalidated sessions
 * @B invalidate() properly removes the attributes from a session
 * 
 *    Revision 1.8 2004/03/23 14:44:14 bebbo
 * @B the original request is now properly resovled from request wrappers
 * 
 *    Revision 1.7 2004/03/23 11:18:00 bebbo
 * @I timeout is handled in ms as long
 * 
 *    Revision 1.6 2003/01/07 18:32:20 bebbo
 * @W removed some deprecated warnings
 * 
 *    Revision 1.5 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.4 2002/09/26 16:27:33 bebbo
 * @B session was not really touched and timeouted to early
 * 
 *    Revision 1.3 2002/04/02 13:02:35 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.2 2002/03/30 15:48:41 franke
 * @R added a dummy for servlet 2.3 function
 * 
 *    Revision 1.1 2001/03/29 18:25:30 bebbo
 * @N new generated
 * 
 *****************************************************************************/
