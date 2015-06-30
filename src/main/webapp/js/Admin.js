function getPageLocation(lg) {
    return "admin.jsp?lang=" + lg;         
}

var currentElem;

function changeCardItem(item, elem) {
    var cardPanel = Ext.getCmp( 'cardPanel' );
    var currentItem = cardPanel.getLayout().getActiveItem();
    if( currentItem.beforeQuit )
        currentItem.beforeQuit();

    cardPanel.getLayout().setActiveItem(item);
    currentElem.removeCls('selectedChoice');
    currentElem.addCls('choice');
    elem.removeCls('choice');
    elem.addCls('selectedChoice');
    currentElem.selected = false;
    currentElem = elem;

    var activeItem = cardPanel.getLayout().getActiveItem();
    if( activeItem.updateData )
        activeItem.updateData();
}

Ext.onReady( function() {    
    Ext.QuickTips.init();
    setAccountRole(function(){init();});
} );

function init() {
    // By default, the interface is displayed in English.
    var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
    utilsInit(lang);      

    var importerLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Import'),
        cls: 'selectedChoice',
        selected: true,
        fn: function() { changeCardItem(0, importerLabel); }
    } );

    currentElem = importerLabel;

    var harvestLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Harvest'),
        cls: 'choice',
        selected: false,
        hidden: !isAdmin(),
        fn: function() { changeCardItem(1, harvestLabel); }
    } );

    var identityLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Identity Management'),
        cls: 'choice',
        selected: false,
        hidden: !isAdmin(),
        fn: function() { changeCardItem(2, identityLabel); }
    } );

    var vocabularyLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Vocabulary Management'),
        cls: 'choice',
        selected: false,
        hidden: !isAdmin(),
        fn: function() { changeCardItem(3, vocabularyLabel); }
    } );

    var recordValidationLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Record Validation'),
        cls: 'choice',
        selected: false,
        hidden: !isAdmin(),
        fn: function() { changeCardItem(4, recordValidationLabel); }
    } );

    var brokenLinksLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Broken Links Management'),
        cls: 'choice',
        selected: false,
        hidden: !isAdmin(),
        fn: function() { changeCardItem(5, brokenLinksLabel); }
    } );

    var miscLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Maintenance'),
        cls: 'choice',
        selected: false,
        hidden: !isAdmin(),
        fn: function() { changeCardItem(6, miscLabel); }
    } );

    var choicePanel = Ext.create('Ext.panel.Panel', {
        layout: 'hbox',         
        border: false,
        height: 40,
        margin: '10 0 0 0',
        items: [ {xtype: 'tbspacer', width: 10}, importerLabel,
                 {xtype: 'tbspacer', width: 10}, harvestLabel,
                 {xtype: 'tbspacer', width: 10}, identityLabel, 
                 {xtype: 'tbspacer', width: 10}, vocabularyLabel, 
                 {xtype: 'tbspacer', width: 10}, recordValidationLabel, 
                 {xtype: 'tbspacer', width: 10}, brokenLinksLabel, 
                 {xtype: 'tbspacer', width: 10}, miscLabel ]
    });    

    var importerPanel = Ext.create('Comete.AdminImport', {
        border: false,
        lang: lang
    } );

    var harvestPanel = Ext.create('Comete.AdminHarvest', {
        border: false,
        lang: lang
    } );

    var adminIdentityPanel = Ext.create('Comete.AdminIdentity', {
        border: false,
        lang: lang
    } );

    var adminVocPanel = Ext.create('Comete.AdminVoc', {
        border: false,
        lang: lang
    } );

    var adminRecordValidationPanel = Ext.create('Comete.AdminRecordValidation', {
        border: false,
        lang: lang
    } );

    var brokenLinksPanel = Ext.create('Comete.BrokenLinkManager', {
        border: false,
        lang: lang
    } );

    var maintenancePanel = Ext.create('Comete.AdminMaintenance', {
        border: false,
        lang: lang
    } );

    var cardPanel = Ext.create('Ext.panel.Panel', {
        id: 'cardPanel',
        layout: 'card',
        region: 'center',
        border: false,
        items: [ importerPanel, harvestPanel, adminIdentityPanel, adminVocPanel, adminRecordValidationPanel, brokenLinksPanel, maintenancePanel ]
    });

    var contentPanel = Ext.create('Ext.panel.Panel', {
        layout: 'border',
        region: 'center',
        border: false,
        hidden: !isPublisher(),
        items: [ { region: 'north', border: false, items: [ choicePanel ] }, cardPanel ]
    });

    new Ext.Viewport( {
        layout: 'border',
        items: [ {
            layout: 'border',
            border: false,
            region: 'center',
            tbar: tbarAdmin,
            items: [ contentPanel ]
        } ]        
    } );

    updateToolbar();
}
