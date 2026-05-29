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

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import com.articulate.sigma.utils.LoggingUtils;
import com.articulate.sigma.tp.ATPQuery.ATPType;
import com.articulate.sigma.tp.ATPQuery.RunSource;
import com.articulate.sigma.tp.ATPQuery.TptpLanguage;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.parsing.CLIMapParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;

public class TheoremProverController {

    public TheoremProverController () {}
    
    /********************************************************************
     * Primary API for the class. Capable of asking all 3 provers.
     * @param query ATPQuery object used to determine which prover to ask with associated options.
     */
    public ATPResult ask (ATPQuery query) {
        
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
            Vampire vampire = new Vampire(query.getKb(), query.getLanguage().name(), query.getVampireMode().name(), query.isModusPonens(), query.getTimeout(), query.getMaxAnswers(), query.getUserSessionId());
            if (query.getLanguage().name().equals("FOF") || query.getLanguage().name().equals("TFF")) vampire.askVampire(query.getQuery());
            else {
                if (query.getTestFilePath() == null) vampire.askVampireHOL(query.getQuery(), query.isHolUseModals());
                else vampire.askVampireTHF(query.getTestFilePath());
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
        EProver eprover = new EProver(query.getKb(), query.getLanguage().name(), query.getTimeout(), query.getMaxAnswers(), query.getUserSessionId());
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
     * Prints the main() console options for this class.
     */
    private static void showHelp() {

        System.out.println("TheoremProverController");
        System.out.println("Usage:");
        System.out.println("  -v \"<SUO-KIF query>\"     Query Vampire");
        System.out.println("  -e \"<SUO-KIF query>\"     Query EProver");
        System.out.println("  -l \"<SUO-KIF query>\"     Query LEO");
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
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        if (argMap.containsKey("a") || argMap.containsKey("available")) {
            System.out.println("Available Provers: " + TheoremProverController.availableProvers());
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