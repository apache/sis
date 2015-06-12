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

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Warnings that occurred during a <cite>Well Known Text</cite> (WKT) parsing or formatting.
 * Some example of information provided by this object are:
 *
 * <ul>
 *   <li>Recoverable exceptions.</li>
 *   <li>At formatting time, object that can not be formatted in a standard-compliant WKT.</li>
 *   <li>At parsing time, unknown keywords.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see WKTFormat#getWarnings()
 */
public final class Warnings implements Localized, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1825161781642905329L;

    /**
     * The locale in which warning messages are reported.
     * Not necessarily the same than the locale for number and date parsing or formating.
     *
     * @see #getLocale()
     */
    private final Locale errorLocale;

    /**
     * Warning messages or exceptions emitted during parsing or formatting.
     * Initially {@code null} and created when first needed.
     *
     * <p>Objects in this list must be a sequence of the following tupple:</p>
     *
     * <ul>
     *   <li>An optional message as an {@link InternationalString}.</li>
     *   <li>An optional warning cause as an {@link Exception}.</li>
     * </ul>
     *
     * Any element of the above tupple can be null, but at least one element must be non-null.
     *
     * @see #add(InternationalString, Exception, String[])
     */
    private List<Object> messages;

    /**
     * The keywords of elements in which exception occurred.
     * Initially {@code null} and created when first needed.
     *
     * <p>For each {@code String[]} value, the first array element shall be the keyword of the WKT element
     * in which the exception occurred. The second array element shall be the parent of above-cited first
     * element. Other array elements can optionally be present for declaring the parents of the parent,
     * but they will be ignored by this {@code Warnings} implementation.</p>
     */
    private Map<Exception, String[]> exceptionSources;

    /**
     * Keyword of unknown elements. This is initially a direct reference to the {@link Parser#ignoredElements} map,
     * which is okay only until a new parsing start. If this {@code Warnings} instance is given to the user, then
     * the {@link #publish()} method must be invoked in order to copy this map.
     *
     * @see Parser#ignoredElements
     */
    private Map<String, List<String>> ignoredElements;

    /**
     * {@code true} if {@link #publish()} has been invoked.
     */
    private boolean published;

    /**
     * Creates a new object for declaring warnings.
     *
     * @param locale The locale for reporting warning messages.
     * @param ignoredElements The {@link Parser#ignoredElements} map, or an empty map (can not be null).
     */
    Warnings(final Locale locale, final Map<String, List<String>> ignoredElements) {
        this.errorLocale     = locale;
        this.ignoredElements = ignoredElements;
    }

    /**
     * Must be invoked before this {@code Warnings} instance is given to the user,
     * in order to protect this instance from changes caused by the next parsing operation.
     */
    final void publish() {
        if (!published) {
            ignoredElements = ignoredElements.isEmpty() ? Collections.emptyMap() : new LinkedHashMap<>(ignoredElements);
            published = true;
        }
    }

    /**
     * Returns the locale in which warning messages are reported by the default {@link #toString()} method.
     * This is not necessarily the same locale than the one used for parsing and formatting dates and numbers
     * in the WKT.
     *
     * @return The locale or warning messages are reported.
     */
    @Override
    public Locale getLocale() {
        return errorLocale;
    }

    /**
     * Adds a warning. At least one of {@code message} or {@code cause} shall be non-null.
     *
     * @param message The message, or {@code null}.
     * @param cause   The exception that caused the warning, or {@code null}
     * @param source  The location of the exception, or {@code null}. If non-null, then {@code source[0]} shall be
     *                the keyword of the WKT element where the exception occurred, and {@code source[1]} the keyword
     *                of the parent of {@code source[0]}.
     */
    final void add(final InternationalString message, final Exception cause, final String[] source) {
        assert (message != null) || (cause != null);
        if (messages == null) {
            messages = new ArrayList<>(4);  // We expect few items.
        }
        messages.add(message);
        messages.add(cause);
        if (cause != null) {
            if (exceptionSources == null) {
                exceptionSources = new LinkedHashMap<>(4);  // We expect few items.
            }
            exceptionSources.put(cause, source);
        }
    }

    /**
     * Returns the non-fatal exceptions that occurred during the parsing or formatting.
     * If no exception occurred, then returns an empty set.
     *
     * @return The non-fatal exceptions that occurred.
     */
    public Set<Exception> getExceptions() {
        return (exceptionSources != null) ? exceptionSources.keySet() : Collections.emptySet();
    }

    /**
     * Returns the keywords of the WKT element where the given exception occurred, or {@code null} if unknown.
     * If this method returns a non-null array, then {@code source[0]} is the keyword of the WKT element where
     * the exception occurred and {@code source[1]} is the keyword of the parent of {@code source[0]}.
     *
     * <div class="note"><b>Note:</b>
     * in other words, this method returns the tail of the path to the WKT element where the exception occurred,
     * but with path elements stored in reverse order.
     * </div>
     *
     * @param  ex The exception for which to get the source.
     * @return The keywords of the WKT element where the given exception occurred, or {@code null} if unknown.
     */
    public String[] getExceptionSource(final Exception ex) {
        return (exceptionSources != null) ? exceptionSources.get(ex) : null;
    }

    /**
     * Returns the keywords of all unknown elements found during the WKT parsing.
     *
     * @return The keywords of unknown WKT elements, or an empty set if none.
     */
    public Set<String> getUnknownElements() {
        return ignoredElements.keySet();
    }

    /**
     * Returns the keyword of WKT elements that contains the given unknown element.
     * If the given element is not one of the value returned by {@link #getUnknownElements()},
     * then this method returns {@code null}.
     *
     * <p>The returned collection elements are in no particular order.</p>
     *
     * @param element The keyword of the unknown element.
     * @return The keywords of elements where the given unknown element was found.
     */
    public Collection<String> getUnknownElementLocations(final String element) {
        return ignoredElements.get(element);
    }

    /**
     * Returns a string representation of the warning messages if the default locale.
     * The locale used by this method is given by {@link #getLocale()}.
     * This is usually the locale given to the {@link WKTFormat} constructor.
     *
     * @return A string representation of the warning messages.
     */
    @Override
    public String toString() {
        return toString(errorLocale);
    }

    /**
     * Returns a string representation of the warning messages in the given locale.
     * This method formats the warnings in a bullet list.
     *
     * @param  locale The locale to use for formatting warning messages.
     * @return A string representation of the warning messages.
     */
    public String toString(final Locale locale) {
        final StringBuilder buffer = new StringBuilder(250);
        final String lineSeparator = System.lineSeparator();
        final Errors resources = Errors.getResources(locale);
        if (messages != null) {
            for (final Iterator<?> it = messages.iterator(); it.hasNext();) {
                InternationalString i18n = (InternationalString) it.next();
                Exception cause = (Exception) it.next();
                final String message;
                if (i18n != null) {
                    message = i18n.toString(locale);
                } else {
                    /*
                     * If there is no message, then we must have at least one exception.
                     * Consequently a NullPointerException below would be a bug.
                     */
                    final String[] sources = exceptionSources.get(cause);
                    if (sources != null) {
                        message = resources.getString(Errors.Keys.UnparsableStringInElement_2, sources);
                    } else {
                        message = cause.toString();
                        cause = null;
                    }
                }
                buffer.append(" • ").append(message).append(lineSeparator);
                if (cause != null) {
                    String details = Exceptions.getLocalizedMessage(cause, locale);
                    if (details == null) {
                        details = cause.toString();
                    }
                    buffer.append("   ").append(details).append(lineSeparator);
                }
            }
        }
        /*
         * If the parser found some unknown elements, formats an enclosed bullet list for them.
         */
        if (!ignoredElements.isEmpty()) {
            final Vocabulary vocabulary = Vocabulary.getResources(locale);
            buffer.append(" • ").append(resources.getString(Errors.Keys.UnknownElementsInText)).append(lineSeparator);
            for (final Map.Entry<String, List<String>> entry : ignoredElements.entrySet()) {
                buffer.append("    ‣ ").append(vocabulary.getString(Vocabulary.Keys.Quoted_1, entry.getKey()));
                String separator = vocabulary.getString(Vocabulary.Keys.InBetweenWords);
                for (final String p : entry.getValue()) {
                    buffer.append(separator).append(p);
                    separator = ", ";
                }
                buffer.append('.').append(lineSeparator);
            }
        }
        return buffer.toString();
    }
}
