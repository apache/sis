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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.builder.LinearTransformBuilder;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.util.ArgumentChecks;


/**
 * Method for replacing a non-linear "grid to CRS" conversion by a linear conversion (affine transform).
 * The {@code GridGeometry} class allows non-linear {@linkplain GridGeometry#getGridToCRS "grid to CRS"}
 * conversions, but some {@code GridGeometry} usages are restricted to linear (affine) conversions.
 * The {@code DomainLinearizer} class encapsulates the method used for replacing non-linear conversions
 * by a linear approximation.
 *
 * <p>The same instance can be reused by invoking {@code apply(…)} methods for many {@link GridCoverage}
 * or {@link GridGeometry} instances.</p>
 *
 * <h2>Limitations</h2>
 * Current implementation is designed for two-dimensional grid geometries.
 * Support for higher dimensions is not guaranteed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see LinearTransformBuilder#approximate(MathTransform, Envelope)
 *
 * @since 1.1
 */
public class DomainLinearizer {
    /**
     * Whether to force lower grid coordinates to (0,0,…).
     *
     * @see #getGridStartsAtZero()
     */
    private boolean gridStartsAtZero;

    /**
     * Scale factor applied on grid coordinates, or 1 if none.
     *
     * @see #getScaleFactor()
     */
    private double scale = 1;

    /**
     * The processor to use for coverage ressampling, created when first needed.
     *
     * @see #processor()
     */
    private GridCoverageProcessor processor;

    /**
     * Creates a new linearizer.
     */
    public DomainLinearizer() {
    }

    /**
     * Returns whether {@code GridExtent} should have their lower grid coordinates set to zero.
     * If {@code true}, then {@code DomainLinearizer} will opportunistically apply translation
     * on the {@linkplain GridGeometry#getGridToCRS "grid to CRS"} conversion in such a way that
     * {@link GridExtent#getLow()} is 0 for all dimensions.
     *
     * @return whether to force lower grid coordinates to (0,0,…).
     *
     * @see GridExtent#startsAtZero()
     */
    public boolean getGridStartsAtZero() {
        return gridStartsAtZero;
    }

    /**
     * Sets whether {@code GridExtent} should have their lower grid coordinates set to zero.
     * The default value is {@code false}.
     *
     * @param  force  whether to force lower grid coordinates to (0,0,…).
     */
    public void setGridStartsAtZero(final boolean force) {
        gridStartsAtZero = force;
    }

    /**
     * Returns the scale factor applied on coordinates in all dimensions.
     *
     * @return scale factor applied on coordinates in all dimensions, or 1 if none.
     */
    public double getScaleFactor() {
        return scale;
    }

    /**
     * Sets the scale factor to apply on coordinates in all dimensions.
     * Must be a value greater than zero. The default value is 1.
     *
     * @param  factor  scale factor applied on coordinates in all dimensions, or 1 if none.
     */
    public void setScaleFactor(final double factor) {
        ArgumentChecks.ensureStrictlyPositive("factor", factor);
        scale = factor;
    }

    /**
     * Returns the grid coverage processor associated to this linearizer.
     */
    private GridCoverageProcessor processor() {
        if (processor == null) {
            processor = new GridCoverageProcessor();
        }
        return processor;
    }

    /**
     * Returns a grid coverage with a linear approximation of the <i>grid to CRS</i> conversion.
     * The linear approximation is computed by {@link #apply(GridGeometry)}. If the <i>grid to CRS</i>
     * conversion of the given coverage is already linear, then this method returns {@code coverage}.
     *
     * @param  coverage  the grid coverage in which to make the <i>grid to CRS</i> conversion linear.
     * @return a grid coverage with a linear approximation of the <i>grid to CRS</i> conversion.
     * @throws TransformException if some cell coordinates cannot be computed.
     */
    public GridCoverage apply(final GridCoverage coverage) throws TransformException {
        final GridGeometry gg = coverage.getGridGeometry();
        final GridGeometry linearized = apply(gg);
        if (gg.equals(linearized)) {
            return coverage;
        }
        return processor().resample(coverage, linearized);
    }

    /**
     * Creates a grid geometry with a linear approximation of the <i>grid to CRS</i> conversion.
     * The approximation is computed by <i>Least Mean Squares</i> method: the affine transform
     * coefficients are chosen in way making the average value of (<var>position</var> − <var>linear
     * approximation of position</var>)² as small as possible for all cells in given grid geometry.
     * If the <i>grid to CRS</i> conversion of the given grid geometry is already linear,
     * then this method returns {@code gg}.
     *
     * @param  gg  the grid geometry in which to make the <i>grid to CRS</i> conversion linear.
     * @return a grid geometry with a linear approximation of the <i>grid to CRS</i> conversion.
     * @throws TransformException if some cell coordinates cannot be computed.
     */
    public GridGeometry apply(final GridGeometry gg) throws TransformException {
        if (gg.nonLinears != 0) try {
            MathTransform   gridToCRS   = gg.requireGridToCRS(true);    // Map pixel center.
            GeneralEnvelope domain      = gg.extent.toEnvelope(true);   // Inclusive bounds.
            MathTransform   approximate = modify(LinearTransformBuilder.approximate(gridToCRS, domain));
            MathTransform   gridToGrid  = MathTransforms.concatenate(gridToCRS, approximate.inverse());
            domain = Envelopes.transform(gridToGrid, domain);
            final int dimension = domain.getDimension();
            final long[] coordinates = new long[dimension * 2];
            final double[] shift = new double[dimension];
            for (int i=0; i<dimension; i++) {
                long low  = Math.round(domain.getMinimum(i));
                long high = Math.round(domain.getMaximum(i));
                high = Math.max(low, Math.decrementExact(high));
                if (gridStartsAtZero) {
                    high = Math.subtractExact(high, low);
                    shift[i] = low;
                } else {
                    coordinates[i] = low;
                }
                coordinates[i + dimension] = high;
            }
            approximate = MathTransforms.concatenate(MathTransforms.translation(shift), approximate);
            if (!approximate.equals(gridToCRS)) {
                return new GridGeometry(new GridExtent(gg.extent, coordinates), PixelInCell.CELL_CENTER,
                                        approximate, gg.envelope.getCoordinateReferenceSystem());
            }
        } catch (FactoryException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof TransformException) {
                throw (TransformException) cause;
            }
            throw new TransformException(e);
        }
        return gg;
    }

    /**
     * Callback for custom modification of linear approximation. This method is invoked by {@link #apply(GridGeometry)}
     * after a linear "grid to CRS" approximation has been computed by the <i>Least Mean Squares</i> method.
     * Subclasses can override this method for example in order to scale the conversion by some arbitrary factor.
     *
     * <h4>Tip</h4>
     * Scales (if desired) should be applied <em>before</em> {@code gridToCRS}, i.e. on grid coordinates.
     * Scales applied after the transform (i.e. on "real world" coordinates) may give unexpected results
     * if the conversion contains a rotation.
     *
     * @param  gridToCRS  an approximation of the "grid to CRS" conversion computed by {@code DomainLinearizer}.
     * @return the approximation to use for creating a new {@link GridGeometry}. Should be linear.
     */
    private MathTransform modify(final LinearTransform gridToCRS) {
        final double[] factors = new double[gridToCRS.getTargetDimensions()];
        Arrays.fill(factors, scale);
        return MathTransforms.concatenate(MathTransforms.scale(factors), gridToCRS);
    }
}
