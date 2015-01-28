<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:comete="http://comete.licef.ca/reference#"
    xmlns:foaf="http://xmlns.com/foaf/0.1/">

    <xsl:template match="comete:formattedAddress">
        <p><xsl:value-of select="translate(replace( ., ';', '&lt;br/&gt;' ), '&lt;&gt;&amp;', '&#xE001;&#xE002;&#xE003;')"/></p><br />
    </xsl:template>

    <xsl:template match="foaf:mbox">
        <xsl:variable name="email" select="if( starts-with(@rdf:resource,'mailto:') ) then substring-after(@rdf:resource,'mailto:') else @rdf:resource"/>
        <xsl:variable name="href" select="if( starts-with(@rdf:resource,'mailto:') ) then @rdf:resource else concat('mailto:',@rdf:resource)"/>
        <span class="Email"><a><xsl:attribute name="href"><xsl:value-of select="$href"/></xsl:attribute><xsl:value-of select="$email"/></a></span><br />
    </xsl:template>

    <xsl:template match="foaf:homepage">
        <span class="Url">
            <a class="Link" target="_blank">
                <xsl:attribute name="href"><xsl:value-of select="@rdf:resource"/></xsl:attribute>
                <xsl:value-of select="@rdf:resource"/>
            </a>
        </span><br />
    </xsl:template>

</xsl:stylesheet>

