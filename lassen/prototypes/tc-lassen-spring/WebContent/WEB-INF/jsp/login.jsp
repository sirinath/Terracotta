<%@ page import="org.terracotta.lassen.util.UrlHelper,
	org.terracotta.lassen.security.StandardAuthorities,
	org.terracotta.lassen.web.registration.SignupController,
	org.terracotta.lassen.web.ListUsersController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Login</title>
</head>
<body>

<h1>Login with user name and password</h1>
<c:if test="${loginError}">
	<p>
		<font color="red">
		Your login attempt was not successful, try again.<br/>
		<c:if test="${errorMessage != null}">
			<br />
			<c:out value="${errorMessage}"/>
		</c:if>
		</font>
	</p>
</c:if>
<form action="j_spring_security_check" method="POST">
	<table>
		<tr><td>User Name:</td><td><input type="text" name="j_username" value="${lastUser}"></td></tr>
		<tr><td>Password:</td><td><input type="password" name="j_password"/></td></tr>
		<tr><td><input type="checkbox" name="_spring_security_remember_me"/></td><td>Remember me on this computer.</td></tr>
		<tr><td colspan="2"><input name="submit" type="submit"/></td></tr>
		
		<tr><td colspan="2"><input name="reset" type="reset"/></td></tr>
</table>
</form>

</body>
</html>