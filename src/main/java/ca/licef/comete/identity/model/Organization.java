package ca.licef.comete.identity.model;

import ca.licef.comete.identity.util.Util;
import ca.licef.comete.vocabularies.COMETE;
import ezvcard.Ezvcard;
import ezvcard.VCardVersion;
import ezvcard.parameters.AddressTypeParameter;
import ezvcard.parameters.EmailTypeParameter;
import ezvcard.parameters.TelephoneTypeParameter;
import ezvcard.types.AddressType;
import ezvcard.types.KindType;
import ezvcard.types.StructuredNameType;
import licef.tsapi.vocabulary.FOAF;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 2-Nov-2011
 */
public class Organization extends Entity {

    private String uri;
    private ArrayList<String> names = new ArrayList<String>();
    private ArrayList<String> emails = new ArrayList<String>();
    private ArrayList<String> addresses = new ArrayList<String>();
    private ArrayList<String> tels = new ArrayList<String>();
    private ArrayList<String> urls = new ArrayList<String>();
    private ArrayList<String> logos = new ArrayList<String>();


    public Organization(String uri) {
        this.uri = uri;

        data.put(FOAF.name.getURI(), names);
        data.put(COMETE.formattedAddress.getURI(), addresses);
        data.put(FOAF.mbox.getURI(), emails);
        data.put(FOAF.phone.getURI(), tels);
        data.put(FOAF.homepage.getURI(), urls);
        data.put(FOAF.logo.getURI(), logos);
    }

    public String getUri() {
        return uri;
    }

    public String getFN() {
       return getName();
    }

    public boolean isSameType(Entity en) {
        return this.getClass().equals(en.getClass());
    }

    public boolean hasJustFN() {
        ArrayList populated = getPopulated();
        return (populated.size() == 1 && populated.get(0) == names);
    }

    public boolean equals(Object anObject) {
        if (this == anObject)
            return true;
        if (anObject instanceof Organization) {
            Organization o = (Organization)anObject;
            if (hasJustFN() && o.hasJustFN())
                return Util.containsAtLeastOneElement(names, o.getList(FOAF.name.getURI()));
            if (Util.containsAtLeastOneElement(names, o.getList(FOAF.name.getURI())) &&
                Util.containsAtLeastOneElement(emails, o.getList(FOAF.mbox.getURI())))
                return true;
        }
        return false;
    }

    public String getName() {
        return getMainValue(names);
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

    public String getLogo() {
        return getMainValue(logos);
    }

    public String getVCard(String uri) throws Exception {
        ezvcard.VCard vcard = new ezvcard.VCard();

        String email = getEmail();
        String url = getUrl();
        String tel = getTel();
        String fax = getFax();
        String address = getAddress();

        vcard.setFormattedName("");  //FN set empty to explicitly let interpretation with ORG field
        vcard.setStructuredName(new StructuredNameType());  //N empty
        vcard.setKind(KindType.org()); //only used in vcard 4.0 :-(
        vcard.setOrganization(getFN());  //ORG

        if (email != null) vcard.addEmail(email.substring("mailto:".length()), EmailTypeParameter.INTERNET); //EMAIL
        if (url != null) vcard.addUrl(url); //URL
        if (tel != null) vcard.addTelephoneNumber(tel.substring("tel:".length()), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE); //TEL
        if (fax != null) vcard.addTelephoneNumber(fax.substring("fax:".length()), TelephoneTypeParameter.WORK, TelephoneTypeParameter.FAX); //TEL (fax)
        if (address != null) {
            AddressType a = new AddressType();
            String[] addrValues = address.split(";");
            if (addrValues.length >= 1 && !"".equals(addrValues[0])) a.setPoBox(addrValues[0].trim());
            if (addrValues.length >= 2 && !"".equals(addrValues[1])) a.setExtendedAddress(addrValues[1].trim());
            if (addrValues.length >= 3 && !"".equals(addrValues[2])) a.setStreetAddress(addrValues[2].trim());
            if (addrValues.length >= 4 && !"".equals(addrValues[3])) a.setLocality(addrValues[3].trim());
            if (addrValues.length >= 5 && !"".equals(addrValues[4])) a.setRegion(addrValues[4].trim());
            if (addrValues.length >= 6 && !"".equals(addrValues[5])) a.setPostalCode(addrValues[5].trim());
            if (addrValues.length >= 7 && !"".equals(addrValues[6])) a.setCountry(addrValues[6].trim());
            a.addType(AddressTypeParameter.POSTAL);
            a.addType(AddressTypeParameter.WORK);
            vcard.addAddress(a);
        }

        String logo = getLogo();
        if (logo != null) {
           /* String mt = IOUtil.getMimeType(logo);
            ImageTypeParameter imageType = ImageTypeParameter.valueOf(mt.substring(mt.indexOf("/") + 1));
            if (logo.startsWith(Core.getInstance().getIdentityUrl())) { //binary
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


        String strVCard = Ezvcard.write(vcard).version(VCardVersion.V3_0).go();

        //removing prodid line -AM
        strVCard = strVCard.replace("PRODID:ez-vcard 0.8.1\r\n", "");

        return strVCard;
    }


}
