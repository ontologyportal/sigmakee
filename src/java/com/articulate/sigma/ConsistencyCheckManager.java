package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/****************************************************************
 * Manages queued and completed consistency checks.
 */
public class ConsistencyCheckManager extends ThreadPoolExecutor {

    /****************************************************************
     * Status for a KB consistency check.
     */
    public enum ConsistencyCheckStatus {
        ONGOING, DONE, QUEUED, NOCCHECK, ERROR
    }

    /****************************************************************
     * Mode for a KB consistency check.
     */
    public enum ConsistencyCheckMode {
        GLOBAL, INCREMENTAL;

        /****************************************************************
         * Parses a consistency-check mode.
         * @param value submitted mode value
         * @return parsed mode, defaulting to GLOBAL
         */
        public static ConsistencyCheckMode fromString(String value) {
            
            if ("INCREMENTAL".equalsIgnoreCase(value)) return INCREMENTAL;
            return GLOBAL;
        }
    }

    private final Map<String, Map<String, Object>> checkedKBs = new ConcurrentHashMap<>();
    private final Map<String, String> consistencyCheckQueue = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(ConsistencyCheckManager.class.getName());

    /****************************************************************
     * Creates a fixed-size consistency-check executor.
     */
    public ConsistencyCheckManager() {

        super(3, 3, 50000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10));
    }

    /****************************************************************
     * Returns the timestamp of the last completed consistency check.
     * @param kbName KB name
     * @return timestamp of the last completed check, or null
     */
    public Timestamp lastCCheck(String kbName) {

        if (!checkedKBs.containsKey(kbName))
            return null;
        Map<String, Object> value = checkedKBs.get(kbName);
        if (value == null)
            return null;

        return (Timestamp) value.get("timestamp");
    }

    /****************************************************************
     * Returns full or partial consistency-check XML results.
     * @param kbName KB name
     * @return XML result text, or null if no result file exists
     */
    public String ccheckResults(String kbName) {

        logger.entering("ConsistencyCheckManager", "ccheckResults", "kbName = " + kbName);
        String filename = null;
        boolean ongoing = consistencyCheckQueue.containsKey(kbName);
        if (ongoing) filename = consistencyCheckQueue.get(kbName);
        else if (checkedKBs.containsKey(kbName)) {
            Map<String, Object> value = checkedKBs.get(kbName);
            if (value != null) filename = (String) value.get("filename");
        }
        if (filename == null) {
            logger.exiting("ConsistencyCheckManager", "ccheckResults", null);
            return null;
        }
        StringBuilder result = new StringBuilder();
        try (Reader fr = new FileReader(filename);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) result.append(line).append("\n");
            if (ongoing && result.length() > 0 && !result.toString().contains("</ConsistencyCheck>")) result.append("  </entries>\n</ConsistencyCheck>\n");
            logger.exiting("ConsistencyCheckManager", "ccheckResults", result.toString());
            return result.toString();
        }
        catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
        logger.exiting("ConsistencyCheckManager", "ccheckResults", null);
        return null;
    }

    /****************************************************************
     * Returns the current consistency-check status for a KB.
     * @param kbName KB name
     * @return consistency-check status
     */
    public ConsistencyCheckStatus ccheckStatus(String kbName) {

        if (consistencyCheckQueue.containsKey(kbName)) return ConsistencyCheckStatus.ONGOING;
        if (checkedKBs.containsKey(kbName)) return ConsistencyCheckStatus.DONE;
        return ConsistencyCheckStatus.NOCCHECK;
    }

    /****************************************************************
     * Queues a consistency check.
     * @param kb KB to check
     * @param userSessionId HTTP session id for isolated prover files
     * @param proverType prover selected by the UI
     * @param language TPTP-family language: FOF, TFF, or THF
     * @param vampireMode Vampire execution mode
     * @param closedWorldAssumption whether to use CWA
     * @param modusPonens whether to use modus ponens
     * @param dropOnePremise whether to drop one-premise formulas
     * @param holUseModals whether to use modal HOL translation
     * @param timeout timeout in seconds
     * @param maxAnswers maximum answers
     * @return queue status
     */
    public ConsistencyCheckStatus performConsistencyCheck(KB kb,
                                                          String userSessionId,
                                                          String proverType,
                                                          String language,
                                                          String vampireMode,
                                                          boolean closedWorldAssumption,
                                                          boolean modusPonens,
                                                          boolean dropOnePremise,
                                                          boolean holUseModals,
                                                          int timeout,
                                                          int maxAnswers,
                                                          ConsistencyCheckMode checkMode) {

        if (checkMode == null) checkMode = ConsistencyCheckMode.GLOBAL;
        String filename = "CCHECK_" + checkMode + "_" + kb.name;
        if (KBmanager.configuration.getKbDir() != null) filename = KBmanager.configuration.getKbDir() + File.separator + filename;
        if (consistencyCheckQueue.putIfAbsent(kb.name, filename) != null) {
            logger.log(Level.INFO, "KB {0} has been rejected for consistency check because it is already being checked.", kb.name);
            return ConsistencyCheckStatus.ONGOING;
        }
        if (checkMode == null) checkMode = ConsistencyCheckMode.GLOBAL;
        try {
            ConsistencyCheck consistencyCheck = new ConsistencyCheck(
                kb,
                filename,
                userSessionId,
                proverType,
                language,
                vampireMode,
                closedWorldAssumption,
                modusPonens,
                dropOnePremise,
                holUseModals,
                timeout,
                maxAnswers,
                checkMode);
            checkedKBs.remove(kb.name);
            super.execute(consistencyCheck);
            logger.log(Level.INFO, "KB {0} has been added to the consistency-check queue.", kb.name);
            return ConsistencyCheckStatus.QUEUED;
        }
        catch (RejectedExecutionException e) {
            consistencyCheckQueue.remove(kb.name, filename);
            logger.warning(e.getMessage());
            return ConsistencyCheckStatus.ERROR;
        }
        catch (Exception e) {
            consistencyCheckQueue.remove(kb.name, filename);
            logger.warning(e.getMessage());
            return ConsistencyCheckStatus.ERROR;
        }
    }

    /****************************************************************
     * Moves a completed consistency check from the running queue to checkedKBs.
     * @param r completed runnable
     * @param t uncaught throwable, if any
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {

        try {
            if (r instanceof ConsistencyCheck) {
                ConsistencyCheck consistencyCheck = (ConsistencyCheck) r;
                String kbName = consistencyCheck.kbName;

                Map<String, Object> value = new HashMap<>();
                value.put("timestamp", new Timestamp(System.currentTimeMillis()));
                value.put("filename", consistencyCheckQueue.get(kbName));

                if (t != null)
                    value.put("error", t.getMessage());

                checkedKBs.put(kbName, value);
                consistencyCheckQueue.remove(kbName);
            }
        }
        finally {
            super.afterExecute(r, t);
        }
    }
}