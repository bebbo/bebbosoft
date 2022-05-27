<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.Collection"%>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="de.bb.wiki.EnDeCode"%>
<%@ page import="de.bb.wiki.WikiManager"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>edit a wiki</title>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<% WikiManager wm = (WikiManager)request.getAttribute("wikiManager");
  if (wm != null && wm.getCssName() != null) {
%> <link href='<%= wm.getCssName() %>' type='text/css' rel='stylesheet'>
<% }
%>
</head>
<body>
<%
    if (de.bb.wiki.Wikilet.canEdit(session)) {
  // get the action URL
  String url = (String) request.getAttribute("wiki_url");
  // get the wiki content InputStream
  BufferedReader br = (BufferedReader) request.getAttribute("wiki_content");
  if (url == null || br == null)
    throw new Exception("no wiki page to edit");

//  boolean isWiki = url.endsWith(".wiki");
  Collection c = (Collection) request.getAttribute("wiki_locks");
  HashSet set = new HashSet();
  if (c != null)
    set.addAll(c);
  set.remove(request.getRemoteAddr());
  if (set.size() > 0)
  {
%><h1>Warning - this file is also under modification from</h1>
<ul>
  <%
    for (Iterator i = set.iterator(); i.hasNext();) {
  %><li><%=i.next()%></li>
  <%
    }
  %>
</ul>
<%
  }
  
  String html = (String)request.getAttribute("wiki_html");
  if (html != null) { %>
  <div style='background-color: #ff0000'>
    <h1>THIS IS A PREVIEW - EDIT BELOW</h1>
  </div>
  <%= html %>
  <div style='background-color: #00ff00'>
    <h1>EDIT HERE</h1>
  </div>
<%}
%>
<form name="f1" action="<%=url%>" method="post" accept-charset="UTF-8" >
  <textarea name="content" rows="30" cols="80"><%
  try
  {
    // and emit the content
    for (;;)
    {
      String line = br.readLine();
      if (line == null)
        break;
      out.write(EnDeCode.escape(line));
      if (br.ready()) {
        out.write(0xd);
        out.write(0xa);
      }
    }
  } catch (Exception e)
  {
    // ignore - we create it new then
  } finally
  {
    // close and suppress all errors
    try
    {
      br.close();
    } catch (Exception ex)
    {
    }
  }
%></textarea> <br>
<input type="submit" name="what" value="save" /> <input type="submit"
  name="what" value="preview" /> <input type="submit" name="what"
  value="cancel" />  </form>
<%} else { %>
  no permission
<%}%>
</body>
</html>