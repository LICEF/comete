package ca.licef.comete.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import licef.reflection.Invoker;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.TripleStore;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.RDF;
import licef.tsapi.vocabulary.FOAF;

/**
 * Created by amiara on 2014-11-03.
 */
public class RepositoryManager {

    private static RepositoryManager instance;

    private licef.tsapi.TripleStore tripleStore = Core.getInstance().getTripleStore();

    public static RepositoryManager getInstance() {
        if (instance == null)
            instance = new RepositoryManager();
        return (instance);
    }

    public String[][] getRepositories() throws Exception {
        String query = Util.getQuery("metadata/getRepositories.sparql");
        Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "sparqlSelect", new Object[] { query } ); 
        Tuple[] tuples = (Tuple[])tripleStore.transactionalCall( inv );
        String[][] res = new String[tuples.length][2];
        for( int i = 0; i < tuples.length; i++ ) {
            res[i][0] = tuples[i].getValue("r").getContent();
            res[i][1] = tuples[i].getValue("label").getContent();
        }
        return res;
    }

    public String addOrUpdateRepository( String id, String name, String type, String url, String adminEmail, String defId ) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.metadata.RepositoryManager", 
            "addOrUpdateRepositoryEff", new Object[] { id, name, type, url, adminEmail, defId } );
        return( (String)tripleStore.transactionalCall( inv, TripleStore.WRITE_MODE ) );
    }

    public String addOrUpdateRepositoryEff( String id, String name, String type, String url, String adminEmail, String defId ) throws Exception {
        String repoUri = Util.makeURI( id, COMETE.Repository.getURI() );

        Triple[] triples = tripleStore.getTriplesWithSubject( repoUri );
        if( triples.length == 0 ) {
            List<Triple> newTriples = new ArrayList<Triple>();
            newTriples.add( new Triple( repoUri, RDF.type, COMETE.Repository ) );
            newTriples.add( new Triple( repoUri, DCTERMS.identifier, defId ) );
            if( name != null && !"".equals( name ) )
                newTriples.add( new Triple( repoUri, FOAF.name, name ) );
            if( type != null && !"".equals( type ) )
                newTriples.add( new Triple( repoUri, COMETE.repoType, type ) );
            if( url != null && !"".equals( url ) )
                newTriples.add( new Triple( repoUri, FOAF.page, url ) );
            if( adminEmail != null && !"".equals( adminEmail ) )
                newTriples.add( new Triple( repoUri, FOAF.mbox, adminEmail ) );
            tripleStore.insertTriples( newTriples ); 
        }
        else {
            for( Triple triple : triples ) {
                if( triple.getPredicate().equals( DCTERMS.identifier.getURI() ) && !triple.getObject().equals( defId ) )
                    tripleStore.updateObjectTriple( repoUri, DCTERMS.identifier, triple.getObject(), defId );
                else if( triple.getPredicate().equals( FOAF.name.getURI() ) && !triple.getObject().equals( name ) )
                    tripleStore.updateObjectTriple( repoUri, FOAF.name, triple.getObject(), name );
                else if( triple.getPredicate().equals( COMETE.repoType.getURI() ) && !triple.getObject().equals( type ) ) 
                    tripleStore.updateObjectTriple( repoUri, COMETE.repoType, triple.getObject(), type );
                else if( triple.getPredicate().equals( FOAF.page.getURI() ) && !triple.getObject().equals( url ) ) 
                    tripleStore.updateObjectTriple( repoUri, FOAF.page, triple.getObject(), url );
                else if( triple.getPredicate().equals( FOAF.mbox.getURI() ) && !triple.getObject().equals( adminEmail ) ) 
                    tripleStore.updateObjectTriple( repoUri, FOAF.mbox, triple.getObject(), adminEmail );
            }
        }

        return( repoUri );
    }

    public String[][] getRepositoryRecords(String repoUri) throws Exception {
        return( Metadata.getInstance().getRepositoryRecords( repoUri ) );
    }

    public void deleteRepositoryRecords(String repoUri) throws Exception {
        Metadata.getInstance().deleteRepositoryRecords( repoUri );
    }
}
