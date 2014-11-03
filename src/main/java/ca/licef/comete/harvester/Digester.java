package ca.licef.comete.harvester;


import ca.licef.comete.core.util.Constants;
import ca.licef.comete.core.util.Util;
import ca.licef.comete.metadata.Metadata;
import ca.licef.comete.metadata.RepositoryManager;

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

        /*WebResource webResource = Core.getInstance().getRestClient().resource( harvestRestURL + URLEncoder.encode(oaiID) );
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add( "namespace", namespace );
        queryParams.add( "datestamp", datestamp );
        queryParams.add( "repoId", repoId );
        ClientResponse response = webResource.queryParams( queryParams ).put(ClientResponse.class, record);
        int status = response.getStatus();
        String res = response.getEntity(String.class);
        if ("added".equals(res))
            return ADDED;
        else if ("updated".equals(res))
            return UPDATED;
        else
            return IGNORED;*/

        String repoUri = Util.makeURI(repoId, Constants.TYPE_REPOSITORY);
        String res = Metadata.getInstance().storeHarvestedRecord(oaiID, namespace, repoUri, record, datestamp, false);
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
