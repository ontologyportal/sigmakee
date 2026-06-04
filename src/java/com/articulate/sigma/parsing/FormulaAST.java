package com.articulate.sigma.parsing;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import org.antlr.v4.runtime.ParserRuleContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class FormulaAST implements Comparable, Serializable {

    // ---------------------------------------------------------------
    // Static constants — copied verbatim from Formula
    // ---------------------------------------------------------------

    public static boolean debug = false;

    private static final Pattern HAS_WHITESPACE  = Pattern.compile(".*\\s.*");
    private static final Pattern STARTS_WITH_AND = Pattern.compile("^\\s*\\(\\s*and.*");
    private static final Pattern WHITESPACE_NORM = Pattern.compile("\\s+");

    public static final String AND    = "and";
    public static final String OR     = "or";
    public static final String XOR    = "xor";
    public static final String NOT    = "not";
    public static final String IF     = "=>";
    public static final String IFF    = "<=>";
    public static final String UQUANT = "forall";
    public static final String EQUANT = "exists";
    public static final String EQUAL  = "equal";
    public static final String GT     = "greaterThan";
    public static final String GTET   = "greaterThanOrEqualTo";
    public static final String LT     = "lessThan";
    public static final String LTET   = "lessThanOrEqualTo";

    public static final String KAPPAFN    = "KappaFn";
    public static final String PLUSFN     = "AdditionFn";
    public static final String MINUSFN    = "SubtractionFn";
    public static final String TIMESFN    = "MultiplicationFn";
    public static final String DIVIDEFN   = "DivisionFn";
    public static final String FLOORFN    = "FloorFn";
    public static final String ROUNDFN    = "RoundFn";
    public static final String CEILINGFN  = "CeilingFn";
    public static final String REMAINDERFN = "RemainderFn";
    public static final String SKFN       = "SkFn";
    public static final String SK_PREF    = "Sk";
    public static final String FN_SUFF    = "Fn";
    public static final String V_PREF     = "?";
    public static final String R_PREF     = "@";
    public static final String VX         = V_PREF + "X";
    public static final String VVAR       = V_PREF + "VAR";
    public static final String RVAR       = R_PREF + "ROW";

    public static final String LP    = "(";
    public static final String RP    = ")";
    public static final String SPACE = " ";

    public static final String LOG_TRUE  = "True";
    public static final String LOG_FALSE = "False";

    public static final String TERM_MENTION_SUFFIX  = "__m";
    public static final String CLASS_SYMBOL_SUFFIX  = "__t";
    public static final String TERM_SYMBOL_PREFIX   = "s__";
    public static final String TERM_VARIABLE_PREFIX = "V__";

    public static final List<String> LOGICAL_OPERATORS = Arrays.asList(
            UQUANT, EQUANT, AND, OR, XOR, NOT, IF, IFF);
    public static final List<String> COMPARISON_OPERATORS = Arrays.asList(
            EQUAL, GT, GTET, LT, LTET);
    public static final List<String> INEQUALITIES = Arrays.asList(GT, GTET, LT, LTET);
    public static final List<String> MATH_FUNCTIONS = Arrays.asList(
            PLUSFN, MINUSFN, TIMESFN, DIVIDEFN, FLOORFN, ROUNDFN, CEILINGFN, REMAINDERFN);
    public static final List<String> DOC_PREDICATES = Arrays.asList(
            "documentation", "comment", "format", "termFormat",
            "lexicon", "externalImage", "synonymousExternalConcept");
    public static final List<String> DEFN_PREDICATES = Arrays.asList(
            "instance", "subclass", "domain", "domainSubclass",
            "range", "rangeSubclass", "subAttribute", "subrelation");

    public static final int MAX_PREDICATE_ARITY = 7;

    // ---------------------------------------------------------------
    // Instance fields — copied verbatim from Formula
    // ---------------------------------------------------------------

    public StringBuilder qlist;
    public volatile String uaSessionId = null;
    public String sourceFile;
    public int startLine;
    public int endLine;
    public long endFilePosition = -1L;
    public Set<String> errors   = new TreeSet<>();
    public Set<String> warnings = new TreeSet<>();
    private String theFormula;
    public Derivation derivation = new Derivation();
    public boolean higherOrder  = false;
    public boolean simpleClause = false;
    public boolean comment      = false;
    public boolean isFunctional = false;
    public boolean isGround     = true;
    public boolean isTFF        = false;
    public String relation      = null;
    public List<String> stringArgs = new ArrayList<>();
    public List<FormulaAST> args      = new ArrayList<>();
    public Set<String> allVarsCache      = new HashSet<>();
    public List<Set<String>> allVarsPairCache = new ArrayList<>();
    public Set<String> quantVarsCache   = new HashSet<>();
    public Set<String> unquantVarsCache = new HashSet<>();
    public Set<String> existVarsCache   = new HashSet<>();
    public Set<String> univVarsCache    = new HashSet<>();
    public Set<String> termCache        = new HashSet<>();
    public Set<String> predVarCache     = null;
    private int cachedHashCode          = 0;
    public Set<String> rowVarCache      = null;
    public Map<String,Set<String>> varTypeCache = new HashMap<>();
    public Set<String> theTptpFormulas  = ConcurrentHashMap.newKeySet();
    public Set<String> theFofFormulas   = ConcurrentHashMap.newKeySet();
    public Set<String> theTffFormulas   = ConcurrentHashMap.newKeySet();
    public Set<String> tffSorts         = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------
    // Getters / setters for fields that Formula exposed as methods
    // ---------------------------------------------------------------

    public String getFormula() { return theFormula; }

    public void setFormula(String f) {
        theFormula = f;
        cachedHashCode = 0;
        formulaASTHashCode = 0;
        expr = null;
        args = new ArrayList<>();
        stringArgs = new ArrayList<>();
    }

    public String getSourceFile() { return this.sourceFile; }

    public void setSourceFile(String filename) { this.sourceFile = filename; }

    public int getLineNumber() { return startLine; }

    public Set<String> getErrors() { return this.errors; }

    public Set<String> getTheTptpFormulas() {
        if (!theTffFormulas.isEmpty()) return theTffFormulas;
        if (!theFofFormulas.isEmpty()) return theFofFormulas;
        return theTptpFormulas;
    }

    //TODO: copy from Formula — refactor to use Expr AST instead of String manipulation
    private void loadArguments() {
        args = new ArrayList<>();
        stringArgs = new ArrayList<>();
        if (!listP(theFormula) || empty(theFormula)) return;
        String input = theFormula.trim();
        int len = input.length();
        int i = 1;
        int end = len - 1;
        while (i < end) {
            while (i < end && Character.isWhitespace(input.charAt(i))) i++;
            if (i >= end) break;
            char ch = input.charAt(i);
            StringBuilder sb = new StringBuilder();
            if (ch == '(') {
                int level = 0;
                char prev = '0';
                boolean insideQuote = false;
                char quoteCharInForce = '0';
                while (i < len) {
                    ch = input.charAt(i);
                    if (!insideQuote) {
                        if (ch == '(') { sb.append(ch); level++; }
                        else if (ch == ')') {
                            sb.append(ch); level--;
                            if (level <= 0) { i++; break; }
                        }
                        else if ((ch == '"' || ch == '\'') && prev != '\\') {
                            sb.append(ch); insideQuote = true; quoteCharInForce = ch;
                        }
                        else sb.append(ch);
                    } else {
                        sb.append(ch);
                        if (ch == quoteCharInForce && prev != '\\') insideQuote = false;
                    }
                    prev = ch; i++;
                }
            } else if (ch == '"' || ch == '\'') {
                char openQuote = ch;
                sb.append(ch); i++;
                char prev = ch;
                while (i < len) {
                    ch = input.charAt(i);
                    sb.append(ch); i++;
                    if (ch == openQuote && prev != '\\') break;
                    prev = ch;
                }
            } else {
                while (i < end) {
                    ch = input.charAt(i);
                    if (Character.isWhitespace(ch) || ch == ')') break;
                    sb.append(ch); i++;
                }
            }
            String element = sb.toString();
            if (!element.isEmpty()) {
                stringArgs.add(element);
                args.add(new FormulaAST(element));
            } else if (i < end && input.charAt(i) == ')') {
                i++;
            }
        }
    }

    // arguments to relations in order to find the types of arg in a second pass
    // first key is a relation name, interior key is argument number starting at 1
    // transient: ANTLR ParserRuleContext objects are not Kryo-serializable
    public transient Map<String, Map<Integer, Set<SuokifParser.ArgumentContext>>> argMap = new HashMap<>();

    // all the equality statements in a formula.  The interior ArrayList must have
    // only two elements, one for each side of the equation
    // transient: ANTLR ParserRuleContext objects are not Kryo-serializable
    public transient List<List<SuokifParser.TermContext>> eqList = new ArrayList<>();

    // a map of all variables that have an explicit type declaration
    public Map<String,Set<String>> explicitTypes = new HashMap<>();

    // a map of variables and all their inferred types
    public Map<String,Set<String>> varTypes = new HashMap<>();

    // transient: ANTLR ParserRuleContext objects are not Kryo-serializable
    public transient Set<ParserRuleContext> rowvarLiterals = new HashSet<>(); // this can have a RelsentContext, FuntermContext,
      // as well as ForallContext or ExistsContext for vars in a quantifier list

    public Map<String,ArgStruct> constants = new HashMap<>(); // constants as arguments and their enclosing literal

    public Map<String,Set<RowStruct>> rowVarStructs = new HashMap<>(); // row var keys

    //public HashMap<String,String> predVarSub = new HashMap<>();

    // transient: ANTLR SentenceContext is not Kryo-serializable
    public transient SuokifParser.SentenceContext parsedFormula = null;

    /** Structured AST representation of this formula. Null until SuokifVisitor populates it. */
    public Expr expr = null;

    public boolean isDoc = false; // a documentation statement that is excluded from theorem proving
    public boolean isRule = false;
    public boolean containsNumber = false;
    public Set<String> antecedentTerms = new HashSet<>();
    public Set<String> consequentTerms = new HashSet<>();
    private int formulaASTHashCode = 0;
    private List formulaASTClausalForm = null;

    /** ***************************************************************
     */
    public FormulaAST() {

    }

    /** ***************************************************************
     */
    public FormulaAST(FormulaAST f) {

        this.endLine = f.endLine;
        this.startLine = f.startLine;
        this.sourceFile = f.sourceFile;
        this.endFilePosition = f.endFilePosition;
        this.uaSessionId = f.uaSessionId;
        this.qlist = f.qlist;
        this.errors.addAll(f.errors);
        this.warnings.addAll(f.warnings);
        this.derivation = f.derivation;
        this.setFormula(f.getFormula());
        this.expr = f.expr; // Expr nodes are immutable records — safe to share reference
        this.higherOrder = f.higherOrder;
        this.simpleClause = f.simpleClause;
        this.comment = f.comment;
        this.isFunctional = f.isFunctional;
        this.isGround = f.isGround;
        this.isTFF = f.isTFF;
        this.relation = f.relation;
        this.allVarsPairCache.addAll(f.allVarsPairCache);
        this.quantVarsCache.addAll(f.quantVarsCache);
        this.unquantVarsCache.addAll(f.unquantVarsCache);
        this.existVarsCache.addAll(f.existVarsCache);
        this.univVarsCache.addAll(f.univVarsCache);
        this.termCache.addAll(f.termCache);
        if (f.predVarCache != null) {
            this.predVarCache = new HashSet<>();
            this.predVarCache.addAll(f.predVarCache);
        }
        if (f.rowVarCache != null) {
            this.rowVarCache = new HashSet<>();
            this.rowVarCache.addAll(f.rowVarCache);
        }
        this.varTypeCache.putAll(f.varTypeCache);
        this.allVarsCache.addAll(f.allVarsCache);
        this.theTptpFormulas.addAll(f.theTptpFormulas);
        this.theFofFormulas.addAll(f.theFofFormulas);
        this.theTffFormulas.addAll(f.theTffFormulas);
        this.tffSorts.addAll(f.tffSorts);

        Map<Integer, Set<SuokifParser.ArgumentContext>> argnummap, newargnummap;
        Set<SuokifParser.ArgumentContext> largs, newargs;
        for (String pred : f.argMap.keySet()) {
            argnummap = f.argMap.get(pred);
            newargnummap = new HashMap<>();
            for (Integer argnum : argnummap.keySet()) {
                largs = argnummap.get(argnum);
                newargs = new HashSet<>();
                newargs.addAll(largs);
                newargnummap.put(argnum, newargs);
            }
            this.argMap.put(pred, newargnummap);
        }

        this.eqList.addAll(f.eqList);

        Set<String> newtypes, existingTypes;
        for (String var : f.explicitTypes.keySet()) {
            newtypes = f.explicitTypes.get(var);
            if (explicitTypes.containsKey(var))
                explicitTypes.get(var).addAll(newtypes);
            else {
                existingTypes = new HashSet<>();
                explicitTypes.put(var,existingTypes);
                existingTypes.addAll(newtypes);
            }
        }

        for (String var : f.varTypes.keySet()) {
            newtypes = f.varTypes.get(var);
            if (varTypes.containsKey(var))
                varTypes.get(var).addAll(newtypes);
            else {
                existingTypes = new HashSet<>();
                varTypes.put(var,existingTypes);
                existingTypes.addAll(newtypes);
            }
        }

        this.isRule = this.isRule || f.isRule;
        this.isDoc = this.isDoc || f.isDoc;
        if (f.containsNumber)
            this.containsNumber = true;
        this.rowvarLiterals.addAll(f.rowvarLiterals);
        this.constants.putAll(f.constants);
        Set<RowStruct> hsrs;
        for (String var : f.rowVarStructs.keySet()) {
            hsrs = f.rowVarStructs.get(var);
            if (debug) System.out.println("merge from rowVarStructs: " + hsrs);
            for (RowStruct rs : hsrs)
                this.addRowVarStruct(var, new RowStruct(rs));
        }
        this.antecedentTerms.addAll(f.antecedentTerms);
        this.consequentTerms.addAll(f.consequentTerms);
        if (f.formulaASTClausalForm != null)
            this.formulaASTClausalForm = new ArrayList<>(f.formulaASTClausalForm);
    }

    /** ***************************************************************
     */
    public FormulaAST(String f) {
        read(f);
    }

    /** ***************************************************************
     * Construct a FormulaAST from an already-parsed Expr without ANTLR re-parsing.
     * Populates formula string, predVarCache, rowVarCache, rowVarStructs, and varTypes
     * by walking the Expr tree in O(n).
     */
    public FormulaAST(Expr expr) {
        setFormula(expr.toKifString());
        this.expr = expr;
        this.predVarCache = new HashSet<>();
        this.rowVarCache = new HashSet<>();
        initCachesFromExpr(expr);
    }

    private void initCachesFromExpr(Expr e) {
        if (!(e instanceof Expr.SExpr sexpr)) {
            if (e instanceof Expr.RowVar rv)
                rowVarCache.add(rv.name());
            return;
        }
        Expr head = sexpr.head();
        List<Expr> args = sexpr.args();
        // Variable in predicate position → pred var
        if (head instanceof Expr.Var v)
            predVarCache.add(v.name());
        // instance/subclass → varTypes
        if (head instanceof Expr.Atom a && args.size() >= 2
                && args.get(0) instanceof Expr.Var varNode
                && args.get(1) instanceof Expr.Atom typeAtom) {
            if (a.name().equals("instance"))
                varTypes.computeIfAbsent(varNode.name(), k -> new HashSet<>()).add(typeAtom.name());
            else if (a.name().equals("subclass"))
                varTypes.computeIfAbsent(varNode.name(), k -> new HashSet<>()).add(typeAtom.name() + "+");
        }
        // Row vars in args → rowVarCache + rowVarStructs
        String predName = head instanceof Expr.Atom a2 ? a2.name()
                        : head instanceof Expr.Var v2 ? v2.name() : null;
        for (Expr arg : args) {
            if (arg instanceof Expr.RowVar rv) {
                rowVarCache.add(rv.name());
                if (predName != null) {
                    RowStruct rs = new RowStruct();
                    rs.pred = predName;
                    rs.rowvar = rv.name();
                    rs.literal = sexpr.toKifString();
                    rs.arity = args.size();
                    addRowVarStruct(rv.name(), rs);
                }
            }
        }
        // Recurse
        if (head != null) initCachesFromExpr(head);
        for (Expr arg : args) initCachesFromExpr(arg);
    }

    /** ***************************************************************
     * Set 'theFormula' to the string clear all cache and populate the expr field.
     * @param s - the formula string
     */
    public void read(String s) {

        setFormula(s);

        allVarsCache = new HashSet<>();
        allVarsPairCache = new ArrayList<>();
        quantVarsCache = new HashSet<>();
        unquantVarsCache = new HashSet<>();
        existVarsCache = new HashSet<>();
        univVarsCache = new HashSet<>();
        termCache = new HashSet<>();

        this.argMap = new HashMap<>();
        this.eqList = new ArrayList<>();
        this.explicitTypes = new HashMap<>();
        this.varTypes = new HashMap<>();
        this.rowvarLiterals = new HashSet<>();
        this.constants = new HashMap<>();
        this.rowVarStructs = new HashMap<>();
        this.parsedFormula = null;
        this.expr = null;
        this.isDoc = false;
        this.isRule = false;
        this.containsNumber = false;
        this.antecedentTerms = new HashSet<>();
        this.consequentTerms = new HashSet<>();
        this.formulaASTHashCode = 0;
        this.formulaASTClausalForm = null;

        SuokifVisitor sv = SuokifVisitor.parseAny(s);
        if (sv.errors != null)
            this.errors.addAll(sv.errors);
        if (sv.result.containsKey(0)) {
            FormulaAST parsed = sv.result.get(0);
            this.expr = parsed.expr;
            this.parsedFormula = parsed.parsedFormula;
            this.argMap = parsed.argMap;
            this.eqList = parsed.eqList;
            this.explicitTypes = parsed.explicitTypes;
            this.varTypes = parsed.varTypes;
            this.rowvarLiterals = parsed.rowvarLiterals;
            this.constants = parsed.constants;
            this.rowVarStructs = parsed.rowVarStructs;
            this.isDoc = parsed.isDoc;
            this.isRule = parsed.isRule;
            this.containsNumber = parsed.containsNumber;
            if (parsed.predVarCache != null) {
                this.predVarCache = new HashSet<>(parsed.predVarCache);
            }
            if (parsed.rowVarCache != null) {
                this.rowVarCache = new HashSet<>(parsed.rowVarCache);
            }
        } else {
            String trimmed = s.trim();
            if (!trimmed.startsWith("(")) {
                // bare atom/variable/literal: set expr directly without SuokifVisitor
                if (trimmed.startsWith("?"))
                    this.expr = new Expr.Var(trimmed);
                else if (trimmed.startsWith("@"))
                    this.expr = new Expr.RowVar(trimmed);
                else if (trimmed.startsWith("\""))
                    this.expr = new Expr.StrLiteral(trimmed);
                else if (trimmed.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?"))
                    this.expr = new Expr.NumLiteral(trimmed);
                else
                    this.expr = new Expr.Atom(trimmed);
            } else {
                System.out.println("[FormulaAST - read] : SuokifVisitor couldn't parse the formula: " + s);
            }
        }
    }
    

    /** ***************************************************************
     * Merge arguments to a predicate, which may themselves be complex
     * formulas, with an existing formula.
     */
    public FormulaAST mergeFormulaAST(FormulaAST f2) {

        this.allVarsCache.addAll(f2.allVarsCache);
        this.allVarsPairCache.addAll(f2.allVarsPairCache);
        this.quantVarsCache.addAll(f2.quantVarsCache);
        this.unquantVarsCache.addAll(f2.unquantVarsCache);
        this.existVarsCache.addAll(f2.existVarsCache);
        this.univVarsCache.addAll(f2.univVarsCache);
        this.termCache.addAll(f2.termCache);
        if (this.rowVarCache == null)
            this.rowVarCache = new HashSet<>();
        if (f2.rowVarCache == null)
            f2.rowVarCache = new HashSet<>();
        this.rowVarCache.addAll(f2.rowVarCache);
        if (this.predVarCache == null)
            this.predVarCache = new HashSet<>();
        if (f2.predVarCache == null)
            f2.predVarCache = new HashSet<>();
        this.predVarCache.addAll(f2.predVarCache);

        Map<Integer, Set<SuokifParser.ArgumentContext>> argnummap, newargnummap;
        Set<SuokifParser.ArgumentContext> largs, newargs;
        for (String pred : f2.argMap.keySet()) {
            argnummap = f2.argMap.get(pred);
            newargnummap = new HashMap<>();
            for (Integer argnum : argnummap.keySet()) {
                largs = argnummap.get(argnum);
                newargs = new HashSet<>();
                newargs.addAll(largs);
                newargnummap.put(argnum,newargs);
            }
            this.argMap.put(pred,newargnummap);
        }
        Set<String> newtypes, existingTypes;
        for (String var : f2.explicitTypes.keySet()) {
            newtypes = f2.explicitTypes.get(var);
            if (explicitTypes.containsKey(var))
                explicitTypes.get(var).addAll(newtypes);
            else {
                existingTypes = new HashSet<>();
                explicitTypes.put(var,existingTypes);
                existingTypes.addAll(newtypes);
            }
        }

        for (String var : f2.varTypes.keySet()) {
            newtypes = f2.varTypes.get(var);
            if (varTypes.containsKey(var))
                varTypes.get(var).addAll(newtypes);
            else {
                existingTypes = new HashSet<>();
                varTypes.put(var,existingTypes);
                existingTypes.addAll(newtypes);
            }
        }

        this.eqList.addAll(f2.eqList);
        this.isRule = this.isRule || f2.isRule;
        this.isDoc = this.isDoc || f2.isDoc;
        if (f2.containsNumber)
            this.containsNumber = true;
        this.rowvarLiterals.addAll(f2.rowvarLiterals);
        this.constants.putAll(f2.constants);
        Set<RowStruct> hsrs;
        for (String var : f2.rowVarStructs.keySet()) {
            hsrs = f2.rowVarStructs.get(var);
            if (debug) System.out.println("merge from rowVarStructs: " + hsrs);
            for (RowStruct rs : hsrs)
                this.addRowVarStruct(var, new RowStruct(rs));
        }
        return this;
    }

    /** ***************************************************************
     * Merge arguments to a predicate, which may themselves be complex
     * formulas, with an existing formula.
     */
    public FormulaAST mergeFormulaAST(List<FormulaAST> ar) {

        if (this.predVarCache == null)
            this.predVarCache = new HashSet<>();
        if (this.rowVarCache == null)
            this.rowVarCache = new HashSet<>();
        Map<Integer, Set<SuokifParser.ArgumentContext>> argnummap, newargnummap;
        Set<SuokifParser.ArgumentContext> largs, newargs;
        Set<String> newtypes, existingTypes;
        for (FormulaAST arf : ar) {
            this.allVarsCache.addAll(arf.allVarsCache);
            this.allVarsPairCache.addAll(arf.allVarsPairCache);
            this.quantVarsCache.addAll(arf.quantVarsCache);
            this.unquantVarsCache.addAll(arf.unquantVarsCache);
            this.existVarsCache.addAll(arf.existVarsCache);
            this.univVarsCache.addAll(arf.univVarsCache);
            this.termCache.addAll(arf.termCache);
            if (arf.rowVarCache == null)
                arf.rowVarCache = new HashSet<>();
            this.rowVarCache.addAll(arf.rowVarCache);
            if (arf.predVarCache == null)
                arf.predVarCache = new HashSet<>();
            this.predVarCache.addAll(arf.predVarCache);
            for (String pred : arf.argMap.keySet()) {
                argnummap = arf.argMap.get(pred);
                newargnummap = new HashMap<>();
                for (Integer argnum : argnummap.keySet()) {
                    largs = argnummap.get(argnum);
                    newargs = new HashSet<>();
                    newargs.addAll(largs);
                    newargnummap.put(argnum,newargs);
                }
                this.argMap.put(pred,newargnummap);
            }

            this.eqList.addAll(arf.eqList);
            for (String var : arf.explicitTypes.keySet()) {
                newtypes = arf.explicitTypes.get(var);
                if (explicitTypes.containsKey(var))
                    explicitTypes.get(var).addAll(newtypes);
                else {
                    existingTypes = new HashSet<>();
                    explicitTypes.put(var,existingTypes);
                    existingTypes.addAll(newtypes);
                }
            }

            for (String var : arf.varTypes.keySet()) {
                newtypes = arf.varTypes.get(var);
                if (varTypes.containsKey(var))
                    varTypes.get(var).addAll(newtypes);
                else {
                    existingTypes = new HashSet<>();
                    varTypes.put(var,existingTypes);
                    existingTypes.addAll(newtypes);
                }
            }
            this.isRule = this.isRule || arf.isRule;
            this.isDoc = this.isDoc || arf.isDoc;
            if (arf.containsNumber)
                this.containsNumber = true;
            this.rowvarLiterals.addAll(arf.rowvarLiterals);
            this.constants.putAll(arf.constants);
            Set<RowStruct> hsrs;
            for (String var : arf.rowVarStructs.keySet()) {
                hsrs = arf.rowVarStructs.get(var);
                if (debug) System.out.println("merge from rowVarStructs: " + hsrs);
                for (RowStruct rs : hsrs)
                    this.addRowVarStruct(var, new RowStruct(rs));
            }
        }
        return this;
    }

    /** *****************************************************************
     * A class for holding information about constants (non-variables) and the literal
     * in which they appear
     */
    public static class ArgStruct {
        public String pred = "";
        public String literal = "";
        public String constant = "";
        public int argPos = -1;
        private StringBuilder sb;

        public ArgStruct() {
            sb = new StringBuilder();
        }

        @Override
        public String toString() {
            sb.setLength(0);
            sb.append(pred).append(":").append(constant).append("::").append(argPos).append(":").append(literal);
            return sb.toString();
        }
    }

    /** *****************************************************************
     * A class for holding information about row variables and the literal
     * in which they appear
     */
    public static class RowStruct {
        public String rowvar = "";
        public String pred = "";
        public String literal = "";
        public int arity = 0; // number of actual arguments in the literal
        private StringBuilder sb;

        public RowStruct() {
            sb = new StringBuilder();
        }
        public RowStruct(RowStruct rs) {
            this();
            this.rowvar = rs.rowvar;
            this.pred = rs.pred;
            this.literal = rs.literal;
            this.arity = rs.arity;
        }

        @Override
        public String toString() {
            sb.setLength(0);
            sb.append(pred).append(":").append(rowvar).append(":").append(literal);
            return sb.toString();
        }
    }

    /** *****************************************************************
     */
    public void addRowVarStruct(String var, RowStruct rs) {

        Set<RowStruct> hrs;
        if (!rowVarStructs.containsKey(var)) {
            hrs = new HashSet<>();
            rowVarStructs.put(var,hrs);
        }
        else
            hrs = rowVarStructs.get(var);
        hrs.add(rs);
    }

    /** *****************************************************************
     * the textual version of the formula
     */
    public static FormulaAST createComment(String input) {

        FormulaAST f = new FormulaAST();
        f.setFormula(input);
        f.comment = true;
        return f;
    }

    /** *****************************************************************
     * the textual version of the formula
     */
    public void printCaches() {

        System.out.println("Formula: " + this);
        System.out.println("all vars: " + allVarsCache);
        System.out.println("all vars pair: " + allVarsPairCache);
        System.out.println("quant vars: " + quantVarsCache);
        System.out.println("unquant vars: " + unquantVarsCache);
        System.out.println("exist vars: " + existVarsCache);
        System.out.println("univ vars: " + univVarsCache);
        System.out.println("terms: " + termCache);
        System.out.println("pred vars: " + predVarCache);
        System.out.println("row vars: " + rowVarCache);
        System.out.println("argMap: ");
        for (String pred : argMap.keySet()) {
            System.out.print("\t" + pred + "\t");
            for (Integer i : argMap.get(pred).keySet()) {
                System.out.print(i + ": ");
                for (SuokifParser.ArgumentContext c : argMap.get(pred).get(i)) {
                    System.out.print(c.getText() + ", ");
                }
            }
            System.out.println();
        }
        System.out.println("varTypes: " + varTypes);
        System.out.println("predVarCache: " + predVarCache);
        System.out.println("explicitTypes: " + explicitTypes);

        System.out.println("containsNumber: " + containsNumber);
        System.out.println("higherOrder: " + higherOrder);

        System.out.println("eqlist: ");
        for (List<SuokifParser.TermContext> al : eqList) {
            System.out.println(al.get(0).getText() + " = " + al.get(1).getText());
        }
        System.out.println("row var literal: ");
        for (ParserRuleContext lit : rowvarLiterals) {
            System.out.println(lit.getText());
        }

        System.out.println("constants: ");
        ArgStruct as;
        String lit;
        for (String c : constants.keySet()) {
            as = constants.get(c);
            lit = as.literal;
            System.out.println(c + " : " + lit);
        }

        System.out.println("row var struct: ");
        Set<RowStruct> rvs;
        for (String var : rowVarStructs.keySet()) {
            rvs = rowVarStructs.get(var);
            System.out.print(var + ":");
            for (RowStruct rv : rvs)
                System.out.print(rv.toString() + ", ");
            System.out.println();
        }
        System.out.println();
    }

    /*****************************************************************
     * Walks the already-parsed {@link Expr} tree without constructing any
     * {link Formula} objects or calling {@code findAllTypeRestrictions}.
     * The logic mirrors {@code Formula.isHigherOrder} exactly.
     */
    static boolean isHigherOrderExpr(Expr expr, KB kb) {

        if (!(expr instanceof Expr.SExpr se)) return false;
        String head = se.headName();
        if (head == null) return false; // var-list node inside a quantifier
        List<String> sig = kb.kbCache.getSignature(head);
        if (sig != null && !FormulaAST.isVariable(head) && sig.contains("Formula"))
            return true;
        boolean logop = FormulaAST.isLogicalOperator(head);
        for (Expr arg : se.args()) {
            if (!(arg instanceof Expr.SExpr argSe)) continue; // atom/var/literal — not HOL
            String argHead = argSe.headName();
            if (argHead != null && !kb.isFunction(argHead)) {
                // compound, non-function arg
                if (logop) {
                    if (isHigherOrderExpr(argSe, kb)) return true; // recurse for logical ops
                } else {
                    return true; // compound non-function arg to non-logop predicate → HOL
                }
            } else {
                // function application (or null-head var-list) — recurse
                if (isHigherOrderExpr(argSe, kb)) return true;
            }
        }
        return false;
    }

    /*****************************************************************
     */
    public boolean listP() {

        if (expr != null) return expr instanceof Expr.SExpr;
        System.out.println("Formula string-based method used: listP");
        return listP(getFormula());
    }

    /*****************************************************************
     */
    public boolean atom() {

        if (expr != null) return !(expr instanceof Expr.SExpr);
        System.out.println("Formula string-based method used: atom");
        return atom(getFormula());
    }

    /** ***************************************************************
     */
    public boolean empty() {

        if (expr != null) return expr instanceof Expr.SExpr se && se.head() == null && se.args().isEmpty();
        System.out.println("Formula string-based method used: empty");
        return empty(getFormula());
    }

    /*****************************************************************
     */
    private List<Expr> getElements() {

        if (!(expr instanceof Expr.SExpr se)) return Collections.emptyList();
        List<Expr> res = new ArrayList<>();
        if (se.head() != null) res.add(se.head());
        res.addAll(se.args());
        return res;
    }

    /*****************************************************************
     */
    public String car() {

        if (expr == null) {
            System.out.println("Formula string-based method used: car()");
            if (!this.listP()) return null;
            if (stringArgs.isEmpty()) {
                if (this.empty()) return "";
                loadArguments();
            }
            return stringArgs.isEmpty() ? "" : stringArgs.get(0);
        }
        if (!(expr instanceof Expr.SExpr)) return null; // atom/var/literal — not a list
        List<Expr> elements = getElements();
        if (elements.isEmpty()) return "";
        return elements.get(0).toKifString();
    }

    /*****************************************************************
     */
    public String cadr() {

        if (expr != null) return getStringArgument(1);
        System.out.println("Formula string-based method used: cadr");
        if (stringArgs.isEmpty()) loadArguments();
        return stringArgs.size() > 1 ? stringArgs.get(1) : "";
    }

    /*****************************************************************
     */
    public String caddr() {

        if (expr != null) return getStringArgument(2);
        System.out.println("Formula string-based method used: caddr");
        if (stringArgs.isEmpty()) loadArguments();
        return stringArgs.size() > 2 ? stringArgs.get(2) : "";
    }

    /*****************************************************************
     */
    public String cdr() {

        if (expr != null) {
            if (!(expr instanceof Expr.SExpr)) return null; // atom/var/literal — not a list
            List<Expr> elements = getElements();
            if (elements.size() <= 1) return "()";
            return "(" + elements.subList(1, elements.size()).stream().map(Expr::toKifString).collect(Collectors.joining(" ")) + ")";
        }
        System.out.println("Formula string-based method used: cdr()");
        if (!listP(theFormula)) return null;
        if (empty(theFormula)) return "()";
        if (stringArgs.isEmpty()) loadArguments();
        if (stringArgs.size() <= 1) return "()";
        return "(" + String.join(SPACE, stringArgs.subList(1, stringArgs.size())) + ")";
    }

    /*****************************************************************
     */
    public String cddr() {

        if (expr != null) {
            FormulaAST fCdr = this.cdrAsFormula();
            if (fCdr != null)
                return fCdr.cdr();
            return null;
        }
        System.out.println("Formula string-based method used: cddr");
        if (stringArgs.isEmpty()) loadArguments();
        if (stringArgs.size() <= 2) return "()";
        return "(" + String.join(SPACE, stringArgs.subList(2, stringArgs.size())) + ")";
    }

    /*****************************************************************
     */
    public FormulaAST carAsFormula() {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (elements.isEmpty()) return null;
            Expr target = elements.get(0);
            FormulaAST f = new FormulaAST();
            f.setFormula(target.toKifString());
            f.expr = target;
            return f;
        }
        System.out.println("Formula string-based method used: carAsFormula");
        loadArguments();
        return args.isEmpty() ? null : args.getFirst();
    }

    /*****************************************************************
     */
    public FormulaAST cdrAsFormula() {

        if (expr != null) {
            List<Expr> elements = getElements();
            FormulaAST f = new FormulaAST();
            if (elements.size() <= 1) {
                f.setFormula("()");
                f.expr = new Expr.SExpr(null, Collections.emptyList());
                return f;
            }
            List<Expr> cdrElements = elements.subList(1, elements.size());
            f.setFormula("(" + cdrElements.stream().map(Expr::toKifString).collect(Collectors.joining(" ")) + ")");
            f.expr = new Expr.SExpr(null, new ArrayList<>(cdrElements));
            return f;
        }
        System.out.println("Formula string-based method used: cdrAsFormula");
        if (!listP()) return null;
        if (empty()) {
            FormulaAST emptyF = new FormulaAST();
            emptyF.setFormula("()");
            emptyF.expr = new Expr.SExpr(null, Collections.emptyList());
            return emptyF;
        }
        loadArguments();
        FormulaAST f = new FormulaAST();
        f.setFormula("(" + String.join(" ", stringArgs.subList(1, stringArgs.size())) + ")");
        return f;
    }

    /*****************************************************************
     */
    public FormulaAST cddrAsFormula() {

        if (expr != null) {
            FormulaAST cdr = cdrAsFormula();
            if (cdr == null || cdr.getFormula().equals("()")) {
                FormulaAST f = new FormulaAST();
                f.setFormula("()");
                return f;
            }
            return cdr.cdrAsFormula();
        }
        System.out.println("Formula string-based method used: cddrAsFormula");
        loadArguments();
        FormulaAST f = new FormulaAST();
        f.setFormula("(" + String.join(" ", stringArgs.subList(Math.min(2, stringArgs.size()), stringArgs.size())) + ")");
        return f;
    }

    /*****************************************************************
     */
    public String getStringArgument(int argnum) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (argnum >= 0 && argnum < elements.size()) return elements.get(argnum).toKifString();
            return "";
        }
        System.out.println("Formula string-based method used: getStringArgument");
        if (stringArgs.isEmpty()) loadArguments();
        return (argnum >= 0 && argnum < stringArgs.size()) ? stringArgs.get(argnum) : "";
    }

    /*****************************************************************
     */
    public FormulaAST getArgument(int argnum) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (argnum < 0 || argnum >= elements.size()) return null;
            Expr target = elements.get(argnum);
            FormulaAST f = new FormulaAST();
            f.setFormula(target.toKifString());
            f.expr = target;
            return f;
        }
        System.out.println("Formula string-based method used: getArgument");
        loadArguments();
        return (argnum >= 0 && argnum < args.size()) ? args.get(argnum) : null;
    }

    /*****************************************************************
     */
    public int listLength() {

        if (expr != null) {
            if (!(expr instanceof Expr.SExpr)) return -1;
            return getElements().size();
        }
        System.out.println("Formula string-based method used: listLength");
        loadArguments();
        return stringArgs.size();
    }

    /*****************************************************************
     */
    public List<String> argumentsToArrayListString(int start) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (start < 0 || start >= elements.size()) return null;
            List<Expr> slice = elements.subList(start, elements.size());
            for (Expr e : slice)
                if (e instanceof Expr.SExpr) return null;
            return slice.stream().map(Expr::toKifString).collect(Collectors.toList());
        }
        System.out.println("Formula string-based method used: argumentsToArrayListString");
        loadArguments();
        if (start < 0 || start >= stringArgs.size()) return null;
        List<String> slice = new ArrayList<>(stringArgs.subList(start, stringArgs.size()));
        for (String s : slice)
            if (s.startsWith("(")) return null;
        return slice;
    }

    /*****************************************************************
     */
    public List<String> complexArgumentsToArrayListString(int start) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (start < 0 || start >= elements.size()) return null;
            List<String> result = elements.subList(start, elements.size()).stream()
                    .map(Expr::toKifString)
                    .collect(Collectors.toList());
            return result.isEmpty() ? null : result;
        }
        System.out.println("Formula string-based method used: complexArgumentsToArrayListString");
        loadArguments();
        if (start < 0 || start >= stringArgs.size()) return null;
        List<String> result = new ArrayList<>(stringArgs.subList(start, stringArgs.size()));
        return result.isEmpty() ? null : result;
    }

    /*****************************************************************
     */
    public List<FormulaAST> complexArgumentsToArrayList(int start) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (start < 0 || start >= elements.size()) return null;
            List<FormulaAST> res = new ArrayList<>();
            for (int i = start; i < elements.size(); i++) {
                Expr e = elements.get(i);
                FormulaAST f = new FormulaAST();
                f.setFormula(e.toKifString());
                f.expr = e;
                res.add(f);
            }
            return res;
        }
        System.out.println("Formula string-based method used: complexArgumentsToArrayList");
        loadArguments();
        return (start >= 0 && start <= args.size()) ? new ArrayList<>(args.subList(start, args.size())) : null;
    }

    /*****************************************************************
     */
    public List<String> literalToArrayList() {

        if (expr != null) return complexArgumentsToArrayListString(0);
        System.out.println("Formula string-based method used: literalToArrayList");
        loadArguments();
        return stringArgs.isEmpty() ? null : new ArrayList<>(stringArgs);
    }

    /*****************************************************************
     */
    private void collectAllVars(Expr e, Set<String> res) {

        if (e instanceof Expr.Var v) res.add(v.name());
        else if (e instanceof Expr.RowVar rv) res.add(rv.name());
        else if (e instanceof Expr.SExpr se) {
            if (se.head() != null) collectAllVars(se.head(), res);
            for (Expr arg : se.args()) collectAllVars(arg, res);
        }
    }

    /*****************************************************************
     */
    public Set<String> collectAllVariables() {

        if (expr != null) {
            if (!allVarsCache.isEmpty()) return allVarsCache;
            collectAllVars(expr, allVarsCache);
            return allVarsCache;
        }
        System.out.println("Formula string-based method used: collectAllVariables");
        return new FormulaAST(getFormula()).collectAllVariables();
    }

    /*****************************************************************
     */
    private void collectQuantVars(Expr e, Set<String> res) {

        if (!(e instanceof Expr.SExpr se)) return;
        String head = se.headName();
        if ("forall".equals(head) || "exists".equals(head)) {
            if (se.args().size() >= 1 && se.args().get(0) instanceof Expr.SExpr varList) {
                for (Expr v : varList.args()) {
                    if (v instanceof Expr.Var || v instanceof Expr.RowVar)
                        res.add(v.toKifString());
                }
            }
            if (se.args().size() >= 2) collectQuantVars(se.args().get(1), res);
        } else {
            if (se.head() != null) collectQuantVars(se.head(), res);
            for (Expr arg : se.args()) collectQuantVars(arg, res);
        }
    }

    /*****************************************************************
     */
    public Set<String> collectQuantifiedVariables() {

        if (expr != null) {
            if (!quantVarsCache.isEmpty()) return quantVarsCache;
            collectQuantVars(expr, quantVarsCache);
            return quantVarsCache;
        }
        System.out.println("Formula string-based method used: collectQuantifiedVariables");
        return new FormulaAST(getFormula()).collectQuantifiedVariables();
    }

    /** ***************************************************************
     */
    public Set<String> collectUnquantifiedVariables() {

        if (expr != null) {
            // We always re-collect from Expr to ensure correctness, ignoring any visitor-populated caches
            Set<String> freeVars = ExprToTPTP.collectFreeVars(expr);
            this.unquantVarsCache.clear();
            this.unquantVarsCache.addAll(freeVars);
            return this.unquantVarsCache;
        }
        System.out.println("Formula string-based method used: collectUnquantifiedVariables");
        Set<String> quantified = new HashSet<>();
        Set<String> unquantified = new HashSet<>();
        collectQuantifiedUnquantifiedVariablesRecurse(this, new HashMap<>(), unquantified, quantified);
        unquantified.removeAll(quantified);
        return unquantified;
    }

    /*****************************************************************
     */
    private void collectTermsHelper(Expr e, Set<String> res) {

        switch (e) {
            case Expr.SExpr se -> {
                if (se.head() != null) collectTermsHelper(se.head(), res);
                for (Expr arg : se.args()) collectTermsHelper(arg, res);
            }
            default -> res.add(e.toKifString());
        }
    }

    /*****************************************************************
     */
    public Set<String> collectTerms() {

        if (expr != null) {
            this.termCache.clear();
            collectTermsHelper(expr, this.termCache);
            return this.termCache;
        }
        System.out.println("Formula string-based method used: collectTerms");
        return new FormulaAST(getFormula()).collectTerms();
    }


    /*****************************************************************
     */
    public boolean isHigherOrder(KB kb) {

        if (expr != null) {
            boolean hol = isHigherOrderExpr(expr, kb);
            if (hol) this.higherOrder = true;
            return hol;
        }
        System.out.println("Formula string-based method used: isHigherOrder");
        return new FormulaAST(getFormula()).isHigherOrder(kb);
    }

    /*****************************************************************
     */
    public boolean isGround() {

        if (expr != null) {
            return collectAllVariables().isEmpty();
        }
        System.out.println("Formula string-based method used: isGround");
        return new FormulaAST(getFormula()).isGround();
    }

    /*****************************************************************
     */
    public boolean isSimpleClause(KB kb) {

        if (expr instanceof Expr.SExpr se) {
            if (se.head() == null || se.head() instanceof Expr.SExpr) return false;
            for (Expr arg : se.args()) {
                if (arg instanceof Expr.SExpr argSe) {
                    String head = argSe.headName();
                    if (head == null) return false;
                    if (kb != null) {
                        if (!kb.isFunction(head)) return false;
                    } else {
                        if (!head.endsWith("Fn")) return false;
                    }
                }
            }
            return true;
        }
        if (expr != null) return false;
        System.out.println("Formula string-based method used: isSimpleClause");
        return new FormulaAST(getFormula()).isSimpleClause(kb);
    }

    /*****************************************************************
     */
    public boolean isSimpleNegatedClause(KB kb) {

        if (expr instanceof Expr.SExpr se && "not".equals(se.headName()) && se.args().size() == 1) {
            Expr arg = se.args().getFirst();
            if (arg instanceof Expr.SExpr) {
                FormulaAST fa = new FormulaAST();
                fa.setFormula(arg.toKifString());
                fa.expr = arg;
                return fa.isSimpleClause(kb);
            }
        }
        if (expr != null) return false;
        System.out.println("Formula string-based method used: isSimpleNegatedClause");
        return new FormulaAST(getFormula()).isSimpleNegatedClause(kb);
    }

    /*****************************************************************
     */
    public boolean isFunctionalTerm() {

        if (expr instanceof Expr.SExpr se) {
            String head = se.headName();
            return head != null && head.endsWith("Fn");
        }
        if (expr != null) return false;
        System.out.println("Formula string-based method used: isFunctionalTerm");
        return new FormulaAST(getFormula()).isFunctionalTerm();
    }

    /*****************************************************************
     */
    public boolean isBinary() {

        if (expr instanceof Expr.SExpr se) {
            return se.args().size() == 2;
        }
        if (expr != null) return false;
        System.out.println("Formula string-based method used: isBinary");
        return new FormulaAST(getFormula()).isBinary();
    }

    /** ***************************************************************
     */
    public boolean isExistentiallyQuantified() {

        if (expr instanceof Expr.SExpr se) return "exists".equals(se.headName());
        if (expr != null) return false;
        System.out.println("Formula string-based method used: isExistentiallyQuantified");
        return new FormulaAST(getFormula()).isExistentiallyQuantified();
    }

    /*****************************************************************
     */
    public boolean isUniversallyQuantified() {

        if (expr instanceof Expr.SExpr se) return "forall".equals(se.headName());
        if (expr != null) return false;
        System.out.println("Formula string-based method used: isUniversallyQuantified");
        return new FormulaAST(getFormula()).isUniversallyQuantified();
    }

    /*****************************************************************
     */
    private Expr substituteExpr(Expr e, Map<String, String> m) {

        if (e instanceof Expr.Var v) {
            String sub = m.get(v.name());
            if (sub == null) return e;
            if (sub.startsWith("?")) return new Expr.Var(sub);
            if (sub.startsWith("@")) return new Expr.RowVar(sub);
            return new Expr.Atom(sub);
        } else if (e instanceof Expr.RowVar rv) {
            String sub = m.get(rv.name());
            if (sub == null) return e;
            if (sub.startsWith("@")) return new Expr.RowVar(sub);
            if (sub.startsWith("?")) return new Expr.Var(sub);
            return new Expr.Atom(sub);
        } else if (e instanceof Expr.Atom a) {
            if (m.containsKey(a.name())) return new Expr.Atom(m.get(a.name()));
        } else if (e instanceof Expr.SExpr se) {
            Expr newHead = se.head() == null ? null : substituteExpr(se.head(), m);
            List<Expr> newArgs = se.args().stream()
                    .map(arg -> substituteExpr(arg, m))
                    .collect(Collectors.toList());
            return new Expr.SExpr(newHead, newArgs);
        }
        return e;
    }

    /*****************************************************************
     */
    public FormulaAST substituteVariables(Map<String, String> m) {

        if (expr != null) {
            Expr newExpr = substituteExpr(expr, m);
            FormulaAST fa = new FormulaAST();
            fa.setFormula(newExpr.toKifString());
            fa.expr = newExpr;
            return fa;
        }
        System.out.println("Formula string-based method used: substituteVariables");
        FormulaAST base = new FormulaAST(getFormula()).substituteVariables(m);
        if (base == null) return null;
        FormulaAST wrapped = new FormulaAST();
        wrapped.setFormula(base.getFormula());
        return wrapped;
    }

    /*****************************************************************
     */
    public FormulaAST replaceVar(String var, String term) {

        if (expr != null) {
            Map<String, String> m = new HashMap<>();
            m.put(var, term);
            return substituteVariables(m);
        }
        System.out.println("Formula string-based method used: replaceVar");
        FormulaAST base = new FormulaAST(getFormula()).replaceVar(var, term);
        if (base == null) return null;
        FormulaAST wrapped = new FormulaAST();
        wrapped.setFormula(base.getFormula());
        return wrapped;
    }

    /*****************************************************************
     * Expr-based hashCode — uses ClausifierExpr.normalizeVariables so that
     * two formulas that differ only in variable names compare equal, matching
     * the contract of equals().
     */
    @Override
    public int hashCode() {
        if (formulaASTHashCode == 0) {
            String s = getFormula();
            if (s == null) return 0;
            String normalized = (expr != null)
                    ? ClausifierExpr.normalizeVariables(expr).toKifString()
                    : ClausifierExpr.normalizeVariables(s);
            int h = normalized.trim().hashCode();
            if (h == 0) h = 1;
            formulaASTHashCode = h;
        }
        return formulaASTHashCode;
    }

    /*****************************************************************
     * Expr-based equals — two FormulaAST objects are equal if their
     * variable-normalized KIF strings are equal (same contract as Formula.equals).
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        String other;
        Expr otherExpr = null;
        if (o instanceof FormulaAST fa2) {
            other = fa2.getFormula();
            otherExpr = fa2.expr;
        } else {
            return false;
        }
        String s = getFormula();
        if (other == null) return (s == null);
        if (s == null) return false;
        String n1 = (expr != null)
                ? ClausifierExpr.normalizeVariables(expr).toKifString().trim()
                : ClausifierExpr.normalizeVariables(s).trim();
        String n2 = (otherExpr != null)
                ? ClausifierExpr.normalizeVariables(otherExpr).toKifString().trim()
                : ClausifierExpr.normalizeVariables(other).trim();
        return n1.replaceAll("\\s+", " ").equals(n2.replaceAll("\\s+", " "));
    }

    /*****************************************************************
     * Expr-based clausal form — uses ClausifierExpr instead of old Clausifier.
     */
    public List getTheClausalForm() {
        if (formulaASTClausalForm == null) {
            String s = getFormula();
            if (s != null && !s.isEmpty())
                formulaASTClausalForm = ClausifierExpr.toNegAndPosLitsWithRenameInfo(new FormulaAST(this.getFormula()));
        }
        return formulaASTClausalForm;
    }

    /*****************************************************************
     */
    public void clearTheClausalForm() {
        if (formulaASTClausalForm != null)
            formulaASTClausalForm.clear();
        formulaASTClausalForm = null;
    }

    // ---------------------------------------------------------------
    // Private / protected helpers — copies from Formula
    // ---------------------------------------------------------------

    //TODO: copy from Formula — refactor to use Expr AST instead of String manipulation
    private List<FormulaAST> parseList(String s) {

        List<FormulaAST> result = new ArrayList<>();
        FormulaAST f = new FormulaAST();
        f.read(LP + s + RP);
        if (f.empty())
            return result;
        String car;
        FormulaAST newForm;
        while (!f.empty()) {
            car = f.car();
            f.read(f.cdr());
            newForm = new FormulaAST();
            newForm.read(car);
            result.add(newForm);
        }
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    private boolean compareFormulaSets(String s) {

        List<FormulaAST> thisList = parseList(this.getFormula().substring(1, this.getFormula().length() - 1));
        List<FormulaAST> sList = parseList(s.substring(1, s.length() - 1));
        if (thisList.size() != sList.size())
            return false;
        for (int i = 0; i < thisList.size(); i++) {
            for (int j = 0; j < sList.size(); j++) {
                if ((thisList.get(i)).logicallyEquals((sList.get(j)).getFormula())) {
                    sList.remove(j);
                    j = sList.size();
                }
            }
        }
        return sList.isEmpty();
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    private static String normalizeParameterOrder(String formula, KB kb, boolean varPlaceholders) {

        if (formula == null)
            return null;
        if (!FormulaAST.listP(formula)) {
            if (varPlaceholders && isVariable(formula))
                return "?XYZ";
            else
                return formula;
        }
        FormulaAST f = new FormulaAST();
        f.read(formula);
        List<String> args = f.complexArgumentsToArrayListString(1);
        if (args == null || args.isEmpty())
            return formula;
        List<String> orderedArgs = new ArrayList<>();
        for (String arg : args)
            orderedArgs.add(FormulaAST.normalizeParameterOrder(arg, kb, varPlaceholders));
        String head = f.car();
        if (isCommutative(head) || (kb != null && kb.isInstanceOf(head, "SymmetricRelation")))
            Collections.sort(orderedArgs);
        StringBuilder result = new StringBuilder(LP);
        if (varPlaceholders && isSkolemTerm(head))
            head = "?SknFn";
        result.append(head);
        result.append(SPACE);
        for (String arg : orderedArgs) {
            result.append(arg);
            result.append(SPACE);
        }
        result.deleteCharAt(result.length() - 1);
        result.append(RP);
        return result.toString();
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    private static String translateInequalities(String s) {

        if (s.equalsIgnoreCase(GT)) return ">";
        if (s.equalsIgnoreCase(GTET)) return ">=";
        if (s.equalsIgnoreCase(LT)) return "<";
        if (s.equalsIgnoreCase(LTET)) return "<=";
        return "";
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    private String validArgsRecurse(FormulaAST f, String filename, Integer lineNo) {

        if ("".equals(f.getFormula()) || !f.listP() || f.atom() || f.empty())
            return "";
        String pred = f.car();
        String rest = f.cdr();
        FormulaAST restF = new FormulaAST();
        restF.read(rest);
        int argCount = 0;
        String arg, result, errString;
        FormulaAST argF;
        while (!restF.empty()) {
            argCount++;
            arg = restF.car();
            argF = new FormulaAST();
            argF.read(arg);
            result = validArgsRecurse(argF, filename, lineNo);
            if (!"".equals(result))
                return result;
            restF.read(restF.cdr());
        }
        String location = "";
        if ((filename != null) && (lineNo != null))
            location = "near line " + lineNo + " in " + filename;
        if (pred.equals(AND) || pred.equals(OR) || pred.equals(XOR)) {
            if (argCount < 2) {
                errString = "Too few arguments for 'and' or 'or' " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(UQUANT) || pred.equals(EQUANT)) {
            if (argCount != 2) {
                errString = "Wrong number of arguments for quantifer " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
            else {
                FormulaAST quantF = new FormulaAST();
                quantF.read(rest);
                if (!listP(quantF.car())) {
                    errString = "No var list for quantifier " + location + ": " + f.toString();
                    errors.add(errString);
                    return errString;
                }
            }
        }
        else if (pred.equals(IFF) || pred.equals(IF)) {
            if (argCount != 2) {
                errString = "Wrong number of arguments for '<=>' or '=>' " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(EQUAL)) {
            if (argCount != 2) {
                errString = "Wrong number of arguments for 'equals' " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (!(isVariable(pred)) && (argCount > (MAX_PREDICATE_ARITY + 1))) {
            errString = "Maybe too many arguments " + location + ": " + f.toString();
            errors.add(errString);
            return errString;
        }
        return "";
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    protected static String getDualOperator(String op) {

        String ans = null;
        if (op instanceof String) {
            String[][] duals = { { UQUANT, EQUANT },
                                 { EQUANT, UQUANT },
                                 { AND,    OR     },
                                 { OR,     AND    },
                                 { NOT,    ""     },
                                 { "",     NOT    },
                                 { LOG_TRUE,  LOG_FALSE  },
                                 { LOG_FALSE, LOG_TRUE   }
            };
            for (String[] dual : duals) {
                if (op.equals(dual[0]))
                    ans = dual[1];
            }
        }
        return ans;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    static class SortByLine implements Comparator<FormulaAST> {

        @Override
        public int compare(FormulaAST a, FormulaAST b) {
            return a.startLine - b.startLine;
        }
    }

    // ---------------------------------------------------------------
    // Public instance methods — pure copies from Formula
    // ---------------------------------------------------------------

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String createID() {

        String fname = sourceFile;
        if (!StringUtil.emptyString(fname) && fname.lastIndexOf(java.io.File.separator) > -1)
            fname = fname.substring(fname.lastIndexOf(java.io.File.separator) + 1);
        int hc = getFormula().hashCode();
        String result;
        if (hc < 0)
            result = "N" + (Integer.valueOf(hc)).toString().substring(1) + fname;
        else
            result = (Integer.valueOf(hc)).toString() + fname;
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST copy() { return new FormulaAST(this); }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST deepCopy() { return copy(); }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public List getClauses() {

        List clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || clausesWithVarMap.isEmpty())
            return null;
        return (List) clausesWithVarMap.get(0);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public Map getVarMap() {

        List clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || (clausesWithVarMap.size() < 3))
            return null;
        return (Map) clausesWithVarMap.get(2);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    // NOTE: FormulaAST also has a `varTypes` field populated at parse time.
    // This method mirrors Formula's runtime cache via varTypeCache + FormulaPreprocessor.
    public Map<String, Set<String>> getVarTypes(KB kb) {

        if (varTypeCache != null)
            return varTypeCache;
        FormulaPreprocessor fp = new FormulaPreprocessor();
        varTypeCache = fp.computeVariableTypesExpr(expr, kb);
        return varTypeCache;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public Set<String> getVarType(KB kb, String var) {

        Map<String, Set<String>> vt = getVarTypes(kb);
        if (vt.containsKey(var))
            return vt.get(var);
        else
            return null;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    @Override
    public int compareTo(Object f) throws ClassCastException {

        if (f == null) {
            System.err.println("Error in Formula.compareTo(): null formula");
            throw new ClassCastException("Error in Formula.compareTo(): null formula");
        }
        String fFormula;
        if (f instanceof FormulaAST fa) {
            fFormula = fa.getFormula();
        } else {
            throw new ClassCastException("Error in Formula.compareTo(): "
                    + "Class cast exception for argument of class: "
                    + f.getClass().getName());
        }
        return getFormula().compareTo(fFormula);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isBalancedList() {

        boolean ans = false;
        if (this.listP()) {
            if (this.empty())
                ans = true;
            else {
                String input = this.getFormula().trim();
                List<Character> quoteChars = Arrays.asList('"', '\'');
                int i = 0;
                int len = input.length();
                int end = len - 1;
                int pLevel = 0;
                int qLevel = 0;
                char prev = '0';
                char ch;
                boolean insideQuote = false;
                char quoteCharInForce = '0';
                while (i < len) {
                    ch = input.charAt(i);
                    if (!insideQuote) {
                        if (ch == '(')
                            pLevel++;
                        else if (ch == ')')
                            pLevel--;
                        else if (quoteChars.contains(ch) && (prev != '\\')) {
                            insideQuote = true;
                            quoteCharInForce = ch;
                            qLevel++;
                        }
                    }
                    else if (quoteChars.contains(ch)
                             && (ch == quoteCharInForce)
                             && (prev != '\\')) {
                        insideQuote = false;
                        quoteCharInForce = '0';
                        qLevel--;
                    }
                    prev = ch;
                    i++;
                }
                ans = ((pLevel == 0) && (qLevel == 0));
            }
        }
        return ans;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST cons(String obj) {

        FormulaAST ans = this;
        String fStr = this.getFormula();
        if (!StringUtil.emptyString(obj) && !StringUtil.emptyString(fStr)) {
            String theNewFormula;
            if (this.listP()) {
                if (this.empty())
                    theNewFormula = (LP + obj + RP);
                else
                    theNewFormula = (LP + obj + SPACE + fStr.substring(1, (fStr.length() - 1)) + RP);
            }
            else
                theNewFormula = (LP + obj + " . " + fStr + RP);
            if (theNewFormula != null) {
                ans = new FormulaAST();
                ans.read(theNewFormula);
            }
        }
        return ans;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST cons(FormulaAST f) {

        return cons(f.getFormula());
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST append(FormulaAST f) {

        FormulaAST newFormula = new FormulaAST();
        newFormula.read(getFormula());
        if (newFormula.equals("") || newFormula.atom()) {
            System.err.println("Error in Formula.append(): attempt to append to non-list: " + getFormula());
            return this;
        }
        if (f == null || f.getFormula() == null || "".equals(f.getFormula()) || f.getFormula().equals("()"))
            return newFormula;
        f.setFormula(f.getFormula().trim());
        if (!f.atom())
            f.setFormula(f.getFormula().substring(1, f.getFormula().length() - 1));
        int lastParen = getFormula().lastIndexOf(RP);
        String sep = "";
        if (lastParen > 1)
            sep = SPACE;
        newFormula.setFormula(newFormula.getFormula().substring(0, lastParen) + sep + f.getFormula() + RP);
        return newFormula;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String validArgs(String filename, Integer lineNo) {

        if (getFormula() == null || "".equals(getFormula()))
            return "";
        FormulaAST f = new FormulaAST();
        f.read(getFormula());
        return validArgsRecurse(f, filename, lineNo);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String validArgs() {

        return this.validArgs(null, null);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String badQuantification() {
        return "";
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public void addAllNoDup(Collection<String> thisCol, Collection<String> arg) {

        for (String s : arg)
            if (!thisCol.contains(s))
                thisCol.add(s);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public List<Set<String>> collectVariables() {

        if (!allVarsPairCache.isEmpty() && KBmanager.initialized)
            return allVarsPairCache;
        List<Set<String>> ans = new ArrayList<>();
        ans.add(new TreeSet<>());
        ans.add(new TreeSet<>());
        allVarsPairCache.add(new TreeSet<>());
        allVarsPairCache.add(new TreeSet<>());
        Set<String> quantified = new TreeSet<>();
        Set<String> unquantified = new TreeSet<>();
        unquantified.addAll(collectAllVariables());
        quantified.addAll(collectQuantifiedVariables());
        unquantified.removeAll(quantified);
        ans.get(0).addAll(quantified);
        ans.get(1).addAll(unquantified);
        allVarsPairCache.get(0).addAll(quantified);
        allVarsPairCache.get(1).addAll(unquantified);
        return ans;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public void collectQuantifiedUnquantifiedVariablesRecurse(FormulaAST f, Map<String, Boolean> varFlag,
                  Set<String> unquantifiedVariables, Set<String> quantifiedVariables) {

        if (f == null || StringUtil.emptyString(f.getFormula()) || f.empty())
            return;
        String carstr = f.car();
        if (atom(carstr) && isLogicalOperator(carstr)) {
            if (carstr.equals(EQUANT) || carstr.equals(UQUANT)) {
                String varString = f.getStringArgument(1);
                String[] varArray = (varString.substring(1, varString.length() - 1)).split(SPACE);
                quantifiedVariables.addAll(Arrays.asList(varArray));
                for (int i = 2; i < f.listLength(); i++) {
                    collectQuantifiedUnquantifiedVariablesRecurse(new FormulaAST(f.getArgument(i).getFormula()),
                            varFlag, unquantifiedVariables, quantifiedVariables);
                }
            }
            else {
                for (int i = 1; i < f.listLength(); i++) {
                    collectQuantifiedUnquantifiedVariablesRecurse(new FormulaAST(f.getArgument(i).getFormula()),
                            varFlag, unquantifiedVariables, quantifiedVariables);
                }
            }
        }
        else {
            String arg;
            for (int i = 0; i < f.listLength(); i++) {
                arg = f.getStringArgument(i);
                if (arg.startsWith(V_PREF) || arg.startsWith(R_PREF)) {
                    if (!varFlag.containsKey(arg) && !quantifiedVariables.contains(arg)) {
                        unquantifiedVariables.add(arg);
                        varFlag.put(arg, false);
                    }
                }
                else {
                    collectQuantifiedUnquantifiedVariablesRecurse(new FormulaAST(arg),
                            varFlag, unquantifiedVariables, quantifiedVariables);
                }
            }
        }
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public List<String> collectAllVariablesOrdered() {

        List<String> result = new ArrayList<>();
        if (listLength() < 1)
            return result;
        FormulaAST fcar = new FormulaAST();
        fcar.read(this.car());
        if (fcar.isVariable())
            result.add(fcar.getFormula());
        else {
            if (fcar.listP())
                result.addAll(fcar.collectAllVariablesOrdered());
        }
        FormulaAST fcdr = new FormulaAST();
        fcdr.read(this.cdr());
        if (fcdr.isVariable())
            result.add(fcdr.getFormula());
        else {
            if (fcdr.listP())
                result.addAll(fcdr.collectAllVariablesOrdered());
        }
        allVarsCache.addAll(result);
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    @Deprecated
    public boolean logicallyEquals(String s) {

        if (this.equals(s))
            return true;
        if (atom(s) && s.compareTo(getFormula()) != 0)
            return false;
        FormulaAST form = new FormulaAST();
        form.read(this.getFormula());
        FormulaAST sform = new FormulaAST();
        sform.read(s);
        if (AND.equals(form.car().intern()) || OR.equals(form.car().intern()) || XOR.equals(form.car().intern())) {
            if (sform.car().intern() == null ? sform.car().intern() != null : !sform.car().intern().equals(sform.car().intern()))
                return false;
            form.read(form.cdr());
            sform.read(sform.cdr());
            return form.compareFormulaSets(sform.getFormula());
        }
        else {
            FormulaAST newForm = new FormulaAST();
            newForm.read(form.car());
            FormulaAST newSform = new FormulaAST();
            newSform.read(sform.cdr());
            return newForm.logicallyEquals(sform.car()) &&
                newSform.logicallyEquals(form.cdr());
        }
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean logicallyEquals(FormulaAST f) {

        boolean equalStrings = this.equals(f);
        if (equalStrings)
            return true;
        else if (!this.deepEquals(f))
            return false;
        else
            return this.unifyWith(f);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    @Deprecated
    public boolean unifyWith(FormulaAST f) {

        if (debug) System.out.println("Formula.unifyWith(): input f : " + f);
        if (debug) System.out.println("Formula.unifyWith(): input this : " + this);
        FormulaAST f1 = ClausifierExpr.clausify(new FormulaAST(this.getFormula()));
        FormulaAST f2 = ClausifierExpr.clausify(new FormulaAST(f.getFormula()));
        if (debug) System.out.println("Formula.unifyWith(): after clausify f : " + f2);
        if (debug) System.out.println("Formula.unifyWith(): after clausify  this : " + f1);
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        Map<FormulaUtil.FormulaMatchMemoMapKey, List<Set<VariableMapping>>> memoMap = new HashMap<>();
        List<Set<VariableMapping>> result = mapFormulaVariables(new FormulaAST(f1.getFormula()), new FormulaAST(f2.getFormula()), kb, memoMap);
        if (debug) System.out.println("Formula.unifyWith(): variable mapping : " + result);
        return result != null;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean deepEquals(FormulaAST f) {

        if (debug)
            System.out.println("deepEquals(): this: " + this + " arg: " + f);
        if (f == null)
            return false;
        boolean stringsEqual = Objects.equals(this.getFormula(), f.getFormula());
        if (stringsEqual || (this.getFormula() == null || f.getFormula() == null))
            return stringsEqual;
        FormulaAST tmp1 = ClausifierExpr.clausify(new FormulaAST(this.getFormula()));
        FormulaAST tmp2 = ClausifierExpr.clausify(new FormulaAST(f.getFormula()));
        if (debug)
            System.out.println("deepEquals(): clausified this: " + tmp1 + " arg: " + tmp2);
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        String normalized1 = FormulaAST.normalizeParameterOrder(tmp1.getFormula(), kb, true);
        String normalized2 = FormulaAST.normalizeParameterOrder(tmp2.getFormula(), kb, true);
        FormulaAST f1 = new FormulaAST(normalized1);
        FormulaAST f2 = new FormulaAST(normalized2);
        if (debug)
            System.out.println("deepEquals(): normalized this: \n" + f1.format("", "  ", "\n") + "\n arg: \n" + f2.format("", "  ", "\n"));
        normalized1 = ClausifierExpr.normalizeVariables(f1.getFormula(), true);
        normalized2 = ClausifierExpr.normalizeVariables(f2.getFormula(), true);
        if (debug)
            System.out.println("deepEquals(2): normalized this: \n" + f1.format("", "  ", "\n") + "\n arg: \n" + f2.format("", "  ", "\n"));
        return normalized1.equals(normalized2);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isVariable() {
        return isVariable(getFormula());
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isRegularVariable() {
        return !empty() && getFormula().startsWith(V_PREF);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isRowVar() {
        return !empty() && getFormula().startsWith(R_PREF);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isCached() {
        return sourceFile != null && KButilities.isCacheFile(sourceFile);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    // NOTE: FormulaAST also has a boolean `isRule` field set during parsing. This method
    // checks car() == "=>" or "<=>". Both coexist in Java (field vs method namespaces).
    public boolean isRule() {

        boolean ans = false;
        if (this.listP()) {
            String arg0 = this.car();
            if (isQuantifier(arg0)) {
                String arg2 = this.getStringArgument(2);
                if (listP(arg2)) {
                    FormulaAST newF = new FormulaAST();
                    newF.read(arg2);
                    ans = newF.isRule();
                }
            }
            else {
                ans = Arrays.asList(IF, IFF).contains(arg0);
            }
        }
        return ans;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isHorn(KB kb) {

        if (!isRule()) {
            System.out.println("Error in Formula.isHorn(): Formula is not a rule: " + this);
            return false;
        }
        if (isHigherOrder(kb))
            return false;
        if (getFormula().contains(EQUANT) || getFormula().contains(UQUANT))
            return false;
        FormulaAST antecedent = cdrAsFormula().carAsFormula();
        if (!antecedent.isSimpleClause(kb) && !antecedent.car().equals(AND))
            return false;
        FormulaAST consequent = cdrAsFormula().cdrAsFormula().carAsFormula();
        return !(!consequent.isSimpleClause(kb) && !consequent.car().equals(AND));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isModal(KB kb) {

        return (this.isHigherOrder(kb) && this.getFormula().contains("modalAttribute"));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isEpistemic(KB kb) {

        return (this.isHigherOrder(kb) &&
                (this.getFormula().contains("knows") || this.getFormula().contains("believes")));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isTemporal(KB kb) {

        return (this.isHigherOrder(kb) && this.getFormula().contains("holdsDuring"));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isOtherHOL(KB kb) {

        return (this.isHigherOrder(kb) && !this.isTemporal(kb) &&
                !this.isEpistemic(kb) && !this.isModal(kb));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public boolean isTFF(KB kb) {
        isTFF = true;
        return (this.isHigherOrder(kb) && this.isModal(kb) &&
                this.isEpistemic(kb) && this.isTemporal(kb));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String findType(KB kb) {

        StringBuilder sb = new StringBuilder();
        if (this.isBinary()) sb.append("binary, ");
        if (this.isExistentiallyQuantified()) sb.append("existential, ");
        if (this.isFunctionalTerm()) sb.append("functional, ");
        if (this.isHorn(kb)) sb.append("horn, ");
        if (this.isRule()) sb.append("rule, ");
        if (this.isSimpleClause(kb)) sb.append("simple clause, ");
        if (this.isSimpleNegatedClause(kb)) sb.append("simple negated clause, ");
        if (this.isUniversallyQuantified()) sb.append("universal, ");
        if (this.isVariable()) sb.append("variable, ");
        if (this.isHigherOrder(kb)) sb.append("hol, ");
        return sb.toString();
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public Set<String> gatherRelationConstants() {

        Set<String> relations = new HashSet<>();
        Set<String> accumulator = new HashSet<>();
        if (this.listP() && !this.empty())
            accumulator.add(this.getFormula());
        List<String> kifLists = new ArrayList<>();
        FormulaAST f;
        String klist, arg;
        while (!accumulator.isEmpty()) {
            kifLists.clear();
            kifLists.addAll(accumulator);
            accumulator.clear();
            for (Iterator<String> it = kifLists.iterator(); it.hasNext();) {
                klist = it.next();
                if (listP(klist)) {
                    f = new FormulaAST();
                    f.read(klist);
                    for (int i = 0; !f.empty(); i++) {
                        arg = f.car();
                        if (listP(arg)) {
                            if (!empty(arg)) accumulator.add(arg);
                        }
                        else if (isQuantifier(arg)) {
                            accumulator.add(f.getStringArgument(2));
                            break;
                        }
                        else if ((i == 0)
                                 && !isVariable(arg)
                                 && !isLogicalOperator(arg)
                                 && !arg.equals(SKFN)
                                 && !StringUtil.isQuotedString(arg)
                                 && !arg.matches(".*\\s.*")) {
                            relations.add(arg);
                        }
                        f = f.cdrAsFormula();
                    }
                }
            }
        }
        return relations;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public Map<String, List> gatherRelationsWithArgTypes(KB kb) {

        Map<String, List> argtypemap = new HashMap<>();
        Set<String> relations = gatherRelationConstants();
        int atlen;
        List argtypes;
        for (String r : relations) {
            atlen = (MAX_PREDICATE_ARITY + 1);
            argtypes = new ArrayList();
            for (int i = 0; i < atlen; i++)
                argtypes.add(kb.getArgType(r, i));
            argtypemap.put(r, argtypes);
        }
        return argtypemap;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST renameVariableArityRelations(KB kb, Map<String, String> relationMap) {

        FormulaAST result = this;
        if (result.listP()) {
            StringBuilder sb = new StringBuilder();
            FormulaAST f = new FormulaAST();
            f.read(this.getFormula());
            int flen = f.listLength();
            String suffix = ("__" + (flen - 1));
            String arg, func;
            FormulaAST argF;
            sb.append(LP);
            for (int i = 0; i < flen; i++) {
                arg = f.getStringArgument(i);
                if (i > 0)
                    sb.append(SPACE);
                func = "";
                if (kb.kbCache.isInstanceOf(arg, "Function"))
                    func = FN_SUFF;
                if ((i == 0) && kb.kbCache.transInstOf(arg, "VariableArityRelation") && !arg.endsWith(suffix + func)) {
                    relationMap.put(arg + suffix + func, arg);
                    arg += suffix + func;
                }
                else if (listP(arg)) {
                    argF = new FormulaAST();
                    argF.read(arg);
                    arg = argF.renameVariableArityRelations(kb, relationMap).getFormula();
                }
                sb.append(arg);
            }
            sb.append(RP);
            f = new FormulaAST();
            f.read(sb.toString());
            result = f;
        }
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String makeQuantifiersExplicit(boolean query) {

        String result = this.getFormula();
        List<Set<String>> vpair = collectVariables();
        Set<String> unquantVariables = vpair.get(1);
        if (!unquantVariables.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append((query ? "(exists (" : "(forall ("));
            boolean afterTheFirst = false;
            Iterator<String> itu = unquantVariables.iterator();
            while (itu.hasNext()) {
                if (afterTheFirst) sb.append(SPACE);
                sb.append(itu.next());
                afterTheFirst = true;
            }
            sb.append(") ");
            sb.append(this.getFormula());
            sb.append(RP);
            result = sb.toString();
        }
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST replaceQuantifierVars(String quantifier, List<String> vars) throws Exception {

        if (!quantifier.equals(this.car()))
            throw new Exception("The formula is not properly quantified: " + this);
        FormulaAST param = new FormulaAST();
        param.read(this.cadr());
        List<String> existVars = param.complexArgumentsToArrayListString(0);
        if (existVars.size() != vars.size())
            throw new Exception("Wrong number of variables: " + vars + " to substitute in existentially quantified formula: " + this);
        FormulaAST result = this;
        for (int i = 0; i < existVars.size(); i++)
            result = result.replaceVar(existVars.get(i), vars.get(i));
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String format(String hyperlink, String indentChars, String eolChars) {

        if (debug) System.out.println("Formula.format(): " + this.getFormula());
        if (this.getFormula() == null)
            return "";
        if (!StringUtil.emptyString(theFormula))
            theFormula = theFormula.trim();
        if (atom())
            return getFormula();
        String legalTermChars = "-:";
        String varStartChars = "?@";
        StringBuilder token = new StringBuilder();
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;
        boolean inQuantifier = false;
        boolean inToken = false;
        boolean inVariable = false;
        boolean inVarlist = false;
        boolean inComment = false;
        int flen = this.getFormula().length();
        char pch = '0';
        char ch;
        for (int i = 0; i < flen; i++) {
            ch = this.getFormula().charAt(i);
            if (inComment) {
                formatted.append(ch);
                if ((i > 70) && (ch == '/'))
                    formatted.append(SPACE);
                if (ch == '"')
                    inComment = false;
            }
            else {
                if ((ch == '(')
                    && !inQuantifier
                    && ((indentLevel != 0) || (i > 1))) {
                    if ((i > 0) && Character.isWhitespace(pch))
                        formatted = formatted.deleteCharAt(formatted.length() - 1);
                    formatted.append(eolChars);
                    for (int j = 0; j < indentLevel; j++)
                        formatted.append(indentChars);
                }
                if ((i == 0) && (indentLevel == 0) && (ch == '('))
                    formatted.append(ch);
                if (!inToken && !inVariable && Character.isJavaIdentifierStart(ch)) {
                    token = new StringBuilder();
                    inToken = true;
                }
                if (inToken && (Character.isJavaIdentifierPart(ch)
                                || (legalTermChars.indexOf(ch) > -1)))
                    token.append(ch);
                if (ch == '(') {
                    if (inQuantifier) {
                        inQuantifier = false;
                        inVarlist = true;
                        token = new StringBuilder();
                    }
                    else
                        indentLevel++;
                }
                if (ch == '"')
                    inComment = true;
                if (ch == ')') {
                    if (!inVarlist)
                        indentLevel--;
                    else
                        inVarlist = false;
                }
                if ((token.indexOf(UQUANT) > -1) || (token.indexOf(EQUANT) > -1))
                    inQuantifier = true;
                if (inVariable
                    && !Character.isJavaIdentifierPart(ch)
                    && (legalTermChars.indexOf(ch) == -1))
                    inVariable = false;
                if (varStartChars.indexOf(ch) > -1)
                    inVariable = true;
                if (inToken
                    && !Character.isJavaIdentifierPart(ch)
                    && (legalTermChars.indexOf(ch) == -1)) {
                    inToken = false;
                    if (StringUtil.isNonEmptyString(hyperlink)) {
                        formatted.append("<a href=\"");
                        formatted.append(hyperlink);
                        formatted.append("&term=");
                        formatted.append(token);
                        formatted.append("\">");
                        formatted.append(token);
                        formatted.append("</a>");
                    }
                    else
                        formatted.append(token);
                    token = new StringBuilder();
                }
                if ((i > 0) && !inToken && !(Character.isWhitespace(ch) && (pch == '('))) {
                    if (Character.isWhitespace(ch)) {
                        if (!Character.isWhitespace(pch))
                            formatted.append(SPACE);
                    }
                    else
                        formatted.append(ch);
                }
            }
            pch = ch;
        }
        if (inToken) {
            if (StringUtil.isNonEmptyString(hyperlink)) {
                formatted.append("<a href=\"");
                formatted.append(hyperlink);
                formatted.append("&term=");
                formatted.append(token);
                formatted.append("\">");
                formatted.append(token);
                formatted.append("</a>");
            }
            else
                formatted.append(token);
        }
        return formatted.toString();
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    @Override
    public String toString() {

        return format("", "  ", Character.valueOf((char) 10).toString());
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String toStringMeta() {

        return format("", "  ", Character.valueOf((char) 10).toString()) +
                "[" + sourceFile + SPACE + startLine + "-" + endLine + "]";
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String htmlFormat(String html) {

        return format(html, "&nbsp;&nbsp;&nbsp;&nbsp;", "<br>\n");
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String htmlFormat(KB kb, String href) {

        String kbHref = (href + "/sigma/Browse.jsp?kb=" + kb.name);
        return format(kbHref, "&nbsp;&nbsp;&nbsp;&nbsp;", "<br>\n");
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public String toProlog() {

        System.out.println("INFO in Formula.toProlog(): formula: " + getFormula());
        if (!listP()) {
            System.out.println("Error in Formula.toProlog(): Not a formula: " + getFormula());
            return null;
        }
        if (empty()) {
            System.out.println("Error in Formula.toProlog(): Empty formula: " + getFormula());
            return null;
        }
        StringBuilder result = new StringBuilder();
        String rel = car();
        FormulaAST f = new FormulaAST();
        f.setFormula(cdr());
        if (!atom(rel)) {
            System.out.println("Error in Formula.toProlog(): Relation not an atom: " + rel);
            return null;
        }
        result.append(rel).append(LP);
        System.out.println("INFO in Formula.toProlog(): result so far: " + result.toString());
        System.out.println("INFO in Formula.toProlog(): remaining formula: " + f);
        String arg, newVar;
        while (!f.empty()) {
            arg = f.car();
            System.out.println("INFO in Formula.toProlog(): argForm: " + arg);
            f.setFormula(f.cdr());
            if (!atom(arg)) {
                System.err.println("Error in Formula.toProlog(): Argument not an atom: " + arg);
                return null;
            }
            if (isVariable(arg)) {
                newVar = Character.toUpperCase(arg.charAt(1)) + arg.substring(2);
                result.append(newVar);
            }
            else if (StringUtil.isQuotedString(arg))
                result.append(arg);
            else
                result.append("'").append(arg).append("'");
            if (!f.empty())
                result.append(",");
            else
                result.append(RP);
        }
        return result.toString();
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST rename(String term2, String term1) {

        FormulaAST newFormula = new FormulaAST();
        newFormula.read("()");
        if (this.atom()) {
            if (getFormula().equals(term2))
                setFormula(term1);
            return this;
        }
        if (!this.empty()) {
            FormulaAST f1 = new FormulaAST();
            f1.read(this.car());
            if (f1.listP())
                newFormula = newFormula.cons(f1.rename(term2, term1));
            else
                newFormula = newFormula.append(f1.rename(term2, term1));
            FormulaAST f2 = new FormulaAST();
            f2.read(this.cdr());
            newFormula = newFormula.append(f2.rename(term2, term1));
        }
        return newFormula;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public FormulaAST negate() {

        return new FormulaAST(LP + NOT + SPACE + getFormula() + RP);
    }

    // ---------------------------------------------------------------
    // Public static methods — pure copies from Formula
    // ---------------------------------------------------------------

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean listP(String s) {

        boolean ans = false;
        if (!StringUtil.emptyString(s)) {
            String str = s.trim();
            ans = (str.startsWith(LP) && str.endsWith(RP));
        }
        return ans;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean atom(String s) {

        if (StringUtil.emptyString(s)) return false;
        if (StringUtil.isQuotedString(s)) return true;
        String str = s.trim();
        if (str.contains(RP)) return false;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isWhitespace(str.charAt(i))) return false;
        }
        return true;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean empty(String s) {

        if (!listP(s)) return false;
        String str = s.trim();
        for (int i = 1, end = str.length() - 1; i < end; i++) {
            if (!Character.isWhitespace(str.charAt(i))) return false;
        }
        return true;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isVariable(String term) {

        return (!StringUtil.emptyString(term)
                && (term.startsWith(V_PREF) || term.startsWith(R_PREF)));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isCommutative(String obj) {

        return (!StringUtil.emptyString(obj)
                && (obj.equals(AND) || obj.equals(EQUAL) || obj.equals(OR)));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isLogicalOperator(String term) {

        return (!StringUtil.emptyString(term) && LOGICAL_OPERATORS.contains(term));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isQuantifier(String pred) {

        return (!StringUtil.emptyString(pred)
                && (pred.equals(EQUANT) || pred.equals(UQUANT)));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isQuantifierList(String listPred, String previousPred) {

        return ((previousPred.equals(EQUANT) || previousPred.equals(UQUANT)) &&
                (listPred.startsWith(R_PREF) || listPred.startsWith(V_PREF)));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isComparisonOperator(String term) {

        return (!StringUtil.emptyString(term) && COMPARISON_OPERATORS.contains(term));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isInequality(String term) {

        return (!StringUtil.emptyString(term) && INEQUALITIES.contains(term));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isMathFunction(String term) {

        return (!StringUtil.emptyString(term) && MATH_FUNCTIONS.contains(term));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isTrueFalse(String term) {

        return (!term.isEmpty() && (term.equals("true") || term.equals("false")));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isTerm(String term) {

        if (!StringUtil.emptyString(term) && !listP(term) &&
                Character.isJavaIdentifierStart(term.charAt(0))) {
            for (int i = 0; i < term.length(); i++) {
                if (!Character.isJavaIdentifierPart(term.charAt(i)) && term.charAt(i) != '-')
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    @Deprecated
    public static boolean isFunction(String term) {

        System.err.println("Error in Formula.isFuction(): must use KB.isFunction() instead");
        return (!StringUtil.emptyString(term) && (term.endsWith(FN_SUFF)));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    @Deprecated
    public static boolean isFunctionalTerm(String s) {

        FormulaAST f = new FormulaAST();
        f.read(s);
        return f.isFunctionalTerm();
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isSkolemTerm(String term) {
        return (!StringUtil.emptyString(term)
                && term.trim().matches("^.?" + SK_PREF + "\\S*\\s*\\d+"));
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isGround(String form) {

        if (StringUtil.emptyString(form))
            return false;
        if (!form.contains("\""))
            return (!form.contains(V_PREF) && !form.contains(R_PREF));
        boolean inQuote = false;
        for (int i = 0; i < form.length(); i++) {
            if (form.charAt(i) == '"')
                inQuote = !inQuote;
            if ((form.charAt(i) == '?' || form.charAt(i) == '@') && !inQuote)
                return false;
        }
        return true;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isQuery(String query, String formula) {

        FormulaAST f = new FormulaAST();
        f.read(formula);
        return f.equals(query);
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static boolean isNegatedQuery(String query, String formulaString) {

        boolean result = false;
        String fstr = formulaString.trim();
        if (fstr.startsWith("(not")) {
            FormulaAST f = new FormulaAST();
            f.read(fstr);
            result = query.equals(f.getArgument(1));
        }
        return result;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static String postProcess(String s) {

        if (StringUtil.emptyString(s))
            return s;
        s = s.replaceAll("holds_\\d+__ ", "");
        s = s.replaceAll("apply_\\d+__ ", "");
        return s;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static String textFormat(String input) {

        FormulaAST f = new FormulaAST(input);
        return f.format("", "  ", Character.valueOf((char) 10).toString());
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static String removeTemporalRelations(String p_f, KB kb) {

        FormulaAST f = new FormulaAST(p_f);
        String newFormula = "";
        String nextCar = f.car();
        String nextCdr, subFormula, formulaArg;
        FormulaAST subF;
        while (nextCar != null && !nextCar.equals("")) {
            nextCdr = f.cdr();
            if (kb.isChildOf(nextCar, "TemporalRelation")) {
                return "";
            }
            else if (nextCar.matches("^\\s*\\(\\s*and.*")) {
                subFormula = removeTemporalRelations(nextCar, kb);
                subF = new FormulaAST(LP + subFormula + RP);
                if (subF.cddr() == null || subF.cddr().isEmpty() || subF.cddr().equals("()")) {
                    subFormula = subFormula.replaceFirst("^\\s*and\\s+", "");
                }
                else {
                    subFormula = LP + subFormula + RP;
                }
                return newFormula + SPACE + subFormula;
            }
            else if (nextCar.startsWith(LP)) {
                subFormula = removeTemporalRelations(f.car(), kb);
                if (!subFormula.equals("")) {
                    newFormula += LP + subFormula + RP;
                    if (newFormula.endsWith(" )"))
                        newFormula = newFormula.substring(0, newFormula.length() - 2) + RP;
                }
            }
            else if (nextCar.equals("holdsDuring")) {
                formulaArg = f.cddr();
                if (formulaArg != null && formulaArg.length() >= 2 && formulaArg.startsWith(LP) && formulaArg.endsWith(RP)) {
                    return removeTemporalRelations(formulaArg.substring(1, formulaArg.length() - 1), kb);
                }
                return removeTemporalRelations(formulaArg, kb);
            }
            else {
                newFormula += nextCar + SPACE + removeTemporalRelations(nextCar, kb);
            }
            f = new FormulaAST(nextCdr);
            nextCar = f.car();
        }
        return newFormula;
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static List<Set<VariableMapping>> mapFormulaVariables(FormulaAST f1, FormulaAST f2, KB kb,
                                     Map<FormulaUtil.FormulaMatchMemoMapKey, List<Set<VariableMapping>>> memoMap) {

        FormulaUtil.FormulaMatchMemoMapKey key = FormulaUtil.createFormulaMatchMemoMapKey(f1.getFormula(), f2.getFormula());
        if (memoMap.containsKey(key))
            return memoMap.get(key);
        if (f1 == null && f2 == null) {
            List<Set<VariableMapping>> result = new ArrayList<>();
            result.add(new HashSet<>());
            return result;
        }
        else if (f1 == null || f2 == null) {
            return null;
        }
        if (f1.atom() && f2.atom()) {
            if ((f1.isVariable() && f2.isVariable()) || (isSkolemTerm(f1.getFormula()) && isSkolemTerm(f2.getFormula()))) {
                List<Set<VariableMapping>> result = new ArrayList<>();
                Set<VariableMapping> set = new HashSet<>();
                set.add(new VariableMapping(f1.getFormula(), f2.getFormula()));
                result.add(set);
                return result;
            }
            else {
                if (f1.getFormula().equals(f2.getFormula())) {
                    List<Set<VariableMapping>> result = new ArrayList<>();
                    result.add(new HashSet<>());
                    return result;
                }
                else {
                    return null;
                }
            }
        }
        else if (f1.atom() || f2.atom()) {
            return null;
        }
        FormulaAST head1 = new FormulaAST();
        head1.read(f1.car());
        FormulaAST head2 = new FormulaAST();
        head2.read(f2.car());
        List<Set<VariableMapping>> headMaps = mapFormulaVariables(head1, head2, kb, memoMap);
        memoMap.put(FormulaUtil.createFormulaMatchMemoMapKey(head1.getFormula(), head2.getFormula()), headMaps);
        if (headMaps == null)
            return null;
        List<String> args1 = f1.complexArgumentsToArrayListString(1);
        List<String> args2 = f2.complexArgumentsToArrayListString(1);
        if (args1.size() != args2.size())
            return null;
        if (!isCommutative(head1.getFormula()) && !(kb != null && kb.isInstanceOf(head1.getFormula(), "SymmetricRelation"))) {
            List<Set<VariableMapping>> runningMaps = headMaps;
            List<Set<VariableMapping>> parameterMaps;
            FormulaAST parameter1, parameter2;
            for (int i = 0; i < args1.size(); i++) {
                parameter1 = new FormulaAST();
                parameter1.read(args1.get(i));
                parameter2 = new FormulaAST();
                parameter2.read(args2.get(i));
                parameterMaps = mapFormulaVariables(parameter1, parameter2, kb, memoMap);
                memoMap.put(FormulaUtil.createFormulaMatchMemoMapKey(parameter1.getFormula(), parameter2.getFormula()), parameterMaps);
                runningMaps = VariableMapping.intersect(runningMaps, parameterMaps);
                if (runningMaps == null)
                    return null;
            }
            return runningMaps;
        }
        else {
            List<Set<VariableMapping>> unionMaps = new ArrayList<>();
            List<int[]> permutations = FormulaUtil.getPermutations(args1.size(),
                    (a, b) -> mapFormulaVariables(new FormulaAST(args1.get(a)), new FormulaAST(args2.get(b)), kb, memoMap) != null);
            List<Set<VariableMapping>> currentMaps, parameterMaps;
            boolean currentPairingValid;
            FormulaAST parameter1, parameter2;
            for (int[] perm : permutations) {
                currentMaps = headMaps;
                currentPairingValid = true;
                for (int i = 0; i < args1.size(); i++) {
                    parameter1 = new FormulaAST();
                    parameter1.read(args1.get(i));
                    parameter2 = new FormulaAST();
                    parameter2.read(args2.get(perm[i]));
                    parameterMaps = mapFormulaVariables(parameter1, parameter2, kb, memoMap);
                    memoMap.put(FormulaUtil.createFormulaMatchMemoMapKey(parameter1.getFormula(), parameter2.getFormula()), parameterMaps);
                    currentMaps = VariableMapping.intersect(currentMaps, parameterMaps);
                    if (currentMaps == null) {
                        currentPairingValid = false;
                        break;
                    }
                }
                if (currentPairingValid)
                    unionMaps = VariableMapping.union(unionMaps, currentMaps);
            }
            if (unionMaps.isEmpty())
                unionMaps = null;
            return unionMaps;
        }
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  -h - show this help screen");
        System.out.println("  --type \"<formula\" - formula type");
        System.out.println("  --format \"<formula\" - format a formula");
        System.out.println("  --remove \"<formula\" - remove temporal relations on formulas");
    }

    //TODO: pure copy from Formula — refactor to use Expr AST instead of String manipulation
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in Formula.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (argMap.containsKey("type") && argMap.get("type").size() == 1) {
                FormulaAST f = new FormulaAST(argMap.get("type").get(0));
                System.out.println("Formula.main() formula type of " + argMap.get("type").get(0) + " : " + f.findType(kb));
            }
            else if (argMap.containsKey("format") && argMap.get("format").size() == 1) {
                System.out.println(textFormat(argMap.get("format").get(0)));
            }
            else if (argMap.containsKey("remove") && argMap.get("remove").size() == 1) {
                System.out.println(removeTemporalRelations(LP + argMap.get("remove").get(0) + RP, kb));
            }
            else
                showHelp();
        }
    }
}
