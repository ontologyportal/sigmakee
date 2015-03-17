package com.articulate.sigma.nlp;

import java.util.*;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.Interpreter;

// code thanks to http://adityamandhare.blogspot.com/2013/02/kmeans-clustering-algorithm-java-code.html

// Hartigan, J. A.; Wong, M. A. (1979). "Algorithm AS 136: A K-Means Clustering Algorithm". 
// Journal of the Royal Statistical Society, Series C 28 (1): 100–108. JSTOR 2346830.

public class KMeans {

    private static int count1,count2,count3;
    private static int k[][];
    private static int tempk[][];
    private static double m[];
    private static double diff[];

    /** ***************************************************************
     * This method will determine the cluster in which an element go at 
     * a particular step.
     */
    private static int calcDiff(int a, int p) {
        
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
     * This method will determine intermediate mean values
     */
    private static void calcMeans(int n, int p) {
        
        for (int i = 0; i < p; ++i)
            m[i] = 0; // initializing means to 0
        int cnt = 0;
        for (int i = 0; i < p; ++i) {
            cnt = 0;
            for (int j = 0; j < n - 1; ++j) {
                if (k[i][j] != -1) {
                    m[i] += k[i][j];
                    ++cnt;
                }
            }
            m[i] = m[i] / cnt;
        }
    }

    /** ***************************************************************
     * This checks if previous k ie. tempk and current k are same.
     * Used as terminating case.
     */
    private static int check1(int n, int p) {
        
        for (int i = 0; i < p; ++i) {
            for (int j = 0; j < n; ++j) {
                if (tempk[i][j] != k[i][j]) {
                    return 0;
                }
            }
        }
        return 1;
    }

    /** ***************************************************************
     */
    public static ArrayList<ArrayList<Integer>> run(int n, int[] d, int p) {
        
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        k = new int[p][n];
        tempk = new int[p][n];
        m = new double[p];
        diff = new double[p];
        /* Initializing m */
        for (int i = 0; i < p; ++i)
            m[i] = d[i];

        int temp=0;
        int flag=0;
        do {
            for (int i = 0; i < p; ++i)
                for (int j = 0; j < n; ++j) {
                    k[i][j] = -1;
                }
            for (int i = 0; i < n; ++i) { // for loop will cal calcDiff(int) for every element.            
                temp = calcDiff(d[i],p);
                if (temp == 0)
                    k[temp][count1++] = d[i];
                else
                    if (temp == 1)
                        k[temp][count2++] = d[i];
                    else
                        if (temp == 2)
                            k[temp][count3++] = d[i]; 
            }
            calcMeans(n,p); // call to method which will calculate mean at this step.
            flag = check1(n,p); // check if terminating condition is satisfied.
            if (flag != 1)
                // Take backup of k in tempk so that you can check for equivalence in next step
                for (int i = 0; i < p; ++i)
                    for (int j = 0; j < n; ++j)
                        tempk[i][j] = k[i][j];

            //System.out.println("\n\nAt this step");
            //System.out.println("\nValue of clusters");
           // for (int i = 0; i < p; ++i) {
                //System.out.print("K" + (i + 1) + "{ ");
                //for (int j = 0; k[i][j] != -1 && j < n - 1; ++j)
                    //System.out.print(k[i][j] + " ");
                //System.out.println("}");
            //}
            //System.out.println("\nValue of m ");
            //for (int i = 0; i < p; ++i)
                //System.out.print("m" + (i + 1) + "=" + m[i] + "  ");

            count1 = 0; count2 = 0; count3 = 0;
        } while (flag == 0);

        //System.out.println("\n\n\nThe Final Clusters By Kmeans are as follows: ");
        for (int i = 0; i < p; ++i) {
            //System.out.print("K" + (i + 1) + "{ ");
            ArrayList<Integer> oneCluster = new ArrayList<Integer>();
            for (int j = 0; k[i][j] != -1 && j < n - 1; ++j) {
                //System.out.print(k[i][j] + " ");
                oneCluster.add(new Integer(k[i][j]));
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
        int[] d = new int[n];
        /* Accepting elements */
        System.out.println("Enter " + n + " elements: ");
        for (int i = 0; i < n; ++i)
            d[i] = scr.nextInt();
        /* Accepting num of clusters */
        System.out.println("Enter the number of clusters: ");
        int p = scr.nextInt();
        /* Initialising arrays */
        ArrayList<ArrayList<Integer>> result = run(n,d,p);
        System.out.println(result);
    }

    /** ***************************************************************
     */
    public static void testKmeans() {
        
        //System.out.println("INFO in KMeans.testKmeans()");
        int n = 8;
        int[] d = new int[] {2, 3, 6, 8, 12, 15, 18, 22};
        int p = 3;
        ArrayList<ArrayList<Integer>> result = run(n,d,p);
        System.out.println(result);
        /* Expected results
        Value of m
        m1=2.5  m2=7.0  m3=16.75

        The Final Clusters By Kmeans are as follows:
        K1{ 2 3 }
        K2{ 6 8 }
        K3{ 12 15 18 22 } */
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
