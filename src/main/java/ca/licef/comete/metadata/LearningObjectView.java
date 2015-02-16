package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.DefaultView;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.vocabulary.Vocabulary;
import licef.CommonNamespaceContext;
import licef.IOUtil;
import licef.reflection.Invoker;
import licef.tsapi.model.Tuple;
import licef.tsapi.TripleStore;
import licef.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 4-Jun-2012
 */
public class LearningObjectView extends DefaultView {

    public String getHtml(String uri, Locale locale, String style) throws Exception {
        Set contributes = new HashSet();
        HashMap contribRoles = new HashMap();
        HashMap contribOrganizations = new HashMap();
        HashMap orgNames = new HashMap();
        getIdentityData( uri, contributes, contribRoles, contribOrganizations, orgNames );

        String loXml = getRdf(uri, "false", false, false);
        InputSource inputSource = new InputSource( new StringReader(loXml) );
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware( true );
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse( inputSource );
        Element rootElement = doc.getDocumentElement();

        // We only consider the first format for now.  A better solution would be to have one format property 
        // associated to each location. - FB
        String firstFormat = null;

        NodeList locationElements = doc.getElementsByTagNameNS( CommonNamespaceContext.foafNSURI, "page" );
        for( int i = 0; i < locationElements.getLength(); i++ ) {
            Element locationElement = (Element)locationElements.item( i );
            try {
                if( firstFormat == null ) {
                    NodeList formatElements = doc.getElementsByTagNameNS( CommonNamespaceContext.dctNSURI, "format" );
                    for( int j = 0; j < formatElements.getLength(); j++ ) {
                        Element formatElement = (Element)formatElements.item( j );
                        firstFormat = formatElement.getAttributeNS( CommonNamespaceContext.rdfNSURI, "resource" );
                        break;
                    }
                }

                if( firstFormat == null || "".equals( firstFormat ) ) {
                    NodeList formatElements = doc.getElementsByTagNameNS( CommonNamespaceContext.dctNSURI, "format" );
                    for( int j = 0; j < formatElements.getLength(); j++ ) {
                        Element formatElement = (Element)formatElements.item( j );
                        firstFormat = formatElement.getTextContent();
                        break;
                    }
                }

                String mimeType = null;
                if( firstFormat == null || "".equals( firstFormat ) )
                    mimeType = IOUtil.getMimeType( locationElement.getTextContent() );
                else
                    mimeType = firstFormat.substring( "http://purl.org/NET/mediatypes/".length() );

                locationElement.setAttributeNS( CommonNamespaceContext.cometeNSURI, "icon", Util.getMimeTypeIcon( mimeType ) );
                locationElement.setAttributeNS( CommonNamespaceContext.cometeNSURI, "mimeType", mimeType );
            }
            catch( IOException ioe ) {
                // For some reasons, the mime type cannot be determined so we just omit it.
            }
        }

        if( contributes.size() > 0 ) {
            ArrayList sortedContributes = new ArrayList( contributes );
            Collections.sort( sortedContributes, new ContributeComparator() );

            Set alreadyShownOrg = new HashSet();

            Element wrapperElement = doc.createElementNS( "", "contributes" );
            for( Iterator it = sortedContributes.iterator(); it.hasNext(); ) {
                String identityUri = (String)it.next();
                String identityType = ca.licef.comete.core.util.Util.getURIType( identityUri );

                // Omit organization that have already been shown with a person.
                if( Constants.OBJ_TYPE_ORGANIZATION.equals( identityType ) && alreadyShownOrg.contains( identityUri ) )
                    continue;

                String identityId = ca.licef.comete.core.util.Util.getIdValue(identityUri);
                String identityXml = getRdf( identityUri, "false", false, false );

                InputSource identitySrc = new InputSource( new StringReader( identityXml ) );
                DocumentBuilder docBuilderIdentity = docFactory.newDocumentBuilder();
                Document docIdentity = docBuilder.parse( identitySrc );

                // Required to make a copy of the node to insert into the main document.
                Node elementCopy = doc.importNode( docIdentity.getDocumentElement(), true );

                Element contributeElement = doc.createElementNS( "", "contribute" );
                wrapperElement.appendChild( contributeElement );
                contributeElement.appendChild( elementCopy );

                Set roles = (Set)contribRoles.get( identityUri );
                for( Iterator itRoles = roles.iterator(); itRoles.hasNext(); ) {
                    String role = (String)itRoles.next();
                    Element roleElement = doc.createElementNS( "", "role" );
                    roleElement.setTextContent( role );
                    contributeElement.appendChild( roleElement );
                }

                // Insert an identity element giving a location where further information can be obtained.
                String identityLocation = ca.licef.comete.core.util.Util.getRestUrl( identityType ) + "/" + identityId + "/html?lang=" + locale.getLanguage();
                Element identityElement = doc.createElementNS( "", "identity" );
                identityElement.setAttributeNS( "", "href", identityLocation );
                contributeElement.appendChild( identityElement );

                Set orgs = (Set)contribOrganizations.get( identityUri );
                if( orgs != null && orgs.size() > 0 ) {
                    for( Iterator itOrgs = orgs.iterator(); itOrgs.hasNext(); ) {
                        String orgUri = (String)itOrgs.next();
                        alreadyShownOrg.add( orgUri ); 
                        String orgId = ca.licef.comete.core.util.Util.getIdValue(orgUri);
                        String orgXml = getRdf( orgUri, "false", false, false );

                        InputSource orgSrc = new InputSource( new StringReader( orgXml ) );
                        DocumentBuilder docBuilderOrg = docFactory.newDocumentBuilder();
                        Document docOrg = docBuilder.parse( orgSrc );

                        // Required to make a copy of the node to insert into the main document.
                        Node elementCopyOrg = doc.importNode( docOrg.getDocumentElement(), true );

                        Element orgElement = doc.createElementNS( "", "organization" );
                        contributeElement.appendChild( orgElement );
                        orgElement.appendChild( elementCopyOrg );
                       
                        // Insert an identity element giving a location where further information can be obtained.
                        String orgIdentityType = ca.licef.comete.core.util.Util.getURIType( orgUri );
                        String orgIdentityLocation = ca.licef.comete.core.util.Util.getRestUrl( orgIdentityType ) + "/" + orgId + "/html?lang=" + locale.getLanguage();
                        Element orgIdentityElement = doc.createElementNS( "", "identity" );
                        orgIdentityElement.setAttributeNS( "", "href", orgIdentityLocation );
                        orgElement.appendChild( orgIdentityElement );

                        // We consider only the first org.
                        break;
                    }
                }
            }
            rootElement.appendChild( wrapperElement );
        }

        NodeList subjectElements = doc.getElementsByTagNameNS( CommonNamespaceContext.dctNSURI, "subject" );
        if( subjectElements.getLength() > 0 ) {
            HashMap<String,String> vocabs = getVocabularies( locale.getLanguage() );        
            for( int i = 0; i < subjectElements.getLength(); i++ ) {
                Element subjectElement = (Element)subjectElements.item( i );
                String subjectUri = subjectElement.getAttributeNS( CommonNamespaceContext.rdfNSURI, "resource" );
                String assocVoc = getAssocVocab( subjectUri, vocabs );
                if( assocVoc != null || subjectUri.startsWith( "http://cegepadistance.ca/ns/typesrea" ) ) {
                    subjectElement.setAttributeNS( "", "vocabLabel", vocabs.get( assocVoc ) );
                    if( subjectUri.contains( "licef/DDC" ) ) {
                        int lastSlashPos = subjectUri.lastIndexOf( "/" );
                        String left = subjectUri.substring( 0, lastSlashPos + 1 );
                        String right = subjectUri.substring( lastSlashPos + 1 );
                        while( right.length() < 3 ) 
                            right += "0";
                        subjectUri = left + right;
                    }
                    String conceptLabel = getVocabularyConceptLabel( subjectUri, locale.getLanguage() );
                    if( conceptLabel != null ) 
                        subjectElement.setAttributeNS( "", "conceptLabel", conceptLabel );
                    subjectElement.setAttributeNS( "", "navigable", ( assocVoc != null ) + "" );
                }
            }
        }

        NodeList learningResourceTypeElements = doc.getElementsByTagNameNS( CommonNamespaceContext.cometeNSURI, "learningResourceType" );
        for( int i = 0; i < learningResourceTypeElements.getLength(); i++ ) {
            Element learningResourceTypeElement = (Element)learningResourceTypeElements.item( i );
            String learningResourceTypeUri = learningResourceTypeElement.getAttributeNS( CommonNamespaceContext.rdfNSURI, "resource" );
            String learningResourceTypeLabel = getVocabularyConceptLabel( learningResourceTypeUri, locale.getLanguage() );
            learningResourceTypeElement.setAttributeNS( "", "label", learningResourceTypeLabel );
        }

        NodeList educationalLevelElements = doc.getElementsByTagNameNS( CommonNamespaceContext.cometeNSURI, "educationalLevel" );
        for( int i = 0; i < educationalLevelElements.getLength(); i++ ) {
            Element educationalLevelElement = (Element)educationalLevelElements.item( i );
            String educationalLevelUri = educationalLevelElement.getAttributeNS( CommonNamespaceContext.rdfNSURI, "resource" );
            String educationalLevelLabel = getVocabularyConceptLabel( educationalLevelUri, locale.getLanguage() );
            educationalLevelElement.setAttributeNS( "", "label", educationalLevelLabel );
        }

        String expandedXml = XMLUtil.serialize( doc, true );
//System.out.println( "expandedXml="+expandedXml );

        String styleSheet = "metadata/learningObjectToHtml";
        if( "ceres".equals( style ) )
            styleSheet = "metadata/learningObjectToHtmlForCeres";
        else if( "refrer".equals( style ) )
            styleSheet = "metadata/learningObjectToHtmlForRefrer";
        else if( "endrea".equals( style ) )
            styleSheet = "metadata/learningObjectToHtmlForEndrea";

        Properties props = new Properties();
        props.setProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

        HashMap<String, String> params = new HashMap<String, String>();
        params.put( "uri", uri );

        StreamSource source = new StreamSource( new StringReader( expandedXml ) );
        String str = ca.licef.comete.core.util.Util.applyXslToDocument( styleSheet, source, props, params, locale );
        return( str );
    }

    private void getIdentityData( String loUri, Set identities, HashMap identityRoles, HashMap identityOrganizations, HashMap orgNames ) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        String query = Util.getQuery( "metadata/getLearningObjectIdentityData.sparql", loUri );
        Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new String[] { query } );
        Tuple[] tuples = (Tuple[])tripleStore.transactionalCall( inv );
        for( Tuple tuple : tuples ) {
            String identityUri = tuple.getValue( "identity" ).getContent();
            String predicate = tuple.getValue( "p" ).getContent();
            String orgUri = tuple.getValue( "org" ).getContent();
            String orgName = tuple.getValue( "orgName" ).getContent();

            identities.add( identityUri );

            Set roles = (Set)identityRoles.get( identityUri );
            if( roles == null ) {
                roles = new HashSet();
                identityRoles.put( identityUri, roles );
            }
            roles.add( predicate );

            if( orgUri != null && !"".equals( orgUri ) ) {
                if( orgName != null && !"".equals( orgName ) )
                    orgNames.put( orgUri, orgName );
                Set organizations = (Set)identityOrganizations.get( identityUri );
                if( organizations == null ) {
                    organizations = new HashSet();
                    identityOrganizations.put( identityUri, organizations );
                }
                organizations.add( orgUri );
            }
        }
    }

    private HashMap<String,String> getVocabularies( String lang ) throws Exception {
        HashMap<String,String> vocTable = new HashMap<String,String>();

        Vocabulary vocabManager = Vocabulary.getInstance();
        String[] vocabUris = vocabManager.getNavigableVocabularies();
        for( String uri : vocabUris )
            vocTable.put( uri, vocabManager.getLabel( uri, lang ) );

        return( vocTable );
    }

    private String getAssocVocab( String subjectUri, HashMap<String,String> vocabs ) {
        for( String vocabUri : vocabs.keySet() ) {
            if( subjectUri.startsWith( vocabUri ) )
                return( vocabUri );
        }
        return( null );
    }

    private String getVocabularyConceptLabel( String subjectUri, String lang ) throws Exception {
        Invoker inv = new Invoker( null, "ca.licef.comete.core.util.Util", "getResourceLabel", new Object[] { subjectUri, lang, true } );
        String[] labels = (String[])Core.getInstance().getTripleStore().transactionalCall( inv );
        return( labels[ 0 ] );
    }

    class ContributeComparator implements Comparator<String> {
        public int compare( String a, String b ) {
            int indexOfPersonInA = a.indexOf( "person" );
            int indexOfOrgInA = a.indexOf( "organization" );
            int indexOfPersonInB = b.indexOf( "person" );
            int indexOfOrgInB = b.indexOf( "organization" );
            
            if( indexOfPersonInA != -1 && indexOfPersonInB != -1 ||
                indexOfOrgInA != -1 && indexOfOrgInB != -1 )
                return( 0 );
                
            if( indexOfOrgInA != -1 && indexOfPersonInB != -1 )
                return( 1 );
                
            return( -1 );
        }
    }

}
