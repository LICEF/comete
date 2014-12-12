package ca.licef.comete.metadata.util;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.metadataformat.MetadataFormat;
import ca.licef.comete.core.util.Constants;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.net.URLEncoder;


/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 19-Nov-2012
 */
public class Util {

    static ca.licef.comete.core.util.Util CoreUtil;

    public static String[] getIdentity(String content, MetadataFormat metadataFormat) {
        //String url = CoreUtil.getRestUrl(Constants.TYPE_IDENTITY) + "?format=" + metadataFormat.getIdentityMimetypeFormat();
        //WebResource webResource = Core.getInstance().getRestClient().resource( url );
        //ClientResponse response = webResource.accept( MediaType.APPLICATION_JSON ).post( ClientResponse.class, content);
        //int status = response.getStatus();
        //String[] uris = null;
        //if( status == 200 ) {
        //    String res = response.getEntity( String.class );
        //    try {
        //        JSONObject json = new JSONObject(res);
        //        String personUri = json.has("personUri")?json.getString("personUri"):null;
        //        String orgUri = json.has("orgUri")?json.getString("orgUri"):null;
        //        uris = new String[]{personUri, orgUri};
        //    } catch (JSONException e) {
        //        e.printStackTrace();
        //    }

        //}
        //return uris;
        return( null );
    }

    public static String getVocabularyConcept(String source, String element, String value) throws Exception {
        //source = URLEncoder.encode(source, "UTF-8");
        //if (element != null)
        //    element = URLEncoder.encode(element, "UTF-8");
        //value = URLEncoder.encode(value, "UTF-8");
        //value = value.replaceAll( "/", "%2F" );
        //String url = CoreUtil.getRestUrl(Constants.TYPE_VOCABULARY) + "/" + source;
        //if (element != null)
        //    url += ("/" + element);
        //url +=  "/" + value;
        //WebResource webResource = Core.getInstance().getRestClient().resource( url );
        //ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).get( ClientResponse.class );
        //int status = response.getStatus();
        //String uri = null;
        //if( status == 200 )
        //    uri = response.getEntity( String.class );
        //return uri;
        return( null );
    }

    public static String getVCard(String identity, String loURI) throws Exception {
        //String type = CoreUtil.getURIType(identity);
        //String url = CoreUtil.getRestUrl(type);
        //url += "/" + CoreUtil.getIdNumberValue(identity) + "/vcard";
        //if (Constants.TYPE_PERSON.equals(type))
        //    url += "?loUri=" + loURI;
        //WebResource webResource = Core.getInstance().getRestClient().resource( url );
        //ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).get( ClientResponse.class );
        //int status = response.getStatus();
        //String res = null;
        //if( status == 200 )
        //    res = response.getEntity( String.class );
        //return res;
        return( null );
    }

    public static String getFN(String identity, String loURI) throws Exception {
        //String type = CoreUtil.getURIType(identity);
        //String url = CoreUtil.getRestUrl(type);
        //url += "/" + CoreUtil.getIdNumberValue(identity) + "/fn";
        //if (Constants.TYPE_PERSON.equals(type))
        //    url += "?loUri=" + loURI;
        //WebResource webResource = Core.getInstance().getRestClient().resource( url );
        //ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).get( ClientResponse.class );
        //int status = response.getStatus();
        //String res = null;
        //if( status == 200 )
        //    res = response.getEntity( String.class );
        //return res;
        return( null );
    }

}
