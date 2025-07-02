

/** This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  */
package com.articulate.delphi;

import java.util.*;
import com.articulate.sigma.*;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public class Table {

      /** The username who owns the table. */
    public String username;
      /** A HashMap of HashMap(s), which are the columns.  The decision
       * string are the keys of the rows and the criteria string are
       * the keys of the columns.  The interior HashMap(s) have
       * integers of range 0-10 as their values, coded as Strings. */
    public HashMap rows = new HashMap();
      /** A HashMap of criteria string keys and string importance weight
       * values, which are integers between 0 and 10, represented as Strings. */
    public HashMap columnWeights = new HashMap();

    /** *****************************************************************
     */
    public void addRow(String text) {

        if (!rows.values().isEmpty()) {
            Iterator it = rows.keySet().iterator();
            String decision = (String) it.next();
            HashMap row = (HashMap) rows.get(decision);
            HashMap newRow = new HashMap();
            Iterator it2 = row.keySet().iterator();
            while (it2.hasNext()) {
                String key = (String) it2.next();
                String value = (String) row.get(key);
                newRow.put(key,"0");
            }
            rows.put(text,newRow);
        }
        else
            rows.put(text,new HashMap());
    }

    /** *****************************************************************
     */
    public void addColumn(String text) {

        columnWeights.put(text,"0");
        if (!rows.values().isEmpty()) {
            Iterator it = rows.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                HashMap row = (HashMap) rows.get(key);
                row.put(text,"0");
            }
        }
    }

    /** *****************************************************************
     */
    public String printRecommendations() {

        Pair[] recommendations = calculateRecommendations();
        String best = recommendations[recommendations.length-1].str;
        StringBuilder sb = new StringBuilder();

        sb = sb.append("<b>Best judgement: ").append(best).append("</b>");
        if (recommendations.length > 1) {
            sb = sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Other judgements, in order: ");
            for (int i = 0; i < recommendations.length - 1 ; i++) {
                sb = sb.append(recommendations[i].str);
                if (i < recommendations.length - 2)
                    sb = sb.append(", ");
            }
        }
        sb = sb.append("<P>\n");
        return sb.toString();
    }

    /** *****************************************************************
     * Generate an HTML table from the "rows" variable in this class.
     * If the option is for a readOnly table, just show the table values
     * as text, otherwise, make them a menu with the stored value as the
     * selected option.
     */
    public String toHTML(boolean readOnly, String projectName, TableAverage average) {

        ArrayList options = new ArrayList();
        for (int i = 0; i < 11; i++) {
            options.add(String.valueOf(i));
        }
        StringBuilder sb = new StringBuilder();
        sb = sb.append("<form>\n<table border=1>");
        Iterator it1 = rows.keySet().iterator();
        boolean firstRow = true;
        while (it1.hasNext()) {
            String rowName = (String) it1.next();
            if (firstRow)
                sb = sb.append("<tr><td>.</td>");
            else
                sb = sb.append("<tr><td>").append(rowName).append("</td>");
            HashMap columns = (HashMap) rows.get(rowName);
            HashMap averageColumns = null;
            if (average != null)
                averageColumns = (HashMap) average.rows.get(rowName);
            Iterator it2 = columns.keySet().iterator();
            while (it2.hasNext()) {
                String columnName = (String) it2.next();
                String value = (String) columns.get(columnName);
                if (firstRow)
                    sb = sb.append("<td>").append(columnName).append("</td>");
                else {
                    if (readOnly) {
                        sb = sb.append("<td>").append(value).append("</td>");
                    }
                    else {
                        if (average != null && averageColumns != null) {
                            Distribution d = (Distribution) averageColumns.get(columnName);
                            if (d != null && Delphi.outsideInterquartile(d.values,Integer.parseInt(value)))
                                sb = sb.append("<td bgcolor=red>");
                            else
                                sb = sb.append("<td>");
                        }
                        else
                            sb = sb.append("<td>");
                        String menuName = rowName + "!" + columnName;
                        sb = sb.append(HTMLformatter.createMenu(menuName, value, options));
                        sb = sb.append("</td>");
                    }
                }
            }
            sb = sb.append("</tr>\n");
            if (firstRow) {
                sb = sb.append("<tr><td><i>weights</i></td>\n");
                it1 = rows.keySet().iterator();
                firstRow = false;
                Iterator it3 = columnWeights.keySet().iterator();
                while (it3.hasNext()) {
                    String critereon = (String) it3.next();
                    String value = (String) columnWeights.get(critereon);
                    if (readOnly) {
                        sb = sb.append("<td>").append(value.toString()).append("</td>");
                    }
                    else {
                        if (average != null) {
                            Distribution d = (Distribution) average.weights.get(critereon);
                            if (d != null && value != null &&
                                Delphi.outsideInterquartile(d.values,Integer.parseInt(value)))
                                sb = sb.append("<td bgcolor=red>");
                            else
                                sb = sb.append("<td>");
                        }
                        else
                            sb = sb.append("<td>");
                        String menuName = critereon;
                        sb = sb.append(HTMLformatter.createMenu(menuName, value, options));
                        sb = sb.append("</td>");
                    }
                }
                sb = sb.append("</tr>\n");
            }
        }
        sb = sb.append("</table>\n");
        sb = sb.append(printRecommendations());
        return sb.toString();
    }

    /** *****************************************************************
     *  Calculate the recommended decision that results from the
     *  judgements and weights.
     */
    public Pair[] calculateRecommendations() {

          /** String keys and String values, which are String representations
           * of integers */
        HashMap rowValues = new HashMap();
        String result;
        Pair[] pairs = new Pair[rows.keySet().size()];
        int index = -1;

        Iterator it = rows.keySet().iterator();
        while (it.hasNext()) {
            String decision = (String) it.next();
            HashMap row = (HashMap) rows.get(decision);
            int total = 0;
            Iterator it2 = row.keySet().iterator();
            while (it2.hasNext()) {
                String column = (String) it2.next();
                String weightStr = (String) columnWeights.get(column);
                String valueStr = (String) row.get(column);
                if (weightStr != null && valueStr != null)
                    total = total + (Integer.valueOf(weightStr) * Integer.valueOf(valueStr));
            }
            rowValues.put(decision,String.valueOf(total));
            index++;
            pairs[index] = new Pair();
            pairs[index].str = decision;
            pairs[index].value = total;
        }
        return pairs;
    }

    /** *****************************************************************
     *  Read in an XML-formatted String
     */
    public void fromXML(BasicXMLelement xml) {

        rows = new HashMap();
        //System.out.println("INFO in Table.fromXML(): Initializing.");
        //System.out.print("INFO in Table.fromXML(): Number of rows:");
        //System.out.println(xml.subelements.size());
        for (int i = 0; i < xml.subelements.size(); i++) {
            BasicXMLelement rowXML = (BasicXMLelement) xml.subelements.get(i);
            if (rowXML.tagname.equalsIgnoreCase("row")) {
                HashMap row = new HashMap();
                String decision = (String) rowXML.attributes.get("decision");
                //System.out.println("INFO in Table.fromXML(): Number of columns: " + rowXML.subelements.size());
                for (int j = 0; j < rowXML.subelements.size(); j++) {
                    BasicXMLelement column = (BasicXMLelement) rowXML.subelements.get(j);
                    if (!column.tagname.equalsIgnoreCase("column"))
                        System.out.println("Error in Table.fromXML(): Bad element: " + column.tagname);
                    String critereon = (String) column.attributes.get("critereon");
                    String value = (String) column.attributes.get("value");
                    row.put(critereon,value);
                }
                rows.put(decision,row);
            }
            else {
                if (rowXML.tagname.equalsIgnoreCase("weight")) {
                    String critereon = (String) rowXML.attributes.get("critereon");
                    String weight = (String) rowXML.attributes.get("weight");
                    columnWeights.put(critereon,weight);
                }
                else
                    System.out.println("Error in Table.fromXML(): Bad element: " + rowXML.tagname);
            }

        }
    }

    /** *****************************************************************
     *  Create an XML-formatted String
     */
    public String toXML() {

        StringBuilder result = new StringBuilder();
        Iterator it3 = columnWeights.keySet().iterator();
        while (it3.hasNext()) {
            String critereon = (String) it3.next();
            String weight = (String) columnWeights.get(critereon);
            result.append("    <weight critereon=\"").append(critereon).append("\" weight=\"").append(weight).append("\"/>\n");
        }
        Iterator it = rows.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            HashMap row = (HashMap) rows.get(key);
            result.append("    <row decision=\"").append(key).append("\">\n");
            Iterator it2 = row.keySet().iterator();
            while (it2.hasNext()) {
                String key2 = (String) it2.next();
                String value = (String) row.get(key2);
                result.append("      <column critereon=\"").append(key2).append("\" value=\"").append(value).append("\"/>\n");
            }
            result.append("    </row>\n");
        }
        return result.toString();
    }

    /** *****************************************************************
     */
    public static void main(String args[]) {

    }

}
