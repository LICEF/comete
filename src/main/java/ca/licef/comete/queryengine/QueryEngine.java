package ca.licef.comete.queryengine;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.vocabularies.COMETE;
import licef.IOUtil;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class QueryEngine {

    static ca.licef.comete.core.util.Util CoreUtil;

    public static QueryEngine getInstance() {
        if( instance == null ) 
            instance = new QueryEngine();
        return( instance );
    }

    Collection collection;
    TripleStore tripleStore;

    private QueryEngine() {
        collection = new Collection();
        tripleStore = Core.getInstance().getTripleStore();
    }

    public Collection getCollection() {
        return collection;
    }

    public ResultSet search( String query, String filters, String lang, boolean isShowHiddenRes, String outputFormat,
                             Integer start, Integer limit, String style, QueryCache cache) throws Exception {

        JSONArray queryArray = new JSONArray(query);

        JSONArray filtersArray = null;
        if (!"".equals(filters))
            filtersArray = new JSONArray(filters);

        JSONObject firstCond = (JSONObject)queryArray.get(0);

        boolean isExplicitLORequested = ( queryArray.length() == 1 &&
                        "uri".equals(firstCond.getString("key"))) ;

        boolean isWithScore = ("fulltext".equals(firstCond.getString("key")) &&
                !firstCond.getString("value").trim().equals(""));

        boolean isSimpleSearch = ( queryArray.length() == 1 &&
                (isWithScore ||  //previous comete's simple search compatibility
                        "ss".equals(firstCond.getString("key"))) );

        boolean isAdvancedSearch = !isSimpleSearch && !isExplicitLORequested;

        String orderByVariable = "?added";
        if (filtersArray != null && filtersArray.length() > 0) {
            JSONObject firstFilter = (JSONObject)filtersArray.get(0);
            String orderBy = firstFilter.getString("value");
            if (isWithScore && "relevance".equals(orderBy))
                orderByVariable = "?score";
        }

        ResultSet rs = null;

        if (isSimpleSearch) {
            Invoker inv = new Invoker(this, "ca.licef.comete.queryengine.QueryEngine",
                    "simpleSearch", new Object[]{firstCond.getString("value"), lang, isShowHiddenRes, orderByVariable, start, limit, outputFormat});
            rs = (ResultSet)tripleStore.transactionalCall(inv);
        }

        if (isExplicitLORequested) {
            Invoker inv = new Invoker(this, "ca.licef.comete.queryengine.QueryEngine",
                    "singleSearch", new Object[]{firstCond.getString("value"), lang, isShowHiddenRes, start, limit});
            rs = (ResultSet)tripleStore.transactionalCall(inv);
        }

        if (isAdvancedSearch) {
            Invoker inv = new Invoker(this, "ca.licef.comete.queryengine.QueryEngine",
                    "advancedSearch", new Object[]{queryArray, lang, isShowHiddenRes, orderByVariable,
                        start, limit, Boolean.valueOf(isWithScore), cache, outputFormat});
            rs = (ResultSet)tripleStore.transactionalCall(inv);
        }

        rs.setStart(start);
        rs.setLimit(limit);
        return rs;
    }

    public ResultSet simpleSearch(String keywords, String lang, boolean isShowHiddenRes, String orderByVariable, Integer start, Integer limit, String outputFormat) throws Exception {
        ResultSet rs;
        int count;
        String keywordsFormattedForRegex = null;
        boolean showAllRecords = (keywords == null || keywords.trim().equals(""));
        String _query;

        //count
        Tuple[] res;
        String flagCondition = ( isShowHiddenRes ? "" : "FILTER( NOT EXISTS { ?s comete:flag ?flag } )" );
        if (showAllRecords) {
            _query = CoreUtil.getQuery("queryengine/getLearningObjectsCount.sparql", flagCondition );
            res = tripleStore.sparqlSelect(_query);
        }
        else {
            keywordsFormattedForRegex = ca.licef.comete.core.util.Util.formatKeywords(keywords);
            _query = CoreUtil.getQuery("queryengine/getLearningObjectsByKeywordsCount.sparql", keywordsFormattedForRegex, lang, flagCondition );
            res = tripleStore.sparqlSelect_textIndex(_query);
        }
        count = Integer.parseInt(res[0].getValue("count").getContent());

        //query
        if (count != 0) {
            Tuple[] results;
            if (showAllRecords) {
                _query = CoreUtil.getQuery("queryengine/getLearningObjects.sparql", flagCondition, start, limit);
                results = tripleStore.sparqlSelect(_query);
            }
            else {
                _query = CoreUtil.getQuery("queryengine/getLearningObjectsByKeywords.sparql", keywordsFormattedForRegex, lang, flagCondition, orderByVariable, start, limit);
                results = tripleStore.sparqlSelect_textIndex(_query);
            }
            rs = buildResultSet(results, count, lang);
        }
        else
            rs = new ResultSet();

        ResourceBundle bundle = ResourceBundle.getBundle( "translations/Strings", new Locale( lang ) );
        String title = MessageFormat.format(bundle.getString("rs.simpleSearch.title"), outputFormat.toUpperCase());
        String description = MessageFormat.format(bundle.getString("rs.simpleSearch.description"), keywords);
        rs.setTitle(title);
        rs.setDescription(description);

        return rs;
    }

    public ResultSet singleSearch(String uri, String lang, boolean isShowHiddenRes, int start, int limit) throws Exception {
        String _query = CoreUtil.getQuery("queryengine/getLearningObject.sparql", uri );
        Tuple[] results = tripleStore.sparqlSelect(_query);
        ResultSet rs = buildResultSet(results, 1, lang);
        rs.setAdditionalData("selectFirstRecord", "true");

        return rs;
    }

    public ResultSet advancedSearch( JSONArray queryArray, String lang, boolean isShowHiddenRes, String orderByVariable,
                                      Integer start, Integer limit, Boolean isWithScore,
                                      QueryCache cache, String outputFormat) throws Exception {
        ResultSet rs;
        String[] data = ca.licef.comete.queryengine.util.Util.buildQueryClauses(queryArray, lang, isWithScore, cache);
        String fromClause = data[0];
        String clauses = data[1];
        String flagCondition = ( isShowHiddenRes ? "" : "FILTER( NOT EXISTS { ?s comete:flag ?flag } )" );
        boolean includeEquivalence = Boolean.parseBoolean(data[2]);

        String queryType = includeEquivalence?"Thematic":"Advanced";

        String query = CoreUtil.getQuery("queryengine/getLearningObjects" + queryType + "QueryForCount.sparql", fromClause, clauses, flagCondition );
        Tuple[] res = tripleStore.sparqlSelect_textIndex(query);
        int count = Integer.parseInt(res[0].getValue("count").getContent());

        if( count > 0 ) {
            String varScore = orderByVariable;
            if (!"?score".equals(orderByVariable))
                varScore = "";
            query = CoreUtil.getQuery("queryengine/getLearningObjects" + queryType + "Query.sparql", fromClause, clauses, flagCondition, orderByVariable, start, limit, varScore);
            Tuple[] results = tripleStore.sparqlSelect_textIndex(query);
            rs = buildResultSet(results, count, lang);
        }
        else
            rs = new ResultSet();

        String[] titleAndDesc = ca.licef.comete.queryengine.util.Util.buildTitleAndDescription( queryArray, lang, outputFormat );
        rs.setTitle( titleAndDesc[ 0 ] );
        rs.setDescription( titleAndDesc[ 1 ] );
        return rs;
    }

    private ResultSet buildResultSet( Tuple[] results, int count, String lang) throws Exception {
        ResultSet rs = new ResultSet();
        for( Tuple tuple : results) {
            String objId = tuple.getValue("s").getContent();
            String location = tuple.getValue( "location").getContent();
            String format = tuple.getValue("format").getContent();
            String id = CoreUtil.getIdValue(objId);

            // mimetype icon as result image
            String image = null;
            String mimetype = null;
            if ( location == null || "".equals(location))
                image = "n/a";
            else {
                if ( format == null )
                    mimetype = IOUtil.getMimeType(location);
                else
                    mimetype = format.substring( "http://purl.org/NET/mediatypes/".length() );
                image = CoreUtil.getMimeTypeIcon(mimetype);
            }
            //

            ResultEntry entry = new ResultEntry();
            entry.setId( id );

            String[] objTitle = tripleStore.getBestLocalizedLiteralObject(objId, DCTERMS.title, lang);
            String title = (objTitle == null)?null:objTitle[ 0 ];
            if (title == null)
                title = "no title for learning object number " + entry.getId();
            entry.setTitle( title );
            String[] objDesc = tripleStore.getBestLocalizedLiteralObject(objId, DCTERMS.description, lang);
            String desc = (objDesc == null)?null:objDesc[ 0 ];
            if (desc != null)
                entry.setDescription( desc );
            entry.setLocation( location );
            entry.setImage(image);

            // $lang will be substituted dynamically by client-side.
            String loHtmlLocation = ca.licef.comete.core.util.Util.getRestUrl(COMETE.LearningObject.getURI().toString()) + "/" + entry.getId() + "/html?lang=$lang";
            entry.setLoAsHtmlLocation( loHtmlLocation );

            //date
            entry.setCreationDate( CoreUtil.manageDateString(tuple.getValue("added").getContent()));
            entry.setModificationDate( CoreUtil.manageDateString(tuple.getValue("updated").getContent()));
            
            //flags
            Triple[] triples = tripleStore.getTriplesWithSubjectPredicate( objId, COMETE.flag );
            for( int i = 0; i < triples.length; i++ ) {
                Triple triple = triples[ i ];
                if( "hidden".equals( triple.getObject() ) )
                    entry.setHidden( true );
                else if( "pending".equals( triple.getObject() ) )
                    entry.setPending( true );
                else if( "inactive".equals( triple.getObject() ) )
                    entry.setInactive( true );
                else if( "invalid".equals( triple.getObject() ) )
                    entry.setInvalid( true );
                else if( "brokenLink".equals( triple.getObject() ) )
                    entry.setBrokenLink( true );
            }

            //type
            /*
            Hashtable<String, String>[] res =
                tripleStore.getResults("getMetadataRecord.sparql", objId);
            Hashtable first = res[0]; //first response only
            String recordType = ( "".equals(first.get( "oaiId" )) ? "local_record" : "harvested_record" );
            entry.setType( recordType );
            *//*


            //metadataFormat
            *//*
            String format = Util.manageQuotes((String)first.get("metadataFormat"));
            MetadataFormat metadataFormat = MetadataFormats.getMetadataFormat(format);
            if (metadataFormat != null)
                entry.setMetadataFormat(metadataFormat.getName());
            */
            
            rs.addEntry( entry );
        }
        rs.setTotalRecords( count );

        return( rs );
    }

    public Tuple[] searchKeywords(String terms, String lang) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.queryengine.QueryEngine",
                "searchKeywordsEff", new Object[]{terms, lang});
        return (Tuple[])tripleStore.transactionalCall(inv);
    }

    public Tuple[] searchKeywordsEff(String terms, String lang) throws Exception {
        String query = CoreUtil.getQuery("queryengine/lookupKeywords.sparql",CoreUtil.formatKeywords(terms), lang);
        return tripleStore.sparqlSelect_textIndex(query);
    }

    private static QueryEngine instance;
}
