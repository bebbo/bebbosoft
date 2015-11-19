package de.bb.rmi;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * @author bebbo
 */
public class EasyRemoteObject implements Serializable
{
  private transient RemoteId rid;
  
  /**
   * default ct.
   */
  protected EasyRemoteObject()
  {
  }

  protected Object writeReplace() throws ObjectStreamException
  {
    if (rid == null)
      synchronized (this) {
        if (rid == null)
          rid = Server.register(this);
      }
    return rid;
  }

}