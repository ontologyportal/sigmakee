package com.articulate.sigma.trans;

import com.articulate.sigma.DB;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.StringUtil;

import java.io.File;
import java.util.*;

/*
copyright 2018- Infosys

contact Adam Pease adam.pease@infosys.com

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

 * Map SQL or CSV files into KIF statements.
 */
public class DB2KIF {

    // A map of column header names to SUMO relation names
    public static HashMap<String,String> column2Rel = new HashMap<>();

    // might need to contextualize this to a column or type in the future
    // since the same short abbreviation could mean different things in different
    // columns
    public static HashMap<String,String> symbol2Term = new HashMap<>();

    // relation keys and key value pairs of first argument and a set of prohibited second arguments
    // When the first argument doesn't matter, include a value for that argument of "*"
    HashMap<String,HashMap<String,HashSet<String>>> badValues = new HashMap<>();

    // relation keys and key value pairs of first argument and a set of possible allowed second arguments
    // When the first argument doesn't matter, include a value for that argument of "*"
    HashMap<String,HashMap<String,HashSet<String>>> goodValues = new HashMap<>();

    // relation keys and key value pairs of argument number and maximum value
    HashMap<String,HashMap<Integer,Double>> maxValue = new HashMap<>();

    // relation keys and key value pairs of argument number and minimum value
    HashMap<String,HashMap<Integer,Double>> minValue = new HashMap<>();

    // whether to correct a value to an allowed value, ignore bad values, or delete the entire row
    enum Action {Correct,Ignore,Delete};
    Action action = Action.Ignore;

    String defaultRowType = "Human";

    public static KB kb = null;

    /** *****************************************************************
     * A convenience method that gets the minimum expected value for
     * the given argument to a relation.  Return null if there is no
     * such value specified for the relation and argument number.
     */
    public Double getMin(String rel, int arg) {

        if (minValue.containsKey(rel)) {
            HashMap<Integer,Double> values = minValue.get(rel);
            if (values.containsKey(arg))
                return values.get(arg);
            else
                return null;
        }
        else
            return null;
    }

    /** *****************************************************************
     * A convenience method that gets the maximum expected value for
     * the given argument to a relation.  Return null if there is no
     * such value specified for the relation and argument number.
     */
    public Double getMax(String rel, int arg) {

        //System.out.println("DB2KIF.getMax(): rel: " + rel + " arg: " + arg);
        if (maxValue.containsKey(rel)) {
            HashMap<Integer,Double> values = maxValue.get(rel);
            //System.out.println("DB2KIF.getMax(): values: " + values);
            if (values.containsKey(arg))
                return values.get(arg);
            else
                return null;
        }
        else
            return null;
    }

    /** *****************************************************************
     * A convenience method that gets the set of expected values for
     * the given argument to a relation.  Return null if there are no
     * such values specified for the relation and argument number.
     */
    public HashSet<String> getGood(String rel, String arg) {

        if (goodValues.containsKey(rel)) {
            HashMap<String,HashSet<String>> values = goodValues.get(rel);
            if (values.containsKey(arg))
                return values.get(arg);
            else
                return null;
        }
        else
            return null;
    }

    /** *****************************************************************
     * A convenience method that gets the set of values not expected for
     * the given argument to a relation.  Return null if there are no
     * such values specified for the relation and argument number.
     */
    public HashSet<String> getBad(String rel, String arg) {

        if (badValues.containsKey(rel)) {
            HashMap<String,HashSet<String>> values = badValues.get(rel);
            if (values.containsKey(arg))
                return values.get(arg);
            else
                return null;
        }
        else
            return null;
    }

    /** *****************************************************************
     * A convenience method that sets the  value for a given
     * relation and "*" or "any" first argument.  Note that valSet could
     * be either the goodValues or the badValues set.
     */
    private void addValueForAny(String rel, String arg, HashMap<String,HashMap<String,HashSet<String>>> valSet) {

        //HashMap<String,HashMap<String,HashSet<String>>>
        HashMap<String,HashSet<String>> forRel = valSet.get(rel);
        if (forRel == null) {
            forRel = new HashMap<String, HashSet<String>>();
            valSet.put(rel, forRel);
        }
        HashSet<String> forAny = forRel.get("*");
        if (forAny == null) {
            forAny = new HashSet<String>();
            forRel.put("*",forAny);
        }
        forAny.add(arg);
    }

    /** *****************************************************************
     * A convenience method that sets the  value for a given
     * relation and "*" or "any" first argument.  Note that valSet could
     * be either the goodValues or the badValues set.
     */
    private void addValuesForAny(String rel, Collection<String> args, HashMap<String,HashMap<String,HashSet<String>>> valSet) {

        for (String s : args)
            addValueForAny(rel,s,valSet);
    }

    /** *****************************************************************
     * Check values in a spreadsheet format where the columns have been
     * mapped to relations and there is a set of good and bad values
     * compiled for the arguments to those relations.
     */
    public String clean(ArrayList<ArrayList<String>> cells) {

        StringBuffer sb = new StringBuffer();
        for (int r = 2; r < cells.size(); r++) {
            ArrayList<String> row = cells.get(r);
            for (int i = 0; i < row.size(); i++) {
                String header = cells.get(0).get(i);
                String relation = column2Rel.get(header);
                if (StringUtil.emptyString(relation))
                    relation = header;
                String value = row.get(i);
                //System.out.println("DB2KIF.test(): header: " + header +
                //    " relation: " + relation + " value: " + value);
                if (StringUtil.isNumeric(value)) {
                    //System.out.println("DB2KIF.test(): is numeric");
                    Double min = getMin(relation,2);
                    if (min != null) {
                        Double doubleVal = Double.parseDouble(value);
                        if (doubleVal != null) {
                            if (doubleVal < min) {
                                sb.append("<b>Error</b> in DB2KIF.clean(): bad value " +
                                        doubleVal + " less than " + min + " for relation " + relation + "<P>\n");
                                sb.append("row: " + row + "<P>\n");
                                if (action == Action.Correct)
                                    value = min.toString();
                            }
                        }
                    }

                    Double max = getMax(relation,2);
                    if (max != null) {
                        Double doubleVal = Double.parseDouble(value);
                        if (doubleVal != null) {
                            if (doubleVal > max) {
                                sb.append("<b>Error</b> in DB2KIF.clean(): bad value " +
                                        doubleVal + " more than " + max + " for relation " + relation + "<P>\n");
                                sb.append("row: " + row + "<P>\n");
                                if (action == Action.Correct)
                                    value = max.toString();
                            }
                        }
                    }
                }
                else {
                    // relation keys and key value pairs of first argument and a set of possible allowed second arguments
                    //HashMap<String,HashMap<String,HashSet<String>>> goodValues = new HashMap<>();
                    //sb.append("DB2KIF.clean(): relation " + relation + "<P>\n");
                    HashMap<String,HashSet<String>> good4Rel = goodValues.get(relation);
                    if (good4Rel != null) {
                        //sb.append("DB2KIF.clean(): found relation " + relation + "<P>\n");
                        HashSet<String> goodVals = good4Rel.get(defaultRowType);
                        if (goodVals != null){
                            //sb.append("DB2KIF.clean(): found row type " + defaultRowType + "<P>\n");
                            if (!goodVals.contains(value)) {
                                sb.append("<b>Error</b> in DB2KIF.clean(): bad value " +
                                        value + " not in list of allowed values " + goodVals + " for relation " + relation + "<P>\n");
                                sb.append("row: " + row + "<P>\n");
                            }
                        }
                    }
                    HashMap<String,HashSet<String>> bad4Rel = badValues.get(relation);
                    if (bad4Rel != null) {
                        HashSet<String> badVals = bad4Rel.get(defaultRowType);
                        if (badVals != null) {
                            if (badVals.contains(value)) {
                                sb.append("<b>Error</b> in DB2KIF.clean(): bad value " +
                                        value + " not an allowed value for relation " + relation +
                                        " with bad value list " + badVals + "<P>\n");
                                sb.append("row: " + row + "<P>\n");
                            }
                        }
                    }
                }
            }
        }
        if (sb.length() == 0)
            return "No errors found in DB2KIF.clean()";
        return sb.toString();
    }

    /** *****************************************************************
     * Create some sample good and bad values for testing
     */
    public static void initSampleValues(DB2KIF dbkif) {

        HashMap<Integer,Double> limit = new HashMap<Integer,Double>();
        limit.put(2,Double.valueOf(80.0));

        dbkif.maxValue.put("hours per week",limit);
        dbkif.column2Rel.put("hours per week","hours per week");

        HashMap<String,HashSet<String>> good4Rel = new HashMap<>();
        HashSet<String> goodVals = new HashSet<>();
        goodVals.add("Male");
        goodVals.add("Female");
        goodVals.add("Unknown");
        good4Rel.put("Human",goodVals);
        dbkif.goodValues.put("gender",good4Rel);

        HashMap<String,HashSet<String>> bad4Rel = new HashMap<>();
        HashSet<String> badVals = new HashSet<>();
        badVals.add("Other");
        bad4Rel.put("Human",badVals);
        dbkif.badValues.put("gender",bad4Rel);
    }

    /** *****************************************************************
     * Take statements in SUMO and generate sets of good or bad values
     * for relations
     */
    public static void initValues(DB2KIF dbkif) {

        kb = KBmanager.getMgr().getKB("SUMO");
        for (String r : column2Rel.values()) {
            String t = kb.getArgType(r,2);
            if (StringUtil.emptyString(t))
                continue;
            HashSet<String> insts = new HashSet<String>();
            if (kb.isInstance(t)) {
                HashMap<String,HashSet<String>> temp = kb.kbCache.children.get("subAttribute");
                if (insts != null)
                    insts = temp.get(t);
                else
                    System.out.println("Error in DB2KIF.initValues(): null set of subAttributes");
            }
            else {
                Collection<String> cls = kb.kbCache.getInstancesForType(t);
                if (cls != null)
                    insts.addAll(cls);
                System.out.println("DB2KIF.initValues(): instances: " + cls);
                for (String c : cls) {
                    HashSet<String> children = kb.kbCache.children.get("subAttribute").get(c);
                    if (children != null)
                        insts.addAll(children);
                }
            }
            // relation keys and key value pairs of first argument and a set of possible allowed second arguments
            // HashMap<String,HashMap<String,HashSet<String>>> goodValues = new HashMap<>();
            System.out.println("DB2KIF.initValues(): for " + r + " : " + insts);
            dbkif.addValuesForAny(r,insts,dbkif.goodValues);
        }
    }

    /** *****************************************************************
     */
    public static void main (String[] args) {

        KBmanager.getMgr().initializeOnce();
        DB2KIF dbkif = new DB2KIF();
        String fname = System.getenv("CORPORA") + File.separator + "UICincome" + File.separator + "adult.data-AP-small.txt.csv";
        ArrayList<ArrayList<String>> cells = DB.readSpreadsheet(fname,null,false,',');
        dbkif.column2Rel.put("color","color");
        initValues(dbkif);
        /*
        ArrayList<String> result = new ArrayList<>();
        String fname = System.getenv("CORPORA") + File.separator + "UICincome" + File.separator + "adult.data-AP-small.txt.csv";
        if (args != null && args.length > 0)
            fname = args[0];
        ArrayList<ArrayList<String>> cells = DB.readSpreadsheet(fname,null,false,',');
        if (cells.size() < 3)
            System.exit(0);
        initSampleValues(dbkif);
        System.out.println(dbkif.clean(cells));
        */
    }
}
