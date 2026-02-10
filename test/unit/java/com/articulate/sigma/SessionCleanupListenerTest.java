package com.articulate.sigma;

import com.articulate.sigma.trans.SessionTPTPManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SessionCleanupListener.
 * Tests HTTP session lifecycle handling for TPTP file cleanup.
 */
public class SessionCleanupListenerTest {

    @Test
    public void testListenerInstantiation() {
        // Test that the listener can be instantiated
        SessionCleanupListener listener = new SessionCleanupListener();
        assertNotNull("Listener should be instantiated", listener);
    }

    @Test
    public void testWebListenerAnnotation() {
        // Verify the @WebListener annotation is present
        Class<?> listenerClass = SessionCleanupListener.class;
        assertTrue("SessionCleanupListener should have @WebListener annotation",
                   listenerClass.isAnnotationPresent(javax.servlet.annotation.WebListener.class));
    }

    @Test
    public void testImplementsHttpSessionListener() {
        // Verify the class implements HttpSessionListener
        assertTrue("SessionCleanupListener should implement HttpSessionListener",
                   javax.servlet.http.HttpSessionListener.class.isAssignableFrom(SessionCleanupListener.class));
    }

    @Test
    public void testSessionCleanupIntegration() {
        // Test that SessionTPTPManager.cleanupSession works (called by listener)
        String testSessionId = "listener-test-" + System.currentTimeMillis();

        // Verify no files exist for this session
        assertFalse("Session should not have files initially",
                    SessionTPTPManager.hasSessionFiles(testSessionId));

        // Cleanup should complete without error
        SessionTPTPManager.cleanupSession(testSessionId);

        // Verify still no files
        assertFalse("Session should not have files after cleanup",
                    SessionTPTPManager.hasSessionFiles(testSessionId));
    }

    @Test
    public void testListenerMethodsExist() {
        // Verify the required HttpSessionListener methods exist
        SessionCleanupListener listener = new SessionCleanupListener();

        try {
            // These methods should exist and be callable
            listener.getClass().getMethod("sessionCreated", javax.servlet.http.HttpSessionEvent.class);
            listener.getClass().getMethod("sessionDestroyed", javax.servlet.http.HttpSessionEvent.class);
        } catch (NoSuchMethodException e) {
            fail("Required HttpSessionListener methods should exist: " + e.getMessage());
        }
    }
}
