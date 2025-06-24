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

import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Unmodifiable tuple array.
 *
 * @author Johann Sorel (Geomatys)
 */
final class TupleArrayUnmodifiable extends AbstractTupleArray {

    private final TupleArray parent;

    public TupleArrayUnmodifiable(TupleArray parent) {
        ArgumentChecks.ensureNonNull("parent", parent);
        this.parent = parent;
    }

    @Override
    public SampleSystem getSampleSystem() {
        return parent.getSampleSystem();
    }

    @Override
    public void setSampleSystem(SampleSystem type) {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public boolean isEmpty() {
        return parent.isEmpty();
    }

    @Override
    public int getLength() {
        return parent.getLength();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return parent.getCoordinateReferenceSystem();
    }

    @Override
    public int getDimension() {
        return parent.getDimension();
    }

    @Override
    public long getSampleCount() {
        return parent.getSampleCount();
    }

    @Override
    public DataType getDataType() {
        return parent.getDataType();
    }

    @Override
    public Tuple get(int index) {
        return parent.get(index);
    }

    @Override
    public void get(int index, Tuple buffer) {
        parent.get(index, buffer);
    }

    @Override
    public void set(int index, Tuple buffer) {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public void set(int index, TupleArray array, int offset, int nb) {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
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
    public byte[] toArrayByte(int offset, int nbTuple) {
        return parent.toArrayByte(offset, nbTuple);
    }

    @Override
    public short[] toArrayShort(int offset, int nbTuple) {
        return parent.toArrayShort(offset, nbTuple);
    }

    @Override
    public int[] toArrayInt(int offset, int nbTuple) {
        return parent.toArrayInt(offset, nbTuple);
    }

    @Override
    public float[] toArrayFloat(int offset, int nbTuple) {
        return parent.toArrayFloat(offset, nbTuple);
    }

    @Override
    public double[] toArrayDouble(int offset, int nbTuple) {
        return parent.toArrayDouble(offset, nbTuple);
    }

    @Override
    public void transform(MathTransform trs) throws TransformException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public void transform(CoordinateReferenceSystem crs) throws FactoryException, TransformException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public TupleArray resize(int newSize) {
        return parent.resize(newSize);
    }

    @Override
    public TupleArray copy() {
        return parent.copy();
    }

    @Override
    public TupleArrayCursor cursor() {
        return new TupleArrayCursorUnmodifiable(parent.cursor());
    }


}
