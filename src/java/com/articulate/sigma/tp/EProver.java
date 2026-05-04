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
package com.articulate.sigma.tp;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EProver {

    /** Turn debugging on and off */
    private static int debug = 1;
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
    /** Temporary problem file generated for inference */
    private String tempProblemFilePath;
    /** The knowledge base to be used for inference */
    private KB kb;
    /** Command list to be run by the process */
    private List<String> commands;
    /** The TPTP Language [fof|tff] */
    private String requestedTptpLanguage;
    /** Time in seconds that eprover will timeout */
    private int timeout;
    /** Max number of output answers EProver will give */
    private int maxAnswers;
    /** Storage for the output of the _eprover process */
    public List<String> output = new ArrayList<>();
    /** List of quantifiers found in the TPTP formulas from the KB */
    public StringBuilder qlist = null;
    /** Container for organizing the results of the query */
    private ATPResult result;

    /***************************************************************
     * Constructor for EProver. Defaults to tptp, 30 sec timeout, 1 max answer.
     * @param kb
     */
    public EProver(KB kb) {

        this(kb, "tptp", 30, 1);
    }

    /***************************************************************
     * Constructor for EProver. Executable defaults to eprover pref.
     * @param kb
     * @param requestedTptpLanguage
     * @param timeout
     * @param maxAnswers
     */
    public EProver(KB kb, String requestedTptpLanguage, int timeout, int maxAnswers) {
        
        if (debug>0) System.out.printf("\nEProver(%s, %s, %d, %d)", kb.name, requestedTptpLanguage, timeout, maxAnswers);
        this.kb = kb;
        this.requestedTptpLanguage = requestedTptpLanguage;
        this.timeout = timeout;
        this.maxAnswers = maxAnswers;
        this.executablePath = KBmanager.getMgr().getPref("eprover");
        this.kbFilePath = KBmanager.getMgr().getPref("kbDir") + File.separator + kb.name + ("tff".equals(requestedTptpLanguage) ? ".tff" : ".tptp");
        this.tempProblemFilePath = KBmanager.getMgr().getPref("kbDir") + File.separator + "temp-eprover-problem.p";
        this.commands = new ArrayList<>();
        this.commands.add(this.executablePath);
        this.commands.add("--cpu-limit=" + String.valueOf(timeout));
        this.commands.add("--conjectures-are-questions");
        this.commands.add("--answers=" + maxAnswers);
        _builder = null;
        _eprover = null;
        _reader = null;
        _writer = null;
    }

    /***************************************************************
     * Submits a query to this E inference engine.
     * @param suoKifFormula The String representation of the SUO-KIF query.
     */
    public void askEProver(String suoKifFormulas) {

        if (debug>0) System.out.printf("\nEProver.askEProver(%s)", suoKifFormulas);
        if (StringUtil.isNonEmptyString(suoKifFormulas)) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(new Formula(suoKifFormulas), true, this.kb);
            if (!processedStmts.isEmpty() && this != null) {
                String strQuery = processedStmts.iterator().next().getFormula();
                this.submitQuery(strQuery);
                System.out.println("ThiNGHERE");
            }
        } else {
            System.out.println("EProver.askEProver(): suoKifFormulas empty!");
        }
    }
    
    public static boolean isAvailable() {return Files.isRegularFile(Paths.get(KBmanager.getMgr().getPref("eprover")));}

    /***************************************************************
     * Submits a query to this EProver. Returns a list of answers from inference
     * engine. If no proof is found, return null.
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @return List of answers; If no proof or answer is found, return null;
     */
    public List<String> askNoProof(String suoKifFormula) {

        if (debug>0) System.out.printf("\nEProver.askNoProof(%s)", suoKifFormula);
        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(query, true, this.kb);
            if (!processedStmts.isEmpty()) {
                EProver.addBatchConfig(null, this.timeout);
                String strQuery = processedStmts.iterator().next().getFormula();
                this.submitQuery(strQuery);
                if (this.output == null || this.output.isEmpty())
                    System.out.println("No response from EProver!");
                else
                    System.out.println("Get response from EProver, start for parsing ...");
                TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
                return tpp.parseAnswerTuples(this.output, strQuery, this.kb, this.qlist);
            }
        }
        return null;
    }

    /***************************************************************
     * Add an assertion for inference.
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion
     */
    public String assertFormula(String formula) {

        if (debug>0) System.out.printf("\nEProver.assertFormula(%s)", formula);
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
            System.err.println("Error in EProver.assertFormula(" + formula + "): " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /***************************************************************
     * Add an assertion for inference.
     * @param userAssertionTPTP the tptp file path to save the user assertion to.
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp if true convert the processed formulas to tptp.
     * @return true if all assertions are added for inference
     * TODO: This function might not be necessary if we find a way to
     * directly add assertion into opened inference engine (e_ltb_runner)
     */
    public boolean assertFormula(String userAssertionTPTP, List<Formula> parsedFormulas, boolean tptp) {

        if (debug>0) System.out.printf("\nEProver.assertFormula(%s, %s, %b)", userAssertionTPTP, parsedFormulas, tptp);
        System.out.println("EProver.assertFormula(2): process: " + _eprover);
        boolean allAdded = (this != null);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)))) {
            Set<Formula> processedFormulas = new HashSet<>();
            FormulaPreprocessor fp;
            Set<String> tptpFormulas;
            String tptpstring;
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                fp = new FormulaPreprocessor();
                processedFormulas.addAll(fp.preProcess(parsedF,false, this.kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP.
                    tptpFormulas = new HashSet<>();
                    if (tptp) {
                        for (Formula p : processedFormulas)
                            if (!p.isHigherOrder(this.kb))
                                tptpFormulas.add(SUMOformulaToTPTPformula.tptpParseSUOKIFString(p.getFormula(),false));
                    }
                    if (this != null) { // 3. Write to new tptp file
                        int axiomIndex = 0;
                        for (String theTPTPFormula : tptpFormulas) {
                            pw.print("fof(kb_" + this.kb.name + "_UserAssertion" + "_" + axiomIndex++);
                            pw.println(",axiom,(" + theTPTPFormula + ")).");
                            tptpstring = "fof(kb_" + this.kb.name + "_UserAssertion" + "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
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
     * Get the ATPResult for this run, provides execution metadata, SZS, and error info.
     * @return The ATPResult, or null if submitQuery hasn't been called
     */
    public ATPResult getResult() {return result;}

    /***************************************************************
     * Get the SZS status from the last run.
     * @return The SZSStatus, or SZSStatus.NOT_RUN if not run
     */
    public SZSStatus getSzsStatus() {return result != null ? result.getSzsStatus() : SZSStatus.NOT_RUN;}

    /***************************************************************
     * Check if there was an error during execution.
     * @return true if the result indicates an error
     */
    public boolean hasError() {return result != null && result.hasErrors();}

    /***************************************************************
     * Create or update batch specification file.
     * e_ltb_runner (not currently workling) processes a batch specification file; 
     * it contains a specification of the background theory, options, and a
     * number of individual job requests. It is used with the option --interactive.
     * "inputFilename" is added into an existing batch specification file for inference.
     * @param inputFilename contains TPTP assertions
     * @param timeout time limit in E to be added to EBatchConfig.txt
     */
    public static void addBatchConfig(String inputFilename, int timeout) {

        if (debug>0) System.out.printf("\nEProver.addBatchConfig(%s, %d)", inputFilename, timeout);
        String kbdir = KBmanager.getMgr().getPref("kbDir");
        File initFile = new File(kbdir, "EBatchConfig.txt");
        Set<String> ebatchfiles = new HashSet<>();
        if (inputFilename != null && !inputFilename.isEmpty()) ebatchfiles.add(inputFilename);
        if (initFile.exists()) { // Collect existing TPTP files
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
                System.err.println("Error in EProver.addBatchConfig()" + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
        try (PrintWriter pw = new PrintWriter(initFile)) { // write existing TPTP files and new tptp files (inputFilename) into EBatchConfig.txt
            pw.println("% SZS start BatchConfiguration");
            pw.println("division.category LTB.SMO");
            pw.println("execution.order ordered");
            pw.println("output.required Assurance");
            pw.println("output.desired Proof Answer");
            pw.println("limit.time.problem.wc " + timeout);
            pw.println("% SZS end BatchConfiguration");
            pw.println("% SZS start BatchIncludes");
            for (String ebatchfile : ebatchfiles) pw.println("include('" + ebatchfile + "').");
            pw.println("% SZS end BatchIncludes");
            pw.println("% SZS start BatchProblems");
            pw.println("% SZS end BatchProblems");
        }
        catch (FileNotFoundException e) {
            System.err.println("Error in EProver.addBatchConfig()" + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /***************************************************************
     * Submit a query.
     * @param formula query in the KIF syntax
     * @return answer to the query
     */
    public void submitQuery(String formula) {

        if (debug>0) System.out.printf("\nEProver.submitQuery(%s)", formula);
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
        System.out.println("here0");
        try {
            tempProblemFile = Files.createTempFile(Paths.get(KBmanager.getMgr().getPref("kbDir")), "temp-eprover-problem", ".p");
            String problem = "include('" + kbFilePath + "').\n" + "fof(conj1,conjecture, " + SUMOformulaToTPTPformula.tptpParseSUOKIFString(formula,true) + ")." + "\n";
            this.qlist = SUMOformulaToTPTPformula.getQlist();
            System.out.println("\n\n\n\nEProver qlist = " + this.qlist);
            Files.writeString(tempProblemFile, problem);
            this.commands.add(tempProblemFile.toString());
            System.out.println("EProver.submitQuery(): commands\n" + String.join(" ", this.commands));
            ProcessBuilder processBuilder = new ProcessBuilder(this.commands);
            processBuilder.redirectErrorStream(false);
            Process proc = processBuilder.start();
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
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
                    this.output.add(line);
                    resultStr += line + "\n";
                }
            }
            stderrReader.join(5000);
            int exitCode = proc.waitFor();System.out.println("EProver exitCode = " + exitCode);

            // System.out.println("EProver stdout:");
            // for (String s : stdoutLines)
            //     System.out.println(s);
            System.out.println("EProver stderr:");
            for (String s : stderrLines)
                System.out.println(s);

            long elapsed = System.currentTimeMillis() - startTime;
            result.setStdout(stdoutLines);
            result.setStderr(stderrLines);
            result.setQList(this.qlist);
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
    }

    // /***************************************************************
    //  * Not used anywhere?
    //  * @param executable eprover executable file
    //  * @param timeout seconds before eprover times out
    //  * @param kbFile the knowledge base file for inference
    //  * @return the command list
    //  */
    // private static String[] createCommandList(File executable, int timeout, File kbFile) {

    //     String[] cmds = new String[3];
    //     cmds[0] = executable.toString();
    //     cmds[1] = Integer.toString(timeout);
    //     cmds[2] = kbFile.toString();
    //     return cmds;
    // }

    /***************************************************************
     * Create a custom commmand list for EProver.
     * Don't include a timeout if @param timeout is 0
     * @param executable eprover executable file
     * @param timeout seconds before eprover times out
     * @param kbFile the knowledge base file for inference
     * @param commands initial command list
     * @return the command list
     */
    private static String[] createCustomCommandList(File executable, int timeout, File kbFile, Collection<String> commands) {

        if (debug>0) System.out.printf("\nEProver.createCustomCommandList(%s, %d, %s, %s)", executable.getName(), timeout, kbFile.getName(), commands);
        StringBuilder opts = new StringBuilder();
        for (String s : commands) opts.append(s).append(Formula.SPACE);
        if (timeout != 0) {
            opts.append("-t").append(Formula.SPACE);
            opts.append(timeout).append(Formula.SPACE);
        }
        opts.append(kbFile.toString());
        String[] optar = opts.toString().split(Formula.SPACE);
        String[] cmds = new String[optar.length + 1];
        cmds[0] = executable.toString();
        System.arraycopy(optar, 0, cmds, 1, optar.length);
        return cmds;
    }

    /***************************************************************
     * Creates a running instance of Eprover with custom command line options.
     * @param kbFile Initial knowledge base loaded by the Eprover executable.
     * @param timeout Seconds before EProver times out.
     * @param commands Command list to be run by the eprover process
     * @throws ExecutableNotFoundException if the EProver executable is not found
     * @throws ProverCrashedException if EProver crashes with a non-zero exit code
     * @throws ProverTimeoutException if EProver times out
     * @throws Exception for other errors
     */
    public void runCustom(File kbFile, int timeout, Collection<String> commands) throws Exception {

        if (debug>0) System.out.printf("\nEProver.runCustom(%s, %d, %s)", kbFile.getName(), timeout, commands);
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;
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
        result.setStdout(stdoutLines);
        result.setStderr(stderrLines);
        result.finalize(exitValue, elapsed, elapsed >= timeoutMs);
        if (exitValue != 0) {
            System.err.println("Error in Eprover.runCustom(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) System.err.println("Stderr: " + stderrLines);
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
     * @throws IOException should not normally be thrown
     */
    public void terminate() throws IOException {

        if (debug>0) System.out.printf("\nEProver.terminate()");
        if (this._eprover == null) return;
        if (_eprover == null || !_eprover.isAlive()) return;
        try (_reader; _writer) {
            _writer.write("quit\n");
            _writer.write("go.\n");
            _writer.flush();
            _eprover.destroy();
            output.clear();
            qlist.setLength(0);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /***************************************************************
     * Returns this instances output list as a String
     * @return String of the output, typically after a query.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (String s : output) sb.append(s).append("\n");
        return sb.toString();
    }

    /*****************************************************************
     */
    public static void showHelp() {

        System.out.println("EProver class:");
        System.out.println("  h - show this help screen");
        System.out.println("  --ask <SuoKif-Query> <fof or tff> <timeoutInSec> <maxAnswers> - ask query");
    }

    /***************************************************************
     * 
     */
    public static void main(String[] args) throws Exception {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) showHelp();
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
                    EProver eprover = new EProver(kb, requestedTptpLang, timeout, maxAnswers);
                    eprover.askEProver(suoKifFormula);
                    System.out.println("EProver.main(): completed Eprover query with result: " + StringUtil.arrayListToCRLFString(eprover.output));
                    TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
                    tpp.parseProofOutput(eprover.output, argMap.get("ask").get(0), kb, eprover.qlist);
                    eprover.terminate();
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            } else showHelp();
        }
    }
}
