package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/********************************************************************
 * Immutable snapshot of all diagnostics rendered by Diagnostics.jsp.
 */
public final class DiagnosticsCache {

    private final String kbName;
    private final Map<String, Set<String>> kifSyntaxErrors;
    private final List<String> termsNotBelowEntity;
    private final List<String> childrenOfDisjointParents;
    private final List<String> partitionViolations;
    private final Map<Formula, Set<String>> formulaeWithTypeViolations;
    private final List<String> relationsWithoutFormat;
    private final List<String> termsWithoutDoc;
    private final List<String> termsWithMultipleDoc;
    private final List<String> termCapDiff;
    private final List<String> membersNotInAnyPartitionClass;
    private final List<String> termsWithoutRules;
    private final List<Formula> quantifierNotInBody;
    private final List<Formula> unquantsInConseq;
    private final boolean termDependencyCacheAvailable;
    private final Map<String, Map<String, List<String>>> missingConstituentDependencies;
    private final Map<String, List<List<String>>> mutualDependencies;

    /********************************************************************
     * Create an immutable snapshot of all diagnostics for a knowledge base.
     * @param kbName the knowledge-base name
     * @param kifSyntaxErrors KIF syntax errors by constituent
     * @param termsNotBelowEntity terms without a path to Entity
     * @param childrenOfDisjointParents terms with disjoint parents
     * @param partitionViolations partition violations
     * @param formulaeWithTypeViolations formula type violations
     * @param relationsWithoutFormat relations without formats
     * @param termsWithoutDoc terms without documentation
     * @param termsWithMultipleDoc terms with multiple documentation strings
     * @param termCapDiff capitalization differences
     * @param membersNotInAnyPartitionClass instances missing from partition classes
     * @param termsWithoutRules terms absent from rules
     * @param quantifierNotInBody formulas with extraneous quantified variables
     * @param unquantsInConseq formulas with unquantified consequent variables
     * @param termDependencyCacheAvailable whether the term-dependency cache is available
     * @param missingConstituentDependencies missing constituent dependencies
     * @param mutualDependencies mutual constituent dependencies
     */
    public DiagnosticsCache(String kbName,
            Map<String, Set<String>> kifSyntaxErrors,
            List<String> termsNotBelowEntity,
            List<String> childrenOfDisjointParents,
            List<String> partitionViolations,
            Map<Formula, Set<String>> formulaeWithTypeViolations,
            List<String> relationsWithoutFormat,
            List<String> termsWithoutDoc,
            List<String> termsWithMultipleDoc,
            List<String> termCapDiff,
            List<String> membersNotInAnyPartitionClass,
            List<String> termsWithoutRules,
            List<Formula> quantifierNotInBody,
            List<Formula> unquantsInConseq,
            boolean termDependencyCacheAvailable,
            Map<String, Map<String, List<String>>> missingConstituentDependencies,
            Map<String, List<List<String>>> mutualDependencies) {

        this.kbName = kbName;
        this.kifSyntaxErrors = copySetMap(kifSyntaxErrors);
        this.termsNotBelowEntity = copyList(termsNotBelowEntity);
        this.childrenOfDisjointParents = copyList(childrenOfDisjointParents);
        this.partitionViolations = copyList(partitionViolations);
        this.formulaeWithTypeViolations = copyFormulaSetMap(formulaeWithTypeViolations);
        this.relationsWithoutFormat = copyList(relationsWithoutFormat);
        this.termsWithoutDoc = copyList(termsWithoutDoc);
        this.termsWithMultipleDoc = copyList(termsWithMultipleDoc);
        this.termCapDiff = copyList(termCapDiff);
        this.membersNotInAnyPartitionClass = copyList(membersNotInAnyPartitionClass);
        this.termsWithoutRules = copyList(termsWithoutRules);
        this.quantifierNotInBody = copyList(quantifierNotInBody);
        this.unquantsInConseq = copyList(unquantsInConseq);
        this.termDependencyCacheAvailable = termDependencyCacheAvailable;
        this.missingConstituentDependencies = copyNestedListMap(missingConstituentDependencies);
        this.mutualDependencies = copyListListMap(mutualDependencies);
    }

    /********************************************************************
     * Create an empty diagnostic snapshot for an unavailable knowledge base.
     * @param kbName the knowledge-base name
     * @return an empty diagnostic snapshot
     */
    public static DiagnosticsCache empty(String kbName) {

        return new DiagnosticsCache(kbName, Collections.emptyMap(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), false, Collections.emptyMap(),
                Collections.emptyMap());
    }

    /********************************************************************
     * Create an immutable defensive copy of a list.
     * @param source the source collection
     * @return an immutable list
     */
    private static <T> List<T> copyList(List<T> source) {

        if (source == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    /********************************************************************
     * Create an immutable defensive copy of a string-to-set map.
     * @param source the source collection
     * @return an immutable map
     */
    private static Map<String, Set<String>> copySetMap(Map<String, Set<String>> source) {

        if (source == null) return Collections.emptyMap();
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : source.entrySet())
            copy.put(entry.getKey(), Collections.unmodifiableSet(new TreeSet<>(entry.getValue())));
        return Collections.unmodifiableMap(copy);
    }

    /********************************************************************
     * Create an immutable defensive copy of a formula-to-error-set map.
     * @param source the source collection
     * @return an immutable map
     */
    private static Map<Formula, Set<String>> copyFormulaSetMap(Map<Formula, Set<String>> source) {

        if (source == null) return Collections.emptyMap();
        Map<Formula, Set<String>> copy = new TreeMap<>();
        for (Map.Entry<Formula, Set<String>> entry : source.entrySet())
            copy.put(entry.getKey(), Collections.unmodifiableSet(new TreeSet<>(entry.getValue())));
        return Collections.unmodifiableMap(copy);
    }

    /********************************************************************
     * Create an immutable defensive copy of a nested string-list map.
     * @param source the source collection
     * @return an immutable map
     */
    private static Map<String, Map<String, List<String>>> copyNestedListMap(
            Map<String, Map<String, List<String>>> source) {

        if (source == null) return Collections.emptyMap();
        Map<String, Map<String, List<String>>> copy = new TreeMap<>();
        for (Map.Entry<String, Map<String, List<String>>> outer : source.entrySet()) {
            Map<String, List<String>> innerCopy = new TreeMap<>();
            for (Map.Entry<String, List<String>> inner : outer.getValue().entrySet())
                innerCopy.put(inner.getKey(), copyList(inner.getValue()));
            copy.put(outer.getKey(), Collections.unmodifiableMap(innerCopy));
        }
        return Collections.unmodifiableMap(copy);
    }

    /********************************************************************
     * Create an immutable defensive copy of a string-to-list-of-lists map.
     * @param source the source collection
     * @return an immutable map
     */
    private static Map<String, List<List<String>>> copyListListMap(
            Map<String, List<List<String>>> source) {

        if (source == null) return Collections.emptyMap();
        Map<String, List<List<String>>> copy = new TreeMap<>();
        for (Map.Entry<String, List<List<String>>> entry : source.entrySet()) {
            List<List<String>> lists = new ArrayList<>();
            for (List<String> values : entry.getValue()) lists.add(copyList(values));
            copy.put(entry.getKey(), Collections.unmodifiableList(lists));
        }
        return Collections.unmodifiableMap(copy);
    }

    /********************************************************************
     * Return the knowledge-base name represented by this snapshot.
     * @return the knowledge-base name
     */
    public String getKbName() { return kbName; }
    /********************************************************************
     * Return cached KIF syntax errors grouped by constituent.
     * @return the cached KIF syntax errors
     */
    public Map<String, Set<String>> getKifSyntaxErrors() { return kifSyntaxErrors; }
    /********************************************************************
     * Return cached terms without a path to Entity.
     * @return the cached terms
     */
    public List<String> getTermsNotBelowEntity() { return termsNotBelowEntity; }
    /********************************************************************
     * Return cached terms whose parents are disjoint.
     * @return the cached terms
     */
    public List<String> getChildrenOfDisjointParents() { return childrenOfDisjointParents; }
    /********************************************************************
     * Return cached partition violations.
     * @return the cached violations
     */
    public List<String> getPartitionViolations() { return partitionViolations; }
    /********************************************************************
     * Return cached formulas with type violations.
     * @return the cached formula violations
     */
    public Map<Formula, Set<String>> getFormulaeWithTypeViolations() { return formulaeWithTypeViolations; }
    /********************************************************************
     * Return cached relations without natural-language formats.
     * @return the cached relations
     */
    public List<String> getRelationsWithoutFormat() { return relationsWithoutFormat; }
    /********************************************************************
     * Return cached terms without documentation.
     * @return the cached terms
     */
    public List<String> getTermsWithoutDoc() { return termsWithoutDoc; }
    /********************************************************************
     * Return cached terms with multiple documentation strings.
     * @return the cached terms
     */
    public List<String> getTermsWithMultipleDoc() { return termsWithMultipleDoc; }
    /********************************************************************
     * Return cached terms differing only in capitalization.
     * @return the cached terms
     */
    public List<String> getTermCapDiff() { return termCapDiff; }
    /********************************************************************
     * Return cached instances missing from their partition classes.
     * @return the cached instances
     */
    public List<String> getMembersNotInAnyPartitionClass() { return membersNotInAnyPartitionClass; }
    /********************************************************************
     * Return cached terms that do not occur in rules.
     * @return the cached terms
     */
    public List<String> getTermsWithoutRules() { return termsWithoutRules; }
    /********************************************************************
     * Return cached formulas with quantified variables absent from their bodies.
     * @return the cached formulas
     */
    public List<Formula> getQuantifierNotInBody() { return quantifierNotInBody; }
    /********************************************************************
     * Return cached formulas with unquantified consequent variables.
     * @return the cached formulas
     */
    public List<Formula> getUnquantsInConseq() { return unquantsInConseq; }
    /********************************************************************
     * Report whether the term-dependency cache existed when this snapshot was built.
     * @return true if the term-dependency cache was available
     */
    public boolean isTermDependencyCacheAvailable() { return termDependencyCacheAvailable; }
    /********************************************************************
     * Return cached missing constituent dependencies.
     * @return the cached missing dependencies
     */
    public Map<String, Map<String, List<String>>> getMissingConstituentDependencies() {
        return missingConstituentDependencies;
    }
    /********************************************************************
     * Return cached mutual constituent dependencies.
     * @return the cached mutual dependencies
     */
    public Map<String, List<List<String>>> getMutualDependencies() { return mutualDependencies; }
}
