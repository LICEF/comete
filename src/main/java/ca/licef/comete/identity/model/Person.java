package ca.licef.comete.identity.model;

import ca.licef.comete.core.Core;
import ca.licef.comete.identity.Identity;
import ca.licef.comete.identity.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import ezvcard.Ezvcard;
import ezvcard.VCardVersion;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Kind;
import ezvcard.property.StructuredName;
import licef.IOUtil;
import licef.tsapi.model.Triple;
import licef.tsapi.vocabulary.DCTERMS;
import licef.tsapi.vocabulary.FOAF;
import licef.tsapi.vocabulary.ORG;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 2-Nov-2011
 */
public class Person extends Entity {

    private String uri;
    private ArrayList<String> formattedNames = new ArrayList<String>();
    private ArrayList<String> firstnames = new ArrayList<String>();
    private ArrayList<String> lastnames = new ArrayList<String>();
    private ArrayList<String> emails = new ArrayList<String>();
    private ArrayList<String> addresses = new ArrayList<String>();
    private ArrayList<String> tels = new ArrayList<String>();  //fax included in tel list
    private ArrayList<String> urls = new ArrayList<String>();
    private ArrayList<String> photos = new ArrayList<String>();
    private ArrayList<String> orgs = new ArrayList<String>();

    public Person(String uri) {
        this.uri = uri;

        data.put(FOAF.name.getURI(), formattedNames);
        data.put(FOAF.givenName.getURI(), firstnames);
        data.put(FOAF.familyName.getURI(), lastnames);
        data.put(FOAF.mbox.getURI(), emails);
        data.put(COMETE.formattedAddress.getURI(), addresses);
        data.put(FOAF.phone.getURI(), tels);
        data.put(FOAF.homepage.getURI(), urls);
        data.put(FOAF.img.getURI(), photos);
        data.put(ORG.memberOf.getURI(), orgs);
    }

    public String getUri() {
        return uri;
    }

    public String getFN() {
       return getFormattedName();
    }

    public boolean isSameType(Entity en) {
        return this.getClass().equals(en.getClass());
    }

    public boolean hasJustFN() {
        ArrayList populated = getPopulated();
        return (populated.size() == 1 && populated.get(0) == formattedNames);
    }

    public boolean equals(Object anObject) {
        if (this == anObject)
            return true;
        if (anObject instanceof Person) {
            Person p = (Person)anObject;
            if (hasJustFN() && p.hasJustFN())
                return Util.containsAtLeastOneElement(formattedNames, p.getList(FOAF.name.getURI()));
            if (Util.containsAtLeastOneElement(lastnames, p.getList(FOAF.givenName.getURI())) &&
                Util.containsAtLeastOneElement(firstnames, p.getList(FOAF.familyName.getURI())) &&
                Util.containsAtLeastOneElement(emails, p.getList(FOAF.mbox.getURI())))
                return true;
        }
        return false;
    }

    public Organization toOrganization() {
        Organization org = new Organization(uri);
        int i = 0;
        for (String name: formattedNames) {
            org.addValue(FOAF.name.getURI(), name, i == 0);
            i++;
        }
        return org;
    }

    public String getFormattedName() {
        return getMainValue(formattedNames);
    }

    public String getFirstname() {
        return getMainValue(firstnames);
    }

    public String getLastname() {
        return getMainValue(lastnames);
    }

    public String getEmail() {
        return getMainValue(emails);
    }

    public String getAddress() {
        return getMainValue(addresses);
    }

    public String getTel() {
        return getMainValue(tels, "tel:");
    }

    public String getFax() {
        return getMainValue(tels, "fax:");
    }

    public String getUrl() {
        return getMainValue(urls);
    }

    public String getPhoto() {
        return getMainValue(photos);
    }

    public ArrayList<String> getOrgs() {
        return orgs;
    }

    public String getVCard(String uri, String loUri) throws Exception {
        ezvcard.VCard vcard = new ezvcard.VCard();

        String email = getEmail();
        String url = getUrl();
        String tel = getTel();
        String fax = getFax();
        String address = getAddress();

        vcard.setFormattedName(getFN()); //FN
        StructuredName n = new StructuredName();
        vcard.setKind(Kind.individual()); //only used in vcard 4.0 :-(
        String firstname = getFirstname();
        String lastname = getLastname();
        String photo = getPhoto();
        if (firstname != null) n.setGiven(firstname);
        if (lastname != null) n.setFamily(lastname);
        vcard.setStructuredName(n);  //N

        if (email != null) vcard.addEmail(email.substring("mailto:".length()), EmailType.INTERNET); //EMAIL
        if (url != null) vcard.addUrl(url); //URL
        if (tel != null) vcard.addTelephoneNumber(tel.substring("tel:".length()), TelephoneType.WORK, TelephoneType.VOICE); //TEL
        if (fax != null) vcard.addTelephoneNumber(fax.substring("fax:".length()), TelephoneType.WORK, TelephoneType.FAX); //TEL (fax)
        Address a = null;
        if (address != null) {
            a = new Address();
            String[] addrValues = address.split(";");
            if (addrValues.length >= 1 && !"".equals(addrValues[0])) a.setPoBox(addrValues[0].trim());
            if (addrValues.length >= 2 && !"".equals(addrValues[1])) a.setExtendedAddress(addrValues[1].trim());
            if (addrValues.length >= 3 && !"".equals(addrValues[2])) a.setStreetAddress(addrValues[2].trim());
            if (addrValues.length >= 4 && !"".equals(addrValues[3])) a.setLocality(addrValues[3].trim());
            if (addrValues.length >= 5 && !"".equals(addrValues[4])) a.setRegion(addrValues[4].trim());
            if (addrValues.length >= 6 && !"".equals(addrValues[5])) a.setPostalCode(addrValues[5].trim());
            if (addrValues.length >= 7 && !"".equals(addrValues[6])) a.setCountry(addrValues[6].trim());
            a.addType(AddressType.POSTAL);
            vcard.addAddress(a);
        }

        if (photo != null) {
            String mt = IOUtil.getMimeType(photo);
            //ImageType imageType = ImageType.valueOf(mt.substring(mt.indexOf("/") + 1));
            ImageType imageType = ImageType.get(null, mt, null);
            /*if (photo.startsWith(Core.getInstance().getIdentityUrl())) { //binary
                byte[] _photo = Identity.getInstance().getPhoto(photo);
                if (_photo != null) {
                    PhotoType p = new PhotoType();
                    p.setData(_photo, imageType); //ez-vcard will encode in base64 -AM
                    vcard.addPhoto(p);
                }
            }
            else { //URL
                PhotoType p = new PhotoType();
                p.setUrl(photo, imageType);
                vcard.addPhoto(p);
            }*/
        }

        if (loUri != null) { //associated organization
            Triple[] triples = Core.getInstance().getTripleStore().getTriplesWithSubjectPredicate(loUri, DCTERMS.publisher);
            String relatedOrg = null;
            for (Triple t : triples) {
                if (getOrgs().contains(t.getObject())) { //the first match will be the associated org -AM
                    relatedOrg = t.getObject();
                    break;
                }
            }
            if (relatedOrg != null) {
                Organization organization = (Organization)Util.getEntity(relatedOrg);
                String orgUrl = organization.getUrl();
                String orgAddress = organization.getAddress();

                vcard.setOrganization(organization.getFN());  //ORG
                if (orgUrl != null) vcard.addUrl(orgUrl); //URL
                if (orgAddress != null) {
                    Address oa = new Address();
                    String[] addrValues = orgAddress.split(";");
                    if (addrValues.length >= 1 && !"".equals(addrValues[0])) oa.setPoBox(addrValues[0].trim());
                    if (addrValues.length >= 2 && !"".equals(addrValues[1])) oa.setExtendedAddress(addrValues[1].trim());
                    if (addrValues.length >= 3 && !"".equals(addrValues[2])) oa.setStreetAddress(addrValues[2].trim());
                    if (addrValues.length >= 4 && !"".equals(addrValues[3])) oa.setLocality(addrValues[3].trim());
                    if (addrValues.length >= 5 && !"".equals(addrValues[4])) oa.setRegion(addrValues[4].trim());
                    if (addrValues.length >= 6 && !"".equals(addrValues[5])) oa.setPostalCode(addrValues[5].trim());
                    if (addrValues.length >= 7 && !"".equals(addrValues[6])) oa.setCountry(addrValues[6].trim());
                    oa.addType(AddressType.POSTAL);
                    oa.addType(AddressType.WORK);
                    vcard.addAddress(oa);
                }

                String logo = organization.getLogo();
                if (logo != null) {
                    String mt = IOUtil.getMimeType(logo);
                    //ImageType imageType = ImageType.valueOf(mt.substring(mt.indexOf("/") + 1));
                    ImageType imageType = ImageType.get(null, mt, null);
                    /*if (logo.startsWith(Core.getInstance().getIdentityUrl())) { //binary
                        byte[] _logo = Identity.getInstance().getPhoto(logo);
                        if (_logo != null) {
                            LogoType l = new LogoType();
                            l.setData(_logo, imageType); //ez-vcard will encode in base64 -AM
                            vcard.addLogo(l);
                        }
                    }
                    else { //URL
                        LogoType l = new LogoType();
                        l.setUrl(logo, imageType);
                        vcard.addLogo(l);
                    }*/
                }
            }
        }

        String strVCard = Ezvcard.write(vcard).version(VCardVersion.V3_0).go();

        //removing prodid line -AM
        strVCard = strVCard.replace("PRODID:ez-vcard 0.8.1\r\n", "");

        return strVCard;
    }

}
