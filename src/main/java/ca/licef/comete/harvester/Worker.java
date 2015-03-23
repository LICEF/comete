package ca.licef.comete.harvester;

import licef.IOUtil;
import licef.Sha1Util;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import licef.DateUtil;
import licef.StringUtil;
import licef.XMLUtil;
import licef.VersionUtil;

public class Worker implements Runnable {

    public Worker( String id, String url, String metadataNamespace ) {
        this.id = id;
        this.url = url;
        this.metadataNamespace = metadataNamespace;
        this.digester = new Digester();

        initRepository();
    }

    private void initRepository() {
        try {
            this.repoId = Sha1Util.hash(url);
            JSONObject def = Harvester.getInstance().getDefinition(this.id);
            digester.addOrUpdateRepository(
                    def.getString("name"), def.getString("type"), getUrl(), getRepoId(), def.getString("adminEmail"), this.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if( thread == null ) {
            thread = new Thread( this, "Worker for " + id );
            thread.start();
            isRunning = true;
        }
    }

    public void stop() {
        if( thread != null ) {
            thread = null;
            isRunning = false;
        }
    }

    void digest(String identifier, String status , String datestamp, String metadata) throws Exception {
        if ("deleted".equals(status)) {
            int resp = digester.deleteHarvestedRecord(identifier, getMetadataNamespace());
            if (resp == Digester.DELETED)
                getReport().incrementDeleted();
        }
        else {
            int resp = digester.addOrUpdateHarvestedRecord(identifier, getMetadataNamespace(), datestamp, getRepoId(), metadata);
            if (resp == Digester.ADDED)
                getReport().incrementAdded();
            else if (resp == Digester.UPDATED)
                getReport().incrementUpdated();
        }
    }

    public void run() {
        isRunning = false;
    }

    public boolean isRunning() {
        return( isRunning );
    }

    public String getId() {
        return( id );
    }

    public String getUrl() {
        return( url );
    }

    public String getMetadataNamespace() {
        return( metadataNamespace );
    }

    public String getRepoId() {
        return( repoId );
    }

    public String getXslt() {
        return( xslt );
    }

    public void setXslt( String xslt ) {
        this.xslt = xslt;
    }

    public String getFrom() {
        return( from );
    }

    public void setFrom( String from ) {
        this.from = from;
    }

    public Report getReport() {
        return report;
    }

    public Node postProcessMetadata( Node metadata ) throws ClassNotFoundException, IllegalAccessException, InstantiationException, TransformerConfigurationException, IOException, NoSuchMethodException, TransformerException {
        if( getXslt() == null )
            return( metadata );
            
        StreamSource xsltSource = new StreamSource( new BufferedReader( new StringReader( getXslt() ) ) );
        DOMSource docSource = new DOMSource( metadata );
        Node transformedMetadata = XMLUtil.applyXslToDocument2( xsltSource, docSource, null, null, null, "net.sf.saxon.TransformerFactoryImpl" );
        return( transformedMetadata );
    }

    protected void updateFromDate() throws IOException {
        String lastHarvestDate = null;
        if( !StringUtil.isEmpty( from ) )
            lastHarvestDate = from;
        else {
            String startISODatetime = DateUtil.toISOString(startDate, null, null);
            lastHarvestDate = startISODatetime.substring(0, startISODatetime.indexOf("T"));
        }
        IOUtil.writeStringToFile(lastHarvestDate, Harvester.getLastHarvestFile( id ));
    }

    protected volatile Thread thread;
    protected Report report;
    protected Date startDate;
    protected Date endDate;

    private String id;
    private String url;
    private String metadataNamespace;
    private String repoId;
    private String xslt;
    private String from;

    private boolean isRunning;

    private Digester digester;

}
