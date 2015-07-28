package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.Settings;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.json.JSONWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Path( "/settings" )
@Api( value = "Settings", description = "General system settings." )
public class SettingsResource {

    @GET
    @Path( "/validatedApplicationProfiles" )
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "Get the validated application profiles when importing records." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of application profile uris with a boolean value indicating which ones are valided for compliance when importing records." )
    } )
    public Response getValidatedApplicationProfiles() throws Exception {
        StringWriter out = new StringWriter();
        try {
            Map<String,Boolean> applProfTable = Settings.getValidatedApplicationProfiles();

            JSONWriter json = new JSONWriter( out );
            json.object();
            for( String applProf : applProfTable.keySet() )
                json.key( applProf ).value( applProfTable.get( applProf ) );
            json.endObject();
        }
        finally {
            out.close();
        }

        return Response.ok(out.toString()).build();
    }

    @PUT
    @Path( "/validatedApplicationProfiles" )
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Set the validated application profiles when importing records.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "Empty body." ),
        @ApiResponse( code = 401, message = "Not authorized to update validated application profiles." )
    } )
    public Response setValidatedApplicationProfiles( 
            @Context HttpServletRequest request,
            @ApiParam( value = "LOM Strict appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_LOM_STRICT ) String lomStrict, 
            @ApiParam( value = "LOM Looset appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_LOM_LOOSE ) String lomLoose,
            @ApiParam( value = "LOM FR 1.0 appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_LOM_FR_1_0 ) String lomFr,
            @ApiParam( value = "SCO LOM FR 1.0 appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_SCO_LOM_FR_1_0 ) String scoLomFr_0_1,
            @ApiParam( value = "SCO LOM FR 1.1 appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_SCO_LOM_FR_1_1 ) String scoLomFr_1_1,
            @ApiParam( value = "LOM Normetic 1.2 appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_LOM_NORMETIC_1_2 ) String lomNormetic_1_2,
            @ApiParam( value = "OAI DC appl. profile", allowableValues = "false,true", required = true ) @FormParam( Constants.APPL_PROF_OAI_DC ) String oaidc 
                ) throws Exception {

        if( !Security.getInstance().isAuthorized( request ) )
            return( Response.status( Response.Status.UNAUTHORIZED ).entity( "Not authorized to update validated application profiles." ).build() );

        Map<String,Boolean> applProfTable = new HashMap<String,Boolean>();
        applProfTable.put( Constants.APPL_PROF_LOM_STRICT, Boolean.valueOf( lomStrict ) );
        applProfTable.put( Constants.APPL_PROF_LOM_LOOSE, Boolean.valueOf( lomLoose ) );
        applProfTable.put( Constants.APPL_PROF_LOM_FR_1_0, Boolean.valueOf( lomFr ) );
        applProfTable.put( Constants.APPL_PROF_SCO_LOM_FR_1_0, Boolean.valueOf( scoLomFr_0_1 ) );
        applProfTable.put( Constants.APPL_PROF_SCO_LOM_FR_1_1, Boolean.valueOf( scoLomFr_1_1 ) );
        applProfTable.put( Constants.APPL_PROF_LOM_NORMETIC_1_2, Boolean.valueOf( lomNormetic_1_2 ) );
        applProfTable.put( Constants.APPL_PROF_OAI_DC, Boolean.valueOf( oaidc ) );
        Settings.setValidatedApplicationProfiles( applProfTable );
        return Response.ok().build();
    }

    @GET
    @Path( "/notifications" )
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "Get the notification settings.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "A list of name-value pairs including notifEmail, brokenLinkValidationCompletionNotif, and harvestCompletionNotif." ),
        @ApiResponse( code = 401, message = "Not authorized to read notification settings." )
    } )
    public Response getNofificationSettings( @Context HttpServletRequest request ) throws Exception {
        if( !Security.getInstance().isAuthorized( request ) )
            return( Response.status( Response.Status.UNAUTHORIZED ).entity( "Not authorized to read notification settings." ).build() );

        Object[] settings = Settings.getNotificationSettings();
        String notifEmail = (String)settings[ 0 ];
        boolean brokenLinkValidationCompletionNotif = (boolean)settings[ 1 ];
        boolean harvestCompletionNotif = (boolean)settings[ 2 ];

        StringWriter out = new StringWriter();
        JSONWriter json = new JSONWriter( out );
       
        json.object()
            .key( "notifEmail" ).value( notifEmail == null ? "" : notifEmail )
            .key( "brokenLinkValidationCompletionNotif" ).value( brokenLinkValidationCompletionNotif )
            .key( "harvestCompletionNotif" ).value( harvestCompletionNotif );
        json.endObject();

        out.close();

        return Response.ok(out.toString()).build();
    }

    @POST
    @Path( "/notifications" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Set the notification settings.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "Notification settings saved." ),
        @ApiResponse( code = 401, message = "Not authorized to write notification settings." )
    } )
    public Response setNotificationSettings( @Context HttpServletRequest request, 
            @ApiParam( value = "Email address where the notification are delivered." ) @FormParam( "notifEmail" ) String notifEmail, 
            @ApiParam( value = "Whether a notification is sent after the broken link check is done.", allowableValues = "false,true", required = true ) @FormParam( "brokenLinkValidationCompletionNotif" ) String brokenLinkValidationCompletionNotif,
            @ApiParam( value = "Whether a notification is sent after the completion of a repository harvesting task.", allowableValues = "false,true", required = true ) @FormParam( "harvestCompletionNotif" ) String harvestCompletionNotif ) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to write notification settings.").build();

        Settings.setNotificationSettings( notifEmail, "true".equals( brokenLinkValidationCompletionNotif ), "true".equals( harvestCompletionNotif ) );

        return( Response.ok( "Notification settings saved." ).build() );
    }

}
