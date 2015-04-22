<%@ page import="de.bb.bejy.mail.MailDBI" %>
<%@ page import="de.bb.bejy.mail.MailCfg" %>
<%@ page import="de.bb.bejy.Configurable" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="de.bb.bejy.Config" %>
<html>
  <head><link href="default.css" type="text/css" rel="stylesheet">
    <script src='script/redir.js' type="text/javascript"></script>
  </head>
  <body style="background-color:#FeeBa1;margin:0px;" onLoad='javascript:reframe()'>
    <h1>BEJY Mail Config Wizard - Basic Settings</h1>
    <form action="w_mail.jsp" method="POST" >
    <input type="hidden" name="$step" value="1" />
      <table border="1" width="100%">
        <!-- available JDBC drivers -->
        <%
          List dbis = Config.getImplementors(de.bb.bejy.mail.MailDBI.class , "*Dbi.class");
          List drivers = Config.getImplementors(java.sql.Driver.class , "*.class");
          if (drivers.size() == 0) {
        %>
        <tr><td>
            <font color="red">No JDBC driver in class path</font><br>
            Please add at least the JDBC driver you want to use to BEJY's class path
            <ul><li>check bejy.cmd</li>
              <li>or check bejy.sh</li></ul>
          </td></tr>
        <%} else {
          Config cfg = Config.getInstance();
          Configurable dns = cfg.getChild("dns");
          MailCfg mail = (MailCfg)cfg.getChild("mail");
        %>
        <!-- DNS config -->
        <tr><td>DNS server</td>
          <td><input type="text" name="$dns" value="<%=dns==null?"":dns.getProperty("server")%>" />
          </td><td>Enter the IP address of your preferred DNS server.</td></tr>
        <!-- main domain -->
        <tr><td>main domain</td>
          <td><input type="text" name="$main" value="<%=mail==null?"":mail.getProperty("mainDomain")%>" />
          </td><td>Enter the main domain for this mail server (used in welcome messages).</td></tr>
        <!-- JDBC drivers -->
        <tr><td>Database interface (dbi)</td>
          <td><select name="$dbi">
              <%
                String mailDbi = mail != null ? mail.getProperty("mailDbi") : "";
                for (Iterator i = dbis.iterator(); i.hasNext();)
                {
                  Object dbi = i.next();
                  %><option value="<%=dbi%>" <%= mailDbi.equals(dbi.toString())?"selected":"" %> >
                <%= dbi %>
              </option>
              <%
                }
              %>
            </select>
          </td><td>select the MailDBI that matches your used database.</tr>
        <tr><td>JDBC driver</td>
          <td><select name="$jdbc">
              <%
                String jdbcDriver = mail != null ? mail.getProperty("jdbcDriver") : "";
                for (Iterator i = drivers.iterator(); i.hasNext();)
                {
                  Object jdbc = i.next();
                  %><option value="<%=jdbc%>" <%= jdbcDriver.equals(jdbc)?"selected":"" %> >
                <%= jdbc %>
              </option>
              <%
                }
              %>
            </select>
          </td><td>select the JDBC driver that matches the used database</tr>
        <!-- JDBC URL-->
        <%
          String url = mail != null ? mail.getProperty("jdbcUrl") : "";
          String user = "";
          String pass = "";
          int q = url.indexOf('?');
          if (q > 0) {
            String rest = url.substring(q + 1);
            url = url.substring(0, q);
            q = rest.indexOf('&');
            String rest2 = "";
            if (q > 0) {
              rest2 = rest.substring(q + 1);
              rest = rest.substring(0, q);
            }
            if (rest.startsWith("user=")) user = rest.substring(5);
            if (rest2.startsWith("user=")) user = rest2.substring(5);
            if (rest.startsWith("password=")) pass = rest.substring(9);
            if (rest2.startsWith("password=")) pass = rest2.substring(9);
          }
        %>
        <tr><td>JDBC URL</td>
          <td><input size="60" type="text" name="$url" value="<%=url%>" />
          </td><td>enter the JDBC url. Some examples:
            <ul>
              <li>MySQL: jdbc:mysql://localhost/mail</li>
              <li>HSQL: jdbc:hsqldb:hsql://localhost/mail</li>
              <li>PostgreSQL: jdbc:postgresql://localhost/mail</li></ul>
          </td></tr>
        <tr><td>JDBC user</td>
          <td><input type="text" name="$user" value="<%=user%>" />
          </td><td>the user name which is used for the database connection</td></tr>
        <tr><td>JDBC password</td>
          <td><input type="text" name="$pass" value="<%=pass%>" />
          </td><td>the password which is used for the database connection</td></tr>
        <tr><td>JDBC status</td>
          <td colspan="2">
          <%
            boolean ok = false;
            if (mail == null) {%> not configured, correct values and update <% }
            else {
              MailDBI dbi = null;
              try {
                dbi = mail.getDbi(request);
                %>Mail database connection established<%
                ok = true;
              } catch (Exception ex) {
              ex.printStackTrace();
              %><font color="red"><%= ex.getMessage() %></font><%
              } finally {
                if (dbi != null) mail.releaseDbi(request, dbi);
              }              
            }
            boolean hadError = "true".equals(session.getAttribute("hadJdbcError"));
            hadError |= !ok;
            session.setAttribute("hadJdbcError", "" + hadError);
          %>
          </td></tr>
          <tr><td colspan="3">
            <input type="submit" name="$action" value="update">
            <%if(ok){%>
            <input type="submit" name="$action" value="save & continue">
            <%}%>
          </td></tr>
        <%}%>
      </table>
    </form>
  </body>
</html>
  