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

import java.io.ObjectStreamException;
import javax.measure.UnitConverter;
import org.apache.sis.math.MathFunctions;


/**
 * Conversions from units represented by a logarithm in base 10.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class PowerOf10 extends AbstractConverter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7960860506196831772L;

    /**
     * Value of {@code Math.log(10)}.
     */
    private static final double LN_10 = 2.302585092994046;

    /**
     * The singleton instance. Can be used for conversion from neper units to {@link Units#UNITY}.
     */
    private static final UnitConverter INSTANCE = new PowerOf10();

    /**
     * Returns the converter from bel unit (B) to dimensionless unit. ISO 80000-3:2006 defines
     * 1 B = ln(10)/2 Np (neper) and 1 Np = 1 (dimensionless), keeping in mind that neper is a
     * logarithmic scale using the natural logarithm. The ln(10) factor is for converting from
     * base ℯ to base 10.
     *
     * <p>The method of expressing a ratio as a level in decibels depends on whether the measured property
     * is a power quantity or a root-power quantity (amplitude of a field). This is because power is often
     * proportional to the square of the amplitude. The /2 in above equation is for taking the square root
     * of a power, since B is used for power and Np is used for root-power.</p>
     *
     * <p>The bel represents the logarithm of a ratio between two power quantities of 10:1.
     * Two signals whose levels differ by <var>x</var> bels have a power ratio of 10^x and
     * an amplitude (field quantity) ratio of 10^(x/2).</p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/Decibel#Definition">Decibel on Wikipedia</a>
     */
    static UnitConverter belToOne() {
        /*
         * We do not put LN_10 in numerator because convert(x) uses log10(x) instead of ln(x),
         * do the multiplication by ln(10) will be implicitly done.
         */
        return new ConcatenatedConverter(LinearConverter.scale(1, 2), INSTANCE);
    }

    /**
     * Creates the singleton instance.
     */
    private PowerOf10() {
    }

    /**
     * Returns the singleton instance on deserialization.
     */
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

    /**
     * Returns the inverse of this converter.
     */
    @Override public UnitConverter inverse() {
        return Logarithm.INSTANCE;
    }

    /**
     * Applies the unit conversion on the given value.
     */
    @Override
    public double convert(final double value) {
        return MathFunctions.pow10(value);
    }

    /**
     * Returns the derivative of this conversion at the given value.
     */
    @Override
    public double derivative(final double value) {
        return LN_10 * MathFunctions.pow10(value);
    }

    /**
     * Compares this converter with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof PowerOf10);
    }

    /**
     * Returns a hash code value for this unit converter.
     */
    @Override
    public int hashCode() {
        return (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this converter for debugging purpose.
     */
    @Override
    public String toString() {
        return "y = 10^x";
    }

    /**
     * Inverse of {@link PowerOf10}.
     */
    private static final class Logarithm extends AbstractConverter {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7089883299592861677L;

        /**
         * The singleton instance.
         */
        static final UnitConverter INSTANCE = new Logarithm();

        /**
         * Creates the singleton instance.
         */
        private Logarithm() {
        }

        /**
         * Returns the singleton instance on deserialization.
         */
        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }

        /**
         * Returns the inverse of this converter.
         */
        @Override
        public UnitConverter inverse() {
            return PowerOf10.INSTANCE;
        }

        /**
         * Applies the unit conversion on the given value.
         */
        @Override
        public double convert(final double value) {
            return Math.log10(value);
        }

        /**
         * Returns the derivative of this conversion at the given value.
         */
        @Override
        public double derivative(final double value) {
            return 1 / (value * LN_10);
        }

        /**
         * Returns a hash code value for this unit converter.
         */
        @Override
        public int hashCode() {
            return (int) serialVersionUID;
        }

        /**
         * Compares this converter with the given object for equality.
         */
        @Override
        public boolean equals(final Object other) {
            return (other instanceof Logarithm);
        }

        /**
         * Returns a string representation of this converter for debugging purpose.
         */
        @Override
        public String toString() {
            return "y = log10(x)";
        }
    }
}
