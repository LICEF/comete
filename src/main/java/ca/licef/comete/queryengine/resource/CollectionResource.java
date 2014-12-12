package ca.licef.comete.queryengine.resource;

import ca.licef.comete.queryengine.QueryEngine;
import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 7-Nov-2012
 */

@Singleton
@Path( "/collections" )
public class CollectionResource {

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public String getCollections( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        List<String[]> collections = QueryEngine.getInstance().getCollection().getAll(lang);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray colls = new JSONArray();
            int index = 0;
            for (String[] _col : collections) {
                JSONObject col = new JSONObject();
                col.put( "id", "coll_" + index );
                col.put( "label", _col[0] );
                JSONArray query = new JSONArray();
                JSONObject queryEl = new JSONObject();
                queryEl.put( "key", "collection" );
                queryEl.put( "value", "coll_" + index );
                query.put(queryEl);
                col.put( "query", query );
                colls.put(col);
                index++;
            }
            json.key( "collections" ).value( colls );
            json.endObject();
        }
        catch( JSONException e ) {
            e.printStackTrace();
        }

        try {
            out.close();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }

        return( out.toString() );
    }

    @POST
    public Response addCollection(@Context HttpServletRequest request,
                                  @FormParam( "label" ) String label,
                                  @FormParam( "q" ) String query) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        QueryEngine.getInstance().getCollection().addCollection(label, query);
        return Response.ok().build();
    }

}
