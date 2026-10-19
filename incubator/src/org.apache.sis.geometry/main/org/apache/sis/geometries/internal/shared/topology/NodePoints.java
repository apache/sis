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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.internal.shared.IndexedPoint;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * The nodes of an {@link IndexedComplex} as data points: one point per node index, with the positions
 * and all the other attributes of the geometries from which the nodes have been built.
 *
 * <p>The positions are stored as doubles, since the topology is computed on them. Other attributes keep the
 * sample system and the data type of the first geometry providing them. A node made of several positions,
 * of the same geometry or of different geometries, takes the value of each attribute from the first of
 * those positions having that attribute. A node without value for an attribute, for example a node added
 * by {@link ComplexEditor} at given coordinates, has zero values for that attribute.</p>
 *
 * <p>The positions are changed only by {@link ComplexEditor}, since they define the topology.
 * The other attributes can be changed freely. The points of removed nodes are kept,
 * so that the index of a point is always the index of its node.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class NodePoints implements DataPoints, DataPointsType {

    /**
     * Number of points for which space is allocated at creation time.
     */
    private static final int INITIAL_CAPACITY = 16;

    /**
     * The reference system of the positions, or {@code null} for the nodes of an empty complex.
     */
    private final CoordinateReferenceSystem crs;

    /**
     * Number of coordinates of each position: 2, 3, or 0 for the nodes of an empty complex.
     */
    final int dimension;

    /**
     * Coordinates of the nodes, {@link #dimension} values per node.
     */
    double[] coordinates;

    /**
     * Coordinates of the nodes in three dimensions, with z = 0 for 2-dimensional coordinates.
     * This is the same array as {@link #coordinates} when the dimension is 3.
     */
    double[] xyz;

    /**
     * Number of points, including those of removed nodes.
     */
    private int size;

    /**
     * Values of the attributes other than positions. The length of the arrays is the capacity, not the size.
     */
    private final Map<String, Array> attributes = new LinkedHashMap<>();

    /**
     * For each attribute other than positions, the nodes having a value for that attribute.
     */
    private final Map<String, BitSet> assigned = new HashMap<>();

    /**
     * Creates an empty set of nodes.
     *
     * @param  crs  the reference system of the positions, or {@code null} for an empty complex.
     */
    NodePoints(final CoordinateReferenceSystem crs) {
        this.crs  = crs;
        dimension = (crs != null) ? crs.getCoordinateSystem().getDimension() : 0;
        coordinates = new double[INITIAL_CAPACITY * dimension];
        xyz = (dimension == 3) ? coordinates : new double[INITIAL_CAPACITY * 3];
    }

    /**
     * Returns the number of points for which space is allocated.
     */
    private int capacity() {
        return xyz.length / 3;
    }

    /**
     * Allocates space for the given number of points.
     */
    private void ensureCapacity(final int required) {
        final int capacity = capacity();
        if (required > capacity) {
            final int n = Math.max(required, capacity * 2);
            coordinates = Arrays.copyOf(coordinates, n * dimension);
            xyz = (dimension == 3) ? coordinates : Arrays.copyOf(xyz, n * 3);
            for (final Map.Entry<String, Array> entry : attributes.entrySet()) {
                final Array old = entry.getValue();
                final Array array = NDArrays.of(old.getSampleSystem(), old.getDataType(), n);
                array.set(0, old, 0, size);
                entry.setValue(array);
            }
        }
    }

    /**
     * Adds a point at the given position, without value for the other attributes.
     *
     * @param  position  the {@link #dimension} coordinates of the point.
     * @return index of the new point.
     */
    final int add(final double[] position) {
        ensureCapacity(size + 1);
        final int node = size++;
        setCoordinates(node, position);
        return node;
    }

    /**
     * Changes the position of a point.
     */
    final void setCoordinates(final int node, final double[] position) {
        System.arraycopy(position, 0, coordinates, node * dimension, dimension);
        if (dimension != 3) {
            for (int k = 0; k < 3; k++) {
                xyz[node * 3 + k] = (k < dimension) ? position[k] : 0;
            }
        }
    }

    /**
     * Copies to the given node the attributes of a point of another set of data points,
     * for the attributes which have no value yet for that node. Positions are not copied.
     * Attributes unknown to this object are added, with zero values for the other nodes.
     *
     * @param  node    index of the node.
     * @param  source  the data points from which to copy the attributes.
     * @param  index   index of the point in the source.
     * @throws IllegalArgumentException if an attribute has not the same number of components as before.
     */
    final void merge(final int node, final DataPoints source, final int index) {
        final DataPointsType type = source.getType();
        for (final String name : type.getAttributeNames()) {
            if (ATT_POSITION.equals(name)) {
                continue;
            }
            Array array = attributes.get(name);
            if (array == null) {
                array = NDArrays.of(type.getAttributeSystem(name), type.getAttributeType(name), capacity());
                attributes.put(name, array);
                assigned.put(name, new BitSet());
            }
            final BitSet set = assigned.get(name);
            if (!set.get(node)) {
                final Vector<?> value = source.getAttribute(index, name);
                if (value != null) {
                    if (value.getDimension() != array.getDimension()) {
                        throw new IllegalArgumentException("The attribute " + name + " has " + value.getDimension()
                                + " components, while " + array.getDimension() + " were expected.");
                    }
                    array.set(node, value);
                    set.set(node);
                }
            }
        }
    }

    /**
     * Sets the attributes of a node by interpolation between two nodes, for the attributes having a value
     * at both nodes. The interpolation method of each attribute is given by {@link #getAttributeInterpolation(String)}.
     * Positions are not interpolated.
     *
     * @param  node   index of the node to set.
     * @param  a      index of the first node.
     * @param  b      index of the second node.
     * @param  ratio  position between the nodes, from 0 at {@code a} to 1 at {@code b}.
     */
    final void interpolate(final int node, final int a, final int b, final double ratio) {
        for (final Map.Entry<String, Array> entry : attributes.entrySet()) {
            final String name = entry.getKey();
            final BitSet set = assigned.get(name);
            if (set.get(a) && set.get(b)) {
                entry.getValue().set(node, interpolateAttribute(name, a, b, ratio));
                set.set(node);
            }
        }
    }

    /**
     * Returns whether the given node has a value for the given attribute.
     *
     * @param  index  index of the node.
     * @param  name   name of the attribute.
     * @return whether the node has a value, always true for positions.
     */
    public boolean hasAttribute(final int index, final String name) {
        Objects.checkIndex(index, size);
        if (ATT_POSITION.equals(name)) {
            return true;
        }
        final BitSet set = assigned.get(name);
        return (set != null) && set.get(index);
    }

    /**
     * Returns the reference system of the positions.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Unsupported, since the topology is computed in the reference system of the complex.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem cs) {
        throw new UnsupportedOperationException("The reference system of a topological complex cannot be changed.");
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public DataPointsType getType() {
        return this;
    }

    /**
     * Returns the number of points, including those of removed nodes.
     */
    @Override
    public int size() {
        return size;
    }

    @Override
    public Point getPoint(final int index) {
        Objects.checkIndex(index, size);
        return new IndexedPoint(this, index);
    }

    @Override
    public Vector<?> getPosition(final int index) {
        Objects.checkIndex(index, size);
        return Vectors.createDouble(dimension).set(coordinates, index * dimension);
    }

    /**
     * Unsupported, since positions define the topology. Use {@link ComplexEditor} instead.
     */
    @Override
    public void setPosition(final int index, final ReadOnly.Vector<?> value) {
        throw new UnsupportedOperationException("The nodes of a topological complex are moved by ComplexEditor.");
    }

    @Override
    public Vector<?> getAttribute(final int index, final String name) {
        if (ATT_POSITION.equals(name)) {
            return getPosition(index);
        }
        Objects.checkIndex(index, size);
        final Array array = attributes.get(name);
        return (array != null) ? array.get(index) : null;
    }

    /**
     * Sets the value of an attribute other than positions.
     *
     * @throws UnsupportedOperationException if the attribute is the positions.
     * @throws IllegalArgumentException if there is no attribute of the given name.
     */
    @Override
    public void setAttribute(final int index, final String name, final ReadOnly.Vector<?> value) {
        if (ATT_POSITION.equals(name)) {
            setPosition(index, value);
        }
        Objects.checkIndex(index, size);
        final Array array = attributes.get(name);
        if (array == null) {
            throw new IllegalArgumentException("No attribute named " + name + '.');
        }
        array.set(index, value);
        assigned.get(name).set(index);
    }

    @Override
    public Array getAttributeArray(final String name) {
        if (ATT_POSITION.equals(name)) {
            return NDArrays.of(crs, Arrays.copyOf(coordinates, size * dimension));
        }
        final Array array = attributes.get(name);
        if (array == null) {
            return null;
        }
        final Array copy = NDArrays.of(array.getSampleSystem(), array.getDataType(), size);
        copy.set(0, array, 0, size);
        return copy;
    }

    /**
     * Returns a copy of the points at the given indices, with all their attributes.
     */
    @Override
    public DataPoints subset(final int[] indices) {
        final double[] c = new double[indices.length * dimension];
        for (int i = 0; i < indices.length; i++) {
            System.arraycopy(coordinates, Objects.checkIndex(indices[i], size) * dimension, c, i * dimension, dimension);
        }
        final Map<String, Array> subset = new LinkedHashMap<>();
        subset.put(ATT_POSITION, NDArrays.of(crs, c));
        attributes.forEach((name, array) -> subset.put(name, NDArrays.subset(array, indices).copy()));
        return new ArrayDataPoints(subset);
    }

    @Override
    public SampleSystem getAttributeSystem(final String name) {
        if (ATT_POSITION.equals(name)) {
            return SampleSystem.of(crs);
        }
        final Array array = attributes.get(name);
        return (array != null) ? array.getSampleSystem() : null;
    }

    @Override
    public DataType getAttributeType(final String name) {
        if (ATT_POSITION.equals(name)) {
            return DataType.DOUBLE;
        }
        final Array array = attributes.get(name);
        return (array != null) ? array.getDataType() : null;
    }

    @Override
    public List<String> getAttributeNames() {
        final List<String> names = new ArrayList<>(attributes.size() + 1);
        names.add(ATT_POSITION);
        names.addAll(attributes.keySet());
        return names;
    }

    @Override
    public int hashCode() {
        return DataPoints.hashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return DataPoints.equals(this, obj);
    }
}
