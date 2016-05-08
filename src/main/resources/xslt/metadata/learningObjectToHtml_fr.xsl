<xsl:stylesheet version="2.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:import href="/xslt/metadata/learningObjectToHtml.xsl"/>

    <xsl:variable name="lang" select="'fr'"/>
    <xsl:variable name="title" select="'Ressource'"/>
    <xsl:variable name="HeaderAboutResource" select="'À propos de la ressource'"/>
    <xsl:variable name="HeaderLanguages" select="'Langues'"/>
    <xsl:variable name="HeaderKeywords" select="'Mots clés'"/>
    <xsl:variable name="HeaderContributes" select="'Contributeurs'"/>
    <xsl:variable name="HeaderSubjects" select="'Catégories associées'"/>
    <xsl:variable name="HeaderEducationLevel" select="'Niveau scolaire'"/>
    <xsl:variable name="HeaderIntellectualProperty" select="'Propriété intellectuelle'"/>
    <xsl:variable name="ContributeLinkLabel" select="'Voir toute l''info.'"/>
    <xsl:variable name="RelatedLearningObjectsToContribLinkLabel" select="'Voir les ressources concernant ce contributeur.'"/>
    <xsl:variable name="RelatedLearningObjectsToOrgLinkLabel" select="'Voir les ressources concernant cette organisation.'"/>
    <xsl:variable name="RelatedLearningObjectsToSubjectLinkLabel" select="'Voir les ressources concernant cette catégorie.'"/>
    <xsl:variable name="RelatedLearningObjectsToKeywordLinkLabel" select="'Voir les ressources associées à ce mot clé.'"/>
    <xsl:variable name="LearningObjectLinkedDataLinkLabel" select="'Voir les données liées de la ressource.'"/>
    <xsl:variable name="PersonLinkedDataLinkLabel" select="'Voir les données liées de la personne.'"/>
    <xsl:variable name="OrgLinkedDataLinkLabel" select="'Voir les données liées de l''organisation.'"/>
    <xsl:variable name="RoleAuthor" select="'auteur'"/>
    <xsl:variable name="RolePublisher" select="'éditeur'"/>

    <xsl:variable name="ResourceTypeText" select="'Ressource texte'"/>
    <xsl:variable name="ResourceTypeAudio" select="'Ressource audio'"/>
    <xsl:variable name="ResourceTypeVideo" select="'Ressource vidéo'"/>
    <xsl:variable name="ResourceTypeImage" select="'Ressource graphique'"/>
    <xsl:variable name="ResourceTypeApplication" select="'Ressource exécutable'"/>
    <xsl:variable name="ResourceTypeMisc" select="'Ressource de type inconnu'"/>
    <xsl:variable name="ResourceTypeArchive" select="'Ressource archive'"/>
    <xsl:variable name="ResourceTypeWord" select="'Document Word'"/>
    <xsl:variable name="ResourceTypeHtml" select="'Ressource Web'"/>
    <xsl:variable name="ResourceTypePowerPoint" select="'Document PowerPoint'"/>
    <xsl:variable name="ResourceTypePDF" select="'Document PDF'"/>
    <xsl:variable name="ResourceTypeExcel" select="'Document Excel'"/>

    <xsl:variable name="ShareResource" select="'Partagez cette fiche'"/>
    <xsl:variable name="ShareOnFacebook" select="'Partager la fiche sur Facebook'"/>
    <xsl:variable name="ShareOnTwitter" select="'Partager la fiche sur Twitter'"/>
    <xsl:variable name="ShareOnLinkedin" select="'Partager la fiche sur LinkedIn'"/>
    <xsl:variable name="ShareByEmail" select="'Partager la fiche par courriel'"/>

    <xsl:variable name="AddedDateLabel" select="'Ajoutée'"/>
    <xsl:variable name="CreatedDateLabel" select="'Créée'"/>
    <xsl:variable name="UpdatedDateLabel" select="'Modifiée'"/>
    <xsl:variable name="PartialDatePrefix" select="' en '"/>
    <xsl:variable name="PartialDateDelimiter" select="' '"/>
    <xsl:variable name="FullDatePattern" select="' le [D] [Mn] [Y]'"/>
    <xsl:variable name="Months" select="('janvier','février','mars','avril','mai','juin','juillet','août','septembre','octobre','novembre','décembre')"/>

    <xsl:variable name="ViewThisCardIn" select="'Visualiser cette fiche dans'"/>

</xsl:stylesheet>
