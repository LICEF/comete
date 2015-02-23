package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.metadataformat.MetadataFormat;
import ca.licef.comete.core.metadataformat.MetadataFormats;
import ca.licef.comete.core.Settings;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.store.Store;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabularies.OAI;
import com.sun.jersey.core.header.FormDataContentDisposition;
import licef.CommonNamespaceContext;
import licef.DateUtil;
import licef.IOUtil;
import licef.StringUtil;
import licef.XMLUtil;
import licef.ZipUtil;
import licef.reflection.Invoker;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.TripleStore;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.RDF;
import licef.tsapi.vocabulary.FOAF;
import org.apache.tika.language.LanguageIdentifier;
import org.ariadne.util.JDomUtils;
import org.ariadne.validation.Validator;
import org.ariadne.validation.exception.InitialisationException;
import org.ariadne.validation.exception.ValidationException;
import org.ariadne.validation.utils.ValidationUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;

public class Metadata {

    private static Metadata instance;
    private LearningObjectView learningObjectView;

    public static File tmpFolder = new File(System.getProperty("java.io.tmpdir"));

    TripleStore tripleStore = Core.getInstance().getTripleStore();

    public static Metadata getInstance() {
        if (instance == null)
            instance = new Metadata();
        return (instance);
    }

    public LearningObjectView getLearningObjectView() {
        if (learningObjectView == null)
            learningObjectView = new LearningObjectView();
        return learningObjectView;
    }

    public String storeHarvestedRecord(String oaiID, String namespace, String repoUri, String record, String datestamp) throws Exception {
        String[] res = manageRecord(oaiID, namespace, repoUri, record, datestamp);
        String state = res[3];
        if (!"ignored".equals(state)) {
            MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
            System.out.println("storeHarvestedRecord: " + oaiID + " (" + metadataFormat.getName() + " format)");
        }
        return state;
    }

    public String deleteHarvestedRecord( String oaiID, String namespace ) throws Exception {
        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
        System.out.println("deleteHarvestedRecord: " +  oaiID + " (" + metadataFormat.getName() + " format)");

        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "getRecordURI", new Object[] { oaiID, namespace } );

        String recordURI = (String)tripleStore.transactionalCall( inv );
        if (recordURI != null) {
            deleteRecord(recordURI);
            return "deleted";
        }
        else
            return "ignored";
    }

    public String getLearningObjectURI( String metadataRecordUri ) throws Exception {
        String query = Util.getQuery( "metadata/getLearningObject.sparql", metadataRecordUri );
        Tuple[] tuples = tripleStore.sparqlSelect( query );
        if( tuples.length > 0 )
            return( tuples[ 0 ].getValue( "res" ).getContent() );
        else
            return( null );
    }

    public String getRecordURI( String oaiID, String namespace ) throws Exception {
        String query = Util.getQuery( "metadata/getMetadataRecordWith-oai-id.sparql", namespace, oaiID );
        Tuple[] tuples = Core.getInstance().getTripleStore().sparqlSelect(query);
        if( tuples.length > 0 )
            return( tuples[ 0 ].getValue( "s" ).getContent() );
        else
            return( null );
    }

    /*****
     * Local record upload management
     */

    public Object[] storeUploadedContentTmp(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
                                            InputStream uploadedInputStreamRes, FormDataContentDisposition fileDetailRes) throws Exception {
        String errorMessage = null;
        File record = null;
        File resource = null;
        if ("".equals(fileDetail.getFileName()))
            errorMessage = "No file.";
        else {
            record = new File(tmpFolder, fileDetail.getFileName());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(record);
                IOUtil.copy(uploadedInputStream, fos);
            }
            finally {
                if( fos != null )
                    fos.close();
            }
        }

        if (errorMessage == null && fileDetailRes != null && !"".equals(fileDetailRes.getFileName())) {
            resource = new File(tmpFolder, "" + (new Date()).getTime());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(resource);
                IOUtil.copy(uploadedInputStreamRes, fos);
            }
            finally {
                if( fos != null )
                    fos.close();
            }
        }

        return new Object[]{errorMessage, record, resource};
    }

    public Object[] storeUploadedContent(File record, File resource) throws Exception {
        Object[] results;
        String extension = record.getName().substring(record.getName().lastIndexOf('.') + 1).toLowerCase();
        if ("zip".equals(extension)) {
            File uploadedRecords = new File(tmpFolder, "uploadedRecords");
            ZipUtil.unzipFile(record.toString(), uploadedRecords.toString());
            String[] records = uploadedRecords.list();
            ArrayList<String[]> uris = new ArrayList<String[]>();
            for (String _record : records) {
                File rec = new File(uploadedRecords, _record);

                String[] res = storeUploadedRecord(rec, null);
                if (res[1] != null)
                    uris.add(new String[]{res[1], res[2]});
                rec.delete();
            }
            if (uris.isEmpty())
                results = new Object[]{"No valid record inside ZIP archive."};
            else {
                String[][] loUris = new String[uris.size()][];
                for (int i = 0; i < uris.size(); i++)
                    loUris[i] = new String[]{uris.get(i)[0], uris.get(i)[1]};
                results = new Object[]{null, loUris};
            }
        }
        else {
            //possible physical associated resource
            String[] res = storeUploadedRecord(record, resource);
            if (res[1] == null)
                results = new Object[]{res[0], null};
            else {
                String[][] uri = new String[1][];
                uri[0] = new String[]{res[1], res[2]};
                results = new Object[]{null, uri};
            }

            record.delete();
            if (resource != null)
                resource.delete();
        }

        return results;
    }

    private String[] parseMetadataRecord(File file) {
        String errorMessage = null;
        String content = null;
        String namespace = null;
        String pseudoOaiID = null;
        String xpath = null;
        try {
            Node node = XMLUtil.getXMLNode(file);
            content = XMLUtil.getXMLString( node );
            String rootname = XMLUtil.getRootTagName(content);
            Hashtable namespaces = XMLUtil.getAttributes(content, "/");
            String[] array = StringUtil.split(rootname, ':');
            rootname = array[array.length - 1].toLowerCase();
            if ("lom".equals(rootname) && namespaces.containsValue(Constants.IEEE_LOM_NAMESPACE)) {
                namespace = Constants.IEEE_LOM_NAMESPACE;
                xpath = "//lom:lom/lom:metaMetadata/lom:identifier/lom:entry";
            }
            else if ("dc".equals(rootname) && namespaces.containsValue(Constants.OAI_DC_NAMESPACE)) {
                namespace = Constants.OAI_DC_NAMESPACE;
                xpath = "//dc:identifier";
            }
            else
                errorMessage = "Wrong metadata format.";

            //check of 3.1 metaMetadata identifier existence for LOM or dc:identifier for DC.
            NodeList identifierNodes = XMLUtil.getNodeList( node, xpath );
            for( int i = 0; i < identifierNodes.getLength(); i++ ) {
                Node identNode = identifierNodes.item( i );
                pseudoOaiID = identNode.getTextContent();
                break;
            }
            if (pseudoOaiID == null)
                errorMessage = "No metametadata identifier field.";
        } catch (Exception e) {
            errorMessage = "Error on record parsing.";
        }

        return new String[] {errorMessage, content, namespace, pseudoOaiID};
    }

    public String[] storeUploadedRecord(File file, File resource) throws Exception {
        String[] values = parseMetadataRecord(file);

        String errorMessage = values[0];

        String record = values[1];
        String namespace = values[2];
        String pseudoOaiID = values[3]; //mapping of oai-id for local records

        ////deploy resource here if present
        //if (resource != null) {
        //    String error;
        //    try {
        //        error = deployer.deploy(resource, content);
        //    } catch (Exception e) {
        //        error = e.toString();
        //    }
        //    if (error != null)
        //        errorMessage = "Cannot deploy resource : " + error;
        //}

        String loURI = null;
        String state = null;
        if (errorMessage == null)
            try {
                String[] res = manageRecord(pseudoOaiID, namespace, null, record, null);
                loURI = res[0];
                state = res[3];
            } catch (Exception e) {
                errorMessage = "Error on record parsing.";
            }

        return new String[]{errorMessage, loURI, state};
    }

    public Object[] isRecordExists(File record) throws Exception {
        String[] values = parseMetadataRecord(record);
        String errorMessage = values[0];
        String namespace = values[2];
        String pseudoOaiID = values[3];
        boolean isExists = false;
        if (errorMessage == null) {
            String recordURI = getRecordURI(pseudoOaiID, namespace);
            isExists = (recordURI != null);
        }
        return new Object[]{errorMessage, isExists};
    }

    /*****
     * Deletion
     */

    public void deleteRepositoryRecords(String repoUri) throws Exception {
    //    String[][] records = getRepositoryRecords(repoUri);
    //    for( int i = 0; i < records.length; i++ )
    //        setState(records[i][1], "D");

    //    //itql enhanced
    //    Core.getInstance().getTripleStoreService().
    //            processTQLQueries("deleteRepositoryRecords.tql", repoUri);
    }

    public void deleteRecord(String recordURI) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "getLearningObjectURI", new Object[] { recordURI } );
        String loURI = (String)tripleStore.transactionalCall( inv );
        deleteLearningObject(loURI);
    }

    public void deleteLearningObject(String loUri) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doDeleteLearningObject", new String[] { loUri } );
        Core.getInstance().getTripleStore().transactionalCall( inv, TripleStore.WRITE_MODE );
    }

    public void doDeleteLearningObject(String loUri) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        String query = Util.getQuery( "metadata/getMetadataRecordFromLO.sparql", loUri ); 
        Tuple[] res = tripleStore.sparqlSelect( query );
        for( Tuple tuple : res ) {
            String recordUri = tuple.getValue( "s" ).getContent();
            String storeId = tuple.getValue( "storeId" ).getContent(); 
            deleteMetadataRecord(recordUri, storeId);
        }
        tripleStore.removeResource_textIndex(loUri);
    }

    private void deleteMetadataRecord(String recordURI, String storeId) throws Exception {
        File metadataRecFolder = new File( Store.getInstance().getLocation() + storeId );
        if( metadataRecFolder.exists() ) {
            File markedForDeletionFile = new File( Store.getInstance().getLocation() + storeId + ".d" );
            if( !metadataRecFolder.renameTo( markedForDeletionFile ) )
                throw( new IOException( "Folder " + metadataRecFolder + " could not be renamed to " + markedForDeletionFile + "." ) );
        }
        Core.getInstance().getTripleStore().removeResource_textIndex(recordURI);
    }

    public String[][] getRepositoryRecords(String repoUri) throws Exception {
    //    Hashtable<String, String>[] results =
    //            Core.getInstance().getTripleStoreService().getResults( "getRepositoryRecords.sparql", repoUri );
    //    String[][] res = new String[results.length][2];
    //    for( int i = 0; i < results.length; i++ ) {
    //        res[i][0] = results[i].get("s");
    //        res[i][1] = results[i].get("doId");
    //    }
    //    return res;
        return( null );
    }

    /****
     * Record's digest
     */

    private String[] manageRecord(String oaiId, String namespace, String repoUri, String record, String datestamp) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.metadata.Metadata",
                "digestRecord", new Object[]{record,  namespace, repoUri, oaiId, datestamp});
        String[] res = (String[])tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);

        //Identity and vocabulary referencement management
        String loURI = res[0];
        String recordURI = res[1];
        String storeId = res[2];
        linkToResources(loURI, recordURI, storeId, namespace);

        return res;
    }

    public String[] digestRecord(String record, String namespace, String repoURI, String oaiId, String datestamp) throws Exception {
        ArrayList<Triple> triples = new ArrayList<>();
        String storeId;
        Store store = Store.getInstance();
        boolean isUpdate = false;

        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);

        String loURI = null;

        //Retrieve of metadata record with metadata format and oaiID
        String recordURI = getRecordURI(oaiId, namespace);

        if (recordURI != null) {
            isUpdate = true;
            Triple[] _triples = tripleStore.getTriplesWithSubjectPredicate(recordURI, OAI.datestamp);
            if (_triples.length > 0) {
                Date d1 = DateUtil.toDate(_triples[0].getObject());
                Date d2 = DateUtil.toDate(datestamp);
                if (d2.after(d1)) {
                    String query = Util.getQuery( "metadata/deleteOAIDatestampTriples.sparql", recordURI, OAI.datestamp.getURI() );
                    tripleStore.sparqlUpdate( query );
                }
                else
                    return new String[]{null, null, null, "ignored"};
            }

            loURI = getLearningObjectURI(recordURI);
            resetLearningObjectNonPersistentTriples(recordURI);
            storeId = getStoreIdFromURI(recordURI);
        }
        else {
            ////Is there another metadata record with the same oai-id ?
            ////if yes, retrieve of the described resource
            String query = Util.getQuery( "metadata/getLearningObjectFromOtherMetadataRecord.sparql", oaiId );
            Tuple[] tuples = tripleStore.sparqlSelect( query );
            if (tuples.length > 0)
                loURI = tuples[0].getValue("res").getContent();

            //creation of new one
            if (loURI == null) {
                loURI = Util.makeURI(COMETE.LearningObject);
                triples.add( new Triple( loURI, RDF.type, COMETE.LearningObject ) );
                triples.add( new Triple( loURI, COMETE.added, DateUtil.toISOString(new Date(), null, null) ) );
            }

            storeId = store.createDigitalObject( Store.PATH_RECORDS );

            // We remove the leading / beforehand.
            recordURI = Util.makeURI(storeId.substring( 1 ), COMETE.MetadataRecord.getURI());
            triples.add(new Triple(recordURI, RDF.type, COMETE.MetadataRecord));
            triples.add(new Triple(recordURI, COMETE.metadataFormat, namespace));
            triples.add(new Triple(recordURI, COMETE.storeDigitalObject, storeId));
            if( repoURI != null && !"".equals( repoURI ) )
                triples.add(new Triple(recordURI, COMETE.repository, repoURI));

            String encodedStoreId = storeId;
            try {
                encodedStoreId = URLEncoder.encode( encodedStoreId, "UTF-8" );
            }
            catch( UnsupportedEncodingException e ) {
                e.printStackTrace();
            }

            String recordLink = Core.getInstance().getCometeUrl() + "/rest/metadataRecords/" + encodedStoreId + "/xml";
            triples.add(new Triple(recordURI, COMETE.originalDataLink, recordLink));

            //Resource association
            triples.add(new Triple(loURI, COMETE.hasMetadataRecord, recordURI));
            triples.add(new Triple(recordURI, COMETE.describes, loURI));
        }

        //store content
        store.setDatastream(storeId, Constants.DATASTREAM_ORIGINAL_DATA, record);

        //validation
        validateRecord( storeId, loURI, recordURI, record, namespace );

        //process record
        String extractedTriplesAsXml = processMetadataRecord( record, loURI, recordURI, namespace );
        Triple[] extractedTriples = Triple.readTriplesFromXml(extractedTriplesAsXml);
        triples.addAll(Arrays.asList(extractedTriples));

        //format adjustment
        manageFormat(loURI, triples);

        //languages adjustments
        manageLanguages(recordURI, loURI, triples );

        //triples insertions
        tripleStore.insertTriples_textIndex(triples);

        //automatic exposition to harvesting
        exposeRecords(loURI, storeId, metadataFormat);

        //oai-pmh properties
        if (!isUpdate)
            tripleStore.insertTriple( new Triple( recordURI, OAI.identifier, oaiId ) );
        if (datestamp != null)
            tripleStore.insertTriple( new Triple( recordURI, OAI.datestamp, datestamp ) ); //always set. also for previous cases. -AM

        String state = isUpdate?"updated":"added";

        return new String[]{loURI, recordURI, storeId, state};
    }

    private String processMetadataRecord( String xml, String loURI, String recordURI, String namespace ) throws Exception {
        String format = MetadataFormats.getMetadataFormat( namespace ).getName();

        StreamSource xmlSource = new StreamSource( new BufferedReader( new StringReader( xml ) ) );
        HashMap<String,String> params = new HashMap<String,String>();
        params.put( "loURI", loURI );
        params.put( "recordURI", recordURI );
        String triplesAsXml = Util.applyXslToDocument( "metadata/process" + StringUtil.capitalize( format ) + "Record", xmlSource, params );
        return( triplesAsXml );
    }

    //private String processMetadataRecord( String recordId ) throws Exception {
    //    String metadataRecordUri = Util.makeURI( recordId, Constants.TYPE_METADATA_RECORD );
    //    String storeId = Core.getInstance().getTripleStore().getFedoraIdFromURI( metadataRecordUri );
    //    String applicationProfile = null;
    //    String learningObjectUri = null;
    //    Triple[] metadataRecordTriples = Core.getInstance().getTripleStore().getTriplesWithSubject( metadataRecordUri );
    //    for( int i = 0; i < metadataRecordTriples.length; i++ ) {
    //        Triple triple = metadataRecordTriples[ i ];
    //        if( Constants.METAMODEL_APPLICATION_PROFILE.equals( triple.getPredicate() ) )
    //            applicationProfile = triple.getObject();
    //        else if( Constants.METAMODEL_DESCRIBES.equals( triple.getPredicate() ) )
    //            learningObjectUri = triple.getObject();
    //    }

    //    String recordXml = Core.getInstance().getFedora().getDatastream( storeId, Constants.DATASTREAM_ORIGINAL_DATA );

    //    return( processMetadataRecord( recordXml, learningObjectUri, metadataRecordUri, applicationProfile ) );
    //}

    private void manageFormat( String loURI, ArrayList<Triple> triples ) throws Exception{
        String format = null, location = null;
        for (Triple triple : triples) {
            if (FOAF.page.getURI().equals(triple.getPredicate())) {
                location = triple.getObject();
                if( location != null && !location.startsWith( "http" ) )
                    location = "http://" + location;
            }
            else if (DCTERMS.format.getURI().equals(triple.getPredicate()))
                format = triple.getObject();
        }

        if (format == null && location != null) {
            String mimetype = IOUtil.getMimeType( location );
            format = "http://purl.org/NET/mediatypes/" + mimetype;
            Triple tripleFormat = new Triple( loURI, DCTERMS.format, format );
            triples.add(tripleFormat);
        }
    }

    private void manageLanguages( String recordURI, String loURI, ArrayList<Triple> triples ) {
        ArrayList<String> loInitLanguages = new ArrayList<String>();
        ArrayList<String> recordInitLanguages = new ArrayList<String>();
        ArrayList<Triple> titles = new ArrayList<Triple>();
        ArrayList<Triple> descriptions = new ArrayList<Triple>();
        for (Triple triple : triples) {
            if (DCTERMS.language.getURI().equals(triple.getPredicate())) {
                if( recordURI.equals( triple.getSubject() ) )
                    recordInitLanguages.add( triple.getObject() );
                else if( loURI.equals( triple.getSubject() ) )
                    loInitLanguages.add( triple.getObject() );
            }
            else if (DCTERMS.title.getURI().equals(triple.getPredicate()))
                titles.add( triple );
            else if (DCTERMS.description.getURI().equals(triple.getPredicate()))
                descriptions.add( triple );
        }

        HashSet<String> recordLanguages = new HashSet<String>();

        // If no languages are specified for the metadata record, try to guess it.
        if( recordInitLanguages.isEmpty() ) {

            // Check if some language attributes are already set for titles and descriptions.
            for( Triple t : titles ) {
                if( t.getLanguage() != null )
                    recordLanguages.add( t.getLanguage() );
            }
            for( Triple t : descriptions ) {
                if( t.getLanguage() != null )
                    recordLanguages.add( t.getLanguage() );
            }

            // Otherwise, try to guess the language used in the descriptions.  The descriptions are likely longer than titles and
            // might give better results.
            if( recordLanguages.isEmpty() ) {
                for( Triple t : descriptions ) {
                    LanguageIdentifier langIdentifier = new LanguageIdentifier( t.getObject() );
                    recordLanguages.add( langIdentifier.getLanguage() );
                }
            }

            // Otherwise, try to guess the language using the titles.
            if( recordLanguages.isEmpty() ) {
                for( Triple t : titles ) {
                    LanguageIdentifier langIdentifier = new LanguageIdentifier( t.getObject() );
                    recordLanguages.add( langIdentifier.getLanguage() );
                }
            }

            // Add the triples for the recordLanguages found.
            for( String lang : recordLanguages ) {
                Triple triple = new Triple( recordURI, DCTERMS.language, lang );
                triples.add( triple );
            }
        }

        // If no languages are specified for the resource, we try to guess it.
        if( loInitLanguages.isEmpty() ) {
            if( !recordInitLanguages.isEmpty() ) {
                // Take the initial languages of the metadata records if any.
                for( String lang : recordInitLanguages ) {
                    Triple triple = new Triple( loURI, DCTERMS.language, lang );
                    triples.add( triple );
                }
            }
            else {
                // Otherwise take the languages that we guessed for the metadata record.
                for( String lang : recordLanguages ) {
                    Triple triple = new Triple( loURI, DCTERMS.language, lang );
                    triples.add( triple );
                }
            }
        }
    }

    /*****
     * Record's Redigest
     */

    private void resetLearningObjectNonPersistentTriples(String recordURI) throws Exception {
        String query = Util.getQuery( "metadata/deleteLOTriplesToReset.sparql", recordURI );
        tripleStore.sparqlUpdate_textIndex(query);
    }

    /**
     * This method uses the old XSL transformation to internal format to link voc concepts and identities
     */
    private void linkToResources(String loURI, String recordURI, String storeId, String namespace) throws Exception {
        if( !Store.getInstance().isDatastreamExists( storeId, Constants.DATASTREAM_ORIGINAL_DATA ) )
            return;

        String xml = Store.getInstance().getDatastream( storeId, Constants.DATASTREAM_ORIGINAL_DATA );

        HashMap<String,String> parameters = new HashMap<>();
        parameters.put( "loURI", loURI );
        parameters.put( "recordURI", recordURI );

        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);

        String stylesheet = null;
        if( metadataFormat.getNamespace().equals( Constants.OAI_DC_NAMESPACE ))
            stylesheet = "metadata/processDcLinking";
        else if( metadataFormat.getNamespace().equals( Constants.IEEE_LOM_NAMESPACE) )
            stylesheet = "metadata/processLomLinking";

        StreamSource source = new StreamSource( new StringReader( xml ) );
        Util.applyXslToDocument( stylesheet, source, parameters );
    }

    /*private void internalFormatToExposedRecords(String loURI, String storeId, MetadataFormat metadataFormat) throws Exception {
        if( !Store.getInstance().isDatastreamExists( storeId, Constants.DATASTREAM_INTERNAL_DATA ) )
            return;
        String recordUri = Util.makeURI(Util.getIdValue(storeId), COMETE.MetadataRecord);

        System.out.println("Expose record : " + recordUri + "...");
        String xml = Store.getInstance().getDatastream( storeId, Constants.DATASTREAM_INTERNAL_DATA );

        HashMap<String,String> parameters = new HashMap<String,String>();
        parameters.put( "loURI", loURI );

        String stylesheet = null;
        if( metadataFormat.getNamespace().equals( Constants.OAI_DC_NAMESPACE ))
            stylesheet = "metadata/convertInternalFormatToDc";
        else if( metadataFormat.getNamespace().equals( Constants.IEEE_LOM_NAMESPACE) )
            stylesheet = "metadata/convertInternalFormatToLom";

        StreamSource source = new StreamSource( new StringReader( xml ) );
        String newXml = Util.applyXslToDocument( stylesheet, source, parameters );

        updateExposedFormat(storeId, newXml, metadataFormat, recordUri);

        // Generate exposed record for other metadata formats.
        stylesheet = null;
        MetadataFormat exposedMetadataFormat = null;
        if( metadataFormat.getNamespace().equals( Constants.OAI_DC_NAMESPACE )) {
            stylesheet = "metadata/convertFromInternalFormatDcToLom";
            exposedMetadataFormat = MetadataFormats.getMetadataFormat( Constants.IEEE_LOM_NAMESPACE );
        }
        else if( metadataFormat.getNamespace().equals( Constants.IEEE_LOM_NAMESPACE) ) {
            stylesheet = "metadata/convertFromInternalFormatLomToDc";
            exposedMetadataFormat = MetadataFormats.getMetadataFormat( Constants.OAI_DC_NAMESPACE );
        }

        if( stylesheet != null ) {
            parameters.put( "recordURI", recordUri );
            source = new StreamSource( new StringReader( xml ) );
            newXml = Util.applyXslToDocument( stylesheet, source, parameters );

            updateExposedFormat(storeId, newXml, exposedMetadataFormat, recordUri);
        }
    }*/

    /*****
     * exposed format management
     */
    private void exposeRecords(String loURI, String storeId, MetadataFormat metadataFormat) throws Exception {
        Store store = Store.getInstance();
        if( !store.isDatastreamExists( storeId, Constants.DATASTREAM_ORIGINAL_DATA ) )
            return;

        String recordURI = Util.makeURI( storeId.substring( 1 ), COMETE.MetadataRecord.getURI() );
        System.out.println( "Expose record : " + recordURI + "..." );

        String xml = store.getDatastream( storeId, Constants.DATASTREAM_ORIGINAL_DATA );

        for( MetadataFormat mf : MetadataFormats.getMetadataFormats() ) {
            String stylesheet = null;
            HashMap<String,String> parameters = new HashMap<String,String>();
            if( mf.equals( metadataFormat ) )
                stylesheet = "identity";
            else { 
                parameters.put( "loURI", loURI );
                parameters.put( "recordURI", recordURI );
                stylesheet = "metadata/convert" + StringUtil.capitalize( metadataFormat.getName() ) + "To" + StringUtil.capitalize( mf.getName() );
            }

            StreamSource source = new StreamSource( new StringReader( xml ) );
            String newXml = Util.applyXslToDocument( stylesheet, source, parameters );
            int resp = store.setDatastream( storeId, "exposed_" + mf.getName(), newXml );
            if (resp == Store.DATASTREAM_STORED)
                System.out.println("-> " + recordURI + " exposed (" + mf.getName() + " format)");
            else if (resp == Store.DATASTREAM_UNCHANGED)
                System.out.println("-> no change on : " + recordURI + ".");
        }
    }

    /*
     * Validation
     */

    private void validateRecord( String storeId, String loURI, String recordURI, String record, String namespace ) throws Exception {
        Store store = Store.getInstance();

        Map<String,Boolean> applProfToValidate = Settings.getValidatedApplicationProfiles();

        String[] applProfiles = null;
        if( Constants.IEEE_LOM_NAMESPACE.equals( namespace ) )
            applProfiles = Constants.lomApplProfiles;
        else if( Constants.OAI_DC_NAMESPACE.equals( namespace ) )
            applProfiles = Constants.dcApplProfiles;

        for( int i = 0; i < applProfiles.length; i++ ) {
            Boolean isValidationRequired = applProfToValidate.get( applProfiles[ i ] );
            if( isValidationRequired == null || !isValidationRequired.booleanValue() )
                continue;

            String profileUri = applProfiles[ i ];
            boolean isValid = true;
            long startTime = System.currentTimeMillis();
            System.out.println( "Validating record " + recordURI + " against " + profileUri );
            String reportDataStream = Util.getReportDataStream( profileUri );
            if( store.isDatastreamExists( storeId, reportDataStream ) )
                store.deleteDatastream( storeId, reportDataStream );
            try {
                getValidator().validateMetadata( record, profileUri );
                tripleStore.insertTriple( new Triple( recordURI, COMETE.applicationProfile, profileUri ) );
            }
            catch( ValidationException e ) {
                isValid = false;
                String errorReport = JDomUtils.parseXml2string(ValidationUtils.collectErrorsAsXml(e.getMessage()),null);
                if( !store.isDatastreamExists( storeId, reportDataStream ) ) {
                    store.setDatastream( storeId, reportDataStream, errorReport);
                    tripleStore.insertTriple( new Triple( recordURI, COMETE.validationReportLink, Util.getReportLink( storeId, profileUri ) ) );
                }
            }
            finally {
                long timeTaken = System.currentTimeMillis() - startTime;
                System.out.println( "Validation complete (elapsed time: "+ timeTaken + " ms): " + ( isValid ? "VALID" : "INVALID" ) );
            }
        }
    }

    private Validator getValidator() throws IOException, FileNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, InitialisationException {
        if( !isValidatorInitialized ) {
            BufferedReader bis = null;
            java.io.BufferedWriter bos = null;
            File configFile = null;
            try {
                configFile = File.createTempFile( "validatorConfig", ".properties" );
                java.io.FileWriter fw = new java.io.FileWriter( configFile );
                bos = new java.io.BufferedWriter( fw );
                InputStream is = Validator.class.getResourceAsStream( "config.properties" );
                bis = new BufferedReader( new java.io.InputStreamReader( is ) );
                for( ;; ) {
                    String line = bis.readLine();
                    if( line == null )
                        break;
                    String fixedLine = null;
                    if( line.startsWith( "#" ) || line.indexOf( "Location" ) == -1 )
                        fixedLine = line;
                    else
                        fixedLine = line.replaceFirst( "( = )(.*)(/validation)", "$1" + Core.getInstance().getCometeUrl() + "$3" );
                    bos.write( fixedLine );
                    bos.write( "\n" );
                    bos.flush();
                }
            }
            finally {
                if( bis != null )
                    bis.close();
                if( bos != null )
                    bos.close();
            }

            Validator.getPropertiesManager().init( configFile );
            if( configFile != null )
                configFile.delete();
            Validator.getValidator().initFromPropertiesManager();

            isValidatorInitialized = true;
        }
        return( Validator.getValidator() );
    }
    
    public ResultSet getMetadataRecordApplicationProfiles( int start, int limit, String applProfile ) throws Exception {
        return( getMetadataRecordApplicationProfiles( start, limit, applProfile, false ) );
    }

    /**
     * @param applProf If <tt>null</tt>, all the application profiles will be returned.  Otherwise, only the selected application profile will be provided.
     * @param showOnlyInvalidRecords If <tt>true</tt> and applProf is different from <tt>null</tt>, only invalid records will be returned.
     */
    public ResultSet getMetadataRecordApplicationProfiles( int start, int limit, String applProfile, boolean showOnlyInvalidRecords ) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", 
            "getMetadataRecordApplicationProfilesEff", new Object[] { start, limit, applProfile, showOnlyInvalidRecords } );
        return( (ResultSet)Core.getInstance().getTripleStore().transactionalCall( inv ) );
    }

    public ResultSet getMetadataRecordApplicationProfilesEff( int start, int limit, String applProfile, boolean showOnlyInvalidRecords ) throws Exception {
        ResultSet rs = new ResultSet();
        String query = Util.getQuery( "metadata/getAllMetadataRecordsCount.sparql" );
        if( applProfile != null && showOnlyInvalidRecords )
            query = Util.getQuery( "metadata/getInvalidMetadataRecordsCount.sparql", Util.getApplProfAbbreviation( applProfile ) );
        Tuple[] res = tripleStore.sparqlSelect( query );
        int count = Integer.parseInt( res[0].getValue( "count" ).getContent() );
        if( count > 0 ) {
            rs.setTotalRecords( count );
            List<String> recordUris = new ArrayList<String>();

            Hashtable<String, String> metadataFormats = new Hashtable<String, String>();
            query = Util.getQuery( "metadata/getAllMetadataRecordFormats.sparql", start, limit );
            if( applProfile != null && showOnlyInvalidRecords )
                query = Util.getQuery( "metadata/getInvalidMetadataRecordFormats.sparql", start, limit, Util.getApplProfAbbreviation( applProfile ) );
            res = tripleStore.sparqlSelect( query );
            for( Tuple tuple : res ) {
                String uri = tuple.getValue( "s" ).getContent();
                String format = tuple.getValue( "format" ).getContent();
                recordUris.add( uri );
                metadataFormats.put( uri, format );
            }

            Hashtable<String, String> metadataRecordRepoTable = new Hashtable<String, String>();
            Hashtable<String, Hashtable<String, String>> repoInfo = new Hashtable<String, Hashtable<String, String>>();
            retrieveMetadataRecordRepoInfo( recordUris, metadataRecordRepoTable, repoInfo );

            String union = buildMetadataRecordsUnion( recordUris );
            query = Util.getQuery( "metadata/getMetadataRecordApplicationProfiles.sparql", union );
            Tuple[] tuples = tripleStore.sparqlSelect( query );
            String tempUri = null;
            List<String> applProfList = null;
            for( Tuple tuple : tuples ) {
                String uri = tuple.getValue( "s" ).getContent();
                String applProf = tuple.getValue( "applProf" ).getContent();
                if( !uri.equals( tempUri ) ) {
                    if( applProfList != null ) {
                        Map entry = new HashMap<String,String>();
                        entry.put( "id", tempUri );
                        entry.put( "metadataFormat", metadataFormats.get( tempUri ) );
                        entry.put( "profiles", applProfList );
                        String repoUri = metadataRecordRepoTable.get( tempUri );
                        if( repoUri != null ) {
                            entry.put( "repoUri", repoUri );
                            Hashtable<String, String> repoInfoData = repoInfo.get( repoUri );
                            if( repoInfoData != null ) {
                                String repoName = repoInfoData.get( "name" );
                                if( repoName != null )
                                    entry.put( "repoName", repoName );
                                String repoAdminEmail = repoInfoData.get( "adminEmail" );
                                if( repoAdminEmail != null )
                                    entry.put( "repoAdminEmail", repoAdminEmail );
                            }
                        }
                        rs.addEntry( entry );
                    }
                    applProfList = new ArrayList<String>();
                    tempUri = uri;
                }
                if( applProfile == null || applProfile.equals( applProf ))
                    applProfList.add( applProf );
            }
            if( applProfList != null ) {
                Map<String, Object> entry = new HashMap<String, Object>();
                entry.put( "id", tempUri );
                entry.put( "metadataFormat", metadataFormats.get( tempUri ) );
                entry.put( "profiles", applProfList );
                String repoUri = metadataRecordRepoTable.get( tempUri );
                if( repoUri != null ) {
                    entry.put( "repoUri", repoUri );
                    Hashtable<String, String> repoInfoData = repoInfo.get( repoUri );
                    if( repoInfoData != null ) {
                        String repoName = repoInfoData.get( "name" );
                        if( repoName != null )
                            entry.put( "repoName", repoName );
                        String repoAdminEmail = repoInfoData.get( "adminEmail" );
                        if( repoAdminEmail != null )
                            entry.put( "repoAdminEmail", repoAdminEmail );
                    }
                }
                rs.addEntry( entry );
            }
        }
        return( rs );
    }

    /*
     * URI Store conversion
     */

    private String getURIFromStoreId(String storeId) {
        String uri = null;
        if (storeId.startsWith("http://"))
            return storeId;
        else {
            if (!storeId.startsWith("info:fedora/"))
                storeId = "info:fedora/" + storeId;
            try {
                Triple[] triples = Core.getInstance().getTripleStore().getTriplesWithPredicateObject(
                        COMETE.storeDigitalObject, storeId, null);
                if (triples.length > 0)
                    uri = triples[0].getSubject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    private String getStoreIdFromURI(String uri) {
        String id = null;
        if (!uri.startsWith("http://"))
            return uri;
        else {
            try {
                Triple[] triples = Core.getInstance().getTripleStore().getTriplesWithSubjectPredicate(
                        uri, COMETE.storeDigitalObject);
                if (triples.length > 0)
                    id = triples[0].getObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    private void retrieveMetadataRecordRepoInfo( List<String> recordUris, Hashtable<String, String> metadataRecordRepoTable, Hashtable<String, Hashtable<String, String>> repoInfo ) throws Exception {
        String repoInfoConstraint = Util.buildFilterConstraints( recordUris, "s", true, "=", "||" );
        String query = Util.getQuery( "metadata/getMetadataRecordRepoInfo.sparql", repoInfoConstraint );
        Tuple[] resRepoInfo = Core.getInstance().getTripleStore().sparqlSelect( query );
        for( Tuple tuple : resRepoInfo ) {
            String uri = tuple.getValue( "s" ).getContent();
            String repoUri = tuple.getValue( "repo" ).getContent();
            String repoName = tuple.getValue( "repoName" ).getContent();
            String repoAdminEmail = tuple.getValue( "repoAdminEmail" ).getContent();
            if( repoUri != null ) {
                metadataRecordRepoTable.put( uri, repoUri );
                Hashtable<String, String> repoInfoData = repoInfo.get( repoUri );
                if( repoInfoData == null )
                    repoInfoData = new Hashtable<String, String>();
                if( repoName != null )
                    repoInfoData.put( "name", repoName );
                if( repoAdminEmail != null )
                    repoInfoData.put( "adminEmail", repoAdminEmail );
                repoInfo.put( repoUri, repoInfoData );
            }
        }
    }

    private String buildMetadataRecordsUnion( List<String> recordUris ) {
        StringBuilder union = new StringBuilder();
        String unionDelimiter = "";
        for( int i = 0; i < recordUris.size(); i++ ) {
            union.append( unionDelimiter );
            union.append( "{" );
            union.append( "?s rdf:type comete:MetadataRecord ." );
            union.append( "?s comete:describes ?record ." );
            union.append( "?record comete:hasMetadataRecord <" ).append( recordUris.get( i ) ).append( "> ." );
            union.append( "OPTIONAL { ?s comete:applicationProfile ?applProf }" );
            union.append( "}" );
            unionDelimiter = " UNION ";
        }
        return( union.toString() );
    }

    private boolean isValidatorInitialized = false;

}
