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

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;


/**
 * Unmodifiable tuple.
 *
 * @author Johann Sorel (Geomatys)
 */
final class TupleUnmodifiable extends AbstractTuple {

    private final Tuple parent;

    public TupleUnmodifiable(Tuple parent) {
        super(parent.getSampleSystem());
        ArgumentChecks.ensureNonNull("parent", parent);
        this.parent = parent;
    }

    @Override
    public SampleSystem getSampleSystem() {
        return parent.getSampleSystem();
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
    public void set(int indice, double value) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple set(DirectPosition values) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple set(double[] values) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple set(double[] values, int offset) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple set(float[] values) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple set(float[] values, int offset) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple set(Tuple values) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("This implementation is unmodifiable");
    }

    @Override
    public Tuple setAll(double value) {
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
    public Tuple copy() {
        return parent.copy();
    }

    @Override
    public boolean isFinite() {
        return parent.isFinite();
    }

    @Override
    public boolean isAll(double value) {
        return parent.isAll(value);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return parent.getCoordinateReferenceSystem();
    }

}
