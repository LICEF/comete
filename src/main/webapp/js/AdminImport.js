Ext.define( 'Comete.AdminImport', {
    extend: 'Ext.panel.Panel',
    layout: 'vbox',  
    initComponent: function( config ) {

        this.waitDialog = Ext.create('Ext.window.MessageBox', {
        });

        this.uploadFile = Ext.create('Ext.form.field.File', {
            name: 'file',
            buttonText: tr('Browse') + '...',
            width: 400
        } );

        this.resultLabel = Ext.create('Ext.form.Label', {
            id: 'result',
            padding: 10, 
            hidden: true
        } );

        this.uploadButton = Ext.create('Ext.button.Button', {
            text: tr('Upload'),
            handler: function() {
                this.resultLabel.setVisible(false);
                this.waitDialog.wait( tr('Please wait') + '...' );
                var uploadForm = this.uploadPanel.getForm();
                if (uploadForm.isValid()) {
                    var isPendingByDefault = this.setPendingByDefaultCheckbox.getValue();
                    var isCheckingBrokenLink = this.setCheckBrokenLinkCheckox.getValue();
                    var isCheckingInvalid = this.setCheckInvalidCheckbox.getValue();
                    var invalidApplProf = encodeURIComponent( this.invalidApplProfComboBox.getValue() );
                    uploadForm.submit({ 
                        url: 'rest/metadataRecords?validation=true&isPendingByDefault=' + isPendingByDefault + 
                            '&isCheckingBrokenLink=' + isCheckingBrokenLink + '&isCheckingInvalid=' + isCheckingInvalid + '&invalidApplProf=' + invalidApplProf,
                        success: function(form, action) {
                            if (action.result.data == "ALREADY_EXISTS") {
                                this.waitDialog.close();
                                Ext.Msg.show({
                                    title: tr('Warning'), 
                                    msg: tr( 'A record with the same identifier already exists.<br/>It will be replaced with this new one.<br/>Do you want to continue ?' ), 
                                    buttons: Ext.Msg.OKCANCEL,
                                    icon: Ext.Msg.WARNING,
                                    fn: function(btn) { 
                                        if( btn == 'ok' )
                                            this.completeSubmit();   
                                        else {
                                            Ext.Ajax.request( {
                                                method: 'PUT',
                                                url: 'rest/metadataRecords/clearUpload',
                                                failure: function( response ) {
                                                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                                                }
                                            } );   
                                        }
                                    },
                                    scope: this
                                });
                            }
                            else 
                                this.update(action.result);
                        },
                        failure: function(form, action) {
                            Ext.Msg.alert('Failure', action.result.error);
                            this.waitDialog.close();
                        },
                        scope: this
                    });
                }
            },
            scope: this
        });

        this.setPendingByDefaultCheckbox = Ext.create( 'Ext.form.field.Checkbox', {
            fieldLabel: tr( 'Actions' ),
            name: 'isPendingByDefault',
            boxLabel: tr( 'Mark resources as "Pending for approval"' )
        } );

        this.setCheckBrokenLinkCheckox = Ext.create( 'Ext.form.field.Checkbox', {
            name: 'isCheckingBrokenLink',
            boxLabel: tr( 'Mark resources that have broken links' )
        } );

        this.setCheckInvalidCheckbox = Ext.create( 'Ext.form.field.Checkbox', {
            name: 'isCheckingInvalid',
            boxLabel: tr( 'Mark resources as "Invalid" when they are not compliant with this profile:' )
        } );
        this.setCheckInvalidCheckbox.on( 'change', 
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

        this.uploadPanel = Ext.create('Ext.form.Panel', {
            padding: 10,
            border: false,    
            items:[ { border: false, html: tr('Select LOM or zip of LOMs.<br/>Metametadata identifier (field 3.1) <b>must be present</b>.') }, 
                    { xtype: 'tbspacer', height: 10 }, this.uploadFile, 
                    { xtype: 'panel', layout: 'form', width: 500, border: 0, margin: '10 0 0 0', items: [ 
                         this.setPendingByDefaultCheckbox, this.setCheckBrokenLinkCheckox, this.setCheckInvalidCheckbox, this.invalidApplProfComboBox ] }, 
                    { xtype: 'tbspacer', height: 10 }, this.uploadButton ]
        });

        var cfg = {
            items: [ this.uploadPanel, this.resultLabel ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    update: function(json) {
        var res = '';
        for (i = 0; i < json.data.length; i++) {
            var state = json.data[i].state;
            if (state == 'ignored')
                state = 'same metadata record => no change';
            res += '<br/><a href="' + json.data[i].uri + '" target="_blank">' + json.data[i].uri + '</a> ' + state;
        }
        Ext.get('result').update( res ); 
        this.resultLabel.setVisible(true); 
        this.waitDialog.close();
    },
    completeSubmit: function() {
        this.waitDialog.wait( tr('Please wait') + '...' );
        Ext.Ajax.request( {
            method: 'PUT',
            url: 'rest/metadataRecords/completeUpload',
            success: function( response ) {
                var json = Ext.decode(response.responseText);
                this.update(json);   
            },
            failure: function( response ) {
                this.waitDialog.close();
                Ext.Msg.alert( tr( 'Failure' ), response.responseText );
            },
            scope: this
        });   
    }

} );


