package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.io.*;
import java.text.ParseException;

/** *****************************************************************
 * A class that finds problems in a knowledge base.  It is not meant
 * to be instantiated.
 */
public class Diagnostics {

    /** *****************************************************************
     * Return a list of terms (for a given argument position) that do not 
     * have a speicifed relation.
     * @param kb the knowledge base
     * @param rel the relation name
     * @param argnum the argument position of the term
     * @param limit the maximum number of results to return, or -1 if all
     * @param letter the first letter of the term name
     */
    public static ArrayList termsWithoutRelation(KB kb, String rel, int argnum, 
                                                 int limit, char letter) {

        ArrayList result = new ArrayList();
        String term = null;
        ArrayList forms = null;
        Iterator it2 = null;
        Formula formula = null;
        String pred = null;
        Iterator it = kb.terms.iterator();
        while ( it.hasNext() ) {
            term = (String) it.next();
            forms = kb.ask("arg",argnum,term);
            if ( forms == null || forms.isEmpty() ) {
                if (letter < 'A' || term.charAt(0) == letter) 
                    result.add(term);                
            }
            else {
                boolean found = false;
                it2 = forms.iterator();
                while ( it2.hasNext() ) {
                    formula = (Formula) it2.next();
                    pred = formula.car();
                    if ( pred.equals(rel) ) {
                        found = true;
                        break;
                    }
                }
                if ( ! found ) {
                    if (letter < 'A' || term.charAt(0) == letter) 
                        result.add(term);                    
                }
            }
            if ( limit > 0 && result.size() > limit ) {
                result.add("limited to " + limit + " results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a documentation string.
     */
    public static ArrayList termsWithoutDoc(KB kb) {

        return termsWithoutRelation(kb,"documentation",1,100,' ');                                              
        /**
        ArrayList result = new ArrayList();
        String term = null;
        ArrayList forms = null;
        Iterator it2 = null;
        Formula formula = null;
        String pred = null;
        Iterator it = kb.terms.iterator();
        int count = 0;
        while ( it.hasNext() ) {
            term = (String) it.next();
            forms = kb.ask("arg",1,term);
            if ( forms == null || forms.isEmpty() ) {
                result.add(term);
            }
            else {
                boolean found = false;
                it2 = forms.iterator();
                while ( it2.hasNext() ) {
                    formula = (Formula) it2.next();
                    pred = formula.car();
                    if ( pred.equals("documentation") ) {
                        found = true;
                        break;
                    }
                }
                if ( ! found ) {
                    result.add(term);
                    count++;
                }
            }
            if ( count > 99 ) {
                result.add("limited to 100 results");
                break;
            }
        }
        return result;
         * **/
    }

    /** *****************************************************************
     * Return a list of terms that have more than one documentation string.
     */
    public static ArrayList termsWithMultipleDoc(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithMultipleDoc(): "); 

        ArrayList result = new ArrayList();
        String term = null;
        ArrayList forms = null;
        Iterator it = kb.terms.iterator();
        int count = 0;
        while ( it.hasNext() ) {
            term = (String) it.next();
            forms = kb.askWithRestriction(0,"documentation",1,term);
            if (forms.size() > 1) {
                result.add(term);
                count++;
            }
            if ( count > 99 ) {
                result.add("limited to 100 results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a parent term.
     */
    public static ArrayList termsWithoutParent(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithoutParent(): "); 
        ArrayList result = new ArrayList();
        List preds = Arrays.asList( "instance", "subclass", "subAttribute", "subrelation", "subCollection" );
        String term = null;
        String pred = null;
        ArrayList forms = null;
        Iterator it = kb.terms.iterator();
        Iterator it2 = null;
        int count = 0;
        while (it.hasNext()) {
            term = (String) it.next();
            forms = kb.ask("arg",1,term);
            if ( forms == null || forms.isEmpty() ) {
                result.add(term);
                count++;
            }
            else {
                boolean found = false;
                it2 = forms.iterator();
                while ( it2.hasNext() ) {
                    pred = ((Formula) it2.next()).car();
                    found = preds.contains(pred);
                    if ( found ) { break; };
                }
                if ( ! found ) { 
                    result.add(term); 
                    count++;
                }
            }
            if ( count > 99 ) {
                result.add("limited to 100 results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that have parents which are disjoint.
     */
    public static ArrayList childrenOfDisjointParents(KB kb) {

        System.out.println("INFO in Diagnostics.childrenOfDisjointParents(): "); 
        ArrayList result = new ArrayList();
        String term = null;
        String termX = null;
        String termY = null;
        Set parentSet = null;
        Object[] parents = null;
        Set disjoints = null;
        Iterator it = kb.terms.iterator();
        int count = 0;
        boolean contradiction = false;
        while (it.hasNext()) {
            contradiction = false;
            term = (String) it.next();
            parentSet = kb.getCachedRelationValues( "subclass", term, 1, 2 );
            parents = null;
            if ( (parentSet != null) && !parentSet.isEmpty() ) {
                parents = parentSet.toArray();
            }
            if ( parents != null ) {
                for ( int i = 0 ; (i < parents.length) && !contradiction ; i++ ) {
                    termX = (String) parents[i];
                    disjoints = kb.getCachedRelationValues( "disjoint", termX, 1, 2 );
                    if ( (disjoints != null) && !disjoints.isEmpty() ) {
                        for ( int j = (i + 1) ; j < parents.length ; j++ ) {
                            termY = (String) parents[j];
                            if ( disjoints.contains(termY) ) {
                                result.add( term );
                                contradiction = true;
                                count++;
                                System.out.println( "INFO in Diagnostics.childrenOfDisjointParents(): " 
                                                    + termX 
                                                    + " and " 
                                                    + termY 
                                                    + " are disjoint parents of " 
                                                    + term  );
                                break;
                            }
                        }
                    }
                }
            }

            if ( count > 99 ) {
                result.add("limited to 100 results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of classes that are subclasses of a partitioned class,
     * which do not appear in the partition listing.  For example,
     * (subclass E A), (partition A B C D).  "exhaustiveDecomposition" has
     * the same meaning and needs to be checked also.
     */
    public static ArrayList extraSubclassInPartition(KB kb) {

        System.out.println("INFO in Diagnostics.extraSubclassInPartition(): "); 
        ArrayList result = new ArrayList();
        ArrayList forms = kb.ask("arg",0,"partition");
        if (forms == null) 
            forms = new ArrayList();
        ArrayList forms2 = kb.ask("arg",0,"exhaustiveDecomposition");
        if (forms2 != null) 
            forms.addAll(forms2);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            String parent = form.getArgument(1);
            ArrayList partition = form.argumentsToArrayList(2);
            ArrayList subs = kb.askWithRestriction(0, "subclass", 2, parent);
            if (subs != null) {
                for (int j = 0; j < subs.size(); j++) {
                    Formula subform = (Formula) subs.get(j);
                    String child = subform.getArgument(1);
                    if (!partition.contains(child.intern())) {
                        result.add(child);
                    }
                    if (result.size() > 99) {
                        result.add("limited to 100 results");
                        return result;
                    }
                }
            }
        }
        return result;
    }

    /** *****************************************************************
     * Find all terms which do not appear in any implication (rule).
     */
    public static ArrayList termsWithoutRules(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithoutRules(): "); 
        ArrayList result = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList forms = kb.ask("ant",-1,term);
            ArrayList forms2 = kb.ask("cons",-1,term);
            if (forms == null && forms2 == null) 
                result.add(term);
            if (result.size() > 99) {
                result.add("limited to 100 results");
                return result;
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return true if a quantifiers in a quantifier list is not found
     * in the body of the statement.
     */
    private static boolean quantifierNotInStatement(Formula f) {

        if (f.theFormula == null || f.theFormula.length() < 1 ||
            f.theFormula.equals("()") || f.theFormula.indexOf("(") == -1)
            return false;
        if (!f.car().equalsIgnoreCase("forall") &&                       // Recurse for complex expressions.
            !f.car().equalsIgnoreCase("exists")) {
            Formula f1 = new Formula();
            f1.read(f.car());
            Formula f2 = new Formula();
            f2.read(f.cdr());
            return (quantifierNotInStatement(f1) || quantifierNotInStatement(f2));
        }
        Formula form = new Formula();
        form.read(f.theFormula);
        if (form.car() != null && form.car().length() > 0) {    // This test shouldn't be needed.
            String rest = form.cdr();                   // Quantifier list plus rest of statement
            Formula quant = new Formula();
            quant.read(rest);

            String q = quant.car();                     // Now just the quantifier list.
            String body = quant.cdr();
            quant.read(q);
            ArrayList qList = quant.argumentsToArrayList(0);  // Put all the quantified variables into a list.
            if (rest.indexOf("exists") != -1 || rest.indexOf("forall") != -1) { //nested quantifiers
                Formula restForm = new Formula();
                restForm.read(rest);
                restForm.read(restForm.cdr());
                if (quantifierNotInStatement(restForm)) 
                    return true;
            }
            for (int i = 0; i < qList.size(); i++) {
                String var = (String) qList.get(i);
                if (body.indexOf(var) == -1) 
                    return true;
            }
        }
        return false;
    }

    /** *****************************************************************
     * Find cases where a variable appears in a quantifier list, but not
     * in the body of the quantified expression.  For example
     * (exists (?FOO) (bar ?FLOO Shmoo))
     * @return an ArrayList of Formula(s).
     */
    public static ArrayList quantifierNotInBody(KB kb) {

        System.out.println("INFO in Diagnostics.quantifierNotInBody(): "); 
        ArrayList result = new ArrayList();
        ArrayList forms = kb.ask("ant",-1,"forall");        // Collect all the axioms with quantifiers.
        if (forms == null) 
            forms = new ArrayList();
        ArrayList forms2 = kb.ask("cons",-1,"forall");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("stmt",-1,"forall");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("ant",-1,"exists");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("cons",-1,"exists");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("stmt",-1,"exists");
        if (forms2 != null) 
            forms.addAll(forms2);
        for (int i = 0; i < forms.size(); i++) {             // Iterate through all the axioms.
            Formula form = (Formula) forms.get(i);
            if (quantifierNotInStatement(form)) 
                result.add(form);

            if (result.size() > 19) {
                result.add("limited to 20 results");
                return result;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not ultimately subclass from Entity.
     * This needs to be modified to allow subAttribute(s) and subrelation(s)
     */
    public static ArrayList unrootedTerms(KB kb) {

        System.out.println("INFO in Diagnostics.unrootedTerms()");
        ArrayList result = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {                          // Check every term in the KB
            String term = (String) it.next();
            ArrayList forms = kb.ask("arg",1,term);     // Get every formula with the term as arg 1
            if (forms == null || forms.size() < 1) {
                result.add(term);
            }
            else {
                boolean found = false;
                boolean isClassOrInstance = false;
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    System.out.println("INFO in Diagnostics.unrootedTerms(): Formula: " + formula.theFormula);
                    if ((formula.theFormula.length() > 13) &&
                        (formula.theFormula.indexOf(")") == formula.theFormula.length()-1) &&
                        (formula.theFormula.substring(1,9).equalsIgnoreCase("instance") ||
                         formula.theFormula.substring(1,13).equalsIgnoreCase("subAttribute") ||
                         formula.theFormula.substring(1,12).equalsIgnoreCase("subrelation") ||
                         formula.theFormula.substring(1,9).equalsIgnoreCase("subclass"))) {
                        isClassOrInstance = true;
                        System.out.println("INFO in Diagnostics.unrootedTerms(): found a candidate ");
                        int firstSpace = formula.theFormula.indexOf(" ");
                        int secondSpace = formula.theFormula.indexOf(" ",firstSpace+1);
                        String parent = formula.theFormula.substring(secondSpace+1,formula.theFormula.length()-1);
                        System.out.println("INFO in Diagnostics.unrootedTerms(): parent: " + parent);
                        HashSet parentList = (HashSet) kb.parents.get(parent.intern());
                        if ((parentList != null && parentList.contains("Entity")) || parent.equalsIgnoreCase("Entity")) {
                            found = true;                                                                     
                        }
                    }
                }
                if (found == false && isClassOrInstance) {
                    result.add(term);
                }
            }

            if (result.size() > 99) {
                result.add("limited to 100 results");
                return result;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Make an empty KB for use in Diagnostics. 
     */
    private static KB makeEmptyKB(String kbName) {

        String kbDir = (String)KBmanager.getMgr().getPref("kbDir");
        if (KBmanager.getMgr().existsKB(kbName)) {
            KBmanager.getMgr().removeKB(kbName);
        }
        File dir = new File( kbDir );
        File emptyCFile = new File( dir, "emptyConstituent.txt" );
        String emptyCFilename = emptyCFile.getAbsolutePath();
        FileWriter fw = null; 
        PrintWriter pw = null;
        KBmanager.getMgr().addKB(kbName);
        KB empty = KBmanager.getMgr().getKB(kbName);
        System.out.println("empty = " + empty);

        try { // Fails elsewhere if no constituents, or empty constituent, thus...
            fw = new FileWriter( emptyCFile );
            pw = new PrintWriter(fw);   
            pw.println("(instance instance BinaryPredicate)\n");
            if (pw != null) pw.close();
            if (fw != null) fw.close();
            empty.addConstituent(emptyCFilename);
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + emptyCFilename);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return empty;
    }

    /** *****************************************************************
     * Returns "" if answer is OK, otherwise reports it. 
     */

    private static String reportAnswer(KB kb, String proof, Formula query, String pQuery, String testType) {

        String language = kb.language;
        String kbName = kb.name;
        String hostname = KBmanager.getMgr().getPref("hostname");
        String result = null;
        if (hostname == null || hostname.length() == 0)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null || port.length() == 0)
            port = "8080";
        String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName;
        String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
        StringBuffer html = new StringBuffer();

        if (proof.indexOf("Syntax error detected") != -1) {
            html = html.append("Syntax error in formula : <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>") + "<br><br>");
            result = HTMLformatter.formatProofResult(proof,query.theFormula,
                                                     pQuery,lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }
            
        BasicXMLparser res = new BasicXMLparser(proof);
        ProofProcessor pp = new ProofProcessor(res.elements);
        if (!pp.returnAnswer(0).equalsIgnoreCase("no")) {
            html = html.append(testType + ": <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>") + "<br><br>");
            result = HTMLformatter.formatProofResult(proof,query.theFormula,
                                                     pQuery,lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }
        return "";
    }


    /** *****************************************************************
     * Iterating through all formulas, return a proof of an inconsistent 
     * or redundant one, if such a thing exists.
     */
    public static String kbConsistencyCheck(KB kb) {

        int timeout = 10;
        int maxAnswers = 1;
        String proof;
        String result = null;

        String answer = new String();
        KB empty = makeEmptyKB("consistencyCheck");

        System.out.println("=================== Consistency Testing ===================");
        try {
            Formula theQuery = new Formula();
            Collection allFormulas = kb.formulaMap.values();
            Iterator it = allFormulas.iterator();
            while (it.hasNext()) {
                Formula query = (Formula) it.next();
                ArrayList processedQueries = query.preProcess(false,kb); // may be multiple because of row vars.
                //System.out.println(" query = " + query);
                //System.out.println(" processedQueries = " + processedQueries);

                String processedQuery = null;
                Iterator q = processedQueries.iterator();

                System.out.println("INFO in Diagnostics.kbConsistencyCheck(): size = " + processedQueries.size());
                while (q.hasNext()) {
                    Formula f = (Formula) q.next();
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): formula = " + f.theFormula);
                    processedQuery = f.makeQuantifiersExplicit(false);
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): processedQuery = " + processedQuery);
                    proof = empty.ask(processedQuery,timeout,maxAnswers);
                    String a = reportAnswer(kb,proof,query,processedQuery,"Redundancy");
                    //  if (answer.length() != 0) return answer;
                    answer = answer + a;
                    a = new String();

                    StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");
                    proof = empty.ask(negatedQuery.toString(),timeout,maxAnswers);
                    a = reportAnswer(kb,proof,query,negatedQuery.toString(),"Inconsistency");
                    if (a.length() != 0) {
                        answer = answer + a;
                        return answer;
                    }
                }
                empty.tell(query.theFormula);
            }
        }
        catch ( Exception ex ) {
            return("Error in Diagnostics.kbConsistencyCheck() while executing query: " + ex.getMessage());
        }
        return "No contradictions or redundancies found.";
    }

    /** ***************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println(Diagnostics.unrootedTerms(kb));
        }
        catch (IOException ioe) {
            System.out.println("Error in Diagnostics.main(): IOException: " + ioe.getMessage());
        }      
    }
}
