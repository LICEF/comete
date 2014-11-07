package ca.licef.comete.core.metadataformat;

import ca.licef.comete.core.util.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 16-Sep-2011
 */
public class IEEE_LOM implements MetadataFormat {

    public String getName() {
        return "lom";
    }

    public String getNamespace() {
        return Constants.IEEE_LOM_NAMESPACE;
    }

    public String getIdentityMimetypeFormat() {
        return Constants.VCARD_MIMETYPE;
    }

    public String getExposedDatastream() {
        return Constants.DATASTREAM_EXPOSED_DATA_LOM;
    }

}
