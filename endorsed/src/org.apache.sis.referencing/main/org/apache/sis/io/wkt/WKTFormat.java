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
package org.apache.sis.io.wkt;

import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.function.Function;
import java.net.URI;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.Unit;
import org.opengis.util.Factory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.system.Loggers;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.privy.ReferencingFactoryContainer;

// Specific to the main branch:
import org.apache.sis.metadata.iso.DefaultIdentifier;


/**
 * Parser and formatter for <i>Well Known Text</i> (WKT) strings.
 * This format handles a pair of {@link org.apache.sis.io.wkt.Parser} and {@link Formatter},
 * used by the {@code parse(…)} and {@code format(…)} methods respectively.
 * {@code WKTFormat} objects allow the following configuration:
 *
 * <ul>
 *   <li>The preferred authority of {@linkplain IdentifiedObject#getName() object name} to
 *       format (see {@link Formatter#getNameAuthority()} for more information).</li>
 *   <li>The {@linkplain Symbols symbols} to use (curly braces or brackets, <i>etc</i>).</li>
 *   <li>The {@linkplain Transliterator transliterator} to use for replacing Unicode characters by ASCII ones.</li>
 *   <li>Whether ANSI X3.64 colors are allowed or not (default is not).</li>
 *   <li>The indentation.</li>
 * </ul>
 *
 * <h2>String expansion</h2>
 * Because the strings to be parsed by this class are long and tend to contain repetitive substrings,
 * {@code WKTFormat} provides a mechanism for performing string substitutions before the parsing take place.
 * Long strings can be assigned short names by calls to the {@link #addFragment(String, String)} method.
 * After fragments have been added, any call to a parsing method will replace all occurrences (except in
 * quoted text) of tokens like {@code $foo} by the WKT fragment named "foo".
 *
 * <h3>Example</h3>
 * In the example below, the {@code $WGS84} substring which appear in the argument given to the
 * {@code parseObject(…)} method will be expanded into the full {@code GeographicCRS[“WGS84”, …]}
 * string before the parsing proceed.
 *
 * <blockquote><code>
 * {@linkplain #addFragment addFragment}("deg", "AngleUnit[“degree”, 0.0174532925199433]");<br>
 * {@linkplain #addFragment addFragment}("lat", "Axis[“Latitude”, NORTH, <strong>$deg</strong>]");<br>
 * {@linkplain #addFragment addFragment}("lon", "Axis[“Longitude”, EAST, <strong>$deg</strong>]");<br>
 * {@linkplain #addFragment addFragment}("MyBaseCRS", "GeographicCRS[“WGS84”, Datum[</code> <i>…etc…</i> <code>],
 * CS[</code> <i>…etc…</i> <code>], <strong>$lat</strong>, <strong>$lon</strong>]");<br>
 * Object crs = {@linkplain #parseObject(String) parseObject}("ProjectedCRS[“Mercator_1SP”, <strong>$MyBaseCRS</strong>,
 * </code> <i>…etc…</i> <code>]");
 * </code></blockquote>
 *
 * Note that the parsing of WKT fragment does not always produce the same object.
 * In particular, the default linear and angular units depend on the context in which the WKT fragment appears.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li><strong>The WKT format is not lossless!</strong>
 *       Objects formatted by {@code WKTFormat} are not guaranteed to be identical after parsing.
 *       Some metadata may be lost or altered, but the coordinate operations between two CRS should produce
 *       the same numerical results provided that the two CRS were formatted independently
 *       (not as inner element of another WKT).</li>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       It is recommended to create separated format instances for each thread.
 *       If multiple threads access a {@code WKTFormat} concurrently, it must be synchronized externally.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Eve (IRD)
 * @version 1.5
 * @since   0.4
 */
public class WKTFormat extends CompoundFormat<Object> {
    /**
     * The logger for Well Known Text operations.
     */
    private static final Logger LOGGER = Logger.getLogger(Loggers.WKT);

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2909110214650709560L;

    /**
     * The indentation value to give to the {@link #setIndentation(int)}
     * method for formatting the complete object on a single line.
     *
     * @see #getIndentation()
     * @see #setIndentation(int)
     * @see org.apache.sis.setup.OptionKey#INDENTATION
     */
    public static final int SINGLE_LINE = -1;

    /**
     * The {@linkplain Symbols#immutable() immutable} set of symbols to use for this formatter.
     * The same object is also referenced in the {@linkplain #parser} and {@linkplain #formatter}.
     * It appears here for serialization purpose.
     *
     * @see #setSymbols(Symbols)
     */
    private Symbols symbols;

    /**
     * The {@linkplain Colors#immutable() immutable} set of colors to use for this formatter,
     * or {@code null} for no syntax coloring. The default value is {@code null}.
     * The same object is also referenced in the {@linkplain #formatter}.
     * It appears here for serialization purpose.
     *
     * @see #setColors(Colors)
     */
    private Colors colors;

    /**
     * The convention to use. The same object is also referenced in the {@linkplain #formatter}.
     * It appears here for serialization purpose.
     */
    private Convention convention;

    /**
     * The preferred authority for objects or parameter names. A {@code null} value
     * means that the authority shall be inferred from the {@linkplain #convention}.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private Citation authority;

    /**
     * Whether WKT keywords shall be formatted in upper case.
     */
    private KeywordCase keywordCase;

    /**
     * Whether to use short or long WKT keywords.
     */
    private KeywordStyle keywordStyle;

    /**
     * {@link Transliterator#IDENTITY} for preserving non-ASCII characters. The default value is
     * {@link Transliterator#DEFAULT}, which causes replacements like "é" → "e" in all elements
     * except {@code REMARKS["…"]}. May also be a user supplied transliterator.
     *
     * <p>A {@code null} value means to infer this property from the {@linkplain #convention}.</p>
     */
    private Transliterator transliterator;

    /**
     * The number of spaces to use in indentation, or {@value #SINGLE_LINE} if indentation is disabled.
     * The same value is also stored in the {@linkplain #formatter}.
     * It appears here for serialization purpose.
     */
    private byte indentation;

    /**
     * Maximum number of elements to show in lists, or {@link Integer#MAX_VALUE} if unlimited.
     * If a list is longer than this length, only the first and the last elements will be shown.
     * This limit applies in particular to {@link org.opengis.referencing.operation.MathTransform}
     * parameter values of {@code double[]} type, since those parameters may be large interpolation tables.
     *
     * @see #getMaximumListElements()
     */
    private int listSizeLimit;

    /**
     * Identifier to assign to parsed {@link IdentifiedObject} if the WKT does not contain an
     * explicit {@code ID[…]} or {@code AUTHORITY[…]} element. The main use case is for implementing
     * a {@link org.opengis.referencing.crs.CRSAuthorityFactory} backed by definitions in WKT format.
     *
     * <p>This field is transient because this is not yet a public API. The {@code transient}
     * keyword may be removed in a future version if we commit to this API.</p>
     *
     * @see #setDefaultIdentifier(DefaultIdentifier)
     */
    private transient DefaultIdentifier defaultIdentifier;

    /**
     * WKT fragments that can be inserted in longer WKT strings, or {@code null} if none. Keys are short identifiers
     * and values are WKT subtrees to substitute to the identifiers when they are found in a WKT to parse.
     * The same map instance may be shared by different {@linkplain #clone() clones} as long as they are not modified.
     *
     * @see #fragments(boolean)
     */
    private TreeMap<String,StoredTree> fragments;

    /**
     * The URI to declare as the source of the WKT definitions, or {@code null} if unknown.
     * This information is not used directly by the parser, but will be stored in parameter values
     * as a hint for resolving relative paths as absolute paths.
     *
     * @see #getSourceFile()
     */
    private URI sourceFile;

    /**
     * {@code true} if the {@link #fragments} map is shared by two or more {@code WKTFormat} instances.
     * In such case, the map shall not be modified; instead it must be copied before any modification.
     *
     * <h4>Use case</h4>
     * This flag allows to clone the {@link #fragments} map only when first needed. In use cases where
     * {@code WKTFormat} is cloned for multi-threading purposes without change in its configuration,
     * this flag avoids completely the need to clone the {@link #fragments} map.
     *
     * @see #clone()
     * @see #fragments(boolean)
     */
    private transient boolean isCloned;

    /**
     * Temporary map used by {@link #addFragment(String, String)} for reusing existing instances when possible.
     * Keys and values are the same {@link String}, {@link Boolean}, {@link Number} or {@link Date} instances.
     *
     * <p>This reference is set to null when we assume that no more fragments will be added to this format.
     * It is not a problem if this map is destroyed too aggressively, since it will be recreated when needed.
     * The only cost of destroying the map too aggressively is that we may have more instance duplications
     * than what we would otherwise have.</p>
     */
    private transient Map<Object,Object> sharedValues;

    /**
     * A formatter using the same symbols as the {@linkplain #parser}.
     * Will be created by the {@link #format(Object, Appendable)} method when first needed.
     */
    private transient Formatter formatter;

    /**
     * The parser. Will be created when first needed.
     */
    private transient AbstractParser parser;

    /**
     * The factories needed by the parser. Those factories are currently not serialized (because usually not
     * serializable), so any value that users may have specified with {@link #setFactory(Class, Factory)}
     * will be lost at serialization time.
     *
     * @see #factories()
     */
    private transient ReferencingFactoryContainer factories;

    /**
     * The warning produced by the last parsing or formatting operation, or {@code null} if none.
     *
     * @see #getWarnings()
     */
    private transient Warnings warnings;

    /**
     * Creates a format for the root locale and UTC timezone.
     * This is the standard configuration for ISO 19162 Well-Known Text.
     *
     * @since 1.5
     */
    public WKTFormat() {
        this(null, (TimeZone) null);
    }

    /**
     * Creates a format for the given locale and timezone. The given locale will be used for
     * {@link InternationalString} localization, <strong>not</strong> for formatting numbers.
     * The given timezone will be used for parsing and formatting dates in temporal elements
     * such as {@code TimeOrigin[…]}. Note that the specified timezone will not be formatted
     * in WKT elements, as it will be assumed implicit.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone for dates in the WKT temporal elements, or {@code null} for UTC.
     *
     * @see #getLocale()
     * @see #getTimeZone()
     *
     * @since 1.5
     */
    public WKTFormat(final Locale locale, final ZoneId timezone) {
        this(locale, (timezone != null) ? TimeZone.getTimeZone(timezone) : null);
    }

    /**
     * Creates a format for the given locale and legacy timezone.
     * See {@link #WKTFormat(Locale, ZoneId)} for the description.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone for dates in the WKT temporal elements, or {@code null} for UTC.
     */
    public WKTFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        convention    = Convention.DEFAULT;
        symbols       = Symbols.getDefault();
        keywordCase   = KeywordCase.DEFAULT;
        keywordStyle  = KeywordStyle.DEFAULT;
        indentation   = Constants.DEFAULT_INDENTATION;
        listSizeLimit = Integer.MAX_VALUE;
    }

    /**
     * Returns the {@link #fragments} map, creating it when first needed.
     * Caller shall not modify the returned map, unless the {@code modifiable} parameter is {@code true}.
     *
     * @param  modifiable  whether the caller intents to modify the map.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,StoredTree> fragments(final boolean modifiable) {
        if (fragments == null) {
            if (!modifiable) {
                // Most common cases: invoked before to parse a WKT and no fragments specified.
                return Map.of();
            }
            fragments = new TreeMap<>();
            isCloned  = false;
        } else if (isCloned & modifiable) {
            fragments = new TreeMap<>(fragments);
            isCloned  = false;
        }
        return fragments;
    }

    /**
     * Returns the container of {@link #factories}, creating it when first needed.
     * This container is needed at parsing time but not at formatting time.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private ReferencingFactoryContainer factories() {
        if (factories == null) {
            factories = new ReferencingFactoryContainer();
        }
        return factories;
    }

    /**
     * Returns the locale for the given category. This method implements the following mapping:
     *
     * <ul>
     *   <li>{@link java.util.Locale.Category#FORMAT}: the value of {@link Symbols#getLocale()},
     *       normally fixed to {@link Locale#ROOT}, used for number formatting.</li>
     *   <li>{@link java.util.Locale.Category#DISPLAY}: the {@code locale} given at construction time,
     *       used for {@link InternationalString} localization.</li>
     * </ul>
     *
     * @param  category  the category for which a locale is desired.
     * @return the locale for the given category (never {@code null}).
     */
    @Override
    public Locale getLocale(final Locale.Category category) {
        if (category == Locale.Category.FORMAT) {
            return symbols.getLocale();
        }
        return super.getLocale(category);
    }

    /**
     * Returns the locale to use for error messages. Other {@link CompoundFormat} classes use the system default.
     * But this class uses a compromise: not exactly the locale used for {@link InternationalString} because that
     * locale is often fixed to English, and not exactly the system default neither because this "error locale"
     * is also used for warnings. The compromise implemented in this method may change in any future version.
     *
     * @see #errors()
     */
    final Locale getErrorLocale() {
        final Locale locale = getLocale(Locale.Category.DISPLAY);
        return (locale != null && locale != Locale.ROOT) ? locale : Locale.getDefault(Locale.Category.DISPLAY);
    }

    /**
     * Returns the symbols used for parsing and formatting WKT. This method returns an unmodifiable instance.
     * Modifications, if desired, should be applied on a {@linkplain Symbols#clone() clone} of the returned object.
     *
     * @return the current set of symbols used for parsing and formatting WKT.
     */
    public Symbols getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols used for parsing and formatting WKT.
     *
     * @param  style  the new set of symbols to use for parsing and formatting WKT.
     */
    public void setSymbols(final Symbols style) {
        if (!style.equals(symbols)) {               // Intentional NullPointerException.
            symbols = style.immutable();
            formatter = null;
            parser = null;
        }
    }

    /**
     * Returns a mapper between Java character sequences and the characters to write in WKT.
     * The intent is to specify how to write characters that are not allowed in WKT strings
     * according ISO 19162 specification. Return values can be:
     *
     * <ul>
     *   <li>{@link Transliterator#DEFAULT} for performing replacements like "é" → "e"
     *       in all WKT elements except {@code REMARKS["…"]}.</li>
     *   <li>{@link Transliterator#IDENTITY} for preserving non-ASCII characters.</li>
     *   <li>Any other user supplied mapping.</li>
     * </ul>
     *
     * @return the mapper between Java character sequences and the characters to write in WKT.
     *
     * @since 0.6
     */
    public Transliterator getTransliterator() {
        Transliterator result = transliterator;
        if (result == null) {
            result = (convention == Convention.INTERNAL) ? Transliterator.IDENTITY : Transliterator.DEFAULT;
        }
        return result;
    }

    /**
     * Sets the mapper between Java character sequences and the characters to write in WKT.
     *
     * <p>If this method is never invoked, or if this method is invoked with a {@code null} value,
     * then the default mapper is {@link Transliterator#DEFAULT} except for WKT formatted according
     * the {@linkplain Convention#INTERNAL internal convention}.</p>
     *
     * @param  map  the new mapper to use, or {@code null} for restoring the default value.
     *
     * @since 0.6
     */
    public void setTransliterator(final Transliterator map) {
        if (transliterator != map) {
            transliterator = map;
            updateFormatter(formatter);
            parser = null;
        }
    }

    /**
     * Returns whether WKT keywords should be written with upper cases or camel cases.
     *
     * @return the case to use for formatting keywords.
     */
    public KeywordCase getKeywordCase() {
        return keywordCase;
    }

    /**
     * Sets whether WKT keywords should be written with upper cases or camel cases.
     *
     * @param  style  the case to use for formatting keywords.
     */
    public void setKeywordCase(final KeywordCase style) {
        keywordCase = Objects.requireNonNull(style);
        updateFormatter(formatter);
    }

    /**
     * Returns whether to use short or long WKT keywords.
     *
     * @return the style used for formatting keywords.
     *
     * @since 0.6
     */
    public KeywordStyle getKeywordStyle() {
        return keywordStyle;
    }

    /**
     * Sets whether to use short or long WKT keywords.
     *
     * @param  style  the style to use for formatting keywords.
     *
     * @since 0.6
     */
    public void setKeywordStyle(final KeywordStyle style) {
        keywordStyle = Objects.requireNonNull(style);
        updateFormatter(formatter);
    }

    /**
     * Returns the colors to use for syntax coloring, or {@code null} if none.
     * This method returns an unmodifiable instance. Modifications, if desired,
     * should be applied on a {@linkplain Colors#clone() clone} of the returned object.
     * By default there is no syntax coloring.
     *
     * @return the colors for syntax coloring, or {@code null} if none.
     */
    public Colors getColors() {
        return colors;
    }

    /**
     * Sets the colors to use for syntax coloring.
     * This property applies only when formatting text.
     *
     * <p>Newly created {@code WKTFormat}s have no syntax coloring. If a non-null argument like
     * {@link Colors#DEFAULT} is given to this method, then the {@link #format(Object, Appendable) format(…)}
     * method tries to highlight most of the elements that are relevant to
     * {@link org.apache.sis.util.Utilities#equalsIgnoreMetadata(Object, Object)}.</p>
     *
     * @param  emphasis  the colors for syntax coloring, or {@code null} if none.
     */
    public void setColors(Colors emphasis) {
        if (emphasis != null) {
            emphasis = emphasis.immutable();
        }
        colors = emphasis;
        updateFormatter(formatter);
    }

    /**
     * Returns the convention for parsing and formatting WKT elements.
     * The default value is {@link Convention#WKT2}.
     *
     * @return the convention to use for formatting WKT elements (never {@code null}).
     */
    public Convention getConvention() {
        return convention;
    }

    /**
     * Sets the convention for parsing and formatting WKT elements.
     *
     * @param  variant  the new convention to use for parsing and formatting WKT elements.
     */
    public void setConvention(final Convention variant) {
        if (convention != variant) {
            convention = Objects.requireNonNull(variant);
            updateFormatter(formatter);
            parser = null;
        }
    }

    /**
     * Returns the preferred authority to look for when fetching identified object names and identifiers.
     * The difference between various authorities are most easily seen in projection and parameter names.
     * If no authority has been {@linkplain #setNameAuthority(Citation) explicitly set}, then this
     * method returns the default authority for the current {@linkplain #getConvention() convention}.
     *
     * <h4>Example</h4>
     * The following table shows the names given by various organizations or projects for the same projection:
     *
     * <table class="sis">
     *   <caption>Projection name examples</caption>
     *   <tr><th>Authority</th> <th>Projection name</th></tr>
     *   <tr><td>EPSG</td>      <td>Mercator (variant A)</td></tr>
     *   <tr><td>OGC</td>       <td>Mercator_1SP</td></tr>
     *   <tr><td>GEOTIFF</td>   <td>CT_Mercator</td></tr>
     * </table>
     *
     * @return the organization, standard or project to look for when fetching projection and parameter names.
     *
     * @see Formatter#getNameAuthority()
     */
    public Citation getNameAuthority() {
        Citation result = authority;
        if (result == null) {
            result = convention.getNameAuthority();
        }
        return result;
    }

    /**
     * Sets the preferred authority for choosing the projection and parameter names.
     * If non-null, the given priority will have precedence over the authority usually
     * associated to the {@linkplain #getConvention() convention}. A {@code null} value
     * restore the default behavior.
     *
     * @param  authority  the new authority, or {@code null} for inferring it from the convention.
     *
     * @see Formatter#getNameAuthority()
     */
    public void setNameAuthority(final Citation authority) {
        this.authority = authority;
        updateFormatter(formatter);
        // No need to update the parser.
    }

    /**
     * Updates the formatter convention, authority, colors and indentation according the current state of this
     * {@code WKTFormat}. The authority may be null, in which case it will be inferred from the convention when
     * first needed.
     */
    private void updateFormatter(final Formatter formatter) {
        if (formatter != null) {
            final byte toUpperCase;
            switch (keywordCase) {
                case LOWER_CASE: toUpperCase = -1; break;
                case UPPER_CASE: toUpperCase = +1; break;
                case CAMEL_CASE: toUpperCase =  0; break;
                default: toUpperCase = convention.toUpperCase ? (byte) +1 : 0; break;
            }
            final byte longKeywords;
            switch (keywordStyle) {
                case SHORT: longKeywords = -1; break;
                case LONG:  longKeywords = +1; break;
                default:    longKeywords = (convention.majorVersion() == 1) ? (byte) -1 : 0; break;
            }
            formatter.configure(convention, authority, colors, toUpperCase, longKeywords, indentation, listSizeLimit);
            if (transliterator != null) {
                formatter.transliterator = transliterator;
            }
        }
    }

    /**
     * Returns the current indentation to be used for formatting objects.
     * The {@value #SINGLE_LINE} value means that the whole WKT is to be formatted on a single line.
     *
     * @return the current indentation.
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets a new indentation to be used for formatting objects.
     * The {@value #SINGLE_LINE} value means that the whole WKT is to be formatted on a single line.
     *
     * @param  numSpaces  the new indentation to use in number of spaces.
     *
     * @see org.apache.sis.setup.OptionKey#INDENTATION
     */
    public void setIndentation(final int numSpaces) {
        ArgumentChecks.ensureBetween("indentation", SINGLE_LINE, Byte.MAX_VALUE, numSpaces);
        indentation = (byte) numSpaces;
        updateFormatter(formatter);
    }

    /**
     * Returns the maximum number of elements to show in lists of values. If a list length is greater than this limit,
     * then only the first and last elements will be shown together with a message saying that some elements were omitted.
     * This limit is useful in particular with {@link org.opengis.referencing.operation.MathTransform} parameter values of
     * {@code double[]} type, since those parameters may be large interpolation tables.
     *
     * @return the current lists size limit, or {@link Integer#MAX_VALUE} if unlimited.
     *
     * @since 1.0
     */
    public int getMaximumListElements() {
        return listSizeLimit;
    }

    /**
     * Sets a new limit for the number of elements to show in lists.
     * If this method is never invoked, then the default is unlimited.
     *
     * @param  limit  the new lists size limit, or {@link Integer#MAX_VALUE} if unlimited.
     *
     * @since 1.0
     */
    public void setMaximumListElements(final int limit) {
        ArgumentChecks.ensureStrictlyPositive("listSizeLimit", limit);
        listSizeLimit = limit;
        updateFormatter(formatter);
    }

    /**
     * Sets the identifier to assign to parsed {@link IdentifiedObject} if the WKT does not contain an
     * explicit {@code ID[…]} or {@code AUTHORITY[…]} element. The main use case is for implementing
     * a {@link org.opengis.referencing.crs.CRSAuthorityFactory} backed by definitions in WKT format.
     *
     * <p>Note that this identifier applies to all objects to be created, which is generally not desirable.
     * Callers should invoke {@code setDefaultIdentifier(null)} in a {@code finally} block.</p>
     *
     * <p>This is not a publicly committed API. If we want to make this functionality public in a future
     * version, we should investigate if we should make it applicable to a wider range of properties and
     * how to handle the fact that the a given identifier should be used for only one object.</p>
     *
     * @param  identifier  the default identifier, or {@code null} if none.
     */
    final void setDefaultIdentifier(final DefaultIdentifier identifier) {
        defaultIdentifier = identifier;
    }

    /**
     * Verifies if the given type is a valid key for the {@link #factories} map.
     */
    private void ensureValidFactoryType(final Class<?> type) throws IllegalArgumentException {
        if (type != CRSFactory.class            &&
            type != CSFactory.class             &&
            type != DatumFactory.class          &&
            type != MathTransformFactory.class  &&
            type != CoordinateOperationFactory.class)
        {
            ArgumentChecks.ensureNonNull("type", type);
            throw new IllegalArgumentException(errors().getString(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
    }

    /**
     * Returns one of the factories used by this {@code WKTFormat} for parsing WKT.
     * The given {@code type} argument can be one of the following values:
     *
     * <ul>
     *   <li><code>{@linkplain CRSFactory}.class</code></li>
     *   <li><code>{@linkplain CSFactory}.class</code></li>
     *   <li><code>{@linkplain DatumFactory}.class</code></li>
     *   <li><code>{@linkplain MathTransformFactory}.class</code></li>
     *   <li><code>{@linkplain CoordinateOperationFactory}.class</code></li>
     * </ul>
     *
     * @param  <T>   the compile-time type of the {@code type} argument.
     * @param  type  the factory type.
     * @return the factory used by this {@code WKTFormat} for the given type.
     * @throws IllegalArgumentException if the {@code type} argument is not one of the valid values.
     */
    public <T extends Factory> T getFactory(final Class<T> type) {
        ensureValidFactoryType(type);
        return factories().getFactory(type);
    }

    /**
     * Sets one of the factories to be used by this {@code WKTFormat} for parsing WKT.
     * The given {@code type} argument can be one of the following values:
     *
     * <ul>
     *   <li><code>{@linkplain CRSFactory}.class</code></li>
     *   <li><code>{@linkplain CSFactory}.class</code></li>
     *   <li><code>{@linkplain DatumFactory}.class</code></li>
     *   <li><code>{@linkplain MathTransformFactory}.class</code></li>
     *   <li><code>{@linkplain CoordinateOperationFactory}.class</code></li>
     * </ul>
     *
     * <h4>Limitation</h4>
     * The current implementation does not serialize the given factories, because they are usually not
     * {@link java.io.Serializable}. The factories used by {@code WKTFormat} instances after deserialization
     * are the default ones.
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  type     the factory type.
     * @param  factory  the factory to be used by this {@code WKTFormat} for the given type.
     * @throws IllegalArgumentException if the {@code type} argument is not one of the valid values.
     */
    public <T extends Factory> void setFactory(final Class<T> type, final T factory) {
        ensureValidFactoryType(type);
        if (factories().setFactory(type, factory)) {
            parser = null;
        }
    }

    /**
     * Returns the URI to declare as the source of the WKT definitions. This value is not used directly by the parser,
     * but stored as a hint for allowing users to interpret the {@code PARAMETERFILE[…]} value as a file relative to
     * the file containing the WKT. The default value is {@code null}.
     *
     * @return the URI to declare as the source of the WKT definitions, or {@code null} if none.
     *
     * @see #setSourceFile(URI)
     * @see org.apache.sis.parameter.DefaultParameterValue#getSourceFile()
     * @see org.apache.sis.xml.MarshalContext#getDocumentURI()
     * @see URI#resolve(URI)
     *
     * @since 1.5
     */
    public URI getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the URI to declare as the source of the WKT definitions. This information will be stored in
     * {@link org.apache.sis.parameter.DefaultParameterValue#getSourceFile()} at WKT parsing time as a
     * hint for resolving relative paths as absolute paths. This value has no effect at formatting time.
     *
     * @param  document  URI to the file that contains the WKT definitions to parse, or {@code null} if none.
     *
     * @see #getSourceFile()
     * @see org.apache.sis.parameter.DefaultParameterValue#setSourceFile(URI)
     * @see URI#resolve(URI)
     *
     * @since 1.5
     */
    public void setSourceFile(final URI document) {
        if (!Objects.equals(sourceFile, document)) {
            sourceFile = document;
            parser = null;
        }
    }

    /**
     * Returns the type of objects formatted by this class. This method has to return {@code Object.class}
     * because it is the only common parent to all object types accepted by this formatter.
     *
     * @return {@code Object.class}
     */
    @Override
    public final Class<Object> getValueType() {
        return Object.class;
    }

    /**
     * Returns the name of all WKT fragments known to this {@code WKTFormat}.
     * The returned collection is initially empty.
     * WKT fragments can be added by call to {@link #addFragment(String, String)}.
     *
     * <p>The returned collection is modifiable. In particular, a call to {@link Set#clear()}
     * removes all fragments from this {@code WKTFormat}.</p>
     *
     * @return the name of all fragments known to this {@code WKTFormat}.
     */
    public Set<String> getFragmentNames() {
        return fragments(true).keySet();
    }

    /**
     * Adds a fragment of Well Know Text (WKT). The {@code wkt} argument given to this method
     * can contains itself other fragments specified in some previous calls to this method.
     *
     * <h4>Example</h4>
     * If the following method is invoked:
     *
     * {@snippet lang="java" :
     *     addFragment("MyEllipsoid", "Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128, ID[“EPSG”,“7004”]]");
     *     }
     *
     * Then other WKT strings parsed by this {@code WKTFormat} instance can refer to the above fragment as below
     * (WKT after the ellipsoid omitted for brevity):
     *
     * {@snippet lang="java" :
     *     Object crs = parseObject("GeographicCRS[“Tokyo”, Datum[“Tokyo”, $MyEllipsoid], …]");
     *     }
     *
     * For removing a fragment, use <code>{@linkplain #getFragmentNames()}.remove(name)</code>.
     *
     * @param  name  the name to assign to the WKT fragment (case-sensitive). Must be a valid Unicode identifier.
     * @param  wkt   the Well Know Text (WKT) fragment represented by the given identifier.
     * @throws IllegalArgumentException if the given name is not a valid Unicode identifier
     *         or if a fragment is already associated to that name.
     * @throws ParseException if an error occurred while parsing the given WKT.
     */
    public void addFragment(final String name, final String wkt) throws IllegalArgumentException, ParseException {
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        ArgumentChecks.ensureNonEmpty("name", name);
        if (!CharSequences.isUnicodeIdentifier(name)) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.NotAUnicodeIdentifier_1, name));
        }
        final ParsePosition pos = new ParsePosition(0);
        final StoredTree definition = textToTree(wkt, pos, name);
        final int length = wkt.length();
        final int index = CharSequences.skipLeadingWhitespaces(wkt, pos.getIndex(), length);
        if (index < length) {
            throw new UnparsableObjectException(getErrorLocale(), Errors.Keys.UnexpectedCharactersAfter_2,
                    new Object[] {name + " = " + definition.keyword() + "[…]", CharSequences.token(wkt, index)}, index);
        }
        addFragment(name, definition);
        logWarnings(WKTFormat.class, "addFragment");
    }

    /**
     * Adds a fragment of Well Know Text (WKT).
     * Caller must have verified that {@code name} is a valid Unicode identifier.
     *
     * @param  name        the Unicode identifier to assign to the WKT fragment.
     * @param  definition  root of the WKT fragment to add.
     * @throws IllegalArgumentException if a fragment is already associated to the given name.
     */
    final void addFragment(final String name, final StoredTree definition) {
        if (fragments(true).putIfAbsent(name, definition) != null) {
            throw new IllegalArgumentException(errors().getString(Errors.Keys.ElementAlreadyPresent_1, name));
        }
    }

    /**
     * Parses a Well Know Text (WKT) for a fragment or an entire object definition.
     * This method should be invoked only for WKT trees to be stored for a long time.
     * It should not be invoked for immediate {@link IdentifiedObject} parsing.
     *
     * <p>If {@code aliasKey} is non-null, this method may return a multi-roots tree.
     * See {@link StoredTree#root} for a discussion. Note that in both cases (single
     * root or multi-roots), we may have some unparsed characters at the end of the string.</p>
     *
     * @param  wkt       the Well Know Text (WKT) fragment to parse.
     * @param  pos       index of the first character to parse (on input) or after last parsed character (on output).
     * @param  aliasKey  key of the alias, or {@code null} if this method is not invoked
     *                   for defining a {@linkplain #addFragment(String, String) fragment}.
     * @return root of the tree of elements.
     */
    final StoredTree textToTree(final String wkt, final ParsePosition pos, final String aliasKey) throws ParseException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final AbstractParser parser  = parser(true);
        final List<Element>  results = new ArrayList<>(4);
        warnings = null;            // Do not invoke `clear()` because we do not want to clear `sharedValues` map.
        try {
            for (;;) {
                results.add(parser.textToTree(wkt, pos));
                if (aliasKey == null) break;
                /*
                 * If we find a separator (usually a coma), search for another element. Contrarily to equivalent
                 * loop in `Element(AbstractParser, …)` constructor, we do not parse number or dates because we
                 * do not have a way as reliable as above-cited constructor to differentiate the kind of value.
                 */
                final int p = CharSequences.skipLeadingWhitespaces(wkt, pos.getIndex(), wkt.length());
                final String separator = parser.symbols.trimmedSeparator();
                if (!wkt.startsWith(separator, p)) break;
                pos.setIndex(p + separator.length());
            }
        } finally {
            // Invoked as a matter of principle, but no warning is expected at this stage.
            warnings = parser.getAndClearWarnings(results.isEmpty() ? null : results.get(0));
        }
        if (sharedValues == null) {
            sharedValues = new HashMap<>();
        }
        if (results.size() == 1) {
            return new StoredTree(results.get(0), sharedValues);      // Standard case.
        } else {
            return new StoredTree(results, sharedValues);             // Anonymous wrapper around multi-roots.
        }
    }

    /**
     * Clears warnings and cache of shared values.
     */
    final void clear() {
        warnings = null;
        sharedValues = null;
    }

    /**
     * Creates an object from the given character sequence.
     * The parsing begins at the index given by the {@code pos} argument.
     * After successful parsing, {@link ParsePosition#getIndex()} gives the position after the last parsed character.
     * In case of error, {@link ParseException#getErrorOffset()} gives the position of the first illegal character.
     *
     * @param  wkt  the character sequence for the object to parse.
     * @param  pos  index of the first character to parse (on input) or after last parsed character (on output).
     * @return the parsed object (never {@code null}).
     * @throws ParseException if an error occurred while parsing the WKT.
     */
    @Override
    public Object parse(final CharSequence wkt, final ParsePosition pos) throws ParseException {
        clear();
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        ArgumentChecks.ensureNonNull ("pos", pos);

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final AbstractParser parser = parser(false);
        Object result = null;
        try {
            result = parser.createFromWKT(wkt.toString(), pos);
        } finally {
            warnings = parser.getAndClearWarnings(result);
        }
        return result;
    }

    /**
     * Parses a tree of {@link Element}s to produce a geodetic object. The {@code tree} argument
     * should be a value returned by {@link #textToTree(String, ParsePosition, String)}.
     * This method is for {@link WKTDictionary#createObject(String)} usage.
     *
     * @param  tree  the tree of WKT elements.
     * @return the parsed object (never {@code null}).
     * @throws ParseException if the tree cannot be parsed.
     */
    final Object buildFromTree(StoredTree tree) throws ParseException {
        clear();
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final AbstractParser parser = parser(false);
        parser.ignoredElements.clear();
        final SingletonElement singleton = new SingletonElement();
        tree.toElements(parser, singleton, 0);
        final Element root = new Element(singleton.value);
        Object result = null;
        try {
            result = parser.buildFromTree(root);
            root.close(parser.ignoredElements);
        } finally {
            warnings = parser.getAndClearWarnings(result);
        }
        return result;
    }

    /**
     * Returns the parser, created when first needed.
     *
     * @param  modifiable  whether the caller intents to modify the {@link #fragments} map.
     */
    private AbstractParser parser(final boolean modifiable) {
        /*
         * `parser` is always null on a fresh clone. However, the `fragments`
         * map may need to be cloned if the caller intents to modify it.
         */
        if (parser == null || (isCloned & modifiable)) {
            parser = new Parser(sourceFile, fragments(modifiable), symbols,
                    (NumberFormat) getFormat(Number.class),
                    (DateFormat)   getFormat(Date.class),
                    (UnitFormat)   getFormat(Unit.class),
                    convention,
                    (transliterator != null) ? transliterator : Transliterator.DEFAULT,
                    getErrorLocale(),
                    factories());
        }
        return parser;
    }

    /**
     * The parser created by {@link #parser(boolean)}, identical to {@link GeodeticObjectParser} except
     * for the source of logging messages which is the enclosing {@code WKTParser} instead of a factory.
     * Also provides a mechanism for adding default identifier to root {@link IdentifiedObject}.
     */
    private final class Parser extends GeodeticObjectParser implements Function<Object,Object> {
        /** Creates a new parser. */
        Parser(final URI sourceFile, final Map<String,StoredTree> fragments, final Symbols symbols,
               final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
               final Convention convention, final Transliterator transliterator, final Locale errorLocale,
               final ReferencingFactoryContainer factories)
        {
            super(sourceFile, fragments, symbols, numberFormat, dateFormat, unitFormat, convention, transliterator, errorLocale, factories);
        }

        /** Returns the source class and method to declare in log records. */
        @Override String getPublicFacade() {return WKTFormat.class.getName();}
        @Override String getFacadeMethod() {return "parse";}

        /** Invoked when an identifier need to be supplied to root {@link IdentifiedObject}. */
        @Override public Object apply(Object key) {
            return new ImmutableIdentifier(defaultIdentifier.getAuthority(), defaultIdentifier.getCodeSpace(),
                    defaultIdentifier.getCode(), defaultIdentifier.getVersion(), defaultIdentifier.getDescription());
        }

        /** Invoked when a root {@link IdentifiedObject} is about to be created. */
        @Override void completeRoot(final Map<String,Object> properties) {
            if (defaultIdentifier != null) {
                properties.computeIfAbsent(IdentifiedObject.IDENTIFIERS_KEY, this);
            }
        }
    }

    /**
     * Formats the specified object as a Well Know Text. The formatter accepts at least the following types:
     * {@link FormattableObject}, {@link IdentifiedObject},
     * {@link org.opengis.referencing.operation.MathTransform},
     * {@link org.opengis.metadata.extent.GeographicBoundingBox},
     * {@link org.opengis.metadata.extent.VerticalExtent},
     * {@link org.opengis.metadata.extent.TemporalExtent},
     * {@link org.opengis.geometry.Envelope},
     * {@link org.opengis.geometry.DirectPosition}
     * and {@link Unit}.
     *
     * @param  object      the object to format.
     * @param  toAppendTo  where the text is to be appended.
     * @throws IOException if an error occurred while writing to {@code toAppendTo}.
     *
     * @see FormattableObject#toWKT()
     */
    @Override
    public void format(final Object object, final Appendable toAppendTo) throws IOException {
        clear();
        ArgumentChecks.ensureNonNull("object",     object);
        ArgumentChecks.ensureNonNull("toAppendTo", toAppendTo);
        /*
         * If the given Appendable is not a StringBuffer, creates a temporary StringBuffer.
         * We cannot write directly in an arbitrary Appendable because Formatter needs the
         * ability to go backward ("append only" is not sufficient), and because it passes
         * the buffer to other java.text.Format instances which work only with StringBuffer.
         */
        final StringBuffer buffer;
        if (toAppendTo instanceof StringBuffer) {
            buffer = (StringBuffer) toAppendTo;
        } else {
            buffer = new StringBuffer(500);
        }
        /*
         * Creates the Formatter when first needed.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        Formatter formatter = this.formatter;
        if (formatter == null) {
            formatter = new Formatter(getLocale(), getErrorLocale(), symbols,
                    (NumberFormat) getFormat(Number.class),
                    (DateFormat)   getFormat(Date.class),
                    (UnitFormat)   getFormat(Unit.class));
            updateFormatter(formatter);
            this.formatter = formatter;
        }
        /*
         * Since each operation on `buffer` is synchronized, this synchronization block allow the lock
         * to be obtained only one time instead of many times for each `StringBuffer` method invoked.
         * As a bonus, it ensures a consistent result if the given `StringBuffer` is used concurrently.
         */
        synchronized (buffer) {
            final boolean valid;
            try {
                formatter.setBuffer(buffer);
                valid = formatter.appendElement(object) || formatter.appendValue(object);
            } finally {
                warnings = formatter.getWarnings();     // Must be saved before formatter.clear() is invoked.
                formatter.setBuffer(null);
                formatter.clear();
            }
            if (warnings != null) {
                warnings.setRoot(object);
            }
            if (!valid) {
                throw new ClassCastException(errors().getString(
                        Errors.Keys.IllegalArgumentClass_2, "object", object.getClass()));
            }
            if (buffer != toAppendTo) {
                toAppendTo.append(buffer);
            }
        }
    }

    /**
     * Creates a new format to use for parsing and formatting values of the given type.
     * This method is invoked the first time that a format is needed for the given type.
     * The {@code valueType} can be any types declared in the
     * {@linkplain CompoundFormat#createFormat(Class) parent class}.
     *
     * @param  valueType  the base type of values to parse or format.
     * @return the format to use for parsing of formatting values of the given type, or {@code null} if none.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        if (valueType == Number.class) {
            return symbols.createNumberFormat();
        }
        if (valueType == Date.class) {
            return new LenientDateFormat(symbols.getLocale(), getTimeZone());
        }
        final Format format = super.createFormat(valueType);
        if (format instanceof UnitFormat) {
            ((UnitFormat) format).setStyle(UnitFormat.Style.NAME);
        }
        return format;
    }

    /**
     * If warnings occurred during the last WKT {@linkplain #parse(CharSequence, ParsePosition) parsing} or
     * {@linkplain #format(Object, Appendable) formatting}, returns the warnings. Otherwise returns {@code null}.
     * The warnings are cleared every time a new object is parsed or formatted.
     *
     * @return the warnings of the last parsing of formatting operation, or {@code null} if none.
     *
     * @since 0.6
     */
    @OptionalCandidate
    public Warnings getWarnings() {
        if (warnings != null) {
            warnings.publish();
        }
        return warnings;
    }

    /**
     * If a warning occurred, logs it. This method is invoked when a WKT was parsed or formatted
     * in another context than a call to a {@code parse(…)} or {@code format(…)} method.
     * For example it may be during the build of {@link WKTDictionary}.
     *
     * @param  classe  the class to report as the source of the logging message.
     * @param  method  the method to report as the source of the logging message.
     */
    final void logWarnings(final Class<?> classe, final String method) {
        if (warnings != null) {
            /*
             * We can avoid the call to `Warnings.publish()` because we know that we are not keeping a
             * reference for long, so we do not need to copy the `AbstractParser.ignoredElements` map.
             */
            final LogRecord record = new LogRecord(Level.WARNING, warnings.toString());
            Logging.completeAndLog(LOGGER, classe, method, record);
        }
    }

    /**
     * Convenience methods for resources for error message in the locale given by {@link #getLocale()}.
     */
    final Errors errors() {
        return Errors.forLocale(getErrorLocale());
    }

    /**
     * Returns a clone of this format. The clone has the same configuration (including any added
     * {@linkplain #addFragment fragments}), except the {@linkplain #getWarnings() warnings}.
     *
     * @return a clone of this format.
     */
    @Override
    public WKTFormat clone() {
        final WKTFormat clone = (WKTFormat) super.clone();
        clone.clear();
        clone.factories = null;                             // Not thread-safe; clone needs its own.
        clone.formatter = null;                             // Do not share the formatter.
        clone.parser    = null;
        clone.isCloned  = isCloned = true;
        // Symbols and Colors do not need to be cloned because they are flagged as immutable.
        return clone;
    }
}
