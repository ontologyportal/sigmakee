package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.articulate.sigma.nlg.LanguageFormatter;

public final class NLGReadability {

    private static String language;

    // Segment markers emitted by LanguageFormatter. Used only internally.
    // Markers are stripped before user-visible output.
    private static final String SEG_O = "[SEG]";
    private static final String SEG_C = "[/SEG]";

    // Future-proof
    private static final String IF_A_O = "[IF_A]";
    private static final String IF_A_C = "[/IF_A]";
    private static final String IF_C_O = "[IF_C]";
    private static final String IF_C_C = "[/IF_C]";

    private static final String AND_O = "[AND]";
    private static final String AND_C = "[/AND]";

    private static final String OR_O  = "[OR]";
    private static final String OR_C  = "[/OR]";

    private enum Kind { ATOM, IF, AND, OR }



    private static final class Protection {
        final String protectedText;
        final Map<String,String> placeholderToOriginal;
        Protection(String protectedText, Map<String,String> placeholderToOriginal) {
            this.protectedText = protectedText;
            this.placeholderToOriginal = placeholderToOriginal;
        }
    }


    private static final class QuantifierSplit {
        final String prefix;
        final String body;
        QuantifierSplit(String prefix, String body) {
            this.prefix = prefix;
            this.body = body;
        }
    }


    private static final class BlockSpan {
        final String text;
        final int nextIndex;
        BlockSpan(String text, int nextIndex) {
            this.text = text;
            this.nextIndex = nextIndex;
        }
    }


    private static final class RunParse {
        final String connectorKw;       // AND or OR keyword detected between segments
        final List<String> items;        // inner texts (without [SEG] markers)
        final int nextIndex;             // where to resume scanning after the run
        RunParse(String connectorKw, List<String> items, int nextIndex) {
            this.connectorKw = connectorKw;
            this.items = items;
            this.nextIndex = nextIndex;
        }
    }

    private static abstract class Node {
        final Kind kind;
        Node parent;                 // set during parsing
        Node(Kind k) { this.kind = k; }
        int depth() {
            int d = 0; Node p = parent;
            while (p != null) { d++; p = p.parent; }
            return d;
        }
    }

    private static final class AtomNode extends Node {
        String text;
        AtomNode(String t) { super(Kind.ATOM); this.text = t; }
    }

    private static final class IfNode extends Node {
        Node antecedent;
        Node consequent;
        IfNode(Node a, Node c) {
            super(Kind.IF);
            this.antecedent = a;
            this.consequent = c;
        }
    }

    private static abstract class NAryNode extends Node {
        final List<Node> children;
        NAryNode(Kind k, List<Node> kids) { super(k); this.children = kids; }
    }

    private static final class AndNode extends NAryNode {
        AndNode(List<Node> kids) { super(Kind.AND, kids); }
    }

    private static final class OrNode extends NAryNode {
        OrNode(List<Node> kids) { super(Kind.OR, kids); }
    }


    private NLGReadability() {
        // utility class
    }

    private static void setParent(Node child, Node parent) {
        if (child != null) child.parent = parent;
    }

    private static Node parseTree(String body) {
        Node root = parseNode(body);
        root.parent = null;
        return root;
    }


    private static String stripKnownMarkers(String s) {
        if (s == null) return null;
        return s.replace(SEG_O, "").replace(SEG_C, "")
                .replace(IF_A_O, "").replace(IF_A_C, "")
                .replace(IF_C_O, "").replace(IF_C_C, "")
                .replace(AND_O, "").replace(AND_C, "")
                .replace(OR_O, "").replace(OR_C, "");
    }


    public static String improveTemplate(String template, LanguageFormatter.RenderMode mode, String language) {

        NLGReadability.language = language;

        if (template == null || template.isEmpty())
            return template;

        // Avoid double-processing already-rendered HTML.
        if (template.indexOf('<') >= 0 || template.indexOf('>') >= 0)
            return template;

        final String andKw = NLGUtils.getKeyword(Formula.AND, language);
        final String orKw  = NLGUtils.getKeyword(Formula.OR, language);

        if ((andKw == null || andKw.isEmpty()) && (orKw == null || orKw.isEmpty()))
            return template;

        // Protect Sigma annotated terms so factoring doesn’t break them.
        Protection prot = protectAnnotatedTerms(template);
        String work = prot.protectedText;

        // Split leading quantifier header from body.
        QuantifierSplit qs = splitLeadingQuantifierHeader(work, andKw);
        String prefix = qs.prefix;
        String body   = qs.body;

        // Preserve your special-case quantified flat OR list rendering.
        if (!prefix.isEmpty() && orKw != null && !orKw.isEmpty()) {
            String listed = renderQuantifiedFlatOrAsList(prefix, body, orKw, mode);
            if (listed != null) {
                return restoreAnnotatedTerms(listed, prot.placeholderToOriginal);
            }
        }

        // NEW: marker-driven parse of IF/AND/OR/SEG into a Node tree.
        Node tree = parseTree(body);

        // Apply readability rewrites recursively (SEG factoring happens inside Atom nodes, not globally).
        tree = rewriteNode(tree, andKw, orKw, mode);

        // Render from the tree.
        String rendered = (mode == LanguageFormatter.RenderMode.HTML)
                ? renderHtml(tree, language, andKw, orKw)
                : renderText(tree, language, andKw, orKw);

        // Reassemble.
        work = prefix + rendered;

        // Strip markers (include AND/OR markers too).
        work = work.replace(SEG_O, "").replace(SEG_C, "")
                .replace(IF_A_O, "").replace(IF_A_C, "")
                .replace(IF_C_O, "").replace(IF_C_C, "")
                .replace(AND_O, "").replace(AND_C, "")
                .replace(OR_O, "").replace(OR_C, "");

        return restoreAnnotatedTerms(work, prot.placeholderToOriginal);
    }

    // =========================
    // Protection: annotated terms
    // =========================

    private static Protection protectAnnotatedTerms(String s) {

        // &%  ...  $"  ...  "  [0-9]*
        Pattern p = Pattern.compile("&%.*?\\$\".*?\"\\d*");
        Matcher m = p.matcher(s);

        StringBuilder out = new StringBuilder(s.length());

        // placeholder -> original
        Map<String,String> phToOrig = new LinkedHashMap<>();
        // original -> placeholder (dedupe)
        Map<String,String> origToPh = new LinkedHashMap<>();

        int last = 0;
        int idx = 0;

        while (m.find()) {
            out.append(s, last, m.start());
            String tok = m.group();

            String ph = origToPh.get(tok);
            if (ph == null) {
                ph = "§T" + (idx++) + "§";
                origToPh.put(tok, ph);
                phToOrig.put(ph, tok);
            }
            out.append(ph);
            last = m.end();
        }
        out.append(s.substring(last));

        return new Protection(out.toString(), phToOrig);
    }


    private static String restoreAnnotatedTerms(String s, Map<String,String> placeholderToOriginal) {

        if (placeholderToOriginal.isEmpty())
            return s;

        String out = s;
        for (Map.Entry<String,String> e : placeholderToOriginal.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }


    private static String smoothSimpleConnectorChains(String s, String connectorKw) {

        if (connectorKw == null || connectorKw.isEmpty())
            return s;

        String needle = " " + connectorKw + " ";
        if (s.indexOf(needle) < 0)
            return s;

        String check = stripKnownMarkers(s);

        if (containsAny(check, "~{", "}", "(", ")", "{", "}", "\n", "\r", ";"))
            return s;

        List<String> segItems = extractSegItems(s);
        if (segItems != null && segItems.size() >= 3) {
            // Factor based on true operands
            String factored = factorSharedPrefix(segItems, connectorKw);
            if (factored != null) return factored;

            // If no factoring, at least remove the markers and rejoin cleanly:
            return joinWithConnector(segItems, connectorKw);
        }


        String kwRegex = "\\s+" + Pattern.quote(connectorKw) + "\\s+";
        String[] parts = s.split(kwRegex);

        if (parts.length < 3 || parts.length > 6)
            return s;

        List<String> items = new ArrayList<>(parts.length);
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty())
                return s;

            String bounded = " " + item + " ";
            if (bounded.contains(needle))
                return s;

            if (item.length() > 180)
                return s;

            items.add(item);
        }

        // If we can safely factor a shared prefix, collapse the list into one concise clause.
        String factored = factorSharedPrefix(items, connectorKw);
        if (factored != null)
            return factored;

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                if (i == items.size() - 1) {
                    out.append(", ").append(connectorKw).append(" ");
                } else {
                    out.append(", ");
                }
            }
            out.append(items.get(i));
        }
        return out.toString();
    }


    /**
     * Chunk long flat connector chains into lists for readability.
     *
     * Trigger only for 7..25 items to avoid surprising rewrites and to avoid
     * interfering with the 3..6 comma-list smoothing from Commit 2.
     *
     * HTML mode:
     *   "All of the following hold:<ul><li>...</li>...</ul>"
     *
     * TEXT mode:
     *   "All of the following hold: (1) ... (2) ... (3) ..."
     */
    private static String chunkLongConnectorChains(String s,
                                                   String connectorKw,
                                                   LanguageFormatter.RenderMode mode,
                                                   String language,
                                                   boolean isAnd) {

        String check = stripKnownMarkers(s);

        if (connectorKw == null || connectorKw.isEmpty())
            return s;

        String needle = " " + connectorKw + " ";
        if (s.indexOf(needle) < 0)
            return s;

        // Do not chunk negation blocks, scoped structures, or quantifier-leading templates in commit 3.
        if (containsAny(check, "~{", "}", "(", ")", "{", "}", "\n", "\r", ";"))
            return s;

        String kwRegex = "\\s+" + Pattern.quote(connectorKw) + "\\s+";
        String[] parts = s.split(kwRegex);

        if (parts.length < 7 || parts.length > 25)
            return s;

        List<String> items = new ArrayList<>(parts.length);
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty())
                return s;

            // Disallow nested connector keyword within an item.
            String bounded = " " + item + " ";
            if (bounded.contains(needle))
                return s;

            // Keep chunking conservative: if an item is extremely long, treat as too complex.
            if (item.length() > 300)
                return s;

            items.add(item);
        }

        // collapse into a single item with a factored object list.
        String factored = factorSharedPrefix(items, connectorKw);
        if (factored != null) {
            items.clear();
            items.add(factored);
        }

        // Lead-in text. Keep it stable for golden tests.
        final String leadIn;
        if (isAnd) {
            leadIn = "All of the following hold:";
        } else {
            leadIn = "At least one of the following holds:";
        }

        if (mode == LanguageFormatter.RenderMode.HTML) {
            StringBuilder out = new StringBuilder(s.length() + 64);
            out.append(leadIn);
            out.append("<ul>");
            for (String item : items) {
                out.append("<li>").append(item).append("</li>");
            }
            out.append("</ul>");
            return out.toString();
        }

        // TEXT mode
        StringBuilder out = new StringBuilder(s.length() + 64);
        out.append(leadIn).append(" ");
        for (int i = 0; i < items.size(); i++) {
            out.append("(").append(i + 1).append(") ").append(items.get(i));
            if (i < items.size() - 1)
                out.append(" ");
        }
        return out.toString();
    }

    // =========================
    // Utilities
    // =========================

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) {
            if (n != null && !n.isEmpty() && s.indexOf(n) >= 0)
                return true;
        }
        return false;
    }


    /**
     * If s begins with a quantifier header ("for all"/"there exists"), split it into:
     * - prefix: the quantifier + declared variables + trailing space
     * - body: the remainder
     *
     * This is conservative and English-surface based. It is designed to work with
     * Sigma’s template patterns such as:
     *   "for all VAR and VAR BODY..."
     *
     * It relies on annotated vars being protected as placeholders (e.g., §T0§),
     * so we can safely identify the variable list as placeholders separated by "and".
     */
    private static QuantifierSplit splitLeadingQuantifierHeader(String s, String andKw) {

        if (s == null || s.isEmpty())
            return new QuantifierSplit("", s);

        String trimmed = s.trim();
        String lower = trimmed.toLowerCase();

        boolean isForAll = lower.startsWith("for all ");
        boolean isExists = lower.startsWith("there exists ");

        if (!isForAll && !isExists)
            return new QuantifierSplit("", s);

        // We want to preserve the quantifier header exactly as it appears (including original spacing
        // as much as possible). To keep this simple and stable, we operate on the trimmed view but
        // return a prefix with a single trailing space.
        String qPrefix = isForAll ? "for all " : "there exists ";

        // If we can't detect "and" keyword, do not split.
        if (andKw == null || andKw.isEmpty())
            return new QuantifierSplit("", s);

        // Tokenize the trimmed string and attempt to parse:
        // qPrefix + <var> (and <var>)* + <body>
        // where <var> is a protected placeholder like §T0§.
        String rest = trimmed.substring(qPrefix.length());
        String[] toks = rest.split("\\s+");

        int i = 0;

        // First var must be a placeholder.
        if (i >= toks.length || !isPlaceholderToken(toks[i]))
            return new QuantifierSplit("", s);

        i++; // consume first var

        // Consume (and <var>)* pattern.
        while (i + 1 < toks.length) {
            if (!toks[i].equals(andKw))
                break;
            if (!isPlaceholderToken(toks[i + 1]))
                break;
            i += 2;
        }

        // i is now the index of the first token of the body.
        // Reconstruct prefix and body using tokens to avoid fragile substring math.
        StringBuilder prefix = new StringBuilder();
        prefix.append(qPrefix);

        // Rebuild the declared vars portion from toks[0..i-1]
        for (int j = 0; j < i; j++) {
            if (j > 0) prefix.append(' ');
            prefix.append(toks[j]);
        }
        prefix.append(' ');

        StringBuilder body = new StringBuilder();
        for (int j = i; j < toks.length; j++) {
            if (j > i) body.append(' ');
            body.append(toks[j]);
        }

        return new QuantifierSplit(prefix.toString(), body.toString());
    }

    private static boolean isPlaceholderToken(String tok) {
        if (tok == null) return false;
        // Commit 2/3 placeholders are of the form §T<number>§
        return tok.matches("§T\\d+§");
    }

    /**
     * If (prefix + body) represents a quantified statement whose body is a flat OR chain,
     * render it as:
     *
     *   "<quantifier header>, at least one of the following holds:<ul>...</ul>"  (HTML)
     *   "<quantifier header>, at least one of the following holds: (1) ... (2) ..." (TEXT)
     *
     * Returns null if the pattern is not safely recognized.
     */
    private static String renderQuantifiedFlatOrAsList(String prefix,
                                                       String body,
                                                       String orKw,
                                                       LanguageFormatter.RenderMode mode) {

        if (prefix == null || prefix.isEmpty() || body == null || body.isEmpty())
            return null;

        // Extremely conservative: do not touch negation blocks or explicit scoping punctuation yet.
        if (containsAny(body, "~{", "}", "(", ")", "[", "]", "{", "}", "\n", "\r", ";"))
            return null;

        // Must contain OR keyword bounded by spaces.
        String needle = " " + orKw + " ";
        if (body.indexOf(needle) < 0)
            return null;

        // Split on OR with whitespace boundaries.
        String kwRegex = "\\s+" + Pattern.quote(orKw) + "\\s+";
        String[] parts = body.split(kwRegex);

        // Only trigger for 3+ disjuncts (this is the “small valid ones” benefit).
        if (parts.length < 3 || parts.length > 30)
            return null;

        List<String> items = new ArrayList<>(parts.length);
        for (String p : parts) {
            String item = p.trim();
            if (item.isEmpty())
                return null;

            // Avoid nested OR inside an item (indicates complexity we are not parsing yet).
            String bounded = " " + item + " ";
            if (bounded.contains(needle))
                return null;

            // Keep conservative: if any item is extremely long, treat as too complex.
            if (item.length() > 400)
                return null;

            items.add(item);
        }

        // Try subject/prefix factoring inside the OR list.
        String factored = factorSharedPrefix(items, orKw);
        if (factored != null) {
            items.clear();
            items.add(factored);
        }

        // Build a stable lead-in.
        String header = prefix.trim();
        String leadIn = header + ", at least one of the following holds:";

        if (mode == LanguageFormatter.RenderMode.HTML) {
            StringBuilder out = new StringBuilder(leadIn.length() + body.length() + 64);
            out.append(leadIn);
            out.append("<ul>");
            for (String item : items) {
                out.append("<li>").append(item).append("</li>");
            }
            out.append("</ul>");
            return out.toString();
        }

        // TEXT mode
        StringBuilder out = new StringBuilder(leadIn.length() + body.length() + 64);
        out.append(leadIn).append(" ");
        for (int i = 0; i < items.size(); i++) {
            out.append("(").append(i + 1).append(") ").append(items.get(i));
            if (i < items.size() - 1)
                out.append(" ");
        }
        return out.toString();
    }


    /**
     * Attempt to factor a shared prefix across a list of clauses, producing one concise clause.
     *
     * Example (AND):
     *   ["Jane is mother of Bill", "Jane is mother of Bob", "Jane is mother of Sue"]
     *    -> "Jane is mother of Bill, Bob, and Sue"
     *
     * Example (OR):
     *   ["X is parent of A", "X is parent of B", "X is parent of C"]
     *    -> "X is parent of A, B, or C"
     *
     * Returns null if not safely applicable.
     */
    private static String factorSharedPrefix(List<String> items, String connectorKw) {

        if (items == null || items.size() < 3 || items.size() > 30)
            return null;

        // Conservative: do not factor if any item includes scope punctuation or block negation.
        for (String it : items) {
            if (it == null) return null;
            if (containsAny(it, "~{", "}", "(", ")", "[", "]", "{", "}", "\n", "\r", ";"))
                return null;
        }

        // Compute longest common prefix across all items.
        String prefix = longestCommonPrefix(items);
        if (prefix == null || prefix.trim().length() < 12)
            return null;

        // Trim prefix to a "safe boundary" to avoid cutting mid-token.
        prefix = trimPrefixToSafeBoundary(prefix);
        if (prefix == null || prefix.trim().length() < 12)
            return null;

        // Extract suffixes and ensure they are simple (ideally single placeholders like §T12§).
        List<String> tails = new ArrayList<>(items.size());
        for (String it : items) {
            if (!it.startsWith(prefix))
                return null;

            String tail = it.substring(prefix.length()).trim();
            tail = tail.replaceAll("[\\.,:;]+$", "").trim();
            if (tail.isEmpty())
                return null;

            // Strictly allow a simple tail:
            // - a protected placeholder token (preferred): §T123§
            // - or a short tail without connectors/punctuation (fallback)
            if (!isSimpleTail(tail, connectorKw))
                return null;

            tails.add(tail);
        }

        // Build the aggregated clause: prefix + joined tails.
        String joinedTails = joinWithConnector(tails, connectorKw);
        if (joinedTails == null)
            return null;

        return prefix + joinedTails;
    }

    private static String longestCommonPrefix(List<String> items) {
        if (items.isEmpty()) return "";

        String p = items.get(0);
        for (int i = 1; i < items.size(); i++) {
            p = commonPrefix(p, items.get(i));
            if (p.isEmpty())
                return "";
        }
        return p;
    }

    private static String commonPrefix(String a, String b) {
        if (a == null || b == null) return "";
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return a.substring(0, i);
    }

    /**
     * Prefer trimming at " of " if present; otherwise trim to the last whitespace boundary.
     * Ensures the prefix ends with a space.
     */
    private static String trimPrefixToSafeBoundary(String prefix) {

        if (prefix == null) return null;

        // For Commit 6, we only factor patterns of the form "... of <ARG>".
        // This avoids cutting through placeholder tokens like "§T".
        int idxOf = prefix.lastIndexOf(" of ");
        if (idxOf < 0)
            return null;

        String p = prefix.substring(0, idxOf + " of ".length());
        return p.endsWith(" ") ? p : (p + " ");
    }


    private static boolean isSimpleTail(String tail, String connectorKw) {

        // Preferred: single protected placeholder token
        if (tail.matches("§T\\d+§"))
            return true;

        // Conservative fallback: allow short tails without obvious structure.
        if (tail.length() > 80)
            return false;

        // No connectors/punctuation that suggest complexity.
        String bounded = " " + tail + " ";
        if (connectorKw != null && !connectorKw.isEmpty()) {
            if (bounded.contains(" " + connectorKw + " "))
                return false;
        }
        if (containsAny(tail, "~{", "}", "(", ")", "[", "]", "{", "}", ";", ","))
            return false;

        // No multiword tails (usually indicates clause remainder, not a single argument).
        if (tail.indexOf(' ') >= 0)
            return false;

        return true;
    }


    private static String joinWithConnector(List<String> items, String connectorKw) {
        if (items == null || items.isEmpty())
            return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(" ").append(connectorKw).append(" ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }


    /**
     * Apply readability improvements only inside "safe" segments of text.
     *
     * Safe segment definition (conservative):
     * - not inside a negation block "~{ ... }"
     * - not inside balanced (), [], or {} blocks
     *
     * We scan left-to-right, copy blocked spans unchanged, and run the existing
     * readability pipeline on safe spans.
     */
    private static String rewriteBySegments(String s,
                                            String andKw,
                                            String orKw,
                                            LanguageFormatter.RenderMode mode,
                                            String language) {

        if (s == null || s.isEmpty())
            return s;

        StringBuilder out = new StringBuilder(s.length());
        int i = 0;

        while (i < s.length()) {

            // 1) Negation blocks: "~{ ... }" are treated as atomic, copied unchanged.
            if (s.startsWith("~{", i)) {
                BlockSpan b = extractNegationBlock(s, i);
                if (b == null) {
                    // If we cannot parse safely, fall back: stop rewriting and return original.
                    return s;
                }
                out.append(b.text);
                i = b.nextIndex;
                continue;
            }

            // 2) Parentheses/brackets/braces blocks: copy unchanged (we don't parse scoped logic yet).
            char ch = s.charAt(i);
            if (ch == '(') {
                BlockSpan b = extractBalanced(s, i, '(', ')');
                if (b == null) return s;
                out.append(b.text);
                i = b.nextIndex;
                continue;
            }
            if (ch == '[') {
                BlockSpan b = extractBalanced(s, i, '[', ']');
                if (b == null) return s;
                out.append(b.text);
                i = b.nextIndex;
                continue;
            }
            if (ch == '{') {
                // Exclude "~{" which is handled above.
                BlockSpan b = extractBalanced(s, i, '{', '}');
                if (b == null) return s;
                out.append(b.text);
                i = b.nextIndex;
                continue;
            }

            // 3) Otherwise, accumulate a safe run until the next blocked construct.
            int j = i;
            while (j < s.length()) {
                if (s.startsWith("~{", j))
                    break;
                char cj = s.charAt(j);
                if (cj == '(' || cj == '[' || cj == '{')
                    break;
                j++;
            }

            String safe = s.substring(i, j);

            // Apply the existing readability pipeline to this safe segment.
            safe = applyReadabilityToSafeSegment(safe, andKw, orKw, mode, language);

            out.append(safe);
            i = j;
        }

        return out.toString();
    }


    /**
     * Apply existing readability rules to a safe segment only.
     * This reuses your already-tested methods:
     * - smoothSimpleConnectorChains
     * - chunkLongConnectorChains
     *
     * We do NOT attempt quantified-list rendering here; that remains a whole-body rule.
     */
    private static String applyReadabilityToSafeSegment(String segment,
                                                        String andKw,
                                                        String orKw,
                                                        LanguageFormatter.RenderMode mode,
                                                        String language) {

        if (segment == null || segment.isEmpty())
            return segment;

        String work = segment;

        // Only attempt work if the keyword exists in this segment.
        if (andKw != null && !andKw.isEmpty() && work.contains(" " + andKw + " "))
            work = smoothSimpleConnectorChains(work, andKw);

        if (orKw != null && !orKw.isEmpty() && work.contains(" " + orKw + " "))
            work = smoothSimpleConnectorChains(work, orKw);

        // Chunking works best after smoothing.
        if (andKw != null && !andKw.isEmpty() && work.contains(" " + andKw + " "))
            work = chunkLongConnectorChains(work, andKw, mode, language, /*isAnd*/true);

        if (orKw != null && !orKw.isEmpty() && work.contains(" " + orKw + " "))
            work = chunkLongConnectorChains(work, orKw, mode, language, /*isAnd*/false);

        return work;
    }



    /**
     * Extract "~{ ... }" from s starting at index i, accounting for nested braces.
     * Returns the full block text and the next scan position.
     */
    private static BlockSpan extractNegationBlock(String s, int i) {

        if (s == null || i < 0 || i + 1 >= s.length())
            return null;

        if (!s.startsWith("~{", i))
            return null;

        int depth = 0;
        int j = i;

        // We treat "~{" as starting a brace depth, and "}" as closing.
        // Nested "{...}" inside the block increments/decrements depth as well.
        while (j < s.length()) {
            char c = s.charAt(j);

            // Detect "~{" opener
            if (j + 1 < s.length() && s.charAt(j) == '~' && s.charAt(j + 1) == '{') {
                depth++;
                j += 2;
                continue;
            }

            // Detect plain '{' inside (rare, but be safe)
            if (c == '{') {
                depth++;
                j++;
                continue;
            }

            // Detect '}'
            if (c == '}') {
                depth--;
                j++;
                if (depth <= 0) {
                    String block = s.substring(i, j);
                    return new BlockSpan(block, j);
                }
                continue;
            }

            j++;
        }

        return null;
    }

    /**
     * Extract a balanced (...) or [...] or {...} block starting at index i.
     * This is conservative and does not attempt to interpret contents.
     */
    private static BlockSpan extractBalanced(String s, int i, char open, char close) {

        if (s == null || i < 0 || i >= s.length())
            return null;

        if (s.charAt(i) != open)
            return null;

        int depth = 0;
        int j = i;

        while (j < s.length()) {
            char c = s.charAt(j);

            if (c == open) {
                depth++;
                j++;
                continue;
            }
            if (c == close) {
                depth--;
                j++;
                if (depth <= 0) {
                    String block = s.substring(i, j);
                    return new BlockSpan(block, j);
                }
                continue;
            }
            j++;
        }

        return null;
    }


    /**
     * Marker-aware rewriting:
     * - Walk the string and detect runs of the form:
     *     [SEG]A[/SEG] <kw> [SEG]B[/SEG] <kw> [SEG]C[/SEG] ...
     * - Apply Commit 6 factoring across the run (using the connector kw).
     * - Also apply existing per-segment readability rules inside each [SEG]...[/SEG].
     *
     * We preserve markers in the output (they will be stripped later).
     */
    private static String rewriteUsingSegMarkers(String s,
                                                 String andKw,
                                                 String orKw,
                                                 LanguageFormatter.RenderMode mode,
                                                 String language) {

        if (s == null || s.isEmpty() || !hasSegMarkers(s))
            return s;

        StringBuilder out = new StringBuilder(s.length());

        int i = 0;
        while (i < s.length()) {

            int segStart = s.indexOf(SEG_O, i);
            if (segStart < 0) {
                out.append(s.substring(i));
                break;
            }

            // Copy text before the next segment marker unchanged.
            out.append(s, i, segStart);

            // Try to parse a run: [SEG]X[/SEG] <kw> [SEG]Y[/SEG] <kw> [SEG]Z[/SEG] ...
            RunParse run = parseSegRun(s, segStart, andKw, orKw);
            if (run == null) {
                // If we can't parse a run, just copy a single segment safely and move on.
                BlockSpan one = extractSingleSegBlock(s, segStart);
                if (one == null) {
                    // Give up conservatively.
                    return s;
                }
                String inner = one.text.substring(SEG_O.length(), one.text.length() - SEG_C.length());
                inner = applyReadabilityToSafeSegment(inner, andKw, orKw, mode, language);
                out.append(SEG_O).append(inner).append(SEG_C);
                i = one.nextIndex;
                continue;
            }

            // Apply per-segment readability inside each segment first.
            List<String> items = new ArrayList<>(run.items.size());
            for (String inner : run.items) {
                items.add(applyReadabilityToSafeSegment(inner, andKw, orKw, mode, language));
            }

            // Now attempt Commit 6 factoring across the run.
            String connectorKw = run.connectorKw;
            String factored = factorSharedPrefix(items, connectorKw);

            if (factored != null) {
                // Replace whole run with a single segment containing the factored text.
                out.append(SEG_O).append(factored).append(SEG_C);
            } else {
                // No factoring: just rebuild the run with the original connector between segments.
                for (int k = 0; k < items.size(); k++) {
                    if (k > 0) {
                        out.append(" ").append(connectorKw).append(" ");
                    }
                    out.append(SEG_O).append(items.get(k)).append(SEG_C);
                }
            }

            i = run.nextIndex;
        }

        return out.toString();
    }


    /**
     * Parse a run starting at [SEG]...[/SEG] followed by repeated " <kw> [SEG]...[/SEG]".
     * Returns null if not a valid run of 3+ segments with a single repeated connector.
     */
    private static RunParse parseSegRun(String s, int start,
                                        String andKw, String orKw) {

        BlockSpan first = extractSingleSegBlock(s, start);
        if (first == null) return null;

        String firstInner = first.text.substring(SEG_O.length(), first.text.length() - SEG_C.length());

        int i = first.nextIndex;

        // Determine connector based on what follows the first segment.
        String kw = readConnectorFollowing(s, i, andKw, orKw);
        if (kw == null) return null;

        List<String> items = new ArrayList<>();
        items.add(firstInner);

        // Consume repeated: <kw> [SEG]...[/SEG]
        int count = 1;
        while (true) {
            // Expect whitespace + kw + whitespace
            int afterKw = consumeConnector(s, i, kw);
            if (afterKw < 0) break;

            BlockSpan next = extractSingleSegBlock(s, afterKw);
            if (next == null) break;

            String inner = next.text.substring(SEG_O.length(), next.text.length() - SEG_C.length());
            items.add(inner);
            count++;

            i = next.nextIndex;
        }

        // Only treat as a "run" if there are 3+ segments.
        if (count < 3)
            return null;

        return new RunParse(kw, items, i);
    }

    /** Extract exactly one [SEG]...[/SEG] block starting at start. */
    private static BlockSpan extractSingleSegBlock(String s, int start) {
        if (s == null || start < 0 || !s.startsWith(SEG_O, start))
            return null;

        int end = s.indexOf(SEG_C, start + SEG_O.length());
        if (end < 0)
            return null;

        int next = end + SEG_C.length();
        return new BlockSpan(s.substring(start, next), next);
    }

    /** Look ahead after index i to determine if the next connector is AND or OR. */
    private static String readConnectorFollowing(String s, int i, String andKw, String orKw) {

        // Skip spaces
        int j = i;
        while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;

        // Prefer AND/OR keywords with whitespace boundaries.
        if (andKw != null && !andKw.isEmpty()) {
            if (startsWithWord(s, j, andKw)) return andKw;
        }
        if (orKw != null && !orKw.isEmpty()) {
            if (startsWithWord(s, j, orKw)) return orKw;
        }

        return null;
    }

    /** Consume " <kw> " starting at i; return index after kw+whitespace, else -1. */
    private static int consumeConnector(String s, int i, String kw) {
        int j = i;
        while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;

        if (!startsWithWord(s, j, kw))
            return -1;

        j += kw.length();
        if (j >= s.length() || !Character.isWhitespace(s.charAt(j)))
            return -1;

        while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
        return j;
    }

    /** True if s starts with word kw at pos and the next char is whitespace or '[' or end. */
    private static boolean startsWithWord(String s, int pos, String kw) {
        if (s == null || kw == null) return false;
        if (pos < 0 || pos + kw.length() > s.length()) return false;
        if (!s.regionMatches(pos, kw, 0, kw.length())) return false;

        int after = pos + kw.length();
        if (after == s.length()) return true;
        char c = s.charAt(after);
        return Character.isWhitespace(c) || c == '[';
    }


    private static boolean hasSegMarkers(String s) {
        return s != null && s.contains(SEG_O) && s.contains(SEG_C);
    }


    private static List<String> extractSegItems(String s) {
        if (s == null) return null;

        // Do not treat SEG blocks as a single list if IF structure exists.
        if (s.contains(IF_A_O) || s.contains(IF_C_O) || s.contains(AND_O) || s.contains(OR_O))
            return null;

        if (s.indexOf(SEG_O) < 0) return null;

        List<String> items = new ArrayList<>();
        Matcher m = Pattern.compile("\\[SEG\\](.*?)\\[/SEG\\]", Pattern.DOTALL).matcher(s);
        while (m.find()) {
            String item = m.group(1).trim();
            if (!item.isEmpty()) items.add(item);
        }
        return items.isEmpty() ? null : items;
    }


    private static BlockSpan extractMarkedBlock(String s, int start, String open, String close) {
        if (s == null || start < 0 || !s.startsWith(open, start)) return null;

        int i = start;
        int depth = 0;
        while (i < s.length()) {
            if (s.startsWith(open, i)) { depth++; i += open.length(); continue; }
            if (s.startsWith(close, i)) {
                depth--;
                i += close.length();
                if (depth == 0) {
                    // full block from start..i
                    String full = s.substring(start, i);
                    return new BlockSpan(full, i);
                }
                continue;
            }
            i++;
        }
        return null;
    }

    private static String innerOf(String full, String open, String close) {
        return full.substring(open.length(), full.length() - close.length());
    }


    private static boolean isIgnorableIfPrefix(String before, String language) {
        if (before == null) return true;
        String b = before.trim();
        if (b.isEmpty()) return true;

        LanguageFormatter.Keywords k = new LanguageFormatter.Keywords(language);
        String ifKw = k.IF == null ? "if" : k.IF;

        // Allow: "if", "if,", "if:" (and whitespace around)
        b = b.replace(",", "").replace(":", "").trim();
        return b.equalsIgnoreCase(ifKw);
    }

    private static boolean isIgnorableTrailing(String after) {
        if (after == null) return true;
        String a = after.trim();
        if (a.isEmpty()) return true;

        // Allow only punctuation that sometimes leaks (be conservative)
        return a.equals(",") || a.equals(".") || a.equals(";");
    }

    private static Node rewriteNode(Node n,
                                    String andKw, String orKw,
                                    LanguageFormatter.RenderMode mode
                                    ) {
        if (n instanceof AtomNode) {
            String t = ((AtomNode) n).text;

            // Important: by this stage AND/OR are structured, so Atom should be "small".
            // Keep your conservative cleanup:
            t = hasSegMarkers(t) ? rewriteUsingSegMarkers(t, andKw, orKw, mode, language)
                    : rewriteBySegments(t, andKw, orKw, mode, language);

            t = applyReadabilityToSafeSegment(t, andKw, orKw, mode, language);
            return new AtomNode(t.trim());
        }

        if (n instanceof IfNode) {

            IfNode in = (IfNode) n;

            Node a2 = rewriteNode(in.antecedent, andKw, orKw, mode);
            Node c2 = rewriteNode(in.consequent,  andKw, orKw, mode);

            IfNode out =  new IfNode(a2,c2);
            setParent(a2, out);
            setParent(c2, out);

            return out;
        }

        if (n instanceof AndNode) {
            AndNode an = (AndNode) n;
            List<Node> ops = new ArrayList<>();
            for (Node c : an.children) {
                Node kid = rewriteNode(c, andKw, orKw, mode);
                ops.add(kid);
            }
            AndNode out = new AndNode(ops);
            for (Node kid: ops){
                setParent(kid,out);
            }
            return out;
        }

        if (n instanceof OrNode) {
            OrNode on = (OrNode) n;
            List<Node> ops = new ArrayList<>();
            for (Node c : on.children) ops.add(rewriteNode(c, andKw, orKw, mode));
            OrNode out = new OrNode(ops);
            for (Node kid: ops){
                setParent(kid,out);
            }
            return out;
        }

        return n;
    }



    private static String renderText(Node n, String language, String andKw, String orKw) {
        if (n instanceof AtomNode) return ((AtomNode) n).text;

        LanguageFormatter.Keywords k = new LanguageFormatter.Keywords(language);

        if (n instanceof IfNode) {
            IfNode in = (IfNode) n;
            String a = renderText(in.antecedent, language, andKw, orKw);
            String c = renderText(in.consequent, language, andKw, orKw);
            return k.IF + " " + a + k.COMMA + " " + k.THEN + " " + c;
        }
        if (n instanceof AndNode) {
            AndNode an = (AndNode) n;
            List<String> items = new ArrayList<>();
            for (Node op : an.children) items.add(renderText(op, language, andKw, orKw));
            return joinWithConnector(items, andKw);
        }
        if (n instanceof OrNode) {
            OrNode on = (OrNode) n;
            List<String> items = new ArrayList<>();
            for (Node op : on.children) items.add(renderText(op, language, andKw, orKw));
            return joinWithConnector(items, orKw);
        }
        return "";
    }

    private static String renderHtml(Node n, String language, String andKw, String orKw) {
        if (n instanceof AtomNode) return escapeHtmlMinimal(((AtomNode) n).text);

        LanguageFormatter.Keywords k = new LanguageFormatter.Keywords(language);

        if (n instanceof IfNode) {
            IfNode in = (IfNode) n;
            String a = renderHtml(in.antecedent, language, andKw, orKw);
            String c = renderHtml(in.consequent, language, andKw, orKw);
            return "<ul>"
                    + "<li>" + escapeHtmlMinimal(k.IF) + " " + a + "</li>"
                    + "<li>" + escapeHtmlMinimal(k.THEN) + " " + c + "</li>"
                    + "</ul>";
        }
        if (n instanceof AndNode) {
            AndNode an = (AndNode) n;
            List<String> items = new ArrayList<>();
            for (Node op : an.children) items.add(renderHtml(op, language, andKw, orKw));
            // Inline is OK for small lists; chunking policy can evolve later.
            return joinWithConnector(items, escapeHtmlMinimal(andKw));
        }
        if (n instanceof OrNode) {
            OrNode on = (OrNode) n;
            List<String> items = new ArrayList<>();
            for (Node op : on.children) items.add(renderHtml(op, language, andKw, orKw));
            return joinWithConnector(items, escapeHtmlMinimal(orKw));
        }
        return "";
    }


    private static String escapeHtmlMinimal(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static Node parseNode(String s) {
        if (s == null) return new AtomNode("");
        String t = s.trim();
        if (t.isEmpty()) return new AtomNode("");

        // IF (tolerant: allow "if" text before [IF_A], and ",then" between blocks)
        if (t.contains(IF_A_O) && t.contains(IF_C_O)) {
            Node n = parseIfNode(t);
            if (n != null) return n;
        }

        // AND / OR blocks
        Node andN = parseAndOrBlock(t, AND_O, AND_C, true);
        if (andN != null) return andN;

        Node orN  = parseAndOrBlock(t, OR_O, OR_C, false);
        if (orN != null) return orN;

        return new AtomNode(t);
    }


    private static Node parseAndOrBlock(String s,
                                        String open, String close,
                                        boolean isAnd) {
        String t = s.trim();
        if (!t.startsWith(open)) return null;

        BlockSpan b = extractMarkedBlock(t, 0, open, close);
        if (b == null) return null;

        // Must consume entire string (allow trailing whitespace only)
        if (!t.substring(b.nextIndex).trim().isEmpty()) return null;

        String inner = innerOf(b.text, open, close).trim();

        // Extract only top-level [SEG]...[/SEG] blocks using balanced extraction
        List<String> opTexts = extractTopLevelSegOperandTexts(inner);
        if (opTexts == null || opTexts.isEmpty()) return null;

        List<Node> kids = new ArrayList<>(opTexts.size());
        for (String op : opTexts) kids.add(parseNode(op));
        Node parent = isAnd ? new AndNode(kids) : new OrNode(kids);
        for (Node kid : kids) setParent(kid, parent);
        return parent;
    }

    private static List<String> extractTopLevelSegOperandTexts(String inner) {
        if (inner == null) return null;

        List<String> ops = new ArrayList<>();
        int i = 0;

        while (i < inner.length()) {
            int segStart = inner.indexOf(SEG_O, i);
            if (segStart < 0) break;

            BlockSpan seg = extractMarkedBlock(inner, segStart, SEG_O, SEG_C);
            if (seg == null) break; // malformed -> conservative stop

            String opText = innerOf(seg.text, SEG_O, SEG_C).trim();
            if (!opText.isEmpty()) ops.add(opText);

            i = seg.nextIndex;
        }

        return ops;
    }

    private static Node parseIfNode(String s) {
        int aPos = s.indexOf(IF_A_O);
        if (aPos < 0) return null;

        BlockSpan aBlock = extractMarkedBlock(s, aPos, IF_A_O, IF_A_C);
        if (aBlock == null) return null;

        int cStart = s.indexOf(IF_C_O, aBlock.nextIndex);
        if (cStart < 0) return null;

        BlockSpan cBlock = extractMarkedBlock(s, cStart, IF_C_O, IF_C_C);
        if (cBlock == null) return null;

        String before = s.substring(0, aPos).trim();
        String after  = s.substring(cBlock.nextIndex).trim();

        if (!isIgnorableIfPrefix(before, language)) return null;
        if (!isIgnorableTrailing(after)) return null;

        String aInner = innerOf(aBlock.text, IF_A_O, IF_A_C).trim();
        String cInner = innerOf(cBlock.text, IF_C_O, IF_C_C).trim();

        Node ifNode = new IfNode(null, null);
        Node antecedent = parseNode(aInner);
        Node consequent = parseNode(cInner);

        ((IfNode) ifNode).antecedent = antecedent;
        ((IfNode) ifNode).consequent = consequent;
        setParent(antecedent, ifNode);
        setParent(consequent, ifNode);

        return ifNode;
    }


}
