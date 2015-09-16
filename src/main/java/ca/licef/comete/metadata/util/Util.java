package ca.licef.comete.metadata.util;

import ca.licef.comete.core.util.Constants;
import licef.StringUtil;
import licef.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Hashtable;

/**
 * Created by amiara on 2015-03-12.
 */
public class Util {

    public static String[] parseMetadataRecord(File file) throws Exception {
        return parseMetadataRecord(XMLUtil.getXMLNode(file));
    }

    public static String[] parseMetadataRecord(Node node) throws Exception {
        String errorMessage = null;
        String content = null;
        String namespace = null;
        String pseudoOaiID = null;
        String xpath = null;
        try {
            content = XMLUtil.getXMLString( node );
            String rootname = XMLUtil.getRootTagName(content);
            Hashtable namespaces = XMLUtil.getAttributes(content, "/");
            String[] array = StringUtil.split(rootname, ':');
            rootname = array[array.length - 1].toLowerCase();
            if ("lom".equals(rootname) && namespaces.containsValue(Constants.IEEE_LOM_NAMESPACE)) {
                namespace = Constants.IEEE_LOM_NAMESPACE;
                xpath = "//lom:lom/lom:metaMetadata/lom:identifier/lom:entry";
            }
            else if ("dc".equals(rootname) && namespaces.containsValue(Constants.OAI_DC_NAMESPACE)) {
                namespace = Constants.OAI_DC_NAMESPACE;
                xpath = "//dc:identifier";
            }
            else
                errorMessage = "Wrong metadata format.";

            //check of 3.1 metaMetadata identifier existence for LOM or dc:identifier for DC.
            NodeList identifierNodes = XMLUtil.getNodeList( node, xpath );
            for( int i = 0; i < identifierNodes.getLength(); i++ ) {
                Node identNode = identifierNodes.item( i );
                pseudoOaiID = identNode.getTextContent();
                break;
            }
            if (pseudoOaiID == null)
                errorMessage = "No metametadata identifier field.";
        } catch (Exception e) {
            errorMessage = "Error on record parsing.";
        }

        return new String[] {errorMessage, content, namespace, pseudoOaiID};
    }

    public static String[] getResourceIdentifier(String content) throws Exception {
        return getResourceIdentifier(XMLUtil.getXMLNode(content));
    }

    public static String[] getResourceIdentifier(File file) throws Exception {
        return getResourceIdentifier(XMLUtil.getXMLNode(file));
    }

    public static String[] getResourceIdentifier(Node node) throws Exception {
        String errorMessage = null;
        String content = null;
        String identifier = null;
        String xpath = null;
        try {
            content = XMLUtil.getXMLString( node );
            String rootname = XMLUtil.getRootTagName(content);
            Hashtable namespaces = XMLUtil.getAttributes(content, "/");
            String[] array = StringUtil.split(rootname, ':');
            rootname = array[array.length - 1].toLowerCase();
            if ("lom".equals(rootname) && namespaces.containsValue(Constants.IEEE_LOM_NAMESPACE)) {
                xpath = "//lom:lom/lom:general/lom:identifier/lom:entry";
            }
            else if ("dc".equals(rootname) && namespaces.containsValue(Constants.OAI_DC_NAMESPACE)) {
                xpath = "//dc:identifier";
            }
            else
                errorMessage = "Wrong metadata format.";

            //check of 1.1 general identifier existence for LOM or dc:identifier for DC.
            NodeList identifierNodes = XMLUtil.getNodeList( node, xpath );
            for( int i = 0; i < identifierNodes.getLength(); i++ ) {
                Node identNode = identifierNodes.item( i );
                identifier = identNode.getTextContent();
                break;
            }
            if (identifier == null)
                errorMessage = "No identifier field.";
        } catch (Exception e) {
            errorMessage = "Error on record parsing.";
        }

        return new String[] {errorMessage, identifier};
    }
}
