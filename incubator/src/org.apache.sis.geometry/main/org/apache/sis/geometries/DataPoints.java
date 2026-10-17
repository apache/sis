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
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


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
    void setPosition(int index, ReadOnly.Vector<?> value);

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
    void setAttribute(int index, String name, ReadOnly.Vector<?> value);

    /**
     * Returns a new point between the two given points of this {@code DataPoints}, for example on a segment.
     * This is a shortcut for {@link #getInterpolatedPoint(int[], double[])} with the weights
     * 1 − {@code ratio} and {@code ratio}.
     *
     * @param  i0     index of the point at ratio 0, the start of the segment.
     * @param  i1     index of the point at ratio 1, the end of the segment.
     * @param  ratio  position between the two points, from 0 to 1.
     * @return a new point holding the computed attributes, independent of this {@code DataPoints}.
     */
    default Point getInterpolatedPoint(final int i0, final int i1, final double ratio) {
        return getInterpolatedPoint(new int[] {i0, i1}, new double[] {1 - ratio, ratio});
    }

    /**
     * Returns a new point interpolated between any number of points of this {@code DataPoints}, for example on
     * a segment or inside a triangle. Each attribute of the new point, the position included, is
     * computed once by {@link #interpolateAttribute(String, int[], double[])}. The returned point is
     * independent of this {@code DataPoints}: changing either of them does not change the other one.
     *
     * @param  indices  indices of the points to interpolate between, at least one.
     * @param  weights  weight of each point, in the same order.
     * @return a new point holding the computed attributes, independent of this {@code DataPoints}.
     * @throws IllegalArgumentException if there is no point, or not one weight per point.
     *
     * @see #viewInterpolatedPoint(int[], double[])
     */
    default Point getInterpolatedPoint(final int[] indices, final double[] weights) {
        final DataPointsType type = getType();
        final Point point = GeometryFactory.DEFAULT.createPoint(type);
        for (final String name : type.getAttributeNames()) {
            point.setAttribute(name, interpolateAttribute(name, indices, weights));
        }
        return point;
    }

    /**
     * Returns a read-only view of the point interpolated between any number of points of this
     * {@code DataPoints}. The attributes of the returned point are not stored: each of them is
     * computed by {@link #interpolateAttribute(String, int[], double[])} every time it is requested,
     * so the view follows the changes of the points of this {@code DataPoints}.
     * Attempts to modify the returned point throw {@link UnsupportedOperationException}.
     *
     * @param  indices  indices of the points to interpolate between, at least one.
     * @param  weights  weight of each point, in the same order.
     * @return a read-only view of the interpolated point.
     * @throws IllegalArgumentException if there is no point, or not one weight per point.
     *
     * @see #getInterpolatedPoint(int[], double[])
     */
    default Point viewInterpolatedPoint(final int[] indices, final double[] weights) {
        return new InterpolatedPoint(this, indices, weights);
    }

    /**
     * Returns the value of an attribute between the two given points of this {@code DataPoints}, for example on
     * a segment. This is a shortcut for {@link #interpolateAttribute(String, int[], double[])} with the
     * weights 1 − {@code ratio} and {@code ratio}.
     *
     * @param  name   name of the attribute.
     * @param  i0     index of the point at ratio 0, the start of the segment.
     * @param  i1     index of the point at ratio 1, the end of the segment.
     * @param  ratio  position between the two points, from 0 to 1.
     * @return the attribute value, independent of this {@code DataPoints}.
     */
    default Vector<?> interpolateAttribute(final String name, final int i0, final int i1, final double ratio) {
        return interpolateAttribute(name, new int[] {i0, i1}, new double[] {1 - ratio, ratio});
    }

    /**
     * Returns the value of an attribute interpolated between any number of points of this {@code DataPoints},
     * for example on a segment, inside a triangle or inside a quadrilateral. The weights are the
     * generalized barycentric coordinates of the position where the value is computed: they are
     * positive and sum to 1. The value is computed as declared by the
     * {@linkplain DataPointsType#getAttributeInterpolation(String) attribute interpolation}:
     *
     * <ul>
     *   <li>{@link AttributeInterpolation#LINEAR LINEAR} and {@link AttributeInterpolation#TRANSLATE TRANSLATE}:
     *       the weighted sum of the values.</li>
     *   <li>{@link AttributeInterpolation#LINEAR_NORMALIZED LINEAR_NORMALIZED}: the weighted sum of the values,
     *       normalized to a unit vector.</li>
     *   <li>{@link AttributeInterpolation#NEAREST NEAREST}: the value of the point of largest weight,
     *       the first one on ties.</li>
     *   <li>{@link AttributeInterpolation#LEADING LEADING}: the value of the first point, which starts
     *       the segment or the surface. Between two points, which is a segment, the end point keeps
     *       its own value.</li>
     * </ul>
     *
     * @param  name     name of the attribute.
     * @param  indices  indices of the points to interpolate between, at least one.
     * @param  weights  weight of each point, in the same order.
     * @return the attribute value, independent of this {@code DataPoints}.
     * @throws IllegalArgumentException if there is no point, or not one weight per point.
     */
    default Vector<?> interpolateAttribute(final String name, final int[] indices, final double[] weights) {
        if (indices.length == 0 || weights.length != indices.length) {
            throw new IllegalArgumentException("Expected at least one point with one weight each, but got "
                    + indices.length + " points and " + weights.length + " weights.");
        }
        final AttributeInterpolation method = getType().getAttributeInterpolation(name);
        switch (method) {
            case LINEAR:
            case LINEAR_NORMALIZED:
            case TRANSLATE: {
                final Vector<?> value = getAttribute(indices[0], name).scale(weights[0]);
                for (int i = 1; i < indices.length; i++) {
                    value.add(getAttribute(indices[i], name).scale(weights[i]));
                }
                if (method == AttributeInterpolation.LINEAR_NORMALIZED) {
                    // TODO: a glTF tangent has 4 components: the direction (x,y,z) and the handedness
                    // (w = ±1) of the bitangent. Only the direction should be normalized, while the
                    // handedness should be kept, for example taken from the nearest point.
                    value.normalize();
                }
                return value;
            }
            case LEADING: {
                return getAttribute(indices[(indices.length == 2 && weights[1] >= 1) ? 1 : 0], name);
            }
            default: {
                int nearest = 0;
                for (int i = 1; i < weights.length; i++) {
                    if (weights[i] > weights[nearest]) nearest = i;
                }
                return getAttribute(indices[nearest], name);
            }
        }
    }

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
     * @return envelope in the attribute sample system and data type, or {@code null} if this {@code DataPoints} is empty.
     */
    default NDRange getAttributeRange(String name) {
        if (isEmpty()) {
            return null;
        }
        final Vector<?> start = Vectors.create(getType().getAttributeSystem(name), getType().getAttributeType(name))
                                       .set(getAttribute(0, name));
        final NDRange env = new NDRange(start, start);
        for (int i = 1, n = size(); i < n; i++) {
            env.add(getAttribute(i, name));
        }
        return env;
    }

    /**
     * Returns a hash code value for the given {@code DataPoints}, computed from the attributes it declares
     * and from the values it holds for them. Implementations of this interface shall base their
     * {@code hashCode()} on this method, so that two {@code DataPoints} holding the same data have the
     * same hash code whatever the way they store it.
     *
     * @param  points  the {@code DataPoints} to hash, not null.
     * @return a hash code value for the given {@code DataPoints}.
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
     * Returns whether the given object is a {@code DataPoints} declaring the same attributes as the given one
     * and holding the same values for them. Implementations of this interface shall base their
     * {@code equals(Object)} on this method, so that two {@code DataPoints} holding the same data are equal
     * whatever the way they store it: a {@code DataPoints} backed by arrays is equal to one backed by
     * a list of points when both describe the same positions.
     *
     * <p>The attributes are compared one by one rather than by delegating to
     * {@link DataPointsType#equals(DataPointsType, Object)}, because a {@code DataPoints} may be its own
     * description, and that method reports two descriptions as different as soon as one of them
     * also carries the positions.</p>
     *
     * @param  points  the {@code DataPoints} to compare, not null.
     * @param  obj     the object to compare to the given {@code DataPoints}, or {@code null}.
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

    /**
     * A read-only view of a point interpolated between points of a {@code DataPoints}.
     * The attributes are not stored but computed every time they are requested.
     *
     * @see DataPoints#viewInterpolatedPoint(int[], double[])
     */
    final class InterpolatedPoint implements Point {
        /**
         * The points between which this point is interpolated.
         */
        private final DataPoints points;

        /**
         * Indices of the points to interpolate between.
         */
        private final int[] indices;

        /**
         * Weight of each point, in the same order as the indices.
         */
        private final double[] weights;

        /**
         * Creates a view of the point interpolated between the given points.
         *
         * @throws IllegalArgumentException if there is no point, or not one weight per point.
         */
        private InterpolatedPoint(final DataPoints points, final int[] indices, final double[] weights) {
            if (indices.length == 0 || weights.length != indices.length) {
                throw new IllegalArgumentException("Expected at least one point with one weight each, but got "
                        + indices.length + " points and " + weights.length + " weights.");
            }
            this.points  = points;
            this.indices = indices.clone();
            this.weights = weights.clone();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Vector<?> getPosition() {
            return points.interpolateAttribute(DataPointsType.ATT_POSITION, indices, weights);
        }

        /**
         * Returns the attribute of the given name, interpolated between the points of the viewed
         * {@code DataPoints}, or {@code null} if that {@code DataPoints} has no such attribute.
         */
        @Override
        public Vector<?> getAttribute(final String name) {
            if (!points.getType().getAttributeNames().contains(name)) {
                return null;
            }
            return points.interpolateAttribute(name, indices, weights);
        }

        /**
         * Throws {@link UnsupportedOperationException}: this point is a read-only view.
         */
        @Override
        public void setAttribute(final String name, final ReadOnly.Vector<?> tuple) {
            throw new UnsupportedOperationException("An interpolated point view is read-only.");
        }

        @Override
        public DataPointsType getDataPointsType() {
            return points.getType();
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return points.getCoordinateReferenceSystem();
        }

        /**
         * Throws {@link UnsupportedOperationException}: this point is a read-only view.
         */
        @Override
        public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
            throw new UnsupportedOperationException("An interpolated point view is read-only.");
        }
    }
}
