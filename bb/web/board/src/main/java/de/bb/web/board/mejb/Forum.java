/******************************************************************************
 * $Source: /export/CVS/java/jspboard/src/de/bb/web/board/Forum.java,v $
 * $Revision: 1.2 $
 * $Date: 2004/11/28 16:58:26 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 ******************************************************************************
 NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Every product and solution using this software, must be free
 of any charge. If the software is used by a client part, the
 server part must also be free and vice versa.

 2. Each redistribution must retain the copyright notice, and
 this list of conditions and the following disclaimer.

 3. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditios and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.

 4. All advertising materials mentioning features or use of this
 software must display the following acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 5. Redistributions of any form whatsoever must retain the following
 acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
 DISCLAIMER OF WARRANTY

 Software is provided "AS IS," without a warranty of any kind.
 You may use it on your own risk.

 ******************************************************************************
 LIMITATION OF LIABILITY

 I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
 AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
 FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
 SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
 COPYRIGHT

 (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 Created on 05.03.2004

 *****************************************************************************/
package de.bb.web.board.mejb;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;

import de.bb.web.user.PersonData;
import de.bb.web.board.mejb.Article;
import de.bb.web.board.mejb.ArticleHome;
import de.bb.web.board.mejb.Board;
import de.bb.web.board.mejb.BoardHome;
import de.bb.web.board.mejb.Finder;
import de.bb.web.board.mejb.Topic;
import de.bb.web.board.mejb.TopicHome;

/**
 * @author bebbo
 */
public class Forum
{
  /** used for empty iterators. */
  private final static ArrayList EMPTY = new ArrayList();

  /** current user, set by login/logout. */
  private PersonData currentUser;

  /** db access object. */
  private Finder finder;
  
  /**
   * Create a new Forum object, using an anonyous user.
   * @throws Exception 
   */
  public Forum() throws Exception
  {
    Properties props = new Properties();
    props.put(Context.PROVIDER_URL, "fastrmi://localhost:1111");
    props.put(Context.INITIAL_CONTEXT_FACTORY, "de.bb.rmi.ICF");
    finder = new Finder(props);
    currentUser = new PersonData();
  }

  /**
   * Creates an Iterator of BoardInof for all accessable Boards.
   * @return Creates an Iterator of BoardInof for all accessable Boards.
   * @throws Exception
   */
  public Iterator boardInfos() throws Exception
  {
    ArrayList al = new ArrayList();

    // ermittelt alle Board EJB objecte
    BoardHome bh = finder.getBoardHome();
    Collection c = bh.findAllOrdered();

    // die nicht sichtbaren ausfiltern
    for (Iterator i = c.iterator(); i.hasNext();) {
      Board board = (Board) i.next();
      String readPermission = board.getReadPermission();
      String writePermission = board.getWritePermission();
      String adminPermission = board.getAdminPermission();
      if (canUserRead(readPermission, writePermission, adminPermission)) {
        al.add(board);
      }
    }

    return al.iterator();
  }

  /**
   * Checks whether current user may read a board.
   * @param readPermission the read permission
   * @param writePermission the write permission
   * @param adminPermission the admin permission
   * @return true, if allowed, false either
   * @throws Exception
   */
  private boolean canUserRead(String readPermission, String writePermission,
      String adminPermission) throws Exception
  {
    if (readPermission == null || readPermission.length() == 0)
      return true;

    if (currentUser.hasPermission(readPermission))
      return true;

    if (writePermission != null && writePermission.length() > 0 && currentUser.hasPermission(writePermission))
      return true;

    if (adminPermission != null && adminPermission.length() > 0 && currentUser.hasPermission(adminPermission))
      return true;

    return false;
  }

  /**
   * Creates a new Article.
   * @param topicId id of topic where the article belongs to
   * @param content content of the article.
   * @throws Exception
   */
  public void createArticle(String topicId, String content) throws Exception
  {
    TopicHome th = finder.getTopicHome();
    Topic t = th.findByPrimaryKey(topicId);

    Timestamp ts = new Timestamp(System.currentTimeMillis());
    t.setModified(ts);
    t.store();

    ArticleHome ah = finder.getArticleHome();

    Article a = ah.create();
    a.setTopic(t);
    a.setContent(content);
    a.setAuthor(currentUser.getName());
    a.setModified(ts);
    a.store();
    
    currentUser.addPost();
  }

  /**
   * Creates a new Board.
   * @param boardName name of the Board
   * @param description description of the board
   * @throws Exception
   */
  public void createBoard(String boardName, String description)
      throws Exception
  {
    BoardHome bh = finder.getBoardHome();
    Board b = bh.create();
    b.setName(boardName);
    b.setAdminPermission("admin");
    b.setDescription(description);
    b.ejbStore();
  }

  /**
   * Creates a new Topic.
   * @param boardId the topic's board
   * @param topicName name of the ropic
   * @param articleContent content of the initial article
   * @return
   * @throws Exception
   */
  public Topic createTopic(String boardId, String topicName,
      String articleContent) throws Exception
  {
    BoardHome bh = finder.getBoardHome();
    Board b = bh.findByPrimaryKey(boardId);
    TopicHome th = finder.getTopicHome();
    Topic t = th.create();
    t.setName(topicName);
    t.setAuthor(currentUser.getName());
    t.setBoard(b);
    Timestamp ts = new Timestamp(System.currentTimeMillis());
    t.setModified(ts);
    t.ejbStore();

    createArticle(t.getId(), articleContent);
    
    return t;
  }

  /**
   * Retrieve the ArticleData.
   * @param articleId the articles ID
   * @return an ArticleData object, containing the article content.
   * @throws Exception
   */
  public Article getArticle(String articleId) throws Exception
  {
    ArticleHome ah = finder.getArticleHome();
    Article a = ah.findByPrimaryKey(articleId);
    return a;
  }

  /**
   * Retrieve the BoardData containing TopicInfos.
   * @param boardId the board ID
   * @return a BoardData object containing the boards data and a list of TopicInfos
   * @throws Exception
   */
  public Board getBoard(String boardId) throws Exception
  {
    BoardHome bh = finder.getBoardHome();
    Board b = bh.findByPrimaryKey(boardId);
    if (b == null)
      return null;

    if (!canUserRead(b.getReadPermission(), b.getWritePermission(), b
        .getAdminPermission()))
      return null;

    return b;
  }

  /**
   * Retrieve the TopicData, containing the topic's articles.
   * @param topicId the topic's id
   * @return the TopicData, containing the topic's articles
   * @throws Exception
   */
  public Topic getTopic(String topicId) throws Exception
  {
    TopicHome th = finder.getTopicHome();
    Topic t = th.findByPrimaryKey(topicId);
    if (t == null)
      return null;

    BoardHome bh = finder.getBoardHome();
    Board b = bh.findByPrimaryKey(t.getBoardId());
    if (b == null)
      return null;

    if (!canUserRead(b.getReadPermission(), b.getWritePermission(), b
        .getAdminPermission()))
      return null;

    currentUser.markTopic(topicId);
    return t;
  }

  /**
   * Get the current user.
   * @return the current user.
   */
  public PersonData getUser()
  {
    return currentUser;
  }
  /**
   * Set the current user.
   * @param user
   */
  public void setUser(PersonData user)
  {
    currentUser = user;
  }

  /**
   * Performs the login for the given username and password.
   */
  public void logout()
  {
    currentUser = new PersonData();
  }

  /**
   * Removes the specified article.
   * @param articleId the article's id.
   * @throws Exception
   */
  public void removeArticle(String articleId) throws Exception
  {
    ArticleHome ah = finder.getArticleHome();
    Article a = ah.findByPrimaryKey(articleId);
    a.remove();
  }

  /**
   * Removes a bord with all topics and articles.
   * @param boardId the boards id.
   * @throws Exception
   */
  public void removeBoard(String boardId) throws Exception
  {
    BoardHome bh = finder.getBoardHome();
    Board b = bh.findByPrimaryKey(boardId);
    if (b == null)
      return;
    Collection c1 = b.getTopicList();
    for (Iterator i = c1.iterator(); i.hasNext();) {
      Topic t = (Topic) i.next();
      Collection c2 = t.getArticleList();
      for (Iterator j = c2.iterator(); j.hasNext();) {
        Article a = (Article) j.next();
        a.remove();
      }
      t.remove();
    }
    b.remove();
  }

  /**
   * Removes a topic with all articles
   * @param topicId the topic's id.
   * @throws Exception
   */
  public void removeTopic(String topicId) throws Exception
  {
    TopicHome th = finder.getTopicHome();
    Topic t = th.findByPrimaryKey(topicId);
    Collection c2 = t.getArticleList();
    for (Iterator j = c2.iterator(); j.hasNext();) {
      Article a = (Article) j.next();
      a.remove();
    }
    t.remove();
  }

  /**
   * @return
   * @throws Exception
   */
  public Collection recentTopics() throws Exception
  {
    if (!currentUser.hasPermission(""))
      return EMPTY;
    
    TopicHome th = finder.getTopicHome();
    return th.findRecent(currentUser.getLastVisit(), currentUser.getId());
  }  
}

/******************************************************************************
 * Log: $Log: Forum.java,v $
 * Log: Revision 1.2  2004/11/28 16:58:26  bebbo
 * Log: @R adapted changes of bb_mejb and bb_rmi
 * Log:
 * Log: Revision 1.1  2004/11/26 09:58:04  bebbo
 * Log: @N new
 * Log:
 ******************************************************************************/