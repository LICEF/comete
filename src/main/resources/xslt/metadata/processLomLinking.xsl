<xsl:stylesheet version="2.0"
    xmlns:lom="http://ltsc.ieee.org/xsd/LOM"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:saxon="java:ca.licef.comete.metadata.util.XSLTUtil"
    extension-element-prefixes="saxon">

    <xsl:output method="xml" indent="yes" cdata-section-elements="lom:entity"/>

    <xsl:param name="loURI"/>
    <xsl:param name="recordURI"/>

    <!-- Identity -->

    <xsl:template name="manageIdentity">
        <xsl:param name="loURI"/>
        <xsl:param name="recordURI"/>
        <xsl:param name="entityType"/>
        <xsl:variable name="emptyString" select="saxon:linkToIdentity($loURI,$recordURI,$entityType,.,'http://ltsc.ieee.org/xsd/LOM')"/>
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
        <xsl:value-of select="$emptyString"/>
    </xsl:template>

    <xsl:template match="lom:entity">
        <xsl:variable name="entityType">
            <xsl:choose>
                <xsl:when test="../../local-name() = 'metaMetadata'">
                    <xsl:value-of select="'metadata-author'"/>
                </xsl:when>
                <xsl:when test="../../local-name() = 'lifeCycle'">
                    <xsl:choose>
                        <xsl:when test="..[lom:role/lom:value = 'editor' or lom:role/lom:value = 'publisher']">
                            <xsl:value-of select="'publisher'"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="'author'"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="manageIdentity">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="recordURI" select="$recordURI"/>
            <xsl:with-param name="entityType" select="$entityType"/>
        </xsl:call-template>
    </xsl:template>


    <!-- Vocabulary -->

    <xsl:template name="manageVocabulary">
        <xsl:param name="loURI"/>
        <xsl:param name="source"/>
        <xsl:param name="element"/>
        <xsl:param name="value"/>
        <xsl:variable name="emptyString" select="saxon:linkToVocabularyConcept( $loURI, $source, $element, $value )"/>
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
        <xsl:value-of select="$emptyString"/>
    </xsl:template>

    <xsl:template match="lom:structure">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'1.7_structure'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:aggregationLevel">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'1.8_aggregation_level'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:status">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'2.2_status'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:type">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'4.4.1.1_type'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:interactivityType">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.1_interactivity_type'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:learningResourceType">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.2_learning_resource_type'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:interactivityLevel">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.3_interactivity_level'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:semanticDensity">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.4_semantic_density'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:intendedEndUserRole">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.5_intended_end_user_role'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:context">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.6_context'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:difficulty">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'5.8_difficulty'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:cost">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'6.1_cost'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:copyrightAndOtherRestrictions">
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source"/>
            <xsl:with-param name="element" select="'6.2_copyright_and_other_restrictions'"/>
            <xsl:with-param name="value" select="lom:value[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="lom:purpose"/>

    <xsl:template match="lom:taxonPath">
        <xsl:copy-of select="lom:source"/>
        <xsl:call-template name="manageVocabulary">
            <xsl:with-param name="loURI" select="$loURI"/>
            <xsl:with-param name="source" select="lom:source/lom:string[1]"/>
            <xsl:with-param name="element" select="'9.2_taxon_path'"/>
            <xsl:with-param name="value" select="lom:taxon[last()]/lom:id"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
