package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import licef.IOUtil;
import licef.tsapi.TripleStore;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;

public class BrokenLinkChecker implements Runnable {

    private static final int WORKER_COUNT = 10;

    public BrokenLinkChecker() {
    }

    public void start( String notifMail ) throws Exception {
        if( master == null ) {
            this.notifMail = notifMail;

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

            //TripleStore tripleStore = Core.getInstance().getTripleStore();
            //Hashtable<String, String>[] results = tripleStore.getResults( "getRecordLinks.sparql" );
            //for( int i = 0; i < results.length; i++ ) {
            //    String loUri = results[ i ].get( "s" );
            //    String url = Util.manageQuotes( results[ i ].get( "url" ) );
            //    List<String> links = learningObjectLinks.get( loUri );
            //    if( links == null ) {
            //        links = new ArrayList<String>();
            //        learningObjectLinks.put( loUri, links );
            //    }
            //    
            //    links.add( url );
            //    linksToProcess.add( url );
            //}
            totalLinkCount = linksToProcess.size(); 
            brokenLinkCount = 0;

            master = new Thread( this, "BrokenLinkChecker" );
            master.start();
        }
    }

    public void run() {
        try {
            validateLinks();
            writeReport();
            if( notifMail != null && !"".equals( notifMail.trim() ) )
                notifyListener();
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

    // Based from DeadlinkWork class found in ori-oai-indexing module. - FB
    public Result testUrlLocation(URL urlLocation) throws IOException {
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

    private void writeReport() {
        for( int i = 0; i < Constants.UI_LANGUAGES.length; i++ ) {
            String lang = Constants.UI_LANGUAGES[ i ];
            writeReport( lang );
        }
    }

    private void writeReport( String lang ) {
        if( !( new File( Core.getInstance().getCometeReportsHome() ).exists() ) )
            IOUtil.createDirectory( Core.getInstance().getCometeReportsHome() );

        ResourceBundle bundle = ResourceBundle.getBundle( "Strings", new Locale( lang ) );

        PrintWriter writer = null;
        try {
            writer = new PrintWriter( new BufferedWriter( new FileWriter( getReport( lang ) ) ) );

            HtmlCanvas html = new HtmlCanvas( writer );
            html.render( DocType.HTML5 )
                .html()
                    .head()
                        .title().content( bundle.getString( "BrokenLinkReport.HeadTitle" ) )
                        .style().render( new StringResource( "BrokenLinkReport.css" ) )
                        ._style()
                    ._head()
                    .body()
                        .h1().content( MessageFormat.format( bundle.getString( "BrokenLinkReport.Title" ), Core.getInstance().getRepositoryName() ) )
                        .h2().content( MessageFormat.format( bundle.getString( "BrokenLinkReport.GenerationDate" ), new Date() ) )
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

    private void notifyListener() {
        String to = notifMail.trim();

        String fromHost = null;
        try {
            fromHost = new URL( Core.getInstance().getUriPrefix() ).getHost();
        }
        catch( MalformedURLException e ) {
            System.out.println( "Cannot determine exact host so notification is cancelled. " + e );
            return;
        }
        // Useful in development while the domain in the uri prefix is not fully qualified. - FB
        if( fromHost.indexOf( "." ) == -1 )
            fromHost = "comete.licef.ca";

        String from = "comete@" + fromHost;
        String host = Core.getInstance().getSmtpHost();

        Properties properties = System.getProperties();
        properties.setProperty( "mail.smtp.host", host );
        Session session = Session.getDefaultInstance( properties );

        try {
            MimeMessage message = new MimeMessage( session );
            message.setFrom( new InternetAddress( from ) );
            message.addRecipient( Message.RecipientType.TO, new InternetAddress( to ) );
            message.setSubject( "Report of broken links ready." );

            StringBuilder msg = new StringBuilder();
            msg.append( "You have asked to be notified when the report of broken links has been completed.\n " );
            msg.append( "The report has been generated successfully and is available here:\n\n" );
            for( int l = 0; l < Constants.UI_LANGUAGES.length; l++ ) {
                String lang = Constants.UI_LANGUAGES[ l ];
                msg.append( Core.getInstance().getCometeUrl() + "/rest/brokenLinkManager/report?lang=" + lang ).append( "\n" );
            }
            message.setText( msg.toString() );

            Transport.send( message );
        }
        catch( MessagingException mex ) {
            mex.printStackTrace();
        }
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
                    if( result.status >= 300 && result.status < 400 )
                        result = testUrlLocation( new URL( result.redirectionUrl ) );
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

    private String notifMail;

}
