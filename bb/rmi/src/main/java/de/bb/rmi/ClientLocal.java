/* 
 * Created on 13.11.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.rmi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * @author sfranke
 */
class ClientLocal
{ 
  
  private Principal principal;
/*
  private Hashtable properties;
  private String helper;
*/
  
  /**
   * @param properties2
   */
  ClientLocal(Principal principal)
  {
    this.principal = principal;
    /*
    this.properties = properties2;
    String user = (String) properties2.get(Context.SECURITY_PRINCIPAL);
    if (user == null)
      user = "anonymous";
    String pwd = (String) properties2.get(Context.SECURITY_CREDENTIALS);
    if (pwd == null)
      pwd = "*";
    
    principal = new Principal(user, properties2);
    helper = (String)properties2.get(Server.PRINCIPAL_HOLDER);
    */
  }

   /**
   * @param name
   * @return
   * @throws NamingException
   */
  public Object lookup(String name) throws NamingException
  {
    String error = null;
    try {
/*
      if (name.startsWith("java:comp/env/")) {
        name = name.substring(14);
      }
      if (!name.endsWith("Bean"))
        name += "Bean";
      
      Class clazz;
      try {
        clazz = CL.cl.loadClass(name);
      } catch (Exception ex) {
        return null; // not found -> null
      }

      Object ret = clazz.newInstance();
*/
//      return Server.makeInstance(principal, helper, name);
      return Server.makeInstance(principal, name);
    } catch (Throwable ex) {
      // ex.printStackTrace();

      while (ex.getCause() != null)
        ex = ex.getCause();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ex.printStackTrace(new PrintStream(bos));

      if (error == null) {
        error = "not loaded: " + name;
      }
      error += "\r\nReason: "
          + new StringTokenizer(bos.toString(), "\r\n").nextToken();
      NamingException ne = new NamingException(error);
      //      re.initCause(ex.getCause());
      throw ne;
    }
  }
}