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

package com.articulate.sigma;

import java.util.*;
import java.io.*;

/** ***************************************************************
 * The Sigma theorem prover. A simple resolution prover in Java.
 */
public class STP extends InferenceEngine {

     /** The knowledge base */
    KB kb;

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {

        return "An STP instance";
    }

    public static class STPEngineFactory extends EngineFactory {

        public InferenceEngine createWithFormulas(Iterable formulaSource) {  
            return new STP(formulaSource);
        }

        public InferenceEngine createFromKBFile(String kbFileName) {
            return STP.getNewInstance(kbFileName);
        }
    }

    /** *************************************************************
     */
    private STP(String kbFileName) throws Exception {
    
        String error = null;
                
        File kbFile = null;
        if (error == null) {
            kbFile = new File(kbFileName);
            if (!kbFile.exists() ) {
                error = ("The file " + kbFileName + " does not exist");
                System.out.println("INFO in STP(): " + error);
                KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                             + "\n<br/>" + error + "\n<br/>");
            }
        }
        
        if (error == null) {
            KIF kif = new KIF();
            kif.setParseMode(KIF.RELAXED_PARSE_MODE);
            kif.readFile(kbFile.getCanonicalPath());
            if (!kif.formulaSet.isEmpty()) {
                //formulas.ensureCapacity(kif.formulaSet.size());
                //loadFormulas(kif.formulaSet);
            }
        }
    }    

    /** *************************************************************
     */
    public STP(Iterable formulaSource) { 
    
        //loadFormulas(formulaSource);
    }
    
    /** *************************************************************
     */
    public static STP getNewInstance(String kbFileName) {

        STP res = null;
        try {
            res = new STP(kbFileName);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return res;
    }

    /** *************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax
     * @param timeLimit time limit for answering the query (in seconds)
     * @param bindingsLimit limit on the number of bindings
     * @return answer to the query (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public  String submitQuery (String formula,int timeLimit,int bindingsLimit) 
        throws IOException {

        return null;
    }

    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String assertFormula(String formula) throws IOException {

        //formulas.add(formula);
        //Formulas asserted through this method will always be used.
        
        return null;
    }
    
    /** *************************************************************
     * Terminates this instance of InferenceEngine. 
     * <font color='red'><b>Warning:</b></font>After calling this functions
     * no further assertions or queries can be done.
     * 
     * Some inference engines might not need/support termination. In that case this
     * method does nothing.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate()
    	throws IOException
    {
    }
}

