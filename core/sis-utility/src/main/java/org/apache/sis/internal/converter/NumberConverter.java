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
package org.apache.sis.internal.converter;

import java.util.Set;
import java.util.EnumSet;
import org.apache.sis.util.Numbers;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;


/**
 * Handles conversions from {@link java.lang.Number} to other kind of numbers.
 * This class supports only the type supported by {@link Numbers}.
 *
 * <div class="section">Performance note</div>
 * We provide a single class for all supported kinds of {@code Number} and delegate the actual
 * work to the {@code Numbers} static methods. This is not a very efficient way to do the work.
 * For example it may be more efficient to provide specialized subclasses for each target class,
 * so we don't have to execute the {@code switch} inside the {@code Numbers} class every time a
 * value is converted. However performance is not the primary concern here, since those converters
 * will typically be used by code doing more costly work (e.g. the {@code sis-metadata} module
 * providing {@code Map} views using Java reflection). So we rather try to be more compact.
 * If nevertheless performance appears to be a problem, consider reverting to revision 1455255
 * of this class, which was using one subclass per target type as described above.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @param <S> The source number type.
 * @param <T> The target number type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class NumberConverter<S extends Number, T extends Number> extends SystemConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3339549290992876106L;

    /**
     * The inverse converter, created when first needed.
     */
    private transient volatile ObjectConverter<T,S> inverse;

    /**
     * Creates a new converter for the given source and target classes.
     * This constructor does not verify the validity of parameter values.
     * It is caller's responsibility to ensure that the given class are
     * supported by the {@link Numbers} methods.
     */
    NumberConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        super(sourceClass, targetClass);
    }

    /**
     * Returns the inverse converter, creating it when first needed.
     * This method delegates to {@link SystemRegistry#INSTANCE} and caches the result.
     * We do not provide pre-defined constant for the various converter because there
     * is too many possibly combinations.
     */
    @Override
    public ObjectConverter<T,S> inverse() throws UnsupportedOperationException {
        // No need to synchronize. This is not a big deal if the same object is fetched twice.
        // The ConverterRegistry clas provides the required synchronization.
        ObjectConverter<T,S> candidate = inverse;
        if (candidate == null) try {
            inverse = candidate = SystemRegistry.INSTANCE.findExact(targetClass, sourceClass);
        } catch (UnconvertibleObjectException e) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.NonInvertibleConversion), e);
        }
        return candidate;
    }

    /**
     * Declares this converter as a injective or surjective function,
     * depending on whether conversions loose information or not.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(Numbers.widestClass(sourceClass, targetClass) == targetClass
                              ? FunctionProperty.INJECTIVE : FunctionProperty.SURJECTIVE,
                          FunctionProperty.ORDER_PRESERVING, FunctionProperty.INVERTIBLE);
    }

    /**
     * Converts the given number to the target type if that type is different.
     * This implementation is inefficient, but avoid us the need to create one
     * subclass for each number type. See class javadoc for more details.
     */
    @Override
    public T apply(final S source) {
        final double sourceValue = source.doubleValue();
        T target = Numbers.cast(source, targetClass);
        final double targetValue = target.doubleValue();
        if (Double.doubleToLongBits(targetValue) != Double.doubleToLongBits(sourceValue)) {
            /*
             * Casted value is not equal to the source value. Maybe we just lost the fraction digits
             * in a (double → long) cast, in which case the difference should be smaller than 1.
             */
            final double delta = Math.abs(targetValue - sourceValue);
            if (!(delta < 0.5)) { // Use '!' for catching NaN.
                if (delta < 1) {
                    target = Numbers.cast(Math.round(sourceValue), targetClass);
                } else {
                    /*
                     * The delta may be greater than 1 in a (BigInteger/BigDecimal → long) cast if the
                     * BigInteger/BigDecimal has more significant digits than what the double type can
                     * hold.
                     */
                    throw new UnconvertibleObjectException(formatErrorMessage(source));
                }
            }
        }
        return target;
    }

    /**
     * Converter from numbers to comparables. This special case exists because {@link Number}
     * does not implement {@link java.lang.Comparable} directly, but all known subclasses do.
     */
    static final class Comparable<S extends Number> extends SystemConverter<S, java.lang.Comparable<?>> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -6366381413315460619L;

        /**
         * Creates a new converter from the given type of numbers to {@code Comparable} instances.
         */
        @SuppressWarnings({"rawtypes","unchecked"})
        Comparable(final Class<S> sourceClass) {
            super(sourceClass, (Class) java.lang.Comparable.class);
        }

        /**
         * If the source class implements {@code Comparable}, then this converter is bijective.
         * Otherwise there is no known property for this converter.
         */
        @Override
        public Set<FunctionProperty> properties() {
            if (targetClass.isAssignableFrom(sourceClass)) {
                return bijective();
            }
            return EnumSet.noneOf(FunctionProperty.class);
        }

        /**
         * Converts the given number to a {@code Comparable} if its type is different.
         */
        @Override
        public java.lang.Comparable<?> apply(final Number source) {
            if (source == null || source instanceof java.lang.Comparable<?>) {
                return (java.lang.Comparable<?>) source;
            }
            return (java.lang.Comparable<?>) Numbers.narrowestNumber(source);
        }
    }
}
