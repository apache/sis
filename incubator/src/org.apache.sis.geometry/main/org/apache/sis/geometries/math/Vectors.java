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

import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * Origin : Adapted from Unlicense-Lib
 *
 * @author Johann Sorel
 * @author Bertrand COTE
 */
public final class Vectors extends Static {

    public static Vector<?> create(CoordinateReferenceSystem crs, DataType dataType) {
        return create(SampleSystem.of(crs), dataType);
    }

    public static Vector<?> create(SampleSystem type, DataType dataType) {
        final int size = type.getSize();
        switch (dataType) {
            case BYTE :
                return switch (size) {
                    case 1 -> new Vector1D.Byte(type, false);
                    case 2 -> new Vector2D.Byte(type, false);
                    case 3 -> new Vector3D.Byte(type, false);
                    case 4 -> new Vector4D.Byte(type, false);
                    default -> new VectorND.Byte(type);
                };
            case UBYTE :
                return switch (size) {
                    case 1 -> new Vector1D.UByte(type, false);
                    case 2 -> new Vector2D.UByte(type, false);
                    case 3 -> new Vector3D.UByte(type, false);
                    case 4 -> new Vector4D.UByte(type, false);
                    default -> new VectorND.UByte(type);
                };
            case SHORT :
                return switch (size) {
                    case 1 -> new Vector1D.Short(type, false);
                    case 2 -> new Vector2D.Short(type, false);
                    case 3 -> new Vector3D.Short(type, false);
                    case 4 -> new Vector4D.Short(type, false);
                    default -> new VectorND.Short(type);
                };
            case USHORT :
                return switch (size) {
                    case 1 -> new Vector1D.UShort(type, false);
                    case 2 -> new Vector2D.UShort(type, false);
                    case 3 -> new Vector3D.UShort(type, false);
                    case 4 -> new Vector4D.UShort(type, false);
                    default -> new VectorND.UShort(type);
                };
            case INT :
                return switch (size) {
                    case 1 -> new Vector1D.Int(type, false);
                    case 2 -> new Vector2D.Int(type, false);
                    case 3 -> new Vector3D.Int(type, false);
                    case 4 -> new Vector4D.Int(type, false);
                    default -> new VectorND.Int(type);
                };
            case UINT :
                return switch (size) {
                    case 1 -> new Vector1D.UInt(type, false);
                    case 2 -> new Vector2D.UInt(type, false);
                    case 3 -> new Vector3D.UInt(type, false);
                    case 4 -> new Vector4D.UInt(type, false);
                    default -> new VectorND.UInt(type);
                };
            case LONG :
                return switch (size) {
                    case 1 -> new Vector1D.Long(type, false);
                    case 2 -> new Vector2D.Long(type, false);
                    case 3 -> new Vector3D.Long(type, false);
                    case 4 -> new Vector4D.Long(type, false);
                    default -> new VectorND.Long(type);
                };
            case FLOAT :
                return switch (size) {
                    case 1 -> new Vector1D.Float(type, false);
                    case 2 -> new Vector2D.Float(type, false);
                    case 3 -> new Vector3D.Float(type, false);
                    case 4 -> new Vector4D.Float(type, false);
                    default -> new VectorND.Float(type);
                };
            case DOUBLE :
                return switch (size) {
                    case 1 -> new Vector1D.Double(type, false);
                    case 2 -> new Vector2D.Double(type, false);
                    case 3 -> new Vector3D.Double(type, false);
                    case 4 -> new Vector4D.Double(type, false);
                    default -> new VectorND.Double(type);
                };
            default :
                throw new UnsupportedOperationException("Unexpected data type");
        }
    }

    public static Vector<?> create(int dimension, DataType dataType) {
        return create(SampleSystem.ofSize(dimension), dataType);
    }

    public static Vector<?> createByte(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.BYTE);
    }

    public static Vector<?> createByte(CoordinateReferenceSystem crs) {
        return create(crs, DataType.BYTE);
    }

    public static Vector<?> createUByte(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.UBYTE);
    }

    public static Vector<?> createUByte(CoordinateReferenceSystem crs) {
        return create(crs, DataType.UBYTE);
    }

    public static Vector<?> createShort(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.SHORT);
    }

    public static Vector<?> createShort(CoordinateReferenceSystem crs) {
        return create(crs, DataType.SHORT);
    }

    public static Vector<?> createUShort(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.USHORT);
    }

    public static Vector<?> createUShort(CoordinateReferenceSystem crs) {
        return create(crs, DataType.USHORT);
    }

    public static Vector<?> createInt(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.INT);
    }

    public static Vector<?> createInt(CoordinateReferenceSystem crs) {
        return create(crs, DataType.INT);
    }

    public static Vector<?> createUInt(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.UINT);
    }

    public static Vector<?> createUInt(CoordinateReferenceSystem crs) {
        return create(crs, DataType.UINT);
    }

    public static Vector<?> createLong(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.LONG);
    }

    public static Vector<?> createLong(CoordinateReferenceSystem crs) {
        return create(crs, DataType.LONG);
    }

    public static Vector<?> createFloat(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.FLOAT);
    }

    public static Vector<?> createFloat(CoordinateReferenceSystem crs) {
        return create(crs, DataType.FLOAT);
    }

    public static Vector<?> createDouble(int dimension) {
        return create(SampleSystem.ofSize(dimension), DataType.DOUBLE);
    }

    public static Vector<?> createDouble(CoordinateReferenceSystem crs) {
        return create(crs, DataType.DOUBLE);
    }

    public static Vector<?> castOrCopy(DirectPosition pos) {
        if (pos instanceof Vector) {
            return (Vector) pos;
        } else {
            final SampleSystem type;
            final DataType dt;
            if (pos instanceof Tuple) {
                type = ((Tuple) pos).getSampleSystem();
                dt = ((Tuple) pos).getDataType();
            } else {
                CoordinateReferenceSystem crs = pos.getCoordinateReferenceSystem();
                type = crs != null ? SampleSystem.of(pos.getCoordinateReferenceSystem()) :
                       SampleSystem.ofSize(pos.getDimension());
                dt = DataType.DOUBLE;
            }
            final Vector v = create(type, dt);
            v.set(pos);
            return v;
        }
    }

    public static Vector<?> castOrWrap(DirectPosition tuple) {
        if (tuple instanceof Vector) {
            return (Vector) tuple;
        } else {
            CoordinateReferenceSystem crs = tuple.getCoordinateReferenceSystem();
            return crs == null ? new WrapVector(tuple, tuple.getDimension()) : new WrapVector(tuple);
        }
    }

    /**
     * Create an unmodifiable view of the given tuple.
     *
     * @param tuple not null
     * @return unmodifiable view of the tuple.
     */
    public static Tuple<?> unmodifiable(Tuple<?> tuple) {
        return new TupleUnmodifiable(tuple);
    }

    private static class WrapVector extends AbstractTuple<WrapVector> implements Vector<WrapVector> {

        private final DirectPosition pos;

        public WrapVector(DirectPosition pos, int size) {
            super(size);
            this.pos = pos;
        }

        public WrapVector(DirectPosition pos) {
            super(pos.getCoordinateReferenceSystem());
            this.pos = pos;
        }

        @Override
        public DataType getDataType() {
            return (pos instanceof Tuple) ? ((Tuple) pos).getDataType() : DataType.DOUBLE;
        }

        @Override
        public double get(int indice) {
            return pos.getCoordinate(indice);
        }

        @Override
        public void set(int indice, double value) {
            pos.setCoordinate(indice, value);
        }

        @Override
        public int getDimension() {
            return pos.getDimension();
        }
    }

    /**
     * Computes the vector's length.
     *
     * @param vector input vector
     * @return length
     */
    public static double length(final double[] vector){
        return Math.sqrt(lengthSquare(vector));
    }

    /**
     * Computes the vector's length.
     *
     * @param vector vector to process
     * @return vector length
     */
    public static float length(final float[] vector){
        return (float)Math.sqrt(lengthSquare(vector));
    }

    /**
     * Computes the vector's square length.
     *
     * @param vector input vector
     * @return vector square length
     */
    public static double lengthSquare(final double[] vector){
        double length = 0;
        for(int i=0;i<vector.length;i++){
            length += vector[i]*vector[i];
        }
        return length;
    }

    /**
     * Computes the vector's square length.
     *
     * @param vector input vector
     * @return vector square length
     */
    public static float lengthSquare(final float[] vector){
        float length = 0;
        for(int i=0;i<vector.length;i++){
            length += vector[i]*vector[i];
        }
        return length;
    }

    /**
     * Computes the shortest angle between given vectors.
     * formula : acos(dot(vector,other)/(length(vector)*length(other)))
     *
     * @param vector input vector
     * @param other second vector
     * @return shortest angle in radian
     */
    public static double shortestAngle(final double[] vector, final double[] other){
        return Math.acos(cos( vector, other ));
    }

    /**
     * Computes the shortest angle between given vectors.
     * formula : acos(dot(vector,other)/(length(vector)*length(other)))
     *
     * @param vector input vector
     * @param other second vector
     * @return shortest angle in radian
     */
    public static float shortestAngle(final float[] vector, final float[] other){
        return (float)Math.acos(cos( vector, other ));
    }

    /**
     * Cosine of the angle between the two vectors.
     * formula : dot(vector,other)
     *
     * @param vector input vector
     * @param other second vector
     * @return angle cosinus value
     */
    public static double cos( double[] vector, double[] other ) {
        return dot(vector, other)/(length(vector)*length(other));
    }

    /**
     * Cosine of the angle between the two vectors.
     * formula : dot(vector,other)
     *
     * @param vector input vector
     * @param other second vector
     * @return angle cosinus value
     */
    public static float cos( float[] vector, float[] other ) {
        return dot(vector, other)/(length(vector)*length(other));
    }

    /**
     * Sinus of the angle between the two vectors.
     * The returned value is signed for 2D vectors, and unsigned for 3D vectors.
     *
     * @param vector input vector
     * @param other second vector
     * @return angle sinus value
     */
    public static double sin( double[] vector, double[] other ) {
        if( vector.length == 2 ) {
            return (vector[0]*other[1]-vector[1]*other[0])/(length(vector)*length(other));
        } else if ( vector.length == 3 ) {
            return length(cross(vector, other))/(length(vector)*length(other));
        }
        throw new IllegalArgumentException(" Vector size must be 2 or 3");
    }

    /**
     * Sinus of the angle between the two vectors.
     * The returned value is signed for 2D vectors, and unsigned for 3D vectors.
     *
     * @param vector input vector
     * @param other second vector
     * @return angle sinus value
     */
    public static float sin( float[] vector, float[] other ) {
        if ( vector.length == 2 ) {
            return (vector[0]*other[1]-vector[1]*other[0])/(length(vector)*length(other));
        } else if ( vector.length == 3 ) {
            return length(cross(vector, other))/(length(vector)*length(other));
        }
        throw new IllegalArgumentException(" Vector size must be 2 or 3");
    }

    // /////////////////////////////////////////////////////////////////////////
    // OPERATIONS WITHOuT BUFFER ///////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Copy vector into another one.
     * @param target
     * @param src
     */
    public static void copy(double[] target, double[] src) {
        if (src.length<target.length) {
            throw new IllegalArgumentException(" Source vector size must be equal or greater than target vector");
        }
        System.arraycopy(src, 0, target, 0, target.length);
    }

    /**
     * Add vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return addition of both vectors
     */
    public static double[] add(final double[] vector, final double[] other){
        return add(vector,other,null);
    }

    /**
     * Add vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return addition of both vectors
     */
    public static float[] add(final float[] vector, final float[] other){
        return add(vector,other,null);
    }

    /**
     * Subtract vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return subtraction of both vectors
     */
    public static double[] subtract(final double[] vector, final double[] other){
        return subtract(vector,other,null);
    }

    /**
     * Subtract vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return subtraction of both vectors
     */
    public static float[] subtract(final float[] vector, final float[] other){
        return subtract(vector,other,null);
    }

    /**
     * Multiply vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return multiplication of both vectors
     */
    public static double[] multiply(final double[] vector, final double[] other){
        return multiply(vector,other,null);
    }

    /**
     * Multiply vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return multiplication of both vectors
     */
    public static float[] multiply(final float[] vector, final float[] other){
        return multiply(vector,other,null);
    }

    /**
     * Divide vector and other.
     * Vectors must have the same size.
     *
     * @param vector numerators vector.
     * @param other denominators vector.
     * @return division of both vectors
     */
    public static double[] divide(final double[] vector, final double[] other){
        return divide(vector,other,null);
    }

    /**
     * Divide vector and other.
     * Vectors must have the same size.
     *
     * @param vector numerators vector.
     * @param other denominators vector.
     * @return division of both vectors
     */
    public static float[] divide(final float[] vector, final float[] other){
        return divide(vector,other,null);
    }

    /**
     * Scale vector by the given scale.
     *
     * @param vector the vector to scale.
     * @param scale the scale coefficient.
     * @return the scaled vector.
     */
    public static double[] scale(final double[] vector, final double scale){
        return scale(vector,scale,null);
    }

    /**
     * Scale vector by the given scale.
     *
     * @param vector the vector to scale.
     * @param scale the scale coefficient.
     * @return the scaled vector.
     */
    public static float[] scale(final float[] vector, final float scale){
        return scale(vector,scale,null);
    }

    /**
     * Cross product of v1 and v2.
     * Vectors must have size 3.
     *
     * @param vector first vector
     * @param other second vector
     * @return the cross product of vector by other.
     */
    public static double[] cross(final double[] vector, final double[] other){
        return cross(vector,other,null);
    }

    /**
     * Cross product of v1 and v2.
     * Vectors must have size 3.
     *
     * @param vector first vector
     * @param other second vector
     * @return the cross product of vector by other.
     */
    public static float[] cross(final float[] vector, final float[] other){
        return cross(vector,other,null);
    }

    /**
     * Dot product of vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return dot product
     */
    public static double dot(final double[] vector, final double[] other){
        double dot = 0;
        for(int i=0;i<vector.length;i++){
            dot += vector[i]*other[i];
        }
        return dot;
    }

    /**
     * Dot product of vector and other.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return dot product
     */
    public static float dot(final float[] vector, final float[] other){
        float dot = 0;
        for(int i=0;i<vector.length;i++){
            dot += vector[i]*other[i];
        }
        return dot;
    }

    /**
     * Normalizes vector.
     * Vectors must have the same size.
     *
     * @param vector the vector to normalize.
     * @return the normalized vector.
     */
    public static double[] normalize(final double[] vector){
        return normalize(vector, null);
    }

    /**
     * Normalizes vector.
     * Vectors must have the same size.
     *
     * @param vector the vector to normalize.
     * @return the normalized vector.
     */
    public static float[] normalize(final float[] vector){
        return normalize(vector, null);
    }

    /**
     * Negates the vector, equivalent to multiply all values by -1.
     *
     * @param vector the vector to negate.
     * @return the negated vector.
     */
    public static double[] negate(final double[] vector){
        return negate(vector, null);
    }

    /**
     * Negates the vector, equivalent to multiply all values by -1.
     *
     * @param vector the vector to negate.
     * @return the negated vector.
     */
    public static float[] negate(final float[] vector){
        return negate(vector, null);
    }

    /**
     * Interpolates between given vectors.
     *
     * @param start start vector (return value for ratio == 0.)
     * @param end end vector (return value for ratio == 1.)
     * @param ratio : 0 is close to start vector, 1 is on end vector
     * @return the interpolated vector.
     */
    public static float[] lerp(final float[] start, final float[] end, final float ratio) {
        return lerp(start, end, ratio, null);
    }

    /**
     * Interpolates between given vectors.
     *
     * @param start start vector (return value for ratio == 0.)
     * @param end end vector (return value for ratio == 1.)
     * @param ratio : 0 is close to start vector, 1 is on end vector
     * @return the interpolated vector.
     */
    public static double[] lerp(final double[] start, final double[] end, final double ratio) {
        return lerp(start, end, ratio, null);
    }

    // /////////////////////////////////////////////////////////////////////////
    // OPERATIONS WITH BUFFER //////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Add vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector or be null.
     * @return addition of both vectors, buffer if not null
     */
    public static double[] add(final double[] vector, final double[] other, double[] buffer) {
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]+other[i];
        }
        return buffer;
    }

    /**
     * Add vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector or be null.
     * @return addition of both vectors, buffer if not null
     */
    public static float[] add(final float[] vector, final float[] other, float[] buffer) {
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]+other[i];
        }
        return buffer;
    }

    /**
     * Subtract vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @return subtraction of both vectors, buffer if not null
     */
    public static double[] subtract(final double[] vector, final double[] other, double[] buffer){
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]-other[i];
        }
        return buffer;
    }

    /**
     * Subtract vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector or be null.
     * @return subtraction of both vectors, buffer if not null
     */
    public static float[] subtract(final float[] vector, final float[] other, float[] buffer){
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]-other[i];
        }
        return buffer;
    }

    /**
     * Multiply vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector or be null.
     * @return multiplication of both vectors, buffer if not null
     */
    public static double[] multiply(final double[] vector, final double[] other, double[] buffer){
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]*other[i];
        }
        return buffer;
    }

    /**
     * Multiply vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector or be null.
     * @return multiplication of both vectors, buffer if not null
     */
    public static float[] multiply(final float[] vector, final float[] other, float[] buffer){
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]*other[i];
        }
        return buffer;
    }

    /**
     * Divide vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector numerators vector.
     * @param other denominators vector.
     * @param buffer must have same size as vector or be null.
     * @return division of both vectors, buffer if not null
     */
    public static double[] divide(final double[] vector, final double[] other, double[] buffer){
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]/other[i];
        }
        return buffer;
    }

    /**
     * Divide vector and other, result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have the same size.
     *
     * @param vector numerators vector.
     * @param other denominators vector.
     * @param buffer must have same size as vector or be null.
     * @return division of both vectors, buffer if not null
     */
    public static float[] divide(final float[] vector, final float[] other, float[] buffer){
        if( vector.length != other.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]/other[i];
        }
        return buffer;
    }

    /**
     * Scale vector by the given scale, result is stored in buffer.
     * If buffer is null, a new vector is created.
     *
     * @param vector the vector to scale.
     * @param scale the scale coefficient.
     * @param buffer must have same size as vector or be null.
     * @return the scaled vector.
     */
    public static double[] scale(final double[] vector, final double scale, double[] buffer){
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]*scale;
        }
        return buffer;
    }

    /**
     * Scale vector by the given scale, result is stored in buffer.
     * If buffer is null, a new vector is created.
     *
     * @param vector the vector to scale.
     * @param scale the scale coefficient.
     * @param buffer must have same size as vector or be null.
     * @return the scaled vector.
     */
    public static float[] scale(final float[] vector, final float scale, float[] buffer){
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for(int i=0;i<vector.length;i++){
            buffer[i] = vector[i]*scale;
        }
        return buffer;
    }

    /**
     * Cross product of v1 and v2. result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have size 3.
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector and other or be null.
     * @return the cross product of vector by other.
     */
    public static double[] cross(final double[] vector, final double[] other, double[] buffer){
        if( vector.length!=3 || other.length!=3 ) {
            throw new IllegalArgumentException("vector and v2 size must be 3.");
        }
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector and other.");
        }

        final double newX = (vector[1] * other[2]) - (vector[2] * other[1]);
        final double newY = (vector[2] * other[0]) - (vector[0] * other[2]);
        final double newZ = (vector[0] * other[1]) - (vector[1] * other[0]);
        buffer[0] = newX;
        buffer[1] = newY;
        buffer[2] = newZ;
        return buffer;
    }

    /**
     * Cross product of v1 and v2. result is stored in buffer.
     * If buffer is null, a new vector is created.
     * Vectors must have size 3.
     * @param vector first vector
     * @param other second vector
     * @param buffer must have same size as vector and other or be null.
     * @return the cross product of vector by other.
     */
    public static float[] cross(final float[] vector, final float[] other, float[] buffer){
        if( vector.length!=3 || other.length!=3 ) {
            throw new IllegalArgumentException("vector and other size must be 3.");
        }
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector and other.");
        }

        buffer[0] = (vector[1] * other[2]) - (vector[2] * other[1]); // new X
        buffer[1] = (vector[2] * other[0]) - (vector[0] * other[2]); // new Y
        buffer[2] = (vector[0] * other[1]) - (vector[1] * other[0]); // new Z
        return buffer;
    }

    /**
     * Normalizes vector, result is stored in buffer.
     *
     * If buffer is null, a new vector is created.
     * Vectors must have the same size
     * @param vector the vector to normalize.
     * @param buffer must have same size as vector or be null.
     * @return the normalized vector.
     */
    public static double[] normalize(final double[] vector, double[] buffer){
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        scale(vector, 1d/length(vector), buffer);
        return buffer;
    }

    /**
     * Normalizes vector, result is stored in buffer.
     *
     * If buffer is null, a new vector is created.
     * Vectors must have the same size
     * @param vector the vector to normalize.
     * @param buffer must have same size as vector or be null.
     * @return the normalized vector.
     */
    public static float[] normalize(final float[] vector, float[] buffer){
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        scale(vector, 1f/length(vector), buffer);
        return buffer;
    }

    /**
     * Negates the vector, equivalent to multiply all values by -1.
     *
     * @param vector the vector to negate.
     * @param buffer must have same size as vector or be null.
     * @return the negated vector.
     */
    public static double[] negate(final double[] vector, double[] buffer){
        if( buffer == null ){
            buffer = new double[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for( int i=0; i<vector.length; i++ ) {
            buffer[i] = - vector[i];
        }
        return buffer;
    }

    /**
     * Negates the vector, equivalent to multiply all values by -1.
     *
     * @param vector the vector to negate.
     * @param buffer must have same size as vector or be null.
     * @return the negated vector.
     */
    public static float[] negate(final float[] vector, float[] buffer){
        if( buffer == null ){
            buffer = new float[vector.length];
        } else if( vector.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as vector.");
        }

        for( int i=0; i<vector.length; i++ ) {
            buffer[i] = - vector[i];
        }
        return buffer;
    }


    /**
     * Interpolates between given vectors.
     *
     * @param start start vector (return value for ratio == 0.)
     * @param end end vector (return value for ratio == 1.)
     * @param ratio : 0 is close to start vector, 1 is on end vector
     * @param buffer must have same size as start and end or be null.
     * @return the interpolated vector.
     */
    public static float[] lerp(final float[] start, final float[] end, final float ratio, float[] buffer) {
        if( start.length != end.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new float[start.length];
        } else if( start.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as start and end vectors.");
        }

        for(int i=0;i<start.length;i++){
            buffer[i] = (1-ratio)*start[i] + ratio * end[i];
        }
        return buffer;
    }

    public static double[] lerp(final double[] start, final double[] end, final double ratio, double[] buffer) {
        if( start.length != end.length ) {
            throw new IllegalArgumentException("Both vectors must have same length.");
        }
        if( buffer == null ){
            buffer = new double[start.length];
        } else if( start.length != buffer.length ) {
                throw new IllegalArgumentException("Buffer must have same length as start and end vectors.");
        }

        for(int i=0;i<start.length;i++){
            buffer[i] = (1-ratio)*start[i] + ratio * end[i];
        }
        return buffer;
    }

    // /////////////////////////////////////////////////////////////////////////
    // OPERATIONS WITH MULTIPLE ELEMENTS AT THE SAME TIME //////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Add source1 and source2, result is stored in buffer.
     * @param source1 first vectors array
     * @param source2 second vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param source2Offset second array offset
     * @param bufferOffset output buffer offset
     * @param tupleSize tuples size
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] add(double[] source1, double[] source2, double[] buffer,
            int source1Offset, int source2Offset, int bufferOffset, int tupleSize, int nbTuple){
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] + source2[source2Offset+i];
        }
        return buffer;
    }

    /**
     * Add source1 and source2, result is stored in buffer.
     * @param source1 first vectors array
     * @param source2 second vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param source2Offset second array offset
     * @param bufferOffset output buffer offset
     * @param tupleSize tuples size
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] add(float[] source1, float[] source2, float[] buffer,
            int source1Offset, int source2Offset, int bufferOffset, int tupleSize, int nbTuple){
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] + source2[source2Offset+i];
        }
        return buffer;
    }

    /**
     * Add 'addition' to all source1 elements, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param addition vector to add
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] addRegular(double[] source1, double[] buffer,
            int source1Offset, int bufferOffset, double[] addition, int nbTuple){
        final int tupleSize = addition.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] + addition[i%tupleSize];
        }
        return buffer;
    }

    /**
     * Add 'addition' to all source1 elements, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param addition vector to add
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] addRegular(float[] source1, float[] buffer,
            int source1Offset, int bufferOffset, float[] addition, int nbTuple){
        final int tupleSize = addition.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] + addition[i%tupleSize];
        }
        return buffer;
    }

    /**
     * Substract source1 and source2, result is stored in buffer.
     * @param source1 first vectors array
     * @param source2 second vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param source2Offset second array offset
     * @param bufferOffset output buffer offset
     * @param tupleSize tuples size
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] subtract(double[] source1, double[] source2, double[] buffer,
            int source1Offset, int source2Offset, int bufferOffset, int tupleSize, int nbTuple){
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] - source2[source2Offset+i];
        }
        return buffer;
    }

    /**
     * Substract source1 and source2, result is stored in buffer.
     * @param source1 first vectors array
     * @param source2 second vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param source2Offset second array offset
     * @param bufferOffset output buffer offset
     * @param tupleSize tuples size
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] subtract(float[] source1, float[] source2, float[] buffer,
            int source1Offset, int source2Offset, int bufferOffset, int tupleSize, int nbTuple){
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] - source2[source2Offset+i];
        }
        return buffer;
    }

    /**
     * Substract 'subtraction' to all source1 elements, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param subtraction vector to subtract
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] subtractRegular(double[] source1, double[] buffer,
            int source1Offset, int bufferOffset, double[] subtraction, int nbTuple){
        final int tupleSize = subtraction.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] - subtraction[i%tupleSize];
        }
        return buffer;
    }

    /**
     * Substract 'subtraction' to all source1 elements, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param subtraction vector to subtract
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] subtractRegular(float[] source1, float[] buffer,
            int source1Offset, int bufferOffset, float[] subtraction, int nbTuple){
        final int tupleSize = subtraction.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] - subtraction[i%tupleSize];
        }
        return buffer;
    }

    /**
     * Scale source1, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param scale sclaing factor
     * @param tupleSize tuples size
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] scaleRegular(double[] source1, double[] buffer, int source1Offset,
            int bufferOffset, double scale, int tupleSize, int nbTuple){
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] * scale;
        }
        return buffer;
    }

    /**
     * Scale source1, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param scale scaling factor
     * @param tupleSize tuples size
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] scaleRegular(float[] source1, float[] buffer, int source1Offset,
            int bufferOffset, float scale, int tupleSize, int nbTuple){
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] * scale;
        }
        return buffer;
    }

    /**
     * Scale source1, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param scale scaling factor
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] multiplyRegular(double[] source1, double[] buffer, int source1Offset,
            int bufferOffset, double[] scale, int nbTuple){
        final int tupleSize = scale.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] * scale[i%tupleSize];
        }
        return buffer;
    }

    /**
     * Scale source1, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param scale scaling factor
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] multiplyRegular(float[] source1, float[] buffer, int source1Offset,
            int bufferOffset, float[] scale, int nbTuple){
        final int tupleSize = scale.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] * scale[i%tupleSize];
        }
        return buffer;
    }


    /**
     * Divide source1, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param scale scaling factor
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static double[] divideRegular(double[] source1, double[] buffer, int source1Offset,
            int bufferOffset, double[] scale, int nbTuple){
        final int tupleSize = scale.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] / scale[i%tupleSize];
        }
        return buffer;
    }

    /**
     * Divide source1, result is stored in buffer.
     * @param source1 first vectors array
     * @param buffer result buffer, not null
     * @param source1Offset first array offset
     * @param bufferOffset output buffer offset
     * @param scale scaling factor
     * @param nbTuple number of tuple to process
     * @return buffer result array, same as buffer parameter
     */
    public static float[] divideRegular(float[] source1, float[] buffer, int source1Offset,
            int bufferOffset, float[] scale, int nbTuple){
        final int tupleSize = scale.length;
        for(int i=0,n=nbTuple*tupleSize;i<n;i++){
            buffer[bufferOffset+i] = source1[source1Offset+i] / scale[i%tupleSize];
        }
        return buffer;
    }


    // =========================================================================

    /**
     * From cartesian to polar coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#To_polar_coordinates_from_Cartesian_coordinates
     * <ul>
     * <li>r: cartesian's norm.</li>
     * <li>theta: angle between x and cartesian = atan2(y , x)</li>
     * </ul>
     *
     * @param cartesian { x, y }
     * @return { r, theta }
     */
    public static double[] cartesianToPolar( double[] cartesian ) {
        if( cartesian.length != 2 ) throw new IllegalArgumentException("cartesian.length must be 2({ x, y }).");
        final double r = Vectors.length( cartesian );
        final double theta = Math.atan2(cartesian[1] , cartesian[0]);
        return new double[]{ r, theta };
    }

    /**
     * From cartesian to polar coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#To_polar_coordinates_from_Cartesian_coordinates
     * <ul>
     * <li>r: cartesian's norm.</li>
     * <li>theta: angle between x and cartesian = atan2(y , x)</li>
     * </ul>
     *
     * @param cartesian { x, y }
     * @return { r, theta }
     */
    public static float[] cartesianToPolar( float[] cartesian ) {
        if( cartesian.length != 2 ) throw new IllegalArgumentException("cartesian.length must be 2({ x, y }).");
        final float r = Vectors.length( cartesian );
        final float theta = (float)Math.atan2(cartesian[1] , cartesian[0]);
        return new float[]{ r, theta };
    }

    /**
     * From polar to cartesian coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#To_Cartesian_coordinates_from_polar_coordinates
     * <ul>
     * <li>x: r*cos(theta).</li>
     * <li>y: r*sin(theta).</li>
     * </ul>
     *
     * @param polar { r, theta }
     * @return { x, y }
     */
    public static double[] polarToCartesian( double[] polar ) {
        if( polar.length != 2 ) throw new IllegalArgumentException("polar.length must be 2({ r, theta }).");
        final double x = polar[0]*Math.cos(polar[1]);
        final double y = polar[0]*Math.sin(polar[1]);
        return new double[]{ x, y };
    }

    /**
     * From polar to cartesian coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#To_Cartesian_coordinates_from_polar_coordinates
     * <ul>
     * <li>x: r*cos(theta).</li>
     * <li>y: r*sin(theta).</li>
     * </ul>
     *
     * @param polar { r, theta }
     * @return { x, y }
     */
    public static float[] polarToCartesian( float[] polar ) {
        if( polar.length != 2 ) throw new IllegalArgumentException("polar.length must be 2({ r, theta }).");
        final float x = polar[0]*(float)Math.cos(polar[1]);
        final float y = polar[0]*(float)Math.sin(polar[1]);
        return new float[]{ x, y };
    }

    /**
     * From cartesian to cylindrical coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_Cartesian_Coordinates_2
     * <ul>
     * <li>r: { x, y }'s norm.</li>
     * <li>theta: angle between x and { x, y, 0 }.</li>
     * <li>z: z (same as in cartesian).</li>
     * </ul>
     *
     * @param cartesian { x, y, z }
     * @return { r, theta, z }
     */
    public static double[] cartesianToCylindrical( double[] cartesian ) {
        if ( cartesian.length != 3 ) throw new IllegalArgumentException("cartesian.length must be 3({ x, y, z }).");
        final double[] polar = cartesianToPolar( new double[] {cartesian[0], cartesian[1]} );
        final double r = polar[0];
        final double theta = polar[1];
        final double h = cartesian[2];
        return new double[]{ r, theta, h };
    }

    /**
     * From cartesian to cylindrical coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_Cartesian_Coordinates_2
     * <ul>
     * <li>r: { x, y }'s norm.</li>
     * <li>theta: angle between x and { x, y, 0 }.</li>
     * <li>z: z (same as in cartesian).</li>
     * </ul>
     *
     * @param cartesian { x, y, z }
     * @return { r, theta, z }
     */
    public static float[] cartesianToCylindrical( float[] cartesian ) {
        if ( cartesian.length != 3 ) throw new IllegalArgumentException("cartesian.length must be 3({ x, y, z }).");
        final float[] polar = cartesianToPolar( new float[] {cartesian[0], cartesian[1]} );
        final float r = polar[0];
        final float theta = polar[1];
        final float h = cartesian[2];
        return new float[]{ r, theta, h };
    }

    /**
     * From cartesian to cylindrical coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_Cartesian_Coordinates_2
     * <ul>
     * <li>x: r*cos(theta).</li>
     * <li>y: r*sin(theta).</li>
     * <li>z: z (same as in cylindrical).</li>
     * </ul>
     *
     * @param cylindrical { r, theta, z }
     * @return { x, y, z }
     */
    public static double[] cylindricalToCartesian( double[] cylindrical ) {
        if ( cylindrical.length != 3 ) throw new IllegalArgumentException("cartesian.length must be 3({ r, theta, z }).");
        final double x = cylindrical[0]*Math.cos(cylindrical[1]);
        final double y = cylindrical[0]*Math.sin(cylindrical[1]);
        final double z = cylindrical[2];
        return new double[]{ x, y, z };
    }

     /**
     * From cartesian to cylindrical coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_Cartesian_Coordinates_2
     * <ul>
     * <li>x: r*cos(theta).</li>
     * <li>y: r*sin(theta).</li>
     * <li>z: z (same as in cylindrical).</li>
     * </ul>
     *
     * @param cylindrical { r, theta, z }
     * @return { x, y, z }
     */
    public static float[] cylindricalToCartesian( float[] cylindrical ) {
        if ( cylindrical.length != 3 ) throw new IllegalArgumentException("cartesian.length must be 3({ r, theta, z }).");
        final float x = cylindrical[0]*(float)Math.cos(cylindrical[1]);
        final float y = cylindrical[0]*(float)Math.sin(cylindrical[1]);
        final float z = cylindrical[2];
        return new float[]{ x, y, z };
    }

    // =========================================================================

    /**
     * From cartesian to spherical coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_Cartesian_coordinates
     * <ul>
     * <li>rho: cartesian's norm.</li>
     * <li>theta: angle between z and cartesian.</li>
     * <li>phi: angle between plan x.z and plan cartesian.z</li>
     * </ul>
     *
     * @param cartesian { x, y, z }
     * @return { rho, theta, phi }
     */
    public static double[] cartesianToSpherical( double[] cartesian ) {
        if( cartesian.length != 3 ) throw new IllegalArgumentException("cartesian.length must 3({ x, y, z }).");
        final double rho = Vectors.length(cartesian);
        final double theta = Math.atan2( cartesian[1], cartesian[0] );
        final double phi = Math.atan2( cartesian[2], Math.sqrt(cartesian[0]*cartesian[0] + cartesian[1]*cartesian[1]) );
        return new double[]{ rho, theta, phi };
    }

    /**
     * From cartesian to spherical coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_Cartesian_coordinates
     * <ul>
     * <li>rho: cartesian's norm.</li>
     * <li>theta: angle between z and cartesian.</li>
     * <li>phi: angle between plan x.z and plan cartesian.z</li>
     * </ul>
     *
     * @param cartesian { x, y, z }
     * @return { rho, theta, phi }
     */
    public static float[] cartesianToSpherical( float[] cartesian ) {
        if( cartesian.length != 3 ) throw new IllegalArgumentException("cartesian.length must 3({ x, y, z }).");
        final float rho = Vectors.length(cartesian);
        final float theta = (float)Math.atan2( cartesian[1], cartesian[0] );
        final float phi = (float)Math.atan2( cartesian[2], Math.sqrt(cartesian[0]*cartesian[0] + cartesian[1]*cartesian[1]) );
        return new float[]{ rho, theta, phi };
    }

    /**
     * From spherical to cartesian coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_spherical_coordinates
     * <ul>
     * <li>x: rho*cos(phi)*cos(theta).</li>
     * <li>y: rho*cos(phi)*sin(theta).</li>
     * <li>z: rho*sin(phi).</li>
     * </ul>
     *
     * @param spherical { rho, theta, phi }
     * @return { x, y, z }
     */
    public static double[] sphericalToCartesian( double[] spherical ) {
        if( spherical.length != 3 ) throw new IllegalArgumentException("cartesian.length must 3({ rho, theta, phi }).");
        final double rhoCosPhi = spherical[0]*Math.cos(spherical[2]);
        final double x = rhoCosPhi*Math.cos(spherical[1]);
        final double y = rhoCosPhi*Math.sin(spherical[1]);
        final double z = spherical[0]*Math.sin(spherical[2]);
        return new double[]{ x, y, z };
    }

    /**
     * From spherical to cartesian coordinate system transformation.
     * http://en.wikipedia.org/wiki/List_of_common_coordinate_transformations#From_spherical_coordinates
     * <ul>
     * <li>x: rho*cos(phi)*cos(theta).</li>
     * <li>y: rho*cos(phi)*sin(theta).</li>
     * <li>z: rho*sin(phi).</li>
     * </ul>
     *
     * @param spherical { rho, theta, phi }
     * @return { x, y, z }
     */
    public static float[] sphericalToCartesian( float[] spherical ) {
        if( spherical.length != 3 ) throw new IllegalArgumentException("cartesian.length must 3({ rho, theta, phi }).");
        final float rhoCosPhi = spherical[0]*(float)Math.cos(spherical[2]);
        final float x = rhoCosPhi*(float)Math.cos(spherical[1]);
        final float y = rhoCosPhi*(float)Math.sin(spherical[1]);
        final float z = spherical[0]*(float)Math.sin(spherical[2]);
        return new float[]{ x, y, z };
    }

    /**
     * Oct compression from : <a href="http://jcgt.org/published/0003/02/01/paper-lowres.pdf">http://jcgt.org/published/0003/02/01/paper-lowres.pdf</a>
     *
     * <pre>
     * {@code
     *   vec2 float32x3_to_oct(vec3 v) {
     *       vec2 p = v.xy * (1.0 / (abs(v.x) + abs(v.y) + abs(v.z)));
     *       return (v.z <= 0.0) ? ((1.0 - abs(p.yx)) * signNotZero(p)) : p;
     *   }
     * }
     * </pre>
     *
     * @param unitVector unencoded unit vector value of size 3
     * @return encoded value
     */
    public static Vector2D.Float toOctEncoding(Tuple<?> unitVector){
        ArgumentChecks.ensureCountBetween("dimension", true, 3, 3, unitVector.getDimension());
        final double x = unitVector.get(0);
        final double y = unitVector.get(1);
        final double z = unitVector.get(2);
        final float s = (float) (1f / (Math.abs(x) + Math.abs(y) + Math.abs(z)));
        final Vector2D.Float p = new Vector2D.Float((float) (s*x), (float) (s*y));
        if (z <= 0) {
            return new Vector2D.Float(
                (1f-Math.abs(p.y)) * ((p.x >= 0.0) ? +1f : -1f),
                (1f-Math.abs(p.x)) * ((p.y >= 0.0) ? +1f : -1f)
            );
        } else {
            return p;
        }
    }

    /**
     * Encode unit vector with Oct compression and store it as ushort values.
     *
     * @param unitVector unencoded unit vector value of size 3
     * @return encoded value
     */
    public static Vector2D.UShort toOctUShort(Tuple<?> unitVector){
        final Vector2D.Float oct = toOctEncoding(unitVector);
        return new Vector2D.UShort(
            (short) ( (oct.x + 1f) / 2f * 65535f),
            (short) ( (oct.y + 1f) / 2f * 65535f)
        );
    }

    /**
     * Encode unit vector with Oct compression and store it as byte values.
     *
     * @param unitVector unencoded unit vector value of size 3
     * @return encoded value
     */
    public static byte[] toOctByte(Tuple<?> unitVector){
        final Vector2D.Float oct = toOctEncoding(unitVector);
        final byte[] b = new byte[2];
        b[0] = (byte) ( (oct.x + 1f) / 2f * 255f);
        b[1] = (byte) ( (oct.y + 1f) / 2f * 255f);
        return b;
    }

    /**
     * Convert a value [0,255] to [-1,+1]
     *
     * @param b input value in range [0,255]
     * @return output value in range [-1,+1]
     */
    public static float fromSNorm(byte b){
        return ((float) (b & 0xFF)) / 255f * 2f - 1f;
    }

    /**
     * Convert a value [0,65535] to [-1,+1]
     *
     * @param b input value in range [0,255]
     * @return output value in range [-1,+1]
     */
    public static float fromSNorm(short b){
        return ((float)(b & 0xFFFF)) / 65535f * 2f - 1f;
    }

    private static float signNotZero(float v){
        return v>=0 ? +1:-1 ;
    }

    /**
     * Oct compression to unit vector.
     *
     * @param x compressed first value
     * @param y compressed second value
     * @return unit vector
     */
    public static Vector3D.Float octToNormal(byte x, byte y){
        Vector3D.Float normal = new Vector3D.Float();
        normal.x = fromSNorm(x);
        normal.y = fromSNorm(y);
        normal.z = 1f - (Math.abs(normal.x) + Math.abs(normal.y));

        if (normal.z < 0.0){
            float oldVX = normal.x;
            normal.x = (1f - Math.abs(normal.y)) * signNotZero(oldVX);
            normal.y = (1f - Math.abs(oldVX)) * signNotZero(normal.y);
        }

        normal.normalize();
        return normal;
    }

    /**
     * Oct compression to unit vector.
     *
     * @param x compressed first value
     * @param y compressed second value
     * @return unit vector
     */
    public static Vector3D.Float octToNormal(short x, short y){
        Vector3D.Float normal = new Vector3D.Float();
        normal.x = fromSNorm(x);
        normal.y = fromSNorm(y);
        normal.z = 1f - (Math.abs(normal.x) + Math.abs(normal.y));

        if (normal.z < 0.0){
            float oldVX = normal.x;
            normal.x = (1f - Math.abs(normal.y)) * signNotZero(oldVX);
            normal.y = (1f - Math.abs(oldVX)) * signNotZero(normal.y);
        }

        normal.normalize();
        return normal;
    }

    /**
     * Quantize a single tuple.
     *
     * @param tuple to quantize
     * @param quantizeBox expected bounding range of input tuple
     * @param quantizeRange resulting quantized values will range between 0 and this value inclusive.
     * @param buffer where quantized value is stored
     */
    public static <R extends Tuple<?>> R toQuantizedEncoding(Tuple<?> tuple, Envelope quantizeBox, int quantizeRange, R buffer) throws MismatchedDimensionException, TransformException {
        final MathTransform trs = quantizedTransform(quantizeBox, quantizeRange);
        if (buffer == null) {
            final DataType dt = DataType.forRange(NumberRange.create(0, true, quantizeRange, true), true);
            buffer = (R) Vectors.create(tuple.getDimension(), dt);
        }
        trs.transform(tuple, buffer);
        return buffer;
    }

    /**
     * Create transform from coordinates in quantizeBox to quantizeRange.
     *
     * @param quantizeBox expected bounding range of input tuple
     * @param quantizeRange resulting quantized values will range between 0 and this value inclusive.
     * @return transform, not null
     */
    public static MathTransform quantizedTransform(Envelope quantizeBox, int quantizeRange){
        final int dim = quantizeBox.getDimension();
        final MatrixSIS matrix = org.apache.sis.referencing.operation.matrix.Matrices.createDiagonal(dim + 1, dim + 1);
        for (int i = 0; i < dim; i++) {
            final double minx = quantizeBox.getMinimum(i);
            double scale = quantizeRange / quantizeBox.getSpan(i);
            if (!Double.isFinite(scale)) {
                scale = 1.0;
            }
            matrix.setElement(i, i, scale);
            matrix.setElement(i, dim, -minx * scale);
        }
        return MathTransforms.linear(matrix);
    }

    private Vectors(){}

}
