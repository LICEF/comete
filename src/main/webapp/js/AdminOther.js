function doResetMetamodel() {
    Ext.Ajax.request( {
        url: metadataUrl + '/rest/metadataRecords/redigestAll',
        method: 'GET',
        failure: function(response, opts) {
            Ext.Msg.alert('Failure', response.responseText );
        }
    } );
}

function doResetLO(recordUri) {    
    resultLabel.setVisible(true); 
    resultLabel.update( tr('Please wait') );
    Ext.Ajax.request( {
        url: metadataUrl + '/rest/metadataRecords/redigestRecord',
        params: { recordUri: uriField.getValue() },
        method: 'GET',
        success: function(response, opts) {
            resultLabel.update( response.responseText );
        },
        failure: function(response, opts) {
            resultLabel.update('');
            Ext.Msg.alert('Failure', response.responseText );
        }
    } );
}

function doDeleteLO(recordUri) {    
    resultLabel.setVisible(true); 
    resultLabel.update( tr('Please wait') );
    Ext.Ajax.request( {
        url: metadataUrl + '/rest/metadataRecords/' + uriToDeleteField.getValue(),
        method: 'DELETE',
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
        Ext.Msg.show( {
            title: tr( 'Question' ),
            msg: tr( 'Are you sure that you want to reset metamodel ?' ),
            buttons: Ext.Msg.YESNO,
            fn: function( btn, text ) {
                if( btn == 'yes' ) {
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

        Ext.Msg.show( {
            title: tr( 'Question' ),
            msg: tr( 'Are you sure that you want to reset this learning object ?' ),
            buttons: Ext.Msg.YESNO,
            fn: function( btn, text ) {
                if( btn == 'yes' ) {
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

        Ext.Msg.show( {
            title: tr( 'Question' ),
            msg: tr( 'Are you sure that you want to delete this learning object ?' ),
            buttons: Ext.Msg.YESNO,
            fn: function( btn, text ) {
                if( btn == 'yes' ) {
                    doDeleteLO();
                }
            },
            minWidth: 250,
            multiline: false,
            icon: Ext.Msg.QUESTION
        } );
    }                     
} );

var uriToDeleteField = Ext.create('Ext.form.field.Text', {
    width: 400,
    emptyText: tr( 'Record URI' )
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
             buttonResetMetamodel, {xtype:'tbspacer', height: 10}, 
             {layout:'hbox', border: false, items: [buttonResetLO, {xtype:'tbspacer', width: 10}, 
                       uriField, {xtype:'tbspacer', width: 10}]},
             {layout:'hbox', border: false, items: [ {xtype:'tbspacer', width: 160}, resultLabel ] },
             { xtype: 'label', text: tr( 'Suppression' ), margin: '20 0 10 0', cls: 'sectionTitle'},
             {layout:'hbox', border: false, items: [buttonDeleteLO, {xtype:'tbspacer', width: 10}, 
                       uriToDeleteField, {xtype:'tbspacer', width: 10}]}
    ]            
} );

var otherPanel = Ext.create('Ext.panel.Panel', {
    layout: 'vbox',
    border: false,
    items: resetPanel
} );

