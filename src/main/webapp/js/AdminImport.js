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
            disabled: !authorized,
            handler: function() {
                resultLabel.setVisible(false);            
                this.waitDialog.wait( tr('Please wait') + '...' );
                var uploadForm = this.uploadPanel.getForm();    
                if (uploadForm.isValid()) {
                    uploadForm.submit({ 
                        url: 'rest/metadataRecords',
                        success: function(form, action) {
                            var res = '';
                            for (i = 0; i < action.result.data.length; i++)
                                res += '<br/><a href="' + action.result.data[i].uri + '" target="_blank">' + action.result.data[i].uri + '</a> ' + action.result.data[i].state;
                            Ext.get('result').update( res ); 
                            this.resultLabel.setVisible(true); 
                            this.waitDialog.close();
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

        this.uploadPanel = Ext.create('Ext.form.Panel', {
            padding: 10,
            border: false,    
            items:[ { border: false, html: tr('Select LOM or zip of LOMs.<br/>Metametadata identifier (field 3.1) <b>must be present</b>.') }, 
                    { xtype: 'tbspacer', height: 10 }, this.uploadFile, this.uploadButton]               
        });

        var cfg = {
            items: [ this.uploadPanel, this.resultLabel ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    }
} );


