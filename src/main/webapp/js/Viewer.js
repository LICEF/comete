Ext.define( 'Comete.Viewer', {
    extend: 'Ext.panel.Panel',
    initComponent: function( config ) {
        this.htmlTemplate = '<iframe width="100%" height="100%" src="{0}" id="CometeViewer" frameborder="0"></iframe>';

        cfg = {
            html: Ext.String.format( this.htmlTemplate, '' )
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    setContent: function( url ) {
        if( url == null || url == '' ) {
            this.body.update( "" );
        }
        else {
            var html = Ext.String.format( this.htmlTemplate, url );
            this.body.update( html );
        }
    },
    expandContent: function() {
        this.expand();
    },
    isContentCollapsed: function() {
        return( this.collapsed );
    }
} );

