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
package org.apache.sis.geometries.internal.shared;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.operation.ToPrimitive;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


/**
 * Centroids and representative points of geometries which do not compute them directly,
 * from their components or from a linear approximation.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Centroids {

    /**
     * Maximal distance between a curve and its linear approximation,
     * as a fraction of the diagonal of the curve envelope.
     */
    private static final double LINEARIZATION_OFFSET = 1E-6;

    private Centroids() {
    }

    /**
     * Returns a fine linear approximation of the given curve.
     *
     * @param  curve  the curve to approximate.
     * @return the linear approximation.
     * @throws UnsupportedOperationException if the curve has no linear approximation.
     */
    public static LineString linearize(final Curve curve) {
        final Envelope envelope = curve.getEnvelope();
        double diagonal = 0;
        if (envelope != null) {
            for (int i = 0, n = envelope.getDimension(); i < n; i++) {
                final double span = envelope.getSpan(i);
                if (Double.isFinite(span)) {
                    diagonal += span * span;
                }
            }
        }
        diagonal = Math.sqrt(diagonal);
        final Quantity<?> offset = (diagonal > 0)
                ? Quantities.create(LINEARIZATION_OFFSET * diagonal, Geometries.getLinearUnit(curve)) : null;
        return curve.asLine(null, offset);
    }

    /**
     * Returns a polygon with the linear approximations of the rings of the given curve polygon.
     *
     * @param  surface  the curve polygon to approximate.
     * @return the polygon approximating the curve polygon.
     */
    public static Polygon linearize(final CurvePolygon surface) {
        final LinearRing exterior = ring(surface.getExteriorRing());
        final List<LinearRing> interiors = new ArrayList<>();
        for (final Curve interior : surface.getInteriorRings()) {
            interiors.add(ring(interior));
        }
        return GeometryFactory.DEFAULT.createPolygon(exterior, interiors);
    }

    private static LinearRing ring(final Curve curve) {
        if (curve instanceof LinearRing ring) {
            return ring;
        }
        return GeometryFactory.DEFAULT.createLinearRing(linearize(curve).getDataPoints());
    }

    /**
     * Returns the centroid of a polyline: the mean of the segment middles weighted by the segment lengths,
     * or the mean of the positions if the polyline has no length. All attributes are interpolated
     * with the same weights, each segment giving half of its length to each of its two points.
     *
     * @param  points  points of the polyline.
     * @return the centroid, or {@code null} if there is no point.
     */
    public static Point polyline(final DataPoints points) {
        final int n = points.size();
        if (n == 0) {
            return null;
        }
        final double[] weights = new double[n];
        double total = 0;
        for (int i = 1; i < n; i++) {
            final double half = distance(points.getPosition(i - 1), points.getPosition(i)) / 2;
            weights[i - 1] += half;
            weights[i]     += half;
            total += 2 * half;
        }
        for (int i = 0; i < n; i++) {
            weights[i] = (total > 0) ? weights[i] / total : 1.0 / n;
        }
        return points.getInterpolatedPoint(indices(n), weights);
    }

    /**
     * Returns the centroid of a polygon: the centroid of its triangulation, attributes being interpolated
     * inside each triangle. Falls back on the centroid of the exterior ring if the polygon has no area.
     *
     * @param  polygon  the polygon.
     * @return the centroid of the polygon.
     */
    public static Point polygon(final Polygon polygon) {
        final Point centroid = ToPrimitive.toPrimitive(polygon).getCentroid();
        return (centroid != null) ? centroid : polyline(polygon.getExteriorRing().getDataPoints());
    }

    /**
     * Returns a point inside a polygon: the centroid of the largest triangle of its triangulation.
     * Falls back on the centroid of the exterior ring if the polygon has no area.
     *
     * @param  polygon  the polygon.
     * @return a point inside the polygon.
     */
    public static Point pointOnPolygon(final Polygon polygon) {
        final Point point = ToPrimitive.toPrimitive(polygon).getRepresentativePoint();
        return (point != null) ? point : polyline(polygon.getExteriorRing().getDataPoints());
    }

    /**
     * Returns the centroid of a collection: the mean of the centroids of the elements having the largest
     * topological dimension, weighted by their number of points, length or area.
     * Elements of lower dimensions are ignored.
     *
     * @param  collection  the collection.
     * @return the centroid, or {@code null} if all elements are empty.
     */
    public static Point collection(final GeometryCollection<?> collection) {
        final int dimension = largestDimension(collection);
        if (dimension < 0) {
            return null;
        }
        final List<Point> centroids = new ArrayList<>();
        final List<Double> weights = new ArrayList<>();
        for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
            final Geometry element = collection.getGeometryN(i);
            if (!element.isEmpty() && element.getTopologicDimension() == dimension) {
                final Point centroid = element.getCentroid();
                if (centroid != null) {
                    centroids.add(centroid);
                    weights.add(measure(element, dimension));
                }
            }
        }
        return mean(centroids, weights);
    }

    /**
     * Returns a representative point of a collection: the one of the first element
     * having the largest topological dimension.
     *
     * @param  collection  the collection.
     * @return the representative point, or {@code null} if all elements are empty.
     */
    public static Point representative(final GeometryCollection<?> collection) {
        final int dimension = largestDimension(collection);
        for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
            final Geometry element = collection.getGeometryN(i);
            if (!element.isEmpty() && element.getTopologicDimension() == dimension) {
                return element.getRepresentativePoint();
            }
        }
        return null;
    }

    /**
     * Returns the centroid of a polyhedral surface: the mean of the centroids of the patches weighted by their area.
     *
     * @param  surface  the polyhedral surface.
     * @return the centroid, or {@code null} if the surface has no patch.
     */
    public static Point patches(final PolyhedralSurface<?> surface) {
        final int n = surface.getNumPatches();
        final List<Point> centroids = new ArrayList<>(n);
        final List<Double> weights = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final Polygon patch = surface.getPatchN(i);
            if (!patch.isEmpty()) {
                centroids.add(patch.getCentroid());
                weights.add(patch.getArea().getValue().doubleValue());
            }
        }
        return mean(centroids, weights);
    }

    /**
     * Returns a representative point of a polyhedral surface: the one of its largest patch.
     *
     * @param  surface  the polyhedral surface.
     * @return a point on the surface, or {@code null} if the surface has no patch.
     */
    public static Point representative(final PolyhedralSurface<?> surface) {
        Polygon largest = null;
        double area = -1;
        for (int i = 0, n = surface.getNumPatches(); i < n; i++) {
            final Polygon patch = surface.getPatchN(i);
            if (!patch.isEmpty()) {
                final double a = patch.getArea().getValue().doubleValue();
                if (a > area) {
                    area = a;
                    largest = patch;
                }
            }
        }
        return (largest != null) ? largest.getRepresentativePoint() : null;
    }

    /**
     * Returns the largest topological dimension among the non-empty elements, or -1 if none.
     */
    private static int largestDimension(final GeometryCollection<?> collection) {
        int dimension = -1;
        for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
            final Geometry element = collection.getGeometryN(i);
            if (!element.isEmpty()) {
                dimension = Math.max(dimension, element.getTopologicDimension());
            }
        }
        return dimension;
    }

    /**
     * Returns the number of points, the length or the area of the given element,
     * for the given topological dimension.
     */
    private static double measure(final Geometry element, final int dimension) {
        if (element instanceof GeometryCollection<?> collection) {
            double sum = 0;
            for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
                final Geometry child = collection.getGeometryN(i);
                if (!child.isEmpty() && child.getTopologicDimension() == dimension) {
                    sum += measure(child, dimension);
                }
            }
            return sum;
        }
        switch (dimension) {
            case 0: return 1;
            case 1: {
                if (element instanceof Curve curve) {
                    final DataPoints points = (curve instanceof LineString line ? line : linearize(curve)).getDataPoints();
                    double length = 0;
                    for (int i = 1, n = points.size(); i < n; i++) {
                        length += distance(points.getPosition(i - 1), points.getPosition(i));
                    }
                    return length;
                }
                break;
            }
            case 2: {
                if (element instanceof Surface surface) {
                    return surface.getArea().getValue().doubleValue();
                }
                break;
            }
        }
        return 1;
    }

    /**
     * Returns the mean of the given points weighted by the given weights, or the plain mean if the weights
     * sum to zero. The attributes present in all points are interpolated with the same weights, as declared
     * by the {@linkplain DataPointsType#getAttributeInterpolation(String) attribute interpolation}.
     */
    private static Point mean(final List<Point> points, final List<Double> weights) {
        final int n = points.size();
        if (n == 0) {
            return null;
        }
        double total = 0;
        for (final double w : weights) {
            total += w;
        }
        final double[] w = new double[n];
        for (int i = 0; i < n; i++) {
            w[i] = (total > 0) ? weights.get(i) / total : 1.0 / n;
        }
        final DataPointsType type = points.get(0).getDataPointsType();
        final Map<String,Array> attributes = new LinkedHashMap<>();
search: for (final String name : type.getAttributeNames()) {
            final List<Vector<?>> values = new ArrayList<>(n);
            for (final Point point : points) {
                if (!point.getDataPointsType().getAttributeNames().contains(name)) {
                    continue search;
                }
                values.add(point.getAttribute(name));
            }
            attributes.put(name, NDArrays.of(values, type.getAttributeSystem(name), type.getAttributeType(name)));
        }
        return new ArrayDataPoints(attributes).getInterpolatedPoint(indices(n), w);
    }

    /**
     * Returns the indices from 0 inclusive to {@code n} exclusive.
     */
    private static int[] indices(final int n) {
        final int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        return indices;
    }

    private static double distance(final Vector<?> a, final Vector<?> b) {
        double sum = 0;
        for (int d = 0, n = a.getDimension(); d < n; d++) {
            final double delta = b.get(d) - a.get(d);
            sum += delta * delta;
        }
        return Math.sqrt(sum);
    }
}
