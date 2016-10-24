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

import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectStreamException;
import javax.measure.Dimension;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.internal.converter.FractionConverter;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * Dimension (length, mass, time, <i>etc.</i>) of a unit of measurement.
 * Only two kind of dimensions are defined in Apache SIS:
 *
 * <ul>
 *   <li>Base dimensions are the 7 base dimensions specified by the SI system.</li>
 *   <li>Derived dimensions are products of base dimensions raised to some power.</li>
 * </ul>
 *
 * The powers should be integers, but this implementation nevertheless accepts fractional power of dimensions.
 * While quantities with dimension such as √M makes no sense physically, on a pragmatic point of view it is easier
 * to write programs in which such units appear in intermediate calculations but become integers in the final result.
 * Furthermore, some dimensions with fractional power actually exist. Examples:
 *
 * <ul>
 *   <li>Voltage noise density measured per √(Hz).</li>
 *   <li><a href="http://en.wikipedia.org/wiki/Specific_detectivity">Specific detectivity</a>
 *       as T^2.5 / (M⋅L) dimension.</li>
 * </ul>
 *
 * All {@code UnitDimension} instances are immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class UnitDimension implements Dimension, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2568769237612674235L;

    /**
     * Pseudo-dimension for dimensionless units.
     */
    static final UnitDimension NONE = new UnitDimension(Collections.emptyMap());
    // No need to store in UnitRegistry since UnitDimension performs special checks for dimensionless instances.

    /**
     * The product of base dimensions that make this dimension. All keys in this map shall be base dimensions
     * (base dimensions are identified by non-zero {@link #symbol}). If this {@code UnitDimension} is itself a
     * base dimension, then the map contains {@code this} raised to power 1. The map shall never be {@code null}.
     *
     * @see #getBaseDimensions()
     */
    final Map<UnitDimension,Fraction> components;

    /**
     * If this {@code UnitDimension} is a base dimension, its symbol (not to be confused with unit symbol).
     * Otherwise (i.e. if this {@code UnitDimension} is a derived dimension), zero.
     */
    final char symbol;

    /**
     * Creates a new base dimension with the given symbol, which shall not be zero.
     * This constructor shall be invoked only during construction of {@link Units} constants.
     *
     * @param  symbol  the symbol of this base dimension (not to be confused with unit symbol).
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")    // Safe because this class is final.
    UnitDimension(final char symbol) {
        this.symbol = symbol;
        components  = Collections.singletonMap(this, new Fraction(1,1).unique());
        UnitRegistry.init(components, this);
    }

    /**
     * Creates a new derived dimension. This constructor shall never be invoked directly
     * (except for {@link #NONE}); use {@link #create(Map)} instead.
     *
     * @param  components  the product of base dimensions together with their power.
     */
    private UnitDimension(final Map<UnitDimension,Fraction> components) {
        this.components = components;
        this.symbol     = 0;
    }

    /**
     * Creates a new derived dimension from the given product of base dimensions with their power.
     * This method returns a shared instance if possible.
     *
     * @param  components  the product of base dimensions together with their power.
     */
    private static UnitDimension create(Map<UnitDimension,Fraction> components) {
        switch (components.size()) {
            case 0: return NONE;
            case 1: {
                final Map.Entry<UnitDimension,Fraction> entry = components.entrySet().iterator().next();
                final Fraction power = entry.getValue();
                if (power.numerator == 1 && power.denominator == 1) {
                    return entry.getKey();
                }
                break;
            }
        }
        /*
         * Implementation note: following code duplicates the functionality of Map.computeIfAbsent(…),
         * but we had to do it because we compute not only the value, but also the 'components' key.
         */
        UnitDimension dim = (UnitDimension) UnitRegistry.get(components);
        if (dim == null) {
            components.replaceAll((c, power) -> power.unique());
            components = CollectionsExt.unmodifiableOrCopy(components);
            dim = new UnitDimension(components);
            if (!Units.initialized) {
                UnitRegistry.init(components, dim);
            } else {
                final UnitDimension c = (UnitDimension) UnitRegistry.putIfAbsent(components, dim);
                if (c != null) {
                    return c;       // UnitDimension created concurrently in another thread.
                }
            }
        }
        return dim;
    }

    /**
     * Invoked on deserialization for returning a unique instance of {@code UnitDimension}.
     */
    Object readResolve() throws ObjectStreamException {
        if (isDimensionless()) {
            return NONE;
        }
        if (Units.initialized) {        // Force Units class initialization.
            final UnitDimension dim = (UnitDimension) UnitRegistry.putIfAbsent(components, this);
            if (dim != null) {
                return dim;
            }
        }
        return this;
    }

    /**
     * Returns {@code true} if this {@code UnitDimension} has no components.
     * Many dimensionless units exist for different quantities as angles, parts per million, <i>etc.</i>
     */
    final boolean isDimensionless() {
        return components.isEmpty();
    }

    /**
     * Returns the (fundamental) base dimensions and their exponent whose product is this dimension,
     * or null if this dimension is a base dimension.
     */
    @Override
    public Map<UnitDimension,Integer> getBaseDimensions() {
        if (symbol != 0) {
            return null;
        }
        return ObjectConverters.derivedValues(components, UnitDimension.class, FractionConverter.INSTANCE);
    }

    /**
     * Returns the base dimensions and their exponents whose product make the given dimension.
     * If the given dimension is a base dimension, then this method returns {@code this} raised
     * to power 1. This method never returns {@code null}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Safe because the map is unmodifiable.
    private static Map<? extends Dimension, Fraction> getBaseDimensions(final Dimension dimension) {
        if (dimension instanceof UnitDimension) {
            return ((UnitDimension) dimension).components;
        }
        /*
         * Fallback for non-SIS implementations. The cast from <? extends Dimension> to <Dimension>
         * is safe if we use the 'components' map as a read-only map (no put operation allowed).
         */
        @SuppressWarnings("unchecked")
        Map<Dimension,Integer> components = (Map<Dimension,Integer>) dimension.getBaseDimensions();
        if (components == null) {
            return Collections.singletonMap(dimension, new Fraction(1,1));
        }
        return ObjectConverters.derivedValues(components, Dimension.class, FractionConverter.FromInteger.INSTANCE);
    }

    /**
     * Returns the product of this dimension with the one specified.
     *
     * @param  multiplicand  the dimension by which to multiply this dimension.
     * @return {@code this} × {@code multiplicand}
     */
    @Override
    public UnitDimension multiply(final Dimension multiplicand) {
        return combine(multiplicand, false);
    }

    /**
     * Returns the quotient of this dimension with the one specified.
     *
     * @param  divisor  the dimension by which to divide this dimension.
     * @return {@code this} ∕ {@code divisor}
     */
    @Override
    public UnitDimension divide(final Dimension divisor) {
        return combine(divisor, true);
    }

    /**
     * Returns the product or the quotient of this dimension with the specified one.
     *
     * @param  other   the dimension by which to multiply or divide this dimension.
     * @param  mapping the operation to apply between the powers of {@code this} and {@code other} dimensions.
     * @return the product of this dimension by the given dimension raised to the given power.
     */
    private UnitDimension combine(final Dimension other, final boolean divide) {
        final Map<UnitDimension,Fraction> product = new LinkedHashMap<>(components);
        for (final Map.Entry<? extends Dimension, Fraction> entry : getBaseDimensions(other).entrySet()) {
            final Dimension dim = entry.getKey();
            Fraction p = entry.getValue();
            if (divide) {
                p = p.negate();
            }
            if (dim instanceof UnitDimension) {
                product.merge((UnitDimension) dim, p, (sum, toAdd) -> {
                    sum = sum.add(toAdd);
                    return (sum.numerator != 0) ? sum : null;
                });
            } else if (p.numerator != 0) {
                throw new UnsupportedImplementationException(Errors.format(Errors.Keys.UnsupportedImplementation_1, dim.getClass()));
            }
        }
        return create(product);
    }

    /**
     * Returns this dimension raised to an exponent.
     *
     * @param  n  power to raise this dimension to (can be negative).
     * @return {@code this}ⁿ
     */
    private UnitDimension pow(final Fraction n) {
        final Map<UnitDimension,Fraction> product = new LinkedHashMap<>(components);
        product.replaceAll((dim, power) -> power.multiply(n));
        return create(product);
    }

    /**
     * Returns this dimension raised to an exponent.
     *
     * @param  n  power to raise this dimension to (can be negative).
     * @return {@code this}ⁿ
     */
    @Override
    public UnitDimension pow(final int n) {
        switch (n) {
            case 0:  return NONE;
            case 1:  return this;
            default: return pow(new Fraction(n,1));
        }
    }

    /**
     * Returns the given root of this dimension.
     *
     * @param  n  the root's order.
     * @return {@code this} raised to power 1/n.
     */
    @Override
    public UnitDimension root(final int n) {
        switch (n) {
            case 0:  throw new ArithmeticException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "n", 0));
            case 1:  return this;
            default: return pow(new Fraction(1,n));
        }
    }

    /**
     * Compares this dimension with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof UnitDimension) {
            final UnitDimension that = (UnitDimension) other;
            if (symbol == that.symbol) {
                /*
                 * Do not compare 'components' if 'symbols' is non-zero because in such case
                 * the components map contains 'this', which would cause an infinite loop.
                 */
                return (symbol != 0) || components.equals(that.components);
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this dimension.
     */
    @Override
    public int hashCode() {
        /*
         * Do not use 'components' in hash code calculation if 'symbols' is non-zero
         * beause in such case the map contains 'this', which would cause an infinite loop.
         */
        return (symbol != 0) ? symbol ^ (int) serialVersionUID : components.hashCode();
    }

    /**
     * Returns a string representation of this dimension.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(8);
        try {
            UnitFormat.formatComponents(components, UnitFormat.Style.SYMBOL, buffer);
        } catch (IOException e) {
            throw new AssertionError(e);      // Should never happen since we are writting to a StringBuilder.
        }
        return buffer.toString();
    }
}
