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

import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.operation.DeBoorAlgorithm;
import org.apache.sis.geometries.surface.BSplineSurfaceForm;
import org.apache.sis.geometries.surface.NurbSurface;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.Identifier;

/**
 * NURBS surface: tensor product of two directions (u, v), each one with its own degree and its own knot vector.
 * The control points form a grid controlPoints[i][j] (i = u direction, j = v direction).
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultNurbSurface extends AbstractGeometry implements NurbSurface {

    final Vector<?>[][] controlPoints;
    final double[][] weights;
    final double[] knotsU;
    final double[] knotsV;
    final int degree;

    public DefaultNurbSurface(final Vector<?>[][] controlPoints, final double[][] weights,
            final double[] knotsU, final double[] knotsV, final int degree) {
        this.controlPoints = controlPoints;
        this.weights = weights;
        this.knotsU = knotsU;
        this.knotsV = knotsV;
        this.degree = degree;
    }

    /**
     * Evaluates the point S(u,v) of the surface at the given parameters.
     *
     * Principle: the formula S(u,v) = ΣΣ N_i,p(u) N_j,q(v) Pw_ij is computed as two passes of the "classical" De Boor
     * algorithm in homogeneous coordinates: 1) for each row i, evaluate a curve along v -> one homogeneous point Q_i
     * per row (this "flattens" the v direction) 2) then evaluate a curve along u using the Q_i as homogeneous control
     * points and only dehomogenize at the very end — exactly the same principle as for curves, just applied twice.
     *
     * N_i,p(u) and N_j,q(v) vanish outside of their support, so only degreeU+1 rows and, within each of them, degreeV+1
     * columns actually contribute. They are selected first through {@link DeBoorAlgorithm#findKnotSpan}, then De Boor's
     * recursion is run on that small window only, through {@link DeBoorAlgorithm#evaluateWindow} — instead of walking
     * the whole grid. The cost is therefore O(degreeU × degreeV) instead of O(rows × cols), independently of the grid
     * size.
     */
    public Vector<?> evaluate(final double u, final double v) {
        final int rows = controlPoints.length;
        final int spatialDim = controlPoints[0][0].getDimension();

        final int ku = DeBoorAlgorithm.findKnotSpan(u, knotsU, degree, rows - 1);

        final Vector<?>[] windowU = new Vector<?>[degree + 1];
        for (int t = 0; t <= degree; t++) {
            final int i = ku - degree + t;
            windowU[t] = evaluateRowAlongV(controlPoints[i], weights[i], v, knotsV, degree);
        }

        final Vector<?> result = DeBoorAlgorithm.evaluateWindow(u, windowU, knotsU, degree, ku);
        return DefaultNurbCurve.fromHomogeneous(result, spatialDim);
    }

    /**
     * Evaluates, for a single row of the grid, the homogeneous point along v (degreeV+1 window only).
     */
    private static Vector<?> evaluateRowAlongV(final Vector<?>[] rowControlPoints, final double[] rowWeights,
            final double v, final double[] knotsV, final int degreeV) {
        final int cols = rowControlPoints.length;
        final int kv = DeBoorAlgorithm.findKnotSpan(v, knotsV, degreeV, cols - 1);

        final Vector<?>[] windowV = new Vector<?>[degreeV + 1];
        for (int t = 0; t <= degreeV; t++) {
            final int j = kv - degreeV + t;
            windowV[t] = DefaultNurbCurve.toHomogeneous(rowControlPoints[j], rowWeights[j]);
        }
        return DeBoorAlgorithm.evaluateWindow(v, windowV, knotsV, degreeV, kv);
    }

    /**
     * Partial derivatives {Su, Sv} at point (u,v) — the two vectors tangent to the surface.
     *
     * Same principle as for curves (differentiation of the homogeneous surface then the quotient rule), applied in each
     * direction:
     *
     * - For Su: the v direction is flattened first (as in {@link #evaluate}), which yields a homogeneous curve in u; it
     * is then differentiated with the same formula as for curves. - For Sv: symmetrically, u is flattened first, then
     * the differentiation happens in v.
     *
     * Unlike {@link #evaluate} (optimized to a local window), this method rebuilds the complete flattened curve in each
     * direction, because the differentiation formula needs the whole knot vector in order to stay correct at the window
     * boundaries.
     */
    public Vector<?>[] derivative(final double u, final double v) {
        final int rows = controlPoints.length;
        final int cols = controlPoints[0].length;
        final int spatialDim = controlPoints[0][0].getDimension();

        // Flatten v -> a homogeneous curve along u (one value per row)
        final Vector<?>[] alongU = new Vector<?>[rows];
        for (int i = 0; i < rows; i++) {
            final Vector<?>[] rowHom = new Vector<?>[cols];
            for (int j = 0; j < cols; j++) {
                rowHom[j] = DefaultNurbCurve.toHomogeneous(controlPoints[i][j], weights[i][j]);
            }
            alongU[i] = DeBoorAlgorithm.evaluate(v, rowHom, knotsV, degree);
        }
        final Vector<?> Sw = DeBoorAlgorithm.evaluate(u, alongU, knotsU, degree);
        final double w = Sw.get(spatialDim);
        final double[] Suv = new double[spatialDim];
        for (int c = 0; c < spatialDim; c++) {
            Suv[c] = Sw.get(c) / w;
        }

        final Vector<?> su = partialDerivative(u, alongU, knotsU, degree, w, Suv, spatialDim);

        // Flatten u -> a homogeneous curve along v (one value per column)
        final Vector<?>[] alongV = new Vector<?>[cols];
        for (int j = 0; j < cols; j++) {
            final Vector<?>[] colHom = new Vector<?>[rows];
            for (int i = 0; i < rows; i++) {
                colHom[i] = DefaultNurbCurve.toHomogeneous(controlPoints[i][j], weights[i][j]);
            }
            alongV[j] = DeBoorAlgorithm.evaluate(u, colHom, knotsU, degree);
        }
        final Vector<?> sv = partialDerivative(v, alongV, knotsV, degree, w, Suv, spatialDim);

        return new Vector<?>[]{su, sv};
    }

    /**
     * Differentiates the "flattened" homogeneous curve (already in homogeneous coordinates) and applies the quotient
     * rule.
     */
    private static Vector<?> partialDerivative(final double param, final Vector<?>[] flattenedHomogeneous, final double[] knots, final int degree,
            final double w, final double[] surfacePoint, final int spatialDim) {
        if (degree == 0) {
            return Vectors.createDouble(spatialDim);
        }

        final Vector<?>[] derivCtrl = DeBoorAlgorithm.derivativeControlPoints(flattenedHomogeneous, knots, degree);
        final double[] derivKnots = Arrays.copyOfRange(knots, 1, knots.length - 1);
        final Vector<?> deriv = DeBoorAlgorithm.evaluate(param, derivCtrl, derivKnots, degree - 1);

        final double wDeriv = deriv.get(spatialDim);
        final double[] result = new double[spatialDim];
        for (int c = 0; c < spatialDim; c++) {
            result[c] = (deriv.get(c) - wDeriv * surfacePoint[c]) / w;
        }
        return Vectors.createDouble(result.length).set(result);
    }

    /**
     * Unit normal at point (u,v) — defined for a 3D surface only.
     */
    public Vector<?> normal(final double u, final double v) {
        final Vector<?>[] d = derivative(u, v);
        final Vector<?> n = d[0].cross(d[1]);
        n.normalize();
        return n;
    }

    public double domainStartU() {
        return knotsU[degree];
    }

    public double domainEndU() {
        return knotsU[controlPoints.length];
    }

    public double domainStartV() {
        return knotsV[degree];
    }

    public double domainEndV() {
        return knotsV[controlPoints[0].length];
    }

    // ------------------------------------------------------------------
    // Subdivision
    // ------------------------------------------------------------------
    /**
     * Splits this surface in two halves along the u direction, at the given u value.
     * Trick: each column of the grid IS a curve in u,
     * so {@link DefaultNurbCurve#subdivide(double)} is reused directly, column by column.
     * The resulting left/right knot vector is the same for every column
     * (it depends only on u and on the original knot vector), so it can be collected once.
     *
     * @param u parameter at which to split the surface
     * @return the two halves, in parameter order
     */
    public DefaultNurbSurface[] subdivideU(final double u) {
        final int rows = controlPoints.length, cols = controlPoints[0].length;
        Vector<?>[][] leftCtrl = null, rightCtrl = null;
        double[][] leftW = null, rightW = null;
        double[] leftKnots = null, rightKnots = null;

        for (int j = 0; j < cols; j++) {
            final Vector<?>[] colCtrl = new Vector<?>[rows];
            final double[] colW = new double[rows];
            for (int i = 0; i < rows; i++) {
                colCtrl[i] = controlPoints[i][j];
                colW[i] = weights[i][j];
            }

            final DataPoints colDp = new ArrayDataPoints(
                NDArrays.of(List.of(colCtrl),
                colCtrl[0].getSampleSystem(),
                colCtrl[0].getDataType()));
            final DefaultNurbCurve[] halves = new DefaultNurbCurve(colDp, colW, knotsU, degree).subdivide(u);

            if (leftCtrl == null) {
                leftCtrl = new Vector<?>[halves[0].nbCtrlPts][cols];
                leftW = new double[halves[0].nbCtrlPts][cols];
                rightCtrl = new Vector<?>[halves[1].nbCtrlPts][cols];
                rightW = new double[halves[1].nbCtrlPts][cols];
                leftKnots = halves[0].knots;
                rightKnots = halves[1].knots;
            }
            for (int i = 0; i < halves[0].nbCtrlPts; i++) {
                leftCtrl[i][j] = halves[0].controlPoints[i];
                leftW[i][j] = halves[0].weights[i];
            }
            for (int i = 0; i < halves[1].nbCtrlPts; i++) {
                rightCtrl[i][j] = halves[1].controlPoints[i];
                rightW[i][j] = halves[1].weights[i];
            }
        }

        return new DefaultNurbSurface[]{
            new DefaultNurbSurface(leftCtrl, leftW, leftKnots, knotsV, degree),
            new DefaultNurbSurface(rightCtrl, rightW, rightKnots, knotsV, degree)
        };
    }

    /**
     * Same as {@link #subdivideU} but along the v direction (each ROW is a curve in v).
     */
    public DefaultNurbSurface[] subdivideV(final double v) {
        final int rows = controlPoints.length;
        Vector<?>[][] leftCtrl = null, rightCtrl = null;
        double[][] leftW = null, rightW = null;
        double[] leftKnots = null, rightKnots = null;

        for (int i = 0; i < rows; i++) {

            final DataPoints dp = new ArrayDataPoints(
                NDArrays.of(List.of(controlPoints[i]),
                controlPoints[i][0].getSampleSystem(),
                controlPoints[i][0].getDataType()));

            final DefaultNurbCurve[] halves = new DefaultNurbCurve(dp, weights[i], knotsV, degree).subdivide(v);

            if (leftCtrl == null) {
                leftCtrl = new Vector<?>[rows][halves[0].nbCtrlPts];
                leftW = new double[rows][halves[0].controlPoints.length];
                rightCtrl = new Vector<?>[rows][halves[1].nbCtrlPts];
                rightW = new double[rows][halves[1].controlPoints.length];
                leftKnots = halves[0].knots;
                rightKnots = halves[1].knots;
            }
            leftCtrl[i] = halves[0].controlPoints;
            leftW[i] = halves[0].weights;
            rightCtrl[i] = halves[1].controlPoints;
            rightW[i] = halves[1].weights;
        }

        return new DefaultNurbSurface[]{
            new DefaultNurbSurface(leftCtrl, leftW, knotsU, leftKnots, degree),
            new DefaultNurbSurface(rightCtrl, rightW, knotsU, rightKnots, degree)
        };
    }


    /**
     * Bounding box of the control points, which contains the surface (convex hull property).
     *
     * @return the bounding box of this surface
     */
    public BBox getEnvelope() {
        final BBox bbox = new BBox(controlPoints[0][0], controlPoints[0][0]);
        for (final Vector<?>[] row : controlPoints) {
            for (final Vector<?> p : row) {
                bbox.add(p);
            }
        }
        return bbox;
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public double[] getKnots() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public KnotType getKnotSpec() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BSplineSurfaceForm getSurfaceForm() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPolynomial() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getArea() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getRows() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getColumns() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DirectPosition> getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataPoints getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getHorizontalCurve(double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Curve getVerticalCurve(double u) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectPosition getSurface(double u, double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Identifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
