Ext.define( 'Comete.Viewer', {
    extend: 'Ext.Component',
    initComponent: function( config ) {
        cfg = {
            border: true,
            margin: '0 20 0 20',
            html: ''
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    clear: function() {
        this.update('');
    },
    setContent: function( url, callback ) {
        if( url == null || url == '')
            this.clear();
        else {
            Ext.Ajax.request( {
                url: url,
                method: 'GET',
                success: function(response) {
                    this.update(response.responseText)
                    if( callback != null )
                        callback();
                },
                scope: this 
            } );
        }
    },
    expandContent: function() {
        this.expand();
    },
    isContentCollapsed: function() {
        return( this.collapsed );
    }
} );

