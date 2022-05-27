/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/TagLibraryInfo.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:45:10 $
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
      notice, this list of conditions and the following disclaimer in
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

  Created on 23.10.2003

 *****************************************************************************/

package de.bb.jsp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.bb.util.XmlFile;

/**
 * TagLibraryInfo helper class
 */
class TagLibraryInfo
{
  private TagInfo[] tags;
  private String info;
  private String shortname;
  private String jspversion;
  private String tlibversion;
  private String urn;
  private String uri;
  private String prefix;
  private final JspCC jspCC;
  private String smallIcon;

  HashMap hTags = new HashMap();

  TagLibraryInfo(JspCC jspCC, String prefix, String uri, String url)
  {
    this.prefix = prefix;
    this.uri = uri;
    this.jspCC = jspCC;
    this.urn = url;
  }

  void init(XmlFile xf) throws Exception
  {
    tlibversion = xf.getContent("/taglib/tlib-version");
    if (tlibversion == null)
      tlibversion = xf.getContent("/taglib/tlibversion");
    jspversion = xf.getContent("/taglib/jsp-version");
    if (jspversion == null)
      jspversion = xf.getContent("/taglib/jspversion");
    shortname = xf.getContent("/taglib/short-name");
    if (shortname == null)
      shortname = xf.getContent("/taglib/shortname");
    this.info = xf.getContent("/taglib/description");
    if (this.info == null)
      this.info = xf.getContent("/taglib/info");

    smallIcon = xf.getContent("/taglib/small-icon");

    // create tags
    List lTags = new LinkedList();
    Vector vTags = xf.getSections("/taglib/tag");
    for (int i = 0; i < vTags.size(); ++i)
    {
      String tag = (String)vTags.elementAt(i);

      String tClazz = xf.getContent(tag + "tag-class");
      if (tClazz == null)
        tClazz = xf.getContent(tag + "tagclass");

      String teiClazzName = xf.getContent(tag + "tei-class");
      if (teiClazzName == null)
        teiClazzName = xf.getContent(tag + "teiclass");
      Vector vars = xf.getSections(tag + "variable");
      if (teiClazzName != null && (vars != null && vars.size() > 0))
        throw new Exception("either <tei-class> or <variable> is allowed");

      String tBody = xf.getContent(tag + "body-content");
      if (tBody == null)
        tBody = xf.getContent(tag + "bodycontent");
      String tInfo = xf.getContent(tag + "description");
      if (tInfo == null)
        tInfo = xf.getContent(tag + "info");
      String tName = xf.getContent(tag + "name");
      String tIcon = xf.getContent(tag + "small-icon");

      String sBody = tBody == null ? "EMPTY" : "TAGDEPENDENT";
      if ("JSP".equals(tBody))
        sBody = tBody;

      List lAttr = new LinkedList();
      Vector vAttr = xf.getSections(tag + "attribute");
      for (int j = 0; j < vAttr.size(); ++j)
      {
        String attr = (String)vAttr.elementAt(j);

        String aName = xf.getContent(attr + "name");
        String aReqr = xf.getContent(attr + "required");
        String aType = xf.getContent(attr + "type");
        String aReqt = xf.getContent(attr + "rtexprvalue");
        String aDesc = xf.getContent(attr + "description");
        lAttr.add(new TagAttributeInfo(aName, aReqr, aType, aReqt, aDesc));
      }

      TagAttributeInfo[] aa = new TagAttributeInfo[lAttr.size()];
      lAttr.toArray(aa);

      TagExtraInfo tei = null;
      // load the tag extra info, if it exists.
      if (teiClazzName != null)
      {
        Class teiClazz = this.jspCC.loadClass(teiClazzName);
        try
        {
          Object oo = teiClazz.newInstance();
          tei = new TagExtraInfo(oo);
        } catch (Exception ex)
        {
          System.out.println(teiClazzName);
          throw ex;
        }
      } else if (vars.size() > 0)
      {
        tei = new TagExtraInfo(xf, vars);
      }

      TagInfo ti =
        new TagInfo(
          tName,
          tClazz,
          sBody,
          tInfo,
          this,
          tei,
          aa,
          null,
          tIcon,
          null,
          null);
      if (tei != null)
      {
        tei.setTagInfo(ti);
      }
      lTags.add(ti);

      hTags.put(tName, ti);
    }
    tags = new TagInfo[lTags.size()];
    lTags.toArray(tags);
  }

  TagInfo getTagInfo(String name)
  {
    return (TagInfo)hTags.get(name);
  }
  /**
   * @return
   */
  String getSmallIcon()
  {
    return smallIcon;
  }

  /**
   * @return
   */
  String getURI()
  {
    return this.uri;
  }

  /**
   * @return
   */
  Object getShortName()
  {
    return this.shortname;
  }

  /**
   * @return
   */
  String getInfoString()
  {
    return this.info;
  }

  /**
   * @return
   */
  TagInfo[] getTags()
  {
    return this.tags;
  }

}
/******************************************************************************
 * $Log: TagLibraryInfo.java,v $
 * Revision 1.1  2004/04/16 13:45:10  bebbo
 * @R contains only the JspCC - runtime moved to de.bb.bejy.http.jsp
 *
 * Revision 1.1  2003/10/23 20:35:34  bebbo
 * @R moved JspCC inner classes into separate files
 * @R jspCC is now reusable
 * @N added more caching to enhance reusability
 *
 *****************************************************************************/