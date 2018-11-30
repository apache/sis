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
import java.util.Optional;
import javax.measure.Unit;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Classes;


/**
 * Describes the data values in a coverage (the range) when those values are numbers.
 * For a grid coverage or a raster, a sample dimension may be a band.
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
public class SampleDimension {
    /**
     * Description for this sample dimension. Typically used as a way to perform a band select by
     * using human comprehensible descriptions instead of just numbers. Web Coverage Service (WCS)
     * can use this name in order to perform band sub-setting as directed from a user request.
     */
    private final InternationalString name;

    /**
     * The range of sample values.
     * May be {@code null} if this sample dimension has no non-{@code NaN} value.
     */
    private final NumberRange<?> range;

    /**
     * The values to indicate "no data" for this sample dimension.
     */
    private final Number[] noDataValues;

    /**
     * The transform from sample to geophysics value. May be {@code null} if this sample dimension
     * do not defines any transform (which is not the same that defining an identity transform).
     */
    private final MathTransform1D transferFunction;

    /**
     * The units of measurement for this sample dimension, or {@code null} if not applicable.
     */
    private final Unit<?> units;

    /**
     * Creates a sample dimension with the specified properties.
     *
     * @param name     the sample dimension title or description.
     * @param nodata   the values to indicate "no data".
     * @param range    the range of sample values.
     * @param toUnit   the transfer function for converting sample values to geophysics values in {@code units}.
     * @param units    the units of measurement for this sample dimension, or {@code null} if not applicable.
     */
    SampleDimension(final InternationalString name,
                    final NumberRange<?>      range,
                    final Number[]            nodata,
                    final MathTransform1D     toUnit,
                    final Unit<?>             units)
    {
        this.name             = name;
        this.range            = range;
        this.noDataValues     = nodata;
        this.transferFunction = toUnit;
        this.units            = units;
    }

    /**
     * Returns a name or description for this sample dimension. This is typically used as a way to perform a band select
     * by using human comprehensible descriptions instead of just numbers. Web Coverage Service (WCS) can use this name
     * in order to perform band sub-setting as directed from a user request.
     *
     * @return The title or description of this sample dimension.
     */
    public InternationalString getName() {
        return name;
    }

    /**
     * Returns the values to indicate "no data" for this sample dimension.
     *
     * @return the values to indicate no data values for this sample dimension, or an empty array if none.
     */
    public Number[] getNoDataValues() {
        return noDataValues.clone();
    }

    /**
     * Returns the range of values in this sample dimension.
     * May be absent if this sample dimension has no non-{@code NaN} value.
     *
     * @return the range of values.
     */
    public Optional<NumberRange<?>> getRange() {
        return Optional.ofNullable(range);
    }

    /**
     * Returns the <cite>transfer function</cite> from sample values to geophysics values.
     * This method returns a transform expecting sample values as input and computing geophysics values as output.
     * This transform will take care of converting all "{@linkplain #getNoDataValues() no data values}" into {@code NaN} values.
     * The <code>transferFunction.{@linkplain MathTransform1D#inverse() inverse()}</code> transform is capable to differentiate
     * {@code NaN} values to get back the original sample value.
     *
     * @return The <cite>transfer function</cite> from sample to geophysics values. May be absent if this sample dimension
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
     * A mutable builder for a {@link SampleDimension}.
     * After properties have been set, the sample dimension is created by {@link #build()}.
     *
     * @param <T> the type of values in the sample dimension.
     */
    public class Builder<T extends Number & Comparable<? super T>> {
        /**
         * Description for this sample dimension.
         */
        private CharSequence name;

        /**
         * The range of sample values.
         * May be {@code null} if this sample dimension has no non-{@code NaN} value.
         */
        private NumberRange<?> range;

        /**
         * The values to indicate "no data" for this sample dimension.
         */
        private final List<T> noDataValues = new ArrayList<>();

        /**
         * Builder for the math transform from sample to geophysics values.
         */
        private final TransferFunction transferFunction = new TransferFunction();

        /**
         * The units of measurement for this sample dimension, or {@code null} if not applicable.
         */
        private Unit<?> units;

        /**
         * Creates a new, initially empty, builder.
         */
        public Builder() {
        }

        /**
         * Sets the name or description of the sample dimension.
         * This is the value to be returned by {@link SampleDimension#getName()}.
         *
         * @param  name the name or description of the sample dimension.
         * @return {@code this}, for method call chaining.
         */
        public Builder<T> setName(final CharSequence name) {
            this.name = name;
            return this;
        }

        /**
         * Creates a new sample with the properties defined to this builder.
         *
         * @return the sample dimension.
         */
        public SampleDimension build() {
            return new SampleDimension(
                    Types.toInternationalString(name), range,
                    noDataValues.toArray(new Number[noDataValues.size()]),
                    transferFunction.getTransform(), units);
        }
    }
}
