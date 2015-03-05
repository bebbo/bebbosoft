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

import java.util.Collection;

import de.bb.bejy.UserGroupDbi;

public class FormVerification implements UserGroupDbi {

    String loginPage;
    String loginErrorPage;
    ServletHandler handler;

    public FormVerification(final String loginPage, final String loginErrorPage, ServletHandler h) {
        this.loginPage = loginPage;
        this.loginErrorPage = loginErrorPage == null ? null : loginErrorPage.trim();
        this.handler = h;
    }

    public Collection<String> verifyUserGroup(String user, String pass) {
        return null;
    }

}
