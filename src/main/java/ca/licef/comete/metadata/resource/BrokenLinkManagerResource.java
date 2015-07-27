package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.Core;
import ca.licef.comete.security.Security;
import ca.licef.comete.metadata.BrokenLinkChecker;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.Date;

@Singleton
@Path( "/brokenLinkManager" )
@Api( value = "BrokenLinkManager" )
public class BrokenLinkManagerResource {

    @GET
    @Path( "/verification" )
    @Produces( MediaType.TEXT_HTML )
    public Response getVerification() throws Exception {
        if( !BrokenLinkChecker.getInstance().isRunning() )
            return( Response.status( Response.Status.NOT_FOUND ).entity( "No verification in progress." ).build() );
        else {
            int progress = BrokenLinkChecker.getInstance().getProgress();
            return( Response.status( Response.Status.OK ).entity( "Verification in progress: " + progress + "%." ).build() );
        }
    }

    @POST
    @Path( "/verification" )
    @Produces( MediaType.TEXT_HTML )
    public Response startVerification( @Context HttpServletRequest request, @FormParam( "setBrokenLinkFlag" ) String setBrokenLinkFlag ) throws Exception {
        if( !Security.getInstance().isAuthorized( request ) )
            return( Response.status( Response.Status.UNAUTHORIZED ).entity( "Not authorized to launch broken links verification." ).build() );

        if( BrokenLinkChecker.getInstance().isRunning() )
            return( Response.status( 503 /* Service unavailable */ ).entity( "The Broken Link Manager is already in the process of validating broken links." ).build() );

        try {
            BrokenLinkChecker.getInstance().start( "true".equals( setBrokenLinkFlag ) );

            return Response.ok( "Verification started on " + (new Date()) ).build();
        }
        catch( Exception e ) {
            return( Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( "An error occurred when retrieving the links of the resources: " + e ).build() );
        }
    }

    @GET
    @Path( "/report" )
    @Produces( MediaType.TEXT_HTML )
    public Response getReport( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        if( BrokenLinkChecker.getInstance().isReportAvailable( lang ) ) {
            StringWriter str = new StringWriter();
            BufferedWriter output = null;
            BufferedReader input = null;
            try {
                output = new BufferedWriter( str );
                input = new BufferedReader( new FileReader( BrokenLinkChecker.getInstance().getReport( lang ) ) );

                char[] buffer = new char[ 4096 ];
                int readChars = 0;
                while( -1 != ( readChars = input.read( buffer ) ) ) {
                    output.write( buffer, 0, readChars );
                }
            }
            finally {
                if( output != null ) {
                    output.flush();
                    output.close();
                }
                if( input != null )
                    input.close();
            }
            return( Response.ok( str.toString() ).build() );
        }
        else 
            return( Response.status( Response.Status.NOT_FOUND ).entity( "No report available." ).build() );
    }

    @HEAD
    @Path( "/report" )
    @Produces( MediaType.TEXT_HTML )
    public Response getReportAvailability( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        if( BrokenLinkChecker.getInstance().isReportAvailable( lang ) ) {
            String reportLocation = Core.getInstance().getCometeUrl() + "/rest/brokenLinkManager/report?lang="+lang;
            return( Response.status( Response.Status.OK ).header( "report-location", reportLocation ).build() );
        }
        else 
            return( Response.status( Response.Status.NOT_FOUND ).build() );
    }

}
