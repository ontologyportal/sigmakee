package com.articulate.sigma.nlg;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import edu.stanford.nlp.ling.CoreLabel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities and variables used by LanguageFormatter and other NLG classes.
 */
public class NLGUtils implements Serializable {

    private static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    private static final String PHRASES_FILENAME = "Translations/language.txt";
    private static NLGUtils nlg = null;
    private Map<String, Map<String, String>> keywordMap;
    // a list of format parameters or words and the sentence words they match with
    public static Map<String, CoreLabel> outputMap = new HashMap<>();
    public static boolean debug = false;

    /** *************************************************************
     */
    public static void init(String kbDir) {

        System.out.println("NLGUtils.init(): ");
        if (KBmanager.getMgr().getPref("loadLexicons").equals("false"))
            return;
        System.out.println("NLGUtils.init(): initializing with " + kbDir);
        nlg = new NLGUtils();
        NLGUtils.readKeywordMap(kbDir);
    }

    /** ***************************************************************
     */
    private static final ThreadLocal<Kryo> kryoLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); //No need to pre-register the class
        kryo.setReferences(true);
        return kryo;
    });

    /** ***************************************************************
     */
    public static void encoder(Object object) {

        String kbDir = SIGMA_HOME + File.separator + "KBs";
        Path path = Paths.get(kbDir, "NLGUtils.ser");
        try (Output output = new Output(Files.newOutputStream(path))) {
            kryoLocal.get().writeObject(output, object);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static <T> T decoder() {

        NLGUtils ob = null;
        String kbDir = SIGMA_HOME + File.separator + "KBs";
        Path path = Paths.get(kbDir, "NLGUtils.ser");
        try (Input input = new Input(Files.newInputStream(path))) {
            ob = kryoLocal.get().readObject(input,NLGUtils.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return (T) ob;
    }

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedExists() {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        File serfile = new File(kbDir + File.separator + "NLGUtils.ser");
        System.out.println("NLGUtils.serializedExists(): " + serfile.exists());
        return serfile.exists();
    }

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedOld() {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String phrasesFilename = kbDir + File.separator + PHRASES_FILENAME;
        File phrasesFile = new File(phrasesFilename);
        if (!phrasesFile.exists()) {
            System.out.println("NLGUtils.serializeOld(): Cannot read " + phrasesFilename);
        }
        Date configDate = new Date(phrasesFile.lastModified());
        File serfile = new File(kbDir + File.separator + "NLGUtils.ser");
        Date saveDate = new Date(serfile.lastModified());
        System.out.println("NLGUtils.serializedOld(): " + serfile.getName() + " save date: " + saveDate.toString());
        return saveDate.compareTo(configDate) < 0;
    }

    /** ***************************************************************
     *  Load the most recently save serialized version.
     */
    public static void loadSerialized() {

        nlg = null;
        try {
            // Reading the object from a file
            //String kbDir = KBmanager.getMgr().getPref("kbDir");
            //FileInputStream file = new FileInputStream(kbDir + File.separator + "NLGUtils.ser");
            //ObjectInputStream in = new ObjectInputStream(file);
            // Method for deserialization of object
            nlg = decoder();
            //in.close();
            //file.close();
            System.out.println("NLGUtils.loadSerialized(): NLGUtils has been deserialized ");
        }
        catch (Exception ex) {
            System.err.println("Error in NLGUtils.loadSerialized(): IOException is caught");
            ex.printStackTrace();
            nlg = null;
        }
    }

    /** ***************************************************************
     *  save serialized version.
     * @deprecated as of OCT 2024, use {@link #encoder(java.lang.Object)} instead
     */
    @Deprecated(since = "OCT 2024", forRemoval = true)
    public static void serialize() {

        try {
            // Reading the object from a file
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            try (OutputStream file = new FileOutputStream(kbDir + File.separator + "NLGUtils.ser");
                ObjectOutputStream out = new ObjectOutputStream(file)) {
                System.out.println("NLGUtils.serialize(): nlg size " + getKeywordMap().keySet().size());
                // Method for deserialization of object
                out.writeObject(nlg);
            }
            System.out.println("NLGUtils.serialize(): NLGUtils has been serialized: " + nlg);
        }
        catch (IOException ex) {
            System.err.println("Error in NLGUtils.serialize(): IOException is caught");
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     */
    static String prettyPrint(String term) {

        if (term.endsWith("Fn"))
            term = term.substring(0,term.length()-2);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < term.length(); i++) {
            if (Character.isLowerCase(term.charAt(i)) || !Character.isLetter(term.charAt(i)))
                result.append(term.charAt(i));
            else {
                if (i + 1 < term.length() && Character.isUpperCase(term.charAt(i+1)))
                    result.append(term.charAt(i));
                else {
                    if (i != 0)
                        result.append(" ");
                    result.append(Character.toLowerCase(term.charAt(i)));
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     * Resolve the "format specifiers" in the given printf type of statement.
     * @param template
     * @param href
     * @return
     */
    public static String resolveFormatSpecifiers(String template, String href) {

        String anchorStart = ("<a href=\"" + href + "&term=");

        StringBuilder sb = new StringBuilder(template);
        int sblen = sb.length();
        String titok = "&%";
        int titoklen = titok.length();
        String ditok = "$\"";
        int ditoklen = ditok.length();
        String dktok = "\"";
        int dktoklen = dktok.length();
        int prevti = -1;
        int ti = 0;
        int tj;
        int di;
        int dj;
        int dk;
        int dl;
//        int digit;
        String termName, displayName;
        StringBuilder rsb;
        int rsblen;
        // The indexed positions: &%termNameString$"termDisplayString"  ti tj   di dj  dk dl
        while (((ti = sb.indexOf(titok, ti)) != -1) && (prevti != ti)) {
            prevti = ti;
            tj = (ti + titoklen);
            if (tj >= sblen)
                break;
            di = sb.indexOf(ditok, tj);
            if (di == -1)
                break;
            termName = sb.substring(tj, di);
            dj = (di + ditoklen);
            if (dj >= sblen)
                break;
            dk = sb.indexOf(dktok, dj);
            if (dk == -1)
                break;

            displayName = sb.substring(dj, dk);
            if (StringUtil.emptyString(displayName))
                displayName = termName;
            rsb = new StringBuilder();
            rsb.append(anchorStart);
            rsb.append(termName);
            rsb.append("\">");
            rsb.append(displayName);
            rsb.append("</a>");
            dl = (dk + dktoklen);
            if (dl > sblen)
                break;
            rsblen = rsb.length();
            if (dk != -1 && (dk+1) < sb.toString().length() && Character.isDigit(sb.toString().charAt(dk+1)))
                sb = sb.replace(ti, dl+1, rsb.toString()); // replace the argument number
            else
                sb = sb.replace(ti, dl, rsb.toString());
            sblen = sb.length();
            ti = (ti + rsblen);
            if (ti >= sblen)
                break;
            tj = -1;
            di = -1;
            dj = -1;
            dk = -1;
            dl = -1;
        }
        return sb.toString();
    }

    /** ***************************************************************
     * Format a list of variables which are not enclosed by parens.
     * Formatting includes inserting the appropriate separator between the elements (usually a comma), as well as
     * inserting the conjunction ("and" or its equivalent in another language) if the conjunction doesn't already exist.
     * @param strseq
     *  the list of variables
     * @param language
     *  the target language (used for the conjunction "and")
     * @return
     *  the formatted string
     */
    public static String formatList(String strseq, String language) {

        if (language == null || language.isEmpty())    {
            throw new IllegalArgumentException("Parameter language is empty or null.");
        }

        StringBuilder result = new StringBuilder();
        String comma = NLGUtils.getKeyword(",", language);
        String space = " ";
        String[] arr = strseq.split(space);
        int lastIdx = (arr.length - 1);
        String val;
        for (int i = 0; i < arr.length; i++) {
            val = arr[i];
            if (i > 0) {
                if (val.equals(NLGUtils.getKeyword("and", language))) {
                    // Make behavior for lists that include "and" the same as for those that don't.
                    continue;
                }
                if (i == lastIdx) {
                    result.append(space);
                    result.append(NLGUtils.getKeyword("and", language));
                }
                else {
                    result.append(comma);
                }
                result.append(space);
            }
            result.append(val);
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    static boolean logicalOperator(String word) {

        String logops = "if,then,=>,and,or,<=>,not,forall,exists,holds";
        return logops.contains(word);
    }

    /** ***************************************************************
     *  Read a set of standard words and phrases in several languages.
     *  Each phrase must appear on a new line with alternatives separated by '|'.
     *  The first entry should be a set of two letter language identifiers.
     *
     *  @return a HashMap of HashMaps where the first HashMap has a key of the
     *  English phrase, and the interior HashMap has a key of the two letter
     *  language identifier.
     */
    public static void readKeywordMap(String dir) {

        if (dir == null || dir.isEmpty())    {
            throw new IllegalArgumentException("Parameter dir is null or empty.");
        }
        File dirFile = new File(dir);
        if (!dirFile.exists())  {
            throw new IllegalArgumentException("Parameter dir points to non-existent path: " + dir);
        }
        System.out.println("NLGUtils.readKeywordMap():");
        nlg = null;
        if (serializedExists() && !serializedOld())
            loadSerialized();
        if (nlg != null)
            return;
        System.out.println("INFO in NLGUtils.readKeywordMap(" + dir + "/" +
                PHRASES_FILENAME + ")");

        if (getKeywordMap() == null)
            setKeywordMap(new HashMap<>());
        int lc = 0;
        File phrasesFile;

        if (getKeywordMap().isEmpty()) {
            System.out.println("Filling keywordMap");

            phrasesFile = new File(dirFile, PHRASES_FILENAME);
            if (!phrasesFile.canRead()) {
                try {
                    throw new Exception("Cannot read \"" + phrasesFile.getCanonicalPath() + "\"");
                } catch (Exception ex) {
                    Logger.getLogger(NLGUtils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(phrasesFile), "UTF-8"))) {
                Map<String, String> phrasesByLang;
                List<String> phraseList;
                List<String> languageKeys = null;
                String delim = "|";
                String key;
                String line;
                int plLen;
                while ((line = br.readLine()) != null) {
                    lc++;
                    line = line.trim();
                    if (line.startsWith(";") || line.equals("")) {
                        continue;
                    }
                    if (line.contains(delim)) {
                        if (line.startsWith("EnglishLanguage|")) // The language key line.
                        {
                            languageKeys = Arrays.asList(line.split("\\" + delim));
                        } else {
                            phraseList = Arrays.asList(line.split("\\" + delim));
                            phrasesByLang = new HashMap<>();
                            key = phraseList.get(0);
                            plLen = phraseList.size();
                            for (int i = 0; i < plLen; i++) {
                                phrasesByLang.put(languageKeys.get(i), phraseList.get(i));
                            }
                            getKeywordMap().put(key.intern(), phrasesByLang);
                        }
                    } else {
                        System.out.println("WARNING in NLGUtils.readKeywordMap(): "
                                + "Unrecognized line");
                        System.out.println(lc + ": \"" + line + "\"");
                    }
                }
            } catch (Exception ex) {
                System.err.println("ERROR loading " + PHRASES_FILENAME + " at line " + lc + ":");
                System.err.println(ex.getMessage());
                ex.printStackTrace();
            } finally {
                System.out.println("EXIT NLGUtils.readKeywordMap(" + dir + ") with size " + getKeywordMap().keySet().size());
            }
//        serialize(); // old method. Replace w/ Kryo serialization (tdn) 10/15/24
            encoder(nlg);
        }
    }

    /** **************************************************************
     * Generate a linguistic article appropriate to how many times in a
     * paraphrase a particular type has already occurred.
     * @param occurrence is the number of times a variables of a
     *                   given type have appeared
     * @param count is the number of times a given variable has
     *              appeared
     */
    public static String getArticle(String s, int count, int occurrence, String language) {

        String ordinal = "";
        switch (occurrence) {
        case 3: ordinal = NLGUtils.getKeyword("third", language); break;
        case 4: ordinal = NLGUtils.getKeyword("fourth", language); break;
        case 5: ordinal = NLGUtils.getKeyword("fifth", language); break;
        case 6: ordinal = NLGUtils.getKeyword("sixth", language); break;
        case 7: ordinal = NLGUtils.getKeyword("seventh", language); break;
        case 8: ordinal = NLGUtils.getKeyword("eighth", language); break;
        case 9: ordinal = NLGUtils.getKeyword("ninth", language); break;
        case 10: ordinal = NLGUtils.getKeyword("tenth", language); break;
        case 11: ordinal = NLGUtils.getKeyword("eleventh", language); break;
        case 12: ordinal = NLGUtils.getKeyword("twelfth", language); break;
        }
        boolean isArabic = (language.matches(".*(?i)arabic.*")
                || language.equalsIgnoreCase("ar"));
        if (count == 1 && occurrence == 2)
            return NLGUtils.getKeyword("another", language);
        if (count > 1) {
            if (occurrence == 1) {
                if (isArabic)
                    return ordinal;
                else
                    return (NLGUtils.getKeyword("the", language));
            }
            else if (occurrence > 2) {
                if (isArabic)
                    return ordinal;
                else
                    return (NLGUtils.getKeyword("the", language) + " " + ordinal);
            }
            else {
                if (isArabic)
                    return (NLGUtils.getKeyword("the", language) + " " + NLGUtils.getKeyword("other", language));
                else
                    return (NLGUtils.getKeyword("the", language) + " " + NLGUtils.getKeyword("other", language));
            }
        }
        // count = 1 (first occurrence of a type)
        if (language.equalsIgnoreCase("EnglishLanguage")) {
            String vowels = "AEIOUaeiou";
            if ((vowels.indexOf(s.charAt(0)) != -1) && (occurrence == 1))
                return "an";
            else
                return "a " + ordinal;
        }
        else if (isArabic) {
            String defArt = NLGUtils.getKeyword("the", language);
            if (ordinal.startsWith(defArt)) {
                // remove the definite article
                ordinal = ordinal.substring(defArt.length());
                // remove shadda
                ordinal = ordinal.replaceFirst("\\&\\#\\x651\\;","");
            }
            return ordinal;
        }
        else
            return ordinal;
    }

    /** **************************************************************
     * Collect all the variables occurring in a formula in order.  Return
     * an ArrayList of Strings.
     */
    static List<String> collectOrderedVariables(String form) {

        boolean inString = false;
        boolean inVar = false;
        String var = "";
        List<String> result = new ArrayList<>();
        for (int i = 0; i < form.length(); i++) {
            char ch = form.charAt(i);
            switch (ch) {
                case '"': inString = !inString; break;
                case '?': if (!inString) inVar = true; break;
                case '@': if (!inString) inVar = true; break;
            }
            if (inVar && !Character.isLetterOrDigit(ch) && ch != '?' && ch != '@') {
                if (!result.contains(var))
                    result.add(var);
                inVar = false;
                var = "";
            }
            if (inVar)
                var = var + ch;
        }

        // Add input-final var if it exists.
        if (! var.trim().isEmpty()) {
            result.add(var);
        }
        return result;
    }

    /** **************************************************************
     */
    public static Map<String, Map<String, String>> getKeywordMap() {

        if (NLGUtils.nlg == null || NLGUtils.nlg.keywordMap == null) {
            return null;
        }
        return NLGUtils.nlg.keywordMap;
    }

    /** **************************************************************
     */
    public static void setKeywordMap(Map<String, Map<String, String>> themap) {

        if (NLGUtils.nlg == null)
            NLGUtils.nlg = new NLGUtils();
        NLGUtils.nlg.keywordMap = themap;
    }

    /** ***************************************************************
     */
    public static String getKeyword(String englishWord, String language) {

        String ans = "";
        if (getKeywordMap() == null) {
            if (debug) System.out.println("Error in NLGUtils.getKeyword(): keyword map is null");
            return ans;
        }
        Map<String,String> hm = getKeywordMap().get(englishWord);
        if (hm != null) {
            String tmp = hm.get(language);
            if (tmp != null)
                ans = tmp;
        }
        return ans;
    }

    /** **************************************************************
     * Hyperlink terms in a natural language format string.  This assumes that
     * terms to be hyperlinked are in the form &%termName$termString , where
     * termName is the name of the term to be browsed in the knowledge base and
     * termString is the text that should be displayed hyperlinked.
     *
     * @param href the anchor string up to the term= parameter, which this method
     *               will fill in.
     * @param stmt the KIF statement that will be passed to paraphraseStatement for formatting.
     * @param phraseMap the set of NL formatting statements that will be passed to paraphraseStatement.
     * @param termMap the set of NL statements for terms that will be passed to paraphraseStatement.
     * @param language the natural language in which the paraphrase should be generated.
     */
    public static String htmlParaphrase(String href, String stmt, Map<String,String> phraseMap,
                                        Map<String,String> termMap, KB kb, String language) {

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, phraseMap, termMap, kb, language);
        outputMap = LanguageFormatter.outputMap;
        return languageFormatter.htmlParaphrase(href);
    }

    /** ***************************************************************
     * This method expands all "star" (asterisk) directives in the input
     * format string, and returns a new format string with individually
     * numbered argument pointers.
     *
     * @param f The Formula being paraphrased.
     *
     * @param strFormat The format string that contains the patterns and
     * directives for paraphrasing f.
     *
     * @param lang A two-character string indicating the language into
     * which f should be paraphrased.
     *
     * @return A format string with all relevant argument pointers
     * expanded.
     */
    public static String expandStar(Formula f, String strFormat, String lang) {

        String result = strFormat;
        List<String> problems = new ArrayList<>();
        try {
            int flen = f.listLength();
            if (StringUtil.isNonEmptyString(strFormat) && (flen > 1)) {
                int p1 = 0;
                int p2 = strFormat.indexOf("%*");
                if (p2 != -1) {
                    int slen = strFormat.length();
                    String lb;
                    String rb;
                    int lbi;
                    int rbi;
                    String ss;
                    String range;
                    String[] rangeArr;
                    String[] rangeArr2;
                    String lowStr;
                    String highStr;
                    int low;
                    int high;
                    String delim;
                    boolean isRange;
                    boolean[] argsToPrint = new boolean[ flen ];
                    int nArgsSet;
                    String AND;
                    int nAdded;
                    boolean addAll;
                    int nToAdd;
                    StringBuilder sb = new StringBuilder();
                    while ((p1 < slen) && (p2 >= 0) && (p2 < slen)) {
                        sb.append(strFormat.substring(p1, p2));
                        p1 = (p2 + 2);
                        for (int k = 0 ; k < argsToPrint.length ; k++) {
                            argsToPrint[k] = false;
                        }
                        lowStr = null;
                        highStr = null;
                        low = -1;
                        high = -1;
                        delim = " ";
                        nArgsSet = 0;
                        lb = null;
                        lbi = p1;
                        if (lbi < slen) { lb = strFormat.substring(lbi, (lbi + 1)); }
                        while ((lb != null) && (lb.equals("{") || lb.equals("["))) {
                            rb = "]";
                            if (lb.equals("{")) { rb = "}"; }
                            rbi = strFormat.indexOf(rb, lbi);
                            if (rbi == -1) {
                                problems.add("Error in format \"" + strFormat + "\": missing \"" + rb + "\"");
                                break;
                            }
                            p1 = (rbi + 1);
                            ss = strFormat.substring((lbi + 1), rbi);
                            if (lb.equals("{")) {
                                range = ss.trim();
                                rangeArr = range.split(",");
                                for (String aRangeArr : rangeArr) {
                                    if (StringUtil.isNonEmptyString(aRangeArr)) {
                                        isRange = (aRangeArr.contains("-"));
                                        rangeArr2 = aRangeArr.split("-");
                                        lowStr = rangeArr2[0].trim();
                                        try {
                                            low = Integer.parseInt(lowStr);
                                        } catch (NumberFormatException e1) {
                                            problems.add("Error in format \"" + strFormat + "\": bad value in \"" + ss + "\"");
                                            low = 1;
                                        }
                                        high = low;
                                        if (isRange) {
                                            if (rangeArr2.length == 2) {
                                                highStr = rangeArr2[1].trim();
                                                try {
                                                    high = Integer.parseInt(highStr);
                                                } catch (NumberFormatException e2) {
                                                    problems.add("Error in format \"" + strFormat + "\": bad value in \"" + ss + "\"");
                                                    high = (flen - 1);
                                                }
                                            } else
                                                high = (flen - 1);
                                        }
                                        for (int j = low; (j <= high) && (j < argsToPrint.length); j++) {
                                            argsToPrint[j] = true;
                                            nArgsSet++;
                                        }
                                    }
                                }
                            }
                            else
                                delim = ss;
                            lb = null;
                            lbi = p1;
                            if (lbi < slen) { lb = strFormat.substring(lbi, (lbi + 1)); }
                        }
                        AND = NLGUtils.getKeyword("and", lang);
                        if (StringUtil.emptyString(AND))
                            AND = "+";
                        nAdded = 0;
                        addAll = (nArgsSet == 0);
                        nToAdd = (addAll ? (argsToPrint.length - 1) : nArgsSet);
                        for (int i = 1 ; i < argsToPrint.length ; i++) {
                            if (addAll || argsToPrint[i]) {
                                if (nAdded >= 1) {
                                    if (nToAdd == 2) {
                                        sb.append(" ");
                                        sb.append(AND);
                                        sb.append(" ");
                                    }
                                    else {
                                        sb.append(delim);
                                        sb.append(" ");
                                    }
                                    if ((nToAdd > 2) && ((nAdded + 1) == nToAdd)) {
                                        sb.append(AND);
                                        sb.append(" ");
                                    }
                                }
                                sb.append("%").append(i);
                                nAdded++;
                            }
                        }
                        if (p1 < slen) {
                            p2 = strFormat.indexOf("%*", p1);
                            if (p2 == -1) {
                                sb.append(strFormat.substring(p1, slen));
                                break;
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        result = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (! problems.isEmpty()) {
            String errStr = KBmanager.getMgr().getError();
            String str;
            if (errStr == null) { errStr = ""; }
            Iterator<String> it = problems.iterator();
            while (it.hasNext()) {
                str = it.next();
                System.err.println("Error in NLGUtils.expandStar(): ");
                System.out.println("  " + str);
                errStr += ("\n<br/>" + str + "\n<br/>");
            }
            KBmanager.getMgr().setError(errStr);
        }
        return result;
    }

    /** **************************************************************
     * Capitalizes the first visible char of htmlParaphrase, if
     * possible, and adds the full stop symbol for language at a
     * workable place near the end of htmlParaphrase if addFullStop is
     * true.
     *
     * @param htmlParaphrase Any String, but assumed to be a Formula
     * paraphrase with HTML markup
     *
     * @param addFullStop If true, this method will try to add a full
     * stop symbol to the result String.
     *
     * @param language The language of the paraphrase String.
     *
     * @return String
     */
    public static String upcaseFirstVisibleChar(String htmlParaphrase,
                                                boolean addFullStop,
                                                String language) {

        //System.out.println("NLGUtils.upcaseFirstVisibleChar(): " + nlg);
        String ans = htmlParaphrase;
        try {
            if (StringUtil.isNonEmptyString(htmlParaphrase)) {
                StringBuilder sb = new StringBuilder(htmlParaphrase.trim());
                String termKey = "term=";
                int sbLen = sb.length();
                if (sbLen > 0) {
                    int codePoint;
                    int termCodePoint;
                    int termPos;
                    String uc;
                    int i = 0;
                    String fs;
                    String ss;
                    while ((i > -1) && (i < sbLen)) {
                        // System.out.println("x");
                        codePoint = Character.codePointAt(sb, i);
                        if (Character.isLetter(codePoint)) {
                            if (Character.isLowerCase(codePoint)) {
                                boolean isKifTermCapitalized = true;
                                termPos = sb.indexOf(termKey);
                                if (termPos > -1) {
                                    termPos = (termPos + termKey.length());
                                    if (termPos < i) {
                                        termCodePoint = Character.codePointAt(sb, termPos);
                                        isKifTermCapitalized = Character.isUpperCase(termCodePoint);
                                    }
                                }
                                if (isKifTermCapitalized) {
                                    uc = sb.substring(i, i + 1).toUpperCase();
                                    sb = sb.replace(i, i + 1, uc);
                                }
                            }
                            break;
                        }
                        i = sb.indexOf(">", i);
                        if (i > -1) {
                            i++;
                            while ((i < sbLen) && sb.substring(i, i + 1).matches("\\s"))
                                i++;
                        }
                    }
                    if (addFullStop) {
                        fs = NLGUtils.getKeyword(".", language);
                        if (StringUtil.isNonEmptyString(fs)) {
                            sbLen = sb.length();
                            i = (sbLen - 1);
                            while ((i > 0) && (i < sbLen)) {
                                // System.out.println("m");
                                ss = sb.substring(i, i + 1);
                                if (ss.matches("\\s")) {
                                    i--;
                                    continue;
                                }
                                if (ss.matches("[\\w;]")) {
                                    sb = sb.insert(i + 1, fs);
                                    break;
                                }
                                while ((i > 0) && !ss.equals("<")) {
                                    i--;
                                    ss = sb.substring(i, i + 1);
                                }
                                i--;
                            }
                        }
                    }
                    ans = sb.toString();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /**************************************************************************************************************
     * Return true if the given list includes "Process", or if one of its elements is a subclass of Process.
     */
    public static boolean containsProcess(Collection<String> vals, KB kb) {

        for (String val : vals)  {
            if (val.equals("Process") || kb.isSubclass(val, "Process"))  {
                return true;
            }
        }
        return false;
    }
}
