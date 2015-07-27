package ca.licef.comete.identity.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.identity.Identity;
import ca.licef.comete.security.Security;
import ca.licef.comete.vocabularies.COMETE;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.FOAF;
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
import java.net.URI;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 5-Jun-2012
 */

@Singleton
@Path( "/persons" )
@Api( value = "Person" )
public class PersonResource {

    static ca.licef.comete.core.util.Util CoreUtil;

    @GET
    @Path( "{id}/html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getPersonAsHtml( @PathParam( "id" ) String id, @DefaultValue( "en" ) @QueryParam( "lang" ) String lang, @DefaultValue( "default" ) @QueryParam( "style" ) String style ) throws Exception {
        Locale locale = ( "fr".equals( lang ) ? Locale.FRENCH : Locale.ENGLISH );
        String personUri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        String html = Identity.getInstance().getPersonView().getHtml( personUri, locale, style, context );
        return Response.ok(html).build();
    }

    @GET
    @Path( "{id}/rdf" )
    @Produces( "application/rdf+xml" )
    public Response getPersonAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "false" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        boolean isRdfMetadataInfos = ( "true".equals( rdfMetadataInfos ) );
        boolean isHumanReadable = ( "true".equals( humanReadable ) );

        String personUri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        String rdf = Identity.getInstance().getPersonView().getRdf( personUri, incomingLinks, isRdfMetadataInfos, isHumanReadable );
        return Response.ok(rdf).build();
    }

    @GET
    @Path( "{id}/incomingLinks/rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getPersonIncomingLinksAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "25" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        boolean isHumanReadable = ( "true".equals( humanReadable ) );

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

        String personUri = CoreUtil.makeURI(id, COMETE.Person.getURI());

        String links = Core.getInstance().getDefaultView().getIncomingLinks( personUri, isHumanReadable, offset, limit, format );
        if( "rdf".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( "application/rdf+xml" ).build() ); 
        if( "json".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( MediaType.APPLICATION_JSON ).build() ); 
        throw( new WebApplicationException( HttpServletResponse.SC_BAD_REQUEST ) ); // Unsupported format.
    }
    
    @GET
    @Path("{id}/photo")
    public Response getPhoto(@PathParam("id") String id) {
        try {
            String uri = CoreUtil.makeURI(id, COMETE.Person.getURI());
            String photoUrl = Identity.getInstance().getPhotoUrl(uri, FOAF.img);
            if (photoUrl == null)
                return Response.status(Response.Status.NOT_FOUND).build();
            else {
                return Response.seeOther(new URI(photoUrl)).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw( new WebApplicationException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
        }
    }

    @GET
    @Path("{id}/vcard")
    @Produces( MediaType.TEXT_PLAIN )
    public Response getVCard(@PathParam("id") String id, @DefaultValue( "" ) @QueryParam( "loUri" ) String loUri) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        return Response.ok(Identity.getInstance().getVCard(uri, loUri)).build();
    }

    @GET
    @Path("{id}/fn")
    @Produces( MediaType.TEXT_PLAIN )
    public Response getFN(@PathParam("id") String id, @DefaultValue( "" ) @QueryParam( "loUri" ) String loUri) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        return Response.ok(Identity.getInstance().getFN(uri, loUri)).build();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response all( @Context HttpServletRequest req, 
                         @DefaultValue( "0" ) @QueryParam( "start" ) String strStart,
                         @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit ) throws Exception {
        int start = -1;
        if( strStart != null ) {
            try {
                start = Integer.parseInt( strStart );
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

        boolean isShowHiddenPersons = Security.getInstance().isAuthorized( req );
        Object[] res = Identity.getInstance().getAllPersons(isShowHiddenPersons, start, limit);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "persons" ).value( buildPersonJSONArray( (Tuple[])res[1]) ).
                 key( "totalCount" ).value( res[0] );
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

        return Response.ok(out.toString()).build();
    }

    @GET
    @Path( "search" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response searchJson( @Context HttpServletRequest req, 
                                @QueryParam( "q" ) String str,
                                @DefaultValue( "0" ) @QueryParam( "start" ) String strStart,
                                @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit ) throws Exception {
        if ("".equals(str))
            return null;

        int start = -1;
        if( strStart != null ) {
            try {
                start = Integer.parseInt( strStart );
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


        boolean isShowHiddenPersons = Security.getInstance().isAuthorized( req );
        Object[] res = Identity.getInstance().searchPersons(str, isShowHiddenPersons, start, limit);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "persons" ).value( buildPersonJSONArray((Tuple[])res[1])).
                 key( "totalCount" ).value( res[0] );
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

        return Response.ok(out.toString()).build();
    }

    @GET
    @Path( "{id}/details" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPersonDetails(@PathParam( "id" ) String id) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray details = Identity.getInstance().getPersonDetails(uri);
            json.key( "personDetails" ).value( details );
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

        return Response.ok(out.toString()).build();
    }

    @GET
    @Path( "{id}/allDetails" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAllPersonDetails(@PathParam( "id" ) String id) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray wrapper = new JSONArray();
            wrapper.put(uri);
            JSONObject details = Identity.getInstance().getAllPersonDetails(wrapper);
            json.key( "details" ).value( details );
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

        return Response.ok(out.toString()).build();
    }

    @PUT
    @Path("{id}")
    public Response updatePerson(@Context HttpServletRequest request, @PathParam( "id" ) String id,
                                 @FormParam("mainValues") String mainValues) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to update persons.").build();

        String uri = CoreUtil.makeURI(id, COMETE.Person.getURI());
        JSONObject values = new JSONObject(mainValues);
        Identity.getInstance().updateIdentity(uri, values, COMETE.Person);
        return Response.ok().build();
    }

    @GET
    @Path("similarGroups")
    @Produces( MediaType.APPLICATION_JSON )
    public Response similarPersonGroups() throws Exception {

        Tuple[] results = Identity.getInstance().getSimilarPersonGroups();

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray groups = new JSONArray();
            for (int i = 0; i < results.length; i++)
                groups.put(results[i].getValue("gid").getContent());
            json.key( "groups" ).value( groups );
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

        return Response.ok(out.toString()).build();
    }

    @GET
    @Path("similar")
    @Produces( MediaType.APPLICATION_JSON )
    public Response similarPersons(@QueryParam("gid") String gid) throws Exception {

        Tuple[] persons = Identity.getInstance().getSimilarIdentities(gid);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "persons" ).value( buildPersonJSONArray(persons) );
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

        return Response.ok(out.toString()).build();
    }

    private JSONArray buildPersonJSONArray(Tuple[] persons) throws Exception{
        JSONArray _persons = new JSONArray();
        for (Tuple person : persons) {
            JSONObject _person = new JSONObject();
            String uri = person.getValue("s").getContent();
            String id = CoreUtil.getIdValue(uri);
            _person.put( "id", id );
            _person.put( "uri", uri );
            _person.put( "label", person.getValue("name").getContent());
            _person.put( "restUrl", CoreUtil.getRestUrl(COMETE.Person) + "/" + id);
            _persons.put(_person);
        }
        return _persons;
    }

    @GET
    @Path("preMergeDetails")
    @Produces( MediaType.APPLICATION_JSON )
    public Response preMergeDetails(@QueryParam("uris") String uris) throws Exception {
        JSONArray uriArray = new JSONArray(uris); //array of persons to merge
        StringWriter out = new StringWriter();
        try{
            JSONWriter json = new JSONWriter( out ).object();
            JSONObject details = Identity.getInstance().getAllPersonDetails(uriArray);
            json.key( "details" ).value( details );
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

        return Response.ok(out.toString()).build();
    }

    @POST
    @Path("merge")
    public Response mergePersons(@Context HttpServletRequest request,
                                 @FormParam("uris") String uris,
                                 @FormParam("mainValues") String mainValues) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to merge persons.").build();

        JSONArray uriArray = new JSONArray(uris);
        JSONObject values = new JSONObject(mainValues);
        String uri = Identity.getInstance().mergeIdentities(uriArray, values, COMETE.Person);
        return Response.ok(uri).build();
    }

    @GET
    @Path("takeOff")
    public Response takeOffPersons(@Context HttpServletRequest request,
                                   @QueryParam("uris") String uris,
                                   @QueryParam("similarGroup") String gId) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to takeoff persons.").build();

        JSONArray uriArray = new JSONArray(uris);
        Identity.getInstance().takeOffIdentities(uriArray, gId);
        return Response.ok().build();
    }

    @GET
    @Path("convertToOrg")
    public Response convertPersonsToOrg(@Context HttpServletRequest request, @QueryParam("uris") String uris) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to typecast persons.").build();

        JSONArray uriArray = new JSONArray(uris);
        Identity.getInstance().convertPersonsToOrg(uriArray);
        return Response.ok().build();
    }

    @Context
    private ServletContext context;
}
