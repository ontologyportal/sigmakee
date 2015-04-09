package com.articulate.sigma.nlp;

import java.util.*;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.Interpreter;

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
 */
// code thanks to http://adityamandhare.blogspot.com/2013/02/kmeans-clustering-algorithm-java-code.html
// modified to solve fixed number of clusters - APease
// modified to support vectors rather than single values - apease 2015/04/05

// Hartigan, J. A.; Wong, M. A. (1979). "Algorithm AS 136: A K-Means Clustering Algorithm". 
// Journal of the Royal Statistical Society, Series C 28 (1): 100–108. JSTOR 2346830.

public class KMeansVec {

    private static float clusters[][][]; // the set of vectors in each cluster [clusters][vectors][values]
    private static float tempk[][][]; // a temporary copy of clusters
    private static float means[][];  // the mean vector for each cluster [cluster][values]

    /** ***************************************************************
     * Print one vector
     */
    private static void printVector(float[] v) {
        
        System.out.print("{");
        for (int i = 0; i < v.length; i++) {
            if (i != 0)
                System.out.print(", ");
            System.out.print(v[i]);
        }
        System.out.print("}");
    }
    
    /** ***************************************************************
     * Print one cluster
     */
    private static void printOneCluster(float[][] c) {
        
        System.out.print("[");
        for (int i = 0; i < c.length; i++) {
            if (i != 0)
                System.out.print(", ");
            printVector(c[i]);
        }
        System.out.println("]");
    }
    
    /** ***************************************************************
     * Print all clusters
     */
    private static void printClusters(float[][][] c) {
        
        System.out.print("[");
        for (int i = 0; i < c.length; i++) {
            printOneCluster(c[i]);
        }
        System.out.println("]");
    }
    
    /** ***************************************************************
     * This method will determine the Euclidean difference between two vectors
     * @param a is one vector
     * @param b is one vector
     */
    private static float euclid(float[] a, float[] b) {
        
        if (a.length != b.length) {
            System.out.println("Error in KMeansVec.calcDiff(): different lengths for vectors ");
            System.out.print("a: ");
            printVector(a);
            System.out.println();
            System.out.print("b: ");
            printVector(b);
            System.out.println();
        }
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum = sum + (a[i]-b[i]) * (a[i]-b[i]);
        }
        sum = (float) Math.sqrt(sum);
        return sum;
    }
    
    /** ***************************************************************
     * This method will determine the cluster in which an vector should 
     * go at a particular step.
     * @param a is the vector
     * @return the index of the cluster
     */
    private static int findCluster(float[] a, int numClusters, int vecSize) {
        
        float diff[] = new float[numClusters];  // The Euclidean distance between the cluster mean and the 
                                                // given vector
        Arrays.fill(diff, (float) 0);
        for (int i = 0; i < numClusters; i++) {
                diff[i] = euclid(a,means[i]);
        }
        int val = 0;
        float temp = diff[0];
        for (int i = 0; i < numClusters; i++) {
            if (diff[i] < temp) {
                temp = diff[i];
                val = i;
            }
        }
        return val;
    }

    /** ***************************************************************
     * This method will determine mean vector of all vectors in list
     * @param list[vector][values]
     * @param numvec is the number of vectors
     * @param numdim is the number of dimensions of each vector
     * @return the mean vector
     */
    public static float[] mean(float[][] list, int numvec, int numdim) {
    
        float[] mean = new float[numdim]; // the mean vector
        Arrays.fill(mean,0);
        for (int j = 0; j < numdim; j++) { // iterate through the values
            int cnt = 0;
            for (int i = 0; i < numvec; i++) { // iterate through the vectors
                if (list[i][j] != -1) {
                    mean[j] += list[i][j];
                    cnt++;
                }
            }
            if (cnt > 0)
                mean[j] = mean[j] / cnt;
        }
        return mean;
    }
    
    /** ***************************************************************
     * This method will determine intermediate mean values for each
     * cluster
     */
    private static void calcMeans() {
        
        int numClusters = clusters.length;
        for (int i = 0; i < numClusters; i++) {
            int numVectors = clusters[i].length;
            int numDim = clusters[i][0].length;
            means[i] = mean(clusters[i],numVectors,numDim);
        }
    }

    /** ***************************************************************
     * This checks if previous k ie. tempk and current k are same.
     * Used as terminating case.
     */
    private static boolean check1(int n, int numClusters) {
        
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < n; j++) {
                if (tempk[i][j] != clusters[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /** ***************************************************************
     */
    public static ArrayList<ArrayList<ArrayList<Float>>> run(float[][] input, int numClusters) {
        
        int numVectors = input.length;
        int numDim = input[0].length; // the number of "dimensions" in each vector
        int[] counts = new int[numClusters]; // how many vectors are in each cluster
        Arrays.fill(counts, 0);
        ArrayList<ArrayList<ArrayList<Float>>> result = new ArrayList<ArrayList<ArrayList<Float>>>();
        clusters = new float[numClusters][numVectors][numDim];
        tempk = new float[numClusters][numVectors][numDim];
        means = new float[numClusters][numDim];
        Random rand = new Random();
        /* Initializing */
        for (int i = 0; i < numClusters; ++i) {
            means[i] = input[rand.nextInt(numVectors)];
        }

        int temp = 0;
        boolean flag = false;
        do {
            for (int i = 0; i < numClusters; i++) {
                for (int j = 0; j < numVectors; j++) {
                    for (int l = 0; l < numDim; l++) {
                        clusters[i][j][l] = -1;
                    }
                }
            }
            for (int i = 0; i < numVectors; i++) { // for loop will cal calcDiff(int) for every element.            
                temp = findCluster(input[i],numClusters,numDim);
                for (int j = 0; j < numDim; j++)
                    clusters[temp][counts[temp]][j] = input[i][j];
                counts[temp]++;
            }
            flag = check1(numVectors,numClusters); // check if terminating condition is satisfied.
            if (flag != true) {
                // Take backup of k in tempk so that you can check for equivalence in next step
                for (int i = 0; i < numClusters; i++) {
                    for (int j = 0; j < numVectors; j++)
                        tempk[i][j] = clusters[i][j];
                }
            }
            Arrays.fill(counts, 0);
        } while (flag == false);

        for (int i = 0; i < numClusters; i++) {
            ArrayList<ArrayList<Float>> oneCluster = new ArrayList<ArrayList<Float>>();
            for (int j = 0; j < clusters[i].length; j++) {
                ArrayList<Float> oneVector = new ArrayList<Float>();
                for (int v = 0; v < numDim && clusters[i][j][v] != -1; v++) {
                    oneVector.add(new Float(clusters[i][j][v]));
                }
                oneCluster.add(oneVector);
            }
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
        int numVectors = scr.nextInt();
        System.out.println("Enter the number of dimensions ");
        int numDim = scr.nextInt();
        float[][] d = new float[numVectors][numDim];
        /* Accepting elements */
        System.out.println("Enter " + numDim + " numbers: ");
        for (int i = 0; i < numVectors; i++)
            for (int j = 0; j < numDim; j++)
                d[i][j] = scr.nextFloat();
        /* Accepting num of clusters */
        System.out.println("Enter the number of clusters: ");
        int p = scr.nextInt();
        /* Initialising arrays */
        ArrayList<ArrayList<ArrayList<Float>>> result = run(d,p);
        System.out.println(result);
    }

    /** ***************************************************************
     */
    public static void testKmeans() {
        
        float[][] input = new float[][] {{2,2} ,{7,3}, {7,6}, {8,8}, {12,2}, {2,15}, {10,10}, {22,22}};
        int numClusters = 3;
        ArrayList<ArrayList<ArrayList<Float>>> result = run(input,numClusters);
        System.out.println("\n\n\nThe Final Clusters By Kmeans are as follows: ");
        System.out.println(result);
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

