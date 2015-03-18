package com.articulate.sigma.nlp;

import java.util.*;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.Interpreter;

// code thanks to http://adityamandhare.blogspot.com/2013/02/kmeans-clustering-algorithm-java-code.html
// modified to solve fixed number of clusters - APease

// Hartigan, J. A.; Wong, M. A. (1979). "Algorithm AS 136: A K-Means Clustering Algorithm". 
// Journal of the Royal Statistical Society, Series C 28 (1): 100–108. JSTOR 2346830.

public class KMeans {

    private static int[] counts;
    private static float k[][];
    private static float tempk[][];
    private static double m[];
    private static double diff[];

    /** ***************************************************************
     * This method will determine the cluster in which an element should 
     * go at a particular step.
     */
    private static int calcDiff(float a, int p) {
        
        int temp1 = 0;
        for (int i = 0; i < p; ++i) {
            if (a > m[i])
                diff[i] = a - m[i];
            else
                diff[i] = m[i] - a;
        }
        int val = 0;
        double temp = diff[0];
        for (int i = 0; i < p; ++i) {
            if (diff[i] < temp) {
                temp = diff[i];
                val = i;
            }
        }
        return val;
    }

    /** ***************************************************************
     * This method will determine mean values
     */
    public static float mean(float[] list, int n) {
    
        int cnt = 0;
        float mean = 0;
        for (int j = 0; j < n - 1; ++j) {
            if (list[j] != -1) {
                mean += list[j];
                ++cnt;
            }
        }
        return mean / cnt;
    }
    
    /** ***************************************************************
     * This method will determine intermediate mean values
     */
    private static void calcMeans(int n, int p) {
        
       // for (int i = 0; i < p; ++i)
       //     m[i] = 0; // initializing means to 0
        int cnt = 0;
        for (int i = 0; i < p; ++i) {
            m[i] = mean(k[i],n);
        }
    }

    /** ***************************************************************
     * This checks if previous k ie. tempk and current k are same.
     * Used as terminating case.
     */
    private static boolean check1(int n, int p) {
        
        for (int i = 0; i < p; ++i) {
            for (int j = 0; j < n; ++j) {
                if (tempk[i][j] != k[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /** ***************************************************************
     */
    public static ArrayList<ArrayList<Float>> run(int n, float[] d, int p) {
        
        counts = new int[p];
        Arrays.fill(counts, 0);
        ArrayList<ArrayList<Float>> result = new ArrayList<ArrayList<Float>>();
        k = new float[p][n];
        tempk = new float[p][n];
        m = new double[p];
        diff = new double[p];
        /* Initializing m */
        for (int i = 0; i < p; ++i)
            m[i] = d[i];

        int temp = 0;
        boolean flag = false;
        do {
            for (int i = 0; i < p; ++i)
                for (int j = 0; j < n; ++j) {
                    k[i][j] = -1;
                }
            for (int i = 0; i < n; ++i) { // for loop will cal calcDiff(int) for every element.            
                temp = calcDiff(d[i],p);
                k[temp][counts[temp]++] = d[i];
            }
            calcMeans(n,p); // call to method which will calculate mean at this step.
            flag = check1(n,p); // check if terminating condition is satisfied.
            if (flag != true)
                // Take backup of k in tempk so that you can check for equivalence in next step
                for (int i = 0; i < p; ++i)
                    for (int j = 0; j < n; ++j)
                        tempk[i][j] = k[i][j];

            //System.out.println("\n\nAt this step");
            //System.out.println("\nValue of clusters");
            //for (int i = 0; i < p; ++i) {
            //    System.out.print("K" + (i + 1) + "{ ");
            //    for (int j = 0; k[i][j] != -1 && j < n - 1; ++j)
            //        System.out.print(k[i][j] + " ");
                //System.out.println("}");
            //}
            //System.out.println("\nValue of m ");
            //for (int i = 0; i < p; ++i)
            //    System.out.print("m" + (i + 1) + "=" + m[i] + "  ");

            Arrays.fill(counts, 0);
        } while (flag == false);

        //System.out.println("\n\n\nThe Final Clusters By Kmeans are as follows: ");
        for (int i = 0; i < p; ++i) {
            //System.out.print("K" + (i + 1) + "{ ");
            ArrayList<Float> oneCluster = new ArrayList<Float>();
            for (int j = 0; k[i][j] != -1 && j < n - 1; ++j) {
                //System.out.print(k[i][j] + " ");
                oneCluster.add(new Float(k[i][j]));
            }
            //System.out.println("}");
            result.add(oneCluster);
        }
        return result;
    }
    
    /** ***************************************************************
     */
    public static void interact() {
        
        System.out.println("INFO in KMeans.interact()");
        Scanner scr = new Scanner(System.in);
        /* Accepting number of elements */
        System.out.println("Enter the number of elements ");
        int n = scr.nextInt();
        float[] d = new float[n];
        /* Accepting elements */
        System.out.println("Enter " + n + " elements: ");
        for (int i = 0; i < n; ++i)
            d[i] = scr.nextFloat();
        /* Accepting num of clusters */
        System.out.println("Enter the number of clusters: ");
        int p = scr.nextInt();
        /* Initialising arrays */
        ArrayList<ArrayList<Float>> result = run(n,d,p);
        System.out.println(result);
    }

    /** ***************************************************************
     */
    public static void testKmeans() {
        
        //System.out.println("INFO in KMeans.testKmeans()");
        int n = 8;
        float[] d = new float[] {2, 3, 6, 8, 12, 15, 18, 22};
        int p = 3;
        ArrayList<ArrayList<Float>> result = run(n,d,p);
        System.out.println(result);
        System.out.println("Should be: [[2.0, 3.0], [6.0, 8.0], [12.0, 15.0, 18.0, 22.0]]");
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {  

        //System.out.println("INFO in KMeans.main()");
        if (args != null && args.length > 0 && args[0].equals("-i")) {
            interact();
        }
        else if (args != null && args.length > 0 && args[0].equals("-h")) {
            System.out.println("K-means");
            System.out.println("  options:");
            System.out.println("  -h - show this help screen");
            System.out.println("  -i - runs a loop of calculations taking user input ");
        }
        else {
            testKmeans();
        }
    }
}
