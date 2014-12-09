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

var frenchButton = Ext.create('Ext.button.Button', {
    text: '&nbsp;Français',
    handler: function() { window.location = "index.jsp?lang=fr"; },
    hidden: 'fr' == lang
} );

var englishButton = Ext.create('Ext.button.Button', {
    text: '&nbsp;English',
    handler: function() { window.location = "index.jsp?lang=en"; },
    hidden: 'en' == lang
} );

var searchButton = Ext.create('Ext.button.Button', {
    icon: 'images/search.gif',
    text: '&nbsp;' + tr( 'Search' ),
    textAlign: 'left',
    handler: function() { window.location = "Search.jsp?lang=" + lang; }
} );

var addButton = Ext.create('Ext.button.Button', {
    text: '&nbsp;' + tr( 'Adding' ),
    icon: 'images/pencil.png',
    textAlign: 'left',
    handler: function() { 
        if (!authorized) {
            Ext.MessageBox.alert(tr('Security'), tr('Unauthorized access.'));
            return;
        }
    
        window.location = "AddRecords.jsp?lang=" + lang;  
    }
} );
    
var adminButton = Ext.create('Ext.button.Button', {
    text: '&nbsp;' + tr( 'Administration' ),
    icon: 'images/gear.png',
    textAlign: 'left',
    handler: function() { 
        if (!authorized) {
            Ext.MessageBox.alert(tr('Security'), tr('Unauthorized access.'));
            return;
        }

        window.location = "Admin.jsp?lang=" + lang; 
   }
} );

var aboutButton = Ext.create('Ext.button.Button', {
    text: tr( 'About' ),
    icon: 'images/about.png',
    textAlign: 'left',
    handler: function() { showAbout(); }
} );

var tbarSearch = {
    xtype: 'toolbar',
    height: 40,   
    items: [ logo, '->',  englishButton, frenchButton, aboutButton, {xtype: 'tbspacer', width: 2} ]
};

var tbarAdmin = {
    xtype: 'toolbar',
    height: 40,
    items: [ logo, {xtype: 'tbspacer', width: 5}, {xtype: 'tbseparator', height: 30}, {xtype: 'tbspacer', width: 2}, 
             addButton, adminButton, '->', englishButton, frenchButton, aboutButton, {xtype: 'tbspacer', width: 2} ]
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
