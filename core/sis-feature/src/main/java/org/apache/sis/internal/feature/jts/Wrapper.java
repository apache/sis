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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.Debug;


/**
 * The wrapper of Java Topology Suite (JTS) geometries.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Wrapper extends GeometryWrapper<Geometry> {
    /**
     * The wrapped implementation.
     */
    private final Geometry geometry;

    /**
     * Creates a new wrapper around the given geometry.
     */
    Wrapper(final Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    public Geometries<Geometry> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    public Object implementation() {
        return geometry;
    }

    /**
     * Returns the geometry coordinate reference system, or {@code null} if none.
     *
     * @return the coordinate reference system, or {@code null} if none.
     * @throws BackingStoreException if the CRS can not be created from the SRID code.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        try {
            return JTS.getCoordinateReferenceSystem(geometry);
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Sets the coordinate reference system. This method overwrites any previous user object.
     * This is okay for the context in which Apache SIS uses this method, which is only for
     * newly created geometries.
     */
    @Override
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        JTS.setCoordinateReferenceSystem(geometry, crs);
    }

    /**
     * If the JTS geometry is non-empty, returns its envelope as an Apache SIS implementation.
     * Otherwise returns {@code null}.
     *
     * @return the envelope, or {@code null} if empty.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final Envelope bounds = geometry.getEnvelopeInternal();
        final GeneralEnvelope env = new GeneralEnvelope(Factory.BIDIMENSIONAL);
        env.setRange(0, bounds.getMinX(), bounds.getMaxX());
        env.setRange(1, bounds.getMinY(), bounds.getMaxY());
        return env.isEmpty() ? null : env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final Coordinate c = geometry.getCentroid().getCoordinate();
        final double z = c.getZ();
        if (Double.isNaN(z)) {
            return new DirectPosition2D(c.x, c.y);
        } else {
            return new GeneralDirectPosition(c.x, c.y, z);
        }
    }

    /**
     * Returns the centroid of the wrapped geometry as a JTS object.
     */
    @Override
    public Object getCentroidImpl() {
        final Point centroid = geometry.getCentroid();
        Factory.copyMetadata(geometry, centroid);
        return centroid;
    }

    /**
     * If the wrapped geometry is a point, returns its coordinates. Otherwise returns {@code null}.
     * If non-null, the returned array may have a length of 2 or 3.
     */
    @Override
    public double[] getPointCoordinates() {
        if (!(geometry instanceof Point)) {
            return null;
        }
        final Coordinate pt = ((Point) geometry).getCoordinate();
        final double z = pt.getZ();
        final double[] coord;
        if (Double.isNaN(z)) {
            coord = new double[Factory.BIDIMENSIONAL];
        } else {
            coord = new double[Factory.TRIDIMENSIONAL];
            coord[2] = z;
        }
        coord[1] = pt.y;
        coord[0] = pt.x;
        return coord;
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    protected double[] getAllCoordinates() {
        final Coordinate[] points = geometry.getCoordinates();
        final double[] coordinates = new double[points.length * Factory.BIDIMENSIONAL];
        int i = 0;
        for (final Coordinate p : points) {
            coordinates[i++] = p.x;
            coordinates[i++] = p.y;
        }
        return coordinates;
    }

    /**
     * Merges a sequence of points or paths after the wrapped geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    protected Geometry mergePolylines(final Iterator<?> polylines) {
        final List<Coordinate> coordinates = new ArrayList<>();
        final List<Geometry> lines = new ArrayList<>();
add:    for (Geometry next = geometry;;) {
            if (next instanceof Point) {
                final Coordinate pt = ((Point) next).getCoordinate();
                if (!Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                    coordinates.add(pt);
                } else {
                    Factory.INSTANCE.toLineString(coordinates, lines, false);
                    coordinates.clear();
                }
            } else {
                final int n = next.getNumGeometries();
                for (int i=0; i<n; i++) {
                    final LineString ls = (LineString) next.getGeometryN(i);
                    if (coordinates.isEmpty()) {
                        lines.add(ls);
                    } else {
                        coordinates.addAll(Arrays.asList(ls.getCoordinates()));
                        Factory.INSTANCE.toLineString(coordinates, lines, false);
                        coordinates.clear();
                    }
                }
            }
            /*
             * 'polylines.hasNext()' check is conceptually part of 'for' instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = (Geometry) polylines.next()) == null);
        }
        Factory.INSTANCE.toLineString(coordinates, lines, false);
        return Factory.INSTANCE.toGeometry(lines, false);
    }

    /**
     * Computes the geometry buffer.
     */
    @Override
    public Geometry buffer(final double distance) {
        final Geometry buffer = geometry.buffer(distance);
        Factory.copyMetadata(geometry, buffer);
        return buffer;
    }

    /**
     * Transforms this geometry using the given coordinate operation.
     * If the operation is null, then the geometry is returned unchanged.
     * If the source CRS is not equals to the geometry CRS, a new operation is inferred.
     *
     * @param  operation  the coordinate operation to apply, or {@code null}.
     * @return the transformed geometry, or the same geometry if it is already in target CRS.
     * @throws FactoryException if transformation to the target CRS can not be found.
     * @throws TransformException if the given geometry can not be transformed.
     */
    @Override
    public Geometry transform(final CoordinateOperation operation) throws FactoryException, TransformException {
        return JTS.transform(geometry, operation);
    }

    /**
     * Transforms this geometry to the specified Coordinate Reference System (CRS).
     * If the given CRS is null, the geometry is returned unchanged.
     * If the geometry has no Coordinate Reference System, a {@link TransformException} is thrown.
     *
     * @param  targetCRS  the target coordinate reference system, or {@code null}.
     * @return the transformed geometry (may be the same geometry instance), or {@code null}.
     * @throws TransformException if the given geometry has no CRS or can not be transformed.
     */
    @Override
    public GeometryWrapper<Geometry> transform(final CoordinateReferenceSystem targetCRS) throws TransformException {
        try {
            return new Wrapper(JTS.transform(geometry, targetCRS));
        } catch (FactoryException e) {
            throw new TransformException(e);
        }
    }

    /**
     * Returns the WKT representation of the wrapped geometry.
     */
    @Override
    public String formatWKT(final double flatness) {
        return geometry.toText();
    }
}
