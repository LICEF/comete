package ca.licef.comete.core.resource;

import ca.licef.comete.core.Core;
import com.sun.jersey.spi.resource.Singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
@Path( "/system" )
public class SystemResource {

    @GET
    @Path( "status" )
    @Produces( MediaType.TEXT_PLAIN )
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
