/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
*/

package de.bb.web.user.mejb;

import java.rmi.RemoteException;
import java.util.Collection;

public class PersonHomeBean extends PersonHomeBaseBean implements de.bb.web.user.mejb.PersonHome {

    public PersonHomeBean() throws RemoteException {
        super();
    }

    @Override
    public Person findByName(String name) throws RemoteException {
        Collection<Person> c = this.queryCollection("SELECT * FROM Person WHERE name = ?", new Object[] { name });
        if (c.isEmpty())
            return null;
        return c.iterator().next();
    }

    @Override
    public Person findByEmail(String email) throws RemoteException {
        Collection<Person> c = this.queryCollection("SELECT * FROM Person WHERE email = ?",
                new Object[] { email });
        if (c.isEmpty())
            return null;
        return c.iterator().next();
    }

    @Override
    public Person findByNamePassword(String username, String password) throws RemoteException {
        Collection<Person> c = this.queryCollection("SELECT * FROM Person WHERE name = ? and password = ?",
                new Object[] { username, password });
        if (c.isEmpty())
            return null;
        return c.iterator().next();
    }

    @Override
    public Collection findByPermission(String permission) throws RemoteException {
        return this.queryCollection("SELECT * FROM Person, Person2Permission WHERE Person.id = Person2Permission.id_person AND Person2Permission.id_permission = ?", new Object[] { permission });
    }
}
