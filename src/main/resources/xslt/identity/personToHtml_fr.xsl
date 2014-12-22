<xsl:stylesheet version="2.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:import href="/personToHtml.xsl"/>

    <xsl:variable name="title" select="'Personne'"/>
    <xsl:variable name="telLabel" select="'Tél.: '"/>
    <xsl:variable name="faxLabel" select="'Fax: '"/>
    <xsl:variable name="assocOrgsLabel" select="'Organisations associées (par ordre alphabétique)'"/>

</xsl:stylesheet>
