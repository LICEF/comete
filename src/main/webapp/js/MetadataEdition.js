function editMetadataRecordAs( loId, metadataFormat ) {
    var form = 'lom-full';
    if( 'dc' == metadataFormat )
        form = 'oaidc-full';
    var src = this.metadataEditorUrl + '/fr/ori-md-editor/' + form + '/edit/' + metadataFormat + ":" + loId + '?fr-language=' + this.lang;
    var htmlTemplate = '<iframe width="100%" height="100%" src="{0}" frameborder="0"></iframe>';
    var html = Ext.String.format( htmlTemplate, src );
    editorPanel.getEl().setHTML(html);
}

function newMetadata( metadataFormat ) {
    Ext.Ajax.request( {
        url: '/Portal/server/newMetadataRecord',
        params: 'format=' + metadataFormat,
        method: 'POST',
        success: function( response, options ) {
            var data = Ext.decode( response.responseText );
            if( data.loId != null )
                editMetadataRecordAs( data.loId, metadataFormat );
            else
                Ext.Msg.alert( tr( 'Warning' ), Ext.String.format( tr( 'An error occurred when performing {0}.' ), 'newMetadataRecord' ) );
        },
        failure: function( response, options ) {
            Ext.Msg.alert( tr( 'Warning' ), Ext.String.format( tr( 'An error occurred when performing {0}.' ), 'newMetadataRecord' ) );
        }
    } );
}

var newRecordButton = Ext.create('Ext.button.Button', { 
    text: 'New LOM',
    handler: function() { newMetadata('lom');
}
} );

var controlPanel = Ext.create('Ext.panel.Panel', {
    region: 'north',
    layout: 'hbox',
    margin: 10,
    border: false,
    items: newRecordButton
} );

var editorPanel = Ext.create('Ext.panel.Panel', {
    region: 'center',
    border: false
} );

var metadataEditorWrapper = Ext.create('Ext.panel.Panel', {
    layout: 'border',
    border: false,
    bodyStyle: 'background-color: white',
    items:[ controlPanel, editorPanel ]
} );

