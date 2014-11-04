/***********************/
/*** Authorized flag ***/
/***********************/
var authorized = false;
function setAuthorized(callback) {
    Ext.Ajax.request( {
        url: '/comete/rest/security/isAuthorized',
        method: 'GET',
        success: function(response, opts) {
            authorized = response.responseText == 'true';
            callback.call();
        }
    } );
}


