<!--

This XSLT produces a list of all the triples that need to be created after processing a DC metadata record.
The output is used to build and add the triples to the metamodel.

The interpreted elements (title, description, etc.) are explicitly handled. For these elements though, only
the first value is considered at the moment.  For example, if a record has 3 titles, only the first one is
interpreted, the other titles are ignored.  Should we consider them as strings to be indexed?

A few empty templates are specified for some elements because they must be processed differently and are
thus ignored by this XSLT.

Other terminal elements containing strings are handled as default string values that will be indexed for
full-text searches.

-->
<xsl:stylesheet version="2.0"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
    xmlns:dc="http://purl.org/dc/elements/1.1/"     
    xmlns:xml="http://www.w3.org/XML/1998/namespace"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    extension-element-prefixes="saxon"
    exclude-result-prefixes="dc oai_dc">

    <xsl:output method="xml" indent="yes"/>

    <xsl:include href="util.xsl"/>

    <xsl:param name="loURI"/>
    <xsl:param name="recordURI"/>

    <xsl:template match="/">
        <triples>
            <xsl:apply-templates/>
        </triples>
    </xsl:template>

    <xsl:template match="dc:title">
        <xsl:variable name="elementName" select="local-name()"/>
        <xsl:variable name="lang" select="if( @xml:lang != '' ) then @xml:lang else ''"/>
        <xsl:variable name="triple" select="saxon:buildTitleTriple( $loURI, ., $lang )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:description">
        <xsl:variable name="elementName" select="local-name()"/>
        <xsl:variable name="lang" select="if( @xml:lang != '' ) then @xml:lang else ''"/>
        <xsl:variable name="triple" select="saxon:buildDescriptionTriple( $loURI, ., $lang )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:language">
        <xsl:variable name="triple" select="saxon:buildLanguageTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:identifier[1]">
        <xsl:variable name="elementName" select="local-name()"/>
        <xsl:variable name="triple" select="saxon:buildLocationTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:format[1]">
        <xsl:variable name="elementName" select="local-name()"/>
        <xsl:variable name="triple" select="saxon:buildFormatTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:contributor">
    </xsl:template>

    <xsl:template match="dc:creator">
    </xsl:template>

    <xsl:template match="dc:date[1]">
        <xsl:variable name="triple" select="saxon:buildCreationDateTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:subject">
        <xsl:variable name="lang" select="if( @xml:lang != '' ) then @xml:lang else ''"/>
        <xsl:variable name="triple" select="saxon:buildKeywordTriple( $loURI, ., $lang )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:publisher">
    </xsl:template>

    <xsl:template match="element()">
        <xsl:choose>
            <xsl:when test="count(*)=0">
                <xsl:variable name="lang" select="if( @xml:lang != '' ) then @xml:lang else ''"/>
                <xsl:variable name="triple" select="saxon:buildExtraInfoTriple( $loURI, ., $lang )"/>
                <xsl:if test="$triple != ''">
                    <xsl:call-template name="generateTripleElements">
                        <xsl:with-param name="triplesAsString" select="$triple"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
