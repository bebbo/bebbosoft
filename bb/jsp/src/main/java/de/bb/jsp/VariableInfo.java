/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/VariableInfo.java,v $
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

  Created on 13.04.2004

 *****************************************************************************/
package de.bb.jsp;

/**
 * @author bebbo
 */
class VariableInfo
{
  private int scope;
  private boolean declare;
  private String varClazz;
  private String nameGiven;
  final static int AT_END = 2;
  final static int AT_BEGIN = 1;
  final static int NESTED = 0;

  /**
   * @param nameGiven
   * @param varClazz
   * @param declare
   * @param scope
   */
  VariableInfo(String nameGiven, String varClazz, boolean declare, int scope)
  {
    this.nameGiven = nameGiven;
    this.varClazz = varClazz;
    this.declare = declare;
    this.scope = scope;
  }

  /**
   * @return
   */
  boolean getDeclare()
  {
    return declare;
  }

  /**
   * @return
   */
  int getScope()
  {
    return scope;
  }

  /**
   * @return
   */
  String getClassName()
  {
    return varClazz;
  }

  /**
   * @return
   */
  String getVarName()
  {
    return nameGiven;
  }

}

/******************************************************************************
 * Log: $Log: VariableInfo.java,v $
 * Log: Revision 1.1  2004/04/16 13:45:10  bebbo
 * Log: @R contains only the JspCC - runtime moved to de.bb.bejy.http.jsp
 * Log:
 ******************************************************************************/