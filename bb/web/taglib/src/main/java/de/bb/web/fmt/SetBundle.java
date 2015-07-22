/* 
 * Created on 20.10.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.web.fmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author sfranke
 */
public class SetBundle extends TagSupport
{
  private String bundle;

  private String scope;

  private String var;

  private long lastModified;

  private PropertyResourceBundle rb;

  public SetBundle()
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

  public int doEndTag() throws JspException
  {
    int iscope = PageContext.APPLICATION_SCOPE;
    if ("request".equals(scope))
      iscope = PageContext.REQUEST_SCOPE;
    else if ("session".equals(scope))
      iscope = PageContext.SESSION_SCOPE;
    else if ("application".equals(scope))
      iscope = PageContext.APPLICATION_SCOPE;
    
    String lang = (String)pageContext.getAttribute("bundle.language", iscope);
    if (lang != null) 
      loadBundle(bundle + "_" + lang + ".properties");
    if (rb == null)
      loadBundle(bundle + ".properties");
   
    if (rb == null)
      throw new JspException("could not load ressource bundle: " + bundle);
    
    this.pageContext.setAttribute(var, rb, iscope);

    init();

    return super.doEndTag();
  }

  private void loadBundle(String rbName)
  {
    File file = new File(rbName);
    if (file.exists() && file.lastModified() > lastModified) {
      lastModified = file.lastModified();
      FileInputStream fis = null;
      try
      {
        fis = new FileInputStream(rbName);
        rb = new PropertyResourceBundle(fis);
      } catch (IOException e)
      {
        // rb remains null
      } finally {
        try
        {
          if (fis != null)
            fis.close();
        } catch (IOException e)
        {
          // quiet
        }
      }
    }
    // no ressource bundle? load from classloader
    if (rb == null) {
      InputStream is = null;
      try
      {
        is = getClass().getClassLoader().getResourceAsStream(rbName);
        if (is != null)
          rb = new PropertyResourceBundle(is);
      } catch (IOException e)
      {
        // rb remains null
      } finally {
        try
        {
          if(is != null)
            is.close();
        } catch (IOException e)
        {
          // quiet
        }
      }      
    }
  }

  /**
   * 
   */
  private void init()
  {
    var = "de.bb.web.fmt.Bundle";
    scope = null;
    bundle = null;
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