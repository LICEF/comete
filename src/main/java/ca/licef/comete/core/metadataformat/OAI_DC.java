package ca.licef.comete.core.metadataformat;

import ca.licef.comete.core.util.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 16-Sep-2011
 */
public class OAI_DC implements MetadataFormat {

    public String getName() {
        return "dc";         
    }

    public String getNamespace() {
        return Constants.OAI_DC_NAMESPACE;
    }

    public String getIdentityMimetypeFormat() {
        return Constants.TEXT_MIMETYPE;
    }

    public String getExposedDatastream() {
        return Constants.DATASTREAM_EXPOSED_DATA_DC;
    }

}
