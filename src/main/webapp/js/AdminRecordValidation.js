Ext.define( 'Comete.AdminRecordValidation', {
    extend: 'Ext.tab.Panel',
    layout: 'border',  
    initComponent: function( config ) {
        this.isDirty = true;

        this.validatedApplProfiles = [];

        this.errorReportViewer = Ext.create( 'Ext.Panel', {
            region: 'center', 
            margin: 10, 
            html: '<iframe width="100%" height="100%" src=""></iframe>'
        } );

        this.recordHalfViewer = Ext.create( 'Ext.Panel', {
            region: 'center', 
            margin: 10, 
            html: '<iframe width="100%" height="100%" src=""></iframe>'
        } );

        this.recordHalfViewPanel = Ext.create( 'Ext.Panel', {
            layout: 'border',
            items: [ { xtype: 'label', text: tr( 'XML Record' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' }, this.recordHalfViewer ],
            region: 'center',
            border: 0
        } );

        this.recordFullViewer = Ext.create( 'Ext.Panel', {
            region: 'center', 
            margin: 10, 
            html: '<iframe width="100%" height="100%" src=""></iframe>'
        } );

        this.recordFullViewPanel = Ext.create( 'Ext.Panel', {
            layout: 'border',
            items: [ { xtype: 'label', text: tr( 'XML Record' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' }, this.recordFullViewer ],
            region: 'center',
            border: 0
        } );

        this.recordErrorReport = Ext.create( 'Ext.Panel', {
            layout: 'border',
            border: 0,
            items: [ 
                { xtype: 'label', text: tr( 'Validation Report' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' }, 
                this.errorReportViewer 
            ]
        } );

        this.recordNotAppl = Ext.create( 'Ext.Panel', {
            layout: 'border',
            border: 0,
            items: [
                { xtype: 'label', text: tr( 'Not Applicable' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' },
                { html: tr( 'This application profile is unrelated to this metadata format.' ), margin: '10 0 0 10', region: 'center', border: 0 }
            ]
        } );

        this.recordValid = Ext.create( 'Ext.Panel', {
            layout: 'border',
            border: 0,
            items: [
                { xtype: 'label', text: tr( 'Valid' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' },
                { html: tr( 'The metadata record is valid according to this application profile.' ), margin: '10 0 0 10', region: 'center', border: 0 }
            ]
        } );

        this.recordUnchecked = Ext.create( 'Ext.Panel', {
            layout: 'border',
            border: 0,
            items: [
                { xtype: 'label', text: tr( 'Unchecked' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' },
                { html: tr( 'The metadata record has not been tested by the validator against this application profile.' ), margin: '10 0 0 10', region: 'center', border: 0 }
            ]
        } );

        this.recordResultPanel = Ext.create( 'Ext.Panel', {
            width: 600,
            layout: 'card',
            margin: '-1 0 0 0',
            items: [ this.recordErrorReport, this.recordNotAppl, this.recordValid, this.recordUnchecked ],
            region: 'east', 
            split: true
        } );

        this.showOnlyColumn = '';
        this.showOnlyInvalid = false;

        this.recordValidationProxy = Ext.create( 'Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: 'records',
                totalProperty: 'totalCount'
            },
            url: 'rest/metadataRecords/applicationProfilesByColumns',
            extraParams: {
                showOnlyColumn: this.showOnlyColumn,
                showOnlyInvalid: this.showOnlyInvalid
            }
        } );

        this.recordStore = Ext.create( 'Ext.data.Store', {
            model: 'RecordValidationModel',
            proxy: this.recordValidationProxy
        } );

        this.pageBar = Ext.create( 'Ext.toolbar.Paging', {
            store: this.recordStore,
            displayInfo: true,
            firstText: tr('First Page'),
            prevText: tr('Previous Page'),
            nextText: tr('Next Page'),
            lastText: tr('Last Page'),
            refreshText: tr('Refresh'),
            afterPageText: tr('of {0}'),
            displayMsg: tr( 'Records {0} - {1} of {2}' ),
            emptyMsg: tr( "No records available" )
        } );

        this.getRecordIdFromUri = function( uri ) {
            var pos = uri.lastIndexOf( '/' );
            return( pos == -1 ? uri : uri.substring( pos + 1 ) );
        };

        var renderIdentifier = this.getRecordIdFromUri;

        var renderValidResult = function( isValid ) {
            var filename;
            if( 'true' == isValid )
                filename = 'validMark';
            else if( 'false' == isValid )
                filename = 'invalidMark';
            else if( '?'== isValid ) 
                filename = 'unknownMark';
            else 
                filename = 'notApplMark';
            return( Ext.String.format( '<img src="images/{0}.png">', filename ) );
        };

        this.recordListPanel = Ext.create( 'Ext.grid.Panel', {
            store: this.recordStore,
            cls: 'lo-grid',
            selType: 'cellmodel',
            columns: [
                { id: 'ColIdentifier', text: tr( 'Identifier' ), dataIndex: 'id', renderer: renderIdentifier },
                { id: 'ColLomStrict', text: tr( 'LOM Strict' ), width: 120, dataIndex: 'LomStrict', renderer: renderValidResult, dataApplProf: 'http://ltsc.ieee.org/xsd/LOM/strict' },
                { id: 'ColLomLoose', text: tr( 'LOM Loose' ), width: 120, dataIndex: 'LomLoose', renderer: renderValidResult, dataApplProf: 'http://ltsc.ieee.org/xsd/LOM/loose' },
                { id: 'ColLomFR', text: tr( 'LOM FR' ), width: 120, dataIndex: 'LomFR', renderer: renderValidResult, dataApplProf: 'http://lom-fr.fr/validation/LomFRv1.0/core' },
                { id: 'ColScoLomFR_1_0', text: tr( 'LOM Scorm<br/>LOM FR 1.0' ), width: 120, dataIndex: 'ScoLomFR_1_0', renderer: renderValidResult, dataApplProf: 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' },
                { id: 'ColScoLomFR_1_1', text: tr( 'LOM Scorm<br/>LOM FR 1.1' ), width: 120, dataIndex: 'ScoLomFR_1_1', renderer: renderValidResult, dataApplProf: 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' },
                { id: 'ColLomNormetic_1_2', text: tr( 'LOM<br/>Normetic 1.2' ), width: 120, dataIndex: 'LomNormetic_1_2', renderer: renderValidResult, dataApplProf: 'http://www.normetic.org/LomNormeticv1.2' },
                { id: 'ColOAIDC', text: tr( 'OAI DC' ), width: 120, dataIndex: 'OAIDC', renderer: renderValidResult, dataApplProf: 'http://www.openarchives.org/OAI/2.0/' },
                { id: 'ColRepo', text: tr( 'Repository' ), width: 120, dataIndex: 'repoName' }
            ],
            viewConfig: {
                loadingText: tr('Loading') + '...',
                stripeRows: false
            },
            hideHeaders: false,
            region: 'center',
            border: true,
            height: '100%', 
            split: true,
            bbar: this.pageBar
        } );

        this.recordListPanel.on( 'cellclick', this.cellSelected, this );

        this.recordHalfWithResultViewPanel = Ext.create( 'Ext.Panel', {
            layout: 'border', 
            width: '100%', 
            region: 'center',
            margin: '-1 0 0 0',
            items: [ this.recordHalfViewPanel, this.recordResultPanel ]
        } );
        
        this.recordRepoInfoBodyPanel = Ext.create( 'Ext.Panel', {
            margin: '10 0 0 10', region: 'center', border: 0
        } );

        this.recordRepoInfoPanel = Ext.create( 'Ext.Panel', {
            layout: 'border',
            border: 0,
            items: [
                { xtype: 'label', text: tr( 'Repository Details' ), margin: '10 0 0 10', cls: 'sectionTitle', region: 'north' },
                this.recordRepoInfoBodyPanel
            ]
        } );

        this.recordDetailsPanel = Ext.create( 'Ext.Panel', {
            layout: 'card', 
            width: '100%', 
            region: 'center',
            margin: '-5 0 0 0',
            items: [ this.recordFullViewPanel, this.recordHalfWithResultViewPanel, this.recordRepoInfoPanel ]
        } );

        this.saveValidations = function() {
            Ext.Ajax.request( {
                url: 'rest/settings/validatedApplicationProfiles',
                method: 'PUT',
                success: function() {
                    Ext.Msg.alert( tr( 'Warning' ), tr( 'To update the Validation Report, the metamodel must be resetted.' ) );
                    this.isDirty = true; // Force to reupdate.
                },
                failure: function( response ) {
                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                },
                params: {
                    'http://ltsc.ieee.org/xsd/LOM/strict': Ext.ComponentManager.get( 'CBLomStrict' ).getValue(),
                    'http://ltsc.ieee.org/xsd/LOM/loose': Ext.ComponentManager.get( 'CBLomLoose' ).getValue(),
                    'http://lom-fr.fr/validation/LomFRv1.0/core': Ext.ComponentManager.get( 'CBLomFR' ).getValue(),
                    'http://lom-fr.fr/validation/ScoLomFRv1.0/core': Ext.ComponentManager.get( 'CBScoLomFR_1_0' ).getValue(),
                    'http://lom-fr.fr/validation/ScoLomFRv1.1/core': Ext.ComponentManager.get( 'CBScoLomFR_1_1' ).getValue(),
                    'http://www.normetic.org/LomNormeticv1.2': Ext.ComponentManager.get( 'CBLomNormetic_1_2' ).getValue(),
                    'http://www.openarchives.org/OAI/2.0/': Ext.ComponentManager.get( 'CBOAIDC' ).getValue()
                },
                scope: this
            } );
        };

        this.applyVisualizationOptions = function() {
            this.showOnlyColumn = '';
            if( Ext.ComponentManager.get( 'RBShowOnlyOneApplProf' ).getValue() ) {
                var checkedItems = Ext.ComponentManager.get( 'RBGroupShowOnlyOneApplProf' ).getChecked();
                if( checkedItems && checkedItems.length > 0 )
                    this.showOnlyColumn = checkedItems[ 0 ].dataApplProf; 

                for( var i = 0; i < this.recordListPanel.columns.length; i++ ) {
                    var col = this.recordListPanel.columns[ i ];
                    col.setVisible( typeof( col.dataApplProf ) == 'undefined' || ( col.dataApplProf == this.showOnlyColumn ) );
                }
            }
            else {
                for( var i = 0; i < this.recordListPanel.columns.length; i++ ) {
                    var col = this.recordListPanel.columns[ i ];
                    col.setVisible( typeof( col.dataApplProf ) == 'undefined' || this.validatedApplProfiles[ col.dataApplProf ] );
                }
            }

            this.showOnlyInvalid = Ext.ComponentManager.get( 'CBShowOnlyInvalidRecords' ).getValue();

            this.recordValidationProxy.setExtraParam( 'showOnlyColumn', this.showOnlyColumn );
            this.recordValidationProxy.setExtraParam( 'showOnlyInvalid', this.showOnlyInvalid );
            this.recordStore.loadPage( 1 );
        }

        this.updateShowAllOrOneApplProf = function( radioButton, isChecked ) {
            if( isChecked ) {
                if( radioButton.id == 'RBShowAllApplProf' ) {
                    Ext.ComponentManager.get( 'RBLomStrict' ).setDisabled( true );
                    Ext.ComponentManager.get( 'RBLomLoose' ).setDisabled( true );
                    Ext.ComponentManager.get( 'RBLomFR' ).setDisabled( true );
                    Ext.ComponentManager.get( 'RBScoLomFR_1_0' ).setDisabled( true );
                    Ext.ComponentManager.get( 'RBScoLomFR_1_1' ).setDisabled( true );
                    Ext.ComponentManager.get( 'RBLomNormetic_1_2' ).setDisabled( true );
                    Ext.ComponentManager.get( 'RBOAIDC' ).setDisabled( true );
                    Ext.ComponentManager.get( 'CBShowOnlyInvalidRecords' ).setDisabled( true );
                }
                else if( radioButton.id == 'RBShowOnlyOneApplProf' ) {
                    Ext.ComponentManager.get( 'RBLomStrict' ).setDisabled( false );
                    Ext.ComponentManager.get( 'RBLomLoose' ).setDisabled( false );
                    Ext.ComponentManager.get( 'RBLomFR' ).setDisabled( false );
                    Ext.ComponentManager.get( 'RBScoLomFR_1_0' ).setDisabled( false );
                    Ext.ComponentManager.get( 'RBScoLomFR_1_1' ).setDisabled( false );
                    Ext.ComponentManager.get( 'RBLomNormetic_1_2' ).setDisabled( false );
                    Ext.ComponentManager.get( 'RBOAIDC' ).setDisabled( false );
                    Ext.ComponentManager.get( 'CBShowOnlyInvalidRecords' ).setDisabled( false );
                }
            }
        }

        this.visualizationOptionsPanel = Ext.create( 'Ext.Panel', {
            border: 0,
            region: 'east',
            layout: 'vbox',
            width: 320,
            split: true,
            autoScroll: true, // Doesn't work... Why? - FB
            items: [ 
                { region: 'north', xtype: 'label', text: tr( 'Options' ), margin: '10 0 0 10', cls: 'sectionTitle' }, 
                { region: 'center', xtype: 'panel', margin: 10, border: 0, layout: 'vbox', width: '100%', height: 300, items: [
                    { xtype: 'radio', id: 'RBShowAllApplProf', width: '100%', height: 20, boxLabel: tr( 'Show all application profiles' ), name: 'RBShowAllOrOneApplProf', handler: this.updateShowAllOrOneApplProf, scope: this },
                    { xtype: 'radio', id: 'RBShowOnlyOneApplProf', width: '100%', height: 20, boxLabel: tr( 'Show only this application profile:' ), name: 'RBShowAllOrOneApplProf', handler: this.updateShowAllOrOneApplProf, scope: this },
                    { xtype: 'radiogroup', id: 'RBGroupShowOnlyOneApplProf', width: '100%', height: '100%', columns: 1, items: [
                        { xtype: 'radio', id: 'RBLomStrict', width: '100%', height: 20, boxLabel: 'LOM Strict', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://ltsc.ieee.org/xsd/LOM/strict' },
                        { xtype: 'radio', id: 'RBLomLoose', width: '100%', height: 20, boxLabel: 'LOM Loose', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://ltsc.ieee.org/xsd/LOM/loose' },
                        { xtype: 'radio', id: 'RBLomFR', width: '100%', height: 20, boxLabel: 'LOM FR', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://lom-fr.fr/validation/LomFRv1.0/core' },
                        { xtype: 'radio', id: 'RBScoLomFR_1_0', width: '100%', height: 20, boxLabel: 'LOM Scorm LOM FR 1.0', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' },
                        { xtype: 'radio', id: 'RBScoLomFR_1_1', width: '100%', height: 20, boxLabel: 'LOM Scorm LOM FR 1.1', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' },
                        { xtype: 'radio', id: 'RBLomNormetic_1_2', width: '100%', height: 20, boxLabel: 'LOM Normetic 1.2', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://www.normetic.org/LomNormeticv1.2' },
                        { xtype: 'radio', id: 'RBOAIDC', width: '100%', height: 20, boxLabel: 'OAI DC', name: 'RBShowOnlyOneApplProf', margin: '0 0 0 20', hidden: true, dataApplProf: 'http://www.openarchives.org/OAI/2.0/' } 
                    ] },
                    { xtype: 'checkbox', id: 'CBShowOnlyInvalidRecords', width: '100%', height: 20, boxLabel: tr( 'Show only invalid records.' ), margin: '20 0 0 24' },
                    { xtype: 'button', text: tr( 'Apply' ), handler: this.applyVisualizationOptions, scope: this, margin: '20 0 0 0' }
                ] }
            ]
        } );

        this.checkFirstVisibleRadioButton = function() {
            var radioButtonParent = Ext.ComponentManager.get( 'RBGroupShowOnlyOneApplProf' );
            for( var i = 0; i < radioButtonParent.items.items.length; i++ ) {
                var child = radioButtonParent.items.items[ i ];
                if( child.getName() == 'RBShowOnlyOneApplProf' && child.isVisible() ) {
                    child.setValue( true );
                    break;
                }
            }
        }

        this.recordListAndOptionsPanel = Ext.create( 'Ext.Panel', {
            border: 0,
            layout: 'border',
            width: '100%', 
            height: 400,
            region: 'north',
            split: true,
            margin: '-1 -1 -1 -1',
            items: [ this.recordListPanel, this.visualizationOptionsPanel ]
        } );

        this.reportPage = Ext.create( 'Ext.Panel', {
            region: 'center',
            width: 400,
            layout: 'border',
            border: 0,
            items: [ this.recordListAndOptionsPanel, this.recordDetailsPanel ]
        } );

        this.noReportPage = Ext.create( 'Ext.Panel', {
            items: { xtype: 'label', text: tr( 'No application profiles have been selected for validation.' ), margin: '10 0 0 10', cls: 'sectionTitle' }
        } );

        this.visualizationPage = Ext.create( 'Ext.Panel', {
            title: tr( 'Visualization' ),
            region: 'center',
            title: tr( 'Visualization' ),
            layout: 'card',
            items: [ { /* Empty page */ }, this.noReportPage, this.reportPage ]
        } );

        this.configPage = Ext.create( 'Ext.Panel', {
            region: 'center',
            layout: 'border',
            margin: '-1 0 0 0',
            title: tr( 'Configuration' ),
            items: [ 
                { region: 'north', xtype: 'label', text: tr( 'List of enabled validations' ), margin: '10 0 0 10', cls: 'sectionTitle' }, 
                { region: 'center', xtype: 'panel', margin: 10, border: 0, layout: 'vbox', width: '100%', id: 'CBPanel', items: [
                    { xtype: 'checkbox', id: 'CBLomStrict', boxLabel: 'LOM Strict', inputValue: 'LomStrict' },
                    { xtype: 'checkbox', id: 'CBLomLoose', boxLabel: 'LOM Loose', inputValue: 'LomLoose' },
                    { xtype: 'checkbox', id: 'CBLomFR', boxLabel: 'LOM FR', inputValue: 'LomFR' },
                    { xtype: 'checkbox', id: 'CBScoLomFR_1_0', boxLabel: 'LOM Scorm LOM FR 1.0', inputValue: 'ScoLomFR_1_0' },
                    { xtype: 'checkbox', id: 'CBScoLomFR_1_1', boxLabel: 'LOM Scorm LOM FR 1.1', inputValue: 'ScoLomFR_1_1' },
                    { xtype: 'checkbox', id: 'CBLomNormetic_1_2', boxLabel: 'LOM Normetic 1.2', inputValue: 'LomNormetic_1_2' },
                    { xtype: 'checkbox', id: 'CBOAIDC', boxLabel: 'OAI DC', inputValue: 'OAIDC' },
                    { xtype: 'button', text: tr( 'Save Settings' ), handler: this.saveValidations, scope: this, margin: '10 0 0 0', disabled: !authorized }
                ] }
            ]
        } );

        var cfg = {
            activeTab: 0,
            plain: true,
            margin: '0 -1 -1 -1',
            region: 'center',      
            width: 400,
            items: [ this.visualizationPage, this.configPage ]
        };

        Ext.apply(this, cfg);
        this.callParent(arguments); 

        this.on( 'tabchange', this.updateData, this );
    },
    cellSelected: function() {
        var pos = this.recordListPanel.getSelectionModel().getCurrentPosition();
        if( pos != null ) {
            if( pos.column == this.recordListPanel.columns.length - 1 ) {
                this.recordDetailsPanel.getLayout().setActiveItem( 2 ); 
                var repoName = this.recordStore.data.items[ pos.row ].data[ 'repoName' ];
                var repoAdminEmail = this.recordStore.data.items[ pos.row ].data[ 'repoAdminEmail' ];
                if( repoAdminEmail.indexOf( 'mailto:' ) == 0 )
                    repoAdminEmail = repoAdminEmail.substring( 'mailto:'.length );
                var html = Ext.String.format( tr( '<p>Name: {0}</p><p>Contact: <a href="{1}">{1}</a></p>' ), repoName, repoAdminEmail );
                this.recordRepoInfoBodyPanel.body.update( html );
            }
            else {
                var id = this.getRecordIdFromUri( this.recordStore.data.items[ pos.row ].id ); 
                var link = './rest/metadataRecords/' + id + '/xml?syntaxHighlighted=true';
                var html = '<iframe width="100%" height="100%" src="' + link + '"></iframe>';

                if( pos.column == 0 ) {
                    this.recordDetailsPanel.getLayout().setActiveItem( 0 ); 
                    this.recordFullViewer.body.update( html );
                }
                else if( pos.column > 0 ) {
                    this.recordDetailsPanel.getLayout().setActiveItem( 1 ); 
                    this.recordHalfViewer.body.update( html );

                    var applProf = this.recordStore.model.getFields()[ pos.column ].name;
                    var isValid = this.recordStore.data.items[ pos.row ].data[ applProf ];
                    if( 'true' == isValid )
                        this.recordResultPanel.getLayout().setActiveItem( 2 );
                    else if( 'notApplicable' == isValid )
                        this.recordResultPanel.getLayout().setActiveItem( 1 );
                    else if( '?' == isValid )
                        this.recordResultPanel.getLayout().setActiveItem( 3 );
                    else {
                        var reportLink = './rest/metadataRecords/' + id + '/validationReport/' + applProf + '/xml?syntaxHighlighted=true';
                        var reportHtml = '<iframe width="100%" height="100%" src="' + reportLink + '"></iframe>';
                        this.errorReportViewer.body.update( reportHtml );
                        this.recordResultPanel.getLayout().setActiveItem( 0 );
                    }
                }
            }

        }
    },
    updateData: function() {
        if( this.isDirty ) {
            Ext.Ajax.request( {
                url: 'rest/settings/validatedApplicationProfiles',
                method: 'GET',
                success: function( response ) {
                    var data = Ext.decode( response.responseText );

                    this.validatedApplProfiles = [];
                    
                    var showOnlyOneApplProfPanelHeight = 0;

                    Ext.ComponentManager.get( 'RBShowAllApplProf' ).setValue( true );

                    Ext.ComponentManager.get( 'RBGroupShowOnlyOneApplProf' ).setHeight( 30 );

                    Ext.ComponentManager.get( 'CBLomStrict' ).setValue( data[ 'http://ltsc.ieee.org/xsd/LOM/strict' ] );   
                    Ext.ComponentManager.get( 'ColLomStrict' ).setVisible( data[ 'http://ltsc.ieee.org/xsd/LOM/strict' ] );
                    Ext.ComponentManager.get( 'RBLomStrict' ).setVisible( data[ 'http://ltsc.ieee.org/xsd/LOM/strict' ] );
                    if( data[ 'http://ltsc.ieee.org/xsd/LOM/strict' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://ltsc.ieee.org/xsd/LOM/strict' ] = data[ 'http://ltsc.ieee.org/xsd/LOM/strict' ];

                    Ext.ComponentManager.get( 'CBLomLoose' ).setValue( data[ 'http://ltsc.ieee.org/xsd/LOM/loose' ] );
                    Ext.ComponentManager.get( 'ColLomLoose' ).setVisible( data[ 'http://ltsc.ieee.org/xsd/LOM/loose' ] );
                    Ext.ComponentManager.get( 'RBLomLoose' ).setVisible( data[ 'http://ltsc.ieee.org/xsd/LOM/loose' ] );
                    if( data[ 'http://ltsc.ieee.org/xsd/LOM/loose' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://ltsc.ieee.org/xsd/LOM/loose' ] = data[ 'http://ltsc.ieee.org/xsd/LOM/loose' ];

                    Ext.ComponentManager.get( 'CBLomFR' ).setValue( data[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ] );
                    Ext.ComponentManager.get( 'ColLomFR' ).setVisible( data[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ] );
                    Ext.ComponentManager.get( 'RBLomFR' ).setVisible( data[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ] );
                    if( data[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ] = data[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ];

                    Ext.ComponentManager.get( 'CBScoLomFR_1_0' ).setValue( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ] );
                    Ext.ComponentManager.get( 'ColScoLomFR_1_0' ).setVisible( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ] );
                    Ext.ComponentManager.get( 'RBScoLomFR_1_0' ).setVisible( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ] );
                    if( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ] = data[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ];

                    Ext.ComponentManager.get( 'CBScoLomFR_1_1' ).setValue( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ] );
                    Ext.ComponentManager.get( 'ColScoLomFR_1_1' ).setVisible( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ] );
                    Ext.ComponentManager.get( 'RBScoLomFR_1_1' ).setVisible( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ] );
                    if( data[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ] = data[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ];

                    Ext.ComponentManager.get( 'CBLomNormetic_1_2' ).setValue( data[ 'http://www.normetic.org/LomNormeticv1.2' ] );
                    Ext.ComponentManager.get( 'ColLomNormetic_1_2' ).setVisible( data[ 'http://www.normetic.org/LomNormeticv1.2' ] );
                    Ext.ComponentManager.get( 'RBLomNormetic_1_2' ).setVisible( data[ 'http://www.normetic.org/LomNormeticv1.2' ] );
                    if( data[ 'http://www.normetic.org/LomNormeticv1.2' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://www.normetic.org/LomNormeticv1.2' ] = data[ 'http://www.normetic.org/LomNormeticv1.2' ];

                    Ext.ComponentManager.get( 'CBOAIDC' ).setValue( data[ 'http://www.openarchives.org/OAI/2.0/' ] );
                    Ext.ComponentManager.get( 'ColOAIDC' ).setVisible( data[ 'http://www.openarchives.org/OAI/2.0/' ] );
                    Ext.ComponentManager.get( 'RBOAIDC' ).setVisible( data[ 'http://www.openarchives.org/OAI/2.0/' ] );
                    if( data[ 'http://www.openarchives.org/OAI/2.0/' ] )
                        showOnlyOneApplProfPanelHeight += 20;
                    this.validatedApplProfiles[ 'http://www.openarchives.org/OAI/2.0/' ] = data[ 'http://www.openarchives.org/OAI/2.0/' ];

                    Ext.ComponentManager.get( 'RBGroupShowOnlyOneApplProf' ).setHeight( showOnlyOneApplProfPanelHeight );

                    var isAtLeastOneApplProf = 
                        ( data[ 'http://ltsc.ieee.org/xsd/LOM/strict' ] ||
                          data[ 'http://ltsc.ieee.org/xsd/LOM/loose' ] ||
                          data[ 'http://lom-fr.fr/validation/LomFRv1.0/core' ] ||
                          data[ 'http://lom-fr.fr/validation/ScoLomFRv1.0/core' ] ||
                          data[ 'http://lom-fr.fr/validation/ScoLomFRv1.1/core' ] ||
                          data[ 'http://www.normetic.org/LomNormeticv1.2' ] ||
                          data[ 'http://www.openarchives.org/OAI/2.0/' ] );
                            
                    if( isAtLeastOneApplProf ) {
                        this.checkFirstVisibleRadioButton();
                        this.recordValidationProxy.setExtraParam( 'showOnlyColumn', this.showOnlyColumn );
                        this.recordValidationProxy.setExtraParam( 'showOnlyInvalid', this.showOnlyInvalid );
                        this.recordStore.loadPage( 1 );
                        this.visualizationPage.getLayout().setActiveItem( 2 );
                    }
                    else
                        this.visualizationPage.getLayout().setActiveItem( 1 );

                    this.isDirty = false;
                },
                failure: function( response ) {
                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                },
                scope: this
            } );
        }
    }
} );

