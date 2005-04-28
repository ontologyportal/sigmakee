/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  */
package com.articulate.delphi;
import java.util.*;
import java.io.*;
import java.text.*;
import com.articulate.sigma.*;

/** *****************************************************************
 * A class that holds and calculates the average judgements of several
 * Delphi users.
 */
public class TableAverage {


      /** A HashMap of HashMap(s), which are the columns.  The decision
       * string are the keys of the rows and the criteria string are 
       * the keys of the columns.  The interior HashMap(s) have 
       * Distribution(s) as their values. */
    public HashMap rows = new HashMap();
      /** A HashMap of Distribution(s).  The criterion
       * strings are the keys of the columns and the Distribution are the values. */
    public HashMap weights = new HashMap();
  
    /** ***************************************************************** 
     */
    public void addRow(String text) {

        if (rows.values().size() > 0) {
            Iterator it = rows.keySet().iterator();
            String decision = (String) it.next();
            HashMap row = (HashMap) rows.get(decision);
            HashMap newRow = new HashMap();
            Iterator it2 = row.keySet().iterator();
            while (it2.hasNext()) {
                String key = new String((String) it2.next());
                // String value = new String((String) row.get(key));
                newRow.put(key,new Distribution());
            }
            rows.put(text,newRow);
        }
        else
            rows.put(text,new HashMap());
    }

    /** ***************************************************************** 
     */
    public void addTable(Table t) {

        if (t.rows.values().size() > 0) {
            Iterator it = t.rows.keySet().iterator();
            while (it.hasNext()) {
                String decision = (String) it.next();
                HashMap trow = (HashMap) t.rows.get(decision);
                if (!rows.keySet().contains(decision)) 
                    addRow(decision);
                HashMap row = (HashMap) rows.get(decision);
                Iterator it2 = trow.keySet().iterator();
                while (it2.hasNext()) {
                    String key = new String((String) it2.next());
                    if (!row.keySet().contains(key))
                        addColumn(key);                
                    Distribution value = (Distribution) row.get(key);
                    String tvalue = (String) trow.get(key);
                    value.denominator++;
                    value.numerator = value.numerator + Integer.valueOf(tvalue).intValue();
                    value.values.add(Integer.valueOf(tvalue));
                    row.put(key,value);
                }
            }
        }
        if (t.columnWeights.values().size() > 0) {
            Iterator it2 = t.columnWeights.keySet().iterator();
            while (it2.hasNext()) {
                String critereon = (String) it2.next();
                String value = (String) t.columnWeights.get(critereon);
                if (!weights.keySet().contains(critereon)) 
                    addColumn(critereon);
                Distribution p = (Distribution) weights.get(critereon);
                p.denominator++;
                p.numerator = p.numerator + Integer.valueOf(value).intValue();
                p.values.add(Integer.valueOf(value));
                weights.put(critereon,p);
            }
        }
    }

    /** ***************************************************************** 
     */
    public void addColumn(String text) {

        if (rows.values().size() > 0) {
            Iterator it = rows.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                HashMap row = (HashMap) rows.get(key);
                row.put(text,new Distribution());
            }
        }
        weights.put(text,new Distribution());
    }

    /** ***************************************************************** 
     *  Convert the TableAverage to a Table by converting each Distribution
     *  to a String value of the numerator divided by the denominator.
     */
    public Table toTable() {

        Table result = new Table();

        if (rows.values().size() > 0) {
            Iterator it = rows.keySet().iterator();
            while (it.hasNext()) {
                String decision = (String) it.next();
                HashMap row = (HashMap) rows.get(decision);
                HashMap newRow = new HashMap();
                Iterator it2 = row.keySet().iterator();
                while (it2.hasNext()) {
                    String key = new String((String) it2.next());
                    Distribution value = (Distribution) row.get(key);
                    String strval = null;
                    if (value.denominator == 0) 
                        strval = String.valueOf(0);
                    else
                        strval = String.valueOf(value.numerator / value.denominator);
                    newRow.put(key,strval);
                }
                result.rows.put(decision,newRow);
            }
        }

        Iterator it2 = weights.keySet().iterator();
        while (it2.hasNext()) {
            String key = (String) it2.next();
            Distribution p = (Distribution) weights.get(key);
            int num = p.numerator / p.denominator;
            result.columnWeights.put(key,String.valueOf(num));
        }

        return result;
    }

    /** ***************************************************************** 
     */
    public static void main(String args[]) {

    }

}
