package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NLGReadability {


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

    private NLGReadability() {
        // utility class
    }


    public static String improveTemplate(String template, LanguageFormatter.RenderMode mode, String language) {

        if (template == null || template.isEmpty())
            return template;

        // Pre-linkification hook. If markup already exists, do nothing.
        if (template.indexOf('<') >= 0 || template.indexOf('>') >= 0)
            return template;

        final String andKw = NLGUtils.getKeyword(Formula.AND, language);
        final String orKw  = NLGUtils.getKeyword(Formula.OR, language);

        if ((andKw == null || andKw.isEmpty()) && (orKw == null || orKw.isEmpty()))
            return template;

        // Protect Sigma annotated terms (&%TERM$"label" + optional trailing digits like "x"1).
        Protection prot = protectAnnotatedTerms(template);
        String work = prot.protectedText;

        // Split leading quantifier header (Commit 4).
        QuantifierSplit qs = splitLeadingQuantifierHeader(work, andKw);
        String prefix = qs.prefix;
        String body   = qs.body;

        // render the disjunction as a list of smaller valid clauses.
        if (!prefix.isEmpty() && orKw != null && !orKw.isEmpty()) {
            String listed = renderQuantifiedFlatOrAsList(prefix, body, orKw, mode);
            if (listed != null) {
                // Restore protected annotated terms and return.
                return restoreAnnotatedTerms(listed, prot.placeholderToOriginal);
            }
        }

        if (andKw != null && !andKw.isEmpty())
            body = smoothSimpleConnectorChains(body, andKw);

        if (orKw != null && !orKw.isEmpty())
            body = smoothSimpleConnectorChains(body, orKw);

        if (andKw != null && !andKw.isEmpty())
            body = chunkLongConnectorChains(body, andKw, mode, language, /*isAnd*/true);

        if (orKw != null && !orKw.isEmpty())
            body = chunkLongConnectorChains(body, orKw, mode, language, /*isAnd*/false);

        work = prefix + body;

        // Restore protected annotated terms.
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

        if (containsAny(s, "~{", "}", "(", ")", "[", "]", "{", "}", "\n", "\r", ";"))
            return s;


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

        if (connectorKw == null || connectorKw.isEmpty())
            return s;

        String needle = " " + connectorKw + " ";
        if (s.indexOf(needle) < 0)
            return s;

        // Do not chunk negation blocks, scoped structures, or quantifier-leading templates in commit 3.
        if (containsAny(s, "~{", "}", "(", ")", "[", "]", "{", "}", "\n", "\r", ";"))
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


}
