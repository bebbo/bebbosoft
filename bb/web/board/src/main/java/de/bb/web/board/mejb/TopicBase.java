/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.board.mejb;

public interface TopicBase extends de.bb.mejb.Simple
{
  public String getId() throws java.rmi.RemoteException;
  public String getBoardId() throws java.rmi.RemoteException;
  public Board getBoard() throws java.rmi.RemoteException;
  public void setBoard(Board a$Board) throws java.rmi.RemoteException;
  public java.lang.String getName() throws java.rmi.RemoteException;
  public void setName(java.lang.String a$Name) throws java.rmi.RemoteException;
  public int getSticky() throws java.rmi.RemoteException;
  public void setSticky(int a$Sticky) throws java.rmi.RemoteException;
  public java.lang.String getAuthor() throws java.rmi.RemoteException;
  public void setAuthor(java.lang.String a$Author) throws java.rmi.RemoteException;
  public java.sql.Timestamp getModified() throws java.rmi.RemoteException;
  public void setModified(java.sql.Timestamp a$Modified) throws java.rmi.RemoteException;
  public int getViewCount() throws java.rmi.RemoteException;
  public void setViewCount(int a$ViewCount) throws java.rmi.RemoteException;
  public StringBuffer toLog() throws java.rmi.RemoteException;
  public void store() throws java.rmi.RemoteException;
  public java.util.Collection getArticleList() throws java.rmi.RemoteException;
}
