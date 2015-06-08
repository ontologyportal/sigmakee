package com.articulate.sigma.nlp;

import java.io.IOException;
import java.util.*;

import com.articulate.sigma.StringUtil;
import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.articulate.sigma.nlp.pipeline.SentenceUtil;
import com.google.common.collect.*;
import com.articulate.sigma.semRewrite.*;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

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

      // the initial similarity scores of pairs of nodes
      // it corresponds to sigma_0 in Melnik's paper
    private HashMap<ArrayList<String>,Float> initialMap = null;
    private boolean exactNodes = true;  // exact match between node labels required
    private boolean allNodes = true;    // no match between node labels required
    private boolean exactLinks = true;  // exact match between link labels required
    private String equation = "basic";  // which equation from table 3

    private boolean asResource = true;

    /** ***************************************************************
     * Converts a text file that has one sentence per line into a list
     * of Graph structures that correspond to Stanford dependency graphs.
     * @return the list of Graphs
     */
    public ArrayList<Graph> readGraphsFromFile(String filename, boolean resource, Pipeline p) {

        ArrayList<Graph> result = new ArrayList<Graph>();
        List<String> lines = null;
        asResource = resource;
        List<String> documents;
        try {
            lines = TextFileUtil.readLines(filename, false);
            for (String l : lines) {
                Annotation a = p.annotate(l);
                CoreMap lastSentence = SentenceUtil.getLastSentence(a);
                List<String> dependenciesList = SentenceUtil.toDependenciesList(ImmutableList.of(lastSentence));
                String in = StringUtil.removeEnclosingCharPair(dependenciesList.toString(), Integer.MAX_VALUE, '[', ']');
                Lexer lex = new Lexer(in);
                CNF cnf = CNF.parseSimple(lex);
                Graph g = new Graph(l);
                g.fromCNF(cnf);
                result.add(g);
            }
        }
        catch (IOException e) {
            System.out.println("Unable to read: " + filename);
            throw new RuntimeException("Unable to read: " + filename);
        }
        catch (Exception e) {
            System.out.println("Error in SimFlood.readGraphsFromFile()" + e.getMessage());
            e.printStackTrace();
        }
        return result;
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
    private static int getLevenshteinDistance(String s, String t) {

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
     */
    private String stripSuffix(String s) {

        if (s.indexOf("-") < 0) {
            return s;
        }
        return s.substring(0,s.lastIndexOf("-"));
    }

    /*************************************************************
     * Find the Levenshtein string distance or binary exact match score
     * for every pair of node
     * names.  Record the inverse of the distance, normalized by
     * the length of the longer string.  Bigger number = better match
     * @return a HashMap of
     */
    private HashMap<ArrayList<String>,Float> stringMatch(Graph g1, Graph g2) {

        HashMap<ArrayList<String>,Float> result = new HashMap<ArrayList<String>,Float>();
        for (Node n1 : g1.nodes.values()) {
            for (Node n2 : g2.nodes.values()) {
                ArrayList<String> s = new ArrayList<String>();
                s.add(n1.label);
                s.add(n2.label);
                float value = (float) 1.0;
                if (exactNodes) {
                    //System.out.println("INFO in SimFlood.stringMatch(): " + n1.label);
                    //System.out.println("INFO in SimFlood.stringMatch(): " + n2.label);
                    if (stripSuffix(n1.label).equals(stripSuffix(n2.label)) || allNodes) {
                        value = (float) 0.0;
                    }
                }
                else
                    value = ((float) getLevenshteinDistance(n1.label,n2.label)) /
                            ((float) Math.max(n1.label.length(),n2.label.length()));
                //System.out.println("INFO in SimFlood.stringMatch(): " + exactNodes);
                //System.out.println("INFO in SimFlood.stringMatch(): " + (1-value));
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
     * @return the link label, if linked, or null if there is no
     * link
     */
    private String linked(Node n1, Node n2) {

        //System.out.println("SimFlood.linked(): checking: " + n1.label + " and " + n2.label);
        Iterator<Arc> it = n1.arcs.iterator();
        while (it.hasNext()) {
            Arc a = it.next();
            if (a.n2 == n2)
                return a.label;
        }
        //System.out.println("SimFlood.linked(): no link ");
        return null;
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
     */
    private static String getNodeNameFromList(ArrayList<String> l) {

        return l.get(0) + "&" + l.get(1);
    }

    /*************************************************************
     */
    private static ArrayList<String> getListFromNodeName(String s) {

        ArrayList<String> result = new ArrayList<String>();
        result.add(s.substring(0, s.indexOf("&")));
        result.add(s.substring(s.indexOf("&") + 1));
        return result;
    }

    /*************************************************************
     * Build the propagation graph.  If g1={A1->A2, A1->A3} and
     * g2={B1->B2, B1->B3} then if A1-B1 and A2-B2 have non-zero
     * similarities then since A1->A2 and B1->B2, create a pair of
     * propagation graph links for these mapping pairs A1-B1->A2-B2
     * and A2-B2->A1-B1.
     * Note that the propagation node names are a conjunction of the
     * two mapped nodes separated by a '&'
     */
    private Graph buildConnect(Graph g1, Graph g2, HashMap<ArrayList<String>,Float> initial) {

        //System.out.println("INFO in SimFlood.buildConnect(): ");
        Graph g = new Graph(g1.label + "|" + g2.label);
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
                    String link1 = linked(g1n1, g1n2);
                    String link2 = linked(g2n1,g2n2);
                    if (link1 != null && link2 != null &&
                            !g1n1.label.equals(g1n2.label) &&
                            !g2n1.label.equals(g2n2.label) &&
                            (!exactLinks || link1.equals(link2))) { // could easily be made approximate
                        //System.out.println("INFO in SimFlood.buildConnect(): adding a link for " + s + " and " + s2);
                        String key1 = getNodeNameFromList(s);
                        String key2 = getNodeNameFromList(s2);
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
     * Divide all values by coeff, which is the maximum, so that
     * all values are scaled to 0..1
     */
    private void normalize(HashMap<String,Float> vector, float coeff) {

        for (String s : vector.keySet()) {
            float f = vector.get(s).floatValue();
            f = f / coeff;
            vector.put(s,f);
        }
    }

    /*************************************************************
     * Find the euclidean distance between two vectors, which is the
     * square root of sum of the squares of the difference between
     * each pair of coefficients.
     */
    private static float euclid(HashMap<String,Float> vector1,  HashMap<String,Float> vector2) {

        if (vector1.size() != vector2.size()) {
            System.out.println("Error in SimFlood.euclid(): incompatible vector sizes: ");
            System.out.println(vector1);
            System.out.println(vector2);
            return (float) 0.0;
        }
        //System.out.println("Info in SimFlood.euclid(): vec1: " + vector1);
        //System.out.println("Info in SimFlood.euclid(): vec2: " + vector2);
        float sum = 0;
        for (String s : vector1.keySet()) {
            float v1f = vector1.get(s).floatValue();
            float diff = v1f - vector2.get(s).floatValue();
            sum += diff * diff;
        }
        float result = (float) Math.sqrt(sum);
        //System.out.println("Info in SimFlood.euclid(): result: " + result);
        return result;
    }

    /*************************************************************
     * @param freemen The men who have not "proposed"
     * @param freewomen The women who are not "engaged"
     * @param men is a mapping for a "partner" to its preference
     *                   for a map to each partner and the degree of
     *                   preference
     * @param proposedTo is the set of women proposed to by a given man
     * @return a String array of a man (index 0) who is free and the highest
     *         ranked woman (index 1) for him to propose to, or null if no
     *         such pair exists
     */
    private String[] freewoman (HashSet<String> freemen, HashSet<String> freewomen,
                                HashMap<String,SortedMap<Float,String>> men,
                                HashMap<String,HashSet<String>> proposedTo) {

        String[] manwoman = null;
        Iterator<String> it = freemen.iterator();
        boolean done = false;
        while (!done && it.hasNext()) {
            String man = it.next();
            SortedMap<Float,String> eligibleWomen = new TreeMap<Float,String>();
            eligibleWomen.putAll(men.get(man));
            String woman = null;
            Iterator<Float> eWomenIt = eligibleWomen.keySet().iterator();
            Float eWoman = eWomenIt.next();
            // find top woman not proposed to
            while (proposedTo.get(man).contains(eligibleWomen.get(eWoman)) && it.hasNext())
                eWoman = eWomenIt.next();
            if (!proposedTo.get(man).contains(eligibleWomen.get(eWoman)))
                woman = eligibleWomen.get(eWoman);
            if (woman != null && proposedTo.get(man) != null && !proposedTo.get(man).contains(woman) ) {
                Float f = eligibleWomen.firstKey();
                String s = eligibleWomen.get(f);
                manwoman = new String[2];
                manwoman[0] = man;
                manwoman[1] = woman;
                return manwoman;
            }
        }
        return null;
    }

    /*************************************************************
     * Find a mapping between two sets of potential "partners"
     * where no two potential partners who are not "married" prefer
     * each other over their existing partners.  This implements
     * Gale, D.; Shapley, L. S. (1962). "College Admissions and the
     * Stability of Marriage". American Mathematical Monthly 69: 9–14.
     * see also http://en.wikipedia.org/wiki/Stable_marriage_problem
     *
     * Apologies for the sexist formulation that has men only choosing
     * partners.  Could easily reverse the genders.
     *
     * @param men is a mapping for a "partner" to its preference
     *                   for a map to each partner and the degree of
     *                   preference
     * @param women is the set of available women
     */
    private void stableMatch(HashMap<String,SortedMap<Float,String>> men,
                             HashSet<String> women) {

        //keys are men, values are the women they have proposed to
        HashMap<String,HashSet<String>> proposedTo = new HashMap<>();

        HashSet<String> freemen = new HashSet<String>();
        freemen.addAll(men.keySet());
        HashSet<String> freewomen = new HashSet<String>();
        freewomen.addAll(women);

        String[] manwoman = freewoman(freemen,freewomen,men,proposedTo);
        while (manwoman != null) {
            if (freewomen.contains(manwoman[1])) { // (m, w) become engaged
                //else some pair (m', w) already exists
                //if w prefers m to m'
                //(m, w) become engaged
                //m' becomes free
               // else
                //(m', w) remain engaged
            }
            manwoman = freewoman(freemen,freewomen,men,proposedTo);
        }
    }

    /*************************************************************
     * propagate all the coefficient influences through the graph
     */
    private void flood(Graph g) {

        //System.out.println("Info in SimFlood.flood(): ");
        HashMap<String,Float> prevIter = new HashMap<String,Float>();
        HashMap<String,Float> nextIter = new HashMap<String,Float>();
        float convergeThreshold = (float) 0.01;
        float delta = (float) 1.0;
        int counter = 0;
        while (delta > convergeThreshold && counter++ < 15) {
            prevIter.putAll(nextIter);
            float maxCoeff = (float) 0.0;
            delta = (float) 0.0;
            for (Node n : g.nodes.values()) {
                float increment = (float) 0.0;
                float iterPrev = n.value;
                float niter0 = initialMap.get(getListFromNodeName(n.label));
                for (Arc a : n.arcs) {
                    float n2iterPrev = a.n2.value;
                    float n2iter0 = initialMap.get(getListFromNodeName(a.n2.label));
                    Node influencer = a.n2;
                    for (Arc a2 : influencer.arcs) {
                        if (a2.n2 == n) {
                            float contrib;
                            if (equation.equals("basic"))
                                contrib = a2.value * n2iterPrev; // table 3 "basic" in Melnik
                            else // if (equation.equals("C"))
                                contrib = a2.value * (n2iterPrev + n2iter0); // table 3 "C" in Melnik"
                            increment += contrib;
                        }
                    }
                }
                float newValue;
                if (equation.equals("basic"))
                    newValue = iterPrev + increment; // table 3 "basic" in Melnik"
                else // if (equation.equals("C"))
                    newValue = iterPrev + niter0 + increment; // table 3 "C" in Melnik"
                nextIter.put(n.label, newValue);
                if (newValue > maxCoeff)
                    maxCoeff = newValue;
            }
            normalize(nextIter,maxCoeff);
            if (prevIter.keySet().size() > 1)
                delta = euclid(prevIter,nextIter);
            else
                delta = convergeThreshold + 1;
            for (String s : nextIter.keySet()) {
                Node n = g.nodes.get(s);
                n.value = nextIter.get(s);
            }
            //System.out.println("Info in SimFlood.flood(): " + g);
            //System.out.println("Info in SimFlood.flood(): delta: " + delta);
        }
    }

    /*************************************************************
     */
    private Graph sfJoin(Graph g1, Graph g2, HashMap<ArrayList<String>,Float> initial) {

        HashMap<ArrayList<String>,Float> result = new HashMap<ArrayList<String>,Float>();
        Graph g = buildConnect(g1,g2,initial);
        System.out.println("\nINFO in SimFlood.sfJoin(): connection graph:\n" + g.toString());
        flood(g);
        return g;
    }

    /*************************************************************
     */
    private float selectThreshold(Graph g, HashMap<String,String> bestMatch) {

        HashMap<String,Float> bestMatchValue = new HashMap<String,Float>();
        HashSet<String> alreadyChosen = new HashSet<String>();
        //return result
        for (Node n : g.nodes.values()) {
            String first = getListFromNodeName(n.label).get(0);
            String second = getListFromNodeName(n.label).get(1);
            float val = n.value;
            ArrayList<String> key = new ArrayList<String>();
            key.add(first);
            key.add(second);
            //System.out.println("Info in SimFlood.selectThreshold(): " + first + "," + second);
            if ((!bestMatch.containsKey(first) || val > bestMatchValue.get(first))
                    && !alreadyChosen.contains(second)) {
                bestMatch.put(first, second);
                bestMatchValue.put(first, new Float(val));
                alreadyChosen.add(second);
                //System.out.println("Info in SimFlood.selectThreshold(): adding with value: " + val);
            }
        }
        float total = (float) 0.0;
        for (String s : bestMatchValue.keySet())
            total += bestMatchValue.get(s);
        return total;
    }

    /*************************************************************
     * Match two graphs using Similarity Flooding
     * @return a map of node names in the first graph and their
     * best match to a node in the second graph.
     */
    public float matchGraphs(Graph g1, Graph g2, HashMap<String,String> bestMatch) {

        initialMap = stringMatch(g1, g2);
        System.out.println("INFO in SimFlood.matchGraphs(): initial: " + initialMap);
        Graph g = sfJoin(g1, g2, initialMap);
        System.out.println("INFO in SimFlood.matchGraphs(): fixpoint: " + g);
        return selectThreshold(g,bestMatch);
    }

    /*************************************************************
     * Match two graphs using Similarity Flooding
     * @return a map of node names in the first graph and their
     * best match to a node in the second graph.
     */
    public Graph match(Graph g1, ArrayList<Graph> graphs) {

        HashMap<String,String> bestMatch = new HashMap<String,String>();
        Graph result = null;
        float bestScore = (float) 0.0;
        for (Graph one : graphs) {
            HashMap<String,String> oneMatch = new HashMap<String,String>();
            System.out.println("----------------------------------------");
            System.out.println("SimFlood.match(): query: " + g1);
            System.out.println("SimFlood.match(): candidate: " + one);
            float score = matchGraphs(g1,one,oneMatch);
            System.out.println("SimFlood.match(): result: " + score);
            System.out.println("SimFlood.match(): match: " + oneMatch);
            System.out.println("----------------------------------------");
            if (score > bestScore) {
                bestScore = score;
                bestMatch = oneMatch;
                result = one;
            }
        }
        System.out.println("SimFlood.match(): result: " + bestScore);
        System.out.println("SimFlood.match(): result: " + result);
        return result;
    }

    /*************************************************************
     */
    private static void testEuclid() {

        ArrayList<Float> vec1 = Lists.newArrayList((float) 0.25002354, (float) 0.2499893,
                (float) 1.0, (float) 0.2499893, (float) 0.24999785);
        ArrayList<Float> vec2 = Lists.newArrayList((float) 0.25001177, (float) 0.24999465,
                (float) 1.0, (float) 0.24999465, (float) 0.24999893);
        //System.out.println(euclid(vec1,vec2));
    }

    /*************************************************************
     */
    private static void testSentencePair(String s1, String l1, String s2, String l2) {

        Lexer lex = new Lexer(s1);
        CNF cnf = CNF.parseSimple(lex);
        SimFlood sf = new SimFlood();
        Graph g1 = new Graph(l1);
        g1.fromCNF(cnf);

        lex = new Lexer(s2);
        cnf = CNF.parseSimple(lex);
        sf = new SimFlood();
        Graph g2 = new Graph(l2);
        g2.fromCNF(cnf);

        HashMap<String,String> bestMatch = new HashMap<String,String>();

        System.out.println();
        System.out.println("INFO in SimFlood.main(): g1: ");
        System.out.println(g1.toString());
        System.out.println("INFO in SimFlood.main(): g2: ");
        System.out.println(g2.toString());
        bestMatch = new HashMap<String,String>();
        float score = sf.matchGraphs(g1,g2,bestMatch);
        System.out.println("INFO in SimFlood.main(): matches: " + bestMatch);
        System.out.println("INFO in SimFlood.main(): score: " + score);
    }

    /*************************************************************
     */
    private static void testSentences() {

        testSentencePair("nsubj(walked-2,John-1), dobj(walked-2,home-3).",
                "John walked home.",
                "nsubj(walked-2,Mary-1), dobj(walked-2,home-3).",
                "Mary walked home.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("nsubj(walked-2,John-1), dobj(walked-2,home-3).",
                "John walked home.",
                "nsubj(walked-2, Mary-1), det(store-5, the-4), prep_to(walked-2, store-5).",
                "Mary walked to the store.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("root(ROOT-0, laughed-2), nsubj(laughed-2, John-1).",
                "John laughed.",
                "root(ROOT-0, laughed-2), nsubj(laughed-2, John-1).",
                "John laughed.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("root(ROOT-0, kissed-2), det(baby-4, the-3), dobj(kissed-2, baby-4), nsubj(kissed-2,John-1).",
                "John kissed the baby.",
                "root(ROOT-0, kicked-2), nsubj(kicked-2, Mark-1), dobj(kicked-2, baby-4), poss(baby-4,Mark-1).",
                "Mark kicked his baby.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("root(ROOT-0, laughed-2), nsubj(laughed-2, John-1).",
                "John laughed.",
                "root(ROOT-0, cried-2), advmod(cried-2, aloud-3), nsubj(cried-2,Mark-1).",
                "Mark cried aloud.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("root(ROOT-0, built-2), nsubj(built-2, Jack-1), det(house-5, a-3), amod(house-5, crooked-4), dobj(built-2, house-5), prep_in(built-2,New_Jersey-7).",
                "Jack built a crooked house in New Jersey.",
                "root(ROOT-0, built-4), nsubj(built-4, Mary-1), conj_and(Mary-1, Susan-3), nsubj(built-4, Susan-3), amod(houses-6, crooked-5), dobj(built-4, houses-6), det(shore-11, the-8), prep_on(built-4, shore-11), nn(shore-11,New_Jersey-9), nn(shore-11,New_Jersey-9)",
                "Mary and Susan built crooked houses on the New Jersey shore.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("root(ROOT-0, built-2), nsubj(built-2, Who-1), det(house-5, a-3), amod(house-5, crooked-4), dobj(built-2, house-5), prep_in(built-2,New_Jersey-7).",
                "Who built a crooked house in New Jersey?",
                "root(ROOT-0, built-4), nsubj(built-4, Mary-1), conj_and(Mary-1, Susan-3), nsubj(built-4, Susan-3), amod(houses-6, crooked-5), dobj(built-4, houses-6), det(shore-11, the-8), prep_on(built-4, shore-11), nn(shore-11,New_Jersey-9), nn(shore-11,New_Jersey-9)",
                "Mary and Susan built crooked houses on the New Jersey shore.");

        System.out.println("\n--------------------------------\n");

        testSentencePair("root(ROOT-0, built-2), nsubj(built-2, John-1), det(house-5, a-3), amod(house-5, crooked-4), dobj(built-2, house-5), prep_in(built-2,New_Jersey-7).",
                "John built a crooked house in New Jersey.",
                "root(ROOT-0, build-4), advmod(build-4, Where-1), aux(build-4, did-2), nsubj(build-4, John-3), det(house-7, a-5), amod(house-7, crooked-6), dobj(build-4, house-7)",
                "Where did John build a crooked house?");
    }

    /*************************************************************
     */
    private static void testSentences2() {


    }

    /*************************************************************
     */
    public static void main (String[] args) {

        //testSentencePair("l1(aa, a1), l1(aa, a2), l2(a1, a2).",
        //        "graph A",
        //        "l1(bb, b1), l2(bb, b2), l2(b2, b1).",
        //        "graph B");
        testSentences();
    }
}
