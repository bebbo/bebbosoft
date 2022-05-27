/******************************************************************************
 * $Source: /export/CVS/java/de/bb/sql/src/main/java/de/bb/sql/Statement.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 19:57:59 $
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

import java.sql.SQLException;

/**
 * This class is a proxy class for java.sql.Statement. In addtion to the wrapped class all ResultSets are tracked, and
 * checked.
 * 
 * @author Stefan Bebbo Franke
 */
public class Statement implements java.sql.Statement {
    Connection connection;
    java.sql.Statement reference;
    protected java.util.Hashtable resultSets = new java.util.Hashtable();
    private Throwable myT;

    Statement(Connection con, java.sql.Statement p) {
        connection = con;
        reference = p;
        myT = new Throwable();
    }

    public String toString() {
        return super.toString() + Connection.stacktrace(myT);
    }

    void remove(java.sql.ResultSet rs) {
        if (null == resultSets.remove(rs)) {
            if (Connection.DEBUG) {
                Connection.logError("removing unexisting ResultSet - already closed!\r\n" + rs.toString() + "at\r\n"
                        + Connection.stacktrace(new Exception()));
            }
        }
    }

    public void close() throws java.sql.SQLException {
        connection.remove(reference);
        if (resultSets.size() > 0) {
            if (Connection.DEBUG) {
                Connection.logError("closing Statement with " + resultSets.size() + " open ResultSets at\r\n"
                        + Connection.stacktrace(new Exception()));
            }
            while (resultSets.size() > 0) {
                ResultSet rs = (ResultSet) resultSets.elements().nextElement();
                if (Connection.DEBUG) {
                    Connection.logError("closing ResultSet " + rs.toString());
                }
                try {
                    rs.close();
                } catch (Exception ex) {
                }
            }
        }
        reference.close();
    }

    public boolean execute(java.lang.String p0) throws java.sql.SQLException {
        return reference.execute(p0);
    }

    public java.sql.ResultSet executeQuery(java.lang.String p0) throws java.sql.SQLException {
        java.sql.ResultSet r = reference.executeQuery(p0);
        ResultSet s = new ResultSet(this, r);
        resultSets.put(r, s);
        return s;
    }

    public int executeUpdate(java.lang.String p0) throws java.sql.SQLException {
        return reference.executeUpdate(p0);
    }

    public int getMaxFieldSize() throws java.sql.SQLException {
        return reference.getMaxFieldSize();
    }

    public void setMaxFieldSize(int p0) throws java.sql.SQLException {
        reference.setMaxFieldSize(p0);
    }

    public int getMaxRows() throws java.sql.SQLException {
        return reference.getMaxRows();
    }

    public void setMaxRows(int p0) throws java.sql.SQLException {
        reference.setMaxRows(p0);
    }

    public void setEscapeProcessing(boolean p0) throws java.sql.SQLException {
        reference.setEscapeProcessing(p0);
    }

    public int getQueryTimeout() throws java.sql.SQLException {
        return reference.getQueryTimeout();
    }

    public void setQueryTimeout(int p0) throws java.sql.SQLException {
        reference.setQueryTimeout(p0);
    }

    public void cancel() throws java.sql.SQLException {
        reference.cancel();
    }

    public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
        return reference.getWarnings();
    }

    public void clearWarnings() throws java.sql.SQLException {
        reference.clearWarnings();
    }

    public void setCursorName(java.lang.String p0) throws java.sql.SQLException {
        reference.setCursorName(p0);
    }

    public java.sql.ResultSet getResultSet() throws java.sql.SQLException {
        java.sql.ResultSet r = reference.getResultSet();

        ResultSet s = (ResultSet) resultSets.get(r);
        if (s == null) {
            s = new ResultSet(this, r);
            resultSets.put(r, s);
        }
        return s;
    }

    public int getUpdateCount() throws java.sql.SQLException {
        return reference.getUpdateCount();
    }

    public boolean getMoreResults() throws java.sql.SQLException {
        return reference.getMoreResults();
    }

    public void setFetchDirection(int p0) throws java.sql.SQLException {
        reference.setFetchDirection(p0);
    }

    public int getFetchDirection() throws java.sql.SQLException {
        return reference.getFetchDirection();
    }

    public void setFetchSize(int p0) throws java.sql.SQLException {
        reference.setFetchSize(p0);
    }

    public int getFetchSize() throws java.sql.SQLException {
        return reference.getFetchSize();
    }

    public int getResultSetConcurrency() throws java.sql.SQLException {
        return reference.getResultSetConcurrency();
    }

    public int getResultSetType() throws java.sql.SQLException {
        return reference.getResultSetType();
    }

    public void addBatch(java.lang.String p0) throws java.sql.SQLException {
        reference.addBatch(p0);
    }

    public void clearBatch() throws java.sql.SQLException {
        reference.clearBatch();
    }

    public int[] executeBatch() throws java.sql.SQLException {
        return reference.executeBatch();
    }

    public java.sql.Connection getConnection() throws java.sql.SQLException {
        return connection;
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return reference.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return reference.execute(sql, columnIndexes);
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return reference.execute(sql, columnNames);
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return reference.executeUpdate(sql, autoGeneratedKeys);
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return reference.executeUpdate(sql, columnIndexes);
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return reference.executeUpdate(sql, columnNames);
    }

    public java.sql.ResultSet getGeneratedKeys() throws SQLException {
        return reference.getGeneratedKeys();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return reference.getMoreResults();
    }

    public int getResultSetHoldability() throws SQLException {
        return reference.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return reference.isClosed();
    }

    public boolean isPoolable() throws SQLException {
        return reference.isPoolable();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        reference.setPoolable(poolable);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return reference.isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return reference.unwrap(iface);
    }

    public void closeOnCompletion() throws SQLException {
        reference.closeOnCompletion();
    }

    public boolean isCloseOnCompletion() throws SQLException {
        return reference.isCloseOnCompletion();
    }
}
/******************************************************************************
 * $Log: Statement.java,v $
 * Revision 1.2  2012/08/11 19:57:59  bebbo
 * @I working stage
 * Revision 1.1 2011/01/01 13:12:13 bebbo
 * 
 * @N added to new CVS repo Revision 1.7 2002/12/16 19:55:08 bebbo
 * 
 * @B fixes for leakage detection
 * 
 *    Revision 1.6 2002/11/06 09:46:20 bebbo
 * @I cleanup for imports
 * 
 *    Revision 1.5 2002/08/21 09:25:04 bebbo
 * @O creation of stacktrace is only done, if it is dumped.
 * 
 *    Revision 1.4 2002/06/03 09:47:21 bebbo
 * @C fixed API doc warnings and errors
 * 
 *    Revision 1.3 2002/06/03 09:42:37 bebbo
 * @C more comments.
 * @C added CVS head/foot.
 * 
 *****************************************************************************/
