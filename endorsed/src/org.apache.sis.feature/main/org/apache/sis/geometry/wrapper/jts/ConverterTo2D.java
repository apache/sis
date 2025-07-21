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

import java.lang.reflect.Array;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.util.resources.Errors;


/**
 * Converts a geometry from 3D to 2D coordinate tuples.
 * <abbr>JTS</abbr> tends to expend the dimension of {@link CoordinateSequence} from 2D to 3D
 * with the addition of a <var>z</var> coordinate initialized to {@link java.lang.Double#NaN}.
 * Since some operations do not want any <var>z</var> coordinates, this class is the base class
 * for any operation that needs to check for the presence of <var>z</var> values and remove them.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
class ConverterTo2D {
    /**
     * Number of dimensions of geometries built by this class.
     */
    protected static final int DIMENSION = Factory.BIDIMENSIONAL;

    /**
     * The <abbr>JTS</abbr> factory for creating geometry. May be user-specified.
     * Note that the {@link org.locationtech.jts.geom.CoordinateSequenceFactory} is ignored.
     */
    protected final GeometryFactory factory;

    /**
     * Creates a new converter from 3D to 2D geometries.
     *
     * @param  factory  the <abbr>JTS</abbr> factory for creating geometry, or {@code null} for automatic.
     * @param  isFloat  whether to store coordinates as {@code float} instead of {@code double}.
     */
    protected ConverterTo2D(final GeometryFactory factory, final boolean isFloat) {
        this.factory = (factory != null) ? factory : Factory.INSTANCE.factory(isFloat);
    }

    /**
     * Creates a two-dimensional copy of the given geometry if not already 2D.
     * This is the general version of {@code enforce2D(…)} for geometries of type unknown at compile time.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     * @throws IllegalArgumentException if the given geometry is an instance of an unsupported class.
     */
    protected final Geometry anyTo2D(final Geometry geometry) {
        if (geometry instanceof Point)              return enforce2D((Point)              geometry);
        if (geometry instanceof LineString)         return enforce2D((LineString)         geometry);
        if (geometry instanceof LinearRing)         return enforce2D((LinearRing)         geometry);
        if (geometry instanceof Polygon)            return enforce2D((Polygon)            geometry);
        if (geometry instanceof MultiPoint)         return enforce2D((MultiPoint)         geometry);
        if (geometry instanceof MultiLineString)    return enforce2D((MultiLineString)    geometry);
        if (geometry instanceof MultiPolygon)       return enforce2D((MultiPolygon)       geometry);
        if (geometry instanceof GeometryCollection) return collect2D((GeometryCollection) geometry);
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, geometry.getGeometryType()));
    }

    /**
     * Creates a two-dimensional copy of the given geometry collection if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final GeometryCollection collect2D(final GeometryCollection geometry) {
        return enforce2D(geometry, Geometry.class, this::anyTo2D, GeometryFactory::createGeometryCollection);
    }

    /**
     * Creates a two-dimensional copy of the given multi-points if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final MultiPoint enforce2D(final MultiPoint geometry) {
        return enforce2D(geometry, Point.class, this::enforce2D, GeometryFactory::createMultiPoint);
    }

    /**
     * Creates a two-dimensional copy of the given point if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final Point enforce2D(final Point geometry) {
        return enforce2D(geometry, geometry.getCoordinateSequence(), GeometryFactory::createPoint);
    }

    /**
     * Creates a two-dimensional copy of the given multi-line-strings if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final MultiLineString enforce2D(final MultiLineString geometry) {
        return enforce2D(geometry, LineString.class, this::enforce2D, GeometryFactory::createMultiLineString);
    }

    /**
     * Creates a two-dimensional copy of the given line-string if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final LineString enforce2D(final LineString geometry) {
        return enforce2D(geometry, geometry.getCoordinateSequence(), GeometryFactory::createLineString);
    }

    /**
     * Creates a two-dimensional copy of the given line-ring if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final LinearRing enforce2D(final LinearRing geometry) {
        return enforce2D(geometry, geometry.getCoordinateSequence(), GeometryFactory::createLinearRing);
    }

    /**
     * Creates a two-dimensional copy of the given multi-polygons if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final MultiPolygon enforce2D(final MultiPolygon geometry) {
        return enforce2D(geometry, Polygon.class, this::enforce2D, GeometryFactory::createMultiPolygon);
    }

    /**
     * Creates a two-dimensional copy of the given polygon if not already 2D.
     *
     * @param  geometry  the geometry to force to a two-dimensional geometry.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    protected final Polygon enforce2D(final Polygon geometry) {
        LinearRing exterior = geometry.getExteriorRing();
        boolean changed = (exterior != (exterior = enforce2D(exterior)));
        final var rings = new LinearRing[geometry.getNumInteriorRing()];
        for (int i = 0; i < rings.length; i++) {
            final LinearRing interior = geometry.getInteriorRingN(i);
            changed |= (rings[i] = enforce2D(interior)) != interior;
        }
        return changed ? factory.createPolygon(exterior, rings) : geometry;
    }

    /**
     * Creates a two-dimensional copy of the given geometry collection if not already 2D.
     * This is a helper method for {@code enforce2D(…)} implementations.
     *
     * @param <G>            the type of the geometry collection.
     * @param <E>            the type of all components in the geometry collection.
     * @param collection     the geometry collection to eventually copy.
     * @param componentType  the type of all components in the geometry collection.
     * @param toComponent2D  the method to invoke for enforcing a component to two dimensions.
     * @param creator        the method to invoke for recreating a collection from the components.
     * @return a two-dimension copy of the given collection, or directly {@code collection} if it was already 2D.
     */
    private <G extends GeometryCollection, E extends Geometry> G enforce2D(
            final G collection,
            final Class<E> componentType,
            final UnaryOperator<E> toComponent2D,
            final BiFunction<GeometryFactory, E[], G> creator)
    {
        boolean changed = false;
        @SuppressWarnings("unchecked")
        final E[] components = (E[]) Array.newInstance(componentType, collection.getNumGeometries());
        for (int i = 0; i < components.length; i++) {
            final E component = componentType.cast(collection.getGeometryN(i));
            changed |= (components[i] = toComponent2D.apply(component)) != component;
        }
        return changed ? creator.apply(factory, components) : collection;
    }

    /**
     * Creates a two-dimensional copy of the given geometry if not already 2D.
     * This is a helper method for {@code enforce2D(…)} implementations.
     *
     * @param  <G>       the type of the geometry argument.
     * @param  geometry  the geometry to eventually copy.
     * @param  cs        the coordinate sequence of the geometry.
     * @param  creator   the factory method to invoke if the geometry needs to be recreated.
     * @return a two-dimension copy of the given geometry, or directly {@code geometry} if it was already 2D.
     */
    private <G extends Geometry> G enforce2D(final G geometry, final CoordinateSequence cs,
                                             final BiFunction<GeometryFactory, CoordinateSequence, G> creator)
    {
        if (cs.getDimension() == DIMENSION) {
            return geometry;
        }
        final int size = cs.size();
        final CoordinateSequence copy = factory.getCoordinateSequenceFactory().create(size, 2);
        for (int i = 0; i < size; i++) {
            copy.setOrdinate(i, 0, cs.getX(i));
            copy.setOrdinate(i, 1, cs.getY(i));
        }
        return creator.apply(factory, copy);
    }
}
