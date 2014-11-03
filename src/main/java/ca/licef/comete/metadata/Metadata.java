package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import licef.DateUtil;
import licef.tsapi.model.Triple;

import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

public class Metadata {

    private static Metadata instance;

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


}
