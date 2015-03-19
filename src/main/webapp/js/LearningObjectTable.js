Ext.define( 'Comete.LearningObjectTable', {
    extend: 'Ext.grid.Panel',
    initComponent: function (config) { 

        this.queryUrl = 'rest/queryEngine/searchJson?lang=' + this.lang;

        this.isFirstLoad = true;

        Ext.define('LearninObjectModel', {
            extend: 'Ext.data.Model',
            fields: [ 'id', 'title', 'desc', 'location', 'image', 'loAsHtmlLocation', 'metadataFormat', 'type' ]
        });

        this.proxy = Ext.create('Ext.data.proxy.Ajax', {
            url: this.queryUrl,
            reader: {
                root: 'learningObjects',
                totalProperty: 'totalCount'
            }
        });

        this.loStore = Ext.create('Ext.data.JsonStore', {
            model: 'LearninObjectModel',
            pageSize: 20,
            proxy: this.proxy
        });

        this.pageBar = Ext.create('Ext.toolbar.Paging', {
            store: this.loStore,
            firstText: tr('First Page'),
            prevText: tr('Previous Page'),
            nextText: tr('Next Page'),
            lastText: tr('Last Page'),
            afterPageText: tr('of {0}')
        } ),
        this.pageBar.remove( this.pageBar.getComponent(10) ); //refresh button
        this.pageBar.remove( this.pageBar.getComponent(9) ); //separator

        this.renderImage = function( value ) {
            var res = "";
            if (value != "n/a")
                res = "<img src=\"" + value + "\" height=\"48\" width=\"48\">";
            return res;
        },

        this.renderTitle = function( value, metaData, lo ) {
            var xf = Ext.util.Format;
            descr = xf.ellipsis( xf.stripTags( lo.data.desc ), 200 );
            return Ext.String.format( '<div class="resourceTitle"><b>{0}</b></div><div class="resourceDesc">{1}</div>', 
                       value == 'null' ? '' : value, descr );
        },

        cfg = {
            store: this.loStore,
            cls: 'lo-grid',
            scroll: 'vertical',
            columns: [ 
                { width: 60, dataIndex: 'image', sortable: true, renderer: this.renderImage},
                { text: tr( 'Id' ), width: 100,  dataIndex: 'id', hidden: !this.editable },
                { text: tr( 'Title' ), flex: 1, dataIndex: 'title', sortable: true, renderer: this.renderTitle },
                { text: tr( 'Type' ), width: 80,  dataIndex: 'type', sortable: true, renderer: this.renderType, hidden: !this.editable}
            ],          
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false
            },
            hideHeaders: !this.editable,
            bbar: this.pageBar
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);

        if( this._query )
            this.setRequest(this._query);
    },
    getCurrentPage: function() {
        return this.loStore.currentPage; 
    },    
    getSelected: function() {
        if( this.getSelectionModel().getSelection().length > 0 )
            return this.getSelectionModel().getSelection()[0];
        else
            return null;
    },
    getSelectedId: function() {
        var currSelectedLo = null;
        selected = this.getSelected();
        if (selected)
            currSelectedLo = selected.getData().id;
        return currSelectedLo;
    },
    getSelectedLoHtmlLocation: function() {
        var selectedLo = this.getSelected();
        if (!selectedLo)
            return(null);
        return (selectedLo.getData().loAsHtmlLocation);
    },
    updateResultInfos: function() { 
        var nbResults = this.loStore.getTotalCount();
        var label = tr( 'No resource found' );
        var atLeastOneResult = true;
        if (nbResults == 1)
            label = '1 ' + tr( 'resource found' );
        else if (nbResults > 1)
            label = nbResults + ' ' + tr( 'resources found' );
        else
            atLeastOneResult = false;
         
        this.loManager.setResultText( label );
        this.loManager.setFeedButtonsVisible(atLeastOneResult);
    },
    setRequest: function( query ) {
        var url = this.queryUrl + '&q=' + encodeURIComponent( JSON.stringify(query) );
        var currPage = this.getCurrentPage();
        var currSelectedLo = this.getSelectedId();
        this.getSelectionModel().clearSelections();
        this.proxy.url = url;
        this.loStore.loadPage(1, { 
            callback: function(records, operation) {
                this.loManager.saveQueryHistory( url, query, currPage, currSelectedLo ); 
                this.updateResultInfos();
                var json = Ext.JSON.decode(operation.getResponse().responseText);
                if (json.selectFirstRecord)
                    this.getSelectionModel().select(0);
            },
            scope: this
        } );
    },
    performGoQuery: function(queryHistoryData) {
        displayQuery(queryHistoryData.queryPanelItem, queryHistoryData.query);
        this.proxy.url = queryHistoryData.url;
        var start = 0;
        var limit = this.loStore.pageSize;
        var currPage = 1
        if( queryHistoryData.page ) {
            currPage = queryHistoryData.page;
            start = ( currPage - 1 ) * this.loStore.pageSize;
        }
        this.loStore.loadPage( currPage, { 
            params: { start: start, limit: limit },
            callback: function( records, options, success ) {
                this.updateQuerySelection( queryHistoryData.selectedLo );
                this.updateResultInfos(true);
            },
            scope: this
        } );
    },    
    updateQuerySelection: function( selectedLo ) {
        if( selectedLo ) {
            var selectedRecord = this.loStore.getById( selectedLo );
            if( selectedRecord ) 
                this.getSelectionModel().select( [ selectedRecord ] );
        }
        this.loManager.updateQueryHistoryButtons();
    },
    clear: function() {
        this.loStore.loadRawData([]);
    }
} );


