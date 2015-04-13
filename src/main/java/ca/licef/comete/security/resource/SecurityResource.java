package ca.licef.comete.security.resource;

import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import licef.IOUtil;
import licef.Sha1Util;

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
        String sha1 = getAdminPasswordSha1();
        try {
            String hashedPassword = Sha1Util.hash( password );
            if( !hashedPassword.equals( sha1 ) )
                return( Response.ok( "false" ).build() );
        }
        catch( NoSuchAlgorithmException shouldNeverHappen ) {
            return( Response.ok( "false" ).build() );
        }

        HttpSession session = req.getSession( true );
        session.setAttribute( "login", "admin" );
        return( Response.ok( "true" ).build() );
    }

    private String getAdminPasswordSha1() {
        String sha1 = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new InputStreamReader( getClass().getResourceAsStream( "/conf/security/adminPassword.txt" ) ) );
            sha1 = reader.readLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
        finally {
            if( reader != null ) {
                try {
                    reader.close();
                }
                catch( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
        return( sha1 );
    }

}
