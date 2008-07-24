<%@ page import="org.terracotta.lassen.util.UrlHelper,org.terracotta.lassen.web.registration.SignupController,org.terracotta.lassen.web.ListUsersController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
<title>Welcome</title>
</head>
<body>

<h1>Commands</h1>
<ul>
	<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, SignupController.class)%>"/>">Register</a></li>
	<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, ListUsersController.class)%>"/>">List Users</a></li>
	<sec:authorize ifAllGranted="STUDENT">
		<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, "logout.do")%>"/>">Logout</a></li>
	</sec:authorize>
</ul>

</body>
</html>