Ext.define( 'Comete.LearningObjectManager', {
    extend: 'Ext.panel.Panel',
    layout: 'border',    
    initComponent: function (config) {    

        //History data
        this.queryHistory = [];
        this.queryHistoryIndex = -1;

        this.goBackwardQueryButton = Ext.create('Ext.button.Button', { 
            tooltip: tr( 'Go back one query' ), 
            iconCls: 'x-tbar-goBackwardQuery',
            height: 23, 
            handler: this.goBackwardQuery,
            scope: this,
            disabled: true
        } );

        this.goForwardQueryButton = Ext.create('Ext.button.Button', { 
            tooltip: tr( 'Go forward one query' ), 
            iconCls: 'x-tbar-goForwardQuery',
            height: 23, 
            handler: this.goForwardQuery, 
            scope: this,
            disabled: true 
        } );

        this.resultLabel = Ext.create('Ext.form.Label', {
            margin: '4',
            cls: 'results'
        } );

        this.resultAtomButton = Ext.create('Ext.button.Button', {
            icon: 'images/rss.png',
            text: 'Atom',
            handler: function() { 
                url = 'rest/queryEngine/searchAtom?lang=' + this.lang + '&q=' + encodeURIComponent(JSON.stringify(this.getCurrentQuery().query));
                window.open(url); 
            },
            scope: this            
        } );
        this.resultAtomButton.setVisible(false); 

        this.resultRssButton = Ext.create('Ext.button.Button', {
            icon: 'images/rss.png',
            text: 'RSS',
            handler: function() { 
                url = 'rest/queryEngine/searchRss?lang=' + this.lang + '&q=' + encodeURIComponent(JSON.stringify(this.getCurrentQuery().query));
                window.open(url); 
            },
            scope: this            
        } );
        this.resultRssButton.setVisible(false); 

        var resultPanel = Ext.create('Ext.panel.Panel', {
            region: 'north',
            height: 30,        
            layout: 'hbox',
            border: false,
            items: [              
                { xtype: 'tbspacer', width: 10 }, 
                this.goBackwardQueryButton, 
                { xtype: 'tbspacer', width: 4 }, 
                this.goForwardQueryButton, 
                { xtype: 'tbspacer', width: 6 },
                this.resultLabel,
                { xtype: 'tbspacer', width: 40 }, 
                this.resultAtomButton, 
                { xtype: 'tbspacer', width: 10 }, 
                this.resultRssButton
            ]        
        } );       


        this.learningObjectTable = Ext.create('Comete.LearningObjectTable', {
            region: 'west',
            lang: this.lang,
            split: true,
            width: 500,
            editable: this.editable,
            _query: this._query,
            loManager: this
        } );

        this.viewer = Ext.create('Comete.Viewer', {
            region: 'center',
            lang: this.lang
        } );

        var mainPanel = Ext.create('Ext.panel.Panel', {
            layout: 'border',
            region: 'center',
            border: false,
            margin: '0 -1 -1 -1',
            items: [ this.learningObjectTable, { region: 'center', autoScroll: true, items: this.viewer } ]
        } );  

        cfg = {
            items: [ resultPanel, mainPanel ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);    

        this.learningObjectTable.on( 'selectionchange', function( model, selected ) {
            if (selected && !this.isViewerCollapsed()) 
                this.initViewerContentForSelection();
        }, this );

        this.learningObjectTable.loStore.on( 'beforeLoad', function() {
            this.setViewerContent( null );
        }, this );
    },
    clear: function() {
        this.setResultText( '' );
        this.setFeedButtonsVisible(false);
        this.learningObjectTable.clear();
    },
    isViewerCollapsed: function() {
        return( this.viewer.isContentCollapsed() );
    },
    setViewerContent: function( url ) {
        this.viewer.setContent( url == 'null' ? null : url );
    },
    initViewerContentForSelection: function() {
        var loc = this.learningObjectTable.getSelectedLoHtmlLocation();
        if( loc ) {
            if( loc.indexOf( '$lang' ) != -1 )
                loc = loc.replace( '$lang', this.lang );
            this.setViewerContent( loc );
        }
        else 
            this.setViewerContent( null );
    },
    setRequest: function( query ) {
        this.learningObjectTable.setRequest( query );
    },
    saveQueryHistory: function( url, query, pageBeforeNewRequest, selectedLoBeforeNewRequest ) {
        var lastQueryUrl = null;
        if( this.queryHistoryIndex >= 0 ) {
            this.queryHistory[ this.queryHistoryIndex ].page = pageBeforeNewRequest;
            this.queryHistory[ this.queryHistoryIndex ].selectedLo = selectedLoBeforeNewRequest;
            lastQueryUrl = this.queryHistory[ this.queryHistoryIndex ].url;
        }

        // Only save the new query if it's different from the last one on the stack.
        if( lastQueryUrl == null || Ext.String.trim(url) != Ext.String.trim(lastQueryUrl) ) {
            this.queryHistoryIndex++;
            this.queryHistory[ this.queryHistoryIndex ] = { url: url, 
                                                            query: query,  
                                                            queryPanelItem: window.currentSearchQueryItem };
            if( this.queryHistory.length > this.queryHistoryIndex + 1 )
                this.queryHistory.splice( this.queryHistoryIndex + 1, this.queryHistory.length - this.queryHistoryIndex - 1 );
            this.updateQueryHistoryButtons();
        } 
    },
    updateQueryHistoryButtons: function() {
        var isBackwardButtonDisabled = ( this.queryHistoryIndex <= 0 || this.queryHistory.length <= 1);
        var isForwardButtonDisabled = ( this.queryHistoryIndex >= this.queryHistory.length - 1 );
        if( this.goBackwardQueryButton )
            this.goBackwardQueryButton.setDisabled( isBackwardButtonDisabled );
        if( this.goForwardQueryButton )
            this.goForwardQueryButton.setDisabled( isForwardButtonDisabled );
    },
    goBackwardQuery: function() {
        this.queryHistoryIndex--;
        this.learningObjectTable.performGoQuery(this.queryHistory[this.queryHistoryIndex]);

    },
    goForwardQuery: function() {
        this.queryHistoryIndex++;
        this.learningObjectTable.performGoQuery(this.queryHistory[this.queryHistoryIndex]);
    },
    getCurrentQuery: function() {
        return this.queryHistory[ this.queryHistoryIndex ];
    },
    setResultText: function(text) {
        this.resultLabel.setText( text );
    },
    setFeedButtonsVisible: function(visible) {
        this.resultAtomButton.setVisible(visible);
        this.resultRssButton.setVisible(visible);
    }
} );


