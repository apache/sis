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
package org.apache.sis.map.service.se1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.adapter.JTSAdapter;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.GeometryProcessor;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.Polygon;


/**
 * Utility methods for preparing SIS geometries for rendering.
 * Geometries from other libraries are converted to SIS geometries.
 * Only JTS is supported in this version.
 *
 * @author Johann Sorel (Geomatys)
 */
final class GeometryResolver {

    /**
     * Whether the JTS library is available.
     * JTS is an optional dependency of this module.
     */
    private static final boolean JTS_PRESENT;
    static {
        boolean present;
        try {
            Class.forName("org.locationtech.jts.geom.Geometry", false, GeometryResolver.class.getClassLoader());
            present = true;
        } catch (ClassNotFoundException | LinkageError e) {
            present = false;
        }
        JTS_PRESENT = present;
    }

    /**
     * Logs the curves which cannot be linearized.
     */
    private static final BiConsumer<Curve, RuntimeException> LOG_FAILURE = (curve, ex) -> SymbolizerToScene2D.LOGGER.log(
            Level.FINE, "Cannot linearize " + curve.getGeometryType() + ", curve is not rendered.", ex);

    private GeometryResolver() {
    }

    /**
     * Returns the given value as a SIS geometry.
     * JTS geometries are wrapped in a SIS view, without copying the coordinates.
     *
     * @param  value  the value of the symbolizer geometry expression, may be null.
     * @return the SIS geometry, or {@code null} if the value is not a supported geometry.
     */
    static Geometry resolve(Object value) {
        if (value instanceof Geometry geometry) {
            return geometry;
        }
        if (JTS_PRESENT) {
            return JTS.fromJTS(value);
        }
        return null;
    }

    /**
     * Isolates the JTS dependency, so that this class can be loaded when JTS is absent.
     */
    private static final class JTS {
        static Geometry fromJTS(Object value) {
            if (value instanceof org.locationtech.jts.geom.Geometry jts) {
                return JTSAdapter.fromJTS(jts, false);
            }
            return null;
        }
    }

    /**
     * Adds all linear components of the given geometry to the given list.
     * Polygons contribute their exterior and interior rings.
     * Curves other than line strings are linearized when supported, otherwise they are skipped
     * and the failure is logged.
     * Points and other geometry types are ignored.
     *
     * @param geometry the geometry to decompose, may be null.
     * @param target where to add the linear components.
     */
    static void linearComponents(Geometry geometry, List<LineString> target) {
        linearComponents(geometry, target, LOG_FAILURE);
    }

    /**
     * Adds all linear components of the given geometry to the given list,
     * reporting the curves which cannot be linearized to the given consumer.
     *
     * @param geometry the geometry to decompose, may be null.
     * @param target where to add the linear components.
     * @param onFailure receives each curve which cannot be linearized, with the cause.
     */
    static void linearComponents(Geometry geometry, List<LineString> target, BiConsumer<Curve, RuntimeException> onFailure) {
        if (geometry == null || geometry.isEmpty()) {
            return;
        }
        if (geometry instanceof LineString line) {
            target.add(line);
        } else if (geometry instanceof Curve curve) {
            final LineString line;
            try {
                line = curve.asLine(null, null);
            } catch (RuntimeException ex) {
                onFailure.accept(curve, ex);
                return;
            }
            if (line != null) target.add(line);
        } else if (geometry instanceof Polygon polygon) {
            linearComponents(polygon.getExteriorRing(), target, onFailure);
            for (Geometry ring : polygon.getInteriorRings()) {
                linearComponents(ring, target, onFailure);
            }
        } else if (geometry instanceof CurvePolygon polygon) {
            linearComponents(polygon.getExteriorRing(), target, onFailure);
            for (Geometry ring : polygon.getInteriorRings()) {
                linearComponents(ring, target, onFailure);
            }
        } else if (geometry instanceof GeometryCollection<?> collection) {
            for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
                linearComponents(collection.getGeometryN(i), target, onFailure);
            }
        }
    }

    /**
     * Adds all surface components of the given geometry to the given list.
     * Each component is a list of rings, the first one being the exterior ring
     * and the other ones the interior rings (holes).
     * Polygon rings are linearized when needed.
     * Linear geometries are interpreted as the exterior ring of a polygon, implicitly closed.
     * Curves which cannot be linearized are logged.
     * Points and other geometry types are ignored.
     *
     * @param geometry the geometry to decompose, may be null.
     * @param target where to add the surface components.
     */
    static void polygonComponents(Geometry geometry, List<List<LineString>> target) {
        polygonComponents(geometry, target, LOG_FAILURE);
    }

    /**
     * Implementation of {@link #polygonComponents(Geometry, List)} reporting failures to the given consumer.
     */
    private static void polygonComponents(Geometry geometry, List<List<LineString>> target, BiConsumer<Curve, RuntimeException> onFailure) {
        if (geometry == null || geometry.isEmpty()) {
            return;
        }
        if (geometry instanceof Polygon polygon) {
            addRings(polygon.getExteriorRing(), polygon.getInteriorRings(), target, onFailure);
        } else if (geometry instanceof CurvePolygon polygon) {
            addRings(polygon.getExteriorRing(), polygon.getInteriorRings(), target, onFailure);
        } else if (geometry instanceof Curve) {
            final List<LineString> rings = new ArrayList<>(1);
            linearComponents(geometry, rings, onFailure);
            if (!rings.isEmpty()) target.add(rings);
        } else if (geometry instanceof GeometryCollection<?> collection) {
            for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
                polygonComponents(collection.getGeometryN(i), target, onFailure);
            }
        }
    }

    /**
     * Adds the linearized rings of a polygon. If the exterior ring cannot be linearized, the polygon is skipped.
     */
    private static void addRings(Curve exterior, List<? extends Curve> interiors, List<List<LineString>> target,
            BiConsumer<Curve, RuntimeException> onFailure)
    {
        final List<LineString> rings = new ArrayList<>(1 + interiors.size());
        linearComponents(exterior, rings, onFailure);
        if (rings.isEmpty()) {
            return;
        }
        for (Curve ring : interiors) {
            linearComponents(ring, rings, onFailure);
        }
        target.add(rings);
    }

    /**
     * Adds the points where graphics should be drawn for the given geometry.
     * Points give their own position, and collections made only of points give each of their points.
     * Other geometries give a single representative point: a point on the surface for polygons,
     * the middle of the curve for curves, or the centroid or envelope center as a fallback.
     *
     * @param geometry the geometry, may be null.
     * @param target where to add the points.
     */
    static void anchorPoints(Geometry geometry, List<Point> target) {
        if (geometry == null || geometry.isEmpty()) {
            return;
        }
        if (geometry instanceof Point point) {
            target.add(point);
        } else if (geometry instanceof GeometryCollection<?> collection && !(geometry instanceof Curve) && isMultiPoint(collection)) {
            for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
                anchorPoints(collection.getGeometryN(i), target);
            }
        } else {
            final Point point = representativePoint(geometry);
            if (point != null) target.add(point);
        }
    }

    /**
     * Returns whether all elements of the given collection are points.
     */
    private static boolean isMultiPoint(GeometryCollection<?> collection) {
        final int n = collection.getNumGeometries();
        for (int i = 0; i < n; i++) {
            if (!(collection.getGeometryN(i) instanceof Point)) return false;
        }
        return n != 0;
    }

    /**
     * Returns a representative point of the given geometry, trying in order the representative point
     * (interior point), the centroid, then the center of the envelope.
     *
     * @return the representative point, or {@code null} if none can be computed.
     */
    private static Point representativePoint(Geometry geometry) {
        try {
            final Point point = geometry.getRepresentativePoint();
            if (point != null && !point.isEmpty()) return point;
        } catch (RuntimeException ex) {
            SymbolizerToScene2D.LOGGER.log(Level.FINE, "Cannot compute representative point, trying centroid.", ex);
        }
        try {
            final Point point = geometry.getCentroid();
            if (point != null && !point.isEmpty()) return point;
        } catch (RuntimeException ex) {
            SymbolizerToScene2D.LOGGER.log(Level.FINE, "Cannot compute centroid, using envelope center.", ex);
        }
        try {
            final Envelope env = geometry.getEnvelope();
            if (env != null) {
                final double[] center = new double[env.getDimension()];
                for (int i = 0; i < center.length; i++) {
                    center[i] = env.getMedian(i);
                }
                return GeometryFactory.DEFAULT.createPoint(geometry.getCoordinateReferenceSystem(), center);
            }
        } catch (RuntimeException ex) {
            SymbolizerToScene2D.LOGGER.log(Level.FINE, "Cannot compute envelope, geometry is not rendered.", ex);
        }
        return null;
    }

    /**
     * Transforms the given point to display space.
     *
     * @param point the point to transform, not null.
     * @param dataToDisplay transform from the point CRS to display space.
     * @return the (x,y) display coordinates, or {@code null} if the point cannot be transformed.
     */
    static double[] toDisplay(Point point, MathTransform dataToDisplay) {
        final double[] source = point.getPosition().toArrayDouble();
        if (source.length != dataToDisplay.getSourceDimensions()) {
            return null;
        }
        final double[] target = new double[dataToDisplay.getTargetDimensions()];
        try {
            dataToDisplay.transform(source, 0, target, 0, 1);
        } catch (TransformException ex) {
            SymbolizerToScene2D.LOGGER.log(Level.FINE, ex.getMessage(), ex);
            return null;
        }
        return (Double.isFinite(target[0]) && Double.isFinite(target[1])) ? target : null;
    }

    /**
     * Transforms the given line string to display space.
     *
     * @param line the line string to transform, not null.
     * @param dataToDisplay transform from the line CRS to display space.
     * @return the line string in display space.
     */
    static LineString toDisplay(LineString line, MathTransform dataToDisplay) {
        return (LineString) new GeometryProcessor().transform(line, Geometries.getUndefinedCRS(2), dataToDisplay);
    }
}
