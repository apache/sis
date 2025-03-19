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

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * An operation transforming a geometry into another geometry. This class decomposes the geometry into it's
 * most primitive elements, the {@link CoordinateSequence}, applies an operation, then rebuilds the geometry.
 * The operation may change coordinate values (for example a map projection), but not necessarily.
 * An operation could also be a clipping for example.
 *
 * @author  Johann Sorel (Geomatys)
 */
public abstract class GeometryTransform {
    /**
     * The factory to use for creating geometries.
     */
    private final GeometryFactory geometryFactory;

    /**
     * The factory to use for creating sequences of coordinate tuples.
     */
    protected final CoordinateSequenceFactory coordinateFactory;

    /**
     * Creates a new operation using the given factory.
     *
     * @param  factory  the factory to use for creating geometries. Shall not be null.
     */
    protected GeometryTransform(final GeometryFactory factory) {
        geometryFactory   = factory;
        coordinateFactory = factory.getCoordinateSequenceFactory();
    }

    /**
     * Transforms the given geometry. This method delegates to one of the {@code transform(…)} methods
     * based on the type of the given geometry.
     *
     * @param  geom  the geometry to transform.
     * @return the transformed geometry.
     * @throws TransformException if an error occurred while transforming the geometry.
     */
    public Geometry transform(final Geometry geom) throws TransformException {
        if (geom instanceof Point)              return transform((Point)              geom);
        if (geom instanceof MultiPoint)         return transform((MultiPoint)         geom);
        if (geom instanceof LinearRing)         return transform((LinearRing)         geom);    // Must be tested before LineString.
        if (geom instanceof LineString)         return transform((LineString)         geom);
        if (geom instanceof MultiLineString)    return transform((MultiLineString)    geom);
        if (geom instanceof Polygon)            return transform((Polygon)            geom);
        if (geom instanceof MultiPolygon)       return transform((MultiPolygon)       geom);
        if (geom instanceof GeometryCollection) return transform((GeometryCollection) geom);
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, Classes.getClass(geom)));
    }

    /**
     * Transforms the given point. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the point to transform.
     * @return the transformed point.
     * @throws TransformException if an error occurred while transforming the geometry.
     */
    public Point transform(final Point geom) throws TransformException {
        final CoordinateSequence coord = geom.getCoordinateSequence();
        return geometryFactory.createPoint(transform(coord, 1));
    }

    /**
     * Transforms the given points. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the points to transform.
     * @return the transformed points.
     * @throws TransformException if an error occurred while transforming a geometry.
     */
    public MultiPoint transform(final MultiPoint geom) throws TransformException {
        final var subs = new Point[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((Point) geom.getGeometryN(i));
        }
        return geometryFactory.createMultiPoint(subs);
    }

    /**
     * Transforms the given line string. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the line string to transform.
     * @return the transformed line string.
     * @throws TransformException if an error occurred while transforming the geometry.
     */
    public LineString transform(final LineString geom) throws TransformException {
        final CoordinateSequence seq = transform(geom.getCoordinateSequence(), 2);
        return geometryFactory.createLineString(seq);
    }

    /**
     * Transforms the given line strings. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the line strings to transform.
     * @return the transformed line strings.
     * @throws TransformException if an error occurred while transforming a geometry.
     */
    public MultiLineString transform(final MultiLineString geom) throws TransformException {
        final var subs = new LineString[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((LineString) geom.getGeometryN(i));
        }
        return geometryFactory.createMultiLineString(subs);
    }

    /**
     * Transforms the given linear ring. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the linear ring to transform.
     * @return the transformed linear ring.
     * @throws TransformException if an error occurred while transforming the geometry.
     */
    public LinearRing transform(final LinearRing geom) throws TransformException {
        final CoordinateSequence seq = transform(geom.getCoordinateSequence(), 4);
        return geometryFactory.createLinearRing(seq);
    }

    /**
     * Transforms the given polygon. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the polygon to transform.
     * @return the transformed polygon.
     * @throws TransformException if an error occurred while transforming the geometry.
     */
    public Polygon transform(final Polygon geom) throws TransformException {
        final LinearRing exterior = transform(geom.getExteriorRing());
        final var holes = new LinearRing[geom.getNumInteriorRing()];
        for (int i = 0; i < holes.length; i++) {
            holes[i] = transform(geom.getInteriorRingN(i));
        }
        return geometryFactory.createPolygon(exterior, holes);
    }

    /**
     * Transforms the given polygons. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the polygons to transform.
     * @return the transformed polygons.
     * @throws TransformException if an error occurred while transforming a geometry.
     */
    public MultiPolygon transform(final MultiPolygon geom) throws TransformException {
        final var subs = new Polygon[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((Polygon) geom.getGeometryN(i));
        }
        return geometryFactory.createMultiPolygon(subs);
    }

    /**
     * Transforms the given geometries. Can be invoked directly if the type is known at compile-time,
     * or indirectly through a call to the more generic {@link #transform(Geometry)} method.
     *
     * @param  geom  the geometries to transform.
     * @return the transformed geometries.
     * @throws TransformException if an error occurred while transforming a geometry.
     */
    public GeometryCollection transform(final GeometryCollection geom) throws TransformException {
        final var subs = new Geometry[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform(geom.getGeometryN(i));
        }
        return geometryFactory.createGeometryCollection(subs);
    }

    /**
     * Transforms the given sequence of coordinate tuples, producing a new sequence of tuples.
     *
     * @param  sequence   sequence of coordinate tuples to transform.
     * @param  minPoints  minimum number of points to preserve.
     * @return the transformed sequence of coordinate tuples.
     * @throws TransformException if an error occurred while transforming a tuple.
     */
    protected abstract CoordinateSequence transform(CoordinateSequence sequence, int minPoints) throws TransformException;
}
