function getPageLocation(lg) {
    return "/Portal/index.jsp?lang=" + lg;         
}

Ext.onReady( function() {

    // By default, the interface is displayed in English.
    var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );

    var submitSearchQuery = function() {
        var query = searchQueryField.getValue();
        if (Ext.String.trim(query) != '') {
            _query = [ { key: "fulltext", value: query } ];
            var url = 'Search.jsp?lang=' + lang + '&' + searchQueryField.getId() + '=' + encodeURIComponent( JSON.stringify( _query ) );
            window.location = url;
        }
    }
    
    var logoPanel = Ext.create('Ext.panel.Panel', {
        layout: {
            type: 'vbox',
            align: 'center',
            pack: 'center'
        },
        height: 200,
        region: 'north',
        border: false,        
        items: [ { xtype: 'image', src: 'images/cometeLogo.gif', width: 108, height: 120 } ]
    } );

    var searchQueryField = new Ext.form.field.Text( {
        id: 'query',
        width: 400,
        height: 28
    } );

    searchQueryField.on( 'specialkey', function( f, e ) {
        if( e.getKey() == e.ENTER ) {
            submitSearchQuery(); 
        }
    } );

    var processQueryButton = new Ext.Button( {
        iconCls: 'x-tbar-search',
        height: 28,
        text: '&nbsp;' + tr('Search'),
        handler: function() { submitSearchQuery(); }
    } );

    var searchForm = Ext.create('Ext.panel.Panel', {
        border: false,
        width: 500,
        height: 28,
        layout: 'column',
        items: [ searchQueryField, 
        {
            width: 2,
            border: false,
            html: '&nbsp;'
        }, processQueryButton ]
    } );

    var searchPanel = {
        id: 'search',        
        region: 'center',
        border: false,
        layout: {
            type: 'vbox',
            align: 'center'
        },
        items: searchForm
    };
   
    var presentationPanel = { 
        id: 'presentation',
        border: false,
        region: 'south',
        html: '<iframe width="100%" height="300" frameborder="0" src="' + lang + '/description.html"></iframe>'
    };

    new Ext.Viewport( {
        layout: 'border',        
        items: {
            layout: 'border',
            border: false,
            region: 'center',            
            items: [ logoPanel, searchPanel, presentationPanel ],
            tbar: tbarNoLogo
        }
    } );

} );
