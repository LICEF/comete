package ca.licef.comete.core;

import ca.licef.comete.vocabularies.COMETE;
import licef.tsapi.TripleStore;

import java.util.ResourceBundle;


/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 8-Sep-2011
 */
public class Core {
    private static Core core;

    private TripleStore tripleStore;

    private String cometeHome;
    private String adminEmail;
    private String version;
    private String uriPrefix;
    private String smtpHost;

    public static Core getInstance() {
        if (core == null)
            core = new Core();
        return core;
    }

    private Core() {
        try {
            ResourceBundle resBundle = ResourceBundle.getBundle("core");
            cometeHome = resBundle.getString("comete.home");
            uriPrefix = resBundle.getString("comete.uriPrefix");
            adminEmail = resBundle.getString("comete.admin.email");
            version = resBundle.getString("comete.version");
            smtpHost = resBundle.getString("smtp.host");

            initTripleStore();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key) {
        try {
            ResourceBundle resBundle = ResourceBundle.getBundle("core");
            return resBundle.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * Repository info
     */

    public String getCometeHome() {
        return cometeHome;
    }

    public String getAdminEmail() {
        return( adminEmail );
    }

    public String getVersion() {
        return( version );
    }

    public String getUriPrefix() {
        return( uriPrefix );
    }

    public String getSmtpHost() {
        return smtpHost;
    }


    /*
     * Triple Store services
     */
    private void initTripleStore() {
        if (tripleStore == null) {
            tripleStore = new TripleStore(cometeHome + "/database", cometeHome, getUriPrefix());
            tripleStore.registerVocabulary("http://comete.licef.ca/reference#", COMETE.class);
            tripleStore.startServer(true);
        }
    }

    public TripleStore getTripleStore() {
        if (tripleStore == null)
            initTripleStore();
        return tripleStore;
    }
}
