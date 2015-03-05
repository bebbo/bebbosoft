/******************************************************************************
 * $Source: /export/CVS/java/de/bb/sql/src/main/java/de/bb/sql/JdbcFactory.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 19:57:58 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * string like class for direct byte manipulation
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

  (c) 2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import de.bb.util.Pool;

/**
 * This class used together with de.bb.util.Pool provides an JDBC connection pool.
 * 
 * Implements the de.bb.util.Pool.Factory interface to create, destroy and validate JDBC connections. This
 * implementations assumes that the uses key is the Thread object!
 * 
 * @author Stefan Bebbo Franke
 * @see de.bb.util.Pool
 */
public class JdbcFactory implements Pool.Factory {
    private Collection initCommands = new ArrayList();
    private String jdbcUrl;
    private String user;
    private String pass;

    /**
     * Constructs a JdbcFactory object.
     * 
     * @param cName
     *            class name of the JDBC driver
     * @param $jdbcUrl
     *            JDBC url
     * @param $user
     *            user name for JDBC connection
     * @param $pass
     *            password for JDBC connection
     * @throws Exception
     *             on error
     */
    public JdbcFactory(String cName, String $jdbcUrl, String $user, String $pass) throws Exception {
        jdbcUrl = $jdbcUrl;
        user = $user;
        pass = $pass;
        Class.forName(cName);
    }

    /**
     * Constructs a JdbcFactory object.
     * 
     * @param cName
     *            class name of the JDBC driver
     * @param $jdbcUrl
     *            JDBC url
     * @param $user
     *            user name for JDBC connection
     * @param $pass
     *            password for JDBC connection
     * @param initCommands
     *            a collection of commands issued on creation if every new database connection.
     * @throws Exception
     *             on error
     */
    public JdbcFactory(String cName, String $jdbcUrl, String $user, String $pass, Collection initCommands)
            throws Exception {
        jdbcUrl = $jdbcUrl;
        user = $user;
        pass = $pass;
        if (initCommands != null)
            this.initCommands.addAll(initCommands);
        Class.forName(cName);
    }

    /**
     * Constructs a JdbcFactory object.
     * 
     * @param cName
     *            class name of the JDBC driver
     * @param $jdbcUrl
     *            JDBC url
     * @param $user
     *            user name for JDBC connection
     * @param $pass
     *            password for JDBC connection
     * @param initCommands
     *            a set of commands issued on creation if every new database connection.
     * @throws Exception
     * @deprecated
     */
    public JdbcFactory(String cName, String $jdbcUrl, String $user, String $pass, Set initCommands) throws Exception {
        jdbcUrl = $jdbcUrl;
        user = $user;
        pass = $pass;
        if (initCommands != null)
            this.initCommands.addAll(initCommands);
        Class.forName(cName);
    }

    /**
     * Creates a new JDBC connection.
     * 
     * @return Returns a proxy object for the real JDBC connection.
     * @throws Exception
     *             on error.
     */
    public Object create() throws Exception {
        Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
        conn = new de.bb.sql.Connection(conn, true);

        if (initCommands != null) {
            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                for (Iterator i = initCommands.iterator(); i.hasNext();) {
                    String cmd = (String) i.next();
                    stmt.execute(cmd);
                }
            } finally {
                stmt.close();
            }
        }
        return conn;
    }

    /**
     * Destroys the JDBC connection. The proxy object for the real JDBC connection also closes all Statements and
     * ResultSets.
     * 
     * @param o
     *            assumed to be a Connection
     */
    public void destroy(Object o) {
        try {
            de.bb.sql.Connection c = (de.bb.sql.Connection) o;
            c.reallyClose();
        } catch (Exception ex) {
        }
    }

    /**
     * Returns true // if there is no Statement left, using the Connection.
     * 
     * @return true //if there is no Statement left, using the Connection.
     * @param o
     *            assumed to be a Connection
     */
    public boolean isIdle(Object o) {
        // de.bb.sql.Connection c = (de.bb.sql.Connection) o;
        // return !c.isBusy();
        return true;
    }

    /**
     * Returns true if there is the Connection is still valid.
     * 
     * @return true if there is the Connection is still valid.
     * @param o
     *            assumed to be a Connection
     */
    public boolean validate(Object o) {
        try {
            Connection c = (Connection) o;
            return !c.isClosed();
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * Returns true if the specified key is still a valid Thread.
     * 
     * @param o
     *            assumed to be a Thread
     * @return true if the thread is still alive.
     */
    public boolean validateKey(Object o) {
        Thread t = (Thread) o;
        return t.isAlive();
    }
}
/******************************************************************************
 * $Log: JdbcFactory.java,v $
 * Revision 1.2  2012/08/11 19:57:58  bebbo
 * @I working stage
 * Revision 1.1 2011/01/01 13:12:15 bebbo
 * 
 * @N added to new CVS repo Revision 1.8 2004/11/19 13:26:27 bebbo
 * 
 * @B init commands are now copied, to avoid clashes with object reuse
 * 
 *    Revision 1.7 2004/11/18 15:16:25 bebbo
 * @R added a new constructor - old is deprecated
 * 
 *    Revision 1.6 2003/09/30 13:57:06 bebbo
 * @C enhanced comments
 * 
 *    Revision 1.5 2003/08/05 15:44:28 bebbo
 * @N new constructor to pass init commands to the JDBC driver
 * 
 *    Revision 1.4 2002/06/03 09:48:28 bebbo
 * @C fixed API doc warnings and errors
 * 
 *    Revision 1.3 2002/06/03 09:47:21 bebbo
 * @C fixed API doc warnings and errors
 * 
 *    Revision 1.2 2002/06/03 09:42:37 bebbo
 * @C more comments.
 * @C added CVS head/foot.
 * 
 *    Revision 1.1 2002/06/03 08:26:35 bebbo
 * @N new!
 * 
 *    Revision 1.3 2002/05/29 10:56:02 bebbo
 * @C commented
 * 
 *****************************************************************************/
