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

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;

public class TheoremProverController {

    public TheoremProverController () {
        
    }

    public ATPResult ask (ATPQuery query) {
        switch (query.getProverType()) {
            case EPROVER:
                return this.askEProver(query);
                break;
            case VAMPIRE: 
                return this.askVampire(query);
                break;
            case LEO:
                return this.askLeo(query);
                break;
            default:
                System.err.println("TheoremProverController.ask(): INVALID PROVER");
                break;
        }
    }

    private ATPResult askEProver(ATPQuery query) {
        
    }

    private ATPResult askVampire(ATPQuery query) {
        Vampire vampire = new Vampire(query.getKb(), query.getLanguage(), query.getVampireMode(), query.getModusPonens(), query.getTimeout(), query.getMaxAnswers());
        switch(query.getLanguage()) {
            case FOF:
                break;
            case TFF:
                if (query.isClosedWorldAssumption()) SUMOKBtoTPTPKB.CWA = true;
                if (query.isDropOnePremise()) query.getKb().dropOnePremiseFormulas = true;
                break;
            case THF:
                break;
        }
    }

    private ATPResult askLEO(ATPQuery query) {

    }
    
    public static void main(String[] args) {

    }
}