package ca.licef.comete.vocabulary;

import ca.licef.comete.core.Core;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.util.Util;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.SKOS;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
        getVocabularyManager().initVocabularyModule();
    }

    public TripleStore getTripleStore() {
        if (tripleStore == null)
            tripleStore = Core.getInstance().getTripleStore();
        return tripleStore;
    }

    /*********************/
    /* concepts, schemes */
    /*********************/

    public String getRestUrl(String uri) throws Exception{
        return CoreUtil.getRestUrl(SKOS.ConceptScheme) + "/" + URLEncoder.encode(uri);
    }

    public String getConceptOld(String id, String concept) throws Exception {
        concept = concept.replaceAll(" ", "%20");
        concept = concept.replaceAll( "/", "%2F" );
        String uri = null;
        String vocUri = getVocabularyUri(id);

        if (vocUri != null && Util.isGraphExists(vocUri)) {
            String vocCtxt = getTripleStore().getTriplesWithPredicateObject(
                    COMETE.vocUri, vocUri, null)[0].getSubject();

            Triple[] pref = getTripleStore().getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriPrefix);
            Triple[] suf = getTripleStore().getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriSuffix);

            if (pref.length != 0)
                uri = pref[0].getObject() + concept;
            else {
                uri = vocUri + "/" + concept; //'/' uri case
            }
            if (suf.length != 0)
                uri += suf[0].getObject();

            if (!getTripleStore().isResourceExists(uri, vocUri)) {
                uri = vocUri + "#" + concept; // or '#' uri
                if (suf.length != 0)
                    uri += suf[0].getObject();
                if (!getTripleStore().isResourceExists(uri, vocUri))
                    uri = null;
            }
        }
        return uri;
    }

    public String getConcept(String id, String concept) throws Exception {
        String uri = null;
        String vocUri = getVocabularyUri(id);

        if (vocUri != null && Util.isGraphExists(vocUri)) {
            //check if concept is already an URI
            if (concept.contains(":")) {
                String scheme = concept.substring(0, concept.indexOf(":"));
                if (scheme.matches("[a-zA-Z][a-zA-Z0-9\\+\\-\\.]*"))
                    uri = concept;
            }

            if (uri == null) {
                concept = concept.replaceAll(" ", "%20");
                concept = concept.replaceAll( "/", "%2F" );

                String vocCtxt = getTripleStore().getTriplesWithPredicateObject(
                        COMETE.vocUri, vocUri, null)[0].getSubject();

                Triple[] pref = getTripleStore().getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriPrefix);
                Triple[] suf = getTripleStore().getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriSuffix);

                if (pref.length != 0)
                    uri = pref[0].getObject() + concept;
                else {
                    String separator = "";
                    if ( !vocUri.endsWith("/") && !vocUri.endsWith("#") )
                        separator = getVocConceptUriIdSeparator(vocCtxt);
                    uri = vocUri + separator + concept;
                }

                if (suf.length != 0)
                    uri += suf[0].getObject();
            }
        }
        return uri;
    }

    public String getVocabularyUri(String id) throws Exception {
        String uri = null;
        String query = CoreUtil.getQuery("vocabulary/getVocUri.sparql", ( id.startsWith( "http://" ) ? id : "http://dummy" ), id);
        Tuple[] tuples = getTripleStore().sparqlSelect(query);
        if (tuples.length > 0)
            uri = tuples[0].getValue("vocUri").getContent();
        return uri;
    }

    public String getConceptLinkingPredicateFromUri(String uri) throws Exception {
        String vocUri = getConceptScheme(uri);
        String predicate = null;
        String query = CoreUtil.getQuery("vocabulary/getVocConceptLinkingPredicateFromUri.sparql", vocUri);
        Tuple[] tuples = getTripleStore().sparqlSelect(query);
        if (tuples.length > 0)
            predicate = tuples[0].getValue("p").getContent();
        return predicate;
    }

    public String[] getAllConceptLinkingPredicates() throws Exception {
        String query = CoreUtil.getQuery("vocabulary/getAllVocConceptLinkingPredicates.sparql");
        Invoker inv = new Invoker( getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[] { query } );
        Tuple[] tuples = (Tuple[])getTripleStore().transactionalCall( inv );
        String[] res = new  String[ tuples.length ];
        for( int i = 0; i < tuples.length; i++ )
            res[ i ] = tuples[ i ].getValue( "p" ).getContent();
        return( res );
    }

    public String[] getNavigableVocabularies() throws Exception{
        String query = CoreUtil.getQuery("vocabulary/getNavigableVocabularies.sparql");
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] tuples = (Tuple[])getTripleStore().transactionalCall(inv);
        String[] res = new String[tuples.length];
        for (int i = 0; i < tuples.length; i++)
            res[i] = tuples[i].getValue("vocUri").getContent();
        return res;
    }

    public String getLabel(String uri, String lang) throws Exception {
        Invoker inv = new Invoker(null, "ca.licef.comete.core.util.Util",
                "getResourceLabel", new Object[]{uri, lang, Boolean.TRUE});
        String[] label = (String[])getTripleStore().transactionalCall(inv);
        if (label == null || label[ 0 ] == null || "".equals(label[ 0 ]))
            label = new String[] { uri, null } ;
        return label[0];
    }

    public String getConceptScheme(String uri) throws Exception{
        Invoker inv = new Invoker(this, "ca.licef.comete.vocabulary.Vocabulary",
                "getConceptSchemeEff", new Object[]{uri});
        return (String)getTripleStore().transactionalCall(inv);
    }

    public String getConceptSchemeEff(String uri) throws Exception{
        if (uri == null)
            return null;

        if (Util.isGraphExists(uri)) //uri is a skos conceptScheme)
            return uri;

        String fromClause = "";
        Tuple[] ctxts = Vocabulary.getInstance().getVocContexts();
        for (Tuple ctxt : ctxts)
            fromClause += "FROM <" + ctxt.getValue("vocUri").getContent() + ">\n";

        String query = CoreUtil.getQuery("vocabulary/getVocUriOfConcept.sparql",
                fromClause, uri);
        Tuple[] tuples = getTripleStore().sparqlSelect(query);
        if (tuples.length > 0)
            return tuples[0].getValue("vocUri").getContent();

        String vocUri = null;
        if (uri.contains("#")) //hash uri case, skos concept
            vocUri = uri.substring(0, uri.lastIndexOf('#'));
        else if (uri.contains("/"))
            vocUri = uri.substring(0, uri.lastIndexOf('/'));
        if (Util.isGraphExists(vocUri))
            return vocUri;
        else
            return null;
    }

    public Object[] getTopConcepts(String uri) throws Exception {
        List<String> topConceptUris = new ArrayList<>();
        String query = CoreUtil.getQuery("vocabulary/getTopConcepts.sparql", uri);
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] res = (Tuple[])getTripleStore().transactionalCall(inv);
        int nbPos = 0;
        for (Tuple tuple : res) {
            topConceptUris.add(tuple.getValue("s").getContent());
            if (!"".equals(tuple.getValue("pos").getContent()))
                nbPos++;
        }
        boolean controlledPosition = (nbPos == topConceptUris.size());
        return new Object[]{controlledPosition, topConceptUris.toArray(new String[topConceptUris.size()])};
    }

    public Object[] getChildren(String uri) throws Exception {
        List<String> childrenUris = new ArrayList<>();
        String query = CoreUtil.getQuery("vocabulary/getChildren.sparql", uri, getConceptScheme(uri));
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] res = (Tuple[])getTripleStore().transactionalCall(inv);
        int nbPos = 0;
        for (Tuple tuple : res) {
            childrenUris.add(tuple.getValue("o").getContent());
            if (!"".equals(tuple.getValue("pos").getContent()))
                nbPos++;
        }
        boolean controlledPosition = (nbPos == childrenUris.size());
        return new Object[]{controlledPosition, childrenUris.toArray(new String[childrenUris.size()])};
    }

    public Tuple[] getHierarchy(String uri) throws Exception {
        String graph = getConceptScheme(uri);
        String query = CoreUtil.getQuery("vocabulary/getConceptHierarchy.sparql", uri, graph);
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        return (Tuple[])getTripleStore().transactionalCall(inv);
    }

    public Tuple[] searchConcepts(String terms, String lang) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.vocabulary.Vocabulary",
                "searchConceptsEff", new Object[]{terms, lang});
        return (Tuple[])getTripleStore().transactionalCall(inv);
    }

    public Tuple[] searchConceptsEff(String terms, String lang) throws Exception {
        String[] nav = getNavigableVocabularies();
        String fromGraphs = "";
        for (String vocUri : nav)
            fromGraphs += "FROM <" + vocUri + "> \n";
        String query = CoreUtil.getQuery("vocabulary/getConcepts.sparql",
                CoreUtil.formatKeywords(terms), lang, fromGraphs);
        return getTripleStore().sparqlSelect_textIndex(query);
    }

    public String getVocConceptUriIdSeparator(String vocCtxt) throws Exception {
        Triple[] sep = getTripleStore().getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocConceptUriIdSeparator);
        if (sep.length > 0)
            return sep[0].getObject();

        String vocUri = getTripleStore().getTriplesWithSubjectPredicate(vocCtxt, COMETE.vocUri)[0].getObject();
        Triple[] concepts = getTripleStore().getTriplesWithPredicateObject(SKOS.inScheme, vocUri, null, vocUri);
        String aConceptUri = concepts[0].getSubject();
        String separator = "/";
        if (aConceptUri.contains("#"))
            separator = "#";
        getTripleStore().insertTriple(new Triple(vocCtxt, COMETE.vocConceptUriIdSeparator, separator));

        return separator;
    }

    public Tuple[] getOrphanConcepts() throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.vocabulary.Vocabulary",
                "getOrphanConceptsEff", new Object[]{});
        return (Tuple[])getTripleStore().transactionalCall(inv);
    }

    public Tuple[] getOrphanConceptsEff() throws Exception {
        String fromClause = "FROM <urn:x-arq:DefaultGraph>\n ";
        Tuple[] ctxts = Vocabulary.getInstance().getVocContexts();
        for (Tuple ctxt : ctxts)
            fromClause += "FROM <" + ctxt.getValue("vocUri").getContent() + ">\n";
        String query = CoreUtil.getQuery("vocabulary/getOrphanConcepts.sparql", fromClause);
        return getTripleStore().sparqlSelect_textIndex(query);
    }

    /************/
    /* Contexts */
    /************/

    public Tuple[] getVocContexts() throws Exception {
        String query = CoreUtil.getQuery("vocabulary/getVocContexts.sparql");
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        return (Tuple[])getTripleStore().transactionalCall(inv);
    }

    public boolean isVocabularyUsed(String uri) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "isVocabularyUsed", new Object[]{uri});
        return (boolean)getTripleStore().transactionalCall(inv);
    }

    public String addNewVocContext(String id, String uriPrefix, String uriSuffix, String linkingPredicate,
                                   String url, String fileName, InputStream uploadedInputStream) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "addNewVocContext", new Object[]{id, uriPrefix, uriSuffix, linkingPredicate,
                                              url, fileName, uploadedInputStream});
        return (String)getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public String modifyVocabularyContent(String id, String uriPrefix, String uriSuffix, String linkingPredicate,
                                          String url, String fileName, InputStream uploadedInputStream) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "modifyVocContext", new Object[]{id, uriPrefix, uriSuffix, linkingPredicate,
                                                     url, fileName, uploadedInputStream});
        return (String)getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public boolean deleteVocContext(String uri) throws Exception {
        Invoker inv = new Invoker(getVocabularyManager(), "ca.licef.comete.vocabulary.VocabularyManager",
                "deleteVocContext", new Object[]{uri, false});
        return (boolean)getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public Tuple[] getVocContextDetails(String uri) throws Exception{
        String query = CoreUtil.getQuery("vocabulary/getVocContextDetails.sparql", uri);
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        return (Tuple[])getTripleStore().transactionalCall(inv);
    }

    public Triple[] getVocContextAliases(String uri) throws Exception {
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore",
                "getTriplesWithSubjectPredicate", new Object[]{
                    uri, COMETE.vocAlias, new String[]{} } );
        return (Triple[])getTripleStore().transactionalCall(inv);
    }

    public void addVocContextAlias(String uri, String alias) throws Exception {
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "insertTriple",
                new Object[]{new Triple(uri, COMETE.vocAlias, alias), new String[]{}});
        getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void updateVocContextAlias(String uri, String prevAlias, String alias) throws Exception {
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "updateObjectTriple",
                new Object[]{uri, COMETE.vocAlias, prevAlias, alias, new String[]{}});
        getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void deleteVocContextAlias(String uri, String alias) throws Exception {
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "removeTriple",
                new Object[]{new Triple(uri, COMETE.vocAlias, alias), new String[]{}});
        getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public void updateVocContextNavigable(String uri, boolean value) throws Exception {
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "updateObjectTriple",
                new Object[]{uri, COMETE.vocNavigable, Boolean.toString(!value), Boolean.toString(value), new String[]{}});
        getTripleStore().transactionalCall(inv, TripleStore.WRITE_MODE);
    }

    public Boolean isVocNavigable(String uri) throws Exception {
        String query = CoreUtil.getQuery("vocabulary/isVocNavigable.sparql", uri);
        Invoker inv = new Invoker(getTripleStore(), "licef.tsapi.TripleStore", "sparqlSelect", new Object[]{query});
        Tuple[] tuples = (Tuple[])getTripleStore().transactionalCall(inv);
        if (tuples.length > 0)
            return Boolean.parseBoolean(tuples[0].getValue("navigable").getContent());
        else
            return Boolean.FALSE;
    }

}
