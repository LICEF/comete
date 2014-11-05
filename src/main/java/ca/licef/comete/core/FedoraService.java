package ca.licef.comete.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
//import ca.licef.comete.core.util.Constants;
//import ca.licef.comete.core.util.Triple;
//import ca.licef.comete.core.util.Util;
//import com.yourmediashelf.fedora.client.FedoraClient;
//import com.yourmediashelf.fedora.client.FedoraClientException;
//import com.yourmediashelf.fedora.client.FedoraCredentials;
//import com.yourmediashelf.fedora.client.request.*;
//import com.yourmediashelf.fedora.client.response.*;
//import licef.CommonNamespaceContext;
//import licef.IOUtil;
//import licef.StreamUtil;
//import licef.XMLUtil;
import org.fcrepo.client.FedoraException;
import org.fcrepo.client.FedoraObject;
import org.fcrepo.client.FedoraRepository;
import org.fcrepo.client.impl.FedoraRepositoryImpl;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.xml.sax.InputSource;
//import org.xml.sax.SAXException;
//
//import javax.servlet.http.HttpServletResponse;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.StringReader;
//import java.net.MalformedURLException;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.Hashtable;
//import java.util.List;

public class FedoraService {

    private String url;
    private String username;
    private String password;

    private FedoraRepository repo;

    public FedoraRepository getRepository() {
        if( repo == null )
            repo = new FedoraRepositoryImpl( url + "/rest", username, password );
        return( repo );
    }

    //public FedoraClient getClient() {
    //    if( fc == null ) {
    //        try {
    //            FedoraCredentials credentials = new FedoraCredentials(getUrl(), getUsername(), getPassword());
    //            fc = new FedoraClient( credentials );
    //        } catch (MalformedURLException e) {
    //            e.printStackTrace();
    //        }
    //    }
    //    return( fc );
    //}
    
    public void setUrl( String url ) {
        this.url = url;
    }

    public String getUrl() {
        return( url );
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getUsername() {
        return( username );
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public String getPassword() {
        return( password );
    }

    //public String getPidNamespace() {
    //    return( Core.getInstance().getFedoraPidNamespace() );
    //}

    ///*
    // * Digital Objects
    // */

    //public String createDigitalObject( String label, String logMessage ) throws FedoraClientException {
    //    String objXML = null;
    //    try {
    //        objXML = createDigitalObjectXML(null, label);
    //    }
    //    catch( Exception e ) {
    //        throw( new FedoraClientException( "Cannot create DO's XML for record labeled: " + label, e ) );
    //    }

    //    return ingestDigitalObject(objXML, logMessage);
    //}

    public String createDigitalObject() throws FedoraException {
        //FedoraObject object = getRepository().createObject( "/", true );
        //System.out.println( "Object created. Name=" + object.getName() +" Path=" + object.getPath() + " Size=" + object.getSize() );
        //Iterator<Triple> properties = object.getProperties();
        //for( ; properties.hasNext(); ) {
        //    Triple property = properties.next();
        //    System.out.println( "Prop: " + property );
        //}
        //return( object.getName() );
        return( null );
    }

    public String createDigitalObject( String path ) throws FedoraException {
        FedoraObject object = getRepository().createObject( path );
        //System.out.println( "Object created. Name=" + object.getName() +" Path=" + object.getPath() + " Size=" + object.getSize() );
        //Iterator<Triple> properties = object.getProperties();
        //for( ; properties.hasNext(); ) {
        //    Triple property = properties.next();
        //    System.out.println( "Prop: " + property );
        //}
        return( object.getName() );
    }

    /**
     * @param contentType Possible values are "application/rdf+xml", "text/turtle", etc.
     */
    public String ingestDigitalObject( String path, String rdf, String contentType ) throws FedoraException {
        //FedoraObject object = getRepository().createObject( "/", true );
        //InputStream is = new ByteArrayInputStream( rdf.getBytes( StandardCharsets.UTF_8 ) );
        //object.updateProperties( is, contentType ); 
        //return( object.getName() );
        return( null );
    }

    //public String ingestDigitalObject( String foxml, String logMessage ) throws FedoraClientException {
    //    Ingest req = new Ingest();
    //    req.format( Constants.FOXML_1_1 );
    //    req.content( foxml );
    //    req.logMessage( logMessage );

    //    IngestResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();
    //    if( status != HttpServletResponse.SC_CREATED )
    //        return( null );
    //    return( resp.getPid() );
    //}

    //public void modifyDigitalObjectState( String objId, String state, String logMessage ) throws FedoraClientException {
    //    if (objId.startsWith("info:fedora/"))
    //        objId = objId.substring("info:fedora/".length());

    //    ModifyObject req = new ModifyObject( objId );
    //    if (logMessage != null)
    //        req.logMessage( logMessage );
    //    req.state(state);
    //    FedoraResponse resp = req.execute( getClient() );
    //}

    //public void purgeDigitalObject( String objId ) throws FedoraClientException {
    //    purgeDigitalObject( objId, null );
    //}

    //public void purgeDigitalObject( String objId, String logMessage ) throws FedoraClientException {
    //    if (objId.startsWith("info:fedora/"))
    //        objId = objId.substring("info:fedora/".length());

    //    PurgeObject req = new PurgeObject( objId );
    //    if (logMessage != null)
    //        req.logMessage( logMessage );
    //    FedoraResponse resp = req.execute( getClient() );
    //}

    //String createDigitalObjectXML(String pid, String label) throws Exception {
    //    StringBuffer xml = new StringBuffer();
    //    pid = encodePid(pid);

    //    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    //    xml.append("<foxml:digitalObject xmlns:xsi=\"" + org.fcrepo.common.Constants.XSI.uri + "\"\n");
    //    xml.append("       xmlns:foxml=\"" + org.fcrepo.common.Constants.FOXML.uri + "\"\n");
    //    xml.append("       xsi:schemaLocation=\"" + org.fcrepo.common.Constants.FOXML.uri  + " " + org.fcrepo.common.Constants.FOXML1_1.xsdLocation + "\"");
    //    xml.append("       VERSION=\"1.1\" PID=\"" + pid + "\">\n");
    //    xml.append("  <foxml:objectProperties>\n");
    //    xml.append("    <foxml:property NAME=\"" + org.fcrepo.common.Constants.MODEL.LABEL.uri + "\" VALUE=\"" + StreamUtil.enc(label) + "\"/>\n");
    //    xml.append("    <foxml:property NAME=\"" + org.fcrepo.common.Constants.MODEL.OWNER.uri + "\" VALUE=\"" + username + "\"/>");
    //    xml.append("  </foxml:objectProperties>\n");

    //    xml.append("</foxml:digitalObject>");

    //    return xml.toString();
    //}

    //private String encodePid(String pid) throws Exception {
    //    if(pid == null || pid.equals("")) {
    //        pid = getNextPID();
    //    }
    //    return StreamUtil.enc(pid);
    //}
    //
    //public String getNextPID() throws FedoraClientException {
    //    return getClient().getNextPID().execute(getClient()).getPid();
    //}

    //public Date getLastDSModificationDate(String id, String dataStream) throws FedoraClientException {
    //    if( id.startsWith( "info:fedora/" ) )
    //        id = id.substring( "info:fedora/".length() );
    //    return getClient().getLastModifiedDate(id);
    //}

    //public void updateDigitalObjectLabel( String id, String label, String logMessage ) throws Exception {
    //    if( id.startsWith( "info:fedora/" ) )
    //        id = id.substring( "info:fedora/".length() );
    //    GetDatastreamDissemination getDCObjectRecordReq = new GetDatastreamDissemination( id, Constants.DATASTREAM_DO_METADATA );
    //    FedoraResponse resp = getDCObjectRecordReq.execute( getClient() );
    //    String dcObjectRecord = resp.getEntity( String.class );
    //    String updatedDcObjectRecord = updateFirstTitleInDCRecord( dcObjectRecord, label );
    //    modifyDatastream( id, Constants.DATASTREAM_DO_METADATA, updatedDcObjectRecord, logMessage );
    //}

    //private String updateFirstTitleInDCRecord( String xml, String newTitle ) throws ParserConfigurationException, SAXException, IOException {
    //    InputSource inputSource = new InputSource( new StringReader( xml ) );
    //    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    //    docFactory.setNamespaceAware( true );
    //    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    //    Document doc = docBuilder.parse( inputSource );
    //    NodeList dcElements = doc.getElementsByTagNameNS( CommonNamespaceContext.oaidcNSURI, "dc" );

    //    // Leave the record as is because it contains no dc elements.
    //    if( dcElements.getLength() == 0 )
    //        return( xml );

    //    Element dcElement = (Element)dcElements.item( 0 );
    //    NodeList titleElements = dcElement.getElementsByTagNameNS( CommonNamespaceContext.dcNSURI, "title" );
    //    Element newTitleElement = doc.createElementNS( CommonNamespaceContext.dcNSURI, "dc:title" );
    //    newTitleElement.appendChild( doc.createTextNode( newTitle ) );
    //    if( titleElements.getLength() == 0 )
    //        dcElement.appendChild( newTitleElement );
    //    else {
    //        Element firstTitleElement = (Element)titleElements.item( 0 );
    //        dcElement.replaceChild( newTitleElement, firstTitleElement );
    //    }

    //    return( XMLUtil.serialize( doc, true ) );
    //}


    ///*
    // * Metadata Formats
    // */

    //public String[] getRecordMetadataFormats( String id ) throws FedoraClientException, IOException {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    String queryName = "getMetadataFormats.sparql";
    //    String query = Util.getQuery( queryName, id );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "csv" );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String str = resp.getEntity( String.class );
    //    
    //    ArrayList<String> formats = new ArrayList<String>();

    //    BufferedReader reader = null;
    //    try {
    //        reader = new BufferedReader( new StringReader( str ) );
    //        String line;
    //        while( ( line = reader.readLine() ) != null ) {
    //            int indexOfLastSlash = line.lastIndexOf( "/" );
    //            if( indexOfLastSlash != -1 )
    //                formats.add( line.substring( indexOfLastSlash + 1 ) );
    //        }
    //    }
    //    finally { 
    //        reader.close();
    //    }

    //    return( formats.toArray( new String[ formats.size() ] ) );
    //}

    ///*
    // * Datastreams
    // */

    //public String getDatastream( String id, String dataStream ) throws FedoraClientException {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    FedoraResponse resp = new GetDatastreamDissemination( id, dataStream ).execute( getClient() );
    //    int status = resp.getStatus();
    //    if( status == HttpServletResponse.SC_OK ) {
    //        String value = resp.getEntity( String.class );
    //        return( value );
    //    }
    //    return( null );
    //}

    //public int addDatastream( String id, String dataStream, String dsLabel, boolean versionable, Object content,
    //                          String mimetype, String formatURI, String controlGroup, String logMessage) throws FedoraClientException {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    AddDatastream req = new AddDatastream(id, dataStream);
    //    req.dsLabel( dsLabel );
    //    if (content instanceof File) //Managed
    //        req.content( (File)content );
    //    else if (IOUtil.isURL((String)content)) //dsLocation Redirect or External
    //        req.dsLocation((String)content);
    //    else //inner content
    //        req.content( (String)content );
    //    req.versionable(versionable);
    //    req.mimeType( mimetype );
    //    if( formatURI != null )
    //        req.formatURI( formatURI );
    //    req.controlGroup( controlGroup );
    //    req.logMessage( logMessage );

    //    AddDatastreamResponse resp = req.execute( getClient() );
    //    return resp.getStatus();
    //}

    //public void modifyDatastream( String id, String dataStream, Object content, String logMessage ) throws FedoraClientException {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    ModifyDatastream req = new ModifyDatastream( id, dataStream );
    //    if (content instanceof File) //Managed
    //        req.content( (File)content );
    //    else if (IOUtil.isURL((String)content)) //dsLocation Redirect or External
    //        req.dsLocation((String)content);
    //    else //inner xml
    //        req.content( (String)content );  
    //    req.logMessage( logMessage );
    //    ModifyDatastreamResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();
    //}

    //public String modifyDatastream( String id, String dataStream, String dsLabel,
    //                                String formatURI, String dsLocation, String logMessage ) throws FedoraClientException, IOException {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    ModifyDatastream req = new ModifyDatastream( id, dataStream );
    //    req.dsLabel( dsLabel );
    //    req.formatURI( formatURI );
    //    req.dsLocation( dsLocation );
    //    req.logMessage( logMessage );
    //    req.checksumType( "DISABLED" );
    //    ModifyDatastreamResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();
    //    return( resp.getLastModifiedDate() + "" );
    //}

    //public void purgeDatastream(String id, String dataStream, String logMessage) throws Exception {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    PurgeDatastream req = new PurgeDatastream( id, dataStream );
    //    req.logMessage( logMessage );
    //    PurgeDatastreamResponse resp = req.execute( getClient() );
    //}

    //public boolean isDatastreamExists(String id, String dataStream) throws FedoraClientException, IOException {
    //    if (id.startsWith("info:fedora/"))
    //        id = id.substring("info:fedora/".length());

    //    String queryName = "getDatastreams.sparql";
    //    String query = Util.getQuery( queryName, id, dataStream );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "count" );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String strCount = resp.getEntity( String.class );
    //    int datastreamCount = 0;
    //    try {
    //        datastreamCount = Integer.parseInt( strCount );
    //    }
    //    catch( NumberFormatException e ) {
    //        throw( new IOException( "Cannot retrieve datastreamCount: " + strCount, e ) );
    //    }

    //    return( datastreamCount > 0 );
    //}


    ///*
    // * Relationships
    // */

    ///**
    // * Add of one relationship.
    // * If literal object id bigger than 5000 bytes, use addRelationships
    // * @param subject
    // * @param predicate
    // * @param object
    // * @param isLiteral
    // * @throws FedoraClientException
    // */
    //public void addRelationship(String subject, String predicate, String object, boolean isLiteral) throws FedoraClientException {
    //    addRelationship(subject, predicate, object, isLiteral, true);
    //}

    //public void addRelationship(String subject, String predicate, String object, boolean isLiteral, boolean useFedoraNS) throws FedoraClientException {
    //    if (subject.startsWith("info:fedora/"))
    //        subject = subject.substring("info:fedora/".length());
    //    if (!isLiteral && !object.startsWith("info:fedora/") && useFedoraNS)
    //        object = "info:fedora/" + object;

    //    AddRelationship req = new AddRelationship( subject );
    //    req.predicate( predicate );
    //    req.object( object );
    //    req.isLiteral( isLiteral );
    //    FedoraResponse resp = req.execute( getClient() );
    //}

    ///**
    // *
    // * @param id
    // * @param triples
    // * @param isRELS_EXT  true: retrieve of existent RELS-EXT datastream
    // *                    false: creation of new RELS-EXT datastream
    // *                    null : let the system check
    // */
    //public void addRelationships(String id, Triple[] triples, Boolean isRELS_EXT) throws Exception {
    //    addRelationships(id, triples, isRELS_EXT, true);
    //}

    ///**
    // *
    // * @param id
    // * @param triples
    // * @param isRELS_EXT  true: retrieve of existent RELS-EXT datastream
    // *                    false: creation of new RELS-EXT datastream
    // *                    null : let the system check
    // * @param useFedoraNS if true, check if "info:fedora/" prefixes target object
    // */
    //public void addRelationships(String id, Triple[] triples, Boolean isRELS_EXT, boolean useFedoraNS) throws Exception {
    //    boolean isRelsExt = (isRELS_EXT == null)?isDatastreamExists(id, "RELS-EXT"):isRELS_EXT;
    //    String relsExt = isRelsExt?getDatastream(id, "RELS-EXT"):Util.getNewRelsExtXml(id);
    //    Node root = XMLUtil.getXMLNode(relsExt);
    //    Document doc = root.getFirstChild().getOwnerDocument();
    //    Element descr = (Element)doc.getElementsByTagNameNS(CommonNamespaceContext.rdfNSURI, "Description").item(0);
    //    boolean isChanges = false;
    //    for (Triple t : triples) {
    //        String predicate = t.getPredicate();
    //        String object = t.getObject();
    //        if (!t.isLiteral() && !object.startsWith("info:fedora/") && useFedoraNS)
    //            object = "info:fedora/" + object;

    //        int i = predicate.lastIndexOf("#");
    //        if (i == -1)
    //            i = predicate.lastIndexOf("/");
    //        String ns = predicate.substring(0, i + 1);
    //        String tagname = predicate.substring(i + 1);
    //        if (isRelationAlreadyExists(descr, ns, tagname, object, t.isLiteral()))
    //            continue;
    //        Element newElement = doc.createElementNS(ns, tagname);
    //        if (t.isLiteral())
    //            newElement.setTextContent(object);
    //        else
    //            newElement.setAttribute("rdf:resource", object);
    //        descr.appendChild(newElement);
    //        isChanges = true;
    //    }
    //    if (!isChanges)
    //        return;
    //    
    //    String newRelsExt = XMLUtil.getXMLString(root);
    //    if (isRelsExt)
    //        modifyDatastream(id, "RELS-EXT", newRelsExt, "addRelationships");
    //    else
    //        addDatastream(id, "RELS-EXT", "Relationships", false, newRelsExt,
    //                "application/rdf+xml", "info:fedora/fedora-system:FedoraRELSExt-1.0",
    //                "X", "addRelationships");
    //}

    //boolean isRelationAlreadyExists(Element description, String ns, String tagName, String value, boolean isLiteral) {
    //    NodeList list = description.getElementsByTagNameNS(ns, tagName);
    //    if (list.getLength() > 0) {
    //        for (int i = 0; i < list.getLength(); i++) {
    //            Element e = (Element)list.item(i);
    //            if (isLiteral && value.equals(e.getTextContent()))
    //                return true;
    //            if (!isLiteral && value.equals(e.getAttributeNS(CommonNamespaceContext.rdfNSURI, "resource")))
    //                return true;
    //        }
    //    }
    //    return false;
    //}

    //public void purgeRelationship(String subject, String predicate, String object, boolean isLiteral) throws FedoraClientException {
    //    if (subject.startsWith("info:fedora/"))
    //        subject = subject.substring("info:fedora/".length());
    //    if (!isLiteral && !object.startsWith("info:fedora/"))
    //        object = "info:fedora/" + object;

    //    PurgeRelationship req = new PurgeRelationship( subject );
    //    req.predicate( predicate );
    //    req.object( object );
    //    req.isLiteral( isLiteral );
    //    FedoraResponse resp = req.execute( getClient() );
    //}

    ///**
    // * Purge of relationships of recird id where predicates starts with predicatePrefix
    // * @param id
    // * @param predicatePrefix
    // * @param except List of relation starting with predicatePrefix to keep alive.
    // *               Set <code>except</code> to <code>null</code> for disable it.  
    // * @throws Exception
    // */
    //public void purgeRelationsWithSubjectPredicateStartsWith(String id, String predicatePrefix, List except) throws Exception{
    //    boolean isRelsExt = isDatastreamExists(id, "RELS-EXT");
    //    if (!isRelsExt)
    //        return;

    //    String relsExt = getDatastream(id, "RELS-EXT");
    //    Node root = XMLUtil.getXMLNode(relsExt);
    //    Document doc = root.getFirstChild().getOwnerDocument();
    //    Element descr = (Element)doc.getElementsByTagNameNS(CommonNamespaceContext.rdfNSURI, "Description").item(0);
    //    if (descr == null) {
    //        purgeDatastream(id, "RELS-EXT", "purgeRelationships");
    //        return;
    //    }

    //    boolean isChanges = false;
    //    NodeList list = descr.getChildNodes();
    //    for (int i = 0; i < list.getLength(); i++) {
    //        Node node = list.item(i);
    //        if (! (node instanceof Element))
    //            continue;
    //        Element e = (Element)node;
    //        if (predicatePrefix.equals(e.getNamespaceURI())) {
    //            boolean ok = true;
    //            if (except != nul5be79c70-6370-11e4-9803-0800200c9a66l && except.contains(e.getNamespaceURI() + e.getTagName()))
    //                ok = false;
    //            if (ok) {
    //                descr.removeChild(e);
    //                isChanges = true;
    //            }
    //        }
    //    }
    //    if (!isChanges)
    //        return;

    //    list = descr.getChildNodes();
    //    int relationsCount = 0;
    //    for (int i = 0; i < list.getLength(); i++) {
    //        Node node = list.item(i);
    //        if (! (node instanceof Element))
    //            continue;
    //        relationsCount++;
    //    }
    //    if (relationsCount == 0)
    //        purgeDatastream(id, "RELS-EXT", "purgeRelationships");
    //    else {
    //        String newRelsExt = XMLUtil.getXMLString(root);
    //        modifyDatastream(id, "RELS-EXT", newRelsExt, "purgeRelationships");
    //    }
    //}

    //public String getDigitalObjectWithRelation(String predicate, String object, boolean isLiteral) throws Exception {
    //    return getDigitalObjectWithRelation(predicate, object, isLiteral, true);            
    //}

    //public String getDigitalObjectWithRelation(String predicate, String object, boolean isLiteral, boolean useFedoraNS) throws Exception {
    //    Triple[] triples = getRelationsWithPredicateObjectOld(predicate, object, isLiteral, useFedoraNS);
    //    if (triples.length > 0) {
    //        String subject = triples[0].getSubject();
    //        if (subject.startsWith("info:fedora/"))
    //            subject = subject.substring("info:fedora/".length());
    //        return subject;
    //    }
    //    return null;
    //}

    //public Triple[] getRelationsWithSubjectOld( String subject ) throws Exception {
    //    if (!subject.startsWith("info:fedora/"))
    //        subject = "info:fedora/" + subject;
    //    String queryName = "getTriplesWithSubject.sparql";
    //    String query = Util.getQuery( queryName, subject );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "json" );
    //    req.stream( true );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String json = Util.readString(resp.getEntityInputStream());
    //    return Util.getTriplesPO( subject, json );
    //}

    //public Triple[] getRelationsWithSubjectPredicateOld(String subject, String predicate ) throws Exception {
    //    if (subject.startsWith("info:fedora/"))
    //        subject = subject.substring("info:fedora/".length());

    //    String queryName = "getTriplesWithSubjectPredicate.sparql";
    //    String query = Util.getQuery( queryName, subject, predicate );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "json" );
    //    req.stream( true );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String json = Util.readString(resp.getEntityInputStream());
    //    return Util.getTriplesO( subject, predicate, json );
    //}

    //public boolean isRelationWithSubjectPredicate(String subject, String predicate ) throws Exception {
    //    if (subject.startsWith("info:fedora/"))
    //        subject = subject.substring("info:fedora/".length());

    //    String queryName = "getTriplesWithSubjectPredicate.sparql";
    //    String query = Util.getQuery( queryName, subject, predicate );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "count" );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String strCount = resp.getEntity( String.class );
    //    int count = 0;
    //    try {
    //        count = Integer.parseInt( strCount );
    //    }
    //    catch( NumberFormatException e ) {
    //        throw( new IOException( "Cannot retrieve count: " + strCount, e ) );
    //    }

    //    return( count > 0 );
    //}

    //public Triple[] getRelationsWithSubjectPredicateStartsWithOld(String subject, String predicatePrefix) throws Exception {
    //    if (subject.startsWith("info:fedora/"))
    //        subject = subject.substring("info:fedora/".length());

    //    String queryName = "getRelationsWithSubjectPredicateStartsWith.sparql";
    //    String query = Util.getQuery( queryName, subject, predicatePrefix );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "json" );
    //    req.stream( true );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String json = Util.readString(resp.getEntityInputStream());
    //    return Util.getTriplesPO( subject, json );
    //}

    //public Triple[] getRelationsWithPredicateObjectOld(String predicate, String object, boolean isLiteral, boolean useFedoraNS) throws Exception {
    //    if (isLiteral)
    //        object = "\"" + object + "\"";
    //    else {
    //        if (!object.startsWith("info:fedora/") && useFedoraNS)
    //            object = "info:fedora/" + object;
    //    }
    //    String object2 = isLiteral?object:"<" + object + ">";

    //    String queryName = "getTriplesWithPredicateObject.sparql";
    //    String query = Util.getQuery( queryName, predicate, object2 );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "json" );
    //    req.stream( true );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String json = Util.readString(resp.getEntityInputStream());
    //    return Util.getTriplesS( predicate, object, json );
    //}

    //public Triple[] getRelationsWithObjectOld(String object, boolean isLiteral) throws Exception {
    //    if (isLiteral)
    //        object = "\"" + object + "\"";
    //    else {
    //        if (!object.startsWith("info:fedora/"))
    //            object = "info:fedora/" + object;
    //    }
    //    String object2 = isLiteral?object:"<" + object + ">";

    //    String queryName = "getTriplesWithObject.sparql";
    //    String query = Util.getQuery( queryName, object2 );

    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "json" );
    //    req.stream( true );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String json = Util.readString(resp.getEntityInputStream());
    //    return Util.getTriple5be79c70-6370-11e4-9803-0800200c9a66sSP( object, json );
    //}


    ///*
    // * General Queries
    // */

    //public Hashtable<String, String> getFirstResult( String queryName, Object... params ) throws Exception {
    //    Hashtable<String, String>[] t = getResults( queryName, params);
    //    if (t.length == 0)
    //        return null;
    //    return t[0];
    //}

    //public Hashtable<String, String>[] getResults( String queryName, Object... params ) throws Exception {
    //    String query = Util.getQuery(queryName, params);
    //    RiSearch req = new RiSearch( query );
    //    req.flush( true );
    //    req.format( "json" );
    //    req.stream( true );
    //    RiSearchResponse resp = req.execute( getClient() );
    //    int status = resp.getStatus();

    //    String json = Util.readString(resp.getEntityInputStream());
    //    return Util.getResults( json );
    //}

    ///*
    // * Misc.
    // */

    ///**
    // * Relationship substitutions of object1 by object2
    // * @param object1 old ref object
    // * @param object2 new target object
    // * @param isLiteral
    // */
    //public void substituteObjectOld(String object1, String object2, boolean isLiteral) throws Exception {
    //    Triple[] triples = getRelationsWithObjectOld(object1, isLiteral);
    //    for (Triple triple : triples) {
    //        Core.getInstance().getFedoraService().purgeRelationship(triple.getSubject(), triple.getPredicate(), object1, isLiteral);
    //        Core.getInstance().getFedoraService().addRelationship(triple.getSubject(), triple.getPredicate(), object2, isLiteral);
    //    }
    //}

}

