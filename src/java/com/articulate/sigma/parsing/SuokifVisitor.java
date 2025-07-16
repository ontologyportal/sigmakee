package com.articulate.sigma.parsing;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.MapUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class SuokifVisitor extends AbstractParseTreeVisitor<String> {

    public static boolean debug = false;
    public static Map<Integer,FormulaAST> result = new HashMap<>();
    public static Map<String,Set<FormulaAST>> keys = new HashMap<>();

    public Set<FormulaAST> hasRowVar;
    public Set<FormulaAST> hasPredVar;
    public Set<FormulaAST> hasNumber;
    public Set<FormulaAST> multipleRowVar;
    public Set<FormulaAST> multiplePredVar;
    public Set<FormulaAST> rules;
    public Set<FormulaAST> nonRulePredRow; // the formulas that are not rules
    public Set<FormulaAST> ground; // formulas with no variables
    public Set<String> errors;

    /** ***************************************************************
     */
    public SuokifVisitor() {

        result.clear();
        keys.clear();

        hasRowVar = new HashSet<>();
        hasPredVar = new HashSet<>();
        hasNumber = new HashSet<>();
        multipleRowVar = new HashSet<>();
        multiplePredVar = new HashSet<>();
        rules = new HashSet<>();
        nonRulePredRow = new HashSet<>(); // the formulas that are not rules
        ground = new HashSet<>(); // formulas with no variables
        errors = new TreeSet<>();
    }

    /** ***************************************************************
     * Parse SUO-KIF from a file
     * @param fname the path to a file containing SUO-KIF
     * @return an instance for accessing a Map that should have a single formula
     */
    public static SuokifVisitor parseFile(File fname) {

        CharStream inputStream;
        SuokifVisitor visitor = new SuokifVisitor();
        try {
            inputStream = CharStreams.fromFileName(fname.getAbsolutePath());
            visitor.parse_common(inputStream);
        }
        catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return visitor;
    }

    /** ***************************************************************
     * Parse a single formula and use this SuokifVisitor to process
     * as the cached information for the formula.
     * @param input the String containing SUO-KIF to process
     * @return an instance for accessing a Map that should have a single formula
     */
    public static SuokifVisitor parseString(String input) {

        if (debug) System.out.println(input);
        CharStream inputStream = CharStreams.fromString(input);
        SuokifVisitor visitor = new SuokifVisitor();
        visitor.parse_common(inputStream);
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        if (hm == null || hm.values().isEmpty()) {
            String errStr = "Error in SuokifVisitor.parseString(): no results for input:\n"  + Formula.textFormat(input);
            System.err.println(errStr);
        }
        return visitor;
    }

    /** ***************************************************************
     * Parse a single formula and use this SuokifVisitor to process
     * as the cached information for the formula. Copy the formula
     * meta-data to the new formulas.
     * @param input the Formula containing SUO-KIF to process
     * @return an instance for accessing a Map that should have a single formula
     */
    public static SuokifVisitor parseFormula(Formula input) {

        if (debug) System.out.println(input);
        SuokifVisitor visitor = SuokifVisitor.parseString(input.getFormula());
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        if (hm != null && !hm.values().isEmpty())
            for (FormulaAST f : hm.values()) {
                f.startLine = input.startLine;
                f.endLine = input.endLine;
                f.sourceFile = input.sourceFile;
            }
        return visitor;
    }

    /** Setup the main syntax processing of SUO-KIF
     *
     * @param inputStream the CharStream containing SUO-KIF
     */
    private void parse_common(CharStream inputStream) {

        SuokifLexer suokifLexer = new SuokifLexer(inputStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(suokifLexer);
        SuokifParser suokifParser = new SuokifParser(commonTokenStream);
        suokifParser.removeErrorListeners();

        // Custom listener that stops parsing on first syntax error
        suokifParser.addErrorListener(new SuokifParserErrorListener());
        SuokifParser.FileContext fileContext;
        try {
            fileContext = suokifParser.file();
            visitFile(fileContext);
            System.out.println("Successful parse, no errors");
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            errors.add(ex.getMessage());
        }
    }

    /** ***************************************************************
     */
    public void addToKeys(String k, FormulaAST f) {

        Set<FormulaAST> values;
        if (!keys.containsKey(k)) {
            values = new HashSet<>();
            keys.put(k,values);
        }
        else
            values = keys.get(k);
        values.add(f);
    }

    /** ***************************************************************
     */
    public void generateRuleKeys(FormulaAST f) {

        for (String s : f.antecedentTerms)
            addToKeys(KIF.createKey(s,true,false,0,0),f);
        for (String s : f.consequentTerms)
            addToKeys(KIF.createKey(s,false,true,0,0),f);
    }

    /** ***************************************************************
     */
    public void generateNonRuleKeys(FormulaAST f) {

        Map<Integer, Set<SuokifParser.ArgumentContext>> args;
        Set<SuokifParser.ArgumentContext> argSet;
        for (String pred : f.argMap.keySet()) {
            args = f.argMap.get(pred);
            for (Integer argnum : args.keySet()) {
                argSet = args.get(argnum);
                for (SuokifParser.ArgumentContext arg : argSet) {
                    if (isConstantArgument(arg))
                        addToKeys(KIF.createKey(arg.getText(),false,false,argnum,0),f);
                }
            }
        }
    }

    /** ***************************************************************
     * file : (sentence | comment)+ EOF ;
     * Fill maps that represent each statement or comment in the same order
     * as the file.  Note that comments at the end of a SUO-KIF line that include
     * a statement are recorded as occurring on the next line
     */
    public void visitFile(SuokifParser.FileContext context) {

        if (debug) System.out.println("visitFile() Visiting file: " + context.getText());
        if (debug) System.out.println("visitFile() # children: " + context.children.size());
        if (debug) System.out.println("visitFile() text: " + context.getText());
        int counter = 0;
        FormulaAST f;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitFile() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                f = visitSentence((SuokifParser.SentenceContext) c);
                f.parsedFormula = (SuokifParser.SentenceContext) c;
                f.startLine = ((SuokifParser.SentenceContext) c).start.getLine();
                f.endLine = ((SuokifParser.SentenceContext) c).stop.getLine();
                f.sourceFile = ((SuokifParser.SentenceContext) c).start.getTokenSource().getSourceName();
                result.put(counter++,f);
                if (f.predVarCache != null && !f.predVarCache.isEmpty()) {
                    hasPredVar.add(f);
                    if (f.predVarCache.size() > 1)
                        multiplePredVar.add(f);
                }
                if (f.rowVarCache != null && !f.rowVarCache.isEmpty()) {
                    hasRowVar.add(f);
                    if (f.rowVarCache.size() > 1)
                        multipleRowVar.add(f);
                }
                if (f.isRule) {
                    rules.add(f);
                    generateRuleKeys(f);
                }
                else {
                    nonRulePredRow.add(f);
                    generateNonRuleKeys(f);
                }
                if (f.containsNumber)
                    hasNumber.add(f);
                if (f.isGround())
                    ground.add(f);
            }
            if (c instanceof SuokifParser.CommentContext) {
                f = visitComment((SuokifParser.CommentContext) c);
                result.put(counter++,f);
            }
        }
        if (debug) System.out.println("visitFile() return file: " + result);
        if (debug) System.out.println();
        if (debug) System.out.println("has pred var: " + hasPredVar);
        if (debug) System.out.println();
        if (debug) System.out.println("has row var: " + hasRowVar);
        if (debug) System.out.println();
        if (debug) System.out.println("rules: " + rules);
        if (debug) System.out.println();
        if (debug) System.out.println("multiple pred var: " + multiplePredVar);
        if (debug) System.out.println();
        if (debug) System.out.println("multiple row var: " + multipleRowVar);
        if (debug) System.out.println();
    }

    /** ***************************************************************
     * sentence : (relsent | logsent | quantsent | variable) ;
     */
    public FormulaAST visitSentence(SuokifParser.SentenceContext context) {

        if (context == null || context.children == null) return null;
        if (debug) System.out.println("visitSentence() Visiting sentence: " + context.getText());
        if (debug) System.out.println("visitSentence() # children: " + context.children.size());
        if (debug) System.out.println("visitSentence() text: " + context.getText());
        FormulaAST f = null;
        for (ParseTree c : context.children) {
            f = null;
            if (debug)  System.out.println("child of sentence: " + c.getClass().getName());
            if (c instanceof SuokifParser.RelsentContext)
                f = visitRelsent((SuokifParser.RelsentContext) c);
            if (c instanceof SuokifParser.LogsentContext)
                f = visitLogsent((SuokifParser.LogsentContext) c);
            if (c instanceof SuokifParser.QuantsentContext)
                f = visitQuantsent((SuokifParser.QuantsentContext) c);
            if (c instanceof SuokifParser.VariableContext)
                f = visitVariable((SuokifParser.VariableContext) c);
            if (f != null) {
                f.startLine = ((ParserRuleContext) c).getStart().getLine();
            }
        }
        if (debug) System.out.println("visitSentence() return sentence: " + f);
        if (debug) System.out.println("visitSentence() return containsNumber: " + f.containsNumber);
        if (debug) System.out.println("visitSentence() rowvarstruct: " + f.rowVarStructs);
        f.unquantVarsCache = f.allVarsCache;
        f.quantVarsCache.addAll(f.existVarsCache);
        f.quantVarsCache.addAll(f.univVarsCache);
        f.unquantVarsCache.removeAll(f.quantVarsCache);
        return f;
    }

    /** ***************************************************************
     */
    public FormulaAST visitComment(SuokifParser.CommentContext context) {

        if (debug) System.out.println("Visiting comment: " + context.getText());
        if (debug) System.out.println(context.COMMENT().getText() + "\n");
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = FormulaAST.createComment(context.getText());
        return f;
    }

    /** ***************************************************************
     * An argument that is a constant should look like
     * argument -> term -> IDENTIFIER with no other children
     * at each level
     *
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    private boolean isConstantArgument(SuokifParser.ArgumentContext c) {

        SuokifParser.SentenceContext sc;
        ParseTree ptv;
        SuokifParser.TermContext tc;
        for (ParseTree pt2 : c.children) {
            if (debug) System.out.println("isRowVarArgument(): Visiting argument: " + pt2.getText());
            if (debug) System.out.println("isRowVarArgument(): Visiting argument type: " + pt2.getClass().getName());
            if (pt2 instanceof SuokifParser.SentenceContext) { // note the grammar has an ambiguous path where this should be term
                sc = (SuokifParser.SentenceContext) pt2;
                if (debug) System.out.println("isRowVarArgument(): Visiting term: " + sc.getText());
                ptv = sc.children.iterator().next();
                if (ptv instanceof SuokifParser.TermContext) {
                    tc = (SuokifParser.TermContext) ptv;
                    if (tc.IDENTIFIER() != null)
                        return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * An argument that is a row variable should look like
     * argument -> term -> variable -> ROWVAR with no other children
     * at each level
     *
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    private boolean isRowVarArgument(SuokifParser.ArgumentContext c) {

        SuokifParser.SentenceContext sc;
        ParseTree ptv;
        SuokifParser.VariableContext vc;
        for (ParseTree pt2 : c.children) {
            if (debug) System.out.println("isRowVarArgument(): Visiting argument: " + pt2.getText());
            if (debug) System.out.println("isRowVarArgument(): Visiting argument type: " + pt2.getClass().getName());
            if (pt2 instanceof SuokifParser.SentenceContext) { // note the grammar has an ambiguous path where this should be term
                sc = (SuokifParser.SentenceContext) pt2;
                if (debug) System.out.println("isRowVarArgument(): Visiting sentence: " + sc.getText());
                ptv = sc.children.iterator().next();
                if (ptv instanceof SuokifParser.VariableContext) {
                    vc = (SuokifParser.VariableContext) ptv;
                    if (debug) System.out.println("isRowVarArgument(): Visiting variable: " + vc.getText());
                    if (vc.ROWVAR() != null)
                        return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * An argument that is a sentence that is not a variable means
     * it's higher-order
     *
     * argument : (sentence | term) ;
     * sentence : (relsent | logsent | quantsent | variable) ;
     */
    private boolean nonTermArg(SuokifParser.SentenceContext c) {

        for (ParseTree pt2 : c.children) {
            if (debug) System.out.println("nonTermArg(): Visiting argument: " + pt2.getText());
            if (debug) System.out.println("nonTermArg(): Visiting argument type: " + pt2.getClass().getName());
            if (!(pt2 instanceof SuokifParser.VariableContext))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * relsent : ('(' IDENTIFIER argument+ ')') | ('(' variable argument+ ')')  ;
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     * Set the types of any variables that appear in an instance or subclass
     * declaration
     */
    public FormulaAST visitRelsent(SuokifParser.RelsentContext context) {

        FormulaAST formast = new FormulaAST(), f;
        Set<FormulaAST.RowStruct> newRowVarStructs = new HashSet<>();
        formast.predVarCache = new HashSet<>();
        if (debug) System.out.println("Visiting relsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        String pred = null;
        int argnum = 0;
        List<String> argList = new ArrayList<>();
        Set<FormulaAST.ArgStruct> constantTerms = new HashSet<>(); // tracks if a constant is an argument
        Map<Integer,Set<SuokifParser.ArgumentContext>> args = new HashMap<>();
        if (context.IDENTIFIER() != null) {
            pred = context.IDENTIFIER().toString();
            sb.append(pred).append(" ");
            if (Formula.DOC_PREDICATES.contains(pred))
                formast.isDoc = true;
            formast.termCache.add(pred);
            formast.relation = pred;
            argnum++;
            if (debug) System.out.println("identifier: " + pred);
        }
        SuokifParser.ArgumentContext ac;
        FormulaAST.RowStruct rs;
        FormulaAST.ArgStruct as;
        Set<SuokifParser.ArgumentContext> argAt;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("child of relsent: " + c.getClass().getName());
            if (c instanceof SuokifParser.VariableContext) {
                f = visitVariable((SuokifParser.VariableContext) c);
                formast.mergeFormulaAST(f);
                pred = f.getFormula();
                if (argnum == 0)
                    formast.predVarCache.add(f.getFormula());
                formast.allVarsCache.add(f.getFormula());
                sb.append(f.getFormula()).append(" ");
            }
            if (c instanceof SuokifParser.ArgumentContext) {
                ac = (SuokifParser.ArgumentContext) c;
                f = visitArgument(ac);
                if (debug) System.out.println("ac: " + ac.getText());
                if (debug) System.out.println("isRowVar: " + isRowVarArgument(ac));
                if (isRowVarArgument(ac)) {
                    f.rowvarLiterals.add(context);
                    rs = f.new RowStruct();
                    rs.pred = pred;
                    rs.rowvar = ac.getText();
                    if (debug) System.out.println("rs: " + rs);
                    // rs.literal = f.getFormula(); // formulas isn't complete so can't do it yet
                    if (debug) System.out.println("visitRelsent(): adding row var struct (to new): " + rs);
                    newRowVarStructs.add(rs); // can't add them to the formula's list until we have the whole literal
                }
                else if (isConstantArgument(ac)) {
                    as = f.new ArgStruct();
                    as.pred = pred;
                    as.constant = ac.getText();
                    as.argPos = argnum;
                    constantTerms.add(as);
                }
                argAt = args.get(argnum);
                if (argAt == null)
                    argAt = new HashSet<>();
                argAt.add((SuokifParser.ArgumentContext) c);
                args.put(argnum,argAt);
                argnum++;
                sb.append(f.getFormula()).append(" ");
                argList.add(f.getFormula());
                if (debug) System.out.println("Visiting relsent: containsNumber: " + f.containsNumber);
                formast.mergeFormulaAST(f);
            }
        }
        if (pred.equals("instance") && argList.size() > 1 && Formula.isVariable(argList.get(0)) && Formula.isTerm(argList.get(1))) {
            if (debug) System.out.println("SuokifVisitor.visitRelsent(): found explicit instance: argList: " + argList);
            MapUtils.addToMap(formast.varTypes, argList.get(0), argList.get(1));
            MapUtils.addToMap(formast.explicitTypes,argList.get(0), argList.get(1));
        }
        if (pred.equals("subclass") && argList.size() > 1 && Formula.isVariable(argList.get(0)) && Formula.isTerm(argList.get(1))) {
            MapUtils.addToMap(formast.varTypes, argList.get(0), argList.get(1) + "+");
            MapUtils.addToMap(formast.explicitTypes,argList.get(0), argList.get(1) + "+");
        }
        formast.argMap.put(pred,args);
        sb.deleteCharAt(sb.length()-1);  // delete trailing space
        sb.append(")");
        formast.setFormula(sb.toString());
        if (debug) System.out.println("pred: " + pred);
        if (debug) if (argList != null && argList.size() > 1)
            System.out.println("arg 1: " + argList.get(0));
        if (debug) if (argList != null && argList.size() > 2)
            System.out.println("arg 2: " + argList.get(1));
        if (debug) System.out.println("return relsent: " + formast);
        if (debug) System.out.println("varTypes: " + formast.varTypes);

        for (FormulaAST.RowStruct r : newRowVarStructs) { // now we have the whole literal to add to the structure
            r.literal = formast.getFormula();
            r.arity = argnum-1;
            if (debug) System.out.println("visitRelsent(): adding row var struct: " + r);
            formast.addRowVarStruct(r.rowvar,r);
        }
        for (FormulaAST.ArgStruct a : constantTerms) {
            a.literal = formast.getFormula();
            formast.constants.put(a.constant, a);
        }
        if (debug) System.out.println("visitRelsent(): returning with row var struct: " + formast.rowVarStructs);
        if (debug) System.out.println("visitRelsent: returning with containsNumber: " + formast.containsNumber);
        return formast;
    }

    /** ***************************************************************
     * argument : (sentence | term) ;
     */
    public FormulaAST visitArgument(SuokifParser.ArgumentContext context) {

        if (debug) System.out.println("Visiting argument: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (context.children.size() != 1)
            System.err.println("Error in visitArgument() wrong # children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("child of argument: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                f = visitSentence((SuokifParser.SentenceContext) c);
                if (nonTermArg((SuokifParser.SentenceContext) c))
                    f.higherOrder = true;
            }
            if (c instanceof SuokifParser.TermContext)
                f = visitTerm((SuokifParser.TermContext) c);
        }
        if (debug) System.out.println("Visiting argument: returning with containsNumber: " + f.containsNumber);
        if (debug) System.out.println("Visiting argument: higherOrder: " + f.higherOrder);
        return f;
    }

    /** ***************************************************************
     * logsent :  (notsent | andsent | orsent | implies | iff | eqsent) ;
     */
    public FormulaAST visitLogsent(SuokifParser.LogsentContext context) {

        if (debug) System.out.println("Visiting logsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (context.children.size() != 1)
            System.err.println("Error in visitLogsent() wrong # children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = null;
         for (ParseTree c : context.children) {
             if (debug) System.out.println("visitLogsent() child: " + c.getClass().getName());
             if (c instanceof SuokifParser.NotsentContext)
                 f = visitNotsent((SuokifParser.NotsentContext) c);
             if (c instanceof SuokifParser.AndsentContext)
                 f = visitAndsent((SuokifParser.AndsentContext) c);
             if (c instanceof SuokifParser.OrsentContext)
                 f = visitOrsent((SuokifParser.OrsentContext) c);
             if (c instanceof SuokifParser.ImpliesContext)
                 f = visitImplies((SuokifParser.ImpliesContext) c);
             if (c instanceof SuokifParser.IffContext)
                 f = visitIff((SuokifParser.IffContext) c);
             if (c instanceof SuokifParser.EqsentContext)
                 f = visitEqsent((SuokifParser.EqsentContext) c);
         }
         return f;
    }

    /** ***************************************************************
     * notsent : '(' 'not' sentence ')' ;
     */
    public FormulaAST visitNotsent(SuokifParser.NotsentContext context) {

        FormulaAST f = null;
        if (debug) System.out.println("Visiting Notsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitNotsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext)
                f = visitSentence((SuokifParser.SentenceContext) c);
        }
        f.setFormula("(not " + f.getFormula() + ")");
        return f;
    }

    /** ***************************************************************
     * andsent : '(' 'and' sentence sentence+ ')' ;
     */
    public FormulaAST visitAndsent(SuokifParser.AndsentContext context) {

        if (debug) System.out.println("Visiting Andsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        List<FormulaAST> ar = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        FormulaAST f;
        sb.append("(and ");
        for (ParseTree c : context.children) {
            if (debug) System.out.println("child of andsent: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                f = visitSentence((SuokifParser.SentenceContext) c);
                if (debug) System.out.println("inside visitAndsent(): rowvarstruct: " + f.rowVarStructs);
                ar.add(f);
                sb.append(f.getFormula()).append(" ");
            }
        }
        sb.delete(sb.length()-1,sb.length());
        sb.append(")");
        f = new FormulaAST();
        f.setFormula(sb.toString());
        f.mergeFormulaAST(ar);
        if (debug) System.out.println("visitAndsent(): rowvarstruct: " + f.rowVarStructs);
        return f;
    }

    /** ***************************************************************
     * orsent : '(' 'or' sentence sentence+ ')' ;
     */
    public FormulaAST visitOrsent(SuokifParser.OrsentContext context) {

        if (debug) System.out.println("Visiting Orsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        List<FormulaAST> ar = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        FormulaAST f;
        sb.append("(or ");
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitOrsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
                f = visitSentence((SuokifParser.SentenceContext) c);
                ar.add(f);
                sb.append(f.getFormula()).append(" ");
            }
        }
        sb.delete(sb.length()-1,sb.length());
        sb.append(")");
        f = new FormulaAST();
        f.setFormula(sb.toString());
        f.mergeFormulaAST(ar);
        return f;
    }

    /** ***************************************************************
     * implies :  '(' '=>' sentence sentence ')' ;
     */
    public FormulaAST visitImplies(SuokifParser.ImpliesContext context) {

        if (debug) System.out.println("Visiting Implies: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f1 = null;
        FormulaAST f2 = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitImplies() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.SentenceContext) {
               if (f1 == null) {
                    f1 = visitSentence((SuokifParser.SentenceContext) c);
                    f1.antecedentTerms.addAll(f1.constants.keySet());
                }
                else {
                    f2 = visitSentence((SuokifParser.SentenceContext) c);
                    f2.consequentTerms.addAll(f2.constants.keySet());
                }
            }
        }
        f1.setFormula("(=> " + f1.getFormula() + " " + f2.getFormula() + ")");
        f1.mergeFormulaAST(f2);
        f1.isRule = true;
        if (debug) System.out.println("visitImplies: returning with containsNumber: " + f1.containsNumber);
        return f1;
    }

    /** ***************************************************************
     * iff : '(' '<=>' sentence sentence ')' ;
     */
    public FormulaAST visitIff(SuokifParser.IffContext context) {

        if (debug) System.out.println("Visiting Iff: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f1 = null;
        FormulaAST f2 = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitIff() child: " + c.getClass().getName());
             if (c instanceof SuokifParser.SentenceContext) {
                if (f1 == null) {  // this is a bit questionable since it makes the lexically first element the antecedent
                    f1 = visitSentence((SuokifParser.SentenceContext) c);
                    f1.antecedentTerms.addAll(f1.constants.keySet());
                }
                else {
                    f2 = visitSentence((SuokifParser.SentenceContext) c);
                    f2.consequentTerms.addAll(f2.constants.keySet());
                }
             }
        }
        f1.setFormula("(<=> " + f1.getFormula() + " " + f2.getFormula() + ")");
        f1.mergeFormulaAST(f2);
        f1.isRule = true;
        return f1;
    }

    /** ***************************************************************
     * eqsent : '(' 'equal' term term ')' ;
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public FormulaAST visitEqsent(SuokifParser.EqsentContext context) {

        if (debug) System.out.println("Visiting Eqsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f1 = null;
        FormulaAST f2 = null;
        SuokifParser.TermContext c1 = null;
        SuokifParser.TermContext c2 = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitEqsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.TermContext) {
               if (f1 == null) {
                    f1 = visitTerm((SuokifParser.TermContext) c);
                    c1 = (SuokifParser.TermContext) c;
                }
                else {
                    f2 = visitTerm((SuokifParser.TermContext) c);
                    c2 = (SuokifParser.TermContext) c;
                }
            }
        }
        List<SuokifParser.TermContext> oneEq = new ArrayList<>();
        oneEq.add(c1);
        oneEq.add(c2);
        f1.eqList.add(oneEq);
        f1.setFormula("(equal " + f1.getFormula() + " " + f2.getFormula() + ")");
        f1.mergeFormulaAST(f2);
        return f1;
    }

    /** ***************************************************************
     * quantsent : (forall | exists) ;
     */
    public FormulaAST visitQuantsent(SuokifParser.QuantsentContext context) {

        if (debug) System.out.println("Visiting quantsent: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = null;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitQuantsent() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.ForallContext)
               f = visitForall((SuokifParser.ForallContext) c);
            if (c instanceof SuokifParser.ExistsContext)
               f = visitExists((SuokifParser.ExistsContext) c);
        }
        return f;
    }

    /** ***************************************************************
     * forall : '(' 'forall' '(' variable+ ')' sentence ')' ;
     */
    public FormulaAST visitForall(SuokifParser.ForallContext context) {

        if (debug) System.out.println("Visiting Forall: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        StringBuilder varlist = new StringBuilder();
        Set<String> quant = new HashSet<>();
        FormulaAST f = null, farg = null;
        String body = null;
        String[] vars;
        FormulaAST.RowStruct rs;
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitForall() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.VariableContext) {
                farg = visitVariable((SuokifParser.VariableContext) c);
                quant.add(farg.getFormula());
                varlist.append(farg.getFormula()).append(" ");
            }
            if (c instanceof SuokifParser.SentenceContext) {
                varlist.deleteCharAt(varlist.length()-1); // remove trailing space
                f = visitSentence((SuokifParser.SentenceContext) c);
                body = f.getFormula();
                if (debug) System.out.println("visitForall(): farg: " + farg);
                if (varlist != null && varlist.toString().contains("@")) {
                    f.rowvarLiterals.add(context);
                    vars = varlist.toString().split(" ");
                    for (String var : vars) {
                        if (var.startsWith("@")) {
                            rs = f.new RowStruct();
                            rs.pred = "__quantList";
                            rs.rowvar = var;
                            if (debug) System.out.println("rs: " + rs);
                            rs.literal = varlist.toString();
                            if (debug) System.out.println("visitForall(): adding row var struct: " + rs);
                            f.addRowVarStruct(rs.rowvar, rs);
                        }
                    }
                }
            }
        }
        f.univVarsCache.addAll(quant);
        f.allVarsCache.addAll(quant);
        f.setFormula("(forall (" + varlist + ") " + body + ")");
        if (debug) System.out.println("visitForall(): varlist: " + varlist);
        return f;
    }

    /** ***************************************************************
     * exists : '(' 'exists' '(' variable+ ')' sentence ')' ;
     */
    public FormulaAST visitExists(SuokifParser.ExistsContext context) {

        if (debug) System.out.println("Visiting Exists: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        StringBuilder varlist = new StringBuilder();
        Set<String> quant = new HashSet<>();
        FormulaAST f = null, farg = null;
        String body = null;
        FormulaAST.RowStruct rs;
        if (debug) System.out.println("text: " + context.getText());
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitExists() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.VariableContext) {
                farg = visitVariable((SuokifParser.VariableContext) c);
                quant.add(farg.getFormula());
                varlist.append(farg.getFormula()).append(" ");
            }
            if (c instanceof SuokifParser.SentenceContext) {
                f = visitSentence((SuokifParser.SentenceContext) c);
                body = f.getFormula();
                if (farg != null && farg.getFormula().contains("@")) {
                    f.rowvarLiterals.add(context);
                    rs = f.new RowStruct();
                    rs.pred = "__quantList";
                    rs.rowvar = farg.toString();
                    if (debug) System.out.println("rs: " + rs);
                    rs.literal = varlist.toString();
                    if (debug) System.out.println("visitForall(): adding row var struct: " + rs);
                    f.addRowVarStruct(rs.rowvar,rs);
                }
            }
        }
        varlist.delete(varlist.length()-1,varlist.length());
        f.existVarsCache.addAll(quant);
        f.allVarsCache.addAll(quant);
        f.setFormula("(exists (" + varlist + ") " + body + ")");
        return f;
    }

    /** ***************************************************************
     * variable : (REGVAR | ROWVAR) ;
     */
    public FormulaAST visitVariable(SuokifParser.VariableContext context) {

        FormulaAST f = new FormulaAST();
        if (debug) System.out.println("Visiting variable: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        if (context.REGVAR() != null) {
            String regvar = context.REGVAR().toString();
            f.setFormula(regvar);
            f.allVarsCache.add(regvar);
            if (debug) System.out.println("regvar: " + f);
        }
        if (context.ROWVAR() != null) {
            String row = context.ROWVAR().toString();
            f.setFormula(row);
            f.rowVarCache = new HashSet<>();
            f.rowVarCache.add(row);
            if (debug) System.out.println("rowv: " + row);
        }
        f.isGround = false;
        return f;
    }

    /** ***************************************************************
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public FormulaAST visitTerm(SuokifParser.TermContext context) {

        FormulaAST f = new FormulaAST();
        if (debug) System.out.println("visitTerm() Visiting Term: " + context.getText());
        if (debug) System.out.println("visitTerm() # children: " + context.children.size());
        if (context.children.size() != 1)
            System.err.println("Error in visitTerm() wrong # children: " + context.children.size());
        if (debug) System.out.println("visitTerm() text: " + context.getText());
        if (context.IDENTIFIER() != null) {
            String ident = context.IDENTIFIER().toString();
            if (debug) System.out.println("visitTerm() identifier: " + ident);
            f.termCache.add(ident);
            f.setFormula(ident);
        }
        if (context.FUNWORD() != null) {
            String funword = context.FUNWORD().toString();
            if (debug) System.out.println("visitTerm() funword: " + funword);
            f.termCache.add(funword);
            f.setFormula(funword);
        }
        for (ParseTree c : context.children) { // there should be only one child
            if (debug) System.out.println("visitTerm() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.FuntermContext)
                f = visitFunterm((SuokifParser.FuntermContext) c);
            if (c instanceof SuokifParser.VariableContext) {
                f = visitVariable((SuokifParser.VariableContext) c);
                f.allVarsCache.add(f.getFormula());
            }
            if (c instanceof SuokifParser.StringContext)
                f = visitString((SuokifParser.StringContext) c);
            if (c instanceof SuokifParser.NumberContext) {
                f = visitNumber((SuokifParser.NumberContext) c);
                f.containsNumber = true;
                if (debug) System.out.println("visitTerm() found a number: " + c.getText());
            }
        }
        return f;
    }

    /** ***************************************************************
     * funterm : '(' FUNWORD argument+ ')' ;
     */
    public FormulaAST visitFunterm(SuokifParser.FuntermContext context) {

        if (debug) System.out.println("Visiting funterm: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        StringBuilder sb = new StringBuilder();
        FormulaAST formast = new FormulaAST();
        Set<FormulaAST.RowStruct> newRowVarStructs = new HashSet<>();
        String funword = null;
        if (context.FUNWORD() != null) {
            funword = context.FUNWORD().toString();
            if (debug) System.out.println("funword: " + funword);
            formast.termCache.add(funword);
            formast.relation = funword;
            sb.append("(").append(funword).append(" ");
        }
        int argnum = 1;
        Map<Integer,Set<SuokifParser.ArgumentContext>> args = new HashMap<>();
        List<FormulaAST> arf = new ArrayList<>();
        FormulaAST farg;
        SuokifParser.ArgumentContext ac;
        FormulaAST.RowStruct rs;
        Set<SuokifParser.ArgumentContext> argAt;
        for (ParseTree c : context.children) {
            if (debug) System.out.println("visitFunterm() child: " + c.getClass().getName());
            if (c instanceof SuokifParser.ArgumentContext) {
                ac = (SuokifParser.ArgumentContext) c;
                farg = visitArgument(ac);
                if (isRowVarArgument(ac)) {
                    farg.rowvarLiterals.add(context);
                    rs = farg.new RowStruct();
                    rs.pred = funword;
                    rs.rowvar = ac.getText();
                    // rs.literal = f.getFormula(); // formulas isn't complete so can't do it yet
                    if (debug) System.out.println("visitFunterm(): adding row var struct (to new): " + rs);
                    newRowVarStructs.add(rs); // can't add them to the formula's list until we have the whole literal
                }
                argAt = args.get(argnum);
                if (argAt == null)
                    argAt = new HashSet<>();
                argAt.add((SuokifParser.ArgumentContext) c);
                args.put(argnum,argAt);
                argnum++;
                arf.add(farg);
                sb.append(farg.getFormula()).append(" ");
                if (debug) System.out.println("Visiting funterm: containsNumber: " + farg.containsNumber);
                formast.mergeFormulaAST(farg);
            }
        }
        formast.argMap.put(funword,args);
        sb.delete(sb.length()-1,sb.length());
        sb.append(")");
        formast.setFormula(sb.toString());
        formast.mergeFormulaAST(arf);
        formast.isFunctional = true;
        for (FormulaAST.RowStruct r : newRowVarStructs) { // now we have the whole literal to add to the structure
            r.literal = formast.getFormula();
            r.arity = argnum-1;
            if (debug) System.out.println("visitFunterm(): adding row var struct: " + r);
            formast.addRowVarStruct(r.rowvar,r);
        }
        if (debug) System.out.println("visitFunterm(): returning with row var struct: " + formast.rowVarStructs);
        if (debug) System.out.println("Visiting funterm: returning with containsNumber: " + formast.containsNumber);
        return formast;
    }

    /** ***************************************************************
     */
    public FormulaAST visitString(SuokifParser.StringContext context) {

        if (debug) System.out.println("Visiting string: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = new FormulaAST();
        f.setFormula(context.getText());
        return f;
    }

    /** ***************************************************************
     */
    public FormulaAST visitNumber(SuokifParser.NumberContext context) {

        if (debug) System.out.println("Visiting number: " + context.getText());
        if (debug) System.out.println("# children: " + context.children.size());
        if (debug) System.out.println("text: " + context.getText());
        FormulaAST f = new FormulaAST();
        f.setFormula(context.getText());
        return f;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("SuokifVisitor class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -f <fname> - parse the file");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        System.out.println("INFO in SuokifVisitor.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            if (args != null && args.length > 1 && args[0].equals("-f")) {
                SuokifVisitor.parseFile(Paths.get(args[1]).toFile());
                Map<Integer,FormulaAST> hm = SuokifVisitor.result;
                StringBuilder sb = new StringBuilder();
                for (FormulaAST f : hm.values())
                    sb.append(f.getFormula()).append("\n");
                System.out.println("result: " + sb);
            }
            else
                showHelp();
        }
    }
}
