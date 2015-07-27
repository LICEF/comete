package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.RepositoryManager;
import ca.licef.comete.security.Security;
import ca.licef.comete.vocabularies.COMETE;
import com.sun.jersey.spi.resource.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
@Path( "/repositories" )
@Api( value = "Repository" )
public class RepositoryResource {

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "Get all known repositories." )
    public Response getRepositories() throws Exception {
        String[][] repositories = RepositoryManager.getInstance().getRepositories();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray repos = new JSONArray();

            for (int i = 0; i < repositories.length; i++) {
                JSONObject repo = new JSONObject();
                repo.put("uri", repositories[i][0]);
                repo.put("label", repositories[i][1]);
                repos.put(repo);
            }
            json.key( "repositories" ).value( repos );
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
        return (Response.ok(out.toString()).build());
    }

    @PUT
    @Path( "{uid}" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Add or update a repository.", notes = "This can only be used by an Administrator." )
    public Response addOrUpdateRepository( @Context HttpServletRequest request,
                                           @PathParam( "uid" ) String uid, @QueryParam( "label" ) String label, @QueryParam( "type" ) String type, @QueryParam( "url" ) String url, @QueryParam( "adminEmail" ) String adminEmail, @QueryParam( "id" ) String id ) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to manage repository objects.").build();

        String repoUri = RepositoryManager.getInstance().addOrUpdateRepository( uid, label, type, url, adminEmail, id );
        return Response.ok( repoUri ).build();
    }

    @GET
    @Path( "{uri}/records" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Get the records contained in the repository." )
    public Response getRecords( @PathParam( "uri" ) String uri ) throws Exception {
        String[][] records = RepositoryManager.getInstance().getRepositoryRecords(uri);
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
    @Path( "{uri}/records" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Delete all the records of the repository.", notes = "This can only be used by an Administrator." )
    public Response deleteRecords( @Context HttpServletRequest request,
                                   @PathParam( "uri" ) String uri ) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete records.").build();

        String repoUri = Util.makeURI(uri, COMETE.Repository);
        RepositoryManager.getInstance().deleteRepositoryRecords(repoUri);
        return (Response.ok().build());
    }

}
