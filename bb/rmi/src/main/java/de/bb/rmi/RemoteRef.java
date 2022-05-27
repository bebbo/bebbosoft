package de.bb.rmi;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * @author bebbo
 */
public class RemoteRef implements Serializable
{
  transient protected RemoteClient client;

  protected long cid, oid;

  /**
   * Used to init the References Parameter.
   * @param client
   * @param oid
   */
  void initialize(RemoteClient client, long oid)
  {
    // System.out.println("new ref: " + oid);
    if (client == null)
      System.out.println("assert client == null");
    this.client = client;
    this.cid = client.cid;
    this.oid = oid;
  }

  protected void finalize() throws Throwable
  {
    if (client != null) {
      synchronized (client) {
        client.release(oid);
      }
    }
    super.finalize();
  }

  /** (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    return (int) oid;
  }

  /** (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o)
  {
    try {
      RemoteRef ro = (RemoteRef) o;
      return oid == ro.oid && cid == ro.cid;
    } catch (RuntimeException e) {
    }
    return false;
  }

  protected Object readResolve()
  {
    return Server.lookupObject(cid, oid);
  }
}