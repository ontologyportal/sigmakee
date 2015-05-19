package com.articulate.sigma.nlp;

import java.util.Arrays;

/**
 * Created by apease on 4/24/15.
 */
public class GaussElim {

    /****************************************************************
     */
    private static void printRow(float[] row) {

        for (int j = 0; j < row.length; j++) {
            if (j > 0)
                System.out.print(", ");
            System.out.print(row[j]);
        }
        System.out.println();
    }

    /****************************************************************
     */
    private static void printMatrix(float[][] matrix) {

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (j > 0)
                    System.out.print(", ");
                System.out.print(matrix[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    /****************************************************************
     * Perform Gaussian elimination on a set of simultaneous equations
     */
    private static float[] gaussianElim(float[][] matrix) {

        int n = matrix.length; // number of rows
        int m = matrix[0].length; // number of columns
        float[] x = new float[n];
        //System.out.println("# rows: " + n + " # columns: " + m);
        if (n + 1 != m)
            //System.out.println("Error in SVM.gaussianElim(): m != n+1");
        for (int row1 = 0; row1 < n-1; row1++) { // looking by row
            //printRow(matrix[k]);
            for (int row2 = row1 + 1; row2 < n; row2++) { // looking by row
                if (matrix[row1][row1] == 0) {
                    //System.out.println("Error in GaussElim.gaussianElim(): Singular matrix!");
                    return null;
                }

                float divisor = matrix[row2][row1] / matrix[row1][row1];
                //System.out.println("divisor: " + divisor);
                //Do for all remaining elements in current row:
                for (int j = row1; j < m; j++) { // looking by column
                    //System.out.println("looking at element row: " + row2 + " column: " + j + " with value: " + matrix[row2][j]);
                    matrix[row2][j] = matrix[row2][j] - (matrix[row1][j] * divisor);
                }
            }
            //printMatrix(matrix);
        }
        //System.out.println();
        //System.out.println("Backward substitution");
        for (int row1 = n - 1; row1 > -1; row1--) {
            //System.out.println("Pivot row: " + row1);
            for (int row2 = row1-1; row2 > -1; row2--) {
                //System.out.println("Modifying row: " + row2);
                float divisor = matrix[row2][row1] / matrix[row1][row1];
                //System.out.println("divisor: " + divisor);
                for (int j = row1; j < m; j++) {
                    //System.out.println("looking at element row: " + row2 + " column: " + j + " with value: " + matrix[row2][j]);
                    matrix[row2][j] = matrix[row2][j] - matrix[row1][j] * divisor;
                }
            }
            //printMatrix(matrix);
            x[row1] = matrix[row1][m-1] / matrix[row1][row1];
        }
        //printMatrix(matrix);
        //printRow(x);
        return x;
    }

    /** **************************************************************
     * test method
     */
    public static void main(String[] args) {

        float[][] matrix = {{2, 4,  4, -1},
                          {4, 11, 9, 1},
                          {4, 9, 11, 1}};
       // float[][] matrix = {{2, 1, -1, 8},
       //         {-3, -1, 2, -11},
       //         {-2, 1, 2, -3}};
        // answer is 2,3,-1
        printMatrix(matrix);
        gaussianElim(matrix);
    }
}