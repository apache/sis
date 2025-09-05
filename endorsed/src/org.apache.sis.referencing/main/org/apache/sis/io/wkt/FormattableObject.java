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

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.xml.bind.annotation.XmlTransient;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Printable;
import org.apache.sis.util.privy.X364;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.system.Environment;


/**
 * Base class for objects that can be formatted as <i>Well Known Text</i> (WKT).
 * {@link WKTFormat} checks for this class at formatting time for each element to format.
 * When a {@code FormattableObject} element is found, its {@link #formatTo(Formatter)} method
 * is invoked for allowing the element to control its formatting.
 *
 * <p>This class provides two methods for getting a default <i>Well Known Text</i>
 * representation of this object:</p>
 *
 * <ul>
 *   <li>{@link #toWKT()} tries to return a strictly compliant WKT or throws {@link UnformattableObjectException}
 *       if this object contains elements not defined by the ISO 19162 standard.</li>
 *   <li>{@link #toString()} returns a WKT with some redundant information omitted and some constraints relaxed.
 *       This method never throw {@code UnformattableObjectException};
 *       it will rather use non-standard representation if necessary.</li>
 * </ul>
 *
 * <h2>Syntax coloring</h2>
 * A convenience {@link #print()} method is provided, which is roughly equivalent to
 * {@code System.out.println(this)} except that syntax coloring is automatically applied
 * if the terminal seems to support the ANSI escape codes.
 *
 * <h2>Non-standard WKT</h2>
 * If this object cannot be formatted without violating some WKT constraints,
 * then the behavior depends on the method invoked:
 *
 * <ul>
 *   <li>{@link #toWKT()} will throw a {@link UnformattableObjectException}.</li>
 *   <li>{@link #toString()} will ignore the problem and uses non-standard elements if needed.</li>
 *   <li>{@link #print()} will show the non-standard elements in red if syntax coloring is enabled.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.4
 */
@XmlTransient
public abstract class FormattableObject implements Printable {
    /**
     * The formatter for the {@link #toWKT()} and {@link #toString()} methods. Formatters are not
     * thread-safe, consequently we must make sure that only one thread uses a given instance.
     *
     * <h4>Implementation notes</h4>
     * We do not use synchronization because the formatter will call back user's code, which
     * introduce a risk of thread lock if the user performs his own synchronization.
     *
     * <p>We do not use {@link ThreadLocal} because {@code Formatter} is not reentrant neither,
     * so it may produce very confusing behavior when debugging a code that perform WKT formatting
     * (some debuggers seem to invoke {@code toString()} for their own purpose in the same thread).
     * Since {@code toString()} is typically invoked for debugging purpose, a single formatter for
     * any thread is presumed sufficient.</p>
     */
    private static final AtomicReference<Formatter> FORMATTER = new AtomicReference<>();

    /**
     * Default constructor.
     */
    protected FormattableObject() {
    }

    /**
     * Returns a strictly compliant <i>Well Known Text</i> (WKT) using the default convention,
     * symbols and indentation. If this object cannot be represented in a standard way, then this
     * method throws an {@link UnformattableObjectException}.
     *
     * <p>By default this method formats this object according the {@link Convention#WKT2} rules.</p>
     *
     * @return the default Well Know Text representation of this object.
     * @throws UnformattableObjectException if this object cannot be formatted as a standard WKT.
     *
     * @see org.opengis.referencing.IdentifiedObject#toWKT()
     */
    public String toWKT() throws UnformattableObjectException {
        return formatWKT(Convention.DEFAULT, false, true);
    }

    /**
     * Returns a <i>Well Known Text</i> (WKT) or an alternative text representation for this object.
     * If this object cannot be represented in a standard way, then this method may fallback on non-standard
     * representation, or leave unformattable elements empty and append warnings after the WKT.
     *
     * <p>By default this method formats this object according the {@link Convention#WKT2_SIMPLIFIED} rules,
     * except that Unicode characters are kept <i>as-is</i> (they are not converted to ASCII).
     * Consequently, the WKT is not guaranteed to be ISO 19162 compliant.
     * For stricter conformance, use {@link #toWKT()} instead.</p>
     *
     * @return the Well Known Text (WKT) or an alternative representation of this object.
     */
    @Override
    public String toString() {
        return formatWKT(Convention.WKT2_SIMPLIFIED, false, false);
    }

    /**
     * Returns a <i>Well Known Text</i> (WKT) for this object using the specified convention.
     * Unicode characters are kept <i>as-is</i> (they are not converted to ASCII).
     * The returned string may contain non-standard elements or warnings
     * if this object cannot be formatted according the given convention.
     *
     * <p>For stricter conformance to ISO 19162 standard, use {@link #toWKT()} or {@link WKTFormat} instead.</p>
     *
     * @param  convention  the WKT convention to use.
     * @return the Well Known Text (WKT) or a pseudo-WKT representation of this object.
     */
    public String toString(final Convention convention) {
        return formatWKT(Objects.requireNonNull(convention), false, false);
    }

    /**
     * Prints a string representation of this object to the {@linkplain System#out standard output stream}.
     * If a {@linkplain Console console} is attached to the running JVM (i.e. if the application is run
     * from the command-line and the output is not redirected to a file) and if Apache SIS thinks that
     * the console supports the ANSI escape codes (a.k.a. X3.64), then a syntax coloring will be applied.
     *
     * <p>This is a convenience method for debugging purpose and for console applications.</p>
     */
    @Debug
    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void print() {
        final Console console = System.console();
        final PrintWriter out = Environment.writer(console, System.out);
        out.println(formatWKT(Convention.WKT2_SIMPLIFIED, (console != null) && X364.isAnsiSupported(), false));
        out.flush();
    }

    /**
     * Returns a WKT for this object using the specified convention.
     * If {@code strict} is true, then an exception is thrown if the WKT is not standard-compliant.
     * If {@code strict} if false, then warnings are appended after the WKT instead.
     *
     * @param  convention  the convention for choosing WKT element names.
     * @param  colorize    {@code true} for applying syntax coloring, or {@code false} otherwise.
     * @param  strict      {@code true} if an exception shall be thrown for unformattable objects,
     *                     or {@code false} for providing a non-standard formatting instead.
     * @return the Well Known Text (WKT) or a pseudo-WKT representation of this object.
     * @throws UnformattableObjectException if {@code strict} is {@code true} and this object cannot be formatted.
     */
    private String formatWKT(final Convention convention, final boolean colorize, final boolean strict)
             throws UnformattableObjectException
    {
        Formatter formatter = FORMATTER.getAndSet(null);
        if (formatter == null) {
            formatter = new Formatter();
        }
        /*
         * Apply the same setting as in 'WKTFormat.updateFormatter(…)'
         * when KeywordCase and KeywordStyle have their default values.
         */
        formatter.configure(convention, null, colorize ? Colors.DEFAULT : null,
                convention.toUpperCase           ? (byte) +1 : 0,
                (convention.majorVersion() == 1) ? (byte) -1 : 0,
                Constants.DEFAULT_INDENTATION,
                strict ? Integer.MAX_VALUE : 10);       // Arbitrary limit of 10 elements in double[] parameter values.
        if (!strict) {
            formatter.transliterator = Transliterator.IDENTITY;
        }
        formatter.verifyCharacterValidity = strict;
        final String wkt;
        try {
            formatter.append(this);
            final Warnings warnings = formatter.getWarnings();
            if (warnings != null) {
                if (warnings.getRootElement() == null) {
                    warnings.setRoot(this);
                }
                if (strict) {
                    /*
                     * If a warning occurred, consider the object as non-formattable.
                     * We take the last message since it is more likely to be about the enclosing element.
                     */
                    final int n = warnings.getNumMessages() - 1;
                    throw new UnformattableObjectException(warnings.getMessage(n), warnings.getException(n));
                }
            }
            formatter.appendWarnings();
            wkt = formatter.toWKT();
        } catch (IOException e) {
            throw new UnformattableObjectException(e);      // Should never happen since we write to a StringBuffer.
        } finally {
            formatter.clear();
        }
        FORMATTER.set(formatter);
        return wkt;
    }

    /**
     * Formats the inner part of this <i>Well Known Text</i> (WKT) element into the given formatter.
     * This method is automatically invoked by {@link WKTFormat} when a formattable element is found.
     *
     * <p>Keywords, opening and closing brackets shall not be formatted here.
     * For example if this formattable element is for a {@code GeodeticCRS[…]} element,
     * then this method shall write the content starting at the insertion point shown below:</p>
     *
     * <pre class="text">
     *   GeodeticCRS[ ]
     *               ↑
     *       (insertion point)</pre>
     *
     * <h4>Formatting non-standard WKT</h4>
     * If the implementation cannot represent this object without violating some WKT constraints,
     * it can uses its own (non-standard) keywords but shall declare that it did so by invoking one
     * of the {@link Formatter#setInvalidWKT(IdentifiedObject, Exception) Formatter.setInvalidWKT(…)}
     * methods.
     *
     * <p>Alternatively, the implementation may also have no WKT keyword for this object.
     * This happen frequently when an abstract class defines a base implementation,
     * while the keyword needs to be defined by the concrete subclasses.
     * In such case, the method in the abstract class shall return {@code null}.</p>
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return the {@linkplain KeywordCase#CAMEL_CASE CamelCase} keyword for the WKT element, or {@code null} if unknown.
     *
     * @see #toWKT()
     * @see #toString()
     */
    protected abstract String formatTo(Formatter formatter);
}
