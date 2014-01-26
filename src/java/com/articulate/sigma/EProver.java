package com.articulate.sigma;
/** This code is copyright Articulate Software (c) 2014.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class EProver {

    private Process _eprover;
    private BufferedReader _reader; 
    private BufferedWriter _writer; 
    private BufferedReader _error;
    private static String __dummyKBdir = "/home/apease/Sigma/KBs";
    
    /** *************************************************************
     *  */
    public static void writeBatchConfig(String inputFilename) {

        try {
            System.out.println("INFO in EProver.writeBatchFile(): writing EBatchConfig.txt with KB file " + inputFilename);
            //File initFile = new File(KBmanager.getMgr().getPref("kbDir"), "EBatchConfig.txt");
            File initFile = new File(__dummyKBdir, "EBatchConfig.txt");
            PrintWriter pw = new PrintWriter(initFile);
    
            pw.println("% SZS start BatchConfiguration");
            pw.println("division.category LTB.SMO");
            pw.println("output.required Assurance");
            pw.println("output.desired Proof Answer");
            pw.println("limit.time.problem.wc 60");
            pw.println("% SZS end BatchConfiguration");
            pw.println("% SZS start BatchIncludes");
            pw.println("include('" + inputFilename + "').");
            pw.println("% SZS end BatchIncludes");
            pw.println("% SZS start BatchProblems");
            pw.println("% SZS end BatchProblems");
            pw.close();
        }
        catch (Exception e1) {
            e1.printStackTrace();
            System.out.println("Error in EProver.writeBatchFile()");
            System.out.println(e1.getMessage());
        }
    }
    
    /** *************************************************************
     */
    private EProver () {
    }

    /** *************************************************************
     * Creates a running instance of EProver.  To obtain a new
     * instance of EProver, use the static factory method
     * EProver.getNewInstance().
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the EProver executable.
     *
     * @throws IOException should not normally be thrown unless either
     *         EProver executable or database file name are incorrect
     *         
     * e_ltb_runner --interactive LTBSampleInput-AP.txt
     */
    public EProver (String executable, String kbFile) throws IOException {

        writeBatchConfig(kbFile);
        System.out.println("INFO in EProver(): executable: " + executable);
        System.out.println("INFO in EProver(): kbFile: " + kbFile);
        // String execString = executable + " --interactive " + 
                // KBmanager.getMgr().getPref("kbDir") + File.separator + "EBatchConfig.txt";
        String execString = executable + " --interactive " + 
                __dummyKBdir + File.separator + "EBatchConfig.txt";
        System.out.println("INFO in EProver(): executing: " + execString);
        _eprover = Runtime.getRuntime().exec(execString);
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_eprover.getErrorStream()));
        System.out.println("INFO in EProver(): initializing process");
        String line = null; 
        while (true) {
            line = _reader.readLine(); 
            System.out.println("INFO in EProver(): Return string: " + line);
            if (line.indexOf("Error:") != -1)
                throw new IOException(line);     
            if (line.indexOf("# Enter job") != -1) {
                break;
            }
        }
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }
    
    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    public String assertFormula(String formula) {
        //public String assertFormula(String formula) throws IOException {

        String result = "";
        try {
            // String assertion = SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,false);
            String assertion = "";
            _writer.write(assertion);
            _writer.flush();
            String line;
            do {
                line = _reader.readLine();  
                if (line.indexOf("Error:") != -1) 
                    throw new IOException(line);                
                System.out.println("INFO EProver(): Response: " + line);
                result += line + "\n";
                if (line.indexOf("# Processing finished") != -1) 
                    break;                
            } while (line != null);
        }
        catch (Exception ex) {
            System.out.println("Error in EProver.assertFormula(" + formula + ")");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Terminate this instance of EProver. 
     * <font color='red'><b>Warning:</b></font>After calling this function
     * no further assertions or queries can be done.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate () throws IOException {

        System.out.println();
        System.out.println("TERMINATING " + this);
        try {
            _writer.write("quit.\n");
            _writer.write("go.\n");
            _writer.close();
            _reader.close();
            System.out.println("DESTROYING the Process " + _eprover);
            System.out.println();
            _eprover.destroy();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax
     * @param timeLimit time limit for answering the query (in seconds)
     * @param bindingsLimit limit on the number of bindings
     * @return answer to the query 
     * @throws IOException should not normally be thrown
     */
    public String submitQuery (String formula, KB kb) {
        //public String submitQuery (String formula, int timeLimit, int bindingsLimit) throws IOException {
                
        String result = "";
        System.out.println("INFO in EProver.submitQuery() formula: " + formula);
        //Formula f = new Formula();
        //f.read(formula);
        //SUMOformulaToTPTPformula sfttptp = new SUMOformulaToTPTPformula();

        try {
            //ArrayList<String> al = sfttptp.tptpParse(f, true, kb);
            String query = SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,true);
            System.out.println("INFO in EProver.submitQuery() TPTP formula: " + query);
            //System.out.println("INFO in EProver.submitQuery() TPTP formula: " + al.get(0));
            //String conjecture = "fof(conj1,conjecture, " + al.get(0) + ").";
            String conjecture = "fof(conj1,conjecture, " + query + ").";
            //String tptpAssert = "( ( ? [V__X] : s__subclass(V__X,s__Object) ) )";
            //System.out.println("INFO in EProver.submitQuery() TPTP formula: " + tptpAssert);
            //String conjecture = "fof(conj1,conjecture, " + tptpAssert + ").";
            System.out.println("INFO in EProver.submitQuery() conjecture: " + conjecture);
            _writer.write(conjecture + "\n");
            _writer.write("go.\n");
            _writer.flush();
            System.out.println("INFO in EProver.submitQuery() executing query.");
            String line = null;
            boolean inProof = false;
            do {
                line = _reader.readLine();
                if (line.indexOf("# Enter job") != -1) 
                    break;
                if (line.indexOf("# SZS status") != -1) 
                    inProof = true;            
                if (inProof)
                    result += line + "\n";
                //System.out.println(line);                         
            } while (line != null);      
        }
        catch (Exception ex) {
            System.out.println("Error in EProver.submitQuery(): " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * A simple test. Works as follows: 
     * <ol>
     *   <li>start E;</li>
     *   <li>make an assertion;</li>
     *   <li>submit a query;</li>
     *   <li>terminate E</li>
     *</ol>
     */
    public static void main (String[] args) throws Exception {

        /*
        String initialDatabase = "SUMO-v.kif";
        EProver eprover = EProver.getNewInstance(initialDatabase);
        eprover.setCommandLineOptions("--cpu-limit=600 --soft-cpu-limit=500 -xAuto -tAuto -l 4 --tptp3-in");
        KBmanager.getMgr().setPref("eprover","/home/apease/Programs/E/Prover/eprover");
        System.out.print(eprover.submitQuery("(holds instance ?X Relation)",5,2));
*/
        try {
            System.out.println("INFO in EProver.main()");
            //KBmanager.getMgr().initializeOnce();
            //KB kb = KBmanager.getMgr().getKB("SUMO");
            KB kb = null;
            System.out.println("------------- INFO in EProver.main() completed initialization--------");
            EProver eprover = new EProver("/home/apease/Programs/E/PROVER/e_ltb_runner",
                    "/home/apease/Sigma/KBs/SUMO.tptp");

            System.out.println(eprover.submitQuery("(subclass ?X Object)",kb));
            eprover.terminate();
        } 
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // System.out.print(eprover.assertFormula("(human Socrates)"));
        // System.out.print(eprover.assertFormula("(holds instance Adam Human)"));
        // System.out.print(eprover.submitQuery("(human ?X)", 1, 2));
        // System.out.print(eprover.submitQuery("(holds instance ?X Human)", 5, 2));
        
    }
    
}

