package ca.licef.comete.harvester.resource;

import ca.licef.comete.harvester.Harvester;
import ca.licef.comete.security.Security;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
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
import java.io.StringWriter;

@Singleton
@Path( "/harvestReports" )
@Api( value = "HarvestReport" )
public class HarvestReportResource {

    @GET
    @Path( "{id}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getHarvestReports(@PathParam( "id" ) String id) throws Exception {
        String[] reports = Harvester.getInstance().getHarvestReports(id);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray _reports = new JSONArray();
            for (int i = 0; i < reports.length; i++) {
                JSONObject report = new JSONObject();
                report.put( "restUrl", "rest/harvestReports/" + id + "/" + reports[i] );
                report.put( "name", reports[i].replace("T", " @ ") );
                _reports.put(report);
            }
            json.key("harvestReports").value(_reports);

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
    @Path( "{id}/{date}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response getHarvestReport(@PathParam( "id" ) String id, @PathParam( "date" ) String date) throws Exception {
        String report = Harvester.getInstance().getHarvestReport(id, date);
        return (Response.ok(report).build());
    }

    @GET
    @Path( "{id}/{date}/html" )
    @Produces( MediaType.TEXT_HTML )
    public Response getHarvestReportHtml(@PathParam( "id" ) String id, @PathParam( "date" ) String date) throws Exception {
        String report = Harvester.getInstance().getHarvestReport(id, date);
        report = "<html><font face=\"courier\">" + report.replace("\r\n", "<br/>") + "</font></html>";
        return (Response.ok(report).build());
    }

    @DELETE
    @Path( "{id}/{date}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response deleteHarvestReport(@Context HttpServletRequest request,
                                        @PathParam( "id" ) String id,
                                        @PathParam( "date" ) String date) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete harvest report.").build();

        Harvester.getInstance().removeHarvestReport(id, date);
        return (Response.ok().build());
    }
}
