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
public final class AffineND extends AbstractAffine<AffineND> {

    protected final double[][] values;
    private final int dim;

    public static Affine<?> create(int dimension) {
        switch (dimension) {
            case 1 : return new Affine1D();
            case 2 : return new Affine2D();
            case 3 : return new Affine3D();
            case 4 : return new Affine4D();
            default : return new AffineND(dimension);
        }
    }

    public static Affine<?> create(Affine<?> toCopy) {
        final Affine<?> cp = create(toCopy.getInputDimensions());
        cp.set(toCopy);
        return cp;
    }

    public AffineND(int dimension) {
        super(dimension);
        this.dim = dimension;
        this.values = new double[dimension][dimension];
    }

    @Override
    public Matrix toMatrix() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AffineND setFromMatrix(Matrix m) {
        for (int y = 0; y < dim; y++) {
            for (int x = 0; x <= dim; x++) {
                values[y][x] = m.get(y, x);
            }
        }
        return this;
    }

    @Override
    public double get(int row, int col) {
        return values[row][col];
    }

    @Override
    public AffineND set(int row, int col, double value) {
        values[row][col] = value;
        return this;
    }

    @Override
    protected void transform1(double[] source, int sourceOffset, double[] dest, int destOffset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void transform1(float[] source, int sourceOffset, float[] dest, int destOffset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix toMatrix(Matrix buffer) {
        if (buffer == null) buffer = MatrixND.create(dim+1, +1);
        for (int y=0;y<dim;y++) {
            for (int x=0;x<=dim;x++) {
                buffer.set(y, x, values[y][x]);
            }
        }
        return buffer;
    }

    @Override
    public AffineND copy() {
        final AffineND aff = new AffineND(dim);
        aff.set(this);
        return aff;
    }
}
