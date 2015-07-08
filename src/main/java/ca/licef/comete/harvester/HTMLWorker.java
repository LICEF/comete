package ca.licef.comete.harvester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import ca.licef.comete.metadata.util.Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Node;


import licef.XMLUtil;

public class HTMLWorker extends Worker {

    public HTMLWorker( String id, String url, String metadataNamespace, 
        boolean isPendingByDefault, boolean isCheckingBrokenLink, boolean isCheckingInvalid, String invalidApplProf ) {
        super( id, url, metadataNamespace, isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, invalidApplProf );
    }

    @Override
    public void run() {
        startDate = new Date();
        report = new Report(getId(), startDate, getFrom());
        Throwable throwable = null;
        try {
            List<LinkFromHtmlPage> links = null;

            links = retrieveLinks();

            Thread thisThread = Thread.currentThread();
            for( Iterator<LinkFromHtmlPage> it = links.iterator(); thread == thisThread && it.hasNext(); ) { 
                LinkFromHtmlPage link = it.next();
                try {
                    String status = null;
                    Node metadataNode = link.getRecord();
                    if( metadataNode == null )
                        throw new Exception( "Empty metadata record or metadata record not found." );
                    String[] data = Util.parseMetadataRecord(metadataNode);
                    String errorMessage = data[0];
                    if (errorMessage != null)
                        throw new Exception(errorMessage);

                    String identifier = data[3];
                    String datestamp = link.getLatestDatestamp();

                    String metadata = null;
                    if( metadataNode != null ) {
                        metadataNode = postProcessMetadata( metadataNode );
                        metadata = XMLUtil.getXMLString( metadataNode, true );
                    }
                    digest(identifier, status, datestamp, metadata);
                }
                catch( Throwable t ) {
                    getReport().addError( new Error(link.getUrl(), t ) );
                }
            }
        }
        catch( Throwable t ) {
            throwable = t;
        }
        finally {
            try {
                endDate = new Date();
                if( throwable == null && thread != null )
                    updateFromDate();
                getReport().generateReport(endDate, throwable, thread == null);
                Harvester.getInstance().removeHarvest(getId());
                try {
                    notifyListener();
                }
                catch( Exception e2 ) {
                    e2.printStackTrace();
                }
                thread = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<LinkFromHtmlPage> retrieveLinks() throws IOException {
        List<LinkFromHtmlPage> links = new ArrayList<LinkFromHtmlPage>();

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet get = new HttpGet( getUrl() );
        HttpResponse response = httpclient.execute(get);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String contentAsString = EntityUtils.toString(entity, "UTF-8");

            BufferedReader reader = new BufferedReader(new StringReader(contentAsString));
            try {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    int workIndex = 0;
                    while (true) {
                        int indexOfAnchorStart = line.indexOf("<a", workIndex);
                        if (indexOfAnchorStart == -1)
                            break;
                        int indexOfHrefStart = line.indexOf("href=\"", indexOfAnchorStart);
                        int indexOfHrefEnd = line.indexOf("\">", indexOfHrefStart);
                        String href = line.substring(indexOfHrefStart + "href=\"".length(), indexOfHrefEnd);
                        String linkUrl = null;
                        if( href.startsWith( "http" ) )
                            linkUrl = href;
                        else
                            linkUrl = getUrl() + (getUrl().endsWith("/") ? "" : "/") + href;
                        int indexOfAnchorEnd = line.indexOf("</a>", indexOfHrefEnd);
                        //String id = "urn:uuid:" + UUIDUtil.constructUUID( UUIDType.NAME_BASED_SHA1, DigestUtils.shaHex(linkUrl).getBytes( "UTF-8" ) ).toString();
                        links.add(new LinkFromHtmlPage(linkUrl));
                        workIndex = indexOfAnchorEnd;
                    }
                }
            }
            finally {
                reader.close();
            }
        }
        return( links );
    }

}


