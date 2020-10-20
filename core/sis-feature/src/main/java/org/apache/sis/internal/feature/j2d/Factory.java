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
package org.apache.sis.internal.feature.j2d;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.math.Vector;
import org.apache.sis.util.UnsupportedImplementationException;


/**
 * The factory of geometry objects backed by Java2D.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   0.7
 * @module
 */
public final class Factory extends Geometries<Shape> {
    /**
     * The singleton instance of this factory.
     */
    public static final Factory INSTANCE = new Factory();

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.JAVA2D, Shape.class, Point2D.class, Shape.class, Shape.class);
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    protected GeometryWrapper<Shape> createWrapper(final Object geometry) {
        if (geometry instanceof Point2D) {
            return new PointWrapper((Point2D) geometry);
        } else {
            return new Wrapper((Shape) geometry);
        }
    }

    /**
     * Notifies that this library can create geometry objects backed by single-precision type.
     */
    @Override
    public boolean supportSinglePrecision() {
        return true;
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return new Point2D.Double(x, y);
    }

    /**
     * Creates an initially empty Java2D path.
     *
     * @param  isFloat {@code true} for {@code float} type, {@code false} for {@code double} type.
     * @param  length  initial capacity.
     * @return an initially empty path of the given type.
     */
    private static Path2D createPath(final boolean isFloat, final int length) {
        return isFloat ? new Path2D.Float (Path2D.WIND_NON_ZERO, length)
                       : new Path2D.Double(Path2D.WIND_NON_ZERO, length);
    }

    /**
     * Creates a path from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     * The geometry may be backed by {@code float} or {@code double} primitive type,
     * depending on the type used by the given vectors.
     *
     * @param  polygon      whether to return the path as a polygon instead than polyline.
     * @param  dimension    the number of dimensions ({@value #BIDIMENSIONAL} or {@value #TRIDIMENSIONAL}).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @throws UnsupportedOperationException if this operation is not implemented for the given number of dimensions.
     */
    @Override
    public Shape createPolyline(final boolean polygon, final int dimension, final Vector... coordinates) {
        if (dimension != BIDIMENSIONAL) {
            throw new UnsupportedOperationException(unsupported(dimension));
        }
        /*
         * Computes the total length of all vectors and verifies if any vector
         * requires double-precision numbers instead of single-precision.
         */
        int length = 0;
        boolean isFloat = true;
        for (final Vector v : coordinates) {
            if (v != null) {
                length = Math.addExact(length, v.size());
                if (isFloat) {
                    isFloat = v.isSinglePrecision();
                }
            }
        }
        /*
         * Shortcut (for performance reason) when building a single line segment.
         * Note: Point2D is not an instance of Shape, so we can not make a special case for it.
         */
        length /= BIDIMENSIONAL;
        if (length == 2 && coordinates.length == 1) {
            final Vector v = coordinates[0];
            final double x1, y1, x2, y2;
            if (!Double.isNaN(x1 = v.doubleValue(0)) &&
                !Double.isNaN(y1 = v.doubleValue(1)) &&
                !Double.isNaN(x2 = v.doubleValue(2)) &&
                !Double.isNaN(y2 = v.doubleValue(3)))
            {
                final Line2D path = isFloat ? new Line2D.Float() : new Line2D.Double();
                path.setLine(x1, y1, x2, y2);
                return path;
            }
        }
        /*
         * General case if we could not use a shortcut.
         */
        final Path2D path = createPath(isFloat, length);
        boolean lineTo = false;
        for (final Vector v : coordinates) {
            final int size = v.size();
            for (int i=0; i<size;) {
                final double x = v.doubleValue(i++);
                final double y = v.doubleValue(i++);
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    if (polygon) {
                        path.closePath();
                    }
                    lineTo = false;
                } else if (lineTo) {
                    path.lineTo(x, y);
                } else {
                    path.moveTo(x, y);
                    lineTo = true;
                }
            }
        }
        if (polygon) {
            path.closePath();
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * Creates a multi-polygon from an array of geometries.
     * Callers must ensure that the given objects are Java2D geometries.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @throws ClassCastException if an element in the array is not a Java2D geometry.
     */
    @Override
    public GeometryWrapper<Shape> createMultiPolygon(final Object[] geometries) {
        if (geometries.length == 1) {
            return new Wrapper((Shape) unwrap(geometries[0]));
        }
        final Shape[] shapes = new Shape[geometries.length];
        for (int i=0; i<geometries.length; i++) {
            shapes[i] = (Shape) unwrap(geometries[i]);
        }
        boolean isFloat = true;
        for (final Shape geometry : shapes) {
            if (!ShapeUtilities.isFloat(geometry)) {
                isFloat = false;
                break;
            }
        }
        final Path2D path = createPath(isFloat, 20);
        for (final Shape geometry : shapes) {
            path.append(geometry, false);
        }
        return new Wrapper(path);
    }

    /**
     * Well Known Text (WKT) parsing not supported with Java2D.
     */
    @Override
    public GeometryWrapper<Shape> parseWKT(final String wkt) {
        throw new UnsupportedImplementationException(unsupported("parseWKT"));
    }

    /**
     * Well Known Binary (WKB) reading not supported with Java2D.
     */
    @Override
    public GeometryWrapper<Shape> parseWKB(final ByteBuffer data) {
        throw new UnsupportedImplementationException(unsupported("parseWKB"));
    }
}
