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
package org.apache.sis.geometry.wrapper.j2d;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.io.ObjectStreamException;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.referencing.internal.shared.AbstractShape;
import org.apache.sis.referencing.internal.shared.ShapeUtilities;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.internal.shared.CollectionsExt;


/**
 * The factory of geometry objects backed by Java2D.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
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
     * Invoked at deserialization time for obtaining the unique instance of this {@code Factory} class.
     *
     * @return {@link #INSTANCE}.
     * @throws ObjectStreamException if the object state is invalid.
     */
    protected Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.JAVA2D, Shape.class, Point2D.class);
    }

    /**
     * Returns a wrapper for the given {@code <G>} or {@code GeometryWrapper} geometry.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null}.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    public GeometryWrapper castOrWrap(final Object geometry) {
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
    protected GeometryWrapper createWrapper(final Shape geometry) {
        return new Wrapper(geometry);
    }

    /**
     * Returns the geometry class of the given type.
     *
     * @param  type  type of geometry for which the class is desired.
     * @return implementation class for the geometry of the specified type.
     */
    @Override
    public Class<?> getGeometryClass(final GeometryType type) {
        return (type == GeometryType.POINT) ? Point2D.class : Shape.class;
    }

    /**
     * Returns the geometry type of the given implementation class.
     *
     * @param  type  class of geometry for which the type is desired.
     * @return implementation-neutral type for the geometry of the specified class.
     */
    @Override
    public GeometryType getGeometryType(final Class<?> type) {
        return Point2D.class.isAssignableFrom(type) ? GeometryType.POINT : GeometryType.GEOMETRY;
    }

    /**
     * Notifies that this library cannot create 2.5-dimensional objects.
     * Notifies that this library can create geometry objects backed by single-precision type.
     */
    @Override
    public boolean supports(final Capability feature) {
        return feature == Capability.SINGLE_PRECISION;
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
     * Creates a two-dimensional point from the given coordinate.
     * The <var>z</var> value is lost.
     */
    @Override
    public Object createPoint(final double x, final double y, final double z) {
        return new Point2D.Double(x, y);
    }

    /**
     * Creates a single point from the given coordinates with the given dimensions.
     * While the returned point is always two-dimensional, the buffer position is
     * advanced by the number specified of dimensions.
     *
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuple.
     * @param  coordinates  a (x,y), (x,y,z), (x,y,m) or (x,y,z,m) coordinate tuple.
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final boolean isFloat, final Dimensions dimensions, final DoubleBuffer coordinates) {
        final double x = coordinates.get();
        final double y = coordinates.get();
        coordinates.position(coordinates.position() + (dimensions.count - BIDIMENSIONAL));
        return isFloat ? new Point2D.Float((float) x, (float) y) : new Point2D.Double(x, y);
    }

    /**
     * Unsupported operation with Java2D.
     */
    @Override
    public Shape createMultiPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates) {
        throw new UnsupportedImplementationException(unsupported("createMultiPoint"));
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
     * @param  isFloat      whether to cast and store numbers to single-precision.
     * @param  dimensions   the dimensions of the coordinate tuples.
     * @param  coordinates  sequence of (x,y) coordinate tuples.
     */
    @Override
    public Shape createPolyline(final boolean polygon, final boolean isFloat,
            final Dimensions dimensions, final DoubleBuffer... coordinates)
    {
        int length = 0;
        for (final DoubleBuffer v : coordinates) {
            if (v != null) {
                length = Math.addExact(length, v.remaining());
            }
        }
        /*
         * Shortcut (for performance reason) when building a single line segment.
         * Note: Point2D is not an instance of Shape, so we cannot make a special case for it.
         */
        length /= BIDIMENSIONAL;
        if (length == 2 && coordinates.length == 1) {
            final DoubleBuffer v = coordinates[0];
            final double x1, y1, x2, y2;
            if (!Double.isNaN(x1 = v.get(0)) &&
                !Double.isNaN(y1 = v.get(1)) &&
                !Double.isNaN(x2 = v.get(dimensions.count)) &&
                !Double.isNaN(y2 = v.get(dimensions.count + 1)))
            {
                v.position(v.position() + dimensions.count * 2);
                final Line2D path = isFloat ? new Line2D.Float() : new Line2D.Double();
                path.setLine(x1, y1, x2, y2);
                return path;
            }
        }
        /*
         * General case if we could not use a shortcut.
         */
        final int skip = dimensions.count - BIDIMENSIONAL;
        final Path2D path = createPath(isFloat, length);
        boolean lineTo = false;
        for (final DoubleBuffer v : coordinates) {
            while (v.hasRemaining()) {
                final double x = v.get();
                final double y = v.get();
                v.position(v.position() + skip);
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
    public GeometryWrapper createMultiPolygon(final Object[] geometries) {
        if (geometries.length == 1) {
            return new Wrapper((Shape) implementation(geometries[0]));
        }
        final Shape[] shapes = new Shape[geometries.length];
        for (int i=0; i<geometries.length; i++) {
            shapes[i] = (Shape) implementation(geometries[i]);
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
        path.trimToSize();
        return new Wrapper(path);
    }

    /**
     * Creates a geometry from components.
     * The expected {@code components} type depends on the target geometry type:
     * <ul>
     *   <li>If {@code type} is a multi-geometry, then the components shall be an array of {@link Shape} elements.</li>
     *   <li>Otherwise, the components shall be an array or collection of {@link Point2D} instances.</li>
     * </ul>
     *
     * @param  type        type of geometry to create.
     * @param  components  the components. Valid classes depend on the type of geometry to create.
     * @return geometry built from the given components.
     * @throws ClassCastException if the given object is not an array or a collection of supported geometry components.
     */
    @Override
    public GeometryWrapper createFromComponents(final GeometryType type, final Object components) {
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
            if (type.isCollection) {
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
            path.trimToSize();
            geometry = path;
        }
        return new Wrapper(geometry);
    }

    /**
     * Well Known Text (WKT) parsing not supported with Java2D.
     */
    @Override
    public GeometryWrapper parseWKT(final String wkt) {
        throw new UnsupportedImplementationException(unsupported("parseWKT"));
    }

    /**
     * Well Known Binary (WKB) reading not supported with Java2D.
     */
    @Override
    public GeometryWrapper parseWKB(final ByteBuffer data) {
        throw new UnsupportedImplementationException(unsupported("parseWKB"));
    }
}
