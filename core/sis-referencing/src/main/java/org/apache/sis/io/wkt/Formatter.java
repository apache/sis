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

import java.util.Collection;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.lang.reflect.Array;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.CodeList;

import org.apache.sis.measure.Units;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.referencing.IdentifiedObjects;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Formats {@linkplain FormattableObject formattable objects} as <cite>Well Known Text</cite> (WKT).
 * Each {@code Formatter} instance if created for a given {@linkplain Symbols set of symbols}.
 * For example in order to format an object with {@linkplain Symbols#CURLY_BRACKETS curly brackets}
 * instead of square ones and the whole text on the same line (no indentation), use the following:
 *
 * {@preformat java
 *     Formatter formatter = new Formatter(Symbols.CURLY_BRACKETS, null, WKTFormat.SINGLE_LINE);
 *     formatter.append(theObject);
 *     String wkt = formatter.toString();
 *
 *     // Following is needed only if you want to reuse the formatter again for other objects.
 *     formatter.clear();
 * }
 *
 * Formatters are not synchronized. It is recommended to create separated formatter instances for each thread.
 * If multiple threads access a formatter concurrently, then the formatter must be synchronized externally.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public class Formatter {
    /**
     * Do not format an {@code "AUTHORITY"} element for instance of this class.
     */
    private static final Class<? extends IdentifiedObject> AUTHORITY_EXCLUDE = CoordinateSystemAxis.class;

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
     * @see WKTFormat#getColors()
     * @see WKTFormat#setColors(Colors)
     */
    Colors colors;

    /**
     * The preferred convention for objects or parameter names.
     * This field should never be {@code null}.
     *
     * @see WKTFormat#getConvention()
     * @see WKTFormat#setConvention(Convention)
     */
    private Convention convention;

    /**
     * The preferred authority for objects or parameter names.
     *
     * @see WKTFormat#getAuthority(Citation)
     * @see WKTFormat#setAuthority(Citation)
     */
    private Citation authority;

    /**
     * The unit for writing length, or {@code null} for the "natural" unit of each WKT element.
     */
    private Unit<Length> linearUnit;

    /**
     * The unit for writing angles, or {@code null} for the "natural" unit of each WKT element.
     * This value is set for example by {@code "GEOGCS"}, which force its enclosing {@code "PRIMEM"}
     * to take the same units than itself.
     */
    private Unit<Angle> angularUnit;

    /**
     * The object to use for formatting numbers.
     */
    private final NumberFormat numberFormat;

    /**
     * The object to use for formatting unit symbols.
     */
    private final UnitFormat unitFormat;

    /**
     * Dummy field position.
     */
    private final FieldPosition dummy = new FieldPosition(0);

    /**
     * The buffer in which to format. Consider this field as private and final. The only method to change
     * the value of this field is {@link WKTFormat#format(Object, StringBuffer, FieldPosition)}.
     */
    StringBuffer buffer;

    /**
     * The starting point in the buffer. Always 0, except when used by
     * {@link WKTFormat#format(Object, StringBuffer, FieldPosition)}.
     */
    int bufferBase;

    /**
     * The amount of spaces to use in indentation, or {@value org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE}
     * if indentation is disabled.
     */
    int indentation;

    /**
     * The amount of space to write on the left side of each line. This amount is increased
     * by {@code indentation} every time a {@link FormattableObject} is appended in a new
     * indentation level.
     */
    private int margin;

    /**
     * {@code true} if a new line were requested during the execution of {@link #append(Formattable)}.
     * This is used to determine if the next {@code UNIT} and {@code AUTHORITY} elements shall appear
     * on a new line.
     */
    private boolean requestNewLine;

    /**
     * {@code true} if the last formatted element was invalid WKT. This field is for internal use only.
     * It is reset to {@code false} after the invalid part has been processed by {@link #append(Formattable)}.
     */
    private boolean wasInvalidWKT;

    /**
     * Non-null if the WKT is invalid. If non-null, then this field contains a keyword that identify the
     * problematic part.
     *
     * @see #isInvalidWKT()
     */
    private String unformattable;

    /**
     * Warning that may be produced during WKT formatting, or {@code null} if none.
     *
     * @see #isInvalidWKT()
     */
    private Exception warning;

    /**
     * Creates a new formatter instance with the default symbols, no syntax coloring and the default indentation.
     */
    public Formatter() {
        this(Convention.OGC, Symbols.DEFAULT, null, WKTFormat.DEFAULT_INDENTATION);
    }

    /**
     * Creates a new formatter instance with the specified convention, colors and indentation.
     *
     * @param convention  The convention to use.
     * @param symbols     The symbols.
     * @param colors      The syntax coloring, or {@code null} if none.
     * @param indentation The amount of spaces to use in indentation for WKT formatting,
     *        or {@link WKTFormat#SINGLE_LINE} for formatting the whole WKT on a single line.
     */
    public Formatter(final Convention convention, final Symbols symbols, final Colors colors, final int indentation) {
        ArgumentChecks.ensureNonNull("convention", convention);
        ArgumentChecks.ensureNonNull("symbols", symbols);
        if (indentation < WKTFormat.SINGLE_LINE) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "indentation", indentation));
        }
        this.convention   = convention;
        this.symbols      = symbols.immutable();
        this.indentation  = indentation;
        this.numberFormat = symbols.createNumberFormat();
        this.unitFormat   = UnitFormat.getInstance(symbols.getLocale());
        this.buffer       = new StringBuffer();
        if (colors != null) {
            this.colors = colors.immutable();
        }
    }

    /**
     * Constructor for private use by {@link WKTFormat#format} only.
     * This constructor helps to share some objects with {@link Parser}.
     */
    Formatter(final Symbols symbols, final NumberFormat numberFormat) {
        this.convention   = Convention.OGC;
        this.symbols      = symbols;
        this.indentation  = WKTFormat.DEFAULT_INDENTATION;
        this.numberFormat = numberFormat; // No clone needed.
        this.unitFormat   = UnitFormat.getInstance(symbols.getLocale());
        // Do not set the buffer. It will be set by WKTFormat.format(...).
    }

    /**
     * Returns the convention to use for formatting the WKT. The default convention is {@link Convention#OGC OGC}.
     * A different convention will usually result in different parameter names, but may also change the WKT syntax.
     *
     * @return The convention (never {@code null}).
     *
     * @see WKTFormat#setConvention(Convention)
     * @see FormattableObject#toWKT(Convention)
     */
    public Convention getConvention() {
        return convention;
    }

    /**
     * Sets the convention and the authority to use for formatting WKT elements.
     *
     * @param convention The convention, or {@code null} for the default value.
     * @param authority  The authority, or {@code null} for inferring it from the convention.
     */
    final void setConvention(Convention convention, final Citation authority) {
        if (convention == null) {
            convention = Convention.forCitation(authority, Convention.OGC);
        }
        this.convention = convention;
        this.authority  = (authority != null) ? authority : convention.authority; // NOT convention.getAuthority()
    }

    /**
     * Returns the preferred name for the specified object.
     * If the specified object contains a name from the preferred authority
     * (usually {@linkplain org.apache.sis.metadata.iso.citation.Citations#OGC Open Geospatial}),
     * then this name is returned. Otherwise, the first name found is returned.
     *
     * <p>The preferred authority can be set by the {@link WKTFormat#setAuthority(Citation)} method.
     * This is not necessarily the authority of the given {@linkplain IdentifiedObject#getName() object name}.</p>
     *
     * {@example The EPSG name of the <code>EPSG:6326</code> datum is "<cite>World Geodetic System 1984</cite>".
     *           However if the preferred authority is OGC (which is the case by default), then this method usually
     *           returns "<cite>WGS84</cite>" (the exact string to be returned depends on the object aliases).}
     *
     * @param  object The object to look for a preferred name.
     * @return The preferred name, or {@code null} if the given object has no name.
     *
     * @see WKTFormat#getAuthority()
     * @see IdentifiedObjects#getName(IdentifiedObject, Citation)
     */
    public String getName(final IdentifiedObject object) {
        String name = IdentifiedObjects.getName(object, authority);
        if (name == null) {
            name = IdentifiedObjects.getName(object, null);
        }
        return name;
    }

    /**
     * Returns the preferred identifier for the specified object.
     * If the specified object contains an identifier from the preferred authority
     * (usually {@linkplain org.apache.sis.metadata.iso.citation.Citations#OGC Open Geospatial}),
     * then this identifier is returned. Otherwise, the first identifier is returned.
     * If the specified object contains no identifier, then this method returns {@code null}.
     *
     * @param  info The object to look for a preferred identifier, or {@code null} if none.
     * @return The preferred identifier, or {@code null} if none.
     */
    public Identifier getIdentifier(final IdentifiedObject info) {
        Identifier first = null;
        if (info != null) {
            final Collection<? extends Identifier> identifiers = info.getIdentifiers();
            if (identifiers != null) {
                for (final Identifier id : identifiers) {
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
            final String color = colors.getAnsiSequence(type);
            if (color != null) {
                buffer.append(color);
            }
        }
    }

    /**
     * Appends in the {@linkplain #buffer} the ANSI escape sequence for reseting the color to the default.
     * This method does nothing unless syntax coloring has been explicitly enabled.
     */
    private void resetColor() {
        if (colors != null) {
            buffer.append(FOREGROUND_DEFAULT);
        }
    }

    /**
     * Increase or reduce the indentation. A value of {@code +1} increase
     * the indentation by the amount of spaces specified at construction time,
     * and a value of {@code +1} reduce it.
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
        } while (Character.isWhitespace(c));
        buffer.append(symbols.getSeparator());
        if (newLine && indentation > WKTFormat.SINGLE_LINE) {
            buffer.append(JDK7.lineSeparator()).append(CharSequences.spaces(margin));
        }
    }

    /**
     * Appends the given {@code Formattable} object.
     * This method will automatically append the keyword (e.g. {@code "GEOCS"}), the name and the authority code,
     * and will invoke <code>formattable.{@linkplain FormattableObject#formatTo(Formatter) formatTo}(this)</code>
     * for completing the inner part of the WKT.
     *
     * @param object The formattable object to append to the WKT, or {@code null} if none.
     */
    public void append(final Formattable object) {
        if (object == null) {
            return;
        }
        final StringBuffer buffer = this.buffer;
        final int open  = symbols.getOpeningBracket(0);
        final int close = symbols.getClosingBracket(0);
        /*
         * Formats the opening bracket and the object name (e.g. "NAD27").
         * The WKT entity name (e.g. "PROJCS") will be formatted later.
         * The result of this code portion looks like the following:
         *
         *         <previous text>,
         *           ["NAD27 / Idaho Central"
         */
        appendSeparator(true);
        int base = buffer.length();
        buffer.appendCodePoint(open);
        final IdentifiedObject info = (object instanceof IdentifiedObject) ? (IdentifiedObject) object : null;
        if (info != null) {
            final ElementKind type = ElementKind.forType(info.getClass());
            if (type != null) {
                setColor(type);
            }
            quote(getName(info));
            if (type != null) {
                resetColor();
            }
        }
        /*
         * Formats the part after the object name, then insert the WKT element name in front of them.
         * The result of this code portion looks like the following:
         *
         *         <previous text>,
         *           PROJCS["NAD27 / Idaho Central",
         *             GEOGCS[...etc...],
         *             ...etc...
         */
        indent(+1);
        requestNewLine = false;
        String keyword = object.formatTo(this);
        if (colors != null && wasInvalidWKT) {
            wasInvalidWKT = false;
            final String color = colors.getAnsiSequence(ElementKind.ERROR);
            if (color != null) {
                buffer.insert(base, color + BACKGROUND_DEFAULT);
                base += color.length();
            }
        }
        buffer.insert(base, keyword);
        /*
         * Formats the AUTHORITY[<name>,<code>] entity, if there is one. The entity
         * will be on the same line than the enclosing one if no line separator were
         * added (e.g. SPHEROID["Clarke 1866", ..., AUTHORITY["EPSG","7008"]]), or on
         * a new line otherwise. After this block, the result looks like the following:
         *
         *         <previous text>,
         *           PROJCS["NAD27 / Idaho Central",
         *             GEOGCS[...etc...],
         *             ...etc...
         *             AUTHORITY["EPSG","26769"]]
         */
        final Identifier identifier = getIdentifier(info);
        if (identifier != null && !AUTHORITY_EXCLUDE.isInstance(info)) {
            final Citation authority = identifier.getAuthority();
            if (authority != null) {
                final String title = Citations.getIdentifier(authority);
                if (title != null) {
                    appendSeparator(requestNewLine);
                    buffer.append("AUTHORITY").appendCodePoint(open);
                    quote(title);
                    final String code = identifier.getCode();
                    if (code != null) {
                        buffer.append(symbols.getSeparator());
                        quote(code);
                    }
                    buffer.appendCodePoint(close);
                }
            }
        }
        buffer.appendCodePoint(close);
        requestNewLine = true;
        indent(-1);
    }

    /**
     * Appends the given {@code IdentifiedObject} object.
     *
     * @param object The identified object to append to the WKT, or {@code null} if none.
     */
    public void append(final IdentifiedObject object) {
        if (object != null) {
            if (object instanceof Formattable) {
                append((Formattable) object);
            } else {
                throw unsupported(object);
            }
        }
    }

    /**
     * Appends the given math transform.
     *
     * @param transform The transform object to append to the WKT, or {@code null} if none.
     */
    public void append(final MathTransform transform) {
        if (transform != null) {
            if (transform instanceof Formattable) {
                append((Formattable) transform);
            } else {
                throw unsupported(transform);
            }
        }
    }

    /**
     * Invoked when an object is not a supported implementation.
     *
     * @param object The object of unknown type.
     * @return The exception to be thrown.
     */
    private static UnformattableObjectException unsupported(final Object object) {
        return new UnformattableObjectException(Errors.format(
                Errors.Keys.IllegalClass_2, Formattable.class, object.getClass()));
    }

    /**
     * Appends a {@linkplain ParameterValue parameter} in WKT form.
     * If the supplied parameter is actually a {@linkplain ParameterValueGroup parameter group},
     * all contained parameters will flattened in a single list.
     *
     * @param parameter The parameter to append to the WKT, or {@code null} if none.
     */
    public void append(final GeneralParameterValue parameter) {
        if (parameter instanceof ParameterValueGroup) {
            for (final GeneralParameterValue param : ((ParameterValueGroup)parameter).values()) {
                append(param);
            }
        }
        if (parameter instanceof ParameterValue<?>) {
            final ParameterValue<?> param = (ParameterValue<?>) parameter;
            final ParameterDescriptor<?> descriptor = param.getDescriptor();
            Unit<?> unit = descriptor.getUnit();
            if (unit != null && !Unit.ONE.equals(unit)) {
                Unit<?> contextUnit = linearUnit;
                if (contextUnit!=null && unit.isCompatible(contextUnit)) {
                    unit = contextUnit;
                } else {
                    contextUnit = convention.forcedAngularUnit;
                    if (contextUnit == null) {
                        contextUnit = angularUnit;
                    }
                    if (contextUnit!=null && unit.isCompatible(contextUnit)) {
                        unit = contextUnit;
                    }
                }
            }
            appendSeparator(true);
            final StringBuffer buffer = this.buffer;
            final int start = buffer.length();
            final int stop = buffer.append("PARAMETER").length();
            buffer.appendCodePoint(symbols.getOpeningBracket(0));
            setColor(ElementKind.PARAMETER);
            quote(getName(descriptor));
            resetColor();
            buffer.append(symbols.getSeparator());
            if (unit != null) {
                double value;
                try {
                    value = param.doubleValue(unit);
                } catch (IllegalStateException exception) {
                    // May happen if a parameter is mandatory (e.g. "semi-major")
                    // but no value has been set for this parameter.
                    if (colors != null) {
                        final String c = colors.getAnsiSequence(ElementKind.ERROR);
                        if (c != null) {
                            buffer.insert(stop, BACKGROUND_DEFAULT).insert(start, c);
                        }
                    }
                    warning = exception;
                    value = Double.NaN;
                }
                format(value);
            } else {
                appendObject(param.getValue());
            }
            buffer.appendCodePoint(symbols.getClosingBracket(0));
            requestNewLine = true;
        }
    }

    /**
     * Appends the specified value to a string buffer. If the value is an array, then the
     * array elements are appended recursively (i.e. the array may contains sub-array).
     */
    private void appendObject(final Object value) {
        final StringBuffer buffer = this.buffer;
        if (value == null) {
            buffer.append("null");
        } else if (value.getClass().isArray()) {
            buffer.appendCodePoint(symbols.getOpenSequence());
            final int length = Array.getLength(value);
            for (int i=0; i<length; i++) {
                if (i != 0) {
                    buffer.append(symbols.getSeparator());
                }
                appendObject(Array.get(value, i));
            }
            buffer.appendCodePoint(symbols.getCloseSequence());
        } else if (value instanceof CodeList<?>) {
            append((CodeList<?>) value);
        } else if (value instanceof Number) {
            final Number number = (Number) value;
            if (Numbers.isInteger(number.getClass())) {
                format(number.longValue());
            } else {
                format(number.doubleValue());
            }
        } else if (value instanceof Boolean) {
            buffer.append(((Boolean) value).booleanValue() ? "TRUE" : "FALSE");
        } else {
            quote(value.toString());
        }
    }

    /**
     * Appends a code list.
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
     * @param text The string to format to the WKT, or {@code null} if none.
     */
    public void append(final String text) {
        if (text != null) {
            appendSeparator(false);
            quote(text);
        }
    }

    /**
     * Appends the given string as a quoted text.
     */
    private void quote(final String text) {
        buffer.appendCodePoint(symbols.getOpenQuote()).append(text).appendCodePoint(symbols.getCloseQuote());
    }

    /**
     * Appends an integer value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param number The integer to append to the WKT.
     */
    public void append(final long number) {
        appendSeparator(false);
        format(number);
    }

    /**
     * Appends an floating point value.
     * The {@linkplain Symbols#getSeparator() element separator} will be written before the number if needed.
     *
     * @param number The floating point value to append to the WKT.
     */
    public void append(final double number) {
        appendSeparator(false);
        format(number);
    }

    /**
     * Formats an integer number.
     */
    private void format(final long number) {
        setColor(ElementKind.INTEGER);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Formats a floating point number.
     */
    private void format(double number) {
        setColor(ElementKind.NUMBER);
        /*
         * The -2 above is for using two less fraction digits than the expected number accuracy.
         * The intend is to give to DecimalFormat a chance to hide rounding errors, keeping in
         * mind that the number value is not necessarily the original one (we may have applied
         * a unit conversion). In the case of WGS84 semi-major axis in metres, we still have a
         * maximum of 8 fraction digits, which is more than enough.
         */
        numberFormat.setMaximumFractionDigits(MathFunctions.fractionDigitsForDelta(Math.ulp(number), false) - 2);
        numberFormat.setMinimumFractionDigits(1); // Must be after setMaximumFractionDigits(…).
        numberFormat.format(number, buffer, dummy);
        resetColor();
    }

    /**
     * Appends a unit in WKT form.
     * For example {@code append(SI.KILO(SI.METRE))} will append "{@code UNIT["km", 1000]}" to the WKT.
     *
     * @param unit The unit to append to the WKT, or {@code null} if none.
     */
    public void append(final Unit<?> unit) {
        if (unit != null) {
            final StringBuffer buffer = this.buffer;
            appendSeparator(requestNewLine);
            buffer.append("UNIT").appendCodePoint(symbols.getOpeningBracket(0));
            setColor(ElementKind.UNIT);
            buffer.appendCodePoint(symbols.getOpenQuote());
            if (NonSI.DEGREE_ANGLE.equals(unit)) {
                buffer.append("degree");
            } else if (SI.METRE.equals(unit)) {
                buffer.append(convention.unitUS ? "meter" : "metre");
            } else {
                unitFormat.format(unit, buffer, dummy);
            }
            buffer.appendCodePoint(symbols.getCloseQuote());
            resetColor();
            append(Units.toStandardUnit(unit));
            buffer.appendCodePoint(symbols.getClosingBracket(0));
        }
    }

    /**
     * Returns the linear unit for expressing lengths, or {@code null} for the "natural" unit of each WKT element.
     *
     * @return The unit for linear measurements. Default value is {@code null}.
     */
    public Unit<Length> getLinearUnit() {
        return linearUnit;
    }

    /**
     * Sets the unit to use for expressing lengths.
     *
     * @param unit The new unit, or {@code null}.
     */
    public void setLinearUnit(final Unit<Length> unit) {
        linearUnit = unit;
    }

    /**
     * Returns the angular unit for expressing angles, or {@code null} for the "natural" unit of each WKT element.
     * This value is set for example when formatting the {@code GEOGCS} element, in which case the enclosed
     * {@code PRIMEM} element shall use the unit of the enclosing {@code GEOGCS}.
     *
     * @return The unit for angle measurement. Default value is {@code null}.
     */
    public Unit<Angle> getAngularUnit() {
        return angularUnit;
    }

    /**
     * Sets the angular unit for formatting measures.
     *
     * @param unit The new unit, or {@code null}.
     */
    public void setAngularUnit(final Unit<Angle> unit) {
        angularUnit = unit;
    }

    /**
     * Returns {@code true} if the WKT written by this formatter is not strictly compliant to the
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">WKT
     * specification</a>. This method returns {@code true} if {@link #setInvalidWKT(String)} has
     * been invoked at least once. The action to take regarding invalid WKT is caller-dependent.
     * For example {@link FormattableObject#toString()} will accepts loose WKT formatting and ignore
     * this flag, while {@link FormattableObject#toWKT()} requires strict WKT formatting and will
     * thrown an exception if this flag is set.
     *
     * @return {@code true} if the WKT is invalid.
     */
    public boolean isInvalidWKT() {
        return unformattable != null || (buffer != null && buffer.length() == 0);
        /*
         * Note: we really use a "and" condition (not an other "or") for the buffer test because
         *       the buffer is reset to 'null' by WKTFormat after a successfull formatting.
         */
    }

    /**
     * Sets a flag marking the current WKT as not strictly compliant to the
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">WKT
     * specification</a>. This method is invoked by {@link FormattableObject#formatTo(Formatter)}
     * methods when the object to format is more complex than what the WKT specification allows.
     * Applications can test {@link #isInvalidWKT()} later for checking WKT validity.
     *
     * @param unformattable A keyword that identify the component that can not be formatted,
     */
    public void setInvalidWKT(final String unformattable) {
        ArgumentChecks.ensureNonNull("unformattable", unformattable);
        this.unformattable = unformattable;
        wasInvalidWKT = true;
    }

    /**
     * Throws an exception if {@link #isInvalidWKT()} is set.
     */
    final void ensureValidWKT() throws UnformattableObjectException {
        if (isInvalidWKT()) {
            throw new UnformattableObjectException(Errors.format(
                    Errors.Keys.CanNotRepresentInFormat_2, "WKT", unformattable), warning);
        }
    }

    /**
     * Returns the WKT formatted by this object.
     */
    @Override
    public String toString() {
        return buffer.toString();
    }

    /**
     * Clears this formatter. All properties (including {@linkplain #getLinearUnit() unit}
     * and {@linkplain #isInvalidWKT() WKT validity flag} are reset to their default value.
     * After this method call, this {@code Formatter} object is ready for formatting a new object.
     */
    public void clear() {
        if (buffer != null) {
            buffer.setLength(0);
        }
        linearUnit     = null;
        angularUnit    = null;
        unformattable  = null;
        warning        = null;
        wasInvalidWKT  = false;
        requestNewLine = false;
        margin         = 0;
    }
}
