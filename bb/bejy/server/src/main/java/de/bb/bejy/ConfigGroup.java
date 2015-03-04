/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/ConfigGroup.java,v $
 * $Revision: 1.6 $
 * $Date: 2014/09/22 09:23:55 $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import de.bb.security.Pkcs5;
import de.bb.util.LogFile;

/**
 * Implementation of the de.bb.bejy.UserGroupDbi interface, which uses BEJY's config file.
 */
public class ConfigGroup extends Configurable implements UserGroupDbi, Loadable {
    private final static String[][] PROPERTIES = { { "name", "group name" }, };

    private static final Collection<String> DEFAULT = new ArrayList<String>();
    
    {
        DEFAULT.add("DEFAULT");
    }

    private HashMap<String, String> user2Password = new HashMap<String, String>();
    private HashMap<String, Collection<String>> user2Roles = new HashMap<String, Collection<String>>();

    /**
     * Creates a user group which is maintained in the config file.
     */
    public ConfigGroup() {
        init("user group", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.inigroup";
    }

    /**
     * Is a user with that password in the specified group?
     * 
     * @param user
     *            the user name
     * @param password
     *            the passwrod
     * @return true if that user/password/group combination exists, false either.
     */
    public Collection<String> verifyUserGroup(String user, String password) {
        if (user == null || password == null)
            return null;
        String pass2 = user2Password.get(user);
        if (pass2 == null)
            return null;
        if (pass2.startsWith("{P5")) {
            if (Pkcs5.verifyPBKDF2(pass2, password))
                return user2Roles.get(user);
        } else 
        if (password.equals(pass2))
            return user2Roles.get(user);

        return null;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#activate(de.bb.util.LogFile)
     */
    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            if (!(c instanceof UserCfg))
                continue;
            UserCfg u = (UserCfg) c;
            String name = u.getProperty("name");
            String pass = u.getProperty("password", "");
            user2Password.put(name, pass);
            user2Roles.put(name, u.getRoles());
        }
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#deactivate(de.bb.util.LogFile)
     */
    public void deactivate(LogFile logFile) throws Exception {
        user2Password.clear();
        super.deactivate(logFile);
    }

    /**
     * Returns the id of the Configurator. The config group extends a "de.bb.bejy.group".
     * 
     * @return the id of the Configurator.
     */
    public String getImplementationId() {
        return "de.bb.bejy.group";
    }
}

/******************************************************************************
 * $Log: ConfigGroup.java,v $
 * Revision 1.6  2014/09/22 09:23:55  bebbo
 * @N added support for user roles
 *
 * Revision 1.5  2014/06/23 19:02:58  bebbo
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 * Revision 1.4 2013/06/18 13:23:36 bebbo
 * 
 * @I preparations to use nio sockets
 * @V 1.5.1.68 Revision 1.3 2004/04/20 13:20:04 bebbo
 * 
 * @B fixed possible NPE if password is null
 * 
 *    Revision 1.2 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 *    Revision 1.1 2003/07/01 12:24:01 bebbo
 * @R renamed IniUserGroup to ConfigUserGroup
 * 
 *    Revision 1.9 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *    Revision 1.8 2003/06/23 14:30:09 bebbo
 * @N passwords are no longer stored as clear text
 * 
 *    Revision 1.7 2003/06/18 08:36:56 bebbo
 * @R modification, dynamic loading, removing - all works now
 * 
 *    Revision 1.6 2003/06/17 15:13:36 bebbo
 * @R more changes to enable on the fly config updates
 * 
 *    Revision 1.5 2003/06/17 12:10:03 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.4 2003/06/17 10:18:10 bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.3 2003/05/13 15:42:07 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.2 2002/05/16 15:19:48 franke
 * @C CVS
 * 
 *    Revision 1.1 2001/09/15 08:49:39 bebbo
 * @R moved to this package
 * 
 *    Revision 1.4 2001/05/14 21:39:06 bebbo
 * @B groups are now working again
 * 
 *    Revision 1.3 2001/05/14 06:27:24 bebbo
 * @B fixed path into XML, to read user correctly
 * 
 *    Revision 1.2 2001/04/16 13:43:55 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.1 2001/04/02 16:14:31 bebbo
 * @N new
 * 
 *****************************************************************************/
