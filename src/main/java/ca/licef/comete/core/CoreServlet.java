package ca.licef.comete.core;

import ca.licef.comete.core.util.Util;
import licef.reflection.Invoker;
import licef.reflection.ThreadInvoker;

import javax.servlet.http.HttpServlet;

/**
 * Created by amiara on 2014-11-03.
 */
public class CoreServlet extends HttpServlet {

    /**
     * This servlet is used to start the Core class and start the Triple Store service (Fuseki)
     */
    public void init() {
        Util.setWebappPath( getServletContext().getRealPath( "" ) );
        try {
            (new ThreadInvoker(new Invoker(null,
                    "ca.licef.comete.core.Core",
                    "getInstance", new Object[]{}))).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        Core.getInstance().getTripleStore().stopServer();
    }
}
