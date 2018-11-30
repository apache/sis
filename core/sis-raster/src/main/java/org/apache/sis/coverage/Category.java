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
import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.Types;


/**
 * A category delimited by a range of sample values. A category may be either <em>qualitative</em> or <em>quantitative</em>.
 * For example, a classified image may have a qualitative category defining sample value {@code 0} as water.
 * An other qualitative category may defines sample value {@code 1} as forest, <i>etc</i>.
 * An other image may define elevation data as sample values in the range [0…100].
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
 * may define a conversion between sample values <var>s</var> and geophysics values <var>x</var>.
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
     * except that it also orders NaN values from raw bit patterns. Remind that NaN values are sorted last.
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
     * A default category for "no data" values. This default qualitative category uses sample value 0,
     * which is mapped to geophysics value {@link Float#NaN}. The name is "no data".
     */
    static final Category NODATA = new Category(Vocabulary.formatInternational(Vocabulary.Keys.Nodata),
                                                NumberRange.create(0, true, 0, true), null);

    /**
     * The category name.
     */
    private final InternationalString name;

    /**
     * The minimal and maximal sample value (inclusive).
     * This category is made of all values in the range {@code minimum} to {@code maximum} inclusive.
     * This value may be one of the multiple possible {@code NaN} values if this category stands for
     * "no data" after all values have been converted to geophysics values.
     */
    final double minimum, maximum;

    /**
     * The [{@linkplain #minimum} … {@linkplain #maximum}] range of values.
     * May be computed only when first requested, or may be user-supplied (which is why it must be serialized).
     */
    private final NumberRange<?> range;

    /**
     * The conversion from sample values to geophysics values, or {@code null} if this category is qualitative.
     */
    final MathTransform1D transferFunction;

    /**
     * Constructs a category with the specified transfer function.
     *
     * @param  name              the category name.
     * @param  range             the minimum and maximum sample values.
     * @param  transferFunction  the conversion from sample values to geophysics values, or {@code null}.
     */
    Category(final CharSequence name, final NumberRange<?> range, final MathTransform1D transferFunction) {
        ArgumentChecks.ensureNonNull("name",  name);
        ArgumentChecks.ensureNonNull("range", range);
        this.name    = Types.toInternationalString(name);
        this.range   = range;
        this.minimum = range.getMinDouble(true);
        this.maximum = range.getMaxDouble(true);
        this.transferFunction = transferFunction;
        /*
         * If we are constructing a qualitative category for a single NaN value,
         * accepts it as a valid one.
         */
        if (transferFunction == null && Double.isNaN(minimum) &&
                Double.doubleToRawLongBits(minimum) == Double.doubleToRawLongBits(maximum))
        {
            return;
        }
        /*
         * Check the arguments. Use '!' in comparison in order to reject NaN values,
         * except for the legal case catched by the "if" block just above.
         */
        if (!(minimum <= maximum) || (minimum == Double.NEGATIVE_INFINITY) || (maximum == Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2,
                                               range.getMinValue(), range.getMaxValue()));
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
     * Returns the range of values occurring in this category.
     * The range are sample values than can be converted into geophysics values using the
     * {@linkplain #getTransferFunction() transfer function}. If that function is identity,
     * then the sample values are already geophysics values and are in the units of the
     * {@link SampleDimension} containing this category.
     *
     * @return the range of sample values.
     *
     * @see NumberRange#getMinValue()
     * @see NumberRange#getMaxValue()
     * @see SampleDimension#getRange()
     */
    public NumberRange<?> getRange() {
        return range;
    }

    /**
     * Returns an object to format for representing the range of values for display purpose only.
     * It may be either the {@link NumberRange} or a {@link String} with a text like "NaN #0".
     */
    final Object getRangeLabel() {
        if (Double.isNaN(minimum)) {
            return "NaN #" + MathFunctions.toNanOrdinal((float) minimum);
        } else {
            return range;
        }
    }

    /**
     * Returns the <cite>transfer function</cite> from sample values to geophysics values.
     * The function is absent if this category is not a quantitative category.
     *
     * @return the <cite>transfer function</cite> from sample values to geophysics values.
     *
     * @see SampleDimension#getTransferFunction()
     */
    public Optional<MathTransform1D> getTransferFunction() {
        return Optional.ofNullable(transferFunction);
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
            return name.equals(that.name) && range.equals(that.range) &&
                   Double.doubleToRawLongBits(minimum) == Double.doubleToRawLongBits(that.minimum) &&
                   Double.doubleToRawLongBits(maximum) == Double.doubleToRawLongBits(that.maximum) &&
                   Objects.equals(transferFunction, that.transferFunction);
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
        return new StringBuilder(getClass().getSimpleName()).append("(“").append(name)
                .append("”:").append(getRangeLabel()).append(')').toString();
    }
}
