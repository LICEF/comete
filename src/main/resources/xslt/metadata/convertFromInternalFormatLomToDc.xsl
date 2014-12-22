<xsl:stylesheet 
    version="2.0"
    xmlns:comete-if="http://comete.licef.ca/internal-format"
    xmlns:dc="http://purl.org/dc/elements/1.1/" 
    xmlns:lom="http://ltsc.ieee.org/xsd/LOM"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    xmlns:xml="http://www.w3.org/XML/1998/namespace"
    xmlns:xsi="http://www.w3.org/2001/XMLSchemainstance"    
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    exclude-result-prefixes="lom saxon">

    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="loURI"/>
    <xsl:param name="recordURI"/>

    <xsl:template match="lom:lom">
        <oai_dc:dc xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
            <xsl:apply-templates select = "lom:technical/lom:location"/>
            <xsl:apply-templates select = "lom:general/lom:title/lom:string"/>
            <xsl:apply-templates select = "lom:lifeCycle/lom:contribute"/>
            <xsl:apply-templates select = "lom:general/lom:keyword/lom:string"/>
            <xsl:apply-templates select = "lom:general/lom:description/lom:string"/>
            <xsl:apply-templates select = "lom:technical/lom:format"/>
            <xsl:apply-templates select = "lom:general/lom:language"/>
            <xsl:apply-templates select = "lom:general/lom:coverage/lom:string"/>
            <xsl:apply-templates select = "lom:rights/lom:description/lom:string"/>
        </oai_dc:dc>    
    </xsl:template>

    <!-- DC:IDENTIFIER -->
    <xsl:template match="lom:technical/lom:location">
        <dc:identifier>
            <xsl:value-of select="."/>      
        </dc:identifier>
    </xsl:template>       

    <!-- DC:TITLE -->
    <xsl:template match="lom:general/lom:title/lom:string">  
        <dc:title>
            <xsl:if test="normalize-space(@language)">
                <xsl:attribute name="xml:lang" namespace="http://www.w3.org/XML/1998/namespace">
                    <xsl:value-of select="normalize-space(@language)"/>
                </xsl:attribute>     
            </xsl:if>
            <xsl:value-of select="."/>
        </dc:title>
    </xsl:template>

    <xsl:template match="lom:lifeCycle/lom:contribute">
        <xsl:apply-templates select="lom:entity"/>
        <xsl:if test="lom:role/lom:value = 'author'">
            <xsl:apply-templates select="lom:date/lom:dateTime"/>
        </xsl:if>
    </xsl:template>

    <!-- DC:CREATOR -->
    <xsl:template match="lom:lifeCycle/lom:contribute[lom:role/lom:value = 'author']/lom:entity">
        <xsl:if test="comete-if:identity">
            <xsl:variable name="identityURI" select="normalize-space( comete-if:identity )"/>
            <xsl:variable name="fn" select="saxon:getFN( $identityURI, $loURI )"/>
            <dc:creator><xsl:value-of select="$fn"/></dc:creator>
        </xsl:if>
    </xsl:template>       
    
    <!-- DC:PUBLISHER-->
    <xsl:template match="lom:lifeCycle/lom:contribute[lom:role/lom:value = 'publisher']/lom:entity">
        <xsl:if test="comete-if:identity">
            <xsl:variable name="identityURI" select="normalize-space( comete-if:identity )"/>
            <xsl:variable name="fn" select="saxon:getFN( $identityURI, $loURI )"/>
            <dc:publisher><xsl:value-of select="$fn"/></dc:publisher>
        </xsl:if>
    </xsl:template>       

    <!-- DC:CONTRIBUTOR-->
    <xsl:template match="lom:lifeCycle/lom:contribute[lom:role/lom:value != 'author' and lom:role/lom:value != 'publisher']/lom:entity">
        <xsl:if test="comete-if:identity">
            <xsl:variable name="identityURI" select="normalize-space( comete-if:identity )"/>
            <xsl:variable name="fn" select="saxon:getFN( $identityURI, $loURI )"/>
            <dc:contributor><xsl:value-of select="$fn"/></dc:contributor>
        </xsl:if>
    </xsl:template>       

    <!-- DC:DATE -->
    <xsl:template match="lom:date/lom:dateTime">
        <dc:date><xsl:value-of select="."/></dc:date>
    </xsl:template>

    <!-- DC:SUBJECT -->
    <xsl:template match="lom:general/lom:keyword/lom:string">    
        <dc:subject>
            <xsl:if test="normalize-space(@language)">
                <xsl:attribute name="xml:lang" namespace="http://www.w3.org/XML/1998/namespace">
                    <xsl:value-of select="normalize-space(@language)"/>
                </xsl:attribute>     
            </xsl:if>
            <xsl:value-of select="."/>
        </dc:subject>
    </xsl:template>       

    <!-- DC:DESCRIPTION -->
    <xsl:template match="lom:general/lom:description/lom:string">    
        <dc:description>
            <xsl:if test="normalize-space(@language)">
                <xsl:attribute name="xml:lang" namespace="http://www.w3.org/XML/1998/namespace">
                    <xsl:value-of select="normalize-space(@language)"/>
                </xsl:attribute>     
            </xsl:if>
            <xsl:value-of select="."/>
        </dc:description>
    </xsl:template>       

    <!-- DC:FORMAT -->
    <xsl:template match="lom:technical/lom:format">
        <dc:format>
            <xsl:value-of select="."/>      
        </dc:format>
    </xsl:template>

    <!-- DC:LANGUAGE -->
    <xsl:template match="lom:general/lom:language">
        <dc:language>
            <xsl:value-of select="."/>      
        </dc:language>
    </xsl:template>   

    <!-- DC:COVERAGE -->
    <xsl:template match="lom:general/lom:coverage/lom:string">
        <dc:coverage>
            <xsl:if test="normalize-space(@language)">
                <xsl:attribute name="xml:lang" namespace="http://www.w3.org/XML/1998/namespace">
                    <xsl:value-of select="normalize-space(@language)"/>
                </xsl:attribute>     
            </xsl:if>
            <xsl:value-of select="."/>  
        </dc:coverage>
    </xsl:template>

    <!-- DC:RIGHTS -->
    <xsl:template match="lom:rights/lom:description/lom:string">
        <dc:rights>
            <xsl:if test="normalize-space(@language)">
                <xsl:attribute name="xml:lang" namespace="http://www.w3.org/XML/1998/namespace">
                    <xsl:value-of select="normalize-space(@language)"/>
                </xsl:attribute>     
            </xsl:if>
            <xsl:value-of select="."/>  
        </dc:rights>
    </xsl:template>   

</xsl:stylesheet>
