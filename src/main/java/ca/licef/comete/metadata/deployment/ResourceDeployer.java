package ca.licef.comete.metadata.deployment;

import java.io.File;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 14-07-28
 */
public abstract class ResourceDeployer {

    public static ResourceDeployer instantiate(ResourceBundle bundle) {
        String target = bundle.getString("target");
        ResourceDeployer deployer = null;
        if ("FEDORA".equals(target))
            deployer =  new FedoraDeployer(bundle.getString("url"), bundle.getString("username"), bundle.getString("password"));

        return deployer;
    }

    public abstract String deploy(File f, String... data) throws Exception;
}
