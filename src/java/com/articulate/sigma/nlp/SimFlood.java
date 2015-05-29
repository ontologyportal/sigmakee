package com.articulate.sigma.nlp;

import java.util.*;
import com.google.common.collect.*;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

Based on :
Melnik, Sergey and Garcia-Molina, Hector and Rahm, Erhard (2001)
Similarity Flooding: A Versatile Graph Matching Algorithm (Extended
Technical Report). Stanford.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
 */
public class SimFlood {

    /*************************************************************
     */
    public class Node {
        public String label = "";
        public float value = (float) 0.0;
        public Set<Arc> arcs = new HashSet<Arc>();  // n1 of the arc is always this node

        public String toString() {
            return label + ":" + Float.toString(value) + " [" + arcs + "]";
        }
    }

    /*************************************************************
     */
    public class Arc {
        public String label = "";
        public float value = (float) 0.0;
        public Node n1 = null;
        public Node n2 = null;

        public String toString() {
            return n1.label + "-" + Float.toString(value) + "->" + n2.label;
        }
    }

    /*************************************************************
     */
    public class Graph {

        public String label = "";
        public Map<String, Node> nodes = new HashMap<String, Node>();
        public List<Arc> arcs = new ArrayList<Arc>();

        /*************************************************************
         */
        public void fromLists(List<String> ns, List<List<String>> as) {

            //System.out.println("INFO in Graph.fromLists(): nodes: " + ns);
            //System.out.println("INFO in Graph.fromLists(): arcs: " + as);
            nodes = new HashMap<String, Node>();
            arcs = new ArrayList<Arc>();
            for (String s : ns) {
                Node n = new Node();
                n.label = s;
                nodes.put(s, n);
            }
            //System.out.println("INFO in Graph.fromLists(): Nodes: " + nodes);

            arcs = new ArrayList<Arc>();
            for (List<String> l : as) {
                //System.out.println("INFO in Graph.fromLists(): arc: " + l);
                Arc a = new Arc();
                a.label = l.get(2);
                a.n1 = nodes.get(l.get(0));
                a.n2 = nodes.get(l.get(1));
                a.n1.arcs.add(a);
                arcs.add(a);
            }
            //System.out.println("INFO in Graph.fromLists(): result # nodes: " + nodes.size());
            //System.out.println("INFO in Graph.fromLists(): result # arcs: " + arcs.size());
        }

        /*************************************************************
         */
        public String toString() {

            StringBuffer sb = new StringBuffer();
            Iterator<String> it = nodes.keySet().iterator();
            while (it.hasNext()) {
                String s = it.next();
                Node n = nodes.get(s);
                Iterator<Arc> it2 = n.arcs.iterator();
                while (it2.hasNext()) {
                    Arc a = it2.next();
                    sb.append(a.toString() + "; ");
                }
                if (n.arcs.size() > 0) sb.append('\n');
            }
            return sb.toString();
        }
    }

    /*************************************************************
     * <p>Find the Levenshtein distance between two Strings.</p>
     *
     * <p>This is the number of changes needed to change one String into
     * another, where each change is a single character modification (deletion,
     * insertion or substitution).</p>
     *
     * <p>Chas Emerick has written an implementation in Java, which avoids an OutOfMemoryError
     * which can occur when my Java implementation is used with very large strings.<br>
     * This implementation of the Levenshtein distance algorithm
     * is from <a href="http://www.merriampark.com/ldjava.htm">http://www.merriampark.com/ldjava.htm</a></p>
     *
     * <pre>
     * StringUtils.getLevenshteinDistance(null, *)             = IllegalArgumentException
     * StringUtils.getLevenshteinDistance(*, null)             = IllegalArgumentException
     * StringUtils.getLevenshteinDistance("","")               = 0
     * StringUtils.getLevenshteinDistance("","a")              = 1
     * StringUtils.getLevenshteinDistance("aaapppp", "")       = 7
     * StringUtils.getLevenshteinDistance("frog", "fog")       = 1
     * StringUtils.getLevenshteinDistance("fly", "ant")        = 3
     * StringUtils.getLevenshteinDistance("elephant", "hippo") = 7
     * StringUtils.getLevenshteinDistance("hippo", "elephant") = 7
     * StringUtils.getLevenshteinDistance("hippo", "zzzzzzzz") = 8
     * StringUtils.getLevenshteinDistance("hello", "hallo")    = 1
     * </pre>
     *
     * @param s  the first String, must not be null
     * @param t  the second String, must not be null
     * @return result distance
     * @throws IllegalArgumentException if either String input <code>null</code>
     */
    public static int getLevenshteinDistance(String s, String t) {

        if (s == null || t == null)
            throw new IllegalArgumentException("Strings must not be null");

    /* We maintain two single-dimensional arrays of length s.length()+1.  The first, d,
     is the 'current working' distance array that maintains the newest distance cost
     counts as we iterate through the characters of String s.  Each time we increment
     the index of String t we are comparing, d is copied to p, the second int[].  Doing so
     allows us to retain the previous cost counts as required by the algorithm (taking
     the minimum of the cost count to the left, up one, and diagonally up and to the left
     of the current cost count being calculated).  (Note that the arrays aren't really
     copied anymore, just switched...this is clearly much better than cloning an array
     or doing a System.arraycopy() each time  through the outer loop.) */

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0)
            return m;
        else if (m == 0)
            return n;

        if (n > m) { // swap the input strings to consume less memory
            String tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }

        int p[] = new int[n+1]; //'previous' cost array, horizontally
        int d[] = new int[n+1]; // cost array, horizontally
        int _d[];               //placeholder to assist in swapping p and d

        int i; // iterates through s
        int j; // iterates through t
        char t_j; // jth character of t
        int cost; // cost

        for (i = 0; i <= n; i++)
            p[i] = i;

        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j-1);
            d[0] = j;
            for (i = 1; i <= n; i++) {
                cost = s.charAt(i-1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i-1] + 1, p[i] + 1),  p[i-1] + cost);
            }
            _d = p; // copy current distance counts to 'previous row' distance counts
            p = d;
            d = _d;
        }
        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    /*************************************************************
     * Find the Levenshtein string distance for every pair of node
     * names.  Record the inverse of the distance, normalized by
     * the length of the longer string.  Bigger number = better match
     */
    public HashMap<ArrayList<String>,Float> stringMatch(Graph g1, Graph g2) {

        HashMap<ArrayList<String>,Float> result = new HashMap<ArrayList<String>,Float>();
        for (Node n1 : g1.nodes.values()) {
            for (Node n2 : g2.nodes.values()) {
                ArrayList<String> s = new ArrayList<String>();
                s.add(n1.label);
                s.add(n2.label);
                float value = ((float) getLevenshteinDistance(n1.label,n2.label)) /
                        ((float) Math.max(n1.label.length(),n2.label.length()));
                result.put(s,new Float(1 - value));
            }
        }
        return result;
    }

    /*************************************************************
     * Checks if n1 has an arc to n2.  If it's a directed graph and
     * n2 points to n1 then this routine returns false.  If it's an
     * undirected graph then the routine that builds the graph is
     * responsible for adding both a link from n1 to n2 and n2 to
     * n1, if the nodes are linked.
     */
    private boolean linked(Node n1, Node n2) {

        //System.out.println("SimFlood.linked(): checking: " + n1.label + " and " + n2.label);
        Iterator<Arc> it = n1.arcs.iterator();
        while (it.hasNext()) {
            Arc a = it.next();
            if (a.n2 == n2)
                return true;
        }
        //System.out.println("SimFlood.linked(): no link ");
        return false;
    }

    /*************************************************************
     * Set link coefficients based on the number of arcs leaving a
     * given node.  Each arc gets an equal "share" of influence.
     */
    private static void setInitialLinkCoeff(Graph g) {

        for (Node n : g.nodes.values()) {
            int numArcs = n.arcs.size();
            float coeff = (float) 1.0 / (float) numArcs;
            for (Arc a : n.arcs) {
                a.value = coeff;
            }
        }
    }

    /*************************************************************
     * Build the propagation graph.  If g1={A1->A2, A1->A3} and
     * g2={B1->B2, B1->B3} then if A1-B1 and A2-B2 have non-zero
     * similarities then since A1->A2 and B1->B2, create a pair of
     * propagation graph links for these mapping pairs A1-B1->A2-B2
     * and A2-B2->A1-B1
     */
    public Graph buildConnect(Graph g1, Graph g2, HashMap<ArrayList<String>,Float> initial) {

        //System.out.println("INFO in SimFlood.buildConnect(): ");
        Graph g = new Graph();
        Iterator<ArrayList<String>> it = initial.keySet().iterator();
        while (it.hasNext()) {
            ArrayList<String> s = it.next();  // first mapping pair
            Iterator<ArrayList<String>> it2 = initial.keySet().iterator();
            while (it2.hasNext()) {
                ArrayList<String> s2 = it2.next(); // second mapping pair
                //System.out.println("INFO in SimFlood.buildConnect(): checking " + s + " - " + s2);
                // now we need to see if both graphs have links between their nodes
                if (!s.equals(s2)) {
                    Node g1n1 = g1.nodes.get(s.get(0));
                    Node g1n2 = g1.nodes.get(s2.get(0));
                    Node g2n1 = g2.nodes.get(s.get(1));
                    Node g2n2 = g2.nodes.get(s2.get(1));
                    if (linked(g1n1,g1n2) && linked(g2n1,g2n2)) {
                        //System.out.println("INFO in SimFlood.buildConnect(): adding a link for " + s + " and " + s2);
                        String key1 = s.get(0) + "-" + s.get(1);
                        String key2 = s2.get(0) + "-" + s2.get(1);
                        Node n1 = null;
                        Node n2 = null;
                        if (!g.nodes.containsKey(key1)) {
                            n1 = new Node();
                            n1.label = key1;
                            n1.value = initial.get(s);
                            g.nodes.put(key1,n1);
                        }
                        else
                            n1 = g.nodes.get(key1);
                        if (!g.nodes.containsKey(key2)) {
                            n2 = new Node();
                            n2.label = key2;
                            n2.value = initial.get(s2);
                            g.nodes.put(key2,n2);
                        }
                        else
                            n2 = g.nodes.get(key2);
                        Arc a = new Arc();
                        a.n1 = n1;
                        a.n2 = n2;
                        a.label = "";
                        n1.arcs.add(a);
                        Arc a2 = new Arc();
                        a2.n1 = n2;
                        a2.n2 = n1;
                        n2.arcs.add(a2);
                        g.nodes.put(n1.label, n1);
                        g.nodes.put(n2.label, n2);
                        g.arcs.add(a);
                        g.arcs.add(a2);
                    }
                }
            }
        }
        setInitialLinkCoeff(g);
        return g;
    }

    /*************************************************************
     */
    private void normalize(HashMap<String,Float> vector, float coeff) {

    }

    /*************************************************************
     */
    private float euclid(HashMap<String,Float> vector1, HashMap<String,Float> vector2) {

        return (float) 0.0;
    }

    /*************************************************************
     * propagate all the coefficient influences through the graph
     */
    private void flood(Graph g) {

        HashMap<String,Float> prevIter = new HashMap<String,Float>();
        HashMap<String,Float> nextIter = new HashMap<String,Float>();
        float convergeThreshold = (float) 0.1;
        float delta = (float) 1.0;
        while (delta > convergeThreshold) {
            prevIter.putAll(nextIter);
            float maxCoeff = (float) 0.0;
            delta = (float) 0.0;
            for (Node n : g.nodes.values()) {
                float increment = (float) 0.0;
                float iter0 = n.value;
                for (Arc a : n.arcs) {
                    float n2iter0 = a.n2.value;
                    Node influencer = a.n2;
                    for (Arc a2 : influencer.arcs) {
                        if (a2.n2 == n) {
                            float contrib = a2.value * n2iter0;
                            increment += contrib;
                        }
                    }
                }
                float newValue = iter0 + increment;
                nextIter.put(n.label,newValue);
                if (newValue > maxCoeff)
                    maxCoeff = newValue;
            }
            normalize(nextIter,maxCoeff);
            delta = euclid(prevIter,nextIter);
        }
    }

    /*************************************************************
     */
    public HashMap<ArrayList<String>,Float> sfJoin(Graph g1, Graph g2,
                                             HashMap<ArrayList<String>,Float> initial) {

        HashMap<ArrayList<String>,Float> result = new HashMap<ArrayList<String>,Float>();
        Graph g = buildConnect(g1,g2,initial);
        System.out.println("\nINFO in SimFlood.sfJoin(): connection graph:\n" + g.toString());
        flood(g);
        return result;
    }

    /*************************************************************
     */
    public HashMap<ArrayList<String>,Float> selectThreshold(HashMap<ArrayList<String>,Float> product) {

        HashMap<ArrayList<String>,Float> result = new HashMap<ArrayList<String>,Float>();
        return result;
    }

    /*************************************************************
     */
    public HashMap<ArrayList<String>,Float> matchGraphs(Graph g1, Graph g2) {

        HashMap<ArrayList<String>,Float> initialMap = stringMatch(g1, g2);
        System.out.println("INFO in SimFlood.matchGraphs(): " + initialMap);
        HashMap<ArrayList<String>,Float> product = sfJoin(g1, g2, initialMap);
        return selectThreshold(product);
    }

    /*************************************************************
     */
    public static void main (String[] args) {

        List<String> nodes1 = Lists.newArrayList("John-1","walked-1","home-1");
        List<String> nodes2 = Lists.newArrayList("Mary-2","walked-2","home-2");

        List<String> arc;
        List<List<String>> arcs1 = new ArrayList<List<String>>();
        arc = Lists.newArrayList("walked-1","John-1","nsubj");
        arcs1.add(arc);
        arc = Lists.newArrayList("walked-1","home-1","dobj");
        arcs1.add(arc);

        List<List<String>> arcs2 = new ArrayList<List<String>>();
        arc = Lists.newArrayList("walked-2","Mary-2","nsubj");
        arcs2.add(arc);
        arc = Lists.newArrayList("walked-2","home-2","dobj");
        arcs2.add(arc);

        SimFlood sf = new SimFlood();
        Graph g1 = sf.new Graph();
        g1.fromLists(nodes1, arcs1);
        Graph g2 = sf.new Graph();
        g2.fromLists(nodes2, arcs2);

        System.out.println("INFO in SimFlood.main(): g1: ");
        System.out.println(g1.toString());
        System.out.println("INFO in SimFlood.main(): g2: ");
        System.out.println(g2.toString());
        HashMap<ArrayList<String>,Float> match = sf.matchGraphs(g1,g2);
        System.out.println("INFO in SimFlood.main(): matches: " + match);
    }
}
