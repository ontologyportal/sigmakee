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
import java.util.*;

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

    private Process _vampire;
    private BufferedReader _reader; 
    private BufferedWriter _writer; 
    private BufferedReader _error; 

    /** *************************************************************
     * This static factory method returns a new Vampire instance.
     *
     * @param kbFileName The complete (absolute) pathname of the KB
     * file that will be used to populate the inference engine
     * instance with assertions.
     *
     * @throws IOException should not normally be thrown unless either
     *         Vampire executable or database file name are incorrect
     */
    public static Vampire getNewInstance ( String kbFileName ) {

        Vampire vpr = null;
        String error = null;

        try {

            String inferenceEngine = KBmanager.getMgr().getPref("inferenceEngine");

            if ( ! Formula.isNonEmptyString(inferenceEngine) ) {
                error = "No pathname has been set for \"inferenceEngine\"";
                System.out.println( "Error in Vampire.getNewInstance(): " + error );
                KBmanager.getMgr().setError( KBmanager.getMgr().getError()
                                             + "\n<br/>" + error + "\n<br/>" );
            }

            File vampireExecutable = null;
            if ( error == null ) {
                vampireExecutable = new File( inferenceEngine );
                if ( ! vampireExecutable.exists() ) {
                    error = ( "The executable file " + vampireExecutable.getCanonicalPath() + " does not exist" );
                    System.out.println( "Error in Vampire.getNewInstance(): " + error );
                    KBmanager.getMgr().setError( KBmanager.getMgr().getError()
                                                 + "\n<br/>" + error + "\n<br/>" );
                }
            }

            File kbFile = null;
            if ( error == null ) {
                kbFile = new File( kbFileName );
                if ( ! kbFile.exists() ) {
                    error = ( "The file " + kbFileName + " does not exist" );
                    System.out.println( "INFO in Vampire.getNewInstance(): " + error );
                    KBmanager.getMgr().setError( KBmanager.getMgr().getError()
                                                 + "\n<br/>" + error + "\n<br/>" );
                }
            }

            if ( error == null ) {

                File vampireDirectory = vampireExecutable.getParentFile();

                System.out.println( "INFO in Vampire.getNewInstance(): executable == " + vampireExecutable.getCanonicalPath() );
                System.out.println( "INFO in Vampire.getNewInstance(): directory == " + vampireDirectory.getCanonicalPath() );

                // It should only ever be necessary to write this file once.
                File initFile = new File( vampireDirectory, "init-v.kif" );
                if ( ! initFile.exists() ) {
                    PrintWriter pw = new PrintWriter( initFile );
                    pw.println( "(instance Process Entity)" );
                    pw.flush();
                    try {
                        pw.close();
                    }
                    catch ( Exception e1 ) {
                    }
                }

                System.out.println( "INFO in Vampire.getNewInstance(): Starting vampire as " 
                                    + vampireExecutable.getCanonicalPath() + " " + initFile.getCanonicalPath() );
        
                Vampire vprInst = new Vampire( vampireExecutable, initFile );
                if ( vprInst instanceof Vampire ) {
                    KIF kif = new KIF();
                    kif.setParseMode( KIF.RELAXED_PARSE_MODE );
                    kif.readFile( kbFile.getCanonicalPath() );
                    if ( ! kif.formulaSet.isEmpty() ) {
                        List badFormulas = new ArrayList();
                        Iterator it = kif.formulaSet.iterator();
                        String formStr = null;
                        String response = null;
                        int goodCount = 0;
                        long start = System.currentTimeMillis();
                        while ( it.hasNext() ) {
                            formStr = (String) it.next();
                            response = vprInst.assertFormula( formStr );
                            if ( ! (response.indexOf("Formula has been added") >= 0) ) {
                                badFormulas.add( formStr );
                            }
                            else {
                                goodCount++ ;
                            }
                        }
                        long duration = (System.currentTimeMillis() - start);

                        System.out.println( goodCount 
                                            + " formulas asserted to " 
                                            + vprInst 
                                            + " in " 
                                            + (duration / 1000.0) 
                                            + " seconds" );
                        if ( ! badFormulas.isEmpty() ) {
                            int bc = badFormulas.size();
                            Iterator it2 = badFormulas.iterator();
                            System.out.println( "INFO in Vampire.getNewInstance(): "
                                                + bc
                                                + " FORMULA"
                                                + ((bc == 1) ? "" : "S")
                                                + " REJECTED from " 
                                                + kbFile.getCanonicalPath() );
                            int badCount = 1;
                            String badStr = null;
                            String mgrErrStr = KBmanager.getMgr().getError();
                            while ( it2.hasNext() ) {
                                badStr = ( "[" + badCount++ + "] " + ((String)it2.next()) );
                                System.out.println( badStr );
                                mgrErrStr += ( "\n<br/>" + "Bad formula: " + badStr + "\n<br/>" );
                            }
                            KBmanager.getMgr().setError( mgrErrStr );
                        }
            
                        if ( goodCount > 0 ) {

                            // If we've made it this far, we have a usable Vampire instance.
                            vpr = vprInst;
                        }
                    }
                }
            }
        }
        catch ( Exception ex ) {
            System.out.println( ex.getMessage() );
            ex.printStackTrace();
        }
        return vpr;
    }

    /** *************************************************************
     * To obtain a new instance of Vampire, use the static factory
     * method Vampire.getNewInstance().
     */
    private Vampire () {
    }


    /** *************************************************************
     * Creates a running instance of Vampire.  To obtain a new
     * instance of Vampire, use the static factory method
     * Vampire.getNewInstance().
     *
     * @param executable A File object denoting the platform-specific
     * Vampire executable.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Vampire executable.
     *
     * @throws IOException should not normally be thrown unless either
     *         Vampire executable or database file name are incorrect
     */
    private Vampire ( File executable, File kbFile ) throws IOException {

        _vampire = Runtime.getRuntime().exec( executable.getCanonicalPath() + " " + kbFile.getCanonicalPath() );

        _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_vampire.getErrorStream()));

        String line = null; 
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
    public String assertFormula(String formula) throws IOException {
        String result = "";
        try {
            String assertion = ( "<assertion> " + formula + " </assertion>\n" );

            // System.out.println( "INFO Vampire.assertFormula(): " + assertion );

            _writer.write( assertion );
            _writer.flush();
            for (;;) {
                String line = _reader.readLine();
                if (line.indexOf("Error:") != -1) {
                    throw new IOException(line);
                }

                // System.out.println("INFO Vampire.assertFormula(): Response: " + line);

                result += line + "\n";
                if (line.indexOf("</assertionResponse>") != -1) {
                    break;
                }
            }
        }
        catch ( Exception ex ) {
            System.out.println("Error in Vampire.assertFormula(" + formula + ")");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
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
        System.out.println();
        System.out.println( "TERMINATING " + this );
        try {
            _writer.write("<bye/>\n");
            _writer.close();
            _reader.close();
            System.out.println( "DESTROYING the Process " + _vampire );
            System.out.println();
            _vampire.destroy();
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
        }
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

        String query = ( "<query timeLimit='" 
                         + timeLimit 
                         + "' bindingsLimit='" 
                         + bindingsLimit 
                         + "'> " 
                         + formula 
                         + " </query>\n" );
        
        System.out.println("INFO in Vampire.submitQuery(): " + query );

        try {
            _writer.write( query );
            _writer.flush();
        }
        catch ( Exception ex ) {
            System.out.println("Error in Vampire.submitQuery(): " + ex.getMessage());
            ex.printStackTrace();
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
        String initialDatabase = "SUMO-v.kif";
        Vampire vampire = Vampire.getNewInstance(initialDatabase);
        System.out.print(vampire.submitQuery("(holds instance ?X Relation)",5,2));

        // System.out.print(vampire.assertFormula("(human Socrates)"));
        // System.out.print(vampire.assertFormula("(holds instance Adam Human)"));
        // System.out.print(vampire.submitQuery("(human ?X)", 1, 2));
        // System.out.print(vampire.submitQuery("(holds instance ?X Human)", 5, 2));
        vampire.terminate();
    }
    
}
