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
    public Response isAuthorized(@Context HttpServletRequest req, @DefaultValue("") @QueryParam("ip") String ip) throws Exception {
        boolean isAuthorized = Security.getInstance().isAuthorized( req );
        return( Response.ok( isAuthorized + "" ).build() );
    }

    @POST
    @Path( "authentication" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response authenticate( @Context HttpServletRequest req, @QueryParam( "password" ) String password ) {
        try {
            boolean ok = Security.getInstance().validatePassword(password);
            if (!ok)
                return( Response.ok( "false" ).build() );
        }
        catch( Exception e ) {
            return( Response.ok( "false" ).build() );
        }

        HttpSession session = req.getSession( true );
        session.setAttribute( "login", "admin" );
        return( Response.ok( "true" ).build() );
    }

    @GET
    @Path( "logout" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response logout( @Context HttpServletRequest req ) {
        HttpSession session = req.getSession();
        if( session != null )
            session.invalidate();
        return( Response.ok().build() );
    }

}
