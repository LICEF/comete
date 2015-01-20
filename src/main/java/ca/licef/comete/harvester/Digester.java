package ca.licef.comete.harvester;


import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.Metadata;
import ca.licef.comete.metadata.RepositoryManager;
import ca.licef.comete.vocabularies.COMETE;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 14-04-02
 */

public class Digester {

    public static final int IGNORED     = -1;
    public static final int ADDED       = 0;
    public static final int UPDATED     = 1;
    public static final int DELETED     = 2;

    public void addOrUpdateRepository(String name, String type, String url, String repoId, String adminEmail) throws Exception {
        RepositoryManager.getInstance().addOrUpdateRepository(repoId, name, type, url, adminEmail);
    }

    public int addOrUpdateHarvestedRecord( String oaiID, String namespace, String datestamp, String repoId, String record ) throws Exception {
        String repoUri = Util.makeURI(repoId, COMETE.Repository );
        String res = Metadata.getInstance().storeHarvestedRecord(oaiID, namespace, repoUri, record, datestamp);
        if ("added".equals(res))
            return ADDED;
        else if ("updated".equals(res))
            return UPDATED;
        else
            return IGNORED;
    }

    public int deleteHarvestedRecord( String oaiID, String namespace ) throws Exception {
        String res = Metadata.getInstance().deleteHarvestedRecord(oaiID, namespace);
        if ("deleted".equals(res))
            return DELETED;
        else
            return IGNORED;
    }
}
