package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Security;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.RepositoryManager;
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

    @GET
    @Path( "{id}/html" )
    @Produces( MediaType.TEXT_HTML )
    public String getRepositoryAsHtml( @PathParam( "id" ) String id, @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
//        Locale locale = ( "fr".equals( lang ) ? Locale.FRENCH : Locale.ENGLISH );
//        String repositoryUri = Util.makeURI(id, Constants.TYPE_REPOSITORY);
//        String html = Core.getInstance().getDefaultView().getHtml(repositoryUri, locale, "default", context);
//        return( html );
        return null;
    }

    @GET
    @Path( "{id}/rdf" )
    @Produces( "application/rdf+xml" )
    public String getRepositoryAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "false" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
//        boolean isRdfMetadataInfos = ( "true".equals( rdfMetadataInfos ) );
//        boolean isHumanReadable = ( "true".equals( humanReadable ) );
//        String repositoryUri = Util.makeURI(id, Constants.TYPE_REPOSITORY);
//        String rdf = Core.getInstance().getDefaultView().getRdf( repositoryUri, incomingLinks, isRdfMetadataInfos, isHumanReadable );
//        return( rdf );

        return null;
    }

    @GET
    @Path( "{id}/incomingLinks/rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getRepositoryIncomingLinksAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "25" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        /*boolean isHumanReadable = ( "true".equals( humanReadable ) );

        int offset = -1;
        if( strOffset != null ) {
            try {
                offset = Integer.parseInt( strOffset );
            }
            catch( NumberFormatException e ) {
                throw( new WebApplicationException( e, HttpServletResponse.SC_BAD_REQUEST ) );
            }
        }

        int limit = -1;
        if( strLimit != null ) {
            try {
                limit = Integer.parseInt( strLimit );
            }
            catch( NumberFormatException e ) {
                throw( new WebApplicationException( e, HttpServletResponse.SC_BAD_REQUEST ) );
            }
        }

        String repositoryUri = Util.makeURI(id, Constants.TYPE_REPOSITORY);

        String links = Core.getInstance().getDefaultView().getIncomingLinks( repositoryUri, isHumanReadable, offset, limit, format );
        if( "rdf".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( "application/rdf+xml" ).build() ); 
        if( "json".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( MediaType.APPLICATION_JSON ).build() ); 
        throw( new WebApplicationException( HttpServletResponse.SC_BAD_REQUEST ) ); // Unsupported format.*/

        return null;
    }

    @PUT
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response addOrUpdateRepository( @Context HttpServletRequest request,
                                           @PathParam( "id" ) String id, @QueryParam( "name" ) String name, @QueryParam( "type" ) String type, @QueryParam( "url" ) String url, @QueryParam( "adminEmail" ) String adminEmail ) throws Exception {
        if (!Security.isAuthorized(request.getRemoteAddr()))
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

        if (!Security.isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete records.").build();

        RepositoryManager.getInstance().deleteRepositoryRecords(id);
        return (Response.ok().build());
    }

}
