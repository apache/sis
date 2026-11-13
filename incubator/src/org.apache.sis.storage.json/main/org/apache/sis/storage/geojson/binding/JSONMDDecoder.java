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
import java.net.URISyntaxException;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.temporal.GeneralDuration;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.Types;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;

/**
 * Context for converting JSON values to SIS metadata values.
 *
 * <p>The decoder logs the values which cannot be decoded and skips them.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3"
 */
public final class JSONMDDecoder {

    private static final Logger LOGGER = Logger.getLogger(JSONMDEncoder.LOGGER_NAME);

    public JSONMDDecoder() {
    }

    /**
     * Returns the text as an international string in the language of the JSON representation.
     *
     * @see "ISO-19115-4:2026 section 4.8"
     */
    public InternationalString text(final String value) {
        return (value == null) ? null : new SimpleInternationalString(value);
    }

    public List<InternationalString> texts(final Collection<String> values) {
        return list(values, this::text);
    }

    /**
     * Returns the texts as a set of character sequences.
     */
    public Set<CharSequence> textSet(final Collection<String> values) {
        return (values == null) ? Set.of() : new LinkedHashSet<>(values);
    }

    /**
     * Returns the code list value for the given code.
     *
     * <p>Unknown codes are created with the given constructor, and a warning is logged.
     *
     * @param type        code list type.
     * @param value       code, UML identifier or name of the value.
     * @param constructor creates a new code when the value is unknown, usually {@code CodeType::valueOf}.
     * @see "ISO-19115-4:2026 section 5.1.4"
     */
    public <T extends CodeList<T>> T code(final Class<T> type, final String value,
                                         final Function<? super String, ? extends T> constructor)
    {
        if (value == null) {
            return null;
        }
        T code = Types.forCodeName(type, value, null);
        if (code == null) {
            warning("Unknown " + type.getSimpleName() + " code \"" + value + "\", a new code is created.");
            code = constructor.apply(value);
        }
        return code;
    }

    public <T extends CodeList<T>> List<T> codes(final Class<T> type, final Collection<String> values,
                                                final Function<? super String, ? extends T> constructor)
    {
        return list(values, (String value) -> code(type, value, constructor));
    }

    /**
     * Returns the topic category, or {@code null} with a warning if the value is unknown.
     *
     * <p>{@code TopicCategory} is an enumeration and cannot be extended.
     */
    public TopicCategory topic(final String value) {
        if (value == null) {
            return null;
        }
        final TopicCategory topic = Types.forEnumName(TopicCategory.class, value);
        if (topic == null) {
            warning("Unknown topic category \"" + value + "\".");
        }
        return topic;
    }

    /**
     * Returns the URI, or {@code null} with a warning if the text is not a valid URI.
     */
    public URI uri(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            warning("Invalid URI \"" + value + "\": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the ISO 8601 duration, or {@code null} with a warning if the text cannot be parsed.
     *
     * @see "ISO-19115-4:2026 section 5.1.5"
     */
    public TemporalAmount duration(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return GeneralDuration.parse(value);
        } catch (DateTimeParseException e) {
            warning("Invalid duration \"" + value + "\": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the unit of measurement, or {@code null} with a warning if the symbol cannot be parsed.
     */
    public Unit<?> unit(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return Units.valueOf(value);
        } catch (MeasurementParseException e) {
            warning("Invalid unit of measurement \"" + value + "\": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the coordinate reference system for the given URI or code.
     *
     * <p>Returns {@code null} with a warning if the reference system cannot be created.
     *
     * @see "ISO-19115-4:2026 section 5.1.5"
     */
    public CoordinateReferenceSystem crs(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return CRS.forCode(value);
        } catch (FactoryException e) {
            warning("Can not create reference system \"" + value + "\": " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts all elements of a list.
     *
     * <p>Constraints:
     * <ul>
     *   <li>{@code null} elements are skipped with a warning (Jackson produces them for unknown {@code type} values),</li>
     *   <li>{@code null} conversion results are skipped silently, the converter is responsible for warnings,</li>
     *   <li>returns an empty list if {@code values} is {@code null}.</li>
     * </ul>
     */
    public <S,T> List<T> list(final Collection<? extends S> values, final Function<? super S, ? extends T> converter) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        final List<T> result = new ArrayList<>(values.size());
        for (final S value : values) {
            if (value != null) {
                final T converted = converter.apply(value);
                if (converted != null) {
                    result.add(converted);
                }
            } else {
                warning("Skipped a null or unrecognized value.");
            }
        }
        return result;
    }

    /**
     * Logs a value which cannot be decoded.
     */
    public void warning(final String message) {
        LOGGER.log(Level.WARNING, message);
    }
}
