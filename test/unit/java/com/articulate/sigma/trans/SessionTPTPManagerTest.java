package com.articulate.sigma.trans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Unit tests for SessionTPTPManager.
 * Tests session-specific TPTP file path generation and cleanup functionality.
 */
public class SessionTPTPManagerTest {

    private Path tempDir;
    private String originalKbDir;

    @Before
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("session-tptp-test");

        // Store original kbDir if KBmanager is initialized
        try {
            if (com.articulate.sigma.KBmanager.getMgr() != null) {
                originalKbDir = com.articulate.sigma.KBmanager.getMgr().getPref("kbDir");
            }
        } catch (Exception e) {
            // KBmanager not initialized, that's fine for unit tests
        }
    }

    @After
    public void tearDown() throws IOException {
        // Clean up temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Test
    public void testGetSessionTPTPPath_FOF() {
        // Test that session path is correctly constructed for FOF
        String sessionId = "test-session-123";
        String kbName = "SUMO";
        String lang = "tptp";

        Path result = SessionTPTPManager.getSessionTPTPPath(sessionId, kbName, lang);

        assertNotNull("Path should not be null", result);
        assertTrue("Path should contain sessions directory",
                   result.toString().contains("sessions"));
        assertTrue("Path should contain session ID",
                   result.toString().contains(sessionId));
        assertTrue("Path should end with .tptp",
                   result.toString().endsWith(kbName + "." + lang));
    }

    @Test
    public void testGetSessionTPTPPath_TFF() {
        // Test that session path is correctly constructed for TFF
        String sessionId = "test-session-456";
        String kbName = "SUMO";
        String lang = "tff";

        Path result = SessionTPTPManager.getSessionTPTPPath(sessionId, kbName, lang);

        assertNotNull("Path should not be null", result);
        assertTrue("Path should contain sessions directory",
                   result.toString().contains("sessions"));
        assertTrue("Path should contain session ID",
                   result.toString().contains(sessionId));
        assertTrue("Path should end with .tff",
                   result.toString().endsWith(kbName + "." + lang));
    }

    @Test
    public void testGetSessionDir() {
        String sessionId = "test-session-789";

        Path result = SessionTPTPManager.getSessionDir(sessionId);

        assertNotNull("Session directory path should not be null", result);
        assertTrue("Path should contain sessions directory",
                   result.toString().contains("sessions"));
        assertTrue("Path should end with session ID",
                   result.toString().endsWith(sessionId));
    }

    @Test
    public void testCleanupSession_NonExistentSession() {
        // Should not throw exception for non-existent session
        String sessionId = "non-existent-session";

        // This should complete without throwing
        SessionTPTPManager.cleanupSession(sessionId);

        // Verify no session files exist
        assertFalse("Session should not have files after cleanup",
                    SessionTPTPManager.hasSessionFiles(sessionId));
    }

    @Test
    public void testCleanupSession_NullSessionId() {
        // Should handle null session ID gracefully
        SessionTPTPManager.cleanupSession(null);
        // No exception should be thrown
    }

    @Test
    public void testCleanupSession_EmptySessionId() {
        // Should handle empty session ID gracefully
        SessionTPTPManager.cleanupSession("");
        // No exception should be thrown
    }

    @Test
    public void testHasSessionFiles_NoFiles() {
        String sessionId = "session-without-files";

        assertFalse("Should return false for session without generated files",
                    SessionTPTPManager.hasSessionFiles(sessionId));
    }

    @Test
    public void testGetSessionGenerationTimestamp_NoGeneration() {
        String sessionId = "session-no-generation";

        assertEquals("Should return -1 for session without generation",
                     -1L, SessionTPTPManager.getSessionGenerationTimestamp(sessionId));
    }

    @Test
    public void testSessionPathIsolation() {
        // Verify that different sessions get different paths
        String session1 = "session-A";
        String session2 = "session-B";
        String kbName = "SUMO";
        String lang = "tptp";

        Path path1 = SessionTPTPManager.getSessionTPTPPath(session1, kbName, lang);
        Path path2 = SessionTPTPManager.getSessionTPTPPath(session2, kbName, lang);

        assertNotEquals("Different sessions should have different paths", path1, path2);
        assertTrue("Path 1 should contain session-A", path1.toString().contains(session1));
        assertTrue("Path 2 should contain session-B", path2.toString().contains(session2));
    }

    @Test
    public void testSessionPathStructure() {
        String sessionId = "structured-session";
        String kbName = "TestKB";
        String lang = "tptp";

        Path sessionPath = SessionTPTPManager.getSessionTPTPPath(sessionId, kbName, lang);
        Path sessionDir = SessionTPTPManager.getSessionDir(sessionId);

        // The file path should be inside the session directory
        assertTrue("Session file should be inside session directory",
                   sessionPath.startsWith(sessionDir));
    }
}
