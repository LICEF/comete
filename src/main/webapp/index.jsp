<%@ page language="java" contentType="text/html; charset=utf-8" %>
<%@ page import="ca.licef.comete.core.Core" %>
<!doctype html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=EDGE" />
    <link rel="shortcut icon" href="images/comete.ico" type="image/x-icon">

    <link rel="stylesheet" type="text/css" href="ext-5.1.0/build/packages/ext-theme-crisp/build/resources/ext-theme-crisp-all.css" />
    <link rel="stylesheet" type="text/css" href="default.css">    

    <script type="text/javascript" src="ext-5.1.0/build/ext-all.js"></script>

    <script type="text/javascript" src="js/i18n.js"></script>
    <% 
        String lang = "en";
        if( request.getParameter( "lang" ) != null && !"en".equals( request.getParameter( "lang" ) ) ) {
            lang = request.getParameter( "lang" );
    %>
        <script type="text/javascript" src="js/i18n_<%= request.getParameter( "lang" ) %>.js"></script>
        <script type="text/javascript" src="ext-5.1.0/build/packages/ext-locale/build/ext-locale-<%= lang %>.js"></script>
    <% } %>

    <title>Comète</title>
    <script type="text/javascript" src="js/utils.js"></script>
    <script type="text/javascript" src="js/Toolbar.js"></script>
    <script type="text/javascript" src="js/learningObjectToHtml.js"></script>
    <script type="text/javascript" src="js/Viewer.js"></script>
    <script type="text/javascript" src="js/LearningObjectTable.js"></script>
    <script type="text/javascript" src="js/LearningObjectManager.js"></script>
    <script type="text/javascript" src="js/SearchSimple.js"></script>
    <script type="text/javascript" src="js/SearchAdvanced.js"></script>
    <script type="text/javascript" src="js/SearchThematic.js"></script>
    <script type="text/javascript" src="js/SearchCollection.js"></script>
    <script type="text/javascript" src="js/Search.js"></script>
    <script type="text/javascript">
        window.cometeUriPrefix = '<%= Core.getInstance().getUriPrefix() %>';
    </script>
</head>
<body>
</body>
</html>
