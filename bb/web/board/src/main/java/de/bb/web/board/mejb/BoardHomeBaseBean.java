/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.board.mejb;

abstract class BoardHomeBaseBean
  extends de.bb.mejb.CMPHomeBean
  implements de.bb.web.board.mejb.BoardHomeBase
{
  private static de.bb.mejb.CMPDbi dbi = null;
  protected de.bb.mejb.CMPDbi getDbi() { return dbi; }
  private synchronized void init() throws java.rmi.RemoteException
  {
    dbi = initDbi("de.bb.web.board.mejb", "Board");
  }

  BoardHomeBaseBean() throws java.rmi.RemoteException
  {
    super("Board");
    if (dbi == null)
      init();
  }
  public javax.ejb.EJBMetaData getEJBMetaData() throws java.rmi.RemoteException
  {
    return null;
  }
  public javax.ejb.HomeHandle getHomeHandle() throws java.rmi.RemoteException
  {
    return null;
  }
  public void remove(Object o) throws java.rmi.RemoteException
  {
    remove((de.bb.mejb.CMPBean)o, dbi);
  }
  public void remove(javax.ejb.Handle handle) throws java.rmi.RemoteException
  {
  }
  public de.bb.web.board.mejb.Board findByPrimaryKey(Object pk) throws java.rmi.RemoteException
  {
    return (de.bb.web.board.mejb.Board)readByPrimaryKey(pk, dbi);
  }
  public de.bb.web.board.mejb.Board create() throws java.rmi.RemoteException
  {
    return new BoardBean(this);
  }
  // not ejb standard functions
  public de.bb.mejb.Simple internCreate() throws java.rmi.RemoteException
  {
    return (de.bb.mejb.Simple)create();
  }
  void store(Object o) throws java.rmi.RemoteException
  {
    store((de.bb.mejb.CMPBean)o, 8, dbi);
  }
  BoardBean load(Object obj) throws java.rmi.RemoteException
  {
    BoardBean o = (BoardBean) obj;
    load(o, dbi);
    return o;
  }
  public java.util.Collection findAll() throws java.rmi.RemoteException
  {
    return this.queryCollection("SELECT * FROM Board ORDER BY ID", NOPARAM);
  }
}
