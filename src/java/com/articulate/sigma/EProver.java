package com.articulate.sigma;
/** This code is copyright Articulate Software (c) 2012.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class EProver extends InferenceEngine {

    private Process _eprover;
    private BufferedReader _reader; 
    private BufferedWriter _writer; 
    private BufferedReader _error;
    private String _cliOptions;
    
    public static class EProverFactory extends EngineFactory {

        @Override
        public InferenceEngine createWithFormulas(Iterable formulaSource) {
                return EProver.getNewInstanceWithFormulas(formulaSource);
        }

        @Override
        public InferenceEngine createFromKBFile(String kbFileName) {
                return EProver.getNewInstance(kbFileName);
        }
    }

    public static EngineFactory getFactory() {
            return new EProverFactory();
    }

    /** *************************************************************
     * This static factory method returns a new EProver instance.
     *
     * @param kbFileName The complete (absolute) pathname of the KB
     * file that will be used to populate the inference engine
     * instance with assertions.  As of 7/2012, E must start from
     * scratch loading a KB for every new query, so this is bypassed.
     *
     * @throws IOException should not normally be thrown unless either
     *         EProver executable or database file name are incorrect
     */
    public static EProver getNewInstance (String kbFileName) {

    	EProver epr = null;
        String error = null;
        try {
            File kbFile = null;
            if (error == null) {
                kbFile = new File(kbFileName);
                if (!kbFile.exists()) {
                    error = ("The file " + kbFileName + " does not exist");
                    System.out.println("INFO in EProver.getNewInstance(): " + error);
                    KBmanager.getMgr().setError( KBmanager.getMgr().getError()
                                                 + "\n<br/>" + error + "\n<br/>");
                }
            }
            if (error == null) {
                KIF kif = new KIF();
                kif.setParseMode(KIF.RELAXED_PARSE_MODE);
                kif.readFile(kbFile.getCanonicalPath());
                Iterable<String> formulaSource = kif.formulaMap.keySet();                
                epr = getNewInstanceWithFormulas(formulaSource);
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return epr;
    }

    /** *************************************************************
     *  */
    public static EProver getNewInstanceWithFormulas(Iterable formulaSource) {

    	EProver epr = null;
        String error = null;

        try {
            String execPathname = KBmanager.getMgr().getPref("eprover");
            if (!StringUtil.isNonEmptyString(execPathname)) {
                error = "No pathname has been set for \"eprover\"";
                System.out.println("Error in EProver.getNewInstanceWithFormulas(): " + error);
                KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                            + "\n<br/>" + error + "\n<br/>");
            }
            File eproverExecutable = null;
            if (error == null) {
            	eproverExecutable = new File(execPathname);
                if (!eproverExecutable.exists()) {
                    error = ("The executable file " + eproverExecutable.getCanonicalPath() + " does not exist");
                    System.out.println("Error in EProver.getNewInstanceWithFormulas(): " + error);
                    KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                                + "\n<br/>" + error + "\n<br/>");
                }
            }
            if (error == null) {
                File eproverDirectory = eproverExecutable.getParentFile();
                System.out.println("INFO in EProver.getNewInstanceWithFormulas(): executable == " 
                                   + eproverExecutable.getCanonicalPath());
                System.out.println("INFO in EProver.getNewInstanceWithFormulas(): directory == " 
                                   + eproverDirectory.getCanonicalPath());
                // It should only ever be necessary to write this file once.
                File initFile = new File(eproverDirectory, "init-v.kif");
                if (!initFile.exists()) {
                    PrintWriter pw = new PrintWriter(initFile);
                    pw.println("(instance Process Entity)");
                    pw.flush();
                    try {
                        pw.close();
                    }
                    catch (Exception e1) {
                    }
                }
                System.out.println("INFO in EProver.getNewInstanceWithFormulas(): "
                                   + "Starting EProver as " + eproverExecutable.getCanonicalPath() 
                                   + " " + initFile.getCanonicalPath());
                EProver eprInst = new EProver(eproverExecutable, initFile);
                if (eprInst instanceof EProver) {
                    Iterator it = formulaSource.iterator();
                    if (it.hasNext()) {
                        List badFormulas = new ArrayList();
                        String formStr = null;
                        String response = null;
                        int goodCount = 0;
                        long start = System.currentTimeMillis();
                        while (it.hasNext()) {
                            formStr = (String) it.next();
                            response = eprInst.assertFormula(formStr);
                            if (!(response.indexOf("Formula has been added") >= 0)) 
                                badFormulas.add(formStr);                            
                            else 
                                goodCount++ ;                            
                        }
                        long duration = (System.currentTimeMillis() - start);

                        System.out.println(goodCount + " formulas asserted to " + eprInst 
                                           + " in " + (duration / 1000.0) + " seconds");
                        if (!badFormulas.isEmpty()) {
                            int bc = badFormulas.size();
                            Iterator it2 = badFormulas.iterator();
                            System.out.println("INFO in EProver(): "
                                               + bc + " FORMULA" + ((bc == 1) ? "" : "S") + " REJECTED ");
                            int badCount = 1;
                            String badStr = null;
                            String mgrErrStr = KBmanager.getMgr().getError();
                            while (it2.hasNext()) {
                                badStr = ("[" + badCount++ + "] " + ((String)it2.next()));
                                System.out.println(badStr);
                                mgrErrStr += ("\n<br/>" + "Bad formula: " + badStr + "\n<br/>");
                            }
                            KBmanager.getMgr().setError(mgrErrStr);
                        }
                        if (goodCount > 0)                             
                            epr = eprInst;     // If we've made it this far, we have a usable EProver instance.                   
                    }
                }
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return epr;
    }

    /** *************************************************************
     * To obtain a new instance of Vampire, use the static factory
     * method EProver.getNewInstance().
     */
    private EProver () {
    }

    /** *************************************************************
     * Creates a running instance of EProver.  To obtain a new
     * instance of EProver, use the static factory method
     * EProver.getNewInstance().
     *
     * @param executable A File object denoting the platform-specific
     * EProver executable.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the EProver executable.
     *
     * @throws IOException should not normally be thrown unless either
     *         EProver executable or database file name are incorrect
     */
    private EProver (File executable, File kbFile) throws IOException {

        _eprover = Runtime.getRuntime().exec(executable.getCanonicalPath() + " " + kbFile.getCanonicalPath());
        _reader = new BufferedReader(new InputStreamReader(_eprover.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_eprover.getErrorStream()));

        String line = null; 
        while (_reader.ready() || _error.ready()) {
            if (_reader.ready())
                line = _reader.readLine();
            else if (_error.ready()) 
                line = _error.readLine();
            System.out.println("INFO in EProver(): Return string: " + line);
            if (line.indexOf("Error:") != -1)
                throw new IOException(line);            
        }
        _writer = new BufferedWriter(new OutputStreamWriter(_eprover.getOutputStream()));
    }

    /** *************************************************************
     **/
    public void setCommandLineOptions(String options) {
    	_cliOptions = options;
    }
    
    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    public String assertFormula(String formula) {
        //public String assertFormula(String formula) throws IOException {

    	System.out.println("Error in EProver.assertFormula: E cannot handle single assertions.");
    	return "Error in EProver.assertFormula: E cannot handle single assertions.";
    	/*
        String result = "";
        try {
            StringBuilder assertion = new StringBuilder();
            String safe = StringUtil.replaceUnsafeNamespaceDelimiters(formula);
            safe = StringUtil.replaceNonAsciiChars(safe);
            assertion.append("<assertion> ");
            assertion.append(safe);
            assertion.append(" </assertion>\n");

            _writer.write(assertion.toString());
            _writer.flush();
            for (;;) {
                String line = _reader.readLine();
                if (line.indexOf("Error:") != -1) 
                    throw new IOException(line);                
                // System.out.println("INFO EProver(): Response: " + line);
                result += line + "\n";
                if (line.indexOf("</assertionResponse>") != -1) 
                    break;                
            }
        }
        catch (Exception ex) {
            System.out.println("Error in EProver.assertFormula(" + formula + ")");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
        */
    }

    /** *************************************************************
     * Terminate this instance of EProver. 
     * <font color='red'><b>Warning:</b></font>After calling this function
     * no further assertions or queries can be done.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate () throws IOException {

        System.out.println();
        System.out.println("TERMINATING " + this);
        try {
            _writer.write("<bye/>\n");
            _writer.close();
            _reader.close();
            System.out.println("DESTROYING the Process " + _eprover);
            System.out.println();
            _eprover.destroy();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax
     * @param timeLimit time limit for answering the query (in seconds)
     * @param bindingsLimit limit on the number of bindings
     * @return answer to the query (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    public String submitQuery (String formula, int timeLimit, int bindingsLimit) {
        //public String submitQuery (String formula, int timeLimit, int bindingsLimit) throws IOException {
    	System.out.println("Error in EProver.assertFormula: E cannot handle queries, they must a conjecture in a file.");
    	return "Error in EProver.assertFormula: E cannot handle queries, they must a conjecture in a file.";
    	
    	/*
        String result = "";
        String query = ("<query timeLimit='" + timeLimit + "' bindingsLimit='" + bindingsLimit 
                        + "'> " + formula + " </query>\n");

        System.out.println("INFO in EProver.submitQuery(): " + query);
        try {
            _writer.write(query);
            _writer.flush();
        }
        catch (Exception ex) {
            System.out.println("Error in EProver.submitQuery(): " + ex.getMessage());
            ex.printStackTrace();
        }
        for (;;) {
            String line = _reader.readLine();
            if (line.indexOf("Error:") != -1) 
                throw new IOException(line);            
            result += line + "\n";
            if ((line.indexOf("</queryResponse>") != -1) ||      // result is ok.
                (line.indexOf("</assertionResponse>") != -1))  { // result is syntax error.
                System.out.println("INFO in EProver.submitQuery(): ===================================");
                System.out.println(result);
                result = result.replaceAll("&lt;","<");
                result = result.replaceAll("&gt;",">");
                return result;
            }
        }
        */
    }

    /** *************************************************************
     * A simple test. Works as follows: 
     * <ol>
     *   <li>start E;</li>
     *   <li>make an assertion;</li>
     *   <li>submit a query;</li>
     *   <li>terminate E</li>
     *</ol>
     */
    public static void main (String[] args) throws Exception {

        String initialDatabase = "SUMO-v.kif";
        EProver eprover = EProver.getNewInstance(initialDatabase);
        eprover.setCommandLineOptions("--cpu-limit=600 --soft-cpu-limit=500 -xAuto -tAuto -l 4 --tptp3-in");
        KBmanager.getMgr().setPref("eprover","/home/apease/Programs/E/Prover/eprover");
        System.out.print(eprover.submitQuery("(holds instance ?X Relation)",5,2));

        // System.out.print(eprover.assertFormula("(human Socrates)"));
        // System.out.print(eprover.assertFormula("(holds instance Adam Human)"));
        // System.out.print(eprover.submitQuery("(human ?X)", 1, 2));
        // System.out.print(eprover.submitQuery("(holds instance ?X Human)", 5, 2));
        eprover.terminate();
    }
    
}

