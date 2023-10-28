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
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"value","type"})
public final class Symbol extends Dictionary<Object> {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * "value" is the symbolic unit notation
     */
    public String value;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * "type" references the unit serialization scheme that is used. "type" MUST
     * HAVE the value "http://www.opengis.net/def/uom/UCUM/" if UCUM is used, or
     * a custom value as recommended in section Extensions.
     */
    public String type;

    public Symbol() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Symbol)) return false;

        final Symbol cdt = ((Symbol) other);
        return super.equals(other)
            && Objects.equals(value, cdt.value)
            && Objects.equals(type, cdt.type);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                value,
                type);
    }
}
