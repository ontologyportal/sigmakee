# Incremental KBCache Update & TPTP Generation

## Problem

TPTP file generation (SUMO.tptp, SUMO.tff, etc.) takes hours for the full SUMO KB. The pipeline
depends on KBcache (type hierarchy, signatures, instance relationships) which is computed once at
startup and never updated after `tell()`. When a user adds a schema-level assertion (e.g.,
`(subclass Robot Agent)`), the system either:

- Skips regeneration entirely (stale file), or
- Triggers "full regeneration" that still uses stale KBcache data (current bug)

**Goal:** After the expensive first-time generation, subsequent schema changes should:

1. Incrementally update KBcache (not rebuild from scratch)
2. Identify only the affected formulas
3. Update only those formulas in the session's TPTP files

## Architecture Overview

```
Startup (once, expensive):
  KIF files → KB.formulaMap → KBcache.buildCaches() → SUMO.tptp/tff (base, shared)

Per-session schema tell():
  tell("(subclass Robot Agent)")
    → Session KBcache copy (deep copy from base)        [M3.1]
    → Incremental KBcache update (only affected fields)  [M3.2]
    → Identify affected formulas (via reverse index)     [M3.3]
    → Retranslate only affected formulas                 [M3.4]
    → Patch session TPTP file                            [M3.4]
```

## Key Functions & Responsibilities

### KIF Parsing → KB Index (`KIF.java`, `KB.java`)

- `KIF.readFile()` parses .kif files, creates lookup keys via `KIF.createKey()`:
  - `"arg-0-subclass"`, `"arg-1-Dog"`, `"arg-2-Animal"` for `(subclass Dog Animal)`
- `KB.addConstituentInfo()` merges per-file indexes into `KB.formulas` (global index) and `KB.formulaMap` (formula store)
- `KB.ask("arg", 0, "subclass")` queries the index — returns all subclass formulas

### KBcache Build (`KBcache.java`)

`buildCaches()` runs these phases sequentially, each using `kb.ask()` to query the formula index:

| Phase | Method                                                | Produces                                  | Queries                                                                  |
| ----- | ----------------------------------------------------- | ----------------------------------------- | ------------------------------------------------------------------------ |
| 1     | `buildInsts()`                                        | `insts` (all instance subjects)           | `ask("arg",0,"instance")`                                                |
| 2     | `buildRelationsSet()`                                 | `relations`, `predicates`, `functions`    | `insts` + type checks                                                    |
| 3     | `buildTransitiveRelationsSet()`                       | `transRels`                               | instance checks                                                          |
| 4     | `buildParents()`                                      | `parents` (transitive ancestors per term) | `ask("arg",0,rel)` for each transitive rel                               |
| 5     | `buildChildren()`                                     | `children` (transitive descendants)       | inverts `parents`                                                        |
| 6     | `collectDomains()`                                    | `signatures`, `valences`                  | `ask("arg",0,"domain/range")` + inheritance via `parents["subrelation"]` |
| 7     | `buildInstTransRels()`                                | `instTransRels`                           | depends on `collectDomains`                                              |
| 8-9   | `buildDirectInstances()` + `addTransitiveInstances()` | `instances`                               | `ask("arg",0,"instance")` + `parents["subclass"]`                        |
| 10    | `buildTransInstOf()`                                  | `instanceOf` (all classes per instance)   | `instances` + `parents["subclass"]`                                      |
| 11    | `correctValences()`                                   | fixes `valences`                          | VariableArityRelation checks                                             |

### Formula Preprocessing (`FormulaPreprocessor.java`)

`preProcess()` transforms each KIF formula before TPTP translation. It depends on KBcache:

| Step | Method                                             | KBcache dependency                                                                             |
| ---- | -------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| 1    | `replacePredVarsAndRowVars()`                      | `predicates`, `valences`, `instanceOf` — enumerates matching relations for predicate variables |
| 2    | `addInstancesOfSetOrClass()`                       | `formulaMap` only                                                                              |
| 3    | `preProcessRecurse()`                              | `relations`, `functions`                                                                       |
| 4    | `addTypeRestrictions()` → `computeVariableTypes()` | `signatures` (arg types per predicate), `children["subclass"]` (for `winnowTypeList`)          |

Type guards are `(instance ?X Type)` clauses injected based on `signatures`. The type is determined
by `findType(argnum, pred, kb)` → `kbCache.signatures.get(pred)`. Winnowing removes redundant
supertypes using `kb.isSubclass()` → `children["subclass"]`.

### TPTP Generation (`SUMOKBtoTPTPKB.java`)

`_tWriteFile()` has two phases:

- **Phase 1 (parallel):** `translateOneFormula()` runs `preProcess → rename → translate` per formula
- **Phase 2 (sequential):** Dedup via `alreadyWrittenTPTPs`, assign axiom names (`kb_SUMO_N`), write to file

`axiomKey` maps `"kb_SUMO_1234" → Formula` — the reverse index from axiom name to source formula.
After initial generation this map is **read-only**; session patches use per-session axiom keys.

### Current Bug

`tell()` adds formulas to `formulaMap` but **never calls `buildCaches()`**. When
`generateSessionTPTP()` triggers "full regen", it runs `preProcess()` against the **stale**
KBcache. Type guards, predicate variable expansions, and winnowing are all based on pre-tell() state.

---

## Milestones

### M3.0 — Fix Session Regen to Use Fresh KBcache (SKIPPED)

**Priority: Critical — fixes correctness bug**
**Status: Skipped** — superseded by M3.1 which provides per-session isolation without mutating
the shared KBcache.

Before any incremental work, fix the existing bug: session TPTP regeneration must use
up-to-date KBcache.

#### Steps

- [ ] In `SessionTPTPManager.generateSessionTPTP()`: before calling `generateFOFToPath()`/
      `generateTFFToPath()`, call `kb.kbCache.buildCaches()` to refresh the cache
- [ ] This makes session regen correct but slow (full `buildCaches()` + full formula translation)
- [ ] Guard with a lock to prevent concurrent `buildCaches()` from corrupting shared state
- [ ] Verify: after `tell("(subclass Robot Agent)")`, regenerated TPTP file has correct type
      guards referencing Robot in Agent's hierarchy

**Note: This mutates the shared KBcache, which is a problem for multi-user scenarios.
M3.1 fixes this with per-session copies.**

#### Files

- `src/java/com/articulate/sigma/trans/SessionTPTPManager.java`

---

### M3.1 — Per-Session KBcache Copy ✅

**Priority: High — enables session isolation for KBcache**
**Status: Complete**

Each session that does a schema-level `tell()` gets its own KBcache copy. The shared base
KBcache remains immutable after startup.

#### Steps

- [x] Complete the `KBcache` copy constructor: add missing fields (`functions`, `predicates`,
      `instRels`, `instances`, `disjoint`, `disjointRelations`, `initialized`)
- [x] Add `sessionCaches: ConcurrentHashMap<String, KBcache>` to `SessionTPTPManager`
- [x] On first schema-level `tell()` in a session: deep-copy the base KBcache via
      `getOrCreateSessionCache(sessionId, kb)`
- [x] `generateSessionTPTP()` swaps `kb.kbCache` to the session cache within the per-session
      lock, restores in `finally`
- [x] Session cleanup (`cleanupSession()`) removes the session KBcache
- [ ] Memory optimization: copy-on-write — session initially shares base map references,
      only copies a map when it's about to be modified _(not yet implemented; full deep copy used)_

#### Implementation Notes

- `getOrCreateSessionCache(sessionId, kb)` — lazily creates deep copy on first call
- `getSessionCache(sessionId)` — returns existing copy or null
- The `kb.kbCache` swap is protected by the per-session lock; cross-session races are
  accepted as a known limitation for this milestone

#### Tests Added

- 7 copy constructor tests in `KBcacheUnitTest.java`
  (`testCopyConstructorFunctions`, `testCopyConstructorPredicates`, `testCopyConstructorInstRels`,
  `testCopyConstructorInstances`, `testCopyConstructorDisjoint`, `testCopyConstructorInitialized`,
  `testCopyConstructorIndependence`)

#### Files

- `src/java/com/articulate/sigma/KBcache.java` (complete copy constructor)
- `src/java/com/articulate/sigma/trans/SessionTPTPManager.java` (session KBcache lifecycle)
- `test/unit/java/com/articulate/sigma/KBcacheUnitTest.java` (copy constructor tests)

---

### M3.2 — Incremental KBcache Update Methods ✅

**Priority: High — avoids full `buildCaches()` rebuild**
**Status: Complete**

Add targeted update methods to KBcache for each schema predicate, instead of rebuilding
everything from scratch.

#### Methods Added

```java
// In KBcache.java — each returns Set<String> of changed terms for M3.3:
Set<String> addSubclass(String child, String parent)
Set<String> addInstance(String instance, String className)
Set<String> addDomain(String relation, int argNum, String type)
Set<String> addRange(String relation, String type)
Set<String> addSubrelation(String child, String parent)
Set<String> addDisjoint(String class1, String class2)
```

The internal `addInstance(child, parent)` helper was renamed to `addDirectInstance()` to
avoid collision with the new public API.

#### Algorithm for `addSubclass(child, parent)`:

```
1. newParents = {parent} ∪ parents["subclass"][parent]
2. parents["subclass"][child] += newParents  (create if absent)
3. For each descendant D of child (from children["subclass"][child]):
     parents["subclass"][D] += newParents
4. allNewChildren = {child} ∪ children["subclass"][child]
5. For parent and each ancestor A of parent:
     children["subclass"][A] += allNewChildren
6. For each instance I where instanceOf[I] contains child or any descendant of child:
     instanceOf[I] += newParents
7. instancesOfNewChildren = ∪ instances[C] for each C in allNewChildren (skip if absent)
   For parent and each ancestor A:
     if instances[A] != null:  // only update existing entries, never create new ones
         instances[A] += instancesOfNewChildren
```

Cost: O(|descendants(child)| + |ancestors(parent)|) — typically tens to hundreds, not 75K.

---

#### Algorithm for `addInstance(inst, className)`:

```
1. insts.add(inst)
2. allClasses = {className} ∪ parents["subclass"][className]
3. instanceOf[inst] += allClasses  (create if absent)
4. instances[className].add(inst)  (always create if absent — mirrors buildDirectInstances)
5. For each ancestor A in allClasses where A ≠ className:
     if instances[A] != null:  // only update existing entries — mirrors addTransitiveInstances
         instances[A].add(inst)
```

Cost: O(|ancestors(className)|)

Note: `instances[className]` is always created (step 4), matching `buildDirectInstances`
which creates an entry for every class with at least one direct `(instance ? className)`
assertion. Ancestor entries are only augmented if they already exist, matching
`addTransitiveInstances` behaviour.

---

#### Algorithm for `addDomain(relation, argNum, type)`:

```
1. sig = signatures[relation]  (create if absent)
   extend sig to length argNum+1, padding with "" where needed
   sig[argNum] = type
2. if argNum > 0:
     valences[relation] = max(valences[relation], argNum)
3. subRels = children["subrelation"][relation]  (all transitive sub-relations)
   for each sr in subRels:
     srSig = signatures[sr]  (create if absent)
     extend srSig to length argNum+1, padding with ""
     if srSig[argNum] is empty:   // inherit only where no explicit binding exists
         srSig[argNum] = type
```

Cost: O(|sub-relations of relation|)

Note: `domainSubclass` assertions pass `type + "+"` (e.g. `"Agent+"`) so the marker is
preserved in the signature list and downstream consumers can distinguish domain from
domainSubclass.

---

#### Algorithm for `addRange(relation, type)`:

```
Delegates to addDomain(relation, 0, type).
Range is stored at position 0 of the signatures list (slot 0 = return type by convention).
```

Cost: same as `addDomain`.

---

#### Algorithm for `addSubrelation(child, parent)`:

Mirrors `addSubclass` for the `subrelation` hierarchy, plus signature inheritance from
the parent relation.

```
1. newParents = {parent} ∪ parents["subrelation"][parent]
2. parents["subrelation"][child] += newParents  (create if absent)
3. For each descendant D of child (from children["subrelation"][child]):
     parents["subrelation"][D] += newParents
4. allNewChildren = {child} ∪ children["subrelation"][child]
5. For parent and each ancestor A of parent:
     children["subrelation"][A] += allNewChildren
6. Signature inheritance:
     parentSig = signatures[parent]  (skip if absent)
     parentValence = valences[parent]
     childSig = signatures[child]  (create if absent)
     extend childSig to length parentValence+1, padding with ""
     for argNum in 0..parentValence:
       if childSig[argNum] is empty and parentSig[argNum] is non-empty:
           childSig[argNum] = parentSig[argNum]
     valences[child] = max(valences[child], parentValence)
```

Cost: O(|descendants(child)| + |ancestors(parent)| + valence(parent))

---

#### Algorithm for `addDisjoint(class1, class2)`:

```
1. explicitDisjoint[class1].add(class2)  (symmetric)
   explicitDisjoint[class2].add(class1)
2. group1 = {class1} ∪ children["subclass"][class1]
   group2 = {class2} ∪ children["subclass"][class2]
3. for each c1 in group1, c2 in group2 where c1 ≠ c2:
     disjoint.add(c1 + "\t" + c2)   // both orderings for O(1) lookup
     disjoint.add(c2 + "\t" + c1)
```

Cost: O(|descendants(class1)| × |descendants(class2)|)

Note: `disjoint` is a flat `Set<String>` of tab-separated pairs. Membership is tested with
`disjoint.contains(c1 + "\t" + c2)` in O(1). Both orderings are stored so the check is
symmetric without requiring the caller to normalise the order.

#### Implementation Notes

- Step 7 of `addSubclass` only updates **existing** `instances` entries — it never creates
  new entries for ancestor classes. This matches `buildCaches()` behaviour where `instances[X]`
  is only populated for classes with at least one direct `(instance ? X)` assertion.
- All methods return the set of terms whose cache entries changed; callers pass this to
  `findAffectedFormulas()` (M3.3).

#### Steps

- [x] Implement `addSubclass()` with transitive propagation to parents, children, instanceOf, instances
- [x] Implement `addInstance()` — add class + all superclasses to instanceOf; add to instances
- [x] Implement `addDomain()` / `addRange()` — update signatures + inherit down subrelation tree
- [x] Implement `addSubrelation()` — same pattern as addSubclass for the subrelation hierarchy
- [x] Implement `addDisjoint()` — update explicitDisjoint + expand to children×children in disjoint
- [x] Unit tests: for each method, verify result matches full `buildCaches()` output
- [x] Each method returns `Set<String>` of changed terms (serves as `getAffectedTerms()`)

#### Tests Added

- 23 tests in `KBcacheIncrementalTest.java` (new file, added to `UnitSigmaTestSuite`)

#### Files

- `src/java/com/articulate/sigma/KBcache.java`
- `test/unit/java/com/articulate/sigma/KBcacheIncrementalTest.java` (new)

---

### M3.3 — Affected Formula Identification ✅

**Priority: High — determines which formulas need retranslation**
**Status: Complete**

Given the M3.2 cache update, identify which existing formulas in the TPTP file need
retranslation. The key insight is that **cache-level changed terms ≠ translation-level
affected formulas**: `addSubclass` returns ancestors like `Entity` and `Object` as
"changed" (because their `children` and `instances` maps grew), but those ancestors appear
in thousands of SUMO formulas whose TPTP output does not actually change. Scanning by
ancestor terms therefore causes unnecessary patching of ~99% of the KB.

Three separate methods handle different predicate types.

---

#### `findAffectedFormulas(KB kb, Set<String> changedTerms)` — generic (domain/range/subrelation)

Used when the update genuinely alters `signatures` (type guards change).

A formula is affected if:

1. **Direct reference** — formula mentions any changed term at argument positions 0–5
   (`kb.ask("arg", N, term)` for N = 0–5)
2. **Predicate variable** — formula has predicate variables (`predVarCache` non-null and
   non-empty) **and** at least one changed term is a relation in `kb.kbCache`
3. **varTypeCache cleared** — on every affected formula, `f.varTypeCache` is reset to
   force `computeVariableTypes()` to recompute on the next `preProcess()` call

---

#### `findAffectedFormulasForSubclass(KB kb, KBcache sessionCache, String child, String parent)`

Used for `subclass` / `immediateSubclass` tells. Does **not** scan ancestor terms.

A formula is affected if any of the following holds:

1. **Direct mention of `child`** — `kb.ask("arg", N, child)` for N = 0–5.
   Formulas referencing the new (or repositioned) class may now have tighter type guards
   due to `winnowTypeList()`.

2. **Signature / type-guard dependency** — **intentionally omitted**.
   Scanning via signature entries causes too many false positives:
   - `kb.ask("arg", N, rel)` sweeps in ground facts such as `(instance rel Class)` that
     mention the relation name by position but have no variables and therefore no type guards.
   - Format / documentation formulas (excluded from TPTP) produce warnings.
   Impact of omitting: at worst a formula retains a slightly redundant type guard, e.g.
   `(and (instance ?X Man) (instance ?X GreekAncestor))` instead of `(instance ?X Man)`.
   That is harmless over-specification — provers still find the same answers.

3. **Predicate variable expansion** — only if `sessionCache.relations.contains(child)`.
   Adding a new relation triggers `PredVarInst` re-expansion of predVar formulas.

4. **varTypeCache cleared** — on every formula in the affected set.

Uses `sessionCache` (the already-updated session copy) for all cache lookups — never
`kb.kbCache`.

---

#### `findAffectedFormulasForInstance(KB kb, KBcache sessionCache, String inst, String className)`

Used for `instance` / `immediateInstance` tells. Instance additions do **not** alter
`signatures`, so no type-guard scan is needed.

A formula is affected if:

1. **Direct mention of `inst`** — `kb.ask("arg", N, inst)` for N = 0–5.

2. **Predicate variable expansion** — only if `sessionCache.relations.contains(inst)`.
   If `inst` is itself a relation (e.g., `(instance myRel BinaryRelation)`), predVar
   formulas must be re-expanded against the updated relations set.

3. **varTypeCache cleared** — on every formula in the affected set.

---

#### Steps

- [x] Add `findAffectedFormulas(KB kb, Set<String> changedTerms)` to `SUMOKBtoTPTPKB`
      (generic, used for domain/range/subrelation)
- [x] Add `findAffectedFormulasForSubclass(KB kb, KBcache sessionCache, String child, String parent)`
      — targeted subclass detection avoiding ancestor explosion
- [x] Add `findAffectedFormulasForInstance(KB kb, KBcache sessionCache, String inst, String className)`
      — targeted instance detection
- [x] All methods use `sessionCache` (not `kb.kbCache`) for cache lookups
- [x] All methods clear `varTypeCache` on every formula in the returned set

#### Tests Added

- 27 tests in `AffectedFormulasTest.java` (added to `UnitSigmaTestSuite`):
  - 12 tests for `findAffectedFormulas` (generic)
  - 8 tests for `findAffectedFormulasForSubclass`
    (direct mention, unrelated ancestor excluded, signature dependency, predVar
    included/excluded based on whether child is a relation, varTypeCache cleared, null safety)
  - 7 tests for `findAffectedFormulasForInstance`
    (direct mention, unrelated excluded, predVar included/excluded, varTypeCache cleared,
    null safety)

#### Files

- `src/java/com/articulate/sigma/trans/SUMOKBtoTPTPKB.java`
- `test/unit/java/com/articulate/sigma/AffectedFormulasTest.java`

---

### M3.4 — Incremental TPTP File Update ✅

**Priority: High — the final piece**
**Status: Complete**

Given the affected formulas from M3.3, update the session's TPTP file without regenerating
all ~35K+ formulas.

#### Approach: Patch from session file if it already exists

Each `patchSessionTPTP` call reads from:

- The **session file** if it already exists (preserves all previous patches from earlier `tell()` calls)
- The **shared base file** (`SUMO.tptp` / `SUMO.tff`) on the first call for a session

This ensures multiple successive `tell()` calls accumulate correctly without the caller
needing to track accumulated affected formulas.

#### Two categories of output in a single atomic write

| Category                        | Description                                                                                                                             | Source                  |
| ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| **Retranslated base axioms**    | Existing KB formulas whose TPTP translation changes because the session KBcache was updated (e.g., new type guards after `addSubclass`) | `affected` parameter    |
| **New tell() assertion axioms** | Formulas just added to the KB via `tell()`, never previously translated                                                                 | `newFormulas` parameter |

Both are translated by `retranslateFormulas()` with `kb.kbCache` swapped to the session cache.
Old axiom lines for retranslated formulas are **commented out** (`% [patched out] ...`);
all new lines are **appended** at the end.

#### Session-isolated axiom key

The global `SUMOKBtoTPTPKB.axiomKey` is **read-only** after initial KB generation.
Each session has its own `sessionAxiomKeys` entry (`ConcurrentHashMap<String, Formula>`)
that tracks all axiom names created by patches in that session.

`buildReverseIndex(sessionId)` merges both sources so successive patches correctly identify
and comment out axioms from previous patches.

#### Steps

- [x] Add `retranslateFormulas(KB kb, Set<Formula> formulas, String lang)` to `SUMOKBtoTPTPKB`
- [x] Add `patchSessionTPTP(sessionId, kb, lang, affected, newFormulas, sessionCache)` to
      `SessionTPTPManager`
- [x] Implement line-by-line copy with axiom replacement (old lines commented out)
- [x] Handle axiom count changes: extras appended at end, removed ones commented out
- [x] Add new `tell()` assertions (`newFormulas`) at the end of the file
- [x] Per-session axiom key (`sessionAxiomKeys`) — global `axiomKey` read-only after init
- [x] Option B: patch from session file if it already exists (multi-tell() support)
- [x] `cleanupSession()` removes session axiom key
- [ ] Verify: diff session file against full-regen reference for correctness

#### Key Design Decisions

- **`buildReverseIndex(sessionId)`** merges global `axiomKey` + session axiom key so the
  second `tell()` correctly finds and comments out axioms appended by the first `tell()`
- **Patch index offset**: new axiom names start at `globalSize + sessionSize + 100_000`
  to avoid collisions with base axiom indices
- **`extractAxiomName(line)`**: parses `kb_*` names from `fof(...)` / `tff(...)` lines;
  ignores comment lines, conjecture lines, and any non-`kb_` names

#### Files

- `src/java/com/articulate/sigma/trans/SessionTPTPManager.java`
- `src/java/com/articulate/sigma/trans/SUMOKBtoTPTPKB.java`

---

### M3.5 — Integration: Wire tell() to Incremental Pipeline ✅

**Priority: High — connects all the pieces**
**Status: Complete**

Modify `tell()` to use the incremental pipeline when a schema-level assertion is detected.

#### Flow After Integration

```
tell("(subclass Robot Agent)", sessionId)
  ├── KB.merge() — add to formulaMap + formulas index (existing)
  ├── Write to session UA file (existing)
  ├── Detect schema predicate (existing: TPTP_BASE_REGEN_PREDICATES)
  ├── Get/create session KBcache: getOrCreateSessionCache(sessionId, kb)  [M3.1]
  ├── applyIncrementalUpdate(kb, sessionId, formula, lang):
  │     ├── sessionCache.addSubclass("Robot", "Agent")                    [M3.2]
  │     ├── findAffectedFormulasForSubclass(kb, sessionCache,             [M3.3]
  │     │       "Robot", "Agent")
  │     │     → affected (formulas whose TPTP output changes)
  │     └── patchSessionTPTP(sessionId, kb, lang,                        [M3.4]
  │             affected,           ← existing formulas to retranslate
  │             Set.of(newFormula), ← the just-tell()'d formula
  │             sessionCache)
  └── (done — session file is up to date)
```

#### Steps

- [x] Modify `KB.tell()` to detect schema predicates and trigger incremental update
- [x] Wire session KBcache creation/retrieval
- [x] Wire incremental update method dispatch based on predicate type
- [x] Wire affected formula identification and TPTP patching
- [x] End-to-end unit tests: `tell()` schema assertion → verify session TPTP is correct
- [ ] Performance test: measure time for incremental update vs full regen _(deferred — non-blocking)_

#### Implementation Details

**`KB.TPTP_BASE_REGEN_PREDICATES`** (private static final `Set<String>`):

```
subclass, domain, domainSubclass, range, rangeSubclass,
immediateInstance, immediateSubclass, disjoint, partition,
exhaustiveDecomposition, successorClass, partialOrderingOn,
trichotomizingOn, totalOrderingOn, disjointDecomposition
```

**`KB.tell()` dispatch (Case A / Case B):**

- **Case A** — predicate in `TPTP_BASE_REGEN_PREDICATES` → `SessionTPTPManager.applyIncrementalUpdate()`
- **Case B** — ground fact on a transitive predicate (not in Case A) → `SessionTPTPManager.generateSessionTPTP()` (full regen fallback)
- **Neither** — simple assertion (e.g., plain `instance`) → no TPTP update in current version

**`SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, lang)`:**

Each switch case both updates the session cache **and** computes `affected` — there is no
shared `changedTerms` variable. The `affected` set is computed by the M3.3 targeted
methods (not the generic `findAffectedFormulas`) for `subclass` and `instance`.

```
switch (pred):
  "subclass" / "immediateSubclass":
      child  = formula.arg(1);  parent = formula.arg(2)
      sessionCache.addSubclass(child, parent)                    [M3.2]
      affected = findAffectedFormulasForSubclass(                [M3.3]
                     kb, sessionCache, child, parent)

  "instance" / "immediateInstance":
      inst  = formula.arg(1);  className = formula.arg(2)
      sessionCache.addInstance(inst, className)                  [M3.2]
      affected = findAffectedFormulasForInstance(                [M3.3]
                     kb, sessionCache, inst, className)

  "domain" / "domainSubclass":
      changedTerms = sessionCache.addDomain(rel, argNum, type)   [M3.2]
      affected = findAffectedFormulas(kb, changedTerms)          [M3.3 generic]

  "range" / "rangeSubclass":
      changedTerms = sessionCache.addRange(rel, type)            [M3.2]
      affected = findAffectedFormulas(kb, changedTerms)          [M3.3 generic]

  "subrelation":
      changedTerms = sessionCache.addSubrelation(child, parent)  [M3.2]
      affected = findAffectedFormulas(kb, changedTerms)          [M3.3 generic]

  "disjoint":
      sessionCache.addDisjoint(c1, c2)                           [M3.2]
      // affected stays empty — disjoint does not change type guards
      // or predicate variable expansion

  default (partition, exhaustiveDecomposition, etc.):
      → fall back to generateSessionTPTP()

patchSessionTPTP(sessionId, kb, lang, affected,
                 singleton(formula), sessionCache)               [M3.4]
```

**`patchSessionTPTP` guard**: if `axiomKey` is empty (no bulk generation yet), falls back to `generateSessionTPTP()` rather than patching an unpopulated file.

#### Tests Added

- 10 tests in `IncrementalTellPipelineTest.java` (new file, added to `UnitSigmaTestSuite`)
  - `testApplyIncrementalUpdate_subclass_cacheUpdated`
  - `testApplyIncrementalUpdate_instance_cacheUpdated`
  - `testApplyIncrementalUpdate_domain_cacheUpdated`
  - `testApplyIncrementalUpdate_range_cacheUpdated`
  - `testApplyIncrementalUpdate_subrelation_cacheUpdated`
  - `testApplyIncrementalUpdate_disjoint_cacheUpdated`
  - `testApplyIncrementalUpdate_unsupported_fallsBackToFullRegen`
  - `testApplyIncrementalUpdate_nullSessionId_returnsNull`
  - `testApplyIncrementalUpdate_emptySessionId_returnsNull`
  - `testApplyIncrementalUpdate_multipleCalls_sessionCacheAccumulates`
  - `testApplyIncrementalUpdate_patchesSessionTPTPFile`

#### Files

- `src/java/com/articulate/sigma/KB.java` — `tell()` dispatch, `TPTP_BASE_REGEN_PREDICATES`
- `src/java/com/articulate/sigma/trans/SessionTPTPManager.java` — `applyIncrementalUpdate()`
- `test/unit/java/com/articulate/sigma/IncrementalTellPipelineTest.java` (new)

---

## Verification

For each milestone:

1. `ant test.unit` — 470 tests pass (401 original + 69 new from M3.1–M3.5)
2. `TPTPGenerationTest` — all 5 tests pass
3. Correctness: after incremental update, compare session TPTP output against a
   full-regen reference (using full `buildCaches()` + `_tWriteFile()`)
4. For M3.2: unit tests verify incremental update matches full `buildCaches()` output
   for each schema predicate type
5. `ant test.integration` — integration tests verify full pipeline against real SUMO KB

### Test Count by Milestone

| Milestone   | New Tests                               | Cumulative Total |
| ----------- | --------------------------------------- | ---------------- |
| Baseline    | —                                       | 401              |
| M3.1        | 7                                       | 408              |
| M3.2        | 23                                      | 431              |
| M3.3        | 27                                      | 458              |
| M3.4        | 15                                      | 473              |
| M3.5        | 11                                      | 484              |
| Integration | 16 (IncrementalPipelineIntegrationTest) | —                |

---

## Post-M3.5 Fix: Targeted Affected Formula Detection for subclass/instance

### Problem

After `tell("(subclass Greek Human)")`, formulas like
`fof(kb_SUMO_2317,axiom,...uses/instrument/agent/AutonomousAgent...)` were being commented
out in the session TPTP file but not re-added.

### Root Cause

`addSubclass("Greek", "Human")` returns `changedTerms` containing ALL ancestors of `Human`
(`AutonomousAgent`, `Object`, `Entity`, …) because the KBcache `children` and `instances`
maps for those ancestors grew. The generic `findAffectedFormulas(kb, changedTerms)` then
scanned for every formula mentioning any of those ancestors — potentially thousands.
Those formulas were commented out, retranslated (producing identical TPTP bodies), and
re-appended under new axiom names. This left the original axiom names missing from the
file, breaking inference.

The fundamental mismatch: **cache-level changed terms ≠ translation-level affected formulas**.

### Analysis: what actually changes after `(subclass child parent)`

Adding a subclass link changes an existing formula's TPTP translation only in these cases:

| Criterion                                                                                                                 | Why it matters                                                       | Scan target                                       |
| ------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------- |
| Formula directly mentions `child`                                                                                         | `child` is the new (or repositioned) class                           | `kb.ask("arg", N, child)`                         |
| Formula uses a relation with `child` in its `signatures` AND also a relation with `parent`/ancestor for the same variable | `winnowTypeList()` now picks `child` over `parent` for that variable | `typeToRels.get(child)` → relations → formulas    |
| `child` is itself a relation → predVar formulas must re-expand                                                            | `PredVarInst` enumerates the updated `relations` set                 | `predVarCache` scan, gated on `child ∈ relations` |

**Key insight: scan from `child`, not `parent`.**
`parent`'s ancestors (Entity, Object, …) appear in signatures of hundreds of relations.
Scanning from `parent`'s side floods the search with nearly the entire KB.
Scanning from `child` is precise: if `child` is new and appears in no signature,
`affectedRels = {}` and 0 existing formulas need retranslation.

### Resolution (post-M3.5 / M3.3 refinement)

**Interim workaround** (now superseded): set `changedTerms = emptySet()` for `subclass`,
`instance`, `disjoint` → `findAffectedFormulas` returned empty → no formulas patched, only
the new formula appended. Correct but overly conservative — missed the narrow set of
formulas whose type guards genuinely do change (signature dependency).

**First implementation** (superseded): introduced `findAffectedFormulasForSubclass` but set
`typeScope = {parent} ∪ parent's ancestors`. This still flooded the search — Entity in
typeScope pulled in nearly the entire KB (51 K retranslated for `(subclass Greek Human)`).

**Current implementation**: `typeScope = {child}` only. For `(subclass Greek Human)`:

- `Greek` is new → not in any signature → `affectedRels = {}` → 0 formulas retranslated
- Only the new `(subclass Greek Human)` formula itself is appended via `newFormulas`

`applyIncrementalUpdate` dispatches to the targeted methods for `subclass` and `instance`;
the `changedTerms` variable is eliminated from those branches. `domain`/`range`/`subrelation`
still use the generic `findAffectedFormulas(kb, changedTerms)` because those predicates
genuinely alter `signatures`.

### Why the Targeted Methods Are Correct

After `(subclass child parent)`:

- **Type guards** (`instance ?X Agent`) are constraints on variables, not enumerations of
  subclasses. Adding `Robot` under `Agent` does not change any type guard unless `Robot`
  already appears in some domain/range declaration (i.e., `Robot ∈ signatures[rel]`).
  Only then can winnowing produce a different result.
- **Predicate-variable expansion** iterates `kbCache.predicates`. Only if `child` is a
  relation does this set grow, triggering re-expansion.
- **Row-variable expansion** and **variable-arity renaming** depend on `valences` /
  `signatures`, not on `subclass` links.

### Test Count Update

| Milestone   | New Tests                               | Cumulative Total |
| ----------- | --------------------------------------- | ---------------- |
| Baseline    | —                                       | 401              |
| M3.1        | 7                                       | 408              |
| M3.2        | 23                                      | 431              |
| M3.3        | 27                                      | 458              |
| M3.4        | 15                                      | 473              |
| M3.5        | 11                                      | 484              |
| Integration | 16 (IncrementalPipelineIntegrationTest) | —                |
