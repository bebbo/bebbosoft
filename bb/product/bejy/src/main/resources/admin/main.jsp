<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<%@ page import="de.bb.bejy.*" %>
<%@ page import="java.util.*" errorPage="/" %>
<html>
<head>
<link href="default.css" type="text/css" rel="stylesheet">
<title>BEJY config page</title>
</head>
<%
  Configurator selCt = null;
  Configurable selectedConfigurable = null;

  // read parameters
  String td = request.getParameter("td");
  if (td == null) td = "";
  String sItem = request.getParameter("item");
  String itemName = request.getParameter("itemName");
  String action = request.getParameter(itemName != null ? itemName : "null");
  String gAction = request.getParameter("global$Action");
  String param = null;
  if ("save".equals(gAction))
  {
    Config.save();
  } else
  if ("revert".equals(gAction))
  {
    param = "?";
    Config.revert();
  } else
  if ("shutdown".equals(gAction))
  {
    param = "?";
    Config.shutdown();
  } else
  if ("refresh".equals(gAction))
  {
    param = "?";
  } else
  if ("run mail wizard".equals(gAction))
  {%>
<script>
this.location="w_mail.jsp";
</script>
<%
    return;  
  }

  int item = -1;
  try { item = Integer.parseInt(sItem); } catch (Exception e) {}
%>

<body style="background-color:#FeeBa1;">
<div style="border:solid;border-width:1;border-color:#875;background-color:#ED9;float:left">
<form action="main.jsp#<%=item%>" method="POST">
<input type="hidden" name="item" value="<%=item%>">
<%
  Config cfg = Config.getInstance();
  LinkedList ll = new LinkedList();
  ll.add(cfg);
  Iterator i = ll.iterator(); //cfg.children();

  Stack iters = new Stack();
  int indent = 0;

  for(int n = 0;;++n)
  {
    if (!i.hasNext())
    {
      if (iters.size() == 0)
      {
        break;
      }
      i = (Iterator)iters.pop();
      --indent;
      --n; // fix counter
      continue;
    }

    Configurable child = (Configurable)i.next();
    Configurator ct = child.getConfigurator();

    if (n == item) {
      selCt = ct;
      selectedConfigurable = child;

      if (action != null && itemName.equals("" + selectedConfigurable.getName()))
      {
        try {
          if ("remove this element".equals(action))
          {
            Configurable parent = selectedConfigurable.getParent();
            if (parent.remove(selectedConfigurable))
            {
              action = null;
              --n;
              selectedConfigurable = null;
              selCt = null;
              parent.update(Config.getLogFile());
              if (!i.hasNext())
                  sItem = Integer.toString(item - 1);
              param = "?item=item=" + sItem;
              break;
            }
          } else
          if ("\\/".equals(action))
          {
            Configurable parent = selectedConfigurable.getParent();
            if (parent.remove(selectedConfigurable)) {
              parent.addChild(selCt.getPath(), selectedConfigurable);
              selectedConfigurable.update(Config.getLogFile());
              --n;
              param = "?item=item=" + sItem;
              break;
            }
          } else
          if ("apply changes".equals(action))
          {
            param = "?item=item=" + sItem;
            for (Iterator pi = selectedConfigurable.propertyNames(); pi.hasNext();)
            {
              String pName = (String)pi.next();
              String val = request.getParameter(pName);

              // special h%andling for class stuff
              if ("class".equals(pName) && !val.equals(selectedConfigurable.getProperty("class")))
              {
                System.out.println(val);
                Class clazz = Class.forName(val);
                Configurable parent = selectedConfigurable.getParent();
                parent.remove(selectedConfigurable);

                child = selectedConfigurable = (Configurable)clazz.newInstance();
                parent.addChild(ct.getPath(), selectedConfigurable);
                selectedConfigurable.setConfigurator(ct);
                selectedConfigurable.setProperty("class", val);
              } else
              // handle passwords different!
              if (!"password".equals(pName))
              {
                selectedConfigurable.setProperty(pName, val);
              } else {
                String val2 = request.getParameter(pName + "2");
                if (val != null && val.length() > 0 && val.equals(val2))
                {
                  selectedConfigurable.setProperty(pName, val);
                } else { %><b>password NOT CHANGED!!!</b>
                <%}
              }
            }
            selectedConfigurable.update(Config.getLogFile());
          } else
          if (action.startsWith("new ")) {
            param = "?item=item=" + sItem;
            String name = action.substring(4);
            for (Iterator k = cfg.configurators(); k.hasNext();) {
              Configurator c = (Configurator)k.next();
              if (c.getName().equals(name)) {
                if (c.loadClass()) {
                  Configurable conf = new ByClass();
                  conf.setConfigurator(c);
                  selectedConfigurable.addChild(c.getPath(), conf);
                } else {
                  Configurable conf = c.create();
                  conf.setConfigurator(c);
                  selectedConfigurable.addChild(c.getPath(), conf);
                }
              }
            }
            selectedConfigurable.update(Config.getLogFile());
            break;
          }
        } catch (Throwable t){}
      }
      action = null;
    }
  %>

  <%
    Iterator j = child.children();
    if (j.hasNext()) {
      ++indent;
      iters.push(i);
      i = j;
    }
  }
  if (param != null) {
%>
<script>
parent.navi.location= "navi.jsp" + "<%=param%>";
</script>
<%
    return;
  }

  if (selCt != null)
  {
%>
    <input type="hidden" name="itemName" value="<%=selectedConfigurable.getName()%>">
<table>
<%
    if (selectedConfigurable.propertyNames().hasNext())
    {
  %>
  <tr style="border:solid"><th>name</th><th>value</th><th>description</th></tr>
  <%
      for (Iterator k = selectedConfigurable.propertyNames(); k.hasNext();)
      {
        String pn = (String)k.next();
        String pv = selectedConfigurable.getProperty(pn);
        if (pv == null) pv = "";
        String pi = selectedConfigurable.getPropertyInfo(pn);
        List clist = selectedConfigurable.getPropertyClassNames(pn);

        %><tr><td><%=pn%></td><td><%
        if (pn.equals("class"))
        {%> <select name="class"> <%
          for (Iterator l = Config.getClassNames(selectedConfigurable.getConfigurator()); l.hasNext();)
          {
            String cn = (String)l.next();
            %> <option value="<%=cn%>" <%=
            cn.equals(pv) ? "selected" : ""
            %>><%=cn%></option><%
          }
         %></select><%
        } else
        if (clist != null)
        {%> <select name="<%=pn%>"> <%
          for (Iterator l = clist.iterator(); l.hasNext();)
          {
            String cn = (String)l.next();
            %> <option value="<%=cn%>" <%=
            cn.equals(pv) ? "selected" : ""
            %> > <%=cn%></option><%
          }
         %></select><%
        } else
        if (pn.equals("password"))
        {%>
          <input type="password" name="<%=pn%>" value="">
          <input type="password" name="<%=pn%>2" value="">
        <% } else {%>
        <input type="text" name="<%=pn%>" value="<%=pv%>">
        <% }
        %></td><td><%=pi%></td></tr><%
      }
      %>
      <tr><td>&nbsp;</td><td>
      <input type="submit" name="<%=selectedConfigurable.getName()%>" value="apply changes">
      </td><td>
      <%
      if (selectedConfigurable.getParent() != null && selectedConfigurable.getParent().mayRemove(selectedConfigurable))
      {%>
        <input type="submit" name="<%=selectedConfigurable.getName()%>" value="\/">
        <input type="submit" name="<%=selectedConfigurable.getName()%>" value="remove this element" onclick=
        "return confirm('really remove <%=selectedConfigurable.getName()%>???_');">
        <%
      } %>
      </td></tr>
      <tr><td colspan="3"><hr></td></tr>
      <%
    }
    String id = selectedConfigurable.getId();
    if (id == null)
      id = selCt.getId();
    for (Iterator k = cfg.configurators(); k.hasNext();)
    {
      Configurator c = (Configurator)k.next();
      String eid = c.getExtensionId();
      if (eid != null)
      for (StringTokenizer st = new StringTokenizer(eid, " ,"); st.hasMoreTokens();)
      {
        if (id.equals(st.nextToken()) && selectedConfigurable.acceptNewChild(c) ) {
          %><tr><td><input type="submit" name="<%=selectedConfigurable.getName()%>" value="new <%=c.getName()%>"></td>
          <td colspan="2">creates a new <%=c.getName()%>, <%=c.getDescription()%></td>
          </tr><%
          break;
        }
      }
    }
%>
</table>
<%      
  }
%>
</form>
</div>
</body>
</html>
