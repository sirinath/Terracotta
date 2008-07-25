<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Password Reset Confirmation</title>
</head>
<body>

<h1>Please provide the following information to reset your password</h1>

<c:if test="${invalid}">
	<p style="color: red">Invalid confirmation data.</p>
</c:if>

<form method="post">
	<table>
		<tr>
			<td>Registration Email:</td>
			<td><input type="text" name="email" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td>Confirmation Code:</td>
			<td><input type="text" name="code" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td colspan="2">
				<p class="submit"><input type="submit" value="Reset your password" /></p>
			</td>
		</tr>
	</table>
</form>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>