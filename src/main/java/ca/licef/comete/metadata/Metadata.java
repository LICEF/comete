package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.deployment.ResourceDeployer;
import com.sun.jersey.core.header.FormDataContentDisposition;
import licef.DateUtil;
import licef.IOUtil;
import licef.StringUtil;
import licef.XMLUtil;
import licef.tsapi.model.Triple;
import licef.tsapi.TripleStore;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

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
System.out.println( "getRecordURI oaiID="+oaiID+" namespace="+namespace );
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        //Hashtable<String, String>[] results = tripleStore.getResults("getMetadataRecordWith-oai-id.sparql", namespace, oaiID);
        //if (results.length > 0)
        //    return results[0].get("s");
        //else
        //    return null;
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
System.out.println( "recordURI="+recordURI );

                //if (recordURI == null) {
                //    loURI = Util.makeURI(Constants.TYPE_LEARNING_OBJECT);
                //    Core.getInstance().getTripleStoreService().addTriple(loURI, Constants.TYPE, Constants.TYPE_LEARNING_OBJECT);
                //    Core.getInstance().getTripleStoreService().addTriple(loURI, Constants.METAMODEL_ADDED, DateUtil.toISOString(new Date(), null, null));
                //    state = "created";
                //}
                //else {
                //    loURI = getLearningObjectURI(recordURI);
                //    resetLearningObjectNonPersistentTriples(recordURI);
                //    state = "updated";
                //}

                //recordURI = digestRecord(loURI, recordURI, content, namespace, null, "localRecord", pseudoOaiID);
                //tripleStore.addTriple(recordURI, Constants.OAI_ID, pseudoOaiID);

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

    private ResourceDeployer deployer;

}
