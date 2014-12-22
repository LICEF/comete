package ca.licef.comete.linkeddata.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.resource.Singleton;
import licef.tsapi.vocabulary.SKOS;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@Singleton
@Path( "/" )
public class URIResolver {

    @GET
    @Path("test/")
    @Produces( MediaType.TEXT_PLAIN )
    public String test() {
        return "Test LinkedData REST service";
    }

    @GET
    @Path( "person/{id}" )
    public Response getPerson( @PathParam( "id" ) String id, @Context HttpServletRequest req ) throws Exception {
        return( getResource( id, Constants.OBJ_TYPE_PERSON, req ) );
    }

    @GET
    @Path( "person/{id}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getPersonAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        return( getResourceAsRdf( id, Constants.OBJ_TYPE_PERSON, incomingLinks, rdfMetadataInfos, humanReadable ) );
    }

    @GET
    @Path( "person/{id}/incomingLinks.rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getPersonIncomingLinks( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        return( getResourceIncomingLinksAsRdf( id, Constants.OBJ_TYPE_PERSON, humanReadable, strOffset, strLimit, format ) );
    }

    @GET
    @Path( "person/{id}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getPersonRdfAsHtml( @PathParam( "id" ) String id ) throws Exception {
        Response resp = getPersonAsRdf( id, "firstPage", "false", "true" );

        String uri = Util.makeURI( id, Constants.OBJ_TYPE_PERSON );
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    @GET
    @Path( "organization/{id}" )
    public Response getOrganization( @PathParam( "id" ) String id, @Context HttpServletRequest req ) throws Exception {
        return( getResource( id, Constants.OBJ_TYPE_ORGANIZATION, req ) );
    }

    @GET
    @Path( "organization/{id}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getOrganizationAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        return( getResourceAsRdf( id, Constants.OBJ_TYPE_ORGANIZATION, incomingLinks, rdfMetadataInfos, humanReadable ) );
    }

    @GET
    @Path( "organization/{id}/incomingLinks.rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getOrganizationIncomingLinks( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        return( getResourceIncomingLinksAsRdf( id, Constants.OBJ_TYPE_ORGANIZATION, humanReadable, strOffset, strLimit, format ) );
    }

    @GET
    @Path( "organization/{id}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getOrganizationRdfAsHtml( @PathParam( "id" ) String id ) throws Exception {
        Response resp = getOrganizationAsRdf( id, "firstPage", "false", "true" );

        String uri = Util.makeURI( id, Constants.OBJ_TYPE_ORGANIZATION );
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    @GET
    @Path( "learningobject/{id}" )
    public Response getLearningObject( @PathParam( "id" ) String id, @Context HttpServletRequest req ) throws Exception {
        return( getResource( id, Constants.OBJ_TYPE_LEARNING_OBJECT, req ) );
    }

    @GET
    @Path( "learningobject/{id}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getLearningObjectAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        return( getResourceAsRdf( id, Constants.OBJ_TYPE_LEARNING_OBJECT, incomingLinks, rdfMetadataInfos, humanReadable ) );
    }

    @GET
    @Path( "learningobject/{id}/incomingLinks.rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getLearningObjectIncomingLinks( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        return( getResourceIncomingLinksAsRdf( id, Constants.OBJ_TYPE_LEARNING_OBJECT, humanReadable, strOffset, strLimit, format ) );
    }

    @GET
    @Path( "learningobject/{id}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getLearningObjectRdfAsHtml( @PathParam( "id" ) String id ) throws Exception {
        Response resp = getLearningObjectAsRdf( id, "firstPage", "false", "true" );

        String uri = Util.makeURI( id, Constants.OBJ_TYPE_LEARNING_OBJECT );
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    @GET
    @Path( "metadatarecord/{id}" )
    public Response getMetadataRecord( @PathParam( "id" ) String id, @Context HttpServletRequest req ) throws Exception {
        return( getResource( id, Constants.OBJ_TYPE_METADATA_RECORD, req ) );
    }

    @GET
    @Path( "metadatarecord/{id}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getMetadataRecordAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        return( getResourceAsRdf( id, Constants.OBJ_TYPE_METADATA_RECORD, incomingLinks, rdfMetadataInfos, humanReadable ) );
    }

    @GET
    @Path( "metadatarecord/{id}/incomingLinks.rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getMetadataRecordIncomingLinks( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        return( getResourceIncomingLinksAsRdf( id, Constants.OBJ_TYPE_METADATA_RECORD, humanReadable, strOffset, strLimit, format ) );
    }

    @GET
    @Path( "metadatarecord/{id}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getMetadataRecordRdfAsHtml( @PathParam( "id" ) String id ) throws Exception {
        Response resp = getMetadataRecordAsRdf( id, "firstPage", "false", "true" );

        String uri = Util.makeURI( id, Constants.OBJ_TYPE_METADATA_RECORD );
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    @GET
    @Path( "repository/{id}" )
    public Response getRepository( @PathParam( "id" ) String id, @Context HttpServletRequest req ) throws Exception {
        return( getResource( id, Constants.OBJ_TYPE_REPOSITORY, req ) );
    }

    @GET
    @Path( "repository/{id}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getRepositoryAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        return( getResourceAsRdf( id, Constants.OBJ_TYPE_REPOSITORY, incomingLinks, rdfMetadataInfos, humanReadable ) );
    }

    @GET
    @Path( "repository/{id}/incomingLinks.rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getRepositoryIncomingLinks( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        return( getResourceIncomingLinksAsRdf( id, Constants.OBJ_TYPE_REPOSITORY, humanReadable, strOffset, strLimit, format ) );
    }

    @GET
    @Path( "repository/{id}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getRepositoryRdfAsHtml( @PathParam( "id" ) String id ) throws Exception {
        Response resp = getRepositoryAsRdf( id, "firstPage", "false", "true" );

        String uri = Util.makeURI( id, Constants.OBJ_TYPE_REPOSITORY);
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    @GET
    @Path( "voccontext/{id}" )
    public Response getVocContext( @PathParam( "id" ) String id, @Context HttpServletRequest req ) throws Exception {
        return( getResource( id, Constants.OBJ_TYPE_VOC_CONTEXT, req ) );
    }

    @GET
    @Path( "voccontext/{id}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getVocContextAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        return( getResourceAsRdf( id, Constants.OBJ_TYPE_VOC_CONTEXT, incomingLinks, rdfMetadataInfos, humanReadable ) );
    }

    @GET
    @Path( "voccontext/{id}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getVocContextRdfAsHtml( @PathParam( "id" ) String id ) throws Exception {
        Response resp = getVocContextAsRdf( id, "true", "false", "false" );

        String uri = Util.makeURI( id, Constants.OBJ_TYPE_VOC_CONTEXT );
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    @GET
    @Path( "voc/{source}/{cat}" )
    public Response getVocabulary( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @Context HttpServletRequest req ) throws Exception {
        //whole vocabulary case : force to return only rdf 
        String uri = Core.getInstance().getUriPrefix() + "/voc/" + source + "/" + cat;
        URI seeOther = new URI( uri + ".rdf" );
        return Response.seeOther( seeOther ).build();
    }

    @GET
    @Path( "voc/{source}/{cat}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getVocabularyAsRdf( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat ) throws Exception {
        String uri = Core.getInstance().getUriPrefix() + "voc/" + source + "/" + cat;
        String url = Util.getRestUrl(SKOS.ConceptScheme) + "/" + source + "/" + cat + "/rdf";
        return( getResourceAsRdfWithRestUrl( url, "false", "false", "false", uri ) );
    }

    @GET
    @Path( "voc/{source}/{cat}/{concept}" )
    public Response getVocabulary( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept, @Context HttpServletRequest req ) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll("/", "%2F");
        String uri = Core.getInstance().getUriPrefix() + "/voc/" + source + "/" + cat + "/" + concept;
        return getResourceWithUri( uri, req );
    }

    @GET
    @Path( "voc/{source}/{cat}/{concept}.rdf" )
    @Produces( "application/rdf+xml" )
    public Response getVocabularyAsRdf( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "true" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll("/", "%2F");
        String url = Util.getRestUrl(SKOS.ConceptScheme) + "/" + source + "/" + cat + "/" + concept + "/rdf";
        String uri = Core.getInstance().getUriPrefix() + "/voc/" + source + "/" + cat + "/" + concept;
        return( getResourceAsRdfWithRestUrl( url, incomingLinks, rdfMetadataInfos, humanReadable, uri ) );
    }

    @GET
    @Path( "voc/{source}/{cat}/{concept}/incomingLinks.rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getVocabularyIncomingLinks( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll("/", "%2F");
        String url = Util.getRestUrl(SKOS.ConceptScheme) + "/" + source + "/" + cat + "/" + concept + "/incomingLinks/rdf";
        String uri = Core.getInstance().getUriPrefix() + "/voc/" + source + "/" + cat + "/" + concept;
        return( getResourceIncomingLinksAsRdfWithRestUrl( url, humanReadable, uri, strOffset, strLimit, format ) );
    }

    @GET
    @Path( "voc/{source}/{cat}/{concept}.html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getVocabularyAsHtml( @PathParam( "source" ) String source, @PathParam( "cat" ) String cat, @PathParam( "concept" ) String concept ) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll("/", "%2F");
        Response resp = getVocabularyAsRdf( source, cat, concept, "firstPage", "false", "true" );

        String uri = Core.getInstance().getUriPrefix() + "/voc/" + source + "/" + cat + "/" + concept;
        String rdf = (String)resp.getEntity();
        return( getResourceAsHtml( uri, rdf ) );
    }

    private HashMap<String,Float> getMediaTypeWeights( String acceptHeader ) {
        String[] mediaTypes = acceptHeader.split( "," );
        HashMap<String,Float> typeWeights = new HashMap<String,Float>();
        for( int i = 0; i < mediaTypes.length; i++ ) {
            String[] info = mediaTypes[ i ].split( ";" );
            String type = info[ 0 ];

            String weightStr = "1.0";
            if( info.length > 1 ) {
                String weightStrTmp = info[ 1 ];
                String[] weightInfo = weightStrTmp.split( "=" );
                if( weightInfo.length > 1 )
                    weightStr = weightInfo[ 1 ];
                else
                    weightStr = "0.0"; // I don't think this case is possible but just in case. - FB
            }

            try {
                typeWeights.put( type, new Float( weightStr ) );
            }
            catch( NumberFormatException e ) {
                e.printStackTrace();
            }
        }

        return( typeWeights );
    }

    private Response getResource( String id, String type, HttpServletRequest req ) throws Exception {
        String uri = Util.makeURI(id, type);
        return getResourceWithUri(uri, req);
    }

    private Response getResourceWithUri( String uri, HttpServletRequest req ) throws Exception {
        String seeOtherStr = uri + ".html";
        String acceptHeader = req.getHeader( "accept" );
        if( acceptHeader != null ) {
            HashMap<String,Float> typeWeights = getMediaTypeWeights( acceptHeader );
            Float rdfWeight = typeWeights.containsKey( "application/rdf+xml" ) ? new Float( typeWeights.get( "application/rdf+xml" ) ) : new Float( 0.0 );
            Float htmlWeight = typeWeights.containsKey( "text/html" ) ? new Float( typeWeights.get( "text/html" ) ) : new Float( 0.0 );
            if( rdfWeight.floatValue() > htmlWeight.floatValue() )
                seeOtherStr = uri + ".rdf";
        }

        URI seeOther = null;
        try {
             seeOther = new URI( seeOtherStr );
        } 
        catch( URISyntaxException e ) {
            e.printStackTrace();
        }

        return( Response.seeOther( seeOther ).build() );
    }

    private Response getResourceAsRdf( String id, String type, String incomingLinks, String rdfMetadataInfos, String humanReadable ) throws Exception {
        String url = Util.getRestUrl( type ) + "/" + id + "/rdf";
        String uri = Util.makeURI( id, type );
        return getResourceAsRdfWithRestUrl(url, incomingLinks, rdfMetadataInfos, humanReadable, uri);
    }

    private Response getResourceIncomingLinksAsRdf( String id, String type, String humanReadable, String strOffset, String strLimit, String format ) throws Exception {
        String url = Util.getRestUrl( type ) + "/" + id + "/incomingLinks/rdf";
        String uri = Util.makeURI( id, type );
        return getResourceIncomingLinksAsRdfWithRestUrl(url, humanReadable, uri, strOffset, strLimit, format);
    }

    private Response getResourceAsRdfWithRestUrl( String url, String incomingLinks, String rdfMetadataInfos, String humanReadable, String uri ) throws Exception {
        //WebResource webResource = Core.getInstance().getRestClient().resource( url );
        //MultivaluedMap queryParams = new MultivaluedMapImpl();
        //queryParams.add( "incomingLinks", incomingLinks );
        //queryParams.add( "rdfMetadataInfos", rdfMetadataInfos );
        //queryParams.add( "humanReadable", humanReadable );
        //ClientResponse response = webResource.queryParams( queryParams ).accept( "application/rdf+xml" ).get( ClientResponse.class );
        //String rdf = null;
        //int status = response.getStatus();
        //if( status == HttpServletResponse.SC_OK ) {
        //    rdf = response.getEntity( String.class );
        //    status = ( Core.getInstance().getTripleStore().isResourceExists( uri ) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND );
        //}
        //Response resp = Response.created( new URI( uri ) ).entity( rdf ).status( status ).build();
        //return( resp );
        Response resp = Response.created( new URI( uri ) ).entity( "TODO" ).status( HttpServletResponse.SC_OK ).build();
        return( resp );
    }

    private Response getResourceIncomingLinksAsRdfWithRestUrl( String url, String humanReadable, String uri, String strOffset, String strLimit, String format ) throws Exception {
        //WebResource webResource = Core.getInstance().getRestClient().resource( url );
        //MultivaluedMap queryParams = new MultivaluedMapImpl();
        //queryParams.add( "humanReadable", humanReadable );
        //queryParams.add( "offset", strOffset );
        //queryParams.add( "limit", strLimit );
        //queryParams.add( "format", format );

        //String acceptedFormat = null;
        //if( "rdf".equals( format ) )
        //    acceptedFormat = "application/rdf+xml";
        //else if( "json".equals( format ) )
        //    acceptedFormat = MediaType.APPLICATION_JSON;

        //ClientResponse response = webResource.queryParams( queryParams ).accept( acceptedFormat ).get( ClientResponse.class );
        //String links = null;
        //int status = response.getStatus();
        //if( status == HttpServletResponse.SC_OK ) {
        //    links = response.getEntity( String.class );
        //    if( Constants.OBJ_TYPE_VOCABULARY_CONCEPT.equals( Util.getURIType( uri ) ) )
        //        status = HttpServletResponse.SC_OK;
        //    else
        //        status = ( Core.getInstance().getTripleStore().isResourceExists( uri ) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND );
        //}
        //Response resp = Response.created( new URI( uri ) ).entity( links ).type( acceptedFormat ).status( status ).build();
        //return( resp );
        Response resp = Response.created( new URI( uri ) ).entity( "TODO" ).status( HttpServletResponse.SC_OK ).build();
        return( resp );
    }

    private Response getResourceAsHtml( String uri, String rdf ) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put( "uri", uri );
        params.put( "ldres", Core.getInstance().getCometeUrl() + "/ld_res");

        StreamSource source = new StreamSource( new BufferedReader( new StringReader( rdf ) ) );
        String html = Util.applyXslToDocument( "convertRdfToHtml", source, params );
        
        int status = ( Core.getInstance().getTripleStore().isResourceExists( uri ) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND );
        Response resp = Response.created( new URI( uri ) ).entity( html ).status( status ).build();
        return( resp );
    }

}
