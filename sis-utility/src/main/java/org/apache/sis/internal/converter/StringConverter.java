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
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.nio.charset.UnsupportedCharsetException;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import net.jcip.annotations.Immutable;

import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * Handles conversions from {@link String} to various objects.
 * The source class is fixed to {@code String}. The target class is determined
 * by the inner class which extends this {@code StringConverter}.
 *
 * <p>All subclasses will have a unique instance. For this reason, it is not necessary to
 * override the {@code hashCode()} and {@code equals(Object)} methods, since identity
 * comparisons will work just well.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
abstract class StringConverter<T> extends SurjectiveConverter<String,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3397013355582381432L;

    /**
     * For inner classes only.
     */
    StringConverter() {
    }

    /**
     * Returns the source class, which is always {@link String}.
     */
    @Override
    public final Class<String> getSourceClass() {
        return String.class;
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
     * Converts the given string to the target type of this converter.
     * This method verifies that the given string is non-null and non-empty,
     * then delegates to {@link #doConvert(String)}.
     *
     * @param  source The string to convert, or {@code null}.
     * @return The converted value, or {@code null} if the given string was null or empty.
     * @throws UnconvertibleObjectException If an error occurred during the conversion.
     */
    @Override
    public final T convert(String source) throws UnconvertibleObjectException {
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
     * Invoked by {@link #convert(String)} for converting the given string to the target
     * type of this converter.
     *
     * @param  source The string to convert, guaranteed to be non-null and non-empty.
     * @return The converted value.
     * @throws Exception If an error occurred during the conversion.
     */
    abstract T doConvert(String source) throws Exception;

    /**
     * Converter from {@link String} to {@link java.lang.Number}.
     * The finest suitable kind of number will be selected using
     * {@link Numbers#narrowestNumber(String)}.
     */
    @Immutable
    static final class Number extends StringConverter<java.lang.Number> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 1557277544742023571L;
        /** The unique, shared instance. */ static final Number INSTANCE = new Number();
        /** For {@link #INSTANCE} only.  */ private Number() {}

        @Override public Class<java.lang.Number> getTargetClass() {
            return java.lang.Number.class;
        }

        @Override java.lang.Number doConvert(String source) throws NumberFormatException {
            return Numbers.narrowestNumber(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Number, String> inverse() {
            return ObjectToString.NUMBER;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Double}.
     */
    @Immutable
    static final class Double extends StringConverter<java.lang.Double> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -9094071164371643060L;
        /** The unique, shared instance. */ static final Double INSTANCE = new Double();
        /** For {@link #INSTANCE} only.  */ private Double() {}

        @Override public Class<java.lang.Double> getTargetClass() {
            return java.lang.Double.class;
        }

        @Override java.lang.Double doConvert(String source) throws NumberFormatException {
            return java.lang.Double.parseDouble(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Double, String> inverse() {
            return new ObjectToString<>(java.lang.Double.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Float}.
     */
    @Immutable
    static final class Float extends StringConverter<java.lang.Float> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -2815192289550338333L;
        /** The unique, shared instance. */ static final Float INSTANCE = new Float();
        /** For {@link #INSTANCE} only.  */ private Float() {}

        @Override public Class<java.lang.Float> getTargetClass() {
            return java.lang.Float.class;
        }

        @Override java.lang.Float doConvert(String source) throws NumberFormatException {
            return java.lang.Float.parseFloat(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Float, String> inverse() {
            return new ObjectToString<>(java.lang.Float.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Long}.
     */
    @Immutable
    static final class Long extends StringConverter<java.lang.Long> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -2171263041723939779L;
        /** The unique, shared instance. */ static final Long INSTANCE = new Long();
        /** For {@link #INSTANCE} only.  */ private Long() {}

        @Override public Class<java.lang.Long> getTargetClass() {
            return java.lang.Long.class;
        }

        @Override java.lang.Long doConvert(String source) throws NumberFormatException {
            return java.lang.Long.parseLong(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Long, String> inverse() {
            return new ObjectToString<>(java.lang.Long.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Integer}.
     */
    @Immutable
    static final class Integer extends StringConverter<java.lang.Integer> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 763211364703205967L;
        /** The unique, shared instance. */ static final Integer INSTANCE = new Integer();
        /** For {@link #INSTANCE} only.  */ private Integer() {}

        @Override public Class<java.lang.Integer> getTargetClass() {
            return java.lang.Integer.class;
        }

        @Override java.lang.Integer doConvert(String source) throws NumberFormatException {
            return java.lang.Integer.parseInt(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Integer, String> inverse() {
            return new ObjectToString<>(java.lang.Integer.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Short}.
     */
    @Immutable
    static final class Short extends StringConverter<java.lang.Short> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -1770870328699572960L;
        /** The unique, shared instance. */ static final Short INSTANCE = new Short();
        /** For {@link #INSTANCE} only.  */ private Short() {}

        @Override public Class<java.lang.Short> getTargetClass() {
            return java.lang.Short.class;
        }

        @Override java.lang.Short doConvert(String source) throws NumberFormatException {
            return java.lang.Short.parseShort(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Short, String> inverse() {
            return new ObjectToString<>(java.lang.Short.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Byte}.
     */
    @Immutable
    static final class Byte extends StringConverter<java.lang.Byte> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 2084870859391804185L;
        /** The unique, shared instance. */ static final Byte INSTANCE = new Byte();
        /** For {@link #INSTANCE} only.  */ private Byte() {}

        @Override public Class<java.lang.Byte> getTargetClass() {
            return java.lang.Byte.class;
        }

        @Override java.lang.Byte doConvert(String source) throws NumberFormatException {
            return java.lang.Byte.parseByte(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Byte, String> inverse() {
            return new ObjectToString<>(java.lang.Byte.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.math.BigDecimal}.
     */
    @Immutable
    static final class BigDecimal extends StringConverter<java.math.BigDecimal> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -8597497425876120213L;
        /** The unique, shared instance. */ static final BigDecimal INSTANCE = new BigDecimal();
        /** For {@link #INSTANCE} only.  */ private BigDecimal() {}

        @Override public Class<java.math.BigDecimal> getTargetClass() {
            return java.math.BigDecimal.class;
        }

        @Override java.math.BigDecimal doConvert(String source) throws NumberFormatException {
            return new java.math.BigDecimal(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.math.BigDecimal, String> inverse() {
            return new ObjectToString<>(java.math.BigDecimal.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.math.BigInteger}.
     */
    @Immutable
    static final class BigInteger extends StringConverter<java.math.BigInteger> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 8658903031519526466L;
        /** The unique, shared instance. */ static final BigInteger INSTANCE = new BigInteger();
        /** For {@link #INSTANCE} only.  */ private BigInteger() {}

        @Override public Class<java.math.BigInteger> getTargetClass() {
            return java.math.BigInteger.class;
        }

        @Override java.math.BigInteger doConvert(String source) throws NumberFormatException {
            return new java.math.BigInteger(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.math.BigInteger, String> inverse() {
            return new ObjectToString<>(java.math.BigInteger.class, this);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.lang.Boolean}.
     * Conversion table:
     *
     * <table>
     *    <tr><th>source</th>          <th>target</th></tr>
     *    <tr><td>{@code "true"}  </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
     *    <tr><td>{@code "false"} </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
     *    <tr><td>{@code "yes"}   </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
     *    <tr><td>{@code "no"}    </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
     *    <tr><td>{@code "on"}    </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
     *    <tr><td>{@code "off"}   </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
     *    <tr><td>{@code "1"}     </td><td>{@link java.lang.Boolean#TRUE}  </td></tr>
     *    <tr><td>{@code "0"}     </td><td>{@link java.lang.Boolean#FALSE} </td></tr>
     * </table>
     */
    @Immutable
    static final class Boolean extends StringConverter<java.lang.Boolean> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -27525398425996373L;
        /** The unique, shared instance. */ static final Boolean INSTANCE = new Boolean();
        /** For {@link #INSTANCE} only.  */ private Boolean() {}

        @Override public Class<java.lang.Boolean> getTargetClass() {
            return java.lang.Boolean.class;
        }

        @Override java.lang.Boolean doConvert(final String source) throws UnconvertibleObjectException {
            switch (source.toLowerCase(java.util.Locale.ROOT)) {
                case "true":  case "yes": case "on":  case "1": return java.lang.Boolean.TRUE;
                case "false": case "no":  case "off": case "0": return java.lang.Boolean.FALSE;
            }
            throw new UnconvertibleObjectException(formatErrorMessage(source));
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.lang.Boolean, String> inverse() {
            return ObjectToString.BOOLEAN;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.util.Locale}.
     * Examples of locale in string form: {@code "fr"}, {@code "fr_CA"}.
     */
    @Immutable
    static final class Locale extends StringConverter<java.util.Locale> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -2888932450292616036L;
        /** The unique, shared instance. */ static final Locale INSTANCE = new Locale();
        /** For {@link #INSTANCE} only.  */ private Locale() {}

        @Override public Class<java.util.Locale> getTargetClass() {
            return java.util.Locale.class;
        }

        @Override java.util.Locale doConvert(String source) throws IllegalArgumentException {
            return Locales.parse(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.util.Locale, String> inverse() {
            return ObjectToString.LOCALE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.nio.charset.Charset}.
     */
    @Immutable
    static final class Charset extends StringConverter<java.nio.charset.Charset> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 4539755855992944656L;
        /** The unique, shared instance. */ static final Charset INSTANCE = new Charset();
        /** For {@link #INSTANCE} only.  */ private Charset() {}

        @Override public Class<java.nio.charset.Charset> getTargetClass() {
            return java.nio.charset.Charset.class;
        }

        @Override java.nio.charset.Charset doConvert(String source) throws UnsupportedCharsetException {
            return java.nio.charset.Charset.forName(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.nio.charset.Charset, String> inverse() {
            return ObjectToString.CHARSET;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link org.opengis.util.InternationalString}.
     */
    @Immutable
    static final class InternationalString extends StringConverter<org.opengis.util.InternationalString> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 730809620191573819L;
        /** The unique, shared instance. */ static final InternationalString INSTANCE = new InternationalString();
        /** For {@link #INSTANCE} only.  */ private InternationalString() {}

        /** Returns the function properties, which is bijective. */
        @Override public Set<FunctionProperty> properties() {
            return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE,
                    FunctionProperty.ORDER_PRESERVING, FunctionProperty.INVERTIBLE);
        }

        @Override public Class<org.opengis.util.InternationalString> getTargetClass() {
            return org.opengis.util.InternationalString.class;
        }

        @Override org.opengis.util.InternationalString doConvert(String source) {
            return new SimpleInternationalString(source);
        }

        @Override public ObjectConverter<org.opengis.util.InternationalString, String> inverse() {
            return ObjectToString.I18N;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.io.File}.
     * This converter is almost bijective, but not completely since various path separators
     * ({@code '/'} and {@code '\'}) produce the same {@code File} object.
     */
    @Immutable
    static final class File extends StringConverter<java.io.File> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 6445208470928432376L;
        /** The unique, shared instance. */ static final File INSTANCE = new File();
        /** For {@link #INSTANCE} only.  */ private File() {}

        @Override public Class<java.io.File> getTargetClass() {
            return java.io.File.class;
        }

        @Override java.io.File doConvert(String source) {
            return new java.io.File(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.io.File, String> inverse() {
            return ObjectToString.FILE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.nio.file.Path}.
     */
    @Immutable
    static final class Path extends StringConverter<java.nio.file.Path> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -5227120925547132828L;
        /** The unique, shared instance. */ static final Path INSTANCE = new Path();
        /** For {@link #INSTANCE} only.  */ private Path() {}

        @Override public Class<java.nio.file.Path> getTargetClass() {
            return java.nio.file.Path.class;
        }

        @Override java.nio.file.Path doConvert(String source) throws InvalidPathException {
            return java.nio.file.Paths.get(source);
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.net.URI}.
     */
    @Immutable
    static final class URI extends StringConverter<java.net.URI> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -2804405634789179706L;
        /** The unique, shared instance. */ static final URI INSTANCE = new URI();
        /** For {@link #INSTANCE} only.  */ private URI() {}

        @Override public Class<java.net.URI> getTargetClass() {
            return java.net.URI.class;
        }

        @Override java.net.URI doConvert(String source) throws URISyntaxException {
            return new java.net.URI(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.net.URI, String> inverse() {
            return ObjectToString.URI;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link java.net.URL}.
     */
    @Immutable
    static final class URL extends StringConverter<java.net.URL> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 2303928306635765592L;
        /** The unique, shared instance. */ static final URL INSTANCE = new URL();
        /** For {@link #INSTANCE} only.  */ private URL() {}

        @Override public Class<java.net.URL> getTargetClass() {
            return java.net.URL.class;
        }

        @Override java.net.URL doConvert(String source) throws MalformedURLException {
            return new java.net.URL(source);
        }

        /** Returns the inverse, since this converter is "almost" bijective. */
        @Override public ObjectConverter<java.net.URL, String> inverse() {
            return ObjectToString.URL;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from {@link String} to {@link org.opengis.util.CodeList}.
     * This converter is particular in that it requires the target class in argument
     * to the constructor.
     */
    @Immutable
    static final class CodeList<T extends org.opengis.util.CodeList<T>> extends StringConverter<T> {
        /** For cross-version compatibility on serialization. */
        static final long serialVersionUID = 3289083947166861278L;

        /** The type of the code list. */
        private final Class<T> targetType;

        /** Creates a new converter for the given code list. */
        CodeList(final Class<T> targetType) {
            this.targetType = targetType;
        }

        /** Returns the target class given at construction time. */
        @Override public Class<T> getTargetClass() {
            return targetType;
        }

        /** Converts the given string to the target type of this converter. */
        @Override T doConvert(String source) {
            return Types.forCodeName(targetType, source, true);
        }
    }
}
