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

import java.util.Map;
import java.util.Collection;
import java.util.stream.Stream;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

// Specific to the main branch:
import org.apache.sis.coverage.CannotEvaluateException;


/**
 * An evaluator which delegates all operations to another evaluator.
 * The default implementation of all methods except {@link #getCoverage()} delegates to the source evaluator.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class EvaluatorWrapper implements GridCoverage.Evaluator {
    /**
     * The evaluator provided by source coverage.
     * This is where all operations are delegated.
     */
    private final GridCoverage.Evaluator source;

    /**
     * Creates a new evaluator wrapper.
     *
     * @param  source  the evaluator to wrap.
     */
    EvaluatorWrapper(final GridCoverage.Evaluator source) {
        this.source = source;
    }

    /**
     * Returns whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
     */
    @Override
    public boolean isNullIfOutside() {
        return source.isNullIfOutside();
    }

    /**
     * Specifies whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
     */
    @Override
    public void setNullIfOutside(final boolean flag) {
        source.setNullIfOutside(flag);
    }

    /**
     * Returns {@code true} if this evaluator is allowed to wraparound coordinates that are outside the grid.
     */
    @Override
    public boolean isWraparoundEnabled() {
        return source.isWraparoundEnabled();
    }

    /**
     * Specifies whether this evaluator is allowed to wraparound coordinates that are outside the grid.
     */
    @Override
    public void setWraparoundEnabled(final boolean allow) {
        source.setWraparoundEnabled(allow);
    }

    /**
     * Returns the default slice where to perform evaluation, or an empty map if unspecified.
     * This method should be overridden if this evaluator has been created for a coverage
     * with a different grid geometry than the coverage of the wrapped evaluator.
     */
    @Override
    public Map<Integer, Long> getDefaultSlice() {
        return source.getDefaultSlice();
    }

    /**
     * Sets the default slice where to perform evaluation when the points do not have enough dimensions.
     * This method should be overridden if this evaluator has been created for a coverage
     * with a different grid geometry than the coverage of the wrapped evaluator.
     */
    @Override
    public void setDefaultSlice(Map<Integer, Long> slice) {
        source.setDefaultSlice(slice);
    }

    /**
     * Converts the specified geospatial position to grid coordinates.
     * This method should be overridden if this evaluator has been created for a coverage
     * with a different grid geometry than the coverage of the wrapped evaluator.
     */
    @Override
    public FractionalGridCoordinates toGridCoordinates(final DirectPosition point) throws TransformException {
        return source.toGridCoordinates(point);
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * This method should be overridden if this evaluator is for a coverage
     * doing some on-the-fly conversion of sample values.
     *
     * @param  point  the position where to evaluate.
     * @throws CannotEvaluateException if the values cannot be computed.
     */
    @Override
    public double[] apply(final DirectPosition point) throws CannotEvaluateException {
        return source.apply(point);
    }

    /**
     * Returns a stream of double values for given points in the coverage.
     * This method should be overridden if this evaluator is for a coverage
     * doing some on-the-fly conversion of sample values.
     *
     * @param  points   positions where to evaluate the sample values.
     * @param  parallel {@code true} for a parallel stream, or {@code false} for a sequential stream.
     */
    @Override
    public Stream<double[]> stream(Collection<? extends DirectPosition> points, boolean parallel) {
        return source.stream(points, parallel);
    }
}
