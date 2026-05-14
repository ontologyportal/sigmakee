package com.articulate.sigma;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.user.UserManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SigmaStartupListener implements ServletContextListener {

    private UserManager userManager;

    @Override
    public void contextInitialized(ServletContextEvent event) {

        System.out.println("\n================================ SIGMAKEE INITIALIZING ================================\n" + "INFO  [SigmaStartupListener.contextInitialized()] SigmaKEE startup beginning...");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            if (kb == null) {
                System.err.println("ERROR  [SigmaStartupListener.contextInitialized()] KB SUMO was null");
            }
            userManager = new UserManager();
            event.getServletContext().setAttribute("userManager", userManager);
        }
        catch (Exception e) {
            System.err.println("ERROR  [SigmaStartupListener.contextInitialized()] startup failed");
            e.printStackTrace();
        }
        System.out.println("INFO  [SigmaStartupListener.contextInitialized()] SigmaKEE startup completed!\n============================ SIGMAKEE INITIALIZING COMPLETE ============================\n");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {

        System.out.println("INFO  [SigmaStartupListener.contextDestroyed()] SigmaKEE shutting down");
        UserManager manager = (UserManager) event.getServletContext().getAttribute("userManager");
        if (manager != null) {
            manager.shutdown();
        }
        TPTPGenerationManager.shutdown();
    }
}