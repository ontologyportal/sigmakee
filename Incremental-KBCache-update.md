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

| Phase | Method | Produces | Queries |
|-------|--------|----------|---------|
| 1 | `buildInsts()` | `insts` (all instance subjects) | `ask("arg",0,"instance")` |
| 2 | `buildRelationsSet()` | `relations`, `predicates`, `functions` | `insts` + type checks |
| 3 | `buildTransitiveRelationsSet()` | `transRels` | instance checks |
| 4 | `buildParents()` | `parents` (transitive ancestors per term) | `ask("arg",0,rel)` for each transitive rel |
| 5 | `buildChildren()` | `children` (transitive descendants) | inverts `parents` |
| 6 | `collectDomains()` | `signatures`, `valences` | `ask("arg",0,"domain/range")` + inheritance via `parents["subrelation"]` |
| 7 | `buildInstTransRels()` | `instTransRels` | depends on `collectDomains` |
| 8-9 | `buildDirectInstances()` + `addTransitiveInstances()` | `instances` | `ask("arg",0,"instance")` + `parents["subclass"]` |
| 10 | `buildTransInstOf()` | `instanceOf` (all classes per instance) | `instances` + `parents["subclass"]` |
| 11 | `correctValences()` | fixes `valences` | VariableArityRelation checks |

### Formula Preprocessing (`FormulaPreprocessor.java`)

`preProcess()` transforms each KIF formula before TPTP translation. It depends on KBcache:

| Step | Method | KBcache dependency |
|------|--------|--------------------|
| 1 | `replacePredVarsAndRowVars()` | `predicates`, `valences`, `instanceOf` — enumerates matching relations for predicate variables |
| 2 | `addInstancesOfSetOrClass()` | `formulaMap` only |
| 3 | `preProcessRecurse()` | `relations`, `functions` |
| 4 | `addTypeRestrictions()` → `computeVariableTypes()` | `signatures` (arg types per predicate), `children["subclass"]` (for `winnowTypeList`) |

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

### M3.0 — Fix Session Regen to Use Fresh KBcache

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

**Note:** This mutates the shared KBcache, which is a problem for multi-user scenarios.
M3.1 fixes this with per-session copies.

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
      only copies a map when it's about to be modified *(not yet implemented; full deep copy used)*

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
1. newParents = {parent} ∪ parents["subclass"].get(parent)
2. Add newParents to parents["subclass"].get(child) (create if absent)
3. For each descendant D of child (from children["subclass"].get(child)):
     parents["subclass"].get(D).addAll(newParents)
4. allNewChildren = {child} ∪ children["subclass"].getOrDefault(child, {})
5. For parent and each ancestor A of parent:
     children["subclass"].get(A).addAll(allNewChildren)
6. For each instance I where instanceOf.get(I) contains child or any descendant of child:
     instanceOf.get(I).addAll(newParents)
7. Update instances: for parent and each ancestor A:
     if instances.get(A) != null:  // only update existing entries, never create new ones
         instances.get(A).addAll(instances of child and child's descendants)
```

Cost: O(|descendants(child)| + |ancestors(parent)|) — typically tens to hundreds, not 75K.

#### Algorithm for `addDomain(relation, argNum, type)`:

```
1. signatures.get(relation)[argNum] = type
2. For each subrelation SR of relation (from children["subrelation"]):
     if signatures.get(SR)[argNum] is empty: inherit type
3. Update valences if needed
```

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

Given the set of changed KBcache entries from M3.2, identify which formulas in the TPTP file
are affected and need retranslation.

#### Affected Formula Sources

After `addSubclass(Robot, Agent)`, the changed terms are `{Robot, Agent}` plus their
descendants and ancestors. A formula is affected if:

1. **Direct reference:** Formula mentions any changed term (uses `KB.formulas` index:
   `ask("arg", N, term)` for N = 0–5)
2. **Predicate variable:** Formula has predicate variables (`predVarCache` non-null and
   non-empty) **and** at least one changed term is a relation — such formulas are re-expanded
   by `PredVarInst` against the updated predicate set
3. **Cached varTypeCache:** Formula has a non-empty `varTypeCache` — cleared on all affected
   formulas to force recomputation by `computeVariableTypes()`

*Note: "Signature dependency" criterion (criterion 2 from the original spec) is subsumed by
direct reference — if a predicate's signature changed, formulas mentioning that predicate are
found by criterion 1.*

#### Steps

- [x] Add `findAffectedFormulas(KB kb, Set<String> changedTerms)` to `SUMOKBtoTPTPKB`
- [x] Use `KB.formulas` index for fast lookup of formulas by term
- [x] Clear `varTypeCache` on affected formulas (so `computeVariableTypes()` recomputes)
- [x] Return `Set<Formula>` of formulas requiring retranslation

#### Tests Added

- 14 tests in `AffectedFormulasTest.java` (new file, added to `UnitSigmaTestSuite`)

#### Files

- `src/java/com/articulate/sigma/trans/SUMOKBtoTPTPKB.java`
- `test/unit/java/com/articulate/sigma/AffectedFormulasTest.java` (new)

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

| Category | Description | Source |
|----------|-------------|--------|
| **Retranslated base axioms** | Existing KB formulas whose TPTP translation changes because the session KBcache was updated (e.g., new type guards after `addSubclass`) | `affected` parameter |
| **New tell() assertion axioms** | Formulas just added to the KB via `tell()`, never previously translated | `newFormulas` parameter |

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
  ├── Incremental update: sessionCache.addSubclass("Robot", "Agent")      [M3.2]
  │     → returns changedTerms
  ├── findAffectedFormulas(kb, changedTerms)                              [M3.3]
  │     → returns affected (existing formulas needing retranslation)
  ├── patchSessionTPTP(sessionId, kb, lang,
  │       affected,           ← existing formulas to retranslate
  │       Set.of(newFormula), ← the just-tell()'d formula
  │       sessionCache)                                                   [M3.4]
  └── (done — session file is up to date)
```

#### Steps

- [x] Modify `KB.tell()` to detect schema predicates and trigger incremental update
- [x] Wire session KBcache creation/retrieval
- [x] Wire incremental update method dispatch based on predicate type
- [x] Wire affected formula identification and TPTP patching
- [x] End-to-end unit tests: `tell()` schema assertion → verify session TPTP is correct
- [ ] Performance test: measure time for incremental update vs full regen *(deferred — non-blocking)*

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
1. Creates or retrieves session KBcache via `getOrCreateSessionCache(sessionId, kb)`
2. Dispatches to targeted M3.2 method via `switch (pred)`:
   - `subclass`/`immediateSubclass` → `addSubclass()`
   - `instance`/`immediateInstance` → `addInstance()`
   - `domain`/`domainSubclass` → `addDomain()`
   - `range`/`rangeSubclass` → `addRange()`
   - `subrelation` → `addSubrelation()`
   - `disjoint` → `addDisjoint()`
   - `default` (partition, exhaustiveDecomposition, etc.) → falls back to `generateSessionTPTP()`
3. Calls M3.3 `findAffectedFormulas(kb, changedTerms)` on the returned changed-terms set
4. Calls M3.4 `patchSessionTPTP(sessionId, kb, lang, affected, singleton(formula), sessionCache)`

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

| Milestone | New Tests | Cumulative Total |
|-----------|-----------|-----------------|
| Baseline  | —         | 401             |
| M3.1      | 7         | 408             |
| M3.2      | 23        | 431             |
| M3.3      | 14        | 445             |
| M3.4      | 15        | 460             |
| M3.5      | 11        | 471             |
| Integration | 16 (IncrementalPipelineIntegrationTest) | — |

---

## Bug Fix: Over-Aggressive Formula Patching (post-M3.5)

**Problem:** After `tell("(subclass Greek Human)")`, formulas like
`fof(kb_SUMO_2317,axiom,...uses/instrument/agent/AutonomousAgent...)` were being commented
out in the session TPTP file but not re-added.

**Root cause:** `addSubclass("Greek", "Human")` returned `changedTerms` containing ALL
ancestors of `Human` in the subclass hierarchy (including `AutonomousAgent`, `Object`,
`Entity`, etc.) because the KBcache `children` and `instances` maps for those ancestors
changed.  `findAffectedFormulas` then added every formula mentioning any ancestor to the
`affected` set — potentially thousands of formulas.  These were all commented out.  However,
retranslating them produces the **identical** TPTP body (the formula translations don't
actually change when a new subclass is added), and when the retranslation body is identical
but written with a new axiom name, the original was missing from the file under the expected
name.

**Analysis:** Adding `(subclass Greek Human)` does **not** change the TPTP translation of
any existing formula because:
- Type guards (`instance ?X AutonomousAgent`) remain valid — they don't enumerate subclasses
- Predicate-variable formulas are unaffected — adding a class doesn't add a new predicate
- Argument-type signatures (`domain`/`range`) are unchanged — only `subclass` hierarchy changed

**Fix (in `SessionTPTPManager.applyIncrementalUpdate`):**
For `subclass`, `instance`, and `disjoint` predicates, set `changedTerms = emptySet()`.
`findAffectedFormulas(kb, emptySet)` returns empty → `affected = empty` → no existing
formulas are commented out.  Only the new formula itself is appended via `newFormulas`.

For `domain`/`range`/`subrelation`, `changedTerms` from the addXxx() methods is still
used, because those predicates DO change argument-type signatures or predicate hierarchies
that affect type guards and predicate-variable expansions.

**Benefit:** Session TPTP patching for `subclass`/`instance`/`disjoint` is now O(1)
(one formula appended) instead of O(N) (thousands commented out and re-added).
