package com.articulate.sigma.nlp;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA

Use the Naive Bayes approach to train a classifier to predict
which class a set of values is likely to be a member of
http://guidetodatamining.com/guide/ch6/DataMining-ch6.pdf
 */

import com.articulate.sigma.DocGen;
import java.util.*;

import com.articulate.sigma.StringUtil;
import com.google.common.collect.Lists;

public class NaiveBayes {

    public ArrayList<ArrayList<String>> input = null;
    public ArrayList<String> labels = new ArrayList<>();
    public ArrayList<String> types = new ArrayList<>();

    public HashMap<String,HashMap<String,Float>> means = new HashMap<>();
    public HashMap<String,HashMap<String,Float>> ssd = new HashMap<>();
    public HashMap<String,HashMap<String,Float>> totals = new HashMap<>();
    public HashMap<String,HashMap<String,ArrayList<Float>>> numericValues = new HashMap<>();

    public HashMap<String,Integer> priorCounts = new HashMap<>();
    public HashMap<String,HashMap<String,HashMap<String,Integer>>> conditionalCounts = new HashMap<>();
    public HashMap<String,Float> priors = new HashMap<>();
    public HashMap<String,HashMap<String,HashMap<String,Float>>> conds = new HashMap<>();

    /** *************************************************************
     */
    public NaiveBayes(String filename) {

        DocGen dg = DocGen.getInstance();
        if (StringUtil.emptyString(filename))
            input = dg.readSpreadsheetFile("/home/apease/IPsoft/NB/NBdata.txt", ',');
        else
            input = dg.readSpreadsheetFile(filename, ',');
        //System.out.println(input);
        input.remove(0);  // remove "start"
        types.addAll(input.get(0));  // these can be discrete "disc", continuous "cont" or "class"
        labels.addAll(input.get(1));
        input.remove(0);  // remove types
        input.remove(0);  // remove headers
        //input.remove(input.size()-1);
        //System.out.println(input);
    }

    /** *************************************************************
     */
    public NaiveBayes(ArrayList<ArrayList<String>> in,
                      ArrayList<String> labels,
                      ArrayList<String> types) {

        input = new ArrayList<ArrayList<String>>();
        //System.out.println("NaiveBayes with #input: " + in.size());
        input.addAll(in);
        this.types = types; // these can be discrete "disc", continuous "cont" or "class"
        this.labels = labels;
        //System.out.println("NaiveBayes() : starting line: " + input.get(0));
    }

    /** *************************************************************
     * Compute P(x|y) given the mean, sample standard deviation and x
     */
    public static float probDensFunc(float mean, float ssd, float x) {

        float epart = (float) Math.exp((double) -(x-mean)*(x-mean)/(2*ssd*ssd));
        return ((float) 1.0 / ((float) Math.sqrt(2*Math.PI)*ssd)) * epart;
    }

    /** *************************************************************
     * Compute P(x|y) given the mean, sample standard deviation and x
     */
    public float probDensFunc(String clss, String label, float x) {

        float mean = means.get(clss).get(label);
        float dev = ssd.get(clss).get(label);
        return probDensFunc(mean,dev,x);
    }

    /** *************************************************************
     * Count the number of occurrences of each class.
     */
    public void createPriorCounts() {

        //System.out.println("NaiveBayes.createPriorCounts() : starting line: " + input.get(0));
        int classIndex = types.indexOf("class");
        for (ArrayList<String> row : input) {
            String clss = row.get(classIndex);
            if (!priorCounts.containsKey(clss))
                priorCounts.put(clss,new Integer(0));
            priorCounts.put(clss,priorCounts.get(clss) + 1);
        }
    }

    /** *************************************************************
     * Count the number of occurrences of each class.  The class name
     * must be the last element of each row of the input data.
     *
     * {i100={interest={appearance=2, health=1, ...}, ...},
     * i500={interest={appearance=3, health=4,...}, ...} }
     */
    public void createConditionalCounts() {

        //System.out.println("NaiveBayes.createConditionalCounts() : starting line: " + input.get(0));
        for (ArrayList<String> row : input) {
            int classIndex = types.indexOf("class");
            String clss = row.get(classIndex);
            //System.out.println("in createConditionalCounts(): " + clss);
            HashMap<String,HashMap<String,Integer>> classInfo = conditionalCounts.get(clss);
            if (classInfo == null) {
                classInfo = new HashMap<String,HashMap<String,Integer>>();
                conditionalCounts.put(clss,classInfo);
            }
            for (String label : labels) {
                int column = labels.indexOf(label);
                if (!types.get(column).equals("disc"))
                    continue;

                HashMap<String,Integer> values = classInfo.get(label);
                if (values == null)
                    values = new HashMap<String,Integer>();
                if (types.get(column).equals("disc")) {
                    String value = row.get(column);
                    if (values.containsKey(value))
                        values.put(value, values.get(value) + 1);
                    else
                        values.put(value, new Integer(1));
                    classInfo.put(label.toString(), values);
                }
            }
            conditionalCounts.put(clss,classInfo);
        }
        //System.out.println("NaiveBayes.createConditionalCounts() : " + conditionalCounts);
    }

    /** *************************************************************
     * Create totals per class of each variable that is continuous.
     */
    public void createTotals() {

        //System.out.println("NaiveBayes.createTotals() : starting line: " + input.get(0));
        for (ArrayList<String> row : input) {
            int classIndex = types.indexOf("class");
            String clss = row.get(classIndex);
            HashMap<String, Float> classInfo = totals.get(clss);
            if (classInfo == null) {
                classInfo = new HashMap<String, Float>();
                totals.put(clss, classInfo);
            }
            for (int i = 0; i < row.size(); i++) {
                if (!types.get(i).equals("cont"))
                    continue;
                String column = labels.get(i);
                float value = Float.parseFloat(row.get(i));
                if (classInfo.containsKey(column)) {
                    value += classInfo.get(column);
                }
                classInfo.put(column, value);
            }
        }
    }

    /** *************************************************************
     */
    public void createMeans() {

        for (String clss : totals.keySet()) {
            HashMap<String,Float> classMeanInfo = means.get(clss);
            if (classMeanInfo == null) {
                classMeanInfo = new HashMap<String, Float>();
                means.put(clss, classMeanInfo);
            }
            HashMap<String,Float> classTotalsInfo = totals.get(clss);
            float count = (float) priorCounts.get(clss);
            for (String column : classTotalsInfo.keySet()) {
                float value = classTotalsInfo.get(column);
                classMeanInfo.put(column,value / count);
            }
            //System.out.println("createMeans(): " + clss + ":" + classMeanInfo);
        }
    }

    /** *************************************************************
     * Note that this computes the sample standard deviation
     * sigma = sqrt( (1/(N-1)) sum(1,n,(xi-meanx)*(xi-meanx)))
     */
    public void createStandardDeviation() {

        for (ArrayList<String> row : input) {
            int classIndex = types.indexOf("class");
            String clss = row.get(classIndex);
            HashMap<String, Float> classMeansInfo = means.get(clss);
            HashMap<String, Float> classSsdInfo = ssd.get(clss);
            if (classSsdInfo == null) {
                classSsdInfo = new HashMap<String, Float>();
                ssd.put(clss, classSsdInfo);
            }
            for (String label : classMeansInfo.keySet()) {
                if (label.equals("cont"))
                    continue;
                float mean = classMeansInfo.get(label);
                int colIndex = labels.indexOf(label);
                float value = Float.parseFloat(row.get(colIndex));
                float squaredDiff = (value - mean) * (value - mean);
                float total = squaredDiff;
                if (classSsdInfo.containsKey(label))
                    total = classSsdInfo.get(label) + squaredDiff;
                classSsdInfo.put(label,total);
            }
        }
        for (String clss : ssd.keySet()) {
            HashMap<String, Float> classSsdInfo = ssd.get(clss);
            int classCount = priorCounts.get(clss);
            for (String label : classSsdInfo.keySet()) {
                float variance = classSsdInfo.get(label) / ((float) (classCount - 1));
                variance = (float) Math.sqrt(variance);
                classSsdInfo.put(label,variance);
            }
        }
    }

    /** *************************************************************
     */
    public int findTrainingSetSize() {

        int sum = 0;
        for (String s : priorCounts.keySet()) {
            sum += priorCounts.get(s);
        }
        return sum;
    }

    /** *************************************************************
     * Calculate the prior probabilities of each class given the
     * numbers of instances of each class.
     */
    public void calcPriors(int sum) {

        for (String s : priorCounts.keySet())
            priors.put(s,new Float((float) priorCounts.get(s) / (float) sum));
    }

    /** *************************************************************
     * Compute conditionals in the format of a class name key then
     * the probabilities of the values for each "column" which is a
     * numerical key.  So below, the probability of getting the value
     * "appearance" as the value of variable 1 for class
     * {'i500': {1: {'appearance':0.33,'health':0.44},
     *           2: {...
     */
    public void calcConditionals(int sum) {

        for (String clss : conditionalCounts.keySet()) {
            int clssCount = priorCounts.get(clss);
            HashMap<String,HashMap<String,Integer>> classCounts = conditionalCounts.get(clss);
            HashMap<String,HashMap<String,Float>> classConditionals = new HashMap<>();
            for (String col : classCounts.keySet()) {
                HashMap<String,Integer> colValues = classCounts.get(col);
                HashMap<String,Float> colConditionals = new HashMap<>();
                for (String val : colValues.keySet()) {
                    float posterior = (float) colValues.get(val).intValue() / clssCount;
                    // m-estimate of probability (Mitchell, Machine Learning, p 179, eq 6.22)
                    //float posterior = (float) colValues.get(val).intValue() + (float) 1.0 /
                    //        ((float) clssCount + (float) colValues.keySet().size());
                    colConditionals.put(val,posterior);
                }
                classConditionals.put(col,colConditionals);
            }
            conds.put(clss,classConditionals);
        }
    }

    /** *************************************************************
     * Given the conditional and prior probabilities, and a particular
     * instance set of attributes, compute which class that instance is
     * mostly likely to fall into.
     */
    public String classify(List<String> values) {

        if (values == null) {
            System.out.println("Error in NaiveBayes.classify: null input");
            return "";
        }
        int classIndex = types.indexOf("class");
        int indexMod = 0;  // if class name is not the last element
        if (classIndex == 0)
            indexMod = 1;
        float maxProb = 0;
        String maxClass = "";
        HashMap<String,Float> probs = new HashMap<String,Float>();
        for (String clss : conds.keySet()) {
            HashMap<String,HashMap<String,Float>> posteriors = conds.get(clss);
            float prior = priors.get(clss);
            float prob = prior;
            for (String label : labels) {
                if (label.equals("class"))
                    continue;
                int index = labels.indexOf(label);
                String type = types.get(index);
                if (type.equals("disc")) {
                    HashMap<String, Float> conditCol = posteriors.get(label);
                    if (conditCol != null) { // trap unseen features
                        String value = values.get(index);
                        if (value == null || value == "" || conditCol.get(value) == null) {
                            System.out.println("Error in NaiveBayes.classify: " + label +
                                    " index: " + index + " values: " + values + " value: " + value);
                            System.out.println(conds.get(clss));
                        }
                        else {
                            float conditional = conditCol.get(value);
                            prob = prob * conditional;
                        }
                    }
                }
                if (type.equals("cont")) {
                    float value = Float.parseFloat(values.get(index));
                    float conditional = probDensFunc(clss,label,value);
                    prob = prob * conditional;
                }
            }
            probs.put(clss, new Float(prob));
            if (prob > maxProb) {
                maxProb = prob;
                maxClass = clss;
            }
        }
        //System.out.println("NaiveBayes.classify(): probabilities: " + probs);
        return maxClass;
    }

    /** *************************************************************
     */
    public void initialize() {

        System.out.println("NaiveBayes.initialize(): first line: " + input.get(0));
        createPriorCounts();
        System.out.println("NaiveBayes.initialize() : priorCounts: " + priorCounts);
        int sum = findTrainingSetSize();
        System.out.println("NaiveBayes.initialize() : sum: " + sum);
        // class name key value map of column number key
        createConditionalCounts();
        System.out.println("NaiveBayes.initialize() : conditionalCounts: " + conditionalCounts);
        createTotals();
        System.out.println("NaiveBayes.initialize() : totals: " + totals);
        createMeans();
        System.out.println("NaiveBayes.initialize() : means: " + means);
        createStandardDeviation();
        System.out.println("NaiveBayes.initialize() : standard deviation: " + ssd);
        calcPriors(sum);
        System.out.println("NaiveBayes.initialize() : priors: " + priors);
        calcConditionals(sum);
        System.out.println("NaiveBayes.initialize() : conds: " + conds);
    }

    /** *************************************************************
     * take a filename and a quoted list of numbers as arguments on
     * the command line
     */
    public static void main(String[] args) {

        /* Sample Data
        both,sedentary,moderate,yes,i100
        both,sedentary,moderate,no,i100
        health,sedentary,moderate,yes,i500
        appearance,active,moderate,yes,i500
        appearance,moderate,aggressive,yes,i500
        appearance,moderate,aggressive,no,i100
        health,moderate,aggressive,no,i500
        both,active,moderate,yes,i100
        both,moderate,aggressive,yes,i500
        appearance,active,aggressive,yes,i500
        both,active,aggressive,no,i500
        health,active,moderate,no,i500
        health,sedentary,aggressive,yes,i500
        appearance,active,moderate,no,i100
        health,sedentary,moderate,no,i100
         */

        // read from a file assuming a list of attributes and a class name last on each line
        DocGen dg = DocGen.getInstance();
        //NaiveBayes nb = new NaiveBayes("/home/apease/IPsoft/NB/NBdata.txt");
        //NaiveBayes nb = new NaiveBayes("/home/apease/IPsoft/NB/house-votes-84.data");
        NaiveBayes nb = null;
        ArrayList<String> values = null;
        if (args.length >= 1) {
            nb = new NaiveBayes(args[0]);
            nb.initialize();
            for (int i = -1; i <= 10; i++) {
                for (int j = -1; j <= 10; j++) {
                    values = new ArrayList<>();
                    values.add(Integer.toString(i));
                    values.add(Integer.toString(j));
                    System.out.println(values.get(0) + ", " + values.get(1) + ", " + nb.classify(values));
                }
            }
        }
        else {
            nb = new NaiveBayes("/home/apease/IPsoft/NB/pima-indians-diabetes.data");
            values = Lists.newArrayList("4","111","72","47","207","37.1","1.390","56");
            nb.initialize();
            System.out.println("main(): most likely class: " + nb.classify(values));
        }
        //ArrayList<String> values = Lists.newArrayList("health","moderate","moderate","yes","class");
        //ArrayList<String> values = Lists.newArrayList("y","y","y","n","n","n","y","y","y","n","n","n","y","n","y","y");
        // ArrayList<String> values = Lists.newArrayList("both","sedentary","aggressive","no","class");
        //ArrayList<String> values = Lists.newArrayList("7","81","88","40","48","46.7","0.261","52");
    }
}
