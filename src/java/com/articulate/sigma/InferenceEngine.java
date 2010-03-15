/** This code is copyright Articulate Software
2009. This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  

Please cite the following article in any publication with references
when addressing Sigma:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net 
*/
package com.articulate.sigma;

import java.io.IOException;

/**
 * @author Adam Pease
 */
public abstract class InferenceEngine {

	public static abstract class EngineFactory {
		public abstract InferenceEngine createWithFormulas(Iterable<String> formulaSource);
		public abstract InferenceEngine createFromKBFile(String kbFileName);
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
    public abstract String submitQuery (String formula,int timeLimit,int bindingsLimit) 
        throws IOException;

    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    public abstract String assertFormula(String formula) throws IOException;
    
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
