<%
  response.addHeader("WWW-Authenticate", "Basic realm=\"BEJY admin\"");
  response.setStatus(401);
%>
