package com.articulate.sigma;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TPTPFormatter {

    /**
     * Format TPTP input text using tptp4X pretty-printing.
     * @param inputText Raw TPTP input.
     * @param fileName  Optional name of the file (used for error messages only).
     * @return Formatted TPTP text, or null if formatting failed.
     */
    public String formatTptpText(String inputText, String fileName) {
        try {
            // 1️⃣ Write input to a temporary file with .tptp extension
            File tmp = writeTempFile(inputText, ".tptp");

            // 2️⃣ Run tptp4X in pretty format mode
            ProcessOutput po = runTptp4x(tmp, "-ftptp", "-uhuman");

            if (po.code == 0 && po.out != null && !po.out.isBlank()) {
                // ✅ Strip leading spaces on the first line only
                String[] lines = po.out.split("\\R", -1); // split on any line break, keep trailing empty lines
                if (lines.length > 0) {
                    lines[0] = lines[0].replaceFirst("^\\s+", ""); // remove leading whitespace from first line
                }
                return String.join(System.lineSeparator(), lines);
            } else {
                System.err.println("[TPTPFormatter] Formatting failed for: " + fileName);
                System.err.println(po.err != null && !po.err.isBlank() ? po.err : po.out);
                return null;
            }

        } catch (Throwable t) {
            System.err.println("[TPTPFormatter] Exception formatting " + fileName + ": " + t.getMessage());
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Utility: Write text to a temporary file.
     */
    private File writeTempFile(String text, String extension) throws IOException {
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
    private ProcessOutput runTptp4x(File inputFile, String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 2];
        cmd[0] = "tptp4X";
        System.arraycopy(args, 0, cmd, 1, args.length);
        cmd[cmd.length - 1] = inputFile.getAbsolutePath();  // ✅ append file path as positional argument

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

    // ===============================
    // ✅ CLI Entry Point
    // ===============================
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
            // Concatenate all remaining args as the formula
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }

            String inputFormula = sb.toString().trim();
            TPTPFormatter formatter = new TPTPFormatter();
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

    /**
     * ✅ Displays help/usage instructions for the CLI.
     */
    private static void showHelp() {
        System.out.println("Usage: java com.articulate.sigma.TPTPFormatter [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f \"<tptp_formula>\"   Format the provided TPTP formula and print the result");
        System.out.println("  -h, --help            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java com.articulate.sigma.TPTPFormatter -f \"fof(ax1, axiom, (p => q)).\"");
        System.out.println();
        System.out.println("Note: Requires 'tptp4X' to be installed and available on your PATH.");
    }
}
