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
 * A category object MUST an "id" and a "label" member, and MAY have a
 * "description" member.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"id","label","description"})
public final class Category extends Dictionary<Object> {
    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The value of "id" MUST be a string and SHOULD be a common identifier.
     */
    public String id;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * The value of "label" MUST be an i18n object of the name of the
     * category and SHOULD be short.
     */
    public I18N label;

    /**
     * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
     * If given, the value of "description" MUST be an i18n object with a
     * textual description of the* category.
     */
    public I18N description;

    public Category() {
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Category)) return false;

        final Category cdt = ((Category) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(label, cdt.label)
            && Objects.equals(description, cdt.description);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                label,
                description);
    }
}
