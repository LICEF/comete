﻿Ext.define( 'Comete.AdminHarvest', {
    extend: 'Ext.panel.Panel',
    layout: 'border',
    initComponent: function( config ) {

        this.isDirty = true;

        this.harvestDefStore = Ext.create('Ext.data.JsonStore', {
            model: 'HarvestDefModel',
            proxy: harvestDefProxy,
            sorters: [ 'name' ]
        });   
 
        this.addButton = Ext.create('Ext.button.Button', {
            text: tr('Add'),
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

        function processIcon(val, cellmetadata, record, row) {
            var icon = record.get('inProcess') == true?'process.gif':'empty.png';
            return '<img src="images/' + icon + '"/>';
        }

        this.harvestDefList = Ext.create('Ext.grid.Panel', { 
            store: this.harvestDefStore,
            margin: '0 20 10 10',
            columns: [ 
                { dataIndex: 'id', hidden: true },
                { dataIndex: 'name', text: tr('Repositories'), flex: 1, height: 28, sortable: true },
                { dataIndex: 'inProcess', width: 40, renderer: processIcon }
            ],     
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
            },
            autoScroll: true,
            bbar: [ this.addButton, this.modifyButton, this.deleteButton ]
        });

        this.harvestDefList.on( 'selectionchange', this.harvestDefChanged, this );

        this.harvestDefStore.on( 'load', function() { this.checkHarvestsInProcess(this) }, this );

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
                     { fieldLabel: tr('Actions'), readOnly:true , xtype: 'checkboxfield', boxLabel: tr( 'Mark resources as "Pending for approval"' ) },
                     { readOnly:true , xtype: 'checkboxfield', boxLabel: tr( 'Mark resources that have broken links' ) },
                     { readOnly: true , xtype: 'checkboxfield', boxLabel: tr( 'Mark resources as "Invalid" when they are not compliant with this profile:' ), width: 200 },
                     { editable: false }, // invalidApplProf
                     { fieldLabel: 'XSLT', xtype: 'textarea', editable: false, inputAttrTpl: 'wrap="off" spellcheck="false"', height: 400 }
                   ]
        }); 


        this.startHarvestButton = Ext.create('Ext.button.Button', {
            text: tr('Start harvest'),
            disabled: true,
            handler: this.startHarvest, 
            scope: this
        } );

        this.stopHarvestButton = Ext.create('Ext.button.Button', {
            text: tr('Stop harvest'),
            disabled: true,
            handler: this.stopHarvest, 
            scope: this
        } );

        this.harvestReportsButton = Ext.create('Ext.button.Button', {
            text: tr('View harvest reports'),
            disabled: true,
            handler: this.viewHarvestReports, 
            scope: this
        } );

        this.harvestPanel = Ext.create('Ext.panel.Panel', { 
            layout: 'hbox',
            region: 'south',
            border: false,
            width: 500,
            margin: '30 0 10 10',
            items: [ this.startHarvestButton, {xtype: 'tbspacer', width: 10}, this.stopHarvestButton, 
                     {xtype: 'tbfill'}, this.harvestReportsButton ]
        });

        var cfg = {
            layout: 'border',
            region: 'center',
            items: [ this.leftPanel, 
                     { border: false, region: 'center', layout: 'fit',
                       items: { layout: 'vbox', border: false, scrollable: true, items: [ this.detailsPanel, this.harvestPanel ] } }
            ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    harvestDefChanged: function( model, selected ) {
        this.currentHarvestDefRestUrl = null;
        this.currentHarvestDefId = null;
        if (selected.length == 1) {
            this.currentHarvestDefRestUrl = selected[0].getData().restUrl;
            this.currentHarvestDefId = selected[0].getData().id;
            this.currentHarvestDefName = selected[0].getData().name;

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
                    this.detailsPanel.getComponent(6).setValue(jsonDetails.isPendingByDefault);
                    this.detailsPanel.getComponent(7).setValue(jsonDetails.isCheckingBrokenLink);
                    this.detailsPanel.getComponent(8).setValue(jsonDetails.isCheckingInvalid);

                    var invalidApplProfLabel = '';
                    if( jsonDetails.invalidApplProf == 'http://ltsc.ieee.org/xsd/LOM/strict' ) 
                        invalidApplProfLabel = tr( 'LOM Strict' );
                    else if( jsonDetails.invalidApplProf == 'http://ltsc.ieee.org/xsd/LOM/loose' )
                        invalidApplProfLabel = tr( 'LOM Loose' );
                    else if( jsonDetails.invalidApplProf == 'http://lom-fr.fr/validation/LomFRv1.0/core' )
                        invalidApplProfLabel = tr( 'LOM FR' );
                    else if( jsonDetails.invalidApplProf == 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' )
                        invalidApplProfLabel = tr( 'LOM Scorm LOM FR 1.0' );
                    else if( jsonDetails.invalidApplProf == 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' )
                        invalidApplProfLabel = tr( 'LOM Scorm LOM FR 1.1' );
                    else if( jsonDetails.invalidApplProf == 'http://www.normetic.org/LomNormeticv1.2' )
                        invalidApplProfLabel = tr( 'Normetic 1.2' );
                    else if( jsonDetails.invalidApplProf == 'http://www.openarchives.org/OAI/2.0/' )
                        invalidApplProfLabel = tr( 'OAI DC' );
                    this.detailsPanel.getComponent(9).setValue(invalidApplProfLabel);

                    this.detailsPanel.getComponent(10).setValue(jsonDetails.xsl);
                },
                scope: this 
            } );

            //buttons
            this.modifyButton.setDisabled(false);
            this.deleteButton.setDisabled(false);
            this.startHarvestButton.setDisabled(false);
            this.stopHarvestButton.setDisabled(false);
            this.harvestReportsButton.setDisabled(false);
        }
        else {
            this.detailsPanel.getComponent(0).setValue("");
            this.detailsPanel.getComponent(1).setValue("");
            this.detailsPanel.getComponent(2).setValue("");
            this.detailsPanel.getComponent(3).setValue("");
            this.detailsPanel.getComponent(4).setValue("");
            this.detailsPanel.getComponent(5).setValue("");
            this.detailsPanel.getComponent(6).setValue("");
            this.detailsPanel.getComponent(7).setValue("");
            this.detailsPanel.getComponent(8).setValue("");
            this.detailsPanel.getComponent(9).setValue("");
            this.detailsPanel.getComponent(10).setValue("");
            //buttons
            this.modifyButton.setDisabled(true);
            this.deleteButton.setDisabled(true);
            this.startHarvestButton.setDisabled(true);
            this.stopHarvestButton.setDisabled(true);
            this.harvestReportsButton.setDisabled(true);
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
    afterAdd: function( harvestDefIdToSelect ) {
        this.harvestDefList.getSelectionModel().deselectAll();
        this.harvestDefStore.loadPage( 1, { 
            callback: function() {
                if( harvestDefIdToSelect ) {
                    var selectedRecord = this.harvestDefStore.getById( harvestDefIdToSelect );
                    if( selectedRecord )
                        this.harvestDefList.getSelectionModel().select( [ selectedRecord ] );
                }
                Ext.Msg.alert(tr('Information'), tr('Repository added.'));
            },
            scope: this
        } );
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
            height: 500,
            modal: true,
            mode: 'modify',
            restUrl: this.currentHarvestDefRestUrl,
            values: values,
            listener: this
        });
        editor.show();
    },
    afterModify: function( harvestDefIdToSelect ) {
        this.harvestDefList.getSelectionModel().deselectAll();
        this.harvestDefStore.loadPage( 1, {
            callback: function() {
                if( harvestDefIdToSelect ) {
                    var selectedRecord = this.harvestDefStore.getById( harvestDefIdToSelect );
                    if( selectedRecord )
                        this.harvestDefList.getSelectionModel().select( [ selectedRecord ] );
                }
                Ext.Msg.alert(tr('Information'), tr('Repository modified.'));
            },
            scope: this
        } );
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
        var waitDialog = Ext.create('Ext.window.MessageBox', {});
        waitDialog.wait( tr('Please wait') + '...' );
        Ext.Ajax.request( {
            url: this.currentHarvestDefRestUrl,
            method: 'DELETE',
            success: function(response, opts) {
                waitDialog.close();
                this.harvestDefList.getSelectionModel().deselectAll();
                this.harvestDefStore.loadPage(1);
                Ext.Msg.alert(tr('Information'), tr('Repository deleted.'));
            },
            failure: function(response, opts) {
                waitDialog.close();
                Ext.Msg.alert(tr('Failure'), response.responseText);  
            },
            scope: this 
        } );
    },
    updateData: function() {
        if( this.isDirty ) {
            this.harvestDefStore.loadPage(1);
            this.isDirty = false;
        }

        //Start thread
        var comp = this;
        this.threadCheckHarvestsInProcess = 
            setInterval( function() { comp.checkHarvestsInProcess(comp) }, 20000);
    },
    startHarvest: function() {
        Ext.Ajax.request( {
            url: 'rest/harvests/' + this.currentHarvestDefId,
            method: 'POST',
            success: function(response, opts) {
                Ext.Msg.alert(tr('Information'), tr('Harvest started.'));
                this.harvestDefStore.loadPage();
            },
            failure: function(response, opts) {
                Ext.Msg.alert(tr('Message'), response.responseText);  
            },
            scope: this 
        } );
    },
    stopHarvest: function() {
        Ext.Ajax.request( {
            url: 'rest/harvests/' + this.currentHarvestDefId,
            method: 'DELETE',
            success: function(response, opts) {
                Ext.Msg.alert(tr('Information'), tr('Harvest stopped.'));
                this.harvestDefStore.loadPage();
            },
            failure: function(response, opts) {
                Ext.Msg.alert(tr('Message'), response.responseText);  
            },
            scope: this 
        } );
    },
    viewHarvestReports: function() {
        var viewer = Ext.create('Comete.AdminHarvestReportViewer', {
            width: 700,
            height: 500,
            modal: true,
            harvestDefId: this.currentHarvestDefId,
            harvestDefName: this.currentHarvestDefName
        });
        viewer.show();
    },
    checkHarvestsInProcess: function(comp) {
        //hide all processing icons
        var records = this.harvestDefStore.getData().items;
        for (var i = 0; i < records.length; i++) {
            records[i].set('inProcess', false);
            records[i].commit(); //probably not good use of commit but avoid mark cell as dirty -AM
        }

        Ext.Ajax.request( {
            url: 'rest/harvests',
            method: 'GET',
            success: function(response, opts) {
                var array = Ext.JSON.decode(response.responseText, true).harvests;
                for (var i = 0; i < array.length; i++) {
                    var record = comp.harvestDefList.getStore().findRecord('id', array[i]);  
                    record.set('inProcess', true);
                    record.commit(); //probably not good use of commit but avoid mark cell as dirty -AM
                }
            },
            failure: function(response, opts) {
                Ext.Msg.alert(tr('Failure'), response.responseText);
            },
            scope: this
        } );
    },
    beforeQuit: function() {
        //Stop thread
        clearInterval(this.threadCheckHarvestsInProcess);
    }
} );

Ext.define( 'Comete.AdminHarvestDefEditor', {
    extend: 'Ext.window.Window',
    layout: 'fit',
    scrollable: true, 
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

        this.isCheckingInvalidCheckbox = Ext.create( 'Ext.form.field.Checkbox', {
            name: 'isCheckingInvalid', boxLabel: tr( 'Mark resources as "Invalid" when they are not compliant with this profile:' )
        } );
        this.isCheckingInvalidCheckbox.on( 'change', 
            function( element, newValue, oldValue, eOpts ) { 
                if( newValue ) 
                    this.invalidApplProfComboBox.enable();
                else 
                    this.invalidApplProfComboBox.disable();
            }, this );

        this.applProfStore = Ext.create( 'Ext.data.Store', {
            fields: ['id', 'label'],
            data : [
                { 'id': 'http://ltsc.ieee.org/xsd/LOM/strict', 'label': tr( 'LOM Strict' ) },
                { 'id': 'http://ltsc.ieee.org/xsd/LOM/loose', 'label': tr( 'LOM Loose' ) },
                { 'id': 'http://lom-fr.fr/validation/LomFRv1.0/core', 'label': tr( 'LOM FR' ) },
                { 'id': 'http://lom-fr.fr/validation/ScoLomFRv1.0/core', 'label': tr( 'LOM Scorm LOM FR 1.0' ) },
                { 'id': 'http://lom-fr.fr/validation/ScoLomFRv1.1/core', 'label': tr( 'LOM Scorm LOM FR 1.1' ) },
                { 'id': 'http://www.normetic.org/LomNormeticv1.2', 'label': tr( 'LOM Normetic 1.2' ) },
                { 'id': 'http://www.openarchives.org/OAI/2.0/', 'label': tr( 'OAI DC' ) }
            ]
        } );

        this.invalidApplProfComboBox = Ext.create( 'Ext.form.field.ComboBox', {
            name: 'invalidApplProf', valueField: 'id', displayField: 'label', disabled: true, store: this.applProfStore, editable: false, 
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
        } );

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
                     { name: 'adminEmail', fieldLabel: tr('Admin email') },
                     { name: 'isPendingByDefault', fieldLabel: tr('Actions'), xtype: 'checkboxfield', boxLabel: tr( 'Mark resources as "Pending for approval"' ) },
                     { name: 'isCheckingBrokenLink', xtype: 'checkboxfield', boxLabel: tr( 'Mark resources that have broken links' ) },
                     this.isCheckingInvalidCheckbox,
                     this.invalidApplProfComboBox,
                     { fieldLabel: 'XSLT',
                       name: 'xsl',
                       xtype: 'textarea', 
                       valueField: 'xsl',
                       inputAttrTpl: 'wrap="off" spellcheck="false"', height: 400, 
                       emptyText: tr('Optional field') } ]
        }); 

        var cfg = {
            title: (this.mode == 'modify')?tr('Modify repository'):tr('Add repository'),
            maximizable: true,
            buttons: [ {text:'OK', handler: this.ok, scope: this}, {text:tr('Cancel'), handler: this.close, scope: this}],
            items: [ { border: false, scrollable: true, items: this.formPanel } ]

        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

       if (this.values)
           this.setValues();
    },
    ok: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {});
        waitDialog.wait( tr('Please wait') + '...' );
        this.formPanel.submit({
            url: ((this.mode == 'modify')?this.restUrl:'rest/harvestDefinitions'),
            method: ((this.mode == 'modify')?'PUT':'POST'),
            success: function(form, action) {
                var harvestDefId = form.getFieldValues().id;
                this.close();
                waitDialog.close();
                if (this.mode == 'modify') 
                    this.listener.afterModify(harvestDefId);
                else
                    this.listener.afterAdd(harvestDefId);
            },
            failure: function(form, action) { 
                Ext.Msg.alert(tr('Failure'), action.result.error);
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
        this.formPanel.getComponent(6).setValue(this.values.isPendingByDefault);
        this.formPanel.getComponent(7).setValue(this.values.isCheckingBrokenLink);
        this.formPanel.getComponent(8).setValue(this.values.isCheckingInvalid);
        this.formPanel.getComponent(9).setValue(this.values.invalidApplProf);
        this.formPanel.getComponent(10).setValue(this.values.xsl);
    }        
});


Ext.define( 'Comete.AdminHarvestReportViewer', {
    extend: 'Ext.window.Window',
    layout: 'border',
    initComponent: function( config ) {

        this.harvestReportStore = Ext.create('Ext.data.JsonStore', {
            model: 'HarvestReportModel',
            proxy: harvestReportProxy
        });

        this.harvestReportStore.getProxy().url = 'rest/harvestReports/' + this.harvestDefId;

        this.deleteButton = Ext.create('Ext.button.Button', {
            text: tr('Delete'),
            disabled: true,
            handler: this.deleteReport, 
            scope: this
        } );

        this.harvestReportList = Ext.create('Ext.grid.Panel', { 
            store: this.harvestReportStore,
            region: 'west',  
            border: false,
            columns: [ 
                { dataIndex: 'name', flex: 1 }
            ],     
            hideHeaders: true,
            margin: '-1 0 0 0',
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false
            },
            autoScroll: true,
            bbar: [ this.deleteButton ]
        });

        this.harvestReportList.on( 'selectionchange', this.harvestReportChanged, this );

        this.reportsPanel = Ext.create('Ext.Panel', { 
            layout: 'fit',
            region: 'west',
            split: true,
            width: 200,
            items: this.harvestReportList
        }); 

        this.content = Ext.create('Ext.panel.Panel', { 
            region: 'center',
            border: true,
            autoScroll: true
        });

        var cfg = {
            title: tr('Harvest reports for :') + ' ' + this.harvestDefName,
            buttons: [ {text:'OK', handler: this.close, scope: this} ],
            maximizable: true,
            items: [ this.reportsPanel, this.content ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.harvestReportStore.load();
    },
    harvestReportChanged: function( model, selected ) {
        this.currentHarvestReportRestUrl = null;
        this.content.update(null);
        if (selected.length == 1) {
            this.currentHarvestReportRestUrl = selected[0].getData().restUrl;

            Ext.Ajax.request( {
                url: this.currentHarvestReportRestUrl + '/html',
                method: 'GET',
                success: function(response) {
                    var report = response.responseText;
                    this.content.update(report);
                },
                scope: this 
            } );

            this.deleteButton.setDisabled(false);
        }
        else
            this.deleteButton.setDisabled(true);
    },
    deleteReport: function() {
        var records = this.harvestReportList.getSelectionModel().getSelection();
        if (records.length == 0)
            return;
        
        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            msg: tr('Do you really want to delete report ?'),
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.QUESTION,
            fn: this.deleteReportEff,
            scope: this
        });
    },
    deleteReportEff: function(button) {
        if (button != 'ok')
            return;
        var waitDialog = Ext.create('Ext.window.MessageBox', {
        });
        waitDialog.wait( tr('Please wait') + '...' );
        Ext.Ajax.request( {
            url: this.currentHarvestReportRestUrl,
            method: 'DELETE',
            success: function(response, opts) {
                waitDialog.close();
                this.harvestReportStore.loadPage(1);
                Ext.Msg.alert(tr('Information'), tr('Report deleted.'));
            },
            failure: function(response, opts) {
                waitDialog.close();
                Ext.Msg.alert(tr('Failure'), response.responseText);  
            },
            scope: this 
        } );
    }
});
