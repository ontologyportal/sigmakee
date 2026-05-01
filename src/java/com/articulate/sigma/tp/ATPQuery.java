package com.articulate.sigma.tp;

import com.articulate.sigma.KB;
import com.articulate.sigma.tp.Vampire;


/**
 * @author Shaun Rose
 * ATPQuery is used by TheoremProverController with the following attributes:
 * (kb, userSessionId, query, testFilePath, proverType, language, vampireMode, closedWorldAssumption, modusPonens dropOnePremise holUseModals, timeout, maxAnswers)
 */
public class ATPQuery {

    public enum RunSource {CUSTOM_QUERY, TEST_FILE}
    public enum TptpLanguage {FOF, TFF, THF}
    public enum ATPType{EPROVER, VAMPIRE, LEO}
    
    private KB kb;
    private String userSessionId;
    private String query;
    private String testFilePath;
    private RunSource runSource;
    private ATPType proverType;
    private TptpLanguage language;
    private Vampire.ModeType vampireMode;
    private boolean closedWorldAssumption;
    private boolean modusPonens;
    private boolean dropOnePremise;
    private boolean holUseModals;
    private int timeout;
    private int maxAnswers;

    /**
     * Construct an ATP query request containing the knowledge base, input source,
     * prover selection, language settings, and execution options needed to run
     * an inference query.
     *
     * @param kb the knowledge base to query against
     * @param userSessionId the current user's session id, used to isolate user assertions and prover/session state
     * @param query the query formula to submit, usually in SUO-KIF syntax for custom queries
     * @param testFilePath the path to a saved test file when {@code runSource} is {@code TEST_FILE}; may be null or empty for custom queries
     * @param runSource whether the query comes from direct user input or from a saved test file
     * @param proverType the theorem prover to use, such as EProver, Vampire, or LEO
     * @param language the target TPTP-family language to use, such as FOF, TFF, or THF
     * @param vampireMode the Vampire execution mode to use when {@code proverType} is {@code VAMPIRE}
     * @param closedWorldAssumption true to enable closed world assumption translation behavior; false otherwise
     * @param modusPonens true to use the Modus Ponens inference mode when supported; false otherwise
     * @param dropOnePremise true to drop one-premise formulas during Modus Ponens processing; false otherwise
     * @param holUseModals true to use modal-aware HOL/THF translation when applicable; false otherwise
     * @param timeout the query timeout in seconds
     * @param maxAnswers the maximum number of answers to request from the prover
     */
    public ATPQuery(KB kb, String userSessionId, String query, String testFilePath,
                    String runSource, String proverType, String language,
                    String vampireMode, boolean closedWorldAssumption,
                    boolean modusPonens, boolean dropOnePremise, boolean holUseModals,
                    int timeout, int maxAnswers) {
        
        this.kb = kb;
        this.userSessionId = userSessionId;
        this.query = query;
        this.testFilePath = testFilePath;
        this.proverType = ATPType.valueOf(proverType.toUpperCase());
        this.runSource = RunSource.valueOf(runSource.toUpperCase());
        this.language = TptpLanguage.valueOf(language.toUpperCase());
        this.vampireMode = Vampire.ModeType.valueOf(vampireMode);
        this.closedWorldAssumption = closedWorldAssumption;
        this.modusPonens = modusPonens;
        this.dropOnePremise = dropOnePremise;
        this.holUseModals = holUseModals;
        this.timeout = timeout;
        this.maxAnswers = maxAnswers;
    }

    public KB getKb() { return this.kb; }

    public String getUserSessionId() { return this.userSessionId; }

    public String getQuery() { return this.query; }

    public String getTestFilePath() { return this.testFilePath; }

    public RunSource getRunSource() { return this.runSource; }

    public ATPType getProverType() { return this.proverType; }

    public TptpLanguage getLanguage() { return this.language; }

    public Vampire.ModeType getVampireMode() { return this.vampireMode; }

    public boolean isClosedWorldAssumption() { return this.closedWorldAssumption; }

    public boolean isModusPonens() { return this.modusPonens; }

    public boolean isDropOnePremise() { return this.dropOnePremise; }

    public boolean isHolUseModals() { return this.holUseModals; }

    public int getTimeout() { return this.timeout; }

    public int getMaxAnswers() { return this.maxAnswers; }
}