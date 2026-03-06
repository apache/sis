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
public final class Affine2D extends AbstractAffine<Affine2D> {

    double m00;
    double m01;
    double m02;
    double m10;
    double m11;
    double m12;

    public Affine2D() {
        super(2);
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;
    }

    public Affine2D(double m00, double m01, double m02, double m10, double m11, double m12) {
        super(2);
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
    }

    public Affine2D(ReadOnly.Affine<?> affine) {
        super(2);
        this.m00 = affine.get(0, 0);
        this.m01 = affine.get(0, 1);
        this.m02 = affine.get(0, 2);
        this.m10 = affine.get(1, 0);
        this.m11 = affine.get(1, 1);
        this.m12 = affine.get(1, 2);
    }

    public Affine2D(ReadOnly.Matrix<?> m) {
        super(2);
        setFromMatrix(m);
    }

    @Override
    public int getInputDimensions() {
        return 2;
    }

    @Override
    public int getOutputDimensions() {
        return 2;
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

    public void setM10(double m10) {
        this.m10 = m10;
    }

    public void setM11(double m11) {
        this.m11 = m11;
    }

    public void setM12(double m12) {
        this.m12 = m12;
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

    public double getM10() {
        return m10;
    }

    public double getM11() {
        return m11;
    }

    public double getM12() {
        return m12;
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;case 2:return m02;}
            case 1 : switch(col){case 0:return m10;case 1:return m11;case 2:return m12;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Affine2D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){case 0:m00=value;break;case 1:m01=value;break;case 2:m02=value;break;} break;
            case 1 : switch(col){case 0:m10=value;break;case 1:m11=value;break;case 2:m12=value;break;} break;
            default: throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
        }
        return this;
    }

    @Override
    public Tuple<?> transform(ReadOnly.Tuple<?> source, Tuple<?> dest) {
        if (dest == null) dest = new Vector2D.Double();
        dest.set(0, m00*source.get(0) + m01*source.get(1) + m02);
        dest.set(1, m10*source.get(0) + m11*source.get(1) + m12);
        return dest;
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        double rx = m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02;
        double ry = m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12;
        dest[destOffset  ] = rx;
        dest[destOffset+1] = ry;
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        float rx = (float) (m00*source[sourceOffset] + m01*source[sourceOffset+1] + m02);
        float ry = (float) (m10*source[sourceOffset] + m11*source[sourceOffset+1] + m12);
        dest[destOffset  ] = rx;
        dest[destOffset+1] = ry;
    }

    @Override
    public Affine2D setFromMatrix(ReadOnly.Matrix<?> m) {
        m00 = m.get(0, 0);
        m01 = m.get(0, 1);
        m02 = m.get(0, 2);
        m10 = m.get(1, 0);
        m11 = m.get(1, 1);
        m12 = m.get(1, 2);
        return this;
    }

    @Override
    public Affine2D multiply(ReadOnly.Affine<?> affine) {
        double b00,b01,b02,b10,b11,b12;

        if (affine instanceof Affine2D o) {
            b00 = this.m00 * o.m00 + this.m01 * o.m10;
            b01 = this.m00 * o.m01 + this.m01 * o.m11;
            b02 = this.m00 * o.m02 + this.m01 * o.m12 + this.m02;
            b10 = this.m10 * o.m00 + this.m11 * o.m10;
            b11 = this.m10 * o.m01 + this.m11 * o.m11;
            b12 = this.m10 * o.m02 + this.m11 * o.m12 + this.m12;
        } else {
            final ReadOnly.Affine<?> o = affine;
            b00 = this.m00 * o.get(0, 0) + this.m01 * o.get(1, 0);
            b01 = this.m00 * o.get(0, 1) + this.m01 * o.get(1, 1);
            b02 = this.m00 * o.get(0, 2) + this.m01 * o.get(1, 2) + this.m02;
            b10 = this.m10 * o.get(0, 0) + this.m11 * o.get(1, 0);
            b11 = this.m10 * o.get(0, 1) + this.m11 * o.get(1, 1);
            b12 = this.m10 * o.get(0, 2) + this.m11 * o.get(1, 2) + this.m12;
        }
        m00 = b00;
        m01 = b01;
        m02 = b02;
        m10 = b10;
        m11 = b11;
        m12 = b12;
        return this;
    }

    @Override
    public Matrix<?> toMatrix() {
        return new Matrix3D(m00, m01, m02, m10, m11, m12, 0, 0, 1);
    }

    @Override
    public Matrix<?> toMatrix(Matrix<?> buffer) {
        if (buffer == null) return toMatrix();
        buffer.set(0, 0, m00);
        buffer.set(0, 1, m01);
        buffer.set(0, 2, m02);
        buffer.set(1, 0, m10);
        buffer.set(1, 1, m11);
        buffer.set(1, 2, m12);
        buffer.set(2, 0, 0);
        buffer.set(2, 1, 0);
        buffer.set(2, 2, 1);
        return buffer;
    }

    public Matrix4D toMatrix4() {
        return new Matrix4D(
                m00, m01,   0, m02,
                m10, m11,   0, m12,
                  0,   0,   1,   0,
                  0,   0,   0,   1);
    }

    @Override
    public Affine2D copy() {
        return new Affine2D(m00, m01, m02, m10, m11, m12);
    }
}
