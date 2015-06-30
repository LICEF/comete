Ext.define( 'Comete.AdminMaintenance', {
    extend: 'Ext.panel.Panel',
    layout: 'border',
    initComponent: function( config ) {
        function doResetMetamodel() {
            Ext.Ajax.request( {
                url: 'rest/metadataRecords/redigestAll',
                method: 'PUT',
                failure: function(response, opts) {
                    Ext.Msg.alert(tr('Failure'), response.responseText );
                }
            } );
        }

        function doResetLO(recordUri) {
            resultLabel.setVisible(true); 
            resultLabel.update( tr('Please wait') );
            Ext.Ajax.request( {
                url: 'rest/metadataRecords/redigestRecord/' + encodeURIComponent(uriField.getValue()),
                method: 'PUT',
                success: function(response, opts) {
                    resultLabel.update( response.responseText );
                },
                failure: function(response, opts) {
                    resultLabel.update('');
                    Ext.Msg.alert(tr('Failure'), response.responseText );
                }
            } );
        }

        function startBackup() {
            Ext.Ajax.request( {
                url: 'rest/backups',
                method: 'POST',
                success: function(response, opts) {
                    Ext.Msg.alert( tr( 'Information' ), tr( 'Backup started.' ) );
                },
                failure: function(response, opts) {
                    if( response.status == 409 )
                        Ext.Msg.alert( tr( 'Warning' ), tr( 'A backup is already in progress.' ) );
                    else
                        Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                }
            } );
        }

        this.saveNotifSettings = function() {
            Ext.Ajax.request( {
                url: 'rest/settings/notifications',
                method: 'POST',
                success: function() {
                    Ext.Msg.alert( tr( 'Warning' ), tr( 'Notification settings saved.' ) );
                },
                failure: function( response ) {
                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                },
                params: {
                    'notifEmail': this.notifEmailField.getValue(),
                    'brokenLinkValidationCompletionNotif': this.brokenLinkNotifCheckbox.getValue(),
                    'harvestCompletionNotif': this.harvestNotifCheckbox.getValue()
                },
                scope: this
            } );
        };

        var buttonResetMetamodel = new Ext.Button( { 
            text: tr( 'Reset Metamodel' ), 
            handler: function() { 
                var promptBox = Ext.Msg;
                promptBox.buttonText = { cancel: tr("Cancel") };
                promptBox.show( {
                    title: tr( 'Question' ),
                    msg: tr( 'Are you sure that you want to reset metamodel ?' ),
                    buttons: Ext.Msg.OKCANCEL,
                    fn: function( btn, text ) {
                        if( btn == 'ok' ) {
                            doResetMetamodel();
                        }
                    },
                    minWidth: 250,
                    multiline: false,
                    icon: Ext.Msg.QUESTION
                } );
            }
        } );


        var buttonResetLO = new Ext.Button( { 
            text: tr( 'Reset Learning Object' ), 
            handler: function() {
                if (uriField.getValue() == '')
                    return;

                var promptBox = Ext.Msg;
                promptBox.buttonText = { cancel: tr("Cancel") };
                promptBox.show( {
                    title: tr( 'Question' ),
                    msg: tr( 'Are you sure that you want to reset this learning object ?' ),
                    buttons: Ext.Msg.OKCANCEL,
                    fn: function( btn, text ) {
                        if( btn == 'ok' ) {
                            doResetLO();
                        }
                    },
                    minWidth: 250,
                    multiline: false,
                    icon: Ext.Msg.QUESTION
                } );
            }
        } );

        var uriField = Ext.create('Ext.form.field.Text', {
            width: 400,
            emptyText: tr( 'Record URI' )
        } );

        var buttonDeleteLO = new Ext.Button( { 
            text: tr( 'Delete Learning Object' ), 
            handler: function() {
                if (uriToDeleteField.getValue() == '')
                    return;

                var promptBox = Ext.Msg;
                promptBox.buttonText = { cancel: tr("Cancel") };
                promptBox.show( {
                    title: tr( 'Question' ),
                    msg: tr( 'Are you sure that you want to delete this learning object ?' ),
                    buttons: Ext.Msg.OKCANCEL,
                    fn: function( btn, text ) {
                        if( btn == 'ok' ) {
                            doDeleteLO();
                        }
                    },
                    minWidth: 250,
                    multiline: false,
                    icon: Ext.Msg.QUESTION
                } );
            }
        } );


        var buttonStartBackup = new Ext.Button( { 
            text: tr( 'Start Backup' ), 
            handler: function() { 
                var promptBox = Ext.Msg;
                promptBox.buttonText = { cancel: tr("Cancel") };
                promptBox.show( {
                    title: tr( 'Question' ),
                    msg: tr( 'Are you sure that you want to start a backup process ?' ),
                    buttons: Ext.Msg.OKCANCEL,
                    fn: function( btn, text ) {
                        if( btn == 'ok' ) {
                            startBackup();
                        }
                    },
                    minWidth: 250,
                    multiline: false,
                    icon: Ext.Msg.QUESTION
                } );
            }
        } );

        var resultLabel = Ext.create('Ext.form.Label', {
            id: 'result',
            padding: 10, 
            hidden: true
        } );

        this.notifEmailField = Ext.create( 'Ext.form.field.Text', {
            fieldLabel: tr( 'Email' ),
            labelWidth: 200,
            name: 'notifEmail'
        } );

        this.brokenLinkNotifCheckbox = Ext.create( 'Ext.form.field.Checkbox', {
            fieldLabel: tr( 'Completion of Broken Links Validation' ),
            name: 'brokenLinkValidationCompletionNotif'
        } );

        this.harvestNotifCheckbox = Ext.create( 'Ext.form.field.Checkbox', {
            fieldLabel: tr( 'Completion of Harvest' ),
            name: 'harvestCompletionNotif'
        } );

        var buttonSaveNotifParams = new Ext.Button( { 
            height: 26,
            text: tr( 'Save Settings' ), 
            handler: this.saveNotifSettings,
            scope: this
        } );

        var resetPanel = Ext.create('Ext.form.Panel', {
            layout: {
                type: 'vbox',
                align: 'left'
            },
            border: false,
            margin: '0 0 0 20',
            items:[
                { xtype: 'label', text: tr( 'Reinitialization' ), margin: '0 0 10 0', cls: 'sectionTitle' },
                buttonResetMetamodel, 
                {layout:'hbox', border: false, margin: '10 0 0 0', items: [buttonResetLO, {xtype:'tbspacer', width: 10}, 
                    uriField, {xtype:'tbspacer', width: 10}]},
                { xtype: 'label', text: tr( 'Backup' ), margin: '20 0 10 0', cls: 'sectionTitle'},
                {layout:'hbox', border: false, items: [ buttonStartBackup ]},
                {layout:'hbox', border: false, items: [ {xtype:'tbspacer', width: 200}, resultLabel ]},
                { xtype: 'label', text: tr( 'Notifications' ), margin: '30 0 0 0', cls: 'sectionTitle' },
                { xtype: 'panel', layout: 'form', border: 0, bodyPadding: 0, items: [ 
                    this.notifEmailField, this.brokenLinkNotifCheckbox, this.harvestNotifCheckbox ] }, 
                buttonSaveNotifParams
            ]
        } );

        var cfg = {
            layout: 'vbox',
            border: false,
            items: resetPanel
        };

        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    updateData: function() {
        Ext.Ajax.request( {
            url: 'rest/settings/notifications',
            method: 'GET',
            success: function( response ) {
                var data = Ext.decode( response.responseText );
                this.notifEmailField.setValue( data.notifEmail );
                this.brokenLinkNotifCheckbox.setValue( data.brokenLinkValidationCompletionNotif );
                this.harvestNotifCheckbox.setValue( data.harvestCompletionNotif );
            },
            failure: function( response ) {
                Ext.Msg.alert( tr( 'Failure' ), response.responseText );
            },
            scope: this
        } );
    }

} );
