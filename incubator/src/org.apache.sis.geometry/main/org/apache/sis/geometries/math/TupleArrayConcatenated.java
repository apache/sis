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

import java.util.Arrays;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TupleArrayConcatenated extends AbstractTupleArray {

    private final TupleArray[] arrays;
    private final int[] offsets;
    private final int length;

    public TupleArrayConcatenated(TupleArray[] arrays) {
        this.arrays = arrays;
        this.offsets = new int[arrays.length];
        final DataType dataType = arrays[0].getDataType();
        final SampleSystem type = arrays[0].getSampleSystem();
        for (int i = 1; i < arrays.length; i++) {
            offsets[i] = offsets[i-1] + arrays[i-1].getLength();

            if (dataType != arrays[i].getDataType()) {
                throw new IllegalArgumentException("Arrays must have the same data type");
            }
            if (!type.equals(arrays[i].getSampleSystem())) {
                throw new IllegalArgumentException("Arrays must have the same crs");
            }
        }
        this.length = offsets[offsets.length - 1] + arrays[arrays.length - 1].getLength();
    }

    @Override
    public SampleSystem getSampleSystem() {
        return arrays[0].getSampleSystem();
    }

    @Override
    public void setSampleSystem(SampleSystem type) {
        for (TupleArray ta : arrays) {
            ta.setSampleSystem(type);
        }
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return arrays[0].getCoordinateReferenceSystem();
    }

    @Override
    public int getDimension() {
        return arrays[0].getDimension();
    }

    @Override
    public long getSampleCount() {
        return arrays[0].getSampleCount();
    }

    @Override
    public DataType getDataType() {
        return arrays[0].getDataType();
    }

    @Override
    public Tuple get(int index) {
        final int taidx = arrayIndex(index);
        return arrays[taidx].get(index - offsets[taidx]);
    }

    @Override
    public void get(int index, Tuple buffer) {
        final int taidx = arrayIndex(index);
        arrays[taidx].get(index - offsets[taidx], buffer);
    }

    @Override
    public void set(int index, Tuple buffer) {
        final int taidx = arrayIndex(index);
        arrays[taidx].set(index - offsets[taidx], buffer);
    }

    @Override
    public void transform(MathTransform trs) throws TransformException {
        for (TupleArray ta : arrays) {
            ta.transform(trs);
        }
    }

    @Override
    public void transform(CoordinateReferenceSystem crs) throws FactoryException, TransformException {
        for (TupleArray ta : arrays) {
            ta.transform(crs);
        }
    }

    @Override
    public TupleArray copy() {
        return resize(length);
    }

    @Override
    public TupleArrayCursor cursor() {
        return new Cursor();
    }

    private int arrayIndex(int tupleIndex) {
        ArgumentChecks.ensureBetween("Tuple index", 0, length - 1, tupleIndex);
        int p = Arrays.binarySearch(offsets, tupleIndex);
        if (p >= 0) {
            return p;
        } else {
            return -p - 2;
        }
    }

    private class Cursor implements TupleArrayCursor {

        private int coordinate = -1;
        private final TupleArrayCursor[] cursors = new TupleArrayCursor[arrays.length];

        @Override
        public Tuple samples() {
            final int taidx = arrayIndex(coordinate);
            if (cursors[taidx] == null) {
                cursors[taidx] = arrays[taidx].cursor();
            }
            cursors[taidx].moveTo(coordinate - offsets[taidx]);
            return cursors[taidx].samples();
        }

        @Override
        public int coordinate() {
            return coordinate;
        }

        @Override
        public void moveTo(int coordinate) {
            if (coordinate<0 || coordinate >= length) {
                throw new ArrayIndexOutOfBoundsException("Invalid coordinate " + coordinate + ", outside of data range [0," + length + "]. ");
            }
            this.coordinate = coordinate;
        }

        @Override
        public boolean next() {
            if (coordinate + 1 >= length) {
                return false;
            } else {
                this.coordinate++;
                return true;
            }
        }

    }

}
