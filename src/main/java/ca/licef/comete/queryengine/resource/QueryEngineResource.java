package ca.licef.comete.queryengine.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.queryengine.QueryCache;
import ca.licef.comete.queryengine.QueryEngine;
import ca.licef.comete.queryengine.ResultEntry;
import ca.licef.comete.queryengine.util.FeedUtil;
import ca.licef.comete.security.Role;
import ca.licef.comete.security.Security;
import com.rometools.rome.feed.synd.SyndFeed;
import com.sun.jersey.spi.container.servlet.PerSession;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import licef.StringUtil;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.RDFS;
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
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@PerSession
@Path( "/queryEngine" )
@Api( value = "QueryEngine", description = "Search services" )
public class QueryEngineResource implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String FEED_ATOM = "atom_1.0";
    public final static String FEED_RSS = "rss_2.0";

    transient QueryCache cache = new QueryCache();

    @GET
    @Path( "searchJson" )
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "Performs a query and returned the result formatted in JSON." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of resources matching the search criterias with their attributes: title, desc, location, image, etc." )
    } )
    public Response searchJson( @Context HttpServletRequest req, 
            @ApiParam( value = "JSON-formatted query. Default: all resources." ) @DefaultValue( "[{\"key\":\"fulltext\",\"value\":\"\"}]" ) @QueryParam( "q" ) String query,
            @ApiParam( value = "Filter to modify the query." ) @DefaultValue( "" ) @QueryParam( "f" ) String filters,
            @ApiParam( value = "Index of the first resource." ) @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, 
            @ApiParam( value = "Number of resources per page." ) @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit,
            @ApiParam( value = "Style of the response." ) @DefaultValue( "default" ) @QueryParam( "style" ) String style,
            @ApiParam( value = "Language of the response.", allowableValues = "en,fr", required = true ) @DefaultValue( "en" ) @QueryParam( "lang" ) String lang) {

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

        ResultSet rs = null;
        try {
            if (cache == null)
                cache = new QueryCache();
            boolean isShowHiddenRes = Security.getInstance().isAuthorized( req );
            rs = QueryEngine.getInstance().search(
                    query, filters, lang, isShowHiddenRes, "json", Integer.valueOf(start), Integer.valueOf(limit), style, cache);
        }
        catch( Exception e ) {
            throw( new WebApplicationException( e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
        }

        StringWriter out = new StringWriter();

        try {
            JSONWriter json = new JSONWriter( out );
        
            JSONArray learningObjects = new JSONArray();
            
            for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
                ResultEntry entry = (ResultEntry)it.next();

                JSONObject learningObject = new JSONObject();
                learningObject.put( "id", entry.getId() )
                    .put( "title", entry.getTitle() )
                    .put( "desc", entry.getDescription() )
                    .put( "location", entry.getLocation() )
                    .put( "image", entry.getImage() )
                    .put( "loAsHtmlLocation", entry.getLoAsHtmlLocation() )
                    .put( "metadataFormat", entry.getMetadataFormat() )
                    .put( "type", entry.getType() )
                    .put( "forcedDiffusion", entry.isForcedDiffusion() )
                    .put( "pending", entry.isPending() )
                    .put( "inactive", entry.isInactive() )
                    .put( "invalid", entry.isInvalid() )
                    .put( "brokenLink", entry.isBrokenLink() );
                learningObjects.put( learningObject );
            }

            json.object()
                .key( "learningObjects" ).value( learningObjects )
                .key( "totalCount" ).value( rs.getTotalRecords() );

            for( Iterator<String> it = rs.getAdditionalDataKeys(); it.hasNext(); ) {
                String key = it.next();
                String value = rs.getAdditionalData( key );
                json.key( key ).value( value );
            }

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

    //@GET
    //@Path( "labels" )
    //@Produces( MediaType.APPLICATION_JSON )
    //public Response getLabels( 
    //        @DefaultValue( "en" ) @QueryParam( "lang" ) String lang, 
    //        @QueryParam( "uris" ) String uris ) throws Exception {
    //    StringWriter out = new StringWriter();
    //    try {
    //        JSONWriter json = new JSONWriter( out ).array();
    //        TripleStore tripleStore = Core.getInstance().getTripleStore();
    //        JSONArray array = new JSONArray(uris);
    //        for (int i = 0; i < array.length(); i++) {
    //            String uri = array.getString(i);
    //            String label = "";
    //            if (uri.startsWith(Core.getInstance().getUriPrefix() + "/voc/")) { //prefix by vocabulary title
    //                String vocUri = uri.substring(0, uri.lastIndexOf('/'));
    //                String s = vocUri.substring((Core.getInstance().getUriPrefix() + "/voc/").length());
    //                String[] elems = StringUtil.split(s, '/');
    //                String graph = "voc_" + (elems[0] + "_" + elems[1]).toLowerCase();
    //                String[] title = tripleStore.getBestLocalizedLiteralObject(vocUri, RDFS.label, lang, graph);
    //                if (!(title == null || title[ 0 ] == null || "".equals(title[ 0 ])))
    //                    label += title[0] + ":";
    //            }
    //              label += tripleStore.getResourceLabel(uri, lang)[0];
    //              json.value(label);
    //        }
    //        json.endArray();
    //    }
    //    catch( JSONException e ) {
    //        e.printStackTrace();
    //    }
    //    try {
    //        out.close();
    //    }
    //    catch( IOException e ) {
    //        e.printStackTrace();
    //    }
    //    return Response.ok(out.toString()).build();
    //}

    @GET
    @Path( "keywords" )
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "List all the known keywords containing the specified value." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of known keywords." )
    } )
    public Response getKeywords( 
            @ApiParam( value = "String contained in a keyword.  Default to empty string so that all keywords are returned." ) @DefaultValue( "" ) @QueryParam( "value" ) String value ) throws Exception {
        StringWriter out = new StringWriter();

        Tuple[] results = QueryEngine.getInstance().searchKeywords(value);
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray keywords = new JSONArray();

            for (Tuple tuple : results) {
                JSONObject kw = new JSONObject();
                kw.put( "keyword", tuple.getValue("o").getContent() );
                keywords.put( kw );
            }

            json.key( "keywords" ).value( keywords );

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
    @Path( "searchAtom" )
    @Produces( MediaType.APPLICATION_ATOM_XML )
    @ApiOperation( value = "Performs a query and returned the result formatted in Atom." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of resources matching the search criterias with their attributes: title, desc, location, image, etc." )
    } )
    public SyndFeed searchAtom( @Context HttpServletRequest req, 
            @ApiParam( value = "JSON-formatted query. Default: all resources." ) @DefaultValue( "[{\"key\":\"fulltext\",\"value\":\"\"}]" ) @QueryParam( "q" ) String query,
            @ApiParam( value = "Index of the first resource." ) @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, 
            @ApiParam( value = "Number of resources per page." ) @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit,
            @ApiParam( value = "Language of the response.", allowableValues = "en,fr", required = true ) @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) {
        boolean isShowHiddenRes = Security.getInstance().isAuthorized( req );
        return( searchFeed( FEED_ATOM, query, strStart, strLimit, lang, isShowHiddenRes ) );
    }

    @GET
    @Path( "searchRss" )
    @Produces( "application/rss+xml" )
    @ApiOperation( value = "Performs a query and returned the result formatted in RSS." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of resources matching the search criterias with their attributes: title, desc, location, image, etc." )
    } )
    public SyndFeed searchRss( @Context HttpServletRequest req, 
            @ApiParam( value = "JSON-formatted query. Default: all resources." ) @DefaultValue( "[{\"key\":\"fulltext\",\"value\":\"\"}]" ) @QueryParam( "q" ) String query,
            @ApiParam( value = "Index of the first resource." ) @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, 
            @ApiParam( value = "Number of resources per page." ) @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit,
            @ApiParam( value = "Language of the response.", allowableValues = "en,fr", required = true ) @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) {
        boolean isShowHiddenRes = Security.getInstance().isAuthorized( req );
        return( searchFeed( FEED_RSS, query, strStart, strLimit, lang, isShowHiddenRes ) );
    }

    private SyndFeed searchFeed( String feedType, String query, String strStart, String strLimit, String lang, boolean isShowHiddenRes ) {
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

        ResultSet rs = null;
        try {
            if (cache == null)
                cache = new QueryCache();
            rs = QueryEngine.getInstance().search( query, "", lang, isShowHiddenRes, feedType, start, limit, null, cache );
        }
        catch( Exception e ) {
            throw( new WebApplicationException( e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
        }
        SyndFeed f = FeedUtil.getFeedFromResultSet( rs, feedType, uriInfo.getAbsolutePath().toString(), query, start, limit, lang, isShowHiddenRes );
        return f;
    }

    /***************/
    /* Collections */
    /***************/
    @GET
    @Path( "collections" )
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "Get the list of collections for the specified language." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of collections with their id, query, and label." )
    } )
    public Response getCollections( 
            @ApiParam( value = "Language of the collections that we want to retrieve. If no collections in the specified language exist, the collections for all languages will be listed.", allowableValues = "en,fr" ) @DefaultValue( "" ) @QueryParam( "lang" ) String lang ) throws Exception {
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

        return Response.ok(out.toString()).build();
    }

    @POST
    @Path( "collections" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Add a new collection with no specified language.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "Empty body." ),
        @ApiResponse( code = 400, message = "Invalid values for parameters.  They should not be empty." ),
        @ApiResponse( code = 401, message = "Not authorized to add a collection." )
    } )
    public Response addCollection(@Context HttpServletRequest request,
            @ApiParam( value = "Label of the collection.", required = true ) @FormParam( "label" ) String label,
            @ApiParam( value = "Query of the collection.", required = true ) @FormParam( "q" ) String query) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity( "Not authorized to add a collection." ).build();

        if( StringUtil.isEmpty( label ) || StringUtil.isEmpty( query ) )
            return Response.status(Response.Status.BAD_REQUEST).entity( "Invalid values for parameters.  They should not be empty." ).build();

        QueryEngine.getInstance().getCollection().addCollection(label, query);
        return Response.ok().build();
    }

    @Context private transient UriInfo uriInfo;
    @Context private ServletContext context;
}
