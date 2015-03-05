/******************************************************************************
 * $Source: /export/CVS/java/de/bb/sql/src/main/java/de/bb/sql/Connection.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 19:57:52 $
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

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * This class is desingned to wrap a java.sql.Connection. The classes in this package will track all open Statements and
 * ResultSets. With that it can be stated that a Connection is still in use, or that all allocated objects are properly
 * closed.
 * 
 * Simply wrap your Connections with this object:
 * 
 * <pre>
 * Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
 * con = new de.bb.sql.Connection(con);
 * </pre>
 * 
 * @author Stefan Bebbo Franke
 */
public class Connection implements java.sql.Connection {
    static boolean DEBUG = true;

    // the log instance
    private static Logger theLogger;

    private java.sql.Connection reference;
    private boolean keep;
    private java.util.Hashtable statements = new java.util.Hashtable();

    static String stacktrace(Throwable t) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(bos);
        t.printStackTrace(ps);
        ps.close();
        String s = bos.toString();
        int idx = s.indexOf('\r');
        if (idx >= 0)
            s = s.substring(idx);
        return s;
    }

    /**
     * Constructs a Connection object that wraps another java.sql.Connection object.
     * 
     * @param con
     *            the wrapped Connection object
     */
    public Connection(java.sql.Connection con, boolean keep) {
        reference = con;
        this.keep = keep;
    }

    public Connection(java.sql.Connection con) {
        reference = con;
        this.keep = false;
    }

    /**
     * Enables some DEBUG output.
     * 
     * @param d
     *            sets the DEBUG flag.
     * @see #getDebug
     */
    public static void setDebug(boolean d) {
        DEBUG = d;
    }

    /**
     * Queries the DEBUG flag.
     * 
     * @return current setting of the DEBUG flag.
     * @see #setDebug
     */
    public static boolean getDebug() {
        return DEBUG;
    }

    void remove(java.sql.Statement s) {
        if (null == statements.remove(s)) {
            if (DEBUG)
                logError("removing unexisting Statement - already closed?\r\n" + s.toString() + "at\r\n"
                        + stacktrace(new Exception()));
        }
    }

    /**
     * Release all open Statements and ResultSets.
     */
    public void releaseAll() {
        if (statements.size() > 0) {
            if (DEBUG) {
                logError("closing connection with " + statements.size() + " open Statements at\r\n"
                        + stacktrace(new Exception()));
            }
            while (statements.size() > 0) {
                Statement s = (Statement) statements.elements().nextElement();
                if (DEBUG) {
                    logError("closing Statement " + s.toString());
                }
                try {
                    s.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * Register a public log callback.
     * 
     * @param logger
     *            the loggin instance
     */
    public static void setLogger(Logger logger) {
        theLogger = logger;
    }

    /**
     * emitt a log error message.
     */
    public static void logError(String msg) {
        if (theLogger == null)
            return;
        theLogger.logError(msg);
    }

    /**
     * Returns true if there are no Statements using this connection.
     * 
     * @return true if there are no Statements using this connection.
     * */
    public boolean isBusy() {
        return statements.size() > 0;
    }

    public void setReadOnly(boolean p0) throws java.sql.SQLException {
        reference.setReadOnly(p0);
    }

    public void close() throws java.sql.SQLException {
        releaseAll();
        if (!keep)
            reference.close();
    }

    public void reallyClose() throws java.sql.SQLException {
        releaseAll();
        reference.close();
    }

    public boolean isReadOnly() throws java.sql.SQLException {
        return reference.isReadOnly();
    }

    public java.sql.Statement createStatement() throws java.sql.SQLException {
        java.sql.Statement r = reference.createStatement();
        java.sql.Statement s = new Statement(this, r);
        statements.put(r, s);
        return s;
    }

    public java.sql.Statement createStatement(int p0, int p1) throws java.sql.SQLException {
        java.sql.Statement r = reference.createStatement(p0, p1);
        java.sql.Statement s = new Statement(this, r);
        statements.put(r, s);
        return s;
    }

    public java.sql.PreparedStatement prepareStatement(java.lang.String p0, int p1, int p2)
            throws java.sql.SQLException {
        java.sql.PreparedStatement r = reference.prepareStatement(p0, p1, p2);
        java.sql.PreparedStatement s = new PreparedStatement(this, r);
        statements.put(r, s);
        return s;
    }

    public java.sql.PreparedStatement prepareStatement(java.lang.String p0) throws java.sql.SQLException {
        java.sql.PreparedStatement r = reference.prepareStatement(p0);
        java.sql.PreparedStatement s = new PreparedStatement(this, r);
        statements.put(r, s);
        return s;
    }

    public java.sql.CallableStatement prepareCall(java.lang.String p0) throws java.sql.SQLException {
        java.sql.CallableStatement r = reference.prepareCall(p0);
        java.sql.CallableStatement s = new CallableStatement(this, r);
        statements.put(r, s);
        return s;
    }

    public java.sql.CallableStatement prepareCall(java.lang.String p0, int p1, int p2) throws java.sql.SQLException {
        java.sql.CallableStatement r = reference.prepareCall(p0, p1, p2);
        java.sql.CallableStatement s = new CallableStatement(this, r);
        statements.put(r, s);
        return s;
    }

    public java.lang.String nativeSQL(java.lang.String p0) throws java.sql.SQLException {
        return reference.nativeSQL(p0);
    }

    public void setAutoCommit(boolean p0) throws java.sql.SQLException {
        reference.setAutoCommit(p0);
    }

    public boolean getAutoCommit() throws java.sql.SQLException {
        return reference.getAutoCommit();
    }

    public void commit() throws java.sql.SQLException {
        reference.commit();
    }

    public void rollback() throws java.sql.SQLException {
        reference.rollback();
    }

    public boolean isClosed() throws java.sql.SQLException {
        return reference.isClosed();
    }

    public java.sql.DatabaseMetaData getMetaData() throws java.sql.SQLException {
        return reference.getMetaData();
    }

    public void setCatalog(java.lang.String p0) throws java.sql.SQLException {
        reference.setCatalog(p0);
    }

    public java.lang.String getCatalog() throws java.sql.SQLException {
        return reference.getCatalog();
    }

    public void setTransactionIsolation(int p0) throws java.sql.SQLException {
        reference.setTransactionIsolation(p0);
    }

    public int getTransactionIsolation() throws java.sql.SQLException {
        return reference.getTransactionIsolation();
    }

    public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
        return reference.getWarnings();
    }

    public void clearWarnings() throws java.sql.SQLException {
        reference.clearWarnings();
    }

    public java.util.Map getTypeMap() throws java.sql.SQLException {
        return reference.getTypeMap();
    }

    protected void finalize() throws Throwable {
        releaseAll();
        super.finalize();
    }

    static {
        theLogger = new Logger() {
            public void logError(String message) {
                System.err.println(message);
            }
        };

    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return reference.createArrayOf(typeName, elements);
    }

    public Blob createBlob() throws SQLException {
        return reference.createBlob();
    }

    public Clob createClob() throws SQLException {
        return reference.createClob();
    }

    public NClob createNClob() throws SQLException {
        return reference.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return reference.createSQLXML();
    }

    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return reference.createStatement(resultSetType, resultSetConcurrency);
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return reference.createStruct(typeName, attributes);
    }

    public Properties getClientInfo() throws SQLException {
        return reference.getClientInfo();
    }

    public String getClientInfo(String name) throws SQLException {
        return reference.getClientInfo(name);
    }

    public int getHoldability() throws SQLException {
        return reference.getHoldability();
    }

    public boolean isValid(int timeout) throws SQLException {
        return reference.isValid(timeout);
    }

    public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return reference.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return reference.prepareStatement(sql, autoGeneratedKeys);
    }

    public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return reference.prepareStatement(sql, columnIndexes);
    }

    public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return reference.prepareStatement(sql, columnNames);
    }

    public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return reference.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        reference.releaseSavepoint(savepoint);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        reference.rollback();
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        reference.setClientInfo(properties);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        reference.setClientInfo(name, value);
    }

    public void setHoldability(int holdability) throws SQLException {
        reference.setHoldability(holdability);
    }

    public Savepoint setSavepoint() throws SQLException {
        return reference.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return reference.setSavepoint(name);
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        reference.setTypeMap(map);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return reference.isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return reference.unwrap(iface);
    }

    public void setSchema(String schema) throws SQLException {
        reference.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return reference.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        reference.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        reference.setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return reference.getNetworkTimeout();
    }
}
/******************************************************************************
 * $Log: Connection.java,v $
 * Revision 1.2  2012/08/11 19:57:52  bebbo
 * @I working stage
 * Revision 1.1 2011/01/01 13:12:28 bebbo
 * 
 * @N added to new CVS repo Revision 1.12 2003/09/30 13:57:06 bebbo
 * 
 * @C enhanced comments
 * 
 *    Revision 1.11 2002/12/16 19:55:08 bebbo
 * @B fixes for leakage detection
 * 
 *    Revision 1.10 2002/12/16 16:45:23 bebbo
 * @N added releaseAll to release all Statements and ResultSets
 * 
 *    Revision 1.9 2002/11/06 09:46:20 bebbo
 * @I cleanup for imports
 * 
 *    Revision 1.8 2002/08/21 09:25:04 bebbo
 * @O creation of stacktrace is only done, if it is dumped.
 * 
 *    Revision 1.7 2002/06/03 10:51:36 bebbo
 * @B using wrapped Statements for close in cleanup
 * 
 *    Revision 1.6 2002/06/03 09:47:21 bebbo
 * @C fixed API doc warnings and errors
 * 
 *    Revision 1.5 2002/06/03 09:42:37 bebbo
 * @C more comments.
 * @C added CVS head/foot.
 * 
 *****************************************************************************/
