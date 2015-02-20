<%@ page language="java" contentType="text/html; charset=utf-8" %>
<!doctype html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link rel="shortcut icon" href="images/comete.ico" type="image/x-icon">

    <link rel="stylesheet" type="text/css" href="ext-5.0.1/build/packages/ext-theme-crisp/build/resources/ext-theme-crisp-all.css" />
    <link rel="stylesheet" type="text/css" href="default.css">    

    <script type="text/javascript" src="ext-5.0.1/build/ext-all.js"></script>

    <script type="text/javascript" src="js/i18n.js"></script>
    <% if( request.getParameter( "lang" ) != null && !"en".equals( request.getParameter( "lang" ) ) ) { %>
        <script type="text/javascript" src="js/i18n_<%= request.getParameter( "lang" ) %>.js"></script>
    <% } %>

    <title>Comète Administration</title>
    <script type="text/javascript" src="js/utils.js"></script>
    <script type="text/javascript" src="js/Toolbar.js"></script>
    <script type="text/javascript" src="js/AdminImport.js"></script>
    <script type="text/javascript" src="js/AdminHarvest.js"></script>
    <script type="text/javascript" src="js/AdminIdentityUtils.js"></script>
    <script type="text/javascript" src="js/AdminIdentityEdition.js"></script>
    <script type="text/javascript" src="js/AdminIdentity.js"></script>
    <script type="text/javascript" src="js/AdminVoc.js"></script>
    <script type="text/javascript" src="js/AdminRecordValidation.js"></script>
    <script type="text/javascript" src="js/AdminBrokenLinkManager.js"></script>
    <script type="text/javascript" src="js/AdminOther.js"></script>
    <script type="text/javascript" src="js/Admin.js"></script>

</head>
<body>
</body>
</html>
