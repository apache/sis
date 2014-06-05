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
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.quantity.Quantity;

import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.util.CodeList;

import org.apache.sis.measure.Units;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Localized;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.extent.Extents;


/**
 * Provides support methods for formatting a <cite>Well Known Text</cite> (WKT).
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
 * @since   0.5 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public class Formatter implements Localized {
    /**
     * Accuracy of geographic bounding boxes, in number of fraction digits.
     * We use the accuracy recommended by ISO 19162.
     */
    static final int BBOX_ACCURACY = 2;

    /**
     * Maximal accuracy of vertical extents, in number of fraction digits.
     * The value used here is arbitrary and may change in any future SIS version.
     */
    private static final int VERTICAL_ACCURACY = 3;

    /**
     * The value of {@code X364.FOREGROUND_DEFAULT.sequence()}, hard-coded for avoiding
     * {@link org.apache.sis.internal.util.X364} class loading.
     */
    static final String FOREGROUND_DEFAULT = "\u001B[39m";

    /**
     * The value of {@code X364.BACKGROUND_DEFAULT.sequence()}, hard-coded for avoiding
     * {@link org.apache.sis.internal.util.X364} class loading.
     */
    static final String BACKGROUND_DEFAULT = "\u001B[49m";

    /**
     * The locale for the localization of international strings.
     * This is not the same than {@link Symbols#getLocale()}.
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
     * Computed by {@link Symbols#lineSeparator()} and stored for reuse.
     */
    private final String lineSeparator;

    /**
     * The colors to use for this formatter, or {@code null} for no syntax coloring.
     * If non-null, the terminal must be ANSI X3.64 compatible.
     * The default value is {@code null}.
     *
     * @see #configure(Convention, Citation, Colors, boolean, byte)
     */
    private Colors colors;

    /**
     * The preferred convention for objects or parameter names.
     * This field should never be {@code null}.
     *
     * @see #configure(Convention, Citation, Colors, boolean, byte)
     */
    private Convention convention;

    /**
     * The preferred authority for objects or parameter names.
     *
     * @see #configure(Convention, Citation, Colors, boolean, byte)
     */
    private Citation authority;

    /**
     * The enclosing WKT element being formatted.
     *
     * @see #getEnclosingElement(int)
     */
    private final List<FormattableObject> enclosingElements = new ArrayList<FormattableObject>();

    /**
     * The contextual units for writing lengths, angles or other type of measurements.
     * A unit not present in this map means that the "natural" unit of the WKT element shall be used.
     * This value is set for example by {@code "GEOGCS"}, which force its enclosing {@code "PRIMEM"}
     * to take the same units than itself.
     *
     * @see #addContextualUnit(Unit)
     * @see #toContextualUnit(Unit)
     */
    private final Map<Unit<?>, Unit<?>> units = new HashMap<Unit<?>, Unit<?>>(4);

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
     * {@code true} if keywords shall be converted to upper cases.
     *
     * @see #configure(Convention, Citation, Colors, boolean, byte)
     */
    private boolean toUpperCase;

    /**
     * Incremented when {@link #setColor(ElementKind)} is invoked, and decremented when {@link #resetColor()}
     * is invoked. Used in order to prevent child elements to overwrite the colors decided by enclosing elements.
     */
    private int colorApplied;

    /**
     * The amount of spaces to use in indentation, or {@value org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE}
     * if indentation is disabled.
     *
     * @see #configure(Convention, Citation, Colors, boolean, byte)
     */
    private byte indentation;

    /**
     * The amount of space to write on the left side of each line. This amount is increased
     * by {@code indentation} every time a {@link FormattableObject} is appended in a new
     * indentation level.
     */
    private int margin;

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
     * @see #appendComplement(IdentifiedObject, boolean)
     */
    private boolean isComplement;

    /**
     * {@code true} if the last formatted element was invalid WKT and shall be highlighted with syntactic coloration.
     * This field has no effect if {@link #colors} is null. This field is reset to {@code false} after the invalid
     * part has been processed by {@link #append(FormattableObject)}, in order to highlight only the first erroneous
     * element without clearing the {@link #invalidElement} value.
     */
    private boolean highlightError;

    /**
     * Non-null if the WKT is invalid. If non-null, then this field contains a keyword that identify the
     * problematic part.
     *
     * @see #isInvalidWKT()
     * @see #getErrorMessage()
     */
    private String invalidElement;

    /**
     * Error that occurred during WKT formatting, or {@code null} if none.
     *
     * @see #getErrorCause()
     */
    private Exception errorCause;

    /**
     * Creates a new formatter instance with the default configuration.
     */
    public Formatter() {
        this(Convention.DEFAULT, Symbols.getDefault(), WKTFormat.DEFAULT_INDENTATION);
    }

    /**
     * Creates a new formatter instance with the specified convention, symbols and indentation.
     *
     * @param convention  The convention to use.
     * @param symbols     The symbols.
     * @param indentation The amount of spaces to use in indentation for WKT formatting,
     *        or {@link WKTFormat#SINGLE_LINE} for formatting the whole WKT on a single line.
     */
    public Formatter(final Convention convention, final Symbols symbols, final int indentation) {
        ArgumentChecks.ensureNonNull("convention",  convention);
        ArgumentChecks.ensureNonNull("symbols",     symbols);
        ArgumentChecks.ensureBetween("indentation", WKTFormat.SINGLE_LINE, Byte.MAX_VALUE, indentation);
        this.locale        = Locale.getDefault();
        this.convention    = convention;
        this.authority     = convention.getNameAuthority();
        this.symbols       = symbols.immutable();
        this.lineSeparator = this.symbols.lineSeparator();
        this.indentation   = (byte) indentation;
        this.numberFormat  = symbols.createNumberFormat();
        this.dateFormat    = new SimpleDateFormat(WKTFormat.DATE_PATTERN, symbols.getLocale());
        this.unitFormat    = UnitFormat.getInstance(symbols.getLocale());
        this.buffer        = new StringBuffer();
    }

    /**
     * Constructor for private use by {@link WKTFormat#getFormatter()} only. This allows to use the number
     * format created by {@link WKTFormat#createFormat(Class)}, which may be overridden by the user.
     */
    Formatter(final Locale locale, final Symbols symbols, final NumberFormat numberFormat,
            final DateFormat dateFormat, final UnitFormat unitFormat)
    {
        this.locale        = locale;
        this.convention    = Convention.DEFAULT;
        this.authority     = Convention.DEFAULT.getNameAuthority();
        this.symbols       = symbols;
        this.lineSeparator = this.symbols.lineSeparator();
        this.indentation   = WKTFormat.DEFAULT_INDENTATION;
        this.numberFormat  = numberFormat; // No clone needed.
        this.dateFormat    = dateFormat;   // No clone needed.
        this.unitFormat    = unitFormat;   // No clone needed.
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
     * @param convention  The convention, or {@code null} for the default value.
     * @param authority   The authority, or {@code null} for inferring it from the convention.
     * @param colors      The syntax coloring, or {@code null} if none.
     * @param toUpperCase Whether keywords shall be converted to upper cases.
     * @param indentation The amount of spaces to use in indentation for WKT formatting,
     *                    or {@link WKTFormat#SINGLE_LINE}.
     */
    final void configure(Convention convention, final Citation authority, final Colors colors,
            final boolean toUpperCase, final byte indentation)
    {
        this.convention  = convention;
        this.authority   = (authority != null) ? authority : convention.getNameAuthority();
        this.colors      = colors;
        this.toUpperCase = toUpperCase;
        this.indentation = indentation;
    }

    /**
     * Returns the convention to use for formatting the WKT. The default is {@link Convention#WKT2}.
     *
     * @return The convention (never {@code null}).
     *
     * @see WKTFormat#setConvention(Convention)
     * @see FormattableObject#toString(Convention)
     */
    public final Convention getConvention() {
        return convention;
    }

    /**
     * Returns the preferred authority for choosing the projection and parameter names.
     *
     * <p>The preferred authority can be set by the {@link WKTFormat#setNameAuthority(Citation)} method.
     * This is not necessarily the authority who created the object to format.</p>
     *
     * <div class="note"><b>Example:</b>
     * The EPSG name of the {@code EPSG:6326} datum is "<cite>World Geodetic System 1984</cite>".
     * However if the preferred authority is OGC, then the formatted datum name will rather look like
     * "<cite>WGS84</cite>" (the exact string depends on the object aliases).</div>
     *
     * @return The authority for projection and parameter names.
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
     * @return The locale to use for localizing international strings.
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
                    return; // Do not increment 'colorApplied' for giving a chance to children to apply their colors.
                }
                buffer.append(color);
            }
            colorApplied++;
        }
    }

    /**
     * Appends in the {@linkplain #buffer} the ANSI escape sequence for reseting the color to the default.
     * This method does nothing unless syntax coloring has been explicitly enabled.
     */
    private void resetColor() {
        if (colors != null && --colorApplied <= 0) {
            colorApplied = 0;
            buffer.append(FOREGROUND_DEFAULT);
        }
    }

    /**
     * Request a line separator before the next element to format. Invoking this method before any
     * {@code append(…)} method call will cause the next element to appear on the next line.
     *
     * <p>This method has no effect in any of the following cases:</p>
     * <ul>
     *   <li>WKT formatting has not yet started.</li>
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
     * the indentation by the amount of spaces specified at construction time,
     * and a value of {@code -1} reduces it by the same amount.
     *
     * @param amount +1 for increasing the indentation, or -1 for decreasing it, or 0 for no-op.
     */
    public void indent(final int amount) {
        margin = Math.max(0, margin + indentation*amount);
    }

    /**
     * Conditionally appends a separator to the {@linkplain #buffer}, if needed.
     * This method does nothing if there is currently no element at the buffer end.
     */
    private void appendSeparator() {
        if (buffer.length() != elementStart) {
            if (requestNewLine) {
                buffer.append(lineSeparator).append(CharSequences.spaces(margin));
            } else {
                buffer.append(symbols.getSeparator());
            }
        }
        requestNewLine = false;
    }

    /**
     * Appends a separator if needed, then opens a new element.
     *
     * @param newLine {@code true} for invoking {@link #newLine()} first.
     * @param keyword The element keyword (e.g. {@code "DATUM"}, {@code "AXIS"}, <i>etc</i>).
     */
    private void openElement(final boolean newLine, String keyword) {
        if (newLine) {
            newLine();
        }
        appendSeparator();
        if (toUpperCase) {
            keyword = keyword.toUpperCase(symbols.getLocale());
        }
        elementStart = buffer.append(keyword).appendCodePoint(symbols.getOpeningBracket(0)).length();
    }

    /**
     * Closes the element opened by {@link #openElement(String)}.
     *
     * @param newLine {@code true} for invoking {@link #newLine()} last.
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
     * @param object The formattable object to append to the WKT, or {@code null} if none.
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
                throw new IllegalStateException(Errors.getResources(locale).getString(Errors.Keys.CircularReference));
            }
        }
        enclosingElements.add(object);
        if (hasContextualUnit < 0) { // Test if leftmost bit is set to 1.
            throw new IllegalStateException(Errors.getResources(locale).getString(Errors.Keys.TreeDepthExceedsMaximum));
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
        final IdentifiedObject info = (object instanceof IdentifiedObject) ? (IdentifiedObject) object : null;
        String keyword = object.formatTo(this);
        if (keyword == null) {
            if (info != null) {
                setInvalidWKT(info, null);
                keyword = getName(info.getClass());
            } else {
                setInvalidWKT(object.getClass(), null);
                keyword = invalidElement;
            }
        } else if (toUpperCase) {
            keyword = keyword.toUpperCase(symbols.getLocale());
        }
        if (highlightError && colors != null) {
            final String color = colors.getAnsiSequence(ElementKind.ERROR);
            if (color != null) {
                buffer.insert(base, color + BACKGROUND_DEFAULT);
                base += color.length();
            }
        }
        highlightError = false;
        buffer.insert(base, keyword);
        /*
         * Format the SCOPE["…"], AREA["…"] and other elements. Some of those information
         * are available only for Datum, CoordinateOperation and ReferenceSystem objects.
         */
        if (info != null) {
            appendComplement(info, stackDepth == 0);
        }
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
     * <p>The {@code ID[<name>,<code>,…]} element is written only for the root element, unless the convention are
     * INTERNAL. If formatted, the ID element will be on the same line than the enclosing one if no line separator
     * were requested (e.g. SPHEROID["Clarke 1866", …, ID["EPSG", 7008]]), or on a new line otherwise. Example:</p>
     *
     * {@preformat text
     *   PROJCS["NAD27 / Idaho Central",
     *     GEOGCS[...etc...],
     *     ...etc...
     *     ID["EPSG", 26769]]
     * }
     *
     * For non-internal conventions, all elements other than {@code ID[…]} are formatted
     * only for {@link CoordinateOperation} and {@link ReferenceSystem} types.
     * In the later case, we also require that the CRS is not the base of a derived CRS.
     * Those restrictions are our interpretation of the following ISO 19162 requirement:
     *
     * <blockquote>(…snip…) {@code <scope extent identifier remark>} is a collection of four optional attributes
     * which may be applied to a coordinate reference system, a coordinate operation or a boundCRS. (…snip…)
     * Identifier (…snip…) may also be utilised for components of these objects although this is not recommended
     * except for coordinate operation methods (including map projections) and parameters. (…snip…)
     * A {@code <remark>} can be included within the descriptions of source and target CRS embedded within
     * a coordinate transformation as well as within the coordinate transformation itself.</blockquote>
     */
    private void appendComplement(final IdentifiedObject object, final boolean isRoot) {
        isComplement = true;
        final boolean showIDs;      // Whether to format ID[…] elements.
        final boolean filterID;     // Whether we shall limit to a single ID[…] element.
        final boolean showOthers;   // Whether to format any element other than ID[…].
        if (convention == Convention.INTERNAL) {
            showIDs    = true;
            filterID   = false;
            showOthers = true;
        } else {
            if (convention == Convention.WKT2_SIMPLIFIED) {
                showIDs = isRoot;
            } else {
                showIDs = isRoot || (object instanceof OperationMethod) || (object instanceof GeneralParameterDescriptor);
            }
            if (convention.majorVersion() == 1) {
                filterID   = true;
                showOthers = false;
            } else {
                filterID = !isRoot;
                if (object instanceof CoordinateOperation) {
                    showOthers = true;
                } else if (object instanceof ReferenceSystem) {
                    showOthers = !(getEnclosingElement(1) instanceof ReferenceSystem);
                } else {
                    showOthers = false; // Mandated by ISO 19162.
                }
            }
        }
        if (showOthers) {
            appendForSubtypes(object);
        }
        if (showIDs) {
            Collection<ReferenceIdentifier> identifiers = object.getIdentifiers();
            if (identifiers != null) { // Paranoiac check
                if (filterID) {
                    for (final ReferenceIdentifier id : identifiers) {
                        if (Citations.identifierMatches(authority, id.getAuthority())) {
                            identifiers = Collections.singleton(id);
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
        if (showOthers) {
            appendOnNewLine("Remarks", object.getRemarks(), ElementKind.REMARKS);
        }
        isComplement = false;
    }

    /**
     * Appends the anchor, scope and domain of validity of the given object. Those information are available
     * only for {@link ReferenceSystem}, {@link Datum} and {@link CoordinateOperation} objects.
     */
    private void appendForSubtypes(final IdentifiedObject object) {
        final InternationalString anchor, scope;
        final Extent area;
        if (object instanceof ReferenceSystem) {
            anchor = null;
            scope  = ((ReferenceSystem) object).getScope();
            area   = ((ReferenceSystem) object).getDomainOfValidity();
        } else if (object instanceof Datum) {
            anchor = ((Datum) object).getAnchorPoint();
            scope  = ((Datum) object).getScope();
            area   = ((Datum) object).getDomainOfValidity();
        } else if (object instanceof CoordinateOperation) {
            anchor = null;
            scope  = ((CoordinateOperation) object).getScope();
            area   = ((CoordinateOperation) object).getDomainOfValidity();
        } else {
            return;
        }
        appendOnNewLine("Anchor", anchor, null);
        appendOnNewLine("Scope", scope, ElementKind.SCOPE);
        if (area != null) {
            appendOnNewLine("Area", area.getDescription(), ElementKind.EXTENT);
            append(Extents.getGeographicBoundingBox(area), BBOX_ACCURACY);
            final MeasurementRange<Double> range = Extents.getVerticalRange(area);
            if (range != null) {
                openElement(true, "VerticalExtent");
                setColor(ElementKind.EXTENT);
                numberFormat.setMinimumFractionDigits(0);
                numberFormat.setMaximumFractionDigits(VERTICAL_ACCURACY);
                numberFormat.setRoundingMode(RoundingMode.FLOOR);
                appendPreset(range.getMinDouble());
                numberFormat.setRoundingMode(RoundingMode.CEILING);
                appendPreset(range.getMaxDouble());
                final Unit<?> unit = range.unit();
                if (!convention.isSimplified() || !SI.METRE.equals(unit)) {
                    append(unit); // Unit are optional if they are metres.
                }
                resetColor();
                closeElement(true);
            }
            final Range<Date> timeRange = Extents.getTimeRange(area);
            if (timeRange != null) {
                openElement(true, "TimeExtent");
                setColor(ElementKind.EXTENT);
                append(timeRange.getMinValue());
                append(timeRange.getMaxValue());
                resetColor();
                closeElement(true);
            }
        }
    }

    /**
     * Appends the given geographic bounding box in a {@code BBOX[…]} element.
     * Longitude and latitude values will be formatted in decimal degrees.
     * Longitudes are relative to the Greenwich meridian, with values increasing toward East.
     * Latitudes values are increasing toward North.
     *
     * {@section Numerical precision}
     * The ISO 19162 standards recommends to format those values with only 2 decimal digits.
     * This is because {@code GeographicBoundingBox} does not specify the datum, so this box
     * is an approximative information only.
     *
     * @param bbox The geographic bounding box to append to the WKT, or {@code null}.
     * @param fractionDigits The number of fraction digits to use. The recommended value is 2.
     */
    public void append(final GeographicBoundingBox bbox, final int fractionDigits) {
        if (bbox != null) {
            openElement(isComplement, "BBox");
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
     * Appends the given math transform, typically (but not necessarily) in a {@code PARAM_MT[…]} element.
     *
     * @param transform The transform object to append to the WKT, or {@code null} if none.
     */
    public void append(final MathTransform transform) {
        if (transform != null) {
            if (transform instanceof FormattableObject) {
                append((FormattableObject) transform);
            } else {
                final Matrix matrix = ReferencingServices.getInstance().getMatrix(transform);
                if (matrix != null) {
                    openElement(true, "Param_MT");
                    quote("Affine");
                    indent(+1);
                    append(matrix);
                    indent(-1);
                    closeElement(true);
                } else {
                    throw new UnformattableObjectException(Errors.format(
                            Errors.Keys.IllegalClass_2, FormattableObject.class, transform.getClass()));
                }
            }
        }
    }

    /**
     * Appends the given matrix as a sequence of {@code PARAMETER[…]} elements.
     * Only elements different than the default values are appended.
     * The default values are 1 on the matrix diagonal and 0 elsewhere.
     *
     * @param matrix The matrix to append to the WKT, or {@code null} if none.
     */
    public void append(final Matrix matrix) {
        if (matrix == null) {
            return;
        }
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        final int openQuote  = symbols.getOpeningQuote(0);
        final int closeQuote = symbols.getClosingQuote(0);
        boolean columns = false;
        do {
            openElement(true, "Parameter");
            quote(columns ? "num_col" : "num_row");
            append(columns ? numCol : numRow);
            closeElement(false);
        } while ((columns = !columns) == true);
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double element = matrix.getElement(j, i);
                if (element != (i == j ? 1 : 0)) {
                    openElement(true, "Parameter");
                    setColor(ElementKind.PARAMETER);
                    buffer.appendCodePoint(openQuote).append("elt_").append(j)
                            .append('_').append(i).appendCodePoint(closeQuote);
                    resetColor();
                    append(element);
                    closeElement(false);
                }
            }
        }
    }

    /**
     * Appends an international text in an element having the given keyword. Since this method
     * is typically invoked for long descriptions, the element will be written on its own line.
     *
     * <div class="note"><b>Example:</b>
     *   <ul>
     *     <li>{@code Scope["Large scale topographic mapping and cadastre."]}</li>
     *     <li>{@code Area["Netherlands offshore."]}</li>
     *   </ul>
     * </div>
     *
     * @param keyword The {@linkplain KeywordCase#CAMEL_CASE camel-case} keyword.
     *                Example: {@code "Scope"}, {@code "Area"} or {@code "Remarks"}.
     * @param text The text, or {@code null} if none.
     * @param type The key of the colors to apply if syntax coloring is enabled.
     */
    private void appendOnNewLine(final String keyword, final InternationalString text, final ElementKind type) {
        ArgumentChecks.ensureNonNull("keyword", keyword);
        if (text != null) {
            final String localized = CharSequences.trimWhitespaces(text.toString(locale));
            if (localized != null && !localized.isEmpty()) {
                openElement(true, keyword);
                setColor(type);
                quote(localized);
                resetColor();
                closeElement(true);
            }
        }
    }

    /**
     * Appends a character string between quotes.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the text if needed.
     *
     * @param text The string to format to the WKT, or {@code null} if none.
     * @param type The key of the colors to apply if syntax coloring is enabled.
     */
    public void append(final String text, final ElementKind type) {
        if (text != null) {
            appendSeparator();
            setColor(type);
            quote(text);
            resetColor();
        }
    }

    /**
     * Appends the given string as a quoted text. If the given string contains the closing quote character,
     * that character will be doubled (WKT 2) or deleted (WKT 1). We check for the closing quote only because
     * it is the character that the parser will look for determining the text end.
     */
    private void quote(final String text) {
        final int base = buffer.appendCodePoint(symbols.getOpeningQuote(0)).length();
        buffer.append(text);
        closeQuote(base);
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
     * Appends a code list.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the code list if needed.
     *
     * @param code The code list to append to the WKT, or {@code null} if none.
     */
    public void append(final CodeList<?> code) {
        if (code != null) {
            appendSeparator();
            setColor(ElementKind.CODE_LIST);
            buffer.append(convention.majorVersion() == 1 ? code.name() : Types.getCodeName(code));
            resetColor();
        }
    }

    /**
     * Appends a date.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the date if needed.
     *
     * @param date The date to append to the WKT, or {@code null} if none.
     */
    public void append(final Date date) {
        if (date != null) {
            appendSeparator();
            dateFormat.format(date, buffer, dummy);
        }
    }

    /**
     * Appends a boolean value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the boolean if needed.
     *
     * @param value The boolean to append to the WKT.
     */
    public void append(final boolean value) {
        appendSeparator();
        buffer.append(value ? "TRUE" : "FALSE");
    }

    /**
     * Appends an integer value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param number The integer to append to the WKT.
     */
    public void append(final long number) {
        appendSeparator();
        setColor(ElementKind.INTEGER);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends an floating point value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param number The floating point value to append to the WKT.
     */
    public void append(final double number) {
        appendSeparator();
        setColor(ElementKind.NUMBER);
        /*
         * The 2 below is for using two less fraction digits than the expected number accuracy.
         * The intend is to give to DecimalFormat a chance to hide rounding errors, keeping in
         * mind that the number value is not necessarily the original one (we may have applied
         * a unit conversion). In the case of WGS84 semi-major axis in metres, we still have a
         * maximum of 8 fraction digits, which is more than enough.
         */
        numberFormat.setMaximumFractionDigits(DecimalFunctions.fractionDigitsForValue(number, 2));
        numberFormat.setMinimumFractionDigits(1); // Must be after setMaximumFractionDigits(…).
        numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        numberFormat.format(number, buffer, dummy);
        resetColor();
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
            setColor(highlightError ? ElementKind.ERROR : ElementKind.NUMBER);
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
        highlightError = false;
    }

    /**
     * Appends a unit in a {@code UNIT[…]} element or one of the specialized elements. Specialized elements are
     * {@code ANGLEUNIT}, {@code LENGTHUNIT}, {@code SCALEUNIT}, {@code PARAMETRICUNIT} and {@code TIMEUNIT}.
     * Specialization is used in WKT 2 format except the <cite>simplified WKT 2</cite> one.
     *
     * <div class="note"><b>Example:</b>
     * {@code append(SI.KILOMETRE)} will append "{@code LENGTHUNIT["km", 1000]}" to the WKT.</div>
     *
     * @param unit The unit to append to the WKT, or {@code null} if none.
     */
    public void append(final Unit<?> unit) {
        if (unit != null) {
            String keyword = "Unit";
            if (!convention.isSimplified()) {
                if (Units.isLinear(unit)) {
                    keyword = "LengthUnit";
                } else if (Units.isAngular(unit)) {
                    keyword = "AngleUnit";
                } else if (Units.isScale(unit)) {
                    keyword = "ScaleUnit";
                } else if (Units.isTemporal(unit)) {
                    keyword = "TimeUnit";
                }
            }
            openElement(false, keyword);
            setColor(ElementKind.UNIT);
            final int fromIndex = buffer.appendCodePoint(symbols.getOpeningQuote(0)).length();
            if (NonSI.DEGREE_ANGLE.equals(unit)) {
                buffer.append("degree");
            } else if (SI.METRE.equals(unit)) {
                buffer.append(convention.usesCommonUnits() ? "meter" : "metre");
            } else if (Units.PPM.equals(unit)) {
                buffer.append("parts per million");
            } else {
                unitFormat.format(unit, buffer, dummy);
            }
            closeQuote(fromIndex);
            resetColor();
            final double conversion = Units.toStandardUnit(unit);
            if (!(conversion > 0)) { // ISO 19162 requires the conversion factor to be positive.
                setInvalidWKT(Unit.class, null);
            }
            appendExact(conversion);
            closeElement(false);
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
     * @param value The value to append to the WKT, or {@code null}.
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
        if (value.getClass().isArray()) {
            appendSeparator();
            buffer.appendCodePoint(symbols.getOpenSequence());
            final int length = Array.getLength(value);
            for (int i=0; i<length; i++) {
                appendAny(Array.get(value, i));
            }
            buffer.appendCodePoint(symbols.getCloseSequence());
        } else if (value instanceof Number) {
            final Number number = (Number) value;
            if (Numbers.isInteger(number.getClass())) {
                append(number.longValue());
            } else {
                append(number.doubleValue());
            }
        }
        else if (value instanceof CodeList<?>) append((CodeList<?>) value);
        else if (value instanceof Date)        append((Date)        value);
        else if (value instanceof Boolean)     append((Boolean)     value);
        else if (value instanceof CharSequence) {
            append((value instanceof InternationalString) ?
                    ((InternationalString) value).toString(locale) : value.toString(), null);
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
            append(ReferencingServices.getInstance().toFormattableObject((IdentifiedObject) value));
        }
        else if (value instanceof GeographicBoundingBox) append((GeographicBoundingBox) value, BBOX_ACCURACY);
        else if (value instanceof MathTransform)         append((MathTransform)         value);
        else if (value instanceof Matrix)                append((Matrix)                value);
        else if (value instanceof Unit<?>)               append((Unit<?>)               value);
        else return false;
        return true;
    }

    /**
     * Delegates the formatting to another {@link FormattableObject} implementation.
     * Invoking this method is equivalent to first verifying the {@code other} class,
     * then delegating as below:
     *
     * {@preformat
     *     return other.formatTo(this);
     * }
     *
     * This method is useful for {@code FormattableObject} which are wrapper around another object.
     * It allows to delegate the WKT formatting to the wrapped object.
     *
     * @param  other The object to format with this formatter.
     * @return The value returned by {@link FormattableObject#formatTo(Formatter)}.
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
     * @param  depth 1 for the immediate parent, 2 for the parent of the parent, <i>etc.</i>
     * @return The parent element at the given depth, or {@code null}.
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
     * <div class="note"><b>Note:</b>
     * The main purpose of this method is to allow {@code AXIS[…]} elements to determine if they should
     * inherit the unit specified by the enclosing CRS, or if they should specify their unit explicitly.</div>
     *
     * @param  depth 1 for the immediate parent, 2 for the parent of the parent, <i>etc.</i>
     * @return Whether the parent element at the given depth has invoked {@code addContextualUnit(…)} at least once.
     */
    public boolean hasContextualUnit(final int depth) {
        ArgumentChecks.ensurePositive("depth", depth);
        return (depth < Long.SIZE) && (hasContextualUnit & (1L << depth)) != 0;
    }

    /**
     * Adds a unit to use for the next measurements of the quantity {@code Q}. The given unit will apply to
     * all WKT elements containing a value of quantity {@code Q} without their own {@code UNIT[…]} element,
     * until the {@link #removeContextualUnit(Unit)} method is invoked with a unit of the same quantity.
     *
     * <p>If the given unit is null, then this method does nothing and returns {@code null}.</p>
     *
     * {@section Special case}
     * If the WKT conventions are {@code WKT1_COMMON_UNITS}, then this method ignores the given unit
     * and returns {@code null}. See {@link Convention#WKT1_COMMON_UNITS} javadoc for more information.
     *
     * @param  <Q>  The unit quantity.
     * @param  unit The contextual unit to add, or {@code null} if none.
     * @return The previous contextual unit for quantity {@code Q}, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public <Q extends Quantity> Unit<Q> addContextualUnit(final Unit<Q> unit) {
        if (unit == null || convention.usesCommonUnits()) {
            return null;
        }
        hasContextualUnit |= 1;
        return (Unit<Q>) units.put(unit.toSI(), unit);
    }

    /**
     * Removes the unit previously added by a call to {@code addContextualUnit(unit)}.
     * If the given unit is null, then this method does nothing.
     *
     * @param unit The contextual unit to remove, or {@code null} if none.
     */
    public void removeContextualUnit(final Unit<?> unit) {
        if (unit != null) {
            units.remove(unit.toSI());
        }
    }

    /**
     * Returns the unit to use instead than the given one, or {@code null} if there is no replacement
     * for {@code unit}. This method searches for a unit specified by {@link #addContextualUnit(Unit)}
     * which {@linkplain Unit#isCompatible(Unit) is compatible} with the given unit.
     *
     * @param  <Q>  The quantity of the unit.
     * @param  unit The unit to replace by the contextual unit, or {@code null}.
     * @return A contextual unit compatible with the given unit, or {@code unit}
     *         if no contextual unit has been found.
     */
    public <Q extends Quantity> Unit<Q> toContextualUnit(final Unit<Q> unit) {
        if (unit != null) {
            @SuppressWarnings("unchecked")
            final Unit<Q> candidate = (Unit<Q>) units.get(unit.toSI());
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the WKT written by this formatter is not strictly compliant to the WKT specification.
     * This method returns {@code true} if {@link #setInvalidWKT(IdentifiedObject, Exception)} has been invoked at
     * least once. The action to take regarding invalid WKT is caller-dependent.
     * For example {@link FormattableObject#toString()} will accepts loose WKT formatting and ignore
     * this flag, while {@link FormattableObject#toWKT()} requires strict WKT formatting and will
     * thrown an exception if this flag is set.
     *
     * @return {@code true} if the WKT is invalid.
     */
    public boolean isInvalidWKT() {
        return (invalidElement != null) || (buffer != null && buffer.length() == 0);
        /*
         * Note: we really use a "and" condition (not an other "or") for the buffer test because
         *       the buffer is reset to 'null' by WKTFormat after a successfull formatting.
         */
    }

    /**
     * Marks the current WKT representation of the given object as not strictly compliant to the WKT specification.
     * This method can be invoked by implementations of {@link FormattableObject#formatTo(Formatter)} when the object
     * to format is more complex than what the WKT specification allows.
     * Applications can test {@link #isInvalidWKT()} later for checking WKT validity.
     *
     * <p>If any {@code setInvalidWKT(…)} method is invoked more than once during formatting,
     * then only information about the first failure will be retained.</p>
     *
     * @param unformattable The object that can not be formatted,
     * @param cause The cause for the failure to format, or {@code null} if the cause is not an exception.
     */
    public void setInvalidWKT(final IdentifiedObject unformattable, final Exception cause) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        if (invalidElement == null) {
            String name;
            final ReferenceIdentifier id = unformattable.getName();
            if (id == null || (name = id.getCode()) == null) {
                name = getName(unformattable.getClass());
            }
            invalidElement = name;
            errorCause     = cause;
        }
        highlightError = true;
    }

    /**
     * Marks the current WKT representation of the given class as not strictly compliant to the WKT specification.
     * This method can be used as an alternative to {@link #setInvalidWKT(IdentifiedObject, Exception)} when the
     * problematic object is not an instance of {@code IdentifiedObject}.
     *
     * <p>If any {@code setInvalidWKT(…)} method is invoked more than once during formatting,
     * then only information about the first failure will be retained.</p>
     *
     * @param unformattable The class of the object that can not be formatted,
     * @param cause The cause for the failure to format, or {@code null} if the cause is not an exception.
     */
    public void setInvalidWKT(final Class<?> unformattable, final Exception cause) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        if (invalidElement == null) {
            invalidElement = getName(unformattable);
            errorCause     = cause;
        }
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
     * Returns the error message {@link #isInvalidWKT()} is set, or {@code null} otherwise.
     * If non-null, a cause may be available in the {@link #getErrorCause()} method.
     */
    final String getErrorMessage() {
        return isInvalidWKT() ? Errors.format(Errors.Keys.CanNotRepresentInFormat_2, "WKT", invalidElement) : null;
    }

    /**
     * Returns the cause of the error, or {@code null} if the cause is not an exception.
     */
    final Exception getErrorCause() {
        return errorCause;
    }

    /**
     * Returns the WKT formatted by this object.
     *
     * @return The WKT formatted by this formatter.
     */
    public String toWKT() {
        return buffer.toString();
    }

    /**
     * Returns a string representation of this formatter for debugging purpose.
     *
     * @return A string representation of this formatter.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(Classes.getShortClassName(this)).append('[');
        String separator = "";
        for (final FormattableObject element : enclosingElements) {
            b.append(separator).append(Classes.getShortClassName(element));
            separator = " 〉 ";
        }
        return b.append(']').toString();
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
        requestNewLine    = false;
        isComplement      = false;
        highlightError    = false;
        invalidElement    = null;
        errorCause        = null;
    }
}
