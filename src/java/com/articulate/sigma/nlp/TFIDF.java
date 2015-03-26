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

import com.articulate.sigma.utils.ProgressPrinter;
import com.google.common.io.Resources;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TFIDF {

      // inverse document frequency = log of number of documents divided by 
      // number of documents in which a term appears
    private HashMap<String,Float> idf = new HashMap<String,Float>();

      // number of documents in which a term appears
    private HashMap<String,Integer> docfreq = new HashMap<String,Integer>();

      // the length of a vector composed from each term frequency
    private HashMap<Integer,Float> euclid = new HashMap<Integer,Float>();

      // number of times a term appears in a document (where each document is an Integer index)
    private HashMap<Integer,HashMap<String,Integer>> tf = new HashMap<Integer,HashMap<String,Integer>>();

      // tf * idf (where each document is an Integer index)
    private HashMap<Integer,HashMap<String,Float>> tfidf = new HashMap<Integer,HashMap<String,Float>>();

    /** English "stop words" such as "a", "at", "them", which have no or little
     * inherent meaning when taken alone. */
    private ArrayList<String> stopwords = new ArrayList<String>();

      // each line of a corpus
    public ArrayList<String> lines = new ArrayList<String>();
    
      // when true, indicates that responses should be the line after the matched line
    private boolean alternating = false;

    private static boolean asResource = false; // use JUnit resource path for input file

      // similarity of each document to the query (index -1)
    private HashMap<Integer,Float> docSim = new HashMap<Integer,Float>();

    private Random rand = new Random();

    /** ***************************************************************
     */
    public TFIDF(String stopwordsFilename) {
        
        //System.out.println("Info in TFIDF(): Initializing");
        readStopWords(stopwordsFilename);
    }

    /** ***************************************************************
     */
    public TFIDF(List<String> documents, String stopwordsFilename) {
        
        //System.out.println("Info in TFIDF(): Initializing");
        prepare(documents, stopwordsFilename);
    }

    /** ***************************************************************
     */
    public TFIDF(String filename, String stopwordsFilename) {
        
        //System.out.println("Info in TFIDF(): Initializing");
        try {
            //List<String> documents = TextFileUtil.readFile(filename, false);
            List<String> documents = TextFileUtil.readLines(filename, false);
            prepare(documents, stopwordsFilename);
        } 
        catch (IOException e) {
            System.out.println("Unable to read: " + filename);
        }
    }

    /** ***************************************************************
     */
    public void prepare(List<String> documents, String stopwordsFilename) {
        
        rand.setSeed(18021918); // Makes test results consistent
        readStopWords(stopwordsFilename);
        readDocuments(documents);
        calcIDF(documents.size());
        calcTFIDF();
    }

    /** ***************************************************************
     * Process a document
     * @param documents - list of strings to be processed
     */
    private void readDocuments(List<String> documents) {
        
        int count = 0;
        for (String doc : documents) {
            lines.add(doc);
            processDoc(doc, count);
            count++;
        }
    }

    /** ***************************************************************
     * Remove punctuation and contractions from a sentence. 
     * @return the sentence in a String minus these elements.
     */
    private String removePunctuation(String sentence) {

        Matcher m = null;
        if (isNullOrEmpty(sentence))
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

        if (isNullOrEmpty(sentence))
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

        if (isNullOrEmpty(word))
            return false;
        if (stopwords.contains(word.trim().toLowerCase())) 
            return true;
        return false;
    }

   /** ***************************************************************
     * Read a file of stopwords into the variable 
     * ArrayList<String> stopwords
     */
    private void readStopWords(String stopwordsFilename) {

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

        if (isNullOrEmpty(st)) {
            System.out.println("Error in TFIDF.splitToArrayList(): empty string input");
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
            float f = (float) Math.log10((float) docCount / (float) docfreq.get(token));
            //System.out.println("token: " + token + ", docCount: " + docCount + ", docFreq: " + docfreq.get(token) + ", idf: " + f);
            idf.put(token,new Float(f));
        }
        //System.out.println("Info in TFIDF.calcIDF(): " + idf);
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
        //System.out.println("Info in TFIDF.calcOneTFIDF(): index: " + int1);
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
        //System.out.println("Info in TFIDF.calcOneTFIDF():euclid: " + euclid);
        //System.out.println("Info in TFIDF.calcOneTFIDF():TF/IDF: " + tfidf);
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
        //System.out.println("Info in TFIDF.calcTFIDF(): TF/IDF: " + tfidf);
    }

    /** ***************************************************************
     * sets the values in tf (term frequency) and tdocfreq (count of
     * documents in which a term appears)
     *
     * @param intlineCount is -1 for query
     */
    private void processDoc(String doc, Integer intlineCount) {

        if (isNullOrEmpty(doc)) 
            return;
        String line = removePunctuation(doc);
        line = removeStopWords(line);    
        if (isNullOrEmpty(line.trim())) 
            return;
        ArrayList<String> tokens = splitToArrayList(line.trim());
        //System.out.println("Info in TFIDF.ProcessDoc(): " + tokens);
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
        //System.out.println("Info in TFIDF.ProcessDoc(): adding for doc# " + intlineCount + "\n freq: " + docfreq);
        tf.put(intlineCount,tdocfreq);
        //System.out.println("Info in TFIDF.ProcessDoc(): tf: " + tf);
    }

    /** ***************************************************************
     */
    public void newLine(String line) {
        
        prepareLine(line);
        calcDFs();
    }

    /** ***************************************************************
     */
    protected void prepareLine(String line) {
        
        if (!isNullOrEmpty(line)) {
            int newLineIndex = lines.size();
            lines.add(line);
            //System.out.println(line);
            processDoc(line, newLineIndex);
        }
    }

    /** ***************************************************************
     */
    protected void calcDFs() {
        
        //System.out.println("Info in TFIDF.calcDFs(): Caclulate IDF, with size: " + lines.size());
        calcIDF(lines.size() - 1);
        calcTFIDF();
    }

    /** ***************************************************************
    * Read a file from @param fname and store it in the 
    * ArrayList<String> lines member variable.
    * @return an int number of lines
     */
    private void readFile(String fname) {
        
        String line = "";
        BufferedReader omcs = null;
        try {
            String filename = fname;
            if (asResource) {
                URL fileURL = Resources.getResource(fname);
                filename = fileURL.getPath();
            }
            omcs = new BufferedReader(new FileReader(filename));
            /* readLine is a bit quirky :
             * it returns the content of a line MINUS the newline.
             * it returns null only for the END of the stream.
             * it returns an empty String if two newlines appear in a row. */
            ProgressPrinter pp = new ProgressPrinter(1000);
            while ((line = omcs.readLine()) != null) {
                pp.tick();
                prepareLine(line);
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

        calcDFs();
    }

    /** ***************************************************************
     * Assume that query is file index -1
     * Calculate the similarity of each document to the query 
     * Put the result in HashMap<Integer,Float> docSim
     */
    private void calcDocSim() {

        //System.out.println("Info in TFIDF.calcDocSim(): tfidf: " + tfidf);
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
        //System.out.println("Info in TFIDF.calcDocSim(): normquery: " + normquery);
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
        //System.out.println("Info in TFIDF.calcDocSim(): Doc sim:\n" + docSim);
    }

    /** *************************************************************
     * add a new document to the set
     */
    public void addInput(String input) {
        
        //System.out.println("Info in TFIDF.addInput(): " + input);
        //System.out.println("Info in TFIDF.addInput(): size: " + lines.size());
        //System.out.println("Info in TFIDF.addInput(): idf: " + idf);
        //System.out.println("Info in TFIDF.addInput(): tfidf: " + tfidf);
        if (!lines.contains(input))
            lines.add(input);
        int linecount = lines.size();
        processDoc(input,linecount-1);
        //System.out.println("Info in TFIDF.addInput(): size: " + lines.size());
        calcIDF(linecount);
        //System.out.println("Info in TFIDF.addInput(): idf: " + idf);
        calcTFIDF();
        //System.out.println("Info in TFIDF.addInput(): tfidf: " + tfidf);
    }
    
    /** *************************************************************
     * @return a list of matches ranked by relevance to the input.
     * If there is a cluster of top matches, return all elements of
     * the cluster.  If no answer has a good match, return
     * "I don't know".  Iterate the number of clusters until the top
     * cluster is no more than 3.
     */
    public ArrayList<String> matchBestInput(String input) {
        
        ArrayList<String> result = new ArrayList<String>();
        TreeMap<Float,ArrayList<Integer>> sortedSim = matchInputFull(input);
        if (sortedSim == null || sortedSim.keySet() == null || 
                sortedSim.keySet().size() < 1 || sortedSim.lastKey() < .1) {
            result.add("I don't know");
            return result;
        }
        Object[] floats = sortedSim.keySet().toArray();
        int numClusters = 3;
        if (floats.length < numClusters)
            numClusters = floats.length;
        float[] floatarray = new float[floats.length];
        for (int i = 0; i < floats.length; i++)
            floatarray[i] = (float) floats[i];
        ArrayList<ArrayList<Float>> res = KMeans.run(floatarray.length, floatarray, numClusters);
        ArrayList<Float> topCluster = res.get(res.size() - 2);
        while (res.get(res.size() - 2).size() > 3 && numClusters < floats.length) {
            numClusters++;
            res = KMeans.run(floatarray.length, floatarray, numClusters);
            topCluster = res.get(res.size() - 2);
            //System.out.println("Info in TFIDF.matchBestInput(): " + res);
            //System.out.println("Info in TFIDF.matchBestInput(): " + topCluster);
        }
        for (int i = 0; i < topCluster.size(); i++) {
            ArrayList<Integer> temp = sortedSim.get(topCluster.get(i));
            for (int j = 0; j < temp.size(); j++)
                result.add(lines.get(temp.get(j).intValue()));
        }
        return result;
    }
    
    /** *************************************************************
     */
    public String matchInput(String input) {
        
        return matchInput(input,1).get(0);        
    }
    
    /** *************************************************************
     * @return a list of matches ranked by relevance to the input.
     */
    protected TreeMap<Float,ArrayList<Integer>> matchInputFull(String input) {

        //System.out.println("Info in TFIDF.matchInputFull(): input: " + input);
        //System.out.println("Info in TFIDF.matchInputFull(): lines: " + lines);
        ArrayList<String> result = new ArrayList<String>();
        if (isNullOrEmpty(input))
            System.exit(0);
        Integer negone = new Integer(-1);
        processDoc(input,negone);
        calcIDF(lines.size()+1);
        calcOneTFIDF(negone);
        calcDocSim();
        TreeMap<Float,ArrayList<Integer>> sortedSim = new TreeMap<Float,ArrayList<Integer>>();
        if (docSim == null)
            return sortedSim;
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
        return sortedSim;
    }

    /** *************************************************************
     * @return a list of strings which are the top n best guess matches to the input.
     * If global variable alternating is set to true, the return the
     * next line in the input file, which is therefore treated like a
     * dialog in which the best response to a given input is the line
     * after the line in the dialog that matches.  If there's more than
     * one reasonable response, pick a random one.
     */
    public List<String> matchInput(String input, int n) {

        //System.out.println("Info in TFIDF.matchInput(): " + input);
        //System.out.println("Info in TFIDF.matchInput(): " + lines);
        ArrayList<String> result = new ArrayList<String>();
        TreeMap<Float,ArrayList<Integer>> sortedSim = matchInputFull(input);
        //System.out.println("Info in TFIDF.matchInput(): " + sortedSim);
        if (sortedSim == null || sortedSim.keySet() == null || 
                sortedSim.keySet().size() < 1 || sortedSim.lastKey() < .1) {
            result.add("I don't know");
            return result;
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
                result.add(lines.get(new Integer(index.intValue())));
            else
                result.add(f + ":" + lines.get(new Integer(index.intValue()+1)));
        }        
        //System.out.println("Info in TFIDF.matchInput(): result: " + result);
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

        List<String> documents = null;
        try {
            if (asResource)
                documents = TextFileUtil.readLines(fname, false);
            //documents = TextFileUtil.readFile(fname, false);
        } 
        catch (IOException e) {
            System.out.println("Couldn't read document: " + fname + ". Exiting");
            return;
        }
        TFIDF cb;
        if (asResource)
            cb = new TFIDF(documents, "testfiles/stopwords.txt");
        else {
            cb = new TFIDF("testfiles/stopwords.txt");
            cb.readFile(fname);
        }

        System.out.println("Hi, I'm a chatbot, tell/ask me something");
        boolean done = false;
        while (!done) {
            Console c = System.console();
            if (c == null) {
                System.err.println("No console.");
                System.exit(1);
            }
            String input = c.readLine("> ");
            //boolean question = input.trim().endsWith("?");
            //System.out.println(cb.matchInput(input,10));
            System.out.println(cb.matchBestInput(input));
        }
    }

    /** *************************************************************
     * Run a series of tests containing a filename,
     * a query and an expected answer.
     */
    private static void staticTest() {
        
        List<String> input = new ArrayList<String>();
        /*input.add("I eat an apple.");
        input.add("People have an apple.");
        input.add("People will eat.");
        TFIDF cb = new TFIDF(input, "testfiles/stopwords.txt");*/
        
        /*input = new ArrayList<String>();
        input.add("John kicks the cart.");
        input.add("Mary pushes the wagon.");
        TFIDF cb = new TFIDF(input, "testfiles/stopwords.txt");
        cb.matchInput("Who kicks the cart?");*/
        
        TFIDF cb = new TFIDF("testfiles/stopwords.txt");
        cb.addInput("John kicks the cart.");
        cb.addInput("Mary pushes the wagon.");
        cb.matchInput("Who kicks the cart?");
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
            staticTest();
    }
}
 