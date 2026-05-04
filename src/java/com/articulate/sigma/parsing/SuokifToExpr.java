package com.articulate.sigma.parsing;

import com.articulate.sigma.utils.StringUtil;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an ANTLR Concrete Syntax Tree (CST) produced by {@link SuokifParser}
 * into a clean {@link Expr} Abstract Syntax Tree.
 *
 * Once this conversion is done, all ANTLR context objects can be discarded;
 * downstream code uses only the Expr hierarchy.
 *
 * Usage:
 * <pre>
 *   SuokifVisitor visitor = SuokifVisitor.parseString("(likes John Mary)");
 *   FormulaAST f = visitor.result.values().iterator().next();
 *   Expr expr = SuokifToExpr.convert(f.parsedFormula);
 * </pre>
 */
public class SuokifToExpr {

    /** Convert a SentenceContext (top-level formula) to an Expr. */
    public static Expr convert(SuokifParser.SentenceContext ctx) {
        if (ctx == null) return null;
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.RelsentContext)
                return convertRelsent((SuokifParser.RelsentContext) c);
            if (c instanceof SuokifParser.LogsentContext)
                return convertLogsent((SuokifParser.LogsentContext) c);
            if (c instanceof SuokifParser.QuantsentContext)
                return convertQuantsent((SuokifParser.QuantsentContext) c);
            if (c instanceof SuokifParser.VariableContext)
                return convertVariable((SuokifParser.VariableContext) c);
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Relational sentence:  (predicate arg+) or (variable arg+)
    // -----------------------------------------------------------------------

    private static Expr convertRelsent(SuokifParser.RelsentContext ctx) {
        Expr head = null;
        List<Expr> args = new ArrayList<>();
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.VariableContext) {
                if (head == null)
                    head = convertVariable((SuokifParser.VariableContext) c);
                else
                    args.add(convertVariable((SuokifParser.VariableContext) c));
            } else if (c instanceof SuokifParser.ArgumentContext) {
                args.add(convertAny((SuokifParser.ArgumentContext) c));
            } else {
                // IDENTIFIER terminal (the predicate name)
                String text = c.getText();
                if (!text.equals("(") && !text.equals(")") && head == null)
                    head = new Expr.Atom(text);
            }
        }
        return new Expr.SExpr(head, args);
    }

    // -----------------------------------------------------------------------
    // Logical connectives
    // -----------------------------------------------------------------------

    private static Expr convertLogsent(SuokifParser.LogsentContext ctx) {
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.NotsentContext)
                return convertNotsent((SuokifParser.NotsentContext) c);
            if (c instanceof SuokifParser.AndsentContext)
                return convertNarySent("and", (SuokifParser.AndsentContext) c);
            if (c instanceof SuokifParser.OrsentContext)
                return convertNarySent("or",  (SuokifParser.OrsentContext) c);
            if (c instanceof SuokifParser.XorsentContext)
                return convertNarySent("xor", (SuokifParser.XorsentContext) c);
            if (c instanceof SuokifParser.ImpliesContext)
                return convertBinarySent("=>",   (SuokifParser.ImpliesContext) c);
            if (c instanceof SuokifParser.IffContext)
                return convertBinarySent("<=>",  (SuokifParser.IffContext) c);
            if (c instanceof SuokifParser.EqsentContext)
                return convertEqsent((SuokifParser.EqsentContext) c);
        }
        return null;
    }

    /** notsent : '(' 'not' sentence ')' */
    private static Expr convertNotsent(SuokifParser.NotsentContext ctx) {
        Expr body = null;
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.SentenceContext)
                body = convert((SuokifParser.SentenceContext) c);
        }
        return new Expr.SExpr(new Expr.Atom("not"), body == null ? List.of() : List.of(body));
    }

    /** andsent / orsent / xorsent : '(' op sentence sentence+ ')' */
    private static Expr convertNarySent(String op, org.antlr.v4.runtime.ParserRuleContext ctx) {
        List<Expr> children = new ArrayList<>();
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.SentenceContext)
                children.add(convert((SuokifParser.SentenceContext) c));
        }
        return new Expr.SExpr(new Expr.Atom(op), children);
    }

    /** implies / iff : '(' op sentence sentence ')' */
    private static Expr convertBinarySent(String op, org.antlr.v4.runtime.ParserRuleContext ctx) {
        List<Expr> args = new ArrayList<>();
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.SentenceContext)
                args.add(convert((SuokifParser.SentenceContext) c));
        }
        return new Expr.SExpr(new Expr.Atom(op), args);
    }

    /** eqsent : '(' 'equal' term term ')' */
    private static Expr convertEqsent(SuokifParser.EqsentContext ctx) {
        List<Expr> args = new ArrayList<>();
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.TermContext)
                args.add(convertTerm((SuokifParser.TermContext) c));
        }
        return new Expr.SExpr(new Expr.Atom("equal"), args);
    }

    // -----------------------------------------------------------------------
    // Quantifiers
    // -----------------------------------------------------------------------

    /** quantsent : (forall | exists) */
    private static Expr convertQuantsent(SuokifParser.QuantsentContext ctx) {
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.ForallContext)
                return convertQuantifier("forall", (SuokifParser.ForallContext) c);
            if (c instanceof SuokifParser.ExistsContext)
                return convertQuantifier("exists", (SuokifParser.ExistsContext) c);
        }
        return null;
    }

    /**
     * forall : '(' 'forall' '(' variable+ ')' sentence ')' ;
     * exists : '(' 'exists' '(' variable+ ')' sentence ')' ;
     *
     * Result: SExpr(Atom("forall"), [SExpr(null, [vars...]), body])
     * The inner SExpr with null head represents the variable list.
     */
    private static Expr convertQuantifier(String keyword, org.antlr.v4.runtime.ParserRuleContext ctx) {
        List<Expr> varList = new ArrayList<>();
        Expr body = null;
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.VariableContext)
                varList.add(convertVariable((SuokifParser.VariableContext) c));
            else if (c instanceof SuokifParser.SentenceContext)
                body = convert((SuokifParser.SentenceContext) c);
        }
        Expr varListExpr = new Expr.SExpr(null, varList);
        List<Expr> args = new ArrayList<>();
        args.add(varListExpr);
        if (body != null) args.add(body);
        return new Expr.SExpr(new Expr.Atom(keyword), args);
    }

    // -----------------------------------------------------------------------
    // Terms
    // -----------------------------------------------------------------------

    /** argument : (sentence | term) */
    public static Expr convertAny(SuokifParser.ArgumentContext ctx) {
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.SentenceContext)
                return convert((SuokifParser.SentenceContext) c);
            if (c instanceof SuokifParser.TermContext)
                return convertTerm((SuokifParser.TermContext) c);
        }
        return null;
    }

    /** term : (funterm | variable | string | number | FUNWORD | IDENTIFIER) */
    private static Expr convertTerm(SuokifParser.TermContext ctx) {
        for (ParseTree c : ctx.children) {
            if (c instanceof SuokifParser.FuntermContext)
                return convertFunterm((SuokifParser.FuntermContext) c);
            if (c instanceof SuokifParser.VariableContext)
                return convertVariable((SuokifParser.VariableContext) c);
            if (c instanceof SuokifParser.StringContext)
                // Normalize whitespace in quoted strings to match KIF's StreamTokenizer behaviour
                // (KIF's normalizeSpaceChars() collapses all whitespace sequences to single spaces).
                return new Expr.StrLiteral(StringUtil.normalizeSpaceChars(c.getText()));
            if (c instanceof SuokifParser.NumberContext)
                return new Expr.NumLiteral(c.getText());
        }
        // FUNWORD or IDENTIFIER terminal
        String text = ctx.getText();
        return new Expr.Atom(text);
    }

    /** funterm : '(' FUNWORD argument+ ')' */
    private static Expr convertFunterm(SuokifParser.FuntermContext ctx) {
        Expr head = null;
        List<Expr> args = new ArrayList<>();
        for (ParseTree c : ctx.children) {
            if (c.getText().equals("(") || c.getText().equals(")")) continue;
            if (head == null && ctx.FUNWORD() != null && c.getText().equals(ctx.FUNWORD().getText())) {
                head = new Expr.Atom(c.getText());
            } else if (c instanceof SuokifParser.ArgumentContext) {
                args.add(convertAny((SuokifParser.ArgumentContext) c));
            }
        }
        return new Expr.SExpr(head, args);
    }

    /** variable : (REGVAR | ROWVAR) */
    private static Expr convertVariable(SuokifParser.VariableContext ctx) {
        if (ctx.ROWVAR() != null)
            return new Expr.RowVar(ctx.ROWVAR().getText());
        return new Expr.Var(ctx.REGVAR().getText());
    }
}
