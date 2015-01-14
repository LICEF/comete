package ca.licef.comete.core;

import ca.licef.comete.core.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import licef.reflection.Invoker;
import licef.tsapi.model.Triple;
import licef.tsapi.TripleStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Settings {

    public static final String GRAPH = "settings";

    public static Map<String,Boolean> getValidatedApplicationProfiles() throws Exception {
        Map<String,Boolean> applProfTable = new HashMap<String,Boolean>();
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "getTriplesWithPredicate", 
            new Object[] { COMETE.validationEnabled, new String[] { Settings.GRAPH } } );
        Triple[] enabledValidationTriples = (Triple[])tripleStore.transactionalCall( inv ); 
        for( Triple triple : enabledValidationTriples )
            applProfTable.put( triple.getSubject(), Boolean.valueOf( triple.getObject() ) );
        return( applProfTable );
    }

    public static void setValidatedApplicationProfiles( Map<String,Boolean> applProfTable ) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( null, "ca.licef.comete.core.Settings", "doSetValidatedApplicationProfiles", new Object[] { applProfTable } ); 
        tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE );
    }

    /*
     * This method must be transactionally called. - FB
     */
    public static void doSetValidatedApplicationProfiles( Map<String,Boolean> applProfTable ) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Triple[] enabledValidationTriples = tripleStore.getTriplesWithPredicate( COMETE.validationEnabled, Settings.GRAPH );

        String query = Util.getQuery( "deleteValidationSettings.sparql", tripleStore.getUri( GRAPH ) );
        tripleStore.sparqlUpdate( query );

        List<Triple> triplesToAdd = new ArrayList<Triple>();
        for( String applProf : applProfTable.keySet() ) {
            Triple triple = new Triple( applProf, COMETE.validationEnabled, applProfTable.get( applProf ) + "" );
            triplesToAdd.add( triple );
        }
        tripleStore.insertTriples( triplesToAdd, Settings.GRAPH );
    }
 
}
