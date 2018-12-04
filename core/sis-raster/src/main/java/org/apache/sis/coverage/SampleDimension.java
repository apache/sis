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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.Collections;
import java.io.Serializable;
import javax.measure.Unit;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;


/**
 * Describes the data values in a coverage (the range). For a raster, a sample dimension is a band.
 * A sample dimension can reserve some values for <cite>qualitative</cite> information like  “this
 * is a forest” and some other values for <cite>quantitative</cite> information like a temperature
 * measurements.
 *
 * <div class="note"><b>Example:</b>
 * an image of sea surface temperature (SST) could define the following categories:
 * <table class="sis">
 *   <caption>Example of categories in a sample dimension</caption>
 *   <tr><th>Values range</th> <th>Meaning</th></tr>
 *   <tr><td>[0]</td>          <td>No data</td></tr>
 *   <tr><td>[1]</td>          <td>Cloud</td></tr>
 *   <tr><td>[2]</td>          <td>Land</td></tr>
 *   <tr><td>[10…210]</td>     <td>Temperature to be converted into Celsius degrees through a linear equation</td></tr>
 * </table>
 * In this example, sample values in range [10…210] define a quantitative category, while all others categories are qualitative.
 * </div>
 *
 * <div class="section">Relationship with metadata</div>
 * This class provides the same information than ISO 19115 {@link org.opengis.metadata.content.SampleDimension},
 * but organized in a different way. The use of the same name may seem a risk, but those two types are typically
 * not used in same time.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class SampleDimension implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6026936545776852758L;

    /**
     * Description for this sample dimension. Typically used as a way to perform a band select by
     * using human comprehensible descriptions instead of just numbers. Web Coverage Service (WCS)
     * can use this name in order to perform band sub-setting as directed from a user request.
     *
     * @see #getName()
     */
    private final InternationalString name;

    /**
     * The list of categories making this sample dimension. May be empty but shall never be null.
     */
    private final CategoryList categories;

    /**
     * The transform from samples to real values. May be {@code null} if this sample dimension
     * does not defines any transform (which is not the same that defining an identity transform).
     *
     * @see #getTransferFunction()
     */
    private final MathTransform1D transferFunction;

    /**
     * Creates a sample dimension with the specified properties.
     *
     * @param name        the sample dimension title or description, or {@code null} for default.
     * @param categories  the list of categories. This list is not cloned and may be modified in-place.
     */
    SampleDimension(InternationalString name, final Category[] categories) {
        if (name == null) {
            name = Vocabulary.formatInternational(Vocabulary.Keys.Untitled);
        }
        final CategoryList list = new CategoryList(categories, null);
        this.name       = name;
        this.categories = list;
        MathTransform1D tr = null;
        if (list.hasQuantitative()) {
            tr = categories[0].transferFunction;
            for (int i=1; i<categories.length; i++) {
                if (!tr.equals(categories[i].transferFunction)) {
                    tr = list;
                    break;
                }
            }
        }
        transferFunction = tr;
    }

    /**
     * Returns a name or description for this sample dimension. This is typically used as a way to perform a band select
     * by using human comprehensible descriptions instead of just numbers. Web Coverage Service (WCS) can use this name
     * in order to perform band sub-setting as directed from a user request.
     *
     * @return the title or description of this sample dimension.
     */
    public InternationalString getName() {
        return name;
    }

    /**
     * Returns the values to indicate "no data" for this sample dimension.
     *
     * @return the values to indicate no data values for this sample dimension, or an empty list if none.
     */
    public List<Number> getNoDataValues() {
        if (!categories.hasQuantitative()) {
            return Collections.emptyList();
        }
        final List<Number> noDataValues = new ArrayList<>(categories.size());
        for (final Category c : categories) {
            if (!c.isQuantitative()) {
                final NumberRange<?> r = c.range;
                final Number value;
                if (r.isMinIncluded()) {
                    value = r.getMinDouble();
                } else if (r.isMaxIncluded()) {
                    value = r.getMaxDouble();
                } else {
                    value = (c.minimum + c.maximum) / 2;
                }
                noDataValues.add(value);
            }
        }
        return noDataValues;
    }

    /**
     * Returns the range of values occurring in this sample dimension. The range delimits sample values that
     * can be converted into real values using the {@linkplain #getTransferFunction() transfer function}.
     * If that function is {@linkplain MathTransform1D#isIdentity() identity}, then the values are already
     * real values and the range may be an instance of {@link MeasurementRange}
     * (i.e. a number range with units of measurement).
     *
     * @return the range of sample values in this sample dimension.
     */
    public Optional<NumberRange<?>> getSampleRange() {
        return Optional.ofNullable(categories.range);
    }

    /**
     * Returns the range of values after conversions by the transfer function.
     * If a unit of measurement is available, then this range will be an instance of {@link MeasurementRange}.
     * This range is absent if there is no transfer function.
     *
     * @return the range of values after conversion by the transfer function.
     */
    public Optional<NumberRange<?>> getMeasurementRange() {
        return Optional.ofNullable(categories.converted.range);
    }

    /**
     * Returns the <cite>transfer function</cite> from sample values to real values.
     * This method returns a transform expecting sample values as input and computing real values as output.
     * The output units of measurement is given by {@link #getUnits()}.
     * This transform will take care of converting all "{@linkplain #getNoDataValues() no data values}" into {@code NaN} values.
     * The <code>transferFunction.{@linkplain MathTransform1D#inverse() inverse()}</code> transform is capable to differentiate
     * {@code NaN} values to get back the original sample value.
     *
     * @return the <cite>transfer function</cite> from sample to real values. May be absent if this sample dimension
     *         do not defines any transform (which is not the same that defining an identity transform).
     *
     * @see TransferFunction
     */
    public Optional<MathTransform1D> getTransferFunction() {
        return Optional.ofNullable(transferFunction);
    }

    /**
     * Returns the units of measurement for this sample dimension.
     * This unit applies to values obtained after the {@linkplain #getTransferFunction() transfer function}.
     * May be absent if not applicable.
     *
     * @return the units of measurement.
     */
    public Optional<Unit<?>> getUnits() {
        Unit<?> units = null;
        for (final Category c : categories.converted) {
            final NumberRange<?> r = c.range;
            if (r instanceof MeasurementRange<?>) {
                final Unit<?> u = ((MeasurementRange<?>) r).unit();
                if (u != null) {
                    if (units == null) {
                        units = u;
                    } else if (!units.equals(u)) {
                        units = null;                   // Different quantitative categories use different units.
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(units);
    }

    /**
     * Returns a string representation of this sample dimension.
     * This string is for debugging purpose only and may change in future version.
     *
     * @return a string representation of this sample dimension for debugging purpose.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + "[“" + name + "”]";
    }




    /**
     * A mutable builder for creating an immutable {@link SampleDimension}.
     * The following properties can be set:
     *
     * <ul>
     *   <li>An optional name for the {@code SampleDimension}.</li>
     *   <li>An arbitrary amount of <cite>qualitative</cite> categories.</li>
     *   <li>An arbitrary amount of <cite>quantitative</cite> categories.</li>
     * </ul>
     *
     * A <cite>qualitative category</cite> is a range of sample values associated to a label (not numbers).
     * For example 0 = cloud, 1 = sea, 2 = land, <i>etc</i>.
     * A <cite>quantitative category</cite> is a range of sample values associated to numbers with units of measurement.
     * For example 10 = 1.0°C, 11 = 1.1°C, 12 = 1.2°C, <i>etc</i>.
     * Those two kind of categories are created by the following methods:
     *
     * <ul>
     *   <li>{@link #addQualitative(CharSequence, NumberRange)}</li>
     *   <li>{@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}</li>
     * </ul>
     *
     * All other {@code addQualitative(…)} and {@code addQuantitative(…)} methods are convenience methods delegating
     * to above-cited methods. Qualitative and quantitative categories can be mixed in the same {@link SampleDimension},
     * provided that their ranges do not overlap.
     * After properties have been set, the sample dimension is created by invoking {@link #build()}.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
     */
    public static class Builder {
        /**
         * Description for this sample dimension.
         */
        private CharSequence dimensionName;

        /**
         * The categories for this sample dimension.
         */
        private final List<Category> categories;

        /**
         * The ordinal NaN values used for this sample dimension.
         * The {@link Category} constructor uses this set for avoiding collisions.
         */
        private final Set<Integer> padValues;

        /**
         * Creates an initially empty builder for a sample dimension.
         * Callers shall invoke at least one {@code addFoo(…)} method before {@link #build()}.
         */
        public Builder() {
            categories = new ArrayList<>();
            padValues  = new HashSet<>();
        }

        /**
         * Sets the name or description of the sample dimension.
         * This is the value to be returned by {@link SampleDimension#getName()}.
         *
         * @param  name the name or description of the sample dimension.
         * @return {@code this}, for method call chaining.
         */
        public Builder setName(final CharSequence name) {
            dimensionName = name;
            return this;
        }

        /**
         * Adds a qualitative category for samples of the given boolean value.
         * The {@code true} value is represented by 1 and the {@code false} value is represented by 0.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as a boolean.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final boolean sample) {
            final byte value = sample ? (byte) 1 : 0;
            return addQualitative(name, NumberRange.create(value, true, value, true));
        }

        /**
         * Adds a qualitative category for samples of the given tiny (8 bits) integer value.
         * The argument is treated as a signed integer ({@value Byte#MIN_VALUE} to {@value Byte#MAX_VALUE}).
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as an integer.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final byte sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given short (16 bits) integer value.
         * The argument is treated as a signed integer ({@value Short#MIN_VALUE} to {@value Short#MAX_VALUE}).
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as an integer.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final short sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given integer value.
         * The argument is treated as a signed integer ({@value Integer#MIN_VALUE} to {@value Integer#MAX_VALUE}).
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as an integer.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final int sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given floating-point value.
         * The given value can not be {@link Float#NaN NaN}.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as a real number.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if the given value is NaN.
         */
        public Builder addQualitative(final CharSequence name, final float sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given double precision floating-point value.
         * The given value can not be {@link Double#NaN NaN}.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as a real number.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if the given value is NaN.
         */
        public Builder addQualitative(final CharSequence name, final double sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for all samples in the specified range of values.
         * This is the most generic method for adding a qualitative category.
         * All other {@code addQualitative(name, …)} methods are convenience methods delegating their work to this method.
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object,
         *                  or {@code null} for a default "no data" name.
         * @param  samples  the minimum and maximum sample values in the category.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if the given range is empty.
         */
        public Builder addQualitative(CharSequence name, final NumberRange<?> samples) {
            if (name == null) {
                name = Vocabulary.formatInternational(Vocabulary.Keys.Nodata);
            }
            categories.add(new Category(name, samples, null, null, padValues));
            return this;
        }

        /**
         * Constructs a quantitative category mapping samples to real values in the specified range.
         * Sample values in the {@code samples} range will be mapped to real values in the {@code geophysics} range
         * through a linear equation of the form:
         *
         * <blockquote><var>measure</var> = <var>sample</var> × <var>scale</var> + <var>offset</var></blockquote>
         *
         * where <var>scale</var> and <var>offset</var> coefficients are computed from the ranges supplied in arguments.
         * The units of measurement will be taken from the {@code geophysics} range if it is an instance of {@link MeasurementRange}.
         *
         * <p><b>Warning:</b> this method is provided for convenience when the scale and offset factors are not explicitly specified.
         * If those factor are available, then the other {@code addQuantitative(name, samples, …)} methods are more reliable.</p>
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}.</div>
         *
         * @param  name        the category name as a {@link String} or {@link InternationalString} object.
         * @param  samples     the minimum and maximum sample values in the category. Element class is usually
         *                     {@link Integer}, but {@link Float} and {@link Double} types are accepted as well.
         * @param  geophysics  the range of real values for this category, as an instance of {@link MeasurementRange}
         *                     if those values are associated to an unit of measurement.
         * @return {@code this}, for method call chaining.
         * @throws ClassCastException if the range element class is not a {@link Number} subclass.
         * @throws IllegalArgumentException if the range is invalid.
         */
        public Builder addQuantitative(final CharSequence name, final NumberRange<?> samples, final NumberRange<?> geophysics) {
            ArgumentChecks.ensureNonNull("samples", samples);
            ArgumentChecks.ensureNonNull("geophysics", geophysics);
            /*
             * We need to perform calculation using the same "included versus excluded" characteristic for sample and geophysics
             * values. We pickup the characteristics of the range using floating point values because it is easier to adjust the
             * bounds of the range using integer values (we just add or subtract 1 for integers, while the amount to add to real
             * numbers is not so clear). If both ranges use floating point values, arbitrarily adjust the geophysics values.
             */
            final boolean isMinIncluded, isMaxIncluded;
            if (Numbers.isInteger(samples.getElementType())) {
                isMinIncluded = geophysics.isMinIncluded();                         // This is the usual case.
                isMaxIncluded = geophysics.isMaxIncluded();
            } else {
                isMinIncluded = samples.isMinIncluded();                            // Less common case.
                isMaxIncluded = samples.isMaxIncluded();
            }
            final double minValue  = geophysics.getMinDouble(isMinIncluded);
            final double Δvalue    = geophysics.getMaxDouble(isMaxIncluded) - minValue;
            final double minSample =    samples.getMinDouble(isMinIncluded);
            final double Δsample   =    samples.getMaxDouble(isMaxIncluded) - minSample;
            final double scale     = Δvalue / Δsample;
            final TransferFunction transferFunction = new TransferFunction();
            transferFunction.setScale(scale);
            transferFunction.setOffset(minValue - scale * minSample);               // TODO: use Math.fma with JDK9.
            return addQuantitative(name, samples, transferFunction.getTransform(),
                    (geophysics instanceof MeasurementRange<?>) ? ((MeasurementRange<?>) geophysics).unit() : null);
        }

        /**
         * Adds a quantitative category for sample values ranging from {@code lower} inclusive to {@code upper} exclusive.
         * Sample values are converted into real values using the following linear equation:
         *
         * <blockquote><var>measure</var> = <var>sample</var> × <var>scale</var> + <var>offset</var></blockquote>
         *
         * Results of above conversion are measurements in the units specified by the {@code units} argument.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object.
         * @param  lower   the lower sample value, inclusive.
         * @param  upper   the upper sample value, exclusive.
         * @param  scale   the scale value which is multiplied to sample values for the category. Must be different than zero.
         * @param  offset  the offset value to add to sample values for this category.
         * @param  units   the units of measurement of values after conversion by the offset and scale factor.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if {@code lower} is not smaller than {@code upper},
         *         or if {@code scale} or {@code offset} are not real numbers, or if {@code scale} is zero.
         */
        public Builder addQuantitative(CharSequence name, int lower, int upper, double scale, double offset, Unit<?> units) {
            final TransferFunction transferFunction = new TransferFunction();
            transferFunction.setScale(scale);
            transferFunction.setOffset(offset);
            return addQuantitative(name, NumberRange.create(lower, true, upper, false), transferFunction.getTransform(), units);
        }

        /**
         * Constructs a quantitative category for all samples in the specified range of values.
         * Sample values (usually integers) will be converted into real values
         * (usually floating-point numbers) through the {@code toUnits} transform.
         * Results of that conversion are measurements in the units specified by the {@code units} argument.
         *
         * <p>This is the most generic method for adding a quantitative category.
         * All other {@code addQuantitative(name, …)} methods are convenience methods delegating their work to this method.</p>
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object.
         * @param  samples  the minimum and maximum sample values in the category. Element class is usually
         *                  {@link Integer}, but {@link Float} and {@link Double} types are accepted as well.
         * @param  toUnits  the transfer function from sample values to real values in the specified units.
         * @param  units    the units of measurement of values after conversion by the transfer function.
         * @return {@code this}, for method call chaining.
         * @throws ClassCastException if the range element class is not a {@link Number} subclass.
         * @throws IllegalArgumentException if the range is invalid.
         *
         * @see TransferFunction
         */
        public Builder addQuantitative(CharSequence name, NumberRange<?> samples, MathTransform1D toUnits, Unit<?> units) {
            ArgumentChecks.ensureNonNull("toUnits", toUnits);
            categories.add(new Category(name, samples, toUnits, units, padValues));
            return this;
        }

        /**
         * Creates a new sample with the properties defined to this builder.
         *
         * @return the sample dimension.
         */
        public SampleDimension build() {
            return new SampleDimension(
                    Types.toInternationalString(dimensionName),
                    categories.toArray(new Category[categories.size()]));
        }
    }
}
