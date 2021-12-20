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
package org.apache.sis.internal.coverage;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.Category;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.Static;


/**
 * Utility methods working on {@link SampleDimension} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final class SampleDimensions extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private SampleDimensions() {
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
            final Optional<Number> bg = band.getBackground();
            if (bg.isPresent()) {
                fillValues[i] = bg.get();
            }
        }
        return fillValues;
    }

    /**
     * Returns the {@code sampleFilters} arguments to use in a call to
     * {@link ImageProcessor#statistics ImageProcessor.statistics(…)} for excluding no-data values.
     * If the given sample dimensions are {@linkplain SampleDimension#converted() converted to units of measurement},
     * then all "no data" values are already NaN values and this method returns an array of {@code null} operators.
     * Otherwise this method returns an array of operators that covert "no data" values to {@link Double#NaN}.
     *
     * <p>This method is not in public API because it partially duplicates the work
     * of {@linkplain SampleDimension#getTransferFunction() transfer function}.</p>
     *
     * @param  processor  the processor to use for creating {@link DoubleUnaryOperator}.
     * @param  bands      the sample dimensions for which to create {@code sampleFilters}, or {@code null}.
     * @return the filters, or {@code null} if {@code bands} was null. The array may contain null elements.
     */
    public static DoubleUnaryOperator[] toSampleFilters(final ImageProcessor processor, final List<SampleDimension> bands) {
        if (bands == null) {
            return null;
        }
        final DoubleUnaryOperator[] sampleFilters = new DoubleUnaryOperator[bands.size()];
        for (int i = 0; i < sampleFilters.length; i++) {
            final SampleDimension band = bands.get(i);
            if (band != null) {
                final List<Category> categories = band.getCategories();
                final Number[] nodataValues = new Number[categories.size()];
                for (int j = 0; j < nodataValues.length; j++) {
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
                sampleFilters[i] = processor.filterNodataValues(nodataValues);
            }
        }
        return sampleFilters;
    }

    /**
     * Adds categories to the given sample dimension builder for an image having no explicit transfer function.
     * This method creates a default transfer function only if needed, for example if a "no data" value exists.
     * If the transfer function would be identity, then this method does not add it even if {@code sampleRange}
     * was non-null. This conservative policy is because the purpose of this method is only to avoid that image
     * operations such as resampling do their calculations on wrong values. If we can avoid to create artificial
     * information that did not existed in the original data, it is better.
     *
     * @param  sampleSize   size of sample values in bits, or 0 if unknown or if sample are floating-point values.
     * @param  isUnsigned   whether sample values are unsigned integers. Ignored if {@code sampleSize} is 0.
     * @param  sampleRange  minimum and maximum sample values, or {@code null} if unknown.
     * @param  fillValue    the "no data" value, or {@code null} if none. May intersect {@code sampleRange}.
     * @param  dest         where to add the categories.
     */
    public static void addDefaultCategories(final int sampleSize, final boolean isUnsigned, NumberRange<?> sampleRange,
                                            final Number fillValue, final SampleDimension.Builder dest)
    {
        if (fillValue != null) {
            dest.setBackground(null, fillValue);
            if (sampleRange == null && sampleSize != 0) {
                long min = 0, max = Numerics.bitmask(sampleSize) - 1;
                if (!isUnsigned) {
                    max >>>= 1;
                    min = ~max;
                }
                sampleRange = NumberRange.createBestFit(min, true, max, true);
            }
            if (sampleRange != null && sampleRange.containsAny(fillValue)) {
                final double fill = fillValue.doubleValue();
                if (sampleRange.getMaxDouble() - fill < fill - sampleRange.getMinDouble()) {
                    sampleRange = NumberRange.createBestFit(sampleRange.getMinValue(), sampleRange.isMinIncluded(), fill, false);
                } else {
                    sampleRange = NumberRange.createBestFit(fill, false, sampleRange.getMaxValue(), sampleRange.isMaxIncluded());
                }
                dest.addQuantitative(Vocabulary.formatInternational(Vocabulary.Keys.Values), sampleRange, sampleRange);
            }
        }
    }
}
