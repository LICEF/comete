package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.Settings;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import licef.IOUtil;
import licef.StringUtil;
import licef.reflection.Invoker;
import licef.tsapi.model.Tuple;
import licef.tsapi.TripleStore;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;

public class BrokenLinkChecker implements Runnable {

    private static final int WORKER_COUNT = 10;

    public BrokenLinkChecker() {
    }

    public static BrokenLinkChecker getInstance() {
        if (instance == null)
            instance = new BrokenLinkChecker();
        return (instance);
    }

    public void start( boolean setBrokenLinkFlag ) throws Exception {
        if( master == null ) {
            this.setBrokenLinkFlag = setBrokenLinkFlag;

            // Remove previous report.
            for( int l = 0; l < Constants.UI_LANGUAGES.length; l++ ) {
                String lang = Constants.UI_LANGUAGES[ l ];
                if( isReportAvailable( lang ) ) {
                    File backupReport = null;
                    int i = 0;
                    do {
                        i++;
                        backupReport = new File( getReport( lang ) + "." + i );
                    }
                    while( backupReport.exists() );

                    if( !getReport( lang ).renameTo( backupReport ) ) {
                        System.out.println( "Could not rename previous report file (" + getReport( lang ) + 
                            ") to backup file (" + backupReport + ").\n\n" +
                            "The broken link validation process cannot start properly and was aborted." );
                        return;
                    }
                }
            }

            learningObjectLinks.clear();
            linksToProcess.clear();

            TripleStore tripleStore = Core.getInstance().getTripleStore();
            String query = Util.getQuery( "metadata/getRecordLinks.sparql" );
            Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[] { query } );
            Tuple[] tuples = (Tuple[])tripleStore.transactionalCall( inv );
            for( int i = 0; i < tuples.length; i++ ) {
                String loUri = tuples[ i ].getValue( "s" ).getContent();
                //String url = Util.manageQuotes( results[ i ].get( "url" ) );
                String url = tuples[ i ].getValue( "url" ).getContent();
                List<String> links = learningObjectLinks.get( loUri );
                if( links == null ) {
                    links = new ArrayList<String>();
                    learningObjectLinks.put( loUri, links );
                }
                
                links.add( url );
                linksToProcess.add( url );
            }
            totalLinkCount = linksToProcess.size(); 
            brokenLinkCount = 0;

            master = new Thread( this, "BrokenLinkChecker" );
            master.start();
        }
    }

    public void run() {
        try {
            validateLinks();
            if( setBrokenLinkFlag )
                markResourcesWithBrokenLinks();
            writeReport();
            try {
                notifyListener();
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
        }
        finally {
            stop();
        }
    }

    public void stop() {
        if( master != null ) {
            if( worker != null ) {
                for( int i = 0; i < worker.length; i++ ) {
                    if( worker[ i ] != null ) {
                        worker[ i ].stop();
                        worker[ i ] = null;
                    }
                }
                worker = null;
            }
            master = null;
        }
    }

    public boolean isRunning() {
        return( master != null );
    }

    public boolean isReportAvailable() {
        for( int i = 0; i < Constants.UI_LANGUAGES.length; i++ ) {
            String lang = Constants.UI_LANGUAGES[ i ];
            if( !isReportAvailable( lang ) )
                return( false );
        }
        return( true );
    }

    public boolean isReportAvailable( String lang ) {
        return( getReport( lang ).exists() );
    }

    public File getReport( String lang ) {
        return( new File( Core.getInstance().getCometeReportsHome(), "brokenLinkReport_" + lang + ".html" ) );
    }

    public synchronized int getProgress() {
        return( totalLinkCount == 0 ? 100 : ( totalLinkCount - linksToProcess.size() ) * 100 / totalLinkCount );
    }

    public Result testUrlLocation(URL urlLocation) throws IOException {
        Result result = testUrlLocationInt( urlLocation );
        if( result.status >= 300 && result.status < 400 )
            result = testUrlLocationInt( new URL( result.redirectionUrl ) );
        return( result ); 
    }

    // Based from DeadlinkWork class found in ori-oai-indexing module. - FB
    private Result testUrlLocationInt(URL urlLocation) throws IOException {
        String redirectionUrl = null;

        // Case of an HTTP URL
        if (urlLocation.getProtocol().equals("http")) {         
            HttpURLConnection urlConnection = (HttpURLConnection)urlLocation.openConnection();          
            redirectionUrl = urlConnection.getHeaderField( "location" );
            return( new Result( urlConnection.getResponseCode(), redirectionUrl ) );    
        }

        // Case of an HTTPS URL
        else if (urlLocation.getProtocol().equals("https")) {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            } };

            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
            }

            // Set at true the HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    // System.out.println("Warning: URL Host: "+urlHostName+"
                    // vs. "+session.getPeerHost());
                    return true;
                }
            };

            HttpsURLConnection.setDefaultHostnameVerifier(hv);

            // Now you can access an https URL without having the certificate in
            // the truststore
            try {
                HttpsURLConnection urlConnectionSecure = (HttpsURLConnection) urlLocation.openConnection();
                redirectionUrl = urlConnectionSecure.getHeaderField( "location" );
                return( new Result( urlConnectionSecure.getResponseCode(), redirectionUrl ) );
            } catch (MalformedURLException e) {
                return( new Result( -1, redirectionUrl ) );
            }
        }
        return( new Result( -1, redirectionUrl ) );
    }

    private void validateLinks() {
        results.clear();
        System.out.println( "Starting validation of " + totalLinkCount + " links: "+ ( new Date() ) );        
        worker = new Worker[ WORKER_COUNT ];
        for( int i = 0; i < worker.length; i++ ) {
            worker[ i ] = new Worker( "BrokenLinkChecker_" + i );
            worker[ i ].start();
        }
        for( int i = 0; i < worker.length; i++ ) {
            try {
                worker[ i ].join();
            }
            catch( InterruptedException e ) {
                e.printStackTrace();
            }
        }
        System.out.println( "Ending validation of links: "+ ( new Date() ) );        
    }

    private void markResourcesWithBrokenLinks() {
        for( Iterator it = learningObjectLinks.keySet().iterator(); it.hasNext(); ) {
            String loUri = (String)it.next();
            List<String> links = learningObjectLinks.get( loUri );
            boolean isBrokenLinkFound = false;
            for( int l = 0; l < links.size(); l++ ) {
                String url = links.get( l );
                String strResult = results.get( url );
                if( strResult != null && strResult.indexOf( "INVALID" ) != -1 ) {
                    isBrokenLinkFound = true;
                    break;
                }
            }
            try {
                Metadata.getInstance().setLearningObjectFlag( loUri, "brokenLink", isBrokenLinkFound ); 
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
        }
    }

    private void writeReport() {
        for( int i = 0; i < Constants.UI_LANGUAGES.length; i++ ) {
            String lang = Constants.UI_LANGUAGES[ i ];
            writeReport( lang );
        }
    }

    private void writeReport( String lang ) {
        if( !( new File( Core.getInstance().getCometeReportsHome() ).exists() ) )
            IOUtil.createDirectory( Core.getInstance().getCometeReportsHome() );

        ResourceBundle bundle = ResourceBundle.getBundle( "translations/Strings", new Locale( lang ) );

        PrintWriter writer = null;
        try {
            writer = new PrintWriter( new BufferedWriter( new FileWriter( getReport( lang ) ) ) );

            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat( "dd/MM/yyyy HH:mm" );

            HtmlCanvas html = new HtmlCanvas( writer );
            html.render( DocType.HTML5 )
                .html()
                    .head()
                        .title().content( bundle.getString( "BrokenLinkReport.HeadTitle" ) )
                        .style().render( new StringResource( "/BrokenLinkReport.css" ) )
                        ._style()
                    ._head()
                    .body()
                        .h1().content( MessageFormat.format( bundle.getString( "BrokenLinkReport.Title" ), Core.getInstance().getRepositoryName() ) )
                        .h2().content( MessageFormat.format( bundle.getString( "BrokenLinkReport.GenerationDate" ), sdf.format( now ) ) )
                        .table( class_( "brokenLinks" ) )
                            .tr()
                                .th().content( bundle.getString( "BrokenLinkReport.ColLabel.LearningObject" ) )
                                .th().content( bundle.getString( "BrokenLinkReport.ColLabel.Link" ) )
                                .th().content( bundle.getString( "BrokenLinkReport.ColLabel.StatusOrError" ) )
                            ._tr();
                            int r = 0;
                            for( Iterator it = learningObjectLinks.keySet().iterator(); it.hasNext(); ) {
                                String loUri = (String)it.next();
                                List<String> links = learningObjectLinks.get( loUri );
                                for( int l = 0; l < links.size(); l++ ) {
                                    String url = links.get( l );
                                    String strResult = results.get( url );
                                    if( strResult != null && strResult.indexOf( "INVALID" ) != -1 ) {
                                        String loUriNumber = loUri.substring( loUri.lastIndexOf( "/" ) + 1 );
                                        String error = strResult.substring( strResult.indexOf( "(" ) + 1, strResult.indexOf( ")" ) );
                                        if( "-1".equals( error ) )
                                            error = bundle.getString( "BrokenLinkReport.NotAppl" );

                                        html.tr( class_( r % 2 == 0 ? "even" : "odd" ) );
                                        
                                        if( l == 0 )
                                            html.td().a( href( loUri ).target( "_blank" ) ).content( loUriNumber )._td();
                                        else
                                            html.td().br()._td();

                                        html.td().a( href( url ).target( "_blank" )).content( url )._td().
                                            td().content( error )
                                        ._tr();
                                        r++;
                                    }
                                }
                            }
                        html._table()
                        .p().write( MessageFormat.format( bundle.getString( "BrokenLinkReport.TotalLinks" ), totalLinkCount ) ).br()
                        .write( MessageFormat.format( bundle.getString( "BrokenLinkReport.TotalBrokenLinks" ), brokenLinkCount ) )._p()
                    ._body()
                ._html();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
        finally {
            if( writer != null ) {
                writer.flush();
                writer.close();
            }
        }
    }

    private void notifyListener() throws Exception {
        Object[] notifSettings = Settings.getNotificationSettings();
        String notifEmail = (String)notifSettings[ 0 ];
        boolean isNotifNeeded = (boolean)notifSettings[ 1 ]; 

        if( !isNotifNeeded || StringUtil.isEmpty( notifEmail ) )
            return;

        StringBuilder msg = new StringBuilder();
        msg.append( "You have asked to be notified when the report of broken links has been completed.\n " );
        msg.append( "The report has been generated successfully and is available here:\n\n" );
        for( int l = 0; l < Constants.UI_LANGUAGES.length; l++ ) {
            String lang = Constants.UI_LANGUAGES[ l ];
            msg.append( Core.getInstance().getCometeUrl() + "/rest/brokenLinkManager/report?lang=" + lang ).append( "\n" );
        }

        Util.sendMail( Util.getSystemEmailFromValue(), notifEmail, "Report of broken links ready.", msg.toString() );
    }

    private synchronized void incrementBrokenLinkCount() {
        brokenLinkCount++;
    }

    private synchronized void storeLinkResult( String link, String result ) {
        results.put( link, result );
    }

    class Result {
       
        public Result( int status, String redirectionUrl ) {
            this.status = status;
            this.redirectionUrl = redirectionUrl;
        }

        public int status;
        public String redirectionUrl;

    }

    class Worker implements Runnable {

        public Worker( String name ) {
            thread = new Thread( Worker.this, name );
        }

        public void start() {
            if( thread != null )
                thread.start();
        }

        public void stop() {
            if( thread != null ) {
                thread.stop();
                thread = null;
            }
        }

        public void join() throws InterruptedException {
            if( thread != null )
                thread.join();
        }

        public void run() {
            for( ;; ) {
                String link = null;
                String strRes = null;
                try {
                    link = linksToProcess.poll();
                    if( link == null )
                        break;

                    Result result = testUrlLocation( new URL( link ) ); 
                    boolean isValid = ( result.status == 200 );
                    if( isValid )
                        strRes = "VALID";
                    else {
                        strRes = "INVALID (" + result.status + ")";
                        incrementBrokenLinkCount();
                    }
                }
                catch( Exception e ) {
                    strRes = "INVALID (" + e + ")";
                    incrementBrokenLinkCount();
                }
                finally {
                    if( strRes != null )
                        storeLinkResult( link, strRes );
                }
            }
        }

        private Thread thread;

    }

    private Queue<String> linksToProcess = new ConcurrentLinkedQueue();
    private Map<String,List<String>> learningObjectLinks = new HashMap<String,List<String>>();
    private int totalLinkCount;
    private int brokenLinkCount;
    private Map<String,String> results = new ConcurrentHashMap<String,String>();

    private Worker[] worker;
    private Thread master;

    private boolean setBrokenLinkFlag;
    private String notifMail;

    private static BrokenLinkChecker instance;

}
