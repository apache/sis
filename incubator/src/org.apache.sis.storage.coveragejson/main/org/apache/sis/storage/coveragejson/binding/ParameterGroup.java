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

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * A parameter group object MUST have either or both the members "label" or/and "observedProperty".
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","id","label","description","observedProperty","members"})
public final class ParameterGroup extends CoverageJsonObject {

    /**
     * A parameter group object MAY have a member with the name "id" where the
     * value MUST be a string and SHOULD be a common identifier.
     */
    public String id;
    /**
     * A parameter group object MAY have a member with the name "label" where
     * the value MUST be an i18n object that is the name of the parameter group
     * and which SHOULD be short. Note that this SHOULD be left out if it would
     * be identical to the "label" of the "observedProperty" member.
     */
    public I18N label;
    /**
     * A parameter group object MAY have a member with the name "description"
     * where the value MUST be an i18n object which is a, perhaps lengthy,
     * textual description of the parameter group.
     */
    public String description;
    /**
     * A parameter group object MAY have a member with the name "observedProperty"
     * where the value is an object as specified for parameter objects.
     */
    public ObservedProperty observedProperty;
    /**
     * A parameter group object MUST have a member with the name "members"
     * where the value is a non-empty array of parameter identifiers
     * (see 6.3 Coverage objects).
     */
    public String[] members;

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof ParameterGroup)) return false;

        final ParameterGroup cdt = ((ParameterGroup) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(label, cdt.label)
            && Objects.equals(description, cdt.description)
            && Objects.equals(observedProperty, cdt.observedProperty)
            && Arrays.equals(members, cdt.members);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                label,
                description,
                observedProperty,
                Arrays.hashCode(members));
    }
}
