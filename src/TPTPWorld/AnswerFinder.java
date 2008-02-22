package TPTPWorld;

import java.util.*;
import java.io.*;
import tptp_parser.*;

public class AnswerFinder {

  // given a list of TPTPFormulas, extract conjecture and lemma vine
  // send to one-answer system (Metis)
  public static String findProofWithAnswers (String tptp, String systemsDir) throws Exception {
    return findProofWithAnswers(new BufferedReader(new StringReader(tptp)), systemsDir);
  } 

  public static String findProofWithAnswers (BufferedReader reader, String systemsDir) throws Exception {
    String proof = "";
    TPTPParser parser = TPTPParser.parse(reader);    
    TPTPFormula conjecture = AnswerExtractor.extractVine(parser.ftable);    
    // no conjecture = no answers
    if (conjecture == null) {    
      String errorMsg = "% WARNING: No fof conjecture in proof -> no lemmas -> cannot call one-answer system -> find answers failed";
      System.out.println(errorMsg);
      return errorMsg;
    }
    ArrayList<TPTPFormula> lemmas = ProofSummary.getLemmaVine(conjecture);
    // gather proof, to be sent to one-answer system
    proof += conjecture.fofify() + "\n\n";
    for (TPTPFormula lemma : lemmas) {
      proof += lemma.fofify() + "\n\n";
    }
   
    // create tempfile
    String filename = "AnswerFinder";
    File outputFile = File.createTempFile(filename, ".p", null);
    outputFile.deleteOnExit();
    //System.out.println("temp file: " + outputFile.getCanonicalPath());
    FileWriter fw = new FileWriter(outputFile);
    fw.write(proof);
    fw.close();

    // call SystemOnTPTP with one-answer system (default: Metis)
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
      String errorMsg = "% ERROR: Can't find OneAnswerSystem (Metis) in Systems: " + systemsDir;
      System.out.println(errorMsg);
      return errorMsg;
    }  
    String quietFlag = "-q4";
    String format = "-S";
    int timeLimit = 300;
    String result = SystemOnTPTP.SystemOnTPTP(OneAnswerSystem, systemsDir, timeLimit, quietFlag, format, outputFile.getCanonicalPath());

    return result;
  } 

  // given a proof, find answers using one-answer system
  public static void main (String args[]) throws Exception {
    TPTPParser.checkArguments(args);
    // assumption: filename is args[0] or "--" for stdin
    BufferedReader reader = TPTPParser.createReader(args[0]);
    
    // find proof with answers (by calling one-answer system)
    String proofWithAnswers = findProofWithAnswers(reader, null);

    // call AnswerExtractor
    StringReader sr = new StringReader(proofWithAnswers);
    TPTPParser parser = TPTPParser.parse(new BufferedReader(sr));
    if (!AnswerExtractor.extractAnswers(parser)) {
      System.out.println("% No answers found in AnswerFinder");
    }
  }
}
