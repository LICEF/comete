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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
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
@Api( value = "HarvestDefinition", description = "Technical data required to harvest a repository." )
public class HarvestDefinitionResource {

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    @ApiOperation( value = "Get all harvest definitions." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "List of harvest definitions with their id, name, and restUrl pointing to details of the definition." )
    } )
    public Response getHarvestDefinitions() throws Exception {
        String[] definitions = Harvester.getInstance().getDefinitions();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray defs = new JSONArray();
            for (String defId : definitions) {
                JSONObject _def = Harvester.getInstance().getDefinition(defId);
                if (_def == null)
                    continue;

                JSONObject def = new JSONObject();
                def.put( "id", defId );
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
    @ApiOperation( value = "Get all the details of a specific harvest definition." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "A set of proerties including the id, name, type, metadataNamespace, adminEmail, url of the endpoint and some actions to perform when harvesting records like isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, and invalidApplProf." )
    } )
    public Response getHarvestDefinition( @PathParam( "id" ) String id ) throws Exception {
        JSONObject def = Harvester.getInstance().getDefinition(id);
        return (Response.ok(def.toString()).build());
    }

    @POST
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Add a new harvest definition.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "success: true|false with error: message if success is false." ),
        @ApiResponse( code = 401, message = "Not authorized to add harvest definition." )
    } )
    public Response addNewHarvestDefinition(
            @Context HttpServletRequest request,
            @ApiParam( value = "Identifier of the harvest definition.", required = true ) @FormParam("id") String id,
            @ApiParam( value = "Name of the harvest definition.  This string is used in the GUI.", required = true ) @FormParam("name") String name,
            @ApiParam( value = "Type of harvesting method.", allowableValues = "HTML, OAI", required = true ) @FormParam("type") String type,
            @ApiParam( value = "Url of the endpoint of the repository.", required = true ) @FormParam("url") String url,
            @ApiParam( value = "Metadata namespace of the metadata records.", required = true ) @FormParam("ns") String ns,
            @ApiParam( value = "Technical contact info of the repository." ) @FormParam("adminEmail") String adminEmail,
            @ApiParam( value = "Whether the harvested records should be marked as Pending for approval by default.", allowableValues = "off, on", required = true ) @DefaultValue( "off" ) @FormParam("isPendingByDefault") String isPendingByDefault,
            @ApiParam( value = "Whether the harvested records should be checked for broken links by default.", allowableValues = "off, on", required = true ) @DefaultValue( "off" ) @FormParam("isCheckingBrokenLink") String isCheckingBrokenLink,
            @ApiParam( value = "Whether the harvested records should be marked when they do not comply to the specified application profile.", allowableValues = "off, on" ) @DefaultValue( "off" ) @FormParam("isCheckingInvalid") String isCheckingInvalid,
            @ApiParam( value = "URI of the application profile to check if isCheckingInvalid is true." ) @FormParam("invalidApplProf") String invalidApplProf,
            @ApiParam( value = "XSLT code that will be applied to harvested records before import." ) @FormParam("xsl") String xsl ) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to add harvest definition.").build();

        String errorMessage = Harvester.getInstance().storeDefinition(id, name, type, url, ns, adminEmail, 
            "on".equals(isPendingByDefault), "on".equals(isCheckingBrokenLink), "on".equals(isCheckingInvalid), invalidApplProf, xsl, false);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key("success").value(errorMessage == null);
            if (errorMessage != null)
                json.key("error").value(errorMessage);
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

        return Response.ok( out.toString() ).build();
    }

    @PUT
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Update an existing harvest definition.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "success: true|false with error: message if success is false." ),
        @ApiResponse( code = 401, message = "Not authorized to update harvest definition." )
    } )
    public Response updateHarvestDefinition(
            @Context HttpServletRequest request,
            @PathParam( "id" ) String id,
            @ApiParam( value = "Name of the harvest definition.  This string is used in the GUI.", required = true ) @FormParam("name") String name,
            @ApiParam( value = "Type of harvesting method.", allowableValues = "HTML, OAI", required = true ) @FormParam("type") String type,
            @ApiParam( value = "Url of the endpoint of the repository.", required = true ) @FormParam("url") String url,
            @ApiParam( value = "Metadata namespace of the metadata records.", required = true ) @FormParam("ns") String ns,
            @ApiParam( value = "Technical contact info of the repository." ) @FormParam("adminEmail") String adminEmail,
            @ApiParam( value = "Whether the harvested records should be marked as Pending for approval by default.", allowableValues = "off, on", required = true ) @DefaultValue( "off" ) @FormParam("isPendingByDefault") String isPendingByDefault,
            @ApiParam( value = "Whether the harvested records should be checked for broken links by default.", allowableValues = "off, on", required = true ) @DefaultValue( "off" ) @FormParam("isCheckingBrokenLink") String isCheckingBrokenLink,
            @ApiParam( value = "Whether the harvested records should be marked when they do not comply to the specified application profile.", allowableValues = "off, on", required = true ) @DefaultValue( "off" ) @FormParam("isCheckingInvalid") String isCheckingInvalid,
            @ApiParam( value = "URI of the application profile to check if isCheckingInvalid is true." ) @FormParam("invalidApplProf") String invalidApplProf,
            @ApiParam( value = "XSLT code that will be applied to harvested records before import." ) @FormParam("xsl") String xsl) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to update harvest definition.").build();

        String errorMessage = Harvester.getInstance().storeDefinition(id, name, type, url, ns, adminEmail, 
            "on".equals( isPendingByDefault ), "on".equals( isCheckingBrokenLink ), "on".equals( isCheckingInvalid ), invalidApplProf, xsl, true);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key("success").value(errorMessage == null);
            if (errorMessage != null)
                json.key("error").value(errorMessage);
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

        return Response.ok( out.toString() ).build();
    }

    @PUT
    @Path( "{id}" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Update an exisintg harvest definition.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "success: true|false with error: message if success is false." ),
        @ApiResponse( code = 401, message = "Not authorized to update harvest definition." )
    } )
    public Response updateHarvestDefinition(@Context HttpServletRequest request,
                                            @PathParam( "id" ) String id,
                                            @FormDataParam("file") InputStream uploadedInputStream,
                                            @FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to update harvest definition.").build();

        Harvester.getInstance().storeDefinition(id, uploadedInputStream, fileDetail);
        return Response.ok("Definition saved.").build();
    }

    @PUT
    @Path( "{id}/xsl" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_PLAIN )
    @ApiOperation( value = "Update the XSLT code that will be applied to harvested records before import.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "success: true|false with error: message if success is false." ),
        @ApiResponse( code = 401, message = "Not authorized to add harvest XSL file." ),
        @ApiResponse( code = 404, message = "The harvest definition is not found." ),
        @ApiResponse( code = 500, message = "An error has occured.  The XSLT code has not been saved properly." )
    } )
    public Response addXsl(@Context HttpServletRequest request,
                           @PathParam( "id" ) String id,
                           @FormDataParam("file") InputStream uploadedInputStream,
                           @FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
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
    @ApiOperation( value = "Delete the details of a specific harvest definition.", notes = "This can only be used by an Administrator." )
    @ApiResponses( value = {
        @ApiResponse( code = 200, message = "Ok" ),
        @ApiResponse( code = 401, message = "Not authorized to delete harvest definition." ),
        @ApiResponse( code = 404, message = "Harvest definition not found." ),
        @ApiResponse( code = 500, message = "An error has occured.  The XSLT code has not been deleted." )
    } )
    public Response deleteHarvestDefinition(@Context HttpServletRequest request,
                                            @PathParam( "id" ) String id) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
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
