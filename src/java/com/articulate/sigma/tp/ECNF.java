package com.articulate.sigma.tp;
/** This code is copyright Articulate Software (c) 2014.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also https://github.com/ontologyportal/sigmakee

Authors:
Adam Pease
*/

import com.articulate.sigma.Formula;
import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

public class ECNF {

    private ProcessBuilder _builder;
    private Process _eprover;
    private final BufferedReader _reader = null;
    private final Writer _writer = null;
    public static String eproverpath = null;
    private static String kbdir;
    private static int axiomIndex = 0;
    public List<String> output = new ArrayList<>();
    public StringBuilder qlist = null;

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (String s : output)
            sb.append(s).append("\n");
        return sb.toString();
    }

    /** *************************************************************
     * Create a new batch specification file, and create a new running
     * instance of EProver.
     *
     * @throws IOException should not normally be thrown unless either
     *         EProver executable or database file name are incorrect
     *
     * e_ltb_runner --interactive LTBSampleInput-AP.txt
     */
    public ECNF() {

        kbdir = KBmanager.getMgr().getPref("kbDir");
    }

    /** *************************************************************
     * Terminate this instance of EProver. After calling this function
     * no further assertions or queries can be done.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate() throws IOException {

        if (this._eprover == null)
            return;

        System.out.println();
        System.out.println("TERMINATING " + this);
        try (_reader; _writer) {
            _writer.write("quit\n");
            _writer.write("go.\n");
            _writer.flush();
            System.out.println("DESTROYING the Process " + _eprover);
            System.out.println();
            _eprover.destroy();
            output.clear();
            qlist.setLength(0);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     * don't include a timeout if @param timeout is 0
     */
    private static String[] createCustomCommandList(File executable,
                                                    int timeout, File kbFile,
                                                    Collection<String> commands) {

        String space = " ";
        StringBuilder opts = new StringBuilder();
        for (String s : commands)
            opts.append(s).append(space);
        if (timeout != 0) {
            opts.append("-t").append(space);
            opts.append(timeout).append(space);
        }
        opts.append(kbFile.toString());
        String[] optar = opts.toString().split(" ");
        String[] cmds = new String[optar.length + 1];
        cmds[0] = executable.toString();
        System.arraycopy(optar, 0, cmds, 1, optar.length);
        return cmds;
    }

    /** *************************************************************
     * Creates a running instance of Eprover with custom command line
     * options.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Eprover executable.
     *
     * @throws IOException should not normally be thrown unless either
     *         Eprover executable or database file name are incorrect
     */
    public void runCustom(File kbFile, int timeout, Collection<String> commands) throws Exception {

        output = new ArrayList<>();
        String eprover = KBmanager.getMgr().getPref("eprover");
        if (StringUtil.emptyString(eprover)) {
            System.err.println("Error in ECNF.runCustom(): no executable string in preferences");
        }
        File executable = new File(eprover);
        if (!executable.exists()) {
            System.err.println("Error in ECNF.runCustom(): no executable " + eprover);
        }
        String[] cmds = createCustomCommandList(executable, timeout, kbFile, commands);
        System.out.println("ECNF.runCustom(): Custom command list:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(true);

        Process _eprover = _builder.start();
        //System.out.println("Eprover.run(): process: " + _eprover);

        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                output.add(line);
            }
        }
        int exitValue = _eprover.waitFor();
        if (exitValue != 0) {
            System.err.println("Error in ECNF.runCustom(): Abnormal process termination");
            System.err.println(output);
        }
        System.out.println("ECNF.runCustom() done executing");
    }

    /** *************************************************************
     * A simple test. Works as follows:
     * <ol>
     *   <li>start E;</li>
     *   <li>make an assertion;</li>
     *   <li>submit a query;</li>
     *   <li>terminate E</li>
     * </ol>
     */
    public static void main(String[] args) throws Exception {

        try {
            System.out.println("INFO in ECNF.main()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("------------- INFO in ECNF.main() completed initialization--------");
            ECNF eprover = new ECNF();
            eprover.terminate();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }
}
