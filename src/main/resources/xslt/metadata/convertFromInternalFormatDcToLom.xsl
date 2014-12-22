<?xml version="1.0"?>
<xsl:stylesheet 
    version="2.0"
    xmlns:comete-if="http://comete.licef.ca/internal-format"
    xmlns:dc="http://purl.org/dc/elements/1.1/" 
    xmlns:lom="http://ltsc.ieee.org/xsd/LOM"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    exclude-result-prefixes="dc oai_dc saxon">

    <xsl:output method="xml" indent="yes" cdata-section-elements="lom:entity"/>

    <xsl:param name="loURI"/>
    <xsl:param name="recordURI"/>

    <xsl:template match="oai_dc:dc">
        <lom:lom xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
            xsi:schemaLocation="http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd">
            <lom:general>
                <lom:identifier>
                    <lom:catalog>URI</lom:catalog>
                    <lom:entry><xsl:value-of select="$loURI"/></lom:entry>
                </lom:identifier>
                <xsl:apply-templates select="dc:title[1]"/>
                <xsl:apply-templates select="dc:language"/>
                <xsl:apply-templates select="dc:description"/>
                <xsl:apply-templates select="dc:subject"/>
                <xsl:apply-templates select="dc:coverage"/>
            </lom:general>
            <xsl:if test="dc:contributor or dc:creator or dc:publisher">
                <lom:lifeCycle>
                    <xsl:apply-templates select="dc:contributor"/>
                    <xsl:apply-templates select="dc:creator"/>
                    <xsl:apply-templates select="dc:publisher"/>
                </lom:lifeCycle>
            </xsl:if>
            <lom:metaMetadata>
                <lom:identifier>
                    <lom:catalog>URI</lom:catalog>
                    <lom:entry><xsl:value-of select="$recordURI"/></lom:entry>
                </lom:identifier>
            </lom:metaMetadata>
            <xsl:if test="dc:identifier or dc:format">
                <lom:technical>
                    <xsl:apply-templates select="dc:format"/>
                    <xsl:apply-templates select="dc:identifier"/>
                </lom:technical>
            </xsl:if>
            <xsl:if test="dc:rights">
                <lom:rights>
                    <xsl:apply-templates select="dc:rights[1]"/>
                </lom:rights>
            </xsl:if>
        </lom:lom>
    </xsl:template>

    <xsl:template match="dc:identifier">
        <lom:location><xsl:value-of select="."/></lom:location>
    </xsl:template>

    <xsl:template match="dc:format">
        <lom:format><xsl:value-of select="."/></lom:format>
    </xsl:template>

    <xsl:template match="dc:language">
        <lom:language><xsl:value-of select="."/></lom:language>
    </xsl:template>

    <xsl:template match="dc:title">
        <lom:title>
            <xsl:element name="lom:string" namespace="http://ltsc.ieee.org/xsd/LOM">
                <xsl:if test="@xml:lang">
                    <xsl:attribute name="language"><xsl:value-of select="@xml:lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </xsl:element>
        </lom:title>
    </xsl:template>

    <xsl:template match="dc:description">
        <lom:description>
            <xsl:element name="lom:string" namespace="http://ltsc.ieee.org/xsd/LOM">
                <xsl:if test="@xml:lang">
                    <xsl:attribute name="language"><xsl:value-of select="@xml:lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </xsl:element>
        </lom:description>
    </xsl:template>

    <xsl:template match="dc:coverage">
        <lom:coverage>
            <xsl:element name="lom:string" namespace="http://ltsc.ieee.org/xsd/LOM">
                <xsl:if test="@xml:lang">
                    <xsl:attribute name="language"><xsl:value-of select="@xml:lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </xsl:element>
        </lom:coverage>
    </xsl:template>

    <xsl:template match="dc:subject">
        <lom:keyword>
            <xsl:element name="lom:string" namespace="http://ltsc.ieee.org/xsd/LOM">
                <xsl:if test="@xml:lang">
                    <xsl:attribute name="language"><xsl:value-of select="@xml:lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </xsl:element>
        </lom:keyword>
    </xsl:template>

    <xsl:template match="dc:contributor | dc:creator | dc:publisher">
        <xsl:variable name="identityURI" select="comete-if:identity"/>
        <xsl:variable name="vcard" select="saxon:getVCard($identityURI, $loURI)"/>
        <lom:contribute>
            <xsl:choose>
                <xsl:when test="local-name() = 'creator'">
                    <lom:role><lom:source>LOMv1.0</lom:source><lom:value>author</lom:value></lom:role>
                </xsl:when>
                <xsl:when test="local-name() = 'publisher'">
                    <lom:role><lom:source>LOMv1.0</lom:source><lom:value>publisher</lom:value></lom:role>
                </xsl:when>
            </xsl:choose>
            <lom:entity><xsl:value-of select="$vcard"/></lom:entity>
        </lom:contribute>
    </xsl:template>

    <xsl:template match="dc:rights">
        <lom:description>
            <xsl:element name="lom:string" namespace="http://ltsc.ieee.org/xsd/LOM">
                <xsl:if test="@xml:lang">
                    <xsl:attribute name="language"><xsl:value-of select="@xml:lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </xsl:element>
        </lom:description>
    </xsl:template>

</xsl:stylesheet>
