<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Password reset</title>
</head>
<body>

<h1>Your password was successfully reset!</h1>

<p>Your new password and your user name have been sent to you through email.</p>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>