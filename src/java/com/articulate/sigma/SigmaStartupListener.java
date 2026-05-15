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
long start = System.nanoTime();
        System.out.println("\n================================ SIGMAKEE INITIALIZING ================================\n");
        LoggingUtils.log("INFO", "SigmaKEE Startup Beginning!");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            if (kb == null) {
                LoggingUtils.log("ERROR", "KB was null!");
            }
            userManager = new UserManager();
            event.getServletContext().setAttribute("userManager", userManager);
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", "SigmaKEE Startup Failed!");
            e.printStackTrace();
        }
        double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        LoggingUtils.log("INFO", "SigmaKEE startup completed in " + elapsedSeconds + " seconds!");
        System.out.println("============================ SIGMAKEE INITIALIZING COMPLETE ============================\n");
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