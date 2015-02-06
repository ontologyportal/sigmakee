package com.articulate.sigma;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.util.*;

/**
 * Class mocks KB and KBcache so that unit tests can be run independently and without long initialization.
 * Add processes and case roles to the static final lists as necessary.
 */
public class SigmaMockTestBase {
    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();
    private static Hashtable<String, String> oldWordNetSynSetTable;

    protected KB kbMock = new KBMock("dummyString");

    private static final ImmutableList<String> RECOGNIZED_PROCESSES = ImmutableList.of(
            "Drinking",
            "Driving",
            "Eating",
            "Giving",
            "Process",
            "Reading",
            "Seeing",
            "Transportation");

    private static final ImmutableList<String> RECOGNIZED_CASE_ROLES = ImmutableList.of(
            "agent",
            "benefactive",
            "destination",
            "experiencer",
            "goal",
            "patient");

    // JERRY: not sure if instance should be here, or not
    private static final ImmutableList<String> RECOGNIZED_VARIABLE_ARITY_RELATIONS = ImmutableList.of("");

    // Add an item to one of these two lists--RECOGNIZED_SUBSTANCES or RECOGNIZED_CORPUSCULAR_OBJECTS--if it is a "participant" in a process and
    // the NL output has it capitalized.
    // Add an item to RECOGNIZED_SUBSTANCES if it is a "participant" in a process and the NL output precedes it with an indefinite article (i.e. "a" or "an").
    private static final ImmutableList<String> RECOGNIZED_SUBSTANCES = ImmutableList.of(
            "Coffee",
            "Food",
            "Tea"
            );
    private static final ImmutableList<String> RECOGNIZED_CORPUSCULAR_OBJECTS = ImmutableList.of(
            "Book",
            "Human",
            "MedicalDoctor",
            "Automobile",
            "Taxi",
            "Telephone",
            "Truck");

    private static final Map<String, List> recognizedMap = Maps.newHashMap();


    private static final ArrayList<String> INSTANCE_SIGNATURES = Lists.newArrayList(
            "",
            "Entity",
            "SetOrClass");

    private static final ArrayList<String> AGENT_SIGNATURES = Lists.newArrayList(
            "",
            "Process",
            "Agent");

    private static final ArrayList<String> NAMES_SIGNATURES = Lists.newArrayList(
            "",
            "SymbolicString",
            "Entity");

    private static final HashMap<String, ArrayList<String>> signaturesMap = Maps.newHashMap();


    private static final ArrayList<Formula> LANG_FORMAT_MAP_COLS = Lists.newArrayList(
            new Formula("(format EnglishLanguage agent \"%2 is %n an &%agent of %1\")"),
            new Formula("(format EnglishLanguage attribute \"%2 is %n an &%attribute of %1\")"),
            new Formula("(format EnglishLanguage instrument \"%2 is %n an &%instrument for %1\")"),
            new Formula("(format EnglishLanguage member \"%1 is %n a &%member of %2\")"),
            new Formula("(format EnglishLanguage patient \"%2 is %n a &%patient of %1\")"),
            new Formula("(format EnglishLanguage subCollection \"%1 is %n a proper &%sub-collection of %2\")"),
            new Formula("(format EnglishLanguage subList \"%1 is %n a &%sublist of %2\")")
            );

//    private static final ArrayList<Formula> LANG_TERM_FORMAT_MAP_COLS = Lists.newArrayList(
//            new Formula("termFormat EnglishLanguage Agent \"agent\")"),
//            new Formula("termFormat EnglishLanguage Process \"process\"")
//            );


//    private static HashMap<String, ArrayList<String>> formulasMap = new HashMap<String, ArrayList<String>>();

    private static HashMap<String, String> termFormatMap = Maps.newHashMap();


    static  {
        recognizedMap.put("Process", RECOGNIZED_PROCESSES);
        recognizedMap.put("CaseRole", RECOGNIZED_CASE_ROLES);
        recognizedMap.put("VariableArityRelation", RECOGNIZED_VARIABLE_ARITY_RELATIONS);
        recognizedMap.put("Substance", RECOGNIZED_SUBSTANCES);
        recognizedMap.put("CorpuscularObject", RECOGNIZED_CORPUSCULAR_OBJECTS);

        List<String> entityList = Lists.newArrayList(RECOGNIZED_SUBSTANCES);
        entityList.addAll(RECOGNIZED_CORPUSCULAR_OBJECTS);
        recognizedMap.put("Entity", entityList);

        signaturesMap.put("instance", INSTANCE_SIGNATURES);
        signaturesMap.put("agent", AGENT_SIGNATURES);
        signaturesMap.put("names", NAMES_SIGNATURES);

//        formulasMap.putAll("format", LANG_FORMAT_MAP_COLS);
//        formulasMap.putAll("termFormat", LANG_TERM_FORMAT_MAP_COLS);

        termFormatMap.put("Process", "process");
        termFormatMap.put("Agent", "agent");
        termFormatMap.put("Entity", "entity");
    }

    /**
     * Mock of KB.
     */
    protected class KBMock extends KB     {
         public KBMock(String dummyStr) {
            super(dummyStr);
            kbCache = new KBcacheMock(this);
        }

        @Override
        public boolean isSubclass(String str1, String str2)    {
            List<String> list = recognizedMap.get(str2);

            if (list.contains(str1))    {
                return true;
            }

            return false;
        }

        @Override
        public ArrayList<Formula> askWithRestriction(int argnum1, String term1, int argnum2, String term2) {
            return LANG_FORMAT_MAP_COLS;
        }

        @Override
        public HashMap<String,String> getTermFormatMap(String lang)    {
            return termFormatMap;
        }
    }

    /**
     * Mock of KBcache.
     */
    protected class KBcacheMock extends KBcache     {

        public KBcacheMock(KB kb) {
            super(kb);
            this.signatures = signaturesMap;
        }

        @Override
        public boolean isInstanceOf(String str1, String str2)    {
            List<String> list = recognizedMap.get(str2);

            if (list.contains(str1))    {
                return true;
            }
            return false;
        }
    }

    @BeforeClass
    // JERRY: do I need to tearDown; i.e. if I don't are other tests messed up?
    public static void setUp() {
        LanguageFormatter.readKeywordMap(KB_PATH);

        Hashtable<String,String> hash = new Hashtable<String, String>();
        hash.put("drink", "");
        hash.put("drive", "");
        hash.put("eat", "");
        hash.put("give", "");
        hash.put("read", "");
        hash.put("go", "");

        oldWordNetSynSetTable = WordNet.wn.verbSynsetHash;
        WordNet.wn.verbSynsetHash = hash;
    }

    @AfterClass
    public static void tearDown() {
        WordNet.wn.verbSynsetHash = oldWordNetSynSetTable;
    }


}
