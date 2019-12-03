/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

package com.articulate.sigma.tp;

import com.articulate.sigma.*;

import java.io.*;
import java.util.*;

/**
 * Class for invoking the latest research version of Vampire from Java
 * A previous version invoked the KIF version of Vampire from Java
 * but that's 15 years old now.  The current Vampire does TPTP3 output
 * instead of XML.
 
 * @author Andrei Voronkov
 * @since 14/08/2003, Acapulco
 * @author apease
 */

public class Vampire {

    public ArrayList<String> output = new ArrayList<>();

    /** *************************************************************
     * To obtain a new instance of Vampire, use the static factory
     * method Vampire.getNewInstance().
     */
    private Vampire () {
    }

    /** *************************************************************
     */
    private static String[] createCommandList(File executable, String opts, File kbFile) {

        String[] optar = opts.split(" ");
        String[] cmds = new String[optar.length + 2];
        cmds[0] = executable.toString();
        for (int i = 0; i < optar.length; i++)
            cmds[i+1] = optar[i];
        cmds[optar.length+1] =  kbFile.toString();
        return cmds;
    }

    /** *************************************************************
     */
    private static String[] createCommandList(File executable, int timeout, File kbFile) {

        String opts = "--mode casc -t";
        String[] optar = opts.split(" ");
        String[] cmds = new String[optar.length + 3];
        cmds[0] = executable.toString();
        for (int i = 0; i < optar.length; i++)
            cmds[i+1] = optar[i];
        cmds[optar.length+1] = Integer.toString(timeout);
        cmds[optar.length+2] = kbFile.toString();
        return cmds;
    }

    /** *************************************************************
     * Creates a running instance of Vampire.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Vampire executable.
     *
     * @throws IOException should not normally be thrown unless either
     *         Vampire executable or database file name are incorrect
     */
    private Vampire (File kbFile, int timeout) throws Exception {

        String vampex = KBmanager.getMgr().getPref("vampire");
        if (StringUtil.emptyString(vampex)) {
            System.out.println("Error in Vampire: no executable string in preferences");
        }
        File executable = new File(vampex);
        if (!executable.exists()) {
            System.out.println("Error in Vampire: no executable " + vampex);
        }
        String[] cmds = createCommandList(executable, timeout, kbFile);
        System.out.println("Initializing Vampire with:\n" + Arrays.toString(cmds));
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmds);

        BufferedReader _reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedWriter _writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
        BufferedReader _error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String line = null;
        while ((line = _reader.readLine()) != null) {
            output.add(line);
        }
        while ((line = _error.readLine()) != null) {
            output.add(line);
        }
        int exitValue = proc.waitFor();
        //if (exitValue != 0) {
        //    System.out.println("Abnormal process termination");
        //}
        boolean inproof = false;
        for (String s : output) {
            if (s.startsWith("% SZS") || inproof) {
                if (!s.startsWith("tff(func_def") && !s.startsWith("tff(pred_def"))
                    System.out.println(s);
            }
            if (s.startsWith("% SZS output start"))
                inproof = true;
            if (s.startsWith("% SZS output end"))
                inproof = false;
        }
        System.out.println("Vampire() done executing");
    }

    /** ***************************************************************
     */
    public void writeStatements(HashSet<String> stmts, String type) {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = KBmanager.getMgr().getPref("kbDir");
        String fname = "temp-stmt." + type;

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            for (String s : stmts)
                pw.println(s);
        }
        catch (Exception e) {
            System.out.println("Error in writeStatements(): " + e.getMessage());
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     */
    public void catFiles(String f1, String f2, String fout) throws Exception {

        PrintWriter pw = new PrintWriter(fout);
        BufferedReader br = new BufferedReader(new FileReader(f1));
        String line = br.readLine();
        while (line != null) {
            pw.println(line);
            line = br.readLine();
        }

        br = new BufferedReader(new FileReader(f2));
        line = br.readLine();
        while(line != null) {
            pw.println(line);
            line = br.readLine();
        }
        pw.flush();
        br.close();
        pw.close();
    }

    /** *************************************************************
     * Creates a running instance of Vampire adding a set of statements
     * in TFF or TPTP language to a file and then calling Vampire.
     * Note that any query must be given as a "conjecture"
     */
    public Vampire (File kbFile, int timeout, HashSet<String> stmts) throws Exception {

        String type = "tff";
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String outfile = dir + "temp-comb." + type;
        String stmtFile = dir + "temp-stmt." + type;
        writeStatements(stmts, type);
        catFiles(kbFile.toString(),stmtFile,outfile);
        File comb = new File(outfile);
        new Vampire(comb,timeout);
    }

    /** *************************************************************
     */
    public static void main (String[] args) throws Exception {

        /*
        String initialDatabase = "SUMO-v.kif";
        Vampire vampire = Vampire.getNewInstance(initialDatabase);
        System.out.print(vampire.submitQuery("(holds instance ?X Relation)",5,2));

        // System.out.print(vampire.assertFormula("(human Socrates)"));
        // System.out.print(vampire.assertFormula("(holds instance Adam Human)"));
        // System.out.print(vampire.submitQuery("(human ?X)", 1, 2));
        // System.out.print(vampire.submitQuery("(holds instance ?X Human)", 5, 2));
        vampire.terminate();
        */
        KBmanager.getMgr().initializeOnce();
        File s = new File("/home/apease/.sigmakee/KBs/SUMO.tptp");
        if (!s.exists())
            System.out.println("Vampire.main(): no such file: " + s);
        else {
            System.out.println("Vampire.main(): first test");
            HashSet<String> query = new HashSet<>();
            query.add("tff(conj1,conjecture,?[V__X] : (s__subclass(V__X,s__Entity))).");
            System.out.println("Vampire.main(): calling Vampire with: " + s + ", 30, " + query);
            Vampire vampire = new Vampire(s, 30, query);

            System.out.println("Vampire.main(): second test");
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println(kb.askVampire("(subclass ?X Entity)",30,1));
            //vampire.terminate();
        }
    }
    
}
