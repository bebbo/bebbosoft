/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
*/


package de.bb.web.board.mejb;

import java.rmi.RemoteException;

import de.bb.web.board.mejb.TopicBase;

public interface Topic extends TopicBase
{

    public abstract long getArticleCount()
        throws RemoteException;

    public abstract Article getLastArticle()
        throws RemoteException;

}
