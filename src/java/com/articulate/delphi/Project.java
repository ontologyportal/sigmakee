
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
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public class Project {

    public String name;
    public String description;
      /** String user names point to Table values.  */
    public HashMap tables = new HashMap();
  
    /** ***************************************************************** 
     * Convert the project to an XML-formatted String.  Calls on 
     * Table.toXML()
     */
    public String toXML() {

        StringBuffer result = new StringBuffer();
        result.append("<project name=\"" + name + "\">\n");
        result.append("  <description>\n  " + description + "\n  </description>\n");
        Iterator it = tables.keySet().iterator();
        while (it.hasNext()) {
            String username = (String) it.next();
            Table t = (Table) tables.get(username);
            result.append("  <table username=\"" + username + "\">\n");
            result.append(t.toXML());
            result.append("  </table>\n");
        }
        result.append("</project>\n");
        return result.toString();
    }

    /** ***************************************************************** 
     * Return a Table which is the average of the judgements of ever
     * user other than the given username.
     */
    public TableAverage average(String username) {

        TableAverage sum = new TableAverage(); // A HashMap of HashMaps but its values will
                                               // be instances of Pair, rather than Strings.
        Iterator it = tables.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (!key.equals(username)) {
                Table t = (Table) tables.get(key);
                System.out.println("INFO in Project.average(): Adding table " + key);
                sum.addTable(t);
            }
        }
        return sum;
    }

    /** ***************************************************************** 
     *  Read in an XML-formatted String
     */
    public void fromXML(BasicXMLelement projectXML) {

        tables = new HashMap();
        //System.out.println("INFO in Project.fromXML(): Initializing.");
        if (projectXML.tagname.equalsIgnoreCase("project")) {
            name = (String) projectXML.attributes.get("name");
            //System.out.print("INFO in Project.fromXML(): Number of tables:");
            //System.out.println(projectXML.subelements.size());
            for (int i = 0; i < projectXML.subelements.size(); i++) {
                BasicXMLelement tableXML = (BasicXMLelement) projectXML.subelements.get(i);
                if (tableXML.tagname.equalsIgnoreCase("table")) {
                    String username = (String) tableXML.attributes.get("username");
                    Table table = new Table();
                    table.fromXML(tableXML);
                    tables.put(username,table);
                }
                else {
                    if (tableXML.tagname.equalsIgnoreCase("description")) 
                        description = tableXML.contents;
                    else
                        System.out.println("Error in Project.fromXML(): Bad element: " + tableXML.tagname);
                }
            }        
        }
        else
            System.out.println("Error in Project.fromXML(): Bad element: " + projectXML.tagname);        
    }

    /** ***************************************************************** 
     */
    public static void main(String args[]) {

    }

}
