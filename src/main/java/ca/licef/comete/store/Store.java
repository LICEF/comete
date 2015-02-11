package ca.licef.comete.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import ca.licef.comete.core.Core;
import licef.IOUtil;

public class Store {

    public static final int DATASTREAM_UNCHANGED = 0;
    public static final int DATASTREAM_STORED = 1;

    public static final String PATH_IDENTITIES = "/identities";
    public static final String PATH_METADATA = "/metadata";
    public static final String PATH_VOCABS = "/vocabularies";

    public static final String PATH_RECORDS = PATH_METADATA + "/records";
    public static final String PATH_PHOTOS = PATH_IDENTITIES + "/photos";

    public static Store getInstance() throws IOException {
        if( instance == null ) {
            String strLoc = Core.getInstance().getCometeHome() + "/store";
            File storeLoc = new File( strLoc );
            if( !storeLoc.exists() ) {
                if( !storeLoc.mkdirs() )
                    throw( new IOException( "Cannot make directory " + storeLoc + "." ) );
            }
            instance = new Store( strLoc );
        }
        return( instance );
    }

    public Store( String location ) {
        this.location = location;
    }

    public String getLocation() {
        return( location );
    }

    public synchronized String createDigitalObject( String path ) throws IOException {
        UUID uuid = UUID.randomUUID();
        File doDir = new File( location + path + "/" + uuid );
        if( !doDir.mkdirs() )
            throw( new IOException( "Cannot make directory " + doDir + "." ) );
        return( path + "/" + uuid );
    }

    public synchronized void deleteDigitalObject(String path) throws IOException {
        File doDir = new File( location + path );
        IOUtil.deleteDirectory(doDir);
    }

    public synchronized String getDatastream( String path, String datastream ) throws IOException {
        File dsLoc = new File( location + path + "/" + datastream );
        return( IOUtil.readStringFromFile( dsLoc ) );
    }

    public synchronized int setDatastream( String path, String datastream, Object content ) throws Exception{
        if (isDatastreamExists(path, datastream)) {
            String previous = getDatastream(path, datastream);
            if (!content.equals(previous))
                deleteDatastream(path, datastream);
            else
                return DATASTREAM_UNCHANGED;
        }

        addDatastream(path, datastream, content);
        return DATASTREAM_STORED;
    }

    private void addDatastream( String path, String datastream, Object content ) throws Exception {
        File dsLoc = new File( location + path + "/" + datastream );
        if( content instanceof File ) {
            File contentFile = (File)content;
            IOUtil.copyFiles(contentFile, dsLoc);
        }
        else if( content instanceof String ) {
            String contentString = (String)content;
            if( IOUtil.isURL( contentString ) )
                IOUtil.getFile( contentString, dsLoc );
            else 
                IOUtil.writeStringToFile( contentString, dsLoc );
        }
    }

    public synchronized void deleteDatastream( String path, String datastream ) throws Exception {
        File dsLoc = new File( location + path + "/" + datastream );
        if( !dsLoc.delete() )
            throw( new IOException( "Cannot delete file " + dsLoc + "." ) );
    }


    public synchronized boolean isDatastreamExists(String path, String datastream) {
        File dsLoc = new File( location + path + "/" + datastream );
        return( dsLoc.exists() );
    }

    private String location;
    private static Store instance = null;

}


