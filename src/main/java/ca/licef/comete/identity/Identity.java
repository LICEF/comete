package ca.licef.comete.identity;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.identity.model.Entity;
import ca.licef.comete.identity.model.Organization;
import ca.licef.comete.identity.model.Person;
import ca.licef.comete.identity.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.rdf.model.Property;
import com.sun.jersey.core.header.FormDataContentDisposition;
import licef.IOUtil;
import licef.StringUtil;
import licef.reflection.Invoker;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.model.Tuple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import licef.tsapi.vocabulary.RDF;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;

public class Identity {

    public static final String IDENTITY_SIMILARITY_GRAPH = "identity-similarity";
    static File PHOTO_FOLDER = new File(Core.getInstance().getCometeHome() + "/photos/");


    public static final int IDENTITY_FULL = 0;
    public static final int IDENTITY_NORMAL = 1;

    private PersonView personView;
    private OrganizationView organizationView;
    private Resolver resolver;

    static TripleStore tripleStore = Core.getInstance().getTripleStore();
    static ca.licef.comete.core.util.Util CoreUtil;

    static {
        try {
            if (!PHOTO_FOLDER.exists())
                PHOTO_FOLDER.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Identity getInstance() {
        if (instance == null)
            instance = new Identity();
        return (instance);
    }

    public PersonView getPersonView() {
        if (personView == null)
            personView = new PersonView();
        return personView;
    }

    public OrganizationView getOrganizationView() {
        if (organizationView == null)
            organizationView = new OrganizationView();
        return organizationView;
    }

    private static Identity instance;

    public Resolver getResolver() {
        if (resolver == null)
            resolver = new Resolver();
        return resolver;
    }

    public String[] digest(String format, String content) throws Exception {
        String personUri = null;
        String orgUri = null;
        if (format.equals(Constants.VCARD_MIMETYPE) && !"".equals(content)) {
            Hashtable<String, String> vcardElements = Util.getVCardElements(content);
            String formattedName = vcardElements.get("formattedName");
            String firstname = vcardElements.get("firstname");
            String lastname = vcardElements.get("lastname");
            String email = vcardElements.get("email");
            String address = vcardElements.get("address");
            String photo = vcardElements.get("photo");
            String tel = vcardElements.get("tel");
            String fax = vcardElements.get("fax");
            String url = vcardElements.get("url");
            String org = vcardElements.get("org");
            String orgAddress = vcardElements.get("orgAddress");
            String logo = vcardElements.get("logo");
            if (email != null && !"".equals(email))
                email = "mailto:" + email;
            if (tel != null && !"".equals(tel))
                tel = "tel:" + tel;
            if (fax != null && !"".equals(tel))
                fax = "fax:" + fax;
            if ("".equals(org))
                org = null;

            formattedName = getFormattedName(formattedName, firstname, lastname, email, url, org);

            //vcard representing only organization, clear formattedName to bypass person case
            if (org != null && org.equals(formattedName))
                formattedName = null;

            //For the moment :
            //  - tel is associated to person if present, else to org
            //  - fax is associated to person if present, else to org
            //  - url is associated to org if present, else to person
            //  - work address is associated to person if org not present
            //  - post address is associated to org if person not present
            // -AM

            //person
            if (formattedName != null) {
                personUri = retrievePerson(formattedName, firstname, lastname, email);
                String urlPerson = (org == null)?url:"";
                String adrPerson = (address == null && org == null)?orgAddress:address;
                if (personUri == null)
                    personUri = createPerson(formattedName, firstname, lastname, email, adrPerson, tel, fax, urlPerson, photo);
                else
                    updatePerson(personUri, formattedName, firstname, lastname, email, adrPerson, tel, fax, urlPerson, photo);

            }

            boolean isPersonRetrieved = (personUri != null);
            //Organization
            if (org != null) {
                orgUri = retrieveOrganization(org);
                String emailOrg = isPersonRetrieved?null:email;
                String telOrg = isPersonRetrieved?null:tel;
                String faxOrg = isPersonRetrieved?null:fax;
                String adrOrg = (orgAddress == null && personUri == null)?address:orgAddress;
                if (!isPersonRetrieved && logo == null && photo != null)
                    logo = photo;
                if (orgUri == null)
                    orgUri = createOrganization(org, adrOrg, emailOrg, telOrg, faxOrg, url, logo);
                else
                    updateOrganization(orgUri, org, adrOrg, emailOrg, telOrg, faxOrg, url, logo);

                //now, a person may part of more than one org
                if (personUri != null )
                    tripleStore.insertTriple(new Triple(personUri, DCTERMS.isPartOf, orgUri));
            }
        } else if (format.equals(Constants.TEXT_MIMETYPE) && !"".equals(content)) {
            content = content.trim();
            personUri = retrieveIdentityFN(content);
            if (personUri == null) //guess it's a person here -AM
                personUri = createPerson(content);
        }

        return new String[]{personUri, orgUri};
    }

    public String retrieveIdentityFN(String formattedName) throws Exception {
        String uri = retrievePersonFN(formattedName);
        if (uri == null)
            uri = retrieveOrganization(formattedName);
        return uri;
    }

    public String retrievePerson(String formattedName, String firstname, String lastname, String email) throws Exception {
        String uri = null;
        int identityLevel = getIdentityLevel(firstname, lastname, email);
        switch (identityLevel) {
            case IDENTITY_FULL:
                firstname = firstname.replaceAll( "\"", "\\\\\"" );
                lastname = lastname.replaceAll( "\"", "\\\\\"" );

                String query = CoreUtil.getQuery("identity/getIdentityPersonFull.sparql", firstname, lastname, email);
                Tuple[] tuples = tripleStore.sparqlSelect(query);
                if (tuples.length > 0)
                    uri = tuples[0].getValue("s").getContent();
                break;
            default:
                uri = retrievePersonFN(formattedName);
        }
        return uri;
    }

    public String retrievePersonFN(String formattedName) throws Exception {
        String uri = null;
        formattedName = formattedName.replaceAll( "\"", "\\\\\"" );

        String query = CoreUtil.getQuery("identity/getIdentityPersonFN.sparql", formattedName);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            uri = tuples[0].getValue("s").getContent();

        return uri;
    }

    String createPerson(String formattedName) throws Exception {
        return createPerson(formattedName, null, null, null, null, null, null, null, null);
    }

    String createPerson(String formattedName, String firstname, String lastname, String email, String address,
                        String tel, String fax, String url, String photo) throws Exception {
        boolean isFirstname = (firstname != null && !"".equals(firstname));
        boolean isLastname = (lastname != null && !"".equals(lastname));
        boolean isEmail = (email != null && !"".equals(email));
        boolean isAddress = (address != null && !"".equals(address));
        boolean isTel = (tel != null && !"".equals(tel));
        boolean isFax = (fax != null && !"".equals(fax));
        boolean isUrl = (url != null && !"".equals(url));
        boolean isPhoto = (photo != null && !"".equals(photo));

        String uri = CoreUtil.makeURI(COMETE.Person);

        ArrayList<Triple> list = new ArrayList<>();
        list.add(new Triple(uri, RDF.type, COMETE.Person));
        //fn always present
        list.add(new Triple(uri, FOAF.name, formattedName));
        if (isFirstname)
            list.add(new Triple(uri, FOAF.givenName, firstname));
        if (isLastname)
            list.add(new Triple(uri, FOAF.familyName, lastname));
        if (isEmail)
            list.add(new Triple(uri, FOAF.mbox, email));
        if (isAddress)
            list.add(new Triple(uri, COMETE.formattedAddress, address));
        if (isTel)
            list.add(new Triple(uri, FOAF.phone, tel));
        if (isFax)
            list.add(new Triple(uri, FOAF.phone, fax));
        if (isUrl)
            list.add(new Triple(uri, FOAF.homepage, IOUtil.enforceAbsoluteUrl( url ) ));

        if (isPhoto) {
            try {
                String photoUrl = setPhoto(photo);
                list.add(new Triple(uri, FOAF.img, photoUrl));
            } catch (Exception e) {
                System.out.println("Photo creation error for " + uri);
                e.printStackTrace();
            }
        }

        tripleStore.insertTriplesWithTextIndex(list, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);

        System.out.println("Person: " + formattedName + " created");

        //Search and add marker for similar persons
        manageSimilarPersons(uri, formattedName, firstname, lastname);

        return uri;
    }

    void updatePerson(String uri, String formattedName, String firstname, String lastname, String email,
                      String adr, String tel, String fax, String url, String photo) throws Exception{
        ArrayList<Triple> list = new ArrayList<Triple>();
        Entity person = Util.getEntity(uri);
        maybeCopyValue(person, FOAF.name, COMETE.altName, formattedName, list);
        maybeCopyValue(person, FOAF.givenName, COMETE.altGivenName, firstname, list);
        maybeCopyValue(person, FOAF.familyName, COMETE.altFamilyName, lastname, list);
        maybeCopyValue(person, FOAF.mbox, COMETE.altMbox, email, list);
        maybeCopyValue(person, COMETE.formattedAddress, COMETE.altFormattedAddress, adr, list);
        maybeCopyValue(person, FOAF.phone, COMETE.altPhone, tel, list);
        maybeCopyValue(person, FOAF.phone, COMETE.altPhone, fax, list);
        maybeCopyValue(person, FOAF.homepage, COMETE.altHomepage, IOUtil.enforceAbsoluteUrl(url), list);

        //photo management
        if (photo != null) {
            try {
                String photoUrl = setPhoto(photo);
                maybeCopyValue(person, FOAF.img, COMETE.altImg, photoUrl, list);
            } catch (Exception e) {
                System.out.println("Photo update error for " + uri);
                e.printStackTrace();
            }
        }

        if (!list.isEmpty())
            tripleStore.insertTriplesWithTextIndex(list, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);
    }

    int getIdentityLevel(String firstname, String lastname, String email) {
        boolean isFirstname = (firstname != null && !"".equals(firstname));
        boolean isLastname = (lastname != null && !"".equals(lastname));
        boolean isEmail = (email != null && !"".equals(email));
        if (isFirstname && isLastname && isEmail)
            return IDENTITY_FULL;
        else if (isFirstname && isLastname)
            return IDENTITY_NORMAL;
        return -1;
    }

    String getFormattedName(String formattedName, String firstname, String lastname, String email, String url, String org) {
        boolean isFormattedName = (formattedName != null && !"".equals(formattedName));
        boolean isFirstname = (firstname != null && !"".equals(firstname));
        boolean isLastname = (lastname != null && !"".equals(lastname));
        if (isFirstname && isLastname)
            return firstname + " " + lastname;
        if (isFormattedName)
            return formattedName;

        formattedName = null;
        if (isFirstname)
            formattedName = firstname;
        else if (isLastname)
            formattedName = lastname;

        boolean isEmail = (email != null && !"".equals(email));
        boolean isURL = (url != null && !"".equals(url));
        boolean isOrg = (org != null && !"".equals(org));
                
        //org + email => email of org
        //lonely email without org => email of person
        if (formattedName == null && isEmail && !isOrg)
            formattedName = email.substring("mailto:".length());

        if (formattedName == null && isURL && !isOrg)
            formattedName = url;

        return formattedName;
    }

    private String retrieveOrganization(String orgName) throws Exception {
        String uri = null;       
        orgName = orgName.replaceAll( "\"", "\\\\\"" );

        String query = CoreUtil.getQuery("identity/getIdentityOrganization.sparql", orgName);
        Tuple[] tuples = tripleStore.sparqlSelect(query);
        if (tuples.length > 0)
            uri = tuples[0].getValue("s").getContent();

        return uri;
    }

    private String createOrganization(String org, String orgAddress, String orgEmail,
                                      String orgTel, String orgFax, String url, String logo) throws Exception{
        boolean isAddr = (orgAddress != null && !"".equals(orgAddress));
        boolean isEmail = (orgEmail != null && !"".equals(orgEmail));
        boolean isTel = (orgTel != null && !"".equals(orgTel));
        boolean isFax = (orgFax != null && !"".equals(orgFax));
        boolean isUrl = (url != null && !"".equals(url));
        boolean isLogo = (logo != null && !"".equals(logo));

        String uri = CoreUtil.makeURI(COMETE.Organization);

        ArrayList<Triple> list = new ArrayList<>();
        list.add(new Triple(uri, RDF.type, COMETE.Organization));
        //org fn always present
        list.add(new Triple(uri, FOAF.name, org));
        if (isAddr)
            list.add(new Triple(uri, COMETE.formattedAddress, orgAddress));
        if (isEmail)
            list.add(new Triple(uri, FOAF.mbox, orgEmail));
        if (isTel)
            list.add(new Triple(uri, FOAF.phone, orgTel));
        if (isFax)
            list.add(new Triple(uri, FOAF.mbox, orgFax));
        if (isUrl)
            list.add(new Triple(uri, FOAF.homepage, IOUtil.enforceAbsoluteUrl(url)));

        if (isLogo) {
            try {
                String logoUrl = setPhoto(logo);
                list.add(new Triple(uri, FOAF.logo, logoUrl));
            } catch (Exception e) {
                System.out.println("Photo creation error for " + uri);
                e.printStackTrace();
            }
        }

        tripleStore.insertTriplesWithTextIndex(list, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);

        System.out.println("Org: " + org + " created");

        //Search and add marker for similar organizations
        manageSimilarOrganizations(org, uri);

        return uri;
    }

    void updateOrganization(String uri, String orgName, String orgAddress, String emailOrg,
                            String telOrg, String faxOrg, String url, String logo) throws Exception {
        ArrayList<Triple> list = new ArrayList<>();
        Entity org = Util.getEntity(uri);
        maybeCopyValue(org, FOAF.name, COMETE.altName, orgName, list);
        maybeCopyValue(org, FOAF.mbox, COMETE.altMbox, emailOrg, list);
        maybeCopyValue(org, COMETE.formattedAddress, COMETE.altFormattedAddress, orgAddress, list);
        maybeCopyValue(org, FOAF.phone, COMETE.altPhone, telOrg, list);
        maybeCopyValue(org, FOAF.phone, COMETE.altPhone, faxOrg, list);
        maybeCopyValue(org, FOAF.homepage, COMETE.altHomepage, IOUtil.enforceAbsoluteUrl(url), list);

        //logo management
        if (logo != null) {
            try {
                String logoUrl = setPhoto(logo);
                maybeCopyValue(org, FOAF.logo, COMETE.altLogo, logoUrl, list);

            } catch (Exception e) {
                System.out.println("Photo update error for " + uri);
                e.printStackTrace();
            }
        }

        if (!list.isEmpty())
            tripleStore.insertTriplesWithTextIndex(list, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);
    }

    void maybeCopyValue(Entity en, Property pred, Property predAlt, String value, List<Triple> list) throws Exception{
        if (value == null || "".equals(value))
            return;
        
        ArrayList<String> values = en.getList(pred.getURI());
        if (!values.contains(value)) {
            Property predicate = (values.isEmpty())?pred:predAlt;
            list.add(new Triple(en.getUri(), predicate, value));
        }
    }

    /********************/
    /* PHOTO MANAGEMENT */
    /********************/

    String setPhoto(String photo) throws Exception {
        //photo format
        //  "image mimetype:BASE64content" or "image mimetype:URL";
        //ex : "image/gif:R0lGODlhXQBeAOMP...." or "image/gif:http://server.com/logo.gif"
        int index = photo.indexOf(":");
        String mimetype = photo.substring(0, index);
        String image = photo.substring(index + 1);
        String imgUrl;

        if (image.startsWith("http"))
            imgUrl = image;
        else { //Binary
            String ext = mimetype.split("/")[1].toLowerCase();
            String sha = DigestUtils.shaHex(image);
            String filename = sha + "." + ext;
            File destFile = new File(Core.getInstance().getCometeHome() + "/photos/" + filename);
            if (!destFile.exists()) {
                try {
                    byte[] array = Base64.decodeBase64(image.getBytes());
                    InputStream in = new ByteArrayInputStream(array);
                    BufferedImage bim = ImageIO.read(in);
                    ImageIO.write(bim, ext, destFile);
                } catch (Exception e) {
                    destFile.delete();
                    throw e;
                }
            }
            imgUrl = CoreUtil.getRestUrl(COMETE.Identity) + "/photo/" + filename;
        }

        return imgUrl;
    }

    public byte[] getPhoto(String localUrl) throws Exception {
        String filename = localUrl.substring(localUrl.lastIndexOf("/") + 1);
        File file = new File(Core.getInstance().getCometeHome() + "/photos/" + filename);
        if (file.exists() && file.length() > 0) {
            BufferedImage bim =  ImageIO.read(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, StringUtil.split(file.getName(), '.')[1], baos);
            return baos.toByteArray();
        }
        return null;
    }

    public String[] storeUploadedPhoto(InputStream uploadedInputStream, FormDataContentDisposition fileDetail) throws Exception {
        String imgUrl = null;
        String errorMessage = null;
        if ("".equals(fileDetail.getFileName()))
            errorMessage = "No file.";
        else {
            try {
                String filename = fileDetail.getFileName();
                String format = filename.substring(filename.lastIndexOf(".") + 1);
                //use of setPhoto method to avoid multi upload of the same image
                //so compute base64 string -AM
                byte[] bytes = IOUtils.toByteArray(uploadedInputStream);
                String base64 = new String(Base64.encodeBase64(bytes));
                imgUrl = setPhoto("image/" + format + ":" + base64);
            } catch (Exception e) {
                errorMessage = "Cannot upload this file.";
            }
        }
        return new String[]{imgUrl, errorMessage};
    }

    /*************************/
    /* SIMILARITY MANAGEMENT */
    /*************************/

    void manageSimilarPersons(String uri, String formattedName, String firstname, String lastname) throws Exception {
        int identityLevel = getIdentityLevel(firstname, lastname, null);
        Tuple[] results;
        switch (identityLevel) {
            case IDENTITY_NORMAL:
                String query = CoreUtil.getQuery("identity/searchSimilarPersons.sparql",
                        firstname, lastname, Core.getInstance().getUriPrefix());
                results = tripleStore.sparqlSelectWithTextIndex(query, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);
                break;
            default:
                //avoid braces interpreted within boolean expression (and also wrong balancing) -AM
                formattedName = formattedName.replace("(", " ");
                formattedName = formattedName.replace(")", " ");
                //remove quotes to avoid lucene problems -AM
                formattedName = formattedName.replace("\"", "");

                //quote each term (even for 1 term ex: e-mail as FN).
                //also for compound names case
                //all terms must be present. so find FN permutation only... (else too much results matched) -AM
                String[] terms = formattedName.split(" ");
                String delimiter = "";
                formattedName = "";
                for (int i = 0; i < terms.length; i++) {
                    formattedName += delimiter;
                    formattedName += "\\\"" + terms[i] + "\\\"";
                    delimiter = " AND ";
                }
                query = CoreUtil.getQuery("identity/searchSimilarPersonsFN.sparql",
                        formattedName, Core.getInstance().getUriPrefix());
                results = tripleStore.sparqlSelectWithTextIndex(
                        query, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);
        }
        if ( results == null || results.length == 0 )
            return;

        //uuid generation
        String uuid = UUID.randomUUID().toString();

        ArrayList<String> groupIds = new ArrayList<String>();
        ArrayList<Triple> similarTriples = new ArrayList<Triple>();

        String constraints = "";
        String delimiter = "";
        boolean insertCurrent = false;
        for (int i = 0; i < results.length; i++) {
            String _uri = results[i].getValue("s").getContent();
            System.out.println("_uri = " + _uri);
            String _gid = results[i].getValue("gid").getContent();
            System.out.println("_gid = " + _gid);
            if ( _gid != null && !"".equals(_gid) && !groupIds.contains(_gid)) {
                constraints += delimiter ;
                constraints += "?gid = \"" + _gid + "\"";
                groupIds.add(_gid);
                delimiter = " || ";
            }
            else
                similarTriples.add(new Triple(_uri, DCTERMS.identifier, uuid));
            insertCurrent = true;
        }

        if (!"".equals(constraints)) {
            String query = CoreUtil.getQuery("identity/manageSimilarIdentities.sparql",
                    Core.getInstance().getUriPrefix(), uuid, constraints  );
            tripleStore.sparqlUpdate(query);
        }
        tripleStore.insertTriples(similarTriples, IDENTITY_SIMILARITY_GRAPH); //to not forget others without gid
        if (insertCurrent)
            tripleStore.insertTriple(new Triple(uri, DCTERMS.identifier, uuid), IDENTITY_SIMILARITY_GRAPH); //current uri
    }

    void manageSimilarOrganizations(String orgName, String orgUri) throws Exception {
        Object[] res = Util.extractPertinentTermsFromOrg(orgName);
        String[] clearedTerms = (String[])res[0];
        String terms = (String)res[1];
        String query = CoreUtil.getQuery("identity/searchSimilarOrganizations.sparql",
                terms, Core.getInstance().getUriPrefix());
        Tuple[] results = tripleStore.sparqlSelectWithTextIndex(
                query, Constants.indexPredicates, Constants.INDEX_LANGUAGES, null);

        if ( results == null || results.length == 0 )
            return;

        //uuid generation
        String uuid = UUID.randomUUID().toString();

        ArrayList<String> groupIds = new ArrayList<String>();
        ArrayList<Triple> similarTriples = new ArrayList<Triple>();

        String constraints = "";
        String delimiter = "";
        boolean insertCurrent = false;
        for (int i = 0; i < results.length; i++) {
            String _uri = results[i].getValue("s").getContent();
            String _name = results[i].getValue("n").getContent();

            //check of similarity (no need to check name similarity with orgUri)
            if (!_uri.equals(orgUri) && !Util.isSimilarOrgNames(clearedTerms, Util.clearOrgName(_name).split(" ")))
                continue;

            String _gid = results[i].getValue("gid").getContent();  //keep the quotes for constraints
            if ( _gid != null && !"".equals(_gid) && !groupIds.contains(_gid)) {
                constraints += delimiter ;
                constraints += "?gid = \"" + _gid + "\"";
                groupIds.add(_gid);
                delimiter = " || ";
            }
            else
                similarTriples.add(new Triple(_uri, DCTERMS.identifier, uuid));
            insertCurrent = true;
        }

        if (!"".equals(constraints)) {
            query = CoreUtil.getQuery("identity/manageSimilarIdentities.sparql",
                    Core.getInstance().getUriPrefix(), uuid, constraints  );
            tripleStore.sparqlUpdate(query);
        }
        tripleStore.insertTriples(similarTriples, IDENTITY_SIMILARITY_GRAPH); //to not forget others without gid
        if (insertCurrent)
            tripleStore.insertTriple(new Triple(orgUri, DCTERMS.identifier, uuid), IDENTITY_SIMILARITY_GRAPH); //current uri
    }


    /********************/
    /* Identity details */
    /********************/

    void loopVars(String[] vars, Tuple[] results, JSONObject detail)  throws Exception {
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < vars.length; j++) {
                if (!"".equals(results[i].getValue(vars[j]).getContent())) {
                    String val = CoreUtil.manageQuotes(results[i].getValue(vars[j]).getContent());
                    if ("email".equals(vars[j]))
                        val = val.substring("mailto:".length());
                    else if ("tel".equals(vars[j]))
                        val = val.substring("tel:".length());
                    else if ("fax".equals(vars[j]))
                        val = val.substring("fax:".length());
                    detail.put( vars[j], val );
                    break;
                }
            }
        }
    }

    public JSONArray getPersonDetails(String uri) throws Exception {
        String query = CoreUtil.getQuery("identity/getPersonDetails.sparql", uri);
        Tuple[] results = tripleStore.sparqlSelect(query);
        JSONArray personDetails = new JSONArray();
        JSONObject detail = new JSONObject();
        String[] vars = new String[]{"name", "firstname", "lastname", "email", "url", "address", "photo", "tel", "fax"};
        loopVars(vars, results, detail);
        personDetails.put(detail);
        return personDetails;
    }

    public JSONArray getOrganizationDetails(String uri) throws Exception {
        String query = CoreUtil.getQuery("identity/getOrganizationDetails.sparql", uri);
        Tuple[] results = tripleStore.sparqlSelect(query);
        JSONArray orgDetails = new JSONArray();
        JSONObject detail = new JSONObject();
        String[] vars = new String[]{"name", "email", "url", "address", "logo", "tel", "fax"};
        loopVars(vars, results, detail);
        orgDetails.put(detail);
        return orgDetails;
    }

    void extractAllDetails(JSONArray uriArray, String queryName, String[] vars, JSONObject details )  throws Exception {
        ArrayList<String> uris = new ArrayList<String>();
        for (int i = 0; i < uriArray.length(); i++)
            uris.add((String)uriArray.get(i));
        String constraints = CoreUtil.buildFilterConstraints(uris, "s", true, "=", "||");
        String query = CoreUtil.getQuery(queryName, constraints);
        Tuple[] results = tripleStore.sparqlSelect(query);

        //cumul arrays init
        JSONArray[] arrays = new JSONArray[vars.length];
        for (int i = 0; i < vars.length; i++)
            arrays[i] = new JSONArray();

        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < vars.length; j++) {
                String key = vars[j];
                String res = results[i].getValue(key).getContent();
                if ("".equals(res)) {
                    key = "alt" + key;
                    res = results[i].getValue(key).getContent();
                }
                if (!"".equals(res)) {
                    String val = CoreUtil.manageQuotes(res);
                    if (key.endsWith("email"))
                        val = val.substring("mailto:".length());
                    else if (key.endsWith("tel"))
                        val = val.substring("tel:".length());
                    else if (key.endsWith("fax"))
                        val = val.substring("fax:".length());
                    JSONObject jsonVal = new JSONObject().put("value", val);
                    if (key.startsWith("alt"))
                        arrays[j].put(jsonVal);
                    else
                        Util.insertFirst(arrays[j], jsonVal);
                    break;
                }
            }
        }

        for (int i = 0; i < vars.length; i++)
            if (arrays[i].length() > 0) details.put(vars[i], arrays[i]);
    }

    public JSONObject getAllPersonDetails(JSONArray uriArray) throws Exception {
        JSONObject details = new JSONObject();
        String[] vars = new String[]{"name", "firstname", "lastname", "email", "url", "address", "photo", "tel", "fax" };
        //details
        extractAllDetails(uriArray, "getAllPersonDetails.sparql", vars, details);
        return details;
    }

    public JSONObject getAllOrganizationDetails(JSONArray uriArray) throws Exception {
        JSONObject details = new JSONObject();
        String[] vars = new String[]{"name", "email", "url", "address", "logo", "tel", "fax" };
        //details
        extractAllDetails(uriArray, "getAllOrganizationDetails.sparql", vars, details);
        return details;
    }

    public Map getEmails( String[] identities ) throws Exception {
        Map emails = new HashMap();

        Tuple[] rs = null;
        try {
            String query = CoreUtil.getQuery("identity/getEmails.sparql",
                    CoreUtil.buildFilterConstraints(identities, "identity", true, "=", "||"));
            rs = tripleStore.sparqlSelect(query);
        }
        catch( Exception ignore ) {
            // The table will be empty but it's no big deal.
        }
        
        if( rs != null ) {
            for( int i = 0; i < rs.length; i++ ) {
                Tuple row = rs[i];
                String uri = row.getValue( "identity" ).getContent();
                String email = CoreUtil.manageQuotes( row.getValue( "mbox" ).getContent() );
                int indexOfMailToPrefix = email.indexOf( "mailto:" );
                if( indexOfMailToPrefix != -1 )
                    email = email.substring( indexOfMailToPrefix + "mailto:".length() );
                if( !"".equals( email ) )
                    emails.put( uri, email );
            }
        }

        return( emails );
    }


    /**********************/
    /** VCard management **/
    /**********************/

    public String getVCard(String uri, String loURI) throws Exception {
        //uri is possibly merged previously and replaced by another one.
        if (!tripleStore.isResourceExists(uri)) {
            Triple triple = tripleStore.getTriplesWithPredicateObject(DCTERMS.replaces, uri, null)[0];
            uri = triple.getSubject();
        }

        Entity identity = Util.getEntity(uri);
        boolean isPerson = identity instanceof Person;
        if (isPerson)
            return ((Person)identity).getVCard(uri, loURI);
        else
            return ((Organization)identity).getVCard(uri);
    }

    public String getFN(String uri, String loURI) throws Exception {
        //uri is possibly merged previously and replaced by another one.
        if (!tripleStore.isResourceExists(uri)) {
            Triple triple = tripleStore.getTriplesWithPredicateObject(DCTERMS.replaces, uri, null)[0];
            uri = triple.getSubject();
        }

        Entity identity = Util.getEntity(uri);
        return identity.getFN();
    }

    public Object[] getAllPersons(int start, int limit) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.identity.Identity",
                "getAllPersonsEff", new Object[]{start, limit});
        return (Object[])tripleStore.transactionalCall(inv);
    }

    public Object[] getAllPersonsEff(int start, int limit) throws Exception {
        int length = tripleStore.getTriplesWithPredicateObject(RDF.type, COMETE.Person.getURI(), null).length;
        String query = CoreUtil.getQuery("identity/getPersons.sparql", start, limit);
        return new Object[] {length, tripleStore.sparqlSelect(query)};
    }

    public Object[] getAllOrganizations(int start, int limit) throws Exception {
        Invoker inv = new Invoker(this, "ca.licef.comete.identity.Identity",
                "getAllOrganizationsEff", new Object[]{start, limit});
        return (Object[])tripleStore.transactionalCall(inv);
    }

    public Object[] getAllOrganizationsEff(int start, int limit) throws Exception {
        int length = tripleStore.getTriplesWithPredicateObject(RDF.type, COMETE.Organization.getURI(), null).length;
        String query = CoreUtil.getQuery("identity/getOrganizations.sparql", start, limit);
        return new Object[] {length, tripleStore.sparqlSelect(query)};
    }
}
    
