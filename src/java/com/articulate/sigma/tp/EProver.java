package com.articulate.sigma.tp;
/** This code is copyright Articulate Software (c) 2014.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also https://github.com/ontologyportal/sigmakee

Authors:
Adam Pease
Infosys LTD.
*/

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

public class EProver {

    private ProcessBuilder _builder;
    private Process _eprover;
    private BufferedReader _reader; 
    private BufferedWriter _writer;
    private static String kbdir;
    private static int axiomIndex = 0;
    public ArrayList<String> output = new ArrayList<>();
    public StringBuffer qlist = null;

    /** *************************************************************
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        for (String s : output)
            sb.append(s + "\n");
        return sb.toString();
    }

    /** *************************************************************
     * Create a new batch specification file.
     *
     * e_ltb_runner processes a batch specification file; it contains
     * a specification of the background theory, some options, and a
     * number of individual job requests. It is used with the option
     * --interactive.
     *
     * @param inputFilename contains TPTP assertions
     * @param timeout time limit in E
     *  */
    public static void writeBatchConfig(String inputFilename, int timeout) {

    	try {
            System.out.println("INFO in EProver.writeBatchFile(): writing EBatchConfig.txt with KB file " + inputFilename);
            File initFile = new File(kbdir, "EBatchConfig.txt");
            PrintWriter pw = new PrintWriter(initFile);
    
            pw.println("% SZS start BatchConfiguration");
            pw.println("division.category LTB.SMO");
            pw.println("output.required Assurance");
            pw.println("output.desired Proof Answer");
            pw.println("limit.time.problem.wc " + timeout);
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
     * Update batch specification file.
     *
     * "inputFilename" is added into existing batch specification file
     * for inference.
     *
     * @param inputFilename contains TPTP assertions
     * @param timeout time limit in E
     *  */
    public static void addBatchConfig(String inputFilename, int timeout) {

        File initFile = new File(kbdir, "EBatchConfig.txt");
        HashSet<String> ebatchfiles = new HashSet<>();
        if (inputFilename != null && !inputFilename.isEmpty())
            ebatchfiles.add(inputFilename);

        // Collect existing TPTP files
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
            System.out.println("Error in EProver.addBatchConfig()");
            System.out.println(e.getMessage());
        }

        // write existing TPTP files and new tptp files (inputFilename) into EBatchConfig.txt
        try {
            PrintWriter pw = new PrintWriter(initFile);
            pw.println("% SZS start BatchConfiguration");
            pw.println("division.category LTB.SMO");
            pw.println("output.required Assurance");
            pw.println("output.desired Proof Answer");
            pw.println("limit.time.problem.wc " + timeout);
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
            System.out.println("Error in EProver.addBatchConfig()");
            System.out.println(e.getMessage());
        }
    }
    
    /** *************************************************************
     * Create a new batch specification file, and create a new running
     * instance of EProver.
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the EProver executable.
     * @throws IOException should not normally be thrown unless either
     *         EProver executable or database file name are incorrect
     *         
     * e_ltb_runner -- interactive LTBSampleInput-AP.txt
     */
    public EProver (String executable, String kbFile) throws IOException {

        if (this._eprover != null)
            this.terminate();
        kbdir = KBmanager.getMgr().getPref("kbDir");
        writeBatchConfig(kbFile, 60);
        System.out.println("INFO in EProver(): executable: " + executable);
        System.out.println("INFO in EProver(): kbFile: " + kbFile);
        if (!(new File(kbFile)).exists()) {
            System.out.println("EProver(): no such file: " + kbFile + ". Creating it.");
            KB kb = KBmanager.getMgr().getKB(kbFile);

        }
        // To make sigma work on windows. 
        //If OS is not detected as Windows it will use the same directory as set in "inferenceEngine".  
        String eproverPath = null;
        String _OS = System.getProperty("os.name");
        if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*")){
        	eproverPath=KBmanager.getMgr().getPref("eproverPath");
        }
		eproverPath = eproverPath != null && eproverPath.length() != 0 ? eproverPath
				: executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                executable, "--interactive", kbdir + File.separator + "EBatchConfig.txt",
                eproverPath));

        System.out.println("EProver(): command: " + commands);
        _builder = new ProcessBuilder(commands);
        _builder.redirectErrorStream(false);
        _eprover = _builder.start();
        System.out.println("EProver(): new process: " + _eprover);
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }

    /** *************************************************************
     * Create a running instance of EProver based on existing batch
     * specification file.
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     * @throws IOException
     */
    public EProver (String executable) throws IOException {

        if (this._eprover != null)
            this.terminate();

        kbdir = KBmanager.getMgr().getPref("kbDir");

        // To make sigma work on windows
        //If OS is not detected as Windows it will use the same directory as set in "inferenceEngine".
               
        String eproverPath = null;
        String _OS = System.getProperty("os.name");
        if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*")){                    
        	eproverPath=KBmanager.getMgr().getPref("eproverPath");
        }
		eproverPath = eproverPath != null && eproverPath.length() != 0 ? eproverPath
				: executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                executable, "--interactive", kbdir + File.separator + "EBatchConfig.txt",
                eproverPath));

        System.out.println("EProver(): command: " + commands);
        _builder = new ProcessBuilder(commands);
        _builder.redirectErrorStream(false);
        _eprover = _builder.start();
        System.out.println("EProver(): process: " + _eprover);
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }
    
    
    /** *************************************************************
     * Create a running instance of EProver based on existing batch
     * specification file.
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     * @param maxAnswers - Limit the answers upto maxAnswers only
     * @throws IOException
     */
    public EProver (String executable,int maxAnswers) throws IOException {

        if (this._eprover != null)
            this.terminate();
        kbdir = KBmanager.getMgr().getPref("kbDir");
        // To make sigma work on windows
        //If OS is not detected as Windows it will use the same directory as set in "inferenceEngine".
        String eproverPath = null;
        String _OS = System.getProperty("os.name");
        if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*")){                    
        	eproverPath = KBmanager.getMgr().getPref("eproverPath");
        }
		eproverPath = eproverPath != null && eproverPath.length() != 0 ? eproverPath
				: executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        //ArrayList<String> commands = new ArrayList<>(Arrays.asList(
        //        executable,"--answers=" + maxAnswers, "--interactive", __dummyKBdir + File.separator + "EBatchConfig.txt",
       //        eproverPath));
        String batchPath = kbdir + File.separator + "EBatchConfig.txt";
        ArrayList<String> commands = new ArrayList<>(Arrays.asList(
                executable, "--interactive", batchPath,
                eproverPath));
        System.out.println("EProver(): command: " + commands);
        _builder = new ProcessBuilder(commands);
        _builder.redirectErrorStream(false);
        _eprover = _builder.start();
        System.out.println("EProver(): process: " + _eprover);
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }

    /** *************************************************************
     * Add an assertion for inference.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion
     * @throws IOException should not normally be thrown
     */
    public String assertFormula(String formula) {

        System.out.println("EProver.assertFormula(1): process: " + _eprover);
        String result = "";
        output = new ArrayList<String>();
        try {
            String assertion = "";
            _writer.write(assertion);
            System.out.println("\nINFO in EProver.assertFormula(1) write: " + assertion + "\n");
            _writer.flush();
            String line;
            do {
                line = _reader.readLine();
                System.out.println("\nINFO in EProver.assertFormula(1) read: " + line);
                if (line.indexOf("Error:") != -1)
                    throw new IOException(line);
                System.out.println("INFO EProver(): Response: " + line);
                result += line + "\n";
                output.add(line);
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
     * Add an assertion for inference.
     *
     * @param userAssertionTPTP asserted formula in the TPTP syntax
     * @param kb Knowledge base
     * @param eprover an instance of EProver
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp convert formula to TPTP if tptp = true
     * @return true if all assertions are added for inference
     *
     * TODO: This function might not be necessary if we find a way to
     * directly add assertion into opened inference engine (e_ltb_runner)
     */
    public boolean assertFormula(String userAssertionTPTP, KB kb, EProver eprover,
                                 ArrayList<Formula> parsedFormulas, boolean tptp) {

        System.out.println("EProver.assertFormula(2): process: " + _eprover);
        boolean allAdded = (eprover != null);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)));
            HashSet<Formula> processedFormulas = new HashSet<Formula>();
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                FormulaPreprocessor fp = new FormulaPreprocessor();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP.
                    Set<String> tptpFormulas = new HashSet<>();
                    if (tptp) {
                        SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                        for (Formula p : processedFormulas)
                            if (!p.isHigherOrder(kb))
                                tptpFormulas.add(stptp.tptpParseSUOKIFString(p.getFormula(),false));
                    }
                    // 3. Write to new tptp file
                    if (eprover != null) {
                        Iterator<String> tptpIt = tptpFormulas.iterator();
                        while (tptpIt.hasNext()) {
                            String theTPTPFormula = tptpIt.next();
                            pw.print("fof(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                            pw.println(",axiom,(" + theTPTPFormula + ")).");
                            String tptpstring = "fof(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
                            System.out.println("INFO in EProver.assertFormula(2): TPTP for user assertion = " + tptpstring);
                        }
                        pw.flush();
                    }
                }
            }
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
     * Terminate this instance of EProver. After calling this function
     * no further assertions or queries can be done.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate() throws IOException {

        if (this._eprover == null)
            return;

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

        System.out.println("EProver.submitQuery(): process: " + _eprover);
        String result = "";
        try {
            String query = SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,true);
            this.qlist = SUMOformulaToTPTPformula.qlist;
            String conjecture = "fof(conj1,conjecture, " + query + ").";
            System.out.println("\nINFO in EProver.submitQuery() write: " + conjecture + "\n");
            _writer.write(conjecture + "\n");
            System.out.println("\nINFO in EProver.submitQuery() write: go.");
            _writer.write("go.\n");
            _writer.flush();
            output = new ArrayList<>();
            String line = _reader.readLine();
            System.out.println("INFO in EProver.submitQuery(1): line: " + line);
            line = _reader.readLine();
            System.out.println("INFO in EProver.submitQuery(1): line: " + line);
            boolean inProof = false;
            while (line != null) {
                output.add(line);
                if (line.indexOf("# SZS status") != -1)
                    inProof = true;
                if (inProof) {
                    if (line.indexOf("# Enter job") != -1)
                        break;
                    result += line + "\n";
                }
                line = _reader.readLine();
                System.out.println("INFO in EProver.submitQuery: line: " + line);
                if (line != null && line.contains("Problem: "))
                    result += line + "\n";
            }
        }
        catch (Exception ex) {
            System.out.println("Error in EProver.submitQuery(): " + ex.getMessage());
            System.out.println("Error might be from EProver constructor, please check your EBatchConfig.txt and TPTP files ...");
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
        KBmanager.getMgr().setPref("eprover",System.getProperty("user.home") + "/Programs/E/Prover/eprover");
        System.out.print(eprover.submitQuery("(holds instance ?X Relation)",5,2));
        */
        try {
            System.out.println("INFO in EProver.main()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("------------- INFO in EProver.main() completed initialization--------");
            EProver eprover = new EProver(KBmanager.getMgr().getPref("eprover"),
                    KBmanager.getMgr().getPref("kbDir") + File.separator + KBmanager.getMgr().getPref("sumokbname") + ".tptp");
            System.out.println("------------- INFO in EProver.main() completed init of E --------");
            System.out.println("Result: " + eprover.submitQuery("(subclass ?X Object)",kb));
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

