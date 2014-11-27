package ca.licef.comete.harvester;

import ca.licef.comete.core.Core;
import com.sun.jersey.core.header.FormDataContentDisposition;

import licef.IOUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 14-04-01
 */
public class Harvester {

    static File HARVESTER_FOLDER = new File(Core.getInstance().getCometeHome() + "/harvester2/");

    public static File getLastHarvestFile( String defId ) {
        File defFolder = new File( Harvester.HARVESTER_FOLDER, defId );
        return( new File(defFolder, "lastHarvest.txt") );
    }

    private static Harvester instance;

    private Map<String,Worker> workers = new HashMap<String,Worker>();

    static {
        try {
            if (!HARVESTER_FOLDER.exists())
                HARVESTER_FOLDER.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Harvester getInstance() {
        if (instance == null)
            instance = new Harvester();
        return (instance);
    }

    /***************/
    /* Definitions */
    /***************/

    public JSONObject getDefinition(String defId) throws Exception {
        File defFolder = new File( HARVESTER_FOLDER, defId );
        File defFile = new File( defFolder, defId + ".json" );
        String jsonStr = new String( IOUtil.readFileIntoByteArray( defFile ), "UTF-8" );
        return new JSONObject( jsonStr );
    }

    public String[] getDefinitions() {
        ArrayList<String> defs = new ArrayList<String>();
        for (String folder : HARVESTER_FOLDER.list()) {
            defs.add(folder);
        }
        return defs.toArray(new String[defs.size()]);
    }

    public void storeDefinition(String defId, InputStream uploadedInputStream, FormDataContentDisposition fileDetail) throws Exception {
        File defFolder = new File(HARVESTER_FOLDER, defId);
        IOUtil.createDirectory(defFolder.getAbsolutePath());
        FileOutputStream fos = new FileOutputStream(new File(defFolder, defId + ".json"));
        IOUtil.copy(uploadedInputStream, fos);
        uploadedInputStream.close();
        fos.close();
    }

    public void storeXsl(String defId, InputStream uploadedInputStream, FormDataContentDisposition fileDetail) throws Exception {
        File defFolder = new File( HARVESTER_FOLDER, defId );
        if( !defFolder.exists() )
            throw new Exception( "Harvest definition not found." );

        FileOutputStream fos = new FileOutputStream(new File(defFolder, defId + ".xsl"));
        IOUtil.copy(uploadedInputStream, fos);
        uploadedInputStream.close();
        fos.close();
    }

    private String getXslt(String defId) throws Exception {
        File defFolder = new File( HARVESTER_FOLDER, defId );
        File xsl = new File(defFolder, defId + ".xsl");
        if (!xsl.exists())
            return null;
        else
            return IOUtil.readStringFromFile( xsl );
    }

    public void removeDefinition(String defId) throws Exception {
        if (isHarvest(defId))
            throw new Exception("A harvest is already in progress with this definition. Cannot delete it.");

        File defFolder = new File( HARVESTER_FOLDER, defId );
        if( !defFolder.exists() )
            throw new Exception( "Harvest definition not found." );

        if (!IOUtil.deleteDirectory(defFolder))
            throw new Exception( "Cannot delete definition." );
    }

    /************/
    /* Harvests */
    /************/

    public String[] getHarvests() {
        Set<String> keys = workers.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    public boolean isHarvest(String defId) {
        return workers.containsKey(defId);
    }

    public String startHarvest( String defId, String from ) throws Exception {
System.out.println( "startHarvest defId="+defId+" from="+from );        
        if (isHarvest(defId))
            throw new Exception( "A harvest is already in progress with this definition." );

        //temporary test to avoid parallel harvests
        if (getHarvests().length > 0)
            throw new Exception( "A harvest is already in progress. Only one at a time for this version." );
        ///////////////

        File defFolder = new File( HARVESTER_FOLDER, defId );
        if( !defFolder.exists() )
            throw new Exception( "Harvest definition not found." );

        if( from == null ) {
            File lastHarvestFile = getLastHarvestFile( defId );
            if( lastHarvestFile.exists() ) {
                Vector lines = IOUtil.readLines( lastHarvestFile );
                if( lines.size() > 0 )
                    from = (String)lines.get( 0 );
            }
        }
    
        JSONObject json = getDefinition(defId);
         
        String type = (String)json.get( "type" );
        String url = (String)json.get( "url" );
        String metadataNamespace = (String)json.get( "metadataNamespace" );
        Worker worker = null;
        if( "OAI".equals( type ) )
            worker = new OAIWorker( defId, url, metadataNamespace );
        else if( "HTML".equals( type ) )
            worker = new HTMLWorker( defId, url, metadataNamespace );
        worker.setXslt( getXslt(defId) );
        worker.setFrom( from );
        workers.put( defId, worker );
        worker.start();
        return( worker.getId() );
    }

    public void stopHarvest( String defId ) throws Exception {
        Worker worker = workers.get( defId );
        if( worker == null )
            throw new Exception( "Harvest not found." );

        worker.stop();
        removeHarvest(defId);
    }

    public void removeHarvest(String defId) {
        Worker worker = workers.get( defId );
        if( worker != null )
            workers.remove( defId );
    }

}
