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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Date;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.Vector;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.IntegerList;
import org.apache.sis.util.privy.X364;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.system.Configuration;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.metadata.simple.SimpleExtent;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.geometry.AbstractDirectPosition;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.xml.NilObject;

// Specific to the main branch:
import org.opengis.util.CodeList;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.DefaultObjectDomain;
import org.apache.sis.referencing.internal.Legacy;
import org.apache.sis.referencing.datum.AbstractDatum;


/**
 * Provides support methods for formatting a <i>Well Known Text</i> (WKT).
 *
 * <p>{@code Formatter} instances are created by {@link WKTFormat} and given to the
 * {@link FormattableObject#formatTo(Formatter)} method of the object to format.
 * {@code Formatter} provides the following services:</p>
 *
 * <ul>
 *   <li>A series of {@code append(…)} methods to be invoked by the {@code formatTo(Formatter)} implementations.</li>
 *   <li>Contextual information. In particular, the {@linkplain #toContextualUnit(Unit) contextual units} depend on
 *       the {@linkplain #getEnclosingElement(int) enclosing WKT element}.</li>
 *   <li>A flag for declaring the object unformattable.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
 * @see <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Legacy WKT 1</a>
 *
 * @since 0.4
 */
public class Formatter implements Localized {
    /**
     * Accuracy of geographic bounding boxes, in number of fraction digits.
     * We use the accuracy recommended by ISO 19162.
     */
    @Configuration
    static final int BBOX_ACCURACY = 2;

    /**
     * Maximal accuracy of vertical extents, in number of fraction digits.
     * The value used here is arbitrary and may change in any future SIS version.
     */
    @Configuration
    private static final int VERTICAL_ACCURACY = 9;

    /**
     * The locale for the localization of international strings.
     * This is not the same as {@link Symbols#getLocale()}.
     *
     * @see #errorLocale
     */
    private final Locale locale;

    /**
     * The symbols to use for this formatter.
     *
     * @see WKTFormat#getSymbols()
     * @see WKTFormat#setSymbols(Symbols)
     */
    private final Symbols symbols;

    /**
     * The value of {@link Symbols#getSeparator()} without trailing spaces, followed by the system line separator.
     * Computed by {@link Symbols#separatorNewLine()} and stored for reuse.
     */
    private final String separatorNewLine;

    /**
     * The colors to use for this formatter, or {@code null} for no syntax coloring.
     * If non-null, the terminal must be ANSI X3.64 compatible.
     * The default value is {@code null}.
     *
     * @see #configure(Convention, Citation, Colors, byte, byte, byte, int)
     */
    private Colors colors;

    /**
     * The preferred convention for objects or parameter names.
     * This field should never be {@code null}.
     *
     * @see #configure(Convention, Citation, Colors, byte, byte, byte, int)
     */
    private Convention convention;

    /**
     * The preferred authority for objects or parameter names.
     *
     * @see #configure(Convention, Citation, Colors, byte, byte, byte, int)
     */
    private Citation authority;

    /**
     * {@link Transliterator#IDENTITY} for preserving non-ASCII characters. The default value is
     * {@link Transliterator#DEFAULT}, which causes replacements like "é" → "e" in all elements
     * except {@code REMARKS["…"]}. May also be a user supplied transliterator.
     *
     * @see #getTransliterator()
     */
    Transliterator transliterator;

    /**
     * {@code true} if this {@code Formatter} should verify the validity of characters in quoted texts.
     * ISO 19162 restricts quoted texts to ASCII characters with addition of degree symbol (°).
     */
    boolean verifyCharacterValidity = true;

    /**
     * The enclosing WKT element being formatted.
     *
     * @see #getEnclosingElement(int)
     */
    private final List<FormattableObject> enclosingElements = new ArrayList<>();

    /**
     * The contextual units for writing lengths, angles or other type of measurements.
     * A unit not present in this map means that the "natural" unit of the WKT element shall be used.
     * This value is set for example by {@code "GEOGCS"}, which force its enclosing {@code "PRIMEM"}
     * to take the same units as itself.
     *
     * @see #addContextualUnit(Unit)
     * @see #toContextualUnit(Unit)
     */
    private final Map<Unit<?>, Unit<?>> units = new HashMap<>(4);

    /**
     * A bits mask of elements which defined a contextual units.
     * The rightmost bit is for the current element. The bit before the rightmost
     * is for the parent of current element, etc.
     *
     * @see #hasContextualUnit(int)
     */
    private long hasContextualUnit;

    /**
     * The object to use for formatting numbers.
     */
    private final NumberFormat numberFormat;

    /**
     * The object to use for formatting dates.
     */
    private final DateFormat dateFormat;

    /**
     * The object to use for formatting unit symbols.
     */
    private final UnitFormat unitFormat;

    /**
     * Dummy field position.
     */
    private final FieldPosition dummy = new FieldPosition(0);

    /**
     * The buffer in which to format. Consider this field as final. The only method to change
     * (indirectly) the value of this field is {@link WKTFormat#format(Object, Appendable)}.
     *
     * @see #setBuffer(StringBuffer)
     */
    private StringBuffer buffer;

    /**
     * Index of the first character in the buffer where the element content will be formatted.
     * This is set after the opening bracket and is used for determining if a separator needs
     * to be appended.
     *
     * @see #setBuffer(StringBuffer)
     */
    private int elementStart;

    /**
     * {@code 1} if keywords shall be converted to upper cases, or {@code -1} for lower cases.
     *
     * @see #configure(Convention, Citation, Colors, byte, byte, byte, int)
     */
    private byte toUpperCase;

    /**
     * {@code -1} for short keywords, {@code +1} for long keywords or 0 for the default.
     */
    private byte longKeywords;

    /**
     * Maximum number of elements to show in lists, or {@link Integer#MAX_VALUE} if unlimited.
     * If a list is longer than this length, only the first and the last elements will be shown.
     * This limit applies in particular to {@link MathTransform} parameter values of {@code double[]}
     * type, since those parameters may be large interpolation tables.
     *
     * @see #configure(Convention, Citation, Colors, byte, byte, byte, int)
     */
    private int listSizeLimit;

    /**
     * Incremented when {@link #setColor(ElementKind)} is invoked, and decremented when {@link #resetColor()}
     * is invoked. Used in order to prevent child elements to overwrite the colors decided by enclosing elements.
     */
    private int colorApplied;

    /**
     * The number of spaces to use in indentation, or {@value org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE}
     * if indentation is disabled.
     *
     * @see #configure(Convention, Citation, Colors, byte, byte, byte, int)
     */
    private byte indentation;

    /**
     * The number of space to write on the left side of each line. This amount is increased
     * by {@code indentation} every time a {@link FormattableObject} is appended in a new
     * indentation level.
     */
    private int margin;

    /**
     * Indices where to insert additional margin, or {@code null} if none. The margin to insert will be
     * the width of the keyword (e.g. {@code "BOX"}), which is usually unknown to {@code Formatter}
     * until {@link FormattableObject} finished to write the element. This field is usually {@code null},
     * unless formatting geometries.
     */
    private IntegerList keywordSpaceAt;

    /**
     * {@code true} if a new line were requested during the execution of {@link #append(FormattableObject)}.
     * This is used to determine if the next {@code UNIT} and {@code ID} elements shall appear on a new line.
     */
    private boolean requestNewLine;

    /**
     * {@code true} if we are in the process of formatting the optional complementary attributes.
     * Those attributes are {@code SCOPE}, {@code AREA}, {@code BBOX}, {@code VERTICALEXTENT}, {@code TIMEEXTENT},
     * {@code ID} (previously known as {@code AUTHORITY}) and {@code REMARKS}, and have a special treatment: they
     * are written by {@link #append(FormattableObject)} after the {@code formatTo(Formatter)} method returned.
     *
     * @see #appendComplement(IdentifiedObject, FormattableObject, FormattableObject)
     */
    private boolean isComplement;

    /**
     * {@code true} if the last formatted element was invalid WKT and shall be highlighted with syntactic coloration.
     * This field has no effect if {@link #colors} is null. This field is reset to {@code false} after the invalid
     * part has been processed by {@link #append(FormattableObject)}, in order to highlight only the first erroneous
     * element without clearing the {@link #warnings} value.
     */
    private boolean highlightError;

    /**
     * The warnings that occurred during WKT formatting, or {@code null} if none.
     *
     * @see #isInvalidWKT()
     * @see #getWarnings()
     */
    private Warnings warnings;

    /**
     * The locale for error messages (not for formatting).
     *
     * @see #locale
     */
    private final Locale errorLocale;

    /**
     * Creates a new formatter instance with the default configuration.
     */
    public Formatter() {
        this(Convention.DEFAULT, Symbols.getDefault(), Constants.DEFAULT_INDENTATION);
    }

    /**
     * Creates a new formatter instance with the specified convention, symbols and indentation.
     *
     * @param  convention   the convention to use.
     * @param  symbols      the symbols.
     * @param  indentation  the number of spaces to use in indentation for WKT formatting,
     *                      or {@link WKTFormat#SINGLE_LINE} for formatting the whole WKT on a single line.
     */
    public Formatter(final Convention convention, final Symbols symbols, final int indentation) {
        ArgumentChecks.ensureNonNull("convention",  convention);
        ArgumentChecks.ensureNonNull("symbols",     symbols);
        ArgumentChecks.ensureBetween("indentation", WKTFormat.SINGLE_LINE, Byte.MAX_VALUE, indentation);
        this.locale           = Locale.getDefault(Locale.Category.DISPLAY);
        this.errorLocale      = locale;
        this.convention       = convention;
        this.authority        = convention.getNameAuthority();
        this.symbols          = symbols.immutable();
        this.transliterator   = (convention == Convention.INTERNAL) ? Transliterator.IDENTITY : Transliterator.DEFAULT;
        this.separatorNewLine = this.symbols.separatorNewLine();
        this.indentation      = (byte) indentation;
        this.numberFormat     = symbols.createNumberFormat();
        this.dateFormat       = new LenientDateFormat(symbols.getLocale());
        this.unitFormat       = new UnitFormat(symbols.getLocale());
        this.buffer           = new StringBuffer();
        unitFormat.setStyle(UnitFormat.Style.NAME);
        if (convention.usesCommonUnits) {
            unitFormat.setLocale(Locale.US);
        }
    }

    /**
     * Constructor for private use by {@link WKTFormat} only. This allows to use the number format
     * created by {@link WKTFormat#createFormat(Class)}, which may be overridden by the user.
     *
     * @param  locale        the locale for the localization of international strings.
     * @param  errorLocale   the locale for error messages (not for parsing), or {@code null} for the system default.
     * @param  symbols       the symbols to use for this formatter.
     * @param  numberFormat  the object to use for formatting numbers.
     * @param  dateFormat    the object to use for formatting dates.
     * @param  unitFormat    the object to use for formatting unit symbols.
     */
    Formatter(final Locale locale, final Locale errorLocale, final Symbols symbols,
              final NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat)
    {
        this.locale           = locale;
        this.errorLocale      = errorLocale;
        this.convention       = Convention.DEFAULT;
        this.authority        = Convention.DEFAULT.getNameAuthority();
        this.symbols          = symbols;
        this.separatorNewLine = symbols.separatorNewLine();
        this.indentation      = Constants.DEFAULT_INDENTATION;
        this.numberFormat     = numberFormat;                      // No clone needed.
        this.dateFormat       = dateFormat;
        this.unitFormat       = unitFormat;
        // Do not set the buffer. It will be set by WKTFormat.format(…).
    }

    /**
     * Sets the destination buffer. Used by {@link WKTFormat#format(Object, Appendable)} only.
     */
    final void setBuffer(final StringBuffer buffer) {
        this.buffer = buffer;
        elementStart = (buffer != null) ? buffer.length() : 0;
    }

    /**
     * Sets the convention, authority, colors and indentation to use for formatting WKT elements.
     * This method does not validate the argument — validation must be done by the caller.
     *
     * @param  convention    the convention, or {@code null} for the default value.
     * @param  authority     the authority, or {@code null} for inferring it from the convention.
     * @param  colors        the syntax coloring, or {@code null} if none.
     * @param  toUpperCase   whether keywords shall be converted to upper cases.
     * @param  longKeywords  {@code -1} for short keywords, {@code +1} for long keywords or 0 for the default.
     * @param  indentation   the number of spaces to use in indentation for WKT formatting, or {@link WKTFormat#SINGLE_LINE}.
     * @param  listSizeLimit maximum number of elements to show in lists, or {@link Integer#MAX_VALUE} if unlimited.
     */
    final void configure(Convention convention, final Citation authority, final Colors colors,
            final byte toUpperCase, final byte longKeywords, final byte indentation, final int listSizeLimit)
    {
        this.convention     = convention;
        this.authority      = (authority != null) ? authority : convention.getNameAuthority();
        this.colors         = colors;
        this.toUpperCase    = toUpperCase;
        this.longKeywords   = longKeywords;
        this.indentation    = indentation;
        this.listSizeLimit  = listSizeLimit;
        this.transliterator = (convention == Convention.INTERNAL) ? Transliterator.IDENTITY : Transliterator.DEFAULT;
        unitFormat.setLocale(convention.usesCommonUnits ? Locale.US : Locale.ROOT);
    }

    /**
     * Returns the convention to use for formatting the WKT. The default is {@link Convention#WKT2}.
     *
     * @return the convention (never {@code null}).
     *
     * @see WKTFormat#setConvention(Convention)
     * @see FormattableObject#toString(Convention)
     */
    public final Convention getConvention() {
        return convention;
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
     * @see WKTFormat#setTransliterator(Transliterator)
     *
     * @since 0.6
     */
    public final Transliterator getTransliterator() {
        return transliterator;
    }

    /**
     * Returns the preferred authority for choosing the projection and parameter names.
     * The preferred authority can be set by the {@link WKTFormat#setNameAuthority(Citation)} method.
     * This is not necessarily the authority who created the object to format.
     *
     * <h4>Example</h4>
     * The EPSG name of the {@code EPSG:6326} datum is <q>World Geodetic System 1984</q>.
     * However if the preferred authority is OGC, then the formatted datum name will rather look like
     * <q>WGS84</q> (the exact string depends on the object aliases).
     *
     * @return the authority for projection and parameter names.
     *
     * @see WKTFormat#getNameAuthority()
     * @see org.apache.sis.referencing.IdentifiedObjects#getName(IdentifiedObject, Citation)
     */
    public final Citation getNameAuthority() {
        return authority;
    }

    /**
     * Returns the locale to use for localizing {@link InternationalString} instances.
     * This is <em>not</em> the locale for formatting dates and numbers.
     *
     * @return the locale to use for localizing international strings.
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Appends in the {@linkplain #buffer} the ANSI escape sequence for the given kind of element.
     * This method does nothing unless syntax coloring has been explicitly enabled.
     */
    private void setColor(final ElementKind type) {
        if (colors != null) {
            if (colorApplied == 0) {
                final String color = colors.getAnsiSequence(type);
                if (color == null) {
                    // Do not increment `colorApplied` for giving a chance to children to apply their colors.
                    return;
                }
                final boolean isStart = (buffer.length() == elementStart);
                buffer.append(color);
                if (isStart) {
                    elementStart = buffer.length();
                }
            }
            colorApplied++;
        }
    }

    /**
     * Appends in the {@linkplain #buffer} the ANSI escape sequence for resetting the color to the default.
     * This method does nothing unless syntax coloring has been explicitly enabled.
     *
     * <h4>Implementation note</h4>
     * This method needs to reset not only the foreground, but also the background if the
     * {@link #highlightError} flag is {@code true}. It is simpler to just use the "reset"
     * sequence unconditionally for the current implementation.
     */
    private void resetColor() {
        if (colors != null && --colorApplied <= 0) {
            colorApplied = 0;
            buffer.append(X364.RESET.sequence());
        }
    }

    /**
     * Request a line separator before the next element to format. Invoking this method before any
     * {@code append(…)} method call will cause the next element to appear on the next line.
     *
     * <p>This method has no effect in any of the following cases:</p>
     * <ul>
     *   <li>This method has already been invoked before the next {@code append(…)}.</li>
     *   <li>The indentation is {@link WKTFormat#SINGLE_LINE}.</li>
     * </ul>
     */
    public void newLine() {
        if (indentation > WKTFormat.SINGLE_LINE) {
            requestNewLine = true;
        }
    }

    /**
     * Increases or decreases the indentation. A value of {@code +1} increases
     * the indentation by the number of spaces specified at construction time,
     * and a value of {@code -1} reduces it by the same amount.
     *
     * @param  amount  +1 for increasing the indentation, or -1 for decreasing it, or 0 for no-op.
     */
    public void indent(final int amount) {
        margin = Math.max(0, margin + indentation*amount);
    }

    /**
     * Selects a short or long keyword depending on the {@link KeywordStyle} value.
     * This method can be used by {@link FormattableObject#formatTo(Formatter)}
     * implementations for choosing the return value.
     *
     * @param  shortKeyword  the keyword to return if the style is {@link KeywordStyle#SHORT}.
     * @param  longKeyword   the keyword to return if the style is {@link KeywordStyle#LONG}.
     * @return the short or long keyword depending on the keyword style setting.
     *
     * @see WKTFormat#setKeywordStyle(KeywordStyle)
     *
     * @since 0.6
     */
    public String shortOrLong(final String shortKeyword, final String longKeyword) {
        return (longKeywords != 0
                ? longKeywords < 0              // If keyword style was explicitly specified, use the setting.
                : convention.toUpperCase)       // Otherwise use the default value determined by the convention.
               ? shortKeyword : longKeyword;
    }

    /**
     * Conditionally appends a separator to the {@linkplain #buffer}, if needed.
     * This method does nothing if there is currently no element at the buffer end.
     */
    private void appendSeparator() {
        if (buffer.length() != elementStart) {
            if (requestNewLine) {
                buffer.append(separatorNewLine).append(CharSequences.spaces(margin));
            } else {
                buffer.append(symbols.getSeparator());
            }
        } else if (requestNewLine) {
            buffer.append(System.lineSeparator()).append(CharSequences.spaces(margin));
        }
        requestNewLine = false;
    }

    /**
     * Appends a separator if needed, then opens a new element.
     *
     * @param  newLine  {@code true} for invoking {@link #newLine()} first.
     * @param  keyword  the element keyword (e.g. {@code "DATUM"}, {@code "AXIS"}, <i>etc</i>).
     */
    private void openElement(final boolean newLine, String keyword) {
        if (newLine && buffer.length() != elementStart) {
            newLine();
        }
        appendSeparator();
        if (toUpperCase != 0) {
            final Locale syntax = symbols.getLocale();      // Not the same purpose as `this.locale`.
            keyword = (toUpperCase >= 0) ? keyword.toUpperCase(syntax) : keyword.toLowerCase(syntax);
        }
        elementStart = buffer.append(keyword).appendCodePoint(symbols.getOpeningBracket(0)).length();
    }

    /**
     * Closes the element opened by {@link #openElement(boolean, String)}.
     *
     * @param  newLine  {@code true} for invoking {@link #newLine()} last.
     */
    private void closeElement(final boolean newLine) {
        buffer.appendCodePoint(symbols.getClosingBracket(0));
        if (newLine) {
            newLine();
        }
    }

    /**
     * Appends the given {@code FormattableObject}.
     * This method performs the following steps:
     *
     * <ul>
     *   <li>Invoke <code>object.{@linkplain FormattableObject#formatTo(Formatter) formatTo}(this)</code>.</li>
     *   <li>Prepend the keyword returned by the above method call (e.g. {@code "GEOCS"}).</li>
     *   <li>If the given object is an instance of {@link IdentifiedObject}, then append complementary information:</li>
     * </ul>
     *
     * <blockquote><table class="sis">
     *   <caption>Complementary WKT elements</caption>
     *   <tr><th>WKT 2 element</th><th>WKT 1 element</th><th>For types</th></tr>
     *   <tr><td>{@code Anchor[…]}</td>        <td></td> <td>{@link Datum}</td></tr>
     *   <tr><td>{@code Scope[…]}</td>         <td></td> <td>{@link ReferenceSystem}, {@link Datum}, {@link CoordinateOperation}</td></tr>
     *   <tr><td>{@code Area[…]}</td>          <td></td> <td>{@link ReferenceSystem}, {@link Datum}, {@link CoordinateOperation}</td></tr>
     *   <tr><td>{@code BBox[…]}</td>          <td></td> <td>{@link ReferenceSystem}, {@link Datum}, {@link CoordinateOperation}</td></tr>
     *   <tr><td>{@code VerticalExtent[…]}</td><td></td> <td>{@link ReferenceSystem}, {@link Datum}, {@link CoordinateOperation}</td></tr>
     *   <tr><td>{@code TimeExtent[…]}</td>    <td></td> <td>{@link ReferenceSystem}, {@link Datum}, {@link CoordinateOperation}</td></tr>
     *   <tr><td>{@code Id[…]}</td><td>{@code Authority[…]}</td><td>{@link IdentifiedObject}</td></tr>
     *   <tr><td>{@code Remarks[…]}</td>       <td></td> <td>{@link ReferenceSystem}, {@link CoordinateOperation}</td></tr>
     * </table></blockquote>
     *
     * @param  object  the formattable object to append to the WKT, or {@code null} if none.
     */
    public void append(final FormattableObject object) {
        if (object == null) {
            return;
        }
        /*
         * Safety check: ensure that we do not have circular dependencies (e.g. a ProjectedCRS contains
         * a Conversion which may contain the ProjectedCRS as its target CRS). Without this protection,
         * a circular dependency would cause an OutOfMemoryError.
         */
        final int stackDepth = enclosingElements.size();
        for (int i=stackDepth; --i >= 0;) {
            if (enclosingElements.get(i) == object) {
                throw new IllegalStateException(Errors.forLocale(errorLocale)
                            .getString(Errors.Keys.CircularReference));
            }
        }
        enclosingElements.add(object);
        if (hasContextualUnit < 0) {                            // Test if leftmost bit is set to 1.
            throw new IllegalStateException(Errors.forLocale(errorLocale)
                        .getString(Errors.Keys.TreeDepthExceedsMaximum));
        }
        hasContextualUnit <<= 1;
        /*
         * Add a new line if it was requested, open the bracket and increase indentation in case the
         * element to format contains other FormattableObject elements.
         */
        appendSeparator();
        int base = buffer.length();
        elementStart = buffer.appendCodePoint(symbols.getOpeningBracket(0)).length();
        indent(+1);
        /*
         * Formats the inner part, then prepend the WKT keyword.
         * The result looks like the following:
         *
         *         <previous text>,
         *           PROJCS["NAD27 / Idaho Central",
         *             GEOGCS[...etc...],
         *             ...etc...
         */
        IdentifiedObject info = (object instanceof IdentifiedObject) ? (IdentifiedObject) object : null;
        String keyword = object.formatTo(this);
        if (keyword == null) {
            if (info != null) {
                setInvalidWKT(info, null);
            } else {
                setInvalidWKT(object.getClass(), null);
            }
            keyword = getName(object.getClass());
        } else if (toUpperCase != 0) {
            final Locale syntax = symbols.getLocale();      // Not the same purpose as `this.locale`.
            keyword = (toUpperCase >= 0) ? keyword.toUpperCase(syntax) : keyword.toLowerCase(syntax);
        }
        /*
         * If the WKT contains errors or non-standard elements, highlight the keyword (if allowed).
         * Some buffer indices will need to be shifted for spaces occupied by the X364 color codes:
         * `base`, `keywordSpaceAt`.
         */
        int highlightOffset = 0;
        if (highlightError && colors != null) {
            final String color = colors.getAnsiSequence(ElementKind.ERROR);
            if (color != null) {
                final String c = color + X364.BACKGROUND_DEFAULT.sequence();
                highlightOffset = c.length();
                buffer.insert(base, c);
                base += color.length();         // Insert keyword before `BACKGROUND_DEFAULT`.
            }
        }
        highlightError = false;
        buffer.insert(base, keyword);
        /*
         * When formatting geometry coordinates, we may need to shift all numbers by the width
         * of the keyword inserted above in order to keep numbers properly aligned. Exemple:
         *
         *     BOX[ 4.000 -10.000
         *         50.000   2.000]
         */
        if (keywordSpaceAt != null) {
            final int length = keyword.length();
            final CharSequence additionalMargin = CharSequences.spaces(keyword.codePointCount(0, length));
            final int n = keywordSpaceAt.size();
            for (int i=0; i<n;) {
                int p = keywordSpaceAt.getInt(i);
                p += highlightOffset + (++i * length);      // Take in account spaces added previously.
                buffer.insert(p, additionalMargin);
            }
            keywordSpaceAt.clear();
        }
        /*
         * Format the SCOPE["…"], AREA["…"] and other elements. Some of those information
         * are available only for Datum, CoordinateOperation and ReferenceSystem objects.
         */
        if (info == null && convention.majorVersion() != 1 && object instanceof GeneralParameterValue) {
            info = ((GeneralParameterValue) object).getDescriptor();
        }
        if (info != null) {
            appendComplement(info, (stackDepth >= 1) ? enclosingElements.get(stackDepth - 1) : null,
                                   (stackDepth >= 2) ? enclosingElements.get(stackDepth - 2) : null);
        }
        /*
         * Close the bracket, then update the queue of enclosed elements by removing this element.
         */
        buffer.appendCodePoint(symbols.getClosingBracket(0));
        indent(-1);
        enclosingElements.remove(stackDepth);
        hasContextualUnit >>>= 1;
    }

    /**
     * Appends the optional complementary attributes common to many {@link IdentifiedObject} subtypes.
     * Those attributes are {@code ANCHOR}, {@code SCOPE}, {@code AREA}, {@code BBOX}, {@code VERTICALEXTENT},
     * {@code TIMEEXTENT}, {@code ID} (previously known as {@code AUTHORITY}) and {@code REMARKS},
     * and have a special treatment: they are written by {@link #append(FormattableObject)}
     * after the {@code formatTo(Formatter)} method returned.
     *
     * <p>The {@code ID[<name>,<code>,…]} element is normally written only for the root element
     * (unless the convention is {@code INTERNAL}), but there is various exceptions to this rule.
     * If formatted, the {@code ID} element will be by default on the same line as the enclosing
     * element (e.g. {@code SPHEROID["Clarke 1866", …, ID["EPSG", 7008]]}). Other example:</p>
     *
     * {@snippet lang="wkt" :
     *   PROJCS["NAD27 / Idaho Central",
     *     GEOGCS[...etc...],
     *     ...etc...
     *     ID["EPSG", 26769]]
     *   }
     *
     * For non-internal conventions, all elements other than {@code ID[…]} are formatted
     * only for {@link CoordinateOperation} and root {@link ReferenceSystem} instances,
     * with an exception for remarks of {@code ReferenceSystem} embedded inside {@code CoordinateOperation}.
     * Those restrictions are our interpretation of the following ISO 19162 requirement:
     *
     * <blockquote>(…snip…) {@code <scope extent identifier remark>} is a collection of four optional attributes
     * which may be applied to a coordinate reference system, a coordinate operation or a boundCRS. (…snip…)
     * Identifier (…snip…) may also be utilised for components of these objects although this is not recommended
     * except for coordinate operation methods (including map projections) and parameters. (…snip…)
     * A {@code <remark>} can be included within the descriptions of source and target CRS embedded within
     * a coordinate transformation as well as within the coordinate transformation itself.</blockquote>
     */
    private void appendComplement(final IdentifiedObject object, final FormattableObject parent, final FormattableObject gp) {
        isComplement = true;
        final boolean showIDs;      // Whether to format ID[…] elements.
        final boolean filterID;     // Whether we shall limit to a single ID[…] element.
        final boolean showOthers;   // Whether to format any element other than ID[…] and Remarks[…].
        final boolean showRemarks;  // Whether to format Remarks[…].
        if (convention == Convention.INTERNAL) {
            showIDs     = true;
            filterID    = false;
            showOthers  = true;
            showRemarks = true;
        } else {
            /*
             * Except for the special cases of OperationMethod and Parameters, ISO 19162 recommends to format the
             * ID only for the root element.  But Apache SIS adds another exception to this rule by handling the
             * components of CompoundCRS as if they were root elements. The reason is that users often create their
             * own CompoundCRS from standard components, for example by adding a time axis to some standard CRS like
             * "WGS84". The resulting CompoundCRS usually have no identifier. Then the users often need to extract a
             * particular component of a CompoundCRS, most often the horizontal part, and will need its identifier
             * for example in a Web Map Service (WMS). Those ID are lost if we do not format them here.
             */
            if (parent == null || parent instanceof CompoundCRS) {
                showIDs = true;
            } else if (gp instanceof CoordinateOperation && !(parent instanceof IdentifiedObject)) {
                // "SourceCRS[…]" and "TargetCRS[…]" sub-elements in CoordinateOperation.
                showIDs = true;
            } else if (convention == Convention.WKT2_SIMPLIFIED) {
                showIDs = false;
            } else {
                showIDs = (object instanceof OperationMethod) || (object instanceof GeneralParameterDescriptor);
            }
            if (convention.majorVersion() == 1) {
                filterID    = true;
                showOthers  = false;
                showRemarks = false;
            } else {
                filterID = (parent != null);
                if (object instanceof CoordinateOperation) {
                    showOthers  = !(parent instanceof ConcatenatedOperation);
                    showRemarks = showOthers;
                } else if (object instanceof ReferenceSystem) {
                    showOthers  = (parent == null);
                    showRemarks = (parent == null) || (gp instanceof CoordinateOperation);
                } else {
                    showOthers  = false;    // Mandated by ISO 19162.
                    showRemarks = false;
                }
            }
        }
        if (showOthers) {
            appendForSubtypes(object);
        }
        if (showIDs) {
            Collection<ReferenceIdentifier> identifiers = object.getIdentifiers();
            if (identifiers != null) {                                                  // Paranoiac check
                if (filterID) {
                    for (final ReferenceIdentifier id : identifiers) {
                        if (Citations.identifierMatches(authority, id.getAuthority())) {
                            identifiers = Set.of(id);
                            break;
                        }
                    }
                }
                for (ReferenceIdentifier id : identifiers) {
                    if (!(id instanceof FormattableObject)) {
                        id = ImmutableIdentifier.castOrCopy(id);
                    }
                    append((FormattableObject) id);
                    if (filterID) break;
                }
            }
        }
        if (showRemarks) {
            appendOnNewLine(WKTKeywords.Remark, object.getRemarks(), ElementKind.REMARKS);
        }
        isComplement = false;
    }

    /**
     * Appends the anchor, scope and domain of validity of the given object.
     * In the ISO 19111 model, those information are valid only for {@link Datum},
     * {@link ReferenceSystem} and {@link CoordinateOperation} objects.
     */
    private void appendForSubtypes(final IdentifiedObject object) {
        InternationalString anchor = null, scope = null;
        Extent area = null;
        if (object instanceof AbstractDatum) {
            anchor = ((AbstractDatum) object).getAnchorPoint();
        } else if (!(object instanceof ReferenceSystem || object instanceof CoordinateOperation)) {
            return;
        }
        for (final DefaultObjectDomain domain : Legacy.getDomains(object)) {
            scope = domain.getScope();
            area = domain.getDomainOfValidity();
            if (area != null) break;
            // TODO: in 2019 revision we need to format all USAGE[…] elements, not only the first one.
        }
        appendOnNewLine(WKTKeywords.Anchor, anchor, null);
        append(scope, area);
    }

    /**
     * Appends the usage (scope and domain of validity) of an object.
     * The arguments are the components of an {@link DefaultObjectDomain}.
     * The given extent is decomposed in horizontal, vertical and temporal components.
     * The horizontal component uses the default number of fraction digits recommended by ISO 19162.
     *
     * <h4>Usage element</h4>
     * In a WKT string, the given elements should be enclosed in an {@code USAGE[…]} element
     * according the ISO 19162:2019 standard but not according the previous version (2015).
     * The {@code USAGE[…]} enclosing element shall be provided when needed by the caller.
     *
     * @param scope  description of domain of usage, or {@code null} if none.
     * @param area   area for which the object is valid, or {@code null} if none.
     *
     * @since 1.4
     */
    public void append(final InternationalString scope, final Extent area) {
        if (scope != null && !(scope instanceof NilObject)) {
            appendOnNewLine(WKTKeywords.Scope, scope, ElementKind.SCOPE);
        }
        if (area != null && !(area instanceof NilObject)) {
            GeographicBoundingBox bbox;
            try {
                bbox = Extents.getGeographicBoundingBox(area);
            } catch (InvalidMetadataException e) {
                warning(e, WKTKeywords.BBox, WKTKeywords.Usage);
                bbox = null;
            }
            appendOnNewLine(WKTKeywords.Area, area.getDescription(), ElementKind.EXTENT);
            append(bbox, BBOX_ACCURACY);
            appendVerticalExtent(area);
            appendTemporalExtent(area);
        }
    }

    /**
     * Appends the given geographic bounding box in a {@code BBOX[…]} element.
     * Longitude and latitude values will be formatted in decimal degrees.
     * Longitudes are relative to the Greenwich meridian, with values increasing toward East.
     * Latitudes values are increasing toward North.
     *
     * <h4>Numerical precision</h4>
     * The ISO 19162 standards recommends to format those values with only 2 decimal digits.
     * This is because {@code GeographicBoundingBox} does not specify the datum, so this box
     * is an approximated information only.
     *
     * @param  bbox  the geographic bounding box to append to the WKT, or {@code null}.
     * @param  fractionDigits  the number of fraction digits to use. The recommended value is 2.
     */
    public void append(final GeographicBoundingBox bbox, final int fractionDigits) {
        if (bbox != null) {
            openElement(isComplement, WKTKeywords.BBox);
            setColor(ElementKind.EXTENT);
            numberFormat.setMinimumFractionDigits(fractionDigits);
            numberFormat.setMaximumFractionDigits(fractionDigits);
            numberFormat.setRoundingMode(RoundingMode.FLOOR);
            appendPreset(bbox.getSouthBoundLatitude());
            appendPreset(bbox.getWestBoundLongitude());
            numberFormat.setRoundingMode(RoundingMode.CEILING);
            appendPreset(bbox.getNorthBoundLatitude());
            appendPreset(bbox.getEastBoundLongitude());
            resetColor();
            closeElement(isComplement);
        }
    }

    /**
     * Appends the vertical of the given area.
     * This method chooses an accuracy from the vertical span.
     * Examples:
     *
     * <ul>
     *   <li>“{@code VerticalExtent[102, 108, LengthUnit["m", 1]]}”       (Δz =   6)</li>
     *   <li>“{@code VerticalExtent[100.2, 100.8, LengthUnit["m", 1]]}”   (Δz = 0.6)</li>
     * </ul>
     *
     * Note that according ISO 19162, heights are positive toward up and relative to an unspecified mean sea level.
     * It is caller's responsibility to ensure that the given range complies with that specification as much as possible.
     */
    private void appendVerticalExtent(final Extent area) {
        final MeasurementRange<Double> range;
        try {
            range = Extents.getVerticalRange(area);
        } catch (InvalidMetadataException e) {
            warning(e, WKTKeywords.VerticalExtent, WKTKeywords.Usage);
            return;
        }
        if (range != null) {
            final double min = range.getMinDouble();
            final double max = range.getMaxDouble();
            int minimumFractionDigits = Numerics.fractionDigitsForDelta(max - min);
            int maximumFractionDigits = Math.min(Math.min(
                    Numerics.suggestFractionDigits(min, max),
                    minimumFractionDigits + 2), VERTICAL_ACCURACY);             // Arbitrarily limit to 2 more digits.
            openElement(true, WKTKeywords.VerticalExtent);
            setColor(ElementKind.EXTENT);
            numberFormat.setMinimumFractionDigits(minimumFractionDigits);
            numberFormat.setMaximumFractionDigits(maximumFractionDigits);
            numberFormat.setRoundingMode(RoundingMode.FLOOR);   appendPreset(min);
            numberFormat.setRoundingMode(RoundingMode.CEILING); appendPreset(max);
            final Unit<?> unit = range.unit();
            if (!convention.isSimplified() || !Units.METRE.equals(unit)) {
                append(unit);                                               // Unit are optional if they are metres.
            }
            resetColor();
            closeElement(true);
        }
    }

    /**
     * Appends the given temporal extent, if non-null.
     * Examples:
     *
     * <ul>
     *   <li>“{@code TemporalExtent[1980-04-12, 1980-04-18]}”</li>
     *   <li>“{@code TemporalExtent[1980-04-12T18:00:00.0Z, 1980-04-12T21:00:00.0Z]}”</li>
     * </ul>
     */
    private void appendTemporalExtent(final Extent area) {
        final Range<Date> range = Extents.getTimeRange(area);
        if (range != null) {
            final Temporal min = TemporalDate.toTemporal(range.getMinValue());
            final Temporal max = TemporalDate.toTemporal(range.getMaxValue());
            if (min != null && max != null) {
                openElement(true, WKTKeywords.TimeExtent);
                setColor(ElementKind.EXTENT);
                append(min);
                append(max);
                resetColor();
                closeElement(true);
            }
        }
    }

    /**
     * Appends the given math transform, typically (but not necessarily) in a {@code PARAM_MT[…]} element.
     *
     * @param  transform  the transform object to append to the WKT, or {@code null} if none.
     */
    public void append(final MathTransform transform) {
        if (transform != null) {
            if (transform instanceof FormattableObject) {
                append((FormattableObject) transform);
            } else {
                final FormattableObject object = WKTUtilities.toFormattable(transform, convention == Convention.INTERNAL);
                if (object != null) {
                    append(object);
                } else {
                    throw new UnformattableObjectException(Errors.format(
                            Errors.Keys.IllegalClass_2, FormattableObject.class, transform.getClass()));
                }
            }
        }
    }

    /**
     * Appends an international text in an element having the given keyword. Since this method
     * is typically invoked for long descriptions, the element will be written on its own line.
     * Examples:
     * <ul>
     *   <li>{@code Scope["Large scale topographic mapping and cadastre."]}</li>
     *   <li>{@code Area["Netherlands offshore."]}</li>
     * </ul>
     *
     * @param  keyword  the {@linkplain KeywordCase#CAMEL_CASE camel-case} keyword.
     *                  Example: {@code "Scope"}, {@code "Area"} or {@code "Remarks"}.
     * @param  text     the text, or {@code null} if none.
     * @param  type     the key of the colors to apply if syntax coloring is enabled.
     */
    private void appendOnNewLine(final String keyword, final InternationalString text, final ElementKind type) {
        ArgumentChecks.ensureNonNull("keyword", keyword);
        if (text != null) {
            String localized = text.toString(locale);
            if (localized != null && !(localized = localized.strip()).isEmpty()) {
                openElement(true, keyword);
                quote(localized, type);
                closeElement(true);
            }
        }
    }

    /**
     * Appends a character string between quotes.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the text if needed.
     *
     * @param  text  the string to format to the WKT, or {@code null} if none.
     * @param  type  the key of the colors to apply if syntax coloring is enabled, or {@code null} if none.
     */
    public void append(final String text, final ElementKind type) {
        if (text != null) {
            appendSeparator();
            if (type != ElementKind.CODE_LIST) {
                quote(text, type);
            } else {
                /*
                 * Code lists have no quotes. They are normally formatted by the append(ControlledVocabulary) method,
                 * but an important exception is the CS[type] element in which the type is defined by the interface
                 * implemented by the CoordinateSystem rather than a CodeList instance.
                 */
                setColor(type);
                buffer.append(text);
                resetColor();
            }
        }
    }

    /**
     * Appends the given string as a quoted text. If the given string contains the closing quote character,
     * that character will be doubled (WKT 2) or deleted (WKT 1). We check for the closing quote only because
     * it is the character that the parser will look for determining the text end.
     */
    private void quote(String text, final ElementKind type) {
        setColor(type);
        final int base = buffer.appendCodePoint(symbols.getOpeningQuote(0)).length();
        if (type != ElementKind.REMARKS) {
            text = transliterator.filter(text);
            if (verifyCharacterValidity) {
                int startAt = 0;                                        // Index of the last space character.
                final int length = text.length();
                for (int i = 0; i < length;) {
                    int c = text.codePointAt(i);
                    int n = Character.charCount(c);
                    if (!Characters.isValidWKT(c)) {
                        final String illegal = text.substring(i, i+n);
                        while ((i += n) < length) {
                            c = text.codePointAt(i);
                            n = Character.charCount(c);
                            if (c == ' ' || c == '_') break;
                        }
                        warnings().add(Errors.formatInternational(Errors.Keys.IllegalCharacterForFormat_3,
                                "Well-Known Text", text.substring(startAt, i), illegal), null, null);
                        break;
                    }
                    i += n;
                    if (c == ' ' || c == '_') {
                        startAt = i;
                    }
                }
            }
        }
        buffer.append(text);
        closeQuote(base);
        resetColor();
    }

    /**
     * Double or delete any closing quote character that may appear at or after the given index,
     * then append the closing quote character. The action taken for the quote character depends
     * on the WKT version:
     *
     * <ul>
     *   <li>For WKT 2, double the quote as specified in the standard.</li>
     *   <li>For WKT 1, conservatively delete the quote because the standard does not said what to do.</li>
     * </ul>
     */
    private void closeQuote(int fromIndex) {
        final String quote = symbols.getQuote();
        while ((fromIndex = buffer.indexOf(quote, fromIndex)) >= 0) {
            final int n = quote.length();
            if (convention.majorVersion() == 1) {
                buffer.delete(fromIndex, fromIndex + n);
            } else {
                buffer.insert(fromIndex += n, quote);
                fromIndex += n;
            }
        }
        buffer.append(quote);
    }

    /**
     * Appends an enumeration or code list value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the code list if needed.
     *
     * <p>For the WKT 2 format, this method uses the {@linkplain Types#getCodeName ISO name if available}
     * (for example {@code "northEast"}).
     * For the WKT 1 format, this method uses the programmatic name instead (for example {@code "NORTH_EAST"}).</p>
     *
     * @param  code  the code list to append to the WKT, or {@code null} if none.
     */
    public void append(final CodeList<?> code) {
        if (code != null) {
            appendSeparator();
            final String name = convention.majorVersion() == 1 ? code.name() : Types.getCodeName(code);
            if (CharSequences.isUnicodeIdentifier(name)) {
                setColor(ElementKind.CODE_LIST);
                buffer.append(name);
                resetColor();
            } else {
                quote(name, ElementKind.ERROR);
                setInvalidWKT(code.getClass(), null);
                highlightError = false;                 // Because already highlighted.
            }
        }
    }

    /**
     * Appends a temporal object (usually an instant).
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the date if needed.
     *
     * @param  date  the date to append to the WKT, or {@code null} if none.
     *
     * @since 1.5
     */
    public void append(final Temporal date) {
        if (date != null) {
            appendSeparator();
            if (date instanceof Instant) {
                dateFormat.format(Date.from((Instant) date), buffer, dummy);
            } else {
                // Preserve the data structure (e.g. whether there is hours or not, timezone or not).
                buffer.append(date);
            }
        }
    }

    /**
     * Appends a date.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the date if needed.
     *
     * @param  date  the date to append to the WKT, or {@code null} if none.
     *
     * @deprecated Replaced by {@link #append(Temporal)}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public void append(final Date date) {
        if (date != null) {
            appendSeparator();
            dateFormat.format(date, buffer, dummy);
        }
    }

    /**
     * Appends a Boolean value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the boolean if needed.
     *
     * @param  value  the Boolean to append to the WKT.
     */
    public void append(final boolean value) {
        appendSeparator();
        buffer.append(value ? "TRUE" : "FALSE");
    }

    /**
     * Appends an integer value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param  number  the integer to append to the WKT.
     */
    public void append(final long number) {
        appendSeparator();
        /*
         * The check for `isComplement` is a hack for ImmutableIdentifier.formatTo(Formatter).
         * We do not have a public API for controlling the integer colors (it may not be desirable).
         */
        setColor(isComplement ? ElementKind.IDENTIFIER : ElementKind.INTEGER);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends an floating point value with a number of fraction digits determined automatically.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param  number  the floating point value to append to the WKT.
     */
    public void append(final double number) {
        appendSeparator();
        setColor(ElementKind.NUMBER);
        /*
         * Use scientific notation if the number magnitude is too high or too low. The threshold values used here
         * may be different than the threshold values used in the standard `StringBuilder.append(double)` method.
         * In particular, we use a higher threshold for large numbers because ellipsoid axis lengths are above the
         * JDK threshold when the axis length is given in feet (about 2.1E+7) while we still want to format them
         * as usual numbers.
         *
         * Note that we perform this special formatting only if the `NumberFormat` is not localized
         * (which is the usual case).
         */
        if (symbols.useScientificNotation(Math.abs(number))) {
            buffer.append(number);
        } else {
            /*
             * The 2 below is for using two less fraction digits than the expected number accuracy.
             * The intent is to give to DecimalFormat a chance to hide rounding errors, keeping in
             * mind that the number value is not necessarily the original one (we may have applied
             * a unit conversion). In the case of WGS84 semi-major axis in metres, we still have a
             * maximum of 8 fraction digits, which is more than enough.
             */
            numberFormat.setMaximumFractionDigits(DecimalFunctions.fractionDigitsForValue(number, 2));
            numberFormat.setMinimumFractionDigits(1);   // Must be after setMaximumFractionDigits(…).
            numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
            numberFormat.format(number, buffer, dummy);
        }
        resetColor();
    }

    /**
     * Appends an floating point value with the given number of fraction digits.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param  number          the floating point value to append to the WKT.
     * @param  fractionDigits  the number of fraction digits to use for formatting the number.
     *
     * @since 1.5
     */
    public void append(final double number, final int fractionDigits) {
        appendSeparator();
        setColor(ElementKind.NUMBER);
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends rows of numbers. Each number is separated by a space, and each row is separated by a comma.
     * Rows usually have all the same length, but this is not mandatory.
     * This method can be used for formatting geometries or matrix.
     *
     * @param  rows            the rows to append, or {@code null} if none.
     * @param  fractionDigits  the number of fraction digits for each column in a row, or {@code null} for default.
     *         A precision can be specified for each column because those columns are often different dimensions of
     *         a Coordinate Reference System (CRS), each with their own units of measurement.
     *         If a row contains more numbers than {@code fractionDigits.length},
     *         then the last value in this array is repeated for all remaining row numbers.
     *
     * @since 1.0
     */
    public void append(final Vector[] rows, int... fractionDigits) {
        if (rows == null || rows.length == 0) {
            return;
        }
        if (fractionDigits == null || fractionDigits.length == 0) {
            fractionDigits = WKTUtilities.suggestFractionDigits(rows);
        }
        numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        /*
         * If the rows are going to be formatted on many lines, then we will need to put some margin before each row.
         * If the first row starts on its own line, then the margin will be the usual indentation. But if the first
         * row starts on the same line as previous elements (or the keyword of this element, e.g. "BOX["), then we
         * will need a different number of spaces if we want to have the numbers properly aligned.
         */
        final int numRows = rows.length;
        final boolean isMultiLines = (indentation > WKTFormat.SINGLE_LINE) && (numRows > 1);
        final boolean needsAlignment = !requestNewLine;
        final CharSequence marginBeforeRow;
        if (isMultiLines) {
            int currentLineLength = margin;
            if (needsAlignment) {
                final int length = buffer.length();
                int i = length;
                while (i > 0) {                                         // Locate beginning of current line.
                    final int c = buffer.codePointBefore(i);
                    if (Characters.isLineOrParagraphSeparator(c)) break;
                    i -= Character.charCount(c);
                }
                currentLineLength = X364.lengthOfPlain(buffer, i, length);
            }
            marginBeforeRow = CharSequences.spaces(currentLineLength);
        } else {
            marginBeforeRow = "";
        }
        /*
         * `formattedNumberMarks` contains, for each number in each row, the index after `base` where
         * the number starts and the index where the number ends (including X364 colors), together with
         * the number lengths (ignoring X364 colors). Those information are stored as (start,end,length)
         * tuples. We compute those values unconditionally for simplicity, but will ignore them if the
         * WKT is formatted on a single line.
         */
        final int TUPLE_LENGTH = 3;     // Number of elements in each `formattedNumberMarks` tuple.
        final int base = elementStart;  // Needs to be saved here because `elementStart` may be modified.
        final int[][] formattedNumberMarks = new int[numRows][];
        int maxNumCols = 0;
        for (int j=0; j<numRows; j++) {
            if (j == 0) {
                appendSeparator();      // It is up to the caller to decide if we begin with a new line.
            } else {
                buffer.append(separatorNewLine).append(marginBeforeRow);
            }
            final Vector numbers = rows[j];
            final int numCols = numbers.size();
            maxNumCols = Math.max(maxNumCols, numCols);             // Store the length of longest row.
            final int[] marks = new int[numCols*TUPLE_LENGTH];      // Positions where numbers are formatted.
            formattedNumberMarks[j] = marks;
            for (int i=0,k=0; i<numCols; i++) {
                if (i != 0) buffer.append(Symbols.NUMBER_SEPARATOR);
                if (i < fractionDigits.length) {                    // Otherwise, same as previous number.
                    final int f = fractionDigits[i];
                    numberFormat.setMaximumFractionDigits(f);
                    numberFormat.setMinimumFractionDigits(f);
                }
                marks[k++] = buffer.length() - base;        // Store the start position where number is formatted.
                setColor(ElementKind.NUMBER);
                final int s = buffer.length();
                final Number n = numbers.get(i);
                if (n != null) {
                    numberFormat.format(n, buffer, dummy);
                } else {
                    buffer.append('…');
                }
                final int e = buffer.length();
                resetColor();
                marks[k++] = buffer.length() - base;        // Store the end position where number is formatted.
                marks[k++] = buffer.codePointCount(s, e);   // Note: there is no X364 colors in this range.
            }
        }
        /*
         * If formatting on more than one line, insert the number of spaces required for aligning numbers.
         * This is possible because we wrote the coordinate values with fixed number of fraction digits.
         */
        if (isMultiLines) {
            // Compute the maximal width of each column.
            final int[] columnWidths = new int[maxNumCols];
            for (final int[] marks : formattedNumberMarks) {
                for (int i = TUPLE_LENGTH-1; i < marks.length; i += TUPLE_LENGTH) {
                    final int w = marks[i];
                    final int k = i / TUPLE_LENGTH;
                    if (w > columnWidths[k]) columnWidths[k] = w;
                }
            }
            final String toWrite = buffer.substring(base);          // Save what we formatted in above loop.
            buffer.setLength(base);                                 // Discard what we formatted - we will rewrite.
            int endOfPrevious = 0;
            boolean requestAlignment = false;
            for (int[] marks : formattedNumberMarks) {              // Recopy the formatted text, with more spaces.
                for (int i = 0; i<marks.length;) {
                    final int w = columnWidths[i / TUPLE_LENGTH];
                    final int s = marks[i++];
                    final int e = marks[i++];
                    final int n = marks[i++];
                    buffer.append(toWrite, endOfPrevious, s).append(CharSequences.spaces(w - n));
                    /*
                     * If we are formatting the first number of a new line except the first line,
                     * we will need to add more spaces than what we added with `marginBeforeRow`.
                     * The number of spaces depends on the number of characters in the keyword to
                     * be written before the values. We do not know that keyword yet, so we need
                     * to remember that more spaces will need to be inserted here.
                     */
                    if (requestAlignment) {
                        requestAlignment = false;
                        if (keywordSpaceAt == null) {
                            keywordSpaceAt = new IntegerList(formattedNumberMarks.length, Integer.MAX_VALUE);
                        }
                        keywordSpaceAt.add(buffer.length());
                    }
                    buffer.append(toWrite, s, e);
                    endOfPrevious = e;
                }
                requestAlignment = needsAlignment;
            }
        }
    }

    /**
     * Appends the given number without any change to the {@link NumberFormat} setting.
     * Caller shall ensure that the following method has been invoked prior this method call:
     *
     * <ul>
     *   <li>{@link NumberFormat#setMinimumFractionDigits(int)}</li>
     *   <li>{@link NumberFormat#setMaximumFractionDigits(int)}</li>
     *   <li>{@link NumberFormat#setRoundingMode(RoundingMode)}</li>
     * </ul>
     */
    private void appendPreset(final double number) {
        appendSeparator();
        setColor(ElementKind.NUMBER);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends a number which is assumed to have no rounding error greater than the limit of IEEE 754 accuracy.
     * This method is invoked for formatting the unit conversion factors, which are defined by the Unit library
     * rather than specified by the user. The given number is formatted by {@link Double#toString(double)} both
     * for accuracy and for automatic usage of scientific notation.  If the given number is an integer, then it
     * formatted without the trailing ".0".
     */
    private void appendExact(final double number) {
        if (Locale.ROOT.equals(symbols.getLocale())) {
            appendSeparator();
            setColor(ElementKind.NUMBER);
            final int i = (int) number;
            if (i == number) {
                buffer.append(i);
            } else {
                buffer.append(number);
            }
            resetColor();
        } else {
            append(number);
        }
    }

    /**
     * Appends a unit in a {@code Unit[…]} element or one of the specialized elements. Specialized elements are
     * {@code AngleUnit}, {@code LengthUnit}, {@code ScaleUnit}, {@code ParametricUnit} and {@code TimeUnit}.
     * By {@linkplain KeywordStyle#DEFAULT default}, specialized unit keywords are used with the
     * {@linkplain Convention#WKT2 WKT 2 convention}.
     *
     * <h4>Example</h4>
     * {@code append(Units.KILOMETRE)} will append "{@code LengthUnit["km", 1000]}" to the WKT.
     *
     * @param  unit  the unit to append to the WKT, or {@code null} if none.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#35">WKT 2 specification §7.4</a>
     */
    @SuppressWarnings("StringEquality")
    public void append(final Unit<?> unit) {
        if (unit != null) {
            final boolean isSimplified = (longKeywords == 0) ? convention.isSimplified() : (longKeywords < 0);
            final boolean isWKT1 = convention.majorVersion() == 1;
            final Unit<?> base = unit.getSystemUnit();
            final String keyword;
            if (base.equals(Units.METRE)) {
                keyword = isSimplified ? WKTKeywords.Unit : WKTKeywords.LengthUnit;
            } else if (base.equals(Units.RADIAN)) {
                keyword = isSimplified ? WKTKeywords.Unit : WKTKeywords.AngleUnit;
            } else if (base.equals(Units.UNITY)) {
                keyword = isSimplified ? WKTKeywords.Unit : WKTKeywords.ScaleUnit;
            } else if (base.equals(Units.SECOND)) {
                keyword = WKTKeywords.TimeUnit;  // "Unit" alone is not allowed for time units according ISO 19162.
            } else {
                keyword = WKTKeywords.ParametricUnit;
            }
            openElement(false, keyword);
            setColor(ElementKind.UNIT);
            final int fromIndex = buffer.appendCodePoint(symbols.getOpeningQuote(0)).length();
            unitFormat.format(unit, buffer, dummy);
            closeQuote(fromIndex);
            resetColor();
            final double conversion = Units.toStandardUnit(unit);
            if (Double.isNaN(conversion) && Units.isAngular(unit)) {
                appendExact(Math.PI / 180);                 // Presume that we have sexagesimal degrees (see below).
            } else {
                appendExact(conversion);
            }
            /*
             * The EPSG code in UNIT elements is generally not recommended. But we make an exception for sexagesimal
             * units (EPSG:9108, 9110 and 9111) because they cannot be represented by a simple scale factor in WKT.
             * Those units are identified by a conversion factor set to NaN since the conversion is non-linear.
             */
            if (convention == Convention.INTERNAL || Double.isNaN(conversion)) {
                final Integer code = Units.getEpsgCode(unit, getEnclosingElement(1) instanceof CoordinateSystemAxis);
                if (code != null) {
                    openElement(false, isWKT1 ? WKTKeywords.Authority : WKTKeywords.Id);
                    append(Constants.EPSG, null);
                    if (isWKT1) {
                        append(code.toString(), null);
                    } else {
                        append(code);
                    }
                    closeElement(false);
                }
            }
            closeElement(false);
            /*
             * ISO 19162 requires the conversion factor to be positive.
             * In addition, keywords other than "Unit" are not valid in WKT 1.
             */
            if (!(conversion > 0) || (keyword != WKTKeywords.Unit && isWKT1)) {
                setInvalidWKT(Unit.class, null);
            }
        }
    }

    /**
     * Appends an object or an array of objects.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the given value is {@code null}, then this method appends the "{@code null}" string (without quotes).</li>
     *   <li>Otherwise if the given value is an array, then this method appends the opening sequence symbol, formats all
     *       elements by invoking this method recursively, then appends the closing sequence symbol.</li>
     *   <li>Otherwise if the value type is assignable to the argument type of one of the {@code append(…)} methods
     *       in this class, then the formatting will be delegated to that method.</li>
     *   <li>Otherwise the given value is appended as a quoted text with its {@code toString()} representation.</li>
     * </ul>
     *
     * @param  value  the value to append to the WKT, or {@code null}.
     */
    public void appendAny(final Object value) {
        if (value == null) {
            appendSeparator();
            buffer.append("null");
        } else if (!appendValue(value) && !appendElement(value)) {
            append(value.toString(), null);
        }
    }

    /**
     * Tries to append a small unit of information like number, date, boolean, code list, character string
     * or an array of those. The key difference between this method and {@link #appendElement(Object)} is
     * that the values formatted by this {@code appendValue(Object)} method do not have keyword.
     *
     * @return {@code true} on success, or {@code false} if the given type is not recognized.
     */
    final boolean appendValue(final Object value) {
        if (value instanceof Number) {
            final Number number = (Number) value;
            if (Numbers.isInteger(number.getClass())) {
                append(number.longValue());
            } else {
                append(number.doubleValue());
            }
        }
        else if (value instanceof CodeList<?>) append((CodeList<?>) value);
        else if (value instanceof Date)        append((Date)        value);
        else if (value instanceof Temporal)    append((Temporal)    value);
        else if (value instanceof Boolean)     append((Boolean)     value);
        else if (value instanceof CharSequence) {
            append((value instanceof InternationalString) ?
                    ((InternationalString) value).toString(locale) : value.toString(), null);
        } else if (value.getClass().isArray()) {
            /*
             * All above cases delegated to another method which invoke `appendSeparator()`.
             * Since the following block is writing itself a new element, we need to invoke
             * `appendSeparator()` here. This block invokes (indirectly) this `appendValue`
             * method recursively for some or all elements in the list.
             */
            appendSeparator();
            elementStart = buffer.appendCodePoint(symbols.getOpenSequence()).length();
            final int length = Array.getLength(value);
            final int cut = (length <= listSizeLimit) ? length : Math.max(listSizeLimit/2 - 1, 1);
            for (int i=0; i<length; i++) {
                if (i == cut) {
                    /*
                     * Skip elements in the middle if the list is too long. The `cut` index has been computed
                     * in such a way that the number of elements to skip should be greater than 1, otherwise
                     * formatting the single missing element would often have been shorter.
                     */
                    final int skip = length - Math.min(2*cut, listSizeLimit);
                    buffer.append(symbols.getSeparator());
                    setColor(ElementKind.REMARKS);
                    buffer.append(Resources.forLocale(locale).getString(Resources.Keys.ElementsOmitted_1, skip));
                    resetColor();
                    i += skip;
                    setInvalidWKT(value.getClass().getSimpleName(), null);
                }
                appendAny(Array.get(value, i));
            }
            buffer.appendCodePoint(symbols.getCloseSequence());
        } else {
            return false;
        }
        return true;
    }

    /**
     * Tries to append an object of the {@code KEYWORD[something]} form. The given value is typically,
     * but not necessarily, a {@link FormattableObject} object or an instance of an interface that can
     * be converted to {@code FormattableObject}.
     *
     * @return {@code true} on success, or {@code false} if the given type is not recognized.
     */
    final boolean appendElement(final Object value) {
        if (value instanceof FormattableObject) {
            append((FormattableObject) value);
        } else if (value instanceof IdentifiedObject) {
            append(AbstractIdentifiedObject.castOrCopy((IdentifiedObject) value));
        } else if (value instanceof MathTransform) {
            append((MathTransform) value);
        } else if (value instanceof Unit<?>) {
            append((Unit<?>) value);
        } else if (value instanceof GeographicBoundingBox) {
            append((GeographicBoundingBox) value, BBOX_ACCURACY);
        } else if (value instanceof VerticalExtent) {
            appendVerticalExtent(new SimpleExtent(null, (VerticalExtent) value, null));
        } else if (value instanceof TemporalExtent) {
            appendTemporalExtent(new SimpleExtent(null, null, (TemporalExtent) value));
        } else if (value instanceof DirectPosition) {
            append(AbstractDirectPosition.castOrCopy((DirectPosition) value));
        } else if (value instanceof Envelope) {
            append(AbstractEnvelope.castOrCopy((Envelope) value));          // Non-standard
        } else {
            return false;
        }
        return true;
    }

    /**
     * Delegates the formatting to another {@link FormattableObject} implementation.
     * Invoking this method is equivalent to first verifying the {@code other} class,
     * then delegating as below:
     *
     * {@snippet lang="java" :
     *     return other.formatTo(this);
     *     }
     *
     * This method is useful for {@code FormattableObject} which are wrapper around another object.
     * It allows to delegate the WKT formatting to the wrapped object.
     *
     * @param   other  the object to format with this formatter.
     * @return  the value returned by {@link FormattableObject#formatTo(Formatter)}.
     *
     * @since 0.5
     */
    public String delegateTo(final Object other) throws UnformattableObjectException {
        ArgumentChecks.ensureNonNull("other", other);
        if (other instanceof FormattableObject) {
            return ((FormattableObject) other).formatTo(this);
        }
        throw new UnformattableObjectException(Errors.format(
                Errors.Keys.IllegalClass_2, FormattableObject.class, other.getClass()));
    }

    /**
     * Returns the enclosing WKT element, or {@code null} if element being formatted is the root.
     * This method can be invoked by child elements having some aspects that depend on the enclosing element.
     *
     * @param  depth  1 for the immediate parent, 2 for the parent of the parent, <i>etc.</i>
     * @return the parent element at the given depth, or {@code null}.
     */
    public FormattableObject getEnclosingElement(int depth) {
        ArgumentChecks.ensurePositive("depth", depth);
        depth = (enclosingElements.size() - 1) - depth;
        return (depth >= 0) ? enclosingElements.get(depth) : null;
    }

    /**
     * Returns {@code true} if the element at the given depth specified a contextual unit.
     * This method returns {@code true} if the formattable object given by {@code getEnclosingElement(depth)}
     * has invoked {@link #addContextualUnit(Unit)} with a non-null unit at least once.
     *
     * <h4>Usage note</h4>
     * The main purpose of this method is to allow {@code AXIS[…]} elements to determine if they should
     * inherit the unit specified by the enclosing CRS, or if they should specify their unit explicitly.
     *
     * @param  depth  1 for the immediate parent, 2 for the parent of the parent, <i>etc.</i>
     * @return whether the parent element at the given depth has invoked {@code addContextualUnit(…)} at least once.
     */
    public boolean hasContextualUnit(final int depth) {
        ArgumentChecks.ensurePositive("depth", depth);
        return (hasContextualUnit & Numerics.bitmask(depth)) != 0;
    }

    /**
     * Adds a unit to use for the next measurements of the quantity {@code Q}. The given unit will apply to
     * all WKT elements containing a value of quantity {@code Q} without their own {@code UNIT[…]} element,
     * until the {@link #restoreContextualUnit(Unit, Unit)} method is invoked.
     *
     * <p>If the given unit is null, then this method does nothing and returns {@code null}.</p>
     *
     * <h4>Special case</h4>
     * If the WKT conventions are {@code WKT1_COMMON_UNITS}, then this method ignores the given unit
     * and returns {@code null}. See {@link Convention#WKT1_COMMON_UNITS} javadoc for more information.
     *
     * @param  <Q>   the unit quantity.
     * @param  unit  the contextual unit to add, or {@code null} if none.
     * @return the previous contextual unit for quantity {@code Q}, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public <Q extends Quantity<Q>> Unit<Q> addContextualUnit(final Unit<Q> unit) {
        if (unit == null || convention.usesCommonUnits) {
            return null;
        }
        hasContextualUnit |= 1;
        return (Unit<Q>) units.put(unit.getSystemUnit(), unit);
    }

    /**
     * Restores the contextual unit to its previous state before the call to {@link #addContextualUnit(Unit)}.
     * This method is used in the following pattern:
     *
     * {@snippet lang="java" :
     *     final Unit<?> previous = formatter.addContextualUnit(unit);
     *     // ... format some WKT elements here.
     *     formatter.restoreContextualUnit(unit, previous);
     *     }
     *
     * @param  unit      the value given in argument to {@code addContextualUnit(unit)} (can be {@code null}).
     * @param  previous  the value returned by {@code addContextualUnit(unit)} (can be {@code null}).
     * @throws IllegalStateException if this method has not been invoked in the pattern documented above.
     *
     * @since 0.6
     */
    public void restoreContextualUnit(final Unit<?> unit, final Unit<?> previous) {
        if (previous == null) {
            if (unit != null && units.remove(unit.getSystemUnit()) != unit) {
                /*
                 * The unit that we removed was not the expected one. Probably the user has invoked
                 * addContextualUnit(…) again without a matching call to `restoreContextualUnit(…)`.
                 * However, this check does not work in `Convention.WKT1_COMMON_UNITS` mode, since the
                 * map is always empty in that mode.
                 */
                if (!convention.usesCommonUnits) {
                    throw new IllegalStateException();
                }
            }
            hasContextualUnit &= ~1;
        } else if (units.put(previous.getSystemUnit(), previous) != unit) {
            /*
             * The unit that we replaced was not the expected one. Probably the user has invoked
             * addContextualUnit(…) again without a matching call to `restoreContextualUnit(…)`.
             * Note that this case should never happen in `Convention.WKT1_COMMON_UNITS` mode,
             * since `previous` should never be non-null in that mode (if the user followed
             * the documented pattern).
             */
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the unit to use instead of the given one, or {@code unit} if there is no replacement.
     * This method searches for a unit specified by {@link #addContextualUnit(Unit)}
     * which {@linkplain Unit#isCompatible(Unit) is compatible} with the given unit.
     *
     * @param  <Q>   the quantity of the unit.
     * @param  unit  the unit to replace by the contextual unit, or {@code null}.
     * @return a contextual unit compatible with the given unit, or {@code unit}
     *         (which may be null) if no contextual unit has been found.
     */
    public <Q extends Quantity<Q>> Unit<Q> toContextualUnit(final Unit<Q> unit) {
        if (unit != null) {
            @SuppressWarnings("unchecked")
            final Unit<Q> candidate = (Unit<Q>) units.get(unit.getSystemUnit());
            if (candidate != null) {
                return candidate;
            }
        }
        return unit;
    }

    /**
     * Returns {@code true} if the WKT written by this formatter is not strictly compliant to the WKT specification.
     * This method returns {@code true} if {@link #setInvalidWKT(IdentifiedObject, Exception)} has been invoked at
     * least once. The action to take regarding invalid WKT is caller-dependent.
     * For example, {@link FormattableObject#toString()} will accepts loose WKT formatting and ignore
     * this flag, while {@link FormattableObject#toWKT()} requires strict WKT formatting and will
     * thrown an exception if this flag is set.
     *
     * @return {@code true} if the WKT is invalid.
     */
    public boolean isInvalidWKT() {
        return (warnings != null) || (buffer != null && buffer.length() == 0);
        /*
         * Note: we really use a "and" condition (not another "or") for the buffer test because
         *       the buffer is reset to `null` by WKTFormat after a successfull formatting.
         */
    }

    /**
     * Marks the current WKT representation of the given object as not strictly compliant with the WKT specification.
     * This method can be invoked by implementations of {@link FormattableObject#formatTo(Formatter)} when the object
     * to format is more complex than what the WKT specification allows.
     * Applications can test {@link #isInvalidWKT()} later for checking WKT validity.
     *
     * @param  unformattable  the object that cannot be formatted,
     * @param  cause  the cause for the failure to format, or {@code null} if the cause is not an exception.
     */
    public void setInvalidWKT(final IdentifiedObject unformattable, final Exception cause) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        String name;
        final Identifier id = unformattable.getName();
        if (id == null || (name = id.getCode()) == null) {
            name = getName(unformattable.getClass());
        }
        setInvalidWKT(name, cause);
    }

    /**
     * Marks the current WKT representation of the given class as not strictly compliant with the WKT specification.
     * This method can be used as an alternative to {@link #setInvalidWKT(IdentifiedObject, Exception)} when the
     * problematic object is not an instance of {@code IdentifiedObject}.
     *
     * @param  unformattable  the class of the object that cannot be formatted,
     * @param  cause  the cause for the failure to format, or {@code null} if the cause is not an exception.
     */
    public void setInvalidWKT(final Class<?> unformattable, final Exception cause) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        setInvalidWKT(getName(unformattable), cause);
    }

    /**
     * Implementation of public {@code setInvalidWKT(…)} methods.
     * The message is stored as an {@link InternationalString}
     * in order to defer the actual message formatting until needed.
     */
    private void setInvalidWKT(final String invalidElement, final Exception cause) {
        warnings().add(Errors.formatInternational(Errors.Keys.CanNotRepresentInFormat_2, "WKT", invalidElement), cause, null);
        highlightError = true;
    }

    /**
     * Returns the name of the GeoAPI interface implemented by the given class.
     * If no GeoAPI interface is found, fallback on the class name.
     */
    private static String getName(Class<?> unformattable) {
        if (!unformattable.isInterface()) {
            for (final Class<?> candidate : unformattable.getInterfaces()) {
                if (candidate.getName().startsWith("org.opengis.")) {
                    unformattable = candidate;
                    break;
                }
            }
        }
        return Classes.getShortName(unformattable);
    }

    /**
     * Adds a warning for an exception that occurred while fetching an optional property.
     *
     * @param e        the exception that occurred.
     * @param element  WKT keyword of the element where the exception occurred.
     * @param parent   WKT keyword of the parent element.
     */
    private void warning(final Exception e, final String element, final String parent) {
        warnings().add(null, e, new String[] {element, parent});
    }

    /**
     * Returns the object where to store warnings.
     */
    private Warnings warnings() {
        if (warnings == null) {
            warnings = new Warnings(errorLocale, false, Map.of());
        }
        return warnings;
    }

    /**
     * Returns the warnings, or {@code null} if none.
     */
    final Warnings getWarnings() {
        return warnings;
    }

    /**
     * Appends the warnings after the WKT string. If there are no warnings, then this method does nothing.
     * If this method is invoked, then it shall be the last method before {@link #toWKT()}.
     */
    final void appendWarnings() throws IOException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Warnings warnings = this.warnings;                    // Protect against accidental changes.
        if (warnings != null) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final StringBuffer buffer = this.buffer;
            final String ln = System.lineSeparator();
            buffer.append(ln).append(ln);
            if (colors != null) {
                buffer.append(X364.FOREGROUND_YELLOW.sequence());
            }
            buffer.append(warnings);
            if (colors != null) {
                buffer.append(X364.RESET.sequence());
            }
        }
    }

    /**
     * Returns the WKT formatted by this object.
     *
     * @return the WKT formatted by this formatter.
     */
    public String toWKT() {
        return buffer.toString();
    }

    /**
     * Returns a string representation of this formatter for debugging purpose.
     *
     * @return a string representation of this formatter.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(Classes.getShortClassName(this));
        String separator = " of ";
        for (int i=enclosingElements.size(); --i >= 0;) {
            b.append(separator).append(Classes.getShortClassName(enclosingElements.get(i)));
            separator = " inside ";
        }
        return b.toString();
    }

    /**
     * Clears this formatter before formatting a new object.
     * This method clears also the {@linkplain #isInvalidWKT() WKT validity flag}.
     */
    final void clear() {
        /*
         * Configuration options (indentation, colors, conventions) are left unchanged.
         * We do not mention that fact in the Javadoc because those options do not appear
         * in the Formatter public API (they are in the WKTFormat API instead).
         */
        if (buffer != null) {
            buffer.setLength(0);
        }
        enclosingElements.clear();
        units.clear();
        hasContextualUnit = 0;
        elementStart      = 0;
        colorApplied      = 0;
        margin            = 0;
        keywordSpaceAt    = null;
        requestNewLine    = false;
        isComplement      = false;
        highlightError    = false;
        warnings          = null;
    }
}
