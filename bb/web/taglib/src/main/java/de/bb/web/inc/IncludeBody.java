package de.bb.web.inc;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.TagSupport;

public class IncludeBody extends TagSupport {
  private String page;

  public IncludeBody() {
    init();
  }
  
  public int doEndTag() throws JspException
  {
    BodyContent x = pageContext.pushBody();
    try {
      pageContext.include(page);
    } catch (Exception e) {
      PrintWriter pw = new PrintWriter(x);
      e.printStackTrace(pw);
      pw.flush();      
    } finally {
      pageContext.popBody();
    }
    String content = x.getString();
    int start = content.indexOf("<body");
    if (start >= 0) {
      int ket = content.indexOf('>', start) + 1;
      int end = content.indexOf("</body>", ket);
      if (end < 0) end = content.length();
      content = content.substring(ket, end);
    }
    try {
      pageContext.getOut().write(content);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    init();
    return super.doEndTag();
  }  
  private void init() {
    page = null;
  }

  public String getPage() {
    return page;
  }

  public void setPage(String page) {
    this.page = page;
  }
}
