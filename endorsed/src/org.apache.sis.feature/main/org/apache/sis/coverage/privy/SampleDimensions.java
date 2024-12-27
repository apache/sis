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
package org.apache.sis.coverage.privy;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.awt.Shape;
import java.awt.image.RenderedImage;
import javax.measure.Unit;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.Category;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Static;


/**
 * Utility methods working on {@link SampleDimension} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SampleDimensions extends Static {
    /**
     * A hidden argument passed to some {@link ImageProcessor} operations.
     * Used for a parameter that we do not want to expose in the public API,
     * because {@link ImageProcessor} is not supposed to know grid coverages.
     * We may revisit in future Apache SIS version if we find a better way to
     * pass this information.
     *
     * This is used in:
     * <ul>
     *   <li>The <em>target</em> sample dimensions of a {@link org.apache.sis.image.BandedSampleConverter} image.</li>
     *   <li>The <em>target</em> sample dimensions of a {@link org.apache.sis.image.BandAggregateImage} image.</li>
     *   <li>The <em>target</em> sample dimensions of a {@link org.apache.sis.image.BandSelectImage} image.</li>
     *   <li>The <em>source</em> sample dimensions of a {@link org.apache.sis.image.Visualization} image.</li>
     * </ul>
     *
     * Usage pattern:
     *
     * {@snippet lang="java" :
     *     try {
     *         SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.set(dataRanges);
     *         return imageProcessor.doSomeStuff(...);
     *     } finally {
     *         SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.remove();
     *     }
     *     }
     *
     * The list in this thread-local variable should be unmodifiable.
     */
    public static final ThreadLocal<List<SampleDimension>> IMAGE_PROCESSOR_ARGUMENT = new ThreadLocal<>();

    /**
     * Do not allow instantiation of this class.
     */
    private SampleDimensions() {
    }

    /**
     * Returns the units of measurement for all bands of the given coverage.
     * The length of the returned array is the number of sample dimensions.
     * The array may contain {@code null} elements.
     *
     * @param  source  the coverage for which to get units of measurement.
     * @return the unit of measurement of all bands in the given coverage.
     */
    public static Unit<?>[] units(final BandedCoverage source) {
        final List<SampleDimension> bands = source.getSampleDimensions();
        final var units = new Unit<?>[bands.size()];
        for (int i=0; i<units.length; i++) {
            units[i] = bands.get(i).getUnits().orElse(null);
        }
        return units;
    }

    /**
     * Returns the range of sample values for all bands of the given coverage.
     * The length of the returned array is the number of sample dimensions.
     * The array may contain {@code null} elements.
     *
     * @param  source  the coverage for which to get sample value ranges.
     * @return the sample value ranges of all bands in the given coverage.
     */
    public static NumberRange<?>[] ranges(final BandedCoverage source) {
        final List<SampleDimension> bands = source.getSampleDimensions();
        final var ranges = new NumberRange<?>[bands.size()];
        for (int i=0; i<ranges.length; i++) {
            ranges[i] = bands.get(i).getSampleRange().orElse(null);
        }
        return ranges;
    }

    /**
     * Returns the background values of all bands in the given list.
     * The length of the returned array is the number of sample dimensions.
     * If a sample dimension does not declare a background value, the corresponding array element is null.
     *
     * @param  bands  the bands for which to get background values, or {@code null}.
     * @return the background values, or {@code null} if the given argument was null.
     *         Otherwise the returned array is never null but may contain null elements.
     */
    public static Number[] backgrounds(final List<SampleDimension> bands) {
        if (bands == null) {
            return null;
        }
        final Number[] fillValues = new Number[bands.size()];
        for (int i=fillValues.length; --i >= 0;) {
            final SampleDimension band = bands.get(i);
            if (band != null) {
                final Optional<Number> bg = band.getBackground();
                if (bg.isPresent()) {
                    fillValues[i] = bg.get();
                }
            }
        }
        return fillValues;
    }

    /**
     * Returns the {@code sampleFilters} arguments to use in a call to
     * {@code ImageProcessor.statistics(…)} for excluding no-data values.
     * If the given sample dimensions are {@linkplain SampleDimension#converted() converted to units of measurement},
     * then all "no data" values are already NaN values and this method returns an array of {@code null} operators.
     * Otherwise this method returns an array of operators that convert "no data" values to {@link Double#NaN}.
     *
     * <p>This method is not in public API because it partially duplicates the work
     * of {@linkplain SampleDimension#getTransferFunction() transfer function}.</p>
     *
     * @param  bands  the sample dimensions for which to create {@code sampleFilters}, or {@code null}.
     * @return the filters, or {@code null} if {@code bands} was null. The array may contain null elements.
     *
     * @see ImageProcessor#statistics(RenderedImage, Shape, DoubleUnaryOperator...)
     */
    public static DoubleUnaryOperator[] toSampleFilters(final List<SampleDimension> bands) {
        if (bands == null) {
            return null;
        }
        final DoubleUnaryOperator[] sampleFilters = new DoubleUnaryOperator[bands.size()];
        for (int i = 0; i < sampleFilters.length; i++) {
            final SampleDimension band = bands.get(i);
            if (band != null) {
                final List<Category> categories = band.getCategories();
                final int count = categories.size();
                final Number[] nodataValues = new Number[count + 1];
                for (int j = 0; j < count; j++) {
                    final Category category = categories.get(j);
                    if (!category.isQuantitative()) {
                        final NumberRange<?> range = category.getSampleRange();
                        final Number value;
                        if (range.isMinIncluded()) {
                            value = range.getMinValue();
                        } else if (range.isMaxIncluded()) {
                            value = range.getMaxValue();
                        } else {
                            continue;
                        }
                        nodataValues[j] = value;
                    }
                }
                nodataValues[count] = band.getBackground().orElse(null);
                sampleFilters[i] = ImageProcessor.filterNodataValues(nodataValues);
            }
        }
        return sampleFilters;
    }
}
