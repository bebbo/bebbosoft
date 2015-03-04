/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/JspEngineInfoImpl.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:46:09 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * (c) 1999-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * a JspEngineInfoImpl
 *
 */

package de.bb.bejy.http.jsp;
import javax.servlet.jsp.JspEngineInfo;

class JspEngineInfoImpl extends JspEngineInfo
{
  public String getImplementationVersion()
  {
    return "1.2";
  }
  public java.lang.String getSpecificationVersion() // from javax.servlet.jsp.JspEngineInfo
  {
    return "1.3";
  }
}

/*
 * $Log: JspEngineInfoImpl.java,v $
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.2  2002/11/06 09:41:41  bebbo
 * @I reorganized imports
 * @I removed unused variables
 *
 * Revision 1.1  2001/03/29 19:55:33  bebbo
 * @N moved to this location
 *
 */
