/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/SecurityConstraint.java,v $
 * $Revision: 1.3 $
 * $Date: 2014/06/23 15:38:46 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
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

  (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

  Created on 16.04.2004

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

/******************************************************************************
 * Log: $Log: SecurityConstraint.java,v $
 * Log: Revision 1.3  2014/06/23 15:38:46  bebbo
 * Log: @N implemented form authentication
 * Log: @R reworked authentication handling to support roles
 * Log: Log: Revision 1.2 2006/03/17 11:30:28 bebbo Log: @N added a getName() method
 * Log: Log: Revision 1.1 2004/04/20 13:23:59 bebbo Log: @N new Log:
 ******************************************************************************/
