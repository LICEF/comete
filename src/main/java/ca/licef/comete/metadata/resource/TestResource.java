package ca.licef.comete.metadata.resource;

import com.sun.jersey.spi.resource.Singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by amiara on 2014-10-28.
 */
@Singleton
@Path( "Test" )
public class TestResource {

    @GET
    @Path( "test1" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response test() throws Exception {
        return Response.ok( "It works!" ).build();
    }
}
