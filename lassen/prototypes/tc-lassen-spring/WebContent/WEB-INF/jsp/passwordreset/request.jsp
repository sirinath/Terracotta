<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
	<head>
		<title>Reset password</title>
	</head>
<body>

<h1>Reset your password</h1>

<p>Please fill in your email address.</p>
<p>You will receive an email that contains the instructions of how to reset your password.</p>

<c:if test="${userNotFound}">
	<p style="color: red">No user was found with this email address.</p>
</c:if>

<form method="post">
	<table>
		<tr>
			<td>Email:</td>
			<td><input type="text" name="email" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td colspan="2">
				<p class="submit"><input type="submit" value="Request password reset" /></p>
			</td>
		</tr>
	</table>
</form>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>