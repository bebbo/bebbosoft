<%@ page import="de.bb.bejy.mail.MailDBI" %>
<%@ page import="de.bb.bejy.Configurator" %>
<%@ page import="de.bb.bejy.mail.MailCfg" %>
<%@ page import="de.bb.bejy.Configurable" %>
<%@ page import="de.bb.bejy.Config" %>
<%-- action handling --%>
<%
  boolean reload = false;
  Config cfg = Config.getInstance();
  String step = request.getParameter("$step");
  if (step == null) step = "1";
  String action = request.getParameter("$action");
  if ("create tables".equals(action)) {
    String file = request.getParameter("$createFile");
    executeSQL(file);
  } else
  if ("drop tables".equals(action)) {
    String file = request.getParameter("$dropFile");
    executeSQL(file);
  } else
  if (action != null && action.startsWith("migrate tables from")) {
    String file = request.getParameter("$patchFile");
    System.out.println(file);
    executeSQL(file);
  } else
  if ("continue".equals(action)) {
    try {
      step = "" + (Integer.parseInt(step)+1);
    } catch (Exception ex) {
      step = "1";
    }
  } else  
  if ("save & finish".equals(action)) {      
    try {
      Config.save();
    } catch (Exception ex) { ex.printStackTrace(); }
    reload = true;
  } else
  if ("back".equals(action)) {
    try {
      step = "" + (Integer.parseInt(step)-1);
    } catch (Exception ex) {
      step = "1";
    }
  } else  
  if ("add POP3 server".equals(action)
   || "add IMAP4 server".equals(action)
   || "add SMTP server".equals(action))
  {
    step = "3";
    Configurator c = cfg.getConfigurator("tcp/ip server");    
    de.bb.bejy.Server s = (de.bb.bejy.Server)c.create();
    s.setConfigurator(c);
    cfg.addChild("server", s);
    
    c = cfg.getConfigurator("tcp/ip protocol");
    String caName = "";
    if ("add POP3 server".equals(action)) {
      caName = "de.bb.bejy.mail.Pop3Factory";
    } else
    if ("add IMAP4 server".equals(action))
    {
      caName = "de.bb.bejy.mail.ImapFactory";
    } else
    if("add SMTP server".equals(action)) {
      caName = "de.bb.bejy.mail.SmtpFactory";
    }
        
    Class cz = Class.forName(caName);
    Configurable p = (Configurable) cz.newInstance();
    p.setProperty("class", caName);

    if ("add POP3 server".equals(action)) {
      s.setProperty("name", "POP3");
      s.setProperty("port", "110");
    } else
    if ("add IMAP4 server".equals(action))
    {
      s.setProperty("name", "IMAP4");
      s.setProperty("port", "143");
    } else
    if("add SMTP server".equals(action)) {
      s.setProperty("name", "SMTP");
      s.setProperty("port", "25");
    }

    p.setConfigurator(c);
    s.addChild("protocol", p);
      
    //reload = true;
  } else
  if ("update".equals(action)
    ||"save & continue".equals(action))
  {
    // apply parameters
    Configurable dns = cfg.getChild("dns");
    if (dns == null) {
      Configurator c = cfg.getConfigurator("dns");
      dns = c.create();
      dns.setConfigurator(c);
      cfg.addChild("dns", dns);
      reload = true;
    }
    
    MailCfg mail = (MailCfg)cfg.getChild("mail");
    if (mail == null) {
      Configurator c = cfg.getConfigurator("mail");
      mail = (MailCfg) c.create();
      mail.setConfigurator(c);
      cfg.addChild("mail", mail);
      reload = true;
    }

    String nameServer = request.getParameter("$dns");
    String mainDomain = request.getParameter("$main");
    String mailDbi = request.getParameter("$dbi");
    String jdbcDriver = request.getParameter("$jdbc");
    String jdbcUrl = request.getParameter("$url");
    String jdbcUser = request.getParameter("$user");
    String jdbcPass = request.getParameter("$pass");
    
    if (nameServer != null)
      dns.setProperty("server", nameServer);
    if (mainDomain != null)
      mail.setProperty("mainDomain", mainDomain);
    if (mailDbi != null)
      mail.setProperty("mailDbi", mailDbi);
    if (jdbcDriver != null)
      mail.setProperty("jdbcDriver", jdbcDriver);
    if (jdbcUrl != null) {
      if (jdbcUser != null) {
        jdbcUrl += "?user=" + jdbcUser;
        if (jdbcPass != null)
          jdbcUrl += "&password=" + jdbcPass;
      }
      mail.setProperty("jdbcUrl", jdbcUrl);
    }
    
    if ("save & continue".equals(action)) {
    try {
      Config.save();
      session.removeAttribute("hadJdbcError");
      } catch (Exception ex) { ex.printStackTrace(); }
      step = "2";
    }
        
    cfg.deactivate(Config.getLogFile());
    cfg.activate(Config.getLogFile());
    
  }
  if (reload) {%>
<script>parent.navi.location= "navi.jsp";</script>      
  <%
}
%>
<jsp:include page='<%= "w_mail_" + step + ".jsp" %>' />
<%!
  String executeSQL(String fileName) {
    Config cfg = Config.getInstance();
    MailCfg mail = (MailCfg)cfg.getChild("mail");
    MailDBI dbi = null;
    try {
      dbi = mail.getDbi(fileName);
      
      return dbi.executeSQL(fileName);
      
    } catch (Exception ex) {
      return "FATAL:" + ex.getMessage();
    } finally {
      if (dbi != null) mail.releaseDbi(fileName, dbi);
    }
  }
%>