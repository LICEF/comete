package ca.licef.comete.vocabulary.util;

import ca.licef.comete.core.Core;
import ca.licef.comete.core.util.Constants;
import licef.IOUtil;
import licef.reflection.Invoker;
import licef.StringUtil;
import licef.XMLUtil;
import licef.tsapi.vocabulary.SKOS;
import licef.tsapi.TripleStore;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;

/**
 * Created by amiara on 2014-11-07.
 */
public class Util {

    public static final int SKOS_FORMAT = 1;
    public static final int VDEX_FORMAT = 2;

    public static int getVocabularyFormatFromLocation(String location) throws Exception {
        String content = (IOUtil.isURL(location))?IOUtil.readStringFromURL(new URL(location)):
                IOUtil.readStringFromFile(new File(location));
        return getVocabularyFormat(content);
    }

    public static int getVocabularyFormat(String vocContent) throws Exception {
        String rootname = null;
        Hashtable namespaces = null;
        try {
            rootname = XMLUtil.getRootTagName(vocContent);
            namespaces = XMLUtil.getAttributes(vocContent, "/");
        } catch (Exception e) {
            return -1;
        }
        String[] array = StringUtil.split(rootname, ':');
        rootname = array[array.length - 1].toLowerCase();
        if ("rdf".equals(rootname) && namespaces.containsValue(SKOS.NS))
            return SKOS_FORMAT;
        else if ("vdex".equals(rootname) && namespaces.containsValue(Constants.VDEX_NAMESPACE))
            return VDEX_FORMAT;
        else
            return -1;
    }

    public static boolean isGraphExists(String uri) throws Exception {
        TripleStore tripleStore = Core.getInstance().getTripleStore();
        Invoker inv = new Invoker( tripleStore, "licef.tsapi.TripleStore", "getNamedGraphs", new Object[] {} );
        String[] names = (String[])tripleStore.transactionalCall( inv );
        for (String name : names)
            if (uri.equals(name))
                return true;
        return false;
    }
}
