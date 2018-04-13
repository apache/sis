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

import java.util.Iterator;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Envelope;
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
     * Creates the singleton instance.
     */
    JTS() throws ClassNotFoundException, NoSuchMethodException {
        super(GeometryLibrary.JTS, Geometry.class, Point.class, LineString.class, Polygon.class);
    }

    /**
     * If the given object is a JTS geometry, returns a short string representation the class name.
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
        return null;   // TODO - see class javadoc
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(double x, double y) {
        throw unsupported(2);   // TODO - see class javadoc
    }

    /**
     * Creates a polyline from the given ordinate values.
     * Each {@link Double#NaN} ordinate value start a new path.
     * The implementation returned by this method must be an instance of {@link #rootClass}.
     */
    @Override
    public Geometry createPolyline(final int dimension, final Vector... ordinates) {
        // TODO - see class javadoc
        throw unsupported(dimension);
    }

    /**
     * Merges a sequence of points or paths if the first instance is an implementation of this library.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    final Geometry tryMergePolylines(final Object first, final Iterator<?> polylines) {
        throw unsupported(2);   // TODO - see class javadoc
    }

    /**
     * Parses the given WKT.
     */
    @Override
    public Object parseWKT(final String wkt) {
        throw unsupported(2);
    }
}
