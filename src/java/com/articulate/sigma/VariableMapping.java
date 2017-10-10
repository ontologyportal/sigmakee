package com.articulate.sigma;

/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License <http://www.gnu.org/copyleft/gpl.html>.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico. See also http://github.com/ontologyportal
*/

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** *****************************************************************
 */
public class VariableMapping {

    String var1;
    String var2;

    /** *****************************************************************
     */
    public VariableMapping(String v1, String v2) {
        var1 = v1;
        var2 = v2;
    }

    /** *****************************************************************
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VariableMapping that = (VariableMapping) o;

        if (var1 != null ? !var1.equals(that.var1) : that.var1 != null) {
            return false;
        }
        if (var2 != null ? !var2.equals(that.var2) : that.var2 != null) {
            return false;
        }

        return true;
    }

    /** *****************************************************************
     */
    @Override
    public int hashCode() {

        int result = var1 != null ? var1.hashCode() : 0;
        result = 31 * result + (var2 != null ? var2.hashCode() : 0);
        return result;
    }

    /** *****************************************************************
     */
    public static List<Set<VariableMapping>> intersect(List<Set<VariableMapping>> mapList1,
                                                       List<Set<VariableMapping>> mapList2) {

        List<Set<VariableMapping>> intersection = new LinkedList<Set<VariableMapping>>();
        if (mapList1 == null || mapList2 == null) {
            return null;
        }
        for (Set<VariableMapping> set1 : mapList1) {
            for (Set<VariableMapping> set2 : mapList2) {
                Set<VariableMapping> newSet = VariableMapping.unify(set1, set2);
                if(newSet != null && !intersection.contains(newSet)) {
                    intersection.add(newSet);
                }
            }
        }
        if (intersection.isEmpty()) {
            //keeping the convention of null meaning imposibility
            intersection = null;
        }
        return intersection;
    }

    /** *****************************************************************
     */
    public static List<Set<VariableMapping>> union(List<Set<VariableMapping>> mapList1,
                                                   List<Set<VariableMapping>> mapList2) {

        List<Set<VariableMapping>> union = new LinkedList<Set<VariableMapping>>();
        if(mapList1 != null) {
            for (Set<VariableMapping> set1 : mapList1) {
                union.add(set1);
            }
        }
        if (mapList2 != null) {
            for (Set<VariableMapping> set2 : mapList2) {
                if ( !union.contains(set2)) {
                    union.add(set2);
                }
            }
        }
        return union;
    }

    /** *****************************************************************
     */
    private static Set<VariableMapping> unify(Set<VariableMapping> set1, Set<VariableMapping> set2) {

        Set<VariableMapping> result = new HashSet<VariableMapping>();
        for(VariableMapping element:set1) {
            result.add(element);
        }
        for(VariableMapping element:set2) {
            //testing the element does not collide with an existing element
            for(VariableMapping e:result){
                boolean leftVarsEqual = e.var1.equals(element.var1);
                boolean rightVarsEqual = e.var2.equals(element.var2);
                if ((leftVarsEqual && !rightVarsEqual) || (!leftVarsEqual && rightVarsEqual)) {
                    return null;
                }
            }
            result.add(element);
        }
        return result;
    }

    /** *****************************************************************
     */
    @Override
    public String toString() {

        return "VariableMapping{" +
                "var1='" + var1 + '\'' +
                ", var2='" + var2 + '\'' +
                '}';
    }
}

