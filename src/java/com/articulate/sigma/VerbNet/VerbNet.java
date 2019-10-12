package com.articulate.sigma.VerbNet;

import com.articulate.sigma.*;
import com.articulate.sigma.wordNet.WordNet;

import java.io.*;
import java.util.*;

/**
 * Created by apease on 7/23/18.
 */
public class VerbNet {

    public static KB kb;

    private static boolean debug = true;
    private static boolean echo = false;
    private static HashMap<String,SimpleElement> verbFiles = new HashMap<>();
    private static HashMap<String,String> roles = new HashMap<>(); // VN to SUMO role mappings
    private static boolean initialized = false;
    public static int verbcount = 0;
    public static int syncount = 0;

    // a mapping of a WordNet key to a VerbNet pair of VerbID\tmember-word-name
    public static HashMap<String,String> wnMapping = new HashMap<>();

    // verb ID keys and Verb values
    public static HashMap<String,Verb> verbs = new HashMap<>();

    /** *************************************************************
     */
    public static void initOnce() {

        ArrayList<String> keys = new ArrayList<String>(Arrays.asList("Actor","involvedInEvent",
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
                roles.put(keys.get(i*2 - 1), keys.get(i*2));
            }
            readVerbFiles();
            initialized = true;
        }
    }

    /** *************************************************************
     */
    public static void readVerbFiles() {

        SimpleElement configuration = null;
        try {
            String dirStr = "/home/apease/ontology/VerbNet3-2";
            File dir = new File(dirStr);
            if (!dir.exists()) {
                return;
            }
            try {
                File folder = new File(dirStr);
                for (File fileEntry : folder.listFiles()) {
                    if (!fileEntry.toString().endsWith(".xml"))
                        continue;
                    BufferedReader br = new BufferedReader(new FileReader(fileEntry.toString()));
                    SimpleDOMParser sdp = new SimpleDOMParser();
                    verbFiles.put(fileEntry.toString(), sdp.parse(br));
                }
            }
            catch (FileNotFoundException e) {
                System.out.println("Error in VerbNet.readVerbFiles(): " + e.getMessage());
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            System.out.println("Error in VerbNet.readVerbFiles(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *************************************************************
     */
    public static void processVerbs() {

        for (String fname : verbFiles.keySet()) {
            if (echo) System.out.println("\n==================");
            if (echo) System.out.println("VerbNet.processVerbs(): " + fname);
            SimpleElement verb = verbFiles.get(fname);
            String name = (String) verb.getAttribute("ID");
            verbcount++;
            String xmlns = (String) verb.getAttribute("xmlns:xsi");
            String xsi = (String) verb.getAttribute("xsi:noNamespaceSchemaLocation");
            Verb v = new Verb();
            v.ID = name;
            v.readVerb(verb);
            verbs.put(name,v);
        }
    }

    /** *************************************************************
     */
    private static String formatForSynset(String synset) {

        StringBuffer result = new StringBuffer();
        String verb = VerbNet.wnMapping.get(synset);
        if (StringUtil.emptyString(verb) || !verb.contains("|"))
            return "";
        String ID = verb.substring(0,verb.indexOf("|"));
        result.append("<a href=\"http://verbs.colorado.edu/verb-index/vn/" + ID + ".php\">" + verb + "</a>, ");
        return result.toString();
    }

    /** *************************************************************
     * @param tm Map of words with their corresponding synset numbers
     */
    public static String formatVerbsList(TreeMap<String,ArrayList<String>> tm) {

        StringBuffer result = new StringBuffer();
        int count = 0;
        Iterator<String> it = tm.keySet().iterator();
        while (it.hasNext() && count < 50) {
            String word = (String) it.next();
            ArrayList<String> synsetList = tm.get(word);
            for (int i = 0; i < synsetList.size(); i++) {
                String synset = synsetList.get(i);
                String res = formatForSynset(synset);
                if (StringUtil.emptyString(res))
                    continue;
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
    public static String formatVerbs(TreeMap<String,String> tm) {

        StringBuffer result = new StringBuffer();
        int count = 0;
        Iterator<String> it = tm.keySet().iterator();
        while (it.hasNext() && count < 50) {
            String word = it.next();
            String synset = tm.get(word);
            String res = formatForSynset(synset);
            if (StringUtil.emptyString(res))
                continue;
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
        kb = KBmanager.getMgr().getKB("SUMO");
        System.out.println("VerbNet.main()");
        initOnce();
        processVerbs();
        System.out.println("# of verbs: " + verbcount);
        System.out.println("# of mapped synsets: " + syncount);
        System.out.println("VerbNet.main(): get vb for wn 200686447: " + VerbNet.wnMapping.get("200686447"));
    }
}