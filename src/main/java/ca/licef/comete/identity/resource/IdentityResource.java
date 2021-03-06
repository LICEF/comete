package ca.licef.comete.identity.resource;

import ca.licef.comete.identity.Identity;
import ca.licef.comete.security.Security;
import ca.licef.comete.store.Store;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Iterator;
import java.util.Map;

@Singleton
@Path( "/identities" )
@Api( value = "Identity" )
public class IdentityResource {

    @GET
    @Path("photo/{filename}")
    public Response getPhoto(@PathParam("filename") String filename) throws Exception {
        String format = filename.substring(filename.lastIndexOf(".") + 1);
        String path = Store.getInstance().getLocation() + Store.PATH_PHOTOS + "/" + filename;
        InputStream is = new BufferedInputStream(new FileInputStream(path));
        return Response.ok(is).type("image/" + format).build();
    }

    @POST
    @Path("photo")
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_HTML ) //!important for ExtJS see Ext.form.Basic.hasUpload() description -AM
    public Response uploadedPhoto(@Context HttpServletRequest request,
                                  @FormDataParam("photo") InputStream uploadedInputStream,
                                  @FormDataParam("photo") FormDataContentDisposition fileDetail) throws Exception {

        boolean b = Security.getInstance().isAuthorized(request);
        String errorMessage;
        String data = null;
        if (b) {
            String[] res = Identity.getInstance().storeUploadedPhoto(uploadedInputStream, fileDetail);
            data = res[0];
            errorMessage = res[1];
        }
        else
            errorMessage = "Not authorized to upload photo.";
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();
            json.key("success").value(errorMessage == null);
            if (data != null)
                json.key("data").value(data);
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
        String res = out.toString();
        if (b)
            return Response.ok(res).build();
        else
            return Response.status(Response.Status.UNAUTHORIZED).entity(res).build();
    }

    @GET
    @Path( "emails" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getEmails(@QueryParam("uris") String identityUriList) throws Exception {
        StringWriter out = new StringWriter();

        try {
            JSONArray jsonIdentities = new JSONArray( identityUriList );
            String[] uris = new String[ jsonIdentities.length() ];
            for( int i = 0; i < jsonIdentities.length(); i++ )
                uris[ i ] = (String)jsonIdentities.get( i );

            Map emails = Identity.getInstance().getEmails( uris );
            JSONWriter json = new JSONWriter( out ).object();
            for( Iterator it = emails.keySet().iterator(); it.hasNext(); ) {
                String uri = (String)it.next();
                String email = (String)emails.get( uri );
                json.key( uri ).value( email );
            }
            json.endObject();
        }
        catch( JSONException e ) {
            e.printStackTrace();
        }
        finally {
            try {
                out.close();
            }
            catch( IOException e ) {
                e.printStackTrace();
            }
        }

        return Response.ok(out.toString()).build();
    }

    @Context
    private ServletContext context;

}
