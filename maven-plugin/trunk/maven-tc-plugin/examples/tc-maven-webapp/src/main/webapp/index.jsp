<html>
<body>
<%
  Integer counter = (Integer) session.getAttribute("counter");
  if(counter==null) {
    counter = new Integer(0);
  } else {
    counter = new Integer(counter.intValue()+1);
  }
  session.setAttribute("counter", counter);

  String requestUrl = request.getRequestURL().toString();
  java.net.URL url1 = new java.net.URL(requestUrl);
  int port = url1.getPort()==8080 ? 8081 : 8080;
  java.net.URL url2 = new java.net.URL(url1.getProtocol(), url1.getHost(), port, url1.getFile());
%>

Request URL: <%= request.getRequestURL() %> <a href="<%= response.encodeURL(url2.toString()) %>"><%= port %></a><br/>
Cookies:<br/>
<%
  javax.servlet.http.Cookie[] cookies = request.getCookies();
  for(int i = 0; i<cookies.length; i++) {
%>
&nbsp; <%= cookies[i].getName() %> : <%= cookies[i].getPath() %> : <%= cookies[i].getValue() %><br/>
<% 
  }
%>

Session id: <%= session.getId() %><br/>
<% 
  for(java.util.Enumeration en = session.getAttributeNames(); en.hasMoreElements(); ) { 
  String name = (String) en.nextElement();
%>
&nbsp; <%= name %> : <%= session.getAttribute(name) %><br/>
<%
  } 
%>

</body>
</html>
