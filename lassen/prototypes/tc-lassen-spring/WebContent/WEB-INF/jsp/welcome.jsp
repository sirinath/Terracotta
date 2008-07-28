<%@ page import="org.terracotta.lassen.util.UrlHelper,
	org.terracotta.lassen.security.StandardAuthoritiesService,
	org.terracotta.lassen.web.registration.*,
	org.terracotta.lassen.web.user.*,
	org.terracotta.lassen.web.passwordreset.*" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Welcome</title>
</head>
<body>

<h1>Commands</h1>
<ul>
	<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, SignupController.class)%>"/>">Register</a></li>
	<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, ListUsersController.class)%>"/>">List users</a></li>
	<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, RequestResetController.class)%>"/>">Reset password</a></li>
	<sec:authorize ifAllGranted="<%=StandardAuthoritiesService.STUDENT_LITERAL%>">
		<li><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, "logout.do")%>"/>">Logout</a></li>
	</sec:authorize>
</ul>

</body>
</html>