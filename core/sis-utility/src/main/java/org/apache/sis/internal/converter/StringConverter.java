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
import java.nio.charset.UnsupportedCharsetException;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;


/**
 * Handles conversions between {@link String} and various kinds of objects.
 * Each inner class in this {@code StringConverter} class defines both the
 * forward and the inverse converters.
 *
 * <p>Most converters are pretty close to bijective functions, but not exactly.
 * For example conversions from {@code String} to {@link java.io.File} is not
 * completely bijective because various path separators ({@code '/'} and {@code '\'})
 * produce the same {@code File} object.</p>
 *
 * <div class="section">Special cases</div>
 * Conversion table from {@link String} to {@link java.lang.Boolean}:
 *
 * <table class="sis">
 *   <caption>Conversion table from strings</caption>
 *   <tr><th>source</th>          <th>target</th></tr>
 *   <tr><td>{@code "true"}  </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
 *   <tr><td>{@code "false"} </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
 *   <tr><td>{@code "yes"}   </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
 *   <tr><td>{@code "no"}    </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
 *   <tr><td>{@code "on"}    </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
 *   <tr><td>{@code "off"}   </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
 *   <tr><td>{@code "1"}     </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
 *   <tr><td>{@code "0"}     </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
 * </table>
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
abstract class StringConverter<T> extends SystemConverter<String, T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7181934724345598473L;

    /**
     * The inverse converter from the target to the source class.
     */
    private final ObjectConverter<T, String> inverse;

    /**
     * Creates a new converter for the given target class.
     *
     * @param targetClass The {@linkplain #getTargetClass() target class}.
     * @param inverse The inverse converter from the target to the source class.
     */
    @SuppressWarnings("OverridableMethodCallDuringObjectConstruction")
    StringConverter(final Class<T> targetClass) {
        super(String.class, targetClass);
        inverse = createInverse();
    }

    /**
     * Invoked by the constructor for creating the inverse converter.
     * To be overridden by classes which need a specialized instance.
     */
    ObjectConverter<T, String> createInverse() {
        return new ObjectToString<T>(targetClass, this);
    }

    /**
     * While this is not a general rule for surjective functions,
     * all converters defined in this class are invertibles.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.INVERTIBLE);
    }

    /**
     * Returns the inverse converter.
     */
    @Override
    public final ObjectConverter<T, String> inverse() {
        return inverse;
    }

    /**
     * Converts the given string to the target type of this converter.
     * This method verifies that the given string is non-null and non-empty,
     * then delegates to {@link #doConvert(String)}.
     *
     * @param  source The string to convert, or {@code null}.
     * @return The converted value, or {@code null} if the given string was null or empty.
     * @throws UnconvertibleObjectException If an error occurred during the conversion.
     */
    @Override
    public final T apply(String source) throws UnconvertibleObjectException {
        source = CharSequences.trimWhitespaces(source);
        if (source == null || source.isEmpty()) {
            return null;
        }
        try {
            return doConvert(source);
        } catch (UnconvertibleObjectException e) {
            throw e;
        } catch (Exception e) {
            throw new UnconvertibleObjectException(formatErrorMessage(source), e);
        }
    }

    /**
     * Invoked by {@link #apply(String)} for converting the given string to the target
     * type of this converter.
     *
     * @param  source The string to convert, guaranteed to be non-null and non-empty.
     * @return The converted value.
     * @throws Exception If an error occurred during the conversion.
     */
    abstract T doConvert(String source) throws Exception;

    /**
     * Converter from {@link String} to various kinds of {@link java.lang.Number}.
     */
    public static final class Number extends StringConverter<java.lang.Number> {
        private static final long serialVersionUID = 8356246549731207392L;
        public Number() {super(java.lang.Number.class);}                            // Instantiated by ServiceLoader.

        @Override java.lang.Number doConvert(String source) throws NumberFormatException {
            return Numbers.narrowestNumber(source);
        }
    }

    public static final class Double extends StringConverter<java.lang.Double> {
        private static final long serialVersionUID = -8576620873911755486L;
        public Double() {super(java.lang.Double.class);}                            // Instantiated by ServiceLoader.

        @Override java.lang.Double doConvert(String source) throws NumberFormatException {
            return java.lang.Double.parseDouble(source);
        }
    }

    public static final class Float extends StringConverter<java.lang.Float> {
        private static final long serialVersionUID = 2301101264860653956L;
        public Float() {super(java.lang.Float.class);}                              // Instantiated by ServiceLoader.

        @Override java.lang.Float doConvert(String source) throws NumberFormatException {
            return java.lang.Float.parseFloat(source);
        }
    }

    public static final class Long extends StringConverter<java.lang.Long> {
        private static final long serialVersionUID = 4711495660311641145L;
        public Long() {super(java.lang.Long.class);}                                // Instantiated by ServiceLoader.

        @Override java.lang.Long doConvert(String source) throws NumberFormatException {
            return java.lang.Long.parseLong(source);
        }
    }

    public static final class Integer extends StringConverter<java.lang.Integer> {
        private static final long serialVersionUID = -5024227987148221073L;
        public Integer() {super(java.lang.Integer.class);}                          // Instantiated by ServiceLoader.

        @Override java.lang.Integer doConvert(String source) throws NumberFormatException {
            return java.lang.Integer.parseInt(source);
        }
    }

    public static final class Short extends StringConverter<java.lang.Short> {
        private static final long serialVersionUID = 4067703596268901375L;
        public Short() {super(java.lang.Short.class);}                              // Instantiated by ServiceLoader.

        @Override java.lang.Short doConvert(String source) throws NumberFormatException {
            return java.lang.Short.parseShort(source);
        }
    }

    public static final class Byte extends StringConverter<java.lang.Byte> {
        private static final long serialVersionUID = 6934470534898203474L;
        public Byte() {super(java.lang.Byte.class);}                                // Instantiated by ServiceLoader.

        @Override java.lang.Byte doConvert(String source) throws NumberFormatException {
            return java.lang.Byte.parseByte(source);
        }
    }

    public static final class BigDecimal extends StringConverter<java.math.BigDecimal> {
        private static final long serialVersionUID = -5949128086478498785L;
        public BigDecimal() {super(java.math.BigDecimal.class);}                    // Instantiated by ServiceLoader.

        @Override java.math.BigDecimal doConvert(String source) throws NumberFormatException {
            return new java.math.BigDecimal(source);
        }
    }

    public static final class BigInteger extends StringConverter<java.math.BigInteger> {
        private static final long serialVersionUID = 6387019773702794255L;
        public BigInteger() {super(java.math.BigInteger.class);}                    // Instantiated by ServiceLoader.

        @Override java.math.BigInteger doConvert(String source) throws NumberFormatException {
            return new java.math.BigInteger(source);
        }
    }

    public static final class Boolean extends StringConverter<java.lang.Boolean> {
        private static final long serialVersionUID = 4689076223535035309L;
        public Boolean() {super(java.lang.Boolean.class);}                          // Instantiated by ServiceLoader.

        /** See {@link StringConverter} for the conversion table. */
        @Override java.lang.Boolean doConvert(final String source) throws UnconvertibleObjectException {
            // "String in switch" in the JDK7 branch.
            final String lower = source.toLowerCase(java.util.Locale.US);
            if (lower.equals("true")  || lower.equals("yes") || lower.equals("on")  || lower.equals("1")) return java.lang.Boolean.TRUE;
            if (lower.equals("false") || lower.equals("no")  || lower.equals("off") || lower.equals("0")) return java.lang.Boolean.FALSE;
            throw new UnconvertibleObjectException(formatErrorMessage(source));
        }
    }

    public static final class Locale extends StringConverter<java.util.Locale> {
        private static final long serialVersionUID = -794933131690043494L;
        public Locale() {super(java.util.Locale.class);}                            // Instantiated by ServiceLoader.

        @Override java.util.Locale doConvert(String source) {
            return Locales.parse(source);
        }
    }

    public static final class Charset extends StringConverter<java.nio.charset.Charset> {
        private static final long serialVersionUID = 4375157214436581095L;
        public Charset() {super(java.nio.charset.Charset.class);}                   // Instantiated by ServiceLoader.

        @Override java.nio.charset.Charset doConvert(String source) throws UnsupportedCharsetException {
            return java.nio.charset.Charset.forName(source);
        }
    }

    public static final class File extends StringConverter<java.io.File> {
        private static final long serialVersionUID = -2157537605361631529L;
        public File() {super(java.io.File.class);}                                  // Instantiated by ServiceLoader.

        @Override java.io.File doConvert(String source) {
            return new java.io.File(source);
        }
    }

    public static final class URI extends StringConverter<java.net.URI> {
        private static final long serialVersionUID = 7266486748714603336L;
        public URI() {super(java.net.URI.class);}                                   // Instantiated by ServiceLoader.

        @Override java.net.URI doConvert(String source) throws URISyntaxException {
            return new java.net.URI(source);
        }
    }

    public static final class URL extends StringConverter<java.net.URL> {
        private static final long serialVersionUID = -6518011235037500143L;
        public URL() {super(java.net.URL.class);}                                   // Instantiated by ServiceLoader.

        @Override java.net.URL doConvert(String source) throws MalformedURLException {
            return new java.net.URL(source);
        }
    }

    public static final class Unit extends StringConverter<javax.measure.unit.Unit<?>> {
        private static final long serialVersionUID = -1809497218136016210L;
        @SuppressWarnings("unchecked")
        public Unit() {super((Class) javax.measure.unit.Unit.class);}               // Instantiated by ServiceLoader.

        @Override javax.measure.unit.Unit<?> doConvert(String source) throws IllegalArgumentException {
            return Units.valueOf(source);
        }
    }

    public static final class Angle extends StringConverter<org.apache.sis.measure.Angle> {
        private static final long serialVersionUID = -6937967772504961327L;
        public Angle() {super(org.apache.sis.measure.Angle.class);}                 // Instantiated by ServiceLoader.

        @Override org.apache.sis.measure.Angle doConvert(String source) throws NumberFormatException {
            return new org.apache.sis.measure.Angle(source);
        }
    }

    public static final class InternationalString extends StringConverter<org.opengis.util.InternationalString> {
        private static final long serialVersionUID = -7386778914761469877L;
        public InternationalString() {super(org.opengis.util.InternationalString.class);} // Instantiated by ServiceLoader.

        @Override org.opengis.util.InternationalString doConvert(String source) {
            return new SimpleInternationalString(source);
        }

        /** Declares that the converter is bijective. */
        @Override public Set<FunctionProperty> properties() {
            return bijective();
        }
    }

    /**
     * Converter from {@link String} to {@link org.opengis.util.CodeList}.
     * This converter is particular in that it requires the target class in argument
     * to the constructor.
     *
     * <p>Instances of this class are created by
     * {@link SystemRegistry#createConverter(Class, Class)}.</p>
     */
    static final class CodeList<T extends org.opengis.util.CodeList<T>> extends StringConverter<T> {
        /** For cross-version compatibility on serialization. */
        private static final long serialVersionUID = -6351669842222010105L;

        /** Creates a new converter for the given code list. */
        CodeList(final Class<T> targetClass) {
            super(targetClass);
        }

        /** Converts the given string to the target type of this converter. */
        @Override T doConvert(final String source) {
            final T code = Types.forCodeName(targetClass, source, false);
            if (code == null) {
                throw new UnconvertibleObjectException(formatErrorMessage(source));
            }
            return code;
        }

        /** Invoked by the constructor for creating the inverse converter. */
        @Override ObjectConverter<T, String> createInverse() {
            return new ObjectToString.CodeList<T>(targetClass, this);
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Enum}.
     * This converter is particular in that it requires the target class in argument
     * to the constructor.
     *
     * <p>Instances of this class are created by
     * {@link SystemRegistry#createConverter(Class, Class)}.</p>
     */
    static final class Enum<T extends java.lang.Enum<T>> extends StringConverter<T> {
        /** For cross-version compatibility on serialization. */
        private static final long serialVersionUID = -4124617013044304640L;

        /** Creates a new converter for the given enumeration. */
        Enum(final Class<T> targetClass) {
            super(targetClass);
        }

        /** Converts the given string to the target type of this converter. */
        @Override T doConvert(final String source) {
            final T code = Types.forEnumName(targetClass, source);
            if (code == null) {
                throw new UnconvertibleObjectException(formatErrorMessage(source));
            }
            return code;
        }

        /** Invoked by the constructor for creating the inverse converter. */
        @Override ObjectConverter<T, String> createInverse() {
            return new ObjectToString.Enum<T>(targetClass, this);
        }
    }
}
