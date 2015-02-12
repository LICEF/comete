package ca.licef.comete.metadata;

import java.io.*;
import java.text.*;
import java.util.*;

import proai.MetadataFormat;
import proai.Record;
import proai.SetInfo;
import proai.driver.OAIDriver;
import proai.driver.RemoteIterator;
import proai.error.RepositoryException;
import proai.driver.impl.MetadataFormatImpl;
import proai.driver.impl.RecordImpl;
import proai.driver.impl.RemoteIteratorImpl;
import proai.driver.impl.SetInfoImpl;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.store.Store;
import licef.DateUtil;
import licef.tsapi.model.Tuple;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;

public class OAIDriverImpl implements OAIDriver {

    public OAIDriverImpl() {
    }

    public void init(Properties props) throws RepositoryException {
        if( "com.mckoi.JDBCDriver".equals( props.getProperty( "proai.db.driverClassName" ) ) ) {
            File oaiDir = new File( Core.getInstance().getCometeHome() + "/oai" );
            if( !oaiDir.exists() )
                oaiDir.mkdir();

            File mckoiConfigFile = new File( oaiDir, "proaidb.conf" );
            if( !mckoiConfigFile.exists() ) {
                try {
                    PrintWriter writer = new PrintWriter( new FileWriter( mckoiConfigFile ) );
                    writeFromResourceStream( writer, "/conf/oai/proaidb.conf", true );
                }
                catch( IOException e ) {
                    System.out.println( "Cannot write proaidb.conf file: " + e );
                    e.printStackTrace();
                }
            }
        }
    }

    public void write(PrintWriter out) throws RepositoryException {
        writeFromResourceStream(out, "/conf/oai/identity.xml", false );
    }

    public Date getLatestDate() {
        Date datestamp = new Date( 0 );
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        try {
            String query = Util.getQuery( "oai/getLatestDatestamp.sparql" );
            Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[] { query } );
            Object resp = tripleStore.transactionalCall( inv );

            Tuple[] tuples = (Tuple[])resp;
            if( tuples.length > 0 ) {
                String strDatestamp = tuples[ 0 ].getValue( "latestDatestamp" ).getContent().toString();
                datestamp = datestampFormat.parse( strDatestamp );
            }
        }
        catch( Exception e ) {
            System.out.println( "Cannot retrieve latest datestamp." );
            e.printStackTrace();
        }
        return( datestamp );
    }

    public RemoteIterator<MetadataFormat> listMetadataFormats() {
        return new RemoteIteratorImpl<MetadataFormat>(getMetadataFormatCollection().iterator());
    }

    public RemoteIterator<SetInfo> listSetInfo() {
        return new RemoteIteratorImpl<SetInfo>( getSetInfoCollection().iterator());
    }

    public RemoteIterator<Record> listRecords(Date from, Date until, String mdPrefix) {
        return new RemoteIteratorImpl<Record>(getRecordCollection(from, until, mdPrefix).iterator());
    }

    // In this case, sourceInfo is the full path to the source file.
    public void writeRecordXML(String itemID, String mdPrefix, String sourceInfo, PrintWriter writer) throws RepositoryException {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        MetadataFormat format = formats.get( mdPrefix );
        if( format != null ) {
            String oaiIdentifier = null;
            String datestamp = null;
            try {
                String query = null;
                if( itemID.startsWith( localOaiIdentifierPrefix ) ) {
                    String recordId = Core.getInstance().getUriPrefix() + "/" + itemID.substring( localOaiIdentifierPrefix.length() );
                    query = Util.getQuery( "oai/getRecordTimestamp.sparql", recordId, format.getNamespaceURI() );
                }
                else
                    query = Util.getQuery( "oai/getRecordTimestampFromOaiIdentifier.sparql", itemID, format.getNamespaceURI() );

                Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[] { query } );
                Object resp = tripleStore.transactionalCall( inv );

                Tuple[] tuples = (Tuple[])resp;
                if( tuples.length > 0 )
                    datestamp = tuples[ 0 ].getValue( "datestamp" ).getContent().toString();

                writer.print( "<record>\n" );
                writer.print( "<header>\n" ); 
                writer.print( "<identifier>" + itemID + "</identifier>" );
                writer.print( "<datestamp>" + datestamp + "</datestamp>\n" );
                writer.print( "</header>\n" ); 
                writer.print( "<metadata>\n" ); 

                File file = new File(sourceInfo);
                writeFromFile(file, writer);

                writer.print( "</metadata>\n" ); 
                writer.print( "</record>\n" );
            }
            catch( Exception e ) {
                System.out.println( "Cannot retrieve record header data.  The record will not be generated." );
                e.printStackTrace();
            }
        }
    }

    public void close() {
        // do nothing (this impl doesn't tie up any resources)
    }

    public static void writeFromFile(File file, PrintWriter out) throws RepositoryException {
        try {
            BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(
                                            new FileInputStream(file), "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                out.println(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            throw new RepositoryException("Error reading from file: " + file.getPath(), e);
        }
    }

    private static void writeFromResourceStream(PrintWriter out, String path, boolean closeOutputStream) throws RepositoryException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new InputStreamReader( OAIDriverImpl.class.getResourceAsStream( path ), "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                out.println(line);
                line = reader.readLine();
            }
            reader.close();
            if( closeOutputStream ) {
                out.flush();
                out.close();
            }
        } 
        catch (Exception e) {
            throw new RepositoryException("Error reading resource file " + path + ".", e);
        }
    }

    private Collection<SetInfo> getSetInfoCollection() {
        try {
            List<SetInfo> list = new ArrayList<SetInfo>();

            // Not implemented yet.
           
            return list;
        } catch (Exception e) {
            throw new RepositoryException("Error getting set information", e);
        }
    }

    private Collection<MetadataFormat> getMetadataFormatCollection() {
        List<MetadataFormat> list = new ArrayList<MetadataFormat>();

        MetadataFormat oaiDc = new MetadataFormatImpl( "oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd" );
        formats.put( "oai_dc", oaiDc );

        MetadataFormat lom = new MetadataFormatImpl( "lom", "http://ltsc.ieee.org/xsd/LOM", "http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd" );
        formats.put( "lom", lom );

        list.add( oaiDc );
        list.add( lom );

        return( list );
    }

    private Collection<Record> getRecordCollection(Date from, Date until, String mdPrefix) {
        List<Record> list = new ArrayList<Record>();
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        MetadataFormat format = formats.get( mdPrefix );
        if( format != null ) {
            String formatNamespace = format.getNamespaceURI();
            Date effectiveFromDate = ( from == null ? new Date( 0 ) : from );
            Date effectiveUntilDate = ( until == null ? new Date( Long.MAX_VALUE ) : until ); 
            String strFrom = DateUtil.toISOString( effectiveFromDate, null, null );
            String strUntil = DateUtil.toISOString( effectiveUntilDate, null, null );
            try {
                String query = Util.getQuery( "oai/getRecords.sparql", formatNamespace, strFrom, strUntil );
                Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[] { query } );
                Object resp = tripleStore.transactionalCall( inv );

                Tuple[] tuples = (Tuple[])resp;
                for( int i = 0; i < tuples.length; i++ ) {
                    String recordId = tuples[ i ].getValue( "s" ).getContent().toString();
                    String oaiId = tuples[ i ].getValue( "exposedOaiId" ).getContent().toString();
                    if( "local".equals( oaiId ) )
                        oaiId = getLocalOaiIdentifier( recordId );
                    String location = tuples[ i ].getValue( "location" ).getContent().toString();
                    File sourceInfo = new File( Store.getInstance().getLocation() + location + "/" + Constants.DATASTREAM_ORIGINAL_DATA );
                    Record record = new RecordImpl( oaiId, mdPrefix, sourceInfo );
                    list.add( record );
                }
            }
            catch( Exception e ) {
                System.out.println( "Cannot retrieve records data." );
                e.printStackTrace();
            }
        }
        return list;
    }

    private String getLocalOaiIdentifier( String recordId ) {
        String recordUid = recordId.substring( recordId.lastIndexOf( "/" ) + 1 ); 
        return( localOaiIdentifierPrefix + recordUid );
    }

    private DateFormat datestampFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
    private Map<String,MetadataFormat> formats = new HashMap<String,MetadataFormat>();
    private String localOaiIdentifierPrefix = "oai:" + Core.getInstance().getRepositoryName() + ":";

}
