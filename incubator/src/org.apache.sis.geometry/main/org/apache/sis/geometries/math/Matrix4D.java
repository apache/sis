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

import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;

/**
 * 4x4 matrix
 *
 * @author Johann Sorel
 */
public class Matrix4D extends AbstractMatrix<Matrix4D> {

    private double m00,m01,m02,m03;
    private double m10,m11,m12,m13;
    private double m20,m21,m22,m23;
    private double m30,m31,m32,m33;

    public Matrix4D() {
        super(4, 4);
    }

    public Matrix4D(Matrix m) {
        super(4, 4);
        m00 = m.get(0, 0);m01 = m.get(0, 1);m02 = m.get(0, 2);m03 = m.get(0, 3);
        m10 = m.get(1, 0);m11 = m.get(1, 1);m12 = m.get(1, 2);m13 = m.get(1, 3);
        m20 = m.get(2, 0);m21 = m.get(2, 1);m22 = m.get(2, 2);m23 = m.get(2, 3);
        m30 = m.get(3, 0);m31 = m.get(3, 1);m32 = m.get(3, 2);m33 = m.get(3, 3);
    }

    public Matrix4D( double m00, double m01, double m02, double m03,
                    double m10, double m11, double m12, double m13,
                    double m20, double m21, double m22, double m23,
                    double m30, double m31, double m32, double m33) {
        super(4,4);
        this.m00 = m00;this.m01 = m01;this.m02 = m02;this.m03 = m03;
        this.m10 = m10;this.m11 = m11;this.m12 = m12;this.m13 = m13;
        this.m20 = m20;this.m21 = m21;this.m22 = m22;this.m23 = m23;
        this.m30 = m30;this.m31 = m31;this.m32 = m32;this.m33 = m33;
    }

    public Matrix4D(double[][] values) {
        super(4,4);
        if (values[0].length != 4 || values.length != 4){
            throw new IllegalArgumentException("Size must be 4x4");
        }
        m00 = values[0][0];m01 = values[0][1];m02 = values[0][2];m03 = values[0][3];
        m10 = values[1][0];m11 = values[1][1];m12 = values[1][2];m13 = values[1][3];
        m20 = values[2][0];m21 = values[2][1];m22 = values[2][2];m23 = values[2][3];
        m30 = values[3][0];m31 = values[3][1];m32 = values[3][2];m33 = values[3][3];
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;case 2:return m02;case 3:return m03;}
            case 1 : switch(col){case 0:return m10;case 1:return m11;case 2:return m12;case 3:return m13;}
            case 2 : switch(col){case 0:return m20;case 1:return m21;case 2:return m22;case 3:return m23;}
            case 3 : switch(col){case 0:return m30;case 1:return m31;case 2:return m32;case 3:return m33;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Matrix4D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){case 0:m00=value;break;case 1:m01=value;break;case 2:m02=value;break;case 3:m03=value;break;} return this;
            case 1 : switch(col){case 0:m10=value;break;case 1:m11=value;break;case 2:m12=value;break;case 3:m13=value;break;} return this;
            case 2 : switch(col){case 0:m20=value;break;case 1:m21=value;break;case 2:m22=value;break;case 3:m23=value;break;} return this;
            case 3 : switch(col){case 0:m30=value;break;case 1:m31=value;break;case 2:m32=value;break;case 3:m33=value;break;} return this;
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Matrix4D set(final Matrix<?> toCopy){
        if (toCopy instanceof Matrix3D o){
            m00 = o.m00;m01 = o.m01;m02 = o.m02;
            m10 = o.m10;m11 = o.m11;m12 = o.m12;
            m20 = o.m20;m21 = o.m21;m22 = o.m22;
            return this;
        } else if (toCopy instanceof Matrix4D o){
            m00 = o.m00;m01 = o.m01;m02 = o.m02;m03 = o.m03;
            m10 = o.m10;m11 = o.m11;m12 = o.m12;m13 = o.m13;
            m20 = o.m20;m21 = o.m21;m22 = o.m22;m23 = o.m23;
            m30 = o.m30;m31 = o.m31;m32 = o.m32;m33 = o.m33;
            return this;
        }
        return super.set(toCopy);
    }

    @Override
    public Matrix4D scale(double scale) {
        m00 *= scale;m01 *= scale;m02 *= scale;m03 *= scale;
        m10 *= scale;m11 *= scale;m12 *= scale;m13 *= scale;
        m20 *= scale;m21 *= scale;m22 *= scale;m23 *= scale;
        m30 *= scale;m31 *= scale;m32 *= scale;m33 *= scale;
        return this;
    }

    @Override
    public Matrix4D scale(double[] scale) {
        m00 *= scale[0];m01 *= scale[1];m02 *= scale[2];m03 *= scale[3];
        m10 *= scale[0];m11 *= scale[1];m12 *= scale[2];m13 *= scale[3];
        m20 *= scale[0];m21 *= scale[1];m22 *= scale[2];m23 *= scale[3];
        m30 *= scale[0];m31 *= scale[1];m32 *= scale[2];m33 *= scale[3];
        return this;
    }

    @Override
    public Matrix4D multiply(Matrix<?> other) {
        if (other instanceof Matrix4D o){
            //usual case
            double b00 = this.m00 * o.m00 + this.m01 * o.m10 + this.m02 * o.m20 + this.m03 * o.m30;
            double b01 = this.m00 * o.m01 + this.m01 * o.m11 + this.m02 * o.m21 + this.m03 * o.m31;
            double b02 = this.m00 * o.m02 + this.m01 * o.m12 + this.m02 * o.m22 + this.m03 * o.m32;
            double b03 = this.m00 * o.m03 + this.m01 * o.m13 + this.m02 * o.m23 + this.m03 * o.m33;
            double b10 = this.m10 * o.m00 + this.m11 * o.m10 + this.m12 * o.m20 + this.m13 * o.m30;
            double b11 = this.m10 * o.m01 + this.m11 * o.m11 + this.m12 * o.m21 + this.m13 * o.m31;
            double b12 = this.m10 * o.m02 + this.m11 * o.m12 + this.m12 * o.m22 + this.m13 * o.m32;
            double b13 = this.m10 * o.m03 + this.m11 * o.m13 + this.m12 * o.m23 + this.m13 * o.m33;
            double b20 = this.m20 * o.m00 + this.m21 * o.m10 + this.m22 * o.m20 + this.m23 * o.m30;
            double b21 = this.m20 * o.m01 + this.m21 * o.m11 + this.m22 * o.m21 + this.m23 * o.m31;
            double b22 = this.m20 * o.m02 + this.m21 * o.m12 + this.m22 * o.m22 + this.m23 * o.m32;
            double b23 = this.m20 * o.m03 + this.m21 * o.m13 + this.m22 * o.m23 + this.m23 * o.m33;
            double b30 = this.m30 * o.m00 + this.m31 * o.m10 + this.m32 * o.m20 + this.m33 * o.m30;
            double b31 = this.m30 * o.m01 + this.m31 * o.m11 + this.m32 * o.m21 + this.m33 * o.m31;
            double b32 = this.m30 * o.m02 + this.m31 * o.m12 + this.m32 * o.m22 + this.m33 * o.m32;
            double b33 = this.m30 * o.m03 + this.m31 * o.m13 + this.m32 * o.m23 + this.m33 * o.m33;
            m00 = b00;m01 = b01;m02 = b02;m03 = b03;
            m10 = b10;m11 = b11;m12 = b12;m13 = b13;
            m20 = b20;m21 = b21;m22 = b22;m23 = b23;
            m30 = b30;m31 = b31;m32 = b32;m33 = b33;
            return this;
        }

        return super.multiply(other);
    }

    /**
     * Get 3x3 part of the matrix.
     *
     * @return 3x3 rotation part of the matrix
     */
    public Matrix3D getRotation(){
        return new Matrix3D(
                m00, m01, m02,
                m10, m11, m12,
                m20, m21, m22);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Matrix4D setToIdentity() {
        m00=1.0; m01=0.0; m02=0.0; m03=0.0;
        m10=0.0; m11=1.0; m12=0.0; m13=0.0;
        m20=0.0; m21=0.0; m22=1.0; m23=0.0;
        m30=0.0; m31=0.0; m32=0.0; m33=1.0;
        return this;
    }

    /**
     * Transform rotation part of the matrix in quaternion.
     * @return Quaternion
     */
    public Quaternion getRotationQuaternion(){
        final Quaternion q = new Quaternion();
        q.setFromMatrix(this);
        return q;
    }

    public void setRotation(Matrix3D rotation){
        m00 = rotation.get(0, 0);m01 = rotation.get(0, 1);m02 = rotation.get(0, 2);
        m10 = rotation.get(1, 0);m11 = rotation.get(1, 1);m12 = rotation.get(1, 2);
        m20 = rotation.get(2, 0);m21 = rotation.get(2, 1);m22 = rotation.get(2, 2);
    }

    public Vector<?> getTranslation(){
        return new Vector4D.Double(m03, m13, m23, 1);
    }

    public void setTranslation(Tuple<?> translation){
        m03 = translation.get(0);
        m13 = translation.get(1);
        m23 = translation.get(2);
    }

    @Override
    public Matrix4D copy() {
        return new Matrix4D(this);
    }

    @Override
    public MatrixSIS toMatrixSIS() {
        return new Matrix4(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33);
    }

    /**
     * @return determinant
     */
    public double getDeterminant(){
        return m00 * getCofactor(m11,m12,m13, m21,m22,m23, m31,m32,m33) -
               m01 * getCofactor(m10,m12,m13, m20,m22,m23, m30,m32,m33) +
               m02 * getCofactor(m10,m11,m13, m20,m21,m23, m30,m31,m33) -
               m03 * getCofactor(m10,m11,m12, m20,m21,m22, m30,m31,m32);
    }

    /**
     * Build rotation matrix from euler angle.
     *
     * @param euler angles in radians (heading/yaw , elevation/pitch , bank/roll)
     * @return Matrix4
     */
    public Matrix4D fromEuler(Tuple<?> euler){
        set(Matrices.fromEuler(euler.toArrayDouble(), null));
        return this;
    }

    @Override
    public Matrix4D invert() {

        double s0 = m00 * m11 - m10 * m01;
        double s1 = m00 * m12 - m10 * m02;
        double s2 = m00 * m13 - m10 * m03;
        double s3 = m01 * m12 - m11 * m02;
        double s4 = m01 * m13 - m11 * m03;
        double s5 = m02 * m13 - m12 * m03;

        double c5 = m22 * m33 - m32 * m23;
        double c4 = m21 * m33 - m31 * m23;
        double c3 = m21 * m32 - m31 * m22;
        double c2 = m20 * m33 - m30 * m23;
        double c1 = m20 * m32 - m30 * m22;
        double c0 = m20 * m31 - m30 * m21;

        double invdet = 1.0 / (s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0);

        final Matrix4D buffer = new Matrix4D();

        buffer.set(0,0, ( m11 * c5 - m12 * c4 + m13 * c3) * invdet);
        buffer.set(0,1, (-m01 * c5 + m02 * c4 - m03 * c3) * invdet);
        buffer.set(0,2, ( m31 * s5 - m32 * s4 + m33 * s3) * invdet);
        buffer.set(0,3, (-m21 * s5 + m22 * s4 - m23 * s3) * invdet);

        buffer.set(1,0, (-m10 * c5 + m12 * c2 - m13 * c1) * invdet);
        buffer.set(1,1, ( m00 * c5 - m02 * c2 + m03 * c1) * invdet);
        buffer.set(1,2, (-m30 * s5 + m32 * s2 - m33 * s1) * invdet);
        buffer.set(1,3, ( m20 * s5 - m22 * s2 + m23 * s1) * invdet);

        buffer.set(2,0, ( m10 * c4 - m11 * c2 + m13 * c0) * invdet);
        buffer.set(2,1, (-m00 * c4 + m01 * c2 - m03 * c0) * invdet);
        buffer.set(2,2, ( m30 * s4 - m31 * s2 + m33 * s0) * invdet);
        buffer.set(2,3, (-m20 * s4 + m21 * s2 - m23 * s0) * invdet);

        buffer.set(3,0, (-m10 * c3 + m11 * c1 - m12 * c0) * invdet);
        buffer.set(3,1, ( m00 * c3 - m01 * c1 + m02 * c0) * invdet);
        buffer.set(3,2, (-m30 * s3 + m31 * s1 - m32 * s0) * invdet);
        buffer.set(3,3, ( m20 * s3 - m21 * s1 + m22 * s0) * invdet);

        return set(buffer);
    }

    @Override
    public Matrix4D transpose(){
        return set(new Matrix4D(Matrices.transpose(toArray2DoubleRowOrder())));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isIdentity() {
        return m00==1.0 && m01==0.0 && m02==0.0 && m03==0.0
            && m10==0.0 && m11==1.0 && m12==0.0 && m13==0.0
            && m20==0.0 && m21==0.0 && m22==1.0 && m23==0.0
            && m30==0.0 && m31==0.0 && m32==0.0 && m33==1.0 ;
    }

    @Override
    public boolean isFinite() {
        return !(
                Double.isNaN(m00) || Double.isInfinite(m00) ||
                Double.isNaN(m01) || Double.isInfinite(m01) ||
                Double.isNaN(m02) || Double.isInfinite(m02) ||
                Double.isNaN(m03) || Double.isInfinite(m03) ||
                Double.isNaN(m10) || Double.isInfinite(m10) ||
                Double.isNaN(m11) || Double.isInfinite(m11) ||
                Double.isNaN(m12) || Double.isInfinite(m12) ||
                Double.isNaN(m13) || Double.isInfinite(m13) ||
                Double.isNaN(m20) || Double.isInfinite(m20) ||
                Double.isNaN(m21) || Double.isInfinite(m21) ||
                Double.isNaN(m22) || Double.isInfinite(m22) ||
                Double.isNaN(m23) || Double.isInfinite(m23) ||
                Double.isNaN(m30) || Double.isInfinite(m30) ||
                Double.isNaN(m31) || Double.isInfinite(m31) ||
                Double.isNaN(m32) || Double.isInfinite(m32) ||
                Double.isNaN(m33) || Double.isInfinite(m33)
                );
    }

    /**
     * Build matrix from rotation,scale and translation.
     *
     * @param rotation Matrix[3x3]
     * @param scale Tuple[3]
     * @param translation Tuple[3]
     * @return Matrix4
     */
    public static Matrix4D createFromComponents(final MatrixND rotation, Tuple<?> scale, Tuple<?> translation){
        final Matrix4D matrix = new Matrix4D()
            .set(rotation)
            .scale(new VectorND.Double(scale).extend(1).toArrayDouble());
        for (int i=0;i<translation.getDimension();i++){
            matrix.set(i, 3, translation.get(i));
        }
        return matrix;
    }

    /**
     * Build rotation matrix from euler angle.
     *
     * @param euler angles in radians (heading/yaw , elevation/pitch , bank/roll)
     * @return Matrix4
     */
    public static Matrix4D createRotationEuler(final Tuple<?> euler){
        return new Matrix4D(Matrices.fromEuler(euler.toArrayDouble(), new double[4][4]));
    }

    /**
     * Create rotation matrix from 3 axis.
     * Each Tuple must be unit length(normalized)
     *
     * @param xAxis values are copied in 1th row
     * @param yAxis values are copied in 2nd row
     * @param zAxis values are copied in 3rd row
     * @return rotation matrix
     */
    public static Matrix4D createFromAxis(final Tuple<?> xAxis, final Tuple<?> yAxis, final Tuple<?> zAxis){
        final Matrix4D m = new Matrix4D().setToIdentity();
        m.setRow(0, xAxis.toArrayDouble());
        m.setRow(1, yAxis.toArrayDouble());
        m.setRow(2, zAxis.toArrayDouble());
        return m;
    }


    /**
     * Create and orbit matrix 4x4 focus on the root point (0,0,0).
     *
     * @param xAngle horizontal angle
     * @param yAngle vertical angle
     * @param rollAngle roll angle
     * @param distance distance from base
     * @return orbit matrix 4x4
     */
    public static Matrix4D focusedOrbit(final double xAngle,
            final double yAngle, double rollAngle, double distance){
        return new Matrix4D(Matrices.focusedOrbit(xAngle, yAngle, rollAngle, distance));
    }

    ///////////////////////////////////////////////////////////////////////////////
    // compute cofactor of 3x3 minor matrix without sign
    // input params are 9 elements of the minor matrix
    // NOTE: The caller must know its sign.
    ///////////////////////////////////////////////////////////////////////////////
    private static double getCofactor(
            double m0, double m1, double m2,
            double m3, double m4, double m5,
            double m6, double m7, double m8) {
        return    m0 * (m4 * m8 - m5 * m7)
                - m1 * (m3 * m8 - m5 * m6)
                + m2 * (m3 * m7 - m4 * m6);
    }

    @Override
    public void transform1(double[] vector, int srcOffset, double[] buffer, int dstOffset) {
        double d0 = m00*vector[srcOffset] + m01*vector[srcOffset+1] + m02*vector[srcOffset+2] + m03*vector[srcOffset+3];
        double d1 = m10*vector[srcOffset] + m11*vector[srcOffset+1] + m12*vector[srcOffset+2] + m13*vector[srcOffset+3];
        double d2 = m20*vector[srcOffset] + m21*vector[srcOffset+1] + m22*vector[srcOffset+2] + m23*vector[srcOffset+3];
        double d3 = m30*vector[srcOffset] + m31*vector[srcOffset+1] + m32*vector[srcOffset+2] + m33*vector[srcOffset+3];
        buffer[dstOffset  ] = d0;
        buffer[dstOffset+1] = d1;
        buffer[dstOffset+2] = d2;
        buffer[dstOffset+3] = d3;
    }

    @Override
    public void transform1(float[] vector, int srcOffset, float[] buffer, int dstOffset) {
        float d0 = (float) (m00*vector[srcOffset] + m01*vector[srcOffset+1] + m02*vector[srcOffset+2] + m03*vector[srcOffset+3]);
        float d1 = (float) (m10*vector[srcOffset] + m11*vector[srcOffset+1] + m12*vector[srcOffset+2] + m13*vector[srcOffset+3]);
        float d2 = (float) (m20*vector[srcOffset] + m21*vector[srcOffset+1] + m22*vector[srcOffset+2] + m23*vector[srcOffset+3]);
        float d3 = (float) (m30*vector[srcOffset] + m31*vector[srcOffset+1] + m32*vector[srcOffset+2] + m33*vector[srcOffset+3]);
        buffer[dstOffset  ] = d0;
        buffer[dstOffset+1] = d1;
        buffer[dstOffset+2] = d2;
        buffer[dstOffset+3] = d3;
    }

    @Override
    public Tuple transform(Tuple vector, Tuple buffer) {
        if (buffer == null) buffer = new Vector4D.Double();

        if (vector instanceof Vector4D.Double && buffer instanceof Vector4D.Double) {
            final Vector4D.Double v = (Vector4D.Double) vector;
            final Vector4D.Double b = (Vector4D.Double) buffer;
            double d0 = m00*v.x + m01*v.y + m02*v.z + m03*v.w;
            double d1 = m10*v.x + m11*v.y + m12*v.z + m13*v.w;
            double d2 = m20*v.x + m21*v.y + m22*v.z + m23*v.w;
            double d3 = m30*v.x + m31*v.y + m32*v.z + m33*v.w;
            b.x = d0;
            b.y = d1;
            b.z = d2;
            b.w = d3;
            return b;
        }

        return super.transform(vector, buffer);
    }
}
