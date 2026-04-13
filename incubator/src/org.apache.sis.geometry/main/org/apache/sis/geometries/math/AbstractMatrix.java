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


/**
 * Abstract writable matrix.
 *
 * @author Johann Sorel
 * @aurhor Bertrand COTE
 */
abstract class AbstractMatrix<T extends AbstractMatrix<T>> extends SimplifiedTransform implements Matrix<T> {

    protected final int nbRow;
    protected final int nbCol;

    /**
     * Create a new abstract matrix of given size.
     *
     * @param nbrow
     * @param nbcol
     */
    protected AbstractMatrix(final int nbrow, final int nbcol) {
        super(nbrow);
        this.nbRow = nbrow;
        this.nbCol = nbcol;
    }

    @Override
    public int getNumRow() {
        return nbRow;
    }

    @Override
    public int getNumCol() {
        return nbCol;
    }

    @Override
    public int getInputDimensions() {
        return nbRow;
    }

    @Override
    public int getOutputDimensions() {
        return nbRow;
    }

    @Override
    public double[][] toArray2Double(boolean rowOrder) {
        if (rowOrder) {
            final double[][] C = new double[nbRow][nbCol];
            for (int r = 0; r < nbRow; r++) {
                for (int c = 0; c < nbCol; c++) {
                    C[r][c] = get(r,c);
                }
            }
            return C;
        } else {
            final double[][] C = new double[nbCol][nbRow];
            for (int r = 0; r < nbRow; r++) {
                for (int c = 0; c < nbCol; c++) {
                    C[c][r] = get(r,c);
                }
            }
            return C;
        }
    }

    @Override
    public float[][] toArray2Float(boolean rowOrder) {
        if (rowOrder) {
            final float[][] C = new float[nbRow][nbCol];
            for (int r = 0; r < nbRow; r++) {
                for (int c = 0; c < nbCol; c++) {
                    C[r][c] = (float) get(r,c);
                }
            }
            return C;
        } else {
            final float[][] C = new float[nbCol][nbRow];
            for (int r = 0; r < nbRow; r++) {
                for (int c = 0; c < nbCol; c++) {
                    C[c][r] = (float) get(r,c);
                }
            }
            return C;
        }
    }

    @Override
    public double[] getRow(int row){
        final double[] rowval = new double[nbCol];
        for (int i=0;i<nbCol;i++) rowval[i] = get(row,i);
        return rowval;
    }

    @Override
    public double[] getColumn(int col){
        final double[] colval = new double[nbRow];
        for (int i=0;i<nbRow;i++) colval[i] = get(i,col);
        return colval;
    }

    @Override
    public Vector<?> getColumnTuple(int col){
        final double[] c = getColumn(col);
        return Vectors.create(c.length, DataType.DOUBLE).set(c);
    }

    @Override
    public double[] getLastColumn(){
        return getColumn(nbCol-1);
    }

    @Override
    public Vector<?> getLastColumnTuple(){
        return getColumnTuple(nbCol-1);
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
    public Matrix<?> getRange(int i0, int i1, int j0, int j1) {
        final Matrix<?> X = MatrixND.create(i1 - i0 + 1, j1 - j0 + 1);
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    X.set(i-i0,j-j0, get(i,j));
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
    public Matrix<?> getRange(int[] r, int[] c) {
        final MatrixND X = new MatrixND(r.length, c.length);
        final double[][] B = X.values;
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    B[i][j] = get(r[i],c[j]);
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
    public Matrix<?> getRange(int i0, int i1, int[] c) {
        final MatrixND X = new MatrixND(i1 - i0 + 1, c.length);
        final double[][] B = X.values;
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = 0; j < c.length; j++) {
                    B[i-i0][j] = get(i,c[j]);
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
    public Matrix<?> getRange(int[] r, int j0, int j1) {
        final MatrixND X = new MatrixND(r.length, j1 - j0 + 1);
        final double[][] B = X.values;
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = j0; j <= j1; j++) {
                    B[i][j - j0] = get(r[i],j);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return X;
    }

    @Override
    public T set(final ReadOnly.Matrix<?> toCopy){
        final int rm = Math.min(this.nbRow, toCopy.getNumRow());
        final int cm = Math.min(this.nbCol, toCopy.getNumCol());
        for (int r=0;r<rm;r++){
            for (int c=0;c<cm;c++){
                set(r, c, toCopy.get(r, c));
            }
        }
        return (T) this;
    }

    @Override
    public T set(final double[] values, boolean rowOrder){
        if (rowOrder) {
            for (int i=0,r=0;r<nbRow;r++){
                for (int c=0;c<nbCol;c++,i++){
                    set(r, c, values[i]);
                }
            }
        } else {
            for (int i=0,c=0;c<nbCol;c++){
                for (int r=0;r<nbRow;r++,i++){
                    set(r, c, values[i]);
                }
            }
        }
        return (T) this;
    }

    @Override
    public T set(final double[][] values, boolean rowOrder){
        if (rowOrder) {
            for (int r=0;r<nbRow;r++){
                for (int c=0;c<nbCol;c++){
                    set(r, c, values[r][c]);
                }
            }
        } else {
            for (int r=0;r<nbRow;r++){
                for (int c=0;c<nbCol;c++){
                    set(r, c, values[c][r]);
                }
            }
        }
        return (T) this;
    }

    @Override
    public T setRow(int row, double[] values){
        for (int i=0;i<values.length;i++){
            set(row,i,values[i]);
        }
        return (T) this;
    }

    @Override
    public T setCol(int col, double[] values){
        for (int i=0;i<values.length;i++){
            set(i,col,values[i]);
        }
        return (T) this;
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
    public T setRange(int i0, int i1, int j0, int j1, ReadOnly.Matrix<?> X) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = j0; j <= j1; j++) {
                    set(i,j, X.get(i - i0, j - j0));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return (T) this;
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
    public T setRange(int[] r, int[] c, ReadOnly.Matrix<?> X) {
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < c.length; j++) {
                    set(r[i],c[j], X.get(i, j));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return (T) this;
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
    public T setRange(int[] r, int j0, int j1, ReadOnly.Matrix<?> X) {
        try {
            for (int i = 0; i < r.length; i++) {
                for (int j = j0; j <= j1; j++) {
                    set(r[i],j, X.get(i, j - j0));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return (T) this;
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
    public T setRange(int i0, int i1, int[] c, ReadOnly.Matrix<?> X) {
        try {
            for (int i = i0; i <= i1; i++) {
                for (int j = 0; j < c.length; j++) {
                    set(i,c[j], X.get(i - i0, j));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
        }
        return (T) this;
    }

    @Override
    public T setFromGeoapi(org.opengis.referencing.operation.Matrix toCopy) {
        final int rm = Math.min(this.nbRow, toCopy.getNumRow());
        final int cm = Math.min(this.nbCol, toCopy.getNumCol());
        for (int r=0;r<rm;r++){
            for (int c=0;c<cm;c++){
                set(r, c, toCopy.getElement(r, c));
            }
        }
        return (T) this;

    }
    /**
     * Set Matrix value to identity matrix.
     * @return this matrix
     */
    @Override
    public T setToIdentity(){
        if (nbCol != nbRow) {
            throw new IllegalArgumentException("The m matrix must be a square matrix.");
        }
        for (int x = 0; x < nbCol; x++) {
            for (int y = 0; y < nbRow; y++) {
                if (x == y) {
                    set(y,x, 1.0);
                } else {
                    set(y,x, 0.0);
                }
            }
        }
        return (T) this;
    }

    @Override
    public boolean isIdentity(){
        if (nbCol != nbRow) return false; // m must be a square matrix
        for ( int x=0; x<nbCol; x++) {
            for ( int y=0; y<nbRow; y++) {
                if (x==y){
                    if ( get(y,x) != 1.0 ) return false;
                } else {
                    if ( get(y,x) != 0.0 ) return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isFinite() {
        for ( int x=0; x<nbCol; x++) {
            for ( int y=0; y<nbRow; y++) {
                double v = get(y,x);
                if (Double.isNaN(v) || Double.isInfinite(v)){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Matrix<?> createInverse() {
        return copy().invert();
    }

    @Override
    public T transpose() {
        Matrix<?> t = copy();
        for (int x=0; x<nbCol;x++){
            for (int y=0; y<nbRow; y++){
                t.set(x,y, get(y,x));
            }
        }
        return set((ReadOnly.Matrix)t);
    }

    @Override
    public T roundZeros(double epsilon){
        for (int x=0;x<nbCol;x++){
            for (int y=0;y<nbRow;y++){
                double d = get(y, x);
                if (!(d>epsilon || d<-epsilon)){
                    set(y,x, 0.0);
                }
            }
        }
        return (T) this;
    }

    /**
     * invert matrix
     */
    @Override
    public T invert(){
        final double[][] inverse = Matrices.localInvert(toArray2Double(ROW_ORDER));
        if (inverse == null){
            throw new IllegalArgumentException("Cannot inverse");
        }
        set(inverse, ROW_ORDER);
        return (T) this;
    }

    @Override
    public T add(ReadOnly.Matrix<?> other){
        set(Matrices.localAdd(dArray(this), dArray(other)), ROW_ORDER);
        return (T) this;
    }

    @Override
    public T subtract(ReadOnly.Matrix<?> other){
        set(Matrices.localSubtract(dArray(this), dArray(other)), ROW_ORDER);
        return (T) this;
    }

    @Override
    public T scale(double[] tuple){
        set(Matrices.localScale(dArray(this), tuple), ROW_ORDER);
        return (T) this;
    }

    @Override
    public T scale(double scale){
        set(Matrices.localScale(dArray(this), scale), ROW_ORDER);
        return (T) this;
    }

    @Override
    public T multiply(ReadOnly.Matrix<?> other){
        set(Matrices.localMultiply(dArray(this), dArray(other)), ROW_ORDER);
        return (T) this;
    }

    @Override
    public double dot(ReadOnly.Matrix<?> other){
        return Matrices.dot(dArray(this), dArray(other));
    }

    @Override
    public Tuple<?> transform(final ReadOnly.Tuple<?> vector, Tuple<?> dest) {
        final double[] array = new double[nbRow];
        transform(vector.toArrayDouble(), 0, array, 0, 1);
        if (dest == null) {
            return new VectorND.Double(array);
        } else {
            dest.set(array);
            return dest;
        }
    }

    @Override
    public double[] toArrayDouble(boolean rowOrder){
        final double[] array = new double[nbRow*nbCol];
        if (rowOrder) {
            for (int p=0,r=0;r<nbRow;r++){
                for (int c=0;c<nbCol;c++,p++){
                    array[p] = get(r, c);
                }
            }
        } else {
            for (int p=0,c=0;c<nbCol;c++){
                for (int r=0;r<nbRow;r++,p++){
                    array[p] = get(r, c);
                }
            }
        }
        return array;
    }

    @Override
    public float[] toArrayFloat(boolean rowOrder){
        final float[] array = new float[nbRow*nbCol];
        if (rowOrder) {
            for (int p=0,r=0;r<nbRow;r++){
                for (int c=0;c<nbCol;c++,p++){
                    array[p] = (float) get(r, c);
                }
            }
        } else {
            for (int p=0,c=0;c<nbCol;c++){
                for (int r=0;r<nbRow;r++,p++){
                    array[p] = (float) get(r, c);
                }
            }
        }
        return array;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.nbRow;
        hash = 53 * hash + this.nbCol;
        return hash;
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
                if (Math.abs(get(r,c)-scalar) > tolerance){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ReadOnly.Matrix)) {
            return false;
        }
        final ReadOnly.Matrix<?> other = (ReadOnly.Matrix) obj;
        if (this.nbRow != other.getNumRow()) {
            return false;
        }
        if (this.nbCol != other.getNumCol()) {
            return false;
        }

        //check values
        if (!Arrays.equals(toArrayDouble(ROW_ORDER), other.toArrayDouble(ROW_ORDER))){
            return false;
        }
        return true;
    }

    @Override
    public org.opengis.referencing.operation.Matrix clone() {
        return copy();
    }

    protected static double[][] dArray(ReadOnly.Matrix<?> matrix){
        return matrix instanceof MatrixND ? ((MatrixND) matrix).values : matrix.toArray2Double(ROW_ORDER);
    }
}
