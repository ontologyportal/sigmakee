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

import com.articulate.sigma.CLIMapParser;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.ATPQuery.ATPType;
import com.articulate.sigma.tp.ATPQuery.RunSource;
import com.articulate.sigma.tp.ATPQuery.TptpLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;

public class TheoremProverController {

    public TheoremProverController () {}

    public ATPResult ask (ATPQuery query) {
        switch (query.getProverType()) {
            case EPROVER:
                return this.askEProver(query);
                break;
            case VAMPIRE: 
                return this.askVampire(query);
            case LEO:
                // return this.askLeo(query);
                break;
            default:
                System.err.println("TheoremProverController.ask(): INVALID PROVER");
                break;
        }
        return null;
    }

    public static List<String> availableProvers() {
        
        List<String> availableProvers = new ArrayList<>();
        if (EProver.isAvailable()) availableProvers.add("eprover");
        if (LEO.isAvailable()) availableProvers.add("leo");
        if(Vampire.isAvailable()) availableProvers.add("vampire");
        return availableProvers;
    }

    private ATPResult askVampire(ATPQuery query) {
        Vampire vampire = new Vampire(query.getKb(), query.getLanguage().name(), query.getVampireMode().name(), query.isModusPonens(), query.getTimeout(), query.getMaxAnswers());
        if (query.getLanguage().name().equals("FOF") || query.getLanguage().name().equals("TFF")) {
            if (query.isClosedWorldAssumption()) SUMOKBtoTPTPKB.CWA = true;
            if (query.isModusPonens() && query.isDropOnePremise()) query.getKb().dropOnePremiseFormulas = true;
            vampire.askVampire(query.getQuery());
        } 
        else {
            if (query.getTestFilePath() == null) vampire.askVampireHOL(query.getQuery(), query.isHolUseModals());
            else vampire.askVampireTHF(query.getTestFilePath());
        }
        System.out.println(String.join("\n",vampire.output));
        return vampire.getResult();
    }

    private ATPResult askEProver(ATPQuery query) {
        
    }

    // private ATPResult askLEO(ATPQuery query) {

    // }

    private static void showHelp() {

        System.out.println("TheoremProverController class");
        System.out.println("  h - show this help screen");
        System.out.println("  -ap - print available provers");
        System.out.println("  -ap - print available provers");
        System.out.println("  -v - query vampire");
        System.out.println("  -e - query EProver");
        System.out.println("  -l - query LEO");
    }
    
    public static void main(String[] args) {
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
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
            boolean closedWorldAssumption = false;
            boolean modusPonens = false;
            boolean dropOnePremise = false;
            boolean holUseModals = false;
            int timeout = 30;
            int maxAnswers = 1;
            ATPQuery atpQuery = new ATPQuery(
                kb, 
                null, 
                "(instance Chair Furniture)", 
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
            ATPResult atpResult = theoremProverController.ask(atpQuery);
            System.out.println("TheoremProverController.main(): Result=\n" + atpResult.toString());
        }
    }
}