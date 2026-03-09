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
import org.apache.sis.referencing.operation.matrix.MatrixSIS;

/**
 * 3x3 matrix
 *
 * @author Johann Sorel
 */
public class Matrix3D extends AbstractMatrix<Matrix3D> {

    //package private for fast access by other classes
    double m00,m01,m02;
    double m10,m11,m12;
    double m20,m21,m22;

    /**
     * New identity matrix3D
     */
    public Matrix3D() {
        super(3, 3);
        m00 = 1;
        m11 = 1;
        m22 = 1;
    }

    public Matrix3D(ReadOnly.Matrix<?> m) {
        super(3, 3);
        m00 = m.get(0, 0);
        m01 = m.get(0, 1);
        m02 = m.get(0, 2);
        m10 = m.get(1, 0);
        m11 = m.get(1, 1);
        m12 = m.get(1, 2);
        m20 = m.get(2, 0);
        m21 = m.get(2, 1);
        m22 = m.get(2, 2);
    }

    public Matrix3D(double[][] values) {
        super(3, 3);
        if (values[0].length != 3 || values.length != 3){
            throw new IllegalArgumentException("Size must be 3x3");
        }
        m00 = values[0][0];
        m01 = values[0][1];
        m02 = values[0][2];
        m10 = values[1][0];
        m11 = values[1][1];
        m12 = values[1][2];
        m20 = values[2][0];
        m21 = values[2][1];
        m22 = values[2][2];
    }

    public Matrix3D(double m00, double m01, double m02,
                   double m10, double m11, double m12,
                   double m20, double m21, double m22) {
        super(3, 3);
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;case 2:return m02;}
            case 1 : switch(col){case 0:return m10;case 1:return m11;case 2:return m12;}
            case 2 : switch(col){case 0:return m20;case 1:return m21;case 2:return m22;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Matrix3D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){case 0:m00=value;break;case 1:m01=value;break;case 2:m02=value;break;} return this;
            case 1 : switch(col){case 0:m10=value;break;case 1:m11=value;break;case 2:m12=value;break;} return this;
            case 2 : switch(col){case 0:m20=value;break;case 1:m21=value;break;case 2:m22=value;break;} return this;
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Matrix3D set(final ReadOnly.Matrix<?> toCopy){
        if (toCopy instanceof Matrix3D o){
            m00 = o.m00;m01 = o.m01;m02 = o.m02;
            m10 = o.m10;m11 = o.m11;m12 = o.m12;
            m20 = o.m20;m21 = o.m21;m22 = o.m22;
            return this;
        }
        return super.set(toCopy);
    }

    public void setAll(double value) {
        m00 = value;
        m01 = value;
        m02 = value;
        m10 = value;
        m11 = value;
        m12 = value;
        m20 = value;
        m21 = value;
        m22 = value;
    }

    @Override
    public Matrix3D setToIdentity() {
        m00=1.0; m01=0.0; m02=0.0;
        m10=0.0; m11=1.0; m12=0.0;
        m20=0.0; m21=0.0; m22=1.0;
        return this;
    }

    @Override
    public boolean isIdentity() {
        return m00==1.0 && m01==0.0 && m02==0.0
            && m10==0.0 && m11==1.0 && m12==0.0
            && m20==0.0 && m21==0.0 && m22==1.0 ;
    }

    @Override
    public boolean isFinite() {
        return !(
                Double.isNaN(m00) || Double.isInfinite(m00) ||
                Double.isNaN(m01) || Double.isInfinite(m01) ||
                Double.isNaN(m02) || Double.isInfinite(m02) ||
                Double.isNaN(m10) || Double.isInfinite(m10) ||
                Double.isNaN(m11) || Double.isInfinite(m11) ||
                Double.isNaN(m12) || Double.isInfinite(m12) ||
                Double.isNaN(m20) || Double.isInfinite(m20) ||
                Double.isNaN(m21) || Double.isInfinite(m21) ||
                Double.isNaN(m22) || Double.isInfinite(m22)
                );
    }

    @Override
    public Matrix3D add(ReadOnly.Matrix<?> other) {
        if (other instanceof Matrix3D o){
            //usual case
            this.m00 += o.m00;
            this.m01 += o.m01;
            this.m02 += o.m02;
            this.m10 += o.m10;
            this.m11 += o.m11;
            this.m12 += o.m12;
            this.m20 += o.m20;
            this.m21 += o.m21;
            this.m22 += o.m22;
        } else {
            this.m00 += other.get(0,0);
            this.m01 += other.get(0,1);
            this.m02 += other.get(0,2);
            this.m10 += other.get(1,0);
            this.m11 += other.get(1,1);
            this.m12 += other.get(1,2);
            this.m20 += other.get(2,0);
            this.m21 += other.get(2,1);
            this.m22 += other.get(2,2);
        }

        return this;
    }

    @Override
    public Matrix3D invert() {
        if (m20==0 && m21==0 && m22==1){
            //affine transform
            double m00 = this.m00;
            double m10 = this.m10;
            double m01 = this.m01;
            double m11 = this.m11;
            double m02 = this.m02;
            double m12 = this.m12;
            double dt = m00 * m11 - m10 * m01;
            this.m00 = m11/dt;
            this.m10 = -m10/dt;
            this.m01 = -m01/dt;
            this.m11 = m00/dt;
            this.m02 = (m01 * m12 - m11 * m02) / dt;
            this.m12 = -(m00 * m12 - m10 * m02) / dt;
            return this;
        }
        return super.invert();
    }

    @Override
    public Matrix3D scale(double scale) {
        m00 *= scale;m01 *= scale;m02 *= scale;
        m10 *= scale;m11 *= scale;m12 *= scale;
        m20 *= scale;m21 *= scale;m22 *= scale;
        return this;
    }

    @Override
    public Matrix3D scale(double[] scale) {
        m00 *= scale[0];m01 *= scale[1];m02 *= scale[2];
        m10 *= scale[0];m11 *= scale[1];m12 *= scale[2];
        m20 *= scale[0];m21 *= scale[1];m22 *= scale[2];
        return this;
    }

    @Override
    public Matrix3D multiply(ReadOnly.Matrix<?> other) {
        if (other instanceof Matrix3D o){
            //usual case
            double b00 = this.m00 * o.m00 + this.m01 * o.m10 + this.m02 * o.m20;
            double b01 = this.m00 * o.m01 + this.m01 * o.m11 + this.m02 * o.m21;
            double b02 = this.m00 * o.m02 + this.m01 * o.m12 + this.m02 * o.m22;
            double b10 = this.m10 * o.m00 + this.m11 * o.m10 + this.m12 * o.m20;
            double b11 = this.m10 * o.m01 + this.m11 * o.m11 + this.m12 * o.m21;
            double b12 = this.m10 * o.m02 + this.m11 * o.m12 + this.m12 * o.m22;
            double b20 = this.m20 * o.m00 + this.m21 * o.m10 + this.m22 * o.m20;
            double b21 = this.m20 * o.m01 + this.m21 * o.m11 + this.m22 * o.m21;
            double b22 = this.m20 * o.m02 + this.m21 * o.m12 + this.m22 * o.m22;
            m00 = b00;m01 = b01;m02 = b02;
            m10 = b10;m11 = b11;m12 = b12;
            m20 = b20;m21 = b21;m22 = b22;
            return this;
        }
        return super.multiply(other);
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        double d0 = m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02*source[sourceOffset+2];
        double d1 = m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12*source[sourceOffset+2];
        double d2 = m20*source[sourceOffset] + m21*source[sourceOffset+1] + m22*source[sourceOffset+2];
        dest[destOffset  ] = d0;
        dest[destOffset+1] = d1;
        dest[destOffset+2] = d2;
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        double d0 = m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02*source[sourceOffset+2];
        double d1 = m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12*source[sourceOffset+2];
        double d2 = m20*source[sourceOffset] + m21*source[sourceOffset+1] + m22*source[sourceOffset+2];
        dest[destOffset  ] = (float) d0;
        dest[destOffset+1] = (float) d1;
        dest[destOffset+2] = (float) d2;
    }

    @Override
    public Tuple<?> transform(ReadOnly.Tuple<?> vector, Tuple<?> buffer) {
        if (buffer == null) buffer = new Vector3D.Double();

        if (vector instanceof Vector3D.Double v && buffer instanceof Vector3D.Double b) {
            double rx = m00*v.x + m01*v.y + m02*v.z;
            double ry = m10*v.x + m11*v.y + m12*v.z;
            double rz = m20*v.x + m21*v.y + m22*v.z;
            b.x = rx;
            b.y = ry;
            b.z = rz;
            return b;
        }

        return super.transform(vector, buffer);
    }

    /**
     * Transform rotation matrix in quaternion.
     * @return Quaternion
     */
    public Quaternion toQuaternion(){
        final Quaternion q = new Quaternion();
        q.setFromMatrix(this);
        return q;
    }

    /**
     * Convert to euler angles.
     * @return euler angle in radians (heading/yaw , elevation/pitch , bank/roll)
     */
    public Vector<?> toEuler(){
        return new VectorND.Double(Matrices.toEuler(toArray2Double(ROW_ORDER), null));
    }

    /**
     * Build rotation matrix from euler angle.
     *
     * @param euler in radians (heading/yaw , elevation/pitch , bank/roll)
     * @return this matrix
     */
    public Matrix3D setFromEuler(ReadOnly.Tuple<?> euler){
        set(Matrices.fromEuler(euler.toArrayDouble(), new double[3][3]), ROW_ORDER);
        return this;
    }

    /**
     * @param angle in radians
     * @param rotationAxis rotation axis
     * @return this matrix
     */
    public Matrix3D setFromAngle(final double angle, final ReadOnly.Tuple<?> rotationAxis){
        final double fCos = Math.cos(angle);
        final double fSin = Math.sin(angle);
        final double fOneMinusCos = (1.0) - fCos;
        final double fX2 = rotationAxis.get(0) * rotationAxis.get(0);
        final double fY2 = rotationAxis.get(1) * rotationAxis.get(1);
        final double fZ2 = rotationAxis.get(2) * rotationAxis.get(2);
        final double fXYM = rotationAxis.get(0) * rotationAxis.get(1) * fOneMinusCos;
        final double fXZM = rotationAxis.get(0) * rotationAxis.get(2) * fOneMinusCos;
        final double fYZM = rotationAxis.get(1) * rotationAxis.get(2) * fOneMinusCos;
        final double fXSin = rotationAxis.get(0) * fSin;
        final double fYSin = rotationAxis.get(1) * fSin;
        final double fZSin = rotationAxis.get(2) * fSin;

        m00 = fX2 * fOneMinusCos + fCos;
        m01 = fXYM - fZSin;
        m02 = fXZM + fYSin;
        m10 = fXYM + fZSin;
        m11 = fY2 * fOneMinusCos + fCos;
        m12 = fYZM - fXSin;
        m20 = fXZM - fYSin;
        m21 = fYZM + fXSin;
        m22 = fZ2 * fOneMinusCos + fCos;
        return this;
    }

    /**
     * Create a rotation matrix as the concatenation of rotation on x,y,z axis.
     *
     * @param x in radians
     * @param y in radians
     * @param z in radians
     * @return
     */
    public Matrix3D setFromAngles(double x, double y, double z){
        setFromAngle(x, new Vector3D.Double(1, 0, 0));
        Matrix3D r2 = new Matrix3D().setFromAngle(y, new Vector3D.Double(0, 1, 0));
        Matrix3D r3 = new Matrix3D().setFromAngle(z, new Vector3D.Double(0, 0, 1));
        this.multiply(r2);
        this.multiply(r3);
        return this;
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
    public Matrix3D setFromAxis(final ReadOnly.Tuple<?> xAxis, final ReadOnly.Tuple<?> yAxis, final ReadOnly.Tuple<?> zAxis){
        setToIdentity();
        this.setRow(0, xAxis.toArrayDouble());
        this.setRow(1, yAxis.toArrayDouble());
        this.setRow(2, zAxis.toArrayDouble());
        return this;
    }

    /**
     * Create rotation matrix to move v1 on v2.
     *
     * @param v1 moving vector
     * @param v2 target vector
     * @return this matrix
     */
    public Matrix3D setFromVectors(ReadOnly.Vector<?> v1, ReadOnly.Vector<?> v2) {
        v1 = v1.copy().normalize();
        v2 = v2.copy().normalize();
        final double angle = Math.acos(v1.dot(v2));
        if (angle == 0){
            //vectors are colinear
            return setToIdentity();
        }
        final Vector<?> axis = v1.cross(v2).normalize();
        return setFromAngle(angle, axis);
    }

    public Matrix3D setFromUpAndRight(ReadOnly.Vector<?> v, ReadOnly.Vector<?> u) {
        v = v.copy().normalize();
        u = u.copy().normalize();

        //W = Normalized(Cross(V,U))
        Vector<?> w = v.cross(u).normalize();

        //to ensure it is correctly perpendicular
        //U = Normalized(Cross(W,V))
        u = w.cross(v).normalize();
        setCol(0, u.toArrayDouble());
        setCol(1, w.toArrayDouble());
        setCol(2, v.toArrayDouble());
        return this;
    }

    @Override
    public Matrix3D setFromAffine(ReadOnly.Affine<?> affine) {
        m00 = affine.get(0, 0);
        m01 = affine.get(0, 1);
        m02 = affine.get(0, 2);
        m10 = affine.get(1, 0);
        m11 = affine.get(1, 1);
        m12 = affine.get(1, 2);
        m20 = 0;
        m21 = 0;
        m22 = 1;
        return this;
    }

    @Override
    public Matrix3D copy() {
        return new Matrix3D(this);
    }

    @Override
    public MatrixSIS toMatrixSIS() {
        return new Matrix3(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    /**
     * @return determinant
     */
    public double getDeterminant(){
        return m00 * (m11 * m22 - m12 * m21) -
               m01 * (m10 * m22 - m12 * m20) +
               m02 * (m10 * m21 - m11 * m20);
    }

}
