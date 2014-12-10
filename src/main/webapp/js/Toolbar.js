// By default, the interface is displayed in English.
var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
if (lang != 'en' && lang != 'fr')
    lang = 'en';

var logo = Ext.create('Ext.Img', {
    src:'images/cometeLogo2.gif',
    width: 106,
    height: 28,
    listeners: {
        el: {             
            click: function() { window.location =  "index.jsp?lang=" + lang; },
            mouseenter: function(evt) { Ext.get(evt.target).setStyle( 'cursor', 'pointer' ) },
            mouseleave: function(evt) { Ext.get(evt.target).setStyle( 'cursor', '') }
        } 
    }
} );

var adminLabel = Ext.create('Ext.form.Label', {
        text: tr( 'Administration' ),
        cls: 'sectionTitle'
    } );

var frenchButton = Ext.create('Ext.button.Button', {
    text: '&nbsp;Français',
    handler: function() { window.location = getPageLocation('fr'); },
    hidden: 'fr' == lang
} );

var englishButton = Ext.create('Ext.button.Button', {
    text: '&nbsp;English',
    handler: function() { window.location = getPageLocation('en'); },
    hidden: 'en' == lang
} );

var aboutButton = Ext.create('Ext.button.Button', {
    text: tr( 'About' ),
    icon: 'images/about.png',
    textAlign: 'left',
    handler: function() { showAbout(); }
} );

var tbar = {
    xtype: 'toolbar',
    height: 40,   
    items: [ logo, '->',  englishButton, frenchButton, aboutButton, {xtype: 'tbspacer', width: 2} ]
};

var tbarAdmin = {
    xtype: 'toolbar',
    height: 40,   
    items: [ logo, {xtype: 'tbspacer', width: 5}, {xtype: 'tbseparator', height: 30}, 
             adminLabel, '->',  englishButton, frenchButton, aboutButton, {xtype: 'tbspacer', width: 2} ]
};

function showAbout() {
    var aboutWindow = new Ext.window.Window( {
        title: tr('About'),
        width: 400,
        height: 410,
        resizable: false,
        html: '<iframe width="100%" height="100%" frameborder="0" src="' + lang + '/about.html"></iframe>' 
    } );
    aboutWindow.show();     
}
