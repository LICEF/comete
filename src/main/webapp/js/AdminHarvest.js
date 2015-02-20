Ext.define( 'Comete.AdminHarvest', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {
        this.isDirty = true;

        this.harvestDefStore = Ext.create('Ext.data.JsonStore', {
            model: 'HarvestDefModel',                
            proxy: harvestDefProxy
        });   
        this.harvestDefStore.sort( 'label', 'ASC' );
 
        this.addButton = Ext.create('Ext.button.Button', {
            text: tr('Add'),
            disabled: !authorized,
            handler: this.addHarvestDef, 
            scope: this
        } );

        this.modifyButton = Ext.create('Ext.button.Button', {
            text: tr('Modify'),
            disabled: true,
            handler: this.modifyHarvestDef, 
            scope: this
        } );

        this.deleteButton = Ext.create('Ext.button.Button', {
            text: tr('Delete'),
            disabled: true,
            handler: this.deleteHarvestDef, 
            scope: this
        } );

        this.harvestDefList = Ext.create('Ext.grid.Panel', { 
            store: this.harvestDefStore,
            margin: '0 20 10 10',                
            columns: [ 
                { dataIndex: 'name', text: tr('Repositories'), flex: 1, height: 28 }
            ],     
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
            },
            autoScroll: true,
            bbar: [ this.addButton, this.modifyButton, this.deleteButton ]
        });

        this.harvestDefList.on( 'selectionchange', this.harvestDefChanged, this );

        this.vocPanel = Ext.create('Ext.Panel', { 
            layout: 'fit',
            region: 'center',
            border: false,
            items: this.harvestDefList
        });

        this.leftPanel = Ext.create('Ext.Panel', { 
            layout: 'border',
            width: 400,
            region: 'west',     
            border: false,
            split: true,
            items: this.vocPanel
        }); 
       
        this.detailsPanel = Ext.create('Ext.Panel', { 
            region: 'center',
            width: 500,
            margin: '0 0 0 10',
            border: false,
            layout: 'form',
            defaultType: 'textfield',
            items: [ { fieldLabel: 'ID', editable: false }, 
                     { fieldLabel: tr('Name'), editable: false},
                     { fieldLabel: tr('Protocol'), editable: false },
                     { fieldLabel: 'URL', editable: false },
                     { fieldLabel: 'Format', editable: false }]        
        }); 


        var cfg = {
            layout: 'border',
            region: 'center',      
            items: [ this.leftPanel, { border: false, items: this.detailsPanel } ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    harvestDefChanged: function( model, selected ) {
        if (selected.length == 1) {
            var restUrl = selected[0].getData().restUrl;

            //fields update
            Ext.Ajax.request( {
                url: restUrl,
                method: 'GET',
                success: function(response) {
                    var jsonDetails = Ext.JSON.decode(response.responseText, true);
                    this.detailsPanel.getComponent(0).setValue(jsonDetails.id);
                    this.detailsPanel.getComponent(1).setValue(jsonDetails.name);
                    var protocol = (jsonDetails.type == 'OAI')?'OAI-PMH':tr('HTML Spider');
                    this.detailsPanel.getComponent(2).setValue(protocol);
                    this.detailsPanel.getComponent(3).setValue(jsonDetails.url);
                    var format = (jsonDetails.metadataNamespace == 'http://ltsc.ieee.org/xsd/LOM')?'IEEE LOM':'OAI DC';
                    this.detailsPanel.getComponent(4).setValue(format);
                },
                scope: this 
            } );

            //buttons
            if (authorized) {  
                this.modifyButton.setDisabled(false);     
                this.deleteButton.setDisabled(false);     
            }
        }
        else {
            this.detailsPanel.getComponent(0).setValue("");
            this.detailsPanel.getComponent(1).setValue("");
            this.detailsPanel.getComponent(2).setValue("");
            this.detailsPanel.getComponent(3).setValue("");
            this.detailsPanel.getComponent(4).setValue("");
            //buttons
            this.modifyButton.setDisabled(true);     
            this.deleteButton.setDisabled(true);     
        }
    },
    addHarvestDef: function() {
        var editor = Ext.create('Comete.AdminHarvestDefEditor', {
            width: 500,
            height: 270,
            modal: true,
            listener: this
        });
        editor.show();       
    },
    afterAdd: function() {
        this.vocCtxtStore.loadPage(1);
        Ext.Msg.alert('Information', tr('Repository added.'));
    },
    modifyHarvestDef: function() {
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
    modifyHarvestDefStep2: function(button) {
        if (button != null && button != 'ok')
            return;
        Ext.Ajax.request( {
            url: this.currentVocContextRestUrl + '/details',
            method: 'GET',
            success: function(response) {
                var jsonDetails = Ext.JSON.decode(response.responseText, true).vocDetails[0];
                this.modifyVocabularyStep3(jsonDetails)
            },
            scope: this 
        } );      
    },
    modifyHarvestDefStep3: function(values) {
        var editor = Ext.create('Comete.AdminHarvestDefEditor', {
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
        Ext.Msg.alert('Information', tr('Repository modified.'));
    },
    deleteHarvestDef: function() {
        var records = this.harvestDefList.getSelectionModel().getSelection();
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
    deleteHarvestDefEff: function(button) {
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
                Ext.Msg.alert('Information', tr('Repository deleted.'));
            },
            failure: function(response, opts) {
                waitDialog.close();
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );
    },    
    updateData: function() {
        if( this.isDirty ) {
            this.harvestDefStore.loadPage(1);
            this.isDirty = false;
        }
    }
} );

Ext.define( 'Comete.AdminHarvestDefEditor', {
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
            title: (this.mode == 'modify')?tr('Modify repository'):tr('Add repository'),
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


