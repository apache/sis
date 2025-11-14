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
package org.apache.sis.geometries;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometries.math.Array;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface PointSequence {

    /**
     * Get geometry coordinate system.
     * @return never null
     */
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Set coordinate system in which the coordinates are declared.
     * This method does not transform the coordinates.
     *
     * @param cs , not null
     * @Throws IllegalArgumentException if coordinate system is not compatible with geometrie.
     */
    void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException;

    /**
     * Get the geometry number of dimensions.
     * This is the same as coordinate system dimension.
     *
     * @return number of dimension
     */
    default int getDimension() {
        return getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Get geometry attributes type.
     * @return attributes type, never null
     */
    AttributesType getAttributesType();

    /**
     * Get the geometry bounding envelope.
     *
     * @return Envelope in geometry coordinate reference system.
     */
    default Envelope getEnvelope() {
        return getAttributeRange(AttributesType.ATT_POSITION);
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    int size();

    Point getPoint(int index);

    /**
     * Get position attribute value.
     *
     * @param index searched index
     * @return copy of the position
     */
    Tuple getPosition(int index);

    /**
     * Set position attribute value.
     *
     * @param index searched index
     * @param value new attribute value
     */
    void setPosition(int index, Tuple value);

    /**
     * Get attribute value.
     *
     * @param index searched index
     * @param name attribute name
     * @return copy of the attribute
     */
    Tuple getAttribute(int index, String name);

    /**
     * Set attribute value.
     *
     * @param index searched index
     * @param name attribute name
     * @param value new attribute value
     */
    void setAttribute(int index, String name, Tuple value);

    /**
     * Get all attribute values as a TupleArray.
     *
     * @param name attribute name
     * @return copy of all attribute values
     */
    default Array getAttributeArray(String name) {
        final AttributesType at = getAttributesType();
        final SampleSystem ss = at.getAttributeSystem(name);
        final DataType type = at.getAttributeType(name);
        final int size = size();
        final Array ta = NDArrays.of(ss, type, size);
        for (int i = 0; i < size; i++) {
            ta.set(i, getAttribute(i, name));
        }
        return ta;
    }

    /**
     * Get attribute values range
     *
     * @return BBox in attribute sample system
     */
    default BBox getAttributeRange(String name) {
        if (isEmpty()) {
            return null;
        }
        final Tuple<?> start = getAttribute(0, name);
        final BBox env = new BBox(start, start);
        for (int i = 1, n = size(); i < n; i++) {
            env.add(getAttribute(i, name));
        }
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        return env;
    }

}
