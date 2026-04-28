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
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class EProver {

    private static boolean debug = true;
    /** Used to create a new OS process using a command list */
    private ProcessBuilder _builder;
    /** The eprover process being built by the ProcessBuilder _builder object */
    private Process _eprover;
    /** Used to effeciently read data from the _eprover process by reducing I/O operations*/
    private final BufferedReader _reader;
    /** Used to effeciently send inputs to the _eprover process */
    private final Writer _writer;
    /** The path where the eprover executable is found */
    private String executablePath;
    /** Directory of the knowledge base EProver will query against */
    private String kbFilePath;
    /**  */
    private String requestedTptpLanguage;
    /**  */
    private String tempProblemFilePath;
    /** Time in seconds that eprover will timeout */
    private int timeout;
    /** Max number of output answers EProver will give */
    private int maxAnswers;
    
    private List<String> commands;

    /** Storage for the output of the _eprover process */
    public List<String> output = new ArrayList<>();
    /** List of quantifiers found in the TPTP formulas from the KB */
    public StringBuilder quantifierList = null;
    /** Container for organizing the results of the query */
    private ATPResult result;

    /***************************************************************
     * Create a running instance of EProver based on existing batch
     * specification file with a max answer of 1.
     *
     * @param executable String path for the EProver executable.
     * @throws IOException
     */
    public EProver() {
        this(KBmanager.getMgr().getPref("eprover"));
    }

    /***************************************************************
     * Create a running instance of EProver based on existing batch
     * specification file with a max answer of 1.
     *
     * @param executable String path for the EProver executable.
     * @throws IOException
     */
    public EProver(String executable) {

        this(executable, 1);
    }

    /***************************************************************
     * Create a running instance of EProver based on existing batch
     * specification file.
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     * @param maxAnswers - Limit the answers up to maxAnswers only
     * @throws IOException
     */
    public EProver(String executable, int maxAnswers) {

        // kbFilePath = KBmanager.getMgr().getPref("kbDir");
        // String eproverPath = executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        // // String batchPath = kbdir + File.separator + "EBatchConfig.txt";
        // // List<String> commands = new ArrayList<>(Arrays.asList(
        // //         executable, batchPath, eproverPath, "-v --interactive"));
        // List<String> commands = Arrays.asList(
        //     executable,
        //     "--cpu-limit=" + 30,
        //     problemFile.toString()
        // );
        // _builder = new ProcessBuilder(commands);
        // _builder.redirectErrorStream(false);
        // _eprover = _builder.start();
        // if (!_eprover.isAlive()) {
        //     String stderr = new String(_eprover.getErrorStream().readAllBytes());
        //     throw new IOException("E exited immediately with code " + _eprover.exitValue() + ": " + stderr);
        // }
        // System.out.println("EProver(): new process: " + _eprover);
        
        _builder = null;
        _eprover = null;
        _reader = null;
        _writer = null;
    }

    /***************************************************************
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

        // kbdir = KBmanager.getMgr().getPref("kbDir");
        // addBatchConfig(kbFile, 60);
        // if (!(new File(kbFile)).exists()) {
        //     System.out.println("EProver(): no such file: " + kbFile + ". Creating it.");
        //     KBmanager.getMgr().getKB(kbFile);
        // }
        // String eproverPath = executable.substring(0, executable.lastIndexOf(File.separator)) + File.separator + "eprover";
        // String batchPath = kbdir + File.separator + "EBatchConfig.txt";
        // // List<String> commands = new ArrayList<>(Arrays.asList(
        // //         executable, batchPath,eproverPath, "-i"));
            
        _builder = null;
        _eprover = null;
        _reader = null;
        _writer = null;
    }

    /***************************************************************
     * Create a new EProver process with a new batch config file
     */
    public EProver(String executable, KB kb, String requestedTptpLanguage, int timeout, int maxAnswers) {
        
        this.executablePath = executable;
        this.kbFilePath = KBmanager.getMgr().getPref("kbDir") + File.separator + kb.name + ("tff".equals(requestedTptpLanguage) ? ".tptp" : ".tff");
        this.requestedTptpLanguage = requestedTptpLanguage;
        this.tempProblemFilePath = kbFilePath + File.separator + "temp-eprover-problem.p";
        this.timeout = timeout;
        this.maxAnswers = maxAnswers;
        this.commands = Arrays.asList(this.executablePath, "--cpu-limit=" + timeout);

        _builder = null;
        _eprover = null;
        _reader = null;
        _writer = null;
    }

    /***************************************************************
     * Submits a query to the E inference engine.
     *
     * @param kb            The knowledge base.
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param requestedTptpLang The tptp language (fof or tff) youd like to run the query in.
     * @param timeout       The number of seconds after which the inference engine should give up.
     * @param maxAnswers    The maximum number of answers (binding sets) the inference engine should return.
     * @return an instance of the EProver with results
     */
    public void askEProver(KB kb, String suoKifFormulas, String requestedTptpLang, int timeout, int maxAnswers) {

        if (StringUtil.isNonEmptyString(suoKifFormulas)) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(new Formula(suoKifFormulas), true, kb);
            if (!processedStmts.isEmpty() && this != null) {
                String strQuery = processedStmts.iterator().next().getFormula();
                this.submitQuery(strQuery, kb);
            }
        } else {
            System.out.println("EProver.askEProver(): suoKifFormulas empty!");
        }
    }

    /***************************************************************
     * Submits a
     * query to the inference engine. Returns a list of answers from inference
     * engine. If no proof is found, return null;vampire
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @return A list of answers from inference engine; If no proof or answer is
     * found, return null;
     */
    public List<String> askNoProof(KB kb, String suoKifFormula, int timeout, int maxAnswers) {

        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(query, true, kb);
            if (!processedStmts.isEmpty()) {
                // set timeout in EBatchConfig file and reload eprover
                EProver.addBatchConfig(null, timeout);
                String strQuery = processedStmts.iterator().next().getFormula();
                this.submitQuery(strQuery, kb);
                if (this.output == null || this.output.isEmpty())
                    System.out.println("No response from EProver!");
                else
                    System.out.println("Get response from EProver, start for parsing ...");
                // System.out.println("Results returned from E = \n" + EResult);
                TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
                return tpp.parseAnswerTuples(this.output, strQuery, kb, this.quantifierList);
            }
        }
        return null;
    }

    /***************************************************************
     * Get the full ATPResult for this run.
     * This provides detailed execution metadata, SZS status, and error info.
     * @return The ATPResult, or null if submitQuery hasn't been called
     */
    public ATPResult getResult() {
        return result;
    }

    /***************************************************************
     * Get the SZS status from the last run.
     * @return The SZSStatus, or SZSStatus.NOT_RUN if not run
     */
    public SZSStatus getSzsStatus() {
        return result != null ? result.getSzsStatus() : SZSStatus.NOT_RUN;
    }


    /***************************************************************
     * Check if there was an error during execution.
     *
     * @return true if the result indicates an error
     */
    public boolean hasError() {
        return result != null && result.hasErrors();
    }

    /***************************************************************
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
     * @param timeout time limit in E to be added to EBatchConfig.txt
     *  */
    public static void addBatchConfig(String inputFilename, int timeout) {

        String kbdir = KBmanager.getMgr().getPref("kbDir");
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

    /***************************************************************
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

    /***************************************************************
     * Add an assertion for inference.
     *
     * @param userAssertionTPTP the tptp file path to save the user assertion to.
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
                        int axiomIndex = 0;
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

    /***************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax
     * @param kb current knowledge base
     * @return answer to the query
     */
    public String submitQuery(String formula, KB kb) {

        long startTime = System.currentTimeMillis();
        result = new ATPResult.Builder()
            .engineName("EProver")
            .engineMode("interactive")
            .inputLanguage("FOF")
            .inputSource("custom")
            .build();
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();
        String resultStr = "";
        Path tempProblemFile = null;

        try {
            this.quantifierList = SUMOformulaToTPTPformula.getQlist();
            tempProblemFile = Files.createTempFile(this.tempProblemFilePath, ".p");
            String problem = "include('" + kbFilePath + "').\n" + "fof(conj1,conjecture, " + SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,true) + ")." + "\n";
            Files.writeString(tempProblemFile, problem);

            this.commands.add(tempProblemFile.toString());

            System.out.println("EProver.submitQuery(): commands\n" + this.commands);

            ProcessBuilder processBuilder = new ProcessBuilder(this.commands);
            processBuilder.redirectErrorStream(false);
            Process proc = processBuilder.start();

            Thread stderrReader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(proc.getErrorStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        stderrLines.add(line);
                    }
                } catch (IOException ignored) {}
            });
            stderrReader.start();
            try (BufferedReader reader = new BufferedReader( new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdoutLines.add(line);
                    output.add(line);
                    resultStr += line + "\n";
                }
            }
            stderrReader.join(5000);
            int exitCode = proc.waitFor();
            long elapsed = System.currentTimeMillis() - startTime;
            result.setStdout(stdoutLines);
            result.setStderr(stderrLines);
            result.setExitCode(exitCode);
            result.setElapsedTimeMs(elapsed);
            result.extractSzsFromOutput();
        } catch (Exception ex) {
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(ex.getMessage());
        } finally {
            if (tempProblemFile != null) {
                try { Files.deleteIfExists(tempProblemFile); } catch (IOException ignored) {}
            }
        }
        return resultStr;
    }

    /***************************************************************
     * Not used anywhere?
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

    /***************************************************************
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
    
    
    /***************************************************************
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
        if (_eprover == null || !_eprover.isAlive()) {
            System.out.println("EProver.terminate(): process is already dead!");
            return;
        }
        try (_reader; _writer) {
            _writer.write("quit\n");
            _writer.write("go.\n");
            _writer.flush();
            System.out.println("DESTROYING the Process " + _eprover);
            System.out.println();
            _eprover.destroy();
            output.clear();
            quantifierList.setLength(0);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /***************************************************************
     * Returns this instances output list as a String
     * 
     * @return String of the output, typically after a query.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (String s : output) sb.append(s).append("\n");
        return sb.toString();
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("EProver class:");
        System.out.println("  h - show this help screen");
        System.out.println("  --ask <SuoKif-Query> <fof or tff> <timeoutInSec> <maxAnswers> - ask query");
    }

    /***************************************************************
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
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h"))
            showHelp();
        else {
            System.out.println("INFO in EProver.main()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            if (argMap.containsKey("ask") && argMap.get("ask").size() == 4) {
                String suoKifFormula = argMap.get("ask").get(0);
                String requestedTptpLang = argMap.get("ask").get(1);
                int timeout = Integer.parseInt(argMap.get("ask").get(2));
                int maxAnswers = Integer.parseInt(argMap.get("ask").get(3));
                try {
                    EProver eprover = new EProver();
                    eprover.askEProver(kb, suoKifFormula, requestedTptpLang, timeout, maxAnswers);
                    System.out.println("EProver.main(): completed Eprover query with result: " + StringUtil.arrayListToCRLFString(eprover.output));
                    TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
                    tpp.parseProofOutput(eprover.output, argMap.get("ask").get(0), kb, eprover.quantifierList);
                    eprover.terminate();
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            } else showHelp();
        }
    }
}
