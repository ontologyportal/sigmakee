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

import com.articulate.sigma.Formula;
import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.THF;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import tptp_parser.TPTPFormula;

import java.io.*;
import java.util.*;

/**
 * Class for invoking the latest version of LEO from Java
 * It should invoke a command like
 * ~/workspace/Leo-III/Leo-III-1.6/bin/leo3 /home/user/.sigmakee/KBs/SUMO.thf -t 60 -p
 * @author apease
 */

public class LEO {

    public StringBuffer qlist = null; // quantifier list in order for answer extraction
    public ArrayList<String> output = new ArrayList<>();
    public static int axiomIndex = 0;
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
        opts = executable.toString() + " " + kbFile.toString() + " -t " + Integer.toString(timeout) + " -p";
        String[] optar = opts.split(" ");
        return optar;
    }

    /** *************************************************************
     * Add an assertion for inference.
     *
     * @param userAssertionTPTP asserted formula in the TPTP/TFF/THF syntax
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

        if (debug) System.out.println("INFO in Leo.assertFormula(2):writing to file " + userAssertionTPTP);
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
                else {   // 2. Translate to THF.
                    Set<String> tptpFormulas = new HashSet<>();
                    if (tptp) {
                        THF thf = new THF();
                        for (Formula p : processedFormulas) {
                            String tptpStr = thf.oneKIF2THF(p,false,kb);
                            if (debug) System.out.println("INFO in LEO.assertFormula(2): formula " + tptpStr);
                            tptpFormulas.add(tptpStr);
                        }
                    }
                    // 3. Write to new tptp file
                    for (String theTPTPFormula : tptpFormulas) {
                        pw.print(SUMOformulaToTPTPformula.lang + "(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        String tptpstring = SUMOformulaToTPTPformula.lang + "(kb_" + kb.name + "_UserAssertion" +
                                "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
                        if (debug) System.out.println("INFO in LEO.assertFormula(2): TPTP for user assertion = " + tptpstring);
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
     * Creates a running instance of Leo.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Leo executable.
     *
     * @throws IOException should not normally be thrown unless either
     *         Leo executable or database file name are incorrect
     */
    private void run(File kbFile, int timeout) throws Exception {

        String leoex = KBmanager.getMgr().getPref("leoExecutable");
        if (StringUtil.emptyString(leoex)) {
            System.out.println("Error in Leo.run(): no executable string in preferences");
        }
        File executable = new File(leoex);
        if (!executable.exists()) {
            System.out.println("Error in Leo.run(): no executable " + leoex);
        }
        String[] cmds = createCommandList(executable, timeout, kbFile);
        System.out.println("Leo.run(): Initializing Leo with:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(true);

        Process _leo = _builder.start();
        //System.out.println("Leo.run(): process: " + _vampire);

        BufferedReader _reader = new BufferedReader(new InputStreamReader(_leo.getInputStream()));
        String line = null;
        while ((line = _reader.readLine()) != null) {
            output.add(line);
        }
        int exitValue = _leo.waitFor();
        if (exitValue != 0) {
            System.out.println("Leo.run(): Abnormal process termination");
            System.out.println(output);
        }
        System.out.println("Leo.run() done executing");
    }

    /** ***************************************************************
     * Write the THF statements to the temp-stmt.thf file
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
            System.out.println("Error in Leo.writeStatements(): " + e.getMessage());
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

        System.out.println("catFiles(): concatenating " + f1 + " and " + f2 + " into " + fout);
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

        String userAssertionTPTP = kb.name + KB._userAssertionsTHF;
        File dir = new File(KBmanager.getMgr().getPref("kbDir"));
        String fname = dir + File.separator + userAssertionTPTP;
        File ufile = new File(fname);
        if (ufile.exists())
            return FileUtil.readLines(dir + File.separator + userAssertionTPTP,false);
        else
            return new ArrayList<String>();
    }

    /** *************************************************************
     * Creates a running instance of LEO-III adding a set of statements
     * in THF language to a file and then calling LEO.
     * Note that any query must be given as a "conjecture"
     * @param stmts should be the query but the list gets expanded here with
     *              any other prior user assertions
     */
    public void run(KB kb, File kbFile, int timeout, HashSet<String> stmts) throws Exception {

        System.out.println("Leo.run(): query : " + stmts);
        String lang = "thf";
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
            System.out.println("Error in Leo.run(): null query or user assertions set");
            return;
        }
        writeStatements(stmts, lang);
        if (!kbFile.exists() || KBmanager.getMgr().infFileOld("thf"))
            THF.writeTHF(kb);
        catFiles(kbFile.toString(),stmtFile,outfile);
        File comb = new File(outfile);
        run(comb,timeout);
    }

    /** *************************************************************
     */
    public static void main (String[] args) throws Exception {

        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String lang = "thf";
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
        File kbFile = new File(dir + kbName + "." + lang);
        System.out.println("Leo.main(): first test");
        HashSet<String> query = new HashSet<>();
        query.add("thf(conj1,conjecture,?[V__X:$i, V__Y:$i] : (subclass_THFTYPE_IiioI @ V__X @ V__Y)).");
        System.out.println("Leo.main(): calling Leo with: " + kbFile + ", 30, " + query);
        LEO leo = new LEO();
        leo.run(kb, kbFile, 30, query);
        System.out.println("----------------\nLeo output\n");
        for (String l : leo.output)
            System.out.println(l);
        String queryStr = "(subclass ?X ?Y)";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.parseProofOutput(leo.output,queryStr,kb,leo.qlist);
        for (TPTPFormula step : tpp.proof) {
            System.out.println(":: " + step);
            Formula f = new Formula(step.sumo);
            System.out.println(f.format("","  ","\n"));
        }
        System.out.println("Leo.main(): bindings: " + tpp.bindings);
        //System.out.println("Leo.main(): proof: " + tpp.proof);
        System.out.println("-----------------\n");
        System.out.println("\n");
/*
        System.out.println("Leo.main(): second test");
        System.out.println(kb.askLeo("(subclass ?X Entity)",30,1));

 */

    }
}
