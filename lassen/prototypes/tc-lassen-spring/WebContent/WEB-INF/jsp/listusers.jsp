<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
	<head>
		<title>Users</title>
	</head>
<body>

<h1>These are the available users</h1>
<ul>
	<c:forEach var="user" items="${users}">
		<li>${user.userName} - ${user.email}</li>
	</c:forEach>
</ul>
<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>