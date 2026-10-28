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

import java.util.function.Function;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * A Vector is an immutable array of values.
 * With arithmetic operations defined, each operation creates a new vector.
 *
 * Note : originaly we had two interfaces 'Tuple' and 'Vector', the Tuple was only a container and Vector extended it
 * with more arithmetic methods. This state created duplicated classes and imposed conversions from Tuple to Vector
 * in many cases. This conversion problem is still raised even further with DirectPosition interface.
 * It has been decided to merge Tuple and Vector to reduce duplication and make code more fluent, readable and user friendly.
 *
 * <h2>Accuracy</h2>
 * Operations are computed with plain {@code double} arithmetic by default. This is fast, and gives the same results
 * as common libraries such as gl-matrix. Plain arithmetic has an error relative to the magnitude of the operands,
 * about 1E-16 times the largest term, which is enough in most cases.
 *
 * <p>Operations made of sums of products lose all significant digits when the terms nearly cancel,
 * for example the dot product of nearly orthogonal vectors or the cross product of nearly parallel vectors.
 * For such cases, the methods with an {@code Accurate} suffix ({@link #dotAccurate(Vector)},
 * {@link #crossAccurate(Vector)}, {@link #lerpAccurate(Vector, double)}) compute each product exactly and
 * compensate the rounding errors of the sums. Their result is as accurate as if computed with twice the
 * {@code double} precision then rounded once, which is the accuracy of Apache Commons Geometry.
 * They are about 3 to 5 times slower, and their results may differ in the last bits from the plain methods.
 * See {@code LinearCombination} for details.</p>
 *
 * <p>Linear interpolation {@link #lerp(Vector, double)} is computed as {@code this + t*(other-this)}, the formula of
 * gl-matrix, except for {@code t = 1} which returns exactly the other vector. It gives exactly this vector for
 * {@code t = 0} and the other vector for {@code t = 1}, keeps the value unchanged when both vectors are equal,
 * and results are monotonic in {@code t}. Its error is about 1E-16 times the largest of the two vectors values.
 * {@link #lerpAccurate(Vector, double)} computes {@code (1-t)*this + t*other} with compensated arithmetic: the result
 * is correctly rounded in most cases, but monotonicity is not guaranteed at the scale of one ulp because
 * {@code 1-t} is rounded.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Vector<T extends Vector<T>> {

    /*
     * Factory methods creating vectors directly. The vector class is selected by the dimension and by
     * the primitive type of the arguments, for example {@code Vector.of(1.0, 2.0)} creates a
     * {@code Vector2D.Double} while {@code Vector.of(1, 2)} creates a {@code Vector2D.Int}.
     */

    /**
     * Create a 1D vector of {@code byte} values.
     *
     * @param x first value
     * @return new vector
     */
    static Vector1D.Byte of(byte x) {
        return new Vector1D.Byte(x);
    }

    /**
     * Create a 2D vector of {@code byte} values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    static Vector2D.Byte of(byte x, byte y) {
        return new Vector2D.Byte(x, y);
    }

    /**
     * Create a 3D vector of {@code byte} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @return new vector
     */
    static Vector3D.Byte of(byte x, byte y, byte z) {
        return new Vector3D.Byte(x, y, z);
    }

    /**
     * Create a 4D vector of {@code byte} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @param w fourth value
     * @return new vector
     */
    static Vector4D.Byte of(byte x, byte y, byte z, byte w) {
        return new Vector4D.Byte(x, y, z, w);
    }

    /**
     * Create a 1D vector of {@code short} values.
     *
     * @param x first value
     * @return new vector
     */
    static Vector1D.Short of(short x) {
        return new Vector1D.Short(x);
    }

    /**
     * Create a 2D vector of {@code short} values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    static Vector2D.Short of(short x, short y) {
        return new Vector2D.Short(x, y);
    }

    /**
     * Create a 3D vector of {@code short} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @return new vector
     */
    static Vector3D.Short of(short x, short y, short z) {
        return new Vector3D.Short(x, y, z);
    }

    /**
     * Create a 4D vector of {@code short} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @param w fourth value
     * @return new vector
     */
    static Vector4D.Short of(short x, short y, short z, short w) {
        return new Vector4D.Short(x, y, z, w);
    }

    /**
     * Create a 1D vector of {@code int} values.
     *
     * @param x first value
     * @return new vector
     */
    static Vector1D.Int of(int x) {
        return new Vector1D.Int(x);
    }

    /**
     * Create a 2D vector of {@code int} values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    static Vector2D.Int of(int x, int y) {
        return new Vector2D.Int(x, y);
    }

    /**
     * Create a 3D vector of {@code int} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @return new vector
     */
    static Vector3D.Int of(int x, int y, int z) {
        return new Vector3D.Int(x, y, z);
    }

    /**
     * Create a 4D vector of {@code int} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @param w fourth value
     * @return new vector
     */
    static Vector4D.Int of(int x, int y, int z, int w) {
        return new Vector4D.Int(x, y, z, w);
    }

    /**
     * Create a 1D vector of {@code long} values.
     *
     * @param x first value
     * @return new vector
     */
    static Vector1D.Long of(long x) {
        return new Vector1D.Long(x);
    }

    /**
     * Create a 2D vector of {@code long} values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    static Vector2D.Long of(long x, long y) {
        return new Vector2D.Long(x, y);
    }

    /**
     * Create a 3D vector of {@code long} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @return new vector
     */
    static Vector3D.Long of(long x, long y, long z) {
        return new Vector3D.Long(x, y, z);
    }

    /**
     * Create a 4D vector of {@code long} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @param w fourth value
     * @return new vector
     */
    static Vector4D.Long of(long x, long y, long z, long w) {
        return new Vector4D.Long(x, y, z, w);
    }

    /**
     * Create a 1D vector of {@code float} values.
     *
     * @param x first value
     * @return new vector
     */
    static Vector1D.Float of(float x) {
        return new Vector1D.Float(x);
    }

    /**
     * Create a 2D vector of {@code float} values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    static Vector2D.Float of(float x, float y) {
        return new Vector2D.Float(x, y);
    }

    /**
     * Create a 3D vector of {@code float} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @return new vector
     */
    static Vector3D.Float of(float x, float y, float z) {
        return new Vector3D.Float(x, y, z);
    }

    /**
     * Create a 4D vector of {@code float} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @param w fourth value
     * @return new vector
     */
    static Vector4D.Float of(float x, float y, float z, float w) {
        return new Vector4D.Float(x, y, z, w);
    }

    /**
     * Create a 1D vector of {@code double} values.
     *
     * @param x first value
     * @return new vector
     */
    static Vector1D.Double of(double x) {
        return new Vector1D.Double(x);
    }

    /**
     * Create a 2D vector of {@code double} values.
     *
     * @param x first value
     * @param y second value
     * @return new vector
     */
    static Vector2D.Double of(double x, double y) {
        return new Vector2D.Double(x, y);
    }

    /**
     * Create a 3D vector of {@code double} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @return new vector
     */
    static Vector3D.Double of(double x, double y, double z) {
        return new Vector3D.Double(x, y, z);
    }

    /**
     * Create a 4D vector of {@code double} values.
     *
     * @param x first value
     * @param y second value
     * @param z third value
     * @param w fourth value
     * @return new vector
     */
    static Vector4D.Double of(double x, double y, double z, double w) {
        return new Vector4D.Double(x, y, z, w);
    }

    /**
     * Create a vector of {@code double} values, of the given values dimension.
     * The implementation is the 1D to 4D class for dimensions 1 to 4, and {@link VectorND} otherwise.
     *
     * @param values vector values, the array is copied
     * @return new vector
     */
    static Vector<?> of(double... values) {
        return Vectors.create(values, DataType.DOUBLE);
    }

    /**
     * Create a vector of the given data type, of the given values dimension.
     * Values are cast to the data type. This method is the only one allowing to
     * create vectors of unsigned values.
     *
     * @param dataType vector data type, not null
     * @param values vector values, the array is copied
     * @return new vector
     */
    static Vector<?> of(DataType dataType, double... values) {
        return Vectors.create(values, dataType);
    }


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
     * Immutable implementations return this tuple, a new tuple is created only
     * for views on values which may change, such as array cursor samples.
     *
     * @return tuple copy, or this tuple if it is immutable.
     */
    default T copy() {
        return (T) Vectors.create(toArrayDouble(), getDataType());
    }

    /**
     * Create a copy of this tuple, with values stored in the given data type.
     * The copy has the same sample system as this tuple.
     *
     * @param  dataType  data type of the copy values.
     * @return tuple copy.
     */
    default Vector<?> copy(DataType dataType) {
        return Vectors.create(toArrayDouble(), dataType);
    }

    /**
     * Create a new vector with the value at given index replaced.
     * This vector is unchanged.
     *
     * @param indice sample index
     * @param value sample value
     * @return new vector
     * @throws IndexOutOfBoundsException if index is not valid
     */
    default Vector<?> with(int indice, double value) throws IndexOutOfBoundsException {
        if (indice < 0 || indice >= getDimension()) {
            throw new IndexOutOfBoundsException("Invalid index " + indice);
        }
        final double[] array = toArrayDouble();
        array[indice] = value;
        return Vectors.create(array, getDataType());
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
     * @return true if any value is NaN
     */
    default boolean isNaN() {
        for (int i = 0, n = getDimension(); i < n; i++) {
            if (Double.isNaN(get(i))) return true;
        }
        return false;
    }

    /**
     * @return true if any value is infinite and none is NaN
     */
    default boolean isInfinite() {
        boolean infinite = false;
        for (int i = 0, n = getDimension(); i < n; i++) {
            final double v = get(i);
            if (Double.isNaN(v)) return false;
            if (Double.isInfinite(v)) infinite = true;
        }
        return infinite;
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
    default boolean equals(Vector<?> other, double tolerance) {
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
        return Math.sqrt(lengthSquare());
    }

    /**
     * @return vector squere length
     */
    default double lengthSquare() {
        double length = 0;
        for (int i = 0, n = getDimension(); i < n; i++) {
            final double v = get(i);
            length += v * v;
        }
        return length;
    }

    /**
     * Cross product.
     * Vectors must have size 3.
     * Computed with plain arithmetic, see {@link #crossAccurate(Vector)} for nearly parallel vectors.
     *
     * @param other
     * @return cross product result
     * @throws IllegalArgumentException if a vector size is not 3
     */
    default Vector<?> cross(Vector<?> other) {
        if (getDimension() != 3 || other.getDimension() != 3) {
            throw new IllegalArgumentException("vector and other size must be 3.");
        }
        final double x = get(0), y = get(1), z = get(2);
        final double ox = other.get(0), oy = other.get(1), oz = other.get(2);
        return new Vector3D.Double(
                (y * oz) - (z * oy),
                (z * ox) - (x * oz),
                (x * oy) - (y * ox));
    }

    /**
     * Cross product computed with compensated arithmetic.
     * Vectors must have size 3.
     * This method keeps the accuracy of the result when vectors are nearly parallel,
     * see the class javadoc about accuracy.
     *
     * @param other
     * @return cross product result
     * @throws IllegalArgumentException if a vector size is not 3
     */
    default Vector<?> crossAccurate(Vector<?> other) {
        if (getDimension() != 3 || other.getDimension() != 3) {
            throw new IllegalArgumentException("vector and other size must be 3.");
        }
        final double x = get(0), y = get(1), z = get(2);
        final double ox = other.get(0), oy = other.get(1), oz = other.get(2);
        return new Vector3D.Double(
                LinearCombination.of(y, oz, -z, oy),
                LinearCombination.of(z, ox, -x, oz),
                LinearCombination.of(x, oy, -y, ox));
    }

    /**
     * Dot product.
     * Computed with plain arithmetic, see {@link #dotAccurate(Vector)} for nearly orthogonal vectors.
     *
     * @param other vector of the same dimension
     * @return dot product result
     * @throws IllegalArgumentException if vectors do not have the same dimension
     */
    default double dot(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        double dot = 0;
        for (int i=0,n=getDimension();i<n;i++){
            dot += get(i) * other.get(i);
        }
        return dot;
    }

    /**
     * Dot product computed with compensated arithmetic.
     * The products are computed exactly and their sum is compensated. This method keeps
     * the accuracy of the result when vectors are nearly orthogonal, see the class javadoc about accuracy.
     *
     * @param other vector of the same dimension
     * @return dot product result
     * @throws IllegalArgumentException if vectors do not have the same dimension
     */
    default double dotAccurate(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        return LinearCombination.dot(this, other);
    }

    /**
     * Distance between this vector and the other vector, both seen as points.
     *
     * @param other second point, of the same dimension
     * @return euclidean distance
     * @throws IllegalArgumentException if vectors do not have the same dimension
     */
    default double distance(Vector<?> other) {
        return Math.sqrt(distanceSquare(other));
    }

    /**
     * Square of the distance between this vector and the other vector, both seen as points.
     * Equivalent to {@code other.vectorTo(this).lengthSquare()} without the intermediate vector.
     *
     * @param other second point, of the same dimension
     * @return square of the euclidean distance
     * @throws IllegalArgumentException if vectors do not have the same dimension
     */
    default double distanceSquare(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        double sum = 0;
        for (int i = 0, n = getDimension(); i < n; i++) {
            final double d = get(i) - other.get(i);
            sum += d * d;
        }
        return sum;
    }

    /**
     * Increase size of the tuple by one value.
     *
     * @param d last dimension value
     * @return Vector
     */
    default Vector<?> extend(double d) {
        final int dim = getDimension();
        final double[] values = new double[dim+1];
        toArrayDouble(values, 0);
        values[dim] = d;
        return Vectors.create(values, getDataType());
    }

    /**
     * Decrease size of the tuple by one value.
     *
     * @param size number of dimension to preserve
     * @return Vector
     */
    default Vector<?> shrink(int size) {
        final double[] values = new double[size];
        for (int i = 0; i < size; i++) {
            values[i] = get(i);
        }
        return Vectors.create(values, getDataType());
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
    default Vector<?> swizzle(int ... axes) throws IndexOutOfBoundsException {
        final double[] values = new double[axes.length];
        for (int i = 0; i < axes.length; i++) {
            values[i] = get(axes[i]);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector, linear combination of this vector and the other vector : a*this + b*other.
     * The new vector has the dimension and data type of this vector.
     *
     * @param a coefficient of this vector
     * @param other second vector, of the same dimension
     * @param b coefficient of the other vector
     * @return new vector
     */
    private Vector<?> combination(double a, Vector<?> other, double b) {
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = a * get(i) + b * other.get(i);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Same as {@link #combination(double, Vector, double)} but computed with compensated arithmetic.
     */
    private Vector<?> combinationAccurate(double a, Vector<?> other, double b) {
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = LinearCombination.of(a, get(i), b, other.get(i));
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new normalized vector, this vector is unchanged.
     *
     * @return new vector
     */
    default Vector<?> normalize() {
        return scale(1.0 / length());
    }

    /**
     * Create a new vector with other vector values added to this vector values.
     * This vector is unchanged.
     *
     * @param other
     * @return new vector
     */
    default Vector<?> add(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = get(i) + other.get(i);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector with other vector values subtracted from this vector values.
     * This vector is unchanged.
     *
     * @param other
     * @return new vector
     */
    default Vector<?> subtract(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = get(i) - other.get(i);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector with this vector values multiplied by other vector values.
     * This vector is unchanged.
     *
     * @param other
     * @return new vector
     */
    default Vector<?> multiply(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = get(i) * other.get(i);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector with this vector values divided by other vector values.
     * This vector is unchanged.
     *
     * @param other
     * @return new vector
     */
    default Vector<?> divide(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = get(i) / other.get(i);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector with this vector values negated, this vector is unchanged.
     *
     * @return new vector
     */
    default Vector<?> negate() {
        return scale(-1);
    }

    /**
     * Create a new vector with this vector values scaled by given value.
     * This vector is unchanged.
     *
     * @param scale scaling factor
     * @return new vector
     */
    default Vector<?> scale(double scale) {
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = get(i) * scale;
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector which is this vector without its component along the given unit vector.
     * The result is the projection of this vector on the hyperplane perpendicular to the given vector.
     * This vector is unchanged.
     *
     * @param unit unit vector, of length 1
     * @return new vector
     */
    default Vector<?> perpendicular(Vector<?> unit) {
        AbstractVector.ensureDimension(unit, getDimension());
        return combination(1, unit, -dot(unit));
    }

    /**
     * Create a new vector going from this vector to the other vector : other - this.
     * The new vector has the dimension and data type of this vector.
     * This vector is unchanged.
     *
     * @param other target of the vector, of the same dimension
     * @return new vector
     */
    default Vector<?> vectorTo(Vector<?> other) {
        AbstractVector.ensureDimension(other, getDimension());
        return combination(-1, other, 1);
    }

    /**
     * Create a new vector, linear interpolation from this vector to the other vector :
     * this + ratio*(other-this), or exactly the other vector if ratio is 1. This vector is unchanged.
     * See the class javadoc about accuracy.
     *
     * @param other vector to interpolate toward
     * @param ratio interpolation factor, zero for this vector and one for the other vector
     * @return new vector
     */
    default Vector<?> lerp(Vector<?> other, double ratio) {
        AbstractVector.ensureDimension(other, getDimension());
        final double[] values = new double[getDimension()];
        for (int i = 0; i < values.length; i++) {
            final double v = get(i);
            values[i] = (ratio == 1) ? other.get(i) : v + ratio * (other.get(i) - v);
        }
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector, linear interpolation from this vector to the other vector :
     * (1-ratio)*this + ratio*other computed with compensated arithmetic. This vector is unchanged.
     * See the class javadoc about accuracy.
     *
     * @param other vector to interpolate toward
     * @param ratio interpolation factor, zero for this vector and one for the other vector
     * @return new vector
     */
    default Vector<?> lerpAccurate(Vector<?> other, double ratio) {
        AbstractVector.ensureDimension(other, getDimension());
        return combinationAccurate(1 - ratio, other, ratio);
    }

    /**
     * Create a new vector, spherical linear interpolation from this vector to the other vector.
     * This vector is unchanged.
     * <p>
     * Where {@link #lerp(Vector, double) } moves along the straight line joining
     * the two vectors, this moves along the arc joining them, sweeping the angle between
     * them proportionally to the ratio. Both vectors having the same length, the result
     * keeps it too, which makes this the interpolation to use for directions. When their
     * lengths differ, the angle is still swept evenly and the length is interpolated
     * linearly.
     * <p>
     * The direction follows the usual
     * <var>sin((1-ratio)·Ω)/sin(Ω)·this + sin(ratio·Ω)/sin(Ω)·other</var>, where Ω is the
     * angle between the two vectors. When the two vectors are nearly parallel the arc is
     * indistinguishable from the straight line, and dividing by <var>sin(Ω)</var> would lose
     * precision, so a linear interpolation is used instead. A vector of length zero has no
     * direction, so that case is linear too.
     *
     * @param other vector to interpolate toward
     * @param ratio interpolation factor, zero for this vector and one for the other vector
     * @return new vector
     * @throws IllegalArgumentException if both vectors point in opposite directions, in
     *         which case they define no unique arc to interpolate along
     */
    default Vector<?> slerp(Vector<?> other, double ratio) {
        return AbstractVector.slerp(this, other, ratio, (a, b) -> combination(a, other, b));
    }

    /**
     * Create a new vector, result of given transform applied on this vector.
     * The new vector has the transform target dimension and the data type of this vector.
     * This vector is unchanged.
     *
     * @param trs not null
     * @return new vector
     * @throws TransformException
     */
    default Vector<?> transform(MathTransform trs) throws TransformException {
        final double[] values = new double[trs.getTargetDimensions()];
        trs.transform(toArrayDouble(), 0, values, 0, 1);
        return Vectors.create(values, getDataType());
    }

    /**
     * Create a new vector, result of given transform applied on this vector.
     * The new vector has the transform output dimension and the data type of this vector.
     * This vector is unchanged.
     *
     * @param trs not null
     * @return new vector
     */
    default Vector<?> transform(Transform trs) {
        return Vectors.create(trs.transform(toArrayDouble(), new double[trs.getOutputDimensions()]), getDataType());
    }

    /**
     * Apply the given function on this vector and return its result.
     * This is a convenience method for chaining operations, for example
     * {@code v.transform(Foo::bar)._normalize()}. This vector is unchanged.
     *
     * @param <R> type of the function result
     * @param fn function to apply on this vector, not null
     * @return function result
     */
    default <R> R transform(Function<? super T, ? extends R> fn) {
        return fn.apply((T) this);
    }

}
