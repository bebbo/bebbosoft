/* 
 * Created on 20.10.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.web.fmt;

import java.util.ResourceBundle;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author sfranke
 */
public class Message extends TagSupport
{
  private String key;

  private String bundle;

  private String scope;

  private String var;

  public Message()
  {
    init();
  }

  public String getBundle()
  {
    return bundle;
  }

  public void setBundle(String bundle)
  {
    this.bundle = bundle;
  }

  public String getScope()
  {
    return scope;
  }

  public void setScope(String scope)
  {
    this.scope = scope;
  }

  public String getKey()
  {
    return key;
  }

  public void setKey(String key)
  {
    this.key = key;
  }

  public int doEndTag() throws JspException
  {
    ResourceBundle rb = null;

    if (bundle == null) {
      int iscope = PageContext.APPLICATION_SCOPE;
      if ("request".equals(scope))
        iscope = PageContext.REQUEST_SCOPE;
      else if ("session".equals(scope))
        iscope = PageContext.SESSION_SCOPE;
      else if ("application".equals(scope))
        iscope = PageContext.APPLICATION_SCOPE;

      rb = (ResourceBundle) pageContext.getAttribute(var, iscope);
    } else {
      rb = ResourceBundle.getBundle(bundle);
    }

    try {
      String val = null;
      try {
        val = rb.getString(key);
      } catch (Exception ex) {
        val = key;
      }
      pageContext.getOut().write(val);
    } catch (Exception e) {
      e.printStackTrace();
    }
    init();

    return super.doEndTag();
  }

  /**
   * 
   */
  private void init()
  {
    var = "de.bb.web.fmt.Bundle";
    scope = null;
    bundle = null;
    key = null;
  }

  public String getVar()
  {
    return var;
  }

  public void setVar(String var)
  {
    this.var = var;
  }
}