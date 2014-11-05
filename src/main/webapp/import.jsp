<%@ page language="java" contentType="text/html; charset=utf-8" %>
<!doctype html>
<html>
<head>
    <title>Comète</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

    <link rel="stylesheet" href="ext-5.0.1/build/packages/ext-theme-crisp/build/resources/ext-theme-crisp-all.css"/>

    <script type="text/javascript" src="ext-5.0.1/build/ext-all.js"></script>
    <script type="text/javascript" src="ext-5.0.1/build/packages/ext-theme-crisp/build/ext-theme-crisp.js"></script>

    <script type="text/javascript" src="js/i18n.js"></script>
    <% if( request.getParameter( "lang" ) != null && !"en".equals( request.getParameter( "lang" ) ) ) { %>
        <script type="text/javascript" src="js/i18n_<%= request.getParameter( "lang" ) %>.js"></script>
    <% } %>

    <script type="text/javascript" src="js/util.js"></script>
    <script type="text/javascript" src="js/import.js"></script>

</head>
<body>
</body>
</html>
