package ca.licef.comete.core.util;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 8-Sep-2011
 */
public class Constants {

    public static final String[] INDEX_LANGUAGES = { "en", "fr", "es" };

    public static final String[] UI_LANGUAGES = { "en", "fr" };

    //Path
    public static final String QUERY_BASE_LOCATION = "/queries";

    //General
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public static final String XSD_DATE_TYPE = "<http://www.w3.org/2001/XMLSchema#date>";
    
    //Standards
    public static final String OAI_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    public static final String IEEE_LOM_NAMESPACE = "http://ltsc.ieee.org/xsd/LOM";
    public static final String VDEX_NAMESPACE = "http://www.imsglobal.org/xsd/imsvdex_v1p0";


    public static final String OAI_ID = "http://www.openarchives.org/OAI/2.0/oai-identifier";

    //Mimetypes
    public static final String VCARD_MIMETYPE = "text/x-vcard";
    public static final String TEXT_MIMETYPE = "text/plain";
    static final String[] _MIMETYPE_CATEGORIES = new String[] {
            "application", "audio", "image", "text", "video"
    };
    public static final List MIMETYPE_CATEGORIES = Arrays.asList(_MIMETYPE_CATEGORIES);

    //********************
    // Object Types (URI)
    //********************
    public static final String OBJ_TYPE_LEARNING_OBJECT = "learningobject";
    public static final String OBJ_TYPE_METADATA_RECORD = "metadatarecord";
    public static final String OBJ_TYPE_VOC_CONTEXT = "voccontext";
    
    //********
    // Fedora
    //********

    //Datastreams
    //public static final String DATASTREAM_DO_METADATA = "DC";
    //public static final String DATASTREAM_RELS_EXT = "RELS-EXT";
    public static final String DATASTREAM_DATA = "data";
    //public static final String DATASTREAM_INTERNAL_DATA = "internal_data";
    public static final String DATASTREAM_EXPOSED_DATA_LOM = "exposed_lom";
    public static final String DATASTREAM_EXPOSED_DATA_DC = "exposed_dc";
    //public static final String DATASTREAM_VDEX = "vdex";
    //public static final String DATASTREAM_SKOS = "skos";

    //Namespace Sets
    static final String[] _LITERAL_PREDICATES = new String[] {
        OAI_ID
//        LABEL, OEAF_IDENTIFIER, MLR_TITLE, OEAF_DESCRIPTION,
//        METAMODEL_VOCABULARY_ID, METAMODEL_VOCABULARY_SOURCE, METAMODEL_VOCABULARY_SOURCE_LOCATION, METAMODEL_VOCABULARY_GRAPH,
//        METAMODEL_VOCABULARY_NAVIGABLE, METAMODEL_VOCABULARY_ALIAS, SKOS_LABEL
    };

    static final String[] _FULL_TEXT_PREDICATES = new String[] {
//        LABEL, MLR_TITLE, OEAF_DESCRIPTION
    };

    static final String[] _DATE_PREDICATES = new String[] {
//        METAMODEL_ADDED
//        METAMODEL_ADDED, METAMODEL_UPDATED, METAMODEL_CREATED
    };

    public static final List LITERAL_PREDICATES = Arrays.asList(_LITERAL_PREDICATES);
    public static final List FULL_TEXT_PREDICATES = Arrays.asList(_FULL_TEXT_PREDICATES);
    public static final List DATE_PREDICATES = Arrays.asList(_DATE_PREDICATES);

}
