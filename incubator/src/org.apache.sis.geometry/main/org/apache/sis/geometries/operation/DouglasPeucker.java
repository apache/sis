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
package org.apache.sis.geometries.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Prism;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;


/**
 * Simplification of geometries using the Douglas-Peucker algorithm.
 * The points of each line string are reduced to a subset such that no removed point
 * is farther than a given tolerance from the simplified line string.
 *
 * <p>The distances are Euclidean distances computed in the coordinate system of the geometry,
 * using all its dimensions. The tolerance is therefore expressed in the units of the coordinate
 * system axes, which are degrees for a geographic reference system.</p>
 *
 * <p>The algorithm applies on all geometry types:</p>
 * <ul>
 *   <li>Line strings keep their first and last points.</li>
 *   <li>Linear rings, and the rings of polygons, keep at least the four points of a triangle
 *       in order to stay valid. Consequently, a ring smaller than the tolerance is not removed.</li>
 *   <li>Each component of a compound curve is simplified with its end points kept,
 *       so the compound curve stays continuous.</li>
 *   <li>Mesh primitives of type line strip or line loop keep their type.</li>
 *   <li>Each element of a collection is simplified, and the base of a prism is simplified.</li>
 *   <li>Other geometries, for example points, triangles, curves with a non-linear interpolation,
 *       polyhedral surfaces whose patches share edges, or solids, are returned unchanged.</li>
 * </ul>
 *
 * <p>Points having a non-finite coordinate are kept and break the line in sections
 * which are simplified independently. All attributes of the kept points are preserved.
 * The rings and the elements of a geometry are simplified independently of each other,
 * so the topology between them is not preserved: simplified rings may cross each other.</p>
 *
 * <p>When no point is removed, the given geometry is returned unchanged.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm">Ramer–Douglas–Peucker algorithm</a>
 */
public final class DouglasPeucker {

    /**
     * Minimal number of points of a valid ring: three distinct points and the closing one.
     */
    private static final int MIN_RING_POINTS = 4;

    private DouglasPeucker(){}

    /**
     * Simplifies the given geometry.
     * See the class Javadoc for the processing applied on each geometry type.
     *
     * @param  geometry   the geometry to simplify.
     * @param  tolerance  maximal distance between a removed point and the simplified geometry.
     *                    If not strictly positive, the geometry is returned unchanged.
     * @return the simplified geometry, of the same type as the given geometry.
     */
    public static Geometry simplify(Geometry geometry, double tolerance) {
        return (tolerance > 0) ? simplifyAny(geometry, tolerance * tolerance) : geometry;
    }

    /**
     * Simplifies the given line string, keeping its first and last points.
     * Linear rings and mesh line loops keep at least the four points of a triangle.
     *
     * @param  line       the line string to simplify.
     * @param  tolerance  maximal distance between a removed point and the simplified line string.
     *                    If not strictly positive, the line string is returned unchanged.
     * @return the simplified line string, of the same type as the given line string.
     */
    public static LineString simplify(LineString line, double tolerance) {
        return (tolerance > 0) ? simplifyLine(line, tolerance * tolerance, false) : line;
    }

    /**
     * Simplifies the given linear ring, keeping at least the four points of a triangle.
     *
     * @param  ring       the linear ring to simplify.
     * @param  tolerance  maximal distance between a removed point and the simplified ring.
     *                    If not strictly positive, the ring is returned unchanged.
     * @return the simplified ring.
     */
    public static LinearRing simplify(LinearRing ring, double tolerance) {
        return (tolerance > 0) ? simplifyRing(ring, tolerance * tolerance) : ring;
    }

    /**
     * Simplifies the exterior and interior rings of the given polygon.
     * Each ring keeps at least the four points of a triangle.
     *
     * @param  polygon    the polygon to simplify.
     * @param  tolerance  maximal distance between a removed point and the simplified rings.
     *                    If not strictly positive, the polygon is returned unchanged.
     * @return the simplified polygon.
     */
    public static Polygon simplify(Polygon polygon, double tolerance) {
        return (tolerance > 0) ? simplifyPolygon(polygon, tolerance * tolerance) : polygon;
    }

    /**
     * Simplifies a geometry of any type.
     *
     * @param  geometry    the geometry to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static Geometry simplifyAny(final Geometry geometry, final double tolerance2) {
        if (geometry.isEmpty()) {
            return geometry;
        }
        /*
         * Case order matters: the first matching case is selected.
         * Curves are checked before mesh primitives and collections because
         * line strips, line loops and product curves are also of those types.
         */
        return switch (geometry) {
            case Curve                 cdt -> simplifyCurve(cdt, tolerance2, false);
            case MeshPrimitive         cdt -> cdt;     // Points, segments or triangles: no removable point.
            case Polygon               cdt -> simplifyPolygon(cdt, tolerance2);
            case CurvePolygon          cdt -> simplifyCurvePolygon(cdt, tolerance2);
            case GeometryCollection<?> cdt -> simplifyCollection(cdt, tolerance2);
            case Prism                 cdt -> simplifyPrism(cdt, tolerance2);
            default -> geometry;
        };
    }

    /**
     * Simplifies a curve. Only curves made of line strings can be simplified,
     * other curves are returned unchanged.
     *
     * @param  curve       the curve to simplify.
     * @param  tolerance2  square of the tolerance.
     * @param  ring        whether the curve is the ring of a surface.
     */
    private static Curve simplifyCurve(final Curve curve, final double tolerance2, final boolean ring) {
        return switch (curve) {
            case LineString    cdt -> simplifyLine(cdt, tolerance2, ring);
            case CompoundCurve cdt -> simplifyCompound(cdt, tolerance2);
            default -> curve;       // Non-linear interpolation, the control points are not vertices.
        };
    }

    /**
     * Simplifies a line string, preserving the mesh primitive types.
     *
     * @param  line        the line string to simplify.
     * @param  tolerance2  square of the tolerance.
     * @param  ring        whether the line string is the ring of a surface.
     */
    private static LineString simplifyLine(final LineString line, final double tolerance2, final boolean ring) {
        if (line instanceof LinearRing cdt) {
            return simplifyRing(cdt, tolerance2);
        }
        final boolean loop = (line instanceof MeshPrimitive.LineLoop);
        final DataPoints points = line.getDataPoints();
        final int[] indices = select(points, tolerance2, ring | loop);
        if (indices == null) {
            return line;
        }
        return switch (line) {
            // The data points of a loop repeat the first point at the end, which is implicit in the primitive.
            case MeshPrimitive.LineLoop  cdt -> new MeshPrimitive.LineLoop(subset(points, Arrays.copyOf(indices, indices.length - 1)));
            case MeshPrimitive.LineStrip cdt -> new MeshPrimitive.LineStrip(subset(points, indices));
            default -> GeometryFactory.DEFAULT.createLineString(points.subset(indices));
        };
    }

    /**
     * Simplifies a linear ring, keeping at least the four points of a triangle.
     *
     * @param  ring        the linear ring to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static LinearRing simplifyRing(final LinearRing ring, final double tolerance2) {
        final DataPoints points = ring.getDataPoints();
        final int[] indices = select(points, tolerance2, true);
        return (indices == null) ? ring : GeometryFactory.DEFAULT.createLinearRing(points.subset(indices));
    }

    /**
     * Simplifies each component of a compound curve. The end points of each component are kept,
     * so the compound curve stays continuous.
     *
     * @param  curve       the compound curve to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static CompoundCurve simplifyCompound(final CompoundCurve curve, final double tolerance2) {
        final Curve[] components = new Curve[curve.getNumCurves()];
        boolean changed = false;
        for (int i = 0; i < components.length; i++) {
            final Curve component = curve.getCurveN(i);
            components[i] = simplifyCurve(component, tolerance2, false);
            changed |= (components[i] != component);
        }
        return changed ? GeometryFactory.DEFAULT.createCompoundCurve(curve.getCoordinateReferenceSystem(), components) : curve;
    }

    /**
     * Simplifies the exterior and interior rings of a polygon.
     *
     * @param  polygon     the polygon to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static Polygon simplifyPolygon(final Polygon polygon, final double tolerance2) {
        final LinearRing exterior = polygon.getExteriorRing();
        final LinearRing simplified = simplifyRing(exterior, tolerance2);
        boolean changed = (simplified != exterior);
        final List<LinearRing> interiors = new ArrayList<>(polygon.getInteriorRings());
        for (int i = 0, n = interiors.size(); i < n; i++) {
            final LinearRing interior = interiors.get(i);
            final LinearRing ring = simplifyRing(interior, tolerance2);
            if (ring != interior) {
                interiors.set(i, ring);
                changed = true;
            }
        }
        return changed ? GeometryFactory.DEFAULT.createPolygon(simplified, interiors) : polygon;
    }

    /**
     * Simplifies the exterior and interior rings of a curve polygon.
     *
     * @param  polygon     the curve polygon to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static CurvePolygon simplifyCurvePolygon(final CurvePolygon polygon, final double tolerance2) {
        final Curve exterior = polygon.getExteriorRing();
        final Curve simplified = simplifyCurve(exterior, tolerance2, true);
        boolean changed = (simplified != exterior);
        final List<Curve> interiors = new ArrayList<>(polygon.getInteriorRings());
        for (int i = 0, n = interiors.size(); i < n; i++) {
            final Curve interior = interiors.get(i);
            final Curve ring = simplifyCurve(interior, tolerance2, true);
            if (ring != interior) {
                interiors.set(i, ring);
                changed = true;
            }
        }
        return changed ? GeometryFactory.DEFAULT.createCurvePolygon(simplified, interiors) : polygon;
    }

    /**
     * Simplifies each element of a collection. The element type of the collection is preserved.
     *
     * @param  collection  the collection to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static Geometry simplifyCollection(final GeometryCollection<?> collection, final double tolerance2) {
        final Geometry[] elements = new Geometry[collection.getNumGeometries()];
        boolean changed = false;
        for (int i = 0; i < elements.length; i++) {
            final Geometry element = collection.getGeometryN(i);
            elements[i] = simplifyAny(element, tolerance2);
            changed |= (elements[i] != element);
        }
        if (!changed) {
            return collection;
        }
        return GeometryFactory.DEFAULT.createGeometryCollection(
                collection.getElementType(), collection.getCoordinateReferenceSystem(), elements);
    }

    /**
     * Simplifies the base of a prism. The extrusion is unchanged.
     *
     * @param  prism       the prism to simplify.
     * @param  tolerance2  square of the tolerance.
     */
    private static Prism simplifyPrism(final Prism prism, final double tolerance2) {
        final Geometry base = prism.getBase();
        final Geometry simplified = simplifyAny(base, tolerance2);
        if (simplified == base) {
            return prism;
        }
        return GeometryFactory.DEFAULT.createPrism(simplified, prism.getExtrusionRange(), prism.getExtrusionCrs());
    }

    /**
     * Returns a subset of the given data points, which are the data points of a mesh primitive.
     */
    private static ArrayDataPoints subset(final DataPoints points, final int[] indices) {
        return (ArrayDataPoints) points.subset(indices);     // Subset of ArrayDataPoints is an ArrayDataPoints.
    }

    /**
     * Selects the points to keep in the given sequence of data points.
     *
     * @param  points      the data points of a line string.
     * @param  tolerance2  square of the tolerance.
     * @param  ring        whether to keep at least the four points of a triangle.
     * @return indices in increasing order of the points to keep, or {@code null} if all points are kept.
     */
    private static int[] select(final DataPoints points, final double tolerance2, final boolean ring) {
        final int n = points.size();
        if (n <= (ring ? MIN_RING_POINTS : 2)) {
            return null;
        }
        final Array positions = points.getAttributeArray(DataPointsType.ATT_POSITION);
        final int dimension = positions.getDimension();
        final double[] coordinates = positions.toArrayDouble();
        final boolean[] keep = new boolean[n];
        /*
         * Simplify independently each section of points having finite coordinates.
         * Points with a non-finite coordinate are kept as section separators.
         */
        int start = -1;
        for (int i = 0; i <= n; i++) {
            if (i == n || !isFinite(coordinates, dimension, i)) {
                if (start >= 0) {
                    simplifySection(coordinates, dimension, start, i - 1, tolerance2, keep);
                    start = -1;
                }
                if (i != n) {
                    keep[i] = true;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        int count = count(keep);
        if (ring && count < MIN_RING_POINTS) {
            /*
             * The ring collapsed to a segment or a point. Keep the triangle made of the first point,
             * the point farthest from it, and the point farthest from the segment between those two.
             * The first and last points have been kept since they are the end points of a section.
             */
            final int last = n - 1;
            final int far = farthest(coordinates, dimension, 0, 0, 1, last);
            if (far >= 0) {
                keep[far] = true;
                final int apex = farthest(coordinates, dimension, 0, far, 1, last);
                if (apex >= 0) {
                    keep[apex] = true;
                }
            }
            count = count(keep);
        }
        if (count == n) {
            return null;
        }
        final int[] indices = new int[count];
        count = 0;
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                indices[count++] = i;
            }
        }
        return indices;
    }

    /**
     * Applies the Douglas-Peucker algorithm on the points between the given indices inclusive.
     * The end points are kept, then the point farthest from the segment between the end points
     * is kept if its distance exceeds the tolerance, and the two resulting sections are processed
     * in the same way. A stack is used instead of recursion for avoiding stack overflow on long lines.
     *
     * @param  coordinates  the coordinates of all points.
     * @param  dimension    number of coordinates per point.
     * @param  first        index of the first point of the section to simplify.
     * @param  last         index of the last point of the section to simplify.
     * @param  tolerance2   square of the tolerance.
     * @param  keep         where to flag the points to keep.
     */
    private static void simplifySection(final double[] coordinates, final int dimension, final int first, final int last,
                                        final double tolerance2, final boolean[] keep)
    {
        keep[first] = true;
        keep[last]  = true;
        int[] stack = new int[16];
        int size = 0;
        stack[size++] = first;
        stack[size++] = last;
        while (size != 0) {
            final int end   = stack[--size];
            final int start = stack[--size];
            final int index = farthest(coordinates, dimension, start, end, start + 1, end);
            if (index >= 0 && distanceSquared(coordinates, dimension, start, end, index) > tolerance2) {
                keep[index] = true;
                if (size + 4 > stack.length) {
                    stack = Arrays.copyOf(stack, stack.length * 2);
                }
                stack[size++] = start;
                stack[size++] = index;
                stack[size++] = index;
                stack[size++] = end;
            }
        }
    }

    /**
     * Returns the index of the point farthest from the segment between the points at index {@code a} and {@code b}.
     * Points with a non-finite distance are ignored.
     *
     * @param  coordinates  the coordinates of all points.
     * @param  dimension    number of coordinates per point.
     * @param  a            index of the segment start point.
     * @param  b            index of the segment end point. May be equal to {@code a}.
     * @param  lower        index of the first point to test, inclusive.
     * @param  upper        index of the last point to test, exclusive.
     * @return index of the farthest point, or -1 if none.
     */
    private static int farthest(final double[] coordinates, final int dimension, final int a, final int b,
                                final int lower, final int upper)
    {
        double max = -1;
        int index = -1;
        for (int i = lower; i < upper; i++) {
            final double d2 = distanceSquared(coordinates, dimension, a, b, i);
            if (d2 > max) {
                max = d2;
                index = i;
            }
        }
        return index;
    }

    /**
     * Returns the square of the distance from the point at index {@code p}
     * to the segment between the points at index {@code a} and {@code b}.
     *
     * @param  coordinates  the coordinates of all points.
     * @param  dimension    number of coordinates per point.
     * @param  a            index of the segment start point.
     * @param  b            index of the segment end point. May be equal to {@code a}.
     * @param  p            index of the point.
     * @return square of the distance, or NaN if a coordinate is NaN.
     */
    private static double distanceSquared(final double[] coordinates, final int dimension,
                                          final int a, final int b, final int p)
    {
        final int oa = a * dimension;
        final int ob = b * dimension;
        final int op = p * dimension;
        double length2 = 0, dot = 0;
        for (int k = 0; k < dimension; k++) {
            final double ab = coordinates[ob + k] - coordinates[oa + k];
            final double ap = coordinates[op + k] - coordinates[oa + k];
            length2 += ab * ab;
            dot     += ab * ap;
        }
        // Position of the projection of the point on the segment, clamped to the segment end points.
        final double t = (length2 > 0) ? Math.max(0, Math.min(1, dot / length2)) : 0;
        double d2 = 0;
        for (int k = 0; k < dimension; k++) {
            final double r = (coordinates[op + k] - coordinates[oa + k])
                       - t * (coordinates[ob + k] - coordinates[oa + k]);
            d2 += r * r;
        }
        return d2;
    }

    /**
     * Returns whether all coordinates of the point at the given index are finite.
     */
    private static boolean isFinite(final double[] coordinates, final int dimension, final int index) {
        for (int k = index * dimension, end = k + dimension; k < end; k++) {
            if (!Double.isFinite(coordinates[k])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of {@code true} values in the given array.
     */
    private static int count(final boolean[] keep) {
        int count = 0;
        for (final boolean k : keep) {
            if (k) count++;
        }
        return count;
    }
}
