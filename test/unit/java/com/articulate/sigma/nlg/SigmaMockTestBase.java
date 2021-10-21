package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBcache;
import com.articulate.sigma.wordNet.WordNet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.util.*;

/****************************************************************
 * Class mocks KB and KBcache so that unit tests can be run independently and without long initialization.
 * Add processes and case roles to the static final lists as necessary.
 */
public class SigmaMockTestBase {

    private static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    private static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();
    private static HashMap<String, HashSet<String>> oldWordNetSynSetTable;

    protected final KB kbMock = new KBMock("dummyString");

    private static final ImmutableList<String> RECOGNIZED_PROCESSES = ImmutableList.of(
            "Drinking",
            "Driving",
            "Eating",
            "Giving",
// Process is not a subclass of Process           "Process",
            "Reading",
            "Seeing",
            "Transportation");

    private static final ImmutableList<String> RECOGNIZED_CASE_ROLES = ImmutableList.of(
            "agent",
     //       "benefactive",
            "destination",
            "experiencer",
            "goal",
            "instrument",
            "patient");

    // FIXME: not sure if instance should be here, or not
    private static final ImmutableList<String> RECOGNIZED_VARIABLE_ARITY_RELATIONS = ImmutableList.of("");

    // Add an item to one of these two lists--RECOGNIZED_SUBSTANCES or RECOGNIZED_CORPUSCULAR_OBJECTS--
    // if it is a "participant" in a process and
    // the NL output has it capitalized.
    // Add an item to RECOGNIZED_SUBSTANCES if it is a "participant" in a process and the NL output
    // precedes it with an indefinite article (i.e. "a" or "an").
    private static final ImmutableList<String> RECOGNIZED_SUBSTANCES = ImmutableList.of(
            "Coffee",
            "Food",
            "Tea"
            );
    private static final ImmutableList<String> RECOGNIZED_CORPUSCULAR_OBJECTS = ImmutableList.of(
            "Bell",
            "Book",
            "Human",
            "MedicalDoctor",
            "Automobile",
            "Taxi",
            "Telephone",
            "Truck");

    private static final Map<String, List<String>> recognizedMap = Maps.newHashMap();

    private static final ArrayList<String> INSTANCE_SIGNATURES = Lists.newArrayList(
            "",
            "Entity",
            "Class");

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
            new Formula("(format EnglishLanguage destination \"%1 %n{doesn't} %n &%end%p{s} at %2\")"),
            new Formula("(format EnglishLanguage instance \"%1 is %n an &%instance of %2\")"),
            new Formula("(format EnglishLanguage instrument \"%2 is %n an &%instrument for %1\")"),
            new Formula("(format EnglishLanguage located \"%1 is %n &%located at %2\")"),
            new Formula("(format EnglishLanguage member \"%1 is %n a &%member of %2\")"),
            new Formula("(format EnglishLanguage orientation \"%1 is %n %3 to %2\")"),
            new Formula("(format EnglishLanguage names \"%2 %n{doesn't have} %p{has} &%name %1\")"),
            new Formula("(format EnglishLanguage patient \"%2 is %n a &%patient of %1\")"),
            new Formula("(format EnglishLanguage subCollection \"%1 is %n a proper &%sub-collection of %2\")"),
            new Formula("(format EnglishLanguage subList \"%1 is %n a &%sublist of %2\")"),
            new Formula("(format EnglishLanguage transported \"%2 is %n &%transported during %1\")")
            );

    private static final HashMap<String, String> termFormatMap = Maps.newHashMap();

    /****************************************************************
     */
    static  {
        termFormatMap.put("Automobile", "automobile");
        termFormatMap.put("City", "city");
        termFormatMap.put("Clean", "clean");
        termFormatMap.put("Female", "female");
        termFormatMap.put("Human", "human");
        termFormatMap.put("Male", "male");
        termFormatMap.put("MedicalDoctor", "medical doctor");
        termFormatMap.put("Taxi", "taxi");
        termFormatMap.put("Tea", "tea");
        termFormatMap.put("Telephone", "telephone");
        termFormatMap.put("Truck", "truck");

        recognizedMap.put("Process", RECOGNIZED_PROCESSES);
        recognizedMap.put("CaseRole", RECOGNIZED_CASE_ROLES);
        recognizedMap.put("VariableArityRelation", RECOGNIZED_VARIABLE_ARITY_RELATIONS);
        recognizedMap.put("Substance", RECOGNIZED_SUBSTANCES);
        recognizedMap.put("CorpuscularObject", RECOGNIZED_CORPUSCULAR_OBJECTS);

        List<String> recognizedCaseRolesCapitalized = Lists.newArrayList();
        for (String str : RECOGNIZED_CASE_ROLES) {
            recognizedCaseRolesCapitalized.add(Character.toUpperCase(str.charAt(0)) + str.substring(1));
        }

        List<String> entityList = Lists.newArrayList(RECOGNIZED_PROCESSES);
        entityList.addAll(RECOGNIZED_SUBSTANCES);
        entityList.addAll(RECOGNIZED_CORPUSCULAR_OBJECTS);
        entityList.addAll(recognizedCaseRolesCapitalized);
        entityList.addAll(Lists.newArrayList("Process", "Substance", "CorpuscularObject", "City"));
        recognizedMap.put("Entity", entityList);

        signaturesMap.put("instance", INSTANCE_SIGNATURES);
        signaturesMap.put("agent", AGENT_SIGNATURES);
        signaturesMap.put("names", NAMES_SIGNATURES);

        termFormatMap.put("Process", "process");
        termFormatMap.put("Agent", "agent");
        termFormatMap.put("Entity", "entity");
    }

    /****************************************************************
     * Mock of KB.
     */
    protected class KBMock extends KB {

         public KBMock(String dummyStr) {
            super(dummyStr);
            kbCache = new KBcacheMock(this);
        }

        @Override
        public boolean isSubclass(String c1, String c2) {
            List<String> list = recognizedMap.get(c2);

            if (list != null && list.contains(c1)) {
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

    /****************************************************************
     * Mock of KBcache.
     */
    protected class KBcacheMock extends KBcache {

        public KBcacheMock(KB kb) {
            super(kb);
            this.signatures = signaturesMap;
        }

        @Override
        public boolean isInstanceOf(String i, String c)    {
            List<String> list = recognizedMap.get(c);

            if (list.contains(i))    {
                return true;
            }
            return false;
        }
    }

    /****************************************************************
     */
    @BeforeClass
    public static void setUp() {

        NLGUtils.readKeywordMap(KB_PATH);

        HashMap<String,HashSet<String>> hash = new HashMap<>();
        hash.put("drink", null);
        hash.put("drive", null);
        hash.put("eat", null);
        hash.put("give", null);
        hash.put("read", null);
        hash.put("go", null);

        oldWordNetSynSetTable = WordNet.wn.verbSynsetHash;
        WordNet.wn.verbSynsetHash = hash;
    }

    /****************************************************************
     */
    @AfterClass
    public static void tearDown() {
        WordNet.wn.verbSynsetHash = oldWordNetSynSetTable;
    }
}
