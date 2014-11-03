package ca.licef.comete.harvester.resource;

import ca.licef.comete.core.util.Security;
import ca.licef.comete.harvester.Harvester;
import com.sun.jersey.spi.resource.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
@Path( "/harvests" )
public class HarvestResource {

    @GET
    @Produces( MediaType.TEXT_PLAIN )
    public Response getHarvests() throws Exception {
        String[] harvests = Harvester.getInstance().getHarvests();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _harvests = new JSONArray();
            for (int i = 0; i < harvests.length; i++)
                _harvests.put( harvests[i] );
            json.key("harvests").value(_harvests);

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
        return (Response.ok(out.toString()).build());
    }

    @POST
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response startHarvest(@Context HttpServletRequest request,
                                  @PathParam( "id" ) String id, @QueryParam( "from" ) String from) throws Exception {
        if (!Security.isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to start harvest.").build();
        
        try {
            String harvestId = Harvester.getInstance().startHarvest(id, from);
            return Response.ok(harvestId).build();
        }
        catch( Exception e ) {
            if( "Harvest definition not found.".equals( e.getMessage() ) )
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            else
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response stopHarvest(@Context HttpServletRequest request,
                                @PathParam( "id" ) String id) throws Exception {
        if (!Security.isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to stop harvest.").build();

        try {
            Harvester.getInstance().stopHarvest( id );
        }
        catch( Exception e ) {
            if( "Harvest not found.".equals( e.getMessage() ) )
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            else
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok("Harvest " + id + " stopped.").build();
    }

}
