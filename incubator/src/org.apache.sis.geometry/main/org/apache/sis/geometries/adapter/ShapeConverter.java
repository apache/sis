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

import java.awt.Shape;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.geom.PathIterator;
import java.awt.geom.IllegalPathStateException;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.referencing.internal.shared.AbstractShape;


/**
 * Converts a Java2D {@link Shape} to a SIS {@link Geometry}.
 * Two subclasses exist depending on whether the geometries will store
 * coordinates as {@code float} or {@code double} floating point numbers.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class ShapeConverter {

    private static final int DIMENSION = 2;

    /**
     * Initial number of coordinate values that the buffer can hold.
     * The buffer capacity will be expanded as needed.
     */
    private static final int INITIAL_CAPACITY = 64;

    /**
     * Bit mask of the kind of geometric objects created.
     * Used for detecting if all objects are of the same type.
     *
     * @see #geometryType
     */
    private static final int POINT = 1, LINESTRING = 2, POLYGON = 4;

    /**
     * All geometries that are component of a multi-geometries.
     * The above masks tell if the geometry can be built as a multi-line strings or multi-points.
     */
    private final List<Geometry> geometries;

    /**
     * Iterator over the coordinates of the Java2D shape to convert to a SIS geometry.
     */
    protected final PathIterator iterator;

    /**
     * Number of values in the {@code float[]} or {@code double[]} array stored by sub-class.
     */
    protected int length;

    /**
     * Bitmask combination of the type of all geometries built.
     * This is a combination of {@link #POINT}, {@link #LINESTRING} and/or {@link #POLYGON}.
     */
    private int geometryType;

    /**
     * Creates a new converter from Java2D shape to SIS geometry.
     *
     * @param  iterator  iterator over the coordinates of the Java2D shape to convert to a SIS geometry.
     */
    ShapeConverter(final PathIterator iterator) {
        this.iterator   = iterator;
        this.geometries = new ArrayList<>();
    }

    /**
     * Converts a Java2D Shape to a SIS geometry.
     * Coordinates are copies; this is not a view.
     *
     * @param  shape     the Java2D shape to convert. Cannot be {@code null}.
     * @param  flatness  the maximum distance that line segments are allowed to deviate from curves.
     * @return SIS geometry with shape coordinates. Never null but can be empty.
     */
    public static Geometry create(final Shape shape, final double flatness) {
        if (shape instanceof ShapeAdapter) {
            return ((ShapeAdapter) shape).geometry;
        }
        final PathIterator iterator = shape.getPathIterator(null, flatness);
        final ShapeConverter converter;
        if (AbstractShape.isFloat(shape)) {
            converter = new ShapeConverter.Float(iterator);
        } else {
            converter = new ShapeConverter.Double(iterator);
        }
        return converter.build();
    }

    /**
     * A converter of Java2D {@link Shape} to a SIS {@link Geometry}
     * storing coordinates as {@code double} values.
     */
    private static final class Double extends ShapeConverter {
        /** A temporary array for the transfer of coordinate values. */
        private final double[] vertex;

        /** Coordinate of current geometry. The number of valid values is {@link #length}. */
        private double[] buffer;

        /** Creates a new converter for the given path iterator. */
        Double(final PathIterator iterator) {
            super(iterator);
            vertex = new double[6];
            buffer = new double[INITIAL_CAPACITY];
        }

        /** Delegates to {@link PathIterator#currentSegment(double[])}. */
        @Override int currentSegment() {
            return iterator.currentSegment(vertex);
        }

        /** Stores the single point obtained by the last call to {@link #currentSegment()}. */
        @Override void addPoint() {
            addPoint(vertex);
        }

        /** Implementation of {@link #addPoint()} shared with {@link #toSequence(boolean)}. */
        private void addPoint(final double[] source) {
            if (length >= buffer.length) {
                buffer = Arrays.copyOf(buffer, length * 2);
            }
            System.arraycopy(source, 0, buffer, length, DIMENSION);
            length += DIMENSION;
        }

        /** Returns a copy of current coordinate values as a SIS coordinate sequence. */
        @Override PointSequence toSequence(final boolean close) {
            if (close && !Arrays.equals(buffer, 0, 2, buffer, length - 2, length)) {
                addPoint(buffer);
            }

            final Array array = NDArrays.of(SampleSystem.cartesian(2), Arrays.copyOf(buffer, length));
            return new ArraySequence(array);
        }
    }

    /**
     * A converter of Java2D {@link Shape} to a SIS {@link Geometry}
     * storing coordinates as {@code float} values.
     */
    private static final class Float extends ShapeConverter {
        /** A temporary array for the transfer of coordinate values. */
        private final float[] vertex;

        /** Coordinate of current geometry. The number of valid values is {@link #length}. */
        private float[] buffer;

        /** Creates a new converter for the given path iterator. */
        Float(final PathIterator iterator) {
            super(iterator);
            vertex = new float[6];
            buffer = new float[INITIAL_CAPACITY];
        }

        /** Delegates to {@link PathIterator#currentSegment(float[])}. */
        @Override int currentSegment() {
            return iterator.currentSegment(vertex);
        }

        /** Stores the single point obtained by the last call to {@link #currentSegment()}. */
        @Override void addPoint() {
            addPoint(vertex);
        }

        /** Implementation of {@link #addPoint()} shared with {@link #toSequence(boolean)}. */
        private void addPoint(final float[] source) {
            if (length >= buffer.length) {
                buffer = Arrays.copyOf(buffer, length * 2);
            }
            System.arraycopy(source, 0, buffer, length, DIMENSION);
            length += DIMENSION;
        }

        /** Returns a copy of current coordinate values as a SIS coordinate sequence. */
        @Override PointSequence toSequence(final boolean close) {
            if (close && !Arrays.equals(buffer, 0, 2, buffer, length - 2, length)) {
                addPoint(buffer);
            }
            final Array array = NDArrays.of(SampleSystem.cartesian(2), Arrays.copyOf(buffer, length));
            return new ArraySequence(array);
        }
    }

    /**
     * Returns the coordinates and type of the current path segment in the iteration.
     * This method delegate to one of the two {@code PathIterator.currentSegment(…)}
     * methods, depending on the precision of floating-point values.
     */
    abstract int currentSegment();

    /**
     * Stores the single point obtained by the last call to {@link #currentSegment()}.
     * As a consequence, {@link #length} is increased by {@value #DIMENSION}.
     */
    abstract void addPoint();

    /**
     * Returns a copy of current coordinate values as a SIS coordinate sequence.
     * The number of values to copy in a new array is {@link #length}.
     *
     * @param  close  whether to ensure that the first point is repeated as the last point.
     * @return a SIS coordinate sequence containing a copy of current coordinate values.
     */
    abstract PointSequence toSequence(boolean close);

    /**
     * Iterates over all coordinates given by the {@link #iterator} and stores them in a SIS geometry.
     * The path shall contain only straight lines. Curves are not supported yet.
     * The geometry will be constrained to two-dimensional coordinate tuples.
     */
    private Geometry build() {
        while (!iterator.isDone()) {
            switch (currentSegment()) {
                case PathIterator.SEG_MOVETO: {
                    flush(false);
                    addPoint();
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    if (length == 0) {
                        throw new IllegalPathStateException("LINETO without previous MOVETO.");
                    }
                    addPoint();
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    flush(true);
                    break;
                }
                default: {
                    throw new IllegalPathStateException("Must contain only flat segments.");
                }
            }
            iterator.next();
        }
        flush(false);
        final int count = geometries.size();
        if (count == 1) {
            return geometries.get(0);
        }

        switch (geometryType) {
            case 0:          return GeometryFactory.INSTANCE.createEmpty(Geometries.getUndefinedCRS(DIMENSION));
            default:         return GeometryFactory.INSTANCE.createGeometryCollection(geometries.toArray(Geometry[]::new));
            case POINT:      return GeometryFactory.INSTANCE.createMultiPoint(geometries.toArray(Point[]::new));
            case LINESTRING: return GeometryFactory.INSTANCE.createMultiLineString(geometries.toArray(LineString[]::new));
            case POLYGON:    break;
        }
        /*
         * Java2D shapes and SIS geometries differ in their way to fill interior.
         * Java2D fills the resulting contour based on visual winding rules.
         * SIS has a system where outer shell and holes are clearly separated.
         * We would need to draw contours as Java2D for computing SIS equivalent,
         * but it would require a lot of work. In the meantime, the SymDifference
         * operation is what behave the most like EVEN_ODD or NON_ZERO winding rules.
         */
        // Sort by area, bigger geometries are the outter rings.
        geometries.sort((Geometry o1, Geometry o2) -> {
                double area1 = (o1 instanceof Surface s) ? s.getArea() : 0.0;
                double area2 = (o2 instanceof Surface s) ? s.getArea() : 0.0;
                return java.lang.Double.compare(area2, area1);
            });

        Geometry result = geometries.get(0);
        for (int i=1; i<count; i++) {
            Geometry other = geometries.get(i);
            if (result.intersects(other)) {
                result = result.symDifference(other);   // Ring is a hole.
            } else {
                result = result.union(other);           // Ring is a separate polygon.
            }
        }
        return result;
    }

    /**
     * Copies current coordinates in a new SIS geometry,
     * then resets {@link #length} to 0 in preparation for the next geometry.
     *
     * @param  isRing  whether the geometry should be a closed polygon.
     */
    private void flush(final boolean isRing) {
        if (length != 0) {
            Geometry geometry;
            if (length == DIMENSION) {
                geometry = GeometryFactory.INSTANCE.createPoint(toSequence(false));
                geometryType |= POINT;
            } else {
                if (isRing) {
                    geometry = GeometryFactory.INSTANCE.createPolygon(GeometryFactory.INSTANCE.createLinearRing(toSequence(true)), null);
                    geometryType |= POLYGON;
                } else {
                    geometry = GeometryFactory.INSTANCE.createLineString(toSequence(false));
                    geometryType |= LINESTRING;
                }
            }
            geometries.add(geometry);
            length = 0;
        }
    }
}
