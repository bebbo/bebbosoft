<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.Collection"%>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="de.bb.wiki.EnDeCode"%>
<%@ page import="java.io.File"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>edit a wiki</title>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
</head>
<body>
<%
    if (de.bb.wiki.Wikilet.canEdit(session)) {
  // get the action URL
  String url = (String) request.getAttribute("wiki_upload");
  String rp = request.getRealPath(url);
  File ufile = new File(url);
  File dir = new File(rp);
  if (!dir.isDirectory()) {
    dir = dir.getParentFile();
    ufile = ufile.getParentFile();
  }
%>
<script type="text/javascript">
function $ask(form, file) {
  if(!confirm('really delete "' + file + '"?'))
      return;
  form.delme.value=file;
}
</script>
<form action="?" method="post" enctype="multipart/form-data">
Upload a file: <input type="file" name="fn"/><br>
<input type="submit" name="what" value="upload" />
<input type="submit" name="what" value="cancel" />
<input type="hidden" name="delme" value="" />
<h2>files in <%= ufile %> </h2>
<table border="1">
<tr><th>file name</th><th>date</th><th>size</th><th>D</th></tr>
<%
    String [] files = dir.list();
    if (files != null)
    for (int i = 0; i < files.length; ++i) {
      File f = new File(dir, files[i]);
      if (f.isDirectory()) continue;
%><tr><td><%= files[i] 
%></td><td><%= de.bb.util.DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(f.lastModified()) 
%></td><td align="right"><%= f.length() 
%></td><td><input type="submit" name="what" value="X" onClick="$ask(this.form, '<%= files[i]%>')"/></td></tr>
<%}
%>
</table>

<%} else { %>
  no permission
<%}%>
</form>
</body>
</html>