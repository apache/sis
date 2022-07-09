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

import java.util.List;
import org.apache.sis.image.DataType;
import org.apache.sis.coverage.SampleDimension;


/**
 * A grid coverage which is derived from a single source coverage,
 * The default implementations of methods in this class assume that this derived coverage
 * uses the same sample dimensions than the source coverage. If it is not the case, then
 * some methods may need to be overridden.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
abstract class DerivedGridCoverage extends GridCoverage {
    /**
     * The source grid coverage.
     */
    protected final GridCoverage source;

    /**
     * Constructs a new grid coverage which is derived from the given source.
     * The new grid coverage share the same sample dimensions than the source.
     *
     * @param  source  the source from which to copy the sample dimensions.
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     */
    DerivedGridCoverage(final GridCoverage source, final GridGeometry domain) {
        super(source, domain);
        this.source = source;
    }

    /**
     * Constructs a new grid coverage which is derived from the given source.
     * The new grid coverage share the same grid geometry than the source.
     * Subclasses which use this constructor may need to override the following methods:
     * {@link #getBandType()}, {@link #evaluator()}.
     *
     * @param  source  the source from which to copy the grid geometry.
     * @param  ranges  sample dimensions for each image band.
     */
    DerivedGridCoverage(final GridCoverage source, final List<? extends SampleDimension> ranges) {
        super(source.getGridGeometry(), ranges);
        this.source = source;
    }

    /**
     * Returns the data type identifying the primitive type used for storing sample values in each band.
     * The default implementation returns the type of the source.
     */
    @Override
    DataType getBandType() {
        return source.getBandType();
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * That function accepts {@link DirectPosition} in arbitrary Coordinate Reference System;
     * conversions to grid indices are applied by the {@linkplain #source} as needed.
     *
     * @todo The results returned by {@link GridEvaluator#toGridCoordinates(DirectPosition)}
     *       would need to be transformed. But it would force us to return a wrapper, which
     *       would add an indirection level for all others (more important) method calls.
     *       Is it worth to do so?
     */
    @Override
    public GridEvaluator evaluator() {
        return source.evaluator();
    }
}
