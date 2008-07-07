<html>
    <head>
		<title>Users</title>
    </head>
    <body>
		<h1>These are the available users</h1>
    	<ul>
			<@s.iterator value="users"> 
				<li>
					<a href="<@s.url action="editUser!input"><@s.param name="id" value="${id}"/></@s.url>"><@s.property value="username"/> - <@s.property value="email"/></a>
				</li>
			</@s.iterator> 
		</ul>
		<p><a href="<@s.url action="index" includeParams="none"/>">back to index</a></p>
    </body>
</html>