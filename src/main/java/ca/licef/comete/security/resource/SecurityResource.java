package ca.licef.comete.security.resource;

import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path( "/security" )
public class SecurityResource {

    @GET
    @Path( "isAuthorized" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response isAuthorized(@Context HttpServletRequest request, @DefaultValue("") @QueryParam("ip") String ip) throws Exception {
        if ("".equals(ip))
            ip = request.getRemoteAddr();
        String res = Boolean.toString(Security.getInstance().isAuthorized(ip));
        return Response.ok(res).build();
    }

    @POST
    @Path( "authentication" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response authenticate( @Context HttpServletRequest req, @QueryParam( "password" ) String password ) {
        if( !"toto".equals( password ) )
            return( Response.ok( "false" ).build() );

        HttpSession session = req.getSession( true );
        session.setAttribute( "login", "admin" );
        return( Response.ok( "true" ).build() );
    }

}
