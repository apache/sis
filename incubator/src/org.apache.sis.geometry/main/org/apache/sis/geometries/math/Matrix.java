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

/**
 *
 * @author Johann Sorel (Geomatys)
 * @todo Remove this class when all elements are merged in MatrixSIS
 */
public interface Matrix<T extends Matrix<T>> extends ReadOnly.Matrix<T>, org.opengis.referencing.operation.Matrix {

    @Override
    int getNumRow();

    @Override
    int getNumCol();

    @Override
    default boolean isIdentity() {
        return org.opengis.referencing.operation.Matrix.super.isIdentity();
    }

    @Override
    default double getElement(int row, int col) {
        return get(row,col);
    }

    @Override
    default void setElement(int row, int col, double value) {
        set(row, col, value);
    }

    T set(int row, int col, double value);

    T set(ReadOnly.Matrix<?> toCopy);

    T setFromGeoapi(org.opengis.referencing.operation.Matrix matrix);

    /**
     * set to ad square matrix of size dimensions+1
     * The last matrix line will be [0,...,1]
     *
     * @return this matrix
     */
    T setFromAffine(ReadOnly.Affine<?> affine);

    T set(double[] values, boolean rowOrder);

    T set(double[][] values, boolean rowOrder);

    T setRow(int row, double[] values);

    T setCol(int col, double[] values);

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
    T setRange(int i0, int i1, int j0, int j1, ReadOnly.Matrix<?> X);

    /**
     * Set a submatrix.
     *
     * @param r Array of row indices.
     * @param c Array of column indices.
     * @param X A(r(:),c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    T setRange(int[] r, int[] c, ReadOnly.Matrix<?> X);

    /**
     * Set a submatrix.
     *
     * @param r Array of row indices.
     * @param j0 Initial column index
     * @param j1 Final column index
     * @param X A(r(:),j0:j1)
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    T setRange(int[] r, int j0, int j1, ReadOnly.Matrix<?> X);

    /**
     * Set a submatrix.
     *
     * @param i0 Initial row index
     * @param i1 Final row index
     * @param c Array of column indices.
     * @param X A(i0:i1,c(:))
     * @exception ArrayIndexOutOfBoundsException Submatrix indices
     */
    T setRange(int i0, int i1, int[] c, ReadOnly.Matrix<?> X);

    /**
     * Set Matrix value to identity matrix.
     * @return this matrix
     */
    T setToIdentity();

    /**
     * invert matrix
     */
    T invert();

    T add(ReadOnly.Matrix<?> other);

    T subtract(ReadOnly.Matrix<?> other);

    T scale(double[] tuple);

    T scale(double scale);

    T multiply(ReadOnly.Matrix<?> other);

    T transpose();

    T copy();

    @Override
    default org.opengis.referencing.operation.Matrix clone() {
        return copy();
    }
}
