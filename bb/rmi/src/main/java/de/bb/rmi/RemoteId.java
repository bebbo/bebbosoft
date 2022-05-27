package de.bb.rmi;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * @author bebbo
 */
class RemoteId implements Serializable
{
  long cid;
  long oid;
  String cn;

  /**
   * Constructor RemoteId.
   * @param cid
   * @param oid
   * @param cn
   */
  RemoteId(long cid, long oid, String cn)
  {
    this.cid = cid;
    this.oid = oid;
    this.cn = cn;
  }

  protected Object readResolve() throws ObjectStreamException
  {
    RemoteClient client = RemoteClient.getClient(cid);
    synchronized (client) {
      RemoteRef rr = client.get(oid);
      if (rr != null)
        return rr;
      try {
        RemoteClient rc = RemoteClient.getClient(cid);
        try {
          Class clazz = rc.loadClass(cn);
          rr = (RemoteRef) clazz.newInstance();
        } catch (ClassNotFoundException cfne) {
          Class clazz = rc.loadStub(cn);
          rr = (RemoteRef) clazz.newInstance();
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new OSE("cannot create " + cn);
      }
      rr.initialize(client, oid);
      client.put(oid, rr);
      return rr;
    }
  }

  static class OSE extends ObjectStreamException
  {
    OSE(String s)
    {
      super(s);
    }
  }
}