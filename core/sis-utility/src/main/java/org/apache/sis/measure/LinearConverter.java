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
import java.util.Arrays;
import java.util.Collections;
import java.math.BigDecimal;
import javax.measure.UnitConverter;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
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
final class LinearConverter extends AbstractConverter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3759983642723729926L;

    /**
     * The SI prefixes in increasing order. The only two-letters prefix – “da” – is encoded using the JCK compatibility
     * character “㍲”. The Greek letter μ is repeated twice: the U+00B5 character for micro sign (this is the character
     * that Apache SIS uses in unit symbols) and the U+03BC character for the Greek small letter “mu” (the later is the
     * character that appears when decomposing JCK compatibility characters with {@link java.text.Normalizer}).
     * Both characters have same appearance but different values.
     *
     * <p>For each prefix at index <var>i</var>, the multiplication factor is given by 10 raised to power {@code POWERS[i]}.</p>
     */
    private static final char[] PREFIXES = {'E','G','M','P','T','Y','Z','a','c','d','f','h','k','m','n','p','y','z','µ','μ','㍲'};
    private static final byte[] POWERS   = {18,  9,  6, 15, 12, 24, 21,-18, -2, -1,-15,  2,  3, -3, -9,-12,-24,-21, -6, -6,  1};

    /**
     * The converters for SI prefixes, created when first needed.
     *
     * @see #forPrefix(char)
     */
    private static final LinearConverter[] SI = new LinearConverter[POWERS.length];

    /**
     * The identity linear converter.
     */
    static final LinearConverter IDENTITY = new LinearConverter(1, 0);

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
     * Returns a linear converter for the given ratio. The scale factor is specified as a ratio because
     * the unit conversion factors are defined with a value which is exact in base 10.
     *
     * @todo modify the {@code LinearConverter} implementation for storing the ratio.
     */
    static LinearConverter scale(final double numerator, final double denominator) {
        return new LinearConverter(numerator / denominator, 0);
    }

    /**
     * Returns the converter for the given SI prefix, or {@code null} if none.
     * Those converters are created when first needed and cached for reuse.
     */
    static LinearConverter forPrefix(final char prefix) {
        final int i = Arrays.binarySearch(PREFIXES, prefix);
        if (i < 0) {
            return null;
        }
        synchronized (SI) {
            LinearConverter c = SI[i];
            if (c == null) {
                final int p = POWERS[i];
                final double numerator, denominator;
                if (p >= 0) {
                    numerator = MathFunctions.pow10(p);
                    denominator = 1;
                } else {
                    numerator = 1;
                    denominator = MathFunctions.pow10(-p);
                }
                c = scale(numerator, denominator);
                SI[i] = c;
            }
            return c;
        }
    }

    /**
     * Raises the given converter to the given power. This method assumes that the given converter
     * {@linkplain #isLinear() is linear} (this is not verified) and take only the scale factor;
     * the offset (if any) is ignored.
     *
     * @param  converter  the converter to raise to the given power.
     * @param  n          the exponent.
     * @param  root       {@code true} for raising to 1/n instead of n.
     * @return the converter raised to the given power.
     */
    static LinearConverter pow(final UnitConverter converter, final int n, final boolean root) {
        double scale = converter.convert(1.0) - converter.convert(0.0);
        if (root) {
            switch (n) {
                case 2:  scale = Math.sqrt(scale); break;
                case 3:  scale = Math.cbrt(scale); break;
                default: scale = Math.pow(scale, 1.0 / n); break;
            }
        } else if (scale == 10) {
            scale = MathFunctions.pow10(n);
        } else {
            scale = Math.pow(scale, n);
        }
        return create(scale, 0);
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
     * Returns the coefficient of this linear conversion.
     */
    @Override
    @SuppressWarnings("fallthrough")
    Number[] coefficients() {
        final Number[] c = new Number[(scale != 1) ? 2 : (offset != 0) ? 1 : 0];
        switch (c.length) {
            case 2: c[1] = scale;
            case 1: c[0] = offset;
            case 0: break;
        }
        return c;
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
        ArgumentChecks.ensureNonNull("value", value);
        if (value instanceof BigDecimal) {
            if (scale != 1) {
                value = ((BigDecimal) value).multiply(BigDecimal.valueOf(scale));
            }
            if (offset != 0) {
                value = ((BigDecimal) value).add(BigDecimal.valueOf(offset));
            }
        } else if (!isIdentity()) {
            final double x;
            if (value instanceof Float) {
                // Because unit conversion factors are usually defined in base 10.
                x = DecimalFunctions.floatToDouble((Float) value);
            } else {
                x = value.doubleValue();
            }
            value = convert(x);
        }
        return value;
    }

    /**
     * Returns the derivative of the conversion at the given value.
     * For a linear converter, the derivative is the same everywhere.
     */
    @Override
    public double derivative(double value) {
        return scale;
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
     * Returns a string representation of this converter for debugging purpose.
     * This string representation may change in any future SIS release.
     * Current format is of the form "y = scale⋅x + offset".
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder().append("y = ");
        if (scale != 1) {
            buffer.append(scale).append('⋅');
        }
        buffer.append('x');
        if (offset != 0) {
            buffer.append(" + ").append(offset);
        }
        return buffer.toString();
    }
}
