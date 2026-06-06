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
import java.io.Serializable;

public class UserSessionManager implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public static UserSessionManager resolve(HttpServletRequest request) {

        UserSessionManager manager = new UserSessionManager();
        HttpSession session = request.getSession(false);
        if (session != null && isLoggedIn(session)) {
            UserSessionManager sessionManager = (UserSessionManager) session.getAttribute(SESSION_KEY);
            if (sessionManager != null) manager = sessionManager.copy();
        }
        manager.updateFromRequest(request);
        return manager;
    }

    /*****************************************************************
     * Gets the session preferences object for the current user session.
     * If create is false and no session exists, returns a temporary default manager
     * without creating a new HttpSession.
     * @param request the servlet request
     * @param create whether to create a session if one does not exist
     * @return the session preferences for this user
     */
    public static UserSessionManager get(HttpServletRequest request, boolean create) {

        HttpSession session = request.getSession(create);
        if (session == null) return new UserSessionManager();
        UserSessionManager preferences = (UserSessionManager) session.getAttribute(SESSION_KEY);
        if (preferences == null) {
            preferences = new UserSessionManager();
            if (create) session.setAttribute(SESSION_KEY, preferences);
        }
        return preferences;
    }

    /*****************************************************************
     * Updates this session manager from request parameters, if present.
     * @param request the servlet request
     */
    public void updateFromRequest(HttpServletRequest request) {

        String flangParam = request.getParameter("flang");
        if (flangParam != null && !flangParam.trim().isEmpty()) setFormalLanguage(FormalLanguage.fromString(flangParam));
        String langParam = request.getParameter("lang");
        if (langParam != null && !langParam.trim().isEmpty()) setHumanLanguage(HumanLanguage.fromString(langParam));
    }

    private static boolean isLoggedIn(HttpSession session) {

        if (session == null) return false;
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");
        return username != null && !username.trim().isEmpty() && role != null && !"guest".equalsIgnoreCase(role);
    }
    
    public UserSessionManager copy() {

        UserSessionManager copy = new UserSessionManager();
        copy.setFormalLanguage(this.formalLanguage);
        copy.setHumanLanguage(this.humanLanguage);
        return copy;
    }

    public static void saveToLoggedInSession(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (!isLoggedIn(session)) return;
        UserSessionManager manager = (UserSessionManager) session.getAttribute(SESSION_KEY);
        if (manager == null) {
            manager = new UserSessionManager();
            session.setAttribute(SESSION_KEY, manager);
        }
        manager.updateFromRequest(request);
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
     * Sets the selected human language.
     * @param humanLanguage the selected human language
     */
    public void setHumanLanguage(HumanLanguage humanLanguage) {

        if (humanLanguage == null) throw new IllegalArgumentException("humanLanguage cannot be null");
        this.humanLanguage = humanLanguage;
    }
}