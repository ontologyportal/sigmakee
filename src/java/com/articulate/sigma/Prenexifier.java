package com.articulate.sigma;

import com.articulate.sigma.parsing.FormulaAST;

/**
 This code is copyright Articulate Software (c) 2003.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code are also requested, to credit Articulate Software in any
 writings, briefings, publications, presentations, or
 other representations of any software which incorporates,
 builds on, or uses this code. Please cite the following
 article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
 */
public class Prenexifier {


    /** ***************************************************************
     */
    public FormulaAST binarizeConnectives(FormulaAST f) {

        if (f.empty()) return f;
        if (f.atom()) return f;
        if (f.car().equals(FormulaAST.OR) || f.car().equals(FormulaAST.XOR) || f.car().equals(FormulaAST.AND)) {
            // TODO: do nothing?
        }
        return f;
    }

    /** ***************************************************************
     */
    public FormulaAST prenex(FormulaAST f) {
        return f;
    }

    /** ***************************************************************
     */
    public static void testBinarize() {

    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

    }
}
