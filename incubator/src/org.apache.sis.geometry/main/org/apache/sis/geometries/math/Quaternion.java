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

import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * Quaternion object.
 *
 * Definition : http://en.wikipedia.org/wiki/Quaternion
 *
 * Original code from Unlicense.science
 */
public class Quaternion extends VectorND.Double {

    private static final Vector3D.Double UNIT_X = new Vector3D.Double(1,0,0);
    private static final Vector3D.Double UNIT_Y = new Vector3D.Double(0,1,0);

    /**
     * Default quaternion with values [0,0,0,1] .
     */
    public Quaternion() {
        super(new double[]{0,0,0,1});
    }

    /**
     * Quaternion from 4 values.
     * @param x quaternion first value
     * @param y quaternion second value
     * @param z quaternion third value
     * @param w quaternion fourth value
     */
    public Quaternion(double x, double y, double z, double w) {
        super(new double[]{x,y,z,w});
    }

    /**
     * Expected an array of size 4.
     * Warning : no copy of the array is made.
     * @param values no copy of the array is made.
     */
    public Quaternion(double[] values) {
        super(values);
    }

    /**
     * Expect an array of size 4.
     * Warning : a copy of the array is made.
     * @param values a copy of the array is made.
     */
    public Quaternion(float[] values) {
        super(values);
    }

    /**
     * Expect an tuple of size 4.
     * @param v quaternion values to copy from
     */
    public Quaternion(Tuple<?> v) {
        super(v);
        if (v.getDimension()!= 4){
            throw new IllegalArgumentException("Tuple must be of size 4");
        }
    }

    /**
     * Convenient method to get value at ordinate 0.
     *
     * @throws IndexOutOfBoundsException if indice is out of range
     * @return double
     */
    public double getX() throws IndexOutOfBoundsException {
        return get(0);
    }

    /**
     * Convenient method to get value at ordinate 1.
     *
     * @throws IndexOutOfBoundsException if indice is out of range
     * @return double
     */
    public double getY() throws IndexOutOfBoundsException {
        return get(1);
    }

    /**
     * Convenient method to get value at ordinate 2.
     *
     * @throws IndexOutOfBoundsException if indice is out of range
     * @return double
     */
    public double getZ() throws IndexOutOfBoundsException {
        return get(2);
    }

    /**
     * Convenient method to get value at ordinate 3.
     *
     * @throws IndexOutOfBoundsException if indice is out of range
     * @return double
     */
    public double getW() throws IndexOutOfBoundsException {
        return get(3);
    }

    /**
     * Convenient method to set value at ordinate 0.
     *
     * @param x first ordinate value
     * @throws IndexOutOfBoundsException if indice is out of range
     */
    public void setX(double x) throws IndexOutOfBoundsException {
        set(0, x);
    }

    /**
     * Convenient method to set value at ordinate 1.
     *
     * @param y second ordinate value
     * @throws IndexOutOfBoundsException if indice is out of range
     */
    public void setY(double y) throws IndexOutOfBoundsException {
        set(1, y);
    }

    /**
     * Convenient method to set value at ordinate 2.
     *
     * @param z third ordinate value
     * @throws IndexOutOfBoundsException if indice is out of range
     */
    public void setZ(double z) throws IndexOutOfBoundsException {
        set(2, z);
    }

    /**
     * Convenient method to set value at ordinate 3.
     *
     * @param w fourth ordinate value
     * @throws InvalidIndexException if indice is out of range
     */
    public void setW(double w) throws IndexOutOfBoundsException {
        set(3, w);
    }

    /**
     * Normalize this quaternion.
     * @return this quaternion
     */
    @Override
    public Quaternion normalize(){
        Quaternions.normalize(values, values);
        return this;
    }

    /**
     * Calculate quaternion norm.
     * @return norm
     */
    public double norm() {
        return Quaternions.length(values);
    }

    /**
     * Calculate quaternion conjugate.
     * @return this quaternion
     */
    public Quaternion conjugate() {
        set(Quaternions.conjugate(values, null));
        return this;
    }

    /**
     * Add two quaternions.
     * @param other
     * @return this quaternion
     */
    public Quaternion add(Quaternion other) {
        set(Quaternions.add(values, other.values, null));
        return this;
    }

    /**
     * Multiply two quaternions. store result in this quaternion.
     * @param other
     * @return this Quaternion
     */
    public Quaternion multiply(Quaternion other) {
        Quaternions.multiplyQuaternion(values, other.values, values);
        return this;
    }

    /**
     * Calculate inverse quaternion.
     * @return this quaternion
     */
    public Quaternion inverse() {
        set(Quaternions.inverse(values, null));
        return this;
    }

    /**
     * Create a copy of this quaternion.
     * @return Quaternion, never null
     */
    @Override
    public Quaternion copy() {
        return new Quaternion(this);
    }

    /**
     * Rotate the given vector, store the result in buffer.
     * @param v vector to rotate
     * @param buffer can be null
     * @return rotated vector
     */
    public Vector<?> rotate(Vector<?> v, Vector<?> buffer){
        if (buffer == null) buffer = v.copy();
        Matrix3 m = toMatrix3();
        double[] r = m.multiply(v.toArrayDouble());
        buffer.set(r);
        return buffer;
    }

    /**
     * Rotate the given vector, store the result in buffer.
     * @param v vector to rotate
     * @param buffer can be null
     * @return rotated vector
     */
    public Vector rotate(Vector3D.Double v, Vector<?> buffer) {
        if (buffer == null) buffer = v.copy();
        double qx = values[0];
        double qy = values[1];
        double qz = values[2];
        double qw = values[3];
        // var qvec = [qx, qy, qz];
        // var uv = vec3.cross([], qvec, a);
        double uvx = qy * v.z - qz * v.y;
        double uvy = qz * v.x - qx * v.z;
        double uvz = qx * v.y - qy * v.x;
        // var uuv = vec3.cross([], qvec, uv);
        double uuvx = qy * uvz - qz * uvy;
        double uuvy = qz * uvx - qx * uvz;
        double uuvz = qx * uvy - qy * uvx;
        // vec3.scale(uv, uv, 2 * w);
        double w2 = qw * 2;
        uvx *= w2;
        uvy *= w2;
        uvz *= w2;
        // vec3.scale(uuv, uuv, 2);
        uuvx *= 2;
        uuvy *= 2;
        uuvz *= 2;
        // return vec3.add(out, a, vec3.add(out, uv, uuv));
        buffer.set(0, v.x + uvx + uuvx);
        buffer.set(1, v.y + uvy + uuvy);
        buffer.set(2, v.z + uvz + uuvz);
        return buffer;
    }

    /**
     * Create an linear interpolated quaternion.
     * A ratio of 0 while return the same values as this quaternion
     * A ratio of 1 while return the same values as other quaternion
     *
     * @param other not null
     * @param ratio range [0...1]
     * @param buffer can be null
     * @return this interpolated quaternion
     */
    public Quaternion lerp(final Quaternion other, final double ratio){
        final double[] lerp = Quaternions.lerp(values, other.toArrayDouble(), ratio, new double[4]);
        set(lerp);
        return this;
    }

    /**
     * Create a spherical interpolated quaternion.
     * A ratio of 0 while return the same values as this quaternion
     * A ratio of 1 while return the same values as other quaternion
     *
     * @param other not null
     * @param ratio range [0...1]
     * @param buffer can be null
     * @return this interpolated quaternion
     */
    public Quaternion slerp(final Quaternion other, final double ratio){
        final double[] slerp = Quaternions.slerp(values, other.toArrayDouble(), ratio, new double[4]);
        set(slerp);
        return this;
    }

    /**
     * Transform quaternion in a rotation matrix 3x3.
     * Source : http://jeux.developpez.com/faq/math/?page=quaternions#Q54
     *
     * @return Matrix3
     */
    public Matrix3 toMatrix3() {
        return Matrices.toMatrix3(Quaternions.toMatrix(values, null));
    }

    /**
     * Transform quaternion in a rotation matrix 4x4.
     * @return Matrix4
     */
    public Matrix4 toMatrix4() {
        final double[][] matrix = new double[4][4];
        Quaternions.toMatrix(values, matrix);
        matrix[3][3] = 1;
        return Matrices.toMatrix4(matrix);
    }

    /**
     * Convert to euler angles.
     * @return euler angle in radians (heading/yaw , elevation/pitch , bank/roll)
     */
    public VectorND.Double toEuler(){
        return new VectorND.Double(Matrices.toEuler(Matrices.toArray(toMatrix3()), null));
    }

    /**
     * Convert quaternion to axis angle.
     * @param axisBuffer
     * @return angle in radians
     */
    public double toAxisAngle(Tuple axisBuffer){
        final double[] values = Vectors.normalize(this.values);
        final double[] array = new double[4];
        final double angle = Quaternions.toAxisAngle(values,array);
        axisBuffer.set(array);
        return angle;
    }

    /**
     * Extract rotation from matrix.
     * http://jeux.developpez.com/faq/math/?page=quaternions#Q55
     *
     * @param matrix
     * @return Quaternion
     */
    public Quaternion fromMatrix(final MatrixSIS matrix){
        final double[][] m = Matrices.toArray(matrix);
        final double trace = m[0][0] + m[1][1] + m[2][2] + 1;

        final double s,x,y,z,w;
        if (trace>0){
            s = 0.5 / Math.sqrt(trace);
            x = ( m[2][1] - m[1][2] ) * s;
            y = ( m[0][2] - m[2][0] ) * s;
            z = ( m[1][0] - m[0][1] ) * s;
            w = 0.25 / s;
        } else if ((m[0][0] >= m[1][1]) && (m[0][0] >= m[2][2])) {
            s = Math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2.0;
            x = 0.25 * s;
            y = (m[0][1] + m[1][0]) / s;
            z = (m[0][2] + m[2][0]) / s;
            w = (m[1][2] - m[2][1]) / s;
        } else if (m[1][1] >= m[2][2]) {
            s = Math.sqrt(1.0 - m[0][0] + m[1][1] - m[2][2]) * 2;
            x = (m[0][1] + m[1][0]) / s;
            y = 0.25 * s;
            z = (m[1][2] + m[2][1]) / s;
            w = (m[0][2] - m[2][0]) / s;
        } else {
            s = Math.sqrt(1.0 - m[0][0] - m[1][1] + m[2][2]) * 2;
            x = (m[0][2] + m[2][0]) / s;
            y = (m[1][2] + m[2][1]) / s;
            z = 0.25 * s;
            w = (m[0][1] - m[1][0]) / s;
        }
        set(new double[]{x,y,z,w});
        return this;
    }

    /**
     * Set quaternion values from rotation axis and angle.
     * @param axis rotation axis, not null
     * @param angle rotation angle, in radians
     * @return this quaternion
     */
    public Quaternion fromAngle(Tuple axis, double angle) {
        Quaternions.fromAngle(axis, angle, values);
        return this;
    }

    /**
     * Set quaternion values from euler angles.
     *
     * @param euler angles in radians (heading/yaw , elevation/pitch , bank/roll)
     * @return this quaternion
     */
    public Quaternion fromEuler(Vector euler) {
        double[][] matrix = Matrices.fromEuler(euler.toArrayDouble(), new double[3][3]); //row / col
        return fromMatrix(Matrices.toMatrix3(matrix));
    }

    /**
     * Set quaternion values to shortest rotation between the two unit vectors.
     *
     * @param base starting vector of unit length
     * @param target target vector of unit length
     * @return this quaternion
     */
    public Quaternion fromUnitVectors(Vector base, Vector target) {
        final double dot = base.dot(target);
        if (dot < -0.999999) {
            //try to find a better value on other axis
            Vector tmpvec3 = UNIT_X.cross(base);
            if (tmpvec3.length() < 0.000001) {
                tmpvec3 = UNIT_Y.cross(base);
            }
            tmpvec3.normalize();
            fromAngle(tmpvec3, Math.PI);
        } else if (dot > 0.999999) {
            //no rotation
            values[0] = 0;
            values[1] = 0;
            values[2] = 0;
            values[3] = 1;
        } else {
            final Vector cross = base.cross(target);
            values[0] = cross.get(0);
            values[1] = cross.get(1);
            values[2] = cross.get(2);
            values[3] = 1 + dot;
            normalize();
        }
        return this;
    }

}
