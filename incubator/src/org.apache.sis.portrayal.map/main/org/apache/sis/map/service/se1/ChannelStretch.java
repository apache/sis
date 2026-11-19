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
package org.apache.sis.map.service.se1;

import org.opengis.feature.Feature;
import org.apache.sis.style.se1.ContrastEnhancement;
import org.apache.sis.style.se1.ContrastMethod;


/**
 * Conversion of the sample values of one channel to intensities in the [0 … 255] range,
 * applying a Symbology Encoding {@link ContrastEnhancement}.
 *
 * <ul>
 *   <li>{@code NORMALIZE}: linear stretch of the [minimum … maximum] range of values to [0 … 255].</li>
 *   <li>{@code HISTOGRAM}: histogram equalization, intensities are proportional to the cumulative
 *       distribution of values.</li>
 *   <li>{@code GAMMA} (or a gamma value with another method): power law applied on normalized intensities,
 *       values greater than 1 brighten the image and values lower than 1 darken it.</li>
 *   <li>No enhancement: values in the [0 … 255] range are used as-is, otherwise they are normalized.</li>
 * </ul>
 *
 * Statistics are computed on the values given at construction time, usually the visible pixels.
 *
 * @author Johann Sorel (Geomatys)
 */
final class ChannelStretch {

    /**
     * Number of bins of the histogram used for histogram equalization.
     */
    private static final int BINS = 1024;

    /**
     * The minimum and maximum finite values.
     */
    private final double minimum, maximum;

    /**
     * Whether to stretch the [minimum … maximum] range to [0 … 255].
     * If false, values are used as-is (clamped).
     */
    private final boolean normalize;

    /**
     * Cumulative distribution of values in [0 … 1] for histogram equalization, or {@code null} if none.
     */
    private final double[] cdf;

    /**
     * The inverse of the gamma value, or 1 if none.
     */
    private final double inverseGamma;

    /**
     * Creates a stretch for the given values.
     *
     * @param  enhancement  the contrast enhancement, or {@code null} if none.
     * @param  feature      feature for evaluating expressions, may be null.
     * @param  values       the sample values of the channel, NaN values are ignored.
     */
    ChannelStretch(ContrastEnhancement<?> enhancement, Feature feature, double[] values) {
        this((enhancement != null) ? enhancement.getMethod() : ContrastMethod.NONE,
             (enhancement != null) ? SymbolizerToScene2D.evaluateNumber(feature, enhancement.getGammaValue(), 1.0) : 1.0,
             values);
    }

    /**
     * Creates a stretch for the given values.
     *
     * @param  method  the contrast enhancement method, not null.
     * @param  gamma   the gamma value, 1 for none.
     * @param  values  the sample values of the channel, NaN values are ignored.
     */
    ChannelStretch(ContrastMethod method, double gamma, double[] values) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            if (!Double.isFinite(v)) continue;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (!(min <= max)) {
            min = max = 0;      // No finite value.
        }
        minimum = min;
        maximum = max;
        if (!(gamma > 0) || Double.isInfinite(gamma)) gamma = 1;
        inverseGamma = 1 / gamma;
        switch (method) {
            case NORMALIZE: normalize = true; cdf = null; break;
            case HISTOGRAM: normalize = true; cdf = cumulativeDistribution(values, min, max); break;
            default: {
                normalize = (min < 0 || max > 255);
                cdf = null;
                break;
            }
        }
    }

    /**
     * Computes the cumulative distribution of the given values.
     */
    private static double[] cumulativeDistribution(double[] values, double min, double max) {
        final double[] cdf = new double[BINS];
        final double span = max - min;
        if (!(span > 0)) {
            return null;
        }
        long count = 0;
        for (double v : values) {
            if (v >= min && v <= max) {
                cdf[Math.min(BINS - 1, (int) ((v - min) / span * BINS))]++;
                count++;
            }
        }
        double sum = 0;
        for (int i = 0; i < BINS; i++) {
            sum += cdf[i];
            cdf[i] = sum / count;
        }
        return cdf;
    }

    /**
     * Returns the intensity in [0 … 255] for the given value, or -1 if the value is NaN.
     *
     * @param  value  the sample value.
     * @return intensity in [0 … 255], or -1 for missing values.
     */
    int intensity(double value) {
        if (Double.isNaN(value)) {
            return -1;
        }
        double n;       // Normalized intensity in [0 … 1].
        if (cdf != null) {
            final int bin = (int) ((value - minimum) / (maximum - minimum) * BINS);
            n = cdf[Math.max(0, Math.min(BINS - 1, bin))];
        } else if (normalize) {
            final double span = maximum - minimum;
            n = (span > 0) ? (value - minimum) / span : 0.5;
        } else {
            n = value / 255;
        }
        n = Math.max(0, Math.min(1, n));
        if (inverseGamma != 1) {
            n = Math.pow(n, inverseGamma);
        }
        return (int) Math.round(n * 255);
    }
}
