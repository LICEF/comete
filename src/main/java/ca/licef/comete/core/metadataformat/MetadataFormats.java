package ca.licef.comete.core.metadataformat;

import ca.licef.comete.core.util.Constants;

import java.util.Collection;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 16-Sep-2011
 */
public class MetadataFormats {

    private static MetadataFormats instance;

    static MetadataFormats getInstance(){
        if (instance == null)
            instance = new MetadataFormats();
        return instance;
    }

    public MetadataFormats() {
        IEEE_LOM lom = new IEEE_LOM();
        formatsByName.put("lom", lom);
        formatsByNamespace.put(Constants.IEEE_LOM_NAMESPACE, lom);

        OAI_DC dc = new OAI_DC();
        formatsByName.put("dc", dc);
        formatsByNamespace.put(Constants.OAI_DC_NAMESPACE, dc);
    }

    public static MetadataFormat getMetadataFormat(String namespace) {
        return getInstance().formatsByNamespace.get(namespace);
    }

    public static MetadataFormat getMetadataFormatFromName(String name) {
        return getInstance().formatsByName.get(name);
    }

    public static MetadataFormat getMetadataFormatFromDatastream(String datastream) {
        return getInstance().formatsByDatastream.get(datastream);
    }

    public static Collection<MetadataFormat> getMetadataFormats() {
        return getInstance().formatsByNamespace.values();
    }

    private Hashtable<String, MetadataFormat> formatsByName = new Hashtable<String, MetadataFormat>();
    private Hashtable<String, MetadataFormat> formatsByNamespace = new Hashtable<String, MetadataFormat>();
    private Hashtable<String, MetadataFormat> formatsByDatastream = new Hashtable<String, MetadataFormat>();
}
