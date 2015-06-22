package ca.licef.comete.vocabulary.resource;

import ca.licef.comete.core.util.Util;
import ca.licef.comete.security.Security;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.Vocabulary;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

@Singleton
@Path( "/vocContexts" )
public class VocContextResource {

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocabularies( @DefaultValue( "en" ) @QueryParam( "lang" ) String lang ) throws Exception {
        Tuple[] ctxts = Vocabulary.getInstance().getVocContexts();
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray vocCtxts = new JSONArray();
            for (int i = 0; i < ctxts.length; i++) {
                String vocCtxtUri = ctxts[i].getValue("s").getContent();
                String vocUri = ctxts[i].getValue("vocUri").getContent();
                JSONObject voc = new JSONObject();
                voc.put( "restUrl",
                        Util.getRestUrl(COMETE.VocContext) + "/" +
                            Util.getIdValue(vocCtxtUri));
                voc.put( "label", Vocabulary.getInstance().getLabel(vocUri, lang) );
                vocCtxts.put(voc);
            }
            json.key( "vocContexts" ).value( vocCtxts );
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

        return Response.ok(out.toString()).build();
    }

    @GET
    @Path( "{id}/used" )
    public Response isVocabularyUsed(@PathParam( "id" ) String id ) throws Exception {
        String uri = Util.makeURI(id, COMETE.VocContext);
        boolean b = Vocabulary.getInstance().isVocabularyUsed(uri);
        return Response.ok(Boolean.toString(b)).build();
    }

    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_HTML ) //!important for ExtJS see Ext.form.Basic.hasUpload() description -AM
    public Response addNewVocContext(@Context HttpServletRequest request,
                                  @FormDataParam("id") String id,
                                  @FormDataParam("uriPrefix") String uriPrefix,
                                  @FormDataParam("uriSuffix") String uriSuffix,
                                  @FormDataParam("linkingPredicate") String linkingPredicate,
                                  @FormDataParam("url") String url,
                                  @FormDataParam("file") java.io.InputStream uploadedInputStream,
                                  @FormDataParam("file") com.sun.jersey.core.header.FormDataContentDisposition fileDetail ) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to add vocabulary.").build();

        // Make sure that the id is a valid filename.
        // Only letters and numbers are allowed.  
        // Other characters are replaced by underscores.
        id = id.replaceAll( "[^a-zA-Z0-9]", "_" );

        String errorMessage = Vocabulary.getInstance().addNewVocContext(
                id, uriPrefix, uriSuffix, linkingPredicate, url, fileDetail.getFileName(), uploadedInputStream);
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

    @POST
    @Path( "{id}" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_HTML ) //!important for ExtJS see Ext.form.Basic.hasUpload() description -AM
    public Response modifyVocContextAndContent(@Context HttpServletRequest request,
                                     @PathParam( "id" ) String id,
                                     @FormDataParam("uriPrefix") String uriPrefix,
                                     @FormDataParam("uriSuffix") String uriSuffix,
                                     @FormDataParam("linkingPredicate") String linkingPredicate,
                                     @FormDataParam("url") String url,
                                     @FormDataParam("file") java.io.InputStream uploadedInputStream,
                                     @FormDataParam("file") com.sun.jersey.core.header.FormDataContentDisposition fileDetail) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to modify vocabulary.").build();

        String errorMessage = Vocabulary.getInstance().modifyVocabularyContent(
                id, uriPrefix, uriSuffix, linkingPredicate, url, fileDetail.getFileName(), uploadedInputStream);
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

    @DELETE
    @Path( "{id}" )
    public Response deleteVocContext(@Context HttpServletRequest request, @PathParam( "id" ) String id ) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to delete vocabulary.").build();

        String uri = Util.makeURI(id, COMETE.VocContext);
        boolean ok = Vocabulary.getInstance().deleteVocContext(uri);
        if (!ok)
            return Response.status(Response.Status.UNAUTHORIZED).entity("Vocabulary linked by resource(s). Cannot delete it.").build();

        return Response.ok().build();
    }

    @GET
    @Path( "{id}/details" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocContextDetails(@PathParam( "id" ) String id) throws Exception {
        String uri = Util.makeURI(id, COMETE.VocContext);
        Tuple[] details = Vocabulary.getInstance().getVocContextDetails(uri);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            json.key( "id" ).value( details[0].getValue("vocId").getContent() );
            json.key( "uri" ).value( details[0].getValue("vocUri").getContent() );
            String location = details[0].getValue("location").getContent();
            if (!location.startsWith("http"))
                location = "local: " + location;
            json.key( "location" ).value( location);
            json.key( "navigable" ).value( details[0].getValue("navigable").getContent() );
            json.key( "linkingPredicate" ).value( details[0].getValue("predicate").getContent() );
            String pref = details[0].getValue("prefix").getContent();
            if (!"".equals(pref))
                json.key( "uriPrefix" ).value( pref);
            String suf = details[0].getValue("suffix").getContent();
            if (!"".equals(suf))
                json.key( "uriSuffix" ).value( suf);

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

        return Response.ok(out.toString()).build();
    }

    @GET
    @Path( "{id}/aliases" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVocContextAliases(@PathParam( "id" ) String id) throws Exception {
        String uri = Util.makeURI(id, COMETE.VocContext);
        Triple[] aliases = Vocabulary.getInstance().getVocContextAliases(uri);
        StringWriter out = new StringWriter();
        try {
            JSONWriter json = new JSONWriter( out ).object();

            JSONArray vocAliases = new JSONArray();
            for (Triple triple : aliases) {
                JSONObject alias = new JSONObject();
                alias.put("alias", triple.getObject());
                vocAliases.put(alias);
            }
            json.key( "vocAliases" ).value( vocAliases );
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

        return Response.ok(out.toString()).build();
    }

    @POST
    @Path( "{id}/aliases" )
    public Response addVocContextAlias(@Context HttpServletRequest request, @PathParam( "id" ) String id,
                                       @FormParam( "alias" ) String alias) throws Exception {

        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to change settings.").build();

        String uri = Util.makeURI(id, COMETE.VocContext);
        Vocabulary.getInstance().addVocContextAlias(uri, alias);

        return Response.ok().build();
    }

    @PUT
    @Path( "{id}/aliases" )
    public Response updateVocContextAlias(@Context HttpServletRequest request,
                                          @PathParam( "id" ) String id,
                                          @FormParam( "alias" ) String alias,
                                          @FormParam( "prevAlias" ) String prevAlias) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to change settings.").build();

        String uri = Util.makeURI(id, COMETE.VocContext);
        Vocabulary.getInstance().updateVocContextAlias(uri, prevAlias, alias);

        return Response.ok().build();
    }

    @DELETE
    @Path( "{id}/aliases" )
    public Response deleteVocContextAlias(@Context HttpServletRequest request, @PathParam( "id" ) String id,
                                          @FormParam( "alias" ) String alias) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to change settings.").build();

        String uri = Util.makeURI(id, COMETE.VocContext);
        Vocabulary.getInstance().deleteVocContextAlias(uri, alias);
        return Response.ok().build();
    }

    @POST
    @Path( "{id}/navigable" )
    public Response setVocContextNav(@Context HttpServletRequest request, @PathParam( "id" ) String id) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to change settings.").build();

        String uri = Util.makeURI(id, COMETE.VocContext);
        Vocabulary.getInstance().updateVocContextNavigable(uri, true);

        return Response.ok().build();
    }

    @DELETE
    @Path( "{id}/navigable" )
    public Response deleteVocContextNav(@Context HttpServletRequest request, @PathParam( "id" ) String id) throws Exception {
        if (!Security.getInstance().isAuthorized(request))
            return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized to change settings.").build();

        String uri = Util.makeURI(id, COMETE.VocContext);
        Vocabulary.getInstance().updateVocContextNavigable(uri, false);

        return Response.ok().build();
    }

    @Context
    private ServletContext context;

}
