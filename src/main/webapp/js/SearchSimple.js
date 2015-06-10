Ext.define( 'Comete.SimpleSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {

        this.advancedButton = Ext.create('Ext.Component', {            
            html: '<div class="whiteLink"><a href="javascript:changeCardItem(1);">' + tr( 'Advanced Search' ) + '</a></div>'
        } );

        this.thematicButton = Ext.create('Ext.Component', {            
            html: '<div class="whiteLink"><a href="javascript:changeCardItem(2);">' + tr( 'Thematic Navigation' ) + '</a></div>'
        } );

        this.goBackwardQueryButton = Ext.create('Comete.ImageButton', {
            img: 'images/goBackwardQuery.png',
            imgDisabled: 'images/goBackwardQueryDisabled.png',
            width: 20,
            height: 20,
            disabled: true,
            tooltip: tr( 'Go back one query' ),
            handler: this.goBackwardQuery,
            scope: this
        } );

        this.goForwardQueryButton = Ext.create('Comete.ImageButton', {
            img: 'images/goForwardQuery.png',
            imgDisabled: 'images/goForwardQueryDisabled.png',
            width: 20,
            height: 20,
            disabled: true,
            tooltip: tr( 'Go forward one query' ), 
            handler: this.goForwardQuery,
            scope: this
        } );


        this.goBackwardQueryButton.on('render', function() {
            this.goBackwardQueryButton.getEl().setOpacity(0);
        }, this);

        this.goForwardQueryButton.on('render', function() {
            this.goForwardQueryButton.getEl().setOpacity(0);
        }, this);

        this.searchQueryField = Ext.create('Ext.form.field.Text', {
            id: 'query',
            width: CARDPANEL_WIDTH,
            height: 24,
            emptyText: tr('Enter your request here'),
            fieldStyle: { 
                border: 'none',
                fontSize: '14px' 
            }
        } );

        if( this._query )
            this.searchQueryField.setValue( this._query[0].value );

        this.searchQueryField.on( 'specialkey', function( f, e ) {
            if( e.getKey() == e.ENTER ) {
                this.submitSearchQuery(); 
            }
        }, this );
        

        this.searchQueryButton = Ext.create('Comete.ImageButton', {
            img: 'images/search.png',
            width: 24,
            height: 24,
            handler: this.submitSearchQuery,
            scope: this
        } );

        this.simpleSearchBar = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            margin: '15 20 0 20',
            items: [ this.searchQueryField,                      
                     this.searchQueryButton ]
        } );

        this.info = Ext.create('Comete.ImageButton', {
            img: 'images/info.png',
            width: 16,
            height: 16,
            margin: '2 0 0 0',
            tooltip: 'Info', 
            handler: this.info,
            scope: this
        } );

        cfg = {
            layout: 'vbox',
            height: SIMPLE_HEIGHT,
            cls: 'searchPanel',
            items: [ 
                     { layout: 'hbox', cls: 'transp2', border: false, margin: '10 20 0 20', width: CARDPANEL_WIDTH + 24,
                       items: [ { xtype: 'label', text: tr('QUICK SEARCH'), 
                                     style: { fontWeight: 'bold', fontSize: '14px' } }, 
                                { xtype: 'tbspacer', width: 5 }, this.info,
                                { xtype: 'tbfill' }, this.advancedButton, 
                                { xtype: 'tbspacer', width: 15 }, this.thematicButton, { xtype: 'tbspacer', width: 1 } ] },
                     this.simpleSearchBar,
                     { xtype: 'tbfill' },
                     { layout: 'hbox', cls: 'transp2', border: false, margin: '10 20 0 20', width: CARDPANEL_WIDTH + 24, 
                       items: [ { xtype: 'tbfill' }, this.goBackwardQueryButton, { xtype: 'tbspacer', width: 4 }, this.goForwardQueryButton ] },
                     { xtype: 'tbspacer', height: 10 }
                   ]
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

