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
package org.apache.sis.referencing.operation.projection;

import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.operation.matrix.Matrix2;

import static java.lang.Math.abs;


/**
 * A temporary Jacobian matrix where to write the derivative of a forward projection.
 * This Jacobian matrix is used for calculation of inverse projection when no inverse
 * formulas is available, or when the inverse formula is too approximate (for example
 * because eccentricity is too high). This class processes as below:
 *
 * <ol>
 *   <li>Given a first estimation of (λ,φ), compute the forward projection (x′,y′)
 *       for that estimation together with the Jacobian matrix at that position.</li>
 *   <li>Compute the errors compared to the specified (x,y) values.</li>
 *   <li>Convert that (Δx,Δy) error into a (Δλ,Δφ) error using the inverse of the Jacobian matrix.</li>
 *   <li>Correct (λ,φ) and continue iteratively until the error is small enough.</li>
 * </ol>
 *
 * This algorithm described in EPSG guidance note for {@link Orthographic} projection
 * but can be applied to any map projection, not only orthographic.
 *
 * <p>This algorithm is defined in a {@link Matrix2} subclass for allowing map projection
 * implementations to use {@code if (derivative instanceof Inverter)} check for detecting
 * when a {@code transform} method is invoked for the purpose of an inverse projection.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-478">SIS-478</a>
 *
 * @since 1.1
 * @module
 */
@SuppressWarnings({"CloneableImplementsClone", "serial"})
final class Inverter extends Matrix2 {
    /**
     * Creates a new matrix initialized to identity.
     */
    Inverter() {
    }

    /**
     * Computes the inverse of the given projection. Before this method is invoked,
     * the {@code dstPts[dstOff]} and {@code dstPts[dstOff+1]} array elements must
     * contain the initial λ and φ estimations respectively. After method returns,
     * the same array elements contain the refined λ and φ values.
     *
     * <p>Note: restricted to {@link Orthographic} projection for now,
     * but may be generalized to any projection in a future version.</p>
     *
     * @param  projection  the forward projection for which to compute an inverse projection.
     * @param  x           the  easting value from {@code srcPts[srcOff]}.
     * @param  y           the northing value from {@code srcPts[srcOff+1]}.
     * @param  dstPts      the array where to refine the (λ,φ) values.
     * @param  dstOff      index of the λ element in the {@code dstPts} array.
     * @throws ProjectionException if the iterative algorithm does not converge.
     */
    final void inverseTransform(final Orthographic projection, final double x, final double y,
            final double[] dstPts, final int dstOff) throws ProjectionException
    {
        double λ = dstPts[dstOff  ];
        double φ = dstPts[dstOff+1];
        for (int it=NormalizedProjection.MAXIMUM_ITERATIONS; --it >= 0;) {
            final double cosφ = projection.transform(dstPts, dstOff, dstPts, dstOff, this);
            final double ΔE   = x - dstPts[dstOff  ];
            final double ΔN   = y - dstPts[dstOff+1];
            final double D    = (m01 * m10) - (m00 * m11);    // Determinant.
            final double Δλ   = (m01*ΔN - m11*ΔE) / D;
            final double Δφ   = (m10*ΔE - m00*ΔN) / D;
            dstPts[dstOff  ]  = λ += Δλ;
            dstPts[dstOff+1]  = φ += Δφ;
            /*
             * Following condition uses ! for stopping iteration on NaN values.
             * We do not use Math.max(…) because we want to check NaN separately
             * for φ and λ.
             */
            if (!(abs(Δφ)      > NormalizedProjection.ANGULAR_TOLERANCE ||
                  abs(Δλ*cosφ) > NormalizedProjection.ANGULAR_TOLERANCE))
            {
                return;
            }
        }
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }
}
