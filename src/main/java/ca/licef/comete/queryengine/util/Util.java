package ca.licef.comete.queryengine.util;

import ca.licef.comete.core.Core;
import ca.licef.comete.queryengine.QueryCache;
import ca.licef.comete.queryengine.QueryEngine;
import licef.DateUtil;
import licef.tsapi.TripleStore;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-01-16
 */
public class Util {

    public static final String COLLECTION = "collection";

    public static final String FULLTEXT = "fulltext";
    public static final String LANGUAGE = "language";
    public static final String TITLE_PREFIX = "title";
    public static final String DESCRIPTION_PREFIX = "description";
    public static final String KEYWORD_PREFIX = "keyword";
    public static final String CONTRIBUTE_PREFIX = "contrib";
    public static final String NOT_CONTRIBUTE_PREFIX = "!contrib";
    public static final String ORGANIZATION_PREFIX = "org";
    public static final String NOT_ORGANIZATION_PREFIX = "!org";
    public static final String CONCEPT_PREFIX = "vocConcept";
    public static final String NOT_CONCEPT_PREFIX = "!vocConcept";
    public static final String ADDED_DATE = "addedDate";
    public static final String FROM_HARVESTED_REPO = "fromHarvestedRepo";

    public static final String REL_OP_GTE = "gte";
    public static final String REL_OP_GT = "gt";
    public static final String REL_OP_LTE = "lte";
    public static final String REL_OP_LT = "lt";
    public static final String REL_OP_EQ = "eq";

    public static HashMap<String,String> relOpTable;

    static ca.licef.comete.core.util.Util CoreUtil;

    public static String webappPath;

    public static void setWebappPath(String path) {
        webappPath = path;
    }

    /**
     * @return clause element and pre compute size
     */
    public static Object[] buildQueryElements(JSONArray queryArray, String lg, boolean isWithScore, QueryCache cache) throws Exception {
        String clauses = "";
        String negationClauses = null;

        boolean waitForOperator = false;

        String orClauses = "";
        String firstClause = null;

        boolean isFromHarvestedRepoClause = false;

        //if collection, retrieve of right query first
        JSONObject firstCond = (JSONObject)queryArray.get(0);
        if (COLLECTION.equals(firstCond.get("key"))) {
            String id = (String)firstCond.get("value");
            int index = Integer.valueOf(id.substring(id.indexOf("_") + 1));
            String query = QueryEngine.getInstance().getCollection().getQuery(index, lg);
            queryArray = new JSONArray(query);
        }

        for (int i = 0; i < queryArray.length(); i++) {
            JSONObject obj = queryArray.getJSONObject(i);
            if (waitForOperator) {
                String op = obj.getString("op");
                if ("AND".equals(op)) {
                    clauses += orClauses;
                    orClauses = "";
                    firstClause = null;
                }
                waitForOperator = !waitForOperator;
            }
            else {
                String condType = obj.getString("key");
                String clause = null;
                String negationClause = null;
                if (FULLTEXT.equals(condType)) {
                    String text = obj.getString("value");
                    if( !text.trim().equals( "" ) )
                        clause = makeFulltextClause(CoreUtil.formatKeywords(text), lg);
                }
                else if (LANGUAGE.equals(condType)) {
                    String lang = obj.getString("value");
                    clause = makeLanguageClause(lang, isWithScore);
                }
                else if (TITLE_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    String lang = obj.getString("lang");
                    clause = makeTitleClause(CoreUtil.formatKeywords(text), lang, isWithScore);
                }
                else if (DESCRIPTION_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    String lang = obj.getString("lang");
                    clause = makeDescriptionClause(CoreUtil.formatKeywords(text), lang, isWithScore);
                }
                else if (KEYWORD_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    String lang = obj.getString("lang");
                    clause = makeKeywordClause(CoreUtil.formatKeywords(text), lang, isWithScore);
                }
                else if (CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    clause = makeContribClause(uri, isWithScore);
                }
                else if (CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    clause = makeContribClause(uri, isWithScore);
                }
                else if (NOT_CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    negationClause = makeContribClause(uri, isWithScore);
                }
                else if (ORGANIZATION_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    clause = makeOrgClause(uri, isWithScore);
                }
                else if (NOT_ORGANIZATION_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    negationClause = makeOrgClause(uri, isWithScore);
                }
                else if (condType.startsWith(CONCEPT_PREFIX)) {
                    /*String uri = obj.getString("value");
                    boolean isSubConcept = obj.has("subConcepts") && obj.getBoolean("subConcepts");
                    if (isSubConcept)
                        clause = makeVocConceptHierarchyClause(TripleStoreService.VOC_GLOBAL_VIEW, uri, isWithScore);
                    else
                        clause = makeVocConceptClause(uri, isWithScore);

                    // equivalent external concepts used only for thematic navigation (for the moment)
                    // So just one clause and little hack for all clauses generation behavior
                    // add recursively in this test -AM
                    if (obj.has("equivalent") && obj.getBoolean("equivalent")) {
                        JSONArray fromVocs = (obj.has("fromVocs"))?obj.getJSONArray("fromVocs"):null;
                        Date d1 = new Date();
                        Date d2;
                        JSONArray equivalents = cache.getCachedEquivalentConcepts(uri, isSubConcept, fromVocs);
                        if (equivalents == null) {
                            equivalents = getEquivalentConcepts(uri, isSubConcept, fromVocs);
                            cache.cacheEquivalentConcepts(uri, isSubConcept, fromVocs, equivalents);
                            d2 = new Date();
                        }
                        else
                            d2 = new Date();
                        long l = d2.getTime() - d1.getTime();
                        System.out.println("equivalent concepts : " + equivalents.length());
                        System.out.println("found in (ms) = " + l);
                        for (int k = 0; k < equivalents.length(); k++) {
                            String eqUri = equivalents.getString(k);
                            if (k == 0 && !isSubConcept) //braces for first clause
                                clause = "\n{ " + clause + " }";
                            clause += "\nUNION\n";
                            if (isSubConcept)
                                clause += makeVocConceptHierarchyClause(TripleStoreService.VOC_GLOBAL_VIEW, eqUri, isWithScore);
                            else
                                clause += "{ " + makeVocConceptClause(eqUri, isWithScore) + " }";
                        }
                    }*/
                }
                else if (condType.startsWith(NOT_CONCEPT_PREFIX)) {
                    /*String uri = obj.getString("value");
                    if (obj.has("subConcepts") && obj.getBoolean("subConcepts"))
                        negationClause = makeVocConceptHierarchyClause(TripleStoreService.VOC_GLOBAL_VIEW, uri, isWithScore);
                    else
                        negationClause = makeVocConceptClause(uri, isWithScore);*/
                }
                else if (ADDED_DATE.equals(condType)) {
                    String relOp = obj.getString( "relOp" );
                    String date = obj.getString( "value" );
                    clause = makeAddedDateClause( relOp, date, isWithScore );
                }
                else if (FROM_HARVESTED_REPO.equals(condType)) {
                    isFromHarvestedRepoClause = true;
                    String uri = obj.getString("value");
                    clause = makeFromHarvestedRepoClause(uri);
                }

                if (clause != null) {
                    if (firstClause == null)
                        firstClause = clause;
                    else {
                        if (!orClauses.contains("UNION")) //braces for first clause (previous one) -AM
                            orClauses = "\n{ " + firstClause + " }";
                        orClauses += "\nUNION";
                    }
                    String orClause = (orClauses.endsWith("UNION") && !clause.contains("UNION"))?
                        "\n{ " + clause + " }":
                        "\n" + clause;

                    orClauses += orClause;
                }
                if (negationClause != null) {
                    if (negationClauses == null)
                        negationClauses = negationClause;
                    else {
                        if (!negationClauses.contains("UNION")) //braces for first neg clause (previous one) -AM
                            negationClauses = "{ " + negationClauses + " }";
                        negationClauses += "\nUNION\n{ " + negationClause + " }";
                    }
                }

                waitForOperator = !waitForOperator;
            }
        }
        //last cond
        clauses += orClauses;

        //mini hack. clause block splitted in java to have this clause once... -AM
        if (isFromHarvestedRepoClause) {
            clauses += "\n"+(isWithScore?"?sWithScore":"?s")+" comete:hasMetadataRecord ?r .";
        }

//        licef.IOUtil.writeStringToFile(clauses, new java.io.File("e:/zzz/ti/clauses.txt"));
//        if (negationClauses != null)
//            licef.IOUtil.writeStringToFile(negationClauses, new java.io.File("e:/zzz/ti/notClauses.txt"));

        int count = 0;
        /*if (negationClauses == null) //"normal" case. i.e without negation
            count = tripleStore.getResultsCount("getLearningObjectsAdvancedQueryForCount" + (isWithScore?"WS":"") + ".sparql", clauses);
        else {
            if ("".equals(clauses)) { //only negation case. Count by difference
                int allCount = learningObjectsCount();
                int negCount = tripleStore.getResultsCount("getLearningObjectsAdvancedQueryForCount" + (isWithScore?"WS":"") + ".sparql", negationClauses);
                count = allCount - negCount;
                clauses = buildFilter(negationClauses, isWithScore);
            }
            else { //both cases. Improvement: if negations doesn't impact, they are not insert as filter.
                List uris = CoreUtil.buildList(
                        tripleStore.getResults("getLearningObjectsAdvancedQueryForCount" + (isWithScore?"WS":"") + ".sparql", clauses),
                        (isWithScore?"sWithScore":"s"));
                List negUris = CoreUtil.buildList(
                        tripleStore.getResults("getLearningObjectsAdvancedQueryForCount" + (isWithScore?"WS":"") + ".sparql", negationClauses),
                        (isWithScore?"sWithScore":"s"));
                ArrayList<String> newFilterNegUris = new ArrayList<String>();
                count = uris.size();
                for (Iterator it = uris.iterator(); it.hasNext();) {
                    String uri = (String)it.next();
                    if (negUris.contains(uri)) {
                        newFilterNegUris.add(uri);
                        count--;
                    }
                }
                if (newFilterNegUris.size() > 0)
                    clauses += "\n" + buildFilter(newFilterNegUris, isWithScore);
            }
        }*/

        return new Object[]{clauses, count};
    }

    public static String makeFulltextClause( String text, String lang ) throws Exception{
        return CoreUtil.getQuery("advancedFulltextFragmentWS.sparql", text, lang);
    }

    public static String makeLanguageClause( String lang, boolean isWithScore ) throws Exception{
        return CoreUtil.getQuery("advancedLanguageFragment" + (isWithScore?"WS":"") + ".sparql", lang);
    }

    public static String makeTitleClause( String text, String lang, boolean isWithScore ) throws Exception{
        return CoreUtil.getQuery("advancedTitleFragment" + (isWithScore?"WS":"") + ".sparql", text, lang);
    }

    public static String makeDescriptionClause( String text, String lang, boolean isWithScore ) throws Exception{
        return CoreUtil.getQuery("advancedDescriptionFragment" + (isWithScore?"WS":"") + ".sparql", text, lang);
    }

    public static String makeKeywordClause( String text, String lang, boolean isWithScore ) throws Exception{
        return CoreUtil.getQuery("advancedKeywordFragment" + (isWithScore?"WS":"") + ".sparql", text, lang);
    }

    public static String makeContribClause( String contribUri, boolean isWithScore ) throws Exception{
        return CoreUtil.getQuery("advancedContribFragment" + (isWithScore?"WS":"") + ".sparql", contribUri);
    }

    public static String makeOrgClause( String orgUri, boolean isWithScore ) throws Exception {
        return CoreUtil.getQuery("advancedOrgFragment" + (isWithScore?"WS":"") + ".sparql", orgUri);
    }

    public static String makeVocConceptClause( String conceptUri, boolean isWithScore ) throws Exception {
        return CoreUtil.getQuery("advancedVocConceptFragment" + (isWithScore?"WS":"") + ".sparql", conceptUri);
    }

    public static String makeVocConceptHierarchyClause( String graph, String conceptUri, boolean isWithScore ) throws Exception {
        return CoreUtil.getQuery("advancedVocConceptHierarchyFragment" + (isWithScore?"WS":"") + ".sparql", graph, conceptUri);
    }

    public static String makeAddedDateClause( String relOpStr, String date, boolean isWithScore ) throws Exception{
        String dateWithoutTime = date;
        int indexOfTimeDelimiter = dateWithoutTime.indexOf( "T" );
        if( indexOfTimeDelimiter != -1 ) 
            dateWithoutTime = dateWithoutTime.substring( 0, indexOfTimeDelimiter );
        String dateClause;
        if (REL_OP_GT.equals(relOpStr)) {
            dateClause = CoreUtil.getQuery("advancedAddedDateFragment" + (isWithScore?"WS":"") + ".sparql", getRelOp( relOpStr ),
                    DateUtil.nextDay(dateWithoutTime));
        }
        else if (REL_OP_EQ.equals(relOpStr)) {
            dateClause = CoreUtil.getQuery("advancedAddedDateFragment" + (isWithScore?"WS":"") + ".sparql", getRelOp( REL_OP_GTE), dateWithoutTime);
            dateClause += "\n" + CoreUtil.getQuery("advancedAddedDateFragment" + (isWithScore?"WS":"") + ".sparql", getRelOp( REL_OP_LT ),
                    DateUtil.nextDay(dateWithoutTime));
        }
        else
            dateClause = CoreUtil.getQuery("advancedAddedDateFragment" + (isWithScore?"WS":"") + ".sparql", getRelOp( relOpStr ), dateWithoutTime);
        return dateClause;
    }

    public static String makeFromHarvestedRepoClause( String repoUri ) throws Exception {
        return CoreUtil.getQuery("advancedFromHarvestedRepoFragment.sparql", repoUri);
    }


    static String buildFilter( String clause, boolean isWithScore ) throws Exception{
        /*Hashtable<String, String>[] results = tripleStore.getResults("getLearningObjectsAdvancedQueryForCount.sparql", clause);
        List<String> uris = CoreUtil.buildList(results, (isWithScore?"sWithScore":"s"));
        return buildFilter(uris, isWithScore);*/

        return null;
    }

    static String buildFilter( List<String> uris, boolean isWithScore ) throws Exception{
        if (uris.isEmpty())
            return "";
        String constraints = CoreUtil.buildFilterConstraints(uris, (isWithScore?"sWithScore":"s"), true, "!=", "&&");
        return "FILTER ( " + constraints + " )";
    }

    /*public static String getVocabularyConceptGraph(String uri) throws Exception {
        String url = CoreUtil.getRestUrl(Constants.TYPE_VOCABULARY) + "/graphName?uri=" + uri;
        WebResource webResource = Core.getInstance().getRestClient().resource(url);
        ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
        int status = response.getStatus();
        String graph = null;
        if( status == 200 )
            graph = response.getEntity(String.class);
        return graph;
    }*/

    public static JSONArray getEquivalentConcepts(String uri, boolean subConcepts, JSONArray fromVocs) throws Exception {
        /*String url = CoreUtil.getRestUrl(Constants.TYPE_VOCABULARY) + "/equivalentConcepts?uri=" + uri;
        if (subConcepts)
            url += "&includeSubConcepts=true";
        if (fromVocs != null && fromVocs.length() > 0)
            url += "&fromVocs=" + URLEncoder.encode(fromVocs.toString());
        WebResource webResource = Core.getInstance().getRestClient().resource(url);
        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        int status = response.getStatus();
        if( status == 200 )
            return new JSONArray(response.getEntity(String.class));
        else*/
            return null;
    }

    public static String[] buildTitleAndDescription(JSONArray queryArray, String outputLang, String outputFormat) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("Strings", new Locale(outputLang));

        StringBuilder title = new StringBuilder();
        StringBuilder description = new StringBuilder( bundle.getString( "rs.advancedSearch.description.prefix" ) );

        /*title.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.title" ), outputFormat.toUpperCase() ) );

        boolean waitForOperator = false;

        for (int i = 0; i < queryArray.length(); i++) {
            JSONObject obj = queryArray.getJSONObject(i);
            if (waitForOperator) {
                String op = obj.getString("op");
                if ("AND".equals(op))
                    description.append( " " ).append( bundle.getString( "rs.advancedSearch.description.and" ) ).append( " " );
                else if ("OR".equals(op))
                    description.append( " " ).append( bundle.getString( "rs.advancedSearch.description.or" ) ).append( " " );

                waitForOperator = !waitForOperator;
            }
            else {
                String condType = obj.getString("key");
                if (FULLTEXT.equals(condType)) {
                    String text = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.fulltext" ), text) );
                }
                else if (TITLE_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    String lang = obj.getString("lang");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.title" ), text, lang ) );
                }
                else if (DESCRIPTION_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    String lang = obj.getString("lang");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.description" ), text, lang ) );
                }
                else if (KEYWORD_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    String lang = obj.getString("lang");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.keyword" ), text, lang ) );
                }
                else if (CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.contributor" ), 
                        tripleStore.getResourceLabel( uri, outputLang ) ) );
                }
                else if (NOT_CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.contributor.not" ),
                        tripleStore.getResourceLabel( uri, outputLang ) ) );
                }
                else if (ORGANIZATION_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.organization" ), 
                        tripleStore.getResourceLabel( uri, outputLang ) ) );
                }
                else if (NOT_ORGANIZATION_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.organization.not" ),
                        tripleStore.getResourceLabel( uri, outputLang ) ) );
                }
                else if (condType.startsWith(CONCEPT_PREFIX)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.concept" ),
                            tripleStore.getResourceLabel( uri, outputLang, true )) );
                    if (obj.has("subConcepts") && obj.getBoolean("subConcepts"))
                        description.append( " " ).append( bundle.getString( "rs.advancedSearch.description.andSubConcepts" ) );
                }
                else if (condType.startsWith(NOT_CONCEPT_PREFIX)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.concept.not" ),
                            tripleStore.getResourceLabel( uri, outputLang, true )) );
                    if (obj.has("subConcepts") && obj.getBoolean("subConcepts"))
                        description.append( " " ).append( bundle.getString( "rs.advancedSearch.description.andSubConcepts" ) );
                }
                else if( ADDED_DATE.equals( condType ) ) {
                    String relOp = obj.getString( "relOp" );
                    String date = obj.getString( "value" );
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.addedDate" ), 
                        bundle.getString( "rs.advancedSearch.description.addedDate." + relOp ), date ) );
                }
                else if (FROM_HARVESTED_REPO.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.harvestedRepository" ),
                            tripleStore.getResourceLabel( uri, outputLang ) ) );
                }

                waitForOperator = !waitForOperator;
            }
        }

        description.append( "." );*/

        return( new String[] { title.toString(), description.toString() } );
    }

    public static String getRelOp( String relOp ) {
        if( relOpTable == null ) {
            relOpTable = new HashMap<String,String>();
            relOpTable.put( REL_OP_GTE, ">=" );
            relOpTable.put( REL_OP_GT, ">" );
            relOpTable.put( REL_OP_LTE, "<=" );
            relOpTable.put( REL_OP_LT, "<" );
            relOpTable.put( REL_OP_EQ, "=" );
        }
        return( relOpTable.get( relOp ) );
    }

    public static String getMimeTypeIcon( String mimeType ) {
        String imagesPath = "images/mimeTypeIcons/";
        String path = webappPath + "/" + imagesPath;
        String iconFileStr = mimeType.replaceAll( "/", "_" ) + ".png";
        File iconFile = new File( path + iconFileStr );
        String effectiveIconFile = iconFileStr;
        if( !iconFile.exists() ) {
            int indexOfFirstUnderscore = iconFileStr.indexOf( "_" );
            if( indexOfFirstUnderscore == -1 )
                effectiveIconFile = "default.png";
            else {
                String iconFileStr2 = iconFileStr.substring( 0, indexOfFirstUnderscore + 1 ) + "default.png";
                File iconFile2 = new File( path + iconFileStr2 );
                effectiveIconFile = iconFileStr2;
                if( !iconFile2.exists() )
                    effectiveIconFile = "default.png";
            }
        }
        return imagesPath + effectiveIconFile;
    }
}
