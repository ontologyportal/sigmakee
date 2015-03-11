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

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class EProver {

    private ProcessBuilder _builder;
    private Process _eprover;
    private BufferedReader _reader; 
    private BufferedWriter _writer; 
    private BufferedReader _error;
    private static String __dummyKBdir = KBmanager.getMgr().getPref("kbDir");

    /** *************************************************************
     *  */
    public static void writeBatchConfig(String inputFilename) {

    	//String inputFilename = "/home/apease/Sigma/KBs/SUMO.tptp.withSortal.plainFormulae";
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
     * Add a new tptp file to EBatching.txt
     * TODO: This function might not be necessary if we find a way to 
     * directly add assertion into opened inference engine (e_ltb_runner)
     *  */
    public static void addBatchConfig(String inputFilename) {

        File initFile = new File(__dummyKBdir, "EBatchConfig.txt");
        HashSet<String> ebatchfiles = new HashSet<>();
        ebatchfiles.add(inputFilename);

        // Collect existed tptp files
        try {
            FileInputStream fis = new FileInputStream(initFile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader in = new BufferedReader(isr);
            String line = in.readLine();
            while (line != null) {
                String split = "include('";
                int isEbatchFile = line.indexOf(split);
                if (isEbatchFile != -1) {
                    String ebatchfile = line.substring(split.length(), line.lastIndexOf("')"));
                    ebatchfiles.add(ebatchfile);
                }
                line = in.readLine();
            }
            fis.close();
            isr.close();
            in.close();
        } 
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in EProver.addBatchFile()");
            System.out.println(e.getMessage());
        }

        // write existed tptp files and new tptpfiles (inputFilename) into EBatching.txt
        try {
            PrintWriter pw = new PrintWriter(initFile);
            pw.println("% SZS start BatchConfiguration");
            pw.println("division.category LTB.SMO");
            pw.println("output.required Assurance");
            pw.println("output.desired Proof Answer");
            pw.println("limit.time.problem.wc 60");
            pw.println("% SZS end BatchConfiguration");
            pw.println("% SZS start BatchIncludes");
            for (String ebatchfile : ebatchfiles) {
                pw.println("include('" + ebatchfile + "').");
            }
            pw.println("% SZS end BatchIncludes");
            pw.println("% SZS start BatchProblems");
            pw.println("% SZS end BatchProblems");
            pw.close();
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Error in EProver.writeBatchFile()");
            System.out.println(e.getMessage());
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

        // executable = /PATH_TO_E_LTB_RUNNER;
        ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                executable, "--interactive", __dummyKBdir + File.separator + "EBatchConfig.txt",
                executable.substring(0, executable.lastIndexOf("/")) + File.separator + "eprover"));

        _builder = new ProcessBuilder(commands);
        _builder.redirectErrorStream(true);
        _eprover = _builder.start();
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }

    /** *************************************************************
     * Create a running instance of EProver.
     * Difference from EProver(String executable, String kbFile): move 
     * writeBatchConfig out of EProver(String executable)
     * TODO: This function might not be necessary if we find a way to 
     * directly add assertion into opened inference engine (e_ltb_runner)
     *
     * @param executable
     * @throws IOException
     */
    public EProver (String executable) throws IOException {

        if (_builder != null && _eprover != null)
            terminate();

        ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                executable, "--interactive", __dummyKBdir + File.separator + "EBatchConfig.txt",
                executable.substring(0, executable.lastIndexOf("/")) + File.separator + "eprover"));

        _builder = new ProcessBuilder(commands);
        _builder.redirectErrorStream(true);
        _eprover = _builder.start();
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
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
     * Add the axiom's tptp formula to tptp file "userAssertionFilename"
     *
     * TODO: This function might not be necessary if we find a way to directly add assertion into opened inference engine (e_ltb_runner)
     */
    public boolean assertFormula(String userAssertionTPTP, KB kb, EProver eprover, ArrayList<Formula> parsedFormulas, boolean tptp) {

        boolean allAdded = (eprover != null);
        PrintWriter pw = null;
        int axiomIndex = 0;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)));
            ArrayList<Formula> processedFormulas = new ArrayList<Formula>();
            Iterator<Formula> it2 = parsedFormulas.iterator();
            while (it2.hasNext()) {
                axiomIndex++;
                processedFormulas.clear();
                Formula parsedF = it2.next();          // 1. Preproccess the formula.
                FormulaPreprocessor fp = new FormulaPreprocessor();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP.
                    if (tptp) {
                        SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                        stptp._f = parsedF;
                        stptp.tptpParse(parsedF,false, kb, processedFormulas);
                    }
                    // 3. Write to new tptp file
                    if (eprover != null) {
                        List<String> tptpFormulas = parsedF.getTheTptpFormulas();
                        Iterator<String> tptpIt = tptpFormulas.iterator();
                        while (tptpIt.hasNext()) {
                            String theTPTPFormula = tptpIt.next();
                            pw.print("fof(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                            pw.println(",axiom,(" + theTPTPFormula + ")).");
                        }
                        pw.flush();
                    }
                }
            }
        } 
        catch (ParseException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            try {
                if (pw != null) pw.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return allAdded;
    }

    /** *************************************************************
     * Terminate this instance of EProver. 
     * <font color='red'><b>Warning:</b></font>After calling this function
     * no further assertions or queries can be done.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate() throws IOException {

        System.out.println();
        System.out.println("TERMINATING " + this);
        try {
            _writer.write("quit.\n");
            _writer.write("go.\n");
            _writer.flush();
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
     * @param kb current knowledge base
     * @return answer to the query 
     * @throws IOException should not normally be thrown
     */
    public String submitQuery(String formula, KB kb) {

        String result = "";

        try {
            String query = SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,true);
            String conjecture = "fof(conj1,conjecture, " + query + ").";
            System.out.println("\nINFO in EProver.submitQuery() conjecture: " + conjecture + "\n");
            _writer.write(conjecture + "\n");
            _writer.write("go.\n");
            _writer.flush();

            String line = _reader.readLine();
            boolean inProof = false;
            while (line != null) {
                if (line.indexOf("# SZS status") != -1)
                    inProof = true;
                if (inProof) {
                    if (line.indexOf("# Enter job") != -1)
                        break;
                    result += line + "\n";
                }
                line = _reader.readLine();
            }
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
    public static void main(String[] args) throws Exception {

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

