/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
*/

package de.bb.web.user.mejb;

import java.rmi.RemoteException;
import java.util.Collection;

public class PermissionHomeBean extends PermissionHomeBaseBean implements PermissionHome {
    public PermissionHomeBean() throws java.rmi.RemoteException {
        super();
    }

    @Override
    public Permission findByName(String name) throws RemoteException {
        Collection<Permission> c = this.queryCollection("SELECT * FROM Permission WHERE name = ?",
                new Object[] { name });
        if (c.isEmpty())
            return null;
        return c.iterator().next();
    }

    @Override
    public Collection findByPerson(Person p) throws RemoteException {
        return this.queryCollection("SELECT Permission.* FROM Permission, Person2Permission "
                + "WHERE Person2Permission.id_person = ? AND Person2Permission.id_permission = Permission.id", new Object[] { p.getId() });
    }
}
