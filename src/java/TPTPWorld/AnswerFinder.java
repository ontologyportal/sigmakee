package TPTPWorld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnswerFinder {
  
    public static String findProof (String problem, String systemsDir) throws Exception {

		Logger logger = Logger.getLogger("");

        // find proof with one-answer system (default: Metis)
        String OneAnswerSystem = "";
        ArrayList<String> atpSystems = SystemOnTPTP.listSystems(systemsDir);
        for (int i = 0; i < atpSystems.size(); i++) {
            String system = atpSystems.get(i);
            if (!system.startsWith("Metis---")) {
                continue;
            }
            if  (OneAnswerSystem.compareTo(system) < 0) {
                OneAnswerSystem = system;
            }
        }
        if (OneAnswerSystem.equals("")) {
            String errorMsg = "% ERROR: Metis not found in " + systemsDir;
			logger.severe(errorMsg);
            return errorMsg;
        }  
        return AnswerFinder.findProof(problem, OneAnswerSystem, systemsDir);
    }

    public static String findProof (String problem, String system, String systemsDir) throws Exception {

        // create tempfile
        String filename = "AnswerFinder.findProof";
        File outputFile = File.createTempFile(filename, ".p", null);
        outputFile.deleteOnExit();
        //System.out.println("temp file: " + outputFile.getCanonicalPath());
        FileWriter fw = new FileWriter(outputFile);
        fw.write(problem);
        fw.close();

        String quietFlag = "-q4";
        String format = "-S";
        int timeLimit = 300;
        String result = SystemOnTPTP.SystemOnTPTP(system, systemsDir, timeLimit, quietFlag, format, outputFile.getCanonicalPath());
        return result;
    }

    /*
    // given a list of TPTPFormulas, extract conjecture and lemma vine
    // send to one-answer system (Metis)
    public static String findProofWithAnswers (String tptp, String systemsDir) throws Exception {
    return findProofWithAnswers(new BufferedReader(new StringReader(tptp)), systemsDir);
    } 
    */

    // given a list of TPTPFormulas, extract conjecture and lemma vine
    // send to one-answer system (Metis)
    public static String findProofWithAnswers (String tptp, String systemsDir) {

		Logger logger = Logger.getLogger("");
    	if (logger.isLoggable(Level.FINER)) {
			String[] params = { "tptp = " + tptp, "systemsDir = " + systemsDir };
			logger.entering("AnswerFinder", "findProofWithAnswers", params);
		}
        String result = "";
        try {
            if ((tptp instanceof String) && !tptp.equals("")) {
                if (!tptp.startsWith("%") && !tptp.startsWith("fof(")) {
                    int idx = tptp.indexOf("fof(");
                    if (idx > 0) {
                        tptp = tptp.substring(idx);
						logger.finest("new tptp == " + tptp);
                        // System.out.println("  new tptp == " + tptp);
                    }
                }
            }
            result = findProofWithAnswersBody(tptp, systemsDir);
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.severe(ex.getMessage());
        }

		logger.exiting("AnswerFinder", "findProofWithAnswers", result);
        return result;
    } 


    public static String findProofWithAnswersBody (String in, String systemsDir) {

		Logger logger = Logger.getLogger("");
		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "reader =" + in,
					"systemsDir = " + systemsDir };
			logger.entering("AnswerFinder", "findProofWithAnswers", params);
		}
		String ans = "";
        try {
            String problem = "";
            TPTPParser parser = TPTPParser.parse(in);
            TPTPFormula conjecture = AnswerExtractor.extractVine(parser.ftable);
            // no conjecture = no answers
            if (conjecture == null) {    
                String errorMsg = "% WARNING: No fof conjecture in proof -> no lemmas -> cannot call one-answer system -> find answers failed";
				logger.warning(errorMsg);
                return errorMsg;
            }
            ArrayList<TPTPFormula> lemmas = ProofSummary.getLemmaVine(conjecture);
            // gather problem, to be sent to one-answer system
            problem += conjecture.fofify() + "\n\n";
            for (TPTPFormula lemma : lemmas) {
                problem += lemma.fofify() + "\n\n";
            }
			logger.finer("Problem: " + problem);
            ans = findProof(problem, systemsDir);
        }
        catch (Exception ex) {
            //ex.printStackTrace();
			logger.severe(ex.getMessage());
        }
		logger.exiting("AnswerFinder", "findProofWithAnswers", ans);
        return ans;
    } 

    // given a proof, find answers using one-answer system
    public static void main (String args[]) throws Exception {

        TPTPParser.checkArguments(args);
        // assumption: filename is args[0] or "--" for stdin
        // locate $TPTP_HOME
        String tptpHome = System.getenv("TPTP_HOME");
        if (tptpHome == null || tptpHome.equals("")) {
            System.out.println("% ERROR: Please specify your $TPTP_HOME environment variable");
            //System.exit(0);
        }
        // set Systems directory
        String systemsDirectory = tptpHome + "/" + "Systems";
        // find proof with answers (by calling one-answer system)
        String proofWithAnswers = findProofWithAnswers(args[0], systemsDirectory);
        // call AnswerExtractor
        TPTPParser parser = TPTPParser.parse(proofWithAnswers);
        if (!AnswerExtractor.extractAnswers(parser)) {
            System.out.println("% No answers found in AnswerFinder");
        }
    }
}
