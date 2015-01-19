package ca.licef.comete.core;

import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.vocabulary.RDF;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import licef.tsapi.vocabulary.RDFS;
import org.json.XML;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 5-Jul-2012
 */

public class DefaultView implements ResourceView {

    static ca.licef.comete.core.util.Util CoreUtil;

    TripleStore tripleStore = Core.getInstance().getTripleStore();

    public String getRdf(String uri, String includeIncomingLinks, boolean includeRdfMetadataInfos, boolean isHumanReadable ) throws Exception {
        Triple[] triples = getTriples( uri, includeIncomingLinks, includeRdfMetadataInfos, isHumanReadable );
        String rdf = Util.getTriplesAsRdf( triples );
        return( rdf );
    }

    public String getIncomingLinks( String uri, boolean isHumanReadable, int offset, int limit, String format ) throws Exception {
        Triple[] triples = getIncomingLinkTriples( uri, isHumanReadable, offset, limit );
        String rdf = Util.getTriplesAsRdf( triples );

        if( "rdf".equals( format ) )
            return( rdf );

        if( "json".equals( format ) )
            return( XML.toJSONObject( rdf ).toString() );

        return( null );
    }

    public String getHtml(String uri, Locale locale, String style, ServletContext context) throws Exception {
        return "<html><body>n/a</body></html>";
    }

    public Triple[] getTriples(String uri, String includeIncomingLinks, boolean includeRdfMetadataInfos, boolean isHumanReadable ) throws Exception {
        Invoker inv = new Invoker( this, "ca.licef.comete.core.DefaultView", "doGetTriples", 
            new Object[] { uri, includeIncomingLinks, Boolean.valueOf( includeRdfMetadataInfos ), Boolean.valueOf( isHumanReadable ) } ); 
        Triple[] triples = (Triple[])tripleStore.transactionalCall( inv );
        return( triples );
    }

    /*
     * This method must be transactionally called. - FB
     */
    public Triple[] doGetTriples(String uri, String includeIncomingLinks, boolean includeRdfMetadataInfos, boolean isHumanReadable ) throws Exception {
        ArrayList<Triple> listTriples = new ArrayList<Triple>();
        HashMap<String,String[]> labels = new HashMap<String,String[]>();
        String rdfResUri = uri + ".rdf";

        if( tripleStore.isResourceExists( uri ) ) {
            if( isHumanReadable ) {
                String[] objLabel = CoreUtil.getResourceLabel( uri );
                labels.put( uri, objLabel );
            }

            Triple[] outgoingLinkTriples = tripleStore.getTriplesWithSubject(uri);
            for( Triple triple : outgoingLinkTriples ) {
                if( isHumanReadable && !triple.isObjectLiteral() ) {
                    String objUri = triple.getObject();
                    String[] objLabel = CoreUtil.getResourceLabel( objUri );
                    if (objLabel != null && !objUri.equals( objLabel[ 0 ] ))
                        labels.put( objUri, objLabel );
                }

                // Omit private info.
                if( DCTERMS.replaces.getURI().equals(triple.getPredicate()) )
                    continue;

                // change formatted address to show only no empty values
                if(COMETE.formattedAddress.getURI().equals(triple.getPredicate()) ) {
                    String[] vals = triple.getObject().split(";");
                    String newObj = "";
                    String delimiter = "";
                    for (String val : vals) {
                        if (!"".equals(val)) {
                            newObj += delimiter;
                            newObj += val;
                            delimiter = ";";
                        }
                    }
                    triple.setObject(newObj);
                }

                listTriples.add( triple );
            }

            if( includeRdfMetadataInfos ) {
                String[] objLabel = CoreUtil.getResourceLabel(uri);
                String label = objLabel[ 0 ];
                listTriples.add( new Triple( uri, FOAF.isPrimaryTopicOf, rdfResUri ));
                listTriples.add( new Triple( rdfResUri, RDF.type, FOAF.Document ) );
                listTriples.add( new Triple( rdfResUri, RDFS.label, "RDF version of : " + label ) );
                listTriples.add( new Triple( rdfResUri, FOAF.primaryTopic, uri ) );
            }

            if( isHumanReadable ) {
                for( String objUri : labels.keySet() ) {
                    String[] objLabel = (String[])labels.get( objUri );
                    String label = objLabel[ 0 ];
                    String lang = objLabel[ 1 ];
                    Triple tripleObjLabel = new Triple( objUri, RDFS.label.getURI(), label, true, lang );
                    listTriples.add( tripleObjLabel );
                }
            }
            
            if( "true".equals( includeIncomingLinks ) || "firstPage".equals( includeIncomingLinks ) ) {
                Triple[] incomingLinkTriples =
                    ( "firstPage".equals( includeIncomingLinks ) ? 
                        getIncomingLinkTriples( uri, isHumanReadable, 0, 20 ) :
                            getIncomingLinkTriples( uri, isHumanReadable, -1, -1 ) );
                listTriples.addAll( Arrays.asList( incomingLinkTriples ) );
            }
        }

        Triple[] triples = listTriples.toArray( new Triple[ listTriples.size() ] );
        return( triples );
    }

    public Triple[] getIncomingLinkTriples(String uri, boolean isHumanReadable, int offset, int limit ) throws Exception {
        ArrayList<Triple> listTriples = new ArrayList<Triple>();
        HashMap<String,String[]> labels = new HashMap<String,String[]>();

        Triple[] incomingLinkTriples = null;
        if ( offset != -1 || limit != -1 ) {
            String query = CoreUtil.getQuery("getTriplesWithObjectRange.sparql", uri, Util.buildRange(offset, limit));
            incomingLinkTriples = tripleStore.getTriplesInSparql(query);
        }
        else
            incomingLinkTriples = tripleStore.getTriplesWithObject( uri, false, null );

        for( Triple triple : incomingLinkTriples ) {
            if (isHumanReadable) {
                String objUri = triple.getSubject();
                String[] objLabel = CoreUtil.getResourceLabel( objUri );
                if (objLabel != null && !objUri.equals( objLabel[ 0 ] ))
                    labels.put( triple.getSubject(), objLabel );
            }
            listTriples.add( triple );
        }

        if( isHumanReadable ) {
            for( String objUri : labels.keySet() ) {
                String[] objLabel = (String[])labels.get( objUri );
                String label = objLabel[ 0 ];
                String lang = objLabel[ 1 ];
                Triple tripleObjLabel = new Triple( objUri, RDFS.label.getURI(), label, true, lang );
                listTriples.add( tripleObjLabel );
            }
        }

        Triple[] triples = listTriples.toArray( new Triple[ listTriples.size() ] );
        return( triples );
    }
}
