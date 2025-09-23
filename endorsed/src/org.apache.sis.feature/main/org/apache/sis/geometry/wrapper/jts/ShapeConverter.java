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
package org.apache.sis.geometry.wrapper.jts;

import java.awt.Shape;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.geom.PathIterator;
import java.awt.geom.IllegalPathStateException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.apache.sis.referencing.internal.shared.AbstractShape;


/**
 * Converts a Java2D {@link Shape} to a JTS {@link Geometry}.
 * Two subclasses exist depending on whether the geometries will store
 * coordinates as {@code float} or {@code double} floating point numbers.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ShapeConverter extends ConverterTo2D {
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
     * Iterator over the coordinates of the Java2D shape to convert to a JTS geometry.
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
     * Creates a new converter from Java2D shape to JTS geometry.
     *
     * @param  factory   the JTS factory for creating geometry, or {@code null} for automatic.
     * @param  iterator  iterator over the coordinates of the Java2D shape to convert to a JTS geometry.
     * @param  isFloat   whether to store coordinates as {@code float} instead of {@code double}.
     */
    ShapeConverter(final GeometryFactory factory, final PathIterator iterator, final boolean isFloat) {
        super(factory, isFloat);
        this.iterator   = iterator;
        this.geometries = new ArrayList<>();
    }

    /**
     * Converts a Java2D Shape to a JTS geometry.
     * Coordinates are copies; this is not a view.
     *
     * @param  factory   factory to use for creating the geometry, or {@code null} for the default.
     * @param  shape     the Java2D shape to convert. Cannot be {@code null}.
     * @param  flatness  the maximum distance that line segments are allowed to deviate from curves.
     * @return JTS geometry with shape coordinates. Never null but can be empty.
     */
    static Geometry create(final GeometryFactory factory, final Shape shape, final double flatness) {
        if (shape instanceof ShapeAdapter) {
            return ((ShapeAdapter) shape).geometry;
        }
        final PathIterator iterator = shape.getPathIterator(null, flatness);
        final ShapeConverter converter;
        if (AbstractShape.isFloat(shape)) {
            converter = new ShapeConverter.Float(factory, iterator);
        } else {
            converter = new ShapeConverter.Double(factory, iterator);
        }
        return converter.build();
    }

    /**
     * A converter of Java2D {@link Shape} to a JTS {@link Geometry}
     * storing coordinates as {@code double} values.
     */
    private static final class Double extends ShapeConverter {
        /** A temporary array for the transfer of coordinate values. */
        private final double[] vertex;

        /** Coordinate of current geometry. The number of valid values is {@link #length}. */
        private double[] buffer;

        /** Creates a new converter for the given path iterator. */
        Double(final GeometryFactory factory, final PathIterator iterator) {
            super(factory, iterator, false);
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

        /** Returns a copy of current coordinate values as a JTS coordinate sequence. */
        @Override PackedCoordinateSequence toSequence(final boolean close) {
            if (close && !Arrays.equals(buffer, 0, 2, buffer, length - 2, length)) {
                addPoint(buffer);
            }
            return new PackedCoordinateSequence.Double(buffer, length);
        }
    }

    /**
     * A converter of Java2D {@link Shape} to a JTS {@link Geometry}
     * storing coordinates as {@code float} values.
     */
    private static final class Float extends ShapeConverter {
        /** A temporary array for the transfer of coordinate values. */
        private final float[] vertex;

        /** Coordinate of current geometry. The number of valid values is {@link #length}. */
        private float[] buffer;

        /** Creates a new converter for the given path iterator. */
        Float(final GeometryFactory factory, final PathIterator iterator) {
            super(factory, iterator, false);
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

        /** Returns a copy of current coordinate values as a JTS coordinate sequence. */
        @Override PackedCoordinateSequence toSequence(final boolean close) {
            if (close && !Arrays.equals(buffer, 0, 2, buffer, length - 2, length)) {
                addPoint(buffer);
            }
            return new PackedCoordinateSequence.Float(buffer, length);
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
     * Returns a copy of current coordinate values as a JTS coordinate sequence.
     * The number of values to copy in a new array is {@link #length}.
     * The copy is wrapped in a {@link PackedCoordinateSequence}.
     *
     * @param  close  whether to ensure that the first point is repeated as the last point.
     * @return a JTS coordinate sequence containing a copy of current coordinate values.
     */
    abstract PackedCoordinateSequence toSequence(boolean close);

    /**
     * Iterates over all coordinates given by the {@link #iterator} and stores them in a JTS geometry.
     * The path shall contain only straight lines. Curves are not supported.
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
            return anyTo2D(geometries.get(0));
        }
        switch (geometryType) {
            case 0:          return factory.createEmpty(DIMENSION);  // No need for `enforce2D(…)` since the geometry is empty.
            default:         return collect2D(factory.createGeometryCollection(GeometryFactory.toGeometryArray  (geometries)));
            case POINT:      return enforce2D(factory.createMultiPoint        (GeometryFactory.toPointArray     (geometries)));
            case LINESTRING: return enforce2D(factory.createMultiLineString   (GeometryFactory.toLineStringArray(geometries)));
            case POLYGON:    break;
        }
        /*
         * Java2D shapes and JTS geometries differ in their way to fill interior.
         * Java2D fills the resulting contour based on visual winding rules.
         * JTS has a system where outer shell and holes are clearly separated.
         * We would need to draw contours as Java2D for computing JTS equivalent,
         * but it would require a lot of work. In the meantime, the SymDifference
         * operation is what behave the most like EVEN_ODD or NON_ZERO winding rules.
         */
        // Sort by area, bigger geometries are the outter rings.
        geometries.sort((Geometry o1, Geometry o2) -> java.lang.Double.compare(o2.getArea(), o1.getArea()));
        Geometry result = geometries.get(0);
        for (int i=1; i<count; i++) {
            Geometry other = geometries.get(i);
            if (result.intersects(other)) {
                result = result.symDifference(other);   // Ring is a hole.
            } else {
                result = result.union(other);           // Ring is a separate polygon.
            }
        }
        return anyTo2D(result);
    }

    /**
     * Copies current coordinates in a new JTS geometry,
     * then resets {@link #length} to 0 in preparation for the next geometry.
     *
     * @param  isRing  whether the geometry should be a closed polygon.
     */
    private void flush(final boolean isRing) {
        if (length != 0) {
            Geometry geometry;
            if (length == DIMENSION) {
                geometry = factory.createPoint(toSequence(false));
                geometryType |= POINT;
            } else {
                if (isRing) {
                    /*
                     * Note: JTS does not care about ring orientation.
                     * https://locationtech.github.io/jts/javadoc/org/locationtech/jts/geom/Polygon.html
                     */
                    geometry = factory.createPolygon(toSequence(true));
                    geometryType |= POLYGON;
                    /*
                     * The following operation is expensive, but must be done because Java2D
                     * is more tolerant than JTS regarding incoherent paths. We need to fix
                     * those otherwise we might have errors when aggregating holes in polygons.
                     */
                    if (!geometry.isValid()) {
                        geometry = GeometryFixer.fix(geometry);
                    }
                } else {
                    geometry = factory.createLineString(toSequence(false));
                    geometryType |= LINESTRING;
                }
            }
            geometries.add(geometry);
            length = 0;
        }
    }
}
