package com.articulate.sigma;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

public class CCheck implements Runnable {
	private KB kb;
	private File ccheckFile;
	private FileWriter fw;
	private PrintWriter pw;
	private Logger logger;
	private String ccheck_kb;
	
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
    	StringBuffer str = new StringBuffer();
    	   	
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
        	pw.println("      <proof>");
        	String[] split = proof.split("\n");
        	for (int i=0; i < split.length; i++)
        		pw.println("      " + split[i]);
        	pw.println("      </proof>");
        	pw.println("    </entry>");
        }
            
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
        	pw.println("      <proof>");
        	String[] split = proof.split("\n");
        	for (int i=0; i < split.length; i++)
        		pw.println("      " + split[i]);
        	pw.println("      </proof>");
        	pw.println("    </entry>");
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
     * This initiates the consistency check
     */
	private void runConsistencyCheck() {
		logger.entering("CCheck", "runConsistencyCheck");
		
		int timeout = 10;
		int maxAnswers = 1;
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
					
					proof = empty.ask(processedQuery, timeout, maxAnswers);
					reportAnswer(proof, query, "Redundancy", processedQuery, sourceFile);
					
					StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");
                    proof = empty.ask(negatedQuery.toString(),timeout,maxAnswers);

                    reportAnswer(proof, query ,"Inconsistency", processedQuery, sourceFile);   

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
	
	@Override
	public void run() {
		runConsistencyCheck();
	}

}
