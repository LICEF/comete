package ca.licef.comete.metadata.deployment;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.Fedora;
import ca.licef.comete.core.util.Util;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import licef.XMLUtil;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 14-07-28
 */
public class FedoraDeployer extends ResourceDeployer {

    Fedora fedora;

    public FedoraDeployer(String fedoraUrl, String fedoraUsername, String fedoraPassword) {
        fedora = new Fedora();
        fedora.setUrl(fedoraUrl);
        fedora.setUsername(fedoraUsername);
        fedora.setPassword(fedoraPassword);
    }

    public String deploy(File f, String... data) throws Exception {
        //String record = data[0];

        //Node root = XMLUtil.getXMLNode(record);

        ////Resource ID
        //String id = XMLUtil.getString(root, "//lom:general/lom:identifier/lom:entry/text()");
        //if ("".equals(id))
        //    return "No identifier field";

        //if (id.startsWith("urn:"))
        //    id = id.substring("urn:".length());

        ////Check digital object existence
        //String url = fedora.getUrl() + "/get/" + id;
        //WebResource webResource = Core.getInstance().getRestClient().resource(url);
        //ClientResponse response = webResource.get( ClientResponse.class );
        //if (response.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
        //    //creation of Digital Object
        //    StreamSource source = new StreamSource( new StringReader( record ) );
        //    String foxml = Util.applyXslToDocument("lom2foxml", source, null);
        //    //patch to insert xsi namespace (no found solution with xslt) -AM
        //    foxml = foxml.replace(
        //            "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"",
        //            "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        //    fedora.ingestDigitalObject(foxml, "Ingest");
        //    fedora.addDatastream(id, "main", "exposed resource", false, f, null, "", "M", "add");
        //}
        //else
        //    fedora.modifyDatastream(id, "main", f, "update");

        //return null;
        return( null );
    }

}
