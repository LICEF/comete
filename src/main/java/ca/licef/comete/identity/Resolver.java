package ca.licef.comete.identity;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.identity.util.Util;
import ca.licef.comete.metadata.Metadata;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 25-Oct-2011
 */
public class Resolver {

    static TripleStore tripleStore = Core.getInstance().getTripleStore();
    static ca.licef.comete.core.util.Util CoreUtil;

    public void setIdentityValues(String uri, JSONObject mainValues, OntClass type,
                                        JSONObject details, ArrayList<Triple> newTriples, ArrayList<Triple> triplesToDelete) throws Exception {

        //reset uri values
        String query = COMETE.Person.equals(type)?
                CoreUtil.getQuery("identity/getPersonTriplesToResetForMerge.sparql", uri):
                CoreUtil.getQuery("identity/getOrganizationTriplesToResetForMerge.sparql", uri);

        Triple[] previousTriples = tripleStore.getTriplesInSparql(query);

        triplesToDelete.addAll(Arrays.asList(previousTriples));
        ArrayList<String> usedKeys = new ArrayList<String>();
        //loop for populate target_uri with main values as main and others as alternates
        for (Iterator it = details.keys(); it.hasNext();) {
            String key = it.next().toString();
            //main value
            String mainValue = null;
            try {
                mainValue = mainValues.getString(key);
            } catch (JSONException e) {
            }
            if (mainValue != null) {
                if ("email".equals(key)) mainValue = "mailto:" + mainValue;
                if ("tel".equals(key)) mainValue = "tel:" + mainValue;
                if ("fax".equals(key)) mainValue = "fax:" + mainValue;
                newTriples.add(new Triple(uri, Constants.getPredicate(key, false), mainValue));
            }

            JSONArray values = details.getJSONArray(key);
            for (int i = 0; i < values.length(); i++) {
                JSONObject _val = values.getJSONObject(i);
                String val = _val.getString("value");
                if ("email".equals(key)) val = "mailto:" + val;
                if ("tel".equals(key)) val = "tel:" + val;
                if ("fax".equals(key)) val = "fax:" + val;
                if (!val.equals(mainValue))
                    newTriples.add(new Triple(uri, Constants.getPredicate(key, true), val));
            }

            usedKeys.add(key);
        }

        //main values typed into editor dialog
        for (Iterator it = mainValues.keys(); it.hasNext();) {
            String key = (String)it.next();
            if (!usedKeys.contains(key)) {
                String val = mainValues.getString(key);
                if ("email".equals(key)) val = "mailto:" + val;
                if ("tel".equals(key)) val = "tel:" + val;
                if ("fax".equals(key)) val = "fax:" + val;
                newTriples.add(new Triple(uri, Constants.getPredicate(key, false), val));
            }
        }
    }


    /**
     * Update values from identity edition
     *
     * @param uri identity to update
     * @param mainValues main values
     * @param type identity type
     */
    public void updateIdentity(String uri, JSONObject mainValues, OntClass type) throws Exception {
        ArrayList<Triple> newTriples = new ArrayList<Triple>();
        ArrayList<Triple> triplesToDelete = new ArrayList<Triple>();

        //retrieve all values for the uri (main and alternates)
        JSONArray wrapper = new JSONArray();
        wrapper.put(uri);
        JSONObject details = COMETE.Person.equals(type)?
                Identity.getInstance().getAllPersonDetails(wrapper):
                Identity.getInstance().getAllOrganizationDetails(wrapper);

        //values management
        setIdentityValues(uri, mainValues, type, details, newTriples, triplesToDelete);

        //delete triples
        tripleStore.removeTriples(triplesToDelete);
        //add new triples
        tripleStore.insertTriples(newTriples);

        //re expose related records of uri (mainly for merged uris).
        //Metadata.getInstance().exposeRecordsOfIdentity(uri);
    }

    /**
     * Merges multiple identities by cumulate differents values of sources into target uri as main or alternatives values.
     *
     * @param uriArray list of uris to merge together
     * @param mainValues main values
     * @param similarGroup groupId of similarity
     * @param type identity type
     * @throws Exception
     */
    public String mergeIdentities(JSONArray uriArray, JSONObject mainValues, String similarGroup, OntClass type) throws Exception {
        ArrayList<Triple> newTriples = new ArrayList<Triple>();
        ArrayList<Triple> triplesToDelete = new ArrayList<Triple>();
        ArrayList<Triple> triplesToDeleteInSimilarityGraph = new ArrayList<Triple>();
        ArrayList<String> uris = new ArrayList<String>();

        String uri = null;
        //Find the target_uri; the historically first one
        int min = -1;
        for (int i = 0; i < uriArray.length(); i++) {
            String _uri = uriArray.getString(i);
            uris.add(_uri);
            int id = Integer.parseInt(ca.licef.comete.core.util.Util.getIdNumberValue(_uri));
            if (i == 0 || id < min) {
                min = id;
                uri = _uri;
            }
        }

        //if not similar group, new group of all involved uris
        if (similarGroup == null) {
            ArrayList<String> groupIds = new ArrayList<String>();
            ArrayList<Triple> similarTriples = new ArrayList<Triple>();

            //uuid generation
            similarGroup = UUID.randomUUID().toString();

            //set target uri with new group
            similarTriples.add(new Triple(uri, DCTERMS.identifier, similarGroup));

            //retrieve all possible gids related to next merged uris
            String constraints = CoreUtil.buildFilterConstraints(uris, "s", true, "=", "||");

            //todo sparql dans le graph Identity.IDENTITY_SIMILARITY_GRAPH
            String query = CoreUtil.getQuery("identity/getInvolvedSimilarGroups.sparql" , constraints);
            Tuple[] results = tripleStore.sparqlSelect(query);
            for (int i = 0; i < results.length; i++)
                groupIds.add(CoreUtil.manageQuotes(results[i].getValue("gid").getContent()));

            //delete and reset the previous commons gid if exists
            if (!groupIds.isEmpty()) {
                constraints = CoreUtil.buildFilterConstraints(groupIds, "gid", false, "=", "||");
                //todo remove select
//                Triple[] existentSimilarTriples = tripleStore.getTriplesFromGraph("getSimilarIdentities.sparql", Identity.IDENTITY_SIMILARITY_GRAPH, constraints );
//                tripleStore.removeTriples(existentSimilarTriples, Identity.IDENTITY_SIMILARITY_GRAPH);
//                for( Triple exTriple : existentSimilarTriples) {
//                    exTriple.setObject(similarGroup);
//                    similarTriples.add(exTriple);
//                }
            }

            if (similarTriples.size() > 1)
                tripleStore.insertTriples(similarTriples, Identity.IDENTITY_SIMILARITY_GRAPH);
        }

        //retrieve all values for the target_uri (main and alternates)
        JSONObject details = COMETE.Person.equals(type)?
                Identity.getInstance().getAllPersonDetails(uriArray):
                Identity.getInstance().getAllOrganizationDetails(uriArray);

        //values management
        setIdentityValues(uri, mainValues, type, details, newTriples, triplesToDelete);

        //substitution for merged uris
        for (int i = 0; i < uriArray.length(); i++) {
            String mergedUri = uriArray.getString(i);

            //ignore target uri
            if (mergedUri.equals(uri))
                continue;

            if (COMETE.Person.equals(type)) {// for persons, set all others possible orgs (merged uri outgoing links)
                Triple[] _triples = tripleStore.getTriplesWithSubjectPredicate(mergedUri, DCTERMS.isPartOf);
                for (Triple triple : _triples)
                    newTriples.add(new Triple(uri, triple.getPredicate(), triple.getObject(),
                            triple.isObjectLiteral(), triple.getLanguage()));
            }

            //redirections for merged uri incoming links (LOs, records)
            Triple[] mergedIncomingTriples = tripleStore.getTriplesWithObject(mergedUri, false, null);
            for (Triple mergeIncomingTriple : mergedIncomingTriples)
                newTriples.add(new Triple(mergeIncomingTriple.getSubject(), mergeIncomingTriple.getPredicate(), uri, false));

            //Keep reference of merged uri
            newTriples.add(new Triple(uri, DCTERMS.replaces, mergedUri));
            //and set all previous merged replace references
            Triple[] _triples = tripleStore.getTriplesWithSubjectPredicate(mergedUri, DCTERMS.replaces);
            for (Triple triple : _triples)
                newTriples.add(new Triple(uri, DCTERMS.replaces, triple.getObject()));

            //delete of merged uri (incoming, outgoing triples and in similarity graph)
            triplesToDelete.addAll(Arrays.asList(mergedIncomingTriples));
            triplesToDelete.addAll(Arrays.asList(tripleStore.getTriplesWithSubject(mergedUri)));
            triplesToDeleteInSimilarityGraph.addAll(Arrays.asList(tripleStore.getTriplesWithSubject(mergedUri, Identity.IDENTITY_SIMILARITY_GRAPH)));
        }

        //delete triples
        tripleStore.removeTriples(triplesToDelete);
        tripleStore.removeTriples(triplesToDeleteInSimilarityGraph, Identity.IDENTITY_SIMILARITY_GRAPH);

        //add new triples
        tripleStore.insertTriples(newTriples);

        //possibly cleaning groups
        cleanSimilarityGroup(similarGroup);

        //re expose related records of uri (mainly for merged uris).
        //Metadata.getInstance().exposeRecordsOfIdentity(uri);

        return uri;
    }

    public void takeOffIdentities(JSONArray uriArray, String similarGroup) throws Exception {
        ArrayList<Triple> triplesToDelete = new ArrayList<Triple>();
        for (int i = 0; i < uriArray.length(); i++) {
            String uri = uriArray.getString(i);
            triplesToDelete.add(new Triple(uri, DCTERMS.identifier, similarGroup));
        }
        tripleStore.removeTriples(triplesToDelete, Identity.IDENTITY_SIMILARITY_GRAPH);
        cleanSimilarityGroup(similarGroup);
    }

    public void convertPersonsToOrg(JSONArray uriArray) throws Exception{
        ArrayList<Triple> newTriples = new ArrayList<Triple>();
        ArrayList<Triple> triplesToDelete = new ArrayList<Triple>();
        ArrayList<Triple> triplesToDeleteInSimilarityGraph = new ArrayList<Triple>();

        ArrayList<String> personUris = new ArrayList<String>();

        //substitution for converted uris
        for (int i = 0; i < uriArray.length(); i++) {
            String personUri = uriArray.getString(i);
            personUris.add(personUri);
            String id = CoreUtil.getIdNumberValue(personUri);
            String orgUri = CoreUtil.makeURI(id, COMETE.Organization);

            //typecasting for outgoing links
            Triple[] outgoingTriples = tripleStore.getTriplesWithSubject(personUri);
            for (Triple triple : outgoingTriples) {
                String predicate = triple.getPredicate();
                String object = triple.getObject();
                if (predicate.equals(RDF.type.getURI()))
                    object = COMETE.Organization.getURI();
                else if (predicate.equals(FOAF.img.getURI()))
                    predicate = FOAF.logo.getURI();
                else if (predicate.equals(COMETE.altImg.getURI()))
                    predicate = COMETE.altLogo.getURI();
                newTriples.add(new Triple(orgUri, predicate, object, false));
            }

            //redirections for incoming links
            Triple[] incomingTriples = tripleStore.getTriplesWithObject(personUri, false, null);
            for (Triple incomingTriple : incomingTriples)
                newTriples.add(new Triple(incomingTriple.getSubject(), incomingTriple.getPredicate(), orgUri, false));

            //Keep reference of cast previous person uri
            newTriples.add(new Triple(orgUri, DCTERMS.replaces, personUri));
            //and set all previous merged replace references
            Triple[] _triples = tripleStore.getTriplesWithSubjectPredicate(personUri, DCTERMS.replaces);
            for (Triple triple : _triples)
                newTriples.add(new Triple(orgUri, DCTERMS.replaces, triple.getObject()));

            //delete of triples associated with person uri (incoming, outgoing triples and in similarity graph)
            triplesToDelete.addAll(Arrays.asList(outgoingTriples));
            triplesToDelete.addAll(Arrays.asList(incomingTriples));
            triplesToDeleteInSimilarityGraph.addAll(Arrays.asList(tripleStore.getTriplesWithSubject(personUri, Identity.IDENTITY_SIMILARITY_GRAPH)));
        }

        //similar groups management 1/2
        //retrieve all possible gids related to persons uris
        ArrayList<String> groupIds = new ArrayList<String>();
        String constraints = CoreUtil.buildFilterConstraints(personUris, "s", true, "=", "||");
        //todo sparql dans le graph Identity.IDENTITY_SIMILARITY_GRAPH
        String query = CoreUtil.getQuery("identity/getInvolvedSimilarGroups.sparql", constraints);
        Tuple[] results = tripleStore.sparqlSelect(query);
        for (int i = 0; i < results.length; i++)
            groupIds.add(CoreUtil.manageQuotes(results[i].getValue("gid").getContent()));

        //delete triples
        tripleStore.removeTriples(triplesToDelete);
        tripleStore.removeTriples(triplesToDeleteInSimilarityGraph, Identity.IDENTITY_SIMILARITY_GRAPH);

        //add new triples
        tripleStore.insertTriples(newTriples);

        //similar groups management 2/2
        for (String gid : groupIds)
            cleanSimilarityGroup(gid);
    }

    void cleanSimilarityGroup(String similarGroup) throws Exception {
        //todo sparql multi-graph
        String query = CoreUtil.getQuery("identity/getIdentitiesOfSimilarGroup.sparql", similarGroup);
        Tuple[] results = tripleStore.sparqlSelect(query);
        //if one remaining uri in group, delete the group
        if (results.length == 1) {
            tripleStore.removeTriple(new Triple(results[0].getValue("s").getContent(), DCTERMS.identifier, similarGroup),
                    Identity.IDENTITY_SIMILARITY_GRAPH);
        }
    }
}
