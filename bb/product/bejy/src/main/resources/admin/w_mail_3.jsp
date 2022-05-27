<%@ page import="java.util.Iterator" %>
<%@ page import="de.bb.bejy.Config" %>
<%@ page import="de.bb.bejy.Server" %>
<html>
  <head><link href="default.css" type="text/css" rel="stylesheet">
    <script src='script/redir.js' type="text/javascript"></script>
  </head>
  <body style="background-color:#FeeBa1;margin:0px;" onLoad='javascript:reframe()'>
    <h1>BEJY Mail Config Wizard - Servers &amp; Protocols</h1>
    <form action="w_mail.jsp" method="POST" >
    <input type="hidden" name="$step" value="3" />
      <table border="1" width="100%">
    <tr><th>server</th><td>port</td><td>protocol</td></tr>
<%
  Config cfg = Config.getInstance();
  boolean hasPop = false;
  boolean hasSmtp = false;
  boolean hasImap = false;
  for (Iterator i = cfg.children("server"); i.hasNext();)
  {
    Server server = (Server)i.next();
    String name = server.getName();
    if ("POP3".equals(name) || "IMAP4".equals(name) || "SMTP".equals(name))
    {
      if ("POP3".equals(name)) hasPop = true;
      if ("IMAP4".equals(name)) hasImap = true; 
      if ("SMTP".equals(name)) hasSmtp = true;    
    %>
    <tr><td><%=server.getProperty("name")%></td><td><%=server.getProperty("port")%></td></tr>
<%    
    } 
  }
%>      
        <tr><td colspan="3">
<%
  if (!hasPop) {%>        
            <input type="submit" name="$action" value="add POP3 server">
  <%}
  if (!hasSmtp) {%>        
            <input type="submit" name="$action" value="add SMTP server">
  <%}
  if (!hasImap) {%>        
            <input type="submit" name="$action" value="add IMAP4 server">
  <%}
  if (hasPop && hasSmtp && hasImap) {%>        
            <input type="submit" name="$action" value="save & finish">
  <%}%>
          </td></tr>
      </table>
    </form>
  </body>
</html>
  