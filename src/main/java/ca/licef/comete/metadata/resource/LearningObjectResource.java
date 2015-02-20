package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.util.Constants;
import ca.licef.comete.security.Security;
import ca.licef.comete.metadata.Metadata;
import ca.licef.comete.vocabularies.COMETE;
import com.sun.jersey.spi.resource.Singleton;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Locale;


@Singleton
@Path( "/learningObjects" )
public class LearningObjectResource {
    
    @GET
    @Path( "{id}/html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getLearningObjectAsHtml( @PathParam( "id" ) String id, @DefaultValue( "en" ) @QueryParam( "lang" ) String lang, @DefaultValue( "default" ) @QueryParam( "style" ) String style ) throws Exception {
        Locale locale = ( "fr".equals( lang ) ? Locale.FRENCH : Locale.ENGLISH );
        String loUri = ca.licef.comete.core.util.Util.makeURI(id, COMETE.LearningObject.getURI().toString()/*Constants.OBJ_TYPE_LEARNING_OBJECT*/);
        String html = Metadata.getInstance().getLearningObjectView().getHtml( loUri, locale, style );
        return Response.ok(html).build();
    }

    @GET
    @Path( "{id}/rdf" )
    @Produces( "application/rdf+xml" )
    public Response getLearningObjectAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "incomingLinks" ) String incomingLinks, @DefaultValue( "false" ) @QueryParam( "rdfMetadataInfos" ) String rdfMetadataInfos, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable ) throws Exception {
        boolean isRdfMetadataInfos = ( "true".equals( rdfMetadataInfos ) );
        boolean isHumanReadable = ( "true".equals( humanReadable ) );
        String loUri = ca.licef.comete.core.util.Util.makeURI(id, Constants.OBJ_TYPE_LEARNING_OBJECT);
        //String rdf = Metadata.getInstance().getLearningObjectView().getRdf( loUri, incomingLinks, isRdfMetadataInfos, isHumanReadable );
        String rdf = null;
        return Response.ok(rdf).build();
    }

    @GET
    @Path( "{id}/incomingLinks/rdf" )
    @Produces( { "application/rdf+xml", MediaType.APPLICATION_JSON } )
    public Response getLearningObjectIncomingLinksAsRdf( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "humanReadable" ) String humanReadable, @DefaultValue( "0" ) @QueryParam( "offset" ) String strOffset, @DefaultValue( "25" ) @QueryParam( "limit" ) String strLimit, @DefaultValue( "rdf" ) @QueryParam( "format" ) String format ) throws Exception {
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

        String learningObjectUri = ca.licef.comete.core.util.Util.makeURI(id, Constants.OBJ_TYPE_LEARNING_OBJECT);

        //String links = Core.getInstance().getDefaultView().getIncomingLinks( learningObjectUri, isHumanReadable, offset, limit, format );
        String links = null;
        if( "rdf".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( "application/rdf+xml" ).build() ); 
        if( "json".equals( format ) )
            return( Response.status( HttpServletResponse.SC_OK ).entity( links ).type( MediaType.APPLICATION_JSON ).build() ); 
        throw( new WebApplicationException( HttpServletResponse.SC_BAD_REQUEST ) ); // Unsupported format.
    }

    @DELETE
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response deleteLearningObject( @Context HttpServletRequest request,
                                          @PathParam( "id" ) String id ) throws Exception {

        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete records.").build();

        String loUri = ca.licef.comete.core.util.Util.makeURI(id, Constants.OBJ_TYPE_LEARNING_OBJECT);
        Metadata.getInstance().deleteLearningObject(loUri);
        return (Response.ok().build());
    }

}
