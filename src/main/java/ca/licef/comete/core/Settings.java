package ca.licef.comete.core;

import ca.licef.comete.core.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import licef.reflection.Invoker;
import licef.StringUtil;
import licef.tsapi.model.Triple;
import licef.tsapi.TripleStore;
import licef.tsapi.vocabulary.FOAF;

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

        tripleStore.removeTriplesWithPredicate(COMETE.validationEnabled, Settings.GRAPH);

        List<Triple> triplesToAdd = new ArrayList<Triple>();
        for( String applProf : applProfTable.keySet() ) {
            Triple triple = new Triple( applProf, COMETE.validationEnabled, applProfTable.get( applProf ) + "" );
            triplesToAdd.add( triple );
        }
        tripleStore.insertTriples( triplesToAdd, Settings.GRAPH );
    }
 
    public static Object[] getNotificationSettings() throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( null, "ca.licef.comete.core.Settings", "doGetNotificationSettings", new Object[] {} ); 
        return( (Object[])tripleStore.transactionalCall( inv ) );
    }

    public static Object[] doGetNotificationSettings() throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Triple[] emailTriples = tripleStore.getTriplesWithSubjectPredicate( "http://notifier", FOAF.mbox, Settings.GRAPH );
        Triple[] brokenLinkValidComplNotifTriples = tripleStore.getTriplesWithSubjectPredicate( "http://brokenLinkValidationCompletionNotif", COMETE.notificationEnabled, Settings.GRAPH );
        Triple[] harvestCompletionNotifTriples = tripleStore.getTriplesWithSubjectPredicate( "http://harvestCompletionNotif", COMETE.notificationEnabled, Settings.GRAPH );

        Object[] resp = new Object[ 3 ];
        resp[ 0 ] = ( emailTriples.length == 0 ? null : emailTriples[ 0 ].getObject() );
        resp[ 1 ] = ( brokenLinkValidComplNotifTriples.length == 0 ? false : "true".equals( brokenLinkValidComplNotifTriples[ 0 ].getObject() ) );
        resp[ 2 ] = ( harvestCompletionNotifTriples.length == 0 ? false : "true".equals( harvestCompletionNotifTriples[ 0 ].getObject() ) );
            
        return( resp );
    }

    public static void setNotificationSettings( String notifEmail, boolean brokenLinkValidationCompletionNotif, boolean harvestCompletionNotif ) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( null, "ca.licef.comete.core.Settings", "doSetNotificationSettings", 
            new Object[] { notifEmail, brokenLinkValidationCompletionNotif, harvestCompletionNotif } ); 
        tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE );
    }

    /*
     * This method must be transactionally called. - FB
     */
    public static void doSetNotificationSettings( String notifEmail, boolean brokenLinkValidationCompletionNotif, boolean harvestCompletionNotif ) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Triple[] enabledValidationTriples = tripleStore.getTriplesWithPredicate( COMETE.validationEnabled, Settings.GRAPH );

        tripleStore.removeTriplesWithSubjectPredicate( "http://notifier", FOAF.mbox, Settings.GRAPH );
        tripleStore.removeTriplesWithPredicate( COMETE.notificationEnabled, Settings.GRAPH );

        List<Triple> triplesToAdd = new ArrayList<Triple>();
        if( !StringUtil.isEmpty( notifEmail ) )
            triplesToAdd.add( new Triple( "http://notifier", FOAF.mbox, notifEmail ) );

        triplesToAdd.add( new Triple( "http://brokenLinkValidationCompletionNotif", COMETE.notificationEnabled, brokenLinkValidationCompletionNotif + "" ) );
        triplesToAdd.add( new Triple( "http://harvestCompletionNotif", COMETE.notificationEnabled, harvestCompletionNotif + "" ) );
        tripleStore.insertTriples( triplesToAdd, Settings.GRAPH );
    }
 
}
