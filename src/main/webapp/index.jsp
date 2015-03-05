<%@ page language="java" contentType="text/html; charset=utf-8" %>
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
        String port = ( request.getServerPort() != 80 ? ":" + request.getServerPort() : "" );
        String webapp = request.getRequestURI().substring( 0, request.getRequestURI().indexOf( "/", 1 ) );
        String portalUrl = request.getScheme() + "://" + request.getServerName() + port + webapp;
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
            var ampersandDelimiter = '<%= '&' %>';
            var lorElement = Ext.get( 'LearningObjectResource' );
            var uri = ( lorElement != null ? lorElement.getAttribute( 'resource' ) : '' );
            var resLink = '<%= portalUrl %>?lang=<%= lang %>' + ampersandDelimiter + 'uri=' + uri; 
            return( resLink );
        }

        function doInitSharingLinks() {
            var titleElement = Ext.get( 'LearningObjectResourceTitle' );
            var title = ( titleElement == null ? null : titleElement.getHtml() );
            // This must be encoded and the quotes must be replaced.  Check xslt transfo. - FB
            //var title = '<xsl:value-of select="replace( escape-html-uri( $title ), '''', '\\''' )"/>';

            var urlFacebook = 'https://www.facebook.com/sharer/sharer.php?u=' + encodeURIComponent( getResourceLink() );
            var urlTwitter = 'https://twitter.com/share?url=' + encodeURIComponent( getResourceLink() );
            var urlLinkedin = 'http://www.linkedin.com/shareArticle?mini=true&amp;url=' + encodeURIComponent( getResourceLink() ) + 
                '&amp;source=Ceres' + 
                '&amp;title=' + encodeURIComponent( title );
            //var urlEmail = 'mailto:?subject=<xsl:value-of select="escape-html-uri( $ShareByEmailSubject )"/><xsl:value-of select="$ampersand" disable-output-escaping="yes"/>body=<xsl:value-of select="escape-html-uri( $ShareByEmailBody )"/>' + encodeURIComponent( getResourceLink() );
            var urlEmail = 'mailto:?subject=TheSubject&amp;body=TheBody' + encodeURIComponent( getResourceLink() );
            
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
    </script>
</head>
<body>
</body>
</html>
