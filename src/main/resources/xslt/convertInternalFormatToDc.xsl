<xsl:stylesheet version="2.0"
    xmlns:comete-if="http://comete.licef.ca/internal-format"
    xmlns="http://purl.org/dc/elements/1.1/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:lom="http://ltsc.ieee.org/xsd/LOM"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    extension-element-prefixes="saxon">

    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="loURI"/>

    <xsl:template name="substituteIdentity">
        <xsl:param name="identityURI"/>
        <xsl:variable name="fn" select="saxon:getFN($identityURI, $loURI)"/>
        <xsl:choose>
            <xsl:when test="$fn">
                <xsl:element name="{name()}" namespace="{namespace-uri()}">
                    <xsl:value-of select="$fn"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dc:creator">
        <xsl:call-template name="substituteIdentity">
            <xsl:with-param name="identityURI" select="comete-if:identity"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:publisher">
        <xsl:call-template name="substituteIdentity">
            <xsl:with-param name="identityURI" select="comete-if:identity"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dc:contributor">
        <xsl:call-template name="substituteIdentity">
            <xsl:with-param name="identityURI" select="comete-if:identity"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:copy>
            <xsl:apply-templates select="."/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="{name()}" namespace="{namespace-uri()}">
            <xsl:copy-of select="namespace::*[not(. = 'http://comete.licef.ca/internal-format')]" />
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>

