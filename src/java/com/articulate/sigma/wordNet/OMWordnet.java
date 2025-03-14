package com.articulate.sigma.wordNet;

import java.io.*;
import java.util.*;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OMWordnet implements Serializable {

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.

 Authors:
 Adam Pease
 Infosys LTD.
 */
    // String key of language name
    // Interior key of a 9-digit WordNet synset and value of and ArrayList of
    // non-English synset Strings
    public Map<String,Map<String,List<String>>> wordnets =
            new HashMap<>();
    public Map<String,Map<String,List<String>>> glosses =
            new HashMap<>();
    public Map<String,Map<String,List<String>>> examples =
            new HashMap<>();

    public static OMWordnet omw;

    public static boolean disable = false; // disable for debugging

    /** *************************************************************
     */
    private static char getOMWMappingSuffix(String SUMOmapping) {

        switch (WordNetUtilities.getSUMOMappingSuffix(SUMOmapping)) {
            case '=': return '=';
            case '+': return '\u2282'; // '⊂';
            case '@': return '\u2208'; // '∈';
            case ':': return '\u2260'; // '≠';
            case '[': return '\u2283'; // '⊃';
        }
        return ' ';
    }

    /** *************************************************************
     */
    private static void generateOMWformat(String fileWithPath) {

        System.out.println("INFO in WordNetUtilities.generateOMWformat(): writing file " + fileWithPath);
        try {
            File f = new File(fileWithPath);
            FileWriter r = new FileWriter(f);
            PrintWriter pw = new PrintWriter(r);
            pw.println("# SUMO http://www.ontologyportal.org");
            String SUMOterm, mappingSuffix;
            for (String key : WordNet.wn.nounSUMOHash.keySet()) {
                SUMOterm = WordNet.wn.nounSUMOHash.get(key);
                mappingSuffix = Character.toString(getOMWMappingSuffix(SUMOterm));
                if (!SUMOterm.contains(" "))
                    pw.println(key + "-n\tsumo:xref\t" + WordNetUtilities.getBareSUMOTerm(SUMOterm) + "\t" + mappingSuffix);
            }
            for (String key : WordNet.wn.verbSUMOHash.keySet()) {
                SUMOterm = WordNet.wn.verbSUMOHash.get(key);
                mappingSuffix = Character.toString(getOMWMappingSuffix(SUMOterm));
                if (!SUMOterm.contains(" "))
                    pw.println(key + "-n\tsumo:xref\t" + WordNetUtilities.getBareSUMOTerm(SUMOterm) + "\t" + mappingSuffix);
            }
            for (String key : WordNet.wn.adjectiveSUMOHash.keySet()) {
                SUMOterm = WordNet.wn.adjectiveSUMOHash.get(key);
                mappingSuffix = Character.toString(getOMWMappingSuffix(SUMOterm));
                if (!SUMOterm.contains(" "))
                    pw.println(key + "-n\tsumo:xref\t" + WordNetUtilities.getBareSUMOTerm(SUMOterm) + "\t" + mappingSuffix);
            }
            for (String key : WordNet.wn.adverbSUMOHash.keySet()) {
                SUMOterm = WordNet.wn.adverbSUMOHash.get(key);
                mappingSuffix = Character.toString(getOMWMappingSuffix(SUMOterm));
                if (!SUMOterm.contains(" "))
                    pw.println(key + "-n\tsumo:xref\t" + WordNetUtilities.getBareSUMOTerm(SUMOterm) + "\t" + mappingSuffix);
            }
        }
        catch (IOException ioe) {
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /** *************************************************************
     */
    private static void readOMWformat(String inputFileWithPath, String langName) {

        //System.out.println("INFO in WordNetUtilities.readOMWformat(): creating table entry for " + langName);
        Map<String,List<String>> wordnet = new HashMap<>();
        OMWordnet.omw.wordnets.put(langName,wordnet);
        Map<String,List<String>> gloss = new HashMap<>();
        OMWordnet.omw.glosses.put(langName,gloss);
        Map<String,List<String>> example = new HashMap<>();
        OMWordnet.omw.examples.put(langName,example);
        File inputf = new File(inputFileWithPath);
        if (!inputf.exists()) return;
        String line;
        //System.out.println("INFO in WordNetUtilities.readOMWformat(): read file " + inputFileWithPath);
        try (Reader fr = new FileReader(inputf);
            LineNumberReader lr = new LineNumberReader(fr)) {
            String id, type, value;
            List<String> val;
            while ((line = lr.readLine()) != null) {
                if (line.startsWith("#")) continue;
                //System.out.println(line);
                int tabIndex = line.indexOf("\t");
                if (tabIndex > -1) {
                    id = line.substring(0,tabIndex);
                    int tab2index = line.indexOf("\t",tabIndex+1);
                    if (tab2index > -1) {
                        //System.out.println(tabIndex + " " + tab2index);
                        type = line.substring(tabIndex+1,tab2index);
                        if (type.endsWith("lemma")) {
                            int end = line.length();
                            value = line.substring(tab2index+1,end);
                            val = wordnet.get(id);
                            if (val == null)
                                val = new ArrayList<>();
                            val.add(value);
                            wordnet.put(id,val);
                        }
                        if (type.contains(":def ")) {
                            int end = line.length();
                            value = line.substring(tab2index+1,end);
                            val = gloss.get(id);
                            if (val == null)
                                val = new ArrayList<>();
                            val.add(value);
                            gloss.put(id,val);
                        }
                        if (type.contains(":exe ")) {
                            int end = line.length();
                            value = line.substring(tab2index+1,end);
                            val = example.get(id);
                            if (val == null)
                                val = new ArrayList<>();
                            val.add(value);
                            example.put(id,val);
                        }
                    }
                }
            }
        }
        catch (IOException ioe) {
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /** *************************************************************
     */
    public static List<String> lcodes = new ArrayList<>(Arrays.asList(
            "als","arb","bul",
            "cat","cow","dan",
            "ell","eng","eus",
            "fas","fin","fra",
            "glg","heb",
            "hrv","isl","ita",
            "ind","jpn","nno",
            "nob","pol","por",
            "qcn","spa","swe",
            "tha","zsm"));
    public static List<String> lnames = new ArrayList<>(Arrays.asList(
            "AlbanianLanguage","ArabicLanguage","BulgarianLanguage",
            "CatalanLanguage","ChineseLanguage","DanishLanguage",
            "GreekLanguage","EnglishLanguage","BasqueLanguage",
            "FarsiLanguage","FinnishLanguage","FrenchLanguage",
            "GalicianLanguage","HebrewLanguage",
            "CroatianLanguage","IcelandicLanguage","ItalianLanguage",
            "IndonesianLanguage","JapaneseLanguage","NorwegianNorskLanguage",
            "NorwegianBokmalLanguage","PolishLanguage","PortugueseLanguage",
            "TaiwanChineseLanguage","SpanishLanguage","SwedishLanguage",
            "ThaiLanguage","MalayLanguage"));


    /** *************************************************************
     */
    public static String codeToLanguage(String code) {

        if (lcodes.contains(code))
            return lnames.get(lcodes.indexOf(code));
        else
            return "";
    }

    /** *************************************************************
     */
    public static String languageToCode(String lang) {

        if (lnames.contains(lang))
            return lcodes.get(lnames.indexOf(lang));
        else
            return "";
    }

    /** *************************************************************
     * Convert a 9-digit, POS-prefixed WordNet synset to a POS-suffix
     * OMW synset.
     */
    public static String toOMWsynset(String synset) {

        //System.out.println("INFO in OMWordnet.toOMWsynset(): " + synset);
        if (synset.length() != 9) {
            System.err.println("Error in OMWordnet.toOMWsynset(): synset not 9 digits: " + synset);
            return synset;
        }
        char POS = WordNetUtilities.posNumberToLetter(synset.charAt(0));
        return synset.substring(1) + "-" + POS;
    }

    /** *************************************************************
     * Convert a POS-suffix OMW synset to an 8-digit WordNet synset.
     */
    public static String fromOMWsynset(String synset) {

        //System.out.println("INFO in OMWordnet.fromOMWsynset(): " + synset);
        if (synset.length() != 10) {
            System.err.println("Error in OMWordnet.fromOMWsynset(): synset not 9 digits: " + synset);
            return synset;
        }
        return synset.substring(0,synset.length()-2);
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

        Path path = Paths.get(WordNet.baseDir + File.separator + "omw.ser");
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

        OMWordnet ob = null;
        Path path = Paths.get(WordNet.baseDir + File.separator + "omw.ser");
        try (Input input = new Input(Files.newInputStream(path))) {
            ob = kryoLocal.get().readObject(input,OMWordnet.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return (T) ob;
    }

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedOld() {

        File serfile = new File(WordNet.baseDir + File.separator + "omw.ser");
        Date saveDate = new Date(serfile.lastModified());
        System.out.println("OMWordnet.serializedOld(): " + serfile.getName() + " save date: " + saveDate.toString());
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String filename;
        Date fileDate;
        File file;
        for (int i = 0; i < lcodes.size(); i++) {
            filename = kbDir + File.separator + "OMW" +
                    File.separator + lcodes.get(i)  + File.separator +
                    "wn-data-" + lcodes.get(i) + ".tab";
            file = new File(filename);
            fileDate = new Date(file.lastModified());
            if (saveDate.compareTo(fileDate) < 0) {
                return true;
            }
        }
        return false;
    }

    /** ***************************************************************
     *  Load the most recently save serialized version.
     */
    public static void loadSerialized() {

        if (KBmanager.getMgr().getPref("loadLexicons").equals("false"))
            return;
        omw = null;
        try {
            if (serializedOld()) {
                System.out.println("OMWordnet.loadSerialized(): serialized file is older than sources, " +
                        "reloding from sources.");
                return;
            }
            omw = decoder();
            System.out.println("OMWordnet.loadSerialized(): OMW has been deserialized ");
        }
        catch(Exception ex) {
            System.err.println("Error in OMWordnet.loadSerialized()");
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     *  save serialized version.
     */
    public static void serialize() {

        try {
            // Reading the object from a file
            //FileOutputStream file = new FileOutputStream(baseDir + File.separator + "omw.ser");
            //ObjectOutputStream out = new ObjectOutputStream(file);
            // Method for deserialization of object
            //out.writeObject(omw);
            encoder(omw);
            //out.close();
            //file.close();
            System.out.println("OMWordnet.serialize(): OMW has been serialized ");
        }
        catch(Exception ex) {
            System.err.println("Error in OMWordNet.serialize(): IOException is caught");
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static boolean serializedExists() {

        File serfile = new File(WordNet.baseDir + File.separator + "omw.ser");
        System.out.println("OMWordnet.serializedExists(): " + serfile.exists());
        return serfile.exists();
    }

    /** *************************************************************
     * Assumes a fixed set of files in the KBs directory.
     */
    public static void readOMWfiles() {

        if (KBmanager.getMgr().getPref("loadLexicons").equals("false"))
            disable = true;
        if (disable)
            return;
        if (!KBmanager.getMgr().getPref("loadFresh").equals("true") && serializedExists())
            loadSerialized();
        if (omw != null)
            return;
        omw = new OMWordnet();
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        System.out.println("INFO in OMWordnet.readOMWfiles(): reading files: ");
        String filename;
        for (int i = 0; i < lcodes.size(); i++) {
            filename = kbDir + File.separator + "OMW" +
                    File.separator + lcodes.get(i)  + File.separator +
                    "wn-data-" + lcodes.get(i) + ".tab";
            System.out.print(filename);
            readOMWformat(filename,lcodes.get(i));
        }
        serialize();
        System.out.println();
    }

    /** *************************************************************
     */
    public static void generateOMWOWLformat(KB kb) {

        //System.out.println("INFO in WordNetUtilities.generateOMWformat(): writing file ");
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        File f = new File(kbDir + File.separator + "OMW" +
                File.separator + "OMW.owl");
        try (FileWriter fw = new FileWriter(f);
            PrintWriter pw = new PrintWriter(fw)) {
            pw.println("<rdf:RDF xml:base=\"http://www.ontologyportal.org/SUMO.owl\">");
            pw.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/SUMO.owl\">");
            pw.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
            pw.println("www.ontologyportal.org for the original KIF, which is the authoritative");
            pw.println("source.  This software is released under the GNU Public License");
            pw.println("www.gnu.org.</rdfs:comment><rdfs:comment xml:lang=\"en\">Produced on date: Tue Sep 03 11:07:34 PDT 2013");
            pw.println("</rdfs:comment></owl:Ontology><owl:Class rdf:about=\"#Object\">");
            pw.println("<rdfs:isDefinedBy rdf:resource=\"http://www.ontologyportal.org/SUMO.owl\"/>");
            pw.println("<wnd:equivalenceRelation rdf:resource=\"http://www.ontologyportal.org/WNDefs.owl#WN30-100019613\"/>");
        }
        catch (IOException ioe) {
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /** ***************************************************************
     * HTML format a list of word senses
     * @param term is the SUMO term
     * @param lang is the SUMO term for a language (EnglishLanguage, FrenchLanguage etc)
     */
    public static String formatWords(String term, String kbName, String lang, String href) {

        //System.out.println("INFO in OMWordnet.formatWords(): " + term + " " + lang);
        Map<String,List<String>> wordnet = omw.wordnets.get(languageToCode(lang));
        if (wordnet == null || wordnet.isEmpty())
            return "";
        StringBuilder result = new StringBuilder();
        List<String> synsets = WordNet.wn.SUMOHash.get(term);
        int limit = synsets.size();
        if (limit > 50)
            limit = 50;

        String synset;
        String OMWsynset;
        List<String> words;
        for (int i = 0; i < limit; i++) {
            synset = synsets.get(i);
            OMWsynset = toOMWsynset(synset);
            words = wordnet.get(OMWsynset);
            if (words != null) {
                for (int j = 0; j < words.size(); j++) {
                    result.append("<a href=\"").append(href).append("OMW.jsp?kb=").append(kbName).append("&synset=").append(OMWsynset).append("\">");
                    result.append(words.get(j));
                    result.append("</a>");
                    if (j < words.size() - 1)
                        result.append(", ");
                }
                if (i < limit - 1)
                    result.append(", ");
            }
        }
        if (synsets.size() > 50)
            result.append("...");
        return result.toString();
    }

    /** *************************************************************
     */
    private static String formatArrayList(List<String> al) {

        if (al == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < al.size(); i++) {
            sb.append(al.get(i));
            if (i<al.size()-1) sb.append(", ");
        }
        return sb.toString();
    }

    /** *************************************************************
     */
    public static String displaySynset(String kbName, String synset, String params) {

        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        String name;
        String id;
        List<String> words;
        List<String> exams;
        List<String> defs;
        for (int i = 0; i < lnames.size(); i++) {
            name = lnames.get(i);
            id = lcodes.get(i);
            words = omw.wordnets.get(id).get(synset);
            exams = omw.examples.get(id).get(synset);
            defs = omw.glosses.get(id).get(synset);
            if (words != null || exams != null || defs != null) {
                sb.append("<tr><td><strong>").append(name.substring(0,name.length()-8)).append("</strong></td>\n");
                sb.append("<td>");
                if (words != null)
                    sb.append(formatArrayList(words)).append("<br>\n");
                if (defs != null)
                    sb.append("<i>").append(formatArrayList(defs)).append("</i><br>\n");
                if (exams != null)
                    sb.append("<small>").append(formatArrayList(exams)).append("</small>\n");
                sb.append("</td></tr>\n");
            }
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
            //readOMWfiles();
            //System.out.println(formatWords("Table","","FrenchLanguage",""));
            generateOMWformat("wn-data-smo.tab");
        }
        catch (Exception e) {
            System.err.println("Error in OMWordnet.main(): Exception: " + e.getMessage());
            e.printStackTrace();
        }

    }
}

