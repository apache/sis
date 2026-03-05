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
 */
public final class Affine3D extends AbstractAffine<Affine3D> {

    double m00;
    double m01;
    double m02;
    double m03;
    double m10;
    double m11;
    double m12;
    double m13;
    double m20;
    double m21;
    double m22;
    double m23;

    public Affine3D() {
        super(3);
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m03 = 0.0;

        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;
        this.m13 = 0.0;

        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        this.m23 = 0.0;
    }

    public Affine3D(
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23) {
        super(3);
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;

    }

    public Affine3D(Affine<?> affine) {
        super(3);
        this.m00 = affine.get(0, 0);
        this.m01 = affine.get(0, 1);
        this.m02 = affine.get(0, 2);
        this.m03 = affine.get(0, 3);

        this.m10 = affine.get(1, 0);
        this.m11 = affine.get(1, 1);
        this.m12 = affine.get(1, 2);
        this.m13 = affine.get(1, 3);

        this.m20 = affine.get(2, 0);
        this.m21 = affine.get(2, 1);
        this.m22 = affine.get(2, 2);
        this.m23 = affine.get(2, 3);
    }

    public Affine3D(Matrix m) {
        super(3);
        setFromMatrix(m);
    }

    @Override
    public int getInputDimensions() {
        return 3;
    }

    @Override
    public int getOutputDimensions() {
        return 3;
    }

    public void setM00(double m00) {
        this.m00 = m00;
    }

    public void setM01(double m01) {
        this.m01 = m01;
    }

    public void setM02(double m02) {
        this.m02 = m02;
    }

    public void setM03(double m03) {
        this.m03 = m03;
    }

    public void setM10(double m10) {
        this.m10 = m10;
    }

    public void setM11(double m11) {
        this.m11 = m11;
    }

    public void setM12(double m12) {
        this.m12 = m12;
    }

    public void setM13(double m13) {
        this.m13 = m13;
    }

    public void setM20(double m20) {
        this.m20 = m20;
    }

    public void setM21(double m21) {
        this.m21 = m21;
    }

    public void setM22(double m22) {
        this.m22 = m22;
    }

    public void setM23(double m23) {
        this.m23 = m23;
    }

    public double getM00() {
        return m00;
    }

    public double getM01() {
        return m01;
    }

    public double getM02() {
        return m02;
    }

    public double getM03() {
        return m03;
    }

    public double getM10() {
        return m10;
    }

    public double getM11() {
        return m11;
    }

    public double getM12() {
        return m12;
    }

    public double getM13() {
        return m13;
    }

    public double getM20() {
        return m20;
    }

    public double getM21() {
        return m21;
    }

    public double getM22() {
        return m22;
    }

    public double getM23() {
        return m23;
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;case 2:return m02;case 3:return m03;}
            case 1 : switch(col){case 0:return m10;case 1:return m11;case 2:return m12;case 3:return m13;}
            case 2 : switch(col){case 0:return m20;case 1:return m21;case 2:return m22;case 3:return m23;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Affine3D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){case 0:m00=value;break;case 1:m01=value;break;case 2:m02=value;break;case 3:m03=value;break;} break;
            case 1 : switch(col){case 0:m10=value;break;case 1:m11=value;break;case 2:m12=value;break;case 3:m13=value;break;} break;
            case 2 : switch(col){case 0:m20=value;break;case 1:m21=value;break;case 2:m22=value;break;case 3:m23=value;break;} break;
            default: throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
        }
        return this;
    }

    @Override
    public Tuple<?> transform(Tuple<?> source, Tuple<?> dest) {
        if (dest==null) dest = new Vector3D.Double();

        if (source instanceof Vector3D.Double s && dest instanceof Vector3D.Double d) {
            final double rx = m00*s.x + m01*s.y + m02*s.z + m03;
            final double ry = m10*s.x + m11*s.y + m12*s.z + m13;
            final double rz = m20*s.x + m21*s.y + m22*s.z + m23;
            d.x = rx;
            d.y = ry;
            d.z = rz;
            return d;
        }

        return super.transform(source, dest);
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        double rx = m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02*source[sourceOffset+2] + m03;
        double ry = m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12*source[sourceOffset+2] + m13;
        double rz = m20*source[sourceOffset] + m21*source[sourceOffset+1] + m22*source[sourceOffset+2] + m23;
        dest[destOffset  ] = rx;
        dest[destOffset+1] = ry;
        dest[destOffset+2] = rz;
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        float rx = (float) (m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02*source[sourceOffset+2] + m03);
        float ry = (float) (m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12*source[sourceOffset+2] + m13);
        float rz = (float) (m20*source[sourceOffset] + m21*source[sourceOffset+1] + m22*source[sourceOffset+2] + m23);
        dest[destOffset  ] = rx;
        dest[destOffset+1] = ry;
        dest[destOffset+2] = rz;
    }

    @Override
    public Affine3D setFromMatrix(Matrix<?> m) {
        m00 = m.get(0, 0);
        m01 = m.get(0, 1);
        m02 = m.get(0, 2);
        m03 = m.get(0, 3);

        m10 = m.get(1, 0);
        m11 = m.get(1, 1);
        m12 = m.get(1, 2);
        m13 = m.get(1, 3);

        m20 = m.get(2, 0);
        m21 = m.get(2, 1);
        m22 = m.get(2, 2);
        m23 = m.get(2, 3);
        return this;
    }

    @Override
    public Affine3D multiply(Affine<?> affine) {
        double b00,b01,b02,b03;
        double b10,b11,b12,b13;
        double b20,b21,b22,b23;

        if (affine instanceof Affine3D o) {
            b00 = this.m00 * o.m00 + this.m01 * o.m10 + this.m02 * o.m20;
            b01 = this.m00 * o.m01 + this.m01 * o.m11 + this.m02 * o.m21;
            b02 = this.m00 * o.m02 + this.m01 * o.m12 + this.m02 * o.m22 ;
            b03 = this.m00 * o.m03 + this.m01 * o.m13 + this.m02 * o.m23 + this.m03;
            b10 = this.m10 * o.m00 + this.m11 * o.m10 + this.m12 * o.m20;
            b11 = this.m10 * o.m01 + this.m11 * o.m11 + this.m12 * o.m21;
            b12 = this.m10 * o.m02 + this.m11 * o.m12 + this.m12 * o.m22;
            b13 = this.m10 * o.m03 + this.m11 * o.m13 + this.m12 * o.m23 + this.m13;
            b20 = this.m20 * o.m00 + this.m21 * o.m10 + this.m22 * o.m20;
            b21 = this.m20 * o.m01 + this.m21 * o.m11 + this.m22 * o.m21;
            b22 = this.m20 * o.m02 + this.m21 * o.m12 + this.m22 * o.m22;
            b23 = this.m20 * o.m03 + this.m21 * o.m13 + this.m22 * o.m23 + this.m23;
        } else {
            final Affine<?> o = affine;
            b00 = this.m00 * o.get(0,0) + this.m01 * o.get(1,0) + this.m02 * o.get(2,0);
            b01 = this.m00 * o.get(0,1) + this.m01 * o.get(1,1) + this.m02 * o.get(2,1);
            b02 = this.m00 * o.get(0,2) + this.m01 * o.get(1,2) + this.m02 * o.get(2,2) ;
            b03 = this.m00 * o.get(0,3) + this.m01 * o.get(1,3) + this.m02 * o.get(2,3) + this.m03;
            b10 = this.m10 * o.get(0,0) + this.m11 * o.get(1,0) + this.m12 * o.get(2,0);
            b11 = this.m10 * o.get(0,1) + this.m11 * o.get(1,1) + this.m12 * o.get(2,1);
            b12 = this.m10 * o.get(0,2) + this.m11 * o.get(1,2) + this.m12 * o.get(2,2);
            b13 = this.m10 * o.get(0,3) + this.m11 * o.get(1,3) + this.m12 * o.get(2,3) + this.m13;
            b20 = this.m20 * o.get(0,0) + this.m21 * o.get(1,0) + this.m22 * o.get(2,0);
            b21 = this.m20 * o.get(0,1) + this.m21 * o.get(1,1) + this.m22 * o.get(2,1);
            b22 = this.m20 * o.get(0,2) + this.m21 * o.get(1,2) + this.m22 * o.get(2,2);
            b23 = this.m20 * o.get(0,3) + this.m21 * o.get(1,3) + this.m22 * o.get(2,3) + this.m23;
        }
        m00 = b00; m01 = b01; m02 = b02; m03 = b03;
        m10 = b10; m11 = b11; m12 = b12; m13 = b13;
        m20 = b20; m21 = b21; m22 = b22; m23 = b23;
        return this;
    }

    @Override
    public Matrix4D toMatrix() {
        return new Matrix4D(
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                  0,   0,   0,   1);
    }

    @Override
    public Matrix<?> toMatrix(Matrix<?> buffer) {
        if (buffer == null) return toMatrix();
        buffer.set(0, 0, m00);
        buffer.set(0, 1, m01);
        buffer.set(0, 2, m02);
        buffer.set(0, 3, m03);

        buffer.set(1, 0, m10);
        buffer.set(1, 1, m11);
        buffer.set(1, 2, m12);
        buffer.set(1, 3, m13);

        buffer.set(2, 0, m20);
        buffer.set(2, 1, m21);
        buffer.set(2, 2, m22);
        buffer.set(2, 3, m23);

        buffer.set(3, 0, 0);
        buffer.set(3, 1, 0);
        buffer.set(3, 2, 0);
        buffer.set(3, 3, 1);
        return buffer;
    }

    /**
     * Create a rotation matrix from given angle and axis.
     *
     * http://en.wikipedia.org/wiki/Rotation_matrix
     *
     * @param angle rotation angle in radians
     * @param rotationAxis Tuple 3
     * @return this affine
     */
    public Affine3D setToRotation(final double angle, final Tuple<?> rotationAxis) {

        final double[][] rot = Matrices.createRotation4(angle, rotationAxis, null);
        m00 = rot[0][0];
        m01 = rot[0][1];
        m02 = rot[0][2];
        m03 = rot[0][3];
        m10 = rot[1][0];
        m11 = rot[1][1];
        m12 = rot[1][2];
        m13 = rot[1][3];
        m20 = rot[2][0];
        m21 = rot[2][1];
        m22 = rot[2][2];
        m23 = rot[2][3];

        return this;
    }

    @Override
    public Affine3D copy() {
        return new Affine3D(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
    }
}
