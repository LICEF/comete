// By default, the interface is displayed in English.
var lang = ( Ext.getUrlParam( 'lang' ) || 'en' );
if (lang != 'en' && lang != 'fr')
    lang = 'en';

var logo = Ext.create('Ext.Img', {
    src:'images/Logo_CERES.png',
    width: 72,
    height: 28,
    listeners: {
        el: {             
            click: function() { window.location =  "index.jsp?lang=" + lang; },
            mouseenter: function(evt) { Ext.get(evt.target).setStyle( 'cursor', 'pointer' ) },
            mouseleave: function(evt) { Ext.get(evt.target).setStyle( 'cursor', '') }
        } 
    }
} );

var adminButton = Ext.create('Ext.Component', {
    margin: '5 0 0 0',
    html: '<a href="javascript:gotoAdmin();">' + tr( 'Administration' ) + '</a>'
} );

var englishButton = Ext.create('Ext.Component', {
    margin: '5 0 0 0',
    html: '<a href="javascript:gotoEnglish();">English</a>',
    hidden: 'en' == lang
} );

var frenchButton = Ext.create('Ext.Component', {
    margin: '5 0 0 0',
    html: '<a href="javascript:gotoFrench();">Français</a>',
    hidden: 'fr' == lang
} );

var aboutButton = Ext.create('Ext.Component', {
    margin: '5 0 0 0',
    html: '<a href="javascript:showAbout();">' + tr( 'About' ) + '</a>'
} );

var signinButton = Ext.create('Ext.Component', {
    margin: '5 0 0 0',
    html: '<a href="javascript:login();">' + tr( 'Sign in' ) + '</a>'
} );

var logoutButton = Ext.create('Ext.Component', {
    margin: '5 0 0 0',
    html: '<a href="javascript:logout();">' + tr( 'Sign out' ) + '</a>'
} );

var tbar = {
    xtype: 'toolbar',
    style: { background: '#c9c9c9'},
    height: 40,   
    items: [ {xtype: 'tbspacer', width: 5}, logo, {xtype: 'tbseparator', height: 30, margin: '0 10 0 8'},
             aboutButton, {xtype: 'tbspacer', width: 10}, 
             adminButton, '->', 
             signinButton, logoutButton, 
             {xtype: 'tbspacer', width: 10}, englishButton, frenchButton, {xtype: 'tbspacer', width: 10} ]
};

var tbarAdmin = {
    xtype: 'toolbar',
    style: { background: '#c9c9c9'},
    height: 40,   
    items: [ {xtype: 'tbspacer', width: 5}, logo, {xtype: 'tbseparator', height: 30, margin: '0 10 0 8'},
             aboutButton, '->', 
             signinButton, logoutButton, 
             {xtype: 'tbspacer', width: 10}, englishButton, frenchButton, {xtype: 'tbspacer', width: 10} ]
};

function updateToolbar() {
    signinButton.setVisible( accountRole == "none");
    logoutButton.setVisible( accountRole != "none" );
    adminButton.setVisible( accountRole != "none" );
}

function gotoAdmin() {
    window.location = 'admin.jsp?lang=' + lang;
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

function gotoEnglish() {
    window.location = getPageLocation('en');
}

function gotoFrench() {
    window.location = getPageLocation('fr');
}
