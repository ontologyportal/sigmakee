package TPTPWorld;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.regex.*;
import tptp_parser.*;
import ClientHttpRequest.*;
import javax.servlet.jsp.*;
import com.articulate.sigma.*;
import TPTPWorld.SystemOnTPTP;

public class InterfaceTPTP {
    
    public static String TPTPWorld = "";

    public static String BuiltInDir = "";
 
    public static String defaultSystemBuiltIn = "";

    public static String defaultSystemLocal = "";

    public static String defaultSystemRemote = "";

    public static String SoTPTP;

    public static String tptp4X;

    public static boolean tptpWorldExists = false;

    public static boolean builtInExists = false;

    public static ArrayList<String> systemListBuiltIn = new ArrayList<String>();

    public static ArrayList<String> systemListLocal = new ArrayList<String>();

    public static ArrayList<String> systemListRemote = new ArrayList<String>();

    public static String SystemOnTPTPFormReplyURL =
            "http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTPTPFormReply";

    private static class ATPResult {

        public static String cleanResult = "";

        public static String originalResult = "";

        public static String printResult = "";

        public static String idvResult = "";

        public ATPResult () {
            cleanResult = "";
            originalResult = "";
            printResult = "";
	    idvResult = "";
	}
    }


    public static void init() {
        TPTPWorld = KBmanager.getMgr().getPref("tptpHomeDir");
        BuiltInDir = KBmanager.getMgr().getPref("systemsDir");
	//----Check available Systems
        String systemsInfo = BuiltInDir + "/SystemInfo";
        builtInExists = (new File(BuiltInDir)).exists() && (new File(systemsInfo)).exists();
        SoTPTP = TPTPWorld + "/SystemExecution/SystemOnTPTP";
        tptp4X = TPTPWorld + "/ServiceTools/tptp4X";
        tptpWorldExists = (new File(SoTPTP)).exists();
        BufferedReader reader;
        String responseLine;
        //----Check builtin systems
        if (builtInExists) {
            systemListBuiltIn = SystemOnTPTP.listSystems(BuiltInDir, "SoTPTP");
            defaultSystemBuiltIn = "EP---0.999";
        }        
        //----Check local TPTP
        if (tptpWorldExists) {
            try {
                String command = SoTPTP + " " + "-w" + " " + "SoTPTP";
                Process proc = Runtime.getRuntime().exec(command);
                systemListLocal.add("Choose system");
                reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        //----Read List of Local Systems
                while ((responseLine = reader.readLine()) != null) {
                    systemListLocal.add(responseLine);
        //----Try use EP as the default system
                    if (responseLine.startsWith("EP---")) {
                        defaultSystemLocal = responseLine;
                    }
                }
                reader.close();
            } catch (Exception ioe) {
                System.err.println("Exception: " + ioe.getMessage());
            }
        }
        //----Call RemoteSoT to retrieve remote list of systems
        Hashtable URLParameters = new Hashtable();

        //----Note, using www.tptp.org does not work

        systemListRemote.add("Choose system");
        URLParameters.put("NoHTML","1");
        URLParameters.put("QuietFlag","-q2");
        URLParameters.put("SubmitButton","ListSystems");
        URLParameters.put("ListStatus","SoTPTP");

        try {
            reader = new BufferedReader(new InputStreamReader(
            ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
        //----Read List of Remote Systems
            while ((responseLine = reader.readLine()) != null) {
                systemListRemote.add(responseLine);
        //----Try use EP as the default system
                if (responseLine.startsWith("EP---")) {
                    defaultSystemRemote = responseLine;
                }
            }
            reader.close();
        } catch (Exception ioe) {
            System.err.println("Exception: " + ioe.getMessage());
        }
    }


    public static String callTPTP(String location, String systemChosen, String problemFile,
                                     int timeout, String quietFlag, String tstpFormat)
	throws Exception {
	if (location.equalsIgnoreCase("builtin"))
	    return callBuiltInTPTP(systemChosen, problemFile, timeout, quietFlag, tstpFormat).originalResult;
	else if (location.equalsIgnoreCase("local"))
	    return callLocalTPTP(systemChosen, problemFile, timeout, quietFlag, tstpFormat).originalResult;
	else if (location.equalsIgnoreCase("remote"))
	    return callRemoteTPTP(systemChosen, problemFile, timeout, quietFlag, tstpFormat).originalResult;
	else
	    throw new Exception("There's no SystemOnTPTP location \""+location+"\".");
    }


    public static ATPResult callRemoteTPTP (String systemChosen, String problemFile, int timeout,
                                            String quietFlag, String tstpFormat) 
                                           throws Exception {
	ATPResult atpOut = new ATPResult ();
        String responseLine = "";
        BufferedReader reader; 
        boolean tptpEnd = false;
//----Need to check the name exists
        Hashtable URLParameters = new Hashtable();
        URLParameters.put("NoHTML","1");
        if (quietFlag.equals("IDV")) {
            URLParameters.put("IDV","-T");
            URLParameters.put("QuietFlag","-q4");
            URLParameters.put("X2TPTP",tstpFormat);
        } else if (quietFlag.equals("hyperlinkedKIF")) {
            URLParameters.put("QuietFlag","-q3");
            URLParameters.put("X2TPTP","-S");
        }else {
            URLParameters.put("QuietFlag",quietFlag);
            URLParameters.put("X2TPTP",tstpFormat);
        }
//----Need to offer automode
        URLParameters.put("System___System",systemChosen);
        URLParameters.put("TimeLimit___TimeLimit", new Integer(timeout));
        URLParameters.put("ProblemSource","UPLOAD");
        URLParameters.put("UPLOADProblem",new File(problemFile));
        URLParameters.put("SubmitButton","RunSelectedSystems");
        reader = new BufferedReader(new InputStreamReader(
                ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
        atpOut.printResult += "<PRE>";
        while ((responseLine = reader.readLine()) != null) {
            if (responseLine.startsWith("Loading IDV")) {
                tptpEnd = true;
            }
            if (!responseLine.equals("") && !responseLine.substring(0,1).equals("%") && !tptpEnd) {
                atpOut.cleanResult += responseLine + "\n";
            }           
            if (tptpEnd && quietFlag.equals("IDV")) {
                atpOut.idvResult += responseLine + "\n";
            }
            atpOut.originalResult += responseLine + "\n";
            if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) {
                atpOut.printResult += responseLine + "\n";
            }
        }
        atpOut.idvResult += "</PRE>\n";
        atpOut.printResult += "</PRE>";
        reader.close();
        return atpOut; 
    }





    public static ATPResult callLocalTPTP (String systemChosen, String problemFile, int timeout,
                                           String quietFlag, String tstpFormat)
	throws Exception { 
	    ATPResult atpOut = new ATPResult ();
            String responseLine = "";
            BufferedReader reader; 
            Process proc;
            String command;
            if (quietFlag.equals("hyperlinkedKIF")) {
              command = SoTPTP + " " +
                        "-q3"        + " " +  // quietFlag
                        systemChosen + " " + 
                        timeout      + " " +
                        "-S"         + " " +  //tstpFormat
                        problemFile;
            } else if (quietFlag.equals("IDV")) {
              command = SoTPTP + " " +
                        "-q4"        + " " +  // quietFlag
                        systemChosen + " " + 
                        timeout      + " " +
                        "-S"           + " " +  //tstpFormat
                        problemFile;            
            } else {
              command = SoTPTP + " " + 
                        quietFlag    + " " + 
                        systemChosen + " " + 
                        timeout      + " " + 
                        tstpFormat   + " " +
                        problemFile;
            }
            proc = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));


            atpOut.printResult += "<PRE>";
            while ((responseLine = reader.readLine()) != null) {
              if (!responseLine.equals("") && !responseLine.substring(0,1).equals("%")) {
                atpOut.cleanResult += responseLine + "\n";
              }
              atpOut.originalResult += responseLine + "\n";
              if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) { atpOut.printResult += responseLine + "\n"; }
            }
            atpOut.printResult += "</PRE>";
            reader.close();
            return atpOut;
    }


    public static ATPResult callBuiltInTPTP (String systemChosen, String problemFile, int timeout,
                                             String quietFlag, String tstpFormat)
	throws Exception { 
	    ATPResult atpOut = new ATPResult ();
            String qq;
            String format = "";
            String result = "";
            if (quietFlag.equals("IDV")) {
              qq = "-q4";
              format = "-S";
            } else if (quietFlag.equals("hyperlinkedKIF")) {
              qq = "-q4";
              format = "-S";
            } else {
              qq = quietFlag;
              format = tstpFormat;
            }
            result = SystemOnTPTP.SystemOnTPTP(systemChosen, BuiltInDir, timeout, qq, format, problemFile);
            atpOut.originalResult = result;
            atpOut.printResult += "<PRE>";
            if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) {
              atpOut.printResult += result;
            } 
            if (quietFlag.equals("IDV")) {
// IDV can't handle comments?
              StringTokenizer st = new StringTokenizer(result,"\n");
              String temp = "";
              while (st.hasMoreTokens()) {
                String next = st.nextToken(); 
                if (!next.equals("") && !next.substring(0,1).equals("%")) {
                  temp += next + "\n";   
                }
              }
              atpOut.cleanResult = temp;
            } else {
		atpOut.cleanResult = result;
            }
            atpOut.printResult += "</PRE>";
            return atpOut;
    }


    public static String queryTPTP (String stmt, int timeout, int maxAnswers, String lineHtml, 
                                     String systemChosen, String location,
                                     String quietFlag, String kbName, String language, JspWriter out) 
                         throws Exception {
//----Setup
        String resultAll = "";
        String tstpFormat = "-S";
        KB kb = KBmanager.getMgr().getKB(kbName);
        String originalKBFileName = null;
        String responseLine;
        BufferedReader reader;

//-----------------------------------------------------------------------------
//----Code for doing the query
        String TPTP_QUESTION_SYSTEM = "SNARK---";
        String TPTP_ANSWER_SYSTEM = "Metis---";
        StringBuffer sbStatu1s = new StringBuffer();
        String kbFileName = null;
        Formula conjectureFormula;
//----Result of query (passed to tptp4X then passed to HTMLformatter.formatProofResult)
        String result = "";
        String newResult = "";
        String idvResult = "";
        String originalResult = "";
        String command;
        Process proc;
        boolean isQuestion = systemChosen.startsWith(TPTP_QUESTION_SYSTEM);
        String conjectureTPTPFormula = "";

// Build query:
//-----------------------------------------------------------------------------
        //----Add KB contents here
        conjectureFormula = new Formula();
        conjectureFormula.theFormula = stmt;
        conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
        conjectureFormula.tptpParse(true,kb);
        Iterator it = conjectureFormula.getTheTptpFormulas().iterator();
        String theTPTPFormula = (String) it.next();
	String originalConjecture = theTPTPFormula;
        if (isQuestion) {
          conjectureTPTPFormula =  "fof(1" + ",question,(" + theTPTPFormula + ")).";
        } else {
          conjectureTPTPFormula =  "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";
        }
        originalKBFileName = kb.writeTPTPFile(null,
                                      null,
                                      true,
                                      systemChosen,
                                      isQuestion);
        ArrayList<Binding> lastAnswer = null;
        ArrayList<Binding> originalAnswer = null;
        int numAnswers = 0;
        TreeSet<TPTPParser.Symbol> symbolsSoFar = new TreeSet(new TPTPParser.SymbolComparator());
        ArrayList<String> ldAxiomsSoFar = new ArrayList();
        ldAxiomsSoFar.addAll(LooksDifferent.getUniqueAxioms());
//----Create symbol list from entire kbFile
        TreeSet<TPTPParser.Symbol> symbolList = TPTPParser.getSymbolList(originalKBFileName);
        ATPResult atpOut;
//----Add while loop to check for more answers
//----If # of answers == maximum answers, exit loop
//----If last check for an answer failed (no answer found or empty answer list), exit loop
//----Each loop around, add ld axioms

//----While loop start:
        do {
	    originalResult = "";
	    result = "";
//----If we found a new set of answers, update query and axiom list
            if (lastAnswer != null) {
	        resultAll += "<hr>";
//----Get symbols from lastAnswer
                TreeSet<TPTPParser.Symbol> newSymbols = TPTPParser.getSymbolList(lastAnswer);
//----Find uniqueSymbols from lastAnswer not in symbolsSoFar
                TreeSet<TPTPParser.Symbol> uniqueSymbols = LooksDifferent.getUniqueSymbols(symbolsSoFar, newSymbols);
//----symbolsSOFar = uniqueSymbols U symbolsSoFar
                symbolsSoFar.addAll(uniqueSymbols);
//----Get new set of ld axioms from the unique symbols
                ArrayList<String> ldAxiomsNew = LooksDifferent.addAxioms(uniqueSymbols, symbolList);
//----Add ld axioms for those uniqueSymbols to ldAxiomsSoFar
                ldAxiomsSoFar.addAll(ldAxiomsNew);
//----Add last answer to conjecture
                theTPTPFormula = LooksDifferent.addToConjecture(theTPTPFormula, lastAnswer);
//----Create new conjectureTPTPFormula
                if (isQuestion) {
                    conjectureTPTPFormula = "fof(1" + ",question,(" + theTPTPFormula + ")).";
                } else {
                    conjectureTPTPFormula = "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";
                }
//----keep originalKBFile intact so that we do not have to keep recreating it, just copy and append to copy then delete copy, only delete original at the end of run
//----delete last kbFileName
                if (kbFileName != null) {
                    (new File(kbFileName)).delete();
                }
//----kbFileName = originalKBFileName + all ld axioms + conjectureTPTPFormula;
//----Copy original kb file
                kbFileName = kb.copyFile(originalKBFileName);
//----Append ld axioms and conjecture to the end
                kb.addToFile(kbFileName, ldAxiomsSoFar, conjectureTPTPFormula);
//----Reset last answer
                lastAnswer = null;
            } else {
//----kbFileName = originalKBFileName + conjectureTPTPFormula
//----Copy original kb file and append conjecture to the end
                kbFileName = kb.copyFile(originalKBFileName);
                kb.addToFile(kbFileName, null, conjectureTPTPFormula);
            }
//----Call RemoteSoT
            if (location.equals("remote")) {
                if (systemChosen.equals("Choose%20system")) {
                    resultAll += "No system chosen";            
                } else {
	            if (numAnswers == 0) {
                        resultAll += "(Remote SystemOnTPTP call)";
                    }
                    atpOut = callRemoteTPTP (systemChosen, kbFileName, timeout,
                                             quietFlag, tstpFormat);
                    resultAll += atpOut.printResult;
                    idvResult += atpOut.idvResult;
                    result += atpOut.cleanResult;
                    originalResult += atpOut.originalResult;                
                }
            } 
            else if (location.equals("local") && tptpWorldExists) {
//----Call local copy of TPTPWorld instead of using RemoteSoT
                if (systemChosen.equals("Choose%20system")) {
                    resultAll += "No system chosen";
                } else {
                    if (numAnswers == 0) {
                        resultAll += "(Local SystemOnTPTP call)";
                    }
                    atpOut = callLocalTPTP (systemChosen, kbFileName, timeout,
                                            quietFlag, tstpFormat);
                    resultAll += atpOut.printResult;
                    idvResult += atpOut.idvResult;
                    result += atpOut.cleanResult;
                    originalResult +=atpOut.originalResult;                
                }
            } 
            else if (location.equals("local") && builtInExists && !tptpWorldExists) {
//----Call built in SystemOnTPTP instead of using RemoteSoT or local
                if (systemChosen.equals("Choose%20system")) {
                    resultAll += "No system chosen";
                } else {
	            if (numAnswers == 0) {
                        resultAll += "(Built-In SystemOnTPTP call)";
                    }
                    atpOut = callBuiltInTPTP (systemChosen, kbFileName, timeout,
                                              quietFlag, tstpFormat);
                    resultAll += atpOut.printResult;
                    idvResult += atpOut.idvResult;
                    result += atpOut.cleanResult;
                    originalResult +=atpOut.originalResult;                
                }            
            }
            else {
                resultAll += "INTERNAL ERROR: chosen option not valid: " + location +
                     ".  Valid options are: 'Local SystemOnTPTP, Built-In SystemOnTPTP, or Remote SystemOnTPTP'.";
            }
//----If selected prover is not an ANSWER system, send proof to default ANSWER system (Metis)
            if (!(systemChosen.startsWith(TPTP_ANSWER_SYSTEM)&&location.equals("local")&&builtInExists && !tptpWorldExists)) {
                  String answerResult = AnswerFinder.findProofWithAnswers(result, BuiltInDir);
//----If answer is blank, ERROR, or WARNING, do not place in result
                  if (!answerResult.equals("") && 
                      !answerResult.startsWith("% ERROR:") &&
                      !answerResult.startsWith("% WARNING:")) {
                      result = answerResult;
                  } 
//----If ERROR is answer result, report to user
                  if (answerResult.startsWith("% ERROR:")) {
                      resultAll += "==" + answerResult;
                  } 
            }
            if (systemChosen.startsWith(TPTP_QUESTION_SYSTEM)) {
//----Procedure if SNARK was chosen
	        String conj = "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";
                ArrayList<Binding> answer = SystemOnTPTP.getSZSBindings(conj, originalResult);
                lastAnswer = answer;
                newResult = TPTP2SUMO.convert(result, answer, false);
            } else {
//----Procedure if not SNARK (call one answer system: Metis)
                TPTPParser parser = TPTPParser.parse(new BufferedReader(new StringReader(result)));
                lastAnswer = AnswerExtractor.extractAnswers(parser.ftable);
//----Get original variable names
                lastAnswer = SystemOnTPTP.getSZSBindings(conjectureTPTPFormula, lastAnswer);
                newResult = TPTP2SUMO.convert(result, false);
            }
            if (quietFlag.equals("IDV") && location.equals("remote")) {
                if (SystemOnTPTP.isTheorem(originalResult)) {
                    int size = SystemOnTPTP.getTPTPFormulaSize(result);
                    if (size == 0) {
                        resultAll += "No solution output by system.  IDV tree unavaiable.";
                    } else {
                        resultAll += idvResult;
                    }
                } else {
                    resultAll += "Not a theorem.  IDV tree unavailable.";
                }
            } else if (quietFlag.equals("IDV") && !location.equals("remote")) {
                if (SystemOnTPTP.isTheorem(originalResult)) {
                    int size = SystemOnTPTP.getTPTPFormulaSize(result);
                    if (size > 0) {
                        String port = KBmanager.getMgr().getPref("port");
                        if ((port == null) || port.equals(""))
                            port = "8080";
                        String hostname = KBmanager.getMgr().getPref("hostname");
                        if (hostname == null) {
                            hostname = "localhost";
                        }
			StringTokenizer st = new StringTokenizer(result,"\n");
			String temp = "";
			while (st.hasMoreTokens()) {
			    String next = st.nextToken();
			    if (!next.equals("") && !next.substring(0,1).equals("%")) {
				temp += next + "\n";
			    }
			}
                        result=temp;
                        String libHref = "http://" + hostname + ":" + port + "/sigma/lib";
                        resultAll += "<APPLET CODE=\"IDVApplet\" archive=\"" + libHref + "/IDV.jar," + libHref + "/TptpParser.jar," + libHref + "/antlr-2.7.5.jar," + libHref + "/ClientHttpRequest.jar\"\n";
                        resultAll += "WIDTH=800 HEIGHT=100 MAYSCRIPT=true>\n";
                        resultAll += "  <PARAM NAME=\"TPTP\" VALUE=\"" + result + "\">\n";
                        resultAll += "  Hey, you cant see my applet!!!\n";
                        resultAll += "</APPLET>\n";
                    } else {
                        resultAll += "No solution output by system.  IDV tree unavaiable.";
                    }
                } else {
                    resultAll += "Not a theorem.  IDV tree unavailable.";
                }
            } else if (quietFlag.equals("hyperlinkedKIF")) {
	        if (originalAnswer == null) {
  		    originalAnswer = lastAnswer;
                } else {
//----This is not the first answer, that means result has dummy ld predicates, bind conjecture with new answer, remove outside existential
	            if (!lastAnswer.equals("")) {
                        //resultAll += "<br>There was an Answer before! <br>";
                        String bindConjecture = "fof(bindConj" + ", conjecture,(" + LooksDifferent.bindConjecture(originalConjecture, originalAnswer, lastAnswer) + ")).";
//----With new bindConjecture, take last result, filter out anything with LDs in it, put in prover
	                String axioms = LooksDifferent.filterLooksDifferent(originalResult);
//----Redo proof using OneAnswerSystem again
                        String bindProblem = axioms + " " + bindConjecture;
                        String bindResult = AnswerFinder.findProof(bindProblem, BuiltInDir);
                        newResult = TPTP2SUMO.convert(bindResult, lastAnswer, true);
                    }
                }
                boolean isTheorem = SystemOnTPTP.isTheorem(originalResult);
                boolean isCounterSatisfiable = SystemOnTPTP.isCounterSatisfiable(originalResult); 
	        boolean proofExists = SystemOnTPTP.proofExists(originalResult);
	        int timeUsed = SystemOnTPTP.timeUsed(originalResult);
                if (isTheorem) { 
                    if (proofExists) {
                        try {
//----If a proof exists, print out as hyperlinked kif
                            resultAll += HTMLformatter.formatProofResult(newResult,
                                                                         stmt,
                                                                         stmt,
                                                                         lineHtml,
                                                                         kbName,
                                                                         language);
                        } catch (Exception e) {}
                    } else {
//----Proof does not exist, but was a theorem
                        resultAll += "Answer 1. Yes [Theorem]<br>";
                    } 
                } else if (isCounterSatisfiable) {
                    resultAll += "Answer 1. No [CounterSatisfiable]<br>";
                } else {
                    if (numAnswers == 0) 
                        resultAll += "Answer 1. No<br>";
                }
	    }
//----If lastAnswer != null (we found an answer) && there is an answer (lastAnswer.size() > 0)
            if (lastAnswer != null && lastAnswer.size() > 0) {
                numAnswers++;
            } else {
//         out.println("No luck finding new answer");
            } 
//----Add query time limit to while loop break
        } while (numAnswers < maxAnswers && lastAnswer != null && lastAnswer.size() > 0);
//----Delete the kbFile
        if (originalKBFileName != null) {
            (new File(originalKBFileName)).delete();
        }
        return resultAll;
    }


    public static void main () {
    }
}
 
