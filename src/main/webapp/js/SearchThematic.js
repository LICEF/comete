isUpdateConceptNeeded = true;

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
                root: 'concepts',
                concept: 'concept',
                label: 'label'
            }
        });

        this.vocConceptStore = Ext.create('Ext.data.JsonStore', {
            model: 'VocabularyConceptModel',
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
                        store.proxy.url = vocabularyUrl + '/rest/voc/search?lang=' + this.lang;
                        if (this.cbId.getValue())
                            store.proxy.url = store.proxy.url + '&showIds=true';
                    }, 
                    this );

        this.vocabularyCombo = Ext.create('Ext.form.field.ComboBox', {
            displayField: 'label',
            valueField: 'uri',
            store: this.vocabStore,
            editable: false,
            width: 400,
            emptyText: tr('Select classification') + '...',
            listConfig: {
                loadingText: tr('Loading') + '...'
            }
        });

        this.vocabularyCombo.on( 'select', this.vocabularySelected, this );

        this.vocabularyButton = Ext.create('Ext.button.Button', {
            icon: 'images/tree.gif',
            disabled: true,
            handler: this.pickVocConcept,             
            scope: this
        } );
                
        this.conceptSearchCombo = Ext.create('Ext.form.field.ComboBox', {
            displayField: 'label',
            valueField: 'uri',
            store: this.conceptStore,
            width: 450,
            hideTrigger: true,
            queryParam: 'q',
            emptyText: tr('Enter the desired category') + '...',
            listConfig: {
                loadingText: tr('Loading') + '...',
                emptyText: tr('No matching category found'),
                getInnerTpl: function() {
                    return '<div><font style="font-weight: bold; color: #04408C">{vocLabel}</font>' +
                                 '<img style="margin-bottom:-1px; margin-right:6px; margin-left:8px" src="images/blueArrow.gif"/>' +
                                 '{label}</div>';
                }

            }
        });

        this.expandConceptListButton = Ext.create('Ext.button.Button', {
            icon: 'images/downWhiteArrow.png',
            handler: function(){ this.conceptSearchCombo.expand() },             
            scope: this
        } );

        this.conceptSearchCombo.on( 'beforeselect', function(combo, record){ 
                        combo.collapse();
                        this.setVocConcept(record.data.uri, record.data.vocLabel, record.data.label, false, true);                        
                        return false;}, this );        

        var vocPanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',         
            border: false,
            margin: '0 0 6 0',
            items: [this.vocabularyCombo, { xtype: 'tbspacer', width: 4 }, this.vocabularyButton, { xtype: 'tbspacer', width: 30 },
                    { xtype: 'label', text:tr('OR'), margin: '2 0 0 0' }, { xtype: 'tbspacer', width: 30 }, this.conceptSearchCombo,
                    { xtype: 'tbspacer', width: 4 }, this.expandConceptListButton, { xtype: 'tbspacer', width: 4 },
                    { xtype: 'label', text:tr('(min. 4 characters)'), margin: '2 0 0 5' } ]
        } );

        this.breadcrumbBar = Ext.create('Comete.Breadcrumb', {
            store: this.vocConceptStore,
            lang: this.lang,
            width: '100%',
            subElementQuery: '/children',
            listener: this
        } );

        this.queryButton = Ext.create('Ext.button.Button', {
            text: tr('Search'),
            disabled: true,
            handler: function() {
                this.setRequestVocConcept(this.breadcrumbBar.getLastElement());
            },
            scope: this
        } );

        this.cbSubconcepts = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Include subcategories'),
            style: 'color: #04408C',
            margin: '3 0 0 0',
            checked: false            
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
            }
        } );

        this.cbId = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Show category IDs'),
            style: 'color: #04408C',
            checked: false            
        } );

        this.cbAutomaticQuery = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('Automatic Query'),
            style: 'color: #04408C',
            checked: true            
        } );

        this.cbSubconcepts.on( 'change', 
            function() {              
                if (this.cbAutomaticQuery.getValue() && this.currentVocConceptUri)
                    this.setRequestVocConcept(this.currentVocConceptUri);                   
            }, this );

        this.cbEquivalence.on( 'change', 
            function() {              
                this.equivalence.setVisible(this.cbEquivalence.getValue());   
                if (this.cbAutomaticQuery.getValue() && this.currentVocConceptUri) {
                    if (this.cbEquivalence.getValue() && this.equivalence.getValue().length == 0)
                        return;
                    this.setRequestVocConcept(this.currentVocConceptUri);           
                }       
            }, this );

        this.equivalence.store.on( 'load', this.manageVocsForEquivalence, this );

        this.cbId.on( 'change', 
            function(cb, value) {
                this.breadcrumbBar.showIDsConcepts(value);
                var uri = this.breadcrumbBar.getLastElement();
                if (uri != null)
                    this.breadcrumbBar.displayElement(uri);        
            }, this );

        this.cbAutomaticQuery.on( 'change', 
            function(cb, value) {
                this.breadcrumbBar.setAutomaticQuery(value);
            }, this );

        cfg = {
            layout: 'vbox',
            region: 'center',      
            margin: '0 10 0 10',
            items: [ vocPanel, this.breadcrumbBar, 
                     { layout: 'hbox',
                       width: '100%',
                       border: false,
                       items:[ { border: false,
                                 margin: '10 20 0 0',
                                 items: this.queryButton},
                               { layout: 'vbox', 
                                 border: false,
                                 margin: '5 10 0 0',
                                 items:[ this.cbSubconcepts, 
                                         { layout: 'hbox',
                                           border: false,
                                           items: [this.cbEquivalence, this.equivalence]
                                         } ] },                               
                               { xtype: 'tbfill' },
                               { xtype: 'fieldset',
                                 width: 200,
                                 title: 'Options',
                                 margin: '5 0 0 0',
                                 padding: '0 5 0 5',
                                 items:[ this.cbId, this.cbAutomaticQuery] }
                             ] 
                     } ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);   

        //init of vocabulary store for general consistency -AM
        this.vocabStore.load();     
    },
    vocabularySelected: function(combo, records) {
        searchManager.clear();
        this.breadcrumbBar.clear();
        this.currentVocRestUrl = records[0].getData().restUrl;
        this.vocConceptProxy.url = this.currentVocRestUrl + '/topConcepts?showIds=' +  this.cbId.getValue() + '&lang=' + this.lang;
        this.vocConceptStore.load();
        this.currentVocConceptUri = null;
        this.vocabularyButton.setDisabled(false);
        this.queryButton.setDisabled(true);
        this.cleanEquivalence();        
    },
    bcElementClicked: function(conceptUri) {
        this.queryButton.setDisabled(false);
        this.currentVocConceptUri = conceptUri;
        if (this.cbAutomaticQuery.getValue())
            this.setRequestVocConcept(conceptUri);
    },   
    setRequestVocConcept: function(conceptUri) {
        var query = [ { key: "vocConcept", value: conceptUri } ];
        if (this.cbSubconcepts.getValue())
            query[0].subConcepts = true;
        if (this.cbEquivalence.getValue()) {
            var fromVocs = this.equivalence.getValue();
            if (fromVocs.length > 0) {
                query[0].equivalent = true;
                query[0].fromVocs = fromVocs;
            }
        }
        searchManager.setRequestVocConcept2( query );
    },
    pickVocConcept: function() { 
        vocConceptPicker = Ext.create('Comete.VocConceptPicker', {
            vocRestUrl: this.currentVocRestUrl,
            showIds: this.cbId.getValue(),
            lang: this.lang,
            aListener: this   
        });        
        vocConceptPicker.show();
    },
    setVocConcept: function(conceptUri, vocLabel, conceptLabel, isLeaf, isQuery) {
        var vocUri = null;
        if (conceptUri.startsWith("http://dewey.info/class/"))
            vocUri = "http://dewey.info/scheme/ddc/";
        else {
            if (conceptUri.indexOf("#") != -1) //hash uri
                vocUri = conceptUri.substring(0, conceptUri.lastIndexOf("#"));
            else 
                vocUri = conceptUri.substring(0, conceptUri.lastIndexOf("/"));
        }
        var currentVoc = this.vocabularyCombo.getValue();
        this.vocabularyCombo.setValue(vocUri);
        var record = this.vocabularyCombo.findRecord("uri", vocUri);
        this.currentVocRestUrl = record.data.restUrl;
        this.vocabularyButton.setDisabled(false);
        this.breadcrumbBar.displayElement(conceptUri);
        this.queryButton.setDisabled(false);
        this.currentVocConceptUri = conceptUri;
        if (currentVoc == null || currentVoc != vocUri) {
            this.cbEquivalence.setValue(false);
            this.cleanEquivalence();  
        }
        if (isQuery && this.cbAutomaticQuery.getValue()) 
            this.setRequestVocConcept(conceptUri);        
    },
    setQuery: function(query) {
        this.currentVocConceptUri = null;        
        this.setVocConcept(query[0].value, null, null, null, false);
        this.cbSubconcepts.setValue( query[0].subConcepts == true );
        this.cbEquivalence.setValue( query[0].equivalent == true );
        if (this.equivalence.getValue())
            this.equivalence.setValue(query[0].fromVocs);    
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

