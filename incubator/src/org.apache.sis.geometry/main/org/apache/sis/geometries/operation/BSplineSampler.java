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
import java.util.List;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.internal.shared.Curves;
import org.apache.sis.geometries.surface.BSplineSurface;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;


/**
 * Approximation of a B-spline surface by a TIN whose vertices lie on the surface,
 * used by {@link GeometryProcessor#discretize(org.apache.sis.geometries.Geometry, Quantity, Quantity)}.
 *
 * <p>The surface is sampled on a grid of parameters, with the same number of divisions in every knot span.
 * Each grid cell gives two triangles split along the diagonal going from (u₀,v₀) to (u₁,v₁), which keeps
 * the up-normal of the surface. Triangles collapsed at a pole or an apex are omitted.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Without spacing nor offset, each knot span is divided {@value #DEFAULT_DIVISIONS} times.</li>
 *   <li>Otherwise the divisions are doubled until the limits are met, up to {@value #MAX_DIVISIONS}
 *       divisions per knot span.</li>
 *   <li>Limitation: the limits are checked at the middles of the triangle edges only.
 *       The distance along the surface between two vertices is estimated by a polyline
 *       through the middle of their edge.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class BSplineSampler {

    /**
     * Number of divisions of each knot span when no limit is given.
     */
    static final int DEFAULT_DIVISIONS = 8;

    /**
     * Maximal number of divisions of each knot span, for protection against unreachable limits.
     */
    static final int MAX_DIVISIONS = 256;

    /**
     * The surface to approximate.
     */
    private final BSplineSurface surface;

    /**
     * Distinct knots of each parameter inside the parameter domain, in increasing order.
     */
    private final double[] breaksU, breaksV;

    /**
     * Prepares the sampling of the given surface.
     *
     * @param  surface  the surface to approximate.
     */
    BSplineSampler(final BSplineSurface surface) {
        this.surface = surface;
        final List<double[]> knots = surface.getKnots();
        final int degree = surface.getDegree();
        breaksU = breaks(knots.get(0), degree);
        breaksV = breaks(knots.get(1), degree);
    }

    /**
     * Returns the distinct knot values from the start to the end of the parameter domain.
     */
    private static double[] breaks(final double[] knots, final int degree) {
        final double start = knots[degree];
        final double end = knots[knots.length - degree - 1];
        final List<Double> values = new ArrayList<>();
        values.add(start);
        for (final double knot : knots) {
            if (knot > values.get(values.size() - 1) && knot <= end) {
                values.add(knot);
            }
        }
        final double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    /**
     * Returns the triangulated approximation of the surface.
     *
     * @param  spacing  maximal distance along the surface between two adjacent vertices, or zero or {@code null}.
     * @param  offset   maximal distance between the surface and the triangles, or zero or {@code null}.
     * @return the approximation of the surface.
     * @throws IllegalArgumentException if the spacing or the offset is negative or not a length.
     */
    TIN sample(final Quantity<?> spacing, final Quantity<?> offset) {
        final Unit<?> unit = Geometries.getLinearUnit(surface);
        final double maxSpacing = distance(spacing, unit);
        final double maxOffset  = distance(offset,  unit);
        if (maxSpacing == 0 && maxOffset == 0) {
            return triangles(grid(DEFAULT_DIVISIONS));
        }
        /*
         * The grid is evaluated with twice the divisions: the samples of even indices are the vertices,
         * and the samples of odd indices are the middles of the edges, used for checking the limits.
         */
        int divisions = 1;
        double[][][] fine = grid(2 * divisions);
        while (divisions < MAX_DIVISIONS && !accepts(fine, maxSpacing, maxOffset)) {
            divisions *= 2;
            fine = grid(2 * divisions);
        }
        return triangles(vertices(fine));
    }

    /**
     * Converts the given spacing or offset to the unit of the coordinates.
     *
     * @return the distance in the unit of the coordinates, or 0 for no limit.
     */
    private static double distance(final Quantity<?> q, final Unit<?> unit) {
        if (q == null) {
            return 0;
        }
        final double value = Curves.convert(q, unit);
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("The spacing and the offset of an approximation must be"
                    + " positive or zero, but got " + q + '.');
        }
        return value;
    }

    /**
     * Evaluates the surface on a grid dividing each knot span the given number of times.
     *
     * @return positions indexed by [u index][v index][coordinate].
     */
    private double[][][] grid(final int divisions) {
        final double[] us = parameters(breaksU, divisions);
        final double[] vs = parameters(breaksV, divisions);
        final double[][][] positions = new double[us.length][vs.length][];
        for (int a = 0; a < us.length; a++) {
            for (int b = 0; b < vs.length; b++) {
                positions[a][b] = surface.getSurface(us[a], vs[b]).toArrayDouble();
            }
        }
        return positions;
    }

    /**
     * Returns the parameters dividing each interval between the given breaks the given number of times.
     */
    private static double[] parameters(final double[] breaks, final int divisions) {
        final double[] values = new double[(breaks.length - 1) * divisions + 1];
        for (int s = 0; s + 1 < breaks.length; s++) {
            for (int d = 0; d < divisions; d++) {
                values[s * divisions + d] = breaks[s] + (breaks[s + 1] - breaks[s]) * d / divisions;
            }
        }
        values[values.length - 1] = breaks[breaks.length - 1];
        return values;
    }

    /**
     * Returns the samples of even indices, which are the vertices of a grid evaluated with twice the divisions.
     */
    private static double[][][] vertices(final double[][][] fine) {
        final double[][][] coarse = new double[fine.length / 2 + 1][fine[0].length / 2 + 1][];
        for (int a = 0; a < coarse.length; a++) {
            for (int b = 0; b < coarse[a].length; b++) {
                coarse[a][b] = fine[2 * a][2 * b];
            }
        }
        return coarse;
    }

    /**
     * Returns whether the triangles made from the even samples of the given grid meet the limits.
     * Each edge between two vertices is checked at its middle, which is an odd sample: the distance of
     * that sample to the middle of the straight edge estimates the offset, and the polyline through it
     * estimates the distance along the surface.
     */
    private static boolean accepts(final double[][][] fine, final double maxSpacing, final double maxOffset) {
        for (int a = 0; a + 2 < fine.length; a += 2) {
            for (int b = 0; b + 2 < fine[a].length; b += 2) {
                final double[] p00 = fine[a][b];
                if (!accepts(p00, fine[a + 1][b],     fine[a + 2][b],     maxSpacing, maxOffset) ||    // Edge along u.
                    !accepts(p00, fine[a][b + 1],     fine[a][b + 2],     maxSpacing, maxOffset) ||    // Edge along v.
                    !accepts(p00, fine[a + 1][b + 1], fine[a + 2][b + 2], maxSpacing, maxOffset))      // Diagonal.
                {
                    return false;
                }
            }
        }
        /*
         * The edges on the last row and the last column are not the first edges of any cell.
         */
        final int lastA = fine.length - 1, lastB = fine[0].length - 1;
        for (int a = 0; a + 2 <= lastA; a += 2) {
            if (!accepts(fine[a][lastB], fine[a + 1][lastB], fine[a + 2][lastB], maxSpacing, maxOffset)) {
                return false;
            }
        }
        for (int b = 0; b + 2 <= lastB; b += 2) {
            if (!accepts(fine[lastA][b], fine[lastA][b + 1], fine[lastA][b + 2], maxSpacing, maxOffset)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the edge from {@code start} to {@code end}, whose middle on the surface is {@code middle},
     * meets the limits.
     */
    private static boolean accepts(final double[] start, final double[] middle, final double[] end,
                                   final double maxSpacing, final double maxOffset)
    {
        if (maxSpacing > 0 && distance(start, middle) + distance(middle, end) > maxSpacing) {
            return false;
        }
        if (maxOffset > 0) {
            double sum = 0;
            for (int i = 0; i < middle.length; i++) {
                final double d = middle[i] - (start[i] + end[i]) / 2;
                sum += d * d;
            }
            return Math.sqrt(sum) <= maxOffset;
        }
        return true;
    }

    private static double distance(final double[] p, final double[] q) {
        double sum = 0;
        for (int i = 0; i < p.length; i++) {
            final double d = p[i] - q[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /**
     * Returns the TIN of the given grid of vertices, two triangles per cell.
     */
    private TIN triangles(final double[][][] grid) {
        final CoordinateReferenceSystem crs = surface.getCoordinateReferenceSystem();
        final List<Triangle> triangles = new ArrayList<>();
        for (int a = 0; a + 1 < grid.length; a++) {
            for (int b = 0; b + 1 < grid[a].length; b++) {
                final double[] p00 = grid[a][b],     p10 = grid[a + 1][b];
                final double[] p01 = grid[a][b + 1], p11 = grid[a + 1][b + 1];
                addTriangle(triangles, crs, p00, p10, p11);
                addTriangle(triangles, crs, p00, p11, p01);
            }
        }
        return GeometryFactory.DEFAULT.createTIN(crs, triangles.toArray(Triangle[]::new));
    }

    /**
     * Adds the triangle of the given vertices, unless two of them are at the same position.
     */
    private static void addTriangle(final List<Triangle> triangles, final CoordinateReferenceSystem crs,
                                    final double[] p0, final double[] p1, final double[] p2)
    {
        if (same(p0, p1) || same(p1, p2) || same(p2, p0)) {
            return;
        }
        final int dim = p0.length;
        final double[] coordinates = new double[4 * dim];
        System.arraycopy(p0, 0, coordinates, 0,       dim);
        System.arraycopy(p1, 0, coordinates, dim,     dim);
        System.arraycopy(p2, 0, coordinates, 2 * dim, dim);
        System.arraycopy(p0, 0, coordinates, 3 * dim, dim);
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        triangles.add(factory.createTriangle(factory.createLinearRing(factory.createDataPoints(crs, coordinates))));
    }

    /**
     * Returns whether the two positions are equal within a tolerance relative to their magnitude,
     * since the poles of a surface of revolution are computed with rounding errors.
     */
    private static boolean same(final double[] p, final double[] q) {
        double scale = 0;
        for (int i = 0; i < p.length; i++) {
            scale = Math.max(scale, Math.max(Math.abs(p[i]), Math.abs(q[i])));
        }
        return distance(p, q) <= 1E-12 * Math.max(1, scale);
    }
}
