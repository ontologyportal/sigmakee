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
 * Tests session isolation for tell() and SessionTPTPManager cleanup.
 */
public class SessionUserAssertionsTest extends UnitTestBase {

    private static final String TEST_SESSION_ID_1 = "test-session-001";
    private static final String TEST_SESSION_ID_2 = "test-session-002";

    private static final String[] USER_ASSERTION_SUFFIXES = {
            KB._userAssertionsString,
            KB._userAssertionsTPTP,
            KB._userAssertionsTFF,
            KB._userAssertionsTHF
    };

    @Before
    public void setUp() throws IOException {
        cleanupTestState();
    }

    @After
    public void tearDown() throws IOException {
        cleanupTestState();
    }

    private void cleanupTestState() throws IOException {
        cleanupTestSessions();
        cleanupSharedUserAssertions();
    }

    private void cleanupTestSessions() {
        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_1);
        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_2);
    }

    private void cleanupSharedUserAssertions() throws IOException {

        SigmaTestBase.kb.purgeUserAssertionsFromMemory();
        SigmaTestBase.kb.deleteUserAssertions();
        deleteSharedUserAssertionFiles();

        if (SigmaTestBase.kb.termDepthCache != null)
            SigmaTestBase.kb.termDepthCache.clear();
    }

    private static File sharedUserAssertionFile() {

        return new File(KBmanager.getMgr().getPref("kbDir"),
                SigmaTestBase.kb.name + KB._userAssertionsString);
    }

    private static void deleteSharedUserAssertionFiles() throws IOException {

        File dir = new File(KBmanager.getMgr().getPref("kbDir"));
        for (String suffix : USER_ASSERTION_SUFFIXES)
            Files.deleteIfExists(new File(dir, SigmaTestBase.kb.name + suffix).toPath());
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }

    /** ***************************************************************
     * Test that tell() with sessionId creates files in session directory.
     */
    @Test
    public void testTellWithSessionIdCreatesSessionFiles() throws IOException {

        System.out.println("============== testTellWithSessionIdCreatesSessionFiles =====================");

        String assertion = "(instance TestEntity001 Human)";
        String result = SigmaTestBase.kb.tell(assertion, TEST_SESSION_ID_1);

        assertTrue("Tell should succeed", result.contains("added"));

        Path sessionUAFile = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("Session UA file should exist: " + sessionUAFile, Files.exists(sessionUAFile));

        File sharedUAFile = sharedUserAssertionFile();
        if (sharedUAFile.exists()) {
            String sharedContent = read(sharedUAFile.toPath());
            assertFalse("Shared UA file should not contain session assertion",
                    sharedContent.contains("TestEntity001"));
        }
    }

    /** ***************************************************************
     * Test that tell() without sessionId uses shared file.
     */
    @Test
    public void testTellWithoutSessionIdUsesSharedFile() {

        System.out.println("============== testTellWithoutSessionIdUsesSharedFile =====================");

        String assertion = "(instance TestEntity002 Human)";
        String result = SigmaTestBase.kb.tell(assertion);

        assertTrue("Tell should succeed", result.contains("added"));
        assertTrue("Shared UA file should exist", sharedUserAssertionFile().exists());
    }

    /** ***************************************************************
     * Test that multiple sessions maintain isolated UA files.
     */
    @Test
    public void testMultipleSessionsAreIsolated() throws IOException {

        System.out.println("============== testMultipleSessionsAreIsolated =====================");

        SigmaTestBase.kb.tell("(instance SessionOneEntity Human)", TEST_SESSION_ID_1);
        SigmaTestBase.kb.tell("(instance SessionTwoEntity Robot)", TEST_SESSION_ID_2);

        Path session1File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        Path session2File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_2, SigmaTestBase.kb.name);

        assertTrue("Session 1 UA file should exist", Files.exists(session1File));
        assertTrue("Session 2 UA file should exist", Files.exists(session2File));

        String session1Content = read(session1File);
        assertTrue("Session 1 should contain SessionOneEntity",
                session1Content.contains("SessionOneEntity"));
        assertFalse("Session 1 should NOT contain SessionTwoEntity",
                session1Content.contains("SessionTwoEntity"));

        String session2Content = read(session2File);
        assertTrue("Session 2 should contain SessionTwoEntity",
                session2Content.contains("SessionTwoEntity"));
        assertFalse("Session 2 should NOT contain SessionOneEntity",
                session2Content.contains("SessionOneEntity"));
    }

    /** ***************************************************************
     * Test that cleaning one session does not delete another session.
     */
    @Test
    public void testCleanupOneSessionDoesNotDeleteOtherSession() {

        System.out.println("============== testCleanupOneSessionDoesNotDeleteOtherSession =====================");

        SigmaTestBase.kb.tell("(instance Session1Entity Human)", TEST_SESSION_ID_1);
        SigmaTestBase.kb.tell("(instance Session2Entity Robot)", TEST_SESSION_ID_2);

        Path session1File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        Path session2File = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_2, SigmaTestBase.kb.name);

        assertTrue("Session 1 file should exist before cleanup", Files.exists(session1File));
        assertTrue("Session 2 file should exist before cleanup", Files.exists(session2File));

        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_1);

        assertFalse("Session 1 file should be deleted after cleanup", Files.exists(session1File));
        assertTrue("Session 2 file should still exist after session 1 cleanup", Files.exists(session2File));
    }

    /** ***************************************************************
     * Test that shared user assertion cleanup deletes shared files.
     */
    @Test
    public void testSharedCleanupDeletesSharedFiles() throws IOException {

        System.out.println("============== testSharedCleanupDeletesSharedFiles =====================");

        SigmaTestBase.kb.tell("(instance SharedEntity Human)");

        File sharedUAFile = sharedUserAssertionFile();
        assertTrue("Shared UA file should exist before cleanup", sharedUAFile.exists());

        cleanupSharedUserAssertions();

        assertFalse("Shared UA file should be deleted after cleanup", sharedUAFile.exists());
    }

    /** ***************************************************************
     * Test SessionTPTPManager.cleanupSession removes all session files.
     */
    @Test
    public void testCleanupSessionRemovesAllFiles() {

        System.out.println("============== testCleanupSessionRemovesAllFiles =====================");

        SigmaTestBase.kb.tell("(instance CleanupTestEntity Human)", TEST_SESSION_ID_1);

        Path sessionDir = SessionTPTPManager.getSessionDir(TEST_SESSION_ID_1);
        assertTrue("Session directory should exist", Files.exists(sessionDir));

        SessionTPTPManager.cleanupSession(TEST_SESSION_ID_1);

        assertFalse("Session directory should be removed after cleanup", Files.exists(sessionDir));
    }

    /** ***************************************************************
     * Test SessionTPTPManager UA path methods.
     */
    @Test
    public void testSessionTPTPManagerUAPathMethods() {

        System.out.println("============== testSessionTPTPManagerUAPathMethods =====================");

        Path uaPath = SessionTPTPManager.getSessionUAPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA path should contain session ID", uaPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA path should end with _UserAssertions.kif",
                uaPath.toString().endsWith(KB._userAssertionsString));

        Path uaTPTPPath = SessionTPTPManager.getSessionUATPTPPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA TPTP path should contain session ID", uaTPTPPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA TPTP path should end with _UserAssertions.tptp",
                uaTPTPPath.toString().endsWith(KB._userAssertionsTPTP));

        Path uaTFFPath = SessionTPTPManager.getSessionUATFFPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA TFF path should contain session ID", uaTFFPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA TFF path should end with _UserAssertions.tff",
                uaTFFPath.toString().endsWith(KB._userAssertionsTFF));

        Path uaTHFPath = SessionTPTPManager.getSessionUATHFPath(TEST_SESSION_ID_1, SigmaTestBase.kb.name);
        assertTrue("UA THF path should contain session ID", uaTHFPath.toString().contains(TEST_SESSION_ID_1));
        assertTrue("UA THF path should end with _UserAssertions.thf",
                uaTHFPath.toString().endsWith(KB._userAssertionsTHF));
    }

    /** ***************************************************************
     * Test that session-specific tell creates directory structure.
     */
    @Test
    public void testSessionTellCreatesDirectoryStructure() {

        System.out.println("============== testSessionTellCreatesDirectoryStructure =====================");

        SigmaTestBase.kb.tell("(instance DirTestEntity Human)", TEST_SESSION_ID_1);

        Path sessionDir = SessionTPTPManager.getSessionDir(TEST_SESSION_ID_1);
        assertTrue("Session directory should be created", Files.exists(sessionDir));
        assertTrue("Session directory should be a directory", Files.isDirectory(sessionDir));

        String sessionDirPath = sessionDir.toString();
        assertTrue("Session directory should contain 'sessions' folder", sessionDirPath.contains("sessions"));
        assertTrue("Session directory should contain session ID", sessionDirPath.contains(TEST_SESSION_ID_1));
    }

    /** ***************************************************************
     * Test edge case: null sessionId behaves like no sessionId.
     */
    @Test
    public void testNullSessionIdUsesSharedFile() {

        System.out.println("============== testNullSessionIdUsesSharedFile =====================");

        String assertion = "(instance NullSessionEntity Human)";
        String result = SigmaTestBase.kb.tell(assertion, null);

        assertTrue("Tell should succeed", result.contains("added"));
        assertTrue("Shared UA file should exist with null sessionId", sharedUserAssertionFile().exists());
    }

    /** ***************************************************************
     * Test edge case: empty sessionId behaves like no sessionId.
     */
    @Test
    public void testEmptySessionIdUsesSharedFile() {

        System.out.println("============== testEmptySessionIdUsesSharedFile =====================");

        String assertion = "(instance EmptySessionEntity Human)";
        String result = SigmaTestBase.kb.tell(assertion, "");

        assertTrue("Tell should succeed", result.contains("added"));
        assertTrue("Shared UA file should exist with empty sessionId", sharedUserAssertionFile().exists());
    }
}