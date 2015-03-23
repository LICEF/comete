<xsl:stylesheet version="2.0"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns:comete="http://comete.licef.ca/reference#"
    xmlns:foaf="http://xmlns.com/foaf/0.1/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:util="java:ca.licef.comete.core.util.Util"
    xmlns:funct="http://comete.licef.ca/functions"
    exclude-result-prefixes="rdf dct comete foaf funct xs xsi xsl util">

    <xsl:include href="/xslt/charMaps.xsl"/>
    <xsl:include href="/xslt/util.xsl"/>

    <xsl:output method="xhtml" encoding="utf-8" indent="yes" use-character-maps="html-tags c1-control-range" omit-xml-declaration="yes"/>

    <xsl:param name="uri"/>

    <xsl:variable name="lang" select="'en'"/>
    <xsl:variable name="title" select="'Resource'"/>
    <xsl:variable name="HeaderAboutResource" select="'About the resource'"/>
    <xsl:variable name="HeaderDates" select="'Dates'"/>
    <xsl:variable name="HeaderLanguages" select="'Languages'"/>
    <xsl:variable name="HeaderKeywords" select="'Keywords'"/>
    <xsl:variable name="HeaderContributes" select="'Contributions'"/>
    <xsl:variable name="HeaderSubjects" select="'Associated Categories'"/>
    <xsl:variable name="HeaderEducationLevel" select="'Educational Level'"/>
    <xsl:variable name="HeaderIntellectualProperty" select="'Intellectual Property'"/>
    <xsl:variable name="ContributeLinkLabel" select="'Show more details'"/>
    <xsl:variable name="RelatedLearningObjectsToContribLinkLabel" select="'Show resources related to this contributor.'"/>
    <xsl:variable name="RelatedLearningObjectsToOrgLinkLabel" select="'Show resources related to this organization.'"/>
    <xsl:variable name="RelatedLearningObjectsToSubjectLinkLabel" select="'Show resources related to this category.'"/>
    <xsl:variable name="RelatedLearningObjectsToKeywordLinkLabel" select="'Show resources associated to this keyword.'"/>
    <xsl:variable name="LearningObjectLinkedDataLinkLabel" select="'Show resource''s linked data.'"/>
    <xsl:variable name="PersonLinkedDataLinkLabel" select="'Show person''s linked data.'"/>
    <xsl:variable name="OrgLinkedDataLinkLabel" select="'Show organization''s linked data.'"/>
    <xsl:variable name="RoleAuthor" select="'author'"/>
    <xsl:variable name="RolePublisher" select="'publisher'"/>
    <xsl:variable name="ResourceTypeText" select="'Text Resource'"/>
    <xsl:variable name="ResourceTypeAudio" select="'Audio Resource'"/>
    <xsl:variable name="ResourceTypeVideo" select="'Video Resource'"/>
    <xsl:variable name="ResourceTypeImage" select="'Graphical Resource'"/>
    <xsl:variable name="ResourceTypeApplication" select="'Executable Resource'"/>
    <xsl:variable name="ResourceTypeMisc" select="'Resource of unknown type'"/>
    <xsl:variable name="ResourceTypeArchive" select="'Archive Resource'"/>
    <xsl:variable name="ResourceTypeWord" select="'Word Document'"/>
    <xsl:variable name="ResourceTypeHtml" select="'Web Resource'"/>
    <xsl:variable name="ResourceTypePowerPoint" select="'PowerPoint Document'"/>
    <xsl:variable name="ResourceTypePDF" select="'PDF Document'"/>
    <xsl:variable name="ResourceTypeExcel" select="'Excel Document'"/>

    <xsl:variable name="ShareResource" select="'Share the resource'"/>
    <xsl:variable name="ShareOnFacebook" select="'Share the resource on Facebook'"/>
    <xsl:variable name="ShareOnTwitter" select="'Share the resource on Twitter'"/>
    <xsl:variable name="ShareOnLinkedin" select="'Share the resource on LinkedIn'"/>
    <xsl:variable name="ShareByEmail" select="'Share the resource by email'"/>
    <xsl:variable name="AddedDateLabel" select="'Added'"/>
    <xsl:variable name="CreatedDateLabel" select="'Created'"/>
    <xsl:variable name="UpdatedDateLabel" select="'Last modified'"/>
    <xsl:variable name="PartialDatePrefix" select="' in '"/>
    <xsl:variable name="PartialDateDelimiter" select="', '"/>
    <xsl:variable name="FullDatePattern" select="' on [MNn] [D], [Y]'"/>
    <xsl:variable name="Months" select="('January','February','March','April','May','June','July','August','September','October','November','December')"/>

    <xsl:template match="/">
        <xsl:apply-templates select="rdf:RDF/rdf:Description[@rdf:about=$uri]" mode="learningObject"/>
    </xsl:template>

    <xsl:template match="rdf:Description" mode="learningObject">
        <xsl:variable name="title">
            <xsl:choose>
                <xsl:when test="dct:title[@xml:lang=$lang][1]">
                    <xsl:value-of select="dct:title[@xml:lang=$lang][1]"/> 
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="$lang = 'en' and dct:title[@xml:lang='fr'][1]">
                            <xsl:value-of select="dct:title[@xml:lang='fr'][1]"/>
                        </xsl:when>
                        <xsl:when test="$lang = 'fr' and dct:title[@xml:lang='en'][1]">
                            <xsl:value-of select="dct:title[@xml:lang='en'][1]"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="dct:title[1]"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="ampersand"><![CDATA[&]]></xsl:variable>
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html></xsl:text>
        <html>
            <body id="LearningObjectBody" class="LearningObject">
                <div id="LearningObjectResource">
                <xsl:attribute name="resource"><xsl:value-of select="$uri"/></xsl:attribute>
                <div id="ResourceHeader">
                    <xsl:apply-templates select="foaf:page" mode="icon"/>
                    <p class="LearningObjectTitle">
                        <span id="LearningObjectResourceTitle" property="http://purl.org/dc/terms/title"><xsl:value-of select="$title"/></span>
                        <xsl:call-template name="render-linked-data-link">
                            <xsl:with-param name="uri" select="$uri"/>
                        </xsl:call-template>
                    </p>
                    <xsl:apply-templates select="foaf:page" mode="resType"/>
                    <xsl:apply-templates select="comete:learningResourceType"/>
                </div>
                <br clear="all"/>
                <xsl:apply-templates select="foaf:page" mode="link"/>
                <xsl:apply-templates select="dct:description"/>
                <h2 class="BigSectionHeader"><xsl:value-of select="$HeaderAboutResource"/></h2>
                <table width="100%">
                    <tr>
                        <xsl:variable name="isRightColVisible" select="comete:keyword or count(*[@navigable = 'true'][@vocabLabel]) &gt; 0"/>
                        <xsl:variable name="colWidth" select="if( $isRightColVisible ) then '50%' else '100%'"/>
                        <td valign="top" class="DetailTableLeftSide">
                            <xsl:attribute name="width"><xsl:value-of select="$colWidth"/></xsl:attribute>
                            <xsl:if test="dct:created or comete:added or comete:updated ">
                                <h2 class="SectionHeader"><xsl:value-of select="$HeaderDates"/></h2>
                                <ul>
                                    <xsl:apply-templates select="dct:created"/>
                                    <xsl:apply-templates select="comete:added"/>
                                    <xsl:apply-templates select="comete:updated"/>
                                </ul>
                            </xsl:if>
                            <xsl:if test="dct:language">
                                <h2 class="SectionHeader"><xsl:value-of select="$HeaderLanguages"/></h2>
                                <ul>
                                    <xsl:apply-templates select="dct:language">
                                        <xsl:sort select="funct:getLanguageString(.,$lang)" lang="$lang"/>
                                    </xsl:apply-templates>
                                </ul>
                            </xsl:if>
                            <xsl:if test="count(../contributes//contribute) &gt; 0">
                                <h2 class="SectionHeader"><xsl:value-of select="$HeaderContributes"/></h2>

                                <ul>
                                <xsl:apply-templates select="../contributes"/>

                                <xsl:for-each select="//dct:publisher[ substring-before( substring-after( @rdf:resource, '/resource/' ), '/' ) = 'organization' ]">
                                    <xsl:variable name="publisherUri" select="@rdf:resource"/>
                                    <xsl:variable name="publisher" select="if( ../../contributes/contribute[ rdf:RDF/rdf:Description/@rdf:about = $publisherUri ] ) then ../../contributes/contribute[ rdf:RDF/rdf:Description/@rdf:about = $publisherUri ] else (../../contributes/contribute/organization[ rdf:RDF/rdf:Description/@rdf:about = $publisherUri ])[ 1 ]"/>
                                    <xsl:call-template name="render-contribute">
                                        <xsl:with-param name="uri" select="$publisherUri"/>
                                        <xsl:with-param name="photo" select="if( $publisher/rdf:RDF/rdf:Description/foaf:img/@rdf:resource ) then 
                                                                                 $publisher/rdf:RDF/rdf:Description/foaf:img[1]/@rdf:resource 
                                                                             else if( $publisher/rdf:RDF/rdf:Description/foaf:logo/@rdf:resource ) then
                                                                                 $publisher/rdf:RDF/rdf:Description/foaf:logo[1]/@rdf:resource
                                                                             else ''"/>
                                        <xsl:with-param name="name" select="$publisher/rdf:RDF/rdf:Description/foaf:name[1]"/> 
                                        <xsl:with-param name="website" select="$publisher/rdf:RDF/rdf:Description/foaf:homepage/@rdf:resource"/>
                                        <xsl:with-param name="identityLink" select="$publisher/identity/@href"/>
                                    </xsl:call-template>
                                </xsl:for-each>
                                </ul>
                            </xsl:if>
                            <xsl:if test="comete:educationalLevel">
                                <h2 class="SectionHeader"><xsl:value-of select="$HeaderEducationLevel"/></h2>
                                <ul>
                                <xsl:apply-templates select="comete:educationalLevel"/>
                                </ul>
                            </xsl:if>
                            <xsl:if test="comete:intellectualProperty">
                                <h2 class="SectionHeader"><xsl:value-of select="$HeaderIntellectualProperty"/></h2>
                                <xsl:apply-templates select="comete:intellectualProperty"/>
                            </xsl:if>
                        </td>
                        <xsl:if test="$isRightColVisible">
                            <td valign="top" class="DetailTableRightSide">
                                <div class="HighlightedBox">
                                    <xsl:attribute name="width"><xsl:value-of select="$colWidth"/></xsl:attribute>
                                    <xsl:if test="comete:keyword">
                                        <h2 class="SectionHeader"><xsl:value-of select="$HeaderKeywords"/></h2>
                                        <ul>
                                            <xsl:apply-templates select="comete:keyword">
                                                <xsl:sort select="." lang="$lang"/>
                                            </xsl:apply-templates>
                                        </ul>
                                    </xsl:if>
                                    <xsl:if test="count(*[@navigable = 'true'][@vocabLabel]) &gt; 0">
                                        <h2 class="SectionHeader"><xsl:value-of select="$HeaderSubjects"/></h2>
                                        <xsl:for-each-group select="*[@navigable = 'true'][@vocabLabel]" group-by="@vocabLabel">
                                            <xsl:sort select="current-grouping-key()" lang="$lang"/>
                                                <ul>
                                                <xsl:for-each select="current-group()">
                                                    <xsl:sort select="@conceptLabel" lang="$lang"/>
                                                    <li><xsl:value-of select="@vocabLabel"/><img style="margin-bottom:-1px; margin-left:8px" src="images/split-arrow-tiny.png" width="12" height="17"/><a class="RelatedLearningObjectsLink"><xsl:attribute name="href">javascript:setRequestVocConcept( '<xsl:value-of select="@rdf:resource"/>' );</xsl:attribute><xsl:value-of select="@conceptLabel"/></a></li>
                                                </xsl:for-each>
                                                </ul>
                                        </xsl:for-each-group>
                                    </xsl:if>
                                </div>
                            </td>
                        </xsl:if>
                    </tr>
                </table>

                <div class="Footer">
                    <div id="SharingIcons">
                        <h2 class="BigSectionHeader"><xsl:value-of select="$ShareResource"/></h2>
                        <a id="ShareOnFacebookLink" target="_blank">
                            <img src="images/shareOnFacebook.png" width="24" height="24">
                                <xsl:attribute name="title"><xsl:value-of select="$ShareOnFacebook"/></xsl:attribute>
                                <xsl:attribute name="alt"><xsl:value-of select="$ShareOnFacebook"/></xsl:attribute>
                            </img>
                        </a>
                        <a id="ShareOnTwitter" target="_blank">
                            <img src="images/shareOnTwitter.png" width="24" height="24">
                                <xsl:attribute name="title"><xsl:value-of select="$ShareOnTwitter"/></xsl:attribute>
                                <xsl:attribute name="alt"><xsl:value-of select="$ShareOnTwitter"/></xsl:attribute>
                            </img>
                        </a>
                        <a id="ShareOnLinkedin" target="_blank">
                            <img src="images/shareOnLinkedin.png" width="24" height="24">
                                <xsl:attribute name="title"><xsl:value-of select="$ShareOnLinkedin"/></xsl:attribute>
                                <xsl:attribute name="alt"><xsl:value-of select="$ShareOnLinkedin"/></xsl:attribute>
                            </img>
                        </a>
                        <a id="ShareByEmail" target="_blank">
                            <img src="images/shareByEmail.png" width="24" height="24">
                                <xsl:attribute name="title"><xsl:value-of select="$ShareByEmail"/></xsl:attribute>
                                <xsl:attribute name="alt"><xsl:value-of select="$ShareByEmail"/></xsl:attribute>
                            </img>
                        </a>
                    </div>
                </div>
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="dct:description">
        <!-- The translate function combined with the html-tags character-set defined at the top are needed
             to render the html tags included in the description in such a way that 
             they can be interpreted (and therefore rendered) by the browser. -->
        <div class="LearningObjectDescription">
            <h2 class="SectionHeader">Description</h2>
            <p><xsl:value-of select="translate(., '&lt;&gt;&amp;', '&#xE001;&#xE002;&#xE003;' )"/></p>
        </div>
    </xsl:template>

    <xsl:template match="dct:created">
        <li><xsl:value-of select="$CreatedDateLabel"/>
            <xsl:call-template name="render-datestamp">
                <xsl:with-param name="datestampStr" select="."/>
            </xsl:call-template>
        </li>
    </xsl:template>

    <xsl:template match="comete:added">
        <li><xsl:value-of select="$AddedDateLabel"/> 
            <xsl:call-template name="render-datestamp">
               <xsl:with-param name="datestampStr" select="."/>
            </xsl:call-template>
        </li>
    </xsl:template>

    <xsl:template match="comete:updated">
        <li><xsl:value-of select="$UpdatedDateLabel"/> 
            <xsl:call-template name="render-datestamp">
                <xsl:with-param name="datestampStr" select="."/>
            </xsl:call-template>
        </li>
    </xsl:template>

    <xsl:template match="dct:language">
        <li><xsl:value-of select="funct:getLanguageString(.,$lang)"/></li>
    </xsl:template>

    <xsl:template match="comete:keyword">
        <xsl:variable name="keyword">
            <xsl:call-template name="escape-single-quotes">
                <xsl:with-param name="string" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <li><a><xsl:attribute name="href">javascript:setRequestKeyword( '<xsl:value-of select="$keyword"/>', '<xsl:value-of select="@xml:lang"/>' );</xsl:attribute><xsl:value-of select="."/></a></li>
    </xsl:template>

    <xsl:template match="foaf:page" mode="icon">
        <img id="ResourceIcon" height="64" border="0" style="float: left; margin-right: 6px; vertical-align: middle;">
            <xsl:attribute name="src"><xsl:value-of select="@icon"/></xsl:attribute>
            <xsl:attribute name="alt"><xsl:value-of select="@mimeType"/></xsl:attribute>
            <xsl:attribute name="title"><xsl:value-of select="@mimeType"/></xsl:attribute>
        </img>
    </xsl:template>

    <xsl:template match="foaf:page" mode="resType">
        <span><xsl:value-of select="comete:getResourceType( @mimeType )"/></span><br/>
    </xsl:template>

    <xsl:template match="comete:learningResourceType">
        <span class="LearningResourceType"><xsl:value-of select="@label"/></span><br/>
    </xsl:template>

    <xsl:template match="foaf:page" mode="link">
        <xsl:variable name="url" select="if( starts-with( @rdf:resource, 'http' ) ) then @rdf:resource else concat( 'http://', @rdf:resource )"/>
        <a class="ResourceLink" target="_blank"><xsl:attribute name="href"><xsl:value-of select="$url"/></xsl:attribute><img height="38"><xsl:attribute name="src"><xsl:value-of select="concat('images/ViewResource_', $lang, '.png' )"/></xsl:attribute></img></a>
        <br clear="all"/>
    </xsl:template>

    <xsl:template match="comete:educationalLevel">
        <li class="EducationalLevel"><xsl:value-of select="@label"/></li>
    </xsl:template>

    <xsl:template match="comete:intellectualProperty">
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="contributes">
        <xsl:if test="count( //contribute[ substring-before( substring-after( rdf:RDF/rdf:Description/@rdf:about, '/resource/' ), '/' ) = 'person' or count( //role[ contains( ., 'publisher' ) ] ) = 0 ] ) &gt; 0">
            <xsl:for-each select="//contribute[ substring-before( substring-after( rdf:RDF/rdf:Description/@rdf:about, '/resource/' ), '/' ) = 'person' or count( //role[ contains( ., 'publisher' ) ] ) = 0 ]">
                <xsl:call-template name="render-contribute">
                    <xsl:with-param name="uri" select="rdf:RDF/rdf:Description/@rdf:about"/>
                    <xsl:with-param name="photo" select="if( rdf:RDF/rdf:Description/foaf:img/@rdf:resource ) then 
                                                             rdf:RDF/rdf:Description/foaf:img[1]/@rdf:resource
                                                         else if( rdf:RDF/rdf:Description/foaf:logo/@rdf:resource ) then
                                                             rdf:RDF/rdf:Description/foaf:logo[1]/@rdf:resource
                                                         else ''"/>
                    <xsl:with-param name="name" select="if( rdf:RDF/rdf:Description/foaf:name ) then
                                                            rdf:RDF/rdf:Description/foaf:name[1] 
                                                        else if( organization/rdf:RDF/rdf:Description/foaf:name ) then
                                                            organization/rdf:RDF/rdf:Description/foaf:name[1]
                                                        else ''"/>
                    <xsl:with-param name="roles" select=".//role"/>
                    <xsl:with-param name="email" select="if( starts-with(rdf:RDF/rdf:Description/foaf:mbox,'mailto:') ) then 
                                                             substring-after(rdf:RDF/rdf:Description/foaf:mbox,'mailto:') 
                                                         else 
                                                             rdf:RDF/rdf:Description/foaf:mbox"/>
                    <xsl:with-param name="identityLink" select="identity/@href"/>
                </xsl:call-template>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <xsl:template name="render-datestamp">
        <xsl:param name="datestampStr"/>
        <xsl:variable name="dateStr" select="if( contains( $datestampStr, 'T' ) ) then substring-before( $datestampStr, 'T' ) else $datestampStr"/>
        <xsl:variable name="dateElements" select="tokenize( $dateStr, '-' )"/>
        <xsl:variable name="year" select="if( count( $dateElements ) &gt; 0 ) then $dateElements[1] else ''"/>
        <xsl:variable name="month" select="if( count( $dateElements ) &gt; 1 ) then $dateElements[2] else ''"/>
        <xsl:variable name="day" select="if( count( $dateElements ) &gt; 2 ) then $dateElements[3] else ''"/>
        <xsl:choose>
            <xsl:when test="$year != '' and $month != '' and $day != ''">
                <xsl:variable name="date" select="xs:date( concat( $year, '-', $month, '-', $day ) )"/>
                <xsl:value-of select="format-date( $date, $FullDatePattern, $lang, (), () )"/>
            </xsl:when>
            <xsl:when test="$year != '' and $month != ''">
                <xsl:value-of select="concat( $PartialDatePrefix, $Months[ number($month) ], $PartialDateDelimiter, $year )"/>
            </xsl:when>
            <xsl:when test="$year != ''">
                <xsl:value-of select="concat( $PartialDatePrefix, $year )"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="render-contribute">
        <xsl:param name="uri"/>
        <xsl:param name="photo"/>
        <xsl:param name="name"/>
        <xsl:param name="roles"/>
        <xsl:param name="email"/>
        <xsl:param name="website"/>
        <xsl:param name="identityLink"/>
        <xsl:variable name="photoSrc" select="if( $photo = '' ) then 'images/noPhoto.png' else $photo"/>
        <li>
            <span class="ContribFullName">
                <xsl:call-template name="render-related-learning-objects-link">
                    <xsl:with-param name="uri" select="$uri"/>
                    <xsl:with-param name="label" select="$name"/>
                    <xsl:with-param name="class" select="'CompactRelatedLearningObjectsLink'"/>
                </xsl:call-template>
                <a class="CompactContributeLink">
                    <xsl:attribute name="href">javascript:showAdditionalIdentityInfo( '<xsl:value-of select="$identityLink"/>', 50, 50, 360, 360 );</xsl:attribute>
                    <img src="images/details.png" width="16" height="16" style="margin-left: 4px; vertical-align: text-bottom;">
                        <xsl:attribute name="alt"><xsl:value-of select="$ContributeLinkLabel"/></xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="$ContributeLinkLabel"/></xsl:attribute>
                    </img>
                </a>
                <xsl:call-template name="render-linked-data-link">
                    <xsl:with-param name="uri" select="$uri"/>
                    <xsl:with-param name="class" select="'CompactLinkedDataLink'"/>
                </xsl:call-template>
            </span>
            <xsl:if test="$roles">
                <xsl:variable name="strRoles">
                    <xsl:call-template name="prettyPrintRoles">
                        <xsl:with-param name="roles" select="$roles"/>
                    </xsl:call-template>
                </xsl:variable>
                <br/><span class="ContribRole"><xsl:value-of select="$strRoles"/></span>
            </xsl:if>
        </li>
    </xsl:template>

    <xsl:template name="prettyPrintRoles">
        <xsl:param name="roles"/>
        <xsl:variable name="strRoles" as="xs:string+">
            <xsl:for-each select="$roles">
                <xsl:call-template name="displayRole">
                    <xsl:with-param name="roleUri" select="."/>
                </xsl:call-template>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="sortedStrRoles" as="xs:string+">
            <xsl:for-each select="$strRoles">
                <xsl:sort select="." lang="$lang"/>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="csvRoles">
            <xsl:value-of select="$sortedStrRoles" separator=", "/>
        </xsl:variable>
        <xsl:call-template name="capitalize-string">
            <xsl:with-param name="str" select="$csvRoles"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="displayRole">
        <xsl:param name="roleUri"/>
        <xsl:value-of select="if( contains( $roleUri, 'creator' ) ) then $RoleAuthor 
            else if( contains( $roleUri, 'publisher' ) ) then $RolePublisher
            else ''"/>
    </xsl:template>

    <xsl:template name="render-related-learning-objects-link">
        <xsl:param name="uri"/>
        <xsl:param name="label"/>
        <xsl:param name="class" select="'RelatedLearningObjectsLink'"/>
        <xsl:variable name="functionName" select="if( contains( $uri, 'person' ) ) then 'setRequestContributor' else 'setRequestOrganization'"/>
        <xsl:variable name="title" select="if( contains( $uri, 'person' ) ) then $RelatedLearningObjectsToContribLinkLabel else $RelatedLearningObjectsToOrgLinkLabel"/>
        <a>
            <xsl:attribute name="class"><xsl:value-of select="$class"/></xsl:attribute>
            <xsl:attribute name="href">javascript:<xsl:value-of select="$functionName"/>( '<xsl:value-of select="$uri"/>', '<xsl:value-of select="replace( $label, '''', '\\''' )"/>' );</xsl:attribute>
            <xsl:value-of select="$label"/>
        </a>
    </xsl:template>

    <xsl:template name="render-linked-data-link">
        <xsl:param name="uri"/>
        <xsl:param name="class" select="'LinkedDataLink'"/>
        <xsl:variable name="type" select="substring-before( substring-after( $uri, '/resource/' ), '/' )"/>
        <xsl:variable name="title" select="if( $type = 'learningobject' ) then $LearningObjectLinkedDataLinkLabel
            else if( $type = 'person' ) then $PersonLinkedDataLinkLabel 
            else if( $type = 'organization' ) then $OrgLinkedDataLinkLabel
            else ''"/>
        <a target="_blank">
            <xsl:attribute name="class"><xsl:value-of select="$class"/></xsl:attribute>
            <xsl:attribute name="href"><xsl:value-of select="$uri"/></xsl:attribute>
            <img src="images/rdf.png" width="16" height="16" style="margin-left: 4px; vertical-align: text-bottom;">
                <xsl:attribute name="alt"><xsl:value-of select="$title"/></xsl:attribute>
                <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
            </img>
        </a>
    </xsl:template>

    <xsl:function name="comete:getResourceType">
        <xsl:param name="mimeType"/>
        <xsl:choose>
            <xsl:when test="contains( $mimeType, 'archive' ) or 
                            contains( $mimeType, 'zip' ) or 
                            ends-with( $mimeType, '-tar' ) or 
                            ends-with( $mimeType, '-gtar' ) or
                            contains( $mimeType, 'compressed' ) or
                            contains( $mimeType, 'zip' ) or
                            contains( $mimeType, 'stuffit' )">
                <xsl:value-of select="$ResourceTypeArchive"/>
            </xsl:when>
            <xsl:when test="contains( $mimeType, 'ms-word' ) or contains( $mimeType, 'msword' )">
                <xsl:value-of select="$ResourceTypeWord"/>
            </xsl:when>
            <xsl:when test="contains( $mimeType, 'html' )">
                <xsl:value-of select="$ResourceTypeHtml"/>
            </xsl:when>
            <xsl:when test="contains( $mimeType, 'powerpoint' )">
                <xsl:value-of select="$ResourceTypePowerPoint"/>
            </xsl:when>
            <xsl:when test="contains( $mimeType, 'pdf' )">
                <xsl:value-of select="$ResourceTypePDF"/>
            </xsl:when>
            <xsl:when test="contains( $mimeType, 'excel' )">
                <xsl:value-of select="$ResourceTypeExcel"/>
            </xsl:when>
            <xsl:when test="starts-with( $mimeType, 'text' )">
                <xsl:value-of select="$ResourceTypeText"/>
            </xsl:when>
            <xsl:when test="starts-with( $mimeType, 'audio' )">
                <xsl:value-of select="$ResourceTypeAudio"/>
            </xsl:when>
            <xsl:when test="starts-with( $mimeType, 'video' )">
                <xsl:value-of select="$ResourceTypeVideo"/>
            </xsl:when>
            <xsl:when test="starts-with( $mimeType, 'image' )">
                <xsl:value-of select="$ResourceTypeImage"/>
            </xsl:when>
            <xsl:when test="starts-with( $mimeType, 'application' )">
                <xsl:value-of select="$ResourceTypeApplication"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$ResourceTypeMisc"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
</xsl:stylesheet>

