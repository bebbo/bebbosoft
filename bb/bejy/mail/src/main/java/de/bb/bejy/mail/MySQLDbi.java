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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An implementation of the DB interface to use a MSSQL server.
 */
public class MySQLDbi extends MailDBI {
    /**
     * ct for a MSSQL implementation of the DBI interface.
     */
    public MySQLDbi() {
        passwdFx1 = "password(";
        passwdFx2 = ")";
        concat1 = "concat(";
        concat2 = ", ";
        concat3 = ")";
        dateFx = "NOW()";
    }

    @Override
	protected String getLastInsertId(String tableName) throws SQLException {
        PreparedStatement ps = getPreparedStatement("SELECT LAST_INSERT_ID()");
        ResultSet rs = ps.executeQuery();
        String id = rs.next() ? rs.getString(1) : null;
        rs.close();
        return id;
    }
}
