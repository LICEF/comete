package ca.licef.comete.metadata.resource;

//import ca.licef.comete.core.Core;
//import ca.licef.comete.core.FedoraService;
import ca.licef.comete.core.util.Constants;
//import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.metadata.Metadata;
import ca.licef.comete.security.Security;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.store.Store;
import ca.licef.comete.vocabularies.COMETE;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;

@Singleton
@Path( "/metadataRecords" )
public class MetadataRecordResource {

    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_HTML ) //!important for ExtJS see Ext.form.Basic.hasUpload() description -AM
    public Response newUploadedRecord(@Context HttpServletRequest request,
                                      @DefaultValue( "false" ) @QueryParam( "validation" ) String validation,
                                      @FormDataParam("file") InputStream uploadedInputStream,
                                      @FormDataParam("file") FormDataContentDisposition fileDetail,
                                      @FormDataParam("res") InputStream uploadedInputStreamRes,
                                      @FormDataParam("res") FormDataContentDisposition fileDetailRes) throws Exception {

        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to upload metadata records.").build();

        String resp = null;
        Object[] files = Metadata.getInstance().storeUploadedContentTmp(uploadedInputStream, fileDetail, uploadedInputStreamRes, fileDetailRes);
        String errorMessage = (String)files[0];
        Object[] res = null;

        boolean isZip = false;
        if (errorMessage == null) {
            File record = (File) files[1];
            String extension = record.getName().substring(record.getName().lastIndexOf('.') + 1).toLowerCase();
            isZip = "zip".equals(extension);
        }

        if (errorMessage == null && "true".equals(validation) && !isZip) {
            res = Metadata.getInstance().isRecordExists((File)files[1]);
            errorMessage = (String)res[0];
        }

        if (errorMessage != null || (res != null && res.length > 0 && (Boolean)res[1]) ) {
            StringWriter out = new StringWriter();
            try {
                JSONWriter json = new JSONWriter( out ).object();
                json.key("success").value(errorMessage == null);
                if (errorMessage == null) {
                    json.key("data").value("ALREADY_EXISTS");
                    fileCache.put(request.getSession().getId(), new Object[]{(File)files[1], (File)files[2]});
                }
                else {
                    json.key("error").value(errorMessage);
                    if (files[1] != null)
                        ((File)files[1]).delete();
                    if (files[2] != null)
                        ((File)files[2]).delete();
                }

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
            resp = out.toString();
        }
        else
            resp = doUploadEff((File)files[1], (File)files[2]);

        return Response.ok(resp).build();
    }

    @PUT
    @Path( "completeUpload" )
    @Produces( MediaType.TEXT_HTML )
    public Response completeUpload( @Context HttpServletRequest request) throws Exception {
        Object[] filesData = fileCache.get(request.getSession().getId());
        String val = doUploadEff((File)filesData[0], (File)filesData[1]);
        fileCache.remove(request.getSession().getId());
        return Response.ok(val).build();
    }

    @PUT
    @Path( "clearUpload" )
    @Produces( MediaType.TEXT_HTML )
    public Response clearUpload( @Context HttpServletRequest request) throws Exception {
        Object[] filesData = fileCache.get(request.getSession().getId());
        ((File)filesData[0]).delete();
        if (filesData[1] != null)
            ((File)filesData[1]).delete();
        fileCache.remove(request.getSession().getId());
        return Response.ok().build();
    }

    String doUploadEff(File record, File resource) throws Exception {
        Object[] results = Metadata.getInstance().storeUploadedContent(record, resource);
        String errorMessage = (String)results[0];
        String[][] data = (String[][])results[1];

        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key("success").value(errorMessage == null);
            if (errorMessage == null && data != null) {
                JSONArray uris = new JSONArray();
                for (String[] uri : data) {
                    JSONObject val = new JSONObject();
                    val.put("uri", uri[0]).put("state", uri[1]);
                    uris.put(val);
                }
                json.key("data").value(uris);
            }
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

        return out.toString();
    }

    @GET
    @Path( "{id}/xml" )
    @Produces( { MediaType.TEXT_HTML, MediaType.APPLICATION_XML } )
    public Response getMetadataRecordAsXml( @PathParam( "id" ) String id, @DefaultValue( "false" ) @QueryParam( "syntaxHighlighted" ) String strIsSyntaxHighlighted ) throws Exception {
        Store store = Store.getInstance();
        String xml = store.getDatastream( "/" + id, Constants.DATASTREAM_DATA );

        if( "true".equals( strIsSyntaxHighlighted ) ) {
            String html = ( xml == null ? "" : Util.getSyntaxHighlightedCode( "xml", xml ) );
            return( Response.status( HttpServletResponse.SC_OK ).entity( html ).type( MediaType.TEXT_HTML ).build() );
        }
        else
            return( Response.status( HttpServletResponse.SC_OK ).entity( xml ).type( MediaType.APPLICATION_XML ).build() );
    }

    private transient Hashtable<String, Object[]> fileCache = new Hashtable<String, Object[]>();

}
