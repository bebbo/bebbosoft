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

class SpoolEntry {
    String msgId;

    String mailId;

    String toName;

    String toDomain;

    int retry;

    String fromName;

    String fromDomain;

    SpoolEntry(String _msgId, String _mailId, String _name, String _domain, int _retry, String _s_name, String _s_domain) {
        msgId = _msgId;
        mailId = _mailId;
        toName = _name;
        toDomain = _domain;
        retry = _retry;
        fromName = _s_name;
        fromDomain = _s_domain;
    }
}

