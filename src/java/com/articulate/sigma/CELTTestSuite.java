

package com.articulate.sigma;

import java.util.*;
import java.io.*;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

/** A framework for testing CELT. 
 */
public class CELTTestSuite {

     /***************************************************************
      * Reads a KIF file consisting of pairs of (sentence... and
      * (answer... statements, where the sentence is a quoted string
      * and the answer is the expected KIF formula.
      */
    public static String test(KB kb) {

        StringBuffer result = new StringBuffer();
        boolean isSentence = false;
        boolean isAnswer = false;
        boolean isPair = false;
        boolean odd = true;
        int count = 1;

        result = result.append("<h2>CELT tests</h2>\n");
        result = result.append("<table><tr><td width='20%'><b>Sentence</b></td>");
        result = result.append("<td><b>Expected</b></td><td><b>Actual</b></td><td><b>ok</b></td></tr>");
        String celtTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
        if (celtTestDir == null)
            celtTestDir = "C:\\Program Files\\Apache Tomcat 4.0\\tests\\";
        String celtTestFile = celtTestDir + File.separator + "celtTest.txt";
        KIF test = new KIF();
        try {
            test.readFile(celtTestFile);
        }
        catch (Exception e) {
            return("Error in CELTTestSuite.test(): exception reading file: " + celtTestFile + ".\n" + e.getMessage());
        }
        System.out.println("INFO in CELTTestSuite.test(): num formulas: " + String.valueOf(test.formulaSet.size()));
        Iterator it = test.formulaSet.iterator();
        String sentence = null;
        String expectedAnswer = null;
        String celtResult = null;
        int numCorrect = 0;
        int numIncorrect = 0;
        String hostname = KBmanager.getMgr().getPref("hostname");
        if (hostname == null)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null)
            port = "8080";
        String html = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=en&kb=" + kb.name;

        while (it.hasNext()) {
            String formula = (String) it.next();
            if (formula.indexOf(";") != -1)
                formula = formula.substring(0,formula.indexOf(";"));
            System.out.println("INFO in CELTTestSuite.test(): Formula: " + formula);
            if (formula.startsWith("(sentence")) {
                sentence = formula.substring(11,formula.length()-2);
                isSentence = true;
                isAnswer = false;
                isPair = false;
            }
            else if (formula.startsWith("(answer")) {
                expectedAnswer = formula.substring(8,formula.length()-1);
                if (isSentence) 
                    isPair = true;
                isAnswer = true;
                isSentence = false;
            }
            else {
                System.out.println("Error in CELTTestSuite.test(): Unknown tag: " + formula);
                isSentence = false;
                isAnswer = false;
                isPair = false;
            }
            if (isPair) {
                try {
                    celtResult = kb.celt.submit(sentence);
                }
                catch (IOException ioe) {
                    System.out.print("Error in CELTTestSuite.test(): Exception submitting sentence ");
                    System.out.println(sentence + " to CELT. " + ioe.getMessage());
                }
                if (celtResult != null) {
                    Formula f = new Formula();
                    f.read(expectedAnswer);
                    boolean correct = false;
                    if (f.logicallyEquals(celtResult))
                        correct = true;
                    else
                        correct = false;
                    Formula c = new Formula();
                    c.read(celtResult);
                    if (!odd)
                        result = result.append("<tr><td valign=top>" + (new Integer(count)).toString());
                    else
                        result = result.append("<tr bgcolor=#eeeeee><td valign=top>" + (new Integer(count)).toString());
                    count++;
                    odd = !odd;
                    result = result.append(". " + sentence + "</td>");
                    result = result.append("<td valign=top>" + f.htmlFormat(html) + "</td>");
                    result = result.append("<td valign=top>" + c.htmlFormat(html) + "</td>");
                    if (correct) {
                        numCorrect++;
                        result = result.append("<td>ok</td>");
                    }
                    else {
                        numIncorrect++;
                        result = result.append("<td>fail</td>");
                    }
                    result = result.append("</tr>\n");
                }
                else {
                    numIncorrect++;
                    Formula f = new Formula();
                    f.read(expectedAnswer);
                    if (!odd)
                        result = result.append("<tr><td valign=top>" + (new Integer(count)).toString());
                    else
                        result = result.append("<tr bgcolor=#eeeeee><td valign=top>" + (new Integer(count)).toString());
                    count++;
                    odd = !odd;
                    result = result.append(". " + sentence + "</td>");
                    result = result.append("<td valign=top>" + f.htmlFormat(html) + "</td>");
                    result = result.append("<td></td><td valign=top>fail</td></tr>\n");
                }
            }
        }
        result = result.append("</table>\n");
        result = result.append("<P><b>Number correct: " + Integer.toString(numCorrect) + "</b><br>\n");
        result = result.append("<P><b>Number incorrect: " + Integer.toString(numIncorrect) + "</b><br>\n");
        return result.toString();
    }
}
