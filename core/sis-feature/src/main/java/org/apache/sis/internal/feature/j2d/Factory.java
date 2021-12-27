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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.io.ObjectStreamException;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryType;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.referencing.j2d.AbstractShape;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.internal.util.CollectionsExt;
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
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2196319174347241786L;

    /**
     * The singleton instance of this factory.
     */
    public static final Factory INSTANCE = new Factory();

    /**
     * Invoked at deserialization time for obtaining the unique instance of this {@code Geometries} class.
     *
     * @return {@link #INSTANCE}.
     */
    @Override
    protected Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.JAVA2D, Shape.class, Point2D.class, Shape.class, Shape.class);
    }

    /**
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper<G>} geometry.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null}.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    public GeometryWrapper<Shape> castOrWrap(final Object geometry) {
        if (geometry == null || geometry instanceof Wrapper) {
            return (Wrapper) geometry;
        } else if (geometry instanceof PointWrapper) {
            return (PointWrapper) geometry;
        } else if (geometry instanceof Point2D) {
            return new PointWrapper((Point2D) geometry);
        } else {
            return new Wrapper((Shape) geometry);
        }
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     */
    @Override
    protected GeometryWrapper<Shape> createWrapper(final Shape geometry) {
        return new Wrapper(geometry);
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
    public Object createPoint(float x, float y) {
        return new Point2D.Float(x, y);
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return new Point2D.Double(x, y);
    }

    /**
     * Unsupported operation with Java2D.
     */
    @Override
    public Object createPoint(final double x, final double y, final double z) {
        throw new UnsupportedOperationException();
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
     * @param  polygon      whether to return the path as a polygon instead of polyline.
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
            if (!AbstractShape.isFloat(geometry)) {
                isFloat = false;
                break;
            }
        }
        final Path2D path = createPath(isFloat, 20);
        for (final Shape geometry : shapes) {
            path.append(geometry, false);
        }
        // path.trimToSize();       // TODO: uncomment with JDK10.
        return new Wrapper(path);
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depend on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be an array of {@link Shape} elements.</li>
     *   <li>Otherwise the components shall be an array or collection of {@link Point2D} instances.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public GeometryWrapper<Shape> createFromComponents(final GeometryType type, final Object components) {
        /*
         * No exhaustive `if (x instanceof y)` checks in this method.
         * `ClassCastException` shall be handled by the caller.
         */
        final Collection<?> data = (components instanceof Collection<?>)
                ? (Collection<?>) components : Arrays.asList((Object[]) components);
        /*
         * Java2D API does not distinguish between single geometry and geometry collection.
         * So if the number of components is 1, there is no reason to create a new geometry object.
         */
        Shape geometry = (Shape) CollectionsExt.singletonOrNull(data);
        if (geometry == null) {
            boolean isFloat = true;
            for (final Object component : data) {
                isFloat = AbstractShape.isFloat(component);
                if (!isFloat) break;
            }
            final Path2D path = createPath(isFloat, 20);
            if (type.isCollection()) {
                for (final Object component : data) {
                    path.append((Shape) component, false);
                }
            } else {
                final Iterator<?> it = data.iterator();
                if (it.hasNext()) {
                    Point2D p = (Point2D) it.next();
                    path.moveTo(p.getX(), p.getY());
                    while (it.hasNext()) {
                        p = (Point2D) it.next();
                        path.lineTo(p.getX(), p.getY());
                    }
                    if (type == GeometryType.POLYGON) {
                        path.closePath();
                    }
                }
            }
            // path.trimToSize();       // TODO: uncomment with JDK10.
            geometry = path;
        }
        return new Wrapper(geometry);
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
