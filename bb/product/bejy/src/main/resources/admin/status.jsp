<%@ page import="de.bb.bejy.*" %>
<%@ page import="java.util.*" errorPage="/" %>
<html>
<head><link href="default.css" type="text/css" rel="stylesheet"></head>
<body style="background-color:#FeeBa1;">
  <h1>BEJY - Status</h1>
  <form action="status.jsp" method="POST" >
  <h2>Servers</h2>
    <table border="1" width="100%">
      <tr><th>name</th><th>port</th><th>protocol</th><th>threads</th></tr>
<%
  try {
   Config cfg = Config.getInstance();
   for (Iterator i = cfg.children();i.hasNext();) {
     Object o = i.next();
     if (!(o instanceof Server))
       continue;

     Server s = (Server)o;
     Configurable c = ((Configurable)s.getChild("protocol"));
     %><tr><td><%=s.getName()%></td>
           <td align=right><%=s.getPort()%></td>
           <td><%=c!=null?c.getName():"?"%></td>
           <td><%=c!=null?""+s.getThreadCount():"?"%></td>
     </tr><%
   }
   } catch (Throwable t) {
     t.printStackTrace();
   }
%>
  </table>

  <h2>Scheduler</h2>
    <table border="1" width="100%">
      <tr><th>name</th><th>state</th><th>intervall [s]</th><th>next start</th></tr>
<%
   Cron cron = Config.getCron();
   for (Iterator i = cron.cronJobs(); i.hasNext();) {
     CronJob cj = (CronJob)i.next();
     %><tr><td><%=cj.getName()%></td>
           <td><%=cj.isActive() ? "running" : "waiting"%></td>
           <td align=right><%=cj.getIntervall()/1000%></td>
           <td align=right><%=new Date(cj.nextLaunch())%></td>
     </tr><%
   }
%>
    </table>

  <h2>Running Schedulers</h2>
    <table border="1" width="100%">
      <tr><th>name</th><th>start time</th></tr>
<%
   for (Iterator i = cron.runningJobs(); i.hasNext();) {
     CronThread ct = (CronThread)i.next();
     %><tr><td><%=ct.getName()%></td>
           <td align=right><%=new Date(ct.getStartTime())%></td>
     </tr><%
   }
%>
    </table>
  </form>
</body></html>


