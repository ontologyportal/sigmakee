package com.articulate.sigma.nlp;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.rmi.*;
import java.math.*;

/** ***************************************************************
 * SVM-JAVA: A simple Java implementation of SMO (Sequential Minimal Optimization) for training SVM

 Developed by
 Xiaoqian Jiang (xiaoqian@cs.cmu.edu)
 Hwanjo Yu (hwanjoyu@postech.ac.kr)

 Department of Computer Science and Engineering, Pohang University of Science and Technology (POSTECH),
 http://iis.hwanjoyu.org/svm-java, 2008
 [note that current URL is https://sites.google.com/site/postechdm/research/implementation/svm-java ]

     Bibtex entry:
     @MISC{POSTECH08svm-java,
     author = "X. Jiang and H. Yu",
     title = "{SVM-JAVA}: A Java Implementation of the {SMO} (Sequential Minimal Optimization) for training {SVM}",
     institution = "Department of Computer Science and Engineering, Pohang University of Science and Technology (POSTECH)",
     address = "http://iis.hwanjoyu.org/svm-java",
     year = "2008"
     }
 * Edited by apease on 4/21/15.
 */
public class SVM {

    class sparse_binary_vector {
        Vector id = new Vector();
    }

    class sparse_vector {
        Vector id = new Vector();
        Vector val =  new Vector();
    }

    public int N = 0;                    /* N points(rows) */
    public int d = -1;                   /* d variables */
    public float C = (float) 0.05;
    public float tolerance = (float) 0.001;
    public float eps = (float) 0.001;
    public float two_sigma_squared = 2;
    public int MATRIX = 4000;

    Vector alph = new Vector();           /* Lagrange multipliers */
    float b = 0;                          /* threshold */
    Vector w =  new Vector();             /* weight vector: only for linear kernel */

    Vector error_cache = new Vector();

    Vector dense_vector =  new Vector();

    public boolean is_sparse_data = false;
    public boolean is_binary = false;

    public boolean is_libsvm_file = true;

    int learned_func_flag = -1;
    int dot_product_flag = -1;
    int kernel_flag = -1;

    Vector sparse_binary_points =  new Vector();

    Vector sparse_points =  new Vector();

    float dense_points[][] = new float[MATRIX][MATRIX];

    Vector target = new Vector();

    boolean is_test_only = false;
    boolean is_linear_kernel = false;

    /* data points with index in [first_test_i .. N)
     * will be tested to compute error rate
     */
    int first_test_i = 0;

    /*
     * support vectors are within [0..end_support_i)
     */
    int end_support_i = -1;

    float delta_b = 0;

    Vector precomputed_self_dot_product = new Vector();

    float precomputed_dot_product[][] = new float[MATRIX][MATRIX];

    /** ***************************************************************
     * Initialize the matrix with 0's
     */
    public SVM() {

        for (int i = 0; i < MATRIX; i++)
            for (int j = 0; j < MATRIX; j++)
                dense_points[i][j] = 0;
    }

    /** ***************************************************************
     */
    private float object2float(Object o) {

        Float result = (Float) o;
        return result.floatValue();
    }

    /** ***************************************************************
     */
    private int object2int(Object o) {

        Integer result = (Integer) o;
        return result.intValue();
    }

    /** ***************************************************************
     */
    private void setVector(Vector v, int location, float value) {

        Float result = new Float(value);
        v.set(location,result);
    }

    /** ***************************************************************
     */
    private void setVector(Vector v, int location, int value) {

        Integer result = new Integer(value);
        v.set(location,result);
    }

    /** ***************************************************************
     */
    private float getFloatValue(Vector v, int location) {

        Float result = (Float) v.elementAt(location);
        return result.floatValue();
    }

    /** ***************************************************************
     */
    private int getIntValue(Vector v, int location) {

        Integer result = (Integer) v.elementAt(location);
        return result.intValue();
    }

    /** ***************************************************************
     */
    private int examineExample(int i1) {

        float y1 = 0, alph1 = 0, E1 = 0, r1 = 0;
        y1 = object2int(target.elementAt(i1));
        alph1 = object2float(alph.elementAt(i1));

        if (alph1 > 0 && alph1 < C)
            E1 = object2float(error_cache.elementAt(i1));
        else
            E1 = learned_func(i1,learned_func_flag) - y1;

        r1 = y1 * E1;
        if ((r1 < -tolerance && alph1 < C) || (r1 > tolerance && alph1 > 0)) {
            {
                int k = 0, i2 = 0;
                float tmax = 0;

                for (i2 = (-1), tmax = 0, k = 0; k < end_support_i; k++)
                    if (object2float(alph.elementAt(k)) > 0 && object2float(alph.elementAt(k)) < C) {
                        float E2 = 0, temp = 0;

                        E2 = object2float(error_cache.elementAt(k));
                        temp = Math.abs(E1 - E2);
                        if (temp > tmax) {
                            tmax = temp;
                            i2 = k;
                        }
                    }

                if (i2 >= 0) {
                    if (takeStep (i1, i2) == 1) {
                        return 1;
                    }
                }
            }
            float rands = 0;
            {
                int k = 0, k0 = 0;
                int i2 = 0;
                for (rands = (float) Math.random(), k0 = (int) (rands * end_support_i), k = k0; k < end_support_i + k0; k++) {
                    i2 = k % end_support_i;
                    if (object2float(alph.elementAt(i2)) > 0 && object2float(alph.elementAt(i2)) < C) {
                        if (takeStep(i1, i2) == 1) {
                            return 1;
                        }
                    }
                }
            }

            {
                int k0 = 0, k = 0, i2 = 0;
                rands = 0;

                for (rands = (float) Math.random(),k0 = (int)(rands * end_support_i), k = k0; k < end_support_i + k0; k++) {
                    i2 = k % end_support_i;
                    if (takeStep(i1, i2) == 1) {
                        return 1;
                    }
                }
            }
        }
        return 0;
    }


    /** ***************************************************************
     */
    private int takeStep(int i1, int i2) {

        int y1 = 0, y2 = 0, s = 0;
        float alph1 = 0, alph2 = 0; /* old_values of alpha_1, alpha_2 */
        float a1 = 0, a2 = 0;       /* new values of alpha_1, alpha_2 */
        float E1 = 0, E2 = 0, L = 0, H = 0, k11 = 0, k22 = 0, k12 = 0, eta = 0, Lobj = 0, Hobj = 0;

        if (i1 == i2) return 0;

        alph1 = object2float(alph.elementAt(i1));
        y1 = object2int(target.elementAt(i1));
        if (alph1 > 0 && alph1 < C)
            E1 = object2float(error_cache.elementAt(i1));
        else
            E1 = learned_func(i1,learned_func_flag) - y1;

        alph2 = object2float(alph.elementAt(i2));
        y2 = object2int(target.elementAt(i2));
        if (alph2 > 0 && alph2 < C)
            E2 = object2float(error_cache.elementAt(i2));
        else
            E2 = learned_func(i2,learned_func_flag) - y2;

        s = y1 * y2;

        if (y1 == y2) {
            float gamma = alph1 + alph2;
            if (gamma > C) {
                L = gamma-C;
                H = C;
            }
            else {
                L = 0;
                H = gamma;
            }
        }
        else {
            float gamma = alph1 - alph2;
            if (gamma > 0) {
                L = 0;
                H = C - gamma;
            }
            else {
                L = -gamma;
                H = C;
            }
        }

        if (L == H) {
            return 0;
        }

        k11 = kernel_func(i1, i1,kernel_flag);
        k12 = kernel_func(i1, i2,kernel_flag);
        k22 = kernel_func(i2, i2,kernel_flag);
        eta = 2 * k12 - k11 - k22;

        if (eta < 0) {
            a2 = alph2 + y2 * (E2 - E1) / eta;
            if (a2 < L)
                a2 = L;
            else if (a2 > H)
                a2 = H;
        }
        else {
            {
                float c1 = eta/2;
                float c2 = y2 * (E1-E2)- eta * alph2;
                Lobj = c1 * L * L + c2 * L;
                Hobj = c1 * H * H + c2 * H;
            }
            if (Lobj > Hobj+eps)
                a2 = L;
            else if (Lobj < Hobj-eps)
                a2 = H;
            else
                a2 = alph2;
        }

        if (Math.abs(a2-alph2) < eps*(a2+alph2+eps))
            return 0;

        a1 = alph1 - s * (a2 - alph2);
        if (a1 < 0) {
            a2 += s * a1;
            a1 = 0;
        }
        else if (a1 > C) {
            float t = a1-C;
            a2 += s * t;
            a1 = C;
        }

        {
            float b1 = 0, b2 = 0, bnew = 0;

            if (a1 > 0 && a1 < C)
                bnew = b + E1 + y1 * (a1 - alph1) * k11 + y2 * (a2 - alph2) * k12;
            else {
                if (a2 > 0 && a2 < C)
                    bnew = b + E2 + y1 * (a1 - alph1) * k12 + y2 * (a2 - alph2) * k22;
                else {
                    b1 = b + E1 + y1 * (a1 - alph1) * k11 + y2 * (a2 - alph2) * k12;
                    b2 = b + E2 + y1 * (a1 - alph1) * k12 + y2 * (a2 - alph2) * k22;
                    bnew = (b1 + b2) / 2;
                }
            }
            delta_b = bnew - b;
            b = bnew;
        }

        if (is_linear_kernel) {
            float t1 = y1 * (a1 - alph1);
            float t2 = y2 * (a2 - alph2);

            if (is_sparse_data && is_binary) {
                int p1 = 0, num1 = 0, p2 = 0, num2 = 0;

                num1 = ((sparse_binary_vector) sparse_binary_points.elementAt(i1)).id.size();

                for (p1 = 0; p1 < num1; p1++) {
                    int temp0 = object2int(((sparse_binary_vector) sparse_binary_points.elementAt(i1)).id.elementAt(p1));
                    float temp = object2float(w.elementAt(temp0));
                    w.set(temp0,new Float(temp + t1));
                }
                num2 = ((sparse_binary_vector) sparse_binary_points.elementAt(i2)).id.size();

                for (p2 = 0; p2 < num2; p2++)  {
                    int temp0 = object2int(((sparse_binary_vector) sparse_binary_points.elementAt(i2)).id.elementAt(p2));
                    float temp = object2float(w.elementAt(temp0));
                    w.set(temp0,new Float(temp + t2));
                }
            }
            else if (is_sparse_data && !is_binary) {
                int p1 = 0,num1 = 0,p2 = 0,num2 = 0;
                num1 = ((sparse_vector)sparse_points.elementAt(i1)).id.size();

                for (p1 = 0; p1 < num1; p1++) {
                    int temp1 = object2int(((sparse_vector) sparse_points.elementAt(i1)).id.elementAt(p1));
                    float temp = object2float(w.elementAt(temp1));
                    float temp2 = object2float(((sparse_vector) sparse_points.elementAt(i1)).val.elementAt(p1));
                    w.set(temp1,new Float(temp + t1 * temp2));
                }

                num2 = ((sparse_vector)sparse_points.elementAt(i2)).id.size();

                for (p2 = 0; p2 < num2; p2++) {
                    int temp1 = object2int(((sparse_vector) sparse_points.elementAt(i2)).id.elementAt(p2));
                    float temp = object2float(w.elementAt(temp1));
                    float temp2 = object2float(((sparse_vector) sparse_points.elementAt(i2)).val.elementAt(p2));
                    temp = temp + t2 * temp2;
                    Float value = new Float(temp);
                    w.set(temp1,value);
                }
            }
            else
                for (int i = 0; i < d; i++) {
                    float temp = dense_points[i1][i] * t1 + dense_points[i2][i] * t2;;
                    float temp1 = object2float(w.elementAt(i));
                    Float value = new Float(temp + temp1);
                    w.set(i,value);
                }
        }

        {
            float t1 = y1 * (a1 - alph1);
            float t2 = y2 * (a2 - alph2);

            for (int i = 0; i < end_support_i; i++)
                if (0 < object2float(alph.elementAt(i)) && object2float(alph.elementAt(i)) < C) {
                    float tmp = object2float(error_cache.elementAt(i));
                    tmp += t1 * kernel_func(i1,i,kernel_flag) + t2 * kernel_func(i2,i,kernel_flag)
                            - delta_b;
                    error_cache.set(i,new Float(tmp));
                }
            error_cache.set(i1,new Float(0));
            error_cache.set(i2,new Float(0));
        }
        alph.set(i1,new Float(a1));
        alph.set(i2,new Float(a2));

        return 1;
    }

    /** ***************************************************************
     */
    private float learned_func_linear_sparse_binary(int k) {

        float s = 0;
        int temp =0;
        for (int i = 0; i < ((sparse_binary_vector) sparse_binary_points.elementAt(k)).id.size(); i++) {
            temp = object2int(((sparse_binary_vector) sparse_binary_points.elementAt(i)).id.elementAt(i));
            s += object2float(w.elementAt(temp));
        }
        s -= b;
        return s;
    }

    /** ***************************************************************
     */
    private float learned_func_linear_sparse_nonbinary(int k) {

        float s = 0;
        for (int i = 0; i < ((sparse_vector) sparse_points.elementAt(k)).id.size(); i++) {
            int j = object2int (((sparse_vector) sparse_points.elementAt(k)).id.elementAt(i));
            float v = object2float (((sparse_vector) sparse_points.elementAt(k)).val.elementAt(i));
            s += object2float(w.elementAt(j)) * v;
        }
        s -= b;
        return s;
    }

    /** ***************************************************************
     */
    private float learned_func_linear_dense(int k) {

        float s = 0;
        for (int i = 0; i < d; i++)
            s += object2float(w.elementAt(i)) * dense_points[k][i];
        s -= b;
        return s;
    }

    /** ***************************************************************
     */
    private float learned_func_nonlinear(int k) {

        float s = 0;
        for (int i = 0; i < end_support_i; i++)
            if (object2float(alph.elementAt(i)) > 0) {
                s += object2float(alph.elementAt(i)) * object2int(target.elementAt(i)) * kernel_func(i,k,kernel_flag);
            }
        s -= b;
        return s;
    }

    /** ***************************************************************
     */
    private float dot_product_sparse_binary(int i1, int i2) {

        int p1 = 0, p2 = 0, dot = 0;
        int num1 = ((sparse_binary_vector) sparse_binary_points.elementAt(i1)).id.size();
        int num2 = ((sparse_binary_vector) sparse_binary_points.elementAt(i2)).id.size();

        while (p1 < num1 && p2 < num2) {
            int a1 = object2int(((sparse_binary_vector) sparse_binary_points.elementAt(i1)).id.elementAt(p1));
            int a2 = object2int(((sparse_binary_vector) sparse_binary_points.elementAt(i2)).id.elementAt(p2));
            if (a1 == a2) {
                dot++;
                p1++;
                p2++;
            }
            else if (a1 > a2)
                p2++;
            else
                p1++;
        }
        return (float)dot;
    }

    /** ***************************************************************
     */
    private float dot_product_sparse_nonbinary(int i1, int i2) {

        int p1 = 0, p2 = 0;
        float dot = 0;
        int num1 = ((sparse_vector) sparse_points.elementAt(i1)).id.size();
        int num2 = ((sparse_vector) sparse_points.elementAt(i2)).id.size();

        while (p1 < num1 && p2 < num2) {
            int a1 = object2int(((sparse_vector) sparse_points.elementAt(i1)).id.elementAt(p1));
            int a2 = object2int(((sparse_vector) sparse_points.elementAt(i2)).id.elementAt(p2));
            if (a1 == a2) {
                float val1 = object2float(((sparse_vector) sparse_points.elementAt(i1)).val.elementAt(p1));
                float val2 = object2float(((sparse_vector) sparse_points.elementAt(i2)).val.elementAt(p2));

                dot += val1 * val2;
                p1++;
                p2++;
            }
            else if (a1 > a2)
                p2++;
            else
                p1++;
        }
        return (float) dot;
    }

    /** ***************************************************************
     */
    private float dot_product_dense(int i1, int i2) {

        float dot = 0;
        for (int i = 0; i < d; i++)
            dot += dense_points[i1][i] * dense_points[i2][i];

        return dot;
    }

    /** ***************************************************************
     */
    private float rbf_kernel(int i1, int i2) {

        float s = this.precomputed_dot_product[i1][i2];
        s *= -2;
        s += object2float(precomputed_self_dot_product.elementAt(i1))
                + object2float(precomputed_self_dot_product.elementAt(i2));
        return (float)Math.exp((float)(-s / two_sigma_squared));
    }

    /** ***************************************************************
     * Read the data from a stream
     * sparse data is in the form
     *   <label> <index1>:<value1> <index2>:<value2>
     *       For classification, <label> is an integer indicating the class label
     *       <index> is an integer starting from 1 and <value> is a real number.
     */
    private int read_data(DataInputStream is) {

        String s = new String();
        int n_lines = 0;
        try {
            for (n_lines = 0; (s = is.readLine()) != null; n_lines++) {
                StringTokenizer st = new StringTokenizer(s," \t\n\r\f:");
                Vector<Float> v = new Vector<Float>();
                float t = 0;

                //int g = 0;
                try {
                    while (st.hasMoreTokens()) {
                        float tmp = Float.valueOf(st.nextToken()).floatValue();
                        v.add(new Float(tmp));
                        //g++;
                    }
                }
                catch (NumberFormatException e) {
                    System.err.println("Number format error " + e.toString());
                }

                int tar = 0;
                int n = 0;

                if (this.is_libsvm_file && is_sparse_data ) {
                    tar = Float.valueOf(v.firstElement().toString()).intValue();
                    target.add(new Integer (tar));

                    if (!this.is_binary) {
                        if (d < Float.valueOf(v.elementAt(v.size() - 2).toString()).intValue())
                            d = Float.valueOf(v.elementAt(v.size() - 2).toString()).intValue();
                    }
                    else {
                        if (d < Float.valueOf(v.elementAt(v.size() - 1).toString()).intValue())
                            d = Float.valueOf(v.elementAt(v.size() - 1).toString()).intValue();
                    }
                    v.remove(0);
                    n = v.size();
                }
                else {
                    tar = Float.valueOf(v.lastElement().toString()).intValue();
                    target.add(new Integer (tar));
                    v.remove(v.size() - 1);
                    n = v.size();
                }
                if (is_sparse_data && is_binary ) {
                    sparse_binary_vector x = new sparse_binary_vector();
                    for (int i = 0; i < n; i++) {
                        if (object2float(v.elementAt(i)) < 1 || object2float(v.elementAt(i)) > d) {
                            int line2 = n_lines + 1;
                            System.out.println("error: line " + line2 + ": attribute index "+ (int)object2float(v.elementAt(i))+ " out of range.\n");
                            System.exit(1);
                        }
                        x.id.add(new Integer((int)object2float(v.elementAt(i)) - 1));
                    }
                    sparse_binary_points.add(x);
                }
                else if (is_sparse_data && !is_binary) {
                    sparse_vector x = new sparse_vector();

                    if (this.is_libsvm_file) {
                        for (int i = 0; i < n; i += 2) {
                            if (object2float(v.elementAt(i)) < 1 || object2float(v.elementAt(i)) > d) {
                                int line3 = n_lines + 1;
                                System.out.println("data file error: line " + line3 + ": attribute index " + (int) object2float(v.elementAt(i)) + " out of range.\n");
                                System.exit(1);
                            }
                            int id = (int) object2float(v.elementAt(i)) - 1;
                            float value = (float) object2float(v.elementAt(i + 1));
                            x.id.add(new Integer(id));
                            x.val.add(new Float(value));
                        }
                        sparse_points.add(x);
                    }

                    else {
                        for (int i = 0; i < n; i += 2) {
                            if (object2float(v.elementAt(i)) < 1 || object2float(v.elementAt(i)) > d) {
                                int line3 = n_lines + 1;
                                System.out.println("data file error: line " + line3 + ": attribute index " + (int) object2float(v.elementAt(i)) + " out of range.\n");
                                System.exit(1);
                            }
                            int id = (int) object2float(v.elementAt(i)) - 1;
                            float value = (float) object2float(v.elementAt(i + 1));
                            x.id.add(new Integer(id));
                            x.val.add(new Float(value));
                        }
                        sparse_points.add(x);
                    }
                }
                else {
                    if (v.size() != d) {
                        int line4 = n_lines + 1;
                        System.out.println("Data file error: line " + line4 + " has " + v.size() + " attributes; should be d=" + d);
                        System.exit(1);
                    }
                    for (int i = 0; i < d; i++) {
                        dense_points[N][i] = object2float(v.elementAt(i));
                    }
                    N = N + 1;
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return n_lines;
    }

    /** ***************************************************************
     * Write the SVM to a stream
     *
     * 	The output order of the model paramters will be
     *   1. The number of attributes d.
     *   2. The flag is-sparse-data
     *   3. The flag is-binary
     *   4. The flag is-linear-kernel
     *   5. The threshold b
     *   6. If the linear kernel is used:
     *      (a) The weight vector w
     *   7. If non-linear kernel is used
     *      (a) Kernel parameters
     *      (b) The number of support vectors
     *      (c) The Lagrange multipliers of the support vectors
     *      (d) The support vectors, one per line
     */
    private void write_svm(PrintStream os) {

        os.println(d);
        os.println(is_sparse_data);
        os.println(is_binary);
        os.println(is_linear_kernel);
        os.println(b);
        if (is_linear_kernel) {
            for (int i = 0; i < d; i++)
                os.println(object2float(w.elementAt(i)));
        }
        else {
            os.println(two_sigma_squared);
            int n_support_vectors = 0;
            for (int i = 0; i < end_support_i; i++)
                if (object2float(alph.elementAt(i)) > 0)
                    n_support_vectors++;
            os.println(n_support_vectors);

            for (int i = 0; i < end_support_i; i++)
                if (object2float(alph.elementAt(i)) > 0)
                    os.println(object2float(alph.elementAt(i)));

            for (int i = 0; i < end_support_i; i++) {
                if (object2float(alph.elementAt(i)) > 0) {
                    if (is_sparse_data && is_binary) {
                        os.print(object2int(target.elementAt(i)));
                        os.print(" ");
                        for (int j = 0; j < ((sparse_binary_vector) sparse_binary_points.elementAt(i)).id.size(); j++) {
                            os.print(object2int(((sparse_binary_vector) sparse_binary_points.elementAt(i)).id.elementAt(j)) + 1);
                            os.print(" ");
                        }
                    }
                    else if (is_sparse_data && !is_binary) {
                        os.print(object2int(target.elementAt(i)));
                        os.print(" ");
                        for (int j = 0; j < ((sparse_vector) sparse_points.elementAt(i)).id.size(); j++) {
                            int id = object2int(((sparse_vector) sparse_points.elementAt(i)).id.elementAt(j)) + 1;
                            float value = object2float(((sparse_vector) sparse_points.elementAt(i)).val.elementAt(j));
                            os.print(id + " " + value + " ");
                        }
                    }
                    else {
                        for (int j = 0; j < d; j++) {
                            os.print(dense_points[i][j]);
                            os.print(" ");
                        }
                        os.print(object2int(target.elementAt(i)));
                    }
                    os.print("\n");
                }
            }
        }
    }

    /** ***************************************************************
     */
    private int read_svm(DataInputStream is) {

        try {
            d = Integer.valueOf(is.readLine().toString()).intValue();
            is_sparse_data = Boolean.valueOf(is.readLine().toString()).booleanValue();
            is_binary = Boolean.valueOf(is.readLine().toString()).booleanValue();
            is_linear_kernel = Boolean.valueOf(is.readLine().toString()).booleanValue();
            b = Float.valueOf(is.readLine().toString()).floatValue();

			/*System.out.println("Finnished reading first few flags ...");
			 System.out.println("d = " + this.d);
			 System.out.println("is_sparse_data = " + this.is_sparse_data);
			 System.out.println("is_binary = " + this.is_binary);
			 System.out.println("is_linear_kernel = " + this.is_linear_kernel);*/
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (is_linear_kernel) {
            resize(w,d,2);
            for (int i = 0; i < d; i++) {
                try {
                    float weight =	Float.valueOf(is.readLine().toString()).floatValue();
                    w.set(i,new Float(weight));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            try {
                two_sigma_squared = Float.valueOf(is.readLine().toString()).floatValue();
                int n_support_vectors =0;
                n_support_vectors = Integer.valueOf(is.readLine().toString()).intValue();

                resize(alph,n_support_vectors,2);

                for (int i = 0; i < n_support_vectors;i++) {
                    float value = Float.valueOf(is.readLine().toString()).floatValue();
                    alph.set(i,new Float(value));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return read_data(is);
        }
        return 0;
    }

    /** ***************************************************************
     */
    private float error_rate() {

        int n_total = 0;
        int n_error = 0;
        for (int i = first_test_i; i < N; i++) {
            if ((learned_func(i,learned_func_flag) > 0) != (object2int(target.elementAt(i)) > 0))
                n_error++;
            n_total++;
        }
        return (float) n_error / (float) n_total;
    }

    /** ***************************************************************
     */
    private float dot_product_func(int i,int j,int flag) {

        float result = 0;
        if (flag == 1)
            result = dot_product_sparse_binary(i,j);
        else if (flag == 2)
            result = dot_product_sparse_nonbinary(i,j);
        else if (flag == 3)
            result = dot_product_dense(i,j);

        return result;
    }

    /** ***************************************************************
     */
    private float learned_func(int i, int flag) {

        float result = 0;
        if (flag == 1)
            result = learned_func_linear_sparse_binary(i);
        else if (flag == 2)
            result = learned_func_linear_sparse_nonbinary(i);
        else if (flag == 3)
            result = learned_func_linear_dense(i);
        else if (flag == 4)
            result = learned_func_nonlinear(i);
        return result;
    }

    /** ***************************************************************
     */
    private float kernel_func(int i, int j, int flag) {

        float result =0;
        if (flag == 1)
            result = dot_product_func(i,j,this.dot_product_flag);
        else if (flag == 2)
            result = rbf_kernel(i,j);
        return result;
    }

    /** ***************************************************************
     */
    private void resize(Vector v, int newSize, int type) {

        int original = v.size();
        if (original > newSize) {
            v.setSize(newSize);
            return;
        }
        for (int i = original; i < newSize; i++) {
            if (type == 1)
                v.add(new Integer(0));
            else if (type == 2)
                v.add(new Float(0));
        }
    }

    /** ***************************************************************
     */
    private void reserve (Vector v, int size, int type) {

        for (int i = 0; i < size; i++) {
            if (type == 1)
                v.add(i,new Integer(0));
            else if (type == 2)
                v.add(i,new Float(0));
        }
    }

    /** ***************************************************************
     */
    private void reserveSparse(Vector v, int size) {

        for (int i = 0; i < size; i++) {
            v.add(i,new sparse_vector());
        }
    }

    /** ***************************************************************
     */
    private void reserveSparseBinary(Vector v, int size) {

        for (int i = 0; i < size; i++)
            v.add(i,new sparse_binary_vector());
    }

    /** ***************************************************************
     */
    private void reserve (float[][] array, int size) {

        for (int i = 0; i < size; i++)
            for (int j = 0; j < d;j++)
                array[i][j] = 0;
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        long time, newTime;
        time = System.currentTimeMillis();
        try {
            String data_file_name = "java-svm.data";
            String svm_file_name = "java-svm.model";
            String output_file_name = "java-svm.output";
            SVM my = new SVM();
            int numChanged = 0;
            int examineAll = 0;

            {
                GetOpt go =  new GetOpt(args,"n:d:c:t:e:p:f:m:o:r:lsbai");
                go.optErr = true;
                int ch = -1;
                int errflg = 0;
                while ((ch = go.getopt()) != go.optEOF)
                    switch (ch) {
                        case 'n':
                            my.N = go.processArg(go.optArgGet(),my.N);
                            break;
                        case 'd':
                            my.d = go.processArg(go.optArgGet(),my.d);
                            break;
                        case 'c':
                            my.C = go.processArg(go.optArgGet(),my.C);
                            break;
                        case 't':
                            my.tolerance = go.processArg(go.optArgGet(),my.tolerance);
                            break;
                        case 'e':
                            my.eps = go.processArg(go.optArgGet(),my.eps);
                            break;
                        case 'p':
                            my.two_sigma_squared = go.processArg(go.optArgGet(),my.two_sigma_squared);
                            break;
                        case 'f':
                            data_file_name = go.optArgGet();
                            break;
                        case 'm':
                            svm_file_name = go.optArgGet();
                            break;
                        case 'o':
                            output_file_name = go.optArgGet();
                            break;
                        case 'r':
                            System.out.println("Random");
                            break;
                        case 'l':
                            my.is_linear_kernel = true;
                            break;
                        case 's':
                            my.is_sparse_data = true;
                            break;
                        case 'b':
                            my.is_binary = true;
                            my.is_sparse_data =true;
                            break;
                        case 'a':
                            my.is_test_only = true;
                            break;
                        case 'i':
                            my.is_libsvm_file = true;
                            break;
                        case '?':
                            errflg++;
                    }
                if (errflg > 0) {
                    System.out.println("usage: " + args[0] + " " +
                            "\n-f  data_file_name\n" +
                            "-m  svm_file_name\n"  +
                            "-o  output_file_name\n" +
                            "-n  N\n" +
                            "-d  d\n" +
                            "-c  C\n" +
                            "-t  tolerance\n" +
                            "-e  epsilon\n" +
                            "-p  two_sigma_squared\n" +
                            //"-r  random_seed\n" +
                            "-l  (is_linear_kernel)\n"+
                            "-s  (is_sparse_data)\n" +
                            "-b  (is_binary)\n" +
                            "-a  (is_test_only)\n" );
                    //  "-i  (is_libsvm_file)\n");
                    System.exit(2);
                }
            }

            {
                int n = 0;
                if (my.is_test_only) {
                    try {
                        FileInputStream svm = new FileInputStream(svm_file_name);
                        DataInputStream svm_file = new DataInputStream(svm);
                        my.end_support_i = my.first_test_i = n = my.read_svm(svm_file);
                        // my.N += n;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (!my.is_test_only)
                    if (my.N > 0) {
                        my.reserve(my.target,my.N,1);

                        if (my.is_sparse_data && my.is_binary)
                            my.reserveSparseBinary(my.sparse_binary_points,my.N);
                        else if (my.is_sparse_data && !my.is_binary) {
                            my.reserveSparse(my.sparse_points,my.N);
                        }
                        else {
                            my.reserve(my.dense_points,my.N);
                        }
                    }

                System.out.println(data_file_name);

                FileInputStream data = new FileInputStream(data_file_name);
                DataInputStream data_file = new DataInputStream (data);
                n = my.read_data(data_file);

                if (my.is_test_only) {
                    my.N = my.first_test_i + n;
                }
                else {
                    my.N = n;
                    my.first_test_i = 0;
                    my.end_support_i = my.N;
                }
            }

            if (!my.is_test_only) {
                my.resize(my.alph,my.end_support_i,2);
                my.b = 0;
                my.resize(my.error_cache,my.N,2);
                if (my.is_linear_kernel)
                    my.resize(my.w,my.d,2);
            }

            if (my.is_linear_kernel && my.is_sparse_data && my.is_binary)
                my.learned_func_flag = 1;
            if (my.is_linear_kernel && my.is_sparse_data && !my.is_binary)
                my.learned_func_flag = 2;
            if (my.is_linear_kernel && !my.is_sparse_data)
                my.learned_func_flag = 3;
            if (!my.is_linear_kernel)
                my.learned_func_flag = 4;

            if (my.is_sparse_data && my.is_binary)
                my.dot_product_flag = 1;
            if (my.is_sparse_data && !my.is_binary)
                my.dot_product_flag = 2;
            if (!my.is_sparse_data)
                my.dot_product_flag = 3;

            if (my.is_linear_kernel)
                my.kernel_flag = 1;

            if (!my.is_linear_kernel)
                my.kernel_flag = 2;
            /***************************************************************************/
            //	System.out.println("All flags " + "dot flag "+ my.dot_product_flag + ",kernel flag " +my.kernel_flag+ ",learn flag " + my.learned_func_flag);
            /***************************************************************************/
            if (!my.is_linear_kernel) {
                my.resize(my.precomputed_self_dot_product,my.N,2);

                for (int i = 0; i < my.N; i++)
                    for (int j = 0; j < my.N; j++) {
                        if (i != j)
                            my.precomputed_dot_product[i][j] = my.dot_product_func(i,j,my.dot_product_flag);
                        else {
                            float temp = my.dot_product_func(i,i,my.dot_product_flag);
                            my.precomputed_self_dot_product.set(i,new Float(temp));
                            my.precomputed_dot_product[i][i] =temp;
                        }
                    }
            }
            if (!my.is_test_only) {
                numChanged = 0;
                examineAll = 1;
                while (numChanged > 0 || examineAll >0) {
                    numChanged = 0;
                    if (examineAll > 0) {
                        for (int k = 0; k < my.N; k++)
                            numChanged += my.examineExample (k);
                    }
                    else {
                        for (int k = 0; k < my.N; k++) {
                            if (my.object2float(my.alph.elementAt(k)) != 0 && my.object2float(my.alph.elementAt(k)) != my.C)
                                numChanged += my.examineExample (k);
                        }
                    }
                    if (examineAll == 1)
                        examineAll = 0;
                    else if (numChanged == 0)
                        examineAll = 1;
                    {
                        int non_bound_support =0;
                        int bound_support =0;
                        for (int i = 0; i < my.N; i++) {
                            if (my.object2float(my.alph.elementAt(i)) > 0) {
                                if (my.object2float(my.alph.elementAt(i)) < my.C)
                                    non_bound_support++;
                                else
                                    bound_support++;
                            }
                        }
                        System.out.println("non_bound= " +non_bound_support+"\t"+"bound_support= "+bound_support);
                    }
                }

                {
                    if (!my.is_test_only && svm_file_name != null) {
                        try {
                            PrintStream svm_file = new PrintStream(new FileOutputStream(svm_file_name));
                            my.write_svm(svm_file);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("Threshold=" + my.b);
            }
            System.out.println("Error_rate=" + my.error_rate());
            newTime = System.currentTimeMillis();
            {
                try {
                    PrintStream svm_file = new PrintStream(new FileOutputStream(output_file_name));
                    for (int i = my.first_test_i; i < my.N; i++) {
                        svm_file.println(my.learned_func(i,my.learned_func_flag));
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Time cost = " + (newTime - time) * 1.0 / 1000);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
