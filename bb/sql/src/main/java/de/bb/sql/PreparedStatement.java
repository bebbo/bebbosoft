/******************************************************************************
 * $Source: /export/CVS/java/de/bb/sql/src/main/java/de/bb/sql/PreparedStatement.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 19:57:54 $
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

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * This class is a proxy class for java.sql.PreparedStatement. In addtion to the wrapped class all ResultSets are
 * tracked, and checked.
 * 
 * @author Stefan Bebbo Franke
 */
public class PreparedStatement extends de.bb.sql.Statement implements java.sql.PreparedStatement {

    PreparedStatement(Connection con, java.sql.PreparedStatement p) {
        super(con, p);
    }

    public boolean execute() throws java.sql.SQLException {
        return ((java.sql.PreparedStatement) reference).execute();
    }

    public java.sql.ResultSet executeQuery() throws java.sql.SQLException {
        java.sql.ResultSet r = ((java.sql.PreparedStatement) reference).executeQuery();
        ResultSet s = new ResultSet(this, r);
        resultSets.put(r, s);
        return s;
    }

    public int executeUpdate() throws java.sql.SQLException {
        return ((java.sql.PreparedStatement) reference).executeUpdate();
    }

    public void setNull(int p0, int p1, java.lang.String p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setNull(p0, p1, p2);
    }

    public void setNull(int p0, int p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setNull(p0, p1);
    }

    public void setBoolean(int p0, boolean p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setBoolean(p0, p1);
    }

    public void setByte(int p0, byte p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setByte(p0, p1);
    }

    public void setShort(int p0, short p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setShort(p0, p1);
    }

    public void setInt(int p0, int p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setInt(p0, p1);
    }

    public void setLong(int p0, long p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setLong(p0, p1);
    }

    public void setFloat(int p0, float p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setFloat(p0, p1);
    }

    public void setDouble(int p0, double p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setDouble(p0, p1);
    }

    public void setBigDecimal(int p0, java.math.BigDecimal p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setBigDecimal(p0, p1);
    }

    public void setString(int p0, java.lang.String p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setString(p0, p1);
    }

    public void setBytes(int p0, byte[] p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setBytes(p0, p1);
    }

    public void setDate(int p0, java.sql.Date p1, java.util.Calendar p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setDate(p0, p1, p2);
    }

    public void setDate(int p0, java.sql.Date p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setDate(p0, p1);
    }

    public void setTime(int p0, java.sql.Time p1, java.util.Calendar p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setTime(p0, p1, p2);
    }

    public void setTime(int p0, java.sql.Time p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setTime(p0, p1);
    }

    public void setTimestamp(int p0, java.sql.Timestamp p1, java.util.Calendar p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setTimestamp(p0, p1, p2);
    }

    public void setTimestamp(int p0, java.sql.Timestamp p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setTimestamp(p0, p1);
    }

    public void setAsciiStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setAsciiStream(p0, p1, p2);
    }

    /**
     * @see java.sql.PreparedStatement#setUnicodeStream(int, InputStream, int)
     * @deprecated
     */
    public void setUnicodeStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setUnicodeStream(p0, p1, p2);
    }

    public void setBinaryStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setBinaryStream(p0, p1, p2);
    }

    public void clearParameters() throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).clearParameters();
    }

    public void setObject(int p0, java.lang.Object p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setObject(p0, p1);
    }

    public void setObject(int p0, java.lang.Object p1, int p2, int p3) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setObject(p0, p1, p2, p3);
    }

    public void setObject(int p0, java.lang.Object p1, int p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setObject(p0, p1, p2);
    }

    public void addBatch() throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).addBatch();
    }

    public void setCharacterStream(int p0, java.io.Reader p1, int p2) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setCharacterStream(p0, p1, p2);
    }

    public void setRef(int p0, java.sql.Ref p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setRef(p0, p1);
    }

    public void setBlob(int p0, java.sql.Blob p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setBlob(p0, p1);
    }

    public void setClob(int p0, java.sql.Clob p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setClob(p0, p1);
    }

    public void setArray(int p0, java.sql.Array p1) throws java.sql.SQLException {
        ((java.sql.PreparedStatement) reference).setArray(p0, p1);
    }

    public java.sql.ResultSetMetaData getMetaData() throws java.sql.SQLException {
        return ((java.sql.PreparedStatement) reference).getMetaData();
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return ((java.sql.PreparedStatement) reference).getParameterMetaData();
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        ((java.sql.PreparedStatement) reference).setAsciiStream(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        ((java.sql.PreparedStatement) reference).setBinaryStream(parameterIndex, x);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setBinaryStream(parameterIndex, x, length);
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        ((java.sql.PreparedStatement) reference).setBlob(parameterIndex, inputStream);
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setBlob(parameterIndex, inputStream, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        ((java.sql.PreparedStatement) reference).setCharacterStream(parameterIndex, reader);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setCharacterStream(parameterIndex, reader, length);
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        ((java.sql.PreparedStatement) reference).setClob(parameterIndex, reader);
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setCharacterStream(parameterIndex, reader, length);
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        ((java.sql.PreparedStatement) reference).setNCharacterStream(parameterIndex, value);
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        ((java.sql.PreparedStatement) reference).setNClob(parameterIndex, value);
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        ((java.sql.PreparedStatement) reference).setNCharacterStream(parameterIndex, reader);
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        ((java.sql.PreparedStatement) reference).setNClob(parameterIndex, reader, length);
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        ((java.sql.PreparedStatement) reference).setNString(parameterIndex, value);
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        ((java.sql.PreparedStatement) reference).setRowId(parameterIndex, x);
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        ((java.sql.PreparedStatement) reference).setSQLXML(parameterIndex, xmlObject);
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        ((java.sql.PreparedStatement) reference).setURL(parameterIndex, x);
    }
}
/******************************************************************************
 * $Log: PreparedStatement.java,v $
 * Revision 1.2  2012/08/11 19:57:54  bebbo
 * @I working stage
 * Revision 1.1 2011/01/01 13:12:31 bebbo
 * 
 * @N added to new CVS repo Revision 1.5 2003/01/07 18:32:24 bebbo
 * 
 * @W removed some deprecated warnings
 * 
 *    Revision 1.4 2002/12/16 19:55:08 bebbo
 * @B fixes for leakage detection
 * 
 *    Revision 1.3 2002/11/06 09:46:20 bebbo
 * @I cleanup for imports
 * 
 *    Revision 1.2 2002/06/03 09:42:37 bebbo
 * @C more comments.
 * @C added CVS head/foot.
 * 
 *****************************************************************************/
