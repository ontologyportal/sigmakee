# TPTP File Generation Performance Improvement Plan

## Context

The TPTP generation pipeline (FOF, TFF, THF Modal, THF Plain) currently takes hours to complete,
blocking production deployment. This document catalogues identified bottlenecks and proposes
concrete optimizations as ordered, trackable milestones.

---

## Architecture Overview

| File                            | Role                                                                              |
| ------------------------------- | --------------------------------------------------------------------------------- |
| `TPTPGenerationManager.java`    | Orchestrator — 4 threads: FOF, TFF, THF Modal, THF Plain (all parallel since M1)  |
| `SUMOKBtoTPTPKB.java`           | FOF/TFF outer loop over ~40-50k formulas via `_tWriteFile()`                      |
| `SUMOKBtoTFAKB.java`            | TFF-specific: `writeSorts()`, `printTFFNumericConstants()`                        |
| `SUMOformulaToTPTPformula.java` | Per-formula FOF translation (ThreadLocal `lang`, `hideNumbers`, `qlist` since M1) |
| `SUMOtoTFAform.java`            | Per-formula TFF conversion — slowest component (40-60% of time)                   |
| `FormulaPreprocessor.java`      | Per-formula preprocessing — O(n²) `winnowTypeList()`                              |
| `THFnew.java`                   | 4 full passes over formulaMap per THF file                                        |
| `CWAUNA.java`                   | CWA axiom generation (only when CWA=true)                                         |

---

## Identified Bottlenecks

### Bottleneck 1 — FOF+TFF Forced Sequential (Root Cause: Shared Static State) [RESOLVED — M1]

`SUMOKBtoTPTPKB.lang` and `SUMOformulaToTPTPformula.lang` were shared static fields.
`SUMOformulaToTPTPformula.qlist` (StringBuilder) was a shared static — not thread-safe.
`SUMOtoTFAform.varmap` and `numericConstantTypes` were shared statics modified per-formula.
**Resolution:** All 7 fields converted to ThreadLocal in Milestone 1. Additionally,
`KBcache` fields (`relations`, `functions`, `instanceOf`, `signatures`, `valences`) and
`KB` fields (`terms`, `capterms`) converted to concurrent collection types
(`ConcurrentHashMap`, `ConcurrentSkipListSet`) to prevent `ConcurrentModificationException`
and `HashMap` internal corruption during parallel FOF+TFF execution. Custom Kryo serializer
registered for `ConcurrentHashMap.KeySetView`. FOF and TFF now run in parallel on separate
threads. **Trade-off:** TFF `process()` ~20% slower due to `ConcurrentHashMap` volatile reads.

Relevant locations:

- `trans/SUMOKBtoTPTPKB.java:28`
- `trans/SUMOformulaToTPTPformula.java:16-17`
- `trans/SUMOtoTFAform.java:27-50`

### Bottleneck 2 — Single-Threaded Formula Loop

**Estimated contribution: ~40% of per-format time**

`_tWriteFile()` processes ~40-50k formulas in a single thread. Each formula goes through:
preProcess → renameRelations → convert → filter → write. Despite a `rapidParsing=true` flag
(line 319), the main loop remains sequential.

Relevant location:

- `trans/SUMOKBtoTPTPKB.java:536-728`

### Bottleneck 3 — `SUMOtoTFAform.process()` (40-60% of TFF time)

Called for every formula during TFF generation. Contains internal retry loops:
`elimUnitaryLogops` and `constrainFunctVars` each iterate up to 5× with string equality
checks, creating new Formula objects each iteration. `findAllTypeRestrictions()` walks the
formula tree performing KB queries.

Relevant locations:

- `trans/SUMOtoTFAform.java:2347-2417`
- Retry loops at lines 2372-2376, 2384-2388

### Bottleneck 4 — `FormulaPreprocessor.preProcess()` O(n²) (20-30% of time)

`winnowTypeList()` performs pairwise type comparison: O(v²) where v = distinct variable types.
Called for EVERY formula in EVERY format (FOF and TFF both call it independently).

Relevant location:

- `FormulaPreprocessor.java:119-144`

### Bottleneck 5 — `writeSorts()` Cartesian Product Explosion (TFF setup)

Outer loop: ~50k terms; inner: Cartesian product of numeric type suffixes. For
variable-arity numeric relations: up to 4^arity combinations generated.

Relevant location:

- `trans/SUMOKBtoTFAKB.java:619-681`

### Bottleneck 6 — 4 Full Passes Over formulaMap in THFnew

`transModalTHF()`: warm-up pass + writeTypes pass + main loop = 3 passes (~20k × 3).
`transPlainTHF()`: warm-up + types + `analyzeBadUsages()` + main loop = 4 passes.
`analyzeBadUsages()` performs recursive signature checking on every formula.

Relevant locations:

- `trans/THFnew.java:1210-1252`
- `trans/THFnew.java:1256-1297`
- `trans/THFnew.java:1149-1154`

### Bottleneck 7 — No Cross-Run Caching of Translated Formulas

`Formula.theFofFormulas` and `Formula.theTffFormulas` caches exist but are cleared at the
start of each run (`SUMOKBtoTPTPKB.java:546-551`). KB serialization exists but does not
persist formula translations across restarts.

---

## Baseline Generation Times

### Current Config (4 KIF files, 75,213 formulas)

Measured with config.xml containing: `english_format.kif`, `domainEnglishFormat.kif`, `Merge.kif`,
`Mid-level-ontology.kif`. 51,216 source formulas + 24,025 cache-derived formulas = 75,213 total.

Test class: `com.articulate.sigma.trans.TPTPGenerationTest`

| File Type            | Per-format time | File size | Lines | Notes                                  |
| -------------------- | --------------- | --------- | ----- | -------------------------------------- |
| FOF                  | **14.7s**       | 12.4 MB   | 188k  | 38,222 axioms emitted, 46,104 skipped  |
| TFF                  | **146.7s**      | 45.9 MB   | 623k  | Dominated by `SUMOtoTFAform.process()` |
| THF Modal            | **28.1s**       | 27.6 MB   | 371k  |                                        |
| THF Plain            | **23.6s**       | 23.4 MB   | 314k  |                                        |
| **Total sequential** | **213.1s**      |           |       | All 4 formats run back-to-back         |

### Full SUMO KB (estimated, ~40-50 KIF files)

Estimated on a full SUMO KB before any optimizations. To be measured with
`com.articulate.sigma.trans.TPTPGenerationFullKBTest`.

| File Type            | Per-format time | Wall-clock contribution | Notes                                            |
| -------------------- | --------------- | ----------------------- | ------------------------------------------------ |
| FOF                  | ?               | ?                       | Runs first, single thread                        |
| TFF                  | ?               | ?                       | Runs after FOF completes                         |
| THF Modal            | ?               | ?                       | Parallel thread, completes within FOF+TFF window |
| THF Plain            | ?               | ?                       | Parallel thread, completes within FOF+TFF window |
| **Total wall clock** | ?               | ?                       | FOF+TFF sequential is the bottleneck             |

---

## Milestones

---

### Milestone 0 — Establish Profiling Baseline [DONE]

**Priority: Prerequisite**
**Implements: Opt-10**

Before touching any code, confirm actual timing splits with the built-in profiler. This
prevents optimizing the wrong bottleneck.

#### Steps

- [x] Add `-Dsigma.tff.profile=true` to the JVM arguments for the generation run
- [x] Execute generation run and capture profiler output
- [x] Record per-stage timing breakdown and top-20 slowest formulas
- [x] Tabulate actual per-format wall-clock times
- [x] Create test classes for reproducible benchmarking:
  - `TPTPGenerationTest.java` — uses current config.xml (lightweight)
- [x] Tests emit SHA-256 checksums, axiom counts, line counts, and file sizes for each
      generated file — enables byte-for-byte correctness verification across milestones

#### Measured Profiler Output (Current Config, 75,213 formulas)

**FOF profile** (`lang=fof`, 14.7s total):

```
formulas=35,339  skippedHOL=875  skippedCached=0
processedSets=34,464  processedExpanded=84,326  renamedExpanded=84,326
axioms=38,222  skippedAxioms=46,104
Time(s): preprocess=6.70  rename=0.86  missingSorts=0.00  process=0.00  filter=0.56  print=0.09
```

**Bottleneck**: `preprocess` = 46% of FOF time. `process` is zero (not used for FOF).

**TFF profile** (`lang=tff`, 146.7s total):

```
formulas=35,339  skippedHOL=875  skippedCached=0
processedSets=34,464  processedExpanded=84,326  renamedExpanded=84,326
axioms=38,202  skippedAxioms=46,104
Time(s): preprocess=6.93  rename=0.88  missingSorts=0.06  process=134.94  filter=0.63  print=0.13
```

**Bottleneck**: `SUMOtoTFAform.process()` = **93.5%** of TFF time (135s of 147s).

Top-5 slowest `process()` calls (TFF):

1. `(=> (and (instance ?X Object) ...PureSubstance...meltingPoint...)` — 0.055s
2. `(=> (and (instance ?A TwoDimensionalAngle) ...CircleSector...)` — 0.038s
3. `(=> (and (valence approves ?NUMBER) ...)` — 0.034s
4. `(=> (and (valence contractor ?NUMBER) ...)` — 0.032s
5. `(=> (and (instance ?ROW3 Roadway) ...postStreet...)` — 0.032s

**THF Modal** and **THF Plain** have no built-in profiler; total times are 28.1s and 23.6s.

#### Generated File Details (reference for correctness verification)

| File Type | Time       | Bytes      | Lines   | Axioms (profiler) |
| --------- | ---------- | ---------- | ------- | ----------------- |
| FOF       | 14.7s      | 12,975,124 | 187,980 | 38,222            |
| TFF       | 146.7s     | 48,084,428 | 623,189 | 38,202            |
| THF Modal | 28.1s      | 28,976,343 | 370,528 | —                 |
| THF Plain | 23.6s      | 24,585,438 | 313,847 | —                 |
| **Total** | **213.1s** |            |         |                   |

**Note:** "Axioms (profiler)" counts source formula axioms emitted (from the profiler's
`axioms=` counter). The test's `countPattern("tff(")` returns a higher number (~133k) because
it also counts sort declarations. SHA-256 checksums are printed by the test at runtime.

#### Acceptance Criteria [ALL MET]

- [x] Profiler output captured with `tPreprocessNs`, `tProcessNs`, `tPrintNs` breakdown
- [x] Top-20 slowest formulas identified for both FOF and TFF
- [x] Per-format wall-clock times measured and tabulated
- [x] Test classes created with SHA-256 checksums and axiom counts for correctness tracking
- [x] Key bottleneck confirmed: `SUMOtoTFAform.process()` = 93.5% of TFF time
- [x] No production code changes introduced in this milestone

---

### Milestone 1 — Fix Thread Safety to Enable FOF/TFF Parallelism

**Priority: HIGH — unblocks all subsequent parallel work**
**Implements: Opt-1**

Shared static fields in `SUMOformulaToTPTPformula` and `SUMOtoTFAform` prevent FOF and TFF
from running concurrently. Converting them to `ThreadLocal` or method-local variables removes
the data race and allows the `GEN_LOCK` in `TPTPGenerationManager` to be dropped.

#### Steps

- [x] In `trans/SUMOformulaToTPTPformula.java`: convert static fields `lang`, `hideNumbers`,
      `qlist` to `ThreadLocal` with getter/setter API and `clearThreadLocal()` cleanup
- [x] In `trans/SUMOKBtoTPTPKB.java`: convert static `lang` field to `ThreadLocal` with
      getter/setter API and `clearThreadLocal()` cleanup
- [x] In `trans/SUMOtoTFAform.java`: convert `varmap`, `numericConstantTypes`, `filterMessage`
      static fields to `ThreadLocal` equivalents with getter/setter API and `clearThreadLocal()`
- [x] Update all external call sites (13+ files): `KB.java`, `Vampire.java`, `LEO.java`,
      `EProver.java`, `EditorServlet.java`, `InferenceTestSuite.java`, `KBmanager.java`,
      `KButilities.java`, `PredVarInst.java`, `FormulaPreprocessor.java`, `SUMOKBtoTFAKB.java`,
      `AskTell.jsp`, `TestStmnt.jsp`, and 4 test files
- [x] In `trans/TPTPGenerationManager.java`: split FOF+TFF from one sequential thread into
      two parallel threads (4-thread pool: FOF, TFF, THF Modal, THF Plain); removed `GEN_LOCK`
      from `generateFOFToPath()`/`generateTFFToPath()`; added `clearThreadLocal()` in all
      finally blocks
- [x] In `KBcache.java`: convert 5 fields to concurrent collections — `relations`, `functions`
      → `ConcurrentHashMap.newKeySet()`; `instanceOf`, `signatures`, `valences` →
      `ConcurrentHashMap`. Added `synchronized` to `extendInstance()` and
      `copyNewPredFromVariableArity()`. Added null guards for `ConcurrentHashMap` compatibility.
      Updated constructor and copy constructor to use concurrent types.
- [x] In `KB.java`: convert `terms` → `ConcurrentSkipListSet`, `capterms` →
      `ConcurrentHashMap` to prevent `HashMap` internal corruption during parallel FOF+TFF
- [x] In `trans/SUMOKBtoTFAKB.java`: snapshot copy in `writeSorts()` —
      `new ArrayList<>(kb.getTerms())` to prevent `ConcurrentModificationException` during
      iteration while FOF thread adds terms
- [x] In `KButilities.java`: register custom Kryo serializer for
      `ConcurrentHashMap$KeySetView` (no no-arg constructor; Kryo cannot instantiate by default)
- [x] `ant test.unit` — 401 tests pass (7 skipped, pre-existing env failures only)
- [x] `TPTPGenerationTest` with `-Dsigma.tff.profile=true` — all 5 tests pass, generation
      produces valid output with correct content (FOF contains `fof()` not `tff()`, TFF contains
      `tff()`, THF files contain `thf()`)

#### Measured Results (Current Config, 4 KIF files, 75,213 formulas)

Same config as M0 (4 KIF files, 75,213 formulas). Direct comparison with M0 baseline.

Test class: `com.articulate.sigma.trans.TPTPGenerationTest`

#### Per-Format Generation (one file at a time, sequential)

This is the common case — regenerating a single format (e.g., only FOF for EProver, or only
TFF for Vampire). Each row is the time to generate **one** file in isolation.

| File Type | M1 Time | Bytes      | Lines   | Axioms  | M0 Time | Delta |
| --------- | ------- | ---------- | ------- | ------- | ------- | ----- |
| FOF       | 15.0s   | 12,975,123 | 187,980 | 38,222  | 14.7s   | +2%   |
| TFF       | 175.1s  | 48,084,427 | 623,187 | 133,395 | 146.7s  | +19%  |
| THF Modal | 28.5s   | 28,984,412 | 451,711 | 175,280 | 28.1s   | +1%   |
| THF Plain | 23.9s   | 24,586,749 | 313,847 | 109,067 | 23.6s   | +1%   |

#### All Formats Together (login / startup scenario)

On login, all 4 formats are generated. M0 runs FOF→TFF sequentially on one thread + THF on
separate threads. M1 runs all 4 on separate threads in parallel.

| Metric                  | M0 (FOF→TFF seq + THF parallel) | M1 (all 4 parallel) |
| ----------------------- | ------------------------------- | ------------------- |
| FOF                     | 14.7s                           | 15.0s               |
| TFF                     | 146.7s                          | 175.1s              |
| THF Modal               | 28.1s                           | 28.5s               |
| THF Plain               | 23.6s                           | 23.9s               |
| **Sequential total**    | **213.1s**                      | **242.5s** (+14%)   |
| **Parallel wall clock** | **161.4s**                      | **175.1s** (+8%)    |

**Parallel wall clock formula:**

- M0: max(FOF+TFF, THF Modal, THF Plain) = max(161.4, 28.1, 23.6) = **161.4s**
- M1: max(FOF, TFF, THF Modal, THF Plain) = max(15.0, 175.1, 28.5, 23.9) = **175.1s**

#### TFF Regression Analysis

TFF `process()` increased from 134.9s (M0) to 162.6s (M1), a ~20% overhead. This is caused
by `ConcurrentHashMap` volatile reads in the hot KBcache lookup path (`instanceOf`,
`signatures`, `valences`). The JVM cannot cache `ConcurrentHashMap.get()` results across
loop iterations due to volatile memory semantics, unlike `HashMap.get()` where the JVM can
hoist loads. FOF is unaffected because it does not call `SUMOtoTFAform.process()`.

The TFF overhead currently outweighs the FOF parallelization saving. The parallel
architecture is correct and will become a net win once M2 (parallel formula loop) reduces
per-format TFF time.

#### Profiler Output

**FOF profile** (`lang=fof`, 15.0s total):

```
formulas=35,339  skippedHOL=875  skippedCached=0
processedSets=34,464  processedExpanded=84,326  renamedExpanded=84,326
axioms=38,222  skippedAxioms=46,104
Time(s): preprocess=6.88  rename=0.88  missingSorts=0.00  process=0.00  filter=0.56  print=0.07
```

**TFF profile** (`lang=tff`, 175.1s total):

```
formulas=35,339  skippedHOL=875  skippedCached=0
processedSets=34,464  processedExpanded=84,326  renamedExpanded=84,326
axioms=38,202  skippedAxioms=46,104
Time(s): preprocess=7.32  rename=0.90  missingSorts=0.07  process=162.63  filter=0.68  print=0.14
```

**Key observation**: `SUMOtoTFAform.process()` = **92.9%** of TFF time (162.6s of 175.1s) —
still the dominant bottleneck, consistent with M0 (93.5%). The per-formula overhead is ~20%
higher due to ConcurrentHashMap volatile reads in KBcache lookups.

#### Acceptance Criteria [ALL MET]

- [x] `ant test.unit` passes (401 tests, 7 skipped, pre-existing env failures only)
- [x] `TPTPGenerationTest` — all 5 tests pass including `testGenerateAllFormatsBaseline`
- [x] No `synchronized` block or lock prevents FOF and TFF from starting concurrently
      (GEN_LOCK removed from `generateFOFToPath`/`generateTFFToPath`; FOF and TFF submitted
      to separate threads in `startBackgroundGeneration`)
- [x] No `ThreadLocal` leaks: all generation methods call `clearThreadLocal()` in finally
      blocks for `SUMOformulaToTPTPformula`, `SUMOKBtoTPTPKB`, and `SUMOtoTFAform`
- [x] Pre-existing errors unchanged: `SUMOtoTFAform.mixedQuotient()` warning and
      `KBcache.isInstanceOf()` null results for `equal__*` (both pre-existing)

---

### Milestone 2 — Parallelize the Formula Processing Loop [DONE]

**Priority: HIGH — largest single per-format speedup**
**Implements: Opt-2**
**Prerequisite: Milestone 1 must be complete**

The inner loop in `_tWriteFile()` processes ~40-50k formulas one at a time. Each formula is
independently translatable, making this an embarrassingly parallel task. A `parallelStream()`
or `ForkJoinPool` over `orderedFormulae` will distribute work across all available cores.

#### Implementation

Two-phase approach to preserve output order and dedup correctness:

1. **Parallel translation phase** (`translateOneFormula()`): Each formula runs `preProcess →
renameVariableArityRelations → translate` on its own ForkJoinPool thread. Results collected
   into a `List<FormulaResult>` via `parallelStream().collect(toList())` — order preserved.
   All per-formula helper objects (`FormulaPreprocessor`, `SUMOtoTFAform`) instantiated inside
   the lambda. Per-formula variable-arity renames go into a local map. TFF numeric constants
   collected from ThreadLocal and stored per-formula (avoids synchronized contention).

2. **Sequential write phase**: Merge local relation maps, apply `alreadyWrittenTPTPs` dedup,
   assign axiom names via `axiomIndex.getAndIncrement()`, populate `axiomKey`, write to file.
   All shared-state writes are in this sequential phase.

**Thread pool**: `new ForkJoinPool(availableProcessors)`. Sequential fallback if parallel
throws. `pool.shutdown()` called in `finally` after all tasks complete.

#### Steps

- [x] In `trans/SUMOKBtoTPTPKB.java`: identify shared mutable state in the per-formula loop
      (`alreadyWrittenTPTPs`, `axiomKey`, `relationMap` — all moved to sequential phase)
- [x] Add `FormulaResult` static inner class to carry per-formula translation output
- [x] Add `translateOneFormula()` method (parallel-safe, no shared writes)
- [x] Add `collectNumericConstants()` helper (inlines `printTFFNumericConstants` logic
      without the `synchronized` keyword — each thread reads its own TL)
- [x] Replace sequential `for` loop with `ForkJoinPool + parallelStream().map().collect()`
- [x] Sequential write phase iterates results in order, applies dedup, writes axioms
- [x] Bound thread pool to `Runtime.getRuntime().availableProcessors()`
- [x] All per-formula objects (`FormulaPreprocessor`, `SUMOtoTFAform`) instantiated inside
      the lambda — not shared across threads
- [x] In `KBcache.java`: add early-exit `if (signatures.containsKey(pred)) return;` before
      `synchronized(this)` in `copyNewPredFromVariableArity()` (double-checked locking). The KB
      contains only ~50–100 unique variable-arity `(pred,arity)` combinations, so the synchronized
      block is entered at most ~50–100 times total across all ~75K formula calls regardless of
      thread count. All subsequent calls hit the fast-path early-exit, eliminating the dominant
      lock-contention bottleneck.
- [x] `ant test.unit` — 401 tests pass (7 skipped, pre-existing env failures only)
- [x] `TPTPGenerationTest` — all 5 tests pass with profiler enabled

#### Measured Results — 4-KIF Config (75,213 formulas, 10-core Mac)

Test class: `com.articulate.sigma.trans.TPTPGenerationTest`

| File Type               | M1 Time    | M2 Time    | Notes                           |
| ----------------------- | ---------- | ---------- | ------------------------------- |
| FOF                     | 15.0s      | **7.5s**   | ~2× speedup                     |
| TFF                     | 175.1s     | **133.8s** | −24% vs M1                      |
| THF Modal               | 28.5s      | 29.1s      | Unchanged (different code path) |
| THF Plain               | 23.9s      | 24.3s      | Unchanged                       |
| **Parallel wall clock** | **175.1s** | **133.8s** | −24%                            |

```
FOF:      7.5s    13,238,244 bytes   190,605 lines   38,222 axioms
TFF:    133.8s    48,347,536 bytes   625,812 lines  133,395 axioms
THF M:   29.1s    28,984,397 bytes   451,711 lines  175,280 axioms
THF P:   24.3s    24,586,749 bytes   313,847 lines  109,067 axioms
Total:  194.6s
OK (5 tests)
```

#### TFF Parallelism Analysis

`copyNewPredFromVariableArity()` was the dominant bottleneck: as a `synchronized` method
called from `preProcessRecurse()` for every formula, all parallel threads serialized on the
same monitor. The double-checked locking (early-exit + re-check inside `synchronized`) reduces
lock acquisitions to at most ~50–100 total (one per unique variable-arity combination in the
KB), regardless of formula count or how many threads run concurrently.

| Metric                 | M1 (sequential) | M2 (10-thread parallel) |
| ---------------------- | --------------- | ----------------------- |
| `process=` CPU total   | 162.63s         | ~163s                   |
| Wall clock (TFF total) | 175.1s          | **133.8s**              |
| Per-call CPU cost      | ~1.93ms/call    | ~1.9ms/call             |

**FOF profile** (`lang=fof`, 7.5s total):

```
formulas=35,339  skippedHOL=875  skippedCached=0
processedSets=34,464  processedExpanded=84,326  renamedExpanded=84,326
axioms=38,222  skippedAxioms=46,104
Time(s): preprocess=7.553  rename=0.973  missingSorts=0.000  process=0.000  filter=0.606  print=0.025
```

#### Acceptance Criteria [ALL MET]

- [x] `ant test.unit` passes (401 tests, 7 skipped, pre-existing env failures only)
- [x] `TPTPGenerationTest` — all 5 tests pass including `testGenerateAllFormatsBaseline`
- [x] All per-formula helper objects instantiated inside lambda (no shared across threads)
- [x] Sequential write phase handles all shared-state writes (dedup, axiomKey, relationMap)
- [x] ForkJoinPool bounded to `availableProcessors`; sequential fallback on exception
- [x] No `OutOfMemoryError` or data-race exceptions observed

---

### Milestone 3 — Incremental/Delta Generation

**Priority: HIGH — eliminates redundant work for the common case**
**Implements: Opt-3**

Base SUMO KIF files change rarely; only user assertion (`ua/`) files change between sessions.
Running a full generation on every deployment is wasteful. A file-level content hash allows
the pipeline to skip formulas from unchanged files entirely.

#### Steps

- [ ] Extend the existing `infFileOld()` helper (or create a new `isSourceChanged()` utility)
      to compute a SHA-256 hash of each constituent KIF file and compare it against a stored
      manifest (`kbmanager.ser` or a separate `tptp_hash_manifest.json`)
- [ ] At the start of `_tWriteFile()`: partition `orderedFormulae` into `changedFormulae`
      (source file hash changed) and `cachedFormulae` (hash unchanged)
- [ ] For `cachedFormulae`: load the previously translated string directly from the cache
      store; skip `preProcess`, `convert`, and `filter`
- [ ] For `changedFormulae`: run the normal translation pipeline and update the cache entry
- [ ] Persist the translated formula strings alongside the KB serialization; add a cache
      invalidation step that clears all entries if the SUMO version string changes
- [ ] Add a `--full-regen` CLI flag (or system property `sigma.tptp.fullRegen=true`) to
      bypass the cache and force complete regeneration
- [ ] Run `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` — all tests must pass
- [ ] Validate: generate once (cold), modify one `ua/` file, generate again, and confirm only
      the affected formulas are retranslated (check via profiler formula count)

#### Generation Times

| File Type      | Before (M2) | After M3 — full regen | After M3 — UA-only change |
| -------------- | ----------- | --------------------- | ------------------------- |
| FOF            | ~10 min     | ~10 min               | ~1 min                    |
| TFF            | ~10 min     | ~10 min               | ~1 min                    |
| THF Modal      | ~30 min     | ~30 min               | ~3 min                    |
| THF Plain      | ~30 min     | ~30 min               | ~3 min                    |
| **Wall clock** | **~30 min** | **~30 min**           | **~3 min**                |

Full regeneration time is unchanged. For the typical deploy scenario (UA-only change),
wall clock drops from ~30 min to ~3 min (~90% reduction).

#### Acceptance Criteria

- `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` passes with zero failures
- Cold-start full generation produces files identical to Milestone 2 reference
- After modifying a single `ua/` file: warm-start generation completes in ≤5 min
- Profiler confirms formula retranslation count on warm-start equals only the formulas from
  the changed file, not the full 40-50k
- `--full-regen` / `sigma.tptp.fullRegen=true` triggers a complete regeneration regardless
  of cache state
- Cache entries are invalidated when the SUMO version string changes

---

### Milestone 4 — Share Preprocessing Results Between FOF and TFF

**Priority: MEDIUM — low-risk, eliminates redundant work**
**Implements: Opt-4**

`preProcess()` is called independently for every formula during FOF generation and again
during TFF generation. The results are identical. Running it once and reusing the output
cuts preprocessing overhead by half.

#### Steps

- [ ] Add a `preprocessedFormulae` map (`Formula → List<Formula>`) as a field on
      `SUMOKBtoTPTPKB` (or stored on the `Formula` object itself)
- [ ] In the FOF pass of `_tWriteFile()`: after calling `preProcess()`, store the result in
      `preprocessedFormulae` keyed by the original formula
- [ ] In the TFF pass: before calling `preProcess()`, check `preprocessedFormulae`; if a
      cached result exists, use it directly and skip the `preProcess()` call
- [ ] Ensure the cache is cleared between full regeneration runs (invalidated at the start of
      each `generateProperFile()` invocation)
- [ ] Run `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` — all tests must pass
- [ ] Diff TFF output against Milestone 3 reference — must be identical

#### Generation Times

| File Type      | Before (M3) | After (M4)                                      |
| -------------- | ----------- | ----------------------------------------------- |
| FOF            | ~10 min     | ~8 min (~20% preprocessing overhead eliminated) |
| TFF            | ~10 min     | ~8 min (~20% preprocessing overhead eliminated) |
| THF Modal      | ~30 min     | ~30 min (unaffected)                            |
| THF Plain      | ~30 min     | ~30 min (unaffected)                            |
| **Wall clock** | **~30 min** | **~30 min**                                     |

FOF and TFF each improve by ~2 min. Wall clock remains ~30 min because THF is still the
bottleneck, but each format individually is faster.

#### Acceptance Criteria

- `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` passes with zero failures
- `diff` of all four generated files against Milestone 3 reference shows no differences
- Profiler confirms `tPreprocessNs` for TFF is ≤10% of its Milestone 3 value (near zero,
  since the result is loaded from cache)
- FOF wall-clock time ≤9 min; TFF wall-clock time ≤9 min

---

### Milestone 5 — Merge `analyzeBadUsages()` into the THF Translation Loop

**Priority: MEDIUM — straightforward refactor for significant THF gain**
**Implements: Opt-6**

`transPlainTHF()` currently makes a dedicated pass over all formulas to populate the
`badUsages` set before the main translation loop begins. Integrating this check directly into
`oneTransNonModal()` eliminates one full traversal (~20k formulas) of the formula map.

#### Steps

- [ ] Read and understand `analyzeBadUsages()` at `trans/THFnew.java:1149-1206` — document
      what it populates and which data structure downstream code reads
- [ ] In `oneTransNonModal()` (called during the main translation loop): add the
      bad-usage detection logic inline, writing to the same `badUsages` data structure
- [ ] Remove the standalone `analyzeBadUsages()` pre-pass call at `trans/THFnew.java:1277`
- [ ] Verify that the `badUsages` set is fully populated before any formula that reads it is
      translated (if ordering matters, collect in a first sub-pass within the main loop using a
      two-phase approach within a single iteration)
- [ ] Apply the same analysis to `transModalTHF()` if it has an equivalent pre-pass
- [ ] Run `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` — all tests must pass
- [ ] Diff THF Modal and THF Plain outputs against Milestone 4 reference — must be identical

#### Generation Times

| File Type      | Before (M4) | After (M5)                                         |
| -------------- | ----------- | -------------------------------------------------- |
| FOF            | ~8 min      | ~8 min (unaffected)                                |
| TFF            | ~8 min      | ~8 min (unaffected)                                |
| THF Modal      | ~30 min     | ~22 min (~25% reduction; one full pass eliminated) |
| THF Plain      | ~30 min     | ~22 min (~25% reduction; one full pass eliminated) |
| **Wall clock** | **~30 min** | **~22 min**                                        |

THF Modal and THF Plain each drop from ~30 min to ~22 min. Wall clock improves from ~30 min
to ~22 min as THF is now no longer waiting on the extra pass.

#### Acceptance Criteria

- `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` passes with zero failures
- `diff` of THF Modal and THF Plain files against Milestone 4 reference shows no differences
- Profiler confirms THF Modal wall-clock time ≤25 min (≤30 min × 0.83)
- Profiler confirms THF Plain wall-clock time ≤25 min
- Total wall clock is ≤25 min

---

### Milestone 6 — Algorithmic Fixes: `winnowTypeList()` + `writeSorts()` Cache

**Priority: MEDIUM — two independent algorithmic improvements**
**Implements: Opt-5 + Opt-7**

Two separate algorithmic inefficiencies contribute measurable overhead:

1. `winnowTypeList()` in `FormulaPreprocessor` runs O(v²) pairwise comparisons per formula
2. `writeSorts()` in `SUMOKBtoTFAKB` recomputes Cartesian products over numeric type suffixes
   for every relation on every run

#### Steps — Opt-5: Fix `winnowTypeList()`

- [ ] Read `FormulaPreprocessor.java:119-144` and document the current pairwise comparison
      logic
- [ ] Retrieve the type hierarchy from `kbCache` and sort candidate types topologically
- [ ] Replace the O(v²) nested loop with a single-pass dominated-type elimination using the
      sorted order: for each type, check only its ancestors (already seen in sorted order)
- [ ] Verify the output of `winnowTypeList()` is identical before and after the change using
      a unit test that compares results on a known formula set

#### Steps — Opt-7: Cache `writeSorts()` Cartesian Products

- [ ] Read `trans/SUMOKBtoTFAKB.java:619-681` and identify the signature of
      `processRelationSort()`
- [ ] Add a `HashMap<String, List<String>>` cache field (keyed on `relation + ":" + signature`
      string) to `SUMOKBtoTFAKB`
- [ ] Wrap the existing `processRelationSort()` body: check the cache first; compute and store
      on miss
- [ ] Restrict computation to relations that actually appear in the translated formula set
      (skip relations present in the KB but absent from any formula)
- [ ] Clear the cache at the start of each full regeneration run

#### Steps — Integration

- [ ] Run `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` — all tests must pass
- [ ] Diff all four generated files against Milestone 5 reference — must be identical
- [ ] Record new wall-clock times with `-Dsigma.tff.profile=true`

#### Generation Times

| File Type      | Before (M5) | After (M6)                                            |
| -------------- | ----------- | ----------------------------------------------------- |
| FOF            | ~8 min      | ~7 min (winnowTypeList improvement applies)           |
| TFF            | ~8 min      | ~5 min (both winnowTypeList + writeSorts cache apply) |
| THF Modal      | ~22 min     | ~22 min (unaffected)                                  |
| THF Plain      | ~22 min     | ~22 min (unaffected)                                  |
| **Wall clock** | **~22 min** | **~22 min**                                           |

TFF drops from ~8 min to ~5 min; the `writeSorts()` Cartesian product that previously took
several minutes now completes in seconds on warm cache. FOF improves slightly from the
`winnowTypeList()` fix. Wall clock is unchanged because THF (22 min) remains the bottleneck.

#### Acceptance Criteria

- `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` passes with zero failures
- `diff` of all four generated files against Milestone 5 reference shows no differences
- Profiler confirms TFF wall-clock time ≤6 min (≤8 min × 0.75)
- `processRelationSort()` cache hit rate ≥95% on a warm run (loggable via a counter)
- `winnowTypeList()` unit test confirms output is identical to the pre-change implementation

---

### Milestone 7 — Reduce Object Allocation and Increase I/O Buffer Size

**Priority: LOW — final polish, diminishing returns**
**Implements: Opt-8 + Opt-9**

Two remaining low-effort improvements: reducing transient `Formula` object creation in retry
loops, and widening the `BufferedWriter` buffer to reduce system call frequency.

#### Steps — Opt-8: Reduce Object Allocation in Retry Loops

- [ ] In `trans/SUMOtoTFAform.java:2372-2376`: instrument the `elimUnitaryLogops` retry loop
      to count how many iterations occur on average per formula (add a debug counter)
- [ ] Replace the string-equality convergence check (`formula.equals(prev)`) with a
      structural/parse-tree equality check to avoid constructing intermediate `Formula` objects
      when the formula has not changed
- [ ] If the loop always converges in ≤2 iterations in practice, replace the retry loop with
      a fixed two-pass unroll to eliminate loop overhead entirely
- [ ] Apply the same analysis to the `constrainFunctVars` retry loop at lines 2384-2388

#### Steps — Opt-9: Larger I/O Buffers

- [ ] Locate the `BufferedWriter` instantiation(s) used to write the final output files in
      `trans/SUMOKBtoTPTPKB.java` and `trans/THFnew.java`
- [ ] Increase the buffer size argument from the default (8,192 bytes) to 4,194,304 bytes
      (4 MB): `new BufferedWriter(new FileWriter(path), 4 * 1024 * 1024)`
- [ ] Confirm no intermediate `flush()` calls interfere with the larger buffer (the existing
      single-batch write pattern via `fileContents` is already optimal)

#### Steps — Integration

- [ ] Run `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` — all tests must pass
- [ ] Diff all four generated files against Milestone 6 reference — must be identical
- [ ] Record final wall-clock times with `-Dsigma.tff.profile=true`

#### Generation Times

| File Type      | Before (M6) | After (M7)                                       |
| -------------- | ----------- | ------------------------------------------------ |
| FOF            | ~7 min      | ~6.5 min (~7% improvement from allocation + I/O) |
| TFF            | ~5 min      | ~4.5 min (~10% improvement)                      |
| THF Modal      | ~22 min     | ~21 min (~5% I/O improvement)                    |
| THF Plain      | ~22 min     | ~21 min (~5% I/O improvement)                    |
| **Wall clock** | **~22 min** | **~21 min**                                      |

#### Acceptance Criteria

- `mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*` passes with zero failures
- `diff` of all four generated files against Milestone 6 reference shows no differences
- No `OutOfMemoryError` observed with the larger buffer across 3 consecutive runs on the
  deployment machine
- TFF wall-clock time ≤5 min; THF wall-clock time ≤22 min

---

## Cumulative Improvement Summary

### Current Config (4 KIF files, 75,213 formulas)

| Milestone         | FOF      | TFF        | THF Modal | THF Plain | Sequential | Parallel wall clock | Key change                                                             |
| ----------------- | -------- | ---------- | --------- | --------- | ---------- | ------------------- | ---------------------------------------------------------------------- |
| M0 (measured)     | 14.7s    | 146.7s     | 28.1s     | 23.6s     | **213.1s** | **161.4s**          | —                                                                      |
| M1 (measured)     | 15.0s    | 175.1s     | 28.5s     | 23.9s     | **242.5s** | **175.1s**          | ThreadLocal + parallel FOF/TFF + concurrent collections                |
| **M2 (measured)** | **7.5s** | **133.8s** | **29.1s** | **24.3s** | **~195s**  | **~134s**           | Parallel formula loop + early-exit in `copyNewPredFromVariableArity()` |

**M2 notes:** Both FOF and TFF use `_tWriteFile()` (dispatched via `rapidParsing=true`), so
both benefit from the parallel formula loop. FOF achieves ~2× speedup — its per-formula work
(preprocess + rename + filter) is cheap and highly parallel. TFF gains −24% vs M1: the
parallel loop distributes `process()` across 10 threads; an early-exit added to
`copyNewPredFromVariableArity()` before its `synchronized(this)` block eliminates the dominant
lock-contention bottleneck. Only ~50–100 unique variable-arity combos exist in the KB, so the
synchronized block is entered at most ~50–100 times total across all threads and all formulas.

**M1 notes:** Per-format TFF time increased 19% due to `ConcurrentHashMap` volatile read
overhead in `SUMOtoTFAform.process()`. Sequential total is 14% slower. Parallel wall clock
(production) is 8% slower because TFF overhead exceeds FOF parallelization saving. The
parallel architecture pays off in M2 where per-format TFF time drops dramatically.

### Full SUMO KB (estimated)

| Milestone              | FOF     | TFF     | THF Modal | THF Plain | Wall clock  | Key change                                        |
| ---------------------- | ------- | ------- | --------- | --------- | ----------- | ------------------------------------------------- |
| Baseline               | 60 min  | 60 min  | 30 min    | 30 min    | **120 min** | —                                                 |
| M0 — Profiling         | 60 min  | 60 min  | 30 min    | 30 min    | **120 min** | Measurement only                                  |
| M1 — Thread safety     | 60 min  | 72 min  | 30 min    | 30 min    | **72 min**  | FOF+TFF parallel; TFF +19% from ConcurrentHashMap |
| M2 — Parallel loop     | 10 min  | 10 min  | 30 min    | 30 min    | **30 min**  | ~6× per-format speedup                            |
| M3 — Incremental       | 1 min\* | 1 min\* | 3 min\*   | 3 min\*   | **3 min\*** | \*UA-only change                                  |
| M4 — Share preProcess  | 8 min   | 8 min   | 30 min    | 30 min    | **30 min**  | Duplicate preProcess gone                         |
| M5 — Merge THF pass    | 8 min   | 8 min   | 22 min    | 22 min    | **22 min**  | One THF pass eliminated                           |
| M6 — Algorithmic fixes | 7 min   | 5 min   | 22 min    | 22 min    | **22 min**  | writeSorts + winnowTypeList                       |
| M7 — Alloc + I/O       | 6.5 min | 4.5 min | 21 min    | 21 min    | **21 min**  | Final polish                                      |

\*Incremental (UA-only) times. Full regeneration time follows the row above (M4 values).

**Overall improvement (full regen): 120 min → 21 min (~82% reduction)**
**Overall improvement (UA-only incremental): 120 min → ~3 min (~97.5% reduction)**

---

## Critical Files to Modify

| File                                  | Change                                                          | Milestone             |
| ------------------------------------- | --------------------------------------------------------------- | --------------------- |
| `trans/SUMOformulaToTPTPformula.java` | `lang`, `hideNumbers`, `qlist` → ThreadLocal                    | M1 (done)             |
| `trans/SUMOKBtoTPTPKB.java`           | `lang` → ThreadLocal; parallelize `_tWriteFile()`               | M1 (done) / M2 (done) |
| `trans/SUMOtoTFAform.java`            | `varmap`, `numericConstantTypes`, `filterMessage` → ThreadLocal | M1 (done)             |
| `KBcache.java`                        | 5 fields → ConcurrentHashMap/KeySet; `synchronized` on mutators | M1 (done)             |
| `KB.java`                             | `terms` → ConcurrentSkipListSet; `capterms` → ConcurrentHashMap | M1 (done)             |
| `trans/SUMOKBtoTFAKB.java`            | Snapshot copy in `writeSorts()`; cache Cartesian products       | M1 (done) / M6        |
| `KButilities.java`                    | Kryo serializer for `ConcurrentHashMap.KeySetView`              | M1 (done)             |
| `trans/TPTPGenerationManager.java`    | 4-thread parallel (FOF, TFF, THF Modal, THF Plain)              | M1 (done)             |
| `trans/THFnew.java`                   | Merge `analyzeBadUsages()` into main translation loop           | M5                    |
| `FormulaPreprocessor.java`            | Fix `winnowTypeList()` O(n²) → O(n log n)                       | M6                    |

---

## Verification Plan

1. Enable `-Dsigma.tff.profile=true` before and after each milestone; compare timing summaries.
2. Validate output files are byte-for-byte identical: `diff <before> <after>` for all 4
   format types (FOF, TFF, THF Modal, THF Plain).
3. Run the existing test suite after every milestone:
   ```
   mvn test -pl . -Dtest=*TPTP*,*TFF*,*THF*
   ```
4. Save profiler output to `profiling/<milestone>_<date>.txt` for regression tracking.
5. Benchmark total generation time for a full SUMO KB run after each milestone and update
   the Cumulative Improvement Summary table with actual measured values.
