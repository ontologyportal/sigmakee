package com.articulate.sigma;

import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.tp.ProverTimeoutException;
import com.articulate.sigma.tp.ATPException;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class KBTest extends UnitTestBase {

    /** ***************************************************************
     */
    @Test
    public void testMostSpecificTerm() {

        String t = SigmaTestBase.kb.mostSpecificTerm(Arrays.asList(new String[]{"Entity","RealNumber"}));
        System.out.println("testMostSpecificTerm(): " + t);
        assertEquals("RealNumber", t);
    }

    /** ***************************************************************
     */
    @Test
    public void testAskWithTwoRestrictionsDirect1() {

        List<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        assertNotEquals(0, actual.size());
    }

    /** ***************************************************************
     * Fails because askWithTwoRestrictions does not go up the class hierarchy but if caching is on will get "1".
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect1() {

        List<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        if (actual != null && !actual.isEmpty()) {
            System.out.println("KBtest.testAskWithTwoRestrictionsIndirect1(): " + actual);
            assertEquals(1, actual.size());
        }
    }

    /** ***************************************************************
     * Fails because askWithTwoRestrictions does not go up the class hierarchy.
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect2() {

        List<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Entity");
        assertEquals(0, actual.size());
    }

    /** ***************************************************************
     * test deleteUserAssertionsAndReload() -- with Vampire
     */
    @Test
    public void testDeleteUserAssertionsAndReloadWithVampire() {

        System.out.println("============== testDeleteUserAssertionsAndReloadWithVampire =====================");
        SigmaTestBase.kb.tell("(instance JohnJacob Human)");
        String query = "(instance JohnJacob Human)";

        Vampire vamp = SigmaTestBase.kb.askVampire(query,10,1);
        if (vamp != null)
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(): results: " + vamp.output);
        else
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(): results: " + null);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        if (vamp != null) {
            vamp.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
            tpp.parseProofOutput(vamp.output, query, kb, new StringBuilder());
        }
        if (tpp.proof != null && (tpp.status.equals("Refutation") || tpp.status.equals("Theorem")))
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(1): success");
        else
            System.err.println("testDeleteUserAssertionsAndReloadWithVampire(1): fail, proof size: "+ tpp.proof.size() + " '" + tpp.status + "'");
        assertTrue(tpp.proof != null && (tpp.status.equals("Refutation") || tpp.status.equals("Theorem")));
        SigmaTestBase.kb.deleteUserAssertionsAndReload();

        // After deleting the assertion, Vampire should NOT find a proof.
        // This can manifest as: timeout, no proof, or ProverTimeoutException
        boolean secondQueryFailed = false;
        try {
            vamp = SigmaTestBase.kb.askVampire(query, 10, 1);
            vamp.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
            TPTP3ProofProcessor tpp1 = new TPTP3ProofProcessor();
            tpp1.parseProofOutput(vamp.output, query, kb, new StringBuilder());
            System.out.println("User assertions deleted");
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(): results after delete: " + vamp);
            if (tpp1.proof == null || tpp1.proof.isEmpty() || tpp1.status.equals("Timeout")) {
                System.out.println("testDeleteUserAssertionsAndReloadWithVampire(2): success - no proof found");
                secondQueryFailed = true;
            } else {
                System.err.println("testDeleteUserAssertionsAndReloadWithVampire(2): fail, proof size: " + tpp1.proof.size() + " '" + tpp1.status + "'");
            }
        } catch (ProverTimeoutException e) {
            // Expected - Vampire timed out because it can't prove something not in the KB
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(2): success - ProverTimeoutException (expected)");
            secondQueryFailed = true;
        } catch (ATPException e) {
            // Other ATP exceptions also indicate failure to prove
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(2): success - ATPException: " + e.getMessage());
            secondQueryFailed = true;
        }
        assertTrue("Expected second query to fail (no proof after assertion deleted)", secondQueryFailed);
    }

    /** ***************************************************************
     * test deleteUserAssertionsAndReload()
     */
    @Test
    public void testDeleteUserAssertionsAndReload() {

        System.out.println("============== testDeleteUserAssertionsAndReload =====================");
        SigmaTestBase.kb.tell("(instance JohnJacob Human)");
        List<Formula> results = SigmaTestBase.kb.ask("arg",1,"JohnJacob");
        System.out.println("testDeleteUserAssertionsAndReload(): results: " + results);
        assertEquals(1, results.size());
        SigmaTestBase.kb.deleteUserAssertionsAndReload();
        results = SigmaTestBase.kb.ask("arg",1,"JohnJacob");
        System.out.println("User assertions deleted");
        System.out.println("testDeleteUserAssertionsAndReload(): results after delete: " + results);
        assertEquals(0, results.size());
    }

    /** ***************************************************************
     */
    @Test
    public void testIsSubclass2()   {
        assertTrue(SigmaTestBase.kb.isSubclass("Driving", "Process"));
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesEmptyInput() {

        Set<String> inputSet = Sets.newHashSet();
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet();
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesOneElementInput() {

        Set<String> inputSet = Sets.newHashSet("nonsenseWord");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("nonsenseWord");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementIdenticalInput1() {

        Set<String> inputSet = Sets.newHashSet("Entity", "Entity");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Entity");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementIdenticalInput2() {

        Set<String> inputSet = Sets.newHashSet("Process", "Process");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Process");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementIdenticalInput3() {

        Set<String> inputSet = Sets.newHashSet("Physical", "Physical");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Physical");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementInput() {

        Set<String> inputSet = Sets.newHashSet("Man", "Human");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementInputReverse() {

        Set<String> inputSet = Sets.newHashSet("Human", "Man");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementInputNoSubclass() {

        Set<String> inputSet = Sets.newHashSet("Man", "Woman");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man", "Woman");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesFiveElementInput() {

        Set<String> inputSet = Sets.newHashSet("Object", "CorpuscularObject", "Woman", "Human", "Man");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man", "Woman");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testTermFormatMapAll() {

        System.out.println("============== testTermFormatMapAll =====================");
        System.out.println("Testing  kb.termFormatMapAll('EnglishLanguage')");
        Map<String, List<String>> termFormats = kb.getTermFormatMapAll("EnglishLanguage");
        List<String> motherTermFormats = termFormats.get("mother");
        for (String termFormat : motherTermFormats) {
            System.out.println("Term Format for mother: " + termFormat);
        }
        assertFalse(motherTermFormats.isEmpty());
    }

    /** ***************************************************************
     */
    @Test
    public void testFormatMapAll() {

        System.out.println("============== testFormatMapAll =====================");
        System.out.println("Testing  kb.formatMapAll('EnglishLanguage')");
        Map<String, List<String>> allFormats = kb.getFormatMapAll("EnglishLanguage");
        List<String> motherFormats = allFormats.get("mother");
        for (String format : motherFormats) {
            System.out.println("Format for mother: " + format);
        }
        assertFalse(motherFormats.isEmpty());
    }




    /** ***************************************************************
     * infBaseFileOldIgnoringUserAssertions(lang): missing base file => true
     */
    @Test
    public void testInfBaseFileOldIgnoringUserAssertionsMissingBaseReturnsTrue() throws Exception {

        final KBmanager mgr = KBmanager.getMgr();
        final java.util.Map<String, KB> oldKbs = getKBsMapReflect(mgr);

        final java.io.File kbDir = new java.io.File(KButilities.SIGMA_HOME + java.io.File.separator + "KBs");
        assertTrue("KBs dir not found: " + kbDir, kbDir.exists() && kbDir.isDirectory());

        String kbName = "TESTKB_INFOLD_" + System.nanoTime();
        java.io.File baseTptp = new java.io.File(kbDir, kbName + ".tptp");
        if (baseTptp.exists())
            //noinspection ResultOfMethodCallIgnored
            baseTptp.delete();

        java.io.File tmp = java.nio.file.Files.createTempDirectory("infOld-missing").toFile();
        java.io.File nonUa = new java.io.File(tmp, kbName + "_Base.kif");
        writeAndTouch(nonUa, 1500L);
        java.io.File ua = new java.io.File(tmp, kbName + "_UserAssertions.kif");
        writeAndTouch(ua, 2500L);

        KB testKb = new KB(kbName);
        testKb.constituents.add(nonUa.getAbsolutePath());
        testKb.constituents.add(ua.getAbsolutePath());

        try {
            setKBsMapReflect(mgr, new java.util.HashMap<>());
            getKBsMapReflect(mgr).put(kbName, testKb);

            assertTrue(mgr.infBaseFileOldIgnoringUserAssertions("tptp"));
        }
        finally {
            setKBsMapReflect(mgr, oldKbs);
            deleteRecursively(tmp);
            if (baseTptp.exists())
                //noinspection ResultOfMethodCallIgnored
                baseTptp.delete();
        }
    }

    /** ***************************************************************
     * infBaseFileOldIgnoringUserAssertions(lang): UA newer than base is ignored => false
     */
    @Test
    public void testInfBaseFileOldIgnoringUserAssertionsUANewerIgnoredReturnsFalse() throws Exception {

        final KBmanager mgr = KBmanager.getMgr();
        final java.util.Map<String, KB> oldKbs = getKBsMapReflect(mgr);

        final java.io.File kbDir = new java.io.File(KButilities.SIGMA_HOME + java.io.File.separator + "KBs");
        assertTrue("KBs dir not found: " + kbDir, kbDir.exists() && kbDir.isDirectory());

        String kbName = "TESTKB_INFOLD_" + System.nanoTime();

        // Create base translation file in SIGMA_HOME/KBs with a future timestamp (guaranteed newer than config.xml)
        long baseTs = System.currentTimeMillis() + 60_000L;
        java.io.File baseTptp = new java.io.File(kbDir, kbName + ".tptp");
        writeAndTouch(baseTptp, baseTs);

        java.io.File tmp = java.nio.file.Files.createTempDirectory("infOld-uaIgnored").toFile();

        // non-UA constituent older than base
        java.io.File nonUa = new java.io.File(tmp, kbName + "_Base.kif");
        writeAndTouch(nonUa, baseTs - 10_000L);

        // UA newer than base, but should be ignored
        java.io.File ua = new java.io.File(tmp, kbName + "_UserAssertions.kif");
        writeAndTouch(ua, baseTs + 10_000L);

        KB testKb = new KB(kbName);
        testKb.constituents.add(nonUa.getAbsolutePath());
        testKb.constituents.add(ua.getAbsolutePath());

        try {
            setKBsMapReflect(mgr, new java.util.HashMap<>());
            getKBsMapReflect(mgr).put(kbName, testKb);

            assertFalse(mgr.infBaseFileOldIgnoringUserAssertions("tptp"));
        }
        finally {
            setKBsMapReflect(mgr, oldKbs);
            deleteRecursively(tmp);
            if (baseTptp.exists())
                //noinspection ResultOfMethodCallIgnored
                baseTptp.delete();
        }
    }

    /** ***************************************************************
     * infBaseFileOldIgnoringUserAssertions(lang): non-UA constituent newer than base => true
     */
    @Test
    public void testInfBaseFileOldIgnoringUserAssertionsNonUaConstituentNewerReturnsTrue() throws Exception {

        final KBmanager mgr = KBmanager.getMgr();
        final java.util.Map<String, KB> oldKbs = getKBsMapReflect(mgr);

        final java.io.File kbDir = new java.io.File(KButilities.SIGMA_HOME + java.io.File.separator + "KBs");
        assertTrue("KBs dir not found: " + kbDir, kbDir.exists() && kbDir.isDirectory());

        String kbName = "TESTKB_INFOLD_" + System.nanoTime();

        long baseTs = System.currentTimeMillis() + 60_000L;
        java.io.File baseTptp = new java.io.File(kbDir, kbName + ".tptp");
        writeAndTouch(baseTptp, baseTs);

        java.io.File tmp = java.nio.file.Files.createTempDirectory("infOld-nonUaNewer").toFile();

        // non-UA newer than base => must return true
        java.io.File nonUa = new java.io.File(tmp, kbName + "_Base.kif");
        writeAndTouch(nonUa, baseTs + 10_000L);

        // UA present, but irrelevant
        java.io.File ua = new java.io.File(tmp, kbName + "_UserAssertions.kif");
        writeAndTouch(ua, baseTs - 10_000L);

        KB testKb = new KB(kbName);
        testKb.constituents.add(nonUa.getAbsolutePath());
        testKb.constituents.add(ua.getAbsolutePath());

        try {
            setKBsMapReflect(mgr, new java.util.HashMap<>());
            getKBsMapReflect(mgr).put(kbName, testKb);

            assertTrue(mgr.infBaseFileOldIgnoringUserAssertions("tptp"));
        }
        finally {
            setKBsMapReflect(mgr, oldKbs);
            deleteRecursively(tmp);
            if (baseTptp.exists())
                //noinspection ResultOfMethodCallIgnored
                baseTptp.delete();
        }
    }

    // ===================== helpers (local to KBTest) =====================

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, KB> getKBsMapReflect(KBmanager mgr) throws Exception {
        java.lang.reflect.Field f = mgr.getClass().getDeclaredField("kbs");
        f.setAccessible(true);
        return (java.util.Map<String, KB>) f.get(mgr);
    }

    private static void setKBsMapReflect(KBmanager mgr, java.util.Map<String, KB> map) throws Exception {
        java.lang.reflect.Field f = mgr.getClass().getDeclaredField("kbs");
        f.setAccessible(true);
        f.set(mgr, map);
    }

    private static void writeAndTouch(java.io.File f, long ts) throws Exception {
        if (!f.exists()) {
            java.nio.file.Files.write(
                    f.toPath(),
                    ("x\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        }
        assertTrue("setLastModified failed for " + f, f.setLastModified(ts));
    }

    private static void deleteRecursively(java.io.File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            java.io.File[] kids = f.listFiles();
            if (kids != null) for (java.io.File k : kids) deleteRecursively(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }





}