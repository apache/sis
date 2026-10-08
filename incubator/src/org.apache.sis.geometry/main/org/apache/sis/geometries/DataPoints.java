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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;


/**
 * Enriched version of ISO-19107 DataPoints with multiple attributes capabilities.
 *
 * <p>
 * ISO-19107 defines DataPoints on Curves as a List of DirectPosition.
 * We extent this list with additional property support.
 * By doing this we can store the old 'M' value as a properly separated information
 * but also all 'normal','color','tangent','wight'.... informations which exist
 * in other specifications.
 * </p>
 *
 * <p>
 * The ISO-19107 DirectPosition becomes the POSITION attribute.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 */
public interface DataPoints {

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
    DataPointsType getType();

    /**
     * Get the geometry bounding envelope.
     *
     * @return Envelope in geometry coordinate reference system.
     */
    default Envelope getEnvelope() {
        return getAttributeRange(DataPointsType.ATT_POSITION);
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
    Vector<?> getPosition(int index);

    /**
     * Set position attribute value.
     *
     * @param index searched index
     * @param value new attribute value
     */
    void setPosition(int index, Vector<?> value);

    /**
     * Get attribute value.
     *
     * @param index searched index
     * @param name attribute name
     * @return copy of the attribute
     */
    Vector<?> getAttribute(int index, String name);

    /**
     * Set attribute value.
     *
     * @param index searched index
     * @param name attribute name
     * @param value new attribute value
     */
    void setAttribute(int index, String name, Vector<?> value);

    /**
     * Get all attribute values as an Array.
     *
     * @param name attribute name
     * @return copy of all attribute values
     */
    default Array getAttributeArray(String name) {
        final DataPointsType at = getType();
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
     * @param name
     * @return BBox in attribute sample system
     */
    default BBox getAttributeRange(String name) {
        if (isEmpty()) {
            return null;
        }
        final Vector<?> start = getAttribute(0, name);
        final BBox env = new BBox(start, start);
        for (int i = 1, n = size(); i < n; i++) {
            env.add(getAttribute(i, name));
        }
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        return env;
    }

    /**
     * Returns a hash code value for the given sequence, computed from the attributes it declares
     * and from the values it holds for them. Implementations of this interface shall base their
     * {@code hashCode()} on this method, so that two sequences holding the same data have the
     * same hash code whatever the way they store it.
     *
     * @param  points  the sequence to hash, not null.
     * @return a hash code value for the given sequence.
     */
    static int hashCode(final DataPoints points) {
        final DataPointsType type = points.getType();
        final int size = points.size();
        int hash = DataPointsType.hashCode(type) + 31 * size;
        for (final String name : type.getAttributeNames()) {
            int attribute = name.hashCode();
            for (int i = 0; i < size; i++) {
                attribute = 31 * attribute + Objects.hashCode(points.getAttribute(i, name));
            }
            // Summed so that the result does not depend on the order in which the names are returned.
            hash += attribute;
        }
        return hash;
    }

    /**
     * Returns whether the given object is a sequence declaring the same attributes as the given one
     * and holding the same values for them. Implementations of this interface shall base their
     * {@code equals(Object)} on this method, so that two sequences holding the same data are equal
     * whatever the way they store it: a sequence backed by arrays is equal to a sequence backed by
     * a list of points when both describe the same positions.
     *
     * <p>The attributes are compared one by one rather than by delegating to
     * {@link DataPointsType#equals(DataPointsType, Object)}, because a sequence may be its own
     * description, and that method reports two descriptions as different as soon as one of them
     * also carries the positions.</p>
     *
     * @param  points  the sequence to compare, not null.
     * @param  obj     the object to compare to the given sequence, or {@code null}.
     * @return whether the two hold the same attributes with the same values.
     */
    static boolean equals(final DataPoints points, final Object obj) {
        if (points == obj) {
            return true;
        }
        if (!(obj instanceof DataPoints other)) {
            return false;
        }
        final int size = points.size();
        if (size != other.size()) {
            return false;
        }
        final DataPointsType type = points.getType();
        final DataPointsType otherType = other.getType();
        final List<String> names = type.getAttributeNames();
        final List<String> others = otherType.getAttributeNames();
        if (names.size() != others.size() || !new HashSet<>(names).containsAll(others)) {
            return false;
        }
        for (final String name : names) {
            if (!Objects.equals(type.getAttributeSystem(name), otherType.getAttributeSystem(name)) ||
                !Objects.equals(type.getAttributeType  (name), otherType.getAttributeType  (name)))
            {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (!Objects.equals(points.getAttribute(i, name), other.getAttribute(i, name))) {
                    return false;
                }
            }
        }
        return true;
    }
}
