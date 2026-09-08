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
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.NurbCurve;
import org.apache.sis.geometries.operation.DeBoorAlgorithm;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;

/**
 * A curve defined by control points, weights, a knot vector and a degree.
 *
 * Depending on the given values it is a Bézier curve, a B-spline or a NURBS:
 * the weights make it rational, and the knot vector multiplicities make it clamped or periodic.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultNurbCurve extends AbstractGeometry implements NurbCurve {

    final DataPoints points;
    final Array controlPointsArray;
    final Vector<?>[] controlPoints;
    final int nbCtrlPts;
    final double[] weights;
    final double[] knots;
    final int degree;

    public DefaultNurbCurve(final DataPoints points, final double[] weights, final double[] knots, final int degree) {
        this.points = points;
        this.controlPointsArray = points.getAttributeArray(AttributesType.ATT_POSITION);
        this.controlPoints = controlPointsArray.toArray();
        this.weights = weights;
        this.knots = knots;
        this.degree = degree;
        this.nbCtrlPts = (int) this.controlPointsArray.getLength();
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public double[] getKnots() {
        return knots.clone();
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public Array getControlPoints() {
        return controlPointsArray;
    }

    Vector getControlPoint(int index) {
        return Vectors.castOrWrap(controlPoints[index]);
    }

    /**
     * Point C(u) of the curve at the given parameter.
     */
    public Vector<?> evaluate(final double u) {
        final int spatialDim = controlPoints.length;
        final Vector<?>[] hom = new Vector<?>[nbCtrlPts];
        for (int i = 0; i < nbCtrlPts; i++) {
            hom[i] = toHomogeneous(getControlPoint(i), weights[i]);
        }
        final Vector<?> result = DeBoorAlgorithm.evaluate(u, hom, knots, degree);
        return fromHomogeneous(result, spatialDim);
    }

    /**
     * First derivative of a curve (Bézier, B-spline or NURBS) at parameter u.
     *
     * Principle: the homogeneous curve Cw(u) = Σ N_i,p(u) Pw_i (weighted control points, dimension N+1) is an ordinary
     * NON rational B-spline. Its derivative Cw'(u) is itself a B-spline of degree p-1 whose control points are computed
     * directly (the classical knot-weighted finite difference formula, without resorting to numerical differentiation).
     *
     * The quotient rule is then applied in order to "divide" by the weight:
     *
     * C(u) = A(u) / w(u) C'(u) = (A'(u) - w'(u) C(u)) / w(u)
     *
     * where A(u) is the spatial part of Cw(u) and w(u) its last component (the homogeneous weight).
     * If all weights are 1 (Bézier / non rational B-spline), then w'(u)=0 and we simply fall back on C'(u) = A'(u).
     */
    public Vector<?> derivative(final double u) {
        final int spatialDim = controlPoints[0].getDimension();
        final Vector<?>[] hom = new Vector<?>[nbCtrlPts];
        for (int i = 0; i < nbCtrlPts; i++) {
            hom[i] = toHomogeneous(getControlPoint(i), weights[i]);
        }

        final Vector<?> Cw = DeBoorAlgorithm.evaluate(u, hom, knots, degree);
        final double w = Cw.get(spatialDim);
        final double[] Cu = new double[spatialDim];
        for (int c = 0; c < spatialDim; c++) {
            Cu[c] = Cw.get(c) / w;
        }

        if (degree == 0) {
            return Vectors.createDouble(spatialDim); // constant curve -> null derivative
        }
        final Vector<?>[] derivCtrl = DeBoorAlgorithm.derivativeControlPoints(hom, knots, degree);
        final double[] derivKnots = Arrays.copyOfRange(knots, 1, knots.length - 1);
        final Vector<?> Cwp = DeBoorAlgorithm.evaluate(u, derivCtrl, derivKnots, degree - 1);

        final double wp = Cwp.get(spatialDim);
        final double[] result = new double[spatialDim];
        for (int c = 0; c < spatialDim; c++) {
            result[c] = (Cwp.get(c) - wp * Cu[c]) / w;
        }
        return Vectors.createDouble(result.length).set(result);
    }

    public double domainStart() {
        return knots[degree];
    }

    public double domainEnd() {
        return knots[nbCtrlPts];
    }

    // ------------------------------------------------------------------
    // Subdivision (Boehm's algorithm: knot insertion)
    // ------------------------------------------------------------------
    /**
     * Returns an equivalent curve, of the same degree and describing exactly the same shape, in which the given knot
     * has been inserted the given number of times.
     *
     * @param u knot value to insert
     * @param times how many times to insert it
     * @return the refined curve
     */
    public DefaultNurbCurve insertKnot(final double u, final int times) {
        Vector<?>[] ctrl = controlPoints;
        double[] w8 = weights;
        double[] kn = knots;
        final int p = degree;
        final int spatialDim = controlPoints[0].getDimension();

        for (int t = 0; t < times; t++) {
            final int n = ctrl.length - 1;
            final int k = DeBoorAlgorithm.findKnotSpan(u, kn, p, n);
            final int s = knotMultiplicity(kn, u);

            final Vector<?>[] Pw = new Vector<?>[ctrl.length];
            for (int i = 0; i < ctrl.length; i++) {
                Pw[i] = toHomogeneous(ctrl[i], w8[i]);
            }

            final Vector<?>[] Qw = new Vector<?>[Pw.length + 1];
            for (int i = 0; i <= k - p; i++) {
                Qw[i] = Pw[i];
            }
            for (int i = k - p + 1; i <= k - s; i++) {
                final double alpha = (u - kn[i]) / (kn[i + p] - kn[i]);
                Qw[i] = Pw[i - 1].copy().lerp(Pw[i], alpha);
            }
            for (int i = k - s; i <= n; i++) {
                Qw[i + 1] = Pw[i];
            }

            final double[] newKnots = new double[kn.length + 1];
            System.arraycopy(kn, 0, newKnots, 0, k + 1);
            newKnots[k + 1] = u;
            System.arraycopy(kn, k + 1, newKnots, k + 2, kn.length - (k + 1));

            final Vector<?>[] newCtrl = new Vector<?>[Qw.length];
            final double[] newWeights = new double[Qw.length];
            for (int i = 0; i < Qw.length; i++) {
                final double w = Qw[i].get(spatialDim);
                newWeights[i] = w;
                final double[] arr = new double[spatialDim];
                for (int c = 0; c < spatialDim; c++) {
                    arr[c] = Qw[i].get(c) / w;
                }
                newCtrl[i] = Vectors.createDouble(arr.length).set(arr);
            }

            ctrl = newCtrl;
            w8 = newWeights;
            kn = newKnots;
        }

        final DataPoints ctrlDp = new ArrayDataPoints(
                NDArrays.of(List.of(ctrl),
                controlPointsArray.getSampleSystem(),
                controlPointsArray.getDataType()));
        return new DefaultNurbCurve(ctrlDp, w8, kn, p);
    }

    /**
     * Splits this curve in two halves at the given parameter.
     * The knot is first inserted enough times for its multiplicity to reach the degree,
     * after which the control points can simply be shared between the two halves.
     *
     * @param u parameter at which to split the curve
     * @return the two halves, in parameter order
     */
    public DefaultNurbCurve[] subdivide(final double u) {
        final int p = degree;
        final int s = knotMultiplicity(knots, u);

        final DefaultNurbCurve refined = (s < p) ? insertKnot(u, p - s) : this;

        final double[] kn = refined.knots;
        final int lastIdx = lastIndexOf(kn, u);
        final int a = lastIdx - p;

        final Vector<?>[] leftCtrl = Arrays.copyOfRange(refined.controlPoints, 0, a + 1);
        final double[] leftWeights = Arrays.copyOfRange(refined.weights, 0, a + 1);
        final double[] leftKnots = new double[lastIdx + 2];
        System.arraycopy(kn, 0, leftKnots, 0, lastIdx + 1);
        leftKnots[lastIdx + 1] = u;

        final int n = refined.controlPoints.length - 1;
        final Vector<?>[] rightCtrl = Arrays.copyOfRange(refined.controlPoints, a, n + 1);
        final double[] rightWeights = Arrays.copyOfRange(refined.weights, a, n + 1);
        final int rightStart = a + 1;
        final double[] rightKnots = new double[kn.length - rightStart + 1];
        rightKnots[0] = u;
        System.arraycopy(kn, rightStart, rightKnots, 1, kn.length - rightStart);

        final DataPoints leftDp = new ArrayDataPoints(
                NDArrays.of(List.of(leftCtrl),
                controlPointsArray.getSampleSystem(),
                controlPointsArray.getDataType()));
        final DataPoints rightDp = new ArrayDataPoints(
                NDArrays.of(List.of(rightCtrl),
                controlPointsArray.getSampleSystem(),
                controlPointsArray.getDataType()));

        return new DefaultNurbCurve[]{
            new DefaultNurbCurve(leftDp, leftWeights, leftKnots, p),
            new DefaultNurbCurve(rightDp, rightWeights, rightKnots, p)
        };
    }

    /**
     * Bounding box of the control points, which contains the curve (convex hull property).
     *
     * @return the bounding box of this curve
     */
    @Override
    public BBox getEnvelope() {
        final BBox bbox = new BBox(controlPoints[0], controlPoints[0]);
        for (final Vector<?> p : controlPoints) {
            bbox.add(p);
        }
        return bbox;
    }

    private static int knotMultiplicity(final double[] knots, final double u) {
        int count = 0;
        for (final double k : knots) {
            if (k == u) {
                count++;
            }
        }
        return count;
    }

    private static int lastIndexOf(final double[] knots, final double u) {
        for (int i = knots.length - 1; i >= 0; i--) {
            if (knots[i] == u) {
                return i;
            }
        }
        throw new IllegalArgumentException("u=" + u + " is not a knot of the vector");
    }

    // ------------------------------------------------------------------
    // Homogeneous coordinates
    //
    // A NURBS is evaluated as a non rational B-spline on the weighted points
    // (x*w, …, w), which is what makes DeBoorAlgorithm usable as-is; the
    // spatial point is recovered by dividing by that last component.
    // ------------------------------------------------------------------
    static Vector<?> toHomogeneous(final Vector<?> p, final double w) {
        return p.extend(1).scale(w);
    }

    static Vector<?> fromHomogeneous(final Vector<?> h, final int spatialDim) {
        return h.shrink(spatialDim).scale(1.0 / h.get(spatialDim));
    }
}
