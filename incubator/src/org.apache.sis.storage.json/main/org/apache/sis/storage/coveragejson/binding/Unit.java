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
package org.apache.sis.storage.coveragejson.binding;

import java.util.Objects;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * A "unit" where the value is an object which MUST have either or both the members
 * "label" or/and "symbol".
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"id","label","symbol"})
public final class Unit extends Dictionary<Object> {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * MAY have the member "id".
     * If given, the value of "id" MUST be a string and
     * SHOULD be a common identifier. It is RECOMMENDED to reference a unit
     * serialization scheme to allow automatic unit conversion.
     */
    public String id;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * If given, the value of "label" MUST be an i18n object of the name of
     * the unit and SHOULD be short.
     */
    public I18N label;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * If given, the value of "symbol" MUST either be a string of the symbolic notation of the unit,
     * or an object with the members "value" and "type".
     */
    public Object symbol;

    public Unit() {
    }

    public Unit(String id, I18N label, Object symbol) {
        this.id = id;
        this.label = label;
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Unit)) return false;

        final Unit cdt = ((Unit) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(label, cdt.label)
            && Objects.equals(symbol, cdt.symbol);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                label,
                symbol);
    }
}
