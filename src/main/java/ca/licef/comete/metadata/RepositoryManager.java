package ca.licef.comete.metadata;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Util;
import licef.tsapi.model.Tuple;

/**
 * Created by amiara on 2014-11-03.
 */
public class RepositoryManager {

    private static RepositoryManager instance;

    public static RepositoryManager getInstance() {
        if (instance == null)
            instance = new RepositoryManager();
        return (instance);
    }

    licef.tsapi.TripleStore tripleStore = Core.getInstance().getTripleStore();

    public String[][] getRepositories() throws Exception {
        String query = Util.getQuery("metadata/getRepositories.sparql");
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        String[][] res = new String[tuples.length][2];
        for( int i = 0; i < tuples.length; i++ ) {
            res[i][0] = tuples[i].getValue("r").getContent();
            res[i][1] = tuples[i].getValue("label").getContent();
        }
        return res;
    }

    public String addOrUpdateRepository( String id, String name, String type, String url, String adminEmail ) throws Exception {
        /*String repoUri = Util.makeURI( id, Constants.TYPE_REPOSITORY );

        Map<String,Triple> newValues = new HashMap<String,Triple>();
        newValues.put( Constants.TYPE, new Triple( repoUri, Constants.TYPE, Constants.TYPE_REPOSITORY ) );
        if( !name.equals( "" ) )
            newValues.put( Constants.METAMODEL_REPOSITORY_NAME, new Triple( repoUri, Constants.METAMODEL_REPOSITORY_NAME, name ) );
        if( !type.equals( "" ) )
            newValues.put( Constants.METAMODEL_REPOSITORY_TYPE, new Triple( repoUri, Constants.METAMODEL_REPOSITORY_TYPE, type ) );
        if( !url.equals( "" ) )
            newValues.put( Constants.METAMODEL_REPOSITORY_LOCATION, new Triple( repoUri, Constants.METAMODEL_REPOSITORY_LOCATION, url ) );
        if( !adminEmail.equals( "" ) )
            newValues.put( Constants.METAMODEL_REPOSITORY_ADMIN_EMAIL, new Triple( repoUri, Constants.METAMODEL_REPOSITORY_ADMIN_EMAIL, adminEmail ) );

        Triple[] oldTriples = tripleStore.getTriplesWithSubject( repoUri );
        if( oldTriples.length == 0 )
            tripleStore.addTriples( newValues.values() );
        else {
            // Compare existing data and update it when needed.
            Map<String,Triple> oldValues = new HashMap<String,Triple>();
            for( int i = 0; i < oldTriples.length; i++ )
                oldValues.put( oldTriples[ i ].getPredicate(), oldTriples[ i ] );

            for( String key : newValues.keySet() ) {
                Triple newValueTriple = newValues.get( key );
                String newValue = newValueTriple.getObject();
                if( newValue != null && !"".equals( newValue ) ) {
                    Triple oldValueTriple = oldValues.get( key );
                    String oldValue = ( oldValueTriple == null ? null : oldValueTriple.getObject() );
                    if( oldValue != null ) {
                        if( !oldValue.equals( newValue ) )
                            tripleStore.deleteTriple( oldValueTriple );
                        tripleStore.addTriple( newValueTriple );
                    }
                }
            }
        }

        return( repoUri );*/

        return null;
    }

    public String[][] getRepositoryRecords(String repoUri) throws Exception {
        /*Hashtable<String, String>[] results =
                Core.getInstance().getTripleStoreService().getResults( "getRepositoryRecords.sparql", repoUri );
        String[][] res = new String[results.length][2];
        for( int i = 0; i < results.length; i++ ) {
            res[i][0] = results[i].get("s");
            res[i][1] = results[i].get("doId");
        }
        return res; */

        return null;
    }

    public void deleteRepositoryRecords(String repoUri) throws Exception {
        /*String[][] records = getRepositoryRecords(repoUri);
        for( int i = 0; i < records.length; i++ )
            setState(records[i][1], "D");

        //itql enhanced
        Core.getInstance().getTripleStoreService().
                processTQLQueries("deleteRepositoryRecords.tql", repoUri); */
    }
}
