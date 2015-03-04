/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/UserGroupDbi.java,v $
 * $Revision: 1.4 $
 * $Date: 2014/06/23 19:02:58 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
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

  (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/
 
package de.bb.bejy;

import java.util.Collection;

/**
 * Interface for all user group implementations.
 * @author bebbo
 */
public interface UserGroupDbi
{
  /**
   * verifies whether the user exists in the specified group and whether the password is valid.
   * @param user the user name
   * @param pass the password
   * @return a collection of the user's groups or null if not a valid login.
   */
  public Collection<String> verifyUserGroup(String user, String pass);
}
 
/******************************************************************************
 * $Log: UserGroupDbi.java,v $
 * Revision 1.4  2014/06/23 19:02:58  bebbo
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 * Revision 1.3  2003/06/24 19:47:34  bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 *
 * Revision 1.2  2002/05/16 15:19:48  franke
 * @C CVS
 *
 * Revision 1.1  2001/03/30 17:27:25  bebbo
 * @N new
 *
 *****************************************************************************/