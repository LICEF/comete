﻿Ext.define( 'Comete.AdvancedSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {

        this.introLabel = Ext.create('Ext.form.Label', {
            text: tr('Search of resources') + '...',            
            cls: 'intro'            
        } );

        this.addCondition = Ext.create('Ext.button.Button', {
            icon: 'images/plus.png',
            handler: function(){ this.addQueryCond(this.createQueryCond( false )); },             
            scope: this
        });

        this.advSearchQueryButton = Ext.create('Ext.button.Button', {
            text: tr('Search'),
            handler: this.submitAdvancedSearchQuery,
            scope: this
        } );

        this.saveAsCollectionButton = Ext.create('Ext.button.Button', {
            text: tr('Save as collection') + '...',
            hidden: !authorized,
            handler: this.saveAsCollection,        
            scope: this
        } );

        this.condPanel = Ext.create('Ext.panel.Panel', {
            layout: 'vbox',
            border: false, 
            region: 'west'
        });

        this.controlPanel = Ext.create('Ext.panel.Panel', {
            layout: 'vbox',
            region: 'center',
            border: false,
            items: this.advSearchQueryButton
        });

        var cfg = {
            layout: 'border',
            region: 'center', 
            border: false,
            margin: '0 0 0 10',
            items: [ { border: false, region: 'north', items: this.introLabel, height: 26 }, 
                       this.condPanel, this.controlPanel, 
                     { border: false, region: 'south', height: 40, layout: 'hbox', 
                       items: [this.addCondition, { xtype: 'tbspacer', width: 20 }, 
                               this.advSearchQueryButton, { xtype: 'tbspacer', width: 24 },
                               this.saveAsCollectionButton] }]    
            
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);         

        this.addQueryCond( this.createQueryCond( true ));        
    },
    clear: function() {
        this.condPanel.removeAll();
    },
    createQueryCond: function(isFirst) {
        var newCond = Ext.create('Comete.QueryCondition', {
            border: false,
            topPane: this,
            lang: this.lang,
            isFirst: isFirst,
            advSearchQueryButton: this.advSearchQueryButton
        } );
        if (!newCond.isFirst) {
            this.firstCond.removeButton.setVisible(true);
            this.firstCond.removeSpace.setVisible(true);
            this.firstCond.firstSpacer.setVisible(true);
        }
        else
            this.firstCond = newCond;
        
        return newCond;        
    },
    addQueryCond: function(cond) {    
        this.condPanel.add(cond); 
        var elems = this.condPanel.items.length;
        setQueryPanelHeight(Math.max(24*elems + 5*(elems - 1) + 135, ADVANCED_HEIGHT), true);
    },
    removeQueryCond: function(cond) {
        this.condPanel.remove(cond);
        var elems = this.condPanel.items.length;

        if (this.firstCond != this.condPanel.getComponent(0)) {
            this.firstCond = this.condPanel.getComponent(0)
            this.firstCond.isFirst = true;
            this.firstCond.andOr.setVisible(false);
            this.firstCond.andOrSpace.setVisible(false);    
            this.firstCond.firstSpacer.setVisible(elems == 1);
        }
        this.firstCond.removeButton.setVisible(elems > 1);
        this.firstCond.removeSpace.setVisible(elems > 1);
        this.firstCond.firstSpacer.setVisible(elems > 1);
        
        setQueryPanelHeight(Math.max(24*elems + 5*(elems - 1) + 135, ADVANCED_HEIGHT), true);
    },
    setNextAnd: function(cond) {
        var i = 0;
        for (; i < this.condPanel.items.length; i++) {
            if (this.condPanel.getComponent(i) == cond)
                break;
        };
        if (this.condPanel.getComponent(i + 1) != undefined )
            this.condPanel.getComponent(i + 1).setAndOr("AND");

    },
    isPreviousNegationChoice: function(cond) {
        var i = 0;
        for (; i < this.condPanel.items.length; i++) {
            if (this.condPanel.getComponent(i) == cond)
                break;
        };
        return this.condPanel.getComponent(i - 1).isNegationChoice();
    }, 
    submitAdvancedSearchQuery: function() {
        var query = this.buildAdvancedSearchQuery();
        if (query.length > 0)
            searchManager.setRequest( query ); 
    },
    buildAdvancedSearchQuery: function() {
        var query = new Array();
        var j = 0;
        for (i = 0; i < this.condPanel.items.length; i++) {
            cond = this.condPanel.getComponent(i);                
            condition = cond.getCondition();
            if (condition == null)
                continue;
            else {
                if (j != 0) {
                    query[j] = condition[0];
                    j++;
                }
                query[j] = condition[1]; 
                j++;
            }
        }

        return query;
    },
    setQuery: function(query) {
        var queryConds = new Array();
        var j = 0;
        for (i = 0; i < query.length; i++) {
            var cond = this.createQueryCond(i == 0);
            op = "AND";
            if (i > 0) {
                op = query[i].op;
                i++;
            }            
            cond.setQueryCondition(op, query[i]);
            queryConds[j] = cond;
            j++;
        }
        this.clear();        
        this.addQueryCond(queryConds);
    },
    saveAsCollection: function() {
        var query = this.buildAdvancedSearchQuery();
        if (query.length == 0) {
            Ext.Msg.alert(tr('Warning'), tr('Cannot save empty query.'));
            return;
        }

        var message = 'Enter the collection label';
        var promptBox = Ext.Msg;
        promptBox.buttonText = { cancel: tr("Cancel") };
        promptBox.show({
            msg: tr(message), 
            buttons: Ext.Msg.OKCANCEL,
            icon: Ext.Msg.QUESTION,
            prompt: true,
            fn: this.saveAsCollectionEff, 
            scope: this 
        });
    },
    saveAsCollectionEff: function(button, text) {      
        if (button != 'ok')
            return;
        if (text == "") {
            this.saveAsCollection();
            return;
        }
        var query = this.buildAdvancedSearchQuery();
        Ext.Ajax.request( {
            url: '/rest/queryEngine/collections',
            params: {
                label: text,
                q: JSON.stringify(query)
            },
            method: 'POST',
            success: function(response, opts) {
                searchManager.initCollections();
                Ext.Msg.alert('Information', tr('Collection saved.'));
            },
            failure: function(response, opts) {
                Ext.Msg.alert('Failure', response.responseText);  
            },
            scope: this 
        } );        
    }
} );

Ext.define( 'Comete.QueryCondition', {
    extend: 'Ext.panel.Panel',
    layout: 'hbox',  
    initComponent: function( config ) {

        this.data = null;
        this.currentType = 'title';

        this.andOrStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data : [
                {'id':'AND', 'label': tr('AND')},
                {'id':'OR', 'label': tr('OR')}
            ]
        });

        this.langStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data : [
                {'id':'fr', 'label': tr('French')},
                {'id':'en', 'label': tr('English')}
            ]
        });

        this.choiceStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data : [
                {'id':'title', 'label': tr('whose title contains')},
                {'id':'description', 'label': tr('whose description contains') },
                {'id':'keyword', 'label': tr('having keyword') },
                {'id':'addedDate', 'label': tr('where the addition date') },
                {'id':'contrib', 'label': tr('from contributor') },
                {'id':'!contrib', 'label': tr('not from contributor') },
                {'id':'org', 'label': tr('from organization') },
                {'id':'!org', 'label': tr('not from organization') },
                {'id':'vocConcept', 'label': tr('related to the category') },
                {'id':'!vocConcept', 'label': tr('not related to the category') }
            ]
        });

        this.andOr = Ext.create('Ext.form.field.ComboBox', {
                valueField: 'id',
                displayField: 'label',
                store: this.andOrStore,
                width: 65,
                editable: false,
                value: 'AND',
                hidden: this.isFirst
            });
 
        this.andOr.on('select', this.andOrSelected, this);

        this.andOrSpace = Ext.create('Ext.toolbar.Spacer', {
                width: 5,
                hidden: this.isFirst
            });

        this.removeButton = Ext.create('Ext.button.Button', {
                icon: 'images/minus.png',
                handler: this.remove,             
                scope: this,
                hidden: this.isFirst
            } );

        this.removeSpace = Ext.create('Ext.toolbar.Spacer', {
                width: 20,
                hidden: this.isFirst
            });

        this.firstSpacer = Ext.create('Ext.toolbar.Spacer', {
                width: 70,
                hidden: true
            });

        this.typeCond = Ext.create('Ext.form.field.ComboBox', {
                valueField: 'id',
                displayField: 'label',
                store: this.choiceStore,
                width: 220,
                editable: false,
                value: 'title'
            });

        this.typeCond.on('select', this.typeCondSelected, this);

        this.typeCondPanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: this.createTextCond()
        });

 
        cfg = {          
            margin: '0 0 5 0',        
            items: [ this.removeButton, this.removeSpace, 
                     this.firstSpacer, this.andOr, this.andOrSpace, 
                     this.typeCond, { xtype: 'tbspacer', width: 5 },
                     this.typeCondPanel                    
                   ]
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);         
    },
    remove: function() {
        this.topPane.removeQueryCond(this);
    },    
    typeCondSelected: function(combo) {

        //for text cond or negation/no negations changes, don't recreate element.
        var type = combo.getValue();
        var switchTextCond = ( (type == "title" && this.currentType == "description") || 
                           (type == "description" && this.currentType == "title") ) ;
        var switchNegation = ( type.substring(1) == this.currentType || 
                           this.currentType.substring(1) == type )
        if (!switchNegation && !switchTextCond) {
            this.element = null;
            this.data = null;
            this.makeCond(type);
        }

        if (type.startsWith("!")) {
            this.setAndOr("AND");
            this.topPane.setNextAnd(this); //change possible next cond to AND
        }

        this.currentType = type;
    },
    makeCond: function(type) {
        if (type == 'title' || type == 'description')
            this.element = this.createTextCond();
        else if (type == 'keyword' )
            this.element = this.createKeywordCond();
        else if (type == 'contrib' || type == '!contrib')
            this.element = this.createContribCond();
        else if (type == 'org' || type == '!org')
            this.element = this.createOrgCond();
        else if (type == 'vocConcept' || type == '!vocConcept')
            this.element = this.createConceptCond();
        else if (type == 'addedDate')
            this.element = this.createAddedDateCond();
        this.setCondPanel(this.element);
    },
    andOrSelected: function(combo) {
        if ( this.isNegationChoice() ||
             this.topPane.isPreviousNegationChoice(this) ) 
            this.setAndOr("AND");      
    },
    setAndOr: function(val) {
        this.andOr.setValue(val);
    },
    isNegationChoice: function() {
        return this.typeCond.getValue().startsWith("!");
    },
    setCondPanel: function(element) {
        this.typeCondPanel.removeAll();
        this.typeCondPanel.add(element);
    },
    createTextCond: function() { 
        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ { xtype: 'textfield', width: 250 },
                     { xtype: 'tbspacer', width: 5 },
                     { xtype: 'combo', valueField: 'id', displayField: 'label', store: this.langStore,
                       editable: false, width: 100, value: this.topPane.lang } ] 
        });
        return panel;
    },    
    createKeywordCond: function() { 
        var keywordProxy = Ext.create( 'Ext.data.proxy.Ajax', {
            reader: {
                type: 'json',
                root: 'keywords'
            }
        } );

        var keywordStore = Ext.create( 'Ext.data.JsonStore', {
            model: 'KeywordModel',
            proxy: keywordProxy
        } );

        keywordStore.on( 'beforeload', function( store ) {
            var selectedLang = this.typeCondPanel.getComponent(0).getComponent(2).getValue();
            store.proxy.url = '/rest/queryEngine/keywords?lang=' + selectedLang;
        }, this );

        var keywordCombo = Ext.create( 'Ext.form.field.ComboBox', {
            displayField: 'keyword',
            valueField: 'keyword',
            store: keywordStore,
            width: 250,
            hideTrigger: true,
            queryParam: 'value',
            listConfig: {
                loadingText: tr( 'Loading' ) + '...',
                emptyText: tr( 'No matching keyword found' ),
                getInnerTpl: function() {
                    return '{keyword}';
                }
            }
        } );

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ keywordCombo,
                     { xtype: 'tbspacer', width: 5 },
                     { xtype: 'combo', valueField: 'id', displayField: 'label', store: this.langStore,
                       editable: false, width: 80, value: this.topPane.lang } ] 
        });
        return panel;
    },    
    createAddedDateCond: function() { 
        var relOpStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data: [
                {'id':'gt', 'label': tr( 'is after the' ) },
                //{'id':'gte', 'label': '>='},
                {'id':'lt', 'label': tr( 'is before the' ) },
                //{'id':'lte', 'label': '<='},
                {'id':'eq', 'label': tr( 'is the' ) }
            ]
        });

        var relOpComboBox = Ext.create('Ext.form.field.ComboBox', {
            valueField: 'id',
            displayField: 'label',
            store: relOpStore,
            width: 100,
            editable: false,
            value: 'gt'
        } );

        var addedDateField = Ext.create('Ext.form.field.Date', {
            value: new Date(),
            format: "Y/m/d",
            editable: false 
        } );

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ 
                relOpComboBox,
                { xtype: 'tbspacer', width: 5 },
                addedDateField
            ] 
        });
        return panel;
    },    
    createContribCond: function() {
        var labelContrib = Ext.create('Ext.form.Label', {
            text: tr('Select contributor') + '...',
            margin: '3 0 0 5'
        });  

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ { xtype: 'button', icon: 'images/person.gif',                         
                       handler: this.pickPerson, 
                       scope: this },
                     { xtype: 'tbspacer', width: 5 },
                     { layout: 'hbox',
                       border: false,
                       height: 22, 
                       items: labelContrib
                     }                     
                   ] 
        });
 
        return panel;
    },
    createOrgCond: function() {
        var labelOrg = Ext.create('Ext.form.Label', {
            text: tr('Select organization') + '...',
            margin: '3 0 0 5'
        });  

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ { xtype: 'button', icon: 'images/building.png',                         
                       handler: this.pickOrg, 
                       scope: this }, 
                     { xtype: 'tbspacer', width: 5 },
                     { layout: 'hbox',
                       border: false,
                       height: 22, 
                       items: labelOrg }
                   ] 
        });
 
        return panel;
    },
    createConceptCond: function() {

        var labelVoc = Ext.create('Ext.form.Label', {
            style: 'font-weight: bold; color: #04408C',
            margin: '2 0 0 5'
        });  

        var arrow = Ext.create('Ext.Img', {
            src: 'images/blueArrow.gif', 
            margin: '4 6 0 6',
            hidden: true
        });  

        var labelConcept = Ext.create('Ext.form.Label', {
            text: tr('Select category') + '...',
            margin: '2 8 0 0'
        });  

        var cbSubconcepts = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('and subcategories'),
            style: 'color: #04408C',
            margin: '0 0 0 0',
            checked: false,
            hidden: true            
        } );

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,            
            items: [ { xtype: 'button', icon: 'images/tree.gif',                         
                       handler: this.pickVocConcept, 
                       scope: this },
                     { xtype: 'tbspacer', width: 5 },
                     labelVoc, 
                     arrow, 
                     labelConcept,
                     { xtype: 'tbspacer', width: 5 },
                     cbSubconcepts                     
                   ] 
        });
 
        return panel;
    }, 
    setText: function(text, lang) {
        var titleField = this.typeCondPanel.getComponent(0).getComponent(0);
        titleField.setValue(text);        
        var combobox = this.typeCondPanel.getComponent(0).getComponent(2);
        combobox.setValue(lang);        
    },
    setDateCond: function( relOp, date ) {
        var relOpField = this.typeCondPanel.getComponent(0).getComponent(0);
        relOpField.setValue( relOp );
        var dateComboBox = this.typeCondPanel.getComponent(0).getComponent(2);
        dateComboBox.setValue( date );
    },
    pickPerson: function() {
        this.pickIdentity('person');   
    },
    pickOrg: function() {
        this.pickIdentity('organization');   
    },
    pickIdentity: function(type) {
        var identityPicker = Ext.create('Comete.IdentityPicker', {
            lang: this.lang,
            type: type,   
            aListener: this   
        });        
        identityPicker.show();       
    },
    setIdentity: function(uri, label) {
        this.data = uri;
        var labelIdentity = this.typeCondPanel.getComponent(0).getComponent(2).getComponent(0);
        labelIdentity.addCls('queryCondData'),
        labelIdentity.setText(label);        
    },
    pickVocConcept: function() { 
        var vocConceptPicker = Ext.create('Comete.VocConceptPicker', {
            showIds: false,
            lang: this.lang,
            aListener: this   
        });        
        vocConceptPicker.show();         
    },
    setVocConcept: function(conceptUri, vocLabel, conceptLabel, isLeaf ) {
        this.data = conceptUri;
        var conceptPanel = this.typeCondPanel.getComponent(0);
        conceptPanel.getComponent(2).setText(vocLabel);
        conceptPanel.getComponent(3).setVisible(true);
        conceptPanel.getComponent(4).setText(conceptLabel);
        conceptPanel.getComponent(6).setVisible(!isLeaf);
    },
    getCondition: function() {
        var lang = null;
        var relOp = null;
        if (this.typeCond.getValue() == 'title' || this.typeCond.getValue() == 'description' || this.typeCond.getValue() == 'keyword') {
            this.data = this.typeCondPanel.getComponent(0).getComponent(0).getValue();
            lang = this.typeCondPanel.getComponent(0).getComponent(2).getValue();
        }
        else if(this.typeCond.getValue() == 'addedDate') {
            relOp = this.typeCondPanel.getComponent(0).getComponent(0).getValue();
            var date = this.typeCondPanel.getComponent(0).getComponent(2).getValue();

            // For > operator, round up the date to the following day because it's more natural to interpret it this way.
            // For instance, if we ask for resources with addedDate > 2013-10-20, we don't want to see resources
            // that have a addedDate equal to 2013-10-20T10:00:000Z but rather resources with addedDates like 
            // 2013-10-21T10:00:000z. - FB
            if( 'gt' == relOp )
                date.setDate( date.getDate() + 1 );

            var year = date.getYear() + 1900;
            var month = date.getMonth() + 1;
            var day = date.getDate();

            this.data = year + '-' + ( month < 10 ? '0' : '' ) + month + '-' + ( day < 10 ? '0' : '' ) + day;
        }
        if (this.data == null || this.data == "")
            return null;

        var label = null;
        if (this.typeCond.getValue().endsWith('contrib') || this.typeCond.getValue().endsWith('org'))
            label = this.typeCondPanel.getComponent(0).getComponent(2).getComponent(0).text;
        else if (this.typeCond.getValue() == 'vocConcept' || this.typeCond.getValue() == '!vocConcept') {            
            conceptPanel = this.typeCondPanel.getComponent(0);
            label = conceptPanel.getComponent(2).text + "#" + conceptPanel.getComponent(4).text;                        
        }
        var operator = { op: this.andOr.getValue() };
        var queryCond = { key: this.typeCond.getValue(), value: this.data };
        if (label) 
            queryCond.label = label;
        if (lang)
           queryCond.lang = lang;
        if (this.typeCond.getValue() == 'vocConcept' || this.typeCond.getValue() == '!vocConcept') {
            var cbSubconcepts = this.typeCondPanel.getComponent(0).getComponent(6);
            if (!cbSubconcepts.isVisible())
                queryCond.leaf = true; 
            if (cbSubconcepts.getValue())
                queryCond.subConcepts = true;
        }
        if (relOp)
            queryCond.relOp = relOp;

        return [operator, queryCond]; 
    },
    setQueryCondition: function(andOr, cond) {
        var type = cond.key;
        var option = "";
        //for the moment exclusives options -AM
        if (cond.subConcepts)
            option = "subConcepts";
        if (cond.leaf)
            option = "leaf";

        this.setAndOr(andOr);
        this.typeCond.setValue(type);
        this.currentType = type;
        this.makeCond(type);
        this.setData(cond.value, type, option, cond.label, cond.lang, cond.relOp);
    },
    setData: function(data, type, option, label, lang, relOp) {
        if (type == 'title' || type == 'description' || type == 'keyword') {
            this.setText(data, lang);
            return;
        }
        if (type =='addedDate') {
            this.setDateCond( relOp, data );
            return;
        }

        if (type == 'contrib' || type == '!contrib' || type == 'org' || type == '!org')
            this.setIdentity(data, label);
        else if (type == 'vocConcept' || type == '!vocConcept') {
            var index = label.indexOf("#");
            var vocLabel = label.substring(0, index);
            var conceptLabel = label.substring(index + 1);
            this.setVocConcept(data, vocLabel, conceptLabel);
            var cbSubconcepts = this.typeCondPanel.getComponent(0).getComponent(6); 
            cbSubconcepts.setValue(option == "subConcepts");
            cbSubconcepts.setVisible(option != "leaf");
        }
    }
});
