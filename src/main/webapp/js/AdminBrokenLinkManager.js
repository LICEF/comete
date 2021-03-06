Ext.define( 'Comete.BrokenLinkManager', {
    extend: 'Ext.panel.Panel',
    layout: {
        type: 'vbox',
        align: 'center'
    },  
    initComponent: function( config ) {
        this.updateDataForReport = function() {
            Ext.Ajax.request( {
                method: 'HEAD',
                url: 'rest/brokenLinkManager/report?lang=' + lang,
                success: function( response ) {
                    var reportLocation = response.getAllResponseHeaders()[ 'report-location' ];
                    var html = Ext.String.format( tr( 'Browse the <a href="{0}" target="_blank">last report</a>'), reportLocation );
                    this.browseReportPanel.update( html );
                    this.reportPanel.setVisible( true );
                    this.launcherPanel.setVisible( true );
                    this.progressPanel.setVisible( false );
                },
                failure: function( response ) {
                    if( response.status == 404 ) {
                        // It means that no report is available.
                        this.reportPanel.setVisible( false );
                        this.launcherPanel.setVisible( true );
                        this.progressPanel.setVisible( false );
                    }
                    else
                        Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                },
                scope: this
            } );
        }

        this.startVerification = function() {
            Ext.Msg.alert( tr( 'Information' ), tr( 'This process can take several minutes to complete.<br/><br/>Come again later to retrieve the report.' ), this.doStartVerification, this );
        }

        this.refresh = function() {
            this.updateData();
        }

        this.cancelVerification = function() {
            Ext.Msg.buttonText = { cancel: tr("Cancel") };
            Ext.Msg.show({
                title: tr('Warning'), 
                msg: tr( 'Are you sure that you want to cancel the current broken links validation process?' ), 
                buttons: Ext.Msg.OKCANCEL,
                icon: Ext.Msg.WARNING,
                fn: this.doCancelVerification, 
                scope: this 
            });
        }

        this.doStartVerification = function() {
            Ext.Ajax.request( {
                url: 'rest/brokenLinkManager/verification',
                params: {
                    setBrokenLinkFlag: this.setFlagCheckbox.getValue()
                },
                method: 'POST',
                success: function( response ) {
                    this.updateData();
                },
                failure: function( response ) {
                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                },
                scope: this
            } );
        }

        this.doCancelVerification = function() {
            Ext.Ajax.request( {
                url: 'rest/brokenLinkManager/validation',
                method: 'DELETE',
                success: function( response ) {
                    this.updateData();
                },
                failure: function( response ) {
                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
                },
                scope: this
            } );
        }

        this.progressLabel = Ext.create( 'Ext.form.Label', { margin: '10 0 0 10', width: '100%' } );

        this.progressPanel = Ext.create( 'Ext.panel.Panel', {
            width: '100%',
            layout: 'vbox',
            border: 0,
            hidden: true,
            items: [
                { xtype: 'label', text: tr( 'Validation in progress...' ), margin: '10 0 0 10', cls: 'sectionTitle', width: '100%' },
                this.progressLabel,
                { xtype: 'panel', layout: 'hbox', border: 0, items: [
                    { xtype: 'button', text: tr( 'Refresh' ), margin: '10 0 0 10', handler: this.refresh, scope: this },
                    { xtype: 'button', text: tr( 'Cancel' ), margin: '10 0 0 10', handler: this.cancelVerification, scope: this, hidden: true }
                ] }
            ]
        } );

        this.browseReportPanel = Ext.create( 'Ext.panel.Panel', { margin: '10 0 0 10', width: '100%', border: 0 } );

        this.reportPanel = Ext.create( 'Ext.panel.Panel', {
            width: '100%',
            layout: 'vbox',
            border: 0,
            hidden: true,
            items: [
                { xtype: 'label', text: tr( 'Last Report' ), margin: '10 0 0 10', cls: 'sectionTitle', width: '100%' },
                this.browseReportPanel,
                { height: 1, width: '100%', margin: '5 10 0 10', border: true, bodyStyle: 'border-color:#8374B0' }
            ]
        } );

        this.setFlagCheckbox = Ext.create( 'Ext.form.field.Checkbox', {
            fieldLabel: tr( 'Set "Broken Link" resource flag' ),
            name: 'setBrokenLinkFlag'
        } );

        this.launcherPanel = Ext.create( 'Ext.panel.Panel', {
            width: '100%',
            border: 0,
            layout: {
                type: 'vbox',
                align: 'left'
            },
            items: [
                { xtype: 'label', text: tr( 'Launching broken links validation task' ), margin: '10 0 0 10', cls: 'sectionTitle', width: '100%' },
                { xtype: 'panel', layout: 'form', width: 500, border: 0, margin: '10 0 0 10', items: [ this.setFlagCheckbox ] },
                { xtype: 'button', text: tr( 'Start Verification' ), margin: '10 0 0 10', handler: this.startVerification, scope: this }
            ]
        } );

        var cfg = {
            items: [ this.progressPanel, this.reportPanel, this.launcherPanel ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    updateData: function() {
        Ext.Ajax.request( {
            url: 'rest/brokenLinkManager/verification',
            method: 'GET',
            success: function( response ) {
                var progress = response.responseText.substring( 
                    response.responseText.indexOf( ': ' ) + 2,
                        response.responseText.indexOf( '%' ) + 1 );
                this.reportPanel.setVisible( false );
                this.launcherPanel.setVisible( false );
                this.progressLabel.setText( Ext.String.format( tr( '{0} completed.' ), progress ) );
                this.progressPanel.setVisible( true );
            },
            failure: function( response ) {
                if( response.status == 404 ) {
                    // It means that no verification is in progress so check if a report is available.
                    this.updateDataForReport();
                }
                else
                    Ext.Msg.alert( tr( 'Failure' ), response.responseText );
            },
            scope: this
        } );
    }
} );
