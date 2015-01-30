package ca.licef.comete.vocabulary;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.util.Util;
import licef.IOUtil;
import licef.StringUtil;
import licef.XMLUtil;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.RDF;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    File vocabulariesDir;
    TripleStore tripleStore;
    static ca.licef.comete.core.util.Util CoreUtil;

    public String addNewVocContext(String name, String source, String cat,
                                   boolean navigableFlag, String urlLocation,
                                   String fileName, InputStream uploadedInputStream) throws Exception{
        File vocDir = new File(vocabulariesDir, name);
        if (vocDir.exists())
            return "Vocabulary with name '" + name + "' already exists.";

        //create dest folder
        IOUtil.createDirectory(vocDir.getAbsolutePath());

        String location = urlLocation;
        File contentFile = null;
        if (location == null || "".equals(location)) {
            if (fileName != null && !"".equals(fileName)) {
                location = "/" + name + "/" + fileName;
                //copy content
                contentFile = new File(vocDir, fileName);
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
            return "Not a VDEX or SKOS content.";
        }

        //descriptor creation
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("vocabulary");
        doc.appendChild(root);
        Element element = doc.createElement("source");
        org.w3c.dom.Text value = doc.createTextNode(source);
        element.appendChild(value);
        root.appendChild(element);
        element = doc.createElement("category");
        value = doc.createTextNode(cat);
        element.appendChild(value);
        root.appendChild(element);
        element = doc.createElement("location");
        value = doc.createTextNode(location);
        element.appendChild(value);
        root.appendChild(element);
        if (navigableFlag) {
            element = doc.createElement("navigable");
            value = doc.createTextNode("true");
            element.appendChild(value);
            root.appendChild(element);
        }
        IOUtil.writeStringToFile(XMLUtil.getXMLString(root), new File(vocDir, "description.xml"));

        initVocabulary(name, false);

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

        File vocDir = new File(vocabulariesDir, vocId);

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
        boolean navigable = Boolean.parseBoolean(details[0].getValue("navigable").getContent());

        //triple store deletion
        tripleStore.removeResource(uri);
        clearVocabularyGraph(vocUri, navigable);

        //physical deletion
        File vocDir = new File(vocabulariesDir, vocId);
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

        vocabulariesDir = new File(Core.getInstance().getCometeHome(), "/conf/vocabularies");
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
        if (!vocabulariesDir.exists())
            IOUtil.createDirectory(vocabulariesDir.getAbsolutePath());

        String[] initVocs = vocabulariesSourceDir.list();
        if (initVocs != null) {
            for (String voc : initVocs) {
                File destVoc = new File(vocabulariesDir, voc);
                if (!destVoc.exists())
                    IOUtil.copyFiles(new File(vocabulariesSourceDir, voc), destVoc);
            }
        }
        //loop on predefined vocabularies
        ArrayList<String> newUris = new ArrayList<String>();
        String[] vocs = vocabulariesDir.list();
        if (vocs != null) {
            for (String voc : vocs) {
                Invoker invk = new Invoker(this, "ca.licef.comete.vocabulary.VocabularyManager",
                        "initVocabulary", new Object[]{voc, false});
                String uri = (String)tripleStore.transactionalCall(invk, TripleStore.WRITE_MODE);
                if (uri != null)
                    newUris.add(uri);
            }
        }

        /*if (!newUris.isEmpty()) {
            //relationships
            for (String uri : newUris)
                initRelationships(uri);
        }*/

        System.out.println("Vocabulary Module initialization done.");
    }

    public String initVocabulary(String voc, boolean forceUpdate) throws Exception {
        File vocDir = new File(vocabulariesDir, voc);
        if (!vocDir.isDirectory())
            return null;

        File descriptor = new File(vocDir, "description.xml");
        String source = null;
        String cat = null;
        String location = null;
        boolean navigableFlag = false;
        String conceptUriPrefix = null;
        ArrayList<String> aliases = new ArrayList<String>();

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
                if ("source".equals(e.getTagName()))
                    source = value;
                if ("category".equals(e.getTagName()))
                    cat = value;
                if ("location".equals(e.getTagName()))
                    location = value;
                if ("navigable".equals(e.getTagName()))
                    navigableFlag = "true".equals(value);
                if ("conceptUriPrefix".equals(e.getTagName()))
                    conceptUriPrefix = value;
                if ("alias".equals(e.getTagName()))
                    aliases.add(value);
            }
        }

        boolean isNew = false;
        String uri = null;
        //set vocUri as local URI (default value for new records). reset later
        String vocUri = Core.getInstance().getUriPrefix() + "/voc/" + source.toLowerCase() + "/" + cat;
        String query = CoreUtil.getQuery("vocabulary/getVocContext.sparql", voc);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0) {
            uri = tuples[0].getValue("s").getContent();
            vocUri = tuples[0].getValue("vocUri").getContent(); //reset
        }

        ArrayList<Triple> list = new ArrayList<Triple>();
        if( uri == null ) {
            uri = CoreUtil.makeURI(voc, COMETE.VocContext);
            list.add(new Triple(uri, RDF.type, COMETE.VocContext));
            list.add(new Triple(uri, COMETE.vocId, voc));
            list.add(new Triple(uri, COMETE.vocSource, source));
            list.add(new Triple(uri, COMETE.vocSourceLocation, location));
            list.add(new Triple(uri, COMETE.vocNavigable, Boolean.toString(navigableFlag)));
            if (conceptUriPrefix != null)
                list.add(new Triple(uri, COMETE.vocConceptUriPrefix, conceptUriPrefix));

            for (String alias : aliases)
                list.add(new Triple(uri, COMETE.vocAlias, alias));

            tripleStore.insertTriples(list);

            isNew = true;
        }

        //content management
        if (isNew || forceUpdate)
            initVocabularyContent(uri, location, vocUri, navigableFlag, !isNew);

        return isNew?uri:null;
    }

    private void initVocabularyContent(String uri, String location, String vocUri,
                                       boolean isNavigable, boolean cleanFirst) throws Exception {
        //remove voc graph
        if (cleanFirst) {
            Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(uri, COMETE.vocUri);
            tripleStore.removeTriples(Arrays.asList(triples));
            clearVocabularyGraph(vocUri, isNavigable);
        }

        String vocContent;
        String skosContent;

        //content retrieving
        if (IOUtil.isURL(location))
            vocContent = IOUtil.readStringFromURL(new URL(location));
        else
            vocContent = IOUtil.readStringFromFile(new File(vocabulariesDir, location));

        int format = Util.getVocabularyFormat(vocContent);
        switch (format) {
            case Util.VDEX_FORMAT : //keep vdex version for history
                skosContent = convertVdexToSkos(vocContent);
//                IOUtil.writeStringToFile(skosContent, new File(vocabulariesDir, "skos.rdf"));
                break;
            default:
                skosContent = vocContent;
        }

        //force reset to catch external vocUris inside vocab
        Hashtable attributes = XMLUtil.getAttributes(skosContent, "//skos:ConceptScheme");
        String newVocUri = attributes.get("about").toString();
        tripleStore.removeTriple(new Triple(uri, COMETE.vocUri, vocUri));
        tripleStore.insertTriple(new Triple(uri, COMETE.vocUri, newVocUri));
        vocUri = newVocUri;

        //load content
        if (isNavigable)
            tripleStore.loadContent_textIndex(new ByteArrayInputStream(skosContent.getBytes()),
                    licef.tsapi.Constants.RDFXML, vocUri);
        else
            tripleStore.loadContent(new ByteArrayInputStream(skosContent.getBytes()),
                    licef.tsapi.Constants.RDFXML, vocUri);

        //Generation of inferred triples (for all vocs, navigable or not)
        tripleStore.doInference(vocUri, SKOS_ONTOLOGY_GRAPH);
    }

    private void clearVocabularyGraph(String vocUri, boolean isNavigable) throws Exception {
        if (isNavigable)
            tripleStore.clear_textIndex(vocUri);
        else
            tripleStore.clear(vocUri);
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
