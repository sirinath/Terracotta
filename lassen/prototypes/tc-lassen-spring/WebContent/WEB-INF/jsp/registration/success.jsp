<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Registration - Confirmation</title>
</head>
<body>

<h1>Welcome ${user.userName}, your registration was successful!</h1>
<p>An email has been sent to your email address at ${user.email} with a confirmation code.</p>
<p>Please use this code to finalize your registration. Until then, your account will be disabled.</p>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>