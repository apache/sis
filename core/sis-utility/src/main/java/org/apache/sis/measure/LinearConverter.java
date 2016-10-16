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
package org.apache.sis.measure;

import java.util.List;
import java.util.Collections;
import java.io.Serializable;
import java.math.BigDecimal;
import javax.measure.UnitConverter;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.Numerics;


/**
 * Conversions between units that can be represented by a linear operation (scale or offset).
 * Note that the "linear" word in this class does not have the same meaning than the same word
 * in the {@link #isLinear()} method inherited from JSR-363.
 *
 * <p><b>Implementation note:</b>
 * for performance reason we should create specialized subtypes for the case where there is only a scale to apply,
 * or only an offset, <i>etc.</i> But we don't do that in Apache SIS implementation because we will rarely use the
 * {@code UnitConverter} for converting a lot of values. We rather use {@code MathTransform} for operations on
 * <var>n</var>-dimensional tuples, and unit conversions are only a special case of those more generic operations.
 * The {@code sis-referencing} module provided the specialized implementations needed for efficient operations
 * and know how to copy the {@code UnitConverter} coefficients into an affine transform matrix.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class LinearConverter implements UnitConverter, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3759983642723729926L;

    /**
     * The identity linear converter.
     */
    private static final LinearConverter IDENTITY = new LinearConverter(1, 0);

    /**
     * The scale to apply for converting values.
     */
    private final double scale;

    /**
     * The offset to apply after the scale.
     */
    private final double offset;

    /**
     * The inverse of this unit converter. Computed when first needed and stored in
     * order to avoid rounding error if the user asks for the inverse of the inverse.
     */
    private transient LinearConverter inverse;

    /**
     * Creates a new linear converter for the given scale and offset.
     */
    private LinearConverter(final double scale, final double offset) {
        this.scale  = scale;
        this.offset = offset;
    }

    /**
     * Returns a linear converter for the given scale and offset.
     */
    static LinearConverter create(final double scale, final double offset) {
        if (offset == 0) {
            if (scale == 1) return IDENTITY;
        }
        return new LinearConverter(scale, offset);
    }

    /**
     * Indicates if this converter is linear.
     * JSR-363 defines a converter as linear if:
     *
     * <ul>
     *   <li>{@code convert(u + v) == convert(u) + convert(v)}</li>
     *   <li>{@code convert(r * u) == r * convert(u)}</li>
     * </ul>
     *
     * Note that this definition allows scale factors but does not allow offsets.
     * Consequently this is a different definition of "linear" than this class and the rest of Apache SIS.
     */
    @Override
    public boolean isLinear() {
        return offset == 0;
    }

    /**
     * Returns {@code true} if the scale is 1 and the offset is zero.
     */
    @Override
    public boolean isIdentity() {
        return scale == 1 && offset == 0;
    }

    /**
     * Returns the inverse of this unit converter.
     */
    @Override
    public synchronized UnitConverter inverse() {
        if (inverse == null) {
            inverse = new LinearConverter(1/scale, -offset/scale);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * Applies the linear conversion on the given IEEE 754 floating-point value.
     */
    @Override
    public double convert(final double value) {
        return value * scale + offset;
    }

    /**
     * Applies the linear conversion on the given value. This method uses {@link BigDecimal} arithmetic if
     * the given value is an instance of {@code BigDecimal}, or IEEE 754 floating-point arithmetic otherwise.
     *
     * <p>This method is inefficient. Apache SIS rarely uses {@link BigDecimal} arithmetic, so providing an
     * efficient implementation of this method is currently not a goal (this decision may be revisited in a
     * future SIS version if the need for {@code BigDecimal} arithmetic increase).</p>
     */
    @Override
    public Number convert(Number value) {
        if (value instanceof BigDecimal) {
            if (scale != 1) {
                value = ((BigDecimal) value).multiply(BigDecimal.valueOf(scale));
            }
            if (offset != 0) {
                value = ((BigDecimal) value).add(BigDecimal.valueOf(offset));
            }
        } else if (!isIdentity()) {
            value = convert(value.doubleValue());
        }
        return value;
    }

    /**
     * Concatenates this converter with another converter. The resulting converter is equivalent to first converting
     * by the specified converter (right converter), and then converting by this converter (left converter).
     */
    @Override
    public UnitConverter concatenate(final UnitConverter converter) {
        ArgumentChecks.ensureNonNull("converter", converter);
        if (converter.isIdentity()) {
            return this;
        }
        if (isIdentity()) {
            return converter;
        }
        final double otherScale, otherOffset;
        if (converter instanceof LinearConverter) {
            otherScale  = ((LinearConverter) converter).scale;
            otherOffset = ((LinearConverter) converter).offset;
        } else if (converter.isLinear()) {
            /*
             * Fallback for foreigner implementations. Note that 'otherOffset' should be restricted to zero
             * according JSR-363 definition of 'isLinear()', but let be safe; maybe we are not the only one
             * to have a different interpretation about the meaning of "linear".
             */
            otherOffset = converter.convert(0.0);
            otherScale  = converter.convert(1.0) - otherOffset;
        } else {
            return new ConcatenatedConverter(converter, this);
        }
        return create(otherScale * scale, otherOffset * scale + offset);
    }

    /**
     * Returns the steps of fundamental converters making up this converter, which is only {@code this}.
     */
    @Override
    public List<LinearConverter> getConversionSteps() {
        return Collections.singletonList(this);
    }

    /**
     * Returns a hash code value for this unit converter.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode((Double.doubleToLongBits(scale) + 31*Double.doubleToLongBits(offset)) ^ serialVersionUID);
    }

    /**
     * Compares this converter with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof LinearConverter) {
            final LinearConverter o = (LinearConverter) other;
            return Numerics.equals(scale, o.scale) && Numerics.equals(offset, o.offset);
        }
        return false;
    }

    /**
     * Returns a string representation of this converter.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder().append("\uD835\uDC66 = ");
        if (scale != 1) {
            buffer.append(scale).append('⋅');
        }
        buffer.append("\uD835\uDC65");
        if (offset != 0) {
            buffer.append(" + ").append(offset);
        }
        return buffer.toString();
    }
}
