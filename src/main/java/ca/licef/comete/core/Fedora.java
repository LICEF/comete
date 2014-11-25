package ca.licef.comete.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import ca.licef.comete.core.util.Constants;
import licef.IOUtil;
import org.fcrepo.client.FedoraContent;
import org.fcrepo.client.FedoraDatastream;
import org.fcrepo.client.FedoraException;
import org.fcrepo.client.FedoraObject;
import org.fcrepo.client.FedoraRepository;
import org.fcrepo.client.impl.FedoraRepositoryImpl;


public class Fedora {

    private String url;
    private String username;
    private String password;

    private FedoraRepository repo;

    public FedoraRepository getRepository() {
        if( repo == null )
            repo = new FedoraRepositoryImpl( url + "/rest", username, password );
        return( repo );
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public String getUrl() {
        return( url );
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getUsername() {
        return( username );
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public String getPassword() {
        return( password );
    }

    public String createDigitalObject() throws FedoraException {
        FedoraObject object = getRepository().createObject( "/", true );
        return( object.getPath() ); 
    }

    public String createDigitalObject( String path ) throws FedoraException {
        FedoraObject object = getRepository().createObject( path );
        return( object.getPath() );
    }

    /**
     * @param contentType Possible values are "application/rdf+xml", "text/turtle", etc.
     */
    public String ingestDigitalObject( String path, String rdf, String contentType ) throws FedoraException {
        //FedoraObject object = getRepository().createObject( "/", true );
        //InputStream is = new ByteArrayInputStream( rdf.getBytes( StandardCharsets.UTF_8 ) );
        //object.updateProperties( is, contentType ); 
        //return( object.getName() );
        return( null );
    }

    public void addDatastream( String parentId, String dataStream, Object content, String mimetype, String checksum ) throws FedoraException, FileNotFoundException, MalformedURLException, IOException, URISyntaxException {
        String path = parentId + "/" + dataStream;

        FedoraContent fc = new FedoraContent();
        if( content instanceof File ) {
            File contentFile = (File)content;
            fc.setFilename( contentFile.toString() );
            fc.setContent( new FileInputStream( contentFile ) );
        }
        else if( content instanceof String ) {
            String contentString = (String)content;
            if( IOUtil.isURL( contentString ) ) {
                URL contentUrl = new URL( contentString );
                fc.setContent( contentUrl.openStream() ); 
            }
            else
                fc.setContent( new ByteArrayInputStream( contentString.getBytes( StandardCharsets.UTF_8 ) ) );
        }
        if( mimetype != null )
            fc.setContentType( mimetype );
        if( checksum != null )
            fc.setChecksum( new URI( checksum ) );

        getRepository().createDatastream( path, fc );
    }

    public void purgeDatastream(String id, String dataStream) throws Exception {
        String path = id + "/" + dataStream;
        FedoraDatastream ds = getRepository().getDatastream( path );
        if( ds != null ) 
            ds.delete();
        // If the parent object (i.e. folder) has no children, should I delete it and all its empty ancerstors as well? - FB
    }

    public boolean isDatastreamExists(String id, String dataStream) throws FedoraException, IOException {
        return( getRepository().exists( id + "/" + dataStream ) );
    }

}

