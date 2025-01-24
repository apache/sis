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
package org.apache.sis.referencing.operation.builder;

import java.util.Iterator;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.resources.Errors;


/**
 * Computes a linear approximation of a {@link MathTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Linearizer {
    /**
     * Do not allow instantiation of this class.
     */
    private Linearizer() {
    }

    /**
     * Returns a linear approximation of the given transform for the specified domain.
     * The source positions are integer coordinates included in the given envelope.
     * The target positions are the results of transforming source coordinates with
     * the given {@code gridToCRS} transform.
     *
     * <p>If a linear approximation can be extracted from the given transform, this method returns
     * that approximation directly. This method tries to avoid expensive calculation; it searches
     * for transforms that can be processed easily.</p>
     *
     * @param  gridToCRS  the transform from source coordinates (grid indices) to target coordinates.
     * @param  domain     domain of integer source coordinates for which to get a linear approximation.
     *                    Both lower and upper coordinate values are <em>inclusive</em>.
     * @return a linear approximation of given transform for the specified domain.
     * @throws FactoryException if the transform approximation cannot be computed.
     */
    static MathTransform approximate(final MathTransform gridToCRS, final Envelope domain)
            throws TransformException, FactoryException
    {
        if (domain.getDimension() != ResidualGrid.SOURCE_DIMENSION) {
            return compute(gridToCRS, domain, null);
        }
        MathTransform result = null;
        final Iterator<MathTransform> it = MathTransforms.getSteps(gridToCRS).iterator();
        while (it.hasNext()) {
            MathTransform step = it.next();
            if (step instanceof LinearTransform) {
                result = (result != null) ? MathTransforms.concatenate(result, step) : step;
            } else if (step instanceof InterpolatedTransform) {
                /*
                 * Non-linear transform found. If it is backed by a `ResidualGrid` and the specified domain
                 * contains the full `ResidualGrid` domain, then we consider that `step` is approximated by
                 * an identity transform. Otherwise this method cannot process `gridToCRS`.
                 */
                final DatumShiftGrid<?,?> grid = ((InterpolatedTransform) step).getShiftGrid();
                MathTransform sourceToGrid = grid.getCoordinateToGrid();
                if (result != null) {
                    sourceToGrid = MathTransforms.concatenate(result, sourceToGrid);
                }
                final GeneralEnvelope gd = Envelopes.transform(sourceToGrid, domain);
checkResidual:  if (grid instanceof ResidualGrid) {
                    /*
                     * Verifiy pixel coordinates as if they were rounded to nearest integers and assuming that
                     * the envelope maximal values are inclusive (this is why we subtract 1.5 instead of 0.5).
                     * If the specified envelope is inside the `ResidualGrid` domain, then the approximation
                     * should be computed using a subset of the points included in this grid.
                     */
                    for (int i=0; i<ResidualGrid.SOURCE_DIMENSION; i++) {
                        if (gd.getMinimum(i) >= 0.5 || gd.getMaximum(i) <= grid.getGridSize(i) - 1.5) {
                            break checkResidual;
                        }
                    }
                    /*
                     * The `step` transform matches criterion. Consider that we can approximate this step by the
                     * identity transform. If we applied `LinearTransformBuilder` on this step, we should indeed
                     * get a result close to identity transform because of the way that `LocalizationGridBuilder`
                     * computes `ResidualGrid`: if we invoke `LinearTransformBuilder.setControlPoints(gridToCRS)`
                     * we would reconstitute the same set of points than the one that `LocalizationGridBuilder`
                     * used for computing a linear approximation before to compute the `ResidualGrid`.
                     */
                    continue;
                }
                /*
                 * The given domain does not use all grid points, or the grid is not a type that we known to be
                 * close to identity. We have to compute the approximation. But instead of computing it on all
                 * points in source domain, we use only the point in the grid. It can be much less computation.
                 */
                while (it.hasNext()) {
                    // Complete the transform will all remaining steps.
                    step = MathTransforms.concatenate(step, it.next());
                }
                return MathTransforms.concatenate(sourceToGrid, compute(step, gd, grid));
            } else {
                /*
                 * Non-linear transform of unknown type. There is no optimization we can do.
                 */
                return compute(gridToCRS, domain, null);
            }
        }
        return result;
    }

    /**
     * Computes a linear approximation of the given transform. This is an expensive fallback used only
     * when we could not find an existing value by inspection of {@code gridToCRS} transform steps.
     *
     * @param  gridToCRS  the transform from source coordinates (grid indices) to target coordinates.
     * @param  domain     domain of integer source coordinates for which to get a linear approximation.
     *                    Both lower and upper coordinate values are <em>inclusive</em>.
     */
    private static MathTransform compute(final MathTransform gridToCRS, final Envelope domain,
            final DatumShiftGrid<?,?> grid) throws TransformException, FactoryException
    {
        final int[] size = new int[domain.getDimension()];
        final double[] shift = new double[size.length];
        for (int i=0; i<size.length; i++) {
            double lower = Math.rint(domain.getMinimum(i));
            double upper = Math.rint(domain.getMaximum(i)) + 1;     // Make exclusive.
            if (grid != null) {
                final double h = grid.getGridSize(i);
                if (upper > h) upper = h;
                if (lower < 0) lower = 0;
            }
            final double span = upper - lower;
            if (!(span <= Integer.MAX_VALUE)) {                               // Use `!` for catching NaN.
                throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
            }
            shift[i] = lower;
            size [i] = (int) span;
        }
        final var builder = new LinearTransformBuilder(size);
        final LinearTransform translate = MathTransforms.translation(shift);
        builder.setControlPoints(MathTransforms.concatenate(translate, gridToCRS));
        return MathTransforms.concatenate(translate.inverse(), builder.create(null));
    }
}
