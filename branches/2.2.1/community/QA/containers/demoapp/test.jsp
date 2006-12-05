<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

-->

<%
final String action = request.getParameter("action");
final String key    = request.getParameter("key");
if ("invalidate".equals(action)) {
  session.invalidate();
  response.getWriter().print("<a href=\"test.jsp\">Session has been invalidated.  RETURN</a>");
  return;
} 
if ("setmaxidle".equals(action)) {
  int secs = Integer.parseInt(key);
  session.setMaxInactiveInterval(secs);
}
%>
<html>
  <head>
    <title>JSP test</title>
  </head>
  
  <body>
    <table border="2">
      <th colspan="2">Request Info</th>
      <tr><td>Requested Type</td><td><%=request.getClass().getName()%></td></tr>
      <tr><td>getRequestedSessionId</td><td><%=request.getRequestedSessionId()%></td></tr>
      <tr><td>isRequestedSessionIdFromCookie</td><td><%=request.isRequestedSessionIdFromCookie()%></td></tr>
      <tr><td>isRequestedSessionIdFromURL</td><td><%=request.isRequestedSessionIdFromURL()%></td></tr>
      <tr><td>isRequestedSessionIdValid</td><td><%=request.isRequestedSessionIdValid()%></td></tr>
    </table>
    <hr>
    <table border="2">
      <th colspan="2">Session Info</th>
      <tr><td>Type</td><td><%=session.getClass().getName()%></td></tr>
      <tr><td>sessionId</td><td><%=session.getId()%></td></tr>
      <tr><td>creationTime (millis)</td><td><%=session.getCreationTime()%></td></tr>
      <tr><td>creationTime (date)  </td><td><%=new java.util.Date(session.getCreationTime())%></td></tr>

      <tr><td>getLastAccessedTime (millis)</td><td><%=session.getLastAccessedTime()%></td></tr>
      <tr><td>getLastAccessedTime (date)  </td><td><%=new java.util.Date(session.getLastAccessedTime())%></td></tr>

      <tr><td>getMaxInactiveInterval</td><td><%=session.getMaxInactiveInterval()%></td></tr>
      <tr><td>isNew</td><td><%=session.isNew()%></td></tr>
    </table>
    <hr>
    <table border="2">
      <tr><th colspan="3">Attributes</th></tr>
      <tr><th>Name</th> <th>Type</th> <th>Value</th></tr>
      <% 
        java.util.Enumeration names = session.getAttributeNames();
        while ( names.hasMoreElements() )
        { 
          final String name = (String)names.nextElement();
          final Object value = session.getAttribute(name);
          final String type = (value == null) ? "null" : value.getClass().getName();
      %>
      <tr><td><%=name%></td>
          <td><%=type%></td>
          <td><%=value%></td>
      </tr>
      <%
        }
      %>
    </table>
    <hr>
    <a href="test.jsp?action=invalidate">Invalidate Current Session</a>
    <hr>
    <h1> Set Max Inactive Interval</h1>
    <form action="test.jsp" method="POST">
      <input type="text" name="key"/>
      <input type="Submit" name="action" value="setmaxidle"/>
    </form>
  </body>
</html>
