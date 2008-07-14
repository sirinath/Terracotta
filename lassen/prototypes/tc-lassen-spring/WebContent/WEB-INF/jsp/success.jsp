<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
<title>Registration successful</title>
</head>
<body>

<h1>Welcome ${user.username}, your registration was successful!</h1>
<p><a href='<c:url value="/welcome.do"/>'>back to index</a></p>

</body>
</html>