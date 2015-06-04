package com.articulate.sigma.semRewrite;

import java.util.*;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

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

/*************************************************************
 */
public class Graph {

    public String label = "";
    public Map<String, Node> nodes = new HashMap<String, Node>();
    public List<Arc> arcs = new ArrayList<Arc>();

    /*************************************************************
     */
    public Graph (String l) {
        label = l;
    }

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
     * Convert from CNF into the Graph structure
     */
    public void fromCNF(CNF cnf) {

        nodes = new HashMap<String, Node>();
        arcs = new ArrayList<Arc>();
        for (Clause c : cnf.clauses) {
            if (c.disjuncts.size() > 1)
                System.out.println("Error in SimFlood.Graph.fromCNF(): multiple disjuncts: " + c);
            for (Literal d : c.disjuncts) {
                if (d.negated)
                    System.out.println("Error in SimFlood.Graph.fromCNF(): negated literal: " + d);
                Node n1 = null;
                Node n2 = null;
                if (!nodes.containsKey(d.arg1)) {
                    n1 = new Node();
                    n1.label = d.arg1;
                    nodes.put(n1.label, n1);
                }
                else
                    n1 = nodes.get(d.arg1);
                if (!nodes.containsKey(d.arg2)) {
                    n2 = new Node();
                    n2.label = d.arg2;
                    nodes.put(n2.label,n2);
                }
                else
                    n2 = nodes.get(d.arg2);
                Arc a = new Arc();
                a.label = d.pred;
                a.n1 = n1;
                a.n2 = n2;
                n1.arcs.add(a);
                n2.arcs.add(a);
                arcs.add(a);
            }
        }
    }

    /*************************************************************
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        Iterator<String> it = nodes.keySet().iterator();
        sb.append(label + "\n");
        while (it.hasNext()) {
            String s = it.next();
            Node n = nodes.get(s);
            sb.append(s + ":" + n.value + " | ");
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
