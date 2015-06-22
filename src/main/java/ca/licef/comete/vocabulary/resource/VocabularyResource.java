package ca.licef.comete.vocabulary.resource;

import ca.licef.comete.store.Store;
import ca.licef.comete.vocabulary.Vocabulary;
import com.sun.jersey.spi.resource.Singleton;
import licef.StringUtil;
import licef.tsapi.model.Tuple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

@Singleton
@Path( "/voc" )
public class VocabularyResource {

    @GET
    @Path( "{id}/{format}" )
    @Produces( MediaType.APPLICATION_XML )
    public Response getVocabularyAsXml( @PathParam( "id" ) String id, @PathParam( "format" ) String format) throws Exception {
        String xml = null;
        try {
            xml = Store.getInstance().getDatastream(Store.PATH_VOCABS + "/" + id, id + "." + format.toLowerCase());
        } catch (IOException e) {
        }
        if (xml == null)
            return( Response.status( HttpServletResponse.SC_NOT_FOUND).build() );
        else
            return( Response.status( HttpServletResponse.SC_OK ).entity( xml ).type( MediaType.APPLICATION_XML ).build() );
    }

    @GET
    @Path( "all" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocabularies( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        String[] vocabularies = Vocabulary.getInstance().getNavigableVocabularies();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray vocabs = new JSONArray();
            for (String vocUri : vocabularies) {
                JSONObject voc = new JSONObject();
                voc.put( "uri", vocUri );
                voc.put( "restUrl", Vocabulary.getInstance().getRestUrl(vocUri));
                voc.put( "label", Vocabulary.getInstance().getLabel(vocUri, lang) );
                vocabs.put(voc);
            }
            json.key( "vocabularies" ).value( vocabs );
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
    @Path( "{uri}/topConcepts" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocabularyTopConcepts(  @PathParam( "uri" ) String uri,
                               @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                               @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {

        Object[] res = Vocabulary.getInstance().getTopConcepts(uri);
        boolean controlledPos = (boolean)res[0];
        String[] uris = (String[])res[1];

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONObject[] _concepts = new JSONObject[uris.length];
            for (int i = 0; i < uris.length; i++)
                _concepts[i] = (buildJSONConcept(uris[i], showIds, null, lang, true));
            if (!controlledPos)
                sortConcepts(_concepts);

            JSONArray _conceptsOrdered = new JSONArray();
            for (JSONObject obj : _concepts) {
                obj.remove("label");
                _conceptsOrdered.put(obj);
            }

            json.key("concepts").value(_conceptsOrdered);
            json.key("label").value(Vocabulary.getInstance().getLabel(uri, lang));
            json.key("restUrl").value(Vocabulary.getInstance().getRestUrl(uri));

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
    @Path( "{uri}/children" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocabularyConceptChildren( @PathParam( "uri" ) String uri,
                                                        @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                                                        @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        Object[] res = Vocabulary.getInstance().getChildren(uri);
        boolean controlledPos = (boolean)res[0];
        String[] children = (String[])res[1];
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONObject[] _concepts = new JSONObject[children.length];
            for (int i = 0; i < children.length; i++)
                _concepts[i] = (buildJSONConcept(children[i], showIds, null, lang, true));
            if (!controlledPos)
                sortConcepts(_concepts);

            JSONArray _conceptsOrdered = new JSONArray();
            for (JSONObject obj : _concepts) {
                obj.remove("label");
                _conceptsOrdered.put(obj);
            }

            json.key( "concepts" ).value( _conceptsOrdered );
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
    @Path( "{uri}/hierarchy" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocabularyConceptHierarchy( @PathParam( "uri" ) String uri,
                                                 @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                                                 @DefaultValue( "false" ) @QueryParam( "includeScheme" ) boolean includeScheme,
                                                 @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        Tuple[] concepts = Vocabulary.getInstance().getHierarchy(uri);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray _concepts = new JSONArray();

            JSONObject top = new JSONObject();
            String scheme = Vocabulary.getInstance().getConceptScheme(uri);
            top.put( "label", Vocabulary.getInstance().getLabel(scheme, lang) );
            top.put( "restUrl", Vocabulary.getInstance().getRestUrl(scheme));
            _concepts.put(top);

            //reversing the list
            for (int i = concepts.length-1; i >= 0; i--) {
                Tuple concept = concepts[i];
                _concepts.put( buildJSONConcept(concept.getValue("parent").getContent(), showIds, i == 0?null:false, lang, false));
            }

            json.key( "concepts" ).value( _concepts );
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
    @Path( "{uri}/scheme" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getConceptScheme( @PathParam( "uri" ) String uri) throws Exception {
        String scheme = Vocabulary.getInstance().getConceptScheme( uri );
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key( "scheme" ).value( scheme );
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
    @Path( "orphanConcepts" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getOrphanConcepts() throws Exception {
        Tuple[] concepts = Vocabulary.getInstance().getOrphanConcepts();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray array = new JSONArray();
            for (Tuple res : concepts) {
                array.put( res.getValue("concept").getContent() );
            }
            json.key( "concepts" ).value( array );
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

    private JSONObject buildJSONConcept(String uri, String showIds, Boolean isLeaf, String lang, boolean includeLabel) throws Exception{
        JSONObject _concept = new JSONObject();
        String label = Vocabulary.getInstance().getLabel(uri, lang);
        label = label.trim();
        String text = label;
        if (Boolean.parseBoolean(showIds)) {
            char delimiter = '/';
            if (uri.contains("#"))
                delimiter = '#';
            String[] spl = StringUtil.split(uri, delimiter);
            String id = spl[spl.length - 1];
            text = id + "&nbsp;&nbsp;" + label;
        }
        _concept.put( "uri", uri );
        _concept.put( "restUrl", Vocabulary.getInstance().getRestUrl(uri));
        _concept.put( "text", text );
        if (includeLabel) //for sorting, will be remove before response
            _concept.put( "label", label );
        if (isLeaf == null) {
            Object[] children2 = Vocabulary.getInstance().getChildren(uri);
            isLeaf = (((Object[])children2[1]).length == 0);
        }
        if (isLeaf)
            _concept.put( "leaf", true );

        return _concept;
    }

    private void sortConcepts(JSONObject[] concepts) {
        Arrays.sort(concepts,
            new Comparator<JSONObject>() {
                public int compare(JSONObject o1, JSONObject o2) {
                    try {
                        String uri1 = (String)o1.get("uri");
                        String uri2 = (String)o2.get("uri");
                        if (uri1.toLowerCase().contains("uuid")) {
                            Collator collator = Collator.getInstance();
                            String label1 = (String) o1.get("label");
                            String label2 = (String) o2.get("label");
                            return collator.compare(label1, label2);
                        }
                        else
                            return uri1.compareTo(uri2);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return -1;
                    }
                }
            }
        );
    }

   /* @GET
    @Path( "equivalentConcepts" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getEquivalentConcepts( @QueryParam( "uri" ) String uri,
                                         @DefaultValue( "false" ) @QueryParam( "includeSubConcepts" ) String includeSubConcepts,
                                         @DefaultValue( "[]" ) @QueryParam( "fromVocs" ) String fromVocs) throws Exception {
        JSONArray fromVocsArray = new JSONArray(fromVocs);
        String[] concepts = Vocabulary.getInstance().getEquivalentConcepts(uri, "true".equals(includeSubConcepts), fromVocsArray);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).array();
            for (String concept : concepts)
                json.value(concept);
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
    }*/

    @GET
    @Path( "search" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response searchJson( @QueryParam( "q" ) String terms,
                              @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                              @DefaultValue( "en" ) @QueryParam( "lang" ) String lang) throws Exception {
        if ("".equals(terms))
            return null;

        boolean showId = Boolean.parseBoolean(showIds);
        Tuple[] results = Vocabulary.getInstance().searchConcepts(terms, lang);

        //local cache
        Hashtable<String, String> vocTitle = new Hashtable<>();

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray concepts = new JSONArray();
            for (Tuple tuple : results) {
                String uri = tuple.getValue("s").getContent();
                String vocUri = Vocabulary.getInstance().getConceptScheme(uri);
                String title = vocTitle.get(vocUri);
                if (title == null) {
                    title = Vocabulary.getInstance().getLabel(vocUri, lang);
                    vocTitle.put(vocUri, title);
                }
                String conceptLabel = Vocabulary.getInstance().getLabel(uri, lang);
                if (showId) {
                    String[] spl = StringUtil.split(uri, '/');
                    String id = spl[spl.length - 1];
                    conceptLabel = id + "&nbsp;&nbsp;" + conceptLabel;
                }
                JSONObject concept = new JSONObject();
                concept.put( "vocUri", vocUri );
                concept.put( "uri", uri );
                concept.put( "label", conceptLabel );
                concept.put( "vocLabel", title);
                concepts.put(concept);
            }
            json.key( "concepts" ).value( concepts );
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

    @Context
    private ServletContext context;
}
