function getPageLocation(lg) {
    return "Admin.jsp?lang=" + lg;         
}

var currentElem;

function changeCardItem(item, elem) {
    if (!authorized) {
        Ext.MessageBox.alert(tr('Security'), tr('Unauthorized access.'));
        return;
    }

    var cardPanel = Ext.getCmp( 'cardPanel' );
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
    setAuthorized(function(){init();})
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

    var identityLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Identity Management'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(1, identityLabel); }
    } );

    var vocabularyLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Vocabulary Management'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(2, vocabularyLabel); }
    } );

    var recordValidationLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Record Validation'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(3, recordValidationLabel); }
    } );

    var brokenLinksLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Broken Links Management'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(4, brokenLinksLabel); }
    } );

    var miscLabel = Ext.create('Comete.ClickableLabel', {
        text: tr('Other'),
        cls: 'choice',
        selected: false,
        fn: function() { changeCardItem(5, miscLabel); }
    } );

    var choicePanel = Ext.create('Ext.panel.Panel', {
        layout: 'hbox',         
        border: false,
        height: 40,
        margin: '10 0 0 0',
        items: [ {xtype: 'tbspacer', width: 10}, importerLabel,
                 {xtype: 'tbspacer', width: 10}, identityLabel, 
                 {xtype: 'tbspacer', width: 10}, vocabularyLabel, 
                 {xtype: 'tbspacer', width: 10}, recordValidationLabel, 
                 {xtype: 'tbspacer', width: 10}, brokenLinksLabel, 
                 {xtype: 'tbspacer', width: 10}, miscLabel ]
    });    

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

    var cardPanel = Ext.create('Ext.panel.Panel', {
        id: 'cardPanel',
        layout: 'card',
        region: 'center',
        border: false,
        items: [ importer, adminIdentityPanel, adminVocPanel, adminRecordValidationPanel, brokenLinksPanel, otherPanel ]
    });

    var contentPanel = Ext.create('Ext.panel.Panel', {
        layout: 'border',
        region: 'center',
        border: false,
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
}
