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
