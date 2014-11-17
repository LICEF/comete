<!--

This XSLT produces a list of all the triples that need to be created after processing a LOM metadata record.
The output is used to build and add the triples to the metamodel.

The interpreted elements (title, description, etc.) are explicitly handled. For these elements though, only
the first value is considered at the moment.  For example, if a record has 3 titles, only the first one is
interpreted, the other titles are ignored.  Should we consider them as strings to be indexed?

Other terminal elements containing strings are handled as default string values that will be indexed for
full-text searches.  Only elements (or parent elements of string or value elements) listed in the 
elementsForStrings variable are considered.

-->
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:lom="http://ltsc.ieee.org/xsd/LOM"     
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    extension-element-prefixes="saxon"
    exclude-result-prefixes="lom">

    <xsl:output method="xml" indent="yes"/>

    <xsl:include href="utilities.xsl"/>

    <xsl:param name="loURI"/>
    <xsl:param name="recordURI"/>

    <xsl:variable name="elementsForStrings" select="('catalog','keyword','coverage','version','description','installationRemarks','otherPlatformRequirements','typicalAgeRange')"/>

    <xsl:template name="handleStringElements">
        <xsl:choose>
            <xsl:when test="count(*)=0">
                <xsl:if test="(local-name() = $elementsForStrings or (local-name() = ('string','value') and ../local-name() = $elementsForStrings))">
                    <xsl:variable name="lang" select="if( @language != '' ) then @language else ''"/>
                    <xsl:variable name="triple" select="saxon:buildExtraInfoTriple( $loURI, ., $lang )"/>
                    <xsl:if test="$triple != ''">
                        <xsl:call-template name="generateTripleElements">
                            <xsl:with-param name="triplesAsString" select="$triple"/>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="/">
        <triples>
            <xsl:apply-templates/>
        </triples>
    </xsl:template>

    <xsl:template match="lom:string">
        <xsl:choose>
            <xsl:when test="local-name(..)='title'">
                <xsl:variable name="elementName" select="local-name()"/>
                <xsl:variable name="lang" select="if( @language != '' ) then @language else ''"/>
                <xsl:variable name="triple" select="saxon:buildTitleTriple( $loURI, ., $lang )"/>
                <xsl:call-template name="generateTripleElements">
                    <xsl:with-param name="triplesAsString" select="$triple"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="local-name(parent::*)='description' and local-name(../..)='general'">
                <xsl:variable name="lang" select="if( @language != '' ) then @language else ''"/>
                <xsl:variable name="triple" select="saxon:buildDescriptionTriple( $loURI, ., $lang )"/>
                <xsl:call-template name="generateTripleElements">
                    <xsl:with-param name="triplesAsString" select="$triple"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="local-name(parent::*)='keyword' and local-name(../..)='general'">
                <xsl:variable name="lang" select="if( @language != '' ) then @language else ''"/>
                <xsl:variable name="triple" select="saxon:buildKeywordTriple( $loURI, ., $lang )"/>
                <xsl:call-template name="generateTripleElements">
                    <xsl:with-param name="triplesAsString" select="$triple"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="local-name(parent::*)='description' and local-name(../..)='rights'">
                <xsl:variable name="lang" select="if( @language != '' ) then @language else ''"/>
                <xsl:variable name="triple" select="saxon:buildIntellectualPropertyTriple( $loURI, ., $lang )"/>
                <xsl:call-template name="generateTripleElements">
                    <xsl:with-param name="triplesAsString" select="$triple"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="handleStringElements"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="lom:location[1]">
        <xsl:variable name="elementName" select="local-name()"/>
        <xsl:variable name="triple" select="saxon:buildLocationTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:format[1]">
        <xsl:variable name="elementName" select="local-name()"/>
        <xsl:variable name="triple" select="saxon:buildFormatTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:lom/lom:lifeCycle/lom:contribute[lom:role/lom:value='author'][1]/lom:date/lom:dateTime">
        <xsl:variable name="triple" select="saxon:buildCreationDateTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:lom/lom:general/lom:language">
        <xsl:variable name="triple" select="saxon:buildLanguageTriple( $loURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:lom/lom:metaMetadata/lom:language">
        <xsl:variable name="triple" select="saxon:buildLanguageTriple( $recordURI, . )"/>
        <xsl:call-template name="generateTripleElements">
            <xsl:with-param name="triplesAsString" select="$triple"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="element()">
        <xsl:call-template name="handleStringElements"/>
    </xsl:template>

</xsl:stylesheet>

