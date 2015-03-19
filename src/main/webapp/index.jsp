<%@ page language="java" contentType="text/html; charset=utf-8" %>
<%@ page import="ca.licef.comete.core.Core" %>
<!doctype html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=EDGE" />
    <link rel="shortcut icon" href="images/comete.ico" type="image/x-icon">

    <link rel="stylesheet" type="text/css" href="ext-5.0.1/build/packages/ext-theme-crisp/build/resources/ext-theme-crisp-all.css" />
    <link rel="stylesheet" type="text/css" href="default.css">    

    <script type="text/javascript" src="ext-5.0.1/build/ext-all.js"></script>

    <script type="text/javascript" src="js/i18n.js"></script>
    <% 
        String lang = "en";
        if( request.getParameter( "lang" ) != null && !"en".equals( request.getParameter( "lang" ) ) ) {
            lang = request.getParameter( "lang" );
    %>
        <script type="text/javascript" src="js/i18n_<%= request.getParameter( "lang" ) %>.js"></script>
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
        function getResourceLink() {
            var lorElement = Ext.get( 'LearningObjectResource' );
            if( lorElement == null )
                return( null );
            var loUri = lorElement.getAttribute( 'resource' );
            var loUuid = loUri.substring( loUri.lastIndexOf( '/' ) + 1 );
            var pathname = '?lang=<%= lang %>&lo-uuid=' + loUuid;
            var baseUrl = location.protocol + '//' + location.hostname + ( location.port && ':' + location.port ) + 
                location.pathname.substring( 0, location.pathname.indexOf( '/', 1 ) );
            return( baseUrl + pathname );
        }

        function doInitSharingLinks() {
            var titleElement = Ext.get( 'LearningObjectResourceTitle' );
            var title = ( titleElement == null ? null : titleElement.getHtml() );
            // This must be encoded and the quotes must be replaced.  Check xslt transfo. - FB
            //var title = '<xsl:value-of select="replace( escape-html-uri( $title ), '''', '\\''' )"/>';
            
            var resLink = encodeURIComponent( getResourceLink() );

            var urlFacebook = 'https://www.facebook.com/sharer/sharer.php?u=' + resLink;
            var urlTwitter = 'https://twitter.com/share?url=' + resLink;
            var urlLinkedin = 'http://www.linkedin.com/shareArticle?mini=true&amp;url=' + resLink + 
                '&amp;source=Ceres' + 
                '&amp;title=' + encodeURIComponent( title );
            //var urlEmail = 'mailto:?subject=<xsl:value-of select="escape-html-uri( $ShareByEmailSubject )"/><xsl:value-of select="$ampersand" disable-output-escaping="yes"/>body=<xsl:value-of select="escape-html-uri( $ShareByEmailBody )"/>' + encodeURIComponent( getResourceLink() );
            var urlEmail = 'mailto:?subject=TheSubject&amp;body=TheBody' + resLink;
            
            var shareOnFacebookElement = Ext.get( 'ShareOnFacebookLink' );
            //alert( 'shareOnFacebookElement='+shareOnFacebookElement );
            if( shareOnFacebookElement != null )
                shareOnFacebookElement.set( { href: urlFacebook } );
            //alert('urlFacebook='+urlFacebook);

            var shareOnTwitterElement = Ext.get( 'ShareOnTwitter' );
            if( shareOnTwitterElement != null )
                shareOnTwitterElement.set( { href: urlTwitter } );

            var shareOnLinkedinElement = Ext.get( 'ShareOnLinkedin' );
            if( shareOnLinkedinElement != null )
                shareOnLinkedinElement.set( { href: urlLinkedin } );

            var shareByEmailElement = Ext.get( 'ShareByEmail' );
            if( shareByEmailElement != null )
                shareByEmailElement.set( { href: urlEmail } );
        }

        function initSharingLinks() {
            // Sleep a bit To allow the page to finish loading.
            setTimeout( doInitSharingLinks, 500 );
        }

        Window.cometeUriPrefix = '<%= Core.getInstance().getUriPrefix() %>';
    </script>
</head>
<body>
</body>
</html>
