/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
*/


package de.bb.web.user.mejb;

import java.rmi.RemoteException;
import java.util.Collection;


public interface PersonHome extends PersonHomeBase
{

  /**
   * @param board
   * @return
   */
  Collection findByPermission(String permission) throws RemoteException;

  /**
   * @param username
   * @param password
   * @return
   */
  Person findByNamePassword(String username, String password) throws RemoteException;

  /**
   * @param username
   * @return
   */
  Person findByName(String username) throws RemoteException;

  /**
   * @param email
   * @return
   */
  Person findByEmail(String email) throws RemoteException;
}
