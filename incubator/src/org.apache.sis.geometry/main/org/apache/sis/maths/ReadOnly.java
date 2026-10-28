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
package org.apache.sis.maths;

import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * Interfaces for main interfaces but without modifying methods.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ReadOnly {

    private ReadOnly(){}

    public static interface Affine<T extends Affine<T>> extends Transform {

        /**
         * Get value at coordinate.
         *
         * @param row value row index
         * @param col value column index
         * @return affine cell value
         * @throws IllegalArgumentException if coordinate is out of affine range
         */
        double get(int row, int col) throws IllegalArgumentException;

        /**
         * Get affine row values.
         *
         * @param row row index
         * @return row values
         * @throws IllegalArgumentException if coordinate is out of affine range
         */
        double[] getRow(int row) throws IllegalArgumentException;

        /**
         * Get affine column values.
         *
         * @param col column index
         * @return column values
         * @throws IllegalArgumentException if coordinate is out of affine range
         */
        double[] getCol(int col) throws IllegalArgumentException;

        /**
         * Create a square matrix of size dimensions+1
         * The last matrix line will be [0,...,1]
         *
         * @return matrix
         */
        org.apache.sis.maths.Matrix<?> toMatrix();

        /**
         * Create a copy of this Affine.
         *
         * @return copy
         */
        org.apache.sis.maths.Affine<?> copy();

    }

    public static interface Matrix<T extends Matrix<T>> extends Transform {

        public static final boolean ROW_ORDER = true;
        public static final boolean COL_ORDER = false;

        int getNumRow();

        int getNumCol();

        double get(int row, int col);

        double[] getRow(int row);

        double[] getColumn(int col);

        Vector<?> getColumnTuple(int col);

        double[] getLastColumn();

        Vector<?> getLastColumnTuple();

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
        org.apache.sis.maths.Matrix<?> getRange(int i0, int i1, int j0, int j1);

        /**
         * Get a submatrix.
         *
         * @param r Array of row indices.
         * @param c Array of column indices.
         * @return A(r(:),c(:))
         * @exception ArrayIndexOutOfBoundsException Submatrix indices
         */
        org.apache.sis.maths.Matrix<?> getRange(int[] r, int[] c);

        /**
         * Get a submatrix.
         *
         * @param i0 Initial row index
         * @param i1 Final row index
         * @param c Array of column indices.
         * @return A(i0:i1,c(:))
         * @exception ArrayIndexOutOfBoundsException Submatrix indices
         */
        org.apache.sis.maths.Matrix<?> getRange(int i0, int i1, int[] c);

        /**
         * Get a submatrix.
         *
         * @param r Array of row indices.
         * @param j0 Initial column index
         * @param j1 Final column index
         * @return A(r(:),j0:j1)
         * @exception ArrayIndexOutOfBoundsException Submatrix indices
         */
        org.apache.sis.maths.Matrix<?> getRange(int[] r, int j0, int j1);

        /**
         * Matrix as 2D double array.
         *
         * @param rowOrder true for row order, col order otherwise
         * @return 2D double array
         */
        double[][] toArray2Double(boolean rowOrder);

        /**
         * Matrix as 2D float array.
         *
         * @param rowOrder true for row order, col order otherwise
         * @return 2D float array
         */
        float[][] toArray2Float(boolean rowOrder);

        /**
         * Matrix as 1D double array.
         *
         * @param rowOrder true for row order, col order otherwise
         * @return 1D double array
         */
        double[] toArrayDouble(boolean rowOrder);

        /**
         * Matrix as 1D float array.
         *
         * @param rowOrder true for row order, col order otherwise
         * @return 1D float array
         */
        float[] toArrayFloat(boolean rowOrder);

        org.apache.sis.maths.Matrix<?> copy();

        /**
         * Test if all cells in the matrix equals given value.
         * @param scalar scalar
         * @param tolerance tolerance
         * @return true if all values match
         */
        boolean allEquals(double scalar, double tolerance);

        double dot(ReadOnly.Matrix<?> other);

        /**
         * Returns true if matrix do not contains any NaN or Infinite values.
         *
         * @return true is matrix is finite
         */
        boolean isFinite();

        /**
         * @return true if matrix has the same number of rows and columns.
         */
        boolean isSquare();

        /**
         * For compatibility with MatrixSIS.
         *
         * @return sis matrix equivalent
         */
        MatrixSIS toMatrixSIS();

    }

    public static interface Similarity<T extends Similarity<T>> extends Transform {

        /**
         * Dimension of the transform.
         * @return int
         */
        int getDimension();

        /**
         * Get transform rotation.
         * Call notifyChanged after if you modified the values.
         *
         * @return Matrix
         */
        ReadOnly.Matrix<?> getRotation();

        /**
         * Get transform scale.
         * Call notifyChanged after if you modified the values.
         *
         * @return Vector
         */
        Vector<?> getScale();

        /**
         * Get transform translation.
         * Call notifyChanged after if you modified the values.
         *ç&é ,
         * +
         * @return Vector
         */
        Vector<?> getTranslation();

        /**
         * Inverse transform a single tuple.
         *
         * @param source tuple, cannot be null.
         * @return new transformed tuple.
         */
        Vector<?> inverseTransform(Vector<?> source);

        /**
         * Create a square matrix of size dimensions+1
         * The last matrix line will be [0,...,1]
         *
         * @return matrix
         */
        org.apache.sis.maths.Matrix<?> toMatrix();

        /**
         * Create and affine transform.
         *
         * @return
         */
        org.apache.sis.maths.Affine<?> toAffine();

        /**
         * Create a copy of this Similarity.
         *
         * @return copy
         */
        org.apache.sis.maths.Similarity<?> copy();

    }

}
