<%@ page import="org.terracotta.lassen.util.UrlHelper,org.terracotta.lassen.web.registration.SignupController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
<title>Welcome</title>
</head>
<body>

<h1>Commands</h1>
<ul>
	<li><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(SignupController.class)%>"/>">Register</a></li>
</ul>

</body>
</html>