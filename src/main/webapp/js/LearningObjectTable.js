Ext.define( 'Comete.LearningObjectTable', {
    extend: 'Ext.grid.Panel',
    selModel: {
        type: 'rowmodel',
        mode: 'MULTI'
        //pruneRemoved: false
    },
    initComponent: function (config) { 

        this.queryUrl = 'rest/queryEngine/searchJson?lang=' + this.lang;

        this.isFirstLoad = true;

        Ext.define('LearningObjectModel', {
            extend: 'Ext.data.Model',
            fields: [ 'id', 'title', 'desc', 'location', 'image', 'loAsHtmlLocation', 'metadataFormat', 'type', 'hidden', 'pending', 'inactive', 'invalid', 'brokenLink'  ]
        });

        this.proxy = Ext.create('Ext.data.proxy.Ajax', {
            url: this.queryUrl,
            reader: {
                rootProperty: 'learningObjects',
                totalProperty: 'totalCount'
            }
        });

        this.loStore = Ext.create('Ext.data.JsonStore', {
            model: 'LearningObjectModel',
            pageSize: 20,
            proxy: this.proxy
        });

        this.pageBar = Ext.create('Ext.toolbar.Paging', {
            store: this.loStore,
            inputItemWidth: 45,
            firstText: tr('First Page'),
            prevText: tr('Previous Page'),
            nextText: tr('Next Page'),
            lastText: tr('Last Page'),
            afterPageText: tr('of {0}')
        } );

        this.pageBar.remove( this.pageBar.getComponent(10) ); //refresh button
        this.pageBar.remove( this.pageBar.getComponent(9) ); //separator

        this.doShowLOs = function() {
            var selectedLOs = this.getSelected();
            if( selectedLOs ) {
                var listOfLOs = selectedLOs.map( function( lo ) { return( lo.getData().id ); } ).join( ',' );
                Ext.Ajax.request( {
                    url: 'rest/learningObjects/setHidden',
                    method: 'POST',
                    params: {
                        ids: listOfLOs,
                        value: 'false'
                    },
                    success: function(response, opts) {
                        this.loStore.reload();
                    },
                    failure: function(response, opts) {
                        Ext.Msg.alert('Failure', response.responseText );
                    },
                    scope: this
                } );
            }
        }

        this.showLearningObjects = function() {
            var promptBox = Ext.Msg;
            promptBox.buttonText = { cancel: tr("Cancel") };
            promptBox.show( {
                title: tr( 'Question' ),
                msg: tr( 'Are you sure that you want to show the selected resources ?' ),
                buttons: Ext.Msg.OKCANCEL,
                fn: function( btn, text ) {
                    if( btn == 'ok' )
                        this.doShowLOs();
                },
                scope: this,
                minWidth: 250,
                multiline: false,
                icon: Ext.Msg.QUESTION
            } );
        }

        this.doHideLOs = function() {
            var selectedLOs = this.getSelected();
            if( selectedLOs ) {
                var listOfLOs = selectedLOs.map( function( lo ) { return( lo.getData().id ); } ).join( ',' );
                Ext.Ajax.request( {
                    url: 'rest/learningObjects/setHidden',
                    method: 'POST',
                    params: {
                        ids: listOfLOs,
                        value: 'true'
                    },
                    success: function(response, opts) {
                        this.loStore.reload();
                    },
                    failure: function(response, opts) {
                        Ext.Msg.alert('Failure', response.responseText );
                    },
                    scope: this
                } );
            }
        }

        this.hideLearningObjects = function() {
            var promptBox = Ext.Msg;
            promptBox.buttonText = { cancel: tr("Cancel") };
            promptBox.show( {
                title: tr( 'Question' ),
                msg: tr( 'Are you sure that you want to hide the selected resources ?' ),
                buttons: Ext.Msg.OKCANCEL,
                fn: function( btn, text ) {
                    if( btn == 'ok' )
                        this.doHideLOs();
                },
                scope: this,
                minWidth: 250,
                multiline: false,
                icon: Ext.Msg.QUESTION
            } );
        }

        this.doDeleteLOs = function() {
            var selectedLOs = this.getSelected();
            if( selectedLOs ) {
                var listOfLOs = selectedLOs.map( function( lo ) { return( lo.getData().id ); } ).join( ',' );
                Ext.Ajax.request( {
                    url: 'rest/learningObjects/delete',
                    method: 'POST',
                    params: {
                        ids: listOfLOs
                    },
                    success: function(response, opts) {
                        this.loStore.reload();
                    },
                    failure: function(response, opts) {
                        Ext.Msg.alert('Failure', response.responseText );
                    },
                    scope: this
                } );
            }
        }

        this.deleteLearningObjects = function() {
            var promptBox = Ext.Msg;
            promptBox.buttonText = { cancel: tr("Cancel") };
            promptBox.show( {
                title: tr( 'Question' ),
                msg: tr( 'Are you sure that you want to delete the selected resources ?' ),
                buttons: Ext.Msg.OKCANCEL,
                fn: function( btn, text ) {
                    if( btn == 'ok' )
                        this.doDeleteLOs();
                },
                scope: this,
                minWidth: 250,
                multiline: false,
                icon: Ext.Msg.QUESTION
            } );
        }

        var loContextMenu = Ext.create('Ext.menu.Menu', {
            items: [ { text: tr('Show'), handler: this.showLearningObjects, scope: this },
                     { text: tr('Hide'), handler: this.hideLearningObjects, scope: this },
                     { text: tr('Delete'), handler: this.deleteLearningObjects, scope: this } ]
        });

        this.renderImage = function( value ) {
            var res = "";
            if (value != "n/a")
                res = "<img src=\"" + value + "\" height=\"48\" width=\"48\">";
            return res;
        };

        this.renderHidden = function( value, metaData, lo ) {
            var flagFilename = ( value ? 'hidden' : 'visible' );
            var flagLabel = ( value ? tr( 'Hidden' ) : tr( 'Visible' ) );
            return Ext.String.format( '<img src="images/flag-{0}.png" title="{1}" alt="{1}" height="20"/>', flagFilename, flagLabel );
        };

        this.renderPending = function( value, metaData, lo ) {
            return( value ? Ext.String.format( '<img src="images/flag-pending.png" title="{0}" alt="{0}" height="20"/>', tr( 'Pending' ) ) : '' );
        };

        this.renderInactive = function( value, metaData, lo ) {
            return( value ? Ext.String.format( '<img src="images/flag-inactive.png" title="{0}" alt="{0}" height="20"/>', tr( 'Inactive' ) ) : '' );
        };

        this.renderInvalid = function( value, metaData, lo ) {
            return( value ? Ext.String.format( '<img src="images/flag-invalid.png" title="{0}" alt="{0}" height="20"/>', tr( 'Invalid' ) ) : '' );
        };

        this.renderBrokenLink = function( value, metaData, lo ) {
            return( value ? Ext.String.format( '<img src="images/flag-brokenLink.png" title="{0}" alt="{0}" height="20"/>', tr( 'Broken Link' ) ) : '' );
        };

        this.renderTitle = function( value, metaData, lo ) {
            var xf = Ext.util.Format;
            descr = xf.ellipsis( xf.stripTags( lo.data.desc ), 200 );
            return Ext.String.format( '<div class="resourceTitle"><b>{0}</b></div><div class="resourceDesc">{1}</div>', 
                       value == 'null' ? '' : value, descr );
        };

        cfg = {
            store: this.loStore,
            cls: 'lo-grid',
            scroll: 'vertical',
            columns: [ 
                { text: tr( 'File Type' ), width: 60, dataIndex: 'image', sortable: true, renderer: this.renderImage},
                { text: tr( 'Id' ), width: 100,  dataIndex: 'id', hidden: true/*!this.editable*/ },
                { text: tr( 'Title' ), flex: 1, dataIndex: 'title', sortable: true, renderer: this.renderTitle },
                { text: tr( 'Type' ), width: 80,  dataIndex: 'type', sortable: true, renderer: this.renderType, hidden: true /*!this.editable*/ },
                { text: tr( 'Visible' ), width: 30,  dataIndex: 'hidden', hidden: !this.editable, renderer: this.renderHidden },
                { text: tr( 'Pending' ), width: 30,  dataIndex: 'pending', hidden: !this.editable, renderer: this.renderPending },
                { text: tr( 'Inactive' ), width: 30,  dataIndex: 'inactive', hidden: !this.editable, renderer: this.renderInactive },
                { text: tr( 'Invalid' ), width: 30,  dataIndex: 'invalid', hidden: !this.editable, renderer: this.renderInvalid },
                { text: tr( 'Broken Link' ), width: 30,  dataIndex: 'brokenLink', hidden: !this.editable, renderer: this.renderBrokenLink }
            ],          
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
                listeners: {
                    itemcontextmenu: function( view, rec, node, index, event ) {
                        if( this.editable ) {
                            event.stopEvent(); // stops the default event. i.e. Windows Context Menu
                            loContextMenu.showAt( event.getXY() ); // show context menu where user right clicked
                            return( false );
                        }
                        else
                            return( true );
                    },
                    scope: this
                }
            },
            hideHeaders: true/*!this.editable*/,
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
            return this.getSelectionModel().getSelection();
        else
            return null;
    },
    getSelectedId: function() {
        var currSelectedLo = null;
        selected = this.getSelected();
        if (selected)
            currSelectedLo = selected[ 0 ].getData().id;
        return currSelectedLo;
    },
    getSelectedLoHtmlLocation: function() {
        var selectedLo = this.getSelected();
        if (!selectedLo)
            return(null);
        return (selectedLo[ 0 ].getData().loAsHtmlLocation);
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
                if (operation.getResponse() != null) {
                    var json = Ext.JSON.decode(operation.getResponse().responseText);
                    if (json.selectFirstRecord)
                        this.getSelectionModel().select(0);
                }
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


