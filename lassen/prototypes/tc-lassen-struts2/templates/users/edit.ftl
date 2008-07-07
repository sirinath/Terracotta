<html>
    <head>
		<title>User modification</title>
    </head>
    <body>
		<h1>Edit the user details</h1>
		<@s.form action="editUser" validate="true">
			<@s.hidden name="id"/>
			<@s.textfield name="username" label="Username"/>
			<@s.password name="password" label="Password"/>
			<@s.textfield name="email" label="Email"/>
			<@s.submit/>
		</@s.form>
		<div><a href="<@s.url action="index"/>">Cancel registration</a></div>
    </body>
</html>