﻿var waitDialog = Ext.create('Ext.window.MessageBox', {       
});

var uploadFile = Ext.create('Ext.form.field.File', {
    name: 'file',
    buttonText: tr('Browse') + '...',
    width: 400
} );

var resultLabel = Ext.create('Ext.form.Label', {
    id: 'result',
    padding: 10, 
    hidden: true
} );

var uploadButton = Ext.create('Ext.button.Button', {
    text: tr('Upload'),
    disabled: !authorized,
    handler: function() {
        resultLabel.setVisible(false);            
        waitDialog.wait( tr('Please wait') + '...' );
        var uploadForm = uploadPanel.getForm();    
        if (uploadForm.isValid()) {
            uploadForm.submit({ 
                url: metadataUrl + '/rest/metadataRecords',
                success: function(form, action) {
                   var res = '';
                   for (i = 0; i < action.result.data.length; i++)
                       res += '<br/><a href="' + action.result.data[i].uri + '" target="_blank">' + action.result.data[i].uri + '</a> ' + action.result.data[i].state;
                   Ext.get('result').update( res ); 
                   resultLabel.setVisible(true); 
                   waitDialog.close();
                },
                failure: function(form, action) {
                   Ext.Msg.alert('Failure', action.result.error);
                   waitDialog.close();
                }
            });
        }
    }      
} );

var uploadPanel = Ext.create('Ext.form.Panel', {
    padding: 10,
    border: false,    
    items:[ { border: false, html: tr('Select LOM or zip of LOMs.<br/>Metametadata identifier (field 3.1) <b>must be present</b>.') }, 
            { xtype: 'tbspacer', height: 10 }, uploadFile, uploadButton]               
});

var importer = Ext.create('Ext.panel.Panel', {
    layout: 'vbox',
    border: false,
    items:[ uploadPanel, resultLabel]
} );

