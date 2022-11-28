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
package org.apache.sis.storage;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.util.function.Function;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.image.DataType;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.internal.storage.ConvertedCoverageResource;
import org.apache.sis.measure.NumberRange;
import org.opengis.referencing.operation.MathTransform1D;

/**
 * A predefined set of operations on resources as convenience methods.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.3
 * @since 1.3
 * @module
 */
public class ResourceProcessor implements Cloneable {

    private final GridCoverageProcessor processor;

    /**
     * Creates a new processor with default configuration.
     */
    public ResourceProcessor() { this(null); }

    public ResourceProcessor(GridCoverageProcessor processor) {
        this.processor = processor == null ? new GridCoverageProcessor() : processor;
    }

    /**
     * @return The processor used internally to transform produced {@link GridCoverage grid coverages}. Not null.
     */
    public GridCoverageProcessor getProcessor() { return processor; }

    @Override
    public ResourceProcessor clone() {
        return new ResourceProcessor(processor.clone());
    }

    /**
     * Returns a coverage resource with sample values converted by the given functions.
     * The number of sample dimensions in the returned coverage is the length of the {@code converters} array,
     * which must be greater than 0 and not greater than the number of sample dimensions in the source coverage.
     * If the {@code converters} array length is less than the number of source sample dimensions,
     * then all sample dimensions at index ≥ {@code converters.length} will be ignored.
     *
     * <h4>Sample dimensions customization</h4>
     * By default, this method creates new sample dimensions with the same names and categories than in the
     * previous coverage, but with {@linkplain org.apache.sis.coverage.Category#getSampleRange() sample ranges}
     * converted using the given converters and with {@linkplain SampleDimension#getUnits() units of measurement}
     * omitted. This behavior can be modified by specifying a non-null {@code sampleDimensionModifier} function.
     * If non-null, that function will be invoked with, as input, a pre-configured sample dimension builder.
     * The {@code sampleDimensionModifier} function can {@linkplain SampleDimension.Builder#setName(CharSequence)
     * change the sample dimension name} or {@linkplain SampleDimension.Builder#categories() rebuild the categories}.
     *
     * <h4>Result relationship with source</h4>
     * If the source coverage is backed by a {@link java.awt.image.WritableRenderedImage},
     * then changes in the source coverage are reflected in the returned coverage and conversely.
     *
     * @see GridCoverageProcessor#convert(org.apache.sis.coverage.grid.GridCoverage, org.opengis.referencing.operation.MathTransform1D[], java.util.function.Function)
     * @see ImageProcessor#convert(RenderedImage, NumberRange[], MathTransform1D[], DataType, ColorModel)
     */
    public GridCoverageResource convert(final GridCoverageResource source, MathTransform1D[] converters,
            Function<SampleDimension.Builder, SampleDimension> sampleDimensionModifier)
    {
        return new ConvertedCoverageResource(source, converters, sampleDimensionModifier);
    }
}
