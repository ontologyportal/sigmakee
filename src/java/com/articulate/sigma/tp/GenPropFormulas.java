package com.articulate.sigma.tp;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.KButilities;
import com.articulate.sigma.SimpleElement;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;

public class GenPropFormulas {
    public static final String NOT = "~";
    public static final String OR = "|";
    public static final String AND = "&";
    public static final String IMPLIES = "=>";
    public static final String IFF = "<=>";
    public static final String PARENS = "()"; // they go around a formula

    public static boolean debug = false;

    public static final String[] OPS = {NOT,OR,AND,IMPLIES,IFF,PARENS};
    public String atom = null; // must be either an atom or an operator - one must be null
    public String operator = null;
    public GenPropFormulas f1 = null;
    public GenPropFormulas f2 = null;

    public enum SZSonto {CONTRA, SAT, OTHER};  // theorem prover status

    private static Random random = new Random();
    public static Vampire vamp = new Vampire();
    public static ECNF ecnf = new ECNF();
    public static TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();

    public static final List<String> vcnfcmds = List.of("--mode","clausify","-updr","off");
    public static final List<String> ecnfcmds = List.of("--cnf","--no-preprocessing");

    public static Set<String> contraResults = new HashSet<>();
    public static Set<String> tautResults = new HashSet<>();
    public static Set<String> satResults = new HashSet<>();
    public static Map<String,String> CNF = new HashMap<>();
    public static Map<String,String> truthTables = new HashMap<>();
    public static Map<String,String> tableaux = new HashMap<>();

    /** ***************************************************************
     * constructor
     */
    public GenPropFormulas(String atom, String operator, GenPropFormulas f1, GenPropFormulas f2) {

        if (atom != null) {
            this.atom = atom;
        }
        else if (operator == null ? PARENS == null : operator.equals(PARENS)) {
            this.operator = operator;
            this.f1 = f1;
        }
        else if (operator == null ? NOT == null : operator.equals(NOT)) {
            this.operator = operator;
            this.f1 = f1;
        }
        else {
            this.operator = operator;
            this.f1 = f1;
            this.f2 = f2;
        }
    }

    /** ***************************************************************
     * convert to a string
     */
    public static void init() {

        random = new Random();
        vamp = new Vampire();
        ecnf = new ECNF();
        tpp = new TPTP3ProofProcessor();

        contraResults = new HashSet<>();
        satResults = new HashSet<>();
        tautResults = new HashSet<>();
        CNF = new HashMap<>();
        truthTables = new HashMap<>();
        tableaux = new HashMap<>();
    }

    /** ***************************************************************
     * convert to a string
     */
    @Override
    public String toString() {

        if (atom != null)
            return atom;
        if (operator == null ? PARENS == null : operator.equals(PARENS))
            return "(" + f1.toString() + ")";
        if (operator == null ? NOT == null : operator.equals(NOT))
            return NOT + f1.toString();
        return f1.toString() + operator + f2.toString();
    }

    /** ***************************************************************
     * convert to a string
     */
    public static String formatCNF(List<String> l) {

        StringBuilder sb = new StringBuilder();
        HashSet<String> seen = new HashSet<>();
        for (String s : l) {
            //System.out.println("formatCNF(): input: " + s);
            if (!s.startsWith("%") && s.length() > 4) {
                int firstComma = s.indexOf(",");
                if (firstComma == -1)
                    continue;
                int secondComma = s.indexOf(",",firstComma+1);
                if (secondComma == -1)
                    continue;
                int end = s.length()-2;
                int thirdComma = s.indexOf(",",secondComma+1);
                if (thirdComma > -1)
                    end = thirdComma;
                String firstParam = s.substring(s.indexOf("(")+1,firstComma).trim();
                String secondParam = s.substring(firstComma+1,secondComma).trim();
                //System.out.println("formatCNF(): firstParam: " + firstParam);
                //System.out.println("formatCNF(): secondParam: " + secondParam);
                //System.out.println(firstComma + ", " + secondComma + ", " + thirdComma);
                // if (!secondParam.equals("axiom")) Vampire uses "axiom"
                if (!secondParam.equals("plain")) // E uses "plain"
                    continue;
                String clause = s.substring(secondComma+1,end).trim();
                //System.out.println("formatCNF(): result: " + clause);
                if (seen.contains(clause))
                    continue;
                seen.add(clause);
                sb.append(clause).append(", ");
            }
        }
        if (sb.length() > 3)
            sb.delete(sb.length()-2,sb.length());
        return sb.toString();
    }

    /** ***************************************************************
     * Write all the strings in @param stmts to a new file. Run vampire
     * to prove whether each string is a tautology, contradiction or
     * satisfiable.
     * @param filename is a side effect that contains the filename on exit.
     *                 The system searches for a filename that doesn't exist
     *                 already.
     * @return a selection from the TPTP SZS ontology of contradiction,
     * saturation or "other"
     */
    private static SZSonto run(Set<String> stmts, StringBuilder filename) throws Exception {

        int count = 0;
        String fname = "prob" + count + ".p";
        File f = new File(fname);
        while (f.exists()) {
            count++;
            fname = "prob" + count + ".p";
            f = new File(fname);
        }
        filename.append(fname);
        System.out.println("GenPropFormulas.run(): filename: " + filename.toString());
        System.out.println("GenPropFormulas.run(): stmts: " + stmts);
        try (Writer fw = new FileWriter(fname);
             PrintWriter pw = new PrintWriter(fw)) {
            for (String s : stmts)
                pw.println(s);
        }
        catch (IOException e) {
            System.err.println("GenPropFormulas.run(): Error in writeStatements(): " + e.getMessage());
            System.err.println("GenPropFormulas.run(): Error writing file " + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        
        // Find path to vampire
        KBmanager mgr = KBmanager.getMgr();
        SimpleElement config = mgr.readConfiguration(KButilities.SIGMA_HOME + File.separator + "KBs");
        for (SimpleElement el : config.getChildElements()) {
            if (el.getTagName().equals("preference")) {
                if (el.getAttribute("name").contains("vampire"))
                    mgr.setPref("vampire", el.getAttribute("value"));
                if (el.getAttribute("name").contains("eprover"))
                    mgr.setPref("eprover", el.getAttribute("value"));
            }
        }
        if (mgr.getPref("vampire").isEmpty())
            KBmanager.getMgr().setPref("vampire","/home/apease/workspace/vampire/vampire");
        Vampire.mode = Vampire.ModeType.CASC; // default
        System.out.println("GenPropFormulas.run(): before vampire");
        if (mgr.getPref("eprover").isEmpty())
            KBmanager.getMgr().setPref("eprover","/home/apease/workspace/eprover/PROVER/eprover");

        boolean sat = false;
        vamp.run(f,5);
        boolean proof = false;
        for (String s : vamp.output) {
            if (s.contains("Refutation found"))
                proof = true;
            if (s.contains("Saturation"))
                sat = true;
        }
        if (proof) {
            System.out.println("run(): Proof found: statement " + stmts + " is a contradiction");
            vamp.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
            return SZSonto.CONTRA;
        }
        else if (sat) {
            System.out.println("run(): Saturation: statement " + stmts);
            vamp.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
            return SZSonto.SAT;
        }
        else {
            System.out.println("run(): no proof: statement " + stmts);
            return SZSonto.OTHER;
        }
    }

    /** ***************************************************************
     * Randomly generate a propositional formula with the given number of
     * atoms and levels of nesting.
     */
    private GenPropFormulas generate(String parentOp, int atomCount, int levels) {

        int prob = random.nextInt(levels);
        if (prob == 0) { // generate an atom
            char atomChar = (char) ('p' + random.nextInt(atomCount));
            return new GenPropFormulas(Character.toString(atomChar),null,null,null);
        }
        else {
            int index = random.nextInt(OPS.length-1); // don't generate PARENS
            //System.out.println("generate(): OPS length: " + OPS.length);
            //System.out.println("generate(): index: " + index);
            String op = OPS[index];
            while (op.equals(parentOp))
                op = OPS[random.nextInt(OPS.length)];
            //System.out.println("generate(): op: " + op);
            if ((operator == null ? PARENS == null : operator.equals(PARENS)) ||  (operator == null ? NOT == null : operator.equals(NOT))) {
                return new GenPropFormulas(null,op,generate(op,atomCount,levels-1),null);
            }
            else {
                return new GenPropFormulas(null,PARENS,
                        new GenPropFormulas(null,op,generate(op,atomCount,levels-1),
                                generate(op,atomCount,levels-1)),null);
            }
        }
    }

    /** ***************************************************************
     */
    private static String listToString(List<String> input) {

        StringBuilder sb = new StringBuilder();
        for (String s : input)
            sb.append(s).append(" ");
        return sb.toString();
    }

    /** ***************************************************************
     * Encode formula for the automated truth table generator
     * https://www.emathhelp.net/calculators/discrete-mathematics/truth-table-calculator/?f=
     * requires <-> and ->
     */
    public static String encodeTT(String s) {

        String result = s.replace("<=>","<->");
        result = result.replace("=>","->");
        result = URLEncoder.encode(result, Charset.defaultCharset());
        return "https://www.emathhelp.net/calculators/discrete-mathematics/truth-table-calculator/?f=" + result;
    }

    /** ***************************************************************
     * Encode formula for the automated tableau generator
     * https://mathlogic.lv/tableau.html?f=%28%28q%E2%88%A8%C2%ACr%29%E2%88%A7%28r%E2%88%A8%C2%ACp%29%29%E2%88%A7%28p%E2%88%A8%C2%ACq%29
     */
    public static String encodeTab(String s) {

        String result = s.replace("~","¬");
        result = result.replace("|","∨");
        result = result.replace("&","∧");
        result = result.replace("<=>","↔");
        result = result.replace("=>","→");
        result = URLEncoder.encode(result,Charset.defaultCharset());
        return "https://mathlogic.lv/tableau.html?f=" + result;
     }

    /** ***************************************************************
     */
    public static void printResults() {

        System.out.println();
        System.out.println("--------------------------");
        System.out.println("Contradictions:");
        for (String s : contraResults)
            System.out.println(s);
        System.out.println("Tautologies:");
        for (String s : tautResults)
            System.out.println(s);
        System.out.println("Satisfiable but not Tautologies:");
        for (String s : satResults)
            System.out.println(s);
        System.out.println("CNF:");
        for (String s : CNF.keySet())
            System.out.println(s + "\n" + CNF.get(s));
        System.out.println("Truth table:");
        for (String s : truthTables.keySet())
            System.out.println(s + "\n" + truthTables.get(s));
        System.out.println("Tableau:");
        for (String s : tableaux.keySet())
            System.out.println(s + "\n" + tableaux.get(s));
    }

    /** ***************************************************************
     */
    public static void generateCNFandLinks(String form, String filename)  throws Exception {

        File fname = new File(filename);
        //System.out.println("generateCNFandLinks(): filename: " + filename);
        //System.out.println("generateFormulas(): formula: " + form);
        //System.out.println("generateFormulas(): Run Vampire for CNF conversion with: " + cnfcmds);
        //vamp.runCustom(fname,0, cnfcmds);
        ecnf.runCustom(fname,0,ecnfcmds);
        String formStr = form;
        //ecnf.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
        String CNFresult = formatCNF(ecnf.output);
        if (CNFresult.isEmpty())
            CNFresult = form;
        System.out.println("generateCNFandLinks(): CNF for " + form + " is " + CNFresult);
        CNF.put(form,CNFresult);
        String encoded = encodeTT(formStr);
        truthTables.put(formStr,encoded);
        encoded = encodeTab(formStr);
        tableaux.put(formStr,encoded);
    }

    /** ***************************************************************
     */
    public static void generateFormulas(int targetCount, int numvars, int depth) throws Exception {

        GenPropFormulas f = new GenPropFormulas("a",null,null,null);
        int iter = 0;
        int count = 0;

        while (count < targetCount && iter < 200) {
            String form = f.generate("", numvars, depth).toString();
            System.out.println("\n*************************\ngenerateFormulas(): form: " + form);
            String wrappedForm = "fof(conj,axiom," + form + ").";
            System.out.println("generateFormulas(): wrapped: " + wrappedForm);
            Set<String> stmts = new HashSet<>();
            stmts.add(wrappedForm);
            StringBuilder filename = new StringBuilder();
            SZSonto result = run(stmts,filename);
            HashSet<String> negstmts = new HashSet<>();
            String negWrappedForm = "fof(conj,axiom,~(" + form + ")).";
            String negForm = "~(" + form + ")";
            System.out.println("generateFormulas(): neg form: " + negForm);
            System.out.println("generateFormulas(): neg wrapped: " + negWrappedForm);
            negstmts.add(negWrappedForm);
            StringBuilder negfilename = new StringBuilder();
            SZSonto negresult = run(negstmts,negfilename);

            if (result == SZSonto.SAT && negresult == SZSonto.CONTRA) {
                count++;
                tautResults.add(form);
                contraResults.add(negForm);
            }

            if (negresult == SZSonto.SAT && result == SZSonto.CONTRA) {
                count++;
                tautResults.add(negForm);
                contraResults.add(form);
            }

            if (negresult == SZSonto.SAT && result == SZSonto.SAT) {
                count++;
                satResults.add(negForm);
                satResults.add(form);
            }

            generateCNFandLinks(form,filename.toString());
            generateCNFandLinks(negForm,negfilename.toString());
            iter++;
        }
        printResults();
    }

    /** ***************************************************************
     */
    public static void printHelp() {

        System.out.println("GenPropFormulas class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -g numvars depth - generate formulas with these params");
        System.out.println("  -c - test format CNF ");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws Exception {

        if (args == null || args.length == 0) {
            printHelp();
        }
        else {
            if (args.length > 0 && args[0].equals("-h")) {
                printHelp();
            }
            if (args.length > 2 && args[0].equals("-g") && args[1] != null && args[2] != null ) {
                int numvars = Integer.parseInt(args[1]);
                int depth = Integer.parseInt(args[2]);
                generateFormulas(1,numvars,depth);  // generate 10 good formulas

                System.out.println("Contradictions");
                for (String s : GenPropFormulas.contraResults) {
                    System.out.println("Formula: " + s);
                    System.out.println("CNF: " + GenPropFormulas.CNF.get(s));
                    System.out.println(GenPropFormulas.truthTables.get(s));
                    System.out.println(GenPropFormulas.tableaux.get(s));
                }
                System.out.println("<P><b>Tautologies</b>:<br>");
                for (String s : GenPropFormulas.tautResults) {
                    System.out.println("Formula: " + s);
                    System.out.println("CNF: " + GenPropFormulas.CNF.get(s));
                    System.out.println(GenPropFormulas.truthTables.get(s));
                    System.out.println(GenPropFormulas.tableaux.get(s));
                }
                System.out.println("<P><b>Satisfiable but not a Tautology</b>:<br>");
                for (String s : GenPropFormulas.satResults) {
                    System.out.println("Formula: " + s);
                    System.out.println("CNF: " + GenPropFormulas.CNF.get(s));
                    System.out.println(GenPropFormulas.truthTables.get(s));
                    System.out.println(GenPropFormulas.tableaux.get(s));
                }
            }
            if (args.length > 0 && args[0].equals("-c")) {
                debug = true;
                //generateFormulas(1);
                String form = "~((((r|r)&(~((p<=>p)&(~p))))=>p))";
                System.out.println(encodeTab(form));
            }
        }
    }
}
