package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NLGReadability {

    private NLGReadability() {
        // utility class
    }

    public static String improveTemplate(String template, LanguageFormatter.RenderMode mode, String language) {

        if (template == null || template.isEmpty())
            return template;

        // This hook runs pre-linkification. If markup already exists, do nothing.
        if (template.indexOf('<') >= 0 || template.indexOf('>') >= 0)
            return template;

        final String andKw = NLGUtils.getKeyword(Formula.AND, language);
        final String orKw  = NLGUtils.getKeyword(Formula.OR, language);

        if ((andKw == null || andKw.isEmpty()) && (orKw == null || orKw.isEmpty()))
            return template;

        // Protect Sigma annotated terms (&%TERM$"label" + optional trailing digits like "x"1).
        Protection prot = protectAnnotatedTerms(template);
        String work = prot.protectedText;

        // Commit 4: If the paraphrase starts with a quantifier header ("for all"/"there exists"),
        // preserve the header (variable declarations) and apply readability only to the body.
        QuantifierSplit qs = splitLeadingQuantifierHeader(work, andKw);
        String prefix = qs.prefix;
        String body   = qs.body;

        // Apply Commit 2/3 improvements to the body only.
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
        Map<String,String> map = new LinkedHashMap<>();

        int last = 0;
        int idx = 0;

        while (m.find()) {
            out.append(s, last, m.start());
            String tok = m.group();

            String ph = "§T" + (idx++) + "§";
            map.put(ph, tok);
            out.append(ph);

            last = m.end();
        }
        out.append(s.substring(last));

        return new Protection(out.toString(), map);
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

    private static final class Protection {
        final String protectedText;
        final Map<String,String> placeholderToOriginal;
        Protection(String protectedText, Map<String,String> placeholderToOriginal) {
            this.protectedText = protectedText;
            this.placeholderToOriginal = placeholderToOriginal;
        }
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

    private static final class QuantifierSplit {
        final String prefix;
        final String body;
        QuantifierSplit(String prefix, String body) {
            this.prefix = prefix;
            this.body = body;
        }
    }

}
