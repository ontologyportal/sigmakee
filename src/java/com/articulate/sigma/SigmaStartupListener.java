/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
*/

package com.articulate.sigma;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.user.UserManager;
import com.articulate.sigma.jobscheduler.JobScheduler;
import com.articulate.sigma.utils.*;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/********************************************************************
 * Listens for server startup and automatically runs SigmaKEE initialization, starting with KBmanager.initializeOnce().
 * @author Shaun Rose
 */
public class SigmaStartupListener implements ServletContextListener {
    
    /** Application-wide user manager stored in the servlet context. */
    private UserManager userManager;
    /** Application-wide job scheduler. */
    private JobScheduler jobScheduler;

    /********************************************************************
     * Initializes SigmaKEE when the web application starts.
     * @param event servlet context startup event
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {

        long start = System.nanoTime();
        System.out.println("================================ SIGMAKEE INITIALIZING ================================");
        LoggingUtils.log("INFO", "SigmaKEE Startup Beginning!");
        try {
            KBmanager manager = KBmanager.getMgr();
            manager.initializeOnce();
            Map<String, DiagnosticsCache> diagnosticsCaches = new ConcurrentHashMap<>();
            for (String kbName : manager.getKBnames()) {
                KB kb = manager.getKB(kbName);
                if (kb == null) {
                    LoggingUtils.log("ERROR", "Cannot build diagnostics cache: KB " + kbName + " was null");
                    continue;
                }
                try {
                    diagnosticsCaches.put(kbName, Diagnostics.buildDiagnosticsCache(kb));
                }
                catch (Exception e) {
                    LoggingUtils.log("ERROR", "Diagnostics cache failed for KB " + kbName + ": " + e.getMessage());
                }
            }
            event.getServletContext().setAttribute("diagnosticsCaches",
                    Collections.unmodifiableMap(diagnosticsCaches));
            LoggingUtils.log("INFO", "Built diagnostics caches for " + diagnosticsCaches.size() + " knowledge bases");
            userManager = new UserManager();
            jobScheduler = new JobScheduler();
            LoggingUtils.log("Job scheduler started");
            event.getServletContext().setAttribute("userManager", userManager);
            event.getServletContext().setAttribute("jobScheduler", jobScheduler);
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", "SigmaKEE Startup Failed!");
            e.printStackTrace();
        }
        double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        LoggingUtils.log("INFO", "SigmaKEE startup completed in " + elapsedSeconds + " seconds!");
        System.out.println("============================ SIGMAKEE INITIALIZING COMPLETE ============================");
    }

    /********************************************************************
     * Shuts down application resources when the web application stops.
     * @param event servlet context shutdown event
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {

        LoggingUtils.log("SigmaKEE shutting down!");
        UserManager manager = (UserManager) event.getServletContext().getAttribute("userManager");
        if (manager != null) manager.shutdown();
        TPTPGenerationManager.shutdown();
    }
}