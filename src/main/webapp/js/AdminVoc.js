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
            disabled: !authorized,
            handler: this.addVocabulary, 
            scope: this
        } );

        this.modifyButton = Ext.create('Ext.button.Button', {
            text: tr('Modify'),
            disabled: true,
            handler: this.modifyVocabularyContent, 
            scope: this
        } );

        this.deleteButton = Ext.create('Ext.button.Button', {
            text: tr('Delete'),
            disabled: true,
            handler: this.deleteVocabulary, 
            scope: this
        } );


        this.updateButton = Ext.create('Ext.button.Button', {
            text: tr('Update'),
            disabled: true,
            handler: this.updateVocabulary, 
            scope: this
        } );
        
        function updateIcon(val, cellmetadata, record, row) {
            var icon = record.get('update') == true?'exclamationMark.png':'validMark.png';
            return '<center><img src="images/' + icon + '"/><center>';
        }

        this.vocabList = Ext.create('Ext.grid.Panel', { 
            store: this.vocCtxtStore,
            margin: '0 20 10 10',                
            columns: [ 
                { dataIndex: 'label', text: tr('Vocabularies'), flex: 1, height: 28 },
                { dataIndex: 'update', text: tr('Mises à jour'), hidden: true, renderer: updateIcon }
            ],     
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
            },
            autoScroll: true,
            bbar: [ this.addButton, this.modifyButton, this.deleteButton, '->', this.updateButton ]
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
            width: 500,
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
            items: [ { fieldLabel: 'URI'}
                     , { fieldLabel: 'Source' }
                     , { fieldLabel: tr('Source Location') }
                     , { fieldLabel: tr('Graph') }
                     , this.cbNavigable ]        
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
            width: 485,  
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
                      disabled: !authorized,
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
            items: [ { xtype: 'label', text: tr('Aliases') + ":", width: 105, margin: '5 0 0 5' },                      
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
                    var jsonDetails = Ext.JSON.decode(response.responseText, true).vocDetails[0];
                    this.detailsPanel.getComponent(0).setValue(jsonDetails.uri);
                    this.detailsPanel.getComponent(1).setValue(jsonDetails.source);
                    this.detailsPanel.getComponent(2).setValue(jsonDetails.location);
                    this.detailsPanel.getComponent(3).setValue(jsonDetails.graph);
                    this.detailsPanel.getComponent(4).setValue(jsonDetails.navigable);
                    this.initDisplay = false;
                    if (authorized)
                        this.modifyButton.setDisabled(jsonDetails.location.startsWith('http'));     
                },
                scope: this 
            } );

            //aliases update
            this.vocAliasProxy.url = this.currentVocContextRestUrl + "/aliases";        
            this.vocAliasStore.load();   
            //buttons
            if (authorized) {  
                this.deleteButton.setDisabled(false);     
                this.updateButton.setDisabled(false);     
            }
        }
        else {
            this.detailsPanel.getComponent(0).setValue("");
            this.detailsPanel.getComponent(1).setValue("");
            this.detailsPanel.getComponent(2).setValue("");
            this.detailsPanel.getComponent(3).setValue("");
            this.detailsPanel.getComponent(4).setValue("");
            this.vocAliasStore.removeAll();
            //buttons
            this.modifyButton.setDisabled(true);     
            this.deleteButton.setDisabled(true);     
            this.updateButton.setDisabled(true);     
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
    modifyVocabularyContent: function() {
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/used',
            method: 'GET',
            success: function(response) {
                if (response.responseText == 'false')
                    this.modifyVocabularyContentStep2(null);
                else {
                    var promptBox = Ext.Msg;
                    promptBox.buttonText = { cancel: tr("Cancel") };
                    promptBox.show({
                        msg: tr('This vocabulary is used.<br>Do you really want to modify it ?'),
                        buttons: Ext.Msg.OKCANCEL,
                        icon: Ext.Msg.QUESTION,
                        fn: this.modifyVocabularyContentStep2,
                        scope: this
                    });
                }
            },
            scope: this 
        } );      
    },
    modifyVocabularyContentStep2: function(button) {
        if (button != null && button != 'ok')
            return;
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/details',
            method: 'GET',
            success: function(response) {
                var jsonDetails = Ext.JSON.decode(response.responseText, true).vocDetails[0];
                this.modifyVocabularyContentStep3(jsonDetails.uri, jsonDetails.location)
            },
            scope: this 
        } );      
    },
    modifyVocabularyContentStep3: function(vocUri, location) {
        var editor = Ext.create('Comete.AdminVocUploader', {
            width: 500,
            height: 170,
            modal: true,
            restUrl: this.currentVocContextRestUrl,
            uri: vocUri,
            location: location,
            listener: this
        });
        editor.show();       
    },
    afterModify: function() {
        this.vocCtxtStore.loadPage(1);
        Ext.Msg.alert('Information', tr('Vocabulary modified.'));
    },
    updateVocabulary: function() {
        var records = this.vocabList.getSelectionModel().getSelection();
        if (records.length == 0)
            return;
        
        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            msg: tr('Do you really want to update vocabulary ?'),
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.QUESTION,
            fn: this.updateVocabularyEff,
            scope: this
        });

    },
    updateVocabularyEff: function(button) {
        if (button != 'ok')
            return;
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/update',
            method: 'GET',
            success: function(response, opts) {
                waitDialog.close();
                var selection = this.vocabList.getSelectionModel().getSelection();
                selection[0].set('update', false);
                selection[0].commit(); //probably not good use of commit but avoid mark cell as dirty -AM
                this.vocabChanged(null, selection);
                Ext.Msg.alert('Information', tr('Vocabulary updated.'));
            },
            failure: function(response, opts) {
                waitDialog.close();
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );
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
        this.urlLocation.on('change', function() { this.fileLocation.reset(); }, this ); 
        this.fileLocation.on('change', function() { this.urlLocation.setValue(""); }, this ); 

        this.formPanel = Ext.create('Ext.form.Panel', { 
            border: false,
            margin: '10',
            layout: 'form',
            defaultType: 'textfield',
            items: [ { name: 'name', fieldLabel: tr('Name'), allowBlank: false },
                     { name: 'source', fieldLabel: 'Source', allowBlank: false },
                     { name: 'category', fieldLabel: tr('Category'), allowBlank: false },
                     this.urlLocation, 
                     this.fileLocation, 
                     { name: 'navigable', xtype: 'checkbox', fieldLabel: 'Navigable ?' }]        
        }); 


        cfg = {
            title: tr('Add vocabulary'),
            buttons: [ {text:'OK', handler: this.ok, scope: this}, {text:tr('Cancel'), handler: this.close, scope: this}],
            items: [ { border: false, items: this.formPanel } ]
                     
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    ok: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        this.formPanel.submit({
            url: 'rest/vocContexts',
            method: 'POST',
            success: function(form, action) {
                this.close();     
                waitDialog.close(); 
                this.listener.afterAdd();          
            },
            failure: function(form, action) {    
                Ext.Msg.alert('Failure', action.result.error);            
                waitDialog.close();   
            },
            scope: this
        });
    }        
});

Ext.define( 'Comete.AdminVocUploader', {
    extend: 'Ext.window.Window',
    layout: 'fit',           
    initComponent: function( config ) {

        this.fileLocation = Ext.create('Ext.form.field.File', {
            name: 'file',
            fieldLabel: tr('local voc. (file)'),
            emptyText: 'VDEX ' + tr('or') + ' SKOS',
            labelWidth: 120,
            buttonText: '...'
        });

        this.formPanel = Ext.create('Ext.form.Panel', { 
            border: false,
            margin: '10',
            layout: 'form',
            defaultType: 'textfield',
            items: [ { fieldLabel: 'URI', disabled: true, value: this.uri },                      
                     this.fileLocation ]        
        }); 


        cfg = {
            title: tr('Modify vocabulary'),
            buttons: [ {text:'OK', handler: this.ok, scope: this}, {text:tr('Cancel'), handler: this.close, scope: this}],
            items: [ { border: false, items: this.formPanel } ]
                     
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    ok: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        this.formPanel.submit({
            url: this.restUrl,
            method: 'POST',
            success: function(form, action) {
                this.close();     
                waitDialog.close(); 
                this.listener.afterModify();
            },
            failure: function(form, action) {                
                Ext.Msg.alert('Failure', action.result.error);
                waitDialog.close();   
            },
            scope: this
        });
    }        
});

