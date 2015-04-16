package ca.licef.comete.security.resource;

import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;
import org.json.JSONException;
import org.json.JSONWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
@Path( "/security" )
public class SecurityResource {

    @GET
    @Path( "role" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response getRole(@Context HttpServletRequest req) throws Exception {
        String role = Security.getInstance().getRole(req);
        return( Response.ok( role ).build() );
    }

    @POST
    @Path( "authentication" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response authenticate( @Context HttpServletRequest req, @FormParam( "login" ) String login, @FormParam( "password" ) String password ) {
        String role;
        try {
            role = Security.getInstance().authenticate(login, password);
            if (role == null)
                return( Response.status( Response.Status.UNAUTHORIZED ).entity( "Authentication failed.").build() );
        }
        catch( Exception e ) {
            return( Response.status( Response.Status.UNAUTHORIZED ).entity("Authentication failed.").build() );
        }

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key("success").value(true);
            json.key("role").value(role);
            json.endObject();
        }
        catch( JSONException e ) {
            e.printStackTrace();
        }

        try {
            out.close();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }

        HttpSession session = req.getSession( true );
        session.setAttribute( "role", role );
        return Response.ok( out.toString() ).build();
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
