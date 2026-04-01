package com.articulate.sigma.security;

import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.CLIMapParser;

import java.util.List;
import java.util.Map;

/**
 * This is a class includes input validation/sanitization functions
 * useful in many different contexts throughout the application.
 */
public final class ValidationUtils {

    /** ***************************************************************
     * Test method for this class.
     *
     * Examples:
     *   java com.articulate.sigma.security.ValidationUtils -i 123
     *   java com.articulate.sigma.security.ValidationUtils -s "<b>Hello</b>"
     *   java com.articulate.sigma.security.ValidationUtils -t "Dog-1!"
     *   java com.articulate.sigma.security.ValidationUtils -k "MyFile.kif"
     *   java com.articulate.sigma.security.ValidationUtils -K "bad file name.txt"
     */
    public static void main(String[] args) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            showHelp();
            return;
        }
        if (argMap.containsKey("i")) {
            String input = getFirstArg(argMap, "i");
            System.out.println("sanitizeInteger(\"" + input + "\") = " + sanitizeInteger(input));
        }
        if (argMap.containsKey("s")) {
            String input = getFirstArg(argMap, "s");
            System.out.println("sanitizeString(\"" + input + "\") = \"" + sanitizeString(input) + "\"");
        }
        if (argMap.containsKey("t")) {
            String input = getFirstArg(argMap, "t");
            System.out.println("sanitizeSumoTerm(\"" + input + "\") = \"" + sanitizeSumoTerm(input) + "\"");
        }
        if (argMap.containsKey("k")) {
            String input = getFirstArg(argMap, "k");
            System.out.println("isValidKifFileName(\"" + input + "\") = " + isValidKifFileName(input));
        }
        if (argMap.containsKey("K")) {
            String input = getFirstArg(argMap, "K");
            System.out.println("santizeKifFileName(\"" + input + "\") = \"" + santizeKifFileName(input) + "\"");
        }
    }

    /** ***************************************************************
     * Show command-line help.
     */
    public static void showHelp() {

        System.out.println("ValidationUtils class");
        System.out.println("Options:");
        System.out.println("  -h <none>          Show this help screen");
        System.out.println("  -i <intString>     Output the sanitized integer value");
        System.out.println("  -s <dirtyString>   Output the sanitized string (HTML removed)");
        System.out.println("  -t <sumoTerm>      Output the sanitized SUMO term");
        System.out.println("  -k <fileName>      Check whether the filename is a valid .kif filename");
        System.out.println("  -K <fileName>      Output the sanitized .kif filename");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  -i 123");
        System.out.println("  -s \"<b>Hello</b>\"");
        System.out.println("  -t \"Human-123!\"");
        System.out.println("  -k \"Merge.kif\"");
        System.out.println("  -K \"bad file name.txt\"");
    }

    /** ***************************************************************
     * Helper to safely get the first argument value for a flag.
     */
    private static String getFirstArg(Map<String, List<String>> argMap, String key) {

        List<String> values = argMap.get(key);
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    private ValidationUtils() {}

    /****************************************************************
     * Wrapper for the sanitizeInteger function that allows a default value.
     *
     * @param s A String
     * @return Validated integer value
     */
    public static int sanitizeInteger(String s, int defaultInt) {
        if (s == null || StringUtil.emptyString(s) || !StringUtil.isInteger(s)) {
            return defaultInt;
        } else return sanitizeInteger(s);
    }

    /****************************************************************
     * Returns the integer value of a string after validating that it is
     * an integer and removing any HTML.
     *
     * @param s A String
     * @return Validated integer value
     */
    public static int sanitizeInteger(String s) {

        if (StringUtil.emptyString(s))
            return 1;
        s = StringUtil.removeHTML(s);
        if (!StringUtil.isInteger(s))
            return 1;
        return Integer.parseInt(s);
    }

    /****************************************************************
     * Wrapper for the sanitizeInteger function that allows a default value.
     *
     * @param s A String
     * @return Validated integer value
     */
    public static String sanitizeIntegerString(String s, String defaultInt) {
       if (s == null || StringUtil.emptyString(s) || !StringUtil.isInteger(s)) {
            return StringUtil.removeHTML(defaultInt);
        } else return sanitizeIntegerString(s);
    }

    /****************************************************************
     * Returns the integer value of a string after validating that it is
     * an integer and removing any HTML.
     *
     * @param s A String
     * @return Validated integer value
     */
    public static String sanitizeIntegerString(String s) {

        if (StringUtil.emptyString(s) || !StringUtil.isInteger(s))
            return "";
        return StringUtil.removeHTML(s);
    }

    /****************************************************************
     * Wrapper for the sanitize string function that allows a default if the string is empty.
     *
     * @param s A String
     * @return Validated String
     */
    public static String sanitizeString(String s, String defaultString) {
        if (s == null || StringUtil.emptyString(s)) {
            return StringUtil.removeHTML(defaultString);
        }
        return StringUtil.removeHTML(s);
    }

    /****************************************************************
     * Returns validated String to ensure it doesn't contain any HTML or jsp tags.
     *
     * @param s A String
     * @return Validated String
     */
    public static String sanitizeString(String s) {
        if (StringUtil.emptyString(s)) {
            return "";
        }
        return StringUtil.removeHTML(s);
    }

    /****************************************************************
     * Returns validated SUMO term string.
     *
     * @param s A String
     * @return Validated SUMO term
     */
    public static String sanitizeSumoTerm(String s) {
        
        return StringUtil.replaceNonIdChars(sanitizeString(s));
    }

    /****************************************************************
     * Returns whether the given file name is a valid .kif filename.
     *
     * @param s A String
     * @return true if valid
     */
    public static boolean isValidKifFileName(String s) {

        if (StringUtil.emptyString(s)) {
            return false;
        }
        return s.matches("[a-zA-Z0-9._-]+") && s.endsWith(".kif");
    }

    /****************************************************************
     * Sanitizes a KIF file name and ensures the extension is .kif.
     *
     * @param s A String
     * @return Sanitized KIF filename
     */
    public static String santizeKifFileName(String s) {

        String cleaned = sanitizeString(s);
        if (StringUtil.emptyString(cleaned)) {
            return ".kif";
        }
        if (cleaned.endsWith(".kif")) {
            return cleaned;
        }
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex > 0) {
            cleaned = cleaned.substring(0, dotIndex);
        }
        return cleaned + ".kif";
    }
}