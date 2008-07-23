<%@ include file="/WEB-INF/jsp/includes.jsp"%>
<html>
<head>
	<title>Registration Confirmation</title>
</head>
<body>

<h1>Please provide the following information to finalize your registration</h1>

<c:if test="${invalid}">
	<h2>Invalid confirmation data.</h2>
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
				<p class="submit"><input type="submit" value="Finalize registration" /></p>
			</td>
		</tr>
	</table>
</form>

<p><a href='<c:url value="/welcome.do"/>'>back to index</a></p>

</body>
</html>