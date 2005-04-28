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
 *  The main container class for the Delphi system
 */
public class Delphi {

    public static Delphi delphi = null;
      /** Relates String keys to Project values */
    public HashMap projects = new HashMap();

    /** ***************************************************************** 
     */
    public static Delphi getInstance() {

        if (delphi == null) {
            delphi = new Delphi();
            try {
                delphi.readProjects();
            }
            catch (IOException ioe) {
                System.out.println("Error in Delphi.getInstance(): IO exception: " + ioe.getMessage());
            }
        }
        return delphi;
    }

    /** ***************************************************************** 
     */
    public static boolean odd(int val) {
        
        if ((val / 2) * 2 == val) 
            return false;
        else
            return true;
    }

    /** ***************************************************************** 
     */
    public static Integer[] fromObjectArray(Object[] ob) {

        Integer[] in = new Integer[ob.length];
        for (int i = 0; i < ob.length; i++) {
            in[i] = (Integer) ob[i];
        }
        return in;
    }
    /** ***************************************************************** 
     */
    public static int median(Integer[] distribution) {

        int length = distribution.length;
        int median = -1;
        if (odd(length+1)) { 
            median = (distribution[(length / 2) - 1].intValue() + distribution[length / 2].intValue()) / 2;
        }
        else {
            median = distribution[length / 2].intValue();
        }
        return median;
    }

    /** ***************************************************************** 
     *  Determine whether an int is outside the interquartile range
     *  of a distribution of int(s).  See 
     *  http://www.statcan.ca/english/edu/power/ch12/range.htm
     *  This routine necessitates totally bogus type conversions because
     *   - there's no sort routine on Collections, just []arrays
     *   - Collections can be converted to []array but not the reverse
     *   - casts can't be done on []arrays, just on their elements
     */
    public static boolean outsideInterquartile(ArrayList distribution, int value) {

        if (distribution.size() < 1) return true;
        if (distribution.size() == 1) {
            Integer comparisonValue = (Integer) distribution.get(0);
            return comparisonValue.intValue() != value;
        }
        System.out.println("INFO in Delphi.outsideInterquartile(): distribution: " + distribution);
        Object[] ob = distribution.toArray();
        Integer[] ar = new Integer[ob.length];
        ar = Delphi.fromObjectArray(ob);
        Arrays.sort(ar);        
        int median = median(ar);
        int lowerMiddle = (ar.length / 2) - 1;
        int upperMiddle = -1;
        if (odd(ar.length)) 
            upperMiddle = lowerMiddle + 2;
        else
            upperMiddle = lowerMiddle + 1;

        System.out.println("INFO in Delphi.outsideInterquartile(): median: " + median);
        System.out.println("INFO in Delphi.outsideInterquartile(): upperMiddle: " + upperMiddle + " lowerMiddle:" + lowerMiddle);
        Integer[] lowerDistribution = new Integer[lowerMiddle+1];
        Integer[] upperDistribution = new Integer[lowerMiddle+1];
        for (int i = 0; i <= lowerMiddle; i++) {
            lowerDistribution[i] = ar[i];
            upperDistribution[i] = ar[i+upperMiddle];
        }
        int lowerQuartile = median(lowerDistribution);
        int upperQuartile = median(upperDistribution);
        System.out.println("INFO in Delphi.outsideInterquartile(): upperDistribution: " + Arrays.asList(upperDistribution)); 
        System.out.println("INFO in Delphi.outsideInterquartile(): lowerDistribution: " + Arrays.asList(lowerDistribution)); 
        System.out.println("INFO in Delphi.outsideInterquartile(): lowerQuartile: " + lowerQuartile + 
                           " upperQuartile: " + upperQuartile);
        if ((value <= upperQuartile) && (value >= lowerQuartile)) 
            return false;
        else
            return true;
    }

    /** ***************************************************************** 
     */
    public ArrayList getColumnNames(String projectName, String username) {

        ArrayList result = new ArrayList();

        Project p = (Project) projects.get(projectName);
        Table t = (Table) p.tables.get(username);
        if (t != null && t.columnWeights != null) 
            result.addAll(t.columnWeights.keySet());
        return result;
    }

    /** ***************************************************************** 
     */
    public void save() throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        String fname = "projects.xml";

        System.out.println("INFO in Delphi.save: Writing delphi project file.");
        try {
            fw = new FileWriter(fname);
            pw = new PrintWriter(fw);
            Iterator it = projects.keySet().iterator();
            while (it.hasNext()) {
                String projectName = (String) it.next();
                Project project = (Project) projects.get(projectName);
                pw.println(project.toXML());
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fname + ". " + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }
    /** ***************************************************************** 
     */
    public void addColumn(String username, String projectName, String text) {

        Project project = (Project) projects.get(projectName);
        if (project != null) {
            Table table = (Table) project.tables.get(username);
            if (table == null) { 
                table = new Table();
                table.username = username;
                project.tables.put(username,table);
            }
            table.addColumn(text);
        }
    }

    /** ***************************************************************** 
     */
    public void addRow(String username, String projectName, String text) {

        Project project = (Project) projects.get(projectName);
        if (project != null) {
            Table table = (Table) project.tables.get(username);
            if (table == null) { 
                table = new Table();
                table.username = username;
                project.tables.put(username,table);
            }        
            table.addRow(text);
        }
    }

    /** ***************************************************************** 
     * Set the value of a rating in a row and column of a specific project
     * and user's table.
     */
    public void setValue(String projectName, String username, String rowcolumn, String value) {

        Project project = (Project) projects.get(projectName);
        Table table = (Table) project.tables.get(username);
        String rowKey = rowcolumn.substring(0,rowcolumn.indexOf("!"));
        String column = rowcolumn.substring(rowcolumn.indexOf("!")+1,rowcolumn.length());

        HashMap row = (HashMap) table.rows.get(rowKey);
        row.put(column,value);
    }

    /** ***************************************************************** 
     * Set the weight of a column of a specific project
     * and user's table.
     */
    public void setWeight(String projectName, String username, String column, String value) {

        Project project = (Project) projects.get(projectName);
        Table table = (Table) project.tables.get(username);

        table.columnWeights.put(column,value);
    }

    /** ***************************************************************** 
     */
    public void fromXML(String xml) {

        projects = new HashMap();
        BasicXMLparser projectsXML = new BasicXMLparser(xml);
        for (int i = 0; i < projectsXML.elements.size(); i++) {
            BasicXMLelement element = (BasicXMLelement) projectsXML.elements.get(i);
            if (element.tagname.equalsIgnoreCase("project")) {
                Project p = new Project();
                p.name = (String) element.attributes.get("name");
                p.fromXML(element);
                projects.put(p.name,p);
            }
        }
    }

    /** ***************************************************************** 
     */
    public void readProjects() throws IOException {

        String fname = System.getProperty("user.dir") + File.separator + "projects.xml";
        StringBuffer xml = new StringBuffer();
        File f = new File(fname);
        if (!f.exists()) 
            return;
        System.out.println("INFO in Delphi.readProjects(): Reading: " + fname);
        BufferedReader br = new BufferedReader(new FileReader(fname));

        try {
            do {
                String line = br.readLine();
                xml.append(line + "\n");
            } while (br.ready());
        }
        catch (java.io.IOException e) {
            System.out.println("Error in Delphi.readProjects(): IO exception parsing file " + fname);
        }
        finally {
            if (br != null) 
                br.close();
        }
        //System.out.println(xml.toString()); 
        fromXML(xml.toString());
    }

    /** ***************************************************************** 
     */
    public static void main(String args[]) {

        ArrayList ar = new ArrayList();
        ar.add(new Integer(34)); ar.add(new Integer(47)); ar.add(new Integer(1));
        ar.add(new Integer(15)); ar.add(new Integer(57)); ar.add(new Integer(24));
        ar.add(new Integer(20)); ar.add(new Integer(11)); ar.add(new Integer(19));
        ar.add(new Integer(50)); ar.add(new Integer(28)); ar.add(new Integer(37));

        System.out.println(Delphi.outsideInterquartile(ar,6));
        
        ar = new ArrayList();
        ar.add(new Integer(1)); ar.add(new Integer(3)); ar.add(new Integer(5));
        ar.add(new Integer(7)); ar.add(new Integer(9)); ar.add(new Integer(11));

        System.out.println(Delphi.outsideInterquartile(ar,6));
    }

}

