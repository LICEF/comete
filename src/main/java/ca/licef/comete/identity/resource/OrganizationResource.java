package ca.licef.comete.identity.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.identity.Identity;
import ca.licef.comete.security.Security;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.sun.jersey.spi.resource.Singleton;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.RDF;
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
import java.util.Hashtable;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 5-Jun-2012
 */

@Singleton
@Path( "/organizations" )
public class OrganizationResource {

    static ca.licef.comete.core.util.Util CoreUtil;

    @GET
    @Path( "{id}/html" )
    @Produces( MediaType.TEXT_HTML )
    public String getOrganizationAsHtml( @PathParam( "id" ) String id, @DefaultValue( "en" ) @QueryParam( "lang" ) String lang, @DefaultValue( "default" ) @QueryParam( "style" ) String style ) throws Exception {
        Locale locale = ( "fr".equals( lang ) ? Locale.FRENCH : Locale.ENGLISH );
        String organizationUri = CoreUtil.makeURI(id, COMETE.Organization);
        String html = Identity.getInstance().getOrganizationView().getHtml( organizationUri, locale, style, context );
        return( html );
    }

    @GET
    @Path( "{id}/rdf" )
    @Produces( "application/rdf+xml" )
    public String getOrganizationAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "false" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        boolean isRdfMetadataInfos = ( "true".equals( rdfMetadataInfos ) );
        boolean isHumanReadable = ( "true".equals( humanReadable ) );
        String organizationUri = CoreUtil.makeURI(id, COMETE.Organization);
        String rdf = Identity.getInstance().getOrganizationView().getRdf( organizationUri, incomingLinks, isRdfMetadataInfos, isHumanReadable );
        return( rdf );
    }

    @GET
    @Path( "{id}/incomingLinks/rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getOrganizationIncomingLinksAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "25" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
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

        String organizationUri = CoreUtil.makeURI(id, COMETE.Organization.getURI());

        String links = Core.getInstance().getDefaultView().getIncomingLinks( organizationUri, isHumanReadable, offset, limit, format );
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
            String uri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
            Triple[] triples = Core.getInstance().getTripleStore().
                    getTriplesWithSubjectPredicate(uri, FOAF.logo);
            if (triples.length == 0)
                return Response.status(Response.Status.NOT_FOUND).build();
            else {
                String url = CoreUtil.manageQuotes(triples[0].getObject());
                return Response.seeOther(new URI(url)).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw( new WebApplicationException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
        }
    }

    @GET
    @Path("{id}/vcard")
    @Produces( MediaType.TEXT_PLAIN )
    public Response getVCard(@PathParam("id") String id) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
        return Response.ok(Identity.getInstance().getVCard(uri, null)).build();
    }

    @GET
    @Path("{id}/fn" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response getFN(@PathParam("id") String id) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
        return Response.ok(Identity.getInstance().getFN(uri, null)).build();
    }

    @GET
    @Path( "{id}/persons" )
    @Produces(  MediaType.APPLICATION_JSON  )
    public String getPersonsOfOrganization( @PathParam( "id" ) String id ) throws Exception {
        String organizationUri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        String query = CoreUtil.getQuery("identity/getPersonsOfOrg.sparql", organizationUri);
        Tuple[] persons = tripleStore.sparqlSelect(query);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _persons = new JSONArray();
            for (Tuple person : persons) {
                JSONObject _person = new JSONObject();
                _person.put( "uri", person.getValue("s").getContent() );
                _person.put( "label", CoreUtil.manageQuotes(person.getValue("n").getContent()));
                _persons.put(_person);
            }
            json.key( "persons" ).value( _persons );
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

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public String all( @DefaultValue( "0" ) @QueryParam( "start" ) String strStart,
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

        Object[] res = Identity.getInstance().getAllOrganizations(start, limit);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "organizations" ).value( buildOrgJSONArray( (Tuple[])res[1]) ).
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

        return( out.toString() );
    }

    @GET
    @Path( "search" )
    @Produces( MediaType.APPLICATION_JSON )
    public String searchJson( @QueryParam( "q" ) String str,
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

        TripleStore tripleStore = Core.getInstance().getTripleStore();

        String query = CoreUtil.getQuery("identity/findOrganizationsCount.sparql", CoreUtil.formatKeywords(str));
        Tuple[] res = tripleStore.sparqlSelect(query);
        int total = Integer.parseInt(res[0].getValue("count").getContent());

        query = CoreUtil.getQuery("identity/findOrganizations.sparql", CoreUtil.formatKeywords(str), start, limit);
        Tuple[] orgs = tripleStore.sparqlSelect(query);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "organizations" ).value( buildOrgJSONArray(orgs) ).
                 key( "totalCount" ).value( total );
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

    @GET
    @Path( "{id}/details" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getOrganizationDetails(@PathParam( "id" ) String id) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray details = Identity.getInstance().getOrganizationDetails(uri);
            json.key( "organizationDetails" ).value( details );
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

    @GET
    @Path( "{id}/allDetails" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getAllOrganizationDetails(@PathParam( "id" ) String id) throws Exception {
        String uri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray wrapper = new JSONArray();
            wrapper.put(uri);
            JSONObject details = Identity.getInstance().getAllOrganizationDetails(wrapper);
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

        return( out.toString() );
    }

    @PUT
    @Path("{id}")
    public Response updateOrganization(@Context HttpServletRequest request, @PathParam( "id" ) String id,
                                       @FormParam("mainValues") String mainValues) throws Exception {

        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to update organizations.").build();

        String uri = CoreUtil.makeURI(id, COMETE.Organization.getURI());
        JSONObject values = new JSONObject(mainValues);
        Identity.getInstance().getResolver().updateIdentity(uri, values, COMETE.Organization.getURI());
        return Response.ok().build();
    }

    @GET
    @Path("similarGroups")
    @Produces( MediaType.APPLICATION_JSON )
    public String similarOrgGroups() throws Exception {
        Identity.getInstance(); //for similar graph creation
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        //todo sparql multi-graph
        String query = CoreUtil.getQuery("identity/getSimilarOrganizationGroups.sparql");
        Tuple[] results = tripleStore.sparqlSelect(query);

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray groups = new JSONArray();
            for (int i = 0; i < results.length; i++)
                groups.put(CoreUtil.manageQuotes(results[i].getValue("gid").getContent()));
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

        return( out.toString() );
    }

    @GET
    @Path("similar")
    @Produces( MediaType.APPLICATION_JSON )
    public String similarOrganizations(@QueryParam("gid") String gid) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        //todo sparql multi-graph
        String query = CoreUtil.getQuery("identity/getIdentitiesOfSimilarGroup.sparql", gid);
        Tuple[] orgs = tripleStore.sparqlSelect(query);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "organizations" ).value( buildOrgJSONArray(orgs) );
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

    private JSONArray buildOrgJSONArray(Tuple[] orgs) throws Exception{
        JSONArray _orgs = new JSONArray();
        for (Tuple org : orgs) {
            JSONObject _org = new JSONObject();
            String uri = org.getValue("s").getContent();
            String id = CoreUtil.getIdNumberValue(uri);
            _org.put( "id", id );
            _org.put( "uri", uri );
            _org.put( "label", CoreUtil.manageQuotes(org.getValue("name").getContent()));
            _org.put( "restUrl", CoreUtil.getRestUrl(COMETE.Organization) + "/" + id);
            _orgs.put(_org);
        }
        return _orgs;
    }

    @GET
    @Path("preMergeDetails")
    @Produces( MediaType.APPLICATION_JSON )
    public String preMergeDetails(@QueryParam("uris") String uris) throws Exception {
        JSONArray uriArray = new JSONArray(uris); //array of orgs to merge
        StringWriter out = new StringWriter();
        try{
            JSONWriter json = new JSONWriter( out ).object();
            JSONObject details = Identity.getInstance().getAllOrganizationDetails(uriArray);
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

        return( out.toString() );
    }

    @POST
    @Path("merge")
    public Response mergeOrganizations(@Context HttpServletRequest request,
                                       @FormParam("uris") String uris,
                                       @FormParam("similarGroup") String similarGroup,
                                       @FormParam("mainValues") String mainValues) throws Exception {

        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to merge organizations.").build();

        JSONArray uriArray = new JSONArray(uris);
        JSONObject values = new JSONObject(mainValues);
        String uri = Identity.getInstance().getResolver().mergeIdentities(uriArray, values, similarGroup, COMETE.Organization.getURI());
        return Response.ok(uri).build();
    }

    @GET
    @Path("takeOff")
    public Response takeOffPersons(@Context HttpServletRequest request, @QueryParam("uris") String uris, @QueryParam("similarGroup") String similarGroup) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to takeoff organizations.").build();

        JSONArray uriArray = new JSONArray(uris);
        Identity.getInstance().getResolver().takeOffIdentities(uriArray, similarGroup);
        return Response.ok().build();
    }

    @Context
    private ServletContext context;
}
