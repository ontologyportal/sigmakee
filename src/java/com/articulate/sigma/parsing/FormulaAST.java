package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class FormulaAST extends Formula {

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

    /** Structured AST representation of this formula (Phase 1+). Null until SuokifVisitor populates it. */
    public Expr expr = null;

    public boolean isDoc = false; // a documentation statement that is excluded from theorem proving
    public boolean isRule = false;
    public boolean containsNumber = false;

    public Set<String> antecedentTerms = new HashSet<>();
    public Set<String> consequentTerms = new HashSet<>();

    public String strForm = null; // a String version of this modified formula

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
        this.setFormula(f.getFormula());
        this.expr = f.expr; // Expr nodes are immutable records — safe to share reference
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
    }

    /** ***************************************************************
     */
    public FormulaAST(String f) {
        read(f);
    }

    /** ***************************************************************
     * Set 'theFormula' to the string clear all cache and populate the expr field.
     * @param s - the formula string
     */
    @Override
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
        this.strForm = null;

        SuokifVisitor sv = SuokifVisitor.parseAny(s);
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
        }else{
            System.out.println("[FormulaAST - read] : SuokifVisitor couldn't parse the formula: " + s);
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
    @Override
    public void printCaches() {

        super.printCaches();
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
     * {@link Formula} objects or calling {@code findAllTypeRestrictions}.
     * The logic mirrors {@code Formula.isHigherOrder} exactly.
     */
    static boolean isHigherOrderExpr(Expr expr, KB kb) {

        if (!(expr instanceof Expr.SExpr se)) return false;
        String head = se.headName();
        if (head == null) return false; // var-list node inside a quantifier
        List<String> sig = kb.kbCache.getSignature(head);
        if (sig != null && !Formula.isVariable(head) && sig.contains("Formula"))
            return true;
        boolean logop = Formula.isLogicalOperator(head);
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
    @Override
    public boolean listP() {

        if (expr != null) return expr instanceof Expr.SExpr;
        System.out.println("Formula string-based method used: listP");
        return super.listP();
    }

    /*****************************************************************
     */
    @Override
    public boolean atom() {

        if (expr != null) return !(expr instanceof Expr.SExpr);
        System.out.println("Formula string-based method used: atom");
        return super.atom();
    }

    /** ***************************************************************
     */
    @Override
    public boolean empty() {

        if (expr != null) return expr instanceof Expr.SExpr se && se.head() == null && se.args().isEmpty();
        System.out.println("Formula string-based method used: empty");
        return super.empty();
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
    @Override
    public String car() {

        if (expr == null) {
            System.out.println("Formula string-based method used: car()");
            return super.car();
        }
        if (!(expr instanceof Expr.SExpr)) return null; // atom/var/literal — not a list
        List<Expr> elements = getElements();
        if (elements.isEmpty()) return "";
        return elements.get(0).toKifString();
    }

    /*****************************************************************
     */
    @Override
    public String cadr() {

        if (expr != null) return getStringArgument(1);
        System.out.println("Formula string-based method used: cadr");
        return super.cadr();
    }

    /*****************************************************************
     */
    @Override
    public String caddr() {

        if (expr != null) return getStringArgument(2);
        System.out.println("Formula string-based method used: caddr");
        return super.caddr();
    }

    /*****************************************************************
     */
    @Override
    public String cdr() {

        if (expr == null) {
            System.out.println("Formula string-based method used: cdr()");
            return super.cdr();
        }
        if (!(expr instanceof Expr.SExpr)) return null; // atom/var/literal — not a list
        List<Expr> elements = getElements();
        if (elements.size() <= 1) return "()";
        return "(" + elements.subList(1, elements.size()).stream().map(Expr::toKifString).collect(Collectors.joining(" ")) + ")";
    }

    /*****************************************************************
     */
    @Override
    public String cddr() {

        if (expr != null) {
            Formula fCdr = this.cdrAsFormula();
            if (fCdr != null)
                return fCdr.cdr();
            return null;
        }
        System.out.println("Formula string-based method used: cddr");
        return super.cddr();
    }

    /*****************************************************************
     */
    @Override
    public Formula carAsFormula() {

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
        return super.carAsFormula();
    }

    /*****************************************************************
     */
    @Override
    public Formula cdrAsFormula() {

        if (expr != null) {
            List<Expr> elements = getElements();
            FormulaAST f = new FormulaAST();
            if (elements.size() <= 1) {
                f.setFormula("()");
                return f;
            }
            List<Expr> cdrElements = elements.subList(1, elements.size());
            f.setFormula("(" + cdrElements.stream().map(Expr::toKifString).collect(Collectors.joining(" ")) + ")");
            f.expr = new Expr.SExpr(null, cdrElements);
            return f;
        }
        System.out.println("Formula string-based method used: cdrAsFormula");
        return super.cdrAsFormula();
    }

    /*****************************************************************
     */
    @Override
    public Formula cddrAsFormula() {

        if (expr != null) {
            Formula cdr = cdrAsFormula();
            if (cdr == null || cdr.getFormula().equals("()")) {
                FormulaAST f = new FormulaAST();
                f.setFormula("()");
                return f;
            }
            return cdr.cdrAsFormula();
        }
        System.out.println("Formula string-based method used: cddrAsFormula");
        return super.cddrAsFormula();
    }

    /*****************************************************************
     */
    @Override
    public String getStringArgument(int argnum) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (argnum >= 0 && argnum < elements.size()) return elements.get(argnum).toKifString();
            return "";
        }
        System.out.println("Formula string-based method used: getStringArgument");
        return super.getStringArgument(argnum);
    }

    /*****************************************************************
     */
    @Override
    public Formula getArgument(int argnum) {

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
        return super.getArgument(argnum);
    }

    /*****************************************************************
     */
    @Override
    public int listLength() {

        if (expr != null) {
            if (!(expr instanceof Expr.SExpr)) return -1;
            return getElements().size();
        }
        System.out.println("Formula string-based method used: listLength");
        return super.listLength();
    }

    /*****************************************************************
     */
    @Override
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
        return super.argumentsToArrayListString(start);
    }

    /*****************************************************************
     */
    @Override
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
        return super.complexArgumentsToArrayListString(start);
    }

    /*****************************************************************
     */
    @Override
    public List<Formula> complexArgumentsToArrayList(int start) {

        if (expr != null) {
            List<Expr> elements = getElements();
            if (start < 0 || start >= elements.size()) return null;
            List<Formula> res = new ArrayList<>();
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
        return super.complexArgumentsToArrayList(start);
    }

    /*****************************************************************
     */
    @Override
    public List<String> literalToArrayList() {

        if (expr != null) return complexArgumentsToArrayListString(0);
        System.out.println("Formula string-based method used: literalToArrayList");
        return super.literalToArrayList();
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
    @Override
    public Set<String> collectAllVariables() {

        if (expr != null) {
            if (!allVarsCache.isEmpty()) return allVarsCache;
            collectAllVars(expr, allVarsCache);
            return allVarsCache;
        }
        System.out.println("Formula string-based method used: collectAllVariables");
        return super.collectAllVariables();
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
    @Override
    public Set<String> collectQuantifiedVariables() {

        if (expr != null) {
            if (!quantVarsCache.isEmpty()) return quantVarsCache;
            collectQuantVars(expr, quantVarsCache);
            return quantVarsCache;
        }
        System.out.println("Formula string-based method used: collectQuantifiedVariables");
        return super.collectQuantifiedVariables();
    }

    /** ***************************************************************
     */
    @Override
    public Set<String> collectUnquantifiedVariables() {

        if (expr != null) {
            // We always re-collect from Expr to ensure correctness, ignoring any visitor-populated caches
            Set<String> freeVars = ExprToTPTP.collectFreeVars(expr);
            this.unquantVarsCache.clear();
            this.unquantVarsCache.addAll(freeVars);
            return this.unquantVarsCache;
        }
        System.out.println("Formula string-based method used: collectUnquantifiedVariables");
        return super.collectUnquantifiedVariables();
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
    @Override
    public Set<String> collectTerms() {

        if (expr != null) {
            this.termCache.clear();
            collectTermsHelper(expr, this.termCache);
            return this.termCache;
        }
        System.out.println("Formula string-based method used: collectTerms");
        return super.collectTerms();
    }


    /*****************************************************************
     */
    @Override
    public boolean isHigherOrder(KB kb) {

        if (expr != null) {
            boolean hol = isHigherOrderExpr(expr, kb);
            if (hol) this.higherOrder = true;
            return hol;
        }
        System.out.println("Formula string-based method used: isHigherOrder");
        return super.isHigherOrder(kb);
    }

    /*****************************************************************
     */
    @Override
    public boolean isGround() {

        if (expr != null) {
            return collectAllVariables().isEmpty();
        }
        System.out.println("Formula string-based method used: isGround");
        return super.isGround();
    }

    /*****************************************************************
     */
    @Override
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
        System.out.println("Formula string-based method used: isSimpleClause");
        return super.isSimpleClause(kb);
    }

    /*****************************************************************
     */
    @Override
    public boolean isSimpleNegatedClause(KB kb) {

        if (expr instanceof Expr.SExpr se && "not".equals(se.headName()) && se.args().size() == 1) {
            Expr arg = se.args().get(0);
            if (arg instanceof Expr.SExpr) {
                FormulaAST fa = new FormulaAST();
                fa.expr = arg;
                fa.setFormula(arg.toKifString());
                return fa.isSimpleClause(kb);
            }
        }
        System.out.println("Formula string-based method used: isSimpleNegatedClause");
        return super.isSimpleNegatedClause(kb);
    }

    /*****************************************************************
     */
    @Override
    public boolean isFunctionalTerm() {

        if (expr instanceof Expr.SExpr se) {
            String head = se.headName();
            return head != null && head.endsWith("Fn");
        }
        System.out.println("Formula string-based method used: isFunctionalTerm");
        return super.isFunctionalTerm();
    }

    /*****************************************************************
     */
    @Override
    public boolean isBinary() {

        if (expr instanceof Expr.SExpr se) {
            return se.args().size() == 2;
        }
        System.out.println("Formula string-based method used: isBinary");
        return super.isBinary();
    }

    /** ***************************************************************
     */
    @Override
    public boolean isExistentiallyQuantified() {

        if (expr instanceof Expr.SExpr se) return "exists".equals(se.headName());
        System.out.println("Formula string-based method used: isExistentiallyQuantified");
        return super.isExistentiallyQuantified();
    }

    /*****************************************************************
     */
    @Override
    public boolean isUniversallyQuantified() {

        if (expr instanceof Expr.SExpr se) return "forall".equals(se.headName());
        System.out.println("Formula string-based method used: isUniversallyQuantified");
        return super.isUniversallyQuantified();
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
    @Override
    public FormulaAST substituteVariables(Map<String, String> m) {

        if (expr != null) {
            Expr newExpr = substituteExpr(expr, m);
            FormulaAST fa = new FormulaAST();
            fa.expr = newExpr;
            fa.setFormula(newExpr.toKifString());
            return fa;
        }
        System.out.println("Formula string-based method used: substituteVariables");
        Formula base = super.substituteVariables(m);
        if (base == null) return null;
        FormulaAST wrapped = new FormulaAST();
        wrapped.setFormula(base.getFormula());
        return wrapped;
    }

    /*****************************************************************
     */
    @Override
    public FormulaAST replaceVar(String var, String term) {

        if (expr != null) {
            Map<String, String> m = new HashMap<>();
            m.put(var, term);
            return substituteVariables(m);
        }
        System.out.println("Formula string-based method used: replaceVar");
        Formula base = super.replaceVar(var, term);
        if (base == null) return null;
        FormulaAST wrapped = new FormulaAST();
        wrapped.setFormula(base.getFormula());
        return wrapped;
    }
}
