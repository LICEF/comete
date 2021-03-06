﻿Ext.define( 'Comete.AdminIdentity', {
    extend: 'Ext.panel.Panel',
    layout: 'fit',  
    initComponent: function( config ) {

        this.persons = Ext.create('Comete.Identity', {
            layout: 'fit',
            border: false,            
            type: 'person',
            parent: this
        } );

        this.similarPerson = Ext.create('Comete.SimilarIdentity', {
            layout: 'fit',
            border: false,            
            type: 'person',
            parent: this
        } );

        this.orgs = Ext.create('Comete.Identity', {
            layout: 'fit',
            border: false,
            type: 'org',
            parent: this
        } );

        this.similarOrg = Ext.create('Comete.SimilarIdentity', {
            layout: 'fit',
            border: false,
            type: 'org',
            parent: this
        } );

        this.mainPanel = Ext.create('Ext.tab.Panel', {
            activeTab: 0, 
            plain: true,          
            margin: '0 -1 -1 -1',
            items: [ 
                     {
                       id: 'managePersons',
                       title: tr('Persons'),
                       layout: 'fit',
                       items: this.persons
                     },
                     {
                       id: 'manageSimilarPersons',
                       title: tr('Similar Persons'),
                       layout: 'fit',
                       items: this.similarPerson
                     },
                     {
                       id: 'manageOrgs',
                       title: tr('Organizations'),
                       layout: 'fit',
                       items: this.orgs
                     },
                     {
                       id: 'manageSimilarOrgs',
                       title: tr('Similar Organizations'),
                       layout: 'fit',
                       items: this.similarOrg
                     }
                   ]        
        });

        var cfg = {
            items: this.mainPanel
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    initList: function(type) {
        if (type == 'person')
            this.persons.retrieveIdentities();
        else    
            this.orgs.retrieveIdentities();
    },
    initSimilarity: function(type) {
        if (type == 'person')
            this.similarPerson.initSimilarity();
        else    
            this.similarOrg.initSimilarity();
    }
} );

Ext.define( 'Comete.Identity', {
    extend: 'Ext.panel.Panel',        
    initComponent: function( config ) {
        
        this.proxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: (this.type == 'person')?'persons':'organizations',
                totalProperty: 'totalCount'
            }
        });

        this.store = Ext.create('Ext.data.JsonStore', {
            model: (this.type == 'person')?'PersonModel':'OrganizationModel',
            pageSize: 20,
            proxy: this.proxy
        });

        this.store.sort('label', 'ASC');        
        this.store.on( 'load', this.maybeSelect, this );

        this.searchField = Ext.create('Ext.form.field.Text', {
            enableKeyEvents: true,
            region: 'center',
            margin: '10 0 0 10'
        });
 
        this.searchField.on( 'keyup', this.retrieveIdentities, this );

        this.allButton = Ext.create('Ext.button.Button', {
            text: tr('All'),
            region: 'east',
            margin: '10 10 0 10',
            handler: function() { this.retrieveIdentities(true); },
            scope: this
        } );

        this.pageBar = Ext.create('Ext.toolbar.Paging', {
            store: this.store,
            firstText: tr('First Page'),
            prevText: tr('Previous Page'),
            nextText: tr('Next Page'),
            lastText: tr('Last Page'),
            afterPageText: tr('of {0}')
        } );
        //modifications of pageBar
        this.pageBar.remove( this.pageBar.getComponent(10) ); //refresh button
        this.pageBar.remove( this.pageBar.getComponent(9) ); //separator

        var sm = Ext.create('Ext.selection.RowModel', {
            mode: 'MULTI'
        });

        var identityContextMenu = Ext.create('Ext.menu.Menu', {
            items: [ { text: tr('Edit'),
                       handler: this.editIdentity,
                       scope: this },
                     { text: tr('Merge'),
                       handler: this.merge,
                       scope: this } ]
        });

        if (this.type == 'person') 
            identityContextMenu.add( { text: tr('Convert as organization'),
                                       handler: this.convertToOrg,
                                       scope: this } );

        
        this.identityList = Ext.create('Ext.grid.Panel', {                        
            margin: '10 10 10 10',
            store: this.store,
            columns: [ 
                { dataIndex: 'label', text: (this.type == 'person')?tr('Persons'):tr('Organizations'), flex: 1 }
            ],  
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
                listeners: {
                    itemcontextmenu: function(view, rec, node, index, event) {
                        event.stopEvent(); // stops the default event. i.e. Windows Context Menu
                        var oneSelection = il.getSelectionModel().getSelection().length == 1;
                        identityContextMenu.getComponent(0).setDisabled(!oneSelection);
                        identityContextMenu.getComponent(1).setDisabled(oneSelection);
                        identityContextMenu.showAt(event.getXY()); // show context menu where user right clicked
                        return false;
                    }
                }
            },
            selModel: sm,
            bbar: this.pageBar
        });

        var il = this.identityList;
        var type = this.type;

        this.identityList.on( 'selectionchange', this.identityChanged, this );

        this.personsPanel = Ext.create('Ext.panel.Panel', {
            layout: 'border',
            border: false,
            items: [ { layout: 'border', region: 'north', border: false, height: 34, 
                       items: [ { layout: 'fit', region: 'center', border: false, items: this.searchField }, 
                                { region: 'east', border: false, items: this.allButton } ] },
                     { layout: 'fit', region: 'center', border: false, items: this.identityList } ]
        });

        this.identityDetails = Ext.create('Comete.IdentityDetails', {
            margin: '15 0 0 10',
            border: false,
            type: this.type,
            mode: 'read'
        }); 

        this.mainPanel = Ext.create('Ext.panel.Panel', {
            layout: 'border',
            border: false,
            items: [ {layout: 'fit', region: 'west', width: 350, border: false,
                      split: true, items: this.personsPanel }, 
                     {layout: 'fit', region: 'center', border: false,
                      items: this.identityDetails} ]
        });

        var cfg = {
            items: this.mainPanel
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    retrieveIdentities: function(unuseText) {
        var text = this.searchField.getValue();
        var url = this.proxy.url = 'rest/' + ((this.type == 'person')?'persons':'organizations');
        if (unuseText != true && text != "")
            url = url + '/search?q=' + text;
        this.proxy.url = url;
        this.store.loadPage(1);
        if (unuseText == true)
            this.searchField.setValue("");
    },
    identityChanged: function(model, selected) {
        this.identityDetails.clear();
        if (selected.length == 1 ) {
            Ext.Ajax.request( {
                url: selected[0].getData().restUrl + '/details',
                method: 'GET',
                success: function(response) {
                    if (this.type == 'person')
                        jsonDetails = Ext.JSON.decode(response.responseText, true).personDetails[0];
                    else
                        jsonDetails = Ext.JSON.decode(response.responseText, true).organizationDetails[0];
                    this.identityDetails.setValues(jsonDetails);
                },
            scope: this 
            } );
        }
    },
    editIdentity: function() {
        var records = this.identityList.getSelectionModel().getSelection();
        this.selectedIdentityId = records[0].data.id;
        var editor = Ext.create('Comete.IdentityEditor', {
            width: 560,
            height: 400,
            type: this.type,            
            modal: true,
            restUrl:  records[0].data.restUrl,
            parent: this
        });
        editor.show();       
    },
    afterEdit: function() {
        this.identityList.getSelectionModel().clearSelections();
        this.store.load();
    },
    maybeSelect: function() {
        if (this.selectedIdentityId != null) {
            var selectedRecord = this.store.getById( this.selectedIdentityId );
            if( selectedRecord ) 
                this.identityList.getSelectionModel().select( [ selectedRecord ] );            
            this.selectedIdentity = null;
        }
    },
    merge: function() {
        var records = this.identityList.getSelectionModel().getSelection();
        if (records.length == 1) { 
            Ext.Msg.alert(tr('Warning'), tr('Merge need at least two identities.'));
            return;
        }
        
        var uris = new Array();
        for (i = 0; i < records.length; i++)
            uris[i] = records[i].data.uri;

        var mergeWizard = Ext.create('Comete.IdentityMerger', {
            width: 560,
            height: 400,
            type: this.type,            
            modal: true,
            uris: uris,
            parent: this
        });
        mergeWizard.show();        
    },
    afterMerge: function(uri) {        
        this.selectedIdentityId = uri.substring(uri.lastIndexOf("/") + 1);
        this.store.load();
        //update related similar list
        this.parent.initSimilarity(this.type);
    },
    convertToOrg: function() {
        var message = 'Selected elements will be converted as organization.<br>It is possible that some of them are included into similar groups<br>if this is the case, they\'re will be taken off.<br>Are you sure you want to proceed ?';

        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            title: tr('Warning'), 
            msg: tr(message), 
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.WARNING,
            fn: this.convertToOrgEff, 
            scope: this 
        });
    },
    convertToOrgEff: function(button) {
        if (button != 'ok')
            return;

        var uris = new Array();
        var records = this.identityList.getSelectionModel().getSelection();
        for (i = 0; i < records.length; i++)
            uris[i] = records[i].data.uri;
        Ext.Ajax.request( {
            url: 'rest/persons/convertToOrg',
            params: { uris: JSON.stringify(uris) },
            method: 'GET',
            success: function(response, opts) {
                this.store.load();     
            },
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText);
            },
            scope: this 
        } );         
    }
} );


Ext.define( 'Comete.SimilarIdentity', {
    extend: 'Ext.panel.Panel',        
    initComponent: function( config ) {

        this.similarGroups = null;
        this.currentGroup = -1;

        this.proxy = Ext.create('Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: (this.type == 'person')?'persons':'organizations'
            }
        });

        this.store = Ext.create('Ext.data.JsonStore', {
            model: (this.type == 'person')?'PersonModel':'OrganizationModel',
            proxy: this.proxy
        });

        this.startButton = Ext.create('Ext.button.Button', {
            text: tr('Start'),
            handler: this.getSimilarGroups,
            scope: this
        } );

        this.previousButton = Ext.create('Ext.button.Button', {
            text: tr('Previous'),
            handler: this.showPreviousGroup,
            hidden: true,
            scope: this
        } );

        this.nextButton = Ext.create('Ext.button.Button', {
            text: tr('Next'),
            handler: this.showNextGroup,
            hidden: true,
            scope: this
        } );

        this.takeOffButton = Ext.create('Ext.button.Button', {
            text: tr('Take Off'),
            disabled: true,
            handler: this.takeOff,
            scope: this
        } );

        this.mergeButton = Ext.create('Ext.button.Button', {
            text: tr('Merge'),
            disabled: true,
            handler: this.merge,
            scope: this
        } );

        var sm = Ext.create('Ext.selection.CheckboxModel');
 
        this.similarityList = Ext.create('Ext.grid.Panel', {            
            margin: '10',
            store: this.store,             
            columns: [ 
                { dataIndex: 'label', flex: 1}
            ],  
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false,
            },
            autoScroll: true,
            selModel: sm,
            bbar: [ this.startButton, this.previousButton, this.nextButton, {xtype: 'tbfill'}, this.takeOffButton, this.mergeButton ]
        });

        this.similarityList.on( 'selectionchange', this.identityChanged, this );

        this.identityDetails = Ext.create('Comete.IdentityDetails', {
            margin: '15 0 0 10',
            border: false,
            type: this.type,
            mode: 'read'
        });

        this.mainPanel = Ext.create('Ext.panel.Panel', {
            layout: 'border',
            border: false,
            items: [ {layout: 'fit', region: 'west', width: 350, border: false,
                      split: true, items: this.similarityList }, 
                     {layout: 'fit', region: 'center', border: false,
                      items: this.identityDetails} ]
        });

        var cfg = {
            items: this.mainPanel
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    initSimilarity: function( text ) {
        this.store.loadRawData([]);
        this.similarGroups = null;
        this.currentGroup = -1;
        this.startButton.setVisible(true);
        this.previousButton.setVisible(false);
        this.nextButton.setVisible(false);
        this.similarityList.columns[0].setText( text );
    },
    getSimilarGroups: function() {   
        if (this.similarGroups == null || this.similarGroups.length == 0) {
            type = (this.type == 'person')?'persons':'organizations';     
            Ext.Ajax.request( {
                url: 'rest/' + type + '/similarGroups',
                method: 'GET',
                success: function(response, opts) {
                    this.similarGroups = Ext.JSON.decode(response.responseText, true).groups;
                    this.startButton.setVisible(this.similarGroups.length == 0);
                    this.previousButton.setVisible(this.similarGroups.length > 0);
                    this.nextButton.setVisible(this.similarGroups.length > 0);
                    if (this.similarGroups.length == 0)
                        this.similarityList.columns[0].setText( tr('No similarity found') );
                    else
                        this.showNextGroup();
                },
                scope: this 
            } );
        }
    }, 
    showPreviousGroup: function() {
        this.currentGroup--;
        if (this.currentGroup == -1)
            this.currentGroup = this.similarGroups.length-1;
        this.showOtherGroup();
    }, 
    showNextGroup: function() {
        this.currentGroup++;
        if (this.currentGroup == this.similarGroups.length)
            this.currentGroup = 0;
        this.showOtherGroup();
    }, 
    showOtherGroup: function() {
        this.similarityList.columns[0].setText( tr('Similar group') + ' ' + (this.currentGroup + 1) + '/' + this.similarGroups.length );

        type = (this.type == 'person')?'persons':'organizations';
        this.proxy.url = 'rest/' + type + '/similar?gid=' + this.similarGroups[this.currentGroup],
        this.store.load();
    }, 
    identityChanged: function(model, selected) {
        this.identityDetails.clear();
        this.mergeButton.setDisabled(true); 
        if (selected.length == 1 ) {
            Ext.Ajax.request( {
                url: selected[0].getData().restUrl + '/details',
                method: 'GET',
                success: function(response) {
                    if (this.type == 'person')
                        jsonDetails = Ext.JSON.decode(response.responseText, true).personDetails[0];
                    else
                        jsonDetails = Ext.JSON.decode(response.responseText, true).organizationDetails[0];
                    this.identityDetails.setValues(jsonDetails);
                },
            scope: this 
            } );
        }
        this.takeOffButton.setDisabled(selected.length == 0); 
        this.mergeButton.setDisabled(selected.length < 2); 
    },
    merge: function() {
        var uris = new Array();
        var records = this.similarityList.getSelectionModel().getSelection();
        for (i = 0; i < records.length; i++)
            uris[i] = records[i].data.uri;

        var mergeWizard = Ext.create('Comete.IdentityMerger', {
            width: 560,
            height: 400,
            type: this.type,            
            modal: true,
            uris: uris,
            parent: this
        });
        mergeWizard.show();        
    },
    afterMerge: function() {
        this.updateList(this.similarityList.getSelectionModel().getSelection().length == this.store.count());
        //update related identity list
        this.parent.initList(this.type);
    },
    takeOff: function() {
        var message = (this.similarityList.getSelectionModel().getSelection().length == 1)?
                  'Element will be removed from the group.<br>Are you sure you want to proceed ?':
                  'Selected elements will be removed from the group.<br>There are maybe possible merge between them.<br>Do you want to proceed anyway ?';

        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            title: tr('Warning'), 
            msg: tr(message), 
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.WARNING,
            fn: this.takeOffEff, 
            scope: this 
        });
    },
    takeOffEff: function(button) {
        if (button != 'ok')
            return;

        var uris = new Array();
        var records = this.similarityList.getSelectionModel().getSelection();
        var totalCount = this.store.count();
        for (i = 0; i < records.length; i++)
            uris[i] = records[i].data.uri;
        Ext.Ajax.request( {
            url: 'rest/persons/takeOff',
            params: {
                uris: JSON.stringify(uris),
                similarGroup: this.similarGroups[this.currentGroup],
            },
            method: 'GET',
            success: function(response, opts) {
                this.updateList( (totalCount - records.length) <= 1 );
            },
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );         
    },
    updateList: function(isGroupObsolete) {
        if (isGroupObsolete) {
            this.similarGroups.splice(this.currentGroup, 1); //remove gid
            if (this.similarGroups.length == 0)
                this.initSimilarity(tr('No similarity found'));
            else {
                this.currentGroup--;
                this.showNextGroup();
            }
        }
        else
            this.store.load();    
    }
} );
