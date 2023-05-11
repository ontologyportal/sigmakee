package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

public class SUMOKBtoTPTPKB {

    public static final boolean filterSimpleOnly = false;

    public KB kb;

    // flags to support including numbers and HOL in pseudo-FOL for flexible provers
    public static boolean removeHOL = true; // remove higher order expressions
    public static boolean removeNum = true; // remove numbers

    public static boolean debug = false;

    public static String lang = "fof"; // or thf

    public static HashSet<String> excludedPredicates = new HashSet<>();

    public Set<String> alreadyWrittenTPTPs = new HashSet<>();

    // maps TPTP axiom IDs to SUMO formulas
    public static HashMap<String,Formula> axiomKey = new HashMap<>();

    /** *************************************************************
     */
    public SUMOKBtoTPTPKB() {

        buildExcludedPredicates();
    }

    /** *************************************************************
     * define a set of predicates which will not be used for inference
     */
    public static HashSet<String> buildExcludedPredicates() {

        excludedPredicates.add("documentation");
        excludedPredicates.add("domain");
        excludedPredicates.add("format");
        excludedPredicates.add("termFormat");
        excludedPredicates.add("externalImage");
        excludedPredicates.add("relatedExternalConcept");
        excludedPredicates.add("relatedInternalConcept");
        excludedPredicates.add("formerName");
        excludedPredicates.add("abbreviation");
        excludedPredicates.add("conventionalShortName");
        excludedPredicates.add("conventionalLongName");

        return excludedPredicates;
    }

    /** *************************************************************
     */
    public String getSanitizedKBname() {

        String sanitizedKBName = kb.name.replaceAll("\\W","_");
        return sanitizedKBName;
    }

    /** *************************************************************
     */
    public static String langToExtension(String l) {

        if (l.equals("fof"))
            return "tptp";
        return l;
    }

    /** *************************************************************
     */
    public static String extensionToLang(String l) {

        if (l.equals("tptp"))
            return "fof";
        return l;
    }

    /** *************************************************************
     */
    public String getInfFilename() {

        String sanitizedKBName = getSanitizedKBname();
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator +
                sanitizedKBName + "." + langToExtension(lang);
        return filename;
    }

    /** *************************************************************
     */
    public String copyFile(String fileName) {

        String outputPath = "";
        FileReader in = null;
        FileWriter out = null;
        try {
            String sanitizedKBName = kb.name.replaceAll("\\W","_");
            File inputFile = new File(fileName);
            File outputFile = File.createTempFile(sanitizedKBName, ".p", null);
            outputPath = outputFile.getCanonicalPath();
            in = new FileReader(inputFile);
            out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1)
                out.write(c);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            }
            catch (Exception ieo) {
                ieo.printStackTrace();
            }
        }
        return outputPath;
    }

    /** *************************************************************
     */
    public static void addToFile (String fileName, ArrayList<String> axioms, String conjecture) {

        DataOutputStream out = null;
        try {
            boolean append = true;
            FileOutputStream file = new FileOutputStream(fileName, append);
            out = new DataOutputStream(file);
            if (axioms != null) {   // add axioms
                for (String axiom : axioms)
                    out.writeBytes(axiom);
                out.flush();
            }
            if (StringUtil.isNonEmptyString(conjecture)) {  // add conjecture
                out.writeBytes(conjecture);
                out.flush();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (out != null) out.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return;
    }

    /** ***************************************************************
     * @param relationMap is a Map of String keys and values where
     *                    the key is the renamed relation and the
     *                    value is the original name.
     */
    protected void printVariableArityRelationContent(PrintWriter pr, TreeMap<String,String> relationMap,
                                                     String sanitizedKBName, int axiomIndex) {

        Iterator<String> it = relationMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = relationMap.get(key);
            ArrayList<Formula> result = kb.ask("arg",1,value);
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    Formula f = result.get(i);
                    String s = f.getFormula().replace(value,key);
                    pr.println(lang + "(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                            ",axiom,(" + SUMOformulaToTPTPformula.tptpParseSUOKIFString(s, false) + ")).");
                }
            }
        }
    }
    /** ***************************************************************
     * Print the sorts of any numeric constants encountered during processing.
     * They are stored in SUMOtoTFAform.numericConstantTypes
     */
    public void printTFFNumericConstants(PrintWriter pr) {

        int size = SUMOtoTFAform.numericConstantTypes.keySet().size();
        if (size == SUMOtoTFAform.numericConstantCount)
            return;
        for (String t : SUMOtoTFAform.numericConstantTypes.keySet()) {
            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
                continue;
            String type = SUMOtoTFAform.numericConstantTypes.get(t);
            if (debug) System.out.println("SUMOKBtoTPTPKB.printTFFNumericConstants(): term, type: " + t + ", " + type);
            pr.println("tff(" + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)  +
                    "_sig,type," + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)  +
                    ":" + SUMOKBtoTFAKB.translateSort(kb,type) + ").");
        }
        for (String t : SUMOtoTFAform.numericConstantTypes.keySet()) {
            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
                continue;
        }
    }

    /** *************************************************************
     *  Sets isQuestion and calls writeTPTPFile() below

    public String writeFile(String fileName,
                                Formula conjecture) {

        final boolean isQuestion = false;
        return writeFile(fileName,conjecture,isQuestion);
    }
*/
    /** *************************************************************
     *  Sets pw and calls writeTPTPFile() below

    public String writeFile(String fileName,
                                Formula conjecture,
                                boolean isQuestion) {

        final PrintWriter pw = null;
        return writeFile(fileName,conjecture,isQuestion,pw);
    }
*/
    /** *************************************************************
     */
    public class OrderedFormulae extends TreeSet<Formula> {

        public int compare(Object o1, Object o2) {
            Formula f1 = (Formula) o1;
            Formula f2 = (Formula) o2;
            int fileCompare = f1.sourceFile.compareTo(f2.sourceFile);
            if (fileCompare == 0) {
                fileCompare = (Integer.valueOf(f1.startLine))
                        .compareTo(Integer.valueOf(f2.startLine));
                if (fileCompare == 0) {
                    fileCompare = (Long.valueOf(f1.endFilePosition))
                            .compareTo(Long.valueOf(f2.endFilePosition));
                }
            }
            return fileCompare;
        }
    }

    /** *************************************************************
     */
    public void writeHeader(PrintWriter pw, String sanitizedKBName) {

        if (pw == null) {
            pw.println("% Articulate Software");
            pw.println("% www.ontologyportal.org www.articulatesoftware.com");
            pw.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pw.println("% This is a translation to TPTP of KB " + sanitizedKBName);
            pw.println("");
        }
    }

    /** *************************************************************
     *  Write all axioms in the KB to TPTP format.
     *
     * @param fileName - the full pathname of the file to write
     */
    public String writeFile(String fileName, Formula conjecture,
                            boolean isQuestion, PrintWriter pw) {

        PredVarInst.init();
        long millis = System.currentTimeMillis();
        if (!KBmanager.initialized) {
            System.out.println("Error in SUMOKBtoTPTPKB.writeFile(): KB initialization not completed");
            return "Error in SUMOKBtoTPTPKB.writeFile(): KB initialization not completed";
        }
        String result = null;
        try {
            int axiomIndex = 1;   // a count appended to axiom names to make a unique ID
            TreeMap<String,String> relationMap = new TreeMap<String,String>(); // A Map of variable arity relations keyed by new name
            writeHeader(pw,fileName);

            OrderedFormulae orderedFormulae = new OrderedFormulae();
            orderedFormulae.addAll(kb.formulaMap.values());
            //if (debug) pr.println("% INFO in SUMOKBtoTPTPKB.writeFile(): added formulas: " + orderedFormulae.size());
            int counter = 0;
            int formCount = 0;
            for (Formula f : orderedFormulae) {
                f.theTptpFormulas = new HashSet<>();
                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : source line: " + f.startLine);
                if (!f.getFormula().startsWith("(documentation")) {
                    pw.println("% f: " + f.format("", "", " "));
                    if (f.derivation.parents.size() > 0) {
                        for (Formula derivF : f.derivation.parents)
                            pw.println("% original f: " + derivF.format("", "", " "));
                    }
                    pw.println("% " + formCount++ + " of " + orderedFormulae.size() +
                            " from file " + f.sourceFile + " at line " + f.startLine);
                }
                if (f.isHigherOrder(kb)) {
                    pw.println("% is higher order");
                    if (removeHOL)
                        continue;
                }
                else
                    pw.println("% not higher order");
                if (!KBmanager.getMgr().prefEquals("cache","yes") && f.isCached())
                    continue;
                if (counter++ == 100) System.out.print(".");
                if ((counter % 4000) == 1)
                    System.out.println("\nSUMOKBtoTPTPKB.writeFile() : still working");
                FormulaPreprocessor fp = new FormulaPreprocessor();
                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : process: " + f);
                Set<Formula> processed = fp.preProcess(f,false,kb);
                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : processed: " + processed);
                if (!processed.isEmpty()) {
                    HashSet<Formula> withRelnRenames = new HashSet<Formula>();
                    for (Formula f2 : processed)
                        withRelnRenames.add(f2.renameVariableArityRelations(kb,relationMap));
                    for (Formula f3 : withRelnRenames) {
                        if (lang.equals("fof")) {
                            SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula(lang);
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tptp input: " + f3.format("",""," "));
                            result = stptp.tptpParseSUOKIFString(f3.getFormula(), false);
                            if (debug) System.out.println("% INFO in SUMOKBtoTPTPKB.writeFile(): result: " + result);
                            if (result != null)
                                f.theTptpFormulas.add(result);
                        }
                        else if (lang.equals("tff")) {
                            SUMOtoTFAform stfa = new SUMOtoTFAform();
                            SUMOtoTFAform.kb = kb;
                            pw.println("% tff input: " + f3.format("",""," "));
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tff input: " + f3.format("",""," "));
                            stfa.sorts = stfa.missingSorts(f3);
                            if (stfa.sorts != null && stfa.sorts.size() > 0)
                                f3.tffSorts.addAll(stfa.sorts);
                            result = stfa.process(f3.getFormula(),false);
                            printTFFNumericConstants(pw);
                            SUMOtoTFAform.initNumericConstantTypes();
                            if (!StringUtil.emptyString(result))
                                f.theTptpFormulas.add(result);
                            else
                                if (!StringUtil.emptyString(SUMOtoTFAform.filterMessage))
                                    pw.println("% " + SUMOtoTFAform.filterMessage);
                        }
                        else
                            pw.println("% unhandled language option " + lang);
                    }
                }
                else {
                    //System.out.println("SUMOKBtoTPTPKB.writeFile() : % empty result from preprocess on " + f.getFormula().replace("\\n"," "));
                    pw.println("% empty result from preprocess on " + f.getFormula().replace("\\n"," "));
                }
                for (String sort : f.tffSorts) {
                    if (!StringUtil.emptyString(sort) &&
                            !alreadyWrittenTPTPs.contains(sort)) {
                        String name = "kb_" + getSanitizedKBname() + "_" + axiomIndex++;
                        axiomKey.put(name,f);
                        pw.print(lang + "(" + name);
                        pw.println(",axiom,(" + sort + ")).");
                        alreadyWrittenTPTPs.add(sort);
                    }
                }
                for (String theTPTPFormula : f.theTptpFormulas) {
                    if (!StringUtil.emptyString(theTPTPFormula) &&
                            !alreadyWrittenTPTPs.contains(theTPTPFormula) &&
                            !filterAxiom(f,theTPTPFormula,pw)) {
                        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : writing " + theTPTPFormula);
                        String name = "kb_" + getSanitizedKBname() + "_" + axiomIndex++;
                        axiomKey.put(name,f);
                        pw.print(lang + "(" + name);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : finished writing " + theTPTPFormula + " with name " + name);
                        alreadyWrittenTPTPs.add(theTPTPFormula);
                    }
                    else
                        pw.println("% empty, already written or filtered formula, skipping : " + theTPTPFormula);
                }
            }
            System.out.println();
            printVariableArityRelationContent(pw,relationMap,getSanitizedKBname(),axiomIndex);
            printTFFNumericConstants(pw);
            if (conjecture != null) {  //----Print conjecture if one has been supplied
                // conjecture.getTheTptpFormulas() should return a
                // List containing only one String, so the iteration
                // below is probably unnecessary
                String type = "conjecture";
                if (isQuestion) type = "question";
                for (String theTPTPFormula : conjecture.theTptpFormulas)
                    pw.println(lang + "(prove_from_" + getSanitizedKBname() + "," + type + ",(" + theTPTPFormula + ")).");
            }
            result = getInfFilename();
            pw.flush();
            pw.close();
        }
        catch (Exception ex) {
            System.out.println("Error in SUMOKBtoTPTPKB.writeFile(): " + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                //kb.kbCache.clearSortalTypeCache();
                if (pw != null) {
                    pw.flush();
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        kb.axiomKey = axiomKey;
        KBmanager.getMgr().serialize();
        System.out.println("SUMOKBtoTPTPKB.writeFile(): axiomKey: " + axiomKey.size());
        KB.axiomKey = axiomKey;
        System.out.println("SUMOKBtoTPTPKB.writeFile(): seconds: " + (System.currentTimeMillis() - millis) / 1000);
        return result;
    }

    /** *************************************************************
     * @return true if the given formula is simple clause,
     *   and contains one of the excluded predicates;
     * otherwise return false;
     */
    public boolean filterExcludePredicates(Formula formula) {

        boolean pass = false;
        if (formula.isSimpleClause(kb))
            pass = excludedPredicates.contains(formula.getArgument(0));
        return pass;
    }

    /** *************************************************************
     */
    public boolean filterAxiom(Formula form, String tptp, PrintWriter pw) {

        //----Don't output ""ed ''ed and numbers
        if (tptp.matches(".*'[a-z][a-zA-Z0-9_]*\\(.*") &&
                this.getClass().equals(SUMOKBtoTPTPKB.class)) { // only filter numbers in TPTP, not TFF
            pw.println("% number: " + tptp);
            if (removeNum)
                return true;
            else
                return false;
        }
        if (tptp.indexOf("'") > -1 || tptp.indexOf('"') >= 0) {
            pw.println("% f: " + form.format("", "", " "));
            pw.println("% quoted thing");
            return true;
        }

        if (form.isHigherOrder(kb))
            if (removeHOL)
                return true;
        if (filterExcludePredicates(form) == false) {
            if (!alreadyWrittenTPTPs.contains(tptp)) {
                //pw.println("% not already written: " + tptp);
                return false;
            }
            else {
                pw.println("% already written: " + tptp);
                return true;
            }
        }
        else {
            pw.println("% filtered predicate: " + form.getArgument(0));
            return true;
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        //debug = true;
        KBmanager.getMgr().initializeOnce();

        SUMOKBtoTPTPKB skbtptpkb = new SUMOKBtoTPTPKB();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        skbtptpkb.kb = KBmanager.getMgr().getKB(kbName);
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + SUMOKBtoTPTPKB.lang;
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        String fileWritten = skbtptpkb.writeFile(filename, null, false, pw);
        if (StringUtil.isNonEmptyString(fileWritten))
            System.out.println("File written: " + fileWritten + " with key: " + axiomKey);
        else
            System.out.println("Could not write " + filename);
    }
}
