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

import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;
import java.io.Serializable;
import javax.measure.Unit;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Types;


/**
 * A category delimited by a range of sample values. A category may be either <em>qualitative</em> or <em>quantitative</em>.
 * For example an image may have a qualitative category defining sample value {@code 0} as water,
 * another qualitative category defining sample value {@code 1} as forest, <i>etc</i>.
 * Another image may define elevation data as sample values in the range [0…100].
 * The later is a <em>quantitative</em> category because sample values are related to measurements in the real world.
 * For example, elevation data may be related to an altitude in metres through the following linear relation:
 *
 * <blockquote><var>altitude</var> = (<var>sample value</var>)×100</blockquote>
 *
 * Some image mixes both qualitative and quantitative categories. For example, images of <cite>Sea Surface Temperature</cite>
 * (SST) may have a quantitative category for temperature with values ranging from -2 to 35°C, and three qualitative categories
 * for cloud, land and ice.
 *
 * <p>All categories must have a human readable name. In addition, quantitative categories
 * may define a conversion from sample values <var>s</var> to real values <var>x</var>.
 * This conversion is usually (but not always) a linear equation of the form:</p>
 *
 * <blockquote><var>x</var> = offset + scale × <var>s</var></blockquote>
 *
 * More general equation are allowed. For example, <cite>SeaWiFS</cite> images use a logarithmic transform.
 * General conversions are expressed with a {@link MathTransform1D} object.
 *
 * <p>All {@code Category} objects are immutable and thread-safe.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Category implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6215962897884256696L;

    /**
     * Compares {@code Category} objects according their {@link #minimum} value.
     */
    static final Comparator<Category> COMPARATOR = (Category c1, Category c2) -> Category.compare(c1.minimum, c2.minimum);

    /**
     * Compares two {@code double} values. This method is similar to {@link Double#compare(double,double)}
     * except that it also orders NaN values from raw bit patterns. Reminder: NaN values are sorted last.
     */
    static int compare(final double v1, final double v2) {
        if (Double.isNaN(v1) && Double.isNaN(v2)) {
            final long bits1 = Double.doubleToRawLongBits(v1);
            final long bits2 = Double.doubleToRawLongBits(v2);
            if (bits1 < bits2) return -1;
            if (bits1 > bits2) return +1;
        }
        return Double.compare(v1, v2);
    }

    /**
     * The category name.
     *
     * @see #getName()
     */
    final InternationalString name;

    /**
     * The minimal and maximal sample value (inclusive). If {@link #range} is non-null, then
     * those fields are equal to the following values, extracted for performance reasons:
     *
     * <ul>
     *   <li>{@code minimum == range.getMinDouble(true)}</li>
     *   <li>{@code maximum == range.getMaxDouble(true)}</li>
     * </ul>
     *
     * If {@link #range} is null, then those values shall be one of the multiple possible {@code NaN} values.
     * This means that this category stands for "no data" after all sample values have been converted to real values.
     */
    final double minimum, maximum;

    /**
     * The [{@linkplain #minimum} … {@linkplain #maximum}] range of values, or {@code null} if that range would
     * contain {@link Float#NaN} bounds. This is partially redundant with the minimum and maximum fields, except
     * for the following differences:
     *
     * <ul>
     *   <li>This field is {@code null} if the minimum and maximum values are NaN (converted qualitative category).</li>
     *   <li>The value type may be different than {@link Double} (typically {@link Integer}).</li>
     *   <li>The bounds may be exclusive instead than inclusive.</li>
     *   <li>The range may be an instance of {@link MeasurementRange} if the {@link #transferFunction}
     *       is identity and the units of measurement are known.</li>
     * </ul>
     *
     * The range is null if this category is a qualitative category converted to real values.
     * Those categories are characterized by two apparently contradictory properties,
     * and are implemented using {@link Float#NaN} values:
     * <ul>
     *   <li>This category is member of a {@code SampleDimension} having an identity
     *       {@linkplain SampleDimension#getTransferFunction() transfer function}.</li>
     *   <li>The {@linkplain #getTransferFunction() transfer function} of this category
     *       is absent (because this category is qualitative).</li>
     * </ul>
     *
     * @see #getSampleRange()
     */
    final NumberRange<?> range;

    /**
     * The conversion from sample values to real values (or conversely), never {@code null} even for qualitative
     * categories. In the case of qualitative categories, this transfer function shall map to {@code NaN} values.
     * In the case of sample values that are already in the units of measurement, this transfer function shall be
     * the identity function.
     */
    final MathTransform1D transferFunction;

    /**
     * The category that describes sample values after {@link #transferFunction} has been applied.
     * Never null, but may be {@code this} if the transfer function is the identity function.
     */
    final Category converted;

    /**
     * Constructs a qualitative of quantitative category.
     *
     * @param  name       the category name (mandatory).
     * @param  samples    the minimum and maximum sample values (mandatory).
     * @param  toUnits    the conversion from sample values to real values,
     *                    or {@code null} for constructing a qualitative category.
     * @param  units      the units of measurement, or {@code null} if not applicable.
     *                    This is the target units after conversion by {@code toUnits}.
     * @param  padValues  an initially empty set to be filled by this constructor for avoiding pad value collisions.
     *                    The same set shall be given to all {@code Category} created for the same sample dimension.
     */
    Category(final CharSequence name, final NumberRange<?> samples, final MathTransform1D toUnits, final Unit<?> units,
             final Set<Integer> padValues)
    {
        ArgumentChecks.ensureNonEmpty("name", name);
        ArgumentChecks.ensureNonNull("samples", samples);
        this.name    = Types.toInternationalString(name);
        this.range   = samples;
        this.minimum = samples.getMinDouble(true);
        this.maximum = samples.getMaxDouble(true);
        /*
         * Following arguments check uses '!' in comparison in order to reject NaN values.
         */
        if (!(minimum <= maximum) || (minimum == Double.NEGATIVE_INFINITY) || (maximum == Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalCategoryRange_2, name, samples));
        }
        /*
         * Creates the transform doing the inverse conversion (from real values to sample values).
         * This transform is assigned to a new Category object with its own minimum and maximum values.
         * Those minimum and maximum may be NaN if this category is a qualitative category.
         */
        try {
            final MathTransform1D toSamples;
            if (toUnits != null) {
                transferFunction = toUnits;
                if (toUnits.isIdentity()) {
                    converted = this;
                    return;
                }
                toSamples = toUnits.inverse();
            } else {
                /*
                 * For qualitative category, we need an ordinal in the [MIN_NAN_ORDINAL … MAX_NAN_ORDINAL] range.
                 * This range is quite large (a few million of values) so using the sample directly usually work.
                 * If it does not work, we will use an arbitrary value in that range.
                 */
                int ordinal = Math.round((float) minimum);
                if (ordinal > MathFunctions.MAX_NAN_ORDINAL) {
                    ordinal = (MathFunctions.MAX_NAN_ORDINAL + 1) / 2;
                } else if (ordinal < MathFunctions.MIN_NAN_ORDINAL) {
                    ordinal = MathFunctions.MIN_NAN_ORDINAL / 2;
                }
search:         if (!padValues.add(ordinal)) {
                    /*
                     * Following algorithms are inefficient, but those loops should be rarely needed.
                     * They are executed only if many qualitative sample values are outside the range
                     * of ordinal NaN values. The range allows a few million of values.
                     */
                    if (ordinal >= 0) {
                        do if (padValues.add(++ordinal)) break search;
                        while (ordinal < MathFunctions.MAX_NAN_ORDINAL);
                    } else {
                        do if (padValues.add(--ordinal)) break search;
                        while (ordinal > MathFunctions.MIN_NAN_ORDINAL);
                    }
                    throw new IllegalStateException(Resources.format(Resources.Keys.TooManyQualitatives));
                }
                /*
                 * For qualitative category, the transfer function maps to NaN while the inverse function maps back
                 * to some value in the [minimum … maximum] range. We chose the value closest to positive zero.
                 */
                transferFunction = (MathTransform1D) MathTransforms.linear(0, MathFunctions.toNanFloat(ordinal));
                final double value = (minimum > 0) ? minimum : (maximum <= 0) ? maximum : 0d;
                toSamples = (MathTransform1D) MathTransforms.linear(0, value);
            }
            converted = new Category(this, toSamples, toUnits != null, units);
        } catch (TransformException e) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalTransferFunction_1, name), e);
        }
    }

    /**
     * Creates a category storing the inverse of the "sample to real values" transfer function. The {@link #transferFunction}
     * of this category will convert real value in specified {@code units} to the sample (packed) value.
     *
     * @param  original        the category storing the conversion from sample to real value.
     * @param  toSamples       the "real to sample values" conversion, as the inverse of {@code original.transferFunction}.
     *                         For qualitative category, this function is a constant mapping NaN to the original sample value.
     * @param  isQuantitative  {@code true} if we are construction a quantitative category, or {@code false} for qualitative.
     * @param  units           the units of measurement, or {@code null} if not applicable.
     *                         This is the source units before conversion by {@code toSamples}.
     */
    private Category(final Category original, final MathTransform1D toSamples, final boolean isQuantitative, final Unit<?> units)
            throws TransformException
    {
        converted        = original;
        name             = original.name;
        transferFunction = Objects.requireNonNull(toSamples);
        /*
         * Compute 'minimum' and 'maximum' (which must be real numbers) using the conversion from samples
         * to real values. To be strict, we should use some numerical algorithm for finding a function's
         * minimum and maximum. For linear and logarithmic functions, minimum and maximum are always at
         * the bounding input values, so we are using a very simple algorithm for now.
         *
         * Note: we could move this code in ConvertedRange constructor if RFE #4093999
         * ("Relax constraint on placement of this()/super() call in constructors") was fixed.
         */
        final NumberRange<?> r = original.range;
        boolean minIncluded = r.isMinIncluded();
        boolean maxIncluded = r.isMaxIncluded();
        final double[] extremums = {
                r.getMinDouble(),
                r.getMaxDouble(),
                r.getMinDouble(!minIncluded),
                r.getMaxDouble(!maxIncluded)};
        original.transferFunction.transform(extremums, 0, extremums, 0, extremums.length);
        if (extremums[minIncluded ? 2 : 0] > extremums[maxIncluded ? 3 : 1]) {              // Compare exclusive min/max.
            ArraysExt.swap(extremums, 0, 1);                                                // Swap minimum and maximum.
            ArraysExt.swap(extremums, 2, 3);
            final boolean tmp = minIncluded;
            minIncluded = maxIncluded;
            maxIncluded = tmp;
        }
        minimum = extremums[minIncluded ? 0 : 2];                                           // Store inclusive values.
        maximum = extremums[maxIncluded ? 1 : 3];
        if (isQuantitative) {
            range = new ConvertedRange(extremums, minIncluded, maxIncluded, units);
        } else {
            range = null;
        }
    }

    /**
     * Returns {@code false} if this instance has been created by above private constructor for real values.
     * This method is for assertions only. We use the range type as a signature for category representing result
     * of conversion by the transfer function.
     */
    final boolean isPublic() {
        return (range != null) && !(range instanceof ConvertedRange);
    }

    /**
     * Returns the category name.
     *
     * @return the category name.
     */
    public InternationalString getName() {
        return name;
    }

    /**
     * Returns {@code true} if this category is quantitative. A quantitative category has a
     * {@linkplain #getTransferFunction() transfer function} mapping sample values to values
     * in some units of measurement. By contrast, a qualitative category maps sample values
     * to a label, for example “2 = forest”. That later mapping can not be represented by a
     * transfer function.
     *
     * @return {@code true} if this category is quantitative, or
     *         {@code false} if this category is qualitative.
     */
    public final boolean isQuantitative() {
        /*
         * This implementation assumes that this method will always be invoked on the instance
         * created for sample values, never on the instance created by the private constructor.
         * If this method was invoked on "real values category", then we would need to test for
         * 'range' directly instead of 'converted.range'.
         */
        assert isPublic() : this;
        return converted.range != null;
    }

    /**
     * Returns the range of values occurring in this category. The range delimits sample values that can
     * be converted into real values using the {@linkplain #getTransferFunction() transfer function}.
     * If that function is {@linkplain MathTransform1D#isIdentity() identity}, then the sample values
     * are already real values and the range may be an instance of {@link MeasurementRange}
     * (i.e. a number range with units of measurement).
     *
     * @return the range of sample values in this category.
     *
     * @see SampleDimension#getSampleRange()
     * @see NumberRange#getMinValue()
     * @see NumberRange#getMaxValue()
     */
    public NumberRange<?> getSampleRange() {
        // Same assumption than in 'isQuantitative()'.
        assert isPublic() : this;
        return range;
    }

    /**
     * Returns the range of values after conversions by the transfer function.
     * This range is absent if there is no transfer function, i.e. if this category is qualitative.
     *
     * @return the range of values after conversion by the transfer function.
     *
     * @see SampleDimension#getMeasurementRange()
     */
    public Optional<MeasurementRange<?>> getMeasurementRange() {
        // Same assumption than in 'isQuantitative()'.
        assert isPublic() : this;
        // A ClassCastException below would be a bug in our constructor.
        return Optional.ofNullable((MeasurementRange<?>) converted.range);
    }

    /**
     * Returns an object to format for representing the range of values for display purpose only.
     * It may be either the {@link NumberRange}, a single {@link Number} or a {@link String} with
     * a text like "NaN #0".
     */
    final Object getRangeLabel() {
        if (Double.isNaN(minimum)) {
            return "NaN #" + MathFunctions.toNanOrdinal((float) minimum);
        } else if (minimum == maximum) {
            return range.getMinValue();
        } else {
            return range;
        }
    }

    /**
     * Returns the <cite>transfer function</cite> from sample values to real values in units of measurement.
     * The function is absent if this category is not a {@linkplain #isQuantitative() quantitative} category.
     *
     * @return the <cite>transfer function</cite> from sample values to real values.
     *
     * @see SampleDimension#getTransferFunction()
     */
    public Optional<MathTransform1D> getTransferFunction() {
        /*
         * This implementation assumes that this method will always be invoked on the instance
         * created for sample values, never on the instance created by the private constructor.
         * If this method was invoked on "real values category", then we would need to return
         * the identity transform instead than 'transferFunction'.
         */
//      assert isPublic();     — invoked by isQuantitative().
        return isQuantitative() ? Optional.of(transferFunction) : Optional.empty();
    }

    /**
     * Returns a hash value for this category. This value needs not remain consistent between
     * different implementations of the same class.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Compares the specified object with this category for equality.
     *
     * @param  object the object to compare with.
     * @return {@code true} if the given object is equals to this category.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (object instanceof Category) {
            final Category that = (Category) object;
            return name.equals(that.name) && Objects.equals(range, that.range) &&
                   Double.doubleToRawLongBits(minimum) == Double.doubleToRawLongBits(that.minimum) &&
                   Double.doubleToRawLongBits(maximum) == Double.doubleToRawLongBits(that.maximum) &&
                   transferFunction.equals(that.transferFunction);
        }
        return false;
    }

    /**
     * Returns a string representation of this category for debugging purpose.
     * This string representation may change in any future SIS version.
     *
     * @return a string representation of this category for debugging purpose.
     */
    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[“").append(name)
                .append("”: ").append(getRangeLabel()).append(']').toString();
    }
}
