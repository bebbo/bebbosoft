<%@ page import="de.bb.bejy.mail.MailCfg" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<html>
  <head>
    <title>BEJY PO configuration</title>
    <link href="default.css" type="text/css" rel="stylesheet">
    <meta http-equiv="content-type" content="text/html; charset=iso-8859-1">
    <script src='script/redir.js' type="text/javascript"></script>
  </head>
  <body style="background-color:#FeeBa1;margin:0px;" onLoad='javascript:reframe()'>
    <%
      String result = "";
      String owner = request.getRemoteUser();
      de.bb.bejy.mail.MailDBI mDbi = null;
      MailCfg cfg  = (MailCfg)de.bb.bejy.Config.getInstance().getChild("mail");
      try {      
        mDbi = cfg.getDbi(this);
        mDbi.getMailDomains(owner);
      } catch (Exception e)
      {
        %>
    <b><font color="#ff0000">
        <hr><%=e.getMessage()%><hr>
        Please check:
        <ul><li>Did you create a proper mail database?
          </li><li>Did you add the JDBC driver JAR to the classpath? (bejy or bejy.cmd)
          </li><li>Did you specify the correct JDBC driver class? (bejy.xml)
          </li><li>Did you specify the correct mail database interface implementation? (bejy.xml)
          </li><li>Did you enter the correct JDBC url, with user and password? (bejy.xml)
          </li></ul>
      </font></b>
    <%
    return;
    }
    // process parameters if any
    String action = request.getParameter("action");
    if (action != null)
    {
    String domain = null;
    String po = request.getParameter("user");
    if (po == null)
    {
      po = "";
    }
    int idx = po.indexOf('@');
    if (idx > 0)
    {
      domain = po.substring(idx + 1);
      po = po.substring(0, idx);
    }
    String replytext = (String)request.getParameter("replytext");
    if (po != null && po.length() > 0)
    {
      if (action.equals("update"))
      {
		// update qutoa
		String quota = request.getParameter("quota");
        mDbi.setPoBoxQuota(owner, po, domain, null, quota);
		
        // update keep flag
        String keep = request.getParameter("keep");
        mDbi.setPoBoxKeep(owner, po, domain, null, "on".equals(keep));
        // update reply flag and text
        String reply = request.getParameter("reply");
      		    if (replytext.length() <= 255)
        {
          mDbi.setPoBoxReply(owner, po, domain, null, "on".equals(reply), replytext);
        }
    
        // remove forwarders
        for (Enumeration e = request.getParameterNames();e.hasMoreElements();)
        {
          String pn = (String)e.nextElement();
          if (pn.startsWith("fwid_"))
          {
            String fwid = request.getParameter(pn);
            if ("on".equals(fwid))
            {
              mDbi.removeForwarder(owner, po, domain, null, pn.substring(5));
            }
          }
        }
    
        // create new forwarder
        String forward = request.getParameter("forwarder");
        if (forward != null && forward.length() > 0 && forward.indexOf('@') > 0)
        {
          String notify = request.getParameter("notify");
          mDbi.createForwarder(owner, po, domain, null, forward, "on".equals(notify));
        }
    
        action = "settings";
      }
      else
      {
        replytext = mDbi.getPoBoxReplytext(owner, po, domain, null);
      }
    
      if (action.equals("settings"))
      {
        if (replytext == null)
     	  {
          replytext = "";
        }
        String quota = "0";
        String checked = "checked";
        String reply = "";
        ResultSet rs = mDbi.selectFromMailUser(po, domain);
        if (rs.next())
        {
          if (!rs.getBoolean(7))
            checked = "";
          if (rs.getBoolean(8))
            reply = "checked";
          quota = rs.getString(9);
        }
      rs.close();
    %>
    <h2>Extended settings for <b><%=po%>@<%=domain%></b></h2>
    <form action="mail.jsp" target="_self" method="POST">
      <input type="hidden" name="user" value="<%=po+'@'+domain%>">
      <table border='1'>
        <tr>
          <td>mail box quota in bytes (0 disables the limit, 10M sets it to 10 MB)<br></td>
          <td valign="top"><input alt="NOTE: set this value to 0 to disable mail box quota!"  type="text" name="quota" value="<%=quota%>" length="16"></td>
        </tr>
        <tr>
          <td>keep received mail<br></td>
          <td valign="top"><input alt="NOTE: if you uncheck this option, the mail account acts as a NOREPLY account.All incoming mail is discarded! There is no way do recover those lost mails!"  type="checkbox" name="keep" <%=checked%> ></td>
        </tr>
        <tr>
          <td>auto reply</td>
          <td colspan="3" ><input type="checkbox" name="reply" <%=reply%> ></td>
        </tr><%
        if (replytext.length() > 255)
        {%><tr><td colspan="4"><b>ERROR:</b>reply text exceeds 255 chars</td></tr><tr><%
        }%>
        <tr><td colspan="4" ><textarea name="replytext" rows=4 cols=80><%=replytext%></textarea></td>
        </tr>
      </table><br>
      <table border='1'>
        <tr><th bgcolor="#ccccff">Forwarder</th><th bgcolor="#ccccff">notify only</th></tr>
        <%
        String uid = null;
        rs = mDbi.selectFromMailUser(po, domain);
        if (rs.next())
        {
          uid = rs.getString(1);
        }
        rs.close();
        if (uid != null) {
          rs = mDbi.selectFromForward(uid);
          while (rs.next())
          {
            String fwid = rs.getString(1);
            %><tr><td><%=rs.getString(3)%></td>
          <td><%=rs.getBoolean(4)%></td>
          <td>remove<input type="checkbox" name="fwid_<%=fwid%>"></td>
        </tr><%
        }
        rs.close();
        }
        %>
        <tr><th colspan='3' align='left'><b>new forwarder</b></th></tr>
        <tr><td><input type="edit" name="forwarder" value=""></td>
          <td>notify<input type="checkbox" name="notify" ></td></tr>
        <tr>
          <td><input type="submit" name="action" value="update"></td>
          <td><input type="submit" name="action" value="back"></td>
        </tr>
      </table>
    </form>
    <%
      return;
    }
    }
    
    if (action.equals("change"))
    {
      String p1 = request.getParameter("p1");
      String p2 = request.getParameter("p2");
      if (p1.equals(p2)) {
        mDbi.setPoBoxPass(owner, po, domain, p1);
        result= "password changed for " + po + "@" + domain;
      } else {
        result = "ERROR: password mismatch - password NOT changed for " + po + "@" + domain;
      }
    } else
    if (action.equals("delete"))
    {
      if (!mDbi.removePoBox(owner, po, domain)) {
        result = "ERROR: did not remove " + po + "@" + domain;
      } else {
        result = "removed " + po + "@" + domain;
      }
    } else
    if (action.equals("create"))
    {
      String p1 = request.getParameter("p1");
      String p2 = request.getParameter("p2");
      if (p1.equals(p2))
      {
        if (!mDbi.createPoBox(owner, po, domain)) {
          result = "ERROR: did not create " + po + "@" + domain;
        } else
        {
          result = "created PO box:" + po + "@" + domain;
          if (p1.length() > 0 && !mDbi.setPoBoxPass(owner, po, domain, p1)) {
            result += ", but password was not set!";
          }
        }
      } else {
        result = "ERROR: password mismatch for " + po + "@" + domain;
      }
    }
    }
    %>
    <h2>Mail configuration for all domains of <%=owner%></h2>
    <b><font color='#ff0000'><%=result%></font></b>
    <table border='1'>
      <%
        Vector domains = mDbi.getMailDomains(owner);
        for (int i = 0; i < domains.size(); ++i)
        {
          String domain = (String)domains.elementAt(i);
      %>
      <tr><th align="left" colspan='6' bgcolor="#ccccff"><%=domain%></th><tr>
      <tr><td align="left">PO box</td><td>new password</td><td>verify password</td></tr>
      <%
          Vector boxes = mDbi.getPoBoxes(domain);
          for (int j = 0; j < boxes.size(); ++j)
          {
            String name = (String)boxes.elementAt(j);
      %>
      <tr><form action="mail.jsp" target="_self" method="POST">
          <td><%=name+'@'+domain%><input type="hidden" name="user" value="<%=name+'@'+domain%>"></td>
          <td><input type="password" name="p1" value=""></td>
          <td><input type="password" name="p2" value=""></td>
          <td ><input type="submit" name="action" value="change"></td>
          <td><input type="submit" name="action" value="delete"></td>
          <td><input type="submit" name="action" value="settings"></td>
        </form></tr>
      <%
          }
      %><tr><td colspan='6'>&nbsp;</td><tr>
      <%
        }
      %>
      <tr><th align="left">New PO box</th><td>new password</td><td>verify password</td></tr>
      <tr><form action="mail.jsp" target="_self" method="POST">
          <td><input type="edit" name="user" value=""></td>
          <td><input type="password" name="p1" value=""></td>
          <td><input type="password" name="p2" value=""></td>
          <td><input type="submit" name="action" value="create"></td>
        </form></tr>
      <tr><th align="left" colspan="5">&nbsp;</th></tr>
      <tr></tr>
    </table>
    <b><font color="#ff0000"><%=result%></font></b>
  </body>
</html>
<%
  cfg.releaseDbi(this, mDbi);
%>