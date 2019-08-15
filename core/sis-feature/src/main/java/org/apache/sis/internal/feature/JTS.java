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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.ParseException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Classes;


/**
 * Centralizes some usages of JTS geometry API by Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
final class JTS extends Geometries<Geometry> {
    /**
     * The factory to use for creating JTS geometries. Currently set to a factory using
     * double-precision floating point numbers and a spatial-reference ID of 0.
     */
    private final GeometryFactory factory;

    /**
     * Creates the singleton instance.
     */
    JTS() {
        super(GeometryLibrary.JTS, Geometry.class, Point.class, LineString.class, Polygon.class);
        factory = new GeometryFactory();            // Default to double precision and SRID of 0.
    }

    /**
     * Parses the given WKT.
     *
     * @return the geometry object for the given WKT.
     * @throws ParseException if the WKT can not be parsed.
     */
    @Override
    public Object parseWKT(final String wkt) throws ParseException {
        return new WKTReader(factory).read(wkt);
    }

    /**
     * If the given object is a JTS geometry, returns its WKT representation.
     */
    @Override
    final String tryFormatWKT(final Object geometry, final double flatness) {
        return (geometry instanceof Geometry) ? ((Geometry) geometry).toText() : null;
    }

    /**
     * If the given object is a JTS geometry, returns a short string representation of the class name.
     */
    @Override
    final String tryGetLabel(Object geometry) {
        return (geometry instanceof Geometry) ? Classes.getShortClassName(geometry) : null;
    }

    /**
     * If the given object is a JTS geometry and its envelope is non-empty, returns
     * that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given object, or {@code null} if the object is not
     *         a recognized geometry or its envelope is empty.
     */
    @Override
    final GeneralEnvelope tryGetEnvelope(final Object geometry) {
        if (geometry instanceof Geometry) {
            final Envelope bounds = ((Geometry) geometry).getEnvelopeInternal();
            final GeneralEnvelope env = new GeneralEnvelope(2);
            env.setRange(0, bounds.getMinX(), bounds.getMaxX());
            env.setRange(1, bounds.getMinY(), bounds.getMaxY());
            if (!env.isEmpty()) {
                return env;
            }
        }
        return null;
    }

    /**
     * If the given point is an implementation of this library, returns its coordinate.
     * Otherwise returns {@code null}. If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    final double[] tryGetCoordinate(final Object point) {
        final Coordinate pt;
        if (point instanceof Point) {
            pt = ((Point) point).getCoordinate();
        } else if (point instanceof Coordinate) {
            pt = (Coordinate) point;
        } else {
            return null;
        }
        final double z = pt.getZ();
        final double[] coord;
        if (Double.isNaN(z)) {
            coord = new double[2];
        } else {
            coord = new double[3];
            coord[2] = z;
        }
        coord[1] = pt.y;
        coord[0] = pt.x;
        return coord;
    }

    /**
     * If the given geometry is an implementation of this library, returns its coordinate reference system.
     * Otherwise returns {@code null}.
     *
     * @see #tryTransform(Object, CoordinateOperation, CoordinateReferenceSystem)
     */
    @Override
    final CoordinateReferenceSystem tryGetCoordinateReferenceSystem(final Object geometry) throws FactoryException {
        if (geometry instanceof Geometry) {
            return org.apache.sis.internal.feature.jts.JTS.getCoordinateReferenceSystem((Geometry) geometry);
        } else {
            return super.tryGetCoordinateReferenceSystem(geometry);
        }
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     *
     * @return the point for the given coordinate values.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return factory.createPoint(new Coordinate(x, y));
    }

    /**
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     *
     * @return the geometric object for the given points.
     */
    @Override
    public Geometry createPolyline(final int dimension, final Vector... coords) {
        final boolean is3D = (dimension == 3);
        if (!is3D && dimension != 2) {
            throw unsupported(dimension);
        }
        final List<Coordinate> coordinates = new ArrayList<>(32);
        final List<LineString> lines = new ArrayList<>();
        for (final Vector v : coords) {
            if (v != null) {
                final int size = v.size();
                for (int i=0; i<size;) {
                    final double x = v.doubleValue(i++);
                    final double y = v.doubleValue(i++);
                    if (!Double.isNaN(x) && !Double.isNaN(y)) {
                        final Coordinate c;
                        if (is3D) {
                            c = new Coordinate(x, y, v.doubleValue(i++));
                        } else {
                            c = new Coordinate(x, y);
                        }
                        coordinates.add(c);
                    } else {
                        if (is3D) i++;
                        toLineString(coordinates, lines);
                        coordinates.clear();
                    }
                }
            }
        }
        toLineString(coordinates, lines);
        return toGeometry(lines);
    }

    /**
     * Makes a line string or linear ring from the given coordinates, and add the line string to the given list.
     * If the given coordinates array is empty, then this method does nothing.
     * This method does not modify the given coordinates list.
     */
    private void toLineString(final List<Coordinate> coordinates, final List<LineString> addTo) {
        final int s = coordinates.size();
        if (s >= 2) {
            final LineString line;
            final Coordinate[] ca = coordinates.toArray(new Coordinate[s]);
            if (ca[0].equals2D(ca[s-1])) {
                line = factory.createLinearRing(ca);        // Throws an exception if s < 4.
            } else {
                line = factory.createLineString(ca);        // Throws an exception if contains duplicated point.
            }
            addTo.add(line);
        }
    }

    /**
     * Returns the given list of line string as a single geometry.
     */
    private Geometry toGeometry(final List<LineString> lines) {
        final int s = lines.size();
        switch (s) {
            case 0:  return factory.createLinearRing((Coordinate[]) null);      // Creates an empty linear ring.
            case 1:  return lines.get(0);
            default: return factory.createMultiLineString(lines.toArray(new LineString[s]));
        }
    }

    /**
     * Merges a sequence of points or paths if the first instance is an implementation of this library.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    final Geometry tryMergePolylines(Object next, final Iterator<?> polylines) {
        if (!(next instanceof MultiLineString || next instanceof LineString || next instanceof Point)) {
            return null;
        }
        final List<Coordinate> coordinates = new ArrayList<>();
        final List<LineString> lines = new ArrayList<>();
add:    for (;;) {
            if (next instanceof Point) {
                final Coordinate pt = ((Point) next).getCoordinate();
                if (!Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                    coordinates.add(pt);
                } else {
                    toLineString(coordinates, lines);
                    coordinates.clear();
                }
            } else {
                final Geometry g = (Geometry) next;
                final int n = g.getNumGeometries();
                for (int i=0; i<n; i++) {
                    final LineString ls = (LineString) g.getGeometryN(i);
                    if (coordinates.isEmpty()) {
                        lines.add(ls);
                    } else {
                        coordinates.addAll(Arrays.asList(ls.getCoordinates()));
                        toLineString(coordinates, lines);
                        coordinates.clear();
                    }
                }
            }
            /*
             * 'polylines.hasNext()' check is conceptually part of 'for' instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = polylines.next()) == null);
        }
        toLineString(coordinates, lines);
        return toGeometry(lines);
    }

    /**
     * Tries to transforms the given geometry to the specified Coordinate Reference System (CRS),
     * or returns {@code null} if this method can not perform this operation on the given object.
     * Exactly one of {@code operation} and {@code targetCRS} shall be non-null.
     *
     * @param  geometry   the geometry to transform.
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     *
     * @see #tryGetCoordinateReferenceSystem(Object)
     */
    @Override
    Geometry tryTransform(final Object geometry, final CoordinateOperation operation, final CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException
    {
        if (geometry instanceof Geometry) {
            if (operation != null) {
                return org.apache.sis.internal.feature.jts.JTS.transform((Geometry) geometry, operation);
            } else {
                return org.apache.sis.internal.feature.jts.JTS.transform((Geometry) geometry, targetCRS);
            }
        }
        return null;
    }
}
