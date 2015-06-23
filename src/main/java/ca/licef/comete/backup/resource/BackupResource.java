package ca.licef.comete.backup.resource;

import ca.licef.comete.backup.Backup;
import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path( "/backups" )
public class BackupResource {

    @POST
    @Produces( MediaType.TEXT_PLAIN )
    public Response doBackup(@Context HttpServletRequest request) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to start backup.").build();

        try {
            Backup.getInstance().backup();
        }
        catch( Exception e ) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        return Response.ok("Backup started.").build();
    }

}
