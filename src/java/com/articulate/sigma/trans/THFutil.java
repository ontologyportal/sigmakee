package com.articulate.sigma.trans;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class THFutil {


    /**
     * Full preprocessing pipeline for a Vampire THF proof before it is handed to
     * the FOF/KIF visitor (TPTPVisitor / TPTP3ProofProcessor).
     *
     * Step 1 — Structure: join multi-line thf() blocks and reorder to forward proof order.
     *   Vampire may emit a single thf() formula spread over several newlines and outputs steps
     *   in reverse dependency order.  The proof section (between SZS output start/end) is
     *   extracted, multi-line blocks are joined until the closing ").", the DAG is trimmed to
     *   the subgraph reachable from $false, and a topological sort ensures each formula appears
     *   after every formula it depends on.
     *
     * Step 2 — Negated-quantifier wrapping: "~ ? [X] : ..." → "~(? [X] : ...)".
     *   The ANTLR THF grammar rule for negation is  thf_unary_formula := '~' '(' logic ')'.
     *   Without the explicit parentheses the parser cannot match the rule.
     *
     * Step 3 — Per-formula normalisation for the FOF visitor, applied to every thf()/tff() line:
     *   a. wrapNegationOnBareAtoms  — "~atom" → "~(atom)",  "~f(x)" → "~(f(x))"
     *      so bare negations become valid thf_unary_formula nodes.
     *   b. rewriteNegatedBinders    — "~! [X] : (Φ)" → "? [X] : ~(Φ)" and vice versa,
     *      pushing the negation inside so that the quantifier structure is visible.
     *   c. expandAndUntypeBinders   — "! [X:$i, Y:$i] : Φ" → "! [X] : (! [Y] : Φ)",
     *      strips HOL type annotations and splits multi-variable binders into nested ones.
     */
    public static List<String> preprocessTHFProof(List<String> lines) {
        // Step 1: locate the SZS proof block, join multi-line blocks, prune to the
        // proof-relevant DAG reachable from $false, and topologically sort.
        int start = -1, end = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("SZS output start")) start = i;
            if (lines.get(i).contains("SZS output end"))   { end = i; break; }
        }
        if (start >= 0 && end > start) {
            List<String> before      = start > 0            ? lines.subList(0, start)          : Collections.emptyList();
            List<String> proofLines  = new ArrayList<>(lines.subList(start + 1, end));
            List<String> after       = end + 1 < lines.size() ? lines.subList(end + 1, lines.size()) : Collections.emptyList();
            List<String> rebuilt = new ArrayList<>(before);
            rebuilt.add(lines.get(start));
            rebuilt.addAll(reorderProofSection(proofLines));
            rebuilt.add(lines.get(end));
            rebuilt.addAll(after);
            lines = rebuilt;
        }

        // Step 2: wrap bare negated quantifiers before the per-formula pass
        lines = fixNegatedQuantifiers(lines);

        // Step 3: per-formula normalisation
        List<String> out = new ArrayList<>(lines.size());
        boolean inProof = false;
        for (String line : lines) {
            if (line.contains("SZS output start")) { inProof = true;  out.add(line); continue; }
            if (line.contains("SZS output end"))   { inProof = false; out.add(line); continue; }
            if (!inProof) { out.add(line); continue; }

            String work = line;
            if (work.matches("^\\d+\\..*")) { int p = work.indexOf('.'); work = work.substring(p + 1).trim(); }

            if (work.trim().startsWith("thf(") || work.trim().startsWith("tff(")) {
                work = normalizeTHFForFOFVisitor(work);
            }
            out.add(work);
        }
        return out;
    }

    /**
     * Wraps THF/TFF negated quantifiers so that "~ ? [..] : ..." becomes "~( ? [..] : ... )".
     * This matches the ANTLR parser's rule thf_unary_formula := '~' '(' thf_logic_formula ')'.
     */
    public static List<String> fixNegatedQuantifiers(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(fixNegatedQuantifiersInAnnotatedLine(line));
        }
        return out;
    }

    private static String fixNegatedQuantifiersInAnnotatedLine(String line) {
        // Handle only THF/TFF annotated lines that contain an inner (...) formula block.
        // We locate the formula slice: thf(name,role,( FORMULA ), ...).
        int funIdx = indexOfAny(line, "thf(", "tff(");
        if (funIdx < 0) return line;

        // Find first '(' that starts the outer functor args: thf( ... )
        int argsOpen = line.indexOf('(', funIdx);
        if (argsOpen < 0) return line;

        // We want the '(' that starts the inner FORMULA (the one after "...,(").
        // Strategy: find the index of ",(" that introduces the inner formula,
        // but be robust to extra commas in the name or role by scanning commas.
        int innerOpen = findInnerFormulaOpenParen(line, argsOpen);
        if (innerOpen < 0) return line;

        int innerClose = findMatchingParen(line, innerOpen);
        if (innerClose < 0) return line; // malformed; leave unchanged

        String before = line.substring(0, innerOpen + 1);
        String formula = line.substring(innerOpen + 1, innerClose);
        String after  = line.substring(innerClose);

        // If the formula starts with "~ ? [ ... ] :" or "~ ! [ ... ] :"
        // (allowing arbitrary whitespace), wrap the WHOLE quantified block in parentheses
        // so it becomes "~( ? [ ... ] : ... )".
        // We only trigger when this pattern occurs at the START (ignoring leading spaces).
        String fixed = wrapNegatedQuantifierHead(formula);

        if (fixed == null) {
            // nothing to change
            return line;
        } else {
            return before + fixed + after;
        }
    }

    /** Returns a fixed formula string or null if no change is needed. */
    private static String wrapNegatedQuantifierHead(String formula) {
        // Quick skip if there is no "~" followed by "?" or "!" near the head
        int start = skipSpaces(formula, 0);
        if (start >= formula.length() || formula.charAt(start) != '~') return null;

        int i = skipSpaces(formula, start + 1);
        if (i >= formula.length()) return null;
        char q = formula.charAt(i);
        if (q != '?' && q != '!') return null;

        // We have "~ ? ..." or "~ ! ...". Now we wrap everything from the quantifier
        // to the end of the formula in parentheses: "~(" + rest + ")"
        // BUT to preserve any trailing outer whitespace, just wrap the ENTIRE remaining formula.
        String rest = formula.substring(i);
        // Ensure we don't already have "~(" pattern
        int afterTilde = skipSpaces(formula, start + 1);
        if (afterTilde < formula.length() && formula.charAt(afterTilde) == '(') {
            return null; // already parenthesized "~("
        }
        return formula.substring(0, start) + "~(" + formula.substring(start + 1).trim() + ")";
    }

    /** Finds the '(' that opens the inner (FORMULA) of thf(...,( FORMULA ),...). */
    private static int findInnerFormulaOpenParen(String s, int argsOpen) {
        // We need the comma that immediately precedes the inner formula '('.
        // Walk the argument list of thf/tff: name , role , ( FORMULA ) , ...
        // We count commas at the top level (depth wrt parentheses).
        int depth = 0;
        int commasSeen = 0;
        for (int i = argsOpen + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                if (depth == 0) return -1; // finished thf( ... ) before finding inner '('
                depth--;
            } else if (c == ',' && depth == 0) {
                commasSeen++;
                // After the second top-level comma, we expect the inner formula to start with '('
                if (commasSeen == 2) {
                    // Skip spaces
                    int j = skipSpaces(s, i + 1);
                    if (j < s.length() && s.charAt(j) == '(') return j;
                    // Some outputs occasionally put extra tokens; fall back: search the next '('
                    int k = s.indexOf('(', j);
                    return k;
                }
            }
        }
        return -1;
    }

    /** Returns index of matching ')' for '(' at pos, with simple parentheses counting. */
    private static int findMatchingParen(String s, int pos) {
        if (pos < 0 || pos >= s.length() || s.charAt(pos) != '(') return -1;
        int depth = 0;
        for (int i = pos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1; // no match
    }

    private static int indexOfAny(String s, String... needles) {
        int best = -1;
        for (String n : needles) {
            int i = s.indexOf(n);
            if (i >= 0) best = (best == -1) ? i : Math.min(best, i);
        }
        return best;
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') i++;
            else break;
        }
        return i;
    }


    private static String normalizeTHFForFOFVisitor(String s){
        int funIdx=s.indexOf("thf("); if(funIdx<0) return s;
        int argsOpen=s.indexOf('(',funIdx);
        int innerOpen=findInnerFormulaOpenParen(s,argsOpen); if(innerOpen<0) return s;
        int innerClose=findMatchingParen(s,innerOpen);

        String before=s.substring(0,innerOpen+1);
        String formula=s.substring(innerOpen+1,innerClose);
        String after=s.substring(innerClose);

        // order matters: first fix "~atom", then push ~ through binders, then degroup/untype binders
        String f0 = wrapNegationOnBareAtoms(formula);
        String f1 = rewriteNegatedBinders(f0);
        String f2 = expandAndUntypeBinders(f1);
        return before + f2 + after;
    }

// ~! [..] : Φ  ->  ? [..] : ~(Φ)
// whitespace tolerant: "~ ! ["
    private static String rewriteNegatedBinders(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            int start = i;
            if (s.charAt(i) == '~') {
                int j = skipSpaces(s, i + 1);
                if (j < s.length()) {
                    char q = s.charAt(j);
                    if (q == '!' || q == '?') {
                        int k = skipSpaces(s, j + 1);
                        if (k < s.length() && s.charAt(k) == '[') {
                            int varsOpen = k;
                            int varsClose = findMatchingBracket(s, varsOpen, '[', ']');
                            if (varsClose > varsOpen) {
                                int colon = s.indexOf(':', varsClose + 1);
                                if (colon > varsClose) {
                                    int matOpen = nextNonSpaceIs(s, colon + 1, '(');
                                    if (matOpen >= 0) {
                                        int matClose = findMatchingBracket(s, matOpen, '(', ')');
                                        if (matClose > matOpen) {
                                            // Build the dual quantifier with negated matrix
                                            String vars = s.substring(varsOpen + 1, varsClose);
                                            String matrix = s.substring(matOpen + 1, matClose);
                                            char dual = (q == '!') ? '?' : '!';
                                            out.append(dual).append(" [").append(vars).append("] : (~(")
                                                    .append(matrix).append("))");
                                            i = matClose + 1;
                                            continue;
                                        }
                                    }
                                }
                            }
                        } else if (k < s.length() && s.charAt(k) == '(') {
                            // Already "~(" ... leave to parser
                        }
                    }
                }
            }
            // default: copy one char and advance
            out.append(s.charAt(start));
            i = start + 1;
        }
        return out.toString();
    }

    // ![X,Y: w] : Φ  ->  ![X]:(![Y]:(Φ))   and drops ": w" types.
    // Also supports matrices NOT wrapped in parentheses after ':'.
    private static String expandAndUntypeBinders(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            if (startsWithQuant(s, i)) {
                char q = s.charAt(i);                      // '!' or '?'
                int open = s.indexOf('[', i);
                int close = findMatchingBracket(s, open, '[', ']');
                int colon = (open > 0) ? s.indexOf(':', close) : -1;

                if (open > 0 && close > open && colon > close) {
                    // split and strip types
                    String rawVars = s.substring(open + 1, close);
                    String[] vars = rawVars.split(",");
                    List<String> vlist = new ArrayList<>();
                    for (String v : vars) {
                        String vv = v.trim().replaceAll("\\s*:\\s*[A-Za-z0-9_\\$]+\\s*$", "");
                        if (!vv.isEmpty()) vlist.add(vv);
                    }
                    // open nested binders
                    for (String v : vlist) out.append(q).append(" [").append(v).append("] : (");

                    // locate matrix (parenthesized or not)
                    int j = nextNonSpaceIndex(s, colon + 1);
                    String matrix;
                    int consumeTo; // index we will advance 'i' to after consuming matrix
                    if (j >= 0 && j < s.length() && s.charAt(j) == '(') {
                        int closeMat = findMatchingBracket(s, j, '(', ')');
                        matrix = s.substring(j + 1, closeMat);
                        consumeTo = closeMat + 1;
                    } else {
                        // no parens -> take the rest of the formula
                        matrix = (j >= 0) ? s.substring(j) : "";
                        consumeTo = s.length();
                    }

                    // recurse into matrix so inner grouped binders expand too
                    String matrixFixed = expandAndUntypeBinders(matrix);
                    out.append(matrixFixed);

                    // close nested binders
                    for (int k = 0; k < vlist.size(); k++) out.append(')');

                    i = consumeTo;           // *** consume what we transformed ***
                    continue;
                }
            }
            out.append(s.charAt(i++));
        }
        return out.toString();
    }

    // helper: like nextNonSpaceIs but returns the index
    private static int nextNonSpaceIndex(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    /* ==== small helpers ==== */
    private static boolean startsWithQuant(String s, int i) {
        return s.startsWith("![", i) || s.startsWith("! [", i)
                || s.startsWith("? [", i) || s.startsWith("?[", i);
    }

    private static boolean valid(int a,int b,int c,int d,int e){ return a>=0 && b>a && c>b && d>=0 && e>d; }
    private static int nextNonSpaceIs(String s, int from, char ch) {
        for (int i = from; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return (s.charAt(i) == ch) ? i : -1;
        }
        return -1;
    }

    private static int findMatchingBracket(String s, int openPos, char openCh, char closeCh) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == openCh) depth++;
            else if (c == closeCh && --depth == 0) return i;
        }
        return -1;
    }

    // e.g. "~spl15_15"  -> "~(spl15_15)"
    private static String wrapNegationOnBareAtoms(String s){
        StringBuilder out=new StringBuilder(s.length());
        for(int i=0;i<s.length();){
            if(s.charAt(i)=='~'){
                int j=i+1; while(j<s.length()&&Character.isWhitespace(s.charAt(j))) j++;
                // if already "~(" then ok
                if(j<s.length() && s.charAt(j)=='('){ out.append('~'); i=j; continue; }
                // capture an identifier that is not a quantifier/binder and not an app/call
                int k=j; if(k<s.length() && (Character.isLetter(s.charAt(k))||s.charAt(k)=='_')){
                    while(k<s.length() && (Character.isLetterOrDigit(s.charAt(k))||s.charAt(k)=='_')) k++;
                    // skip spaces
                    int t=k; while(t<s.length() && Character.isWhitespace(s.charAt(t))) t++;
                    if(t>=s.length() || (s.charAt(t)!='(' && s.charAt(t)!='@' && s.charAt(t)!='[')){
                        out.append("~(").append(s, j, k).append(")");
                        i=k; continue;
                    }
                    // ~f(x) — negation of function application: wrap from identifier through closing paren
                    if(s.charAt(t)=='('){
                        int closeIdx=findMatchingBracket(s,t,'(', ')');
                        if(closeIdx>t){
                            out.append("~(").append(s, j, closeIdx+1).append(")");
                            i=closeIdx+1; continue;
                        }
                    }
                }
            }
            out.append(s.charAt(i++));
        }
        return out.toString();
    }

    // -------------------------------------------------------------------------
    // Proof reordering helpers (moved from TPTP3ProofProcessor)
    // -------------------------------------------------------------------------

    private static final boolean debug = false;

    /**
     * Join multi-line thf()/tff()/fof()/cnf() blocks, prune to the subgraph
     * reachable from $false, and topologically sort the proof steps.
     * Input and output are the lines BETWEEN the SZS start/end markers.
     */
    private static List<String> reorderProofSection(List<String> lines) {

        final Set<String> DIALECTS = new LinkedHashSet<>(Arrays.asList("fof(", "tff(", "thf(", "cnf("));
        final Pattern HEAD_ID =
                Pattern.compile("^\\s*(fof|tff|thf|cnf)\\(([^,\\s]+)\\s*,", Pattern.CASE_INSENSITIVE);
        final Pattern PARENTS =
                Pattern.compile("(?:inference|introduced)\\([^\\]]*\\[(.*?)\\]\\)", Pattern.CASE_INSENSITIVE);
        final Pattern FORMULA_ID = Pattern.compile("\\bf\\d+\\b", Pattern.CASE_INSENSITIVE);

        List<String> prefix = new ArrayList<>();
        List<String> suffix = new ArrayList<>();
        List<ProofItem> items = new ArrayList<>();

        boolean seenBlock = false;
        boolean collecting = false;
        List<String> cur = new ArrayList<>();
        int appearanceIdx = 0;

        for (String ln : lines) {
            String trimmed = ln.trim();
            boolean startsBlock = DIALECTS.stream().anyMatch(d -> trimmed.startsWith(d));
            if (!collecting && !seenBlock && !startsBlock) { prefix.add(ln); continue; }
            if (startsBlock && !collecting) { collecting = true; seenBlock = true; cur.clear(); }
            if (collecting) {
                cur.add(ln);
                if (trimmed.endsWith(").")) {
                    String blockText = String.join("\n", cur);
                    Matcher m = HEAD_ID.matcher(cur.get(0));
                    String id = null;
                    if (m.find()) id = m.group(2);
                    if (id == null) {
                        suffix.addAll(cur);
                    } else {
                        Set<String> parents = new LinkedHashSet<>();
                        Matcher br = PARENTS.matcher(blockText);
                        while (br.find()) {
                            Matcher ids = FORMULA_ID.matcher(br.group(1));
                            while (ids.find()) { String pid = ids.group(); if (!pid.equalsIgnoreCase(id)) parents.add(pid); }
                        }
                        items.add(new ProofItem(id, blockText, parents, appearanceIdx++, blockText.contains("$false")));
                    }
                    collecting = false; cur.clear();
                }
                continue;
            }
            suffix.add(ln);
        }

        if (items.isEmpty()) return new ArrayList<>(lines);

        ProofItem falseItem = null;
        for (ProofItem it : items) if (it.containsFalse) falseItem = it;
        if (falseItem == null) return new ArrayList<>(lines);

        Map<String, ProofItem> byId = new HashMap<>();
        for (ProofItem it : items) byId.put(it.id, it);

        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(falseItem.id);
        while (!stack.isEmpty()) {
            String id = stack.pop();
            if (!reachable.add(id)) continue;
            for (String p : byId.getOrDefault(id, ProofItem.EMPTY).parents)
                if (byId.containsKey(p)) stack.push(p);
        }

        Map<String, Integer> indeg = new HashMap<>();
        Map<String, Set<String>> children = new HashMap<>();
        for (String id : reachable) { indeg.put(id, 0); children.put(id, new LinkedHashSet<>()); }
        for (String id : reachable) {
            ProofItem it = byId.get(id);
            if (it == null) continue;
            for (String p : it.parents) {
                if (!reachable.contains(p)) continue;
                children.get(p).add(id);
                indeg.put(id, indeg.get(id) + 1);
            }
        }

        Comparator<String> idCmp = (a, b) -> {
            long na = numericTail(a), nb = numericTail(b);
            if (na != -1 && nb != -1) return Long.compare(na, nb);
            return Integer.compare(byId.get(a).appearance, byId.get(b).appearance);
        };
        PriorityQueue<String> q = new PriorityQueue<>(idCmp);
        for (Map.Entry<String,Integer> e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());

        List<String> topo = new ArrayList<>();
        while (!q.isEmpty()) {
            String u = q.poll();
            topo.add(u);
            for (String v : children.getOrDefault(u, Collections.emptySet())) {
                indeg.put(v, indeg.get(v) - 1);
                if (indeg.get(v) == 0) q.add(v);
            }
        }

        if (topo.size() != reachable.size()) {
            if (debug) System.out.println("reorderProofSection: topo/reachable mismatch, keeping original order");
            return new ArrayList<>(lines);
        }

        List<ProofItem> unreachable = new ArrayList<>();
        for (ProofItem it : items) if (!reachable.contains(it.id)) unreachable.add(it);
        unreachable.sort(Comparator.comparingInt(x -> x.appearance));

        List<String> out = new ArrayList<>(prefix);
        for (ProofItem it : unreachable) out.add(it.blockText);
        for (String id : topo) out.add(byId.get(id).blockText);
        out.addAll(suffix);
        return joinAndSplitStable(out);
    }

    private static final class ProofItem {
        static final ProofItem EMPTY = new ProofItem("", "", Collections.emptySet(), -1, false);
        final String id;
        final String blockText;
        final Set<String> parents;
        final int appearance;
        final boolean containsFalse;
        ProofItem(String id, String blockText, Set<String> parents, int appearance, boolean containsFalse) {
            this.id = id; this.blockText = blockText; this.parents = parents;
            this.appearance = appearance; this.containsFalse = containsFalse;
        }
    }

    private static long numericTail(String id) {
        int i = id.length() - 1;
        while (i >= 0 && Character.isDigit(id.charAt(i))) i--;
        if (i == id.length() - 1) return -1;
        try { return Long.parseLong(id.substring(i + 1)); } catch (Exception e) { return -1; }
    }

    private static List<String> joinAndSplitStable(List<String> chunks) {
        List<String> result = new ArrayList<>();
        for (String ch : chunks) { Collections.addAll(result, ch.split("\\R", -1)); }
        return result;
    }

}
