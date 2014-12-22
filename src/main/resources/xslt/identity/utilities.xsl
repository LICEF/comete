<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns="http://www.w3.org/1999/xhtml">

    <xsl:template name="render-phone">
        <xsl:param name="phone"/>
        <xsl:param name="phoneLabel"/>
            <p><span class="TelLabel"><xsl:value-of select="$phoneLabel" /></span>
            <span class="TelValue"><xsl:value-of select="substring-after( $phone, 'tel:' )"/></span></p>
    </xsl:template>

    <xsl:template name="render-fax">
        <xsl:param name="fax"/>
        <xsl:param name="faxLabel"/>
            <p><span class="TelLabel"><xsl:value-of select="$faxLabel" /></span>
            <span class="TelValue"><xsl:value-of select="substring-after( $fax, 'fax:' )"/></span></p>
    </xsl:template>

</xsl:stylesheet>

