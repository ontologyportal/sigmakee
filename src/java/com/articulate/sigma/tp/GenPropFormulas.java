package com.articulate.sigma.tp;

import com.articulate.sigma.Formula;
import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenPropFormulas {
    public static final String NOT = "~";
    public static final String OR = "|";
    public static final String AND = "&";
    public static final String IMPLIES = "=>";
    public static final String IFF = "<=>";
    public static final String PARENS = "()"; // they go around a formula

    public static boolean debug = false;

    public static final String[] ops = {NOT,OR,AND,IMPLIES,IFF,PARENS};
    public String atom = null; // must be either an atom or an operator - one must be null
    public String operator = null;
    public GenPropFormulas f1 = null;
    public GenPropFormulas f2 = null;

    private static Random random = new Random();
    public static Vampire vamp = new Vampire();
    public static TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();

    public static ArrayList<String> cmds = new ArrayList<>();

    public static HashSet<String> contraResults = new HashSet<>();
    public static HashSet<String> tautResults = new HashSet<>();
    public static HashMap<String,String> CNF = new HashMap<>();
    public static HashMap<String,String> truthTables = new HashMap<>();
    public static HashMap<String,String> tableaux = new HashMap<>();

    /** ***************************************************************
     * constructor
     */
    public GenPropFormulas(String atom, String operator, GenPropFormulas f1, GenPropFormulas f2) {

        if (atom != null) {
            this.atom = atom;
        }
        else if (operator == PARENS) {
            this.operator = operator;
            this.f1 = f1;
        }
        else if (operator == NOT) {
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
        tpp = new TPTP3ProofProcessor();

        cmds = new ArrayList<>();

        contraResults = new HashSet<>();
        tautResults = new HashSet<>();
        CNF = new HashMap<>();
        truthTables = new HashMap<>();
        tableaux = new HashMap<>();
    }

    /** ***************************************************************
     * convert to a string
     */
    public String toString() {

        if (atom != null)
            return atom;
        if (operator == PARENS)
            return "(" + f1.toString() + ")";
        if (operator == NOT)
            return NOT + f1.toString();
        return f1.toString() + operator + f2.toString();
    }

    /** ***************************************************************
     * convert to a string
     */
    public static String formatCNF(List<String> l) {

        StringBuffer sb = new StringBuffer();
        for (String s : l) {
            if (!s.startsWith("%") && s.length() > 4) {
                //System.out.println("formatCNF(): input: " + s);
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
                //System.out.println(firstComma + ", " + secondComma + ", " + thirdComma);
                //System.out.println("formatCNF(): result: " + s.substring(secondComma+1,end));
                sb.append(s.substring(secondComma+1,end).trim() + ", ");
            }
        }
        sb.delete(sb.length()-2,sb.length());
        return sb.toString();
    }

    /** ***************************************************************
     * Write all the strings in @param stmts to a new file
     * @param filename is a side effect that contains the filename on exit
     * @return a boolean that is true when there is a proof of a contradiction
     */
    private static boolean run(Set<String> stmts, StringBuffer filename) throws Exception {

        int count = 0;
        String fname = "prob" + count + ".p";
        File f = new File(fname);
        while (f.exists()) {
            count++;
            fname = "prob" + count + ".p";
            f = new File(fname);
        }
        filename.append(fname);
        System.out.println("GenFormula.run(): filename: " + filename.toString());
        try (FileWriter fw = new FileWriter(fname);
             PrintWriter pw = new PrintWriter(fw)) {
            for (String s : stmts)
                pw.println(s);
        }
        catch (IOException e) {
            System.err.println("run(): Error in writeStatements(): " + e.getMessage());
            System.err.println("run(): Error writing file " + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        KBmanager.getMgr().setPref("vampire","/home/apease/workspace/vampire/vampire");
        Vampire.mode = Vampire.ModeType.CASC; // default
        System.out.println("GenFormula.run(): before vampire");
        ArrayList<String> cmds = new ArrayList<>();

        vamp.run(f,5);
        boolean proof = false;
        for (String s : vamp.output) {
            if (s.contains("Refutation found"))
                proof = true;
        }
        if (proof) {
            System.out.println("Proof found");
            vamp.output = tpp.joinNreverseInputLines(vamp.output);
            return true;
        }
        else {
            System.out.println("no proof");
            return false;
        }
    }

    /** ***************************************************************
     */
    private GenPropFormulas generate(String parentOp, int atomCount, int levels) {

        int prob = random.nextInt(levels);
        if (prob == 0) { // generate an atom
            char atomChar = (char) ('p' + random.nextInt(atomCount));
            return new GenPropFormulas(Character.toString(atomChar),null,null,null);
        }
        else {
            int index = random.nextInt(ops.length-1); // don't generate PARENS
            //System.out.println("generate(): ops length: " + ops.length);
            //System.out.println("generate(): index: " + index);
            String op = ops[index];
            while (op.equals(parentOp))
                op = ops[random.nextInt(ops.length)];
            //System.out.println("generate(): op: " + op);
            if ((operator == PARENS) ||  (operator == NOT)) {
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

        StringBuffer sb = new StringBuffer();
        for (String s : input)
            sb.append(s + " ");
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
        result = URLEncoder.encode(result);
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
        result = result.replace("=>","→");
        result = URLEncoder.encode(result);
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
    public static void generateFormulas(int targetCount) throws Exception {

        GenPropFormulas f = new GenPropFormulas("a",null,null,null);
        int count = 0;

        while (count < targetCount) {
            GenPropFormulas form = f.generate("", 3, 6);
            System.out.println("generateFormulas(): form: " + form);
            String wrappedForm = "fof(conj,axiom," + form.toString() + ").";
            System.out.println("generateFormulas(): wrapped: " + wrappedForm);
            HashSet<String> stmts = new HashSet<>();
            stmts.add(wrappedForm);
            StringBuffer filename = new StringBuffer();
            boolean result = run(stmts,filename);
            if (result) {
                count++;
                tautResults.add(form.toString());
                File fname = new File(filename.toString());
                System.out.println("generateFormulas(): filename: " + filename);
                vamp.runCustom(fname,5,cmds);
                String formStr = form.toString();
                vamp.output = tpp.joinNreverseInputLines(vamp.output);
                System.out.println("CNF proof: " + StringUtil.arrayListToCRLFString(vamp.output));
                CNF.put(form.toString(),formatCNF(vamp.output));
                String encoded = encodeTT(formStr);
                truthTables.put(formStr,encoded);
                encoded = encodeTab(formStr);
                tableaux.put(formStr,encoded);
            }
            System.out.println();

            stmts = new HashSet<>();
            wrappedForm = "fof(conj,axiom,~(" + form.toString() + ")).";
            String formStr = "~(" + form.toString() + ")";
            System.out.println(formStr);
            stmts.add(wrappedForm);
            filename = new StringBuffer();
            result = run(stmts,filename);
            if (result) {
                count++;
                contraResults.add(formStr);
                File fname = new File(filename.toString());
                System.out.println("generateFormulas(): filename: " + filename);
                vamp.runCustom(fname,5,cmds);
                vamp.output = tpp.joinNreverseInputLines(vamp.output);
                System.out.println("CNF proof: " + StringUtil.arrayListToCRLFString(vamp.output));
                CNF.put(form.toString(),formatCNF(vamp.output));
                String encoded = encodeTT(formStr);
                truthTables.put(formStr,encoded);
                encoded = encodeTab(formStr);
                tableaux.put(formStr,encoded);
            }
        }
        printResults();
    }

    /** ***************************************************************
     */
    public static void printHelp() {

        System.out.println("GenPropFormulas class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -g - generate formulas");
        System.out.println("  -c - test format CNF ");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws Exception {

        if (args == null || args.length == 0) {
            printHelp();
        }
        else {
            cmds.add("--mode");
            cmds.add("clausify");
            cmds.add("-updr");
            cmds.add("off");
            if (args.length > 0 && args[0].equals("-h")) {
                printHelp();
            }
            if (args.length > 0 && args[0].equals("-g")) {
                generateFormulas(10);  // generate 10 good formulas

                System.out.println("Contradictions");
                for (String s : GenPropFormulas.contraResults) {
                    System.out.println("CNF: " + GenPropFormulas.CNF.get(s));
                    System.out.println(GenPropFormulas.truthTables.get(s));
                    System.out.println(GenPropFormulas.tableaux.get(s));
                }
                System.out.println("<P><b>Tautologies</b>:<br>");
                for (String s : GenPropFormulas.tautResults) {
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
