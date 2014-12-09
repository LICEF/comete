Ext.define( 'Comete.IdentityEditor', {
    extend: 'Ext.window.Window',
    layout: 'border',           
    initComponent: function( config ) {

        this.identityDetails = Ext.create('Comete.IdentityDetails', {
            margin: '15 0 0 10',
            border: false,
            type: this.type,
            mode: 'edition'
        }); 

        cfg = {
            title: (this.type == 'person')?tr('Person Editor'):tr('Organization Editor'),
            buttons: [ {text:'OK', handler: this.ok, scope: this}, {text:tr('Cancel'), handler: this.close, scope: this}],
            items: [ { region: 'north', border: false, layout: 'hbox',
                       items: { xtype: 'label', height: 50, padding: '10 0 0 10', 
                                style: 'font-size: 14px', text: tr('Select and/or fill values') } }, 
                     { layout: 'fit', region: 'center', border: false,
                       items: this.identityDetails }  ]
                     
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        //init values of fields
        this.loadValues();  
    },
    loadValues: function() {
        var restUrl = this.restUrl;
        Ext.Ajax.request( {
            url: restUrl + '/allDetails',
            method: 'GET',
            success: function(response, opts) {
                details = Ext.JSON.decode(response.responseText, true).details;
                this.identityDetails.setValues(details);                   
            },
            scope: this 
        } );
    },    
    ok: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        var mainValues = this.identityDetails.getMainValues();
        Ext.Ajax.request( {
            url: this.restUrl,
            params: { mainValues: JSON.stringify(mainValues) },
            method: 'PUT',
            success: function(response, opts) {
                this.close();       
                waitDialog.close();           
                this.parent.afterEdit();
            },
            failure: function(response, opts) {
                this.close();
                waitDialog.close();   
                Ext.Msg.alert('Failure', response.responseText);
            },
            scope: this 
        } );
    }        
});


Ext.define( 'Comete.IdentityMerger', {
    extend: 'Ext.window.Window',
    layout: 'border',           
    initComponent: function( config ) {

        this.identityDetails = Ext.create('Comete.IdentityDetails', {
            margin: '15 0 0 10',
            border: false,
            type: this.type,
            mode: 'edition'
        }); 

        cfg = {
            title: (this.type == 'person')?tr('Person Merger'):tr('Organization Merger'),
            buttons: [ {text:tr('Merge'), handler: this.doMerge, scope: this}, {text:tr('Cancel'), handler: this.close, scope: this}],
            items: [ { region: 'north', border: false, layout: 'hbox',
                       items: { xtype: 'label', height: 50, padding: '10 0 0 10', 
                                style: 'font-size: 14px', text: tr('Select and/or fill values for the merged result') } }, 
                     { layout: 'fit', region: 'center', border: false,
                       items: this.identityDetails }  ]
                     
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 

        //init values of fields
        this.loadValues();  
    },
    loadValues: function() {
        var url = (this.type == 'person')?
            identityUrl + '/rest/persons/preMergeDetails':
            identityUrl + '/rest/organizations/preMergeDetails';
        Ext.Ajax.request( {
            url: url,
            params: { uris: JSON.stringify(this.uris) },
            method: 'GET',
            success: function(response, opts) {
                details = Ext.JSON.decode(response.responseText, true).details;
                this.identityDetails.setValues(details);                   
            },
            scope: this 
        } );
    },    
    doMerge: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {       
        });
        waitDialog.wait( tr('Please wait') + '...' );
        var mainValues = this.identityDetails.getMainValues();
        var url = (this.type == 'person')?
            identityUrl + '/rest/persons/merge':
            identityUrl + '/rest/organizations/merge'; 
        var params = {
            uris: JSON.stringify(this.uris),
            mainValues: JSON.stringify(mainValues)
        };
        if (this.similarGroup != 'none')
            params.similarGroup = this.similarGroup;
        Ext.Ajax.request( {
            url: url,
            params: params,
            method: 'POST',
            success: function(response, opts) {
                this.close();       
                waitDialog.close();           
                this.parent.afterMerge(response.responseText);
            },
            failure: function(response, opts) {
                this.close();       
                waitDialog.close();           
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );
    }        
});
