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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-04-10
 */
public class Security {

    static File settingsDir = new File(Core.getInstance().getCometeHome(), "/conf/security");

    //authorization levels
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

    public Role authenticate(String sessionId, String login, String encryptedPassword) throws Exception {
        File account = new File(settingsDir, login + ".txt");
        Vector lines = IOUtil.readLines(account);
        String accountPasswordSha1 = (String)lines.get(0);
        String encryptedAccountPassword = Sha1Util.hash(sessionId + accountPasswordSha1);
        if (encryptedPassword.equals(encryptedAccountPassword))
            return Role.get( (String)lines.get(1) );
        else
            return Role.NONE;
    }

    public Role getRole(HttpServletRequest req) {
        HttpSession session = req.getSession( true );
        Role role = (Role)session.getAttribute( "role" );
        if (role == null )
            role = Role.NONE;
        return role;
    }

    public boolean isAuthorized(HttpServletRequest req) {
        Role accountAccess = getRole(req);
        return accountAccess.equals(Role.ADMIN);
    }

    public boolean isAuthorized(HttpServletRequest req, Role[] roles ) {
        Role userRole = getRole( req );
        Set<Role> roleSet = new HashSet<Role>( Arrays.asList( roles ) );
        return( roleSet.contains( userRole ) );
    }

    public boolean isContributeAuthorized(HttpServletRequest req) {
        Role accountAccess = getRole(req);
        return ( accountAccess.equals(Role.ADMIN) ||
                 accountAccess.equals(Role.PUBLISHER) ||
                 accountAccess.equals(Role.CONTRIBUTOR) );
    }
}
