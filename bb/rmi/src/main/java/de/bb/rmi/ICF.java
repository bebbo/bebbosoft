package de.bb.rmi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * @author bebbo
 */
public class ICF implements InitialContextFactory
{
  /**
   * @see javax.naming.spi.InitialContextFactory#getInitialContext(Hashtable)
   */
  public Context getInitialContext(Hashtable environment)
    throws NamingException
  {
    return new IC(environment);
  }
}
