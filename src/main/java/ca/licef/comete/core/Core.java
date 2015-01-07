package ca.licef.comete.core;

import ca.licef.comete.vocabularies.COMETE;
import ca.licef.comete.vocabulary.Vocabulary;

import licef.reflection.Invoker;
import licef.reflection.ThreadInvoker;
import licef.tsapi.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


/**
 * Created by IntelliJ IDEA.
 * User: amiara
 * Date: 8-Sep-2011
 */
public class Core {
    private static Core core;

    private TripleStore tripleStore;

    private ResourceView defaultView;

    private String cometeHome;
    private String repositoryName;
    private String adminEmail;
    private String version;
    private String cometeUrl;
    private String uriPrefix;
    private String sparqlEndpoint;
    private String smtpHost;

    private static boolean initProcess = false;

    public static Core getInstance() {
        if (core == null || !initProcess) {
            initProcess = true;
            core = new Core();
        }
        return core;
    }

    private Core() {
        try {
            //to hide all Jena debug traces
            Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) ;
            ((ch.qos.logback.classic.Logger)logger).setLevel(ch.qos.logback.classic.Level.INFO);

            ResourceBundle resBundle = ResourceBundle.getBundle("/conf/core");
            cometeHome = resBundle.getString("comete.home");
            uriPrefix = resBundle.getString("comete.uri.prefix");
            cometeUrl = resBundle.getString("comete.url");
            repositoryName = resBundle.getString("comete.repositoryName");
            adminEmail = resBundle.getString("comete.admin.email");
            version = resBundle.getString("comete.version");
            sparqlEndpoint = resBundle.getString("sparql.endpoint");
            smtpHost = resBundle.getString("smtp.host");

            initTripleStore();

            (new ThreadInvoker(new Invoker(Vocabulary.getInstance(),
                    "ca.licef.comete.vocabulary.Vocabulary",
                        "initVocabularyModule", new Object[]{}))).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Repository info
     */

    public String getCometeHome() {
        return cometeHome;
    }

    public String getCometeReportsHome() {
        return getCometeHome() + "/reports";
    }

    public String getRepositoryName() {
        return( repositoryName );
    }

    public String getAdminEmail() {
        return( adminEmail );
    }

    public String getVersion() {
        return( version );
    }

    public String getCometeUrl() {
        return( cometeUrl );
    }

    public String getUriPrefix() {
        return( uriPrefix );
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    /*
     * Triple Store services
     */
    private void initTripleStore() {
        if (tripleStore == null) {
            tripleStore = new TripleStore(cometeHome + "/database", cometeHome, getUriPrefix() + "/");
            tripleStore.registerVocabulary("http://comete.licef.ca/reference#", COMETE.class);
            tripleStore.startServer(true);
            waitTripleStoreUp();
        }
    }

    public TripleStore getTripleStore() {
        if (tripleStore == null)
            initTripleStore();
        return tripleStore;
    }

    private void waitTripleStoreUp() {
        try {
            URL endpoint = new URL(sparqlEndpoint);
            String host = endpoint.getHost();
            int port = endpoint.getPort();
            URL server = new URL("http://" + host + ((port != -1)?":"+port:""));
            boolean up = false;
            while (!up) {
                try {
                    server.getContent();
                    up = true;
                    Thread.sleep(100);
                } catch (IOException e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    * Default implementation of ResourceView
    */
    public ResourceView getDefaultView() {
        if (defaultView == null)
            defaultView = new DefaultView();
        return defaultView;
    }

}
