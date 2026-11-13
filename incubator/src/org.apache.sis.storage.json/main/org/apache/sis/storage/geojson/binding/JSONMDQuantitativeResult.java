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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.util.iso.DefaultRecord;
import org.apache.sis.util.iso.DefaultRecordType;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.util.MemberName;
import org.opengis.util.Record;

/**
 * Binding of the {@code QuantitativeResult} JSON type.
 *
 * <p>Each record is written as a JSON value: a single-field record as the field value,
 * other records as an object with the field names as keys.
 *
 * <p>Constraints:
 * <ul>
 *   <li>when reading, only numbers and strings are supported as values, other values are ignored with a warning,</li>
 *   <li>{@code valueRecordType} is not supported.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json#QuantitativeResult</a>
 */
@JsonPropertyOrder({
    JSONMDQualityResult.PROPERTY_TYPE,
    JSONMDQualityResult.PROPERTY_DATE_TIME,
    JSONMDQualityResult.PROPERTY_RESULT_SCOPE,
    JSONMDQuantitativeResult.PROPERTY_VALUE,
    JSONMDQuantitativeResult.PROPERTY_VALUE_UNIT
})
public final class JSONMDQuantitativeResult extends JSONMDQualityResult {

    /**
     * Value of the {@code type} member.
     */
    public static final String TYPE = "QuantitativeResult";
    public static final String PROPERTY_VALUE = "value";
    public static final String PROPERTY_VALUE_UNIT = "valueUnit";

    @JsonProperty(PROPERTY_VALUE)
    private List<Object> value;
    @JsonProperty(PROPERTY_VALUE_UNIT)
    private String valueUnit;

    public JSONMDQuantitativeResult() {
    }

    public JSONMDQuantitativeResult(final QuantitativeResult source, final JSONMDEncoder encoder) {
        super(source, encoder);
        value     = encoder.list(source.getValues(), JSONMDQuantitativeResult::toJSON);
        valueUnit = encoder.unit(source.getValueUnit());
    }

    private static Object toJSON(final Record record) {
        final Map<MemberName, Object> fields = record.getFields();
        if (fields.size() == 1) {
            return simple(fields.values().iterator().next());
        }
        final Map<String, Object> json = new LinkedHashMap<>();
        for (final Map.Entry<MemberName, Object> entry : fields.entrySet()) {
            json.put(entry.getKey().toString(), simple(entry.getValue()));
        }
        return json;
    }

    private static Object simple(final Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        return value.toString();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public List<Object> getValue() {
        return value;
    }

    public void setValue(final List<Object> value) {
        this.value = value;
    }

    public String getValueUnit() {
        return valueUnit;
    }

    public void setValueUnit(final String valueUnit) {
        this.valueUnit = valueUnit;
    }

    @Override
    public DefaultQuantitativeResult toMetadata(final JSONMDDecoder decoder) {
        final DefaultQuantitativeResult target = new DefaultQuantitativeResult();
        fill(target, decoder);
        target.setValues(decoder.list(value, (Object e) -> toRecord(e, decoder)));
        target.setValueUnit(decoder.unit(valueUnit));
        return target;
    }

    private static Record toRecord(final Object value, final JSONMDDecoder decoder) {
        final DefaultRecord record;
        if (value instanceof Number number) {
            record = new DefaultRecord(DefaultRecordType.SINGLE_REAL);
            record.setAll(number.doubleValue());
        } else if (value instanceof String text) {
            record = new DefaultRecord(DefaultRecordType.SINGLE_STRING);
            record.setAll(text);
        } else {
            decoder.warning("Quantitative result value is not supported: " + value);
            return null;
        }
        return record;
    }
}
