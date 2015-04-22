<%@ page import="java.sql.Timestamp" %>
<%@ page import="de.bb.bejy.mail.MailDBI" %>
<%@ page import="java.io.File" %>
<%@ page import="de.bb.bejy.mail.MailCfg" %>
<%@ page import="de.bb.bejy.Config" %>
<html>
  <head><link href="default.css" type="text/css" rel="stylesheet">
    <script src='script/redir.js' type="text/javascript"></script>
  </head>
  <body style="background-color:#FeeBa1;margin:0px;" onLoad='javascript:reframe()'>
    <h1>BEJY Mail Config Wizard - Create Database</h1>
    <%
      Config cfg = Config.getInstance();
      MailCfg mail = (MailCfg)cfg.getChild("mail");
      String mailDbi = mail.getProperty("mailDbi");
      int dot = mailDbi.lastIndexOf('.');
      String db = mailDbi.substring(dot + 1);
      db = db.substring(0, db.length() - 3);
      String create = "create_" + db.toLowerCase() + ".sql";
      String drop = "drop_" + db.toLowerCase() + ".sql";
      
      MailDBI dbi = mail.getDbi(this);
      String fromVersion = dbi.getPatchlevel();
      String toVersion = null;
      String patch = null;
      if (fromVersion != null) {
           dot = fromVersion.lastIndexOf('.') + 1;
           int next = Integer.parseInt(fromVersion.substring(dot)) + 1;
           toVersion = fromVersion.substring(0, dot) + next;
           patch = "patch" + fromVersion + "_to_" + toVersion + "_" + db.toLowerCase() + ".sql";
      }
    %><form action="w_mail.jsp" method="POST" >
      <input type="hidden" name="$step" value="2" />
      <table border="1" width="100%">
        <tr><td>Database</td><td><%=db%>
          </td></tr>
        <tr><td>Create script</td><td><%=create%>
    <%
      File cf = new File("sql", create);
      if (!cf.exists()) {%> <font color="red"><br>SQL create file not found: <%=
      cf.getAbsolutePath()%> </font><% }
    %>  </td></tr><tr><td>Drop script</td><td><%=drop%>
    <%
      File df = new File("sql", drop);
      if (!df.exists()) {%> <font color="red"><br>SQL drop file not found: <%=
      df.getAbsolutePath()%> </font><% }
    %>
    <%
      File pf = new File("sql", patch);
      if (!pf.exists()) patch = null;
    %>
          </td></tr>
        <tr><td colspan="2">
            <input type="submit" name="$action" value="back">
<%
  boolean hasDatabase = false;
  boolean hasTables = false;
  boolean hasCorrectTables = false;
  MailDBI mdb = null;
  try {
    mdb = mail.getDbi(request);
    hasDatabase = true;
    mdb.selectFromImapUnit("");
    hasTables = true;
    mdb.selectFromSpool(new java.sql.Timestamp(0));
    hasCorrectTables = patch == null;
  } catch (Exception ex) {
    // ex.printStackTrace();
  }
  finally {
    if (mdb != null) mail.releaseDbi(request, mdb);
  }

  if (!hasTables && cf.exists()) {%>            
            <input type="hidden" name="$createFile" value="<%= cf.getAbsolutePath() %>">
            <input type="submit" name="$action" value="create tables">
<% }            
  if (hasDatabase && df.exists()) {%>            
<% if (hasTables) { %>            
            <input type="hidden" name="$dropFile" value="<%= df.getAbsolutePath() %>">
            <input type="submit" name="$action" value="drop tables"
            onClick="return confirm('All data in the mail database will be lost if you drop the tables. Really drop the tables?')" />
<% 
   if (hasCorrectTables) { %>            
     <input type="submit" name="$action" value="continue">
<% } else { %>
            <input type="hidden" name="$patchFile" value="<%= pf.getAbsolutePath() %>">
            <input type="submit" name="$action" value="migrate tables from <%= fromVersion %> to <%= toVersion %>"
            onClick="return confirm('If the current database does not fit the expected state, it might become corrupt. Have you made a backup and are you really sure?')" />
<% } } }%>            
          </td></tr>
      </table>
    </form>
  </body>
</html>
