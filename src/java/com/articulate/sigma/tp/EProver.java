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
    private final BufferedReader _reader;
    private final Writer _writer;
    private static String kbdir;
    private static int axiomIndex = 0;
    public List<String> output = new ArrayList<>();
    public StringBuilder qlist = null;

    // === NEW: Full result structure for error handling ===
    private ATPResult result;

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (String s : output)
            sb.append(s).append("\n");
        return sb.toString();
    }

    /** *************************************************************
     * Get the full ATPResult for this run.
     * This provides detailed execution metadata, SZS status, and error info.
     *
     * @return The ATPResult, or null if submitQuery hasn't been called
     */
    public ATPResult getResult() {
        return result;
    }

    /** *************************************************************
     * Check if there was an error during execution.
     *
     * @return true if the result indicates an error
     */
    public boolean hasError() {
        return result != null && result.hasErrors();
    }

    /** *************************************************************
     * Get the SZS status from the last run.
     *
     * @return The SZSStatus, or SZSStatus.NOT_RUN if not run
     */
    public SZSStatus getSzsStatus() {
        return result != null ? result.getSzsStatus() : SZSStatus.NOT_RUN;
    }

    /** *************************************************************
     * Create or update batch specification file.
     *
     * e_ltb_runner processes a batch specification file; it contains
     * a specification of the background theory, some options, and a
     * number of individual job requests. It is used with the option
     * --interactive.
     * "inputFilename" is added into an existing batch specification file
     * for inference.
     *
     * @param inputFilename contains TPTP assertions
     * @param timeout time limit in E
     *  */
    public static void addBatchConfig(String inputFilename, int timeout) {

        File initFile = new File(kbdir, "EBatchConfig.txt");
        Set<String> ebatchfiles = new HashSet<>();
        if (inputFilename != null && !inputFilename.isEmpty())
            ebatchfiles.add(inputFilename);

        // Collect existing TPTP files
        if (initFile.exists()) {
            try (InputStream fis = new FileInputStream(initFile);
                Reader isr = new InputStreamReader(fis);
                BufferedReader in = new BufferedReader(isr)) {
                String line = in.readLine(), split, ebatchfile;
                int isEbatchFile;
                while (line != null) {
                    split = "include('";
                    isEbatchFile = line.indexOf(split);
                    if (isEbatchFile != -1) {
                        ebatchfile = line.substring(split.length(), line.lastIndexOf("')"));
                        ebatchfiles.add(ebatchfile);
                    }
                    line = in.readLine();
                }
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
                System.err.println("Error in EProver.addBatchConfig()");
                System.err.println(e.getMessage());
            }
        }

        // write existing TPTP files and new tptp files (inputFilename) into EBatchConfig.txt
        try (PrintWriter pw = new PrintWriter(initFile)) {
            pw.println("% SZS start BatchConfiguration");
            pw.println("division.category LTB.SMO");
            pw.println("execution.order ordered");
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
        }
        catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
            System.err.println("Error in EProver.addBatchConfig()");
            System.err.println(e.getMessage());
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
     * e_ltb_runner --interactive LTBSampleInput-AP.txt
     */
    public EProver(String executable, String kbFile) throws IOException {

        kbdir = KBmanager.getMgr().getPref("kbDir");
        addBatchConfig(kbFile, 60);
        System.out.println("INFO in EProver(): executable: " + executable);
        System.out.println("INFO in EProver(): kbFile: " + kbFile);
        if (!(new File(kbFile)).exists()) {
            System.out.println("EProver(): no such file: " + kbFile + ". Creating it.");
            KBmanager.getMgr().getKB(kbFile);
        }
        // To make sigma work on windows.
        // If OS is not detected as Windows it will use the same directory as set in "inferenceEngine".
        String eproverPath = null;
        String _OS = System.getProperty("os.name");
        if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*")) {
            eproverPath=KBmanager.getMgr().getPref("eproverPath");
        }
        eproverPath = eproverPath != null && eproverPath.length() != 0 ? eproverPath
                        : executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        String batchPath = kbdir + File.separator + "EBatchConfig.txt";
        List<String> commands = new ArrayList<>(Arrays.asList(
                executable, batchPath,eproverPath, "-i"));
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
     * specification file with a max answer of 1.
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     * @throws IOException
     */
    public EProver(String executable) throws IOException {

        this(executable, 1);
    }

    /** *************************************************************
     * Create a running instance of EProver based on existing batch
     * specification file.
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     * @param maxAnswers - Limit the answers up to maxAnswers only
     * @throws IOException
     */
    public EProver(String executable, int maxAnswers) throws IOException {

        kbdir = KBmanager.getMgr().getPref("kbDir");
        // To make sigma work on windows
        // If OS is not detected as Windows it will use the same directory as set in "inferenceEngine".
        String eproverPath = null;
        String _OS = System.getProperty("os.name");
        if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*")){
            eproverPath = KBmanager.getMgr().getPref("eproverPath");
        }
        eproverPath = eproverPath != null && eproverPath.length() != 0 ? eproverPath
                            : executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        String batchPath = kbdir + File.separator + "EBatchConfig.txt";
        List<String> commands = new ArrayList<>(Arrays.asList(
                executable, batchPath, eproverPath, "--answers=" + maxAnswers, "--interactive"));
        System.out.println("EProver(): commands: " + commands);
        _builder = new ProcessBuilder(commands);
        _builder.redirectErrorStream(false);
        _eprover = _builder.start();
        System.out.println("EProver(): new process: " + _eprover);
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }

    /** *************************************************************
     * Add an assertion for inference.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion
     */
    public String assertFormula(String formula) {

        System.out.println("EProver.assertFormula(1): process: " + _eprover);
        String result = "";
        output = new ArrayList<>();
        try {
            String assertion = "";
            _writer.write(assertion);
            System.out.println("\nINFO in EProver.assertFormula(1) write: " + assertion + "\n");
            _writer.flush();
            String line;
            do {
                line = _reader.readLine();
                System.out.println("\nINFO in EProver.assertFormula(1) read: " + line);
                if (line.contains("Error:"))
                    throw new IOException(line);
                System.out.println("INFO EProver(): Response: " + line);
                result += line + "\n";
                output.add(line);
                if (line.contains("# Processing finished"))
                    break;
            } while (line != null);
        }
        catch (IOException ex) {
            System.err.println("Error in EProver.assertFormula(" + formula + ")");
            System.err.println(ex.getMessage());
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
                                 List<Formula> parsedFormulas, boolean tptp) {

        System.out.println("EProver.assertFormula(2): process: " + _eprover);
        boolean allAdded = (eprover != null);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)))) {
            Set<Formula> processedFormulas = new HashSet<>();
            FormulaPreprocessor fp;
            Set<String> tptpFormulas;
            String tptpstring;
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                fp = new FormulaPreprocessor();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP.
                    tptpFormulas = new HashSet<>();
                    if (tptp) {
                        for (Formula p : processedFormulas)
                            if (!p.isHigherOrder(kb))
                                tptpFormulas.add(SUMOformulaToTPTPformula.tptpParseSUOKIFString(p.getFormula(),false));
                    }
                    // 3. Write to new tptp file
                    if (eprover != null) {
                        for (String theTPTPFormula : tptpFormulas) {
                            pw.print("fof(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                            pw.println(",axiom,(" + theTPTPFormula + ")).");
                            tptpstring = "fof(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
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
        try (_reader; _writer) {
            _writer.write("quit\n");
            _writer.write("go.\n");
            _writer.flush();
            System.out.println("DESTROYING the Process " + _eprover);
            System.out.println();
            _eprover.destroy();
            output.clear();
            qlist.setLength(0);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax
     * @param kb current knowledge base
     * @return answer to the query
     */
    public String submitQuery(String formula, KB kb) {

        long startTime = System.currentTimeMillis();

        // Initialize result structure
        result = new ATPResult.Builder()
                .engineName("EProver")
                .engineMode("interactive")
                .inputLanguage("FOF")
                .inputSource("custom")
                .build();

        System.out.println("EProver.submitQuery(): process: " + _eprover);
        String resultStr = "";
        List<String> stdoutLines = new ArrayList<>();

        try {
            String query = SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,true);
            this.qlist = SUMOformulaToTPTPformula.getQlist();
            String conjecture = "fof(conj1,conjecture, " + query + ").";
            System.out.println("\nINFO in EProver.submitQuery() write: " + conjecture + "\n");
            System.out.println("\nINFO in EProver.submitQuery() write: go.");
            _writer.write("job sigma_1.\n");
            _writer.write(conjecture + "\n");
            _writer.write("go.\n");
            _writer.flush();
            output = new ArrayList<>();
            String line = _reader.readLine();
            System.out.println("INFO in EProver.submitQuery(1): line: " + line);
            boolean inProof = false;
            while (line != null) {
                output.add(line);
                stdoutLines.add(line);
                if (line.contains("# SZS status"))
                    inProof = true;
                if (inProof) {
                    if (line.contains("# Enter job"))
                        break;
                    resultStr += line + "\n";
                }
                line = _reader.readLine();
                System.out.println("INFO in EProver.submitQuery: line: " + line);
                if (line != null && line.contains("Problem: "))
                    resultStr += line + "\n";
            }

            long elapsed = System.currentTimeMillis() - startTime;

            // Populate result
            result.setStdout(stdoutLines);
            result.setElapsedTimeMs(elapsed);
            result.setExitCode(0);  // Interactive mode - if we got here, it succeeded
            result.extractSzsFromOutput();

        }
        catch (IOException ex) {
            System.err.println("Error in EProver.submitQuery(): " + ex.getMessage());
            System.err.println("Error might be from EProver constructor, please check your EBatchConfig.txt and TPTP files ...");
            ex.printStackTrace();

            // Populate error in result
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(ex.getMessage());
            result.setStdout(stdoutLines);
        }
        return resultStr;
    }

    /** *************************************************************
     */
    private static String[] createCommandList(File executable, int timeout, File kbFile) {

        String[] cmds = new String[3];
        cmds[0] = executable.toString();
        cmds[1] = Integer.toString(timeout);
        cmds[2] = kbFile.toString();
        return cmds;
    }

    /** *************************************************************
     * don't include a timeout if @param timeout is 0
     */
    private static String[] createCustomCommandList(File executable,
                                                    int timeout, File kbFile,
                                                    Collection<String> commands) {

        String space = Formula.SPACE;
        StringBuilder opts = new StringBuilder();
        for (String s : commands)
            opts.append(s).append(space);
        if (timeout != 0) {
            opts.append("-t").append(space);
            opts.append(timeout).append(space);
        }
        opts.append(kbFile.toString());
        String[] optar = opts.toString().split(Formula.SPACE);
        String[] cmds = new String[optar.length + 1];
        cmds[0] = executable.toString();
        System.arraycopy(optar, 0, cmds, 1, optar.length);
        return cmds;
    }

    /** *************************************************************
     * Creates a running instance of Eprover with custom command line
     * options.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Eprover executable.
     *
     * @throws ExecutableNotFoundException if the EProver executable is not found
     * @throws ProverCrashedException if EProver crashes with a non-zero exit code
     * @throws ProverTimeoutException if EProver times out
     * @throws Exception for other errors
     */
    public void runCustom(File kbFile, int timeout, Collection<String> commands) throws Exception {

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;

        // Initialize result structure
        result = new ATPResult.Builder()
                .engineName("EProver")
                .engineMode("custom")
                .inputLanguage("FOF")
                .inputSource(kbFile != null ? kbFile.getName() : "unknown")
                .timeoutMs(timeoutMs)
                .build();

        String eprover = KBmanager.getMgr().getPref("eprover");
        if (StringUtil.emptyString(eprover)) {
            String msg = "Error in Eprover.runCustom(): no executable string in preferences";
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("EProver", eprover, "eprover");
        }
        File executable = new File(eprover);
        if (!executable.exists()) {
            String msg = "Error in Eprover.runCustom(): no executable " + eprover;
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("EProver", eprover, "eprover");
        }

        String[] cmds = createCustomCommandList(executable, timeout, kbFile, commands);
        System.out.println("EProver.runCustom(): Custom command list:\n" + Arrays.toString(cmds));
        ArrayList<String> moreCommands = new ArrayList<>();
        moreCommands.addAll(Arrays.asList(cmds));
        moreCommands.addAll(commands);
        cmds = moreCommands.toArray(cmds);
        result.setCommandLine(cmds);
        System.out.println("Eprover.runCustom(): Initializing Eprover with:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(false);  // Keep stderr separate

        Process _eprover = _builder.start();

        // Read stdout and stderr in parallel
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(_eprover.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderrLines.add(line);
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        stderrReader.start();

        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                stdoutLines.add(line);
                output.add(line);  // Maintain backward compatibility
            }
        }

        stderrReader.join(5000);

        int exitValue = _eprover.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;

        // Populate result
        result.setStdout(stdoutLines);
        result.setStderr(stderrLines);
        result.finalize(exitValue, elapsed, elapsed >= timeoutMs);

        if (exitValue != 0) {
            System.err.println("Error in Eprover.runCustom(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) {
                System.err.println("Stderr: " + stderrLines);
            }
            System.err.println(output);

            // Throw appropriate exception
            if (result.isTimedOut() || result.getSzsStatus() == SZSStatus.TIMEOUT) {
                throw new ProverTimeoutException("EProver", timeoutMs, elapsed, true, stdoutLines, stderrLines, result);
            } else if (exitValue > 128 && exitValue < 160) {
                throw new ProverCrashedException("EProver", exitValue, stdoutLines, stderrLines, result);
            }
        }
        System.out.println("Eprover.runCustom() done executing");
    }

    /** *************************************************************
     * A simple test. Works as follows:
     * <ol>
     *   <li>start E;</li>
     *   <li>make an assertion;</li>
     *   <li>submit a query;</li>
     *   <li>terminate E</li>
     * </ol>
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
            System.out.println("Result:\n " + eprover.submitQuery("(subclass ?X Object)", kb));
            eprover.terminate();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }

        // System.out.print(eprover.assertFormula("(human Socrates)"));
        // System.out.print(eprover.assertFormula("(holds instance Adam Human)"));
        // System.out.print(eprover.submitQuery("(human ?X)", 1, 2));
        // System.out.print(eprover.submitQuery("(holds instance ?X Human)", 5, 2));
    }
}
