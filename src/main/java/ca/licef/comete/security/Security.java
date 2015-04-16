package ca.licef.comete.security;

import ca.licef.comete.core.Core;
import licef.IOUtil;
import licef.Sha1Util;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-04-10
 */
public class Security {

    static File settingsDir = new File(Core.getInstance().getCometeHome(), "/conf/security");

    //authorization levels
    public static final String ADMIN_ROLE = "admin";
    public static final String PUBLISHER_ROLE = "publisher";
    public static final String CONTRIBUTOR_ROLE = "contributor";
    public static final String NONE_ROLE = "none";

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
        if (!settingsDir.exists()) {
            IOUtil.createDirectory(settingsDir.getAbsolutePath());
            URL folder = getClass().getResource("/conf/security");
            IOUtil.copyFiles(new File(folder.toURI()), settingsDir);
        }
    }

    public String authenticate(String login, String password) throws Exception {
        File account = new File(settingsDir, login + ".txt");
        Vector lines = IOUtil.readLines(account);
        String accountPasswordSha1 = (String)lines.get(0);
        String hashedPassword = Sha1Util.hash(password);
        if (hashedPassword.equals(accountPasswordSha1))
            return (String)lines.get(1);
        else
            return null;
    }

    public String getRole(HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession( true );
        String role = (String)session.getAttribute( "role" );
        if (role == null || "".equals(role))
            role = NONE_ROLE;
        return role;
    }

    public boolean isAuthorized(HttpServletRequest req) throws Exception {
        String accountAccess = getRole(req);
        return accountAccess.equals(ADMIN_ROLE);
    }

    public boolean isContributeAuthorized(HttpServletRequest req) throws Exception {
        String accountAccess = getRole(req);
        return ( accountAccess.equals(ADMIN_ROLE) ||
                 accountAccess.equals(PUBLISHER_ROLE) ||
                 accountAccess.equals(CONTRIBUTOR_ROLE) );
    }
}
