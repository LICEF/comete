package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.RepositoryManager;
import ca.licef.comete.security.Security;
import ca.licef.comete.vocabularies.COMETE;
import com.sun.jersey.spi.resource.Singleton;
import licef.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Locale;

@Singleton
@Path( "/repositories" )
public class RepositoryResource {

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getRepositories() throws Exception {
        String[][] repositories = RepositoryManager.getInstance().getRepositories();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).array();
            for (int i = 0; i < repositories.length; i++) {
                JSONObject _repo = new JSONObject();
                _repo.put("uri", repositories[i][0]);
                _repo.put("label", repositories[i][1]);
                json.value(_repo);
            }
            json.endArray();
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
        return (Response.ok(out.toString()).build());
    }

    @PUT
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response addOrUpdateRepository( @Context HttpServletRequest request,
                                           @PathParam( "id" ) String id, @QueryParam( "name" ) String name, @QueryParam( "type" ) String type, @QueryParam( "url" ) String url, @QueryParam( "adminEmail" ) String adminEmail ) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to manage repository objects.").build();

        String repoUri = RepositoryManager.getInstance().addOrUpdateRepository( id, name, type, url, adminEmail );
        return Response.ok( repoUri ).build();
    }

    @GET
    @Path( "{id}/records" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response getRecords( @PathParam( "id" ) String id ) throws Exception {
        String[][] records =RepositoryManager.getInstance().getRepositoryRecords(id);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).array();
            for (int i = 0; i < records.length; i++)
                json.value( records[i][0] );
            json.endArray();
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
        return (Response.ok(out.toString()).build());
    }

    //BE CAREFUL WITH THIS SERVICE !!! -AM
    @DELETE
    @Path( "{id}/records" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response deleteRecords( @Context HttpServletRequest request,
                                   @PathParam( "id" ) String id ) throws Exception {

        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete records.").build();

        String repoUri = Util.makeURI(id, COMETE.Repository);
        RepositoryManager.getInstance().deleteRepositoryRecords(repoUri);
        return (Response.ok().build());
    }

}
