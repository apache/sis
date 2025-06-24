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

import java.util.Arrays;
import java.util.AbstractList;
import java.io.Serializable;
import java.io.ObjectStreamException;
import static java.lang.Double.isNaN;
import static java.lang.Double.doubleToRawLongBits;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix1;
import org.apache.sis.io.wkt.UnformattableObjectException;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;


/**
 * An immutable list of categories and a <i>transfer function</i> implementation backed by that list.
 * The category list (exposed by the {@link java.util.List} interface) has the following properties:
 *
 * <ul>
 *   <li>Categories are sorted by their sample values.</li>
 *   <li>Overlapping ranges of sample values are not allowed.</li>
 *   <li>A {@code CategoryList} can contain a mix of qualitative and quantitative categories.</li>
 * </ul>
 *
 * The transfer function exposed by the {@link MathTransform1D} interface is used only if this list contains
 * at least 2 categories. More specifically:
 *
 * <ul>
 *   <li>If this list contains 0 category, then the {@linkplain SampleDimension#getTransferFunction() transfer function}
 *       shall be absent.</li>
 *   <li>If this list contains 1 category, then the transfer function should be {@linkplain Category#getTransferFunction()
 *       the function provided by that single category}, without the indirection level implemented by {@code CategoryList}.</li>
 *   <li>If this list contains 2 or more categories, then the transfer function implementation provided by this
 *       {@code CategoryList} is necessary for {@linkplain #search(double) searching the category} where belong
 *       each sample value.</li>
 * </ul>
 *
 * The transfer function allows some extrapolations if a sample values to convert falls in a gap between two categories.
 * The category immediately below will be used (i.e. its domain is expanded up to the next category), except if one category
 * is qualitative while the next category is quantitative. In the latter case, the quantitative category has precedence.
 * The reason for allowing some extrapolations is because the range of values given to {@link Category} are often only
 * estimations, and we don't want the transfer function to fail because a value is slightly outside the estimated domain.
 *
 * <p>Instances of {@link CategoryList} are immutable and thread-safe.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class CategoryList extends AbstractList<Category> implements MathTransform1D, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -457688134719705403L;

    /**
     * An empty list of categories.
     */
    static final CategoryList EMPTY = new CategoryList();

    /**
     * The union of the ranges of every categories, excluding {@code NaN} values.
     * May be {@code null} if this list has no non-{@code NaN} category.
     *
     * <p>A {@link NumberRange} object gives more information than a (minimum, maximum) tuple since
     * it also contains the type (integer, float, etc.) and inclusion/exclusion information.</p>
     */
    final NumberRange<?> range;

    /**
     * List of minimum values (inclusive) for each category in {@link #categories}, in strictly increasing order.
     * For each category, {@code minimums[i]} is often equal to {@code categories[i].range.getMinDouble(true)} but
     * may also be lower for filling the gap between a quantitative category and its preceding qualitative category.
     * We do not store maximum values; range of a category is assumed to span up to the start of the next category.
     *
     * <p>This array <strong>must</strong> be in increasing order, with {@link Double#NaN} values last.
     * This is the need to sort this array that determines the element order in {@link #categories}.</p>
     */
    private final double[] minimums;

    /**
     * The list of categories to use for decoding samples. This list must be sorted in increasing
     * order of {@link Category#range} minimum. Qualitative categories with NaN values are last.
     */
    private final Category[] categories;

    /**
     * Minimum and maximum values (inclusive) of {@link Category#converse} for each category.
     * For each category at index {@code i}, the converse minimum is at index {@code i*2} and
     * the converse maximum is at index {@code i*2+1}.  This information is used for ensuring
     * that extrapolated values (i.e. the result of a conversion when the input value was not
     * in the range of any category) do not accidentally fall in the range of another category.
     * This field may be {@code null} if there is no need to perform such verification because
     * there is less than 2 categories bounded by real (non-NaN) values.
     */
    private final double[] converseRanges;

    /**
     * Index of the last used category. We assume that this category is the most likely to be
     * requested in the next {@code transform(…)} method invocation. This field does not need
     * to be volatile because it is not a problem if a thread see an outdated value; this is
     * only a hint, and the arrays used with this index are immutable.
     */
    private transient int lastUsed;

    /**
     * The {@code CategoryList} that describes values after {@linkplain #getTransferFunction() transfer function}
     * has been applied, or if this {@code CategoryList} is already converted then the original {@code CategoryList}.
     * Never null, but may be {@code this} if the transfer function is the identity function.
     * May also be {@link #EMPTY} if this category list has no quantitative category.
     *
     * <p>Except for the {@link #EMPTY} special case, this field establishes a bidirectional navigation between
     * sample values and real values. This is in contrast with methods named {@code converted()}, which establish
     * a unidirectional navigation from sample values to real values.</p>
     *
     * @see Category#converse
     * @see SampleDimension#converse
     */
    final CategoryList converse;

    /**
     * The action to take in {@code transform(…)} methods when converting a NaN value to sample value
     * and no mapping is found for that specific NaN value. The action can be one of the following,
     * in preference order:
     *
     * <ul>
     *   <li>+0 means to leave the NaN value as-is. In such case, casting the NaN value to an integer will
     *     produce 0 (so the 0 value is not set explicitly, but obtained as a result of casting to integer).
     *     This action can be taken only if no category include the 0 value, or if 0 is for the background.</li>
     *   <li>Any non-zero and non-NaN value means to use that value directly. In such case, the value should
     *     be {@link SampleDimension#background}.</li>
     *   <li>{@link Double#NaN} means that none of the above can be applied, in which case an exception will
     *     be thrown.</li>
     * </ul>
     *
     * @see #unmappedValue(double)
     */
    private final double fallback;

    /**
     * The constructor for the {@link #EMPTY} constant.
     */
    private CategoryList() {
        range          = null;
        minimums       = ArraysExt.EMPTY_DOUBLE;
        categories     = new Category[0];
        converseRanges = null;
        converse       = this;
        fallback       = Double.NaN;        // Specify that NaN values cannot be converted to a sample value.
    }

    /**
     * Constructs a category list using the specified array of categories.
     * The {@code categories} array should contain at least one element,
     * otherwise the {@link #EMPTY} constant should be used.
     *
     * @param  categories  the list of categories. This array is not cloned and is modified in-place.
     * @param  converse    if we are creating the list of categories after conversion from samples to real values,
     *                     the original list before conversion. Otherwise {@code null}.
     * @param  background  the {@link SampleDimension#background} sample value as a real number (not NaN), or {@code null}.
     *                     Despite being a sample value, this is used only for constructing the converted category list
     *                     ({@code converse != null}) because this is used as a fallback for <em>inverse</em> transforms.
     * @throws IllegalSampleDimensionException if two or more categories have overlapping sample value range.
     */
    private CategoryList(final Category[] categories, CategoryList converse, final Number background) {
        this.categories = categories;
        final int count = categories.length;
        /*
         * If users specify Category instances themselves, maybe they took existing instances from another
         * sample dimension. A list of "non-converted" categories should not contain any ConvertedCategory
         * instances, otherwise confusion will occur later.  Note that the converse is not true: a list of
         * converted categories may contain plain Category instances if the conversion is identity.
         */
        final boolean isSampleToUnit = (converse == null);
        if (isSampleToUnit) {
            for (int i=0; i<count; i++) {
                final Category c = categories[i];
                if (c instanceof ConvertedCategory) {
                    categories[i] = new Category(c, null);
                }
            }
        }
        Arrays.sort(categories, Category.COMPARATOR);
        /*
         * Constructs the array of minimum values (inclusive). This array shall be in increasing order since
         * we sorted the categories based on that criterion.  We also collect the minimum and maximum values
         * expected after conversion, but those values are not necessarily in any order.
         */
        final double[] extremums;
        extremums = new double[count * 2];
        minimums  = new double[count];

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        NumberRange<?> range = null;
        int countOfFiniteRanges = 0;
        for (int i=count; --i >= 0;) {                  // Reverse order for making computation of `range` more convenient.
            final Category category = categories[i];
            if (!isNaN(minimums[i] = category.range.getMinDouble(true))) {
                /*
                 * Initialize with the union of ranges at index 0 and index i. In most cases, the result will cover the whole
                 * range so all future calls to `range.unionAny(…)` will be no-op. The `categories[0].range` field should not
                 * be NaN because categories with NaN ranges are sorted last.
                 */
                if (range == null) {
                    range = categories[0].range;
                    assert !isNaN(range.getMinDouble()) : range;
                }
                range = range.unionAny(category.range);
            }
            final int j = i << 1;
            final NumberRange<?> cr = category.converse.range;
            if (!isNaN(extremums[j | 1] = cr.getMaxDouble(true)) |
                !isNaN(extremums[j    ] = cr.getMinDouble(true)))
            {
                countOfFiniteRanges++;
            }
        }
        this.range = range;
        this.converseRanges = (countOfFiniteRanges >= 2) ? extremums : null;
        assert ArraysExt.isSorted(minimums, false);
        /*
         * Verify that the ranges do not overlap and perform adjustments in `minimums` values for filling some gaps:
         * if we find a qualitative category followed by a quantitative category and empty space between them, then
         * the quantitative category takes that empty space. We do not perform similar check for the opposite side
         * (quantitative followed by qualitative) because CategoryList does not store maximum values; each category
         * take all spaces up to the next category.
         */
        for (int i=1; i<count; i++) {
            final Category category = categories[i];
            final Category previous = categories[i-1];
            final double   minimum  = minimums[i];
            if (Category.compare(minimum, previous.range.getMaxDouble(true)) <= 0) {
                throw new IllegalSampleDimensionException(Resources.format(Resources.Keys.CategoryRangeOverlap_4,
                            previous.name, previous.getRangeLabel(),
                            category.name, category.getRangeLabel()));
            }
            // No overlapping check for `converse` ranges here; see next block below.
            final double limit = previous.range.getMaxDouble(false);
            if (minimum > limit &&  previous.converse.isConvertedQualitative()      // (a>b) implies that values are not NaN.
                                && !category.converse.isConvertedQualitative())
            {
                minimums[i] = limit;    // Expand the range of quantitative `category` to the limit of qualitative `previous`.
            }
        }
        assert ArraysExt.isSorted(minimums, true);
        /*
         * If we are creating the list of "samples to real values" conversions, we need to create the list of categories
         * resulting from conversions to real values. Note that this will indirectly test if some coverted ranges overlap,
         * since this block invokes recursively this CategoryList constructor with a non-null `converse` argument.
         * Note also that converted categories may not be in the same order.
         */
        if (isSampleToUnit) {
            boolean isQualitative = true;
            boolean isIdentity    = true;
            final Category[] convertedCategories = new Category[count];
            for (int i=0; i<count; i++) {
                final Category category  = categories[i];
                final Category converted = category.converse;
                convertedCategories[i] = converted;
                isQualitative &= converted.isConvertedQualitative();
                isIdentity    &= (category == converted);
            }
            if (isQualitative) {
                converse = EMPTY;
            } else if (isIdentity) {
                converse = this;
            } else {
                converse = new CategoryList(convertedCategories, this, background);
                if (converseRanges != null) {
                    /*
                     * For "samples to real values" conversion (only that direction, not the converse) and only if there
                     * is two or more quantitative categories (should be very rare), adjust the converted maximum values
                     * for filling gaps between converted categories.
                     */
                    for (int i = 1; i < converseRanges.length; i += 2) {
                        final double maximum = converseRanges[i];
                        final int p = ~Arrays.binarySearch(converse.minimums, maximum);
                        if (p >= 0 && p < count) {
                            double limit = Math.nextDown(converse.minimums[p]);     // Minimum value of next category - ε
                            if (isNaN(limit)) limit = Double.POSITIVE_INFINITY;     // Because NaN are last, no higher values.
                            if (limit > maximum) converseRanges[i] = limit;         // Expand this category to fill the gap.
                            if (p == 1) {
                                converseRanges[i-1] = Double.NEGATIVE_INFINITY;     // Consistent with converse.minimums[0] = −∞
                            }
                        } else if (p == count) {
                            converseRanges[i] = Double.POSITIVE_INFINITY;           // No higher category; take all the space.
                        }
                    }
                }
            }
        }
        this.converse = converse;
        /*
         * Make the first quantitative category applicable to all low values. This is consistent with
         * the last quantitative category being applicable to all high values. Note that quantitative
         * categories are always before qualitative categories (NaN values) in the `minimums` array.
         */
        if (count != 0) {
            if (!isNaN(minimums[0])) {
                minimums[0] = Double.NEGATIVE_INFINITY;
            }
            /*
             * If we are converting from sample values to units of measurement, we should not have NaN inputs.
             * If it happens anyway, assume that we can propagate NaN sample values unchanged as output values
             * if the user seems prepared to see NaN values.
             *
             * Design note: we could propagate sample NaN values unconditionally because converted values should
             * always allow NaN. But even if NaN should be allowed, we are not sure that the user really expects
             * them if no such value appears in the arguments (s)he provided. Given that NaN sample values are
             * probably errors, we will let the `unmappedValue(double)` method throws an exception in such case.
             */
            if (isSampleToUnit) {
                final int n = converse.minimums.length;
                if (n != 0 && isNaN(converse.minimums[n - 1])) {
                    fallback = 0;
                    return;
                }
            } else {
                /*
                 * If a NaN value cannot be mapped to a sample value, keep the NaN value only if the 0 value
                 * (the result of casting NaN to integers) would not conflict with an existing category range.
                 * This check is important for "unit to sample" conversions, because we typically expect all
                 * results to be convertible to integers (ignoring rounding errors).
                 */
                if (background == null && converse.categories.length != 0) {
                    final NumberRange<?> cr = converse.categories[0].range;
                    final double cv = cr.getMinDouble();
                    if ((cv > 0) || (cv == 0 && !cr.isMinIncluded())) {
                        fallback = 0;
                        return;
                    }
                }
            }
        }
        /*
         * If we cannot let NaN value be propagated, use the background value if available.
         * Note that the background value given in argument is a sample value, so it can be
         * used only for the "unit to sample" conversion. If that background value is zero,
         * it will be interpreted as "let NaN values propagate" but it should be okay since
         * NaN casted to integers become 0.
         */
        fallback = (!isSampleToUnit && background != null) ? background.doubleValue() : Double.NaN;
    }

    /**
     * Returns a shared instance if applicable.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object contains invalid data.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Object readResolve() throws ObjectStreamException {
        return (categories.length == 0) ? EMPTY : this;
    }

    /**
     * Constructs a category list using the specified array of categories.
     * The {@code categories} array should contain at least one element,
     * otherwise the {@link #EMPTY} constant should be used.
     *
     * <p>This is defined as a static method for allowing the addition of a caching mechanism in the future if desired.</p>
     *
     * @param  categories  the list of categories. This array is not cloned and is modified in-place.
     * @param  background  the {@link SampleDimension#background} value (may be {@code null}).
     *                     This is a sample value (not a NaN value from converted categories).
     * @throws IllegalSampleDimensionException if two or more categories have overlapping sample value range.
     */
    static CategoryList create(final Category[] categories, final Number background) {
        return new CategoryList(categories, null, background);
    }

    /**
     * Returns {@code true} if the category list contains at least one NaN value.
     */
    final boolean allowsNaN() {
        final int n = minimums.length;
        return (n != 0) && isNaN(minimums[n-1]);
    }

    /**
     * Returns the <i>transfer function</i> from sample values to real values, including conversion of
     * "no data" values to NaNs. Callers shall ensure that there is at least one quantitative category
     * before to invoke this method.
     *
     * @see SampleDimension#getTransferFunction()
     */
    final MathTransform1D getTransferFunction() {
        MathTransform1D tr = categories[0].toConverse;          // See condition in javadoc.
        for (int i=categories.length; --i >= 1;) {
            if (!tr.equals(categories[i].toConverse)) {
                tr = this;
                break;
            }
        }
        return tr;
    }

    /**
     * Performs a bi-linear search of the specified value in the given sorted array. If an exact match is found,
     * its index is returned. If no exact match is found, index of the highest value smaller than {@code sample}
     * is returned. If no such index exists, -1 is returned. Said otherwise, if the return value is positive and
     * the given array is {@link #minimums}, then this method returns the index in the {@link #categories} array
     * of the {@link Category} to use for a given sample value.
     *
     * <p>This method differs from {@link Arrays#binarySearch(double[],double)} in the following aspects:</p>
     * <ul>
     *   <li>It differentiates the various NaN values.</li>
     *   <li>It does not differentiate exact matches from insertion points.</li>
     * </ul>
     *
     * @param  minimums  {@link #minimums}.
     * @param  sample    the sample value to search.
     * @return index of the category to use, or -1 if none.
     */
    static int binarySearch(final double[] minimums, final double sample) {
        int low  = 0;
        int high = minimums.length - 1;
        final boolean sampleIsNaN = isNaN(sample);
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final double midVal = minimums[mid];
            if (midVal < sample) {                      // Neither value is NaN, midVal is smaller.
                low = mid + 1;
                continue;
            }
            if (midVal > sample) {                      // Neither value is NaN, midVal is larger.
                high = mid - 1;
                continue;
            }
            final long midRawBits = doubleToRawLongBits(midVal);
            final long smpRawBits = doubleToRawLongBits(sample);
            if (midRawBits == smpRawBits) {
                return mid;                             // Exact match found.
            }
            final boolean midIsNaN = isNaN(midVal);
            final boolean adjustLow;
            if (sampleIsNaN) {
                /*
                 * If (mid,sample)==(!NaN, NaN): mid is lower.
                 * If two NaN arguments, compare NaN bits.
                 */
                adjustLow = (!midIsNaN || midRawBits < smpRawBits);
            } else {
                /*
                 * If (mid,sample)==(NaN, !NaN): mid is greater.
                 * Otherwise, case for (-0.0, 0.0) and (0.0, -0.0).
                 */
                adjustLow = (!midIsNaN && midRawBits < smpRawBits);
            }
            if (adjustLow) low = mid + 1;
            else          high = mid - 1;
        }
        /*
         * If we reach this point and the sample is NaN, then it is not one of the NaN values known
         * to CategoryList constructor and cannot be mapped to a category.  Otherwise we found the
         * index of "insertion point" (~i). This means that `sample` is lower than category minimum
         * at that index. Consequently, if the sample value is inside the range of some category, it
         * can only be the previous category (~i-1).
         */
        return sampleIsNaN ? -1 : low - 1;
    }

    /**
     * Returns the category of the specified sample value.
     * If no category fits, then this method returns {@code null}.
     *
     * @param  sample  the value.
     * @return the category of the supplied value, or {@code null}.
     */
    final Category search(final double sample) {
        final int i = binarySearch(minimums, sample);
        return (i >= 0) ? categories[i] : null;
    }

    /**
     * Invoked when a value cannot be located in the {@link #minimums} array. It should happen
     * only for NaN input values, which in turn should happen only in "unit to sample" conversions.
     * In such case we fallback on zero value if non ambiguous, or on the background value if available,
     * or throw an exception otherwise.
     *
     * @param  value  the (usually NaN) value that we cannot map to a category range.
     * @return the value to use as converted value.
     * @throws TransformException if the value cannot be converted.
     */
    private double unmappedValue(final double value) throws TransformException {
        if (MathFunctions.isPositiveZero(fallback)) {
            return value;
        }
        if (isNaN(fallback)) {
            throw new TransformException(formatNoCategory(value));
        }
        return fallback;
    }

    /**
     * Formats the "No category for value" message.
     */
    private static String formatNoCategory(final double value) {
        return Resources.format(Resources.Keys.NoCategoryForValue_1,
                isNaN(value) ? "NaN #" + MathFunctions.toNanOrdinal((float) value) : value);
    }

    /**
     * Transforms a sequence of coordinate tuples. This implementation accepts float or double arrays,
     * since the quasi-totality of the implementation is the same. Locale variables still of the
     * {@code double} type because this is the type used in {@link Category} objects.
     */
    private void transform(final double[] srcPts, final float[] srcFloat, int srcOff,
                           final double[] dstPts, final float[] dstFloat, int dstOff,
                           int numPts) throws TransformException
    {
        final int srcToDst = dstOff - srcOff;
        final int direction;
        if (srcOff >= dstOff || (srcFloat != null ? srcFloat != dstFloat : srcPts != dstPts)) {
            direction = +1;
        } else {
            direction = -1;
//          dstOff += numPts-1;             // Not updated because not used.
            srcOff += numPts-1;
        }
        /*
         * Scan every points.  Transforms will be applied by blocks, each time the loop detects that
         * the category has changed. The break condition (numPts >= 0) is near the end of the loop,
         * after we have done the conversion but before to change category.
         */
        int index = lastUsed;
        double value = Double.NaN;
        for (int peekOff = srcOff; /* numPts >= 0 */; peekOff += direction) {
            final double minimum = minimums[index];
            final double limit = (index+1 < minimums.length) ? minimums[index+1] : Double.NaN;
            final long   rawBits = doubleToRawLongBits(minimum);
            while (--numPts >= 0) {
                value = (srcFloat != null) ? srcFloat[peekOff] : srcPts[peekOff];
                if (value >= minimum) {
                    if (value >= limit) {
                        break;                                      // Category has changed; stop the search.
                    }
                } else if (doubleToRawLongBits(value) != rawBits) {
                    break;                                          // Not the expected NaN value.
                }
                peekOff += direction;
            }
            /*
             * The category has changed. Compute the start point (which depends on `direction`) and perform
             * the conversion on many values in a single `transform` method call.
             */
            int count = peekOff - srcOff;                       // May be negative if we are going backward.
            if (count < 0) {
                count  = -count;
                srcOff -= count - 1;
            }
            final int stepOff = srcOff + srcToDst;
            final MathTransform1D piece = categories[index].toConverse;
            if (srcFloat != null) {
                if (dstFloat != null) {
                    piece.transform(srcFloat, srcOff, dstFloat, stepOff, count);
                } else {
                    piece.transform(srcFloat, srcOff, dstPts, stepOff, count);
                }
            } else {
                if (dstFloat != null) {
                    piece.transform(srcPts, srcOff, dstFloat, stepOff, count);
                } else {
                    piece.transform(srcPts, srcOff, dstPts, stepOff, count);
                }
            }
            /*
             * If we need safety against extrapolations (for avoiding that a value falls in the range of another category),
             * verify that transformed values are in expected ranges. Values out of range will be clamped.
             */
            if (converseRanges != null) {
                dstOff = srcOff + srcToDst;
                if (dstFloat != null) {                                             // Loop for the `float` version.
                    final float min = (float) converseRanges[(index << 1)    ];
                    final float max = (float) converseRanges[(index << 1) | 1];
                    while (--count >= 0) {
                        final float check = dstFloat[dstOff];
                        if (check < min) {
                            dstFloat[dstOff] = min;
                        } else if (check > max) {
                            dstFloat[dstOff] = max;
                        }
                        dstOff++;
                    }
                } else {                                                            // Loop for the `double` version.
                    final double min = converseRanges[(index << 1)    ];
                    final double max = converseRanges[(index << 1) | 1];
                    while (--count >= 0) {
                        final double check = dstPts[dstOff];
                        if (check < min) {
                            dstPts[dstOff] = min;
                        } else if (check > max) {
                            dstPts[dstOff] = max;
                        }
                        dstOff++;
                    }
                }
            }
            /*
             * Conversion is now finished for all values in the range [srcOff … peekOff]
             * (not including `peekOff`). If there is more values to examine, get the new
             * category for the next values.
             */
            if (numPts < 0) break;
            while ((index = binarySearch(minimums, value)) < 0) {
                final double fill = unmappedValue(value);
                final int i = peekOff + srcToDst;
                if (dstFloat != null) {
                    dstFloat[i] = (float) fill;
                } else {
                    dstPts[i] = fill;
                }
                if (--numPts < 0) {
                    // Skip also the assignment to `lastUsed` because `index` is invalid.
                    return;
                }
                peekOff += direction;
                value = (srcFloat != null) ? srcFloat[peekOff] : srcPts[peekOff];
            }
            srcOff = peekOff;
        }
        lastUsed = index;
    }

    /**
     * Ensures that the given arrays are non-null.
     */
    private static void ensureNonNull(final Object srcPts, final Object dstPts) {
        ArgumentChecks.ensureNonNull("srcPts", srcPts);
        ArgumentChecks.ensureNonNull("dstPts", dstPts);
    }

    /**
     * Transforms a sequence of coordinate tuples. This method can be invoked only if {@link #categories} contains
     * at least two elements, otherwise a {@code MathTransform} implementation from another package is used.
     */
    @Override
    public final void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        ensureNonNull(srcPts, dstPts);
        transform(srcPts, null, srcOff, dstPts, null, dstOff, numPts);
    }

    /**
     * Transforms a sequence of coordinate tuples. This method can be invoked only if {@link #categories} contains
     * at least two elements, otherwise a {@code MathTransform} implementation from another package is used.
     */
    @Override
    public final void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        ensureNonNull(srcPts, dstPts);
        transform(null, srcPts, srcOff, null, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a sequence of coordinate tuples. This method can be invoked only if {@link #categories} contains
     * at least two elements, otherwise a {@code MathTransform} implementation from another package is used.
     */
    @Override
    public final void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        ensureNonNull(srcPts, dstPts);
        transform(null, srcPts, srcOff, dstPts, null, dstOff, numPts);
    }

    /**
     * Transforms a sequence of coordinate tuples. This method can be invoked only if {@link #categories} contains
     * at least two elements, otherwise a {@code MathTransform} implementation from another package is used.
     */
    @Override
    public final void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        ensureNonNull(srcPts, dstPts);
        transform(srcPts, null, srcOff, null, dstPts, dstOff, numPts);
    }

    /**
     * Transforms the specified value. This method can be invoked only if {@link #categories} contains at
     * least two elements, otherwise a {@code MathTransform} implementation from another package is used.
     *
     * @param  value  the value to transform.
     * @return the transformed value.
     * @throws TransformException if the value cannot be transformed.
     */
    @Override
    public final double transform(double value) throws TransformException {
        int index = lastUsed;
        final double minimum = minimums[index];
        if (value >= minimum ? (index+1 < minimums.length && value >= minimums[index+1])
                             : doubleToRawLongBits(value) != doubleToRawLongBits(minimum))
        {
            index = binarySearch(minimums, value);
            if (index < 0) {
                return unmappedValue(value);
            }
            lastUsed = index;
        }
        value = categories[index].toConverse.transform(value);
        if (converseRanges != null) {
            double bound;
            if (value < (bound = converseRanges[(index << 1)    ])) return bound;
            if (value > (bound = converseRanges[(index << 1) | 1])) return bound;
        }
        return value;
    }

    /**
     * Gets the derivative of this function at a value. This method can be invoked only if {@link #categories}
     * contains at least two elements, otherwise a {@code MathTransform} implementation from another package is used.
     *
     * @param  value  the value where to evaluate the derivative.
     * @return the derivative at the specified point.
     * @throws TransformException if the derivative cannot be evaluated at the specified point.
     */
    @Override
    public final double derivative(final double value) throws TransformException {
        int index = lastUsed;
        final double minimum = minimums[index];
        if (value >= minimum ? (index+1 < minimums.length && value >= minimums[index+1])
                             : doubleToRawLongBits(value) != doubleToRawLongBits(minimum))
        {
            index = binarySearch(minimums, value);
            if (index < 0) {
                throw new TransformException(formatNoCategory(value));
            }
            lastUsed = index;
        }
        return categories[index].toConverse.derivative(value);
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     */
    @Override
    public final DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        ArgumentChecks.ensureNonNull("ptSrc", ptSrc);
        ArgumentChecks.ensureDimensionMatches("ptSrc", 1, ptSrc);
        if (ptDst == null) {
            ptDst = new GeneralDirectPosition(1);
        } else {
            ArgumentChecks.ensureDimensionMatches("ptDst", 1, ptDst);
        }
        ptDst.setCoordinate(0, transform(ptSrc.getCoordinate(0)));
        return ptDst;
    }

    /**
     * Gets the derivative of this transform at a point.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) throws TransformException {
        ArgumentChecks.ensureNonNull("point", point);
        ArgumentChecks.ensureDimensionMatches("point", 1, point);
        return new Matrix1(derivative(point.getCoordinate(0)));
    }

    /**
     * Tests whether this transform does not move any points.
     */
    @Override
    public boolean isIdentity() {
        return converse == this;
    }

    /**
     * Returns the inverse transform of this object, which may be {@code this} if this transform is identity.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final MathTransform1D inverse() {
        return converse;
    }

    /**
     * Gets the dimension of input points, which is 1.
     */
    @Override
    public final int getSourceDimensions() {
        return 1;
    }

    /**
     * Gets the dimension of output points, which is 1.
     */
    @Override
    public final int getTargetDimensions() {
        return 1;
    }

    /**
     * Returns the number of categories in this list.
     */
    @Override
    public final int size() {
        return categories.length;
    }

    /**
     * Returns the element at the specified position in this list.
     */
    @Override
    public final Category get(final int i) {
        return categories[i];
    }

    /**
     * Compares the specified object with this category list for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof CategoryList) {
            final CategoryList that = (CategoryList) object;
            if (Arrays.equals(categories, that.categories)) {
                assert Arrays.equals(minimums, that.minimums);
            } else {
                return false;
            }
        }
        return super.equals(object);
    }

    /**
     * Returns a <i>Well Known Text</i> (WKT) for this object. This operation
     * may fail if an object is too complex for the WKT format capability.
     *
     * @return the Well Know Text for this object.
     * @throws UnsupportedOperationException if this object cannot be formatted as WKT.
     *
     * @todo Not yet implemented.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnformattableObjectException("Not yet implemented.");
    }
}
