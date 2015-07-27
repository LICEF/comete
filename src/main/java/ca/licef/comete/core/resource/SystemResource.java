package ca.licef.comete.core.resource;

import ca.licef.comete.core.Core;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
@Path( "/system" )
@Api( value = "System", description = "General services of the system." )
public class SystemResource {

    @GET
    @Path( "status" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Indicates whether the system is ready to be used or if it's in maintenance mode." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "ok when ready or maintenance message." ) 
    } )
    public Response isReady() throws Exception {
        StringWriter out = new StringWriter();
        out.write(Core.getInstance().serverStatus());

        try {
            out.close();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }

        return Response.ok(out.toString()).build();
    }
}
