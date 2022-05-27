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
public class HSQLDbi extends MailDBI {
    /**
     * ct for a MSSQL implementation of the DBI interface.
     */
    public HSQLDbi() {
        passwdFx1 = "";
        passwdFx2 = "";
        concat1 = "(";
        concat2 = " + ";
        concat3 = ")";
        dateFx = "CURRENT_TIMESTAMP";
    }

    protected String getLastInsertId(String tableName) throws SQLException {
        PreparedStatement ps = getPreparedStatement("SELECT value from SYSTEM_SESSIONINFO where key='IDENTITY'");
        ResultSet rs = ps.executeQuery();
        String id = rs.next() ? rs.getString(1) : null;
        rs.close();
        return id;
    }
}
