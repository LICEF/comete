package ca.licef.comete.core.metadataformat;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 16-Sep-2011
 */
public interface MetadataFormat {

    public String getName();
    public String getNamespace();
    String getIdentityMimetypeFormat();
    String getExposedDatastream();
}
