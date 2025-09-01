/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

package com.articulate.sigma;
import java.io.*;
import java.util.*;

/**
 * Validates a .kif file for common SUO-KIF issues:
 *   • Balanced parentheses across lines
 *   • Logical operator shapes (via Formula.validArgs + a few explicit checks)
 *   • Unquantified variables (checked ONLY at top level)
 *   • Arity checks (KB signatures with small built-ins)
 *   • Light semantics: arguments “below Entity” for instance/subclass
 *     using Diagnostics.termNotBelowEntity(...) + local declarations
 *   • Unknown head symbol (flags misspelled predicates like "ibnstance")
 *   • Recurses into nested formulas (and/or/not/exists/forall/=>/<=>)
 */
public class KifFileChecker {

    private static final Map<String, Integer> BUILTIN_ARITY = new HashMap<>();
    static {
        BUILTIN_ARITY.put("instance", 2);
        BUILTIN_ARITY.put("subclass", 2);
        BUILTIN_ARITY.put("names",    2);
        BUILTIN_ARITY.put("editor",   2);
        BUILTIN_ARITY.put("attribute",2);
    }

    /** *************************************************************
     * Validate a character stream of SUO-KIF with optional “below Entity”
     * semantic checks.

     * @param reader character stream containing SUO-KIF
     * @param sourceName a label used in messages (e.g., file name)
     * @param includeBelow when true, enforce “below Entity/Attribute” checks
     * @return a list of error/warning messages; empty if none
     * @throws IOException if reading the stream fails
     */
    public static List<String> check(Reader reader, String sourceName, boolean includeBelow) throws IOException {

        final List<String> out = new ArrayList<>();
        final StringBuilder buf = new StringBuilder();     // preserves newlines
        final Set<String> localClasses = new HashSet<>();
        int lineNo = 0, startLine = 0;
        final KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        try (BufferedReader br = new BufferedReader(reader)) {
            String raw;
            while ((raw = br.readLine()) != null) {
                lineNo++;
                final String trimmed = raw.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(";")) continue;

                if (buf.length() == 0) {
                    startLine = lineNo;
                } else {
                    buf.append('\n');               // <— key change
                }
                buf.append(raw);                    // keep original spacing/newlines
                final String chunkRaw = buf.toString();
                final String chunk    = chunkRaw.trim();
                Formula probe = new Formula(); probe.read(chunk);
                if (!probe.isBalancedList()) continue;
                Formula f = new Formula();
                f.read(chunk);
                f.startLine = startLine;
                f.endLine   = lineNo;
                f.setSourceFile(sourceName);
                validateDeep(f, sourceName, startLine, kb, localClasses, out,
                            /*topLevel=*/true, includeBelow, chunkRaw);
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) out.add(lineMsg(startLine, "Unbalanced parentheses nearby."));
        return out;
    }

    /** *************************************************************
    * Recursively validate a formula and all sentence-valued subformulas.
    * 
    * @param f the current formula
    * @param fileName source label for messages
    * @param startLine first line number of the current balanced chunk
    * @param kb the knowledge base used for signatures and taxonomy
    * @param localClasses locally declared classes collected so far
    * @param out destination list for messages
    * @param topLevel true when f is the top-level formula of the chunk
    * @param includeBelow toggle for semantic checks that rely on the taxonomy
    */
    private static void validateDeep(Formula f,
                                    String fileName,
                                    int startLine,
                                    KB kb,
                                    Set<String> localClasses,
                                    List<String> out,
                                    boolean topLevel,
                                    boolean includeBelow,
                                    String sourceText) {

        // Learn any local class facts introduced by this literal (e.g., subclass/instance Class)
        recordLocalClassFacts(f, localClasses, kb, includeBelow);

        // Validate this literal itself (quantifier check only at top level)
        out.addAll(validateFormula(f, fileName, startLine, kb, localClasses, topLevel, includeBelow));

        // Recurse into sentence-valued arguments
        final String pred = f.car();
        if (pred == null || pred.isEmpty()) return;

        final List<String> args = f.argumentsToArrayListString(1);
        if (args == null) return;

        switch (pred) {
            case "not":
                if (!args.isEmpty())
                    descend(args.get(0), f, fileName, startLine, kb, localClasses, out,
                            /*topLevel=*/false, includeBelow, sourceText);
                break;

            case "and":
            case "or":
            case "=>":
            case "<=>":
                for (String a : args)
                    descend(a, f, fileName, startLine, kb, localClasses, out,
                            /*topLevel=*/false, includeBelow, sourceText);
                break;

            case "exists":
            case "forall":
                // (exists (?vars) BODY) — args: [varlist, body]
                if (args.size() >= 2)
                    descend(args.get(1), f, fileName, startLine, kb, localClasses, out,
                            /*topLevel=*/false, includeBelow, sourceText);
                break;

            default:
                // Non-logical heads have no sentence-valued arguments to descend into
                break;
        }
    }


    /** *************************************************************
     * Parse and validate a child S-expression if present.
     * 
     * @param s child string (potential S-expression)
     * @param parent the parent formula (used for end line and source)
     * @param fileName source label
     * @param startLine start line of the parent chunk
     * @param kb knowledge base for semantic/arity checks
     * @param localClasses locally declared classes
     * @param out destination list for messages
     * @param topLevel always false for descendants
     * @param includeBelow toggle for semantic checks
     */
    private static void descend(String s,
                                Formula parent,
                                String fileName,
                                int parentStartLine,
                                KB kb,
                                Set<String> localClasses,
                                List<String> out,
                                boolean topLevel,
                                boolean includeBelow,
                                String parentText) {
        if (s == null) return;
        s = s.trim();
        if (!s.startsWith("(")) return; // quick guard against constants/vars/strings

        Formula g = new Formula();
        g.read(s);
        if (!g.isBalancedList()) return;

        // Compute child's own start line by locating it inside the parent's raw text
        int childStartLine = estimateStartLine(parentText, s, parentStartLine);

        g.startLine = childStartLine;
        g.endLine   = parent.endLine;
        g.setSourceFile(fileName);

        // For the next level, the child's own raw text becomes the new sourceText
        validateDeep(g, fileName, childStartLine, kb, localClasses, out,
                    /*topLevel=*/false, includeBelow, s);
    }





    // Make any message single-line (remove all newlines)
    private static String flatten(String s) {
        return (s == null) ? "" : s.replaceAll("[\\r\\n]+", " ").trim();
    }

    // Compact a formula to a single inline snippet (no excessive whitespace)
    private static String inlineFormula(Formula f) {
        String s = (f == null) ? "" : f.getFormula();
        return (s == null) ? "" : s.replaceAll("\\s+", " ").trim();
    }

    // Dedup + add a line-prefixed, single-line message
    private static void addMsg(List<String> errors, Set<String> seen, int line, String msg) {
        String one = flatten(msg);
        if (seen.add(one)) errors.add(lineMsg(line, one));
    }

    private static String oneLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return (i >= 0) ? s.substring(0, i) : s;
    }

    /** *************************************************************
     * Validate one balanced formula (non-recursive).

    * @param f the formula to validate
    * @param fileName source label for messages
    * @param startLine first line number of the current balanced chunk
    * @param kb knowledge base for signatures/taxonomy
    * @param localClasses locally declared classes available for this chunk
    * @param topLevel true to enable the unquantified-variable check
    * @param includeBelow toggle for taxonomy-based checks
    * @return a list of messages for this formula; possibly empty
    */
    private static List<String> validateFormula(Formula f, String fileName, int startLine,
                                            KB kb, Set<String> localClasses,
                                            boolean topLevel, boolean includeBelow) {
        final List<String> errors = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        // Built-in validator messages
        String argError = f.validArgs(fileName, startLine);
        if (!argError.isEmpty()) addMsg(errors, seen, startLine, argError);
        for (String err : f.getErrors()) addMsg(errors, seen, startLine, err);

        // Unquantified variables (top-level only)
        if (topLevel) {
            Set<String> unq = f.collectUnquantifiedVariables();
            if (!f.isRule() && !unq.isEmpty())
                addMsg(errors, seen, startLine, "Unquantified variables → " + unq);
        }

        final String pred = f.car();
        final List<String> args = (pred == null || pred.isEmpty()) ? null : f.argumentsToArrayListString(1);

        // Unknown head
        if (includeBelow && pred != null && !pred.isEmpty()
                && !Formula.isLogicalOperator(pred) && !Formula.isVariable(pred)) {
            boolean known = (kb != null) && !Diagnostics.termNotBelowEntity(pred, kb);
            if (!known)
                addMsg(errors, seen, startLine, "Unknown predicate/operator '" + pred +
                        "' (not a SUMO term below Entity).");
        }

        if (pred != null && !pred.isEmpty()) {
            // Operator shapes
            if ("not".equals(pred) && size(args) != 1)
                addMsg(errors, seen, startLine, "'not' must have exactly one argument → " + inlineFormula(f));

            if (("=>".equals(pred) || "<=>".equals(pred)) && size(args) != 2)
                addMsg(errors, seen, startLine, "'" + pred + "' must have exactly two arguments → " + inlineFormula(f));

            // Arity
            int expected = expectedArity(pred, kb);
            if (expected != -1 && args != null && args.size() != expected)
                addMsg(errors, seen, startLine, "Arity error for '" + pred + "'. Expected " + expected +
                        " args, found " + args.size() + " → " + inlineFormula(f));

            // Light semantic checks
            if (kb != null && args != null && includeBelow) {
                if ("instance".equals(pred) && args.size() == 2) {
                    String cls = args.get(1);
                    if (!isBelowEntity(cls, localClasses, kb, true))
                        addMsg(errors, seen, startLine, "'instance' expects a Class below Entity as arg2, found: " + cls);
                } else if ("subclass".equals(pred) && args.size() == 2) {
                    boolean ok1 = isBelowEntity(args.get(0), localClasses, kb, true);
                    boolean ok2 = isBelowEntity(args.get(1), localClasses, kb, true);
                    if (!ok1 || !ok2)
                        addMsg(errors, seen, startLine, "'subclass' expects Classes below Entity → " + inlineFormula(f));
                } else if ("attribute".equals(pred) && args.size() == 2) {
                    String attr = args.get(1);
                    boolean attrOK = kb.kbCache.subclassOf(attr, "Attribute") || kb.kbCache.transInstOf(attr, "Attribute");
                    if (!attrOK)
                        addMsg(errors, seen, startLine, "'attribute' expects arg2 below Attribute, found: " + attr);
                }
            }

            // names("... ", x)
            if ("names".equals(pred) && args != null && args.size() == 2) {
                String s = args.get(0);
                if (!isQuoted(s))
                    addMsg(errors, seen, startLine, "'names' arg1 must be a quoted string → " + inlineFormula(f));
            }
        }

        return errors;
    }


    /** Best-effort line number for a child S-expression within its parent's raw text. */
    private static int estimateStartLine(String parentText, String childText, int parentStartLine) {
        if (parentText == null || childText == null) return parentStartLine;
        int idx = parentText.indexOf(childText);
        if (idx < 0) return parentStartLine;  // fallback if not found

        int newlines = 0;
        for (int i = 0; i < idx; i++) {
            if (parentText.charAt(i) == '\n') newlines++;
        }
        return parentStartLine + newlines;
    }

    /** *************************************************************
     * Record locally declared classes introduced by the current literal.
     *
     * @param f the current formula
     * @param localClasses mutable set of locally known classes
     * @param kb knowledge base used to confirm C2 where applicable
     * @param includeBelow whether to consult the KB for C2
     */
    private static void recordLocalClassFacts(Formula f, Set<String> localClasses, KB kb, boolean includeBelow) {
        
        final String pred = f.car();
        if (pred == null || pred.isEmpty()) 
            return;
        final List<String> args = f.argumentsToArrayListString(1);
        if (args == null) 
            return;
        if ("subclass".equals(pred) && args.size() == 2) {
            localClasses.add(args.get(0));
            if (includeBelow && kb != null && !Diagnostics.termNotBelowEntity(args.get(1), kb)) {
                localClasses.add(args.get(1));
            }
        } else if ("instance".equals(pred) && args.size() == 2 && "Class".equals(args.get(1))) {
            localClasses.add(args.get(0));
        }
    }

    /** *************************************************************
     * Return true if term should be considered “below Entity”.
     *
     * @param term symbol to test
     * @param localClasses locally declared classes for the current chunk
     * @param kb knowledge base providing taxonomy
     * @param includeBelow toggle for performing the taxonomy check
     * @return true if considered below Entity; false otherwise
     */
    private static boolean isBelowEntity(String term, Set<String> localClasses, KB kb, boolean includeBelow) {

        if (!includeBelow) return true; 
        if (term == null || term.isEmpty()) return false;
        if (localClasses != null && localClasses.contains(term)) return true;
        if (kb == null) return false;
        return !Diagnostics.termNotBelowEntity(term, kb);
    }

    /** *************************************************************
     * Determine expected arity for a predicate.

    * @param pred predicate symbol
    * @param kb knowledge base to query for the signature
    * @return expected number of arguments, or -1 if not known
    */
    private static int expectedArity(String pred, KB kb) {

        if (kb == null || kb.kbCache == null) return -1;
        final List<String> sig = kb.kbCache.getSignature(pred);
        return (sig == null) ? -1 : sig.size() - 1; // subtract predicate position
    }

    /** *************************************************************
     * Null-safe list size helper.
     *
     * @param l list whose size is needed
     * @return 0 if is null; otherwise l.size()
     */
    private static int size(List<?> l) { 
        
        return (l == null) ? 0 : l.size(); 
    }

    /** *************************************************************
     * Test whether a string is a quoted literal (minimal check).
     *
     * @param s candidate string
     * @return true if non-null, length ≥ 2, starts and ends with a double quote
     */
    private static boolean isQuoted(String s) {

        return s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"");
    }

    /** *************************************************************
     * Format a message prefixed with a line number.
     *
     * @param line the starting line of the formula that produced the message
     * @param msg the human-readable message
     * @return a string in the form "Line <line>: <msg>"
     */
    private static String lineMsg(int line, String msg) {

        return "Line " + line + ": " + msg;
    }

}
