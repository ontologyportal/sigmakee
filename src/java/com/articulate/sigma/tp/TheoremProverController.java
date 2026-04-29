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
import java.util.List;
import java.util.Map;

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;

public class TheoremProverController {

    public TheoremProverController () {}

    public ATPResult ask (ATPQuery query) {
        switch (query.getProverType()) {
            case EPROVER:
                // return this.askEProver(query);
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

    private ATPResult askVampire(ATPQuery query) {
        Vampire vampire = new Vampire(query.getKb(), query.getLanguage().name(), query.getVampireMode().name(), query.isModusPonens(), query.getTimeout(), query.getMaxAnswers());
        if (query.getLanguage().name().equals("FOF") || query.getLanguage().name().equals("TFF")) {
            if (query.isClosedWorldAssumption()) SUMOKBtoTPTPKB.CWA = true;
            if (query.isModusPonens() && query.isDropOnePremise()) query.getKb().dropOnePremiseFormulas = true;
            vampire.askVampire(query.getQuery());
            return vampire.getResult();
        } 
        else {
            if (query.getTestFilePath().isEmpty() || query.getTestFilePath() == null) vampire.askVampireHOL(query.getQuery(), query.isHolUseModals());
            else vampire.askVampireTHF(query.getTestFilePath());
            return vampire.getResult();
        }
    }

    // private ATPResult askEProver(ATPQuery query) {
        
    // }

    // private ATPResult askLEO(ATPQuery query) {

    // }

    private static void showHelp() {

        System.out.println("TheoremProverController class");
        System.out.println("  h - show this help screen");
        System.out.println("  --v <> <> - query vampire");
        System.out.println("  --e <> <> - query EProver");
        System.out.println("  --l <> <> - query LEO");
    }
    
    public static void main(String[] args) {
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            showHelp();
            return;
        }
        TheoremProverController theoremProverController = new TheoremProverController();
        
    }
}