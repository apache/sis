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
package org.apache.sis.xml;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.util.MissingResourceException;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import javax.measure.unit.Unit;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Locales;

import static org.apache.sis.util.CharSequences.trimWhitespaces;


/**
 * Performs conversions of XML element or attribute values encountered during XML (un)marshalling.
 * Each method in this class is a converter and can be invoked at (un)marshalling time.
 * The default implementation is straightforward and documented in the javadoc of each method.
 *
 * <p>This class provides a way to handle the errors which may exist in some XML documents.
 * For example a URL in the document may be malformed, causing a {@link MalformedURLException}
 * to be thrown. If this error is not handled, it will cause the (un)marshalling of the entire
 * document to fail. An application may want to change this behavior by replacing URLs that
 * are known to be erroneous by fixed versions of those URLs. Example:</p>
 *
 * {@preformat java
 *     class URLFixer extends ValueConverter {
 *         &#64;Override
 *         public URL toURL(MarshalContext context, URI uri) throws MalformedURLException {
 *             try {
 *                 return super.toURL(context, uri);
 *             } catch (MalformedURLException e) {
 *                 if (uri.equals(KNOWN_ERRONEOUS_URI) {
 *                     return FIXED_URL;
 *                 } else {
 *                     throw e;
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * See the {@link XML#CONVERTER} javadoc for an example of registering a custom
 * {@code ValueConverter} to a (un)marshaller.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public class ValueConverter {
    /**
     * The default, thread-safe and immutable instance. This instance defines the
     * converters used during every (un)marshalling if no {@code ValueConverter}
     * was explicitly set.
     */
    public static final ValueConverter DEFAULT = new ValueConverter();

    /**
     * Creates a default {@code ValueConverter}. This is for subclasses only,
     * since new instances are useful only if at least one method is overridden.
     */
    protected ValueConverter() {
    }

    /**
     * Invoked when an exception occurred in any {@code toXXX(…)} method. The default implementation
     * does nothing and return {@code false}, which will cause the (un)marshalling process of the
     * whole XML document to fail.
     *
     * <p>This method provides a single hook that subclasses can override in order to provide their
     * own error handling for every methods defined in this class, like the example documented in
     * the {@link XML#CONVERTER} javadoc. Subclasses also have the possibility to override individual
     * {@code toXXX(…)} methods, like the example provided in this <a href="#skip-navbar_top">class
     * javadoc</a>.</p>
     *
     * @param  <T> The compile-time type of the {@code sourceType} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The value that can't be converted.
     * @param  sourceType The base type of the value to convert. This is determined by the argument
     *         type of the method that caught the exception. For example the source type is always
     *         {@code URI.class} if the exception has been caught by the {@link #toURL(MarshalContext, URI)} method.
     * @param  targetType The expected type of the converted object.
     * @param  exception The exception that occurred during the conversion attempt.
     * @return {@code true} if the (un)marshalling process should continue despite this error,
     *         or {@code false} (the default) if the exception should be propagated, thus causing
     *         the (un)marshalling to fail.
     */
    protected <T> boolean exceptionOccured(MarshalContext context, T value,
            Class<T> sourceType, Class<?> targetType, Exception exception)
    {
        return false;
    }

    /**
     * Converts the given locale to a language code. For better ISO 19139 compliance, the language code
     * should be a 3-letters ISO 639-2 code (e.g. {@code "jpn"} for {@linkplain Locale#JAPANESE Japanese}).
     * However those codes may not be available for every locales.
     *
     * <p>The default implementation performs the following steps:</p>
     * <ul>
     *   <li>Try {@link Locale#getISO3Language()}:<ul>
     *     <li>On success, return that value if non-empty, or {@code null} otherwise.</li>
     *     <li>If an exception has been thrown, then:<ul>
     *       <li>If {@link #exceptionOccured exceptionOccured(…)} return {@code true}, then
     *           returns {@code value.getLanguage()} if non-empty or {@code null} otherwise.</li>
     *       <li>If {@code exceptionOccured(…)} returned {@code false} (which is the default
     *           behavior), then let the exception propagate.</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The locale to convert to a language code, or {@code null}.
     * @return The language code, or {@code null} if the given value was null or does not contains a language code.
     * @throws MissingResourceException If no language code can be found for the given locale.
     *
     * @see Locale#getISO3Language()
     * @see Locale#getLanguage()
     */
    public String toLanguageCode(final MarshalContext context, final Locale value) throws MissingResourceException {
        if (value != null) {
            String code;
            try {
                code = value.getISO3Language();
            } catch (MissingResourceException e) {
                if (!exceptionOccured(context, value, Locale.class, String.class, e)) {
                    throw e;
                }
                code = value.getLanguage();
            }
            if (!code.isEmpty()) {
                return code;
            }
        }
        return null;
    }

    /**
     * Converts the given locale to a country code. For better ISO 19139 compliance, the country code
     * should be a 2-letters ISO 3166 code (e.g. {@code "JP"} for {@linkplain Locale#JAPAN Japan}).
     *
     * <p>The default implementation returns {@link Locale#getCountry()} if non-empty, or {@code null} otherwise.</p>
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The locale to convert to a country code, or {@code null}.
     * @return The country code, or {@code null} if the given value was null or does not contains a country code.
     * @throws MissingResourceException If no country code can be found for the given locale.
     *
     * @see Locale#getISO3Country()
     * @see Locale#getCountry()
     */
    public String toCountryCode(final MarshalContext context, final Locale value) throws MissingResourceException {
        if (value != null) {
            String code = value.getCountry();
            if (!code.isEmpty()) {
                return code;
            }
        }
        return null;
    }

    /**
     * Converts the given character set to a code.
     *
     * <p>The default implementation first invokes {@link Charset#name()}. Then if marshalling to ISO 19139:2007,
     * this method converts the <a href="http://www.iana.org/assignments/character-sets">IANA</a> name to a
     * ISO 19115:2003 {@code MD_CharacterSetCode} using the following equivalence table:</p>
     *
     * <table class="sis">
     *   <caption>IANA to ISO 19115:2003 character set code</caption>
     *   <tr>
     *     <td><table class="compact" summary="IANA to ISO 19115:2003">
     *       <tr><td style="width: 90px"><b>IANA</b></td><td><b>ISO 19115:2003</b></td></tr>
     *       <tr><td>{@code ISO-8859-1}</td>  <td>{@code 8859part1}</td></tr>
     *       <tr><td>{@code ISO-8859-2}</td>  <td>{@code 8859part2}</td></tr>
     *       <tr><td>{@code ISO-8859-3}</td>  <td>{@code 8859part3}</td></tr>
     *       <tr><td>{@code ISO-8859-4}</td>  <td>{@code 8859part4}</td></tr>
     *       <tr><td>{@code ISO-8859-5}</td>  <td>{@code 8859part5}</td></tr>
     *       <tr><td>{@code ISO-8859-6}</td>  <td>{@code 8859part6}</td></tr>
     *       <tr><td>{@code ISO-8859-7}</td>  <td>{@code 8859part7}</td></tr>
     *       <tr><td>{@code ISO-8859-8}</td>  <td>{@code 8859part8}</td></tr>
     *       <tr><td>{@code ISO-8859-9}</td>  <td>{@code 8859part9}</td></tr>
     *       <tr><td>{@code ISO-8859-10}</td> <td>{@code 8859part10}</td></tr>
     *       <tr><td>{@code ISO-8859-11}</td> <td>{@code 8859part11}</td></tr>
     *       <tr><td>{@code ISO-8859-12}</td> <td>{@code 8859part12}</td></tr>
     *       <tr><td>{@code ISO-8859-13}</td> <td>{@code 8859part13}</td></tr>
     *       <tr><td>{@code ISO-8859-14}</td> <td>{@code 8859part14}</td></tr>
     *       <tr><td>{@code ISO-8859-15}</td> <td>{@code 8859part15}</td></tr>
     *       <tr><td>{@code ISO-8859-16}</td> <td>{@code 8859part16}</td></tr>
     *     </table></td>
     *     <td class="sep"><table class="compact" summary="IANA to ISO 19115:2003">
     *       <tr><td style="width: 90px"><b>IANA</b></td><td><b>ISO 19115:2003</b></td></tr>
     *       <tr><td>{@code UCS-2}</td>     <td>{@code ucs2}</td></tr>
     *       <tr><td>{@code UCS-4}</td>     <td>{@code ucs4}</td></tr>
     *       <tr><td>{@code UTF-7}</td>     <td>{@code utf7}</td></tr>
     *       <tr><td>{@code UTF-8}</td>     <td>{@code utf8}</td></tr>
     *       <tr><td>{@code UTF-16}</td>    <td>{@code utf16}</td></tr>
     *       <tr><td>{@code JIS_X0201}</td> <td>{@code jis}</td></tr>
     *       <tr><td>{@code Shift_JIS}</td> <td>{@code shiftJIS}</td></tr>
     *       <tr><td>{@code EUC-JP}</td>    <td>{@code eucJP}</td></tr>
     *       <tr><td>{@code US-ASCII}</td>  <td>{@code usAscii}</td></tr>
     *       <tr><td>{@code EBCDIC}</td>    <td>{@code ebcdic}</td></tr>
     *       <tr><td>{@code EUC-KR}</td>    <td>{@code eucKR}</td></tr>
     *       <tr><td>{@code Big5}</td>      <td>{@code big5}</td></tr>
     *       <tr><td>{@code GB2312}</td>    <td>{@code GB2312}</td></tr>
     *     </table></td>
     *   </tr>
     * </table>
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The locale to convert to a character set code, or {@code null}.
     * @return The country code, or {@code null} if the given value was null.
     *
     * @see Charset#name()
     *
     * @since 0.5
     */
    public String toCharsetCode(final MarshalContext context, final Charset value) {
        if (value != null) {
            return LegacyCodes.fromIANA(value.name());
        }
        return null;
    }

    /**
     * Converts the given string to a locale. The string is the language code either as the 2
     * letters or the 3 letters ISO code. It can optionally be followed by the {@code '_'}
     * character and the country code (again either as 2 or 3 letters), optionally followed
     * by {@code '_'} and the variant.
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The string to convert to a locale, or {@code null}.
     * @return The converted locale, or {@code null} if the given value was null or empty, or
     *         if an exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws RuntimeException If the given string can not be converted to a locale
     *         ({@code IllformedLocaleException} on the JDK7 branch).
     *
     * @see Locales#parse(String)
     */
    public Locale toLocale(final MarshalContext context, String value) {
        value = trimWhitespaces(value);
        if (value != null && !value.isEmpty()) try {
            return Locales.parse(value);
        } catch (RuntimeException e) { // IllformedLocaleException on the JDK7 branch.
            if (!exceptionOccured(context, value, String.class, Locale.class, e)) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Converts the given string to a character set. The string can be either a
     * <a href="http://www.iana.org/assignments/character-sets">IANA</a> identifier,
     * or one of the ISO 19115:2003 {@code MD_CharacterSetCode} identifier.
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The string to convert to a character set, or {@code null}.
     * @return The converted character set, or {@code null} if the given value was null or empty, or
     *         if an exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws IllegalCharsetNameException If the given string can not be converted to a character set.
     *
     * @see Charset#forName(String)
     *
     * @since 0.5
     */
    public Charset toCharset(final MarshalContext context, String value) throws IllegalCharsetNameException {
        value = trimWhitespaces(value);
        if (value != null && !value.isEmpty()) {
            value = LegacyCodes.toIANA(value);
            try {
                return Charset.forName(value);
            } catch (IllegalCharsetNameException e) {
                if (!exceptionOccured(context, value, String.class, Charset.class, e)) {
                    throw e;
                }
            }
        }
        return null;
    }

    /**
     * Converts the given string to a unit. The default implementation is as below, omitting
     * the check for null value and the call to {@link #exceptionOccured exceptionOccured(…)}
     * in case of error:
     *
     * {@preformat java
     *     return Units.valueOf(value);
     * }
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The string to convert to a unit, or {@code null}.
     * @return The converted unit, or {@code null} if the given value was null or empty, or
     *         if an exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws IllegalArgumentException If the given string can not be converted to a unit.
     *
     * @see Units#valueOf(String)
     */
    public Unit<?> toUnit(final MarshalContext context, String value) throws IllegalArgumentException {
        value = trimWhitespaces(value);
        if (value != null && !value.isEmpty()) try {
            return Units.valueOf(value);
        } catch (IllegalArgumentException e) {
            if (!exceptionOccured(context, value, String.class, Unit.class, e)) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Converts the given string to a Universal Unique Identifier. The default implementation
     * is as below, omitting the check for null value and the call to {@link #exceptionOccured
     * exceptionOccured(…)} in case of error:
     *
     * {@preformat java
     *     return UUID.fromString(value);
     * }
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The string to convert to a UUID, or {@code null}.
     * @return The converted UUID, or {@code null} if the given value was null or empty, or
     *         if an exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws IllegalArgumentException If the given string can not be converted to a UUID.
     *
     * @see UUID#fromString(String)
     */
    public UUID toUUID(final MarshalContext context, String value) throws IllegalArgumentException {
        value = trimWhitespaces(value);
        if (value != null && !value.isEmpty()) try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            if (!exceptionOccured(context, value, String.class, UUID.class, e)) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Converts the given string to a URI. The default performs the following work
     * (omitting the check for null value and the call to {@link #exceptionOccured
     * exceptionOccured(…)} in case of error):
     *
     * {@preformat java
     *     return new URI(value);
     * }
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The string to convert to a URI, or {@code null}.
     * @return The converted URI, or {@code null} if the given value was null or empty, or if
     *         an exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws URISyntaxException If the given string can not be converted to a URI.
     *
     * @see URI#URI(String)
     */
    public URI toURI(final MarshalContext context, String value) throws URISyntaxException {
        value = trimWhitespaces(value);
        if (value != null && !value.isEmpty()) try {
            return new URI(value);
        } catch (URISyntaxException e) {
            if (!exceptionOccured(context, value, String.class, URI.class, e)) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Converts the given URL to a URI. The default implementation is as below, omitting
     * the check for null value and the call to {@link #exceptionOccured exceptionOccured(…)}
     * in case of error:
     *
     * {@preformat java
     *     return value.toURI();
     * }
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The URL to convert to a URI, or {@code null}.
     * @return The converted URI, or {@code null} if the given value was null or if an
     *         exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws URISyntaxException If the given URL can not be converted to a URI.
     *
     * @see URL#toURI()
     */
    public URI toURI(final MarshalContext context, final URL value) throws URISyntaxException {
        if (value != null) try {
            return value.toURI();
        } catch (URISyntaxException e) {
            if (!exceptionOccured(context, value, URL.class, URI.class, e)) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Converts the given URI to a URL. The default implementation is as below, omitting
     * the check for null value and the call to {@link #exceptionOccured exceptionOccured(…)}
     * in case of error:
     *
     * {@preformat java
     *     return value.toURL();
     * }
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The URI to convert to a URL, or {@code null}.
     * @return The converted URL, or {@code null} if the given value was null or if an
     *         exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws MalformedURLException If the given URI can not be converted to a URL.
     *
     * @see URI#toURL()
     */
    public URL toURL(final MarshalContext context, final URI value) throws MalformedURLException {
        if (value != null) try {
            return value.toURL();
        } catch (Exception e) { // MalformedURLException | IllegalArgumentException
            if (!exceptionOccured(context, value, URI.class, URL.class, e)) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw (MalformedURLException) e;
            }
        }
        return null;
    }

    /**
     * Converts the given string to a {@code NilReason}. The default implementation is as below,
     * omitting the check for null value and the call to {@link #exceptionOccured exceptionOccured(…)}
     * in case of error:
     *
     * {@preformat java
     *     return NilReason.valueOf(value);
     * }
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value The string to convert to a nil reason, or {@code null}.
     * @return The converted nil reason, or {@code null} if the given value was null or empty, or
     *         if an exception was thrown and {@code exceptionOccured(…)} returned {@code true}.
     * @throws URISyntaxException If the given string can not be converted to a nil reason.
     *
     * @see NilReason#valueOf(String)
     */
    public NilReason toNilReason(final MarshalContext context, String value) throws URISyntaxException {
        value = trimWhitespaces(value);
        if (value != null && !value.isEmpty()) try {
            return NilReason.valueOf(value);
        } catch (URISyntaxException e) {
            if (!exceptionOccured(context, value, String.class, URI.class, e)) {
                throw e;
            }
        }
        return null;
    }
}
