package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.articulate.sigma.KB;

/** This code is copyright Articulate Software (c) 2014.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
 */

/** ***************************************************************
 * This class manages the threads that run consistency checks for the different 
 * KBs in the system.
 * @author Karen Joy Nomorosa, Rearden Commerce Inc.
 *
 */
public class CCheckManager extends ThreadPoolExecutor {
	/**
	 * ccheckQueue keeps track of the KBs that are currently being checked.
	 * checkedKBs keeps track of the KBs that have already been checked, and a timestamp of when that check finished.
	 */
	public enum CCheckStatus {
		ONGOING, DONE, QUEUED, NOCCHECK, ERROR
	}
	
	private HashMap<String, HashMap<String, Object>> checkedKBs = null;
	private HashMap<String, String> ccheckQueue= null;
	private Logger logger = null;
	
	public CCheckManager() {
		super(3, 3, 50000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(10));

		if (logger == null)
			logger = Logger.getLogger(this.getClass().getName());
		
		ccheckQueue = new HashMap<String, String>();
		checkedKBs = new HashMap<String, HashMap<String, Object>>();
	}

	/** ***************************************************************
	 * Returns the timestamp of when the last consistency check was run on this KB.
	 * @param kbName - name of the KB
	 * @return Timestamp if a consistency check has been run previously, null if it hasn't.
	 */
	public Timestamp lastCCheck(String kbName) {

		if (checkedKBs.containsKey(kbName)) {
			HashMap<String, Object> obj = checkedKBs.get(kbName);
			
			if (obj.containsKey("timestamp"))
				return (Timestamp) obj.get("timestamp");
		}
		
		return null;
	}

	/** ***************************************************************
	 * This method returns full or partial results of the consistency checks.
	 * @param kbName - name of the KB that we want the results of
	 * @return SimpleElement of the parsed XML file or null if there are errors or it does not exist.
	 */
	public String ccheckResults(String kbName) {

		logger.entering("CCheckManager", "ccheckResults", "kbName = " + kbName);
		StringBuilder result = new StringBuilder();
		FileReader fr = null;
		BufferedReader br = null;

		// These are the KBs that are still undergoing consistency checks
		if (ccheckQueue.containsKey(kbName)) {
			String filename = ccheckQueue.get(kbName);
			if (filename != null) {
				try {
					fr = new FileReader(filename);
					br = new BufferedReader(fr);
					
					String line = null;
					while ((line = br.readLine()) != null)
						result.append(line + "\n");
					
					// Need to append the closing tags because these probably have not been added by 
					// CCheck.java yet, as the process is still ongoing.
					if (result.length() > 0)
						result.append("  </entries>\n</ConsistencyCheck>");

					logger.exiting("CCheckManager", "ccheckResults", result.toString());
					return result.toString();
				}
				catch (Exception ex) {
					if (fr != null)
						try {
							fr.close();
						}
						catch (Exception e1) {
							logger.warning(e1.getMessage());
						}
					if (br != null)
						try {
							br. close();
						}
						catch (Exception e2) {
							logger.warning(e2.getMessage());
						}
					logger.warning(ex.getMessage());
				}
			}			
		}
		else if (checkedKBs.containsKey(kbName)) {
			// These are for the consistency checks that are already done.  
			// Note that code in performing CChecks ensures that a KBName cannot be in 
			// both checkedKBs and ccheckQueue at the same time.
			HashMap<String, Object> value = checkedKBs.get(kbName);
			String filename = (String) value.get("filename");
			
			if (filename != null) {
				try {				
					fr = new FileReader(filename);
					br = new BufferedReader(fr);
					
					String line = null;
					while ((line = br.readLine()) != null)
						result.append(line);
					
					logger.exiting("CCheckManager", "ccheckResults", result.toString());					
					return result.toString();
				}
				catch (Exception ex){
					logger.warning(ex.getMessage());
				}
			}
			else {
				logger.exiting("CCheckManager", "ccheckResults", null);
				return null;
			}
		}
		logger.exiting("CCheckManager", "ccheckResults", null);
		return null;
	}

    /** ***************************************************************
	 * Returns the current status of a KB
	 * @param kbName - the name of the KB to be checked
	 * @return true if there is a worker thread currently performing consistency checks on it, and false if not
	 */
	public CCheckStatus ccheckStatus(String kbName) {

		if (ccheckQueue.containsKey(kbName))
			return CCheckStatus.ONGOING;
		else if (checkedKBs.containsKey(kbName))
			return CCheckStatus.DONE;
		else return CCheckStatus.NOCCHECK;
	}

    /** ***************************************************************
	 * Main code that performs the consistency check on the KB.
	 * @param kb - KB to be checked
	 * @return the status of the check (whether it has been accepted or rejected)
	 */
	public CCheckStatus performConsistencyCheck(KB kb, String chosenEngine, String systemChosen,  
			String location, String language, int timeout) {
		
		if (!ccheckQueue.containsKey(kb.name)) {
			try {
				String filename = "CCHECK_" + kb.name;
				if (KBmanager.getMgr().getPref("baseDir") != null)
					filename = KBmanager.getMgr().getPref("baseDir") + File.separator + filename;

				// lines up the Runnable CCheck for execution
				if (chosenEngine.equals("SoTPTP"))
					super.execute(new CCheck(kb, filename, chosenEngine,
							systemChosen, "hyperlinkedKIF", location, language,
							timeout));
				else
					super.execute(new CCheck(kb, filename, chosenEngine, timeout));
			
				ccheckQueue.put(kb.name, filename);
				
				// remove this KB from checkedKBs because a new consistency check is being run for it.
				if (checkedKBs.containsKey(kb.name))
					checkedKBs.remove(kb.name);
				
				logger.info("KB " + kb.name + " has been added to the queue for consistency check.");
				return CCheckStatus.QUEUED;
			}
			catch (RejectedExecutionException e) {
				logger.warning(e.getMessage());
				return CCheckStatus.ERROR;
			}			
			catch (Exception e) {
				logger.warning(e.getMessage());
				return CCheckStatus.ERROR;
			}
		}
		else {
			logger.info("KB " + kb.name + "has been rejected for consistency check as it is already undergoing the check.");
			return CCheckStatus.ONGOING;
		}
	}

    /** ***************************************************************
	 * Removes the KB from the list of kbs currently being checked, and add it to the checkedKBs list.  
	 * This method is overridden from the parent class.
	 * @param r 
	 * @param t
	 */
	protected void afterExecute(CCheck r, Throwable t) {
		
		HashMap<String, Object> value = new HashMap<String, Object>();
		value.put("timestamp", new Timestamp((new Date()).getTime()));
		value.put("filename", ccheckQueue.get(r.getKBName()));
		checkedKBs.put(r.getKBName(), value);
		ccheckQueue.remove(r.getKBName());

		super.afterExecute(r, t);
	}
}
