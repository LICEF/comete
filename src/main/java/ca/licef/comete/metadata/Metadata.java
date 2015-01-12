package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.metadataformat.MetadataFormat;
import ca.licef.comete.core.metadataformat.MetadataFormats;
import ca.licef.comete.core.Settings;
import ca.licef.comete.core.util.Constants;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringReader;
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

    public static File tmpFolder = new File(System.getProperty("java.io.tmpdir"));

    TripleStore tripleStore = Core.getInstance().getTripleStore();

    public static Metadata getInstance() {
        if (instance == null)
            instance = new Metadata();
        return (instance);
    }

    public String storeHarvestedRecord(String oaiID, String namespace, String repoUri, String record, String datestamp, boolean isUpdate) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.metadata.Metadata",
                "storeHarvestedRecordEff", new Object[]{ oaiID, namespace, repoUri, record, datestamp, isUpdate } );
        Object resp = Core.getInstance().getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
        return( (String)resp );
    }

    /*
     * This method must be transactionally called. - FB
     */
    public String storeHarvestedRecordEff(String oaiID, String namespace, String repoUri, String record, String datestamp, boolean isUpdate ) throws Exception {
        if (datestamp != null) {
            String recordURI = getRecordURI(oaiID, namespace);
            if (recordURI != null) {
                isUpdate = true;
                Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(recordURI, OAI.datestamp);
                if (triples.length > 0) {
                    Date d1 = DateUtil.toDate(triples[0].getObject());
                    Date d2 = DateUtil.toDate(datestamp);
                    if (d2.after(d1)) {
                        String query = Util.getQuery( "metadata/deleteOAIDatestampTriples.sparql", recordURI, OAI.datestamp.getURI() );
                        tripleStore.sparqlUpdate( query );
                    }
                    else
                        return "ignored";
                }
            }
        }

        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
        System.out.println("storeHarvestedRecord: " +  oaiID + " (" + metadataFormat.getName() + " format)");

        String loURI = null;

        //Retrieve of metadata record with metadata format and oaiID
        String recordURI = getRecordURI(oaiID, namespace);

        if (recordURI == null) {
            isUpdate = false; //to force new MetadataRecord creation context

            ////Is there another metadata record with the same oai-id ?
            ////if yes, retrieve of the described resource
            //Hashtable<String, String>[] res = tripleStore.getResults("getLearningObjectFromOtherMetadataRecord.sparql", oaiID);
            //if (res.length > 0)
            //    loURI = res[0].get("res");

            //creation of new one
            if (loURI == null) {
                loURI = Util.makeURI(COMETE.LearningObject);
                tripleStore.insertTriple( new Triple( loURI, RDF.type, COMETE.LearningObject ) );
                tripleStore.insertTriple( new Triple( loURI, COMETE.added, DateUtil.toISOString(new Date(), null, null) ) );
            }
        }
        else {
            loURI = getLearningObjectURI(recordURI);
            resetLearningObjectNonPersistentTriples(recordURI);
        }

        recordURI = digestRecord(loURI, recordURI, record, namespace, repoUri, oaiID);

        //oai-pmh properties
        if (!isUpdate)
            tripleStore.insertTriple( new Triple( recordURI, OAI.identifier, oaiID ) );
        if (datestamp != null)
            tripleStore.insertTriple( new Triple( recordURI, OAI.datestamp, datestamp ) ); //always set. also for previous cases. -AM

        if (isUpdate)
            return "updated";
        else
            return "added";
    }

    public String deleteHarvestedRecord( String oaiID, String namespace ) throws Exception {
        /*MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
        System.out.println("deleteHarvestedRecord: " +  oaiID + " (" + metadataFormat.getName() + " format)");

        String recordURI = getRecordURI(oaiID, namespace);
        if (recordURI != null) {
            deleteRecord(recordURI);
            return "deleted";
        }
        else
            return "ignored";*/

        return null;
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

                Invoker inv = new Invoker(this, "ca.licef.comete.metadata.Metadata", "storeUploadedRecord", new Object[]{ rec, null} );
                String[] res = (String[])tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
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
            Invoker inv = new Invoker(this, "ca.licef.comete.metadata.Metadata", "storeUploadedRecord", new Object[]{ record, resource} );
            String[] res = (String[])tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
            if (res[1] == null)
                results = new Object[]{res[0], null};
            else {
                String[][] uri = new String[1][];
                uri[0] = new String[]{res[1], res[2]};
                results = new Object[]{null, uri};
            }

            boolean b = record.delete();
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
        try {
            Node node = XMLUtil.getXMLNode(file);
            content = XMLUtil.getXMLString( node );
            String rootname = XMLUtil.getRootTagName(content);
            Hashtable namespaces = XMLUtil.getAttributes(content, "/");
            String[] array = StringUtil.split(rootname, ':');
            rootname = array[array.length - 1].toLowerCase();
            if ("lom".equals(rootname) && namespaces.containsValue(Constants.IEEE_LOM_NAMESPACE))
                namespace = Constants.IEEE_LOM_NAMESPACE;
            else if ("dc".equals(rootname) && namespaces.containsValue(Constants.OAI_DC_NAMESPACE))
                namespace = Constants.OAI_DC_NAMESPACE;
            else
                errorMessage = "Wrong metadata format.";

            //check of 3.1 metaMetadata identifier existence
            String xpath = "//lom:lom/lom:metaMetadata/lom:identifier/lom:entry";
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

    /*
     * This method must be transactionally called. - FB
     */
    public String[] storeUploadedRecord(File file, File resource) throws Exception {
        String[] values = parseMetadataRecord(file);

        String errorMessage = values[0];

        String content = values[1];
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
        if (errorMessage == null) {
            try {
                String recordURI = getRecordURI(pseudoOaiID, namespace);
                if (recordURI == null) {
                    loURI = Util.makeURI(COMETE.LearningObject);
                    List<Triple> triples = new ArrayList<Triple>();
                    triples.add( new Triple( loURI, RDF.type, COMETE.LearningObject) );
                    triples.add( new Triple( loURI, COMETE.added, DateUtil.toISOString(new Date(), null, null) ) );
                    tripleStore.insertTriples( triples );
                    state = "created";
                }
                else {
                    loURI = getLearningObjectURI(recordURI);
                    resetLearningObjectNonPersistentTriples(recordURI);
                    state = "updated";
                }
                recordURI = digestRecord(loURI, recordURI, content, namespace, CommonNamespaceContext.cometeNSURI + "localRecord", pseudoOaiID);
                tripleStore.insertTriple(new Triple( recordURI, Constants.OAI_ID, pseudoOaiID, true ) );

            } catch (Exception e) {
                e.printStackTrace();
                loURI = null;
                errorMessage = "Error on record parsing.";
            }
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

    //public void deleteRepositoryRecords(String repoUri) throws Exception {
    //    String[][] records = getRepositoryRecords(repoUri);
    //    for( int i = 0; i < records.length; i++ )
    //        setState(records[i][1], "D");

    //    //itql enhanced
    //    Core.getInstance().getTripleStoreService().
    //            processTQLQueries("deleteRepositoryRecords.tql", repoUri);
    //}

    public void deleteRecord(String recordURI) throws Exception {
    //    String loURI = getLearningObjectURI(recordURI);
    //    deleteLearningObject(loURI);
    }

    public void deleteLearningObject(String loUri) throws Exception {
    //    //associated metadata records
    //    Hashtable<String, String>[] results =
    //            Core.getInstance().getTripleStoreService().getResults( "getMetadataRecords.sparql", loUri );
    //    for( int i = 0; i < results.length; i++ ) {
    //        String recordUri = results[i].get("s");
    //        String doId = results[i].get("doId");
    //        deleteMetadataRecord(recordUri, doId);
    //    }
    //    Core.getInstance().getTripleStoreService().deleteResource(loUri);
    }

    //void deleteMetadataRecord(String recordURI, String doId) throws Exception {
    //    setState(doId, "D");
    //    Core.getInstance().getTripleStoreService().deleteResource(recordURI);
    //}

    ///**
    // * set fedora digital object state as "deleted" for oaiprovider exposition
    // * @param doId fedora digital object ID
    // * @throws Exception
    // */
    //void setState(String doId, String state) throws Exception {
    //    FedoraService fedora = Core.getInstance().getFedoraService();
    //    fedora.modifyDigitalObjectState(doId, state, "change state");
    //}

    /****
     * Record's digest
     */

    private String digestRecord(String loURI, String recordURI, String record, String namespace, String repoURI, String oaiId) throws Exception {
        ArrayList<Triple> triples = new ArrayList<Triple>();
        String storeId;
        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
        Store store = Store.getInstance();

        if (recordURI == null) {
            String recordId = store.createDigitalObject();

            //if (record == null) { //case used for new record from metadata editor. -AM
            //    if (namespace.equals(Constants.IEEE_LOM_NAMESPACE))
            //        record = Util.getNewLomXml( recordId );
            //    else if (namespace.equals(Constants.OAI_DC_NAMESPACE))
            //        record = Util.getNewDcXml( recordId );
            //}

            storeId = recordId; 
            // We remove the leading / beforehand.
            recordURI = Util.makeURI(recordId.substring( 1 ), COMETE.MetadataRecord.getURI());

            triples.add(new Triple(recordURI, RDF.type, COMETE.MetadataRecord));
            triples.add(new Triple(recordURI, COMETE.metadataFormat, namespace));

            triples.add(new Triple(recordURI, COMETE.storeDigitalObject, storeId));
            if( repoURI != null && !"".equals( repoURI ) )
                triples.add(new Triple(recordURI, COMETE.repository, repoURI));

            String recordLink = Core.getInstance().getCometeUrl() + "/rest/metadataRecords" + recordId + "/xml";
            triples.add(new Triple(recordURI, COMETE.originalDataLink, recordLink));

            //Resource association
            triples.add(new Triple(loURI, COMETE.hasMetadataRecord, recordURI));
            triples.add(new Triple(recordURI, COMETE.describes, loURI));

            //Fix the future oai identifier for oai-pmh exposition with oaiprovider (via Fedora RELS-EXT)
            //SHA of harvested oai id to keep a trace of source in our oai come from
            //String _oaiId = DigestUtils.shaHex(oaiId);
            //fedora.addRelationship(storeId, Constants.OAI_ID,
            //        "oai:" + Core.getInstance().getRepositoryNamespace()+ ":" + _oaiId, true);
        }
        else
            storeId = getStoreIdFromURI(recordURI);

        //store content
        store.setDatastream(storeId, Constants.DATASTREAM_DATA, record);

        validateRecord( storeId, loURI, recordURI, record, namespace );

        String extractedTriplesAsXml = processMetadataRecord( record, loURI, recordURI, namespace );

        Triple[] extractedTriples = Triple.readTriplesFromXml(extractedTriplesAsXml);
        triples.addAll(Arrays.asList(extractedTriples));

        String format = null, location = null;
        ArrayList<Triple> titles = new ArrayList<Triple>();
        ArrayList<Triple> descriptions = new ArrayList<Triple>();
        ArrayList<String> loLanguages = new ArrayList<String>();
        ArrayList<String> recordLanguages = new ArrayList<String>();
        for (Triple triple : triples) {
            if (FOAF.page.getURI().equals(triple.getPredicate())) {
                location = triple.getObject();
                if( location != null && !location.startsWith( "http" ) ) 
                    location = "http://" + location;
            }
            else if (DCTERMS.format.getURI().equals(triple.getPredicate()))
                format = triple.getObject();
            else if (DCTERMS.language.getURI().equals(triple.getPredicate())) {
                if( recordURI.equals( triple.getSubject() ) )
                    recordLanguages.add( triple.getObject() );
                else if( loURI.equals( triple.getSubject() ) )
                    loLanguages.add( triple.getObject() );
            }
            else if (DCTERMS.title.getURI().equals(triple.getPredicate()))
                titles.add( triple );
            else if (DCTERMS.description.getURI().equals(triple.getPredicate()))
                descriptions.add( triple );
        }

        if (format == null && location != null) {
            String mimetype = IOUtil.getMimeType( location );
            format = "http://purl.org/NET/mediatypes/" + mimetype;
            Triple tripleFormat = new Triple( loURI, DCTERMS.format, format );
            triples.add(tripleFormat);
        }
        
        manageLanguages( recordLanguages, loLanguages, recordURI, loURI, titles, descriptions, triples );

        //triples insertions
        tripleStore.insertTriplesWithTextIndex(triples, Constants.indexMetadataPredicates, Constants.INDEX_LANGUAGES, null);

        //Identity and vocabulary referencement management
        recordToInternalFormat(loURI, recordURI, storeId, metadataFormat);

        //automatic exposition to harvesting
        internalFormatToExposedRecords(loURI, storeId, metadataFormat);

        return recordURI;
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

    //    String recordXml = Core.getInstance().getFedora().getDatastream( storeId, Constants.DATASTREAM_DATA );

    //    return( processMetadataRecord( recordXml, learningObjectUri, metadataRecordUri, applicationProfile ) );
    //}

    private void manageLanguages( ArrayList<String> recordInitLanguages, ArrayList<String> loInitLanguages, String recordURI, String loURI, ArrayList<Triple> titles, ArrayList<Triple> descriptions, ArrayList<Triple> triples ) {
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
        tripleStore.sparqlUpdateWithTextIndex(query, Constants.indexMetadataPredicates, Constants.INDEX_LANGUAGES, null);
    }

    /*****
     * Internal/exposed format management
     */

    private void recordToInternalFormat( String loURI, String recordURI, String storeId, MetadataFormat metadataFormat ) throws Exception {
        if( !Store.getInstance().isDatastreamExists( storeId, Constants.DATASTREAM_DATA ) )
            return;

        String xml = Store.getInstance().getDatastream( storeId, Constants.DATASTREAM_DATA );

        HashMap<String,String> parameters = new HashMap<String,String>();
        parameters.put( "loURI", loURI );
        parameters.put( "recordURI", recordURI );

        String stylesheet = null;
        if( metadataFormat.getNamespace().equals( Constants.OAI_DC_NAMESPACE ))
            stylesheet = "metadata/convertDcToInternalFormat";
        else if( metadataFormat.getNamespace().equals( Constants.IEEE_LOM_NAMESPACE) )
            stylesheet = "metadata/convertLomToInternalFormat";

        StreamSource source = new StreamSource( new StringReader( xml ) );
        String newXml = Util.applyXslToDocument( stylesheet, source, parameters );

        Store.getInstance().setDatastream(storeId, Constants.DATASTREAM_INTERNAL_DATA, newXml);
    }

    private void internalFormatToExposedRecords(String loURI, String storeId, MetadataFormat metadataFormat) throws Exception {
        if( !Store.getInstance().isDatastreamExists( storeId, Constants.DATASTREAM_INTERNAL_DATA ) )
            return;
        String recordUri = Util.makeURI(Util.getIdNumberValue(storeId), COMETE.MetadataRecord);

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
    }

    /**
     *  Update the exposed datastream.
     *  @param id Record to update.
     *  @param record Content to update.
     *  @param metadataFormat Metadata format of the record.
     */
    void updateExposedFormat(String id, String record, MetadataFormat metadataFormat, String recordUri) throws Exception{
        String datastream = metadataFormat.getExposedDatastream();
        int resp = Store.getInstance().setDatastream(id, datastream, record);
        if (resp == Store.DATASTREAM_STORED)
            System.out.println("-> " + recordUri + " exposed (" + metadataFormat.getName() + " format)");
        else if (resp == Store.DATASTREAM_UNCHANGED)
            System.out.println("-> no change on : " + recordUri + ".");
    }

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
                if( !store.isDatastreamExists( storeId, reportDataStream ) )
                    store.setDatastream( storeId, reportDataStream, errorReport);
            }
            finally {
                long timeTaken = System.currentTimeMillis() - startTime;
                System.out.println( "Validation complete (elapsed time: "+ timeTaken + " ms): " + ( isValid ? "VALID" : "INVALID" ) );
            }
        }
    }

    /**
     * re expose record for modified identities
     * @param uri
     */
    public void exposeRecordsOfIdentity(String uri) throws Exception {
        /*Hashtable<String, String>[] results =
                Core.getInstance().getTripleStoreService().getResults( "getMetadataRecordsOfIdentity.sparql", uri );
        for( int i = 0; i < results.length; i++ ) {
            MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(Util.manageQuotes(results[i].get("metadataFormat")));
            String fedoraId = results[ i ].get( "doId" );
            String loUri = results[ i ].get( "lo" );
            internalFormatToExposedRecords(loUri, fedoraId, metadataFormat);
        }*/
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

    private boolean isValidatorInitialized = false;

}
