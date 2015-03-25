Ext.define( 'Comete.ThematicNavigationSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {
        
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
                root: 'concepts'
            }
        });

        this.vocConceptStore = Ext.create('Ext.data.TreeStore', {
            proxy: this.vocConceptProxy
        });                

        this.conceptProxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: 'concepts'
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
            width: 400,
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
            width: 400,
            hideTrigger: true,
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

        this.expandConceptListButton = Ext.create('Comete.ImageButton', {
            img: 'images/downArrow.png',
            imgDisabled: 'images/downArrowDisabled.png',
            margin: '4 0 0 0',
            handler: function(){ this.conceptSearchCombo.expand() },          
            scope: this,
            disabled: true
        } );

        this.conceptSearchCombo.on( 'beforeselect', function(combo, record){ 
            combo.collapse();
            this.setVocConcept(record.data.vocUri, record.data.uri, record.data.vocLabel, record.data.label, false, true);                        
            return false;}, this );

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
            width: '100%',
            subElementQuery: '/children',
            listener: this
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
            style: 'color: #04408C',
            margin: '3 0 0 0',
            checked: true
        } );

        this.cbEquivalence = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Include equivalent categories from :'),
            style: 'color: #04408C',
            margin: '3 0 0 0'
        } );

        this.equivalence = Ext.create('Ext.form.field.ComboBox', {
            displayField: 'label',
            valueField: 'uri',
            store: this.eqVocabStore,
            editable: false,
            multiSelect: true,
            margin: '4 0 0 10',
            hidden: true,
            width: 300,
            listConfig: {
                loadingText: tr('Loading') + '...'
            },
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
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
        

        cfg = {
            layout: 'vbox',
            region: 'center',
            margin: '0 10 0 10',
            items: [ vocPanel, 
                     this.breadcrumb,
                     { border: true, margin: '0 0 2 0', height: 1, width: '100%'}, 
                     { layout: 'hbox',
                       width: '100%',
                       border: false,
                       items:[ { layout: 'vbox', 
                                 border: false,
                                 items:[ this.cbSubconcepts, 
                                         { layout: 'hbox',
                                           border: false,
                                           items: [this.cbEquivalence, this.equivalence]
                                         } ] },
                                 { xtype: 'tbfill' },
                                 this.cbId
                             ] 
                     }, 
                     this.queryButton ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);

        //init of vocabulary store for general consistency -AM
        this.vocabStore.load();
    },
    vocabularySelected: function(combo, records) {        
        searchManager.clear();
        this.breadcrumb.clear();
        this.currentVocRestUrl = records[0].getData().restUrl;
        this.currentVocUri = records[0].getData().uri;
        this.vocConceptProxy.url = this.currentVocRestUrl + '/topConcepts?showIds=' + this.cbId.getValue() + '&lang=' + this.lang;        
        this.vocConceptStore.load();
        this.currentVocConceptUri = null;
        this.vocabularyButton.setDisabled(false);
        this.queryButton.setDisabled(true);
        this.cleanEquivalence();
    },
    bcElementClicked: function(conceptUri) {
        this.queryButton.setDisabled(false);
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
        searchManager.setRequestVocConcept2( query );
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
        this.currentVocRestUrl = record.data.restUrl;
        this.currentVocUri = vocUri;
        this.vocabularyButton.setDisabled(false);
        this.breadcrumb.displayElement(conceptUri);
        this.queryButton.setDisabled(false);
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
    setQueryNext: function(vocUri, query) {
        this.currentVocConceptUri = null;
        this.setVocConcept(vocUri, query[0].value, null, null, null, false);
        this.cbSubconcepts.setValue( query[0].subConcepts == true );
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
    }
} );

