package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;

import java.io.*;
import java.util.*;

public class SUMOKBtoTPTPKB {

    public static final boolean filterSimpleOnly = false;

    public KB kb;

    private static boolean debug = false;

    /** *************************************************************
     * This method translates the entire KB to TPTP format, storing
     * the translation for each Formula in the List identified by the
     * private member Formula.theTptpFormulas.  Use
     * Formula.getTheTptpFormulas() to accesss the TPTP sentences
     * (Strings) that constitute the translation for a single SUO-KIF
     * Formula.
     *
     * @return An int indicating the number of Formulas that were
     * successfully translated.
     */
    public int tptpParse() {

        int goodCount = 0;
        try {
            ArrayList<Formula> badList = new ArrayList<Formula>();
            Iterator<Formula> it = kb.formulaMap.values().iterator();
            while (it.hasNext()) {
                Formula f = it.next();
                SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                stptp._f = f;
                stptp.tptpParse(f,false, kb);
                if (f.getTheTptpFormulas().isEmpty()) {
                    if (badList.size() < 11)
                        badList.add(f);
                }
                else
                    goodCount++;
            }
        }
        catch (Exception ex) {
            System.out.println("Error in SUMOKBtoTPTPKB.tptpParse(): " + ex.getMessage());
            ex.printStackTrace();
        }
        return goodCount;
    }

    /** *************************************************************
     */
    public String copyFile (String fileName) {

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
                                                     String sanitizedKBName, int axiomIndex, boolean onlyPlainFOL) {

        Iterator<String> it = relationMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = relationMap.get(key);
            ArrayList<Formula> result = kb.ask("arg",1,value);
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    Formula f = result.get(i);
                    String s = f.theFormula.replace(value,key);
                    if (!onlyPlainFOL) {
                        pr.println("fof(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                                ",axiom,(" + SUMOformulaToTPTPformula.tptpParseSUOKIFString(s, false) + ")).");
                    }
                    else {
                    //    pr.println("%FOL fof(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                    //            ",axiom,(" + SUMOformulaToTPTPformula.tptpParseSUOKIFString(s, false) + ")).");
                    }
                }
            }
        }
    }

    /** *************************************************************
     *  Sets reasoner and calls writeTPTPFile() below
     */
    public String writeTPTPFile(String fileName,
                                boolean onlyPlainFOL) {

        final String reasoner = "EProver";
        return writeTPTPFile(fileName,onlyPlainFOL,
                reasoner);
    }

    /** *************************************************************
     *  Sets conjecture and calls writeTPTPFile() below
     */
    public String writeTPTPFile(String fileName,
                                boolean onlyPlainFOL,
                                String reasoner) {

        final Formula conjecture = null;
        return writeTPTPFile(fileName,conjecture,onlyPlainFOL,
                reasoner);
    }

    /** *************************************************************
     *  Sets isQuestion and calls writeTPTPFile() below
     */
    public String writeTPTPFile(String fileName,
                                Formula conjecture,
                                boolean onlyPlainFOL,
                                String reasoner) {

        final boolean isQuestion = false;
        return writeTPTPFile(fileName,conjecture,onlyPlainFOL,
                reasoner,isQuestion);
    }

    /** *************************************************************
     *  Sets pw and calls writeTPTPFile() below
     */
    public String writeTPTPFile(String fileName,
                                Formula conjecture,
                                boolean onlyPlainFOL,
                                String reasoner,
                                boolean isQuestion) {

        final PrintWriter pw = null;
        return writeTPTPFile(fileName,conjecture,onlyPlainFOL,
                reasoner,isQuestion,pw);
    }

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
     *  Write all axioms in the KB to TPTP format.
     *
     * @param fileName - the full pathname of the file to write
     */
    public String writeTPTPFile(String fileName, Formula conjecture, boolean onlyPlainFOL,
                                String reasoner, boolean isQuestion, PrintWriter pw) {

        if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeTPTPFile()");

        ArrayList<String> alreadyWrittenTPTPs = new ArrayList<String>();
        HashSet<String> excludedPredicates = buildExcludedPredicates();
        String result = null;
        PrintWriter pr = null;
        try {
            File outputFile;
            int axiomIndex = 1;   // a count appended to axiom names to make a unique ID
            TreeMap<String,String> relationMap = new TreeMap<String,String>(); // A Map of variable arity relations keyed by new name
            String sanitizedKBName = kb.name.replaceAll("\\W","_");
            //----If file name is a directory, create filename therein
            if (fileName == null) {
                outputFile = File.createTempFile(sanitizedKBName, ".p", null);
                //----Delete temp file when program exits.
                outputFile.deleteOnExit();
            }
            else
                outputFile = new File(fileName);
            String canonicalPath = outputFile.getCanonicalPath();
            if (pw instanceof PrintWriter)
                pr = pw;
            else
                pr = new PrintWriter(new FileWriter(outputFile));
            // If a PrintWriter object is passed in, we suppress this
            // copyright notice and assume that such a notice will be
            // provided somewhere is the wider calling context.
            if (pw == null) {
                pr.println("% Articulate Software");
                pr.println("% www.ontologyportal.org www.articulatesoftware.com");
                pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
                pr.println("% This is a translation to TPTP of KB " + sanitizedKBName);
                pr.println("");
            }

            OrderedFormulae orderedFormulae = new OrderedFormulae();
            orderedFormulae.addAll(kb.formulaMap.values());
            if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeTPTPFile(): added formulas: " + orderedFormulae.size());
            List<String> tptpFormulas = null;
            String oldSourceFile = "";
            String sourceFile = "";
            File sf = null;
            Iterator<Formula> ite = orderedFormulae.iterator();
            String theTPTPFormula = null;
            int counter = 0;
            while (ite.hasNext()) {
                Formula f = ite.next();
                counter++;
                if (counter == 100) {
                    System.out.print(".");
                    counter = 0;
                }
                sf = new File(f.sourceFile);
                sourceFile = sf.getName();
                sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf("."));
                //if (!sourceFile.equals(oldSourceFile))
                //    kb.warnings.add("Source file has changed to " + sourceFile);
                oldSourceFile = sourceFile;
                tptpFormulas = f.getTheTptpFormulas();

                Formula tmpF = new Formula();
                tmpF.read(f.theFormula);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                List<Formula> processed = fp.preProcess(tmpF,false, kb);
                if (!processed.isEmpty()) {
                    ArrayList<Formula> withRelnRenames = new ArrayList<Formula>();
                    Iterator<Formula> procit = processed.iterator();
                    while (procit.hasNext()) {
                        Formula f2 = procit.next();
                        withRelnRenames.add(f2.renameVariableArityRelations(kb,relationMap));
                    }
                    SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                    stptp._f = tmpF;
                    stptp.tptpParse(tmpF,false, kb, withRelnRenames);
                    tptpFormulas = tmpF.getTheTptpFormulas();
                    f.theTptpFormulas.addAll(tptpFormulas);
                }
                if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeTPTPFile(): formula: " + f);
                if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeTPTPFile(): tptp formulas: " + f.theTptpFormulas);

                Iterator<String> tptpIt = tptpFormulas.iterator();
                while (tptpIt.hasNext()) {
                    theTPTPFormula = tptpIt.next();
                    if (onlyPlainFOL) {   //----Remove interpretations of arithmetic
                        theTPTPFormula = theTPTPFormula
                                .replaceAll("[$]less","dollar_less")
                                .replaceAll("[$]greater","dollar_greater")
                                .replaceAll("[$]time","dollar_times")
                                .replaceAll("[$]divide","dollar_divide")
                                .replaceAll("[$]plus","dollar_plus")
                                .replaceAll("[$]minus","dollar_minus");
                        //----Don't output ""ed ''ed and numbers
                        if (theTPTPFormula.matches(".*'[a-z][a-zA-Z0-9_]*\\(.*") ||
                                theTPTPFormula.indexOf("'") > -1
                                || theTPTPFormula.indexOf('"') >= 0)
                            //pr.print("%FOL ");
                            continue;
                        if (reasoner.matches(".*(?i)Equinox.*")
                                && f.theFormula.indexOf("equal") > 2) {
                            Formula f2 = new Formula();
                            f2.read(f.cdr());
                            f2.read(f.car());
                            if (f2.theFormula.equals("equal"))
                                //pr.print("%FOL ");
                                continue;
                        }
                    }

                    if (filterExcludePredicates(excludedPredicates, f) == false) {
                        if (!alreadyWrittenTPTPs.contains(theTPTPFormula)) {
                            pr.print("fof(kb_" + sanitizedKBName + "_" + axiomIndex++);
                            pr.println(",axiom,(" + theTPTPFormula + ")).");
                            alreadyWrittenTPTPs.add(theTPTPFormula);
                        }
                    }
                }
                pr.flush();
                if (f.getTheTptpFormulas().isEmpty()) {
                    String addErrStr = "No TPTP formula for:" + f.theFormula;
                    kb.warnings.add(addErrStr);
                }
            }
            System.out.println();
            printVariableArityRelationContent(pr,relationMap,sanitizedKBName,axiomIndex,onlyPlainFOL);
            if (conjecture != null) {  //----Print conjecture if one has been supplied
                // conjecture.getTheTptpFormulas() should return a
                // List containing only one String, so the iteration
                // below is probably unnecessary
                String type = "conjecture";
                if (isQuestion) type = "question";
                Iterator<String> tptpIt = conjecture.getTheTptpFormulas().iterator();
                while (tptpIt.hasNext()) {
                    theTPTPFormula = (String) tptpIt.next();
                    pr.println("fof(prove_from_" + sanitizedKBName + "," + type + ",(" + theTPTPFormula + ")).");
                }
            }
            result = canonicalPath;
        }
        catch (Exception ex) {
            System.out.println("Error in SUMOKBtoTPTPKB.writeTPTPfile(): " + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                //kb.kbCache.clearSortalTypeCache();
                if (pr != null) pr.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return result;
    }

    /** *************************************************************
     * define a set of predicates which will not be used for inference
     */
    public static HashSet<String> buildExcludedPredicates() {

        HashSet<String> excludedPredicates = new HashSet<>();
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
     * @return true if the given formula is simple clause,
     *   and contains one of the excluded predicates;
     * otherwise return false;
     */
    public boolean filterExcludePredicates(HashSet<String> excludedPredicates, Formula formula) {

        boolean pass = false;
        if (formula.isSimpleClause(kb))
            pass = excludedPredicates.contains(formula.getArgument(0));
        return pass;
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        //debug = true;
        KBmanager.getMgr().initializeOnce();
                /* not needed since initialization will create the tptp file
        SUMOKBtoTPTPKB skbtptpkb = new SUMOKBtoTPTPKB();

        skbtptpkb.kb = KBmanager.getMgr().getKB("SUMO");
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tptp";
        String fileWritten = skbtptpkb.writeTPTPFile(filename, null, true, "none");
        if (StringUtil.isNonEmptyString(fileWritten))
            System.out.println("File written: " + fileWritten);
        else
            System.out.println("Could not write " + filename);
        return;
        */
    }
}
