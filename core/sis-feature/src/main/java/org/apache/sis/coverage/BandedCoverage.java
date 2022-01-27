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
package org.apache.sis.coverage;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A coverage where all sample values at a given location can be provided in an array of primitive type.
 * This class does not require sample values to be "physically" stored in different bands,
 * but it enforces similar constraints:
 *
 * <ul>
 *   <li>Sample values are represented by a primitive type, typically {@code byte}, {@code short} or {@code float}.</li>
 *   <li>All sample dimensions (bands) use the same primitive type.</li>
 *   <li>Sample dimensions (bands) are accessed by band index with the first band at index 0.</li>
 * </ul>
 *
 * <div class="note"><b>Comparison with ISO 19123:</b>
 * by contrast an ISO {@code Coverage} does not restrict sample values to primitive types,
 * does not require all sample dimensions to use the same type,
 * and sample values are accessed by field names instead of band indices.
 * Said otherwise, an ISO {@code Coverage} can provide a complex structure (a {@link org.opengis.util.Record})
 * at every location while this {@code BandedCoverage} class provides only primitive arrays such as {@code float[]}.
 * </div>
 *
 * The effect of above restrictions appears in {@link #getSampleDimensions()} and
 * {@link Evaluator#apply(DirectPosition)} method signatures.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public abstract class BandedCoverage {
    /**
     * Constructs a coverage.
     */
    protected BandedCoverage() {
    }

    /**
     * Returns the coordinate reference system to which the cells are referenced.
     *
     * @return the coordinate reference system to which the cells are referenced.
     *
     * @since 1.2
     */
    public abstract CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Returns the bounding box for the coverage domain in CRS coordinates.
     * The envelope encompasses all cell surfaces, from the left border of leftmost cell
     * to the right border of the rightmost cell and similarly along other axes.
     *
     * <p>For most common cases, the envelope should be present.
     * However, the return value may be empty in cases like:</p>
     * <ul>
     *   <li>
     *     Functional dataset: in case of a computed resource, the coverage could be potentially valid
     *     in an infinite extent (repeating pattern, random numbers for tests, <i>etc.</i>).
     *   </li><li>
     *     Computational cost: if obtaining the overall envelope is too costly,
     *     an implementation might decide to leave the result empty instead of returning a too approximate envelope.
     *     For example, if a coverage aggregates a lot of data (by dynamically choosing data in a catalog upon evaluation),
     *     it might rather not compute envelope union for the entire catalog.
     *   </li><li>
     *     When the function does not have a clear boundary for its domain of validity,
     *     for example because the sample values accuracy decreases progressively with distance.
     *   </li>
     * </ul>
     *
     * @return the bounding box for the coverage domain in CRS coordinates if available.
     *
     * @since 1.2
     */
    public abstract Optional<Envelope> getEnvelope();

    /**
     * Returns information about the <cite>range</cite> of this coverage.
     * Information include names, sample value ranges, fill values and transfer functions for all bands in this coverage.
     * The length of the returned list should be equal to the {@linkplain java.awt.image.SampleModel#getNumBands() number
     * of bands} in rendered images.
     *
     * @return names, value ranges, fill values and transfer functions for all bands in this grid coverage.
     */
    public abstract List<SampleDimension> getSampleDimensions();

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * That function accepts {@link DirectPosition} in arbitrary Coordinate Reference System;
     * conversions to the coverage reference system are applied as needed.
     *
     * <h4>Multi-threading</h4>
     * {@code Evaluator}s are not thread-safe. For computing sample values concurrently,
     * a new {@link Evaluator} instance should be created for each thread by invoking this
     * method multiply times.
     *
     * @return a new function for computing or interpolating sample values.
     */
    public abstract Evaluator evaluator();

    /**
     * Computes or interpolates values of sample dimensions at given positions.
     * Values are computed by calls to {@link #apply(DirectPosition)} and are returned as {@code double[]}.
     *
     * <h2>Multi-threading</h2>
     * Evaluators are not thread-safe. An instance of {@code Evaluator} should be created
     * for each thread that need to compute sample values.
     *
     * @author  Johann Sorel (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     *
     * @see BandedCoverage#evaluator()
     *
     * @since 1.1
     * @module
     */
    public interface Evaluator extends Function<DirectPosition, double[]> {
        /**
         * Returns the coverage from which this evaluator is computing sample values.
         *
         * @return the source of sample values for this evaluator.
         */
        BandedCoverage getCoverage();

        /**
         * Returns whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
         * The default value is {@code false}, which means that the default {@link #apply(DirectPosition)} behavior is to
         * throw {@link PointOutsideCoverageException} for points outside bounds.
         *
         * @return whether {@link #apply(DirectPosition)} return {@code null} for points outside coverage bounds.
         */
        boolean isNullIfOutside();

        /**
         * Sets whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
         * The default value is {@code false}. Setting this flag to {@code true} may improve performances if the caller
         * expects that many points will be outside coverage bounds, since it reduces the amount of exceptions to be thrown.
         *
         * @param  flag  whether {@link #apply(DirectPosition)} should use {@code null} return value instead of
         *               {@link PointOutsideCoverageException} for signaling that a point is outside coverage bounds.
         */
        void setNullIfOutside(boolean flag);

        /**
         * Returns a sequence of double values for a given point in the coverage.
         * The CRS of the given point may be any coordinate reference system;
         * coordinate conversions will be applied as needed.
         * If the CRS of the point is undefined, then it is assumed to be the {@linkplain #getCoverage() coverage} CRS.
         * The returned sequence includes a value for each {@linkplain SampleDimension sample dimension}.
         *
         * @param  point   the coordinate point where to evaluate.
         * @return the sample values at the specified point, or {@code null} if the point is outside the coverage.
         *         For performance reason, this method may return the same array
         *         on every method call by overwriting previous values.
         *         Callers should not assume that the array content stay valid for a long time.
         * @throws PointOutsideCoverageException if the evaluation failed because the input point
         *         has invalid coordinates and the {@link #isNullIfOutside()} flag is {@code false}.
         * @throws CannotEvaluateException if the values can not be computed at the specified coordinates
         *         for another reason. For example this exception may be thrown if the coverage data type
         *         can not be converted to {@code double} by an identity or widening conversion.
         */
        @Override
        double[] apply(DirectPosition point) throws CannotEvaluateException;
    }
}
