<%@ page import="org.terracotta.lassen.web.WelcomeController" %>
<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Registration</title>
</head>
<body>

<h1>Please provide your personal details</h1>

<form:form modelAttribute="user">
	<table>
		<tr>
			<td>User Name: <form:errors path="userName" cssStyle="color: red" /></td>
			<td><form:input path="userName" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td>Password: <form:errors path="password" cssStyle="color: red" /></td>
			<td><form:password path="password" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td>Email: <form:errors path="email" cssStyle="color: red" /></td>
			<td><form:input path="email" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td>First Name: <form:errors path="firstName" cssStyle="color: red" /></td>
			<td><form:input path="firstName" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td>Last Name: <form:errors path="lastName" cssStyle="color: red" /></td>
			<td><form:input path="lastName" size="30" maxlength="80" /></td>
		</tr>
		<tr>
			<td colspan="2">
				<p class="submit"><input type="submit" value="Register" /></p>
			</td>
		</tr>
	</table>
</form:form>

<p><a href="<c:url value="<%=UrlHelper.getControllerRequestMapping(WelcomeController.class)%>"/>">back to index</a></p>

</body>
</html>