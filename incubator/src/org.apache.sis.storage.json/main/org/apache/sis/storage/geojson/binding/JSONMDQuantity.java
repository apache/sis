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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;

/**
 * A value with its unit of measurement, written as {@code {"value": 250, "uom": "m"}}.
 *
 * <p>When reading, a plain number (unit omitted) and a single-element array as value
 * (as in the ISO 19115-4 reference example) are also accepted.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class JSONMDQuantity {

    public static final String PROPERTY_VALUE = "value";
    public static final String PROPERTY_UOM = "uom";

    private final double value;
    private final String uom;

    /**
     * Creates a quantity.
     *
     * @param value the value.
     * @param uom   symbol of the unit of measurement, or {@code null} if unspecified.
     */
    public JSONMDQuantity(final double value, final String uom) {
        this.value = value;
        this.uom   = uom;
    }

    /**
     * Parses a number, or an object with {@code value} and {@code uom} members.
     *
     * @throws IllegalArgumentException if the JSON value has another form.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public JSONMDQuantity(final JsonNode node) {
        if (node.isNumber()) {
            value = node.doubleValue();
            uom   = null;
        } else if (node.isObject()) {
            JsonNode number = node.get(PROPERTY_VALUE);
            if (number != null && number.isArray() && number.size() == 1) {
                number = number.get(0);
            }
            if (number == null || !number.isNumber()) {
                throw new IllegalArgumentException("Expected a numeric value, found: " + node);
            }
            value = number.doubleValue();
            final JsonNode unit = node.get(PROPERTY_UOM);
            uom   = (unit == null || unit.isNull()) ? null : unit.asText();
        } else {
            throw new IllegalArgumentException("Expected a number or a quantity object, found: " + node);
        }
    }

    public double getValue() {
        return value;
    }

    /**
     * Returns the symbol of the unit of measurement, or {@code null} if unspecified.
     */
    public String getUom() {
        return uom;
    }

    @JsonValue
    public Map<String, Object> toJSON() {
        final Map<String, Object> json = new LinkedHashMap<>();
        json.put(PROPERTY_VALUE, value);
        if (uom != null) {
            json.put(PROPERTY_UOM, uom);
        }
        return json;
    }

    /**
     * Returns the value converted to the given unit.
     *
     * <p>A value without unit is assumed to be already in the target unit.
     * Returns {@code null} with a warning if the unit is invalid or not convertible.
     */
    public Double toValue(final Unit<?> target, final JSONMDDecoder decoder) {
        if (uom == null) {
            return value;
        }
        final Unit<?> unit = decoder.unit(uom);
        if (unit == null) {
            return null;
        }
        try {
            return unit.getConverterToAny(target).convert(value);
        } catch (IncommensurableException | UnconvertibleException e) {
            decoder.warning("Unit \"" + uom + "\" can not be converted to " + target + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof JSONMDQuantity other
                && Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value)
                && Objects.equals(uom, other.uom);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value) + 31 * Objects.hashCode(uom);
    }

    @Override
    public String toString() {
        return (uom == null) ? Double.toString(value) : value + " " + uom;
    }
}
