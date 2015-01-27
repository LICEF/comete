package ca.licef.comete.identity;

import ca.licef.comete.core.DefaultView;
import ca.licef.comete.core.util.Util;
import licef.CommonNamespaceContext;
import licef.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 5-Jun-2012
 */
public class PersonView extends DefaultView {

    public String getHtml( String uri, Locale locale, String style, ServletContext context) throws Exception {
        String rdf = getRdf( uri, "false", false, false );
        InputSource inputSource = new InputSource( new BufferedReader( new StringReader( rdf ) ) );
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware( true );
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse( inputSource );
        Element rootElement = doc.getDocumentElement();

        // Insert organization element for each organization.
        NodeList orgElements = doc.getElementsByTagNameNS( CommonNamespaceContext.dctNSURI, "isPartOf" );
        for( int i = 0; i < orgElements.getLength(); i++ ) {
            Node orgNode = orgElements.item( i );
        
            // Skip non-element nodes.
            if( orgNode.getNodeType() != Node.ELEMENT_NODE )
                continue;

            Element orgElement = (Element)orgNode;

            String orgUri = orgElement.getAttributeNS( CommonNamespaceContext.rdfNSURI, "resource" );
            String orgRdf = Identity.getInstance().getOrganizationView().getRdf( orgUri, "false", false, false );

            InputSource orgSrc = new InputSource( new BufferedReader( new StringReader( orgRdf ) ) );
            DocumentBuilder docBuilderOrg = docFactory.newDocumentBuilder();
            Document docOrg = docBuilder.parse( orgSrc );

            // Required to make a copy of the node to insert into the main document.
            Node elementCopy = doc.importNode( docOrg.getDocumentElement(), true );

            Element newOrgElement = doc.createElementNS( "", "organization" );
            rootElement.appendChild( newOrgElement );
            newOrgElement.appendChild( elementCopy );
        }

        String expandedXml = XMLUtil.serialize( doc, true );
//System.out.println( "expandedXml for Person:"+expandedXml );

        String styleSheet = "identity/personToHtml";
        if( "ceres".equals( style ) )
            styleSheet = "identity/personToHtmlForCeres";
        else if( "endrea".equals( style ) )
            styleSheet = "identity/personToHtmlForEndrea";

        Properties props = new Properties();
        props.setProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

        HashMap<String, String> params = new HashMap<String, String>();
        params.put( "uri", uri );

        StreamSource source = new StreamSource( new BufferedReader( new StringReader( expandedXml ) ) );
        return( Util.applyXslToDocument( styleSheet, source, props, params, locale ) );
    }
}
