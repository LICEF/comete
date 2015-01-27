Ext.define( 'Comete.Viewer', {
    extend: 'Ext.Component',
    initComponent: function( config ) {
        cfg = {
            border: true,
            style: { paddingLeft: '20px' },
            html: ''
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    setContent: function( url ) {
        if( url == null || url == '')
            this.update( '' );
        else {
            Ext.Ajax.request( {
                url: url,
                method: 'GET',
                success: function(response) {
                    this.update(response.responseText)
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

