package com.articulate.sigma.nlp;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by apease on 10/6/15.
 * adapted from http://blog.smellthedata.com/2009/06/python-logistic-regression-with-l2.html
 */
public class LogisticRegression {
    // A simple logistic regression model with L2 regularization (zero-mean Gaussian priors on parameters).

    public double[][] x_train;
    public double[] y_train;
    public double[][] x_test;
    public double[] y_test;
    int n;
    double alpha;
    double[] betas;

    /****************************************************************
     * Create N instances of d dimensional input vectors and a 1D class label (-1 or 1).
     */
    public void generate(int N, int d) {

        Random rnd = new Random();
        rnd.setSeed(0);
        double[][] means = new double[2][d];
        Arrays.fill(means,0.05 * rnd.nextDouble()*d);

        x_train = new double[N][d];
        Arrays.fill(x_train, 0.0);
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
            y_train[i] = 2.0 * y - 1;
        }
        x_test = new double[N][d];
        Arrays.fill(x_test, 0.0);
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
    }

    /****************************************************************
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
        betas = new double[x_train.length];
        Arrays.fill(betas,0.0);
    }

    /****************************************************************
     */
    public double negative_lik() {

        return -1 * lik();
    }

    /****************************************************************
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
     * Likelihood of the data under the current settings of parameters.
     */
    public double lik() {

        // Data likelihood
        double l = 0;
        for (int i = 0; i < n; i++)
            l += Math.log(sigmoid(y_train[i] * dotProduct(betas, x_train[i])));

        //Prior likelihood
        for (int k = 1; k < x_train[0].length; k++)
            l -= (alpha / 2.0) * betas[k] * betas[k];

        return l;
    }

    /****************************************************************
     * Define the derivative of the likelihood with respect to beta_k.
     * Need to multiply by -1 because we will be minimizing.
     */
    public double dB_k(double[] B, int k) {

        double dB_k_result = 0;
        double sum = 0;
        for (int i = 0; i < n; i++)
            sum += y_train[i] * x_train[i][k] * sigmoid(-y_train[i] * dotProduct(B, x_train[i]));
        if (k > 0)
            dB_k_result = alpha * B[k] - sum;

        return dB_k_result;
    }

    /****************************************************************
     * The full gradient is just an array of componentwise derivatives
     */
    public double[] dB(double[] B) {

        double[] result = new double[x_train.length];
        for (int k = 0; k < x_train.length; k++)
            result[k] = dB_k(B, k);
        return result;
    }

    /****************************************************************
     * Define the gradient and hand it off to a scipy gradient-based optimizer.
     */
    public void train() {

        int num_iterations = 1000;
        //Optimize (objective function, initial guess, gradient of f
        for (int i = 0; i < num_iterations; i++)
            betas[i] = 0; // fmin_bfgs(negative_lik, betas, fprime = dB);
    }

    /****************************************************************
     */
    public void set_data(LogisticRegression lr) {
       // Take data that's already been generated.

        this.x_train = lr.x_train;
        this.y_train = lr.y_train;
        this.x_test = lr.x_test;
        this.y_test = lr.y_test;
        n = lr.y_train.length;
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
        for  (int i = 0; i < n; i++)
            p_y1[i] = sigmoid(dotProduct(betas, x_test[i]));

        return p_y1;
    }

    /****************************************************************
     */
    public static void main(String[] args) {

        //Create 20 dimensional data set with 25 points-- this will be
        //susceptible to overfitting.
        LogisticRegression lr1 = new LogisticRegression();
        lr1.generate(25, 20);

        //Run for a variety of regularization strengths
        double[] alphas = {0, .001, .01, .1};
        for (int j = 0 ; j < alphas.length; j++) {
            double a = alphas[j];
            //Create a new learner, but use the same data for each run

            LogisticRegression lr = new LogisticRegression();
            lr.set_data(lr1);
            lr.alpha = a;
            System.out.println("Initial likelihood:");
            System.out.println(lr.lik());

            //Train the model
            lr.train();

            //Display execution info
            System.out.println("Final betas:");
            System.out.println(lr.betas);
            System.out.println("Final lik:");
            System.out.println(lr.lik());
        }
    }
}
