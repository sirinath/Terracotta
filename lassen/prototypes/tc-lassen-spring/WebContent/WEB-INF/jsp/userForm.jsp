<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
<c:choose>
	<c:when test="${0 == user.id}">
		<title>Registration</title>
	</c:when>
	<c:otherwise>
		<title>User modification</title>
	</c:otherwise>
</c:choose>
</head>
<body>

<c:choose>
	<c:when test="${0 == user.id}">
		<h1>Please provide your personal details</h1>
	</c:when>
	<c:otherwise>
		<h1>Edit the user details</h1>
	</c:otherwise>
</c:choose>

<form:form modelAttribute="user">
	<table>
		<tr>
			<th>Username: <form:errors path="username" /> <br />
			<form:input path="username" size="30" maxlength="80" /></th>
		</tr>
		<tr>
			<th>Password: <form:errors path="password" /> <br />
			<form:password path="password" size="30" maxlength="80" /></th>
		</tr>
		<tr>
			<th>Email: <form:errors path="email" /> <br />
			<form:input path="email" size="30" maxlength="80" /></th>
		</tr>
		<tr>
			<td><c:choose>
				<c:when test="${0 == user.id}">
					<p class="submit"><input type="submit" value="Add user" /></p>
				</c:when>
				<c:otherwise>
					<p class="submit"><input type="submit" value="Update user" /></p>
				</c:otherwise>
			</c:choose></td>
		</tr>
	</table>
</form:form>
<p><a href='<c:url value="/welcome.do"/>'>back to index</a></p>

</body>
</html>