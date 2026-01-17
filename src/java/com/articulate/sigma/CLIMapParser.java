package com.articulate.sigma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ***************************************************************
 * A simple command-line argument parser that handles options
 * in any order and returns them as a map.
 */
public class CLIMapParser {

    /** ***************************************************************
     * Parses the command-line arguments into a Map.
     * Options should start with '-' or '--'.
     * Only double dash options can have arguments
     * Single dash options can be combined, as in -xvf
     *
     * @param args The array of command-line arguments.
     * @return A Map of options (keys) and their values.
     */
    public static Map<String, List<String>> parse(String[] args) {
        Map<String, List<String>> map = new HashMap<>();
        String lastKey = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("--")) {
                // Handle long options (e.g., --output)
                lastKey = arg.substring(2);
                map.putIfAbsent(lastKey, new ArrayList<>());
            }
            else if (arg.startsWith("-") && arg.length() > 1) {
                // Handle bundled short options (e.g., -abc)
                String bundle = arg.substring(1);
                for (int j = 0; j < bundle.length(); j++) {
                    lastKey = String.valueOf(bundle.charAt(j));
                    map.putIfAbsent(lastKey, new ArrayList<>());
                }
                // Only the LAST letter in a bundle typically receives the next argument
                // e.g., in -xvf file.tar, 'f' gets 'file.tar'
            }
            else {
                // Handle values or positional arguments
                if (lastKey != null) {
                    map.get(lastKey).add(arg);
                } else {
                    map.computeIfAbsent("_positional", k -> new ArrayList<>()).add(arg);
                }
            }
        }
        return map;
    }

    /** ***************************************************************
     * Helper method to print the contents of the map.
     */
    private static void printParsedMap(Map<String, List<String>> map) {
        map.forEach((cmd, values) ->
                System.out.println(cmd + " -> " + values));
    }

    /** ***************************************************************
     * Main method to demonstrate the parser's usage.
     */
    public static void main(String[] args) {
        // Example command line arguments:
        // -output results.txt --verbose -level 3 -config debug.cfg

        // Example 1: Passing arguments via the main method's args array (as if run from command line)
        String[] exampleArgs1 = {"--output", "results.txt", "--verbose", "--level", "3", "--config", "debug.cfg"};
        System.out.println("--- Example 1: Arguments in a specific order ---");
        Map<String, List<String>> parsedArgs1 = parse(exampleArgs1);
        printParsedMap(parsedArgs1);

        System.out.println("\n------------------------------------------------\n");

        // Example 2: Arguments in a different order to demonstrate flexibility
        String[] exampleArgs2 = {"--verbose", "--config", "debug.cfg", "--level", "3", "--output", "results.txt"};
        System.out.println("--- Example 2: Arguments in a different order ---");
        Map<String, List<String>> parsedArgs2 = parse(exampleArgs2);
        printParsedMap(parsedArgs2);

        System.out.println("\n------------------------------------------------\n");

        // Example 3: Mixed case and single dashes
        String[] exampleArgs3 = {"--user", "admin", "--logLevel", "INFO", "--flagOnly"};
        System.out.println("--- Example 3: User/Flag example ---");
        Map<String, List<String>> parsedArgs3 = parse(exampleArgs3);
        printParsedMap(parsedArgs3);

        System.out.println("\n------------------------------------------------\n");

        // Example 4: multiple single letter commands with parse()
        String[] exampleArgs4 = {"-avr", "--logLevel", "INFO", "--flagOnly", "--many", "foo", "bar"};
        System.out.println("--- Example 4: User/Flag example ---");
        Map<String, List<String>> parsedArgs4 = parse(exampleArgs4);
        printParsedMap(parsedArgs4);
    }
}

