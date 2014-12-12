package ca.licef.comete.queryengine;

import ca.licef.comete.core.Core;
import licef.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 7-Nov-2012
 */

public class Collection {

    static File settingDir = new File(Core.getInstance().getCometeHome(), "/conf/collections");
    static File definitions = new File(settingDir, "collections.ini");

    public Collection() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void init() throws Exception {
        if (!settingDir.exists())
            IOUtil.createDirectory(settingDir.getAbsolutePath());

        if (!definitions.exists()) {
            InputStream is = getClass().getResourceAsStream("/sample-collections.ini");
            OutputStream os = new FileOutputStream(definitions);
            IOUtil.copy(is, os);
            is.close();
            os.close();
        }
    }

    public List<String[]> getAll(String lang) throws Exception {
        File def = definitions;
        File lgDef = new File( definitions.getParentFile(), lang + "_" + definitions.getName());
        if (lgDef.exists())
            def = lgDef;
        ArrayList<String[]> collections = new ArrayList<String[]> ();

        Vector v = IOUtil.readLines(def);
        for (Enumeration e = v.elements(); e.hasMoreElements();) {
            String line = ((String)e.nextElement()).trim();
            if (line.startsWith("#") || "".equals(line))
                continue;
            String[] keyVal = line.split("=");
            collections.add(new String[]{keyVal[0], keyVal[1]});
        }
        return collections;
    }

    public void addCollection(String label, String query) throws Exception {
        String content = IOUtil.readStringFromFile(definitions);
        content += "\r\n" + label + "=" + query;
        IOUtil.writeStringToFile(content, definitions);
    }

    public String getQuery(int index, String lang) throws Exception {
        List<String[]> collections = QueryEngine.getInstance().getCollection().getAll(lang);
        return collections.get(index)[1];
    }
}
