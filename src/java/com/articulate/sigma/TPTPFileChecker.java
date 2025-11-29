package com.articulate.sigma;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import tptp_parser.TPTPVisitor;
import tptp_parser.TPTPFormula;
import tptp_parser.TptpLexer;
import tptp_parser.TptpParser;
import java.util.regex.Matcher; 
import java.util.regex.Pattern;

public class TPTPFileChecker {

    static boolean debug = true;

    private static void showHelp() {
        System.out.println("Usage: java com.articulate.sigma.TPTPFileChecker [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f \"<tptp_formula>\"   Format the provided TPTP formula and print the result");
        System.out.println("  -h, --help            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java com.articulate.sigma.TPTPFileChecker -f \"fof(ax1, axiom, (p => q)).\"");
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
            TPTPFileChecker formatter = new TPTPFileChecker();
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

        if (debug) System.out.println("TPTPFileChecker.check(): Start");
        List<ErrRec> results = new ArrayList<>();
        if (contents == null || contents.isBlank())
            return results;
        if (debug) System.out.println("TPTPFileChecker.check(): Checking with ANTLR");
        //results.addAll(checkWithAntlr(contents, fileName));
        try {
            File tmp = writeTempFile(contents, ".tptp");
        if (debug) System.out.println("TPTPFileChecker.check(): Checking with TPTP4X");
            ProcessOutput po = runTptp4x(tmp, "-w", "-z", "-u", "machine");
            if (!po.err.isBlank())
                results.addAll(parseTptpOutput(fileName, po.err, 2));
            if (!po.out.isBlank())
                results.addAll(parseTptpOutput(fileName, po.out, 1));
        } catch (Throwable t) {
            results.add(new ErrRec(0, fileName, 0, 0, 1, "tptp4X check failed: " + t.getMessage()));
        }
        return results;
    }

    /**
     * Parse TPTP text using the ANTLR-based TPTPVisitor.
     * Returns ErrRecs for any syntax problems or empty results.
     */
    public static List<ErrRec> checkWithAntlr(String contents, String fileName) {

        List<ErrRec> errs = new ArrayList<>();
        try {
            TPTPVisitor visitor = new TPTPVisitor();
            Map<String, TPTPFormula> parsed = visitor.parseString(contents);
            if (parsed == null || parsed.isEmpty()) {
                errs.add(new ErrRec(0, fileName, 0, 0, 1,
                        "ANTLR parser found no valid formulas in input"));
            }
        } catch (Exception e) {
            errs.add(new ErrRec(0, fileName, 0, 0, 1,
                    "ANTLR parse error: " + e.getMessage()));
        }
        return errs;
    }

    /**
     * Thin wrapper that turns TPTP4X output into a List<ErrRec>
     * by delegating to parseTPTP4XOutputToErrRec.
     */
    private static List<ErrRec> parseTptpOutput(String fileName, String text, int severity) {

        if (text == null || text.isBlank())
            return Collections.emptyList();
        if (severity == 1) {
            String lower = text.toLowerCase(Locale.ROOT);
            boolean looksLikeError =
                    lower.contains("syntaxerror") ||
                    lower.contains("inputerror")  ||
                    lower.contains("error")       ||
                    lower.contains("warning");

            if (!looksLikeError) {
                return Collections.emptyList();
            }
        }
        ErrRec rec = parseTPTP4XOutputToErrRec(fileName, text, severity);
        if (rec != null)
            return Collections.singletonList(rec);
        return Collections.emptyList();
    }


    /**
     * Extract a single ErrRec from a chunk of TPTP4X output.
     * TPTP4X uses 1-based coordinates; ErrRec uses 0-based.
     *
     * Example TPTP4X line:
     *   % SZS status SyntaxError : Line 2 Char 3 Token "tf" ...
     *
     * @param fileName    logical file name for the editor
     * @param tptpOutput  raw TPTP4X stdout/stderr (possibly multi-line)
     * @param severity    1 = warning, 2 = error (currently unused but kept for API symmetry)
     * @return ErrRec or null if we couldn't parse anything useful
     */
    public static ErrRec parseTPTP4XOutputToErrRec(String fileName,
                                                   String tptpOutput,
                                                   int severity) {

        if (tptpOutput == null || tptpOutput.isBlank())
            return null;

        int lineNum0 = 0;
        int charStart0 = 0;
        int charEnd0   = 1;

        // Find "Line <n> Char <m>"
        Pattern p = Pattern.compile("Line\\s+(\\d+)\\s+Char\\s+(\\d+)");
        Matcher m = p.matcher(tptpOutput);

        String tptpLocStr = null;
        if (m.find()) {
            try {
                int line1 = Integer.parseInt(m.group(1)); // 1-based from TPTP4X
                int char1 = Integer.parseInt(m.group(2)); // 1-based from TPTP4X

                // Keep line 1-based to match what the editor prints,
                // but keep char 0-based so start+1 matches TPTP4X.
                lineNum0   = Math.max(line1, 0);        // <<-- changed: no "- 1"
                charStart0 = Math.max(char1 - 1, 0);
                charEnd0   = charStart0 + 1;

                tptpLocStr = "Line " + line1 + " Char " + char1;
            }
            catch (NumberFormatException ignored) {}
        }


        // Try to pick a "main" message line, preferring the SZS line
        String[] lines = tptpOutput.split("\\R");
        String msgCore = null;
        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            msgCore = trimmed;
            if (trimmed.contains("SZS status"))
                break;
        }
        if (msgCore == null)
            msgCore = tptpOutput.trim();

        StringBuilder msg = new StringBuilder(msgCore);
        if (tptpLocStr != null && !msgCore.contains(tptpLocStr)) {
            // Append the TPTP4X coordinate so the user can see both
            msg.append(" [TPTP4X: ").append(tptpLocStr).append("]");
        }

        return new ErrRec(
                0,          // code (unused in Sigma)
                fileName,
                lineNum0,
                charStart0,
                charEnd0,
                msg.toString()
        );
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
                System.err.println("[TPTPFileChecker] STDOUT:\n" + po.out);
                System.err.println("[TPTPFileChecker] STDERR:\n" + po.err);
                System.err.println("[TPTPFileChecker] Temp file: " + tmp.getAbsolutePath());
                System.err.println("[TPTPFileChecker] Formatting failed for: " + fileName);
                System.err.println(po.err != null && !po.err.isBlank() ? po.err : po.out);
                return inputText;
            }
        } catch (Throwable t) {
            System.err.println("[TPTPFileChecker] Exception formatting " + fileName + ": " + t.getMessage());
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

    private static List<ErrRec> parseWithAntlr(String contents, String fileName) {

        List<ErrRec> results = new ArrayList<>();
        try {
            CharStream input = CharStreams.fromString(contents);
            TptpLexer lexer = new TptpLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TptpParser parser = new TptpParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    results.add(new ErrRec( 0, fileName, line - 1, charPositionInLine, charPositionInLine + 1, "ANTLR syntax error: " + msg));
                }
            });
            parser.tptp_file();
        } catch (Throwable t) {
            results.add(new ErrRec( 0, fileName, 0, 0, 1, "ANTLR parse failed: " + t.getMessage()));
        }
        return results;
    }
}
