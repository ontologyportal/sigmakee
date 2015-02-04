package com.articulate.sigma;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 */
public class LanguageFormatterHtmlParaphraseIntegrationTest extends IntegrationTestBase {


    /**
     * Ideal: "The oldest customer enters an invalid card."
     */
    @Test
    public void testOldestCustomerEntersCard()     {
        String stmt =   "(exists \n" +
                "                  (?card ?customer ?event ?salesperson) \n" +
                "                  (and \n" +
                "                    (forall \n" +
                "                      (?X) \n" +
                "                      (=> \n" +
                "                        (and \n" +
                "                          (instance ?X customer) \n" +
                "                          (not \n" +
                "                            (equal ?X ?customer))) \n" +
                "                        (and \n" +
                "                          (greaterThan ?val1 ?val2) \n" +
                "                          (age ?customer ?val1) \n" +
                "                          (age ?X ?val2)))) \n" +
                "                    (attribute ?card Incorrect) \n" +
                "                    (instance ?card BankCard) \n" +
                "                    (instance ?customer CognitiveAgent) \n" +
                "                    (instance ?event Motion) \n" +
                "                    (instance ?salesperson CognitiveAgent) \n" +
                "                    (patient ?event ?card) \n" +
                "                    (agent ?event ?customer) \n" +
                "                    (customer ?customer ?salesperson)))";

        String expectedResult = "there exist an object, a cognitive agent, , , a process and another cognitive agent such that for all another object if the other object is an instance of customer and the other object is not equal to the cognitive agent, then a quantity is greater than another quantity and the age of the cognitive agent is the quantity and the age of the other object is the other quantity and Incorrect is an attribute of the object and the object is an instance of BankCard and the cognitive agent is an instance of cognitive agent and the process is an instance of motion and the other cognitive agent is an instance of cognitive agent and the object is a patient of the process and the cognitive agent is an agent of the process and customer the cognitive agent and the other cognitive agent";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

}