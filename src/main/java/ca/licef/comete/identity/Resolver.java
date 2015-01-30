package ca.licef.comete.identity;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.vocabulary.RDF;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import licef.tsapi.vocabulary.ORG;
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

    public void updateIdentityValues(String uri, JSONObject mainValues, OntClass type) throws Exception {
        updateIdentityValues(uri, new JSONArray().put(uri), mainValues, type);
    }

    public void updateIdentityValues(String uri, JSONArray uriArray, JSONObject mainValues, OntClass type) throws Exception {
        ArrayList<Triple> newTriples = new ArrayList<Triple>();
        ArrayList<String> usedKeys = new ArrayList<String>();

        //retrieve all values for the target_uri (main and alternates)
        JSONObject details = COMETE.Person.equals(type)?
                Identity.getInstance().getAllPersonDetails(uriArray):
                Identity.getInstance().getAllOrganizationDetails(uriArray);

        //loop for populate uri with main values as main and others as alternates
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

        //remaining main values typed into editor dialog
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

        //reset uri values
        String query = COMETE.Person.equals(type)?
                CoreUtil.getQuery("identity/getPersonTriplesToReset.sparql", uri):
                CoreUtil.getQuery("identity/getOrganizationTriplesToReset.sparql", uri);
        Triple[] previousTriples = tripleStore.getTriplesInSparql(query);
        tripleStore.removeTriples_textIndex(Arrays.asList(previousTriples));

        //add new triples
        tripleStore.insertTriples_textIndex(newTriples);
    }


    /**
     * Merges multiple identities by cumulate differents values of sources into target uri as main or alternatives values.
     *
     * @param uriArray list of uris to merge together
     * @param mainValues main values
     * @param type identity type
     * @throws Exception
     */
    public String mergeIdentities(JSONArray uriArray, JSONObject mainValues, OntClass type) throws Exception {
        ArrayList<String> uris = new ArrayList<>();
        ArrayList<String> groupIds = new ArrayList<>();

        //First uri will be the target one
        String uri = uriArray.getString(0);

        for (int i = 0; i < uriArray.length(); i++)
            uris.add(uriArray.getString(i));

        //retrieve all possible gids related to next merged uris
        String constraints = CoreUtil.buildFilterConstraints(uris, "s", true, "=", "||");
        String query = CoreUtil.getQuery("identity/getInvolvedSimilarGroups.sparql" ,
                constraints, tripleStore.getUri(Identity.IDENTITY_SIMILARITY_GRAPH));
        Tuple[] results = tripleStore.sparqlSelect(query);
        for (int i = 0; i < results.length; i++)
            groupIds.add(results[i].getValue("gid").getContent());
        System.out.println("groupIds = " + groupIds);
        //values management
        updateIdentityValues(uri, uriArray, mainValues, type);

        //substitution for merged uris
        for (int i = 0; i < uriArray.length(); i++) {
            String mergedUri = uriArray.getString(i);
            System.out.println("mergedUri = " + mergedUri);
            //ignore target uri
            if (mergedUri.equals(uri))
                continue;
            System.out.println("substitution= ");

            //merged uri becomes uri and delete in similarity graph
            tripleStore.substituteResourceUri_textIndex(mergedUri, uri);
            tripleStore.removeResource(mergedUri, tripleStore.getUri(Identity.IDENTITY_SIMILARITY_GRAPH));
        }

        //possibly cleaning groups
        for (String gId : groupIds)
            cleanSimilarityGroup(gId);

        //re expose related records of uri (mainly for merged uris).
        //Metadata.getInstance().exposeRecordsOfIdentity(uri);

        return uri;
    }

    public void takeOffIdentities(JSONArray uriArray, String gId) throws Exception {
        ArrayList<Triple> triplesToDelete = new ArrayList<>();
        for (int i = 0; i < uriArray.length(); i++) {
            String uri = uriArray.getString(i);
            triplesToDelete.add(new Triple(uri, DCTERMS.identifier, gId));
        }
        tripleStore.removeTriples(triplesToDelete, Identity.IDENTITY_SIMILARITY_GRAPH);
        cleanSimilarityGroup(gId);
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
            String id = CoreUtil.getIdValue(personUri);
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
        String query = CoreUtil.getQuery("identity/getInvolvedSimilarGroups.sparql",
                constraints, tripleStore.getUri(Identity.IDENTITY_SIMILARITY_GRAPH));
        Tuple[] results = tripleStore.sparqlSelect(query);
        for (int i = 0; i < results.length; i++)
            groupIds.add(results[i].getValue("gid").getContent());

        //delete triples
        tripleStore.removeTriples(triplesToDelete);
        tripleStore.removeTriples(triplesToDeleteInSimilarityGraph, Identity.IDENTITY_SIMILARITY_GRAPH);

        //add new triples
        tripleStore.insertTriples(newTriples);

        //similar groups management 2/2
        for (String gid : groupIds)
            cleanSimilarityGroup(gid);
    }

    void cleanSimilarityGroup(String gId) throws Exception {
        Triple[] results = tripleStore.getTriplesWithPredicateObject(DCTERMS.identifier,
                gId, null, Identity.IDENTITY_SIMILARITY_GRAPH);
        //if one remaining uri in group, delete last trace of group
        if (results.length == 1)
            tripleStore.removeTriple(results[0], Identity.IDENTITY_SIMILARITY_GRAPH);
    }
}
