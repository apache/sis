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

    public static interface Vector<T extends Vector<T>> {

        /**
         * @return sample system size.
         */
        int getDimension();

        /**
         * @return data type of primitives in this array
         */
        DataType getDataType();

        /**
         * Get sample at index.
         *
         * @param indice sample index
         * @return value at index
         * @throws IndexOutOfBoundsException if index is not valid
         */
        double get(int indice) throws IndexOutOfBoundsException;

        /**
         * Tuple to array.
         *
         * @return values as cast byte array
         */
        default byte[] toArrayByte() {
            byte[] buffer = new byte[getDimension()];
            toArrayByte(buffer, 0);
            return buffer;
        }

        /**
         * Tuple to array.
         *
         * @return values as cast short array
         */
        default short[] toArrayShort() {
            short[] buffer = new short[getDimension()];
            toArrayShort(buffer, 0);
            return buffer;
        }

        /**
         * Tuple to array.
         *
         * @return values as cast integer array
         */
        default int[] toArrayInt() {
            int[] buffer = new int[getDimension()];
            toArrayInt(buffer, 0);
            return buffer;
        }

        /**
         * Tuple to array.
         *
         * @return values as cast float array
         */
        default float[] toArrayFloat() {
            float[] buffer = new float[getDimension()];
            toArrayFloat(buffer, 0);
            return buffer;
        }

        /**
         * Tuple to array.
         *
         * @return values as double array
         */
        default double[] toArrayDouble() {
            double[] buffer = new double[getDimension()];
            toArrayDouble(buffer, 0);
            return buffer;
        }

        /**
         * Tuple to array.
         *
         * @param buffer array to write into
         * @param offset offset at which to write
         */
        default void toArrayByte(byte[] buffer, int offset) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                buffer[offset+i] = (byte) get(i);
            }
        }

        /**
         * Tuple to array.
         *
         * @param buffer array to write into
         * @param offset offset at which to write
         */
        default void toArrayShort(short[] buffer, int offset) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                buffer[offset+i] = (short) get(i);
            }
        }

        /**
         * Tuple to array.
         *
         * @param buffer array to write into
         * @param offset offset at which to write
         */
        default void toArrayInt(int[] buffer, int offset) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                buffer[offset+i] = (int) get(i);
            }
        }

        /**
         * Tuple to array.
         *
         * @param buffer array to write into
         * @param offset offset at which to write
         */
        default void toArrayFloat(float[] buffer, int offset) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                buffer[offset+i] = (float) get(i);
            }
        }

        /**
         * Tuple to array.
         *
         * @param buffer array to write into
         * @param offset offset at which to write
         */
        default void toArrayDouble(double[] buffer, int offset) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                buffer[offset+i] = get(i);
            }
        }

        /**
         * Create a copy of this tuple.
         *
         * @return tuple copy.
         */
        default org.apache.sis.maths.Vector<?> copy() {
            return Vectors.create(getDimension(), getDataType()).set(this);
        }

        /**
         * Create a copy of this tuple, with values stored in the given data type.
         * The copy has the same sample system as this tuple.
         *
         * @param  dataType  data type of the copy values.
         * @return tuple copy.
         */
        default org.apache.sis.maths.Vector<?> copy(DataType dataType) {
            return Vectors.create(getDimension(), dataType).set(this);
        }

        /**
         * @return true if all values are finite (not infinite or NaN)
         */
        default boolean isFinite(){
            for (int i = 0, n = getDimension(); i < n; i++) {
                if (!Double.isFinite(get(i))) return false;
            }
            return true;
        }

        /**
         * @return true if all samples match given value.
         */
        default boolean isAll(double value) {
            for (int i = 0, n = getDimension(); i < n; i++) {
                if (value != get(i)) return false;
            }
            return true;
        }

        /**
         * Tuples are equal when all samples match.
         */
        @Override
        boolean equals(Object candidate);

        /**
         * Test tuples equality.
         *
         * Tuples are a key element in OpenGL and GPU which have variable bits representation of numbers
         * including half-floats, oct-compression, normalisation, quantization, unsigned values and more.
         *
         * This equality test checks values but does not verify that data types are identical.
         * This behavior allows to compare Tuple following their contract ignoring the storage backend.
         *
         * @param obj second tuple to test against
         * @param tolerance tolerance value for compare operation
         * @return true if tuples are value equal
         */
        default boolean equals(ReadOnly.Vector<?> other, double tolerance) {
            if (this == other) {
                return true;
            }
            if (other == null) {
                return false;
            }

            final int dim = getDimension();
            if (dim != other.getDimension()) {
                return false;
            }
            for (int i = 0; i < dim; i++) {
                double v1 = get(i);
                double v2 = other.get(i);
                if (v1 != v2) {
                    //check with tolerance
                    if (Math.abs(v1 - v2) <= tolerance) {
                        continue;
                    }
                    //check for NaN equality
                    if (Double.doubleToRawLongBits(v1) != Double.doubleToRawLongBits(v2)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * @return vector length
         */
        default double length() {
            return Vectors.length(toArrayDouble());
        }

        /**
         * @return vector squere length
         */
        default double lengthSquare() {
            return Vectors.lengthSquare(toArrayDouble());
        }

        /**
         * Cross product.
         *
         * @param other
         * @return cross product result
         */
        default org.apache.sis.maths.Vector<?> cross(ReadOnly.Vector<?> other) {
            double[] v1 = toArrayDouble();
            double[] v2 = other.toArrayDouble();
            double[] buffer = new double[v1.length];
            Vectors.cross(v1, v2, buffer);
            org.apache.sis.maths.Vector<?> res = Vectors.createDouble(buffer.length);
            res.set(buffer);
            return res;
        }

        /**
         * Dot product.
         *
         * @param other
         * @return dot product result
         */
        default double dot(ReadOnly.Vector<?> other) {
            double dot = 0;
            for (int i=0,n=getDimension();i<n;i++){
                dot += get(i) * other.get(i);
            }
            return dot;
        }

        /**
         * Increase size of the tuple by one value.
         *
         * @param d last dimension value
         * @return Vector
         */
        default org.apache.sis.maths.Vector<?> extend(double d) {
            final int dim = getDimension();
            final org.apache.sis.maths.Vector<?> v = Vectors.create(dim+1, getDataType());
            for (int i = 0; i < dim; i++) {
                v.set(i, get(i));
            }
            v.set(dim, d);
            return v;
        }

        /**
         * Decrease size of the tuple by one value.
         *
         * @param size number of dimension to preserve
         * @return Vector
         */
        default org.apache.sis.maths.Vector<?> shrink(int size) {
            final org.apache.sis.maths.Vector<?> v = Vectors.create(size, getDataType());
            for (int i = 0; i < size; i++) {
                v.set(i, get(i));
            }
            return v;
        }

        /**
         * Create a new tuple with the values of the given axes, in the given order.
         * Axes can be omitted or repeated, for example {@code swizzle(1, 0)} swaps the
         * two first values and {@code swizzle(0, 0, 0)} repeats the first one.
         *
         * @param axes indices of the values of this tuple to copy, one per dimension of the result
         * @return Vector of dimension {@code axes.length}
         * @throws IndexOutOfBoundsException if an axis index is not valid
         */
        default org.apache.sis.maths.Vector<?> swizzle(int ... axes) throws IndexOutOfBoundsException {
            final org.apache.sis.maths.Vector<?> v = Vectors.create(axes.length, getDataType());
            for (int i = 0; i < axes.length; i++) {
                v.set(i, get(axes[i]));
            }
            return v;
        }
    }

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
        ReadOnly.Vector<?> getScale();

        /**
         * Get transform translation.
         * Call notifyChanged after if you modified the values.
         *ç&é ,
         * +
         * @return Vector
         */
        ReadOnly.Vector<?> getTranslation();

        /**
         * Inverse transform a single tuple.
         *
         * @param source tuple, cannot be null.
         * @param dest tuple, can be null.
         * @return destination tuple.
         */
        org.apache.sis.maths.Vector<?> inverseTransform(ReadOnly.Vector<?> source, org.apache.sis.maths.Vector<?> dest);

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
