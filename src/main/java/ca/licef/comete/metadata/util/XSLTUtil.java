package ca.licef.comete.metadata.util;

import java.util.ArrayList;
import ca.licef.comete.core.Core;
import ca.licef.comete.core.metadataformat.MetadataFormat;
import ca.licef.comete.core.metadataformat.MetadataFormats;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.identity.Identity;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.Vocabulary;
import licef.IOUtil;
import licef.LangUtil;
import licef.StringUtil;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;

public class XSLTUtil {

    public static String buildTitleTriple( String loURI, String title, String language ) throws Exception {
        String lang = ( "".equals( language ) ? null : language );
        Triple tripleTitle = new Triple( loURI, DCTERMS.title.getURI(), title, true, lang );
        return( getTripleAsString( tripleTitle ) );
    }

    public static String buildDescriptionTriple( String loURI, String desc, String language ) throws Exception {
        String lang = ( "".equals( language ) ? null : language );
        Triple tripleDesc = new Triple( loURI, DCTERMS.description.getURI(), desc, true, lang );
        return( getTripleAsString( tripleDesc ) );
    }

    public static String buildKeywordTriple( String loURI, String keyword, String language ) throws Exception {
        // Ignore keywords that look like URI because they are most likely vocabulary terms.
        if( IOUtil.isURL( keyword ) )
            return( null );

        String lang = ( "".equals( language ) ? null : language );
        Triple tripleKeyword = new Triple( loURI, COMETE.keyword.getURI(), keyword, true, lang );
        return( getTripleAsString( tripleKeyword ) );
    }

    public static String buildIntellectualPropertyTriple( String loURI, String desc, String language ) throws Exception {
        String lang = ( "".equals( language ) ? null : language );
        Triple tripleDesc = new Triple( loURI, COMETE.intellectualProperty.getURI(), desc, true, lang );
        return( getTripleAsString( tripleDesc ) );
    }

    public static String buildLocationTriple( String loURI, String location ) throws Exception {
        location = location.trim().replaceAll(" ", "%20");
        Triple tripleLocation = new Triple( loURI, FOAF.page, location );
        return( getTripleAsString( tripleLocation ) );
    }

    public static String buildFormatTriple( String loURI, String format ) throws Exception {
        format = format.toLowerCase();
        String cat = StringUtil.split(format, '/')[0];
        if (Constants.MIMETYPE_CATEGORIES.contains(cat)) {
            format = "http://purl.org/NET/mediatypes/" + format;
            Triple tripleFormat = new Triple( loURI, DCTERMS.format.getURI(), format, false );
            return( getTripleAsString( tripleFormat ) );
        }
        return null;
    }

    public static String buildLanguageTriple( String loURI, String lang ) throws Exception {
        Triple tripleLang = new Triple( loURI, DCTERMS.language.getURI(), LangUtil.convertLangToISO2( lang ), true );
        return( getTripleAsString( tripleLang ) );
    }

    public static String buildCreationDateTriple( String loURI, String creationDate ) throws Exception {
        Triple tripleCreationDate = new Triple( loURI, DCTERMS.created, creationDate );
        return( getTripleAsString( tripleCreationDate ) );
    }

    public static String buildExtraInfoTriple( String loURI, String str, String language ) throws Exception {
        if( str == null || "".equals( str.trim() ) )
            return( "" );
        String lang = ( "".equals( language ) ? null : language );
        Triple tripleExtraInfo = new Triple( loURI, COMETE.extraInfo.getURI(), str, true, lang );
        return( getTripleAsString( tripleExtraInfo ) );
    }

    public static String getTriplesAsString(Triple[] triples) throws Exception {
        StringBuilder str = new StringBuilder();
        String tripleDelimiter = "";
        for (Triple triple : triples) {
            str.append(tripleDelimiter);
            str.append(convertTripleToString(triple));
            tripleDelimiter = "@@@";
        }
        return (str.toString());
    }

    public static String getTripleAsString(Triple triple) throws Exception {
        StringBuilder str = new StringBuilder();
        str.append(convertTripleToString(triple));
        return (str.toString());
    }

    private static String convertTripleToString(Triple triple) {
        StringBuilder str = new StringBuilder();
        str.append(triple.getSubject()).append("###");
        str.append(triple.getPredicate()).append("###");
        str.append(triple.getObject()).append("###");
        str.append(triple.isObjectLiteral());
        if (triple.getLanguage() != null)
            str.append("###").append(triple.getLanguage());
        return (str.toString());
    }

    public static String linkToIdentity( String loURI, String recordURI, String entityType, String entityData, String metadataFormatNamespace ) {
        try {
            Invoker inv = new Invoker(null, "ca.licef.comete.metadata.util.XSLTUtil",
                    "linkToIdentityEff", new Object[]{loURI, recordURI, entityType, entityData, metadataFormatNamespace});
            return (String)Core.getInstance().getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
        }
        catch( Exception e ) {
            return( null );
        }
    }

    public static String linkToIdentityEff( String loURI, String recordURI, String entityType, String entityData, String metadataFormatNamespace ) throws Exception {
        String linkSrc = null;
        String effectiveEntityType = null;
        if( "metadata-author".equals( entityType ) ) {
            effectiveEntityType = DCTERMS.creator.getURI();
            linkSrc = "metadataRecord";
        }
        if( "publisher".equals( entityType ) ) {
            effectiveEntityType = DCTERMS.publisher.getURI();
            linkSrc = "learningObject";
        }
        else if( "author".equals( entityType ) ) {
            effectiveEntityType = DCTERMS.creator.getURI();
            linkSrc = "learningObject";
        }

        // Other entity elements are not interpreted by the metamodel for now.
        if( effectiveEntityType == null )
            return( null );

        MetadataFormat mf = MetadataFormats.getMetadataFormat(metadataFormatNamespace);
        String[] identityUris = Identity.getInstance().digest(mf.getIdentityMimetypeFormat(), entityData);
        if (identityUris == null)
            return( null );

        String personUri = identityUris[0];
        String orgUri = identityUris[1];
        ArrayList<Triple> triples = new ArrayList<Triple>();
        if( personUri != null ) {
            if( "metadataRecord".equals( linkSrc ) )
                triples.add( new Triple( recordURI, effectiveEntityType, personUri, false ) );
            else if( "learningObject".equals( linkSrc ) )
                triples.add( new Triple( loURI, effectiveEntityType, personUri, false ) );
        }
        if( orgUri != null && "learningObject".equals( linkSrc ) )
            triples.add( new Triple( loURI, DCTERMS.publisher.getURI(), orgUri, false ) );

        Core.getInstance().getTripleStore().insertTriples(triples);
        return "";
    }

    public static String linkToVocabularyConcept( String loURI, String source, String element, String value ) {
        try {
            Invoker inv = new Invoker(null, "ca.licef.comete.metadata.util.XSLTUtil",
                    "linkToVocabularyConceptEff", new Object[]{loURI, source, element, value});
            return (String)Core.getInstance().getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
        }
        catch( Exception e ) {
            return( null );
        }
    }

    public static String linkToVocabularyConceptEff( String loURI, String source, String element, String value ) throws Exception {
        //filter for used vocabularies
        if ( !"5.2".equals(element) &&
             !"5.6".equals(element) &&
             !"9.2".equals(element) )
            return null;

        if ("9.2".equals(element))
            element = null;
        else {
            if (source.startsWith("LOM"))
                value = StringUtil.toCamelCase(value);
        }
        if( source == null)
            return null;
        source = source.trim();
        if ("".equals(source))
            return( null );
        if (value == null || "".equals(value))
            return null;

        String id = (element == null || source.startsWith("http"))?source:source + "-" + element;
        String conceptUri = Vocabulary.getInstance().getConcept(id, value);

        if( conceptUri != null ) {
            String vocUri = Vocabulary.getInstance().getVocabularyUri(id);
            String predicate = Vocabulary.getInstance().getConceptLinkingPredicateFromUri(vocUri);
            Triple triple = new Triple(loURI, predicate, conceptUri, false);
            Core.getInstance().getTripleStore().insertTriple(triple);
        }
        else
            System.out.println("-> no concept URI found for " + source + ", " + value);
        return "";
    }

    public static String getFN( String vcard ) throws Exception {
        return( Identity.getInstance().getVCardFormattedName( vcard ) );
    }

    public static String getVCard( String dcIdentityStr ) {
        if( dcIdentityStr == null || "".equals( dcIdentityStr.trim() ) )
            return( null );

        // If the identity is already a vcard, return it as is.
        if( dcIdentityStr.trim().startsWith( "BEGIN:VCARD" ) )
            return( dcIdentityStr.trim() );

        // Otherwise, create a simple vcard with only the FN field.
        StringBuilder str = new StringBuilder();
        str.append( "BEGIN:VCARD\n" );
        str.append( "VERSION:3.0\n" );
        str.append( "FN:" + dcIdentityStr );
        str.append( "END:VCARD" );
        return( str.toString() );
    }

}
