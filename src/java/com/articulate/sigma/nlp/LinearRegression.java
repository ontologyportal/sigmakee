package com.articulate.sigma.nlp;

import com.articulate.sigma.DocGen;

import java.util.ArrayList;

/**
 * Created by apease and ported to Java on 10/6/15.  Original license for the
 * python code is:
 *
 * The MIT License (MIT)

 Copyright (c) 2015 Matt Nedrich

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class LinearRegression {

    // y = mx + b
    // m is slope, b is y-intercept

    /****************************************************************
     */
    public static double compute_error_for_line_given_points(double b, double m, double[][] points) {

        double totalError = 0;
        for (int i = 0; i < points.length; i++) {
            double x = points[i][0];
            double y = points[i][1];
            totalError += (y - (m * x + b)) * (y - (m * x + b));
        }
        return totalError / (double) points.length;
    }

    /****************************************************************
     */
    public static double[] step_gradient(double b_current, double m_current, double[][] points, double learningRate) {

        double b_gradient = 0;
        double m_gradient = 0;
        double N = (double) points.length;
        for (int i = 0; i < points.length; i++) {
            double x = points[i][0];
            double y = points[i][1];
            b_gradient += -(2 / N) * (y - ((m_current * x) + b_current));
            m_gradient += -(2 / N) * x * (y - ((m_current * x) + b_current));
        }
        double new_b = b_current - (learningRate * b_gradient);
        double new_m = m_current - (learningRate * m_gradient);
        double[] result = {new_b, new_m};
        return result;
    }

    /****************************************************************
     */
    public static double[] gradient_descent_runner(double[][] points, double starting_b, double starting_m,
                                            double learning_rate, double num_iterations) {

        double b = starting_b;
        double m = starting_m;
        for (int i = 0; i < num_iterations; i++) {
            double[] bm = step_gradient(b, m, points, learning_rate);
            b = bm[0];
            m = bm[1];
        }
        double[] result = {b, m};
        return result;
    }

    /****************************************************************
     */
    public static void run() {

        DocGen dg = DocGen.getInstance();
        ArrayList<ArrayList<String>> input = dg.readSpreadsheetFile("/home/apease/IPsoft/NB/LRdata.csv", ',');
        input.remove(0);  // eliminate the "start" line that DocGen adds in
        if (input.size() < 1)
            System.out.println("Error no rows in file");
        double[][] points = new double[input.size()][input.get(1).size()];
        for (int i = 0; i < input.size(); i++) {
            ArrayList<String> row = input.get(i);
            System.out.println(row);
            for (int j = 0; j < row.size(); j++)
                points[i][j] = Double.parseDouble(row.get(j));
        }
        double learning_rate = 0.0001;
        double initial_b = 0; // initial y -intercept guess
        double initial_m = 0; // initial slope guess
        double num_iterations = 1000;
        System.out.format("Starting gradient descent at b = {%f}, m = {%f}, error = {%f}\n", initial_b,
                initial_m, compute_error_for_line_given_points(initial_b, initial_m, points));
        System.out.println("Running...");
        double[] bm = gradient_descent_runner(points, initial_b, initial_m, learning_rate, num_iterations);
        System.out.format("After {%f} iterations b = {%f}, m = {%f}, error = {%f}\n", num_iterations, bm[0], bm[1],
                compute_error_for_line_given_points(bm[0], bm[1], points));
    }

    /****************************************************************
     */
    public static void main(String[] args) {

        run();
    }
}
