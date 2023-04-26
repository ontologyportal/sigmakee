package com.articulate.sigma.mlpipeline;

import com.articulate.sigma.utils.AVPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * **************************************************************
 */
public class LFeatures {

    private static final boolean debug = false;

    public boolean testMode = false;

    private final GenSimpTestData genSimpTestData;
    public boolean attNeg = false; // for propositional attitudes
    public boolean attPlural = false;
    public int attCount = 1;
    public String attSubj = null; // the agent holding the attitude
    public String attitude = "None";
    public boolean negatedModal = false;
    public boolean negatedBody = false;
    public ArrayList<AVPair> modals = null;
    public AVPair modal = new AVPair("None", "none"); // attribute if SUMO ModalAttribute, value is English
    public HashMap<String, String> genders = null;
    public RandSet humans = null;
    public RandSet socRoles = null;
    public RandSet objects = null;
    public RandSet bodyParts = null;
    public String directPrep = "";
    public String indirectPrep = "";
    public String secondVerb = ""; // the verb word that appears as INFINITIVE or VERB-ing or V-ing in the frame
    public String secondVerbType = ""; // the SUMO type of the second verb
    public String secondVerbSynset = "";
    public HashSet<String> prevHumans = new HashSet<>();
    public String subj = "";
    public String subjName = "";
    public boolean subjectPlural = false;
    public int subjectCount = 1;

    public RandSet processes = null;
    public static boolean useCapabilities = true; // include process types from capabilities list

    public ArrayList<String> frames = null;  // verb frames for the current process type
    public String frame = null; // the particular verb frame under consideration.
    public String framePart = null; // the frame that gets "consumed" during processing
    // Note that the frame is destructively modified as we proceed through the sentence
    public String verbSynset = null;
    public String directName = null;  // the direct object
    public String directType = null;  // the direct object
    public boolean directPlural = false;
    public int directCount = 1;
    public String indirectName = null; // the indirect object
    public String indirectType = null; // the indirect object
    public boolean indirectPlural = false;
    public int indirectCount = 1;
    public boolean question = false;
    public String verb = "";
    public String verbType = ""; // the SUMO class of the verb
    public int tense = GenSimpTestData.NOTIME;
    public boolean polite = false;  // will a polite phrase be used for a sentence if it's an imperative
    public boolean politeFirst = true; // if true and an imperative and politness used, put it at the beginning of the sentence, otherwise at the end

    public LFeatures(GenSimpTestData genSimpTestData) {
        this.genSimpTestData = genSimpTestData;

        //  get capabilities from axioms like
        //  (=> (instance ?GUN Gun) (capability Shooting instrument ?GUN))
        // indirect = collectCapabilities(); // TODO: need to restore and combine this filter with verb frames
        System.out.println("LFeatures(): collect terms");
        genders = GenSimpTestData.readHumans();
        humans = RandSet.listToEqualPairs(genders.keySet());

        modals = initModals();

        HashSet<String> roles = GenSimpTestData.kb.kbCache.getInstancesForType("SocialRole");
        //if (debug) System.out.println("LFeatures(): SocialRoles: " + roles);
        Collection<AVPair> roleFreqs = genSimpTestData.findWordFreq(roles);
        socRoles = RandSet.create(roleFreqs);

        HashSet<String> parts = GenSimpTestData.kb.kbCache.getInstancesForType("BodyPart");
        //if (debug) System.out.println("LFeatures(): BodyParts: " + parts);
        Collection<AVPair> bodyFreqs = genSimpTestData.findWordFreq(parts);
        bodyParts = RandSet.create(bodyFreqs);

        HashSet<String> artInst = GenSimpTestData.kb.kbCache.getInstancesForType("Artifact");
        HashSet<String> artClass = GenSimpTestData.kb.kbCache.getChildClasses("Artifact");

        Collection<AVPair> procFreqs = genSimpTestData.findWordFreq(GenSimpTestData.kb.kbCache.getChildClasses("Process"));
        processes = RandSet.create(procFreqs);

        if (useCapabilities) {
            RandSet rs = RandSet.listToEqualPairs(GenSimpTestData.capabilities.keySet());
            processes.terms.addAll(rs.terms);
        }

        HashSet<String> orgInst = GenSimpTestData.kb.kbCache.getInstancesForType("OrganicObject");
        HashSet<String> orgClass = GenSimpTestData.kb.kbCache.getChildClasses("OrganicObject");

        HashSet<String> objs = new HashSet<>();
        objs.addAll(orgClass);
        objs.addAll(artClass);
        HashSet<String> objs2 = new HashSet<>();
        for (String s : objs)
            if (!s.equals("Human") && !GenSimpTestData.kb.isSubclass(s, "Human"))
                objs2.add(s);
        if (debug) System.out.println("LFeatures(): OrganicObjects and Artifacts: " + objs);
        Collection<AVPair> objFreqs = genSimpTestData.findWordFreq(objs2);
        System.out.println("LFeatures(): create objects");
        objects = RandSet.create(objFreqs);
        //System.out.println("LFeatures(): objects: " + objects.terms);
    }


    /** ***************************************************************
     */
    public ArrayList<AVPair> initModals() {

        ArrayList<AVPair> modals = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            modals.add(new AVPair("None",""));
        if (!GenSimpTestData.suppress.contains("modal")) {
            modals.add(new AVPair("Necessity", "it is necessary that "));
            modals.add(new AVPair("Possibility", "it is possible that "));
            modals.add(new AVPair("Obligation", "it is obligatory that "));
            modals.add(new AVPair("Permission", "it is permitted that "));
            modals.add(new AVPair("Prohibition", "it is prohibited that "));
            modals.add(new AVPair("Likely", "it is likely that "));
            modals.add(new AVPair("Unlikely", "it is unlikely that "));
        }
        return modals;
    }

    /**
     * **************************************************************
     * clear basic flags in the non-modal part of the sentence
     */
    public void clearSVO() {

        subj = null;
        subjectPlural = false;
        subjectCount = 1;
        directName = null;  // the direct object
        directType = null;  // the direct object
        directPlural = false;
        directPrep = "";
        directCount = 1;
        indirectName = null; // the indirect object
        indirectType = null; // the indirect object
        indirectPlural = false;
        indirectPrep = "";
        indirectCount = 1;
        secondVerb = "";
        secondVerbType = "";
    }
}
