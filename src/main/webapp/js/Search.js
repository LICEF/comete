function getPageLocation(lg) {
    return "index.jsp?lang=" + lg;         
}

window.currentSearchQueryItem = 0;

SIMPLE_HEIGHT = 110;
ADVANCED_HEIGHT = 150;
THEMATIC_HEIGHT = 220;

QUERYPANEL_HEIGHT = 500;

CARDPANEL_WIDTH = 500;

var currentAdvancedHeight = ADVANCED_HEIGHT;

function changeCardItem(item, elem, height) {
    window.currentSearchQueryItem = item;
    Ext.getCmp('cardPanel').getLayout().setActiveItem(item);
    //setQueryPanelHeight(height, false);    
}

function setQueryPanelHeight(height, isAdvanced) {
    if (isAdvanced)
        currentAdvancedHeight = height;
    if (window.searchManager != undefined)
        window.searchManager.queryPanel.setHeight(height);
}

function displayQuery(queryItem, query) {
    var elem;
    var panel;
    var height;  
    switch(queryItem) {
        case 0:
            elem = searchManager.simpleLabel;
            panel = searchManager.simpleSearchPanel; 
            height = SIMPLE_HEIGHT; break;
        case 1:
            elem = searchManager.advancedLabel;
            panel = searchManager.advancedSearchPanel;
            height = currentAdvancedHeight; break;
        case 2:
            elem = searchManager.thematicLabel;
            panel = searchManager.thematicSearchPanel;
            height = THEMATIC_HEIGHT; break;
        case 3:
            elem = searchManager.collectionLabel;
            panel = searchManager.collectionSearchPanel;
            height = COLLECTION_HEIGHT; break;
    };
    panel.setQuery(query);

    if (queryItem == 1) //setQuery change it
        height = currentAdvancedHeight; 
  
    if (window.currentSearchQueryItem != queryItem)
        changeCardItem(queryItem, elem, height);
}

Ext.define( 'Comete.SearchManager', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {
        
        // By default, the interface is displayed in English.
        var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
        utilsInit(lang);

        var query = Ext.getUrlParam( 'query' );
        if (query)
            query = eval(query);
        else {
            var uri = Ext.getUrlParam( 'uri' );       
            if (uri != null) 
                query = [ { key: "uri", value: uri } ];
            else {
                var loUuid = Ext.getUrlParam( 'lo-uuid' );
                if( loUuid != null ) 
                    query = [ { key: "uri", value: window.cometeUriPrefix + '/learningobject/' + loUuid } ];
            }
        }

        this.simpleSearchPanel = Ext.create('Comete.SimpleSearch', {
            border: false,
            lang: lang
            //_query: query
        } );

        this.advancedSearchPanel = Ext.create('Comete.AdvancedSearch', {
            border: false,
            lang: lang
        } );

        this.thematicSearchPanel = Ext.create('Comete.ThematicNavigationSearch', {
            border: false,
            lang: lang
        } );
 
        this.cardPanel = Ext.create('Ext.panel.Panel', {
            id: 'cardPanel',
            layout: 'card',
            border: false,
            items: [ this.simpleSearchPanel, this.advancedSearchPanel, this.thematicSearchPanel ]
        });

        this.logo = Ext.create('Ext.Img', {
            src: 'images/Logo_CERES.png',
            height: (query != null)?40:58
        } );

        this.firstQueryPanelSpacer = Ext.create('Ext.toolbar.Spacer', {            
            height: 80
        } );

        this.descriptionPanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox', 
            width: '100%', 
            border: false,
            cls: 'transp1',                                          
            items: [ {xtype: 'tbfill'}, 
                     { border: false, width: 560, 
                       html: '<iframe width="100%" height="500" frameborder="0" src="' + lang + '/description.html"></iframe>' },
                     {xtype: 'tbfill'} ]
        });

        this.queryPanel = Ext.create('Ext.panel.Panel', {
            layout: 'vbox',
            region: 'north',
            height: QUERYPANEL_HEIGHT,
            border: false,
            cls: 'grad', 
            items: [ this.firstQueryPanelSpacer,
                     { layout: 'hbox', 
                       width: '100%', 
                       border: false,
                       cls: 'transp1',                                          
                       items: [ {xtype: 'tbfill'}, 
                                { layout: 'vbox', border: false, 
                                  items: [ this.logo, { xtype: 'tbspacer', height: 5}, this.cardPanel ] }, 
                                {xtype: 'tbfill'} ] },
                     {xtype: 'tbfill'},
                     { xtype: 'tbspacer', height: 30 },
                     this.descriptionPanel                     
                   ]
        });

        this.loManager = Ext.create('Comete.LearningObjectManager', {        
            region: 'center',
            border: false,
            lang: lang,
            editable: isEditable(),
            //_query: query
        } );

        if (query == null) {
            this.loManager.on('render', function() {
                this.loManager.getEl().setOpacity(0);
            }, this);
        }

        cfg = {
            items: [ this.queryPanel, this.loManager ]
        };
        
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.on('render', function() {
            if (query != null)
                this.setRequest(query);
        })

    }, 
    clear: function() {
        this.loManager.clear();
    },
    setRequest: function( query ) {
        this.firstQueryPanelSpacer.setHeight(0);
        this.queryPanel.setHeight(null);
        this.logo.setHeight(40);
        this.descriptionPanel.setVisible(false);
        if (this.loManager.getEl() != undefined) {
            this.loManager.getEl().fadeIn({
                duration: 1500
            });
        }
        this.loManager.setRequest( query );
    },
    setRequestSimpleSearch: function( textQuery ) {
        var query = new Array();
        query[0] = { key: "fulltext", value: textQuery };
        if (this.getLanguageCondition() != null) {            
            if (textQuery != '') {
                query[1] = { "op": "AND" };
                query[2] = { key: "language", value: searchManager.getLanguageCondition() };
            }
            else {
                query = [ { key: "language", value: searchManager.getLanguageCondition() } ];
            }
        }
        this.setRequest(query);
    },
    setRequestContributor: function( contribId, label ) { 
        var query = new Array();
        var i = 0;
        if (this.getLanguageCondition() != null) {
            query[0] = { key: "language", value: searchManager.getLanguageCondition() };
            query[1] = { "op": "AND" };
            i = 2;
        }
        query[i] = { key: "contrib", value: contribId, label: label };
        displayQuery(1, query);
        this.setRequest( query );
    },
    setRequestOrganization: function( orgId, label ) {
        var query = new Array();
        var i = 0;
        if (this.getLanguageCondition() != null) {
            query[0] = { key: "language", value: searchManager.getLanguageCondition() };
            query[1] = { "op": "AND" };
            i = 2;
        }
        query[i] = { key: "org", value: orgId, label: label };
        displayQuery(1, query);
        this.setRequest( query );
    },
    setRequestVocConcept: function( conceptUri ) {
        var query = [ { key: "vocConcept", value: conceptUri } ];
        displayQuery(2, query);
        this.setRequestVocConcept2( query );
    },
    setRequestVocConcept2: function( query ) {
        this.setRequest( query );
    },
    setRequestCollection: function( id, query ) {
        this.setRequest( query );
    },
    setRequestKeyword: function( keyword ) {
        var query = [ { key: "keyword", value: keyword } ];
        displayQuery(1, query);
        this.setRequest( query );
    },
    redoRequest: function( textQuery ) {
        this.cardPanel.getLayout().getActiveItem().redoRequest();
    },
    getLanguageCondition: function() {
        return this.loManager.getLanguageCondition();
    },
    initCollections: function() {
        this.collectionSearchPanel.init();
    },
    closeDialog: function() {
        IdentityDetailsWindow.close();
    },
    deleteFoundLearningObjects: function() {
        Ext.Ajax.request( {
            url: 'rest/learningObjects/deleteByQuery',
            method: 'GET',
            params: {
                query: encodeURIComponent( JSON.stringify( this.loManager.learningObjectTable.getQuery() ) ),
                lang: lang
            },
            success: function(response, opts) {
                Ext.Msg.alert(tr( 'In progress...' ), tr( 'The request has been received and is currently being processed.' ) );
                this.loManager.learningObjectTable.reload();
            },
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText );
            },
            scope: this
        } );
    },
    setFlagForFoundLearningObjects: function( flag, value ) {
        var action = (flag == 'all')?'clearFlagsByQuery':'setFlagByQuery';
        Ext.Ajax.request( {
            url: 'rest/learningObjects/' + action,
            method: 'GET',
            params: {
                query: encodeURIComponent( JSON.stringify( this.loManager.learningObjectTable.getQuery() ) ),
                lang: lang,
                flag: flag,
                value: value
            },
            success: function(response, opts) {
                Ext.Msg.alert(tr( 'In progress...' ), tr( 'The request has been received and is currently being processed.' ) );
                this.loManager.learningObjectTable.reload();
            },
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText );
            },
            scope: this
        } );
    }
} );

function setAdvancedSearchPanelHeight(height) {
    if (window.searchManager != undefined)
        window.searchManager.advancedSearchPanel.setHeight(height);
}

Ext.onReady( function() {    
    Ext.QuickTips.init();
    setAccountRole(function(){init();});
} );

function init() {
    var searchManager = Ext.create('Comete.SearchManager', {
        region: 'center',
        border: false
    });

    // This global reference is needed for interoperability with pages generated by Data Transformer.
    window.searchManager = searchManager;

    new Ext.Viewport( {
        layout: 'border',
        items: [ {
            layout: 'border',
            border: false,
            region: 'center',
            tbar: tbar,
            items: [ searchManager ]
        } ]    
    } );

    updateToolbar();
}
