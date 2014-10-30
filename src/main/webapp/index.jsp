<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%
    String protocol = request.getScheme();
    String server = request.getServerName();
    int port = request.getServerPort();
    String webapp = request.getContextPath();
    String portalUrl = protocol + "://" + server + ( port == 80 ? "" : ":" + port ) + webapp;
%>
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>Comète</title>
</head>
<body>
Welcome to Comète
</body>
</html>
