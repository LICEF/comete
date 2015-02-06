package ca.licef.comete.vocabulary;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.util.Util;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.RDFS;
import licef.tsapi.vocabulary.SKOS;
import org.json.JSONArray;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class Vocabulary {

    private static Vocabulary instance;
    private VocabularyManager vocabularyManager;
    static ca.licef.comete.core.util.Util CoreUtil;
    TripleStore tripleStore;

    public static Vocabulary getInstance() {
        if (instance == null) {
            instance = new Vocabulary();
        }
        return (instance);
    }

    public VocabularyManager getVocabularyManager() {
        if (vocabularyManager == null)
            vocabularyManager = new VocabularyManager();
        return vocabularyManager;
    }

    public void initVocabularyModule() throws Exception {
        tripleStore = Core.getInstance().getTripleStore();
        getVocabularyManager().initVocabularyModule();
    }

    /*********************/
    /* concepts, schemes */
    /*********************/

    public String getRestUrl(String uri) throws Exception{
        return CoreUtil.getRestUrl(SKOS.ConceptScheme) + "/" + URLEncoder.encode(uri);
    }

    public String getConcept(String id, String concept) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll( "/", "%2F" );
        String uri = null;
        String vocUri = getVocabularyUri(id);

        if (vocUri != null && Util.isGraphExists(vocUri)) {
            String vocCtxt = tripleStore.getTriplesWithPredicateObject(
                    COMETE.vocUri, vocUri, null)[0].getSubject();

            Triple[] pref = tripleStore.getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriPrefix);
            Triple[] suf = tripleStore.getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriSuffix);

            if (pref.length != 0)
                uri = pref[0].getObject() + concept;
            else {
                uri = vocUri + "/" + concept; //'/' uri case
            }
            if (suf.length != 0)
                uri += suf[0].getObject();

            if (!tripleStore.isResourceExists(uri, vocUri)) {
                uri = vocUri + "#" + concept; // or '#' uri
                if (suf.length != 0)
                    uri += suf[0].getObject();
                if (!tripleStore.isResourceExists(uri, vocUri))
                    uri = null;
            }
        }
        return uri;
    }

    public String getVocabularyUri(String id) throws Exception {
        String uri = null;
        String query = CoreUtil.getQuery("vocabulary/getVocUri.sparql", id);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            uri = tuples[0].getValue("vocUri").getContent();
        return uri;
    }

    public String getConceptLinkingPredicate (String id) throws Exception {
        String predicate = null;
        String query = CoreUtil.getQuery("vocabulary/getVocConceptLinkingPredicate.sparql", id);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            predicate = tuples[0].getValue("p").getContent();
        return predicate;
    }

    public String[] getNavigableVocabularies() throws Exception{
        String query = CoreUtil.getQuery("vocabulary/getNavigableVocabularies.sparql");
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] tuples = (Tuple[])tripleStore.transactionalCall(inv);
        String[] res = new String[tuples.length];
        for (int i = 0; i < tuples.length; i++)
            res[i] = tuples[i].getValue("vocUri").getContent();
        return res;
    }

    public String getVocabularyTitle(String uri, String lang, boolean forceConceptScheme) throws Exception {
        if (!forceConceptScheme) //check and/or retrieve scheme uri first
            uri = getConceptScheme(uri);

        Invoker inv = new Invoker(null, "ca.licef.comete.core.util.Util",
                "getResourceLabel", new Object[]{uri, lang, Boolean.TRUE});
        String[] label = (String[])tripleStore.transactionalCall(inv);
        if (label == null || label[ 0 ] == null || "".equals(label[ 0 ]))
            label = new String[] { uri, null } ;

        return label[0];
    }

    public String getConceptScheme(String uri) throws Exception{
        if (Util.isGraphExists(uri)) //uri is a skos conceptScheme)
            return uri;

        //Existent prefix case
        String query = CoreUtil.getQuery("vocabulary/getVocContextsDetails.sparql");
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        for (Tuple tuple : tuples) {
            String prefix = tuple.getValue("prefix").getContent();
            if (uri.startsWith(prefix))
                return tuple.getValue("vocUri").getContent();
        }

        String vocUri;
        if (uri.contains("#")) //hash uri case, skos concept
            vocUri = uri.substring(0, uri.lastIndexOf('#'));
        else
            vocUri = uri.substring(0, uri.lastIndexOf('/'));

        return vocUri;
    }

    /************/
    /* Contexts */
    /************/

    public Tuple[] getVocContexts() throws Exception {
        String query = CoreUtil.getQuery("vocabulary/getVocContexts.sparql");
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        return (Tuple[])tripleStore.transactionalCall(inv);
    }

    public boolean isVocabularyUsed(String uri) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "isVocabularyUsed", new Object[]{uri});
        return (boolean)tripleStore.transactionalCall(inv);
    }

    public String addNewVocContext(String name, String source, String cat, boolean navigable,
                                   String url, String fileName, InputStream uploadedInputStream) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "addNewVocContext", new Object[]{name, source, cat, navigable,
                                              url, fileName, uploadedInputStream});
        return (String)tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void updateVocContext(String uri) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "updateVocContext", new Object[]{uri});
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public String modifyVocabularyContent(String uri, String fileName, InputStream uploadedInputStream) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "modifyVocabularyContent", new Object[]{uri, fileName, uploadedInputStream});
        return (String)tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public boolean deleteVocContext(String uri) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "deleteVocContext", new Object[]{uri});
        return (boolean)tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public Tuple[] getVocContextDetails(String uri) throws Exception{
        String query = CoreUtil.getQuery("vocabulary/getVocContextDetails.sparql", uri);
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        return (Tuple[])tripleStore.transactionalCall(inv);
    }

    public Tuple[] getVocContextAliases(String uri) throws Exception {
        String query = CoreUtil.getQuery("vocabulary/getVocAliases.sparql", uri);
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        return (Tuple[])tripleStore.transactionalCall(inv);
    }

    public void addVocContextAlias(String uri, String alias) throws Exception {
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "insertTriple",
                new Object[]{new Triple(uri, COMETE.vocAlias, alias), new String[]{}});
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void updateVocContextAlias(String uri, String prevAlias, String alias) throws Exception {
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "updateObjectTriple",
                new Object[]{uri, COMETE.vocAlias, prevAlias, alias, new String[]{}});
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void deleteVocContextAlias(String uri, String alias) throws Exception {
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "removeTriple",
                new Object[]{new Triple(uri, COMETE.vocAlias, alias), new String[]{}});
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void updateVocContextNavigable(String uri, boolean value) throws Exception {
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "updateObjectTriple",
                new Object[]{uri, COMETE.vocNavigable, Boolean.toString(!value), Boolean.toString(value), new String[]{}});
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);
    }


}
