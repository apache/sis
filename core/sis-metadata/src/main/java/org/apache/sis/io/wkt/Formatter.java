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
import java.util.Collection;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.util.CodeList;

import org.apache.sis.measure.Units;
import org.apache.sis.math.DecimalFunctions;
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
 *   <li>Contextual information. In particular, the {@linkplain #getLinearUnit() linear unit} and the
 *       {@linkplain #getAngularUnit() angular unit} depend on the enclosing WKT element.</li>
 *   <li>A flag for declaring the object unformattable.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public class Formatter implements Localized {
    /**
     * Do not format an {@code ID[…]} element for instance of this class.
     */
    private static final Class<? extends IdentifiedObject> ID_EXCLUDE = CoordinateSystemAxis.class;

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
     * The colors to use for this formatter, or {@code null} for no syntax coloring.
     * If non-null, the terminal must be ANSI X3.64 compatible.
     * The default value is {@code null}.
     *
     * @see #configure(Convention, Citation, Colors, byte)
     */
    private Colors colors;

    /**
     * The preferred convention for objects or parameter names.
     * This field should never be {@code null}.
     *
     * @see #configure(Convention, Citation, Colors, byte)
     */
    private Convention convention;

    /**
     * The preferred authority for objects or parameter names.
     *
     * @see #configure(Convention, Citation, Colors, byte)
     */
    private Citation authority;

    /**
     * The unit for writing length, or {@code null} for the "natural" unit of each WKT element.
     *
     * @see #setLinearUnit(Unit)
     */
    private Unit<Length> linearUnit;

    /**
     * The unit for writing angles, or {@code null} for the "natural" unit of each WKT element.
     * This value is set for example by {@code "GEOGCS"}, which force its enclosing {@code "PRIMEM"}
     * to take the same units than itself.
     *
     * @see #setAngularUnit(Unit)
     */
    private Unit<Angle> angularUnit;

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
     * The starting point in the buffer. Always 0, except when used by
     * {@link WKTFormat#format(Object, Appendable)}.
     *
     * @see #setBuffer(StringBuffer)
     */
    private int bufferBase;

    /**
     * Incremented when {@link #setColor(ElementKind)} is invoked, and decremented when {@link #resetColor()}
     * is invoked. Used in order to prevent child elements to overwrite the colors decided by enclosing elements.
     */
    private int colorApplied;

    /**
     * The amount of spaces to use in indentation, or {@value org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE}
     * if indentation is disabled.
     *
     * @see #configure(Convention, Citation, Colors, byte)
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
     * @see #getErrorMessage()
     */
    Exception errorCause;

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
        this.locale       = Locale.getDefault(Locale.Category.DISPLAY);
        this.convention   = convention;
        this.authority    = convention.getNameAuthority();
        this.symbols      = symbols.immutable();
        this.indentation  = (byte) indentation;
        this.numberFormat = symbols.createNumberFormat();
        this.dateFormat   = new SimpleDateFormat(WKTFormat.DATE_PATTERN, symbols.getLocale());
        this.unitFormat   = UnitFormat.getInstance(symbols.getLocale());
        this.buffer       = new StringBuffer();
    }

    /**
     * Constructor for private use by {@link WKTFormat#getFormatter()} only. This allows to use the number
     * format created by {@link WKTFormat#createFormat(Class)}, which may be overridden by the user.
     */
    Formatter(final Locale locale, final Symbols symbols, final NumberFormat numberFormat,
            final DateFormat dateFormat, final UnitFormat unitFormat)
    {
        this.locale       = locale;
        this.convention   = Convention.DEFAULT;
        this.authority    = Convention.DEFAULT.getNameAuthority();
        this.symbols      = symbols;
        this.indentation  = WKTFormat.DEFAULT_INDENTATION;
        this.numberFormat = numberFormat; // No clone needed.
        this.dateFormat   = dateFormat;   // No clone needed.
        this.unitFormat   = unitFormat;   // No clone needed.
        // Do not set the buffer. It will be set by WKTFormat.format(…).
    }

    /**
     * Sets the destination buffer. Used by {@link WKTFormat#format(Object, Appendable)} only.
     */
    final void setBuffer(final StringBuffer buffer) {
        this.buffer = buffer;
        bufferBase = (buffer != null) ? buffer.length() : 0;
    }

    /**
     * Sets the convention, authority, colors and indentation to use for formatting WKT elements.
     * This method does not validate the argument — validation must be done by the caller.
     *
     * @param convention  The convention, or {@code null} for the default value.
     * @param authority   The authority, or {@code null} for inferring it from the convention.
     * @param colors      The syntax coloring, or {@code null} if none.
     * @param indentation The amount of spaces to use in indentation for WKT formatting,
     *                    or {@link WKTFormat#SINGLE_LINE}.
     */
    final void configure(Convention convention, final Citation authority, final Colors colors, final byte indentation) {
        this.convention  = convention;
        this.authority   = (authority != null) ? authority : convention.getNameAuthority();
        this.colors      = colors;
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
    public Convention getConvention() {
        return convention;
    }

    /**
     * Returns the preferred authority for choosing the projection and parameter names.
     *
     * <p>The preferred authority can be set by the {@link WKTFormat#setNameAuthority(Citation)} method.
     * This is not necessarily the authority who created the object to format.</p>
     *
     * {@example The EPSG name of the <code>EPSG:6326</code> datum is "<cite>World Geodetic System 1984</cite>".
     *           However if the preferred authority is OGC, then the formatted datum name will rather look like
     *           "<cite>WGS84</cite>" (the exact string depends on the object aliases).}
     *
     * @return The authority for projection and parameter names.
     *
     * @see WKTFormat#getNameAuthority()
     * @see org.apache.sis.referencing.IdentifiedObjects#getName(IdentifiedObject, Citation)
     */
    public Citation getNameAuthority() {
        return authority;
    }

    /**
     * Returns the locale to use for localizing {@link InternationalString} instances.
     * This is <em>not</em> the locale for formatting dates and numbers.
     *
     * @return The locale to use for localizing international strings.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the preferred identifier for the specified object.
     * If the specified object contains an identifier from the preferred authority, then this identifier is returned.
     * Otherwise, the first identifier is returned.
     * If the specified object contains no identifier, then this method returns {@code null}.
     *
     * @param  info The object to look for a preferred identifier, or {@code null} if none.
     * @return The preferred identifier, or {@code null} if none.
     */
    private ReferenceIdentifier getIdentifier(final IdentifiedObject info) {
        ReferenceIdentifier first = null;
        if (info != null) {
            final Collection<ReferenceIdentifier> identifiers = info.getIdentifiers();
            if (identifiers != null) {
                for (final ReferenceIdentifier id : identifiers) {
                    if (Citations.identifierMatches(authority, id.getAuthority())) {
                        return id;
                    }
                    if (first == null) {
                        first = id;
                    }
                }
            }
        }
        return first;
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
     * Increase or reduce the indentation. A value of {@code +1} increase
     * the indentation by the amount of spaces specified at construction time,
     * and a value of {@code -1} reduce it.
     */
    private void indent(final int amount) {
        margin = Math.max(0, margin + indentation*amount);
    }

    /**
     * Conditionally appends a separator to the {@linkplain #buffer}, if needed.
     * This method does nothing if there is currently no element at the buffer end.
     *
     * @param newLine If {@code true}, add a line separator too.
     */
    private void appendSeparator(final boolean newLine) {
        final StringBuffer buffer = this.buffer;
        int length = buffer.length();
        int c;
        do {
            if (length <= bufferBase) {
                return; // We are at the buffer beginning.
            }
            c = buffer.codePointBefore(length);
            if (symbols.matchingBracket(c) >= 0 || c == symbols.getOpenSequence()) {
                return; // We are the first item inside a new keyword.
            }
            length -= Character.charCount(c);
        } while (Character.isSpaceChar(c) || c < 32); // c < 32 is for ignoring all control characters.
        buffer.append(symbols.getSeparator());
        if (newLine && indentation > WKTFormat.SINGLE_LINE) {
            buffer.append(System.lineSeparator()).append(CharSequences.spaces(margin));
        }
    }

    /**
     * Appends a separator if needed, then opens a new element.
     *
     * @param keyword The element keyword (e.g. {@code "DATUM"}, {@code "AXIS"}, <i>etc</i>).
     */
    private void openElement(final String keyword) {
        appendSeparator(requestNewLine);
        buffer.append(keyword).appendCodePoint(symbols.getOpeningBracket(0));
    }

    /**
     * Closes the element opened by {@link #openElement(String)}.
     */
    private void closeElement() {
        buffer.appendCodePoint(symbols.getClosingBracket(0));
    }

    /**
     * Appends the given {@code FormattableObject}.
     * This method performs the following steps:
     *
     * <ul>
     *   <li>Invoke <code>formattable.{@linkplain FormattableObject#formatTo(Formatter) formatTo}(this)</code>.</li>
     *   <li>Prepend the keyword returned by the above method call (e.g. {@code "GEOCS"}).</li>
     *   <li>Append the {@code SCOPE[…]} element, if any (WKT 2 only).</li>
     *   <li>Append the {@code AREA[…]} element, if any (WKT 2 only).</li>
     *   <li>Append the {@code BBOX[…]} element, if any (WKT 2 only).</li>
     *   <li>Append the {@code VERTICALEXTENT[…]} element, if any (WKT 2 only).</li>
     *   <li>Append the {@code TIMEEXTENT[…]} element, if any (WKT 2 only).</li>
     *   <li>Append the {@code ID[…]} (WKT 2) or {@code AUTHORITY[…]}} (WKT 1) element, if any.</li>
     *   <li>Append the {@code REMARKS[…]} element, if any (WKT 2 only).</li>
     * </ul>
     *
     * @param object The formattable object to append to the WKT, or {@code null} if none.
     */
    public void append(final FormattableObject object) {
        if (object == null) {
            return;
        }
        final StringBuffer buffer = this.buffer;
        appendSeparator(requestNewLine || !(object instanceof ReferenceIdentifier));
        int base = buffer.length();
        buffer.appendCodePoint(symbols.getOpeningBracket(0));
        final IdentifiedObject info = (object instanceof IdentifiedObject) ? (IdentifiedObject) object : null;
        /*
         * Formats the inner part, then prepend the WKT keyword.
         * The result looks like the following:
         *
         *         <previous text>,
         *           PROJCS["NAD27 / Idaho Central",
         *             GEOGCS[...etc...],
         *             ...etc...
         */
        indent(+1);
        requestNewLine = false;
        String keyword = object.formatTo(this);
        if (keyword == null) {
            if (info != null) {
                setInvalidWKT(info);
                keyword = getName(info.getClass());
            } else {
                setInvalidWKT(object.getClass());
                keyword = invalidElement;
            }
        }
        if (colors != null && highlightError) {
            highlightError = false;
            final String color = colors.getAnsiSequence(ElementKind.ERROR);
            if (color != null) {
                buffer.insert(base, color + BACKGROUND_DEFAULT);
                base += color.length();
            }
        }
        buffer.insert(base, keyword);
        /*
         * Format the SCOPE["…"] and AREA["…"] elements (WKT 2 only). Those information
         * are available only for Datum, CoordinateOperation and ReferenceSystem objects.
         */
        final boolean isWKT1 = convention.isWKT1();
        if (!isWKT1) {
            appendScopeAndArea(object);
        }
        /*
         * Formats the ID[<name>,<code>,…] element. The element will be on the same line than the enclosing
         * one if no line separator were requested (e.g. SPHEROID["Clarke 1866", …, ID["EPSG", 7008]]), or
         * on a new line otherwise. Example:
         *
         *           PROJCS["NAD27 / Idaho Central",
         *             GEOGCS[...etc...],
         *             ...etc...
         *             ID["EPSG", 26769]]
         */
        if (!ID_EXCLUDE.isInstance(info)) {
            ReferenceIdentifier id = getIdentifier(info);
            if (!(id instanceof FormattableObject)) {
                id = ImmutableIdentifier.castOrCopy(id);
            }
            append((FormattableObject) id);
        }
        /*
         * Format remarks if any, and close the element.
         */
        if (!isWKT1 && info != null) {
            append("REMARKS", info.getRemarks(), ElementKind.REMARKS);
        }
        buffer.appendCodePoint(symbols.getClosingBracket(0));
        requestNewLine = true;
        indent(-1);
    }

    /**
     * Appends the scope and domain of validity of the given object. Those information are available
     * only for {@link ReferenceSystem}, {@link Datum} and {@link CoordinateOperation} objects.
     */
    private void appendScopeAndArea(final Object object) {
        final InternationalString scope;
        final Extent area;
        if (object instanceof ReferenceSystem) {
            scope = ((ReferenceSystem) object).getScope();
            area  = ((ReferenceSystem) object).getDomainOfValidity();
        } else if (object instanceof Datum) {
            scope = ((Datum) object).getScope();
            area  = ((Datum) object).getDomainOfValidity();
        } else if (object instanceof CoordinateOperation) {
            scope = ((CoordinateOperation) object).getScope();
            area  = ((CoordinateOperation) object).getDomainOfValidity();
        } else {
            return;
        }
        append("SCOPE", scope, ElementKind.SCOPE);
        if (area != null) {
            append("AREA", area.getDescription(), ElementKind.EXTENT);
            append(Extents.getGeographicBoundingBox(area), BBOX_ACCURACY);
            final MeasurementRange<Double> range = Extents.getVerticalRange(area);
            if (range != null) {
                openElement("VERTICALEXTENT");
                setColor(ElementKind.EXTENT);
                numberFormat.setMinimumFractionDigits(0);
                numberFormat.setMaximumFractionDigits(VERTICAL_ACCURACY);
                numberFormat.setRoundingMode(RoundingMode.FLOOR);
                appendPreset(range.getMinDouble());
                numberFormat.setRoundingMode(RoundingMode.CEILING);
                appendPreset(range.getMaxDouble());
                final Unit<?> unit = range.unit();
                if (!convention.isSimple() || !SI.METRE.equals(unit)) {
                    requestNewLine = false;
                    append(unit); // Unit are optional if they are metres.
                }
                resetColor();
                closeElement();
                requestNewLine = true;
            }
            final Range<Date> timeRange = Extents.getTimeRange(area);
            if (timeRange != null) {
                openElement("TIMEEXTENT");
                setColor(ElementKind.EXTENT);
                append(timeRange.getMinValue());
                append(timeRange.getMaxValue());
                resetColor();
                closeElement();
                requestNewLine = true;
            }
        }
    }

    /**
     * Appends the given {@code IdentifiedObject}.
     *
     * <p>The default implementation delegates to {@link #append(FormattableObject)},
     * after wrapping the given object in an adapter if necessary.</p>
     *
     * @param object The identified object to append to the WKT, or {@code null} if none.
     */
    public void append(IdentifiedObject object) {
        if (object != null) {
            if (!(object instanceof FormattableObject)) {
                object = ReferencingServices.getInstance().toFormattableObject(object);
            }
            append((FormattableObject) object);
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
            openElement("BBOX");
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
            closeElement();
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
                    appendSeparator(true);
                    buffer.append("PARAM_MT").appendCodePoint(symbols.getOpeningBracket(0));
                    quote("Affine");
                    indent(+1);
                    append(matrix);
                    indent(-1);
                    buffer.appendCodePoint(symbols.getClosingBracket(0));
                } else {
                    throw new UnformattableObjectException(Errors.format(
                            Errors.Keys.IllegalClass_2, FormattableObject.class, transform.getClass()));
                }
            }
        }
    }

    /**
     * Appends a sequence of {@code PARAMETER[…]} elements for the given matrix.
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
        requestNewLine = true;
        do {
            openElement("PARAMETER");
            quote(columns ? "num_col" : "num_row");
            append(columns ? numCol : numRow);
            closeElement();
        } while ((columns = !columns) == true);
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double element = matrix.getElement(j, i);
                if (element != (i == j ? 1 : 0)) {
                    openElement("PARAMETER");
                    setColor(ElementKind.PARAMETER);
                    buffer.appendCodePoint(openQuote).append("elt_").append(j)
                            .append('_').append(i).appendCodePoint(closeQuote);
                    resetColor();
                    append(element);
                    closeElement();
                }
            }
        }
    }

    /**
     * Appends a code list.
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>current line</td></tr>
     *   <tr><th>Color key:</th> <td>{@link ElementKind#CODE_LIST}</td></tr>
     * </table></blockquote>
     *
     * @param code The code list to append to the WKT, or {@code null} if none.
     */
    public void append(final CodeList<?> code) {
        if (code != null) {
            appendSeparator(false);
            setColor(ElementKind.CODE_LIST);
            buffer.append(code.name());
            resetColor();
        }
    }

    /**
     * Appends a character string between quotes.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the text if needed.
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>current line</td></tr>
     *   <tr><th>Color key:</th> <td>given</td></tr>
     * </table></blockquote>
     *
     * @param text The string to format to the WKT, or {@code null} if none.
     * @param type The key of the colors to apply if syntax coloring is enabled.
     */
    public void append(final String text, final ElementKind type) {
        if (text != null) {
            appendSeparator(false);
            setColor(type);
            quote(text);
            resetColor();
        }
    }

    /**
     * Appends an international text in an element having the given keyword. Since this method
     * is typically invoked for long descriptions, the element will be written on its own line.
     *
     * {@example
     *   <ul>
     *     <li><code>SCOPE["Large scale topographic mapping and cadastre."]</code></li>
     *     <li><code>AREA["Netherlands offshore."]</code></li>
     *   </ul>
     * }
     *
     * @param keyword The keyword. Example: {@code "SCOPE"}, {@code "AREA"} or {@code "REMARKS"}.
     * @param text The text, or {@code null} if none.
     */
    private void append(final String keyword, final InternationalString text, final ElementKind type) {
        if (text != null) {
            final String localized = CharSequences.trimWhitespaces(text.toString(locale));
            if (localized != null && !localized.isEmpty()) {
                requestNewLine = true;
                openElement(keyword);
                setColor(type);
                quote(localized);
                resetColor();
                closeElement();
            }
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
            if (convention.isWKT1()) {
                buffer.delete(fromIndex, fromIndex + n);
            } else {
                buffer.insert(fromIndex += n, quote);
                fromIndex += n;
            }
        }
        buffer.append(quote);
    }

    /**
     * Appends a date.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the date if needed.
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>current line</td></tr>
     *   <tr><th>Color key:</th> <td>none</td></tr>
     * </table></blockquote>
     *
     * @param date The date to append to the WKT, or {@code null} if none.
     */
    public void append(final Date date) {
        if (date != null) {
            appendSeparator(false);
            dateFormat.format(date, buffer, dummy);
        }
    }

    /**
     * Appends a boolean value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the boolean if needed.
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>current line</td></tr>
     *   <tr><th>Color key:</th> <td>none</td></tr>
     * </table></blockquote>
     *
     * @param value The boolean to append to the WKT.
     */
    public void append(final boolean value) {
        appendSeparator(false);
        buffer.append(value ? "TRUE" : "FALSE");
    }

    /**
     * Appends an integer value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>current line</td></tr>
     *   <tr><th>Color key:</th> <td>{@link ElementKind#INTEGER}</td></tr>
     * </table></blockquote>
     *
     * @param number The integer to append to the WKT.
     */
    public void append(final long number) {
        appendSeparator(false);
        setColor(ElementKind.INTEGER);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends an floating point value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>current line</td></tr>
     *   <tr><th>Color key:</th> <td>{@link ElementKind#NUMBER}</td></tr>
     * </table></blockquote>
     *
     * @param number The floating point value to append to the WKT.
     */
    public void append(final double number) {
        appendSeparator(false);
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
        appendSeparator(false);
        setColor(ElementKind.NUMBER);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends a unit in a {@code UNIT[…]} element or one of the specialized elements. Specialized elements are
     * {@code ANGLEUNIT}, {@code LENGTHUNIT}, {@code SCALEUNIT}, {@code PARAMETRICUNIT} and {@code TIMEUNIT}.
     * Specialization is used in WKT 2 format except the <cite>simplified WKT 2</cite> one.
     *
     * {@example <code>append(SI.KILOMETRE)</code> will append "<code>LENGTHUNIT["km", 1000]</code>" to the WKT.}
     *
     * <blockquote><table class="compact">
     *   <tr><th>Position:</th>  <td>depending the previous element</td></tr>
     *   <tr><th>Color key:</th> <td>none</td></tr>
     * </table></blockquote>
     *
     * @param unit The unit to append to the WKT, or {@code null} if none.
     */
    public void append(final Unit<?> unit) {
        if (unit != null) {
            final StringBuffer buffer = this.buffer;
            appendSeparator(requestNewLine);
            String keyword = "UNIT";
            if (!convention.isSimple()) {
                if (Units.isLinear(unit)) {
                    keyword = "LENGTHUNIT";
                } else if (Units.isAngular(unit)) {
                    keyword = "ANGLEUNIT";
                } else if (Units.isScale(unit)) {
                    keyword = "SCALEUNIT";
                } else if (Units.isTemporal(unit)) {
                    keyword = "TIMEUNIT";
                }
            }
            buffer.append(keyword).appendCodePoint(symbols.getOpeningBracket(0));
            setColor(ElementKind.UNIT);
            final int fromIndex = buffer.appendCodePoint(symbols.getOpeningQuote(0)).length();
            if (NonSI.DEGREE_ANGLE.equals(unit)) {
                buffer.append("degree");
            } else if (SI.METRE.equals(unit)) {
                buffer.append(convention.usesCommonUnits() ? "meter" : "metre");
            } else {
                unitFormat.format(unit, buffer, dummy);
            }
            closeQuote(fromIndex);
            resetColor();
            append(Units.toStandardUnit(unit));
            closeElement();
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
            appendSeparator(false);
            buffer.append("null");
        } else if (value.getClass().isArray()) {
            appendSeparator(false);
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
        else if (value instanceof CodeList<?>)           append((CodeList<?>)           value);
        else if (value instanceof Date)                  append((Date)                  value);
        else if (value instanceof Boolean)               append((Boolean)               value);
        else if (value instanceof Unit<?>)               append((Unit<?>)               value);
        else if (value instanceof FormattableObject)     append((FormattableObject)     value);
        else if (value instanceof IdentifiedObject)      append((IdentifiedObject)      value);
        else if (value instanceof GeographicBoundingBox) append((GeographicBoundingBox) value, BBOX_ACCURACY);
        else if (value instanceof MathTransform)         append((MathTransform)         value);
        else if (value instanceof Matrix)                append((Matrix)                value);
        else append((value instanceof InternationalString) ?
                ((InternationalString) value).toString(locale) : value.toString(), null);
    }

    /**
     * Returns the linear unit for expressing lengths, or {@code null} for the default unit of each WKT element.
     * If {@code null}, then the default value depends on the object to format.
     *
     * <p>This method may return a non-null value if the next WKT elements to format are enclosed in a larger WKT
     * element, and the child elements shall inherit the linear unit of the enclosing element. The most typical
     * cases are the {@code PARAMETER[…]} elements enclosed in a {@code PROJCS[…]} element.</p>
     *
     * <p>The value returned by this method can be ignored if the WKT element to format contains an explicit
     * {@code UNIT[…]} element.</p>
     *
     * @return The unit for linear measurements, or {@code null} for the default unit.
     */
    public Unit<Length> getLinearUnit() {
        return linearUnit;
    }

    /**
     * Sets the unit to use for the next lengths to format. If non-null, the given unit will apply to all WKT elements
     * that do not define their own {@code UNIT[…]}, until this {@code setLinearUnit(…)} method is invoked again.
     *
     * @param unit The new unit, or {@code null} for letting element uses their own default.
     */
    public void setLinearUnit(final Unit<Length> unit) {
        linearUnit = unit;
    }

    /**
     * Returns the angular unit for expressing angles, or {@code null} for the default unit of each WKT element.
     * If {@code null}, then the default value depends on the object to format.
     *
     * <p>This method may return a non-null value if the next WKT elements to format are enclosed in a larger WKT
     * element, and the child elements shall inherit the linear unit of the enclosing element. A typical case is
     * the {@code PRIMEM[…]} element enclosed in a {@code GEOGCS[…]} element.</p>
     *
     * <p>The value returned by this method can be ignored if the WKT element to format contains an explicit
     * {@code UNIT[…]} element.</p>
     *
     * @return The unit for angular measurement, or {@code null} for the default unit.
     */
    public Unit<Angle> getAngularUnit() {
        return angularUnit;
    }

    /**
     * Sets the unit to use for the next angles to format. If non-null, the given unit will apply to all WKT elements
     * that do not define their own {@code UNIT[…]}, until this {@code setAngularUnit(…)} method is invoked again.
     *
     * {@section Special case}
     * If the WKT conventions are {@code WKT1_COMMON_UNITS}, then this method ignores the given unit.
     * See {@link Convention#WKT1_COMMON_UNITS} javadoc for more information.
     *
     * @param unit The new unit, or {@code null} for letting element uses their own default.
     */
    public void setAngularUnit(final Unit<Angle> unit) {
        if (!convention.usesCommonUnits()) {
            angularUnit = unit;
        }
    }

    /**
     * Returns {@code true} if the WKT written by this formatter is not strictly compliant to the WKT specification.
     * This method returns {@code true} if {@link #setInvalidWKT(IdentifiedObject)} has been invoked at least once.
     * The action to take regarding invalid WKT is caller-dependent.
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
     * @param unformattable The object that can not be formatted,
     */
    public void setInvalidWKT(final IdentifiedObject unformattable) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        String name;
        final ReferenceIdentifier id = unformattable.getName();
        if (id == null || (name = id.getCode()) == null) {
            name = getName(unformattable.getClass());
        }
        invalidElement = name;
        highlightError = true;
    }

    /**
     * Marks the current WKT representation of the given class as not strictly compliant to the WKT specification.
     * This method can be used as an alternative to {@link #setInvalidWKT(IdentifiedObject)} when the problematic
     * object is not an instance of {@code IdentifiedObject}.
     *
     * @param unformattable The class of the object that can not be formatted,
     */
    public void setInvalidWKT(final Class<?> unformattable) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        invalidElement = getName(unformattable);
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
     * If non-null, a cause may be available in the {@link #errorCause} field.
     */
    final String getErrorMessage() {
        return isInvalidWKT() ? Errors.format(Errors.Keys.CanNotRepresentInFormat_2, "WKT", invalidElement) : null;
    }

    /**
     * Returns the WKT formatted by this object.
     *
     * @return The WKT formatted by this instance.
     */
    @Override
    public String toString() {
        return buffer.toString();
    }

    /**
     * Clears this formatter before formatting a new object. This method clears the
     * {@linkplain #getLinearUnit() linear unit} and {@linkplain #isInvalidWKT() WKT validity flag}.
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
        linearUnit     = null;
        angularUnit    = null;
        invalidElement = null;
        errorCause     = null;
        highlightError = false;
        requestNewLine = false;
        margin         = 0;
        colorApplied   = 0;
    }
}
