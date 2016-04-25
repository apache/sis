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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
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
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.internal.util.LocalizedParseException;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Parser and formatter for <cite>Well Known Text</cite> (WKT) strings.
 * This format handles a pair of {@link Parser} and {@link Formatter},
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
 * <div class="section">String expansion</div>
 * Because the strings to be parsed by this class are long and tend to contain repetitive substrings,
 * {@code WKTFormat} provides a mechanism for performing string substitutions before the parsing take place.
 * Long strings can be assigned short names by calls to the {@link #addFragment(String, String)} method.
 * After fragments have been added, any call to a parsing method will replace all occurrences (except in
 * quoted text) of tokens like {@code $foo} by the WKT fragment named "foo".
 *
 * <div class="note"><b>Example:</b>
 * In the example below, the {@code $WGS84} substring which appear in the argument given to the
 * {@code parseObject(…)} method will be expanded into the full {@code GeodeticCRS[“WGS84”, …]}
 * string before the parsing proceed.
 *
 * <blockquote><code>
 * {@linkplain #addFragment addFragment}("deg", "AngleUnit[“degree”, 0.0174532925199433]");<br>
 * {@linkplain #addFragment addFragment}("lat", "Axis[“Latitude”, NORTH, <strong>$deg</strong>]");<br>
 * {@linkplain #addFragment addFragment}("lon", "Axis[“Longitude”, EAST, <strong>$deg</strong>]");<br>
 * {@linkplain #addFragment addFragment}("MyBaseCRS", "GeodeticCRS[“WGS84”, Datum[</code> <i>…etc…</i> <code>],
 * CS[</code> <i>…etc…</i> <code>], <strong>$lat</strong>, <strong>$lon</strong>]");<br>
 * Object crs = {@linkplain #parseObject(String) parseObject}("ProjectedCRS[“Mercator_1SP”, <strong>$MyBaseCRS</strong>,
 * </code> <i>…etc…</i> <code>]");
 * </code></blockquote>
 *
 * Note that the parsing of WKT fragment does not always produce the same object.
 * In particular, the default linear and angular units depend on the context in which the WKT fragment appears.
 * </div>
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li><strong>The WKT format is not lossless!</strong>
 *       Objects formatted by {@code WKTFormat} are not guaranteed to be identical after parsing.
 *       Some metadata may be lost or altered, but the coordinate operations between two CRS should produce
 *       the same numerical results provided that the two CRS were formatted independently (do not rely on
 *       {@link org.opengis.referencing.crs.GeneralDerivedCRS#getConversionFromBase()} for instance).</li>
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
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
 * @see <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Legacy WKT 1</a>
 */
public class WKTFormat extends CompoundFormat<Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2909110214650709560L;

    /**
     * The indentation value to give to the {@link #setIndentation(int)}
     * method for formatting the complete object on a single line.
     */
    public static final int SINGLE_LINE = -1;

    /**
     * The default indentation value.
     */
    static final byte DEFAULT_INDENTATION = 2;

    /**
     * The symbols to use for this formatter.
     * The same object is also referenced in the {@linkplain #parser} and {@linkplain #formatter}.
     * It appears here for serialization purpose.
     */
    private Symbols symbols;

    /**
     * The colors to use for this formatter, or {@code null} for no syntax coloring.
     * The same object is also referenced in the {@linkplain #formatter}.
     * It appears here for serialization purpose.
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
     * except {@code REMARKS["…"]}. May also be a user-supplied transliterator.
     *
     * <p>A {@code null} value means to infer this property from the {@linkplain #convention}.</p>
     */
    private Transliterator transliterator;

    /**
     * The amount of spaces to use in indentation, or {@value #SINGLE_LINE} if indentation is disabled.
     * The same value is also stored in the {@linkplain #formatter}.
     * It appears here for serialization purpose.
     */
    private byte indentation;

    /**
     * WKT fragments that can be inserted in longer WKT strings, or {@code null} if none. Keys are short identifiers
     * and values are WKT subtrees to substitute to the identifiers when they are found in a WKT to parse.
     *
     * @see #fragments()
     */
    private Map<String,Element> fragments;

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
     * A formatter using the same symbols than the {@linkplain #parser}.
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
    private transient Map<Class<?>,Factory> factories;

    /**
     * The warning produced by the last parsing or formatting operation, or {@code null} if none.
     *
     * @see #getWarnings()
     */
    private transient Warnings warnings;

    /**
     * Creates a format for the given locale and timezone. The given locale will be used for
     * {@link InternationalString} localization; this is <strong>not</strong> the locale for number format.
     *
     * @param locale   The locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param timezone The timezone, or {@code null} for UTC.
     */
    public WKTFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        convention   = Convention.DEFAULT;
        symbols      = Symbols.getDefault();
        keywordCase  = KeywordCase.DEFAULT;
        keywordStyle = KeywordStyle.DEFAULT;
        indentation  = DEFAULT_INDENTATION;
    }

    /**
     * Returns the {@link #fragments} map, creating it when first needed.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Element> fragments() {
        if (fragments == null) {
            fragments = new TreeMap<String,Element>();
        }
        return fragments;
    }

    /**
     * Returns the {@link #factories} map, creating it when first needed.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<Class<?>,Factory> factories() {
        if (factories == null) {
            factories = new HashMap<Class<?>,Factory>(8);
        }
        return factories;
    }

    /**
     * Returns the symbols used for parsing and formatting WKT.
     *
     * @return The current set of symbols used for parsing and formatting WKT.
     */
    public Symbols getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols used for parsing and formatting WKT.
     *
     * @param symbols The new set of symbols to use for parsing and formatting WKT.
     */
    public void setSymbols(final Symbols symbols) {
        ArgumentChecks.ensureNonNull("symbols", symbols);
        if (!symbols.equals(this.symbols)) {
            this.symbols = symbols.immutable();
            formatter = null;
            parser = null;
        }
    }

    /**
     * Returns a mapper between Java character sequences and the characters to write in WKT.
     * The intend is to specify how to write characters that are not allowed in WKT strings
     * according ISO 19162 specification. Return values can be:
     *
     * <ul>
     *   <li>{@link Transliterator#DEFAULT} for performing replacements like "é" → "e"
     *       in all WKT elements except {@code REMARKS["…"]}.</li>
     *   <li>{@link Transliterator#IDENTITY} for preserving non-ASCII characters.</li>
     *   <li>Any other user-supplied mapping.</li>
     * </ul>
     *
     * @return The mapper between Java character sequences and the characters to write in WKT.
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
     * @param transliterator The new mapper to use, or {@code null} for restoring the default value.
     *
     * @since 0.6
     */
    public void setTransliterator(final Transliterator transliterator) {
        if (this.transliterator != transliterator) {
            this.transliterator = transliterator;
            updateFormatter(formatter);
            parser = null;
        }
    }

    /**
     * Returns whether WKT keywords should be written with upper cases or camel cases.
     *
     * @return The case to use for formatting keywords.
     */
    public KeywordCase getKeywordCase() {
        return keywordCase;
    }

    /**
     * Sets whether WKT keywords should be written with upper cases or camel cases.
     *
     * @param keywordCase The case to use for formatting keywords.
     */
    public void setKeywordCase(final KeywordCase keywordCase) {
        ArgumentChecks.ensureNonNull("keywordCase", keywordCase);
        this.keywordCase = keywordCase;
        updateFormatter(formatter);
    }

    /**
     * Returns whether to use short or long WKT keywords.
     *
     * @return The style used for formatting keywords.
     *
     * @since 0.6
     */
    public KeywordStyle getKeywordStyle() {
        return keywordStyle;
    }

    /**
     * Sets whether to use short or long WKT keywords.
     *
     * @param keywordStyle The style to use for formatting keywords.
     *
     * @since 0.6
     */
    public void setKeywordStyle(final KeywordStyle keywordStyle) {
        ArgumentChecks.ensureNonNull("keywordStyle", keywordStyle);
        this.keywordStyle = keywordStyle;
        updateFormatter(formatter);
    }

    /**
     * Returns the colors to use for syntax coloring, or {@code null} if none.
     * By default there is no syntax coloring.
     *
     * @return The colors for syntax coloring, or {@code null} if none.
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
     * @param colors The colors for syntax coloring, or {@code null} if none.
     */
    public void setColors(Colors colors) {
        if (colors != null) {
            colors = colors.immutable();
        }
        this.colors = colors;
        updateFormatter(formatter);
    }

    /**
     * Returns the convention for parsing and formatting WKT elements.
     * The default value is {@link Convention#WKT2}.
     *
     * @return The convention to use for formatting WKT elements (never {@code null}).
     */
    public Convention getConvention() {
        return convention;
    }

    /**
     * Sets the convention for parsing and formatting WKT elements.
     *
     * @param convention The new convention to use for parsing and formatting WKT elements.
     */
    public void setConvention(final Convention convention) {
        ArgumentChecks.ensureNonNull("convention", convention);
        if (this.convention != convention) {
            this.convention = convention;
            updateFormatter(formatter);
            parser = null;
        }
    }

    /**
     * Returns the preferred authority to look for when fetching identified object names and identifiers.
     * The difference between various authorities are most easily seen in projection and parameter names.
     *
     * <div class="note"><b>Example:</b>
     * The following table shows the names given by various organizations or projects for the same projection:
     *
     * <table class="sis">
     *   <caption>Projection name examples</caption>
     *   <tr><th>Authority</th> <th>Projection name</th></tr>
     *   <tr><td>EPSG</td>      <td>Mercator (variant A)</td></tr>
     *   <tr><td>OGC</td>       <td>Mercator_1SP</td></tr>
     *   <tr><td>GEOTIFF</td>   <td>CT_Mercator</td></tr>
     * </table></div>
     *
     * If no authority has been {@linkplain #setNameAuthority(Citation) explicitly set}, then this
     * method returns the default authority for the current {@linkplain #getConvention() convention}.
     *
     * @return The organization, standard or project to look for when fetching projection and parameter names.
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
     * @param authority The new authority, or {@code null} for inferring it from the convention.
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
            formatter.configure(convention, authority, colors, toUpperCase, longKeywords, indentation);
            if (transliterator != null) {
                formatter.transliterator = transliterator;
            }
        }
    }

    /**
     * Returns the current indentation to be used for formatting objects.
     * The {@value #SINGLE_LINE} value means that the whole WKT is to be formatted on a single line.
     *
     * @return The current indentation.
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets a new indentation to be used for formatting objects.
     * The {@value #SINGLE_LINE} value means that the whole WKT is to be formatted on a single line.
     *
     * @param indentation The new indentation to use.
     */
    public void setIndentation(final int indentation) {
        ArgumentChecks.ensureBetween("indentation", SINGLE_LINE, Byte.MAX_VALUE, indentation);
        this.indentation = (byte) indentation;
        updateFormatter(formatter);
    }

    /**
     * Verifies if the given type is a valid key for the {@link #factories} map.
     */
    private void ensureValidFactoryType(final Class<?> type) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("type", type);
        if (type != CRSFactory.class            &&
            type != CSFactory.class             &&
            type != DatumFactory.class          &&
            type != MathTransformFactory.class  &&
            type != CoordinateOperationFactory.class)
        {
            throw new IllegalArgumentException(Errors.getResources(getLocale())
                    .getString(Errors.Keys.IllegalArgumentValue_2, "type", type));
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
     * @param  <T>  The compile-time type of the {@code type} argument.
     * @param  type The factory type.
     * @return The factory used by this {@code WKTFormat} for the given type.
     * @throws IllegalArgumentException if the {@code type} argument is not one of the valid values.
     */
    public <T extends Factory> T getFactory(final Class<T> type) {
        ensureValidFactoryType(type);
        if (type == CoordinateOperationFactory.class) {
            /*
             * HACK: we have a special way to get the CoordinateOperationFactory because of its dependency
             * toward MathTransformFactory.  A lazy (but costly) way to ensure a consistent behavior is to
             * let the GeodeticObjectParser constructor do its job.  This is costly, but should not happen
             * often.
             */
            parser();
        }
        return GeodeticObjectParser.getFactory(type, factories());
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
     * <div class="section">Limitation</div>
     * The current implementation does not serialize the given factories, because they are usually not
     * {@link java.io.Serializable}. The factories used by {@code WKTFormat} instances after deserialization
     * are the default ones.
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  type    The factory type.
     * @param  factory The factory to be used by this {@code WKTFormat} for the given type.
     * @throws IllegalArgumentException if the {@code type} argument is not one of the valid values.
     */
    public <T extends Factory> void setFactory(final Class<T> type, final T factory) {
        ensureValidFactoryType(type);
        if (factories().put(type, factory) != factory) {
            parser = null;
        }
    }

    /**
     * Returns the type of objects formatted by this class. This method has to return {@code Object.class}
     * since it is the only common parent to all object types accepted by this formatter.
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
     * @return The name of all fragments known to this {@code WKTFormat}.
     */
    public Set<String> getFragmentNames() {
        return fragments().keySet();
    }

    /**
     * Adds a fragment of Well Know Text (WKT). The {@code wkt} argument given to this method
     * can contains itself other fragments specified in some previous calls to this method.
     *
     * <div class="note"><b>Example</b>
     * if the following method is invoked:
     *
     * {@preformat java
     *   addFragment("MyEllipsoid", "Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128, ID[“EPSG”,“7004”]]");
     * }
     *
     * Then other WKT strings parsed by this {@code WKTFormat} instance can refer to the above fragment as below
     * (WKT after the ellipsoid omitted for brevity):
     *
     * {@preformat java
     *   Object crs = parseObject("GeodeticCRS[“Tokyo”, Datum[“Tokyo”, $MyEllipsoid], …]");
     * }
     * </div>
     *
     * For removing a fragment, use <code>{@linkplain #getFragmentNames()}.remove(name)</code>.
     *
     * @param  name The name to assign to the WKT fragment. Identifiers are case-sensitive.
     * @param  wkt The Well Know Text (WKT) fragment represented by the given identifier.
     * @throws IllegalArgumentException if the name is invalid or if a fragment is already present for that name.
     * @throws ParseException If an error occurred while parsing the given WKT.
     */
    public void addFragment(final String name, final String wkt) throws IllegalArgumentException, ParseException {
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        ArgumentChecks.ensureNonEmpty("name", name);
        short error = Errors.Keys.NotAUnicodeIdentifier_1;
        if (CharSequences.isUnicodeIdentifier(name)) {
            if (sharedValues == null) {
                sharedValues = new HashMap<Object,Object>();
            }
            final ParsePosition pos = new ParsePosition(0);
            final Element element = new Element(parser(), wkt, pos, sharedValues);
            final int index = CharSequences.skipLeadingWhitespaces(wkt, pos.getIndex(), wkt.length());
            if (index < wkt.length()) {
                throw new LocalizedParseException(getLocale(), Errors.Keys.UnexpectedCharactersAfter_2,
                        new Object[] {name + " = " + element.keyword + "[…]", CharSequences.token(wkt, index)}, index);
            }
            // 'fragments' map has been created by 'parser()'.
            if (JDK8.putIfAbsent(fragments, name, element) == null) {
                return;
            }
            error = Errors.Keys.ElementAlreadyPresent_1;
        }
        throw new IllegalArgumentException(Errors.getResources(getLocale()).getString(error, name));
    }

    /**
     * Creates an object from the given character sequence.
     * The parsing begins at the index given by the {@code pos} argument.
     *
     * @param  wkt The character sequence for the object to parse.
     * @param  pos The position where to start the parsing.
     * @return The parsed object.
     * @throws ParseException If an error occurred while parsing the WKT.
     */
    @Override
    public Object parse(final CharSequence wkt, final ParsePosition pos) throws ParseException {
        warnings = null;
        sharedValues = null;
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        ArgumentChecks.ensureNonNull ("pos", pos);
        final AbstractParser parser = parser();
        Object object = null;
        try {
            return object = parser.parseObject(wkt.toString(), pos);
        } finally {
            warnings = parser.getAndClearWarnings(object);
        }
    }

    /**
     * Returns the parser, created when first needed.
     */
    private AbstractParser parser() {
        AbstractParser parser = this.parser;
        if (parser == null) {
            this.parser = parser = new Parser(symbols, fragments(),
                    (NumberFormat) getFormat(Number.class),
                    (DateFormat)   getFormat(Date.class),
                    (UnitFormat)   getFormat(Unit.class),
                    convention,
                    (transliterator != null) ? transliterator : Transliterator.DEFAULT,
                    getLocale(),
                    factories());
        }
        return parser;
    }

    /**
     * The parser created by {@link #parser()}, identical to {@link GeodeticObjectParser} except for
     * the source of logging messages which is the enclosing {@code WKTParser} instead than a factory.
     */
    private static final class Parser extends GeodeticObjectParser {
        Parser(final Symbols symbols, final Map<String,Element> fragments,
                final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
                final Convention convention, final Transliterator transliterator, final Locale errorLocale,
                final Map<Class<?>,Factory> factories)
        {
            super(symbols, fragments, numberFormat, dateFormat, unitFormat, convention, transliterator, errorLocale, factories);
        }

        @Override String getPublicFacade() {return WKTFormat.class.getName();}
        @Override String getFacadeMethod() {return "parse";}
    }

    /**
     * Formats the specified object as a Well Know Text. The formatter accepts at least the following types:
     * {@link FormattableObject}, {@link IdentifiedObject},
     * {@link org.opengis.referencing.operation.MathTransform},
     * {@link org.opengis.metadata.extent.GeographicBoundingBox},
     * {@link org.opengis.metadata.extent.VerticalExtent},
     * {@link org.opengis.metadata.extent.TemporalExtent}
     * and {@link Unit}.
     *
     * @param  object     The object to format.
     * @param  toAppendTo Where the text is to be appended.
     * @throws IOException If an error occurred while writing to {@code toAppendTo}.
     *
     * @see FormattableObject#toWKT()
     */
    @Override
    public void format(final Object object, final Appendable toAppendTo) throws IOException {
        warnings = null;
        ArgumentChecks.ensureNonNull("object",     object);
        ArgumentChecks.ensureNonNull("toAppendTo", toAppendTo);
        /*
         * If the given Appendable is not a StringBuffer, creates a temporary StringBuffer.
         * We can not write directly in an arbitrary Appendable because Formatter needs the
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
        Formatter formatter = this.formatter;
        if (formatter == null) {
            formatter = new Formatter(getLocale(), symbols,
                    (NumberFormat) getFormat(Number.class),
                    (DateFormat)   getFormat(Date.class),
                    (UnitFormat)   getFormat(Unit.class));
            updateFormatter(formatter);
            this.formatter = formatter;
        }
        final boolean valid;
        try {
            formatter.setBuffer(buffer);
            valid = formatter.appendElement(object) || formatter.appendValue(object);
        } finally {
            warnings = formatter.getWarnings();  // Must be saved before formatter.clear() is invoked.
            formatter.setBuffer(null);
            formatter.clear();
        }
        if (warnings != null) {
            warnings.setRoot(object);
        }
        if (!valid) {
            throw new ClassCastException(Errors.getResources(getLocale()).getString(
                    Errors.Keys.IllegalArgumentClass_2, "object", object.getClass()));
        }
        if (buffer != toAppendTo) {
            toAppendTo.append(buffer);
        }
    }

    /**
     * Creates a new format to use for parsing and formatting values of the given type.
     * This method is invoked the first time that a format is needed for the given type.
     * The {@code valueType} can be any types declared in the
     * {@linkplain CompoundFormat#createFormat(Class) parent class}.
     *
     * @param  valueType The base type of values to parse or format.
     * @return The format to use for parsing of formatting values of the given type, or {@code null} if none.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        if (valueType == Number.class) {
            return symbols.createNumberFormat();
        }
        if (valueType == Date.class) {
            return new StandardDateFormat(symbols.getLocale(), getTimeZone());
        }
        return super.createFormat(valueType);
    }

    /**
     * If warnings occurred during the last WKT {@linkplain #parse(CharSequence, ParsePosition) parsing} or
     * {@linkplain #format(Object, Appendable) formatting}, returns the warnings. Otherwise returns {@code null}.
     * The warnings are cleared every time a new object is parsed or formatted.
     *
     * @return The warnings of the last parsing of formatting operation, or {@code null} if none.
     *
     * @since 0.6
     */
    public Warnings getWarnings() {
        final Warnings w = warnings;
        if (w != null) {
            w.publish();
        }
        return w;
    }

    /**
     * Returns a clone of this format.
     *
     * @return A clone of this format.
     */
    @Override
    public WKTFormat clone() {
        final WKTFormat clone = (WKTFormat) super.clone();
        clone.formatter = null; // Do not share the formatter.
        clone.parser    = null;
        clone.warnings  = null;
        return clone;
    }
}
