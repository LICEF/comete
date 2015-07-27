package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.Settings;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
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
@Api( value = "Settings" )
public class SettingsResource {

    @GET
    @Path( "/validatedApplicationProfiles" )
    @Produces( MediaType.APPLICATION_JSON )
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
    public Response setValidatedApplicationProfiles( 
        @Context HttpServletRequest request,
        @FormParam( Constants.APPL_PROF_LOM_STRICT ) String lomStrict, 
        @FormParam( Constants.APPL_PROF_LOM_LOOSE ) String lomLoose,
        @FormParam( Constants.APPL_PROF_LOM_FR_1_0 ) String lomFr,
        @FormParam( Constants.APPL_PROF_SCO_LOM_FR_1_0 ) String scoLomFr_0_1,
        @FormParam( Constants.APPL_PROF_SCO_LOM_FR_1_1 ) String scoLomFr_1_1,
        @FormParam( Constants.APPL_PROF_LOM_NORMETIC_1_2 ) String lomNormetic_1_2,
        @FormParam( Constants.APPL_PROF_OAI_DC ) String oaidc 
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
    public Response setNotificationSettings( @Context HttpServletRequest request, 
        @FormParam( "notifEmail" ) String notifEmail, @FormParam( "brokenLinkValidationCompletionNotif" ) String brokenLinkValidationCompletionNotif,
            @FormParam( "harvestCompletionNotif" ) String harvestCompletionNotif ) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to write notification settings.").build();

        Settings.setNotificationSettings( notifEmail, "true".equals( brokenLinkValidationCompletionNotif ), "true".equals( harvestCompletionNotif ) );

        return( Response.ok( "Notification settings saved." ).build() );
    }

}
