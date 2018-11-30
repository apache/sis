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
import java.util.Comparator;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.measure.Unit;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrix1;
import org.apache.sis.io.wkt.UnformattableObjectException;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.NumberRange;

import static java.lang.Double.isNaN;
import static java.lang.Double.doubleToRawLongBits;


/**
 * An immutable list of categories. Categories are sorted by their sample values.
 * Overlapping ranges of sample values are not allowed. A {@code CategoryList} can contains a mix of
 * qualitative and quantitative categories. The {@link #getCategory(double)} method is responsible
 * for finding the right category for an arbitrary sample value.
 *
 * <p>Instances of {@link CategoryList} are immutable and thread-safe.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class CategoryList extends AbstractList<Category> implements MathTransform1D, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2647846361059903365L;

    /**
     * The policy when {@link #getCategory(double)} does not find a match for a sample value.
     * {@code true} means that it should search for the nearest category, while {@code false}
     * means that it should returns {@code null}.
     */
    private static final boolean SEARCH_NEAREST = false;

    /**
     * The range of values in this category list. This is the union of the range of values of every categories,
     * excluding {@code NaN} values. This field will be computed only when first requested.
     *
     * @see #getRange()
     */
    private transient volatile NumberRange<?> range;

    /**
     * List of {@link Category#minimum} values for each category in {@link #categories}.
     * This array <strong>must</strong> be in increasing order. Actually, this is the
     * need to sort this array that determines the element order in {@link #categories}.
     */
    private final double[] minimums;

    /**
     * The list of categories to use for decoding samples. This list must be sorted in increasing order
     * of {@link Category#minimum}. This {@code CategoryList} object may be used as a {@link Comparator}
     * for that purpose. Qualitative categories (with NaN values) are last.
     */
    private final Category[] categories;

    /**
     * The "main" category, or {@code null} if there is none. The main category
     * is the quantitative category with the widest range of sample values.
     */
    private final Category main;

    /**
     * The "no data" category (never {@code null}). The "no data" category is a category mapping the {@link Double#NaN} value.
     * If none has been found, a default "no data" category is used. This category is used to transform geophysics values to
     * sample values into rasters when no suitable category has been found for a given geophysics value.
     */
    private final Category nodata;

    /**
     * The last used category. We assume that this category is the most likely to be requested in the next
     * {@code transform(…)} method invocation.
     *
     * <p>This field is not declared {@code volatile} because we will never assign newly created objects to it.
     * It will always be a reference to an existing category, and it does not matter if referenced category is
     * not really the last used one.</p>
     */
    private transient Category last;

    /**
     * {@code true} if there is gaps between categories, or {@code false} otherwise. A gap is found if for
     * example the range of value is [-9999 … -9999] for the first category and [0 … 1000] for the second one.
     */
    private final boolean hasGaps;


    /**
     * Constructs a category list using the specified array of categories.
     *
     * @param  categories  the list of categories. May be empty, but can not be null. This array is not cloned.
     * @param  units       the geophysics unit, or {@code null} if none.
     * @throws IllegalArgumentException if two or more categories have overlapping sample value range.
     */
    CategoryList(final Category[] categories, final Unit<?> units) {
        this.categories = categories;
        Arrays.sort(categories, Category.COMPARATOR);
        /*
         * Constructs the array of Category.minimum values. During the loop, we make sure there is no overlapping ranges.
         * We also take the "no data" category mapped to the sample value 0 if it exists, or the first "no data" category
         * otherwise.
         */
        double   range   = 0;
        Category main    = null;
        Category nodata  = null;
        boolean  hasGaps = false;
        minimums = new double[categories.length];
        for (int i=0; i < categories.length; i++) {
            final Category category = categories[i];
            final double minimum = category.minimum;
            minimums[i] = minimum;
            if (category.transferFunction != null) {
                final double r = category.maximum - category.minimum;
                if (r >= range) {
                    range = r;
                    main  = category;
                }
            } else if (nodata == null || minimum == 0) {
                nodata = category;
            }
            if (i != 0) {
                assert !(minimum <= minimums[i-1]) : minimum;                   // Use '!' to accept NaN.
                final Category previous = categories[i-1];
                if (!hasGaps && !isNaN(minimum) && minimum != previous.getRange().getMaxDouble(false)) {
                    hasGaps = true;
                }
                if (Category.compare(minimum, previous.maximum) <= 0) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.CategoryRangeOverlap_4, new Object[] {
                                previous.getName(), previous.getRangeLabel(),
                                category.getName(), category.getRangeLabel()}));
                }
            }
        }
        this.main    = main;
        this.last    = (main != null || categories.length == 0) ? main : categories[0];
        this.nodata  = (nodata != null) ? nodata : Category.NODATA;
        this.hasGaps = hasGaps;
        assert isSorted(categories);
    }

    /**
     * Resets the {@link #last} field to a non-null value after deserialization.
     *
     * @param  in  the input stream from which to deserialize a category list.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        last = (main != null || categories.length == 0) ? main : categories[0];
    }

    /**
     * Returns {@code true} if the specified categories are sorted. This method
     * ignores {@code NaN} values. This method is used for assertions only.
     */
    private static boolean isSorted(final Category[] categories) {
        for (int i=1; i<categories.length; i++) {
            Category c;
            assert !((c=categories[i  ]).minimum > c.maximum) : c;
            assert !((c=categories[i-1]).minimum > c.maximum) : c;
            if (Category.compare(categories[i-1].maximum, categories[i].minimum) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs a bi-linear search of the specified value. This method is similar to
     * {@link Arrays#binarySearch(double[],double)} except that it can differentiate
     * NaN values.
     */
    private static int binarySearch(final double[] array, final double key) {
        int low  = 0;
        int high = array.length - 1;
        final boolean keyIsNaN = isNaN(key);
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final double midVal = array[mid];
            if (midVal < key) {                         // Neither value is NaN, midVal is smaller.
                low = mid + 1;
                continue;
            }
            if (midVal > key) {                         // Neither value is NaN, midVal is larger.
                high = mid - 1;
                continue;
            }
            final long midRawBits = doubleToRawLongBits(midVal);
            final long keyRawBits = doubleToRawLongBits(key);
            if (midRawBits == keyRawBits) {
                return mid;                             // Key found.
            }
            final boolean midIsNaN = isNaN(midVal);
            final boolean adjustLow;
            if (keyIsNaN) {
                // If (mid,key)==(!NaN, NaN): mid is lower.
                // If two NaN arguments, compare NaN bits.
                adjustLow = (!midIsNaN || midRawBits < keyRawBits);
            } else {
                // If (mid,key)==(NaN, !NaN): mid is greater.
                // Otherwise, case for (-0.0, 0.0) and (0.0, -0.0).
                adjustLow = (!midIsNaN && midRawBits < keyRawBits);
            }
            if (adjustLow) low = mid + 1;
            else          high = mid - 1;
        }
        return ~low;                                    // key not found.
    }

    /**
     * Returns the category of the specified sample value.
     * If no category fits, then this method returns {@code null}.
     *
     * @param  sample  the value.
     * @return the category of the supplied value, or {@code null}.
     */
    public final Category getCategory(final double sample) {
        /*
         * Search which category contains the given value.
         * Note: NaN values are at the end of 'minimums' array, so:
         *
         * 1) if 'value' is NaN, then 'i' will be the index of a NaN category.
         * 2) if 'value' is a real number, then 'i' may be the index of a category
         *    of real numbers or the first category containing NaN values.
         */
        int i = binarySearch(minimums, sample);                             // Special 'binarySearch' for NaN
        if (i >= 0) {
            assert doubleToRawLongBits(sample) == doubleToRawLongBits(minimums[i]);
            return categories[i];
        }
        /*
         * If we reach this point and the value is NaN, then it is not one of the
         * registered NaN values. Consequently we can not map a category to this value.
         */
        if (isNaN(sample)) {
            return null;
        }
        assert i == Arrays.binarySearch(minimums, sample) : i;
        /*
         * 'binarySearch' found the index of "insertion point" (~i). This means that
         * 'sample' is lower than 'Category.minimum' at this index. Consequently, if
         * this value fits in a category's range, it fits in the previous category (~i-1).
         */
        i = ~i - 1;
        if (i >= 0) {
            final Category category = categories[i];
            assert sample > category.minimum : sample;
            if (sample <= category.maximum) {
                return category;
            }
            if (SEARCH_NEAREST) {
                if (++i < categories.length) {
                    final Category upper = categories[i];
                    /*
                     * ASSERT: if 'upper.minimum' was smaller than 'value', it should has been
                     *         found by 'binarySearch'. We use '!' in order to accept NaN values.
                     */
                    assert !(upper.minimum <= sample) : sample;
                    return (upper.minimum-sample < sample-category.maximum) ? upper : category;
                }
                while (--i >= 0) {
                    final Category previous = categories[i];
                    if (!isNaN(previous.minimum)) {
                        return previous;
                    }
                }
            }
        } else if (SEARCH_NEAREST) {
            /*
             * If the value is smaller than the smallest Category.minimum, returns
             * the first category (except if there is only NaN categories).
             */
            if (categories.length != 0) {
                final Category category = categories[0];
                if (!isNaN(category.minimum)) {
                    return category;
                }
            }
        }
        return null;
    }

    /**
     * Returns the range of values in this category list. This is the union of the ranges of every categories,
     * excluding {@code NaN} values. A {@link NumberRange} object give more information than a (minimum, maximum)
     * tuple since it contains also the type (integer, float, etc.) and inclusion/exclusion information.
     *
     * @return The range of values. May be {@code null} if this category list has no quantitative category.
     *
     * @see Category#getRange()
     */
    public final NumberRange<?> getRange() {
        NumberRange<?> range = this.range;
        if (range == null) {
            for (final Category category : categories) {
                final NumberRange<?> extent = category.getRange();
                if (!isNaN(extent.getMinDouble()) && !isNaN(extent.getMaxDouble())) {
                    if (range != null) {
                        range = range.unionAny(extent);
                    } else {
                        range = extent;
                    }
                }
            }
            this.range = range;
        }
        return range;
    }




    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////       I M P L E M E N T A T I O N   O F   List   I N T E R F A C E       ////////
    ////////                                                                          ////////
    //////////////////////////////////////////////////////////////////////////////////////////

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
     * Returns all categories in this {@code CategoryList}.
     */
    @Override
    public final Category[] toArray() {
        Category[] array = categories;
        if (array.length != 0) {
            array = array.clone();
        }
        return array;
    }

    /**
     * Compares the specified object with this category list for equality.
     * If the two objects are instances of {@link CategoryList}, then the
     * test is a stricter than the default {@link AbstractList#equals(Object)}.
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




    ///////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                               ////////
    ////////    I M P L E M E N T A T I O N   O F   MathTransform1D   I N T E R F A C E    ////////
    ////////                                                                               ////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

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
     * Tests whether this transform does not move any points.
     */
    @Override
    public boolean isIdentity() {
        for (final Category category : categories) {
            final MathTransform1D tr = category.transferFunction;
            if (tr == null || !tr.isIdentity()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the inverse transform of this object.
     *
     * @todo Not yet implemented.
     */
    @Override
    public final MathTransform1D inverse() throws NoninvertibleTransformException {
        throw new NoninvertibleTransformException();
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
        ptDst.setOrdinate(0, transform(ptSrc.getOrdinate(0)));
        return ptDst;
    }

    /**
     * Gets the derivative of this transform at a point.
     */
    @Override
    public final Matrix derivative(final DirectPosition point) throws TransformException {
        ArgumentChecks.ensureNonNull("point", point);
        ArgumentChecks.ensureDimensionMatches("ptSrc", 1, point);
        return new Matrix1(derivative(point.getOrdinate(0)));
    }

    /**
     * Gets the derivative of this function at a value.
     *
     * @param  value  the value where to evaluate the derivative.
     * @return the derivative at the specified point.
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public final double derivative(final double value) throws TransformException {
        Category category = last;
        if (!(value >= category.minimum  &&  value <= category.maximum) &&
             doubleToRawLongBits(value) != doubleToRawLongBits(category.minimum))
        {
            category = getCategory(value);
            if (category == null) {
                throw new TransformException(Resources.format(Resources.Keys.NoCategoryForValue_1, value));
            }
            last = category;
        }
        return category.transferFunction.derivative(value);
    }

    /**
     * Transforms the specified value.
     *
     * @param value The value to transform.
     * @return the transformed value.
     * @throws TransformException if the value can't be transformed.
     */
    @Override
    public final double transform(double value) throws TransformException {
        Category category = last;
        if (!(value >= category.minimum  &&  value <= category.maximum) &&
             doubleToRawLongBits(value) != doubleToRawLongBits(category.minimum))
        {
            category = getCategory(value);
            if (category == null) {
                throw new TransformException(Resources.format(Resources.Keys.NoCategoryForValue_1, value));
            }
            last = category;
        }
        value = category.transferFunction.transform(value);
        if (SEARCH_NEAREST) {
//          if (value < category.inverse.minimum) return category.inverse.minimum;
//          if (value > category.inverse.maximum) return category.inverse.maximum;
        }
//      assert category == inverse.getCategory(value).inverse : category;
        return value;
    }

    /**
     * Transforms a list of coordinate point ordinal values. This implementation accepts
     * float or double arrays, since the quasi-totality of the implementation is the same.
     * Locale variables still of the {@code double} type because this is the type used in
     * {@link Category} objects.
     *
     * @todo We could add an optimization after the loops checking for category change:
     *       if we were allowed to search for nearest category (overflowFallback!=null),
     *       then make sure that the category really changed. There is already a slight
     *       optimization for the most common cases, but maybe we could go a little bit
     *       further.
     */
    private void transform(final double[] srcPts, final float[] srcFloat, int srcOff,
                           final double[] dstPts, final float[] dstFloat, int dstOff,
                           int numPts) throws TransformException
    {
        final int srcToDst = dstOff - srcOff;
        Category  category = last;
        double     maximum = category.maximum;
        double     minimum = category.minimum;
        long       rawBits = doubleToRawLongBits(minimum);
        final int direction;
        if (srcOff >= dstOff || (srcFloat != null ? srcFloat != dstFloat : srcPts != dstPts)) {
            direction = +1;
        } else {
            direction = -1;
            dstOff += numPts-1;             // Updated for safety, but not used.
            srcOff += numPts-1;
        }
        /*
         * Scan every points. Transforms will be performed by blocks, each time
         * the loop detects that the category has changed. The break point is near
         * the end of the loop, after we have done the transformation but before
         * to change category.
         */
        for (int peekOff=srcOff; true; peekOff += direction) {
            double value = 0;
            while (--numPts >= 0) {
                value = (srcFloat != null) ? srcFloat[peekOff] : srcPts[peekOff];
                if ((value >= minimum && value <= maximum) ||
                    doubleToRawLongBits(value) == rawBits)
                {
                    peekOff += direction;
                    continue;
                }
                break;                          // The category has changed. Stop the search.
            }
            if (SEARCH_NEAREST) {
                /*
                 * TODO: Slight optimization. We could go further by checking if 'value' is closer
                 *       to this category than to the previous category or the next category.  But
                 *       we may need the category index, and binarySearch is a costly operation...
                 */
//              if (value > maximum && category == overflowFallback) {
//                  continue;
//              }
                if (value < minimum && category == categories[0]) {
                    continue;
                }
            }
            /*
             * The category has changed. Compute the start point (which depends of 'direction')
             * and performs the transformation. If 'getCategory' was allowed to search for the
             * nearest category, clamp all output values in their category range.
             */
            int count = peekOff - srcOff;  // May be negative if we are going backward.
            if (count < 0) {
                count  = -count;
                srcOff -= count-1;
            }
            final int stepOff = srcOff + srcToDst;
            final MathTransform1D step = category.transferFunction;
            if (srcFloat != null) {
                if (dstFloat != null) {
                    step.transform(srcFloat, srcOff, dstFloat, stepOff, count);
                } else {
                    step.transform(srcFloat, srcOff, dstPts, stepOff, count);
                }
            } else {
                if (dstFloat != null) {
                    step.transform(srcPts, srcOff, dstFloat, stepOff, count);
                } else {
                    step.transform(srcPts, srcOff, dstPts, stepOff, count);
                }
            }
            if (SEARCH_NEAREST) {
                dstOff = srcOff + srcToDst;
                final Category inverse = null;  // TODO category.inverse;
                if (dstFloat != null) { // Loop for the 'float' version.
                    final float min = (float) inverse.minimum;
                    final float max = (float) inverse.maximum;
                    while (--count >= 0) {
                        final float check = dstFloat[dstOff];
                        if (check < min) {
                            dstFloat[dstOff] = min;
                        } else if (check > max) {
                            dstFloat[dstOff] = max;
                        }
                        dstOff++;
                    }
                } else { // Loop for the 'double' version.
                    final double min = inverse.minimum;
                    final double max = inverse.maximum;
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
             * Transformation is now finished for all points in the range [srcOff..peekOff]
             * (not including 'peekOff'). If there is more points to examine, gets the new
             * category for the next points.
             */
            if (numPts < 0) {
                break;
            }
            category = getCategory(value);
            if (category == null) {
                throw new TransformException(Resources.format(Resources.Keys.NoCategoryForValue_1, value));
            }
            maximum = category.maximum;
            minimum = category.minimum;
            rawBits = doubleToRawLongBits(minimum);
            srcOff  = peekOff;
        }
        last = category;
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    @Override
    public final void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        transform(srcPts, null, srcOff, dstPts, null, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    @Override
    public final void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        transform(null, srcPts, srcOff, null, dstPts, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    @Override
    public final void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        transform(null, srcPts, srcOff, dstPts, null, dstOff, numPts);
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     */
    @Override
    public final void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException {
        transform(srcPts, null, srcOff, null, dstPts, dstOff, numPts);
    }

    /**
     * Returns a <cite>Well Known Text</cite> (WKT) for this object. This operation
     * may fails if an object is too complex for the WKT format capability.
     *
     * @return the Well Know Text for this object.
     * @throws UnsupportedOperationException if this object can not be formatted as WKT.
     *
     * @todo Not yet implemented.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnformattableObjectException("Not yet implemented.");
    }
}
