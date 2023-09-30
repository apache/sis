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
package org.apache.sis.geometry.wrapper;

import java.nio.ByteBuffer;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.math.Vector;


/**
 * The factory of geometry objects backed by GeoAPI geometry interfaces.
 * This implementation relies on the fact that all {@link GeometryWrapper} implement {@link Geometry}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param   <G>  the base class of geometry objects in the backing implementation.
 */
final class StandardGeometries<G> extends Geometries<Geometry> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 356933858560693015L;

    /**
     * The factory used for implementations.
     */
    private final Geometries<G> implementation;

    /**
     * Creates a new factory backed by the given implementation.
     */
    StandardGeometries(final Geometries<G> implementation) {
        super(GeometryLibrary.GEOAPI, Geometry.class, Geometry.class, Geometry.class, Geometry.class);
        this.implementation = implementation;
    }

    /**
     * Returns the geometry object to return to the user in public API.
     *
     * @param  wrapper  the wrapper for which to get the geometry.
     * @return the GeoAPI geometry instance.
     */
    @Override
    public Object getGeometry(GeometryWrapper wrapper) {
        return wrapper;
    }

    /**
     * Returns a wrapper for the given geometry or wrapper.
     *
     * @param  geometry  the geometry instance to wrap (can be {@code null}).
     * @return a wrapper for the given geometry implementation, or {@code null}.
     * @throws ClassCastException if the given geometry is not an instance of valid type.
     */
    @Override
    public GeometryWrapper castOrWrap(final Object geometry) {
        return implementation.castOrWrap(geometry);
    }

    /**
     * Creates a wrapper for the given geometry instance.
     *
     * @param  geometry  the geometry to wrap.
     * @return wrapper for the given geometry.
     */
    @Override
    protected GeometryWrapper createWrapper(final Geometry geometry) {
        return implementation.castOrWrap(geometry);
    }

    /**
     * Returns whether this library can produce geometry backed by the {@code float} primitive type.
     */
    @Override
    public boolean supportSinglePrecision() {
        return implementation.supportSinglePrecision();
    }

    /**
     * Creates a two-dimensional point from the given coordinates.
     */
    @Override
    public Object createPoint(final float x, final float y) {
        return implementation.createPoint(x, y);
    }

    /**
     * Creates a two-dimensional point from the given coordinates.
     */
    @Override
    public Object createPoint(final double x, final double y) {
        return implementation.createPoint(x, y);
    }

    /**
     * Creates a three-dimensional point from the given coordinates.
     */
    @Override
    public Object createPoint(final double x, final double y, final double z) {
        return implementation.createPoint(x, y, z);
    }

    /**
     * Creates a polyline from the given coordinate values.
     */
    @Override
    public Geometry createPolyline(final boolean polygon, final int dimension, final Vector... coordinates) {
        return implementation.createWrapper(implementation.createPolyline(polygon, dimension, coordinates));
    }

    /**
     * Creates a multi-polygon from an array of geometries.
     */
    @Override
    public GeometryWrapper createMultiPolygon(final Object[] geometries) {
        return implementation.createMultiPolygon(geometries);
    }

    /**
     * Creates a geometry from components.
     */
    @Override
    public GeometryWrapper createFromComponents(final GeometryType type, Object components) {
        return implementation.createFromComponents(type, components);
    }

    /**
     * Transforms an envelope to a two-dimensional polygon.
     */
    @Override
    public GeometryWrapper toGeometry2D(final Envelope envelope, final WraparoundMethod strategy) {
        return implementation.toGeometry2D(envelope, strategy);
    }

    /**
     * Parses the given Well Known Text (WKT).
     */
    @Override
    public GeometryWrapper parseWKT(final String wkt) throws Exception {
        return implementation.parseWKT(wkt);
    }

    /**
     * Reads the given Well Known Binary (WKB).
     */
    @Override
    public GeometryWrapper parseWKB(final ByteBuffer data) throws Exception {
        return implementation.parseWKB(data);
    }
}
