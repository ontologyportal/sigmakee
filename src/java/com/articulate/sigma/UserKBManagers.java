/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
*/

package com.articulate.sigma;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of active per-user KB managers.
 * Lets you run many user managers concurrently alongside the global KBmanager.
 */
public class UserKBManagers {

    private static final ConcurrentHashMap<String, UserKBmanager> REGISTRY = new ConcurrentHashMap<>();

    /** ***************************************************************
     * Return an existing UserKBmanager for a username, or lazily
     * create and initialize one if it does not exist.
     *
     * @param username the user ID to look up or initialize
     * @return a thread-safe UserKBmanager instance for that user
     */
    public static UserKBmanager getOrInit(String username) {
    
        return REGISTRY.computeIfAbsent(username, u -> {
            UserKBmanager m = new UserKBmanager();
            m.initializeOnceUser(u);
            return m;
        });
    }

    /** ***************************************************************
     * Remove a user’s manager from the registry.
     * This does not persist or delete user files, just the in-memory map.
     *
     * @param username the user ID to remove
     */
    public static void remove(String username) {
        REGISTRY.remove(username);
    }

    /** ***************************************************************
     * Look up a user’s manager without creating a new one.
     *
     * @param username the user ID to look up
     * @return the UserKBmanager if present, else null
     */
    public static UserKBmanager get(String username) {
        return REGISTRY.get(username);
    }
}
