<%@ page import="java.sql.ResultSet" %>
<%@ page import="de.bb.bejy.mail.MailDBI" %>
<%@ page import="de.bb.bejy.mail.MailCfg" %>
<%@ page import="de.bb.bejy.Config" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
  <head><link href="default.css" type="text/css" rel="stylesheet"/>
    <title>BEJY domain config</title>
    <script src='script/redir.js' type="text/javascript"></script>
  </head>
  <body style="background-color:#FeeBa1;margin:0px;" onLoad='javascript:reframe()'>
    <h1>BEJY Mail Config Wizard - Configure Domains</h1>
      <%
      Config cfg = Config.getInstance();
      MailCfg mail = (MailCfg)cfg.getChild("mail");
      if (mail == null)
      {%><h1>Mail is not configured properly</h1><%
        return;
      }
      MailDBI mdb = null;
      try {
        mdb = mail.getDbi(request);

        String action = request.getParameter("$action");
        if ("create domain".equals(action)) {
          String domain = request.getParameter("$new_domain");
          String owner = request.getParameter("$new_owner");
          String guessPermission = request.getParameter("$guessPermission");
          if (domain != null && domain.trim().length() > 0)
            mdb.createDomain(domain.trim(), owner, "true".equals(guessPermission));
        } else
        if ("update domain".equals(action)) {
          String domain = request.getParameter("$domain");
          String owner = request.getParameter("$owner");
          String guessPermission = request.getParameter("$guessPermission");
          if (domain != null && domain.trim().length() > 0)
            mdb.createDomain(domain, owner, "true".equals(guessPermission));
        } else
        if ("delete domain".equals(action)) {
          String domain = request.getParameter("$domain");
          if (domain != null && domain.trim().length() > 0)
            mdb.deleteFromDomain(domain);
        }
%>
    <form action="domain.jsp" method="post" >
      <input type="hidden" name="$step" value="3" />
      <input type="hidden" name="$domain" value="" />
      <input type="hidden" name="$owner" value="" />
      <table border="1" width="100%">
        <tr><th>domain</th><th>owner</th><th>guessPermission</th><th>action</th></tr>
    <%
       ResultSet rs = mdb.selectFromDomain();
        while (rs.next()) {
          String domain = rs.getString(1);
          String owner = rs.getString(2);
          boolean guess = rs.getBoolean(3);
          %>
          <tr><td><%=domain%></td>
            <td><input type="text" name="$domain_<%=domain%>" value="<%=owner%>"/></td>
            <td><select name="$guess_<%=domain%>"><option value="true" <%= guess ? "selected" : ""%>>true</option><option value="false" <%= guess ? "" : "selected"%>>false</option></select></td>
            <td><input type="submit" name="$action" value="update domain"
                 onClick="document.forms[0]['$domain'].value='<%=domain%>';
                          document.forms[0]['$owner'].value=document.forms[0]['$domain_<%=domain%>'].value;
                          document.forms[0]['$guessPermission'].value=document.forms[0]['$guess_<%=domain%>'].value
                          "
                />
                <input type="submit" name="$action" value="delete domain"
                 onClick="document.forms[0]['$domain'].value='<%=domain%>';
                          return confirm('All mail accounts will be erased too!\nReally delete domain <%=domain%>?')"/>
            </td>
          </tr>
          <%
        }
        rs.close();
            %>
          <tr>
            <td><input type="text" name="$new_domain" value="" size="50" /></td>
            <td><input type="text" name="$new_owner" value="<%=request.getRemoteUser()%>"/></td>
            <td><select name="$guessPermission"><option value="true" selected>true</option><option value="false">false</option></select></td>
            <td><input type="submit" name="$action" value="create domain"/></td>
          </tr>
          <%
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      finally {
        if (mdb != null) mail.releaseDbi(request, mdb);
      }

    %>
      </table>
    </form>
  </body>
</html>
