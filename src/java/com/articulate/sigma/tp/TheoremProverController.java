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
import com.articulate.sigma.tp.ATPQuery.ATPType;
import com.articulate.sigma.tp.ATPQuery.RunSource;
import com.articulate.sigma.tp.ATPQuery.TptpLanguage;
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
     * Prints the main() console options for this class
     */
    private static void showHelp() {

        System.out.println("TheoremProverController class");
        System.out.println("  h - show this help screen");
        System.out.println("  -a - print available provers");
        System.out.println("  -v - query vampire");
        System.out.println("  -e - query EProver");
        System.out.println("  -l - query LEO");
    }
    
    /********************************************************************
     * Main method for this class used to test the different theorem provers and their options.
     */
    public static void main(String[] args) {
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        System.out.printf("TheoremProverController.main(%s)", argMap);
        TheoremProverController theoremProverController = new TheoremProverController();
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            showHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        if (argMap.containsKey("a")) {
            System.out.println("Available Provers: " + TheoremProverController.availableProvers());
            return;
        }
        if (argMap.containsKey("v")) {
            ATPQuery atpQuery = new ATPQuery(
                kb, 
                null, 
                "(instance ?X Relation)", 
                null, 
                "CUSTOM", 
                "VAMPIRE", 
                "FOF", 
                "CASC", 
                false, 
                false, 
                false, 
                false, 
                30, 
                1
            );
            System.out.println("TheoremProverController.main(): Result=\n" + theoremProverController.ask(atpQuery).toString());
        }
        if (argMap.containsKey("e")) {
            ATPQuery atpQuery = new ATPQuery(
                kb, 
                null, 
                "(instance ?X Relation)", 
                null, 
                "CUSTOM",
                "EPROVER", 
                "FOF", 
                null,
                false, 
                false, 
                false, 
                false, 
                30, 
                1
            );
            System.out.println("TheoremProverController.main(): Result=\n" + theoremProverController.ask(atpQuery).toString());
        }
    }
}