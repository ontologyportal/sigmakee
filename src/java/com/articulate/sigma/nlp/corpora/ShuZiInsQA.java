package com.articulate.sigma.nlp.corpora;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by apease on 9/4/15.
 * read data from https://github.com/shuzi/insuranceQA as per
 * http://arxiv.org/pdf/1508.01585.pdf
 */
public class ShuZiInsQA {

    /*


question.(dev|test1|test2).label.token_idx.pool:
<ground truth labels><TAB><question text in word index form><TAB><answer candidate pool>
To get the word of from its index idx_* ,  please use the file vocabulary
Notice we make an answer candidate pool with size 500 here for dev, test1 and test2.
If running time is not a problem for your applicaiton, you are surely encouraged to use the whole answer set as the pool (label 1-24981).

     */
    HashMap<String,String> vocab = new HashMap<>();
    HashMap<String,String> answers = new HashMap<>();
    HashMap<String,List<String>> training = new HashMap<>();

    /****************************************************************
     * vocabulary
     * <word index><TAB><original word>
     */
    private List<String> readLines(String filename) {

        List<String> lines = new ArrayList<String>();
        System.out.println("INFO in MUC.cleanSGML(): Reading files");
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
     */
    private void readVocab() {

        List<String> lines = readLines("/home/apease/IPsoft/insuranceQA-master/vocabulary");
        for (String l : lines) {
            String[] elements = l.split("\\t");
            if (elements.length == 2) {
                vocab.put(elements[0], elements[1]);
                System.out.println(elements[0] + "," + elements[1]);
            }
        }
    }

    /****************************************************************
     * <answer label><TAB><answer text in word index form>
     * To get the word of from its index idx_* ,  please use the file vocabulary
     */
    private void readAnswers() {

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
            sent.deleteCharAt(sent.length()-1);
            answers.put(answerID,sent.toString());
            System.out.println(answerID + ":" + sent);
        }
        System.out.println(answers);
    }

    /****************************************************************
     * <question text in word index form><TAB><answer labels>
     * To get the word of from its index idx_* ,  please use the file vocabulary
     */
    private void readTrainingQuestions() {

        List<String> lines = readLines("/home/apease/IPsoft/insuranceQA-master/question.train.token_idx.label");
        for (String l : lines) {
            String[] elements = l.split("\\t");
            String answer = elements[1];
            String[] answerIDs = answer.split(" ");
            String sentence = elements[0];
            String[] words = sentence.split(" ");
            StringBuffer sent = new StringBuffer();
            for (String id : words) {
                String word = vocab.get(id);
                sent = sent.append(word + " ");
            }
            sent.deleteCharAt(sent.length() - 1);
            ArrayList<String> answers = new ArrayList<>();
            answers.addAll(Arrays.asList(answerIDs));
            training.put(sent.toString(),answers);
            System.out.println(sent + ":" + answers);
        }
        System.out.println(answers);
    }

    /****************************************************************
     */
    public static void main(String[] args) {

        ShuZiInsQA sziq = new ShuZiInsQA();
        sziq.readVocab();
        sziq.readAnswers();
    }
}
