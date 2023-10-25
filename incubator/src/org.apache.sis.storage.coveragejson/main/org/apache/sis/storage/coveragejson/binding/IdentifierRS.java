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
import java.util.Objects;

/**
 * Identifier-based reference systems (identifier RS) .
 *
 * Coordinate values associated with an identifier RS MUST be strings.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","id","label","description","targetConcept","identifiers"})
public final class IdentifierRS extends CoverageJsonObject {

    /**
     * An identifier RS object MAY have a member "id" where the value MUST be a
     * string and SHOULD be a common identifier for the reference system.
     */
    public String id;
    /**
     * An identifier RS object MAY have a member "label" where the value MUST be
     * an i18n object that is the name of the reference system.
     */
    public I18N label;
    /**
     * An identifier RS object MAY have a member "description" where the value
     * MUST be an i18n object that is the (perhaps lengthy) description of the
     * reference system.
     */
    public I18N description;
    /**
     * An identifier RS object MUST have a member "targetConcept"
     */
    public TargetConcept targetConcept;
    /**
     * An identifier RS object MAY have a member "identifiers".
     */
    public Identifiers identifiers;


    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof IdentifierRS)) return false;

        final IdentifierRS cdt = ((IdentifierRS) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(label, cdt.label)
            && Objects.equals(description, cdt.description)
            && Objects.equals(targetConcept, cdt.targetConcept)
            && Objects.equals(identifiers, cdt.identifiers);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                label,
                description,
                targetConcept,
                identifiers);
    }

}
