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

import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.Serializable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.ObjectStreamException;
import javax.measure.Dimension;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.converter.FractionConverter;
import org.apache.sis.util.internal.shared.CollectionsExt;


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
 *   <li><a href="https://en.wikipedia.org/wiki/Specific_detectivity">Specific detectivity</a>
 *       as T^2.5 / (M⋅L) dimension.</li>
 * </ul>
 *
 * All {@code UnitDimension} instances are immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class UnitDimension implements Dimension, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2568769237612674235L;

    /**
     * Pseudo-dimension for dimensionless units.
     */
    static final UnitDimension NONE = new UnitDimension(Map.of());
    // No need to store in UnitRegistry since UnitDimension performs special checks for dimensionless instances.

    /**
     * The product of base dimensions that make this dimension. All keys in this map shall be base dimensions
     * (base dimensions are identified by non-zero {@link #symbol}). If this {@code UnitDimension} is itself a
     * base dimension, then the map contains {@code this} raised to power 1. The map shall never be {@code null}.
     *
     * @see #getBaseDimensions()
     */
    @SuppressWarnings("serial")                     // The implementation is serializable.
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
    UnitDimension(final char symbol) {
        this.symbol = symbol;
        components  = Map.of(this, new Fraction(1,1).unique());
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
         * but we had to do it because we compute not only the value, but also the `components` key.
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
     * Returns {@code true} if the given dimension has no components.
     */
    static boolean isDimensionless(final Dimension dim) {
        if (dim instanceof UnitDimension) {
            return ((UnitDimension) dim).isDimensionless();
        } else if (dim != null) {
            // Fallback for foreigner implementations.
            final Map<? extends Dimension, Integer> bases = dim.getBaseDimensions();
            if (bases != null) return bases.isEmpty();
        }
        return false;       // Unit is a base unit (not a product of existing units).
    }

    /**
     * Returns {@code true} if the numerator is the dimension identified by the given symbol.
     * This method returns {@code true} only if the numerator is not be raised to any exponent
     * other than 1 and there is no other numerator. All denominator terms are ignored.
     *
     * <p>This method is used for identifying units like "kg", "kg/s", <i>etc</i> for handling
     * the "kg" prefix in a special way.</p>
     */
    final boolean numeratorIs(final char s) {
        if (symbol == s) {                                  // Optimization for a simple case.
            assert components.keySet().equals(Set.of(this));
            return true;
        }
        boolean found = false;
        for (final Map.Entry<UnitDimension,Fraction> e : components.entrySet()) {
            final Fraction value = e.getValue();
            if (e.getKey().symbol == s) {
                if (value.numerator != value.denominator) {
                    return false;                           // Raised to a power different than 1.
                }
                found = true;
            } else if (value.signum() >= 0) {
                return false;                               // Found other numerators.
            }
        }
        return found;
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
         * is safe if we use the `components` map as a read-only map (no put operation allowed).
         */
        @SuppressWarnings("unchecked")
        final var components = (Map<Dimension,Integer>) dimension.getBaseDimensions();
        if (components == null) {
            return Map.of(dimension, new Fraction(1,1));
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
     * This method may return a cached instance.
     *
     * @param  other   the dimension by which to multiply or divide this dimension.
     * @param  divide  {@code false} for a multiplication, {@code true} for a division.
     * @return the product or division of this dimension by the given dimension.
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
            if ((symbol | that.symbol) != 0) {
                return symbol == that.symbol;
            }
            /*
             * Do not compare `components` if `symbols` is non-zero because in such case
             * the components map contains `this`, which would cause an infinite loop.
             */
            return components.equals(that.components);
        }
        return false;
    }

    /**
     * Compares this dimension with the given object for equality, taking element order in account.
     *
     * @param  that  the other object to compare with.
     * @return whether the two objects are equal with elements in the same order.
     */
    final boolean equalsOrdered(final UnitDimension that) {
        if ((symbol | that.symbol) != 0) {
            return symbol == that.symbol;
        }
        return equalsOrdered(components, that.components);
    }

    /**
     * Compares the given map in a way that take order in account.
     *
     * @param  m1  the first map to compare.
     * @param  m2  the second map to compare.
     * @return whether the two maps contain the same element in the same order.
     */
    static boolean equalsOrdered(final Map<?,?> m1, final Map<?,?> m2) {
        final var i1 = m1.entrySet().iterator();
        final var i2 = m2.entrySet().iterator();
        while (i1.hasNext()) {
            if (!(i2.hasNext() && i1.next().equals(i2.next()))) {
                return false;
            }
        }
        return !i2.hasNext();
    }

    /**
     * Returns a hash code value for this dimension.
     */
    @Override
    public int hashCode() {
        /*
         * Do not use `components` in hash code calculation if `symbols` is non-zero
         * beause in such case the map contains `this`, which would cause an infinite loop.
         */
        return (symbol != 0 ? symbol : components.hashCode()) ^ (int) serialVersionUID;
    }

    /**
     * {@return returns a hash code value which takes element order in account}.
     * If the map is empty or contains only one element, then the returned value
     * is the same as {@link #hashCode()}.
     */
    final int hashCodeOrdered() {
        int code = symbol;
        if (code == 0) code = hashCodeOrdered(components);
        return code ^ (int) serialVersionUID;
    }

    /**
     * {@return an hash code value of the given map computed in an order-sensitive was}.
     *
     * @param  components  the map for which to compute hash code.
     */
    static int hashCodeOrdered(final Map<?,?> components) {
        int code = 0;
        for (final Map.Entry<?,?> entry : components.entrySet()) {
            code = code * 31 + entry.hashCode();
        }
        return code;
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
            throw new UncheckedIOException(e);      // Should never happen since we are writting to a StringBuilder.
        }
        return buffer.toString();
    }
}
