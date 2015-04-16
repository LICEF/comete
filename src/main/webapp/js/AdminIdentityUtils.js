Ext.define( 'Comete.IdentityDetails', {
    extend: 'Ext.panel.Panel', 
    layout: 'border',           
    initComponent: function( config ) {

        this.name = Ext.create('Comete.IdentityDetail', { 
            _label: 'Display Name',
            mode: this.mode
        });

        this.firstname = Ext.create('Comete.IdentityDetail', {
            _label: 'Firstname',
            mode: this.mode
        });

        this.lastname = Ext.create('Comete.IdentityDetail', {
            _label: 'Lastname',
            mode: this.mode
        });

        this.email = Ext.create('Comete.IdentityDetail', {
            _label: 'Email',
            mode: this.mode
        });

        this.tel = Ext.create('Comete.IdentityDetail', {
            _label: 'Telephone',
            mode: this.mode
        });

        this.fax = Ext.create('Comete.IdentityDetail', {
            _label: 'Fax',
            mode: this.mode
        });

        this.url = Ext.create('Comete.IdentityDetail', {
            _label: 'URL',
            mode: this.mode
        });

        this.address = Ext.create('Comete.IdentityDetail', {
            _label: 'Address',
            mode: this.mode
        });

        this.dataPanel = Ext.create('Ext.panel.Panel', {
            layout: 'vbox',
            region: 'west',
            width: 350,
            border: false,
            items: (this.type == 'person')?
                       [ this.name, this.firstname, this.lastname, this.email, this.tel, this.fax, this.url, this.address ]:
                       [ this.name, this.email, this.tel, this.fax, this.url, this.address ]
        });

        this.photoValues = Ext.create('Ext.form.field.ComboBox', {
            queryMode: 'local',
            displayField: 'value',
            valueField: 'value',
            emptyText: (this.type == 'person')?'Photo selection':'Logo selection',
            margin: '0 0 0 20',
            width: 129,
            store: Ext.create('Ext.data.Store', {
                fields: ['value'],
            }),            
            tpl: '<div><tpl for="."><div class="x-boundlist-item"><img width=90 src="{value}"/></div></tpl></div>'
        });

        this.photoValues.on( 'beforeselect', function(combo, record){ 
                        combo.collapse();
                        this.photo.setSrc(record.data.value);
                        return false;}, this ); 

        this.clearPhotoButton = Ext.create('Ext.button.Button', {
            margin: '0 0 0 2',
            icon: 'images/delete.png',
            handler: function(){ this.photo.setSrc(null); },
            scope: this
        } );
       
        this.photo = Ext.create('Ext.Img', {
            width: 100,
            margin: '0 0 0 20',
            border: true,
            style: {
                borderColor: '#99BCE8',
                borderStyle: 'solid'
            }
        });

        this.urlPhotoField = Ext.create('Ext.form.field.Text', {
            margin: '0 0 0 20',
            width: 129,
            emptyText: (this.type == 'person')?'Photo URL':'Logo URL'
        } );

        this.setUrlPhotoButton = Ext.create('Ext.button.Button', {
            margin: '0 0 0 2',
            icon: 'images/whiteArrow.png',
            handler: this.setUrlPhoto,
            scope: this
        } );

        this.uploadPhotoField = Ext.create('Ext.form.field.File', {
            name: 'photo',
            margin: '0 0 0 20',
            buttonText: '...',
            width: 129,
            emptyText: (this.type == 'person')?'Photo upload':'Logo upload'
        } );

        this.uploadButton = Ext.create('Ext.button.Button', {
            margin: '0 0 0 2',
            icon: 'images/whiteArrow.png',
            handler: this.uploadPhoto,
            scope: this
        } );       
 
        this.uploadPanel = Ext.create('Ext.form.Panel', {
            layout: 'hbox',
            border: false,
            items: [ this.uploadPhotoField, this.uploadButton ]               
        });

        cfg = {
            items: [ this.dataPanel,
                     { layout: 'vbox', region: 'center', border: false,
                       items: (this.mode == 'read')?
                                  this.photo:
                                  [ { layout: 'hbox', border: false, items: [ this.urlPhotoField, this.setUrlPhotoButton ] }, 
                                    {xtype: 'tbspacer', height: 5},
                                    this.uploadPanel, 
                                    {xtype: 'tbspacer', height: 5},
                                    { layout: 'hbox', border: false, items: [ this.photoValues, this.clearPhotoButton ] }, 
                                    {xtype: 'tbspacer', height: 5}, 
                                    this.photo ] } ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    setValues: function(details) {
        if (this.type == 'person') {
            this.dataPanel.getComponent(0).set(details.name);
            this.dataPanel.getComponent(1).set(details.firstname);
            this.dataPanel.getComponent(2).set(details.lastname);
            this.dataPanel.getComponent(3).set(details.email);
            this.dataPanel.getComponent(4).set(details.tel);
            this.dataPanel.getComponent(5).set(details.fax);
            this.dataPanel.getComponent(6).set(details.url);
            this.dataPanel.getComponent(7).set(details.address);
            if (details.photo != null) {
                var isMainPhoto = details.photo[0].value != "";
                if (!isMainPhoto) 
                    details.photo.splice(0, 1);
                this.photoValues.store.loadData(details.photo);
                if (this.mode == 'edition') {
                    if (isMainPhoto)
                        this.photo.setSrc(details.photo[0].value);
                }
                else
                    this.photo.setSrc(details.photo);
            }
        }
        else {
            this.dataPanel.getComponent(0).set(details.name);
            this.dataPanel.getComponent(1).set(details.email);
            this.dataPanel.getComponent(2).set(details.tel);
            this.dataPanel.getComponent(3).set(details.fax);
            this.dataPanel.getComponent(4).set(details.url);
            this.dataPanel.getComponent(5).set(details.address);
            if (details.logo != null) {
                var isMainLogo = details.logo[0].value != "";
                if (!isMainLogo) 
                    details.logo.splice(0, 1);
                this.photoValues.store.loadData(details.logo);
                if (this.mode == 'edition') {
                    if (isMainLogo)
                        this.photo.setSrc(details.logo[0].value);
                }
                else
                    this.photo.setSrc(details.logo);
            }
        } 
    },
    clear: function() {
        for (i = 0; i < this.dataPanel.items.length; i++)
            this.dataPanel.getComponent(i).clear();
        this.photo.setSrc(null);
    },
    getMainValues: function() {
        mainValues = {};
        if (this.name.getMainValue() != null) { mainValues['name'] = this.name.getMainValue(); }
        if (this.type == 'person') {
            if (this.firstname.getMainValue() != null) { mainValues['firstname'] = this.firstname.getMainValue(); }
            if (this.lastname.getMainValue() != null) { mainValues['lastname'] = this.lastname.getMainValue(); }
        }
        if (this.email.getMainValue() != null) { mainValues['email'] = this.email.getMainValue(); }
        if (this.tel.getMainValue() != null) { mainValues['tel'] = this.tel.getMainValue(); }
        if (this.fax.getMainValue() != null) { mainValues['fax'] = this.fax.getMainValue(); }
        if (this.url.getMainValue() != null) { mainValues['url'] = this.url.getMainValue(); }
        if (this.address.getMainValue() != null) { mainValues['address'] = this.address.getMainValue(); }
        if (this.photo.src != "" && this.photo.src != null) { 
            if (this.type == 'person')
                mainValues['photo'] = this.photo.src;
            else
                mainValues['logo'] = this.photo.src;
        }
        return mainValues;
    },
    setUrlPhoto: function() {
        var url = this.urlPhotoField.getValue();
        if (url != null && url != "" && url.startsWith("http")) 
            this.photo.setSrc(url);
    },
    uploadPhoto: function() {
        var waitDialog = Ext.create('Ext.window.MessageBox', {}); 
        waitDialog.wait( tr('Please wait') + '...' );
        var uploadForm = this.uploadPanel.getForm();  
        var photo = this.photo;  
        if (uploadForm.isValid()) {
            uploadForm.submit({ 
                url: 'rest/identities/photo',
                success: function(form, action) {
                   waitDialog.close();
                   photo.setSrc(action.result.data);
                },
                failure: function(form, action) {                   
                   Ext.Msg.alert('Failure', action.result.error);
                   waitDialog.close();
                }
            });
        }
    }
});

Ext.define( 'Comete.IdentityDetail', {
    extend: 'Ext.panel.Panel',  
    layout: 'hbox',         
    initComponent: function( config ) {

        this.label = Ext.create('Ext.form.Label', {
            text: tr(this._label)
        });

        this.dataField = (this.mode == 'read')?
            Ext.create('Ext.form.Label', {
                margin: '2',
                width: 240,
                height: (this.name == 'Address')?36:18,                 
                cls: 'identity'
            } ):
            Ext.create('Ext.form.field.ComboBox', {
                queryMode: 'local',
                displayField: 'value',
                valueField: 'value',
                width: 240,
                store: new Ext.data.ArrayStore({
                     fields: [ 'value' ]
                }),
                tpl: '<div><tpl for="."><div class="x-boundlist-item">{value}</div></tpl></div>'
            });

        cfg = {
            border: false,
            width: 350,
            margin: '0 0 5 0',
            height: (this.mode == 'read' && this.name == 'Address')?80:24,
            items: [ this.label, {xtype:'tbfill'}, { layout: 'fit', border: this.mode == 'read', items: this.dataField } ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments); 
    },
    set: function(data) {
        if (data != null && data != undefined) {
            if (this.mode == 'read')
                this.dataField.setText(data);
            else {
                this.dataField.store.loadData(data);
                this.dataField.setValue(data[0].value);
            }
        }
    },
    clear: function() {
        this.dataField.setText("");
    },
    getMainValue: function() { 
        return this.dataField.getValue();
    }
});
