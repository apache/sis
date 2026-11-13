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
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.opengis.metadata.citation.Telephone;
import org.opengis.metadata.citation.TelephoneType;

/**
 * Binding of the {@code CI_Telephone} JSON type, an object with the telephone type codes as keys.
 *
 * <p>Constraints:
 * <ul>
 *   <li>only one number per telephone type, other numbers are ignored with a warning,</li>
 *   <li>a number without telephone type is written with the {@code voice} type.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 c)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Telephone</a>
 */
public final class JSONMDTelephones {

    private final Map<String, String> numbers = new LinkedHashMap<>();

    public JSONMDTelephones() {
    }

    public JSONMDTelephones(final Collection<? extends Telephone> source, final JSONMDEncoder encoder) {
        if (source == null) {
            return;
        }
        for (final Telephone phone : source) {
            if (phone == null || phone.getNumber() == null) {
                continue;
            }
            String type = encoder.code(phone.getNumberType());
            if (type == null) {
                type = encoder.code(TelephoneType.VOICE);
            }
            if (numbers.putIfAbsent(type, phone.getNumber()) != null) {
                encoder.warning("Only one telephone number of type \"" + type + "\" can be encoded, "
                        + phone.getNumber() + " is ignored.");
            }
        }
    }

    @JsonAnyGetter
    public Map<String, String> getNumbers() {
        return numbers;
    }

    @JsonAnySetter
    public void setNumber(final String type, final String number) {
        numbers.put(type, number);
    }

    public List<Telephone> toMetadata(final JSONMDDecoder decoder) {
        final List<Telephone> result = new ArrayList<>(numbers.size());
        for (final Map.Entry<String, String> entry : numbers.entrySet()) {
            if (entry.getValue() != null) {
                final TelephoneType type = decoder.code(TelephoneType.class, entry.getKey(), TelephoneType::valueOf);
                result.add(new DefaultTelephone(entry.getValue(), type));
            }
        }
        return result;
    }
}
