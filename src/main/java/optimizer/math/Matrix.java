/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package optimizer.math;

/**
 * Data structure for dense matrices with double entries
 * Created by david on 2018. 02. 22..
 */
public class Matrix {

    protected double[][] M;
    protected int nrows;
    protected int ncols;


    /**
     * Returns the number of rows
     */
    public int getNrows() {
        return nrows;
    }

    /**
     * Returns the number of rows
     */
    public int getNcols() {
        return ncols;
    }

    /**
     * Constructor for m x n matrix with zero entries
     * @param m number of rows
     * @param n number of columns
     */
    public Matrix(int m, int n) {
        this.nrows = m;
        this.ncols = n;
        this.M = new double[m][n];
    }

    /**
     * Constructor for column vector matrix from array
     * @param m number of rows
     * @param n number of columns
     */
    public Matrix(double[] b) {
        this.nrows = b.length;
        this.ncols = 1;
        this.M = new double[nrows][1];
        for(int i = 0; i < nrows; ++i) {
            this.M[i][0] = b[i];
        }
    }

    /**
     * Set the entry of the matrix with given index
     * @param i row index
     * @param j column index
     * @param k new value of entry
     */
    public void set(int i, int j, double k) {
        M[i][j] = k;
    }

    /**
     * Return the entry of the matrix with given index
     * @param i row index
     * @param j column index
     */
    public double get(int i, int j) {
        return M[i][j];
    }

    /**
     * Add a number to an entry of the matrix
     * @param i row index
     * @param j column index
     * @param k added value
     */
    public void add(int i, int j, double k) {
        M[i][j] += k;
    }

    /**
     * Subtract a number from an entry of the matrix
     * @param i row index
     * @param j column index
     * @param k subtracted value
     */
    public void sub(int i, int j, double k) {
        M[i][j] -= k;
    }

    /**
     * Multiply an entry of the matrix with a number
     * @param i row index
     * @param j column index
     * @param k multiplier
     */
    public void mul(int i, int j, double k) {
        M[i][j] *= k;
    }

    /**
     * Divide an entry of the matrix with a number
     * @param i row index
     * @param j column index
     * @param k divisor
     */
    public void div(int i, int j, double k) {
        M[i][j] /= k;
    }

    /**
     * Returns the Cholesky decomposition of M. M=L*L', where L is a lower triangular matrix.
     * @return
     */
    public Cholesky cholesky() {
        if (nrows != ncols) {
            throw new UnsupportedOperationException("Cholesky decomposition on non-square matrix");
        }

        int n = this.nrows;

        // Main loop.
        for (int j = 0; j < n; j++) {
            double d = 0.0;
            for (int k = 0; k < j; k++) {
                double s = 0.0;
                for (int i = 0; i < k; i++) {
                    s += M[k][i] * M[j][i];
                }
                s = (M[j][k] - s) / M[k][k];
                M[j][k] = s;
                d = d + s * s;
            }
            d = M[j][j] - d;

            if (d < 0.0) {
                throw new IllegalArgumentException("The matrix is not positive definite.");
            }
            M[j][j] = Math.sqrt(d);
        }
        return new Cholesky(this);
    }

    /**
     * Returns an n x n identity matrix.
     * @param n size
     * @return n x n identity matrix
     */
    public static Matrix identity(int n) {
        Matrix m = new Matrix(n, n);
        for (int i = 0; i < n; i++) {
            m.set(i,i,1.0);
        }
        return m;
    }

    /**
     * Compute M * x.
     * @param x column vector represented by an array of doubles
     * @return y = M * x
     */
    public  double[] ax(double[] x) {
        if (ncols != x.length) {
            throw new UnsupportedOperationException("Matrix and vector size do not match");
        }
        double[] y = new double[nrows];
        for(int i = 0; i < nrows; ++i) {
            for(int j = 0; j < ncols; ++j) {
                y[i] += M[i][j] * x[j];
            }
        }
        return y;
    }

    /**
     * Compute x' * M * x.
     * @param x column vector represented by an array of doubles
     * @return s = x' * M * x
     */
    public double xax(double[] x) {
        if (nrows != ncols) {
            throw new IllegalArgumentException("Not square matrix");
        }

        if (nrows != x.length) {
            throw new IllegalArgumentException("Matrix and vector size do not match");
        }

        int n = x.length;
        double s = 0.0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                s += get(i, j) * x[i] * x[j];
            }
        }

        return s;
    }


}
