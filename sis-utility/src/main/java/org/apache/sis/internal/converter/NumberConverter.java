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

import java.io.Serializable;
import java.io.ObjectStreamException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.resources.Errors;


/**
 * Handles conversions from {@link java.lang.Number} to various objects.
 *
 * @param <S> The source type. All shared instances will declare {@link Number},
 *            but some more specific types will occasionally need to be declared
 *            for inverse converters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
abstract class NumberConverter<S extends Number, T> extends InjectiveConverter<S,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8715054480508622025L;

    /**
     * The source class.
     */
    private final Class<S> sourceClass;

    /**
     * For inner classes only.
     */
    NumberConverter(final Class<S> sourceClass) {
        this.sourceClass = sourceClass;
    }

    /**
     * Returns the source class given at construction time.
     */
    @Override
    public final Class<S> getSourceClass() {
        return sourceClass;
    }

    /**
     * Default implementation suitable only for subclasses having a target class assignable
     * to {@link java.lang.Number}. In particular, the {@code Comparable} and {@code String}
     * subclasses <strong>must</strong> override this method.
     */
    @Override
    @SuppressWarnings("unchecked")
    public ObjectConverter<T,S> inverse() {
        assert Number.class.isAssignableFrom(getTargetClass()) : this;
        return (ObjectConverter<T,S>) create((Class<? extends Number>) getTargetClass(), sourceClass);
    }

    /**
     * Creates a converter between numbers of the given classes.
     */
    @SuppressWarnings("unchecked") // Only for the last line.
    private static <S extends Number, T extends Number> ObjectConverter<S,T>
            create(final Class<S> sourceClass, final Class<T> targetClass)
    {
        final ObjectConverter<S,?> c;
        switch (Numbers.getEnumConstant(targetClass)) {
            case Numbers.DOUBLE:      c = new Double<>    (sourceClass); break;
            case Numbers.FLOAT:       c = new Float<>     (sourceClass); break;
            case Numbers.LONG:        c = new Long<>      (sourceClass); break;
            case Numbers.INTEGER:     c = new Integer<>   (sourceClass); break;
            case Numbers.SHORT:       c = new Short<>     (sourceClass); break;
            case Numbers.BYTE:        c = new Byte<>      (sourceClass); break;
            case Numbers.BIG_INTEGER: c = new BigInteger<>(sourceClass); break;
            case Numbers.BIG_DECIMAL: c = new BigDecimal<>(sourceClass); break;
            default: throw new AssertionError(targetClass);
        }
        return (ObjectConverter<S,T>) c;
    }

    /**
     * Returns the singleton instance on deserialization if the type is {@link Number}.
     */
    final Object readResolve() throws ObjectStreamException {
        return (sourceClass == Number.class) ? singleton() : this;
    }

    /**
     * Returns the singleton instance of this converter.
     */
    abstract NumberConverter<Number,T> singleton();

    /**
     * Converter from numbers to comparables. This special case exists because {@link Number}
     * does not implement {@link java.lang.Comparable} directly, but all known subclasses do.
     */
    @Immutable
    static final class Comparable<S extends Number> extends NumberConverter<S, java.lang.Comparable<?>> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 3716134638218072176L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Comparable<Number> INSTANCE = new Comparable<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Comparable} instances.
         */
        Comparable(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override @SuppressWarnings({"rawtypes","unchecked"})
        public Class<java.lang.Comparable<?>> getTargetClass() {
            return (Class) java.lang.Comparable.class;
        }

        /** Converts the given number to a {@code Comparable} if its type is different. */
        @Override public java.lang.Comparable<?> convert(final Number source) {
            if (source == null || source instanceof java.lang.Comparable<?>) {
                return (java.lang.Comparable<?>) source;
            }
            return (java.lang.Comparable<?>) Numbers.narrowestNumber(source);
        }

        /** Non-invertible converter (for now). */
        @Override public ObjectConverter<java.lang.Comparable<?>, S> inverse() {
            throw new UnsupportedOperationException(Errors.format(
                    Errors.Keys.UnsupportedOperation_1, "inverse"));
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Comparable<?>> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to doubles.
     */
    @Immutable
    static final class Double<S extends Number> extends NumberConverter<S, java.lang.Double> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1643009985070268985L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Double<Number> INSTANCE = new Double<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Double} instances.
         */
        Double(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.lang.Double> getTargetClass() {
            return java.lang.Double.class;
        }

        /** Converts the given number to a {@code Double} if its type is different. */
        @Override public java.lang.Double convert(final S source) {
            if (source == null || source instanceof java.lang.Double) {
                return (java.lang.Double) source;
            }
            return java.lang.Double.valueOf(source.doubleValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Double> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to floats.
     */
    @Immutable
    static final class Float<S extends Number> extends NumberConverter<S, java.lang.Float> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5900985555014433974L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Float<Number> INSTANCE = new Float<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Float} instances.
         */
        Float(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.lang.Float> getTargetClass() {
            return java.lang.Float.class;
        }

        /** Converts the given number to a {@code Float} if its type is different. */
        @Override public java.lang.Float convert(final S source) {
            if (source == null || source instanceof java.lang.Float) {
                return (java.lang.Float) source;
            }
            return java.lang.Float.valueOf(source.floatValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Float> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to longs.
     */
    @Immutable
    static final class Long<S extends Number> extends NumberConverter<S, java.lang.Long> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5320144566275003574L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Long<Number> INSTANCE = new Long<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Long} instances.
         */
        Long(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.lang.Long> getTargetClass() {
            return java.lang.Long.class;
        }

        /** Converts the given number to a {@code Long} if its type is different. */
        @Override public java.lang.Long convert(final S source) {
            if (source == null || source instanceof java.lang.Long) {
                return (java.lang.Long) source;
            }
            return java.lang.Long.valueOf(source.longValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Long> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to integers.
     */
    @Immutable
    static final class Integer<S extends Number> extends NumberConverter<S, java.lang.Integer> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2661178278691398269L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Integer<Number> INSTANCE = new Integer<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Integer} instances.
         */
        Integer(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.lang.Integer> getTargetClass() {
            return java.lang.Integer.class;
        }

        /** Converts the given number to an {@code Integer} if its type is different. */
        @Override public java.lang.Integer convert(final S source) {
            if (source == null || source instanceof java.lang.Integer) {
                return (java.lang.Integer) source;
            }
            return java.lang.Integer.valueOf(source.intValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Integer> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to shorts.
     */
    @Immutable
    static final class Short<S extends Number> extends NumberConverter<S, java.lang.Short> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5943559376400249179L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Short<Number> INSTANCE = new Short<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Short} instances.
         */
        Short(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.lang.Short> getTargetClass() {
            return java.lang.Short.class;
        }

        /** Converts the given number to a {@code Short} if its type is different. */
        @Override public java.lang.Short convert(final S source) {
            if (source == null || source instanceof java.lang.Short) {
                return (java.lang.Short) source;
            }
            return java.lang.Short.valueOf(source.shortValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Short> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to shorts.
     */
    @Immutable
    static final class Byte<S extends Number> extends NumberConverter<S, java.lang.Byte> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1381038535870541045L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final Byte<Number> INSTANCE = new Byte<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code Byte} instances.
         */
        Byte(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.lang.Byte> getTargetClass() {
            return java.lang.Byte.class;
        }

        /** Converts the given number to a {@code Byte} if its type is different. */
        @Override public java.lang.Byte convert(final S source) {
            if (source == null || source instanceof java.lang.Byte) {
                return (java.lang.Byte) source;
            }
            return java.lang.Byte.valueOf(source.byteValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.lang.Byte> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to {@link java.math.BigDecimal}.
     */
    @Immutable
    static final class BigDecimal<S extends Number> extends NumberConverter<S, java.math.BigDecimal> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -6318144992861058878L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final BigDecimal<Number> INSTANCE = new BigDecimal<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code BigDecimal} instances.
         */
        BigDecimal(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.math.BigDecimal> getTargetClass() {
            return java.math.BigDecimal.class;
        }

        /** Converts the given number to a {@code BigDecimal} if its type is different. */
        @Override public java.math.BigDecimal convert(final S source) {
            if (source == null || source instanceof java.math.BigDecimal) {
                return (java.math.BigDecimal) source;
            }
            if (source instanceof java.math.BigInteger) {
                return new java.math.BigDecimal((java.math.BigInteger) source);
            }
            if (Numbers.isInteger(source.getClass())) {
                return java.math.BigDecimal.valueOf(source.longValue());
            }
            return java.math.BigDecimal.valueOf(source.doubleValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.math.BigDecimal> singleton() {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to {@link java.math.BigInteger}.
     */
    @Immutable
    static final class BigInteger<S extends Number> extends NumberConverter<S, java.math.BigInteger> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 5940724099300523246L;

        /**
         * The usually shared instance. {@link ConverterRegistry} needs only the {@link Number}
         * type. Other types are created only by {@code StringConverter.Foo.inverse()} methods.
         */
        static final BigInteger<Number> INSTANCE = new BigInteger<>(Number.class);

        /**
         * Creates a new converter from the given type of numbers to {@code BigInteger} instances.
         */
        BigInteger(final Class<S> sourceClass) {
            super(sourceClass);
        }

        /** Returns the destination type (same for all instances of this class). */
        @Override public Class<java.math.BigInteger> getTargetClass() {
            return java.math.BigInteger.class;
        }

        /** Converts the given number to a {@code BigInteger} if its type is different. */
        @Override public java.math.BigInteger convert(final S source) {
            if (source == null || source instanceof java.math.BigInteger) {
                return (java.math.BigInteger) source;
            }
            if (source instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) source).toBigInteger();
            }
            return java.math.BigInteger.valueOf(source.longValue());
        }

        /** Returns the singleton instance on deserialization. */
        @Override NumberConverter<Number, java.math.BigInteger> singleton() {
            return INSTANCE;
        }
    }
}
