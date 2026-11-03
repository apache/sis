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
package org.apache.sis.geometries.internal.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Cursor;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArgumentChecks;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ArrayDataPoints implements DataPoints, DataPointsType {

    private Array positions;
    private final Map<String,Array> attributes = new HashMap<>(1);

    public ArrayDataPoints(Array positions) {
        this(Collections.singletonMap(DataPointsType.ATT_POSITION, positions));
    }

    public ArrayDataPoints(Map<String, Array> attributes) {
        this.attributes.putAll(attributes);
        this.positions = attributes.get(DataPointsType.ATT_POSITION);
        ArgumentChecks.ensureNonNull("positions", this.positions);
        ArgumentChecks.ensureNonNull("positions crs", this.positions.getCoordinateReferenceSystem());
        final long size = this.positions.getLength();
        for (Array ta : this.attributes.values()) {
            if (ta.getLength() != size) {
                throw new IllegalArgumentException("All arrays must have the same length");
            }
        }
    }

    public ArrayDataPoints(DataPointsType attDef) {
        for (String name : attDef.getAttributeNames()) {
            attributes.put(name, NDArrays.of(attDef.getAttributeSystem(name), attDef.getAttributeType(name), 0));
        }
        positions = attributes.get(DataPointsType.ATT_POSITION);
        ArgumentChecks.ensureNonNull(DataPointsType.ATT_POSITION, positions);
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
        return Math.toIntExact(positions.getLength());
    }

    @Override
    public Vertex getPoint(int index) {
        return new Vertex(this, index);
    }

    @Override
    public Vector getPosition(int index) {
        return positions.get(index);
    }

    @Override
    public void setPosition(int index, Vector value) {
        positions.set(index, value);
    }

    public Array getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Replaces all attributes at once.
     * Used when all attributes are rebuilt with a new length, which {@link #setAttribute(String, Array)}
     * cannot do one attribute at a time.
     *
     * @param attributes the new attributes, which must contain the positions.
     * @throws IllegalArgumentException if positions are missing or the arrays have different lengths.
     */
    public void setAttributes(Map<String, Array> attributes) {
        final Array newPositions = attributes.get(DataPointsType.ATT_POSITION);
        ArgumentChecks.ensureNonNull("positions", newPositions);
        ArgumentChecks.ensureNonNull("positions crs", newPositions.getCoordinateReferenceSystem());
        final long size = newPositions.getLength();
        for (Map.Entry<String,Array> entry : attributes.entrySet()) {
            if (entry.getValue().getLength() != size) {
                throw new IllegalArgumentException("Array must have the same length as the positions : " + entry.getKey());
            }
        }
        this.attributes.clear();
        this.attributes.putAll(attributes);
        this.positions = newPositions;
    }

    /**
     * Sets or removes an attribute.
     *
     * <p>Once positions are defined, all attributes must have the same length as the positions.
     * While positions are empty, attributes of any length are accepted, so that the attributes
     * of new data points can be set in any order. Setting positions of a different length then
     * requires all non-empty attributes to have that length, the empty ones are removed.</p>
     *
     * @param name attribute name
     * @param array attribute values, or {@code null} to remove the attribute
     * @throws IllegalArgumentException if the array length does not match the other attributes.
     */
    public void setAttribute(String name, Array array) {
        if (array == null) {
            if (DataPointsType.ATT_POSITION.equals(name)) {
                throw new IllegalArgumentException("Positions attribute cannot be removed");
            }
            attributes.remove(name);
        } else if (DataPointsType.ATT_POSITION.equals(name)) {
            final long length = array.getLength();
            if (length != positions.getLength()) {
                for (Map.Entry<String,Array> entry : attributes.entrySet()) {
                    final long l = entry.getValue().getLength();
                    if (l != 0 && l != length && !DataPointsType.ATT_POSITION.equals(entry.getKey())) {
                        throw new IllegalArgumentException("Array must have the same length as attribute " + entry.getKey());
                    }
                }
                //empty attributes do not match the new positions anymore
                attributes.values().removeIf((Array a) -> a.getLength() == 0);
            }
            this.positions = array;
            attributes.put(name, array);
        } else if (array.getLength() == size() || size() == 0) {
            attributes.put(name, array);
        } else {
            throw new IllegalArgumentException("Array must have the same length");
        }
    }

    @Override
    public Vector getAttribute(int index, String name) {
        final Array array = attributes.get(name);
        return (array != null) ? array.get(index) : null;
    }

    @Override
    public void setAttribute(int index, String name, Vector value) {
        attributes.get(name).set(index, value);
    }

    @Override
    public DataPointsType getType() {
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
    public Array getAttributeArray(String name) {
        return attributes.get(name).copy();
    }

    @Override
    public NDRange getAttributeRange(String name) {
        return NDArrays.computeRange(attributes.get(name));
    }

    /**
     * Returns a copy of the points at the given indices.
     * This implementation copies a {@linkplain NDArrays#subset(Array, int...) view} of each attribute array,
     * without creating a vector for each point.
     */
    @Override
    public DataPoints subset(final int[] indices) {
        final int size = size();
        for (final int index : indices) {
            Objects.checkIndex(index, size);
        }
        final Map<String,Array> subset = new LinkedHashMap<>();
        attributes.forEach((name, array) -> subset.put(name, NDArrays.subset(array, indices).copy()));
        return new ArrayDataPoints(subset);
    }

    /**
     * Returns a copy of the given range of points.
     * This implementation copies the range of each attribute array at once.
     */
    @Override
    public DataPoints subset(final int start, final int length) {
        Objects.checkFromIndexSize(start, length, size());
        final Map<String,Array> subset = new LinkedHashMap<>();
        for (final Map.Entry<String,Array> entry : attributes.entrySet()) {
            final Array source = entry.getValue();
            final Array target = NDArrays.of(source.getSampleSystem(), source.getDataType(), length);
            target.set(0, source, start, length);
            subset.put(entry.getKey(), target);
        }
        return new ArrayDataPoints(subset);
    }

    @Override
    public int hashCode() {
        return DataPoints.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return DataPoints.equals(this, obj);
    }

}
