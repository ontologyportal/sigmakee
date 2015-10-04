package com.articulate.sigma.nlp.corpora;

import com.articulate.sigma.StringUtil;
import com.articulate.sigma.nlp.NGramOverlap;
import com.articulate.sigma.nlp.NaiveBayes;
import com.articulate.sigma.nlp.TFIDF;
import com.articulate.sigma.nlp.TokenOverlap;
import com.articulate.sigma.utils.ProgressPrinter;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by apease on 9/4/15.
 * read data from https://github.com/shuzi/insuranceQA as per
 * http://arxiv.org/pdf/1508.01585.pdf
 */
public class ShuZiInsQA {

    Map<String,String> vocab = new HashMap<>();
    ArrayList<String> answers = new ArrayList<>();
    ArrayList<HashMap<Integer,HashSet<String>>> answerNgrams = new ArrayList<>(); // HashMap key is N
    Map<String,List<String>> training = new HashMap<>();

    // map of answer ID to answer line
    Map<Integer,Integer> idToLine = new HashMap<>();

    public class Dev {
        public String question = "";
        public HashMap<Integer,HashSet<String>> questionNgrams = new HashMap<>();
        public List<String> answersID = new ArrayList<>();

        // note that this is a partial selected set of 500, rather than the
        // full 24,000+
        public List<String> wrongAnswerIDs = new ArrayList<>();

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(question + "\n");
            if (answersID.size() > 0)
                sb.append(StringUtil.getFirstNChars(answers.get(Integer.parseInt(answersID.get(0))),40));
            return sb.toString();
        }
    }

    //List<Dev> devs = new ArrayList<>();

    /****************************************************************
     * vocabulary
     * <word index><TAB><original word>
     */
    private List<String> readLines(String filename) {

        List<String> lines = new ArrayList<String>();
        //System.out.println("INFO in ShuZiInsQA.readLines(): Reading files");
        LineNumberReader lr = null;
        try {
            String line;
            File nounFile = new File(filename);
            if (nounFile == null) {
                System.out.println("Error in readLines(): The file '" + filename + "' does not exist ");
                return lines;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(nounFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                lines.add(line);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (lr != null) {
                    lr.close();
                }
            }
            catch (Exception ex) {
            }
        }
        return lines;
    }

    /****************************************************************
     * vocabulary
     * <word index><TAB><original word>
     * 400KB file
     */
    private void readVocab() {

        List<String> lines = readLines("/home/apease/IPsoft/insuranceQA-master/vocabulary-mod");
        for (String l : lines) {
            String[] elements = l.split("\\t");
            if (elements.length == 2) {
                vocab.put(elements[0], elements[1]);
                //System.out.println(elements[0] + "," + elements[1]);
            }
        }
    }

    /****************************************************************
     */
    private HashMap<Integer,HashSet<String>> addNgrams(TFIDF tfidf, String s) {

        HashMap<Integer,HashSet<String>> result = new HashMap<>();
        int nMax = 3;
        String str1 = tfidf.removePunctuation(s);
        str1 = tfidf.removeStopWords(str1);
        ArrayList<String> s1 = new ArrayList<String>();
        String[] sspl = str1.split(" ");
        s1.addAll(Arrays.asList(sspl));

        for (int n = 2; n < nMax; n++) {
            HashSet<String> ngrams = new HashSet<>();
            for (int i = 0; i < s1.size() - n; i++) {
                StringBuffer s1tok = new StringBuffer();
                for (int z = 0; z < n; z++)
                    s1tok.append(s1.get(i + z));
                ngrams.add(s1tok.toString());
            }
            result.put(n,ngrams);
        }
        return result;
    }

    /****************************************************************
     * <answer label><TAB><answer text in word index form>
     * To get the word of from its index idx_* ,  please use the file vocabulary
     * 22MB file.  We rely strictly on that the line number of an answer is
     * its ID.
     */
    private void readAnswers() {

        TFIDF cb = null;
        try {
            cb = new TFIDF("/home/apease/Sigma/KBs/stopwords.txt");
        }
        catch (IOException ioe) {
            System.out.println("Error in ShuZiInsQA.readAnswers()");
            ioe.printStackTrace();
        }
        answers.add(""); // make 0th element blank
        answerNgrams.add(new HashMap<>());
        int linenum = 1;
        List<String> lines = readLines("/home/apease/IPsoft/insuranceQA-master/answers.label.token_idx");
        for (String l : lines) {
            String[] elements = l.split("\\t");
            String answerID = elements[0];
            String sentence = elements[1];
            String[] words = sentence.split(" ");
            StringBuffer sent = new StringBuffer();
            for (String id : words) {
                String word = vocab.get(id);
                sent = sent.append(word + " ");
            }
            sent.deleteCharAt(sent.length() - 1);
            //if (answerID.equals("8362"))
            //    System.out.println("readAnswers(): line " + linenum + " " + answerID + " " + sent);
            answers.add(sent.toString());
            answerNgrams.add(addNgrams(cb, sent.toString()));
            if (!answerID.equals(Integer.toString(linenum)))
                System.out.println("Error in readAnswers(): no match for line and ID " + linenum + " " + answerID);
            idToLine.put(Integer.parseInt(answerID),linenum);
            linenum++;
            //System.out.println(answerID + ":" + sent);
        }
        //System.out.println(answers);
    }

    /****************************************************************
     * <question text in word index form><TAB><answer labels>
     * To get the word of from its index idx_* ,  please use the file vocabulary
     * 1MB file
     */
    private void readTrainingQuestions() {

        List<String> lines = readLines("/home/apease/IPsoft/insuranceQA-master/question.train.token_idx.label");
        for (String l : lines) {
            String[] elements = l.split("\\t");
            String answer = elements[1];
            String[] answerIDs = answer.split(" ");
            String sentence = elements[0];
            String[] words = sentence.split(" ");
            StringBuffer question = new StringBuffer();
            for (String id : words) {
                String word = vocab.get(id);
                question = question.append(word + " ");
            }
            question.deleteCharAt(question.length() - 1);
            ArrayList<String> answerSet = new ArrayList<>();
            for (String s : answerIDs) {
                answerSet.add(answers.get(Integer.parseInt(s)) + "\n");
            }
            training.put(question.toString(), answerSet);
            //System.out.println(question + ":" + answerSet);
        }
        //System.out.println(answers);
    }

    /****************************************************************
     * question.(dev|test1|test2).label.token_idx.pool:
     * <ground truth labels><TAB><question text in word index form><TAB><answer candidate pool>
     * To get the word of from its index idx_* ,  please use the file vocabulary
     * Notice we make an answer candidate pool with size 500 here for dev, test1 and test2.
     * If running time is not a problem for your application, you are surely encouraged to use
     * the whole answer set as the pool (label 1-24981)
     */
    private List<Dev> readDevTestQuestions() {

        List<Dev> result = new ArrayList<>();
        TFIDF cb = null;
        try {
            cb = new TFIDF("/home/apease/Sigma/KBs/stopwords.txt");
        }
        catch (IOException ioe) {
            System.out.println("Error in ShuZiInsQA.readAnswers()");
            ioe.printStackTrace();
        }
        List<String> lines = readLines("/home/apease/IPsoft/insuranceQA-master/question.dev.label.token_idx.pool");
        for (String l : lines) {
            if (StringUtil.emptyString(l)) continue;
            String[] elements = l.split("\\t");
            String answersAr = elements[0];
            String[] rightAnswers = answersAr.split(" ");
            String sentence = elements[1];
            String[] words = sentence.split(" ");
            String wrongAnswers = elements[2];
            String[] wrongAnswerIDsAr = wrongAnswers.split(" ");

            StringBuffer question = new StringBuffer();
            for (String id : words) {
                String word = vocab.get(id);
                question = question.append(word + " ");
            }
            question.deleteCharAt(question.length() - 1);

            Dev dev = new Dev();
            dev.question = question.toString();
            dev.questionNgrams = addNgrams(cb, question.toString());
            dev.answersID = (ArrayList<String>) Arrays.stream(rightAnswers).collect(Collectors.toList());//(ArrayList<String>) Arrays.asList(rightAnswers);

            //System.out.println("ShuZiInsQA.readDevTestQuestions(): " + dev.answersID);
            for (String s : wrongAnswerIDsAr) {
                if (!dev.answersID.contains(s))
                    dev.wrongAnswerIDs.add(s);
            }
            result.add(dev);
        }
        return result;
    }

    /****************************************************************
     * question.(dev|test1|test2).label.token_idx.pool:
     * <ground truth labels><TAB><question text in word index form><TAB><answer candidate pool>
     * To get the word of from its index idx_* ,  please use the file vocabulary
     * Notice we make an answer candidate pool with size 500 here for dev, test1 and test2.
     * If running time is not a problem for your application, you are surely encouraged to use
     * the whole answer set as the pool (label 1-24981)
     */
    private ArrayList<Dev> readTestQuestionFile(String filename) {

        TFIDF cb = null;
        try {
            cb = new TFIDF("/home/apease/Sigma/KBs/stopwords.txt");
        }
        catch (IOException ioe) {
            System.out.println("Error in ShuZiInsQA.readAnswers()");
            ioe.printStackTrace();
        }
        System.out.println("ShuZiInsQA.readTestQuestionFile(): " + filename);
        ArrayList<Dev> test = new ArrayList<>();
        List<String> lines = readLines(filename);
        for (String l : lines) {
            if (StringUtil.emptyString(l)) continue;
            String[] elements = l.split("\\t");
            String answersAr = elements[0];
            String[] rightAnswers = answersAr.split(" ");
            String sentence = elements[1];
            String[] words = sentence.split(" ");
            String wrongAnswers = elements[2];
            String[] wrongAnswerIDsAr = wrongAnswers.split(" ");

            StringBuffer question = new StringBuffer();
            for (String id : words) {
                String word = vocab.get(id);
                question = question.append(word + " ");
            }
            question.deleteCharAt(question.length() - 1);

            Dev dev = new Dev();
            dev.question = question.toString();
            dev.questionNgrams = addNgrams(cb, question.toString());
            dev.answersID = (ArrayList<String>) Arrays.stream(rightAnswers).collect(Collectors.toList());//(ArrayList<String>) Arrays.asList(rightAnswers);

            //System.out.println("ShuZiInsQA.readDevTestQuestions(): " + dev.answersID);
            for (String s : wrongAnswerIDsAr) {
                if (!dev.answersID.contains(s))
                    dev.wrongAnswerIDs.add(s);
            }
            test.add(dev);
        }
        return test;
    }

    /****************************************************************
     */
    private ArrayList<Dev> readTestQuestionOneFile() {

        ArrayList<Dev> test = readTestQuestionFile("/home/apease/IPsoft/insuranceQA-master/question.dev.label.token_idx.pool");
        return test;
    }

    /****************************************************************
     * @param ansID is the ID of the answer
     */
    private int scoreCandidate(TreeMap<Float,ArrayList<Integer>> candidates, String ansID) {

        int ansInt = idToLine.get(Integer.parseInt(ansID));
        //System.out.println("ShuZiInsQA.scoreCandidates(): searching for answer id: " + ansInt);
        int result = 0;
        ArrayList<Float> fAr = new ArrayList<>();
        fAr.addAll(candidates.descendingKeySet());  // search from high to low match scores
        int index = 0;
        boolean found = false;
        while (index < fAr.size() && index < 10 && !found) {
            //System.out.println("ShuZiInsQA.scoreCandidates(): score: " + fAr.get(index));
            //if (fAr.get(index) > 1)
               // System.out.println("ShuZiInsQA.scoreCandidates(): " + StringUtil.getFirstNChars(candidates.get(fAr.get(index)).toString(),100));
            //    System.out.println("ShuZiInsQA.scoreCandidates(): " + candidates.get(fAr.get(index)));
            if (candidates.get(fAr.get(index)).contains(ansInt)) {
                found = true;
                //System.out.println("ShuZiInsQA.scoreCandidates(): index of sentence: " + index);
            }
            else
                index++;
        }
        if (!found)
            result = -1;
        else
            result = (int) (10.0 - (index / (candidates.keySet().size() / 10.0)));
        return result;
    }

    /****************************************************************
     * @return a set of scores where each array element is an array of
     * three integers - a TFIDF rank for the candidate answer, a
     * term overlap rank, and a binary value for whether the answer
     * is in fact a valid answer
     */
    private ArrayList<ArrayList<Integer>> scoreOneDev(Dev dev, TFIDF cb, TokenOverlap to) {

        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        //System.out.println("ShuZiInsQA.scoreOneDev(): " + dev);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> tfidfCandidates = new TreeMap<>();
        tfidfCandidates = cb.rank(dev.question,dev.answersID,tfidfCandidates);
        tfidfCandidates = cb.rank(dev.question,dev.wrongAnswerIDs,tfidfCandidates);

        //System.out.println("ShuZiInsQA.devsToInputs(): tfidf: " + tfidfCandidates);
        //System.out.println("ShuZiInsQA.devsToInputs(): match with overlap: " + dev.question);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> overlapCandidates = new TreeMap<>();
        overlapCandidates = to.rank(dev.question,dev.answersID,overlapCandidates);
        //System.out.println("ShuZiInsQA.scoreOneDev(): overlap: " + overlapCandidates);
        overlapCandidates = to.rank(dev.question,dev.wrongAnswerIDs,overlapCandidates);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> nGramCandidates = new TreeMap<>();
        //to.debug = true;
        nGramCandidates = to.nGramRank(dev, dev.answersID, answerNgrams, nGramCandidates);
        //System.out.println("ShuZiInsQA.scoreOneDev(): ngrams: " + nGramCandidates);
        //System.out.println("ShuZiInsQA.scoreOneDev(): answers: " + dev.answersID);
        //to.debug = false;
        nGramCandidates = to.nGramRank(dev, dev.wrongAnswerIDs, answerNgrams, nGramCandidates);

        //System.out.println("ShuZiInsQA.devsToInputs(): overlap:  " + overlapCandidates);
        //System.out.println("ShuZiInsQA.devsToInputs(): question:  " + dev.question);
        for (String ansID : dev.answersID) {
            //System.out.println("ShuZiInsQA.devsToInputs(): answer candidate: [" + ansID + "]: " + answers.get(Integer.parseInt(ansID)));
            //System.out.println(cb.lines.indexOf(answers.get(Integer.parseInt(ansID))));
            //System.out.println("ShuZiInsQA.devsToInputs(): scoring for TFIDF");
            int tfScore = scoreCandidate(tfidfCandidates,ansID);
            //System.out.println("ShuZiInsQA.devsToInputs(): score for TFIDF: " + tfScore);
            //System.out.println("ShuZiInsQA.devsToInputs(): scoring for term overlap");
            int toScore = scoreCandidate(overlapCandidates,ansID);
            int ngScore = scoreCandidate(nGramCandidates,ansID);

            //System.out.println("ShuZiInsQA.scoreOneDev(): score for ngram overlap: " + ngScore);
            ArrayList<Integer> inputLine = new ArrayList<>();
            inputLine.add(tfScore);
            inputLine.add(toScore);
            inputLine.add(ngScore);
            inputLine.add(1); // a correct answer
            result.add(inputLine);
            //System.out.println(StringUtil.removeEnclosingCharPair(inputLine.toString(), 1, '[', ']'));
        }
        Iterator<String> it = dev.wrongAnswerIDs.iterator();
        int count = 0;
        while (it.hasNext() && count < 2) {
            count++;
            String ansID = it.next();
            //System.out.println("ShuZiInsQA.devsToInputs(): non-answer candidate: " + answers.get(ansID));
            int tfScore = scoreCandidate(tfidfCandidates,ansID);
            int toScore = scoreCandidate(overlapCandidates,ansID);
            int ngScore = scoreCandidate(nGramCandidates,ansID);
            ArrayList<Integer> inputLine = new ArrayList<>();
            inputLine.add(tfScore);
            inputLine.add(toScore);
            inputLine.add(ngScore);
            inputLine.add(0); // an incorrect answer
            result.add(inputLine);
            //System.out.println(StringUtil.removeEnclosingCharPair(inputLine.toString(), 1, '[', ']'));
        }
        return result;
    }

    /****************************************************************
     * Create a table of scores to train from.  For each test question
     * score the correct answer(s) and negative answers with TFIDF and
     * term overlap and add to the table
     */
    private ArrayList<ArrayList<Integer>> devsToInputs(List<Dev> devs, TFIDF cb, TokenOverlap to) {

        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        ProgressPrinter pp = new ProgressPrinter(10);
        for (Dev dev : devs) {
            pp.tick();
            result.addAll(scoreOneDev(dev, cb, to));
        }
        System.out.println();
        return result;
    }

    /****************************************************************
     */
    private ArrayList<ArrayList<String>> matrixIntegerToString(ArrayList<ArrayList<Integer>> input) {

        ArrayList<ArrayList<String>> result = new ArrayList<>();
        for (ArrayList<Integer> row : input) {
            ArrayList<String> resultRow = new ArrayList<String>();
            for (Integer i : row)
                resultRow.add(Integer.toString(i));
            result.add(resultRow);
        }
        return result;
    }

    /****************************************************************
     */
    private NaiveBayes createTrainingClasses(List<Dev> devs, TFIDF cb, TokenOverlap to, NGramOverlap ng) {

        ArrayList<ArrayList<String>> inputs = new ArrayList<>();
        // add types header and labels header
        inputs.add(Lists.newArrayList("cont", "cont", "cont", "class"));
        inputs.add(Lists.newArrayList("tfidf", "overlap", "bigram", "answer"));
        inputs.addAll(matrixIntegerToString(devsToInputs(devs, cb, to)));
        NaiveBayes nb = new NaiveBayes(inputs);
        nb.initialize();
        return nb;
    }

    /****************************************************************
     */
    private NaiveBayes createTrainingClasses2(List<Dev> devs, TFIDF cb, TokenOverlap to) {

        ArrayList<ArrayList<String>> inputs = new ArrayList<>();
        // add types header and labels header
        inputs.add(Lists.newArrayList("cont", "cont", "class"));
        inputs.add(Lists.newArrayList("tfidf", "overlap", "answer"));
        ArrayList<ArrayList<String>> body = matrixIntegerToString(devsToInputs(devs, cb, to));
        body.stream()
                .forEach(a -> a.remove(2));
        inputs.addAll(body);
        NaiveBayes nb = new NaiveBayes(inputs);
        nb.initialize();
        return nb;
    }

    /****************************************************************
     * get the results on the test set for TFIDF and term overlap, then
     * classify it with naive bayes, then compare that classification to
     * the actual classification.
     * @return a list of scores for each question and candidate answer.
     * we get a rank for TFIDF and term overlap followed by a binary
     * answer for naive bayes and lastly the binary classification of
     * whether the sentence really is the answer.
     */
    private ArrayList<ArrayList<Integer>> classify(ArrayList<Dev> test, NaiveBayes nb, NaiveBayes nb2, TFIDF cb, TokenOverlap to) {

        //System.out.print("ShuZiInsQA.classify()");
        ProgressPrinter pp = new ProgressPrinter(10);
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        for (Dev dev : test) {
            //System.out.println("ShuZiInsQA.classify(): dev: " + dev);
            pp.tick();
            ArrayList<ArrayList<Integer>> oneDev = scoreOneDev(dev, cb, to);
            ArrayList<ArrayList<String>> processedDev = matrixIntegerToString(oneDev);
            for (ArrayList<String> line : processedDev) {
                String clss = nb.classify(line);
                ArrayList<String> shortened = new ArrayList<>(line);
                shortened.remove(line.size()-2);  // remove the ngram classification
                //System.out.println("ShuZiInsQA.classify(): " + line);
                //System.out.println("ShuZiInsQA.classify(): " + shortened);
                String clssShort = nb2.classify(shortened);
                ArrayList<Integer> oneLine = new ArrayList<>();
                oneLine.add(Integer.parseInt(line.get(0))); // tfidf
                oneLine.add(Integer.parseInt(line.get(1))); // term overlap
                oneLine.add(Integer.parseInt(line.get(2))); // bigram overlap
                oneLine.add(Integer.parseInt(clss));        // naive bayes combined
                oneLine.add(Integer.parseInt(clssShort));   // naive bayes just over tfidf&overlap
                oneLine.add(Integer.parseInt(line.get(3))); // 1=answer 0=not the answer
                result.add(oneLine);
            }
        }
        System.out.println();
        return result;
    }

    /****************************************************************
     * score whether each method gets the right answer.  For TFIDF
     * and Term Overlap, a right answer must have a value of 10
     * @return a list of three floats which are the percentage
     * correct for each of the three algorithms
     */
    private void score(ArrayList<ArrayList<Integer>> classifications, int classIndex) {

        ArrayList<Float> scoresPositive = new ArrayList<Float>();
        ArrayList<Float> scoresNegative = new ArrayList<Float>();
        int numPos = 0;
        int numNeg = 0;
        for (int i = 0; i < classIndex; i++) {
            scoresPositive.add(new Float(0));
            scoresNegative.add(new Float(0));
        }
        //System.out.println("ShuZiInsQA.score(): classes: tfidf, term overlap, bigram overlap, naive bayes combined, 1=answer 0=not the answer");
        for (ArrayList<Integer> classes : classifications) {
            //System.out.println("ShuZiInsQA.score(): classes: " + classes);
            if (classes.size() == classIndex + 1) {
                if (classes.get(classIndex) == 1) { // a correct answer
                    numPos++;
                    if (classes.get(0) == 10)  // overlap
                        scoresPositive.set(0, scoresPositive.get(0) + 1);
                    if (classes.get(1) == 10) // tfidf
                        scoresPositive.set(1, scoresPositive.get(1) + 1);
                    if (classes.get(2) == 10)  // bigram
                        scoresPositive.set(2, scoresPositive.get(2) + 1);
                    if (classes.get(3) == 1)  // NB
                        scoresPositive.set(3, scoresPositive.get(3) + 1);
                    if (classes.get(4) == 1)  // NBshort
                        scoresPositive.set(4, scoresPositive.get(4) + 1);
                }
                if (classes.get(classIndex) == 0) { // a wrong answer
                    numNeg++;
                    if (classes.get(0) == -1)  // overlap
                        scoresNegative.set(0, scoresNegative.get(0) + 1);
                    if (classes.get(1) == -1) // tfidf
                        scoresNegative.set(1, scoresNegative.get(1) + 1);
                    if (classes.get(2) == -1)  // bigram
                        scoresNegative.set(2, scoresNegative.get(2) + 1);
                    if (classes.get(3) == 0)  // NB
                        scoresNegative.set(3, scoresNegative.get(3) + 1);
                    if (classes.get(4) == 0)  // NBshort
                        scoresNegative.set(4, scoresNegative.get(4) + 1);
                }
            }
            //System.out.println("ShuZiInsQA.score(): positive scores: " + scoresPositive);
            //System.out.println("ShuZiInsQA.score(): negative scores: " + scoresNegative);
        }
        for (int i = 0; i < classIndex; i++) {
            scoresPositive.set(i, scoresPositive.get(i) / numPos);
            scoresNegative.set(i, scoresNegative.get(i) / numNeg);
        }
        System.out.println("ShuZiInsQA.score(): positive scores: " + scoresPositive);
        System.out.println("ShuZiInsQA.score(): negative scores: " + scoresNegative);
    }

    /** ***************************************************************
     */
    public static void testOverlap() {

        //String s1 = "do Medicare cover my spouse";
        //String s2 = "if your spouse have work and pay Medicare tax for the entire require 40 quarter or be eligible for Medicare by virtue of be disable or some other reason , your spouse can receive his / her own medicare benefit if your spouse have not meet those qualification , if you have meet them and if your spouse be age 65 he / she can receive Medicare based on your eligibility";
        String s1 = "can you borrow against globe Life Insurance";
        String s2 = "borrowing against a life insurance policy require cash value inside that policy term life insurance do not have cash value but whole life insurance policy may so you will need have a whole life policy with global Life Insurance in order to be able borrow against it call up your company and ask if you have any cash value inside your policy and what the borrowing option and cost be";

        ShuZiInsQA sziq = null;
        ArrayList<ArrayList<String>> inputs = new ArrayList<>();
        TFIDF cb = null;
        TokenOverlap to = null;
        try {
            List<String> a = new ArrayList<>();
            sziq = new ShuZiInsQA();
            sziq.readVocab();
            sziq.readAnswers();
            a.addAll(sziq.answers);
            cb = new TFIDF(a,"/home/apease/Sigma/KBs/stopwords.txt");
            to = new TokenOverlap(cb);

        }
        catch (IOException ioe) {
            System.out.println("Error in ShuZiInsQA.devsToInputs()");
            ioe.printStackTrace();
        }
        System.out.println("testOverlap(): overlap: " + to.overlap(s1, s2));
        System.out.println("testOverlap(): mapped to line: " + cb.lines.get(sziq.idToLine.get(8362)));
        System.out.println("testOverlap(): unmapped to line: " + cb.lines.get(8362));
        System.out.println("testOverlap(): index of answer: " + cb.lines.indexOf(s2));
        System.out.println("testOverlap(): last index of answer: " + cb.lines.lastIndexOf(s2));
        System.out.println("testOverlap(): overlap with mapped line: " + to.overlap(s1, cb.lines.get(8362)));
        System.out.println("testOverlap(): index of answer: " + cb.lines.indexOf(sziq.answers.get(Integer.parseInt("8362"))));
    }

    /****************************************************************
     */
    public void run() {

        readVocab();
        readAnswers();
        readTrainingQuestions();
        List<Dev> devs = readDevTestQuestions();
        ArrayList<ArrayList<Integer>> inputs = new ArrayList<>();
        TFIDF cb = null;
        TokenOverlap to = null;
        NGramOverlap ng = null;;
        try {
            List<String> a = new ArrayList<>();
            a.addAll(answers);
            cb = new TFIDF(a,"/home/apease/Sigma/KBs/stopwords.txt");
            to = new TokenOverlap(cb);
            ng = new NGramOverlap(cb);
        }
        catch (IOException ioe) {
            System.out.println("Error in ShuZiInsQA.devsToInputs()");
            ioe.printStackTrace();
        }
        NaiveBayes nb = createTrainingClasses(devs,cb,to,ng);
        NaiveBayes nb2 = createTrainingClasses2(devs, cb, to);
        ArrayList<Dev> test = readTestQuestionOneFile();

        inputs = classify(test,nb,nb2,cb,to);
        score(inputs,5);
    }

    /****************************************************************
     */
    public static void streamTest() {

    }

    /****************************************************************
     */
    public static void main(String[] args) {

        ShuZiInsQA sziq = new ShuZiInsQA();
        sziq.run();
        //testOverlap();
    }
}
