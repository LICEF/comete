package ca.licef.comete.identity;

import ca.licef.comete.core.DefaultView;
import licef.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
public class OrganizationView extends DefaultView {

    public String getHtml( String uri, Locale locale, String style, ServletContext context ) throws Exception {
        String rdf = getRdf( uri, "false", false, false );
        InputSource inputSource = new InputSource( new BufferedReader( new StringReader( rdf ) ) );
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware( true );
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse( inputSource );
        Element rootElement = doc.getDocumentElement();

        String expandedXml = XMLUtil.serialize( doc, true );
//System.out.println( "expandedXml for Org:"+expandedXml );

        String styleSheet = "organizationToHtml";
        if( "ceres".equals( style ) )
            styleSheet = "organizationToHtmlForCeres";
        if( "endrea".equals( style ) )
            styleSheet = "organizationToHtmlForEndrea";

        Properties props = new Properties();
        props.setProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

        HashMap<String, String> params = new HashMap<String, String>();
        params.put( "uri", uri );

        StreamSource source = new StreamSource( new BufferedReader( new StringReader( expandedXml ) ) );
        return( ca.licef.comete.core.util.Util.applyXslToDocument( styleSheet, source, props, params, locale ) );
    }

}
