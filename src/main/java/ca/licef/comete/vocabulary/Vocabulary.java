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
        return CoreUtil.getRestUrl(SKOS.ConceptScheme) + "/" + URLEncoder.encode(uri);
    }

    public String getConcept(String source, String concept) throws Exception {
        return getConcept(source, null, concept);
    }

    public String getConcept(String source, String cat, String concept) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll( "/", "%2F" );
        String uri = null;
        String vocUri = null;
        if (cat == null)
            vocUri = getVocabularyUri(source);
        else {
            source = getRealVocabularySource(source);
            if (source != null)
                vocUri = Core.getInstance().getUriPrefix() + "/voc/" + source.toLowerCase() + "/" + cat;
        }

        //Just lonely case where scheme is not prefix of concepts
        //Should be generalized when other cases
        boolean isDeweyInfo = "http://dewey.info/scheme/ddc/".equals(vocUri);

        if (vocUri != null && Util.isGraphExists(vocUri)) {
            String vocConceptPrefix = vocUri;

            if (isDeweyInfo)
                vocConceptPrefix = "http://dewey.info/class";

            uri = vocConceptPrefix + "/" + concept; //'/' uri case

            if (isDeweyInfo)
                uri += "/";

            if (!tripleStore.isResourceExists(uri, vocUri)) {
                uri = vocConceptPrefix + "#" + concept; // or '#' uri
                if (!tripleStore.isResourceExists(uri, vocUri))
                    uri = null;
            }
        }
        return uri;
    }

    public String getRealVocabularySource(String source) throws Exception {
        String realSource = null; //because source may be the alias
        String query = CoreUtil.getQuery("vocabulary/getVocSource.sparql", source);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            realSource = tuples[0].getValue("vocSource").getContent();
        return realSource;
    }

    public String getVocabularyUri(String source) throws Exception {
        String uri = null;
        String query = CoreUtil.getQuery("vocabulary/getVocUri.sparql", source);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            uri = tuples[0].getValue("vocUri").getContent();
        if (uri == null)
            uri = source;
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

        Invoker inv = new Invoker(null, "ca.licef.comete.core.util.Util",
                "getResourceLabel", new Object[]{uri, lang, true});
        String[] label = (String[])tripleStore.transactionalCall(inv);
        if (label == null || label[ 0 ] == null || "".equals(label[ 0 ]))
            label = new String[] { uri, null } ;

        return label[0];
    }

    public String getConceptScheme(String uri) throws Exception{
        //Just lonely case where scheme is not prefix of concepts
        //Should be generalized when other cases with Sparql -AM
        if (uri.startsWith("http://dewey.info/class/"))
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
