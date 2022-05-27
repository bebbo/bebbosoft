/******************************************************************************
 * $Source: /export/CVS/java/de/bb/sql/src/main/java/de/bb/sql/ResultSet.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 19:57:56 $
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
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Map;

/**
 * This class is a proxy class for java.sql.ResultSet.
 * 
 * @author Stefan Bebbo Franke
 */
public class ResultSet implements java.sql.ResultSet {
    Statement statement;
    java.sql.ResultSet reference;
    private Throwable myT;

    ResultSet(Statement stmt, java.sql.ResultSet rs) {
        statement = stmt;
        reference = rs;
        myT = new Throwable();
    }

    public String toString() {
        return super.toString() + Connection.stacktrace(myT);
    }

    public byte[] getBytes(int p0) throws java.sql.SQLException {
        return reference.getBytes(p0);
    }

    public byte[] getBytes(java.lang.String p0) throws java.sql.SQLException {
        return reference.getBytes(p0);
    }

    public boolean next() throws java.sql.SQLException {
        return reference.next();
    }

    public boolean getBoolean(int p0) throws java.sql.SQLException {
        return reference.getBoolean(p0);
    }

    public boolean getBoolean(java.lang.String p0) throws java.sql.SQLException {
        return reference.getBoolean(p0);
    }

    public int getType() throws java.sql.SQLException {
        return reference.getType();
    }

    public long getLong(java.lang.String p0) throws java.sql.SQLException {
        return reference.getLong(p0);
    }

    public long getLong(int p0) throws java.sql.SQLException {
        return reference.getLong(p0);
    }

    public boolean previous() throws java.sql.SQLException {
        return reference.previous();
    }

    public void close() throws java.sql.SQLException {
        statement.remove(reference);
        try {
            reference.close();
        } catch (NullPointerException npe) {
            // for oracle
        }
    }

    public java.lang.Object getObject(int p0) throws java.sql.SQLException {
        return reference.getObject(p0);
    }

    public java.lang.Object getObject(java.lang.String p0) throws java.sql.SQLException {
        return reference.getObject(p0);
    }

    public java.sql.Ref getRef(int p0) throws java.sql.SQLException {
        return reference.getRef(p0);
    }

    public java.sql.Ref getRef(java.lang.String p0) throws java.sql.SQLException {
        return reference.getRef(p0);
    }

    public java.sql.Date getDate(int p0) throws java.sql.SQLException {
        return reference.getDate(p0);
    }

    public java.sql.Date getDate(java.lang.String p0, java.util.Calendar p1) throws java.sql.SQLException {
        return reference.getDate(p0, p1);
    }

    public java.sql.Date getDate(int p0, java.util.Calendar p1) throws java.sql.SQLException {
        return reference.getDate(p0, p1);
    }

    public java.sql.Date getDate(java.lang.String p0) throws java.sql.SQLException {
        return reference.getDate(p0);
    }

    public boolean wasNull() throws java.sql.SQLException {
        return reference.wasNull();
    }

    public java.lang.String getString(int p0) throws java.sql.SQLException {
        return reference.getString(p0);
    }

    public java.lang.String getString(java.lang.String p0) throws java.sql.SQLException {
        return reference.getString(p0);
    }

    public byte getByte(java.lang.String p0) throws java.sql.SQLException {
        return reference.getByte(p0);
    }

    public byte getByte(int p0) throws java.sql.SQLException {
        return reference.getByte(p0);
    }

    public short getShort(int p0) throws java.sql.SQLException {
        return reference.getShort(p0);
    }

    public short getShort(java.lang.String p0) throws java.sql.SQLException {
        return reference.getShort(p0);
    }

    public int getInt(int p0) throws java.sql.SQLException {
        return reference.getInt(p0);
    }

    public int getInt(java.lang.String p0) throws java.sql.SQLException {
        return reference.getInt(p0);
    }

    public float getFloat(int p0) throws java.sql.SQLException {
        return reference.getFloat(p0);
    }

    public float getFloat(java.lang.String p0) throws java.sql.SQLException {
        return reference.getFloat(p0);
    }

    public double getDouble(int p0) throws java.sql.SQLException {
        return reference.getDouble(p0);
    }

    public double getDouble(java.lang.String p0) throws java.sql.SQLException {
        return reference.getDouble(p0);
    }

    /**
     * 
     * @see java.sql.ResultSet#getBigDecimal(int, int)
     * @deprecated
     */
    public java.math.BigDecimal getBigDecimal(int p0, int p1) throws java.sql.SQLException {
        return reference.getBigDecimal(p0, p1);
    }

    public java.math.BigDecimal getBigDecimal(int p0) throws java.sql.SQLException {
        return reference.getBigDecimal(p0);
    }

    public java.math.BigDecimal getBigDecimal(java.lang.String p0) throws java.sql.SQLException {
        return reference.getBigDecimal(p0);
    }

    /**
     * 
     * @see java.sql.ResultSet#getBigDecimal(String, int)
     * @deprecated
     */
    public java.math.BigDecimal getBigDecimal(java.lang.String p0, int p1) throws java.sql.SQLException {
        return reference.getBigDecimal(p0, p1);
    }

    public java.sql.Time getTime(int p0) throws java.sql.SQLException {
        return reference.getTime(p0);
    }

    public java.sql.Time getTime(java.lang.String p0) throws java.sql.SQLException {
        return reference.getTime(p0);
    }

    public java.sql.Time getTime(java.lang.String p0, java.util.Calendar p1) throws java.sql.SQLException {
        return reference.getTime(p0, p1);
    }

    public java.sql.Time getTime(int p0, java.util.Calendar p1) throws java.sql.SQLException {
        return reference.getTime(p0, p1);
    }

    public java.sql.Timestamp getTimestamp(int p0, java.util.Calendar p1) throws java.sql.SQLException {
        return reference.getTimestamp(p0, p1);
    }

    public java.sql.Timestamp getTimestamp(java.lang.String p0) throws java.sql.SQLException {
        return reference.getTimestamp(p0);
    }

    public java.sql.Timestamp getTimestamp(java.lang.String p0, java.util.Calendar p1) throws java.sql.SQLException {
        return reference.getTimestamp(p0, p1);
    }

    public java.sql.Timestamp getTimestamp(int p0) throws java.sql.SQLException {
        return reference.getTimestamp(p0);
    }

    public java.io.InputStream getAsciiStream(java.lang.String p0) throws java.sql.SQLException {
        return reference.getAsciiStream(p0);
    }

    public java.io.InputStream getAsciiStream(int p0) throws java.sql.SQLException {
        return reference.getAsciiStream(p0);
    }

    /**
     * 
     * @see java.sql.ResultSet#getUnicodeStream(int)
     * @deprecated
     */
    public java.io.InputStream getUnicodeStream(int p0) throws java.sql.SQLException {
        return reference.getUnicodeStream(p0);
    }

    /**
     * 
     * @see java.sql.ResultSet#getUnicodeStream(String)
     * @deprecated
     */
    public java.io.InputStream getUnicodeStream(java.lang.String p0) throws java.sql.SQLException {
        return reference.getUnicodeStream(p0);
    }

    public java.io.InputStream getBinaryStream(java.lang.String p0) throws java.sql.SQLException {
        return reference.getBinaryStream(p0);
    }

    public java.io.InputStream getBinaryStream(int p0) throws java.sql.SQLException {
        return reference.getBinaryStream(p0);
    }

    public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
        return reference.getWarnings();
    }

    public void clearWarnings() throws java.sql.SQLException {
        reference.clearWarnings();
    }

    public java.lang.String getCursorName() throws java.sql.SQLException {
        return reference.getCursorName();
    }

    public java.sql.ResultSetMetaData getMetaData() throws java.sql.SQLException {
        return reference.getMetaData();
    }

    public int findColumn(java.lang.String p0) throws java.sql.SQLException {
        return reference.findColumn(p0);
    }

    public java.io.Reader getCharacterStream(java.lang.String p0) throws java.sql.SQLException {
        return reference.getCharacterStream(p0);
    }

    public java.io.Reader getCharacterStream(int p0) throws java.sql.SQLException {
        return reference.getCharacterStream(p0);
    }

    public boolean isBeforeFirst() throws java.sql.SQLException {
        return reference.isBeforeFirst();
    }

    public boolean isAfterLast() throws java.sql.SQLException {
        return reference.isAfterLast();
    }

    public boolean isFirst() throws java.sql.SQLException {
        return reference.isFirst();
    }

    public boolean isLast() throws java.sql.SQLException {
        return reference.isLast();
    }

    public void beforeFirst() throws java.sql.SQLException {
        reference.beforeFirst();
    }

    public void afterLast() throws java.sql.SQLException {
        reference.afterLast();
    }

    public boolean first() throws java.sql.SQLException {
        return reference.first();
    }

    public boolean last() throws java.sql.SQLException {
        return reference.last();
    }

    public int getRow() throws java.sql.SQLException {
        return reference.getRow();
    }

    public boolean absolute(int p0) throws java.sql.SQLException {
        return reference.absolute(p0);
    }

    public boolean relative(int p0) throws java.sql.SQLException {
        return reference.relative(p0);
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

    public int getConcurrency() throws java.sql.SQLException {
        return reference.getConcurrency();
    }

    public boolean rowUpdated() throws java.sql.SQLException {
        return reference.rowUpdated();
    }

    public boolean rowInserted() throws java.sql.SQLException {
        return reference.rowInserted();
    }

    public boolean rowDeleted() throws java.sql.SQLException {
        return reference.rowDeleted();
    }

    public void updateNull(int p0) throws java.sql.SQLException {
        reference.updateNull(p0);
    }

    public void updateNull(java.lang.String p0) throws java.sql.SQLException {
        reference.updateNull(p0);
    }

    public void updateBoolean(int p0, boolean p1) throws java.sql.SQLException {
        reference.updateBoolean(p0, p1);
    }

    public void updateBoolean(java.lang.String p0, boolean p1) throws java.sql.SQLException {
        reference.updateBoolean(p0, p1);
    }

    public void updateByte(java.lang.String p0, byte p1) throws java.sql.SQLException {
        reference.updateByte(p0, p1);
    }

    public void updateByte(int p0, byte p1) throws java.sql.SQLException {
        reference.updateByte(p0, p1);
    }

    public void updateShort(int p0, short p1) throws java.sql.SQLException {
        reference.updateShort(p0, p1);
    }

    public void updateShort(java.lang.String p0, short p1) throws java.sql.SQLException {
        reference.updateShort(p0, p1);
    }

    public void updateInt(int p0, int p1) throws java.sql.SQLException {
        reference.updateInt(p0, p1);
    }

    public void updateInt(java.lang.String p0, int p1) throws java.sql.SQLException {
        reference.updateInt(p0, p1);
    }

    public void updateLong(int p0, long p1) throws java.sql.SQLException {
        reference.updateLong(p0, p1);
    }

    public void updateLong(java.lang.String p0, long p1) throws java.sql.SQLException {
        reference.updateLong(p0, p1);
    }

    public void updateFloat(java.lang.String p0, float p1) throws java.sql.SQLException {
        reference.updateFloat(p0, p1);
    }

    public void updateFloat(int p0, float p1) throws java.sql.SQLException {
        reference.updateFloat(p0, p1);
    }

    public void updateDouble(int p0, double p1) throws java.sql.SQLException {
        reference.updateDouble(p0, p1);
    }

    public void updateDouble(java.lang.String p0, double p1) throws java.sql.SQLException {
        reference.updateDouble(p0, p1);
    }

    public void updateBigDecimal(java.lang.String p0, java.math.BigDecimal p1) throws java.sql.SQLException {
        reference.updateBigDecimal(p0, p1);
    }

    public void updateBigDecimal(int p0, java.math.BigDecimal p1) throws java.sql.SQLException {
        reference.updateBigDecimal(p0, p1);
    }

    public void updateString(java.lang.String p0, java.lang.String p1) throws java.sql.SQLException {
        reference.updateString(p0, p1);
    }

    public void updateString(int p0, java.lang.String p1) throws java.sql.SQLException {
        reference.updateString(p0, p1);
    }

    public void updateBytes(int p0, byte[] p1) throws java.sql.SQLException {
        reference.updateBytes(p0, p1);
    }

    public void updateBytes(java.lang.String p0, byte[] p1) throws java.sql.SQLException {
        reference.updateBytes(p0, p1);
    }

    public void updateDate(int p0, java.sql.Date p1) throws java.sql.SQLException {
        reference.updateDate(p0, p1);
    }

    public void updateDate(java.lang.String p0, java.sql.Date p1) throws java.sql.SQLException {
        reference.updateDate(p0, p1);
    }

    public void updateTime(java.lang.String p0, java.sql.Time p1) throws java.sql.SQLException {
        reference.updateTime(p0, p1);
    }

    public void updateTime(int p0, java.sql.Time p1) throws java.sql.SQLException {
        reference.updateTime(p0, p1);
    }

    public void updateTimestamp(int p0, java.sql.Timestamp p1) throws java.sql.SQLException {
        reference.updateTimestamp(p0, p1);
    }

    public void updateTimestamp(java.lang.String p0, java.sql.Timestamp p1) throws java.sql.SQLException {
        reference.updateTimestamp(p0, p1);
    }

    public void updateAsciiStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        reference.updateAsciiStream(p0, p1, p2);
    }

    public void updateAsciiStream(java.lang.String p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        reference.updateAsciiStream(p0, p1, p2);
    }

    public void updateBinaryStream(int p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        reference.updateBinaryStream(p0, p1, p2);
    }

    public void updateBinaryStream(java.lang.String p0, java.io.InputStream p1, int p2) throws java.sql.SQLException {
        reference.updateBinaryStream(p0, p1, p2);
    }

    public void updateCharacterStream(int p0, java.io.Reader p1, int p2) throws java.sql.SQLException {
        reference.updateCharacterStream(p0, p1, p2);
    }

    public void updateCharacterStream(java.lang.String p0, java.io.Reader p1, int p2) throws java.sql.SQLException {
        reference.updateCharacterStream(p0, p1, p2);
    }

    public void updateObject(int p0, java.lang.Object p1) throws java.sql.SQLException {
        reference.updateObject(p0, p1);
    }

    public void updateObject(int p0, java.lang.Object p1, int p2) throws java.sql.SQLException {
        reference.updateObject(p0, p1, p2);
    }

    public void updateObject(java.lang.String p0, java.lang.Object p1) throws java.sql.SQLException {
        reference.updateObject(p0, p1);
    }

    public void updateObject(java.lang.String p0, java.lang.Object p1, int p2) throws java.sql.SQLException {
        reference.updateObject(p0, p1, p2);
    }

    public void insertRow() throws java.sql.SQLException {
        reference.insertRow();
    }

    public void updateRow() throws java.sql.SQLException {
        reference.updateRow();
    }

    public void deleteRow() throws java.sql.SQLException {
        reference.deleteRow();
    }

    public void refreshRow() throws java.sql.SQLException {
        reference.refreshRow();
    }

    public void cancelRowUpdates() throws java.sql.SQLException {
        reference.cancelRowUpdates();
    }

    public void moveToInsertRow() throws java.sql.SQLException {
        reference.moveToInsertRow();
    }

    public void moveToCurrentRow() throws java.sql.SQLException {
        reference.moveToCurrentRow();
    }

    public java.sql.Statement getStatement() throws java.sql.SQLException {
        return statement;
    }

    public java.sql.Blob getBlob(int p0) throws java.sql.SQLException {
        return reference.getBlob(p0);
    }

    public java.sql.Blob getBlob(java.lang.String p0) throws java.sql.SQLException {
        return reference.getBlob(p0);
    }

    public java.sql.Clob getClob(java.lang.String p0) throws java.sql.SQLException {
        return reference.getClob(p0);
    }

    public java.sql.Clob getClob(int p0) throws java.sql.SQLException {
        return reference.getClob(p0);
    }

    public java.sql.Array getArray(int p0) throws java.sql.SQLException {
        return reference.getArray(p0);
    }

    public java.sql.Array getArray(java.lang.String p0) throws java.sql.SQLException {
        return reference.getArray(p0);
    }

    public int getHoldability() throws SQLException {
        return reference.getHoldability();
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return reference.getNCharacterStream(columnIndex);
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return reference.getNCharacterStream(columnLabel);
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        return reference.getNClob(columnIndex);
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        return reference.getNClob(columnLabel);
    }

    public String getNString(int columnIndex) throws SQLException {
        return reference.getNString(columnIndex);
    }

    public String getNString(String columnLabel) throws SQLException {
        return reference.getNString(columnLabel);
    }

    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return reference.getObject(columnIndex, map);
    }

    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return reference.getObject(columnLabel, map);
    }

    public RowId getRowId(int columnIndex) throws SQLException {
        return reference.getRowId(columnIndex);
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        return reference.getRowId(columnLabel);
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return reference.getSQLXML(columnIndex);
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return reference.getSQLXML(columnLabel);
    }

    public URL getURL(int columnIndex) throws SQLException {
        return reference.getURL(columnIndex);
    }

    public URL getURL(String columnLabel) throws SQLException {
        return reference.getURL(columnLabel);
    }

    public boolean isClosed() throws SQLException {
        return reference.isClosed();
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        reference.updateArray(columnIndex, x);
    }

    public void updateArray(String columnLabel, Array x) throws SQLException {
        reference.updateArray(columnLabel, x);
    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        reference.updateAsciiStream(columnIndex, x);
    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        reference.updateAsciiStream(columnLabel, x);
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        reference.updateAsciiStream(columnIndex, x, length);
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        reference.updateAsciiStream(columnLabel, x, length);
    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        reference.updateBinaryStream(columnIndex, x);
    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        reference.updateBinaryStream(columnLabel, x);
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        reference.updateBinaryStream(columnIndex, x, length);
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        reference.updateBinaryStream(columnLabel, x, length);
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        reference.updateBlob(columnIndex, x);
    }

    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        reference.updateBlob(columnLabel, x);
    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        reference.updateBlob(columnIndex, inputStream);
    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        reference.updateBlob(columnLabel, inputStream);
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        reference.updateBlob(columnIndex, inputStream, length);
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        reference.updateBlob(columnLabel, inputStream, length);
    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        reference.updateCharacterStream(columnIndex, x);
    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        reference.updateCharacterStream(columnLabel, reader);
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        reference.updateCharacterStream(columnIndex, x, length);
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        reference.updateCharacterStream(columnLabel, reader, length);
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        reference.updateClob(columnIndex, x);
    }

    public void updateClob(String columnLabel, Clob x) throws SQLException {
        reference.updateClob(columnLabel, x);
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        reference.updateClob(columnIndex, reader);
    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        reference.updateClob(columnLabel, reader);
    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        reference.updateClob(columnIndex, reader, length);
    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        reference.updateClob(columnLabel, reader, length);
    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        reference.updateNCharacterStream(columnIndex, x);
    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        reference.updateNCharacterStream(columnLabel, reader);
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        reference.updateNCharacterStream(columnIndex, x, length);
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        reference.updateNCharacterStream(columnLabel, reader, length);
    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        reference.updateNClob(columnIndex, nClob);
    }

    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        reference.updateNClob(columnLabel, nClob);
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        reference.updateNClob(columnIndex, reader);
    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        reference.updateNClob(columnLabel, reader);
    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        reference.updateNClob(columnIndex, reader, length);
    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        reference.updateNClob(columnLabel, reader, length);
    }

    public void updateNString(int columnIndex, String nString) throws SQLException {
        reference.updateNString(columnIndex, nString);
    }

    public void updateNString(String columnLabel, String nString) throws SQLException {
        reference.updateNString(columnLabel, nString);
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        reference.updateRef(columnIndex, x);
    }

    public void updateRef(String columnLabel, Ref x) throws SQLException {
        reference.updateRef(columnLabel, x);
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        reference.updateRowId(columnIndex, x);
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        reference.updateRowId(columnLabel, x);
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        reference.updateSQLXML(columnIndex, xmlObject);
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        reference.updateSQLXML(columnLabel, xmlObject);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return reference.isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return reference.unwrap(iface);
    }

    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return reference.getObject(columnIndex, type);
    }

    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return reference.getObject(columnLabel, type);
    }
}
/******************************************************************************
 * $Log: ResultSet.java,v $
 * Revision 1.2  2012/08/11 19:57:56  bebbo
 * @I working stage
 * Revision 1.1 2011/01/01 13:12:22 bebbo
 * 
 * @N added to new CVS repo Revision 1.8 2003/01/07 18:32:24 bebbo
 * 
 * @W removed some deprecated warnings
 * 
 *    Revision 1.7 2002/12/16 19:55:08 bebbo
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
