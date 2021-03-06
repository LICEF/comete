<xsl:stylesheet version="2.0"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:comete="http://comete.licef.ca/reference#"
    xmlns:foaf="http://xmlns.com/foaf/0.1/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    exclude-result-prefixes="rdf comete foaf xsi xsl">

    <xsl:include href="/xslt/charMaps.xsl"/>
    <xsl:include href="/xslt/util.xsl"/>
    <xsl:include href="/xslt/sharedTemplates.xsl"/>

    <xsl:output method="xhtml" encoding="utf-8" indent="yes" use-character-maps="html-tags c1-control-range" omit-xml-declaration="yes"/>

    <xsl:param name="uri"/>

    <xsl:variable name="title" select="'Person'"/>
    <xsl:variable name="telLabel" select="'Tel.: '"/>
    <xsl:variable name="faxLabel" select="'Fax: '"/>
    <xsl:variable name="assocOrgsLabel" select="'Associated organizations (by alphabetic order)'"/>

    <xsl:template match="/">
        <xsl:apply-templates select="rdf:RDF/rdf:Description[@rdf:about=$uri]" mode="person"/>
    </xsl:template>

    <xsl:template match="rdf:Description" mode="person">
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                <title><xsl:value-of select="$title" /></title>
                <link href="../../../ext-5.0.1/build/packages/ext-theme-crisp/build/resources/ext-theme-crisp-all.css" rel="stylesheet" type="text/css"/>
                <link href="../../../layout-browser.css" rel="stylesheet" type="text/css"/>
                <link href="../../../default.css" rel="stylesheet" type="text/css"/>
                <link href="../../../custom.css" rel="stylesheet" type="text/css"/>
            </head>
            <body class="MetadataPerson">
                <xsl:variable name="personPhoto" select="if( foaf:img/@rdf:resource ) then foaf:img[1]/@rdf:resource else if( foaf:logo/@rdf:resource ) then foaf:logo[1]/@rdf:resource else ''"/>
                <table class="MetadataPerson">
                    <col class="MetadataPersonCol"/>
                    <xsl:if test="$personPhoto != ''">
                        <tr>
                            <td>
                                <xsl:call-template name="render-photo">
                                    <xsl:with-param name="photo" select="$personPhoto"/>
                                    <xsl:with-param name="photoCssClass" select="'ContributeImageDetailedView'"/>
                                    <xsl:with-param name="maxWidth" select="218"/>
                                    <xsl:with-param name="maxHeight" select="100"/>
                                </xsl:call-template>
                            </td>
                        </tr>
                    </xsl:if>
                    <tr>
                        <td>
                            <xsl:apply-templates select="foaf:name"/>
                            <br/>

                            <xsl:if test="comete:formattedAddress">
                                <xsl:apply-templates select="comete:formattedAddress"/>
                            </xsl:if>

                            <xsl:variable name="phone" select="foaf:phone[starts-with(@rdf:resource,'tel:')]/@rdf:resource"/>
                            <xsl:variable name="fax" select="foaf:phone[starts-with(@rdf:resource,'fax:')]/@rdf:resource"/>
                            <xsl:if test="$phone != ''">
                                <xsl:call-template name="render-phone">
                                    <xsl:with-param name="phone" select="$phone"/>
                                    <xsl:with-param name="phoneLabel" select="$telLabel"/>
                                </xsl:call-template>
                            </xsl:if>
                            <xsl:if test="$fax != ''">
                                <xsl:call-template name="render-fax">
                                    <xsl:with-param name="fax" select="$fax"/>
                                    <xsl:with-param name="faxLabel" select="$faxLabel"/>
                                </xsl:call-template>
                            </xsl:if>
                            <xsl:apply-templates select="foaf:mbox"/>
                        </td>
                    </tr>
                </table>
                <br/><br/>
                <xsl:if test="count(../organization) &gt; 0">
                    <xsl:if test="count(../organization) &gt; 1">
                        <div class="assocOrgsHeader"><xsl:value-of select="$assocOrgsLabel"/></div>
                    </xsl:if>
                    <table class="MetadataOrganization secondaryData">
                        <xsl:apply-templates select="../organization">
                            <xsl:sort select="rdf:RDF/rdf:Description/foaf:name" lang="$lang"/>
                        </xsl:apply-templates>
                    </table>
                </xsl:if>
                <br/><br/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="organization/rdf:RDF/rdf:Description">
        <xsl:variable name="orgPhoto" select="if( foaf:logo/@rdf:resource ) then foaf:logo[1]/@rdf:resource else if( foaf:img/@rdf:resource ) then foaf:img[1]/@rdf:resource else ''"/>
        <xsl:if test="$orgPhoto != ''">
            <tr>
                <td>
                    <xsl:call-template name="render-photo">
                        <xsl:with-param name="photo" select="$orgPhoto"/>
                        <xsl:with-param name="photoCssClass" select="'ContributeImageDetailedView'"/>
                        <xsl:with-param name="maxWidth" select="218"/>
                        <xsl:with-param name="maxHeight" select="100"/>
                    </xsl:call-template>
                </td>
            </tr>
        </xsl:if>
        <tr>
            <td>
                <xsl:apply-templates select="foaf:name" mode="organization"/>
                <br/>
                <xsl:if test="comete:formattedAddress">
                    <xsl:apply-templates select="comete:formattedAddress"/>
                </xsl:if>

                <xsl:variable name="phone" select="foaf:phone[starts-with(@rdf:resource,'tel:')]/@rdf:resource"/>
                <xsl:variable name="fax" select="foaf:phone[starts-with(@rdf:resource,'fax:')]/@rdf:resource"/>
                <xsl:if test="$phone != ''">
                    <xsl:call-template name="render-phone">
                        <xsl:with-param name="phone" select="$phone"/>
                        <xsl:with-param name="phoneLabel" select="$telLabel"/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:if test="$fax != ''">
                    <xsl:call-template name="render-fax">
                        <xsl:with-param name="fax" select="$fax"/>
                        <xsl:with-param name="faxLabel" select="$faxLabel"/>
                    </xsl:call-template>
                </xsl:if>

                <xsl:apply-templates select="foaf:mbox"/>
                <xsl:apply-templates select="foaf:homepage"/>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="foaf:name">
        <h2 class="ContribFullName"><xsl:value-of select="."/></h2>
    </xsl:template>

    <xsl:template match="foaf:name" mode="organization">
        <h2 class="OrganizationName"><xsl:value-of select="."/></h2>
    </xsl:template>

</xsl:stylesheet>


