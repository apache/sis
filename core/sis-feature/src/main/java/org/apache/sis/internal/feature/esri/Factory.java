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
package org.apache.sis.internal.feature.esri;

import java.nio.ByteBuffer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.OperatorImportFromWkb;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WkbImportFlags;
import com.esri.core.geometry.WktImportFlags;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.math.Vector;


/**
 * The factory of geometry objects backed by ESRI.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   0.7
 * @module
 */
public final class Factory extends Geometries<Geometry> {
    /**
     * The singleton instance of this factory.
     */
    public static final Factory INSTANCE = new Factory();

    /**
     * Creates the singleton instance.
     */
    private Factory() {
        super(GeometryLibrary.ESRI, Geometry.class, Point.class, Polyline.class, Polygon.class);
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    protected GeometryWrapper<Geometry> createWrapper(final Object geometry) {
        return new Wrapper((Geometry) geometry);
    }

    /**
     * Creates a two-dimensional point from the given coordinate.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        // Need to explicitly set z to NaN because default value is 0.
        return new Point(x, y, Double.NaN);
    }

    /**
     * Creates a polyline from the given coordinate values.
     * Each {@link Double#NaN} coordinate value starts a new path.
     *
     * @param  polygon      whether to return the path as a polygon instead than polyline.
     * @param  dimension    the number of dimensions ({@value #BIDIMENSIONAL} or {@value #TRIDIMENSIONAL}).
     * @param  coordinates  sequence of (x,y) or (x,y,z) tuples.
     * @throws UnsupportedOperationException if this operation is not implemented for the given number of dimensions.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final int dimension, final Vector... coordinates) {
        if (dimension != BIDIMENSIONAL) {
            throw new UnsupportedOperationException(unsupported(dimension));
        }
        boolean lineTo = false;
        final Polyline path = new Polyline();
        for (final Vector v : coordinates) {
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
        if (polygon) {
            final Polygon p = new Polygon();
            p.add(path, false);
            return p;
        }
        return path;
    }

    /**
     * Creates a multi-polygon from an array of geometries.
     * Callers must ensure that the given objects are ESRI geometries.
     *
     * @param  geometries  the polygons or linear rings to put in a multi-polygons.
     * @throws ClassCastException if an element in the array is not an ESRI geometry.
     */
    @Override
    public GeometryWrapper<Geometry> createMultiPolygon(final Object[] geometries) {
        final Polygon polygon = new Polygon();
        for (final Object geometry : geometries) {
            polygon.add((MultiPath) unwrap(geometry), false);
        }
        return new Wrapper(polygon);
    }

    /**
     * Parses the given Well Known Text (WKT).
     *
     * @param  wkt  the Well Known Text to parse.
     * @return the geometry object for the given WKT.
     */
    @Override
    public GeometryWrapper<Geometry> parseWKT(final String wkt) {
        return new Wrapper(OperatorImportFromWkt.local().execute(WktImportFlags.wktImportDefaults, Geometry.Type.Unknown, wkt, null));
    }

    /**
     * Reads the given Well Known Binary (WKB).
     *
     * @param  data  the sequence of bytes to parse.
     * @return the geometry object for the given WKB.
     */
    @Override
    public GeometryWrapper<Geometry> parseWKB(final ByteBuffer data) {
        return new Wrapper(OperatorImportFromWkb.local().execute(WkbImportFlags.wkbImportDefaults, Geometry.Type.Unknown, data, null));
    }
}
