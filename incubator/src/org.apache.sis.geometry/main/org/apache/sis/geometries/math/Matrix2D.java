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

import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class Matrix2D extends AbstractMatrix<Matrix2D> {

    //package private for fast access by other classes
    double m00,m01;
    double m10,m11;

    /**
     * New identity matrix2D
     */
    public Matrix2D() {
        super(2, 2);
        m00 = 1;
        m11 = 1;
    }

    public Matrix2D(Matrix<?> m) {
        super(2, 2);
        m00 = m.get(0, 0);
        m01 = m.get(0, 1);
        m10 = m.get(1, 0);
        m11 = m.get(1, 1);
    }

    public Matrix2D(double[][] values) {
        super(2, 2);
        if (values[0].length != 2 || values.length != 2) {
            throw new IllegalArgumentException("Size must be 2x2");
        }
        m00 = values[0][0];
        m01 = values[0][1];
        m10 = values[1][0];
        m11 = values[1][1];
    }

    public Matrix2D(double m00, double m01,
                     double m10, double m11) {
        super(2, 2);
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
    }

    @Override
    public double get(int row, int col) {
        switch(row){
            case 0 : switch(col){case 0:return m00;case 1:return m01;}
            case 1 : switch(col){case 0:return m10;case 1:return m11;}
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Matrix2D set(int row, int col, double value) {
        switch(row){
            case 0 : switch(col){case 0:m00=value;break;case 1:m01=value;break;} return this;
            case 1 : switch(col){case 0:m10=value;break;case 1:m11=value;break;} return this;
        }
        throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
    }

    @Override
    public Matrix2D set(final Matrix<?> toCopy){
        if (toCopy instanceof Matrix2D){
            Matrix2D o = (Matrix2D) toCopy;
            m00 = o.m00;m01 = o.m01;
            m10 = o.m10;m11 = o.m11;
            return this;
        }
        return super.set(toCopy);
    }

    public void setAll(double value) {
        m00 = value;
        m01 = value;
        m10 = value;
        m11 = value;
    }

    @Override
    public Matrix2D setToIdentity() {
        m00=1.0; m01=0.0;
        m10=0.0; m11=1.0;
        return this;
    }

    /**
     * Sets the elements to a rotation matrix of the given arithmetic angle.
     * Angle 0 is oriented toward positive <var>x</bar> axis,
     * rotation is counter-clockwise and the unit of measurement is radians.
     * The resulting matrix is not affine in the sense of {@link #isAffine()}.
     * The matrix is:
     *
     * <pre class="math">
     *        ┌                  ┐
     *        │ cos(θ)  −sin(θ)  │
     *        │ sin(θ)   cos(θ)  │
     *        └                  ┘</pre>
     *
     * @param θ  arithmetic rotation angle in radians.
     * @since 1.5
     */
    public Matrix2D setToRotation(double θ) {
        m00 =  (m11 = Math.cos(θ));
        m01 = -(m10 = Math.sin(θ));
        return this;
    }

    @Override
    public boolean isIdentity() {
        return m00==1.0 && m01==0.0
            && m10==0.0 && m11==1.0;
    }

    @Override
    public boolean isFinite() {
        return !(
                Double.isNaN(m00) || Double.isInfinite(m00) ||
                Double.isNaN(m01) || Double.isInfinite(m01) ||
                Double.isNaN(m10) || Double.isInfinite(m10) ||
                Double.isNaN(m11) || Double.isInfinite(m11)
                );
    }

    @Override
    public Matrix2D scale(double scale) {
        m00 *= scale;m01 *= scale;
        m10 *= scale;m11 *= scale;
        return this;
    }

    @Override
    public Matrix2D scale(double[] scale) {
        m00 *= scale[0];m01 *= scale[1];
        m10 *= scale[0];m11 *= scale[1];
        return this;
    }

    @Override
    public Matrix2D multiply(Matrix<?> other) {
        if (other instanceof Matrix2D){
            //usual case
            Matrix2D o = (Matrix2D) other;
            double b00 = this.m00 * o.m00 + this.m01 * o.m10;
            double b01 = this.m00 * o.m01 + this.m01 * o.m11;
            double b10 = this.m10 * o.m00 + this.m11 * o.m10;
            double b11 = this.m10 * o.m01 + this.m11 * o.m11;
            m00 = b00;m01 = b01;
            m10 = b10;m11 = b11;
            return this;
        } else {
            return (Matrix2D) super.multiply(other);
        }
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        double d0 = m00*source[sourceOffset] + m01*source[sourceOffset+1];
        double d1 = m10*source[sourceOffset] + m11*source[sourceOffset+1];
        dest[destOffset  ] = d0;
        dest[destOffset+1] = d1;
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        double d0 = m00*source[sourceOffset] + m01*source[sourceOffset+1];
        double d1 = m10*source[sourceOffset] + m11*source[sourceOffset+1];
        dest[destOffset  ] = (float) d0;
        dest[destOffset+1] = (float) d1;
    }

    @Override
    public Tuple<?> transform(Tuple<?> vector, Tuple<?> buffer) {
        if (buffer == null) buffer = new Vector2D.Double();

        if (vector instanceof Vector2D.Double && buffer instanceof Vector2D.Double) {
            final Vector2D.Double v = (Vector2D.Double) vector;
            final Vector2D.Double b = (Vector2D.Double) buffer;
            double rx = m00*v.x + m01*v.y ;
            double ry = m10*v.x + m11*v.y;
            b.x = rx;
            b.y = ry;
            return b;
        }

        return super.transform(vector, buffer);
    }

    @Override
    public Matrix2D copy() {
        return new Matrix2D(m00, m01, m10, m11);
    }

    @Override
    public MatrixSIS toMatrixSIS() {
        return new Matrix2(m00, m01, m10, m11);
    }

}