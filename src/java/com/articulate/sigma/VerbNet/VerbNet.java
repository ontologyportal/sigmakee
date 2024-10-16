package com.articulate.sigma.VerbNet;

/** This code is copyright Infosys 2019.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
 */

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WordNet;

import java.io.*;
import java.util.*;

/**
 * Created by apease on 7/23/18.
 */
public class VerbNet {

    public static KB kb;

    private static final boolean DEBUG = false;
    private static final boolean ECHO = false;
    private static final Map<String,SimpleElement> VERB_FILES = new HashMap<>();
    private static final Map<String,String> ROLES = new HashMap<>(); // VN to SUMO role mappings
    private static boolean initialized = false;
    public static int verbcount = 0;
    public static int syncount = 0;

    // a mapping of a WordNet key to a VerbNet pair of VerbID\tmember-word-name
    public static Map<String,String> wnMapping = new HashMap<>();

    // verb ID keys and Verb values
    public static Map<String,Verb> verbs = new HashMap<>();

    public static boolean disable = false;

    /** *************************************************************
     */
    public static void initOnce() {

        if (KBmanager.getMgr().getPref("loadLexicons").equals("false"))
            disable = true;
        if (disable) return;
        List<String> keys = new ArrayList<>(Arrays.asList("Actor","involvedInEvent",
            "Agent","agent", "Asset","objectTransferred", "Attribute","attribute",
            "Beneficiary","beneficiary", "Cause","involvedInEvent",
            "Co-Agent","agent", "Co-Patient","patient", "Co-Theme","patient",
            "Destination","destination", "Duration","time",
            "Experiencer","experiencer", "Extent","", "Final_Time","EndFn",
            "Frequency","frequency", "Goal","", "Initial_Location","origin",
            "Initial_Time","BeginFn", "Instrument","instrument",
            "Location","located", "Material","resource",
            "Participant","involvedInEvent", "Patient","patient",
            "Pivot","patient", "Place","located", "Product","result",
            "Recipient","recipient", "Result","result",
            "Source","origin", "Stimulus","causes", "Time","WhenFn",
            "Theme","patient", "Trajectory","path",
            "Topic","containsInformation", "Undergoer","patient",
            "Value", "measure"));
        if (!initialized) {
            for (int i = 1; i < keys.size()/2; i++) {
                ROLES.put(keys.get(i*2 - 1), keys.get(i*2));
            }
            readVerbFiles();
            initialized = true;
        }
    }

    /** *************************************************************
     */
    public static void readVerbFiles() {

        try {
            String dirStr = KBmanager.getMgr().getPref("verbnet");
            System.out.println("VerbNet.readVerbFiles(): loading files from: " + dirStr);
            File dir = new File(dirStr);
            if (!dir.exists()) {
                System.out.println("VerbNet.readVerbFiles(): no such dir: " + dirStr);
                return;
            }
            try {
                File folder = new File(dirStr);
                BufferedReader br;
                SimpleDOMParser sdp;
                for (File fileEntry : folder.listFiles()) {
                    if (!fileEntry.toString().endsWith(".xml"))
                        continue;
                    br = new BufferedReader(new FileReader(fileEntry.toString()));
                    sdp = new SimpleDOMParser();
                    VERB_FILES.put(fileEntry.toString(), sdp.parse(br));
                }
            }
            catch (FileNotFoundException e) {
                System.err.println("Error in VerbNet.readVerbFiles(): " + e.getMessage());
                e.printStackTrace();
            }
        }
        catch (IOException e) {
            System.err.println("Error in VerbNet.readVerbFiles(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *************************************************************
     */
    public static void processVerbs() {

        SimpleElement verb;
        String name, xmlns, xsi;
        Verb v;
        for (String fname : VERB_FILES.keySet()) {
            if (ECHO) System.out.println("\n==================");
            if (ECHO) System.out.println("VerbNet.processVerbs(): " + fname);
            verb = VERB_FILES.get(fname);
            name = (String) verb.getAttribute("ID");
            verbcount++;
            xmlns = (String) verb.getAttribute("xmlns:xsi");
            xsi = (String) verb.getAttribute("xsi:noNamespaceSchemaLocation");
            v = new Verb();
            v.ID = name;
            v.readVerb(verb);
            verbs.put(name,v);
        }
    }

    /** *************************************************************
     */
    private static String formatForSynset(String synset) {

        StringBuilder result = new StringBuilder();
        String verb = VerbNet.wnMapping.get(synset);
        if (StringUtil.emptyString(verb) || !verb.contains("|"))
            return "";
        String ID = verb.substring(0,verb.indexOf("|"));
        String link = "<a href=\"http://verbs.colorado.edu/verb-index/vn/" + ID + ".php\">" + verb + "</a>, ";
        if (!result.toString().contains(verb))
            result.append(link);
        return result.toString();
    }

    /** *************************************************************
     * @param tm Map of words with their corresponding synset numbers
     */
    public static String formatVerbsList(TreeMap<String,ArrayList<String>> tm) {

        StringBuilder result = new StringBuilder();
        int count = 0;
        Iterator<String> it = tm.keySet().iterator();
        String word, synset, res;
        List<String> synsetList;
        while (it.hasNext() && count < 50) {
            word = (String) it.next();
            synsetList = tm.get(word);
            for (int i = 0; i < synsetList.size(); i++) {
                synset = synsetList.get(i);
                res = formatForSynset(synset);
                if (StringUtil.emptyString(res))
                    continue;
                if (StringUtil.emptyString(result.toString()))
                    result.append("VerbNet: ");
                result.append(res);
                count++;
            }
        }
        if (it.hasNext() && count >= 50)
            result.append("...");
        return result.toString();
    }

    /** *************************************************************
     * @param tm Map of words with their corresponding synset numbers
     */
    public static String formatVerbs(Map<String,String> tm) {

        StringBuilder result = new StringBuilder();
        int count = 0;
        Iterator<String> it = tm.keySet().iterator();
        String word, synset, res;
        while (it.hasNext() && count < 50) {
            word = it.next();
            synset = tm.get(word);
            res = formatForSynset(synset);
            if (StringUtil.emptyString(res))
                continue;
            if (StringUtil.emptyString(result.toString()))
                result.append("VerbNet: ");
            result.append(res);
            count++;
        }
        if (it.hasNext() && count >= 50)
            result.append("...");
        return result.toString();
    }

    /** *************************************************************
    */
    public static void main(String[] args) {

        KBmanager.getMgr().initializeOnce();
        WordNet.initOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        kb = KBmanager.getMgr().getKB(kbName);
        System.out.println("VerbNet.main()");
        initOnce();
        processVerbs();
        System.out.println("# of verbs: " + verbcount);
        System.out.println("# of mapped synsets: " + syncount);
        System.out.println("VerbNet.main(): get vb for wn 200686447: " + VerbNet.wnMapping.get("200686447"));
    }
}