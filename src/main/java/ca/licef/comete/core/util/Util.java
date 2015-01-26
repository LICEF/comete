package ca.licef.comete.core.util;

import ca.licef.comete.core.Core;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.Vocabulary;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Property;
import licef.*;
import licef.jrdf.JRDFFactory;
import licef.jrdf.SortedMemoryJRDFFactory;
import licef.jrdf.collection.MemMapFactory;
import licef.jrdf.graph.Graph;
import licef.jrdf.graph.TripleFactory;
import licef.jrdf.writer.*;
import licef.jrdf.writer.rdfxml.RdfXmlWriter;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import licef.tsapi.vocabulary.RDFS;
import licef.tsapi.vocabulary.SKOS;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 8-Sep-2011
 */
public class Util {

    public static String readString(InputStream is) throws IOException {
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            contents.append(line);
            contents.append(System.getProperty("line.separator"));
        }
        reader.close();
        return contents.toString();
    }

    public static void writeString(OutputStream os, String text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(text);
        writer.flush();
        writer.close();
    }

    /* ID conversion and management*/

    public static String getNewId() {
        return UUID.randomUUID().toString();
    }

    public static String getIdNumberValue(String id) {
        String[] vals = StringUtil.split(id, '/');
        String[] val = StringUtil.split(vals[vals.length - 1], ':');
        return val[val.length - 1];
    }

    public static String makeURI(String type) {
        return makeURI(getNewId(), type);
    }

    public static String makeURI(String id, String type) {
        if (id.startsWith("http://"))
            return id;
        String typeVal = getTypeLabel(type);
        return Core.getInstance().getUriPrefix() + "/" + typeVal + "/" + getIdNumberValue(id);
    }

    public static String makeURI(OntClass _class) {
        return makeURI(getNewId(), _class);
    }

    public static String makeURI(String id, OntClass _class) {
        return makeURI(id, _class.getURI());
    }

    /**
     * Returns a condensed form of an uri
     * @param uri
     * example : "http://server/ld/resource/person/3" returns "person:3"
     */
    public static String makeCondensedFormat(String uri) {
        if (!uri.startsWith("http"))
            return uri;
        String type = getURIType(uri);
        String val = getIdNumberValue(uri);
        return getTypeLabel(type) + ":" +val;
    }

    public static String makeUriFromCondensedFormat(String id) {
        if (id.startsWith("http"))
            return id;
        String[] vals = StringUtil.split(id, ':');
        return makeURI(vals[1], getTypeFromLabel(vals[0]));
    }

    public static String getTypeLabel(String type) {
        String typeVal = null;
        if (COMETE.MetadataRecord.getURI().equals(type))
            typeVal = Constants.OBJ_TYPE_METADATA_RECORD;
        else if (COMETE.LearningObject.getURI().equals(type))
            typeVal = Constants.OBJ_TYPE_LEARNING_OBJECT;
        else if (COMETE.Person.getURI().equals(type))
            typeVal = Constants.OBJ_TYPE_PERSON;
        else if (COMETE.Organization.getURI().equals(type))
            typeVal = Constants.OBJ_TYPE_ORGANIZATION;
        else if (COMETE.Repository.getURI().equals(type))
            typeVal = Constants.OBJ_TYPE_REPOSITORY;
        else if (COMETE.VocContext.getURI().equals(type))
            typeVal = Constants.OBJ_TYPE_VOC_CONTEXT;
        return typeVal;
    }

    public static String getRestUrl(OntClass type) {
        return getRestUrl(type.getURI());
    }

    public static String getRestUrl(String type) {
        String url = null;
        if (COMETE.MetadataRecord.getURI().equals(type))
            url = "rest/metadataRecords";
        else if (COMETE.LearningObject.getURI().equals(type))
            url = "rest/learningObjects";
        else if (COMETE.Identity.getURI().equals(type))
            url = "rest/identities";
        else if (COMETE.Person.getURI().equals(type))
            url = "rest/persons";
        else if (COMETE.Organization.getURI().equals(type))
            url = "rest/organizations";
        else if (COMETE.Repository.getURI().equals(type))
            url = "rest/repositories";
        else if (COMETE.VocContext.getURI().equals(type))
            url = "rest/vocContexts";
        else if (SKOS.ConceptScheme.getURI().equals(type))
            url = "rest/voc";

        return url;
    }

    public static String getURIType(String uri) {
        String prefix = Core.getInstance().getUriPrefix();
        int indexOfPrefix = uri.indexOf( prefix );
        if( indexOfPrefix == -1 )
            return( null );

        String uriWithoutPrefix = uri.substring( indexOfPrefix + prefix.length() );
        String[] vals = uriWithoutPrefix.split( "/" );
        if( vals.length < 2 )
            return( null );
        String label = vals[1];
        if( "voc".equals( label ) ) {
            if( vals.length == 4 )
                return( SKOS.ConceptScheme.getURI() );
            else if( vals.length == 5 )
                return( SKOS.Concept.getURI() );
            else
                return( null );
        }
        return getTypeFromLabel(label);
    }

    public static String getTypeFromLabel(String label) {
        String type = null;
        if ("metadatarecord".equals(label))
            type = COMETE.MetadataRecord.getURI();
        else if ("learningobject".equals(label))
            type = COMETE.LearningObject.getURI();
        else if ("person".equals(label))
            type = COMETE.Person.getURI();
        else if ("organization".equals(label))
            type = COMETE.Organization.getURI();
        else if ("repository".equals(label))
            type = COMETE.Repository.getURI();
        else if ("voccontext".equals(label))
            type = COMETE.VocContext.getURI();
        return type;
    }

    public static String getReportDataStream( String profileUri ) throws Exception {
        if( Constants.APPL_PROF_LOM_STRICT.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_LOM_STRICT );
        else if( Constants.APPL_PROF_LOM_LOOSE.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_LOM_LOOSE );
        else if( Constants.APPL_PROF_LOM_FR_1_0.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_LOM_FR );
        else if( Constants.APPL_PROF_SCO_LOM_FR_1_0.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_SCO_LOM_FR_1_0 );
        else if( Constants.APPL_PROF_SCO_LOM_FR_1_1.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_SCO_LOM_FR_1_1 );
        else if( Constants.APPL_PROF_LOM_NORMETIC_1_2.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_LOM_NORMETIC_1_2 );
        else if( Constants.APPL_PROF_OAI_DC.equals( profileUri ) )
            return( Constants.DATASTREAM_VALIDATION_ERRORS_OAI_DC );
        else
            throw( new Exception( "Unknown application profile: " + profileUri + "." ) );
    }

    public static String buildFilterConstraints(String[] values, String varName, boolean resourceValues, String test, String delimiter) {
        return buildFilterConstraints(Arrays.asList(values), varName, resourceValues, test, delimiter);
    }

    public static String buildFilterConstraints(List<String> values, String varName, boolean resourceValues, String test, String delimiter) {
        String _delimiter = "";
        StringBuilder constraints = new StringBuilder();
        for (Iterator it = values.iterator(); it.hasNext();) {
            String val = it.next().toString();
            constraints.append( _delimiter );
            constraints.append( "?" ).append( varName ).append( " " ).append( test );
            if (resourceValues)
                constraints.append( " <" ).append( val ).append( ">" );
            else
                constraints.append( " \"" ).append( val ).append( "\"" );
            _delimiter = " " + delimiter + " ";
        }
        return( constraints.toString() );
    }

    public static List<String> buildList( Hashtable<String, String>[] results, String varName ) throws Exception{
        ArrayList<String> list = new ArrayList<String>();
        for (Hashtable<String, String> lo : results) {
            list.add(lo.get(varName));
        }
        return list;
    }

    /*
     * @param queryId Name of the query that corresponds to a file name in the query resource directory.
     * @param params Parameters which their value will be substituted in the query.  Beware, numeric values should be 
     * converted to string before the call to prevent locale-dependant formatting.
     */
    public static String getQuery( String queryId, Object... params ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream( baos );
        InputStream is = Core.getInstance().getClass().getResourceAsStream(Constants.QUERY_BASE_LOCATION + "/" + queryId );

        BufferedInputStream bis = new BufferedInputStream( is );
        try {
            IOUtil.copy( bis, bos );
        }
        finally {
            bis.close();
            bos.close();
        }
        String rawQuery = baos.toString( "UTF-8" );
        if( params == null || params.length == 0 )
            return( rawQuery );

        String query = MessageFormat.format( rawQuery, params );
        return( query );
    }

    public static String buildRange( int offset, int limit ) {
        StringBuilder str = new StringBuilder();
        if( offset != -1 )
            str.append( "OFFSET " ).append( offset ).append( " " );
        if( limit != -1 )
            str.append( "LIMIT " ).append( limit );
        return( str.toString() );
    }

    public static String[] getResourceLabel(String uri) throws Exception {
        return getResourceLabel(uri, null, false);
    }

    public static String[] getResourceLabel(String uri, String lang) throws Exception {
        return getResourceLabel(uri, lang, false);
    }

    public static String[] getResourceLabel(String uri, String lang, boolean forceVocType) throws Exception {
        lang = LangUtil.convertLangToISO2(lang);
        String predicate = RDFS.label.getURI(); //default case
        String graph = null;
        //vocabulary concept case
        String type = Util.getURIType(uri);
        if (type == null && !forceVocType)
            return( new String[] { uri, null } );

        if (forceVocType ||
            SKOS.ConceptScheme.getURI().equals(type) ||
            SKOS.Concept.getURI().equals(type) ) {
            //predicate = SKOS.prefLabel.getURI();
            predicate = RDFS.label.getURI(); //preferred
            graph = Vocabulary.getInstance().getConceptScheme(uri);
        }
        else if (type.equals(COMETE.LearningObject.getURI()))
            predicate = DCTERMS.title.getURI();
        else if (type.equals(COMETE.Person.getURI()))
            predicate = FOAF.name.getURI();
        else if (type.equals(COMETE.Organization.getURI()))
            predicate = FOAF.name.getURI();
        else if (type.equals(COMETE.Repository.getURI()))
            predicate = FOAF.name.getURI();

        String[] label = Core.getInstance().getTripleStore().getBestLocalizedLiteralObject( uri, predicate, lang, graph );

        if (label == null || label[ 0 ] == null || "".equals(label[ 0 ]))
            return( new String[] { uri, null } );
        return label;
    }

    /*
     * Metadatas
     */
    public static String generateMetaMetadataIdentifier( String recordId, String metadataFormat ) {
        StringBuilder str = new StringBuilder();
        str.append( "urn:" );
//        str.append( Core.getInstance().getRepositoryNamespace() ).append( ":" );
        str.append( recordId ).append( ":" );
        str.append( metadataFormat );
        return( str.toString() );
    }

    public static String generateGeneralIdentifier( String recordId ) {
        StringBuilder str = new StringBuilder();
        str.append( "urn:" );
//        str.append( Core.getInstance().getRepositoryNamespace() ).append( ":" );
        str.append( recordId );
        return( str.toString() );
    }


    public static String applyXslToDocument( String xsltBaseFilename, StreamSource doc, Properties transformerProps, HashMap<String,String> params, Locale locale ) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, TransformerConfigurationException, TransformerException {
        if( locale != null ) {
            if( "fr".equals( locale.getLanguage() ) )
                xsltBaseFilename = xsltBaseFilename + "_" + locale.getLanguage();
        }
        String xsltFile = "/xslt/" + xsltBaseFilename + ".xsl";
        StreamSource xslt = new StreamSource( Util.class.getResourceAsStream( xsltFile ) );
        return( XMLUtil.applyXslToDocument( xslt, doc, resolver, transformerProps, params, "net.sf.saxon.TransformerFactoryImpl" ) ); 
    }

    public static String applyXslToDocument( String xsltBaseFilename, StreamSource doc, HashMap<String,String> params, Locale locale ) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, TransformerConfigurationException, TransformerException {
        return( applyXslToDocument( xsltBaseFilename, doc, null, params, locale ) ); 
    }

    public static String applyXslToDocument( String xsltBaseFilename, StreamSource doc, HashMap<String,String> params ) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, TransformerConfigurationException, TransformerException {
        return( applyXslToDocument( xsltBaseFilename, doc, null, params, null ) );
    }

    public static String applyXslToDocument( String xsltBaseFilename, StreamSource doc ) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, TransformerConfigurationException, TransformerException {
        return( applyXslToDocument( xsltBaseFilename, doc, null, null, null ) );
    }

    public static String getTriplesAsRdf( Collection<Triple> triples ) throws Exception {
        Triple[] tripleArray = triples.toArray( new Triple[ triples.size() ] );
        return( getTriplesAsRdf( tripleArray ) );
    }

    public static String getTriplesAsRdf( Triple[] tripleArray ) throws Exception {
        Graph graph = makeJrdfGraph( tripleArray );

        BlankNodeRegistry nodeRegistry = new MappedBlankNodeRegistry( new MemMapFactory() );
        RdfWriter writer = new RdfXmlWriter( nodeRegistry, getRdfNamespaceMap() );
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream( os );
        try {
            try {
                writer.write( graph, bos );
            } finally {
                writer.close();
            }
        } finally {
            bos.close();
        }
        String rdf = os.toString( "UTF-8" );
        StreamSource source = new StreamSource( new BufferedReader( new StringReader( rdf ) ) );
        String prettyRdf = applyXslToDocument( "removeUnusedNamespaces", source );

        return( prettyRdf );
    }

    private static RdfNamespaceMap rdfNamespaceMap;

    private static RdfNamespaceMap getRdfNamespaceMap() {
        if( rdfNamespaceMap == null ) {
            MemMapFactory mapFactory = new MemMapFactory();
            rdfNamespaceMap = new RdfNamespaceMapImpl( mapFactory );
            System.setProperty( RdfXmlWriter.WRITE_LOCAL_NAMESPACE, "true" );
            for( Iterator it = CommonNamespaceContext.getInstance().getAllPrefixes(); it.hasNext(); ) {
                String prefix = (String)it.next();
                String namespace = CommonNamespaceContext.getInstance().getNamespaceURI( prefix );
                try {
                    rdfNamespaceMap.addNamespace( prefix, namespace );
                }
                catch( NamespaceException ignore ) {
                    // Ignore namespace that we cannot add.
                }
            }
        }
        return( rdfNamespaceMap );
    }

    private static Graph makeJrdfGraph( Triple[] tripleArray ) {
        JRDFFactory jrdfFactory = SortedMemoryJRDFFactory.getFactory();
        Graph graph = jrdfFactory.getGraph();
        TripleFactory tripleFactory = graph.getTripleFactory();

        for( Triple triple : tripleArray ) {
            try {
                URI subject = new URI( triple.getSubject() );
                URI predicate = new URI( triple.getPredicate() );
                if( triple.isObjectLiteral() ) {
                    String literal = triple.getObject();
                    if( triple.getLanguage() == null )
                        tripleFactory.addTriple( subject, predicate, literal );
                    else
                        tripleFactory.addTriple( subject, predicate, literal, triple.getLanguage() );
                }
                else {
                    URI object = new URI( triple.getObject() );
                    tripleFactory.addTriple( subject, predicate, object );
                }
            }
            catch( URISyntaxException ignore ) {
                // The invalid triple is not added to the graph.
            }
        }

        return( graph );
    }

    private static URIResolver resolver = new URIResolver() {
        public Source resolve( String href, String base ) {
            InputStream is = getClass().getResourceAsStream( href );
            if( is == null )
                is = getClass().getResourceAsStream( "/" + href );
            if( is == null )
                is = getClass().getResourceAsStream( "/xslt/" + href );

            return( new StreamSource( is ) );
        }
    };

    public static String encodeSingleQuote(String str) {
        str = str.replaceAll("\\\\'", "'"); //first
        str = str.replaceAll("\\\\'", " \'"); //2nd step
        return str.replaceAll("'", "\\\\'");
    }

    public static boolean isDate(String str) {
        return (str.endsWith("^^" + Constants.XSD_DATE_TYPE));
    }

    public static String manageDateString(String str) {
        if (isDate(str))
            str = manageQuotes( str.substring(0, str.indexOf("^^" + Constants.XSD_DATE_TYPE)) );
        return str;
    }

    public static String[] manageLocalizedString(String str) {
        String content = str;
        String lang = null;
        Matcher m = localizedStringPattern.matcher( str );
        if (m.find()) {
            int i = str.lastIndexOf('@');
            content = str.substring(0, i);
            lang = str.substring(i + 1);
        } 
        content = manageQuotes(content);
        return new String[]{content, lang};
    }

    public static String manageQuotes(String str) {
        str = StringUtil.unquote(str);
        return str.replace("\\\"", "\"");
    }

    public static String formatLanguage(String lang) {
        String[] vals  = StringUtil.split(lang, '-');
        String res = vals[0].substring(0, 2);
        if (vals.length == 2)
            res += "-" + vals[1].substring(0, 2);
        return res;
    }

    public static Dimension getPhotoDimension( String location ) {
        ImageInputStream in = null;
        try {
            in = ImageIO.createImageInputStream( new URL( location ).openStream() );
            final Iterator readers = ImageIO.getImageReaders( in );
            if( readers.hasNext() ) {
                ImageReader reader = (ImageReader)readers.next();
                try {
                    reader.setInput( in );
                    return( new Dimension( reader.getWidth( 0 ), reader.getHeight( 0 ) ) );
                }
                finally {
                    reader.dispose();
                }
            }
        }
        catch( IOException e ) {
            return( null );
        }
        finally {
            if( in != null ) {
                try {
                    in.close();
                }
                catch( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
        return( null );
    }

    public static String formatKeywords(String keywords) {
        String quoteEsc = "SINGLEQUOTEREGEXESC";
        String dblQuoteEsc = "DBLQUOTEREGEXESC";
        keywords = keywords.replace("\'", quoteEsc);
        keywords = keywords.replace("\"", dblQuoteEsc);

        int index = keywords.indexOf(dblQuoteEsc);
        int cp = 0;
        while (index != -1) {
            cp++;
            index = keywords.indexOf(dblQuoteEsc, index + 1);
        }
        if ((cp % 2) != 0)
            keywords += dblQuoteEsc;

        String keywordsFormattedForRegex = keywords.replaceAll( "[^\\p{L}\\p{N}]", " " );

        //I think it can be removed... -AM
        keywordsFormattedForRegex = keywordsFormattedForRegex.replace( dblQuoteEsc, "\\\"" );
        keywordsFormattedForRegex = keywordsFormattedForRegex.replace( quoteEsc, "\\\'" );

        return keywordsFormattedForRegex;
    }

    public static String getSyntaxHighlightedCode( String language, String code ) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, TransformerConfigurationException, ParserConfigurationException, TransformerException {
        File tempSyntaxHighlighterFile = createSyntaxHighlighterFile( language, code );

        HashMap<String,String> params = new HashMap<String,String>();
        String configFileLocation = getSyntaxHighlighterConfigFileLocation();
        if( configFileLocation != null )
            params.put( "xslthl.config", configFileLocation );

        StreamSource source = new StreamSource( new BufferedInputStream( new FileInputStream( tempSyntaxHighlighterFile ) ) );
        String result = Util.applyXslToDocument( "highlightSyntax", source, params ); 

        if( !tempSyntaxHighlighterFile.delete() )
            System.err.println( "Cannot delete temp file: " + tempSyntaxHighlighterFile );

        return( result );
    }

    private static String getSyntaxHighlighterConfigFileLocation() {
        if( syntaxHighlighterConfigFileLocation == null ) {
            try {
                File configFile = new File( System.getProperty( "java.io.tmpdir" ), "xslthl-config.xml" );

                if( configFile.exists() ) {
                    if( !configFile.delete() )
                        System.err.println( "Cannot delete temp file: " + configFile );
                }

                InputStream is = Util.class.getResourceAsStream( "/xslthl-config.xml" );
                OutputStream os = new FileOutputStream( configFile );
                try {
                    IOUtil.copy( is, os );
                }
                finally {
                    if( os != null )
                        os.close();
                    if( is != null )
                        is.close();
                }

                syntaxHighlighterConfigFileLocation = ( new URL( "file:///" + configFile ) ).toString();
            }
            catch( IOException e ) {
                // Syntax highlighting will not work.
                return( null );
            }
        }
        return( syntaxHighlighterConfigFileLocation );
    }

    private static File createSyntaxHighlighterFile( String language, String code ) throws IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().newDocument();

        Element rootElement = doc.createElement( "document" );
        doc.appendChild( rootElement );

        Element codeElement = doc.createElement( "code" );
        codeElement.setAttribute( "language", language );
        CDATASection xmlAsCData = doc.createCDATASection( code );
        codeElement.appendChild( xmlAsCData );

        rootElement.appendChild( codeElement );

        String inputData = XMLUtil.getXMLString( doc );

        File outputFile = File.createTempFile( "syntaxHighlighting", ".xml" );
        IOUtil.writeStringToFile( inputData, outputFile );
        return( outputFile );
    }

    public static String getMimeTypeIcon( String mimeType ) {
        String imagesPath = "images/mimeTypeIcons/";
        String path = webappPath + "/" + imagesPath;
        String iconFileStr = mimeType.replaceAll( "/", "_" ) + ".png";
        File iconFile = new File( path + iconFileStr );
        String effectiveIconFile = iconFileStr;
        if( !iconFile.exists() ) {
            int indexOfFirstUnderscore = iconFileStr.indexOf( "_" );
            if( indexOfFirstUnderscore == -1 )
                effectiveIconFile = "default.png";
            else {
                String iconFileStr2 = iconFileStr.substring( 0, indexOfFirstUnderscore + 1 ) + "default.png";
                File iconFile2 = new File( path + iconFileStr2 );
                effectiveIconFile = iconFileStr2;
                if( !iconFile2.exists() )
                    effectiveIconFile = "default.png";
            }
        }
        return imagesPath + effectiveIconFile;
    }

    public static void setWebappPath(String path) {
        webappPath = path;
    }

    public static String webappPath;

    private static Pattern localizedStringPattern = Pattern.compile( "\".+\"@.+", Pattern.DOTALL );
    private static String syntaxHighlighterConfigFileLocation = null;

}
