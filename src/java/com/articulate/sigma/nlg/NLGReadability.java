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

        // (We will generate <ul>...</ul> ourselves when mode==HTML, but we do not want to rewrite
        // already-marked-up strings repeatedly.)
        if (template.indexOf('<') >= 0 || template.indexOf('>') >= 0)
            return template;

        final String andKw = NLGUtils.getKeyword(Formula.AND, language);
        final String orKw  = NLGUtils.getKeyword(Formula.OR, language);

        if ((andKw == null || andKw.isEmpty()) && (orKw == null || orKw.isEmpty()))
            return template;

        // Protect Sigma annotated terms (&%TERM$"label" + optional trailing digits like "x"1).
        Protection prot = protectAnnotatedTerms(template);
        String work = prot.protectedText;

        // Commit 2: conservative comma-list smoothing for 3..6 items.
        if (andKw != null && !andKw.isEmpty())
            work = smoothSimpleConnectorChains(work, andKw);

        if (orKw != null && !orKw.isEmpty())
            work = smoothSimpleConnectorChains(work, orKw);

        // Commit 3: chunk long flat chains (7+ items) into bullet/numbered lists.
        if (andKw != null && !andKw.isEmpty())
            work = chunkLongConnectorChains(work, andKw, mode, language, /*isAnd*/true);

        if (orKw != null && !orKw.isEmpty())
            work = chunkLongConnectorChains(work, orKw, mode, language, /*isAnd*/false);

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

        String trimmed = s.trim().toLowerCase();
        if (trimmed.startsWith("for all ") || trimmed.startsWith("there exists "))
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

        String trimmed = s.trim().toLowerCase();
        if (trimmed.startsWith("for all ") || trimmed.startsWith("there exists "))
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
}
