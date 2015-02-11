package ca.licef.comete.vocabulary;

import ca.licef.comete.core.Core;
import ca.licef.comete.store.Store;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.util.Util;
import licef.IOUtil;
import licef.StringUtil;
import licef.XMLUtil;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.RDF;
import licef.tsapi.vocabulary.SKOS;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

//import ca.licef.comete.core.Fedora;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-05-03
 */
public class VocabularyManager {

    public static final String VOC_EQUIVALENCE_GRAPH = "voc-equivalence";
    public static final String SKOS_ONTOLOGY_GRAPH = "skos-ontology";

    File vocabulariesSourceDir; //init vocabularies
    File vocabulariesDirConfig;
    TripleStore tripleStore;
    static ca.licef.comete.core.util.Util CoreUtil;

    public String addNewVocContext(String id, String uriPrefix, String uriSuffix, String linkingPredicate,
                                   String urlLocation, String fileName, InputStream uploadedInputStream) throws Exception{
        File vocDir = new File(vocabulariesDirConfig, id);
        if (vocDir.exists())
            return "Vocabulary with id '" + id + "' already exists.";

        //create dest folder
        IOUtil.createDirectory(vocDir.getAbsolutePath());

        String location = urlLocation;
        File contentFile = null;
        String storeId = Store.PATH_VOCABS + "/" + id;
        if (location == null || "".equals(location)) {
            if (fileName != null && !"".equals(fileName)) {
                location = fileName;
                //copy content
                File destFolder = new File(Store.getInstance().getLocation() + storeId);
                destFolder.mkdirs();
                contentFile = new File(destFolder, fileName);
                OutputStream os = new FileOutputStream(contentFile);
                IOUtil.copy(uploadedInputStream, os);
                uploadedInputStream.close();
                os.close();
            }
        }

        //checking content
        int format = Util.getVocabularyFormatFromLocation((contentFile != null)?contentFile.getAbsolutePath():location);
        if (format == -1) {
            IOUtil.deleteDirectory(vocDir);
            Store.getInstance().deleteDigitalObject(storeId);
            return "Not a VDEX or SKOS content.";
        }

        //descriptor creation
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("vocabulary");
        doc.appendChild(root);
        Element element = doc.createElement("id");
        org.w3c.dom.Text value = doc.createTextNode(id);
        element.appendChild(value);
        root.appendChild(element);
        element = doc.createElement("location");
        value = doc.createTextNode(location);
        element.appendChild(value);
        root.appendChild(element);
        if (!"".equals(uriPrefix)) {
            element = doc.createElement("conceptUriPrefix");
            value = doc.createTextNode(uriPrefix);
            element.appendChild(value);
            root.appendChild(element);
        }
        if (!"".equals(uriSuffix)) {
            element = doc.createElement("conceptUriSuffix");
            value = doc.createTextNode(uriSuffix);
            element.appendChild(value);
            root.appendChild(element);
        }
        if (!"".equals(linkingPredicate)) {
            element = doc.createElement("linkingPredicate");
            value = doc.createTextNode(linkingPredicate);
            element.appendChild(value);
            root.appendChild(element);
        }
        IOUtil.writeStringToFile(XMLUtil.getXMLString(root), new File(vocDir, "description.xml"));

        initVocabulary(id, false);

        return null;
    }

    public void updateVocContext(String uri) throws Exception {
        Tuple[] details = Vocabulary.getInstance().getVocContextDetails(uri);
        String vocId = details[0].getValue("vocId").getContent();
        System.out.println("Update of vocabulary: " + vocId);
        initVocabulary(vocId, true);
        //initRelationships(vocContextUri);
        System.out.println("Update done.");
    }

    public String modifyVocabularyContent(String uri, String fileName, InputStream uploadedInputStream) throws Exception {
        Tuple[] details = Vocabulary.getInstance().getVocContextDetails(uri);
        String location = details[0].getValue("location").getContent();
        String vocId = details[0].getValue("vocId").getContent();

        File vocDir = new File(vocabulariesDirConfig, vocId);

        String[] vals = StringUtil.split(location, '/');
        String contentFilename = vals[vals.length - 1];

        File tmpContentFile = new File(vocDir, contentFilename + "_tmp");
        OutputStream os = new FileOutputStream(tmpContentFile);
        IOUtil.copy(uploadedInputStream, os);
        uploadedInputStream.close();
        os.close();

        //content checking
        int format = Util.getVocabularyFormatFromLocation(tmpContentFile.getAbsolutePath());
        if (format == -1) {
            tmpContentFile.delete();
            return "Not a VDEX or SKOS content.";
        }

        FileInputStream fisTmp = new FileInputStream(tmpContentFile);
        File content = new File(vocDir, contentFilename);
        FileInputStream fis = new FileInputStream(content);
        boolean isSameContent = DigestUtils.shaHex(fisTmp).equals(DigestUtils.shaHex(fis));
        fisTmp.close();
        fis.close();
        if (isSameContent) {
            tmpContentFile.delete();
            return "Identical content.";
        }
        else {
            content.delete();
            File newContent = new File(vocDir, fileName);
            tmpContentFile.renameTo(newContent);

            //change description
            String newLocation = "/" + vocId + "/" + fileName;
            File descr = new File(vocDir, "description.xml");
            String xmlDescr = XMLUtil.getXMLString( XMLUtil.getXMLNode(descr) );
            Hashtable t = new Hashtable();
            t.put(location, newLocation);
            xmlDescr = XMLUtil.substituteXMLContent(xmlDescr, "//location/text()", t, false);
            IOUtil.writeStringToFile(xmlDescr, descr);

            tripleStore.updateObjectTriple(uri, COMETE.vocSourceLocation, location, newLocation);

            updateVocContext(uri);
        }

        return null;
    }

    public boolean deleteVocContext(String uri) throws Exception {
        if (isVocabularyUsed(uri))
            return false;

        Tuple[] details = Vocabulary.getInstance().getVocContextDetails(uri);
        String vocUri = details[0].getValue("vocUri").getContent();
        String vocId = details[0].getValue("vocId").getContent();

        //triple store deletion
        tripleStore.removeResource(uri);
        tripleStore.clear_textIndex(vocUri);

        //physical deletion
        File vocDir = new File(vocabulariesDirConfig, vocId);
        IOUtil.deleteDirectory(vocDir);

        return true;
    }

    public boolean isVocabularyUsed(String uri) throws Exception {
        String query = CoreUtil.getQuery("vocabulary/getLOsUsingVoc.sparql", uri);
        Tuple[] res = tripleStore.sparqlSelect(query);
        return res.length > 0;
    }

    public void initVocabularyModule() throws Exception {
        System.out.println("Vocabulary Module initialization...");

        vocabulariesDirConfig = new File(Core.getInstance().getCometeHome(), "/conf/vocabularies");
        tripleStore = Core.getInstance().getTripleStore();

        //init SKOS ontology
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore",
                "loadContent", new Object[]{ getClass().getResourceAsStream("/vocabularies/skos.rdf"),
                    licef.tsapi.Constants.RDFXML, new String[]{SKOS_ONTOLOGY_GRAPH}} );
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);

        //vocs definition folder
        if (vocabulariesSourceDir == null)
            vocabulariesSourceDir = new File(getClass().getResource("/vocabularies").getFile());

        //copy initial vocabularies into COMETE conf folder
        if (!vocabulariesDirConfig.exists())
            IOUtil.createDirectory(vocabulariesDirConfig.getAbsolutePath());

        String[] initVocs = vocabulariesSourceDir.list();
        if (initVocs != null) {
            for (String voc : initVocs) {
                File srcVocDir = new File(vocabulariesSourceDir, voc);
                if (!srcVocDir.isDirectory())
                    continue;
                File descriptor = new File(srcVocDir, "description.xml");
                String id = XMLUtil.getSubXML(new InputSource(new FileInputStream(descriptor)), "//id/text()")[0];
                String location = XMLUtil.getSubXML(new InputSource(new FileInputStream(descriptor)), "//location/text()")[0];
                File destVocConf = new File(vocabulariesDirConfig, id);
                if (!destVocConf.exists()) {
                    IOUtil.copyFiles(descriptor, new File(destVocConf, "description.xml"));
                    if (!location.startsWith("http")) {
                        File vocab = new File(vocabulariesSourceDir, voc + "/" + location);
                        Store.getInstance().setDatastream(Store.PATH_VOCABS + "/" + id, location, vocab);
                    }
                }
            }
        }

        //loop on predefined vocabularies
        ArrayList<String> newUris = new ArrayList<>();
        String[] vocs = vocabulariesDirConfig.list();
        if (vocs != null) {
            for (String voc : vocs) {
                Invoker invk = new Invoker(this, "ca.licef.comete.vocabulary.VocabularyManager",
                        "initVocabulary", new Object[]{voc, false});
                String uri = (String) tripleStore.transactionalCall(invk, TripleStore.WRITE_MODE);
                if (uri != null)
                    newUris.add(uri);
            }
        }

        /*if (!newUris.isEmpty()) {
            //relationships
            for (String uri : newUris)
                initRelationships(uri);
        }*//*
*/
        System.out.println("Vocabulary Module initialization done.");
    }

    public String initVocabulary(String vocId, boolean forceUpdate) throws Exception {
        File vocDir = new File(vocabulariesDirConfig, vocId);
        if (!vocDir.isDirectory())
            return null;

        File descriptor = new File(vocDir, "description.xml");
        String id = null;
        String location = null;
        boolean navigableFlag = false;
        String conceptUriPrefix = null;
        String conceptUriSuffix = null;
        String conceptLinkingPredicate = DCTERMS.subject.getURI();
        ArrayList<String> aliases = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setCoalescing(true); //convert CDATA node to Text node
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(descriptor);
        Element top = document.getDocumentElement();
        NodeList childNodesList = top.getChildNodes();
        for (int i = 0; i < childNodesList.getLength(); i++) {
            Node node = childNodesList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String value = e.getFirstChild().getNodeValue().trim();
                if ("id".equals(e.getTagName()))
                    id = value;
                if ("location".equals(e.getTagName()))
                    location = value;
                if ("navigable".equals(e.getTagName()))
                    navigableFlag = "true".equals(value);
                if ("conceptUriPrefix".equals(e.getTagName()))
                    conceptUriPrefix = value;
                if ("conceptUriSuffix".equals(e.getTagName()))
                    conceptUriSuffix = value;
                if ("conceptLinkingPredicate".equals(e.getTagName()))
                    conceptLinkingPredicate = value;
                if ("alias".equals(e.getTagName()))
                    aliases.add(value);
            }
        }

        boolean isNew = false;
        String uri = null;
        String vocUri = null;
        String query = CoreUtil.getQuery("vocabulary/getVocContext.sparql", id);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0) {
            uri = tuples[0].getValue("s").getContent();
            vocUri = tuples[0].getValue("vocUri").getContent(); //reset
        }

        if( uri == null ) {
            uri = CoreUtil.makeURI(vocId, COMETE.VocContext);
            tripleStore.insertTriple(new Triple(uri, RDF.type, COMETE.VocContext));
            tripleStore.insertTriple(new Triple(uri, COMETE.vocId, id));
            tripleStore.insertTriple(new Triple(uri, COMETE.vocSourceLocation, location));
            tripleStore.insertTriple(new Triple(uri, COMETE.vocNavigable, Boolean.toString(navigableFlag)));
            tripleStore.insertTriple(new Triple(uri, COMETE.vocConceptLinkingPredicate, conceptLinkingPredicate));
            if (conceptUriPrefix != null)
                tripleStore.insertTriple(new Triple(uri, COMETE.vocConceptUriPrefix, conceptUriPrefix));
            if (conceptUriSuffix != null)
                tripleStore.insertTriple(new Triple(uri, COMETE.vocConceptUriSuffix, conceptUriSuffix));
            for (String alias : aliases)
                tripleStore.insertTriple(new Triple(uri, COMETE.vocAlias, alias));

            isNew = true;
        }

        //content management
        if (isNew || forceUpdate)
            initVocabularyContent(uri, location, id, vocUri, !isNew);

        return isNew?uri:null;
    }

    private void initVocabularyContent(String uri, String location, String vocId, String vocUri, boolean cleanFirst) throws Exception {
        //remove voc graph
        if (cleanFirst) {
            Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(uri, COMETE.vocUri);
            tripleStore.removeTriples(Arrays.asList(triples));
            tripleStore.clear_textIndex(vocUri);
        }

        String vocContent;
        String skosContent;

        //content retrieving
        if (IOUtil.isURL(location))
            vocContent = IOUtil.readStringFromURL(new URL(location));
        else
            vocContent = Store.getInstance().getDatastream(Store.PATH_VOCABS +"/" + vocId, location);

        String storeId = Store.PATH_VOCABS + "/" + vocId;
        int format = Util.getVocabularyFormat(vocContent);
        switch (format) {
            case Util.VDEX_FORMAT : //keep vdex version for history
                Store.getInstance().setDatastream(storeId, vocId + ".vdex", vocContent);
                tripleStore.insertTriple(new Triple(uri, COMETE.vocLocalURL,
                        Core.getInstance().getCometeUrl() + "/" + CoreUtil.getRestUrl(SKOS.ConceptScheme) + "/" +
                                vocId + "/vdex"));
                skosContent = convertVdexToSkos(vocContent);
                break;
            default:
                skosContent = vocContent;
        }

        Store.getInstance().setDatastream(Store.PATH_VOCABS + "/" + vocId, vocId + ".skos", skosContent);
        tripleStore.insertTriple(new Triple(uri, COMETE.vocLocalURL,
                Core.getInstance().getCometeUrl() + "/" + CoreUtil.getRestUrl(SKOS.ConceptScheme) + "/" +
                        vocId + "/skos"));

        //force reset to catch external vocUris inside vocab
        Hashtable attributes = XMLUtil.getAttributes(skosContent, "//skos:ConceptScheme");
        String newVocUri = attributes.get("about").toString();
        tripleStore.updateObjectTriple(uri, COMETE.vocUri, vocUri, newVocUri);
        vocUri = newVocUri;

        //load content (with index even for non-navigable vocs, for possible future switch to)
        tripleStore.loadContent_textIndex(new ByteArrayInputStream(skosContent.getBytes()),
                licef.tsapi.Constants.RDFXML, vocUri);

        //Generation of inferred triples (for all vocs, navigable or not)
        tripleStore.doInference(SKOS_ONTOLOGY_GRAPH, vocUri);
    }

    //Relationships management
    /* private void initRelationships(String uri) throws Exception {
        String location = tripleStore.getTriplesWithSubjectPredicate(uri,
                Constants.METAMODEL_VOCABULARY_SOURCE_LOCATION)[0].getObject();
        XSLTUtil.initVocUris();
        File rels = getRelationships(location);
        if (rels != null) {
            tripleStore.loadRDFContent(rels.toURI().toString(), VOC_EQUIVALENCE_GRAPH);
            tripleStore.applyRules(skosRlogFile.toURI().toString(), VOC_EQUIVALENCE_GRAPH);
        }
    }

    /*private File getRelationships(String location) throws Exception {
        String voc = location.split("/")[1];
        File relDescr = new File(vocabulariesDir, voc + "/relationships.rdf");

        if (!relDescr.exists()) {
            String vocContent = "";
            URL sourceUrl;
            if (IOUtil.isURL(location)) {
                sourceUrl = new URL( location );
                vocContent = IOUtil.readStringFromURL(sourceUrl);
            }
            else
                vocContent = IOUtil.readStringFromFile(new File(vocabulariesDir, location));

            relDescr = null;
            String skosContent = "";
            int format = Util.getVocabularyFormat(vocContent);
            switch (format) {
                case Util.VDEX_FORMAT : //keep vdex version for history
                    skosContent = convertVdexRelationshipsToSkos(vocContent);
                    break;
            }
            if (skosContent.contains("Match")) {
                relDescr = new File(System.getProperty("java.io.tmpdir"), "relationships.rdf");
                IOUtil.writeStringToFile(skosContent, relDescr);
            }
        }

        return relDescr;
    }*/

    public String convertVdexToSkos( String vdexContent ) throws Exception {
        StreamSource source = new StreamSource( new ByteArrayInputStream( vdexContent.getBytes() ) );
        return( CoreUtil.applyXslToDocument( "vocabulary/convertVDEXToSKOS", source ) );
    }

    public String convertVdexRelationshipsToSkos( String vdexContent ) throws Exception {
        StreamSource source = new StreamSource( new ByteArrayInputStream( vdexContent.getBytes() ) );
        return( CoreUtil.applyXslToDocument( "vocablary/convertVDEXRelationshipsToSKOS", source ) );
    }

    /*boolean hasVocabularyChanged(String id, String location) throws Exception {
        Fedora fedora = Core.getInstance().getFedora();
        String vocContent;
        URL sourceUrl;

        if (IOUtil.isURL(location)) {
            sourceUrl = new URL( location );
            vocContent = IOUtil.readStringFromURL(sourceUrl);
        }
        else
            vocContent = IOUtil.readStringFromFile(new File( vocabulariesDir, location));

        int format = Util.getVocabularyFormat(vocContent);
        String locationFormat = Constants.DATASTREAM_SKOS;
        if (format == Util.VDEX_FORMAT)
            locationFormat = Constants.DATASTREAM_VDEX;

        //current voc datastream
        String previousVocContent = fedora.getDatastream(id, locationFormat);

        return !DigestUtils.shaHex(previousVocContent).equals(DigestUtils.shaHex(vocContent));
    }  */

    /*public ArrayList<String> getVocsWithAvailableUpdate() throws Exception {
        ArrayList<String> vocsWithUpdate = new ArrayList<String>();

        Hashtable<String, String>[] results = tripleStore.getResults("getVocContextsDetails.sparql");

        for (Hashtable<String, String> result : results) {
            if ( hasVocabularyChanged(result.get("doId"), Vocabulary.CoreUtil.manageQuotes(result.get("location"))) )
                vocsWithUpdate.add(result.get("s"));
        }
        return vocsWithUpdate;
    } */
}
