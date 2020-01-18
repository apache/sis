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
package org.apache.sis.internal.feature;

import java.util.List;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.j2d.ShapeProperties;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.math.Vector;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.UnsupportedImplementationException;


/**
 * Centralizes usages of some (not all) Java2D geometry API by Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package to Java2D API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   0.7
 * @module
 */
final class Java2D extends Geometries<Shape> {
    /**
     * Creates the singleton instance.
     */
    Java2D() {
        super(GeometryLibrary.JAVA2D, Shape.class, Point2D.class, Shape.class, Shape.class);
    }

    /**
     * If the given geometry is a Java2D geometry, returns a short string representation of the class name,
     * ignoring the primitive type specialization. For example if the class is {@code Rectangle2D.Float},
     * then this method returns {@code "Rectangle2D"}.
     */
    @Override
    final String tryGetLabel(Object geometry) {
        if (geometry instanceof Shape) {
            Class<?> c = geometry.getClass();
            Class<?> e = c.getEnclosingClass();
            return Classes.getShortName(e != null ? e : c);
        }
        return null;
    }

    /**
     * If the given object is a Java2D geometry and its envelope is non-empty, returns
     * that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given object, or {@code null} if the object is not
     *         a recognized geometry or its envelope is empty.
     */
    @Override
    final GeneralEnvelope tryGetEnvelope(final Object geometry) {
        if (geometry instanceof Shape) {
            final Rectangle2D bounds = ((Shape) geometry).getBounds2D();
            if (!bounds.isEmpty()) {                                     // Test if there is NaN values.
                final GeneralEnvelope env = new GeneralEnvelope(BIDIMENSIONAL);
                env.setRange(0, bounds.getMinX(), bounds.getMaxX());
                env.setRange(1, bounds.getMinY(), bounds.getMaxY());
                return env;
            }
        }
        return null;
    }

    /**
     * If the given point is an implementation of this library, returns its coordinates.
     * Otherwise returns {@code null}.
     */
    @Override
    final double[] tryGetPointCoordinates(final Object point) {
        if (point instanceof Point2D) {
            final Point2D pt = (Point2D) point;
            return new double[] {
                pt.getX(),
                pt.getY()
            };
        }
        return null;
    }

    /**
     * If the given geometry is an implementation of this library, returns all its coordinate tuples.
     * Otherwise returns {@code null}. This method is currently used only for testing purpose.
     */
    @Debug
    @Override
    final double[] tryGetAllCoordinates(final Object geometry) {
        if (geometry instanceof Point2D) {
            final Point2D pt = (Point2D) geometry;
            return new double[] {
                pt.getX(),
                pt.getY()
            };
        }
        if (geometry instanceof Shape) {
            final List<double[]> coordinates = new ShapeProperties((Shape) geometry).coordinatesAsDoubles();
            switch (coordinates.size()) {
                case 0:  return ArraysExt.EMPTY_DOUBLE;
                case 1:  return coordinates.get(0);
                default: {
                    /*
                     * Concatenate the coordinates of all polygons in a single array. We lost the distinction
                     * between the different polygons, which is why this method should not be used except for
                     * testing.
                     */
                    final double[] tgt = new double[coordinates.stream().mapToInt((a) -> a.length).sum()];
                    int p = 0;
                    for (final double[] src : coordinates) {
                        System.arraycopy(src, 0, tgt, p, src.length);
                        p += src.length;
                    }
                    return tgt;
                }
            }
        }
        return null;
    }

    /**
     * If the given object is a Java2D geometry, returns its centroid. Otherwise returns {@code null}.
     */
    @Override
    final Object tryGetCentroid(final Object geometry) {
        if (geometry instanceof Shape) {
            final RectangularShape frame = (geometry instanceof RectangularShape)
                    ? (RectangularShape) geometry : ((Shape) geometry).getBounds2D();
            return new Point2D.Double(frame.getCenterX(), frame.getCenterY());
        }
        return null;
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(double x, double y) {
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
     * @param  dimension  the number of dimensions.
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
                    isFloat = Numbers.getEnumConstant(v.getElementType()) <= Numbers.FLOAT;
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
    public Shape createMultiPolygon(final Object[] geometries) {
        if (geometries.length == 1) {
            return (Shape) geometries[0];
        }
        boolean isFloat = true;
        for (final Object geometry : geometries) {
            if (!ShapeUtilities.isFloat(geometry)) {
                isFloat = false;
                break;
            }
        }
        final Path2D path = createPath(isFloat, 20);
        for (final Object geometry : geometries) {
            path.append((Shape) geometry, false);
        }
        return path;
    }

    /**
     * Merges a sequence of points or paths if the first instance is an implementation of this library.
     *
     * @throws ClassCastException if an element in the iterator is not a {@link Shape} or a {@link Point2D}.
     */
    @Override
    final Shape tryMergePolylines(Object next, final Iterator<?> polylines) {
        if (!(next instanceof Shape || next instanceof Point2D)) {
            return null;
        }
        boolean isFloat = ShapeUtilities.isFloat(next);
        Path2D path = isFloat ? new Path2D.Float() : new Path2D.Double();
        boolean lineTo = false;
add:    for (;;) {
            if (next instanceof Point2D) {
                final double x = ((Point2D) next).getX();
                final double y = ((Point2D) next).getY();
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    lineTo = false;
                } else if (lineTo) {
                    path.lineTo(x, y);
                } else {
                    path.moveTo(x, y);
                    lineTo = true;
                }
            } else {
                path.append((Shape) next, false);
                lineTo = false;
            }
            /*
             * 'polylines.hasNext()' check is conceptually part of 'for' instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = polylines.next()) == null);
            /*
             * Convert the path from single-precision to double-precision if needed.
             */
            if (isFloat && !ShapeUtilities.isFloat(next)) {
                path = new Path2D.Double(path);
                isFloat = false;
            }
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * If the given object is a Java2D shape, builds its WKT representation.
     * Current implementation assumes that all closed shapes are polygons and that polygons have no hole
     * (i.e. if a polygon is followed by more data, this method assumes that the additional data is a disjoint polygon).
     *
     * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">Well-known text on Wikipedia</a>
     */
    @Override
    final String tryFormatWKT(final Object geometry, final double flatness) {
        return (geometry instanceof Shape) ? new ShapeProperties((Shape) geometry).toWKT(flatness) : null;
    }

    /**
     * Well Known Text (WKT) parsing not supported with Java2D.
     */
    @Override
    public Shape parseWKT(final String wkt) {
        throw new UnsupportedImplementationException(unsupported("parseWKT"));
    }

    /**
     * Well Known Binary (WKB) reading not supported with Java2D.
     */
    @Override
    public Shape parseWKB(ByteBuffer data) {
        throw new UnsupportedImplementationException(unsupported("parseWKB"));
    }
}
