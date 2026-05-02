package com.articulate.sigma;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * <p>Maps arbitrary strings to compact {@code int} identifiers (symbol IDs) and
 * back.  Interning a string that has already been registered returns the same ID
 * in O(1) average time (one HashMap lookup).  This mirrors the {@code SymbolId}
 * type in the Rust {@code sigma-rs} project, where all KB terms are stored as
 * {@code u64} symbols to enable O(1) equality and cheap array indexing.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   SymbolTable syms = new SymbolTable();
 *   int dogId  = syms.intern("Dog");       // 0
 *   int catId  = syms.intern("Cat");       // 1
 *   int dog2   = syms.intern("Dog");       // 0  — same ID
 *   String name = syms.getName(dogId);     // "Dog"
 * </pre>
 *
 * <h3>Thread safety</h3>
 * <p>Not thread-safe.  Populate from a single thread (e.g. during
 * {@code buildCaches()}) then treat as read-only for concurrent accessors.</p>
 *
 * <h3>Integration with KBcache</h3>
 * <p>{@link KBcache} creates one {@code SymbolTable} instance and uses it to
 * build int-indexed parent/child adjacency maps via
 * {@link KBcache#buildSymbolTaxonomy()}.  The symbol-indexed accessors
 * ({@link KBcache#getParentsSymbol}, {@link KBcache#getChildrenSymbol}) avoid
 * repeated String hashing during hot query paths.</p>
 */
public class SymbolTable {

    /** nameToId.get(name) returns the int ID assigned to name, or null if unknown. */
    private final HashMap<String, Integer> nameToId;

    /** idToName.get(id) returns the string for that ID. */
    private final ArrayList<String> idToName;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /** Creates an empty SymbolTable. */
    public SymbolTable() {
        nameToId = new HashMap<>();
        idToName = new ArrayList<>();
    }

    /**
     * Creates an empty SymbolTable with pre-allocated capacity for
     * {@code expectedSize} symbols.
     *
     * @param expectedSize anticipated number of distinct symbols
     */
    public SymbolTable(int expectedSize) {
        nameToId = new HashMap<>(Math.max(16, expectedSize * 4 / 3 + 1));
        idToName = new ArrayList<>(expectedSize);
    }

    // -----------------------------------------------------------------------
    // Core API
    // -----------------------------------------------------------------------

    /**
     * Returns the integer ID for {@code name}, assigning a new ID if this is
     * the first time {@code name} has been seen.
     *
     * <p>IDs are assigned sequentially starting at 0.</p>
     *
     * @param name the string to intern (must not be {@code null})
     * @return the stable integer ID for {@code name}
     */
    public int intern(String name) {
        Integer existing = nameToId.get(name);
        if (existing != null) return existing;
        int id = idToName.size();
        idToName.add(name);
        nameToId.put(name, id);
        return id;
    }

    /**
     * Returns the string registered for {@code id}.
     *
     * @param id a symbol ID previously returned by {@link #intern}
     * @return the original string, or {@code null} if {@code id} is out of range
     */
    public String getName(int id) {
        if (id < 0 || id >= idToName.size()) return null;
        return idToName.get(id);
    }

    /**
     * Returns the ID for {@code name} if it has already been interned, or
     * {@code -1} if it is unknown.  Does NOT register the string.
     *
     * @param name the string to look up
     * @return the ID, or {@code -1} if not interned
     */
    public int lookup(String name) {
        Integer id = nameToId.get(name);
        return id == null ? -1 : id;
    }

    /**
     * Returns the number of distinct strings currently registered.
     */
    public int size() {
        return idToName.size();
    }

    /**
     * Returns {@code true} if {@code name} has been interned.
     */
    public boolean contains(String name) {
        return nameToId.containsKey(name);
    }
}
