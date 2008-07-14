<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
<title>Welcome</title>
</head>
<body>

<h1>Commands</h1>
<ul>
	<li><a href='<c:url value="/register.do"/>'>Register</a></li>
	<li><a href='<c:url value="/listUsers.do"/>'>ListUsers</a></li>
</ul>

</body>
</html>