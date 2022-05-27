<html><head>
<title>A JSP page</title>
<link href="/default.css" type=text/css rel=stylesheet>
</head><body>
<h2>This is a JSP page.</h2>
A <a href="/WelcomeServlet">Welcome</a> servlet.<br>
A <a href="/WelcomeTest">Test</a> servlet.<br>
<table border=1>
<tr><th>name</th><th>value</th></tr>

<tr><td>
session.getId()
</td><td><%=
session.getId()
%></td></tr>

<tr><td>
request.getParameterNames()
</td><td><%
for (java.util.Enumeration e = request.getParameterNames(); e.hasMoreElements();)
{
  String s = (String)e.nextElement();
  %><%=s%> &nbsp;<%
}
%></td></tr>

<tr><td>
request.getContentType()
</td><td><%=
request.getContentType()
%></td></tr>

<tr><td></td><td></td></tr>

<tr><td>
request.getScheme()
</td><td><%=
request.getScheme()
%></td></tr>

<tr><td>
request.getServerName()
</td><td><%=
request.getServerName()
%></td></tr>

<tr><td>
request.getServerPort()
</td><td><%=
request.getServerPort()
%></td></tr>

<tr><td>
request.getRequestURI()
</td><td><%=
request.getRequestURI()
%></td></tr>

<tr><td>
request.getContextPath()
</td><td><%=
request.getContextPath()
%></td></tr>

<tr><td>
request.getPathInfo()
</td><td><%=
request.getPathInfo()
%></td></tr>

<tr><td>
request.getPathTranslated()
</td><td><%=
request.getPathTranslated()
%></td></tr>

<tr><td></td><td></td></tr>

<tr><td>
request.getProtocol()
</td><td><%=
request.getProtocol()
%></td></tr>

<tr><td>
request.getRemoteAddr()
</td><td><%=
request.getRemoteAddr()
%></td></tr>

<tr><td>
request.getMethod()
</td><td><%=
request.getMethod()
%></td></tr>

<tr><td></td><td></td></tr>

<tr><td>
application.getInitParameter("key")
</td><td><%=
application.getInitParameter("key")
%></td></tr>

<tr><td>
config.getInitParameter ("debug")
</td><td><%=
config.getInitParameter ("debug")
%></td></tr>

</table>
<hr>
<%
  for (int i = 0; i < 10; ++i) {
    %><%=i%><%
    for(int j = 0; j < i; ++j) {
      %>&nbsp;<%
    }
  }
%>
</body></html>
