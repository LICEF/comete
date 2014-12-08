package ca.licef.comete.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import ca.licef.comete.core.Core;
import licef.IOUtil;

public class Store {

    public static Store getInstance() throws IOException {
        if( instance == null ) {
            String strLoc = Core.getInstance().getCometeHome() + "/store";
            File storeLoc = new File( strLoc );
            if( !storeLoc.exists() ) {
                if( !storeLoc.mkdir() )
                    throw( new IOException( "Cannot make directory " + storeLoc + "." ) );
            }
            instance = new Store( strLoc );
        }
        return( instance );
    }

    public Store( String location ) {
        this.location = location;
    }

    public String createDigitalObject() throws IOException {
        return( createDigitalObject( "/" ) );
    }

    public String createDigitalObject( String path ) throws IOException {
        UUID uuid = UUID.randomUUID();
        File pathDir = new File( location, path );
        File doDir = new File( pathDir, uuid.toString() );
        if( !doDir.mkdir() )
            throw( new IOException( "Cannot make directory " + doDir + "." ) );
        return( path + "/" + uuid );
    }

    /**
     * @param contentType Possible values are "application/rdf+xml", "text/turtle", etc.
     */
    public String ingestDigitalObject( String path, String rdf, String contentType ) throws IOException {
        File pathDir = new File( location, path );
        String encodedContentType = contentType.replaceAll( "/", "_" );
        File doLoc = new File( pathDir, encodedContentType );
        IOUtil.writeStringToFile( rdf, doLoc );
        return( path + "/" + encodedContentType ); 
    }

    public void addDatastream( String parentId, String datastream, Object content, String mimetype ) throws Exception {
        File doLoc = new File( location, parentId );
        File loc = new File( doLoc, datastream );
        if( !loc.mkdir() ) 
            throw( new IOException( "Cannot make directory " + loc + "." ) );

        String encodedMimeType = mimetype.replaceAll( "/", "_" );
        File dsLoc = new File( loc, encodedMimeType );
        if( content instanceof File ) {
            File contentFile = (File)content;
            Files.copy( contentFile.toPath(), dsLoc.toPath() );
        }
        else if( content instanceof String ) {
            String contentString = (String)content;
            if( IOUtil.isURL( contentString ) )
                IOUtil.getFile( contentString, dsLoc );
            else 
                IOUtil.writeStringToFile( contentString, dsLoc );
        }

    }

    public void purgeDatastream(String path, String datastream) throws IOException {
        File dsLoc = new File( path, datastream );
        if( !dsLoc.delete() )
            throw( new IOException( "Cannot delete datastream " + dsLoc + "." ) );
        // If the parent object (i.e. folder) has no children, should I delete it and all its empty ancerstors as well? - FB
    }

    public boolean isDatastreamExists(String path, String datastream) {
        File dsLoc = new File( path, datastream );
        return( dsLoc.exists() );
    }

    private String location;
    private static Store instance = null;

}


