/******************************************************************************
 * $Source: /export/CVS/java/jspboard/src/de/bb/web/user/Manager.java,v $
 * $Revision: 1.2 $
 * $Date: 2004/11/28 16:58:26 $
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

 Created on 05.03.2004

 *****************************************************************************/
package de.bb.web.user;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import javax.naming.Context;

import de.bb.web.user.mejb.Finder;
import de.bb.web.user.mejb.Permission;
import de.bb.web.user.mejb.PermissionHome;
import de.bb.web.user.mejb.Person;
import de.bb.web.user.mejb.Person2Permission;
import de.bb.web.user.mejb.Person2PermissionHome;
import de.bb.web.user.mejb.PersonHome;

/**
 * @author bebbo
 */
public class Manager {
    /** used for empty iterators. */
    private final static ArrayList<String> EMPTY = new ArrayList<String>();

    /** current user, set by login/logout. */
    private PersonData currentUser;

    /** db access object. */
    private Finder finder;

    /**
     * Create a new Forum object, using an anonyous user.
     * 
     * @throws Exception
     */
    public Manager() throws Exception {
        Properties props = new Properties();
        props.put(Context.PROVIDER_URL, "fastrmi://localhost:1111");
        props.put(Context.INITIAL_CONTEXT_FACTORY, "de.bb.rmi.ICF");

        finder = new Finder(props);
        currentUser = new PersonData();

        // ensure that an admin exists!
        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName("admin");
        PermissionHome permh = finder.getPermissionHome();
        Permission perm = permh.findByName("admin");

        if (p == null) {
            // create admin if necessary
            String password = hashPassword("admin");
            p = ph.create();
            p.setName("admin");
            p.setPassword(password);
            p.ejbStore();
        }
        if (perm == null) {
            createPermission("admin");
        }

        // and set admin right
        try {
            PersonData pd = new PersonData(finder, p);
            if (!pd.hasPermission("admin"))
                setPersonPermissions(pd, new String[] { "admin" });
        } catch (Exception ex) {
        }
    }

    /**
     * Creates a random 8-letter password, containing a-z, A-Z und 0-9.
     * 
     * @return a random 8-letter password.
     */
    private String createPassword() {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < 8; ++i) {
            int n = random.nextInt(62);
            char ch;
            if (n < 26) {
                ch = (char) ('A' + n);
            } else if (n < 52) {
                ch = (char) ('a' + n - 26);
            } else {
                ch = (char) ('0' + n - 52);
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Creates a new Permission.
     * 
     * @param permission
     *            name of the Permission
     * @throws Exception
     */
    public void createPermission(String permission) throws Exception {
        PermissionHome ph = finder.getPermissionHome();
        Permission p = ph.create();
        p.setName(permission);
        p.ejbStore();
    }

    /**
     * Creates a new User, by sending an email to the specified email address.
     * 
     * @param name
     *            the user's name
     * @param email
     *            the user's email address
     * @param avatar
     *            the user's avatar url
     * @throws Exception
     */
    public void createUser(String name, String email, String avatar) throws Exception {
        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName(name);
        if (p != null || "anonymous".equalsIgnoreCase(name))
            throw new Exception("error.userExists");

        String password = createPassword();

        Mailer.sendPasswordMail(name, email, password);

        /*
         Properties props = new Properties();
         props.put("mail.smtp.host", host);
         props.put("mail.smtp.auth", "true");
         
         Authenticator auth = new MyAuthenticator(mailUser, mailPassword);
         Session session = Session.getDefaultInstance(props, auth);
         
         MimeMessage message = new MimeMessage(session);
         message.setFrom(new InternetAddress(mailUser));
         message.setRecipient(
         Message.RecipientType.TO,
         new InternetAddress(email));
         message.setSubject(mailMessage);
         message.setText(mailMessage + ": " + password);
         Transport.send(message);
         */

        password = hashPassword(password);

        p = ph.create();
        p.setName(name);
        p.setEmail(email);
        p.setAvatar(avatar);
        p.setPassword(password);
        p.ejbStore();
    }

    /**
     * Creates a new User, by sending an email to the specified email address.
     * 
     * @param name
     *            the user's name
     * @param email
     *            the user's email address
     * @param avatar
     *            the user's avatar url
     * @throws Exception
     */
    public void resendUserPassword(String name, String email) throws Exception {
        if ("anonymous".equalsIgnoreCase(name))
            throw new Exception("error.userNotExists");

        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName(name);
        if (p == null && email.length() != 0)
            p = ph.findByEmail(email);

        if (p == null)
            throw new Exception("error.userNotExists");

        String password = createPassword();

        Mailer.sendPasswordMail(p.getName(), p.getEmail(), password);

        /*
         Properties props = new Properties();
         props.put("mail.smtp.host", host);
         props.put("mail.smtp.auth", "true");
         
         Authenticator auth = new MyAuthenticator(mailUser, mailPassword);
         Session session = Session.getDefaultInstance(props, auth);
         
         MimeMessage message = new MimeMessage(session);
         message.setFrom(new InternetAddress(mailUser));
         message.setRecipient(
         Message.RecipientType.TO,
         new InternetAddress(email));
         message.setSubject(mailMessage);
         message.setText(mailMessage + ": " + password);
         Transport.send(message);
         */

        password = hashPassword(password);

        p.setPassword(password);
        p.ejbStore();
    }

    /**
     * Return the Iterator over all existing permissions.
     * 
     * @return an Iterator over all existing permissions.
     * @throws Exception
     */
    public Iterator permissions() throws Exception {
        PermissionHome ph = finder.getPermissionHome();
        Collection c = ph.findAll();
        ArrayList al = new ArrayList();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Permission p = (Permission) i.next();
            al.add(p.getName());
        }
        return al.iterator();
    }

    /**
     * Return an Iterator over all existing users.
     * 
     * @return an Iterator over all existing users.
     * @throws Exception
     */
    public Iterator<String> getPersonNames() throws Exception {
        PersonHome ph = finder.getPersonHome();
        Collection c = ph.findAll();
        ArrayList<String> al = new ArrayList<String>();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Person p = (Person) i.next();
            al.add(p.getName());
        }
        return al.iterator();
    }

    /**
     * Return an Iterator over all existing users.
     * 
     * @param permission
     * @return an Iterator over all existing users.
     * @throws Exception
     */
    public Iterator getPersonNamesByPermission(String permission) throws Exception {
        PersonHome ph = finder.getPersonHome();
        Collection c = ph.findByPermission(permission);
        ArrayList al = new ArrayList();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Person p = (Person) i.next();
            al.add(p.getName());
        }
        return al.iterator();
    }

    /**
     * Get the current user.
     * 
     * @return the current user.
     */
    public PersonData getUser() {
        return currentUser;
    }

    /**
     * Lookup an user by name.
     * 
     * @param username
     *            the user's name
     * @return the PersonData object.
     * @throws Exception
     */
    public PersonData getUserByName(String username) throws Exception {
        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName(username);
        PersonData user = new PersonData(finder, p);
        return user;
    }

    /**
     * Get an iterator over all permission, the user dont have.
     * 
     * @param user
     *            the user's PersonData object.
     * @return an iterator over all permission, the user dont have.
     * @throws Exception
     */
    public Iterator getUserNotPermissions(PersonData user) throws Exception {
        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName(user.getName());

        PermissionHome permh = finder.getPermissionHome();
        Collection c = permh.findByPerson(p);
        Collection all = permh.findAll();
        ArrayList rem = new ArrayList();
        rem.addAll(all);
        rem.removeAll(c);
        ArrayList al = new ArrayList();
        for (Iterator i = rem.iterator(); i.hasNext();) {
            Permission perm = (Permission) i.next();
            al.add(perm.getName());
        }
        return al.iterator();
    }

    /**
     * Get an iterator over all user's permissions.
     * 
     * @param person
     *            the user's PersonData object
     * @return an iterator over all user's permissions.
     */
    public Iterator getUserPermissions(PersonData person) {
        try {
            PersonHome ph = finder.getPersonHome();
            Person p = ph.findByName(person.getName());

            PermissionHome permh = finder.getPermissionHome();
            Collection c = permh.findByPerson(p);
            ArrayList al = new ArrayList();
            for (Iterator i = c.iterator(); i.hasNext();) {
                Permission perm = (Permission) i.next();
                al.add(perm.getName());
            }
            return al.iterator();
        } catch (Exception ex) {
        }
        return EMPTY.iterator();
    }

    /**
     * Hash a password string, to make it unreadable.
     * 
     * @param password
     *            the password
     * @return the hashed passwort.
     * @throws Exception
     */
    public static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        BigInteger bi = new BigInteger(1, md.digest(password.getBytes()));
        password = bi.toString(16);
        return password;
    }

    /**
     * Performs the login for the given username and password.
     * 
     * @param username
     *            the username
     * @param password
     *            the password
     * @throws Exception
     *             on failure
     */
    public void login(String username, String password) throws Exception {
        password = hashPassword(password);
        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByNamePassword(username, password);
        if (p == null)
            throw new Exception("error.login");

        currentUser = new PersonData(finder, p);
        currentUser.login();
    }

    /**
     * Performs the login for the given username and password.
     */
    public void logout() {
        currentUser = new PersonData();
    }

    /**
     * Removes a permission with all associations.
     * 
     * @param permission
     *            the permissions name.
     * @throws Exception
     */
    public void removePermission(String permission) throws Exception {
        PermissionHome ph = finder.getPermissionHome();
        Permission p = ph.findByName(permission);
        if (p == null)
            return;

        Collection c = p.getPerson2PermissionList();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Person2Permission p2p = (Person2Permission) i.next();
            p2p.remove();
        }
        p.remove();
    }

    /**
     * Removes an user with all associations.
     * 
     * @param username
     *            the user's name
     * @throws Exception
     */
    public void removePerson(String username) throws Exception {
        if ("admin".equals(username))
            return;

        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName(username);
        if (p == null)
            return;

        // remove the person from the permissions
        Collection c = p.getPerson2PermissionList();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Person2Permission p2p = (Person2Permission) i.next();
            p2p.remove();
        }

        p.remove();
    }

    /**
     * Update a persons password.
     * 
     * @param person
     *            a PersonData object
     * @param password
     *            the new password
     * @param avatar
     * @throws Exception
     */
    public void setPersonData(PersonData person, String password, String avatar) throws Exception {
        Person p = person.getPerson();
        if (p == null)
            throw new Exception("user does not exist: " + person);

        if (password != null && password.length() > 0) {
            password = hashPassword(password);
            p.setPassword(password);
        }
        p.setAvatar(avatar);
        p.store();
    }

    /**
     * Update a person's permissions, by setting new permissions and removing old.
     * 
     * @param person
     *            a PersonData object.
     * @param permissionNames
     *            array of new permissions
     * @throws Exception
     */
    public void setPersonPermissions(PersonData person, String[] permissionNames) throws Exception {
        HashSet permissions = new HashSet();
        if (permissionNames == null) {
            permissionNames = new String[] {};
        }
        for (int i = 0; i < permissionNames.length; ++i) {
            permissions.add(permissionNames[i]);
        }
        PersonHome ph = finder.getPersonHome();
        Person p = ph.findByName(person.getName());

        PermissionHome permh = finder.getPermissionHome();
        Person2PermissionHome p2ph = finder.getPerson2PermissionHome();

        // entfernen der alten
        Collection c = permh.findByPerson(p);
        for (Iterator i = c.iterator(); i.hasNext();) {
            Permission perm = (Permission) i.next();
            boolean exists = permissions.remove(perm.getName());
            if (!exists) {
                Person2Permission p2p = p2ph.findByPersonPermission(p, perm);
                if (p2p != null)
                    p2p.remove();
            }
        }

        // neue Hinzufuegen
        for (Iterator i = permissions.iterator(); i.hasNext();) {
            String pn = (String) i.next();
            Permission perm = permh.findByName(pn);
            Person2Permission p2p = p2ph.create();
            p2p.setPermission(perm);
            p2p.setPerson(p);
            p2p.ejbStore();
        }

        if (person.getName().equals(currentUser.getName())) {
            currentUser = new PersonData(finder, p);
        } else {

        }
    }
}

/******************************************************************************
 * Log: $Log: Manager.java,v $ Log: Revision 1.2 2004/11/28 16:58:26 bebbo Log: @R adapted changes of bb_mejb and bb_rmi
 * Log: Log: Revision 1.1 2004/11/26 09:58:04 bebbo Log: @N new Log:
 ******************************************************************************/
