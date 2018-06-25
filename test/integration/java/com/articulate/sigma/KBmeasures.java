package com.articulate.sigma;

import org.junit.Test;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class KBmeasures extends IntegrationTestBase {

    /** *************************************************************
     */
    @Test
    public void testTermDepth1() {

        KB kb = KBmanager.getMgr().getKB("SUMO");
        assertTrue(kb.termDepth("AudioRecorder") > kb.termDepth("Device"));
    }

    /** *************************************************************
     */
    @Test
    public void testTermDepth2() {

        KB kb = KBmanager.getMgr().getKB("SUMO");
        assertTrue(kb.compareTermDepth("AudioRecorder","Device") == 1);
    }

    /** *************************************************************
     */
    @Test
    public void testTermDepth3() {

        KB kb = KBmanager.getMgr().getKB("SUMO");
        assertTrue(kb.termDepth("VacuumCleaner") > kb.termDepth("Device"));
    }

    /** *************************************************************
     */
    @Test
    public void testTermDepth4() {

        KB kb = KBmanager.getMgr().getKB("SUMO");
        assertTrue(kb.compareTermDepth("VacuumCleaner","Device") == 1);
    }
}
