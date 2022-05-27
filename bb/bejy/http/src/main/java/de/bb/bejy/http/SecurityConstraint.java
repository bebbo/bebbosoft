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
package de.bb.bejy.http;

import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * @author bebbo
 */
class SecurityConstraint {
    private String name;
    private String roles = "";
    private HashSet<String> methods = new HashSet<String>();

    /**
     * @param name
     */
    SecurityConstraint(String name) {
        this.name = name;
    }

    /**
     * @param method
     */
    void addMethod(String method) {
        methods.add(method);
    }

    /**
     * @param role
     */
    void addRole(String role) {
        if (roles.length() > 0)
            roles += ",";
        roles += role;
    }

    /**
     * @param transport
     */
    void setTransport(String transport) {
        // TODO Auto-generated method stub

    }

    /**
     * @param method
     * @return
     */
    boolean containsMethod(String method) {
        return methods.isEmpty() || methods.contains(method) || methods.contains("*");
    }

    /**
     * @return
     */
    StringTokenizer roles() {
        return new StringTokenizer(roles, ", \t\r\n");
    }

    String getName() {
        return name;
    }

    public String toString() {
        return methods + ":" + roles;
    }

    public boolean isAllowAll() {
        final StringTokenizer st = roles();
        if (!st.hasMoreElements())
            return false;
        return st.nextToken().equals("*");
    }
}
