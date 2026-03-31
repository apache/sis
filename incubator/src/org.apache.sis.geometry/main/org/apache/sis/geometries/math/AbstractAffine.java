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
abstract class AbstractAffine<T extends AbstractAffine<T>> extends SimplifiedTransform implements Affine<T> {

    public AbstractAffine(int size) {
        super(size);
    }

    @Override
    public double[] getCol(int col) {
        final double[] array = new double[getInputDimensions()];
        for (int i = 0; i < array.length; i++) array[i] = get(i,col);
        return array;
    }

    @Override
    public double[] getRow(int row) {
        final double[] array = new double[getInputDimensions()+1];
        for (int i = 0;i < array.length; i++) array[i] = get(row, i);
        return array;
    }

    @Override
    public boolean isIdentity() {
        final int size = getInputDimensions();

        for (int x = 0;x <= size; x++) {
            for (int y = 0;y < size; y++) {
                if ( x == y ){
                    if (get(y,x) != 1.0) return false;
                } else {
                    if (get(y,x) != 0.0) return false;
                }
            }
        }
        return true;
    }

    @Override
    public T invert() {
        setFromMatrix(toMatrix().invert());
        return (T) this;
    }

    @Override
    public T createInverse() {
        return copy().invert();
    }

    @Override
    public T set(final ReadOnly.Affine<?> toCopy) {
        final int dim = toCopy.getInputDimensions();
        for (int x = 0; x <= dim; x++) {
            for (int y = 0; y < dim; y++) {
                set(y, x, toCopy.get(y, x));
            }
        }
        return (T) this;
    }

    @Override
    public T setRow(int row, double[] values) {
        if (values.length != getInputDimensions()+1) throw new IllegalArgumentException("Unvalid array size");
        for (int x = 0; x < values.length; x++) {
            set(row, x, values[x]);
        }
        return (T) this;
    }

    @Override
    public T setCol(int col, double[] values) {
        if (values.length != getInputDimensions()) throw new IllegalArgumentException("Unvalid array size");
        for (int y = 0; y < values.length; y++) {
            set(y, col, values[y]);
        }
        return (T) this;
    }

    /**
     * Set affine values to identity transform.
     *
     * @return this affine
     */
    @Override
    public T setToIdentity() {
        final int dim = getInputDimensions();
        for(int x = 0; x <= dim; x++) {
            for(int y = 0; y < dim; y++) {
                set(y, x, x==y ? 1:0);
            }
        }
        return (T) this;
    }

    @Override
    public T scale(double scale) {
        setFromMatrix(this.toMatrix().scale(scale));
        return (T) this;
    }

    @Override
    public T scale(double[] tuple) {
        setFromMatrix(this.toMatrix().scale(tuple));
        return (T) this;
    }

    @Override
    public T multiply(ReadOnly.Affine<?> affine) {
        setFromMatrix(this.toMatrix().multiply(affine.toMatrix()));
        return (T) this;
    }

    @Override
    public int hashCode() {
        return getInputDimensions();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Affine)) return false;

        final ReadOnly.Affine<?> aff = (ReadOnly.Affine) obj;
        final int dim = aff.getInputDimensions();
        if (aff.getInputDimensions() != getInputDimensions()) {
            return false;
        }

        for(int x = 0; x <= dim; x++) {
            for(int y = 0; y < dim; y++) {
                if (get(y, x) != aff.get(y, x)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void chechRowCol(int row, int col) {
        if (row < 0 || row > input || col < 0 || col > input+1) {
            throw new IllegalArgumentException("Invalid row/col index "+row+":"+col);
        }
    }
}
