/** This code is copyright Articulate Software (c) 2003.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.

 NLGReadability provides a dedicated, post-processing hook for improving the
 naturalness/readability of paraphrases produced by LanguageFormatter.
 */
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

    /**
     * Improve the readability of a paraphrase "template" (pre-linkification).
     *
     * IMPORTANT:
     * - Meaning-preserving only.
     * - Runs before NLGUtils.resolveFormatSpecifiers(), i.e., before <a href=...> is injected.
     */
    public static String improveTemplate(String template, LanguageFormatter.RenderMode mode, String language) {

        if (template == null || template.isEmpty())
            return template;

        // Commit 2 is intentionally conservative. Skip if it already contains HTML tags.
        if (template.indexOf('<') >= 0 || template.indexOf('>') >= 0)
            return template;

        final String andKw = NLGUtils.getKeyword(Formula.AND, language);
        final String orKw  = NLGUtils.getKeyword(Formula.OR, language);

        if ((andKw == null || andKw.isEmpty()) && (orKw == null || orKw.isEmpty()))
            return template;

        // Protect Sigma annotated terms (&%TERM$"label" + optional trailing digits like "x"1) for further processing.
        Protection prot = protectAnnotatedTerms(template);

        String work = prot.protectedText;

        // Run conservative smoothing.
        if (andKw != null && !andKw.isEmpty())
            work = smoothSimpleConnectorChains(work, andKw);

        if (orKw != null && !orKw.isEmpty())
            work = smoothSimpleConnectorChains(work, orKw);

        // Restore protected annotated terms.
        return restoreAnnotatedTerms(work, prot.placeholderToOriginal);
    }

    /**
     * Protect occurrences of &%TERM$"label" (optionally followed by digits) from rewriting.
     *
     * Examples protected as a single unit:
     *   &%Animal$"animal"
     *   &%Organism$"an organism"1
     *   &%Jane7_1$"Jane7_1"
     */
    private static Protection protectAnnotatedTerms(String s) {

        // &%  ...  $"  ...  "  [0-9]*
        // Non-greedy in the middle to avoid spanning across tokens.
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

    /**
     * Convert simple connector chains into comma lists when safe.
     * Only rewrites chains of 3..6 items.
     */
    private static String smoothSimpleConnectorChains(String s, String connectorKw) {

        if (connectorKw == null || connectorKw.isEmpty())
            return s;

        // Quick reject if connector keyword doesn't appear bounded by spaces.
        String needle = " " + connectorKw + " ";
        if (s.indexOf(needle) < 0)
            return s;

        // Conservative: avoid rewriting in the presence of constructs we want to handle later
        // (negation blocks, quantifiers, explicit scoping punctuation).
        if (containsAny(s, "~{", "}", "(", ")", "[", "]", "{", "}", "\n", "\r", ";"))
            return s;

        // Also skip quantifier-leading templates. (You can expand this later with better detection.)
        String trimmed = s.trim().toLowerCase();
        if (trimmed.startsWith("for all ") || trimmed.startsWith("there exists "))
            return s;

        // Split on connector keyword with whitespace boundaries.
        // Example: "A and B and C" -> ["A", "B", "C"]
        String kwRegex = "\\s+" + Pattern.quote(connectorKw) + "\\s+";
        String[] parts = s.split(kwRegex);

        if (parts.length < 3 || parts.length > 6)
            return s;

        List<String> items = new ArrayList<>(parts.length);
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty())
                return s;

            // If an item contains another connector keyword bounded by spaces, treat as nested and skip.
            String bounded = " " + item + " ";
            if (bounded.contains(needle))
                return s;

            // Very long items tend to be complex; keep as-is for now.
            if (item.length() > 180)
                return s;

            items.add(item);
        }

        // Build comma list with Oxford comma.
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
