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
package org.apache.sis.geometries.adapter;

import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * Java2D path iterator for SIS geometry.
 * This iterator gets coordinates from the {@link CoordinateSequence} associated to each geometry.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PathIteratorAdapter implements PathIterator {
    /**
     * The transform to apply on returned coordinate values.
     * Never null (may be the identity transform instead).
     */
    private final AffineTransform at;

    /**
     * Provider of point sequences.
     */
    private final Iterator<PointSequence> sequences;

    /**
     * The sequence of point tuples to return,
     * or {@code null} if the iteration is finished.
     */
    private PointSequence coordinates;

    /**
     * Number of points to return in the sequence.
     */
    private int pointCount;

    /**
     * Index of the coordinates tuple which closes the current polygon, or -1 if none.
     */
    private int closingPoint;

    /**
     * Index of current position in the sequence of coordinate tuples.
     */
    private int currentIndex;

    /**
     * Creates a new iterator which will transform coordinates using the given transform.
     *
     * @param  geometry  the geometry on which to iterator.
     * @param  at  the transform to apply, or {@code null} for the identity transform.
     */
    PathIteratorAdapter(final Geometry geometry, final AffineTransform at) {
        this.at = (at != null) ? at : new AffineTransform();
        sequences = iterator(geometry);
        nextSequence();
    }

    /**
     * Moves to the next sequence of coordinate tuples. The {@link #coordinates} sequence
     * should be null when this method is invoked. If there are no more sequences,
     * then the {@link #coordinates} will be left unchanged (i.e. null).
     */
    private void nextSequence() {
        while (sequences.hasNext()) {
            coordinates  = sequences.next();
            pointCount   = coordinates.size();
            closingPoint = pointCount - 1;
            if (closingPoint < 1 || !coordinates.getPosition(0).equals(coordinates.getPosition(closingPoint))) {
                closingPoint = -1;      // No closing point.
            }
            if (pointCount > 0) {
                return;
            }
        }
    }

    /**
     * Moves the iterator to the next segment.
     */
    @Override
    public void next() {
        if (++currentIndex >= pointCount) {
            currentIndex = 0;
            coordinates = null;
            nextSequence();
        }
    }

    /**
     * Returns {@code true} if iteration is finished.
     */
    @Override
    public boolean isDone() {
        return coordinates == null;
    }

    /**
     * Returns the winding rule for determining the interior of the path.
     * Current implementation returns the same rule as the one returned
     * by {@link org.locationtech.jts.awt.ShapeCollectionPathIterator}.
     */
    @Override
    public int getWindingRule() {
        return WIND_EVEN_ODD;
    }

    /**
     * Returns the coordinates and type of the current path segment in the iteration.
     *
     * @param  coords an array where to store the data returned from this method.
     * @return the path-segment type of the current path segment.
     */
    @Override
    public int currentSegment(final double[] coords) {
        if (currentIndex == closingPoint) {
            return SEG_CLOSE;
        }
        Tuple<?> position = coordinates.getPosition(currentIndex);
        coords[0] = position.get(0);
        coords[1] = position.get(1);
        at.transform(coords, 0, coords, 0, 1);
        return (currentIndex == 0) ? SEG_MOVETO : SEG_LINETO;
    }

    /**
     * Returns the coordinates and type of the current path segment in the iteration.
     *
     * @param  coords an array where to store the data returned from this method.
     * @return the path-segment type of the current path segment.
     */
    @Override
    public int currentSegment(final float[] coords) {
        if (currentIndex == closingPoint) {
            return SEG_CLOSE;
        }
        Tuple<?> position = coordinates.getPosition(currentIndex);
        coords[0] = (float) position.get(0);
        coords[1] = (float) position.get(1);
        at.transform(coords, 0, coords, 0, 1);
        return (currentIndex == 0) ? SEG_MOVETO : SEG_LINETO;
    }

    /**
     * Returns an iterator over the coordinate sequences of the given geometry.
     *
     * @param  geometry  the geometry for which to get coordinate sequences.
     * @return coordinate sequences over the given geometry.
     */
    private static Iterator<PointSequence> iterator(final Geometry geometry) {
        final Collection<PointSequence> sequences;
        if (geometry instanceof LineString) {
            sequences = List.of(((LineString) geometry).getPoints());
        } else if (geometry instanceof Point) {
            sequences = List.of(((Point) geometry).asPointSequence());
        } else if (geometry instanceof Polygon) {
            return new RingIterator((Polygon) geometry);
        } else if (geometry instanceof GeometryCollection) {
            return new GeomIterator((GeometryCollection) geometry);
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, Classes.getShortClassName(geometry)));
        }
        return sequences.iterator();
    }

    /**
     * An iterator over the coordinate sequences of a polygon.
     * The first coordinate sequence is the exterior ring and
     * all other sequences are interior rings.
     */
    private static final class RingIterator implements Iterator<PointSequence> {
        /** The polygon for which to return rings. */
        private final Polygon polygon;

        /** Index of the interior ring, or -1 for the exterior ring. */
        private int interior;

        /** Created a new iterator for the given polygon. */
        RingIterator(final Polygon geometry) {
            polygon  = geometry;
            interior = -1;
        }

        /** Returns {@code true} if there is more rings to return. */
        @Override public boolean hasNext() {
            return interior < polygon.getNumInteriorRing();
        }

        /** Returns the coordinate sequence of the next ring. */
        @Override public PointSequence next() {
            final LineString current;
            if (interior < 0) {
                current = polygon.getExteriorRing();
            } else {
                current = polygon.getInteriorRingN(interior);
            }
            interior++;
            return current.getPoints();
        }
    }

    /**
     * An iterator over the coordinate sequences of a geometry collection.
     */
    private static final class GeomIterator implements Iterator<PointSequence> {
        /** The collection for which to return geometries. */
        private final GeometryCollection collection;

        /** Index of current geometry. */
        private int index;

        /** Coordinate sequences of the current geometry. */
        private Iterator<PointSequence> current;

        /** Created a new iterator for the given collection. */
        GeomIterator(final GeometryCollection collection) {
            this.collection = collection;
            while (index < collection.getNumGeometries()) {
                current = iterator(collection.getGeometryN(index));
                if (current.hasNext()) break;
                index++;
            }
        }

        /** Returns {@code true} if there is more sequences to return. */
        @Override public boolean hasNext() {
            while (!current.hasNext()) {
                if (++index >= collection.getNumGeometries()) {
                    return false;
                }
                current = iterator(collection.getGeometryN(index));
            }
            return true;
        }

        /** Returns the coordinate sequence of the next geometry. */
        @Override public PointSequence next() {
            return current.next();
        }
    }
}
