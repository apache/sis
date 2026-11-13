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
package org.apache.sis.storage.geojson.binding;

import java.net.URI;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.iso.Types;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.ControlledVocabulary;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;

/**
 * Context for converting SIS metadata values to their JSON form.
 *
 * <p>The encoder holds the output locale and logs the values which cannot be encoded.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3"
 */
public final class JSONMDEncoder {

    /**
     * Name of the logger receiving the encoding and decoding warnings.
     */
    public static final String LOGGER_NAME = "org.geotoolkit.metadata.json";

    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);

    private final Locale locale;

    /**
     * Creates an encoder writing texts in the given locale.
     *
     * @param locale locale of the JSON representation, use {@code Locale.ROOT} for the default text.
     */
    public JSONMDEncoder(final Locale locale) {
        this.locale = Objects.requireNonNull(locale);
    }

    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the text localized in the output locale.
     *
     * @see "ISO-19115-4:2026 section 4.8"
     */
    public String text(final InternationalString value) {
        return (value == null) ? null : value.toString(locale);
    }

    public List<String> texts(final Collection<? extends InternationalString> values) {
        return list(values, this::text);
    }

    /**
     * Returns the code of a code list or enumeration value.
     *
     * @see "ISO-19115-4:2026 section 5.1.4"
     */
    public String code(final ControlledVocabulary value) {
        return (value == null) ? null : Types.getCodeName(value);
    }

    public List<String> codes(final Collection<? extends ControlledVocabulary> values) {
        return list(values, this::code);
    }

    /**
     * Returns the date, or {@code null} with a warning if the temporal type is not supported.
     *
     * @see "ISO-19115-4:2026 section 5.1.3 d)"
     */
    public JSONMDDateOrDateTime date(final Temporal value) {
        if (value == null) {
            return null;
        }
        try {
            return new JSONMDDateOrDateTime(value);
        } catch (IllegalArgumentException e) {
            warning(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the dates keyed by date type, or {@code null} if there is none.
     *
     * @see "ISO-19115-4:2026 section 5.1.3 c)"
     */
    public JSONMDDates dates(final Collection<? extends CitationDate> values) {
        final JSONMDDates dates = new JSONMDDates(values, this);
        return dates.getDates().isEmpty() ? null : dates;
    }

    /**
     * Returns the instant or interval, or {@code null} with a warning if it cannot be encoded.
     *
     * <p>Constraints:
     * <ul>
     *   <li>only {@code Instant} and {@code Period} are supported,</li>
     *   <li>an instant without position cannot be encoded,</li>
     *   <li>a period bound without position is encoded as an open bound.</li>
     * </ul>
     *
     * @see "ISO-19115-4:2026 section 5.1.5"
     */
    public JSONMDInstantOrInterval time(final TemporalPrimitive value) {
        if (value instanceof Period period) {
            final JSONMDDateOrDateTime begin = position(period.getBeginning());
            final JSONMDDateOrDateTime end = position(period.getEnding());
            if (begin != null || end != null) {
                return new JSONMDInstantOrInterval(begin, end);
            }
        } else if (value instanceof Instant instant) {
            final JSONMDDateOrDateTime position = position(instant);
            if (position != null) {
                return new JSONMDInstantOrInterval(position);
            }
        } else if (value == null) {
            return null;
        }
        warning("Temporal primitive can not be encoded: " + value);
        return null;
    }

    private JSONMDDateOrDateTime position(final Instant instant) {
        return (instant == null) ? null : date(instant.getPosition());
    }

    public String uri(final URI value) {
        return (value == null) ? null : value.toString();
    }

    /**
     * Returns the duration in ISO 8601 form.
     *
     * @see "ISO-19115-4:2026 section 5.1.5"
     */
    public String duration(final TemporalAmount value) {
        return (value == null) ? null : value.toString();
    }

    public String unit(final Unit<?> value) {
        return (value == null) ? null : value.toString();
    }

    /**
     * Returns the URN of a reference system, or {@code null} with a warning if none is found.
     *
     * @see "ISO-19115-4:2026 section 5.1.5"
     */
    public String crs(final IdentifiedObject value) {
        if (value == null) {
            return null;
        }
        try {
            final String urn = IdentifiedObjects.lookupURN(value, null);
            if (urn == null) {
                warning("No URN found for reference system " + IdentifiedObjects.getDisplayName(value, locale));
            }
            return urn;
        } catch (FactoryException e) {
            warning("No URN found for reference system: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts all elements of a collection.
     *
     * <p>Constraints:
     * <ul>
     *   <li>{@code null} elements and {@code null} conversion results are skipped,</li>
     *   <li>returns {@code null} instead of an empty list.</li>
     * </ul>
     */
    public <S,T> List<T> list(final Collection<? extends S> values, final Function<? super S, ? extends T> converter) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        final List<T> result = new ArrayList<>(values.size());
        for (final S value : values) {
            if (value != null) {
                final T converted = converter.apply(value);
                if (converted != null) {
                    result.add(converted);
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Converts the first element of a collection which has a single value in JSON.
     *
     * <p>A warning is logged if the collection contains more than one element.
     *
     * @param property name of the JSON property, used in the warning.
     */
    public <S,T> T first(final Collection<? extends S> values, final Function<? super S, ? extends T> converter,
                         final String property)
    {
        if (values == null || values.isEmpty()) {
            return null;
        }
        final Iterator<? extends S> it = values.iterator();
        final S value = it.next();
        if (it.hasNext()) {
            warning("Property \"" + property + "\" has a single value in JSON, other values are ignored.");
        }
        return (value == null) ? null : converter.apply(value);
    }

    /**
     * Logs a value which cannot be encoded.
     */
    public void warning(final String message) {
        LOGGER.log(Level.WARNING, message);
    }
}
