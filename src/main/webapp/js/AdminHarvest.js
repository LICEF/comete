﻿Ext.define( 'Comete.AdminHarvest', {
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

        this.defPanel = Ext.create('Ext.Panel', { 
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
            items: this.defPanel
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
                     { fieldLabel: 'Format', editable: false },
                     { fieldLabel: tr('Admin email'), editable: false },
                     { fieldLabel: 'XSL', xtype: 'textarea', editable: false,
                       inputAttrTpl: 'wrap="off" spellcheck="false"', height: 180 }
                   ]
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
        this.currentHarvestDefRestUrl = null;
        if (selected.length == 1) {
            this.currentHarvestDefRestUrl = selected[0].getData().restUrl;

            //fields update
            Ext.Ajax.request( {
                url: this.currentHarvestDefRestUrl,
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
                    this.detailsPanel.getComponent(5).setValue(jsonDetails.adminEmail);
                    this.detailsPanel.getComponent(6).setValue(jsonDetails.xsl);
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
            this.detailsPanel.getComponent(5).setValue("");
            this.detailsPanel.getComponent(6).setValue("");
            //buttons
            this.modifyButton.setDisabled(true);     
            this.deleteButton.setDisabled(true);     
        }
    },
    addHarvestDef: function() {
        var editor = Ext.create('Comete.AdminHarvestDefEditor', {
            width: 500,
            modal: true,
            listener: this
        });
        editor.show();       
    },
    afterAdd: function() {
        this.harvestDefStore.loadPage();
        Ext.Msg.alert('Information', tr('Repository added.'));
    },
    modifyHarvestDef: function() {        
        Ext.Ajax.request( {
            url: this.currentHarvestDefRestUrl,
            method: 'GET',
            success: function(response) {
                var jsonDetails = Ext.JSON.decode(response.responseText, true);
                this.modifyHarvestDefStep2(jsonDetails)
            },
            scope: this 
        } );      
    },
    modifyHarvestDefStep2: function(values) {
        var editor = Ext.create('Comete.AdminHarvestDefEditor', {
            width: 500,
            modal: true,
            mode: 'modify',
            restUrl: this.currentHarvestDefRestUrl,
            values: values,
            listener: this            
        });
        editor.show();
    },
    afterModify: function() {
        this.harvestDefStore.loadPage();
        this.harvestDefList.getSelectionModel().deselectAll();
        Ext.Msg.alert('Information', tr('Repository modified.'));
    },
    deleteHarvestDef: function() {
        var records = this.harvestDefList.getSelectionModel().getSelection();
        if (records.length == 0)
            return;
        
        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            msg: tr('Do you really want to delete repository ?'),
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.QUESTION,
            fn: this.deleteHarvestDefEff,
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
            url: this.currentHarvestDefRestUrl,
            method: 'DELETE',
            success: function(response, opts) {
                waitDialog.close();
                this.harvestDefStore.loadPage(1);
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

        var protocolStore = Ext.create('Ext.data.Store', {
            fields: ['type', 'name'],
            data : [ { 'type': 'OAI', 'name': 'OAI-PMH'}, 
                     { 'type': 'HTML', 'name': tr('HTML Spider') } ]
        });

        var namespaceStore = Ext.create('Ext.data.Store', {
            fields: ['ns', 'name'],
            data : [ { 'ns': 'http://ltsc.ieee.org/xsd/LOM', 'name': tr('IEEE LOM') },
                     { 'ns': 'http://www.openarchives.org/OAI/2.0/oai_dc/HTML', 'name': tr('OAI DC') } ]
        });

        this.formPanel = Ext.create('Ext.form.Panel', { 
            border: false,
            margin: '10',
            layout: 'form',
            defaultType: 'textfield',
            items: [ { name: 'id', fieldLabel: 'ID', editable: this.mode != 'modify' },
                     { name: 'name', fieldLabel:  tr('Name') },
                     { fieldLabel: tr('Protocol'),
                       name: 'type',
                       xtype: 'combo',
                       editable: false,
                       store: protocolStore,
                       displayField: 'name', 
                       valueField: 'type',
                       value: 'OAI',
                       tpl: '<div><tpl for="."><div class="x-boundlist-item">{name}</div></tpl></div>' },
                     { name: 'url', fieldLabel:  'URL' },
                     { fieldLabel: tr('Format'),
                       name: 'ns',
                       xtype: 'combo',                      
                       editable: false,
                       store: namespaceStore, 
                       displayField: 'name',
                       valueField: 'ns',
                       value: 'http://ltsc.ieee.org/xsd/LOM',
                       tpl: '<div><tpl for="."><div class="x-boundlist-item">{name}</div></tpl></div>' },
                     { name: 'adminEmail', fieldLabel:  tr('Admin email'), emptyText: tr('Optional field') },
                     { fieldLabel: 'XSL',
                       name: 'xsl',
                       xtype: 'textarea', 
                       valueField: 'xsl',
                       inputAttrTpl: 'wrap="off" spellcheck="false"', height: 180,
                       emptyText: tr('Optional field') } ]
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
            url: ((this.mode == 'modify')?this.restUrl:'rest/harvestDefinitions'),
            method: ((this.mode == 'modify')?'PUT':'POST'),
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
        this.formPanel.getComponent(1).setValue(this.values.name);
        this.formPanel.getComponent(2).setValue(this.values.type);
        this.formPanel.getComponent(3).setValue(this.values.url);
        this.formPanel.getComponent(4).setValue(this.values.metadataNamespace);
        this.formPanel.getComponent(5).setValue(this.values.adminEmail);
        this.formPanel.getComponent(6).setValue(this.values.xsl);
    }        
});


