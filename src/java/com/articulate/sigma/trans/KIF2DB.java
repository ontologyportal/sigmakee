package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
 */
public class KIF2DB {

    // open the password DB as a server so both Sigma and SigmaNLP can access at once
    //public static final String JDBCString = "jdbc:h2:tcp://localhost/~/var/passwd";
    public static final String JDBCString = "jdbc:h2:~/var/passwd";
    public static String UserName = "sumo";
    public Connection conn = null;
    //public static Server server = null;
    public static KB kb = null;

    // Every key is for a class, the value is a map of
    // instance names to relations, the interior values are relations on instances
    // Each interior HashMap is relation name keys and second argument values -
    // class name
    //   instance name
    //     relation name
    //       list of values
    // Class-level information is put under the class "Class"
    public static Map<String,Map<String,Map<String,Set<String>>>> tables = new HashMap<>();

    /** ***************************************************************
     */
    public String getAllRels(Collection<Formula> forms) {

        Set<String> result = new HashSet();
        String rel;
        for (Formula f : forms) {
            rel = f.getStringArgument(0);
            result.add(rel + " varchar(255)");
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : result) {
            if (!first)
                sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    /** ***************************************************************
     */
    public void writeSUMOTerm(Connection conn, String term) {

        System.out.println("INFO in KIF2DB.writeSUMOTerm(): checking term " + term);
        boolean inst = kb.isInstance(term);
        boolean relationP = kb.isRelation(term);
        Collection<String> parents = kb.immediateParents(term);
        //if (kb.childOf(term,"BinaryRelation") && kb.isInstance(term))
        //    writeRelations(pw,term);
        //if (Character.isUpperCase(term.charAt(0))) {
        Collection<Formula> forms = kb.ask("arg",1,term);
        System.out.println("INFO in KIF2DB.writeSUMOTerm(): formulas " + forms);
        Map<String,Map<String,Set<String>>> table;
        Map<String, Set<String>> rels;
        Set<String> vals;
        String rel, arg;
        for (Formula f : forms) {
            if (!f.isBinary()) {
                if (f.getArgument(0).equals("domain")) { // convert domain statements to a binary relation
                                                         // assuming it to be on a binary relation

                }
                continue;
            }
            rel = f.getStringArgument(0);
            if (f.complexArgumentsToArrayList(0).size() > 1) {
                arg = f.getStringArgument(2);
                if (!inst) {
                    if (!tables.keySet().contains("Class")) {
                        table = new HashMap<>();
                        tables.put("Class", table);
                    }
                    else {
                        table = tables.get("Class");
                        //String s = "create table " + term + "(" + rel + ")";
                    }

                    if (!table.keySet().contains(term)) {
                        rels = new HashMap<>();
                        table.put(term, rels);
                    }
                    else {
                        rels = table.get(term);
                        //String s = "create table " + term + "(" + rel + ")";
                    }

                    if (!rels.keySet().contains(rel)) {
                        vals = new HashSet<>();
                        rels.put(rel, vals);
                    }
                    else {
                        vals = rels.get(rel);
                        //String s = "create table " + term + "(" + rel + ")";
                    }
                    vals.add(arg);
                }
                else {
                    System.out.println("INFO in KIF2DB.writeSUMOTerm(): it's an instance: " + term);
                    System.out.println("INFO in KIF2DB.writeSUMOTerm(): parents: " + parents);
                    for (String p : parents) {
                        if (!tables.keySet().contains(p)) {
                            table = new HashMap<>();
                            tables.put(p, table);
                        }
                        else {
                            table = tables.get(p);
                            //String s = "create table " + term + "(" + rel + ")";
                        }
                        if (!table.keySet().contains(term)) {
                            rels = new HashMap<>();
                            table.put(term, rels);
                        }
                        else {
                            rels = table.get(term);
                            //String s = "create table " + term + "(" + rel + ")";
                        }

                        if (!rels.keySet().contains(rel)) {
                            vals = new HashSet<>();
                            rels.put(rel, vals);
                        }
                        else {
                            vals = rels.get(rel);
                            //String s = "create table " + term + "(" + rel + ")";
                        }
                        vals.add(arg);
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void writeSQLfile(PrintWriter pw) {

    }

    /** ***************************************************************
     */
    public void writeCSVfile(PrintWriter pw) {

        Map<String,Map<String,Set<String>>> insts;
        Map<String,Set<String>> rels;
        Set<String> vals;
        for (String c : tables.keySet()) {
            insts = tables.get(c);
            for (String i : insts.keySet()) {
                rels = insts.get(i);
                for (String rel : rels.keySet()) {
                    vals = rels.get(rel);
                    for (String val : vals)
                        pw.println(i + ", " + rel + ", " + val);
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void writeKB(Connection conn, String path) throws IOException {

        System.out.println("INFO in KIF2DB.writeKB(): writing " + path);

        Set<String> kbterms = kb.getTerms();
        System.out.println("INFO in KIF2DB.writeKB(): " + kbterms.size());
        for (String term : kbterms) {
            writeSUMOTerm(conn,term);
        }
        try (Writer fw = new FileWriter(path);
             PrintWriter pw = new PrintWriter(fw)) {
            writeCSVfile(pw);
        }
    }

    /** *****************************************************************
     */
    public static void main (String[] args) {

        KIF2DB kif2db = new KIF2DB();
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        kb = KBmanager.getMgr().getKB(kbName);
        System.out.println("KIF2DB()");
        try {
            //server = Server.createTcpServer().start();
            Class.forName("org.h2.Driver");
            kif2db.conn = DriverManager.getConnection(JDBCString, UserName, "");
            System.out.println("main(): Opened DB " + JDBCString);
            String path = KButilities.SIGMA_HOME + File.separator + kbName + "DB.csv";
            kif2db.writeKB(kif2db.conn,path);
        }
        catch (IOException | ClassNotFoundException | SQLException e) {
            System.err.println("Error in KIF2DB(): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
