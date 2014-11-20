<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron">
	<sch:ns prefix="lom" uri="http://ltsc.ieee.org/xsd/LOM"/>
	<sch:ns prefix="lomfr" uri="http://www.lom-fr.fr/xsd/LOMFR"/>
	<sch:ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>

	<!-- Les métadonnées obligatoires en LOMFR-->
	<sch:pattern id="eltsObligatoires">
		<sch:title>Test de présence des éléments obligatoires</sch:title>
		<!-- remarque : pas de validation de la présence ou pas d'un contenu : du côté des applications -->
		<sch:rule context="/lom:lom/lom:general">
			<sch:assert test="lom:identifier/lom:catalog">Erreur catégorie "general" : Le document LOMFR doit avoir un identifiant dans un catalogue précisé (élément "identifier / catalog").</sch:assert>
			<sch:assert test="lom:identifier/lom:entry">Erreur catégorie "general" : Le document LOMFR doit avoir un identifiant (élément "identifier / entry").</sch:assert>
			<sch:assert test="lom:title/lom:string">Erreur catégorie "general" : Le document LOMFR doit présenter un titre (élément "title").</sch:assert>
		</sch:rule>
	</sch:pattern>

	<sch:pattern id="eltsSupprimes">
		<sch:title>Tests de non utilisation des éléments qui étaient présents en LOM et qui ont été supprimés en LOMFR</sch:title>
		<sch:rule context="/lom:lom/lom:educational">
			<sch:assert test="not(lom:interactivityType)">Erreur catégorie "educational" : L'élément "interactivityType" a été supprimé dans le LOMFR.</sch:assert>
			<sch:assert test="not(lom:interactivityLevel)">Erreur catégorie "educational" : L'élément "interactivityLevel" a été supprimé dans le LOMFR.</sch:assert>
			<sch:assert test="not(lom:semanticDensity)">Erreur catégorie "educational" : L'élément "semanticDensity" a été supprimé dans le LOMFR.</sch:assert>
		</sch:rule>
	</sch:pattern>

	<sch:pattern id="vocabSupprimes">
		<sch:title>Tests de non utilisation des vocabulaires qui étaient présents en LOM et qui ont été supprimés en LOMFR</sch:title>
		<sch:rule context="lom:lom/lom:educational/lom:learningResourceType/lom:value">
			<sch:assert test="not(text() = 'problem statement'  or text() = 'self assessment' or text() = 'diagram' or text() = 'figure' or text() = 'graph' or text() = 'index' or text() = 'slide' or text() = 'table' or text() = 'narrative text')">Erreur catégorie "educational", élément "learningResourceType" : La valeur "<sch:value-of select="text()"/>" a été supprimée de ce vocabulaire dans le LOMFR.</sch:assert>
		</sch:rule>
	</sch:pattern>

	<sch:pattern id="cardinalites">
		<sch:title>Tests de non répétabilité des éléments qui sont passés de répétables en LOM à non répétables en LOMFR</sch:title>

		<sch:rule context="lom:lom/lom:general">
			<sch:assert test="count(lom:description) &lt;= 1">Erreur catégorie "general" : L'élément "description" n'est pas répétable en LOMFR.</sch:assert>
		</sch:rule>
		<sch:rule context="lom:lom/lom:lifeCycle/lom:contribute">
			<sch:assert test="count(lom:entity) &lt;= 1">Erreur catégorie "lifeCycle" : L'élément "entity" n'est pas répétable en LOMFR.</sch:assert>
		</sch:rule>
		<sch:rule context="lom:lom/lom:educational">
			<sch:assert test="count(lom:description) &lt;= 1">Erreur catégorie "educational" : L'élément "description" n'est pas répétable en LOMFR.</sch:assert>
		</sch:rule>
	</sch:pattern>

	<sch:pattern id="langstring">
		<sch:title>Tests d'unicité des balises string par langue et présence obligatoire de l'attribut language pour chaque élément de type LangString</sch:title>

		<sch:rule abstract="true" id="attributLanguageCheck">
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:assert test="@language"> Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:value-of select="name(parent::node())"/>/<sch:name/>" : la présence d'un attribut "language" pour les balises "string" des éléments de type LangString est obligatoire.</sch:assert>
		</sch:rule>
		
		<sch:rule context="/lom:lom//lom:string">
			<sch:extends rule="attributLanguageCheck"/>
		</sch:rule>

		<sch:rule abstract="true" id="uniciteLanguageCheck">
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:assert test="count(lom:string[@language = preceding-sibling::node()/@language]) = 0">Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:name/>" : les éléments de type LangString ne doivent contenir qu'une balise "string" par langue. </sch:assert>
		</sch:rule>
		
		<sch:rule context="/lom:lom//child::node()[lom:string]">
			<sch:extends rule="uniciteLanguageCheck"/>
		</sch:rule>
	</sch:pattern>

	<sch:pattern id="vocab">
		<sch:title>Tests de présence des balises source et value et de leurs valeurs pour chaque élément</sch:title>

		<!--Règles abstraites : balises source et value appartenant au nom de domaine LOM-->
		<sch:rule abstract="true" id="balisesLOMCheck">
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>

			<sch:assert test="lom:value">Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:name/>" : les éléments de types vocabulaires doivent contenir un élément value.</sch:assert>
			<sch:assert test="lom:source">Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:name/>" : les éléments de types vocabulaires doivent contenir un élément source. </sch:assert>
			<!--<sch:assert test="lom:source">test</sch:assert>-->
		</sch:rule>

		<!--Règles abstraites : balises source et value appartenant au nom de domaine LOMFR-->
		<sch:rule abstract="true" id="balisesLOMFRCheck">
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:assert test="lomfr:value">Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:name/>" : les éléments de types vocabulaires doivent contenir un élément value. </sch:assert>
			<sch:assert test="lomfr:source">Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:name/>" : les éléments de types vocabulaires doivent contenir un élément source. </sch:assert>
		</sch:rule>

		<!--Règle abstraite : valeur de l'élément SOURCE des vocabulaires LOM-->
		<sch:rule abstract="true" id="sourceLOMCheck">
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:let name="value" value="parent::node()/node()[name() = 'value']"/>
			<sch:assert test="text() = 'LOMv1.0'">Erreur catégorie "<sch:value-of select="name(ancestor::node()[$nbAncestor - 2])"/>", élément "<sch:value-of select="name(parent::node())"/>" : Le vocabulaire de cet élément est issu du LOM, il doit avoir comme source "LOMv1.0"</sch:assert>
		</sch:rule>

		<!--Règle abstraite : valeur de l'élément SOURCE des vocabulaires LOMFR-->
		<sch:rule abstract="true" id="sourceLOMFRCheck">
			<sch:let name="eltName" value="name(parent::node())"/>
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:let name="categorieName" value="name(ancestor::node()[$nbAncestor - 2])"/>

			<sch:assert test="text() = 'LOMFRv1.0'">Erreur catégorie "<sch:value-of select="$categorieName"/>", élément "<sch:value-of select="$eltName"/>" : Le vocabulaire de cet élément est issu du LOMFR, il doit avoir comme source "LOMFRv1.0"</sch:assert>
		</sch:rule>
		<!--Règle abstraite : valeur de l'élément SOURCE des valeurs LOM des vocabulaires mixtes-->
		<sch:rule abstract="true" id="sourceMixteLOMCheck">
			<sch:let name="eltName" value="name(parent::node())"/>
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:let name="categorieName" value="name(ancestor::node()[$nbAncestor - 2])"/>

			<sch:assert test="text() = 'LOMv1.0'">Erreur catégorie "<sch:value-of select="$categorieName"/>", élément "<sch:value-of select="$eltName"/>" : Le mot "<sch:value-of select="parent::node()/node()[name() = 'value']"/>" appartient à un vocabulaire issu du LOM, il doit avoir comme source "LOMv1.0"</sch:assert>
		</sch:rule>
		<!--Règle abstraite : valeur de l'élément SOURCE des valeurs LOMFR des vocabulaires mixtes-->
		<sch:rule abstract="true" id="sourceMixteLOMFRCheck">
			<sch:let name="eltName" value="name(parent::node())"/>
			<sch:let name="nbAncestor" value="count(ancestor::node())"/>
			<sch:let name="categorieName" value="name(ancestor::node()[$nbAncestor - 2])"/>

			<sch:assert test="text() = 'LOMFRv1.0'">Erreur catégorie "<sch:value-of select="$categorieName"/>", élément "<sch:value-of select="$eltName"/>" : Le mot "<sch:value-of select="parent::node()/node()[name() = 'value']"/>" appartient à un vocabulaire issu du LOMFR, il doit avoir comme source "LOMFRv1.0"</sch:assert>
		</sch:rule>

		<!-- 
			elements dont le vocabulaire est issu du LOM
		-->

		<!--/lom:general/lom:structure-->
		<sch:rule context="/lom:lom/lom:general/lom:structure">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:general/lom:structure/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:general/lom:aggregationLevel-->
		<sch:rule context="/lom:lom/lom:general/lom:aggregationLevel">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:general/lom:aggregationLevel/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:lifeCycle/lom:status-->
		<sch:rule context="/lom:lom/lom:lifeCycle/lom:status">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:lifeCycle/lom:status/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:metaMetadata/lom:contribute/lom:role-->
		<sch:rule context="/lom:lom/lom:metaMetadata/lom:contribute/lom:role">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:metaMetadata/lom:contribute/lom:role/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:technical/lom:requirement/lom:orComposite/lom:type-->
		<sch:rule context="/lom:lom/lom:technical/lom:requirement/lom:orComposite/lom:type">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule
			context="/lom:lom/lom:technical/lom:requirement/lom:orComposite/lom:type/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:educational/lom:intendedEndUserRole-->
		<sch:rule context="/lom:lom/lom:educational/lom:intendedEndUserRole">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lom:intendedEndUserRole/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:educational/lom:difficulty-->
		<sch:rule context="/lom:lom/lom:educational/lom:difficulty">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lom:difficulty/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:rights/lom:cost-->
		<sch:rule context="/lom:lom/lom:rights/lom:cost">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:rights/lom:cost/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:rights/lom:copyrightAndOtherRestrictions-->
		<sch:rule context="/lom:lom/lom:rights/lom:copyrightAndOtherRestrictions">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:rights/lom:copyrightAndOtherRestrictions/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!--/lom:classification/lom:purpose-->
		<sch:rule context="/lom:lom/lom:classification/lom:purpose">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:classification/lom:purpose/lom:source">
			<sch:extends rule="sourceLOMCheck"/>
		</sch:rule>

		<!-- 
			elements dont le vocabulaire est issu du LOMFR
		-->

		<!-- /lom:lom/lom:general/lomfr:documentType -->
		<sch:rule context="/lom:lom/lom:general/lomfr:documentType">
			<sch:extends rule="balisesLOMFRCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:general/lomfr:documentType/lomfr:source">
			<sch:extends rule="sourceLOMFRCheck"/>
		</sch:rule>

		<!-- /lom:lom/lom:educational/lomfr:activity -->
		<sch:rule context="/lom:lom/lom:educational/lomfr:activity">
			<sch:extends rule="balisesLOMFRCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lomfr:activity/lomfr:source">
			<sch:extends rule="sourceLOMFRCheck"/>
		</sch:rule>

		<!-- 
			elements dont le vocabulaire est mixte : issu du LOMFR et du LOM 
		-->

		<!-- /lom:lom/lom:lifeCycle/lomfr:contribute/lom:role -->
		<sch:rule context="/lom:lom/lom:lifeCycle/lom:contribute/lom:role">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:lifeCycle/lom:contribute/lom:role[lom:value/text() = 'author' or  lom:value/text() = 'publisher' or  lom:value/text() = 'graphical designer' or  lom:value/text() = 'instructional designer' or  lom:value/text() = 'subject matter expert' or  lom:value/text() = 'content provider' or  lom:value/text() = 'technical implementer' or  lom:value/text() = 'unknown' or  lom:value/text() = 'initiator' or  lom:value/text() = 'editor' or  lom:value/text() = 'script writer' or  lom:value/text() = 'terminator' or  lom:value/text() = 'validator' or  lom:value/text() = 'educational validator' or  lom:value/text() = 'technical validator']/lom:source">
			<sch:extends rule="sourceMixteLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:lifeCycle/lom:contribute/lom:role[lom:value/text() = 'contributeur']/lom:source">
			<sch:extends rule="sourceMixteLOMFRCheck"/>
		</sch:rule>

		<!-- /lom:lom/lom:technical/lom:requirement/lom:orComposite/lom:name -->
		<sch:rule context="/lom:lom/lom:technical/lom:requirement/lom:orComposite/lom:name">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:technical/lom:requirement/lom:orComposite/lom:name[lom:value/text() = 'pc-dos' or  lom:value/text() = 'ms-windows' or  lom:value/text() = 'macos' or  lom:value/text() = 'unix' or  lom:value/text() = 'multi-os' or  lom:value/text() = 'none' or lom:value/text() = 'any' or  lom:value/text() = 'opera' or  lom:value/text() = 'ms-internet explorer' or  lom:value/text() = 'amaya' or  lom:value/text() = 'netscape communicator']/lom:source">
			<sch:extends rule="sourceMixteLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:technical/lom:requirement/lom:orComposite/lom:name[lom:value/text() = 'linux' or lom:value/text() = 'firefox' or lom:value/text() = 'safari']/lom:source">
			<sch:extends rule="sourceMixteLOMFRCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:technical/lom:requirement/lom:orComposite[lom:type/lom:value = 'operating system']/lom:name/lom:value">
			<sch:assert test="text() = 'pc-dos' or  text() = 'ms-windows' or  text() = 'macos' or  text() = 'unix' or  text() = 'multi-os' or  text() = 'none' or text() = 'linux'">Erreur catégorie "technical", élément "name" : Le vocabulaire "<sch:value-of select="parent::node()/node()[name() = 'value']"/>" est lié au navigateur (type = 'browser') et non au système d'exploitation (type = 'operating system').</sch:assert>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:technical/lom:requirement/lom:orComposite[lom:type/lom:value = 'browser']/lom:name/lom:value">
			<sch:assert test="text() = 'any' or  text() = 'opera' or  text() = 'ms-internet explorer' or  text() = 'amaya' or  text() = 'netscape communicator' or text() = 'firefox' or text() = 'safari'">Erreur catégorie "technical", élément "name" : Le vocabulaire "<sch:value-of select="parent::node()/node()[name() = 'value']"/>" est lié au système d'exploitation (type = 'operating system') et non au navigateur (type = 'browser').</sch:assert>
		</sch:rule>

		<!-- /lom:lom/lom:educational/lom:learningResourceType -->
		<sch:rule context="/lom:lom/lom:educational/lom:learningResourceType">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lom:learningResourceType[lom:value/text() = 'exercise' or lom:value/text() = 'simulation' or lom:value/text() = 'questionnaire' or lom:value/text() = 'exam' or lom:value/text() = 'experiment' or lom:value/text() = 'lecture']/lom:source">
			<sch:extends rule="sourceMixteLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lom:learningResourceType[lom:value/text() = 'démonstration' or lom:value/text() = 'évaluation' or lom:value/text() = 'animation' or lom:value/text() = 'tutoriel' or lom:value/text() = 'glossaire' or lom:value/text() = 'guide' or lom:value/text() = 'matériel de référence' or lom:value/text() = 'méthodologie' or lom:value/text() = 'outil' or lom:value/text() = 'scénario pédagogique']/lom:source">
			<sch:extends rule="sourceMixteLOMFRCheck"/>
		</sch:rule>

		<!-- /lom:lom/lom:educational/lom:context -->
		<sch:rule context="/lom:lom/lom:educational/lom:context">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lom:context[lom:value/text() = 'school' or  lom:value/text() = 'higher education' or  lom:value/text() = 'training' or  lom:value/text() = 'other']/lom:source">
			<sch:extends rule="sourceMixteLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:educational/lom:context[lom:value/text() = 'enseignement primaire' or lom:value/text() = 'enseignement secondaire' or lom:value/text() = 'licence' or lom:value/text() = 'master' or lom:value/text() = 'doctorat' or lom:value/text() = 'formation continue' or lom:value/text() = 'formation en entreprise']/lom:source">
			<sch:extends rule="sourceMixteLOMFRCheck"/>
		</sch:rule>

		<!-- /lom:lom/lom:relation/lom:kind -->
		<sch:rule context="/lom:lom/lom:relation/lom:kind">
			<sch:extends rule="balisesLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:relation/lom:kind[lom:value/text() = 'ispartof' or  lom:value/text() = 'haspart' or  lom:value/text() = 'isversionof' or  lom:value/text() = 'hasversion' or  lom:value/text() = 'isformatof' or  lom:value/text() = 'hasformat' or  lom:value/text() = 'references' or  lom:value/text() = 'isreferencedby' or  lom:value/text() = 'isbasedon' or  lom:value/text() = 'isbasisfor' or  lom:value/text() = 'requires' or  lom:value/text() = 'isrequiredby']/lom:source">
			<sch:extends rule="sourceMixteLOMCheck"/>
		</sch:rule>
		<sch:rule context="/lom:lom/lom:relation/lom:kind[lom:value/text() = &quot;est associée à&quot; or lom:value/text() = &quot;est la traduction de&quot; or lom:value/text() = &quot;fait l&apos;objet d’une traduction&quot; or lom:value/text() = &quot;est la localisation de&quot; or lom:value/text() = &quot;fait l&apos;objet d’une localisation&quot; or lom:value/text() = &quot;est pré-requis de&quot; or lom:value/text() = &quot;a pour pré-requis&quot;]/lom:source">
			<sch:extends rule="sourceMixteLOMFRCheck"/>
		</sch:rule>
	</sch:pattern>
</sch:schema>
