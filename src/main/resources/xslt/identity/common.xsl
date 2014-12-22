<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:comete="http://comete.licef.ca/reference#"
    xmlns:foaf="http://xmlns.com/foaf/0.1/">

    <xsl:template match="comete:formattedAddress">
        <p><xsl:value-of select="translate(replace( ., ';', '&lt;br/&gt;' ), '&lt;&gt;&amp;', '&#xE001;&#xE002;&#xE003;')"/></p><br />
    </xsl:template>

    <xsl:template match="foaf:mbox">
        <xsl:variable name="email" select="if( starts-with(.,'mailto:') ) then substring-after(.,'mailto:') else ."/>
        <xsl:variable name="href" select="if( starts-with(.,'mailto:') ) then . else concat('mailto:',.)"/>
        <span class="Email"><a><xsl:attribute name="href"><xsl:value-of select="$href"/></xsl:attribute><xsl:value-of select="$email"/></a></span><br />
    </xsl:template>

    <xsl:template match="foaf:homepage">
        <span class="Url">
            <a class="Link" target="_blank">
                <xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute>
                <xsl:value-of select="."/>
            </a>
        </span><br />
    </xsl:template>

</xsl:stylesheet>
