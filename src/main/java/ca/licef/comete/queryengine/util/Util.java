package ca.licef.comete.queryengine.util;

import ca.licef.comete.queryengine.QueryCache;
import ca.licef.comete.queryengine.QueryEngine;
import ca.licef.comete.vocabulary.Vocabulary;
import licef.DateUtil;
import licef.tsapi.model.Tuple;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

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
    public static final String FROM_HARVESTED_REPO = "fromHarvestedRepo";
    public static final String NOT_FROM_HARVESTED_REPO = "!fromHarvestedRepo";
    public static final String FLAG_PREFIX = "flag";
    public static final String NOT_FLAG_PREFIX = "!flag";
    public static final String ADDED_DATE = "addedDate";

    public static final String REL_OP_GTE = "gte";
    public static final String REL_OP_GT = "gt";
    public static final String REL_OP_LTE = "lte";
    public static final String REL_OP_LT = "lt";
    public static final String REL_OP_EQ = "eq";

    public static HashMap<String,String> relOpTable;

    static ca.licef.comete.core.util.Util CoreUtil;

    /**
     * @return clause element and pre compute size
     */
    public static String[] buildQueryClauses(JSONArray queryArray, String lg, boolean isWithScore, QueryCache cache) throws Exception {
        String fromClause = "";
        String clauses = "";

        boolean waitForOperator = false;

        String orClauses = "";
        String firstClause = null;

        boolean includeEquivalence = false;

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
                if (FULLTEXT.equals(condType)) {
                    String text = obj.getString("value");
                    if( !text.trim().equals( "" ) )
                        clause = CoreUtil.getQuery("queryengine/advancedFulltextFragment.sparql",
                                    CoreUtil.formatKeywords(text), lg);
                }
                else if (LANGUAGE.equals(condType)) {
                    String lang = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedLanguageFragment.sparql", lang);
                }
                else if (TITLE_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedTitleFragment.sparql", text);
                }
                else if (DESCRIPTION_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedDescriptionFragment.sparql", text);
                }
                else if (KEYWORD_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedKeywordFragment.sparql", text);
                }
                else if (CONTRIBUTE_PREFIX.equals(condType) || condType.equals(NOT_CONTRIBUTE_PREFIX)) {
                    String uri = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedContribFragment.sparql", uri);
                    //negation
                    if (condType.equals(NOT_CONTRIBUTE_PREFIX))
                        clause = "MINUS { " + clause + " }\n";
                }
                else if (ORGANIZATION_PREFIX.equals(condType) || condType.equals(NOT_ORGANIZATION_PREFIX)) {
                    String uri = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedOrgFragment.sparql", uri);

                    //negation
                    if (condType.equals(NOT_ORGANIZATION_PREFIX))
                        clause = "MINUS { " + clause + " }\n";
                }
                else if (condType.equals(CONCEPT_PREFIX) || condType.startsWith(NOT_CONCEPT_PREFIX)) {
                    String uri = obj.getString("value");
                    String vocUri = Vocabulary.getInstance().getConceptScheme(uri);
                    boolean isSubConcept = obj.has("subConcepts") && obj.getBoolean("subConcepts");
                    includeEquivalence = obj.has("equivalent") && obj.getBoolean("equivalent");

                    if (isSubConcept)
                        clause = CoreUtil.getQuery("queryengine/advancedVocConceptHierarchyFragment.sparql", uri, vocUri);
                    else
                        clause = CoreUtil.getQuery("queryengine/advancedVocConceptFragment.sparql", uri, vocUri);

                    //negation
                    if (condType.equals(NOT_CONCEPT_PREFIX))
                        clause = "MINUS " + clause;

                    //Equivalence
                    //for the moment, if equivalence requested, it comes from thematic navigation,
                    //so new clause to build without negation.
                    if (includeEquivalence) {
                        JSONArray eqVocs = (obj.has("eqVocs"))?obj.getJSONArray("eqVocs"):null;
                        if (eqVocs != null) {
                            fromClause = "FROM <urn:x-arq:DefaultGraph>\n";
                            Tuple[] ctxts = Vocabulary.getInstance().getVocContexts();
                            for (Tuple ctxt : ctxts)
                                fromClause += "FROM <" + ctxt.getValue("vocUri").getContent() + ">\n";

                            ArrayList<String> _eqVocs = new ArrayList<>();
                            //first add start vocUri -AM
                            _eqVocs.add(Vocabulary.getInstance().getConceptScheme(uri));
                            for (int k = 0; k < eqVocs.length(); k++)
                                _eqVocs.add(eqVocs.getString(k));

                            String vocFilterConstraint = CoreUtil.buildFilterConstraints(_eqVocs, "vocUri", true, "=", "||");
                            if (isSubConcept)
                                clause = CoreUtil.getQuery("queryengine/advancedVocConceptEquivalenceHierarchyFragment.sparql", uri, vocFilterConstraint);
                            else
                                clause = CoreUtil.getQuery("queryengine/advancedVocConceptEquivalenceFragment.sparql", uri, vocFilterConstraint);
                        }
                        else
                            clause = "";
                    }
                }
                else if (FROM_HARVESTED_REPO.equals(condType) || condType.equals(NOT_FROM_HARVESTED_REPO)) {
                    String uri = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedFromHarvestedRepoFragment.sparql", uri);

                    //negation
                    if (condType.equals(NOT_FROM_HARVESTED_REPO))
                        clause = "MINUS { " + clause + " }\n";
                }
                else if (FLAG_PREFIX.equals(condType) || condType.equals(NOT_FLAG_PREFIX)) {
                    String flag = obj.getString("value");
                    clause = CoreUtil.getQuery("queryengine/advancedFlagFragment.sparql", flag);

                    //negation
                    if (condType.equals(NOT_FLAG_PREFIX))
                        clause = "MINUS { " + clause + " }\n";
                }
                else if (ADDED_DATE.equals(condType)) {
                    String relOp = obj.getString( "relOp" );
                    String date = obj.getString( "value" );
                    clause = makeAddedDateClause( relOp, date );
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

                waitForOperator = !waitForOperator;
            }
        }
        //last cond
        clauses += orClauses;

        return new String[]{fromClause, clauses, Boolean.toString(includeEquivalence)};
    }

    public static String makeAddedDateClause( String relOpStr, String date ) throws Exception{
        String dateWithoutTime = date;
        int indexOfTimeDelimiter = dateWithoutTime.indexOf( "T" );
        if( indexOfTimeDelimiter != -1 )
            dateWithoutTime = dateWithoutTime.substring( 0, indexOfTimeDelimiter );
        String dateClause;
        if (REL_OP_GTE.equals(relOpStr)) {
            dateClause = CoreUtil.getQuery("queryengine/advancedAddedDateFragment.sparql", getRelOp( relOpStr ), dateWithoutTime);
        }
        else if (REL_OP_EQ.equals(relOpStr)) {
            dateClause = CoreUtil.getQuery("queryengine/advancedAddedDateFragment.sparql", getRelOp( REL_OP_GTE), dateWithoutTime);
            dateClause += "\n" + CoreUtil.getQuery("queryengine/advancedAddedDateFragment.sparql", getRelOp( REL_OP_LT ),
                    DateUtil.nextDay(dateWithoutTime));
        }
        else
            dateClause = CoreUtil.getQuery("queryengine/advancedAddedDateFragment.sparql", getRelOp( relOpStr ),
                    DateUtil.nextDay(dateWithoutTime));
        return dateClause;
    }

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
        ResourceBundle bundle = ResourceBundle.getBundle("translations/Strings", new Locale(outputLang));

        StringBuilder title = new StringBuilder();
        StringBuilder description = new StringBuilder( bundle.getString( "rs.advancedSearch.description.prefix" ) );

        title.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.title" ), outputFormat.toUpperCase() ) );

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
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.title" ), text ) );
                }
                else if (DESCRIPTION_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.description" ), text ) );
                }
                else if (KEYWORD_PREFIX.equals(condType)) {
                    String text = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.keyword" ), text ) );
                }
                else if (CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.contributor" ), 
                        CoreUtil.getResourceLabel( uri, outputLang ) ) );
                }
                else if (NOT_CONTRIBUTE_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.contributor.not" ),
                        CoreUtil.getResourceLabel( uri, outputLang ) ) );
                }
                else if (ORGANIZATION_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.organization" ),
                        CoreUtil.getResourceLabel( uri, outputLang ) ) );
                }
                else if (NOT_ORGANIZATION_PREFIX.equals(condType)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.organization.not" ),
                        CoreUtil.getResourceLabel( uri, outputLang ) ) );
                }
                else if (condType.startsWith(CONCEPT_PREFIX)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format(bundle.getString("rs.advancedSearch.description.concept"),
                        CoreUtil.getResourceLabel(uri, outputLang, true)) );
                    if (obj.has("subConcepts") && obj.getBoolean("subConcepts"))
                        description.append( " " ).append( bundle.getString( "rs.advancedSearch.description.andSubConcepts" ) );
                }
                else if (condType.startsWith(NOT_CONCEPT_PREFIX)) {
                    String uri = obj.getString("value");
                    description.append( MessageFormat.format( bundle.getString( "rs.advancedSearch.description.concept.not" ),
                        CoreUtil.getResourceLabel( uri, outputLang, true )) );
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
                        CoreUtil.getResourceLabel( uri, outputLang ) ) );
                }

                waitForOperator = !waitForOperator;
            }
        }

        description.append( "." );

        return( new String[] { title.toString(), description.toString() } );
    }

    public static String getRelOp( String relOp ) {
        if( relOpTable == null ) {
            relOpTable = new HashMap<>();
            relOpTable.put( REL_OP_GTE, ">=" );
            relOpTable.put( REL_OP_GT, ">" );
            relOpTable.put( REL_OP_LTE, "<=" );
            relOpTable.put( REL_OP_LT, "<" );
            relOpTable.put( REL_OP_EQ, "=" );
        }
        return( relOpTable.get( relOp ) );
    }

}
