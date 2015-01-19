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
    <xsl:param name="cometeUrl"/>

    <xsl:variable name="contribColCount" select="4" as="xs:integer"/>
    
    <xsl:variable name="lang" select="'en'"/>
    <xsl:variable name="title" select="'Resource'"/>
    <xsl:variable name="HeaderLanguages" select="'Languages'"/>
    <xsl:variable name="HeaderKeywords" select="'Keywords'"/>
    <xsl:variable name="HeaderTechnical" select="'Access to the resource'"/>
    <xsl:variable name="HeaderContributes" select="'Contributions'"/>
    <xsl:variable name="HeaderSubjects" select="'Associated Categories'"/>
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

    <xsl:template match="/">
        <xsl:apply-templates select="rdf:RDF/rdf:Description[@rdf:about=$uri]" mode="learningObject"/>
    </xsl:template>

    <xsl:template match="rdf:Description" mode="learningObject">
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                <title><xsl:value-of select="$title" /></title>
                <link href="../../../ext-5.0.1/build/packages/ext-theme-crisp/build/resources/ext-theme-crisp-all.css" rel="stylesheet" type="text/css"/>
                <link href="../../../default.css" rel="stylesheet" type="text/css"/>
                <link href="../../../custom.css" rel="stylesheet" type="text/css"/>
                <script type="text/javascript" src="../../../ext-5.0.1/build/ext-all.js"></script>
                <script type="text/javascript" src="../../../utils.js"></script>
                <script type="text/javascript" src="../../../learningObjectToHtml.js"></script>
            </head>
            <body class="LearningObject">
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
                <p class="LearningObjectTitle"><xsl:value-of select="$title"/> 
                <xsl:call-template name="render-linked-data-link">
                    <xsl:with-param name="uri" select="$uri"/>
                </xsl:call-template>
                </p>
                <hr/>
                <xsl:apply-templates select="dct:description"/>
                <xsl:if test="dct:language">
                    <h2 class="SectionHeader"><xsl:value-of select="$HeaderLanguages"/></h2>
                    <ul>
                        <xsl:apply-templates select="dct:language">
                            <xsl:sort select="funct:getLanguageString(.,$lang)" lang="$lang"/>
                        </xsl:apply-templates>
                    </ul>
                </xsl:if>
                <xsl:if test="comete:keyword">
                    <h2 class="SectionHeader"><xsl:value-of select="$HeaderKeywords"/></h2>
                    <ul>
                        <xsl:apply-templates select="comete:keyword">
                            <xsl:sort select="." lang="$lang"/>
                        </xsl:apply-templates>
                    </ul>
                </xsl:if>
                <xsl:if test="foaf:page">
                    <h2 class="SectionHeader"><xsl:value-of select="$HeaderTechnical"/></h2>

                    <xsl:apply-templates select="foaf:page"/> 
                </xsl:if>
                <xsl:if test="count(../contributes//contribute) &gt; 0">
                    <h2 class="SectionHeader"><xsl:value-of select="$HeaderContributes"/></h2>

                    <table class="Contributes">
                    <xsl:apply-templates select="../contributes"/>

                    <xsl:for-each select="//dct:publisher[ substring-before( substring-after( @rdf:resource, '/resource/' ), '/' ) = 'organization' ]">
                        <xsl:variable name="publisherUri" select="@rdf:resource"/>
                        <xsl:variable name="publisher" select="if( ../../contributes/contribute[ rdf:RDF/rdf:Description/@rdf:about = $publisherUri ] ) then ../../contributes/contribute[ rdf:RDF/rdf:Description/@rdf:about = $publisherUri ] else (../../contributes/contribute/organization[ rdf:RDF/rdf:Description/@rdf:about = $publisherUri ])[ 1 ]"/>
                        <xsl:if test="( position() - 1 ) mod $contribColCount = 0">
                            <xsl:text disable-output-escaping="yes">&lt;tr&gt;</xsl:text>
                        </xsl:if>
                        <td class="ContributesRow">
                            <xsl:call-template name="render-contribute">
                                <xsl:with-param name="uri" select="$publisherUri"/>
                                <xsl:with-param name="photo" select="if( $publisher/rdf:RDF/rdf:Description/foaf:img ) then 
                                                                         $publisher/rdf:RDF/rdf:Description/foaf:img[1] 
                                                                     else if( $publisher/rdf:RDF/rdf:Description/foaf:logo ) then
                                                                         $publisher/rdf:RDF/rdf:Description/foaf:logo[1]
                                                                     else ''"/>
                                <xsl:with-param name="name" select="$publisher/rdf:RDF/rdf:Description/foaf:name[1]"/> 
                                <xsl:with-param name="website" select="$publisher/rdf:RDF/rdf:Description/foaf:homepage"/>
                                <xsl:with-param name="identityLink" select="$publisher/identity/@href"/>
                            </xsl:call-template>
                        </td>
                        <xsl:if test="position() mod $contribColCount = 0 or position() = last()">
                            <xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                    </table>
                </xsl:if>
                <xsl:if test="count(dct:subject[@navigable = 'true'][@vocabLabel]) &gt; 0">
                    <h2 class="SectionHeader"><xsl:value-of select="$HeaderSubjects"/></h2>
                    <ul>
                    <xsl:for-each-group select="dct:subject[@navigable = 'true'][@vocabLabel]" group-by="@vocabLabel">
                        <xsl:sort select="current-grouping-key()" lang="$lang"/>
                            <ul>
                            <xsl:for-each select="current-group()">
                                <xsl:sort select="@conceptLabel" lang="$lang"/>
                                <li><font style="font-weight: bold; color: #04408C"><xsl:value-of select="@vocabLabel"/></font>
                                    <img style="margin-bottom:-1px; margin-right:6px; margin-left:8px" src="../../../images/blueArrow.gif"/>
                                    <xsl:value-of select="@conceptLabel"/>
                                    <a class="RelatedLearningObjectsLink"><xsl:attribute name="href">javascript:setRequestVocConcept( '<xsl:value-of select="@rdf:resource"/>' );</xsl:attribute><img src="../../../images/relatedResources.png" width="16" height="16"><xsl:attribute name="alt"><xsl:value-of select="$RelatedLearningObjectsToSubjectLinkLabel"/></xsl:attribute><xsl:attribute name="title"><xsl:value-of select="$RelatedLearningObjectsToSubjectLinkLabel"/></xsl:attribute></img></a></li>
                            </xsl:for-each>
                            </ul>
                    </xsl:for-each-group>
                    </ul>
                </xsl:if>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="dct:title">
        <p class="LearningObjectTitle"><xsl:value-of select="."/></p>
        <hr/>
    </xsl:template>

    <xsl:template match="dct:description">
        <!-- The translate function combined with the html-tags character-set defined at the top are needed
             to render the html tags included in the description in such a way that 
             they can be interpreted (and therefore rendered) by the browser. -->
        <div class="LearningObjectDescription"><xsl:value-of select="translate(., '&lt;&gt;&amp;', '&#xE001;&#xE002;&#xE003;' )"/></div>
    </xsl:template>

    <xsl:template match="dct:language">
        <li><xsl:value-of select="funct:getLanguageString(.,$lang)"/></li>
    </xsl:template>

    <xsl:template match="comete:keyword">
        <!--xsl:variable name="keyword">
            <xsl:call-template name="escape-single-quotes">
                <xsl:with-param name="string" select="."/>
            </xsl:call-template>
        </xsl:variable-->
        <xsl:variable name="keyword" select="."/>
        <li><xsl:value-of select="."/>
            <a class="RelatedLearningObjectsLink">
                <xsl:attribute name="href">javascript:setRequestKeyword( '<xsl:value-of select="$keyword"/>', '<xsl:value-of select="@xml:lang"/>' );</xsl:attribute>
                <img src="../../../images/relatedResources.png" width="16" height="16">
                    <xsl:attribute name="alt"><xsl:value-of select="$RelatedLearningObjectsToKeywordLinkLabel"/></xsl:attribute>
                    <xsl:attribute name="title"><xsl:value-of select="$RelatedLearningObjectsToKeywordLinkLabel"/></xsl:attribute>
                </img>
            </a>
        </li>
    </xsl:template>

    <xsl:template match="foaf:page">
        <xsl:variable name="url" select="if( starts-with( ., 'http' ) ) then . else concat( 'http://', . )"/>
        <a class="ResourceLink" target="_blank">
            <xsl:attribute name="href"><xsl:value-of select="$url"/></xsl:attribute>
            <img class="ResourceIcon" height="64" border="0">
                <xsl:attribute name="src"><xsl:value-of select="concat( $cometeUrl, '/', @icon )"/></xsl:attribute>
                <xsl:attribute name="alt"><xsl:value-of select="@mimeType"/></xsl:attribute>
                <xsl:attribute name="title"><xsl:value-of select="@mimeType"/></xsl:attribute>
            </img>
        </a>
    </xsl:template>

    <xsl:template match="contributes">
        <xsl:if test="count( //contribute[ substring-before( substring-after( rdf:RDF/rdf:Description/@rdf:about, '/resource/' ), '/' ) = 'person' or count( //role[ contains( ., 'publisher' ) ] ) = 0 ] ) &gt; 0">
            <xsl:for-each select="//contribute[ substring-before( substring-after( rdf:RDF/rdf:Description/@rdf:about, '/resource/' ), '/' ) = 'person' or count( //role[ contains( ., 'publisher' ) ] ) = 0 ]">
                <xsl:if test="( position() - 1 ) mod $contribColCount = 0">
                    <xsl:text disable-output-escaping="yes">&lt;tr&gt;</xsl:text>
                </xsl:if>
                <td class="ContributesRow">
                    <xsl:call-template name="render-contribute">
                        <xsl:with-param name="uri" select="rdf:RDF/rdf:Description/@rdf:about"/>
                        <xsl:with-param name="photo" select="if( rdf:RDF/rdf:Description/foaf:img ) then 
                                                                 rdf:RDF/rdf:Description/foaf:img[1] 
                                                             else if( rdf:RDF/rdf:Description/foaf:logo ) then
                                                                 rdf:RDF/rdf:Description/foaf:logo[1]
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
                </td>
                <xsl:if test="position() mod $contribColCount = 0 or position() = last()">
                    <xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <xsl:template name="render-contribute">
        <xsl:param name="uri"/>
        <xsl:param name="photo"/>
        <xsl:param name="name"/>
        <xsl:param name="roles"/>
        <xsl:param name="email"/>
        <xsl:param name="website"/>
        <xsl:param name="identityLink"/>
        <xsl:variable name="photoSrc" select="if( $photo = '' ) then concat( $cometeUrl, '/images/noPhoto.png' ) else $photo"/>
        <table>
            <tr>
                <td class="ContributeCell">
                    <table>
                        <tr>
                            <td rowspan="3">
                                <xsl:call-template name="render-photo">
                                    <xsl:with-param name="photo" select="$photoSrc"/>
                                    <xsl:with-param name="photoCssClass" select="'ContributeImage'"/>
                                </xsl:call-template>
                            </td>
                            <td>
                                <a class="CompactContributeLink">
                                    <xsl:attribute name="href">javascript:showAdditionalIdentityInfo( '<xsl:value-of select="$identityLink"/>', 100, 100, 660, 400 );</xsl:attribute>
                                    <img src="../../../images/details.png" width="16" height="16">
                                        <xsl:attribute name="alt"><xsl:value-of select="$ContributeLinkLabel"/></xsl:attribute>
                                        <xsl:attribute name="title"><xsl:value-of select="$ContributeLinkLabel"/></xsl:attribute>
                                    </img>
                                </a>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <xsl:call-template name="render-related-learning-objects-link">
                                    <xsl:with-param name="uri" select="$uri"/>
                                    <xsl:with-param name="label" select="$name"/>
                                    <xsl:with-param name="class" select="'CompactRelatedLearningObjectsLink'"/>
                                </xsl:call-template>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <xsl:call-template name="render-linked-data-link">
                                    <xsl:with-param name="uri" select="$uri"/>
                                    <xsl:with-param name="class" select="'CompactLinkedDataLink'"/>
                                </xsl:call-template>
                            </td>
                        </tr>
                    </table>
                    <span class="ContribFullName"><xsl:value-of select="$name"/></span><br/>
                    <xsl:if test="$email != ''">
                        <span class="ContribEmail"><a target="_blank"><xsl:attribute name="href"><xsl:value-of select="concat( 'mailto:', $email )"/></xsl:attribute><xsl:value-of select="$email"/></a></span><br/>
                    </xsl:if>
                    <xsl:if test="$website != ''">
                        <span class="ContribWebsite"><a target="_blank"><xsl:attribute name="href"><xsl:value-of select="$website"/></xsl:attribute><xsl:value-of select="$website"/></a></span><br/>
                    </xsl:if>
                    <xsl:if test="count($roles) &gt; 0">
                        <xsl:variable name="strRoles">
                            <xsl:call-template name="prettyPrintRoles">
                                <xsl:with-param name="roles" select="$roles"/>
                            </xsl:call-template>
                        </xsl:variable>
                        <span class="ContribRole"><xsl:value-of select="$strRoles"/></span><br />
                    </xsl:if>
                </td>
            </tr>
        </table>
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
            <xsl:attribute name="href">javascript:<xsl:value-of select="$functionName"/>( '<xsl:value-of select="$uri"/>', '<xsl:value-of select="$label"/>' );</xsl:attribute>
            <img src="../../../images/relatedResources.png" width="16" height="16">
                <xsl:attribute name="alt"><xsl:value-of select="$title"/></xsl:attribute>
                <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
            </img>
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
            <img src="../../../images/rdf.png" width="16" height="16">
                <xsl:attribute name="alt"><xsl:value-of select="$title"/></xsl:attribute>
                <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
            </img>
        </a>
    </xsl:template>

</xsl:stylesheet>
