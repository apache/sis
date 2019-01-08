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
 * Abstract class parent to all JTS geometry transformations.
 * This class decompose the geometry to it's most primitive element, the
 * CoordinateSequence, then rebuild the geometry.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class GeometryTransform {

    protected final GeometryFactory gf;
    protected final CoordinateSequenceFactory csf;

    public GeometryTransform() {
        this((CoordinateSequenceFactory) null);
    }

    public GeometryTransform(final CoordinateSequenceFactory csf) {
        if (csf == null) {
            this.gf = new GeometryFactory();
            this.csf = gf.getCoordinateSequenceFactory();
        } else {
            this.csf = csf;
            this.gf = new GeometryFactory(csf);
        }
    }

    public GeometryTransform(final GeometryFactory gf) {
        if (gf == null) {
            this.gf = new GeometryFactory();
            this.csf = gf.getCoordinateSequenceFactory();
        } else {
            this.csf = gf.getCoordinateSequenceFactory();
            this.gf = gf;
        }
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
            throw new IllegalArgumentException("Geometry type is unknowed or null : " + geom);
        }
    }

    protected Point transform(final Point geom) throws TransformException {
        final CoordinateSequence coord = geom.getCoordinateSequence();
        return gf.createPoint(transform(coord, 1));
    }

    protected MultiPoint transform(final MultiPoint geom) throws TransformException {
        final int nbGeom = geom.getNumGeometries();

        final Point[] subs = new Point[geom.getNumGeometries()];
        for (int i = 0; i < nbGeom; i++) {
            subs[i] = transform((Point) geom.getGeometryN(i));
        }
        return gf.createMultiPoint(subs);
    }

    protected LineString transform(final LineString geom) throws TransformException {
        final CoordinateSequence seq = transform(geom.getCoordinateSequence(), 2);
        return gf.createLineString(seq);
    }

    protected LinearRing transform(final LinearRing geom) throws TransformException {
        final CoordinateSequence seq = transform(geom.getCoordinateSequence(), 4);
        return gf.createLinearRing(seq);
    }

    protected MultiLineString transform(final MultiLineString geom) throws TransformException {
        final LineString[] subs = new LineString[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((LineString) geom.getGeometryN(i));
        }
        return gf.createMultiLineString(subs);
    }

    protected Polygon transform(final Polygon geom) throws TransformException {
        final LinearRing exterior = transform((LinearRing) geom.getExteriorRing());
        final LinearRing[] holes = new LinearRing[geom.getNumInteriorRing()];
        for (int i = 0; i < holes.length; i++) {
            holes[i] = transform((LinearRing) geom.getInteriorRingN(i));
        }
        return gf.createPolygon(exterior, holes);
    }

    protected MultiPolygon transform(final MultiPolygon geom) throws TransformException {
        final Polygon[] subs = new Polygon[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform((Polygon) geom.getGeometryN(i));
        }
        return gf.createMultiPolygon(subs);
    }

    protected GeometryCollection transform(final GeometryCollection geom) throws TransformException {
        final Geometry[] subs = new Geometry[geom.getNumGeometries()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = transform(geom.getGeometryN(i));
        }
        return gf.createGeometryCollection(subs);
    }

    /**
     *
     * @param sequence Sequence to transform
     * @param minpoints Minimum number of point to preserve
     * @return transformed sequence
     * @throws TransformException
     */
    protected abstract CoordinateSequence transform(CoordinateSequence sequence, int minpoints) throws TransformException;

}
