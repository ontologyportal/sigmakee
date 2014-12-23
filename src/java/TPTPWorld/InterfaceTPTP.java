package TPTPWorld;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.articulate.sigma.*;

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

	private static Logger logger;
	
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
	
	/** ***************************************************************
	 */
	public static void init() {
		if (logger == null)
			logger = Logger.getLogger(InterfaceTPTP.class.getName().toString());

		logger.info("Initializing InterfaceTPTP.");

		TPTPWorld = KBmanager.getMgr().getPref("tptpHomeDir");
		BuiltInDir = TPTPWorld + "/Systems";
		
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
				logger.severe("Exception: " + ioe.getMessage());
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
			URL replyUrl = new URL(SystemOnTPTPFormReplyURL);
			//InputStreamReader isr = null;
			InputStreamReader isr = new InputStreamReader(ClientHttpRequest.post(replyUrl,
					URLParameters));
			reader = new BufferedReader(isr);
			//----Read List of Remote Systems
			while ((responseLine = reader.readLine()) != null) {
				systemListRemote.add(responseLine);
				//----Try use EP as the default system
				if (responseLine.startsWith("EP---")) {
					defaultSystemRemote = responseLine;
				}
				else defaultSystemRemote = "";
			}
			reader.close();
		} catch (Exception ioe) {
			logger.severe("Exception: " + ioe.getMessage());
		}
	}

	/** ***************************************************************
	 */
	public static String callTPTP(String location, 
			String systemChosen, 
			String problemFile,
			int timeout, 
			String quietFlag, 
			String tstpFormat) throws Exception {
		String result = "";
		if (location.equalsIgnoreCase("builtin"))
			result = callBuiltInTPTP(systemChosen, 
					problemFile, 
					timeout, 
					quietFlag, 
					tstpFormat).originalResult;
		else if (location.equalsIgnoreCase("local"))
			result = callLocalTPTP(systemChosen, 
					problemFile, 
					timeout, 
					quietFlag, 
					tstpFormat).originalResult;
		else if (location.equalsIgnoreCase("remote"))
			result = callRemoteTPTP(systemChosen, 
					problemFile, 
					timeout, 
					quietFlag, 
					tstpFormat).originalResult;
		else
			throw new Exception("There's no SystemOnTPTP location \""+location+"\".");
		return trimUnexpectedTokens(result);
	}

	/** ***************************************************************
	 * This method attempts to remove from the String input any
	 * unexpected and spurious leading tokens, such as those resulting
	 * from error or status messages.
	 *
	 * @param input The entire multi-line response returned by a call
	 * to built-in or remote SystemOnTPTP
	 *
	 * @return A String with any unexpected leading text removed
	 */
	private static String trimUnexpectedTokens(String input) {
		
		String output = input;
		String trimmed = null;
		try {
			if ((output instanceof String) && !output.equals("")) {
				output = output.trim();
				List<String> highLevelForms = Arrays.asList("%",
						"fof(",
						"cnf(",
						"include",
						"input_formula",
				"input_clause");
				int idx = -1;
				int nextIdx = -1;
				for (String token : highLevelForms) {
					nextIdx = output.indexOf(token);
					if ((nextIdx > -1) && ((idx == -1) || (nextIdx < idx))) 
						idx = nextIdx;					
				}
				if (idx == -1) {
					trimmed = output;
					output = "";
				}
				else if (idx > 0) {
					trimmed = output.substring(0, idx);
					output = output.substring(idx);
				}
			}
			if (trimmed != null) {
				logger.info((((input instanceof String) && (input.length() > 20))
								? (input.substring(0, 20) + " ...")
										: input) + ";  trimmed == \"" + trimmed + "\"");
			}
		}
		catch (Exception ex) {
			logger.warning("ERROR in InterfaceTPTP.trimUnexpectedTokens("
					+ (((input instanceof String) && (input.length() > 20))
							? (input.substring(0, 20) + " ...")
									: input) + ");  trimmed == \"" + trimmed + "\";  output == \"" + output + "\"");
			ex.printStackTrace();
		}
		return output;
	}

	/** ***************************************************************
	 */
	public static ATPResult callRemoteTPTP (String systemChosen, 
			String problemFile, 
			int timeout,
			String quietFlag, 
			String tstpFormat) {
		
		ATPResult atpOut = new ATPResult ();
		BufferedReader reader = null;
		try {
			String responseLine = "";
			boolean tptpEnd = false;
			//----Need to check the name exists
			Hashtable URLParameters = new Hashtable();
			URLParameters.put("NoHTML","1");
			if (quietFlag.equals("IDV")) {
				URLParameters.put("IDV","-T");
				URLParameters.put("QuietFlag","-q4");
				URLParameters.put("X2TPTP",tstpFormat);
			} 
			else if (quietFlag.equals("hyperlinkedKIF")) {
				URLParameters.put("QuietFlag","-q3");
				URLParameters.put("X2TPTP","-S");
			} 
			else {
				URLParameters.put("QuietFlag",quietFlag);
				URLParameters.put("X2TPTP",tstpFormat);
			}
			//----Need to offer automode
			URLParameters.put("System___System",systemChosen);
			URLParameters.put("TimeLimit___TimeLimit", new Integer(timeout));
			URLParameters.put("ProblemSource","UPLOAD");
			URLParameters.put("UPLOADProblem",new File(problemFile));
			URLParameters.put("SubmitButton","RunSelectedSystems");
			URL url = new URL(SystemOnTPTPFormReplyURL);
			//InputStreamReader isr = null;
			InputStreamReader isr = 
					new InputStreamReader(ClientHttpRequest.post(url,URLParameters));
			reader = new BufferedReader(isr);
			atpOut.printResult += "<PRE>";
			while ((responseLine = reader.readLine()) != null) {
				responseLine = trimUnexpectedTokens(responseLine);
				responseLine = StringUtil.safeToKifNamespaceDelimiters(responseLine);
				if (responseLine.startsWith("Loading IDV")) 
					tptpEnd = true;				
				if (StringUtil.isNonEmptyString(responseLine)
						&& !responseLine.startsWith("%") 
						&& !tptpEnd) 
					atpOut.cleanResult += responseLine + "\n";				   
				if (tptpEnd && quietFlag.equals("IDV")) 
					atpOut.idvResult += responseLine + "\n";				
				atpOut.originalResult += responseLine + "\n";
				if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) 
					atpOut.printResult += responseLine + "\n";				
			}
			atpOut.idvResult += "</PRE>\n";
			atpOut.printResult += "</PRE>";
		}
		catch (Exception ex) {
			logger.warning("Error: " + ex.getStackTrace());
			ex.printStackTrace();
		}
		finally {
			try {
				if (reader != null) reader.close();
			}
			catch (Exception ioe) {
				logger.warning("Error: " + ioe.getStackTrace());
				ioe.printStackTrace();
			}
		}
		return atpOut; 
	}

	/** ***************************************************************
	 */
	public static ATPResult callLocalTPTP (String systemChosen, 
			String problemFile, 
			int timeout,
			String quietFlag, 
			String tstpFormat) { 

		if (logger.isLoggable(Level.FINER)) {
			String[] params = {"systemChosen = " + systemChosen, "problemFile = " + problemFile, "timeout = " + timeout, "quietFlag = " + quietFlag, "tstpFormat = " + tstpFormat};
			logger.entering("InterfaceTPTP", "callLocalTPTP", params);
		}

		ATPResult atpOut = new ATPResult ();
		BufferedReader reader = null;
		try {
			String responseLine = "";
			Process proc;
			String command;
			if (quietFlag.equals("hyperlinkedKIF")) {
				command = SoTPTP + " " +
				"-q3"        + " " +  // quietFlag
				systemChosen + " " + 
				timeout      + " " +
				"-S"         + " " +  //tstpFormat
				problemFile;
			} 
			else if (quietFlag.equals("IDV")) {
				command = SoTPTP + " " +
				"-q4"        + " " +  // quietFlag
				systemChosen + " " + 
				timeout      + " " +
				"-S"           + " " +  //tstpFormat
				problemFile;            
			} 
			else {
				command = SoTPTP + " " + 
				quietFlag    + " " + 
				systemChosen + " " + 
				timeout      + " " + 
				tstpFormat   + " " +
				problemFile;
			}
			logger.finer("command: " + command);

			proc = Runtime.getRuntime().exec(command);
			reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			atpOut.printResult += "<PRE>";
			while ((responseLine = reader.readLine()) != null) {
				//responseLine = trimUnexpectedTokens(responseLine);
				responseLine = StringUtil.safeToKifNamespaceDelimiters(responseLine);
				atpOut.originalResult += responseLine + "\n";

				if (StringUtil.isNonEmptyString(responseLine) 
					&& !responseLine.startsWith("%")) 				
						atpOut.cleanResult += responseLine + "\n";				
				if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) 
					atpOut.printResult += responseLine + "\n"; 
			}
			atpOut.printResult += "</PRE>";
		}
		catch (Exception ex) {
			logger.severe("ERROR: " + ex.getStackTrace());
			ex.printStackTrace();
		}
		finally {
			try {
				if (reader != null) reader.close();
			}
			catch (Exception ioe) {
				logger.warning("ERROR: " + ioe.getStackTrace());
				ioe.printStackTrace();
			}
		}
		
		logger.exiting("InterfaceTPTP", "callLocalTPTP", atpOut);
		return atpOut;
	}

	/** ***************************************************************
	 */
	public static ATPResult callBuiltInTPTP (String systemChosen, 
			String problemFile, 
			int timeout,
			String quietFlag, 
			String tstpFormat) { 
		
		ATPResult atpOut = new ATPResult ();
		try {
			String qq;
			String format = "";
			String result = "";
			if (quietFlag.equals("IDV")) {
				qq = "-q4";
				format = "-S";
			} 
			else if (quietFlag.equals("hyperlinkedKIF")) {
				qq = "-q4";
				format = "-S";
			} 
			else {
				qq = quietFlag;
				format = tstpFormat;
			}
			result = SystemOnTPTP.SystemOnTPTP(systemChosen, 
					BuiltInDir, 
					timeout, 
					qq, 
					format, 
					problemFile);

			result = trimUnexpectedTokens(result);
			result = StringUtil.safeToKifNamespaceDelimiters(result);

			atpOut.originalResult = result;
			atpOut.printResult += "<PRE>";
			if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) 
				atpOut.printResult += result;			
			if (quietFlag.equals("IDV")) {
				// IDV can't handle comments?
						StringTokenizer st = new StringTokenizer(result,"\n");
						String temp = "";
						while (st.hasMoreTokens()) {
							String next = st.nextToken(); 
							if (StringUtil.isNonEmptyString(next) && !next.startsWith("%")) 
								temp += next + "\n";   							
						}
						atpOut.cleanResult = temp;
			} 
			else 
				atpOut.cleanResult = result;			
			atpOut.printResult += "</PRE>";
		}
		catch (Exception ex) {
			logger.severe("ERROR: " + ex.getStackTrace());
			ex.printStackTrace();
		}
		return atpOut;
	}
	
	/** ***************************************************************
	 */
	public static String queryTPTP (String stmt, 
			int timeout, 
			int maxAnswers, 
			String lineHtml, 
			String systemChosen, 
			String location,
			String quietFlag, 
			String kbName, 
			String language) 
	throws Exception {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = {"stmt = " + stmt, "timeout = " + timeout, "maxAnswers = " + maxAnswers,
					"lineHtml = " + lineHtml, "systemChosen = " + systemChosen, "location = " + location, 
					"quietFlag = " + quietFlag, "kbName = " + kbName, "language = " + language};
			logger.entering("InterfaceTPTP", "queryTPTP", params);
		}
		logger.finest("BuiltIn Exists? : " + builtInExists);
		logger.finest("TPTP World Exists? :" + tptpWorldExists);
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

		String oldConjecture = conjectureFormula.theFormula;
		conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
		boolean suppressAnswerExtraction = oldConjecture.equals(conjectureFormula.theFormula);
		logger.finest("conjectureFormula.theFormula == " + conjectureFormula.theFormula + "\nsuppressAnswerExtraction == " + suppressAnswerExtraction);
		//if (suppressAnswerExtraction) resultAll += "suppress definite answers<br/>";
		SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
    	stptp._f = conjectureFormula;
    	stptp.tptpParse(conjectureFormula,true, kb);
		Iterator<String> it = conjectureFormula.getTheTptpFormulas().iterator();
		String theTPTPFormula = (String) it.next();
		String originalConjecture = theTPTPFormula;
		if (isQuestion) 
			conjectureTPTPFormula =  "fof(1" + ",question,(" + theTPTPFormula + ")).";
		else 
			conjectureTPTPFormula =  "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";		
		SUMOKBtoTPTPKB stptpkb = new SUMOKBtoTPTPKB();
    	stptpkb.kb = kb;
		originalKBFileName = stptpkb.writeTPTPFile(null,null,true,systemChosen,isQuestion);
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
		do {  //----While loop start:
			originalResult = "";
			result = "";
			//----If we found a new set of answers, update query and axiom list
			if (lastAnswer != null) {
				resultAll += "<hr>";
				//----Get symbols from lastAnswer
				TreeSet<TPTPParser.Symbol> newSymbols = TPTPParser.getSymbolList(lastAnswer);
				//----Find uniqueSymbols from lastAnswer not in symbolsSoFar
				TreeSet<TPTPParser.Symbol> uniqueSymbols = 
					LooksDifferent.getUniqueSymbols(symbolsSoFar, newSymbols);
				//----symbolsSOFar = uniqueSymbols U symbolsSoFar
				symbolsSoFar.addAll(uniqueSymbols);
				//----Get new set of ld axioms from the unique symbols
				ArrayList<String> ldAxiomsNew = LooksDifferent.addAxioms(uniqueSymbols, symbolList);
				//----Add ld axioms for those uniqueSymbols to ldAxiomsSoFar
				ldAxiomsSoFar.addAll(ldAxiomsNew);
				//----Add last answer to conjecture
				theTPTPFormula = LooksDifferent.addToConjecture(theTPTPFormula, lastAnswer);
				//----Create new conjectureTPTPFormula
				if (isQuestion) 
					conjectureTPTPFormula = "fof(1" + ",question,(" + theTPTPFormula + ")).";
				else 
					conjectureTPTPFormula = "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";				
				//----keep originalKBFile intact so that we do not
				//----have to keep recreating it, just copy and append
				//----to copy then delete copy, only delete original
				//----at the end of run delete last kbFileName
				if (kbFileName != null) 
					(new File(kbFileName)).delete();				
				//----kbFileName = originalKBFileName + all ld axioms + conjectureTPTPFormula;
				//----Copy original kb file
				
				stptpkb = new SUMOKBtoTPTPKB();
		    	stptpkb.kb = kb;
				kbFileName = stptpkb.copyFile(originalKBFileName);

				//----Append ld axioms and conjecture to the end
				stptpkb = new SUMOKBtoTPTPKB();
		    	stptpkb.kb = kb;
		    	stptpkb.addToFile(kbFileName, ldAxiomsSoFar, conjectureTPTPFormula);
				//----Reset last answer
				lastAnswer = null;
			} 
			else {
				//----kbFileName = originalKBFileName + conjectureTPTPFormula
				//----Copy original kb file and append conjecture to the end
				stptpkb = new SUMOKBtoTPTPKB();
		    	stptpkb.kb = kb;
				kbFileName = stptpkb.copyFile(originalKBFileName);
				System.out.println("  kbFileName == " + kbFileName);
				SUMOKBtoTPTPKB.addToFile(kbFileName, null, conjectureTPTPFormula);
			}					
			//----Call RemoteSoT
			if (location.equals("remote")) {
				if (systemChosen.equals("Choose%20system")) 
					resultAll += "No system chosen";            
				else {
					if (numAnswers == 0) 
						resultAll += "(Remote SystemOnTPTP call)";					
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
				if (systemChosen.equals("Choose%20system")) 
					resultAll += "No system chosen";
				else {
					if (numAnswers == 0) 
						resultAll += "(Local SystemOnTPTP call)";					
					atpOut = callLocalTPTP (systemChosen, kbFileName, timeout,
							quietFlag, tstpFormat);
					resultAll += atpOut.printResult;
					logger.finest("resultAll: " + resultAll);
					idvResult += atpOut.idvResult;
					logger.finest("idvResult: " + idvResult);
					result += atpOut.cleanResult;
					logger.finest("result: " + result);
					originalResult +=atpOut.originalResult; 
					logger.finest("originalResult: " + originalResult);
				}
			} 
			else if (location.equals("local") && builtInExists && !tptpWorldExists) {
				//----Call built in SystemOnTPTP instead of using RemoteSoT or local
				if (systemChosen.equals("Choose%20system")) 
					resultAll += "No system chosen";
				else {
					if (numAnswers == 0) 
						resultAll += "(Built-In SystemOnTPTP call)";					
					atpOut = callBuiltInTPTP (systemChosen, kbFileName, timeout,
							quietFlag, tstpFormat);
					resultAll += atpOut.printResult;
					idvResult += atpOut.idvResult;
					result += atpOut.cleanResult;
					logger.finest("result == " + result);
					originalResult += atpOut.originalResult;    
					logger.finest("originalResult == " + originalResult);
				}            
			}
			else {
				logger.warning("INTERNAL ERROR: chosen option not valid: " + location +
				".  Valid options are: 'Local SystemOnTPTP, Built-In SystemOnTPTP, or Remote SystemOnTPTP'.");
				resultAll += "INTERNAL ERROR: chosen option not valid: " + location +
				".  Valid options are: 'Local SystemOnTPTP, Built-In SystemOnTPTP, or Remote SystemOnTPTP'.";
			}
			
			//----If selected prover is not an ANSWER system, send proof to default ANSWER system (Metis)
			if (!(systemChosen.startsWith(TPTP_ANSWER_SYSTEM)
					&& location.equals("local")
					&& builtInExists 
					&& !tptpWorldExists)) {
				logger.finest("Sending proof to Metis because selected prover is not an Answer System.");
				String answerResult = AnswerFinder.findProofWithAnswers(result, BuiltInDir);
				//----If answer is blank, ERROR, or WARNING, do not place in result
				if (StringUtil.isNonEmptyString(answerResult) 
						&& !answerResult.startsWith("% ERROR:") 
						&& !answerResult.startsWith("% WARNING:")) {
					result = answerResult;
				} 
				//----If ERROR is answer result, report to user
				if (answerResult.startsWith("% ERROR:")) 
					resultAll += ("==" + answerResult);				
			}
			
			if (systemChosen.startsWith(TPTP_QUESTION_SYSTEM)) {
				//----Procedure if SNARK was chosen
				String conj = "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";
				ArrayList<Binding> answer = SystemOnTPTP.getSZSBindings(conj, originalResult);
				lastAnswer = answer;
				newResult = TPTP2SUMO.convert(result, answer, false);
			} 
			else {
				//----Procedure if not SNARK (call one answer system: Metis)
				try {
					logger.finest("Parsing the following result from Metis = " + result);
					TPTPParser parser = TPTPParser.parse(new BufferedReader(new StringReader(result)));
					lastAnswer = AnswerExtractor.extractAnswers(parser.ftable);
					//----Get original variable names
					lastAnswer = SystemOnTPTP.getSZSBindings(conjectureTPTPFormula, lastAnswer);
					newResult = TPTP2SUMO.convert(result, false);
				}
				catch (Exception e) {
					logger.severe("Error when parsing result from Metis: " + e.getMessage());
					resultAll = "Error parsing result from Metis = \n" + result;
				}
			}

			if (quietFlag.equals("IDV") && location.equals("remote")) {
				if (SystemOnTPTP.isTheorem(originalResult)) {
					int size = SystemOnTPTP.getTPTPFormulaSize(result);
					if (size == 0) 
						resultAll += "No solution output by system.  IDV tree unavaiable.";
					else 
						resultAll += idvResult;					
				} 
				else 
					resultAll += "Not a theorem.  IDV tree unavailable.";				
			} 
			else if (quietFlag.equals("IDV") && !location.equals("remote")) {
				if (SystemOnTPTP.isTheorem(originalResult)) {
					int size = SystemOnTPTP.getTPTPFormulaSize(result);
					if (size > 0) {
						String port = KBmanager.getMgr().getPref("port");
						if (StringUtil.emptyString(port))
							port = "8080";
						String hostname = KBmanager.getMgr().getPref("hostname");
						if (StringUtil.emptyString(hostname)) 
							hostname = "localhost";						
						StringTokenizer st = new StringTokenizer(result,"\n");
						String temp = "";
						while (st.hasMoreTokens()) {
							String next = st.nextToken();
							if (StringUtil.isNonEmptyString(next) 
									&& !next.startsWith("%")) 
								temp += next + "\n";							
						}
						result=temp;
						String libHref = "http://" + hostname + ":" + port + "/sigma/lib";
						resultAll += "<APPLET CODE=\"IDVApplet\" archive=\"" + libHref + "/IDV.jar," + libHref + "/TptpParser.jar," + libHref + "/antlr-2.7.5.jar," + libHref + "/ClientHttpRequest.jar\"\n";
						resultAll += "WIDTH=800 HEIGHT=100 MAYSCRIPT=true>\n";
						resultAll += "  <PARAM NAME=\"TPTP\" VALUE=\"" + result + "\">\n";
						resultAll += "  Hey, you cant see my applet!!!\n";
						resultAll += "</APPLET>\n";
					} 
					else 
						resultAll += "No solution output by system.  IDV tree unavaiable.";					
				} 
				else 
					resultAll += "Not a theorem.  IDV tree unavailable.";				
			} 
			else if (quietFlag.equals("hyperlinkedKIF")) {
				if (originalAnswer == null) 
					originalAnswer = lastAnswer;
				//----This is not the first answer, that means result has dummy ld predicates, bind conjecture with new answer, remove outside existential
				if (!lastAnswer.equals("")) {
					//resultAll += "<br>There was an Answer before! <br>";
					String bindConjecture = ("fof(bindConj, conjecture,(" 
							+ LooksDifferent.bindConjecture(originalConjecture, 
									originalAnswer,	lastAnswer)	+ ")).");
									//----With new bindConjecture, take last result, filter out anything with LDs in it, put in prover
					String axioms = LooksDifferent.filterLooksDifferent(originalResult);
					//----Redo proof using OneAnswerSystem again
					String bindProblem = axioms + " " + bindConjecture;
					String bindResult = AnswerFinder.findProof(bindProblem, BuiltInDir);
					newResult = TPTP2SUMO.convert(bindResult, lastAnswer, true);
				}
				boolean isTheorem = SystemOnTPTP.isTheorem(originalResult);
				boolean isCounterSatisfiable = SystemOnTPTP.isCounterSatisfiable(originalResult); 
				boolean proofExists = SystemOnTPTP.proofExists(originalResult);
				int timeUsed = SystemOnTPTP.timeUsed(originalResult);
				if (isTheorem) { 
					if (proofExists) {
						try {
							//----Remove bindings, if no existential quantifiers have been 
							//----made explicit, i.e., the query is closed
							if (suppressAnswerExtraction) {
								int opnTagIdx = newResult.indexOf("  <bindingSet");
								int clsTagIdx = newResult.indexOf("</bindingSet>");
								int idx3 = (clsTagIdx + 14);
								if ((opnTagIdx > -1) 
										&& (clsTagIdx > opnTagIdx)
										&& (idx3 < newResult.length())) {
									newResult =
										newResult
										.substring(0,newResult.indexOf("  <bindingSet"))
										+ newResult
										.substring(newResult.indexOf("</bindingSet>") + 14);
									lastAnswer = null;
								}
							}
							// System.out.println(newResult);
							//----If a proof exists, print out as hyperlinked kif
							resultAll += HTMLformatter.formatProofResult(newResult,
									stmt,stmt,lineHtml,kbName,language,numAnswers+1);
						} 
						catch (Exception e) {}
					} 
					else 
						//----Proof does not exist, but was a theorem
						resultAll += "Answer "+(numAnswers+1)+". Yes [Theorem]<br>";			
				} 
				else if (isCounterSatisfiable) 
					resultAll += "Answer "+(numAnswers+1)+". No [CounterSatisfiable]<br>";
				else if (numAnswers == 0) 
						resultAll += "Answer "+(numAnswers+1)+". No<br>";				
			}
			//----If lastAnswer != null (we found an answer) && there is an answer (lastAnswer.size() > 0)
			if (lastAnswer != null && lastAnswer.size() > 0) 
				numAnswers++;
			//----Add query time limit to while loop break
		} while (numAnswers < maxAnswers && lastAnswer != null && lastAnswer.size() > 0);
		
		//----Delete the kbFile
		if (originalKBFileName != null) 
			(new File(originalKBFileName)).delete();		

		logger.exiting("InterfaceTPTP", "queryTPTP", resultAll);
		return resultAll;
	}

	/** ***************************************************************
	 */
	public static void main () {
	}
}

