/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

package com.articulate.sigma.tp;

import com.articulate.sigma.user.EmailService;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.e.*;
import com.articulate.sigma.utils.LoggingUtils;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.parsing.CLIMapParser;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.Formula;
import com.articulate.sigma.KIF;
import com.articulate.sigma.utils.StringUtil;
import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TheoremProverController {

    public record FilteredVampireAttempt(Path problemFile, ATPResult result) {
        
        public boolean foundContradiction() {

            if (result == null) return false;
            return result.getSzsStatus() == SZSStatus.THEOREM || result.getSzsStatus() == SZSStatus.UNSATISFIABLE || result.getSzsStatus() == SZSStatus.CONTRADICTORY_AXIOMS;
        }
    }

    public record EAxFilterVampireResult(ECNF.CNFResult cnfResult, EAxFilter.EAxFilterResult filterResult, List<FilteredVampireAttempt> attempts, FilteredVampireAttempt successfulAttempt) {
        public EAxFilterVampireResult { attempts = List.copyOf(attempts); }
        public boolean foundContradiction() { return successfulAttempt != null; }
    }

    public TheoremProverController () {}
    
    /********************************************************************
     * Runs the primary ask API without requiring an ATPQuery object.
     * @param kb knowledge base to query.
     * @param userSessionId user session identifier.
     * @param query query to send to the prover.
     * @param testFilePath path to the test file, if applicable.
     * @param runSource source of the run.
     * @param proverType prover to use.
     * @param language target logical language.
     * @param vampireMode Vampire execution mode.
     * @param closedWorldAssumption whether to use the closed world assumption.
     * @param modusPonens whether to enable modus ponens.
     * @param dropOnePremise whether to drop one premise during inference.
     * @param holUseModals whether HOL modal translation is enabled.
     * @param timeout prover timeout in seconds.
     * @param maxAnswers maximum number of answers to return.
     * @return result object for the query.
     */
    public ATPResult runQuery(KB kb, String userSessionId, String query, String testFilePath, String runSource, String proverType, String language, String vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) {

        ATPQuery atpQuery = new ATPQuery(
            kb, 
            userSessionId,
            query,
            testFilePath,
            runSource,
            proverType,
            language,
            vampireMode,
            closedWorldAssumption,
            modusPonens,
            dropOnePremise,
            holUseModals,
            timeout,
            maxAnswers
        );
        return ask(atpQuery);
    }

    /********************************************************************
     * Primary API for the class. Capable of asking all 3 provers.
     * @param query ATPQuery object used to determine which prover to ask with associated options.
     * @return ATPQuery result object of the query.
     */
    public ATPResult ask (ATPQuery query) {
        LoggingUtils.log("Querying " + query.getProverType());
        TPTPGenerationManager.waitForAllTPTP(600);
        switch (query.getProverType()) {
            case EPROVER:
                return this.askEProver(query);
            case VAMPIRE: 
                return this.askVampire(query);
            case LEO:
                return this.askLeo(query);
            default:
                System.err.println("TheoremProverController.ask(): INVALID PROVER");
                break;
        }
        return null;
    }

    /**
     * Run a complete TPTP-family problem file as-is. No SUMO translation,
     * generated KB, or InferenceTest processing is involved.
     */
    public ATPResult runProblemFile(KB kb, Path problemFile, String proverType,
                                    String language, String vampireMode,
                                    int timeout, int maxAnswers,
                                    String sessionId) throws Exception {

        if (problemFile == null || !Files.isRegularFile(problemFile))
            throw new IOException("Problem file does not exist: " + problemFile);

        String prover = proverType == null ? "VAMPIRE" :
                proverType.trim().toUpperCase();
        String lang = language == null ? "FOF" :
                language.trim().toUpperCase();
        File file = problemFile.toFile();

        switch (prover) {
            case "VAMPIRE": {
                Vampire vampire = new Vampire(lang, vampireMode, timeout, maxAnswers);
                vampire.runCustom(file);
                return vampire.getResult();
            }
            case "EPROVER": {
                if ("THF".equals(lang))
                    return ATPResult.notRun("EProver",
                            "EProver does not support THF problems.");
                EProver eprover = new EProver(kb,
                        "TFF".equals(lang) ? "tff" : "fof",
                        timeout, maxAnswers, null);
                eprover.runProblemFile(file);
                return eprover.getResult();
            }
            case "LEO": {
                if (!"THF".equals(lang))
                    return ATPResult.notRun("LEO-III",
                            "LEO-III direct editor runs require a THF file.");
                LEO leo = new LEO(kb, "thf", timeout, maxAnswers, null);
                leo.runProblemFile(file);
                return leo.getResult();
            }
            default:
                throw new IllegalArgumentException(
                        "Unsupported theorem prover: " + proverType);
        }
    }

    /********************************************************************
     * Returns a list of available provers based on whether their executable path is valid.
     * @return List of available provers
     */
    public static List<String> availableProvers() {
        
        List<String> availableProvers = new ArrayList<>();
        if (EProver.isAvailable()) availableProvers.add("eprover");
        if (LEO.isAvailable()) availableProvers.add("leo");
        if(Vampire.isAvailable()) availableProvers.add("vampire");
        return availableProvers;
    }

    /********************************************************************
     * Main method for querying vampire class
     * @param query ATPQuery object containing options for Vampire [Language|ClosedWorldAssumption|ModusPonens|TestFilePath]
     * @return ATPResult object containing the outcome of the Vampire Query
     */
    private ATPResult askVampire(ATPQuery query) {

        boolean previousCWA = SUMOKBtoTPTPKB.CWA;
        boolean previousModusPonens = query.getKb().modensPonens;
        boolean previousDropOnePremise = query.getKb().dropOnePremiseFormulas;
        try {
            SUMOKBtoTPTPKB.CWA = query.isClosedWorldAssumption();
            query.getKb().modensPonens = query.isModusPonens();
            query.getKb().dropOnePremiseFormulas = query.isModusPonens() && query.isDropOnePremise();
            Vampire vampire = new Vampire(
                query.getKb(),
                query.getLanguage().name(),
                query.getVampireMode().name(),
                query.isModusPonens(),
                query.getTimeout(),
                query.getMaxAnswers(),
                query.getUserSessionId()
            );
            if (query.getLanguage().name().equals("FOF") || query.getLanguage().name().equals("TFF")) {
                vampire.setAskQuestion(isAnswerSeekingQuery(query.getQuery()));
                vampire.askVampire(query.getQuery());
            }
            else {
                String testFilePath = query.getTestFilePath();
                if (testFilePath != null && testFilePath.endsWith(".thf")) vampire.askVampireTHF(testFilePath);
                else vampire.askVampireHOL(query.getQuery(), query.isHolUseModals());
            }
            return vampire.getResult();
        }
        finally {
            SUMOKBtoTPTPKB.CWA = previousCWA;
            query.getKb().modensPonens = previousModusPonens;
            query.getKb().dropOnePremiseFormulas = previousDropOnePremise;
        }
    }
    
    /********************************************************************
     * Main method for querying the EProver class
     * @param query ATPQuery object containing options for EProver
     * @return ATPResult object containing the outcome of the EProver Query
     */
    private ATPResult askEProver(ATPQuery query) {

        String lang = "TFF".equals(query.getLanguage().name()) ? "tff" : "fof";
        if ("THF".equals(query.getLanguage().name()))
            return ATPResult.notRun("EProver", "EProver does not support THF/HOL.");
        EProver eprover = new EProver(
                query.getKb(),
                lang,
                query.getTimeout(),
                query.getMaxAnswers(),
                query.getUserSessionId());
        eprover.askEProver(query.getQuery());
        return eprover.getResult();
    }

    /********************************************************************
     * Main method for querying the LEO class
     * @param query ATPQuery object containing options for LEO
     * @return ATPResult object containing the outcome of the LEO Query
     */
    private ATPResult askLeo(ATPQuery query) {
        LEO leo = new LEO(query.getKb(), query.getLanguage().name(), query.getTimeout(), query.getMaxAnswers(), query.getUserSessionId());
        leo.askLeo(query.getQuery());
        return leo.getResult();
    }

    /********************************************************************
     * Return true only when the user is asking for bindings, not merely asking Vampire to prove a Boolean conjecture.
     */
    private static boolean isAnswerSeekingQuery(String stmt) {

        if (StringUtil.emptyString(stmt)) return false;
        try {
            KIF kif = new KIF();
            String err = kif.parseStatement(stmt);
            if (err == null && !kif.formulaMap.isEmpty()) {
                Formula f = kif.formulaMap.values().iterator().next();
                Set<String> vars = f.collectUnquantifiedVariables();
                return vars != null && !vars.isEmpty();
            }
        }
        catch (Exception e) {
        }
        return stmt.matches(".*[?@][A-Za-z][A-Za-z0-9_-]*.*");
    }

    /********************************************************************
     * Converts a TPTP problem to CNF, normalizes CNF axiom roles, generates filtered problems with e_axfilter, runs each with Vampire, and stops after the first contradiction.
     * @param kb knowledge base associated with the problem
     * @param inputProblem complete TPTP input problem
     * @param options e_axfilter options
     * @param cnfTimeout E CNF conversion timeout in seconds
     * @param filterTimeout e_axfilter timeout in seconds
     * @param vampireTimeout Vampire timeout per generated problem in seconds
     * @param vampireMode Vampire mode: CASC, AVATAR, VAMPIRE, or CUSTOM
     */
    public EAxFilterVampireResult runEAxFilterWithVampire(KB kb, Path inputProblem, EAxFilter.EAxFilterOptions options, int cnfTimeout, int filterTimeout, int vampireTimeout, String vampireMode) throws IOException, InterruptedException {
        
        if (!EAxFilter.isAvailable()) throw new IllegalStateException("e_axfilter executable is unavailable");
        if (!Vampire.isAvailable()) throw new IllegalStateException("Vampire executable is unavailable");
        if (inputProblem == null || !Files.isRegularFile(inputProblem)) throw new IllegalArgumentException("TPTP input problem does not exist: " + inputProblem);
        Path runDirectory = Files.createTempDirectory("eaxfilter-vampire-");
        Path cnfDirectory = runDirectory.resolve("cnf");
        Path filteredDirectory = runDirectory.resolve("filtered");
        System.out.println("TheoremProverController.runEAxFilterWithVampire(): converting input to CNF");
        ECNF.CNFResult cnfResult = new ECNF().convertForAxiomFiltering(inputProblem, cnfDirectory, cnfTimeout);
        if (!cnfResult.succeeded()) {
            System.err.println("TheoremProverController.runEAxFilterWithVampire(): CNF conversion failed");
            System.err.println("Command: " + String.join(" ", cnfResult.command()));
            System.err.println("Exit code: " + cnfResult.exitCode());
            System.err.println("Timed out: " + cnfResult.timedOut());
            System.err.println("E stderr: " + cnfResult.stderrFile());
            throw new IOException("E CNF conversion failed");
        }
        System.out.println("TheoremProverController.runEAxFilterWithVampire(): produced " + cnfResult.clauseCount() + " CNF clauses and normalized " + cnfResult.normalizedRoles() + " plain roles");
        EAxFilter.EAxFilterResult filterResult = new EAxFilter().generate(cnfResult.normalizedCNF(), filteredDirectory, options, filterTimeout);
        if (!filterResult.succeeded()) {
            System.err.println("TheoremProverController.runEAxFilterWithVampire(): e_axfilter failed");
            System.err.println("Command: " + String.join(" ", filterResult.command()));
            System.err.println("Exit code: " + filterResult.exitCode());
            System.err.println("Timed out: " + filterResult.timedOut());
            if (!filterResult.stderr().isEmpty()) {
                int firstLine = Math.max(0, filterResult.stderr().size() - 20);
                System.err.println("Final " + (filterResult.stderr().size() - firstLine) + " of " + filterResult.stderr().size() + " e_axfilter stderr lines:");
                System.err.println(String.join(System.lineSeparator(), filterResult.stderr().subList(firstLine, filterResult.stderr().size())));
            }
            throw new IOException("e_axfilter failed");
        }
        System.out.println("TheoremProverController.runEAxFilterWithVampire(): generated " + filterResult.generatedProblems().size() + " filtered problems");
        List<FilteredVampireAttempt> attempts = new ArrayList<>();
        FilteredVampireAttempt successfulAttempt = null;
        int current = 0;
        for (Path problemFile : filterResult.generatedProblems()) {
            current++;
            System.out.println("TheoremProverController.runEAxFilterWithVampire(): running Vampire " + current + "/" + filterResult.generatedProblems().size() + ": " + problemFile.getFileName());
            Vampire vampire = new Vampire(kb, "fof", vampireMode, false, vampireTimeout, 1);
            vampire.setAskQuestion(false);
            ATPResult result = vampire.runProblem(problemFile);
            FilteredVampireAttempt attempt = new FilteredVampireAttempt(problemFile, result);
            attempts.add(attempt);
            if (result == null) {
                System.err.println("TheoremProverController.runEAxFilterWithVampire(): Vampire returned no result for " + problemFile);
                continue;
            }
            System.out.println("TheoremProverController.runEAxFilterWithVampire(): " + problemFile.getFileName() + " -> " + result.getSzsStatus());
            if (attempt.foundContradiction()) {
                successfulAttempt = attempt;
                System.out.println("TheoremProverController.runEAxFilterWithVampire(): contradiction found in " + problemFile);
                break;
            }
        }
        if (successfulAttempt == null) System.out.println("TheoremProverController.runEAxFilterWithVampire(): no contradiction found within the limits of " + attempts.size() + " attempted problems");
        return new EAxFilterVampireResult(cnfResult, filterResult, attempts, successfulAttempt);
    }

    /********************************************************************
     * Prints the main() console options for this class.
     */
    private static void showHelp() {

        System.out.println("TheoremProverController");
        System.out.println("Usage:");
        System.out.println("  -v \"<SUO-KIF query>\"     Query Vampire");
        System.out.println("  -e \"<SUO-KIF query>\"     Query EProver");
        System.out.println("  -l \"<SUO-KIF query>\"     Query LEO");
        System.out.println("  --axfilter <file>          Filter a TPTP problem and search for contradictions with Vampire");
        System.out.println("Basic options:");
        System.out.println("  -h, --help               Show this help screen");
        System.out.println("  -a, --available          Print available provers");
        System.out.println("  --timeout <seconds>      Query timeout. Default: 30");
        System.out.println("  --answers <n>            Max answers. Default: 1");
        System.out.println("Language options:");
        System.out.println("  --lang fof               Use FOF/TPTP");
        System.out.println("  --lang tff               Use TFF");
        System.out.println("  --lang thf               Use THF/HOL. Vampire and LEO only");
        System.out.println("Vampire-only options:");
        System.out.println("  --mode casc              Vampire CASC mode. Default");
        System.out.println("  --mode avatar            Vampire AVATAR mode");
        System.out.println("  --mode vampire           Vampire native mode");
        System.out.println("  --mode custom            Use VAMPIRE_OPTS");
        System.out.println("  --cwa                    Enable closed world assumption");
        System.out.println("  --mp                     Enable modus ponens mode");
        System.out.println("  --dropOnePremise         Drop one-premise formulas. Requires --mp");
        System.out.println("  --modal                  In THF/HOL mode, use modal THF translation");
        System.out.println("Examples:");
        System.out.println("  java -cp build/classes:lib/* com.articulate.sigma.tp.TheoremProverController -v \"(subclass ?X Object)\"");
        System.out.println("  java -cp build/classes:lib/* com.articulate.sigma.tp.TheoremProverController -v \"(subclass ?X Object)\" --lang tff");
        System.out.println("  java -cp build/classes:lib/* com.articulate.sigma.tp.TheoremProverController -v \"(instance ?X Relation)\" --lang thf --modal");
        System.out.println("  java -cp build/classes:lib/* com.articulate.sigma.tp.TheoremProverController -v \"(=> (instance ?X Human) (instance ?X Mammal))\" --mp");
        System.out.println("  java -cp build/classes:lib/* com.articulate.sigma.tp.TheoremProverController -e \"(subclass ?X Object)\" --lang fof");
        System.out.println("  java -cp build/classes:lib/* com.articulate.sigma.tp.TheoremProverController -l \"(instance ?X Relation)\"");
    }
    
    /********************************************************************
     * Main method for this class used to test the different theorem provers and their options.
     */
    public static void main(String[] args) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        System.out.printf("TheoremProverController.main(%s)%n", argMap);
        if (argMap.isEmpty() || argMap.containsKey("h") || argMap.containsKey("help")) {
            showHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
        if (argMap.containsKey("a") || argMap.containsKey("available")) {
            System.out.println("Available Provers: " + TheoremProverController.availableProvers());
            return;
        }        
        if (argMap.containsKey("axfilter")) {
            String input = argMap.get("axfilter").get(0);
            int cnfTimeout = argMap.containsKey("cnfTimeout") ? Integer.parseInt(argMap.get("cnfTimeout").get(0)) : 300;
            int filterTimeout = argMap.containsKey("filterTimeout") ? Integer.parseInt(argMap.get("filterTimeout").get(0)) : 300;
            int vampireTimeout = argMap.containsKey("timeout") ? Integer.parseInt(argMap.get("timeout").get(0)) : 30;
            TheoremProverController controller = new TheoremProverController();
            try {
                EAxFilterVampireResult result = controller.runEAxFilterWithVampire(kb, Path.of(input), EAxFilter.EAxFilterOptions.forContradictions(), cnfTimeout, filterTimeout, vampireTimeout, "CASC");
                System.out.println("Normalized CNF: " + result.cnfResult().normalizedCNF());
                System.out.println("CNF clauses: " + result.cnfResult().clauseCount());
                System.out.println("Normalized roles: " + result.cnfResult().normalizedRoles());
                System.out.println("Generated problems: " + result.filterResult().generatedProblems().size());
                System.out.println("Attempted problems: " + result.attempts().size());
                System.out.println("Contradiction found: " + result.foundContradiction());
                if (result.successfulAttempt() != null) System.out.println("Successful problem: " + result.successfulAttempt().problemFile());
            }
            catch (Exception exception) {
                System.err.println("Filtered Vampire test failed: " + exception.getMessage());
                exception.printStackTrace();
            }
            return;
        }
        String proverType = null;
        String queryString = null;
        if (argMap.containsKey("v")) {
            proverType = "VAMPIRE";
            queryString = String.join(" ", argMap.get("v"));
        }
        else if (argMap.containsKey("e")) {
            proverType = "EPROVER";
            queryString = String.join(" ", argMap.get("e"));
        }
        else if (argMap.containsKey("l")) {
            proverType = "LEO";
            queryString = String.join(" ", argMap.get("l"));
        }
        else {
            System.err.println("No prover selected. Use -v, -e, or -l.");
            showHelp();
            return;
        }
        if (queryString == null || queryString.trim().isEmpty()) {
            System.err.println("Missing SUO-KIF query.");
            showHelp();
            return;
        }
        String lang = "FOF";
        if (argMap.containsKey("lang") && !argMap.get("lang").isEmpty()) {
            lang = argMap.get("lang").get(0).toUpperCase();
            if ("TPTP".equals(lang)) lang = "FOF";
        }
        String vampireMode = "CASC";
        if (argMap.containsKey("mode") && !argMap.get("mode").isEmpty()) vampireMode = argMap.get("mode").get(0).toUpperCase();
        int timeout = 30;
        if (argMap.containsKey("timeout") && !argMap.get("timeout").isEmpty()) {
            try {
                timeout = Integer.parseInt(argMap.get("timeout").get(0));
            }
            catch (NumberFormatException nfe) {
                System.err.println("Invalid timeout: " + argMap.get("timeout").get(0));
                return;
            }
        }
        int maxAnswers = 1;
        if (argMap.containsKey("answers") && !argMap.get("answers").isEmpty()) {
            try {
                maxAnswers = Integer.parseInt(argMap.get("answers").get(0));
            }
            catch (NumberFormatException nfe) {
                System.err.println("Invalid answers value: " + argMap.get("answers").get(0));
                return;
            }
        }
        boolean cwa = argMap.containsKey("cwa") || argMap.containsKey("CWA");
        boolean modusPonens = argMap.containsKey("mp") || argMap.containsKey("modusPonens");
        boolean dropOnePremise = argMap.containsKey("dropOnePremise");
        boolean holUseModals = argMap.containsKey("modal");
        if (!"FOF".equals(lang) && !"TFF".equals(lang) && !"THF".equals(lang)) {
            System.err.println("Invalid language: " + lang + ". Use --lang fof, --lang tff, or --lang thf.");
            return;
        }
        if ("EPROVER".equals(proverType)) {
            if ("THF".equals(lang)) {
                System.err.println("EProver does not support THF/HOL in this controller. Use --lang fof or --lang tff.");
                return;
            }
            if (argMap.containsKey("mode") || cwa || modusPonens || dropOnePremise || holUseModals) {
                System.err.println("Invalid option for EProver. --mode, --cwa, --mp, --dropOnePremise, and --modal are Vampire-only.");
                return;
            }
            vampireMode = null;
        }
        if ("LEO".equals(proverType)) {
            if (argMap.containsKey("lang") && !"THF".equals(lang)) {
                System.err.println("LEO should be used with THF/HOL. Do not use --lang fof or --lang tff.");
                return;
            }
            if (argMap.containsKey("mode") || cwa || modusPonens || dropOnePremise || holUseModals) {
                System.err.println("Invalid option for LEO. --mode, --cwa, --mp, --dropOnePremise, and --modal are Vampire-only.");
                return;
            }
            lang = "THF";
            vampireMode = null;
        }
        if ("VAMPIRE".equals(proverType)) {
            if (!"CASC".equals(vampireMode) && !"AVATAR".equals(vampireMode) && !"VAMPIRE".equals(vampireMode) && !"CUSTOM".equals(vampireMode)) {
                System.err.println("Invalid Vampire mode: " + vampireMode + ". Use casc, avatar, vampire, or custom.");
                return;
            }
            if (dropOnePremise && !modusPonens) {
                System.err.println("--dropOnePremise requires --mp.");
                return;
            }
            if (holUseModals && !"THF".equals(lang)) {
                System.err.println("--modal only applies with --lang thf.");
                return;
            }
        }
        ATPQuery atpQuery = new ATPQuery(
                kb,
                null,
                queryString,
                null,
                "CUSTOM",
                proverType,
                lang,
                vampireMode,
                cwa,
                modusPonens,
                dropOnePremise,
                holUseModals,
                timeout,
                maxAnswers
        );
        TheoremProverController theoremProverController = new TheoremProverController();
        ATPResult result = theoremProverController.ask(atpQuery);
        if (result == null) {
            System.err.println("No ATPResult returned. Query may not have translated, or the prover may not have run.");
            return;
        }
        System.out.println("TheoremProverController.main(): Summary=");
        System.out.println(result.getSummary());
        System.out.println("\nRaw prover output:");
        for (String line : result.getStdout()) System.out.println(line);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.parseProofOutput(result.getStdout(), atpQuery.getQuery(), kb, result.getQList());
        tpp.processAnswersFromProof(result.getQList(), atpQuery.getQuery());
        System.out.println("\nBindings:");
        System.out.println(tpp.bindings);
        System.out.println("\nBinding map:");
        System.out.println(tpp.bindingMap);
        System.out.println("\nProof steps:");
        System.out.println(tpp.proof == null ? 0 : tpp.proof.size());
        LoggingUtils.log("INFO", "Query Result: " + result.getSummary());
    }
}