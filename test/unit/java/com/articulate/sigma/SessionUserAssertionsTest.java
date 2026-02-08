package com.articulate.sigma;

import com.articulate.sigma.trans.SessionTPTPManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Unit tests for session-specific UserAssertions functionality.
 * Tests session isolation for tell() and resetAllForInference().
 */
public class SessionUserAssertionsTest extends UnitTestBase {

    private static final String TEST_SESSION_ID_1 = "test-session-001";
    private static final String TEST_SESSION_ID_2 = "test-session-002";

    @Before
    public void setUp() {
        // Clean up any existing test session directories
        cleanupTestSessions();
    }

    @After
    public void tearDown() {
        // Clean up test session directories after each test
        cleanupTestSessions();
    }

    private void cleanupTestSessions() {
        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_1);
        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_2);
    }

    /** ***************************************************************
     * Test that tell() with sessionId creates files in session directory
     */
    @Test
    public void testTellWithSessionIdCreatesSessionFiles() {

        System.out.println("============== testTellWithSessionIdCreatesSessionFiles =====================");

        // Tell with session ID
        String assertion = "(instance TestEntity001 Human)";
        String result = SigmaTestBase.kb.tell(assertion, TEST_SESSION_ID_1);

        System.out.println("Tell result: " + result);
        assertTrue("Tell should succeed", result.contains("added"));

        // Verify session-specific UA file exists
        Path sessionUAFile = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("Session UA file should exist: " + sessionUAFile, Files.exists(sessionUAFile));

        // Verify shared UA file was NOT modified
        File sharedUAFile = new File(KBmanager.getMgr().getPref("kbDir"),
                SigmaTestBase.kb.name + KB._userAssertionsString);

        // If shared file exists, it should not contain our test assertion
        if (sharedUAFile.exists()) {
            try {
                String sharedContent = new String(Files.readAllBytes(sharedUAFile.toPath()));
                assertFalse("Shared UA file should not contain session assertion",
                        sharedContent.contains("TestEntity001"));
            } catch (IOException e) {
                fail("Failed to read shared UA file: " + e.getMessage());
            }
        }
    }

    /** ***************************************************************
     * Test that tell() without sessionId uses shared file (backward compatibility)
     */
    @Test
    public void testTellWithoutSessionIdUsesSharedFile() {

        System.out.println("============== testTellWithoutSessionIdUsesSharedFile =====================");

        // Tell without session ID (backward compatible)
        String assertion = "(instance TestEntity002 Human)";
        String result = SigmaTestBase.kb.tell(assertion);

        System.out.println("Tell result: " + result);
        assertTrue("Tell should succeed", result.contains("added"));

        // Verify shared UA file exists
        File sharedUAFile = new File(KBmanager.getMgr().getPref("kbDir"),
                SigmaTestBase.kb.name + KB._userAssertionsString);
        assertTrue("Shared UA file should exist", sharedUAFile.exists());

        // Clean up shared file
        try {
            InferenceTestSuite.resetAllForInference(SigmaTestBase.kb);
        } catch (IOException e) {
            fail("Failed to reset: " + e.getMessage());
        }
    }

    /** ***************************************************************
     * Test that multiple sessions maintain isolated UA files
     */
    @Test
    public void testMultipleSessionsAreIsolated() {

        System.out.println("============== testMultipleSessionsAreIsolated =====================");

        // Session 1 assertion
        String assertion1 = "(instance SessionOneEntity Human)";
        SigmaTestBase.kb.tell(assertion1, TEST_SESSION_ID_1);

        // Session 2 assertion
        String assertion2 = "(instance SessionTwoEntity Robot)";
        SigmaTestBase.kb.tell(assertion2, TEST_SESSION_ID_2);

        // Verify session 1 file
        Path session1File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("Session 1 UA file should exist", Files.exists(session1File));

        // Verify session 2 file
        Path session2File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_2, SigmaTestBase.kb.name);
        assertTrue("Session 2 UA file should exist", Files.exists(session2File));

        // Verify session 1 contains only its assertion
        try {
            String session1Content = new String(Files.readAllBytes(session1File));
            assertTrue("Session 1 should contain SessionOneEntity", session1Content.contains("SessionOneEntity"));
            assertFalse("Session 1 should NOT contain SessionTwoEntity", session1Content.contains("SessionTwoEntity"));
        } catch (IOException e) {
            fail("Failed to read session 1 file: " + e.getMessage());
        }

        // Verify session 2 contains only its assertion
        try {
            String session2Content = new String(Files.readAllBytes(session2File));
            assertTrue("Session 2 should contain SessionTwoEntity", session2Content.contains("SessionTwoEntity"));
            assertFalse("Session 2 should NOT contain SessionOneEntity", session2Content.contains("SessionOneEntity"));
        } catch (IOException e) {
            fail("Failed to read session 2 file: " + e.getMessage());
        }
    }

    /** ***************************************************************
     * Test resetAllForInference with sessionId deletes only session files
     */
    @Test
    public void testResetWithSessionIdDeletesOnlySessionFiles() throws IOException {

        System.out.println("============== testResetWithSessionIdDeletesOnlySessionFiles =====================");

        // Create assertions in both sessions
        SigmaTestBase.kb.tell("(instance Session1Entity Human)", TEST_SESSION_ID_1);
        SigmaTestBase.kb.tell("(instance Session2Entity Robot)", TEST_SESSION_ID_2);

        // Verify both files exist
        Path session1File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        Path session2File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_2, SigmaTestBase.kb.name);
        assertTrue("Session 1 file should exist before reset", Files.exists(session1File));
        assertTrue("Session 2 file should exist before reset", Files.exists(session2File));

        // Reset only session 1
        InferenceTestSuite.resetAllForInference(SigmaTestBase.kb, TEST_SESSION_ID_1);

        // Verify session 1 file is deleted
        assertFalse("Session 1 file should be deleted after reset", Files.exists(session1File));

        // Verify session 2 file still exists
        assertTrue("Session 2 file should still exist after session 1 reset", Files.exists(session2File));
    }

    /** ***************************************************************
     * Test resetAllForInference without sessionId deletes shared files (backward compatibility)
     */
    @Test
    public void testResetWithoutSessionIdDeletesSharedFiles() throws IOException {

        System.out.println("============== testResetWithoutSessionIdDeletesSharedFiles =====================");

        // Create assertion in shared file
        SigmaTestBase.kb.tell("(instance SharedEntity Human)");

        // Verify shared file exists
        File sharedUAFile = new File(KBmanager.getMgr().getPref("kbDir"),
                SigmaTestBase.kb.name + KB._userAssertionsString);
        assertTrue("Shared UA file should exist before reset", sharedUAFile.exists());

        // Reset without session ID (backward compatible)
        InferenceTestSuite.resetAllForInference(SigmaTestBase.kb);

        // Verify shared file is deleted
        assertFalse("Shared UA file should be deleted after reset", sharedUAFile.exists());
    }

    /** ***************************************************************
     * Test SessionTPTPManager.cleanupSession removes all session files
     */
    @Test
    public void testCleanupSessionRemovesAllFiles() {

        System.out.println("============== testCleanupSessionRemovesAllFiles =====================");

        // Create assertion in session
        SigmaTestBase.kb.tell("(instance CleanupTestEntity Human)", TEST_SESSION_ID_1);

        // Verify session directory exists
        Path sessionDir = SessionTPTPManager.getSessionDir(TEST_SESSION_ID_1);
        assertTrue("Session directory should exist", Files.exists(sessionDir));

        // Cleanup session
        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_1);

        // Verify session directory is removed
        assertFalse("Session directory should be removed after cleanup", Files.exists(sessionDir));
    }

    /** ***************************************************************
     * Test SessionTPTPManager UA path methods
     */
    @Test
    public void testSessionTPTPManagerUAPathMethods() {

        System.out.println("============== testSessionTPTPManagerUAPathMethods =====================");

        // Test getSessionUAPath
        Path uaPath = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA path should contain session ID", uaPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA path should end with _UserAssertions.kif",
                uaPath.toString().endsWith(KB._userAssertionsString));

        // Test getSessionUATPTPPath
        Path uaTPTPPath = SessionTPTPManager.getSessionUATPTPPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA TPTP path should contain session ID", uaTPTPPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA TPTP path should end with _UserAssertions.tptp",
                uaTPTPPath.toString().endsWith(KB._userAssertionsTPTP));

        // Test getSessionUATFFPath
        Path uaTFFPath = SessionTPTPManager.getSessionUATFFPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA TFF path should contain session ID", uaTFFPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA TFF path should end with _UserAssertions.tff",
                uaTFFPath.toString().endsWith(KB._userAssertionsTFF));

        // Test getSessionUATHFPath
        Path uaTHFPath = SessionTPTPManager.getSessionUATHFPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA THF path should contain session ID", uaTHFPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA THF path should end with _UserAssertions.thf",
                uaTHFPath.toString().endsWith(KB._userAssertionsTHF));
    }

    /** ***************************************************************
     * Test that session-specific tell creates directory structure
     */
    @Test
    public void testSessionTellCreatesDirectoryStructure() {

        System.out.println("============== testSessionTellCreatesDirectoryStructure =====================");

        // Tell with session ID
        SigmaTestBase.kb.tell("(instance DirTestEntity Human)", TEST_SESSION_ID_1);

        // Verify session directory exists
        Path sessionDir = SessionTPTPManager.getSessionDir(TEST_SESSION_ID_1);
        assertTrue("Session directory should be created", Files.exists(sessionDir));
        assertTrue("Session directory should be a directory", Files.isDirectory(sessionDir));

        // Verify session directory path structure
        String sessionDirPath = sessionDir.toString();
        assertTrue("Session directory should contain 'sessions' folder", sessionDirPath.contains("sessions"));
        assertTrue("Session directory should contain session ID", sessionDirPath.contains(TEST_SESSION_ID_1));
    }

    /** ***************************************************************
     * Test edge case: null sessionId behaves like no sessionId
     */
    @Test
    public void testNullSessionIdUsesSharedFile() {

        System.out.println("============== testNullSessionIdUsesSharedFile =====================");

        // Tell with null session ID
        String assertion = "(instance NullSessionEntity Human)";
        String result = SigmaTestBase.kb.tell(assertion, null);

        assertTrue("Tell should succeed", result.contains("added"));

        // Verify shared UA file exists (null session ID = shared file)
        File sharedUAFile = new File(KBmanager.getMgr().getPref("kbDir"),
                SigmaTestBase.kb.name + KB._userAssertionsString);
        assertTrue("Shared UA file should exist with null sessionId", sharedUAFile.exists());

        // Clean up
        try {
            InferenceTestSuite.resetAllForInference(SigmaTestBase.kb, null);
        } catch (IOException e) {
            fail("Failed to reset: " + e.getMessage());
        }
    }

    /** ***************************************************************
     * Test edge case: empty sessionId behaves like no sessionId
     */
    @Test
    public void testEmptySessionIdUsesSharedFile() {

        System.out.println("============== testEmptySessionIdUsesSharedFile =====================");

        // Tell with empty session ID
        String assertion = "(instance EmptySessionEntity Human)";
        String result = SigmaTestBase.kb.tell(assertion, "");

        assertTrue("Tell should succeed", result.contains("added"));

        // Verify shared UA file exists (empty session ID = shared file)
        File sharedUAFile = new File(KBmanager.getMgr().getPref("kbDir"),
                SigmaTestBase.kb.name + KB._userAssertionsString);
        assertTrue("Shared UA file should exist with empty sessionId", sharedUAFile.exists());

        // Clean up
        try {
            InferenceTestSuite.resetAllForInference(SigmaTestBase.kb, "");
        } catch (IOException e) {
            fail("Failed to reset: " + e.getMessage());
        }
    }
}
