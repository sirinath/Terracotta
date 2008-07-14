<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Lassen - <decorator:title /></title>
		<link href="<c:url value="/css/lassen.css"/>" rel="stylesheet" type="text/css"/>
        <decorator:head />
    </head>
    <body>
		<decorator:body />
    </body>
</html>