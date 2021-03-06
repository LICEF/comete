﻿function getPageLocation(lg) {
    return "index.jsp?lang=" + lg;         
}

window.currentSearchQueryItem = 0;

SIMPLE_HEIGHT = 110;
ADVANCED_HEIGHT = 180;
THEMATIC_HEIGHT = 220;
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
    window.searchManager.loManager.updateQueryHistoryButtons();  
}

function setQueryPanelHeight(height, isAdvanced) {
    if (isAdvanced)
        currentAdvancedHeight = height;
    if (window.searchManager != undefined)
        window.searchManager.queryPanel.setHeight(height);
}

function getCurrentSearchPanel() {
    if( !window.searchManager )
        return( null );

    switch(window.currentSearchQueryItem) {
        case 0: return( searchManager.simpleSearchPanel );
        case 1: return( searchManager.advancedSearchPanel );
        case 2: return( searchManager.thematicSearchPanel );
        case 3: return( searchManager.collectionSearchPanel );
        default: return( null ); 
    };
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

        var mainMenu = Ext.create('Ext.panel.Panel', {
            region: 'center',
            layout: 'hbox',                     
            border: false,
            items: [ {xtype: 'tbspacer', width: 10}, this.simpleLabel, {xtype: 'tbspacer', width: 10}, 
                      this.advancedLabel, {xtype: 'tbspacer', width: 10}, this.thematicLabel, {xtype: 'tbspacer', width: 10}, this.collectionLabel  ]
        });    

        var choicePanel = Ext.create('Ext.panel.Panel', {
            layout: 'border',
            border: false,
            height: 40,
            margin: '10 0 0 0',
            items: [ mainMenu ]
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
            editable: isEditable(),
            _query: query
        } );

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
        this.loManager.setRequest( query );
    },
    setRequestSimpleSearch: function( textQuery ) {
        var query = [ { key: "fulltext", value: textQuery } ];
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
    setRequestKeyword: function( keyword ) {
        var query = [ { key: "keyword", value: keyword } ];
        displayQuery(1, query);
        this.setRequest( query );
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


Ext.onReady( function() {    
    Ext.QuickTips.init();
    setAccountRole(function(){init();});
} );

function init() {
    Ext.Ajax.request( {
        url: 'rest/system/status',
        method: 'GET',
        success: function(response, opts) {
            var status = response.responseText;
            if (status == "ok")
                initUI();
            else
                unavailable(status);
        }
    } );
}

function initUI() {
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

function unavailable(status) {
    new Ext.Viewport( {
        layout: 'fit',
        items: {
            border: false,
            margin: '10 0 0 10',
            html: '<img src="images/cometeLogo.gif"/><br/><br/><font style="font-size:16px">' +
                  tr('Server temporarily unavailable') + ' : ' + status + '</font>'
        }    
    } );
}
