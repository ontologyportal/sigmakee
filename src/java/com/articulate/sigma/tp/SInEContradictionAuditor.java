package com.articulate.sigma.tp;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.SInE;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Searches randomly rooted SInE neighborhoods for inconsistent SUMO axioms. */
public class SInEContradictionAuditor {


    public record Options(int attempts, double sampleFraction, int scope, int timeout, int minAxioms, int maxAxioms, long seed) {

        public Options {
            if (attempts < 1 || sampleFraction <= 0 || sampleFraction > 1 || scope < 0 || timeout < 1 || minAxioms < 1 || maxAxioms < minAxioms) throw new IllegalArgumentException("Invalid contradiction audit options");
        }

        public static Options defaults() {

            return new Options(1000, 0.001, 2, 10, 2, 1000, System.nanoTime());
        }
    }

    public record Attempt(int number, Path problem, ATPResult result, int axiomCount) {

        public boolean foundContradiction() {

            if (result == null) return false;
            SZSStatus status = result.getSzsStatus();
            return status == SZSStatus.THEOREM || status == SZSStatus.UNSATISFIABLE || status == SZSStatus.CONTRADICTORY_AXIOMS;
        }
    }

    public record Result(long seed, Path runDirectory, List<Attempt> attempts, Attempt contradiction, List<Formula> sourceAxioms) {

        public boolean foundContradiction() {

            return contradiction != null;
        }
    }

    /** Runs a randomized, source-aware contradiction audit. */
    public Result audit(KB kb, Options options) throws IOException, InterruptedException {

        if (kb == null) throw new IllegalArgumentException("Knowledge base cannot be null");
        if (!Vampire.isAvailable()) throw new IllegalStateException("Vampire executable is unavailable");
        List<String> formulas = new ArrayList<>(kb.getFormulas());
        formulas.removeIf(form -> {
            Formula formula = kb.formulaMap.get(form);
            return formula == null || formula.sourceFile == null || formula.isCached() || isNonReasoning(form);
        });
        if (formulas.isEmpty()) throw new IllegalStateException("Knowledge base has no source axioms to audit");
        SInE sine = new SInE(formulas);
        Random random = new Random(options.seed());
        Path runDirectory = Files.createTempDirectory("sine-contradiction-");
        List<Attempt> attempts = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int number = 1; number <= options.attempts(); number++) {
            Set<String> selected = new LinkedHashSet<>();
            Set<Integer> roots = new HashSet<>();
            int rootCount = Math.max(1, (int) Math.ceil(formulas.size() * options.sampleFraction()));
            while (roots.size() < rootCount) roots.add(random.nextInt(formulas.size()));
            Set<String> rootSymbols = new HashSet<>();
            for (int root : roots) {
                String formula = formulas.get(root);
                selected.add(formula);
                rootSymbols.addAll(sine.getSymbols(formula));
            }
            selected.addAll(select(sine, rootSymbols, options.scope()));
            if (selected.size() < options.minAxioms() || selected.size() > options.maxAxioms() || !seen.add(selected.hashCode())) continue;
            Path problem = runDirectory.resolve(String.format("sample-%06d.p", number));
            Map<String,Formula> sources = writeProblem(kb, selected, problem);
            if (sources.size() < options.minAxioms()) {
                Files.deleteIfExists(problem);
                continue;
            }
            System.out.println("SInEContradictionAuditor.audit(): running Vampire " + number + "/" + options.attempts() + " with " + sources.size() + " axioms");
            Vampire vampire = new Vampire(kb, "fof", "CASC", false, options.timeout(), 1);
            vampire.setAskQuestion(false);
            ATPResult atpResult = vampire.runProblem(problem);
            Attempt attempt = new Attempt(number, problem, atpResult, sources.size());
            attempts.add(attempt);
            System.out.println("SInEContradictionAuditor.audit(): sample " + number + " -> " + (atpResult == null ? "NoResult" : atpResult.getSzsStatus()));
            if (attempt.foundContradiction()) {
                List<Formula> sourceAxioms = findSourceAxioms(atpResult, sources);
                retainContradiction(runDirectory, problem, atpResult, sourceAxioms, options.seed());
                return new Result(options.seed(), runDirectory, List.copyOf(attempts), attempt, sourceAxioms);
            }
            if (atpResult != null && atpResult.getSzsStatus() == SZSStatus.ERROR) retainFailure(runDirectory, problem, atpResult);
            else Files.deleteIfExists(problem);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Contradiction audit interrupted");
        }
        System.out.println("SInEContradictionAuditor.audit(): completed " + attempts.size() + " attempts without a contradiction; seed=" + options.seed() + "; artifacts=" + runDirectory);
        return new Result(options.seed(), runDirectory, List.copyOf(attempts), null, List.of());
    }

    /** Returns whether a source formula is metadata rather than a logical axiom. */
    private boolean isNonReasoning(String formula) {

        return formula.startsWith("(termFormat ") || formula.startsWith("(format ") || formula.startsWith("(documentation");
    }

    /** Selects a bounded SInE neighborhood from a combined set of source formulas. */
    private Set<String> select(SInE sine, Set<String> rootSymbols, int scope) {

        Set<String> selected = new LinkedHashSet<>();
        Set<String> symbols = new HashSet<>(rootSymbols);
        for (int level = 0; level < scope && !symbols.isEmpty(); level++) {
            Set<String> added = sine.get1RequiredFormulas(symbols);
            added.removeAll(selected);
            if (added.isEmpty()) break;
            selected.addAll(added);
            symbols = sine.getSymbols(added);
        }
        return selected;
    }

    /** Writes selected SUMO formulas as a conjecture-free FOF problem. */
    private Map<String,Formula> writeProblem(KB kb, Collection<String> selected, Path problem) throws IOException {

        Set<Formula> selectedFormulas = new LinkedHashSet<>();
        for (String text : selected) {
            Formula formula = kb.formulaMap.get(text);
            if (formula != null) selectedFormulas.add(formula);
        }
        Map<Formula,List<String>> translated = SUMOKBtoTPTPKB.retranslateFormulas(kb, selectedFormulas, "fof");
        Map<String,Formula> sources = new HashMap<>();
        List<String> lines = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Formula,List<String>> entry : translated.entrySet()) {
            for (String body : entry.getValue()) {
                String name = "audit_" + index++;
                lines.add("fof(" + name + ",axiom,(" + body + ")).");
                sources.put(name, entry.getKey());
            }
        }
        Files.write(problem, lines, StandardCharsets.UTF_8);
        return sources;
    }

    /** Finds original formulas named as input axioms in Vampire's proof. */
    private List<Formula> findSourceAxioms(ATPResult result, Map<String,Formula> sources) {

        if (result == null) return List.of();
        String output = String.join("\n", result.getStdout());
        Set<Formula> found = new LinkedHashSet<>();
        for (Map.Entry<String,Formula> entry : sources.entrySet()) if (output.matches("(?s).*\\b" + entry.getKey() + "\\b.*")) found.add(entry.getValue());
        return List.copyOf(found);
    }

    /** Retains the contradictory problem, proof, and source witness. */
    private void retainContradiction(Path runDirectory, Path problem, ATPResult result, List<Formula> sources, long seed) throws IOException {

        Path contradiction = runDirectory.resolve("contradiction");
        Files.createDirectories(contradiction);
        Files.move(problem, contradiction.resolve("problem.p"), StandardCopyOption.REPLACE_EXISTING);
        Files.write(contradiction.resolve("proof.txt"), result.getStdout(), StandardCharsets.UTF_8);
        List<String> witness = new ArrayList<>();
        witness.add("seed=" + seed);
        witness.add("created=" + Instant.now());
        for (Formula source : sources) witness.add(source.sourceFile + ":" + source.startLine + " " + source.getFormula());
        Files.write(contradiction.resolve("sources.txt"), witness, StandardCharsets.UTF_8);
    }

    /** Retains a problem Vampire could not process and its diagnostics. */
    private void retainFailure(Path runDirectory, Path problem, ATPResult result) throws IOException {

        Path failures = runDirectory.resolve("failures");
        Files.createDirectories(failures);
        Path retained = failures.resolve(problem.getFileName());
        Files.move(problem, retained, StandardCopyOption.REPLACE_EXISTING);
        List<String> diagnostics = new ArrayList<>(result.getStderr());
        diagnostics.addAll(result.getErrorLines());
        Files.write(failures.resolve(problem.getFileName() + ".log"), diagnostics, StandardCharsets.UTF_8);
        System.err.println("SInEContradictionAuditor.audit(): retained failed problem " + retained);
    }
}
