function addCondition() {
    var asp = Ext.getCmp('advancedSearchPanel');
    asp.needWidthUpdate = true;
    asp.addQueryCond(asp.createQueryCond( false ));
}

Ext.define( 'Comete.AdvancedSearch', {
    extend: 'Ext.panel.Panel',
    layout: 'border',  
    initComponent: function( config ) {

        this.simpleButton = Ext.create('Ext.Component', {
            html: '<div class="whiteLink"><a href="javascript:changeCardItem(0);">' + tr( 'Quick Search' ) + '</a></div>'
        } );

        this.thematicButton = Ext.create('Ext.Component', {          
            html: '<div class="whiteLink"><a href="javascript:changeCardItem(2);">' + tr( 'Thematic Navigation' ) + '</a></div>'
        } );

        this.goBackwardQueryButton = Ext.create('Comete.ImageButton', {
            img: 'images/goBackwardQuery.png',
            imgDisabled: 'images/goBackwardQueryDisabled.png',
            width: 20,
            height: 20,
            disabled: true,
            tooltip: tr( 'Go back one query' ),
            handler: this.goBackwardQuery,
            scope: this
        } );

        this.goForwardQueryButton = Ext.create('Comete.ImageButton', {
            img: 'images/goForwardQuery.png',
            imgDisabled: 'images/goForwardQueryDisabled.png',
            width: 20,
            height: 20,
            disabled: true,
            tooltip: tr( 'Go forward one query' ), 
            handler: this.goForwardQuery,
            scope: this
        } );

        this.goBackwardQueryButton.on('render', function() {
            this.goBackwardQueryButton.getEl().setOpacity(0);
        }, this);

        this.goForwardQueryButton.on('render', function() {
            this.goForwardQueryButton.getEl().setOpacity(0);
        }, this);

        this.introLabel = Ext.create('Ext.form.Label', {
            text: tr('Search of resources') + '...',
            cls: 'intro'
        } );
        
        this.fulltextField = Ext.create('Ext.form.field.Text', {
            width: CARDPANEL_WIDTH,
            height: 24,
            emptyText: tr('Enter your request here'),
            fieldStyle: { 
                border: 'none',
                fontSize: '14px' 
            }
        } );

        if( this._query )
            this.fulltextField.setValue( this._query[0].value );

        this.fulltextField.on( 'specialkey', function( f, e ) {
            if( e.getKey() == e.ENTER ) {
                this.submitAdvancedSearchQuery(); 
            }
        }, this );
        
        this.searchQueryButton = Ext.create('Comete.ImageButton', {
            img: 'images/search.png',
            width: 24,
            height: 24,
            handler: this.submitAdvancedSearchQuery,
            scope: this
        } );

        this.simpleSearchBar = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            margin: '15 20 0 20',
            items: [ this.fulltextField, this.searchQueryButton ]
        } );

        this.condPanel = Ext.create('Ext.panel.Panel', {
            layout: 'vbox',
            border: false, 
            cls: 'transp2',
            margin: '10 20 0 20'
        });

        this.addCondition = Ext.create('Ext.Component', {
            html: '<a style="color: white" href="javascript:addCondition();">' + tr( 'Add condition' ) + '</a>'
        } );
        
        this.info = Ext.create('Comete.ImageButton', {
            img: 'images/info.png',
            width: 16,
            height: 16,
            margin: '2 0 0 0',
            tooltip: 'Info', 
            handler: this.info,
            scope: this
        } );

        var cfg = {
            id: 'advancedSearchPanel',
            layout: 'vbox',
            height: ADVANCED_HEIGHT,
            cls: 'searchPanel',
            items: [
                     { layout: 'hbox', cls: 'transp2', border: false, margin: '10 20 0 20', width: CARDPANEL_WIDTH + 24, 
                       items: [ { xtype: 'label', text: tr('ADVANCED SEARCH'), 
                                     style: {color: 'white', fontWeight: 'bold', fontSize: '14px' } }, 
                                { xtype: 'tbspacer', width: 5 }, this.info,
                                { xtype: 'tbfill' }, this.simpleButton, 
                                { xtype: 'tbspacer', width: 15 }, this.thematicButton, { xtype: 'tbspacer', width: 1 } ] },
                     this.simpleSearchBar,
                     this.condPanel,
                     { xtype: 'tbfill' },
                     { layout: 'hbox', cls: 'transp2', border: false, margin: '10 20 0 20', width: CARDPANEL_WIDTH + 24, 
                       items: [ this.addCondition, { xtype: 'tbfill' }, this.goBackwardQueryButton, { xtype: 'tbspacer', width: 4 }, this.goForwardQueryButton] },
                     { xtype: 'tbspacer', height: 10 }
                   ]               
        };
        Ext.apply(this, cfg);
        this.callParent(arguments);

        this.needWidthUpdate = false;
        this.addQueryCond( this.createQueryCond( true ));
        this.needWidthUpdate = true;
    },
    info: function() {
        if (this.infoWindow != undefined)
            this.infoWindow.close();

        this.infoWindow = new Ext.window.Window( {
            title: tr('Advanced Search'),
            width: 450,
            height: 300,
            resizable: false,
            html: '<iframe width="100%" height="100%" frameborder="0" src="' + this.lang + '/advancedSearchInfo.html"></iframe>' 
        } );
        this.infoWindow.show();
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
            this.firstCond.endSpace.setWidth(8);
            this.firstCond.removeButton.setVisible(true);
            newCond.firstAnd.setVisible(false);
            this.firstCond.firstAnd.setWidth(40);
        }
        else
            this.firstCond = newCond;
        
        return newCond;
    },
    addQueryCond: function(cond) {
        this.condPanel.add(cond); 
        this.updateElementsWidth();
        var elems = this.condPanel.items.length;
        setAdvancedSearchPanelHeight(Math.max(22*elems + 5*(elems - 1) + 125, ADVANCED_HEIGHT));
    },
    removeQueryCond: function(cond) {
        //refresh improvment. Update width only when deleted condition has max width -AM
        cond.setWidth(null);
        var removeCondWidth = cond.getWidth();
        var maxWidth = CARDPANEL_WIDTH;;
        for (i = 0; i < this.condPanel.items.length; i++) {
            var c = this.condPanel.getComponent(i); 
            maxWidth = Math.max(maxWidth, c.getWidth());   
        }
        var needWidthUpdate = ( this.condPanel.items.length == 2 ||
                                ( removeCondWidth == maxWidth && maxWidth > CARDPANEL_WIDTH + 24) );
        //

        this.condPanel.remove(cond);
        var elems = this.condPanel.items.length;

        if (this.firstCond != this.condPanel.getComponent(0)) {
            this.firstCond = this.condPanel.getComponent(0)
            this.firstCond.isFirst = true;
        }
        this.firstCond.andOr.setVisible(false);
        this.firstCond.andOr.setValue("AND");
        this.firstCond.endSpace.setWidth(elems > 1?8:24);
        this.firstCond.removeButton.setVisible(elems > 1);
        this.firstCond.firstAnd.setVisible(true); 
        if (elems == 1)
            this.firstCond.firstAnd.setWidth(null);
        else
            this.firstCond.firstAnd.setWidth(40);
        
        if (needWidthUpdate)
            this.updateElementsWidth(true);

        setAdvancedSearchPanelHeight(Math.max(22*elems + 5*(elems - 1) + 125, ADVANCED_HEIGHT));        
    },
    updateElementsWidth: function(resetAll) {
       if (!this.needWidthUpdate)
            return;

        var maxWidth = CARDPANEL_WIDTH;
        for (i = 0; i < this.condPanel.items.length; i++) {
            var cond = this.condPanel.getComponent(i);
            if (resetAll || i == 0)
                cond.setWidth(null);
            maxWidth = Math.max(maxWidth, cond.getWidth());
        } 

        var width = maxWidth;
        if (width <= CARDPANEL_WIDTH + 24)
            width = CARDPANEL_WIDTH;
        else
            width -= 24;
        this.fulltextField.setWidth(width);

        width = maxWidth;
        if (width <= CARDPANEL_WIDTH + 24)
            width = CARDPANEL_WIDTH + 24;
        
        for (i = 0; i < this.condPanel.items.length; i++) {
            var cond = this.condPanel.getComponent(i); 
            if (cond.getWidth() != width)
                cond.setWidth(width);
        } 

        //top and bottom menubar
        if (this.getComponent(0).getWidth() != width)
            this.getComponent(0).setWidth(width);
        if (this.getComponent(4).getWidth() != width)
            this.getComponent(4).setWidth(width);
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
        if (query.length > 0) {
            this.fadeInHistoryButtons();
            searchManager.setRequest( query ); 
        }
    },
    isQueryCriterias: function() {
        //check only from condition panel
        var query = this.buildAdvancedSearchQuery();
        if (query.length == 1 && query[0].key == "language")
            return false;
        return query.length > 0;
    },
    buildAdvancedSearchQuery: function() {
        var query = new Array();
        var j = 0;
        if (this.getFulltextQuery() != '') {
            query[j] = { key: "fulltext", value: this.getFulltextQuery() };
            j++;
        }
        if (searchManager.getLanguageCondition() != null) {
            if (j != 0) {
                query[j] = { "op": "AND" };
                j++;
            }
            query[j] = { key: "language", value: searchManager.getLanguageCondition() };
            j++;
        }
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
        this.needWidthUpdate = false;
        var queryConds = new Array();
        var j = 0;
        var i = 0;
        var isFulltext = query[0].key == 'fulltext';
        var offset = 0;
        if (isFulltext) {
            this.fulltextField.setValue(query[0].value);
            i += 2;
        }
        else 
            this.fulltextField.setValue(null);
        if (query[i].key == 'language')
            i += 2;
        var offset = i;
        for (; i < query.length; i++) {
            var cond = this.createQueryCond(j == 0);
            var op = "AND";
            if (i > offset) {
                op = query[i].op;
                i++;
            }           
            cond.setQueryCondition(op, query[i]);
            queryConds[j] = cond;
            j++;
        }
        if (queryConds.length == 0)
            queryConds[0] = this.createQueryCond(true);
        this.needWidthUpdate = true;
        this.clear();
        this.addQueryCond(queryConds);
    },
    redoRequest: function() {
        this.submitAdvancedSearchQuery();
    },
    getFulltextQuery: function() {
        return this.fulltextField.getValue();
    },
    setFulltextQuery: function(text) {
        if (this.fulltextField.getValue() == '' && !this.isQueryCriterias())
            this.fulltextField.setValue(text);
    },
    goBackwardQuery: function() {
        if (!this.goBackwardQueryButton.isDisabled()) 
            window.searchManager.goBackwardQuery();
    },
    goForwardQuery: function() {
        if (!this.goForwardQueryButton.isDisabled()) 
            window.searchManager.goForwardQuery();
    },
    fadeInHistoryButtons: function(isBackwardButtonDisabled, isForwardButtonDisabled) {
        this.goBackwardQueryButton.getEl().fadeIn({
            duration: 1500
        });      
        this.goForwardQueryButton.getEl().fadeIn({
            duration: 1500
        });      
    },
    updateQueryHistoryButtons: function(isBackwardButtonDisabled, isForwardButtonDisabled) {
        this.goBackwardQueryButton.getEl().setOpacity(1)
        this.goForwardQueryButton.getEl().setOpacity(1)
        this.goBackwardQueryButton.setDisabled(isBackwardButtonDisabled);
        this.goForwardQueryButton.setDisabled(isForwardButtonDisabled);
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
                {'id':'contrib', 'label': tr('from contributor') },
                {'id':'!contrib', 'label': tr('not from contributor') },
                {'id':'org', 'label': tr('from organization') },
                {'id':'!org', 'label': tr('not from organization') },
                {'id':'addedDate', 'label': tr('where the addition date') },
                {'id':'vocConcept', 'label': tr('related to the category') },
                {'id':'!vocConcept', 'label': tr('not related to the category') },
                {'id':'title', 'label': tr('whose title contains') },
                {'id':'description', 'label': tr('whose description contains') },
                {'id':'keyword', 'label': tr('having keyword') }
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
            width: 55,
            editable: false,
            value: 'AND',
            hidden: this.isFirst,
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div></tpl></div>'
        });
 
        this.andOr.on('select', this.andOrSelected, this);

        this.andOrSpace = Ext.create('Ext.toolbar.Spacer', {
            width: 5
        });

        this.removeButton = Ext.create('Comete.ImageButton', {
            img: 'images/trash.png',
            width: 16,
            height: 16,
            margin: '3 0 0 0',
            handler: this.remove,
            scope: this,
            hidden: this.isFirst,
            tooltip: tr('Remove condition')
        } );

        this.endSpace = Ext.create('Ext.toolbar.Spacer', {
            width: this.isFirst?24:8
        });

        this.firstAnd = Ext.create('Ext.form.Label', { 
            text: tr('AND'), 
            margin: '3 11 0 4'
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
            value: 'contrib',
            tpl: '<div><tpl for="."><div class="x-boundlist-item">{label}</div>' + separator + '</tpl></div>'
        });

        this.typeCond.on('select', this.typeCondSelected, this);

        this.typeCondPanel = Ext.create('Ext.panel.Panel', {
            layout: 'hbox',
            border: false,
            items: this.createContribCond()
        });
 
        cfg = {
            margin: '0 0 5 0',
            items: [ this.firstAnd,
                     this.andOr, this.andOrSpace, 
                     this.typeCond, { xtype: 'tbspacer', width: 5 },
                     this.typeCondPanel, { xtype: 'tbfill' }, this.endSpace, this.removeButton
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

        this.needWidthUpdate = true;
        this.topPane.updateElementsWidth(true);
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
            width: 220
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
            width: 220,
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
            src: 'images/split-arrow-tiny-white.png', 
            margin: '1 6 0 6',
            hidden: true
        });  

        var labelConcept = Ext.create('Ext.form.Label', {
            text: tr('Select category') + '...',
            margin: '4 0 0 0'
        });  

        var cbSubconcepts = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: tr('and subcategories'),
            style: 'color: white',
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
            width: 220,
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

        this.setWidth(null);
        this.topPane.updateElementsWidth(); 
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
        //conceptPanel.getComponent(6).setVisible(!isLeaf);  //for Ceres always checked and hidden -AM

        this.setWidth(null);
        this.topPane.updateElementsWidth(); 
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
