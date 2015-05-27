package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.util.Constants;
import ca.licef.comete.security.Security;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.Metadata;
import com.sun.jersey.spi.resource.Singleton;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Singleton
@Path( "/harvestedRecords" )
public class HarvestedRecordResource {
    
    @PUT
    @Path( "{oaiID}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response addOrUpdateHarvestedRecord( @Context HttpServletRequest request,
                                        @PathParam( "oaiID" ) String oaiID,
                                        @QueryParam( "namespace" ) String namespace,
                                        @QueryParam( "datestamp" ) String datestamp,
                                        @QueryParam( "repoId" ) String repoId,
                                        String record ) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to post records.").build();

        String repoUri = Util.makeURI( repoId, Constants.OBJ_TYPE_REPOSITORY );
        String res = Metadata.getInstance().storeHarvestedRecord( oaiID, namespace, repoUri, record, datestamp, false, false );
        return (Response.ok(res).build());
    }

    @DELETE
    @Path( "{oaiID}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response deleteHarvestedRecord( @Context HttpServletRequest request,
                                           @PathParam( "oaiID" ) String oaiID,
                                           @QueryParam( "namespace" ) String namespace ) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete records.").build();

        String res = Metadata.getInstance().deleteHarvestedRecord( oaiID, namespace );
        return (Response.ok(res).build());
    }

}
