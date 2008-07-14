<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
<title>Users</title>
</head>
<body>

<h1>These are the available users</h1>
<ul>
	<c:forEach var="user" items="${users}">
		<li><a
			href='<c:url value="editUser.do"><c:param name="id" value="${user.id}"/></c:url>'>${user.username}
		- ${user.email}</a></li>
	</c:forEach>
</ul>
<p><a href='<c:url value="/welcome.do"/>'>back to index</a></p>

</body>
</html>