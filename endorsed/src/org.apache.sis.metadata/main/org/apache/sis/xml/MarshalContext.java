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

import java.time.ZoneId;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Version;


/**
 * Context of a marshalling or unmarshalling process.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public abstract class MarshalContext implements Localized {
    /**
     * Creates a new (un)marshalling context.
     */
    protected MarshalContext() {
    }

    /**
     * Returns the marshaller pool that produced the marshaller or unmarshaller in use.
     * This pool may be used for creating new (un)marshaller when a document contains
     * {@code xlink:href} to another document.
     *
     * @return the marshaller pool that produced the marshaller or unmarshaller in use.
     *
     * @since 1.5
     */
    public abstract MarshallerPool getPool();

    /**
     * Returns the locale to use for (un)marshalling, or {@code null} if no locale was explicitly specified.
     * The locale returned by this method can be used for choosing a language in an {@link InternationalString}.
     *
     * <p>This locale may vary in different fragments of the same XML document.
     * In particular children of {@link org.opengis.metadata.Metadata} inherit the locale
     * specified by the {@link org.opengis.metadata.Metadata#getLanguage()} attribute.</p>
     *
     * <h4>Handling of {@code Locale.ROOT}</h4>
     * {@link Locale#ROOT} is interpreted as a request for locale-neutral strings.
     * The meaning of "locale-neutral" is implementation specific - this is usually
     * very close to the English locale, but not necessarily. For examples dates are
     * formatted according ISO standard instead of the rules of the English locale.
     *
     * <h4>Handling of {@code null} locale</h4>
     * A {@code null} value means that the locale is unspecified. Callers are encouraged
     * to use the root locale as the default value, but some flexibility is allowed.
     *
     * @return the locale for the XML fragment being (un)marshalled, or {@code null} if unspecified.
     *
     * @see org.apache.sis.util.DefaultInternationalString#toString(Locale)
     */
    @Override
    public abstract Locale getLocale();

    /**
     * Returns the timezone to use for (un)marshalling, or {@code null} if none was explicitly specified.
     *
     * <h4>Handling of <code>null</code> timezone</h4>
     * A {@code null} value means that the timezone is unspecified. Callers are encouraged
     * to use the <abbr>UTC</abbr> timezone as the default value, but some flexibility is allowed.
     *
     * @return the timezone for the <abbr>XML</abbr> fragment being (un)marshalled, or {@code null} if unspecified.
     *
     * @since 1.5
     */
    public abstract ZoneId getZoneId();

    /**
     * Returns the legacy timezone to use for (un)marshalling, or {@code null} if none was explicitly specified.
     * This is he value returned by {@link #getZoneId()} converted to the legacy Java object.
     *
     * @return the timezone for the XML fragment being (un)marshalled, or {@code null} if unspecified.
     */
    public TimeZone getTimeZone() {
        ZoneId timezone = getZoneId();
        return (timezone != null) ? TimeZone.getTimeZone(timezone) : null;
    }

    /**
     * Returns the schema version of the XML document being (un)marshalled.
     * The {@code prefix} argument can be any of the following values (case-sensitive):
     *
     * <table class="sis">
     *   <caption>Supported schemas</caption>
     *   <tr><th>Prefix</th>  <th>Standard</th>                   <th>Typical values</th></tr>
     *   <tr><td>gml</td>     <td>Geographic Markup Language</td> <td>{@code 3.0}, {@code 3.2}</td></tr>
     *   <tr><td>gmd</td>     <td>Geographic MetaData</td>        <td>{@code 2007}, {@code 2016}</td></tr>
     * </table>
     *
     * @param  prefix  one of the above-cited prefix.
     * @return the version for the given schema, or {@code null} if unknown.
     */
    public abstract Version getVersion(String prefix);

    /**
     * Returns the URI of the document being (un)marshalled, if this URI is known.
     * The URI is generally unknown if the source of the XML document is,
     * for example, an {@link java.io.InputStream}.
     *
     * @return the URI of the document being marshalled or unmarshalled.
     *
     * @since 1.5
     */
    public abstract Optional<URI> getDocumentURI();
}
