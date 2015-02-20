
/***********************/
/*** Authorized flag ***/
/***********************/
var authorized = false;
function setAuthorized(callback) {
    Ext.Ajax.request( {
        url: 'rest/security/isAuthorized',
        method: 'GET',
        success: function(response, opts) {
            authorized = response.responseText == 'true';
            callback.call();
        }
    } );
}


/**************************************************/
/*** Miscellaneous utility functions or objects ***/
/**************************************************/

Ext.override(Ext.Window, {
    constrain: true
});


function utilsInit( lg ) {
    vocabProxy.url = 'rest/voc/all?lang=' + lg;
    vocCtxtProxy.url = 'rest/vocContexts?lang=' + lg;
}

Ext.getUrlParam = function( param ) {
   var params = Ext.urlDecode( location.search.substring( 1 ) );
   return( param ? params[ param ] : params );
}

function replaceAll( str, strToLookFor, strToReplaceWith ) {
    return( str.replace( new RegExp( strToLookFor, 'g' ), strToReplaceWith ) );
}

// Strip leading and trailing whitespace characters and replace all repeated whitespace characters into 1 space (like XSLT's normalize-space() function). 
function normalizeSpace( str ) {
    return( str.replace( /^\s*|\s(?=\s)|\s*$/g, '' ) );
}
Ext.Ajax.timeout = 120000; // Overwrite default Ajax timeout to 2 minutes.


function normalizeSearchQuery( query ) {
    // Handle double quotes here and other special keywords or criterias.
    // TODO

    // Remove undesirable commas.
    var newValue = replaceAll( query, ',', ' ' );

    // Normalize whitespace characters.
    newValue = normalizeSpace( newValue );

    return( newValue );
}

//only FF has startsWith and endsWith methods !
//patch for IE, Safari, Chrome, Opera -AM

if (!String.prototype.startsWith) {
     String.prototype.startsWith = function (str) {
         return this.slice(0, str.length) == str;
     };
}
 
if (!String.prototype.endsWith) {
     String.prototype.endsWith = function(str) {
         return this.slice(-str.length) == str;
     };
}

if (!Array.prototype.indexOf) {
     Array.prototype.indexOf = function(obj, start) {
         for (var i = (start || 0), j = this.length; i < j; i++) {
             if (this[i] === obj) { return i; }
         }
         return -1;
     }
}

Array.prototype.indexOf = function(obj, start) {
     for (var i = (start || 0), j = this.length; i < j; i++) {
         if (this[i] === obj) { return i; }
     }
     return -1;
}

function showIdentity( windowId, url, left, top, width, height ) {
    IdentityDetailsWindow.init( windowId, url, left, top, width, height );
}

var IdentityDetailsWindow = function() {
    var win;

    return {
        init: function( winId, url, xPos, yPos, width, height ) {
            win = new Ext.Window( {
                id: winId,
                resizable: true,
                maximizable: true,
                border: false,
                width: width,
                height: height,
                html:  '<iframe id="IdentityDetailsIFrame" width="100%" height="100%" src="' + url + '" frameborder="0"></iframe>'
            } );
            win.showAt( xPos, yPos );
        },
        close: function() {
            if( win != null ) {
                win.close();
                win = null;
            }
        }
    };
}();


/**********************/
/*** ClickableLabel ***/
/**********************/

Ext.define( 'Comete.ClickableLabel', {
    extend: 'Ext.form.Label',
    initComponent: function( config ) {
        var fn = this.fn;
        var label = this;
        var cfg = {
            listeners: {
                el: {
                    click: function() { 
                        if (!label.selected) {
                            fn.call(); 
                            label.selected = true;
                            label.getEl().setStyle( 'cursor', '');
                        }
                    },
                    mouseenter: function(evt) { 
                        if (!label.selected)
                            Ext.get(evt.target).setStyle( 'cursor', 'pointer' ) 
                    },
                    mouseleave: function(evt) { Ext.get(evt.target).setStyle( 'cursor', '') }
                } 
            }
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    }
} );


/******************/
/*** Breadcrumb ***/
/******************/

Ext.define( 'Comete.Breadcrumb', {
    extend: 'Ext.panel.Panel',
    initComponent: function( config ) {

        this.rootButton;
        this.lastElement;
        this.showIds = false;
        this.automaticQuery = true;

        this.toolbar = Ext.create('Ext.toolbar.Toolbar', {
            height: 36,
            overflowHandler: 'scroller',
            autoDestroy: false,           
            baseCls: Ext.baseCSSPrefix + 'breadcrumb'
        });        

        this.hiddenButton = Ext.create('Ext.button.Button', {
            text: 'should not be seen'
        });

        var cfg = {
            height: 36,
            layout: 'hbox',
            margin: '0 0 -1 0',
            items: this.hiddenButton,
            tbar: this.toolbar
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.store.on( 'load', function() { this.displayData(this.store.getProxy().getReader().rawData); }, this);
    },
    displayData: function(json) {
        var button;
        var manageRoot = this.rootButton == null;
        var uri = json.uri;
        var label = manageRoot?json.label:json.text;
        var minLabel = label;
        var restUrl = json.restUrl;
        if (label.length > 35)
            minLabel = label.substring(0, 35) + '...';
                    
        if (json.leaf) {
            button = Ext.create('Ext.button.Button', {
                    text: minLabel,
                    cls: 'breadcrumb-button',
                    tooltip: label,
                    uri: uri,
                    height: 24,
                    handler: function() {this.goElement(button, true);},
                    scope: this
                })
        }
        else {
            
            var handler = null;
            if (!manageRoot)
                handler = function() {this.goElement(button, true);}
            button = Ext.create('Ext.button.Split', {
                cls: 'breadcrumb-button',
                text: manageRoot?label:minLabel,
                tooltip: manageRoot?null:label,
                uri: uri,
                height: 24,
                handler: handler,
                scope: this
            });   

            button.on( 'arrowclick', function(b) { 
                if (b.menu != null) 
                    b.showMenu();
                else
                    this.showDynamicMenu(b, restUrl, manageRoot);
            }, this);
            if (manageRoot)
                this.rootButton = button;
        }
        this.displayButton(button, label);
        if (button != this.rootButton)
            this.lastElement = button.uri;
        //to unfocus button
        this.hiddenButton.focus();
    },
    displayButton: function(button, label) {            
        pos = this.toolbar.items.length;
        this.toolbar.insert( pos, button ); 
        button.index = pos;
    },
    createMenu: function(concepts) {
        var menu = Ext.create('Ext.menu.Menu');
        for (var i = 0; i < concepts.length; i++) {
            var element = concepts[i];
            var cfg = {
                element: element,
                text: element.text,
                handler: this.goDown,
                scope: this
            };
            menu.add(cfg);
        };
        return menu;
    },
    showDynamicMenu: function(button, restUrl, isTopConcepts) {
        Ext.Ajax.request({
            url: restUrl + (isTopConcepts?"/topConcepts":this.subElementQuery) + '?showIds=' + this.showIds + '&lang=' + this.lang,
            method: 'GET',
            success: function(response) {
                var json = Ext.JSON.decode(response.responseText, true);
                var menu = this.createMenu(json.concepts);
                button.setMenu(menu);
                menu.button = button;
                button.showMenu();
            },
            scope: this
        }); 
    },
    goDown: function(item) {
        this.goElement(item.parentMenu.button, false);
        this.displayData(item.element);
        this.listener.bcElementClicked(item.element.uri);        
    },
    goElement: function(button, stayHere) {
        var stopIndex = button.index;
        var lastButtonIndex = this.toolbar.items.length - 1;
        for (i = lastButtonIndex; i > stopIndex; i--) {
            var elem = this.toolbar.getComponent(i);
            this.toolbar.remove(elem);
        };        
        if (stayHere) {
            this.lastElement = button.uri;
            this.listener.bcElementClicked(this.lastElement);
            //to unfocus button
            this.hiddenButton.focus();
        }
    },
    clear: function() {

        for (i = this.toolbar.items.length - 1; i >= 0; i--) {
            var elem = this.toolbar.getComponent(i);
            this.toolbar.remove(elem);
        };
        this.rootButton = null;
        this.lastElement = null;
    },
    showIDsConcepts: function(b) {
        this.showIds = b;
    },
    setAutomaticQuery: function(b) {
        this.automaticQuery = b;
    },
    getLastElement: function() {
        return this.lastElement;
    },   
    displayElement: function(uri) {
        if (uri != null) {
            Ext.Ajax.request({
                url: 'rest/voc/' + encodeURIComponent(uri) + '/hierarchy?includeScheme=true',
                method: 'GET',
                params: {
                    showIds: this.showIds,
                    lang: this.lang
                },
                success: function(response) { 
                    //loop to rebuild concept buttons
                    this.displayPath(Ext.JSON.decode(response.responseText, true));
                },
                scope: this
            }); 
        }
    },
    displayPath: function(json) {
        if (json != null) { 
            //clear toolbar
            this.clear();
    
            //build toolbar
            for (var i = 0; i < json.concepts.length; i++) 
                this.displayData( json.concepts[i] ); 
        }
    }
} );


/*********************************/
/*** Vocabulary related models ***/
/*********************************/

Ext.define('KeywordModel', {
    extend: 'Ext.data.Model',
    fields: [ 'keyword' ]
});

Ext.define('VocabModel', {
    extend: 'Ext.data.Model',
    fields: [ 'uri', 'restUrl', 'label' ]
});

Ext.define('VocabularyConceptModel', {
    extend: 'Ext.data.Model',
    fields: [ 'uri', 'restUrl', 'text' ]
});

Ext.define('ConceptModel', {
    extend: 'Ext.data.Model',
    fields: [ 'uri', 'label', 'vocLabel' ]
});

Ext.define('VocCtxtModel', {
    extend: 'Ext.data.Model',
    fields: [ 'restUrl', 'label' ]
});

Ext.define('VocAliasModel', {
    extend: 'Ext.data.Model',
    fields: [ 'alias' ]
});

Ext.define('CollectionModel', {
    extend: 'Ext.data.Model',
    fields: [ 'id', 'label', 'query' ]
});

Ext.define('HarvestDefModel', {
    extend: 'Ext.data.Model',
    fields: [ 'restUrl', 'name' ]
});

var vocabProxy = Ext.create('Ext.data.proxy.Ajax', {
    reader: {
        type: 'json',
        root: 'vocabularies'
    }
});

var vocCtxtProxy = Ext.create('Ext.data.proxy.Ajax', {
    reader: {
        type: 'json',
        root: 'vocContexts'
    }
});

var collectionProxy = Ext.create('Ext.data.proxy.Ajax', {
    reader: {
        type: 'json',
        root: 'collections'
    }
});

var harvestDefProxy = Ext.create('Ext.data.proxy.Ajax', {
    url: 'rest/harvestDefinitions',
    reader: {
        type: 'json',
        root: 'harvestDefs',        
    }
});

/************************/
/*** VocConceptPicker ***/
/************************/


Ext.define( 'Comete.VocConceptPicker', {
    extend: 'Ext.window.Window',
    initComponent: function( config ) {

        this.extendedMode = (this.vocRestUrl == undefined);

        this.conceptProxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: 'concepts'
            }
        });

        this.treeConceptStore = Ext.create('Ext.data.TreeStore', {
            proxy: this.conceptProxy            
        });

        this.treeConceptStore.on( 'beforeload', function(store, operation) {
            if (operation.getUrl() == "" && this.conceptProxy.url == "")
                return false;                
        }, this);        

        this.treeConceptStore.on( 'load', function(store, node) {
            if (!this.extendedMode && this.getTitle() == null && this.conceptProxy.getReader().rawData != undefined)
                this.setTitle(this.conceptProxy.getReader().rawData.label);
            this.treeConceptStore.getRootNode().expand();        
        }, this);

        var label = Ext.create('Ext.form.Label', {
            height: 14,
            margin: '10 0 0 5'
        });        

        this.tree = Ext.create('Ext.tree.Panel', {
            region: 'center',
            store: this.treeConceptStore,
            border: this.extendedMode,
            useArrows: true,
            rootVisible: false,
            viewConfig: {
                loadingText: tr('Loading') + '...',
                margin: '0 -1 0 -1'
            }
        });

        this.tree.on( 'beforeitemexpand', function(node) {
            this.conceptProxy.url = node.getData().restUrl +'/children?showIds=' + this.showIds + '&lang=' + this.lang;
        }, this);


        this.selectConceptButton = Ext.create('Ext.button.Button', {
            text: tr('Select'),
            disabled: true,
            handler: function(){
                this.hide();
                elem = this.tree.getSelectionModel().getSelection()[0].getData();
                vocLabel = '';
                conceptLabel = elem.text;
                if (this.extendedMode) {
                    vocLabel = this.vocabList.getSelectionModel().getSelection()[0].getData().label;
                    if (this.showIds && conceptLabel.indexOf("&nbsp;&nbsp;") != -1)
                        conceptLabel = conceptLabel.substring(conceptLabel.indexOf("&nbsp;&nbsp;") + "&nbsp;&nbsp;".length);
                }
                this.aListener.setVocConcept(this.vocUri, elem.uri, vocLabel, conceptLabel, elem.leaf, true);
            },
            scope: this
        } );

        this.tree.on( 'itemclick', function(view, record) {
            this.selectConceptButton.setDisabled(false);
        }, this);

        this.vocPanel = { region: 'west', width: 0 }; 

        this.cbId = { xtype: 'component'};

        if (this.extendedMode) {

            this.setTitle( tr('Vocabularies') );

            this.vocabStore = Ext.create('Ext.data.JsonStore', {
                model: 'VocabModel',
                proxy: vocabProxy
            });   
            
            this.vocabList = Ext.create('Ext.grid.Panel', {
                store: this.vocabStore,
                border: false,
                columns: [ 
                    { dataIndex: 'label', flex: 1 }
                ], 
                hideHeaders: true,
                margin: '-1 0 0 0',
                viewConfig: {
                    loadingText: tr('Loading') + '...',
                    stripeRows: false
                }
            }); 

            this.vocabList.on( 'selectionchange', this.vocabChanged, this );

            this.vocPanel = Ext.create('Ext.Panel', { 
                layout: 'fit',
                region: 'west',
                split: true,
                width: 200,
                items: this.vocabList
            }); 

            this.cbId = Ext.create('Ext.form.field.Checkbox', {
                boxLabel: tr('Show category IDs'),
                style: 'color: #04408C',
                checked: false
            } );  

            this.cbId.on( 'change', this.updateVocab, this);          
        }

        var cfg = {
            layout: 'border',
            width: this.extendedMode?600:300,
            height: 400,
            modal: true,
            closeAction: 'hide',
            items: [ this.vocPanel, this.tree ],
            buttons: [
                this.cbId, '->', 
                this.selectConceptButton, 
                {
                    text: tr('Cancel'),
                    handler  : function(){
                        this.hide();
                },
                scope: this
            }]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.show(); // Needed for IE8. - FB
        if (this.extendedMode)
            this.vocabStore.loadPage(1);
        else {
            this.treeConceptStore.load({
                url: this.vocRestUrl + '/topConcepts?showIds=' + this.showIds + '&lang=' + this.lang
            });
        }
    },
    vocabChanged: function( model, selected ) {
        this.selectConceptButton.setDisabled(true);
        if (model.hasSelection()) {            
            this.vocUri = selected[0].getData().uri;
            this.treeConceptStore.load({
                url: selected[0].getData().restUrl + '/topConcepts?showIds=' + this.showIds + '&lang=' + this.lang
            }); 
        }
        else 
            this.treeConceptStore.setData([]);           
    },
    updateVocab: function(cb, value) {
        this.showIds = value;
        var model = this.vocabList.getSelectionModel();
        if (model.hasSelection())
            this.vocabChanged( model, model.getSelection() );
    }
} );


/*******************************/
/*** Identity related models ***/
/*******************************/

Ext.define('PersonModel', {
    extend: 'Ext.data.Model',
    fields: [ 'id', 'uri', 'label', 'restUrl' ]
});

Ext.define('OrganizationModel', {
    extend: 'Ext.data.Model',
    fields: [ 'id', 'uri', 'label', 'restUrl' ]
});


/**********************/
/*** IdentityPicker ***/
/**********************/


Ext.define( 'Comete.IdentityPicker', {
    extend: 'Ext.window.Window',
    initComponent: function( config ) {

        this.selectButton = Ext.create('Ext.button.Button', {
            text: tr('Select'),
            disabled: true,
            handler: function(){
                this.hide(); 
                var elem = this.identityFinder.getSelectedIdentity();
                this.aListener.setIdentity(elem.uri, elem.label);
            },             
            scope: this
        } );
 
            
        this.identityFinder = Ext.create('Comete.IdentityFinderPanel', { 
            type: this.type,
            region: 'center',
            border: false, 
            aListener: this
        }); 

        var cfg = {
            layout: 'border',
            width: 300,
            height: 400,
            modal: true,
            closeAction: 'hide',
            items: this.identityFinder,
            buttons: [
                this.selectButton, 
                {
                    text: tr('Cancel'),
                    handler  : function(){
                        this.hide();
                },
                scope: this
            }]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.show(); // Needed for IE8. - FB
    },
    ifpSelectionChanged: function(view, record) {
        this.selectButton.setDisabled(false);
    }
} );

Ext.define( 'Comete.IdentityFinderPanel', {
    extend: 'Ext.panel.Panel',
    layout: 'border',
    initComponent: function( config ) {

        this.proxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: (this.type == 'person')?'persons':'organizations',
                totalProperty: 'totalCount'
            }
        });
 
        this.store = Ext.create('Ext.data.JsonStore', {
            model: (this.type == 'person')?'PersonModel':'OrganizationModel',
            pageSize: 20,
            proxy: this.proxy
        });
        this.searchField = Ext.create('Ext.form.field.Text', {
            enableKeyEvents: true
        });

        this.searchField.on( 'keyup', this.retrieveIdentities, this );

        this.allButton = Ext.create('Ext.button.Button', {
            text: tr('All'),
            handler: function() { this.retrieveIdentities(true); },
            scope: this
        } );

        this.pageBar = Ext.create('Ext.toolbar.Paging', {
            store: this.store,
            firstText: tr('First Page'),
            prevText: tr('Previous Page'),
            nextText: tr('Next Page'),
            lastText: tr('Last Page'),
            refreshText: tr('Refresh'),
            afterPageText: tr('of {0}')
        } ),

        this.identityList = Ext.create('Ext.grid.Panel', { 
            margin: '0 10 10 10',
            store: this.store,
            bbar: this.pageBar,
            columns: [ 
                { dataIndex: 'label', text: (this.type == 'person')?tr('Persons'):tr('Organizations'), flex: 1 }
            ],     
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false
            }
        });

        this.identityList.on( 'selectionchange', function(view, record) {
            this.aListener.ifpSelectionChanged(view, record);
        }, this);

        cfg = {
            border: false,
            items: [ { layout: 'border', region: 'north', border: false, height: 25, margin: '10',
                       items: [ { layout: 'fit', region: 'center', border: false, items: this.searchField }, 
                                { region: 'east', margin: '0 0 0 5', border: false, items: this.allButton } ] }, 
                     { layout: 'fit', region: 'center', border: false, items: this.identityList} ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.store.sort('label', 'ASC');
    },
    retrieveIdentities: function(unuseText) {
        var text = this.searchField.getValue();
        var url = this.proxy.url = 'rest/' + ((this.type == 'person')?'persons':'organizations');
        if (unuseText != true && text != "")
            url = url + '/search?q=' + text;
        this.proxy.url = url;
        this.store.loadPage(1); 
        if (unuseText == true)
            this.searchField.setValue("");
    },
    getSelectedIdentity: function() {
        return this.identityList.getSelectionModel().getSelection()[0].getData(); 
    }
} );


/*******************/
/*** ImageButton ***/
/*******************/

Ext.define( 'Comete.ImageButton', {
    extend: 'Ext.Img',
    alias: 'widget.imagebutton',
    initComponent: function( config ) {
        var cfg = {
            border: false,
            src: this.isDisabled()?this.imgDisabled:this.img,
            listeners: {
                el: {
                    click: {
                        fn: function() {
                            if (!this.isDisabled())
                                this.handler.apply(this.scope);
                        },
                        scope: this
                    },
                    mouseover: {
                        fn: function(evt) { 
                            if (this.isDisabled()) 
                                Ext.get(evt.target).setStyle( 'cursor', '' ) 
                            else
                                Ext.get(evt.target).setStyle( 'cursor', 'pointer' ) 
 
                            if (!this.isDisabled() && this.imgOver != undefined)
                                this.setSrc(this.imgOver);
                        },
                        scope: this
                    },
                    mouseout: {
                        fn: function(evt) { 
                            if (this.isDisabled() && this.imgDisabled != undefined) 
                                this.setSrc(this.imgDisabled);
                            else
                                this.setSrc(this.img);
                        },
                        scope: this
                    }
                }
            }
        };

                
        if (this.tooltip != undefined) {
            this.on('render', function() {
                Ext.create('Ext.tip.ToolTip', {
                    target: this.getEl(),
                    html: this.tooltip
                });
            })
        }

        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    setDisabled: function(disabled) {
        if( disabled ) {
            this.disable();
            if (this.imgDisabled != undefined)
                this.setSrc(this.imgDisabled);
        } 
        else {
            this.enable();
            this.setSrc(this.img);
        }
    }
});



/**************************/
/*** RecordValidation   ***/
/**************************/

Ext.define( 'RecordValidationModel', {
    extend: 'Ext.data.Model',
    fields: [ 
        'id', 
        'LomStrict', 
        'LomLoose', 
        'LomFR', 
        'ScoLomFR_1_0', 
        'ScoLomFR_1_1', 
        'LomNormetic_1_2',
        'OAIDC',
        'repoUri',
        'repoName',
        'repoAdminEmail'
    ]
} );
