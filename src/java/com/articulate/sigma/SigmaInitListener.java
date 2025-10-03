package com.articulate.sigma;
import javax.servlet.annotation.WebListener;
import javax.servlet.*;

@WebListener
public class SigmaInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("SigmaInitListener: Initializing KBmanager...");
        KBmanager mgr = KBmanager.getMgr();
        if (mgr != null) {
            mgr.initializeOnce();   // <-- correct method name
        } else {
            System.err.println("SigmaInitListener: KBmanager is null!");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("SigmaInitListener: Shutting down.");
    }
}
