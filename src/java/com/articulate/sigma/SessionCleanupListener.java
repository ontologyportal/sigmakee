/* This code is copyright Articulate Software (c) 2003-2025.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.

HTTP session lifecycle listener for cleaning up session-specific resources.
*/

package com.articulate.sigma;

import com.articulate.sigma.trans.SessionTPTPManager;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Listens for HTTP session lifecycle events to clean up session-specific resources.
 *
 * When a session is destroyed (timeout or invalidation), this listener:
 * 1. Cleans up session-specific TPTP files (via SessionTPTPManager)
 * 2. Cleans up session-specific proof files (if any)
 */
@WebListener
public class SessionCleanupListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // No action needed on session creation
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

        String sessionId = se.getSession().getId();
        System.out.println("SessionCleanupListener: Session destroyed: " + sessionId);

        // Clean up session-specific TPTP files
        SessionTPTPManager.cleanupSession(sessionId);

        // Clean up session-specific proof files (if proofs directory exists)
        cleanupProofFiles(sessionId);
    }

    /*********************************************************************************
     * Clean up any session-specific proof files.
     *
     * @param sessionId The HTTP session ID
     */
    private void cleanupProofFiles(String sessionId) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        if (kbDir == null) {
            return;
        }

        Path proofsDir = Paths.get(kbDir, "proofs", sessionId);

        if (!Files.exists(proofsDir)) {
            return;
        }

        System.out.println("SessionCleanupListener: Cleaning up proof directory: " + proofsDir);

        try {
            Files.walk(proofsDir)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order for depth-first deletion
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        }
                        catch (IOException e) {
                            System.err.println("SessionCleanupListener: Failed to delete " + path + ": " + e.getMessage());
                        }
                    });
        }
        catch (IOException e) {
            System.err.println("SessionCleanupListener: Error cleaning up proof files: " + e.getMessage());
        }
    }
}
