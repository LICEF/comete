package ca.licef.comete.security;

import ca.licef.comete.core.Core;
import licef.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-04-10
 */
public class Security {

    private static Security instance;

    public static Security getInstance() {
        if (instance == null)
            instance = new Security();
        return (instance);
    }

    public Security() {
    }

    public boolean isAuthorized( HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession( true );
        String login = (String)session.getAttribute( "login" );
        return( "admin".equals( login ) );
    }
}
