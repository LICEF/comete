package ca.licef.comete.queryengine.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.queryengine.QueryCache;
import ca.licef.comete.queryengine.QueryEngine;
import ca.licef.comete.queryengine.ResultEntry;
import ca.licef.comete.queryengine.util.FeedUtil;
import ca.licef.comete.queryengine.util.Util;
import com.sun.jersey.spi.container.servlet.PerSession;
import com.sun.syndication.feed.synd.SyndFeed;
import licef.StringUtil;
import licef.tsapi.TripleStore;
import licef.tsapi.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.ListIterator;

@PerSession
@Path( "/queryEngine" )
public class QueryEngineResource implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String FEED_ATOM = "atom_1.0";
    public final static String FEED_RSS = "rss_2.0";

    transient QueryCache cache = new QueryCache();

    @GET
    @Path( "searchJson" )
    @Produces( MediaType.APPLICATION_JSON )
    public String searchJson( @DefaultValue( "" ) @QueryParam( "q" ) String query,
        @DefaultValue( "" ) @QueryParam( "f" ) String filters,
        @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit,
        @DefaultValue( "default" ) @QueryParam( "style" ) String style,
        @DefaultValue( "en" ) @QueryParam( "lang" ) String lang) {

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
            rs = QueryEngine.getInstance().search(
                    query, filters, lang, "json", Integer.valueOf(start), Integer.valueOf(limit), style, cache);
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
                    .put( "type", entry.getType() );
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

        return( out.toString() );
    }

    @GET
    @Path( "labels" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getLabels( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang, @QueryParam( "uris" ) String uris ) throws Exception {
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).array();

            TripleStore tripleStore = Core.getInstance().getTripleStore();
            JSONArray array = new JSONArray(uris);
            for (int i = 0; i < array.length(); i++) {
                String uri = array.getString(i);
                String label = "";
                if (uri.startsWith(Core.getInstance().getUriPrefix() + "/voc/")) { //prefix by vocabulary title
                    String vocUri = uri.substring(0, uri.lastIndexOf('/'));
                    String s = vocUri.substring((Core.getInstance().getUriPrefix() + "/voc/").length());
                    String[] elems = StringUtil.split(s, '/');
                    String graph = "voc_" + (elems[0] + "_" + elems[1]).toLowerCase();
                    String[] title = tripleStore.getBestLocalizedLiteralObject(vocUri, RDFS.label, lang, graph);
                    if (!(title == null || title[ 0 ] == null || "".equals(title[ 0 ])))
                        label += title[0] + ":";
                }
//                label += tripleStore.getResourceLabel(uri, lang)[0];
//                json.value(label);
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
        return( out.toString() );
    }

    @GET
    @Path( "keywords" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getKeywords( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang, @QueryParam( "value" ) String value ) throws Exception {
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray keywords = new JSONArray();

            if( value != null && !"".equals( value ) ) {
                TripleStore tripleStore = Core.getInstance().getTripleStore();
                /*Hashtable<String, String>[] results = tripleStore.getResults( "lookupKeywords.sparql", lang, value );
                for( int i = 0; i < results.length; i++ ) {
                    Hashtable<String, String> result = results[ i ];
                    String keyword = Util.manageLocalizedString(result.get( "o" ))[0];

                    JSONObject kw = new JSONObject();
                    kw.put( "keyword", keyword );
                    keywords.put( kw );
                }*/
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
        return( out.toString() );
    }

    @GET
    @Path( "searchAtom" )
    @Produces( MediaType.APPLICATION_ATOM_XML )
    public SyndFeed searchAtom( @DefaultValue( "" ) @QueryParam( "q" ) String query,
        @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit,
        @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) {
        return( searchFeed( FEED_ATOM, query, strStart, strLimit, lang ) );
    }

    @GET
    @Path( "searchRss" )
    @Produces( "application/rss+xml" )
    public SyndFeed searchRss( @DefaultValue( "" ) @QueryParam( "q" ) String query,
        @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit,
        @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) {
        return( searchFeed( FEED_RSS, query, strStart, strLimit, lang ) );
    }

    private SyndFeed searchFeed( String feedType, String query, String strStart, String strLimit, String lang ) {
System.out.println( "searchFeed feedType="+feedType+" query="+query+" strStart="+strStart+" strLimit="+strLimit+" lang="+lang );        
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
System.out.println( "Before rs" );            
            rs = QueryEngine.getInstance().search( query, "", lang, feedType, start, limit, null, cache );
System.out.println( "After rs" );            
        }
        catch( Exception e ) {
            throw( new WebApplicationException( e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
        }
System.out.println( "Before f" );
        SyndFeed f = FeedUtil.getFeedFromResultSet( rs, feedType, uriInfo.getAbsolutePath().toString(), query, start, limit, lang );
System.out.println( "After f" );        
        return f;
    }

    @Context private transient UriInfo uriInfo;
    @Context private ServletContext context;
}
