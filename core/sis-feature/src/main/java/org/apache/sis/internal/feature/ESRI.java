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
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Point;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.math.Vector;


/**
 * Centralizes some usages of ESRI geometry API by Apache SIS.
 * We use this class for isolating dependencies from the {@code org.apache.feature} package
 * to ESRI's API or to Java Topology Suite (JTS) API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
final class ESRI extends Geometries<Geometry> {
    /**
     * Creates the singleton instance.
     */
    ESRI() {
        super(GeometryLibrary.ESRI, Geometry.class, Point.class, Polyline.class, Polygon.class);
    }

    /**
     * If the given object is an ESRI geometry and its envelope is non-empty, returns
     * that envelope as an Apache SIS implementation. Otherwise returns {@code null}.
     *
     * @param  geometry  the geometry from which to get the envelope, or {@code null}.
     * @return the envelope of the given object, or {@code null} if the object is not
     *         a recognized geometry or its envelope is empty.
     */
    @Override
    final GeneralEnvelope tryGetEnvelope(final Object geometry) {
        if (geometry instanceof Geometry) {
            final Envelope2D bounds = new Envelope2D();
            ((Geometry) geometry).queryEnvelope2D(bounds);
            if (!bounds.isEmpty()) {                                     // Test if there is NaN values.
                final GeneralEnvelope env = new GeneralEnvelope(2);
                env.setRange(0, bounds.xmin, bounds.xmax);
                env.setRange(1, bounds.ymin, bounds.ymax);
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
        if (point instanceof Point) {
            final Point pt = (Point) point;
            final double z = pt.getZ();
            final double[] coord;
            if (Double.isNaN(z)) {
                coord = new double[2];
            } else {
                coord = new double[3];
                coord[2] = z;
            }
            coord[1] = pt.getY();
            coord[0] = pt.getX();
            return coord;
        }
        return null;
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(double x, double y) {
        return new Point(x, y, Double.NaN);
    }

    /**
     * Creates a polyline from the given ordinate values.
     * Each {@link Double#NaN} ordinate value start a new path.
     * The implementation returned by this method must be an instance of {@link #rootClass}.
     */
    @Override
    public Geometry createPolyline(final int dimension, final Vector... ordinates) {
        if (dimension != 2) {
            throw unsupported(dimension);
        }
        boolean lineTo = false;
        final Polyline path = new Polyline();
        for (final Vector v : ordinates) {
            if (v != null) {
                final int size = v.size();
                for (int i=0; i<size;) {
                    final double x = v.doubleValue(i++);
                    final double y = v.doubleValue(i++);
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        lineTo = false;
                    } else if (lineTo) {
                        path.lineTo(x, y);
                    } else {
                        path.startPath(x, y);
                        lineTo = true;
                    }
                }
            }
        }
        return path;
    }

    /**
     * Merges a sequence of points or paths if the first instance is an implementation of this library.
     *
     * @throws ClassCastException if an element in the iterator is not a JTS geometry.
     */
    @Override
    final Geometry tryMergePolylines(Object next, final Iterator<?> polylines) {
        if (!(next instanceof MultiPath || next instanceof Point)) {
            return null;
        }
        final Polyline path = new Polyline();
        boolean lineTo = false;
        for (;; next = polylines.next()) {
            if (next != null) {
                if (next instanceof Point) {
                    final double x = ((Point) next).getX();
                    final double y = ((Point) next).getY();
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        lineTo = false;
                    } else if (lineTo) {
                        path.lineTo(x, y);
                    } else {
                        path.startPath(x, y);
                        lineTo = true;
                    }
                } else {
                    path.add((MultiPath) next, false);
                    lineTo = false;
                }
            }
            if (!polylines.hasNext()) {         // Should be part of the 'for' instruction, but we need
                break;                          // to skip this condition during the first iteration.
            }
        }
        return path;
    }
}
