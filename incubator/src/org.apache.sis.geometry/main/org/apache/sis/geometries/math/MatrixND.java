/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.geometries.math;

import java.util.Arrays;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 *
 * @author Johann Sorel
 * @aurhor Bertrand COTE
 */
public class MatrixND extends AbstractMatrix<MatrixND>{

    protected final double[][] values;

    /**
     * Create a new matrix of given size.
     * This methods create the most efficient implementation available.
     *
     * @param nbrow
     * @param nbcol
     * @return
     */
    public static Matrix<?> create(int nbrow, int nbcol){
        if (nbrow==nbcol) {
            switch(nbrow){
                case 2 : return new Matrix2D();
                case 3 : return new Matrix3D();
                case 4 : return new Matrix4D();
            }
        }
        return new MatrixND(nbrow, nbcol);
    }

    /**
     * Create a new matrix of given size.
     * It is recommended to use static create method to benefit from specialized
     * matrices implementations such as Matrix2D,Matrix3D,Matrix4D.
     *
     * @param nbrow
     * @param nbcol
     */
    public MatrixND(final int nbrow, final int nbcol) {
        super(nbrow,nbcol);
        values = new double[nbrow][nbcol];
    }

    public MatrixND(final double[][] values) {
        super(values.length,values[0].length);
        this.values = values;
    }

    public MatrixND(ReadOnly.Matrix<?> m) {
        super(m.getNumRow(),m.getNumCol());
        values = m.toArray2Double(ROW_ORDER);
    }

    public double[][] getValues() {
        return values;
    }

    @Override
    public double[] getRow(int row){
        final double[] rowval = new double[nbCol];
        for (int i=0;i<nbCol;i++) rowval[i] = values[row][i];
        return rowval;
    }

    @Override
    public double[] getColumn(int col){
        final double[] colval = new double[nbRow];
        for (int i=0;i<nbRow;i++) colval[i] = values[i][col];
        return colval;
    }

    /**
     * Get a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param j0 Initial column index
     * @param j1 Final column index
     * @return A(i0:i1,j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND getRange(int i0, int i1, int j0, int j1) {
        final MatrixND X = new MatrixND(i1 - i0 + 1, j1 - j0 + 1);
        final double[][] B = X.values;
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    B[i-i0][j-j0] = values[i][j];
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Get a submatrix.
     *
     * @param r Array of row indices.
     * @param c Array of column indices.
     * @return A(r(:),c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND getRange(int[] r, int[] c) {
        final MatrixND X = new MatrixND(r.length, c.length);
        final double[][] B = X.values;
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    B[i][j] = values[r[i]][c[j]];
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Get a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param c Array of column indices.
     * @return A(i0:i1,c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND getRange(int i0, int i1, int[] c) {
        final MatrixND X = new MatrixND(i1 - i0 + 1, c.length);
        final double[][] B = X.values;
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = 0; j < c.length; j++) {
                    B[i-i0][j] = values[i][c[j]];
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    /**
     * Get a submatrix.
     *
     * @param r Array of row indices.
     * @param j0 Initial column index
     * @param j1 Final column index
     * @return A(r(:),j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND getRange(int[] r, int j0, int j1) {
        final MatrixND X = new MatrixND(r.length, j1 - j0 + 1);
        final double[][] B = X.values;
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = j0; j <= j1; j++) {
                    B[i][j - j0] = values[r[i]][j];
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    @Override
    public double get(int row, int col) {
        return values[row][col];
    }

    @Override
    public MatrixND set(int row, int col, double value) {
        values[row][col] = value;
        return this;
    }

    @Override
    public MatrixND setRow(int row, double[] values){
        for (int i=0;i<values.length;i++){
            this.values[row][i] = values[i];
        }
        return this;
    }

    @Override
    public MatrixND setCol(int col, double[] values){
        for (int i=0;i<values.length;i++){
            this.values[i][col] = values[i];
        }
        return this;
    }

    /**
     * Set a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param j0 Initial column index
     * @param j1 Final column index
     * @param X A(i0:i1,j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND setRange(int i0, int i1, int j0, int j1, ReadOnly.Matrix<?> X) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    values[i][j] = X.get(i - i0, j - j0);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return this;
    }

    /**
     * Set a submatrix.
     *
     * @param r Array of row indices.
     * @param c Array of column indices.
     * @param X A(r(:),c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND setRange(int[] r, int[] c, ReadOnly.Matrix<?> X) {
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    values[r[i]][c[j]] = X.get(i, j);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return this;
    }

    /**
     * Set a submatrix.
     *
     * @param r Array of row indices.
     * @param j0 Initial column index
     * @param j1 Final column index
     * @param X A(r(:),j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND setRange(int[] r, int j0, int j1, ReadOnly.Matrix<?> X) {
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = j0; j <= j1; j++) {
                    values[r[i]][j] = X.get(i, j - j0);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return this;
    }

    /**
     * Set a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param c Array of column indices.
     * @param X A(i0:i1,c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    @Override
    public MatrixND setRange(int i0, int i1, int[] c, ReadOnly.Matrix<?> X) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = 0; j < c.length; j++) {
                    values[i][c[j]] = X.get(i - i0, j);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return this;
    }

    @Override
    public MatrixND setToIdentity(){
        if (values.length != values[0].length) {
            throw new IllegalArgumentException("The matrix is not a square matrix.");
        }
        for (int x = 0; x < values[0].length; x++) {
            for (int y = 0; y < values.length; y++) {
                if (x == y) {
                    values[y][x] = 1.;
                } else {
                    values[y][x] = 0.;
                }
            }
        }
        return this;
    }

    @Override
    public MatrixND setFromAffine(ReadOnly.Affine<?> affine) {
        setToIdentity();
        final int dim = affine.getInputDimensions();
        for (int y=0;y<dim;y++) {
            for (int x=0;x<=dim;x++) {
                set(y, x, affine.get(y, x));
            }
        }
        return this;
    }

    @Override
    public boolean isIdentity() {
        if (values.length != values[0].length) {
            return false; // m must be a square matrix
        }
        for (int x = 0; x < values[0].length; x++) {
            for (int y = 0; y < values.length; y++) {
                if (x == y) {
                    if (values[y][x] != 1) {
                        return false;
                    }
                } else {
                    if (values[y][x] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * invert matrix
     */
    @Override
    public MatrixND invert(){
        final double[][] inverse = Matrices.localInvert(values);
        if (inverse == null){
            throw new IllegalArgumentException("Cannot inverse");
        }
        return this;
    }

    @Override
    public MatrixND add(ReadOnly.Matrix<?> other){
        Matrices.localAdd(values, dArray(other));
        return this;
    }

    @Override
    public MatrixND subtract(ReadOnly.Matrix<?> other){
        Matrices.localSubtract(values, dArray(other));
        return this;
    }

    @Override
    public MatrixND scale(double[] tuple){
        Matrices.localScale(values, tuple);
        return this;
    }

    @Override
    public MatrixND scale(double scale){
        Matrices.localScale(values, scale);
        return this;
    }

    @Override
    public MatrixND multiply(ReadOnly.Matrix<?> other){
        Matrices.localMultiply(values, dArray(other));
        return this;
    }

    @Override
    public MatrixND transpose(){
        return set(new MatrixND(Matrices.transpose(values)));
    }

    @Override
    public double dot(ReadOnly.Matrix<?> other){
        return Matrices.dot(values, dArray(other));
    }

    @Override
    public MatrixND copy() {
        return new MatrixND(this);
    }

    @Override
    public MatrixSIS toMatrixSIS() {
        throw new UnsupportedOperationException();
    }

    /**
     * Test if all cells in the matrix equals given value.
     * @param scalar scalar
     * @param tolerance tolerance
     * @return true if all values match
     */
    @Override
    public boolean allEquals(double scalar, double tolerance) {
        for (int r=0; r<nbRow; r++){
            for (int c=0; c<nbCol; c++){
                if (Math.abs(values[r][c]-scalar) > tolerance){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        //TODO improve it, slow and memory greedy
        final double[] v = Arrays.copyOfRange(source, sourceOffset, sourceOffset + getInputDimensions());
        final double[] d = new double[getOutputDimensions()];
        internalTransform(v, d);
        System.arraycopy(d,0,dest,destOffset,d.length);
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        //TODO improve it, slow and memory greedy
        final double[] v = new double[getInputDimensions()];
        for (int i = 0; i < v.length; i++) {
            v[i] = source[sourceOffset+i];
        }
        final double[] d = new double[getOutputDimensions()];
        internalTransform(v, d);
        for (int i=0;i<d.length;i++) dest[destOffset+i] = (float) d[i];
    }

    /**
     * Transform given vector.
     *
     * @param vector vector to transform
     * @param buffer result vector buffer, not null
     * @return the product of matrix and vector.
     */
    private void internalTransform(final double[] vector, double[] buffer) {
        for (int r = 0; r < nbRow; r++) {
            double s = 0;
            for (int c = 0; c < nbCol; c++) {
                s += values[r][c] * vector[c];
            }
            buffer[r] = s;
        }
    }
}
