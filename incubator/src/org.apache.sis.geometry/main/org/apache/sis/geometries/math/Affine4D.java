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
public final class Affine4D extends AbstractAffine<Affine4D> {

    private double m00;
    private double m01;
    private double m02;
    private double m03;
    private double m04;

    private double m10;
    private double m11;
    private double m12;
    private double m13;
    private double m14;

    private double m20;
    private double m21;
    private double m22;
    private double m23;
    private double m24;

    private double m30;
    private double m31;
    private double m32;
    private double m33;
    private double m34;

    public Affine4D() {
        super(4);
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m03 = 0.0;
        this.m04 = 0.0;

        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;
        this.m13 = 0.0;
        this.m14 = 0.0;

        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        this.m23 = 0.0;
        this.m24 = 0.0;

        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.m33 = 1.0;
        this.m34 = 0.0;
    }

    public Affine4D(
            double m00, double m01, double m02, double m03, double m04,
            double m10, double m11, double m12, double m13, double m14,
            double m20, double m21, double m22, double m23, double m24,
            double m30, double m31, double m32, double m33, double m34) {
        super(4);
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m04 = m04;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m14 = m14;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m24 = m24;

        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        this.m34 = m34;
    }

    public Affine4D(Affine<?> affine) {
        super(4);
        this.m00 = affine.get(0, 0);
        this.m01 = affine.get(0, 1);
        this.m02 = affine.get(0, 2);
        this.m03 = affine.get(0, 3);
        this.m04 = affine.get(0, 4);

        this.m10 = affine.get(1, 0);
        this.m11 = affine.get(1, 1);
        this.m12 = affine.get(1, 2);
        this.m13 = affine.get(1, 3);
        this.m14 = affine.get(1, 4);

        this.m20 = affine.get(2, 0);
        this.m21 = affine.get(2, 1);
        this.m22 = affine.get(2, 2);
        this.m23 = affine.get(2, 3);
        this.m24 = affine.get(2, 4);

        this.m30 = affine.get(3, 0);
        this.m31 = affine.get(3, 1);
        this.m32 = affine.get(3, 2);
        this.m33 = affine.get(3, 3);
        this.m34 = affine.get(3, 4);
    }

    public Affine4D(Matrix m) {
        super(4);
        setFromMatrix(m);
    }

    @Override
    public int getInputDimensions() {
        return 4;
    }

    @Override
    public int getOutputDimensions() {
        return 4;
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

    public void setM04(double m04) {
        this.m04 = m04;
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

    public void setM14(double m14) {
        this.m14 = m14;
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

    public void setM24(double m24) {
        this.m24 = m24;
    }

    public void setM30(double m30) {
        this.m30 = m30;
    }

    public void setM31(double m31) {
        this.m31 = m31;
    }

    public void setM32(double m32) {
        this.m32 = m32;
    }

    public void setM33(double m33) {
        this.m33 = m33;
    }

    public void setM34(double m34) {
        this.m34 = m34;
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

    public double getM04() {
        return m04;
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

    public double getM14() {
        return m14;
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

    public double getM24() {
        return m24;
    }

    public double getM30() {
        return m30;
    }

    public double getM31() {
        return m31;
    }

    public double getM32() {
        return m32;
    }

    public double getM33() {
        return m33;
    }

    public double getM34() {
        return m34;
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;case 2:return m02;case 3:return m03;case 4:return m04;}
            case 1 : switch(col){case 0:return m10;case 1:return m11;case 2:return m12;case 3:return m13;case 4:return m14;}
            case 2 : switch(col){case 0:return m20;case 1:return m21;case 2:return m22;case 3:return m23;case 4:return m24;}
            case 3 : switch(col){case 0:return m30;case 1:return m31;case 2:return m32;case 3:return m33;case 4:return m34;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Affine4D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){case 0:m00=value;break;case 1:m01=value;break;case 2:m02=value;break;case 3:m03=value;break;case 4:m04=value;} break;
            case 1 : switch(col){case 0:m10=value;break;case 1:m11=value;break;case 2:m12=value;break;case 3:m13=value;break;case 4:m14=value;} break;
            case 2 : switch(col){case 0:m20=value;break;case 1:m21=value;break;case 2:m22=value;break;case 3:m23=value;break;case 4:m24=value;} break;
            case 3 : switch(col){case 0:m30=value;break;case 1:m31=value;break;case 2:m32=value;break;case 3:m33=value;break;case 4:m34=value;} break;
            default: throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
        }
        return this;
    }

    @Override
    public Tuple<?> transform(Tuple<?> source, Tuple<?> dest) {
        if (dest == null) dest = new Vector4D.Double();

        if (source instanceof Vector4D.Double s && dest instanceof Vector4D.Double d) {
            final double rx = m00*s.x + m01*s.y + m02*s.z + m03*s.w + m04;
            final double ry = m10*s.x + m11*s.y + m12*s.z + m13*s.w + m14;
            final double rz = m20*s.x + m21*s.y + m22*s.z + m23*s.w + m24;
            final double rw = m30*s.x + m31*s.y + m32*s.z + m33*s.w + m34;
            d.x = rx;
            d.y = ry;
            d.z = rz;
            d.w = rw;
            return d;
        }

        return super.transform(source, dest);
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        double rx = m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02*source[sourceOffset+2] + m03*source[sourceOffset+3] + m04;
        double ry = m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12*source[sourceOffset+2] + m13*source[sourceOffset+3] + m14;
        double rz = m20*source[sourceOffset] + m21*source[sourceOffset+1] + m22*source[sourceOffset+2] + m23*source[sourceOffset+3] + m24;
        double rw = m30*source[sourceOffset] + m31*source[sourceOffset+1] + m32*source[sourceOffset+2] + m33*source[sourceOffset+3] + m34;
        dest[destOffset  ] = rx;
        dest[destOffset+1] = ry;
        dest[destOffset+2] = rz;
        dest[destOffset+3] = rw;
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        float rx = (float) (m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02*source[sourceOffset+2] + m03*source[sourceOffset+3] + m04);
        float ry = (float) (m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12*source[sourceOffset+2] + m13*source[sourceOffset+3] + m14);
        float rz = (float) (m20*source[sourceOffset] + m21*source[sourceOffset+1] + m22*source[sourceOffset+2] + m23*source[sourceOffset+3] + m24);
        float rw = (float) (m30*source[sourceOffset] + m31*source[sourceOffset+1] + m32*source[sourceOffset+2] + m33*source[sourceOffset+3] + m34);
        dest[destOffset  ] = rx;
        dest[destOffset+1] = ry;
        dest[destOffset+2] = rz;
        dest[destOffset+3] = rw;
    }

    @Override
    public Affine4D setFromMatrix(Matrix m) {
        m00 = m.get(0, 0);
        m01 = m.get(0, 1);
        m02 = m.get(0, 2);
        m03 = m.get(0, 3);
        m04 = m.get(0, 4);

        m10 = m.get(1, 0);
        m11 = m.get(1, 1);
        m12 = m.get(1, 2);
        m13 = m.get(1, 3);
        m14 = m.get(1, 4);

        m20 = m.get(2, 0);
        m21 = m.get(2, 1);
        m22 = m.get(2, 2);
        m23 = m.get(2, 3);
        m24 = m.get(2, 4);

        m30 = m.get(3, 0);
        m31 = m.get(3, 1);
        m32 = m.get(3, 2);
        m33 = m.get(3, 3);
        m34 = m.get(3, 4);
        return this;
    }

    @Override
    public Affine4D multiply(Affine<?> affine) {
        double b00,b01,b02,b03,b04;
        double b10,b11,b12,b13,b14;
        double b20,b21,b22,b23,b24;
        double b30,b31,b32,b33,b34;

        if (affine instanceof Affine4D o) {
            b00 = this.m00 * o.m00 + this.m01 * o.m10 + this.m02 * o.m20 + this.m03 * o.m30;
            b01 = this.m00 * o.m01 + this.m01 * o.m11 + this.m02 * o.m21 + this.m03 * o.m31;
            b02 = this.m00 * o.m02 + this.m01 * o.m12 + this.m02 * o.m22 + this.m03 * o.m32;
            b03 = this.m00 * o.m03 + this.m01 * o.m13 + this.m02 * o.m23 + this.m03 * o.m33;
            b04 = this.m00 * o.m04 + this.m01 * o.m14 + this.m02 * o.m24 + this.m03 * o.m34 + this.m04;

            b10 = this.m10 * o.m00 + this.m11 * o.m10 + this.m12 * o.m20 + this.m13 * o.m30;
            b11 = this.m10 * o.m01 + this.m11 * o.m11 + this.m12 * o.m21 + this.m13 * o.m31;
            b12 = this.m10 * o.m02 + this.m11 * o.m12 + this.m12 * o.m22 + this.m13 * o.m32;
            b13 = this.m10 * o.m03 + this.m11 * o.m13 + this.m12 * o.m23 + this.m13 * o.m33;
            b14 = this.m10 * o.m04 + this.m11 * o.m14 + this.m12 * o.m24 + this.m13 * o.m34 + this.m14;

            b20 = this.m20 * o.m00 + this.m21 * o.m10 + this.m22 * o.m20 + this.m23 * o.m30;
            b21 = this.m20 * o.m01 + this.m21 * o.m11 + this.m22 * o.m21 + this.m23 * o.m31;
            b22 = this.m20 * o.m02 + this.m21 * o.m12 + this.m22 * o.m22 + this.m23 * o.m32;
            b23 = this.m20 * o.m03 + this.m21 * o.m13 + this.m22 * o.m23 + this.m23 * o.m33;
            b24 = this.m20 * o.m04 + this.m21 * o.m14 + this.m22 * o.m24 + this.m23 * o.m34 + this.m24;

            b30 = this.m30 * o.m00 + this.m31 * o.m10 + this.m32 * o.m20 + this.m33 * o.m30;
            b31 = this.m30 * o.m01 + this.m31 * o.m11 + this.m32 * o.m21 + this.m33 * o.m31;
            b32 = this.m30 * o.m02 + this.m31 * o.m12 + this.m32 * o.m22 + this.m33 * o.m32;
            b33 = this.m30 * o.m03 + this.m31 * o.m13 + this.m32 * o.m23 + this.m33 * o.m33;
            b34 = this.m30 * o.m04 + this.m31 * o.m14 + this.m32 * o.m24 + this.m33 * o.m34 + this.m34;
        } else {
            final Affine<?> o = affine;
            b00 = this.m00 * o.get(0,0) + this.m01 * o.get(1,0) + this.m02 * o.get(2,0) + this.m03 * o.get(3,0);
            b01 = this.m00 * o.get(0,1) + this.m01 * o.get(1,1) + this.m02 * o.get(2,1) + this.m03 * o.get(3,1);
            b02 = this.m00 * o.get(0,2) + this.m01 * o.get(1,2) + this.m02 * o.get(2,2) + this.m03 * o.get(3,2);
            b03 = this.m00 * o.get(0,3) + this.m01 * o.get(1,3) + this.m02 * o.get(2,3) + this.m03 * o.get(3,3);
            b04 = this.m00 * o.get(0,4) + this.m01 * o.get(1,4) + this.m02 * o.get(2,4) + this.m03 * o.get(3,4) + this.m04;

            b10 = this.m10 * o.get(0,0) + this.m11 * o.get(1,0) + this.m12 * o.get(2,0) + this.m13 * o.get(3,0);
            b11 = this.m10 * o.get(0,1) + this.m11 * o.get(1,1) + this.m12 * o.get(2,1) + this.m13 * o.get(3,1);
            b12 = this.m10 * o.get(0,2) + this.m11 * o.get(1,2) + this.m12 * o.get(2,2) + this.m13 * o.get(3,2);
            b13 = this.m10 * o.get(0,3) + this.m11 * o.get(1,3) + this.m12 * o.get(2,3) + this.m13 * o.get(3,3);
            b14 = this.m10 * o.get(0,4) + this.m11 * o.get(1,4) + this.m12 * o.get(2,4) + this.m13 * o.get(3,4) + this.m14;

            b20 = this.m20 * o.get(0,0) + this.m21 * o.get(1,0) + this.m22 * o.get(2,0) + this.m23 * o.get(3,0);
            b21 = this.m20 * o.get(0,1) + this.m21 * o.get(1,1) + this.m22 * o.get(2,1) + this.m23 * o.get(3,1);
            b22 = this.m20 * o.get(0,2) + this.m21 * o.get(1,2) + this.m22 * o.get(2,2) + this.m23 * o.get(3,2);
            b23 = this.m20 * o.get(0,3) + this.m21 * o.get(1,3) + this.m22 * o.get(2,3) + this.m23 * o.get(3,3);
            b24 = this.m20 * o.get(0,4) + this.m21 * o.get(1,4) + this.m22 * o.get(2,4) + this.m23 * o.get(3,4) + this.m24;

            b30 = this.m30 * o.get(0,0) + this.m31 * o.get(1,0) + this.m32 * o.get(2,0) + this.m33 * o.get(3,0);
            b31 = this.m30 * o.get(0,1) + this.m31 * o.get(1,1) + this.m32 * o.get(2,1) + this.m33 * o.get(3,1);
            b32 = this.m30 * o.get(0,2) + this.m31 * o.get(1,2) + this.m32 * o.get(2,2) + this.m33 * o.get(3,2);
            b33 = this.m30 * o.get(0,3) + this.m31 * o.get(1,3) + this.m32 * o.get(2,3) + this.m33 * o.get(3,3);
            b34 = this.m30 * o.get(0,4) + this.m31 * o.get(1,4) + this.m32 * o.get(2,4) + this.m33 * o.get(3,4) + this.m34;
        }
        m00 = b00; m01 = b01; m02 = b02; m03 = b03; m04 = b04;
        m10 = b10; m11 = b11; m12 = b12; m13 = b13; m14 = b14;
        m20 = b20; m21 = b21; m22 = b22; m23 = b23; m24 = b24;
        m30 = b30; m31 = b31; m32 = b32; m33 = b33; m34 = b34;
        return this;
    }

    @Override
    public Matrix toMatrix() {
        final Matrix m = MatrixND.create(5, 5);
        m.set(0, 0, m00);m.set(0, 1, m01);m.set(0, 2, m02);m.set(0, 3, m03);m.set(0, 4, m04);
        m.set(1, 0, m10);m.set(1, 1, m11);m.set(1, 2, m12);m.set(1, 3, m13);m.set(1, 4, m14);
        m.set(2, 0, m20);m.set(2, 1, m21);m.set(2, 2, m22);m.set(2, 3, m23);m.set(2, 4, m24);
        m.set(3, 0, m30);m.set(3, 1, m31);m.set(3, 2, m32);m.set(3, 3, m33);m.set(3, 4, m34);
        m.set(4, 0,   0);m.set(4, 1,   0);m.set(4, 2,   0);m.set(4, 3,   0);m.set(4, 4,   1);
        return m;
    }

    @Override
    public Matrix toMatrix(Matrix buffer) {
        if (buffer == null) return toMatrix();
        buffer.set(0, 0, m00);
        buffer.set(0, 1, m01);
        buffer.set(0, 2, m02);
        buffer.set(0, 3, m03);
        buffer.set(0, 4, m04);

        buffer.set(1, 0, m10);
        buffer.set(1, 1, m11);
        buffer.set(1, 2, m12);
        buffer.set(1, 3, m13);
        buffer.set(1, 4, m14);

        buffer.set(2, 0, m20);
        buffer.set(2, 1, m21);
        buffer.set(2, 2, m22);
        buffer.set(2, 3, m23);
        buffer.set(2, 4, m24);

        buffer.set(3, 0, m30);
        buffer.set(3, 1, m31);
        buffer.set(3, 2, m32);
        buffer.set(3, 3, m33);
        buffer.set(3, 4, m34);

        buffer.set(4, 0, 0);
        buffer.set(4, 1, 0);
        buffer.set(4, 2, 0);
        buffer.set(4, 3, 0);
        buffer.set(4, 4, 1);
        return buffer;
    }

    @Override
    public Affine4D copy() {
        return new Affine4D(m00, m01, m02, m03, m04, m10, m11, m12, m13, m14, m20, m21, m22, m23, m24, m30, m31, m32, m33, m34);
    }
}
