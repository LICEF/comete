Ext.define( 'Comete.ThematicNavigationSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {

        this.simpleButton = Ext.create('Ext.Component', {            
            html: '<div class="whiteLink"><a href="javascript:changeCardItem(0);">' + tr( 'Quick Search' ) + '</a></div>'
        } );

        this.advancedButton = Ext.create('Ext.Component', {
            html: '<div class="whiteLink"><a href="javascript:changeCardItem(1);">' + tr( 'Advanced Search' ) + '</a></div>'
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
        
        this.currentVocConceptUri = null;
        this.currentVocRestUrl = null;
        
        this.vocabStore = Ext.create('Ext.data.JsonStore', {
            model: 'VocabModel',
            proxy: vocabProxy
        });        

        this.eqVocabStore = Ext.create('Ext.data.JsonStore', {
            model: 'VocabModel',
            proxy: vocabProxy
        });        

        this.vocConceptProxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                rootProperty: 'concepts'
            }
        });

        this.vocConceptStore = Ext.create('Ext.data.TreeStore', {
            proxy: this.vocConceptProxy
        });                

        this.conceptProxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                rootProperty: 'concepts'
            }
        });

        this.conceptStore = Ext.create('Ext.data.JsonStore', {
            model: 'ConceptModel',
            proxy: this.conceptProxy
        });        

        this.conceptStore.on( 'beforeload', function(store) {
            store.proxy.url = 'rest/voc/search?lang=' + this.lang + (this.cbId.getValue()?'&showIds=true':'');
        }, this );

        this.conceptStore.on( 'load', function(store){
            this.expandConceptListButton.setDisabled(store.getCount() == 0);
              
        }, this );

        this.vocabularyCombo = Ext.create('Ext.form.field.ComboBox', {
            displayField: 'label',
            valueField: 'uri',
            store: this.vocabStore,
            editable: false,
            width: CARDPANEL_WIDTH + 24,
            margin: '10 20 0 20',
            emptyText: tr('Select classification') + '...',
            listConfig: {
                loadingText: tr('Loading') + '...'
            },
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
        });

        this.vocabularyCombo.on( 'select', this.vocabularySelected, this ); 

        this.vocabularyButton = Ext.create('Comete.ImageButton', {
            img: 'images/tree.gif',
            imgDisabled: 'images/treeDisabled.gif',
            margin: '4 0 0 0',
            handler: this.pickVocConcept,
            disabled: true,
            scope: this
        } );       
                
        this.conceptSearchCombo = Ext.create('Ext.form.field.ComboBox', {
            displayField: 'label',
            valueField: 'uri',
            store: this.conceptStore,
            width: CARDPANEL_WIDTH + 24,
            margin: '15 20 0 20',
            queryParam: 'q',
            emptyText: tr('Enter the desired category') + '... ' + tr('(min. 4 characters)'),
            listConfig: {
                loadingText: tr('Loading') + '...',
                emptyText: tr('No matching category found'),
                tpl: '<div><tpl for="."><div class="x-boundlist-item">{vocLabel}' +
                                 '<img style="margin-bottom:-2px; margin-right:6px; margin-left:4px" src="images/split-arrow-tiny.png"/>' +
                                 '{label}</div></tpl></div>'
            }
        });

        this.conceptSearchCombo.on( 'beforeselect', function(combo, record){ 
            combo.collapse();
            this.setVocConcept(record.data.vocUri, record.data.uri, record.data.vocLabel, record.data.label, false, true);                        
            return false;}, this );

        this.conceptSearchCombo.on( 'beforequery', function(queryPlan){ 
           if (queryPlan.query == "") {
               queryPlan.combo.expand();
               return false;
           }
        }, this );        

        this.expandConceptListButton = Ext.create('Comete.ImageButton', {
            img: 'images/downArrow.png',
            imgDisabled: 'images/downArrowDisabled.png',
            margin: '4 0 0 0',
            handler: function(){ this.conceptSearchCombo.expand() },          
            scope: this,
            disabled: true
        } );

        var vocPanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            margin: '0 0 6 0',
            items: [this.vocabularyCombo, { xtype: 'tbspacer', width: 4 }, this.vocabularyButton, { xtype: 'tbspacer', width: 30 },
                    { xtype: 'label', text:tr('OR'), margin: '2 0 0 0' }, { xtype: 'tbspacer', width: 30 }, this.conceptSearchCombo,
                    { xtype: 'tbspacer', width: 4 }, this.expandConceptListButton ]
        } );

        this.breadcrumb = Ext.create('Comete.Breadcrumb', {
            store: this.vocConceptStore,
            lang: this.lang,
            border: false,
            width: CARDPANEL_WIDTH + 24,
            margin: '15 20 0 20',
            subElementQuery: '/children',
            listener: this,
            hidden: true
        } );

        this.queryButton = Ext.create('Ext.button.Button', {
            text: tr('Search'),
            disabled: true,
            margin: '8 0 0 0',
            handler: function() {
                this.setRequestVocConcept(this.breadcrumb.getLastElement());
            },
            scope: this
        } );

        this.cbSubconcepts = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Include subcategories'),
            style: 'color: white',
            margin: '3 0 0 0',
            checked: true
        } );

        this.cbEquivalence = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Include equivalent categories from'),
            hidden: true
        } );

        this.cbEquivalence.on( 'change', 
            function() {              
                this.equivalence.setVisible(this.cbEquivalence.getValue());   
                this.colonLabel.setText(this.cbEquivalence.getValue()?':':'...');

                if (this.currentVocConceptUri) {
                    if (this.cbEquivalence.getValue() && this.equivalence.getValue().length == 0)
                        return;
                    this.redoRequest();           
                }       
            }, this );


        this.colonLabel = Ext.create('Ext.form.Label', {
            text: '...',
            margin: '4 5 0 5',
            hidden: true
        } );

        this.equivalence = Ext.create('Ext.form.field.ComboBox', {
            displayField: 'label',
            valueField: 'uri',
            store: this.eqVocabStore,
            editable: false,
            multiSelect: true,
            hidden: true,
            width: 200,
            listConfig: {
                loadingText: tr('Loading') + '...'
            },
            tpl: '<div><tpl for="."><div class="x-boundlist-item"><img src="' + Ext.BLANK_IMAGE_URL + '" class="chkCombo"/>' +
                 '<span style="vertical-align: top">{label}</span></div></tpl></div>'
        } );

        this.cbId = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Show category IDs'),
            style: 'color: #04408C',
            checked: false
        } );

        this.cbSubconcepts.on( 'change', 
            function() {
                if (this.currentVocConceptUri)
                    this.setRequestVocConcept(this.currentVocConceptUri);                   
            }, this );

        this.cbEquivalence.on( 'change', 
            function() {
                this.equivalence.setVisible(this.cbEquivalence.getValue());   
                if (this.currentVocConceptUri) {
                    if (this.cbEquivalence.getValue() && this.equivalence.getValue().length == 0)
                        return;
                    this.setRequestVocConcept(this.currentVocConceptUri);
                }       
            }, this );

        this.equivalence.store.on( 'load', this.manageVocsForEquivalence, this );

        this.cbId.on( 'change', 
            function(cb, value) {
                this.breadcrumb.showIDsConcepts(value);
                var uri = this.breadcrumb.getLastElement();
                this.breadcrumb.displayElement(uri);
            }, this );
        
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
            height: THEMATIC_HEIGHT,
            cls: 'searchPanel',
            region: 'north',
            items: [ 
                     { layout: 'hbox', cls: 'transp2', border: false, margin: '10 20 0 20', width: CARDPANEL_WIDTH + 24,
                       items: [ { xtype: 'label', text: tr('THEMATIC NAVIGATION'), 
                                     style: { fontWeight: 'bold', fontSize: '14px' } }, 
                                { xtype: 'tbspacer', width: 5 }, this.info,
                                { xtype: 'tbfill' }, this.simpleButton, 
                                { xtype: 'tbspacer', width: 15 }, this.advancedButton, { xtype: 'tbspacer', width: 1 } ] },
                     this.conceptSearchCombo,
                     //{ border: false, margin: '10 20 0 20', html: '<b>- ' + tr('OR') + ' -</b>' },
                     this.vocabularyCombo,
                     this.breadcrumb,
                     { xtype: 'tbfill' },
                     { layout: 'hbox', cls: 'transp2', border: false, margin: '10 20 0 20', width: CARDPANEL_WIDTH + 24, 
                       items: [ this.cbEquivalence, this.colonLabel, this.equivalence, { xtype: 'tbfill' }, 
                                this.goBackwardQueryButton, { xtype: 'tbspacer', width: 4 }, this.goForwardQueryButton ] },                     
                     { xtype: 'tbspacer', height: 10 }
                   ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);

        //init of vocabulary store for general consistency -AM
        this.vocabStore.load();
    },
    info: function() {
        if (this.infoWindow != undefined)
            this.infoWindow.close();

        this.infoWindow = new Ext.window.Window( {
            title: tr('Thematic Navigation'),
            width: 450,
            height: 450,
            resizable: false,
            html: '<iframe width="100%" height="100%" frameborder="0" src="' + this.lang + '/thematicSearchInfo.html"></iframe>' 
        } );
        this.infoWindow.show();
    },
    vocabularySelected: function(combo, records) {        
        searchManager.clear();

        this.showHiddenComponentsAtInit();

        this.breadcrumb.clear();
        this.currentVocRestUrl = records.getData().restUrl;
        this.currentVocUri = records.getData().uri;
        //this.vocConceptProxy.url = this.currentVocRestUrl + '/topConcepts?showIds=' + this.cbId.getValue() + '&lang=' + this.lang;        
        this.vocConceptProxy.url = this.currentVocRestUrl + '/topConcepts?lang=' + this.lang; //for ceres
        this.vocConceptStore.load();
        this.currentVocConceptUri = null;
        //this.vocabularyButton.setDisabled(false);
        //this.queryButton.setDisabled(true);
        this.cleanEquivalence();
    },
    bcElementClicked: function(conceptUri) {
        //this.queryButton.setDisabled(false);
        this.currentVocConceptUri = conceptUri;
        this.setRequestVocConcept(conceptUri);
    },
    setRequestVocConcept: function(conceptUri) {
        var query = [ { key: "vocConcept", value: conceptUri } ];
        if (this.cbSubconcepts.getValue())
            query[0].subConcepts = true;
        if (this.cbEquivalence.getValue()) {
            var eqVocs = this.equivalence.getValue();
            if (eqVocs.length > 0) {
                query[0].equivalent = true;
                query[0].eqVocs = eqVocs;
            }
        }
        //searchManager.setRequestVocConcept2( query );
        searchManager.setRequestVocConcept3( query );
    },
    pickVocConcept: function() { 
        vocConceptPicker = Ext.create('Comete.VocConceptPicker', {
            vocRestUrl: this.currentVocRestUrl,
            vocUri: this.currentVocUri,
            showIds: this.cbId.getValue(),
            lang: this.lang,
            aListener: this   
        });        
        vocConceptPicker.show();
    },
    setVocConcept: function(vocUri, conceptUri, vocLabel, conceptLabel, isLeaf, isQuery) {     
        var currentVoc = this.vocabularyCombo.getValue();
        this.vocabularyCombo.setValue(vocUri);
        var record = this.vocabularyCombo.findRecord("uri", vocUri);
        this.currentVocRestUrl = record.getData().restUrl;
        this.currentVocUri = vocUri;
        //this.vocabularyButton.setDisabled(false);
        this.breadcrumb.displayElement(conceptUri);
        //this.queryButton.setDisabled(false);
        this.showHiddenComponentsAtInit();
        this.currentVocConceptUri = conceptUri;
        if (currentVoc == null || currentVoc != vocUri) {
            this.cbEquivalence.setValue(false);
            this.cleanEquivalence();  
        }
        if (isQuery) 
            this.setRequestVocConcept(conceptUri);        
    },
    setQuery: function(query) {
        var conceptUri = query[0].value;
        Ext.Ajax.request( {
            url: 'rest/voc/' + encodeURIComponent( conceptUri ) + '/scheme',
            method: 'GET',
            success: function(response) {
                this.setQueryNext(Ext.JSON.decode(response.responseText, true).scheme, query);
            },
            scope: this 
        } );
    },
    redoRequest: function() {
        this.setRequestVocConcept(this.currentVocConceptUri);
    },
    setQueryNext: function(vocUri, query) {
        this.currentVocConceptUri = null;
        this.setVocConcept(vocUri, query[0].value, null, null, null, false);
        //this.cbSubconcepts.setValue( query[0].subConcepts == true );
        this.cbEquivalence.setValue( query[0].equivalent == true );
        if (this.equivalence.getValue())
            this.equivalence.setValue(query[0].eqVocs);
    },
    cleanEquivalence: function() {
        this.equivalence.clearValue();
        this.equivalence.getStore().load();
    },
    manageVocsForEquivalence: function(query) {
        var record = this.vocabularyCombo.getValue();
        var eqRecord = this.equivalence.findRecordByValue(record);
        this.equivalence.getStore().remove(eqRecord);
    },
    goBackwardQuery: function() {
        if (!this.goBackwardQueryButton.isDisabled()) 
            window.searchManager.goBackwardQuery();
    },
    goForwardQuery: function() {
        if (!this.goForwardQueryButton.isDisabled()) 
            window.searchManager.goForwardQuery();
    },
    fadeInHistoryButtons: function(isBackwardButtonDisabled, isForwardButtonDisabled) {
        this.goBackwardQueryButton.getEl().fadeIn({
            duration: 1500
        });      
        this.goForwardQueryButton.getEl().fadeIn({
            duration: 1500
        });      
    },
    updateQueryHistoryButtons: function(isBackwardButtonDisabled, isForwardButtonDisabled) {        
        this.goBackwardQueryButton.getEl().setOpacity(1);
        this.goForwardQueryButton.getEl().setOpacity(1)
        this.goBackwardQueryButton.setDisabled(isBackwardButtonDisabled);
        this.goForwardQueryButton.setDisabled(isForwardButtonDisabled);
    },
    showHiddenComponentsAtInit: function() {
        if (this.breadcrumb.isHidden()) {
            this.breadcrumb.setVisible(true);    
            this.updateQueryHistoryButtons(true, true);
            this.setHeight(190);
        }
        if (this.cbEquivalence.isHidden()) {
            this.cbEquivalence.setVisible(true);
            this.colonLabel.setVisible(true);
        }
    },
    adjustWidth: function(elementsWidth) {
        var maxWidth = CARDPANEL_WIDTH + 24;
        maxWidth = Math.max(CARDPANEL_WIDTH + 24, elementsWidth + 25);
        
        if (this.breadcrumb.getWidth() != maxWidth) {
            this.breadcrumb.setWidth(maxWidth);

            this.conceptSearchCombo.setWidth(maxWidth);
            this.vocabularyCombo.setWidth(maxWidth);

            //top and bottom menubar
            this.getComponent(0).setWidth(maxWidth);
            this.getComponent(5).setWidth(maxWidth);
        }
        
    }
} );

