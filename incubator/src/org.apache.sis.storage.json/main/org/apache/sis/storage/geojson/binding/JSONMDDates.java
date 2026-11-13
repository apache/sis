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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;

/**
 * Binding of the {@code CI_Date} JSON type, an object with the date type codes as keys.
 *
 * <p>Constraints:
 * <ul>
 *   <li>only one date per date type, other dates are ignored with a warning,</li>
 *   <li>dates without date type are ignored with a warning.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 c)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Date</a>
 */
public final class JSONMDDates {

    private final Map<String, JSONMDDateOrDateTime> dates = new LinkedHashMap<>();

    public JSONMDDates() {
    }

    public JSONMDDates(final Collection<? extends CitationDate> source, final JSONMDEncoder encoder) {
        if (source == null) {
            return;
        }
        for (final CitationDate date : source) {
            if (date == null) {
                continue;
            }
            final String type = encoder.code(date.getDateType());
            final JSONMDDateOrDateTime value = encoder.date(date.getReferenceDate());
            if (type == null) {
                encoder.warning("Citation date without date type is ignored: " + value);
            } else if (value != null && dates.putIfAbsent(type, value) != null) {
                encoder.warning("Only one citation date of type \"" + type + "\" can be encoded, " + value + " is ignored.");
            }
        }
    }

    @JsonAnyGetter
    public Map<String, JSONMDDateOrDateTime> getDates() {
        return dates;
    }

    @JsonAnySetter
    public void setDate(final String type, final JSONMDDateOrDateTime date) {
        dates.put(type, date);
    }

    public List<CitationDate> toMetadata(final JSONMDDecoder decoder) {
        final List<CitationDate> result = new ArrayList<>(dates.size());
        for (final Map.Entry<String, JSONMDDateOrDateTime> entry : dates.entrySet()) {
            if (entry.getValue() != null) {
                final DateType type = decoder.code(DateType.class, entry.getKey(), DateType::valueOf);
                result.add(new DefaultCitationDate(entry.getValue().getValue(), type));
            }
        }
        return result;
    }
}
