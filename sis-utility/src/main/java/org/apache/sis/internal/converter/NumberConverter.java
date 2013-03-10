/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2012, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.apache.sis.internal.converter;

import java.io.Serializable;
import java.io.ObjectStreamException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.Numbers;


/**
 * Handles conversions from {@link java.lang.Number} to various objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
abstract class NumberConverter<T> extends SurjectiveConverter<Number,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8715054480508622025L;

    /**
     * For inner classes only.
     */
    NumberConverter() {
    }

    /**
     * Returns the source class, which is always {@link Number}.
     */
    @Override
    public final Class<Number> getSourceClass() {
        return Number.class;
    }

    /**
     * Converter from numbers to comparables. This special case exists because {@link Number}
     * does not implement {@link java.lang.Comparable} directly, but all known subclasses do.
     */
    @Immutable
    static final class Comparable extends NumberConverter<java.lang.Comparable<?>> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 3716134638218072176L;
        /** The unique, shared instance. */ static final Comparable INSTANCE = new Comparable();
        /** For {@link #INSTANCE} only.  */ private Comparable() {}

        @Override
        @SuppressWarnings({"rawtypes","unchecked"})
        public Class<java.lang.Comparable<?>> getTargetClass() {
            return (Class) java.lang.Comparable.class;
        }

        @Override public java.lang.Comparable<?> convert(final Number source) {
            if (source == null || source instanceof java.lang.Comparable<?>) {
                return (java.lang.Comparable<?>) source;
            }
            return (java.lang.Comparable<?>) Numbers.narrowestNumber(source);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to strings.
     */
    @Immutable
    static final class String extends NumberConverter<java.lang.String> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 1460382215827540172L;
        /** The unique, shared instance. */ static final String INSTANCE = new String();
        /** For {@link #INSTANCE} only.  */ private String() {}

        @Override public Class<java.lang.String> getTargetClass() {
            return java.lang.String.class;
        }

        @Override public java.lang.String convert(final Number source) {
            return (source != null) ? source.toString() : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to doubles.
     */
    @Immutable
    static final class Double extends NumberConverter<java.lang.Double> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 1643009985070268985L;
        /** The unique, shared instance. */ static final Double INSTANCE = new Double();
        /** For {@link #INSTANCE} only.  */ private Double() {}

        @Override public Class<java.lang.Double> getTargetClass() {
            return java.lang.Double.class;
        }

        @Override public java.lang.Double convert(final Number source) {
            return (source != null) ? java.lang.Double.valueOf(source.doubleValue()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to floats.
     */
    @Immutable
    static final class Float extends NumberConverter<java.lang.Float> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -5900985555014433974L;
        /** The unique, shared instance. */ static final Float INSTANCE = new Float();
        /** For {@link #INSTANCE} only.  */ private Float() {}

        @Override public Class<java.lang.Float> getTargetClass() {
            return java.lang.Float.class;
        }

        @Override public java.lang.Float convert(final Number source) {
            return (source != null) ? java.lang.Float.valueOf(source.floatValue()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to longs.
     */
    @Immutable
    static final class Long extends NumberConverter<java.lang.Long> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -5320144566275003574L;
        /** The unique, shared instance. */ static final Long INSTANCE = new Long();
        /** For {@link #INSTANCE} only.  */ private Long() {}

        @Override public Class<java.lang.Long> getTargetClass() {
            return java.lang.Long.class;
        }

        @Override public java.lang.Long convert(final Number source) {
            return (source != null) ? java.lang.Long.valueOf(source.longValue()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to integers.
     */
    @Immutable
    static final class Integer extends NumberConverter<java.lang.Integer> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 2661178278691398269L;
        /** The unique, shared instance. */ static final Integer INSTANCE = new Integer();
        /** For {@link #INSTANCE} only.  */ private Integer() {}

        @Override public Class<java.lang.Integer> getTargetClass() {
            return java.lang.Integer.class;
        }

        @Override public java.lang.Integer convert(final Number source) {
            return (source != null) ? java.lang.Integer.valueOf(source.intValue()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to shorts.
     */
    @Immutable
    static final class Short extends NumberConverter<java.lang.Short> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -5943559376400249179L;
        /** The unique, shared instance. */ static final Short INSTANCE = new Short();
        /** For {@link #INSTANCE} only.  */ private Short() {}

        @Override public Class<java.lang.Short> getTargetClass() {
            return java.lang.Short.class;
        }

        @Override public java.lang.Short convert(final Number source) {
            return (source != null) ? java.lang.Short.valueOf(source.shortValue()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to shorts.
     */
    @Immutable
    static final class Byte extends NumberConverter<java.lang.Byte> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 1381038535870541045L;
        /** The unique, shared instance. */ static final Byte INSTANCE = new Byte();
        /** For {@link #INSTANCE} only.  */ private Byte() {}

        @Override public Class<java.lang.Byte> getTargetClass() {
            return java.lang.Byte.class;
        }

        @Override public java.lang.Byte convert(final Number source) {
            return (source != null) ? java.lang.Byte.valueOf(source.byteValue()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to {@link java.math.BigDecimal}.
     */
    @Immutable
    static final class BigDecimal extends NumberConverter<java.math.BigDecimal> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -6318144992861058878L;
        /** The unique, shared instance. */ static final BigDecimal INSTANCE = new BigDecimal();
        /** For {@link #INSTANCE} only.  */ private BigDecimal() {
        }

        @Override public Class<java.math.BigDecimal> getTargetClass() {
            return java.math.BigDecimal.class;
        }

        @Override public java.math.BigDecimal convert(final Number source) {
            if (source == null) {
                return null;
            }
            if (source instanceof java.math.BigDecimal) {
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
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from numbers to {@link java.math.BigInteger}.
     */
    @Immutable
    static final class BigInteger extends NumberConverter<java.math.BigInteger> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 5940724099300523246L;
        /** The unique, shared instance. */ static final BigInteger INSTANCE = new BigInteger();
        /** For {@link #INSTANCE} only.  */ private BigInteger() {
        }

        @Override public Class<java.math.BigInteger> getTargetClass() {
            return java.math.BigInteger.class;
        }

        @Override public java.math.BigInteger convert(final Number source) {
            if (source == null) {
                return null;
            }
            if (source instanceof java.math.BigInteger) {
                return (java.math.BigInteger) source;
            }
            if (source instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) source).toBigInteger();
            }
            return java.math.BigInteger.valueOf(source.longValue());
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
