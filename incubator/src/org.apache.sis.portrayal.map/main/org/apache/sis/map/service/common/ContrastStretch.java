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
package org.apache.sis.map.service.common;

import java.util.BitSet;
import org.apache.sis.util.ArgumentChecks;


/**
 * Contrast enhancement of a channel: conversion of its values to intensities between 0 and 1.
 * The stretch is configured with builder methods, fitted on the values of the channel
 * (usually the visible cells), then applied:
 *
 * {@snippet lang="java" :
 *     ContrastStretch stretch = new ContrastStretch(ContrastStretch.Method.HISTOGRAM)
 *             .setNativeRange(0, 255)
 *             .setGamma(2)
 *             .fit(values);
 *     double intensity = stretch.apply(value);
 *     }
 *
 * The methods are:
 *
 * <ul>
 *   <li>{@code NONE}: values are scaled from the native range to [0 … 1].
 *       If some values are outside the native range, the values are normalized instead.</li>
 *   <li>{@code NORMALIZE}: linear stretch of the [minimum … maximum] range of the values to [0 … 1].</li>
 *   <li>{@code HISTOGRAM}: histogram equalization, so that the intensities have about the same number of values.
 *       The intensity is (<var>c</var>(<var>v</var>) − <var>c</var><sub>min</sub>) / (<var>n</var> − <var>c</var><sub>min</sub>)
 *       where <var>c</var> is the cumulative histogram on 1024 bins and <var>n</var> the number of values,
 *       so that the minimum gives 0 and the maximum gives 1.</li>
 * </ul>
 *
 * Other rules:
 *
 * <ul>
 *   <li>If all values are equal, they are scaled from the native range as with {@code NONE}.</li>
 *   <li>The gamma value is applied after the method as the power 1/γ: values greater than 1 brighten the intensities.</li>
 *   <li>NaN values are ignored by the statistics, and their intensity is NaN.</li>
 * </ul>
 *
 * @see "OGC CartoSym Part 4 (draft) section 7.3"
 * @see org.apache.sis.style.se1.ContrastEnhancement
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ContrastStretch {
    /**
     * The contrast enhancement methods.
     */
    public enum Method {
        /** Values scaled from the native range, unless they are outside that range. */
        NONE,

        /** Linear stretch of the range of the values. */
        NORMALIZE,

        /** Histogram equalization. */
        HISTOGRAM
    }

    /**
     * Number of bins of the histogram used for histogram equalization.
     */
    private static final int BINS = 1024;

    /**
     * The contrast enhancement method.
     */
    private final Method method;

    /**
     * The inverse of the gamma value, 1 for none.
     */
    private double inverseGamma;

    /**
     * The native range of the values, mapped to [0 … 1] when the values are not stretched.
     */
    private double lower, upper;

    /**
     * The minimum and maximum finite values of the last fit, NaN if none.
     */
    private double minimum, maximum;

    /**
     * Whether to stretch the [minimum … maximum] range of the values rather than the native range.
     */
    private boolean normalize;

    /**
     * Intensity of each bin of the histogram, or {@code null} if no histogram equalization.
     */
    private double[] equalized;

    /**
     * Creates a stretch with the given method, a gamma of 1 and a native range of [0 … 1].
     * The stretch shall be {@linkplain #fit(double[]) fitted} before to be applied.
     *
     * @param  method  the contrast enhancement method.
     */
    public ContrastStretch(final Method method) {
        ArgumentChecks.ensureNonNull("method", method);
        this.method  = method;
        inverseGamma = 1;
        upper        = 1;
        minimum      = Double.NaN;
        maximum      = Double.NaN;
    }

    /**
     * Sets the gamma value, applied after the method as the power 1/γ.
     *
     * @param  gamma  the gamma value. The default is 1.
     * @return this stretch.
     * @throws IllegalArgumentException if the gamma is not strictly positive and finite.
     */
    public ContrastStretch setGamma(final double gamma) {
        ArgumentChecks.ensureStrictlyPositive("gamma", gamma);
        ArgumentChecks.ensureFinite("gamma", gamma);
        inverseGamma = 1 / gamma;
        return this;
    }

    /**
     * Sets the native range of the values, mapped to [0 … 1] when the values are not stretched.
     *
     * @param  lower  the value giving the intensity 0. The default is 0.
     * @param  upper  the value giving the intensity 1. The default is 1.
     * @return this stretch.
     * @throws IllegalArgumentException if the lower value is not less than the upper value.
     */
    public ContrastStretch setNativeRange(final double lower, final double upper) {
        ArgumentChecks.ensureFinite("lower", lower);
        ArgumentChecks.ensureFinite("upper", upper);
        if (!(lower < upper)) {
            throw new IllegalArgumentException("Empty native range: [" + lower + " … " + upper + "].");
        }
        this.lower = lower;
        this.upper = upper;
        return this;
    }

    /**
     * Computes the statistics of the given values. NaN and infinite values are ignored.
     *
     * @param  values  the values of the channel.
     * @return this stretch.
     */
    public ContrastStretch fit(final double[] values) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (final double v : values) {
            if (Double.isFinite(v)) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        final boolean spread = (max > min);
        minimum   = spread ? min : Double.NaN;
        maximum   = spread ? max : Double.NaN;
        equalized = null;
        switch (method) {
            case NONE:      normalize = spread && (min < lower || max > upper); break;
            case NORMALIZE: normalize = spread; break;
            case HISTOGRAM: {
                normalize = spread;
                if (spread) {
                    equalized = histogram(values);
                }
                break;
            }
        }
        return this;
    }

    /**
     * Computes the statistics of the values of the given cells. NaN and infinite values are ignored.
     *
     * @param  channel  the values of the channel.
     * @param  cells    indices of the cells on which to compute the statistics.
     * @return this stretch.
     */
    public ContrastStretch fit(final float[] channel, final BitSet cells) {
        final double[] values = new double[cells.cardinality()];
        int n = 0;
        for (int i = cells.nextSetBit(0); i >= 0; i = cells.nextSetBit(i+1)) {
            values[n++] = channel[i];
        }
        return fit(values);
    }

    /**
     * Computes the intensity of each bin of the histogram of the given values,
     * or returns {@code null} if all values are in the first bin.
     */
    private double[] histogram(final double[] values) {
        final long[] counts = new long[BINS];
        for (final double v : values) {
            if (v >= minimum && v <= maximum) {
                counts[bin(v)]++;
            }
        }
        final long lowest = counts[0];
        long total = 0;
        for (final long count : counts) {
            total += count;
        }
        if (total <= lowest) {
            return null;
        }
        final double[] intensities = new double[BINS];
        long cumulated = 0;
        for (int i = 0; i < BINS; i++) {
            cumulated += counts[i];
            intensities[i] = (double) (cumulated - lowest) / (total - lowest);
        }
        return intensities;
    }

    /**
     * Returns the histogram bin of the given value.
     */
    private int bin(final double value) {
        return Math.max(0, Math.min(BINS - 1, (int) ((value - minimum) / (maximum - minimum) * BINS)));
    }

    /**
     * Returns the intensity of the given value.
     *
     * @param  value  the value of the channel.
     * @return the intensity between 0 and 1, or NaN if the value is NaN.
     */
    public double apply(final double value) {
        if (Double.isNaN(value)) {
            return Double.NaN;
        }
        double n;
        if (equalized != null) {
            n = equalized[bin(value)];
        } else if (normalize) {
            n = (value - minimum) / (maximum - minimum);
        } else {
            n = (value - lower) / (upper - lower);
        }
        n = Math.max(0, Math.min(1, n));
        if (inverseGamma != 1) {
            n = Math.pow(n, inverseGamma);
        }
        return n;
    }

    /**
     * Replaces the values of the given cells by their intensities.
     *
     * @param  channel  the values of the channel, modified in place.
     * @param  cells    indices of the cells to modify.
     */
    public void apply(final float[] channel, final BitSet cells) {
        for (int i = cells.nextSetBit(0); i >= 0; i = cells.nextSetBit(i+1)) {
            channel[i] = (float) apply(channel[i]);
        }
    }
}
