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
package org.apache.sis.internal.feature.jts;

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


/**
 * An operation transforming a geometry into another geometry. This class decomposes the geometry into it's
 * most primitive elements, the {@link CoordinateSequence}, applies an operation, then rebuilds the geometry.
 * The operation may change coordinate values (for example a map projection), but not necessarily. An operation
 * could also be a clipping for example.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
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

    protected GeometryTransform() {
        this((GeometryFactory) null);
    }

    protected GeometryTransform(final CoordinateSequenceFactory factory) {
        if (factory == null) {
            geometryFactory   = new GeometryFactory();
            coordinateFactory = geometryFactory.getCoordinateSequenceFactory();
        } else {
            coordinateFactory = factory;
            geometryFactory   = new GeometryFactory(factory);
        }
    }

    protected GeometryTransform(GeometryFactory factory) {
        if (factory == null) {
            factory = new GeometryFactory();
        }
        geometryFactory   = factory;
        coordinateFactory = factory.getCoordinateSequenceFactory();
    }

    public Geometry transform(final Geometry geom) throws TransformException {
        if (geom instanceof Point) {
            return transform((Point) geom);
        } else if (geom instanceof MultiPoint) {
            return transform((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            return transform((LineString) geom);
        } else if (geom instanceof LinearRing) {
            return transform((LinearRing) geom);
        } else if (geom instanceof MultiLineString) {
            return transform((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            return transform((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            return transform((MultiPolygon) geom);
        } else if (geom instanceof GeometryCollection) {
            return transform((GeometryCollection) geom);
        } else {
            throw new IllegalArgumentException("Geometry type is unknown or null: " + geom);
        }
    }

    protected Point transform(final Point geom) throws TransformException {
        final CoordinateSequence coord = geom.getCoordinateSequence();
        return geometryFactory.createPoint(transform(coord, 1));
    }

    protected MultiPoint transform(final MultiPoint geom) throws TransformException {
        final int nbGeom = geom.getNumGeometries();
        final Point[] subs = new Point[geom.getNumGeometries()];
        for (int i = 0; i < nbGeom; i++) {
            subs[i] = transform((Point) geom.getGeometryN(i));
        }
        return geometryFactory.createMultiPoint(subs);
    }

    protected LineString transform(final LineString geom) throws TransformException {
        final CoordinateSequence seq = transform(geom.getCoordinateSequence(), 2);
        return geometryFactory.createLineString(seq);
    }

    protected LinearRing transform(final LinearRing geom) throws TransformException {
        final CoordinateSequence seq = transform(geom.getCoordinateSequence(), 4);
        return geometryFactory.createLinearRing(seq);
    }

    protected MultiLineString transform(final MultiLineString geom) throws TransformException {
        final LineString[] subs = new LineString[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((LineString) geom.getGeometryN(i));
        }
        return geometryFactory.createMultiLineString(subs);
    }

    protected Polygon transform(final Polygon geom) throws TransformException {
        final LinearRing exterior = transform((LinearRing) geom.getExteriorRing());
        final LinearRing[] holes = new LinearRing[geom.getNumInteriorRing()];
        for (int i = 0; i < holes.length; i++) {
            holes[i] = transform((LinearRing) geom.getInteriorRingN(i));
        }
        return geometryFactory.createPolygon(exterior, holes);
    }

    protected MultiPolygon transform(final MultiPolygon geom) throws TransformException {
        final Polygon[] subs = new Polygon[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((Polygon) geom.getGeometryN(i));
        }
        return geometryFactory.createMultiPolygon(subs);
    }

    protected GeometryCollection transform(final GeometryCollection geom) throws TransformException {
        final Geometry[] subs = new Geometry[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform(geom.getGeometryN(i));
        }
        return geometryFactory.createGeometryCollection(subs);
    }

    /**
     * Transforms the given sequence of coordinate tuples, producing a new sequence of tuples.
     *
     * @param  sequence   sequence of coordinate tuples to transform.
     * @param  minpoints  minimum number of points to preserve.
     * @return the transformed sequence of coordinate tuples.
     * @throws TransformException if an error occurred while transforming a tuple.
     */
    protected abstract CoordinateSequence transform(CoordinateSequence sequence, int minpoints) throws TransformException;
}
