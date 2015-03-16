package com.articulate.sigma.nlp;

/** This code is copyright Articulate Software (c) 2014.   This software is
released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.  
Users of this code also consent, by use of this code, to credit Articulate 
Software in any writings, briefings, publications, presentations, or other 
representations of any software which incorporates, builds on, or uses this code.  

This is a simple ChatBot written to illustrate TF/IDF-based information
retrieval.  It searches for the best match between user input and
a dialog corpus.  Once a match is found, it returns the next turn in the
dialog.  If there are multiple, equally good matches, a random one is
chosen.  It was written to use the Cornell Movie Dialogs Corpus
http://www.mpi-sws.org/~cristian/Cornell_Movie-Dialogs_Corpus.html
http://www.mpi-sws.org/~cristian/data/cornell_movie_dialogs_corpus.zip
(Danescu-Niculescu-Mizil and Lee, 2011) and the Open Mind Common Sense 
corpus (Singh et al., 2002) http://www.ontologyportal.org/content/omcsraw.txt.bz2
but has sense been revised to use any textual corpus.

The corpus will be read into ArrayList<String> lines and the user must
be boolean alternating to true if it's a dialog corpus in which a response
is in the line following a match.

Author: Adam Pease apease@articulatesoftware.com
*/

/*******************************************************************/

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

import com.google.common.io.Resources;

public class TFIDF {

      // inverse document frequency = log of number of documents divided by 
      // number of documents in which a term appears
    private HashMap<String,Float> idf = new HashMap<String,Float>();

      // number of documents in which a term appears
    private HashMap<String,Integer> docfreq = new HashMap<String,Integer>();

      // the length of a vector composed from each term frequency
    private HashMap<Integer,Float> euclid = new HashMap<Integer,Float>();

      // number of times a term appears in a document
    private HashMap<Integer,HashMap<String,Integer>> tf = new HashMap<Integer,HashMap<String,Integer>>();

      // number of times a term appears in a document * idf
    private HashMap<Integer,HashMap<String,Float>> tfidf = new HashMap<Integer,HashMap<String,Float>>();

    /** English "stop words" such as "a", "at", "them", which have no or little
     * inherent meaning when taken alone. */
    private ArrayList<String> stopwords = new ArrayList<String>();

      // each line of a corpus
    private ArrayList<String> lines = new ArrayList<String>();
    
      // when true, indicates that responses should be the line after the matched line
    private boolean alternating = false;
    
    private static boolean asResource = true; // use JUnit resource path for input file
    
      // similarity of each document to the query (index -1)
    private HashMap<Integer,Float> docSim = new HashMap<Integer,Float>();

    private Random rand = new Random(); 

    /** ***************************************************************
     */
    public TFIDF() {
        
        readStopWords();
    }
    
    /** ***************************************************************
     */
    public TFIDF(String filename) {
        
        readStopWords();
        int linecount = readFile("resources" + File.separator + "textfiles" + File.separator + filename);
        calcIDF(linecount);
        calcTFIDF();
    }

    /** ***************************************************************
     */
    public TFIDF(List<String> l) {
        
        int linecount = lines.size() - 1;
        readStopWords();
        for (String s : l) {
            linecount++;
            lines.add(s);
            processDoc(s,linecount);
        }
        lines.addAll(l);
        calcIDF(linecount);
        calcTFIDF();
    }
    
    /** ***************************************************************
     * @param s An input Object, expected to be a String.
     * @return true if s == null or s is an empty String, else false.
     */
    private static boolean emptyString(Object s) {

        return ((s == null)
                || ((s instanceof String)
                    && s.equals("")));
    }

    /** ***************************************************************
     * Remove punctuation and contractions from a sentence. 
     * @return the sentence in a String minus these elements.
     */
    private String removePunctuation(String sentence) {

        Matcher m = null;
        if (emptyString(sentence))
            return sentence;
        m = Pattern.compile("(\\w)\\'re").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'m").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)n\\'t").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'ll").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'s").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'d").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'ve").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        sentence = sentence.replaceAll("\\'","");
        sentence = sentence.replaceAll("\"","");
        sentence = sentence.replaceAll("\\.","");
        sentence = sentence.replaceAll("\\;","");
        sentence = sentence.replaceAll("\\:","");
        sentence = sentence.replaceAll("\\?","");
        sentence = sentence.replaceAll("\\!","");
        sentence = sentence.replaceAll("\\, "," ");
        sentence = sentence.replaceAll("\\,[^ ]",", ");
        sentence = sentence.replaceAll("  "," ");
        return sentence;
    }

    /** ***************************************************************
     * Remove stop words from a sentence.
     * @return a string that is the sentence minus the stop words.
     */
    private String removeStopWords(String sentence) {

        if (emptyString(sentence))
            return "";
        String result = "";
        ArrayList al = splitToArrayList(sentence);
        if (al == null)
            return "";
        for (int i = 0; i < al.size(); i++) {
            String word = (String) al.get(i);
            if (!stopwords.contains(word.toLowerCase())) {
                if (result == "")
                    result = word;
                else
                    result = result + " " + word;
            }
        }
        return result;
    }

    /** ***************************************************************
     * Check whether the word is a stop word
     */
    private boolean isStopWord(String word) {

        if (emptyString(word))
            return false;
        if (stopwords.contains(word.trim().toLowerCase())) 
            return true;
        return false;
    }

   /** ***************************************************************
     * Read a file of stopwords into the variable 
     * ArrayList<String> stopwords
     */
    private void readStopWords() {

       // System.out.println("INFO in readStopWords(): Reading stop words");
        String filename = "";
        try {
            if (asResource) {
                URL stopWordsFile = Resources.getResource("resources/textfiles/stopwords.txt");
                filename = stopWordsFile.getPath();
            }
            else
                filename = "stopwords.txt";
            FileReader r = new FileReader(filename);
            LineNumberReader lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null)
                stopwords.add(line.intern());
        }
        catch (Exception i) {
            System.out.println("Error in readStopWords() reading file " + filename + ": " + i.getMessage());
            i.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     * @return an ArrayList of the string split by spaces.
     */
    private static ArrayList<String> splitToArrayList(String st) {

        if (emptyString(st)) {
            System.out.println("Error in WordNet.splitToArrayList(): empty string input");
            return null;
        }
        String[] sentar = st.split(" ");
        ArrayList<String> words = new ArrayList(Arrays.asList(sentar));
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equals("") || words.get(i) == null || words.get(i).matches("\\s*"))
                words.remove(i);
        }
        return words;
    }

    /** ***************************************************************
      * inverse document frequency = log of number of documents divided by 
      * number of documents in which a term appears.
      * Note that if the query is included as index -1 then it will
      * get processed too. Put the results into
      * HashMap<String,Float> idf 
     */
    private void calcIDF(int docCount) {

        Iterator<String> it = docfreq.keySet().iterator();
        while (it.hasNext()) {
            String token = it.next();
            float f = (float) Math.log10(docCount / docfreq.get(token));
            idf.put(token,new Float(f));
        }
        //System.out.println("IDF:\n" + idf);
    }

    /** ***************************************************************
      * Calculate TF/IDF and put the results in 
      * HashMap<Integer,HashMap<String,Float>> tfidf 
      * In the process, calculate the euclidean distance of the word
      * vectors and put in HashMap<Integer,Float> euclid
      * Note that if the query is included as index -1 then it will
      * get processed too.
     */
    private void calcOneTFIDF(Integer int1) {

        HashMap<String,Integer> tftermlist = tf.get(int1);
        if (tftermlist == null) {
            System.out.println("Error in calcOneTFIDF(): bad index: " + int1);
            return;
        }
        HashMap<String,Float> tfidflist = new HashMap<String,Float>();
        float euc = 0;
        Iterator<String> it2 = tftermlist.keySet().iterator();
        while (it2.hasNext()) {
            String term = it2.next();
            int tfint = tftermlist.get(term).intValue();
            float idffloat = idf.get(term).floatValue();
            float tfidffloat = idffloat * tfint;
            tfidflist.put(term,new Float(tfidffloat));
            euc = euc + (tfidffloat * tfidffloat);
        }
        euclid.put(int1,new Float((float) Math.sqrt(euc)));
        tfidf.put(int1,tfidflist);        
        //System.out.println("TF/IDF:\n" + tfidf);
    } 

    /** ***************************************************************
      * Calculate TF/IDF and put results in  
      * HashMap<Integer,HashMap<String,Float>> tfidf 
      * Note that if the query is included as index -1 then it will
      * get processed too.
      * This calls calcOneTFIDF() that does most of the work.
     */
    private void calcTFIDF() {

        Iterator<Integer> it1 = tf.keySet().iterator();
        while (it1.hasNext()) {
            Integer int1 = it1.next();
            calcOneTFIDF(int1);
        }
        //System.out.println("TF/IDF:\n" + tfidf);
    }

    /** ***************************************************************
     * sets the values in tf (term frequency) and tdocfreq (count of
     * documents in which a term appears)
     *
     * @param intlineCount is -1 for query
     */
    private void processDoc(String doc, Integer intlineCount) {

        if (emptyString(doc)) return;
        String line = removePunctuation(doc);
        line = removeStopWords(line);    
        if (emptyString(line.trim())) return;        
        ArrayList<String> tokens = splitToArrayList(line.trim());
        //System.out.println("ProcessDoc: " + tokens);
        HashSet<String> tokensNoDup = new HashSet<String>();
        HashMap<String,Integer> tdocfreq = new HashMap<String,Integer>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            Integer tcount = new Integer(0);
            if (tdocfreq.containsKey(token))
                tcount = tdocfreq.get(token);
            int tcountint = tcount.intValue() + 1;
            tcount = new Integer(tcountint);
            tdocfreq.put(token,tcount);
            if (!docfreq.containsKey(token))
                docfreq.put(token,new Integer(1));
            else {
                if (!tokensNoDup.contains(token)) {
                    Integer intval = docfreq.get(token);
                    int intvalint = intval.intValue();
                    docfreq.put(token,new Integer(intvalint + 1));
                    tokensNoDup.add(token);
                }
            }    
        }
        //System.out.println("ProcessDoc: adding for doc " + intlineCount + "\n" + tdocfreq);
        tf.put(intlineCount,tdocfreq);
    }

   /** ***************************************************************
    * Read a file from @param fname and store it in the 
    * ArrayList<String> lines member variable.
    * @return an int number of lines
     */
    private int readFile(String fname) {

        int linecount = lines.size() - 1;
        int counter = 0;
        String line = "";
        String filename = "";
        BufferedReader omcs = null;
        try {
            if (asResource) {
                URL fileURL = Resources.getResource(fname);
                filename = fileURL.getPath();
            }
            else
                filename = fname;
            File f = new File(filename);
            if (!f.exists())
                filename = fname;
            omcs = new BufferedReader(new FileReader(filename));
            /* readLine is a bit quirky :
             * it returns the content of a line MINUS the newline.
             * it returns null only for the END of the stream.
             * it returns an empty String if two newlines appear in a row. */
            while ((line = omcs.readLine()) != null) {
                counter++;
                if (counter == 1000) {
                    counter = 0;
                    System.out.print(".");
                }
                if (!emptyString(line)) {
                    linecount++;
                    Integer intlineCount = new Integer(linecount);
                    lines.add(line); 
                    //System.out.println(line);
                    processDoc(line,intlineCount);
                }
            }  
            System.out.println();
            omcs.close();         
        }
        catch (Exception ex)  {
            System.out.println("Error in readFile(): " + ex.getMessage());
            System.out.println("Error in at line: " + line);
            ex.printStackTrace();
        }
        //System.out.println("Movie lines:\n" + lines);
        //System.out.println("TF:\n" + tf);
        return linecount;      
    }

    /** ***************************************************************
     * Assume that query is file index -1
     * Calculate the similarity of each document to the query 
     * Put the result in HashMap<Integer,Float> docSim
     */
    private void calcDocSim() {

        Integer negone = new Integer(-1);
        HashMap<String,Float> tfidflist = tfidf.get(negone);
        HashMap<String,Float> normquery = new HashMap<String,Float>();
        float euc = euclid.get(negone);
        Iterator<String> it2 = tfidflist.keySet().iterator();
        while (it2.hasNext()) {
           String term = it2.next();
           float tfidffloat = tfidflist.get(term).floatValue();
           normquery.put(term,new Float(tfidffloat / euc));
        }

        Iterator<Integer> it1 = tf.keySet().iterator();
        while (it1.hasNext()) {
            Integer int1 = it1.next();
            if (int1.intValue() != -1) {
                tfidflist = tfidf.get(int1);
                euc = euclid.get(int1);
                float fval = 0;
                Iterator<String> it3 = tfidflist.keySet().iterator();
                while (it3.hasNext()) {
                    String term = it3.next();
                    float tfidffloat = tfidflist.get(term).floatValue();
                    float query = 0;
                    if (normquery.containsKey(term))
                        query = normquery.get(term).floatValue();
                    float normalize = 0;
                    if (euc != 0)
                        normalize = tfidffloat / euc;
                    fval = fval + (normalize * query);
                }
                docSim.put(int1,fval);
            }
        }
        //System.out.println("Doc sim:\n" + docSim);
    }

    /** *************************************************************
     * add a new document to the set
     */
    public void addInput(String input) {
        
        lines.add(input);
        int linecount = lines.size();
        calcIDF(linecount);
        calcTFIDF();
    }
    
    /** *************************************************************
     */
    protected String matchInput(String input) {
        return matchInput(input,1).get(0);        
    }
    
    /** *************************************************************
     * @return a string which is the best guess match of the input.
     * If global variable alternating is set to true, the return the
     * next line in the input file, which is therefore treated like a
     * dialog in which the best response to a given input is the line
     * after the line in the dialog that matches.  If there's more than
     * one reasonable response, pick a random one.
     */
    protected List<String> matchInput(String input, int n) {

        ArrayList<String> result = new ArrayList<String>();
        if (emptyString(input))
            System.exit(0);
        Integer negone = new Integer(-1);
        processDoc(input,negone);
        calcIDF(lines.size()+1);
        calcOneTFIDF(negone);
        //System.out.println("Caclulate docsim");
        calcDocSim();
        //System.out.println("Caclulate sorted sim");
        TreeMap<Float,ArrayList<Integer>> sortedSim = new TreeMap<Float,ArrayList<Integer>>();
          // private HashMap<Integer,Float> docSim = HashMap<Integer,Float>();
        Iterator<Integer> it = docSim.keySet().iterator();
        while (it.hasNext()) {
           Integer i = it.next();
           Float f = docSim.get(i);
           if (sortedSim.containsKey(f)) {
               ArrayList<Integer> vals = sortedSim.get(f);
               vals.add(i);
           }
           else {
               ArrayList<Integer> vals = new ArrayList<Integer>();
               vals.add(i);
               sortedSim.put(f,vals);
           }
        }
        
        Iterator<Float> it2 = sortedSim.descendingKeySet().iterator();
        int counter = n;
        while (it2.hasNext() && counter > 0) {
            Float f = it2.next();
            ArrayList<Integer> vals = sortedSim.get(f);
            int random = 0;
            Integer index = null;
            random = rand.nextInt(vals.size());
            index = vals.get(new Integer(random));
            counter--;
            if (!alternating)
                result.add(f + ":" + lines.get(new Integer(index.intValue())));
            else
                result.add(f + ":" + lines.get(new Integer(index.intValue()+1)));
        }        
        return result;
    }

    /** *************************************************************
     * Run a chatbot-style loop, asking for user input and finding
     * a response in the lines corpus via the TF/IDF algorithm.
     */
    private static void run() {

        run("ShellDoc.txt");
    }
        
    /** *************************************************************
     * Run with a given file
     */
    private static void run(String fname) {

        TFIDF cb = new TFIDF();
        cb.readStopWords();
        //System.out.println("Read movie lines");
        //int linecount = cb.readMovieLinesFile();
        //System.out.println("Read open mind");
        //linecount = linecount + cb.readOpenMind();
        System.out.println(fname);
        int linecount = cb.readFile(fname);

        System.out.println("Caclulate IDF");
        cb.calcIDF(linecount);
        System.out.println("Caclulate TFIDF");
        cb.calcTFIDF();

        //System.out.println("Hi, I'm a chatbot, tell/ask me something");
        boolean done = false;
        while (!done) {
            Console c = System.console();
            if (c == null) {
                System.err.println("No console.");
                System.exit(1);
            }
            String input = c.readLine("> ");
            //boolean question = input.trim().endsWith("?");
            System.out.println(cb.matchInput(input,10));
        }
    }

    /** *************************************************************
     * Run a series of tests containing a filename,
     * a query and an expected answer.
     */
    private static void staticTest() {
        
        ArrayList<String> input = new ArrayList<String>();
        input.add("I eat an apple.");
        input.add("People have an apple.");
        input.add("People will eat.");
        TFIDF cb = null;
        cb = new TFIDF(input);
    }

    /** *************************************************************
     * Run a series of tests containing a filename,
     * a query and an expected answer.
     */
    public static void main(String[] args) {
        
        if (args != null && args.length > 0 && args[0].equals("-h")) {
            System.out.println("Usage: ");
            System.out.println("TFIDF -h         % show this help info");
            System.out.println("      -f fname   % use a particular input file");
        }
        else if (args != null && args.length > 1 && args[0].equals("-f")) {
            asResource = false;
            run(args[1]);
        }
        else
            run();
    }
}
 