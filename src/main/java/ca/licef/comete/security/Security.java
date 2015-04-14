package ca.licef.comete.security;

import ca.licef.comete.core.Core;
import licef.IOUtil;
import licef.Sha1Util;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-04-10
 */
public class Security {

    static File settingsDir = new File(Core.getInstance().getCometeHome(), "/conf/security");
    static File passwordFile = new File(settingsDir, "adminPassword.txt");

    private static Security instance;

    public static Security getInstance() {
        if (instance == null)
            instance = new Security();
        return (instance);
    }

    public Security() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void init() throws Exception {
        if (!settingsDir.exists())
            IOUtil.createDirectory(settingsDir.getAbsolutePath());

        if (!passwordFile.exists()) {
            //for them moment, default admin password is sha1 of "admin" string
            IOUtil.writeStringToFile(Sha1Util.hash("admin"), passwordFile);
        }

    }

    public boolean validatePassword(String password) throws Exception {
        String adminPasswordSha1 = IOUtil.readStringFromFile(passwordFile);
        String hashedPassword = Sha1Util.hash(password);
        return hashedPassword.equals(adminPasswordSha1);
    }

    public boolean isAuthorized( HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession( true );
        String login = (String)session.getAttribute( "login" );
        return( "admin".equals( login ) );
    }

}
