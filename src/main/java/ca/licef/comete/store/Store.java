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

    public synchronized String createDigitalObject() throws IOException {
        return( createDigitalObject( "" ) );
    }

    public synchronized String createDigitalObject( String path ) throws IOException {
        UUID uuid = UUID.randomUUID();
        File doDir = new File( location + path + "/" + uuid );
        if( !doDir.mkdir() )
            throw( new IOException( "Cannot make directory " + doDir + "." ) );
        return( path + "/" + uuid );
    }

    public synchronized String getDatastream( String path, String datastream ) throws IOException {
        File dsLoc = new File( location + path + "/" + datastream );
        return( IOUtil.readStringFromFile( dsLoc ) );
    }

    public synchronized void addDatastream( String path, String datastream, Object content ) throws Exception {
        File dsLoc = new File( location + path + "/" + datastream );
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

    public synchronized void modifyDatastream( String path, String datastream, Object content ) throws Exception {
        File dsLoc = new File( location + path + "/" + datastream );
        if( dsLoc.delete() )
            throw( new IOException( "Cannot delete file " + dsLoc + "." ) );

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

    public synchronized void purgeDatastream(String path, String datastream) throws IOException {
        File dsLoc = new File( location + path + "/" + datastream );
        if( !dsLoc.delete() )
            throw( new IOException( "Cannot delete datastream " + dsLoc + "." ) );
        // If the parent object (i.e. folder) has no children, 
        // should I delete it and all its empty ancerstors as well? - FB
    }

    public synchronized boolean isDatastreamExists(String path, String datastream) {
        File dsLoc = new File( location + path + "/" + datastream );
        return( dsLoc.exists() );
    }

    private String location;
    private static Store instance = null;

}


