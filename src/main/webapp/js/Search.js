﻿function getPageLocation(lg) {
    return "index.jsp?lang=" + lg;         
}

window.currentSearchQueryItem = 0;

SIMPLE_HEIGHT = 110;
ADVANCED_HEIGHT = 150;
THEMATIC_HEIGHT = 200;
COLLECTION_HEIGHT = 90;
var currentAdvancedHeight = ADVANCED_HEIGHT;

var currentElem;

function changeCardItem(item, elem, height) {

    currentElem.removeCls('selectedChoice');
    currentElem.addCls('choice');
    elem.removeCls('choice');
    elem.addCls('selectedChoice');
    currentElem.selected = false;
    currentElem = elem;
    window.currentSearchQueryItem = item;
    Ext.getCmp('cardPanel').getLayout().setActiveItem(item);
    setQueryPanelHeight(height, false);    
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

        var isEditable = ( ( 'true' == Ext.getUrlParam( 'editable' ) ) || false );
        var query = Ext.getUrlParam( 'query' );
        if (query)
            query = eval(query);

        this.simpleLabel = Ext.create('Comete.ClickableLabel', {
            text: tr('Simple Search'),
            cls: 'selectedChoice',
            selected: true,
            fn: function() { changeCardItem(0, searchManager.simpleLabel, SIMPLE_HEIGHT); 
                             searchManager.clear(); }
        } );

        currentElem = this.simpleLabel;

        this.advancedLabel = Ext.create('Comete.ClickableLabel', {
            text: tr('Advanced Search'),
            cls: 'choice',
            selected: false,
            fn: function() { changeCardItem(1, searchManager.advancedLabel, currentAdvancedHeight); 
                             searchManager.clear(); }
        } );

        this.thematicLabel = Ext.create('Comete.ClickableLabel', {
            text: tr('Thematic Navigation'),
            cls: 'choice',
            selected: false,
            fn: function() { changeCardItem(2, searchManager.thematicLabel, THEMATIC_HEIGHT); 
                             searchManager.clear(); }
        } );

        this.collectionLabel = Ext.create('Comete.ClickableLabel', {
            text: tr('Collections'),
            cls: 'choice',
            selected: false,
            fn: function() { changeCardItem(3, searchManager.collectionLabel, COLLECTION_HEIGHT); 
                             searchManager.clear(); }
        } );

        var choicePanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',                     
            border: false,
            height: 40, 
            margin: '10 0 0 0',
            items: [ {xtype: 'tbspacer', width: 10}, this.simpleLabel, {xtype: 'tbspacer', width: 10}, 
                      this.advancedLabel, {xtype: 'tbspacer', width: 10}, this.thematicLabel, {xtype: 'tbspacer', width: 10}, this.collectionLabel  ]
        });    

        this.simpleSearchPanel = Ext.create('Comete.SimpleSearch', {
            border: false,
            lang: lang,
            _query: query
        } );

        this.advancedSearchPanel = Ext.create('Comete.AdvancedSearch', {
            border: false,
            lang: lang
        } );

        this.thematicSearchPanel = Ext.create('Comete.ThematicNavigationSearch', {
            border: false,
            lang: lang
        } );

        this.collectionSearchPanel = Ext.create('Comete.CollectionSearch', {
            border: false,
            lang: lang
        } );
 
        var cardPanel = Ext.create('Ext.panel.Panel', {
            id: 'cardPanel',
            layout: 'card',
            region: 'center',
            border: false,
            items: [ this.simpleSearchPanel, this.advancedSearchPanel, this.thematicSearchPanel, this.collectionSearchPanel ]
        });

        this.queryPanel = Ext.create('Ext.panel.Panel', {
            layout: 'border',
            region: 'north',
            height: 100,
            border: false, 
            items: [ { region: 'north', border: false, items: [ choicePanel ] }, cardPanel ]
        });

        this.loManager = Ext.create('Comete.LearningObjectManager', {        
            region: 'center',
            border: false,
            lang: lang,
            editable: isEditable,
            _query: query
        } );

        cfg = {
            items: [ this.queryPanel, this.loManager ]
        };
        
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    }, 
    clear: function() {
        this.loManager.clear();
    },
    setRequest: function( query ) {
        this.loManager.setRequest( query );
    },
    setRequestSimpleSearch: function( textQuery ) {
        var query = [ { key: "ss", value: textQuery } ];
        this.setRequest(query);
    },
    setRequestContributor: function( contribId, label ) { 
        var query = [ { key: "contrib", value: contribId, label: label } ];
        displayQuery(1, query);
        this.setRequest( query );
    },
    setRequestOrganization: function( orgId, label ) {
        var query = [ { key: "org", value: orgId, label: label } ];
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
    setRequestKeyword: function( keyword, lang ) {
        var query = [ { key: "keyword", value: keyword, lang: lang } ];
        displayQuery(1, query);
        this.setRequest( query );
    },
    initCollections: function() {
        this.collectionSearchPanel.init();
    },
    closeDialog: function() {
        IdentityDetailsWindow.close();
    }
} );


Ext.onReady( function() {    
    Ext.QuickTips.init();
    setAuthorized(function(){init();})
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
}