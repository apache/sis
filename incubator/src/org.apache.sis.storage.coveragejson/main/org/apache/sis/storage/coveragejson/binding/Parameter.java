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
 * A parameter object MAY have any number of members (name/value pairs).
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","id","label","description","unit","observedProperty","categoryEncoding"})
public final class Parameter extends CoverageJsonObject {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A parameter object MAY have a member with the name "id" where the value
     * MUST be a string and SHOULD be a common identifier.
     */
    public String id;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A parameter object MAY have a member with the name "label" where the value
     * MUST be an i18n object that is the name of the parameter and which SHOULD be short.
     * Note that this SHOULD be left out if it would be identical to the "label"
     * of the "observedProperty" member.
     */
    public I18N label;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A parameter object MAY have a member with the name "description" where
     * the value MUST be an i18n object which is a, perhaps lengthy, textual
     * description of the parameter.
     */
    public I18N description;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A parameter object MAY have a member with the name "unit".
     * A parameter object MUST NOT have a "unit" member if the "observedProperty"
     * member has a "categories" member.
     */
    public Unit unit;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A parameter object MUST have a member with the name "observedProperty".
     */
    public ObservedProperty observedProperty;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * A parameter object MAY have a member with the name "categoryEncoding".
     */
    public CategoryEncoding categoryEncoding;

    public Parameter() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Parameter)) return false;

        final Parameter cdt = ((Parameter) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(label, cdt.label)
            && Objects.equals(description, cdt.description)
            && Objects.equals(unit, cdt.unit)
            && Objects.equals(observedProperty, cdt.observedProperty)
            && Objects.equals(categoryEncoding, cdt.categoryEncoding);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                label,
                description,
                unit,
                observedProperty,
                categoryEncoding);
    }
}
