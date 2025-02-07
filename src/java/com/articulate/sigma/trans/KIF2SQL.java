package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.io.*;
import java.util.*;

public class KIF2SQL {

/** This code is copyright Articulate Software (c) 2010.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:
 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico. See also sigmakee.sourceforge.net

 Authors :
 Adam Pease
Articulate Software
 */
/** Read and write SQL format from Sigma data structures.
 */

    public static KIF2SQL ot = new KIF2SQL();
    public static boolean initNeeded = true;
    public static boolean debug = false;
    public static int maxForDebug = 10;
    public KB kb;

    /** A map of functional statements and the automatically
     *  generated term that is created for it. */
    private HashMap functionTable = new HashMap();
    private int functionKeyNum = 0;

    private TreeMap axiomMap = new TreeMap();
    private static String termPrefix = "";

    private static int _debugLevelCounter = 0;

    private PrintWriter pw = null;

    /** ***************************************************************
     *  Remove quotes around a string
     */
    public static String removeQuotes(String s) {

        if (s == null)
            return s;
        s = s.trim();
        if (s.length() < 1)
            return s;
        if (s.length() > 1 && s.charAt(0) == '"' && s.charAt(s.length()-1) == '"')
            s = s.substring(1,s.length()-1);
        return s;
    }

    /** ***************************************************************
     * Create a new formula, inserting a new first argument that is the
     * database table foreign key that identifies the function
     */
    private Formula createNewFunction(Formula f, String funcID) {

        StringBuilder sb = new StringBuilder();
        String pred = f.getStringArgument(0);
        List<String> l = f.complexArgumentsToArrayListString(1);
        if (l == null || l.isEmpty())
            System.err.println("Error in KIF2SQL.createNewFunction(): null argument list for: " + f);
        sb.append("(").append(pred).append(" ").append(funcID);
        for (String s : l)
            sb.append(" ").append(s);
        sb.append(")");
        return new Formula(sb.toString());
    }

   /** ***************************************************************
     */
    private String processArg(String arg, int limit) {

        if (!Formula.atom(arg)) {
            Formula f = new Formula(arg);
            if (kb.isFunctional(f)) {
                System.out.println("EIF2SQL.createNewFunction(): f: " + f);
                String pred = f.getStringArgument(0);
                int arity = kb.kbCache.getArity(pred);
                String funcID = "Function" + functionKeyNum;
                f = createNewFunction(f, funcID);
                writeAxiom(f);
                functionKeyNum++;
                return funcID;
            }
            return null;
        }
        else {
            String result = arg;
            result = result.replaceAll("'", "''");
            result = removeQuotes(result);
            if (result.length() > limit)
                result = result.substring(0, limit);
            return result;
        }
    }

    /** ***************************************************************
     */
    private void writeWordNetLink(String term) {

        WordNet.wn.initOnce();
        // get list of synsets with part of speech prepended to the synset number.
        ArrayList al = (ArrayList) WordNet.wn.SUMOHash.get(term);
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {
                String synset = (String) al.get(i);
                String termMapping = null;
                // GetSUMO terms with the &% prefix and =, +, @ or [ suffix.
                switch (synset.charAt(0)) {
                    case '1': termMapping = (String) WordNet.wn.nounSUMOHash.get(synset.substring(1)); break;
                    case '2': termMapping = (String) WordNet.wn.verbSUMOHash.get(synset.substring(1)); break;
                    case '3': termMapping = (String) WordNet.wn.adjectiveSUMOHash.get(synset.substring(1)); break;
                    case '4': termMapping = (String) WordNet.wn.adverbSUMOHash.get(synset.substring(1)); break;
                }
                String rel = null;
                if (termMapping != null) {
                    switch (termMapping.charAt(termMapping.length()-1)) {
                        case '=': rel = "equivalenceRelation"; break;
                        case '+': rel = "subsumingRelation"; break;
                        case '@': rel = "instanceRelation"; break;
                        case ':': rel = "antiEquivalenceRelation"; break;
                        case '[': rel = "antiSubsumingRelation"; break;
                        case ']': rel = "antiInstanceRelation"; break;
                    }
                }
                pw.println(" INSERT INTO edges (source, rel, target) values ('" + term + "', '" + rel + "', '" + processArg("WN30-" + synset,50) + "');");
            }
        }
    }

    /** ***************************************************************
     * note that for the function tables, the first argument is a key
     * used in the original relation to point to this function
     */
    private void writeBinary(Formula form) {

        String rel = form.getStringArgument(0);
        String table = "edges";
        if (kb.isFunction(rel))
            table = "edgesFn";
        String arg1 = processArg(form.getStringArgument(1),50);
        String arg2 = processArg(form.getStringArgument(2),50);
        pw.println("INSERT INTO " + table + " (source, rel, target) values ('" + arg1 + "', '" + rel + "', '" + arg2 + "');");
    }

    /** ***************************************************************
     *  note that for the function tables, the first argument is a key
     *  used in the original relation to point to this function
     */
    private void writeTernary(Formula form) {

        String rel = form.getStringArgument(0);
        String table = "ternary";
        if (kb.isFunction(rel))
            table = "ternaryFn";
        String arg1 = processArg(form.getStringArgument(1),50);
        String arg2 = processArg(form.getStringArgument(2),50);
        String arg3 = processArg(form.getStringArgument(3),50);
        pw.println("INSERT INTO " + table + " (rel, arg1, arg2, arg3) values ('" + rel + "', '" + arg1 + "', '" + arg2 + "', '" + arg3 + "');");
    }

    /** ***************************************************************
     */
    private void writeQuaternary(Formula form) {

        String rel = form.getStringArgument(0);
        String table = "quaternary";
        if (kb.isFunction(rel))
            table = "quaternaryFn";
        String arg1 = processArg(form.getStringArgument(1),50);
        String arg2 = processArg(form.getStringArgument(2),50);
        String arg3 = processArg(form.getStringArgument(3),50);
        String arg4 = processArg(form.getStringArgument(4),50);
        pw.println("INSERT INTO " + table + " (rel, arg1, arg2, arg3, arg4) values ('" + rel + "', '" + arg1 + "', '" + arg2 + "', '" + arg3 + "', '" + arg4 + "');");
    }

    /** ***************************************************************
     */
    private void writeQuinntary(Formula form) {

        String rel = form.getStringArgument(0);
        String table = "quintary";
        if (kb.isFunction(rel))
            table = "quintaryFn";
        String arg1 = processArg(form.getStringArgument(1),50);
        String arg2 = processArg(form.getStringArgument(2),50);
        String arg3 = processArg(form.getStringArgument(3),50);
        String arg4 = processArg(form.getStringArgument(4),50);
        String arg5 = processArg(form.getStringArgument(5),50);
        pw.println("INSERT INTO " + table + " (rel, arg1, arg2, arg3, arg4, arg5) values ('" + rel + "', '" + arg1 + "', '" + arg2 + "', '" + arg3 + "', '" + arg4 +  "', '" + arg5 + "');");
    }

    /** ***************************************************************
     */
    private void writeAxiom(Formula f) {

        if (f.isRule())
            pw.println("INSERT INTO doc (lang, term, doc)  values ('SUOKIF', 'axiom-" + f.createID() + "', '" + processArg(f.getFormula(),500) + "');");
        else if (f.isSimpleClause(kb)) {
            if (f.argumentsToArrayListString(1).size() == 2)
                writeBinary(f);
            if (f.argumentsToArrayListString(1).size() == 3 && !f.getArgument(0).equals("documentation"))
                writeTernary(f);
            if (f.argumentsToArrayListString(1).size() == 4)
                writeQuaternary(f);
            if (f.argumentsToArrayListString(1).size() == 5)
                writeQuinntary(f);
        }
        else
            System.out.println("writeAxioms(): not a simple clause: " + f);
    }

    /** ***************************************************************
     */
    private void writeAxioms() {

        Iterator it = kb.formulaMap.values().iterator();
        for (Formula f : kb.formulaMap.values()) {
            System.out.println("writeAxioms(): f: " + f);
            writeAxiom(f);
        }
    }

    /** ***************************************************************
     */
    private void writeDocumentation(String term) {

        List doc = kb.askWithRestriction(0,"documentation",1,term);    // Class expressions for term.
        if (!doc.isEmpty()) {
            for (int i = 0; i < doc.size(); i++) {
                Formula form = (Formula) doc.get(i);
                String lang = form.getStringArgument(2);
                String documentation = form.getStringArgument(3);
                if (documentation != null)
                    pw.println("INSERT INTO doc (lang, term, doc) values ('" + lang + "', '" + term + "', '" + processArg(documentation,500) + "');");
            }
        }
    }

    /** ***************************************************************
     */
    public void writeSUMOTerm(String term) {

        pw.println("INSERT INTO nodes (id, label) values ('" + term + "', '" + term + "');");
    }

    /** ***************************************************************
     * Write SQL format.
     */
    public void writeKB() {

        int counter = 0;
        Set<String> kbterms = kb.getTerms();
        for (String term : kbterms) {
            if (debug && counter++ > maxForDebug)
                continue;
            writeSUMOTerm(term);
            writeDocumentation(term);
            writeWordNetLink(term);
            pw.flush();
        }
        writeAxioms();
        pw.close();
    }

    /** ***************************************************************
     */
    private void writeVerbFrames() throws IOException {

        ArrayList VerbFrames = new ArrayList(Arrays.asList("Something ----s",
                "Somebody ----s",
                "It is ----ing",
                "Something is ----ing PP",
                "Something ----s something Adjective/Noun",
                "Something ----s Adjective/Noun",
                "Somebody ----s Adjective",
                "Somebody ----s something",
                "Somebody ----s somebody",
                "Something ----s somebody",
                "Something ----s something",
                "Something ----s to somebody",
                "Somebody ----s on something",
                "Somebody ----s somebody something",
                "Somebody ----s something to somebody",
                "Somebody ----s something from somebody",
                "Somebody ----s somebody with something",
                "Somebody ----s somebody of something",
                "Somebody ----s something on somebody",
                "Somebody ----s somebody PP",
                "Somebody ----s something PP",
                "Somebody ----s PP",
                "Somebody''s (body part) ----s",
                "Somebody ----s somebody to INFINITIVE",
                "Somebody ----s somebody INFINITIVE",
                "Somebody ----s that CLAUSE",
                "Somebody ----s to somebody",
                "Somebody ----s to INFINITIVE",
                "Somebody ----s whether INFINITIVE",
                "Somebody ----s somebody into V-ing something",
                "Somebody ----s something with something",
                "Somebody ----s INFINITIVE",
                "Somebody ----s VERB-ing",
                "It ----s that CLAUSE",
                "Something ----s INFINITIVE"));
        for (int i = 0; i < VerbFrames.size(); i ++) {
            String frame = (String) VerbFrames.get(i);
            String numString = String.valueOf(i);
            if (numString.length() == 1)
                numString = "0" + numString;
            pw.println("INSERT INTO wordnet (source, rel, target) values ('#WN30VerbFrame-" + numString + "', 'frame', '" + frame + "');");
        }
    }

    /** ***************************************************************
     * Write OWL format for SUMO-WordNet mappings.
     * @param synset is a POS prefixed synset number
     */
    private void writeWordNetSynset(String synset) {

        //System.out.println("INFO in KIF2SQL.writeWordNetSynset(): " + synset);
        if (synset.startsWith("WN30-"))
            synset = synset.substring(5);
        ArrayList al = (ArrayList) WordNet.wn.synsetsToWords.get(synset);
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {
                String word = (String) al.get(i);
                pw.println("INSERT INTO wordnet (source, rel, target) values ('WN30-" + synset + "', 'word', '" + processArg(word,50) + "');");
            }
            String doc = null;
            switch (synset.charAt(0)) {
                case '1': doc = (String) WordNet.wn.nounDocumentationHash.get(synset.substring(1)); break;
                case '2': doc = (String) WordNet.wn.verbDocumentationHash.get(synset.substring(1)); break;
                case '3': doc = (String) WordNet.wn.adjectiveDocumentationHash.get(synset.substring(1)); break;
                case '4': doc = (String) WordNet.wn.adverbDocumentationHash.get(synset.substring(1)); break;
            }
            pw.println("INSERT INTO doc (lang, term, doc) values ('EnglishLanguage', 'WN30-" + synset + "', '" + processArg(doc,500) + "');");
            al = (ArrayList) WordNet.wn.relations.get(synset);
            if (al != null) {
                for (int i = 0; i < al.size(); i++) {
                    AVPair avp = (AVPair) al.get(i);
                    String rel = StringUtil.stringToKIFid(avp.attribute);
                    pw.println("INSERT INTO wordnet (source, rel, target) values ('WN30-" + synset + "', '" + rel + "', '" + avp.value + "');");
                }
            }
        }
    }

    /** ***************************************************************
     */
    private void writeWordNetExceptions() throws IOException {

        Iterator it = WordNet.wn.exceptionNounHash.keySet().iterator();
        while (it.hasNext()) {
            String plural = (String) it.next();
            String singular = (String) WordNet.wn.exceptionNounHash.get(plural);
            pw.println("INSERT INTO wordnet (source, rel, target) values ('" + processArg(plural,50) + "', 'pluralOf', '" + processArg(singular,50) + "');");
        }
        it = WordNet.wn.exceptionVerbHash.keySet().iterator();
        while (it.hasNext()) {
            String past = (String) it.next();
            String infinitive = (String) WordNet.wn.exceptionVerbHash.get(past);
            pw.println("INSERT INTO wordnet (source, rel, target) values ('" + processArg(infinitive,50) + "', 'infinitiveOf', '" + processArg(past,50) + "');");
        }
    }

    /** ***************************************************************
     */
    private void writeOneWordToSenses(String word) {

        String wordAsID = StringUtil.stringToKIFid(word);
        String wordOrPhrase = "word";
        if (word.indexOf("_") != -1)
            wordOrPhrase = "phrase";
        ArrayList senses = (ArrayList) WordNet.wn.wordsToSenseKeys.get(word);
        if (senses != null) {
            for (int i = 0; i < senses.size(); i++) {
                String sense = (String) senses.get(i);
                if (sense.length() > 50)
                    sense = sense.substring(0,50);
                pw.println("INSERT INTO wordnet (source, rel, target) values ('" + processArg(word,50) + "', 'senseKey', '" + processArg("WN30WordSense-" + sense,50) + "');");
            }
        }
        else
            System.out.println("Error in KIF2SQL.writeOneWordToSenses(): no senses for word: " + word);
    }

    /** ***************************************************************
     */
    private void writeWordsToSenses() throws IOException {

        int counter = 0;
        for (String word : WordNet.wn.wordsToSenseKeys.keySet()) {
            if (debug && counter++ > maxForDebug)
                continue;
            writeOneWordToSenses(word);
        }
    }

    /** ***************************************************************
     */
    private void writeSenseIndex() throws IOException {

        int counter = 0;
        for (String sense : WordNet.wn.senseIndex.keySet()) {
            if (debug && counter++ > maxForDebug)
                continue;
            String synset = (String) WordNet.wn.senseIndex.get(sense);
            String pos = WordNetUtilities.getPOSfromKey(sense);
            String word = WordNetUtilities.getWordFromKey(sense);
            String posNum = WordNetUtilities.posLettersToNumber(pos);
            pw.println("INSERT INTO wordnet (source, rel, target) values ('" + processArg(sense,50) + "', 'keyTosense', 'WN30-" + posNum + synset + "');");
            if (posNum.equals("2")) {
                ArrayList frames = (ArrayList) WordNet.wn.verbFrames.get(synset + "-" + word);
                if (frames != null) {
                    for (int i = 0; i < frames.size(); i++) {
                        String frame = (String) frames.get(i);
                        pw.println("INSERT INTO wordnet (source, rel, target) values ('" + processArg(sense,50) + "', 'frame', 'WN30VerbFrame-" + frame + "');");
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void writeWordNet() throws IOException {

        System.out.println("INFO in KIF2SQL.writeWordNet()");
        // Get POS-prefixed synsets.
        Iterator it = WordNet.wn.synsetsToWords.keySet().iterator();
        int counter = 0;
        for (String synset : WordNet.wn.synsetsToWords.keySet()) {
            if (!debug || counter++ < maxForDebug)
                writeWordNetSynset(synset);
        }
        writeWordNetExceptions();
        writeVerbFrames();
        writeWordsToSenses();
        writeSenseIndex();
    }

    /** ***************************************************************
     */
    public static void initOnce(String kbName) {

        if (ot.kb == null || !kbName.equals(ot.kb.name))
            initNeeded = true;
        if (initNeeded == true) {
            initNeeded = false;
            ot.kb = KBmanager.getMgr().getKB(kbName);
            // ot.createAxiomMap();
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KIF2SQL translator class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        System.out.println("  -s - translate and write SQL version of kb to " + kbName + ".sql");
    }

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) throws IOException {

        if (args != null && args.length > 0 && args[0].equals("-h")) {
            showHelp();
        }
        else {
            System.out.println("INFO in KIF2SQL.main()");
            KBmanager.getMgr().initializeOnce();
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            String path = kbDir + File.separator + kbName + ".sql";
            System.out.println("INFO in KIF2SQL.write(): writing " + path);
            FileWriter fw = new FileWriter(path);

            System.out.println("KIF2SQL.main(): completed initialization");
            if (args != null && args.length > 0 && args[0].equals("-s")) {
                KIF2SQL ot = new KIF2SQL();
                ot.pw = new PrintWriter(fw);
                try {
                    System.out.println("KIF2SQL.main(): starting translation");
                    ot.kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                    //ot.writeWordNet(pw);
                    ot.writeKB();
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
            else
                showHelp();
            System.out.println("KIF2SQL.main(): finished");
        }
    }
}

