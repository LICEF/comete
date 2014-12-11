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
import ca.licef.comete.core.util.Util;
import licef.tsapi.model.Tuple;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;


/**
 * An simple OAIDriver for testing/demonstration purposes.
 *
 * The directory should contain the following files:
 *
 * identity.xml
 * records/
 *   item1-oai_dc-2005-01-01T08-50-44.xml
 * sets/
 *   abovetwo.xml
 *   abovetwo-even.xml
 *   abovetwo-odd.xml
 *   prime.xml
 * formats/
 *   oai_dc.txt
 *     line1: ns
 *     line2: loc
 */
public class OAIDriverImpl implements OAIDriver {

    public static final String BASE_DIR_PROPERTY = "proai.driver.simple.baseDir";

    public static final String RECORDS_DIRNAME   = "records";
    public static final String SETS_DIRNAME      = "sets";

    private File m_recordsDir;
    private File m_setsDir;

    public OAIDriverImpl() {
    }

    public OAIDriverImpl(File dir) throws RepositoryException {
        Properties props = new Properties();
        props.setProperty(BASE_DIR_PROPERTY, dir.getPath());
        init(props);
    }

    public void init(Properties props) throws RepositoryException {
        String baseDir = props.getProperty(BASE_DIR_PROPERTY);
        if (baseDir == null) {
            throw new RepositoryException("Required property is not set: " 
                    + BASE_DIR_PROPERTY);
        }
        File dir = new File(baseDir);
        m_recordsDir   = new File(dir, RECORDS_DIRNAME);
        m_setsDir      = new File(dir, SETS_DIRNAME);
        if (!dir.exists()) {
            throw new RepositoryException("Base directory does not exist: " 
                    + dir.getPath());
        }
        if (!m_recordsDir.exists()) {
            throw new RepositoryException("Records directory does not exist: " 
                    + m_recordsDir.getPath());
        }
        if (!m_setsDir.exists()) {
            throw new RepositoryException("Sets directory does not exist: " 
                    + m_setsDir.getPath());
        }
    }

    public void write(PrintWriter out) throws RepositoryException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new InputStreamReader( getClass().getResourceAsStream( "/identity.xml" ), "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                out.println(line);
                line = reader.readLine();
            }
            reader.close();
        } 
        catch (Exception e) {
            throw new RepositoryException("Error reading identity file.", e);
        }
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
        return new RemoteIteratorImpl<SetInfo>(
                getSetInfoCollection().iterator());
    }

    public RemoteIterator<Record> listRecords(Date from, 
                                      Date until, 
                                      String mdPrefix) {
        return new RemoteIteratorImpl<Record>(getRecordCollection(from,
                                                          until,
                                                          mdPrefix).iterator());
    }

    // In this case, sourceInfo is the full path to the source file.
    public void writeRecordXML(String itemID,
                               String mdPrefix,
                               String sourceInfo,
                               PrintWriter writer) throws RepositoryException {

        File file = new File(sourceInfo);
        writeFromFile(file, writer);
    }

    public void close() {
        // do nothing (this impl doesn't tie up any resources)
    }

    public static void writeFromFile(File file, 
                                     PrintWriter out) throws RepositoryException {
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

    private Collection<SetInfo> getSetInfoCollection() {
        try {
            List<SetInfo> list = new ArrayList<SetInfo>();
            String[] names = m_setsDir.list();
            for (int i = 0; i < names.length; i++) {
                if (names[i].endsWith(".xml")) {
                    String spec = names[i].split("\\.")[0].replaceAll("-", ":");
                    list.add(new SetInfoImpl(spec, new File(m_setsDir, 
                                                            names[i])));
                }
            }
            return list;
        } catch (Exception e) {
            throw new RepositoryException("Error getting set information", e);
        }
    }

    private Collection<MetadataFormat> getMetadataFormatCollection() {
        List<MetadataFormat> list = new ArrayList<MetadataFormat>();
        MetadataFormat oaiDc = new MetadataFormatImpl( "oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd" );
        MetadataFormat lom = new MetadataFormatImpl( "lom", "http://ltsc.ieee.org/xsd/LOM", "http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd" );
        list.add( oaiDc );
        list.add( lom );
        return( list );
    }

    private Collection<Record> getRecordCollection(Date from, 
                                           Date until, 
                                           String mdPrefix) {
        List<Record> list = new ArrayList<Record>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
        String[] names = m_recordsDir.list();
        for (int i = 0; i < names.length; i++) {
            String[] temp = names[i].replaceFirst("-", " ")
                                    .replaceFirst("-", " ")
                                    .split(" ");
            if (temp.length == 3 && temp[2].indexOf(".") != -1) {
                String[] parts = names[i].split("-");
                if (parts[1].equals(mdPrefix)) {
                    try {
                        long recDate = df.parse(temp[2].substring(0, temp[2].indexOf("."))).getTime();
                        if ( (from == null || from.getTime() < recDate)
                                && (until.getTime() >= recDate) ) {
                            String itemID = "oai:example.org:" + parts[0];
                            list.add(new RecordImpl(itemID,
                                                    mdPrefix,
                                                    new File(m_recordsDir,
                                                             names[i])));
                        }
                    } catch (Exception e) { 
                        System.out.println("WARNING: Ignoring unparsable filename: " 
                                           + names[i]);
                    }
                }
            }
        }
        return list;
    }

    private DateFormat datestampFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );

}
