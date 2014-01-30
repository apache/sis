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

import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Parser and formatter for <cite>Well Known Text</cite> (WKT) objects.
 * This format handles a pair of {@link Parser} and {@link Formatter},
 * to be used by {@code parse} and {@code format} methods respectively.
 * {@code WKTFormat} objects allow the following configuration:
 *
 * <ul>
 *   <li>The {@linkplain Symbols symbols} to use (curly braces or brackets, <i>etc.</i>)</li>
 *   <li>The preferred authority of {@linkplain IdentifiedObject#getName() object name} to
 *       format (see {@link Formatter#getName(IdentifiedObject)} for more information)</li>
 *   <li>Whatever ANSI X3.64 colors are allowed or not (default is not)</li>
 *   <li>The indentation</li>
 * </ul>
 *
 * {@section String expansion}
 * Because the strings to be parsed by this class are long and tend to contain repetitive substrings,
 * {@code WKTFormat} provides a mechanism for performing string substitutions before the parsing take place.
 * Long strings can be assigned short names by calls to the
 * <code>{@linkplain #definitions()}.put(<var>key</var>,<var>value</var>)</code> method.
 * After definitions have been added, any call to a parsing method will replace all occurrences
 * of a short name by the associated long string.
 *
 * <p>The short names must comply with the rules of Java identifiers. It is recommended, but not
 * required, to prefix the names by some symbol like {@code "$"} in order to avoid ambiguity.
 * Note however that this class doesn't replace occurrences between quoted text, so string
 * expansion still relatively safe even when used with non-prefixed identifiers.</p>
 *
 * <blockquote><font size="-1"><b>Example:</b>
 * In the example below, the {@code $WGS84} substring which appear in the argument given to the
 * {@code parseObject(…)} method will be expanded into the full {@code GEOGCS["WGS84", …]} string
 * before the parsing proceed.
 *
 * <blockquote><code>{@linkplain #definitions()}.put("$WGS84", "GEOGCS[\"WGS84\", DATUM[</code> <i>…etc…</i> <code>]]);<br>
 * Object crs = {@linkplain #parseObject(String) parseObject}("PROJCS[\"Mercator_1SP\", <strong>$WGS84</strong>,
 * PROJECTION[</code> <i>…etc…</i> <code>]]");</code></blockquote>
 * </font></blockquote>
 *
 * {@section Thread safety}
 * {@code WKTFormat}s are not synchronized. It is recommended to create separated format instances for each thread.
 * If multiple threads access a {@code WKTFormat} concurrently, it must be synchronized externally.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Eve (IRD)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
public class WKTFormat extends Format {
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
    private Symbols symbols = Symbols.DEFAULT;

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
    private Convention convention = Convention.DEFAULT;

    /**
     * The preferred authority for objects or parameter names. A {@code null} value
     * means that the authority shall be inferred from the {@linkplain #convention}.
     */
    private Citation authority;

    /**
     * The amount of spaces to use in indentation, or {@value #SINGLE_LINE} if indentation is disabled.
     * The same value is also stored in the {@linkplain #formatter}.
     * It appears here for serialization purpose.
     */
    private byte indentation = DEFAULT_INDENTATION;

    /**
     * A formatter using the same symbols than the {@linkplain #parser}.
     * Will be created by the {@link #format} method when first needed.
     */
    private transient Formatter formatter;

    /**
     * Constructs a format using the default factories.
     */
    public WKTFormat() {
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
            this.symbols = symbols;
            formatter = null;
        }
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
     * {@link Colors#CONSOLE} is given to this method, then the {@link #format(Object) format(…)}
     * method tries to highlight most of the elements that are relevant to
     * {@link org.apache.sis.util.Utilities#equalsIgnoreMetadata(Object, Object)}.</p>
     *
     * @param colors The colors for syntax coloring, or {@code null} if none.
     */
    public void setColors(final Colors colors) {
        this.colors = colors;
        if (formatter != null) {
            formatter.colors = colors;
        }
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
        this.convention = convention;
        updateFormatter(formatter);
    }

    /**
     * Returns the preferred authority for choosing the projection and parameter names.
     * If no authority has been {@link #setNameAuthority(Citation) explicitly set}, then this
     * method returns the authority associated to the {@linkplain #getConvention() convention}.
     *
     * @return The authority for projection and parameter names.
     *
     * @see Convention#getNameAuthority()
     * @see Formatter#getName(IdentifiedObject)
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
     * @see Formatter#getName(IdentifiedObject)
     */
    public void setNameAuthority(final Citation authority) {
        this.authority = authority;
        updateFormatter(formatter);
        // No need to update the parser.
    }

    /**
     * Updates the formatter convention and authority according the current state of this
     * {@code WKTFormat}. The authority may be null, in which case it will be inferred from
     * the convention when first needed.
     */
    private void updateFormatter(final Formatter formatter) {
        if (formatter != null) {
            formatter.setConvention(convention, authority);
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
        ArgumentChecks.ensureBetween("indentation", WKTFormat.SINGLE_LINE, Byte.MAX_VALUE, indentation);
        this.indentation = (byte) indentation;
        if (formatter != null) {
            formatter.indentation = this.indentation;
        }
    }

    /**
     * Not yet supported.
     *
     * @param  text The text to parse.
     * @param  position The index of the first character to parse.
     * @return The parsed object, or {@code null} in case of failure.
     */
    @Override
    public Object parseObject(final String text, final ParsePosition position) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the formatter, creating it if needed.
     */
    private Formatter getFormatter() {
        Formatter formatter = this.formatter;
        if (formatter == null) {
            formatter = new Formatter(Convention.DEFAULT, symbols, colors, indentation);
            updateFormatter(formatter);
            this.formatter = formatter;
        }
        return formatter;
    }

    /**
     * Formats the specified object as a Well Know Text. The given object shall be an instance of one of
     * {@link FormattableObject}, {@link IdentifiedObject}, {@link MathTransform}, {@link GeneralParameterValue}
     * or {@link Matrix}.
     *
     * @param  object     The object to format.
     * @param  toAppendTo Where the text is to be appended.
     * @param  pos        An identification of a field in the formatted text.
     * @return The given {@code toAppendTo} buffer.
     *
     * @see #getWarning()
     */
    @Override
    public StringBuffer format(final Object        object,
                               final StringBuffer  toAppendTo,
                               final FieldPosition pos)
    {
        final Formatter formatter = getFormatter();
        try {
            formatter.clear();
            formatter.buffer = toAppendTo;
            formatter.bufferBase = toAppendTo.length();
            if (object instanceof FormattableObject) {
                formatter.append((FormattableObject) object);
            } else if (object instanceof IdentifiedObject) {
                formatter.append((IdentifiedObject) object);
            } else if (object instanceof MathTransform) {
                formatter.append((MathTransform) object);
            } else if (object instanceof GeneralParameterValue) {
                /*
                 * Special processing for parameter values, which is formatted directly in 'Formatter'.
                 * Note that this interface doesn't share the same parent interface than other interfaces.
                 */
                formatter.append((GeneralParameterValue) object);
            } else if (object instanceof Matrix) {
                formatter.append((Matrix) object);
            } else {
                throw new ClassCastException(Errors.format(
                        Errors.Keys.IllegalArgumentClass_2, "object", object.getClass()));
            }
            return toAppendTo;
        } finally {
            formatter.buffer = null;
        }
    }

    /**
     * If a warning occurred during the last WKT {@linkplain #format formatting}, returns the warning.
     * Otherwise returns {@code null}. The warning is cleared every time a new object is formatted.
     *
     * @return The last warning, or {@code null} if none.
     */
    public String getWarning() {
        return (formatter != null) ? formatter.getErrorMessage() : null;
    }
}
