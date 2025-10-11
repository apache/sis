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

import java.util.Objects;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.measure.UnitConverter;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.DoubleDouble;


/**
 * Conversions between units that can be represented by a linear operation (scale or offset).
 * Note that the "linear" word in this class does not have the same meaning as the same word
 * in the {@link #isLinear()} method inherited from JSR-385.
 *
 * <h2>Implementation note</h2>
 * for performance reason we should create specialized subtypes for the case where there is only a scale to apply,
 * or only an offset, <i>etc.</i> But we don't do that in Apache SIS implementation because we will rarely use the
 * {@code UnitConverter} for converting a lot of values. We rather use {@code MathTransform} for operations on
 * <var>n</var>-dimensional tuples, and unit conversions are only a special case of those more generic operations.
 * The {@code org.apache.sis.referencing} module provided the specialized implementations needed for efficient
 * operations and know how to copy the {@code UnitConverter} coefficients into an affine transform matrix.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class LinearConverter extends AbstractConverter implements LenientComparable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3759983642723729926L;

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
     * IEEE 754 cannot represent fractional values in base 10 accurately.
     */
    private final double divisor;

    /**
     * The scale and offset factors represented in base 10, computed when first needed.
     * Those terms are pre-divided by the {@linkplain #divisor}.
     */
    private transient volatile BigDecimal scale10, offset10;

    /**
     * The inverse of this unit converter. Computed when first needed.
     */
    private transient volatile LinearConverter inverse;

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
     * Creates a linear converter from the given scale and offset, which may be {@link BigDecimal} instances.
     * This is the implementation of public {@link Units#converter(Number, Number)} method.
     */
    static AbstractConverter create(final Number scale, final Number offset) {
        final double numerator, divisor;
        double shift = (offset != null) ? doubleValue(offset) : 0;
        if (scale instanceof Fraction) {
            numerator = ((Fraction) scale).numerator;
            divisor   = ((Fraction) scale).denominator;
            shift    *= divisor;
        } else {
            numerator = (scale != null) ? doubleValue(scale) : 1;
            divisor   = 1;
        }
        if (shift == 0 && numerator == divisor) {
            return IdentityConverter.INSTANCE;
        }
        final LinearConverter c = new LinearConverter(numerator, shift, divisor);
        if (scale  instanceof BigDecimal) c.scale10  = (BigDecimal) scale;
        if (offset instanceof BigDecimal) c.offset10 = (BigDecimal) offset;
        return c;
    }

    /**
     * Returns a linear converter for the given ratio. The scale factor is specified as a ratio because
     * the unit conversion factors are often defined with a value in base 10.  That value is considered
     * exact by definition, but IEEE 754 has no exact representation of decimal fraction digits.
     *
     * <p>It is caller's responsibility to skip this method call when {@code numerator} = {@code denominator}.
     * This method does not perform this check because it is usually already done (indirectly) by the caller.</p>
     */
    static LinearConverter scale(final double numerator, final double denominator) {
        return new LinearConverter(numerator, 0, denominator);
    }

    /**
     * Returns a converter for the given shift. The translation is specified as a fraction because the
     * unit conversion terms are often defined with a value in base 10. That value is considered exact
     * by definition, but IEEE 754 has no exact representation of decimal fraction digits.
     *
     * <p>It is caller's responsibility to skip this method call when {@code numerator} = 0.
     * This method does not perform this check because it is usually already done by the caller.</p>
     */
    static LinearConverter offset(final double numerator, final double denominator) {
        return new LinearConverter(denominator, numerator, denominator);
    }

    /**
     * Raises the given converter to the given power. This method assumes that the given converter
     * {@linkplain #isLinear() is linear} (this is not verified) and takes only the scale factor;
     * the offset (if any) is ignored.
     *
     * <p>It is caller's responsibility to skip this method call when {@code n} = 1.
     * This method does not perform this check because it is usually already done (indirectly) by the caller.</p>
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
            numerator   = converter.convert(1d) - converter.convert(0d);
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
     * JSR-385 defines a converter as linear if:
     *
     * <ul>
     *   <li>{@code convert(u + v) == convert(u) + convert(v)}</li>
     *   <li>{@code convert(r * u) == r * convert(u)}</li>
     * </ul>
     *
     * Note that this definition allows scale factors but does not allow offsets.
     * Consequently, this is a different definition of "linear" than this class and the rest of Apache SIS.
     *
     * @deprecated This method is badly named, but we can't change since it is defined by JSR-385.
     */
    @Override
    @Deprecated
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
     * Returns {@code true} if this converter is close to an identity converter.
     */
    final boolean almostIdentity() {
        return epsilonEquals(scale, divisor) && epsilonEquals(offset, 0);
    }

    /**
     * Returns the inverse of this unit converter.
     * Given that the formula applied by this converter is:
     *
     * <pre class="math">
     *    y = (x⋅scale + offset) ∕ divisor</pre>
     *
     * the inverse formula is:
     *
     * <pre class="math">
     *    x = (y⋅divisor - offset) ∕ scale</pre>
     */
    @Override
    public synchronized UnitConverter inverse() {
        if (inverse == null) {
            inverse = isIdentity() ? this : new LinearConverter(divisor, -offset, scale);
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * Returns the coefficient of this linear conversion.
     * Coefficients are offset and scale, in that order.
     */
    @Override
    @SuppressWarnings("fallthrough")
    final Number[] coefficients() {
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
        if (value != 0) {
            final int numerator = (int) value;
            if (numerator == value) {
                final int denominator = (int) divisor;
                if (denominator == divisor) {
                    return (denominator == 1) ? numerator : new Fraction(numerator, denominator);
                }
            }
        }
        return value / divisor;
    }

    /**
     * Applies the linear conversion on the given IEEE 754 floating-point value.
     */
    @Override
    public double convert(final double value) {
        return Math.fma(value, scale, offset) / divisor;
    }

    /**
     * Applies the linear conversion on the given value. This method uses {@link BigDecimal} arithmetic if
     * the given value is an instance of {@code BigDecimal}, or double-double arithmetic if the value is an
     * instance of {@link DoubleDouble}, or IEEE 754 floating-point arithmetic otherwise.
     */
    @Override
    public Number convert(Number value) {
        Objects.requireNonNull(value);
        if (!isIdentity()) {
            if (value instanceof DoubleDouble) {
                var dd = (DoubleDouble) value;
                return dd.multiply(scale, true).add(offset, true).divide(divisor, true);
            }
            if (value instanceof BigInteger) {
                value = new BigDecimal((BigInteger) value);
            }
            if (value instanceof BigDecimal) {
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                BigDecimal scale10  = this.scale10,
                           offset10 = this.offset10;
                if (scale10 == null || offset10 == null) {
                    @SuppressWarnings("LocalVariableHidesMemberVariable")
                    final BigDecimal divisor = BigDecimal.valueOf(this.divisor);
                    scale10  = BigDecimal.valueOf(scale) .divide(divisor);
                    offset10 = BigDecimal.valueOf(offset).divide(divisor);
                    this.scale10  = scale10;            // Volatile fields
                    this.offset10 = offset10;
                }
                value = ((BigDecimal) value).multiply(scale10).add(offset10);
            } else {
                value = convert(doubleValue(value));
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
     * <pre class="math">
     *    t = (x⋅scale₁ + offset₁) ∕ divisor₁
     *    y = (t⋅scale₂ + offset₂) ∕ divisor₂</pre>
     *
     * We rewrite as:
     *
     * <pre class="math">
     *    y = (x⋅scale₁⋅scale₂ + offset₁⋅scale₂ + divisor₁⋅offset₂) ∕ (divisor₁⋅divisor₂)</pre>
     */
    @Override
    public UnitConverter concatenate(final UnitConverter converter) {
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
             * Fallback for foreigner implementations. Note that `otherOffset` should be restricted to zero
             * according JSR-385 definition of `isLinear()`, but let be safe; maybe we are not the only one
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
        if (otherOffset == 0 && otherScale == otherDivisor) {
            return IdentityConverter.INSTANCE;
        }
        return new LinearConverter(otherScale, otherOffset, otherDivisor);
    }

    /**
     * Returns a hash code value for this unit converter.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(scale)
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
        if (other instanceof IdentityConverter) {
            return isIdentity();
        }
        return false;
    }

    /**
     * Compares this converter with the given object for equality, optionally ignoring rounding errors.
     */
    @Override
    public boolean equals(final Object other, final ComparisonMode mode) {
        if (mode.isApproximate()) {
            if (other instanceof LinearConverter) {
                return equivalent((LinearConverter) other);
            } else if (other instanceof IdentityConverter) {
                return almostIdentity();
            } else {
                return false;
            }
        } else {
            return equals(other);
        }
    }

    /**
     * Returns {@code true} if the given converter perform the same conversion as this converter,
     * except for rounding errors.
     */
    final boolean equivalent(final LinearConverter other) {
        return epsilonEquals(scale  * other.divisor, other.scale  * divisor) &&
               epsilonEquals(offset * other.divisor, other.offset * divisor);
    }

    /**
     * Returns a string representation of this converter for debugging purpose.
     * This string representation may change in any future SIS release.
     * Current format is of the form "y = scale⋅x + offset".
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder().append("y = ");
        if (offset != 0) {
            buffer.append('(');
        }
        if (scale != 1) {
            StringBuilders.trimFractionalPart(buffer.append(scale));
            buffer.append(AbstractUnit.MULTIPLY);
        }
        buffer.append('x');
        if (offset != 0) {
            StringBuilders.trimFractionalPart(buffer.append(" + ").append(offset));
            buffer.append(')');
        }
        if (divisor != 1) {
            StringBuilders.trimFractionalPart(buffer.append(AbstractUnit.DIVIDE).append(divisor));
        }
        return buffer.toString();
    }
}
