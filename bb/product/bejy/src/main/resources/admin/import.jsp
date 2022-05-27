<%@ page import="de.bb.bejy.mail.MailCfg" %>
<%@ page import="de.bb.bejy.Config" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
  <title>foot</title>
  <link href="default.css" type="text/css" rel="stylesheet"/>
  <script src='script/redir.js' type="text/javascript"></script>
</head>
<body style="background-color:#FeeBa1;margin:0px;" onLoad='javascript:reframe()'>
<h1>Recover e-mails from existant mail folder</h1>
<%
  String action = request.getParameter("$action");
  Config cfg = Config.getInstance();
  MailCfg mail = (MailCfg)cfg.getChild("mail");
  if (mail == null)
  {%><h1>Mail is not configured properly</h1><%
    return;
  }
  try {
    if ("recover e-mails".equals(action))
    {
      mail.recoverMails(false, false);
    } else
    if ("recover e-mails and users".equals(action))
    {
      mail.recoverMails(true, false);
    } else
    if ("recover e-mails, users and domains".equals(action))
    {
     // mail.recoverMails(true, true);
    }
  } catch (Exception ex) {
  %><h1>Mail is not configured properly</h1><%
    return;
  }
%>
<form action="import.jsp" method="post">
<table border=0 width="100%">
<tr><td>recover e-mails for configured users</td><td>
    <input type="submit" name="$action" value="recover e-mails"/>
  </td></tr>
<tr><td>recover e-mails and create missing users for your domains</td><td>
    <input type="submit" name="$action" value="recover e-mails and users"/>
  </td></tr>
</table>
<h1>Import sendmail files</h1>
<table border=0 width="600">
<tr><td>Use the commandline to import sendmail files:</td></tr>
<tr><td>java -cp <%=
  System.getProperty("java.class.path")
%> de.bb.bejy.mail.ImportSendmail
[-c&lt;configfile&gt;=bejy.xml]
[-d&lt;domainname&gt;=<%= mail.getProperty("mainDomain") %>]
[-p&lt;path_to_sendmail&gt;=/var/mail]
</td></tr>
</table>
</form>
</body>
</html>
