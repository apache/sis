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
package org.apache.sis.geometries.privy;

import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrayCursor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ArraySequence implements PointSequence, AttributesType {

    private TupleArray positions;
    private final Map<String,TupleArray> attributes = new HashMap<>(1);

    public ArraySequence(TupleArray positions) {
        this(Collections.singletonMap(AttributesType.ATT_POSITION, positions));
    }

    public ArraySequence(Map<String, TupleArray> attributes) {
        this.attributes.putAll(attributes);
        this.positions = attributes.get(AttributesType.ATT_POSITION);
        ArgumentChecks.ensureNonNull("positions", this.positions);
        ArgumentChecks.ensureNonNull("positions crs", this.positions.getCoordinateReferenceSystem());
        final int size = this.positions.getLength();
        for (TupleArray ta : this.attributes.values()) {
            if (ta.getLength() != size) {
                throw new IllegalArgumentException("All arrays must have the same length");
            }
        }
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return positions.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        positions.setSampleSystem(SampleSystem.of(cs));
    }

    @Override
    public int getDimension() {
        return positions.getDimension();
    }

    @Override
    public boolean isEmpty() {
        return positions.isEmpty();
    }

    @Override
    public int size() {
        return positions.getLength();
    }

    @Override
    public Point getPoint(int index) {
        return new Indexed(this, index);
    }

    @Override
    public Tuple getPosition(int index) {
        return positions.get(index);
    }

    @Override
    public void setPosition(int index, Tuple value) {
        positions.set(index, value);
    }

    public TupleArray getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, TupleArray array) {
        if (array == null) {
            if (AttributesType.ATT_POSITION.equals(name)) {
                throw new IllegalArgumentException("Positions attribute can not be removed");
            }
            attributes.remove(name);
        } else if (array.getLength() != size()) {
            throw new IllegalArgumentException("Array must have the same length");
        } else if (AttributesType.ATT_POSITION.equals(name)) {
            this.positions = array;
            attributes.put(name, array);
        } else {
            attributes.put(name, array);
        }
    }

    @Override
    public Tuple getAttribute(int index, String name) {
        return attributes.get(name).get(index);
    }

    @Override
    public void setAttribute(int index, String name, Tuple value) {
        attributes.get(name).set(index, value);
    }

    @Override
    public AttributesType getAttributesType() {
        return this;
    }

    @Override
    public SampleSystem getAttributeSystem(String name) {
        return attributes.get(name).getSampleSystem();
    }

    @Override
    public DataType getAttributeType(String name) {
        return attributes.get(name).getDataType();
    }

    @Override
    public List<String> getAttributeNames() {
        return new ArrayList<>(attributes.keySet());
    }

    @Override
    public TupleArray getAttributeArray(String name) {
        return attributes.get(name).copy();
    }

    @Override
    public BBox getAttributeRange(String name) {
        return TupleArrays.computeRange(attributes.get(name));
    }

    /**
     * An indexed point in the point sequence
     */
    public static class Indexed implements Point {

        private final ArraySequence parent;
        private final int index;

        public Indexed(ArraySequence geometry, int index) {
            this.parent = geometry;
            this.index = index;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Tuple getPosition() {
            final TupleArrayCursor cursor = parent.positions.cursor();
            cursor.moveTo(index);
            return cursor.samples();
        }

        /**
         * @return index in the parent mesh.
         */
        public int getIndex() {
            return index;
        }

        @Override
        public Tuple getAttribute(String key) {
            final TupleArray tupleGrid = parent.attributes.get(key);
            if (tupleGrid == null) return null;
            final TupleArrayCursor cursor = tupleGrid.cursor();
            cursor.moveTo(index);
            return cursor.samples();
        }

        @Override
        public void setAttribute(String name, Tuple tuple) {
            final TupleArray tupleGrid = parent.attributes.get(name);
            if (tupleGrid == null) throw new IllegalArgumentException("Attribute " + name + " do not exist");
            final TupleArrayCursor cursor = tupleGrid.cursor();
            cursor.moveTo(index);
            cursor.samples().set(tuple);
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return parent.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public AttributesType getAttributesType() {
            return parent.getAttributesType();
        }
    }
}
