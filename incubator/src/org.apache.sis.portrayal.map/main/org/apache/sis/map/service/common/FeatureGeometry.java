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
package org.apache.sis.map.service.common;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.adapter.JTSAdapter;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.maths.Vector;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * The geometry of a feature: an Apache SIS geometry, a JTS geometry, or an unsupported value.
 * The geometry can be projected to display coordinates as Java2D shapes, or viewed as an Apache SIS geometry.
 *
 * <ul>
 *   <li>JTS geometries are viewed as Apache SIS geometries, without copying the coordinates.</li>
 *   <li>JTS is an optional dependency: JTS geometries are recognized by their class name before being viewed.</li>
 *   <li>A geometry without CRS, or with an undefined CRS, is assumed in the CRS of the scene.</li>
 *   <li>Curves other than line strings are linearized. Curves which cannot be linearized are ignored.</li>
 *   <li>Surfaces other than polygons, and solids, are ignored.</li>
 *   <li>Missing coordinates, and NaN coordinates after the two first ones (such as a missing <var>z</var>), are set to zero.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FeatureGeometry {
    /**
     * Prefix of the names of the JTS geometry classes.
     */
    private static final String JTS_PACKAGE = "org.locationtech.jts.geom.";

    /**
     * Margin added on each side of the scene for deciding whether a geometry is visible,
     * as a fraction of the largest scene size. This is the room for the graphics drawn around the geometry.
     */
    private static final double MARGIN = 0.25;

    /**
     * Number of horizontal coordinates, which are kept when not a number.
     */
    private static final int HORIZONTAL = 2;

    /**
     * The geometry, or {@code null} if the value is not a supported geometry.
     */
    private final Geometry geometry;

    /**
     * Wraps the value of a geometry property.
     * The class name of a JTS geometry is checked before any JTS class is used.
     *
     * @param  value  the geometry, or {@code null}.
     */
    public FeatureGeometry(final Object value) {
        Geometry sis = null;
        if (value instanceof Geometry g) {
            sis = g;
        } else if (value != null && value.getClass().getName().startsWith(JTS_PACKAGE)
                && value instanceof org.locationtech.jts.geom.Geometry jts)
        {
            try {
                sis = JTSAdapter.fromJTS(jts, false);
            } catch (RuntimeException e) {
                Scene2D.LOGGER.log(Level.FINE, "Cannot view a JTS geometry as an Apache SIS geometry.", e);
            }
        }
        geometry = sis;
    }

    /**
     * Returns the topological dimension of the geometry.
     *
     * @return 0 for points, 1 for lines, 2 for polygons, 3 for solids, or -1 if empty, null or unsupported.
     */
    public int getDimension() {
        return (geometry == null || geometry.isEmpty()) ? -1 : geometry.getTopologicDimension();
    }

    /**
     * Returns the geometry as an Apache SIS geometry.
     * JTS geometries are wrapped in a view, without copying the coordinates.
     *
     * @return the Apache SIS geometry, or {@code null} if the value is not a supported geometry.
     */
    public Geometry toGeometry() {
        return geometry;
    }

    /**
     * Projects the geometry to the display coordinates of the given scene.
     *
     * @param  scene  the scene where the geometry is drawn.
     * @return the projected geometry, or {@code null} if empty, unsupported or outside the scene.
     */
    public DisplayGeometry project(final Scene2D scene) {
        if (geometry == null) {
            return null;
        }
        CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
        if (crs != null && Geometries.isUndefined(crs)) {
            crs = null;
        }
        final DisplayGeometry display;
        try {
            display = new DisplayGeometry(scene.getDataToDisplay(crs));
        } catch (RenderingException e) {
            Scene2D.LOGGER.log(Level.FINE, "Cannot project a geometry to the scene.", e);
            return null;
        }
        read(geometry, display);
        return (display.getDimension() >= 0 && display.intersects(visibleArea(scene))) ? display : null;
    }

    /**
     * Sends the points, lines and polygons of a component of the geometry to the given target.
     */
    private void read(final Geometry component, final DisplayGeometry target) {
        try {
            switch (component) {
                case Point point -> target.addPoints(coordinates(point.getDataPoints(), target));
                case Curve curve -> target.addLine(coordinates(curve.asLine(null, null).getDataPoints(), target));
                case Polygon polygon -> {
                    final List<double[]> rings = new ArrayList<>(1 + polygon.getNumInteriorRing());
                    rings.add(coordinates(polygon.getExteriorRing().getDataPoints(), target));
                    for (final Curve ring : polygon.getInteriorRings()) {
                        rings.add(coordinates(ring.getDataPoints(), target));
                    }
                    target.addPolygon(rings);
                }
                case CurvePolygon polygon -> {
                    final List<double[]> rings = new ArrayList<>(1 + polygon.getNumInteriorRing());
                    rings.add(coordinates(polygon.getExteriorRing().asLine(null, null).getDataPoints(), target));
                    for (final Curve ring : polygon.getInteriorRings()) {
                        rings.add(coordinates(ring.asLine(null, null).getDataPoints(), target));
                    }
                    target.addPolygon(rings);
                }
                case GeometryCollection<?> collection -> {
                    for (int i=0; i<collection.getNumGeometries(); i++) {
                        read(collection.getGeometryN(i), target);
                    }
                }
                default -> { }
            }
        } catch (RuntimeException e) {
            Scene2D.LOGGER.log(Level.FINE, "Cannot read a component of a geometry.", e);
        }
    }

    /**
     * Returns the coordinates of the given points with the source dimension of the target.
     * Missing coordinates, and NaN coordinates after the horizontal ones, are set to zero.
     */
    private double[] coordinates(final DataPoints points, final DisplayGeometry target) {
        final int dimension = target.getSourceDimension();
        final int count = points.size();
        final double[] coordinates = new double[count * dimension];
        for (int i=0; i<count; i++) {
            final Vector<?> position = points.getPosition(i);
            final int available = Math.min(position.getDimension(), dimension);
            for (int d=0; d<available; d++) {
                final double value = position.get(d);
                if (d < HORIZONTAL || !Double.isNaN(value)) {
                    coordinates[i*dimension + d] = value;
                }
            }
        }
        return coordinates;
    }

    /**
     * Returns the area of the scene enlarged by the margin, in display coordinates.
     */
    private Rectangle2D visibleArea(final Scene2D scene) {
        final GridExtent extent = scene.grid.getExtent();
        final double width  = extent.getSize(0);
        final double height = extent.getSize(1);
        final double margin = MARGIN * Math.max(width, height);
        return new Rectangle2D.Double(extent.getLow(0) - margin, extent.getLow(1) - margin,
                                      width + 2*margin, height + 2*margin);
    }
}
