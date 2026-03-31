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
 * @author Johann Sorel
 */
public final class Affine1D extends AbstractAffine<Affine1D> implements Transform1D {

    double m00;
    double m01;

    public Affine1D() {
        super(1);
        this.m00 = 1.0;
        this.m01 = 0.0;
    }

    /**
     *
     * @param m00 also called scale
     * @param m01 also called translate
     */
    public Affine1D(double m00, double m01) {
        super(1);
        this.m00 = m00;
        this.m01 = m01;
    }

    public Affine1D(ReadOnly.Affine<?> affine) {
        super(1);
        this.m00 = affine.get(0, 0);
        this.m01 = affine.get(0, 1);
    }

    public Affine1D(ReadOnly.Matrix<?> m) {
        super(1);
        setFromMatrix(m);
    }

    @Override
    public int getInputDimensions() {
        return 1;
    }

    @Override
    public int getOutputDimensions() {
        return 1;
    }

    public void setM00(double m00) {
        this.m00 = m00;
    }

    public void setM01(double m01) {
        this.m01 = m01;
    }

    public double getM00() {
        return m00;
    }

    public double getM01() {
        return m01;
    }

    public double getScale() {
        return m00;
    }

    public double getTranslation() {
        return m01;
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Affine1D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){
                case 0:m00=value;break;
                case 1:m01=value;break;
            } break;
            default: throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
        }
        return this;
    }

    @Override
    public float transform(float source) {
        return (float) (m00*source + m01);
    }

    @Override
    public double transform(double source) {
        return m00*source + m01;
    }

    @Override
    public Tuple<?> transform(ReadOnly.Tuple<?> source, Tuple<?> dest) {
        if (dest == null) dest = new Vector1D.Double();
        dest.set(0, m00*source.get(0) + m01);
        return dest;
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        dest[destOffset] = m00*source[sourceOffset] + m01;
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        dest[destOffset] = (float) (m00*source[sourceOffset] + m01);
    }

    @Override
    public Affine1D invert() {
        m01 = -(m01/m00);
        m00 = 1.0/m00;
        return this;
    }

    @Override
    public Affine1D setFromMatrix(ReadOnly.Matrix<?> m) {
        m00 = m.get(0, 0);
        m01 = m.get(0, 1);
        return this;
    }

    @Override
    public Matrix2D toMatrix() {
        return new Matrix2D(m00, m01, 0, 1);
    }

    @Override
    public Affine1D copy() {
        return new Affine1D(m00, m01);
    }

}
