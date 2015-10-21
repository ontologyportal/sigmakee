package com.articulate.sigma.nlp;

import com.articulate.sigma.DB;
import com.sun.org.apache.bcel.internal.classfile.LineNumber;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by apease on 10/6/15.
 * adapted from http://blog.smellthedata.com/2009/06/python-logistic-regression-with-l2.html
 */
public class LogisticRegression {
    // A simple logistic regression model with L2 regularization (zero-mean Gaussian priors on parameters).

    public Random rand = new Random();
    public double[][] x_train;  // first dimension is the list of points, second dimension is the dimensions
    public double[] y_train;    // the class of each point
    public double[][] x_test;
    public double[] y_test;
    public int n; // number of points
    public int dim; // number of dimensions
    public double alpha = 0.0001;
    public double[] betas;             // a set of coefficients the same length as the x vector
    public ArrayList<String> labels = new ArrayList<>();
    public ArrayList<String> types = new ArrayList<>();
    public int epochBatch = 0;
    public static final int numBatches = 10;
    public boolean epochs = false; // sample the training space

    // batch number, then example number and original example number
    public HashMap<Integer,HashSet<Integer>> epochMap = new HashMap<>();

    /****************************************************************
     */
    public LogisticRegression() {

    }

    /****************************************************************
     * load betas from a file to bypass training phase
     */
    public LogisticRegression(String filename) {

        load(filename);
        n = 0;  // training params don't matter here
        dim = betas.length;
        alpha = 0; // default no smoothing, but training params don't matter here
    }

    /****************************************************************
     */
    public LogisticRegression(ArrayList<ArrayList<String>> inputs,
                              ArrayList<String> labels,
                              ArrayList<String> types) {

        this.types = types;  // these can be discrete "disc", continuous "cont" or "class"
        this.labels = labels;
        System.out.println("LogisticRegressions(): types: " + types);
        System.out.println("LogisticRegressions(): labels: " + labels);
        System.out.println("LogisticRegressions(): first line: " + inputs.get(0));
        int numpoints = inputs.size();
        int numDimensions = inputs.get(0).size()-1; // note that it includes the class as final element
        if (types.indexOf("class") != numDimensions)
            System.out.println("Error in LogisticRegressions(): class is not class column");
        x_train = new double[numpoints][numDimensions];
        y_train = new double[numpoints];
        for (int i = 0; i < numpoints; i++) {
            ArrayList<String> row = inputs.get(i);
            for (int j = 0; j < numDimensions; j++) {
                x_train[i][j] = Double.parseDouble(row.get(j));
            }
            y_train[i] = Double.parseDouble(row.get(row.size() - 1));
        }
        n = numpoints;
        dim = numDimensions;
        alpha = 0; // default no smoothing
    }

    /****************************************************************
     */
    public void printTabbedLine(PrintWriter pw, List<String> ar) throws IOException {

        for (int i = 0; i < ar.size(); i++) {
            if (i != 0)
                pw.print("\t");
            pw.print(ar.get(i));
        }
        pw.println();
    }

    /****************************************************************
     * load a set of betas with their labels and types
     */
    public void load(String filename) {

        ArrayList<ArrayList<String>> fn = DB.readSpreadsheet(filename,null,false,',');
        labels = new ArrayList<>();
        labels.addAll(fn.get(0));
        types = new ArrayList<>();
        types.addAll(fn.get(1));
        betas = new double[types.size()];
        for (int i = 0; i < fn.get(0).size(); i++)
            betas[i] = Double.parseDouble(fn.get(2).get(i));
    }

    /****************************************************************
     * Save tab-delimited data for the coefficients
     */
    public void save() {

        ArrayList<ArrayList<String>> values = new ArrayList<>();
        values.add(labels);
        values.add(types);
        ArrayList<String> betaString = new ArrayList<>();
        for (int i = 0; i < betas.length; i++)
            betaString.add(Double.toString(betas[i]));
        values.add(betaString);
        String out = DB.writeSpreadsheet(values,false);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter("LRbetas.txt"));
            pw.println(out);
        }
        catch (Exception ex) {
            System.out.println("Error writing file LRbetas.txt");
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /****************************************************************
     * Create a new set of randomized batches of examples for training
     */
    public void shuffleExamples() {

        System.out.println("LogisticRegression.shuffleExamples()");
        for (int i = 0; i < numBatches; i++)
            epochMap.put(i,new HashSet<Integer>());
        for (int i = 0; i < n; i++) {
            int batch = rand.nextInt(numBatches);
            HashSet<Integer> abatch = epochMap.get(batch);
            abatch.add(i);
        }
    }

    /****************************************************************
     */
    public static List<String> vectorToStringList(double[] ar) {

        List<String> result = new ArrayList<>();
        for (int i = 0; i < ar.length; i++)
            result.add(Double.toString(ar[i]));
        return result;
    }

    /****************************************************************
     */
    public static String toStringArrayWithPrecision(double[] ar) {

        DecimalFormat myFormatter = new DecimalFormat("###.##");
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < ar.length; i++) {
            if (i != 0)
                sb.append(", ");
            sb.append(myFormatter.format(ar[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    /****************************************************************
     * Create N instances of d dimensional input vectors and a 1D class label (-1 or 1).
     */
    public void generateData(int N, int d) {

        Random rnd = new Random();
        rnd.setSeed(0);
        double[][] means = new double[2][d];
        for (int i = 0; i < 2; i++)
            Arrays.fill(means[i], 0.05 * rnd.nextDouble()*d);

        x_train = new double[N][d];
        for (int i = 0; i < N; i++)
            Arrays.fill(x_train[i], 0.0);
        y_train = new double[N];
        Arrays.fill(y_train, 0.0);
        int y = 0;
        for (int i = 0; i < N; i++) {
            if (rnd.nextDouble() > .5)
                y = 1;
            else
                y = 0;
            for (int j = 0; j < d; j++)
                x_train[i][j] = rnd.nextDouble()*d + means[y][j];
            y_train[i] = 2.0 * y - 1;  // 1 or -1
        }
        x_test = new double[N][d];
        for (int i = 0; i < N; i++)
            Arrays.fill(x_test[i], 0.0);
        y_test = new double[N];
        Arrays.fill(y_test, 0.0);
        for (int i = 0; i < N; i++) {
            if (rnd.nextDouble() > .5)
                y = 1;
            else
                y = 0;
            for (int j = 0; j < d; j++)
                x_test[i][j] = rnd.nextDouble()*d + means[y][j];
            y_test[i] = 2.0 * y - 1;
        }
        System.out.println("generate()");
        for (int i = 0; i < N; i++)
            System.out.println(i + ":" + toStringArrayWithPrecision(x_train[i]));
        System.out.println(Arrays.toString(y_train));
        for (int i = 0; i < N; i++)
            System.out.println(i + ":" + toStringArrayWithPrecision(x_test[i]));
        System.out.println(Arrays.toString(y_test));
        n = N;
        dim = d;
        alpha = 0; // default no smoothing
    }

    /****************************************************************
     * @return value approaches 1.0 as x is large and positive, and
     * approaches 0 as x is large and negative
     */
    public double sigmoid(double x) {

        return 1.0 / (1.0 + Math.exp(-x));
    }

    /****************************************************************
     */
    public void init() {

        // Set L2 regularization strength
        //this.alpha=alpha;

        // Initialize parameters to zero, for lack of a better choice.
        betas = new double[dim];
        Arrays.fill(betas,0.0);
        System.out.println("init() with betas: " + toStringArrayWithPrecision(betas) + " and dim: " + dim);
    }

    /****************************************************************
     */
    public double negative_lik(double[] bs) {

        return -1.0 * lik(bs);
    }

    /****************************************************************
     * Likelihood of the data under the current settings of parameters.
     * Data is smoothed with the alpha parameter, data points are sampled
     * with the minibatch method
     */
    public double lik_minibatch(double[] bs) {

        // Data likelihood
        double l = 0;
        HashSet<Integer> elements = epochMap.get(epochBatch);
        for (Integer i : elements) {
            l += Math.log(sigmoid(y_train[i] * dotProduct(bs, x_train[i])));
        }

        //Prior likelihood
        for (int k = 0; k < dim; k++)
            l -= (alpha / 2.0) * bs[k] * bs[k];

        return l;
    }

    /****************************************************************
     * Likelihood of the data under the current settings of parameters.
     * Data is smoothed with the alpha parameter
     */
    public double lik(double[] bs) {

        // Data likelihood
        double l = 0;
        for (int i = 0; i < n; i++)
            l += Math.log(sigmoid(y_train[i] * dotProduct(bs, x_train[i])));

        //Prior likelihood
        for (int k = 0; k < dim; k++)
            l -= (alpha / 2.0) * bs[k] * bs[k];

        return l;
    }

    /****************************************************************
     * Equal to the cosine of the angle between the vectors times the
     * product of the length of the vectors
     */
    public double dotProduct(double[] a1, double[] a2) {

        double sum = 0;
        if (a1.length != a2.length)
            return sum;
        for (int i = 0; i < a1.length; i++)
            sum += a1[i] * a2[i];
        return sum;
    }

    /****************************************************************
     */
    public double sum(double[] a1) {

        double sum = 0;
        for (int i = 0; i < a1.length; i++)
            sum += a1[i];
        return sum;
    }

    /****************************************************************
     */
    public double[] addConstant(double[] a1, double a2) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] + a2;
        return result;
    }

    /****************************************************************
     */
    public double[] addVectors(double[] a1, double[] a2) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] + a2[i];
        return result;
    }

    /****************************************************************
     * subtract a2 from a1
     */
    public double[] subtractVectors(double[] a1, double[] a2) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] - a2[i];
        return result;
    }

    /****************************************************************
     * multiply a vector by a constant
     */
    public double[] multVec(double[] a1, double c) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] * c;
        return result;
    }

    /****************************************************************
     * divide a vector by a constant
     */
    public double[] divVec(double[] a1, double c) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] / c;
        return result;
    }

    /****************************************************************
     * multiple two vectors
     */
    public double[] multVec(double[] a1, double[] a2) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] * a2[i];
        return result;
    }

    /****************************************************************
     * Define the derivative of the likelihood with respect to beta_k.
     * Need to multiply by -1 because we will be minimizing.
     */
    public double dB_k_minibatch(int k) {

        double dB_k_result = 0;
        double sum = 0;
        HashSet<Integer> elements = epochMap.get(epochBatch);
        if (elements == null)
            System.out.println("error in LogisticRegression.dB_k_minibatch(): " + epochBatch);
        for (Integer i : elements) {
            sum += y_train[i] * x_train[i][k] * sigmoid(-y_train[i] * dotProduct(betas, x_train[i]));
        }
        if (k > 0)
            dB_k_result = alpha * betas[k] - sum;
        else
            dB_k_result =  - sum;

        return dB_k_result;
    }

    /****************************************************************
     * Define the derivative of the likelihood with respect to beta_k.
     * Need to multiply by -1 because we will be minimizing.
     */
    public double dB_k(int k) {

        double dB_k_result = 0;
        double sum = 0;
        for (int i = 0; i < n; i++)
            sum += y_train[i] * x_train[i][k] * sigmoid(-y_train[i] * dotProduct(betas, x_train[i]));
        if (k > 0)
            dB_k_result = alpha * betas[k] - sum;
        else
            dB_k_result =  - sum;

        return dB_k_result;
    }

    /****************************************************************
     * The full gradient is just an array of componentwise derivatives
     */
    public double[] dB() {

        double[] result = new double[dim];
        for (int k = 0; k < dim; k++)
            result[k] = dB_k(k);
        return result;
    }

    /****************************************************************
     * Define the gradient and train
     */
    public void train() {

        System.out.println("LogisticRegression.train(): initial betas: " + toStringArrayWithPrecision(betas));
        int num_iterations = 1000;
        double learningRate = 0.0001;
        double epsilon = 0.0001;
        double[] epsilonAr = new double[betas.length];
        int count = 0;
        double delta = 1.0;
        double deltaLimit = 0.01;
        double lastLik = 0;
        //Optimize (objective function, initial guess, gradient of f
        while (count < num_iterations && delta > deltaLimit) {
            count++;
            betas = addVectors(betas, multVec(dB(), -learningRate)); // fmin_bfgs(negative_lik, betas, fprime = dB);
            double lik = negative_lik(betas);
            delta = Math.abs(lastLik - lik);
            System.out.println("LogisticRegression.train(): negative lik: " + lik);
            lastLik = lik;
            //System.out.println("LogisticRegression.train(): betas: " + toStringArrayWithPrecision(betas));
        }
        System.out.println("LogisticRegression.train(): iterations: " + count);
        System.out.println("LogisticRegression.train(): delta: " + delta);
        System.out.println("LogisticRegression.train(): final betas: " + toStringArrayWithPrecision(betas));
    }

    /****************************************************************
     */
    private double classifyAlt(double[] x) {

        double logit = 0.0;
        for (int i = 0; i < betas.length;i++)  {
            logit += betas[i] * x[i];
        }
        return sigmoid(logit);
    }

    /****************************************************************
     * Define the gradient and train per https://github.com/tpeng/logistic-regression
     */
    public void trainAlt () {

        System.out.println("LogisticRegression.trainAlt(): initial betas: " + toStringArrayWithPrecision(betas));
        int num_iterations = 1000;
        int count = 0;
        double delta = 1.0;
        double deltaLimit = 0.01;
        double learningRate = 0.0001;
        double lastLik = 0;
        while (count < num_iterations && delta > deltaLimit) {
            count++;
            double lik = 0.0;
            for (int i = 0; i < x_train.length; i++) {
                double[] x = x_train[i];
                double predicted = classifyAlt(x);
                double label = y_train[i];
                for (int j = 0; j < betas.length; j++) {
                    betas[j] = betas[j] + learningRate * (label - predicted) * x[j];
                }
                // not necessary for learning
                lik += label * classifyAlt(x) + (1 - label) * Math.log(1 - classifyAlt(x));
                //System.out.println("LogisticRegression.trainAlt(): lik: " + lik);
                delta = Math.abs(lastLik - lik);
                lastLik = lik;
            }
            System.out.println("LogisticRegression.trainAlt(): iterations: " + count);
            System.out.println("LogisticRegression.trainAlt(): delta: " + delta);
            System.out.println("LogisticRegression.trainAlt(): final betas: " + toStringArrayWithPrecision(betas));
        }
    }

    /****************************************************************
     * train using AdaGrad http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
     */
    public void trainAdaGrad() {

        System.out.println("LogisticRegression.trainAdaGrad(): initial betas: " + toStringArrayWithPrecision(betas));
        int num_iterations = 1000;
        double[] learningRates = new double[betas.length];
        Arrays.fill(learningRates,0.0001);
        double[] learningRateSums = new double[betas.length]; // sum of the squared rates
        Arrays.fill(learningRateSums,0.0001);
        double globalLearningRate = 0.0001;
        int count = 0;
        double delta = 1.0;
        double deltaLimit = 0.01;
        double lastLik = 0;
        //Optimize (objective function, initial guess, gradient of f
        while (count < num_iterations && delta > deltaLimit) {
            count++;
            double[] multiple = new double[betas.length];
            for (int i = 0; i < betas.length; i++) {
                multiple[i] = -globalLearningRate / Math.sqrt(learningRateSums[i]);
            }
            double[] gradient = dB();
            betas = addVectors(betas, multVec(gradient, multiple));
            double lik = negative_lik(betas);
            delta = Math.abs(lastLik - lik);
            System.out.println("LogisticRegression.trainAdaGrad(): negative lik: " + lik);
            lastLik = lik;
            learningRateSums = addVectors(learningRateSums, multVec(gradient,gradient));
            System.out.println("LogisticRegression.train(): betas: " + toStringArrayWithPrecision(betas));
        }
        System.out.println("LogisticRegression.trainAdaGrad(): iterations: " + count);
        System.out.println("LogisticRegression.trainAdaGrad(): delta: " + delta);
        System.out.println("LogisticRegression.trainAdaGrad(): final betas: " + toStringArrayWithPrecision(betas));
    }

    /****************************************************************
     * train using AdaGrad http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
     */
    public void trainAdaGradMiniBatch() {

        epochBatch = 0;
        System.out.println("LogisticRegression.trainAdaGrad(): initial betas: " + toStringArrayWithPrecision(betas));
        int num_iterations = 1000;
        double[] learningRates = new double[betas.length];
        Arrays.fill(learningRates,0.0001);
        double[] learningRateSums = new double[betas.length]; // sum of the squared rates
        Arrays.fill(learningRateSums,0.0001);
        double globalLearningRate = 0.0001;
        int count = 0;
        double delta = 1.0;
        double deltaLimit = 0.01;
        double lastLik = 0;
        //Optimize (objective function, initial guess, gradient of f
        while ((count < num_iterations && delta > deltaLimit) || epochBatch != numBatches) {
            count++;
            if (epochBatch == numBatches)
                epochBatch = 0;
            if (epochBatch == 0)
                shuffleExamples();
            double[] multiple = new double[betas.length];
            for (int i = 0; i < betas.length; i++) {
                multiple[i] = -globalLearningRate / Math.sqrt(learningRateSums[i]);
            }
            double[] gradient = dB();
            betas = addVectors(betas, multVec(gradient, multiple));
            double lik = negative_lik(betas);
            delta = Math.abs(lastLik - lik);
            System.out.println("LogisticRegression.trainAdaGrad(): negative lik: " + lik);
            lastLik = lik;
            learningRateSums = addVectors(learningRateSums, multVec(gradient,gradient));
            System.out.println("LogisticRegression.train(): betas: " + toStringArrayWithPrecision(betas));
            epochBatch++;
        }
        System.out.println("LogisticRegression.trainAdaGrad(): iterations: " + count);
        System.out.println("LogisticRegression.trainAdaGrad(): delta: " + delta);
        System.out.println("LogisticRegression.trainAdaGrad(): final betas: " + toStringArrayWithPrecision(betas));
    }

    /****************************************************************
     * train using AdaDelta http://arxiv.org/pdf/1212.5701v1.pdf
     */
    public void trainAdaDelta() {


    }

    /****************************************************************
     */
    public void set_data(LogisticRegression lr) {
       // Take data that's already been generated.

        this.x_train = lr.x_train;
        this.y_train = lr.y_train;
        this.x_test = lr.x_test;
        this.y_test = lr.y_test;
        this.n = lr.n;
        this.dim = lr.dim;
    }

    /****************************************************************
     */
    public double[] training_reconstruction() {

        double[] p_y1 = new double[n];
        Arrays.fill(p_y1,0);
        for (int i = 0; i < n; i++)
            p_y1[i] = sigmoid(dotProduct(betas, x_train[i]));

        return p_y1;
    }

    /****************************************************************
     */
    public double[] test_predictions() {

        double[] p_y1 = new double[n];
        Arrays.fill(p_y1,0);
        for (int i = 0; i < n; i++)
            p_y1[i] = sigmoid(dotProduct(betas, x_test[i]));

        return p_y1;
    }

    /****************************************************************
     * @param values a list of string values that will be assumed to be floats
     *               of features and a final element for the class
     * @return a String representation of the class which is either 1 or 0
     */
    public String classify(List<String> values) {

        double result = classifyContinuous(values);
        //System.out.println("LogisticRegression.classify(): result: " + result + " values: " + values);
        //System.out.println("LogisticRegression.classify(): betas: " + toStringArrayWithPrecision(betas));
        if (result > 0.5)
            return "1";
        else
            return "0";
    }

    /****************************************************************
     * @param values a list of string values that will be assumed to be floats
     *               of features and a final element for the class
     * @return a double representation of the likelihood of the class
     */
    public double classifyContinuous(List<String> values) {

        //System.out.println("LogisticRegression.classifyContinuous(): values: " + values);
        //System.out.println("LogisticRegression.classifyContinuous(): betas: " + toStringArrayWithPrecision(betas));
        //System.out.println("LogisticRegression.classifyContinuous(): dim: " + dim);
        double[] input = new double[dim];
        if (values.size() != dim)  // size minus the expected class column
            System.out.println("Error in LogisticRegression.classifyContinuous(): wrong size array " + values.size());
        for (int i = 0; i < values.size()-1; i++) {
            input[i] = Double.parseDouble(values.get(i));
        }
        return sigmoid(dotProduct(betas,input));
    }

    /****************************************************************
     * Check whether the derivative function for the likelihood dB() is the same
     * as a very small change in the likelihood.  This is a cross-check
     * for whether the derivative function is correct.
     */
    public static void gradientCheck() {

        double epsilon = 0.0001;
        double[] newBetas = {-0.04, 0.48, 0.1, -0.15, -0.41, 0.73, 0.55, -0.14,
                0.27, -0.97, 0.07, 0.22, 0.11, 0.15, 0.09, 0.2, -0.38, -0.69, 0.23, 0.1};
        LogisticRegression lr1 = new LogisticRegression();
        lr1.generateData(25, 20);
        lr1.init();
        lr1.betas = newBetas;
        double[] alphas = {0};
        for (int i = 0; i < lr1.betas.length-1; i++ ) {
            double[] betasPlus = Arrays.copyOf(lr1.betas,lr1.betas.length);
            betasPlus[i] = betasPlus[i] + epsilon;
            double[] betasMinus = Arrays.copyOf(lr1.betas,lr1.betas.length);
            betasMinus[i] = betasMinus[i] - epsilon;

            double analytic = lr1.dB()[i];
            System.out.println("analytical derivative: " + analytic);

            double divisor = 2 * epsilon;
            double approx = (lr1.negative_lik(betasPlus) - lr1.negative_lik(betasMinus)) / divisor;
            System.out.println("approx derivative: " + approx);
            System.out.println("difference: " + Math.abs(analytic - approx));
        }
    }

    /****************************************************************
     */
    public static void main(String[] args) {

        //Create 20 dimensional data set with 25 points-- this will be
        //susceptible to overfitting.
        LogisticRegression lr1 = new LogisticRegression();

        lr1.generateData(25, 20);
        lr1.init();

        //Run for a variety of regularization strengths
        //double[] alphas = {0, .001, .01, .1};
        double[] alphas = {0.0001};
        for (int j = 0 ; j < alphas.length; j++) {
            double a = alphas[j];
            System.out.println();
            System.out.println("***** alpha: " + a + " *****");
            //Create a new learner, but use the same data for each run

            LogisticRegression lr = new LogisticRegression();
            lr.set_data(lr1);
            lr.init();
            lr.alpha = a;
            System.out.println("Initial likelihood:");
            System.out.println(lr.lik(lr.betas));

            //Train the model
            lr.train();

            //Display execution info
            System.out.println("Final betas:");
            System.out.println(toStringArrayWithPrecision(lr.betas));
            System.out.println("Final lik:");
            System.out.println(lr.lik(lr.betas));
            gradientCheck();
        }
    }
}
