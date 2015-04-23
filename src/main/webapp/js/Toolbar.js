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

function gotoAdminPage() {
    window.location = 'admin.jsp?lang=' + lang;
}

var adminPageLabel = Ext.create('Comete.ClickableLabel', {
    text: tr('Admin'),
    cls: 'choice',
    selected: false,
    hidden: true,
    fn: gotoAdminPage,
    selectable: false
});

var pageMenu = Ext.create('Ext.panel.Panel', {
    layout: 'hbox',
    border: false,
    items: [ adminPageLabel ]
});

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
    handler: showAbout
} );

var signinButton = Ext.create('Ext.button.Button', {
    text: tr( 'Sign in' ),
    hidden: true,
    textAlign: 'left',
    handler: login
} );

var logoutButton = Ext.create('Ext.button.Button', {
    text: tr( 'Sign out' ),
    hidden: true,
    textAlign: 'left',
    handler: logout
} );

var pageMenuSeparator = Ext.create( 'Ext.toolbar.Separator', {
    height: 30
} );

var tbar = {
    xtype: 'toolbar',
    height: 40,   
    items: [ logo, , {xtype: 'tbspacer', width: 5}, pageMenuSeparator, pageMenu, '->', 
             englishButton, frenchButton, aboutButton, signinButton, logoutButton, {xtype: 'tbspacer', width: 2} ]
};

var tbarAdmin = {
    xtype: 'toolbar',
    height: 40,   
    items: [ logo, {xtype: 'tbspacer', width: 5}, {xtype: 'tbseparator', height: 30}, 
             adminLabel, '->',  englishButton, frenchButton, aboutButton, signinButton, logoutButton, {xtype: 'tbspacer', width: 2} ]
};

function updateToolbar() {
    signinButton.setVisible( accountRole == "none");
    logoutButton.setVisible( accountRole != "none" );
    pageMenuSeparator.setVisible( accountRole != "none" );
    adminPageLabel.setVisible( accountRole != "none" );
    pageMenu.setVisible( accountRole != "none" );
}

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

function login() {
    var loginDialog = Ext.create('Comete.LoginDialog', {
        width: 300,
        height: 170,
        modal: true,
        resizable: false
    });
    loginDialog.show();        
}

function logout() {
    Ext.Ajax.request( {
        url: 'rest/security/logout',
        method: 'GET',
        success: function(response) {
            if( response.status == 200 )
                window.location.reload();
            else
                Ext.Msg.alert( tr( 'Failure' ), tr( 'Attempt to log out failed.' ) );
        },
        scope: this 
    } );
}
