package ca.licef.comete.vocabulary.resource;

import ca.licef.comete.core.util.Util;
import ca.licef.comete.store.Store;
import ca.licef.comete.vocabulary.Vocabulary;
import com.sun.jersey.spi.resource.Singleton;
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


@Singleton
@Path( "/voc" )
public class VocabularyResource {

    /*@GET
    @Path( "{source}/{cat}/rdf" )
    @Produces( "application/rdf+xml" )
    public Response getVocabularyAsRdf( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "false" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        *//*boolean isIncomingLinks = ( "true".equals( incomingLinks ) );
        boolean isRdfMetadataInfos = ( "true".equals( rdfMetadataInfos ) );
        boolean isHumanReadable = ( "true".equals( humanReadable ) );
        String graph = "voc_" + (source + "_" + cat).toLowerCase();
        String rdf = Vocabulary.getInstance().getVocabularyView().getRdf( graph, isIncomingLinks, isRdfMetadataInfos, isHumanReadable );
        if( rdf == null )
            return( Response.status( Response.Status.NOT_FOUND ).build() );
        else
            return( Response.ok( rdf ).build() );*//*

        return( Response.ok().build() );
    }*/

    /*@GET
    @Path( "{source}/{cat}/{concept}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response getVocabularyConcept( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept ) throws Exception {
        *//*source = URLDecoder.decode(source, "UTF-8");
        cat = URLDecoder.decode(cat, "UTF-8");
        concept = URLDecoder.decode(concept, "UTF-8");
        String uri = Vocabulary.getInstance().getConcept( source, cat, concept );
        if (uri == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else
            return Response.ok(uri).build();*//*

        return( Response.ok().build() );
    }*/

    /*@GET
    @Path( "{source}/{concept}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response findVocabularyConcept( @PathParam( "source" ) String source, @PathParam( "concept" ) String concept ) throws Exception {
        *//*source = URLDecoder.decode(source, "UTF-8");
        concept = URLDecoder.decode(concept, "UTF-8");
        String uri = Vocabulary.getInstance().getConcept( source, concept );
        if (uri == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else
            return (Response.ok(uri).build());*//*

        return( Response.ok().build() );
    }*/

    /*@GET
    @Path( "{source}/{cat}/{concept}/rdf" )
    @Produces( "application/rdf+xml" )
    public Response getVocabularyConceptAsRdf( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "false" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable  ) throws Exception {
        *//*boolean isRdfMetadataInfos = ( "true".equals( rdfMetadataInfos ) );
        boolean isHumanReadable = ( "true".equals( humanReadable ) );
        String infos = source + "#" + cat + "#" + concept;
        String rdf = Vocabulary.getInstance().getVocabularyConceptView().getRdf( infos, incomingLinks, isRdfMetadataInfos, isHumanReadable );
        if( rdf == null )
            return( Response.status( Response.Status.NOT_FOUND ).build() );
        else
            return( Response.ok( rdf ).build() );*//*

        return( Response.ok().build() );
    }*/

    /*@GET
    @Path( "{source}/{cat}/{concept}/incomingLinks/rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getVocabularyConceptIncomingLinksAsRdf( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "25" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        *//*boolean isHumanReadable = ( "true".equals( humanReadable ) );

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

        String vocConceptUri = Core.getInstance().getUriPrefix() + "/voc/" + source + "/" + cat + "/" + concept;

        String links = Core.getInstance().getDefaultView().getIncomingLinks( vocConceptUri, isHumanReadable, offset, limit, format );
        if( "rdf".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( "application/rdf+xml" ).build() );
        if( "json".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( MediaType.APPLICATION_JSON ).build() );
        throw( new WebApplicationException( HttpServletResponse.SC_BAD_REQUEST ) ); // Unsupported format.*//*

        return( Response.ok().build() );
    }*/

    @GET
    @Path( "{id}/{format}" )
    @Produces( MediaType.APPLICATION_XML )
    public Response getVocabularyAsXml( @PathParam( "id" ) String id, @PathParam( "format" ) String format) throws Exception {
        String xml = null;
        try {
            xml = Store.getInstance().getDatastream("/vocabularies/" + id, id + "." + format.toLowerCase());
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
                voc.put( "label", Vocabulary.getInstance().getVocabularyTitle(vocUri, lang, true) );
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


    /*@GET
    @Path( "{uri}/topConcepts" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getVocabularyTopConcepts(  @PathParam( "uri" ) String uri,
                               @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                               @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {

        String[] concepts = Vocabulary.getInstance().getTopConcepts(uri, true);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _concepts = new JSONArray();
            for (String concept : concepts)
                _concepts.put(buildJSONConcept(concept, showIds, null, lang));

            json.key( "concepts" ).value( _concepts );
            json.key( "label" ).value( Vocabulary.getInstance().getVocabularyTitle(uri, lang, false) );
            json.key( "restUrl").value(Vocabulary.getInstance().getRestUrl(uri));

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
    }*/

    /*@GET
    @Path( "{uri}/children" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getVocabularyConceptChildren( @PathParam( "uri" ) String uri,
                                                        @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                                                        @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        String[] children = Vocabulary.getInstance().getChildren(uri);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _concepts = new JSONArray();
            for (String child : children)
                _concepts.put(buildJSONConcept(child, showIds, null, lang));

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

        return( out.toString() );
    }*/

    /*@GET
    @Path( "graphName" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response getVocabularyConceptGraphName( @QueryParam( "uri" ) String uri ) throws Exception {
        String res = Util.getGraphName(uri);
        return Response.ok(res).build();
    }*/

    /*@GET
    @Path( "{uri}/subconcepts" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getVocabularySubConcepts( @PathParam( "uri" ) String uri ) throws Exception {
        String[] subs = Vocabulary.getInstance().getSubConcepts(uri);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _concepts = new JSONArray();
            for (String c : subs)
                _concepts.put(c);

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

        return( out.toString() );
    }*/

    /*@GET
    @Path( "{uri}/hierarchy" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getVocabularyConceptHierarchy( @PathParam( "uri" ) String uri,
                                                 @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                                                 @DefaultValue( "false" ) @QueryParam( "includeScheme" ) boolean includeScheme,
                                                 @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        String[] concepts = Vocabulary.getInstance().getHierarchy(uri);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            JSONArray _concepts = new JSONArray();

            JSONObject top = new JSONObject();
            top.put( "label", Vocabulary.getInstance().getVocabularyTitle(uri, lang, false) );
            String scheme = Vocabulary.getInstance().getConceptScheme(uri);
            top.put( "restUrl", Vocabulary.getInstance().getRestUrl(scheme));
            _concepts.put(top);

            for (String concept : concepts)
                _concepts.put(buildJSONConcept(concept, showIds, false, lang));

            //uri param as last element
            _concepts.put(buildJSONConcept(uri, showIds, null, lang));

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

        return( out.toString() );
    }*/

    //@GET
    //@Path( "{uri}/label" )
    //@Produces( MediaType.APPLICATION_JSON )
    //public String getConceptLabel( @PathParam( "uri" ) String uri, @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
    //    //String[] concepts = Vocabulary.getInstance().getHierarchy(uri);
    //    String label = Vocabulary.getInstance().getConceptLabel( uri, lang );
    //    return( label );
    //    //StringWriter out = new StringWriter();
    //    //try {
    //    //    JSONWriter json = new JSONWriter( out ).object();

    //    //    JSONArray _concepts = new JSONArray();
    //    //    for (String concept : concepts)
    //    //        _concepts.put(buildJSONConcept(concept, showIds, lang));

    //    //    //uri param as last element
    //    //    _concepts.put(buildJSONConcept(uri, showIds, lang));

    //    //    json.key( "concepts" ).value( _concepts );
    //    //    json.endObject();
    //    //}
    //    //catch( JSONException e ) {
    //    //    e.printStackTrace();
    //    //}

    //    //try {
    //    //    out.close();
    //    //}
    //    //catch( IOException e ) {
    //    //    e.printStackTrace();
    //    //}

    //    //return( out.toString() );
    //}

    /*@GET
    @Path( "{uri}/extendedHierarchy" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getVocabularyConceptsHierarchy( @PathParam( "uri" ) String uri,
                                                  @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                                                  @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _all = new JSONArray();

            String[] topConcepts = Vocabulary.getInstance().getTopConcepts(uri, false);
            JSONObject top = new JSONObject();
            JSONArray _concepts = new JSONArray();
            for (String concept : topConcepts)
                _concepts.put(buildJSONConcept(concept, showIds, null, lang));

            top.put( "concepts", _concepts );
            top.put( "label", Vocabulary.getInstance().getVocabularyTitle(uri, lang, false) );
            _all.put(top);

            String[] hierarchy = Vocabulary.getInstance().getHierarchy(uri);
            for (String concept : hierarchy) {
                String[] children = Vocabulary.getInstance().getChildren(concept);

                _concepts = new JSONArray();
                for (String child : children)
                    _concepts.put(buildJSONConcept(child, showIds, false, lang));

                JSONObject _element = buildJSONConcept(concept, showIds, null, lang);
                _element.put( "concepts", _concepts );
                _all.put(_element);
            }

            //uri param as last element
            String[] children = Vocabulary.getInstance().getChildren(uri);
            _concepts = new JSONArray();
            for (String child : children)
                _concepts.put(buildJSONConcept(child, showIds, null, lang));

            JSONObject _element = buildJSONConcept(uri, showIds, null, lang);
            _element.put( "concepts", _concepts );
            _all.put(_element);

            json.key( "all" ).value( _all );
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
    }*/

    /*private JSONObject buildJSONConcept(String uri, String showIds, Boolean isLeaf, String lang) throws Exception{
        TripleStoreService tripleStore = Core.getInstance().getTripleStoreService();
        JSONObject _concept = new JSONObject();
        String label = tripleStore.getResourceLabel(uri, lang, true)[0];
        label = label.trim();
        if (Boolean.parseBoolean(showIds)) {
            char delimiter = '/';
            if (uri.contains("#"))
                delimiter = '#';
            String[] spl = StringUtil.split(uri, delimiter);
            String id = spl[spl.length - 1];
            label = id + "&nbsp;&nbsp;" + label;
        }
        _concept.put( "uri", uri );
        _concept.put( "restUrl", Vocabulary.getInstance().getRestUrl(uri));
        _concept.put( "label", label );
        if (isLeaf == null) {
            String[] children2 = Vocabulary.getInstance().getChildren(uri);
            isLeaf = (children2.length == 0);
        }
        if (isLeaf)
            _concept.put( "leaf", "true" );
        return _concept;
    }*/

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

    /*@GET
    @Path( "search" )
    @Produces( MediaType.APPLICATION_JSON )
    public String searchJson( @QueryParam( "q" ) String terms,
                              @DefaultValue( "false" ) @QueryParam( "showIds" ) String showIds,
                              @DefaultValue( "en" ) @QueryParam( "lang" ) String lang) throws Exception {
        if ("".equals(terms))
            return null;

        boolean showId = Boolean.parseBoolean(showIds);
        Hashtable<String, String>[] results = Core.getInstance().getTripleStoreService().
                getResultsFromGraph("getConcepts.sparql", "voc-fullTextView_" + lang,
                        ca.licef.comete.core.util.Util.formatKeywords(terms));

        Hashtable<String, Boolean> vocNavigable = new Hashtable<String, Boolean>();

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray concepts = new JSONArray();
            for (int i = 0; i < results.length; i++) {
                String uri = results[i].get("s");
                String vocUri = Vocabulary.getInstance().getConceptScheme(uri);

                Boolean isNavigable = vocNavigable.get(vocUri);
                if (isNavigable == null) {
                    Hashtable<String, String>[] res = Core.getInstance().getTripleStoreService().
                            getResults("isVocNavigable.sparql", vocUri);
                    isNavigable = Boolean.parseBoolean(
                            ca.licef.comete.core.util.Util.manageQuotes(res[0].get("navigable")));
                    vocNavigable.put(vocUri, isNavigable);
                }

                if (!isNavigable) //Only show navigable vocabularies
                    continue;

                String vocLabel = Vocabulary.getInstance().getVocabularyTitle(uri, lang, false);
                String conceptLabel = Core.getInstance().getTripleStoreService().
                        getResourceLabel(uri, lang, true)[0];
                if (showId) {
                    String[] spl = StringUtil.split(uri, '/');
                    String id = spl[spl.length - 1];
                    conceptLabel = id + "&nbsp;&nbsp;" + conceptLabel;
                }
                JSONObject concept = new JSONObject();
                concept.put( "uri", uri );
                concept.put( "label", conceptLabel );
                concept.put( "vocLabel", vocLabel);
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

        return( out.toString() );
    }*/

    @Context
    private ServletContext context;
}
