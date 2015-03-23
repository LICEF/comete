var identityWindowIndex = 1;

function showAdditionalIdentityInfo( url, left, top, width, height, style ) {
    if( 'endrea' == style ) { 
        $.fancybox({
            type: 'iframe',
            width: 400,
            height:  400,
            padding: 2,
            autoSize: false,
            modal: true,
            href: url
        });
    }
    else {
        window.parent.showIdentity( "CometeIdentity_" + identityWindowIndex, url, left, top, width, height );
        identityWindowIndex++;
    }
}

function setRequestContributor( contribId, label ) {
    if( Ext.isIE )
        label = decodeUtf8( label );
    if( window.parent.searchManager ) {
        window.parent.searchManager.closeDialog();
        window.parent.searchManager.setRequestContributor( contribId, label );
    }
}

function setRequestOrganization( orgId, label ) {
    if( Ext.isIE )
        label = decodeUtf8( label );
    if( window.parent.searchManager ) {
        window.parent.searchManager.closeDialog();
        window.parent.searchManager.setRequestOrganization( orgId, label );
    }
}

function setRequestVocConcept( conceptUri ) {
    if( window.parent.searchManager ) {
        window.parent.searchManager.closeDialog();
        window.parent.searchManager.setRequestVocConcept( conceptUri );
    }
}

function setRequestKeyword( keyword, lang ) {
    if( Ext.isIE )
        keyword = decodeUtf8( keyword );
    if( window.parent.searchManager ) {
        window.parent.searchManager.closeDialog();
        window.parent.searchManager.setRequestKeyword( keyword, lang );
    }
}


/*******************/
/* Social networks */
/*******************/

function getResourceLink() {
    var lorElement = Ext.get( 'LearningObjectResource' );
    if( lorElement == null )
        return( null );
    var loUri = lorElement.getAttribute( 'resource' );
    var loUuid = loUri.substring( loUri.lastIndexOf( '/' ) + 1 );
    var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
    var pathname = '?lang='+lang+'&lo-uuid=' + loUuid;
    var baseUrl = location.protocol + '//' + location.hostname + ( location.port && ':' + location.port ) + 
        location.pathname.substring( 0, location.pathname.indexOf( '/', 1 ) );
    return( baseUrl + pathname );
}

function doInitSharingLinks() {
    var titleElement = Ext.get( 'LearningObjectResourceTitle' );
    var title = ( titleElement == null ? null : titleElement.getHtml() );
            
    var resLink = encodeURIComponent( getResourceLink() );

    var urlFacebook = 'https://www.facebook.com/sharer/sharer.php?u=' + resLink;
    var urlTwitter = 'https://twitter.com/share?url=' + resLink;
    var urlLinkedin = 'http://www.linkedin.com/shareArticle?mini=true&url=' + resLink + 
        '&source=Comete' + 
        '&title=' + encodeURIComponent( title );
    var urlEmail = 'mailto:?subject=' + tr( 'Check this out!' ) + '&body=' + tr( 'I think that this could interest you: ' ) + resLink;
           
    var shareOnFacebookElement = Ext.get( 'ShareOnFacebookLink' );
    if( shareOnFacebookElement != null )
        shareOnFacebookElement.set( { href: urlFacebook } );

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

