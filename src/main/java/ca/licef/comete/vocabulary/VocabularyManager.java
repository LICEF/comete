package ca.licef.comete.vocabulary;

import ca.licef.comete.core.Core;
import ca.licef.comete.store.Store;
import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.util.Util;
import licef.IOUtil;
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

    public static final String SKOS_ONTOLOGY_GRAPH = "skos-ontology";

    File vocabulariesSourceDir; //init vocabularies
    File vocabulariesDirConfig;
    TripleStore tripleStore;
    static ca.licef.comete.core.util.Util CoreUtil;

    public String addNewVocContext(String id, String uriPrefix, String uriSuffix, String linkingPredicate,
                                   String urlLocation, String fileName, InputStream uploadedInputStream) throws Exception{
        if (id == null || "".equals(id))
            return "Missing ID";

        File vocDirConfig = new File(vocabulariesDirConfig, id);
        if (vocDirConfig.exists())
            return "Vocabulary with ID '" + id + "' already exists.";

        //create dest folder
        IOUtil.createDirectory(vocDirConfig.getAbsolutePath());

        String location = urlLocation;
        File contentFile = null;
        if (location == null || "".equals(location)) {
            if (fileName != null && !"".equals(fileName)) {
                location = fileName;
                //copy content
                contentFile = new File(vocDirConfig, fileName);
                OutputStream os = new FileOutputStream(contentFile);
                IOUtil.copy(uploadedInputStream, os);
                uploadedInputStream.close();
                os.close();
            }
            else
                return "Missing content (URL or uploaded vocabulary)";
        }

        //checking content
        int format = Util.getVocabularyFormatFromLocation((contentFile != null)?contentFile.getAbsolutePath():location);
        if (format == -1) {
            IOUtil.deleteDirectory(vocDirConfig);
            return "Not a VDEX or SKOS content.";
        }

        //descriptor creation
        createDecriptor(id, location, uriPrefix, uriSuffix, linkingPredicate);

        initVocabulary(id, false);

        return null;
    }

    public String modifyVocContext(String id, String uriPrefix, String uriSuffix, String linkingPredicate,
                                          String urlLocation, String fileName, InputStream uploadedInputStream) throws Exception {
        String error = null;
        File vocDirConfig = new File(vocabulariesDirConfig, id);
        String uri = ca.licef.comete.core.util.Util.makeURI(id, COMETE.VocContext);
        if (!tripleStore.isResourceExists(uri))
            return "Unknown vocabulary to modify.";

        tripleStore.removeTriplesWithSubjectPredicate(uri, COMETE.vocConceptUriPrefix);
        tripleStore.removeTriplesWithSubjectPredicate(uri, COMETE.vocConceptUriSuffix);
        tripleStore.removeTriplesWithSubjectPredicate(uri, COMETE.vocConceptLinkingPredicate);
        if (!"".equals(uriPrefix))
            tripleStore.insertTriple(new Triple(uri, COMETE.vocConceptUriPrefix, uriPrefix));
        if (!"".equals(uriSuffix))
            tripleStore.insertTriple(new Triple(uri, COMETE.vocConceptUriSuffix, uriSuffix));
        if (!"".equals(linkingPredicate))
            tripleStore.insertTriple(new Triple(uri, COMETE.vocConceptLinkingPredicate, linkingPredicate));

        //change content if needed
        Tuple[] details = Vocabulary.getInstance().getVocContextDetails(uri);
        String location = details[0].getValue("location").getContent();
        String newLocation = null;
        boolean isContentToUpdate = !"".equals(urlLocation) || !"".equals(fileName);
        boolean needContentUpdate = false;
        if (isContentToUpdate) {
            //possible modification from uploaded file
            if (!"".equals(fileName)) {
                File tmpContentFile = new File(vocDirConfig, fileName + "_tmp");
                OutputStream os = new FileOutputStream(tmpContentFile);
                IOUtil.copy(uploadedInputStream, os);
                uploadedInputStream.close();
                os.close();

                int format = Util.getVocabularyFormatFromLocation(tmpContentFile.getAbsolutePath());
                if (format == -1) {
                    tmpContentFile.delete();
                    error = "Not a VDEX or SKOS content.";
                }
                else {
                    if (location.startsWith("http"))
                        needContentUpdate = true;
                    else {
                        File content = new File(vocDirConfig, location);
                        FileInputStream fis = new FileInputStream(content);
                        FileInputStream fisTmp = new FileInputStream(tmpContentFile);
                        boolean isSameContent = DigestUtils.shaHex(fisTmp).equals(DigestUtils.shaHex(fis));
                        fisTmp.close();
                        fis.close();
                        content.delete();
                        needContentUpdate = !isSameContent;
                    }
                    File newContent = new File(vocDirConfig, fileName);
                    tmpContentFile.renameTo(newContent);
                    newLocation = fileName;
                }
            }

            //possible modification from external location
            if (!"".equals(urlLocation)) {
                int format = Util.getVocabularyFormatFromLocation(urlLocation);
                if (format == -1)
                    error = "Not a VDEX or SKOS content.";
                else {
                    if (location.startsWith("http")) {
                        InputStream is = new URL(location).openStream();
                        InputStream isTmp = new URL(urlLocation).openStream();
                        boolean isSameContent = DigestUtils.shaHex(isTmp).equals(DigestUtils.shaHex(is));
                        isTmp.close();
                        is.close();
                        needContentUpdate = !isSameContent;
                    }
                    else {
                        File content = new File(vocDirConfig, location);
                        content.delete();
                        needContentUpdate = true;
                    }
                    newLocation = urlLocation;
                }
            }
        }

        if (newLocation != null) {
            tripleStore.updateObjectTriple(uri, COMETE.vocSourceLocation, location, newLocation);
            location = newLocation;
        }

        //replacing descriptor
        (new File(vocDirConfig, "description.xml")).delete();
        createDecriptor(id, location, uriPrefix, uriSuffix, linkingPredicate);

        //update of content
        if (needContentUpdate)
           initVocabulary(id, true);

        return error;
    }

    private void createDecriptor(String id, String location, String uriPrefix, String uriSuffix, String linkingPredicate) throws Exception {
        File vocDirConfig = new File(vocabulariesDirConfig, id);

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
        IOUtil.writeStringToFile(XMLUtil.getXMLString(root), new File(vocDirConfig, "description.xml"));
    }

    public boolean deleteVocContext(String uri, boolean force) throws Exception {
        if (isVocabularyUsed(uri) && !force)
            return false;

        Tuple[] details = Vocabulary.getInstance().getVocContextDetails(uri);
        String vocUri = details[0].getValue("vocUri").getContent();
        String vocId = details[0].getValue("vocId").getContent();

        //triple store deletion
        tripleStore.removeResource(uri);
        tripleStore.clear_textIndex(vocUri);

        //physical deletion
        IOUtil.deleteDirectory(new File(vocabulariesDirConfig, vocId));
        Store.getInstance().deleteDigitalObject(Store.PATH_VOCABS + "/" + vocId);

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
                File destVocConf = new File(vocabulariesDirConfig, id);
                if (!destVocConf.exists())
                    IOUtil.copyFiles(new File(vocabulariesSourceDir, voc), destVocConf);
            }
        }

        //loop on predefined vocabularies
        String[] vocs = vocabulariesDirConfig.list();
        if (vocs != null) {
            for (String voc : vocs) {
                Invoker invk = new Invoker(this, "ca.licef.comete.vocabulary.VocabularyManager",
                        "initVocabulary", new Object[]{voc, false});
                tripleStore.transactionalCall(invk, TripleStore.WRITE_MODE);
            }
        }

        System.out.println("Vocabulary Module initialization done.");
    }

    public void initVocabulary(String vocId, boolean forceUpdate) throws Exception {
        File vocDir = new File(vocabulariesDirConfig, vocId);
        if (!vocDir.isDirectory())
            return;

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
    }

    private void initVocabularyContent(String uri, String location, String vocId, String vocUri, boolean cleanFirst) throws Exception {
        String storeId = Store.PATH_VOCABS + "/" + vocId;

        //remove voc graph and exposed vocs
        if (cleanFirst) {
            Triple[] triples = tripleStore.getTriplesWithSubjectPredicate(uri, COMETE.vocUri);
            tripleStore.removeTriples(Arrays.asList(triples));
            tripleStore.clear_textIndex(vocUri);
            Store.getInstance().deleteDigitalObject(storeId);
        }

        String vocContent;
        String skosContent;

        //content retrieving
        if (IOUtil.isURL(location))
            vocContent = IOUtil.readStringFromURL(new URL(location));
        else
            vocContent = IOUtil.readStringFromFile(new File(vocabulariesDirConfig, vocId + "/" + location));

        String vocUrlPrefix = Core.getInstance().getCometeUrl() + "/" +
                CoreUtil.getRestUrl(SKOS.ConceptScheme) + "/" + vocId;

        int format = Util.getVocabularyFormat(vocContent);

        switch (format) {
            case Util.VDEX_FORMAT : //keep vdex version for history
                Store.getInstance().setDatastream(storeId, vocId + ".vdex", vocContent);
                tripleStore.insertTriple(new Triple(uri, COMETE.vocLocalURL, vocUrlPrefix + "/vdex"));
                //convert content + relationships
                skosContent = convertVdexToSkos(vocContent);
                break;
            default:
                skosContent = vocContent;
        }

        Store.getInstance().setDatastream(storeId, vocId + ".skos", skosContent);
        tripleStore.insertTriple(new Triple(uri, COMETE.vocLocalURL, vocUrlPrefix + "/skos"));

        //force reset to catch external vocUris inside vocab
        Hashtable attributes = XMLUtil.getAttributes(skosContent, "//skos:ConceptScheme");
        String newVocUri = attributes.get("about").toString();
        tripleStore.updateObjectTriple(uri, COMETE.vocUri, vocUri, newVocUri);
        vocUri = newVocUri;

        //load content (with index even for non-navigable vocs, for possible future switch to)
        tripleStore.loadContent_textIndex(new ByteArrayInputStream(skosContent.getBytes()),
                licef.tsapi.Constants.RDFXML, vocUri);

        //Generation of inferred triples (for all vocs, navigable or not)
        //tripleStore.doInference(SKOS_ONTOLOGY_GRAPH, vocUri);
        //Use of tripleStore.doInference replaced with controlled 'manual' inference
        //to avoid huge generation of scrap (blank nodes, unnecessary inherited properties, ...) -AM
        doInference(vocUri);
    }

    private void doInference(String vocUri) throws Exception {
        tripleStore.sparqlUpdate( CoreUtil.getQuery("vocabulary/inferBroader.sparql", vocUri) );
        tripleStore.sparqlUpdate( CoreUtil.getQuery("vocabulary/inferNarrower.sparql", vocUri) );
        tripleStore.sparqlUpdate( CoreUtil.getQuery("vocabulary/inferNarrowerTransitive.sparql", vocUri) );
        tripleStore.sparqlUpdate( CoreUtil.getQuery("vocabulary/inferCloseMatch.sparql", vocUri) );
        tripleStore.sparqlUpdate( CoreUtil.getQuery("vocabulary/inferNarrowMatch.sparql", vocUri) );
        tripleStore.sparqlUpdate( CoreUtil.getQuery("vocabulary/inferPrefLabel.sparql", vocUri) );
    }

    public String convertVdexToSkos( String vdexContent ) throws Exception {
        StreamSource source = new StreamSource( new ByteArrayInputStream( vdexContent.getBytes() ) );
        return( CoreUtil.applyXslToDocument( "vocabulary/convertVDEXToSKOS", source ) );
    }
}
