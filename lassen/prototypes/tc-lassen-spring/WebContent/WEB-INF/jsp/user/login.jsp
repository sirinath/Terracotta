<%@ page import="org.terracotta.lassen.web.WelcomeController,
	org.terracotta.lassen.web.registration.*,
	org.terracotta.lassen.web.user.*,
	org.terracotta.lassen.web.passwordreset.*" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Login</title>
</head>
<body>

<h1>Login with user name and password</h1>
<c:if test="${loginError}">
	<p style="color: red">
		Your login attempt was not successful, try again.<br/>
		<c:if test="${errorMessage != null}">
			<br />
			<c:out value="${errorMessage}"/>
		</c:if>
	</p>
</c:if>
<form action="j_spring_security_check" method="POST">
	<table>
		<tr><td>User Name:</td><td><input type="text" name="j_username" value="${lastUser}"></td></tr>
		<tr><td>Password:</td><td><input type="password" name="j_password"/></td></tr>
		<tr><td><input type="checkbox" name="_spring_security_remember_me"/></td><td>Remember me on this computer.</td></tr>
		<tr><td colspan="2"><a href="<c:url value="<%=UrlHelper.createAbsolutePrettyUrl(request, RequestResetController.class)%>"/>">Forgot password?</a></td></tr>
		<tr><td colspan="2"><input name="submit" type="submit"/></td></tr>
		<tr><td colspan="2"><input name="reset" type="reset"/></td></tr>
	</table>
</form>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>