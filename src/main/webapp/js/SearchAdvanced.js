function addCondition() {
    var asp = Ext.getCmp('advancedSearchPanel');
    asp.addQueryCond(asp.createQueryCond( false ));
}

Ext.define( 'Comete.AdvancedSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {

        this.introLabel = Ext.create('Ext.form.Label', {
            text: tr('Search of resources') + '...',
            cls: 'intro'
        } );
        
        this.advSearchQueryButton = Ext.create('Ext.button.Button', {
            text: tr('Search'),
            region: 'east',
            handler: this.submitAdvancedSearchQuery,
            scope: this
        } );

        this.condPanel = Ext.create('Ext.panel.Panel', {
            layout: 'vbox',
            border: false, 
            region: 'west'
        });

        this.addCondition = Ext.create('Ext.Component', {
            html: '<a style="color: #04408c;" href="javascript:addCondition();">' + tr( 'Add condition' ) + '</a>'
        } );
        
        var cfg = {
            id: 'advancedSearchPanel',
            layout: 'border',
            region: 'center', 
            border: false,
            margin: '0 0 0 10',
            items: [ { border: false, region: 'north', items: this.introLabel, height: 26 }, 
                       this.condPanel,
                     { border: false, region: 'south', layout: 'vbox', 
                       items: [ this.addCondition, { xtype: 'tbspacer', height: 10 }, this.advSearchQueryButton, { xtype: 'tbspacer', height: 15 } ] } ]   
            
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
        setQueryPanelHeight(Math.max(24*elems + 5*(elems - 1) + 155, ADVANCED_HEIGHT), true);
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
        
        setQueryPanelHeight(Math.max(24*elems + 5*(elems - 1) + 155, ADVANCED_HEIGHT), true);
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
    getQuery: function() {
        return( this.buildAdvancedSearchQuery() );
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

        this.flagStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data: [
                {'id':'forcedDiffusion', label: tr('Forced Diffusion')},
                {'id':'inactive', label: tr('Inactive')},
                {'id':'invalid', label: tr('Invalid')},
                {'id':'brokenLink', label: tr('Broken Link')},
                {'id':'pending', label: tr('Pending for approval')}
            ]
        });

        this.repositoryStore = Ext.create('Ext.data.Store', {
            model: 'RepositoryModel',
            proxy: repositoryProxy
        });

        var choices = [
                {'id':'title', 'label': tr('whose title contains') },
                {'id':'description', 'label': tr('whose description contains') },
                {'id':'keyword', 'label': tr('having keyword') },
                {'id':'addedDate', 'label': tr('where the addition date') },
                {'id':'contrib', 'label': tr('from contributor') },
                {'id':'!contrib', 'label': tr('not from contributor') },
                {'id':'org', 'label': tr('from organization') },
                {'id':'!org', 'label': tr('not from organization') },
                {'id':'vocConcept', 'label': tr('related to the category') },
                {'id':'!vocConcept', 'label': tr('not related to the category') }
            ];


        if (isAdmin()) {
            choices.push( {'id':'fromHarvestedRepo', 'label': tr('from the harvested repository') }, 
                          {'id':'!fromHarvestedRepo', 'label': tr('not from the harvested repository') },
                          {'id':'flag', 'label': tr('having flag') }, 
                          {'id':'!flag', 'label': tr('not having flag') } );
        }

        this.choiceStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data: choices
        });

        this.andOr = Ext.create('Ext.form.field.ComboBox', {
            valueField: 'id',
            displayField: 'label',
            store: this.andOrStore,
            width: 65,
            editable: false,
            value: 'AND',
            hidden: this.isFirst,
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
        });
 
        this.andOr.on('select', this.andOrSelected, this);

        this.andOrSpace = Ext.create('Ext.toolbar.Spacer', {
            width: 5,
            hidden: this.isFirst
        });

        this.removeButton = Ext.create('Comete.ImageButton', {
            img: 'images/trash.png',
            margin: '3 0 0 0',
            handler: this.remove,
            scope: this,
            hidden: this.isFirst,
            tooltip: tr('Remove condition')
        } );

        this.removeSpace = Ext.create('Ext.toolbar.Spacer', {
            width: 15,
            hidden: this.isFirst
        });

        this.firstSpacer = Ext.create('Ext.toolbar.Spacer', {
            width: 70,
            hidden: true
        });

        var separator = (isAdmin()?'<tpl if="xindex == 10"><hr /></tpl>':'');
 
        this.typeCond = Ext.create('Ext.form.field.ComboBox', {
            valueField: 'id',
            displayField: 'label',
            store: this.choiceStore,
            width: 240,
            listConfig: {
                maxHeight: 400
            },
            editable: false,
            value: 'title',
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div>' + separator + '</tpl></div>'
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
        else if (type == 'flag' || type == '!flag')
            this.element = this.createFlagCond();
        else if (type == 'fromHarvestedRepo' || type == '!fromHarvestedRepo')
            this.element = this.createFromHarvestedRepoCond();
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
        var textfield = Ext.create('Ext.form.field.Text', {
            width: 250
        });
        return textfield;
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
            store.proxy.url = 'rest/queryEngine/keywords';
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
            },
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{keyword}</div></tpl></div>'
        } );

        return keywordCombo;
    },    
    createAddedDateCond: function() { 
        var relOpStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'label'],
            data: [
                {'id':'gte', 'label': tr( 'is from the' ) },
                {'id':'lte', 'label': tr( 'is until the' ) },
                {'id':'eq', 'label': tr( 'is the' ) }
            ]
        });

        var relOpComboBox = Ext.create('Ext.form.field.ComboBox', {
            valueField: 'id',
            displayField: 'label',
            store: relOpStore,
            width: 120,
            editable: false,
            value: 'gte',
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
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
            margin: '4 0 0 4'
        });  

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ { xtype: 'imagebutton', 
                       img: 'images/person.gif', 
                       margin: '4 0 0 0',
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
            margin: '4 0 0 4'
        });  

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ { xtype: 'imagebutton', 
                       img: 'images/building.png', 
                       margin: '4 0 0 0',
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
            margin: '4 0 0 0'
        });  

        var arrow = Ext.create('Ext.Img', {
            src: 'images/split-arrow-tiny.png', 
            margin: '1 6 0 6',
            hidden: true
        });  

        var labelConcept = Ext.create('Ext.form.Label', {
            text: tr('Select category') + '...',
            margin: '4 0 0 0'
        });  

        var cbSubconcepts = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('and subcategories'),
            style: 'color: #04408C',
            checked: true,
            hidden: true
        } );

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ { xtype: 'imagebutton', 
                       img: 'images/tree.gif', 
                       margin: '4 0 0 1',
                       handler: this.pickVocConcept, 
                       scope: this },
                     { xtype: 'tbspacer', width: 10 },
                     labelVoc, 
                     arrow, 
                     labelConcept,
                     { xtype: 'tbspacer', width: 10 },
                     cbSubconcepts
                   ] 
        });
 
        return panel;
    },
    createFlagCond: function() {
        var flagCombo = Ext.create('Ext.form.field.ComboBox', {
            valueField: 'id', 
            displayField: 'label',
            store: this.flagStore,
            editable: false,
            width: 180,
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
        });

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ flagCombo ]
        });

        return panel;
    }, 
    createFromHarvestedRepoCond: function() {
        var flagCombo = Ext.create('Ext.form.field.ComboBox', {
            valueField: 'uri', 
            displayField: 'label',
            store: this.repositoryStore,
            editable: false,
            width: 200,
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
        });

        var panel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: [ flagCombo ]
        });

        return panel;
    }, 
    setText: function(text) {
        var field = this.typeCondPanel.getComponent(0);
        field.setValue(text);
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
    setVocConcept: function(vocUri, conceptUri, vocLabel, conceptLabel, isLeaf ) {
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
            this.data = this.typeCondPanel.getComponent(0).getValue();
        }
        else if(this.typeCond.getValue() == 'addedDate') {
            relOp = this.typeCondPanel.getComponent(0).getComponent(0).getValue();
            var date = this.typeCondPanel.getComponent(0).getComponent(2).getValue();

            var year = date.getYear() + 1900;
            var month = date.getMonth() + 1;
            var day = date.getDate();

            this.data = year + '-' + ( month < 10 ? '0' : '' ) + month + '-' + ( day < 10 ? '0' : '' ) + day;
        }
        else if(this.typeCond.getValue().endsWith('fromHarvestedRepo'))
            this.data = this.typeCondPanel.getComponent(0).getComponent(0).getValue();
        else if(this.typeCond.getValue().endsWith('flag'))
            this.data = this.typeCondPanel.getComponent(0).getComponent(0).getValue();

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
        this.setData(cond.value, type, option, cond.label, cond.relOp);
    },
    setData: function(data, type, option, label, relOp) {
        if (type == 'title' || type == 'description' || type == 'keyword') {
            this.setText(data);
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
