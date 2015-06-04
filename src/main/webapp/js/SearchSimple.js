Ext.define( 'Comete.SimpleSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {

        this.searchQueryField = Ext.create('Ext.form.field.Text', {
            id: 'query',
            width: 400,
            height: 28,
            style: {  marginLeft: '10px' }
        } );

        if( this._query && this._query[0].value.indexOf( "http" ) != 0 ) 
            this.searchQueryField.setValue( this._query[0].value );

        this.searchQueryField.on( 'specialkey', function( f, e ) {
            if( e.getKey() == e.ENTER ) {
                this.submitSearchQuery(); 
            }
        }, this );

        this.searchQueryButton = Ext.create('Ext.button.Button', {
            height: 28,
            text: tr('Search'),
            handler: this.submitSearchQuery,
            scope: this
        } );

        this.simpleSearchBar = Ext.create('Ext.panel.Panel', {
            region: 'center',
            //height: 24,        
            layout: 'hbox',
            border: false,
            items: [ this.searchQueryField, 
                     { xtype: 'tbspacer', width: 4 }, 
                     this.searchQueryButton ]
        } );

        cfg = {
            layout: 'border',
            region: 'center',      
            items: this.simpleSearchBar
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    submitSearchQuery: function() {
        var textQuery = this.searchQueryField.getValue();
        searchManager.setRequestSimpleSearch( textQuery ); 
    },
    setQuery: function(query) {
        this.searchQueryField.setValue( query == null ? '' : query[0].value );
    },
    redoRequest: function() {
        this.submitSearchQuery();
    } 
} );

