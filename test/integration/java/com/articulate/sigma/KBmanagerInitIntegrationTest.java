package com.articulate.sigma;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class KBmanagerInitIntegrationTest extends IntegrationTestBase {

    // Verify how long the base class's KBmanager initialization took.
    @Test
    public void testInitializationTime()   {
        assertTrue("Actual time = " + new String(String.valueOf(totalKbMgrInitTime)), totalKbMgrInitTime < 75000);
    }
}