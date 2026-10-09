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

import java.util.Arrays;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;


/**
 * Several sequences of positions seen as a single one, in the order they are given.
 * This is the sequence of an aggregate geometry: the rings of a surface, the elements of a
 * collection, the segments of a compound curve. The positions a geometry holds several times,
 * such as the end position two consecutive segments have in common, appear as many times.
 *
 * <p>The sequences must all declare the same attributes, which {@link #ConcatenatedDataPoints}
 * verifies.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ConcatenatedDataPoints implements DataPoints {

    /**
     * The sequences this view concatenates, never empty.
     */
    private final DataPoints[] sources;

    /**
     * Index at which each source starts in this view, plus the total size at the last position.
     * {@code offsets[i]} is the index of the first position of {@code sources[i]}, so the size
     * of this view is {@code offsets[sources.length]}.
     */
    private final int[] offsets;

    /**
     * Creates a view over the given sequences.
     *
     * @param  sources  the sequences to concatenate, at least one.
     * @throws IllegalArgumentException if no sequence is given,
     *         or if the sequences do not declare the same attributes.
     */
    public ConcatenatedDataPoints(final DataPoints... sources) {
        if (sources.length == 0) {
            throw new IllegalArgumentException("At least one sequence of positions is required");
        }
        this.sources = sources;
        this.offsets = new int[sources.length + 1];
        final DataPointsType type = sources[0].getType();
        for (int i = 0; i < sources.length; i++) {
            if (i != 0) {
                Geometries.ensureSameAttributes(type, sources[i].getType());
            }
            offsets[i+1] = offsets[i] + sources[i].size();
        }
    }

    /**
     * Creates a view over the sequences of the given geometries.
     *
     * @param  geometries  the geometries whose sequences to concatenate, at least one.
     * @return the positions of the given geometries, as a single sequence.
     * @throws IllegalArgumentException if no geometry is given,
     *         or if they do not declare the same attributes.
     */
    public static DataPoints of(final Geometry... geometries) {
        final DataPoints[] sources = new DataPoints[geometries.length];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = geometries[i].getDataPoints();
        }
        return new ConcatenatedDataPoints(sources);
    }

    /**
     * Creates a view over the sequences of the given geometries.
     *
     * @param  first  the geometry whose sequence comes first, not null.
     * @param  others the geometries whose sequences follow, possibly empty.
     * @return the positions of the given geometries, as a single sequence.
     */
    public static DataPoints of(final Geometry first, final List<? extends Geometry> others) {
        final DataPoints[] sources = new DataPoints[others.size() + 1];
        sources[0] = first.getDataPoints();
        for (int i = 1; i < sources.length; i++) {
            sources[i] = others.get(i-1).getDataPoints();
        }
        return new ConcatenatedDataPoints(sources);
    }

    /**
     * Returns the index of the source holding the given position of this view.
     */
    private int sourceOf(final int index) {
        if (index < 0 || index >= offsets[sources.length]) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside the [0 … "
                    + offsets[sources.length] + "[ range of this sequence");
        }
        final int i = Arrays.binarySearch(offsets, 0, sources.length, index);
        // A negative result is the insertion point: the source is the one starting before the index.
        return (i >= 0) ? i : ~i - 1;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return sources[0].getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        for (final DataPoints source : sources) {
            source.setCoordinateReferenceSystem(cs);
        }
    }

    @Override
    public DataPointsType getType() {
        return sources[0].getType();
    }

    @Override
    public int size() {
        return offsets[sources.length];
    }

    @Override
    public Point getPoint(int index) {
        final int i = sourceOf(index);
        return sources[i].getPoint(index - offsets[i]);
    }

    @Override
    public Vector<?> getPosition(int index) {
        final int i = sourceOf(index);
        return sources[i].getPosition(index - offsets[i]);
    }

    @Override
    public void setPosition(int index, ReadOnly.Vector<?> value) {
        final int i = sourceOf(index);
        sources[i].setPosition(index - offsets[i], value);
    }

    @Override
    public Vector<?> getAttribute(int index, String name) {
        final int i = sourceOf(index);
        return sources[i].getAttribute(index - offsets[i], name);
    }

    @Override
    public void setAttribute(int index, String name, ReadOnly.Vector<?> value) {
        final int i = sourceOf(index);
        sources[i].setAttribute(index - offsets[i], name, value);
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
