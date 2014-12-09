function getPageLocation(lg) {
    return "AddRecords.jsp?lang=" + lg;         
}

var currentElem;

function changeCardItem(item, elem) {
    if (!authorized) {
        Ext.MessageBox.alert(tr('Security'), tr('Unauthorized access.'));
        return;
    }
    
    Ext.getCmp('cardPanel').getLayout().setActiveItem(item);
    currentElem.removeCls('selectedChoice');
    currentElem.addCls('choice');
    elem.removeCls('choice');
    elem.addCls('selectedChoice');
    currentElem.selected = false;
    currentElem = elem;
}

Ext.onReady( function() {    
    Ext.QuickTips.init();
    setAuthorized(function(){init();})
} );

function init() {

    // By default, the interface is displayed in English.
    var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
    var isEditable = ( ( 'true' == Ext.getUrlParam( 'editable' ) ) || false );
    var query = Ext.getUrlParam( 'query' );

    var htmlTemplate = '<iframe width="100%" height="100%" src="{0}" frameborder="0"></iframe>';

    this.sectionLabel = Ext.create('Ext.form.Label', {
        text: tr( 'Resources' ),
        margin: '10 0 0 10',
        cls: 'sectionTitle'
    } );

    var sectionPanel = Ext.create('Ext.panel.Panel', {
        layout: 'vbox',
        region: 'north',
        border: false,     
        height: 50,   
        items: [ 
            sectionLabel, 
            { height: 1, width: '100%', margin: '5 10 0 10', border: true, 
              bodyStyle: 'border-color:#8374B0' }
        ]
    } );

    var importLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Import'),
        cls: 'selectedChoice',
        selected: true,
        fn: function() { changeCardItem(0, importLabel); }
    } );

    currentElem = importLabel;

    var editionLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Metadata Edition'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(1, editionLabel); }
    } );

    var harvestLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Harvest'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(2, harvestLabel); }
    } );

    var choicePanel = Ext.create('Ext.panel.Panel', {
        layout: 'hbox',         
        region: 'north',
        border: false,
        height: 40,
        items: [ {xtype: 'tbspacer', width: 10}, importLabel, {xtype: 'tbspacer', width: 10}, 
                  editionLabel, {xtype: 'tbspacer', width: 10}, harvestLabel ]
    });    

    var cardPanel = Ext.create('Ext.panel.Panel', {
        id: 'cardPanel',
        layout: 'card',
        region: 'center',
        border: false,
        items: [ importer, metadataEditorWrapper,
                 {html: Ext.String.format( htmlTemplate, '/Harvester/admin/listharvest.html') } ]
    });

    var contentPanel = Ext.create('Ext.panel.Panel', {
        layout: 'border',
        region: 'center',
        border: false,
        items: [ choicePanel, cardPanel ]
    });

    new Ext.Viewport( {
        layout: 'border',
        items: [ {
            layout: 'border',
            border: false,
            region: 'center',
            tbar: tbarLogo,
            items: [ sectionPanel, contentPanel ]
        } ]        
    } );

    //cause buttons created at previous declaration -AM
    uploadButton.setDisabled(!authorized); 
    newRecordButton.setDisabled(!authorized); 

}
