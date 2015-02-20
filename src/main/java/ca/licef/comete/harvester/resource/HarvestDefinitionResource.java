package ca.licef.comete.harvester.resource;

import ca.licef.comete.harvester.Harvester;
import ca.licef.comete.security.Security;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;


@Singleton
@Path( "/harvestDefinitions" )
public class HarvestDefinitionResource {

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getHarvestDefinitions() throws Exception {
        String[] definitions = Harvester.getInstance().getDefinitions();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray defs = new JSONArray();
            for (String defId : definitions) {
                JSONObject _def = Harvester.getInstance().getDefinition(defId);
                JSONObject def = new JSONObject();
                def.put( "restUrl", "rest/harvestDefinitions/" + defId );
                def.put( "name", _def.get("name") );
                defs.put(def);
            }
            json.key("harvestDefs").value(defs);

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

    @GET
    @Path( "{id}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getHarvestDefinition( @PathParam( "id" ) String id ) throws Exception {
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONObject _def = Harvester.getInstance().getDefinition(id);
            json.key( "id" ).value( id );
            json.key( "name" ).value( _def.get("name") );
            json.key( "adminEmail" ).value( _def.get("adminEmail") );
            json.key( "type" ).value( _def.get("type") );
            json.key( "url" ).value( _def.get("url") );
            json.key( "metadataNamespace" ).value( _def.get("metadataNamespace") );

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

    @PUT
    @Path( "{id}" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_PLAIN )
    public Response storeHarvestDefinition(@Context HttpServletRequest request,
                                  @PathParam( "id" ) String id,
                                  @FormDataParam("file") InputStream uploadedInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to add harvest definition.").build();

        Harvester.getInstance().storeDefinition(id, uploadedInputStream, fileDetail);
        return Response.ok("Definition saved.").build();
    }

    @PUT
    @Path( "{id}/xsl" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_PLAIN )
    public Response addXsl(@Context HttpServletRequest request,
                                  @PathParam( "id" ) String id,
                                  @FormDataParam("file") InputStream uploadedInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to add harvest XSL file.").build();

        try {
            Harvester.getInstance().storeXsl(id, uploadedInputStream, fileDetail);
            return Response.ok("XSL file saved.").build();
        }
        catch (Exception e) {
            if( "Harvest definition not found.".equals( e.getMessage() ) )
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            else
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response deleteHarvestDefinition(@Context HttpServletRequest request,
                                     @PathParam( "id" ) String id) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete harvest definition.").build();

        try {
            Harvester.getInstance().removeDefinition(id);
            return Response.ok().build();
        }
        catch (Exception e) {
            if( "Harvest definition not found.".equals( e.getMessage() ) )
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            else
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
