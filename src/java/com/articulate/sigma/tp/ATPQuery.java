package com.articulate.sigma.tp;

import com.articulate.sigma.KB;
import com.articulate.sigma.tp.Vampire;

public class ATPQuery {

    public enum RunSource {CUSTOM_QUERY, TEST_FILE}
    public enum TptpLanguage {FOF, TFF, THF}
    private enum ATPType{EPROVER, VAMPIRE, LEO}
    
    private KB kb;
    private String userSessionId;
    private String query;
    private String testFilePath;
    private RunSource runSource;
    private TheoremProverController.ProverType proverType;
    private TptpLanguage language;
    private Vampire.ModeType vampireMode;
    private boolean closedWorldAssumption;
    private boolean modusPonens;
    private boolean dropOnePremise;
    private boolean holUseModals;
    private int timeout;
    private int maxAnswers;

    public ATPQuery (KB kb, String userSessionId, String query, String testFilePath, RunSource runSource, TheoremProverController.ProverType proverType, TranslationMode translationMode, TptpLanguage language, Vampire.ModeType vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) {
        
        this.kb = kb;
        this.userSessionId = userSessionId;
        this.query = query;
        this.testFilePath = testFilePath;
        this.runSource = runSource;
        this.proverType = proverType;
        this.language = language;
        this.vampireMode = vampireMode;
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

    public TranslationMode getTranslationMode() { return this.translationMode; }

    public TptpLanguage getLanguage() { return this.language; }

    public Vampire.ModeType getVampireMode() { return this.vampireMode; }

    public boolean isClosedWorldAssumption() { return this.closedWorldAssumption; }

    public boolean isModusPonens() { return this.modusPonens; }

    public boolean isDropOnePremise() { return this.dropOnePremise; }

    public boolean isHolUseModals() { return this.holUseModals; }

    public int getTimeout() { return this.timeout; }

    public int getMaxAnswers() { return this.maxAnswers; }
}