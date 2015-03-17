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

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-04-10
 */
public class Security {

    static File settingDir = new File(Core.getInstance().getCometeHome(), "/conf/security");
    static File authorized = new File(settingDir, "authorized.ini");

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
        if (!settingDir.exists())
            IOUtil.createDirectory(settingDir.getAbsolutePath());

        if (!authorized.exists()) {
            InputStream is = getClass().getResourceAsStream("/conf/security/sample-authorized.ini");
            OutputStream os = new FileOutputStream(authorized);
            IOUtil.copy(is, os);
            is.close();
            os.close();
        }
    }

    public boolean isAuthorized(String ip) throws Exception {
        //trust local address
        if (ip.equals(InetAddress.getLocalHost().getHostAddress()) ||
            "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip))
            return true;

        //check on the trusted list
        Vector v;
        try {
            v = IOUtil.readLines(authorized);
        } catch (Exception e) {
            return false;
        }
        for (Enumeration e = v.elements(); e.hasMoreElements();) {
            String line = ((String)e.nextElement()).trim();
            if (line.startsWith("#") || "".equals(line))
                continue;
            if (line.equals(ip))
                return true;
        }
        return false;
    }
}
