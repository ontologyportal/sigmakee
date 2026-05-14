package com.articulate.sigma;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

public class SigmaStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        System.out.println("INFO  [SigmaStartupListener.contextInitialized()] SigmaKEE startup beginning");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            if (kb == null) {
                System.err.println("ERROR  [SigmaStartupListener.contextInitialized()] KB SUMO was null");
            }
        }
        catch (Exception e) {
            System.err.println("ERROR  [SigmaStartupListener.contextInitialized()]: startup failed");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {

        System.out.println("SigmaStartupListener.contextDestroyed(): SigmaKEE shutting down");
    }
}