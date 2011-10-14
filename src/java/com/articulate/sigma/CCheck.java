package com.articulate.sigma;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import TPTPWorld.InterfaceTPTP;

public class CCheck implements Runnable {
	private KB kb;
	private File ccheckFile;
	private FileWriter fw;
	private PrintWriter pw;
	private Logger logger;
	private String ccheck_kb;
	private String inferenceEngine;
	private HashMap<String, String> ieSettings;
	private int timeOut = 10;
	private String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
	
	public CCheck(KB kb, String filename) {
		this.kb = kb;
		logger = Logger.getLogger(this.getClass().getName());
		
		try {
			ccheckFile = new File(filename);
						
			fw = new FileWriter(ccheckFile);
			pw = new PrintWriter(fw);
			
		}
		catch (Exception e) {
			logger.severe(e.getStackTrace().toString());
		}
	}
	
	public CCheck(KB kb, String fileName, String chosenEngine, int timeout) throws Exception {
		this(kb, fileName);

		timeOut = timeout;

		if (setInferenceEngine(chosenEngine) == false) {
			logger.severe("Unable to create CCheck for kb: " + kb.name + "; Error setting up inference engine = " + inferenceEngine);
			throw new Exception("Could not set inference engine with the following params for KB " + kb.name + ". Inference Engine chosen = " + chosenEngine);
		}

	}

	public CCheck(KB kb, String fileName, String chosenEngine, String systemChosen, String quietFlag,
				String location, String language, int timeout) throws Exception {
		this (kb, fileName);

		timeOut = timeout;
		
		if (!setInferenceEngine(chosenEngine, systemChosen, location.toLowerCase(), quietFlag, language))
			throw new Exception("Could not set inference engine with the following params: {chosenEngine=" + chosenEngine + ", systemChosen=" + systemChosen + ", location=" + location + "}");
		else logger.info("Set up inference engine for Consistency Check of KB: " + kb.name + ". Engine Chosen: " + chosenEngine);

	}

	/** This sets the inference engine to be used for the consistency check.  This particular method sets it if chosenEngine == 'SoTPTP'
	 * 
	 * @param chosenEngine - string describing the inference engine to be used.  for this particular method, it should be 'SoTPTP'
	 * @param systemChosen - the theorem prover to be used
	 * @param location - if it's local or remote
	 * @param quietFlag - command option as to the verbosity of the result 
	 * @param language - language for formatting
	 * @return true if there are no errors in setting the engine, false if errors are encountered.
	 */
	private boolean setInferenceEngine(String chosenEngine, String systemChosen, String location,
			String quietFlag, String language) {

		try {
			if (chosenEngine.equals("SoTPTP")) {
				String result = InterfaceTPTP.queryTPTP("(instance instance BinaryPredicate)", 10, 1, lineHtml,
						systemChosen, location, quietFlag, kb.name, language);
				inferenceEngine = "SoTPTP";
				ieSettings = new HashMap<String, String>();
				ieSettings.put("systemChosen", systemChosen);
				if (location == "" || location == null) 
					return false;
				else ieSettings.put("location", location);
				
				if (quietFlag == "" || quietFlag == null)
					ieSettings.put("quietFlag", "hyperlinkedKIF");
				else
					ieSettings.put("quietFlag", quietFlag);
				
				if (language == "" || language == null)
					language = "EnglishLanguage";
				ieSettings.put("language", language);
				
				return true;
				
			}
			else setInferenceEngine(chosenEngine);
		}
		catch (Exception e) {
			logger.severe("Error in setting up SystemOnTPTP: " + e.getMessage());
			return false;
		}
		return false;
	}
	
	/**This sets the inference engine to be used for the consistency check.  It sends a test query to the inference engine to
	 * ensure that the engine works.
	 * @param chosenEngine - string describing the inference engine to be used.
	 * @return true if there are no errors in setting the engine, false if errors are encountered
	 */
	
	private boolean setInferenceEngine(String chosenEngine) {
		String result = "";
		try {
			if (chosenEngine.equals("Vampire")) {
				result = kb.ask("(instance instance BinaryPredicate)", 10, 1);
				inferenceEngine = "Vampire";
				return true;
			}
			else if (chosenEngine.equals("STP")) {
				result = kb.askSTP("(instance instance BinaryPredicate)", 10, 1);
				inferenceEngine = "STP";
				return true;
			}
			else if (chosenEngine.equals("STP2")) {
				result = kb.askSTP2("(instance instance BinaryPredicate)", 10, 1);
				inferenceEngine = "STP2";
				return true;
			}
			else if (chosenEngine.equals("SInE")) {
				result = kb.askSInE("(instance instance BinaryPredicate)", 10, 1);
				inferenceEngine = "SInE";
				return true;
			}
			else if (chosenEngine.equals("LeoSine")) {
				result = kb.askLEO("(instance instance BinaryPredicate)", 10, 1, "LeoSine");
				inferenceEngine = "LeoSine";
				return true;
			}
			else if (chosenEngine.equals("LeoLocal")) {
				result = kb.askLEO("(instance instance BinaryPredicate)", 10, 1, "LeoLocal");
				inferenceEngine = "LeoLocal";
				return true;
			}
			else if (chosenEngine.equals("LeoGlobal")) {
				result = kb.askLEO("(instance instance BinaryPredicate)", 10, 1, "LeoGlobal");
				inferenceEngine = "LeoGlobal";
				return true;
			}        
			else return false;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public String getKBName() {
		return kb.name;
	}

    private KB makeEmptyKB() {
    	ccheck_kb = "CCheck-" + kb.name;
        String kbDir = (String)KBmanager.getMgr().getPref("kbDir");
        if (KBmanager.getMgr().existsKB(ccheck_kb)) {
            KBmanager.getMgr().removeKB(ccheck_kb);
        }
        File dir = new File( kbDir );
        File emptyCFile = new File( dir, "emptyConstituent.txt" );
        String emptyCFilename = emptyCFile.getAbsolutePath();
        
        FileWriter fwriter = null; 
        PrintWriter pwriter = null;

        KBmanager.getMgr().addKB(ccheck_kb, false);
        KB empty = KBmanager.getMgr().getKB(ccheck_kb);
        logger.info("KB created: " + empty);
        
        try { // Fails elsewhere if no constituents, or empty constituent, thus...
        	fwriter = new FileWriter( emptyCFile );
        	pwriter = new PrintWriter(fwriter);   
        	pwriter.println("(instance instance BinaryPredicate)\n");
            if (pwriter != null) pwriter.close();
            if (fwriter != null) fwriter.close();
            empty.addConstituent(emptyCFilename);
        }
        catch (java.io.IOException e) {
            logger.warning("Error writing file " + emptyCFilename);
        }
        catch (Exception e) {
            logger.warning(e.getMessage());
        }
        return empty;
    }
	

    /***
     * This method saves the answer and proof for detected redundancies or inconsistencies into
     * the file.
     * @param proof - the proof presented that establishes the redundancy or inconsistency
     * @param query - the statement that caused the error
     * @param testType - whether it is a redundancy or inconsistency
     */
    private void reportAnswer(String proof, Formula query, String testType, String processedQ, String sourceFile) {
    	   	
        if (proof.indexOf("Syntax error detected") != -1) {
        	pw.println("    <entry>");
        	pw.println("      <query>");        	
        	pw.println("        " + query.theFormula);
        	pw.println("      </query>");
        	pw.println("      <processedStatement>");
        	pw.println("        " + processedQ);
        	pw.println("      </processedStatement>");
        	pw.println("      <sourceFile>");
        	if (sourceFile != null)
        		pw.println("        " + sourceFile);
        	pw.println("      </sourceFile>");
        	pw.println("      <type>");
        	pw.println("        Syntax error in formula");
        	pw.println("      </type>");
        	pw.println("      <proof src=\"" + inferenceEngine + "\">");
        	String[] split = proof.split("\n");
        	for (int i=0; i < split.length; i++)
        		pw.println("      " + split[i]);
        	pw.println("      </proof>");
        	pw.println("    </entry>");
        }
 else if (inferenceEngine.equals("Vampire")) {
	        BasicXMLparser res = new BasicXMLparser(proof);
	        ProofProcessor pp = new ProofProcessor(res.elements);
	        if (!pp.returnAnswer(0).equalsIgnoreCase("no")) {
	        	pw.println("    <entry>");
	        	pw.println("      <query>");        	
	        	pw.println("        " + query.theFormula);
	        	pw.println("      </query>");
	        	pw.println("      <processedStatement>");
	        	pw.println("        " + processedQ);
	        	pw.println("      </processedStatement>");
	        	pw.println("      <sourceFile>");
	        	if (sourceFile != null)
	        		pw.println("        " + sourceFile);
	        	pw.println("      </sourceFile>");
	        	pw.println("      <type>");
	        	pw.println("        " + testType);
	        	pw.println("      </type>");
	        	pw.println("      <proof src=\"" + inferenceEngine + "\">");
	        	String[] split = proof.split("\n");
	        	for (int i=0; i < split.length; i++)
	        		pw.println("      " + split[i]);
	        	pw.println("      </proof>");
	        	pw.println("    </entry>");
	        }
        }
        else if (inferenceEngine.equals("SoTPTP")) {
        	try {
				proof = proof.replaceAll("<", "%3C");
				proof = proof.replaceAll(">", "%3E");
				proof = proof.replaceAll("/n", "");
        		
        		boolean resultFound = false;
        		boolean inconsistencyFound = false;
        		int i = 0;

				if (proof.contains("[yes]") || proof.contains("[Theorem]")
						|| proof.contains("[definite]")) {
		        	pw.println("    <entry>");
		        	pw.println("      <query>");        	
		        	pw.println("        " + query.theFormula);
		        	pw.println("      </query>");
		        	pw.println("      <processedStatement>");
		        	pw.println("        " + processedQ);
		        	pw.println("      </processedStatement>");
		        	pw.println("      <sourceFile>");
		        	if (sourceFile != null)
		        		pw.println("        " + sourceFile);
		        	pw.println("      </sourceFile>");
		        	pw.println("      <type>");
		        	pw.println("        " + testType);
		        	pw.println("      </type>");
		        	pw.println("      <proof src=\"" + inferenceEngine + "\">");
					pw.println(proof);
		        	pw.println("      </proof>");
		        	pw.println("    </entry>");
	        	}
        	}
        	catch (Exception ex) {
        		reportError(ex.getMessage(), query, processedQ, sourceFile);
        	}
	   }
        
        
        try {
        	pw.flush();
        	fw.flush();
        }
        catch (Exception ex) {
        	logger.warning(ex.getMessage());
        }
    }
    
	/**
	 * This would save the error message for a formula in the CCheck results
	 * file to inform the user that an error occurred while performing a
	 * consistency check on one of the statements.
	 * 
	 * @param message
	 *            - error message
	 * @param query
	 *            - the formula being tested
	 * @param processedQ
	 *            - the processed query
	 * @param sourceFile
	 *            - the source file where the formula being tested came from
	 */
    private void reportError(String message, Formula query, String processedQ, String sourceFile) {
    	pw.println("    <entry>");
    	pw.println("      <query>");        	
    	pw.println("        " + query.theFormula);
    	pw.println("      </query>");
    	pw.println("      <processedStatement>");
    	pw.println("        " + processedQ);
    	pw.println("      </processedStatement>");
    	pw.println("      <sourceFile>");
    	if (sourceFile != null)
    		pw.println("        " + sourceFile);
    	pw.println("      </sourceFile>");
    	pw.println("      <type>");
    	pw.println("        Error from Inference Engine");
    	pw.println("      </type>");
    	pw.println("      <proof src=\"" + inferenceEngine + "\">");
    	pw.println("        " + message);
    	pw.println("      </proof>");
    	pw.println("    </entry>");
    	
        try {
        	pw.flush();
        	fw.flush();
        }
        catch (Exception ex) {
        	logger.warning(ex.getMessage());
        }
    }
    
    
    /**
     * This initiates the consistency check
     */
	private void runConsistencyCheck() {
		logger.entering("CCheck", "runConsistencyCheck");
		
		String proof;
		
		KB empty = this.makeEmptyKB();
		
		try {
			pw.println("<ConsistencyCheck>");
			pw.println("  <kb>");
			pw.println("    " + kb.name);
			pw.println("  </kb>");
			
			Collection<Formula> allFormulas = kb.formulaMap.values();
			Iterator<Formula> it = allFormulas.iterator();
			pw.println("  <entries>");
			
			while (it.hasNext()) {
				Formula query = (Formula) it.next();
				ArrayList processedQueries = query.preProcess(false, kb);
				
				String processedQuery = null;
				String sourceFile = null;
				Iterator q = processedQueries.iterator();

				logger.finer("processedQuery size = " + processedQueries.size());
				
				while(q.hasNext()) {
					Formula f = (Formula) q.next();
					logger.finer("Current formula = " + f.theFormula);
					
					processedQuery = f.makeQuantifiersExplicit(false);
					logger.finer("Processed Query = " + processedQuery);
					
					sourceFile = f.sourceFile;
					sourceFile = sourceFile.replace("/", "&#47;");

					logger.finer("Source File = " + sourceFile);
					
					try {
						proof = askInferenceEngine(empty, processedQuery);
						reportAnswer(proof, query, "Redundancy", processedQuery, sourceFile);
						logger.info("Proof: " + proof);
					}
					catch(Exception e) {
					    reportError(e.getMessage(), query, processedQuery, sourceFile);			        	
			        	logger.warning("Error from inference engine: " + e.getMessage());
					}
						
					StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");

                    try {
	                    proof = askInferenceEngine(empty, negatedQuery.toString());
	                    reportAnswer(proof, query ,"Inconsistency", processedQuery, sourceFile);   
						logger.info("Proof: " + proof);
					}
					catch(Exception e) {
					    reportError(e.getMessage(), query, processedQuery, sourceFile);			        	
			        	logger.warning("Error from inference engine: " + e.getMessage());
					}


				}				
                
				empty.tell(query.theFormula);
			}
			pw.println("  </entries>");
			pw.print("</ConsistencyCheck>");
		}
		catch (Exception e) {
			pw.println("  </entries>");
			pw.print("  <error>");
			pw.print("Error encountered while running consistency check.");
			pw.println("</error>");			
			pw.print("</ConsistencyCheck>");
			logger.warning(e.getMessage());			
		}
		finally {
			KBmanager.getMgr().removeKB(ccheck_kb);
		}
		
		logger.exiting("CCheck", "runConsistencyCheck");
	}

	/**
	 * Picks the inference engine to use for the consistency check based on the
	 * set-up inference engine.
	 * 
	 * @param empty
	 *            - the kb to be used for the check
	 * @param query
	 *            - the statement to be checked
	 * @return - the result of the query
	 */
	private String askInferenceEngine(KB empty, String query) {
		String result = "";
		
		try {
			if (inferenceEngine.equals("Vampire")) {
				result = empty.ask(query, timeOut, 1);
			}
			else if (inferenceEngine.equals("STP")) {
				result = empty.askSTP(query, timeOut, 1);
			}
			else if (inferenceEngine.equals("STP2")) {
				result = empty.askSTP2(query, timeOut, 1);
			}
			else if (inferenceEngine.equals("SInE")) {
				result = empty.askSInE(query, timeOut, 1);
			}
			else if (inferenceEngine.equals("LeoSine")) {
				result = empty.askLEO(query, timeOut, 1, "LeoSine");
			}
			else if (inferenceEngine.equals("LeoLocal")) {
				result = empty.askLEO(query, timeOut, 1, "LeoLocal");
			}
			else if (inferenceEngine.equals("LeoGlobal")) {
				result = empty.askLEO(query, timeOut, 1, "LeoGlobal");
			}        
			else if (inferenceEngine.equals("SoTPTP")) {
				result = InterfaceTPTP.queryTPTP(query, timeOut, 1, lineHtml,
						ieSettings.get("systemChosen"),
						ieSettings.get("location"),
						ieSettings.get("quietFlag"), empty.name,
						ieSettings.get("language"));

			}						
			else throw new Exception("No inference engine.");
		}
		catch (Exception e) {
			logger.warning(e.getMessage());
			result = "ERROR [for query: " + query + "]: " + e.getMessage(); 
		}
		return result;
	}
	
	@Override
	public void run() {
		runConsistencyCheck();
	}

}
