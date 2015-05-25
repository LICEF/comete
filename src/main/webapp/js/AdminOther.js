function doResetMetamodel() {
    Ext.Ajax.request( {
        url: 'rest/metadataRecords/redigestAll',
        method: 'PUT',
        failure: function(response, opts) {
            Ext.Msg.alert('Failure', response.responseText );
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
            Ext.Msg.alert('Failure', response.responseText );
        }
    } );
}

function startBackup() {
    resultLabel.setVisible(true); 
    resultLabel.update( tr('Please wait') ); 
    Ext.Ajax.request( {
        url: 'rest/backups',
        method: 'POST',
        success: function(response, opts) {
            resultLabel.update( response.responseText );
        },
        failure: function(response, opts) {
            resultLabel.update('');
            Ext.Msg.alert('Failure', response.responseText );
        }
    } );
}

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


var resetPanel = Ext.create('Ext.form.Panel', {
    layout: {
        type: 'vbox',
        align: 'left'
    },
    border: false,
    margin: '0 0 0 20',
    items:[  { xtype: 'label', text: tr( 'Reinitialization' ), margin: '0 0 10 0', cls: 'sectionTitle' },
             buttonResetMetamodel, 
             {xtype:'tbspacer', height: 10}, 
             {layout:'hbox', border: false, items: [buttonResetLO, {xtype:'tbspacer', width: 10}, 
                       uriField, {xtype:'tbspacer', width: 10}]},
             { xtype: 'label', text: tr( 'Backup' ), margin: '20 0 10 0', cls: 'sectionTitle'},
             {layout:'hbox', border: false, items: [ buttonStartBackup ]},
             {layout:'hbox', border: false, items: [ {xtype:'tbspacer', width: 200}, resultLabel ]}
          
    ]
} );

var otherPanel = Ext.create('Ext.panel.Panel', {
    layout: 'vbox',
    border: false,
    items: resetPanel
} );

