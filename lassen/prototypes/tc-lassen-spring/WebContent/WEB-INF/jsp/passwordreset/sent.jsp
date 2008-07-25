<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
	<head>
		<title>Reset password</title>
	</head>
<body>

<h1>Password reset instructions sent</h1>

<p>The instructions to reset the password were sent to <%=request.getParameter("email")%>.</p>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>