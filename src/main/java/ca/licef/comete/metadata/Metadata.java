package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.Settings;
import ca.licef.comete.core.metadataformat.MetadataFormat;
import ca.licef.comete.core.metadataformat.MetadataFormats;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.harvester.Harvester;
import ca.licef.comete.metadata.util.Util;
import ca.licef.comete.queryengine.QueryEngine;
import ca.licef.comete.queryengine.ResultEntry;
import ca.licef.comete.store.Store;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabularies.OAI;
import com.sun.jersey.core.header.FormDataContentDisposition;
import licef.DateUtil;
import licef.IOUtil;
import licef.StringUtil;
import licef.ZipUtil;
import licef.reflection.Invoker;
import licef.reflection.ThreadInvoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import licef.tsapi.vocabulary.RDF;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.language.LanguageIdentifier;
import org.ariadne.util.JDomUtils;
import org.ariadne.validation.Validator;
import org.ariadne.validation.exception.InitialisationException;
import org.ariadne.validation.exception.ValidationException;
import org.ariadne.validation.utils.ValidationUtils;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class Metadata {

    private static Metadata instance;
    private LearningObjectView learningObjectView;

    public static File tmpFolder = new File(System.getProperty("java.io.tmpdir"));

    TripleStore tripleStore = Core.getInstance().getTripleStore();
    static ca.licef.comete.core.util.Util CoreUtil;

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

    public String storeHarvestedRecord(String oaiID, String namespace, String repoUri, String record, String datestamp, 
            boolean isPendingByDefault, boolean isCheckingBrokenLink, boolean isCheckingInvalid, String invalidApplProf) throws Exception {
        String[] res = manageRecord(oaiID, namespace, repoUri, record, datestamp, isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, invalidApplProf);
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

        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "getRecordURI", new Object[] { oaiID, namespace } );

        String recordURI = (String)tripleStore.transactionalCall( inv );
        if (recordURI != null) {
            deleteRecord(recordURI, true);
            return "deleted";
        }
        else
            return "ignored";
    }

    public String getLearningObjectURI( String metadataRecordUri ) throws Exception {
        Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(metadataRecordUri, COMETE.describes);
        if( triples.length == 0 )
            return( null );
        else
            return( triples[ 0 ].getObject() );
    }

    public String getRecordURI( String oaiID, String namespace ) throws Exception {
        String query = CoreUtil.getQuery( "metadata/getMetadataRecordWith-oai-id.sparql", namespace, oaiID );
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] tuples = (Tuple[])tripleStore.transactionalCall(inv);
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

    public Object[] storeUploadedContent(File record, File resource, boolean isPendingByDefault, boolean isCheckingBrokenLink, boolean isCheckingInvalid, String invalidApplProf) throws Exception {
        Object[] results;
        String extension = record.getName().substring(record.getName().lastIndexOf('.') + 1).toLowerCase();
        if ("zip".equals(extension)) {
            File uploadedRecords = new File(tmpFolder, "uploadedRecords");
            ZipUtil.unzipFile(record.toString(), uploadedRecords.toString());
            String[] records = uploadedRecords.list();
            ArrayList<String[]> uris = new ArrayList<String[]>();
            for (String _record : records) {
                File rec = new File(uploadedRecords, _record);

                String[] res = storeUploadedRecord(rec, null, isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, invalidApplProf);
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
            String[] res = storeUploadedRecord(record, resource, isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, invalidApplProf );
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

    public String[] storeUploadedRecord(File file, File resource, 
            boolean isPendingByDefault, boolean isCheckingBrokenLink, boolean isCheckingInvalid, String invalidApplProf ) throws Exception {
        String[] values = Util.parseMetadataRecord(file);

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
        if (errorMessage == null) {
            String recordURI = getRecordURI(pseudoOaiID, namespace);
            if (recordURI != null) {
                if (isSameMetadataRecordAlreadyUploaded(record, recordURI)) {
                    Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore",
                            "getTriplesWithSubjectPredicate", new Object[]{recordURI,
                            COMETE.describes, new String[]{}});
                    Triple triple = ((Triple[])tripleStore.transactionalCall(inv))[0];
                    loURI = triple.getObject();
                    return new String[]{null, loURI, "ignored"};
                }
            }

            try {
                String now = DateUtil.toISOString(new Date(), null, null);
                String[] res = manageRecord(pseudoOaiID, namespace, null, record, now, isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, invalidApplProf);
                loURI = res[0];
                state = res[3];
            } catch (Exception e) {
                errorMessage = "Error on record parsing.";
            }
        }

        return new String[]{errorMessage, loURI, state};
    }

    private boolean isSameMetadataRecordAlreadyUploaded(String record, String recordURI) throws Exception{
        //Retrieve of stored record for similarity test
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore",
                "getTriplesWithSubjectPredicate", new Object[]{recordURI,
                COMETE.storeDigitalObject, new String[]{}});
        Triple triple = ((Triple[])tripleStore.transactionalCall(inv))[0];
        String xml = Store.getInstance().getDatastream(triple.getObject(), Constants.DATASTREAM_ORIGINAL_DATA);
        return DigestUtils.shaHex(record).equals(DigestUtils.shaHex(xml));
    }

    public Object[] isRecordExists(File record) throws Exception {
        String[] values = Util.parseMetadataRecord(record);
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
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doDeleteRepositoryRecords", new String[] { repoUri } );
        tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE ); 
    }

    public void doDeleteRepositoryRecords( String repoUri ) throws Exception {
        String[][] records = getRepositoryRecords(repoUri);
        for( int i = 0; i < records.length; i++ ) {
            String recordUri = records[ i ][ 0 ];
            deleteRecord( recordUri, false );
        }
        // Mark the records for deletion after that all the triples
        // have been deleted.  This way, if an error occurs while the
        // triples are deleted, the transaction will be aborted and no
        // data will be lost and the integrity will be kept.
        for( int i = 0; i < records.length; i++ ) {
            String storeId = records[ i ][ 1 ];
            markStoreRecordForDeletion( storeId );
        }

        //removing lastHarvest mark for future harvesting process
        Triple[] res = tripleStore.getTriplesWithSubjectPredicate(repoUri, DCTERMS.identifier);
        String defId = res[0].getObject();
        Harvester.getInstance().clearLastHarvest(defId);
    }

    public Set<String> getLearningObjectFlags(String loUri) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doGetLearningObjectFlags", new Object[] { loUri } );
        return( (Set<String>)tripleStore.transactionalCall( inv ) );
    }

    public Set<String> doGetLearningObjectFlags(String loUri) throws Exception {
        Set<String> flags = new HashSet<String>();
        Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(loUri, COMETE.flag);
        for( Triple t : triples )
            flags.add( t.getObject() );
        if( !flags.contains( "inactive" ) )
            flags.add( "active" );
        if( !flags.contains( "brokenLink" ) )
            flags.add( "validLink" );
        if( !flags.contains( "pending" ) )
            flags.add( "accepted" );
        if( !flags.contains( "invalid" ) )
            flags.add( "valid" );
        if( !flags.contains( "forcedDiffusion" ) )
            flags.add( "unforcedDiffusion" );
        return( flags );
    }

    public boolean getLearningObjectFlag( String loUri, String flag ) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doGetLearningObjectFlag", new Object[] { loUri, flag } );
        return( ((Boolean)tripleStore.transactionalCall( inv )).booleanValue() );
    }

    public Boolean doGetLearningObjectFlag(String loUri, String flag) throws Exception {
        Set<String> flags = getLearningObjectFlags( loUri );
        return( Boolean.valueOf( flags.contains( flag ) ) );
    }

    public void setLearningObjectsFlagByQuery( String query, String lang, boolean isShowHiddenRes, String flag, boolean value ) throws Exception {
        (new ThreadInvoker(new Invoker(this, "ca.licef.comete.metadata.Metadata",
                               "doSetLearningObjectsFlagByQuery", new Object[]{ query, lang, isShowHiddenRes, flag, value }))).start();
    }

    public void doSetLearningObjectsFlagByQuery( String query, String lang, boolean isShowHiddenRes, String flag, boolean value ) throws Exception {
        ResultSet rs = QueryEngine.getInstance().search( query, "", lang, isShowHiddenRes, "json", 0, Integer.MAX_VALUE, "default", null );
        for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
            ResultEntry entry = (ResultEntry)it.next();
            String loUri = CoreUtil.makeURI( entry.getId(), COMETE.LearningObject );
            setLearningObjectFlag( loUri, flag, value );
        }
    }

    public void setLearningObjectFlag(String loUri, String flag, boolean value) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doSetLearningObjectFlag", new Object[] { loUri, flag, value } );
        tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE );
    }

    public void doSetLearningObjectFlag(String loUri, String flag, boolean value) throws Exception {
        String recordUri = getMetadataRecordUriFromLO( loUri );
        Triple flaggedTriple = new Triple( loUri, COMETE.flag, flag );
        if( value )
            tripleStore.insertTriple( flaggedTriple );
        else 
            tripleStore.removeTriple( flaggedTriple );
        updateOaiDatestamp( recordUri, new Date() );
    }

    public void clearLearningObjectsFlagByQuery( String query, String lang, boolean isShowHiddenRes ) throws Exception {
        (new ThreadInvoker(new Invoker(this, "ca.licef.comete.metadata.Metadata",
                "doClearLearningObjectsFlagByQuery", new Object[]{ query, lang, isShowHiddenRes }))).start();
    }

    public void doClearLearningObjectsFlagByQuery( String query, String lang, boolean isShowHiddenRes ) throws Exception {
        ResultSet rs = QueryEngine.getInstance().search( query, "", lang, isShowHiddenRes, "json", 0, Integer.MAX_VALUE, "default", null );
        for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
            ResultEntry entry = (ResultEntry)it.next();
            String loUri = CoreUtil.makeURI( entry.getId(), COMETE.LearningObject );
            clearLearningObjectFlags( loUri );
        }
    }

    public void clearLearningObjectFlags(String loUri) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doClearLearningObjectFlags", new Object[] { loUri } );
        tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE );
    }

    public void doClearLearningObjectFlags(String loUri) throws Exception {
        Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(loUri, COMETE.flag);
        if (triples.length > 0) {
            tripleStore.removeTriplesWithSubjectPredicate(loUri, COMETE.flag);
            String recordUri = getMetadataRecordUriFromLO( loUri );
            updateOaiDatestamp(recordUri, new Date());
        }
    }

    public void updateOaiDatestamp( String recordUri, Date date ) throws Exception {
        String dateValue = DateUtil.toISOString(new Date(), null, null);
        String query = CoreUtil.getQuery( "metadata/updateOaiDatestamp.sparql", recordUri, dateValue );
        tripleStore.sparqlUpdate_textIndex(query);
    }

    public void deleteRecord(String recordURI, boolean markStoreRecordForDeletion) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "getLearningObjectURI", new Object[] { recordURI } );
        String loURI = (String)tripleStore.transactionalCall( inv );
        if( loURI != null ) 
            deleteLearningObject(loURI, markStoreRecordForDeletion);
    }

    public String getMetadataRecordUriFromLO(String loUri) throws Exception {
        Triple[] triples = tripleStore.getTriplesWithPredicateObject(COMETE.describes, loUri, null);
        if( triples.length == 0 )
            return( null );
        else
            return( triples[ 0 ].getSubject() );
    }

    public void deleteLearningObjectsByQuery( String query, String lang, boolean isShowHiddenRes ) throws Exception {
        (new ThreadInvoker(new Invoker(this, "ca.licef.comete.metadata.Metadata",
                               "doDeleteLearningObjectsByQuery", new Object[]{ query, lang, isShowHiddenRes }))).start();
    }

    public void doDeleteLearningObjectsByQuery( String query, String lang, boolean isShowHiddenRes ) throws Exception {
        ResultSet rs = QueryEngine.getInstance().search( query, "", lang, isShowHiddenRes, "json", 0, Integer.MAX_VALUE, "default", null );
        for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
            ResultEntry entry = (ResultEntry)it.next();
            String loUri = CoreUtil.makeURI( entry.getId(), COMETE.LearningObject );
            deleteLearningObject( loUri, true );
        }
    }

    public void deleteLearningObject(String loUri, boolean markStoreRecordForDeletion) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", "doDeleteLearningObject", new Object[] { loUri, markStoreRecordForDeletion } );
        tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE );
    }

    public void doDeleteLearningObject(String loUri, boolean markStoreRecordForDeletion) throws Exception {
        String query = CoreUtil.getQuery( "metadata/getRecordUriAndStoreIdFromLO.sparql", loUri );
        Tuple[] res = tripleStore.sparqlSelect( query );
        for( Tuple tuple : res ) {
            String recordUri = tuple.getValue( "s" ).getContent();
            String storeId = tuple.getValue( "storeId" ).getContent(); 
            deleteMetadataRecord(recordUri, ( markStoreRecordForDeletion ? storeId : null ) );
        }
        tripleStore.removeResource_textIndex(loUri);
    }

    private void deleteMetadataRecord(String recordURI, String storeId) throws Exception {
        markStoreRecordForDeletion(  storeId );
        tripleStore.removeResource_textIndex(recordURI);
    }

    private void markStoreRecordForDeletion( String storeId ) throws Exception {
        if( storeId == null )
            return;
        File metadataRecFolder = new File( Store.getInstance().getLocation() + storeId );
        if( metadataRecFolder.exists() ) {
            File markedForDeletionFile = new File( Store.getInstance().getLocation() + storeId + ".d" );
            if( !metadataRecFolder.renameTo( markedForDeletionFile ) )
                throw( new IOException( "Folder " + metadataRecFolder + " could not be renamed to " + markedForDeletionFile + "." ) );
        }
    }

    public String[][] getRepositoryRecords(String repoUri) throws Exception {
        String query = CoreUtil.getQuery( "metadata/getRepositoryRecords.sparql", repoUri );
        Tuple[] tuples = tripleStore.sparqlSelect( query );
        String[][] res = new String[ tuples.length ][ 2 ];
        for( int i = 0; i < tuples.length; i++ ) {
            res[ i ][ 0 ] = tuples[i].getValue("s").getContent();
            res[ i ][ 1 ] = tuples[i].getValue("storeId").getContent();
        }
        return res;
    }

    /****
     * Record's digest
     */

    private String[] manageRecord(String oaiId, String namespace, String repoUri, String record, String datestamp, 
            boolean isPendingByDefault, boolean isCheckingBrokenLink, boolean isCheckingInvalid, String invalidApplProf) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.metadata.Metadata",
                "digestRecord", new Object[]{record, namespace, repoUri, oaiId, datestamp, isPendingByDefault, isCheckingBrokenLink, isCheckingInvalid, invalidApplProf});
        String[] res = (String[])tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);

        //Identity and vocabulary referencement management
        String loURI = res[0];
        String recordURI = res[1];
        String storeId = res[2];
        linkToResources(loURI, recordURI, storeId, namespace);

        return res;
    }

    public String[] digestRecord(String record, String namespace, String repoURI, String oaiId, String datestamp, 
            boolean isPendingByDefault, boolean isCheckingBrokenLink, boolean isCheckingInvalid, String invalidApplProf) throws Exception {
        System.out.println( "Digesting record oaiId=" + oaiId + " from repoURI=" + repoURI + " datestamp=" + datestamp );        
        ArrayList<Triple> triples = new ArrayList<Triple>();
        String storeId;
        Store store = Store.getInstance();
        boolean isUpdate = false;

        MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(namespace);

        String loURI = null;

        //Retrieve of metadata record with metadata format and oaiID
        String recordURI = getRecordURI(oaiId, namespace);

        if (recordURI != null) {
            if (datestamp != null) {
                isUpdate = true;
                Triple[] _triples = tripleStore.getTriplesWithSubjectPredicate(recordURI, OAI.datestamp);
                if (_triples.length > 0) {
                    Date d1 = DateUtil.toDate(_triples[0].getObject());
                    Date d2 = DateUtil.toDate(datestamp);
                    if (d2.after(d1))
                        tripleStore.removeTriplesWithSubjectPredicate(recordURI, OAI.datestamp);
                    else
                        return new String[]{null, null, null, "ignored"};
                }
            }

            loURI = getLearningObjectURI(recordURI);
            resetLearningObjectNonPersistentTriples(recordURI);
            storeId = getStoreIdFromURI(recordURI);
        }
        else {
            ////Is there another metadata record with the same oai-id ?
            ////if yes, retrieve of the described resource
            String query = CoreUtil.getQuery( "metadata/getLearningObjectFromOtherMetadataRecord.sparql", oaiId );
            Tuple[] tuples = tripleStore.sparqlSelect( query );
            if (tuples.length > 0)
                loURI = tuples[0].getValue("res").getContent();

            //creation of new one
            if (loURI == null) {
                loURI = CoreUtil.makeURI(COMETE.LearningObject);
                triples.add( new Triple( loURI, RDF.type, COMETE.LearningObject ) );
                triples.add( new Triple( loURI, COMETE.added, DateUtil.toISOString(new Date(), null, null) ) );
                if( isPendingByDefault )
                    triples.add( new Triple( loURI, COMETE.flag, "pending" ) );
            }

            storeId = store.createDigitalObject( Store.PATH_RECORDS );

            // We remove the leading / beforehand.
            recordURI = CoreUtil.makeURI(storeId.substring( 1 ), COMETE.MetadataRecord.getURI());
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
        validateRecord( storeId, loURI, recordURI, record, namespace, isCheckingInvalid, invalidApplProf );

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

        if( isCheckingBrokenLink ) 
            manageBrokenLinks( loURI, triples );

        //automatic exposition to harvesting
        exposeRecords(loURI, storeId, metadataFormat);

        //oai-pmh properties
        if (!isUpdate)
            tripleStore.insertTriple( new Triple( recordURI, OAI.identifier, oaiId ) );

        //Set datestamp with current date unless not present (redigest case) -AM
        if (datestamp != null) {
            String now = DateUtil.toISOString(new Date(), null, null);
            tripleStore.insertTriple(new Triple(recordURI, OAI.datestamp, now));
        }

        String state = isUpdate?"updated":"added";

        return new String[]{loURI, recordURI, storeId, state};
    }

    private String processMetadataRecord( String xml, String loURI, String recordURI, String namespace ) throws Exception {
        String format = MetadataFormats.getMetadataFormat( namespace ).getName();

        StreamSource xmlSource = new StreamSource( new BufferedReader( new StringReader( xml ) ) );
        HashMap<String,String> params = new HashMap<String,String>();
        params.put( "loURI", loURI );
        params.put( "recordURI", recordURI );
        String triplesAsXml = CoreUtil.applyXslToDocument( "metadata/process" + StringUtil.capitalize( format ) + "Record", xmlSource, params );
        return( triplesAsXml );
    }

    private void manageFormat( String loURI, ArrayList<Triple> triples ) throws Exception{
        String format = null, location = null;
        for (Triple triple : triples) {
            if (FOAF.page.getURI().equals(triple.getPredicate()))
                location = triple.getObject();
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

    private void manageBrokenLinks( String loURI, ArrayList<Triple> triples ) throws Exception {
        boolean isBrokenLinkFound = false;
        for (Triple triple : triples) {
            if (FOAF.page.getURI().equals(triple.getPredicate())) {
                String location = triple.getObject();
                if( location != null && !location.startsWith( "http" ) )
                    location = "http://" + location;
                try {
                    BrokenLinkChecker.Result result = BrokenLinkChecker.getInstance().testUrlLocation( new URL( location ) );
                    boolean isValid = ( result.status == 200 );
                    if( !isValid ) {
                        isBrokenLinkFound = true;
                        break;
                    }
                }
                catch( IOException e ) {
                    // Assume that the link is broken if the testUrlLocation() method cannot complete.
                    isBrokenLinkFound = true;
                    break;
                }
            }
        }
        setLearningObjectFlag( loURI, "brokenLink", isBrokenLinkFound );
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
        CoreUtil.applyXslToDocument( stylesheet, source, parameters );
    }


    /*****
     * Record's Redigest
     */

    private void resetLearningObjectNonPersistentTriples(String recordURI) throws Exception {
        String query = CoreUtil.getQuery( "metadata/deleteLOTriplesToReset.sparql", recordURI );
        tripleStore.sparqlUpdate_textIndex(query);
    }

    public void redigestAllRecords() throws Exception {
        (new ThreadInvoker(new Invoker(this, "ca.licef.comete.metadata.Metadata",
                               "redigestAllRecordsEff", new Object[]{}))).start();
    }

    public void redigestAllRecordsEff() throws Exception {
        System.out.println("Start Reset Metamodel at : " + new Date());

        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore",
                "getTriplesWithPredicateObject", new Object[]{
                    RDF.type, COMETE.MetadataRecord.getURI(), null, new String[]{} } );
        Triple[] triples = ((Triple[])tripleStore.transactionalCall(inv));

        for (Triple triple : triples)
            redigestRecord(triple.getSubject());

        System.out.println("Reset Metamodel Done at : " + new Date());
    }

    public void redigestRecord(String recordUri) throws Exception {
        String query = CoreUtil.getQuery( "metadata/getMetadataRecordToRedigest.sparql", recordUri );
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] tuples = (Tuple[])tripleStore.transactionalCall(inv);
        if( tuples.length > 0 ) {
            Tuple tuple = tuples[0];
            String oaiId = tuple.getValue("oaiId").getContent();
            String namespace = tuple.getValue("metadataFormat").getContent();
            String storeId = tuple.getValue("storeId").getContent();
            String record = Store.getInstance().getDatastream(storeId, Constants.DATASTREAM_ORIGINAL_DATA);
            manageRecord(oaiId, namespace, null, record, null, false, false, false, null);
        }
    }


    /*****
     * exposed format management
     */
    private void exposeRecords(String loURI, String storeId, MetadataFormat metadataFormat) throws Exception {
        Store store = Store.getInstance();
        if( !store.isDatastreamExists( storeId, Constants.DATASTREAM_ORIGINAL_DATA ) )
            return;

        String recordURI = CoreUtil.makeURI( storeId.substring( 1 ), COMETE.MetadataRecord.getURI() );
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
            String newXml = CoreUtil.applyXslToDocument( stylesheet, source, parameters );
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

    private void validateRecord( String storeId, String loURI, String recordURI, String record, String namespace, boolean setInvalidFlag, String invalidApplProf ) throws Exception {
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
            String reportDataStream = CoreUtil.getReportDataStream( profileUri );
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
                    tripleStore.insertTriple( new Triple( recordURI, COMETE.validationReportLink, CoreUtil.getReportLink( storeId, profileUri ) ) );
                }
            }
            finally {
                long timeTaken = System.currentTimeMillis() - startTime;
                System.out.println( "Validation complete (elapsed time: "+ timeTaken + " ms): " + ( isValid ? "VALID" : "INVALID" ) );
            }
            if( setInvalidFlag && profileUri.equals( invalidApplProf ) )
                setLearningObjectFlag( loURI, "invalid", !isValid );
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
    
    public ResultSet getMetadataRecordApplicationProfiles( int start, int limit, String lang, String applProfile ) throws Exception {
        return( getMetadataRecordApplicationProfiles( start, limit, lang, applProfile, false ) );
    }

    /**
     * @param applProfile If <tt>null</tt>, all the application profiles will be returned.  Otherwise, only the selected application profile will be provided.
     * @param showOnlyInvalidRecords If <tt>true</tt> and applProf is different from <tt>null</tt>, only invalid records will be returned.
     */
    public ResultSet getMetadataRecordApplicationProfiles( int start, int limit, String lang, String applProfile, boolean showOnlyInvalidRecords ) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.Metadata", 
            "getMetadataRecordApplicationProfilesEff", new Object[] { start, limit, lang, applProfile, showOnlyInvalidRecords } );
        return( (ResultSet)tripleStore.transactionalCall( inv ) );
    }

    public ResultSet getMetadataRecordApplicationProfilesEff( int start, int limit, String lang, String applProfile, boolean showOnlyInvalidRecords ) throws Exception {
        ResultSet rs = new ResultSet();
        String query = CoreUtil.getQuery( "metadata/getAllMetadataRecordsCount.sparql" );
        if( applProfile != null && showOnlyInvalidRecords )
            query = CoreUtil.getQuery( "metadata/getInvalidMetadataRecordsCount.sparql", CoreUtil.getApplProfAbbreviation( applProfile ) );
        Tuple[] res = tripleStore.sparqlSelect( query );
        int count = Integer.parseInt( res[0].getValue( "count" ).getContent() );
        if( count > 0 ) {
            rs.setTotalRecords( count );
            List<String> recordUris = new ArrayList<String>();

            Hashtable<String, String> metadataFormats = new Hashtable<String, String>();
            query = CoreUtil.getQuery( "metadata/getAllMetadataRecordFormats.sparql", start, limit );
            if( applProfile != null && showOnlyInvalidRecords )
                query = CoreUtil.getQuery( "metadata/getInvalidMetadataRecordFormats.sparql", start, limit, CoreUtil.getApplProfAbbreviation( applProfile ) );
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

            Map<String,List<String>> tableApplProfs = getMetadataRecordApplProfTable( recordUris, applProfile );
            Map<String,List<String>> tableInvalidApplProfs = getMetadataRecordInvalidApplProfTable( recordUris,applProfile );

            Collections.sort( recordUris );
            for( String recordUri : recordUris ) {
                Map entry = new HashMap<String,String>();
                entry.put( "id", recordUri );
                Triple[] triples = tripleStore.getTriplesWithSubjectPredicate( recordUri, COMETE.describes );
                String loUri = (String)triples[ 0 ].getObject();
                String[] loTitles = tripleStore.getBestLocalizedLiteralObject( loUri, DCTERMS.title, lang );
                entry.put( "title", loTitles[ 0 ] );
                entry.put( "metadataFormat", metadataFormats.get( recordUri ) );

                List<String> applProfList = tableApplProfs.get( recordUri );
                entry.put( "profiles", applProfList );

                List<String> invalidApplProfList = tableInvalidApplProfs.get( recordUri );
                entry.put( "invalidProfiles", invalidApplProfList );
                
                String repoUri = metadataRecordRepoTable.get( recordUri );
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

    private Map<String,List<String>> getMetadataRecordApplProfTable( List<String> recordUris, String applProfile ) throws Exception {
        Map<String,List<String>> res = new HashMap<String,List<String>>();

        String union = buildMetadataRecordApplProfUnion( recordUris );
        String query = CoreUtil.getQuery( "metadata/getMetadataRecordApplicationProfiles.sparql", union );
        Tuple[] tuples = tripleStore.sparqlSelect( query );
        String tempUri = null;
        List<String> applProfList = null;
        for( Tuple tuple : tuples ) {
            String uri = tuple.getValue( "s" ).getContent();
            String applProf = tuple.getValue( "applProf" ).getContent();
            if( !uri.equals( tempUri ) ) {
                if( applProfList != null )
                    res.put( tempUri, applProfList );
                applProfList = new ArrayList<String>();
                tempUri = uri;
            }
            if( applProf != null && !applProf.equals( "" ) ) {
                if( applProfile == null || applProfile.equals( applProf ) )
                    applProfList.add( applProf );
            }
        }
        if( applProfList != null )
            res.put( tempUri, applProfList );

        return( res );
    }

    private Map<String,List<String>> getMetadataRecordInvalidApplProfTable( List<String> recordUris, String applProfile ) throws Exception {
        Map<String,List<String>> res = new HashMap<String,List<String>>();

        String union = buildMetadataRecordReportUnion( recordUris );
        String query = CoreUtil.getQuery( "metadata/getMetadataRecordValidationReports.sparql", union );
        Tuple[] tuples = tripleStore.sparqlSelect( query );
        String tempUri = null;
        List<String> invalidApplProfList = null;
        for( Tuple tuple : tuples ) {
            String uri = tuple.getValue( "s" ).getContent();
            String report = tuple.getValue( "report" ).getContent();
            if( !uri.equals( tempUri ) ) {
                if( invalidApplProfList != null )
                    res.put( tempUri, invalidApplProfList );
                invalidApplProfList = new ArrayList<String>();
                tempUri = uri;
            }

            if( report != null && !"".equals( report ) ) {
                String applProfUri = CoreUtil.getProfileUriFromReportLink( report );
                if( applProfile == null || applProfile.equals( applProfUri ) )
                    invalidApplProfList.add( applProfUri );
            }
        }
        if( invalidApplProfList != null )
            res.put( tempUri, invalidApplProfList );

        return( res );
    }
   
    /*
     * URI Store conversion
     */

    private String getStoreIdFromURI(String uri) {
        String id = null;
        if (!uri.startsWith("http://"))
            return uri;
        else {
            try {
                Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(
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
        String repoInfoConstraint = CoreUtil.buildFilterConstraints( recordUris, "s", true, "=", "||" );
        String query = CoreUtil.getQuery( "metadata/getMetadataRecordRepoInfo.sparql", repoInfoConstraint );
        Tuple[] resRepoInfo = tripleStore.sparqlSelect( query );
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

    private String buildMetadataRecordApplProfUnion( List<String> recordUris ) {
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

    private String buildMetadataRecordReportUnion( List<String> recordUris ) {
        StringBuilder union = new StringBuilder();
        String unionDelimiter = "";
        for( int i = 0; i < recordUris.size(); i++ ) {
            union.append( unionDelimiter );
            union.append( "{" );
            union.append( "?s rdf:type comete:MetadataRecord ." );
            union.append( "?s comete:describes ?record ." );
            union.append( "?record comete:hasMetadataRecord <" ).append( recordUris.get( i ) ).append( "> ." );
            union.append( "OPTIONAL { ?s comete:validationReportLink ?report }" );
            union.append( "}" );
            unionDelimiter = " UNION ";
        }
        return( union.toString() );
    }

    private boolean isValidatorInitialized = false;
}
