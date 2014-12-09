Ext.define( 'Comete.CollectionSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {
        
        this.currentCollection = null;

        Ext.define('CollectionModel', {
            extend: 'Ext.data.Model',
            fields: [ 'id', 'label', 'query' ]
        });

        this.collectionProxy = Ext.create('Ext.data.proxy.Ajax', {
            url: '/rest/queryEngine/collections',
            reader: {
                type: 'json',
                root: 'collections'
            }
        });

        this.collectionStore = Ext.create('Ext.data.JsonStore', {
            model: 'CollectionModel',
            proxy: this.collectionProxy
        });

        this.collectionCombo = Ext.create('Ext.form.field.ComboBox', {
            editable: false,
            width: 400,
            displayField: 'label',
            emptyText: tr('Select collection') + '...',
            listConfig: {
                loadingText: tr('Loading') + '...'
            },
            store: this.collectionStore
        });

        this.collectionCombo.on( 'select', this.collectionSelected, this );

        this.collectionQueryButton = Ext.create('Ext.button.Button', {
            text: tr('Search'),
            disabled: true,
            handler: this.submitCollectionQuery,
            scope: this
        } );

        var collectionPanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',         
            border: false,
            margin: '0 0 6 0',
            items: [ this.collectionCombo,
                     { xtype: 'tbspacer', width: 4 }, 
                     this.collectionQueryButton ]
        } );

        cfg = {
            layout: 'vbox',
            region: 'center',      
            margin: '0 10 0 10',
            items: collectionPanel
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);         
    },
    collectionSelected: function(combo, records) {
        this.currentCollection = records[0].getData();
        this.collectionQueryButton.setDisabled(false);
    },
    submitCollectionQuery: function() {
        searchManager.setRequestCollection( this.currentCollection.id, eval(this.currentCollection.query) ); 
    },
    setQuery: function(query) {
        record = this.collectionCombo.findRecord("query", JSON.stringify(query));
        this.collectionCombo.setValue(record.data.label);
    },
    init: function() {
        this.collectionStore.load();
    }
} );

