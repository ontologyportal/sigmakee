
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

import java.io.*;

/**
 * Class for invoking the KIF version of Vampire from Java. Vampire takes XML input
 * and returns XML output.  Input forms are either <query optionalArgs>KIF formula</query>
 * or <assertion>KIF formula</assertion>.  Results of queries have the following form:
 * <queryResponse>
 *   <answer=yes/no number ='#'>
 *     <bindingSet type='definite/disjunctive'>
 *       <binding>
 *         <var name='' value=''>
 *       </binding>
 *     </bindingSet>
 *     <proof>
 *       <proofStep>
 *         <premises>
 *           <premise>
 *             <clause/formula number='#'>
 *               KIF formula
 *             </clause>
 *           </premise>
 *         </premises>
 *         <conclusion> 
 *           <clause/formula number='#'>
 *             KIF formula
 *           </clause>
 *         </conclusion>
 *       </proofStep>
 *     </proof>
 *   </answer>
 *   <summary proofs='#'>
 * </queryResponse>
 * 
 * Note that if the result of a query is not a variable binding, then the <bindingSet>
 * element will be omitted.
 *
 * @author Andrei Voronkov
 * @since 14/08/2003, Acapulco 
 */
public class Vampire {

    public String EMPTY_FILE;
    private Process _vampire;
    private BufferedReader _reader; 
    private BufferedWriter _writer; 
    private BufferedReader _error; 

    /** *************************************************************
     * Create a running instance of Vampire.
     *
     * @param kbFileName file name of the initial knowledge base to be 
     *        downloaded by Vampire.
     * @throws IOException should not normally be thrown unless either
     *         Vampire executable or database file name are incorrect
     */
    public Vampire (String kbFileName) throws IOException {

        String VAMPIRE_EXECUTABLE;
        String line = null; 
        if (KBmanager.getMgr().getPref("inferenceEngine") == null)
            KBmanager.getMgr().setPref("inferenceEngine","C:\\Artic\\vampire\\Vampire_VSWorkspace\\vampire\\Release\\kif.exe");  
        VAMPIRE_EXECUTABLE = KBmanager.getMgr().getPref("inferenceEngine"); 

        String VAMPIRE_DIRECTORY = VAMPIRE_EXECUTABLE.substring(0,VAMPIRE_EXECUTABLE.lastIndexOf(File.separator));
        if (VAMPIRE_DIRECTORY.substring(VAMPIRE_DIRECTORY.length()-1,VAMPIRE_DIRECTORY.length()).equals(File.separator)) 
            VAMPIRE_DIRECTORY = VAMPIRE_DIRECTORY.substring(0,VAMPIRE_DIRECTORY.length()-1);
        System.out.println("INFO in Vampire(): Setting inference engine to: " + VAMPIRE_EXECUTABLE);
        System.out.println("INFO in Vampire(): Setting directory to: " + VAMPIRE_DIRECTORY);
        EMPTY_FILE = VAMPIRE_DIRECTORY + File.separator + "emptyFile.kif";

        if (!(new File(VAMPIRE_EXECUTABLE)).exists())
            throw new IOException("Error in Vampire(): Executable file " + VAMPIRE_EXECUTABLE + " does not exist.");
        if (!(new File(VAMPIRE_DIRECTORY + File.separator + kbFileName)).exists())
            throw new IOException("Error in Vampire(): KB file " + VAMPIRE_DIRECTORY + File.separator + kbFileName + " does not exist.");
        System.out.println("INFO in Vampire(): Starting vampire as "+VAMPIRE_EXECUTABLE+" "+VAMPIRE_DIRECTORY+File.separator+kbFileName);
    
        _vampire = Runtime.getRuntime().exec(VAMPIRE_EXECUTABLE + " " + VAMPIRE_DIRECTORY + File.separator + kbFileName);

        _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_vampire.getErrorStream()));

        while (_reader.ready() || _error.ready()) {
            if (_reader.ready())
                line = _reader.readLine();
            else if (_error.ready()) 
                line = _error.readLine();
            System.out.println("INFO in Vampire(): Return string: " + line);
            if (line.indexOf("Error:") != -1)
                throw new IOException(line);            
        }
        _writer = new BufferedWriter(new OutputStreamWriter(_vampire.getOutputStream()));
    }
    
    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    public String assertFormula (String formula) 
        throws IOException
    {
        System.out.println("INFO Vampire.assertFormula(): <assertion> " + formula + " </assertion>");
        _writer.write("<assertion> " + formula + " </assertion>\n");
        _writer.flush();
        String result = "";
        for (;;) {
            String line = _reader.readLine();
            if (line.indexOf("Error:") != -1) {
                throw new IOException(line);
            }
            System.out.println("INFO Vampire.assertFormula(): Response: " + line);
            result += line + "\n";
            if (line.indexOf("</assertionResponse>") != -1) {
                return result;
            }
        }
    }

    /** *************************************************************
     * Terminate this instance of Vampire. 
     * <font color='red'><b>Warning:</b></font>After calling this functions
     * no further assertions or queries can be done.
     *
     * @throws IOException should not normally be thrown
     */
     public void terminate () 
        throws IOException
     {
         _writer.write("<bye/>\n");
         _writer.close();
         _reader.close();
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
    public String submitQuery (String formula,int timeLimit,int bindingsLimit) 
        throws IOException
    {
        String result = "";
        
        System.out.println("INFO in Vampire.submitQuery(): <query timeLimit='" + timeLimit + "' bindingsLimit='" + bindingsLimit + "'> " + formula + " </query>\n");
        try {
            _writer.write("<query timeLimit='" + timeLimit + "' bindingsLimit='" + bindingsLimit + "'> " + formula + " </query>\n");
            _writer.flush();
        }
        catch (IOException ioe) {
            System.out.println("Error in Vampire.submitQuery(): " + ioe.getMessage());
            throw new IOException("Error in Vampire.submitQuery(): " + ioe.getMessage());
        }
        for (;;) {
            String line = _reader.readLine();

            if (line.indexOf("Error:") != -1) {
                throw new IOException(line);
            }
            result += line + "\n";
            if ((line.indexOf("</queryResponse>") != -1) ||      // result is ok.
                (line.indexOf("</assertionResponse>") != -1))  { // result is syntax error.
                System.out.println("INFO in Vampire.submitQuery(): ===================================");
                System.out.println(result);
                result = result.replaceAll("&lt;","<");
                result = result.replaceAll("&gt;",">");
                return result;
            }
        }
    }

    /** *************************************************************
     * A simple test. Works as follows: 
     * <ol>
     *   <li>start Vampire;</li>
     *   <li>make an assertion;</li>
     *   <li>submit a query;</li>
     *   <li>terminate Vampire.</li>
     *</ol>
     */
    public static void main (String[] args)
        throws Exception
    {
        String initialDatabase = "SUMOonly-v.kif";
        Vampire vampire = new Vampire(initialDatabase);
        System.out.print(vampire.submitQuery("(holds instance ?X Relation)",5,2));

        // System.out.print(vampire.assertFormula("(human Socrates)"));
        // System.out.print(vampire.assertFormula("(holds instance Adam Human)"));
        // System.out.print(vampire.submitQuery("(human ?X)", 1, 2));
        // System.out.print(vampire.submitQuery("(holds instance ?X Human)", 5, 2));
        vampire.terminate();
    }
    
}