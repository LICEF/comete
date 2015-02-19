<xsl:stylesheet version="2.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:import href="/xslt/metadata/learningObjectToHtml.xsl"/>

    <xsl:variable name="lang" select="'fr'"/>
    <xsl:variable name="title" select="'Ressource'"/>
    <xsl:variable name="HeaderLanguages" select="'Langues'"/>
    <xsl:variable name="HeaderKeywords" select="'Mots-clés'"/>
    <xsl:variable name="HeaderTechnical" select="'Accès à la ressource'"/>
    <xsl:variable name="HeaderContributes" select="'Contributions'"/>
    <xsl:variable name="HeaderSubjects" select="'Catégories associées'"/>
    <xsl:variable name="ContributeLinkLabel" select="'Voir toute l''info.'"/>
    <xsl:variable name="RelatedLearningObjectsToContribLinkLabel" select="'Voir les ressources concernant ce contributeur.'"/>
    <xsl:variable name="RelatedLearningObjectsToOrgLinkLabel" select="'Voir les ressources concernant cette organisation.'"/>
    <xsl:variable name="RelatedLearningObjectsToSubjectLinkLabel" select="'Voir les ressources concernant cette catégorie.'"/>
    <xsl:variable name="RelatedLearningObjectsToKeywordLinkLabel" select="'Voir les ressources associées à ce mot-clef.'"/>
    <xsl:variable name="LearningObjectLinkedDataLinkLabel" select="'Voir les données liées de la ressource.'"/>
    <xsl:variable name="PersonLinkedDataLinkLabel" select="'Voir les données liées de la personne.'"/>
    <xsl:variable name="OrgLinkedDataLinkLabel" select="'Voir les données liées de l''organisation.'"/>
    <xsl:variable name="RoleAuthor" select="'auteur'"/>
    <xsl:variable name="RolePublisher" select="'éditeur'"/>

</xsl:stylesheet>





