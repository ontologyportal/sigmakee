package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
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
    public static HashMap<String,HashMap<String,HashMap<String,HashSet<String>>>> tables = new HashMap<>();

    /** ***************************************************************
     */
    public String getAllRels(Collection<Formula> forms) {

        HashSet<String> result = new HashSet();
        for (Formula f : forms) {
            String rel = f.getArgument(0);
            result.add(rel + " varchar(255)");
        }

        StringBuffer sb = new StringBuffer();
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
        for (Formula f : forms) {
            if (!f.isBinary()) {
                if (f.getArgument(0).equals("domain")) { // convert domain statements to a binary relation
                                                         // assuming it to be on a binary relation

                }
                continue;
            }
            String rel = f.getArgument(0);
            if (f.argumentsToArrayList(0).size() > 1) {
                String arg = f.getArgument(2);
                if (!inst) {
                    HashMap<String,HashMap<String,HashSet<String>>> table = null;
                    if (!tables.keySet().contains("Class")) {
                        table = new HashMap<String,HashMap<String,HashSet<String>>>();
                        tables.put("Class", table);
                    }
                    else {
                        table = tables.get("Class");
                        //String s = "create table " + term + "(" + rel + ")";
                    }

                    HashMap<String, HashSet<String>> rels = null;
                    if (!table.keySet().contains(term)) {
                        rels = new HashMap<String, HashSet<String>>();
                        table.put(term, rels);
                    }
                    else {
                        rels = table.get(term);
                        //String s = "create table " + term + "(" + rel + ")";
                    }

                    HashSet<String> vals = null;
                    if (!rels.keySet().contains(rel)) {
                        vals = new HashSet<String>();
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
                        HashMap<String,HashMap<String,HashSet<String>>> table = null;
                        if (!tables.keySet().contains(p)) {
                            table = new HashMap<String,HashMap<String,HashSet<String>>>();
                            tables.put(p, table);
                        }
                        else {
                            table = tables.get(p);
                            //String s = "create table " + term + "(" + rel + ")";
                        }
                        HashMap<String, HashSet<String>> rels = null;
                        if (!table.keySet().contains(term)) {
                            rels = new HashMap<String, HashSet<String>>();
                            table.put(term, rels);
                        }
                        else {
                            rels = table.get(term);
                            //String s = "create table " + term + "(" + rel + ")";
                        }

                        HashSet<String> vals = null;
                        if (!rels.keySet().contains(rel)) {
                            vals = new HashSet<String>();
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

        for (String c : tables.keySet()) {
            HashMap<String,HashMap<String,HashSet<String>>> insts = tables.get(c);
            for (String i : insts.keySet()) {
                HashMap<String,HashSet<String>> rels = insts.get(i);
                for (String rel : rels.keySet()) {
                    HashSet<String> vals = rels.get(rel);
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
        FileWriter fw = new FileWriter(path);
        PrintWriter pw = new PrintWriter(fw);
        writeCSVfile(pw);
        pw.close();
    }

    /** *****************************************************************
     */
    public static void main (String[] args) {

        KIF2DB kif2db = new KIF2DB();
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB("SUMO");
        System.out.println("KIF2DB()");
        try {
            //server = Server.createTcpServer().start();
            Class.forName("org.h2.Driver");
            kif2db.conn = DriverManager.getConnection(JDBCString, UserName, "");
            System.out.println("main(): Opened DB " + JDBCString);
            String path = System.getenv("SIGMA_HOME") + File.separator + "SUMODB.csv";
            kif2db.writeKB(kif2db.conn,path);
        }
        catch (Exception e) {
            System.out.println("Error in KIF2DB(): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
