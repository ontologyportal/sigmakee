package com.articulate.sigma.nlp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by apease on 10/6/15.
 * adapted from http://blog.smellthedata.com/2009/06/python-logistic-regression-with-l2.html
 */
public class LogisticRegression {
    // A simple logistic regression model with L2 regularization (zero-mean Gaussian priors on parameters).

    public double[][] x_train;  // first dimension is the list of points, second dimension is the dimensions
    public double[] y_train;    // the class of each point
    public double[][] x_test;
    public double[] y_test;
    public int n; // number of points
    public int dim; // number of dimensions
    public double alpha;
    public double[] betas;             // a set of coefficients the same length as the x vector

    /****************************************************************
     */
    public LogisticRegression() {

    }

    /****************************************************************
     */
    public LogisticRegression(ArrayList<ArrayList<String>> inputs) {

        inputs.remove(0);  // remove types
        inputs.remove(0);  // remove headers
        int numpoints = inputs.size();
        int numDimensions = inputs.get(0).size(); // note that it includes the class as final element
        x_train = new double[numpoints][numDimensions];
        y_train = new double[numpoints];
        for (int i = 0; i < numpoints; i++) {
            ArrayList<String> row = inputs.get(i);
            for (int j = 0; j < numDimensions-1; j++) {
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
    public double negative_lik() {

        return -1.0 * lik();
    }

    /****************************************************************
     * Likelihood of the data under the current settings of parameters.
     * Data is smoothed with the alpha parameter
     */
    public double lik() {

        // Data likelihood
        double l = 0;
        for (int i = 0; i < n; i++)
            l += Math.log(sigmoid(y_train[i] * dotProduct(betas, x_train[i])));

        //Prior likelihood
        for (int k = 1; k < dim; k++)
            l -= (alpha / 2.0) * betas[k] * betas[k];

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
    public double[] addVectors(double[] a1, double[] a2) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] + a2[i];
        return result;
    }

    /****************************************************************
     */
    public double[] multVec(double[] a1, double c) {

        double[] result = new double[a1.length];
        for (int i = 0; i < a1.length; i++)
            result[i] = a1[i] * c;
        return result;
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
     * Define the gradient and hand it off to a scipy gradient-based optimizer.
     */
    public void train() {

        System.out.println("LogisticRegression.train(): initial betas: " + Arrays.toString(betas));
        int num_iterations = 1000;
        double learningRate = 0.1;
        int count = 0;
        double delta = 1.0;
        double deltaLimit = 0.01;
        double lastLik = 0;
        //Optimize (objective function, initial guess, gradient of f
        while (count < num_iterations && delta > deltaLimit) {
            count++;
            betas = addVectors(betas, multVec(dB(), -learningRate)); // fmin_bfgs(negative_lik, betas, fprime = dB);
            double lik = negative_lik();
            delta = Math.abs(lastLik - lik);
            //System.out.println("LogisticRegression.train(): negative lik: " + lik);
            //System.out.println("LogisticRegression.train(): betas: " + toStringArrayWithPrecision(betas));
        }
        System.out.println("LogisticRegression.train(): iterations: " + count);
        System.out.println("LogisticRegression.train(): delta: " + delta);
        System.out.println("LogisticRegression.train(): final betas: " + Arrays.toString(betas));
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
     * @return a String representation of the class which is either 1 or 0
     */
    public String classify(List<String> values) {

        //System.out.println("LogisticRegression.classify(): " + values);
        double[] input = new double[dim];
        if (values.size() != dim)
            System.out.println("Error in LogisticRegression.classify(): wrong size array " + n);
        for (int i = 0; i < values.size(); i++) {
            input[i] = Double.parseDouble(values.get(i));
        }
        double result = sigmoid(dotProduct(betas,input));
        //System.out.println("LogisticRegression.classify(): result: " + result + " values: " + values);
        //System.out.println("LogisticRegression.classify(): betas: " + toStringArrayWithPrecision(betas));
        if (result > 0.5)
            return "1";
        else
            return "0";
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
        double[] alphas = {0, .001, .01, .1};
        //double[] alphas = {0};
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
            System.out.println(lr.lik());

            //Train the model
            lr.train();

            //Display execution info
            System.out.println("Final betas:");
            System.out.println(toStringArrayWithPrecision(lr.betas));
            System.out.println("Final lik:");
            System.out.println(lr.lik());
        }
    }
}
