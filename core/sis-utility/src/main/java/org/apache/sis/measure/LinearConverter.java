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
import java.math.BigInteger;
import javax.measure.UnitConverter;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.math.Fraction;
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
    static final LinearConverter IDENTITY = new LinearConverter(1, 0, 1);

    /**
     * The scale to apply for converting values, before division by {@link #divisor}.
     */
    private final double scale;

    /**
     * The offset to apply after the scale, before division by {@link #divisor}.
     */
    private final double offset;

    /**
     * A divisor applied after the conversion.
     * The complete formula used by Apache SIS is {@code y = (x*scale + offset) / divisor}.
     * This division is mathematically unneeded since we could divide the offset and scale factor directly,
     * but we keep it for accuracy reasons because most unit conversion factors are defined in base 10 and
     * IEEE 754 can not represent fractional values in base 10 accurately.
     */
    private final double divisor;

    /**
     * The scale and offset factors represented in base 10, computed when first needed.
     * Those terms are pre-divided by the {@linkplain #divisor}.
     */
    private transient volatile BigDecimal scale10, offset10;

    /**
     * Creates a new linear converter for the given scale and offset.
     * The complete formula applied is {@code y = (x*scale + offset) / divisor}.
     */
    LinearConverter(final double scale, final double offset, final double divisor) {
        this.scale   = scale;
        this.offset  = offset;
        this.divisor = divisor;
    }

    /**
     * Returns a linear converter for the given scale and offset.
     */
    private static LinearConverter create(final double scale, final double offset, final double divisor) {
        if (offset == 0) {
            if (scale == divisor) return IDENTITY;
        }
        return new LinearConverter(scale, offset, divisor);
    }

    /**
     * Returns a linear converter for the given ratio. The scale factor is specified as a ratio because
     * the unit conversion factors are often defined with a value in base 10.  That value is considered
     * exact by definition, but IEEE 754 has no exact representation of decimal fraction digits.
     */
    static LinearConverter scale(final double numerator, final double denominator) {
        return new LinearConverter(numerator, 0, denominator);
    }

    /**
     * Returns a converter for the given shift. The translation is specified as a fraction because the
     * unit conversion terms are often defined with a value in base 10. That value is considered exact
     * by definition, but IEEE 754 has no exact representation of decimal fraction digits.
     */
    static LinearConverter offset(final double numerator, final double denominator) {
        return new LinearConverter(denominator, numerator, denominator);
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
     * {@linkplain #isLinear() is linear} (this is not verified) and takes only the scale factor;
     * the offset (if any) is ignored.
     *
     * @param  converter  the converter to raise to the given power.
     * @param  n          the exponent.
     * @param  root       {@code true} for raising to 1/n instead of n.
     * @return the converter raised to the given power.
     */
    static LinearConverter pow(final UnitConverter converter, final int n, final boolean root) {
        double numerator, denominator;
        if (converter instanceof LinearConverter) {
            final LinearConverter lc = (LinearConverter) converter;
            numerator   = lc.scale;
            denominator = lc.divisor;
        } else {
            // Subtraction by convert(0) is a paranoiac safety.
            numerator   = converter.convert(1.0) - converter.convert(0.0);
            denominator = 1;
        }
        if (root) {
            switch (n) {
                case 1:  break;
                case 2:  numerator   = Math.sqrt(numerator);
                         denominator = Math.sqrt(denominator);
                         break;
                case 3:  numerator   = Math.cbrt(numerator);
                         denominator = Math.cbrt(denominator);
                         break;
                default: final double r = 1.0 / n;
                         numerator   = Math.pow(numerator,   r);
                         denominator = Math.pow(denominator, r);
                         break;
            }
        } else {
            numerator   = (numerator   == 10) ? MathFunctions.pow10(n) : Math.pow(numerator,   n);
            denominator = (denominator == 10) ? MathFunctions.pow10(n) : Math.pow(denominator, n);
        }
        return scale(numerator, denominator);
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
     * Returns {@code true} if the effective scale factor is 1 and the offset is zero.
     */
    @Override
    public boolean isIdentity() {
        return scale == divisor && offset == 0;
    }

    /**
     * Returns the inverse of this unit converter.
     * Given that the formula applied by this converter is:
     *
     * {@preformat math
     *    y = (x⋅scale + offset) ∕ divisor
     * }
     *
     * the inverse formula is:
     *
     * {@preformat math
     *    x = (y⋅divisor - offset) ∕ scale
     * }
     */
    @Override
    public synchronized UnitConverter inverse() {
        return isIdentity() ? this : new LinearConverter(divisor, -offset, scale);
    }

    /**
     * Returns the coefficient of this linear conversion.
     */
    @Override
    @SuppressWarnings("fallthrough")
    Number[] coefficients() {
        final Number[] c = new Number[(scale != divisor) ? 2 : (offset != 0) ? 1 : 0];
        switch (c.length) {
            case 2: c[1] = ratio(scale,  divisor);
            case 1: c[0] = ratio(offset, divisor);
            case 0: break;
        }
        return c;
    }

    /**
     * Returns the given ratio as a {@link Fraction} if possible, or as a {@link Double} otherwise.
     * The use of {@link Fraction} allows the {@link org.apache.sis.referencing.operation.matrix}
     * package to perform more accurate calculations.
     */
    private static Number ratio(final double value, final double divisor) {
        final int numerator = (int) value;
        if (numerator == value) {
            final int denominator = (int) divisor;
            if (denominator == divisor) {
                return (denominator == 1) ? numerator : new Fraction(numerator, denominator);
            }
        }
        return value / divisor;
    }

    /**
     * Applies the linear conversion on the given IEEE 754 floating-point value.
     */
    @Override
    public double convert(final double value) {
        // TODO: use JDK9' Math.fma(…) and verify if it solve the accuracy issue in LinearConverterTest.inverse().
        return (value * scale + offset) / divisor;
    }

    /**
     * Applies the linear conversion on the given value. This method uses {@link BigDecimal} arithmetic if
     * the given value is an instance of {@code BigDecimal}, or IEEE 754 floating-point arithmetic otherwise.
     *
     * <p>Apache SIS rarely uses {@link BigDecimal} arithmetic, so providing an efficient implementation of
     * this method is not a goal.</p>
     */
    @Override
    public Number convert(Number value) {
        ArgumentChecks.ensureNonNull("value", value);
        if (!isIdentity()) {
            if (value instanceof BigInteger) {
                value = new BigDecimal((BigInteger) value);
            }
            if (value instanceof BigDecimal) {
                BigDecimal scale10  = this.scale10;
                BigDecimal offset10 = this.offset10;
                if (scale10 == null || offset10 == null) {
                    final BigDecimal divisor = BigDecimal.valueOf(this.divisor);
                    scale10  = BigDecimal.valueOf(scale) .divide(divisor);
                    offset10 = BigDecimal.valueOf(offset).divide(divisor);
                    this.scale10  = scale10;            // Volatile fields
                    this.offset10 = offset10;
                }
                value = ((BigDecimal) value).multiply(scale10).add(offset10);
            } else {
                final double x;
                if (value instanceof Float) {
                    // Because unit conversion factors are usually defined in base 10.
                    x = DecimalFunctions.floatToDouble((Float) value);
                } else {
                    x = value.doubleValue();
                }
                value = convert(x);
            }
        }
        return value;
    }

    /**
     * Returns the derivative of the conversion at the given value.
     * For a linear converter, the derivative is the same everywhere.
     */
    @Override
    public double derivative(double value) {
        return scale / divisor;
    }

    /**
     * Concatenates this converter with another converter. The resulting converter is equivalent to first converting
     * by the specified converter (right converter), and then converting by this converter (left converter).  In the
     * following equations, the 1 subscript is for the specified converter and the 2 subscript is for this converter:
     *
     * {@preformat math
     *    t = (x⋅scale₁ + offset₁) ∕ divisor₁
     *    y = (t⋅scale₂ + offset₂) ∕ divisor₂
     * }
     *
     * We rewrite as:
     *
     * {@preformat math
     *    y = (x⋅scale₁⋅scale₂ + offset₁⋅scale₂ + divisor₁⋅offset₂) ∕ (divisor₁⋅divisor₂)
     * }
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
        double otherScale, otherOffset, otherDivisor;
        if (converter instanceof LinearConverter) {
            final LinearConverter lc = (LinearConverter) converter;
            otherScale   = lc.scale;
            otherOffset  = lc.offset;
            otherDivisor = lc.divisor;
        } else if (converter.isLinear()) {
            /*
             * Fallback for foreigner implementations. Note that 'otherOffset' should be restricted to zero
             * according JSR-363 definition of 'isLinear()', but let be safe; maybe we are not the only one
             * to have a different interpretation about the meaning of "linear".
             */
            otherOffset  = converter.convert(0.0);
            otherScale   = converter.convert(1.0) - otherOffset;
            otherDivisor = 1;
        } else {
            return new ConcatenatedConverter(converter, this);
        }
        otherScale   *= scale;
        otherOffset   = otherOffset * scale + otherDivisor * offset;
        otherDivisor *= divisor;
        /*
         * Following loop is a little bit similar to simplifying a fraction, but checking only for the
         * powers of 10 since unit conversions are often such values. Algorithm is not very efficient,
         * but the loop should not be executed often.
         */
        if (otherScale != 0 || otherOffset != 0 || otherDivisor != 0) {
            double cf, f = 1;
            do {
                cf = f;
                f *= 10;
            } while (otherScale % f == 0 && otherOffset % f == 0 && otherDivisor % f == 0);
            otherScale   /= cf;
            otherOffset  /= cf;
            otherDivisor /= cf;
        }
        return create(otherScale, otherOffset, otherDivisor);
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
        return Numerics.hashCode(Double.doubleToLongBits(scale)
                         + 31 * (Double.doubleToLongBits(offset)
                         + 37 *  Double.doubleToLongBits(divisor)));
    }

    /**
     * Compares this converter with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof LinearConverter) {
            final LinearConverter o = (LinearConverter) other;
            return Numerics.equals(scale,   o.scale)  &&
                   Numerics.equals(offset,  o.offset) &&
                   Numerics.equals(divisor, o.divisor);
        }
        return false;
    }

    /**
     * Returns {@code true} if the given converter perform the same conversion than this converter,
     * except for rounding errors.
     */
    boolean equivalent(final LinearConverter other) {
        return AbstractUnit.epsilonEquals(scale  * other.divisor, other.scale  * divisor) &&
               AbstractUnit.epsilonEquals(offset * other.divisor, other.offset * divisor);
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
        if (offset != 0) {
            buffer.append('(');
        }
        if (scale != 1) {
            StringBuilders.trimFractionalPart(buffer.append(scale));
            buffer.append('⋅');
        }
        buffer.append('x');
        if (offset != 0) {
            StringBuilders.trimFractionalPart(buffer.append(" + ").append(offset));
            buffer.append(')');
        }
        if (divisor != 1) {
            StringBuilders.trimFractionalPart(buffer.append('∕').append(divisor));
        }
        return buffer.toString();
    }
}
