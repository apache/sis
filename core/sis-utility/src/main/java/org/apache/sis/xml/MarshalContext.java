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

import java.util.Locale;
import java.util.TimeZone;
import org.apache.sis.util.Version;
import org.opengis.util.InternationalString;


/**
 * Context of a marshalling or unmarshalling process.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class MarshalContext {
    /**
     * Creates a new (un)marshalling context.
     */
    protected MarshalContext() {
    }

    /**
     * Returns the locale to use for (un)marshalling, or {@code null} if no locale were explicitly specified.
     * The locale returned by this method can be used for choosing a language in an {@link InternationalString}.
     *
     * <p>This locale may vary in different fragments of the same XML document.
     * In particular children of {@link org.opengis.metadata.Metadata} inherit the locale
     * specified by the {@link org.opengis.metadata.Metadata#getLanguage()} attribute.</p>
     *
     * <div class="section">Handling of {@code Locale.ROOT}</div>
     * {@link Locale#ROOT} is interpreted as a request for locale-neutral strings.
     * The meaning of "locale-neutral" is implementation specific - this is usually
     * very close to the English locale, but not necessarily. For examples dates are
     * formatted according ISO standard instead than the rules of the English locale.
     *
     * <div class="section">Handling of {@code null} locale</div>
     * A {@code null} value means that the locale is unspecified. Callers are encouraged
     * to use the root locale as the default value, but some flexibility is allowed.
     *
     * @return The locale for the XML fragment being (un)marshalled, or {@code null} is unspecified.
     *
     * @see org.apache.sis.util.iso.DefaultInternationalString#toString(Locale)
     */
    public abstract Locale getLocale();

    /**
     * Returns the timezone to use for (un)marshalling, or {@code null} if none were explicitely specified.
     *
     * <div class="section">Handling of <code>null</code> timezone</div>
     * A {@code null} value means that the timezone is unspecified. Callers are encouraged
     * to use the UTC timezone as the default value, but some flexibility is allowed.
     *
     * @return The timezone for the XML fragment being (un)marshalled, or {@code null} if unspecified.
     */
    public abstract TimeZone getTimeZone();

    /**
     * Returns the schema version of the XML document being (un)marshalled.
     * The {@code prefix} argument can be any of the following values (case-sensitive):
     *
     * <table class="sis">
     *   <caption>Supported schemas</caption>
     *   <tr>
     *     <th>Prefix</th>
     *     <th>Standard</th>
     *     <th>Typical values</th>
     *   </tr>
     *   <tr>
     *     <td>gml</td> <td>Geographic Markup Language</td> <td>{@code 3.0}, {@code 3.2}</td>
     *   </tr>
     * </table>
     *
     * @param  prefix One of the above-cited prefix.
     * @return The version for the given schema, or {@code null} if unknown.
     */
    public abstract Version getVersion(final String prefix);
}
