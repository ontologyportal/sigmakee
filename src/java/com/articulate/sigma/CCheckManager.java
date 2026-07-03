// package com.articulate.sigma;

// import java.io.BufferedReader;
// import java.io.File;
// import java.io.FileReader;
// import java.io.Reader;
// import java.sql.Timestamp;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.LinkedBlockingQueue;
// import java.util.concurrent.RejectedExecutionException;
// import java.util.concurrent.ThreadPoolExecutor;
// import java.util.concurrent.TimeUnit;
// import java.util.logging.Level;
// import java.util.logging.Logger;


// /** This code is copyright Articulate Software (c) 2014.
//  This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
//  Users of this code also consent, by use of this code, to credit Articulate Software
//  and Teknowledge in any writings, briefings, publications, presentations, or
//  other representations of any software which incorporates, builds on, or uses this
//  code.  Please cite the following article in any publication with references:

//  Pease, A., (2003). The Sigma Ontology Development Environment,
//  in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
//  August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
//  */

// /** ***************************************************************
//  * This class manages the threads that run consistency checks for the different
//  * KBs in the system.
//  * @author Karen Joy Nomorosa, Rearden Commerce Inc.
//  *
//  */
// public class CCheckManager extends ThreadPoolExecutor {
// 	/**
// 	 * ccheckQueue keeps track of the KBs that are currently being checked.
// 	 * checkedKBs keeps track of the KBs that have already been checked, and a timestamp of when that check finished.
// 	 */
// 	public enum CCheckStatus {
// 		ONGOING, DONE, QUEUED, NOCCHECK, ERROR
// 	}

// 	private final Map<String, Map<String, Object>> checkedKBs = new ConcurrentHashMap<>();
// 	private final Map<String, String> ccheckQueue = new ConcurrentHashMap<>();
// 	private static final Logger logger = Logger.getLogger(CCheckManager.class.getName());

// 	public CCheckManager() {
		
// 		super(3, 3, 50000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10));
// 	}

// 	/** ***************************************************************
// 	 * Returns the timestamp of when the last consistency check was run on this KB.
// 	 * @param kbName - name of the KB
// 	 * @return Timestamp if a consistency check has been run previously, null if it hasn't.
// 	 */
// 	public Timestamp lastCCheck(String kbName) {

// 		if (checkedKBs.containsKey(kbName)) {
// 			Map<String, Object> obj = checkedKBs.get(kbName);

// 			if (obj.containsKey("timestamp"))
// 				return (Timestamp) obj.get("timestamp");
// 		}

// 		return null;
// 	}

// 	/** ***************************************************************
// 	 * This method returns full or partial results of the consistency checks.
// 	 * @param kbName - name of the KB that we want the results of
// 	 * @return SimpleElement of the parsed XML file or null if there are errors or it does not exist.
// 	 */
// 	public String ccheckResults(String kbName) {

// 		logger.entering("CCheckManager", "ccheckResults", "kbName = " + kbName);
// 		StringBuilder result = new StringBuilder();

// 		// These are the KBs that are still undergoing consistency checks
// 		if (ccheckQueue.containsKey(kbName)) {
// 			String filename = ccheckQueue.get(kbName);
// 			if (filename != null) {
// 				try (Reader fr = new FileReader(filename);
//                                      BufferedReader br = new BufferedReader(fr)) {

// 					String line;
// 					while ((line = br.readLine()) != null)
// 						result.append(line).append("\n");

// 					// Need to append the closing tags because these probably have not been added by
// 					// CCheck.java yet, as the process is still ongoing.
// 					if (result.length() > 0)
// 						result.append("  </entries>\n</ConsistencyCheck>");

// 					logger.exiting("CCheckManager", "ccheckResults", result.toString());
// 					return result.toString();
// 				} catch (Exception ex){
// 					logger.warning(ex.getMessage());
// 				}
// 			}
// 		}
// 		else if (checkedKBs.containsKey(kbName)) {
// 			// These are for the consistency checks that are already done.
// 			// Note that code in performing CChecks ensures that a KBName cannot be in
// 			// both checkedKBs and ccheckQueue at the same time.
// 			Map<String, Object> value = checkedKBs.get(kbName);
// 			String filename = (String) value.get("filename");

// 			if (filename != null) {
// 				try (Reader fr = new FileReader(filename);
//                                      BufferedReader br = new BufferedReader(fr)) {

// 					String line;
// 					while ((line = br.readLine()) != null)
// 						result.append(line);

// 					logger.exiting("CCheckManager", "ccheckResults", result.toString());
// 					return result.toString();
// 				}
// 				catch (Exception ex){
// 					logger.warning(ex.getMessage());
// 				}
// 			}
// 			else {
// 				logger.exiting("CCheckManager", "ccheckResults", null);
// 				return null;
// 			}
// 		}
// 		logger.exiting("CCheckManager", "ccheckResults", null);
// 		return null;
// 	}

//     /** ***************************************************************
// 	 * Returns the current status of a KB
// 	 * @param kbName - the name of the KB to be checked
// 	 * @return true if there is a worker thread currently performing consistency checks on it, and false if not
// 	 */
// 	public CCheckStatus ccheckStatus(String kbName) {

// 		if (ccheckQueue.containsKey(kbName))
// 			return CCheckStatus.ONGOING;
// 		else if (checkedKBs.containsKey(kbName))
// 			return CCheckStatus.DONE;
// 		else return CCheckStatus.NOCCHECK;
// 	}

// 	/** ***************************************************************
// 	 * Main code that performs the consistency check on the KB.
// 	 * @param kb KB to be checked
// 	 * @param userSessionId session id for isolated prover files
// 	 * @param proverType prover selected by the UI
// 	 * @param language TPTP-family language: FOF, TFF, or THF
// 	 * @param vampireMode Vampire mode
// 	 * @param closedWorldAssumption whether to use CWA
// 	 * @param modusPonens whether to use modus ponens mode
// 	 * @param dropOnePremise whether to drop one-premise formulas
// 	 * @param holUseModals whether to use modal HOL translation
// 	 * @param timeout timeout in seconds
// 	 * @param maxAnswers maximum answers
// 	 * @return the status of the check whether it has been accepted or rejected
// 	 */
// 	public CCheckStatus performConsistencyCheck(KB kb,
// 			String userSessionId,
// 			String proverType,
// 			String language,
// 			String vampireMode,
// 			boolean closedWorldAssumption,
// 			boolean modusPonens,
// 			boolean dropOnePremise,
// 			boolean holUseModals,
// 			int timeout,
// 			int maxAnswers) {

// 		String filename = "CCHECK_" + kb.name;
// 		if (KBmanager.configuration.getBaseDir() != null)
// 			filename = KBmanager.configuration.getBaseDir() + File.separator + filename;
// 		if (ccheckQueue.putIfAbsent(kb.name, filename) != null) {
// 			logger.log(Level.INFO,
// 					"KB {0} has been rejected for consistency check as it is already undergoing the check.",
// 					kb.name);
// 			return CCheckStatus.ONGOING;
// 		}
// 		try {
// 			CCheck ccheck = new CCheck(
// 				kb,
// 				filename,
// 				userSessionId,
// 				proverType,
// 				language,
// 				vampireMode,
// 				closedWorldAssumption,
// 				modusPonens,
// 				dropOnePremise,
// 				holUseModals,
// 				timeout,
// 				maxAnswers);
// 			checkedKBs.remove(kb.name);
// 			super.execute(ccheck);
// 			logger.log(Level.INFO, "KB {0} has been added to the queue for consistency check.", kb.name);
// 			return CCheckStatus.QUEUED;
// 		}
// 		catch (RejectedExecutionException e) {
// 			ccheckQueue.remove(kb.name, filename);
// 			logger.warning(e.getMessage());
// 			return CCheckStatus.ERROR;
// 		}
// 		catch (Exception e) {
// 			ccheckQueue.remove(kb.name, filename);
// 			logger.warning(e.getMessage());
// 			return CCheckStatus.ERROR;
// 		}
// 	}

// 	/** ***************************************************************
// 	 * Main code that performs the consistency check on the KB.
// 	 * @param kb - KB to be checked
// 	 * @return the status of the check whether it has been accepted or rejected
// 	 */
// 	public CCheckStatus performConsistencyCheck(KB kb, String chosenEngine, String systemChosen,
// 			String location, String language, int timeout) {

// 		String filename = "CCHECK_" + kb.name;
// 		if (KBmanager.configuration.getBaseDir() != null)
// 			filename = KBmanager.configuration.getBaseDir() + File.separator + filename;
// 		if (ccheckQueue.putIfAbsent(kb.name, filename) != null) {
// 			logger.log(Level.INFO,
// 					"KB {0} has been rejected for consistency check as it is already undergoing the check.",
// 					kb.name);
// 			return CCheckStatus.ONGOING;
// 		}
// 		try {
// 			CCheck ccheck;
// 			if ("SoTPTP".equals(chosenEngine)) {
// 				ccheck = new CCheck(kb, filename, chosenEngine,
// 						systemChosen, "hyperlinkedKIF", location, language, timeout);
// 			}
// 			else {
// 				ccheck = new CCheck(kb, filename, chosenEngine, timeout);
// 			}
// 			checkedKBs.remove(kb.name);
// 			super.execute(ccheck);
// 			logger.log(Level.INFO, "KB {0} has been added to the queue for consistency check.", kb.name);
// 			return CCheckStatus.QUEUED;
// 		}
// 		catch (RejectedExecutionException e) {
// 			ccheckQueue.remove(kb.name, filename);
// 			logger.warning(e.getMessage());
// 			return CCheckStatus.ERROR;
// 		}
// 		catch (Exception e) {
// 			ccheckQueue.remove(kb.name, filename);
// 			logger.warning(e.getMessage());
// 			return CCheckStatus.ERROR;
// 		}
// 	}

//     /** ***************************************************************
// 	 * Removes the KB from the list of kbs currently being checked, and add it to the checkedKBs list.
// 	 * This method is overridden from the parent class.
// 	 * @param r
// 	 * @param t
// 	 */
// 	@Override
// 	protected void afterExecute(Runnable r, Throwable t) {

// 		try {
// 			if (r instanceof CCheck) {
// 				CCheck ccheck = (CCheck) r;
// 				Map<String, Object> value = new HashMap<>();
// 				value.put("timestamp", new Timestamp((new Date()).getTime()));
// 				value.put("filename", ccheckQueue.get(ccheck.getKBName()));
// 				checkedKBs.put(ccheck.getKBName(), value);
// 				ccheckQueue.remove(ccheck.getKBName());
// 			}
// 		}
// 		finally {
// 			super.afterExecute(r, t);
// 		}
// 	}
// }
