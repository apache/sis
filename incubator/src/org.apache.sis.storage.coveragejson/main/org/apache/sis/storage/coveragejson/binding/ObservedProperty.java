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
import java.util.List;
import java.util.Objects;

/**
 * Observed property is an object which MUST have the member "label" and which
 * MAY have the members "id", "description", and "categories".
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"id","label","description","categories"})
public final class ObservedProperty extends Dictionary<Object> {

    /**
     * If given, the value of "id" MUST be a string and SHOULD be a common
     * identifier.
     */
    public String id;
    /**
     * The value of "label" MUST be an i18n object that is the name of the
     * observed property and which SHOULD be short.
     */
    public I18N label;
    /**
     * If given, the value of "description" MUST be an i18n object with a
     * textual description of the observed property.
     */
    public I18N description;
    /**
     *
     * If given, the value of "categories" MUST be a non-empty array of category
     * objects.
     */
    public List<Category> categories;

    public ObservedProperty() {
    }

    public ObservedProperty(String id, I18N label, I18N description, List<Category> categories) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.categories = categories;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof ObservedProperty)) return false;

        final ObservedProperty cdt = ((ObservedProperty) other);
        return super.equals(other)
            && Objects.equals(id, cdt.id)
            && Objects.equals(label, cdt.label)
            && Objects.equals(description, cdt.description)
            && Objects.equals(categories, cdt.categories);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                id,
                label,
                description,
                categories);
    }

}
