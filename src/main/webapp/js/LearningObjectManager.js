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
                this.resultAtomButton, 
                { xtype: 'tbspacer', width: 10 }, 
                this.resultRssButton,
                { xtype: 'tbspacer', width: 10 }, 
                this.resultLabel
            ]        
        } );       


        this.learningObjectTable = Ext.create('Comete.LearningObjectTable', {
            region: 'west',
            lang: this.lang,
            split: true,
            width: 500,
            margin: '-1 0 -1 -1',
            editable: this.editable,
            _query: this._query,
            loManager: this
        } );

        this.facetsPanel = Ext.create('Comete.Facets', {
            region: 'west',
            width: 150,
            resizable: false,
            border: false,
            //margin: '-1 0 -1 -1',
            lang: this.lang
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
            items: [ this.facetsPanel, 
                     { layout: 'border', 
                       region: 'center', 
                       border: true,
                       bodyStyle: { borderColor: 'lightgrey' },
                       items: [ this.learningObjectTable, this.viewer ] }
                     ]  
            //items: [ this.learningObjectTable, { region: 'center', autoScroll: true, items: this.viewer } ]
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
        this.viewer.clear();
    },
    isViewerCollapsed: function() {
        return( this.viewer.isContentCollapsed() );
    },
    setViewerContent: function( url ) {
        if( url == 'null' )
            this.viewer.setContent( null );
        else {
            this.viewer.setContent( url, initSharingLinks );
            //initSharingLinks();
        }
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


Ext.define( 'Comete.Facets', {
    extend: 'Ext.panel.Panel',
    layout: 'vbox',
    initComponent: function( config ) {
        
        this.cbRelevance = Ext.create('Ext.form.field.Checkbox', {
            checked: true, 
            style: { color: 'white' }, 
            margin: '5 0 0 20', 
            boxLabel: tr('Relevance'),
            handler: this.checkRelevance,
            scope: this,
            key: 'relevance'
        });        

        this.cbDateAdded = Ext.create('Ext.form.field.Checkbox', {
            style: { color: 'white' }, 
            margin: '5 0 0 20', 
            boxLabel: tr('Date added'),
            handler: this.checkDateAdded,
            scope: this,
            key: 'added'
        });

        this.sortBySelection = this.cbRelevance;

        this.cbAll = Ext.create('Ext.form.field.Checkbox', {
            checked: true,
            style: { color: 'white' }, 
            margin: '5 0 0 20', 
            boxLabel: tr('All '),
            handler: this.checkAll,
            scope: this,
            key: null
        });

        this.cbFrench = Ext.create('Ext.form.field.Checkbox', {
            style: { color: 'white' }, 
            margin: '5 0 0 20', 
            boxLabel: tr('French'),
            handler: this.checkFrench,
            scope: this,
            key: 'fr'
        });

        this.cbEnglish = Ext.create('Ext.form.field.Checkbox', {
            style: { color: 'white' }, 
            margin: '5 0 0 20', 
            boxLabel: tr('English'),
            handler: this.checkEnglish,
            scope: this,
            key: 'en'
        });

        this.languageSelection = this.cbAll;

        cfg = {
            cls: 'searchPanel',
            bodyStyle: { border: 'none' },
            /*header: {
                style: { background: '#D6128A' },
            },  */          
            items: [
                { xtype: 'label', text: tr('SORT BY'), margin: '20 0 0 20',
                   style: {color: 'white', fontWeight: 'bold', fontSize: '12px' } },
                this.cbRelevance,
                this.cbDateAdded,

                { xtype: 'box', width: 120, height: 2, margin: '10 0 0 15', style: { background: 'white' } },

                { xtype: 'label', text: tr('LANGUAGE'), margin: '10 0 0 20',
                   style: {color: 'white', fontWeight: 'bold', fontSize: '12px' } },
                this.cbAll,
                this.cbFrench,
                this.cbEnglish
            ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    manageFacetElement: function(cb, checked, group) {
         var selected;
         if (group == 'sortBy')
             selected = this.sortBySelection;
         else if (group == 'language')
             selected = this.languageSelection;

         if (!checked && selected == cb)
             cb.setValue(true);
    },
    checkRelevance: function(cb, checked) {         
         this.manageFacetElement(cb, checked, 'sortBy');
         if (checked && this.sortBySelection != cb) {
             this.sortBySelection = cb;
             this.cbDateAdded.setValue(false);

             //action
             window.searchManager.redoRequest();
         }
    },
    checkDateAdded: function(cb, checked) {
         this.manageFacetElement(cb, checked, 'sortBy');
         if (checked && this.sortBySelection != cb) {
             this.sortBySelection = cb;
             this.cbRelevance.setValue(false);

             //action
             window.searchManager.redoRequest();
         }
    },
    checkAll: function(cb, checked) {         
         this.manageFacetElement(cb, checked, 'language');
         if (checked && this.languageSelection != cb) {
             this.languageSelection = cb;
             this.cbFrench.setValue(false);
             this.cbEnglish.setValue(false);

             //action
             window.searchManager.redoRequest();
         }
    },
    checkFrench: function(cb, checked) {         
         this.manageFacetElement(cb, checked, 'language');
         if (checked && this.languageSelection != cb) {
             this.languageSelection = cb;
             this.cbAll.setValue(false);
             this.cbEnglish.setValue(false);

             //action
             window.searchManager.redoRequest();
         }
    },
    checkEnglish: function(cb, checked) {         
         this.manageFacetElement(cb, checked, 'language');
         if (checked && this.languageSelection != cb) {
             this.languageSelection = cb;
             this.cbAll.setValue(false);
             this.cbFrench.setValue(false);

             //action
             window.searchManager.redoRequest();
         }
    },
    getOrderBy: function() {
        return this.sortBySelection.key;
    },
    getLanguage: function() {
        return this.languageSelection.key;
    }
} );