<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="ca.licef.comete.core.Core,ca.licef.comete.core.FedoraService" %>

<%

System.out.println( "1" );    
    Core core = Core.getInstance();
System.out.println( "core="+core );    
    FedoraService fedoraService = core.getFedoraService();
System.out.println( "fedoraService="+fedoraService );    
    fedoraService.createDigitalObject( "/fredobj9" );
System.out.println( "obj9 created." );    
%>
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>Comète</title>
</head>
<body>
Test FedoraService
</body>
</html>

