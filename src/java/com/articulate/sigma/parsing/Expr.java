package com.articulate.sigma.parsing;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sealed AST node hierarchy for SUO-KIF expressions.
 *
 * ANTLR is used only as the parser front-end; once the Concrete Syntax Tree
 * (CST) has been visited by SuokifToExpr, every ANTLR context object is
 * discarded and downstream code works exclusively with these plain Java records.
 *
 * Grammar summary:
 *   Expr = Atom | Var | RowVar | NumLiteral | StrLiteral | SExpr
 *
 * Naming note: Expr.RowVar (nested record) is distinct from the standalone
 * RowVar.java class in this package, which handles row-variable expansion.
 */
public sealed interface Expr
        permits Expr.Atom, Expr.Var, Expr.RowVar, Expr.NumLiteral, Expr.StrLiteral, Expr.SExpr {

    /** Reconstruct the canonical KIF string for this node. */
    String toKifString();

    // -----------------------------------------------------------------------
    // Leaf nodes
    // -----------------------------------------------------------------------

    /**
     * A constant symbol or operator keyword.
     * Examples: John, likes, =>, forall, and, ListFn
     */
    record Atom(String name) implements Expr {
        public String toKifString() { return name; }
    }

    /**
     * A regular (object) variable.  Always starts with '?'.
     * Examples: ?X, ?ARG, ?SYLLABLE
     */
    record Var(String name) implements Expr {
        public String toKifString() { return name; }
    }

    /**
     * A row variable.  Always starts with '@'.
     * Examples: @ARGS, @ROW
     *
     * Note: this is Expr.RowVar; it is NOT the RowVar class used for
     * row-variable expansion elsewhere in this package.
     */
    record RowVar(String name) implements Expr {
        public String toKifString() { return name; }
    }

    /**
     * A numeric literal.
     * The raw lexeme is stored as a String to preserve the original text
     * (e.g. "3.14" vs "3.1400").
     */
    record NumLiteral(String value) implements Expr {
        public String toKifString() { return value; }
    }

    /**
     * A double-quoted string literal.
     * The value includes the surrounding quotes exactly as they appear
     * in the source (e.g. {@code "\"hello world\""}).
     */
    record StrLiteral(String value) implements Expr {
        public String toKifString() { return value; }
    }

    // -----------------------------------------------------------------------
    // Compound node
    // -----------------------------------------------------------------------

    /**
     * A parenthesised S-expression: {@code (head arg1 arg2 ...)}.
     *
     * <ul>
     *   <li>{@code head} is {@code null} only for the variable-list subexpression
     *       inside a quantifier, e.g. {@code (?X ?Y)} in
     *       {@code (forall (?X ?Y) body)}.  In all other positions head is
     *       non-null.</li>
     *   <li>{@code args} is never null but may be empty for zero-argument
     *       sentences (unusual but syntactically possible).</li>
     * </ul>
     *
     * Examples:
     * <pre>
     *   (likes John Mary)
     *     → SExpr(Atom("likes"), [Atom("John"), Atom("Mary")])
     *
     *   (=> A B)
     *     → SExpr(Atom("=>"), [A, B])
     *
     *   (forall (?X ?Y) body)
     *     → SExpr(Atom("forall"), [SExpr(null,[Var("?X"),Var("?Y")]), body])
     *
     *   (ListFn @ARGS)
     *     → SExpr(Atom("ListFn"), [RowVar("@ARGS")])
     * </pre>
     */
    record SExpr(Expr head, List<Expr> args) implements Expr {

        public String toKifString() {
            StringBuilder sb = new StringBuilder("(");
            if (head != null) {
                sb.append(head.toKifString());
                if (!args.isEmpty()) sb.append(' ');
            }
            sb.append(args.stream()
                    .map(Expr::toKifString)
                    .collect(Collectors.joining(" ")));
            sb.append(')');
            return sb.toString();
        }

        /** Convenience: the name of the head atom, or null if head is not an Atom. */
        public String headName() {
            return (head instanceof Atom a) ? a.name() : null;
        }
    }
}
