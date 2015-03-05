/******************************************************************************
 * $Source: /export/CVS/java/de/bb/sql/src/main/java/de/bb/sql/CallableStatement.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 19:57:55 $
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
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * This class is a proxy class for java.sql.CallableStatement. In addtion to the wrapped class all ResultSets are
 * tracked, and checked.
 * 
 * @author Stefan Bebbo Franke
 */
public class CallableStatement extends PreparedStatement implements java.sql.CallableStatement {
    CallableStatement(Connection con, java.sql.PreparedStatement p) {
        super(con, p);
    }

    public byte[] getBytes(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getBytes(p0);
    }

    public boolean getBoolean(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getBoolean(p0);
    }

    public long getLong(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getLong(p0);
    }

    public java.lang.Object getObject(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getObject(p0);
    }

    public <T> T getObject(int p0, Class<T> clazz) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getObject(p0, clazz);
    }

    public java.sql.Ref getRef(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getRef(p0);
    }

    public java.sql.Date getDate(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getDate(p0);
    }

    public java.sql.Date getDate(int p0, java.util.Calendar p1) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getDate(p0, p1);
    }

    public void registerOutParameter(int p0, int p1) throws java.sql.SQLException {
        ((java.sql.CallableStatement) reference).registerOutParameter(p0, p1);
    }

    public void registerOutParameter(int p0, int p1, java.lang.String p2) throws java.sql.SQLException {
        ((java.sql.CallableStatement) reference).registerOutParameter(p0, p1, p2);
    }

    public void registerOutParameter(int p0, int p1, int p2) throws java.sql.SQLException {
        ((java.sql.CallableStatement) reference).registerOutParameter(p0, p1, p2);
    }

    public boolean wasNull() throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).wasNull();
    }

    public java.lang.String getString(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getString(p0);
    }

    public byte getByte(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getByte(p0);
    }

    public short getShort(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getShort(p0);
    }

    public int getInt(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getInt(p0);
    }

    public float getFloat(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getFloat(p0);
    }

    public double getDouble(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getDouble(p0);
    }

    /**
     * @see java.sql.CallableStatement#getBigDecimal(int, int)
     * @deprecated
     */
    public java.math.BigDecimal getBigDecimal(int p0, int p1) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getBigDecimal(p0, p1);
    }

    public java.math.BigDecimal getBigDecimal(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getBigDecimal(p0);
    }

    public java.sql.Time getTime(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getTime(p0);
    }

    public java.sql.Time getTime(int p0, java.util.Calendar p1) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getTime(p0, p1);
    }

    public java.sql.Timestamp getTimestamp(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getTimestamp(p0);
    }

    public java.sql.Timestamp getTimestamp(int p0, java.util.Calendar p1) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getTimestamp(p0, p1);
    }

    public java.sql.Blob getBlob(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getBlob(p0);
    }

    public java.sql.Clob getClob(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getClob(p0);
    }

    public java.sql.Array getArray(int p0) throws java.sql.SQLException {
        return ((java.sql.CallableStatement) reference).getArray(p0);
    }

    public Array getArray(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getArray(parameterName);
    }

    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getBigDecimal(parameterName);
    }

    public Blob getBlob(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getBlob(parameterName);
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getBoolean(parameterName);
    }

    public byte getByte(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getByte(parameterName);
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getBytes(parameterName);
    }

    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getCharacterStream(parameterIndex);
    }

    public Reader getCharacterStream(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getCharacterStream(parameterName);
    }

    public Clob getClob(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getClob(parameterName);
    }

    public Date getDate(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getDate(parameterName);
    }

    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return ((java.sql.CallableStatement) reference).getDate(parameterName, cal);
    }

    public double getDouble(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getDouble(parameterName);
    }

    public float getFloat(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getFloat(parameterName);
    }

    public int getInt(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getInt(parameterName);
    }

    public long getLong(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getLong(parameterName);
    }

    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getNCharacterStream(parameterIndex);
    }

    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getNCharacterStream(parameterName);
    }

    public NClob getNClob(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getNClob(parameterIndex);
    }

    public NClob getNClob(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getNClob(parameterName);
    }

    public String getNString(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getNString(parameterIndex);
    }

    public String getNString(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getNString(parameterName);
    }

    public Object getObject(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getObject(parameterName);
    }

    public <T> T getObject(String parameterName, Class<T> clazz) throws SQLException {
        return ((java.sql.CallableStatement) reference).getObject(parameterName, clazz);
    }

    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return ((java.sql.CallableStatement) reference).getObject(parameterIndex, map);
    }

    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return ((java.sql.CallableStatement) reference).getObject(parameterName, map);
    }

    public Ref getRef(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getRef(parameterName);
    }

    public RowId getRowId(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getRowId(parameterIndex);
    }

    public RowId getRowId(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getRowId(parameterName);
    }

    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getSQLXML(parameterIndex);
    }

    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getSQLXML(parameterName);
    }

    public short getShort(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getShort(parameterName);
    }

    public String getString(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getString(parameterName);
    }

    public Time getTime(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getTime(parameterName);
    }

    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return ((java.sql.CallableStatement) reference).getTime(parameterName, cal);
    }

    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getTimestamp(parameterName);
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return ((java.sql.CallableStatement) reference).getTimestamp(parameterName, cal);
    }

    public URL getURL(int parameterIndex) throws SQLException {
        return ((java.sql.CallableStatement) reference).getURL(parameterIndex);
    }

    public URL getURL(String parameterName) throws SQLException {
        return ((java.sql.CallableStatement) reference).getURL(parameterName);
    }

    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        ((java.sql.CallableStatement) reference).registerOutParameter(parameterName, sqlType);
    }

    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        ((java.sql.CallableStatement) reference).registerOutParameter(parameterName, sqlType, scale);

    }

    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        ((java.sql.CallableStatement) reference).registerOutParameter(parameterName, sqlType, typeName);
    }

    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        ((java.sql.CallableStatement) reference).setAsciiStream(parameterName, x);
    }

    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        ((java.sql.CallableStatement) reference).setAsciiStream(parameterName, x, length);
    }

    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setAsciiStream(parameterName, x, length);
    }

    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        ((java.sql.CallableStatement) reference).setBigDecimal(parameterName, x);
    }

    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        ((java.sql.CallableStatement) reference).setBinaryStream(parameterName, x);
    }

    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        ((java.sql.CallableStatement) reference).setBinaryStream(parameterName, x, length);
    }

    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setBinaryStream(parameterName, x, length);
    }

    public void setBlob(String parameterName, Blob x) throws SQLException {
        ((java.sql.CallableStatement) reference).setBlob(parameterName, x);
    }

    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        ((java.sql.CallableStatement) reference).setBlob(parameterName, inputStream);
    }

    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setBlob(parameterName, inputStream, length);

    }

    public void setBoolean(String parameterName, boolean x) throws SQLException {
        ((java.sql.CallableStatement) reference).setBoolean(parameterName, x);
    }

    public void setByte(String parameterName, byte x) throws SQLException {
        ((java.sql.CallableStatement) reference).setByte(parameterName, x);
    }

    public void setBytes(String parameterName, byte[] x) throws SQLException {
        ((java.sql.CallableStatement) reference).setBytes(parameterName, x);
    }

    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        ((java.sql.CallableStatement) reference).setCharacterStream(parameterName, reader);
    }

    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        ((java.sql.CallableStatement) reference).setCharacterStream(parameterName, reader, length);
    }

    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setCharacterStream(parameterName, reader, length);
    }

    public void setClob(String parameterName, Clob x) throws SQLException {
        ((java.sql.CallableStatement) reference).setClob(parameterName, x);
    }

    public void setClob(String parameterName, Reader reader) throws SQLException {
        ((java.sql.CallableStatement) reference).setClob(parameterName, reader);
    }

    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setClob(parameterName, reader, length);
    }

    public void setDate(String parameterName, Date x) throws SQLException {
        ((java.sql.CallableStatement) reference).setDate(parameterName, x);
    }

    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        ((java.sql.CallableStatement) reference).setDate(parameterName, x, cal);
    }

    public void setDouble(String parameterName, double x) throws SQLException {
        ((java.sql.CallableStatement) reference).setDouble(parameterName, x);
    }

    public void setFloat(String parameterName, float x) throws SQLException {
        ((java.sql.CallableStatement) reference).setFloat(parameterName, x);
    }

    public void setInt(String parameterName, int x) throws SQLException {
        ((java.sql.CallableStatement) reference).setInt(parameterName, x);
    }

    public void setLong(String parameterName, long x) throws SQLException {
        ((java.sql.CallableStatement) reference).setLong(parameterName, x);
    }

    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        ((java.sql.CallableStatement) reference).setNCharacterStream(parameterName, value);
    }

    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setNCharacterStream(parameterName, value, length);
    }

    public void setNClob(String parameterName, NClob value) throws SQLException {
        ((java.sql.CallableStatement) reference).setNClob(parameterName, value);
    }

    public void setNClob(String parameterName, Reader reader) throws SQLException {
        ((java.sql.CallableStatement) reference).setNClob(parameterName, reader);
    }

    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        ((java.sql.CallableStatement) reference).setNClob(parameterName, reader, length);
    }

    public void setNString(String parameterName, String value) throws SQLException {
        ((java.sql.CallableStatement) reference).setNString(parameterName, value);
    }

    public void setNull(String parameterName, int sqlType) throws SQLException {
        ((java.sql.CallableStatement) reference).setNull(parameterName, sqlType);
    }

    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        ((java.sql.CallableStatement) reference).setNull(parameterName, sqlType, typeName);
    }

    public void setObject(String parameterName, Object x) throws SQLException {
        ((java.sql.CallableStatement) reference).setObject(parameterName, x);
    }

    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        ((java.sql.CallableStatement) reference).setObject(parameterName, x, targetSqlType);
    }

    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        ((java.sql.CallableStatement) reference).setObject(parameterName, x, targetSqlType, scale);
    }

    public void setRowId(String parameterName, RowId x) throws SQLException {
        ((java.sql.CallableStatement) reference).setRowId(parameterName, x);
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        ((java.sql.CallableStatement) reference).setSQLXML(parameterName, xmlObject);
    }

    public void setShort(String parameterName, short x) throws SQLException {
        ((java.sql.CallableStatement) reference).setShort(parameterName, x);
    }

    public void setString(String parameterName, String x) throws SQLException {
        ((java.sql.CallableStatement) reference).setString(parameterName, x);
    }

    public void setTime(String parameterName, Time x) throws SQLException {
        ((java.sql.CallableStatement) reference).setTime(parameterName, x);
    }

    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        ((java.sql.CallableStatement) reference).setTime(parameterName, x, cal);
    }

    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        ((java.sql.CallableStatement) reference).setTimestamp(parameterName, x);
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        ((java.sql.CallableStatement) reference).setTimestamp(parameterName, x, cal);
    }

    public void setURL(String parameterName, URL val) throws SQLException {
        ((java.sql.CallableStatement) reference).setURL(parameterName, val);
    }
}
/******************************************************************************
 * $Log: CallableStatement.java,v $
 * Revision 1.2  2012/08/11 19:57:55  bebbo
 * @I working stage
 * Revision 1.1 2011/01/01 13:12:25 bebbo
 * 
 * @N added to new CVS repo Revision 1.4 2003/01/07 18:32:24 bebbo
 * 
 * @W removed some deprecated warnings
 * 
 *    Revision 1.3 2002/11/06 09:46:20 bebbo
 * @I cleanup for imports
 * 
 *    Revision 1.2 2002/06/03 09:42:37 bebbo
 * @C more comments.
 * @C added CVS head/foot.
 * 
 *****************************************************************************/
