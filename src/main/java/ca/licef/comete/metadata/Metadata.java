package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.FedoraService;
import ca.licef.comete.core.metadataformat.MetadataFormat;
import ca.licef.comete.core.metadataformat.MetadataFormats;
import ca.licef.comete.core.metadataformat.OAI_DC;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.deployment.ResourceDeployer;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.core.header.FormDataContentDisposition;
import licef.DateUtil;
import licef.IOUtil;
import licef.StringUtil;
import licef.XMLUtil;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.TripleStore;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class Metadata {

    private static Metadata instance;

    public static File tmpFolder = new File(System.getProperty("java.io.tmpdir"));

    public static Metadata getInstance() {
        if (instance == null)
            instance = new Metadata();
        return (instance);
    }

    public String storeHarvestedRecord(String oaiID, String namespace, String repoUri, String record, String datestamp, boolean isUpdate) throws Exception {
        /*if (datestamp != null) {
            String recordURI = getRecordURI(oaiID, namespace);
            if (recordURI != null) {
                isUpdate = true;
                TripleStoreService tripleStore = Core.getInstance().getTripleStoreService();
                Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(recordURI, Constants.OAI_DATESTAMP);
                if (triples.length > 0) {
                    Date d1 = DateUtil.toDate(triples[0].getObject());
                    Date d2 = DateUtil.toDate(datestamp);
                    if (d2.after(d1))
                        tripleStore.deleteTriples(triples);
                    else
                        return "ignored";
                }
            }
        }*/

        return storeHarvestedRecordEff(oaiID, namespace, repoUri, record, datestamp, isUpdate);
    }

    public String storeHarvestedRecordEff(String oaiID, String namespace, String repoUri, String record, String datestamp, boolean isUpdate ) throws Exception {
        //MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
        /*System.out.println("storeHarvestedRecord: " +  oaiID + " (" + metadataFormat.getName() + " format)");
        TripleStoreService tripleStore = Core.getInstance().getTripleStoreService();

        String loURI = null;

        //Retrieve of metadata record with metadata format and oaiID
        String recordURI = getRecordURI(oaiID, namespace);

        if (recordURI == null) {
            isUpdate = false; //to force new MetadataRecord creation context

            //Is there another metadata record with the same oai-id ?
            //if yes, retrieve of the described resource
            Hashtable<String, String>[] res = tripleStore.getResults("getLearningObjectFromOtherMetadataRecord.sparql", oaiID);
            if (res.length > 0)
                loURI = res[0].get("res");

            //creation of new one
            if (loURI == null) {
                loURI = Util.makeURI(Constants.TYPE_LEARNING_OBJECT);
                tripleStore.addTriple(loURI, Constants.TYPE, Constants.TYPE_LEARNING_OBJECT);
                tripleStore.addTriple(loURI, Constants.METAMODEL_ADDED, DateUtil.toISOString(new Date(), null, null));
            }
        }
        else {
            loURI = getLearningObjectURI(recordURI);
            resetLearningObjectNonPersistentTriples(recordURI);
        }

        recordURI = digestRecord(loURI, recordURI, record, namespace, repoUri, logMessage, oaiID);

        //oai-pmh properties
        if (!isUpdate)
            tripleStore.addTriple(recordURI, Constants.OAI_ID, oaiID);
        if (datestamp != null)
            tripleStore.addTriple(recordURI, Constants.OAI_DATESTAMP, datestamp); //always set. also for previous cases. -AM
*/

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

    public String getRecordURI( String oaiID, String namespace ) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        String query = Util.getQuery( "getMetadataRecordWith-oai-id.sparql", namespace, oaiID );
        Tuple[] tuples = tripleStore.sparqlSelect(query);
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

System.out.println( "fileDetail.fn="+fileDetail.getFileName() );        
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
System.out.println( "errorMessage="+errorMessage );
System.out.println( "record="+record );
System.out.println( "resource="+resource);

        return new Object[]{errorMessage, record, resource};
    }

    public Object[] storeUploadedContent(File record, File resource) throws Exception {
        Object[] results;
        String extension = record.getName().substring(record.getName().lastIndexOf('.') + 1).toLowerCase();
        if ("zip".equals(extension)) {
System.out.println( "handle zip" ); results = new Object[]{"No valid record inside ZIP archive."};

            //File uploadedRecords = new File(tmpFolder, "uploadedRecords");
            //ZipUtil.unzipFile(record.toString(), uploadedRecords.toString());
            //String[] records = uploadedRecords.list();
            //ArrayList<String[]> uris = new ArrayList<String[]>();
            //for (String _record : records) {
            //    File rec = new File(uploadedRecords, _record);
            //    String[] res = storeUploadedRecord(rec, null);
            //    if (res[1] != null)
            //        uris.add(new String[]{res[1], res[2]});
            //    rec.delete();
            //}
            //if (uris.isEmpty())
            //    results = new Object[]{"No valid record inside ZIP archive."};
            //else {
            //    String[][] loUris = new String[uris.size()][];
            //    for (int i = 0; i < uris.size(); i++)
            //        loUris[i] = new String[]{uris.get(i)[0], uris.get(i)[1]};
            //    results = new Object[]{loUris, null};
            //}
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

    String[] parseMetadataRecord(File file) {
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

    public String[] storeUploadedRecord(File file, File resource) throws Exception {
        String[] values = parseMetadataRecord(file);

for( int i = 0; i < values.length; i++ ) 
System.out.println( "values["+i+"]="+values[i]);

        String errorMessage = values[0];

        String content = values[1];
        String namespace = values[2];
        String pseudoOaiID = values[3]; //mapping of oai-id for local records

        //deploy resource here if present
        if (resource != null) {
            String error;
            try {
                error = deployer.deploy(resource, content);
            } catch (Exception e) {
                error = e.toString();
            }
            if (error != null)
                errorMessage = "Cannot deploy resource : " + error;
        }

        String loURI = null;
        String state = null;
        if (errorMessage == null) {
            try {
                TripleStore tripleStore = Core.getInstance().getTripleStore();
                String recordURI = getRecordURI(pseudoOaiID, namespace);
                if (recordURI == null) {
                    loURI = Util.makeURI(COMETE.LearningObject.getURI());
System.out.println( "loURI="+loURI );       
                    List<Triple> triples = new ArrayList<Triple>();
                    triples.add( new Triple( loURI, RDF.type, COMETE.LearningObject.getURI()) );
                    triples.add( new Triple( loURI, COMETE.added, DateUtil.toISOString(new Date(), null, null) ) );
                    tripleStore.insertTriples( triples );
                    state = "created";
System.out.println( "Created" );                    
                }
                //else {
                //    loURI = getLearningObjectURI(recordURI);
                //    resetLearningObjectNonPersistentTriples(recordURI);
                //    state = "updated";
                //}

                recordURI = digestRecord(loURI, recordURI, content, namespace, null, "localRecord", pseudoOaiID);
                tripleStore.insertTriple(new Triple( recordURI, Constants.OAI_ID, pseudoOaiID, false ) );

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
            //String recordURI = getRecordURI(pseudoOaiID, namespace);
            String recordURI = null;
            isExists = (recordURI != null);
        }
        return new Object[]{errorMessage, isExists};
    }

    /****
     * Record's digest
     */

    String digestRecord(String loURI, String recordURI, String record, String namespace, String repoURI, String logMessage, String oaiId) throws Exception {
        ArrayList<Triple> triples = new ArrayList<Triple>();
        String fedoraId;
        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);
        FedoraService fedora = Core.getInstance().getFedoraService();
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        if (recordURI == null) {
            String recordId = fedora.createDigitalObject();
System.out.println( "recordId afer fedora.createDigitalObject()="+recordId );

            //if (record == null) { //case used for new record from metadata editor. -AM
            //    if (namespace.equals(Constants.IEEE_LOM_NAMESPACE))
            //        record = Util.getNewLomXml( recordId );
            //    else if (namespace.equals(Constants.OAI_DC_NAMESPACE))
            //        record = Util.getNewDcXml( recordId );
            //}

            fedoraId = /*"info:fedora/" +*/ recordId;
            recordURI = Util.makeURI(recordId, COMETE.MetadataRecord);
System.out.println( "fedoraId="+fedoraId+" recordURI="+recordURI );

            triples.add(new Triple(recordURI, RDF.type, COMETE.MetadataRecord.getURI()));
            triples.add(new Triple(recordURI, COMETE.metadataFormat, namespace));
            triples.add(new Triple(recordURI, COMETE.fedoraDigitalObject, fedoraId));
            if( repoURI != null && !"".equals( repoURI ) )
                triples.add(new Triple(recordURI, COMETE.repository, repoURI));

            ////format datastream creation
            //fedora.addDatastream(fedoraId, Constants.DATASTREAM_DATA, Constants.DATASTREAM_DATA_LABEL, false, record, "text/xml", namespace, "M", logMessage);
            //triples.add(new Triple(recordURI, Constants.METAMODEL_LOCATION, Core.getInstance().getFedoraUrl() + "/objects/" + recordId + "/datastreams/data/content"));

            ////Resource association
            //triples.add(new Triple(loURI, Constants.METAMODEL_METADATA_RECORD, recordURI));
            //triples.add(new Triple(recordURI, Constants.METAMODEL_DESCRIBES, loURI));

            ////Fix the future oai identifier for oai-pmh exposition with oaiprovider (via Fedora RELS-EXT)
            ////SHA of harvested oai id to keep a trace of source in our oai come from
            //String _oaiId = DigestUtils.shaHex(oaiId);
            //fedora.addRelationship(fedoraId, Constants.OAI_ID,
            //        "oai:" + Core.getInstance().getRepositoryNamespace()+ ":" + _oaiId, true);
        }
        else {
            //fedoraId = getFedoraIdFromURI(recordURI);

            ////format datastream update
            //String previous = fedora.getDatastream(fedoraId, Constants.DATASTREAM_DATA);
            //if (!record.equals(previous))
            //    fedora.modifyDatastream(fedoraId, Constants.DATASTREAM_DATA, record, logMessage);
        }

        //validateRecord( fedoraId, loURI, recordURI, record, namespace, logMessage );

        //String extractedTriplesAsXml = processMetadataRecord( record, loURI, recordURI, namespace );
        //Triple[] extractedTriples = Triple.readTriplesFromXml(extractedTriplesAsXml);
        //triples.addAll(Arrays.asList(extractedTriples));

        //String format = null, location = null;
        //ArrayList<Triple> titles = new ArrayList<Triple>();
        //ArrayList<Triple> descriptions = new ArrayList<Triple>();
        //ArrayList<String> loLanguages = new ArrayList<String>();
        //ArrayList<String> recordLanguages = new ArrayList<String>();
        //for (Triple triple : triples) {
        //    if (Constants.METAMODEL_LOCATION.equals(triple.getPredicate())) {
        //        location = triple.getObject();
        //        if( location != null && !location.startsWith( "http" ) ) 
        //            location = "http://" + location;
        //    }
        //    else if (Constants.METAMODEL_FORMAT.equals(triple.getPredicate()))
        //        format = triple.getObject();
        //    else if (Constants.METAMODEL_LANGUAGE.equals(triple.getPredicate())) {
        //        if( recordURI.equals( triple.getSubject() ) )
        //            recordLanguages.add( triple.getObject() );
        //        else if( loURI.equals( triple.getSubject() ) )
        //            loLanguages.add( triple.getObject() );
        //    }
        //    else if (Constants.METAMODEL_TITLE.equals(triple.getPredicate()))
        //        titles.add( triple );
        //    else if (Constants.METAMODEL_DESCRIPTION.equals(triple.getPredicate()))
        //        descriptions.add( triple );
        //}

        //if (format == null && location != null) {
        //    String mimetype = IOUtil.getMimeType( location );
        //    format = "http://purl.org/NET/mediatypes/" + mimetype;
        //    Triple tripleFormat = new Triple( loURI, Constants.METAMODEL_FORMAT, format, false );
        //    triples.add(tripleFormat);
        //}
        //
        //manageLanguages( recordLanguages, loLanguages, recordURI, loURI, titles, descriptions, triples );

        ////triples insertions
        //tripleStore.addTriples(triples);

        ////Identity and vocabulary referencement management
        //recordToInternalFormat(loURI, recordURI, fedoraId, metadataFormat);

        ////automatic exposition to harvesting
        //internalFormatToExposedRecords(loURI, fedoraId, metadataFormat);

        return recordURI;
    }

    //public String processMetadataRecord( String xml, String loURI, String recordURI, String namespace ) throws Exception {
    //    String format = MetadataFormats.getMetadataFormat( namespace ).getName();

    //    StreamSource xmlSource = new StreamSource( new BufferedReader( new StringReader( xml ) ) );
    //    HashMap<String,String> params = new HashMap<String,String>();
    //    params.put( "loURI", loURI );
    //    params.put( "recordURI", recordURI );
    //    String triplesAsXml = Util.applyXslToDocument( "process" + StringUtil.capitalize( format ) + "Record", xmlSource, params );
    //    return( triplesAsXml );
    //}

    //public String processMetadataRecord( String recordId ) throws Exception {
    //    String metadataRecordUri = Util.makeURI( recordId, Constants.TYPE_METADATA_RECORD );
    //    String fedoraId = Core.getInstance().getTripleStoreService().getFedoraIdFromURI( metadataRecordUri );
    //    String applicationProfile = null;
    //    String learningObjectUri = null;
    //    Triple[] metadataRecordTriples = Core.getInstance().getTripleStoreService().getTriplesWithSubject( metadataRecordUri );
    //    for( int i = 0; i < metadataRecordTriples.length; i++ ) {
    //        Triple triple = metadataRecordTriples[ i ];
    //        if( Constants.METAMODEL_APPLICATION_PROFILE.equals( triple.getPredicate() ) )
    //            applicationProfile = triple.getObject();
    //        else if( Constants.METAMODEL_DESCRIBES.equals( triple.getPredicate() ) )
    //            learningObjectUri = triple.getObject();
    //    }

    //    String recordXml = Core.getInstance().getFedoraService().getDatastream( fedoraId, Constants.DATASTREAM_DATA );

    //    return( processMetadataRecord( recordXml, learningObjectUri, metadataRecordUri, applicationProfile ) );
    //}

    //private void manageLanguages( ArrayList<String> recordInitLanguages, ArrayList<String> loInitLanguages, String recordURI, String loURI, ArrayList<Triple> titles, ArrayList<Triple> descriptions, ArrayList<Triple> triples ) {
    //    HashSet<String> recordLanguages = new HashSet<String>();
    //    
    //    // If no languages are specified for the metadata record, try to guess it.
    //    if( recordInitLanguages.isEmpty() ) {

    //        // Check if some language attributes are already set for titles and descriptions.
    //        for( Triple t : titles ) {
    //            if( t.getLanguage() != null )
    //                recordLanguages.add( t.getLanguage() );
    //        }
    //        for( Triple t : descriptions ) {
    //            if( t.getLanguage() != null )
    //                recordLanguages.add( t.getLanguage() );
    //        }

    //        // Otherwise, try to guess the language used in the descriptions.  The descriptions are likely longer than titles and
    //        // might give better results.
    //        if( recordLanguages.isEmpty() ) {
    //            for( Triple t : descriptions ) {
    //                LanguageIdentifier langIdentifier = new LanguageIdentifier( t.getObject() );
    //                recordLanguages.add( langIdentifier.getLanguage() );
    //            }
    //        }

    //        // Otherwise, try to guess the language using the titles.
    //        if( recordLanguages.isEmpty() ) {
    //            for( Triple t : titles ) {
    //                LanguageIdentifier langIdentifier = new LanguageIdentifier( t.getObject() );
    //                recordLanguages.add( langIdentifier.getLanguage() );
    //            }
    //        }

    //        // Add the triples for the recordLanguages found.
    //        for( String lang : recordLanguages ) {
    //            Triple triple = new Triple( recordURI, Constants.METAMODEL_LANGUAGE, lang );
    //            triples.add( triple );
    //        }
    //    }

    //    // If no languages are specified for the resource, we try to guess it.
    //    if( loInitLanguages.isEmpty() ) {
    //        if( !recordInitLanguages.isEmpty() ) {
    //            // Take the initial languages of the metadata records if any.
    //            for( String lang : recordInitLanguages ) {
    //                Triple triple = new Triple( loURI, Constants.METAMODEL_LANGUAGE, lang );
    //                triples.add( triple );
    //            }
    //        }
    //        else {
    //            // Otherwise take the languages that we guessed for the metadata record.
    //            for( String lang : recordLanguages ) {
    //                Triple triple = new Triple( loURI, Constants.METAMODEL_LANGUAGE, lang );
    //                triples.add( triple );
    //            }
    //        }
    //    }
    //}

    /*
    * URI Fedora conversion
    */

    private String getURIFromFedoraId(String fedoraId) {
        String uri = null;
        if (fedoraId.startsWith("http://"))
            return fedoraId;
        else {
            if (!fedoraId.startsWith("info:fedora/"))
                fedoraId = "info:fedora/" + fedoraId;
            try {
                Triple[] triples = Core.getInstance().getTripleStore().getTriplesWithPredicateObject(
                        COMETE.fedoraDigitalObject, fedoraId, false, null);
                if (triples.length > 0)
                    uri = triples[0].getSubject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    private String getFedoraIdFromURI(String uri) {
        String id = null;
        if (!uri.startsWith("http://"))
            return uri;
        else {
            try {
                Triple[] triples = Core.getInstance().getTripleStore().getTriplesWithSubjectPredicate(
                        uri, COMETE.fedoraDigitalObject);
                if (triples.length > 0)
                    id = triples[0].getObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    private ResourceDeployer deployer;

}
