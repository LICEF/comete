package ca.licef.comete.vocabularies;

/* CVS $Id: $ */
 
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
 
/**
 * Vocabulary definitions from comete.rdf 
 * @author Auto-generated by schemagen on 25 nov. 2013 14:52 
 */
public class COMETE {
    /** <p>The ontology model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://comete.licef.ca/reference#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    /** <p>alternative homepage</p> */
    public static final ObjectProperty altHomepage = m_model.createObjectProperty( "http://comete.licef.ca/reference#altHomepage" );
    
    /** <p>alternative image</p> */
    public static final ObjectProperty altImg = m_model.createObjectProperty( "http://comete.licef.ca/reference#altImg" );
    
    /** <p>alternative logo</p> */
    public static final ObjectProperty altLogo = m_model.createObjectProperty( "http://comete.licef.ca/reference#altLogo" );
    
    /** <p>describes learning object</p> */
    public static final ObjectProperty describes = m_model.createObjectProperty( "http://comete.licef.ca/reference#describes" );
    
    /** <p>Link to local Store persistent digital object identifier</p> */
    public static final DatatypeProperty storeDigitalObject = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#storeDigitalObject" );
    
    /** <p>an associated metadata record</p> */
    public static final ObjectProperty hasMetadataRecord = m_model.createObjectProperty( "http://comete.licef.ca/reference#hasMetadataRecord" );
    
    /** <p>uri of a vocabulary context</p> */
    public static final ObjectProperty vocUri = m_model.createObjectProperty( "http://comete.licef.ca/reference#vocUri" );

    /** <p>to be completed</p> */
    public static final ObjectProperty vocConceptLinkingPredicate = m_model.createObjectProperty( "http://comete.licef.ca/reference#vocConceptLinkingPredicate" );


    /** <p>insertion date into the system</p> */
    public static final DatatypeProperty added = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#added" );
    
    /** <p>alternative family name</p> */
    public static final DatatypeProperty altFamilyName = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#altFamilyName" );
    
    /** <p>alternative formatted postal address</p> */
    public static final DatatypeProperty altFormattedAddress = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#altFormattedAddress" );
    
    /** <p>alternative given name</p> */
    public static final DatatypeProperty altGivenName = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#altGivenName" );
    
    /** <p>alternative mail box</p> */
    public static final ObjectProperty altMbox = m_model.createObjectProperty( "http://comete.licef.ca/reference#altMbox" );
    
    /** <p>alternative name</p> */
    public static final DatatypeProperty altName = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#altName" );
    
    /** <p>alternative phone (tel or fax)</p> */
    public static final ObjectProperty altPhone = m_model.createObjectProperty( "http://comete.licef.ca/reference#altPhone" );
    
    /** <p>metadata application profile</p> */
    public static final DatatypeProperty applicationProfile = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#applicationProfile" );
    
    /** <p>extra unqualified information</p> */
    public static final DatatypeProperty educationalLevel = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#educationalLevel" );
    
    /** <p>extra unqualified information</p> */
    public static final DatatypeProperty extraInfo = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#extraInfo" );
    
    /** <p>formatted postal address</p> */
    public static final DatatypeProperty formattedAddress = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#formattedAddress" );
    
    /** <p>significative word describing the object</p> */
    public static final DatatypeProperty intellectualProperty = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#intellectualProperty" );

    /** <p>significative word describing the object</p> */
    public static final DatatypeProperty keyword = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#keyword" );

    /** <p>significative word describing the object</p> */
    public static final ObjectProperty learningResourceType = m_model.createObjectProperty( "http://comete.licef.ca/reference#learningResourceType" );

    /** <p>significative word describing the object</p> */
    public static final ObjectProperty metadataFormat  = m_model.createObjectProperty( "http://comete.licef.ca/reference#metadataFormat" );

    /** <p>Whether a specific kind of notification is enabled or not</p> */
    public static final DatatypeProperty notificationEnabled = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#notificationEnabled" );

    /** <p>Link to original data.</p> */
    public static final DatatypeProperty originalDataLink = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#originalDataLink" );

    /** <p>Type of repository like OAI or HTML</p> */
    public static final ObjectProperty repository = m_model.createObjectProperty( "http://comete.licef.ca/reference#repository" );

    /** <p>Type of repository like OAI or HTML</p> */
    public static final DatatypeProperty repoType = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#repoType" );
    
    /** <p>Flag of the resource specifying whether a resource is inactive, invalid, pending, has broken link, etc.</p> */
    public static final DatatypeProperty flag = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#flag" );
    
    /** <p>modification date of object into the system</p> */
    public static final DatatypeProperty updated = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#updated" );
    
    /** <p>Whether a specific validation is enabled or not</p> */
    public static final DatatypeProperty validationEnabled = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#validationEnabled" );

    /** <p>Link to a validation report for a specific application profile</p> */
    public static final DatatypeProperty validationReportLink = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#validationReportLink" );

    /** <p>Alias corresponding to the vocabulary</p> */
    public static final DatatypeProperty vocAlias = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocAlias" );

    /** <p>to be completed</p> */
    public static final DatatypeProperty vocConceptUriIdSeparator = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocConceptIdSeparator" );

    /** <p>to be completed</p> */
    public static final DatatypeProperty vocConceptUriPrefix = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocConceptUriPrefix" );

    /** <p>to be completed</p> */
    public static final DatatypeProperty vocConceptUriSuffix = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocConceptUriSuffix" );

    /** <p>ID for internal management</p> */
    public static final DatatypeProperty vocId = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocId" );
    
    /** <p>Usable vocabulary for thematic navigation</p> */
    public static final DatatypeProperty vocNavigable = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocNavigable" );

    /** <p>Location of the related vocabulary</p> */
    public static final DatatypeProperty vocSourceLocation = m_model.createDatatypeProperty( "http://comete.licef.ca/reference#vocSourceLocation" );

    /** <p>to be completed</p> */
    public static final ObjectProperty vocLocalURL = m_model.createObjectProperty( "http://comete.licef.ca/reference#vocLocalURL" );

    /** <p>class of identities</p> */
    public static final OntClass Identity = m_model.createClass( "http://comete.licef.ca/reference#Identity" );
    
    /** <p>class of learning objects</p> */
    public static final OntClass LearningObject = m_model.createClass( "http://comete.licef.ca/reference#LearningObject" );
    
    /** <p>class of metadata records</p> */
    public static final OntClass MetadataRecord = m_model.createClass( "http://comete.licef.ca/reference#MetadataRecord" );
    
    /** <p>class of comete objects</p> */
    public static final OntClass Object = m_model.createClass( "http://comete.licef.ca/reference#Object" );
    
    /** <p>class of organizations</p> */
    public static final OntClass Organization = m_model.createClass( "http://comete.licef.ca/reference#Organization" );
    
    /** <p>class of persons</p> */
    public static final OntClass Person = m_model.createClass( "http://comete.licef.ca/reference#Person" );
    
    /** <p>class of repositories</p> */
    public static final OntClass Repository = m_model.createClass( "http://comete.licef.ca/reference#Repository" );
    
    /** <p>class of vocabularies</p> */
    public static final OntClass VocContext = m_model.createClass( "http://comete.licef.ca/reference#VocContext" );
    
}
