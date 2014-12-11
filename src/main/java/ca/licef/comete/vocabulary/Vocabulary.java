package ca.licef.comete.vocabulary;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.vocabulary.util.Util;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.RDFS;
import licef.tsapi.vocabulary.SKOS;
import org.json.JSONArray;

import javax.xml.transform.stream.StreamSource;
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

    public String getRestUrl(String uri) throws Exception{
        return CoreUtil.getRestUrl(SKOS.ConceptScheme.getURI()) + "/" + URLEncoder.encode(uri);
    }

    public String getVocabularyUri(String source) throws Exception {
        String uri = null;
        String query = CoreUtil.getQuery("vocabulary/getVocUri.sparql", source);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            uri = tuples[0].getValue("vocUri").getContent();
        return uri;
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
        Invoker inv = new Invoker(this, "ca.licef.comete.vocabulary.Vocabulary",
                "getBestTitle", new Object[]{uri, lang});
        return (String)tripleStore.transactionalCall(inv);
    }

    public String getBestTitle(String uri, String lang) throws Exception {
        String[] label = tripleStore.getBestLocalizedLiteralObject(uri, RDFS.label, lang, uri);
        if (label == null || label[ 0 ] == null || "".equals(label[ 0 ]))
            label = new String[] { uri, null } ;
        return label[0];
    }

    public String getConceptScheme(String uri) throws Exception{
        if (uri.startsWith("http://dewey.info/class/")) //should be generalized with sparql query -AM
            return "http://dewey.info/scheme/ddc/";

        String vocUri;
        if (uri.contains("#")) //hash uri case, skos concept
            vocUri = uri.substring(0, uri.lastIndexOf('#'));
        else {
            if (Util.isGraphExists(uri)) //uri is a skos conceptScheme)
                vocUri = uri;
            else //2nd pass with end truncation
                vocUri = uri.substring(0, uri.lastIndexOf('/'));
        }
        return vocUri;
    }

}
