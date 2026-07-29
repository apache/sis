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
package org.apache.sis.storage.rs;

import java.util.function.Function;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.storage.coverage.BandedCoverageExt;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A coverage which is structured by a collection of locations defined by a ReferenceSystem.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class CodedCoverage extends BandedCoverageExt {

    /**
     * Returns the coverage geometry.
     *
     * @return geometry of the coverage
     */
    public abstract CodedGeometry getGeometry();

    /**
     * Create an iterator over the coverage locations.
     * No assumption should be made on the iteration order.
     *
     * @return iterator, not null
     */
    public abstract CodeIterator createIterator();

    /**
     * Create a writable iterator over the coverage locations.
     * No assumption should be made on the iteration order.
     *
     * @return writable iterator, not null
     * @throws UnsupportedOperationException if coverage do not support writing
     */
    public abstract WritableCodeIterator createWritableIterator();

    /**
     * Creates a new function for computing or interpolating sample values at given codes.
     * That function accepts {@link Code} in arbitrary Reference System;
     * conversions to the coverage reference system are applied as needed.
     *
     * <h4>Multi-threading</h4>
     * {@code Evaluator}s are not thread-safe. For computing sample values concurrently,
     * a new {@code Evaluator} instance should be created for each thread by invoking this
     * method multiply times.
     *
     * @return a new function for computing or interpolating sample values.
     */
    public CodeEvaluator codeEvaluator() {
        return new CodedEvaluator(evaluator());
    }

    /**
     * Computes or interpolates values of sample dimensions at given positions.
     * Values are computed by calls to {@link #apply(Code)} and are returned as {@code double[]}.
     *
     * <h2>Multi-threading</h2>
     * Evaluators are not thread-safe. An instance of {@code Evaluator} should be created
     * for each thread that need to compute sample values.
     *
     * @author  Johann Sorel (Geomatys)
     */
    public interface CodeEvaluator extends Function<Code, double[]> {
        /**
         * Returns the coverage from which this evaluator is computing sample values.
         * This is <em>usually</em> the instance on which the {@link CodedCoverage#codeEvaluator()}
         * method has been invoked, but not necessarily. Evaluators are allowed to fetch values
         * from a different source for better performances or accuracies.
         *
         * @return the source of sample values for this evaluator.
         */
        CodedCoverage getCoverage();

        /**
         * Returns whether to return {@code null} instead of throwing an exception if a code is outside coverage bounds.
         * The default value is {@code false}, which means that the default {@link #apply(Code)} behavior is to
         * throw {@link PointOutsideCoverageException} for points outside bounds.
         *
         * @return whether {@link #apply(Code)} return {@code null} for codes outside coverage bounds.
         */
        boolean isNullIfOutside();

        /**
         * Sets whether to return {@code null} instead of throwing an exception if a code is outside coverage bounds.
         * The default value is {@code false}. Setting this flag to {@code true} may improve performances if the caller
         * expects that many points will be outside coverage bounds, since it reduces the number of exceptions to be thrown.
         *
         * @param  flag  whether {@link #apply(Code)} should use {@code null} return value instead of
         *               {@link PointOutsideCoverageException} for signaling that a code is outside coverage bounds.
         */
        void setNullIfOutside(boolean flag);

        /**
         * Returns {@code true} if this evaluator is allowed to wraparound coordinates that are outside the coverage.
         * The initial value is {@code false}. This method may continue to return {@code false} even after a call to
         * {@code setWraparoundEnabled(true)} if no wraparound axis has been found in the coverage CRS,
         * or if automatic wraparound is not supported.
         *
         * @return {@code true} if this evaluator may wraparound coordinates that are outside the coverage.
         *
         * @since 1.3
         */
        boolean isWraparoundEnabled();

        /**
         * Specifies whether this evaluator is allowed to wraparound coordinates that are outside the coverage.
         * If {@code true} and if a given coordinate is outside the coverage, then this evaluator may translate
         * the point along a wraparound axis in an attempt to get the point inside the coverage. For example, if
         * the coverage CRS has a longitude axis, then the evaluator may translate the longitude value by a
         * multiple of 360°.
         *
         * @param  allow  whether to allow wraparound of coordinates that are outside the coverage.
         *
         * @since 1.3
         */
        void setWraparoundEnabled(final boolean allow);

        /**
         * Returns a sequence of double values for a given code in the coverage.
         * The RS of the given point may be any reference system;
         * code conversions will be applied as needed.
         * If the RS of the code is undefined, then it is assumed to be the {@linkplain #getCoverage() coverage} RS.
         * The returned sequence includes a value for each {@linkplain SampleDimension sample dimension}.
         *
         * @param  code   the position where to evaluate.
         * @return the sample values at the specified code, or {@code null} if the point is outside the coverage.
         *         For performance reason, this method may return the same array
         *         on every method call by overwriting previous values.
         *         Callers should not assume that the array content stay valid for a long time.
         * @throws PointOutsideCoverageException if the evaluation failed because the input point
         *         has invalid coordinates and the {@link #isNullIfOutside()} flag is {@code false}.
         * @throws CannotEvaluateException if the values cannot be computed at the specified coordinates
         *         for another reason. For example, this exception may be thrown if the coverage data type
         *         cannot be converted to {@code double} by an identity or widening conversion.
         */
        @Override
        double[] apply(Code code) throws CannotEvaluateException;
    }


    private final class CodedEvaluator implements CodeEvaluator {

        private final Evaluator ceval;

        public CodedEvaluator(Evaluator ceval) {
            this.ceval = ceval;
        }

        @Override
        public CodedCoverage getCoverage() {
            return (CodedCoverage) ceval.getCoverage();
        }

        @Override
        public boolean isNullIfOutside() {
            return ceval.isNullIfOutside();
        }

        @Override
        public void setNullIfOutside(boolean flag) {
            ceval.setNullIfOutside(flag);
        }

        @Override
        public boolean isWraparoundEnabled() {
            return ceval.isWraparoundEnabled();
        }

        @Override
        public void setWraparoundEnabled(boolean allow) {
            ceval.setWraparoundEnabled(allow);
        }

        @Override
        public double[] apply(Code code) throws CannotEvaluateException {
            try {
                return ceval.apply(code.toDirectPosition());
            } catch (TransformException | FactoryException ex) {
                throw new CannotEvaluateException(ex.getMessage(), ex);
            }
        }

    }
}
