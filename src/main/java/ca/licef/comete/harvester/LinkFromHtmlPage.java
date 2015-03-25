package ca.licef.comete.harvester;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import licef.XMLUtil;

public class LinkFromHtmlPage {

    public LinkFromHtmlPage(String url) {
        this.url = url;
    }

    public Document getRecord() throws IOException, ParserConfigurationException, IllegalStateException, SAXException {
        if( doc == null ) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet get = new HttpGet( url );
            HttpResponse response = httpclient.execute( get );
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware( true );
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    doc = builder.parse( entity.getContent() );
                }
                catch( SAXParseException e ) {
                    doc = getRecordWithMalformedXml();
                }

                // Try to initialize the datestamp.
                initDatestamp( response );
            }
        }

        return( doc );
    }

    public boolean isAvailable() throws IOException, ParserConfigurationException, IllegalStateException, SAXException {
        String xpath = "//lom:lom/lom:lifeCycle/lom:status/lom:value";
        NodeList statusNodes = XMLUtil.getNodeList( getRecord(), xpath );
        for( int i = 0; i < statusNodes.getLength(); i++ ) {
            Node statusNode = statusNodes.item( i );
            if( "unavailable".equals( statusNode.getTextContent() ) )
                return( false );
        }
        return( true );
    }

    public String getVersion() throws IOException, ParserConfigurationException, IllegalStateException, SAXException {
        String version = null;
        String xpath = "//lom:lom/lom:lifeCycle/lom:version/lom:string[last()]";
        NodeList versionNodes = XMLUtil.getNodeList( getRecord(), xpath );
        for( int i = 0; i < versionNodes.getLength(); i++ ) {
            Node versionNode = versionNodes.item( i );
            version = versionNode.getTextContent();
        }
        return( version );
    }

    /*
     * As it's the getRecord() method that initializes datestamp, getLatestDatestamp() must always be called after getRecord(). - FB
     */
    public String getLatestDatestamp() throws IOException, ParserConfigurationException, IllegalStateException, SAXException {
        List<String> strIsoDates = new ArrayList<String>();

        if( datestamp != null )
            strIsoDates.add( datestamp ); 

        String[] xpath = new String[] {
            "//lom:lom/lom:lifeCycle/lom:contribute/lom:date/lom:dateTime", // 2.3.3
            "//lom:lom/lom:metaMetadata/lom:contribute/lom:date/lom:dateTime", // 3.2.3
            "//lom:lom/lom:annotation/lom:date/lom:dateTime" // 8.2
        };
        for( int xp = 0; xp < xpath.length; xp++ ) {
            NodeList dateTimeNodes = XMLUtil.getNodeList( getRecord(), xpath[ xp ] );
            for( int i = 0; i < dateTimeNodes.getLength(); i++ ) {
                Node childNode = dateTimeNodes.item( i );
                if( childNode != null ) {
                    String strDate = childNode.getTextContent();
                    strIsoDates.add( strDate );
                }
            }
        }

        Calendar latestDate = null;
        for( String strIsoDate : strIsoDates ) {
            try {
                Calendar cal = DateParser.parse( strIsoDate );
                if( latestDate == null )
                    latestDate = cal;
                else {
                    if( cal.compareTo( latestDate ) > 0 ) 
                        latestDate = cal;
                }
            }
            catch( InvalidDateException ignore ) {
                // Ignore date that we cannot parse.
            }
        }

        return( latestDate == null ? null : DateParser.getIsoDate( latestDate ) );
    }

    public String toString() {
        return this.url;
    }

    public String getUrl() {
        return this.url;
    }

    private void initDatestamp( HttpResponse response ) {
        if( response.containsHeader( "Last-Modified" ) ) {
            try {
                Header lastModifHeader = response.getFirstHeader( "Last-Modified" );
                Date lastModif = lastModifFormat.parse( lastModifHeader.getValue() );
                Calendar cal = Calendar.getInstance();
                cal.setTime( lastModif );
                datestamp = DateParser.getIsoDate( cal );
            }
            catch( ParseException e ) {
                // Too bad so datestamp will not be considered.
            }
        }
    }

    private Document getRecordWithMalformedXml() throws IOException, ParserConfigurationException, IllegalStateException, SAXException {
        Document document = null;
        String strContent = getRecordAsString();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware( true );

      parseConcent:

        for( ;; ) {
            if( strContent != null ) {
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    document = builder.parse( new InputSource( new StringReader( strContent ) ) );
                    break;
                }
                catch( SAXParseException e ) {
                    Matcher m = patternEntityWithoutSemicolon.matcher( e.getMessage() );
                    if( m.find() ) {
                        String textEntity = m.group( 1 );
                       
                        strContent = strContent.replaceAll( "&" + textEntity, "&amp;" + textEntity );
                        continue parseConcent;
                    }
                   
                    m = patternEntityWithoutName.matcher( e.getMessage() );
                    if( m.find() ) {
                        strContent = strContent.replaceAll( "&", "&amp;" );
                        continue parseConcent;
                    }

                    throw( e );
                }
            }
        }
        
        return( document );
    }

    private String getRecordAsString() throws IOException, UnsupportedEncodingException {
        String encoding = getRecordEncoding();

        if( encoding != null ) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet get = new HttpGet( url );
            HttpResponse response = httpclient.execute( get );
            HttpEntity entity = response.getEntity();
            if( entity != null ) {
                String str = EntityUtils.toString( entity, encoding );
                return( str );
            }
        }

        return( null );
    }

    private String getRecordEncoding() throws IOException {
        if( encoding == null ) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet get = new HttpGet( url );
            HttpResponse response = httpclient.execute( get );
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BufferedInputStream bis = new BufferedInputStream( entity.getContent() );
                encoding = XMLUtil.detectEncoding( bis );
            }
        }
        return( encoding );
    }

    private Pattern patternEntityWithoutSemicolon = Pattern.compile( "The reference to entity \"(.+?)\" must end with the ';' delimiter." );
    private Pattern patternEntityWithoutName = Pattern.compile( "The entity name must immediately follow the '&' in the entity reference." );

    private String url;
    private Document doc;
    private String encoding;
    private String datestamp;

    private static SimpleDateFormat lastModifFormat = new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss zzz" );

}

