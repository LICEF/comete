package ca.licef.comete.identity.util;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import ca.licef.comete.identity.Identity;
import ca.licef.comete.identity.model.Entity;
import ca.licef.comete.identity.model.Organization;
import ca.licef.comete.identity.model.Person;
import ca.licef.comete.vocabularies.COMETE;
import licef.IOUtil;
import licef.StringUtil;
import licef.tsapi.TripleStore;
import licef.tsapi.model.Triple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.ORG;
import licef.tsapi.vocabulary.RDF;
import net.fortuna.ical4j.util.Strings;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.property.N;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 25-Oct-2011
 * Time: 2:04:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {

    public static List<String> ORGANIZATION_STOP_WORDS = new ArrayList<String>();
    public static List<String> ORGANIZATION_STOP_WORDS_EDUC = new ArrayList<String>();
    public static List<String> EDUC_SYNONYMS = new ArrayList<String>();

    static TripleStore tripleStore = Core.getInstance().getTripleStore();

    static {
        try {
            String stopWords = IOUtil.readString(Identity.getInstance().getClass().getResourceAsStream("/conf/identity/OrganizationStopWords.txt"));
            String[] words = stopWords.split(";");
            for (String word : words)
                ORGANIZATION_STOP_WORDS.add(word.trim());

            stopWords = IOUtil.readString(Identity.getInstance().getClass().getResourceAsStream("/conf/identity/OrganizationStopWordsEduc.txt"));
            words = stopWords.split(";");
            for (String word : words)
                ORGANIZATION_STOP_WORDS_EDUC.add(word.trim());

            Vector v = IOUtil.readLines(Identity.getInstance().getClass().getResourceAsStream("/conf/identity/OrganizationSynonyms.txt"));
            for (Enumeration e = v.elements(); e.hasMoreElements();) {
                String line = (String)e.nextElement();
                if (!line.startsWith("#"))
                    EDUC_SYNONYMS.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**********************/
    /** VCard management **/
    /**********************/
    public static void unescapePut(Hashtable<String, String> table, String key, String value) {
        if (value == null)
            value = "";
        else {
            value = value.replaceAll("\\\\,", ",");
            value = value.trim();
            if (value.startsWith(","))
                value = value.substring(1);
            if (value.endsWith(","))
                value = value.substring(0, value.length() - 1);
        }
        value = Strings.unescape(value);
        value = value.replace( "\\", "\\\\" );
        table.put(key, value);
    }

    public static Hashtable<String, String> getVCardElements(String vcard) throws Exception {
        Hashtable<String, String> vcardTable = new Hashtable<String, String>();
        /* Corrections to let ical4j-vcard parser works */

        //clear spaces before all properties
        vcard = preAdjustment(vcard);

        //BEGIN and END must be without ending spaces
        vcard = clearSpacesAndCr(vcard, "BEGIN:");
        int endIndex = vcard.indexOf("END:VCARD");
        vcard = vcard.substring(0, endIndex + "END:VCARD".length());

        //ponctuation corrections
        vcard = vcard.replaceAll("\\\\,", ",");
        vcard = vcard.replaceAll("\\\\:", ":");
        vcard = vcard.replaceAll("\\\\;", ";");

        //URL case, value must be trimmed inside vcard
        vcard = clearSpacesAndCr(vcard, "URL:");
        vcard = clearSpacesAndCr(vcard, "URL;");

        //PHOTO, LOGO case, value must be without space if url
        vcard = adjustPhoto(vcard, "PHOTO;");
        vcard = adjustPhoto(vcard, "LOGO;");

        //ADR case must have 7 values
        vcard = adjustAddress(vcard);

        VCardBuilder builder = new VCardBuilder(new ByteArrayInputStream(vcard.getBytes()));
        VCard card = builder.build();

        for (Property property : card.getProperties()) {
            String propertyName = property.getId().getPropertyName();
            if ("FN".equals(propertyName))
                unescapePut(vcardTable, "formattedName", property.getValue());
            else if ("N".equals(propertyName)) {
                N name = (N) property;
                unescapePut(vcardTable, "firstname", name.getGivenName());
                unescapePut(vcardTable, "lastname", name.getFamilyName());
            }
            else if ("PHOTO".equals(propertyName) || "LOGO".equals(propertyName)) {
                String mimetype = "image/jpeg";
                for (Parameter param : property.getParameters()) {
                    if ("TYPE".equals(param.getId().getPname())) {
                        mimetype = "image/" + param.getValue();
                        break;
                    }
                }
                String photo = property.getValue();
                if (photo.startsWith("http")) {
                    try {
                        String mt = IOUtil.getMimeType(photo);
                        if (mt != null)
                            mimetype = mt;
                    } catch (IOException e) {
                        System.out.println("Cannot retrieve mimetype for " + photo + " : " + e);
                    }
                }
                unescapePut(vcardTable, propertyName.toLowerCase(), mimetype + ":" + photo);  // important! ':' not in BASE64 alphabet -AM
            }
            else if ("ADR".equals(propertyName)) {
                for (Parameter param : property.getParameters()) {
                    if ("TYPE".equals(param.getId().getPname())) {
                        if (param.getValue().toUpperCase().contains("WORK")) {
                            if (vcardTable.get("orgAddress") == null)
                                unescapePut(vcardTable, "orgAddress", property.getValue());
                        } else if (vcardTable.get("address") == null) //home, post, etc...
                            unescapePut(vcardTable, "address", property.getValue());
                    }
                }
            }
            else if ("TEL".equals(propertyName)) {
                for (Parameter param : property.getParameters()) {
                    if ("TYPE".equals(param.getId().getPname()) &&
                            param.getValue().toUpperCase().contains("WORK")) {
                        String number = property.getValue();
                        if (number.startsWith("+"))
                            number = number.substring(1);
                        if (param.getValue().toUpperCase().contains("VOICE")) {
                            number = number.replace("/", "-");
                            unescapePut(vcardTable, "tel", number);
                        }
                        else if (param.getValue().toUpperCase().contains("FAX")) {
                            number = number.replace("/", "-");
                            unescapePut(vcardTable, "fax", number);
                        }
                    }
                }
            }
            else
                unescapePut(vcardTable, propertyName.toLowerCase(), property.getValue());
        }
        return vcardTable;
    }

    private static String preAdjustment(String s) throws Exception {
        String res = "";
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(s.getBytes()));
        BufferedReader br = new BufferedReader(reader);
        boolean done = false;
        while (!done) {
            String line = br.readLine();
            if (line != null) {
                //line = line.trim();
                //list of corrections
                if (line.toUpperCase().startsWith("BEGIN:"))
                    res += "BEGIN:VCARD\n";
                else if (line.toUpperCase().startsWith("END:"))
                    res += "END:VCARD";
                else if (!line.startsWith("UID:"))  //do not manage UIDs
                    res += line + "\n";
            }
            else
                done = true;
        }
        return res;
    }

    private static String clearSpacesAndCr(String s, String property) {
        int index = s.indexOf(property);
        if (index != -1) {
            int index2 = indexOfNextVCardProperty(s, index);
            String content = s.substring(index + property.length(), index2);
            content = content.replace(" ", "").replace("\r\n", "").replace("\n", "").replace("\r", "");
            if ("URL:".equals(property) && content.startsWith("."))
                content = content.substring(1);
            s = s.substring(0, index + property.length()) + content + s.substring(index2);
        }
        return s;
    }

    private static String adjustPhoto(String s, String property) {
        int index = s.indexOf(property);
        if (index != -1) {
            int index2 = indexOfNextVCardProperty(s, index);
            String photoContent = s.substring(index + property.length(), index2);
            photoContent = photoContent.replaceAll("\r\n", "").replaceAll("\n", "").replaceAll("\r", "");
            boolean isURL = photoContent.toLowerCase().contains("value=ur");
            if (isURL) {
                photoContent = photoContent.replaceAll(" ", "");
                photoContent = photoContent.replaceAll("=URI", "=uri");
                photoContent = photoContent.replaceAll("=URL", "=uri");
                photoContent = photoContent.replaceAll("=url", "=uri");
                if (!(photoContent.contains("http") || photoContent.contains("ftp"))) { //add http protocol when missing
                    int i3 = photoContent.lastIndexOf(":");
                    photoContent = photoContent.substring(0, i3 + 1) + "http://" + photoContent.substring(i3 + 1);
                }
            }
            s = s.substring(0, index + property.length()) + photoContent + s.substring(index2);
        }
        return s;
    }

    private static String adjustAddress(String s) {
        String property = "ADR;";
        int index = s.indexOf(property);
        if (index == -1) {
            property = "ADR:";
            index = s.indexOf(property);
            if (index != -1 )
                return( adjustAddress( s.replace( "ADR:", "ADR;" ) ) );
        }
        else {
            int index1b = s.indexOf(":", index) + 1;
            int index2 = indexOfNextVCardProperty(s, index1b);
            String adr = s.substring(index1b, index2);
            adr = adr.replaceAll("\r\n ", "");
            adr = adr.replaceAll("\n ", "");
            adr = adr.replaceAll("\r ", "");
            adr = adr.replaceAll(" \r\n", "");
            adr = adr.replaceAll(" \n", "");
            adr = adr.replaceAll(" \r", "");
            adr = adr.replaceAll("\r\n", "");
            adr = adr.replaceAll("\n", "");
            adr = adr.replaceAll("\r", "");

            int i = StringUtil.occurrenceOf(";", adr);
            while (i < 6) {
                adr += ";";
                i++;
            }
            while (i > 6) {
                if (adr.endsWith(";")) //ending treated before center multi ;
                    adr = adr.substring(0, adr.length() - 1);
                else {
                    int indexDbl = adr.indexOf(";;");
                    if (indexDbl != -1)
                        adr = adr.substring(0, indexDbl) + adr.substring(indexDbl + 1);
                }
                i--;
            }
            if (adr.endsWith(";"))
                adr += " ";
            s = s.substring(0, index1b) + adr + s.substring(index2);
        }
        return s;
    }

    private static int indexOfNextVCardProperty(String s, int fromIndex) {
        int res = -1;
        for (Property.Id prop: Property.Id.values()) {
            int i = s.indexOf("\n" + prop.getPropertyName(), fromIndex);
            if ((i != -1) && (res == -1 || i < res))
                res = i;
        }
        if (res == -1)
           res = getIndexOfVcardEnd( s, fromIndex );
        return res;
    }
   
    public static boolean containsAtLeastOneElement(List<String> l1, List<String> l2) {
        if (l1 == null || l2 == null)
            return false;
        
        for (String s: l1) {
            if (l2.contains(s))
                return true;
        }
        return false;
    }

    public static boolean substringOfAtLeastOneElement(List<String> l1, List<String> l2) {
        if (l1 == null || l2 == null)
            return false;

        for (String s1: l1) {
            for (String s2: l2) {
                if (s1.contains(s2))
                    return true;
            }
        }
        return false;
    }

    //Entity manipulation
    public static Entity getEntity(String uri) throws Exception{
        Triple[] res = tripleStore.getTriplesWithSubjectPredicate(uri, RDF.type);
        if (res.length == 0)
            return null;

        Triple type = res[0];
        boolean isPerson = COMETE.Person.getURI().equals(type.getObject());
        Entity entity = isPerson?new Person(uri):new Organization(uri);

        Triple[] triples = tripleStore.getTriplesWithSubject(uri);
        for (Triple triple : triples) {
            if (ORG.memberOf.getURI().equals(triple.getPredicate()) ||
                    Constants.IDENTITY_PREDICATES.contains(triple.getPredicate()) ||
                        Constants.IDENTITY_ALT_PREDICATES.contains(triple.getPredicate()))
                entity.addValue(triple.getPredicate(), triple.getObject());
        }

        return entity;
    }

    public static Object[] extractPertinentTermsFromOrg(String orgName) throws Exception {
        String clearedName = clearOrgName(orgName);

        //remove educational stop words
        String result = "";
        String[] clearedTerms = clearedName.split(" ");
        String delimiter = "";
        for (String term : clearedTerms) {
            if (!"".equals(term) && !ORGANIZATION_STOP_WORDS_EDUC.contains(term)) {
                result += delimiter + term;
                delimiter = " ";
            }
        }

        //if all terms are stop words, all must be present
        if ("".equals(result))
            result = clearedName.replace(" ", " AND ");

        return new Object[] {clearedTerms, result};
    }

    /**
     * remove non-alphanumerical characters, accents and possible stop words of org names
     * @param name
     * @return cleared org name
     */
    public static String clearOrgName(String name) {
        name = name.trim().toLowerCase();
        name = StringUtil.removeAccents(name);
        String orgTerms = ca.licef.comete.core.util.Util.formatKeywords(name);
        String clearedName = "";
        String[] terms = orgTerms.split(" ");
        String delimiter = "";
        for (String term : terms) {
            if (!"".equals(term) && !ORGANIZATION_STOP_WORDS.contains(term)) {
                clearedName += delimiter + term;
                delimiter = " ";
            }
        }
        return clearedName;
    }

    /**
     * All terms of smaller list must be present (or their synonyms) in other list
     * @param terms1
     * @param terms2
     * @return
     */
    public static boolean isSimilarOrgNames(String[] terms1, String[] terms2) {
        String[] termsSmall = terms1;
        String[] termsLarge = terms2;
        if (terms1.length > terms2.length) {
            termsSmall = terms2;
            termsLarge = terms1;
        }

        for (int i = 0; i < termsSmall.length; i++) {
            String term1 = termsSmall[i];
            boolean present = false;
            for (int j = 0; j < termsLarge.length; j++) {
                String term2 = termsLarge[j];
                if (isSynonym(term1, term2)) {
                    present = true;
                    break;
                }
            }
            if (!present)
                return false;
        }
        return true;
    }

    public static boolean isSynonym(String t1, String t2) {
        if (t1.equals(t2))
            return true;
        for (String synonyms : EDUC_SYNONYMS) {
            if (synonyms.contains(t1 + ";") &&
                synonyms.contains(t2 + ";"))
                return true;
        }
        return false;
    }

    public static void insertFirst(JSONArray array, JSONObject obj) {
        try {
            ArrayList<JSONObject> l = new ArrayList<JSONObject>();
            for (int i = 0; i < array.length(); i++)
                l.add((JSONObject)array.get(i));
            array.put(0, obj);
            int i = 1;
            for (JSONObject o : l) {
                array.put(i, o);
                i++;
            }
        } catch (JSONException e) {
           e.printStackTrace();
        }
    }

    private static int getIndexOfVcardEnd( String str, int fromIndex ) {
        Matcher m = patternVcardEnd.matcher( str );
        return( m.find( fromIndex ) ? m.start() : -1 );
    }

    private static final Pattern patternVcardEnd = Pattern.compile( "\n.*END:VCARD" );

}
