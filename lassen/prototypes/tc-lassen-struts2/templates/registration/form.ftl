<html>
    <head>
		<title>Registration</title>
    </head>
    <body>
		<h1>Please provide your personal details</h1>
		<@s.form action="register" validate="true">
			<@s.textfield name="username" label="Username"/>
			<@s.password name="password" label="Password"/>
			<@s.textfield name="email" label="Email"/>
			<@s.submit/>
		</@s.form>
		<div><a href="<@s.url action="index"/>">Cancel registration</a></div>
    </body>
</html>