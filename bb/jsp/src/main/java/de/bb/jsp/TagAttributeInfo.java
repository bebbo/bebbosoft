/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/TagAttributeInfo.java,v $
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


class TagAttributeInfo
{
  private boolean rtexpr;

  private String type;

  private boolean required;

  private String name;

//  private String smallIcon;

  private String desc, sName;

  // aName, aReqr, aType, aReqt, aDesc
  TagAttributeInfo(String name, String required, String type, String rtexpr, String descr)
  {
    this.name = name;     
    this.required = "true".equals(required) || "yes".equals(required);
    this.type =  type;
    this.rtexpr =  "true".equals(rtexpr)  || "yes".equals(rtexpr);
    this.desc = descr;
    
    sName = (isRequired() ? "required" : "optional");
    if (canBeRequestTime())
      sName += ", request-time-expression";
  }
  /**
   * @return
   */
  boolean canBeRequestTime()
  {
    return rtexpr;
  }
  String getInfoString()
  {
    return desc;
  }
  /**
   * @return
   */
  String getShortName()
  {
    return sName;
  }
  /**
   * @return
   */
  String getName()
  {
    return name;
  }
  /**
   * @return
   */
  boolean isRequired()
  {
    return required;
  }

}
/******************************************************************************
 * $Log: TagAttributeInfo.java,v $
 * Revision 1.1  2004/04/16 13:45:10  bebbo
 * @R contains only the JspCC - runtime moved to de.bb.bejy.http.jsp
 *
 * Revision 1.1  2003/10/23 20:35:34  bebbo
 * @R moved JspCC inner classes into separate files
 * @R jspCC is now reusable
 * @N added more caching to enhance reusability
 *
 *****************************************************************************/