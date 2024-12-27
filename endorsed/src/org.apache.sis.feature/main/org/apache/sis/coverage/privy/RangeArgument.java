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
import java.util.Arrays;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RasterFormatException;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Localized;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.math.MathFunctions;


/**
 * The user-provided {@code ranges} argument together with a set of convenience tools.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RangeArgument {
    /**
     * The range indices specified by user in high bits, together (in the low bits)
     * with the position in the {@code ranges} array where each index was specified.
     * This packing is used for making easier to sort this array in increasing order
     * of user-specified range index.
     */
    private final long[] packed;

    /**
     * Whether the selection contains all bands of the resource, not necessarily in order.
     */
    public final boolean hasAllBands;

    /**
     * If a {@linkplain #insertSubsampling subsampling} has been applied, indices of the first and last band
     * to read, together with the interval (stride) between bands.  Those information are computed only when
     * the {@code insertFoo(…)} methods are invoked.
     *
     * @see #insertBandDimension(GridExtent, int)
     * @see #insertSubsampling(long[], int)
     */
    private int first, last, interval;

    /**
     * A builder for sample dimensions, created when first needed.
     */
    private SampleDimension.Builder builder;

    /**
     * Encapsulates the user supplied {@code ranges} argument, packed in high bits of {@code packed}.
     */
    private RangeArgument(final long[] packed, final boolean hasAllBands) {
        this.packed      = packed;
        this.hasAllBands = hasAllBands;
        this.interval    = 1;
    }

    /**
     * Validate the {@code ranges} argument given to
     * {@link org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...)}.
     * This method verifies that all indices are between 0 and {@code numSampleDimensions}
     * and that there is no duplicated index.
     *
     * @param  numSampleDimensions  number of sample dimensions in the resource.
     *         Equal to <code>{@linkplain org.apache.sis.storage.GridCoverageResource#getSampleDimensions()}.size()</code>.
     * @param  ranges  the {@code ranges} argument given by the user. May be null or empty.
     * @param  listeners  source of locale to use if an exception must be thrown, or {@code null} if none.
     * @return the {@code ranges} argument encapsulated with a set of convenience tools.
     * @throws IllegalArgumentException if a range index is invalid.
     */
    public static RangeArgument validate(final int numSampleDimensions, final int[] ranges, final Localized listeners) {
        ArgumentChecks.ensureStrictlyPositive("numSampleDimensions", numSampleDimensions);
        final long[] packed;
        if (ranges == null || ranges.length == 0) {
            packed = new long[numSampleDimensions];
            for (int i=1; i<numSampleDimensions; i++) {
                packed[i] = Numerics.tuple(i, i);
            }
        } else {
            /*
             * Pattern: [specified `range` value | index in `ranges` where the value was specified]
             */
            packed = new long[ranges.length];
            for (int i=0; i<ranges.length; i++) {
                final int r = ranges[i];
                if (r < 0 || r >= numSampleDimensions) {
                    throw new IllegalArgumentException(resources(listeners).getString(
                            Resources.Keys.InvalidSampleDimensionIndex_2, numSampleDimensions - 1, r));
                }
                packed[i] = Numerics.tuple(r, i);
            }
            /*
             * Sort by increasing `range` value, but keep together with index in `ranges` where each
             * value was specified. After sorting, it become easy to check for duplicated values.
             */
            Arrays.sort(packed);
            int previous = -1;
            for (int i=0; i<packed.length; i++) {
                // Never negative because of check in previous loop.
                final int r = (int) (packed[i] >>> Integer.SIZE);
                if (r == previous) {
                    throw new IllegalArgumentException(resources(listeners).getString(
                            Resources.Keys.DuplicatedSampleDimensionIndex_1, r));
                }
                previous = r;
            }
        }
        return new RangeArgument(packed, packed.length == numSampleDimensions);
    }

    /**
     * Returns the resources for the locale specified by the given listeners.
     *
     * @param  listeners  source of locale to use for an exception to be thrown, or {@code null} if none.
     * @return the resources for the given locale.
     */
    private static Resources resources(final Localized listeners) {
        return Resources.forLocale((listeners != null) ? listeners.getLocale() : null);
    }

    /**
     * Returns {@code true} if user specified all bands in increasing order.
     * This method always return {@code false} if {@link #insertSubsampling(long[], int)} has been invoked.
     *
     * @return whether user specified all bands in increasing order without subsampling inserted.
     */
    public boolean isIdentity() {
        if (!hasAllBands || interval != 1) {
            return false;
        }
        for (int i=0; i<packed.length; i++) {
            if (packed[i] != Numerics.tuple(i, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of sample dimensions. This is the length of the {@code ranges} array supplied by user,
     * or the number of bands in the source coverage if the {@code ranges} array was null or empty.
     *
     * @return the number of sample dimensions selected by user.
     */
    public int getNumBands() {
        return packed.length;
    }

    /**
     * Returns the indices of bands selected by the user.
     * This is a copy of the {@code ranges} argument specified by the user, in same order.
     * Note that this is not necessarily increasing order.
     *
     * @return a copy of the {@code ranges} argument specified by the user.
     */
    public int[] getSelectedBands() {
        final int[] bands = new int[getNumBands()];
        for (int i=0; i<bands.length; i++) {
            bands[getTargetIndex(i)] = getSourceIndex(i);
        }
        return bands;
    }

    /**
     * Returns the value of the first index specified by the user. This is not necessarily equal to
     * {@code getSourceIndex(0)} if the user specified the bands out of order.
     *
     * @return index of the first value in the user-specified {@code ranges} array.
     */
    public int getFirstSpecified() {
        for (final long p : packed) {
            if (((int) p) == 0) {
                return (int) (p >>> Integer.SIZE);
            }
        }
        throw new IllegalStateException();              // Should never happen.
    }

    /**
     * Returns the i<sup>th</sup> index of the band to read from the resource.
     * Indices are returned in strictly increasing order.
     *
     * @param  i  index of the range index to get, from 0 inclusive to {@link #getNumBands()} exclusive.
     * @return index of the i<sup>th</sup> band to read from the resource.
     */
    public int getSourceIndex(final int i) {
        return (int) (packed[i] >>> Integer.SIZE);
    }

    /**
     * Returns the i<sup>th</sup> band position. This is the index in the user supplied {@code ranges} array
     * where the {@code getSourceIndex(i)} value was specified.
     *
     * @param  i  index of the range index to get, from 0 inclusive to {@link #getNumBands()} exclusive.
     * @return index in user supplied {@code ranges} array where was specified the {@code getSourceIndex(i)} value.
     */
    public int getTargetIndex(final int i) {
        return (int) packed[i];
    }

    /**
     * Returns the i<sup>th</sup> index of the band to read from the resource, after subsampling has been applied.
     * The subsampling results from calls to {@link #insertBandDimension(GridExtent, int)} and
     * {@link #insertSubsampling(long[], int)} methods.
     *
     * {@snippet lang="java" :
     *     areaOfInterest = rangeIndices.insertBandDimension(areaOfInterest, bandDimension);
     *     subsampling    = rangeIndices.insertSubsampling  (subsampling,    bandDimension);
     *     data = myReadMethod(areaOfInterest, subsampling);
     *     for (int i=0; i<numBands; i++) {
     *         int bandIndexInTheDataWeJustRead = rangeIndices.getSubsampledIndex(i);
     *     }
     *     }
     *
     * If the {@code insertXXX(…)} methods have never been invoked, then this method is equivalent to {@link #getSourceIndex(int)}.
     *
     * @param  i  index of the range index to get, from 0 inclusive to {@link #getNumBands()} exclusive.
     * @return index of the i<sup>th</sup> band to read from the resource, after subsampling.
     */
    public int getSubsampledIndex(final int i) {
        return (getSourceIndex(i) - first) / interval;
    }

    /**
     * Returns the increment to apply on index for moving to the same band of the next pixel.
     * If the {@code insertXXX(…)} methods have never been invoked, then this method returns 1.
     *
     * @return the increment to apply on index for moving to the next pixel in the same band.
     *
     * @see java.awt.image.PixelInterleavedSampleModel#getPixelStride()
     */
    public int getPixelStride() {
        return (last - first) / interval + 1;
    }

    /**
     * Returns the given extent with a new dimension added for the bands. The extent in the new dimension
     * will range from the minimum {@code range} value to the maximum {@code range} value inclusive.
     * This method should be used together with {@link #insertSubsampling(long[], int)}.
     *
     * <h4>Use case</h4>
     * This method is useful for reading a <var>n</var>-dimensional data cube with values stored in a
     * {@link java.awt.image.PixelInterleavedSampleModel} fashion (except if {@code bandDimension} is
     * after all existing {@code areaOfInterest} dimensions, in which case data become organized in a
     * {@link java.awt.image.BandedSampleModel} fashion). This method converts the specified domain
     * (decomposed in {@code areaOfInterest} and {@code subsampling} parameters) into a larger domain
     * encompassing band dimension as if it was an ordinary space or time dimension. It makes possible
     * to use this domain with {@link org.apache.sis.io.stream.HyperRectangleReader} for example.
     *
     * @param  areaOfInterest  the extent to which to add a new dimension for bands.
     * @param  bandDimension   index of the band dimension.
     * @return a new extent with the same values as the given extent plus one dimension for bands.
     */
    public GridExtent insertBandDimension(final GridExtent areaOfInterest, final int bandDimension) {
        first = getSourceIndex(0);
        last  = getSourceIndex(packed.length - 1);
        return areaOfInterest.insertDimension(bandDimension, DimensionNameType.valueOf("BAND"), first, last, true);
    }

    /**
     * Returns the given subsampling with a new dimension added for the bands. The subsampling in the new
     * dimension will be the greatest common divisor of the difference between all user-specified values.
     * This method should be used together with {@link #insertBandDimension(GridExtent, int)}.
     * See that method for more information.
     *
     * <p>Invoking this method changes the values returned by following methods:</p>
     * <ul>
     *   <li>{@link #isIdentity()}</li>
     *   <li>{@link #getSubsampledIndex(int)}</li>
     *   <li>{@link #getPixelStride()}</li>
     * </ul>
     *
     * @param  subsampling    the subsampling to which to add a new dimension for bands.
     * @param  bandDimension  index of the band dimension.
     * @return a new subsampling array with the same values as the given array plus one dimension for bands.
     */
    public long[] insertSubsampling(long[] subsampling, final int bandDimension) {
        final int[] delta = new int[packed.length - 1];
        for (int i=0; i<delta.length; i++) {
            delta[i] = getSourceIndex(i+1) - getSourceIndex(i);
        }
        final int[] divisors = MathFunctions.commonDivisors(delta);
        interval = (divisors.length != 0) ? divisors[divisors.length - 1] : 1;
        subsampling = ArraysExt.insert(subsampling, bandDimension, 1);
        subsampling[bandDimension] = interval;
        return subsampling;
    }

    /**
     * Returns sample dimensions selected by the user. This is a convenience method for situations where
     * sample dimensions are already in memory and there is no advantage to read them in "physical" order.
     *
     * @param  sourceBands  bands in the source coverage.
     * @return bands selected by user, in user-specified order.
     */
    public SampleDimension[] select(final List<? extends SampleDimension> sourceBands) {
        final var bands = new SampleDimension[getNumBands()];
        for (int i=0; i<bands.length; i++) {
            bands[getTargetIndex(i)] = sourceBands.get(getSourceIndex(i));
        }
        return bands;
    }

    /**
     * Returns a sample model for the bands specified by the user.
     * The model created by this method can be a "view" or can be "compressed":
     *
     * <ul class="verbose">
     *   <li>If {@code view} is {@code true}, the sample model returned by this method will expect the
     *       same {@link java.awt.image.DataBuffer} than the one expected by the original {@code model}.
     *       Bands enumerated in the {@code ranges} argument will be used and other bands will be ignored.
     *       This mode is efficient if the data are already in memory and we want to avoid copying them.
     *       An inconvenient is that all bands, including the ignored ones, are retained in memory.</li>
     *   <li>If {@code view} is {@code false}, then this method will "compress" bank indices and bit masks
     *       for making them consecutive. For example if the {@code ranges} argument specifies that the bands
     *       to read are {1, 3, 4, 6, …}, then "compressed" sample model will use bands {0, 1, 2, 3, …}.
     *       This mode is efficient if the data are not yet in memory and the reader is capable to skip
     *       the bands to ignore. In such case, this mode save memory.</li>
     * </ul>
     *
     * @param  model  the original sample model with all bands. Can be {@code null}.
     * @param  view   whether the band subset shall be a view over the full band set.
     * @return the sample model for a subset of bands, or {@code null} if the given sample model was null.
     * @throws RasterFormatException if the given sample model is not recognized.
     * @throws IllegalArgumentException if an error occurred when constructing the new sample model.
     *
     * @see SampleModel#createSubsetSampleModel(int[])
     * @see SampleModelBuilder#subsetAndCompress(int[])
     */
    public SampleModel select(final SampleModel model, final boolean view) {
        if (model == null || isIdentity()) {
            return model;
        }
        final int[] bands = getSelectedBands();
        if (view) {
            return model.createSubsetSampleModel(bands);
        } else {
            final var factory = new SampleModelBuilder(model);
            factory.subsetAndCompress(bands);
            return factory.build();
        }
    }

    /**
     * Returns a color model for the bands specified by the user.
     * This method may return {@code null} if the color model cannot be created.
     *
     * @param  colors  the original color model with all bands. Can be {@code null}.
     * @return the color model for a subset of bands, or null if the given color model was null.
     */
    public ColorModel select(final ColorModel colors) {
        if (colors == null || isIdentity()) {
            return colors;
        }
        return ColorModelFactory.createSubset(colors, getSelectedBands());
    }

    /**
     * Returns a builder for sample dimensions. This method recycles the same builder on every calls.
     * If the builder has been returned by a previous call to this method,
     * then it is {@linkplain SampleDimension.Builder#clear() cleared} before to be returned again.
     *
     * @return a recycled builder for sample dimensions.
     */
    public SampleDimension.Builder builder() {
        if (builder == null) {
            builder = new SampleDimension.Builder();
        } else {
            builder.clear();
        }
        return builder;
    }
}
