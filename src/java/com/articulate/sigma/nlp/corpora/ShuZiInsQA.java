package com.articulate.sigma.nlp.corpora;

import com.articulate.sigma.StringUtil;
import com.articulate.sigma.nlp.*;
import com.articulate.sigma.utils.ProgressPrinter;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Time;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
    ArrayList<String> resultHeader = null;

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
    private String listAsTable(List<Float> l) {

        DecimalFormat myFormatter = new DecimalFormat("###.##");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < l.size(); i++) {
            if (i != 0)
                sb.append("\t");
            sb.append(myFormatter.format(l.get(i)));
        }
        return sb.toString();
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
    private ArrayList<ArrayList<Integer>> scoreOneDev(Dev dev, TFIDF cb, TokenOverlap to, NGramOverlap ng) {

        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        //System.out.println("ShuZiInsQA.scoreOneDev(): " + dev);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> tfidfCandidates = new TreeMap<>();
        tfidfCandidates = cb.rank(dev.question,dev.answersID,tfidfCandidates);
        tfidfCandidates = cb.rank(dev.question,dev.wrongAnswerIDs,tfidfCandidates);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> overlapCandidates = new TreeMap<>();
        overlapCandidates = to.rank(dev.question,dev.answersID,overlapCandidates);
        overlapCandidates = to.rank(dev.question,dev.wrongAnswerIDs,overlapCandidates);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> nGramCandidatesN2 = new TreeMap<>();
        nGramCandidatesN2 = ng.nGramRank(dev, dev.answersID, answerNgrams, nGramCandidatesN2, 2);
        nGramCandidatesN2 = ng.nGramRank(dev, dev.wrongAnswerIDs, answerNgrams, nGramCandidatesN2, 2);

        // key is the match score and value is the list of line numbers that have that score
        TreeMap<Float,ArrayList<Integer>> nGramCandidatesN3 = new TreeMap<>();
        nGramCandidatesN3 = ng.nGramRank(dev, dev.answersID, answerNgrams, nGramCandidatesN3,3);
        nGramCandidatesN3 = ng.nGramRank(dev, dev.wrongAnswerIDs, answerNgrams, nGramCandidatesN3,3);

        for (String ansID : dev.answersID) {
            int tfScore = scoreCandidate(tfidfCandidates,ansID);
            int toScore = scoreCandidate(overlapCandidates,ansID);
            int ng2Score = scoreCandidate(nGramCandidatesN2,ansID);
            int ng3Score = scoreCandidate(nGramCandidatesN3,ansID);

            //System.out.println("ShuZiInsQA.scoreOneDev(): score for ngram overlap: " + ngScore);
            ArrayList<Integer> inputLine = new ArrayList<>();
            inputLine.add(tfScore);
            inputLine.add(toScore);
            inputLine.add(ng2Score);
            inputLine.add(ng3Score);
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
            int ng2Score = scoreCandidate(nGramCandidatesN2,ansID);
            int ng3Score = scoreCandidate(nGramCandidatesN3,ansID);

            ArrayList<Integer> inputLine = new ArrayList<>();
            inputLine.add(tfScore);
            inputLine.add(toScore);
            inputLine.add(ng2Score);
            inputLine.add(ng3Score);
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
    private ArrayList<ArrayList<Integer>> devsToInputs(List<Dev> devs, TFIDF cb, TokenOverlap to, NGramOverlap ng) {

        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        ProgressPrinter pp = new ProgressPrinter(10);
        for (Dev dev : devs) {
            pp.tick();
            result.addAll(scoreOneDev(dev, cb, to, ng));
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
    private List<NaiveBayes> createTrainingClasses(List<Dev> devs, TFIDF cb, TokenOverlap to, NGramOverlap ng,
                                              ArrayList<ArrayList<String>> inputs) {

        List<NaiveBayes> bayesList = new ArrayList<>();
        ArrayList<ArrayList<String>> newinputs = new ArrayList<>();
        // add types header and labels header
        inputs.addAll(matrixIntegerToString(devsToInputs(devs, cb, to, ng)));
        NaiveBayes nb = new NaiveBayes(inputs);
        nb.initialize();
        bayesList.add(nb);  // full feature set

        inputs.stream()
                .forEach(a -> a.remove(3));
        newinputs.addAll(inputs);
        NaiveBayes nb2 = new NaiveBayes(inputs);
        nb2.initialize();
        bayesList.add(nb2); // no trigrams

        inputs.stream()
                .forEach(a -> a.remove(2));
        newinputs.addAll(inputs);
        NaiveBayes nb3 = new NaiveBayes(inputs);
        nb3.initialize();
        bayesList.add(nb3); // no bigrams

        return bayesList;
    }

    /****************************************************************
     */
    private List<LogisticRegression> createTrainingClassesLR(List<Dev> devs, TFIDF cb, TokenOverlap to, NGramOverlap ng,
                                                   ArrayList<ArrayList<String>> inputs) {

        List<LogisticRegression> lrList = new ArrayList<>();
        ArrayList<ArrayList<String>> newinputs = new ArrayList<>();
        // add types header and labels header
        inputs.addAll(matrixIntegerToString(devsToInputs(devs, cb, to, ng)));
        LogisticRegression lr = new LogisticRegression(inputs);
        lr.init();
        lr.train();
        lrList.add(lr);  // full feature set

        inputs.stream()
                .forEach(a -> a.remove(3));
        newinputs.addAll(inputs);
        LogisticRegression lr2 = new LogisticRegression(inputs);
        lr2.init();
        lr2.train();
        lrList.add(lr2); // no trigrams

        inputs.stream()
                .forEach(a -> a.remove(2));
        newinputs.addAll(inputs);
        LogisticRegression lr3 = new LogisticRegression(inputs);
        lr3.init();
        lr3.train();
        lrList.add(lr3); // no bigrams

        return lrList;
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
    private ArrayList<ArrayList<Integer>> classify(ArrayList<Dev> test,
                                                   List<NaiveBayes> nbList,
                                                   List<LogisticRegression> lrList,
                                                   TFIDF cb, TokenOverlap to,
                                                   NGramOverlap ng) {

        //System.out.print("ShuZiInsQA.classify()");
        ProgressPrinter pp = new ProgressPrinter(10);
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        for (Dev dev : test) {
            //System.out.println("ShuZiInsQA.classify(): dev: " + dev);
            pp.tick();
            ArrayList<ArrayList<Integer>> oneDev = scoreOneDev(dev, cb, to, ng);
            ArrayList<ArrayList<String>> processedDev = matrixIntegerToString(oneDev);
            for (ArrayList<String> line : processedDev) {
                ArrayList<Integer> oneLine = new ArrayList<>();
                oneLine.add(Integer.parseInt(line.get(0))); // tfidf
                oneLine.add(Integer.parseInt(line.get(1))); // term overlap
                oneLine.add(Integer.parseInt(line.get(2))); // bigram overlap
                oneLine.add(Integer.parseInt(line.get(3))); // trigram overlap
                ArrayList<String> shortened = new ArrayList<>(line);
                for (NaiveBayes nb : nbList) {
                    //System.out.println("ShuZiInsQA.classify(): header: " + nb.labels);
                    //System.out.println("ShuZiInsQA.classify(): line: " + shortened);
                    String clss = nb.classify(shortened);
                    shortened.remove(shortened.size() - 2);  // remove the last feature
                    oneLine.add(Integer.parseInt(clss));        // naive bayes combined
                }
                shortened = new ArrayList<>(line);
                for (LogisticRegression lr : lrList) {
                    //System.out.println("ShuZiInsQA.classify(): header: " + nb.labels);
                    //System.out.println("ShuZiInsQA.classify(): line: " + shortened);
                    String clss = lr.classify(shortened);
                    shortened.remove(shortened.size() - 2);  // remove the last feature
                    oneLine.add(Integer.parseInt(clss));        // naive bayes combined
                }
                oneLine.add(Integer.parseInt(line.get(line.size()-1))); // 1=answer 0=not the answer
                result.add(oneLine);
            }
        }
        System.out.println();
        return result;
    }

    /****************************************************************
     */
    private void printMetrics(ArrayList<Float> scoresPositive,
                              ArrayList<Float> scoresNegative,
                              int numPos, int numNeg, List<String> resultHeader,
                              int classIndex) {

        ArrayList<Float> recall = new ArrayList<Float>(scoresNegative);
        ArrayList<Float> precision = new ArrayList<Float>(scoresNegative);
        ArrayList<Float> F1 = new ArrayList<Float>(scoresNegative);
        for (int i = 0; i < scoresNegative.size(); i++) {
            recall.set(i,scoresPositive.get(i) / (scoresPositive.get(i) + (numNeg - scoresNegative.get(i))));
            precision.set(i,scoresPositive.get(i) / (scoresPositive.get(i) + (numPos - scoresPositive.get(i))));
            F1.set(i,2 * (precision.get(i) * recall.get(i)) / (precision.get(i) + recall.get(i)));
        }
        System.out.println("ShuZiInsQA.score(): pos/neg count: " + numPos + " " + numNeg);
        System.out.println("ShuZiInsQA.score(): labels:    " + resultHeader);

        System.out.println("ShuZiInsQA.score(): pos count: " + listAsTable(scoresPositive));
        System.out.println("ShuZiInsQA.score(): neg count: " + listAsTable(scoresNegative));
        System.out.println("ShuZiInsQA.score(): recall:    " + listAsTable(recall));
        System.out.println("ShuZiInsQA.score(): precision: " + listAsTable(precision));
        System.out.println("ShuZiInsQA.score(): F1:        " + listAsTable(F1));
        for (int i = 0; i < classIndex; i++) {
            scoresPositive.set(i, scoresPositive.get(i) / numPos);
            scoresNegative.set(i, scoresNegative.get(i) / numNeg);
        }
        System.out.println("ShuZiInsQA.score(): pos ratio: " + listAsTable(scoresPositive));
        System.out.println("ShuZiInsQA.score(): neg ratio: " + listAsTable(scoresNegative));
    }

    /****************************************************************
     * score whether each method gets the right answer.  For TFIDF
     * and Term Overlap, a right answer must have a value of 10
     * @return a list of floats which are the percentage
     * correct for each of the algorithms
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
        System.out.println("ShuZiInsQA.score(): classes: tfidf, term overlap, bigram overlap, trigram overlap, naive bayes combined, 1=answer 0=not the answer");
        for (ArrayList<Integer> classes : classifications) {
            //System.out.println("ShuZiInsQA.score(): classes: " + classes);
            //System.out.println("ShuZiInsQA.score(): class index: " + classIndex);
            if (classes.size() == classIndex + 1) {
                if (classes.get(classIndex) == 1) { // a correct answer
                    numPos++;
                    if (classes.get(0) == 10)  // overlap
                        scoresPositive.set(0, scoresPositive.get(0) + 1);
                    if (classes.get(1) == 10) // tfidf
                        scoresPositive.set(1, scoresPositive.get(1) + 1);
                    if (classes.get(2) == 10)  // bigram
                        scoresPositive.set(2, scoresPositive.get(2) + 1);
                    if (classes.get(3) == 10)  // trigram
                        scoresPositive.set(3, scoresPositive.get(3) + 1);
                    for (int counter = 4; counter < 7; counter++)
                        if (classes.get(counter) == 1)  // NB
                            scoresPositive.set(counter, scoresPositive.get(counter) + 1);
                    for (int counter = 7; counter < classes.size()-1; counter++)
                        if (classes.get(counter) == 1)  // LR
                            scoresPositive.set(counter, scoresPositive.get(counter) + 1);
                    if (scoresPositive.toString().contains("NaN"))
                        System.out.println("ShuZiInsQA.score(): number error: " + scoresPositive);
                }
                if (classes.get(classIndex) == 0) { // a wrong answer
                    numNeg++;
                    if (classes.get(0) == -1)  // overlap
                        scoresNegative.set(0, scoresNegative.get(0) + 1);
                    if (classes.get(1) == -1) // tfidf
                        scoresNegative.set(1, scoresNegative.get(1) + 1);
                    if (classes.get(2) == -1)  // bigram
                        scoresNegative.set(2, scoresNegative.get(2) + 1);
                    if (classes.get(3) == -1)  // trigram
                        scoresPositive.set(3, scoresNegative.get(3) + 1);
                    for (int counter = 4; counter < 7; counter++)
                        if (classes.get(counter) == 0)  // NB
                            scoresNegative.set(counter, scoresNegative.get(counter) + 1);
                    for (int counter = 7; counter < classes.size()-1; counter++)
                        if (classes.get(counter) == 0)  // LR
                            scoresNegative.set(counter, scoresNegative.get(counter) + 1);
                    if (scoresNegative.toString().contains("NaN"))
                        System.out.println("ShuZiInsQA.score(): number error: " + scoresNegative);
                }
            }
            //System.out.println("ShuZiInsQA.score(): positive scores: " + scoresPositive);
            //System.out.println("ShuZiInsQA.score(): negative scores: " + scoresNegative);
        }
        //System.out.println("ShuZiInsQA.score(): num positive scores: " + numPos);
        //System.out.println("ShuZiInsQA.score(): num negative scores: " + numNeg);
        printMetrics(scoresPositive, scoresNegative, numPos, numNeg, resultHeader,classIndex);
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

        ArrayList<ArrayList<String>> inputs = new ArrayList<>();
        // add types header and labels header
        inputs.add(Lists.newArrayList("disc", "disc", "disc", "disc", "class"));
        inputs.add(Lists.newArrayList("tfidf", "overlap", "bigram", "trigram", "answer"));
        //inputs.addAll(matrixIntegerToString(devsToInputs(devs,cb,to,ng)));
        List<NaiveBayes> nblist = createTrainingClasses(devs,cb,to,ng,inputs);

        inputs = new ArrayList<>();
        // add types header and labels header
        inputs.add(Lists.newArrayList("disc", "disc", "disc", "disc", "class"));
        inputs.add(Lists.newArrayList("tfidf", "overlap", "bigram", "trigram", "answer"));
        List<LogisticRegression> lrList = createTrainingClassesLR(devs, cb, to, ng, inputs);

        ArrayList<Dev> test = readTestQuestionOneFile();

        resultHeader = Lists.newArrayList("tfidf", "overlap", "bigram", "trigram", "NBall",
                "NBnoTri", "NBnoBi", "LRall", "LRnoTri", "LRnoBi","answer");
        ArrayList<ArrayList<Integer>> intInputs = classify(test, nblist, lrList, cb, to, ng);
        score(intInputs,10);
    }

    /****************************************************************
     */
    public static void streamTest() {

    }

    /****************************************************************
     */
    public static void main(String[] args) {

        long t1 = java.lang.System.currentTimeMillis();
        ShuZiInsQA sziq = new ShuZiInsQA();
        sziq.run();
        long t2 = java.lang.System.currentTimeMillis();
        System.out.println("ShuZiInsQA.main(): total time: " + (t2-t1)/1000 + " seconds");
        //testOverlap();
    }
}
