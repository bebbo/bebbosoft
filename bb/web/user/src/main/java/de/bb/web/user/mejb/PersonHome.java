/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
*/


package de.bb.web.user.mejb;

import java.rmi.RemoteException;
import java.util.Collection;

public interface PersonHome extends PersonHomeBase
{

    Person findByName(String name) throws RemoteException;

    Person findByEmail(String email) throws RemoteException;

    Person findByNamePassword(String username, String password) throws RemoteException;

    Collection findByPermission(String permission) throws RemoteException;
}
