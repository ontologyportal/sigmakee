package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Tests on SumoProcessEntityProperty with a mock set of KBs.

public class SumoProcessEntityPropertySimpleTest extends SigmaMockTestBase {

    private final KB knowledgeBase = this.kbMock;

    @Test
    public void testAttributeFemale() {
        Formula formula = new Formula("(attribute ?human Female)");
        SumoProcessEntityProperty prop = new SumoProcessEntityProperty(formula);

        String expected = "female human";
        String actual = prop.getSurfaceFormForNoun("human", knowledgeBase);
        assertEquals(expected, actual);
    }

    /**
     * Test on an attribute not in the KBs' term format map.
     */
    @Test
    public void testAttributeNotInKBs() {
        Formula formula = new Formula("(attribute ?x Stultifying)");
        SumoProcessEntityProperty prop = new SumoProcessEntityProperty(formula);

        String expected = "Stultifying bureaucracy";
        String actual = prop.getSurfaceFormForNoun("bureaucracy", knowledgeBase);
        assertEquals(expected, actual);
    }

}