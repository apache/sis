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

import java.util.Optional;
import javax.measure.Unit;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;


/**
 * A category of "real values" range. Sample values in this category are equal to real values.
 * By definition, the {@link #getTransferFunction()} method for this class returns the identity transform,
 * or an empty optional if this category is a qualitative one.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class ConvertedCategory extends Category {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 336103757882427857L;

    /**
     * Creates a category storing the inverse of the "sample to real values" transfer function. The {@link #toConverse}
     * of this category will convert real value in specified {@code units} to the sample (packed) value.
     *
     * @param  original        the category storing the conversion from sample to real value.
     * @param  toSamples       the "real to sample values" conversion, as the inverse of {@code original.transferFunction}.
     *                         For qualitative category, this function is a constant mapping NaN to the original sample value.
     * @param  isQuantitative  {@code true} if we are construction a quantitative category, or {@code false} for qualitative.
     * @param  units           the units of measurement, or {@code null} if not applicable.
     *                         This is the source units before conversion by {@code toSamples}.
     */
    ConvertedCategory(final Category original, final MathTransform1D toSamples, final boolean isQuantitative, final Unit<?> units)
            throws TransformException
    {
        super(original, toSamples, isQuantitative, units);
    }

    /**
     * Returns {@code this} since the values represented by {@code ConvertedCategory} are already converted.
     */
    @Override
    Category converted() {
        return this;
    }

    /**
     * Returns a category that describes real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively.
     *
     * @param  converted  {@code true} for a category describing converted values,
     *                    or {@code false} for a category describing packed values.
     */
    @Override
    public Category forConvertedValues(final boolean converted) {
        return converted ? this : converse;
    }

    /**
     * Returns the <cite>transfer function</cite> from sample values to real values in units of measurement.
     * The function is absent if this category is not a {@linkplain #isQuantitative() quantitative} category.
     */
    @Override
    public Optional<MathTransform1D> getTransferFunction() {
        return isConvertedQualitative() ? Optional.empty() : Optional.of(identity());
    }
}
