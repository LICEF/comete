Ext.define( 'Comete.AdminVoc', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {
        this.isDirty = true;

        this.currentVocContextRestUrl = null;

        this.vocCtxtStore = Ext.create('Ext.data.JsonStore', {
            model: 'VocCtxtModel',                
            proxy: vocCtxtProxy
        });   
        this.vocCtxtStore.sort( 'label', 'ASC' );

        this.vocAliasProxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: 'vocAliases'
            }
        });
           
        this.vocAliasStore = Ext.create('Ext.data.JsonStore', {
            model: 'VocAliasModel',                
            proxy: this.vocAliasProxy
        });

        this.vocAliasStore.on( 'load', function() { this.aliasesCount = this.vocAliasStore.getCount(); }, this );
 
        this.addButton = Ext.create('Ext.button.Button', {
            text: tr('Add'),
            handler: this.addVocabulary,
            scope: this
        } );

        this.modifyButton = Ext.create('Ext.button.Button', {
            text: tr('Modify'),
            disabled: true,
            handler: this.modifyVocabulary, 
            scope: this
        } );

        this.deleteButton = Ext.create('Ext.button.Button', {
            text: tr('Delete'),
            disabled: true,
            handler: this.deleteVocabulary, 
            scope: this
        } );

        this.vocabList = Ext.create('Ext.grid.Panel', { 
            store: this.vocCtxtStore,
            margin: '0 20 10 10',                
            columns: [ 
                { dataIndex: 'label', text: tr('Vocabularies'), flex: 1, height: 28 }
            ],     
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
            },
            autoScroll: true,
            bbar: [ this.addButton, this.modifyButton, this.deleteButton ]
        });

        this.vocabList.on( 'selectionchange', this.vocabChanged, this );

        this.vocPanel = Ext.create('Ext.Panel', { 
            layout: 'fit',
            region: 'center',
            border: false,
            items: this.vocabList
        });

        this.leftPanel = Ext.create('Ext.Panel', { 
            layout: 'border',
            width: 400,
            region: 'west',     
            border: false,
            split: true,
            items: this.vocPanel
        }); 

        this.cbNavigable = Ext.create('Ext.form.field.Checkbox', {
            fieldLabel: 'Navigable',
            checked: false            
        } );

        this.cbNavigable.on( 'change', this.setNavigable, this );

        this.detailsPanel = Ext.create('Ext.Panel', { 
            region: 'center',
            width: 600,
            margin: '0 0 0 10',
            border: false,
            layout: 'form',
            defaultType: 'textfield',
            items: [ { fieldLabel: 'ID', editable: false }, 
                     { fieldLabel: 'URI', editable: false},
                     { fieldLabel: tr('Source Location'), editable: false },
                     { fieldLabel: tr('Concept URI prefix'), editable: false },
                     { fieldLabel: tr('Concept URI suffix'), editable: false },
                     { fieldLabel: tr('Linking predicate'), editable: false },
                     this.cbNavigable ]        
        }); 

        this.deleteAliasButton = Ext.create('Ext.button.Button', {
            text: tr('Delete'),
            disabled: true,
            handler: this.deleteAlias,
            scope: this
        } );

        var cellEditing = Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 2
        });

        this.aliases = Ext.create('Ext.grid.Panel', { 
            store: this.vocAliasStore,
            height: 150,  
            width: 475,  
            margin: '5 0 0 0',    
            columns: [ 
                { dataIndex: 'alias', width: '99%',
                  editor: {
                      allowBlank: false
                  }
                }
            ],     
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false
            },
            hideHeaders: true,
            autoScroll: true,
            plugins: [ cellEditing ],
            bbar: [ { text: tr('Add'),
                      handler: function() {
                          var alias = Ext.create('VocAliasModel', { alias: tr('New Alias') });
                          this.aliasesCount = this.vocAliasStore.getCount();
                          this.vocAliasStore.add(alias);                          
                          cellEditing.startEditByPosition({row: this.vocAliasStore.getCount()-1, column: 0});
                      }, scope: this}, 
                      this.deleteAliasButton
                  ]            
        }); 


        this.aliases.on( 'selectionchange', function(){ this.deleteAliasButton.setDisabled(false); }, this );

        this.aliases.on( 'edit', function(editor, e) { 
            var isNewAlias = (this.vocAliasStore.getCount() > this.aliasesCount);
            var methodType = 'POST';
            var params = {
                alias: e.value
            };
            if (!isNewAlias) {
                if (e.value == e.originalValue)
                    return;
                methodType = 'PUT';
                params.prevAlias = e.originalValue;
            }
            Ext.Ajax.request( {
                url: this.currentVocContextRestUrl + '/aliases',
                params: params,
                method: methodType,
                success: function(response, opts) {
                    this.aliasesCount = this.vocAliasStore.getCount();  
                },
                failure: function(response, opts) {
                    Ext.Msg.alert('Failure', response.responseText);  
                },
                scope: this
            } ); 
        }, this );

        this.aliasPanel = Ext.create('Ext.Panel', { 
            region: 'south',
            width: 600,
            margin: '4 0 0 10',
            border: false,
            layout: 'hbox',
            items: [ { xtype: 'label', text: tr('Aliases') + ":", width: 115, margin: '5 0 0 5' },                      
                     this.aliases ]
        }); 

        this.centerPanel = Ext.create('Ext.Panel', { 
            region: 'center',
            border: false,
            items: [ this.detailsPanel, this.aliasPanel ]        
        }); 

        var cfg = {
            layout: 'border',
            region: 'center',      
            items: [ this.leftPanel, this.centerPanel ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    vocabChanged: function( model, selected ) {
        this.currentVocContextRestUrl = null;
        if (selected.length == 1) {
            this.currentVocContextRestUrl = selected[0].getData().restUrl;

            //fields update
            Ext.Ajax.request( {
                url: this.currentVocContextRestUrl + '/details',
                method: 'GET',
                success: function(response) {
                    this.initDisplay = true;
                    var jsonDetails = Ext.JSON.decode(response.responseText, true);
                    this.detailsPanel.getComponent(0).setValue(jsonDetails.id);
                    this.detailsPanel.getComponent(1).setValue(jsonDetails.uri);
                    this.detailsPanel.getComponent(2).setValue(jsonDetails.location);
                    this.detailsPanel.getComponent(3).setValue(jsonDetails.uriPrefix);
                    this.detailsPanel.getComponent(4).setValue(jsonDetails.uriSuffix);
                    this.detailsPanel.getComponent(5).setValue(jsonDetails.linkingPredicate);
                    this.detailsPanel.getComponent(6).setValue(jsonDetails.navigable);
                    this.initDisplay = false;
                },
                scope: this 
            } );

            //aliases update
            this.vocAliasProxy.url = this.currentVocContextRestUrl + "/aliases";        
            this.vocAliasStore.load();   
            //buttons
            this.modifyButton.setDisabled(false);
            this.deleteButton.setDisabled(false);
        }
        else {
            this.detailsPanel.getComponent(0).setValue("");
            this.detailsPanel.getComponent(1).setValue("");
            this.detailsPanel.getComponent(2).setValue("");
            this.detailsPanel.getComponent(3).setValue("");
            this.detailsPanel.getComponent(4).setValue("");
            this.detailsPanel.getComponent(5).setValue("");
            this.detailsPanel.getComponent(6).setValue("");
            this.vocAliasStore.removeAll();
            //buttons
            this.modifyButton.setDisabled(true);     
            this.deleteButton.setDisabled(true);     
        }
        this.deleteAliasButton.setDisabled(true);  
    },
    addVocabulary: function() {
        var editor = Ext.create('Comete.AdminVocEditor', {
            width: 500,
            height: 270,
            modal: true,
            listener: this
        });
        editor.show();       
    },
    afterAdd: function() {
        this.vocCtxtStore.loadPage(1);
        Ext.Msg.alert('Information', tr('Vocabulary added.'));
    },
    modifyVocabulary: function() {
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/used',
            method: 'GET',
            success: function(response) {
                if (response.responseText == 'false')
                    this.modifyVocabularyStep2(null);
                else {
                    var promptBox = Ext.Msg;
                    promptBox.buttonText = { cancel: tr("Cancel") };
                    promptBox.show({
                        msg: tr('This vocabulary is used.<br>Do you really want to modify it ?'),
                        buttons: Ext.Msg.OKCANCEL,
                        icon: Ext.Msg.QUESTION,
                        fn: this.modifyVocabularyStep2,
                        scope: this
                    });
                }
            },
            scope: this 
        } );      
    },
    modifyVocabularyStep2: function(button) {
        if (button != null && button != 'ok')
            return;
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/details',
            method: 'GET',
            success: function(response) {
                var jsonDetails = Ext.JSON.decode(response.responseText, true);
                this.modifyVocabularyStep3(jsonDetails)
            },
            scope: this 
        } );      
    },
    modifyVocabularyStep3: function(values) {
        var editor = Ext.create('Comete.AdminVocEditor', {
            width: 500,
            height: 270,
            modal: true,
            mode: 'modify',
            restUrl: this.currentVocContextRestUrl,
            values: values,
            listener: this            
        });
        editor.show();
    },
    afterModify: function() {
        this.vocCtxtStore.loadPage(1);
        Ext.Msg.alert('Information', tr('Vocabulary modified.'));
    },
    deleteVocabulary: function() {
        var records = this.vocabList.getSelectionModel().getSelection();
        if (records.length == 0)
            return;
        
        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            msg: tr('Do you really want to delete vocabulary ?'),
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.QUESTION,
            fn: this.deleteVocabularyEff,
            scope: this
        });
    },
    deleteVocabularyEff: function(button) {
        if (button != 'ok')
            return;
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl,
            method: 'DELETE',
            success: function(response, opts) {
                waitDialog.close();
                this.vocCtxtStore.loadPage(1);
                Ext.Msg.alert('Information', tr('Vocabulary deleted.'));
            },
            failure: function(response, opts) {
                waitDialog.close();
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );
    },
    deleteAlias: function() {
        var records = this.aliases.getSelectionModel().getSelection();
        if (records.length == 0)
            return;
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/aliases',
            params: { alias: records[0].data.alias },
            method: 'DELETE',
            success: function(response, opts) {
                this.vocAliasStore.remove(records[0]);
                this.deleteAliasButton.setDisabled(true);
                this.aliasesCount = this.vocAliasStore.getCount();     
            },
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );
    },
    setNavigable: function(cb, value) {
        if (this.initDisplay)
            return;

        if (this.currentVocContextRestUrl == null)
            return;
        methodType = 'POST';
        if (!value) 
            methodType = 'DELETE';
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/navigable',
            method: methodType,
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText);  
            }
        } );
    },
    updateData: function() {
        if( this.isDirty ) {
            this.vocCtxtStore.loadPage(1);
            this.isDirty = false;
        }
    }
} );

Ext.define( 'Comete.AdminVocEditor', {
    extend: 'Ext.window.Window',
    layout: 'fit',           
    initComponent: function( config ) {
        this.urlLocation = Ext.create('Ext.form.TextField', {
            name: 'url',
            labelWidth: 130,
            fieldLabel: tr('External voc. (URL)')
        });

        this.fileLocation = Ext.create('Ext.form.field.File', {
            name: 'file',
            fieldLabel: tr('OR') + ' ' + tr('local voc. (file)'),
            labelWidth: 130,
            buttonText: '...'
        });

        //must select uploadfield twice if urlfield is not empty. 
        //Best I can do cause uploadfield reset doesn't works properly... (known bug) -AM
        this.urlLocation.on('change', function() { 
            this.fileLocation.emptyText = " ";
            this.fileLocation.reset();
        }, this ); 
        this.fileLocation.on('change', function() { this.urlLocation.setValue(""); }, this ); 

        this.formPanel = Ext.create('Ext.form.Panel', { 
            border: false,
            margin: '10',
            layout: 'form',
            defaultType: 'textfield',
            items: [ { name: 'id', fieldLabel: 'ID', editable: this.mode != 'modify' },
                     this.urlLocation, 
                     this.fileLocation, 
                     { name: 'uriPrefix', fieldLabel: tr('Concept URI prefix'), emptyText: tr('Optional field') },
                     { name: 'uriSuffix', fieldLabel: tr('Concept URI suffix'), emptyText: tr('Optional field') },
                     { name: 'linkingPredicate', fieldLabel: tr('Linking predicate'), emptyText: tr('Optional field') } ]
        }); 


        cfg = {
            title: (this.mode == 'modify')?tr('Modify vocabulary'):tr('Add vocabulary'),
            buttons: [ {text:'OK', handler: this.ok, scope: this}, {text:tr('Cancel'), handler: this.close, scope: this}],
            items: [ { border: false, items: this.formPanel } ]
                     
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

       if (this.values)
           this.setValues();
    },
    ok: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        this.formPanel.submit({
            url: ((this.mode == 'modify')?this.restUrl:'rest/vocContexts'),
            method: 'POST',
            success: function(form, action) {
                this.close();     
                waitDialog.close();
                if (this.mode == 'modify') 
                    this.listener.afterModify();          
                else
                    this.listener.afterAdd();
            },
            failure: function(form, action) { 
                Ext.Msg.alert('Failure', action.result.error);            
                waitDialog.close();   
            },
            submitEmptyText: false,
            scope: this
        });
    },
    setValues: function() {
        this.formPanel.getComponent(0).setValue(this.values.id);
        var initLocation = this.values.location;
        if (initLocation.startsWith('http'))
            this.urlLocation.setValue(initLocation);
        else
            this.fileLocation.emptyText = initLocation;
        this.formPanel.getComponent(3).setValue(this.values.uriPrefix);
        this.formPanel.getComponent(3).setValue(this.values.uriPrefix);
        this.formPanel.getComponent(4).setValue(this.values.uriSuffix);
        this.formPanel.getComponent(5).setValue(this.values.linkingPredicate);
    }        
});


