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

import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;
import java.util.function.DoubleToIntFunction;
import java.io.Serializable;
import javax.measure.Unit;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Types;

import static java.lang.Double.doubleToRawLongBits;


/**
 * Describes a sub-range of sample values in a sample dimension.
 * A category maps a range of values to an observation, which may be either <em>qualitative</em> or <em>quantitative</em>:
 *
 * <ul class="verbose">
 *   <li><b>Examples of qualitative observations:</b>
 *   a sample dimension may have one {@code Category} instance specifying that sample value {@code 0} stands for water,
 *   another {@code Category} instance specifying that sample value {@code 1} stands for forest, <i>etc</i>.</li>
 *
 *   <li><b>Example of quantitative observation:</b>
 *   another sample dimension may have a {@code Category} instance specifying that sample values in the range [0…100]
 *   stands for elevation data. Those sample values are related to measurements in the real world (altitudes in metres)
 *   through a <cite>transfer function</cite>, foe example <var>altitude</var> = (<var>sample value</var>)×100 - 25.</li>
 * </ul>
 *
 * Some image mixes both qualitative and quantitative categories. For example, images of <cite>Sea Surface Temperature</cite>
 * (SST) may have a quantitative category for temperature with values ranging from -2 to 35°C, and three qualitative categories
 * for cloud, land and ice. There is usually at most one quantitative category per sample dimension, but Apache SIS accepts an
 * arbitrary amount of them.
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
 * @version 1.1
 * @since   1.0
 * @module
 */
public class Category implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2630516005075467646L;

    /**
     * Compares {@code Category} objects according their {@link NumberRange#getMinDouble(boolean)} value.
     */
    static final Comparator<Category> COMPARATOR = (Category c1, Category c2) ->
            Category.compare(c1.range.getMinDouble(true), c2.range.getMinDouble(true));

    /**
     * Compares two {@code double} values. This method is similar to {@link Double#compare(double,double)}
     * except that it also orders NaN values from raw bit patterns. Reminder: NaN values are sorted last.
     */
    static int compare(final double v1, final double v2) {
        if (Double.isNaN(v1) && Double.isNaN(v2)) {
            final long bits1 = doubleToRawLongBits(v1);
            final long bits2 = doubleToRawLongBits(v2);
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
     * The [minimum … maximum] range of values in this category (never {@code null}). Notes:
     *
     * <ul>
     *   <li>The minimum and maximum values may be one of the {@linkplain Float#isNaN() NaN} values (see below).</li>
     *   <li>The value type may be different than {@link Double} (typically {@link Integer}).</li>
     *   <li>The bounds may be exclusive instead of inclusive.</li>
     *   <li>The range may be an instance of {@link MeasurementRange} if the {@link #toConverse} is identity
     *       and the units of measurement are known.</li>
     * </ul>
     *
     * The range may be {@code NaN} if this category is a qualitative category converted to real values.
     * Those categories are characterized by two apparently contradictory properties,
     * and are implemented using {@link Float#NaN} values:
     *
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
     * categories. In the case of qualitative categories, this transfer function shall map to {@code NaN} values
     * or conversely. In the case of sample values that are already in the units of measurement, this transfer
     * function shall be the identity function.
     *
     * @see #getTransferFunction()
     */
    final MathTransform1D toConverse;

    /**
     * The category that describes values after {@linkplain #getTransferFunction() transfer function}
     * has been applied, or if this category is already converted then the original category.
     * Never null, but may be {@code this} if the transfer function is the identity function.
     *
     * <p>This field establishes a bidirectional navigation between sample values and real values.
     * This is in contrast with methods named {@code converted()}, which establish a unidirectional
     * navigation from sample values to real values.</p>
     *
     * @see #converted()
     * @see CategoryList#converse
     * @see SampleDimension#converse
     */
    final Category converse;

    /**
     * Creates a copy of the given category. This constructor is provided for subclasses
     * wanting to extent an existing category with custom information.
     *
     * @param copy  the category to copy.
     */
    protected Category(final Category copy) {
        name       = copy.name;
        range      = copy.range;
        toConverse = copy.toConverse;
        if (copy.converse == copy) {
            converse = this;
        } else {
            converse = new Category(copy.converse, this);
        }
    }

    /**
     * Creates a copy of the given category except for the {@link #converse} and {@link #toConverse} fields.
     * This constructor serves two purposes:
     * <ul>
     *   <li>If {@code caller} is null, then {@link #toConverse} is is set to identity.
     *       This is used only if a user specify a {@code ConvertedCategory} to {@link SampleDimension} constructor.
     *       Such converted category can only come from another {@code SampleDimension} and may have inconsistent
     *       information for the new sample dimension that the user is creating.</li>
     *   <li>If {@code caller} is non-null, then {@link #toConverse} is set to the same transform than {@code copy} and
     *       {@link #converse} is set to {@code caller}. This is used only as a complement for the copy constructor.</li>
     * </ul>
     *
     * @param copy    the category to copy.
     * @param caller  the converse, or {@code null} for {@code this}.
     */
    Category(final Category copy, final Category caller) {
        name  = copy.name;
        range = copy.range;
        if (caller != null) {
            toConverse = copy.toConverse;
            converse   = caller;
        } else {
            toConverse = identity();
            converse   = this;
        }
    }

    /**
     * Constructs a qualitative or quantitative category. This constructor is accessible for sub-classing.
     * For other usages, {@link SampleDimension.Builder} should be used instead.
     *
     * @param  name     the category name (mandatory).
     * @param  samples  the minimum and maximum sample values (mandatory).
     * @param  toUnits  the conversion from sample values to real values (possibly identity), or {@code null}
     *                  for constructing a qualitative category. Mandatory if {@code units} is non-null.
     * @param  units    the units of measurement, or {@code null} if not applicable.
     *                  This is the target units after conversion by {@code toUnits}.
     * @param  toNaN    mapping from sample values to ordinal values to be supplied to {@link MathFunctions#toNanFloat(int)}.
     *                  That mapping is used only if {@code toUnits} is {@code null} and {@code samples} are not NaN values.
     *                  That mapping is responsible to ensure that there is no ordinal value collision between different categories
     *                  in the same {@link SampleDimension}.
     *                  The input is a real number in the {@code samples} range and the output shall be a unique value between
     *                  {@value MathFunctions#MIN_NAN_ORDINAL} and {@value MathFunctions#MAX_NAN_ORDINAL} inclusive.
     * @throws IllegalSampleDimensionException if the {@code samples} range of values is empty
     *         or the transfer function can not be used.
     */
    protected Category(final CharSequence name, NumberRange<?> samples, final MathTransform1D toUnits, final Unit<?> units,
             final DoubleToIntFunction toNaN)
    {
        ArgumentChecks.ensureNonEmpty("name", name);
        ArgumentChecks.ensureNonNull("samples", samples);
        if (units != null) {
            ArgumentChecks.ensureNonNull("toUnits", toUnits);
            // The converse is not true: we allow `units` to be null even if `toUnits` is non-null.
        }
        this.name = Types.toInternationalString(name);
        final double  minimum = samples.getMinDouble(true);
        final double  maximum = samples.getMaxDouble(true);
        final boolean isNaN   = Double.isNaN(minimum);
        /*
         * Following arguments check uses `!` in comparison in order to reject NaN values in quantitative category.
         * For qualitative category, NaN is accepted provided that it is the same NaN for both ends of the range.
         */
        if (!(minimum <= maximum)) {
            if (toUnits != null || !isNaN || doubleToRawLongBits(minimum) != doubleToRawLongBits(maximum)) {
                throw new IllegalSampleDimensionException(Resources.format(Resources.Keys.IllegalCategoryRange_2, name, samples));
            }
        }
        if (isNaN) {
            range      = samples;
            converse   = this;
            toConverse = identity();
        } else try {
            /*
             * Creates the transform doing the inverse conversion (from real values to sample values).
             * This transform is assigned to a new Category object with its own minimum and maximum values.
             * Those minimum and maximum may be NaN if this category is a qualitative category.
             */
            final MathTransform1D toSamples;
            if (toUnits != null) {
                toConverse = toUnits;
                if (toUnits.isIdentity()) {
                    converse = this;
                    if (!(samples instanceof MeasurementRange<?>)) {
                        samples = new MeasurementRange<>(samples, units);   // Avoid ClassCastException in getMeasurementRange().
                    }
                    range = samples;
                    return;
                }
                toSamples = toUnits.inverse();
            } else {
                /*
                 * For qualitative category, the transfer function maps to NaN while the inverse function maps back
                 * to some value in the [minimum … maximum] range. We chose the value closest to positive zero.
                 */
                ArgumentChecks.ensureNonNull("toNaN", toNaN);
                final double value = (minimum > 0) ? minimum : (maximum <= 0) ? maximum : 0d;
                final float nan = MathFunctions.toNanFloat(toNaN.applyAsInt(value));
                toConverse = (MathTransform1D) MathTransforms.linear(0, nan);
                toSamples  = (MathTransform1D) MathTransforms.linear(0, value);
            }
            range = samples;
            converse = new ConvertedCategory(this, toSamples, toUnits != null, units);
        } catch (TransformException e) {
            throw new IllegalSampleDimensionException(Resources.format(Resources.Keys.IllegalTransferFunction_1, name), e);
        }
    }

    /**
     * Creates a category storing the inverse of the "sample to real values" transfer function. The {@link #toConverse}
     * of this category will convert real value in specified {@code units} to the sample (packed) value.
     * This constructor is reserved to {@link ConvertedCategory} usage only.
     *
     * @param  original        the category storing the conversion from sample to real value.
     * @param  toSamples       the "real to sample values" conversion, as the inverse of {@code original.toConverse}.
     *                         For qualitative category, this function is a constant mapping NaN to the original sample value.
     * @param  isQuantitative  {@code true} if we are construction a quantitative category, or {@code false} for qualitative.
     * @param  units           the units of measurement, or {@code null} if not applicable.
     *                         This is the source units before conversion by {@code toSamples}.
     */
    Category(final Category original, final MathTransform1D toSamples, final boolean isQuantitative, final Unit<?> units)
            throws TransformException
    {
        converse   = original;
        name       = original.name;
        toConverse = Objects.requireNonNull(toSamples);
        /*
         * Compute `minimum` and `maximum` (which must be real numbers) using the conversion from samples
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
        original.toConverse.transform(extremums, 0, extremums, 0, extremums.length);
        if (extremums[minIncluded ? 2 : 0] > extremums[maxIncluded ? 3 : 1]) {              // Compare exclusive min/max.
            ArraysExt.swap(extremums, 0, 1);                                                // Swap minimum and maximum.
            ArraysExt.swap(extremums, 2, 3);
            final boolean tmp = minIncluded;
            minIncluded = maxIncluded;
            maxIncluded = tmp;
        }
        if (isQuantitative) {
            range = new ConvertedRange(extremums, minIncluded, maxIncluded, units);
        } else {
            final double minimum = extremums[minIncluded ? 0 : 2];                          // Take inclusive value.
            final float min = (float) minimum;
            if (doubleToRawLongBits(minimum) == doubleToRawLongBits(min)) {
                range = NumberRange.create(Float.class, min);
            } else {
                range = NumberRange.create(Double.class, minimum);
            }
        }
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
     * The category that describes values after {@linkplain #getTransferFunction() transfer function} has been applied.
     * If the values are already converted (eventually to NaN values), returns {@code this}.  This method differs from
     * {@link #converse} field in being unidirectional: navigate from sample to converted values but never backward.
     *
     * @see #forConvertedValues(boolean)
     */
    Category converted() {
        return converse;        // Overridden in ConvertedCategory.
    }

    /**
     * Returns {@code true} if this category is a qualitative category that has been converted to "real values".
     * In such case, the real values are {@link Float#isNaN()} numbers. If {@code false}, then this category is
     * either a quantitative category or a qualitative category that has not been converted to "real values".
     */
    final boolean isConvertedQualitative() {
        return Double.isNaN(range.getMinDouble());
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
    public boolean isQuantitative() {
        return !converted().isConvertedQualitative();
    }

    /**
     * Returns the range of values occurring in this category. The range delimits sample values that can
     * be converted into real values using the {@linkplain #getTransferFunction() transfer function}.
     * If that function is {@linkplain MathTransform1D#isIdentity() identity}, then the sample values
     * are already real values and the range may be an instance of {@link MeasurementRange}
     * (i.e. a number range with units of measurement).
     *
     * <p>This method never returns {@code null}, but may return an {@linkplain NumberRange#isBounded() unbounded range}
     * or a range containing a singleton {@link Double#NaN} value. The {@code NaN} values happen if this range is derived
     * from a "no data" value converted to "real value" by the {@linkplain #getTransferFunction() transfer function}.</p>
     *
     * @return the range of sample values in this category.
     *
     * @see SampleDimension#getSampleRange()
     */
    public NumberRange<?> getSampleRange() {
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
        final NumberRange<?> mr = converted().range;
        if (Double.isNaN(mr.getMinDouble())) {
            return Optional.empty();
        } else {
            // A ClassCastException below would be a bug in our constructor.
            return Optional.of((MeasurementRange<?>) mr);
        }
    }

    /**
     * Returns an object to format for representing the range of values for display purpose only.
     * It may be either the {@link NumberRange}, a single {@link Number} or a {@link String} with
     * a text like "NaN #0".
     */
    final Object getRangeLabel() {
        if (range != null) {                                // Temporarily null during object construction.
            final Number minimum = range.getMinValue();
            if (minimum != null && minimum.equals(range.getMaxValue())) {
                final float f = minimum.floatValue();
                if (Float.isNaN(f)) {
                    return "NaN #" + MathFunctions.toNanOrdinal(f);
                } else {
                    return minimum;
                }
            }
        }
        return range;
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
         * Note: if this method is invoked on "real values category", then we need to return
         * the identity transform instead of `toConverse`. This is done by ConvertedCategory.
         */
        if (converse.isConvertedQualitative()) {
            return Optional.empty();
        } else {
            return Optional.of(toConverse);
        }
    }

    /**
     * Returns a category that describes measurement values or packed values,
     * depending if {@code converted} is {@code true} or {@code false} respectively.
     * Notes:
     *
     * <ul class="verbose">
     *   <li>The converted values of a qualitative category is a NaN value.</li>
     *   <li>The converted values of a {@linkplain #isQuantitative() quantitative} category are real values.
     *       Those values are computed by the {@linkplain #getTransferFunction() transfer function}.
     *       That function may be identity, in which case this method returns {@code this}.</li>
     * </ul>
     *
     * @param  converted  {@code true} for a category describing values in units of measurement,
     *                    or {@code false} for a category describing packed values (usually as integers).
     * @return a category describing converted or packed values, depending on {@code converted} argument value.
     *         May be {@code this} but never {@code null}.
     *
     * @see #getMeasurementRange()
     * @see SampleDimension#forConvertedValues(boolean)
     *
     * @since 1.1
     */
    public Category forConvertedValues(final boolean converted) {
        return converted ? converse : this;                             // Overridden in ConvertedCategory.
    }

    /**
     * Returns the identity transform. This is the value returned by {@link ConvertedCategory#getTransferFunction()}.
     */
    static MathTransform1D identity() {
        return (MathTransform1D) MathTransforms.identity(1);
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
     * @return {@code true} if the given object is equal to this category.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (object != null && getClass().equals(object.getClass())) {
            final Category that = (Category) object;
            if (name.equals(that.name)) {
                final NumberRange<?> other = that.range;
                /*
                 * The NumberRange.equals(Object) comparison is not sufficient because it considers all NaN values as equal.
                 * For the purpose of Category, we need to distinguish the different NaN values.
                 */
                if (range == other || (range.equals(other)
                        && doubleToRawLongBits(range.getMinDouble()) == doubleToRawLongBits(other.getMinDouble())
                        && doubleToRawLongBits(range.getMaxDouble()) == doubleToRawLongBits(other.getMaxDouble())))
                {
                    return toConverse.equals(that.toConverse);
                }
            }
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
