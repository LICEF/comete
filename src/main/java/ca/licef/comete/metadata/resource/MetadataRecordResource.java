package ca.licef.comete.metadata.resource;

import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.ResultSet;
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
import javax.ws.rs.DELETE;
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
import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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

    @DELETE
    @Path( "{id}" )
    @Produces( MediaType.TEXT_PLAIN )
    public Response deleteMetadataRecord( @Context HttpServletRequest request,
                                          @PathParam( "id" ) String id ) throws Exception {
        if (!Security.getInstance().isAuthorized(request.getRemoteAddr()))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete records.").build();

        String metadataRecordUri = Util.makeURI(id, COMETE.MetadataRecord);
        Metadata.getInstance().deleteRecord(metadataRecordUri, true);
        return (Response.ok().build());
    }

    @GET
    @Path( "{id}/validationReport/{applProf}/xml" )
    @Produces( { MediaType.TEXT_HTML, MediaType.APPLICATION_XML } )
    public Response getMetadataRecordValidationReportAsXml( @PathParam( "id" ) String id, @PathParam( "applProf" ) String applProf, @DefaultValue( "false" ) @QueryParam( "syntaxHighlighted" ) String strIsSyntaxHighlighted ) throws Exception {
        Store store = Store.getInstance();
        String path = Store.PATH_RECORDS + "/" + id;
        String datastream = "ValidationReport" + applProf;

        String xml = null;
        if( store.isDatastreamExists( path, datastream )  )
            xml = store.getDatastream( path, datastream );

        if( "true".equals( strIsSyntaxHighlighted ) ) {
            String html = ( xml == null ? "" : Util.getSyntaxHighlightedCode( "xml", xml ) );
            return( Response.status( HttpServletResponse.SC_OK ).entity( html ).type( MediaType.TEXT_HTML ).build() );
        }
        else
            return( Response.status( HttpServletResponse.SC_OK ).entity( xml ).type( MediaType.APPLICATION_XML ).build() );
    }

    @GET
    @Path( "/applicationProfiles" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getMetadataRecordApplicationProfiles( @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit ) throws Exception {
        int start = -1;
        if( strStart != null ) {
            try {
                start = Integer.parseInt( strStart );
            }
            catch( NumberFormatException e ) {
                throw( new WebApplicationException( e, HttpServletResponse.SC_BAD_REQUEST ) );
            }
        }

        int limit = -1;
        if( strLimit != null ) {
            try {
                limit = Integer.parseInt( strLimit );
            }
            catch( NumberFormatException e ) {
                throw( new WebApplicationException( e, HttpServletResponse.SC_BAD_REQUEST ) );
            }
        }

        ResultSet rs = Metadata.getInstance().getMetadataRecordApplicationProfiles( start, limit, null, false );

        StringWriter out = new StringWriter();
        JSONWriter json = new JSONWriter( out );
        
        JSONArray records = new JSONArray();
        
        for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
            Map<String, Object> entry = (Map<String, Object>)it.next();

            JSONObject record = new JSONObject();
            record.put( "id", entry.get( "id" ) )
                .put( "profiles", entry.get( "profiles" ) );
            records.put( record );
        }

        json.object()
            .key( "records" ).value( records )
            .key( "totalCount" ).value( rs.getTotalRecords() );

        json.endObject();

        out.close();

        return( out.toString() );
    }

    @GET
    @Path( "/applicationProfilesByColumns" )
    @Produces( MediaType.APPLICATION_JSON )
    public String getApplicationProfilesByColumn( @DefaultValue( "0" ) @QueryParam( "start" ) String strStart, @DefaultValue( "20" ) @QueryParam( "limit" ) String strLimit, @QueryParam( "showOnlyColumn" ) String showOnlyColumn, @QueryParam( "showOnlyInvalid" ) String showOnlyInvalid ) throws Exception {
        int start = -1;
        if( strStart != null ) {
            try {
                start = Integer.parseInt( strStart );
            }
            catch( NumberFormatException e ) {
                throw( new WebApplicationException( e, HttpServletResponse.SC_BAD_REQUEST ) );
            }
        }

        int limit = -1;
        if( strLimit != null ) {
            try {
                limit = Integer.parseInt( strLimit );
            }
            catch( NumberFormatException e ) {
                throw( new WebApplicationException( e, HttpServletResponse.SC_BAD_REQUEST ) );
            }
        }

        ResultSet rs = null;
        if( showOnlyColumn != null && !"".equals( showOnlyColumn ) ) 
            if( "true".equals( showOnlyInvalid ) ) 
                rs = Metadata.getInstance().getMetadataRecordApplicationProfiles( start, limit, showOnlyColumn, true );
            else
                rs = Metadata.getInstance().getMetadataRecordApplicationProfiles( start, limit, showOnlyColumn, false );
        else
            rs = Metadata.getInstance().getMetadataRecordApplicationProfiles( start, limit, null );

        StringWriter out = new StringWriter();
        JSONWriter json = new JSONWriter( out );
        
        JSONArray records = new JSONArray();
        
        for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
            Map<String, Object> entry = (Map<String, Object>)it.next();

            JSONObject record = new JSONObject();
            record.put( "id", entry.get( "id" ) );
            List<String> profiles = (List<String>)entry.get( "profiles" );
            for( int i = 0; i < Constants.lomApplProfiles.length; i++ ) {
                String isValid = null;
                if( profiles.contains( Constants.lomApplProfiles[ i ] ) )
                    isValid = "true";
                else if( Constants.IEEE_LOM_NAMESPACE.equals( entry.get( "metadataFormat" ) ) )
                    isValid = "false";
                else
                    isValid = "notApplicable";
                record.put( Constants.lomApplProfAbbrevs[ i ], isValid );
            }
            for( int i = 0; i < Constants.dcApplProfiles.length; i++ ) {
                String isValid = null;
                if( profiles.contains( Constants.dcApplProfiles[ i ] ) )
                    isValid = "true";
                else if( Constants.OAI_DC_NAMESPACE.equals( entry.get( "metadataFormat" ) ) )
                    isValid = "false";
                else
                    isValid = "notApplicable";
                record.put( Constants.dcApplProfAbbrevs[ i ], isValid );
            }
            String repoUri = (String)entry.get( "repoUri" );
            if( repoUri != null )
                record.put( "repoUri", repoUri );
            String repoName = (String)entry.get( "repoName" );
            if( repoName != null )
                record.put( "repoName", repoName );
            String repoAdminEmail = (String)entry.get( "repoAdminEmail" );
            if( repoAdminEmail != null )
                record.put( "repoAdminEmail", repoAdminEmail );

            records.put( record );
        }

        json.object()
            .key( "records" ).value( records )
            .key( "totalCount" ).value( rs.getTotalRecords() );

        json.endObject();

        out.close();

        return( out.toString() );
    }

    /*
     * Possible values for datastreamType are: xml, lom, and dc.
     */
    @GET
    @Path( "{id}/{datastreamType}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.APPLICATION_XML } )
    public Response getMetadataRecordAsXml( @PathParam( "id" ) String id, @PathParam( "datastreamType" ) String datastreamType, @DefaultValue( "false" ) @QueryParam( "syntaxHighlighted" ) String strIsSyntaxHighlighted ) throws Exception {
        String datastream = null;
        if( datastreamType.equals( "xml" ) )
            datastream = Constants.DATASTREAM_ORIGINAL_DATA;
        else if( datastreamType.equals( "lom" ) )
            datastream = Constants.DATASTREAM_EXPOSED_DATA_LOM;
        else if( datastreamType.equals( "dc" ) )
            datastream = Constants.DATASTREAM_EXPOSED_DATA_DC;
        else
            return( Response.status( Response.Status.BAD_REQUEST ).entity( "Invalid datastream type." ).build());

        boolean isSyntaxHighlighted = ( "true".equals( strIsSyntaxHighlighted ) );
        
        return( getXmlDatastream( Store.PATH_RECORDS + "/" + id, datastream, isSyntaxHighlighted ) );
    }

    private Response getXmlDatastream( String path, String datastream, boolean isSyntaxHighlighted ) throws Exception {
        Store store = Store.getInstance();
        String xml = store.getDatastream( path, datastream );

        if( isSyntaxHighlighted ) {
            String html = ( xml == null ? "" : Util.getSyntaxHighlightedCode( "xml", xml ) );
            return( Response.status( HttpServletResponse.SC_OK ).entity( html ).type( MediaType.TEXT_HTML ).build() );
        }
        else
            return( Response.status( HttpServletResponse.SC_OK ).entity( xml ).type( MediaType.APPLICATION_XML ).build() );
    }

    private transient Hashtable<String, Object[]> fileCache = new Hashtable<String, Object[]>();

}
