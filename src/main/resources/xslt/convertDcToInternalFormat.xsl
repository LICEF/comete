<xsl:stylesheet version="2.0"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    extension-element-prefixes="saxon">

    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="loURI"/>
    <xsl:param name="recordURI"/>

    <xsl:template match="/*">
        <xsl:copy>
            <xsl:namespace name="comete-if" select="'http://comete.licef.ca/internal-format'"/>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="dc:contributor">
        <xsl:if test=". != ''">
            <xsl:variable name="identityIdentifier" select="saxon:getURIMarkerForEntity($loURI,$recordURI,'author',.,'http://www.openarchives.org/OAI/2.0/oai_dc/')"/>
            <xsl:choose>
                <xsl:when test="$identityIdentifier">
                    <xsl:element name="{name()}" namespace="{namespace-uri()}">
                        <xsl:element name="comete-if:identity" namespace="http://comete.licef.ca/internal-format">
                            <xsl:value-of select="$identityIdentifier"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy>
                        <xsl:apply-templates select="@*|node()"/>
                    </xsl:copy>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dc:creator">
        <xsl:if test=". != ''">
            <xsl:variable name="identityIdentifier" select="saxon:getURIMarkerForEntity($loURI,$recordURI,'author',.,'http://www.openarchives.org/OAI/2.0/oai_dc/')"/>
            <xsl:if test="$identityIdentifier">
                <xsl:element name="{name()}" namespace="{namespace-uri()}">
                    <xsl:element name="comete-if:identity" namespace="http://comete.licef.ca/internal-format">
                        <xsl:value-of select="$identityIdentifier"/>
                    </xsl:element>
                </xsl:element>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dc:publisher">
        <xsl:if test=". != ''">
            <xsl:variable name="identityIdentifier" select="saxon:getURIMarkerForEntity($loURI,$recordURI,'publisher',.,'http://www.openarchives.org/OAI/2.0/oai_dc/')"/>
            <xsl:if test="$identityIdentifier">
                <xsl:element name="{name()}" namespace="{namespace-uri()}">
                    <xsl:element name="comete-if:identity" namespace="http://comete.licef.ca/internal-format">
                        <xsl:value-of select="$identityIdentifier"/>
                    </xsl:element>
                </xsl:element>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>


