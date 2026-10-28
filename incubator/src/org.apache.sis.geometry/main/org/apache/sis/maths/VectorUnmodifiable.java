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
package org.apache.sis.maths;

import org.apache.sis.util.ArgumentChecks;


/**
 * Unmodifiable tuple.
 *
 * @author Johann Sorel (Geomatys)
 */
final class VectorUnmodifiable extends AbstractVector<VectorUnmodifiable> {

    private final Vector<?> parent;

    public VectorUnmodifiable(Vector<?> parent) {
        ArgumentChecks.ensureNonNull("parent", parent);
        this.parent = parent;
    }

    @Override
    public int getDimension() {
        return parent.getDimension();
    }

    @Override
    public DataType getDataType() {
        return parent.getDataType();
    }

    @Override
    public double get(int indice) throws IndexOutOfBoundsException {
        return parent.get(indice);
    }

    @Override
    public byte[] toArrayByte() {
        return parent.toArrayByte();
    }

    @Override
    public short[] toArrayShort() {
        return parent.toArrayShort();
    }

    @Override
    public int[] toArrayInt() {
        return parent.toArrayInt();
    }

    @Override
    public float[] toArrayFloat() {
        return parent.toArrayFloat();
    }

    @Override
    public double[] toArrayDouble() {
        return parent.toArrayDouble();
    }

    @Override
    public void toArrayByte(byte[] buffer, int offset) {
        parent.toArrayByte(buffer, offset);
    }

    @Override
    public void toArrayShort(short[] buffer, int offset) {
        parent.toArrayShort(buffer, offset);
    }

    @Override
    public void toArrayInt(int[] buffer, int offset) {
        parent.toArrayInt(buffer, offset);
    }

    @Override
    public void toArrayFloat(float[] buffer, int offset) {
        parent.toArrayFloat(buffer, offset);
    }

    @Override
    public void toArrayDouble(double[] buffer, int offset) {
        parent.toArrayDouble(buffer, offset);
    }

    @Override
    public VectorUnmodifiable copy() {
        return new VectorUnmodifiable(parent.copy());
    }

    @Override
    public boolean isFinite() {
        return parent.isFinite();
    }

    @Override
    public boolean isNaN() {
        return parent.isNaN();
    }

    @Override
    public boolean isInfinite() {
        return parent.isInfinite();
    }

    @Override
    public boolean isAll(double value) {
        return parent.isAll(value);
    }

    @Override
    public double length() {
        return parent.length();
    }

    @Override
    public double lengthSquare() {
        return parent.lengthSquare();
    }

    @Override
    public double dot(Vector<?> other) {
        return parent.dot(other);
    }

    @Override
    public double dotAccurate(Vector<?> other) {
        return parent.dotAccurate(other);
    }

}
