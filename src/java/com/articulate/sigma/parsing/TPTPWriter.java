package com.articulate.sigma.parsing;

import org.antlr.v4.runtime.tree.ParseTree;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.FileUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TPTPWriter {

    public Set<FormulaAST> formulas = null;

    public static boolean debug = false;

    /** ***************************************************************
     * file : (sentence | comment)+ EOF ;
     * @return a String of TPTP formulas
     */
    public String visitFile(SuokifParser.FileContext context) {

        StringBuilder sb = new StringBuilder();
        if (debug) System.out.println("visitFile() Visiting file: " + context.getText());
        if (debug) System.out.println("visitFile() # children: " + context.children.size());
        if (debug) System.out.println("visitFile() text: " + context.getText());
//        int counter = 0;
//        FormulaAST f = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitFile() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                sb.append(visitSentence((SuokifParser.SentenceContext) c));
            }
            if (c instanceof SuokifParser.CommentContext) {
                sb.append("# ").append(visitComment((SuokifParser.CommentContext) c));
            }
        }
        return sb.toString();
    }

    /** ***************************************************************
     * sentence : (relsent | logsent | quantsent | variable) ;
     */
    public String visitSentence(SuokifParser.SentenceContext context) {

        if (context == null || context.children == null) return null;
        if (debug) System.out.println("visitSentence() Visiting sentence: " + context.getText());
        if (debug) System.out.println("visitSentence() # children: " + context.children.size());
        if (debug) System.out.println("visitSentence() text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug)  System.out.println("child of sentence: " + c.getClass().getName());
            switch (c.getClass().getName()) {
                case "com.articulate.sigma.parsing.SuokifParser$RelsentContext":
                    return visitRelsent((SuokifParser.RelsentContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$LogsentContext":
                    return visitLogsent((SuokifParser.LogsentContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$QuantsentContext":
                    return visitQuantsent((SuokifParser.QuantsentContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$VariableContext":
                    return visitVariable((SuokifParser.VariableContext) c);
                default:
                    break;
            }
        }
        return null;
    }

    /** ***************************************************************
     */
    public String visitComment(SuokifParser.CommentContext context) {

        if (debug) System.out.println("Visiting comment: " + context.getText());
        if (debug) System.out.println(context.COMMENT().getText() + "\n");
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        return context.getText();
    }

    /** ***************************************************************
     * relsent : ('(' IDENTIFIER argument+ ')') | ('(' variable argument+ ')')  ;
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     * Set the types of any variables that appear in an instance or subclass
     * declaration
     */
    public String visitRelsent(SuokifParser.RelsentContext context) {

        StringBuilder sb = new StringBuilder();
        FormulaAST result = new FormulaAST();
//        Set<FormulaAST.RowStruct> newRowVarStructs = new HashSet<>();
        result.predVarCache = new HashSet<>();
        if (debug) System.out.println("Visiting relsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        String pred;
//        int argnum;
//        List<String> argList = new ArrayList<>();
//        Set<FormulaAST.ArgStruct> constantTerms = new HashSet<>(); // tracks if a constant is an argument
//        Map<Integer,Set<SuokifParser.ArgumentContext>> args = new HashMap<>();
        if (context.IDENTIFIER() != null) {
            pred = context.IDENTIFIER().toString();
            sb.append("s__").append(pred).append("(");
        }
//        FormulaAST f = null;
        SuokifParser.ArgumentContext ac;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("child of relsent: " + c.getClass().getName());
            if (c instanceof SuokifParser.VariableContext) {
                sb.append(visitVariable((SuokifParser.VariableContext) c)).append(",");
            }
            else if (c instanceof SuokifParser.ArgumentContext) {
                ac = (SuokifParser.ArgumentContext) c;
                if (Preprocessor.kb.kbCache.relations.contains(ac.getText()))
                    sb.append(visitArgument(ac)).append("__m,");
                else
                    sb.append(visitArgument(ac)).append(",");
                if (debug) System.out.println("ac: " + ac.getText());
            }
        }
        sb.delete(sb.length()-1,sb.length());
        sb.append(")");
        return sb.toString();
    }

    /** ***************************************************************
     * argument : (sentence | term) ;
     */
    public String visitArgument(SuokifParser.ArgumentContext context) {

        if (debug) System.out.println("Visiting argument: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (context.children.size() != 1)
            System.err.println("Error in TPTPWriter.visitArgument() wrong # children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("child of argument: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                return visitSentence((SuokifParser.SentenceContext) c);
            }
            else if (c instanceof SuokifParser.TermContext)
                return visitTerm((SuokifParser.TermContext) c);
        }
        return null;
    }

    /** ***************************************************************
     * logsent :  (notsent | andsent | orsent | implies | iff | eqsent) ;
     */
    public String visitLogsent(SuokifParser.LogsentContext context) {

        if (debug) System.out.println("Visiting logsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (context.children.size() != 1)
            System.err.println("Error in TPTPWriter.visitLogsent() wrong # children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitLogsent() child: " + c.getClass().getName());
            switch (c.getClass().getName()) {
                case "com.articulate.sigma.parsing.SuokifParser$NotsentContext":
                    return visitNotsent((SuokifParser.NotsentContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$AndsentContext":
                    return visitAndsent((SuokifParser.AndsentContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$OrsentContext":
                    return visitOrsent((SuokifParser.OrsentContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$ImpliesContext":
                    return visitImplies((SuokifParser.ImpliesContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$IffContext":
                    return visitIff((SuokifParser.IffContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$EqsentContext":
                    return visitEqsent((SuokifParser.EqsentContext) c);
                default:
                    break;
            }
        }
        return null;
    }

    /** ***************************************************************
     * notsent : '(' 'not' sentence ')' ;
     */
    public String visitNotsent(SuokifParser.NotsentContext context) {

        String f = null;
        if (debug) System.out.println("Visiting Notsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitNotsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext)
                f = visitSentence((SuokifParser.SentenceContext) c);
        }
        return "~(" + f + ")";
    }

    /** ***************************************************************
     * andsent : '(' 'and' sentence sentence+ ')' ;
     */
    public String visitAndsent(SuokifParser.AndsentContext context) {

        StringBuilder sb = new StringBuilder();
        if (debug) System.out.println("Visiting Andsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
//        List<FormulaAST> ar = new ArrayList<>();
        sb.append("( ");
        for (ParseTree c : context.children) {
            if (debug) System.out.println("child of andsent: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                sb.append(visitSentence((SuokifParser.SentenceContext) c)).append(" & ");
            }
        }
        sb.delete(sb.length()-2,sb.length());
        sb.append(")");
        return sb.toString();
    }

    /** ***************************************************************
     * orsent : '(' 'or' sentence sentence+ ')' ;
     */
    public String visitOrsent(SuokifParser.OrsentContext context) {

        StringBuilder sb = new StringBuilder();
        if (debug) System.out.println("Visiting Orsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        sb.append("(");
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitOrsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                sb.append(visitSentence((SuokifParser.SentenceContext) c)).append(" | ");
            }
        }
        sb.delete(sb.length()-2,sb.length());
        sb.append(")");
        return sb.toString();
    }

    /** ***************************************************************
     * implies :  '(' '=>' sentence sentence ')' ;
     */
    public String visitImplies(SuokifParser.ImpliesContext context) {

//        StringBuilder sb = new StringBuilder();
        if (debug) System.out.println("Visiting Implies: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        String f1 = null;
        String f2 = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitImplies() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                if (f1 == null) {
                    f1 = visitSentence((SuokifParser.SentenceContext) c);
                }
                else {
                    f2 = visitSentence((SuokifParser.SentenceContext) c);
                }
            }
        }
        return "( " + f1 + " => " + f2 + ")";
    }

    /** ***************************************************************
     * iff : '(' '<=>' sentence sentence ')' ;
     */
    public String visitIff(SuokifParser.IffContext context) {

        if (debug) System.out.println("Visiting Iff: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        String f1 = null;
        String f2 = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitIff() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                if (f1 == null) {  // this is a bit questionable since it makes the lexically first element the antecedent
                    f1 = visitSentence((SuokifParser.SentenceContext) c);
                }
                else {
                    f2 = visitSentence((SuokifParser.SentenceContext) c);
                }
            }
        }
        return "( " + f1 + " <=> " + f2 + ")";
    }

    /** ***************************************************************
     * eqsent : '(' 'equal' term term ')' ;
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public String visitEqsent(SuokifParser.EqsentContext context) {

        if (debug) System.out.println("Visiting Eqsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        String f1 = null;
        String f2 = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitEqsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.TermContext) {
                if (f1 == null) {
                    f1 = visitTerm((SuokifParser.TermContext) c);
                }
                else {
                    f2 = visitTerm((SuokifParser.TermContext) c);
                }
            }
        }
        return "(" + f1 + " = " + f2 + ")";
    }

    /** ***************************************************************
     * quantsent : (forall | exists) ;
     */
    public String visitQuantsent(SuokifParser.QuantsentContext context) {

        if (debug) System.out.println("Visiting quantsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
//        FormulaAST f = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitQuantsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.ForallContext)
                return visitForall((SuokifParser.ForallContext) c);
            else if (c instanceof SuokifParser.ExistsContext)
                return visitExists((SuokifParser.ExistsContext) c);
        }
        return null;
    }

    /** ***************************************************************
     * forall : '(' 'forall' '(' variable+ ')' sentence ')' ;
     */
    public String visitForall(SuokifParser.ForallContext context) {

        if (debug) System.out.println("Visiting Forall: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        StringBuilder varlist = new StringBuilder();
//        FormulaAST f = null;
        String body = null, farg;
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitForall() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.VariableContext) {
                farg = visitVariable((SuokifParser.VariableContext) c);
                varlist.append(farg).append(", ");
            }
            if (c instanceof SuokifParser.SentenceContext) {
                body = visitSentence((SuokifParser.SentenceContext) c);
            }
        }
        varlist.delete(varlist.length()-2,varlist.length());
        return "! [" + varlist + "] : (" + body + ")";
    }

    /** ***************************************************************
     * exists : '(' 'exists' '(' variable+ ')' sentence ')' ;
     */
    public String visitExists(SuokifParser.ExistsContext context) {


        if (debug) System.out.println("Visiting Exists: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        StringBuilder varlist = new StringBuilder();
        String body = null, farg;
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitExists() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.VariableContext) {
                farg = visitVariable((SuokifParser.VariableContext) c);
                varlist.append(farg).append(", ");
            }
            if (c instanceof SuokifParser.SentenceContext) {
                body = visitSentence((SuokifParser.SentenceContext) c);
            }
        }
        varlist.delete(varlist.length()-2,varlist.length());
        return "? [" + varlist + "] : (" + body + ")";
    }

    /** ***************************************************************
     * variable : (REGVAR | ROWVAR) ;
     */
    public String visitVariable(SuokifParser.VariableContext context) {

        if (debug) System.out.println("Visiting variable: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        if (context.REGVAR() != null) {
            if (debug) System.out.println("regvar: " + context.REGVAR().toString());
            return "V__" + context.REGVAR().toString().substring(1);
        }
        if (context.ROWVAR() != null) {
            if (debug) System.out.println("Error - no row vars should exist at this point - rowv: " + context.ROWVAR().toString());
            return context.ROWVAR().toString();
        }
        return null;
    }

    /** ***************************************************************
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public String visitTerm(SuokifParser.TermContext context) {

        if (debug) System.out.println("visitTerm() Visiting Term: " + context.getText());
        if (debug) System.out.println("visitTerm() # children: " + context.children.size());
        if (context.children.size() != 1)
            System.err.println("Error in TPTPWriter.visitTerm() wrong # children: " + context.children.size());
        if (debug) System.out.println("visitTerm() text: " + context.getText());
        if (context.IDENTIFIER() != null) {
            String ident = context.IDENTIFIER().toString();
            if (debug) System.out.println("visitTerm() identifier: " + ident);
            return "s__" + ident;
        }
        if (context.FUNWORD() != null) {
            String funword = context.FUNWORD().toString();
            if (debug) System.out.println("visitTerm() funword: " + funword);
            return "s__" + funword;
        }
        for (ParseTree c : context.children) { // there should be only one child
            if (debug) System.out.println("visitTerm() child: " + c.getClass().getName());
            switch (c.getClass().getName()) {
                case "com.articulate.sigma.parsing.SuokifParser$FuntermContext":
                    return visitFunterm((SuokifParser.FuntermContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$VariableContext":
                    return visitVariable((SuokifParser.VariableContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$StringContext":
                    return visitString((SuokifParser.StringContext) c);
                case "com.articulate.sigma.parsing.SuokifParser$NumberContext":
                    if (debug) System.out.println("visitTerm() found a number: " + c.getText());
                    return visitNumber((SuokifParser.NumberContext) c);
                default:
                    break;
            }
        }
        return null;
    }

    /** ***************************************************************
     * funterm : '(' FUNWORD argument+ ')' ;
     */
    public String visitFunterm(SuokifParser.FuntermContext context) {

        StringBuilder sb = new StringBuilder();
        if (debug) System.out.println("Visiting funterm: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
//        Set<FormulaAST.RowStruct> newRowVarStructs = new HashSet<>();
        String funword;
        if (context.FUNWORD() != null) {
            funword = context.FUNWORD().toString();
            if (debug) System.out.println("funword: " + funword);
            sb.append("s__").append(funword).append("(");
        }
//        int argnum = 1;
//        Map<Integer,Set<SuokifParser.ArgumentContext>> args = new HashMap<>();
//        List<FormulaAST> arf = new ArrayList<>();
//        FormulaAST farg = null;
        SuokifParser.ArgumentContext ac;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitFunterm() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.ArgumentContext) {
                ac = (SuokifParser.ArgumentContext) c;
                if (Preprocessor.kb.kbCache.relations.contains(ac.getText()))
                    sb.append(visitArgument(ac)).append("__m,");
                else
                    sb.append(visitArgument(ac)).append(",");
            }
        }
        sb.delete(sb.length()-1,sb.length());
        sb.append(")");
        return sb.toString();
    }

    /** ***************************************************************
     */
    public String visitString(SuokifParser.StringContext context) {

        if (debug) System.out.println("Visiting string: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = new FormulaAST();
        f.setFormula(context.getText());
        return context.getText();
    }

    /** ***************************************************************
     */
    public String visitNumber(SuokifParser.NumberContext context) {

        if (debug) System.out.println("Visiting number: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        return context.getText();
    }

    /** ***************************************************************
     */
    public String wrappedMetaFormat(FormulaAST f) {
        return "fof(kb_" + FileUtil.noExt(FileUtil.noPath(f.sourceFile)) + "_" + f.startLine + ",axiom," + this.visitSentence(f.parsedFormula) + ").";
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("TPTPWriter class");
        System.out.println("  options (with a leading '-':");
        System.out.println("  h - show this help screen");
        System.out.println("  t - translate configured KB");
        System.out.println("  r - remove explosive multiple pred vars (can be combined with t)");
        System.out.println("    - <file> path of file to process");
        System.out.println("    - <file> path of output file (fof)");
    }

    /** ***************************************************************
     */
    public static void translate(String[] args) {

        long start = System.currentTimeMillis();
//        Path path = Paths.get(System.getenv("SIGMA_HOME") + File.separator + "KBs" + File.separator + "Merge.kif");
        Path path = Paths.get(args[1]);
        SuokifVisitor sv = SuokifVisitor.parseFile(path.toFile());                                        // 1. Parsing
        long end = (System.currentTimeMillis()-start)/1000;
        System.out.println("INFO in TPTPWriter.translate(): # time to parse: " + end);
        start = System.currentTimeMillis();
        Preprocessor pre = new Preprocessor(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));
        if (args[0].contains("r"))
            Preprocessor.removeMultiplePredVar(sv); // remove explosive rules with multiple predicate variables
        Collection<FormulaAST> rules = pre.preprocess(sv.hasPredVar, sv.hasRowVar, sv.rules); // 2. Pre-processing
        end = (System.currentTimeMillis()-start)/1000;
        System.out.println("INFO in TPTPWriter.translate(): # time to preprocess: " + end);
        start = System.currentTimeMillis();
        TPTPWriter tptpW = new TPTPWriter();
        int counter = 0;
        System.out.println("INFO in TPTPWriter.translate(): # statements not in rules: " + SuokifVisitor.result.keySet().size());
        String id, tptp;
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(args[2])))) {
            for (FormulaAST fast : SuokifVisitor.result.values()) {
                if (!sv.rules.contains(fast) && !fast.isDoc && !fast.comment) {
                    if (fast.parsedFormula == null) {
                        System.err.println("Error in TPTPWriter.translate(): non rules - null formula " + fast);
                        continue;
                    }
                    id = "kb_" + FileUtil.noExt(FileUtil.noPath(fast.sourceFile)) + "_" + fast.startLine + "_" + counter++;
                    // TODO this may not fit with proof processing that uses suffix to find original formula
                    tptp = tptpW.visitSentence(fast.parsedFormula);
                    if (!StringUtil.emptyString(tptp)) {
    //                    System.out.println("fof(" + id + ",axiom," + tptp + ").");                                 // 3. Translate non-rules
                        pw.println("fof(" + id + ",axiom," + tptp + ").");
                    }
                    else
                        System.err.println("Error in TPTPWriter.translate(): null translation for " + fast.parsedFormula);
                }
            }
            end = (System.currentTimeMillis()-start)/1000;
            System.out.println("INFO in TPTPWriter.translate(): # time to translate non-rules: " + end);
            start = System.currentTimeMillis();
            System.out.println("INFO in TPTPWriter.translate(): # statements in rules after preprocess: " + rules.size());
            for (FormulaAST fast : rules) {
                if (!fast.isDoc && !fast.comment) {
                    if (fast.parsedFormula == null)  {
                        System.err.println("Error in TPTPWriter.translate(): rules - null formula " + fast);
                        continue;
                    }
                    id = "kb_" + FileUtil.noExt(FileUtil.noPath(fast.sourceFile)) + "_" + fast.startLine + "_" + counter++;
                    // TODO this may not fit with proof processing that uses suffix to find original formula
                    tptp = tptpW.visitSentence(fast.parsedFormula);
                    if (!StringUtil.emptyString(tptp)) {
    //                    System.out.println("fof(" + id + ",axiom," + tptp + ").");                                 // 4. Translate rules
                        pw.println("fof(" + id + ",axiom," + tptp + ").");
                    }
                    else
                        System.err.println("Error in TPTPWriter.translate(): null translation for " + fast.parsedFormula);
                }
            }
        } catch (IOException ex) {
            System.err.printf("Error in TPTPWriter.translate(): %s%n", ex);
        }
        end = (System.currentTimeMillis()-start)/1000;
        System.out.println("INFO in TPTPWriter.translate(): # time to translate rules: " + end);
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        System.out.println("INFO in TPTPWriter.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            if (args != null && args.length > 0 && args[0].contains("r") || args[0].contains("t"))
                translate(args);
            else
                showHelp();
        }
    }
}
