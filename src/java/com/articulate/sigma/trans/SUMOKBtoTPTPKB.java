package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SUMOKBtoTPTPKB {

    public static final boolean FILTER_SIMPLE_ONLY = false;

    public KB kb;

    // flags to support including numbers and HOL in pseudo-FOL for flexible provers
    public static boolean removeHOL = true; // remove higher order expressions
    public static boolean removeNum = true; // remove numbers
    public static boolean removeStrings = true;

    /** Flag to enable rapid parsing via multiple threads coordinated by an ExecutorService */
    public static boolean rapidParsing = true;

    public static boolean debug = false;

    public static String lang = "fof"; // or thf

    public static boolean CWA = false;  // implement the closed world assumption

    public static Set<String> excludedPredicates = new HashSet<>();

    // maps TPTP axiom IDs to SUMO formulas
    public static Map<String,Formula> axiomKey = new HashMap<>();

    public Set<String> alreadyWrittenTPTPs = new HashSet<>();

    /** Progress bar text capture */
    private final StringBuilder progressSb = new StringBuilder();

    /** *************************************************************
     */
    public SUMOKBtoTPTPKB() {
        buildExcludedPredicates();
    }

    /** *************************************************************
     * define a set of predicates which will not be used for inference
     */
    public static Set<String> buildExcludedPredicates() {

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

        return kb.name.replaceAll("\\W","_");
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
        return KBmanager.getMgr().getPref("kbDir") + File.separator +
                sanitizedKBName + "." + langToExtension(lang);
    }

    /** *************************************************************
     */
    public String copyFile(String fileName) {

        String outputPath = "";
        try {
            String sanitizedKBName = getSanitizedKBname();
            File inputFile = new File(fileName);
            File outputFile = File.createTempFile(sanitizedKBName, ".p", null);
            outputPath = outputFile.getCanonicalPath();
            try (Reader in = new FileReader(inputFile);
                 Writer out = new FileWriter(outputFile)) {
                int c;
                while ((c = in.read()) != -1)
                    out.write(c);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return outputPath;
    }

    /** *************************************************************
     */
    public static void addToFile(String fileName, List<String> axioms, String conjecture) {

        boolean append = true;
        try (OutputStream file = new FileOutputStream(fileName, append);
             DataOutputStream out = new DataOutputStream(file)) {
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
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     * @param relationMap is a Map of String keys and values where
     *                    the key is the renamed relation and the
     *                    value is the original name.
     */
    protected void printVariableArityRelationContent(PrintWriter pr, Map<String,String> relationMap,
                                                     String sanitizedKBName, AtomicInteger axiomIndex) {

        Iterator<String> it = relationMap.keySet().iterator();
        String key, value;
        List<Formula> result;
        String s;
        while (it.hasNext()) {
            key = it.next();
            value = relationMap.get(key);
            result = kb.ask("arg",1,value);
            if (result != null) {
                for (Formula f : result) {
                    s = f.getFormula().replace(value,key);
                    pr.println(lang + "(kb_" + sanitizedKBName + "_" + axiomIndex.getAndIncrement() +
                            ",axiom,(" + SUMOformulaToTPTPformula.tptpParseSUOKIFString(s, false) + ")).");
                }
            }
        }
    }

    /** ***************************************************************
     * Print the sorts of any numeric constants encountered during processing.
     * They are stored in SUMOtoTFAform.numericConstantTypes
     */
    public void printTFFNumericConstants(PrintWriter pw) {

        int size = SUMOtoTFAform.numericConstantTypes.keySet().size();
        if (size == SUMOtoTFAform.numericConstantCount)
            return;
        String type;
        for (String t : SUMOtoTFAform.numericConstantTypes.keySet()) {
            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
                continue;
            type = SUMOtoTFAform.numericConstantTypes.get(t);
            if (debug) System.out.println("SUMOKBtoTPTPKB.printTFFNumericConstants(): term, type: " + t + ", " + type);
            pw.println("tff(" + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)  +
                    "_sig,type," + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)  +
                    ":" + SUMOKBtoTFAKB.translateSort(kb,type) + ").");
        }
//        for (String t : SUMOtoTFAform.numericConstantTypes.keySet()) {
//            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
//                continue;
//        }
    }

    /** ***************************************************************
     * Print the sorts of any numeric constants encountered during processing.
     * They are stored in SUMOtoTFAform.numericConstantTypes
     */
    public synchronized void printTFFNumericConstants(List<String> fileContents) {

        int size = SUMOtoTFAform.numericConstantTypes.keySet().size();
        if (size == SUMOtoTFAform.numericConstantCount)
            return;
        String type;
        for (String t : SUMOtoTFAform.numericConstantTypes.keySet()) {
            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
                continue;
            type = SUMOtoTFAform.numericConstantTypes.get(t);
            if (debug) System.out.println("SUMOKBtoTPTPKB.printTFFNumericConstants(): term, type: " + t + ", " + type);
            fileContents.add("tff(" + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)  +
                    "_sig,type," + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)  +
                    ":" + SUMOKBtoTFAKB.translateSort(kb,type) + ").");
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
                        .compareTo(f2.startLine);
                if (fileCompare == 0) {
                    fileCompare = (Long.valueOf(f1.endFilePosition))
                            .compareTo(f2.endFilePosition);
                }
            }
            return fileCompare;
        }
    }

    /** *************************************************************
     */
    public void writeHeader(PrintWriter pw, String sanitizedKBName) {

        if (pw != null) {
            pw.println("% Articulate Software");
            pw.println("% www.ontologyportal.org www.articulatesoftware.com");
            pw.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pw.println("% This is a translation to TPTP of KB " + sanitizedKBName);
            pw.println("");
        }
    }

    private final AtomicInteger axiomIndex = new AtomicInteger(1); // a count appended to axiom names to make a unique ID
    private final AtomicInteger formCount = new AtomicInteger(0);
    private final AtomicInteger idxCount = new AtomicInteger(1);
    private int counter = 0;
    private long millis = 0L;

    /** *************************************************************
     *  Write all axioms in the KB to TPTP format.
     *
     * @param fileName - the full pathname of the file to write
     * @param conjecture a conjecture to query the KB with if any
     * @param isQuestion flag to denote a question being posed
     * @param pw the PrintWriter to write TPTP with
     *
     * @return the name of the KB translation to TPTP file
     */
    public String writeFile(String fileName, Formula conjecture,
                            boolean isQuestion, PrintWriter pw) {

        // DEBUG
        System.out.println("sigma.tff.profile=" + System.getProperty("sigma.tff.profile"));

        PredVarInst.init();
        millis = System.currentTimeMillis();
        if (!KBmanager.initialized) {
            String msg = "Error in SUMOKBtoTPTPKB.writeFile(): KB initialization not completed";
            System.err.println(msg);
            return msg;
        }

        // PROFILER
        if (PROFILE_TFF) {
            Runtime rt = Runtime.getRuntime();
            System.out.printf("TFF_PROFILE_ENV: maxMB=%.1f totalMB=%.1f freeMB=%.1f processors=%d%n",
                    rt.maxMemory() / 1024.0 / 1024.0,
                    rt.totalMemory() / 1024.0 / 1024.0,
                    rt.freeMemory() / 1024.0 / 1024.0,
                    rt.availableProcessors());
        }

        String retVal = null;
        try {
            // (orig) sequential processing
            if (!rapidParsing)
                retVal = _writeFile(fileName, conjecture, isQuestion, pw);
            else {
                /* Experimental threading of main loop writes big SUMO in half
                 * the time as the sequential method. 2/17/25 tdn
                 */
                retVal = _tWriteFile(fileName, conjecture, isQuestion, pw);
            }

        }
        catch (Exception ex) {
            System.err.println("Error in SUMOKBtoTPTPKB.writeFile(): " + ex.getMessage());
            ex.printStackTrace();
            return retVal;
        }

        KB.axiomKey = axiomKey;
        // Skip serialization during background generation - TPTPGenerationManager handles it
//        if (!TPTPGenerationManager.isBackgroundGenerating()) {
//            System.out.println("SUMOKBtoTPTPKB.writeFile(): serializing KB after writing");
//            KBmanager.serialize();
//        }
        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile(): axiomKey: " + axiomKey.size());
        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile(): seconds: " + (System.currentTimeMillis() - millis) / KButilities.ONE_K);

        axiomIndex.set(1); // reset
        counter = 0; // reset
        formCount.set(0); // reset
        millis = 0L; // reset
        idxCount.set(1); // reset

        return retVal;
    }

    /** *************************************************************
     * Conventional/sequential version
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    private String _writeFile(String fileName, Formula conjecture,
                            boolean isQuestion, PrintWriter pw) {

        Map<String,String> relationMap = new TreeMap<>(); // A Map of variable arity relations keyed by new name
        writeHeader(pw,fileName);

        OrderedFormulae orderedFormulae = new OrderedFormulae();
        orderedFormulae.addAll(kb.formulaMap.values());
        //if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): added formulas: " + orderedFormulae.size());

        int total = orderedFormulae.size();
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Formula> processed, withRelnRenames;
        String result, name;
        SUMOtoTFAform stfa;
        for (Formula f : orderedFormulae) {

            // Skip Formulas that start with "documentation, termFormat, format""
            if (isNonReasoningForATP(f.getFormula())) continue;

            // Format-specific cache clearing to prevent FOF/TFF overwrites
            if (lang.equals("fof")) {
                f.theFofFormulas.clear();
            } else if (lang.equals("tff")) {
                f.theTffFormulas.clear();
            }
            f.theTptpFormulas.clear(); // Legacy compatibility
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : source line: " + f.startLine);
            if (!f.getFormula().startsWith("(documentation")) {
                pw.println("% f: " + f.format("", "", Formula.SPACE));
                if (!f.derivation.parents.isEmpty()) {
                    for (Formula derivF : f.derivation.parents)
                        pw.println("% original f: " + derivF.format("", "", Formula.SPACE));
                }
                pw.println("% " + formCount.getAndIncrement() + " of " + total +
                        " from file " + f.sourceFile + " at line " + f.startLine);
            }
            if (f.isHigherOrder(kb)) {
                pw.println("% is higher order");
                if (lang.equals("thf")) {  // TODO create a flag for adding modals (or not)
                    f = Modals.processModals(f,kb);
                }
                if (removeHOL)
                    continue;
            }
            else
                pw.println("% not higher order");
            if (!KBmanager.getMgr().prefEquals("cache","yes") && f.isCached())
                continue;
            if (counter++ % 100 == 0) /*System.out.print(".");*/ progressSb.append(".");
            if ((counter % 4000) == 1) {
                if (debug) System.out.print(progressSb.toString() + "x");
                progressSb.setLength(0);
                if (debug) System.out.printf("%nSUMOKBtoTPTPKB.writeFile(%s) : still working. %d%% done.%n",fileName, counter*100/total);
            }
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : process: " + f);
            processed = fp.preProcess(f,false,kb);
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : processed: " + processed);
            if (!processed.isEmpty()) {
                withRelnRenames = new HashSet<>(); // somehow makes a diff. in tff doc. ordering
                for (Formula f2 : processed)
                    withRelnRenames.add(f2.renameVariableArityRelations(kb,relationMap));
                for (Formula f3 : withRelnRenames) {
                    switch (lang) {
                        case "fof":
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tptp input: " + f3.format("", "", Formula.SPACE));
                            result = SUMOformulaToTPTPformula.tptpParseSUOKIFString(f3.getFormula(), false);
                            if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): result: " + result);
                            if (result != null) {
                                f.theFofFormulas.add(result);
                                f.theTptpFormulas.add(result); // Legacy compatibility
                            }
                            break;
                        case "tff":
                            stfa = new SUMOtoTFAform();
                            SUMOtoTFAform.kb = kb;
                            pw.println("% tff input: " + f3.format("", "", Formula.SPACE));
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tff input: " + f3.format("", "", " "));
                            stfa.sorts = stfa.missingSorts(f3);
                            if (stfa.sorts != null && !stfa.sorts.isEmpty()) {
                                f3.tffSorts.addAll(stfa.sorts);
                            }
                            result = SUMOtoTFAform.process(f3.getFormula(), false);
                            printTFFNumericConstants(pw);
                            SUMOtoTFAform.initNumericConstantTypes();
                            if (!StringUtil.emptyString(result)) {
                                f.theTffFormulas.add(result);
                                f.theTptpFormulas.add(result); // Legacy compatibility
                            } else if (!StringUtil.emptyString(SUMOtoTFAform.filterMessage)) {
                                pw.println("% " + SUMOtoTFAform.filterMessage);
                            }
                            break;
                        default:
                            pw.println("% unhandled language option " + lang);
                            break;
                    }
                }
            }
            else {
                //System.out.println("SUMOKBtoTPTPKB.writeFile() : % empty result from preprocess on " + f.getFormula().replace("\\n"," "));
                pw.println("% empty result from preprocess on " + f.getFormula().replace("\\n",Formula.SPACE));
            }
            for (String sort : f.tffSorts) {
                if (!StringUtil.emptyString(sort) &&
                        !alreadyWrittenTPTPs.contains(sort)) {
                    name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                    axiomKey.put(name,f);
                    pw.println(lang + Formula.LP + name + ",axiom,(" + sort + ")).");
                    alreadyWrittenTPTPs.add(sort);
                }
            }
            // Use format-specific field for file writing
            Set<String> formulasToWrite = lang.equals("fof") ? f.theFofFormulas : f.theTffFormulas;
            for (String theTPTPFormula : formulasToWrite) {
                if (!StringUtil.emptyString(theTPTPFormula) &&
                        !alreadyWrittenTPTPs.contains(theTPTPFormula) &&
                        !filterAxiom(f,theTPTPFormula,pw)) {
                    if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : writing " + theTPTPFormula);
                    name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                    axiomKey.put(name,f);
                    pw.println(lang + Formula.LP + name + ",axiom,(" + theTPTPFormula + ")).");
                    if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : finished writing " + theTPTPFormula + " with name " + name);
                    alreadyWrittenTPTPs.add(theTPTPFormula);
                }
//                else
//                    pw.println("% empty, already written or filtered formula, skipping : " + theTPTPFormula);
            }
        } // end outer (main) for loop
        if (debug) System.out.println();
        printVariableArityRelationContent(pw,relationMap,getSanitizedKBname(),axiomIndex);
        printTFFNumericConstants(pw);
        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() CWA: " + CWA);
        if (CWA)
            pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));
        if (conjecture != null) {  //----Print conjecture if one has been supplied
            // conjecture.getTheTptpFormulas() should return a
            // List containing only one String, so the iteration
            // below is probably unnecessary
            String type = "conjecture";
            if (isQuestion) type = "question";
            for (String theTPTPFormula : conjecture.theTptpFormulas)
                pw.println(lang + "(prove_from_" + getSanitizedKBname() + "," + type + ",(" + theTPTPFormula + ")).");
        }
        pw.flush();

        relationMap.clear();
        orderedFormulae.clear();
        progressSb.setLength(0);

        return getInfFilename();
    }


    private static boolean isNonReasoningForATP(String kif) {
        // keep it cheap: no parsing, just prefix checks / contains checks
        return kif.startsWith("(termFormat ")
                || kif.startsWith("(format ")
                || kif.startsWith("(documentation");
    }

    /** *************************************************************
     * Threaded execution of the main loop
     *
     * @param fileName - the full pathname of the file to write
     * @param conjecture a conjecture to query the KB with if any
     * @param isQuestion flag to denote a question being posed
     * @param pw the PrintWriter to write TPTP with
     *
     * @return the name of the KB translation to TPTP file
     */
    private String _tWriteFile(String fileName, Formula conjecture,
                               boolean isQuestion, PrintWriter pw) {

        final String localLang = this.lang; // snapshot once
        final ExportProfile prof = PROFILE_TFF ? new ExportProfile() : null;

        Map<String, String> relationMap = new TreeMap<>(); // variable-arity relations keyed by new name
        writeHeader(pw, fileName);

        OrderedFormulae orderedFormulae = new OrderedFormulae();
        orderedFormulae.addAll(kb.formulaMap.values());
        int total = orderedFormulae.size();

        // NOTE: single-threaded export (debugging mode): no executor, no futures, no writeMap
        for (Formula formula : orderedFormulae) {

            Formula f = formula;

            // Skip Formulas that start with "documentation, termFormat, format""
            if (isNonReasoningForATP(f.getFormula())) continue;

            if (prof != null) prof.nFormulas++;

            // Format-specific cache clearing to prevent FOF/TFF overwrites
            if (localLang.equals("fof")) {
                f.theFofFormulas.clear();
            } else if (localLang.equals("tff")) {
                f.theTffFormulas.clear();
            }
            f.theTptpFormulas.clear(); // Legacy compatibility

            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processed = null, withRelnRenames;
            List<String> fileContents = new LinkedList<>();
            String name, result;
            SUMOtoTFAform stfa;

            try {
                if (!f.getFormula().startsWith("(documentation")) {
                    fileContents.add("% f: " + f.format("", "", Formula.SPACE));
                    if (!f.derivation.parents.isEmpty()) {
                        for (Formula derivF : f.derivation.parents)
                            fileContents.add("% original f: " + derivF.format("", "", Formula.SPACE));
                    }
                    fileContents.add("% " + formCount.getAndIncrement() + " of " + total +
                            " from file " + f.sourceFile + " at line " + f.startLine);
                }

                if (f.isHigherOrder(kb)) {
                    fileContents.add("% is higher order");
                    if (localLang.equals("thf"))
                        f = Modals.processModals(f, kb);
                    if (removeHOL) {
                        if (prof != null) prof.nSkippedHOL++;
                        continue;
                    }
                } else {
                    fileContents.add("% not higher order");
                }

                if (!KBmanager.getMgr().prefEquals("cache", "yes") && f.isCached()) {
                    if (prof != null) prof.nSkippedCached++;
                    continue;
                }

                if (counter++ % 100 == 0) progressSb.append(".");
                if ((counter % 4000) == 1) {
                    if (debug) System.out.print(progressSb.toString() + "x");
                    progressSb.setLength(0);
                    if (debug) System.out.printf("%nSUMOKBtoTPTPKB.writeFile(%s) : still working. %d%% done.%n",
                            fileName, counter * 100 / total);
                }

                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : process: " + f);

                // ---- preprocess timing ----
                long tPre0 = (prof != null) ? System.nanoTime() : 0L;
                processed = fp.preProcess(f, false, kb);
                if (prof != null) {
                    prof.tPreprocessNs += (System.nanoTime() - tPre0);
                    prof.nProcessedSets++;
                    if (processed != null) prof.nProcessedExpanded += processed.size();
                }

                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : processed: " + processed);

                if (processed != null && !processed.isEmpty()) {

                    // ---- rename timing ----
                    long tRen0 = (prof != null) ? System.nanoTime() : 0L;
                    withRelnRenames = new HashSet<>();
                    for (Formula f2 : processed)
                        withRelnRenames.add(f2.renameVariableArityRelations(kb, relationMap));
                    if (prof != null) {
                        prof.tRenameNs += (System.nanoTime() - tRen0);
                        prof.nRenamedExpanded += withRelnRenames.size();
                    }

                    for (Formula f3 : withRelnRenames) {
                        switch (localLang) {
                            case "fof":
                                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tptp input: "
                                        + f3.format("", "", " "));
                                result = SUMOformulaToTPTPformula.tptpParseSUOKIFString(f3.getFormula(), false);
                                if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): result: " + result);
                                if (result != null) {
                                    f.theFofFormulas.add(result);
                                    f.theTptpFormulas.add(result); // Legacy compatibility
                                }
                                break;

                            case "tff":
                                stfa = new SUMOtoTFAform();
                                SUMOtoTFAform.kb = kb; // leave for now

                                // For top-N slowest process() tracking
                                String src = f3.sourceFile + ":" + f3.startLine;
                                String preview = f3.getFormula().replace('\n', ' ');
                                if (preview.length() > 180) preview = preview.substring(0, 180);

                                fileContents.add("% tff input: " + f3.format("", "", Formula.SPACE));

                                if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tff input: "
                                        + f3.format("", "", " "));

                                long tMs0 = (prof != null) ? System.nanoTime() : 0L;
                                stfa.sorts = stfa.missingSorts(f3);
                                if (prof != null) prof.tMissingSortsNs += (System.nanoTime() - tMs0);

                                if (stfa.sorts != null && !stfa.sorts.isEmpty())
                                    f3.tffSorts.addAll(stfa.sorts);

                                long tP0 = (prof != null) ? System.nanoTime() : 0L;
                                result = SUMOtoTFAform.process(f3.getFormula(), false);
                                if (prof != null) {
                                    long dt = System.nanoTime() - tP0;
                                    prof.tProcessNs += dt;
                                    prof.topProcess.offer(dt, src, preview);
                                }

                                printTFFNumericConstants(fileContents);
                                SUMOtoTFAform.initNumericConstantTypes();

                                if (!StringUtil.emptyString(result)) {
                                    f.theTffFormulas.add(result);
                                    f.theTptpFormulas.add(result); // Legacy compatibility
                                } else if (!StringUtil.emptyString(SUMOtoTFAform.filterMessage))
                                    fileContents.add("% " + SUMOtoTFAform.filterMessage);
                                break;

                            default:
                                fileContents.add("% unhandled language option " + localLang);
                                break;
                        }
                    }
                } else {
                    fileContents.add("% empty result from preprocess on "
                            + f.getFormula().replace("\\n", Formula.SPACE));
                }

                for (String sort : f.tffSorts) {
                    if (!StringUtil.emptyString(sort) && !alreadyWrittenTPTPs.contains(sort)) {
                        name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                        axiomKey.put(name, f);
                        fileContents.add(localLang + Formula.LP + name + ",axiom,(" + sort + ")).");
                        alreadyWrittenTPTPs.add(sort);
                        if (prof != null) prof.nSortsEmitted++;
                    }
                }

                // Use format-specific field for file writing
                Set<String> formulasToWrite = localLang.equals("fof") ? f.theFofFormulas : f.theTffFormulas;
                for (String theTPTPFormula : formulasToWrite) {
                    if (!StringUtil.emptyString(theTPTPFormula) &&
                            !alreadyWrittenTPTPs.contains(theTPTPFormula)) {

                        // ---- filter timing ----
                        long tF0 = (prof != null) ? System.nanoTime() : 0L;
                        boolean filtered = filterAxiom(f, theTPTPFormula, fileContents);
                        if (prof != null) prof.tFilterNs += (System.nanoTime() - tF0);

                        if (!filtered) {
                            name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                            axiomKey.put(name, f);
                            fileContents.add(localLang + Formula.LP + name + ",axiom,(" + theTPTPFormula + ")).");
                            alreadyWrittenTPTPs.add(theTPTPFormula);
                            if (prof != null) prof.nAxiomsEmitted++;
                        } else {
                            if (prof != null) prof.nAxiomsSkipped++;
                        }
                    } else {
                        fileContents.add("% empty, already written or filtered formula, skipping : " + theTPTPFormula);
                        if (prof != null) prof.nAxiomsSkipped++;
                    }
                }

            } finally {
                if (processed != null)
                    processed.clear();
            }

            // Write this formula's output immediately (single-threaded)
            long tW0 = (prof != null) ? System.nanoTime() : 0L;
            for (String line : fileContents)
                pw.println(line);
            if (prof != null) prof.tPrintNs += (System.nanoTime() - tW0);
        }

        System.out.println();
        printVariableArityRelationContent(pw, relationMap, getSanitizedKBname(), axiomIndex);
        printTFFNumericConstants(pw);
        System.out.println("SUMOKBtoTPTPKB.writeFile() CWA: " + CWA);
        if (CWA)
            pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));

        if (conjecture != null) {
            String type = isQuestion ? "question" : "conjecture";
            for (String theTPTPFormula : conjecture.theTptpFormulas)
                pw.println(localLang + "(prove_from_" + getSanitizedKBname() + "," + type + ",(" + theTPTPFormula + ")).");
        }

        pw.flush();

        // ---- profile summary ----
        if (prof != null)
            prof.printSummary(fileName + " lang=" + localLang);

        relationMap.clear();
        orderedFormulae.clear();
        progressSb.setLength(0);

        return getInfFilename();
    }


    /** *************************************************************
     * @return true if the given formula is simple clause,
     *   and contains one of the excluded predicates;
     * otherwise return false;
     */
    public boolean filterExcludePredicates(Formula formula) {

        boolean pass = false;
        if (formula.isSimpleClause(kb))
            pass = excludedPredicates.contains(formula.getArgument(0).toString());
        return pass;
    }

    /** *************************************************************
     * @deprecated
     */
    @Deprecated
    public boolean filterAxiom(Formula form, String tptp, PrintWriter pw) {

        //----Don't output ""ed ''ed and numbers
        if (tptp.matches(".*'[a-z][a-zA-Z0-9_]*\\(.*") &&
                this.getClass().equals(SUMOKBtoTPTPKB.class)) { // only filter numbers in TPTP, not TFF
            pw.println("% number: " + tptp);
            return removeNum;
        }
        if (removeStrings && (tptp.contains("'") || tptp.indexOf('"') >= 0)) {
            pw.println("% f: " + form.format("", "", Formula.SPACE));
            pw.println("% quoted thing");
            return true;
        }

        if (form.isHigherOrder(kb))
            if (removeHOL)
                return true;
        if (!filterExcludePredicates(form)) {
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

    public boolean filterAxiom(Formula form, String tptp, List<String> fileContents) {

        //----Don't output ""ed ''ed and numbers
        if (tptp.matches(".*'[a-z][a-zA-Z0-9_]*\\(.*") &&
                this.getClass().equals(SUMOKBtoTPTPKB.class)) { // only filter numbers in TPTP, not TFF
            fileContents.add("% number: " + tptp);
            return removeNum;
        }
        if (removeStrings && (tptp.contains("'") || tptp.indexOf('"') >= 0)) {
            fileContents.add("% f: " + form.format("", "", Formula.SPACE));
            fileContents.add("% quoted thing");
            return true;
        }

        if (form.isHigherOrder(kb))
            if (removeHOL)
                return true;
        if (!filterExcludePredicates(form)) {
            if (!alreadyWrittenTPTPs.contains(tptp)) {
                return false;
            }
            else {
                fileContents.add("% already written: " + tptp);
                return true;
            }
        }
        else {
            fileContents.add("% filtered predicate: " + form.getArgument(0));
            return true;
        }
    }

    /**
     *  PROFILER METHODS
     */

    // ---- Profiling toggle ----
    private static final boolean PROFILE_TFF = Boolean.getBoolean("sigma.tff.profile");

    // ---- Tiny profiling helpers (keep inside the same class) ----
    private static final class ExportProfile {
        final long startedNs = System.nanoTime();

        // counts
        long nFormulas = 0;
        long nSkippedHOL = 0;
        long nSkippedCached = 0;

        long nProcessedSets = 0;
        long nProcessedExpanded = 0;   // sum(processed.size())
        long nRenamedExpanded = 0;     // sum(withRelnRenames.size())

        long nSortsEmitted = 0;
        long nAxiomsEmitted = 0;
        long nAxiomsSkipped = 0;

        // stage times
        long tPreprocessNs = 0;
        long tRenameNs = 0;
        long tMissingSortsNs = 0;
        long tProcessNs = 0;
        long tFilterNs = 0;
        long tPrintNs = 0;

        // top N slow formulas by SUMOtoTFAform.process()
        final TopN topProcess = new TopN(20);

        void printSummary(String label) {
            long totalNs = System.nanoTime() - startedNs;

            System.out.println("==== TFF EXPORT PROFILE: " + label + " ====");
            System.out.printf("Total: %.3fs, formulas=%d, skippedHOL=%d, skippedCached=%d%n",
                    totalNs / 1e9, nFormulas, nSkippedHOL, nSkippedCached);

            System.out.printf("Expanded: processedSets=%d, processedExpanded=%d, renamedExpanded=%d%n",
                    nProcessedSets, nProcessedExpanded, nRenamedExpanded);

            System.out.printf("Emitted: sorts=%d, axioms=%d, skippedAxioms=%d%n",
                    nSortsEmitted, nAxiomsEmitted, nAxiomsSkipped);

            System.out.printf("Time(s): preprocess=%.3f rename=%.3f missingSorts=%.3f process=%.3f filter=%.3f print=%.3f%n",
                    tPreprocessNs / 1e9, tRenameNs / 1e9, tMissingSortsNs / 1e9, tProcessNs / 1e9,
                    tFilterNs / 1e9, tPrintNs / 1e9);

            System.out.println("-- Top slowest SUMOtoTFAform.process() calls --");
            topProcess.print();
            System.out.println("==== END TFF EXPORT PROFILE ====");
        }
    }

    private static final class TopN {
        private final int limit;
        private final java.util.PriorityQueue<Entry> pq;

        private static final class Entry {
            final long ns;
            final String src;
            final String preview;
            Entry(long ns, String src, String preview) { this.ns = ns; this.src = src; this.preview = preview; }
        }

        TopN(int limit) {
            this.limit = limit;
            // Min-heap; smallest at head so we can evict cheaply
            this.pq = new java.util.PriorityQueue<>(java.util.Comparator.comparingLong(e -> e.ns));
        }

        void offer(long ns, String src, String preview) {
            if (limit <= 0) return;
            if (pq.size() < limit) {
                pq.add(new Entry(ns, src, preview));
                return;
            }
            Entry min = pq.peek();
            if (min != null && ns > min.ns) {
                pq.poll();
                pq.add(new Entry(ns, src, preview));
            }
        }

        void print() {
            java.util.ArrayList<Entry> list = new java.util.ArrayList<>(pq);
            // sort descending
            list.sort((a,b) -> Long.compare(b.ns, a.ns));
            for (Entry e : list) {
                System.out.printf("  %.3fs  %s  %s%n", e.ns / 1e9, e.src, e.preview);
            }
        }
    }






    /** *************************************************************
     * Will first write out SUMO.tptp if the KB had not yet been
     * serialized, or serialized files are older than the sources,
     * then, will write out a fresh SUMO.fof.
     *
     * @param args any given command line arguments (not currently used)
     */
    public static void main(String[] args) {

        System.out.println("SUMOKBtoTPTPKB.main(): SUMOKBtoTPTPKB.rapidParsing==" + SUMOKBtoTPTPKB.rapidParsing);
        KBmanager.getMgr().initializeOnce();
        SUMOKBtoTPTPKB skbtptpkb = new SUMOKBtoTPTPKB();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        skbtptpkb.kb = KBmanager.getMgr().getKB(kbName);
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + "." + SUMOKBtoTPTPKB.lang;
        String fileWritten = null;
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(filename)))) {
            fileWritten = skbtptpkb.writeFile(filename, null, false, pw);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (StringUtil.isNonEmptyString(fileWritten))
            System.out.println("File written: " + filename);
        else
            System.err.println("Could not write: " + filename);
    }
}
