package com.articulate.sigma.tp;

import com.articulate.sigma.KB;
import com.articulate.sigma.tp.TheoremProverController.EAxFilterVampireResult;
import com.articulate.sigma.tp.TheoremProverController.FilteredVampireAttempt;
import com.articulate.sigma.tp.e.EAxFilter;
import com.articulate.sigma.tp.e.EAxFilter.ContradictionProbe;
import com.articulate.sigma.tp.e.EAxFilter.ProbeResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs conjecture-driven E axiom filtering followed by Vampire. */
public class EAxFilterContradictionRunner {

    private static final Pattern AXIOM = Pattern.compile("\\s*(?:fof|cnf)\\(\\s*([^,\\s]+)\\s*,\\s*axiom\\s*,.*");

    /**
     * Selects an axiom, negates it as the conjecture, filters the ontology, and runs Vampire.
     * @param kb knowledge base associated with the problem
     * @param inputProblem complete conjecture-free FOF ontology problem
     * @param filterTimeout e_axfilter timeout in seconds
     * @param vampireTimeout Vampire timeout per filtered problem in seconds
     * @param random random source used to select the source axiom
     * @return filter and Vampire results
     */
    public EAxFilterVampireResult run(KB kb, Path inputProblem, int filterTimeout, int vampireTimeout, Random random) throws IOException, InterruptedException {

        if (!EAxFilter.isAvailable()) throw new IllegalStateException("e_axfilter executable is unavailable");
        if (!Vampire.isAvailable()) throw new IllegalStateException("Vampire executable is unavailable");
        Path runDirectory = Files.createTempDirectory("eaxfilter-contradiction-");
        ProbeResult probeResult = new EAxFilter().generateContradictionProbe(inputProblem, runDirectory, filterTimeout, random);
        EAxFilter.EAxFilterResult filterResult = probeResult.filterResult();
        if (!filterResult.succeeded()) throw new IOException("e_axfilter failed for negated axiom " + probeResult.probe().axiomName());
        Map<String,String> sourceAxioms = readSourceAxioms(inputProblem);
        System.out.println("EAxFilterContradictionRunner.run(): selected " + probeResult.probe().axiomName());
        List<FilteredVampireAttempt> attempts = new ArrayList<>();
        FilteredVampireAttempt successfulAttempt = null;
        for (Path problem : filterResult.generatedProblems()) {
            if (!rebuildFilteredProblem(problem, probeResult.probe(), sourceAxioms)) {
                System.out.println("EAxFilterContradictionRunner.run(): skipping empty filtered problem " + problem.getFileName());
                continue;
            }
            Vampire vampire = new Vampire(kb, "fof", "VAMPIRE", false, vampireTimeout, 1);
            vampire.setAskQuestion(false);
            ATPResult result = vampire.runProblem(problem);
            FilteredVampireAttempt attempt = new FilteredVampireAttempt(problem, result);
            attempts.add(attempt);
            System.out.println("EAxFilterContradictionRunner.run(): " + problem.getFileName() + " -> " + (result == null ? "NoResult" : result.getSzsStatus()));
            if (attempt.foundContradiction()) {
                successfulAttempt = attempt;
                break;
            }
        }
        return new EAxFilterVampireResult(null, filterResult, attempts, successfulAttempt);
    }

    /** Rebuilds E's selection from the original valid FOF source statements. */
    private boolean rebuildFilteredProblem(Path problem, ContradictionProbe probe, Map<String,String> sourceAxioms) throws IOException {

        List<String> selectedNames = new ArrayList<>();
        for (String line : Files.readAllLines(problem)) {
            Matcher matcher = AXIOM.matcher(line);
            if (matcher.matches() && sourceAxioms.containsKey(matcher.group(1))) selectedNames.add(matcher.group(1));
        }
        if (selectedNames.isEmpty()) return false;
        List<String> rebuilt = new ArrayList<>();
        rebuilt.add("% Rebuilt from EAxFilter-selected source axioms");
        for (String name : selectedNames) rebuilt.add(sourceAxioms.get(name));
        if (!selectedNames.contains(probe.axiomName())) rebuilt.add("fof(" + probe.axiomName() + ",axiom,(" + probe.axiom() + ")).");
        rebuilt.add("fof(" + probe.conjectureName() + ",conjecture,(~(" + probe.axiom() + "))).");
        Files.write(problem, rebuilt);
        return true;
    }

    /** Indexes the generated SUMO FOF source by axiom name. */
    private Map<String,String> readSourceAxioms(Path inputProblem) throws IOException {

        Map<String,String> axioms = new LinkedHashMap<>();
        for (String line : Files.readAllLines(inputProblem)) {
            Matcher matcher = AXIOM.matcher(line);
            if (matcher.matches()) axioms.put(matcher.group(1), line);
        }
        return axioms;
    }
}
