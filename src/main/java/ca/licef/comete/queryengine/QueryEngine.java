package ca.licef.comete.queryengine;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.metadata.Metadata;
import ca.licef.comete.queryengine.util.Util;
import licef.IOUtil;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.Hashtable;
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

    public ResultSet search( String query, String filters, String lang, String outputFormat,
                             int start, int limit, String style, QueryCache cache) throws Exception {

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
                    "simpleSearch", new Object[]{firstCond.getString("value"), lang, orderByVariable, start, limit, outputFormat});
            rs = (ResultSet)tripleStore.transactionalCall(inv);
        }

        if (isExplicitLORequested) {
            Invoker inv = new Invoker(this, "ca.licef.comete.queryengine.QueryEngine",
                    "singleSearch", new Object[]{firstCond.getString("value"), lang, start, limit});
            rs = (ResultSet)tripleStore.transactionalCall(inv);
        }

        if (isAdvancedSearch) {
            Invoker inv = new Invoker(this, "ca.licef.comete.queryengine.QueryEngine",
                    "advancedSearch", new Object[]{queryArray, lang, orderByVariable,
                        start, limit, isWithScore, style, cache, outputFormat});
            rs = (ResultSet)tripleStore.transactionalCall(inv);
        }

        rs.setStart( start );
        rs.setLimit( limit );
        return rs;
    }

    public ResultSet simpleSearch(String keywords, String lang, String orderByVariable, int start, int limit, String outputFormat) throws Exception {
        ResultSet rs = null;
        int count;
        String keywordsFormattedForRegex = null;
        boolean showAllRecords = (keywords == null || keywords.trim().equals(""));
        String _query;

        //count
        Tuple[] res;
        if (showAllRecords) {
            _query = CoreUtil.getQuery("queryengine/getLearningObjectsCount.sparql");
            res = tripleStore.sparqlSelect(_query);
        }
        else {
            keywordsFormattedForRegex = ca.licef.comete.core.util.Util.formatKeywords(keywords);
            _query = CoreUtil.getQuery("queryengine/getLearningObjectsByKeywordsCount.sparql", lang, keywordsFormattedForRegex);
            res = tripleStore.sparqlSelectWithTextIndex(_query,
                    Constants.indexQueryPredicates, Constants.INDEX_LANGUAGES, null);
        }
        count = Integer.parseInt(res[0].getValue("count").getContent());

        //query
        if (count != 0) {
            Tuple[] results;
            if (showAllRecords) {
                _query = CoreUtil.getQuery("queryengine/getLearningObjects.sparql", start, limit);
                results = tripleStore.sparqlSelect(_query);
            }
            else {
                _query = CoreUtil.getQuery("queryengine/getLearningObjectsByKeywords.sparql", lang, keywordsFormattedForRegex, orderByVariable, start, limit);
                results = tripleStore.sparqlSelectWithTextIndex(_query,
                        Constants.indexQueryPredicates, Constants.INDEX_LANGUAGES, null);
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

    public ResultSet singleSearch(String uri, String lang, int start, int limit) throws Exception {
        String _query = CoreUtil.getQuery("queryengine/getLearningObject.sparql", uri);
        Tuple[] results = tripleStore.sparqlSelect(_query);
        ResultSet rs = buildResultSet(results, 1, lang);
        rs.setAdditionalData("selectFirstRecord", "true");

        return rs;
    }

    private ResultSet advancedSearch( JSONArray queryArray, String lang, String orderByVariable,
                                      int start, int limit, boolean isWithScore, String style,
                                      QueryCache cache, String outputFormat) throws Exception {
        ResultSet rs = null;
        Object[] elements = ca.licef.comete.queryengine.util.Util.buildQueryElements(queryArray, lang, isWithScore, cache);
        String clauses = (String)elements[0];
        int count = ((Integer)elements[1]).intValue();

//suffix by WS when needed -AM
        if( count > 0 ) {
            /*String varScore = orderByVariable;
            if (!"?score".equals(orderByVariable))
                varScore = "";
            Hashtable<String, String>[] results =
                    tripleStore.getResults("getLearningObjectsAdvancedQuery" + (isWithScore?"WS":"") + ".sparql", clauses, orderByVariable, start, limit, varScore);
            rs = buildResultSet(results, count, lang, style);*/
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
            String id = CoreUtil.getIdNumberValue( objId );

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
                image = Util.getMimeTypeIcon(mimetype);
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
            /*String loHtmlLocation = Util.getRestUrl(Constants.TYPE_LEARNING_OBJECT) + "/" + entry.getId() + "/html?lang=$lang";
            entry.setLoAsHtmlLocation( loHtmlLocation );*/

            //date
            entry.setCreationDate( CoreUtil.manageDateString(tuple.getValue("added").getContent()));
            entry.setModificationDate( CoreUtil.manageDateString(tuple.getValue("updated").getContent()));
            
            //type
            /*
            Hashtable<String, String>[] res =
                tripleStore.getResults("getMetadataRecord.sparql", objId);
            Hashtable first = res[0]; //first response only
            String recordType = ( "".equals(first.get( "oaiId" )) ? "local_record" : "harvested_record" );
            entry.setType( recordType );
            *//*

            if ("endrea".equals(style)) {
                String resType = "unknown";
                Hashtable<String, String>[] res = tripleStore.getResults("getCaDResType.sparql", objId);
                if (res.length > 0) {
                    Hashtable<String, String> first = res[0]; //first response only
                    String resTypeValue = first.get("o");
                    int indexOfHash = resTypeValue.indexOf("#");
                    if (indexOfHash != -1)
                        resType = resTypeValue.substring(indexOfHash + 1);
                }
                entry.setType(resType);
            }

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


    
    private static QueryEngine instance;


}
