function init() {
    
    // By default, the interface is displayed in English.
    //var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
    //var isEditable = ( ( 'true' == Ext.getUrlParam( 'editable' ) ) || false );
    //var query = Ext.getUrlParam( 'query' );

    var waitDialog = Ext.create('Ext.window.MessageBox', {       
    });

    var uploadFile = Ext.create('Ext.form.field.File', {
        name: 'file',
        buttonText: tr('Browse') + '...',
        width: 400
    });

    var resultLabel = Ext.create('Ext.form.Label', {
        id: 'result',
        padding: 10, 
        hidden: true
    });

    var uploadButton = Ext.create('Ext.button.Button', {
        text: tr('Upload'),
        disabled: !authorized,
        handler: function() {
            resultLabel.setVisible(false);            
            waitDialog.wait( tr('Please wait') + '...' );
            var uploadForm = uploadPanel.getForm();    
            if (uploadForm.isValid()) {
                uploadForm.submit({ 
                    url: 'rest/metadataRecords',
                    success: function(form, action) {
                        /*var res = '';
                        for (i = 0; i < action.result.data.length; i++)
                            res += '<br/><a href="' + action.result.data[i].uri + '" target="_blank">' + action.result.data[i].uri + '</a> ' + action.result.data[i].state;
                        Ext.get('result').update( res ); 
                        resultLabel.setVisible(true); */
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
                { xtype: 'tbspacer', height: 10 }, uploadFile, uploadButton ]               
    });

    var importer = Ext.create('Ext.panel.Panel', {
        layout: 'vbox',
        border: false,
        items:[ uploadPanel, resultLabel ]
    } );

    Ext.create('Ext.Panel', {
        layout: 'fit',
        renderTo: Ext.getBody(),
        items: [ importer ]
    });
    
}

Ext.application({
    name: 'Comete',
    launch: function() {
        setAuthorized(function(){init();})
    }
});

