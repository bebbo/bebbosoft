<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<%@ page import="de.bb.bejy.*" %>
<%@ page import="java.util.*" errorPage="/" %>
<html>
<head>
  <title>navigation</title>
  <link href="default.css" type="text/css" rel="stylesheet" >
  <script src='script/menu.js' type="text/javascript"></script>
</head>
<body style="margin: 0 0 0 0;background-color: #FDD468;">
<div style="position:absolute;border:none;">
<script type="text/javascript">
<%
  Config cfg = Config.getInstance();
  LinkedList ll = new LinkedList();
  ll.add(cfg);
  Iterator i = ll.iterator(); //cfg.children();

  Stack iters = new Stack();


  for(int n = 0;;++n)
  {
    if (!i.hasNext())
    {
      if (iters.size() == 0)
      {
        break;
      }
      %>$e();
<%
      i = (Iterator)iters.pop();
      --n; // fix counter

      continue;
    }

    Configurable current = (Configurable)i.next();
    Configurator ct = current.getConfigurator();
    if (ct == null)
      continue;

    Iterator j = current.children();
    boolean hasNext = i.hasNext();
    boolean hasChildren = j.hasNext();
    String name = current.getName();
    if (!ct.getClass().equals(current.getClass())) {
      name = ct.getName() + " " + name;
    }
    %>$m(<%=hasChildren%>,<%=hasNext || n==0%>,"<%=name%>","main.jsp?item=<%=n%>", "main");
<%
    if (hasChildren)
    {
      iters.push(i);
      i = j;
    } else {%>$e();
<%
    }
  }
%>
$m(1, 1, "E-Mail", "o_mail.jsp", "main");
  $m(0, 1, "Mail Wizard", "w_mail.jsp", "main");
  $e();
  $m(0, 1, "Domains", "domain.jsp", "main");
  $e();
  $m(0, 1, "Mail User", "mail.jsp", "main");
  $e();
  $m(0, 0, "Recover/Import", "import.jsp", "main");
  $e();
$e();
$m(0, 1, "Status", "status.jsp", "main");
$e();
$m(0, 0, "Logout", "logout.jsp", "main");
$e();
$z();
<%
  String item = request.getParameter("item");
  if (item != null) {
%>
$f("<%=item%>");
<%}%>
</script>
</div>
</body>
</html>