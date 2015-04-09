package ca.licef.comete.harvester;

import java.util.ArrayList;
import java.util.Date;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ORG.oclc.oai.harvester2.verb.*;

import licef.XMLUtil;

public class OAIWorker extends Worker {

    public OAIWorker( String id, String url, String metadataNamespace ) {
        super( id, url, metadataNamespace );
    }

    @Override
    public void run() {
        startDate = new Date();
        report = new Report(getId(), startDate, getFrom());
        Throwable throwable = null;
        try {
            Thread thisThread = Thread.currentThread();
            String metadataPrefix = getMetadataPrefix();
            ListRecords listRecords = new ListRecords( getUrl(), getFrom(), null, null, metadataPrefix );
            boolean isFirstPage = true;
            for( ; thread == thisThread; ) {

                if( isFirstPage ) {
                    listRecords = new ListRecords( getUrl(), getFrom(), null, null, metadataPrefix );
                    isFirstPage = false;
                }
                else {
                    String resumptionToken = listRecords.getResumptionToken();
                    if( resumptionToken == null || resumptionToken.length() == 0 )
                        break;

                    listRecords = new ListRecords( getUrl(), resumptionToken );
                }

                NodeList errors = listRecords.getErrors();
                boolean isNoRecordsMatch = (errors != null && errors.getLength() == 1 &&
                        "noRecordsMatch".equals(errors.item(0).getAttributes().getNamedItem("code").getTextContent()));

                //noRecordsMatch not considered as error
                if (!isNoRecordsMatch)
                    handleErrors( "retrieving metadata records", errors );

                handleRecords( listRecords.getDocument(), thisThread );
            }
        }
        catch( Throwable t ) {
            throwable = t;
        }

        try {
            endDate = new Date(); 
            if( throwable == null && thread != null )
                updateFromDate();
            getReport().generateReport(endDate, throwable, thread == null);
            Harvester.getInstance().removeHarvest(getId());
            thread = null;
        } catch (Exception e) {
            System.out.println("e = " + e);
            e.printStackTrace();
        }
    }

    private String getMetadataPrefix() throws Exception {
        ListMetadataFormats listMetadataFormats = new ListMetadataFormats( getUrl() );
        handleErrors( "retrieving metadata formats", listMetadataFormats.getErrors() );

        Node tmpNode;
        Document doc = listMetadataFormats.getDocument();
        if( doc != null ) {
            NodeList firstChildren = doc.getDocumentElement().getChildNodes();
            for( int i = 0; i < firstChildren.getLength(); i++ ) {
                tmpNode = firstChildren.item( i );
                if( Node.ELEMENT_NODE == tmpNode.getNodeType() && "ListMetadataFormats".equals( tmpNode.getLocalName() ) ) {
                    NodeList secondChildren = tmpNode.getChildNodes();
                    for( int j = 0; j < secondChildren.getLength(); j++ ) {
                        tmpNode = secondChildren.item( j );
                        if( Node.ELEMENT_NODE == tmpNode.getNodeType() && "metadataFormat".equals( tmpNode.getLocalName() ) ) {
                            NodeList thirdChildren = tmpNode.getChildNodes();
                            String prefix = null;
                            boolean isNamespaceFound = false;
                            for( int k = 0; k < thirdChildren.getLength(); k++ ) {
                                tmpNode = thirdChildren.item( k );
                                if( Node.ELEMENT_NODE == tmpNode.getNodeType() ) {
                                    if( "metadataPrefix".equals( tmpNode.getLocalName() ) ) {
                                        prefix = tmpNode.getFirstChild().getNodeValue();
                                        if( isNamespaceFound )
                                            return( prefix );
                                    }
                                    else if( "metadataNamespace".equals( tmpNode.getLocalName() ) ) {
                                        isNamespaceFound = getMetadataNamespace().equals( tmpNode.getFirstChild().getNodeValue() );
                                        if( isNamespaceFound && prefix != null )
                                            return( prefix );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new Exception( "Cannot find metadata prefix for namespace: " + getMetadataNamespace() );
    }

    private void handleRecords( Document doc, Thread workerThread ) throws Exception {
        Node tmpNode;
        if( doc != null ) {
            NodeList firstChildren = doc.getDocumentElement().getChildNodes();
            for( int i = 0; thread == workerThread && i < firstChildren.getLength(); i++ ) {
                tmpNode = firstChildren.item( i );
                if( Node.ELEMENT_NODE == tmpNode.getNodeType() ) {
                    if( "ListRecords".equals( tmpNode.getLocalName() ) ) {
                        NodeList secondChildren = tmpNode.getChildNodes();

                        OaiRecord:

                        for( int j = 0; thread == workerThread && j < secondChildren.getLength(); j++ ) {
                            tmpNode = secondChildren.item( j );
                            if( Node.ELEMENT_NODE == tmpNode.getNodeType() && "record".equals( tmpNode.getLocalName() ) ) {
                                String status = null;
                                String identifier = null;
                                String datestamp = null;
                                String metadata = null;

                                NodeList thirdChildren = tmpNode.getChildNodes();
                                for( int k = 0; k < thirdChildren.getLength(); k++ ) {
                                    tmpNode = thirdChildren.item( k );
                                    if( Node.ELEMENT_NODE == tmpNode.getNodeType() ) {
                                        if( "header".equals( tmpNode.getLocalName() ) ) {
                                            Node statusNode = tmpNode.getAttributes().getNamedItem( "status" );
                                            if( statusNode != null )
                                                status = statusNode.getNodeValue();

                                            NodeList fourthChildren = tmpNode.getChildNodes();
                                            for( int l = 0; l < fourthChildren.getLength(); l++ ) {
                                                tmpNode = fourthChildren.item( l );
                                                if( Node.ELEMENT_NODE == tmpNode.getNodeType() ) {
                                                    if( "identifier".equals( tmpNode.getLocalName() ) )
                                                        identifier = tmpNode.getFirstChild().getNodeValue().trim();
                                                    else if( "datestamp".equals( tmpNode.getLocalName() ) ) {
                                                        Node datestampFirstChild = tmpNode.getFirstChild();
                                                        if( datestampFirstChild != null ) 
                                                            datestamp = tmpNode.getFirstChild().getNodeValue().trim();
                                                    }
                                                }
                                            }
                                        }
                                        else if( "metadata".equals( tmpNode.getLocalName() ) ) {
                                            Node metadataNode = XMLUtil.getFirstChildElementNode( tmpNode );
                                            
                                            // Skip unwanted records from Eureka.
                                            boolean isOk = getId().toLowerCase().indexOf( "eureka" ) == -1 || isRecordOk( metadataNode );
                                            if( !isOk )
                                                continue OaiRecord;

                                            if( metadataNode != null ) {
                                                metadataNode = postProcessMetadata( metadataNode );
                                                metadata = XMLUtil.getXMLString( metadataNode, true );
                                            }
                                        }
                                    }
                                }

                                try {
                                    digest(identifier, status, datestamp, metadata);
                                }
                                catch( Throwable t ) {
                                    getReport().addError( new Error(identifier, t ) );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleErrors( String operation, NodeList errors ) throws Exception {
        if( errors != null && errors.getLength() > 0 )  {
            String message = "Some errors occurred while " + operation + ".\n";
            int errorCount = errors.getLength();
            for( int i = 0; i < errorCount; i++ ) {
                Node error = errors.item( i );
                message += error.getAttributes().getNamedItem("code").getTextContent() + ": ";
                message += error.getTextContent();
            }
            throw new Exception( message );
        }
    }

    /**
     * This method checks if the metadata record must be harvested or not.
     * It is used only for the Eureka repository.
     * This prevents the harvester from fetching records that have already been harvested from local HTML or OAI-PMH harvesting definitions. 
     * Only records that come from Eureka itself will be considered.
     * @param node OAI record
     * @return <tt>true</tt> if the record must be harvested, <tt>false</tt> otherwise.
     */
    private boolean isRecordOk( Node node ) {
        String[] entries = getMetaMetadataIdentifierEntries( (Element)node ); 
        if( entries != null ) {
            for( int i = 0; i < entries.length; i++ ) {
                if( entries[ i ].startsWith( "oai:monde.ccdmd.qc.ca" ) || // Records from the CCDMD Monde en image.
                    entries[ i ].startsWith( "oai:infiressources.ca" ) || // Records from Infiressources.
                    entries[ i ].startsWith( "oai:ccdmd.qc.ca" ) || // Records from CCDMD.
                    entries[ i ].startsWith( "oai:enseignement.be" ) || // Records from Respel.
                    entries[ i ].startsWith( "oai:diapason.bibl.ulaval.ca" ) || // Records from Diapason.
                    entries[ i ].startsWith( "oai:canalsavoir.ca" ) ) // Records from Canal Savoir.
                { 
                    return( false ); 
                }
            }
        }
         
        String[] sources = getClassificationSources( (Element)node ); 
        if( sources != null ) {
            for( int i = 0; i < sources.length; i++ ) {
                if( sources[ i ].equals( "http://eureka.ntic.org/vdex/respel.xml" ) || //Records from Respel.
                    sources[ i ].equals( "http://www.oclc.org/dewey/" ) ) // Records from Canal-U.
                {
                    return( false ); 
                }
            }
        }
 
        return( true );
    }

    private String[] getMetaMetadataIdentifierEntries(Element element) {
        NodeList metaMetadataNodes = element.getElementsByTagNameNS("http://ltsc.ieee.org/xsd/LOM", "metaMetadata");
        if( metaMetadataNodes.getLength() == 1 ) {
            Element metaMetadataElement = (Element)metaMetadataNodes.item( 0 );
            NodeList entryNodes = metaMetadataElement.getElementsByTagNameNS("http://ltsc.ieee.org/xsd/LOM", "entry");
            if( entryNodes.getLength() > 0 ) {
                String[] entries = new String[ entryNodes.getLength() ];
                for( int i = 0; i < entryNodes.getLength(); i++ ) {
                    Node entryNode = entryNodes.item( i );
                    entries[ i ] = entryNode.getTextContent().trim();
                }
                return( entries );
            }
        }
        return( null );
    }

    public String[] getClassificationSources(Element element) {
        ArrayList<String> sources = new ArrayList<String>();

        NodeList classificationNodes = element.getElementsByTagNameNS( "http://ltsc.ieee.org/xsd/LOM", "classification" );
        for( int c = 0; c < classificationNodes.getLength(); c++ ) {
            Element classificationElement = (Element)classificationNodes.item( c );
            NodeList sourceNodes = classificationElement.getElementsByTagNameNS( "http://ltsc.ieee.org/xsd/LOM", "source");
            for( int s = 0; s < sourceNodes.getLength(); s++ ) {
                Element sourceElement = (Element)sourceNodes.item( s );
                NodeList stringNodes = sourceElement.getElementsByTagNameNS( "http://ltsc.ieee.org/xsd/LOM", "string" );
                for( int str = 0; str < stringNodes.getLength(); str++ ) {
                    Node stringNode = stringNodes.item( str );
                    sources.add( stringNode.getTextContent().trim() );
                }
            }
        }

        if( sources.size() == 0 )
            return( null );

        String[] res = new String[ sources.size() ];
        for( int i = 0; i < res.length; i++ ) 
            res[ i ] = sources.get( i );
        return( res );
    }

}

