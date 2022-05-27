/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/TagInfo.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:45:09 $
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

  Created on 13.04.2004

 *****************************************************************************/
package de.bb.jsp;

/**
 * @author bebbo
 */
class TagInfo
{

  private String displayName;

  private TagAttributeInfo[] tai;

  private String tIcon;

  private TagExtraInfo tei;

  private TagLibraryInfo info;

  private String tInfo;

  private String sBody;

  private String tClazz;

  private String tName;

  /**
   * @param tName
   * @param tClazz
   * @param sBody
   * @param tInfo
   * @param info
   * @param tei
   * @param aa
   * @param object
   * @param tIcon
   * @param object2
   * @param object3
   */
  TagInfo(String tName, String tClazz, String sBody, String tInfo, TagLibraryInfo info, TagExtraInfo tei, TagAttributeInfo[] tai, String displayName, String tIcon, Object object2, Object object3)
  {
    this.tName = tName;
    this.tClazz = tClazz;
    this.sBody = sBody;
    this.tInfo = tInfo;
    this.info = info;
    this.tei = tei;
    this.displayName = displayName;
    this.tIcon = tIcon;
    this.tai = tai;
  }

  /**
   * @return
   */
  TagAttributeInfo[] getAttributes()
  {
    return tai;
  }

  /**
   * @return
   */
  String getTagClassName()
  {
    return tClazz;
  }

  /**
   * @return
   */
  TagExtraInfo getTagExtraInfo()
  {
    return tei;
  }

  /**
   * @return
   */
  String getTagName()
  {
    return tName;
  }

  /**
   * @return
   */
  Object getDisplayName()
  {
    return displayName;
  }

  /**
   * @return
   */
  String getInfoString()
  {
    return tInfo;
  }

  /**
   * @return
   */
  String getSmallIcon()
  {
    return tIcon;
  }

}

/******************************************************************************
 * Log: $Log: TagInfo.java,v $
 * Log: Revision 1.1  2004/04/16 13:45:09  bebbo
 * Log: @R contains only the JspCC - runtime moved to de.bb.bejy.http.jsp
 * Log:
 ******************************************************************************/