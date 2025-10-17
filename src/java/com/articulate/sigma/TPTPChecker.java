package com.articulate.sigma;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TPTPChecker {

    /**
     * Run syntax & warning checks on TPTP content using tptp4X,
     * returning a list of ErrRec diagnostics.
     *
     * @param contents TPTP text to check
     * @return list of ErrRec objects (errors & warnings)
     */
    public static List<ErrRec> check(String contents) {
        return check(contents, "(buffer)");
    }

    /**
     * Run syntax & warning checks on TPTP content using tptp4X,
     * returning a list of ErrRec diagnostics.
     *
     * @param contents TPTP text to check
     * @param fileName pseudo filename used in diagnostics
     * @return list of ErrRec objects
     */
    public static List<ErrRec> check(String contents, String fileName) {
        List<ErrRec> results = new ArrayList<>();
        if (contents == null || contents.isBlank())
            return results;
        try {
            File tmp = writeTempFile(contents, ".tptp");
            ProcessOutput po = runTptp4x(tmp, "-w", "-z", "-u", "machine");
            if (!po.err.isBlank())
                results.addAll(parseTptpOutput(fileName, po.err, 2));
            if (!po.out.isBlank())
                results.addAll(parseTptpOutput(fileName, po.out, 1));
        } catch (Throwable t) {
            results.add(new ErrRec(2, fileName, 0, 0, 1,"TPTP check failed: " + t.getMessage()));
        }
        return results;
    }

    /**
     * Parse TPTP4X output lines into ErrRec objects.
     * This is a simple parser; you can make it smarter later to extract line numbers.
     *
     * @param fileName pseudo filename
     * @param text raw stdout or stderr from tptp4X
     * @param severity 1 = warning, 2 = error
     */
    private static List<ErrRec> parseTptpOutput(String fileName, String text, int severity) {
        List<ErrRec> recs = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (line.isBlank()) continue;
            int lineNum = 0;
            int start = 0;
            int end = 1;
            String msg = line;
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("Line\\s+(\\d+)\\s+Char\\s+(\\d+)")
                    .matcher(line);
            if (m.find()) {
                try {
                    lineNum = Integer.parseInt(m.group(1)) - 1;
                    start = Integer.parseInt(m.group(2)) - 1;
                    end = start + 1;
                } catch (NumberFormatException ignored) { }
            }
            if (line.contains("SZS status") || line.contains("SyntaxError") || m.find())
                recs.add(new ErrRec(severity, fileName, lineNum, start, end, msg));
        }
        return recs;
    }

    /**
     * Format TPTP input text using tptp4X pretty-printing.
     * @param inputText Raw TPTP input.
     * @param fileName  Optional name of the file (used for error messages only).
     * @return Formatted TPTP text, or null if formatting failed.
     */
    public static String formatTptpText(String inputText, String fileName) {
        try {
            File tmp = writeTempFile(inputText, ".tptp");
            ProcessOutput po = runTptp4x(tmp, "-ftptp", "-uhuman");
            if (po.code == 0 && po.out != null && !po.out.isBlank()) {
                String[] lines = po.out.split("\\R", -1);
                if (lines.length > 0)
                    lines[0] = lines[0].replaceFirst("^\\s+", "");
                return String.join(System.lineSeparator(), lines);
            } else {
                System.err.println("[TPTPChecker] Formatting failed for: " + fileName);
                System.err.println(po.err != null && !po.err.isBlank() ? po.err : po.out);
                return inputText;
            }
        } catch (Throwable t) {
            System.err.println("[TPTPChecker] Exception formatting " + fileName + ": " + t.getMessage());
            t.printStackTrace();
            return inputText;
        }
    }

    /**
     * Utility: Write text to a temporary file.
     */
    private static File writeTempFile(String text, String extension) throws IOException {
        File tempFile = File.createTempFile("tptp_format_", extension);
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(text);
        }
        return tempFile;
    }

    /**
     * Utility: Run tptp4X and capture output.
     */
    private static ProcessOutput runTptp4x(File inputFile, String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 2];
        cmd[0] = "tptp4X";
        System.arraycopy(args, 0, cmd, 1, args.length);
        cmd[cmd.length - 1] = inputFile.getAbsolutePath(); 
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();
        String out = new String(process.getInputStream().readAllBytes());
        String err = new String(process.getErrorStream().readAllBytes());
        int code = process.waitFor();
        return new ProcessOutput(code, out, err);
    }

    /**
     * Simple holder for process output.
     */
    private static class ProcessOutput {
        final int code;
        final String out;
        final String err;
        ProcessOutput(int code, String out, String err) {
            this.code = code;
            this.out = out;
            this.err = err;
        }
    }

    private static void showHelp() {
        System.out.println("Usage: java com.articulate.sigma.TPTPChecker [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f \"<tptp_formula>\"   Format the provided TPTP formula and print the result");
        System.out.println("  -h, --help            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java com.articulate.sigma.TPTPChecker -f \"fof(ax1, axiom, (p => q)).\"");
        System.out.println();
        System.out.println("Note: Requires 'tptp4X' to be installed and available on your PATH.");
    }

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            showHelp();
            return;
        }
        if (args[0].equals("-f")) {
            if (args.length < 2) {
                System.err.println("Error: No TPTP formula provided after -f.");
                showHelp();
                System.exit(1);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++)
                sb.append(args[i]).append(" ");
            String inputFormula = sb.toString().trim();
            TPTPChecker formatter = new TPTPChecker();
            String formatted = formatter.formatTptpText(inputFormula, "(command-line)");
            if (formatted != null) {
                System.out.println("=== Formatted TPTP ===");
                System.out.println(formatted);
            } else {
                System.err.println("Failed to format input.");
                System.exit(1);
            }
        } else {
            System.err.println("Unknown option: " + args[0]);
            showHelp();
            System.exit(1);
        }
    }
}
