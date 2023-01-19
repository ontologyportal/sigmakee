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
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

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

    public StringBuffer qlist = null; // quantifier list in order for answer extraction
    public ArrayList<String> output = new ArrayList<>();
    public static int axiomIndex = 0;
    public enum ModeType {AVATAR, CASC}; // Avatar is faster but doesn't provide answer variables
    public static ModeType mode = ModeType.CASC;
    public static boolean debug = false;

    /** *************************************************************
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        for (String s : output)
            sb.append(s + "\n");
        return sb.toString();
    }

    /** *************************************************************
     */
    private static String[] createCommandList(File executable, int timeout, File kbFile) {

        String opts = "";
        if (mode == ModeType.AVATAR)
            opts = "--proof tptp -t";
        if (mode == ModeType.CASC)
            opts = "--avatar off -qa answer_literal --mode casc --proof tptp -t";
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
     * Add an assertion for inference.
     *
     * @param userAssertionTPTP asserted formula in the TPTP/TFF syntax
     * @param kb Knowledge base
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp convert formula to TPTP if tptp = true
     * @return true if all assertions are added for inference
     *
     * TODO: This function might not be necessary if we find a way to
     * directly add assertion into opened inference engine (e_ltb_runner)
     */
    public static boolean assertFormula(String userAssertionTPTP, KB kb,
                                 ArrayList<Formula> parsedFormulas, boolean tptp) {

        if (debug) System.out.println("INFO in Vampire.assertFormula(2):writing to file " + userAssertionTPTP);
        boolean allAdded = false;
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)));
            HashSet<Formula> processedFormulas = new HashSet();
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                FormulaPreprocessor fp = new FormulaPreprocessor();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP/TFF.
                    Set<String> tptpFormulas = new HashSet<>();
                    if (tptp) {
                        SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                        for (Formula p : processedFormulas) {
                            if (!p.isHigherOrder(kb)) {
                                String tptpStr = stptp.tptpParseSUOKIFString(p.getFormula(), false);
                                if (debug) System.out.println("INFO in Vampire.assertFormula(2): formula " + tptpStr);
                                tptpFormulas.add(tptpStr);
                            }
                        }
                    }
                    // 3. Write to new tptp file
                    for (String theTPTPFormula : tptpFormulas) {
                        pw.print(SUMOformulaToTPTPformula.lang + "(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        String tptpstring = SUMOformulaToTPTPformula.lang + "(kb_" + kb.name + "_UserAssertion" +
                                "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
                        if (debug) System.out.println("INFO in Vampire.assertFormula(2): TPTP for user assertion = " + tptpstring);
                    }
                    pw.flush();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null) pw.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return allAdded;
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
    private void run(File kbFile, int timeout) throws Exception {

        String vampex = KBmanager.getMgr().getPref("vampire");
        if (StringUtil.emptyString(vampex)) {
            System.out.println("Error in Vampire.run(): no executable string in preferences");
        }
        File executable = new File(vampex);
        if (!executable.exists()) {
            System.out.println("Error in Vampire.run(): no executable " + vampex);
        }
        String[] cmds = createCommandList(executable, timeout, kbFile);
        System.out.println("Vampire.run(): Initializing Vampire with:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(true);

        Process _vampire = _builder.start();
        //System.out.println("Vampire.run(): process: " + _vampire);

        BufferedReader _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()));
        String line = null;
        while ((line = _reader.readLine()) != null) {
            output.add(line);
        }
        int exitValue = _vampire.waitFor();
        if (exitValue != 0) {
            System.out.println("Vampire.run(): Abnormal process termination");
            System.out.println(output);
        }
        System.out.println("Vampire.run() done executing");
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
     * Read in two files and write their contents to a new file
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
        while (line != null) {
            pw.println(line);
            line = br.readLine();
        }
        pw.flush();
        br.close();
        pw.close();
    }

    /** *************************************************************
     */
    public List<String> getUserAssertions(KB kb) {

        String userAssertionTPTP = kb.name + KB._userAssertionsTPTP;
        if (SUMOKBtoTPTPKB.lang.equals("tff"))
            userAssertionTPTP = kb.name + KB._userAssertionsTFF;
        File dir = new File(KBmanager.getMgr().getPref("kbDir"));
        String fname = dir + File.separator + userAssertionTPTP;
        File ufile = new File(fname);
        if (ufile.exists())
            return FileUtil.readLines(dir + File.separator + userAssertionTPTP,false);
        else
            return new ArrayList<String>();
    }

    /** *************************************************************
     * Creates a running instance of Vampire adding a set of statements
     * in TFF or TPTP language to a file and then calling Vampire.
     * Note that any query must be given as a "conjecture"
     */
    public void run(KB kb, File kbFile, int timeout, HashSet<String> stmts) throws Exception {

        String lang = "tff";
        if (SUMOKBtoTPTPKB.lang.equals("fof"))
            lang = "tptp";
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String outfile = dir + "temp-comb." + lang;
        String stmtFile = dir + "temp-stmt." + lang;
        File fout = new File(outfile);
        if (fout.exists())
            fout.delete();
        File fstmt = new File(stmtFile);
        if (fstmt.exists())
            fstmt.delete();
        List<String> userAsserts = getUserAssertions(kb);
        if (userAsserts != null && stmts != null)
            stmts.addAll(userAsserts);
        else {
            System.out.println("Error in Vampire.run(): null query or user assertions set");
            return;
        }
        writeStatements(stmts, lang);
        catFiles(kbFile.toString(),stmtFile,outfile);
        File comb = new File(outfile);
        run(comb,timeout);
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
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String lang = "tff";
        if (SUMOKBtoTPTPKB.lang.equals("fof"))
            lang = "tptp";
        String outfile = dir + "temp-comb." + lang;
        String stmtFile = dir + "temp-stmt." + lang;
        File f1 = new File(outfile);
        f1.delete();
        File f2 = new File(stmtFile);
        f2.delete();
        File f3 = new File(dir + kbName + KB._userAssertionsString);
        f3.delete();
        File f4 = new File(dir + kbName + KB._userAssertionsTPTP);
        f4.delete();
        File s = new File(dir + kbName + "." + lang);
        if (!s.exists())
            System.out.println("Vampire.main(): no such file: " + s);
        else {
            System.out.println("Vampire.main(): first test");
            HashSet<String> query = new HashSet<>();
            query.add("tff(conj1,conjecture,?[V__X, V__Y] : (s__subclass(V__X,V__Y))).");
            System.out.println("Vampire.main(): calling Vampire with: " + s + ", 30, " + query);
            Vampire vampire = new Vampire();
            vampire.run(kb, s, 30, query);
            System.out.println("----------------\nVampire output\n");
            for (String l : vampire.output)
                System.out.println(l);
            String queryStr = "(subclass ?X ?Y)";
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output,queryStr,kb,vampire.qlist);
            System.out.println("Vampire.main(): bindings: " + tpp.bindings);
            System.out.println("Vampire.main(): proof: " + tpp.proof);
            System.out.println("-----------------\n");
            System.out.println("\n");

            System.out.println("Vampire.main(): second test");
            System.out.println(kb.askVampire("(subclass ?X Entity)",30,1));
            //vampire.terminate();
        }
    }
}
