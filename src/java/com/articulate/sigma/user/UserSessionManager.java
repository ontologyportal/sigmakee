/** 
This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.
*/

package com.articulate.sigma.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class UserSessionManager {

    public enum FormalLanguage { 
        SUOKIF("SUO-KIF"),TPTP("TPTP"),OWL("OWL"),TRADITIONAL_LOGIC("Traditional Logic");
        private final String sigmaName;

        FormalLanguage(String sigmaName) {this.sigmaName = sigmaName;}
        public String getSigmaName() {return sigmaName;}

        public static FormalLanguage fromString(String value) {
            if (value == null || value.trim().isEmpty()) return SUOKIF;
            String normalized = value.trim()
                    .replace("-", "")
                    .replace("_", "")
                    .replace(" ", "")
                    .toUpperCase();
            if ("SUOKIF".equals(normalized)) return SUOKIF;
            if ("TPTP".equals(normalized)) return TPTP;
            if ("OWL".equals(normalized)) return OWL;
            if ("TRADITIONALLOGIC".equals(normalized)) return TRADITIONAL_LOGIC;
            return SUOKIF;
        }
    }

    public enum HumanLanguage {
        ENGLISH("EnglishLanguage"),
        CHINESE("ChineseLanguage"),
        CHINESE_TRADITIONAL("ChineseTraditionalLanguage"),
        GERMAN("GermanLanguage"),
        JAPANESE("JapaneseLanguage"),
        SPANISH("SpanishLanguage"),
        SWEDISH("SwedishLanguage");

        private final String sigmaName;

        HumanLanguage(String sigmaName) {this.sigmaName = sigmaName;}

        public String getSigmaName() {return sigmaName;}

        public static HumanLanguage fromString(String value) {
            if (value == null || value.trim().isEmpty()) return ENGLISH;
            String normalized = value.trim()
                    .replace("-", "")
                    .replace("_", "")
                    .replace(" ", "")
                    .toUpperCase();
            if ("ENGLISH".equals(normalized) || "ENGLISHLANGUAGE".equals(normalized)) return ENGLISH;
            if ("CHINESE".equals(normalized) || "CHINESELANGUAGE".equals(normalized)) return CHINESE;
            if ("CHINESETRADITIONAL".equals(normalized) || "CHINESETRADITIONALLANGUAGE".equals(normalized)) return CHINESE_TRADITIONAL;
            if ("GERMAN".equals(normalized) || "GERMANLANGUAGE".equals(normalized)) return GERMAN;
            if ("JAPANESE".equals(normalized) || "JAPANESELANGUAGE".equals(normalized)) return JAPANESE;
            if ("SPANISH".equals(normalized) || "SPANISHLANGUAGE".equals(normalized)) return SPANISH;
            if ("SWEDISH".equals(normalized) || "SWEDISHLANGUAGE".equals(normalized)) return SWEDISH;
            return ENGLISH;
        }
    }

    private FormalLanguage formalLanguage = FormalLanguage.SUOKIF;
    private HumanLanguage humanLanguage = HumanLanguage.ENGLISH;

    private static final String SESSION_KEY = UserSessionManager.class.getName();

    /*****************************************************************
     * Gets the session preferences object for the current user session.
     * @param request the servlet request
     * @return the session preferences for this user
     */
    public static UserSessionManager get(HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        UserSessionManager preferences = (UserSessionManager) session.getAttribute(SESSION_KEY);
        if (preferences == null) {
            preferences = new UserSessionManager();
            session.setAttribute(SESSION_KEY, preferences);
        }
        return preferences;
    }

    /*****************************************************************
     * Gets the selected formal language.
     * @return the selected formal language
     */
    public FormalLanguage getFormalLanguage() {return formalLanguage;}

    /*****************************************************************
     * Sets the selected formal language.
     * @param formalLanguage the selected formal language
     */
    public void setFormalLanguage(FormalLanguage formalLanguage) {

        if (formalLanguage == null) throw new IllegalArgumentException("formalLanguage cannot be null");
        this.formalLanguage = formalLanguage;
    }

    /*****************************************************************
     * Gets the selected formal language.
     * @return the selected formal language
     */
    public HumanLanguage getHumanLanguage() {return humanLanguage;}

    /*****************************************************************
     * Sets the selected formal language.
     * @param formalLanguage the selected formal language
     */
    public void setHumanLanguage(HumanLanguage humanLanguage) {

        if (humanLanguage == null) throw new IllegalArgumentException("formalLanguage cannot be null");
        this.humanLanguage = humanLanguage;
    }
}